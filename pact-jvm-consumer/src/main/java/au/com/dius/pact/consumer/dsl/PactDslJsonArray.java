package au.com.dius.pact.consumer.dsl;

import au.com.dius.pact.consumer.InvalidMatcherException;
import nl.flotsam.xeger.Xeger;
import org.apache.commons.lang3.RandomStringUtils;
import org.apache.commons.lang3.time.DateFormatUtils;
import org.apache.commons.lang3.time.FastDateFormat;
import org.json.JSONArray;
import org.json.JSONObject;

import java.math.BigDecimal;
import java.util.Date;
import java.util.UUID;

/**
 * DSL to define a JSON array
 */
public class PactDslJsonArray extends DslPart {

    private static final String EXAMPLE = "Example \"";
    private final JSONArray body;
    private boolean wildCard;
    private int numberExamples = 1;

    public PactDslJsonArray() {
      this("", null, false);
    }
	
    public PactDslJsonArray(String root, DslPart parent) {
        this(root, parent, false);
    }

    public PactDslJsonArray(String root, DslPart parent, boolean wildCard) {
        super(parent, root);
        this.wildCard = wildCard;
        body = new JSONArray();
    }

    /**
     * Closes the current array
     */
    public DslPart closeArray() {
      if (parent != null) {
        parent.putArray(this);
      }
      closed = true;
      return parent;
    }

    @Override
    @Deprecated
    public PactDslJsonBody arrayLike(String name) {
        throw new UnsupportedOperationException("use the eachLike() form");
    }

    /**
     * Element that is an array where each item must match the following example
     * @deprecated use eachLike
     */
    @Override
    @Deprecated
    public PactDslJsonBody arrayLike() {
        return eachLike();
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
      matchers.put(root + appendArrayIndex(1), matchMin(0));
      PactDslJsonArray parent = new PactDslJsonArray(root, this, true);
      parent.setNumberExamples(numberExamples);
      return new PactDslJsonBody(".", parent);
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
        return minArrayLike(size, 1);
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
      matchers.put(root + appendArrayIndex(1), matchMin(size));
      PactDslJsonArray parent = new PactDslJsonArray("", this, true);
      parent.setNumberExamples(numberExamples);
      return new PactDslJsonBody(".", parent);
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
      matchers.put(root + appendArrayIndex(1), matchMax(size));
      PactDslJsonArray parent = new PactDslJsonArray("", this, true);
      parent.setNumberExamples(numberExamples);
      return new PactDslJsonBody(".", parent);
    }

    protected void putObject(DslPart object) {
      for(String matcherName: object.matchers.keySet()) {
          matchers.put(root + appendArrayIndex(1) + matcherName, object.matchers.get(matcherName));
      }
      for (int i = 0; i < getNumberExamples(); i++) {
        body.put(object.getBody());
      }
    }

    protected void putArray(DslPart object) {
        for(String matcherName: object.matchers.keySet()) {
            matchers.put(root + appendArrayIndex(1) + matcherName, object.matchers.get(matcherName));
        }
        body.put(object.getBody());
    }

    @Override
    public Object getBody() {
        return body;
    }

    /**
     * Element that must be the specified value
     * @param value string value
     */
    public PactDslJsonArray stringValue(String value) {
      if (value == null) {
        body.put(JSONObject.NULL);
      } else {
        body.put(value);
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
        body.put(value);
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
        body.put(value);
        return this;
    }

    /**
     * Element that can be any string
     */
    public PactDslJsonArray stringType() {
        body.put(RandomStringUtils.randomAlphabetic(20));
        matchers.put(root + appendArrayIndex(0), matchType());
        return this;
    }

    /**
     * Element that can be any string
     * @param example example value to use for generated bodies
     */
    public PactDslJsonArray stringType(String example) {
        body.put(example);
        matchers.put(root + appendArrayIndex(0), matchType());
        return this;
    }

    /**
     * Element that can be any number
     */
    public PactDslJsonArray numberType() {
        return numberType(Long.parseLong(RandomStringUtils.randomNumeric(9)));
    }

    /**
     * Element that can be any number
     * @param number example number to use for generated bodies
     */
    public PactDslJsonArray numberType(Number number) {
        body.put(number);
        matchers.put(root + appendArrayIndex(0), matchType("type"));
        return this;
    }

    /**
     * Element that must be an integer
     */
    public PactDslJsonArray integerType() {
        return integerType(Long.parseLong(RandomStringUtils.randomNumeric(9)));
    }

    /**
     * Element that must be an integer
     * @param number example integer value to use for generated bodies
     */
    public PactDslJsonArray integerType(Long number) {
        body.put(number);
        matchers.put(root + appendArrayIndex(0), matchType("integer"));
        return this;
    }

    /**
     * Element that must be a real value
     * @deprecated Use decimalType instead
     */
    @Deprecated
    public PactDslJsonArray realType() {
        return decimalType();
    }

    /**
     * Element that must be a real value
     * @param number example real value
     * @deprecated Use decimalType instead
     */
    @Deprecated
    public PactDslJsonArray realType(Double number) {
      return decimalType(number);
    }

  /**
   * Element that must be a decimal value
   */
  public PactDslJsonArray decimalType() {
      return decimalType(new BigDecimal(RandomStringUtils.randomNumeric(10)));
  }

  /**
   * Element that must be a decimalType value
   * @param number example decimalType value
   */
  public PactDslJsonArray decimalType(BigDecimal number) {
      body.put(number);
      matchers.put(root + appendArrayIndex(0), matchType("decimal"));
      return this;
  }

  /**
   * Attribute that must be a decimalType value
   * @param number example decimalType value
   */
  public PactDslJsonArray decimalType(Double number) {
      body.put(number);
      matchers.put(root + appendArrayIndex(0), matchType("decimal"));
      return this;
  }

    /**
     * Element that must be a boolean
     */
    public PactDslJsonArray booleanType() {
        body.put(true);
        matchers.put(root + appendArrayIndex(0), matchType());
        return this;
    }

    /**
     * Element that must be a boolean
     * @param example example boolean to use for generated bodies
     */
    public PactDslJsonArray booleanType(Boolean example) {
        body.put(example);
        matchers.put(root + appendArrayIndex(0), matchType());
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
        body.put(value);
        matchers.put(root + appendArrayIndex(0), regexp(regex));
        return this;
    }

    /**
     * Element that must match the regular expression
     * @param regex regular expression
     */
    public PactDslJsonArray stringMatcher(String regex) {
        stringMatcher(regex, new Xeger(regex).generate());
        return this;
    }

    /**
     * Element that must be an ISO formatted timestamp
     */
    public PactDslJsonArray timestamp() {
        body.put(DateFormatUtils.ISO_DATETIME_FORMAT.format(new Date()));
        matchers.put(root + appendArrayIndex(0), matchTimestamp(DateFormatUtils.ISO_DATETIME_FORMAT.getPattern()));
        return this;
    }

    /**
     * Element that must match the given timestamp format
     * @param format timestamp format
     */
    public PactDslJsonArray timestamp(String format) {
        FastDateFormat instance = FastDateFormat.getInstance(format);
        body.put(instance.format(new Date()));
        matchers.put(root + appendArrayIndex(0), matchTimestamp(format));
        return this;
    }

    /**
     * Element that must match the given timestamp format
     * @param format timestamp format
     * @param example example date and time to use for generated bodies
     */
    public PactDslJsonArray timestamp(String format, Date example) {
        FastDateFormat instance = FastDateFormat.getInstance(format);
        body.put(instance.format(example));
        matchers.put(root + appendArrayIndex(0), matchTimestamp(format));
        return this;
    }

    /**
     * Element that must be formatted as an ISO date
     */
    public PactDslJsonArray date() {
        body.put(DateFormatUtils.ISO_DATE_FORMAT.format(new Date()));
        matchers.put(root + appendArrayIndex(0), matchDate(DateFormatUtils.ISO_DATE_FORMAT.getPattern()));
        return this;
    }

    /**
     * Element that must match the provided date format
     * @param format date format to match
     */
    public PactDslJsonArray date(String format) {
        FastDateFormat instance = FastDateFormat.getInstance(format);
        body.put(instance.format(new Date()));
        matchers.put(root + appendArrayIndex(0), matchDate(format));
        return this;
    }

    /**
     * Element that must match the provided date format
     * @param format date format to match
     * @param example example date to use for generated values
     */
    public PactDslJsonArray date(String format, Date example) {
        FastDateFormat instance = FastDateFormat.getInstance(format);
        body.put(instance.format(example));
        matchers.put(root + appendArrayIndex(0), matchDate(format));
        return this;
    }

    /**
     * Element that must be an ISO formatted time
     */
    public PactDslJsonArray time() {
        body.put(DateFormatUtils.ISO_TIME_FORMAT.format(new Date()));
        matchers.put(root + appendArrayIndex(0), matchTime(DateFormatUtils.ISO_TIME_FORMAT.getPattern()));
        return this;
    }

    /**
     * Element that must match the given time format
     * @param format time format to match
     */
    public PactDslJsonArray time(String format) {
        FastDateFormat instance = FastDateFormat.getInstance(format);
        body.put(instance.format(new Date()));
        matchers.put(root + appendArrayIndex(0), matchTime(format));
        return this;
    }

    /**
     * Element that must match the given time format
     * @param format time format to match
     * @param example example time to use for generated bodies
     */
    public PactDslJsonArray time(String format, Date example) {
        FastDateFormat instance = FastDateFormat.getInstance(format);
        body.put(instance.format(example));
        matchers.put(root + appendArrayIndex(0), matchTime(format));
        return this;
    }

    /**
     * Element that must be an IP4 address
     */
    public PactDslJsonArray ipAddress() {
        body.put("127.0.0.1");
        matchers.put(root + appendArrayIndex(0), regexp("(\\d{1,3}\\.)+\\d{1,3}"));
        return this;
    }

    public PactDslJsonBody object(String name) {
        throw new UnsupportedOperationException("use the object() form");
    }

    /**
     * Element that is a JSON object
     */
    public PactDslJsonBody object() {
        return new PactDslJsonBody(".", this);
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

  public PactDslJsonArray array(String name) {
        throw new UnsupportedOperationException("use the array() form");
    }

    /**
     * Element that is a JSON array
     */
    public PactDslJsonArray array() {
        return new PactDslJsonArray("", this);
    }

    /**
     * Element that must be a numeric identifier
     */
    public PactDslJsonArray id() {
        body.put(Long.parseLong(RandomStringUtils.randomNumeric(10)));
        matchers.put(root + appendArrayIndex(0), matchType());
        return this;
    }

    /**
     * Element that must be a numeric identifier
     * @param id example id to use for generated bodies
     */
    public PactDslJsonArray id(Long id) {
        body.put(id);
        matchers.put(root + appendArrayIndex(0), matchType());
        return this;
    }

    /**
     * Element that must be encoded as a hexadecimal value
     */
    public PactDslJsonArray hexValue() {
        return hexValue(RandomStringUtils.random(10, "0123456789abcdef"));
    }

    /**
     * Element that must be encoded as a hexadecimal value
     * @param hexValue example value to use for generated bodies
     */
    public PactDslJsonArray hexValue(String hexValue) {
        if (!hexValue.matches(HEXADECIMAL)) {
            throw new InvalidMatcherException(EXAMPLE + hexValue + "\" is not a hexadecimal value");
        }
        body.put(hexValue);
        matchers.put(root + appendArrayIndex(0), regexp("[0-9a-fA-F]+"));
        return this;
    }

    /**
     * Element that must be encoded as a GUID
     * @deprecated use uuid instead
     */
    @Deprecated
    public PactDslJsonArray guid() {
        return uuid();
    }

    /**
     * Element that must be encoded as a GUID
     * @param uuid example UUID to use for generated bodies
     * @deprecated use uuid instead
     */
    @Deprecated
    public PactDslJsonArray guid(String uuid) {
        return uuid(uuid);
    }

    /**
     * Element that must be encoded as an UUID
     */
    public PactDslJsonArray uuid() {
        return uuid(UUID.randomUUID().toString());
    }

    /**
     * Element that must be encoded as an UUID
     * @param uuid example UUID to use for generated bodies
     */
    public PactDslJsonArray uuid(String uuid) {
        if (!uuid.matches(UUID_REGEX)) {
            throw new InvalidMatcherException(EXAMPLE + uuid + "\" is not an UUID");
        }
        body.put(uuid);
        matchers.put(root + appendArrayIndex(0), regexp(UUID_REGEX));
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
            index = String.valueOf(body.length() - 1 + offset);
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
    PactDslJsonArray parent = new PactDslJsonArray("", null, true);
    parent.setNumberExamples(numberExamples);
    parent.matchers.put("", parent.matchMin(0));
    return new PactDslJsonBody(".", parent);
  }

  /**
   * Array with a minimum size where each item must match the following example
   * @param minSize minimum size
   */
  public static PactDslJsonBody arrayMinLike(int minSize) {
      return arrayMinLike(minSize, 1);
  }

  /**
   * Array with a minimum size where each item must match the following example
   * @param minSize minimum size
   * @param numberExamples Number of examples to generate
   */
  public static PactDslJsonBody arrayMinLike(int minSize, int numberExamples) {
    PactDslJsonArray parent = new PactDslJsonArray("", null, true);
    parent.setNumberExamples(numberExamples);
    parent.matchers.put("", parent.matchMin(minSize));
    return new PactDslJsonBody(".", parent);
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
    PactDslJsonArray parent = new PactDslJsonArray("", null, true);
    parent.setNumberExamples(numberExamples);
    parent.matchers.put("", parent.matchMax(maxSize));
    return new PactDslJsonBody(".", parent);
  }

  /**
   * Adds a null value to the list
   */
  public PactDslJsonArray nullValue() {
    body.put(JSONObject.NULL);
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
}
