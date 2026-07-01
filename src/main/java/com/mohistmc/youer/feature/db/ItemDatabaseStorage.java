package com.mohistmc.youer.feature.db;

import com.github.luben.zstd.Zstd;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.List;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.bukkit.Material;
import org.bukkit.inventory.ItemStack;

/**
 * @author Mgazul by MohistMC
 * @date 2026/07/01
 *
 * Database-backed storage for saved items.
 * Both SQLite and MySQL store Zstd-compressed binary data:
 * - SQLite: BLOB column
 * - MySQL:  MEDIUMBLOB column
 *
 * Table: "items" (SQLite) or "youer_items" (MySQL)
 */
public class ItemDatabaseStorage {

    private static final Logger LOGGER = LogManager.getLogger("Youer-DB");
    private static final String MODULE = "items";
    private final boolean isMysql;
    private final String tableName;

    public ItemDatabaseStorage() {
        this.isMysql = DatabaseConfig.isMysql(MODULE);
        this.tableName = isMysql ? DatabaseConfig.getMysqlTablePrefix() + "_items" : "items";
    }

    /**
     * Initialize the storage: create table if not exists.
     */
    public void init() {
        Connection conn = DatabaseManager.getConnection(MODULE);
        try (Statement stmt = conn.createStatement()) {
            if (isMysql) {
                stmt.executeUpdate(
                    "CREATE TABLE IF NOT EXISTS " + tableName + " (" +
                    "name VARCHAR(255) PRIMARY KEY, " +
                    "data MEDIUMBLOB NOT NULL" +
                    ") ENGINE=InnoDB DEFAULT CHARSET=utf8mb4"
                );
            } else {
                stmt.executeUpdate(
                    "CREATE TABLE IF NOT EXISTS " + tableName + " (" +
                    "name TEXT PRIMARY KEY, " +
                    "data BLOB NOT NULL" +
                    ")"
                );
            }
        } catch (SQLException e) {
            throw new RuntimeException("[Youer-DB] Failed to initialize item storage table", e);
        }
    }

    /**
     * Save an ItemStack with the given key.
     * serializeAsBytes → Zstd compress → store raw bytes.
     */
    public void put(String key, ItemStack item) {
        byte[] raw = item.serializeAsBytes();
        byte[] compressed = Zstd.compress(raw);
        Connection conn = DatabaseManager.getConnection(MODULE);

        try (PreparedStatement ps = conn.prepareStatement(
                isMysql
                    ? "REPLACE INTO " + tableName + " (name, data) VALUES (?, ?)"
                    : "INSERT OR REPLACE INTO " + tableName + " (name, data) VALUES (?, ?)")) {
            ps.setString(1, key);
            ps.setBytes(2, compressed);
            ps.executeUpdate();
        } catch (SQLException e) {
            LOGGER.warn("[Youer-DB] Failed to save item '{}': {}", key, e.getMessage());
        }
    }

    /**
     * Retrieve an ItemStack by key.
     *
     * @param key the item name
     * @return the ItemStack, or AIR if not found
     */
    public ItemStack get(String key) {
        Connection conn = DatabaseManager.getConnection(MODULE);
        try (PreparedStatement ps = conn.prepareStatement(
                "SELECT data FROM " + tableName + " WHERE name = ?")) {
            ps.setString(1, key);

            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    byte[] compressed = rs.getBytes("data");
                    byte[] raw = Zstd.decompress(compressed, (int) Zstd.decompressedSize(compressed));
                    return ItemStack.deserializeBytes(raw);
                }
            }
        } catch (SQLException e) {
            LOGGER.warn("[Youer-DB] Failed to get item '{}': {}", key, e.getMessage());
        }
        return new ItemStack(Material.AIR);
    }

    /**
     * Remove an item by key.
     */
    public void remove(String key) {
        Connection conn = DatabaseManager.getConnection(MODULE);
        try (PreparedStatement ps = conn.prepareStatement(
                "DELETE FROM " + tableName + " WHERE name = ?")) {
            ps.setString(1, key);
            ps.executeUpdate();
        } catch (SQLException e) {
            LOGGER.warn("[Youer-DB] Failed to remove item '" + key + "': " + e.getMessage());
        }
    }

    /**
     * @return list of all saved item keys
     */
    public List<String> getAllKeys() {
        List<String> keys = new ArrayList<>();
        Connection conn = DatabaseManager.getConnection(MODULE);
        try (Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery("SELECT name FROM " + tableName)) {
            while (rs.next()) {
                keys.add(rs.getString("name"));
            }
        } catch (SQLException e) {
            LOGGER.warn("[Youer-DB] Failed to list item keys: {}", e.getMessage());
        }
        return keys;
    }

    /**
     * @return list of all saved ItemStacks
     */
    public List<ItemStack> getAllItems() {
        List<ItemStack> items = new ArrayList<>();
        Connection conn = DatabaseManager.getConnection(MODULE);
        try (Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery("SELECT name, data FROM " + tableName)) {
            while (rs.next()) {
                try {
                    byte[] compressed = rs.getBytes("data");
                    byte[] raw = Zstd.decompress(compressed, (int) Zstd.decompressedSize(compressed));
                    items.add(ItemStack.deserializeBytes(raw));
                } catch (Exception e) {
                    LOGGER.warn("[Youer-DB] Failed to deserialize item '{}': {}", rs.getString("name"), e.getMessage());
                }
            }
        } catch (SQLException e) {
            LOGGER.warn("[Youer-DB] Failed to list items: {}", e.getMessage());
        }
        return items;
    }
}
