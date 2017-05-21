package au.com.dius.pact.util;

import org.apache.commons.collections4.Closure;

import java.util.List;

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

  public static <V> Optional<V> ofNullable(V value) {
    return value == null ? (Optional<V>) empty() : of(value);
  }

  public V orElseThrow(RuntimeException exception) {
    if (value == null) {
      throw exception;
    }
    return value;
  }

  public V orElse(V other) {
    return value == null ? other : value;
  }
}
