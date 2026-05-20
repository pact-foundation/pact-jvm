package au.com.dius.pact.core.support.json;

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

import java.io.ByteArrayInputStream;
import java.io.StringReader;
import java.nio.charset.StandardCharsets;
import java.util.concurrent.TimeUnit;

@BenchmarkMode(Mode.AverageTime)
@OutputTimeUnit(TimeUnit.MILLISECONDS)
@Warmup(iterations = 3, time = 1)
@Measurement(iterations = 5, time = 1)
@Fork(1)
public class JsonParserBenchmark {

  @State(Scope.Thread)
  public static class BenchmarkState {
    @Param({"user-batch", "catalog", "http-pact", "message-pact"})
    public String payloadType;

    @Param({"10", "50", "100"})
    public int itemCount;

    public String payload;
    public byte[] payloadBytes;

    @Setup(Level.Trial)
    public void setup() {
      payload = switch (payloadType) {
        case "user-batch" -> buildUserBatchPayload(itemCount);
        case "catalog" -> buildCatalogPayload(itemCount);
        case "http-pact" -> buildHttpPactPayload(itemCount);
        case "message-pact" -> buildMessagePactPayload(itemCount);
        default -> throw new IllegalArgumentException("Unknown payload type: " + payloadType);
      };
      payloadBytes = payload.getBytes(StandardCharsets.UTF_8);
    }
  }

  @Benchmark
  public JsonValue parseString(BenchmarkState state) {
    return JsonParser.parseString(state.payload);
  }

  @Benchmark
  public JsonValue parseReader(BenchmarkState state) {
    return JsonParser.parseReader(new StringReader(state.payload));
  }

  @Benchmark
  public JsonValue parseStream(BenchmarkState state) {
    return JsonParser.parseStream(new ByteArrayInputStream(state.payloadBytes));
  }

  private static String buildUserBatchPayload(int itemCount) {
    StringBuilder builder = new StringBuilder(256 + (itemCount * 220));
    builder.append("{\"users\":[");

    for (int index = 0; index < itemCount; index++) {
      if (index > 0) {
        builder.append(',');
      }

      builder
        .append("{\"id\":\"").append(uuidFor(index, 1)).append("\",")
        .append("\"username\":\"user-").append(index).append("\",")
        .append("\"displayName\":\"Benchmark User ").append(index).append("\",")
        .append("\"active\":").append((index & 1) == 0).append(',')
        .append("\"score\":").append(1250 + (index * 7)).append(',')
        .append("\"rating\":").append(decimalFor(index, 11)).append(',')
        .append("\"email\":\"user-").append(index).append("@example.org\",")
        .append("\"tags\":[\"").append(tagFor(index)).append("\",\"group-").append(index % 9).append("\"],")
        .append("\"settings\":{\"theme\":\"").append((index & 1) == 0 ? "dark" : "light")
        .append("\",\"locale\":\"en-AU\",\"newsletter\":").append(index % 3 == 0).append("},")
        .append("\"metadata\":null}");
    }

    builder
      .append("],\"meta\":{\"page\":1,\"count\":").append(itemCount)
      .append(",\"traceId\":\"").append(uuidFor(itemCount, 21)).append("\"}}");
    return builder.toString();
  }

  private static String buildCatalogPayload(int itemCount) {
    StringBuilder builder = new StringBuilder(512 + (itemCount * 420));
    builder.append("{\"catalog\":{\"generatedAt\":\"2026-05-20T00:00:00Z\",\"categories\":[");

    for (int category = 0; category < itemCount; category++) {
      if (category > 0) {
        builder.append(',');
      }

      builder
        .append("{\"id\":\"cat-").append(category).append("\",")
        .append("\"name\":\"Category ").append(category).append("\",")
        .append("\"priority\":").append(category % 5).append(',')
        .append("\"facets\":{\"featured\":").append(category % 2 == 0)
        .append(",\"weight\":").append(decimalFor(category, 17)).append("},")
        .append("\"items\":[");

      for (int item = 0; item < 3; item++) {
        if (item > 0) {
          builder.append(',');
        }

        int sku = (category * 10) + item;
        builder
          .append("{\"sku\":\"SKU-").append(sku).append("\",")
          .append("\"name\":\"Item ").append(category).append('-').append(item).append("\",")
          .append("\"available\":").append((sku & 1) == 0).append(',')
          .append("\"price\":").append(decimalFor(sku, 5)).append(',')
          .append("\"attributes\":{\"colour\":\"").append(colourFor(sku)).append("\",")
          .append("\"size\":\"").append(sizeFor(sku)).append("\"},")
          .append("\"history\":[{\"status\":\"created\",\"at\":\"2026-01-01T00:00:00Z\"},")
          .append("{\"status\":\"updated\",\"at\":\"2026-02-01T00:00:00Z\"}]}");
      }

      builder.append("]}");
    }

    builder.append("]}}");
    return builder.toString();
  }

  private static String buildHttpPactPayload(int itemCount) {
    StringBuilder builder = new StringBuilder(2048 + (itemCount * 1800));
    builder
      .append("{\"consumer\":{\"name\":\"Json Parser Consumer\"},")
      .append("\"provider\":{\"name\":\"Json Parser Provider\"},")
      .append("\"interactions\":[");

    for (int index = 0; index < itemCount; index++) {
      if (index > 0) {
        builder.append(',');
      }

      builder
        .append("{\"key\":\"interaction-").append(index).append("\",")
        .append("\"type\":\"Synchronous/HTTP\",")
        .append("\"description\":\"fetch item ").append(index).append("\",")
        .append("\"providerStates\":[{\"name\":\"item ").append(index).append(" exists\",")
        .append("\"params\":{\"id\":").append(index).append(",\"region\":\"ap-southeast-2\"}}],")
        .append("\"request\":{")
        .append("\"method\":\"GET\",")
        .append("\"path\":\"/items/").append(index).append("\",")
        .append("\"query\":[[\"include\",\"details\"],[\"verbose\",\"true\"]],")
        .append("\"headers\":{\"Accept\":[\"application/json\"],\"X-Trace-Id\":[\"")
        .append(uuidFor(index, 31)).append("\"]}},")
        .append("\"response\":{")
        .append("\"status\":200,")
        .append("\"headers\":{\"Content-Type\":[\"application/json;charset=UTF-8\"]},")
        .append("\"body\":{\"id\":").append(index).append(",")
        .append("\"name\":\"Item ").append(index).append("\",")
        .append("\"price\":").append(decimalFor(index, 23)).append(',')
        .append("\"available\":").append((index & 1) == 0).append(',')
        .append("\"tags\":[\"").append(tagFor(index)).append("\",\"region-au\"],")
        .append("\"attributes\":{\"origin\":\"AU\",\"warehouse\":\"WH-").append(index % 7).append("\"}}},")
        .append("\"matchingRules\":{\"body\":{")
        .append("\"$.body.id\":{\"matchers\":[{\"match\":\"integer\"}]},")
        .append("\"$.body.name\":{\"matchers\":[{\"match\":\"type\"}]},")
        .append("\"$.body.price\":{\"matchers\":[{\"match\":\"decimal\"}]},")
        .append("\"$.body.tags\":{\"combine\":\"AND\",\"matchers\":[{\"min\":2,\"match\":\"type\"}]}}},")
        .append("\"generators\":{\"body\":{")
        .append("\"$.body.id\":{\"type\":\"RandomInt\",\"min\":1,\"max\":999999},")
        .append("\"$.body.name\":{\"type\":\"RandomString\",\"size\":16}}},")
        .append("\"comments\":{\"testname\":\"benchmark-generated\"}}");
    }

    builder
      .append("],\"metadata\":{\"pactSpecification\":{\"version\":\"4.0\"},")
      .append("\"pact-jvm\":{\"version\":\"4.7.0\"}}}");
    return builder.toString();
  }

  private static String buildMessagePactPayload(int itemCount) {
    StringBuilder builder = new StringBuilder(1536 + (itemCount * 1200));
    builder
      .append("{\"consumer\":{\"name\":\"Json Parser Consumer\"},")
      .append("\"provider\":{\"name\":\"Json Parser Provider\"},")
      .append("\"messages\":[");

    for (int index = 0; index < itemCount; index++) {
      if (index > 0) {
        builder.append(',');
      }

      builder
        .append("{\"description\":\"inventory event ").append(index).append("\",")
        .append("\"providerStates\":[{\"name\":\"inventory event ").append(index).append(" ready\"}],")
        .append("\"contents\":{\"eventId\":\"").append(uuidFor(index, 41)).append("\",")
        .append("\"type\":\"inventory.updated\",")
        .append("\"sequence\":").append(index).append(',')
        .append("\"payload\":{\"sku\":\"SKU-").append(index).append("\",")
        .append("\"quantity\":").append(50 + index).append(',')
        .append("\"reserved\":").append(index % 4).append(',')
        .append("\"locations\":[{\"code\":\"MEL\",\"quantity\":").append(10 + index)
        .append("},{\"code\":\"SYD\",\"quantity\":").append(5 + (index % 10)).append("}]},")
        .append("\"notes\":[\"restock scheduled\",\"priority-").append(index % 5).append("\"]},")
        .append("\"metaData\":{\"contentType\":\"application/json\",\"source\":\"benchmark-suite\"},")
        .append("\"matchingRules\":{\"content\":{")
        .append("\"$.payload.quantity\":{\"matchers\":[{\"match\":\"integer\"}]},")
        .append("\"$.payload.locations\":{\"matchers\":[{\"match\":\"type\",\"min\":2}]}}},")
        .append("\"generators\":{\"content\":{\"$.eventId\":{\"type\":\"Uuid\"}}}}");
    }

    builder
      .append("],\"metadata\":{\"pactSpecification\":{\"version\":\"3.0.0\"},")
      .append("\"pact-jvm\":{\"version\":\"4.7.0\"}}}");
    return builder.toString();
  }

  private static String decimalFor(int value, int salt) {
    return ((value * 17) + salt) + "." + ((value + salt) % 100);
  }

  private static String uuidFor(int index, int variant) {
    long suffix = ((long) variant * 10_000L) + index;
    return String.format("00000000-0000-4000-8000-%012d", suffix);
  }

  private static String tagFor(int index) {
    return switch (index % 5) {
      case 0 -> "beta";
      case 1 -> "stable";
      case 2 -> "internal";
      case 3 -> "priority";
      default -> "preview";
    };
  }

  private static String colourFor(int index) {
    return switch (index % 6) {
      case 0 -> "red";
      case 1 -> "green";
      case 2 -> "blue";
      case 3 -> "black";
      case 4 -> "white";
      default -> "orange";
    };
  }

  private static String sizeFor(int index) {
    return switch (index % 4) {
      case 0 -> "S";
      case 1 -> "M";
      case 2 -> "L";
      default -> "XL";
    };
  }
}
