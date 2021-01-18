package au.com.dius.pact.consumer.dsl;

import au.com.dius.pact.consumer.InvalidMatcherException;
import au.com.dius.pact.core.matchers.UrlMatcherSupport;
import au.com.dius.pact.core.model.generators.Category;
import au.com.dius.pact.core.model.generators.DateGenerator;
import au.com.dius.pact.core.model.generators.DateTimeGenerator;
import au.com.dius.pact.core.model.generators.MockServerURLGenerator;
import au.com.dius.pact.core.model.generators.ProviderStateGenerator;
import au.com.dius.pact.core.model.generators.RandomBooleanGenerator;
import au.com.dius.pact.core.model.generators.RandomDecimalGenerator;
import au.com.dius.pact.core.model.generators.RandomHexadecimalGenerator;
import au.com.dius.pact.core.model.generators.RandomIntGenerator;
import au.com.dius.pact.core.model.generators.RandomStringGenerator;
import au.com.dius.pact.core.model.generators.TimeGenerator;
import au.com.dius.pact.core.model.generators.UuidGenerator;
import au.com.dius.pact.core.model.matchingrules.EqualsIgnoreOrderMatcher;
import au.com.dius.pact.core.model.matchingrules.EqualsMatcher;
import au.com.dius.pact.core.model.matchingrules.MatchingRule;
import au.com.dius.pact.core.model.matchingrules.MatchingRuleGroup;
import au.com.dius.pact.core.model.matchingrules.MaxEqualsIgnoreOrderMatcher;
import au.com.dius.pact.core.model.matchingrules.MinEqualsIgnoreOrderMatcher;
import au.com.dius.pact.core.model.matchingrules.MinMaxEqualsIgnoreOrderMatcher;
import au.com.dius.pact.core.model.matchingrules.NumberTypeMatcher;
import au.com.dius.pact.core.model.matchingrules.RuleLogic;
import au.com.dius.pact.core.model.matchingrules.TypeMatcher;
import au.com.dius.pact.core.support.Json;
import au.com.dius.pact.core.support.expressions.DataType;
import au.com.dius.pact.core.support.json.JsonValue;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.time.DateFormatUtils;
import org.apache.commons.lang3.time.FastDateFormat;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.format.DateTimeFormatter;
import java.util.Arrays;
import java.util.Date;

/**
 * DSL to define a JSON array
 */
public class PactDslJsonArray extends DslPart {

  private static final String EXAMPLE = "Example \"";
  private final JsonValue.Array body;
  private boolean wildCard;
  private int numberExamples = 1;

  /**
   * Construct a root level array
   */
  public PactDslJsonArray() {
      this("", "", null, false);
    }

  /**
   * Construct an array as a child
   * @param rootPath Path to the child array
   * @param rootName Name to associate the child as
   * @param parent Parent to attach the child to
   */
  public PactDslJsonArray(String rootPath, String rootName, DslPart parent) {
      this(rootPath, rootName, parent, false);
  }

  /**
   * Construct an array as a child copied from an existing array
   * @param rootPath Path to the child array
   * @param rootName Name to associate the child as
   * @param parent Parent to attach the child to
   * @param array Array to copy
   */
  public PactDslJsonArray(String rootPath, String rootName, DslPart parent, PactDslJsonArray array) {
    super(parent, rootPath, rootName);
    this.body = array.body;
    this.wildCard = array.wildCard;
    this.matchers = array.matchers.copyWithUpdatedMatcherRootPrefix(rootPath);
    this.generators = array.generators;
  }

  /**
   * Construct a array as a child
   * @param rootPath Path to the child array
   * @param rootName Name to associate the child as
   * @param parent Parent to attach the child to
   * @param wildCard If it should be matched as a wild card
   */
  public PactDslJsonArray(String rootPath, String rootName, DslPart parent, boolean wildCard) {
    super(parent, rootPath, rootName);
    this.wildCard = wildCard;
    body = new JsonValue.Array();
  }

    /**
     * Closes the current array
     */
    public DslPart closeArray() {
      if (parent != null) {
        parent.putArray(this);
      } else {
        getMatchers().applyMatcherRootPrefix("$");
        getGenerators().applyRootPrefix("$");
      }
      closed = true;
      return parent;
    }

    @Override
    public PactDslJsonBody eachLike(String name) {
        throw new UnsupportedOperationException("use the eachLike() form");
    }

    @Override
    public PactDslJsonBody eachLike(String name, int numberExamples) {
      throw new UnsupportedOperationException("use the eachLike(numberExamples) form");
    }

    /**
     * Element that is an array where each item must match the following example
     */
    @Override
    public PactDslJsonBody eachLike() {
        return eachLike(1);
    }

    /**
     * Element that is an array where each item must match the following example
     * @param numberExamples Number of examples to generate
     */
    @Override
    public PactDslJsonBody eachLike(int numberExamples) {
      matchers.addRule(rootPath + appendArrayIndex(1), matchMin(0));
      PactDslJsonArray parent = new PactDslJsonArray(rootPath, "", this, true);
      parent.setNumberExamples(numberExamples);
      return new PactDslJsonBody(".", "", parent);
    }

    @Override
    public PactDslJsonBody minArrayLike(String name, Integer size) {
        throw new UnsupportedOperationException("use the minArrayLike(Integer size) form");
    }

    /**
     * Element that is an array with a minimum size where each item must match the following example
     * @param size minimum size of the array
     */
    @Override
    public PactDslJsonBody minArrayLike(Integer size) {
        return minArrayLike(size, size);
    }

    @Override
    public PactDslJsonBody minArrayLike(String name, Integer size, int numberExamples) {
      throw new UnsupportedOperationException("use the minArrayLike(Integer size, int numberExamples) form");
    }

    /**
     * Element that is an array with a minimum size where each item must match the following example
     * @param size minimum size of the array
     * @param numberExamples number of examples to generate
     */
    @Override
    public PactDslJsonBody minArrayLike(Integer size, int numberExamples) {
      if (numberExamples < size) {
        throw new IllegalArgumentException(String.format("Number of example %d is less than the minimum size of %d",
          numberExamples, size));
      }
      matchers.addRule(rootPath + appendArrayIndex(1), matchMin(size));
      PactDslJsonArray parent = new PactDslJsonArray("", "", this, true);
      parent.setNumberExamples(numberExamples);
      return new PactDslJsonBody(".", "", parent);
    }

    @Override
    public PactDslJsonBody maxArrayLike(String name, Integer size) {
        throw new UnsupportedOperationException("use the maxArrayLike(Integer size) form");
    }

    /**
     * Element that is an array with a maximum size where each item must match the following example
     * @param size maximum size of the array
     */
    @Override
    public PactDslJsonBody maxArrayLike(Integer size) {
        return maxArrayLike(size, 1);
    }

    @Override
    public PactDslJsonBody maxArrayLike(String name, Integer size, int numberExamples) {
      throw new UnsupportedOperationException("use the maxArrayLike(Integer size, int numberExamples) form");
    }

    /**
     * Element that is an array with a maximum size where each item must match the following example
     * @param size maximum size of the array
     * @param numberExamples number of examples to generate
     */
    @Override
    public PactDslJsonBody maxArrayLike(Integer size, int numberExamples) {
      if (numberExamples > size) {
        throw new IllegalArgumentException(String.format("Number of example %d is more than the maximum size of %d",
          numberExamples, size));
      }
      matchers.addRule(rootPath + appendArrayIndex(1), matchMax(size));
      PactDslJsonArray parent = new PactDslJsonArray("", "", this, true);
      parent.setNumberExamples(numberExamples);
      return new PactDslJsonBody(".", "", parent);
    }

    protected void putObject(DslPart object) {
      for(String matcherName: object.matchers.getMatchingRules().keySet()) {
        matchers.setRules(rootPath + appendArrayIndex(1) + matcherName,
          object.matchers.getMatchingRules().get(matcherName));
      }
      generators.addGenerators(object.generators, rootPath + appendArrayIndex(1));
      for (int i = 0; i < getNumberExamples(); i++) {
        body.add(object.getBody());
      }
    }

    protected void putArray(DslPart object) {
      for(String matcherName: object.matchers.getMatchingRules().keySet()) {
        matchers.setRules(rootPath + appendArrayIndex(1) + matcherName,
          object.matchers.getMatchingRules().get(matcherName));
      }
      generators.addGenerators(object.generators, rootPath + appendArrayIndex(1));
      for (int i = 0; i < getNumberExamples(); i++) {
        body.add(object.getBody());
      }
    }

    @Override
    public JsonValue getBody() {
        return body;
    }

    /**
     * Element that must be the specified value
     * @param value string value
     */
    public PactDslJsonArray stringValue(String value) {
      if (value == null) {
        body.add(JsonValue.Null.INSTANCE);
      } else {
        body.add(new JsonValue.StringValue(value.toCharArray()));
      }
      return this;
    }

    /**
     * Element that must be the specified value
     * @param value string value
     */
    public PactDslJsonArray string(String value) {
        return stringValue(value);
    }

    public PactDslJsonArray numberValue(Number value) {
      body.add(new JsonValue.Decimal(value.toString().toCharArray()));
      return this;
    }

    /**
     * Element that must be the specified value
     * @param value number value
     */
    public PactDslJsonArray number(Number value) {
        return numberValue(value);
    }

    /**
     * Element that must be the specified value
     * @param value boolean value
     */
    public PactDslJsonArray booleanValue(Boolean value) {
      body.add(value ? JsonValue.True.INSTANCE : JsonValue.False.INSTANCE);
      return this;
    }

    /**
     * Element that can be any string
     */
    public PactDslJsonArray stringType() {
      body.add(new JsonValue.StringValue("string".toCharArray()));
      generators.addGenerator(Category.BODY, rootPath + appendArrayIndex(0), new RandomStringGenerator(20));
      matchers.addRule(rootPath + appendArrayIndex(0), TypeMatcher.INSTANCE);
      return this;
    }

    /**
     * Element that can be any string
     * @param example example value to use for generated bodies
     */
    public PactDslJsonArray stringType(String example) {
      body.add(new JsonValue.StringValue(example.toCharArray()));
      matchers.addRule(rootPath + appendArrayIndex(0), TypeMatcher.INSTANCE);
      return this;
    }

    /**
     * Element that can be any number
     */
    public PactDslJsonArray numberType() {
      generators.addGenerator(Category.BODY, rootPath + appendArrayIndex(1), new RandomIntGenerator(0, Integer.MAX_VALUE));
      return numberType(100);
    }

    /**
     * Element that can be any number
     * @param number example number to use for generated bodies
     */
    public PactDslJsonArray numberType(Number number) {
      body.add(new JsonValue.Decimal(number.toString().toCharArray()));
      matchers.addRule(rootPath + appendArrayIndex(0), new NumberTypeMatcher(NumberTypeMatcher.NumberType.NUMBER));
      return this;
    }

    /**
     * Element that must be an integer
     */
    public PactDslJsonArray integerType() {
      generators.addGenerator(Category.BODY, rootPath + appendArrayIndex(1), new RandomIntGenerator(0, Integer.MAX_VALUE));
      return integerType(100L);
    }

    /**
     * Element that must be an integer
     * @param number example integer value to use for generated bodies
     */
    public PactDslJsonArray integerType(Long number) {
      body.add(new JsonValue.Integer(number.toString().toCharArray()));
      matchers.addRule(rootPath + appendArrayIndex(0), new NumberTypeMatcher(NumberTypeMatcher.NumberType.INTEGER));
      return this;
    }

  /**
   * Element that must be a decimal value
   */
  public PactDslJsonArray decimalType() {
    generators.addGenerator(Category.BODY, rootPath + appendArrayIndex(1), new RandomDecimalGenerator(10));
    return decimalType(new BigDecimal("100"));
  }

  /**
   * Element that must be a decimalType value
   * @param number example decimalType value
   */
  public PactDslJsonArray decimalType(BigDecimal number) {
    body.add(new JsonValue.Decimal(number.toString().toCharArray()));
    matchers.addRule(rootPath + appendArrayIndex(0), new NumberTypeMatcher(NumberTypeMatcher.NumberType.DECIMAL));
    return this;
  }

  /**
   * Attribute that must be a decimalType value
   * @param number example decimalType value
   */
  public PactDslJsonArray decimalType(Double number) {
    body.add(new JsonValue.Decimal(number.toString().toCharArray()));
    matchers.addRule(rootPath + appendArrayIndex(0), new NumberTypeMatcher(NumberTypeMatcher.NumberType.DECIMAL));
    return this;
  }

  /**
   * Element that must be a boolean
   */
  public PactDslJsonArray booleanType() {
    generators.addGenerator(Category.BODY, rootPath + appendArrayIndex(1), RandomBooleanGenerator.INSTANCE);
    body.add(JsonValue.True.INSTANCE);
    matchers.addRule(rootPath + appendArrayIndex(0), TypeMatcher.INSTANCE);
    return this;
  }

  /**
   * Element that must be a boolean
   * @param example example boolean to use for generated bodies
   */
  public PactDslJsonArray booleanType(Boolean example) {
    body.add(example ? JsonValue.True.INSTANCE : JsonValue.False.INSTANCE);
    matchers.addRule(rootPath + appendArrayIndex(0), TypeMatcher.INSTANCE);
    return this;
  }

    /**
     * Element that must match the regular expression
     * @param regex regular expression
     * @param value example value to use for generated bodies
     */
    public PactDslJsonArray stringMatcher(String regex, String value) {
      if (!value.matches(regex)) {
        throw new InvalidMatcherException(EXAMPLE + value + "\" does not match regular expression \"" +
            regex + "\"");
      }
      body.add(new JsonValue.StringValue(value.toCharArray()));
      matchers.addRule(rootPath + appendArrayIndex(0), regexp(regex));
      return this;
    }

    /**
     * Element that must be an ISO formatted timestamp
     */
    public PactDslJsonArray timestamp() {
      String pattern = DateFormatUtils.ISO_DATETIME_FORMAT.getPattern();
      body.add(new JsonValue.StringValue(
        DateFormatUtils.ISO_DATETIME_FORMAT.format(new Date(DATE_2000)).toCharArray()));
      generators.addGenerator(Category.BODY, rootPath + appendArrayIndex(0), new DateTimeGenerator(pattern));
      matchers.addRule(rootPath + appendArrayIndex(0), matchTimestamp(pattern));
      return this;
    }

    /**
     * Element that must match the given timestamp format
     * @param format timestamp format
     */
    public PactDslJsonArray timestamp(String format) {
      FastDateFormat instance = FastDateFormat.getInstance(format);
      body.add(new JsonValue.StringValue(instance.format(new Date(DATE_2000)).toCharArray()));
      generators.addGenerator(Category.BODY, rootPath + appendArrayIndex(0), new DateTimeGenerator(format));
      matchers.addRule(rootPath + appendArrayIndex(0), matchTimestamp(format));
      return this;
    }

    /**
     * Element that must match the given timestamp format
     * @param format timestamp format
     * @param example example date and time to use for generated bodies
     */
    public PactDslJsonArray timestamp(String format, Date example) {
      FastDateFormat instance = FastDateFormat.getInstance(format);
      body.add(new JsonValue.StringValue(instance.format(example).toCharArray()));
      matchers.addRule(rootPath + appendArrayIndex(0), matchTimestamp(format));
      return this;
    }

    /**
     * Element that must match the given timestamp format
     * @param format timestamp format
     * @param example example date and time to use for generated bodies
     */
    public PactDslJsonArray timestamp(String format, Instant example) {
      DateTimeFormatter formatter = DateTimeFormatter.ofPattern(format);
      body.add(new JsonValue.StringValue(formatter.format(example).toCharArray()));
      matchers.addRule(rootPath + appendArrayIndex(0), matchTimestamp(format));
      return this;
    }

    /**
     * Element that must be formatted as an ISO date
     */
    public PactDslJsonArray date() {
      String pattern = DateFormatUtils.ISO_DATE_FORMAT.getPattern();
      body.add(new JsonValue.StringValue(DateFormatUtils.ISO_DATE_FORMAT.format(new Date(DATE_2000)).toCharArray()));
      generators.addGenerator(Category.BODY, rootPath + appendArrayIndex(0), new DateGenerator(pattern));
      matchers.addRule(rootPath + appendArrayIndex(0), matchDate(pattern));
      return this;
    }

    /**
     * Element that must match the provided date format
     * @param format date format to match
     */
    public PactDslJsonArray date(String format) {
      FastDateFormat instance = FastDateFormat.getInstance(format);
      body.add(new JsonValue.StringValue(instance.format(new Date(DATE_2000)).toCharArray()));
      generators.addGenerator(Category.BODY, rootPath + appendArrayIndex(0), new DateTimeGenerator(format));
      matchers.addRule(rootPath + appendArrayIndex(0), matchDate(format));
      return this;
    }

    /**
     * Element that must match the provided date format
     * @param format date format to match
     * @param example example date to use for generated values
     */
    public PactDslJsonArray date(String format, Date example) {
      FastDateFormat instance = FastDateFormat.getInstance(format);
      body.add(new JsonValue.StringValue(instance.format(example).toCharArray()));
      matchers.addRule(rootPath + appendArrayIndex(0), matchDate(format));
      return this;
    }

    /**
     * Element that must be an ISO formatted time
     */
    public PactDslJsonArray time() {
      String pattern = DateFormatUtils.ISO_TIME_FORMAT.getPattern();
      body.add(new JsonValue.StringValue(DateFormatUtils.ISO_TIME_FORMAT.format(new Date(DATE_2000)).toCharArray()));
      generators.addGenerator(Category.BODY, rootPath + appendArrayIndex(0), new TimeGenerator(pattern));
      matchers.addRule(rootPath + appendArrayIndex(0), matchTime(pattern));
      return this;
    }

    /**
     * Element that must match the given time format
     * @param format time format to match
     */
    public PactDslJsonArray time(String format) {
      FastDateFormat instance = FastDateFormat.getInstance(format);
      body.add(new JsonValue.StringValue(instance.format(new Date(DATE_2000)).toCharArray()));
      generators.addGenerator(Category.BODY, rootPath + appendArrayIndex(0), new TimeGenerator(format));
      matchers.addRule(rootPath + appendArrayIndex(0), matchTime(format));
      return this;
    }

    /**
     * Element that must match the given time format
     * @param format time format to match
     * @param example example time to use for generated bodies
     */
    public PactDslJsonArray time(String format, Date example) {
      FastDateFormat instance = FastDateFormat.getInstance(format);
      body.add(new JsonValue.StringValue(instance.format(example).toCharArray()));
      matchers.addRule(rootPath + appendArrayIndex(0), matchTime(format));
      return this;
    }

    /**
     * Element that must be an IP4 address
     */
    public PactDslJsonArray ipAddress() {
      body.add(new JsonValue.StringValue("127.0.0.1".toCharArray()));
      matchers.addRule(rootPath + appendArrayIndex(0), regexp("(\\d{1,3}\\.)+\\d{1,3}"));
      return this;
    }

    public PactDslJsonBody object(String name) {
        throw new UnsupportedOperationException("use the object() form");
    }

    /**
     * Element that is a JSON object
     */
    public PactDslJsonBody object() {
        return new PactDslJsonBody(".", "", this);
    }

    @Override
    public DslPart closeObject() {
        throw new UnsupportedOperationException("can't call closeObject on an Array");
    }

  @Override
  public DslPart close() {
    DslPart parentToReturn = this;

    if (!closed) {
      DslPart parent = closeArray();
      while (parent != null) {
        parentToReturn = parent;
        if (parent instanceof PactDslJsonArray) {
          parent = parent.closeArray();
        } else {
          parent = parent.closeObject();
        }
      }
    }

    return parentToReturn;
  }

  @Override
  public DslPart arrayContaining(String name) {
    throw new UnsupportedOperationException(
      "arrayContaining is not currently supported for arrays");
  }

  public PactDslJsonArray array(String name) {
        throw new UnsupportedOperationException("use the array() form");
    }

    /**
     * Element that is a JSON array
     */
    public PactDslJsonArray array() {
        return new PactDslJsonArray("", "", this);
    }

  @Override
  public PactDslJsonArray unorderedArray(String name) {
    throw new UnsupportedOperationException("use the unorderedArray() form");
  }

  @Override
  public PactDslJsonArray unorderedArray() {
    matchers.addRule(rootPath + appendArrayIndex(1), EqualsIgnoreOrderMatcher.INSTANCE);
    return this.array();
  }

  @Override
  public PactDslJsonArray unorderedMinArray(String name, int size) {
    throw new UnsupportedOperationException("use the unorderedMinArray(int size) form");
  }

  @Override
  public PactDslJsonArray unorderedMinArray(int size) {
    matchers.addRule(rootPath + appendArrayIndex(1), new MinEqualsIgnoreOrderMatcher(size));
    return this.array();
  }

  @Override
  public PactDslJsonArray unorderedMaxArray(String name, int size) {
    throw new UnsupportedOperationException("use the unorderedMaxArray(int size) form");
  }

  @Override
  public PactDslJsonArray unorderedMaxArray(int size) {
    matchers.addRule(rootPath + appendArrayIndex(1), new MaxEqualsIgnoreOrderMatcher(size));
    return this.array();
  }

  @Override
  public PactDslJsonArray unorderedMinMaxArray(String name, int minSize, int maxSize) {
    throw new UnsupportedOperationException("use the unorderedMinMaxArray(int minSize, int maxSize) form");
  }

  @Override
  public PactDslJsonArray unorderedMinMaxArray(int minSize, int maxSize) {
    if (minSize > maxSize) {
      throw new IllegalArgumentException(String.format("The minimum size of %d is greater than the maximum of %d",
          minSize, maxSize));
    }
    matchers.addRule(rootPath + appendArrayIndex(1), new MinMaxEqualsIgnoreOrderMatcher(minSize, maxSize));
    return this.array();
  }

    /**
     * Matches rule for all elements in array
     * @param rule Matching rule to apply across array
     */
  public PactDslJsonArray wildcardArrayMatcher(MatchingRule rule) {
    wildCard = true;
    matchers.addRule(rootPath + appendArrayIndex(1), rule);
    return this;
  }

  /**
   * Element that must be a numeric identifier
   */
  public PactDslJsonArray id() {
    body.add(new JsonValue.Integer("100".toCharArray()));
    generators.addGenerator(Category.BODY, rootPath + appendArrayIndex(0), new RandomIntGenerator(0, Integer.MAX_VALUE));
    matchers.addRule(rootPath + appendArrayIndex(0), TypeMatcher.INSTANCE);
    return this;
  }

  /**
   * Element that must be a numeric identifier
   * @param id example id to use for generated bodies
   */
  public PactDslJsonArray id(Long id) {
    body.add(new JsonValue.Integer(id.toString().toCharArray()));
    matchers.addRule(rootPath + appendArrayIndex(0), TypeMatcher.INSTANCE);
    return this;
  }

    /**
     * Element that must be encoded as a hexadecimal value
     */
    public PactDslJsonArray hexValue() {
      generators.addGenerator(Category.BODY, rootPath + appendArrayIndex(1), new RandomHexadecimalGenerator(10));
      return hexValue("1234a");
    }

  /**
   * Element that must be encoded as a hexadecimal value
   * @param hexValue example value to use for generated bodies
   */
  public PactDslJsonArray hexValue(String hexValue) {
    if (!hexValue.matches(HEXADECIMAL)) {
      throw new InvalidMatcherException(EXAMPLE + hexValue + "\" is not a hexadecimal value");
    }
    body.add(new JsonValue.StringValue(hexValue.toCharArray()));
    matchers.addRule(rootPath + appendArrayIndex(0), regexp("[0-9a-fA-F]+"));
    return this;
  }

    /**
     * Element that must be encoded as an UUID
     */
    public PactDslJsonArray uuid() {
      generators.addGenerator(Category.BODY, rootPath + appendArrayIndex(1), UuidGenerator.INSTANCE);
      return uuid("e2490de5-5bd3-43d5-b7c4-526e33f71304");
    }

  /**
   * Element that must be encoded as an UUID
   * @param uuid example UUID to use for generated bodies
   */
  public PactDslJsonArray uuid(String uuid) {
    if (!uuid.matches(UUID_REGEX)) {
      throw new InvalidMatcherException(EXAMPLE + uuid + "\" is not an UUID");
    }
    body.add(new JsonValue.StringValue(uuid.toCharArray()));
    matchers.addRule(rootPath + appendArrayIndex(0), regexp(UUID_REGEX));
    return this;
  }

  /**
   * Adds the template object to the array
   * @param template template object
   */
	public PactDslJsonArray template(DslPart template) {
		putObject(template);
		return this;
	}

  /**
   * Adds a number of template objects to the array
   * @param template template object
   * @param occurrences number to add
   */
	public PactDslJsonArray template(DslPart template, int occurrences) {
		for(int i = 0; i < occurrences; i++) {
			template(template);	
		}
		return this;
	}
	
	@Override
	public String toString() {
		return body.toString();
	}

  private String appendArrayIndex(Integer offset) {
    String index = "*";
    if (!wildCard) {
      index = String.valueOf(body.getSize() - 1 + offset);
    }
    return "[" + index + "]";
  }

  /**
   * Array where each item must match the following example
   */
  public static PactDslJsonBody arrayEachLike() {
    return arrayEachLike(1);
  }

  /**
   * Array where each item must match the following example
   * @param numberExamples Number of examples to generate
   */
  public static PactDslJsonBody arrayEachLike(Integer numberExamples) {
    PactDslJsonArray parent = new PactDslJsonArray("", "", null, true);
    parent.setNumberExamples(numberExamples);
    parent.matchers.addRule("", parent.matchMin(0));
    return new PactDslJsonBody(".", "", parent);
  }

  /**
   * Root level array where each item must match the provided matcher
   */
  public static PactDslJsonArray arrayEachLike(PactDslJsonRootValue rootValue) {
    return arrayEachLike(1, rootValue);
  }

  /**
   * Root level array where each item must match the provided matcher
   * @param numberExamples Number of examples to generate
   */
  public static PactDslJsonArray arrayEachLike(Integer numberExamples, PactDslJsonRootValue value) {
    PactDslJsonArray parent = new PactDslJsonArray("", "", null, true);
    parent.setNumberExamples(numberExamples);
    parent.matchers.addRule("", parent.matchMin(0));
    parent.putObject(value);
    return parent;
  }

  /**
   * Array with a minimum size where each item must match the following example
   * @param minSize minimum size
   */
  public static PactDslJsonBody arrayMinLike(int minSize) {
      return arrayMinLike(minSize, minSize);
  }

  /**
   * Array with a minimum size where each item must match the following example
   * @param minSize minimum size
   * @param numberExamples Number of examples to generate
   */
  public static PactDslJsonBody arrayMinLike(int minSize, int numberExamples) {
    if (numberExamples < minSize) {
      throw new IllegalArgumentException(String.format("Number of example %d is less than the minimum size of %d",
        numberExamples, minSize));
    }
    PactDslJsonArray parent = new PactDslJsonArray("", "", null, true);
    parent.setNumberExamples(numberExamples);
    parent.matchers.addRule("", parent.matchMin(minSize));
    return new PactDslJsonBody(".", "", parent);
  }

  /**
   * Root level array with minimum size where each item must match the provided matcher
   * @param minSize minimum size
   */
  public static PactDslJsonArray arrayMinLike(int minSize, PactDslJsonRootValue value) {
    return arrayMinLike(minSize, minSize, value);
  }

  /**
   * Root level array with minimum size where each item must match the provided matcher
   * @param minSize minimum size
   * @param numberExamples Number of examples to generate
   */
  public static PactDslJsonArray arrayMinLike(int minSize, int numberExamples, PactDslJsonRootValue value) {
    if (numberExamples < minSize) {
      throw new IllegalArgumentException(String.format("Number of example %d is less than the minimum size of %d",
        numberExamples, minSize));
    }
    PactDslJsonArray parent = new PactDslJsonArray("", "", null, true);
    parent.setNumberExamples(numberExamples);
    parent.matchers.addRule("", parent.matchMin(minSize));
    parent.putObject(value);
    return parent;
  }

  /**
   * Array with a maximum size where each item must match the following example
   * @param maxSize maximum size
   */
  public static PactDslJsonBody arrayMaxLike(int maxSize) {
      return arrayMaxLike(maxSize, 1);
  }

  /**
   * Array with a maximum size where each item must match the following example
   * @param maxSize maximum size
   * @param numberExamples Number of examples to generate
   */
  public static PactDslJsonBody arrayMaxLike(int maxSize, int numberExamples) {
    if (numberExamples > maxSize) {
      throw new IllegalArgumentException(String.format("Number of example %d is more than the maximum size of %d",
        numberExamples, maxSize));
    }
    PactDslJsonArray parent = new PactDslJsonArray("", "", null, true);
    parent.setNumberExamples(numberExamples);
    parent.matchers.addRule("", parent.matchMax(maxSize));
    return new PactDslJsonBody(".", "", parent);
  }

  /**
   * Root level array with maximum size where each item must match the provided matcher
   * @param maxSize maximum size
   */
  public static PactDslJsonArray arrayMaxLike(int maxSize, PactDslJsonRootValue value) {
    return arrayMaxLike(maxSize, 1, value);
  }

  /**
   * Root level array with maximum size where each item must match the provided matcher
   * @param maxSize maximum size
   * @param numberExamples Number of examples to generate
   */
  public static PactDslJsonArray arrayMaxLike(int maxSize, int numberExamples, PactDslJsonRootValue value) {
    if (numberExamples > maxSize) {
      throw new IllegalArgumentException(String.format("Number of example %d is more than the maximum size of %d",
        numberExamples, maxSize));
    }
    PactDslJsonArray parent = new PactDslJsonArray("", "", null, true);
    parent.setNumberExamples(numberExamples);
    parent.matchers.addRule("", parent.matchMax(maxSize));
    parent.putObject(value);
    return parent;
  }

  /**
   * Array with a minimum and maximum size where each item must match the following example
   * @param minSize minimum size
   * @param maxSize maximum size
   */
  public static PactDslJsonBody arrayMinMaxLike(int minSize, int maxSize) {
    return arrayMinMaxLike(minSize, maxSize, minSize);
  }

  /**
   * Array with a minimum and maximum size where each item must match the following example
   * @param minSize minimum size
   * @param maxSize maximum size
   * @param numberExamples Number of examples to generate
   */
  public static PactDslJsonBody arrayMinMaxLike(int minSize, int maxSize, int numberExamples) {
    if (numberExamples < minSize) {
      throw new IllegalArgumentException(String.format("Number of example %d is less than the minimum size of %d",
        numberExamples, minSize));
    } else if (numberExamples > maxSize) {
      throw new IllegalArgumentException(String.format("Number of example %d is more than the maximum size of %d",
        numberExamples, maxSize));
    }
    PactDslJsonArray parent = new PactDslJsonArray("", "", null, true);
    parent.setNumberExamples(numberExamples);
    parent.matchers.addRule("", parent.matchMinMax(minSize, maxSize));
    return new PactDslJsonBody(".", "", parent);
  }

  /**
   * Root level array with minimum and maximum size where each item must match the provided matcher
   * @param minSize minimum size
   * @param maxSize maximum size
   */
  public static PactDslJsonArray arrayMinMaxLike(int minSize, int maxSize, PactDslJsonRootValue value) {
    return arrayMinMaxLike(minSize, maxSize, minSize, value);
  }

  /**
   * Root level array with minimum and maximum size where each item must match the provided matcher
   * @param minSize minimum size
   * @param maxSize maximum size
   * @param numberExamples Number of examples to generate
   */
  public static PactDslJsonArray arrayMinMaxLike(int minSize, int maxSize, int numberExamples, PactDslJsonRootValue value) {
    if (numberExamples < minSize) {
      throw new IllegalArgumentException(String.format("Number of example %d is less than the minimum size of %d",
        numberExamples, minSize));
    } if (numberExamples > maxSize) {
      throw new IllegalArgumentException(String.format("Number of example %d is more than the maximum size of %d",
        numberExamples, maxSize));
    }
    PactDslJsonArray parent = new PactDslJsonArray("", "", null, true);
    parent.setNumberExamples(numberExamples);
    parent.matchers.addRule("", parent.matchMinMax(minSize, maxSize));
    parent.putObject(value);
    return parent;
  }

  /**
   * Root level array where order is ignored
   */
  public static PactDslJsonArray newUnorderedArray() {
    PactDslJsonArray root = new PactDslJsonArray();
    root.matchers.addRule(root.rootPath, EqualsIgnoreOrderMatcher.INSTANCE);
    return root;
  }

  /**
   * Root level array of min size where order is ignored
   * @param size minimum size
   */
  public static PactDslJsonArray newUnorderedMinArray(int size) {
    PactDslJsonArray root = new PactDslJsonArray();
    root.matchers.addRule(root.rootPath, new MinEqualsIgnoreOrderMatcher(size));
    return root;
  }

  /**
   * Root level array of max size where order is ignored
   * @param size maximum size
   */
  public static PactDslJsonArray newUnorderedMaxArray(int size) {
    PactDslJsonArray root = new PactDslJsonArray();
    root.matchers.addRule(root.rootPath, new MaxEqualsIgnoreOrderMatcher(size));
    return root;
  }

  /**
   * Root level array of min and max size where order is ignored
   * @param minSize minimum size
   * @param maxSize maximum size
   */
  public static PactDslJsonArray newUnorderedMinMaxArray(int minSize, int maxSize) {
    if (minSize > maxSize) {
      throw new IllegalArgumentException(String.format("The minimum size of %d is greater than the maximum of %d",
          minSize, maxSize));
    }
    PactDslJsonArray root = new PactDslJsonArray();
    root.matchers.addRule(root.rootPath, new MinMaxEqualsIgnoreOrderMatcher(minSize, maxSize));
    return root;
  }

  /**
   * Adds a null value to the list
   */
  public PactDslJsonArray nullValue() {
    body.add(JsonValue.Null.INSTANCE);
    return this;
  }

  /**
   * Returns the number of example elements to generate for sample bodies
   */
  public int getNumberExamples() {
    return numberExamples;
  }

  /**
   * Sets the number of example elements to generate for sample bodies
   */
  public void setNumberExamples(int numberExamples) {
    this.numberExamples = numberExamples;
  }

  @Override
  public PactDslJsonArray eachArrayLike(String name) {
    throw new UnsupportedOperationException("use the eachArrayLike() form");
  }

  @Override
  public PactDslJsonArray eachArrayLike(String name, int numberExamples) {
    throw new UnsupportedOperationException("use the eachArrayLike(numberExamples) form");
  }

  @Override
  public PactDslJsonArray eachArrayLike() {
    return eachArrayLike(1);
  }

  @Override
  public PactDslJsonArray eachArrayLike(int numberExamples) {
    matchers.addRule(rootPath + appendArrayIndex(1), matchMin(0));
    PactDslJsonArray parent = new PactDslJsonArray(rootPath, "", this, true);
    parent.setNumberExamples(numberExamples);
    return new PactDslJsonArray("", "", parent);
  }

  @Override
  public PactDslJsonArray eachArrayWithMaxLike(String name, Integer size) {
    throw new UnsupportedOperationException("use the eachArrayWithMaxLike() form");
  }

  @Override
  public PactDslJsonArray eachArrayWithMaxLike(String name, int numberExamples, Integer size) {
    throw new UnsupportedOperationException("use the eachArrayWithMaxLike(numberExamples) form");
  }

  @Override
  public PactDslJsonArray eachArrayWithMaxLike(Integer size) {
    return eachArrayWithMaxLike(1, size);
  }

  @Override
  public PactDslJsonArray eachArrayWithMaxLike(int numberExamples, Integer size) {
    if (numberExamples > size) {
      throw new IllegalArgumentException(String.format("Number of example %d is more than the maximum size of %d",
        numberExamples, size));
    }
    matchers.addRule(rootPath + appendArrayIndex(1), matchMax(size));
    PactDslJsonArray parent = new PactDslJsonArray(rootPath, "", this, true);
    parent.setNumberExamples(numberExamples);
    return new PactDslJsonArray("", "", parent);
  }

  @Override
  public PactDslJsonArray eachArrayWithMinLike(String name, Integer size) {
    throw new UnsupportedOperationException("use the eachArrayWithMinLike() form");
  }

  @Override
  public PactDslJsonArray eachArrayWithMinLike(String name, int numberExamples, Integer size) {
    throw new UnsupportedOperationException("use the eachArrayWithMinLike(numberExamples) form");
  }

  @Override
  public PactDslJsonArray eachArrayWithMinLike(Integer size) {
    return eachArrayWithMinLike(size, size);
  }

  @Override
  public PactDslJsonArray eachArrayWithMinLike(int numberExamples, Integer size) {
    if (numberExamples < size) {
      throw new IllegalArgumentException(String.format("Number of example %d is less than the minimum size of %d",
        numberExamples, size));
    }
    matchers.addRule(rootPath + appendArrayIndex(1), matchMin(size));
    PactDslJsonArray parent = new PactDslJsonArray(rootPath, "", this, true);
    parent.setNumberExamples(numberExamples);
    return new PactDslJsonArray("", "", parent);
  }

  /**
   * Array of values that are not objects where each item must match the provided example
   * @param value Value to use to match each item
   */
  public PactDslJsonArray eachLike(PactDslJsonRootValue value) {
    return eachLike(value, 1);
  }

  /**
   * Array of values that are not objects where each item must match the provided example
   * @param value Value to use to match each item
   * @param numberExamples number of examples to generate
   */
  public PactDslJsonArray eachLike(PactDslJsonRootValue value, int numberExamples) {
    if (numberExamples == 0) {
      throw new IllegalArgumentException("Testing Zero examples is unsafe. Please make sure to provide at least one " +
        "example in the Pact provider implementation. See https://github.com/DiUS/pact-jvm/issues/546");
    }

    matchers.addRule(rootPath + appendArrayIndex(1), matchMin(0));
    PactDslJsonArray parent = new PactDslJsonArray(rootPath, "", this, true);
    parent.setNumberExamples(numberExamples);
    parent.putObject(value);
    return (PactDslJsonArray) parent.closeArray();
  }

  /**
   * Array of values with a minimum size that are not objects where each item must match the provided example
   * @param size minimum size of the array
   * @param value Value to use to match each item
   */
  public PactDslJsonArray minArrayLike(Integer size, PactDslJsonRootValue value) {
    return minArrayLike(size, value, size);
  }

  /**
   * Array of values with a minimum size that are not objects where each item must match the provided example
   * @param size minimum size of the array
   * @param value Value to use to match each item
   * @param numberExamples number of examples to generate
   */
  public PactDslJsonArray minArrayLike(Integer size, PactDslJsonRootValue value, int numberExamples) {
    if (numberExamples < size) {
      throw new IllegalArgumentException(String.format("Number of example %d is less than the minimum size of %d",
        numberExamples, size));
    }
    matchers.addRule(rootPath + appendArrayIndex(1), matchMin(size));
    PactDslJsonArray parent = new PactDslJsonArray(rootPath, "", this, true);
    parent.setNumberExamples(numberExamples);
    parent.putObject(value);
    return (PactDslJsonArray) parent.closeArray();
  }

  /**
   * Array of values with a maximum size that are not objects where each item must match the provided example
   * @param size maximum size of the array
   * @param value Value to use to match each item
   */
  public PactDslJsonArray maxArrayLike(Integer size, PactDslJsonRootValue value) {
    return maxArrayLike(size, value, 1);
  }

  /**
   * Array of values with a maximum size that are not objects where each item must match the provided example
   * @param size maximum size of the array
   * @param value Value to use to match each item
   * @param numberExamples number of examples to generate
   */
  public PactDslJsonArray maxArrayLike(Integer size, PactDslJsonRootValue value, int numberExamples) {
    if (numberExamples > size) {
      throw new IllegalArgumentException(String.format("Number of example %d is more than the maximum size of %d",
        numberExamples, size));
    }
    matchers.addRule(rootPath + appendArrayIndex(1), matchMax(size));
    PactDslJsonArray parent = new PactDslJsonArray(rootPath, "", this, true);
    parent.setNumberExamples(numberExamples);
    parent.putObject(value);
    return (PactDslJsonArray) parent.closeArray();
  }

  /**
   * List item that must include the provided string
   * @param value Value that must be included
   */
  public PactDslJsonArray includesStr(String value) {
    body.add(new JsonValue.StringValue(value.toCharArray()));
    matchers.addRule(rootPath + appendArrayIndex(0), includesMatcher(value));
    return this;
  }

  /**
   * Attribute that must be equal to the provided value.
   * @param value Value that will be used for comparisons
   */
  public PactDslJsonArray equalsTo(Object value) {
    body.add(Json.toJson(value));
    matchers.addRule(rootPath + appendArrayIndex(0), EqualsMatcher.INSTANCE);
    return this;
  }

  /**
   * Combine all the matchers using AND
   * @param value Attribute example value
   * @param rules Matching rules to apply
   */
  public PactDslJsonArray and(Object value, MatchingRule... rules) {
    body.add(Json.toJson(value));
    matchers.setRules(rootPath + appendArrayIndex(0), new MatchingRuleGroup(Arrays.asList(rules), RuleLogic.AND));
    return this;
  }

  /**
   * Combine all the matchers using OR
   * @param value Attribute example value
   * @param rules Matching rules to apply
   */
  public PactDslJsonArray or(Object value, MatchingRule... rules) {
    body.add(Json.toJson(value));
    matchers.setRules(rootPath + appendArrayIndex(0), new MatchingRuleGroup(Arrays.asList(rules), RuleLogic.OR));
    return this;
  }

  /**
   * Matches a URL that is composed of a base path and a sequence of path expressions
   * @param basePath The base path for the URL (like "http://localhost:8080/") which will be excluded from the matching
   * @param pathFragments Series of path fragments to match on. These can be strings or regular expressions.
   */
  public PactDslJsonArray matchUrl(String basePath, Object... pathFragments) {
    UrlMatcherSupport urlMatcher = new UrlMatcherSupport(basePath, Arrays.asList(pathFragments));
    String exampleValue = urlMatcher.getExampleValue();
    body.add(new JsonValue.StringValue(exampleValue.toCharArray()));
    String regexExpression = urlMatcher.getRegexExpression();
    matchers.addRule(rootPath + appendArrayIndex(0), regexp(regexExpression));
    if (StringUtils.isEmpty(basePath)) {
      generators.addGenerator(Category.BODY, rootPath + appendArrayIndex(0),
        new MockServerURLGenerator(exampleValue, regexExpression));
    }
    return this;
  }

  @Override
  public DslPart matchUrl(String name, String basePath, Object... pathFragments) {
    throw new UnsupportedOperationException(
      "URL matcher with an attribute name is not supported for arrays. " +
        "Use matchUrl(String base, Object... fragments)");
  }

  @Override
  public PactDslJsonBody matchUrl2(String name, Object... pathFragments) {
    throw new UnsupportedOperationException(
      "URL matcher with an attribute name is not supported for arrays. " +
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

  @Override
  public PactDslJsonBody minMaxArrayLike(String name, Integer minSize, Integer maxSize) {
    throw new UnsupportedOperationException("use the minMaxArrayLike(minSize, maxSize) form");
  }

  @Override
  public PactDslJsonBody minMaxArrayLike(Integer minSize, Integer maxSize) {
    return minMaxArrayLike(minSize, maxSize, minSize);
  }

  @Override
  public PactDslJsonBody minMaxArrayLike(String name, Integer minSize, Integer maxSize, int numberExamples) {
    throw new UnsupportedOperationException("use the minMaxArrayLike(minSize, maxSize, numberExamples) form");
  }

  @Override
  public PactDslJsonBody minMaxArrayLike(Integer minSize, Integer maxSize, int numberExamples) {
    if (minSize > maxSize) {
      throw new IllegalArgumentException(String.format("The minimum size of %d is greater than the maximum of %d",
        minSize, maxSize));
    } else if (numberExamples < minSize) {
      throw new IllegalArgumentException(String.format("Number of example %d is less than the minimum size of %d",
        numberExamples, minSize));
    } else if (numberExamples > maxSize) {
      throw new IllegalArgumentException(String.format("Number of example %d is more than the maximum size of %d",
        numberExamples, maxSize));
    }
    matchers.addRule(rootPath + appendArrayIndex(1), matchMinMax(minSize, maxSize));
    PactDslJsonArray parent = new PactDslJsonArray("", "", this, true);
    parent.setNumberExamples(numberExamples);
    return new PactDslJsonBody(".", "", parent);
  }

  @Override
  public PactDslJsonArray eachArrayWithMinMaxLike(String name, Integer minSize, Integer maxSize) {
    throw new UnsupportedOperationException("use the eachArrayWithMinMaxLike(minSize, maxSize) form");
  }

  @Override
  public PactDslJsonArray eachArrayWithMinMaxLike(Integer minSize, Integer maxSize) {
    return eachArrayWithMinMaxLike(minSize, minSize, maxSize);
  }

  @Override
  public PactDslJsonArray eachArrayWithMinMaxLike(String name, int numberExamples, Integer minSize, Integer maxSize) {
    throw new UnsupportedOperationException("use the eachArrayWithMinMaxLike(numberExamples, minSize, maxSize) form");
  }

  @Override
  public PactDslJsonArray eachArrayWithMinMaxLike(int numberExamples, Integer minSize, Integer maxSize) {
    if (minSize > maxSize) {
      throw new IllegalArgumentException(String.format("The minimum size of %d is greater than the maximum of %d",
        minSize, maxSize));
    } else if (numberExamples < minSize) {
      throw new IllegalArgumentException(String.format("Number of example %d is less than the minimum size of %d",
        numberExamples, minSize));
    } else if (numberExamples > maxSize) {
      throw new IllegalArgumentException(String.format("Number of example %d is more than the maximum size of %d",
        numberExamples, maxSize));
    }
    matchers.addRule(rootPath + appendArrayIndex(1), matchMinMax(minSize, maxSize));
    PactDslJsonArray parent = new PactDslJsonArray(rootPath, "", this, true);
    parent.setNumberExamples(numberExamples);
    return new PactDslJsonArray("", "", parent);
  }

  /**
   * Adds an element that will have it's value injected from the provider state
   * @param expression Expression to be evaluated from the provider state
   * @param example Example value to be used in the consumer test
   */
  public PactDslJsonArray valueFromProviderState(String expression, Object example) {
    body.add(Json.toJson(example));
    generators.addGenerator(Category.BODY, rootPath + appendArrayIndex(0),
            new ProviderStateGenerator(expression, DataType.from(example)));
    matchers.addRule(rootPath + appendArrayIndex(0), TypeMatcher.INSTANCE);
    return this;
  }

  /**
   * Adds a date value formatted as an ISO date with the value generated by the date expression
   * @param expression Date expression to use to generate the values
   */
  public PactDslJsonArray dateExpression(String expression) {
    return dateExpression(expression, DateFormatUtils.ISO_DATE_FORMAT.getPattern());
  }

  /**
   * Adds a date value with the value generated by the date expression
   * @param expression Date expression to use to generate the values
   * @param format Date format to use
   */
  public PactDslJsonArray dateExpression(String expression, String format) {
    FastDateFormat instance = FastDateFormat.getInstance(format);
    body.add(new JsonValue.StringValue(instance.format(new Date(DATE_2000)).toCharArray()));
    generators.addGenerator(Category.BODY, rootPath + appendArrayIndex(0), new DateGenerator(format, expression));
    matchers.addRule(rootPath + appendArrayIndex(0), matchDate(format));
    return this;
  }

  /**
   * Adds a time value formatted as an ISO time with the value generated by the time expression
   * @param expression Time expression to use to generate the values
   */
  public PactDslJsonArray timeExpression(String expression) {
    return timeExpression(expression, DateFormatUtils.ISO_TIME_NO_T_FORMAT.getPattern());
  }

  /**
   * Adds a time value with the value generated by the time expression
   * @param expression Time expression to use to generate the values
   * @param format Time format to use
   */
  public PactDslJsonArray timeExpression(String expression, String format) {
    FastDateFormat instance = FastDateFormat.getInstance(format);
    body.add(new JsonValue.StringValue(instance.format(new Date(DATE_2000)).toCharArray()));
    generators.addGenerator(Category.BODY, rootPath + appendArrayIndex(0), new TimeGenerator(format, expression));
    matchers.addRule(rootPath + appendArrayIndex(0), matchTime(format));
    return this;
  }

  /**
   * Adds a datetime value formatted as an ISO datetime with the value generated by the expression
   * @param expression Datetime expression to use to generate the values
   */
  public PactDslJsonArray datetimeExpression(String expression) {
    return datetimeExpression(expression, DateFormatUtils.ISO_DATETIME_FORMAT.getPattern());
  }

  /**
   * Adds a datetime value with the value generated by the expression
   * @param expression Datetime expression to use to generate the values
   * @param format Datetime format to use
   */
  public PactDslJsonArray datetimeExpression(String expression, String format) {
    FastDateFormat instance = FastDateFormat.getInstance(format);
    body.add(new JsonValue.StringValue(instance.format(new Date(DATE_2000)).toCharArray()));
    generators.addGenerator(Category.BODY, rootPath + appendArrayIndex(0), new DateTimeGenerator(format, expression));
    matchers.addRule(rootPath + appendArrayIndex(0), matchTimestamp(format));
    return this;
  }
}
