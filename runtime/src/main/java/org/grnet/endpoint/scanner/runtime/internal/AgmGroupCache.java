package org.grnet.endpoint.scanner.runtime.internal;

import io.quarkus.cache.CacheInvalidateAll;
import io.quarkus.cache.CacheResult;
import io.quarkus.logging.Log;
import io.quarkus.runtime.StartupEvent;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.enterprise.event.Observes;
import jakarta.inject.Inject;
import org.grnet.endpoint.scanner.runtime.clients.groupmanagement.AuthGroupManagement;
import org.grnet.endpoint.scanner.runtime.clients.groupmanagement.response.Group;
import org.jboss.logging.Logger;

import java.util.Map;

@ApplicationScoped
public class AgmGroupCache {

    private static final Logger LOG = Logger.getLogger(AgmGroupCache.class);

    @Inject
    AuthGroupManagement authGroupManagement;

    @CacheResult(cacheName = "agm-groups")
    public Map<String, Group> getGroups() {
        LOG.info("Loading group tree from AGM.");
        return authGroupManagement.flattenGroups();
    }

    @CacheInvalidateAll(cacheName = "agm-groups")
    public void invalidate() {
        LOG.info("AGM groups cache invalidated.");
    }

    void warmUp(@Observes StartupEvent event) {
        try {
            getGroups();
            Log.info("AGM groups cache warmed up.");
        } catch (Exception e) {
            Log.warn("AGM groups cache warm-up failed. Cache will be loaded on first request.", e);
        }
    }
}