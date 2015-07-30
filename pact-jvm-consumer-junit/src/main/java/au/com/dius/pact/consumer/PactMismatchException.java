package au.com.dius.pact.consumer;

public class PactMismatchException extends RuntimeException {
    private final PactMismatch result;

    public PactMismatchException(PactMismatch result) {
        super(result.toString(), result.userError().isDefined() ? result.userError().get() : null);
        this.result = result;
    }
}
