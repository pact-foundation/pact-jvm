package au.com.dius.pact.consumer.dsl;

import au.com.dius.pact.consumer.InvalidMatcherException;
import com.mifmif.common.regex.Generex;
import io.gatling.jsonpath.Parser$;
import org.apache.commons.lang3.RandomStringUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.time.DateFormatUtils;
import org.apache.commons.lang3.time.FastDateFormat;
import org.json.JSONObject;

import java.math.BigDecimal;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * DSL to define a JSON Object
 */
public class PactDslJsonBody extends DslPart {

    private static final String EXAMPLE = "Example \"";
    private final JSONObject body;

    public PactDslJsonBody() {
        super(".");
        body = new JSONObject();
    }

    public PactDslJsonBody(String root, DslPart parent) {
        super(parent, root);
        body = new JSONObject();
    }

    public String toString() {
        return body.toString();
    }

    protected void putObject(DslPart object) {
        for(String matcherName: object.matchers.keySet()) {
            matchers.put(matcherName, object.matchers.get(matcherName));
        }
        String elementBase = StringUtils.difference(this.root, object.root);
        String name = StringUtils.strip(elementBase, ".");
        Pattern p = Pattern.compile("\\['(.+)'\\]");
        Matcher matcher = p.matcher(name);
        if (matcher.matches()) {
            body.put(matcher.group(1), object.getBody());
        } else {
            body.put(name, object.getBody());
        }
    }

    protected void putArray(DslPart object) {
        for(String matcherName: object.matchers.keySet()) {
            matchers.put(matcherName, object.matchers.get(matcherName));
        }
        body.put(StringUtils.difference(this.root, object.root), object.getBody());
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
        return stringType(name, RandomStringUtils.randomAlphabetic(20));
    }

    /**
     * Attribute that can be any string
     * @param name attribute name
     * @param example example value to use for generated bodies
     */
    public PactDslJsonBody stringType(String name, String example) {
        body.put(name, example);
        matchers.put(matcherKey(name), matchType());
        return this;
    }

    private String matcherKey(String name) {
        String key = root + name;
        if (!name.matches(Parser$.MODULE$.FieldRegex().toString())) {
            key = StringUtils.stripEnd(root, ".") + "['" + name + "']";
        }
        return key;
    }

    /**
     * Attribute that can be any number
     * @param name attribute name
     */
    public PactDslJsonBody numberType(String name) {
        return numberType(name, Long.parseLong(RandomStringUtils.randomNumeric(9)));
    }

    /**
     * Attribute that can be any number
     * @param name attribute name
     * @param number example number to use for generated bodies
     */
    public PactDslJsonBody numberType(String name, Number number) {
        body.put(name, number);
        matchers.put(matcherKey(name), matchType());
        return this;
    }

    /**
     * Attribute that must be an integer
     * @param name attribute name
     */
    public PactDslJsonBody integerType(String name) {
        return integerType(name, Long.parseLong(RandomStringUtils.randomNumeric(9)));
    }

    /**
     * Attribute that must be an integer
     * @param name attribute name
     * @param number example integer value to use for generated bodies
     */
    public PactDslJsonBody integerType(String name, Long number) {
        body.put(name, number);
        matchers.put(matcherKey(name), matchType("integer"));
        return this;
    }

    /**
     * Attribute that must be an integer
     * @param name attribute name
     * @param number example integer value to use for generated bodies
     */
    public PactDslJsonBody integerType(String name, Integer number) {
        body.put(name, number);
        matchers.put(matcherKey(name), matchType("integer"));
        return this;
    }

    /**
     * Attribute that must be a real value
     * @param name attribute name
     * @deprecated Use decimal instead
     */
    @Deprecated
    public PactDslJsonBody realType(String name) {
        return realType(name, Double.parseDouble(RandomStringUtils.randomNumeric(10)));
    }

    /**
     * Attribute that must be a real value
     * @param name attribute name
     * @param number example real value
     * @deprecated Use decimal instead
     */
    @Deprecated
    public PactDslJsonBody realType(String name, Double number) {
        body.put(name, number);
        matchers.put(matcherKey(name), matchType("real"));
        return this;
    }

  /**
   * Attribute that must be a decimal value
   * @param name attribute name
   */
  public PactDslJsonBody decimalType(String name) {
      return decimalType(name, new BigDecimal(RandomStringUtils.randomNumeric(10)));
  }

  /**
   * Attribute that must be a decimalType value
   * @param name attribute name
   * @param number example decimalType value
   */
  public PactDslJsonBody decimalType(String name, BigDecimal number) {
      body.put(name, number);
      matchers.put(matcherKey(name), matchType("decimal"));
      return this;
  }

  /**
   * Attribute that must be a decimalType value
   * @param name attribute name
   * @param number example decimalType value
   */
  public PactDslJsonBody decimalType(String name, Double number) {
    body.put(name, number);
    matchers.put(matcherKey(name), matchType("decimal"));
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
     * Attribute that must be a boolean
     * @param name attribute name
     * @param example example boolean to use for generated bodies
     */
    public PactDslJsonBody booleanType(String name, Boolean example) {
        body.put(name, example);
        matchers.put(matcherKey(name), matchType());
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
        matchers.put(matcherKey(name), regexp(regex));
        return this;
    }

    /**
     * Attribute that must match the regular expression
     * @param name attribute name
     * @param regex regular expression
     */
    public PactDslJsonBody stringMatcher(String name, String regex) {
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
     */
    public PactDslJsonBody timestamp(String name) {
        body.put(name, DateFormatUtils.ISO_DATETIME_FORMAT.format(new Date()));
        matchers.put(matcherKey(name), matchTimestamp(DateFormatUtils.ISO_DATETIME_FORMAT.getPattern()));
        return this;
    }

    /**
     * Attribute that must match the given timestamp format
     * @param name attribute name
     * @param format timestamp format
     */
    public PactDslJsonBody timestamp(String name, String format) {
        FastDateFormat instance = FastDateFormat.getInstance(format);
        body.put(name, instance.format(new Date()));
        matchers.put(matcherKey(name), matchTimestamp(format));
        return this;
    }

    /**
     * Attribute that must match the given timestamp format
     * @param name attribute name
     * @param format timestamp format
     * @param example example date and time to use for generated bodies
     */
    public PactDslJsonBody timestamp(String name, String format, Date example) {
        FastDateFormat instance = FastDateFormat.getInstance(format);
        body.put(name, instance.format(example));
        matchers.put(matcherKey(name), matchTimestamp(format));
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
        body.put(name, DateFormatUtils.ISO_DATE_FORMAT.format(new Date()));
        matchers.put(matcherKey(name), matchDate(DateFormatUtils.ISO_DATE_FORMAT.getPattern()));
        return this;
    }

    /**
     * Attribute that must match the provided date format
     * @param name attribute date
     * @param format date format to match
     */
    public PactDslJsonBody date(String name, String format) {
        FastDateFormat instance = FastDateFormat.getInstance(format);
        body.put(name, instance.format(new Date()));
        matchers.put(matcherKey(name), matchDate(format));
        return this;
    }

    /**
     * Attribute that must match the provided date format
     * @param name attribute date
     * @param format date format to match
     * @param example example date to use for generated values
     */
    public PactDslJsonBody date(String name, String format, Date example) {
        FastDateFormat instance = FastDateFormat.getInstance(format);
        body.put(name, instance.format(example));
        matchers.put(matcherKey(name), matchDate(format));
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
        body.put(name, DateFormatUtils.ISO_TIME_FORMAT.format(new Date()));
        matchers.put(matcherKey(name), matchTime(DateFormatUtils.ISO_TIME_FORMAT.getPattern()));
        return this;
    }

    /**
     * Attribute that must match the given time format
     * @param name attribute name
     * @param format time format to match
     */
    public PactDslJsonBody time(String name, String format) {
        FastDateFormat instance = FastDateFormat.getInstance(format);
        body.put(name, instance.format(new Date()));
        matchers.put(matcherKey(name), matchTime(format));
        return this;
    }

    /**
     * Attribute that must match the given time format
     * @param name attribute name
     * @param format time format to match
     * @param example example time to use for generated bodies
     */
    public PactDslJsonBody time(String name, String format, Date example) {
        FastDateFormat instance = FastDateFormat.getInstance(format);
        body.put(name, instance.format(example));
        matchers.put(matcherKey(name), matchTime(format));
        return this;
    }

    /**
     * Attribute that must be an IP4 address
     * @param name attribute name
     */
    public PactDslJsonBody ipAddress(String name) {
        body.put(name, "127.0.0.1");
        matchers.put(matcherKey(name), regexp("(\\d{1,3}\\.)+\\d{1,3}"));
        return this;
    }

    /**
     * Attribute that is a JSON object
     * @param name field name
     */
    public PactDslJsonBody object(String name) {
        String base = root + name;
        if (!name.matches(Parser$.MODULE$.FieldRegex().toString())) {
            base = StringUtils.substringBeforeLast(root, ".") + "['" + name + "']";
        }
        return new PactDslJsonBody(base + ".", this);
    }

    public PactDslJsonBody object() {
        throw new UnsupportedOperationException("use the object(String name) form");
    }

    /**
     * Closes the current JSON object
     */
    public DslPart closeObject() {
      if (parent != null) {
        parent.putObject(this);
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
        return new PactDslJsonArray(matcherKey(name), this);
    }

    public PactDslJsonArray array() {
        throw new UnsupportedOperationException("use the array(String name) form");
    }

    /**
     * Closes the current array
     */
    @Override
    public DslPart closeArray() {
        throw new UnsupportedOperationException("can't call closeArray on an Object");
    }

    /**
     * Attribute that is an array where each item must match the following example
     * @param name field name
     * @deprecated use eachLike
     */
    @Override
    @Deprecated
    public PactDslJsonBody arrayLike(String name) {
        Map<String, Object> matcher = new HashMap<String, Object>();
        matcher.put("match", "type");
        matchers.put(matcherKey(name), matcher);
        return new PactDslJsonBody(".", new PactDslJsonArray(matcherKey(name), this, true));
    }

    @Override
    @Deprecated
    public PactDslJsonBody arrayLike() {
        throw new UnsupportedOperationException("use the arrayLike(String name) form");
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
      matchers.put(matcherKey(name), matchMin(0));
      PactDslJsonArray parent = new PactDslJsonArray(matcherKey(name), this, true);
      parent.setNumberExamples(numberExamples);
      return new PactDslJsonBody(".", parent);
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
      matchers.put(matcherKey(name), matchMin(0));
      PactDslJsonArray parent = new PactDslJsonArray(matcherKey(name), this, true);
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
        return minArrayLike(name, size, 1);
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
      matchers.put(matcherKey(name), matchMin(size));
      PactDslJsonArray parent = new PactDslJsonArray(matcherKey(name), this, true);
      parent.setNumberExamples(numberExamples);
      return new PactDslJsonBody(".", parent);
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
      return minArrayLike(name, size, value, 1);
    }

    /**
     * Attribute that is an array of values with a minimum size that are not objects where each item must match the following example
     * @param name field name
     * @param size minimum size of the array
     * @param value Value to use to match each item
     * @param numberExamples number of examples to generate
     */
    public PactDslJsonBody minArrayLike(String name, Integer size, PactDslJsonRootValue value, int numberExamples) {
      matchers.put(matcherKey(name), matchMin(size));
      PactDslJsonArray parent = new PactDslJsonArray(matcherKey(name), this, true);
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
      matchers.put(matcherKey(name), matchMax(size));
      PactDslJsonArray parent = new PactDslJsonArray(matcherKey(name), this, true);
      parent.setNumberExamples(numberExamples);
      return new PactDslJsonBody(".", parent);
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
      matchers.put(matcherKey(name), matchMax(size));
      PactDslJsonArray parent = new PactDslJsonArray(matcherKey(name), this, true);
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
        body.put(name, Long.parseLong(RandomStringUtils.randomNumeric(10)));
        matchers.put(matcherKey(name), matchType());
        return this;
    }

    /**
     * Attribute that must be a numeric identifier
     * @param name attribute name
     * @param id example id to use for generated bodies
     */
    public PactDslJsonBody id(String name, Long id) {
        body.put(name, id);
        matchers.put(matcherKey(name), matchType());
        return this;
    }

    /**
     * Attribute that must be encoded as a hexadecimal value
     * @param name attribute name
     */
    public PactDslJsonBody hexValue(String name) {
        return hexValue(name, RandomStringUtils.random(10, "0123456789abcdef"));
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
        matchers.put(matcherKey(name), regexp("[0-9a-fA-F]+"));
        return this;
    }

    /**
     * Attribute that must be encoded as a GUID
     * @param name attribute name
     * @deprecated use uuid instead
     */
    @Deprecated
    public PactDslJsonBody guid(String name) {
        return uuid(name);
    }

    /**
     * Attribute that must be encoded as a GUID
     * @param name attribute name
     * @param uuid example UUID to use for generated bodies
     * @deprecated use uuid instead
     */
    @Deprecated
    public PactDslJsonBody guid(String name, UUID uuid) {
        return uuid(name, uuid);
    }

    /**
     * Attribute that must be encoded as a GUID
     * @param name attribute name
     * @param uuid example UUID to use for generated bodies
     * @deprecated use uuid instead
     */
    @Deprecated
    public PactDslJsonBody guid(String name, String uuid) {
        return uuid(name, uuid);
    }

    /**
     * Attribute that must be encoded as an UUID
     * @param name attribute name
     */
    public PactDslJsonBody uuid(String name) {
        return uuid(name, UUID.randomUUID().toString());
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
        matchers.put(matcherKey(name), regexp(UUID_REGEX));
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
    matchers.put(matcherKey(name), matchMin(0));
    PactDslJsonArray parent = new PactDslJsonArray(matcherKey(name), this, true);
    parent.setNumberExamples(numberExamples);
    return new PactDslJsonArray("", parent);
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
    matchers.put(matcherKey(name), matchMax(size));
    PactDslJsonArray parent = new PactDslJsonArray(matcherKey(name), this, true);
    parent.setNumberExamples(numberExamples);
    return new PactDslJsonArray("", parent);
  }

  @Override
  public PactDslJsonArray eachArrayWithMaxLike(int numberExamples, Integer size) {
    throw new UnsupportedOperationException("use the eachArrayWithMaxLike(String name, int numberExamples, Integer size) form");
  }

  @Override
  public PactDslJsonArray eachArrayWithMinLike(String name, Integer size) {
    return eachArrayWithMinLike(name, 1, size);
  }

  @Override
  public PactDslJsonArray eachArrayWithMinLike(Integer size) {
    throw new UnsupportedOperationException("use the eachArrayWithMinLike(String name, Integer size) form");
  }

  @Override
  public PactDslJsonArray eachArrayWithMinLike(String name, int numberExamples, Integer size) {
    matchers.put(matcherKey(name), matchMin(size));
    PactDslJsonArray parent = new PactDslJsonArray(matcherKey(name), this, true);
    parent.setNumberExamples(numberExamples);
    return new PactDslJsonArray("", parent);
  }

  @Override
  public PactDslJsonArray eachArrayWithMinLike(int numberExamples, Integer size) {
    throw new UnsupportedOperationException("use the eachArrayWithMinLike(String name, int numberExamples, Integer size) form");
  }
}
