package au.com.dius.pact.consumer.junit.exampleclients;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.common.net.UrlEscapers;
import org.apache.commons.lang3.StringUtils;
import org.apache.hc.client5.http.fluent.Content;
import org.apache.hc.client5.http.fluent.Request;
import org.apache.hc.client5.http.fluent.Response;
import org.apache.hc.core5.http.ContentType;
import org.apache.hc.core5.http.NameValuePair;
import org.apache.hc.core5.http.message.BasicNameValuePair;
import org.apache.hc.core5.net.URIBuilder;

import java.io.IOException;
import java.net.URISyntaxException;
import java.nio.charset.Charset;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

public class ConsumerClient{
    private static final String TESTREQHEADER = "testreqheader";
    private static final String TESTREQHEADERVALUE = "testreqheadervalue";
    private String url;

    public ConsumerClient(String url) {
        this.url = url;
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
        Content response = Request.get(uriBuilder.toString())
          .addHeader(TESTREQHEADER, TESTREQHEADERVALUE)
          .execute().returnContent();
        return jsonToMap(response.getType() != null ? response.asString() : response.asString(Charset.defaultCharset()));
    }

    private List<NameValuePair> parseQueryString(String queryString) {
        return Arrays.stream(queryString.split("&")).map(s -> s.split("="))
          .map(p -> new BasicNameValuePair(p[0], p[1]))
          .collect(Collectors.toList());
    }

    private String encodePath(String path) {
        return Arrays.asList(path.split("/"))
                .stream().map(UrlEscapers.urlPathSegmentEscaper()::escape).collect(Collectors.joining("/"));
    }

    public List getAsList(String path) throws IOException {
		return jsonToList(Request.get(url + encodePath(path))
                .addHeader(TESTREQHEADER, TESTREQHEADERVALUE)
                .execute().returnContent().asString());
	}

    public Map post(String path, String body, ContentType mimeType) throws IOException {
        String respBody = Request.post(url + encodePath(path))
                .addHeader(TESTREQHEADER, TESTREQHEADERVALUE)
                .bodyString(body, mimeType)
                .execute().returnContent().asString();
        return jsonToMap(respBody);
    }

    private HashMap jsonToMap(String respBody) throws IOException {
        if (respBody.isEmpty()) {
          return new HashMap();
        }
        return new ObjectMapper().readValue(respBody, HashMap.class);
    }
	
	private List jsonToList(String respBody) throws IOException {
		return new ObjectMapper().readValue(respBody, ArrayList.class);		
	}

    public int options(String path) throws IOException {
        return Request.options(url + encodePath(path))
                .addHeader(TESTREQHEADER, TESTREQHEADERVALUE)
                .execute().returnResponse().getCode();
    }

    public String postBody(String path, String body, ContentType mimeType) throws IOException {
        return Request.post(url + encodePath(path))
            .bodyString(body, mimeType)
            .execute().returnContent().asString(Charset.defaultCharset());
    }

    public Map putAsMap(String path, String body) throws IOException {
        String respBody = Request.put(url + encodePath(path))
                .addHeader(TESTREQHEADER, TESTREQHEADERVALUE)
                .bodyString(body, ContentType.APPLICATION_JSON)
                .execute().returnContent().asString(Charset.defaultCharset());
        return jsonToMap(respBody);
    }
}
