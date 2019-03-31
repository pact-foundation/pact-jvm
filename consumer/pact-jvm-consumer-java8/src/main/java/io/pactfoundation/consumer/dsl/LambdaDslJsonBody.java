package io.pactfoundation.consumer.dsl;

import au.com.dius.pact.consumer.dsl.DslPart;
import au.com.dius.pact.consumer.dsl.PactDslJsonBody;

public class LambdaDslJsonBody extends LambdaDslObject {

    private final PactDslJsonBody dslPart;

    LambdaDslJsonBody(final PactDslJsonBody dslPart) {
        super(dslPart);
        this.dslPart = dslPart;
    }

    public DslPart build() {
        return dslPart;
    }
}
