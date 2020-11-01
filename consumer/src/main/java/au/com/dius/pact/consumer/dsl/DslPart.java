package au.com.dius.pact.consumer.dsl;

import au.com.dius.pact.core.model.generators.Generators;
import au.com.dius.pact.core.model.matchingrules.MatchingRuleCategory;
import au.com.dius.pact.core.model.matchingrules.DateMatcher;
import au.com.dius.pact.core.model.matchingrules.EqualsIgnoreOrderMatcher;
import au.com.dius.pact.core.model.matchingrules.IncludeMatcher;
import au.com.dius.pact.core.model.matchingrules.MaxEqualsIgnoreOrderMatcher;
import au.com.dius.pact.core.model.matchingrules.MaxTypeMatcher;
import au.com.dius.pact.core.model.matchingrules.MinEqualsIgnoreOrderMatcher;
import au.com.dius.pact.core.model.matchingrules.MinMaxEqualsIgnoreOrderMatcher;
import au.com.dius.pact.core.model.matchingrules.MinMaxTypeMatcher;
import au.com.dius.pact.core.model.matchingrules.MinTypeMatcher;
import au.com.dius.pact.core.model.matchingrules.RegexMatcher;
import au.com.dius.pact.core.model.matchingrules.TimeMatcher;
import au.com.dius.pact.core.model.matchingrules.TimestampMatcher;

/**
 * Abstract base class to support Object and Array JSON DSL builders
 */
public abstract class DslPart {
    public static final String HEXADECIMAL = "[0-9a-fA-F]+";
    public static final String IP_ADDRESS = "(\\d{1,3}\\.)+\\d{1,3}";
    public static final String UUID_REGEX = "[0-9a-f]{8}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{12}";
    public static final long DATE_2000 = 949323600000L;

    protected final DslPart parent;
    protected final String rootPath;
    protected final String rootName;
    protected MatchingRuleCategory matchers = new MatchingRuleCategory("body");
    protected Generators generators = new Generators();
    protected boolean closed = false;

    public DslPart(DslPart parent, String rootPath, String rootName) {
        this.parent = parent;
        this.rootPath = rootPath;
        this.rootName = rootName;
    }

    public DslPart(String rootPath, String rootName) {
        this.parent = null;
        this.rootPath = rootPath;
        this.rootName = rootName;
    }

    protected abstract void putObject(DslPart object);
    protected abstract void putArray(DslPart object);
    public abstract Object getBody();

    /**
     * Field which is an array
     * @param name field name
     */
    public abstract PactDslJsonArray array(String name);

    /**
     * Element as an array
     */
    public abstract PactDslJsonArray array();

    /**
     * Array field where order is ignored
     * @param name field name
     */
    public abstract PactDslJsonArray unorderedArray(String name);

    /**
     * Array element where order is ignored
     */
    public abstract PactDslJsonArray unorderedArray();

    /**
     * Array field of min size where order is ignored
     * @param name field name
     * @param size minimum size
     */
    public abstract PactDslJsonArray unorderedMinArray(String name, int size);

    /**
     * Array element of min size where order is ignored
     * @param size minimum size
     */
    public abstract PactDslJsonArray unorderedMinArray(int size);

    /**
     * Array field of max size where order is ignored
     * @param name field name
     * @param size maximum size
     */
    public abstract PactDslJsonArray unorderedMaxArray(String name, int size);

    /**
     * Array element of max size where order is ignored
     * @param size maximum size
     */
    public abstract PactDslJsonArray unorderedMaxArray(int size);

    /**
     * Array field of min and max size where order is ignored
     * @param name field name
     * @param minSize minimum size
     * @param maxSize maximum size
     */
    public abstract PactDslJsonArray unorderedMinMaxArray(String name, int minSize, int maxSize);

    /**
     * Array element of min and max size where order is ignored
     * @param minSize minimum size
     * @param maxSize maximum size
     */
    public abstract PactDslJsonArray unorderedMinMaxArray(int minSize, int maxSize);

    /**
     * Close of the previous array element
     */
    public abstract DslPart closeArray();

    /**
     * Array field where each element must match the following object
     * @param name field name
     */
    public abstract PactDslJsonBody eachLike(String name);

    /**
     * Array element where each element of the array must match the following object
     */
    public abstract PactDslJsonBody eachLike();

    /**
     * Array field where each element must match the following object
     * @param name field name
     * @param numberExamples number of examples to generate
     */
    public abstract PactDslJsonBody eachLike(String name, int numberExamples);

    /**
     * Array element where each element of the array must match the following object
     * @param numberExamples number of examples to generate
     */
    public abstract PactDslJsonBody eachLike(int numberExamples);

    /**
     * Array field with a minimum size and each element must match the provided object
     * @param name field name
     * @param size minimum size
     */
    public abstract PactDslJsonBody minArrayLike(String name, Integer size);

    /**
     * Array element with a minimum size and each element of the array must match the provided object
     * @param size minimum size
     */
    public abstract PactDslJsonBody minArrayLike(Integer size);

    /**
     * Array field with a minimum size and each element must match the provided object
     * @param name field name
     * @param size minimum size
     * @param numberExamples number of examples to generate
     */
    public abstract PactDslJsonBody minArrayLike(String name, Integer size, int numberExamples);

    /**
     * Array element with a minimum size and each element of the array must match the provided object
     * @param size minimum size
     * @param numberExamples number of examples to generate
     */
    public abstract PactDslJsonBody minArrayLike(Integer size, int numberExamples);

    /**
     * Array field with a maximum size and each element must match the provided object
     * @param name field name
     * @param size maximum size
     */
    public abstract PactDslJsonBody maxArrayLike(String name, Integer size);

    /**
     * Array element with a maximum size and each element of the array must match the provided object
     * @param size maximum size
     */
    public abstract PactDslJsonBody maxArrayLike(Integer size);

    /**
     * Array field with a maximum size and each element must match the provided object
     * @param name field name
     * @param size maximum size
     * @param numberExamples number of examples to generate
     */
    public abstract PactDslJsonBody maxArrayLike(String name, Integer size, int numberExamples);

    /**
     * Array element with a maximum size and each element of the array must match the provided object
     * @param size maximum size
     * @param numberExamples number of examples to generate
     */
    public abstract PactDslJsonBody maxArrayLike(Integer size, int numberExamples);

    /**
     * Array field with a minimum and maximum size and each element must match the provided object
     * @param name field name
     * @param minSize minimum size
     * @param maxSize maximum size
     */
    public abstract PactDslJsonBody minMaxArrayLike(String name, Integer minSize, Integer maxSize);

    /**
     * Array element with a minimum and maximum size and each element of the array must match the provided object
     * @param minSize minimum size
     * @param maxSize maximum size
     */
    public abstract PactDslJsonBody minMaxArrayLike(Integer minSize, Integer maxSize);

    /**
     * Array field with a minimum and maximum size and each element must match the provided object
     * @param name field name
     * @param minSize minimum size
     * @param maxSize maximum size
     * @param numberExamples number of examples to generate
     */
    public abstract PactDslJsonBody minMaxArrayLike(String name, Integer minSize, Integer maxSize, int numberExamples);

    /**
     * Array element with a minimum and maximum size and each element of the array must match the provided object
     * @param minSize minimum size
     * @param maxSize maximum size
     * @param numberExamples number of examples to generate
     */
    public abstract PactDslJsonBody minMaxArrayLike(Integer minSize, Integer maxSize, int numberExamples);

    /**
     * Array field where each element is an array and must match the following object
     * @param name field name
     */
    public abstract PactDslJsonArray eachArrayLike(String name);

    /**
     * Array element where each element of the array is an array and must match the following object
     */
    public abstract PactDslJsonArray eachArrayLike();

    /**
     * Array field where each element is an array and must match the following object
     * @param name field name
     * @param numberExamples number of examples to generate
     */
    public abstract PactDslJsonArray eachArrayLike(String name, int numberExamples);

    /**
     * Array element where each element of the array is an array and must match the following object
     * @param numberExamples number of examples to generate
     */
    public abstract PactDslJsonArray eachArrayLike(int numberExamples);

    /**
     * Array field where each element is an array and must match the following object
     * @param name field name
     * @param size Maximum size of the outer array
     */
    public abstract PactDslJsonArray eachArrayWithMaxLike(String name, Integer size);

    /**
     * Array element where each element of the array is an array and must match the following object
     * @param size Maximum size of the outer array
     */
    public abstract PactDslJsonArray eachArrayWithMaxLike(Integer size);

    /**
     * Array field where each element is an array and must match the following object
     * @param name field name
     * @param numberExamples number of examples to generate
     * @param size Maximum size of the outer array
     */
    public abstract PactDslJsonArray eachArrayWithMaxLike(String name, int numberExamples, Integer size);

    /**
     * Array element where each element of the array is an array and must match the following object
     * @param numberExamples number of examples to generate
     * @param size Maximum size of the outer array
     */
    public abstract PactDslJsonArray eachArrayWithMaxLike(int numberExamples, Integer size);

    /**
     * Array field where each element is an array and must match the following object
     * @param name field name
     * @param size Minimum size of the outer array
     */
    public abstract PactDslJsonArray eachArrayWithMinLike(String name, Integer size);

    /**
     * Array element where each element of the array is an array and must match the following object
     * @param size Minimum size of the outer array
     */
    public abstract PactDslJsonArray eachArrayWithMinLike(Integer size);

    /**
     * Array field where each element is an array and must match the following object
     * @param name field name
     * @param numberExamples number of examples to generate
     * @param size Minimum size of the outer array
     */
    public abstract PactDslJsonArray eachArrayWithMinLike(String name, int numberExamples, Integer size);

    /**
     * Array element where each element of the array is an array and must match the following object
     * @param numberExamples number of examples to generate
     * @param size Minimum size of the outer array
     */
    public abstract PactDslJsonArray eachArrayWithMinLike(int numberExamples, Integer size);

  /**
   * Array field where each element is an array and must match the following object
   * @param name field name
   * @param minSize minimum size
   * @param maxSize maximum size
   */
  public abstract PactDslJsonArray eachArrayWithMinMaxLike(String name, Integer minSize, Integer maxSize);

  /**
   * Array element where each element of the array is an array and must match the following object
   * @param minSize minimum size
   * @param maxSize maximum size
   */
  public abstract PactDslJsonArray eachArrayWithMinMaxLike(Integer minSize, Integer maxSize);

  /**
   * Array field where each element is an array and must match the following object
   * @param name field name
   * @param numberExamples number of examples to generate
   * @param minSize minimum size
   * @param maxSize maximum size
   */
  public abstract PactDslJsonArray eachArrayWithMinMaxLike(String name, int numberExamples, Integer minSize,
                                                           Integer maxSize);

  /**
   * Array element where each element of the array is an array and must match the following object
   * @param numberExamples number of examples to generate
   * @param minSize minimum size
   * @param maxSize maximum size
   */
  public abstract PactDslJsonArray eachArrayWithMinMaxLike(int numberExamples, Integer minSize, Integer maxSize);

    /**
     * Object field
     * @param name field name
     */
    public abstract PactDslJsonBody object(String name);

    /**
     * Object element
     */
    public abstract PactDslJsonBody object();

    /**
     * Close off the previous object
     * @return
     */
    public abstract DslPart closeObject();

    public MatchingRuleCategory getMatchers() {
        return matchers;
    }

    public void setMatchers(MatchingRuleCategory matchers) {
        this.matchers = matchers;
    }

    protected RegexMatcher regexp(String regex) {
        return new RegexMatcher(regex);
    }

    protected TimestampMatcher matchTimestamp(String format) {
        return new TimestampMatcher(format);
    }

    protected DateMatcher matchDate(String format) {
        return new DateMatcher(format);
    }

    protected TimeMatcher matchTime(String format) {
        return new TimeMatcher(format);
    }

  protected MinTypeMatcher matchMin(Integer min) {
      return new MinTypeMatcher(min);
  }

  protected MaxTypeMatcher matchMax(Integer max) {
        return new MaxTypeMatcher(max);
    }

  protected MinMaxTypeMatcher matchMinMax(Integer minSize, Integer maxSize) {
    return new MinMaxTypeMatcher(minSize, maxSize);
  }

    protected EqualsIgnoreOrderMatcher matchIgnoreOrder() {
        return EqualsIgnoreOrderMatcher.INSTANCE;
    }

    protected MinEqualsIgnoreOrderMatcher matchMinIgnoreOrder(Integer min) {
        return new MinEqualsIgnoreOrderMatcher(min);
    }

    protected MaxEqualsIgnoreOrderMatcher matchMaxIgnoreOrder(Integer max) {
        return new MaxEqualsIgnoreOrderMatcher(max);
    }

    protected MinMaxEqualsIgnoreOrderMatcher matchMinMaxIgnoreOrder(Integer minSize, Integer maxSize) {
        return new MinMaxEqualsIgnoreOrderMatcher(minSize, maxSize);
    }

    protected IncludeMatcher includesMatcher(Object value) {
      return new IncludeMatcher(String.valueOf(value));
    }

    public PactDslJsonBody asBody() {
        return (PactDslJsonBody) this;
    }

    public PactDslJsonArray asArray() {
        return (PactDslJsonArray) this;
    }

  /**
   * This closes off the object graph build from the DSL in case any close[Object|Array] methods have not been called.
   * @return The root object of the object graph
   */
  public abstract DslPart close();

  public Generators getGenerators() {
    return generators;
  }

  public void setGenerators(Generators generators) {
    this.generators = generators;
  }

  /**
   * Returns the parent of this part (object or array)
   * @return parent, or null if it is the root
   */
  public DslPart getParent() {
    return parent;
  }
}
