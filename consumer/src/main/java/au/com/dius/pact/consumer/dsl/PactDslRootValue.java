package au.com.dius.pact.consumer.dsl;

import au.com.dius.pact.consumer.InvalidMatcherException;
import au.com.dius.pact.core.model.generators.Category;
import au.com.dius.pact.core.model.generators.DateGenerator;
import au.com.dius.pact.core.model.generators.DateTimeGenerator;
import au.com.dius.pact.core.model.generators.ProviderStateGenerator;
import au.com.dius.pact.core.model.generators.RandomDecimalGenerator;
import au.com.dius.pact.core.model.generators.RandomHexadecimalGenerator;
import au.com.dius.pact.core.model.generators.RandomIntGenerator;
import au.com.dius.pact.core.model.generators.RandomStringGenerator;
import au.com.dius.pact.core.model.generators.TimeGenerator;
import au.com.dius.pact.core.model.generators.UuidGenerator;
import au.com.dius.pact.core.model.matchingrules.MatchingRule;
import au.com.dius.pact.core.model.matchingrules.MatchingRuleGroup;
import au.com.dius.pact.core.model.matchingrules.NumberTypeMatcher;
import au.com.dius.pact.core.model.matchingrules.RuleLogic;
import au.com.dius.pact.core.model.matchingrules.TypeMatcher;
import au.com.dius.pact.core.support.Json;
import au.com.dius.pact.core.support.expressions.DataType;
import au.com.dius.pact.core.support.json.JsonValue;
import org.apache.commons.lang3.time.DateFormatUtils;
import org.apache.commons.lang3.time.FastDateFormat;
import org.json.JSONObject;

import java.math.BigDecimal;
import java.util.Arrays;
import java.util.Date;
import java.util.UUID;

/**
 * Matcher to create a plain root matching strategy. Used with text/plain to match regex responses
 */
public class PactDslRootValue extends DslPart {

  private static final String USE_PACT_DSL_JSON_ARRAY_FOR_ARRAYS = "Use PactDslJsonArray for arrays";
  private static final String USE_PACT_DSL_JSON_BODY_FOR_OBJECTS = "Use PactDslJsonBody for objects";
  private static final String EXAMPLE = "Example \"";

  private JsonValue value;
  private boolean encodeJson = false;

  public PactDslRootValue() {
    super("", "");
  }

  @Override
  public void putObjectPrivate(DslPart object) {
    throw new UnsupportedOperationException();
  }

  @Override
  public void putArrayPrivate(DslPart object) {
    throw new UnsupportedOperationException();
  }

  @Override
  public JsonValue getBody() {
    return value;
  }

  @Override
  public void setBody(JsonValue body) {
    value = body;
  }

  /**
   * @deprecated Use PactDslJsonArray for arrays
   */
  @Override
  public PactDslJsonArray array(String name) {
    throw new UnsupportedOperationException(USE_PACT_DSL_JSON_ARRAY_FOR_ARRAYS);
  }

  /**
   * @deprecated Use PactDslJsonArray for arrays
   */
  @Override
  public PactDslJsonArray array() {
    throw new UnsupportedOperationException(USE_PACT_DSL_JSON_ARRAY_FOR_ARRAYS);
  }

  /**
   * @deprecated Use PactDslJsonArray for arrays
   */
  @Override
  public DslPart closeArray() {
    throw new UnsupportedOperationException(USE_PACT_DSL_JSON_ARRAY_FOR_ARRAYS);
  }

  /**
   * @deprecated Use PactDslJsonArray for arrays
   */
  @Override
  public PactDslJsonBody eachLike(String name) {
    throw new UnsupportedOperationException(USE_PACT_DSL_JSON_ARRAY_FOR_ARRAYS);
  }

  @Override
  public PactDslJsonBody eachLike(String name, DslPart object) {
    throw new UnsupportedOperationException(USE_PACT_DSL_JSON_ARRAY_FOR_ARRAYS);
  }

  /**
   * @deprecated Use PactDslJsonArray for arrays
   */
  @Override
  public PactDslJsonBody eachLike(int numberExamples) {
    throw new UnsupportedOperationException(USE_PACT_DSL_JSON_ARRAY_FOR_ARRAYS);
  }

  /**
   * @deprecated Use PactDslJsonArray for arrays
   */
  @Override
  public PactDslJsonBody eachLike(String name, int numberExamples) {
    throw new UnsupportedOperationException(USE_PACT_DSL_JSON_ARRAY_FOR_ARRAYS);
  }

  /**
   * @deprecated Use PactDslJsonArray for arrays
   */
  @Override
  public PactDslJsonBody eachLike() {
    throw new UnsupportedOperationException(USE_PACT_DSL_JSON_ARRAY_FOR_ARRAYS);
  }

  @Override
  public PactDslJsonArray eachLike(DslPart object) {
    throw new UnsupportedOperationException(USE_PACT_DSL_JSON_ARRAY_FOR_ARRAYS);
  }

  /**
   * @deprecated Use PactDslJsonArray for arrays
   */
  @Override
  public PactDslJsonBody minArrayLike(String name, int size) {
    throw new UnsupportedOperationException(USE_PACT_DSL_JSON_ARRAY_FOR_ARRAYS);
  }

  /**
   * @deprecated Use PactDslJsonArray for arrays
   */
  @Override
  public PactDslJsonBody minArrayLike(int size) {
    throw new UnsupportedOperationException(USE_PACT_DSL_JSON_ARRAY_FOR_ARRAYS);
  }

  @Override
  public PactDslJsonBody minArrayLike(String name, int size, DslPart object) {
    throw new UnsupportedOperationException(USE_PACT_DSL_JSON_ARRAY_FOR_ARRAYS);
  }

  @Override
  public PactDslJsonArray minArrayLike(int size, DslPart object) {
    throw new UnsupportedOperationException(USE_PACT_DSL_JSON_ARRAY_FOR_ARRAYS);
  }

  /**
   * @deprecated Use PactDslJsonArray for arrays
   */
  @Override
  public PactDslJsonBody minArrayLike(String name, int size, int numberExamples) {
    throw new UnsupportedOperationException(USE_PACT_DSL_JSON_ARRAY_FOR_ARRAYS);
  }

  /**
   * @deprecated Use PactDslJsonArray for arrays
   */
  @Override
  public PactDslJsonBody minArrayLike(int size, int numberExamples) {
    throw new UnsupportedOperationException(USE_PACT_DSL_JSON_ARRAY_FOR_ARRAYS);
  }

  /**
   * @deprecated Use PactDslJsonArray for arrays
   */
  @Override
  public PactDslJsonBody maxArrayLike(String name, int size) {
    throw new UnsupportedOperationException(USE_PACT_DSL_JSON_ARRAY_FOR_ARRAYS);
  }

  /**
   * @deprecated Use PactDslJsonArray for arrays
   */
  @Override
  public PactDslJsonBody maxArrayLike(int size) {
    throw new UnsupportedOperationException(USE_PACT_DSL_JSON_ARRAY_FOR_ARRAYS);
  }

  @Override
  public PactDslJsonBody maxArrayLike(String name, int size, DslPart object) {
    throw new UnsupportedOperationException(USE_PACT_DSL_JSON_ARRAY_FOR_ARRAYS);
  }

  @Override
  public PactDslJsonArray maxArrayLike(int size, DslPart object) {
    throw new UnsupportedOperationException(USE_PACT_DSL_JSON_ARRAY_FOR_ARRAYS);
  }

  /**
   * @deprecated Use PactDslJsonArray for arrays
   */
  @Override
  public PactDslJsonBody maxArrayLike(String name, int size, int numberExamples) {
    throw new UnsupportedOperationException(USE_PACT_DSL_JSON_ARRAY_FOR_ARRAYS);
  }

  /**
   * @deprecated Use PactDslJsonArray for arrays
   */
  @Override
  public PactDslJsonBody maxArrayLike(int size, int numberExamples) {
    throw new UnsupportedOperationException(USE_PACT_DSL_JSON_ARRAY_FOR_ARRAYS);
  }

  /**
   * @deprecated Use PactDslJsonArray for arrays
   */
  @Override
  public PactDslJsonBody minMaxArrayLike(String name, int minSize, int maxSize) {
    throw new UnsupportedOperationException(USE_PACT_DSL_JSON_ARRAY_FOR_ARRAYS);
  }

  @Override
  public PactDslJsonBody minMaxArrayLike(String name, int minSize, int maxSize, DslPart object) {
    return null;
  }

  /**
   * @deprecated Use PactDslJsonArray for arrays
   */
  @Override
  public PactDslJsonBody minMaxArrayLike(int minSize, int maxSize) {
    throw new UnsupportedOperationException(USE_PACT_DSL_JSON_ARRAY_FOR_ARRAYS);
  }

  @Override
  public PactDslJsonArray minMaxArrayLike(int minSize, int maxSize, DslPart object) {
    throw new UnsupportedOperationException(USE_PACT_DSL_JSON_ARRAY_FOR_ARRAYS);
  }

  /**
   * @deprecated Use PactDslJsonArray for arrays
   */
  @Override
  public PactDslJsonBody minMaxArrayLike(String name, int minSize, int maxSize, int numberExamples) {
    throw new UnsupportedOperationException(USE_PACT_DSL_JSON_ARRAY_FOR_ARRAYS);
  }

  /**
   * @deprecated Use PactDslJsonArray for arrays
   */
  @Override
  public PactDslJsonBody minMaxArrayLike(int minSize, int maxSize, int numberExamples) {
    throw new UnsupportedOperationException(USE_PACT_DSL_JSON_ARRAY_FOR_ARRAYS);
  }

  /**
   * @deprecated Use PactDslJsonBody for objects
   */
  @Override
  public PactDslJsonBody object(String name) {
    throw new UnsupportedOperationException(USE_PACT_DSL_JSON_BODY_FOR_OBJECTS);
  }

  /**
   * @deprecated Use PactDslJsonBody for objects
   */
  @Override
  public PactDslJsonBody object() {
    throw new UnsupportedOperationException(USE_PACT_DSL_JSON_BODY_FOR_OBJECTS);
  }

  /**
   * @deprecated Use PactDslJsonBody for objects
   */
  @Override
  public DslPart closeObject() {
    throw new UnsupportedOperationException(USE_PACT_DSL_JSON_BODY_FOR_OBJECTS);
  }

  @Override
  public DslPart close() {
    getMatchers().applyMatcherRootPrefix("$");
    getGenerators().applyRootPrefix("$");
    return this;
  }

  /**
   * Value that can be any string
   */
  public static PactDslRootValue stringType() {
    PactDslRootValue value = new PactDslRootValue();
    value.getGenerators().addGenerator(Category.BODY, "", new RandomStringGenerator(20));
    value.setValue("string");
    value.setMatcher(TypeMatcher.INSTANCE);
    return value;
  }

  /**
   * Value that can be any string
   *
   * @param example example value to use for generated bodies
   */
  public static PactDslRootValue stringType(String example) {
    PactDslRootValue value = new PactDslRootValue();
    value.setValue(example);
    value.setMatcher(TypeMatcher.INSTANCE);
    return value;
  }

  /**
   * Value that can be any number
   */
  public static PactDslRootValue numberType() {
    PactDslRootValue value = new PactDslRootValue();
    value.getGenerators().addGenerator(Category.BODY, "", new RandomIntGenerator(0, Integer.MAX_VALUE));
    value.setValue(100);
    value.setMatcher(TypeMatcher.INSTANCE);
    return value;
  }

  /**
   * Value that can be any number
   * @param number example number to use for generated bodies
   */
  public static PactDslRootValue numberType(Number number) {
    PactDslRootValue value = new PactDslRootValue();
    value.setValue(number);
    value.setMatcher(TypeMatcher.INSTANCE);
    return value;
  }

  /**
   * Value that must be an integer
   */
  public static PactDslRootValue integerType() {
    PactDslRootValue value = new PactDslRootValue();
    value.getGenerators().addGenerator(Category.BODY, "", new RandomIntGenerator(0, Integer.MAX_VALUE));
    value.setValue(100);
    value.setMatcher(new NumberTypeMatcher(NumberTypeMatcher.NumberType.INTEGER));
    return value;
  }

  /**
   * Value that must be an integer
   * @param number example integer value to use for generated bodies
   */
  public static PactDslRootValue integerType(Long number) {
    PactDslRootValue value = new PactDslRootValue();
    value.setValue(number);
    value.setMatcher(new NumberTypeMatcher(NumberTypeMatcher.NumberType.INTEGER));
    return value;
  }

  /**
   * Value that must be an integer
   * @param number example integer value to use for generated bodies
   */
  public static PactDslRootValue integerType(int number) {
    PactDslRootValue value = new PactDslRootValue();
    value.setValue(number);
    value.setMatcher(new NumberTypeMatcher(NumberTypeMatcher.NumberType.INTEGER));
    return value;
  }

  /**
   * Value that must be a decimal value
   */
  public static PactDslRootValue decimalType() {
    PactDslRootValue value = new PactDslRootValue();
    value.getGenerators().addGenerator(Category.BODY, "", new RandomDecimalGenerator(10));
    value.setValue(100);
    value.setMatcher(new NumberTypeMatcher(NumberTypeMatcher.NumberType.DECIMAL));
    return value;
  }

  /**
   * Value that must be a decimalType value
   * @param number example decimalType value
   */
  public static PactDslRootValue decimalType(BigDecimal number) {
    PactDslRootValue value = new PactDslRootValue();
    value.setValue(number);
    value.setMatcher(new NumberTypeMatcher(NumberTypeMatcher.NumberType.DECIMAL));
    return value;
  }

  /**
   * Value that must be a decimalType value
   * @param number example decimalType value
   */
  public static PactDslRootValue decimalType(Double number) {
    PactDslRootValue value = new PactDslRootValue();
    value.setValue(number);
    value.setMatcher(new NumberTypeMatcher(NumberTypeMatcher.NumberType.DECIMAL));
    return value;
  }

  /**
   * Value that must be a boolean
   */
  public static PactDslRootValue booleanType() {
    return booleanType(true);
  }

  /**
   * Value that must be a boolean
   * @param example example boolean to use for generated bodies
   */
  public static PactDslRootValue booleanType(Boolean example) {
    PactDslRootValue value = new PactDslRootValue();
    value.setValue(example);
    value.setMatcher(TypeMatcher.INSTANCE);
    return value;
  }

  /**
   * Value that must match the regular expression
   * @param regex regular expression
   * @param value example value to use for generated bodies
   */
  public static PactDslRootValue stringMatcher(String regex, String value) {
    if (!value.matches(regex)) {
      throw new InvalidMatcherException(EXAMPLE + value + "\" does not match regular expression \"" +
              regex + "\"");
    }
    PactDslRootValue rootValue = new PactDslRootValue();
    rootValue.setValue(value);
    rootValue.setMatcher(rootValue.regexp(regex));
    return rootValue;
  }

  /**
   * Value that must be an ISO formatted timestamp
   */
  public static PactDslRootValue timestamp() {
    return timestamp(DateFormatUtils.ISO_DATETIME_FORMAT.getPattern());
  }

  /**
   * Value that must match the given timestamp format
   * @param format timestamp format
   */
  public static PactDslRootValue timestamp(String format) {
    PactDslRootValue value = new PactDslRootValue();
    value.getGenerators().addGenerator(Category.BODY, "", new DateTimeGenerator(format));
    FastDateFormat instance = FastDateFormat.getInstance(format);
    value.setValue(instance.format(new Date(DATE_2000)));
    value.setMatcher(value.matchTimestamp(format));
    return value;
  }

  /**
   * Value that must match the given timestamp format
   * @param format timestamp format
   * @param example example date and time to use for generated bodies
   */
  public static PactDslRootValue timestamp(String format, Date example) {
    FastDateFormat instance = FastDateFormat.getInstance(format);
    PactDslRootValue value = new PactDslRootValue();
    value.setValue(instance.format(example));
    value.setMatcher(value.matchTimestamp(format));
    return value;
  }

  /**
   * Value that must be formatted as an ISO date
   */
  public static PactDslRootValue date() {
    return date(DateFormatUtils.ISO_DATE_FORMAT.getPattern());
  }

  /**
   * Value that must match the provided date format
   * @param format date format to match
   */
  public static PactDslRootValue date(String format) {
    FastDateFormat instance = FastDateFormat.getInstance(format);
    PactDslRootValue value = new PactDslRootValue();
    value.getGenerators().addGenerator(Category.BODY, "", new DateGenerator(format));
    value.setValue(instance.format(new Date(DATE_2000)));
    value.setMatcher(value.matchDate(format));
    return value;
  }

  /**
   * Value that must match the provided date format
   * @param format date format to match
   * @param example example date to use for generated values
   */
  public static PactDslRootValue date(String format, Date example) {
    FastDateFormat instance = FastDateFormat.getInstance(format);
    PactDslRootValue value = new PactDslRootValue();
    value.setValue(instance.format(example));
    value.setMatcher(value.matchDate(format));
    return value;
  }

  /**
   * Value that must be an ISO formatted time
   */
  public static PactDslRootValue time() {
    return time(DateFormatUtils.ISO_TIME_FORMAT.getPattern());
  }

  /**
   * Value that must match the given time format
   * @param format time format to match
   */
  public static PactDslRootValue time(String format) {
    FastDateFormat instance = FastDateFormat.getInstance(format);
    PactDslRootValue value = new PactDslRootValue();
    value.getGenerators().addGenerator(Category.BODY, "", new TimeGenerator(format));
    value.setValue(instance.format(new Date(DATE_2000)));
    value.setMatcher(value.matchTime(format));
    return value;
  }

  /**
   * Value that must match the given time format
   * @param format time format to match
   * @param example example time to use for generated bodies
   */
  public static PactDslRootValue time(String format, Date example) {
    FastDateFormat instance = FastDateFormat.getInstance(format);
    PactDslRootValue value = new PactDslRootValue();
    value.setValue(instance.format(example));
    value.setMatcher(value.matchTime(format));
    return value;
  }

  /**
   * Value that must be an IP4 address
   */
  public static PactDslRootValue ipAddress() {
    PactDslRootValue value = new PactDslRootValue();
    value.setValue("127.0.0.1");
    value.setMatcher(value.regexp("(\\d{1,3}\\.)+\\d{1,3}"));
    return value;
  }

  /**
   * Value that must be a numeric identifier
   */
  public static PactDslRootValue id() {
    return numberType();
  }

  /**
   * Value that must be a numeric identifier
   * @param id example id to use for generated bodies
   */
  public static PactDslRootValue id(Long id) {
    return numberType(id);
  }

  /**
   * Value that must be encoded as a hexadecimal value
   */
  public static PactDslRootValue hexValue() {
    PactDslRootValue value = new PactDslRootValue();
    value.getGenerators().addGenerator(Category.BODY, "", new RandomHexadecimalGenerator(10));
    value.setValue("1234a");
    value.setMatcher(value.regexp("[0-9a-fA-F]+"));
    return value;
  }

  /**
   * Value that must be encoded as a hexadecimal value
   * @param hexValue example value to use for generated bodies
   */
  public static PactDslRootValue hexValue(String hexValue) {
    if (!hexValue.matches(DslPart.Companion.getHEXADECIMAL().getPattern())) {
      throw new InvalidMatcherException(EXAMPLE + hexValue + "\" is not a hexadecimal value");
    }
    PactDslRootValue value = new PactDslRootValue();
    value.setValue(hexValue);
    value.setMatcher(value.regexp("[0-9a-fA-F]+"));
    return value;
  }

  /**
   * Value that must be encoded as an UUID
   */
  public static PactDslRootValue uuid() {
    PactDslRootValue value = new PactDslRootValue();
    value.getGenerators().addGenerator(Category.BODY, "", UuidGenerator.INSTANCE);
    value.setValue("e2490de5-5bd3-43d5-b7c4-526e33f71304");
    value.setMatcher(value.regexp(DslPart.Companion.getUUID_REGEX().getPattern()));
    return value;
  }

  /**
   * Value that must be encoded as an UUID
   * @param uuid example UUID to use for generated bodies
   */
  public static PactDslRootValue uuid(UUID uuid) {
    return uuid(uuid.toString());
  }

  /**
   * Value that must be encoded as an UUID
   * @param uuid example UUID to use for generated bodies
   */
  public static PactDslRootValue uuid(String uuid) {
    if (!uuid.matches(DslPart.Companion.getUUID_REGEX().getPattern())) {
      throw new InvalidMatcherException(EXAMPLE + uuid + "\" is not an UUID");
    }

    PactDslRootValue value = new PactDslRootValue();
    value.setValue(uuid);
    value.setMatcher(value.regexp(DslPart.Companion.getUUID_REGEX().getPattern()));
    return value;
  }

  public void setValue(Object value) {
    this.value = Json.toJson(value);
  }

  public void setMatcher(MatchingRule matcher) {
    getMatchers().addRule(matcher);
  }

  /**
   * @deprecated Use PactDslJsonArray for arrays
   */
  @Override
  public PactDslJsonArray eachArrayLike(String name) {
    throw new UnsupportedOperationException(USE_PACT_DSL_JSON_ARRAY_FOR_ARRAYS);
  }

  /**
   * @deprecated Use PactDslJsonArray for arrays
   */
  @Override
  public PactDslJsonArray eachArrayLike(int numberExamples) {
    throw new UnsupportedOperationException(USE_PACT_DSL_JSON_ARRAY_FOR_ARRAYS);
  }

  /**
   * @deprecated Use PactDslJsonArray for arrays
   */
  @Override
  public PactDslJsonArray eachArrayWithMaxLike(String name, int size) {
    throw new UnsupportedOperationException(USE_PACT_DSL_JSON_ARRAY_FOR_ARRAYS);
  }

  /**
   * @deprecated Use PactDslJsonArray for arrays
   */
  @Override
  public PactDslJsonArray eachArrayWithMaxLike(int size) {
    throw new UnsupportedOperationException(USE_PACT_DSL_JSON_ARRAY_FOR_ARRAYS);
  }

  /**
   * @deprecated Use PactDslJsonArray for arrays
   */
  @Override
  public PactDslJsonArray eachArrayWithMaxLike(String name, int numberExamples, int size) {
    throw new UnsupportedOperationException(USE_PACT_DSL_JSON_ARRAY_FOR_ARRAYS);
  }

  /**
   * @deprecated Use PactDslJsonArray for arrays
   */
  @Override
  public PactDslJsonArray eachArrayWithMaxLike(int numberExamples, int size) {
    throw new UnsupportedOperationException(USE_PACT_DSL_JSON_ARRAY_FOR_ARRAYS);
  }

  /**
   * @deprecated Use PactDslJsonArray for arrays
   */
  @Override
  public PactDslJsonArray eachArrayWithMinLike(String name, int size) {
    throw new UnsupportedOperationException(USE_PACT_DSL_JSON_ARRAY_FOR_ARRAYS);
  }

  /**
   * @deprecated Use PactDslJsonArray for arrays
   */
  @Override
  public PactDslJsonArray eachArrayWithMinLike(int size) {
    throw new UnsupportedOperationException(USE_PACT_DSL_JSON_ARRAY_FOR_ARRAYS);
  }

  /**
   * @deprecated Use PactDslJsonArray for arrays
   */
  @Override
  public PactDslJsonArray eachArrayWithMinLike(String name, int numberExamples, int size) {
    throw new UnsupportedOperationException(USE_PACT_DSL_JSON_ARRAY_FOR_ARRAYS);
  }

  /**
   * @deprecated Use PactDslJsonArray for arrays
   */
  @Override
  public PactDslJsonArray eachArrayWithMinLike(int numberExamples, int size) {
    throw new UnsupportedOperationException(USE_PACT_DSL_JSON_ARRAY_FOR_ARRAYS);
  }

  /**
   * @deprecated Use PactDslJsonArray for arrays
   */
  @Override
  public PactDslJsonArray eachArrayWithMinMaxLike(String name, int minSize, int maxSize) {
    throw new UnsupportedOperationException(USE_PACT_DSL_JSON_ARRAY_FOR_ARRAYS);
  }

  /**
   * @deprecated Use PactDslJsonArray for arrays
   */
  @Override
  public PactDslJsonArray eachArrayWithMinMaxLike(int minSize, int maxSize) {
    throw new UnsupportedOperationException(USE_PACT_DSL_JSON_ARRAY_FOR_ARRAYS);
  }

  /**
   * @deprecated Use PactDslJsonArray for arrays
   */
  @Override
  public PactDslJsonArray eachArrayWithMinMaxLike(String name, int numberExamples, int minSize, int maxSize) {
    throw new UnsupportedOperationException(USE_PACT_DSL_JSON_ARRAY_FOR_ARRAYS);
  }

  /**
   * @deprecated Use PactDslJsonArray for arrays
   */
  @Override
  public PactDslJsonArray eachArrayWithMinMaxLike(int numberExamples, int minSize, int maxSize) {
    throw new UnsupportedOperationException(USE_PACT_DSL_JSON_ARRAY_FOR_ARRAYS);
  }

  /**
   * @deprecated Use PactDslJsonArray for arrays
   */
  @Override
  public PactDslJsonArray eachArrayLike(String name, int numberExamples) {
    throw new UnsupportedOperationException(USE_PACT_DSL_JSON_ARRAY_FOR_ARRAYS);
  }

  /**
   * @deprecated Use PactDslJsonArray for arrays
   */
  @Override
  public PactDslJsonArray eachArrayLike() {
    throw new UnsupportedOperationException(USE_PACT_DSL_JSON_ARRAY_FOR_ARRAYS);
  }

  /**
   * Combine all the matchers using AND
   * @param example Attribute example value
   * @param rules Matching rules to apply
   */
  public static PactDslRootValue and(Object example, MatchingRule... rules) {
    PactDslRootValue value = new PactDslRootValue();
    if (example != null) {
      value.setValue(example);
    } else {
      value.setValue(JSONObject.NULL);
    }
    value.getMatchers().setRules("", new MatchingRuleGroup(Arrays.asList(rules), RuleLogic.AND));
    return value;
  }

  /**
   * Combine all the matchers using OR
   * @param example Attribute name
   * @param rules Matching rules to apply
   */
  public static PactDslRootValue or(Object example, MatchingRule... rules) {
    PactDslRootValue value = new PactDslRootValue();
    if (example != null) {
      value.setValue(example);
    } else {
      value.setValue(JSONObject.NULL);
    }
    value.getMatchers().setRules("", new MatchingRuleGroup(Arrays.asList(rules), RuleLogic.OR));
    return value;
  }

  /**
   * @deprecated Use PactDslJsonArray for arrays
   */
  @Override
  @Deprecated
  public PactDslJsonArray unorderedArray(String name) {
    throw new UnsupportedOperationException(USE_PACT_DSL_JSON_ARRAY_FOR_ARRAYS);
  }

  /**
   * @deprecated Use PactDslJsonArray for arrays
   */
  @Override
  @Deprecated
  public PactDslJsonArray unorderedArray() {
    throw new UnsupportedOperationException(USE_PACT_DSL_JSON_ARRAY_FOR_ARRAYS);
  }

  /**
   * @deprecated Use PactDslJsonArray for arrays
   */
  @Override
  @Deprecated
  public PactDslJsonArray unorderedMinArray(String name, int size) {
    throw new UnsupportedOperationException(USE_PACT_DSL_JSON_ARRAY_FOR_ARRAYS);
  }

  /**
   * @deprecated Use PactDslJsonArray for arrays
   */
  @Override
  @Deprecated
  public PactDslJsonArray unorderedMinArray(int size) {
    throw new UnsupportedOperationException(USE_PACT_DSL_JSON_ARRAY_FOR_ARRAYS);
  }

  /**
   * @deprecated Use PactDslJsonArray for arrays
   */
  @Override
  @Deprecated
  public PactDslJsonArray unorderedMaxArray(String name, int size) {
    throw new UnsupportedOperationException(USE_PACT_DSL_JSON_ARRAY_FOR_ARRAYS);
  }

  /**
   * @deprecated Use PactDslJsonArray for arrays
   */
  @Override
  @Deprecated
  public PactDslJsonArray unorderedMaxArray(int size) {
    throw new UnsupportedOperationException(USE_PACT_DSL_JSON_ARRAY_FOR_ARRAYS);
  }

  /**
   * @deprecated Use PactDslJsonArray for arrays
   */
  @Override
  @Deprecated
  public PactDslJsonArray unorderedMinMaxArray(String name, int minSize, int maxSize) {
    throw new UnsupportedOperationException(USE_PACT_DSL_JSON_ARRAY_FOR_ARRAYS);
  }

  /**
   * @deprecated Use PactDslJsonArray for arrays
   */
  @Override
  @Deprecated
  public PactDslJsonArray unorderedMinMaxArray(int minSize, int maxSize) {
    throw new UnsupportedOperationException(USE_PACT_DSL_JSON_ARRAY_FOR_ARRAYS);
  }

  /**
   * Adds a value that will have it's value injected from the provider state
   * @param expression Expression to be evaluated from the provider state
   * @param example Example value to be used in the consumer test
   */
  public static PactDslRootValue valueFromProviderState(String expression, Object example) {
    PactDslRootValue value = new PactDslRootValue();
    value.getGenerators().addGenerator(Category.BODY, "", new ProviderStateGenerator(expression, DataType.from(example)));
    value.setValue(example);
    return value;
  }

  @Override
  public DslPart matchUrl(String name, String basePath, Object... pathFragments) {
    throw new UnsupportedOperationException("matchUrl is not currently supported for PactDslRootValue");
  }

  @Override
  public DslPart matchUrl(String basePath, Object... pathFragments) {
    throw new UnsupportedOperationException("matchUrl is not currently supported for PactDslRootValue");
  }

  @Override
  public DslPart matchUrl2(String name, Object... pathFragments) {
    throw new UnsupportedOperationException("matchUrl2 is not currently supported for PactDslRootValue");
  }

  @Override
  public DslPart matchUrl2(Object... pathFragments) {
    throw new UnsupportedOperationException("matchUrl2 is not currently supported for PactDslRootValue");
  }

  @Override
  public DslPart arrayContaining(String name) {
    throw new UnsupportedOperationException("arrayContaining is not currently supported for PactDslRootValue");
  }
}
