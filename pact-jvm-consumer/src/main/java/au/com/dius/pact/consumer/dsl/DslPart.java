package au.com.dius.pact.consumer.dsl;

import au.com.dius.pact.model.matchingrules.Category;
import au.com.dius.pact.model.matchingrules.DateMatcher;
import au.com.dius.pact.model.matchingrules.MaxTypeMatcher;
import au.com.dius.pact.model.matchingrules.MinTypeMatcher;
import au.com.dius.pact.model.matchingrules.RegexMatcher;
import au.com.dius.pact.model.matchingrules.TimeMatcher;
import au.com.dius.pact.model.matchingrules.TimestampMatcher;

/**
 * Abstract base class to support Object and Array JSON DSL builders
 */
public abstract class DslPart {
    public static final String HEXADECIMAL = "[0-9a-fA-F]+";
    public static final String IP_ADDRESS = "(\\d{1,3}\\.)+\\d{1,3}";
    public static final String UUID_REGEX = "[0-9a-f]{8}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{12}";
    private static final String MATCH = "match";

    protected final DslPart parent;
    protected final String root;
    protected Category matchers = new Category("body");
    protected boolean closed = false;

    public DslPart(DslPart parent, String root) {
        this.parent = parent;
        this.root = root;
    }

    public DslPart(String root) {
        this.parent = null;
        this.root = root;
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
     * Close of the previous array element
     */
    public abstract DslPart closeArray();

    /**
     * Array field where each element must match the following object
     * @param name field name
     * @deprecated Use eachLike instead
     */
    @Deprecated
    public abstract PactDslJsonBody arrayLike(String name);

    /**
     * Array element where each element of the array must match the following object
     * @deprecated Use eachLike instead
     */
    @Deprecated
    public abstract PactDslJsonBody arrayLike();

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
     * Array field with a minumum size and each element must match the provided object
     * @param name field name
     * @param size minimum size
     */
    public abstract PactDslJsonBody minArrayLike(String name, Integer size);

    /**
     * Array element with a minumum size and each element of the array must match the provided object
     * @param size minimum size
     */
    public abstract PactDslJsonBody minArrayLike(Integer size);

    /**
     * Array field with a minumum size and each element must match the provided object
     * @param name field name
     * @param size minimum size
     * @param numberExamples number of examples to generate
     */
    public abstract PactDslJsonBody minArrayLike(String name, Integer size, int numberExamples);

    /**
     * Array element with a minumum size and each element of the array must match the provided object
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
     * @param size minimum size
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
     * @param size minimum size
     * @param numberExamples number of examples to generate
     */
    public abstract PactDslJsonBody maxArrayLike(Integer size, int numberExamples);

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

    public Category getMatchers() {
        return matchers;
    }

    public void setMatchers(Category matchers) {
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
}
