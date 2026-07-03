package com.mohistmc.youer.feature.db;

import java.io.File;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.sql.Statement;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.jetbrains.annotations.NotNull;

/**
 * @author Mgazul by MohistMC
 * @date 2026/07/01
 *
 * Manages database connections for all feature modules.
 * - SQLite: single shared file youer-config/database/youer.db, each module uses its own table
 * - MySQL: shared connection, differentiated by table names
 */
public class DatabaseManager {

    private static final Logger LOGGER = LogManager.getLogger("Youer-DB");
    private static Connection sqliteConnection;
    private static Connection mysqlConnection;
    private static boolean initialized = false;

    /**
     * Initialize the database manager. Must be called after DatabaseConfig.init().
     */
    public static void init() {
        if (initialized) return;
        initialized = true;
        LOGGER.info("[Youer-DB] DatabaseManager initialized.");
    }

    /**
     * Get a database connection for the given feature module.
     * The connection type is determined by DatabaseConfig.getDatabaseType(module).
     *
     * @param module the feature module name (e.g. "items", "back", "warps")
     * @return a valid JDBC Connection
     */
    @NotNull
    public static Connection getConnection(@NotNull String module) {
        if (DatabaseConfig.isMysql(module)) {
            return getMysqlConnection();
        }
        return getSqliteConnection();
    }

    /**
     * Get the shared SQLite connection (single youer.db for all modules).
     */
    @NotNull
    static synchronized Connection getSqliteConnection() {
        if (sqliteConnection != null) {
            try {
                if (!sqliteConnection.isClosed()) {
                    return sqliteConnection;
                }
            } catch (SQLException ignored) {
                // stale, reconnect
            }
        }

        try {
            File dbDir = new File(DatabaseConfig.DB_DIR);
            if (!dbDir.exists()) {
                dbDir.mkdirs();
            }
            String dbPath = DatabaseConfig.DB_DIR + File.separator + "youer.db";
            sqliteConnection = DriverManager.getConnection("jdbc:sqlite:" + dbPath);

            try (Statement stmt = sqliteConnection.createStatement()) {
                stmt.execute("PRAGMA journal_mode=WAL");
                stmt.execute("PRAGMA synchronous=NORMAL");
                stmt.execute("PRAGMA busy_timeout=5000");
                stmt.execute("PRAGMA wal_autocheckpoint=100");
            }

            LOGGER.info("[Youer-DB] SQLite connection opened: " + dbPath);
            return sqliteConnection;
        } catch (SQLException e) {
            throw new RuntimeException("[Youer-DB] Failed to open SQLite connection", e);
        }
    }

    @NotNull
    static synchronized Connection getMysqlConnection() {
        if (mysqlConnection != null) {
            try {
                if (!mysqlConnection.isClosed() && mysqlConnection.isValid(3)) {
                    return mysqlConnection;
                }
            } catch (SQLException ignored) {
                // stale, reconnect
            }
        }

        try {
            String url = DatabaseConfig.getMysqlUrl();
            mysqlConnection = DriverManager.getConnection(
                    url,
                    DatabaseConfig.getMysqlUsername(),
                    DatabaseConfig.getMysqlPassword()
            );
            LOGGER.info("[Youer-DB] MySQL connection opened: " + url);
            return mysqlConnection;
        } catch (SQLException e) {
            throw new RuntimeException("[Youer-DB] Failed to open MySQL connection: " +
                    DatabaseConfig.getMysqlUrl(), e);
        }
    }

    /**
     * Close all database connections. Call on server shutdown.
     */
    public static void shutdown() {
        if (sqliteConnection != null) {
            try {
                if (!sqliteConnection.isClosed()) {
                    try (Statement stmt = sqliteConnection.createStatement()) {
                        stmt.execute("PRAGMA wal_checkpoint(TRUNCATE)");
                    }
                    sqliteConnection.close();
                    LOGGER.info("[Youer-DB] SQLite connection closed.");
                }
            } catch (SQLException e) {
                LOGGER.warn("[Youer-DB] Error closing SQLite connection: " + e.getMessage());
            }
            sqliteConnection = null;
        }

        if (mysqlConnection != null) {
            try {
                if (!mysqlConnection.isClosed()) {
                    mysqlConnection.close();
                    LOGGER.info("[Youer-DB] MySQL connection closed.");
                }
            } catch (SQLException e) {
                LOGGER.warn("[Youer-DB] Error closing MySQL connection: " + e.getMessage());
            }
            mysqlConnection = null;
        }

        initialized = false;
    }
}
