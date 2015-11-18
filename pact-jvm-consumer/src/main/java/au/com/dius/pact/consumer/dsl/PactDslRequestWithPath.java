package au.com.dius.pact.consumer.dsl;

import au.com.dius.pact.consumer.ConsumerPactBuilder;
import au.com.dius.pact.model.Consumer;
import au.com.dius.pact.model.Interaction;
import au.com.dius.pact.model.Provider;
import nl.flotsam.xeger.Xeger;
import org.apache.http.entity.ContentType;
import org.json.JSONObject;
import org.w3c.dom.Document;

import javax.xml.transform.TransformerException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class PactDslRequestWithPath {
    private final ConsumerPactBuilder consumerPactBuilder;

    Consumer consumer;
    Provider provider;

    String state;
    String description;
    String path = "/";
    String requestMethod = "GET";
    Map<String, String> requestHeaders = new HashMap<String, String>();
    String query;
    String requestBody;
    Map<String, Map<String, Object>> requestMatchers = new HashMap<String, Map<String, Object>>();

    public PactDslRequestWithPath(ConsumerPactBuilder consumerPactBuilder,
                                  String consumerName,
                                  String providerName,
                                  String state,
                                  String description,
                                  String path,
                                  String requestMethod,
                                  Map<String, String> requestHeaders,
                                  String query,
                                  String requestBody,
                                  Map<String, Map<String, Object>> requestMatchers) {
        this.consumerPactBuilder = consumerPactBuilder;
        this.requestMatchers = requestMatchers;
        this.consumer = new Consumer(consumerName);
        this.provider = new Provider(providerName);

        this.state = state;

        this.description = description;
        this.path = path;
        this.requestMethod = requestMethod;
        this.requestHeaders = requestHeaders;
        this.query = query;
        this.requestBody = requestBody;
        this.requestMatchers = requestMatchers;
    }

    public PactDslRequestWithPath(ConsumerPactBuilder consumerPactBuilder,
                                  PactDslRequestWithPath existing,
                                  String description) {
        this.consumerPactBuilder = consumerPactBuilder;
        this.consumer = existing.consumer;
        this.provider = existing.provider;
        this.state = existing.state;
        this.description = description;
    }

    /**
     * The HTTP method for the request
     *
     * @param method Valid HTTP method
     */
    public PactDslRequestWithPath method(String method) {
        requestMethod = method;
        return this;
    }

    /**
     * Headers to be included in the request
     *
     * @param headers Key-value pairs
     */
    public PactDslRequestWithPath headers(Map<String, String> headers) {
        requestHeaders.putAll(headers);
        return this;
    }

    /**
     * The query string for the request
     *
     * @param query query string
     */
    public PactDslRequestWithPath query(String query) {
        this.query = query;
        return this;
    }

    /**
     * The body of the request
     *
     * @param body Request body in string form
     */
    public PactDslRequestWithPath body(String body) {
        requestBody = body;
        return this;
    }

    /**
     * The body of the request
     *
     * @param body Request body in string form
     */
    public PactDslRequestWithPath body(String body, String mimeType) {
        requestBody = body;
        requestHeaders.put("Content-Type", mimeType);
        return this;
    }

    /**
     * The body of the request
     *
     * @param body Request body in string form
     */
    public PactDslRequestWithPath body(String body, ContentType mimeType) {
        return body(body, mimeType.toString());
    }

    /**
     * The body of the request
     *
     * @param body Request body in JSON form
     */
    public PactDslRequestWithPath body(JSONObject body) {
        requestBody = body.toString();
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
    public PactDslRequestWithPath body(DslPart body) {
        for (String matcherName : body.matchers.keySet()) {
            requestMatchers.put("$.body" + matcherName, body.matchers.get(matcherName));
        }
        requestBody = body.toString();
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
    public PactDslRequestWithPath body(Document body) throws TransformerException {
        requestBody = ConsumerPactBuilder.xmlToString(body);
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
        this.path = path;
        return this;
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
        this.path = path;
        return this;
    }

    /**
     * Match a request header. A random example header value will be generated from the provided regular expression.
     *
     * @param header Header to match
     * @param regex  Regular expression to match
     */
    public PactDslRequestWithPath matchHeader(String header, String regex) {
        return matchHeader(header, regex, new Xeger(regex).generate());
    }

    /**
     * Match a request header.
     *
     * @param header        Header to match
     * @param regex         Regular expression to match
     * @param headerExample Example value to use
     */
    public PactDslRequestWithPath matchHeader(String header, String regex, String headerExample) {
        HashMap<String, Object> matcher = new HashMap<String, Object>();
        matcher.put("regex", regex);
        requestMatchers.put("$.headers." + header, matcher);
        requestHeaders.put(header, headerExample);
        return this;
    }

    /**
     * Define the response to return
     */
    public PactDslResponse willRespondWith() {
        return new PactDslResponse(consumerPactBuilder, this);
    }
}
