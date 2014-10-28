package au.com.dius.pact.consumer;

import au.com.dius.pact.model.Consumer;
import au.com.dius.pact.model.Interaction;
import au.com.dius.pact.model.Interaction$;
import au.com.dius.pact.model.PactFragment;
import au.com.dius.pact.model.Provider;
import au.com.dius.pact.model.Request$;
import au.com.dius.pact.model.Response$;
import org.apache.http.entity.ContentType;
import org.json.JSONObject;
import org.w3c.dom.Document;
import scala.None$;
import scala.Some$;
import scala.collection.JavaConverters$;

import javax.xml.transform.OutputKeys;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerConfigurationException;
import javax.xml.transform.TransformerException;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stream.StreamResult;
import java.io.StringWriter;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class ConsumerPactBuilder {

    private String consumerName;

    public ConsumerPactBuilder(String consumer) {
        this.consumerName = consumer;
    }

    /**
     * Name the consumer of the pact
     * @param consumer Consumer name
     */
    public static ConsumerPactBuilder consumer(String consumer) {
        return new ConsumerPactBuilder(consumer);
    }

    /**
     * Name the provider that the consumer has a pact with
     * @param provider provider name
     */
    public PactDslWithProvider hasPactWith(String provider) {
        return new PactDslWithProvider(provider);
    }

    public class PactDslWithProvider {
        private String providerName;

        public PactDslWithProvider(String provider) {
            this.providerName = provider;
        }

        /**
         * Describe the state the provider needs to be in for the pact test to be verified.
         * @param state Provider state
         */
        public PactDslWithState given(String state) {
            return new PactDslWithState(state);
        }

        /**
         * Description of the request that is expected to be received
         * @param description request description
         */
        public PactDslWithState.PactDslRequestWithoutPath uponReceiving(String description) {
            return new PactDslWithState(null).uponReceiving(description);
        }

        public class PactDslWithState {
            private String state;

            public PactDslWithState(String state) {
                this.state = state;
            }

            /**
             * Description of the request that is expected to be received
             * @param description request description
             */
            public PactDslRequestWithoutPath uponReceiving(String description) {
                return new PactDslRequestWithoutPath(description);
            }

            public class PactDslRequestWithoutPath {
                private String description;
                private String requestMethod;
                private Map<String, String> requestHeaders = new HashMap<String, String>();
                private String query;
                private String requestBody;
                private Map<String, Object> requestMatchers = new HashMap<String, Object>();

                public PactDslRequestWithoutPath(String description) {
                    this.description = description;
                }

                /**
                 * The HTTP method for the request
                 * @param method Valid HTTP method
                 */
                public PactDslRequestWithoutPath method(String method) {
                    requestMethod = method;
                    return this;
                }

                /**
                 * Headers to be included in the request
                 * @param headers Key-value pairs
                 */
                public PactDslRequestWithoutPath headers(Map<String, String> headers) {
                    requestHeaders = new HashMap<String, String>(headers);
                    return this;
                }

                /**
                 * The query string for the request
                 * @param query query string
                 */
                public PactDslRequestWithoutPath query(String query) {
                    this.query = query;
                    return this;
                }

                /**
                 * The body of the request
                 * @param body Request body in string form
                 */
                public PactDslRequestWithoutPath body(String body) {
                    requestBody = body;
                    return this;
                }

                /**
                 * The body of the request
                 * @param body Request body in string form
                 */
                public PactDslRequestWithoutPath body(String body, String mimeType) {
                    requestBody = body;
                    requestHeaders.put("Content-Type", mimeType);
                    return this;
                }

                /**
                 * The body of the request
                 * @param body Request body in string form
                 */
                public PactDslRequestWithoutPath body(String body, ContentType mimeType) {
                    return body(body, mimeType.toString());
                }

                /**
                 * The body of the request
                 * @param body Request body in JSON form
                 */
                public PactDslRequestWithoutPath body(JSONObject body) {
                    requestBody = body.toString();
                    if (!requestHeaders.containsKey("Content-Type")) {
                        requestHeaders.put("Content-Type", ContentType.APPLICATION_JSON.toString());
                    }
                    return this;
                }

                /**
                 * The body of the request
                 * @param body Built using the Pact body DSL
                 */
                public PactDslRequestWithoutPath body(PactDslJsonBody body) {
                    requestMatchers = body.getMatchers();
                    requestBody = body.toString();
                    if (!requestHeaders.containsKey("Content-Type")) {
                        requestHeaders.put("Content-Type", ContentType.APPLICATION_JSON.toString());
                    }
                    return this;
                }

                /**
                 * The body of the request
                 * @param body XML Document
                 */
                public PactDslRequestWithoutPath body(Document body) throws TransformerException {
                    requestBody = xmlToString(body);
                    if (!requestHeaders.containsKey("Content-Type")) {
                        requestHeaders.put("Content-Type", ContentType.APPLICATION_XML.toString());
                    }
                    return this;
                }

                /**
                 * The path of the request
                 * @param path string path
                 */
                public PactDslRequestWithPath path(String path) {
                    return new PactDslRequestWithPath(consumerName, providerName, state, description, path,
                        requestMethod, requestHeaders, query, requestBody, requestMatchers);
                }
            }
        }
    }

    public class PactDslRequestWithPath {
        private Consumer consumer;
        private Provider provider;

        private String state;

        private String description;
        private String path = "/";
        private String requestMethod = "GET";
        private Map<String, String> requestHeaders = new HashMap<String, String>();
        private String query;
        private String requestBody;
        private Map<String, Object> requestMatchers = new HashMap<String, Object>();

        private List<Interaction> interactions = new ArrayList<Interaction>();

        public PactDslRequestWithPath(String consumerName,
                                      String providerName,
                                      String state,
                                      String description,
                                      String path,
                                      String requestMethod,
                                      Map<String, String> requestHeaders,
                                      String query,
                                      String requestBody,
                                      Map<String, Object> requestMatchers) {
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

        public PactDslRequestWithPath(PactDslRequestWithPath existing, String description) {
            this.consumer = existing.consumer;
            this.provider = existing.provider;
            this.state = existing.state;
            this.description = description;
            this.interactions = existing.interactions;
        }

        /**
         * The HTTP method for the request
         * @param method Valid HTTP method
         */
        public PactDslRequestWithPath method(String method) {
            requestMethod = method;
            return this;
        }

        /**
         * Headers to be included in the request
         * @param headers Key-value pairs
         */
        public PactDslRequestWithPath headers(Map<String, String> headers) {
            requestHeaders = new HashMap<String, String>(headers);
            return this;
        }

        /**
         * The query string for the request
         * @param query query string
         */
        public PactDslRequestWithPath query(String query) {
            this.query = query;
            return this;
        }

        /**
         * The body of the request
         * @param body Request body in string form
         */
        public PactDslRequestWithPath body(String body) {
            requestBody = body;
            return this;
        }

        /**
         * The body of the request
         * @param body Request body in string form
         */
        public PactDslRequestWithPath body(String body, String mimeType) {
            requestBody = body;
            requestHeaders.put("Content-Type", mimeType);
            return this;
        }

        /**
         * The body of the request
         * @param body Request body in string form
         */
        public PactDslRequestWithPath body(String body, ContentType mimeType) {
            return body(body, mimeType.toString());
        }

        /**
         * The body of the request
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
         * @param body Built using the Pact body DSL
         */
        public PactDslRequestWithPath body(DslPart body) {
            requestMatchers = body.getMatchers();
            requestBody = body.toString();
            if (!requestHeaders.containsKey("Content-Type")) {
                requestHeaders.put("Content-Type", ContentType.APPLICATION_JSON.toString());
            }
            return this;
        }

        /**
         * The body of the request
         * @param body XML Document
         */
        public PactDslRequestWithPath body(Document body) throws TransformerException {
            requestBody = xmlToString(body);
            if (!requestHeaders.containsKey("Content-Type")) {
                requestHeaders.put("Content-Type", ContentType.APPLICATION_XML.toString());
            }
            return this;
        }

        /**
         * The path of the request
         * @param path string path
         */
        public PactDslRequestWithPath path(String path) {
            this.path = path;
            return this;
        }

        /**
         * Define the response to return
         */
        public PactDslResponse willRespondWith() {
            return new PactDslResponse(this);
        }

    }

    public class PactDslResponse {
        private PactDslRequestWithPath existing;

        private int responseStatus = 200;
        private Map<String, String> responseHeaders = new HashMap<String, String>();
        private String responseBody;
        private Map<String, Object> responseMatchers = new HashMap<String, Object>();

        public PactDslResponse(PactDslRequestWithPath existing) {
            this.existing = existing;
        }

        /**
         * Response status code
         * @param status HTTP status code
         */
        public PactDslResponse status(int status) {
            this.responseStatus = status;
            return this;
        }

        /**
         * Response headers to return
         * @param headers key-value pairs of headers
         */
        public PactDslResponse headers(Map<String, String> headers) {
            this.responseHeaders = new HashMap<String, String>(headers);
            return this;
        }

        /**
         * Response body to return
         * @param body Response body in string form
         */
        public PactDslResponse body(String body) {
            this.responseBody = body;
            return this;
        }

        /**
         * Response body to return
         * @param body body in string form
         */
        public PactDslResponse body(String body, String mimeType) {
            responseBody = body;
            responseHeaders.put("Content-Type", mimeType);
            return this;
        }

        /**
         * Response body to return
         * @param body body in string form
         */
        public PactDslResponse body(String body, ContentType mimeType) {
            return body(body, mimeType.toString());
        }

        /**
         * Response body to return
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
         * @param body Response body built using the Pact body DSL
         */
        public PactDslResponse body(DslPart body) {
            responseMatchers = body.getMatchers();
            responseBody = body.toString();
            if (!responseHeaders.containsKey("Content-Type")) {
                responseHeaders.put("Content-Type", ContentType.APPLICATION_JSON.toString());
            }
            return this;
        }

        /**
         * Response body to return
         * @param body Response body as an XML Document
         */
        public PactDslResponse body(Document body) throws TransformerException {
            responseBody = xmlToString(body);
            if (!responseHeaders.containsKey("Content-Type")) {
                responseHeaders.put("Content-Type", ContentType.APPLICATION_XML.toString());
            }
            return this;
        }

        private void addInteraction() {
            Interaction currentInteraction;
            if (existing.state == null) {
                currentInteraction = Interaction$.MODULE$.apply(
                        existing.description,
                        None$.apply(existing.state),
                        Request$.MODULE$.apply(existing.requestMethod, existing.path, existing.query,  existing.requestHeaders,
                                existing.requestBody, existing.requestMatchers),
                        Response$.MODULE$.apply(responseStatus, responseHeaders, responseBody, responseMatchers)
                );
            } else {
                currentInteraction = Interaction$.MODULE$.apply(
                        existing.description,
                        Some$.MODULE$.apply(existing.state),
                        Request$.MODULE$.apply(existing.requestMethod, existing.path, existing.query, existing.requestHeaders,
                                existing.requestBody, existing.requestMatchers),
                        Response$.MODULE$.apply(responseStatus, responseHeaders, responseBody, responseMatchers)
                );
            }

            existing.interactions.add(currentInteraction);
        }

        /**
         * Terminates the DSL and builds a pact fragment to represent the interactions
         * @return
         */
        public PactFragment toFragment() {
            addInteraction();
            return new PactFragment(
                    existing.consumer,
                    existing.provider,
                    JavaConverters$.MODULE$.asScalaBufferConverter(existing.interactions).asScala());
        }

        /**
         * Description of the request that is expected to be received
         * @param description request description
         */
        public PactDslRequestWithPath uponReceiving(String description) {
            addInteraction();
            return new PactDslRequestWithPath(existing, description);
        }
    }

    public static PactDslJsonBody jsonBody() {
        return new PactDslJsonBody();
    }

    static String xmlToString(Document body) throws TransformerException {
        Transformer transformer = TransformerFactory.newInstance().newTransformer();
        transformer.setOutputProperty(OutputKeys.INDENT, "yes");
        StreamResult result = new StreamResult(new StringWriter());
        DOMSource source = new DOMSource(body);
        transformer.transform(source, result);
        return result.getWriter().toString();
    }
}
