package org.grnet.endpoint.scanner.runtime;

public interface ResourceRelationshipResolver {

    String getSourceResourceName();

    String getTargetResourceName();

    boolean isRelatedTo(String sourceResourceId, String targetResourceId);
}