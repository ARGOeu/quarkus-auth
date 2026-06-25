package org.grnet.endpoint.scanner.runtime;
public class ApiResourceMetadata {

    private String resourceName;
    private String className;

    public ApiResourceMetadata() {}

    public ApiResourceMetadata(String resourceName, String className) {
        this.resourceName = resourceName;
        this.className = className;
    }

    public String getResourceName() {
        return resourceName;
    }

    public void setResourceName(String resourceName) {
        this.resourceName = resourceName;
    }

    public String getClassName() {
        return className;
    }

    public void setClassName(String className) {
        this.className = className;
    }
}