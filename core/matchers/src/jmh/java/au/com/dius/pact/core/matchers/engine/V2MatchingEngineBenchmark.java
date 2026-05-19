package au.com.dius.pact.core.matchers.engine;

import au.com.dius.pact.core.model.Consumer;
import au.com.dius.pact.core.model.ContentType;
import au.com.dius.pact.core.model.HttpRequest;
import au.com.dius.pact.core.model.HttpResponse;
import au.com.dius.pact.core.model.OptionalBody;
import au.com.dius.pact.core.model.Provider;
import au.com.dius.pact.core.model.V4Interaction;
import au.com.dius.pact.core.model.V4Pact;
import au.com.dius.pact.core.model.generators.Generators;
import au.com.dius.pact.core.model.matchingrules.MatchingRuleCategory;
import au.com.dius.pact.core.model.matchingrules.MatchingRulesImpl;
import au.com.dius.pact.core.model.matchingrules.MinTypeMatcher;
import au.com.dius.pact.core.model.matchingrules.RegexMatcher;
import au.com.dius.pact.core.model.matchingrules.TypeMatcher;
import org.openjdk.jmh.annotations.Benchmark;
import org.openjdk.jmh.annotations.BenchmarkMode;
import org.openjdk.jmh.annotations.Fork;
import org.openjdk.jmh.annotations.Level;
import org.openjdk.jmh.annotations.Measurement;
import org.openjdk.jmh.annotations.Mode;
import org.openjdk.jmh.annotations.OutputTimeUnit;
import org.openjdk.jmh.annotations.Param;
import org.openjdk.jmh.annotations.Scope;
import org.openjdk.jmh.annotations.Setup;
import org.openjdk.jmh.annotations.State;
import org.openjdk.jmh.annotations.Warmup;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;

@BenchmarkMode(Mode.AverageTime)
@OutputTimeUnit(TimeUnit.MILLISECONDS)
@Warmup(iterations = 3, time = 1)
@Measurement(iterations = 5, time = 1)
@Fork(1)
public class V2MatchingEngineBenchmark {
  private static final String UUID_REGEX =
    "[0-9a-f]{8}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{12}";

  @State(Scope.Thread)
  public static class BenchmarkState {
    @Param({"10", "50", "100"})
    public int itemCount;

    public HttpResponse expectedResponse;
    public HttpResponse actualResponse;
    public PlanMatchingContext context;
    public ExecutionPlan responsePlan;

    @Setup(Level.Trial)
    public void setup() {
      expectedResponse = buildExpectedResponse(itemCount);
      actualResponse = buildActualResponse(itemCount);

      V4Pact pact = new V4Pact(new Consumer("benchmark-consumer"), new Provider("benchmark-provider"));
      V4Interaction.SynchronousHttp interaction = new V4Interaction.SynchronousHttp(
        "V2 matching engine benchmark",
        List.of(),
        new HttpRequest(),
        expectedResponse,
        null
      );
      MatchingConfiguration configuration = new MatchingConfiguration(false, false, false, false, false);
      context = new PlanMatchingContext(pact, interaction, configuration);
      responsePlan = V2MatchingEngine.INSTANCE.buildResponsePlan(expectedResponse, context);
    }
  }

  @Benchmark
  public int buildResponsePlan(BenchmarkState state) {
    return V2MatchingEngine.INSTANCE.buildResponsePlan(state.expectedResponse, state.context)
      .getPlanRoot()
      .getChildren()
      .size();
  }

  @Benchmark
  public boolean executeResponsePlan(BenchmarkState state) {
    ExecutionPlan executedPlan = V2MatchingEngine.INSTANCE.executeResponsePlan(
      state.responsePlan,
      state.actualResponse,
      state.context
    );
    return executedPlan.intoResponseMatchResult().matchedOk();
  }

  @Benchmark
  public boolean buildAndExecuteResponsePlan(BenchmarkState state) {
    ExecutionPlan plan = V2MatchingEngine.INSTANCE.buildResponsePlan(state.expectedResponse, state.context);
    ExecutionPlan executedPlan = V2MatchingEngine.INSTANCE.executeResponsePlan(plan, state.actualResponse, state.context);
    return executedPlan.intoResponseMatchResult().matchedOk();
  }

  private static HttpResponse buildExpectedResponse(int itemCount) {
    MatchingRulesImpl matchingRules = new MatchingRulesImpl();
    MatchingRuleCategory bodyRules = matchingRules.addCategory("body");
    bodyRules.addRule("$.users", new MinTypeMatcher(itemCount));
    bodyRules.addRule("$.users[*].id", new RegexMatcher(UUID_REGEX));
    bodyRules.addRule("$.users[*].name", TypeMatcher.INSTANCE);
    bodyRules.addRule("$.users[*].active", TypeMatcher.INSTANCE);
    bodyRules.addRule("$.users[*].score", TypeMatcher.INSTANCE);
    bodyRules.addRule("$.users[*].tags", new MinTypeMatcher(2));
    bodyRules.addRule("$.users[*].tags[*]", TypeMatcher.INSTANCE);
    bodyRules.addRule("$.users[*].address.line1", TypeMatcher.INSTANCE);
    bodyRules.addRule("$.users[*].address.postcode", new RegexMatcher("\\d{4}"));
    bodyRules.addRule("$.meta.page", TypeMatcher.INSTANCE);
    bodyRules.addRule("$.meta.count", TypeMatcher.INSTANCE);
    bodyRules.addRule("$.meta.traceId", new RegexMatcher(UUID_REGEX));

    return new HttpResponse(
      200,
      jsonHeaders(),
      OptionalBody.body(buildResponseBody(itemCount, 0), new ContentType("application/json")),
      matchingRules,
      new Generators()
    );
  }

  private static HttpResponse buildActualResponse(int itemCount) {
    return new HttpResponse(
      200,
      jsonHeaders(),
      OptionalBody.body(buildResponseBody(itemCount, 7), new ContentType("application/json")),
      new MatchingRulesImpl(),
      new Generators()
    );
  }

  private static Map<String, List<String>> jsonHeaders() {
    Map<String, List<String>> headers = new LinkedHashMap<>();
    headers.put("Content-Type", List.of("application/json"));
    return headers;
  }

  private static String buildResponseBody(int itemCount, int variant) {
    StringBuilder builder = new StringBuilder(512 + itemCount * 220);
    builder.append("{\"users\":[");

    for (int index = 0; index < itemCount; index++) {
      if (index > 0) {
        builder.append(',');
      }

      int postcode = 2000 + ((index + variant) % 7000);
      builder
        .append("{\"id\":\"").append(uuidFor(index, variant)).append("\",")
        .append("\"name\":\"user-").append(variant).append('-').append(index).append("\",")
        .append("\"active\":").append(((index + variant) & 1) == 0).append(',')
        .append("\"score\":").append(1000 + variant + (index * 3)).append(',')
        .append("\"tags\":[\"tag-").append(index % 11).append("\",\"group-").append((index + variant) % 7).append("\"],")
        .append("\"address\":{\"line1\":\"").append(100 + index).append(" Benchmark Street\",")
        .append("\"postcode\":\"").append(pad4(postcode)).append("\"}}");
    }

    builder
      .append("],\"meta\":{\"page\":1,\"count\":").append(itemCount)
      .append(",\"traceId\":\"").append(uuidFor(itemCount, variant + 13)).append("\"}}");

    return builder.toString();
  }

  private static String uuidFor(int index, int variant) {
    long suffix = ((long) variant * 10_000L) + index;
    return String.format("00000000-0000-4000-8000-%012d", suffix);
  }

  private static String pad4(int value) {
    if (value >= 1000) {
      return Integer.toString(value);
    }
    if (value >= 100) {
      return "0" + value;
    }
    if (value >= 10) {
      return "00" + value;
    }
    return "000" + value;
  }
}
