package au.com.dius.pact.provider.junit.sysprops;

import org.junit.Before;
import org.junit.Test;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.is;

public class SystemPropertyResolverTest {

  private SystemPropertyResolver resolver;

  @Before
  public void setup() {
    resolver = new SystemPropertyResolver();
  }

  @Test
  public void returnsTheSystemPropertyWithTheProvidedName() {
    assertThat(resolver.resolveValue("java.version"), is(equalTo(System.getProperty("java.version"))));
    assertThat(resolver.resolveValue("java.version:1234"), is(equalTo(System.getProperty("java.version"))));
  }

  @Test
  public void returnsTheEnvironmentVariableWithTheProvidedNameIfThereIsNoSystemProperty() {
    assertThat(resolver.resolveValue("PATH"), is(equalTo(System.getenv("PATH"))));
    assertThat(resolver.resolveValue("PATH:1234"), is(equalTo(System.getenv("PATH"))));
  }

  @Test(expected = RuntimeException.class)
  public void throwsAnExceptionIfTheNameIsNotResolved() {
    resolver.resolveValue("value.that.should.not.be.found!");
  }

  @Test
  public void defaultsToAnyDefaultValueProvidedIfTheNameIsNotResolved() {
    assertThat(resolver.resolveValue("value.that.should.not.be.found!:defaultValue"), is(equalTo("defaultValue")));
  }

  @Test(expected = RuntimeException.class)
  public void throwsAnExceptionIfTheNameIsNotResolvedAndTheDefaultValueIsEmpty() {
    resolver.resolveValue("value.that.should.not.be.found!:");
  }

}
