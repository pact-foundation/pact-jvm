package au.com.dius.pact.model;

/**
 * Pact Specification Version
 */
public enum PactSpecVersion {
    V1(2), V1_1(2), V2(2), V3(3);

    private final int supportedVersion;

    PactSpecVersion(int supportedVersion) {
        this.supportedVersion = supportedVersion;
    }

    public int getSupportedVersion() {
        return supportedVersion;
    }

    public static PactSpecVersion fromInt(int version) {
        switch (version) {
            case 1:
                return V1;
            case 3:
                return V3;
        }

        return V2;
    }
}
