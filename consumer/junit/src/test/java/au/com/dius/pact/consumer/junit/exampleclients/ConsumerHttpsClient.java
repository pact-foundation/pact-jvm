package au.com.dius.pact.consumer.junit.exampleclients;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.common.net.UrlEscapers;
import org.apache.commons.lang3.StringUtils;
import org.apache.http.NameValuePair;
import org.apache.http.client.fluent.InsecureHttpsRequest;
import org.apache.http.client.utils.URIBuilder;
import org.apache.http.entity.ContentType;
import org.apache.http.message.BasicNameValuePair;

import java.io.IOException;
import java.net.URISyntaxException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

public class ConsumerHttpsClient {
    private static final String TESTREQHEADERVALUE = "testreqheadervalue";
    private static final String TESTREQHEADER = "testreqheader";
    private String url;

  public ConsumerHttpsClient(String url) {
    this.url = url.replaceFirst("http:", "https:");
  }

  public Map getAsMap(String path, String queryString) throws IOException {
      URIBuilder uriBuilder;
      try {
          uriBuilder = new URIBuilder(url).setPath(path);
      } catch (URISyntaxException e) {
          throw new RuntimeException(e);
      }
      if (StringUtils.isNotEmpty(queryString)) {
          uriBuilder.setParameters(parseQueryString(queryString));
      }
      return jsonToMap(InsecureHttpsRequest.httpsGet(uriBuilder.toString())
              .addHeader(TESTREQHEADER, TESTREQHEADERVALUE)
              .execute().returnContent().asString());
  }

    private List<NameValuePair> parseQueryString(String queryString) {
        return Arrays.asList(queryString.split("&")).stream().map(s -> s.split("="))
                .map(p -> new BasicNameValuePair(p[0], p[1])).collect(Collectors.toList());
    }

    private String encodePath(String path) {
        return Arrays.asList(path.split("/"))
                .stream().map(UrlEscapers.urlPathSegmentEscaper()::escape).collect(Collectors.joining("/"));
    }

  public List getAsList(String path) throws IOException {
    return jsonToList(InsecureHttpsRequest.httpsGet(url + encodePath(path))
                .addHeader(TESTREQHEADER, TESTREQHEADERVALUE)
                .execute().returnContent().asString());
  }

  public Map post(String path, String body, ContentType mimeType) throws IOException {
      String respBody = InsecureHttpsRequest.httpsPost(url + encodePath(path))
              .addHeader(TESTREQHEADER, TESTREQHEADERVALUE)
              .bodyString(body, mimeType)
              .execute().returnContent().asString();
      return jsonToMap(respBody);
  }

    private HashMap jsonToMap(String respBody) throws IOException {
        return new ObjectMapper().readValue(respBody, HashMap.class);
    }
	
	private List jsonToList(String respBody) throws IOException {
		return new ObjectMapper().readValue(respBody, ArrayList.class);		
	}

  public int options(String path) throws IOException {
      return InsecureHttpsRequest.httpsOptions(url + encodePath(path))
              .addHeader(TESTREQHEADER, TESTREQHEADERVALUE)
              .execute().returnResponse().getStatusLine().getStatusCode();
  }

  public String postBody(String path, String body, ContentType mimeType) throws IOException {
      return InsecureHttpsRequest.httpsPost(url + encodePath(path))
          .bodyString(body, mimeType)
          .execute().returnContent().asString();
  }

  public Map putAsMap(String path, String body) throws IOException {
      String respBody = InsecureHttpsRequest.httpsPut(url + encodePath(path))
              .addHeader(TESTREQHEADER, TESTREQHEADERVALUE)
              .bodyString(body, ContentType.APPLICATION_JSON)
              .execute().returnContent().asString();
      return jsonToMap(respBody);
  }
}
