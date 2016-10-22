package au.com.dius.pact.util;

public class Optional<V> {
  private V value = null;

  private Optional(V value) {
    this.value = value;
  }

  private Optional() {
  }

  public boolean isPresent() {
    return value != null;
  }

  public V get() {
    return value;
  }

  public static <V> Optional<V> of(V value) {
    return new Optional<V>(value);
  }

  public static <V> Optional<V> empty() {
    return new Optional<V>();
  }
}
