package au.com.dius.pact.provider.junit

import au.com.dius.pact.core.model.FilteredPact
import au.com.dius.pact.core.model.Interaction
import au.com.dius.pact.core.model.Pact
import au.com.dius.pact.core.model.PactSource
import au.com.dius.pact.provider.DefaultVerificationReporter
import au.com.dius.pact.provider.IProviderVerifier
import au.com.dius.pact.provider.ProviderVerifierBase.Companion.PACT_VERIFIER_PUBLISH_RESULTS
import au.com.dius.pact.provider.junit.target.Target
import au.com.dius.pact.provider.junit.target.TestClassAwareTarget
import au.com.dius.pact.provider.junit.target.TestTarget
import mu.KLogging
import org.apache.http.HttpRequest
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
import java.util.function.BiConsumer

/**
 * Internal class to support pact test running
 *
 * Developed with [org.junit.runners.BlockJUnit4ClassRunner] in mind
 */
open class InteractionRunner<I>(
  private val testClass: TestClass,
  private val pact: Pact<I>,
  private val pactSource: PactSource
) : Runner() where I: Interaction {

  private val results = ConcurrentHashMap<String, Pair<Boolean, IProviderVerifier>>()
  private val testContext = ConcurrentHashMap<String, Any>()
  private val childDescriptions = ConcurrentHashMap<String, Description>()
  var verificationReporter = DefaultVerificationReporter

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

  protected fun describeChild(interaction: Interaction): Description {
    if (!childDescriptions.containsKey(interaction.uniqueKey())) {
      childDescriptions[interaction.uniqueKey()] = Description.createTestDescription(testClass.javaClass,
        "${pact.consumer.name} - ${interaction.description}")
    }
    return childDescriptions[interaction.uniqueKey()]!!
  }

  // Validation
  protected fun validate() {
    val errors = mutableListOf<Throwable>()

    validatePublicVoidNoArgMethods(Before::class.java, false, errors)
    validatePublicVoidNoArgMethods(After::class.java, false, errors)
    validateStateChangeMethods(testClass, errors)
    validateConstructor(errors)
    validateTestTarget(errors)
    validateRules(errors)
    validateTargetRequestFilters(errors)

    if (!errors.isEmpty()) {
      throw InitializationError(errors)
    }
  }

  private fun validateTargetRequestFilters(errors: MutableList<Throwable>) {
    testClass.getAnnotatedMethods(TargetRequestFilter::class.java).forEach { method ->
      method.validatePublicVoid(false, errors)
      if (method.method.parameterTypes.size != 1) {
        errors.add(Exception("Method ${method.name} should take only a single HttpRequest parameter"))
      } else if (!HttpRequest::class.java.isAssignableFrom(method.method.parameterTypes[0])) {
        errors.add(Exception("Method ${method.name} should take only a single HttpRequest parameter"))
      }
    }
  }

  protected fun validatePublicVoidNoArgMethods(
    annotation: Class<out Annotation>,
    isStatic: Boolean,
    errors: MutableList<Throwable>
  ) {
    testClass.getAnnotatedMethods(annotation).forEach { method -> method.validatePublicVoidNoArg(isStatic, errors) }
  }

  protected fun validateConstructor(errors: MutableList<Throwable>) {
    if (!hasOneConstructor()) {
      errors.add(Exception("Test class should have exactly one public constructor"))
    }
    if (!testClass.isANonStaticInnerClass && hasOneConstructor() &&
      testClass.onlyConstructor.parameterTypes.isNotEmpty()) {
      errors.add(Exception("Test class should have exactly one public zero-argument constructor"))
    }
  }

  protected fun hasOneConstructor() = testClass.javaClass.constructors.size == 1

  protected fun validateTestTarget(errors: MutableList<Throwable>) {
    val annotatedFields = testClass.getAnnotatedFields(TestTarget::class.java)
    if (annotatedFields.size != 1) {
      errors.add(Exception("Test class should have exactly one field annotated with ${TestTarget::class.java.name}"))
    } else if (!Target::class.java.isAssignableFrom(annotatedFields[0].type)) {
      errors.add(Exception("Field annotated with ${TestTarget::class.java.name} should implement " +
        "${Target::class.java.name} interface"))
    }
  }

  protected fun validateRules(errors: List<Throwable>) {
    RULE_VALIDATOR.validate(testClass, errors)
    RULE_METHOD_VALIDATOR.validate(testClass, errors)
  }

  // Running
  override fun run(notifier: RunNotifier) {
    var allPassed = true
    for (interaction in pact.interactions) {
      val description = describeChild(interaction)
      notifier.fireTestStarted(description)
      try {
        interactionBlock(interaction, pactSource, testContext).evaluate()
      } catch (e: Throwable) {
        notifier.fireTestFailure(Failure(description, e))
        allPassed = false
      } finally {
        notifier.fireTestFinished(description)
      }
    }

    val publishingDisabled = results.values.any { it.second.publishingResultsDisabled() }
    if (!publishingDisabled && (pact !is FilteredPact<*> || pact.isNotFiltered())) {
      verificationReporter.reportResults(pact, allPassed, providerVersion())
    } else {
      if (publishingDisabled) {
        logger.warn { "Skipping publishing of verification results (" + PACT_VERIFIER_PUBLISH_RESULTS +
          " is not set to 'true')" }
      } else {
        logger.warn { "Skipping publishing of verification results as the interactions have been filtered" }
      }
    }
  }

  private fun providerVersion(): String {
    val version = System.getProperty("pact.provider.version")
    return if (version != null) {
      version
    } else {
      logger.warn { "Set the provider version using the 'pact.provider.version' property. Defaulting to '0.0.0'" }
      "0.0.0"
    }
  }

  protected open fun createTest(): Any {
    return testClass.onlyConstructor.newInstance()
  }

  protected fun interactionBlock(interaction: Interaction, source: PactSource, context: Map<String, Any>): Statement {

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

    val target = lookupTarget(testInstance)

    var statement: Statement = object : Statement() {
      override fun evaluate() {
        setupTargetForInteraction(target)
        target.addResultCallback(BiConsumer { result, verifier ->
          results[interaction.uniqueKey()] = Pair(result, verifier)
        })
        surrogateTestMethod()
        target.testInteraction(pact.consumer.name, interaction, source, mapOf("providerState" to context))
      }
    }
    statement = withStateChanges(interaction, testInstance, statement)
    statement = withBefores(interaction, testInstance, statement)
    statement = withRules(interaction, testInstance, statement)
    statement = withAfters(interaction, testInstance, statement)
    return statement
  }

  fun surrogateTestMethod() { }

  protected open fun setupTargetForInteraction(target: Target) { }

  protected fun lookupTarget(testInstance: Any): Target {
    val target = testClass.getAnnotatedFieldValues(testInstance, TestTarget::class.java, Target::class.java).first()
    if (target is TestClassAwareTarget) {
      target.setTestClass(testClass, testInstance)
    }
    return target
  }

  protected fun withStateChanges(interaction: Interaction, target: Any, statement: Statement): Statement {
    return if (interaction.providerStates.isNotEmpty()) {
      var stateChange = statement
      for (state in interaction.providerStates) {
        val methods = getAnnotatedMethods(testClass, State::class.java)
          .map { method -> method to method.getAnnotation(State::class.java) }
          .filter { pair -> pair.second.value.contains(state.name) }
        if (methods.isEmpty()) {
          return Fail(MissingStateChangeMethod("MissingStateChangeMethod: Did not find a test class method annotated " +
            "with @State(\"${state.name}\")"))
        } else {
          stateChange = RunStateChanges(stateChange, methods, target, state, testContext)
        }
      }
      stateChange
    } else {
      statement
    }
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

  companion object : KLogging() {

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
