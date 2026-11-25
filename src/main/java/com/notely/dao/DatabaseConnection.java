package com.notely.dao;

import java.io.IOException;
import java.io.InputStream;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.util.Properties;

/**
 * Factory for obtaining JDBC connections to the MySQL database.
 *
 * <h3>Design decisions</h3>
 * <ul>
 *   <li><b>Not a singleton connection</b> — each call to
 *       {@link #getConnection()} returns a <em>new</em> connection.
 *       The caller owns it and must close it (via try-with-resources).
 *       A shared connection would break under concurrent access.</li>
 *   <li><b>Credentials from file + environment</b> — the class reads
 *       {@code db.properties} from the classpath, then checks for
 *       environment-variable overrides ({@code NOTELY_DB_URL},
 *       {@code NOTELY_DB_USERNAME}, {@code NOTELY_DB_PASSWORD}).
 *       This means developers can set their own passwords without
 *       touching committed files.</li>
 *   <li><b>Lazy loading</b> — properties are read once on first use
 *       and cached in a static field.</li>
 * </ul>
 */
public class DatabaseConnection {

    /** Classpath path to the properties file. */
    private static final String PROPERTIES_FILE = "/com/notely/db.properties";

    /** Cached configuration — loaded once, reused for every connection. */
    private static Properties config;

    // Prevent instantiation — all methods are static.
    private DatabaseConnection() {
    }

    /**
     * Returns a fresh JDBC connection to the configured database.
     *
     * <p>The caller is responsible for closing it, ideally with
     * try-with-resources:</p>
     * <pre>{@code
     * try (Connection conn = DatabaseConnection.getConnection()) {
     *     // use connection
     * }
     * }</pre>
     *
     * @return a new open {@link Connection}
     * @throws SQLException if the connection cannot be established
     */
    public static Connection getConnection() throws SQLException {
        Properties props = loadConfig();
        String url = props.getProperty("db.url");
        String user = props.getProperty("db.username");
        String password = props.getProperty("db.password");
        return DriverManager.getConnection(url, user, password);
    }

    /**
     * Loads configuration from the properties file on first call,
     * then applies any environment-variable overrides.
     *
     * @return the resolved configuration
     */
    private static Properties loadConfig() {
        if (config != null) {
            return config;
        }

        config = new Properties();

        // 1. Load from classpath (db.properties in resources)
        try (InputStream input = DatabaseConnection.class
                .getResourceAsStream(PROPERTIES_FILE)) {
            if (input != null) {
                config.load(input);
            } else {
                System.err.println("[DatabaseConnection] WARNING: "
                        + PROPERTIES_FILE + " not found on classpath.");
            }
        } catch (IOException e) {
            System.err.println("[DatabaseConnection] WARNING: "
                    + "Failed to read " + PROPERTIES_FILE + ": " + e.getMessage());
        }

        // 2. Environment variables override file values
        String envUrl = System.getenv("NOTELY_DB_URL");
        String envUser = System.getenv("NOTELY_DB_USERNAME");
        String envPass = System.getenv("NOTELY_DB_PASSWORD");

        if (envUrl != null && !envUrl.isBlank()) {
            config.setProperty("db.url", envUrl);
        }
        if (envUser != null && !envUser.isBlank()) {
            config.setProperty("db.username", envUser);
        }
        if (envPass != null && !envPass.isBlank()) {
            config.setProperty("db.password", envPass);
        }

        return config;
    }

    /**
     * Resets the cached configuration so that the next call to
     * {@link #getConnection()} re-reads the properties file.
     * Useful in tests to switch between databases.
     */
    public static void resetConfig() {
        config = null;
    }
}
