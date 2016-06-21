package au.com.dius.pact.provider.groovysupport;

import org.apache.http.HttpRequest;

import java.util.function.BiFunction;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.function.Supplier;

public class GroovyJavaUtils {

  private GroovyJavaUtils() {}

  public static Consumer<HttpRequest> consumerRequestFilter() {
    return request -> request.addHeader("Java Consumer", "was called");
  }

  public static Function<HttpRequest, HttpRequest> functionRequestFilter() {
    return request -> {
      request.addHeader("Java Function", "was called");
      return request;
    };
  }

  public static BiFunction<HttpRequest, Object, HttpRequest> function2RequestFilter() {
    return (request, other) -> {
      request.addHeader("Java Function", "was called");
      return request;
    };
  }

  public static BiFunction<Object, HttpRequest, HttpRequest> function2RequestFilterWithParametersSwapped() {
    return (other, request) -> {
      request.addHeader("Java Function", "was called");
      return request;
    };
  }

  public static BiFunction<Boolean, Long, Object> invalidFunction2RequestFilter() {
    return (p1, p2) -> { return null; };
  }

  public static Supplier<HttpRequest> supplierRequestFilter() {
    return () -> { return null; };
  }
}
