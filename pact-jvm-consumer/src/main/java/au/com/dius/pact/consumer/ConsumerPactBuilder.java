package au.com.dius.pact.consumer;

import au.com.dius.pact.model.*;
import scala.Option$;

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

    class PactDslWithProvider {
        private String providerName;
        public PactDslWithProvider(String provider) {
            this.providerName = provider;
        }

        public PactDslWithState given(String state) {
            return new PactDslWithState(state);
        }

        class PactDslWithState {
            private String state;
            public PactDslWithState(String state) {
                this.state = state;
            }

            public PactDslRequestWithoutPath uponReceiving(String description) {
                return new PactDslRequestWithoutPath(description);
            }

            class PactDslRequestWithoutPath {
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
                    return new PactDslRequestWithPath(path);
                }

                class PactDslRequestWithPath {
                    private String path;
                    public PactDslRequestWithPath(String path) {
                        this.path = path;
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

                    public PactDslResponse willRespondWith() {
                        return new PactDslResponse();
                    }

                    class PactDslResponse {
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

                        public PactFragment toFragment() {
                            return new PactFragment(
                                    new Consumer(consumerName),
                                    new Provider(providerName),
                                    Option$.MODULE$.apply(state),
                                    description,
                                    Request$.MODULE$.apply(requestMethod, path, requestHeaders, requestBody),
                                    Response$.MODULE$.apply(responseStatus, responseHeaders, responseBody));
                        }
                    }
                }

            }
        }
    }
}
