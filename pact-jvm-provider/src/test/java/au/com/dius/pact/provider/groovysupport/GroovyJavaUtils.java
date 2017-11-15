package au.com.dius.pact.provider.groovysupport;

import org.apache.http.HttpRequest;

import java.util.concurrent.Callable;
import java.util.function.BiFunction;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.function.Supplier;

public class GroovyJavaUtils {

  private static final String WAS_CALLED = "was called";
  private static final String JAVA_FUNCTION = "Java Function";

  private GroovyJavaUtils() {}

  public static Consumer<HttpRequest> consumerRequestFilter() {
    return request -> request.addHeader("Java Consumer", WAS_CALLED);
  }

  public static Callable<String> callableRequestFilter() {
    return () -> "Java Callable was called";
  }

  public static Function<HttpRequest, HttpRequest> functionRequestFilter() {
    return request -> {
      request.addHeader(JAVA_FUNCTION, WAS_CALLED);
      return request;
    };
  }

  public static BiFunction<HttpRequest, Object, HttpRequest> function2RequestFilter() {
    return (request, other) -> {
      request.addHeader(JAVA_FUNCTION, WAS_CALLED);
      return request;
    };
  }

  public static BiFunction<Object, HttpRequest, HttpRequest> function2RequestFilterWithParametersSwapped() {
    return (other, request) -> {
      request.addHeader(JAVA_FUNCTION, WAS_CALLED);
      return request;
    };
  }

  public static BiFunction<Boolean, Long, Object> invalidFunction2RequestFilter() {
    return (p1, p2) -> null;
  }

  public static Supplier<HttpRequest> supplierRequestFilter() {
    return () -> null;
  }
}
