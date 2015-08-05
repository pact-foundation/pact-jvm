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

    /**
     * Loads a pact file from either a File or a URL
     * @param source a File or a URL
     */
    def loadPact(def source) {
        def pact = loadFile(source)
        def specVersion = Version.valueOf(pact.metadata?.'pact-specification'?.version ?: '2.0.0')
        switch (specVersion.majorVersion) {
            case 3:
                return loadV3Pact(source, pact)
            default:
                return loadV2Pact(source, pact)
        }
    }

    def loadV3Pact(def source, def pactJson) {
        if (pactJson.messages) {
            pactJson as MessagePact
        } else {
            loadV2Pact(source, pactJson)
        }
    }

    Pact loadV2Pact(def source, def pactJson) {
        if (source instanceof File) {
            Pact$.MODULE$.from(new FileInput(source))
        } else {
            Pact$.MODULE$.from(new StreamInput(source.newInputStream(requestProperties:
                ['Accept': 'application/json'])))
        }
    }

    private static loadFile(def source) {
        if (source instanceof File) {
            new JsonSlurper().parse(source)
        } else if (source instanceof URL) {
            new JsonSlurper().parse(source, requestProperties: ['Accept': 'application/json'])
        } else {
            throw new UnsupportedOperationException(
                "Unable to load pact file from $source as it is neither a file or a URL")
        }
    }

}
