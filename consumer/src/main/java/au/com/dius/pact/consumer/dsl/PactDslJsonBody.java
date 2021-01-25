package au.com.dius.pact.consumer.dsl;

import au.com.dius.pact.consumer.InvalidMatcherException;
import au.com.dius.pact.core.matchers.UrlMatcherSupport;
import au.com.dius.pact.core.model.Feature;
import au.com.dius.pact.core.model.FeatureToggles;
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
import au.com.dius.pact.core.model.matchingrules.ValuesMatcher;
import au.com.dius.pact.core.support.Json;
import au.com.dius.pact.core.support.expressions.DataType;
import au.com.dius.pact.core.support.json.JsonValue;
import com.mifmif.common.regex.Generex;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.time.DateFormatUtils;
import org.apache.commons.lang3.time.FastDateFormat;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.Arrays;
import java.util.Date;
import java.util.TimeZone;
import java.util.UUID;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static au.com.dius.pact.consumer.dsl.Dsl.matcherKey;

/**
 * DSL to define a JSON Object
 */
public class PactDslJsonBody extends DslPart {

  private static final String EXAMPLE = "Example \"";
  private final JsonValue.Object body;

  /**
   * Constructs a new body as a root
   */
  public PactDslJsonBody() {
    super(".", "");
    body = new JsonValue.Object();
  }

  /**
   * Constructs a new body as a child
   * @param rootPath Path to prefix to this child
   * @param rootName Name to associate this object as in the parent
   * @param parent Parent to attach to
   */
  public PactDslJsonBody(String rootPath, String rootName, DslPart parent) {
    super(parent, rootPath, rootName);
    body = new JsonValue.Object();
  }

  /**
   * Constructs a new body as a child as a copy of an existing one
   * @param rootPath Path to prefix to this child
   * @param rootName Name to associate this object as in the parent
   * @param parent Parent to attach to
   * @param body Body to copy values from
   */
  public PactDslJsonBody(String rootPath, String rootName, DslPart parent, PactDslJsonBody body) {
    super(parent, rootPath, rootName);
    this.body = body.body;
    this.setMatchers(body.getMatchers().copyWithUpdatedMatcherRootPrefix(rootPath));
    this.setGenerators(body.getGenerators().copyWithUpdatedMatcherRootPrefix(rootPath));
  }

    public String toString() {
        return body.toString();
    }

  public void putObjectPrivate(DslPart object) {
    for (String matcherName: object.getMatchers().getMatchingRules().keySet()) {
      getMatchers().setRules(matcherName, object.getMatchers().getMatchingRules().get(matcherName));
    }
    getGenerators().addGenerators(object.getGenerators());
    String elementBase = StringUtils.difference(this.getRootPath(), object.getRootPath());
    if (StringUtils.isNotEmpty(object.getRootName())) {
      body.add(object.getRootName(), object.getBody());
    } else {
      String name = StringUtils.strip(elementBase, ".");
      Pattern p = Pattern.compile("\\['(.+)'\\]");
      Matcher matcher = p.matcher(name);
      if (matcher.matches()) {
        body.add(matcher.group(1), object.getBody());
      } else {
        body.add(name, object.getBody());
      }
    }
  }

  public void putArrayPrivate(DslPart object) {
    for(String matcherName: object.getMatchers().getMatchingRules().keySet()) {
      getMatchers().setRules(matcherName, object.getMatchers().getMatchingRules().get(matcherName));
    }
    getGenerators().addGenerators(object.getGenerators());
    if (StringUtils.isNotEmpty(object.getRootName())) {
      body.add(object.getRootName(), object.getBody());
    } else {
      body.add(StringUtils.difference(this.getRootPath(), object.getRootPath()), object.getBody());
    }
  }

    @Override
    public JsonValue getBody() {
        return body;
    }

    /**
     * Attribute that must be the specified value
     * @param name attribute name
     * @param value string value
     */
    public PactDslJsonBody stringValue(String name, String value) {
      if (value == null) {
        body.add(name, JsonValue.Null.INSTANCE);
      } else {
        body.add(name, new JsonValue.StringValue(value.toCharArray()));
      }
      return this;
    }

    /**
     * Attribute that must be the specified number
     * @param name attribute name
     * @param value number value
     */
    public PactDslJsonBody numberValue(String name, Number value) {
      body.add(name, new JsonValue.Decimal(value.toString().toCharArray()));
      return this;
    }

    /**
     * Attribute that must be the specified boolean
     * @param name attribute name
     * @param value boolean value
     */
    public PactDslJsonBody booleanValue(String name, Boolean value) {
      body.add(name, value ? JsonValue.True.INSTANCE : JsonValue.False.INSTANCE);
      return this;
    }

    /**
     * Attribute that can be any string
     * @param name attribute name
     */
    public PactDslJsonBody stringType(String name) {
        getGenerators().addGenerator(Category.BODY, matcherKey(name, getRootPath()), new RandomStringGenerator(20));
        return stringType(name, "string");
    }

    /**
     * Attributes that can be any string
     * @param names attribute names
     */
    public PactDslJsonBody stringType(String... names) {
      for (String name: names) {
        stringType(name);
      }
      return this;
    }

    /**
     * Attribute that can be any string
     * @param name attribute name
     * @param example example value to use for generated bodies
     */
    public PactDslJsonBody stringType(String name, String example) {
      body.add(name, new JsonValue.StringValue(example.toCharArray()));
      getMatchers().addRule(matcherKey(name, getRootPath()), TypeMatcher.INSTANCE);
      return this;
    }

    /**
     * Attribute that can be any number
     * @param name attribute name
     */
    public PactDslJsonBody numberType(String name) {
        getGenerators().addGenerator(Category.BODY, matcherKey(name, getRootPath()), new RandomIntGenerator(0, Integer.MAX_VALUE));
        return numberType(name, 100);
    }

    /**
     * Attributes that can be any number
     * @param names attribute names
     */
    public PactDslJsonBody numberType(String... names) {
      for (String name: names) {
        numberType(name);
      }
      return this;
    }

    /**
     * Attribute that can be any number
     * @param name attribute name
     * @param number example number to use for generated bodies
     */
    public PactDslJsonBody numberType(String name, Number number) {
      body.add(name, new JsonValue.Decimal(number.toString().toCharArray()));
      getMatchers().addRule(matcherKey(name, getRootPath()), new NumberTypeMatcher(NumberTypeMatcher.NumberType.NUMBER));
      return this;
    }

    /**
     * Attribute that must be an integer
     * @param name attribute name
     */
    public PactDslJsonBody integerType(String name) {
        getGenerators().addGenerator(Category.BODY, matcherKey(name, getRootPath()), new RandomIntGenerator(0, Integer.MAX_VALUE));
        return integerType(name, 100);
    }

    /**
     * Attributes that must be an integer
     * @param names attribute names
     */
    public PactDslJsonBody integerType(String... names) {
      for (String name: names) {
        integerType(name);
      }
      return this;
    }

    /**
     * Attribute that must be an integer
     * @param name attribute name
     * @param number example integer value to use for generated bodies
     */
    public PactDslJsonBody integerType(String name, Long number) {
      body.add(name, new JsonValue.Integer(number.toString().toCharArray()));
      getMatchers().addRule(matcherKey(name, getRootPath()), new NumberTypeMatcher(NumberTypeMatcher.NumberType.INTEGER));
      return this;
    }

    /**
     * Attribute that must be an integer
     * @param name attribute name
     * @param number example integer value to use for generated bodies
     */
    public PactDslJsonBody integerType(String name, Integer number) {
      body.add(name, new JsonValue.Integer(number.toString().toCharArray()));
      getMatchers().addRule(matcherKey(name, getRootPath()), new NumberTypeMatcher(NumberTypeMatcher.NumberType.INTEGER));
      return this;
    }

  /**
   * Attribute that must be a decimal value
   * @param name attribute name
   */
  public PactDslJsonBody decimalType(String name) {
      getGenerators().addGenerator(Category.BODY, matcherKey(name, getRootPath()), new RandomDecimalGenerator(10));
      return decimalType(name, 100.0);
  }

  /**
   * Attributes that must be a decimal values
   * @param names attribute names
   */
  public PactDslJsonBody decimalType(String... names) {
    for (String name: names) {
      decimalType(name);
    }
    return this;
  }

  /**
   * Attribute that must be a decimalType value
   * @param name attribute name
   * @param number example decimalType value
   */
  public PactDslJsonBody decimalType(String name, BigDecimal number) {
    body.add(name, new JsonValue.Decimal(number.toString().toCharArray()));
    getMatchers().addRule(matcherKey(name, getRootPath()), new NumberTypeMatcher(NumberTypeMatcher.NumberType.DECIMAL));
    return this;
  }

  /**
   * Attribute that must be a decimalType value
   * @param name attribute name
   * @param number example decimalType value
   */
  public PactDslJsonBody decimalType(String name, Double number) {
    body.add(name, new JsonValue.Decimal(number.toString().toCharArray()));
    getMatchers().addRule(matcherKey(name, getRootPath()), new NumberTypeMatcher(NumberTypeMatcher.NumberType.DECIMAL));
    return this;
  }

    /**
     * Attribute that must be a boolean
     * @param name attribute name
     */
    public PactDslJsonBody booleanType(String name) {
        return booleanType(name, true);
    }

    /**
     * Attributes that must be a boolean
     * @param names attribute names
     */
    public PactDslJsonBody booleanType(String... names) {
      for (String name: names) {
        booleanType(name);
      }
      return this;
    }

    /**
     * Attribute that must be a boolean
     * @param name attribute name
     * @param example example boolean to use for generated bodies
     */
    public PactDslJsonBody booleanType(String name, Boolean example) {
      body.add(name, example ? JsonValue.True.INSTANCE : JsonValue.False.INSTANCE);
      getMatchers().addRule(matcherKey(name, getRootPath()), TypeMatcher.INSTANCE);
      return this;
    }

    /**
     * Attribute that must match the regular expression
     * @param name attribute name
     * @param regex regular expression
     * @param value example value to use for generated bodies
     */
    public PactDslJsonBody stringMatcher(String name, String regex, String value) {
      if (!value.matches(regex)) {
        throw new InvalidMatcherException(EXAMPLE + value + "\" does not match regular expression \"" +
            regex + "\"");
      }
      body.add(name, new JsonValue.StringValue(value.toCharArray()));
      getMatchers().addRule(matcherKey(name, getRootPath()), regexp(regex));
      return this;
    }

    /**
     * Attribute that must match the regular expression
     * @param name attribute name
     * @param regex regular expression
     */
    public PactDslJsonBody stringMatcher(String name, String regex) {
      getGenerators().addGenerator(Category.BODY, matcherKey(name, getRootPath()), new RegexGenerator(regex));
      stringMatcher(name, regex, new Generex(regex).random());
      return this;
    }

    /**
     * Attribute named 'timestamp' that must be an ISO formatted timestamp
     */
    public PactDslJsonBody timestamp() {
        return timestamp("timestamp");
    }

    /**
     * Attribute that must be an ISO formatted timestamp
     * @param name
     * @deprecated Use datetime instead
     */
    @Deprecated
    public PactDslJsonBody timestamp(String name) {
      datetime(name);
      return this;
    }

    /**
     * Attribute that must match the given datetime format
     * @param name attribute name
     * @param format timestamp format
     * @deprecated use datetime instead
     */
    @Deprecated
    public PactDslJsonBody timestamp(String name, String format) {
        datetime(name, format);
        return this;
    }

    /**
     * Attribute that must match the given timestamp format
     * @param name attribute name
     * @param format timestamp format
     * @param example example date and time to use for generated bodies
     * @deprecated use datetime instead
     */
    @Deprecated
    public PactDslJsonBody timestamp(String name, String format, Date example) {
        return datetime(name, format, example, TimeZone.getDefault());
    }

    /**
     * Attribute that must match the given timestamp format
     * @param name attribute name
     * @param format timestamp format
     * @param example example date and time to use for generated bodies
     * @param timeZone time zone used for formatting of example date and time
     * @deprecated use datetime instead
     */
    @Deprecated
    public PactDslJsonBody timestamp(String name, String format, Date example, TimeZone timeZone) {
        datetime(name, format, example, timeZone);
        return this;
    }

    /**
     * Attribute that must match the given timestamp format
     * @param name attribute name
     * @param format timestamp format
     * @param example example date and time to use for generated bodies
     * @deprecated use datetime instead
     */
    @Deprecated
    public PactDslJsonBody timestamp(String name, String format, Instant example) {
      return datetime(name, format, example, TimeZone.getDefault());
    }

    /**
     * Attribute that must match the given timestamp format
     * @param name attribute name
     * @param format timestamp format
     * @param example example date and time to use for generated bodies
     * @param timeZone time zone used for formatting of example date and time
     * @deprecated use datetime instead
     */
    @Deprecated
    public PactDslJsonBody timestamp(String name, String format, Instant example, TimeZone timeZone) {
      datetime(name, format, example, timeZone);
      return this;
    }

  /**
   * Attribute that must be an ISO formatted datetime
   * @param name
   */
  public PactDslJsonBody datetime(String name) {
    String pattern = DateFormatUtils.ISO_DATETIME_FORMAT.getPattern();
    getGenerators().addGenerator(Category.BODY, matcherKey(name, getRootPath()), new DateTimeGenerator(pattern, null));
    body.add(name, new JsonValue.StringValue(
      DateFormatUtils.ISO_DATETIME_FORMAT.format(new Date(DATE_2000)).toCharArray()));
    getMatchers().addRule(matcherKey(name, getRootPath()), matchTimestamp(pattern));
    return this;
  }

  /**
   * Attribute that must match the given datetime format
   * @param name attribute name
   * @param format datetime format
   */
  public PactDslJsonBody datetime(String name, String format) {
    getGenerators().addGenerator(Category.BODY, matcherKey(name, getRootPath()), new DateTimeGenerator(format, null));
    DateTimeFormatter formatter = DateTimeFormatter.ofPattern(format).withZone(ZoneId.systemDefault());
    body.add(name, new JsonValue.StringValue(formatter.format(new Date(DATE_2000).toInstant()).toCharArray()));
    getMatchers().addRule(matcherKey(name, getRootPath()), matchTimestamp(format));
    return this;
  }

  /**
   * Attribute that must match the given datetime format
   * @param name attribute name
   * @param format datetime format
   * @param example example date and time to use for generated bodies
   */
  public PactDslJsonBody datetime(String name, String format, Date example) {
    return datetime(name, format, example, TimeZone.getDefault());
  }

  /**
   * Attribute that must match the given datetime format
   * @param name attribute name
   * @param format datetime format
   * @param example example date and time to use for generated bodies
   * @param timeZone time zone used for formatting of example date and time
   */
  public PactDslJsonBody datetime(String name, String format, Date example, TimeZone timeZone) {
    DateTimeFormatter formatter = DateTimeFormatter.ofPattern(format).withZone(timeZone.toZoneId());
    body.add(name, new JsonValue.StringValue(formatter.format(example.toInstant()).toCharArray()));
    getMatchers().addRule(matcherKey(name, getRootPath()), matchTimestamp(format));
    return this;
  }

  /**
   * Attribute that must match the given datetime format
   * @param name attribute name
   * @param format datetime format
   * @param example example date and time to use for generated bodies
   */
  public PactDslJsonBody datetime(String name, String format, Instant example) {
    return datetime(name, format, example, TimeZone.getDefault());
  }

  /**
   * Attribute that must match the given datetime format
   * @param name attribute name
   * @param format timestamp format
   * @param example example date and time to use for generated bodies
   * @param timeZone time zone used for formatting of example date and time
   */
  public PactDslJsonBody datetime(String name, String format, Instant example, TimeZone timeZone) {
    DateTimeFormatter formatter = DateTimeFormatter.ofPattern(format).withZone(timeZone.toZoneId());
    body.add(name, new JsonValue.StringValue(formatter.format(example).toCharArray()));
    getMatchers().addRule(matcherKey(name, getRootPath()), matchTimestamp(format));
    return this;
  }

    /**
     * Attribute named 'date' that must be formatted as an ISO date
     */
    public PactDslJsonBody date() {
        return date("date");
    }

    /**
     * Attribute that must be formatted as an ISO date
     * @param name attribute name
     */
    public PactDslJsonBody date(String name) {
      String pattern = DateFormatUtils.ISO_DATE_FORMAT.getPattern();
      getGenerators().addGenerator(Category.BODY, matcherKey(name, getRootPath()), new DateGenerator(pattern, null));
      body.add(name, new JsonValue.StringValue(
        DateFormatUtils.ISO_DATE_FORMAT.format(new Date(DATE_2000)).toCharArray()));
      getMatchers().addRule(matcherKey(name, getRootPath()), matchDate(pattern));
      return this;
    }

    /**
     * Attribute that must match the provided date format
     * @param name attribute date
     * @param format date format to match
     */
    public PactDslJsonBody date(String name, String format) {
      getGenerators().addGenerator(Category.BODY, matcherKey(name, getRootPath()), new DateGenerator(format, null));
      FastDateFormat instance = FastDateFormat.getInstance(format);
      body.add(name, new JsonValue.StringValue(instance.format(new Date(DATE_2000)).toCharArray()));
      getMatchers().addRule(matcherKey(name, getRootPath()), matchDate(format));
      return this;
    }

    /**
     * Attribute that must match the provided date format
     * @param name attribute date
     * @param format date format to match
     * @param example example date to use for generated values
     */
    public PactDslJsonBody date(String name, String format, Date example) {
        return date(name, format, example, TimeZone.getDefault());
    }

    /**
     * Attribute that must match the provided date format
     * @param name attribute date
     * @param format date format to match
     * @param example example date to use for generated values
     * @param timeZone time zone used for formatting of example date
     */
    public PactDslJsonBody date(String name, String format, Date example, TimeZone timeZone) {
      FastDateFormat instance = FastDateFormat.getInstance(format, timeZone);
      body.add(name, new JsonValue.StringValue(instance.format(example).toCharArray()));
      getMatchers().addRule(matcherKey(name, getRootPath()), matchDate(format));
      return this;
    }

    /**
     * Attribute named 'time' that must be an ISO formatted time
     */
    public PactDslJsonBody time() {
        return time("time");
    }

    /**
     * Attribute that must be an ISO formatted time
     * @param name attribute name
     */
    public PactDslJsonBody time(String name) {
      String pattern = DateFormatUtils.ISO_TIME_FORMAT.getPattern();
      getGenerators().addGenerator(Category.BODY, matcherKey(name, getRootPath()), new TimeGenerator(pattern, null));
      body.add(name, new JsonValue.StringValue(
        DateFormatUtils.ISO_TIME_FORMAT.format(new Date(DATE_2000)).toCharArray()));
      getMatchers().addRule(matcherKey(name, getRootPath()), matchTime(pattern));
      return this;
    }

    /**
     * Attribute that must match the given time format
     * @param name attribute name
     * @param format time format to match
     */
    public PactDslJsonBody time(String name, String format) {
      getGenerators().addGenerator(Category.BODY, matcherKey(name, getRootPath()), new TimeGenerator(format, null));
      FastDateFormat instance = FastDateFormat.getInstance(format);
      body.add(name, new JsonValue.StringValue(instance.format(new Date(DATE_2000)).toCharArray()));
      getMatchers().addRule(matcherKey(name, getRootPath()), matchTime(format));
      return this;
    }

    /**
     * Attribute that must match the given time format
     * @param name attribute name
     * @param format time format to match
     * @param example example time to use for generated bodies
     */
    public PactDslJsonBody time(String name, String format, Date example) {
        return time(name, format, example, TimeZone.getDefault());
    }

    /**
     * Attribute that must match the given time format
     * @param name attribute name
     * @param format time format to match
     * @param example example time to use for generated bodies
     * @param timeZone time zone used for formatting of example time
     */
    public PactDslJsonBody time(String name, String format, Date example, TimeZone timeZone) {
      FastDateFormat instance = FastDateFormat.getInstance(format, timeZone);
      body.add(name, new JsonValue.StringValue(instance.format(example).toCharArray()));
      getMatchers().addRule(matcherKey(name, getRootPath()), matchTime(format));
      return this;
    }

    /**
     * Attribute that must be an IP4 address
     * @param name attribute name
     */
    public PactDslJsonBody ipAddress(String name) {
      body.add(name, new JsonValue.StringValue("127.0.0.1".toCharArray()));
      getMatchers().addRule(matcherKey(name, getRootPath()), regexp("(\\d{1,3}\\.)+\\d{1,3}"));
      return this;
    }

    /**
     * Attribute that is a JSON object
     * @param name field name
     */
    public PactDslJsonBody object(String name) {
      return new PactDslJsonBody(matcherKey(name, getRootPath()) + ".", "", this);
    }

    public PactDslJsonBody object() {
        throw new UnsupportedOperationException("use the object(String name) form");
    }

  /**
   * Attribute that is a JSON object defined from a DSL part
   * @param name field name
   * @param value DSL Part to set the value as
   */
  public PactDslJsonBody object(String name, DslPart value) {
    String base = matcherKey(name, getRootPath());
    if (value instanceof PactDslJsonBody) {
      PactDslJsonBody object = new PactDslJsonBody(base, "", this, (PactDslJsonBody) value);
      putObjectPrivate(object);
    } else if (value instanceof PactDslJsonArray) {
      PactDslJsonArray object = new PactDslJsonArray(base, "", this, (PactDslJsonArray) value);
      putArrayPrivate(object);
    }
    return this;
  }

  /**
   * Closes the current JSON object
   */
  public DslPart closeObject() {
    if (getParent() != null) {
      getParent().putObjectPrivate(this);
    } else {
      getMatchers().applyMatcherRootPrefix("$");
      getGenerators().applyRootPrefix("$");
    }
    setClosed(true);
    return getParent();
  }

  @Override
  public DslPart close() {
    DslPart parentToReturn = this;

    if (!getClosed()) {
      DslPart parent = closeObject();
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

  /**
     * Attribute that is an array
     * @param name field name
     */
    public PactDslJsonArray array(String name) {
        return new PactDslJsonArray(matcherKey(name, getRootPath()), name, this);
    }

    public PactDslJsonArray array() {
        throw new UnsupportedOperationException("use the array(String name) form");
    }

  @Override
  public PactDslJsonArray unorderedArray(String name) {
    getMatchers().addRule(matcherKey(name, getRootPath()), EqualsIgnoreOrderMatcher.INSTANCE);
    return this.array(name);
  }

  @Override
  public PactDslJsonArray unorderedArray() {
    throw new UnsupportedOperationException("use the unorderedArray(String name) form");
  }

  @Override
  public PactDslJsonArray unorderedMinArray(String name, int size) {
    getMatchers().addRule(matcherKey(name, getRootPath()), new MinEqualsIgnoreOrderMatcher(size));
    return this.array(name);
  }

  @Override
  public PactDslJsonArray unorderedMinArray(int size) {
    throw new UnsupportedOperationException("use the unorderedMinArray(String name, int size) form");
  }

  @Override
  public PactDslJsonArray unorderedMaxArray(String name, int size) {
    getMatchers().addRule(matcherKey(name, getRootPath()), new MaxEqualsIgnoreOrderMatcher(size));
    return this.array(name);
  }

  @Override
  public PactDslJsonArray unorderedMaxArray(int size) {
    throw new UnsupportedOperationException("use the unorderedMaxArray(String name, int size) form");
  }

  @Override
  public PactDslJsonArray unorderedMinMaxArray(String name, int minSize, int maxSize) {
    if (minSize > maxSize) {
      throw new IllegalArgumentException(String.format("The minimum size of %d is greater than the maximum of %d",
          minSize, maxSize));
    }
    getMatchers().addRule(matcherKey(name, getRootPath()), new MinMaxEqualsIgnoreOrderMatcher(minSize, maxSize));
    return this.array(name);
  }

  @Override
  public PactDslJsonArray unorderedMinMaxArray(int minSize, int maxSize) {
    throw new UnsupportedOperationException("use the unorderedMinMaxArray(String name, int minSize, int maxSize) form");
  }

    /**
     * Closes the current array
     */
    @Override
    public DslPart closeArray() {
      if (getParent() instanceof PactDslJsonArray) {
        closeObject();
        return getParent().closeArray();
      } else {
        throw new UnsupportedOperationException("can't call closeArray on an Object");
      }
    }

    /**
     * Attribute that is an array where each item must match the following example
     * @param name field name
     */
    @Override
    public PactDslJsonBody eachLike(String name) {
        return eachLike(name, 1);
    }

  @Override
  public PactDslJsonBody eachLike(String name, DslPart object) {
    String base = matcherKey(name, getRootPath());
    getMatchers().addRule(base, matchMin(0));
    PactDslJsonArray parent = new PactDslJsonArray(matcherKey(name, getRootPath()), "", this, true);

    if (object instanceof PactDslJsonBody) {
      parent.putObjectPrivate(object);
    } else if (object instanceof PactDslJsonArray) {
      parent.putArrayPrivate(object);
    }

    return (PactDslJsonBody) parent.closeArray();
  }

    @Override
    public PactDslJsonBody eachLike() {
        throw new UnsupportedOperationException("use the eachLike(String name) form");
    }

  @Override
  public PactDslJsonArray eachLike(DslPart object) {
    throw new UnsupportedOperationException("use the eachLike(String name, DslPart object) form");
  }

    /**
     * Attribute that is an array where each item must match the following example
     * @param name field name
     * @param numberExamples number of examples to generate
     */
    @Override
    public PactDslJsonBody eachLike(String name, int numberExamples) {
      getMatchers().addRule(matcherKey(name, getRootPath()), matchMin(0));
      PactDslJsonArray parent = new PactDslJsonArray(matcherKey(name, getRootPath()), "", this, true);
      parent.setNumberExamples(numberExamples);
      return new PactDslJsonBody(".", ".", parent);
    }

    @Override
    public PactDslJsonBody eachLike(int numberExamples) {
      throw new UnsupportedOperationException("use the eachLike(String name, int numberExamples) form");
    }

    /**
     * Attribute that is an array of values that are not objects where each item must match the following example
     * @param name field name
     * @param value Value to use to match each item
     */
    public PactDslJsonBody eachLike(String name, PactDslJsonRootValue value) {
      return eachLike(name, value, 1);
    }

    /**
     * Attribute that is an array of values that are not objects where each item must match the following example
     * @param name field name
     * @param value Value to use to match each item
     * @param numberExamples number of examples to generate
     */
    public PactDslJsonBody eachLike(String name, PactDslJsonRootValue value, int numberExamples) {
      getMatchers().addRule(matcherKey(name, getRootPath()), matchMin(0));
      PactDslJsonArray parent = new PactDslJsonArray(matcherKey(name, getRootPath()), "", this, true);
      parent.setNumberExamples(numberExamples);
      parent.putObjectPrivate(value);
      return (PactDslJsonBody) parent.closeArray();
    }

    /**
     * Attribute that is an array with a minimum size where each item must match the following example
     * @param name field name
     * @param size minimum size of the array
     */
    @Override
    public PactDslJsonBody minArrayLike(String name, int size) {
        return minArrayLike(name, size, size);
    }

    @Override
    public PactDslJsonBody minArrayLike(int size) {
        throw new UnsupportedOperationException("use the minArrayLike(String name, Integer size) form");
    }

  @Override
  public PactDslJsonBody minArrayLike(String name, int size, DslPart object) {
    String base = matcherKey(name, getRootPath());
    getMatchers().addRule(base, matchMin(size));
    PactDslJsonArray parent = new PactDslJsonArray(matcherKey(name, getRootPath()), "", this, true);

    if (object instanceof PactDslJsonBody) {
      parent.putObjectPrivate(object);
    } else if (object instanceof PactDslJsonArray) {
      parent.putArrayPrivate(object);
    }

    return (PactDslJsonBody) parent.closeArray();
  }

  @Override
  public PactDslJsonArray minArrayLike(int size, DslPart object) {
    throw new UnsupportedOperationException("use the minArrayLike(String name, Integer size, DslPart object) form");
  }

    /**
     * Attribute that is an array with a minimum size where each item must match the following example
     * @param name field name
     * @param size minimum size of the array
     * @param numberExamples number of examples to generate
     */
    @Override
    public PactDslJsonBody minArrayLike(String name, int size, int numberExamples) {
      if (numberExamples < size) {
        throw new IllegalArgumentException(String.format("Number of example %d is less than the minimum size of %d",
          numberExamples, size));
      }
      getMatchers().addRule(matcherKey(name, getRootPath()), matchMin(size));
      PactDslJsonArray parent = new PactDslJsonArray(matcherKey(name, getRootPath()), "", this, true);
      parent.setNumberExamples(numberExamples);
      return new PactDslJsonBody(".", "", parent);
    }

    @Override
    public PactDslJsonBody minArrayLike(int size, int numberExamples) {
      throw new UnsupportedOperationException("use the minArrayLike(String name, Integer size, int numberExamples) form");
    }

    /**
     * Attribute that is an array of values with a minimum size that are not objects where each item must match the following example
     * @param name field name
     * @param size minimum size of the array
     * @param value Value to use to match each item
     */
    public PactDslJsonBody minArrayLike(String name, int size, PactDslJsonRootValue value) {
      return minArrayLike(name, size, value, 2);
    }

    /**
     * Attribute that is an array of values with a minimum size that are not objects where each item must match the following example
     * @param name field name
     * @param size minimum size of the array
     * @param value Value to use to match each item
     * @param numberExamples number of examples to generate
     */
    public PactDslJsonBody minArrayLike(String name, int size, PactDslJsonRootValue value, int numberExamples) {
      if (numberExamples < size) {
        throw new IllegalArgumentException(String.format("Number of example %d is less than the minimum size of %d",
          numberExamples, size));
      }
      getMatchers().addRule(matcherKey(name, getRootPath()), matchMin(size));
      PactDslJsonArray parent = new PactDslJsonArray(matcherKey(name, getRootPath()), "", this, true);
      parent.setNumberExamples(numberExamples);
      parent.putObjectPrivate(value);
      return (PactDslJsonBody) parent.closeArray();
    }

    /**
     * Attribute that is an array with a maximum size where each item must match the following example
     * @param name field name
     * @param size maximum size of the array
     */
    @Override
    public PactDslJsonBody maxArrayLike(String name, int size) {
        return maxArrayLike(name, size, 1);
    }

    @Override
    public PactDslJsonBody maxArrayLike(int size) {
        throw new UnsupportedOperationException("use the maxArrayLike(String name, Integer size) form");
    }

  @Override
  public PactDslJsonBody maxArrayLike(String name, int size, DslPart object) {
    String base = matcherKey(name, getRootPath());
    getMatchers().addRule(base, matchMax(size));
    PactDslJsonArray parent = new PactDslJsonArray(matcherKey(name, getRootPath()), "", this, true);

    if (object instanceof PactDslJsonBody) {
      parent.putObjectPrivate(object);
    } else if (object instanceof PactDslJsonArray) {
      parent.putArrayPrivate(object);
    }

    return (PactDslJsonBody) parent.closeArray();
  }

  @Override
  public PactDslJsonArray maxArrayLike(int size, DslPart object) {
    throw new UnsupportedOperationException("use the maxArrayLike(String name, Integer size, DslPart object) form");
  }

    /**
     * Attribute that is an array with a maximum size where each item must match the following example
     * @param name field name
     * @param size maximum size of the array
     * @param numberExamples number of examples to generate
     */
    @Override
    public PactDslJsonBody maxArrayLike(String name, int size, int numberExamples) {
      if (numberExamples > size) {
        throw new IllegalArgumentException(String.format("Number of example %d is more than the maximum size of %d",
          numberExamples, size));
      }
      getMatchers().addRule(matcherKey(name, getRootPath()), matchMax(size));
      PactDslJsonArray parent = new PactDslJsonArray(matcherKey(name, getRootPath()), "", this, true);
      parent.setNumberExamples(numberExamples);
      return new PactDslJsonBody(".", "", parent);
    }

    @Override
    public PactDslJsonBody maxArrayLike(int size, int numberExamples) {
      throw new UnsupportedOperationException("use the maxArrayLike(String name, Integer size, int numberExamples) form");
    }

    /**
     * Attribute that is an array of values with a maximum size that are not objects where each item must match the following example
     * @param name field name
     * @param size maximum size of the array
     * @param value Value to use to match each item
     */
    public PactDslJsonBody maxArrayLike(String name, int size, PactDslJsonRootValue value) {
      return maxArrayLike(name, size, value, 1);
    }

    /**
     * Attribute that is an array of values with a maximum size that are not objects where each item must match the following example
     * @param name field name
     * @param size maximum size of the array
     * @param value Value to use to match each item
     * @param numberExamples number of examples to generate
     */
    public PactDslJsonBody maxArrayLike(String name, int size, PactDslJsonRootValue value, int numberExamples) {
      if (numberExamples > size) {
        throw new IllegalArgumentException(String.format("Number of example %d is more than the maximum size of %d",
          numberExamples, size));
      }
      getMatchers().addRule(matcherKey(name, getRootPath()), matchMax(size));
      PactDslJsonArray parent = new PactDslJsonArray(matcherKey(name, getRootPath()), "", this, true);
      parent.setNumberExamples(numberExamples);
      parent.putObjectPrivate(value);
      return (PactDslJsonBody) parent.closeArray();
    }

    /**
     * Attribute named 'id' that must be a numeric identifier
     */
    public PactDslJsonBody id() {
        return id("id");
    }

    /**
     * Attribute that must be a numeric identifier
     * @param name attribute name
     */
    public PactDslJsonBody id(String name) {
      getGenerators().addGenerator(Category.BODY, matcherKey(name, getRootPath()), new RandomIntGenerator(0, Integer.MAX_VALUE));
      body.add(name, new JsonValue.Integer("1234567890".toCharArray()));
      getMatchers().addRule(matcherKey(name, getRootPath()), TypeMatcher.INSTANCE);
      return this;
    }

    /**
     * Attribute that must be a numeric identifier
     * @param name attribute name
     * @param id example id to use for generated bodies
     */
    public PactDslJsonBody id(String name, Long id) {
      body.add(name, new JsonValue.Integer(id.toString().toCharArray()));
      getMatchers().addRule(matcherKey(name, getRootPath()), TypeMatcher.INSTANCE);
      return this;
    }

    /**
     * Attribute that must be encoded as a hexadecimal value
     * @param name attribute name
     */
    public PactDslJsonBody hexValue(String name) {
      getGenerators().addGenerator(Category.BODY, matcherKey(name, getRootPath()), new RandomHexadecimalGenerator(10));
      return hexValue(name, "1234a");
    }

    /**
     * Attribute that must be encoded as a hexadecimal value
     * @param name attribute name
     * @param hexValue example value to use for generated bodies
     */
    public PactDslJsonBody hexValue(String name, String hexValue) {
      if (!hexValue.matches(HEXADECIMAL)) {
        throw new InvalidMatcherException(EXAMPLE + hexValue + "\" is not a hexadecimal value");
      }
      body.add(name, new JsonValue.StringValue(hexValue.toCharArray()));
      getMatchers().addRule(matcherKey(name, getRootPath()), regexp("[0-9a-fA-F]+"));
      return this;
    }

    /**
     * Attribute that must be encoded as an UUID
     * @param name attribute name
     */
    public PactDslJsonBody uuid(String name) {
      getGenerators().addGenerator(Category.BODY, matcherKey(name, getRootPath()), UuidGenerator.INSTANCE);
      return uuid(name, "e2490de5-5bd3-43d5-b7c4-526e33f71304");
    }

    /**
     * Attribute that must be encoded as an UUID
     * @param name attribute name
     * @param uuid example UUID to use for generated bodies
     */
    public PactDslJsonBody uuid(String name, UUID uuid) {
        return uuid(name, uuid.toString());
    }

    /**
     * Attribute that must be encoded as an UUID
     * @param name attribute name
     * @param uuid example UUID to use for generated bodies
     */
    public PactDslJsonBody uuid(String name, String uuid) {
      if (!uuid.matches(UUID_REGEX)) {
        throw new InvalidMatcherException(EXAMPLE + uuid + "\" is not an UUID");
      }
      body.add(name, new JsonValue.StringValue(uuid.toCharArray()));
      getMatchers().addRule(matcherKey(name, getRootPath()), regexp(UUID_REGEX));
      return this;
    }

    /**
     * Sets the field to a null value
     * @param fieldName field name
     */
    public PactDslJsonBody nullValue(String fieldName) {
      body.add(fieldName, JsonValue.Null.INSTANCE);
      return this;
    }

  @Override
  public PactDslJsonArray eachArrayLike(String name) {
    return eachArrayLike(name, 1);
  }

  @Override
  public PactDslJsonArray eachArrayLike() {
    throw new UnsupportedOperationException("use the eachArrayLike(String name) form");
  }

  @Override
  public PactDslJsonArray eachArrayLike(String name, int numberExamples) {
    getMatchers().addRule(matcherKey(name, getRootPath()), matchMin(0));
    PactDslJsonArray parent = new PactDslJsonArray(matcherKey(name, getRootPath()), name, this, true);
    parent.setNumberExamples(numberExamples);
    return new PactDslJsonArray("", "", parent);
  }

  @Override
  public PactDslJsonArray eachArrayLike(int numberExamples) {
    throw new UnsupportedOperationException("use the eachArrayLike(String name, int numberExamples) form");
  }

  @Override
  public PactDslJsonArray eachArrayWithMaxLike(String name, int size) {
    return eachArrayWithMaxLike(name, 1, size);
  }

  @Override
  public PactDslJsonArray eachArrayWithMaxLike(int size) {
    throw new UnsupportedOperationException("use the eachArrayWithMaxLike(String name, Integer size) form");
  }

  @Override
  public PactDslJsonArray eachArrayWithMaxLike(String name, int numberExamples, int size) {
    if (numberExamples > size) {
      throw new IllegalArgumentException(String.format("Number of example %d is more than the maximum size of %d",
        numberExamples, size));
    }
    getMatchers().addRule(matcherKey(name, getRootPath()), matchMax(size));
    PactDslJsonArray parent = new PactDslJsonArray(matcherKey(name, getRootPath()), name, this, true);
    parent.setNumberExamples(numberExamples);
    return new PactDslJsonArray("", "", parent);
  }

  @Override
  public PactDslJsonArray eachArrayWithMaxLike(int numberExamples, int size) {
    throw new UnsupportedOperationException("use the eachArrayWithMaxLike(String name, int numberExamples, Integer size) form");
  }

  @Override
  public PactDslJsonArray eachArrayWithMinLike(String name, int size) {
    return eachArrayWithMinLike(name, size, size);
  }

  @Override
  public PactDslJsonArray eachArrayWithMinLike(int size) {
    throw new UnsupportedOperationException("use the eachArrayWithMinLike(String name, Integer size) form");
  }

  @Override
  public PactDslJsonArray eachArrayWithMinLike(String name, int numberExamples, int size) {
    if (numberExamples < size) {
      throw new IllegalArgumentException(String.format("Number of example %d is less than the minimum size of %d",
        numberExamples, size));
    }
    getMatchers().addRule(matcherKey(name, getRootPath()), matchMin(size));
    PactDslJsonArray parent = new PactDslJsonArray(matcherKey(name, getRootPath()), name, this, true);
    parent.setNumberExamples(numberExamples);
    return new PactDslJsonArray("", "", parent);
  }

  @Override
  public PactDslJsonArray eachArrayWithMinLike(int numberExamples, int size) {
    throw new UnsupportedOperationException("use the eachArrayWithMinLike(String name, int numberExamples, Integer size) form");
  }

  /**
   * Accepts any key, and each key is mapped to a list of items that must match the following object definition
   * Note: this needs the Java system property "pact.matching.wildcard" set to value "true" when the pact file is verified.
   * @param exampleKey Example key to use for generating bodies
   */
  public PactDslJsonBody eachKeyMappedToAnArrayLike(String exampleKey) {
    if (FeatureToggles.isFeatureSet(Feature.UseMatchValuesMatcher)) {
      getMatchers().addRule(getRootPath().endsWith(".") ? getRootPath().substring(0, getRootPath().length() - 1) : getRootPath(), ValuesMatcher.INSTANCE);
    } else {
      getMatchers().addRule(getRootPath() + "*", matchMin(0));
    }
    PactDslJsonArray parent = new PactDslJsonArray(getRootPath() + "*", exampleKey, this, true);
    return new PactDslJsonBody(".", "", parent);
  }

  /**
   * Accepts any key, and each key is mapped to a map that must match the following object definition
   * Note: this needs the Java system property "pact.matching.wildcard" set to value "true" when the pact file is verified.
   * @param exampleKey Example key to use for generating bodies
   */
  public PactDslJsonBody eachKeyLike(String exampleKey) {
    if (FeatureToggles.isFeatureSet(Feature.UseMatchValuesMatcher)) {
      getMatchers().addRule(getRootPath().endsWith(".") ? getRootPath().substring(0, getRootPath().length() - 1) : getRootPath(), ValuesMatcher.INSTANCE);
    } else {
      getMatchers().addRule(getRootPath() + "*", TypeMatcher.INSTANCE);
    }
    return new PactDslJsonBody(getRootPath() + "*.", exampleKey, this);
  }

  /**
   * Accepts any key, and each key is mapped to a map that must match the provided object definition
   * Note: this needs the Java system property "pact.matching.wildcard" set to value "true" when the pact file is verified.
   * @param exampleKey Example key to use for generating bodies
   * @param value Value to use for matching and generated bodies
   */
  public PactDslJsonBody eachKeyLike(String exampleKey, PactDslJsonRootValue value) {
    body.add(exampleKey, value.getBody());
    if (FeatureToggles.isFeatureSet(Feature.UseMatchValuesMatcher)) {
      getMatchers().addRule(getRootPath().endsWith(".") ? getRootPath().substring(0, getRootPath().length() - 1) : getRootPath(), ValuesMatcher.INSTANCE);
    }
    for(String matcherName: value.getMatchers().getMatchingRules().keySet()) {
      getMatchers().addRules(getRootPath() + "*" + matcherName, value.getMatchers().getMatchingRules().get(matcherName).getRules());
    }
    return this;
  }

  /**
   * Attribute that must include the provided string value
   * @param name attribute name
   * @param value Value that must be included
   */
  public PactDslJsonBody includesStr(String name, String value) {
    body.add(name, new JsonValue.StringValue(value.toCharArray()));
    getMatchers().addRule(matcherKey(name, getRootPath()), includesMatcher(value));
    return this;
  }

  /**
   * Attribute that must be equal to the provided value.
   * @param name attribute name
   * @param value Value that will be used for comparisons
   */
  public PactDslJsonBody equalTo(String name, Object value) {
    body.add(name, Json.toJson(value));
    getMatchers().addRule(matcherKey(name, getRootPath()), EqualsMatcher.INSTANCE);
    return this;
  }

  /**
   * Combine all the matchers using AND
   * @param name Attribute name
   * @param value Attribute example value
   * @param rules Matching rules to apply
   */
  public PactDslJsonBody and(String name, Object value, MatchingRule... rules) {
    body.add(name, Json.toJson(value));
    getMatchers().setRules(matcherKey(name, getRootPath()), new MatchingRuleGroup(Arrays.asList(rules), RuleLogic.AND));
    return this;
  }

  /**
   * Combine all the matchers using OR
   * @param name Attribute name
   * @param value Attribute example value
   * @param rules Matching rules to apply
   */
  public PactDslJsonBody or(String name, Object value, MatchingRule... rules) {
    body.add(name, Json.toJson(value));
    getMatchers().setRules(matcherKey(name, getRootPath()), new MatchingRuleGroup(Arrays.asList(rules), RuleLogic.OR));
    return this;
  }

  /**
   * Matches a URL that is composed of a base path and a sequence of path expressions
   * @param name Attribute name
   * @param basePath The base path for the URL (like "http://localhost:8080/") which will be excluded from the matching
   * @param pathFragments Series of path fragments to match on. These can be strings or regular expressions.
   */
  @Override
  public PactDslJsonBody matchUrl(String name, String basePath, Object... pathFragments) {
    UrlMatcherSupport urlMatcher = new UrlMatcherSupport(basePath, Arrays.asList(pathFragments));
    String exampleValue = urlMatcher.getExampleValue();
    body.add(name, new JsonValue.StringValue(exampleValue.toCharArray()));
    String regexExpression = urlMatcher.getRegexExpression();
    getMatchers().addRule(matcherKey(name, getRootPath()), regexp(regexExpression));
    if (StringUtils.isEmpty(basePath)) {
      getGenerators().addGenerator(Category.BODY, matcherKey(name, getRootPath()),
        new MockServerURLGenerator(exampleValue, regexExpression));
    }
    return this;
  }

  @Override
  public DslPart matchUrl(String basePath, Object... pathFragments) {
    throw new UnsupportedOperationException(
      "URL matcher without an attribute name is not supported for objects. " +
        "Use matchUrl(String name, String basePath, Object... pathFragments)");
  }

  /**
   * Matches a URL that is composed of a base path and a sequence of path expressions. Base path from the mock server
   * will be used.
   * @param name Attribute name
   * @param pathFragments Series of path fragments to match on. These can be strings or regular expressions.
   */
  @Override
  public PactDslJsonBody matchUrl2(String name, Object... pathFragments) {
    return matchUrl(name, null, pathFragments);
  }

  @Override
  public DslPart matchUrl2(Object... pathFragments) {
    throw new UnsupportedOperationException(
      "URL matcher without an attribute name is not supported for objects. " +
        "Use matchUrl2(Object... pathFragments)");
  }

  @Override
  public PactDslJsonBody minMaxArrayLike(String name, int minSize, int maxSize) {
    return minMaxArrayLike(name, minSize, maxSize, minSize);
  }

  @Override
  public PactDslJsonBody minMaxArrayLike(String name, int minSize, int maxSize, DslPart object) {
    validateMinAndMaxAndExamples(minSize, maxSize, minSize);
    String base = matcherKey(name, getRootPath());
    getMatchers().addRule(base, matchMinMax(minSize, maxSize));
    PactDslJsonArray parent = new PactDslJsonArray(matcherKey(name, getRootPath()), "", this, true);

    if (object instanceof PactDslJsonBody) {
      parent.putObjectPrivate(object);
    } else if (object instanceof PactDslJsonArray) {
      parent.putArrayPrivate(object);
    }

    return (PactDslJsonBody) parent.closeArray();
  }

  @Override
  public PactDslJsonBody minMaxArrayLike(int minSize, int maxSize) {
    throw new UnsupportedOperationException("use the minMaxArrayLike(String name, Integer minSize, Integer maxSize) form");
  }

  @Override
  public PactDslJsonArray minMaxArrayLike(int minSize, int maxSize, DslPart object) {
    throw new UnsupportedOperationException("use the minMaxArrayLike(String name, Integer minSize, Integer maxSize, DslPart object) form");
  }

  @Override
  public PactDslJsonBody minMaxArrayLike(String name, int minSize, int maxSize, int numberExamples) {
    validateMinAndMaxAndExamples(minSize, maxSize, numberExamples);
    getMatchers().addRule(matcherKey(name, getRootPath()), matchMinMax(minSize, maxSize));
    PactDslJsonArray parent = new PactDslJsonArray(matcherKey(name, getRootPath()), "", this, true);
    parent.setNumberExamples(numberExamples);
    return new PactDslJsonBody(".", "", parent);
  }

  private void validateMinAndMaxAndExamples(Integer minSize, Integer maxSize, int numberExamples) {
    if (minSize > maxSize) {
      throw new IllegalArgumentException(String.format("The minimum size %d is more than the maximum size of %d",
        minSize, maxSize));
    } else if (numberExamples < minSize) {
      throw new IllegalArgumentException(String.format("Number of example %d is less than the minimum size of %d",
        numberExamples, minSize));
    } else if (numberExamples > maxSize) {
      throw new IllegalArgumentException(String.format("Number of example %d is more than the maximum size of %d",
        numberExamples, maxSize));
    }
  }

  @Override
  public PactDslJsonBody minMaxArrayLike(int minSize, int maxSize, int numberExamples) {
    throw new UnsupportedOperationException("use the minMaxArrayLike(String name, Integer minSize, Integer maxSize, int numberExamples) form");
  }

  @Override
  public PactDslJsonArray eachArrayWithMinMaxLike(String name, int minSize, int maxSize) {
    return eachArrayWithMinMaxLike(name, minSize, minSize, maxSize);
  }

  @Override
  public PactDslJsonArray eachArrayWithMinMaxLike(int minSize, int maxSize) {
    throw new UnsupportedOperationException("use the eachArrayWithMinMaxLike(String name, Integer minSize, Integer maxSize) form");
  }

  @Override
  public PactDslJsonArray eachArrayWithMinMaxLike(String name, int numberExamples, int minSize, int maxSize) {
    validateMinAndMaxAndExamples(minSize, maxSize, numberExamples);
    getMatchers().addRule(matcherKey(name, getRootPath()), matchMinMax(minSize, maxSize));
    PactDslJsonArray parent = new PactDslJsonArray(matcherKey(name, getRootPath()), name, this, true);
    parent.setNumberExamples(numberExamples);
    return new PactDslJsonArray("", "", parent);
  }

  @Override
  public PactDslJsonArray eachArrayWithMinMaxLike(int numberExamples, int minSize, int maxSize) {
    throw new UnsupportedOperationException("use the eachArrayWithMinMaxLike(String name, int numberExamples, Integer minSize, Integer maxSize) form");
  }

  /**
   * Attribute that is an array of values with a minimum and maximum size that are not objects where each item must
   * match the following example
   * @param name field name
   * @param minSize minimum size
   * @param maxSize maximum size
   * @param value Value to use to match each item
   * @param numberExamples number of examples to generate
   */
  public PactDslJsonBody minMaxArrayLike(String name, int minSize, int maxSize, PactDslJsonRootValue value,
                                         int numberExamples) {
    validateMinAndMaxAndExamples(minSize, maxSize, numberExamples);
    getMatchers().addRule(matcherKey(name, getRootPath()), matchMinMax(minSize, maxSize));
    PactDslJsonArray parent = new PactDslJsonArray(matcherKey(name, getRootPath()), "", this, true);
    parent.setNumberExamples(numberExamples);
    parent.putObjectPrivate(value);
    return (PactDslJsonBody) parent.closeArray();
  }

  /**
   * Adds an attribute that will have it's value injected from the provider state
   * @param name Attribute name
   * @param expression Expression to be evaluated from the provider state
   * @param example Example value to be used in the consumer test
   */
  public PactDslJsonBody valueFromProviderState(String name, String expression, Object example) {
    getGenerators().addGenerator(Category.BODY, matcherKey(name, getRootPath()), new ProviderStateGenerator(expression, DataType.from(example)));
    body.add(name, Json.toJson(example));
    getMatchers().addRule(matcherKey(name, getRootPath()), TypeMatcher.INSTANCE);
    return this;
  }

  /**
   * Adds a date attribute formatted as an ISO date with the value generated by the date expression
   * @param name Attribute name
   * @param expression Date expression to use to generate the values
   */
  public PactDslJsonBody dateExpression(String name, String expression) {
    return dateExpression(name, expression, DateFormatUtils.ISO_DATE_FORMAT.getPattern());
  }

  /**
   * Adds a date attribute with the value generated by the date expression
   * @param name Attribute name
   * @param expression Date expression to use to generate the values
   * @param format Date format to use
   */
  public PactDslJsonBody dateExpression(String name, String expression, String format) {
    getGenerators().addGenerator(Category.BODY, matcherKey(name, getRootPath()), new DateGenerator(format, expression));
    FastDateFormat instance = FastDateFormat.getInstance(format);
    body.add(name, new JsonValue.StringValue(instance.format(new Date(DATE_2000)).toCharArray()));
    getMatchers().addRule(matcherKey(name, getRootPath()), matchDate(format));
    return this;
  }

  /**
   * Adds a time attribute formatted as an ISO time with the value generated by the time expression
   * @param name Attribute name
   * @param expression Time expression to use to generate the values
   */
  public PactDslJsonBody timeExpression(String name, String expression) {
    return timeExpression(name, expression, DateFormatUtils.ISO_TIME_NO_T_FORMAT.getPattern());
  }

  /**
   * Adds a time attribute with the value generated by the time expression
   * @param name Attribute name
   * @param expression Time expression to use to generate the values
   * @param format Time format to use
   */
  public PactDslJsonBody timeExpression(String name, String expression, String format) {
    getGenerators().addGenerator(Category.BODY, matcherKey(name, getRootPath()), new TimeGenerator(format, expression));
    FastDateFormat instance = FastDateFormat.getInstance(format);
    body.add(name, new JsonValue.StringValue(instance.format(new Date(DATE_2000)).toCharArray()));
    getMatchers().addRule(matcherKey(name, getRootPath()), matchTime(format));
    return this;
  }

  /**
   * Adds a datetime attribute formatted as an ISO datetime with the value generated by the expression
   * @param name Attribute name
   * @param expression Datetime expression to use to generate the values
   */
  public PactDslJsonBody datetimeExpression(String name, String expression) {
    return datetimeExpression(name, expression, DateFormatUtils.ISO_DATETIME_FORMAT.getPattern());
  }

  /**
   * Adds a datetime attribute with the value generated by the expression
   * @param name Attribute name
   * @param expression Datetime expression to use to generate the values
   * @param format Datetime format to use
   */
  public PactDslJsonBody datetimeExpression(String name, String expression, String format) {
    getGenerators().addGenerator(Category.BODY, matcherKey(name, getRootPath()), new DateTimeGenerator(format, expression));
    FastDateFormat instance = FastDateFormat.getInstance(format);
    body.add(name, new JsonValue.StringValue(instance.format(new Date(DATE_2000)).toCharArray()));
    getMatchers().addRule(matcherKey(name, getRootPath()), matchTimestamp(format));
    return this;
  }

  @Override
  public DslPart arrayContaining(String name) {
    return new PactDslJsonArrayContaining(getRootPath(), name, this);
  }
}
