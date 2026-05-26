package org.grnet.endpoint.scanner.runtime.repositories.jdbc;

import io.agroal.api.AgroalDataSource;
import jakarta.inject.Inject;
import org.grnet.endpoint.scanner.runtime.entities.RoleEndpoint;
import org.grnet.endpoint.scanner.runtime.repositories.RoleEndpointRepository;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

public class RoleEndpointJdbcRepository implements RoleEndpointRepository {


    @Inject
    AgroalDataSource dataSource;

    @Override
    public List<RoleEndpoint> findAll() {
        String sql = "SELECT * FROM quarkus_auth.role_endpoint";
        List<RoleEndpoint> endpoints = new ArrayList<>();

        try (Connection conn = dataSource.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {

            while (rs.next()) {

                var ts = rs.getTimestamp("created_at");
                var createdAt = ts.toLocalDateTime();
                var endpoint = new RoleEndpoint(
                        rs.getLong("id"),
                        rs.getString("role_name"),
                        rs.getString("role_id"),
                        rs.getString("secured_endpoint_id"),
                        createdAt,
                        rs.getString("scope")
                );
                endpoints.add(endpoint);
            }

        } catch (Exception e) {
            throw new RuntimeException(e);
        }

        return endpoints;
    }

    public RoleEndpoint findById(Long id) {
        String sql = "SELECT * FROM quarkus_auth.role_endpoint WHERE id = ?";

        try (Connection conn = dataSource.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {

            ps.setLong(1, id);

            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    var ts = rs.getTimestamp("created_at");
                    var createdAt = ts.toLocalDateTime();

                    return new RoleEndpoint(
                            rs.getLong("id"),

                            rs.getString("role_name"),
                            rs.getString("secured_endpoint_id"),
                            rs.getString("role_id"),
                            createdAt,
                            rs.getString("scope")
                    );
                }
            }

        } catch (SQLException e) {
            throw new RuntimeException("Error fetching RoleEndpoint with id " + id, e);
        }

        return null; // or throw NotFoundException if you prefer
    }

    public void delete(Long id) {
        String sql = "DELETE FROM quarkus_auth.role_endpoint WHERE id = ?";
        try (Connection conn = dataSource.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setLong(1, id);
            ps.executeUpdate();
        } catch (SQLException e) {
            throw new RuntimeException(e);
        }
    }

    public void deleteByRoleIdAndEndpointId(String roleId, String securedEndpointId) {
        String sql = "DELETE FROM quarkus_auth.role_endpoint WHERE role_id = ? and secured_endpoint_id = ?";
        try (Connection conn = dataSource.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, roleId);
            ps.setString(2, securedEndpointId);
            ps.executeUpdate();
        } catch (SQLException e) {
            throw new RuntimeException(e);
        }
    }

    public void deleteByRoleId(String roleId) {
        String sql = "DELETE FROM quarkus_auth.role_endpoint WHERE role_id = ? ";
        try (Connection conn = dataSource.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, roleId);
            ps.executeUpdate();
        } catch (SQLException e) {
            throw new RuntimeException(e);
        }
    }

    public void deleteByRoleIdAndEndpointIds(String roleId, List<String> securedEndpointIds) {

        if (securedEndpointIds == null || securedEndpointIds.isEmpty()) {
            return;
        }

        String placeholders = securedEndpointIds.stream()
                .map(id -> "?")
                .collect(Collectors.joining(","));

        String sql = "DELETE FROM quarkus_auth.role_endpoint WHERE role_id = ? AND secured_endpoint_id IN (" + placeholders + ")";

        try (Connection conn = dataSource.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {

            ps.setString(1, roleId);

            for (int i = 0; i < securedEndpointIds.size(); i++) {
                ps.setString(i + 2, securedEndpointIds.get(i));
            }

            ps.executeUpdate();

        } catch (SQLException e) {
            throw new RuntimeException(e);
        }
    }
    public List<RoleEndpoint> list(String column, String value) {
        Set<String> allowedColumns = Set.of("id", "role_name", "role_id", "secured_endpoint_id", "created_at");
        if (!allowedColumns.contains(column)) {
            throw new IllegalArgumentException("Invalid column: " + column);
        }

        String sql = "SELECT * FROM quarkus_auth.role_endpoint WHERE " + column + " = ?";

        List<RoleEndpoint> results = new ArrayList<>();

        try (Connection conn = dataSource.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {

            // Log before setting parameters

            if ("id".equals(column)) {
                try {
                    long parsed = Long.parseLong(value);
                    ps.setLong(1, parsed);
                } catch (NumberFormatException e) {
                    throw e;
                }
            } else if ("created_at".equals(column)) {
                // TODO: implement Timestamp parsing if needed
                ps.setString(1, value); // temporary
            } else {
                ps.setString(1, value);
            }

            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {

                    var ts = rs.getTimestamp("created_at");
                    var createdAt = ts.toLocalDateTime();
                    var roleEndpoint = new RoleEndpoint(
                            rs.getLong("id"),

                            rs.getString("role_name"),
                            rs.getString("role_id"),
                            rs.getString("secured_endpoint_id"),
                            createdAt,
                            rs.getString("scope")
                    );
                    results.add(roleEndpoint);
                }
            }


        } catch (Exception e) {
            throw new RuntimeException(e);
        }
        return results;
    }

    public void create(RoleEndpoint entity) {

        String sql = "INSERT INTO quarkus_auth.role_endpoint " +
                "(secured_endpoint_id, role_id,role_name,scope) " +
                "VALUES (?, ?,?,?)";

        try (Connection conn = dataSource.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {

            ps.setString(1, entity.getSecuredEndpointId());
            ps.setString(2, entity.getRoleId());
            ps.setString(3, entity.getRoleName());
            String scope = entity.getScope();

            ps.setString(
                    4,
                    (scope == null || scope.isBlank())
                            ? null
                            : scope.trim().toUpperCase()
            );
            ps.executeUpdate();
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }
    public void update(RoleEndpoint entity) {

        String sql = """
        UPDATE quarkus_auth.role_endpoint
        SET role_name = ?,
            scope = ?
        WHERE role_id = ?
          AND secured_endpoint_id = ?
    """;

        try (Connection conn = dataSource.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {

            ps.setString(1, entity.getRoleName());
            String scope = entity.getScope();

            ps.setString(
                    2,
                    (scope == null || scope.isBlank())
                            ? null
                            : scope.trim().toUpperCase()
            );
            ps.setString(3, entity.getRoleId());
            ps.setString(4, entity.getSecuredEndpointId());

            int updated = ps.executeUpdate();

            if (updated == 0) {
                throw new RuntimeException(
                        "No RoleEndpoint found to update for roleId="
                                + entity.getRoleId()
                                + " endpointId="
                                + entity.getSecuredEndpointId()
                );
            }

        } catch (SQLException e) {
            throw new RuntimeException("Error updating RoleEndpoint", e);
        }
    }
}


