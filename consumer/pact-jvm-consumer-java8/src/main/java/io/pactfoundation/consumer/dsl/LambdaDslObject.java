package io.pactfoundation.consumer.dsl;

import au.com.dius.pact.consumer.dsl.PactDslJsonArray;
import au.com.dius.pact.consumer.dsl.PactDslJsonBody;
import au.com.dius.pact.consumer.dsl.PactDslJsonRootValue;
import au.com.dius.pact.core.model.matchingrules.MatchingRule;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.ZonedDateTime;
import java.util.Date;
import java.util.TimeZone;
import java.util.UUID;
import java.util.function.Consumer;

public class LambdaDslObject {

    private final PactDslJsonBody object;

    LambdaDslObject(final PactDslJsonBody object) {
        this.object = object;
    }

    public PactDslJsonBody getPactDslObject() {
        return object;
    }

    public LambdaDslObject stringValue(final String name, final String value) {
        object.stringValue(name, value);
        return this;
    }

    public LambdaDslObject stringType(final String name, final String example) {
        object.stringType(name, example);
        return this;
    }

    public LambdaDslObject stringType(final String name) {
        object.stringType(name);
        return this;
    }

    public LambdaDslObject stringType(final String... names) {
        object.stringType(names);
        return this;
    }

    public LambdaDslObject stringMatcher(final String name, final String example) {
        object.stringMatcher(name, example);
        return this;
    }

    public LambdaDslObject stringMatcher(final String name, final String regex, final String value) {
        object.stringMatcher(name, regex, value);
        return this;
    }

    public LambdaDslObject numberValue(final String name, final Number value) {
        object.numberValue(name, value);
        return this;
    }

    public LambdaDslObject numberType(final String name, final Number example) {
        object.numberType(name, example);
        return this;
    }

    public LambdaDslObject numberType(final String... names) {
        object.numberType(names);
        return this;
    }

    public LambdaDslObject decimalType(final String name, final BigDecimal value) {
        object.decimalType(name, value);
        return this;
    }

    public LambdaDslObject decimalType(final String name, final Double example) {
        object.decimalType(name, example);
        return this;
    }

    public LambdaDslObject decimalType(final String... names) {
        object.decimalType(names);
        return this;
    }

    public LambdaDslObject booleanValue(final String name, final Boolean value) {
        object.booleanValue(name, value);
        return this;
    }

    public LambdaDslObject booleanType(final String name, final Boolean example) {
        object.booleanType(name, example);
        return this;
    }

    public LambdaDslObject booleanType(final String... names) {
        object.booleanType(names);
        return this;
    }

    public LambdaDslObject id() {
        object.id();
        return this;
    }

    public LambdaDslObject id(final String name) {
        object.id(name);
        return this;
    }

    public LambdaDslObject id(final String name, Long id) {
        object.id(name, id);
        return this;
    }

    public LambdaDslObject uuid(final String name) {
        object.uuid(name);
        return this;
    }

    public LambdaDslObject uuid(final String name, UUID id) {
        object.uuid(name, id);
        return this;
    }

    /**
     * Attribute named 'date' that must be formatted as an ISO date
     */
    public LambdaDslObject date() {
        object.date();
        return this;
    }

    /**
     * Attribute that must be formatted as an ISO date
     *
     * @param name attribute name
     */
    public LambdaDslObject date(String name) {
        object.date(name);
        return this;
    }

    /**
     * Attribute that must match the provided date format
     *
     * @param name   attribute date
     * @param format date format to match
     */
    public LambdaDslObject date(String name, String format) {
        object.date(name, format);
        return this;
    }

    /**
     * Attribute that must match the provided date format
     *
     * @param name    attribute date
     * @param format  date format to match
     * @param example example date to use for generated values
     */
    public LambdaDslObject date(String name, String format, Date example) {
        object.date(name, format, example);
        return this;
    }

    /**
     * Attribute that must match the provided date format
     *
     * @param name    attribute date
     * @param format  date format to match
     * @param example example date to use for generated values
     * @param timeZone time zone used for formatting of example date
     */
    public LambdaDslObject date(String name, String format, Date example, TimeZone timeZone) {
        object.date(name, format, example, timeZone);
        return this;
    }

    /**
     * Attribute that must match the provided date format
     *
     * @param name    attribute date
     * @param format  date format to match
     * @param example example date to use for generated values
     */
    public LambdaDslObject date(String name, String format, ZonedDateTime example) {
        object.date(name, format, Date.from(example.toInstant()), TimeZone.getTimeZone(example.getZone()));
        return this;
    }

    /**
     * Attribute named 'time' that must be an ISO formatted time
     */
    public LambdaDslObject time() {
        object.time();
        return this;
    }

    /**
     * Attribute that must be an ISO formatted time
     *
     * @param name attribute name
     */
    public LambdaDslObject time(String name) {
        object.time(name);
        return this;
    }

    /**
     * Attribute that must match the provided time format
     *
     * @param name   attribute time
     * @param format time format to match
     */
    public LambdaDslObject time(String name, String format) {
        object.time(name, format);
        return this;
    }

    /**
     * Attribute that must match the provided time format
     *
     * @param name    attribute name
     * @param format  time format to match
     * @param example example time to use for generated values
     */
    public LambdaDslObject time(String name, String format, Date example) {
        object.time(name, format, example);
        return this;
    }
    /**
     * Attribute that must match the provided time format
     *
     * @param name    attribute name
     * @param format  time format to match
     * @param example example time to use for generated values
     * @param timeZone time zone used for formatting of example time
     */
    public LambdaDslObject time(String name, String format, Date example, TimeZone timeZone) {
        object.time(name, format, example, timeZone);
        return this;
    }

    /**
     * Attribute that must match the provided time format
     *
     * @param name    attribute name
     * @param format  time format to match
     * @param example example time to use for generated values
     */
    public LambdaDslObject time(String name, String format, ZonedDateTime example) {
        object.time(name, format, Date.from(example.toInstant()), TimeZone.getTimeZone(example.getZone()));
        return this;
    }

    /**
     * Attribute named 'timestamp' that must be an ISO formatted timestamp
     */
    public LambdaDslObject timestamp() {
        object.timestamp();
        return this;
    }

    /**
     * Attribute that must be an ISO formatted timestamp
     *
     * @param name attribute name
     */
    public LambdaDslObject timestamp(String name) {
        object.timestamp(name);
        return this;
    }

    /**
     * Attribute that must match the given timestamp format
     *
     * @param name   attribute name
     * @param format timestamp format
     */
    public LambdaDslObject timestamp(String name, String format) {
        object.timestamp(name, format);
        return this;
    }

    /**
     * Attribute that must match the given timestamp format
     *
     * @param name    attribute name
     * @param format  timestamp format
     * @param example example date and time to use for generated bodies
     */
    public LambdaDslObject timestamp(String name, String format, Date example) {
        object.timestamp(name, format, example);
        return this;
    }

    /**
     * Attribute that must match the given timestamp format
     *
     * @param name    attribute name
     * @param format  timestamp format
     * @param example example date and time to use for generated bodies
     */
    public LambdaDslObject timestamp(String name, String format, Instant example) {
        object.timestamp(name, format, example);
        return this;
    }

    /**
     * Attribute that must match the given timestamp format
     *
     * @param name    attribute name
     * @param format  timestamp format
     * @param example example date and time to use for generated bodies
     * @param timeZone time zone used for formatting of example date and time
     */
    public LambdaDslObject timestamp(String name, String format, Date example, TimeZone timeZone){
        object.timestamp(name, format, example, timeZone);
        return this;
    }

    /**
     * Attribute that must match the given timestamp format
     *
     * @param name    attribute name
     * @param format  timestamp format
     * @param example example date and time to use for generated bodies
     */
    public LambdaDslObject timestamp(String name, String format, ZonedDateTime example) {
        object.timestamp(name, format, Date.from(example.toInstant()), TimeZone.getTimeZone(example.getZone()));
        return this;
    }

    /**
     * Attribute that must be an IP4 address
     *
     * @param name attribute name
     */
    public LambdaDslObject ipV4Address(String name) {
        object.ipAddress(name);
        return this;
    }

    /** Combine all the matchers using AND
    * @param name  Attribute name
    * @param value Attribute example value
    * @param rules Matching rules to apply
    * @return
    */
    public LambdaDslObject and(String name, Object value, MatchingRule... rules) {
        object.and(name, value, rules);
        return this;
    }

    /**
    * Combine all the matchers using OR
    * @param name  Attribute name
    * @param value Attribute example value
    * @param rules Matching rules to apply
    * @return
    */
    public LambdaDslObject or(String name, Object value, MatchingRule... rules) {
        object.or(name, value, rules);
        return this;
    }

    public LambdaDslObject array(final String name, final Consumer<LambdaDslJsonArray> array) {
        final PactDslJsonArray pactArray = object.array(name);
        final LambdaDslJsonArray dslArray = new LambdaDslJsonArray(pactArray);
        array.accept(dslArray);
        pactArray.closeArray();
        return this;
    }

    public LambdaDslObject object(final String name, final Consumer<LambdaDslObject> nestedObject) {
        final PactDslJsonBody pactObject = object.object(name);
        final LambdaDslObject dslObject = new LambdaDslObject(pactObject);
        nestedObject.accept(dslObject);
        pactObject.closeObject();
        return this;
    }

    /**
     * Attribute that is an array where each item must match the following example
     *
     * @param name field name
     */
    public LambdaDslObject eachLike(String name, Consumer<LambdaDslObject> nestedObject) {
        final PactDslJsonBody arrayLike = object.eachLike(name);
        final LambdaDslObject dslObject = new LambdaDslObject(arrayLike);
        nestedObject.accept(dslObject);
        arrayLike.closeArray();
        return this;
    }

    /**
     * Attribute that is an array where each item must match the following example
     *
     * @param name           field name
     * @param numberExamples number of examples to generate
     */
    public LambdaDslObject eachLike(String name, int numberExamples, Consumer<LambdaDslObject> nestedObject) {
        final PactDslJsonBody arrayLike = object.eachLike(name, numberExamples);
        final LambdaDslObject dslObject = new LambdaDslObject(arrayLike);
        nestedObject.accept(dslObject);
        arrayLike.closeArray();
        return this;
    }

    /**
     * Attribute that is an array where each item is a primitive that must match the provided value
     *
     * @param name field name
     * @param value Value that each item in the array must match
     */
    public LambdaDslObject eachLike(String name, PactDslJsonRootValue value) {
        object.eachLike(name, value);
        return this;
    }

    /**
     * Attribute that is an array where each item is a primitive that must match the provided value
     *
     * @param name field name
     * @param value Value that each item in the array must match
     * @param numberExamples Number of examples to generate
     */
    public LambdaDslObject eachLike(String name, PactDslJsonRootValue value, int numberExamples) {
        object.eachLike(name, value, numberExamples);
        return this;
    }

    /**
     * Attribute that is an array with a minimum size where each item must match the following example
     *
     * @param name field name
     * @param size minimum size of the array
     */
    public LambdaDslObject minArrayLike(String name, Integer size, Consumer<LambdaDslObject> nestedObject) {
        final PactDslJsonBody minArrayLike = object.minArrayLike(name, size);
        final LambdaDslObject dslObject = new LambdaDslObject(minArrayLike);
        nestedObject.accept(dslObject);
        minArrayLike.closeArray();
        return this;
    }

    /**
     * Attribute that is an array with a minimum size where each item must match the following example
     *
     * @param name           field name
     * @param size           minimum size of the array
     * @param numberExamples number of examples to generate
     */
    public LambdaDslObject minArrayLike(String name, Integer size, int numberExamples,
                                        Consumer<LambdaDslObject> nestedObject) {
        final PactDslJsonBody minArrayLike = object.minArrayLike(name, size, numberExamples);
        final LambdaDslObject dslObject = new LambdaDslObject(minArrayLike);
        nestedObject.accept(dslObject);
        minArrayLike.closeArray();
        return this;
    }

    /**
     * Attribute that is an array of values with a minimum size that are not objects where each item must match
     * the following example
     * @param name field name
     * @param size minimum size of the array
     * @param value Value to use to match each item
     * @param numberExamples number of examples to generate
     */
    public LambdaDslObject minArrayLike(String name, Integer size, PactDslJsonRootValue value, int numberExamples) {
      object.minArrayLike(name, size, value, numberExamples);
      return this;
    }

    /**
     * Attribute that is an array with a maximum size where each item must match the following example
     *
     * @param name field name
     * @param size maximum size of the array
     */
    public LambdaDslObject maxArrayLike(String name, Integer size, Consumer<LambdaDslObject> nestedObject) {
        final PactDslJsonBody maxArrayLike = object.maxArrayLike(name, size);
        final LambdaDslObject dslObject = new LambdaDslObject(maxArrayLike);
        nestedObject.accept(dslObject);
        maxArrayLike.closeArray();
        return this;
    }

    /**
     * Attribute that is an array with a maximum size where each item must match the following example
     *
     * @param name           field name
     * @param size           maximum size of the array
     * @param numberExamples number of examples to generate
     */
    public LambdaDslObject maxArrayLike(String name, Integer size, int numberExamples,
                                        Consumer<LambdaDslObject> nestedObject) {
        final PactDslJsonBody maxArrayLike = object.maxArrayLike(name, size, numberExamples);
        final LambdaDslObject dslObject = new LambdaDslObject(maxArrayLike);
        nestedObject.accept(dslObject);
        maxArrayLike.closeArray();
        return this;
    }

  /**
   * Attribute that is an array of values with a maximum size that are not objects where each item must match the
   * following example
   * @param name field name
   * @param size maximum size of the array
   * @param value Value to use to match each item
   * @param numberExamples number of examples to generate
   */
  public LambdaDslObject maxArrayLike(String name, Integer size, PactDslJsonRootValue value, int numberExamples) {
    object.maxArrayLike(name, size, value, numberExamples);
    return this;
  }

  /**
   * Attribute that is an array with a minimum and maximum size where each item must match the following example
   *
   * @param name field name
   * @param minSize minimum size of the array
   * @param maxSize maximum size of the array
   */
  public LambdaDslObject minMaxArrayLike(String name, Integer minSize, Integer maxSize, Consumer<LambdaDslObject> nestedObject) {
    final PactDslJsonBody maxArrayLike = object.minMaxArrayLike(name, minSize, maxSize);
    final LambdaDslObject dslObject = new LambdaDslObject(maxArrayLike);
    nestedObject.accept(dslObject);
    maxArrayLike.closeArray();
    return this;
  }

  /**
   * Attribute that is an array with a minimum and maximum size where each item must match the following example
   *
   * @param name           field name
   * @param minSize minimum size of the array
   * @param maxSize maximum size of the array
   * @param numberExamples number of examples to generate
   */
  public LambdaDslObject minMaxArrayLike(String name, Integer minSize, Integer maxSize, int numberExamples,
                                      Consumer<LambdaDslObject> nestedObject) {
    final PactDslJsonBody maxArrayLike = object.minMaxArrayLike(name, minSize, maxSize, numberExamples);
    final LambdaDslObject dslObject = new LambdaDslObject(maxArrayLike);
    nestedObject.accept(dslObject);
    maxArrayLike.closeArray();
    return this;
  }

  /**
   * Attribute that is an array of values with a minimum and maximum size that are not objects where each item must
   * match the following example
   * @param name field name
   * @param minSize minimum size of the array
   * @param maxSize maximum size of the array
   * @param value Value to use to match each item
   * @param numberExamples number of examples to generate
   */
  public LambdaDslObject minMaxArrayLike(String name, Integer minSize, Integer maxSize, PactDslJsonRootValue value,
                                         int numberExamples) {
    object.minMaxArrayLike(name, minSize, maxSize, value, numberExamples);
    return this;
  }

    /**
     * Sets the field to a null value
     *
     * @param fieldName field name
     */
    public LambdaDslObject nullValue(String fieldName) {
        object.nullValue(fieldName);
        return this;
    }

    public LambdaDslObject eachArrayLike(String name, Consumer<LambdaDslJsonArray> nestedArray) {
        final PactDslJsonArray arrayLike = object.eachArrayLike(name);
        final LambdaDslJsonArray dslArray = new LambdaDslJsonArray(arrayLike);
        nestedArray.accept(dslArray);
        arrayLike.closeArray().closeArray();
        return this;
    }

    public LambdaDslObject eachArrayLike(String name, int numberExamples, Consumer<LambdaDslJsonArray> nestedArray) {
        final PactDslJsonArray arrayLike = object.eachArrayLike(name, numberExamples);
        final LambdaDslJsonArray dslArray = new LambdaDslJsonArray(arrayLike);
        nestedArray.accept(dslArray);
        arrayLike.closeArray().closeArray();
        return this;
    }

    public LambdaDslObject eachArrayWithMaxLike(String name, Integer size, Consumer<LambdaDslJsonArray> nestedArray) {
        final PactDslJsonArray arrayLike = object.eachArrayWithMaxLike(name, size);
        final LambdaDslJsonArray dslArray = new LambdaDslJsonArray(arrayLike);
        nestedArray.accept(dslArray);
        arrayLike.closeArray().closeArray();
        return this;
    }


    public LambdaDslObject eachArrayWithMaxLike(String name, int numberExamples, Integer size,
                                                Consumer<LambdaDslJsonArray> nestedArray) {
        final PactDslJsonArray arrayLike = object.eachArrayWithMaxLike(name, numberExamples, size);
        final LambdaDslJsonArray dslArray = new LambdaDslJsonArray(arrayLike);
        nestedArray.accept(dslArray);
        arrayLike.closeArray().closeArray();
        return this;
    }


    public LambdaDslObject eachArrayWithMinLike(String name, Integer size, Consumer<LambdaDslJsonArray> nestedArray) {
        final PactDslJsonArray arrayLike = object.eachArrayWithMinLike(name, size);
        final LambdaDslJsonArray dslArray = new LambdaDslJsonArray(arrayLike);
        nestedArray.accept(dslArray);
        arrayLike.closeArray().closeArray();
        return this;
    }


    public LambdaDslObject eachArrayWithMinLike(String name, int numberExamples, Integer size,
                                                Consumer<LambdaDslJsonArray> nestedArray) {
        final PactDslJsonArray arrayLike = object.eachArrayWithMinLike(name, numberExamples, size);
        final LambdaDslJsonArray dslArray = new LambdaDslJsonArray(arrayLike);
        nestedArray.accept(dslArray);
        arrayLike.closeArray().closeArray();
        return this;
    }

  public LambdaDslObject eachArrayWithMinMaxLike(String name, Integer minSize, Integer maxSize, Consumer<LambdaDslJsonArray> nestedArray) {
    final PactDslJsonArray arrayLike = object.eachArrayWithMinMaxLike(name, minSize, maxSize);
    final LambdaDslJsonArray dslArray = new LambdaDslJsonArray(arrayLike);
    nestedArray.accept(dslArray);
    arrayLike.closeArray().closeArray();
    return this;
  }

  public LambdaDslObject eachArrayWithMinMaxLike(String name, Integer minSize, Integer maxSize, int numberExamples,
                                              Consumer<LambdaDslJsonArray> nestedArray) {
    final PactDslJsonArray arrayLike = object.eachArrayWithMinMaxLike(name, numberExamples, minSize, maxSize);
    final LambdaDslJsonArray dslArray = new LambdaDslJsonArray(arrayLike);
    nestedArray.accept(dslArray);
    arrayLike.closeArray().closeArray();
    return this;
  }

    /**
     * Accepts any key, and each key is mapped to a list of items that must match the following object definition.
     * Note: this needs the Java system property "pact.matching.wildcard" set to value "true" when the pact file is verified.
     *
     * @param exampleKey Example key to use for generating bodies
     */
    public LambdaDslObject eachKeyMappedToAnArrayLike(String exampleKey, Consumer<LambdaDslObject> nestedObject) {
        final PactDslJsonBody objectLike = object.eachKeyMappedToAnArrayLike(exampleKey);
        final LambdaDslObject dslObject = new LambdaDslObject(objectLike);
        nestedObject.accept(dslObject);
        objectLike.closeObject().closeArray();
        return this;
    }

    /**
     * Accepts any key, and each key is mapped to a map that must match the following object definition.
     * Note: this needs the Java system property "pact.matching.wildcard" set to value "true" when the pact file is verified.
     *
     * @param exampleKey Example key to use for generating bodies
     */
    public LambdaDslObject eachKeyLike(String exampleKey, Consumer<LambdaDslObject> nestedObject) {
        final PactDslJsonBody objectLike = object.eachKeyLike(exampleKey);
        final LambdaDslObject dslObject = new LambdaDslObject(objectLike);
        nestedObject.accept(dslObject);
        objectLike.closeObject();
        return this;
    }

    /**
     * Accepts any key, and each key is mapped to a map that must match the provided object definition
     * Note: this needs the Java system property "pact.matching.wildcard" set to value "true" when the pact file is verified.
     * @param exampleKey Example key to use for generating bodies
     * @param value Value to use for matching and generated bodies
     */
    public LambdaDslObject eachKeyLike(String exampleKey, PactDslJsonRootValue value) {
        object.eachKeyLike(exampleKey, value);
        return this;
    }

  /**
   * Attribute whose values are generated from the provided expression. Will use an ISO format.
   * @param name Attribute name
   * @param expression Date expression
   */
  public LambdaDslObject dateExpression(String name, String expression) {
    object.dateExpression(name, expression);
    return this;
  }

  /**
   * Attribute whose values are generated from the provided expression
   * @param name Attribute name
   * @param expression Date expression
   * @param format Date format to use for values
   */
  public LambdaDslObject dateExpression(String name, String expression, String format) {
    object.dateExpression(name, expression, format);
    return this;
  }

  /**
   * Attribute whose values are generated from the provided expression. Will use an ISO format.
   * @param name Attribute name
   * @param expression Time expression
   */
  public LambdaDslObject timeExpression(String name, String expression) {
    object.timeExpression(name, expression);
    return this;
  }

  /**
   * Attribute whose values are generated from the provided expression
   * @param name Attribute name
   * @param expression Time expression
   * @param format Time format to use for values
   */
  public LambdaDslObject timeExpression(String name, String expression, String format) {
    object.timeExpression(name, expression, format);
    return this;
  }

  /**
   * Attribute whose values are generated from the provided expression. Will use an ISO format.
   * @param name Attribute name
   * @param expression Datetime expression
   */
  public LambdaDslObject datetimeExpression(String name, String expression) {
    object.datetimeExpression(name, expression);
    return this;
  }

  /**
   * Attribute whose values are generated from the provided expression
   * @param name Attribute name
   * @param expression Datetime expression
   * @param format Datetime format to use for values
   */
  public LambdaDslObject datetimeExpression(String name, String expression, String format) {
    object.datetimeExpression(name, expression, format);
    return this;
  }
}
