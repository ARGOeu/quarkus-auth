package org.grnet.endpoint.scanner.runtime.entities.jdbc;

import io.agroal.api.AgroalDataSource;
import jakarta.inject.Inject;
import org.grnet.endpoint.scanner.runtime.entities.ResourceAuthorization;
import org.grnet.endpoint.scanner.runtime.entities.ResourceAuthorizationRepository;

import java.sql.*;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;

public class ResourceAuthorizationJdbcRepository implements ResourceAuthorizationRepository {

    @Inject
    AgroalDataSource dataSource;

    @Override
    public List<ResourceAuthorization> findAll() {
        String sql = "SELECT * FROM quarkus_auth.resource_authorization";
        List<ResourceAuthorization> users = new ArrayList<>();

        try (Connection conn = dataSource.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {

            while (rs.next()) {
                var ts = rs.getTimestamp("created_at");
                var createdAt = ts.toLocalDateTime();
                var user = new ResourceAuthorization(
                        rs.getLong("id"),
                        rs.getString("secured_endpoint_id"),
                        rs.getString("rule"),
                        createdAt
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

        String sql = "SELECT * FROM quarkus_auth.resource_authorization WHERE " + column + " = ?";
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
                    var ts = rs.getTimestamp("created_at");
                    var createdAt = ts.toLocalDateTime();
                    var user = new ResourceAuthorization(
                            rs.getLong("id"),
                            rs.getString("secured_endpoint_id"),
                            rs.getString("rule"),
                            createdAt
                    );
                    results.add(user);
                }
            }

        } catch (Exception e) {
            throw new RuntimeException(e);
        }

        return results;
    }
    public ResourceAuthorization findById(Long id) {
        String sql = "SELECT * FROM quarkus_auth.resource_authorization WHERE id = ?";

        try (Connection conn = dataSource.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {

            ps.setLong(1, id);

            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    var ts = rs.getTimestamp("created_at");
                    var createdAt = ts.toLocalDateTime();

                    return new ResourceAuthorization(
                            rs.getLong("id"),
                            rs.getString("secured_endpoint_id"),
                            rs.getString("rule"),
                            createdAt
                    );
                }
            }

        } catch (SQLException e) {
            throw new RuntimeException("Error fetching ResourceAuthorization with id " + id, e);
        }

        return null; // or throw NotFoundException if you prefer
    }

    @Override
    public void create(ResourceAuthorization entity) {
        String sql = "INSERT INTO quarkus_auth.resource_authorization (secured_endpoint_id, rule, created_at) VALUES (?, ?, ?)";

        try (Connection conn = dataSource.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {

            ps.setString(1, entity.getSecuredEndpointId());
            ps.setString(2, entity.getRule());

            if (entity.getCreatedAt() == null) {
                entity.setCreatedAt(LocalDateTime.now());
            }

            ps.setTimestamp(3, Timestamp.valueOf(entity.getCreatedAt()));

            ps.executeUpdate();


        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }
    public void delete(Long id) {
        String sql = "DELETE FROM quarkus_auth.resource_authorization WHERE id = ?";
        try (Connection conn = dataSource.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setLong(1, id);
            ps.executeUpdate();
        } catch (SQLException e) {
            throw new RuntimeException(e);
        }
    }

    @Override
    public void update(Long id, String rule) {
        String sql = "UPDATE quarkus_auth.resource_authorization SET rule = ?, created_at = ? WHERE id = ?";
        try (Connection conn = dataSource.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {

            ps.setString(1, rule);
            ps.setTimestamp(2, Timestamp.valueOf(LocalDateTime.now()));

            ps.setLong(3, id);

            ps.executeUpdate();

        } catch (SQLException e) {
            throw new RuntimeException(e);
        }
    }
}