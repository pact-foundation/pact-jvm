package au.com.dius.pact.consumer;

import nl.flotsam.xeger.Xeger;
import org.apache.commons.lang3.RandomStringUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.time.DateFormatUtils;
import org.json.JSONObject;

import javax.naming.OperationNotSupportedException;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public class PactDslJsonBody extends DslPart {

    private final JSONObject body;

    public PactDslJsonBody() {
        super("$.body");
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
        String name = StringUtils.difference(root + ".", object.root);
        for(String matcherName: object.matchers.keySet()) {
            matchers.put(matcherName, object.matchers.get(matcherName));
        }
        body.put(name, object.getBody());
    }

    protected void putArray(DslPart object) {
        String name = StringUtils.difference(root + ".", object.root);
        for(String matcherName: object.matchers.keySet()) {
            matchers.put(matcherName, object.matchers.get(matcherName));
        }
        body.put(name, object.getBody());
    }

    @Override
    protected Object getBody() {
        return body;
    }

    public PactDslJsonBody stringValue(String name, String value) {
        body.put(name, value);
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

    public PactDslJsonBody stringType(String name) {
        body.put(name, RandomStringUtils.randomAlphabetic(20));
        matchers.put(root + "." + name, matchType());
        return this;
    }

    public PactDslJsonBody numberType(String name) {
        body.put(name, Long.parseLong(RandomStringUtils.randomNumeric(10)));
        matchers.put(root + "." + name, matchType());
        return this;
    }

    public PactDslJsonBody booleanType(String name) {
        body.put(name, true);
        matchers.put(root + "." + name, matchType());
        return this;
    }

    public PactDslJsonBody stringMatcher(String name, String regex, String value) {
        body.put(name, value);
        matchers.put(root + "." + name, regexp(regex));
        return this;
    }

    public PactDslJsonBody stringMatcher(String name, String regex) {
        stringMatcher(name, regex, new Xeger(regex).generate());
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
        matchers.put(root + "." + name, regexp("(\\d{1,3}\\.)+\\d{1,3}"));
        return this;
    }

    public PactDslJsonBody object(String name) {
        return new PactDslJsonBody(root + "." + name, this);
    }

    public PactDslJsonBody object() {
        throw new UnsupportedOperationException("use the object(String name) form");
    }

    public DslPart closeObject() {
        parent.putObject(this);
        return parent;
    }

    public PactDslJsonArray array(String name) {
        return new PactDslJsonArray(root + "." + name, this);
    }

    public PactDslJsonArray array() {
        throw new UnsupportedOperationException("use the array(String name) form");
    }

    @Override
    public DslPart closeArray() {
        throw new UnsupportedOperationException("can't call closeArray on an Object");
    }

    public PactDslJsonBody id() {
        return id("id");
    }

    public PactDslJsonBody id(String name) {
        body.put(name, Long.parseLong(RandomStringUtils.randomNumeric(10)));
        matchers.put(root + "." + name, matchType());
        return this;
    }

    public PactDslJsonBody hexValue(String name) {
        body.put(name, RandomStringUtils.random(10, "0123456789abcdef"));
        matchers.put(root + "." + name, regexp("[0-9a-fA-F]+"));
        return this;
    }

    public PactDslJsonBody guid(String name) {
        body.put(name, UUID.randomUUID().toString());
        matchers.put(root + "." + name, regexp("[0-9a-f]{8}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{12}"));
        return this;
    }

}
