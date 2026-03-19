package org.grnet.endpoint.scanner.runtime.entities.jdbc;

import io.agroal.api.AgroalDataSource;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import org.grnet.endpoint.scanner.runtime.entities.ResourceAuthorization;
import org.grnet.endpoint.scanner.runtime.entities.ResourceAuthorizationRepository;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.util.ArrayList;
import java.util.List;

public class ResourceAuthorizationJdbcRepository implements ResourceAuthorizationRepository {

    @Inject
    AgroalDataSource dataSource;

    public List<ResourceAuthorization> findAll() {

        String sql = "SELECT * FROM resource_authorization";

        List<ResourceAuthorization> users = new ArrayList<>();

        try (Connection conn = dataSource.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {

            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    var user = new ResourceAuthorization(
                            rs.getLong("id"),
                            rs.getString("name"),
                            rs.getTime("created_at")
                    );
                    users.add(user);
                }
            }

        } catch (Exception e) {
            throw new RuntimeException(e);
        }

        return users;
    }
}
