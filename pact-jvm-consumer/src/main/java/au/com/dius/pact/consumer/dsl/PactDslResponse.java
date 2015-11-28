package au.com.dius.pact.consumer.dsl;

import au.com.dius.pact.consumer.ConsumerPactBuilder;
import au.com.dius.pact.model.RequestResponseInteraction;
import au.com.dius.pact.model.PactFragment;
import au.com.dius.pact.model.PactReader;
import au.com.dius.pact.model.Request;
import au.com.dius.pact.model.Response;
import nl.flotsam.xeger.Xeger;
import org.apache.http.entity.ContentType;
import org.json.JSONObject;
import org.w3c.dom.Document;
import scala.collection.JavaConverters$;

import javax.xml.transform.TransformerException;
import java.util.HashMap;
import java.util.Map;

public class PactDslResponse {
    private final ConsumerPactBuilder consumerPactBuilder;
    private PactDslRequestWithPath request;

    private int responseStatus = 200;
    private Map<String, String> responseHeaders = new HashMap<String, String>();
    private String responseBody;
    private Map<String, Map<String, Object>> responseMatchers = new HashMap<String, Map<String, Object>>();

    public PactDslResponse(ConsumerPactBuilder consumerPactBuilder, PactDslRequestWithPath request) {
        this.consumerPactBuilder = consumerPactBuilder;
        this.request = request;
    }

    /**
     * Response status code
     *
     * @param status HTTP status code
     */
    public PactDslResponse status(int status) {
        this.responseStatus = status;
        return this;
    }

    /**
     * Response headers to return
     *
     * @param headers key-value pairs of headers
     */
    public PactDslResponse headers(Map<String, String> headers) {
        this.responseHeaders.putAll(headers);
        return this;
    }

    /**
     * Response body to return
     *
     * @param body Response body in string form
     */
    public PactDslResponse body(String body) {
        this.responseBody = body;
        return this;
    }

    /**
     * Response body to return
     *
     * @param body body in string form
     */
    public PactDslResponse body(String body, String mimeType) {
        responseBody = body;
        responseHeaders.put("Content-Type", mimeType);
        return this;
    }

    /**
     * Response body to return
     *
     * @param body body in string form
     */
    public PactDslResponse body(String body, ContentType mimeType) {
        return body(body, mimeType.toString());
    }

    /**
     * Response body to return
     *
     * @param body Response body in JSON form
     */
    public PactDslResponse body(JSONObject body) {
        this.responseBody = body.toString();
        if (!responseHeaders.containsKey("Content-Type")) {
            responseHeaders.put("Content-Type", ContentType.APPLICATION_JSON.toString());
        }
        return this;
    }

    /**
     * Response body to return
     *
     * @param body Response body built using the Pact body DSL
     */
    public PactDslResponse body(DslPart body) {
        for (String matcherName : body.matchers.keySet()) {
            responseMatchers.put("$.body" + matcherName, body.matchers.get(matcherName));
        }
        responseBody = body.toString();
        if (!responseHeaders.containsKey("Content-Type")) {
            responseHeaders.put("Content-Type", ContentType.APPLICATION_JSON.toString());
        }
        return this;
    }

    /**
     * Response body to return
     *
     * @param body Response body as an XML Document
     */
    public PactDslResponse body(Document body) throws TransformerException {
        responseBody = ConsumerPactBuilder.xmlToString(body);
        if (!responseHeaders.containsKey("Content-Type")) {
            responseHeaders.put("Content-Type", ContentType.APPLICATION_XML.toString());
        }
        return this;
    }

    /**
     * Match a response header. A random example header value will be generated from the provided regular expression.
     *
     * @param header Header to match
     * @param regexp Regular expression to match
     */
    public PactDslResponse matchHeader(String header, String regexp) {
        return matchHeader(header, regexp, new Xeger(regexp).generate());
    }

    /**
     * Match a response header.
     *
     * @param header        Header to match
     * @param regexp        Regular expression to match
     * @param headerExample Example value to use
     */
    public PactDslResponse matchHeader(String header, String regexp, String headerExample) {
        HashMap<String, Object> matcher = new HashMap<String, Object>();
        matcher.put("regex", regexp);
        responseMatchers.put("$.headers." + header, matcher);
        responseHeaders.put(header, headerExample);
        return this;
    }

    private void addInteraction() {
        consumerPactBuilder.getInteractions().add(new RequestResponseInteraction(
          request.description,
          request.state,
          new Request(request.requestMethod, request.path, PactReader.queryStringToMap(request.query, false),
            request.requestHeaders, request.requestBody, request.requestMatchers),
          new Response(responseStatus, responseHeaders, responseBody, responseMatchers)
        ));
    }

    /**
     * Terminates the DSL and builds a pact fragment to represent the interactions
     *
     * @return
     */
    public PactFragment toFragment() {
        addInteraction();
        return new PactFragment(
                request.consumer,
                request.provider,
                JavaConverters$.MODULE$.asScalaBufferConverter(consumerPactBuilder.getInteractions()).asScala());
    }

    /**
     * Description of the request that is expected to be received
     *
     * @param description request description
     */
    public PactDslRequestWithPath uponReceiving(String description) {
        addInteraction();
        return new PactDslRequestWithPath(consumerPactBuilder, request, description);
    }

    public PactDslWithState given(String state) {
        addInteraction();
        return new PactDslWithState(consumerPactBuilder, request.consumer.getName(), request.provider.getName(), state);
    }
}
