package com.mohistmc.youer.feature.db;

import com.mohistmc.youer.feature.entitylimits.EntityLimits;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.List;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

/**
 * @author Mgazul by MohistMC
 * @date 2026/07/02
 *
 * Database-backed storage for entity spawn limits per world.
 * Table: entity_limits (world, entity_type, limit)
 */
public class EntityLimitsDatabaseStorage {

    private static final Logger LOGGER = LogManager.getLogger("Youer-DB");
    private static final String MODULE = "entity_limits";
    private final String tableName;

    public EntityLimitsDatabaseStorage() {
        this.tableName = DatabaseConfig.isMysql(MODULE)
                ? DatabaseConfig.getMysqlTablePrefix() + "_entity_limits" : "entity_limits";
    }

    public void init() {
        Connection conn = DatabaseManager.getConnection(MODULE);
        try (Statement stmt = conn.createStatement()) {
            String ddl = "CREATE TABLE IF NOT EXISTS " + tableName + " ("
                    + "world TEXT NOT NULL, entity_type TEXT NOT NULL, limit_count INT NOT NULL, "
                    + "PRIMARY KEY (world, entity_type))";
            if (isMysql(conn)) {
                ddl = "CREATE TABLE IF NOT EXISTS " + tableName + " ("
                        + "world VARCHAR(255) NOT NULL, entity_type VARCHAR(255) NOT NULL, limit_count INT NOT NULL, "
                        + "PRIMARY KEY (world, entity_type)) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4";
            }
            stmt.executeUpdate(ddl);
        } catch (SQLException e) {
            throw new RuntimeException("[Youer-DB] Failed to init entity_limits table", e);
        }
    }

    public boolean hasWorld(String worldName) {
        Connection conn = DatabaseManager.getConnection(MODULE);
        try (PreparedStatement ps = conn.prepareStatement(
                "SELECT 1 FROM " + tableName + " WHERE world = ?")) {
            ps.setString(1, worldName);
            try (ResultSet rs = ps.executeQuery()) {
                return rs.next();
            }
        } catch (SQLException e) {
            return false;
        }
    }

    public List<EntityLimits> getAll() {
        List<EntityLimits> list = new ArrayList<>();
        Connection conn = DatabaseManager.getConnection(MODULE);
        try (Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery("SELECT world, entity_type, limit_count FROM " + tableName)) {
            while (rs.next()) {
                list.add(new EntityLimits(
                        rs.getString("world"), rs.getString("entity_type"), rs.getInt("limit_count")));
            }
        } catch (SQLException e) {
            LOGGER.warn("[Youer-DB] Failed to get entity limits: {}", e.getMessage());
        }
        return list;
    }

    public void set(String worldName, String entityName, int limit) {
        Connection conn = DatabaseManager.getConnection(MODULE);
        try (PreparedStatement ps = conn.prepareStatement(
                "INSERT OR REPLACE INTO " + tableName + " (world, entity_type, limit_count) VALUES (?, ?, ?)")) {
            ps.setString(1, worldName);
            ps.setString(2, entityName);
            ps.setInt(3, limit);
            ps.executeUpdate();
        } catch (SQLException e) {
            LOGGER.warn("[Youer-DB] Failed to set entity limit: {}", e.getMessage());
        }
    }

    public void remove(String worldName, String entityName) {
        Connection conn = DatabaseManager.getConnection(MODULE);
        try (PreparedStatement ps = conn.prepareStatement(
                "DELETE FROM " + tableName + " WHERE world = ? AND entity_type = ?")) {
            ps.setString(1, worldName);
            ps.setString(2, entityName);
            ps.executeUpdate();
        } catch (SQLException e) {
            LOGGER.warn("[Youer-DB] Failed to remove entity limit: {}", e.getMessage());
        }
    }

    public int getLimit(String worldName, String entityName) {
        Connection conn = DatabaseManager.getConnection(MODULE);
        try (PreparedStatement ps = conn.prepareStatement(
                "SELECT limit_count FROM " + tableName + " WHERE world = ? AND entity_type = ?")) {
            ps.setString(1, worldName);
            ps.setString(2, entityName);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) return rs.getInt("limit_count");
            }
        } catch (SQLException e) {
            LOGGER.warn("[Youer-DB] Failed to get entity limit: {}", e.getMessage());
        }
        return -1;
    }

    private static boolean isMysql(Connection conn) throws SQLException {
        return conn.getMetaData().getURL().contains("mysql");
    }
}
