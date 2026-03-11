package org.grnet.endpoint.scanner.runtime.database;

import io.agroal.api.AgroalDataSource;
import io.quarkus.datasource.common.runtime.DatabaseKind;
import org.apache.ibatis.jdbc.ScriptRunner;
import org.grnet.endpoint.scanner.runtime.EndpointMetadata;
import org.jboss.logging.Logger;

import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.util.HexFormat;
import java.util.List;
import java.util.Objects;

public class SchemaInitializer {

    private static final Logger LOG = Logger.getLogger(SchemaInitializer.class);

    private final AgroalDataSource dataSource;

    public SchemaInitializer(AgroalDataSource dataSource) {
        this.dataSource = dataSource;
    }

    public void createTables(String dbKind, List<EndpointMetadata> endpoints) {

        if(DatabaseKind.isPostgreSQL(dbKind)){

            String insertSql = """
                INSERT INTO secured_endpoint (secured_endpoint_id, resource, action, path, description)
                VALUES (?, ?, ?, ?, ?)
                """;

            String checkSql = "SELECT COUNT(*) FROM secured_endpoint WHERE secured_endpoint_id = ?";

            LOG.info("Secured Endpoints extension: Creating tables for PostgreSQL...");

            try (Connection conn = dataSource.getConnection(); var reader = new InputStreamReader(Objects.requireNonNull(getClass().getResourceAsStream("/db/postgresql/init.sql")))) {

                var runner = new ScriptRunner(conn);
                runner.setAutoCommit(false);       // use transactions
                runner.setStopOnError(true);
                runner.setSendFullScript(true);    // PostgreSQL handles full scripts well
                runner.setLogWriter(null);         // suppress output
                runner.setErrorLogWriter(null);
                runner.runScript(reader);
                conn.commit();

                insertEndpoints(conn, endpoints, insertSql, checkSql);
            } catch (Exception e) {
                throw new RuntimeException("Failed to run extension SQL script for Postgresql", e);
            }
        } else {
            throw new RuntimeException("Unsupported database kind: " + dbKind);
        }
    }

    private void insertEndpoints(Connection conn, List<EndpointMetadata> endpoints, String insertSql, String checkSql) throws SQLException {

        conn.setAutoCommit(false);

        try (PreparedStatement checkStmt = conn.prepareStatement(checkSql);
             PreparedStatement insertStmt = conn.prepareStatement(insertSql)) {

            for (EndpointMetadata endpoint : endpoints) {

                String securedEndpointId = generateSecuredEndpointId(endpoint);

                checkStmt.setString(1, securedEndpointId);
                try (var rs = checkStmt.executeQuery()) {
                    if (rs.next() && rs.getInt(1) > 0) {
                        LOG.info(String.format("Endpoint already exists, skipping: %s", securedEndpointId));
                        continue;
                    }
                }

                insertStmt.setString(1, securedEndpointId);
                insertStmt.setString(2, endpoint.getResource());
                insertStmt.setString(3, endpoint.getAction());
                insertStmt.setString(4, endpoint.getPath());
                insertStmt.setString(5, endpoint.getDescription());
                insertStmt.executeUpdate();
            }

            conn.commit();
            LOG.info("Endpoints inserted successfully.");

        } catch (SQLException e) {
            conn.rollback();
            LOG.error("Failed to insert endpoints, transaction rolled back.", e);
            throw new RuntimeException("Failed to insert endpoints", e);
        } finally {
            conn.setAutoCommit(true);
        }
    }

    private String generateSecuredEndpointId(EndpointMetadata endpoint) {
        String raw = endpoint.getResource() + endpoint.getAction() + endpoint.getPath();
        try {
            var digest = MessageDigest.getInstance("SHA-256");
            byte[] hash = digest.digest(raw.getBytes(StandardCharsets.UTF_8));
            return HexFormat.of().formatHex(hash);
        } catch (NoSuchAlgorithmException e) {
            throw new RuntimeException("Failed to generate securedEndpointId hash", e);
        }
    }
}
