package au.com.dius.pact.consumer.dsl;

import au.com.dius.pact.consumer.InvalidMatcherException;
import au.com.dius.pact.core.matchers.UrlMatcherSupport;
import au.com.dius.pact.core.model.generators.Category;
import au.com.dius.pact.core.model.generators.DateGenerator;
import au.com.dius.pact.core.model.generators.DateTimeGenerator;
import au.com.dius.pact.core.model.generators.MockServerURLGenerator;
import au.com.dius.pact.core.model.generators.ProviderStateGenerator;
import au.com.dius.pact.core.model.generators.RandomDecimalGenerator;
import au.com.dius.pact.core.model.generators.RandomHexadecimalGenerator;
import au.com.dius.pact.core.model.generators.RandomIntGenerator;
import au.com.dius.pact.core.model.generators.RandomStringGenerator;
import au.com.dius.pact.core.model.generators.RegexGenerator;
import au.com.dius.pact.core.model.generators.TimeGenerator;
import au.com.dius.pact.core.model.generators.UuidGenerator;
import au.com.dius.pact.core.model.matchingrules.MatchingRule;
import au.com.dius.pact.core.model.matchingrules.MatchingRuleGroup;
import au.com.dius.pact.core.model.matchingrules.NumberTypeMatcher;
import au.com.dius.pact.core.model.matchingrules.RegexMatcher;
import au.com.dius.pact.core.model.matchingrules.RuleLogic;
import au.com.dius.pact.core.model.matchingrules.TypeMatcher;
import au.com.dius.pact.core.support.Json;
import au.com.dius.pact.core.support.expressions.DataType;
import au.com.dius.pact.core.support.json.JsonValue;
import com.mifmif.common.regex.Generex;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.time.DateFormatUtils;
import org.apache.commons.lang3.time.FastDateFormat;
import org.json.JSONObject;

import java.math.BigDecimal;
import java.util.Arrays;
import java.util.Date;
import java.util.UUID;

public class PactDslJsonRootValue extends DslPart {

  private static final String USE_PACT_DSL_JSON_ARRAY_FOR_ARRAYS = "Use PactDslJsonArray for arrays";
  private static final String USE_PACT_DSL_JSON_BODY_FOR_OBJECTS = "Use PactDslJsonBody for objects";
  private static final String EXAMPLE = "Example \"";

  private Object value;

  public PactDslJsonRootValue() {
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
    return Json.toJson(value);
  }

  /**
   * @deprecated Use PactDslJsonArray for arrays
   */
  @Override
  @Deprecated
  public PactDslJsonArray array(String name) {
    throw new UnsupportedOperationException(USE_PACT_DSL_JSON_ARRAY_FOR_ARRAYS);
  }

  /**
   * @deprecated Use PactDslJsonArray for arrays
   */
  @Deprecated
  @Override
  public PactDslJsonArray array() {
    throw new UnsupportedOperationException(USE_PACT_DSL_JSON_ARRAY_FOR_ARRAYS);
  }

  /**
   * @deprecated Use PactDslJsonArray for arrays
   */
  @Override
  @Deprecated
  public DslPart closeArray() {
    throw new UnsupportedOperationException(USE_PACT_DSL_JSON_ARRAY_FOR_ARRAYS);
  }

  /**
   * @deprecated Use PactDslJsonArray for arrays
   */
  @Override
  @Deprecated
  public PactDslJsonBody eachLike(String name) {
    throw new UnsupportedOperationException(USE_PACT_DSL_JSON_ARRAY_FOR_ARRAYS);
  }

  @Override
  @Deprecated
  public PactDslJsonBody eachLike(String name, DslPart object) {
    throw new UnsupportedOperationException(USE_PACT_DSL_JSON_ARRAY_FOR_ARRAYS);
  }

  /**
   * @deprecated Use PactDslJsonArray for arrays
   */
  @Override
  @Deprecated
  public PactDslJsonBody eachLike(int numberExamples) {
    throw new UnsupportedOperationException(USE_PACT_DSL_JSON_ARRAY_FOR_ARRAYS);
  }

  /**
   * @deprecated Use PactDslJsonArray for arrays
   */
  @Override
  @Deprecated
  public PactDslJsonBody eachLike(String name, int numberExamples) {
    throw new UnsupportedOperationException(USE_PACT_DSL_JSON_ARRAY_FOR_ARRAYS);
  }

  /**
   * @deprecated Use PactDslJsonArray for arrays
   */
  @Override
  @Deprecated
  public PactDslJsonBody eachLike() {
    throw new UnsupportedOperationException(USE_PACT_DSL_JSON_ARRAY_FOR_ARRAYS);
  }

  @Override
  @Deprecated
  public PactDslJsonArray eachLike(DslPart object) {
    throw new UnsupportedOperationException(USE_PACT_DSL_JSON_ARRAY_FOR_ARRAYS);
  }

  /**
   * @deprecated Use PactDslJsonArray for arrays
   */
  @Override
  @Deprecated
  public PactDslJsonBody minArrayLike(String name, int size) {
    throw new UnsupportedOperationException(USE_PACT_DSL_JSON_ARRAY_FOR_ARRAYS);
  }

  /**
   * @deprecated Use PactDslJsonArray for arrays
   */
  @Override
  @Deprecated
  public PactDslJsonBody minArrayLike(int size) {
    throw new UnsupportedOperationException(USE_PACT_DSL_JSON_ARRAY_FOR_ARRAYS);
  }

  @Override
  @Deprecated
  public PactDslJsonBody minArrayLike(String name, int size, DslPart object) {
    throw new UnsupportedOperationException(USE_PACT_DSL_JSON_ARRAY_FOR_ARRAYS);
  }

  @Override
  @Deprecated
  public PactDslJsonArray minArrayLike(int size, DslPart object) {
    throw new UnsupportedOperationException(USE_PACT_DSL_JSON_ARRAY_FOR_ARRAYS);
  }

  /**
   * @deprecated Use PactDslJsonArray for arrays
   */
  @Override
  @Deprecated
  public PactDslJsonBody minArrayLike(String name, int size, int numberExamples) {
    throw new UnsupportedOperationException(USE_PACT_DSL_JSON_ARRAY_FOR_ARRAYS);
  }

  /**
   * @deprecated Use PactDslJsonArray for arrays
   */
  @Override
  @Deprecated
  public PactDslJsonBody minArrayLike(int size, int numberExamples) {
    throw new UnsupportedOperationException(USE_PACT_DSL_JSON_ARRAY_FOR_ARRAYS);
  }

  /**
   * @deprecated Use PactDslJsonArray for arrays
   */
  @Override
  @Deprecated
  public PactDslJsonBody maxArrayLike(String name, int size) {
    throw new UnsupportedOperationException(USE_PACT_DSL_JSON_ARRAY_FOR_ARRAYS);
  }

  /**
   * @deprecated Use PactDslJsonArray for arrays
   */
  @Override
  @Deprecated
  public PactDslJsonBody maxArrayLike(int size) {
    throw new UnsupportedOperationException(USE_PACT_DSL_JSON_ARRAY_FOR_ARRAYS);
  }

  @Override
  @Deprecated
  public PactDslJsonBody maxArrayLike(String name, int size, DslPart object) {
    throw new UnsupportedOperationException(USE_PACT_DSL_JSON_ARRAY_FOR_ARRAYS);
  }

  @Override
  @Deprecated
  public PactDslJsonArray maxArrayLike(int size, DslPart object) {
    throw new UnsupportedOperationException(USE_PACT_DSL_JSON_ARRAY_FOR_ARRAYS);
  }

  /**
   * @deprecated Use PactDslJsonArray for arrays
   */
  @Override
  @Deprecated
  public PactDslJsonBody maxArrayLike(String name, int size, int numberExamples) {
    throw new UnsupportedOperationException(USE_PACT_DSL_JSON_ARRAY_FOR_ARRAYS);
  }

  /**
   * @deprecated Use PactDslJsonArray for arrays
   */
  @Override
  @Deprecated
  public PactDslJsonBody maxArrayLike(int size, int numberExamples) {
    throw new UnsupportedOperationException(USE_PACT_DSL_JSON_ARRAY_FOR_ARRAYS);
  }

  /**
   * @deprecated Use PactDslJsonArray for arrays
   */
  @Override
  @Deprecated
  public PactDslJsonBody minMaxArrayLike(String name, int minSize, int maxSize) {
    throw new UnsupportedOperationException(USE_PACT_DSL_JSON_ARRAY_FOR_ARRAYS);
  }

  @Override
  @Deprecated
  public PactDslJsonBody minMaxArrayLike(String name, int minSize, int maxSize, DslPart object) {
    throw new UnsupportedOperationException(USE_PACT_DSL_JSON_ARRAY_FOR_ARRAYS);
  }

  /**
   * @deprecated Use PactDslJsonArray for arrays
   */
  @Override
  @Deprecated
  public PactDslJsonBody minMaxArrayLike(int minSize, int maxSize) {
    throw new UnsupportedOperationException(USE_PACT_DSL_JSON_ARRAY_FOR_ARRAYS);
  }

  @Override
  @Deprecated
  public PactDslJsonArray minMaxArrayLike(int minSize, int maxSize, DslPart object) {
    throw new UnsupportedOperationException(USE_PACT_DSL_JSON_ARRAY_FOR_ARRAYS);
  }

  /**
   * @deprecated Use PactDslJsonArray for arrays
   */
  @Override
  @Deprecated
  public PactDslJsonBody minMaxArrayLike(String name, int minSize, int maxSize, int numberExamples) {
    throw new UnsupportedOperationException(USE_PACT_DSL_JSON_ARRAY_FOR_ARRAYS);
  }

  /**
   * @deprecated Use PactDslJsonArray for arrays
   */
  @Override
  @Deprecated
  public PactDslJsonBody minMaxArrayLike(int minSize, int maxSize, int numberExamples) {
    throw new UnsupportedOperationException(USE_PACT_DSL_JSON_ARRAY_FOR_ARRAYS);
  }

  /**
   * @deprecated Use PactDslJsonBody for objects
   */
  @Override
  @Deprecated
  public PactDslJsonBody object(String name) {
    throw new UnsupportedOperationException(USE_PACT_DSL_JSON_BODY_FOR_OBJECTS);
  }

  /**
   * @deprecated Use PactDslJsonBody for objects
   */
  @Override
  @Deprecated
  public PactDslJsonBody object() {
    throw new UnsupportedOperationException(USE_PACT_DSL_JSON_BODY_FOR_OBJECTS);
  }

  /**
   * @deprecated Use PactDslJsonBody for objects
   */
  @Override
  @Deprecated
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
  public static PactDslJsonRootValue stringType() {
    PactDslJsonRootValue value = new PactDslJsonRootValue();
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
  public static PactDslJsonRootValue stringType(String example) {
    PactDslJsonRootValue value = new PactDslJsonRootValue();
    value.setValue(example);
    value.setMatcher(TypeMatcher.INSTANCE);
    return value;
  }

  /**
   * Value that can be any number
   */
  public static PactDslJsonRootValue numberType() {
    PactDslJsonRootValue value = new PactDslJsonRootValue();
    value.getGenerators().addGenerator(Category.BODY, "", new RandomIntGenerator(0, Integer.MAX_VALUE));
    value.setValue(100);
    value.setMatcher(TypeMatcher.INSTANCE);
    return value;
  }

  /**
   * Value that can be any number
   * @param number example number to use for generated bodies
   */
  public static PactDslJsonRootValue numberType(Number number) {
    PactDslJsonRootValue value = new PactDslJsonRootValue();
    value.setValue(number);
    value.setMatcher(TypeMatcher.INSTANCE);
    return value;
  }

  /**
   * Value that must be an integer
   */
  public static PactDslJsonRootValue integerType() {
    PactDslJsonRootValue value = new PactDslJsonRootValue();
    value.getGenerators().addGenerator(Category.BODY, "", new RandomIntGenerator(0, Integer.MAX_VALUE));
    value.setValue(100);
    value.setMatcher(new NumberTypeMatcher(NumberTypeMatcher.NumberType.INTEGER));
    return value;
  }

  /**
   * Value that must be an integer
   * @param number example integer value to use for generated bodies
   */
  public static PactDslJsonRootValue integerType(Long number) {
    PactDslJsonRootValue value = new PactDslJsonRootValue();
    value.setValue(number);
    value.setMatcher(new NumberTypeMatcher(NumberTypeMatcher.NumberType.INTEGER));
    return value;
  }

  /**
   * Value that must be an integer
   * @param number example integer value to use for generated bodies
   */
  public static PactDslJsonRootValue integerType(int number) {
    PactDslJsonRootValue value = new PactDslJsonRootValue();
    value.setValue(number);
    value.setMatcher(new NumberTypeMatcher(NumberTypeMatcher.NumberType.INTEGER));
    return value;
  }

  /**
   * Value that must be a decimal value
   */
  public static PactDslJsonRootValue decimalType() {
    PactDslJsonRootValue value = new PactDslJsonRootValue();
    value.getGenerators().addGenerator(Category.BODY, "", new RandomDecimalGenerator(10));
    value.setValue(100);
    value.setMatcher(new NumberTypeMatcher(NumberTypeMatcher.NumberType.DECIMAL));
    return value;
  }

  /**
   * Value that must be a decimalType value
   * @param number example decimalType value
   */
  public static PactDslJsonRootValue decimalType(BigDecimal number) {
    PactDslJsonRootValue value = new PactDslJsonRootValue();
    value.setValue(number);
    value.setMatcher(new NumberTypeMatcher(NumberTypeMatcher.NumberType.DECIMAL));
    return value;
  }

  /**
   * Value that must be a decimalType value
   * @param number example decimalType value
   */
  public static PactDslJsonRootValue decimalType(Double number) {
    PactDslJsonRootValue value = new PactDslJsonRootValue();
    value.setValue(number);
    value.setMatcher(new NumberTypeMatcher(NumberTypeMatcher.NumberType.DECIMAL));
    return value;
  }

  /**
   * Value that must be a boolean
   */
  public static PactDslJsonRootValue booleanType() {
    return booleanType(true);
  }

  /**
   * Value that must be a boolean
   * @param example example boolean to use for generated bodies
   */
  public static PactDslJsonRootValue booleanType(Boolean example) {
    PactDslJsonRootValue value = new PactDslJsonRootValue();
    value.setValue(example);
    value.setMatcher(TypeMatcher.INSTANCE);
    return value;
  }

  /**
   * Value that must match the regular expression
   * @param regex regular expression
   * @param value example value to use for generated bodies
   */
  public static PactDslJsonRootValue stringMatcher(String regex, String value) {
    if (!value.matches(regex)) {
      throw new InvalidMatcherException(EXAMPLE + value + "\" does not match regular expression \"" +
        regex + "\"");
    }
    PactDslJsonRootValue rootValue = new PactDslJsonRootValue();
    rootValue.setValue(value);
    rootValue.setMatcher(new RegexMatcher(regex, value));
    return rootValue;
  }

  /**
   * Value that must match the regular expression
   * @param regex regular expression
   * @deprecated Use the version that takes an example value
   */
  @Deprecated
  public static PactDslJsonRootValue stringMatcher(String regex) {
    PactDslJsonRootValue rootValue = new PactDslJsonRootValue();
    rootValue.getGenerators().addGenerator(Category.BODY, "", new RegexGenerator(regex));
    rootValue.setValue(new Generex(regex).random());
    rootValue.setMatcher(rootValue.regexp(regex));
    return rootValue;
  }

  /**
   * Value that must be an ISO formatted timestamp
   */
  public static PactDslJsonRootValue timestamp() {
    return timestamp(DateFormatUtils.ISO_DATETIME_FORMAT.getPattern());
  }

  /**
   * Value that must match the given timestamp format
   * @param format timestamp format
   */
  public static PactDslJsonRootValue timestamp(String format) {
    PactDslJsonRootValue value = new PactDslJsonRootValue();
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
  public static PactDslJsonRootValue timestamp(String format, Date example) {
    FastDateFormat instance = FastDateFormat.getInstance(format);
    PactDslJsonRootValue value = new PactDslJsonRootValue();
    value.setValue(instance.format(example));
    value.setMatcher(value.matchTimestamp(format));
    return value;
  }

  /**
   * Value that must be formatted as an ISO date
   */
  public static PactDslJsonRootValue date() {
    return date(DateFormatUtils.ISO_DATE_FORMAT.getPattern());
  }

  /**
   * Value that must match the provided date format
   * @param format date format to match
   */
  public static PactDslJsonRootValue date(String format) {
    FastDateFormat instance = FastDateFormat.getInstance(format);
    PactDslJsonRootValue value = new PactDslJsonRootValue();
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
  public static PactDslJsonRootValue date(String format, Date example) {
    FastDateFormat instance = FastDateFormat.getInstance(format);
    PactDslJsonRootValue value = new PactDslJsonRootValue();
    value.setValue(instance.format(example));
    value.setMatcher(value.matchDate(format));
    return value;
  }

  /**
   * Value that must be an ISO formatted time
   */
  public static PactDslJsonRootValue time() {
    return time(DateFormatUtils.ISO_TIME_FORMAT.getPattern());
  }

  /**
   * Value that must match the given time format
   * @param format time format to match
   */
  public static PactDslJsonRootValue time(String format) {
    FastDateFormat instance = FastDateFormat.getInstance(format);
    PactDslJsonRootValue value = new PactDslJsonRootValue();
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
  public static PactDslJsonRootValue time(String format, Date example) {
    FastDateFormat instance = FastDateFormat.getInstance(format);
    PactDslJsonRootValue value = new PactDslJsonRootValue();
    value.setValue(instance.format(example));
    value.setMatcher(value.matchTime(format));
    return value;
  }

  /**
   * Value that must be an IP4 address
   */
  public static PactDslJsonRootValue ipAddress() {
    PactDslJsonRootValue value = new PactDslJsonRootValue();
    value.setValue("127.0.0.1");
    value.setMatcher(value.regexp("(\\d{1,3}\\.)+\\d{1,3}"));
    return value;
  }

  /**
   * Value that must be a numeric identifier
   */
  public static PactDslJsonRootValue id() {
    return numberType();
  }

  /**
   * Value that must be a numeric identifier
   * @param id example id to use for generated bodies
   */
  public static PactDslJsonRootValue id(Long id) {
    return numberType(id);
  }

  /**
   * Value that must be encoded as a hexadecimal value
   */
  public static PactDslJsonRootValue hexValue() {
    PactDslJsonRootValue value = new PactDslJsonRootValue();
    value.getGenerators().addGenerator(Category.BODY, "", new RandomHexadecimalGenerator(10));
    value.setValue("1234a");
    value.setMatcher(value.regexp("[0-9a-fA-F]+"));
    return value;
  }

  /**
   * Value that must be encoded as a hexadecimal value
   * @param hexValue example value to use for generated bodies
   */
  public static PactDslJsonRootValue hexValue(String hexValue) {
    if (!hexValue.matches(HEXADECIMAL)) {
      throw new InvalidMatcherException(EXAMPLE + hexValue + "\" is not a hexadecimal value");
    }
    PactDslJsonRootValue value = new PactDslJsonRootValue();
    value.setValue(hexValue);
    value.setMatcher(value.regexp("[0-9a-fA-F]+"));
    return value;
  }

  /**
   * Value that must be encoded as an UUID
   */
  public static PactDslJsonRootValue uuid() {
    PactDslJsonRootValue value = new PactDslJsonRootValue();
    value.getGenerators().addGenerator(Category.BODY, "", UuidGenerator.INSTANCE);
    value.setValue("e2490de5-5bd3-43d5-b7c4-526e33f71304");
    value.setMatcher(value.regexp(UUID_REGEX));
    return value;
  }

  /**
   * Value that must be encoded as an UUID
   * @param uuid example UUID to use for generated bodies
   */
  public static PactDslJsonRootValue uuid(UUID uuid) {
    return uuid(uuid.toString());
  }

  /**
   * Value that must be encoded as an UUID
   * @param uuid example UUID to use for generated bodies
   */
  public static PactDslJsonRootValue uuid(String uuid) {
    if (!uuid.matches(UUID_REGEX)) {
      throw new InvalidMatcherException(EXAMPLE + uuid + "\" is not an UUID");
    }

    PactDslJsonRootValue value = new PactDslJsonRootValue();
    value.setValue(uuid);
    value.setMatcher(value.regexp(UUID_REGEX));
    return value;
  }

  public void setValue(Object value) {
    this.value = value;
  }

  public void setMatcher(MatchingRule matcher) {
    getMatchers().addRule(matcher);
  }

  /**
   * @deprecated Use PactDslJsonArray for arrays
   */
  @Override
  @Deprecated
  public PactDslJsonArray eachArrayLike(String name) {
    throw new UnsupportedOperationException(USE_PACT_DSL_JSON_ARRAY_FOR_ARRAYS);
  }

  /**
   * @deprecated Use PactDslJsonArray for arrays
   */
  @Override
  @Deprecated
  public PactDslJsonArray eachArrayLike(int numberExamples) {
    throw new UnsupportedOperationException(USE_PACT_DSL_JSON_ARRAY_FOR_ARRAYS);
  }

  /**
   * @deprecated Use PactDslJsonArray for arrays
   */
  @Override
  @Deprecated
  public PactDslJsonArray eachArrayWithMaxLike(String name, int size) {
    throw new UnsupportedOperationException(USE_PACT_DSL_JSON_ARRAY_FOR_ARRAYS);
  }

  /**
   * @deprecated Use PactDslJsonArray for arrays
   */
  @Override
  @Deprecated
  public PactDslJsonArray eachArrayWithMaxLike(int size) {
    throw new UnsupportedOperationException(USE_PACT_DSL_JSON_ARRAY_FOR_ARRAYS);
  }

  /**
   * @deprecated Use PactDslJsonArray for arrays
   */
  @Override
  @Deprecated
  public PactDslJsonArray eachArrayWithMaxLike(String name, int numberExamples, int size) {
    throw new UnsupportedOperationException(USE_PACT_DSL_JSON_ARRAY_FOR_ARRAYS);
  }

  /**
   * @deprecated Use PactDslJsonArray for arrays
   */
  @Override
  @Deprecated
  public PactDslJsonArray eachArrayWithMaxLike(int numberExamples, int size) {
    throw new UnsupportedOperationException(USE_PACT_DSL_JSON_ARRAY_FOR_ARRAYS);
  }

  /**
   * @deprecated Use PactDslJsonArray for arrays
   */
  @Override
  @Deprecated
  public PactDslJsonArray eachArrayWithMinLike(String name, int size) {
    throw new UnsupportedOperationException(USE_PACT_DSL_JSON_ARRAY_FOR_ARRAYS);
  }

  /**
   * @deprecated Use PactDslJsonArray for arrays
   */
  @Override
  @Deprecated
  public PactDslJsonArray eachArrayWithMinLike(int size) {
    throw new UnsupportedOperationException(USE_PACT_DSL_JSON_ARRAY_FOR_ARRAYS);
  }

  /**
   * @deprecated Use PactDslJsonArray for arrays
   */
  @Override
  @Deprecated
  public PactDslJsonArray eachArrayWithMinLike(String name, int numberExamples, int size) {
    throw new UnsupportedOperationException(USE_PACT_DSL_JSON_ARRAY_FOR_ARRAYS);
  }

  /**
   * @deprecated Use PactDslJsonArray for arrays
   */
  @Override
  @Deprecated
  public PactDslJsonArray eachArrayWithMinLike(int numberExamples, int size) {
    throw new UnsupportedOperationException(USE_PACT_DSL_JSON_ARRAY_FOR_ARRAYS);
  }

  /**
   * @deprecated Use PactDslJsonArray for arrays
   */
  @Override
  @Deprecated
  public PactDslJsonArray eachArrayWithMinMaxLike(String name, int minSize, int maxSize) {
    throw new UnsupportedOperationException(USE_PACT_DSL_JSON_ARRAY_FOR_ARRAYS);
  }

  /**
   * @deprecated Use PactDslJsonArray for arrays
   */
  @Override
  @Deprecated
  public PactDslJsonArray eachArrayWithMinMaxLike(int minSize, int maxSize) {
    throw new UnsupportedOperationException(USE_PACT_DSL_JSON_ARRAY_FOR_ARRAYS);
  }

  /**
   * @deprecated Use PactDslJsonArray for arrays
   */
  @Override
  @Deprecated
  public PactDslJsonArray eachArrayWithMinMaxLike(String name, int numberExamples, int minSize, int maxSize) {
    throw new UnsupportedOperationException(USE_PACT_DSL_JSON_ARRAY_FOR_ARRAYS);
  }

  /**
   * @deprecated Use PactDslJsonArray for arrays
   */
  @Override
  @Deprecated
  public PactDslJsonArray eachArrayWithMinMaxLike(int numberExamples, int minSize, int maxSize) {
    throw new UnsupportedOperationException(USE_PACT_DSL_JSON_ARRAY_FOR_ARRAYS);
  }

  /**
   * @deprecated Use PactDslJsonArray for arrays
   */
  @Override
  @Deprecated
  public PactDslJsonArray eachArrayLike(String name, int numberExamples) {
    throw new UnsupportedOperationException(USE_PACT_DSL_JSON_ARRAY_FOR_ARRAYS);
  }

  /**
   * @deprecated Use PactDslJsonArray for arrays
   */
  @Override
  @Deprecated
  public PactDslJsonArray eachArrayLike() {
    throw new UnsupportedOperationException(USE_PACT_DSL_JSON_ARRAY_FOR_ARRAYS);
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
   * Combine all the matchers using AND
   * @param example Attribute example value
   * @param rules Matching rules to apply
   */
  public static PactDslJsonRootValue and(Object example, MatchingRule... rules) {
    PactDslJsonRootValue value = new PactDslJsonRootValue();
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
  public static PactDslJsonRootValue or(Object example, MatchingRule... rules) {
    PactDslJsonRootValue value = new PactDslJsonRootValue();
    if (example != null) {
      value.setValue(example);
    } else {
      value.setValue(JSONObject.NULL);
    }
    value.getMatchers().setRules("", new MatchingRuleGroup(Arrays.asList(rules), RuleLogic.OR));
    return value;
  }

  /**
   * Matches a URL that is composed of a base path and a sequence of path expressions
   * @param basePath The base path for the URL (like "http://localhost:8080/") which will be excluded from the matching
   * @param pathFragments Series of path fragments to match on. These can be strings or regular expressions.
   */
  public PactDslJsonRootValue matchUrl(String basePath, Object... pathFragments) {
    UrlMatcherSupport urlMatcher = new UrlMatcherSupport(basePath, Arrays.asList(pathFragments));
    PactDslJsonRootValue value = new PactDslJsonRootValue();
    String exampleValue = urlMatcher.getExampleValue();
    value.setValue(exampleValue);
    String regexExpression = urlMatcher.getRegexExpression();
    value.setMatcher(value.regexp(regexExpression));
    if (StringUtils.isEmpty(basePath)) {
      value.getGenerators().addGenerator(Category.BODY, "", new MockServerURLGenerator(exampleValue, regexExpression));
    }
    return value;
  }

  @Override
  public DslPart matchUrl(String name, String basePath, Object... pathFragments) {
    throw new UnsupportedOperationException(
      "URL matcher with an attribute name is not supported. " +
        "Use matchUrl(String basePath, Object... pathFragments)");
  }

  @Override
  public PactDslJsonBody matchUrl2(String name, Object... pathFragments) {
    throw new UnsupportedOperationException(
      "URL matcher with an attribute name is not supported. " +
        "Use matchUrl2(Object... pathFragments)");
  }

  /**
   * Matches a URL that is composed of a base path and a sequence of path expressions. Base path from the mock server
   * will be used.
   * @param pathFragments Series of path fragments to match on. These can be strings or regular expressions.
   */
  @Override
  public DslPart matchUrl2(Object... pathFragments) {
    return matchUrl(null, pathFragments);
  }

  /**
   * Adds a value that will have it's value injected from the provider state
   * @param expression Expression to be evaluated from the provider state
   * @param example Example value to be used in the consumer test
   */
  public static PactDslJsonRootValue valueFromProviderState(String expression, Object example) {
    PactDslJsonRootValue value = new PactDslJsonRootValue();
    value.getGenerators().addGenerator(Category.BODY, "", new ProviderStateGenerator(expression, DataType.from(example)));
    value.setValue(example);
    value.setMatcher(TypeMatcher.INSTANCE);
    return value;
  }

  /**
   * Date value generated from an expression. The date will be formatted as an ISO date.
   * @param expression Date expression
   */
  public static PactDslJsonRootValue dateExpression(String expression) {
    return dateExpression(expression, DateFormatUtils.ISO_DATE_FORMAT.getPattern());
  }

  /**
   * Date value generated from an expression.
   * @param expression Date expression
   * @param format Date format to use
   */
  public static PactDslJsonRootValue dateExpression(String expression, String format) {
    FastDateFormat instance = FastDateFormat.getInstance(format);
    PactDslJsonRootValue value = new PactDslJsonRootValue();
    value.getGenerators().addGenerator(Category.BODY, "", new DateGenerator(format, expression));
    value.setValue(instance.format(new Date(DATE_2000)));
    value.setMatcher(value.matchDate(format));
    return value;
  }

  /**
   * Time value generated from an expression. The time will be formatted as an ISO time.
   * @param expression Date expression
   */
  public static PactDslJsonRootValue timeExpression(String expression) {
    return timeExpression(expression, DateFormatUtils.ISO_TIME_NO_T_FORMAT.getPattern());
  }

  /**
   * Time value generated from an expression.
   * @param expression Time expression
   * @param format Time format to use
   */
  public static PactDslJsonRootValue timeExpression(String expression, String format) {
    FastDateFormat instance = FastDateFormat.getInstance(format);
    PactDslJsonRootValue value = new PactDslJsonRootValue();
    value.getGenerators().addGenerator(Category.BODY, "", new TimeGenerator(format, expression));
    value.setValue(instance.format(new Date(DATE_2000)));
    value.setMatcher(value.matchTime(format));
    return value;
  }

  /**
   * Datetime value generated from an expression. The datetime will be formatted as an ISO datetime.
   * @param expression Datetime expression
   */
  public static PactDslJsonRootValue datetimeExpression(String expression) {
    return datetimeExpression(expression, DateFormatUtils.ISO_DATETIME_FORMAT.getPattern());
  }

  /**
   * Datetime value generated from an expression.
   * @param expression Datetime expression
   * @param format Datetime format to use
   */
  public static PactDslJsonRootValue datetimeExpression(String expression, String format) {
    FastDateFormat instance = FastDateFormat.getInstance(format);
    PactDslJsonRootValue value = new PactDslJsonRootValue();
    value.getGenerators().addGenerator(Category.BODY, "", new DateTimeGenerator(format, expression));
    value.setValue(instance.format(new Date(DATE_2000)));
    value.setMatcher(value.matchTimestamp(format));
    return value;
  }

  @Override
  public DslPart arrayContaining(String name) {
    throw new UnsupportedOperationException("arrayContaining is not supported for PactDslJsonRootValue");
  }
}
