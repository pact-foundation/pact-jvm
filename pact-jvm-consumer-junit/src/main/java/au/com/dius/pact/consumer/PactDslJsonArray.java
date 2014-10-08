package au.com.dius.pact.consumer;

import nl.flotsam.xeger.Xeger;
import org.apache.commons.lang3.RandomStringUtils;
import org.apache.commons.lang3.time.DateFormatUtils;
import org.json.JSONArray;

import java.util.Date;
import java.util.UUID;

public class PactDslJsonArray extends DslPart {

    private final JSONArray body;

    public PactDslJsonArray(String root, DslPart parent) {
        super(parent, root);
        body = new JSONArray();
    }

    public DslPart closeArray() {
        parent.putArray(this);
        return parent;
    }

    protected void putObject(DslPart object) {
        for(String matcherName: object.matchers.keySet()) {
            matchers.put(matcherName, object.matchers.get(matcherName));
        }
        body.put(object.getBody());
    }

    protected void putArray(DslPart object) {
        for(String matcherName: object.matchers.keySet()) {
            matchers.put(matcherName, object.matchers.get(matcherName));
        }
        body.put(object.getBody());
    }

    @Override
    protected Object getBody() {
        return body;
    }

    public PactDslJsonArray stringValue(String value) {
        body.put(value);
        return this;
    }

    public PactDslJsonArray string(String value) {
        return stringValue(value);
    }

    public PactDslJsonArray numberValue(Number value) {
        body.put(value);
        return this;
    }

    public PactDslJsonArray number(Number value) {
        return numberValue(value);
    }

    public PactDslJsonArray booleanValue(Boolean value) {
        body.put(value);
        return this;
    }

    public PactDslJsonArray stringType() {
        body.put(RandomStringUtils.randomAlphabetic(20));
        matchers.put(root + "." + body.length(), matchType());
        return this;
    }

    public PactDslJsonArray numberType() {
        body.put(RandomStringUtils.randomNumeric(10));
        matchers.put(root + "." + body.length(), matchType());
        return this;
    }

    public PactDslJsonArray booleanType(String name) {
        body.put(true);
        matchers.put(root + "." + body.length(), matchType());
        return this;
    }

    public PactDslJsonArray stringMatcher(String regex, String value) {
        body.put(value);
        matchers.put(root + "." + body.length(), regexp(regex));
        return this;
    }

    public PactDslJsonArray stringMatcher(String regex) {
        stringMatcher(regex, new Xeger(regex).generate());
        return this;
    }

    public PactDslJsonArray timestamp() {
        body.put(DateFormatUtils.ISO_DATETIME_FORMAT.format(new Date()));
        matchers.put(root + "." + body.length(), matchTimestamp());
        return this;
    }

    public PactDslJsonArray ipAddress() {
        body.put("127.0.0.1");
        matchers.put(root + "." + body.length(), regexp("(\\d{1,3}\\.)+\\d{1,3}"));
        return this;
    }

    public PactDslJsonBody object(String name) {
        throw new UnsupportedOperationException("use the object() form");
    }

    public PactDslJsonBody object() {
        return new PactDslJsonBody(root + "." + (body.length() + 1), this);
    }

    @Override
    public DslPart closeObject() {
        throw new UnsupportedOperationException("can't call closeObject on an Array");
    }

    public PactDslJsonArray array(String name) {
        throw new UnsupportedOperationException("use the array() form");
    }

    public PactDslJsonArray array() {
        return new PactDslJsonArray(root + "." + (body.length() + 1), this);
    }

    public PactDslJsonArray id() {
        body.put(RandomStringUtils.randomNumeric(10));
        matchers.put(root + "." + body.length(), matchType());
        return this;
    }

    public PactDslJsonArray hexValue() {
        body.put(RandomStringUtils.random(10, "0123456789abcdef"));
        matchers.put(root + "." + body.length(), regexp("[0-9a-fA-F]+"));
        return this;
    }

    public PactDslJsonArray guid(String name) {
        body.put(UUID.randomUUID().toString());
        matchers.put(root + "." + name, regexp("[0-9a-f]{8}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{12}"));
        return this;
    }
}
