package com.mohistmc.youer.feature.db;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.ResultSetMetaData;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

/**
 * @author Mgazul by MohistMC
 * @date 2026/07/03
 * <p>
 * Migrates data between SQLite and MySQL for individual feature tables.
 * Each table can be migrated independently while the server is running.
 */
public class DatabaseMigration {

    private static final Logger LOGGER = LogManager.getLogger("Youer-DB");

    // Module name -> base table name (insertion-ordered)
    private static final Map<String, String> MODULE_TABLES = new LinkedHashMap<>();

    // Module name -> display name for user feedback
    private static final Map<String, String> MODULE_DISPLAY = new LinkedHashMap<>();

    static {
        MODULE_TABLES.put("items", "items");
        MODULE_TABLES.put("warps", "warps");
        MODULE_TABLES.put("entity_limits", "entity_limits");
        MODULE_TABLES.put("back", "back");
        MODULE_TABLES.put("bans.item", "ban_item");
        MODULE_TABLES.put("bans.item_moshou", "ban_item_moshou");
        MODULE_TABLES.put("bans.entity", "ban_entity");
        MODULE_TABLES.put("bans.enchantment", "ban_enchantment");
        MODULE_TABLES.put("bans.recipe", "ban_recipe");
        MODULE_TABLES.put("bans.block", "ban_block");
        MODULE_TABLES.put("bans.world", "ban_world");
        MODULE_TABLES.put("bans.nbt", "ban_nbt");

        MODULE_DISPLAY.put("items", "items");
        MODULE_DISPLAY.put("warps", "warps");
        MODULE_DISPLAY.put("entity_limits", "entity limits");
        MODULE_DISPLAY.put("back", "back locations");
        MODULE_DISPLAY.put("bans.item", "bans (item)");
        MODULE_DISPLAY.put("bans.item_moshou", "bans (item_moshou)");
        MODULE_DISPLAY.put("bans.entity", "bans (entity)");
        MODULE_DISPLAY.put("bans.enchantment", "bans (enchantment)");
        MODULE_DISPLAY.put("bans.recipe", "bans (recipe)");
        MODULE_DISPLAY.put("bans.block", "bans (block)");
        MODULE_DISPLAY.put("bans.world", "bans (world)");
        MODULE_DISPLAY.put("bans.nbt", "bans (NBT)");
    }

    /** All known module names (for tab-complete / validation). */
    public static Set<String> getModuleNames() {
        return MODULE_TABLES.keySet();
    }

    /** Human-readable display name for a module. */
    public static String getDisplayName(String module) {
        return MODULE_DISPLAY.getOrDefault(module, module);
    }

    /**
     * Result of a single migration operation.
     */
    public record MigrationResult(boolean success, String message, int rows) {}

    /**
     * Migrate one module's data from its current database type to the target type.
     * <p>
     * <b>Important:</b> migration is <em>copy-only</em> — source data is preserved.
     * After a successful migration, manually update {@code database.yml}
     * and restart the server to switch to the new backend.
     *
     * @param module     feature module name (e.g. "items", "bans.item")
     * @param targetType "sqlite" or "mysql"
     * @return MigrationResult
     */
    public static MigrationResult migrate(String module, String targetType) {
        String baseTable = MODULE_TABLES.get(module);
        if (baseTable == null) {
            return new MigrationResult(false, "Unknown module: " + module, 0);
        }

        String target = targetType.toLowerCase(Locale.ROOT);
        if (!target.equals("sqlite") && !target.equals("mysql")) {
            return new MigrationResult(false, "Target type must be 'sqlite' or 'mysql'", 0);
        }

        // Determine current type
        String current;
        if ("back".equals(module)) {
            current = "sqlite"; // back is always SQLite
        } else {
            String configured = DatabaseConfig.getDatabaseType(module);
            current = (configured != null) ? configured.toLowerCase(Locale.ROOT) : "sqlite";
        }

        if (current.equals(target)) {
            return new MigrationResult(false,
                    "Source and target are both " + current + " for " + getDisplayName(module), 0);
        }

        // Validate both connections are reachable before starting
        MigrationResult srcCheck = checkConnection(current, "source");
        if (!srcCheck.success()) return srcCheck;
        MigrationResult tgtCheck = checkConnection(target, "target");
        if (!tgtCheck.success()) return tgtCheck;

        try {
            Connection srcConn = current.equals("mysql")
                    ? DatabaseManager.getMysqlConnection()
                    : DatabaseManager.getSqliteConnection();
            Connection tgtConn = target.equals("mysql")
                    ? DatabaseManager.getMysqlConnection()
                    : DatabaseManager.getSqliteConnection();

            String srcTable = tableName(module, baseTable, current);
            String tgtTable = tableName(module, baseTable, target);

            // Create target table if it doesn't exist
            createTargetTable(baseTable, tgtConn, tgtTable, target.equals("mysql"));

            // Transfer data
            int rows = transferData(srcConn, tgtConn, srcTable, tgtTable);
            return new MigrationResult(true, "Migrated " + rows + " rows", rows);

        } catch (Exception e) { // catches both SQLException and RuntimeException
            LOGGER.error("Migration failed for {}: {}", module, e.getMessage(), e);
            return new MigrationResult(false, "Error: " + e.getMessage(), 0);
        }
    }

    /**
     * Verify that a connection to the given database type can be established.
     */
    private static MigrationResult checkConnection(String dbType, String label) {
        try {
            if (dbType.equals("mysql")) {
                Connection c = DatabaseManager.getMysqlConnection();
                if (c == null || !c.isValid(5)) {
                    return new MigrationResult(false, "MySQL " + label + " connection is not valid. Check database.yml", 0);
                }
            } else {
                Connection c = DatabaseManager.getSqliteConnection();
                if (c == null || c.isClosed()) {
                    return new MigrationResult(false, "SQLite " + label + " connection is not valid", 0);
                }
            }
            return new MigrationResult(true, null, 0);
        } catch (Exception e) {
            String detail = e.getMessage() != null ? e.getMessage() : e.getClass().getSimpleName();
            return new MigrationResult(false,
                    "Failed to connect to " + dbType + " (" + label + "): " + detail, 0);
        }
    }

    // ── helpers ──────────────────────────────────────────────────────────

    private static String tableName(String module, String baseTable, String dbType) {
        if (dbType.equals("mysql")) {
            return DatabaseConfig.getMysqlTablePrefix() + "_" + baseTable;
        }
        return baseTable;
    }

    private static void createTargetTable(String baseTable, Connection conn,
                                           String tgtTable, boolean isMysql) throws SQLException {
        String ddl;
        switch (baseTable) {
            case "items" -> ddl = isMysql
                    ? "CREATE TABLE IF NOT EXISTS " + tgtTable + " (name VARCHAR(255) PRIMARY KEY, data MEDIUMBLOB NOT NULL) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4"
                    : "CREATE TABLE IF NOT EXISTS " + tgtTable + " (name TEXT PRIMARY KEY, data BLOB NOT NULL)";
            case "warps" -> ddl = isMysql
                    ? "CREATE TABLE IF NOT EXISTS " + tgtTable + " (name VARCHAR(255) PRIMARY KEY, world VARCHAR(255) NOT NULL, x DOUBLE NOT NULL, y DOUBLE NOT NULL, z DOUBLE NOT NULL, pitch FLOAT NOT NULL, yaw FLOAT NOT NULL) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4"
                    : "CREATE TABLE IF NOT EXISTS " + tgtTable + " (name TEXT PRIMARY KEY, world TEXT NOT NULL, x REAL NOT NULL, y REAL NOT NULL, z REAL NOT NULL, pitch REAL NOT NULL, yaw REAL NOT NULL)";
            case "entity_limits" -> ddl = isMysql
                    ? "CREATE TABLE IF NOT EXISTS " + tgtTable + " (world VARCHAR(255) NOT NULL, entity_type VARCHAR(255) NOT NULL, limit_count INT NOT NULL, PRIMARY KEY (world, entity_type)) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4"
                    : "CREATE TABLE IF NOT EXISTS " + tgtTable + " (world TEXT NOT NULL, entity_type TEXT NOT NULL, limit_count INT NOT NULL, PRIMARY KEY (world, entity_type))";
            case "back" -> ddl = isMysql
                    ? "CREATE TABLE IF NOT EXISTS " + tgtTable + " (player_uuid VARCHAR(36) PRIMARY KEY, world VARCHAR(255) NOT NULL, x DOUBLE NOT NULL, y DOUBLE NOT NULL, z DOUBLE NOT NULL, pitch FLOAT NOT NULL, yaw FLOAT NOT NULL, back_type VARCHAR(64) NOT NULL) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4"
                    : "CREATE TABLE IF NOT EXISTS " + tgtTable + " (player_uuid TEXT PRIMARY KEY, world TEXT NOT NULL, x REAL NOT NULL, y REAL NOT NULL, z REAL NOT NULL, pitch REAL NOT NULL, yaw REAL NOT NULL, back_type TEXT NOT NULL)";
            case "ban_nbt" -> ddl = isMysql
                    ? "CREATE TABLE IF NOT EXISTS " + tgtTable + " (item_type VARCHAR(255) NOT NULL, nbt_tag VARCHAR(255) NOT NULL, PRIMARY KEY (item_type, nbt_tag)) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4"
                    : "CREATE TABLE IF NOT EXISTS " + tgtTable + " (item_type TEXT NOT NULL, nbt_tag TEXT NOT NULL, PRIMARY KEY (item_type, nbt_tag))";
            default ->
                // ban_item, ban_item_moshou, ban_entity, ban_enchantment,
                // ban_recipe, ban_block, ban_world
                ddl = isMysql
                        ? "CREATE TABLE IF NOT EXISTS " + tgtTable + " (value VARCHAR(255) PRIMARY KEY, message TEXT NOT NULL) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4"
                        : "CREATE TABLE IF NOT EXISTS " + tgtTable + " (value TEXT PRIMARY KEY, message TEXT NOT NULL DEFAULT '')";
        }

        try (Statement stmt = conn.createStatement()) {
            stmt.executeUpdate(ddl);
        }
    }

    /**
     * Transfer all rows from source table to target table using generic JDBC.
     * Works for any schema — uses ResultSetMetaData for column discovery.
     */
    private static int transferData(Connection src, Connection tgt,
                                     String srcTable, String tgtTable) throws SQLException {
        // ── read all rows from source ──
        List<Object[]> rows = new ArrayList<>();
        List<Boolean> isBlob = new ArrayList<>();
        int colCount;

        try (Statement stmt = src.createStatement();
             ResultSet rs = stmt.executeQuery("SELECT * FROM " + srcTable)) {
            ResultSetMetaData meta = rs.getMetaData();
            colCount = meta.getColumnCount();

            for (int i = 1; i <= colCount; i++) {
                String typeName = meta.getColumnTypeName(i);
                isBlob.add(typeName != null && typeName.toUpperCase(Locale.ROOT).contains("BLOB"));
            }

            while (rs.next()) {
                Object[] row = new Object[colCount];
                for (int i = 1; i <= colCount; i++) {
                    if (isBlob.get(i - 1)) {
                        row[i - 1] = rs.getBytes(i);
                    } else {
                        row[i - 1] = rs.getObject(i);
                    }
                }
                rows.add(row);
            }
        }

        if (rows.isEmpty()) {
            return 0;
        }

        // ── write all rows to target (REPLACE works on both MySQL and SQLite) ──
        StringBuilder sql = new StringBuilder("REPLACE INTO ");
        sql.append(tgtTable).append(" VALUES (");
        for (int i = 0; i < colCount; i++) {
            sql.append(i > 0 ? ",?" : "?");
        }
        sql.append(")");

        try (PreparedStatement ps = tgt.prepareStatement(sql.toString())) {
            for (Object[] row : rows) {
                for (int i = 0; i < colCount; i++) {
                    if (row[i] instanceof byte[]) {
                        ps.setBytes(i + 1, (byte[]) row[i]);
                    } else {
                        ps.setObject(i + 1, row[i]);
                    }
                }
                ps.addBatch();
            }
            ps.executeBatch();
        }

        return rows.size();
    }
}
