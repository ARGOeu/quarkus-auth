package org.grnet.endpoint.scanner.runtime.services;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.transaction.Transactional;
import org.grnet.endpoint.scanner.runtime.entities.ResourceAuthorization;

import java.time.OffsetDateTime;
import java.util.List;

@ApplicationScoped
public class ResourceAuthorizationService {

    @Transactional
    public List<ResourceAuthorization> findAll() {

       var ra = new ResourceAuthorization();
       ra.setName("lalalala");
       ra.setCreatedAt(OffsetDateTime.now());
       ResourceAuthorization.persist(ra);
       return ResourceAuthorization.listAll();
    }
}
