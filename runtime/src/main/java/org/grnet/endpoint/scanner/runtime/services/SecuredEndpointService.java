package org.grnet.endpoint.scanner.runtime.services;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.transaction.Transactional;
import org.grnet.endpoint.scanner.runtime.entities.SecuredEndpoint;

import java.util.List;

@ApplicationScoped
public class SecuredEndpointService {

    @Transactional
    public List<SecuredEndpoint> findAll(){

        return SecuredEndpoint.findAll().list();
    }
}
