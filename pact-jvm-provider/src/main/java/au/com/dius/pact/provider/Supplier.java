package au.com.dius.pact.provider;

public interface Supplier<T> {
  /**
   * Gets a result.
   *
   * @return a result
   */
  T get();
}
