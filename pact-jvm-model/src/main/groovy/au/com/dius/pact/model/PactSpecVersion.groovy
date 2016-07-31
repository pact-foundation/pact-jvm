package au.com.dius.pact.model

/**
 * Pact Specification Version
 */
@SuppressWarnings('SerializableClassMustDefineSerialVersionUID')
enum PactSpecVersion {
    V1, V1_1, V2, V3

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
