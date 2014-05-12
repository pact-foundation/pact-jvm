package au.com.dius.pact.consumer;

import au.com.dius.pact.model.*;
import au.com.dius.pact.model.Interaction$;
import scala.collection.JavaConverters$;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public class ConsumerPactBuilder {

    private String consumerName;
    public ConsumerPactBuilder(String consumer) {
        this.consumerName = consumer;
    }

    public static ConsumerPactBuilder consumer(String consumer) {
        return new ConsumerPactBuilder(consumer);
    }

    public PactDslWithProvider hasPactWith(String provider) {
        return new PactDslWithProvider(provider);
    }

    public class PactDslWithProvider {
        private String providerName;
        public PactDslWithProvider(String provider) {
            this.providerName = provider;
        }

        public PactDslWithState given(String state) {
            return new PactDslWithState(state);
        }

        public class PactDslWithState {
            private String state;
            public PactDslWithState(String state) {
                this.state = state;
            }

            public PactDslRequestWithoutPath uponReceiving(String description) {
                return new PactDslRequestWithoutPath(description);
            }

            public class PactDslRequestWithoutPath {
                private String description;
                public PactDslRequestWithoutPath(String description) {
                    this.description = description;
                }


                private String requestMethod;
                public PactDslRequestWithoutPath method(String method) {
                    requestMethod = method;
                    return this;
                }

                private Map<String, String> requestHeaders;
                public PactDslRequestWithoutPath headers(Map<String, String> headers) {
                    requestHeaders = headers;
                    return this;
                }

                private String requestBody;
                public PactDslRequestWithoutPath body(String body) {
                    requestBody = body;
                    return this;
                }

                public PactDslRequestWithPath path(String path) {
                    return new PactDslRequestWithPath(consumerName, providerName, state, description, path, requestMethod, requestHeaders, requestBody);
                }
            }
        }
    }

    public class PactDslRequestWithPath {
        private Consumer consumer;
        private Provider provider;

        private String state;

        private String description;
        private String path;
        private String requestMethod;
        private Map<String, String> requestHeaders;
        private String requestBody;

        private List<Interaction> interactions = new ArrayList<Interaction>();

        public PactDslRequestWithPath(String consumerName,
                                      String providerName,
                                      String state,
                                      String description,
                                      String path,
                                      String requestMethod,
                                      Map<String, String> requestHeaders,
                                      String requestBody) {
            this.consumer = new Consumer(consumerName);
            this.provider = new Provider(providerName);

            this.state = state;

            this.description = description;
            this.path = path;
            this.requestMethod = requestMethod;
            this.requestHeaders = requestHeaders;
            this.requestBody = requestBody;
        }

        public PactDslRequestWithPath(PactDslRequestWithPath existing, String description) {
            this.consumer = existing.consumer;
            this.provider = existing.provider;

            this.state = existing.state;

            this.description = description;
            this.path = existing.path;
            this.requestMethod = existing.requestMethod;
            this.requestHeaders = existing.requestHeaders;
            this.requestBody = existing.requestBody;


            this.interactions = existing.interactions;
        }

        public PactDslRequestWithPath method(String method) {
            requestMethod = method;
            return this;
        }

        public PactDslRequestWithPath headers(Map<String, String> headers) {
            requestHeaders = headers;
            return this;
        }

        public PactDslRequestWithPath body(String body) {
            requestBody = body;
            return this;
        }

        public PactDslRequestWithPath path(String path) {
            this.path = path;
            return this;
        }

        public PactDslResponse willRespondWith() {
            return new PactDslResponse(this);
        }
    }

    public class PactDslResponse {
        private PactDslRequestWithPath existing;

        public PactDslResponse(PactDslRequestWithPath existing) {
            this.existing = existing;
        }

        private int responseStatus;
        public PactDslResponse status(int status) {
            this.responseStatus = status;
            return this;
        }

        private Map<String, String> responseHeaders;
        public PactDslResponse headers(Map<String, String> headers) {
            this.responseHeaders = headers;
            return this;
        }

        private String responseBody;
        public PactDslResponse body(String body) {
            this.responseBody = body;
            return this;
        }

        private void addInteraction() {
            Interaction currentInteraction = Interaction$.MODULE$.apply(
                    existing.description,
                    existing.state,
                    Request$.MODULE$.apply(existing.requestMethod, existing.path, existing.requestHeaders, existing.requestBody),
                    Response$.MODULE$.apply(responseStatus, responseHeaders, responseBody)
            );

            existing.interactions.add(currentInteraction);
        }

        public PactFragment toFragment() {
            addInteraction();
            return new PactFragment(
                    existing.consumer,
                    existing.provider,
                    JavaConverters$.MODULE$.asScalaBufferConverter(existing.interactions).asScala());
        }

        public PactDslRequestWithPath uponReceiving(String description) {
            addInteraction();
            return new PactDslRequestWithPath(existing, description);
        }
    }
}
