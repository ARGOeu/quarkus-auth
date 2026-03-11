package org.grnet.endpoint.scanner.runtime.entities.jdbc;

import io.agroal.api.AgroalDataSource;
import jakarta.inject.Inject;
import org.grnet.endpoint.scanner.runtime.entities.EndpointResolver;
import org.grnet.endpoint.scanner.runtime.entities.EndpointResolverRepository;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;

public class EndpointResolverJdbcRepository implements EndpointResolverRepository {

    @Inject
    AgroalDataSource dataSource;

    @Override
    public List<EndpointResolver> findAll() {
        String sql = "SELECT * FROM endpoint_resolver";
        List<EndpointResolver> users = new ArrayList<>();

        try (Connection conn = dataSource.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {

            while (rs.next()) {
                var user = new EndpointResolver(
                        rs.getLong("id"),
                        rs.getString("secured_endpoint_id"),
                        rs.getString("resource"),
                        rs.getString("original_field"),
                        rs.getString("mapped_field"),
                        rs.getTimestamp("created_at")
                );
                users.add(user);
            }

        } catch (Exception e) {
            throw new RuntimeException(e);
        }

        return users;
    }
    public List<EndpointResolver> list(String column, String value) {
        Set<String> allowedColumns = Set.of("id", "resource","original_field","mapped_field", "secured_endpoint_id", "created_at");
        if (!allowedColumns.contains(column)) {
            throw new IllegalArgumentException("Invalid column: " + column);
        }

        String sql = "SELECT * FROM endpoint_resolver WHERE " + column + " = ?";

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
                    var user = new EndpointResolver(
                            rs.getLong("id"),
                            rs.getString("secured_endpoint_id"),
                            rs.getString("resource"),
                            rs.getString("original_field"),
                            rs.getString("mapped_field"),
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
    public void create(EndpointResolver entity) {

        String sql = "INSERT INTO endpoint_resolver " +
                "(secured_endpoint_id, resource, original_field, mapped_field) " +
                "VALUES (?, ?, ?, ?)";

        try (Connection conn = dataSource.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {

            ps.setString(1, entity.getSecuredEndpointId());
            ps.setString(2, entity.getResource());
            ps.setString(3, entity.getOriginalField());
            ps.setString(4, entity.getMappedField());

            ps.executeUpdate();

//            try (ResultSet rs = ps.getGeneratedKeys()) {
//
//                if (rs.next()) {
//                    entity.setId(rs.getString(1));
//                }
//            }

        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }
}