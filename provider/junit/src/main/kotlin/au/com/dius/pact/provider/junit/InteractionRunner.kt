package au.com.dius.pact.provider.junit

import au.com.dius.pact.core.matchers.generators.ArrayContainsJsonGenerator
import au.com.dius.pact.core.model.BrokerUrlSource
import au.com.dius.pact.core.model.FilteredPact
import au.com.dius.pact.core.model.Interaction
import au.com.dius.pact.core.model.Pact
import au.com.dius.pact.core.model.PactSource
import au.com.dius.pact.core.model.ProviderState
import au.com.dius.pact.core.support.MetricEvent
import au.com.dius.pact.core.support.Metrics
import au.com.dius.pact.core.support.Result
import au.com.dius.pact.core.support.expressions.SystemPropertyResolver
import au.com.dius.pact.core.support.expressions.ValueResolver
import au.com.dius.pact.provider.DefaultTestResultAccumulator
import au.com.dius.pact.provider.IProviderVerifier
import au.com.dius.pact.provider.ProviderUtils
import au.com.dius.pact.provider.ProviderVersion
import au.com.dius.pact.provider.TestResultAccumulator
import au.com.dius.pact.provider.VerificationFailureType
import au.com.dius.pact.provider.VerificationResult
import au.com.dius.pact.provider.junit.descriptions.DescriptionGenerator
import au.com.dius.pact.provider.junit.target.TestClassAwareTarget
import au.com.dius.pact.provider.junitsupport.IgnoreMissingStateChange
import au.com.dius.pact.provider.junitsupport.MissingStateChangeMethod
import au.com.dius.pact.provider.junitsupport.State
import au.com.dius.pact.provider.junitsupport.TargetRequestFilter
import au.com.dius.pact.provider.junitsupport.target.Target
import au.com.dius.pact.provider.junitsupport.target.TestTarget
import io.github.oshai.kotlinlogging.KotlinLogging
import org.junit.After
import org.junit.Before
import org.junit.Rule
import org.junit.internal.runners.model.ReflectiveCallable
import org.junit.internal.runners.rules.RuleMemberValidator.RULE_METHOD_VALIDATOR
import org.junit.internal.runners.rules.RuleMemberValidator.RULE_VALIDATOR
import org.junit.internal.runners.statements.Fail
import org.junit.internal.runners.statements.RunAfters
import org.junit.internal.runners.statements.RunBefores
import org.junit.rules.RunRules
import org.junit.rules.TestRule
import org.junit.runner.Description
import org.junit.runner.Runner
import org.junit.runner.notification.Failure
import org.junit.runner.notification.RunNotifier
import org.junit.runners.model.FrameworkMethod
import org.junit.runners.model.InitializationError
import org.junit.runners.model.Statement
import org.junit.runners.model.TestClass
import java.util.concurrent.ConcurrentHashMap
import java.util.function.Supplier
import kotlin.Annotation
import kotlin.Any
import kotlin.Boolean
import kotlin.Exception
import kotlin.Pair
import kotlin.RuntimeException
import kotlin.String
import kotlin.Throwable
import kotlin.reflect.jvm.isAccessible
import kotlin.reflect.jvm.kotlinProperty
import kotlin.to
import org.apache.commons.lang3.tuple.Pair as TuplePair

private val logger = KotlinLogging.logger {}

/**
 * Internal class to support pact test running
 *
 * Developed with [org.junit.runners.BlockJUnit4ClassRunner] in mind
 */
open class InteractionRunner(
  protected val testClass: TestClass,
  private val pact: Pact,
  private val pactSource: PactSource
) : Runner() {

  private val results = ConcurrentHashMap<String, Pair<VerificationResult, IProviderVerifier>>()
  private val testContext = ConcurrentHashMap<String, Any>()
  private val childDescriptions = ConcurrentHashMap<String, Description>()
  private val descriptionGenerator = DescriptionGenerator(testClass, pactSource, pact.consumer.name)
  protected var propertyResolver: ValueResolver = SystemPropertyResolver

  var testResultAccumulator: TestResultAccumulator = DefaultTestResultAccumulator

  init {
    validate()
  }

  override fun getDescription(): Description {
    val description = Description.createSuiteDescription(testClass.javaClass)
    pact.interactions.forEach {
      description.addChild(describeChild(it))
    }
    return description
  }

  private fun describeChild(interaction: Interaction): Description {
    if (!childDescriptions.containsKey(interaction.uniqueKey())) {
      childDescriptions[interaction.uniqueKey()] = descriptionGenerator.generate(interaction)
    }
    return childDescriptions[interaction.uniqueKey()]!!
  }

  // Validation
  private fun validate() {
    val errors = mutableListOf<Throwable>()

    validatePublicVoidNoArgMethods(Before::class.java, false, errors)
    validatePublicVoidNoArgMethods(After::class.java, false, errors)
    validateStateChangeMethods(testClass, errors)
    validateConstructor(errors)
    validateTestTarget(errors)
    validateRules(errors)
    validateTargetRequestFilters(errors)

    if (errors.isNotEmpty()) {
      throw InitializationError(errors)
    }
  }

  private fun validateTargetRequestFilters(errors: MutableList<Throwable>) {
    testClass.getAnnotatedMethods(TargetRequestFilter::class.java).forEach { method ->
      method.validatePublicVoid(false, errors)
    }
  }

  private fun validatePublicVoidNoArgMethods(
    annotation: Class<out Annotation>,
    isStatic: Boolean,
    errors: MutableList<Throwable>
  ) {
    testClass.getAnnotatedMethods(annotation).forEach { method -> method.validatePublicVoidNoArg(isStatic, errors) }
  }

  private fun validateConstructor(errors: MutableList<Throwable>) {
    if (!hasOneConstructor()) {
      errors.add(Exception("Test class should have exactly one public constructor"))
    }
    if (!testClass.isANonStaticInnerClass && hasOneConstructor() &&
      testClass.javaClass.kotlin.constructors.first().parameters.isNotEmpty()) {
      errors.add(Exception("Test class should have exactly one public zero-argument constructor"))
    }
  }

  private fun hasOneConstructor() = testClass.javaClass.kotlin.constructors.size == 1

  private fun validateTestTarget(errors: MutableList<Throwable>) {
    val annotatedFields = testClass.getAnnotatedFields(TestTarget::class.java)
    if (annotatedFields.isEmpty()) {
      errors.add(Exception("Test class should have at least one field annotated with ${TestTarget::class.java.name}"))
    } else if (!Target::class.java.isAssignableFrom(annotatedFields[0].type)) {
      errors.add(Exception("Field annotated with ${TestTarget::class.java.name} should implement " +
        "${Target::class.java.name} interface"))
    }
  }

  private fun validateRules(errors: List<Throwable>) {
    RULE_VALIDATOR.validate(testClass, errors)
    RULE_METHOD_VALIDATOR.validate(testClass, errors)
  }

  // Running
  override fun run(notifier: RunNotifier) {
    for (interaction in pact.interactions) {
      val description = describeChild(interaction)
      val interactionId = interaction.interactionId
      var testResult: VerificationResult = VerificationResult.Ok(interactionId, emptyList())
      val pending = when {
        interaction.isV4() && interaction.asV4Interaction().pending -> true
        pact.source is BrokerUrlSource -> (pact.source as BrokerUrlSource).result?.pending == true
        else -> false
      }
      val included = interactionIncluded(interaction)
      if (!pending && included) {
        notifier.fireTestStarted(description)
      } else {
        if (!included) {
          logger.warn { "Ignoring interaction '${interaction.description}' as it does not match the filter " +
            "pact.filter.description='${System.getProperty("pact.filter.description")}'" }
        }
        notifier.fireTestIgnored(description)
      }

      if (included) {
        try {
          interactionBlock(interaction, pactSource, testContext, pending).evaluate()
        } catch (e: Throwable) {
          testResult = VerificationResult.Failed("Request to provider failed with an exception", description.displayName,
            mapOf(interaction.interactionId.orEmpty() to
              listOf(VerificationFailureType.ExceptionFailure("Request to provider failed with an exception",
                e, interaction))),
            pending)
        } finally {
          val updateTestResult = testResultAccumulator.updateTestResult(if (pact is FilteredPact) pact.pact else pact, interaction,
            testResult.toTestResult(), pactSource, propertyResolver)
          if (testResult is VerificationResult.Ok && updateTestResult is Result.Err) {
            testResult = VerificationResult.Failed("Failed to publish results to Pact broker",
              description.displayName, mapOf(interaction.interactionId.orEmpty() to
                listOf(VerificationFailureType.PublishResultsFailure(updateTestResult.error))),
              pending)
          }

          if (!pending) {
            when (testResult) {
              is VerificationResult.Ok -> notifier.fireTestFinished(description)
              is VerificationResult.Failed -> {
                val failure = testResult.failures[interactionId.orEmpty()]?.first()
                if (failure is VerificationFailureType.ExceptionFailure) {
                  notifier.fireTestFailure(Failure(description, failure.getException()))
                } else {
                  notifier.fireTestFailure(Failure(description, RuntimeException()))
                }
                notifier.fireTestFinished(description)
              }
            }
          }
        }
      }
    }
  }

  private fun interactionIncluded(interaction: Interaction): Boolean {
    val interactionFilter = System.getProperty("pact.filter.description")
    return interactionFilter.isNullOrEmpty() || interaction.description.matches(Regex(interactionFilter))
  }

  private fun providerVersion(): String {
    return ProviderVersion { System.getProperty("pact.provider.version") }.get()
  }

  protected open fun createTest(): Any {
    return testClass.javaClass.getDeclaredConstructor().newInstance()
  }

  protected fun interactionBlock(
    interaction: Interaction,
    source: PactSource,
    context: Map<String, Any>,
    pending: Boolean
  ): Statement {

    // 1. prepare object
    // 2. get Target
    // 3. run Rule`s
    // 4. run Before`s
    // 5. run OnStateChange`s
    // 6. run test
    // 7. run After`s

    val testInstance: Any
    try {
      testInstance = object : ReflectiveCallable() {
        override fun runReflectiveCall() = createTest()
      }.run()
    } catch (e: Throwable) {
      return Fail(e)
    }

    val target = lookupTarget(testInstance, interaction)
    target.configureVerifier(source, pact.consumer.name, interaction)
    target.verifier.verificationSource = "junit"
    target.verifier.reportInteractionDescription(interaction)

    var statement: Statement = object : Statement() {
      override fun evaluate() {
        setupTargetForInteraction(target)
        target.addResultCallback { result, verifier ->
          results[interaction.uniqueKey()] = Pair(result, verifier)
        }
        Metrics.sendMetrics(MetricEvent.ProviderVerificationRan(1, "junit"))
        target.testInteraction(pact.consumer.name, interaction, source,
          mutableMapOf("providerState" to context, "ArrayContainsJsonGenerator" to ArrayContainsJsonGenerator),
          pending
        )
      }
    }
    statement = withStateChanges(interaction, testInstance, statement, target)
    statement = withBefores(interaction, testInstance, statement)
    statement = withRules(interaction, testInstance, statement)
    statement = withAfters(interaction, testInstance, statement)
    return statement
  }

  protected open fun setupTargetForInteraction(target: Target) { }

  protected fun lookupTarget(testInstance: Any, interaction: Interaction): Target {
    val target = testClass.getAnnotatedFields(TestTarget::class.java).map {
      if (it.field.kotlinProperty != null) {
        it.field.kotlinProperty!!.getter.isAccessible = true
        it.field.kotlinProperty!!.getter.call(testInstance)
      } else {
        it.field.isAccessible = true
        it.get(testInstance)
      }
    }.first { (it as Target).validForInteraction(interaction) }
    if (target is TestClassAwareTarget) {
      target.setTestClass(testClass, testInstance)
    }
    return target as Target
  }

  protected fun withStateChanges(interaction: Interaction, target: Any, statement: Statement, testTarget: Target): Statement {
    return if (interaction.providerStates.isNotEmpty()) {
      var stateChange = statement
      for (state in interaction.providerStates.reversed()) {
        testContext.putAll(state.params.filterValues { it != null } as Map<String, Any>)

        val methods = findStateChangeMethod(state, testTarget.getStateHandlers())
        if (methods.isEmpty()) {
          return if (ignoreMissingStateChangeMethod()) {
            MissingStateChangeMethodStatement(state, interaction, pact.consumer.name)
          } else {
            Fail(
              MissingStateChangeMethod(
                "MissingStateChangeMethod: Did not find a test class method annotated " +
                  "with @State(\"${state.name}\") " +
                  "for Interaction (\"${interaction.description}\") " +
                  "and Consumer ${pact.consumer.name}"
              )
            )
          }
        } else {
          stateChange = RunStateChanges(stateChange, methods, listOf(Supplier { target }) +
            testTarget.getStateHandlers().map { it.right }, state, testContext, testTarget.verifier)
        }
      }
      stateChange
    } else {
      statement
    }
  }

  private fun ignoreMissingStateChangeMethod(): Boolean {
    return ProviderUtils.findAnnotation(testClass.javaClass, IgnoreMissingStateChange::class.java) != null
  }

  private fun findStateChangeMethod(
    state: ProviderState,
    stateHandlers: List<TuplePair<Class<out Any>, Supplier<out Any>>>
  ): List<Pair<FrameworkMethod, State>> {
    return (listOf(testClass) + stateHandlers.map { TestClass(it.left) })
    .flatMap { getAnnotatedMethods(it, State::class.java) }
    .map { method -> method to method.getAnnotation(State::class.java) }
    .filter { pair -> pair.second.value.contains(state.name) }
  }

  protected open fun withBefores(interaction: Interaction, target: Any, statement: Statement): Statement {
    val befores = testClass.getAnnotatedMethods(Before::class.java)
    return if (befores.isEmpty()) statement else RunBefores(statement, befores, target)
  }

  protected open fun withAfters(interaction: Interaction, target: Any, statement: Statement): Statement {
    val afters = testClass.getAnnotatedMethods(After::class.java)
    return if (afters.isEmpty()) statement else RunAfters(statement, afters, target)
  }

  protected fun withRules(interaction: Interaction, target: Any, statement: Statement): Statement {
    val testRules = testClass.getAnnotatedMethodValues(target, Rule::class.java, TestRule::class.java)
    testRules.addAll(testClass.getAnnotatedFieldValues(target, Rule::class.java, TestRule::class.java))
    return if (testRules.isEmpty()) statement else RunRules(statement, testRules, describeChild(interaction))
  }

  companion object {

    private fun validateStateChangeMethods(testClass: TestClass, errors: MutableList<Throwable>) {
      getAnnotatedMethods(testClass, State::class.java).forEach { method ->
        if (method.isStatic) {
          errors.add(Exception("Method ${method.name}() should not be static"))
        }
        if (!method.isPublic) {
          errors.add(Exception("Method ${method.name}() should be public"))
        }
        if (method.method.parameterCount == 1 && !Map::class.java.isAssignableFrom(method.method.parameterTypes[0])) {
          errors.add(Exception("Method ${method.name} should take only a single Map parameter"))
        } else if (method.method.parameterCount > 1) {
          errors.add(Exception("Method ${method.name} should either take no parameters or a single Map parameter"))
        }
      }
    }

    private fun getAnnotatedMethods(testClass: TestClass, annotation: Class<out Annotation>): List<FrameworkMethod> {
      val methodsFromTestClass = testClass.getAnnotatedMethods(annotation)
      val allMethods = mutableListOf<FrameworkMethod>()
      allMethods.addAll(methodsFromTestClass)
      allMethods.addAll(getAnnotatedMethodsFromInterfaces(testClass, annotation))
      return allMethods
    }

    private fun getAnnotatedMethodsFromInterfaces(testClass: TestClass, annotation: Class<out Annotation>): List<FrameworkMethod> {
      val stateMethods = mutableListOf<FrameworkMethod>()
      val interfaces = testClass.javaClass.interfaces
      for (interfaceClass in interfaces) {
        for (method in interfaceClass.declaredMethods) {
          if (method.isAnnotationPresent(annotation)) {
            stateMethods.add(FrameworkMethod(method))
          }
        }
      }
      return stateMethods
    }
  }
}

class MissingStateChangeMethodStatement(
  val state: ProviderState,
  val interaction: Interaction,
  val consumerName: String
) : Statement() {
  override fun evaluate() {
    logger.warn { "MissingStateChangeMethod: Did not find a test class method annotated " +
      "with @State(\"${state.name}\") " +
      "for Interaction (\"${interaction.description}\") " +
      "and Consumer $consumerName" }
  }
}
