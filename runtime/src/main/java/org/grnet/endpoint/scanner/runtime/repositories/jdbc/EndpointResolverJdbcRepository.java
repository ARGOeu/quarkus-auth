package org.grnet.endpoint.scanner.runtime.repositories.jdbc;

import io.agroal.api.AgroalDataSource;
import jakarta.inject.Inject;
import org.grnet.endpoint.scanner.runtime.entities.EndpointResolver;
import org.grnet.endpoint.scanner.runtime.repositories.EndpointResolverRepository;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;

public class EndpointResolverJdbcRepository implements EndpointResolverRepository {

    @Inject
    AgroalDataSource dataSource;

    @Override
    public List<EndpointResolver> findAll() {
        String sql = "SELECT * FROM quarkus_auth.endpoint_resolver";
        List<EndpointResolver> resolvers = new ArrayList<>();

        try (Connection conn = dataSource.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {

            while (rs.next()) {

                var ts = rs.getTimestamp("created_at");
                var createdAt = ts.toLocalDateTime();
                var resolver = new EndpointResolver(
                        rs.getLong("id"),
                        rs.getString("secured_endpoint_id"),
                        rs.getString("resource"),
                        rs.getString("original_field"),
                        rs.getString("mapped_field"),
                        createdAt
                );
                resolvers.add(resolver);
            }

        } catch (Exception e) {
            throw new RuntimeException(e);
        }

        return resolvers;
    }

    public EndpointResolver findById(Long id) {
        String sql = "SELECT * FROM quarkus_auth.endpoint_resolver WHERE id = ?";

        try (Connection conn = dataSource.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {

            ps.setLong(1, id);

            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    var ts = rs.getTimestamp("created_at");
                    var createdAt = ts.toLocalDateTime();

                    return new EndpointResolver(
                            rs.getLong("id"),
                            rs.getString("secured_endpoint_id"),
                            rs.getString("resource"),
                            rs.getString("original_field"),
                            rs.getString("mapped_field"),
                            createdAt
                    );
                }
            }

        } catch (SQLException e) {
            throw new RuntimeException("Error fetching ResourceAuthorization with id " + id, e);
        }

        return null; // or throw NotFoundException if you prefer
    }

    public void delete(Long id) {
        String sql = "DELETE FROM quarkus_auth.endpoint_resolver WHERE id = ?";
        try (Connection conn = dataSource.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setLong(1, id);
            ps.executeUpdate();
        } catch (SQLException e) {
            throw new RuntimeException(e);
        }
    }

    public void update(EndpointResolver re) {
        String sql = "UPDATE quarkus_auth.endpoint_resolver SET original_field = ?, mapped_field = ? , created_at = ? WHERE id = ?";
        try (Connection conn = dataSource.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {

            ps.setString(1, re.getOriginalField());
            ps.setString(2, re.getMappedField());
            ps.setTimestamp(3,Timestamp.valueOf(re.getCreatedAt()));
            ps.setLong(4, re.getId());

            ps.executeUpdate();

        } catch (SQLException e) {
            throw new RuntimeException(e);
        }
    }

    public List<EndpointResolver> list(String column, String value) {
        Set<String> allowedColumns = Set.of("id", "resource", "original_field", "mapped_field", "secured_endpoint_id", "created_at");
        if (!allowedColumns.contains(column)) {
            throw new IllegalArgumentException("Invalid column: " + column);
        }

        String sql = "SELECT * FROM quarkus_auth.endpoint_resolver WHERE " + column + " = ?";

        List<EndpointResolver> results = new ArrayList<>();

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
                    var user = new EndpointResolver(
                            rs.getLong("id"),
                            rs.getString("secured_endpoint_id"),
                            rs.getString("resource"),
                            rs.getString("original_field"),
                            rs.getString("mapped_field"),
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
    public void create(EndpointResolver entity) {

        String sql = "INSERT INTO quarkus_auth.endpoint_resolver " +
                "(secured_endpoint_id, resource, original_field, mapped_field) " +
                "VALUES (?, ?, ?, ?)";

        try (Connection conn = dataSource.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {

            ps.setString(1, entity.getSecuredEndpointId());
            ps.setString(2, entity.getResource());
            ps.setString(3, entity.getOriginalField());
            ps.setString(4, entity.getMappedField());

            ps.executeUpdate();
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }
}