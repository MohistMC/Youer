package com.mohistmc.youer.feature.db;

import com.mohistmc.youer.feature.ban.BanType;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

/**
 * @author Mgazul by MohistMC
 * @date 2026/07/01
 *
 * Database-backed storage for bans — 8 independent tables, all with message column.
 * Each can be independently set to sqlite/mysql via database.yml features.bans.<type>.
 */
public class BanDatabaseStorage {

    private static final Logger LOGGER = LogManager.getLogger("Youer-DB");

    private static final String ITEM        = "bans.item";
    private static final String ITEM_MOSHOU = "bans.item_moshou";
    private static final String ENTITY      = "bans.entity";
    private static final String ENCHANTMENT = "bans.enchantment";
    private static final String RECIPE      = "bans.recipe";
    private static final String BLOCK       = "bans.block";
    private static final String WORLD       = "bans.world";
    private static final String STRUCTURE   = "bans.structure";
    private static final String EFFECT      = "bans.effect";
    private static final String NBT         = "bans.nbt";

    public void init() {
        initListTable(ITEM,        "ban_item");
        initListTable(ITEM_MOSHOU, "ban_item_moshou");
        initListTable(ENTITY,      "ban_entity");
        initListTable(ENCHANTMENT, "ban_enchantment");
        initListTable(RECIPE,      "ban_recipe");
        initListTable(BLOCK,       "ban_block");
        initListTable(WORLD,       "ban_world");
        initListTable(STRUCTURE,   "ban_structure");
        initListTable(EFFECT,      "ban_effect");
        initNbtTable();
    }

    private void initListTable(String module, String baseTable) {
        Connection conn = DatabaseManager.getConnection(module);
        try (Statement stmt = conn.createStatement()) {
            String table = tableName(module, baseTable);
            String ddl = isMysql(conn)
                ? "CREATE TABLE IF NOT EXISTS " + table + " (value VARCHAR(255) PRIMARY KEY, message TEXT NOT NULL) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4"
                : "CREATE TABLE IF NOT EXISTS " + table + " (value TEXT PRIMARY KEY, message TEXT NOT NULL DEFAULT '')";
            stmt.executeUpdate(ddl);
        } catch (SQLException e) {
            throw new RuntimeException("[Youer-DB] Failed to init " + baseTable, e);
        }
    }

    private void initNbtTable() {
        Connection conn = DatabaseManager.getConnection(NBT);
        try (Statement stmt = conn.createStatement()) {
            String table = tableName(NBT, "ban_nbt");
            String ddl = isMysql(conn)
                ? "CREATE TABLE IF NOT EXISTS " + table + " (item_type VARCHAR(255) NOT NULL, nbt_tag VARCHAR(255) NOT NULL, PRIMARY KEY (item_type, nbt_tag)) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4"
                : "CREATE TABLE IF NOT EXISTS " + table + " (item_type TEXT NOT NULL, nbt_tag TEXT NOT NULL, PRIMARY KEY (item_type, nbt_tag))";
            stmt.executeUpdate(ddl);
        } catch (SQLException e) {
            throw new RuntimeException("[Youer-DB] Failed to init ban_nbt", e);
        }
    }

    // === Helpers ===

    private static String tableName(String module, String baseName) {
        return DatabaseConfig.isMysql(module) ? DatabaseConfig.getMysqlTablePrefix() + "_" + baseName : baseName;
    }

    private static boolean isMysql(Connection conn) throws SQLException {
        return conn.getMetaData().getURL().contains("mysql");
    }

    private static String moduleFor(BanType type) {
        return switch (type) {
            case ITEM        -> ITEM;
            case ITEM_MOSHOU -> ITEM_MOSHOU;
            case ENTITY      -> ENTITY;
            case ENCHANTMENT -> ENCHANTMENT;
            case RECIPE      -> RECIPE;
            case BLOCK       -> BLOCK;
            case WORLD       -> WORLD;
            case STRUCTURE   -> STRUCTURE;
            case EFFECT      -> EFFECT;
        };
    }

    private static String tableBase(BanType type) {
        return switch (type) {
            case ITEM        -> "ban_item";
            case ITEM_MOSHOU -> "ban_item_moshou";
            case ENTITY      -> "ban_entity";
            case ENCHANTMENT -> "ban_enchantment";
            case RECIPE      -> "ban_recipe";
            case BLOCK       -> "ban_block";
            case WORLD       -> "ban_world";
            case STRUCTURE   -> "ban_structure";
            case EFFECT      -> "ban_effect";
        };
    }

    // === Ban list operations ===

    public List<String> getList(BanType type) {
        String module = moduleFor(type);
        String table = tableName(module, tableBase(type));
        List<String> list = new ArrayList<>();
        Connection conn = DatabaseManager.getConnection(module);
        try (Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery("SELECT value FROM " + table)) {
            while (rs.next()) list.add(rs.getString("value"));
        } catch (SQLException e) {
            LOGGER.warn("[Youer-DB] Failed to get list '{}': {}", table, e.getMessage());
        }
        return list;
    }

    public void setList(BanType type, List<String> values) {
        String module = moduleFor(type);
        String table = tableName(module, tableBase(type));
        Connection conn = DatabaseManager.getConnection(module);
        try {
            if (values == null || values.isEmpty()) {
                try (Statement stmt = conn.createStatement()) {
                    stmt.executeUpdate("DELETE FROM " + table);
                }
                return;
            }
            // Insert new values (IGNORE existing — preserves messages)
            String insertSql = isMysql(conn)
                ? "INSERT IGNORE INTO " + table + " (value) VALUES (?)"
                : "INSERT OR IGNORE INTO " + table + " (value) VALUES (?)";
            try (PreparedStatement ps = conn.prepareStatement(insertSql)) {
                for (String v : values) {
                    ps.setString(1, v);
                    ps.addBatch();
                }
                ps.executeBatch();
            }
            // Remove values no longer in the list
            StringBuilder sb = new StringBuilder("DELETE FROM " + table + " WHERE value NOT IN (");
            for (int i = 0; i < values.size(); i++) sb.append(i == 0 ? "?" : ",?");
            sb.append(")");
            try (PreparedStatement ps = conn.prepareStatement(sb.toString())) {
                for (int i = 0; i < values.size(); i++) ps.setString(i + 1, values.get(i));
                ps.executeUpdate();
            }
        } catch (SQLException e) {
            LOGGER.warn("[Youer-DB] Failed to set list '{}': {}", table, e.getMessage());
        }
    }

    public boolean has(BanType type, String value) {
        String module = moduleFor(type);
        String table = tableName(module, tableBase(type));
        Connection conn = DatabaseManager.getConnection(module);
        try (PreparedStatement ps = conn.prepareStatement(
                "SELECT 1 FROM " + table + " WHERE value = ?")) {
            ps.setString(1, value);
            try (ResultSet rs = ps.executeQuery()) {
                return rs.next();
            }
        } catch (SQLException e) {
            return false;
        }
    }

    // === Ban messages (unified — works for all list types) ===

    public void setMessage(BanType type, String key, String message) {
        String module = moduleFor(type);
        String table = tableName(module, tableBase(type));
        Connection conn = DatabaseManager.getConnection(module);
        try (PreparedStatement ps = conn.prepareStatement(
                "UPDATE " + table + " SET message = ? WHERE value = ?")) {
            ps.setString(1, message);
            ps.setString(2, key);
            ps.executeUpdate();
        } catch (SQLException e) {
            LOGGER.warn("[Youer-DB] Failed to set message '{}': {}", key, e.getMessage());
        }
    }

    public String getMessage(BanType type, String key) {
        String module = moduleFor(type);
        String table = tableName(module, tableBase(type));
        Connection conn = DatabaseManager.getConnection(module);
        try (PreparedStatement ps = conn.prepareStatement(
                "SELECT message FROM " + table + " WHERE value = ?")) {
            ps.setString(1, key);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    String msg = rs.getString("message");
                    return msg != null ? msg : "";
                }
            }
        } catch (SQLException e) {
            LOGGER.warn("[Youer-DB] Failed to get message '{}': {}", key, e.getMessage());
        }
        return "";
    }

    // === NBT bans ===

    public Set<String> getNbtKeys() {
        Set<String> keys = new HashSet<>();
        String table = tableName(NBT, "ban_nbt");
        Connection conn = DatabaseManager.getConnection(NBT);
        try (Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery("SELECT DISTINCT item_type FROM " + table)) {
            while (rs.next()) keys.add(rs.getString("item_type"));
        } catch (SQLException e) {
            LOGGER.warn("[Youer-DB] Failed to get NBT keys: {}", e.getMessage());
        }
        return keys;
    }

    public List<String> getNbtList(String itemType) {
        List<String> list = new ArrayList<>();
        String table = tableName(NBT, "ban_nbt");
        Connection conn = DatabaseManager.getConnection(NBT);
        try (PreparedStatement ps = conn.prepareStatement(
                "SELECT nbt_tag FROM " + table + " WHERE item_type = ?")) {
            ps.setString(1, itemType);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) list.add(rs.getString("nbt_tag"));
            }
        } catch (SQLException e) {
            LOGGER.warn("[Youer-DB] Failed to get NBT list '{}': {}", itemType, e.getMessage());
        }
        return list;
    }

    public boolean hasNbtKey(String itemType) {
        String table = tableName(NBT, "ban_nbt");
        Connection conn = DatabaseManager.getConnection(NBT);
        try (PreparedStatement ps = conn.prepareStatement(
                "SELECT 1 FROM " + table + " WHERE item_type = ?")) {
            ps.setString(1, itemType);
            try (ResultSet rs = ps.executeQuery()) {
                return rs.next();
            }
        } catch (SQLException e) {
            return false;
        }
    }

    public void addNbt(String itemType, String nbtTag) {
        String table = tableName(NBT, "ban_nbt");
        Connection conn = DatabaseManager.getConnection(NBT);
        try (PreparedStatement ps = conn.prepareStatement(
                "INSERT OR IGNORE INTO " + table + " (item_type, nbt_tag) VALUES (?, ?)")) {
            ps.setString(1, itemType);
            ps.setString(2, nbtTag);
            ps.executeUpdate();
        } catch (SQLException e) {
            LOGGER.warn("[Youer-DB] Failed to add NBT '{}': {}", itemType, e.getMessage());
        }
    }

    public void removeNbt(String itemType, String nbtTag) {
        String table = tableName(NBT, "ban_nbt");
        Connection conn = DatabaseManager.getConnection(NBT);
        try (PreparedStatement ps = conn.prepareStatement(
                "DELETE FROM " + table + " WHERE item_type = ? AND nbt_tag = ?")) {
            ps.setString(1, itemType);
            ps.setString(2, nbtTag);
            ps.executeUpdate();
        } catch (SQLException e) {
            LOGGER.warn("[Youer-DB] Failed to remove NBT '{}': {}", itemType, e.getMessage());
        }
    }

    public void clearNbt(String itemType) {
        String table = tableName(NBT, "ban_nbt");
        Connection conn = DatabaseManager.getConnection(NBT);
        try (PreparedStatement ps = conn.prepareStatement(
                "DELETE FROM " + table + " WHERE item_type = ?")) {
            ps.setString(1, itemType);
            ps.executeUpdate();
        } catch (SQLException e) {
            LOGGER.warn("[Youer-DB] Failed to clear NBT '{}': {}", itemType, e.getMessage());
        }
    }
}
