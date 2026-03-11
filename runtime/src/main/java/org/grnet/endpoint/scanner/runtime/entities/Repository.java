package org.grnet.endpoint.scanner.runtime.entities;

public interface Repository<E, ID> {
    E findById(ID id);
}