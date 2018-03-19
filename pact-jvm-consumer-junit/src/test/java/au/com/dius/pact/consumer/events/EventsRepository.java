package au.com.dius.pact.consumer.events;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.reflect.TypeToken;
import org.apache.http.client.ClientProtocolException;
import org.apache.http.client.fluent.Content;
import org.apache.http.client.fluent.Request;
import org.apache.http.entity.ContentType;

import java.io.IOException;
import java.lang.reflect.Type;
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
      Gson gson = new Gson();
      Content content = Request.Post(baseUrl + "/all")
        .bodyString(gson.toJson(new EventRequest("asdf")), ContentType.APPLICATION_JSON)
        .setHeader("Accept", ContentType.APPLICATION_JSON.toString())
        .execute().returnContent();
      return Arrays.asList(gson.fromJson(content.asString(), Event[].class));
    } catch (IOException e) {
      throw new RuntimeException(e);
    }
  }

//  public Map<String, Event> getEventsMap() {
//
//    try {
//      Client client = ClientBuilder.newClient().register(LoggingFilter.class);
//
//      Response response = client.target(baseUrl + "/dictionary").request(MediaType.APPLICATION_JSON_TYPE).get(Response.class);
//      if (response.getStatus() == Response.Status.OK.getStatusCode()) {
//        Map<String, Event> events = response.readEntity(new GenericType<Map<String, Event>>() {
//        });
//        return events;
//      } else {
//        throw new RuntimeException("failed to get events as dictionary. status code was " + response.getStatus());
//      }
//    } catch (WebApplicationException e) {
//      throw e; //TODO handle correctly
//    }
//  }
//
//  public Map<String, List<Event>> getEventsMapArray() {
//
//    try {
//      Client client = ClientBuilder.newClient().register(LoggingFilter.class);
//
//      Response response =
//        client.target(baseUrl + "/dictionaryArray").request(MediaType.APPLICATION_JSON_TYPE).get(Response.class);
//      if (response.getStatus() == Response.Status.OK.getStatusCode()) {
//        Map<String, List<Event>> events = response.readEntity(new GenericType<Map<String, List<Event>>>() {
//        });
//        return events;
//      } else {
//        throw new RuntimeException("failed to get events as map array. status code was " + response.getStatus());
//      }
//    } catch (WebApplicationException e) {
//      throw e; //TODO handle correctly
//    }
//  }

  public  Map<String, Map<String, List<Event>>> getEventsMapNestedArray() {
    try {
      Gson gson = new Gson();
      Content content = Request.Get(baseUrl + "/dictionaryNestedArray")
        .setHeader("Accept", ContentType.APPLICATION_JSON.toString())
        .execute().returnContent();
      Type collectionType = new TypeToken<Map<String, Map<String, List<Event>>>>(){}.getType();
      return gson.fromJson(content.asString(), collectionType);
    } catch (IOException e) {
      throw new RuntimeException(e);
    }
  }

//  public  int getPrimitive() {
//
//    try {
//      Client client = ClientBuilder.newClient().register(LoggingFilter.class);
//
//      Response response =
//        client.target(baseUrl + "/primitive").request(MediaType.APPLICATION_JSON_TYPE).get(Response.class);
//      if (response.getStatus() == Response.Status.OK.getStatusCode()) {
//        int num = response.readEntity(Integer.class);
//        return num;
//      } else {
//        throw new RuntimeException("failed to get primitive. status code was " + response.getStatus());
//      }
//    } catch (WebApplicationException e) {
//      throw e; //TODO handle correctly
//    }
//  }
}
