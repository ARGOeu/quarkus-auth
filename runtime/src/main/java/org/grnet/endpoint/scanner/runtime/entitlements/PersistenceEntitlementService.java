package org.grnet.endpoint.scanner.runtime.entitlements;

import org.grnet.endpoint.scanner.runtime.repositories.PersistenceEntitlementRepository;

import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

public class PersistenceEntitlementService implements EntitlementService{

    private final PersistenceEntitlementRepository persistenceEntitlementRepository;
    private final UserContextInterface userContextInterface;

    public PersistenceEntitlementService(
            PersistenceEntitlementRepository persistenceEntitlementRepository,
            UserContextInterface userContextInterface) {
        this.persistenceEntitlementRepository = persistenceEntitlementRepository;
        this.userContextInterface = userContextInterface;
    }

    @Override
    public List<Entitlement> fetchEntitlements() {

        var actor = persistenceEntitlementRepository.findActorByOidcIdAndIssuer(userContextInterface.getId(), userContextInterface.getIssuer());

        if(actor.isEmpty()){

            return Collections.emptyList();
        } else {

            var rawEntitlements = persistenceEntitlementRepository.findActorEntitlements(actor.get().getId());

            if (rawEntitlements.isEmpty()) {

                return Collections.emptyList();
            } else {

                var entitlements = rawEntitlements
                        .stream()
                        .filter(s -> s.startsWith(userContextInterface.getNamespace()))
                        .map(s -> s.replace(userContextInterface.getNamespace() + ":", ""))
                        .collect(Collectors.toList());

                return EntitlementUtils.parseEntitlements(entitlements);
            }
        }
    }
}
