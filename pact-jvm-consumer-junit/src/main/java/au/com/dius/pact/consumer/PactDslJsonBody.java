package au.com.dius.pact.consumer;

import au.com.dius.pact.matchers.Matcher;
import org.apache.commons.lang3.RandomStringUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.time.DateFormatUtils;
import org.json.JSONObject;
import scala.None$;
import scala.Some$;

import java.util.Date;
import java.util.HashMap;
import java.util.Map;

public class PactDslJsonBody {

    private JSONObject body;
    private Map<String, Object> matchers;
    private String root;
    private PactDslJsonBody parent;

    public PactDslJsonBody() {
        root = "$.body";
        matchers = new HashMap<String, Object>();
        body = new JSONObject();
    }

    public PactDslJsonBody(String root, PactDslJsonBody parent) {
        this();
        this.root = root;
        this.parent = parent;
    }

    public String toString() {
        return body.toString();
    }

    public PactDslJsonBody stringValue(String name, String value) {
        body.put(name, value);
        return this;
    }

    public PactDslJsonBody stringValue(String name) {
        body.put(name, RandomStringUtils.randomAlphabetic(20));
        matchers.put(root + "." + name, matchType());
        return this;
    }

    public PactDslJsonBody numberValue(String name, Number value) {
        body.put(name, value);
        return this;
    }

    public PactDslJsonBody booleanValue(String name, Boolean value) {
        body.put(name, value);
        return this;
    }

    public PactDslJsonBody stringMatcher(String name, String regexp, String value) {
        body.put(name, value);
        matchers.put(root + "." + name, regexp(regexp, value));
        return this;
    }

    public PactDslJsonBody timestamp() {
        return timestamp("timestamp");
    }

    public PactDslJsonBody timestamp(String name) {
        body.put(name, DateFormatUtils.ISO_DATETIME_FORMAT.format(new Date()));
        matchers.put(root + "." + name, matchTimestamp());
        return this;
    }

    public PactDslJsonBody ipAddress(String name) {
        body.put(name, "127.0.0.1");
        matchers.put(root + "." + name, regexp("(\\d{1-3}\\.)+\\d{1-3}"));
        return this;
    }

    public PactDslJsonBody object(String name) {
        return new PactDslJsonBody(root + "." + name, this);
    }

    public PactDslJsonBody closeObject() {
        parent.putObject(this);
        return parent;
    }

    public PactDslJsonBody id() {
        return id("id");
    }

    public PactDslJsonBody id(String name) {
        body.put(name, RandomStringUtils.randomNumeric(10));
        matchers.put(root + "." + name, regexp("\\d+"));
        return this;
    }

    public PactDslJsonBody hexValue(String name) {
        body.put(name, RandomStringUtils.random(10, "0123456789abcdef"));
        matchers.put(root + "." + name, regexp("[0-9a-fA-F]+"));
        return this;
    }

    private void putObject(PactDslJsonBody object) {
        String name = StringUtils.difference(root + ".", object.root);
        for(String matcherName: object.matchers.keySet()) {
            matchers.put(matcherName, object.matchers.get(matcherName));
        }
        body.put(name, object.body);
    }

    private Map<String, Object> matchType() {
        Map<String, Object> jsonObject = new HashMap<String, Object>();
        jsonObject.put("match", "type");
        return jsonObject;
    }

    private Map<String, Object> regexp(String regex) {
        Map<String, Object> jsonObject = new HashMap<String, Object>();
        jsonObject.put("regex", new Matcher(regex, None$.<String>empty()));
        return jsonObject;
    }

    private Map<String, Object> regexp(String regex, String value) {
        Map<String, Object> jsonObject = new HashMap<String, Object>();
        jsonObject.put("regex", new Matcher(regex, Some$.MODULE$.apply(value)));
        return jsonObject;
    }

    private Map<String, Object> matchTimestamp() {
        Map<String, Object> jsonObject = new HashMap<String, Object>();
        jsonObject.put("match", "timestamp");
        return jsonObject;
    }

    public Map<String, Object> getMatchers() {
        return matchers;
    }

    public void setMatchers(Map<String, Object> matchers) {
        this.matchers = matchers;
    }
}
