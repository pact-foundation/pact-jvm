package au.com.dius.pact.model

/**
 * Pact Specification Version
 */
@SuppressWarnings(['DuplicateNumberLiteral', 'SerializableClassMustDefineSerialVersionUID'])
enum PactSpecVersion {
    V1(2), V1_1(2), V2(2), V3(3)

    private final int supportedVersion

    PactSpecVersion(int supportedVersion) {
        this.supportedVersion = supportedVersion
    }

    int getSupportedVersion() {
        supportedVersion
    }

    static PactSpecVersion fromInt(int version) {
        switch (version) {
            case 1:
                return V1
            case 3:
                return V3
        }

        V2
    }
}
