package au.com.dius.pact.consumer.dsl;

import au.com.dius.pact.core.model.matchingrules.MaxTypeMatcher;
import au.com.dius.pact.core.model.matchingrules.MinMaxTypeMatcher;
import au.com.dius.pact.core.model.matchingrules.MinTypeMatcher;
import au.com.dius.pact.core.model.matchingrules.TypeMatcher;

import java.util.function.Consumer;

/**
 * An alternative, lambda based, dsl for pact that runs on top of the default pact dsl objects.
 */
public class LambdaDsl {

    private LambdaDsl() {
    }

    /**
     * DSL function to simplify creating a {@link DslPart} generated from a {@link LambdaDslJsonArray}.
     */
    public static LambdaDslJsonArray newJsonArray(Consumer<LambdaDslJsonArray> array) {
        final PactDslJsonArray pactDslJsonArray = new PactDslJsonArray();
        final LambdaDslJsonArray dslArray = new LambdaDslJsonArray(pactDslJsonArray);
        array.accept(dslArray);
        return dslArray;
    }

    /**
     * DSL function to simplify creating a {@link DslPart} generated from a {@link LambdaDslJsonArray}.
     * @param examples Number of examples to populate the array with
     */
    public static LambdaDslJsonArray newJsonArray(Integer examples, Consumer<LambdaDslJsonArray> array) {
        final PactDslJsonArray pactDslJsonArray = new PactDslJsonArray();
        pactDslJsonArray.setNumberExamples(examples);
        final LambdaDslJsonArray dslArray = new LambdaDslJsonArray(pactDslJsonArray);
        array.accept(dslArray);
        return dslArray;
    }

    /**
     * Array where each item must match the provided example.
     */
    public static LambdaDslJsonArray newJsonArrayLike(Consumer<LambdaDslObject> obj) {
      final PactDslJsonArray pactDslJsonArray = new PactDslJsonArray("", "", null, true);
      pactDslJsonArray.getMatchers().addRule("", TypeMatcher.INSTANCE);
      final LambdaDslJsonArray dslArray = new LambdaDslJsonArray(pactDslJsonArray);

      final PactDslJsonBody pactObject = pactDslJsonArray.object();
      LambdaDslObject object = new LambdaDslObject(pactObject);
      obj.accept(object);
      pactObject.closeObject();

      return dslArray;
    }

    /**
     * Array where each item must match the provided example.
     * @param examples Number of examples to populate the array with
     */
    public static LambdaDslJsonArray newJsonArrayLike(Integer examples, Consumer<LambdaDslObject> obj) {
      final PactDslJsonArray pactDslJsonArray = new PactDslJsonArray("", "", null, true);
      pactDslJsonArray.getMatchers().addRule("", TypeMatcher.INSTANCE);

      pactDslJsonArray.setNumberExamples(examples);
      final LambdaDslJsonArray dslArray = new LambdaDslJsonArray(pactDslJsonArray);

      final PactDslJsonBody pactObject = pactDslJsonArray.object();
      LambdaDslObject object = new LambdaDslObject(pactObject);
      obj.accept(object);
      pactObject.closeObject();

      return dslArray;
    }

    /**
     * DSL function to simplify creating a {@link DslPart} generated from a {@link LambdaDslJsonArray} where a minimum base array size is specified
     */
    public static LambdaDslJsonArray newJsonArrayMinLike(Integer size, Consumer<LambdaDslJsonArray> array) {
        final PactDslJsonArray pactDslJsonArray = new PactDslJsonArray("", "", null, true);
        pactDslJsonArray.setNumberExamples(size);
        pactDslJsonArray.getMatchers().addRule(new MinTypeMatcher(size));

        final LambdaDslJsonArray dslArray = new LambdaDslJsonArray(pactDslJsonArray);
        array.accept(dslArray);
        return dslArray;
    }

    /**
     * DSL function to simplify creating a {@link DslPart} generated from a {@link LambdaDslJsonArray} where a maximum base array size is specified
     */
    public static LambdaDslJsonArray newJsonArrayMaxLike(Integer size, Consumer<LambdaDslJsonArray> array) {
        final PactDslJsonArray pactDslJsonArray = new PactDslJsonArray("", "", null, true);
        pactDslJsonArray.setNumberExamples(1);
        pactDslJsonArray.getMatchers().addRule(new MaxTypeMatcher(size));

        final LambdaDslJsonArray dslArray = new LambdaDslJsonArray(pactDslJsonArray);
        array.accept(dslArray);
        return dslArray;
    }

    /**
     * DSL function to simplify creating a {@link DslPart} generated from a {@link LambdaDslJsonArray} where a minimum and maximum base array size is specified
     */
    public static LambdaDslJsonArray newJsonArrayMinMaxLike(Integer minSize, Integer maxSize, Consumer<LambdaDslJsonArray> array) {
        final PactDslJsonArray pactDslJsonArray = new PactDslJsonArray("", "", null, true);
        pactDslJsonArray.setNumberExamples(minSize);
        pactDslJsonArray.getMatchers().addRule(new MinMaxTypeMatcher(minSize, maxSize));

        final LambdaDslJsonArray dslArray = new LambdaDslJsonArray(pactDslJsonArray);
        array.accept(dslArray);
        return dslArray;
    }

    /**
     * New JSON array element where order is ignored
     */
    public static LambdaDslJsonArray newJsonArrayUnordered(final Consumer<LambdaDslJsonArray> array) {
        final PactDslJsonArray pactDslJsonArray = PactDslJsonArray.newUnorderedArray();
        final LambdaDslJsonArray dslArray = new LambdaDslJsonArray(pactDslJsonArray);
        array.accept(dslArray);
        return dslArray;
    }

    /**
     * New JSON array element of min size where order is ignored
     * @param size
     */
    public static LambdaDslJsonArray newJsonArrayMinUnordered(int size, final Consumer<LambdaDslJsonArray> array) {
        final PactDslJsonArray pactDslJsonArray = PactDslJsonArray.newUnorderedMinArray(size);
        final LambdaDslJsonArray dslArray = new LambdaDslJsonArray(pactDslJsonArray);
        array.accept(dslArray);
        return dslArray;
    }

    /**
     * New JSON array element of max size where order is ignored
     * @param size
     */
    public static LambdaDslJsonArray newJsonArrayMaxUnordered(int size, final Consumer<LambdaDslJsonArray> array) {
        final PactDslJsonArray pactDslJsonArray = PactDslJsonArray.newUnorderedMaxArray(size);
        final LambdaDslJsonArray dslArray = new LambdaDslJsonArray(pactDslJsonArray);
        array.accept(dslArray);
        return dslArray;
    }

    /**
     * New JSON array element of min and max size where order is ignored
     * @param minSize
     * @param maxSize
     */
    public static LambdaDslJsonArray newJsonArrayMinMaxUnordered(int minSize, int maxSize, final Consumer<LambdaDslJsonArray> array) {
        final PactDslJsonArray pactDslJsonArray = PactDslJsonArray.newUnorderedMinMaxArray(minSize, maxSize);
        final LambdaDslJsonArray dslArray = new LambdaDslJsonArray(pactDslJsonArray);
        array.accept(dslArray);
        return dslArray;
    }

    /**
     * DSL function to simplify creating a {@link DslPart} generated from a {@link LambdaDslJsonBody}.
     */
    public static LambdaDslJsonBody newJsonBody(Consumer<LambdaDslJsonBody> array) {
        final PactDslJsonBody pactDslJsonBody = new PactDslJsonBody();
        final LambdaDslJsonBody dslBody = new LambdaDslJsonBody(pactDslJsonBody);
        array.accept(dslBody);
        return dslBody;
    }

  /**
   * DSL function to simplify creating a {@link DslPart} generated from a {@link LambdaDslJsonBody}. This takes a
   * base template to copy the attributes from.
   */
  public static LambdaDslJsonBody newJsonBody(LambdaDslJsonBody baseTemplate, Consumer<LambdaDslJsonBody> array) {
    final PactDslJsonBody pactDslJsonBody = new PactDslJsonBody();
    pactDslJsonBody.extendFrom((PactDslJsonBody) baseTemplate.build());
    final LambdaDslJsonBody dslBody = new LambdaDslJsonBody(pactDslJsonBody);
    array.accept(dslBody);
    return dslBody;
  }
}
