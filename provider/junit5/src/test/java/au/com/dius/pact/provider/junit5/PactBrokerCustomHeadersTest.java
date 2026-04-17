package au.com.dius.pact.provider.junit5;

import au.com.dius.pact.provider.junitsupport.Provider;
import au.com.dius.pact.provider.junitsupport.loader.PactBroker;
import au.com.dius.pact.provider.junitsupport.loader.PactBrokerHttpHeader;
import com.github.tomakehurst.wiremock.WireMockServer;
import com.github.tomakehurst.wiremock.core.WireMockConfiguration;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.TestTemplate;
import org.junit.jupiter.api.extension.ExtendWith;

import java.net.MalformedURLException;
import java.net.URL;

import static com.github.tomakehurst.wiremock.client.WireMock.aResponse;
import static com.github.tomakehurst.wiremock.client.WireMock.equalTo;
import static com.github.tomakehurst.wiremock.client.WireMock.get;
import static com.github.tomakehurst.wiremock.client.WireMock.getRequestedFor;
import static com.github.tomakehurst.wiremock.client.WireMock.post;
import static com.github.tomakehurst.wiremock.client.WireMock.postRequestedFor;
import static com.github.tomakehurst.wiremock.client.WireMock.urlEqualTo;
import static com.github.tomakehurst.wiremock.client.WireMock.urlPathEqualTo;

/**
 * Example demonstrating how to add custom HTTP headers to all Pact Broker requests using
 * {@link PactBrokerHttpHeader}.
 *
 * <p>This is useful when the Pact Broker sits behind a gateway that requires extra
 * authentication headers — for example Cloudflare Zero Trust (which checks a
 * {@code cf-access-token} header on every request).
 *
 * <p>Both header values support expression syntax:
 * <ul>
 *   <li>A literal string is sent verbatim.</li>
 *   <li>{@code ${property.name:fallback}} is resolved from system properties / environment
 *       variables at test run time, falling back to the value after {@code :} when the
 *       property is unset.</li>
 * </ul>
 *
 * <p>Two WireMock servers stand in for the real services:
 * <ul>
 *   <li><b>brokerServer</b> — acts as the Pact Broker; the HAL-navigation stubs require the
 *       custom headers so the test fails fast when they are missing.</li>
 *   <li><b>providerServer</b> — acts as the provider under test.</li>
 * </ul>
 *
 * <p>Equivalent annotation for a real project pointing at Cloudflare Zero Trust:
 * <pre>
 * {@literal @}PactBroker(
 *     url = "https://broker.internal",
 *     customHeaders = {
 *         {@literal @}PactBrokerHttpHeader(name = "cf-access-token", value = "${CF_ACCESS_TOKEN}"),
 *         {@literal @}PactBrokerHttpHeader(name = "X-Tenant-ID",     value = "my-org")
 *     }
 * )
 * </pre>
 */
@Provider("BrokerCustomHeadersProvider")
@PactBroker(
    url = "${pactbroker.url}",
    customHeaders = {
        // Static value — always sent verbatim.
        @PactBrokerHttpHeader(name = "X-CF-Access-Token", value = "test-cf-token"),
        // Expression — resolved from a system property; falls back to "acme-corp" when unset.
        @PactBrokerHttpHeader(name = "X-Tenant-ID", value = "${example.tenant.id:acme-corp}")
    }
)
class PactBrokerCustomHeadersTest {

    private static WireMockServer brokerServer;
    private static WireMockServer providerServer;

    /**
     * Starts both WireMock servers and registers the broker URL as a system property so the
     * {@code ${pactbroker.url}} expression in {@link PactBroker#url()} resolves correctly.
     *
     * <p>This runs before JUnit 5 asks {@link PactVerificationInvocationContextProvider} for
     * test-template invocation contexts, so the broker is reachable when pacts are fetched.
     */
    @BeforeAll
    static void startServers() {
        providerServer = new WireMockServer(WireMockConfiguration.wireMockConfig().dynamicPort());
        providerServer.start();

        brokerServer = new WireMockServer(WireMockConfiguration.wireMockConfig().dynamicPort());
        brokerServer.start();

        System.setProperty("pactbroker.url", "http://localhost:" + brokerServer.port());

        configureBrokerMock();
        configureProviderMock();
    }

    /**
     * Verifies that the Pact Broker HAL-navigation requests carried both custom headers, then
     * shuts both servers down.
     *
     * <p><b>Note:</b> pact <em>content</em> downloads (the final GET that fetches the pact
     * JSON) are made by {@code DefaultPactReader} using a separate HTTP client that is
     * currently built from the {@code authentication} options only, not from
     * {@code customHeaders}.  That step is therefore excluded from the header assertions below.
     */
    @AfterAll
    static void stopServers() {
        try {
            brokerServer.verify(getRequestedFor(urlEqualTo("/"))
                .withHeader("X-CF-Access-Token", equalTo("test-cf-token"))
                .withHeader("X-Tenant-ID", equalTo("acme-corp")));

            brokerServer.verify(postRequestedFor(urlPathEqualTo(
                        "/pacts/provider/BrokerCustomHeadersProvider/for-verification"))
                .withHeader("X-CF-Access-Token", equalTo("test-cf-token"))
                .withHeader("X-Tenant-ID", equalTo("acme-corp")));
        } finally {
            brokerServer.stop();
            providerServer.stop();
            System.clearProperty("pactbroker.url");
        }
    }

    @BeforeEach
    void configureTarget(PactVerificationContext context) throws MalformedURLException {
        context.setTarget(HttpTestTarget.fromUrl(new URL("http://localhost:" + providerServer.port())));
    }

    @TestTemplate
    @ExtendWith(PactVerificationInvocationContextProvider.class)
    void verifyPactInteraction(PactVerificationContext context) {
        context.verifyInteraction();
    }

    // -------------------------------------------------------------------------
    // WireMock setup
    // -------------------------------------------------------------------------

    /**
     * Configures the mock Pact Broker with the minimal HAL responses needed to load a single
     * consumer pact.
     *
     * <p>Steps mirroring the real Pact Broker HAL navigation:
     * <ol>
     *   <li>{@code GET /} — root; advertises {@code pb:provider-pacts-for-verification}.</li>
     *   <li>{@code POST .../for-verification} — returns pact references for the provider.</li>
     *   <li>{@code GET .../latest} — serves the actual pact JSON.</li>
     * </ol>
     *
     * <p>Steps 1 and 2 require both custom headers (they go through {@code HalClient} which
     * attaches the headers configured in {@code PactBrokerClientConfig}).  Step 3 does not
     * require them (see note in {@link #stopServers()}).
     */
    private static void configureBrokerMock() {
        int port = brokerServer.port();

        // Step 1: HAL root — must include custom headers
        brokerServer.stubFor(get(urlEqualTo("/"))
            .withHeader("X-CF-Access-Token", equalTo("test-cf-token"))
            .withHeader("X-Tenant-ID", equalTo("acme-corp"))
            .willReturn(aResponse()
                .withStatus(200)
                .withHeader("Content-Type", "application/hal+json")
                .withBody("""
                        {
                          "_links": {
                            "pb:provider-pacts-for-verification": {
                              "href": "http://localhost:%d/pacts/provider/{provider}/for-verification",
                              "templated": true
                            }
                          }
                        }
                        """.formatted(port))));

        // Step 2: Pacts-for-verification — must include custom headers; returns one pact reference
        brokerServer.stubFor(post(urlPathEqualTo(
                    "/pacts/provider/BrokerCustomHeadersProvider/for-verification"))
            .withHeader("X-CF-Access-Token", equalTo("test-cf-token"))
            .withHeader("X-Tenant-ID", equalTo("acme-corp"))
            .willReturn(aResponse()
                .withStatus(200)
                .withHeader("Content-Type", "application/hal+json")
                .withBody("""
                        {
                          "_embedded": {
                            "pacts": [
                              {
                                "_links": {
                                  "self": {
                                    "href": "http://localhost:%d/pacts/provider/BrokerCustomHeadersProvider/consumer/BrokerCustomHeadersConsumer/latest",
                                    "name": "BrokerCustomHeadersConsumer"
                                  }
                                },
                                "verificationProperties": {
                                  "notices": [],
                                  "pending": false,
                                  "wip": false
                                }
                              }
                            ]
                          }
                        }
                        """.formatted(port))));

        // Step 3: Pact content — custom headers not required here (see note in stopServers)
        brokerServer.stubFor(get(urlPathEqualTo(
                    "/pacts/provider/BrokerCustomHeadersProvider/consumer/BrokerCustomHeadersConsumer/latest"))
            .willReturn(aResponse()
                .withStatus(200)
                .withHeader("Content-Type", "application/json")
                .withBody("""
                        {
                          "provider": { "name": "BrokerCustomHeadersProvider" },
                          "consumer": { "name": "BrokerCustomHeadersConsumer" },
                          "interactions": [
                            {
                              "description": "a request to the greeting endpoint",
                              "request": {
                                "method": "GET",
                                "path":   "/greeting"
                              },
                              "response": {
                                "status": 200
                              }
                            }
                          ],
                          "metadata": {
                            "pact-specification": { "version": "2.0.0" },
                            "pact-jvm":           { "version": "4.0.0" }
                          }
                        }
                        """)));
    }

    private static void configureProviderMock() {
        providerServer.stubFor(get(urlPathEqualTo("/greeting"))
            .willReturn(aResponse().withStatus(200)));
    }
}
