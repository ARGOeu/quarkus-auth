package org.grnet.endpoint.scanner.runtime.entities;

import java.util.List;

public interface ResourceAuthorizationRepository {

    List<?> findAll();
}
