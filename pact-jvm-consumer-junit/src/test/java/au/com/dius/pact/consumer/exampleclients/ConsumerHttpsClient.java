package au.com.dius.pact.consumer.exampleclients;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.common.escape.Escaper;
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
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class ConsumerHttpsClient {
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
      return jsonToMap(InsecureHttpsRequest.Get(uriBuilder.toString())
              .addHeader("testreqheader", "testreqheadervalue")
              .execute().returnContent().asString());
  }

    private List<NameValuePair> parseQueryString(String queryString) {
      ArrayList<NameValuePair> queryParameters = new ArrayList<NameValuePair>();
      for (String query: queryString.split("&")) {
        String[] keyValue = query.split("=");
        queryParameters.add(new BasicNameValuePair(keyValue[0], keyValue[1]));
      }
      return queryParameters;
    }

    private String encodePath(String path) {
      List<String> pathFragments = new ArrayList<String>();
      Escaper escaper = UrlEscapers.urlPathSegmentEscaper();
      for (String pathFragment: path.split("/")) {
        pathFragments.add(escaper.escape(pathFragment));
      }
      return StringUtils.join(pathFragments, "/");
    }

  public List getAsList(String path) throws IOException {
    return jsonToList(InsecureHttpsRequest.Get(url + encodePath(path))
                .addHeader("testreqheader", "testreqheadervalue")
                .execute().returnContent().asString());
  }

  public Map post(String path, String body, ContentType mimeType) throws IOException {
      String respBody = InsecureHttpsRequest.Post(url + encodePath(path))
              .addHeader("testreqheader", "testreqheadervalue")
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
      return InsecureHttpsRequest.Options(url + encodePath(path))
              .addHeader("testreqheader", "testreqheadervalue")
              .execute().returnResponse().getStatusLine().getStatusCode();
  }

  public String postBody(String path, String body, ContentType mimeType) throws IOException {
      return InsecureHttpsRequest.Post(url + encodePath(path))
          .bodyString(body, mimeType)
          .execute().returnContent().asString();
  }

  public Map putAsMap(String path, String body) throws IOException {
      String respBody = InsecureHttpsRequest.Put(url + encodePath(path))
              .addHeader("testreqheader", "testreqheadervalue")
              .bodyString(body, ContentType.APPLICATION_JSON)
              .execute().returnContent().asString();
      return jsonToMap(respBody);
  }
}
