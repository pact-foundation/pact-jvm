package au.com.dius.pact.provider.junit.target;

import au.com.dius.pact.model.*;
import au.com.dius.pact.provider.ProviderClient;
import au.com.dius.pact.provider.ProviderInfo;
import scala.collection.Seq;

import java.util.Collections;
import java.util.Map;

/**
 * Out-of-the-box implementation of {@link Target},
 * that run {@link Interaction} against http service and verify response
 */
public class HttpTarget implements Target {
    private final String host;
    private final int port;

    /**
     * @param host host of tested service
     * @param port port of tested service
     */
    public HttpTarget(final String host, final int port) {
        this.host = host;
        this.port = port;
    }

    /**
     * Host of tested service is assumed as "localhost"
     *
     * @param port port of tested service
     */
    public HttpTarget(final int port) {
        this("localhost", port);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void testInteraction(final Interaction interaction) {
        final ProviderClient providerClient = new ProviderClient();
        providerClient.setProvider(getProviderInfo());
        providerClient.setRequest(interaction.request());
        final Map<String, Object> actualResponse = (Map<String, Object>) providerClient.makeRequest();

        final Seq<ResponsePartMismatch> mismatches = ResponseMatching$.MODULE$.responseMismatches(
                interaction.response(),
                Response$.MODULE$.apply(
                        ((Integer) actualResponse.get("statusCode")).intValue(),
                        (Map<String, String>) actualResponse.get("headers"),
                        (String) actualResponse.get("data"),
                        Collections.<String, Object>emptyMap())
        );

        if (!mismatches.isEmpty()) {
            throw getAssertionError(mismatches);
        }
    }

    private ProviderInfo getProviderInfo() {
        final ProviderInfo providerInfo = new ProviderInfo();
        providerInfo.setPort(port);
        providerInfo.setHost(host);
        return providerInfo;
    }

    private AssertionError getAssertionError(final Seq<ResponsePartMismatch> mismatches) {
        final StringBuilder result = new StringBuilder();
        for (ResponsePartMismatch mismatch: scala.collection.JavaConversions.seqAsJavaList(mismatches)) {
            result.append("\n");
            if (mismatch instanceof StatusMismatch) {
                final StatusMismatch statusMismatch = (StatusMismatch) mismatch;
                result.append("StatusMismatch - Expected status " + statusMismatch.expected() + " but was " +
                        statusMismatch.actual());
            } else if (mismatch instanceof HeaderMismatch) {
                result.append(((HeaderMismatch) mismatch).description());
            } else if (mismatch instanceof BodyTypeMismatch) {
                final BodyTypeMismatch bodyTypeMismatch = (BodyTypeMismatch) mismatch;
                result.append("BodyTypeMismatch - Expected body to have type '" + bodyTypeMismatch.expected() +
                        "' but was '" + bodyTypeMismatch.actual() + "'");
            } else if (mismatch instanceof BodyMismatch) {
                result.append(((BodyMismatch) mismatch).description());
            } else {
                result.append(mismatch.toString());
            }
        }
        return new AssertionError(result.toString());
    }
}
