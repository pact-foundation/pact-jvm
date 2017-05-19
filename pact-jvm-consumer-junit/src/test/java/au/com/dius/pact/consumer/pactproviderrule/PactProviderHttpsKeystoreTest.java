package au.com.dius.pact.consumer.pactproviderrule;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.security.Key;
import java.security.KeyPair;
import java.security.KeyStore;
import java.security.KeyStoreException;
import java.security.NoSuchAlgorithmException;
import java.security.PrivateKey;
import java.security.PublicKey;
import java.security.UnrecoverableKeyException;
import java.security.cert.Certificate;
import java.security.cert.CertificateException;
import java.security.cert.CertificateFactory;
import java.util.HashMap;
import java.util.Map;

import javax.net.ssl.SSLHandshakeException;

import org.hamcrest.Matchers;
import org.junit.Rule;
import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.jayway.restassured.RestAssured;
import au.com.dius.pact.consumer.Pact;
import au.com.dius.pact.consumer.PactProviderRule;
import au.com.dius.pact.consumer.PactVerification;
import au.com.dius.pact.consumer.dsl.PactDslWithProvider;
import au.com.dius.pact.consumer.exampleclients.ConsumerHttpsClient;
import au.com.dius.pact.model.MockHttpsKeystoreProviderConfig;
import au.com.dius.pact.model.PactFragment;
import au.com.dius.pact.model.PactSpecVersion;

public class PactProviderHttpsKeystoreTest {
    private static final Logger LOGGER = LoggerFactory.getLogger(PactProviderHttpsKeystoreTest.class);

    @Rule
    public PactProviderRule mockTestProvider = new PactProviderRule("test_provider", "localhost", 11443, true,
            //Generated jks with the following command:
            //keytool -genkeypair -alias localhost -keyalg RSA -validity 36500 -keysize 512 -keystore pact-jvm-512.jks
            Paths.get("src/test/resources/keystore/pact-jvm-512.jks").toFile().getAbsolutePath(),"brentwashere", PactSpecVersion.V2, this);

    @Pact(provider="test_provider", consumer="test_consumer")
    public PactFragment createFragment(PactDslWithProvider builder) {
        Map<String, String> headers = new HashMap<String, String>();
        headers.put("testreqheader", "testreqheadervalue");

        Map<String, String> jsonResponseHeaders = new HashMap<String, String>();
        jsonResponseHeaders.put("Content-Type", "application/json");
        jsonResponseHeaders.put("testreqheader", "testreqheadervalue");

        return builder
            .given("good state")
            .uponReceiving("PactProviderTest test interaction")
                .path("/")
                .method("GET")
                .headers(headers)
            .willRespondWith()
                .status(200)
                .headers(jsonResponseHeaders)
                .body("{\"responsetest\": true, \"name\": \"harry\"}")
            .uponReceiving("PactProviderTest second test interaction")
                .method("OPTIONS")
                .headers(headers)
                .path("/second")
                .body("")
            .willRespondWith()
                .status(200)
                .headers(headers)
                .body("")
            .toFragment();
    }

    @Test
    @PactVerification(value = "test_provider")
    public void testKeystoreHappyPath() {
        MockHttpsKeystoreProviderConfig config = (MockHttpsKeystoreProviderConfig) mockTestProvider.getConfig();
        LOGGER.info("Keystore path: " + config.getKeystore());
        RestAssured
                .given()
                    .header("testreqheader", "testreqheadervalue")
                    .trustStore(config.getKeystore(), config.getKeystorePassword())
                .when()
                    .options(mockTestProvider.getConfig().url() + "/second")

                .then()
                    .statusCode(200);

        RestAssured
                .given()
                    .header("testreqheader", "testreqheadervalue")
                    .trustStore(config.getKeystore(), config.getKeystorePassword())
                .when()
                    .get(mockTestProvider.getConfig().url() + "/")
                .then()
                    .body("responsetest", Matchers.equalTo(true))
                    .body("name", Matchers.equalTo("harry"));
    }

    @Test(expected = SSLHandshakeException.class)
    @PactVerification(value = "test_provider")
    public void testSslHandshakeException() throws IOException {
        testKeystoreHappyPath();
        new ConsumerHttpsClient(mockTestProvider.getConfig().url()).getAsMap("/", "");
    }

    @Test(expected = SSLHandshakeException.class)
    @PactVerification(value = "test_provider")
    public void testMisMatchedTrustStore() {
        testKeystoreHappyPath();

        //Used the following command to create jks file:
        //keytool -genkeypair -alias localhost -keyalg RSA -validity 36500 -keystore pact-jvm-other.jks
        File trustStore = Paths.get("src/test/resources/keystore/pact-jvm-other.jks").toFile();

        RestAssured
            .given()
                .header("testreqheader", "testreqheadervalue")
                .trustStore(trustStore, "bbarkewashere")
            .when()
                .options(mockTestProvider.getConfig().url() + "/second")
            .then()
                .statusCode(200);
    }
}
