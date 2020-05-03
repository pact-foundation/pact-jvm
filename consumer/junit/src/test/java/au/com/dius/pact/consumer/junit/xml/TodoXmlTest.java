package au.com.dius.pact.consumer.junit.xml;

import au.com.dius.pact.consumer.dsl.PactDslWithProvider;
import au.com.dius.pact.consumer.junit.PactProviderRule;
import au.com.dius.pact.consumer.junit.PactVerification;
import au.com.dius.pact.consumer.xml.PactXmlBuilder;
import au.com.dius.pact.core.model.RequestResponsePact;
import au.com.dius.pact.core.model.annotations.Pact;
import org.apache.commons.collections4.MapUtils;
import org.junit.Rule;
import org.junit.Test;

import java.io.IOException;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

import static au.com.dius.pact.consumer.dsl.Matchers.bool;
import static au.com.dius.pact.consumer.dsl.Matchers.integer;
import static au.com.dius.pact.consumer.dsl.Matchers.string;
import static au.com.dius.pact.consumer.dsl.Matchers.timestamp;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.greaterThan;
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.isEmptyString;
import static org.hamcrest.Matchers.not;

public class TodoXmlTest {
  @Rule
  public PactProviderRule provider = new PactProviderRule("TodoProvider", "localhost", 1234, this);

  // body: <?xml version="1.0" encoding="UTF-8"?>
  //     <projects foo="bar">
  //       <project id="1" name="Project 1" due="2016-02-11T09:46:56.023Z">
  //         <tasks>
  //           <task id="1" name="Do the laundry" done="true"/>
  //           <task id="2" name="Do the dishes" done="false"/>
  //           <task id="3" name="Do the backyard" done="false"/>
  //           <task id="4" name="Do nothing" done="false"/>
  //         </tasks>
  //       </project>
  //       <project/>
  //     </projects>

  @Pact(provider = "TodoProvider", consumer = "TodoConsumer")
  public RequestResponsePact projects(PactDslWithProvider builder) {
    return builder
    .given("i have a list of projects")
    .uponReceiving("a request for projects in XML")
      .path("/projects")
      .query("from=today")
      .headers(mapOf("Accept", "application/xml"))
    .willRespondWith()
      .headers(mapOf("Content-Type", "application/xml"))
      .status(200)
    .body(
        new PactXmlBuilder("projects", "http://some.namespace/and/more/stuff").build(root -> {
          root.setAttributes(mapOf("id", "1234"));
          root.eachLike("project", 2, mapOf(
            "id", integer(),
            "type", "activity",
            "name", string("Project 1"),
            "due", timestamp("yyyy-MM-dd'T'HH:mm:ss.SSSX", "2016-02-11T09:46:56.023Z")
          ), project -> {
            project.appendElement("tasks", Collections.emptyMap(), task -> {
              task.eachLike("task", 5, mapOf(
                "id", integer(),
                "name", string("Task 1"),
                "done", bool(true)
              ));
            });
          });
        })
    )
    .toPact();
  }

  @PactVerification("TodoProvider")
  @Test
  public void testGeneratesAListOfTODOsForTheMainScreen() throws IOException {
    Projects projects = new TodoApp()
      .setUrl(provider.getMockServer().getUrl())
      .getProjects("xml");
    assertThat(projects.getId(), is("1234"));
    assertThat(projects.getProjects(), hasSize(2));
    projects.getProjects().forEach(project -> {
      assertThat(project.getId(), is(greaterThan(0)));
      assertThat(project.getType(), is("activity"));
      assertThat(project.getName(), is("Project 1"));
      assertThat(project.getDue(), not(isEmptyString()));
      assertThat(project.getTasks().getTasks(), hasSize(5));
    });
  }

  private <T> Map<String, T> mapOf(String key, T value) {
    return MapUtils.putAll(new HashMap<>(), new Object[] { key, value });
  }

  private Map<String, Object> mapOf(String key1, Object value1, String key2, Object value2) {
    return MapUtils.putAll(new HashMap<>(), new Object[] { key1, value1, key2, value2 });
  }

  private Map<String, Object> mapOf(String key1, Object value1, String key2, Object value2, String key3, Object value3) {
    return MapUtils.putAll(new HashMap<>(), new Object[] { key1, value1, key2, value2, key3, value3 });
  }

  private Map<String, Object> mapOf(String key1, Object value1, String key2, Object value2, String key3, Object value3,
    String key4, Object value4) {
    return MapUtils.putAll(new HashMap<>(), new Object[] { key1, value1, key2, value2, key3, value3, key4, value4 });
  }
}
