package io.pactfoundation.consumer.dsl;

import au.com.dius.pact.consumer.dsl.PactDslJsonArray;
import au.com.dius.pact.consumer.dsl.PactDslJsonBody;
import au.com.dius.pact.core.model.matchingrules.MaxTypeMatcher;
import au.com.dius.pact.core.model.matchingrules.MinMaxTypeMatcher;
import au.com.dius.pact.core.model.matchingrules.MinTypeMatcher;

import java.util.function.Consumer;

/**
 * An alternative, lambda based, dsl for pact that runs on top of the default pact dsl objects.
 */
public class LambdaDsl {

    public static LambdaDslJsonArray newJsonArray(Consumer<LambdaDslJsonArray> array) {
        final PactDslJsonArray pactDslJsonArray = new PactDslJsonArray();
        final LambdaDslJsonArray dslArray = new LambdaDslJsonArray(pactDslJsonArray);
        array.accept(dslArray);
        return dslArray;
    }

    public static LambdaDslJsonArray newJsonArrayMinLike(Integer size, Consumer<LambdaDslJsonArray> array) {
        final PactDslJsonArray pactDslJsonArray = new PactDslJsonArray();
        pactDslJsonArray.setNumberExamples(size);
        pactDslJsonArray.getMatchers().addRule(new MinTypeMatcher(size));

        final LambdaDslJsonArray dslArray = new LambdaDslJsonArray(pactDslJsonArray);
        array.accept(dslArray);
        return dslArray;
    }

    public static LambdaDslJsonArray newJsonArrayMaxLike(Integer size, Consumer<LambdaDslJsonArray> array) {
        final PactDslJsonArray pactDslJsonArray = new PactDslJsonArray();
        pactDslJsonArray.setNumberExamples(1);
        pactDslJsonArray.getMatchers().addRule(new MaxTypeMatcher(size));

        final LambdaDslJsonArray dslArray = new LambdaDslJsonArray(pactDslJsonArray);
        array.accept(dslArray);
        return dslArray;
    }

    public static LambdaDslJsonArray newJsonArrayMinMaxLike(Integer minSize, Integer maxSize, Consumer<LambdaDslJsonArray> array) {
        final PactDslJsonArray pactDslJsonArray = new PactDslJsonArray();
        pactDslJsonArray.setNumberExamples(minSize);
        pactDslJsonArray.getMatchers().addRule(new MinMaxTypeMatcher(minSize, maxSize));

        final LambdaDslJsonArray dslArray = new LambdaDslJsonArray(pactDslJsonArray);
        array.accept(dslArray);
        return dslArray;
    }

    public static LambdaDslJsonBody newJsonBody(Consumer<LambdaDslJsonBody> array) {
        final PactDslJsonBody pactDslJsonBody = new PactDslJsonBody();
        final LambdaDslJsonBody dslBody = new LambdaDslJsonBody(pactDslJsonBody);
        array.accept(dslBody);
        return dslBody;
    }

}
