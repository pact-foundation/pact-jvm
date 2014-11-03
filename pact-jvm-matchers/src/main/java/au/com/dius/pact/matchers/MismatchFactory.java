package au.com.dius.pact.matchers;

public interface MismatchFactory<Mismatch> {
    Mismatch create(Object expected, Object actual, String message, String path);
}
