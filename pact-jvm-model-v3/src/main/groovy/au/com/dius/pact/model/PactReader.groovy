package au.com.dius.pact.model

import au.com.dius.pact.model.v3.messaging.MessagePact
import com.github.zafarkhaja.semver.Version
import groovy.json.JsonSlurper
import org.json4s.FileInput
import org.json4s.StreamInput

/**
 * Class to load a Pact from a JSON source using a version strategy
 */
@SuppressWarnings(['EmptyMethod', 'UnusedMethodParameter'])
class PactReader {

    private static final String JSON = 'application/json'
    private static final Map ACCEPT_JSON = [requestProperties: [Accept: JSON] ]

    /**
     * Loads a pact file from either a File or a URL
     * @param source a File or a URL
     */
    def loadPact(def source) {
        def pact = loadFile(source)
        def version = pact.metadata?.'pact-specification'?.version ?: '2.0.0'
        if (version == '3.0') {
            version = '3.0.0'
        }
        def specVersion = Version.valueOf(version)
        switch (specVersion.majorVersion) {
            case 3:
                return loadV3Pact(source, pact)
            default:
                return loadV2Pact(source, pact)
        }
    }

    def loadV3Pact(def source, def pactJson) {
        if (pactJson.messages) {
            new MessagePact().fromMap(pactJson)
        } else {
            loadV2Pact(source, pactJson)
        }
    }

    Pact loadV2Pact(def source, def pactJson) {
        if (source instanceof File) {
            Pact$.MODULE$.from(new FileInput(source))
        } else {
            Pact$.MODULE$.from(new StreamInput(source.newInputStream(ACCEPT_JSON)))
        }
    }

    private static loadFile(def source) {
        if (source instanceof File) {
            new JsonSlurper().parse(source)
        } else if (source instanceof URL) {
            new JsonSlurper().parse(source, ACCEPT_JSON)
        } else {
            throw new UnsupportedOperationException(
                "Unable to load pact file from $source as it is neither a file or a URL")
        }
    }

}
