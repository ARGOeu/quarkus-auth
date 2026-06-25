package org.grnet.endpoint.scanner.runtime.repositories;

public interface Repository<E, ID> {
    E findById(ID id);
}