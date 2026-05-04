package org.grnet.endpoint.scanner.runtime.clients.groupmanagement;

import jakarta.ws.rs.ForbiddenException;
import jakarta.ws.rs.NotAuthorizedException;
import jakarta.ws.rs.NotFoundException;
import jakarta.ws.rs.core.MultivaluedMap;
import jakarta.ws.rs.core.Response;
import org.eclipse.microprofile.rest.client.ext.ResponseExceptionMapper;

public class KeycloakExceptionMapper implements ResponseExceptionMapper<RuntimeException> {

    @Override
    public RuntimeException toThrowable(Response response) {

        var reason = response.getStatusInfo()!=null ? response.getStatusInfo().getReasonPhrase() : "unknown reason";
        int status = response.getStatus();

        switch (status) {
            case 400, 404:
                return new NotFoundException("Not found. Reason : "+ reason);
            case 401:
                return new NotAuthorizedException("Unauthorized group management client. Reason : "+ reason);
            case 403:
                return new ForbiddenException("Forbidden group management client. Reason : "+ reason);
            default:
                return new RuntimeException("HTTP group management error. Reason : " + reason);
        }
    }

    @Override
    public boolean handles(int status, MultivaluedMap<String, Object> headers) {
        return status >= 400;
    }
}