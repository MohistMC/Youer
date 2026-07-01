package com.mohistmc.youer.feature.db;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.HashSet;
import java.util.Set;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.World;

/**
 * @author Mgazul by MohistMC
 * @date 2026/07/02
 *
 * Database-backed storage for warps.
 * Table: warps (name, world, x, y, z, pitch, yaw)
 */
public class WarpsDatabaseStorage {

    private static final Logger LOGGER = LogManager.getLogger("Youer-DB");
    private static final String MODULE = "warps";
    private final String tableName;

    public WarpsDatabaseStorage() {
        this.tableName = DatabaseConfig.isMysql(MODULE)
                ? DatabaseConfig.getMysqlTablePrefix() + "_warps" : "warps";
    }

    public void init() {
        Connection conn = DatabaseManager.getConnection(MODULE);
        try (Statement stmt = conn.createStatement()) {
            String ddl = "CREATE TABLE IF NOT EXISTS " + tableName + " ("
                    + "name TEXT PRIMARY KEY, world TEXT NOT NULL, "
                    + "x REAL NOT NULL, y REAL NOT NULL, z REAL NOT NULL, "
                    + "pitch REAL NOT NULL, yaw REAL NOT NULL)";
            if (isMysql(conn)) {
                ddl = "CREATE TABLE IF NOT EXISTS " + tableName + " ("
                        + "name VARCHAR(255) PRIMARY KEY, world VARCHAR(255) NOT NULL, "
                        + "x DOUBLE NOT NULL, y DOUBLE NOT NULL, z DOUBLE NOT NULL, "
                        + "pitch FLOAT NOT NULL, yaw FLOAT NOT NULL) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4";
            }
            stmt.executeUpdate(ddl);
        } catch (SQLException e) {
            throw new RuntimeException("[Youer-DB] Failed to init warps table", e);
        }
    }

    public void set(String name, Location loc) {
        Connection conn = DatabaseManager.getConnection(MODULE);
        try (PreparedStatement ps = conn.prepareStatement(
                "INSERT OR REPLACE INTO " + tableName + " (name, world, x, y, z, pitch, yaw) VALUES (?, ?, ?, ?, ?, ?, ?)")) {
            ps.setString(1, name);
            ps.setString(2, loc.getWorld().getName());
            ps.setDouble(3, loc.getX());
            ps.setDouble(4, loc.getY());
            ps.setDouble(5, loc.getZ());
            ps.setFloat(6, loc.getPitch());
            ps.setFloat(7, loc.getYaw());
            ps.executeUpdate();
        } catch (SQLException e) {
            LOGGER.warn("[Youer-DB] Failed to set warp '{}': {}", name, e.getMessage());
        }
    }

    public Location get(String name) {
        Connection conn = DatabaseManager.getConnection(MODULE);
        try (PreparedStatement ps = conn.prepareStatement(
                "SELECT world, x, y, z, pitch, yaw FROM " + tableName + " WHERE name = ?")) {
            ps.setString(1, name);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    World world = Bukkit.getWorld(rs.getString("world"));
                    if (world == null) return null;
                    return new Location(world,
                            rs.getDouble("x"), rs.getDouble("y"), rs.getDouble("z"),
                            rs.getFloat("yaw"), rs.getFloat("pitch"));
                }
            }
        } catch (SQLException e) {
            LOGGER.warn("[Youer-DB] Failed to get warp '{}': {}", name, e.getMessage());
        }
        return null;
    }

    public void remove(String name) {
        Connection conn = DatabaseManager.getConnection(MODULE);
        try (PreparedStatement ps = conn.prepareStatement(
                "DELETE FROM " + tableName + " WHERE name = ?")) {
            ps.setString(1, name);
            ps.executeUpdate();
        } catch (SQLException e) {
            LOGGER.warn("[Youer-DB] Failed to remove warp '{}': {}", name, e.getMessage());
        }
    }

    public boolean has(String name) {
        Connection conn = DatabaseManager.getConnection(MODULE);
        try (PreparedStatement ps = conn.prepareStatement(
                "SELECT 1 FROM " + tableName + " WHERE name = ?")) {
            ps.setString(1, name);
            try (ResultSet rs = ps.executeQuery()) {
                return rs.next();
            }
        } catch (SQLException e) {
            return false;
        }
    }

    public Set<String> getAllNames() {
        Set<String> names = new HashSet<>();
        Connection conn = DatabaseManager.getConnection(MODULE);
        try (Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery("SELECT name FROM " + tableName)) {
            while (rs.next()) names.add(rs.getString("name"));
        } catch (SQLException e) {
            LOGGER.warn("[Youer-DB] Failed to get warp names: {}", e.getMessage());
        }
        return names;
    }

    private static boolean isMysql(Connection conn) throws SQLException {
        return conn.getMetaData().getURL().contains("mysql");
    }
}
