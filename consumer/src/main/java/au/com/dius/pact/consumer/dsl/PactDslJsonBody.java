package au.com.dius.pact.consumer.dsl;

import au.com.dius.pact.consumer.InvalidMatcherException;
import au.com.dius.pact.core.matchers.UrlMatcherSupport;
import au.com.dius.pact.core.model.Feature;
import au.com.dius.pact.core.model.FeatureToggles;
import au.com.dius.pact.core.model.generators.Category;
import au.com.dius.pact.core.model.generators.DateGenerator;
import au.com.dius.pact.core.model.generators.DateTimeGenerator;
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
import au.com.dius.pact.core.support.expressions.DataType;
import com.mifmif.common.regex.Generex;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.time.DateFormatUtils;
import org.apache.commons.lang3.time.FastDateFormat;
import org.json.JSONObject;

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
    private final JSONObject body;

  /**
   * Constructs a new body as a root
   */
  public PactDslJsonBody() {
      super(".", "");
      body = new JSONObject();
  }

  /**
   * Constructs a new body as a child
   * @param rootPath Path to prefix to this child
   * @param rootName Name to associate this object as in the parent
   * @param parent Parent to attach to
   */
  public PactDslJsonBody(String rootPath, String rootName, DslPart parent) {
      super(parent, rootPath, rootName);
      body = new JSONObject();
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
    this.matchers = body.matchers.copyWithUpdatedMatcherRootPrefix(rootPath);
    this.generators = body.generators.copyWithUpdatedMatcherRootPrefix(rootPath);
  }

    public String toString() {
        return body.toString();
    }

    protected void putObject(DslPart object) {
        for (String matcherName: object.matchers.getMatchingRules().keySet()) {
            matchers.setRules(matcherName, object.matchers.getMatchingRules().get(matcherName));
        }
        generators.addGenerators(object.generators);
        String elementBase = StringUtils.difference(this.rootPath, object.rootPath);
        if (StringUtils.isNotEmpty(object.rootName)) {
          body.put(object.rootName, object.getBody());
        } else {
          String name = StringUtils.strip(elementBase, ".");
          Pattern p = Pattern.compile("\\['(.+)'\\]");
          Matcher matcher = p.matcher(name);
          if (matcher.matches()) {
            body.put(matcher.group(1), object.getBody());
          } else {
            body.put(name, object.getBody());
          }
        }
    }

    protected void putArray(DslPart object) {
        for(String matcherName: object.matchers.getMatchingRules().keySet()) {
            matchers.setRules(matcherName, object.matchers.getMatchingRules().get(matcherName));
        }
        generators.addGenerators(object.generators);
        if (StringUtils.isNotEmpty(object.rootName)) {
          body.put(object.rootName, object.getBody());
        } else {
          body.put(StringUtils.difference(this.rootPath, object.rootPath), object.getBody());
        }
    }

    @Override
    public Object getBody() {
        return body;
    }

    /**
     * Attribute that must be the specified value
     * @param name attribute name
     * @param value string value
     */
    public PactDslJsonBody stringValue(String name, String value) {
        if (value == null) {
          body.put(name, JSONObject.NULL);
        } else {
          body.put(name, value);
        }
        return this;
    }

    /**
     * Attribute that must be the specified number
     * @param name attribute name
     * @param value number value
     */
    public PactDslJsonBody numberValue(String name, Number value) {
        body.put(name, value);
        return this;
    }

    /**
     * Attribute that must be the specified boolean
     * @param name attribute name
     * @param value boolean value
     */
    public PactDslJsonBody booleanValue(String name, Boolean value) {
        body.put(name, value);
        return this;
    }

    /**
     * Attribute that can be any string
     * @param name attribute name
     */
    public PactDslJsonBody stringType(String name) {
        generators.addGenerator(Category.BODY, matcherKey(name, rootPath), new RandomStringGenerator(20));
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
        body.put(name, example);
        matchers.addRule(matcherKey(name, rootPath), TypeMatcher.INSTANCE);
        return this;
    }

    /**
     * Attribute that can be any number
     * @param name attribute name
     */
    public PactDslJsonBody numberType(String name) {
        generators.addGenerator(Category.BODY, matcherKey(name, rootPath), new RandomIntGenerator(0, Integer.MAX_VALUE));
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
        body.put(name, number);
        matchers.addRule(matcherKey(name, rootPath), new NumberTypeMatcher(NumberTypeMatcher.NumberType.NUMBER));
        return this;
    }

    /**
     * Attribute that must be an integer
     * @param name attribute name
     */
    public PactDslJsonBody integerType(String name) {
        generators.addGenerator(Category.BODY, matcherKey(name, rootPath), new RandomIntGenerator(0, Integer.MAX_VALUE));
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
        body.put(name, number);
        matchers.addRule(matcherKey(name, rootPath), new NumberTypeMatcher(NumberTypeMatcher.NumberType.INTEGER));
        return this;
    }

    /**
     * Attribute that must be an integer
     * @param name attribute name
     * @param number example integer value to use for generated bodies
     */
    public PactDslJsonBody integerType(String name, Integer number) {
        body.put(name, number);
        matchers.addRule(matcherKey(name, rootPath), new NumberTypeMatcher(NumberTypeMatcher.NumberType.INTEGER));
        return this;
    }

  /**
   * Attribute that must be a decimal value
   * @param name attribute name
   */
  public PactDslJsonBody decimalType(String name) {
      generators.addGenerator(Category.BODY, matcherKey(name, rootPath), new RandomDecimalGenerator(10));
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
      body.put(name, number);
      matchers.addRule(matcherKey(name, rootPath), new NumberTypeMatcher(NumberTypeMatcher.NumberType.DECIMAL));
      return this;
  }

  /**
   * Attribute that must be a decimalType value
   * @param name attribute name
   * @param number example decimalType value
   */
  public PactDslJsonBody decimalType(String name, Double number) {
    body.put(name, number);
    matchers.addRule(matcherKey(name, rootPath), new NumberTypeMatcher(NumberTypeMatcher.NumberType.DECIMAL));
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
        body.put(name, example);
        matchers.addRule(matcherKey(name, rootPath), TypeMatcher.INSTANCE);
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
        body.put(name, value);
        matchers.addRule(matcherKey(name, rootPath), regexp(regex));
        return this;
    }

    /**
     * Attribute that must match the regular expression
     * @param name attribute name
     * @param regex regular expression
     */
    public PactDslJsonBody stringMatcher(String name, String regex) {
      generators.addGenerator(Category.BODY, matcherKey(name, rootPath), new RegexGenerator(regex));
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
    generators.addGenerator(Category.BODY, matcherKey(name, rootPath), new DateTimeGenerator(pattern, null));
    body.put(name, DateFormatUtils.ISO_DATETIME_FORMAT.format(new Date(DATE_2000)));
    matchers.addRule(matcherKey(name, rootPath), matchTimestamp(pattern));
    return this;
  }

  /**
   * Attribute that must match the given datetime format
   * @param name attribute name
   * @param format datetime format
   */
  public PactDslJsonBody datetime(String name, String format) {
    generators.addGenerator(Category.BODY, matcherKey(name, rootPath), new DateTimeGenerator(format, null));
    DateTimeFormatter formatter = DateTimeFormatter.ofPattern(format).withZone(ZoneId.systemDefault());
    body.put(name, formatter.format(new Date(DATE_2000).toInstant()));
    matchers.addRule(matcherKey(name, rootPath), matchTimestamp(format));
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
    body.put(name, formatter.format(example.toInstant()));
    matchers.addRule(matcherKey(name, rootPath), matchTimestamp(format));
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
    body.put(name, formatter.format(example));
    matchers.addRule(matcherKey(name, rootPath), matchTimestamp(format));
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
      generators.addGenerator(Category.BODY, matcherKey(name, rootPath), new DateGenerator(pattern, null));
      body.put(name, DateFormatUtils.ISO_DATE_FORMAT.format(new Date(DATE_2000)));
      matchers.addRule(matcherKey(name, rootPath), matchDate(pattern));
      return this;
    }

    /**
     * Attribute that must match the provided date format
     * @param name attribute date
     * @param format date format to match
     */
    public PactDslJsonBody date(String name, String format) {
      generators.addGenerator(Category.BODY, matcherKey(name, rootPath), new DateGenerator(format, null));
      FastDateFormat instance = FastDateFormat.getInstance(format);
      body.put(name, instance.format(new Date(DATE_2000)));
      matchers.addRule(matcherKey(name, rootPath), matchDate(format));
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
        body.put(name, instance.format(example));
        matchers.addRule(matcherKey(name, rootPath), matchDate(format));
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
      generators.addGenerator(Category.BODY, matcherKey(name, rootPath), new TimeGenerator(pattern, null));
      body.put(name, DateFormatUtils.ISO_TIME_FORMAT.format(new Date(DATE_2000)));
      matchers.addRule(matcherKey(name, rootPath), matchTime(pattern));
      return this;
    }

    /**
     * Attribute that must match the given time format
     * @param name attribute name
     * @param format time format to match
     */
    public PactDslJsonBody time(String name, String format) {
      generators.addGenerator(Category.BODY, matcherKey(name, rootPath), new TimeGenerator(format, null));
      FastDateFormat instance = FastDateFormat.getInstance(format);
      body.put(name, instance.format(new Date(DATE_2000)));
      matchers.addRule(matcherKey(name, rootPath), matchTime(format));
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
        body.put(name, instance.format(example));
        matchers.addRule(matcherKey(name, rootPath), matchTime(format));
        return this;
    }

    /**
     * Attribute that must be an IP4 address
     * @param name attribute name
     */
    public PactDslJsonBody ipAddress(String name) {
        body.put(name, "127.0.0.1");
        matchers.addRule(matcherKey(name, rootPath), regexp("(\\d{1,3}\\.)+\\d{1,3}"));
        return this;
    }

    /**
     * Attribute that is a JSON object
     * @param name field name
     */
    public PactDslJsonBody object(String name) {
      return new PactDslJsonBody(matcherKey(name, rootPath) + ".", "", this);
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
    String base = matcherKey(name, rootPath);
    if (value instanceof PactDslJsonBody) {
      PactDslJsonBody object = new PactDslJsonBody(base, "", this, (PactDslJsonBody) value);
      putObject(object);
    } else if (value instanceof PactDslJsonArray) {
      PactDslJsonArray object = new PactDslJsonArray(base, "", this, (PactDslJsonArray) value);
      putArray(object);
    }
    return this;
  }

  /**
   * Closes the current JSON object
   */
  public DslPart closeObject() {
    if (parent != null) {
      parent.putObject(this);
    } else {
      getMatchers().applyMatcherRootPrefix("$");
      getGenerators().applyRootPrefix("$");
    }
    closed = true;
    return parent;
  }

  @Override
  public DslPart close() {
    DslPart parentToReturn = this;

    if (!closed) {
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
        return new PactDslJsonArray(matcherKey(name, rootPath), name, this);
    }

    public PactDslJsonArray array() {
        throw new UnsupportedOperationException("use the array(String name) form");
    }

  @Override
  public PactDslJsonArray unorderedArray(String name) {
    matchers.addRule(matcherKey(name), EqualsIgnoreOrderMatcher.INSTANCE);
    return this.array(name);
  }

  @Override
  public PactDslJsonArray unorderedArray() {
    throw new UnsupportedOperationException("use the unorderedArray(String name) form");
  }

  @Override
  public PactDslJsonArray unorderedMinArray(String name, int size) {
    matchers.addRule(matcherKey(name), new MinEqualsIgnoreOrderMatcher(size));
    return this.array(name);
  }

  @Override
  public PactDslJsonArray unorderedMinArray(int size) {
    throw new UnsupportedOperationException("use the unorderedMinArray(String name, int size) form");
  }

  @Override
  public PactDslJsonArray unorderedMaxArray(String name, int size) {
    matchers.addRule(matcherKey(name), new MaxEqualsIgnoreOrderMatcher(size));
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
    matchers.addRule(matcherKey(name), new MinMaxEqualsIgnoreOrderMatcher(minSize, maxSize));
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
      if (parent instanceof PactDslJsonArray) {
        closeObject();
        return parent.closeArray();
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
    public PactDslJsonBody eachLike() {
        throw new UnsupportedOperationException("use the eachLike(String name) form");
    }

    /**
     * Attribute that is an array where each item must match the following example
     * @param name field name
     * @param numberExamples number of examples to generate
     */
    @Override
    public PactDslJsonBody eachLike(String name, int numberExamples) {
      matchers.addRule(matcherKey(name, rootPath), matchMin(0));
      PactDslJsonArray parent = new PactDslJsonArray(matcherKey(name, rootPath), "", this, true);
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
      matchers.addRule(matcherKey(name, rootPath), matchMin(0));
      PactDslJsonArray parent = new PactDslJsonArray(matcherKey(name, rootPath), "", this, true);
      parent.setNumberExamples(numberExamples);
      parent.putObject(value);
      return (PactDslJsonBody) parent.closeArray();
    }

    /**
     * Attribute that is an array with a minimum size where each item must match the following example
     * @param name field name
     * @param size minimum size of the array
     */
    @Override
    public PactDslJsonBody minArrayLike(String name, Integer size) {
        return minArrayLike(name, size, size);
    }

    @Override
    public PactDslJsonBody minArrayLike(Integer size) {
        throw new UnsupportedOperationException("use the minArrayLike(String name, Integer size) form");
    }

    /**
     * Attribute that is an array with a minimum size where each item must match the following example
     * @param name field name
     * @param size minimum size of the array
     * @param numberExamples number of examples to generate
     */
    @Override
    public PactDslJsonBody minArrayLike(String name, Integer size, int numberExamples) {
      if (numberExamples < size) {
        throw new IllegalArgumentException(String.format("Number of example %d is less than the minimum size of %d",
          numberExamples, size));
      }
      matchers.addRule(matcherKey(name, rootPath), matchMin(size));
      PactDslJsonArray parent = new PactDslJsonArray(matcherKey(name, rootPath), "", this, true);
      parent.setNumberExamples(numberExamples);
      return new PactDslJsonBody(".", "", parent);
    }

    @Override
    public PactDslJsonBody minArrayLike(Integer size, int numberExamples) {
      throw new UnsupportedOperationException("use the minArrayLike(String name, Integer size, int numberExamples) form");
    }

    /**
     * Attribute that is an array of values with a minimum size that are not objects where each item must match the following example
     * @param name field name
     * @param size minimum size of the array
     * @param value Value to use to match each item
     */
    public PactDslJsonBody minArrayLike(String name, Integer size, PactDslJsonRootValue value) {
      return minArrayLike(name, size, value, 2);
    }

    /**
     * Attribute that is an array of values with a minimum size that are not objects where each item must match the following example
     * @param name field name
     * @param size minimum size of the array
     * @param value Value to use to match each item
     * @param numberExamples number of examples to generate
     */
    public PactDslJsonBody minArrayLike(String name, Integer size, PactDslJsonRootValue value, int numberExamples) {
      if (numberExamples < size) {
        throw new IllegalArgumentException(String.format("Number of example %d is less than the minimum size of %d",
          numberExamples, size));
      }
      matchers.addRule(matcherKey(name, rootPath), matchMin(size));
      PactDslJsonArray parent = new PactDslJsonArray(matcherKey(name, rootPath), "", this, true);
      parent.setNumberExamples(numberExamples);
      parent.putObject(value);
      return (PactDslJsonBody) parent.closeArray();
    }

    /**
     * Attribute that is an array with a maximum size where each item must match the following example
     * @param name field name
     * @param size maximum size of the array
     */
    @Override
    public PactDslJsonBody maxArrayLike(String name, Integer size) {
        return maxArrayLike(name, size, 1);
    }

    @Override
    public PactDslJsonBody maxArrayLike(Integer size) {
        throw new UnsupportedOperationException("use the maxArrayLike(String name, Integer size) form");
    }

    /**
     * Attribute that is an array with a maximum size where each item must match the following example
     * @param name field name
     * @param size maximum size of the array
     * @param numberExamples number of examples to generate
     */
    @Override
    public PactDslJsonBody maxArrayLike(String name, Integer size, int numberExamples) {
      if (numberExamples > size) {
        throw new IllegalArgumentException(String.format("Number of example %d is more than the maximum size of %d",
          numberExamples, size));
      }
      matchers.addRule(matcherKey(name, rootPath), matchMax(size));
      PactDslJsonArray parent = new PactDslJsonArray(matcherKey(name, rootPath), "", this, true);
      parent.setNumberExamples(numberExamples);
      return new PactDslJsonBody(".", "", parent);
    }

    @Override
    public PactDslJsonBody maxArrayLike(Integer size, int numberExamples) {
      throw new UnsupportedOperationException("use the maxArrayLike(String name, Integer size, int numberExamples) form");
    }

    /**
     * Attribute that is an array of values with a maximum size that are not objects where each item must match the following example
     * @param name field name
     * @param size maximum size of the array
     * @param value Value to use to match each item
     */
    public PactDslJsonBody maxArrayLike(String name, Integer size, PactDslJsonRootValue value) {
      return maxArrayLike(name, size, value, 1);
    }

    /**
     * Attribute that is an array of values with a maximum size that are not objects where each item must match the following example
     * @param name field name
     * @param size maximum size of the array
     * @param value Value to use to match each item
     * @param numberExamples number of examples to generate
     */
    public PactDslJsonBody maxArrayLike(String name, Integer size, PactDslJsonRootValue value, int numberExamples) {
      if (numberExamples > size) {
        throw new IllegalArgumentException(String.format("Number of example %d is more than the maximum size of %d",
          numberExamples, size));
      }
      matchers.addRule(matcherKey(name, rootPath), matchMax(size));
      PactDslJsonArray parent = new PactDslJsonArray(matcherKey(name, rootPath), "", this, true);
      parent.setNumberExamples(numberExamples);
      parent.putObject(value);
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
      generators.addGenerator(Category.BODY, matcherKey(name, rootPath), new RandomIntGenerator(0, Integer.MAX_VALUE));
      body.put(name, 1234567890L);
      matchers.addRule(matcherKey(name, rootPath), TypeMatcher.INSTANCE);
      return this;
    }

    /**
     * Attribute that must be a numeric identifier
     * @param name attribute name
     * @param id example id to use for generated bodies
     */
    public PactDslJsonBody id(String name, Long id) {
        body.put(name, id);
        matchers.addRule(matcherKey(name, rootPath), TypeMatcher.INSTANCE);
        return this;
    }

    /**
     * Attribute that must be encoded as a hexadecimal value
     * @param name attribute name
     */
    public PactDslJsonBody hexValue(String name) {
      generators.addGenerator(Category.BODY, matcherKey(name, rootPath), new RandomHexadecimalGenerator(10));
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
        body.put(name, hexValue);
        matchers.addRule(matcherKey(name, rootPath), regexp("[0-9a-fA-F]+"));
        return this;
    }

    /**
     * Attribute that must be encoded as an UUID
     * @param name attribute name
     */
    public PactDslJsonBody uuid(String name) {
      generators.addGenerator(Category.BODY, matcherKey(name, rootPath), UuidGenerator.INSTANCE);
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
        body.put(name, uuid);
        matchers.addRule(matcherKey(name, rootPath), regexp(UUID_REGEX));
        return this;
    }

    /**
     * Sets the field to a null value
     * @param fieldName field name
     */
    public PactDslJsonBody nullValue(String fieldName) {
        body.put(fieldName, JSONObject.NULL);
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
    matchers.addRule(matcherKey(name, rootPath), matchMin(0));
    PactDslJsonArray parent = new PactDslJsonArray(matcherKey(name, rootPath), name, this, true);
    parent.setNumberExamples(numberExamples);
    return new PactDslJsonArray("", "", parent);
  }

  @Override
  public PactDslJsonArray eachArrayLike(int numberExamples) {
    throw new UnsupportedOperationException("use the eachArrayLike(String name, int numberExamples) form");
  }

  @Override
  public PactDslJsonArray eachArrayWithMaxLike(String name, Integer size) {
    return eachArrayWithMaxLike(name, 1, size);
  }

  @Override
  public PactDslJsonArray eachArrayWithMaxLike(Integer size) {
    throw new UnsupportedOperationException("use the eachArrayWithMaxLike(String name, Integer size) form");
  }

  @Override
  public PactDslJsonArray eachArrayWithMaxLike(String name, int numberExamples, Integer size) {
    if (numberExamples > size) {
      throw new IllegalArgumentException(String.format("Number of example %d is more than the maximum size of %d",
        numberExamples, size));
    }
    matchers.addRule(matcherKey(name, rootPath), matchMax(size));
    PactDslJsonArray parent = new PactDslJsonArray(matcherKey(name, rootPath), name, this, true);
    parent.setNumberExamples(numberExamples);
    return new PactDslJsonArray("", "", parent);
  }

  @Override
  public PactDslJsonArray eachArrayWithMaxLike(int numberExamples, Integer size) {
    throw new UnsupportedOperationException("use the eachArrayWithMaxLike(String name, int numberExamples, Integer size) form");
  }

  @Override
  public PactDslJsonArray eachArrayWithMinLike(String name, Integer size) {
    return eachArrayWithMinLike(name, size, size);
  }

  @Override
  public PactDslJsonArray eachArrayWithMinLike(Integer size) {
    throw new UnsupportedOperationException("use the eachArrayWithMinLike(String name, Integer size) form");
  }

  @Override
  public PactDslJsonArray eachArrayWithMinLike(String name, int numberExamples, Integer size) {
    if (numberExamples < size) {
      throw new IllegalArgumentException(String.format("Number of example %d is less than the minimum size of %d",
        numberExamples, size));
    }
    matchers.addRule(matcherKey(name, rootPath), matchMin(size));
    PactDslJsonArray parent = new PactDslJsonArray(matcherKey(name, rootPath), name, this, true);
    parent.setNumberExamples(numberExamples);
    return new PactDslJsonArray("", "", parent);
  }

  @Override
  public PactDslJsonArray eachArrayWithMinLike(int numberExamples, Integer size) {
    throw new UnsupportedOperationException("use the eachArrayWithMinLike(String name, int numberExamples, Integer size) form");
  }

  /**
   * Accepts any key, and each key is mapped to a list of items that must match the following object definition
   * Note: this needs the Java system property "pact.matching.wildcard" set to value "true" when the pact file is verified.
   * @param exampleKey Example key to use for generating bodies
   */
  public PactDslJsonBody eachKeyMappedToAnArrayLike(String exampleKey) {
    if (FeatureToggles.isFeatureSet(Feature.UseMatchValuesMatcher)) {
      matchers.addRule(rootPath.endsWith(".") ? rootPath.substring(0, rootPath.length() - 1) : rootPath, ValuesMatcher.INSTANCE);
    } else {
      matchers.addRule(rootPath + "*", matchMin(0));
    }
    PactDslJsonArray parent = new PactDslJsonArray(rootPath + "*", exampleKey, this, true);
    return new PactDslJsonBody(".", "", parent);
  }

  /**
   * Accepts any key, and each key is mapped to a map that must match the following object definition
   * Note: this needs the Java system property "pact.matching.wildcard" set to value "true" when the pact file is verified.
   * @param exampleKey Example key to use for generating bodies
   */
  public PactDslJsonBody eachKeyLike(String exampleKey) {
    if (FeatureToggles.isFeatureSet(Feature.UseMatchValuesMatcher)) {
      matchers.addRule(rootPath.endsWith(".") ? rootPath.substring(0, rootPath.length() - 1) : rootPath, ValuesMatcher.INSTANCE);
    } else {
      matchers.addRule(rootPath + "*", TypeMatcher.INSTANCE);
    }
    return new PactDslJsonBody(rootPath + "*.", exampleKey, this);
  }

  /**
   * Accepts any key, and each key is mapped to a map that must match the provided object definition
   * Note: this needs the Java system property "pact.matching.wildcard" set to value "true" when the pact file is verified.
   * @param exampleKey Example key to use for generating bodies
   * @param value Value to use for matching and generated bodies
   */
  public PactDslJsonBody eachKeyLike(String exampleKey, PactDslJsonRootValue value) {
    body.put(exampleKey, value.getBody());
    if (FeatureToggles.isFeatureSet(Feature.UseMatchValuesMatcher)) {
      matchers.addRule(rootPath.endsWith(".") ? rootPath.substring(0, rootPath.length() - 1) : rootPath, ValuesMatcher.INSTANCE);
    }
    for(String matcherName: value.matchers.getMatchingRules().keySet()) {
      matchers.addRules(rootPath + "*" + matcherName, value.matchers.getMatchingRules().get(matcherName).getRules());
    }
    return this;
  }

  /**
   * Attribute that must include the provided string value
   * @param name attribute name
   * @param value Value that must be included
   */
  public PactDslJsonBody includesStr(String name, String value) {
    body.put(name, value);
    matchers.addRule(matcherKey(name, rootPath), includesMatcher(value));
    return this;
  }

  /**
   * Attribute that must be equal to the provided value.
   * @param name attribute name
   * @param value Value that will be used for comparisons
   */
  public PactDslJsonBody equalTo(String name, Object value) {
    body.put(name, value);
    matchers.addRule(matcherKey(name, rootPath), EqualsMatcher.INSTANCE);
    return this;
  }

  /**
   * Combine all the matchers using AND
   * @param name Attribute name
   * @param value Attribute example value
   * @param rules Matching rules to apply
   */
  public PactDslJsonBody and(String name, Object value, MatchingRule... rules) {
    if (value != null) {
      body.put(name, value);
    } else {
      body.put(name, JSONObject.NULL);
    }
    matchers.setRules(matcherKey(name, rootPath), new MatchingRuleGroup(Arrays.asList(rules), RuleLogic.AND));
    return this;
  }

  /**
   * Combine all the matchers using OR
   * @param name Attribute name
   * @param value Attribute example value
   * @param rules Matching rules to apply
   */
  public PactDslJsonBody or(String name, Object value, MatchingRule... rules) {
    if (value != null) {
      body.put(name, value);
    } else {
      body.put(name, JSONObject.NULL);
    }
    matchers.setRules(matcherKey(name, rootPath), new MatchingRuleGroup(Arrays.asList(rules), RuleLogic.OR));
    return this;
  }

  /**
   * Matches a URL that is composed of a base path and a sequence of path expressions
   * @param name Attribute name
   * @param basePath The base path for the URL (like "http://localhost:8080/") which will be excluded from the matching
   * @param pathFragments Series of path fragments to match on. These can be strings or regular expressions.
   */
  public PactDslJsonBody matchUrl(String name, String basePath, Object... pathFragments) {
    UrlMatcherSupport urlMatcher = new UrlMatcherSupport(basePath, Arrays.asList(pathFragments));
    body.put(name, urlMatcher.getExampleValue());
    matchers.addRule(matcherKey(name, rootPath), regexp(urlMatcher.getRegexExpression()));
    return this;
  }

  @Override
  public PactDslJsonBody minMaxArrayLike(String name, Integer minSize, Integer maxSize) {
    return minMaxArrayLike(name, minSize, maxSize, minSize);
  }

  @Override
  public PactDslJsonBody minMaxArrayLike(Integer minSize, Integer maxSize) {
    throw new UnsupportedOperationException("use the minMaxArrayLike(String name, Integer minSize, Integer maxSize) form");
  }

  @Override
  public PactDslJsonBody minMaxArrayLike(String name, Integer minSize, Integer maxSize, int numberExamples) {
    validateMinAndMaxAndExamples(minSize, maxSize, numberExamples);
    matchers.addRule(matcherKey(name, rootPath), matchMinMax(minSize, maxSize));
    PactDslJsonArray parent = new PactDslJsonArray(matcherKey(name, rootPath), "", this, true);
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
  public PactDslJsonBody minMaxArrayLike(Integer minSize, Integer maxSize, int numberExamples) {
    throw new UnsupportedOperationException("use the minMaxArrayLike(String name, Integer minSize, Integer maxSize, int numberExamples) form");
  }

  @Override
  public PactDslJsonArray eachArrayWithMinMaxLike(String name, Integer minSize, Integer maxSize) {
    return eachArrayWithMinMaxLike(name, minSize, minSize, maxSize);
  }

  @Override
  public PactDslJsonArray eachArrayWithMinMaxLike(Integer minSize, Integer maxSize) {
    throw new UnsupportedOperationException("use the eachArrayWithMinMaxLike(String name, Integer minSize, Integer maxSize) form");
  }

  @Override
  public PactDslJsonArray eachArrayWithMinMaxLike(String name, int numberExamples, Integer minSize, Integer maxSize) {
    validateMinAndMaxAndExamples(minSize, maxSize, numberExamples);
    matchers.addRule(matcherKey(name, rootPath), matchMinMax(minSize, maxSize));
    PactDslJsonArray parent = new PactDslJsonArray(matcherKey(name, rootPath), name, this, true);
    parent.setNumberExamples(numberExamples);
    return new PactDslJsonArray("", "", parent);
  }

  @Override
  public PactDslJsonArray eachArrayWithMinMaxLike(int numberExamples, Integer minSize, Integer maxSize) {
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
  public PactDslJsonBody minMaxArrayLike(String name, Integer minSize, Integer maxSize, PactDslJsonRootValue value,
                                         int numberExamples) {
    validateMinAndMaxAndExamples(minSize, maxSize, numberExamples);
    matchers.addRule(matcherKey(name, rootPath), matchMinMax(minSize, maxSize));
    PactDslJsonArray parent = new PactDslJsonArray(matcherKey(name, rootPath), "", this, true);
    parent.setNumberExamples(numberExamples);
    parent.putObject(value);
    return (PactDslJsonBody) parent.closeArray();
  }

  /**
   * Adds an attribute that will have it's value injected from the provider state
   * @param name Attribute name
   * @param expression Expression to be evaluated from the provider state
   * @param example Example value to be used in the consumer test
   */
  public PactDslJsonBody valueFromProviderState(String name, String expression, Object example) {
    generators.addGenerator(Category.BODY, matcherKey(name, rootPath), new ProviderStateGenerator(expression, DataType.from(example)));
    body.put(name, example);
    matchers.addRule(matcherKey(name, rootPath), TypeMatcher.INSTANCE);
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
    generators.addGenerator(Category.BODY, matcherKey(name, rootPath), new DateGenerator(format, expression));
    FastDateFormat instance = FastDateFormat.getInstance(format);
    body.put(name, instance.format(new Date(DATE_2000)));
    matchers.addRule(matcherKey(name, rootPath), matchDate(format));
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
    generators.addGenerator(Category.BODY, matcherKey(name, rootPath), new TimeGenerator(format, expression));
    FastDateFormat instance = FastDateFormat.getInstance(format);
    body.put(name, instance.format(new Date(DATE_2000)));
    matchers.addRule(matcherKey(name, rootPath), matchTime(format));
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
    generators.addGenerator(Category.BODY, matcherKey(name, rootPath), new DateTimeGenerator(format, expression));
    FastDateFormat instance = FastDateFormat.getInstance(format);
    body.put(name, instance.format(new Date(DATE_2000)));
    matchers.addRule(matcherKey(name, rootPath), matchTimestamp(format));
    return this;
  }
}
