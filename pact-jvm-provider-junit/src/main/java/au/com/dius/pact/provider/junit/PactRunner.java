package au.com.dius.pact.provider.junit;

import au.com.dius.pact.model.Pact;
import au.com.dius.pact.provider.junit.loader.NoPactsFoundException;
import au.com.dius.pact.provider.junit.loader.PactBroker;
import au.com.dius.pact.provider.junit.loader.PactFolder;
import au.com.dius.pact.provider.junit.loader.PactLoader;
import au.com.dius.pact.provider.junit.loader.PactSource;
import au.com.dius.pact.provider.junit.target.HttpTarget;
import au.com.dius.pact.provider.junit.target.Target;
import au.com.dius.pact.provider.junit.target.TestTarget;
import groovy.json.JsonException;
import org.junit.runner.Description;
import org.junit.runner.notification.RunNotifier;
import org.junit.runners.ParentRunner;
import org.junit.runners.model.InitializationError;
import org.junit.runners.model.TestClass;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.lang.annotation.Annotation;
import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;
import java.util.ArrayList;
import java.util.List;

/**
 * JUnit Runner runs pacts against provider
 * To set up name of tested provider use {@link Provider} annotation
 * To point on pact's source use {@link PactBroker}, {@link PactFolder} or {@link PactSource} annotations
 * <p>
 * To point provider for testing use combination of {@link Target} interface and {@link TestTarget} annotation
 * There is out-of-the-box implementation of {@link Target}:
 * {@link HttpTarget} that will play interaction from pacts as http request and check http responses
 * <p>
 * Runner supports:
 * - {@link org.junit.BeforeClass}, {@link org.junit.AfterClass} and {@link org.junit.ClassRule} annotations,
 * that will be run once - before/after whole contract test suite
 * <p>
 * - {@link org.junit.Before}, {@link org.junit.After} and {@link org.junit.Rule} annotations,
 * that will be run before/after each test of interaction
 * <b>WARNING:</b> please note, that only {@link org.junit.rules.TestRule} is possible to use with this runner,
 * i.e. {@link org.junit.rules.MethodRule} <b>IS NOT supported</b>
 * <p>
 * - {@link State} - before each interaction that require state change,
 * all methods annotated by {@link State} with appropriate state listed will be invoked
 */
public class PactRunner extends ParentRunner<InteractionRunner> {
    private final Logger LOGGER = LoggerFactory.getLogger(PactRunner.class);
    private final List<InteractionRunner> child;

    public PactRunner(final Class<?> clazz) throws InitializationError {
        super(clazz);

        final Provider providerInfo = clazz.getAnnotation(Provider.class);
        if (providerInfo == null) {
            throw new InitializationError("Provider name should be specified by using " + Provider.class.getName() + " annotation");
        }
        final String serviceName = providerInfo.value();

      final Consumer consumerInfo = clazz.getAnnotation(Consumer.class);
      final String consumerName = consumerInfo != null ? consumerInfo.value() : null;

      final TestClass testClass = new TestClass(clazz);

        this.child = new ArrayList<>();
        final List<Pact> pacts = new ArrayList<>();
        PactLoader pactLoader = getPactSource(testClass);
        try {
            List<Pact> list = filterPacts(pactLoader.load(serviceName));
            for (final Pact p : list) {
                if ( consumerName == null || p.getConsumer().getName().equals(consumerName)){
                  pacts.add(p);
                }
            }
        } catch (final IOException | JsonException e) {
            throw new InitializationError(e);
        } catch (NoPactsFoundException e) {
          LOGGER.debug("No pacts found", e);
        }

      if (pacts.isEmpty()) {
        if (clazz.isAnnotationPresent(IgnoreNoPactsToVerify.class)) {
          LOGGER.warn("Did not find any pact files for provider " + providerInfo.value());
        } else {
          throw new InitializationError("Did not find any pact files for provider " + providerInfo.value());
        }
      }

      setupInteractionRunners(testClass, pacts, pactLoader);
    }

  protected void setupInteractionRunners(TestClass testClass, List<Pact> pacts, PactLoader pactLoader) throws InitializationError {
    if (pacts != null) {
      for (final Pact pact : pacts) {
        this.child.add(newInteractionRunner(testClass, pact, pactLoader.getPactSource()));
      }
    }
  }

  protected InteractionRunner newInteractionRunner(TestClass testClass, Pact pact, au.com.dius.pact.model.PactSource pactSource) throws InitializationError {
    return new InteractionRunner(testClass, pact, pactSource);
  }

  protected List<Pact> filterPacts(List<Pact> pacts){
        return pacts;
    }

    @Override
    protected List<InteractionRunner> getChildren() {
        return child;
    }

    @Override
    protected Description describeChild(final InteractionRunner child) {
        return child.getDescription();
    }

    @Override
    protected void runChild(final InteractionRunner interaction, final RunNotifier notifier) {
        interaction.run(notifier);
    }

    protected PactLoader getPactSource(final TestClass clazz) throws InitializationError {
        final PactSource pactSource = clazz.getAnnotation(PactSource.class);
        List<Annotation> pactLoaders = new ArrayList<>();
        for(Annotation annotation: clazz.getAnnotations()) {
            if (annotation.annotationType().getAnnotation(PactSource.class) != null) {
                pactLoaders.add(annotation);
            }
        }
        if ((pactSource == null ? 0 : 1) + pactLoaders.size() != 1) {
            throw new InitializationError("Exactly one pact source should be set");
        }
        try {
            if (pactSource != null) {
                final Class<? extends PactLoader> pactLoaderClass = pactSource.value();
                try {
                    // Checks if there is a constructor with one argument of type Class.
                    final Constructor<? extends PactLoader> contructorWithClass = pactLoaderClass.getDeclaredConstructor(Class.class);
                    contructorWithClass.setAccessible(true);
                    return contructorWithClass.newInstance(clazz.getJavaClass());
                } catch(NoSuchMethodException e) {
                    LOGGER.error(e.getMessage(), e);
                    return pactLoaderClass.newInstance();
                }
            } else {
                final Annotation annotation = pactLoaders.iterator().next();
                return annotation.annotationType().getAnnotation(PactSource.class).value()
                  .getConstructor(annotation.annotationType()).newInstance(annotation);
            }
        } catch (final InstantiationException | IllegalAccessException | NoSuchMethodException | InvocationTargetException e) {
            LOGGER.error("Error while creating pact source", e);
            throw new InitializationError(e);
        }
    }
}
