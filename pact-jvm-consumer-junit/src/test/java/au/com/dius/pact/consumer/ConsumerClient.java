package au.com.dius.pact.consumer;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.common.net.UrlEscapers;
import org.apache.commons.lang3.StringUtils;
import org.apache.http.NameValuePair;
import org.apache.http.client.fluent.Request;
import org.apache.http.client.utils.URIBuilder;
import org.apache.http.client.utils.URLEncodedUtils;
import org.apache.http.entity.ContentType;
import org.apache.http.message.BasicNameValuePair;

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
        return jsonToMap(Request.Get(uriBuilder.toString())
                .addHeader("testreqheader", "testreqheadervalue")
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
		return jsonToList(Request.Get(url + encodePath(path))
                .addHeader("testreqheader", "testreqheadervalue")
                .execute().returnContent().asString());
	}

    public Map post(String path, String body, ContentType mimeType) throws IOException {
        String respBody = Request.Post(url + encodePath(path))
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
        return Request.Options(url + encodePath(path))
                .addHeader("testreqheader", "testreqheadervalue")
                .execute().returnResponse().getStatusLine().getStatusCode();
    }

    public String postBody(String path, String body, ContentType mimeType) throws IOException {
        return Request.Post(url + encodePath(path))
            .bodyString(body, mimeType)
            .execute().returnContent().asString();
    }

    public Map putAsMap(String path, String body) throws IOException {
        String respBody = Request.Put(url + encodePath(path))
                .addHeader("testreqheader", "testreqheadervalue")
                .bodyString(body, ContentType.APPLICATION_JSON)
                .execute().returnContent().asString();
        return jsonToMap(respBody);
    }
}
