package org.grnet.endpoint.scanner.runtime.process;

import java.util.HashMap;
import java.util.Map;

public class Event {

    public Event(Map<String, Object> extras) {
        this.extras = extras;
    }

    private Map<String, Object> extras = new HashMap<>();

    private boolean skipDefault = false;

    private Object result;

    public Map<String, Object> getExtras() {
        return extras;
    }

    public void setExtras(Map<String, Object> extras) {
        this.extras = extras;
    }

    public boolean isSkipDefault() {
        return skipDefault;
    }

    public void setSkipDefault(boolean skipDefault) {
        this.skipDefault = skipDefault;
    }

    public Object getResult() {
        return result;
    }

    public void setResult(Object result) {
        this.result = result;
    }
}
