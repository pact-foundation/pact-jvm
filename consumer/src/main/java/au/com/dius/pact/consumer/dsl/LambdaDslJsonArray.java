package au.com.dius.pact.consumer.dsl;

import au.com.dius.pact.core.model.matchingrules.MatchingRule;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.Date;
import java.util.function.Consumer;

public class LambdaDslJsonArray {

    private final PactDslJsonArray pactArray;

    LambdaDslJsonArray(final PactDslJsonArray pactArray) {
        this.pactArray = pactArray;
    }

    /**
     * Get the raw {@link PactDslJsonArray} which is abstracted with {@link PactDslJsonArray}
     */
    public PactDslJsonArray getPactDslJsonArray() {
        return pactArray;
    }

    /**
     * Element that is a JSON object
     */
    public LambdaDslJsonArray object(final Consumer<LambdaDslObject> o) {
        final PactDslJsonBody pactObject = pactArray.object();
        LambdaDslObject object = new LambdaDslObject(pactObject);
        o.accept(object);
        pactObject.closeObject();
        return this;
    }

    /**
     * Element that is a JSON array
     */
    public LambdaDslJsonArray array(final Consumer<LambdaDslJsonArray> a) {
        final PactDslJsonArray pactArray = this.pactArray.array();
        LambdaDslJsonArray array = new LambdaDslJsonArray(pactArray);
        a.accept(array);
        pactArray.closeArray();
        return this;
    }

  /**
   * Array element where order is ignored
   */
  public LambdaDslJsonArray unorderedArray(final Consumer<LambdaDslJsonArray> a) {
    final PactDslJsonArray pactArray = this.pactArray.unorderedArray();
    LambdaDslJsonArray array = new LambdaDslJsonArray(pactArray);
    a.accept(array);
    pactArray.closeArray();
    return this;
  }

  /**
   * Array element of min size where order is ignored
   * @param size
   */
  public LambdaDslJsonArray unorderedMinArray(int size, final Consumer<LambdaDslJsonArray> a) {
    final PactDslJsonArray pactArray = this.pactArray.unorderedMinArray(size);
    LambdaDslJsonArray array = new LambdaDslJsonArray(pactArray);
    a.accept(array);
    pactArray.closeArray();
    return this;
  }

  /**
   * Array element of max size where order is ignored
   * @param size
   */
  public LambdaDslJsonArray unorderedMaxArray(int size, final Consumer<LambdaDslJsonArray> a) {
    final PactDslJsonArray pactArray = this.pactArray.unorderedMaxArray(size);
    LambdaDslJsonArray array = new LambdaDslJsonArray(pactArray);
    a.accept(array);
    pactArray.closeArray();
    return this;
  }

  /**
   * Array element of min and max size where order is ignored
   * @param minSize
   * @param maxSize
   */
  public LambdaDslJsonArray unorderedMinMaxArray(int minSize, int maxSize, final Consumer<LambdaDslJsonArray> a) {
    final PactDslJsonArray pactArray = this.pactArray.unorderedMinMaxArray(minSize, maxSize);
    LambdaDslJsonArray array = new LambdaDslJsonArray(pactArray);
    a.accept(array);
    pactArray.closeArray();
    return this;
  }

    /**
     * Element that must be the specified value
     *
     * @param value string value
     */
    public LambdaDslJsonArray stringValue(final String value) {
        pactArray.stringValue(value);
        return this;
    }

    /**
     * Element that can be any string
     *
     * @param example example value to use for generated bodies
     */
    public LambdaDslJsonArray stringType(final String example) {
        pactArray.stringType(example);
        return this;
    }

    /**
     * Element that must match the regular expression
     *
     * @param regex   regular expression
     * @param example example value to use for generated bodies
     */
    public LambdaDslJsonArray stringMatcher(final String regex, final String example) {
        pactArray.stringMatcher(regex, example);
        return this;
    }

    /**
     * Element that must be the specified number
     *
     * @param value number value
     */
    public LambdaDslJsonArray numberValue(final Number value) {
        pactArray.numberValue(value);
        return this;
    }

    /**
     * Element that can be any number
     *
     * @param example example number to use for generated bodies
     */
    public LambdaDslJsonArray numberType(final Number example) {
        pactArray.numberType(example);
        return this;
    }

    /**
     * Element that must be an integer
     */
    public LambdaDslJsonArray integerType() {
        pactArray.integerType();
        return this;
    }

    /**
     * Element that must be an integer
     *
     * @param example example integer value to use for generated bodies
     */
    public LambdaDslJsonArray integerType(final Long example) {
        pactArray.integerType(example);
        return this;
    }

    /**
     * Element that must be a decimal value
     */
    public LambdaDslJsonArray decimalType() {
        pactArray.decimalType();
        return this;
    }

    /**
     * Element that must be a decimalType value
     *
     * @param example example decimalType value
     */
    public LambdaDslJsonArray decimalType(final BigDecimal example) {
        pactArray.decimalType(example);
        return this;
    }

    /**
     * Attribute that must be a decimalType value
     *
     * @param example example decimalType value
     */
    public LambdaDslJsonArray decimalType(final Double example) {
        pactArray.decimalType(example);
        return this;
    }

    /**
     * Element that must be the specified value
     *
     * @param value boolean value
     */
    public LambdaDslJsonArray booleanValue(final Boolean value) {
        pactArray.booleanValue(value);
        return this;
    }

    /**
     * Element that must be a boolean
     *
     * @param example example boolean to use for generated bodies
     */
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

    /**
     * Element that must match the given timestamp format
     *
     * @param format  timestamp format
     * @param example example date and time to use for generated bodies
     */
    public LambdaDslJsonArray timestamp(final String format, final Instant example) {
        pactArray.timestamp(format, example);
        return this;
    }

    /**
     * Element that must be a numeric identifier
     */
    public LambdaDslJsonArray id() {
        pactArray.id();
        return this;
    }

    /**
     * Element that must be a numeric identifier
     *
     * @param example example id to use for generated bodies
     */
    public LambdaDslJsonArray id(final Long example) {
        pactArray.id(example);
        return this;
    }

    /**
     * Element that must be encoded as an UUID
     */
    public LambdaDslJsonArray uuid() {
        pactArray.uuid();
        return this;
    }

    /**
     * Element that must be encoded as an UUID
     *
     * @param example example UUID to use for generated bodies
     */
    public LambdaDslJsonArray uuid(final String example) {
        pactArray.uuid(example);
        return this;
    }

    /**
     * Element that must be encoded as a hexadecimal value
     */
    public LambdaDslJsonArray hexValue() {
        pactArray.hexValue();
        return this;
    }

    /**
     * Element that must be encoded as a hexadecimal value
     *
     * @param example example value to use for generated bodies
     */
    public LambdaDslJsonArray hexValue(final String example) {
        pactArray.hexValue(example);
        return this;
    }

    /**
     * Element that must be an IP4 address
     */
    public LambdaDslJsonArray ipV4Address() {
        pactArray.ipAddress();
        return this;
    }

    /**
     * Combine all the matchers using AND
     *
     * @param value Attribute example value
     * @param rules Matching rules to apply
     */
    public LambdaDslJsonArray and(Object value, MatchingRule... rules) {
        pactArray.and(value, rules);
        return this;
    }

    /**
     * Combine all the matchers using OR
     *
     * @param value Attribute example value
     * @param rules Matching rules to apply
     */
    public LambdaDslJsonArray or(Object value, MatchingRule... rules) {
        pactArray.or(value, rules);
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
     * @param value Value that each item in the array must match
     */
    public LambdaDslJsonArray eachLike(PactDslJsonRootValue value) {
        pactArray.eachLike(value);
        return this;
    }

    /**
     * Element that is an array where each item must match the following example
     *
     * @param value          Value that each item in the array must match
     * @param numberExamples Number of examples to generate
     */
    public LambdaDslJsonArray eachLike(PactDslJsonRootValue value, int numberExamples) {
        pactArray.eachLike(value, numberExamples);
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
     * @param minSize        minimum size of the array
     * @param maxSize        maximum size of the array
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

    /**
     * Array element where each element of the array is an array and must match the following object
     */
    public LambdaDslJsonArray eachArrayLike(Consumer<LambdaDslJsonArray> nestedArray) {
        final PactDslJsonArray arrayLike = pactArray.eachArrayLike();
        final LambdaDslJsonArray dslArray = new LambdaDslJsonArray(arrayLike);
        nestedArray.accept(dslArray);
        arrayLike.closeArray().closeArray();
        return this;
    }

    /**
     * Array element where each element of the array is an array and must match the following object
     *
     * @param numberExamples number of examples to generate
     */
    public LambdaDslJsonArray eachArrayLike(int numberExamples, Consumer<LambdaDslJsonArray> nestedArray) {
        final PactDslJsonArray arrayLike = pactArray.eachArrayLike(numberExamples);
        final LambdaDslJsonArray dslArray = new LambdaDslJsonArray(arrayLike);
        nestedArray.accept(dslArray);
        arrayLike.closeArray().closeArray();
        return this;
    }

    /**
     * Array element where each element of the array is an array and must match the following object.
     * This will generate 1 example value, if you want to change that number use {@link #eachArrayWithMaxLike(int, Integer, Consumer)}
     *
     * @param size Maximum size of the outer array
     */
    public LambdaDslJsonArray eachArrayWithMaxLike(Integer size, Consumer<LambdaDslJsonArray> nestedArray) {
        final PactDslJsonArray arrayLike = pactArray.eachArrayWithMaxLike(size);
        final LambdaDslJsonArray dslArray = new LambdaDslJsonArray(arrayLike);
        nestedArray.accept(dslArray);
        arrayLike.closeArray().closeArray();
        return this;
    }

    /**
     * Array element where each element of the array is an array and must match the following object
     *
     * @param numberExamples number of examples to generate
     * @param size           Maximum size of the outer array
     */
    public LambdaDslJsonArray eachArrayWithMaxLike(int numberExamples, Integer size,
                                                   Consumer<LambdaDslJsonArray> nestedArray) {
        final PactDslJsonArray arrayLike = pactArray.eachArrayWithMaxLike(numberExamples, size);
        final LambdaDslJsonArray dslArray = new LambdaDslJsonArray(arrayLike);
        nestedArray.accept(dslArray);
        arrayLike.closeArray().closeArray();
        return this;
    }

    /**
     * Array element where each element of the array is an array and must match the following object.
     * This will generate 1 example value, if you want to change that number use {@link #eachArrayWithMinLike(int, Integer, Consumer)}
     *
     * @param size Minimum size of the outer array
     */
    public LambdaDslJsonArray eachArrayWithMinLike(Integer size, Consumer<LambdaDslJsonArray> nestedArray) {
        final PactDslJsonArray arrayLike = pactArray.eachArrayWithMinLike(size);
        final LambdaDslJsonArray dslArray = new LambdaDslJsonArray(arrayLike);
        nestedArray.accept(dslArray);
        arrayLike.closeArray().closeArray();
        return this;
    }

    /**
     * Array element where each element of the array is an array and must match the following object
     *
     * @param numberExamples number of examples to generate
     * @param size           Minimum size of the outer array
     */
    public LambdaDslJsonArray eachArrayWithMinLike(int numberExamples, Integer size,
                                                   Consumer<LambdaDslJsonArray> nestedArray) {
        final PactDslJsonArray arrayLike = pactArray.eachArrayWithMinLike(numberExamples, size);
        final LambdaDslJsonArray dslArray = new LambdaDslJsonArray(arrayLike);
        nestedArray.accept(dslArray);
        arrayLike.closeArray().closeArray();
        return this;
    }

    /**
     * Array element where each element of the array is an array and must match the following object.
     * This will generate 1 example value, if you want to change that number use {@link #eachArrayWithMinMaxLike(Integer, Integer, int, Consumer)}
     *
     * @param minSize minimum size
     * @param maxSize maximum size
     */
    public LambdaDslJsonArray eachArrayWithMinMaxLike(Integer minSize, Integer maxSize, Consumer<LambdaDslJsonArray> nestedArray) {
        final PactDslJsonArray arrayLike = pactArray.eachArrayWithMinMaxLike(minSize, maxSize);
        final LambdaDslJsonArray dslArray = new LambdaDslJsonArray(arrayLike);
        nestedArray.accept(dslArray);
        arrayLike.closeArray().closeArray();
        return this;
    }

    /**
     * Array element where each element of the array is an array and must match the following object
     *
     * @param minSize minimum size
     * @param maxSize maximum size
     */
    public LambdaDslJsonArray eachArrayWithMinMaxLike(Integer minSize, Integer maxSize, int numberExamples,
                                                      Consumer<LambdaDslJsonArray> nestedArray) {
        final PactDslJsonArray arrayLike = pactArray.eachArrayWithMinMaxLike(numberExamples, minSize, maxSize);
        final LambdaDslJsonArray dslArray = new LambdaDslJsonArray(arrayLike);
        nestedArray.accept(dslArray);
        arrayLike.closeArray().closeArray();
        return this;
    }

    /**
     * Date value generated from the provided expression. Will use an ISO format.
     *
     * @param expression Date expression
     */
    public LambdaDslJsonArray dateExpression(String expression) {
        pactArray.dateExpression(expression);
        return this;
    }

    /**
     * Date value generated from the provided expression
     *
     * @param expression Date expression
     * @param format     Date format to use for values
     */
    public LambdaDslJsonArray dateExpression(String expression, String format) {
        pactArray.dateExpression(expression, format);
        return this;
    }

    /**
     * Time value generated from the provided expression. Will use an ISO format.
     *
     * @param expression Time expression
     */
    public LambdaDslJsonArray timeExpression(String expression) {
        pactArray.timeExpression(expression);
        return this;
    }

    /**
     * Time value generated from the provided expression
     *
     * @param expression Time expression
     * @param format     Time format to use for values
     */
    public LambdaDslJsonArray timeExpression(String expression, String format) {
        pactArray.timeExpression(expression, format);
        return this;
    }

    /**
     * Datetime generated from the provided expression. Will use an ISO format.
     *
     * @param expression Datetime expression
     */
    public LambdaDslJsonArray datetimeExpression(String expression) {
        pactArray.datetimeExpression(expression);
        return this;
    }

    /**
     * Datetime generated from the provided expression
     *
     * @param expression Datetime expression
   * @param format Datetime format to use for values
   */
  public LambdaDslJsonArray datetimeExpression(String expression, String format) {
    pactArray.datetimeExpression(expression, format);
    return this;
  }

  public DslPart build() {
    return pactArray;
  }
}
