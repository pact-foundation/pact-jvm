package au.com.dius.pact.consumer.dsl;

import au.com.dius.pact.core.matchers.UrlMatcherSupport;
import au.com.dius.pact.core.model.matchingrules.MatchingRule;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.ZonedDateTime;
import java.util.Arrays;
import java.util.Date;
import java.util.TimeZone;
import java.util.UUID;
import java.util.function.Consumer;

import static au.com.dius.pact.consumer.dsl.Dsl.matcherKey;

public class LambdaDslObject {

    private final PactDslJsonBody object;

    LambdaDslObject(final PactDslJsonBody object) {
        this.object = object;
    }

    /**
     * Get the raw {@link PactDslJsonBody} which is abstracted with {@link LambdaDslObject}
     */
    public PactDslJsonBody getPactDslObject() {
        return object;
    }

    /**
     * Attribute that must be the specified value
     *
     * @param name  attribute name
     * @param value string value
     */
    public LambdaDslObject stringValue(final String name, final String value) {
        object.stringValue(name, value);
        return this;
    }

    /**
     * Attribute that can be any string
     *
     * @param name    attribute name
     * @param example example value to use for generated bodies
     */
    public LambdaDslObject stringType(final String name, final String example) {
        object.stringType(name, example);
        return this;
    }

    /**
     * Attribute that can be any string
     *
     * @param name attribute name
     */
    public LambdaDslObject stringType(final String name) {
        object.stringType(name);
        return this;
    }

    /**
     * Attributes that can be any string
     *
     * @param names attribute names
     */
    public LambdaDslObject stringType(final String... names) {
        object.stringTypes(names);
        return this;
    }

    /**
     * Attribute that must match the regular expression
     *
     * @param name  attribute name
     * @param regex regular expression
     */
    public LambdaDslObject stringMatcher(final String name, final String regex) {
        object.stringMatcher(name, regex);
        return this;
    }

    /**
     * Attribute that must match the regular expression
     *
     * @param name    attribute name
     * @param regex   regular expression
     * @param example example value to use for generated bodies
     */
    public LambdaDslObject stringMatcher(final String name, final String regex, final String example) {
        object.stringMatcher(name, regex, example);
        return this;
    }

    /**
     * Attribute that must be the specified number
     *
     * @param name  attribute name
     * @param value number value
     */
    public LambdaDslObject numberValue(final String name, final Number value) {
        object.numberValue(name, value);
        return this;
    }

    /**
     * Attribute that can be any number
     *
     * @param name    attribute name
     * @param example example number to use for generated bodies
     */
    public LambdaDslObject numberType(final String name, final Number example) {
        object.numberType(name, example);
        return this;
    }

    /**
     * Attributes that can be any number
     *
     * @param names attribute names
     */
    public LambdaDslObject numberType(final String... names) {
        object.numberTypes(names);
        return this;
    }

    /**
     * Attribute that must be an integer
     * @param name attribute name
     * @param example example integer value to use for generated bodies
     */
    public LambdaDslObject integerType(final String name, final Integer example) {
      object.integerType(name, example);
      return this;
    }

    /**
     * Attributes that must be an integer
     * @param names attribute names
     */
    public LambdaDslObject integerType(final String... names) {
      object.integerTypes(names);
      return this;
    }

    /**
     * Attribute that must be a decimalType value
     *
     * @param name    attribute name
     * @param example example decimalType value
     */
    public LambdaDslObject decimalType(final String name, final BigDecimal example) {
        object.decimalType(name, example);
        return this;
    }

    /**
     * Attribute that must be a decimalType value
     *
     * @param name    attribute name
     * @param example example decimalType value
     */
    public LambdaDslObject decimalType(final String name, final Double example) {
        object.decimalType(name, example);
        return this;
    }

    /**
     * Attributes that must be decimal values
     *
     * @param names attribute names
     */
    public LambdaDslObject decimalType(final String... names) {
        object.decimalTypes(names);
        return this;
    }

    /**
     * Attribute that must be the specified boolean
     *
     * @param name  attribute name
     * @param value boolean value
     */
    public LambdaDslObject booleanValue(final String name, final Boolean value) {
        object.booleanValue(name, value);
        return this;
    }

    /**
     * Attribute that must be a boolean
     *
     * @param name    attribute name
     * @param example example boolean to use for generated bodies
     */
    public LambdaDslObject booleanType(final String name, final Boolean example) {
        object.booleanType(name, example);
        return this;
    }

    /**
     * Attributes that must be a boolean
     *
     * @param names attribute names
     */
    public LambdaDslObject booleanType(final String... names) {
        object.booleanTypes(names);
        return this;
    }

    /**
     * Attribute named 'id' that must be a numeric identifier
     */
    public LambdaDslObject id() {
        object.id();
        return this;
    }

    /**
     * Attribute that must be a numeric identifier
     *
     * @param name attribute name
     */
    public LambdaDslObject id(final String name) {
        object.id(name);
        return this;
    }

    /**
     * Attribute that must be a numeric identifier
     *
     * @param name    attribute name
     * @param example example id to use for generated bodies
     */
    public LambdaDslObject id(final String name, Long example) {
        object.id(name, example);
        return this;
    }

    /**
     * Attribute that must be encoded as an UUID
     *
     * @param name attribute name
     */
    public LambdaDslObject uuid(final String name) {
        object.uuid(name);
        return this;
    }

    /**
     * Attribute that must be encoded as an UUID
     *
     * @param name    attribute name
     * @param example example UUID to use for generated bodies
     */
    public LambdaDslObject uuid(final String name, UUID example) {
        object.uuid(name, example);
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
     * @param name     attribute date
     * @param format   date format to match
     * @param example  example date to use for generated values
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
     * @param name     attribute name
     * @param format   time format to match
     * @param example  example time to use for generated values
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
     * @deprecated Use datetime
     */
    @Deprecated
    public LambdaDslObject timestamp() {
        object.datetime("timestamp");
        return this;
    }

    /**
     * Attribute that must be an ISO formatted datetime
     *
     * @param name attribute name
     * @deprecated Use datetime
     */
    @Deprecated
    public LambdaDslObject datetime(String name) {
        object.datetime(name);
        return this;
    }

    /**
     * Attribute that must match the given datetime format
     *
     * @param name   attribute name
     * @param format datetime format
     */
    public LambdaDslObject datetime(String name, String format) {
        object.datetime(name, format);
        return this;
    }

    /**
     * Attribute that must match the given datetime format
     *
     * @param name    attribute name
     * @param format  datetime format
     * @param example example date and time to use for generated bodies
     */
    public LambdaDslObject datetime(String name, String format, Date example) {
        object.datetime(name, format, example);
        return this;
    }

    /**
     * Attribute that must match the given datetime format
     *
     * @param name    attribute name
     * @param format  datetime format
     * @param example example date and time to use for generated bodies
     */
    public LambdaDslObject datetime(String name, String format, Instant example) {
        object.datetime(name, format, example);
        return this;
    }

    /**
     * Attribute that must match the given datetime format
     *
     * @param name     attribute name
     * @param format   datetime format
     * @param example  example date and time to use for generated bodies
     * @param timeZone time zone used for formatting of example date and time
     */
    public LambdaDslObject datetime(String name, String format, Date example, TimeZone timeZone) {
        object.datetime(name, format, example, timeZone);
        return this;
    }

    /**
     * Attribute that must match the given timestamp format
     *
     * @param name    attribute name
     * @param format  datetime format
     * @param example example date and time to use for generated bodies
     */
    public LambdaDslObject datetime(String name, String format, ZonedDateTime example) {
        object.datetime(name, format, Date.from(example.toInstant()), TimeZone.getTimeZone(example.getZone()));
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

    /**
     * Attribute that will have its value injected from the provider state
     *
     * @param name       Attribute name
     * @param expression Expression to be evaluated from the provider state
     * @param example    Example value to be used in the consumer test
     */
    public LambdaDslObject valueFromProviderState(String name, String expression, Object example) {
        object.valueFromProviderState(name, expression, example);
        return this;
    }

    /**
     * Combine all the matchers using AND
     *
     * @param name  Attribute name
     * @param value Attribute example value
     * @param rules Matching rules to apply
     */
    public LambdaDslObject and(String name, Object value, MatchingRule... rules) {
        object.and(name, value, rules);
        return this;
    }

    /**
     * Combine all the matchers using OR
     *
     * @param name  Attribute name
     * @param value Attribute example value
     * @param rules Matching rules to apply
     */
    public LambdaDslObject or(String name, Object value, MatchingRule... rules) {
        object.or(name, value, rules);
        return this;
    }

    /**
     * Attribute that is an array
     *
     * @param name field name
     */
    public LambdaDslObject array(final String name, final Consumer<LambdaDslJsonArray> array) {
        final PactDslJsonArray pactArray = object.array(name);
        final LambdaDslJsonArray dslArray = new LambdaDslJsonArray(pactArray);
        array.accept(dslArray);
        pactArray.closeArray();
        return this;
    }

    /**
     * Attribute that is a JSON object
     *
     * @param name field name
     */
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
     * @param name  field name
     * @param value Value that each item in the array must match
     */
    public LambdaDslObject eachLike(String name, PactDslJsonRootValue value) {
        object.eachLike(name, value);
        return this;
    }

    /**
     * Attribute that is an array where each item is a primitive that must match the provided value
     *
     * @param name           field name
     * @param value          Value that each item in the array must match
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
     *
     * @param name           field name
     * @param size           minimum size of the array
     * @param value          Value to use to match each item
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
     *
     * @param name           field name
     * @param size           maximum size of the array
     * @param value          Value to use to match each item
     * @param numberExamples number of examples to generate
     */
    public LambdaDslObject maxArrayLike(String name, Integer size, PactDslJsonRootValue value, int numberExamples) {
        object.maxArrayLike(name, size, value, numberExamples);
        return this;
    }

    /**
     * Attribute that is an array with a minimum and maximum size where each item must match the following example
     *
     * @param name    field name
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
     * @param minSize        minimum size of the array
     * @param maxSize        maximum size of the array
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
     *
     * @param name           field name
     * @param minSize        minimum size of the array
     * @param maxSize        maximum size of the array
     * @param value          Value to use to match each item
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

    /**
     * Array field where each element is an array and must match the following object
     *
     * @param name field name
     */
    public LambdaDslObject eachArrayLike(String name, Consumer<LambdaDslJsonArray> nestedArray) {
        final PactDslJsonArray arrayLike = object.eachArrayLike(name);
        final LambdaDslJsonArray dslArray = new LambdaDslJsonArray(arrayLike);
        nestedArray.accept(dslArray);
        arrayLike.closeArray().closeArray();
        return this;
    }

    /**
     * Array field where each element is an array and must match the following object
     *
     * @param name           field name
     * @param numberExamples number of examples to generate
     */
    public LambdaDslObject eachArrayLike(String name, int numberExamples, Consumer<LambdaDslJsonArray> nestedArray) {
        final PactDslJsonArray arrayLike = object.eachArrayLike(name, numberExamples);
        final LambdaDslJsonArray dslArray = new LambdaDslJsonArray(arrayLike);
        nestedArray.accept(dslArray);
        arrayLike.closeArray().closeArray();
        return this;
    }

    /**
     * Array field where each element is an array and must match the following object.
     * This will generate 1 example value, if you want to change that number use {@link #eachArrayWithMaxLike(String, int, Integer, Consumer)}
     *
     * @param name field name
     * @param size Maximum size of the outer array
     */
    public LambdaDslObject eachArrayWithMaxLike(String name, Integer size, Consumer<LambdaDslJsonArray> nestedArray) {
        final PactDslJsonArray arrayLike = object.eachArrayWithMaxLike(name, size);
        final LambdaDslJsonArray dslArray = new LambdaDslJsonArray(arrayLike);
        nestedArray.accept(dslArray);
        arrayLike.closeArray().closeArray();
        return this;
    }

    /**
     * Array field where each element is an array and must match the following object
     *
     * @param name           field name
     * @param numberExamples number of examples to generate
     * @param size           Maximum size of the outer array
     */
    public LambdaDslObject eachArrayWithMaxLike(String name, int numberExamples, Integer size,
                                                Consumer<LambdaDslJsonArray> nestedArray) {
        final PactDslJsonArray arrayLike = object.eachArrayWithMaxLike(name, numberExamples, size);
        final LambdaDslJsonArray dslArray = new LambdaDslJsonArray(arrayLike);
        nestedArray.accept(dslArray);
        arrayLike.closeArray().closeArray();
        return this;
    }

    /**
     * Array field where each element is an array and must match the following object.
     * This will generate 1 example value, if you want to change that number use {@link #eachArrayWithMinLike(String, int, Integer, Consumer)}
     *
     * @param name field name
     * @param size Minimum size of the outer array
     */
    public LambdaDslObject eachArrayWithMinLike(String name, Integer size, Consumer<LambdaDslJsonArray> nestedArray) {
        final PactDslJsonArray arrayLike = object.eachArrayWithMinLike(name, size);
        final LambdaDslJsonArray dslArray = new LambdaDslJsonArray(arrayLike);
        nestedArray.accept(dslArray);
        arrayLike.closeArray().closeArray();
        return this;
    }

    /**
     * Array field where each element is an array and must match the following object
     *
     * @param name           field name
     * @param numberExamples number of examples to generate
     * @param size           Minimum size of the outer array
     */
    public LambdaDslObject eachArrayWithMinLike(String name, int numberExamples, Integer size,
                                                Consumer<LambdaDslJsonArray> nestedArray) {
        final PactDslJsonArray arrayLike = object.eachArrayWithMinLike(name, numberExamples, size);
        final LambdaDslJsonArray dslArray = new LambdaDslJsonArray(arrayLike);
        nestedArray.accept(dslArray);
        arrayLike.closeArray().closeArray();
        return this;
    }

    /**
     * Array field where each element is an array and must match the following object.
     * This will generate 1 example value, if you want to change that number use {@link #eachArrayWithMinMaxLike(String, Integer, Integer, int, Consumer)}
     *
     * @param name    field name
     * @param minSize minimum size
     * @param maxSize maximum size
     */
    public LambdaDslObject eachArrayWithMinMaxLike(String name, Integer minSize, Integer maxSize, Consumer<LambdaDslJsonArray> nestedArray) {
        final PactDslJsonArray arrayLike = object.eachArrayWithMinMaxLike(name, minSize, maxSize);
        final LambdaDslJsonArray dslArray = new LambdaDslJsonArray(arrayLike);
        nestedArray.accept(dslArray);
        arrayLike.closeArray().closeArray();
        return this;
    }

    /**
     * Array field where each element is an array and must match the following object
     *
     * @param name           field name
     * @param numberExamples number of examples to generate
     * @param minSize        minimum size
     * @param maxSize        maximum size
     */
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
     *
     * @param exampleKey Example key to use for generating bodies
     * @param value      Value to use for matching and generated bodies
     */
    public LambdaDslObject eachKeyLike(String exampleKey, PactDslJsonRootValue value) {
        object.eachKeyLike(exampleKey, value);
        return this;
    }

    /**
     * Attribute whose values are generated from the provided expression. Will use an ISO format.
     *
     * @param name       Attribute name
     * @param expression Date expression
     */
    public LambdaDslObject dateExpression(String name, String expression) {
        object.dateExpression(name, expression);
        return this;
    }

    /**
     * Attribute whose values are generated from the provided expression
     *
     * @param name       Attribute name
     * @param expression Date expression
     * @param format     Date format to use for values
     */
    public LambdaDslObject dateExpression(String name, String expression, String format) {
        object.dateExpression(name, expression, format);
        return this;
    }

    /**
     * Attribute whose values are generated from the provided expression. Will use an ISO format.
     *
     * @param name       Attribute name
     * @param expression Time expression
     */
    public LambdaDslObject timeExpression(String name, String expression) {
        object.timeExpression(name, expression);
        return this;
    }

    /**
     * Attribute whose values are generated from the provided expression
     *
     * @param name       Attribute name
     * @param expression Time expression
     * @param format     Time format to use for values
     */
    public LambdaDslObject timeExpression(String name, String expression, String format) {
        object.timeExpression(name, expression, format);
        return this;
    }

    /**
     * Attribute whose values are generated from the provided expression. Will use an ISO format.
     *
     * @param name       Attribute name
     * @param expression Datetime expression
     */
    public LambdaDslObject datetimeExpression(String name, String expression) {
        object.datetimeExpression(name, expression);
        return this;
    }

    /**
     * Attribute whose values are generated from the provided expression
     *
     * @param name       Attribute name
     * @param expression Datetime expression
     * @param format     Datetime format to use for values
     */
    public LambdaDslObject datetimeExpression(String name, String expression, String format) {
        object.datetimeExpression(name, expression, format);
        return this;
    }

  /**
   * Array field where order is ignored
   * @param name field name
   */
  public LambdaDslObject unorderedArray(String name, final Consumer<LambdaDslJsonArray> nestedArray) {
    final PactDslJsonArray pactArray = object.unorderedArray(name);
    LambdaDslJsonArray array = new LambdaDslJsonArray(pactArray);
    nestedArray.accept(array);
    pactArray.closeArray();
    return this;
  }

  /**
   * Array field of min size where order is ignored
   * @param name field name
   * @param size minimum size
   */
  public LambdaDslObject unorderedMinArray(String name, int size, final Consumer<LambdaDslJsonArray> nestedArray) {
    final PactDslJsonArray pactArray = object.unorderedMinArray(name, size);
    LambdaDslJsonArray array = new LambdaDslJsonArray(pactArray);
    nestedArray.accept(array);
    pactArray.closeArray();
    return this;
  }

  /**
   * Array field of max size where order is ignored
   * @param name field name
   * @param size maximum size
   */
  public LambdaDslObject unorderedMaxArray(String name, int size, final Consumer<LambdaDslJsonArray> nestedArray) {
    final PactDslJsonArray pactArray = object.unorderedMaxArray(name, size);
    LambdaDslJsonArray array = new LambdaDslJsonArray(pactArray);
    nestedArray.accept(array);
    pactArray.closeArray();
    return this;
  }

  /**
   * Array field of min and max size where order is ignored
   * @param name field name
   * @param minSize minimum size
   * @param maxSize maximum size
   */
  public LambdaDslObject unorderedMinMaxArray(String name, int minSize, int maxSize, final Consumer<LambdaDslJsonArray> nestedArray) {
    final PactDslJsonArray pactArray = object.unorderedMinMaxArray(name, minSize, maxSize);
    LambdaDslJsonArray array = new LambdaDslJsonArray(pactArray);
    nestedArray.accept(array);
    pactArray.closeArray();
    return this;
  }

  /**
   * Matches a URL that is composed of a base path and a sequence of path expressions
   * @param name Attribute name
   * @param basePath The base path for the URL (like "http://localhost:8080/") which will be excluded from the matching
   * @param pathFragments Series of path fragments to match on. These can be strings or regular expressions.
   */
  public LambdaDslObject matchUrl(String name, String basePath, Object... pathFragments) {
    object.matchUrl(name, basePath, pathFragments);
    return this;
  }

  /**
   * Matches a URL that is composed of a base path and a sequence of path expressions. The base path of the
   * mock server will be used.
   * @param name Attribute name
   * @param pathFragments Series of path fragments to match on. These can be strings or regular expressions.
   */
  public LambdaDslObject matchUrl2(String name, Object... pathFragments) {
    object.matchUrl2(name, pathFragments);
    return this;
  }

  /**
   * Matches the items in an array against a number of variants. Matching is successful if each variant
   * occurs once in the array. Variants may be objects containing matching rules.
   * @param name Attribute name
   */
  public LambdaDslObject arrayContaining(String name, Consumer<LambdaDslJsonArray> nestedArray) {
    PactDslJsonArray arrayContaining = (PactDslJsonArray) object.arrayContaining(name);
    final LambdaDslJsonArray dslObject = new LambdaDslJsonArray(arrayContaining);
    nestedArray.accept(dslObject);
    arrayContaining.closeArray();
    return this;
  }
}
