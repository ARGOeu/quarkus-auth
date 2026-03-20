package org.grnet.endpoint.scanner.runtime.entities.jdbc;

import io.agroal.api.AgroalDataSource;
import jakarta.inject.Inject;
import org.grnet.endpoint.scanner.runtime.entities.ResourceAuthorization;
import org.grnet.endpoint.scanner.runtime.entities.ResourceAuthorizationRepository;
//import org.grnet.endpoint.scanner.runtime.entities.ResourceAuthorizationRepository;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;

public class ResourceAuthorizationJdbcRepository implements ResourceAuthorizationRepository {

    @Inject
    AgroalDataSource dataSource;

    @Override
    public List<ResourceAuthorization> findAll() {
        String sql = "SELECT * FROM resource_authorization";
        List<ResourceAuthorization> users = new ArrayList<>();

        try (Connection conn = dataSource.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {

            while (rs.next()) {
                var user = new ResourceAuthorization(
                        rs.getLong("id"),
                        rs.getString("secured_endpoint_id"),
                        rs.getString("rule"),
                        rs.getTimestamp("created_at")
                );
                users.add(user);
            }

        } catch (Exception e) {
            throw new RuntimeException(e);
        }

        return users;
    }

    public List<ResourceAuthorization> list(String column, String value) {
        Set<String> allowedColumns = Set.of("id", "rule", "secured_endpoint_id", "created_at");
        if (!allowedColumns.contains(column)) {
            throw new IllegalArgumentException("Invalid column: " + column);
        }

        String sql = "SELECT * FROM resource_authorization WHERE " + column + " = ?";
        List<ResourceAuthorization> results = new ArrayList<>();

        try (Connection conn = dataSource.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {

            // Handle numeric column separately
            if ("id".equals(column)) {
                ps.setLong(1, Long.parseLong(value));
            } else if ("created_at".equals(column)) {
                ps.setTimestamp(1, Timestamp.valueOf(value)); // expects "HH:mm:ss"
            } else {
                ps.setString(1, value);
            }

            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    var user = new ResourceAuthorization(
                            rs.getLong("id"),
                            rs.getString("secured_endpoint_id"),
                            rs.getString("rule"),
                            rs.getTimestamp("created_at")
                    );
                    results.add(user);
                }
            }

        } catch (Exception e) {
            throw new RuntimeException(e);
        }

        return results;
    }

    @Override
    public void create(ResourceAuthorization entity) {
        String sql = "INSERT INTO resource_authorization (secured_endpoint_id, rule, created_at) VALUES (?, ?, ?)";

        try (Connection conn = dataSource.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {

            ps.setString(1, entity.getSecuredEndpointId());
            ps.setString(2, entity.getRule());

            if (entity.getCreatedAt() == null) {
                entity.setCreatedAt(new Timestamp(System.currentTimeMillis()));
            }
            ps.setTimestamp(3, entity.getCreatedAt());

            ps.executeUpdate();

        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }
}