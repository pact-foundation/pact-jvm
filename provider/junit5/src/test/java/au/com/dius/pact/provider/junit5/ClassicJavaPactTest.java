package au.com.dius.pact.provider.junit5;

import au.com.dius.pact.core.model.Interaction;
import au.com.dius.pact.core.model.Pact;
import au.com.dius.pact.provider.junitsupport.Provider;
import au.com.dius.pact.provider.junitsupport.State;
import au.com.dius.pact.provider.junitsupport.loader.PactBroker;
import com.github.tomakehurst.wiremock.WireMockServer;
import org.apache.hc.core5.http.HttpRequest;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.TestTemplate;
import org.junit.jupiter.api.extension.ExtendWith;
import static com.github.tomakehurst.wiremock.client.WireMock.*;

@Provider("ProviderTestPost")
@PactBroker(host = "localhost", port = "9292", consumers = "ConsumerTestPost")
public class ClassicJavaPactTest {
    private static final int WIREMOCK_PORT = 8080;
    private WireMockServer wireMockServer;

    @BeforeEach
    public void setup() {
        wireMockServer = new WireMockServer(WIREMOCK_PORT);
        wireMockServer.stubFor(
                post(urlPathEqualTo("/users/prudhvi")).withRequestBody(equalToJson("{\n" +
                                "  \"firstName\": \"Prudhvi\",\n" +
                                "  \"lastName\": \"Raj\"\n" +
                                "}"))
                        .willReturn(aResponse()
                                .withStatus(200)
                                .withHeader("Content-Type", "application/json; charset=UTF-8")
                        )
        );
        wireMockServer.start();
    }

    @BeforeEach
    void setTarget(PactVerificationContext context) {
        HttpTestTarget target = new HttpTestTarget("localhost", WIREMOCK_PORT);
        context.setTarget(target);
        String buildVersion = "1.0.0";
        System.setProperty("pact.provider.version", buildVersion);
        System.setProperty("pact.verifier.publishResults", "true");
    }

    @TestTemplate
    @ExtendWith(PactVerificationInvocationContextProvider.class)
    void testTemplate(Pact pact, Interaction interaction, HttpRequest request, PactVerificationContext context) {
        context.verifyInteraction();
    }

    @State("User Prudhvi")
    public void testStatusofuserPrudhvi() {
        System.out.println("Pact verification started...");
    }

}
