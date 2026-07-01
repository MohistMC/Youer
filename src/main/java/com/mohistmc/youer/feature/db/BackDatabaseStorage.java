package com.mohistmc.youer.feature.db;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.World;

/**
 * @author Mgazul by MohistMC
 * @date 2026/07/01
 *
 * SQLite-only storage for player back locations (teleport/death).
 * Simple structured data — no Zstd needed, stored as normal columns.
 */
public class BackDatabaseStorage {

    private static final Logger LOGGER = LogManager.getLogger("Youer-DB");

    public void init() {
        Connection conn = DatabaseManager.getSqliteConnection();
        try (Statement stmt = conn.createStatement()) {
            stmt.executeUpdate(
                "CREATE TABLE IF NOT EXISTS back (" +
                "player_uuid TEXT PRIMARY KEY, " +
                "world TEXT NOT NULL, " +
                "x REAL NOT NULL, " +
                "y REAL NOT NULL, " +
                "z REAL NOT NULL, " +
                "pitch REAL NOT NULL, " +
                "yaw REAL NOT NULL, " +
                "back_type TEXT NOT NULL" +
                ")"
            );
            LOGGER.info("[Youer-DB] Back location table ready (SQLite)");
        } catch (SQLException e) {
            throw new RuntimeException("[Youer-DB] Failed to initialize back location table", e);
        }
    }

    public void saveLocation(String uuid, String worldName, double x, double y, double z,
                             float pitch, float yaw, String backType) {
        Connection conn = DatabaseManager.getSqliteConnection();
        try (PreparedStatement ps = conn.prepareStatement(
                "INSERT OR REPLACE INTO back (player_uuid, world, x, y, z, pitch, yaw, back_type) " +
                "VALUES (?, ?, ?, ?, ?, ?, ?, ?)")) {
            ps.setString(1, uuid);
            ps.setString(2, worldName);
            ps.setDouble(3, x);
            ps.setDouble(4, y);
            ps.setDouble(5, z);
            ps.setFloat(6, pitch);
            ps.setFloat(7, yaw);
            ps.setString(8, backType);
            ps.executeUpdate();
        } catch (SQLException e) {
            LOGGER.warn("[Youer-DB] Failed to save back location for '{}': {}", uuid, e.getMessage());
        }
    }

    public Location getLocation(String uuid) {
        Connection conn = DatabaseManager.getSqliteConnection();
        try (PreparedStatement ps = conn.prepareStatement(
                "SELECT world, x, y, z, pitch, yaw FROM back WHERE player_uuid = ?")) {
            ps.setString(1, uuid);
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
            LOGGER.warn("[Youer-DB] Failed to get back location for '{}': {}", uuid, e.getMessage());
        }
        return null;
    }

    public String getBackType(String uuid) {
        Connection conn = DatabaseManager.getSqliteConnection();
        try (PreparedStatement ps = conn.prepareStatement(
                "SELECT back_type FROM back WHERE player_uuid = ?")) {
            ps.setString(1, uuid);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    return rs.getString("back_type");
                }
            }
        } catch (SQLException e) {
            LOGGER.warn("[Youer-DB] Failed to get back type for '{}': {}", uuid, e.getMessage());
        }
        return null;
    }

    public boolean has(String uuid) {
        Connection conn = DatabaseManager.getSqliteConnection();
        try (PreparedStatement ps = conn.prepareStatement(
                "SELECT 1 FROM back WHERE player_uuid = ?")) {
            ps.setString(1, uuid);
            try (ResultSet rs = ps.executeQuery()) {
                return rs.next();
            }
        } catch (SQLException e) {
            return false;
        }
    }
}
