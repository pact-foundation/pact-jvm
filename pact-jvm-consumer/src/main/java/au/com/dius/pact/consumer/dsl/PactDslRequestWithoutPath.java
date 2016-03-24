package au.com.dius.pact.consumer.dsl;

import au.com.dius.pact.consumer.ConsumerPactBuilder;
import au.com.dius.pact.model.OptionalBody;
import nl.flotsam.xeger.Xeger;
import org.apache.http.entity.ContentType;
import org.json.JSONObject;
import org.w3c.dom.Document;

import javax.xml.transform.TransformerException;
import java.util.HashMap;
import java.util.Map;

import static au.com.dius.pact.consumer.ConsumerPactBuilder.xmlToString;

public class PactDslRequestWithoutPath {
    private final ConsumerPactBuilder consumerPactBuilder;
    private PactDslWithState pactDslWithState;
    private String description;
    private String requestMethod;
    private Map<String, String> requestHeaders = new HashMap<String, String>();
    private String query;
    private OptionalBody requestBody = OptionalBody.missing();
    private Map<String, Map<String, Object>> requestMatchers = new HashMap<String, Map<String, Object>>();
    private String consumerName;
    private String providerName;

    public PactDslRequestWithoutPath(ConsumerPactBuilder consumerPactBuilder,
                                     PactDslWithState pactDslWithState,
                                     String description) {
        this.consumerPactBuilder = consumerPactBuilder;
        this.pactDslWithState = pactDslWithState;
        this.description = description;
        this.consumerName = pactDslWithState.consumerName;
        this.providerName = pactDslWithState.providerName;
    }

    /**
     * The HTTP method for the request
     *
     * @param method Valid HTTP method
     */
    public PactDslRequestWithoutPath method(String method) {
        requestMethod = method;
        return this;
    }

    /**
     * Headers to be included in the request
     *
     * @param headers Key-value pairs
     */
    public PactDslRequestWithoutPath headers(Map<String, String> headers) {
        requestHeaders = new HashMap<String, String>(headers);
        return this;
    }

    /**
     * The query string for the request
     *
     * @param query query string
     */
    public PactDslRequestWithoutPath query(String query) {
        this.query = query;
        return this;
    }

    /**
     * The body of the request
     *
     * @param body Request body in string form
     */
    public PactDslRequestWithoutPath body(String body) {
        requestBody = OptionalBody.body(body);
        return this;
    }

    /**
     * The body of the request
     *
     * @param body Request body in string form
     */
    public PactDslRequestWithoutPath body(String body, String mimeType) {
        requestBody = OptionalBody.body(body);
        requestHeaders.put("Content-Type", mimeType);
        return this;
    }

    /**
     * The body of the request
     *
     * @param body Request body in string form
     */
    public PactDslRequestWithoutPath body(String body, ContentType mimeType) {
        return body(body, mimeType.toString());
    }

    /**
     * The body of the request
     *
     * @param body Request body in JSON form
     */
    public PactDslRequestWithoutPath body(JSONObject body) {
        requestBody = OptionalBody.body(body.toString());
        if (!requestHeaders.containsKey("Content-Type")) {
            requestHeaders.put("Content-Type", ContentType.APPLICATION_JSON.toString());
        }
        return this;
    }

    /**
     * The body of the request
     *
     * @param body Built using the Pact body DSL
     */
    public PactDslRequestWithoutPath body(DslPart body) {
        requestMatchers = new HashMap<String, Map<String, Object>>();
        for (String matcherName : body.matchers.keySet()) {
            requestMatchers.put("$.body" + matcherName, body.matchers.get(matcherName));
        }
        requestBody = OptionalBody.body(body.toString());
        if (!requestHeaders.containsKey("Content-Type")) {
            requestHeaders.put("Content-Type", ContentType.APPLICATION_JSON.toString());
        }
        return this;
    }

    /**
     * The body of the request
     *
     * @param body XML Document
     */
    public PactDslRequestWithoutPath body(Document body) throws TransformerException {
        requestBody = OptionalBody.body(xmlToString(body));
        if (!requestHeaders.containsKey("Content-Type")) {
            requestHeaders.put("Content-Type", ContentType.APPLICATION_XML.toString());
        }
        return this;
    }

    /**
     * The path of the request
     *
     * @param path string path
     */
    public PactDslRequestWithPath path(String path) {
        return new PactDslRequestWithPath(consumerPactBuilder, consumerName, providerName, pactDslWithState.state, description, path,
                requestMethod, requestHeaders, query, requestBody, requestMatchers);
    }

    /**
     * The path of the request. This will generate a random path to use when generating requests
     *
     * @param pathRegex string path regular expression to match with
     */
    public PactDslRequestWithPath matchPath(String pathRegex) {
        return matchPath(pathRegex, new Xeger(pathRegex).generate());
    }

    /**
     * The path of the request
     *
     * @param path      string path to use when generating requests
     * @param pathRegex regular expression to use to match paths
     */
    public PactDslRequestWithPath matchPath(String pathRegex, String path) {
        HashMap<String, Object> matcher = new HashMap<String, Object>();
        matcher.put("regex", pathRegex);
        requestMatchers.put("$.path", matcher);
        return new PactDslRequestWithPath(consumerPactBuilder, consumerName, providerName, pactDslWithState.state, description, path,
                requestMethod, requestHeaders, query, requestBody, requestMatchers);
    }
}
