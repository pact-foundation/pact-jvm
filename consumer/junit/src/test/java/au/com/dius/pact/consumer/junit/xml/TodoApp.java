package au.com.dius.pact.consumer.junit.xml;

import org.apache.hc.client5.http.fluent.Request;

import javax.xml.bind.JAXBContext;
import javax.xml.bind.JAXBException;
import javax.xml.bind.Unmarshaller;
import java.io.IOException;

public class TodoApp {
  private String url;

  public TodoApp setUrl(String url) {
    this.url = url;
    return this;
  }

  public Projects getProjects(String format) throws IOException {
    String contentType = "application/json";
    if (format.equalsIgnoreCase("xml")) {
      contentType = "application/xml";
    }
    return (Projects) Request.get(this.url + "/projects?from=today")
      .addHeader("Accept", contentType)
      .execute().handleResponse(httpResponse -> {
        try {
          JAXBContext jaxbContext = JAXBContext.newInstance(Projects.class);
          Unmarshaller jaxbUnmarshaller = jaxbContext.createUnmarshaller();
          return jaxbUnmarshaller.unmarshal(httpResponse.getEntity().getContent());
        } catch (JAXBException e) {
          throw new IOException(e);
        }
      });
  }
}
