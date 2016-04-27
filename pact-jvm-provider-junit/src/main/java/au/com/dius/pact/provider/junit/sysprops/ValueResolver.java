package au.com.dius.pact.provider.junit.sysprops;

public interface ValueResolver {
  String resolveValue(String expression);
  boolean propertyDefined(String property);
}
