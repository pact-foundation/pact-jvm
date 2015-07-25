package au.com.dius.pact.consumer;

public class PactMismatchException extends RuntimeException {
    private final PactMismatch result;

    public PactMismatchException(PactMismatch result) {
        super(result.toString());
        this.result = result;
    }
}
