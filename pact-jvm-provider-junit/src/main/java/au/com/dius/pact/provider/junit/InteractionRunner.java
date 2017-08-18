package au.com.dius.pact.provider.junit;

import au.com.dius.pact.model.PactSource;
import au.com.dius.pact.model.ProviderState;
import au.com.dius.pact.model.Interaction;
import au.com.dius.pact.model.Pact;
import au.com.dius.pact.provider.ProviderVerifierKt;
import au.com.dius.pact.provider.junit.target.Target;
import au.com.dius.pact.provider.junit.target.TestClassAwareTarget;
import au.com.dius.pact.provider.junit.target.TestTarget;
import org.apache.commons.lang3.ArrayUtils;
import org.apache.http.HttpRequest;
import org.junit.After;
import org.junit.Before;
import org.junit.Rule;
import org.junit.internal.runners.model.ReflectiveCallable;
import org.junit.internal.runners.statements.Fail;
import org.junit.internal.runners.statements.RunAfters;
import org.junit.internal.runners.statements.RunBefores;
import org.junit.rules.RunRules;
import org.junit.rules.TestRule;
import org.junit.runner.Description;
import org.junit.runner.Runner;
import org.junit.runner.notification.Failure;
import org.junit.runner.notification.RunNotifier;
import org.junit.runners.model.FrameworkField;
import org.junit.runners.model.FrameworkMethod;
import org.junit.runners.model.InitializationError;
import org.junit.runners.model.Statement;
import org.junit.runners.model.TestClass;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.lang.annotation.Annotation;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

import static org.junit.internal.runners.rules.RuleMemberValidator.RULE_METHOD_VALIDATOR;
import static org.junit.internal.runners.rules.RuleMemberValidator.RULE_VALIDATOR;

/**
 * Internal class to support pact test running
 * <p>
 * Developed with {@link org.junit.runners.BlockJUnit4ClassRunner} in mind
 */
class InteractionRunner extends Runner {
  private static final Logger LOGGER = LoggerFactory.getLogger(InteractionRunner.class);

  private final TestClass testClass;
  private final Pact pact;
  private final PactSource pactSource;

  private final ConcurrentHashMap<Interaction, Description> childDescriptions = new ConcurrentHashMap<>();

  public InteractionRunner(final TestClass testClass, final Pact pact, final PactSource pactSource) throws InitializationError {
    this.testClass = testClass;
    this.pact = pact;
    this.pactSource = pactSource;

    validate();
  }

    @Override
    public Description getDescription() {
        final Description description = Description.createSuiteDescription(testClass.getJavaClass());
        for (Interaction i: pact.getInteractions()) {
            description.addChild(describeChild(i));
        }
        return description;
    }

    protected Description describeChild(final Interaction interaction) {
      if (!childDescriptions.containsKey(interaction)) {
          childDescriptions.put(interaction, Description.createTestDescription(testClass.getJavaClass(),
            pact.getConsumer().getName() + " - " + interaction.getDescription()));
      }
      return childDescriptions.get(interaction);
    }

    // Validation
    protected void validate() throws InitializationError {
        final List<Throwable> errors = new ArrayList<>();

        validatePublicVoidNoArgMethods(Before.class, false, errors);
        validatePublicVoidNoArgMethods(After.class, false, errors);
        validateStateChangeMethods(State.class, false, errors);
        validateConstructor(errors);
        validateTestTarget(errors);
        validateRules(errors);
        validateTargetRequestFilters(errors);

        if (!errors.isEmpty()) {
            throw new InitializationError(errors);
        }
    }

  private void validateStateChangeMethods(final Class<? extends Annotation> annotation, final boolean isStatic, final List<Throwable> errors) {
    testClass.getAnnotatedMethods(annotation).forEach(method -> {
      method.validatePublicVoid(isStatic, errors);
      if (method.getMethod().getParameterCount() == 1 && !Map.class.isAssignableFrom(method.getMethod().getParameterTypes()[0])) {
        errors.add(new Exception("Method " + method.getName() + " should take only a single Map parameter"));
      } else if (method.getMethod().getParameterCount() > 1) {
        errors.add(new Exception("Method " + method.getName() + " should either take no parameters or a single Map parameter"));
      }
    });
  }

  private void validateTargetRequestFilters(final List<Throwable> errors) {
    testClass.getAnnotatedMethods(TargetRequestFilter.class).forEach(method -> {
        method.validatePublicVoid(false, errors);
        if (method.getMethod().getParameterTypes().length != 1) {
          errors.add(new Exception("Method " + method.getName() + " should take only a single HttpRequest parameter"));
        } else if (!HttpRequest.class.isAssignableFrom(method.getMethod().getParameterTypes()[0])) {
          errors.add(new Exception("Method " + method.getName() + " should take only a single HttpRequest parameter"));
        }
      });
  }

  protected void validatePublicVoidNoArgMethods(final Class<? extends Annotation> annotation, final boolean isStatic, final List<Throwable> errors) {
    testClass.getAnnotatedMethods(annotation).forEach(method -> method.validatePublicVoidNoArg(isStatic, errors));
  }

    protected void validateConstructor(final List<Throwable> errors) {
        if (!hasOneConstructor()) {
            errors.add(new Exception("Test class should have exactly one public constructor"));
        }
        if (!testClass.isANonStaticInnerClass()
                && hasOneConstructor()
                && (testClass.getOnlyConstructor().getParameterTypes().length != 0)) {
            errors.add(new Exception("Test class should have exactly one public zero-argument constructor"));
        }
    }

    protected boolean hasOneConstructor() {
        return testClass.getJavaClass().getConstructors().length == 1;
    }

    protected void validateTestTarget(final List<Throwable> errors) {
        final List<FrameworkField> annotatedFields = testClass.getAnnotatedFields(TestTarget.class);
        if (annotatedFields.size() != 1) {
            errors.add(new Exception("Test class should have exactly one field annotated with " + TestTarget.class.getName()));
        } else if (!Target.class.isAssignableFrom(annotatedFields.get(0).getType())) {
            errors.add(new Exception("Field annotated with " + TestTarget.class.getName() + " should implement " + Target.class.getName() + " interface"));
        }
    }

    protected void validateRules(final List<Throwable> errors) {
        RULE_VALIDATOR.validate(testClass, errors);
        RULE_METHOD_VALIDATOR.validate(testClass, errors);
    }

    // Running
    public void run(final RunNotifier notifier) {
      Boolean allPassed = true;
      for (final Interaction interaction : pact.getInteractions()) {
        final Description description = describeChild(interaction);
        notifier.fireTestStarted(description);
        try {
          interactionBlock(interaction, pactSource).evaluate();
        } catch (final Throwable e) {
          notifier.fireTestFailure(new Failure(description, e));
          allPassed = false;
        } finally {
          notifier.fireTestFinished(description);
        }
      }

      ProviderVerifierKt.reportVerificationResults(pact, allPassed, providerVersion());
    }

  private String providerVersion() {
    String version = System.getProperty("pact.provider.version");
    if (version != null) {
      return version;
    }
    LOGGER.warn("Set the provider version using the pact.provider.version property");
    return "0.0.0";
  }

    protected Object createTest() throws Exception {
        return testClass.getOnlyConstructor().newInstance();
    }

    protected Statement interactionBlock(final Interaction interaction, final PactSource source) {
        //1. prepare object
        //2. get Target
        //3. run Rule`s
        //4. run Before`s
        //5. run OnStateChange`s
        //6. run test
        //7. run After`s
        final Object test;
        try {
            test = new ReflectiveCallable() {
                @Override
                protected Object runReflectiveCall() throws Throwable {
                    return createTest();
                }
            }.run();
        } catch (Throwable e) {
            return new Fail(e);
        }
        final Target target = testClass.getAnnotatedFieldValues(test, TestTarget.class, Target.class).get(0);
        if (target instanceof TestClassAwareTarget) {
          ((TestClassAwareTarget) target).setTestClass(testClass, test);
        }

        Statement statement = new Statement() {
            @Override
            public void evaluate() throws Throwable {
                target.testInteraction(pact.getConsumer().getName(), interaction, source);
            }
        };
        statement = withStateChanges(interaction, test, statement);
        statement = withBefores(interaction, test, statement);
        statement = withRules(interaction, test, statement);
        statement = withAfters(interaction, test, statement);
        return statement;
    }

    protected Statement withStateChanges(final Interaction interaction, final Object target, final Statement statement) {
        if (!interaction.getProviderStates().isEmpty()) {
          Statement stateChange = statement;
          for (ProviderState state: interaction.getProviderStates()) {
            List<FrameworkMethod> methods = testClass.getAnnotatedMethods(State.class)
              .stream().filter(ann -> ArrayUtils.contains(ann.getAnnotation(State.class).value(), state.getName()))
              .collect(Collectors.toList());
            if (methods.isEmpty()) {
              return new Fail(new MissingStateChangeMethod("MissingStateChangeMethod: Did not find a test class method annotated with @State(\""
                + state.getName() + "\")"));
            } else {
              stateChange = new RunStateChanges(stateChange, methods, target, state);
            }
          }
          return stateChange;
        } else {
            return statement;
        }
    }

    protected Statement withBefores(final Interaction interaction, final Object target, final Statement statement) {
        final List<FrameworkMethod> befores = testClass.getAnnotatedMethods(Before.class);
        return befores.isEmpty() ? statement : new RunBefores(statement, befores, target);
    }

    protected Statement withAfters(final Interaction interaction, final Object target, final Statement statement) {
        final List<FrameworkMethod> afters = testClass.getAnnotatedMethods(After.class);
        return afters.isEmpty() ? statement : new RunAfters(statement, afters, target);
    }

    protected Statement withRules(final Interaction interaction, final Object target, final Statement statement) {
        final List<TestRule> testRules = testClass.getAnnotatedMethodValues(target, Rule.class, TestRule.class);
        testRules.addAll(testClass.getAnnotatedFieldValues(target, Rule.class, TestRule.class));
        return testRules.isEmpty() ? statement : new RunRules(statement, testRules, describeChild(interaction));
    }
}
