package io.pactfoundation.consumer.dsl;

import au.com.dius.pact.consumer.dsl.DslPart;
import au.com.dius.pact.consumer.dsl.PactDslJsonArray;
import au.com.dius.pact.consumer.dsl.PactDslJsonBody;

import java.math.BigDecimal;
import java.util.Date;
import java.util.function.Consumer;

public class LambdaDslJsonArray {


    private final PactDslJsonArray pactArray;

    LambdaDslJsonArray(final PactDslJsonArray pactArray) {
        this.pactArray = pactArray;
    }

    public LambdaDslJsonArray object(final Consumer<LambdaDslObject> o) {
        final PactDslJsonBody pactObject = pactArray.object();
        LambdaDslObject object = new LambdaDslObject(pactObject);
        o.accept(object);
        pactObject.closeObject();
        return this;
    }

    public LambdaDslJsonArray array(final Consumer<LambdaDslJsonArray> a) {
        final PactDslJsonArray pactArray = this.pactArray.array();
        LambdaDslJsonArray array = new LambdaDslJsonArray(pactArray);
        a.accept(array);
        pactArray.closeArray();
        return this;
    }

    public LambdaDslJsonArray stringValue(final String value) {
        pactArray.stringValue(value);
        return this;
    }

    public LambdaDslJsonArray stringType(final String example) {
        pactArray.stringType(example);
        return this;
    }

    public LambdaDslJsonArray stringMatcher(final String regex) {
        pactArray.stringMatcher(regex);
        return this;
    }

    public LambdaDslJsonArray stringMatcher(final String regex, final String example) {
        pactArray.stringMatcher(regex, example);
        return this;
    }

    public LambdaDslJsonArray numberValue(final Number value) {
        pactArray.numberValue(value);
        return this;
    }

    public LambdaDslJsonArray numberType(final Number example) {
        pactArray.numberType(example);
        return this;
    }

    public LambdaDslJsonArray integerType() {
        pactArray.integerType();
        return this;
    }

    public LambdaDslJsonArray integerType(final Long example) {
        pactArray.integerType(example);
        return this;
    }

    public LambdaDslJsonArray decimalType() {
        pactArray.decimalType();
        return this;
    }

    public LambdaDslJsonArray decimalType(final BigDecimal example) {
        pactArray.decimalType(example);
        return this;
    }

    public LambdaDslJsonArray decimalType(final Double example) {
        pactArray.decimalType(example);
        return this;
    }

    public LambdaDslJsonArray booleanValue(final Boolean value) {
        pactArray.booleanValue(value);
        return this;
    }

    public LambdaDslJsonArray booleanType(final Boolean example) {
        pactArray.booleanType(example);
        return this;
    }

    /**
     * Element that must be formatted as an ISO date
     */
    public LambdaDslJsonArray date() {
        pactArray.date();
        return this;
    }

    /**
     * Element that must match the provided date format
     *
     * @param format date format to match
     */
    public LambdaDslJsonArray date(final String format) {
        pactArray.date(format);
        return this;
    }

    /**
     * Element that must match the provided date format
     *
     * @param format  date format to match
     * @param example example date to use for generated values
     */
    public LambdaDslJsonArray date(final String format, final Date example) {
        pactArray.date(format, example);
        return this;
    }

    /**
     * Element that must be an ISO formatted time
     */
    public LambdaDslJsonArray time() {
        pactArray.time();
        return this;
    }

    /**
     * Element that must match the given time format
     *
     * @param format time format to match
     */
    public LambdaDslJsonArray time(final String format) {
        pactArray.time(format);
        return this;
    }

    /**
     * Element that must match the given time format
     *
     * @param format  time format to match
     * @param example example time to use for generated bodies
     */
    public LambdaDslJsonArray time(final String format, final Date example) {
        pactArray.time(format, example);
        return this;
    }

    /**
     * Element that must be an ISO formatted timestamp
     */
    public LambdaDslJsonArray timestamp() {
        pactArray.timestamp();
        return this;
    }

    /**
     * Element that must match the given timestamp format
     *
     * @param format timestamp format
     */
    public LambdaDslJsonArray timestamp(final String format) {
        pactArray.timestamp(format);
        return this;
    }

    /**
     * Element that must match the given timestamp format
     *
     * @param format  timestamp format
     * @param example example date and time to use for generated bodies
     */
    public LambdaDslJsonArray timestamp(final String format, final Date example) {
        pactArray.timestamp(format, example);
        return this;
    }

    public LambdaDslJsonArray id() {
        pactArray.id();
        return this;
    }

    public LambdaDslJsonArray id(final Long example) {
        pactArray.id(example);
        return this;
    }

    public LambdaDslJsonArray uuid() {
        pactArray.uuid();
        return this;
    }

    public LambdaDslJsonArray uuid(final String example) {
        pactArray.uuid(example);
        return this;
    }

    public LambdaDslJsonArray hexValue() {
        pactArray.hexValue();
        return this;
    }

    public LambdaDslJsonArray hexValue(final String value) {
        pactArray.hexValue(value);
        return this;
    }

    public LambdaDslJsonArray ipV4Address() {
        pactArray.ipAddress();
        return this;
    }

    /**
     * Element that is an array where each item must match the following example
     */
    public LambdaDslJsonArray eachLike(Consumer<LambdaDslJsonBody> nestedObject) {
        final PactDslJsonBody arrayLike = pactArray.eachLike();
        final LambdaDslJsonBody dslBody = new LambdaDslJsonBody(arrayLike);
        nestedObject.accept(dslBody);
        arrayLike.closeArray();
        return this;
    }

    /**
     * Element that is an array where each item must match the following example
     *
     * @param numberExamples Number of examples to generate
     */
    public LambdaDslJsonArray eachLike(int numberExamples, Consumer<LambdaDslJsonBody> nestedObject) {
        final PactDslJsonBody arrayLike = pactArray.eachLike(numberExamples);
        final LambdaDslJsonBody dslBody = new LambdaDslJsonBody(arrayLike);
        nestedObject.accept(dslBody);
        arrayLike.closeArray();
        return this;
    }

    /**
     * Element that is an array with a minimum size where each item must match the following example
     *
     * @param size minimum size of the array
     */
    public LambdaDslJsonArray minArrayLike(Integer size, Consumer<LambdaDslJsonBody> nestedObject) {
        final PactDslJsonBody arrayLike = pactArray.minArrayLike(size);
        final LambdaDslJsonBody dslBody = new LambdaDslJsonBody(arrayLike);
        nestedObject.accept(dslBody);
        arrayLike.closeArray();
        return this;
    }


    /**
     * Element that is an array with a minimum size where each item must match the following example
     *
     * @param size           minimum size of the array
     * @param numberExamples number of examples to generate
     */
    public LambdaDslJsonArray minArrayLike(Integer size, int numberExamples,
                                           Consumer<LambdaDslJsonBody> nestedObject) {
        final PactDslJsonBody arrayLike = pactArray.minArrayLike(size, numberExamples);
        final LambdaDslJsonBody dslBody = new LambdaDslJsonBody(arrayLike);
        nestedObject.accept(dslBody);
        arrayLike.closeArray();
        return this;
    }

    /**
     * Element that is an array with a maximum size where each item must match the following example
     *
     * @param size maximum size of the array
     */
    public LambdaDslJsonArray maxArrayLike(Integer size, Consumer<LambdaDslJsonBody> nestedObject) {
        final PactDslJsonBody arrayLike = pactArray.maxArrayLike(size);
        final LambdaDslJsonBody dslBody = new LambdaDslJsonBody(arrayLike);
        nestedObject.accept(dslBody);
        arrayLike.closeArray();
        return this;
    }


    /**
     * Element that is an array with a maximum size where each item must match the following example
     *
     * @param size           maximum size of the array
     * @param numberExamples number of examples to generate
     */
    public LambdaDslJsonArray maxArrayLike(Integer size, int numberExamples,
                                           Consumer<LambdaDslJsonBody> nestedObject) {
        final PactDslJsonBody arrayLike = pactArray.maxArrayLike(size, numberExamples);
        final LambdaDslJsonBody dslBody = new LambdaDslJsonBody(arrayLike);
        nestedObject.accept(dslBody);
        arrayLike.closeArray();
        return this;
    }

  /**
   * Element that is an array with a minimum and maximum size where each item must match the following example
   *
   * @param minSize minimum size of the array
   * @param maxSize maximum size of the array
   */
  public LambdaDslJsonArray minMaxArrayLike(Integer minSize, Integer maxSize, Consumer<LambdaDslJsonBody> nestedObject) {
    final PactDslJsonBody arrayLike = pactArray.minMaxArrayLike(minSize, maxSize);
    final LambdaDslJsonBody dslBody = new LambdaDslJsonBody(arrayLike);
    nestedObject.accept(dslBody);
    arrayLike.closeArray();
    return this;
  }

  /**
   * Element that is an array with a minimum and maximum size where each item must match the following example
   *
   * @param minSize minimum size of the array
   * @param maxSize maximum size of the array
   * @param numberExamples number of examples to generate
   */
  public LambdaDslJsonArray minMaxArrayLike(Integer minSize, Integer maxSize, int numberExamples,
                                         Consumer<LambdaDslJsonBody> nestedObject) {
    final PactDslJsonBody arrayLike = pactArray.minMaxArrayLike(minSize, maxSize, numberExamples);
    final LambdaDslJsonBody dslBody = new LambdaDslJsonBody(arrayLike);
    nestedObject.accept(dslBody);
    arrayLike.closeArray();
    return this;
  }

    /**
     * Adds a null value to the list
     */
    public LambdaDslJsonArray nullValue() {
        pactArray.nullValue();
        return this;
    }

    public LambdaDslJsonArray eachArrayLike(Consumer<LambdaDslJsonArray> nestedArray) {
        final PactDslJsonArray arrayLike = pactArray.eachArrayLike();
        final LambdaDslJsonArray dslArray = new LambdaDslJsonArray(arrayLike);
        nestedArray.accept(dslArray);
        arrayLike.closeArray().closeArray();
        return this;
    }

    public LambdaDslJsonArray eachArrayLike(int numberExamples, Consumer<LambdaDslJsonArray> nestedArray) {
        final PactDslJsonArray arrayLike = pactArray.eachArrayLike(numberExamples);
        final LambdaDslJsonArray dslArray = new LambdaDslJsonArray(arrayLike);
        nestedArray.accept(dslArray);
        arrayLike.closeArray().closeArray();
        return this;
    }

    public LambdaDslJsonArray eachArrayWithMaxLike(Integer size, Consumer<LambdaDslJsonArray> nestedArray) {
        final PactDslJsonArray arrayLike = pactArray.eachArrayWithMaxLike(size);
        final LambdaDslJsonArray dslArray = new LambdaDslJsonArray(arrayLike);
        nestedArray.accept(dslArray);
        arrayLike.closeArray().closeArray();
        return this;
    }

    public LambdaDslJsonArray eachArrayWithMaxLike(int numberExamples, Integer size,
                                                   Consumer<LambdaDslJsonArray> nestedArray) {
        final PactDslJsonArray arrayLike = pactArray.eachArrayWithMaxLike(numberExamples, size);
        final LambdaDslJsonArray dslArray = new LambdaDslJsonArray(arrayLike);
        nestedArray.accept(dslArray);
        arrayLike.closeArray().closeArray();
        return this;
    }

    public LambdaDslJsonArray eachArrayWithMinLike(Integer size, Consumer<LambdaDslJsonArray> nestedArray) {
        final PactDslJsonArray arrayLike = pactArray.eachArrayWithMinLike(size);
        final LambdaDslJsonArray dslArray = new LambdaDslJsonArray(arrayLike);
        nestedArray.accept(dslArray);
        arrayLike.closeArray().closeArray();
        return this;
    }

    public LambdaDslJsonArray eachArrayWithMinLike(int numberExamples, Integer size,
                                                   Consumer<LambdaDslJsonArray> nestedArray) {
        final PactDslJsonArray arrayLike = pactArray.eachArrayWithMinLike(numberExamples, size);
        final LambdaDslJsonArray dslArray = new LambdaDslJsonArray(arrayLike);
        nestedArray.accept(dslArray);
        arrayLike.closeArray().closeArray();
        return this;
    }

  public LambdaDslJsonArray eachArrayWithMinMaxLike(Integer minSize, Integer maxSize, Consumer<LambdaDslJsonArray> nestedArray) {
    final PactDslJsonArray arrayLike = pactArray.eachArrayWithMinMaxLike(minSize, maxSize);
    final LambdaDslJsonArray dslArray = new LambdaDslJsonArray(arrayLike);
    nestedArray.accept(dslArray);
    arrayLike.closeArray().closeArray();
    return this;
  }

  public LambdaDslJsonArray eachArrayWithMinMaxLike(Integer minSize, Integer maxSize, int numberExamples,
                                                 Consumer<LambdaDslJsonArray> nestedArray) {
    final PactDslJsonArray arrayLike = pactArray.eachArrayWithMinMaxLike(numberExamples, minSize, maxSize);
    final LambdaDslJsonArray dslArray = new LambdaDslJsonArray(arrayLike);
    nestedArray.accept(dslArray);
    arrayLike.closeArray().closeArray();
    return this;
  }

    public DslPart build() {
        return pactArray;
    }
}
