package au.com.dius.pact.consumer;

import java.util.HashMap;
import java.util.Map;

public abstract class DslPart {
    protected final DslPart parent;
    protected final String root;
    protected Map<String, Object> matchers = new HashMap<String, Object>();

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
    protected abstract Object getBody();

    public abstract PactDslJsonArray array(String name);
    public abstract PactDslJsonArray array();
    public abstract DslPart closeArray();

    public abstract PactDslJsonBody object(String name);
    public abstract PactDslJsonBody object();
    public abstract DslPart closeObject();

    public Map<String, Object> getMatchers() {
        return matchers;
    }

    public void setMatchers(Map<String, Object> matchers) {
        this.matchers = matchers;
    }

    protected Map<String, Object> matchType() {
        Map<String, Object> jsonObject = new HashMap<String, Object>();
        jsonObject.put("match", "type");
        return jsonObject;
    }

    protected Map<String, Object> regexp(String regex) {
        Map<String, Object> jsonObject = new HashMap<String, Object>();
        jsonObject.put("regex", regex);
        return jsonObject;
    }

    protected Map<String, Object> matchTimestamp(String format) {
        Map<String, Object> jsonObject = new HashMap<String, Object>();
        jsonObject.put("timestamp", format);
        return jsonObject;
    }

    protected Map<String, Object> matchDate(String format) {
        Map<String, Object> jsonObject = new HashMap<String, Object>();
        jsonObject.put("date", format);
        return jsonObject;
    }

    protected Map<String, Object> matchTime(String format) {
        Map<String, Object> jsonObject = new HashMap<String, Object>();
        jsonObject.put("time", format);
        return jsonObject;
    }

    public PactDslJsonBody asBody() {
        return (PactDslJsonBody) this;
    }

    public PactDslJsonArray asArray() {
        return (PactDslJsonArray) this;
    }
}
