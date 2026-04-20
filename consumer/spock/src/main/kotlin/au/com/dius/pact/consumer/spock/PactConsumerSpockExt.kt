package au.com.dius.pact.consumer.spock

import au.com.dius.pact.consumer.BaseMockServer
import au.com.dius.pact.consumer.ConsumerPactBuilder
import au.com.dius.pact.consumer.MessagePactBuilder
import au.com.dius.pact.consumer.MockServer
import au.com.dius.pact.consumer.PactVerificationResult
import au.com.dius.pact.consumer.dsl.PactBuilder
import au.com.dius.pact.consumer.dsl.SynchronousMessagePactBuilder
import au.com.dius.pact.consumer.junit.JUnitTestSupport
import au.com.dius.pact.consumer.mockServer
import au.com.dius.pact.core.model.BasePact
import au.com.dius.pact.core.model.Consumer
import au.com.dius.pact.core.model.PactSpecVersion
import au.com.dius.pact.core.model.Provider
import au.com.dius.pact.core.model.V4Interaction
import au.com.dius.pact.core.model.annotations.Pact
import au.com.dius.pact.core.model.annotations.PactDirectory
import au.com.dius.pact.core.support.BuiltToolConfig
import au.com.dius.pact.core.support.MetricEvent
import au.com.dius.pact.core.support.Metrics
import au.com.dius.pact.core.support.expressions.DataType
import au.com.dius.pact.core.support.expressions.ExpressionParser
import io.github.oshai.kotlinlogging.KotlinLogging
import org.spockframework.runtime.extension.IAnnotationDrivenExtension
import org.spockframework.runtime.extension.IMethodInvocation
import org.spockframework.runtime.model.SpecInfo
import spock.lang.Specification
import java.lang.reflect.Method
import java.util.concurrent.ConcurrentHashMap

private val logger = KotlinLogging.logger {}

class PactConsumerSpockExt : IAnnotationDrivenExtension<PactConsumerSpockTest> {

  private val ep = ExpressionParser()

  override fun visitSpecAnnotation(annotation: PactConsumerSpockTest, spec: SpecInfo) {
    val executedFragments = ConcurrentHashMap.newKeySet<Method>()
    val pactsToWrite = ConcurrentHashMap<Pair<Consumer, Provider>, Pair<BasePact, PactSpecVersion>>()

    val classLevelAnnotation = spec.reflection.getAnnotation(PactSpecFor::class.java)

    for (feature in spec.allFeatures) {
      val methodAnnotation = feature.featureMethod.reflection.getAnnotation(PactSpecFor::class.java)
      val pactTestFor = methodAnnotation ?: classLevelAnnotation
      if (pactTestFor != null) {
        feature.addInterceptor { invocation ->
          runPactTest(pactTestFor, spec, invocation, executedFragments, pactsToWrite)
        }
      }
    }

    spec.addInterceptor { invocation ->
      invocation.proceed()
      val pactDirectory = lookupPactDirectory(spec)
      pactsToWrite.values.forEach { (pact, version) ->
        logger.debug { "Writing pact ${pact.consumer.name} -> ${pact.provider.name} to ${pact.fileForPact(pactDirectory)}" }
        pact.write(pactDirectory, version)
      }
      validateExecutedFragments(executedFragments, spec)
    }
  }

  private fun runPactTest(
    pactTestFor: PactSpecFor,
    spec: SpecInfo,
    invocation: IMethodInvocation,
    executedFragments: MutableSet<Method>,
    pactsToWrite: MutableMap<Pair<Consumer, Provider>, Pair<BasePact, PactSpecVersion>>
  ) {
    val instance = invocation.instance as Specification
    val providerInfo = ProviderInfo.fromAnnotation(pactTestFor)
    val pact = buildPact(providerInfo, pactTestFor, spec, instance, executedFragments)
    val config = providerInfo.mockServerConfig()
    val mockServer = mockServer(pact, config)

    injectMockServer(instance, spec, mockServer)
    mockServer.start()
    mockServer.waitForServer()

    var testPassed = false
    try {
      invocation.proceed()
      testPassed = true
    } finally {
      Thread.sleep(100)
      mockServer.stop()
      if (testPassed) {
        val result = mockServer.validateMockServerState(null)
        if (result is PactVerificationResult.Ok) {
          storePactForWrite(pactsToWrite, pact, mockServer, providerInfo)
        } else {
          JUnitTestSupport.validateMockServerResult(result)
        }
      }
    }
    Metrics.sendMetrics(MetricEvent.ConsumerTestRun(pact.interactions.size, "spock"))
  }

  private fun buildPact(
    providerInfo: ProviderInfo,
    pactTestFor: PactSpecFor,
    spec: SpecInfo,
    instance: Specification,
    executedFragments: MutableSet<Method>
  ): BasePact {
    val providerName = providerInfo.providerName.ifEmpty { "default" }
    val method = findPactMethod(spec, providerName, pactTestFor.pactMethod)
      ?: throw UnsupportedOperationException(
        "No method annotated with @Pact was found on test class '${spec.reflection.simpleName}' " +
          "for provider '$providerName'"
      )

    val providerType = providerInfo.providerType ?: ProviderType.SYNCH
    when {
      providerType == ProviderType.SYNCH &&
        !JUnitTestSupport.conformsToSignature(method, providerInfo.pactVersion ?: PactSpecVersion.V4) ->
        throw UnsupportedOperationException(
          "Method '${method.name}' does not conform to required signature " +
            "'public [RequestResponsePact|V4Pact] xxx([PactDslWithProvider|PactBuilder] builder)'"
        )
      providerType == ProviderType.ASYNCH &&
        !JUnitTestSupport.conformsToMessagePactSignature(method, providerInfo.pactVersion ?: PactSpecVersion.V4) ->
        throw UnsupportedOperationException(
          "Method '${method.name}' does not conform to required signature " +
            "'public [MessagePact|V4Pact] xxx([MessagePactBuilder|PactBuilder] builder)'"
        )
      providerType == ProviderType.SYNCH_MESSAGE &&
        !JUnitTestSupport.conformsToSynchMessagePactSignature(method, providerInfo.pactVersion ?: PactSpecVersion.V4) ->
        throw UnsupportedOperationException(
          "Method '${method.name}' does not conform to required signature " +
            "'public V4Pact xxx([SynchronousMessagePactBuilder|PactBuilder] builder)'"
        )
    }

    val pactAnnotation = method.getAnnotation(Pact::class.java)
    val pactConsumer = ep.parseExpression(pactAnnotation.consumer, DataType.STRING)?.toString()
      ?: pactAnnotation.consumer
    val pactProviderFromAnnotation = ep.parseExpression(pactAnnotation.provider, DataType.STRING)?.toString()
    val providerNameToUse = if (pactProviderFromAnnotation.isNullOrEmpty()) providerName else pactProviderFromAnnotation

    logger.debug { "Invoking '@Pact' method '${method.name}' to build pact for '${spec.reflection.simpleName}'" }

    val pact = invokePactMethod(method, instance, providerInfo, pactConsumer, providerNameToUse, providerType)

    if (providerInfo.pactVersion != null && providerInfo.pactVersion >= PactSpecVersion.V4) {
      pact.asV4Pact().unwrap().interactions.forEach { i ->
        (i as V4Interaction).setTestName("${spec.reflection.name}.${method.name}")
      }
    }

    executedFragments.add(method)
    return pact
  }

  private fun invokePactMethod(
    method: Method,
    instance: Specification,
    providerInfo: ProviderInfo,
    pactConsumer: String,
    providerName: String,
    providerType: ProviderType
  ): BasePact {
    method.isAccessible = true
    return when (providerType) {
      ProviderType.SYNCH, ProviderType.UNSPECIFIED -> {
        if (method.parameterTypes[0].isAssignableFrom(
            Class.forName("au.com.dius.pact.consumer.dsl.PactDslWithProvider")
          )
        ) {
          val builder = ConsumerPactBuilder.consumer(pactConsumer)
          if (providerInfo.pactVersion != null) builder.pactSpecVersion(providerInfo.pactVersion)
          method.invoke(instance, builder.hasPactWith(providerName)) as BasePact
        } else {
          val builder = PactBuilder(pactConsumer, providerName)
          if (providerInfo.pactVersion != null) builder.pactSpecVersion(providerInfo.pactVersion)
          method.invoke(instance, builder) as BasePact
        }
      }
      ProviderType.ASYNCH -> {
        if (method.parameterTypes[0].isAssignableFrom(
            Class.forName("au.com.dius.pact.consumer.MessagePactBuilder")
          )
        ) {
          method.invoke(
            instance,
            MessagePactBuilder(providerInfo.pactVersion ?: PactSpecVersion.V3)
              .consumer(pactConsumer).hasPactWith(providerName)
          ) as BasePact
        } else {
          val builder = PactBuilder(pactConsumer, providerName)
          if (providerInfo.pactVersion != null) builder.pactSpecVersion(providerInfo.pactVersion)
          method.invoke(instance, builder) as BasePact
        }
      }
      ProviderType.SYNCH_MESSAGE -> {
        if (method.parameterTypes[0].isAssignableFrom(
            Class.forName("au.com.dius.pact.consumer.dsl.SynchronousMessagePactBuilder")
          )
        ) {
          method.invoke(
            instance,
            SynchronousMessagePactBuilder(providerInfo.pactVersion ?: PactSpecVersion.V4)
              .consumer(pactConsumer).hasPactWith(providerName)
          ) as BasePact
        } else {
          val builder = PactBuilder(pactConsumer, providerName)
          if (providerInfo.pactVersion != null) builder.pactSpecVersion(providerInfo.pactVersion)
          method.invoke(instance, builder) as BasePact
        }
      }
    }
  }

  private fun findPactMethod(spec: SpecInfo, providerName: String, pactMethod: String): Method? {
    val methods = generateSequence(spec.reflection) { it.superclass }
      .flatMap { it.declaredMethods.asSequence() }
      .filter { it.isAnnotationPresent(Pact::class.java) }
      .toList()
    return when {
      pactMethod.isNotEmpty() -> {
        logger.debug { "Looking for @Pact method named '$pactMethod' for provider '$providerName'" }
        methods.firstOrNull { it.name == pactMethod }
      }
      providerName.isEmpty() -> {
        logger.debug { "Looking for first @Pact method" }
        methods.firstOrNull()
      }
      else -> {
        logger.debug { "Looking for first @Pact method for provider '$providerName'" }
        methods.firstOrNull {
          val annotationProvider = it.getAnnotation(Pact::class.java).provider
          val resolved = ep.parseExpression(annotationProvider, DataType.STRING)?.toString() ?: annotationProvider
          resolved.isEmpty() || resolved == providerName
        }
      }
    }
  }

  private fun injectMockServer(instance: Specification, spec: SpecInfo, mockServer: BaseMockServer) {
    val field = generateSequence(spec.reflection) { it.superclass }
      .flatMap { it.declaredFields.asSequence() }
      .firstOrNull { MockServer::class.java.isAssignableFrom(it.type) }
    if (field != null) {
      field.isAccessible = true
      field.set(instance, mockServer)
    } else {
      logger.warn {
        "No field of type MockServer found on '${spec.reflection.simpleName}' — " +
          "declare a 'MockServer mockServer' field to receive the injected mock server"
      }
    }
  }

  private fun storePactForWrite(
    pactsToWrite: MutableMap<Pair<Consumer, Provider>, Pair<BasePact, PactSpecVersion>>,
    pact: BasePact,
    mockServer: BaseMockServer,
    providerInfo: ProviderInfo
  ) {
    val updatedPact = mockServer.updatePact(pact) as BasePact
    val version = providerInfo.pactVersion ?: PactSpecVersion.V4
    pactsToWrite.merge(
      Pair(updatedPact.consumer, updatedPact.provider),
      Pair(updatedPact, version)
    ) { (currentPact, currentVersion), _ ->
      Pair(
        currentPact.mergeInteractions(updatedPact.interactions) as BasePact,
        maxOf(version, currentVersion)
      )
    }
  }

  private fun lookupPactDirectory(spec: SpecInfo): String {
    val annotation = generateSequence(spec.reflection) { it.superclass }
      .mapNotNull { it.getAnnotation(PactDirectory::class.java) }
      .firstOrNull()
    return if (annotation != null) {
      logger.info { "Writing pacts out to directory from @PactDirectory annotation: ${annotation.value}" }
      annotation.value
    } else {
      logger.info { "Writing pacts out to default directory: ${BuiltToolConfig.pactDirectory}" }
      BuiltToolConfig.pactDirectory
    }
  }

  private fun validateExecutedFragments(executedFragments: Set<Method>, spec: SpecInfo) {
    val allPactMethods = generateSequence(spec.reflection) { it.superclass }
      .flatMap { it.declaredMethods.asSequence() }
      .filter { it.isAnnotationPresent(Pact::class.java) }
      .toList()
    val nonExecuted = allPactMethods.filter { it !in executedFragments }
    if (nonExecuted.isNotEmpty()) {
      val names = nonExecuted.joinToString(", ") { "${it.declaringClass.simpleName}.${it.name}" }
      throw AssertionError(
        "The following methods annotated with @Pact were not executed during the test: $names\n" +
          "If these are currently a work in progress, add @Ignore to the corresponding feature method\n"
      )
    }
  }
}
