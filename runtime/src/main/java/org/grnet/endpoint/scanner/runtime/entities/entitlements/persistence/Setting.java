package org.grnet.endpoint.scanner.runtime.entities.entitlements.persistence;


import java.util.Objects;

public class Setting {

    private String id;
    private APISetting key;
    private String value;

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public APISetting getKey() {
        return key;
    }

    public void setKey(APISetting key) {
        this.key = key;
    }

    public String getValue() {
        return value;
    }

    public void setValue(String value) {
        this.value = value;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        Setting actor = (Setting) o;
        return Objects.equals(key, actor.key);
    }

    @Override
    public int hashCode() {
        return Objects.hash(key);
    }
}
