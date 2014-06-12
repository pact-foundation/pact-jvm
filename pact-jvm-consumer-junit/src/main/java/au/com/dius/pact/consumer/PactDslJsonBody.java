package au.com.dius.pact.consumer;

import org.apache.commons.lang3.RandomStringUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.time.DateFormatUtils;
import org.json.Cookie;
import org.json.JSONObject;

import java.util.Date;

public class PactDslJsonBody {

    private JSONObject body;
    private JSONObject matchers;
    private String root;
    private PactDslJsonBody parent;

    public PactDslJsonBody() {
        root = "$.body";
        matchers = new JSONObject();
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

    public PactDslJsonBody stringMatcher(String name, String value) {
        body.put(name, value);
        matchers.put(root + "." + name, regexp(value));
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
        for(String matcherName: JSONObject.getNames(object.matchers)) {
            matchers.put(matcherName, object.matchers.get(matcherName));
        }
        body.put(name, object.body);
    }

    private JSONObject matchType() {
        JSONObject jsonObject = new JSONObject();
        jsonObject.put("match", "type");
        return jsonObject;
    }

    private JSONObject regexp(String value) {
        JSONObject jsonObject = new JSONObject();
        jsonObject.put("regex", value);
        return jsonObject;
    }

    private JSONObject matchTimestamp() {
        JSONObject jsonObject = new JSONObject();
        jsonObject.put("match", "timestamp");
        return jsonObject;
    }

    public JSONObject getMatchers() {
        return matchers;
    }

    public void setMatchers(JSONObject matchers) {
        this.matchers = matchers;
    }
}
