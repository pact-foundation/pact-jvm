package au.com.dius.pact.consumer.junit.events;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.apache.http.client.fluent.Content;
import org.apache.http.client.fluent.Request;
import org.apache.http.entity.ContentType;

import java.io.IOException;
import java.util.Arrays;
import java.util.List;
import java.util.Map;

public class EventsRepository {
  private final String baseUrl;

  public EventsRepository(String baseUrl) {
    this.baseUrl = baseUrl;
  }

  public List<Event> getEvents() {
    try {
      ObjectMapper mapper = new ObjectMapper();
      Content content = Request.Post(baseUrl + "/all")
        .bodyString(mapper.writeValueAsString(new EventRequest("asdf")), ContentType.APPLICATION_JSON)
        .setHeader("Accept", ContentType.APPLICATION_JSON.toString())
        .execute().returnContent();
      return Arrays.asList(mapper.readValue(content.asString(), Event[].class));
    } catch (IOException e) {
      throw new RuntimeException(e);
    }
  }

  private static class MapTypeReference extends TypeReference<Map<String, Map<String, List<Event>>>> {}

  public  Map<String, Map<String, List<Event>>> getEventsMapNestedArray() {
    try {
      ObjectMapper mapper = new ObjectMapper();
      Content content = Request.Get(baseUrl + "/dictionaryNestedArray")
        .setHeader("Accept", ContentType.APPLICATION_JSON.toString())
        .execute().returnContent();
      return mapper.readValue(content.asString(), new MapTypeReference());
    } catch (IOException e) {
      throw new RuntimeException(e);
    }
  }
}
