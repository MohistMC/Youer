package com.mohistmc.youer.feature.db;

import com.mohistmc.youer.util.YamlUtils;
import java.io.File;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.Locale;
import org.bukkit.configuration.file.YamlConfiguration;

/**
 * @author Mgazul by MohistMC
 * @date 2026/07/01
 *
 * Reads youer-config/database.yml and provides modular database type switches.
 * Each feature can independently choose "sqlite" (default) or "mysql".
 */
public class DatabaseConfig {

    public static final String DB_DIR = "youer-config" + File.separator + "database";
    private static File configFile;
    private static YamlConfiguration yaml;

    public static void init() {
        configFile = new File("youer-config", "database.yml");
        if (!configFile.getParentFile().exists()) {
            configFile.getParentFile().mkdirs();
        }

        // Load existing config (or create empty)
        yaml = YamlConfiguration.loadConfiguration(configFile);

        // Load defaults from template in resources (preserves comments)
        YamlConfiguration defaults = YamlConfiguration.loadConfiguration(
                new InputStreamReader(
                        DatabaseConfig.class.getClassLoader().getResourceAsStream("configurations/database.yml"),
                        StandardCharsets.UTF_8));
        yaml.setDefaults(defaults);
        yaml.options().copyDefaults(true);

        save();

        // Ensure database directory exists
        File dbDir = new File(DB_DIR);
        if (!dbDir.exists()) {
            dbDir.mkdirs();
        }
    }

    public static void save() {
        YamlUtils.save(configFile, yaml);
    }

    /**
     * Get the database type for a given feature module.
     *
     * @param feature the feature name (e.g. "items", "back", "warps")
     * @return "sqlite" or "mysql"
     */
    public static String getDatabaseType(String feature) {
        String type = yaml.getString("database.features." + feature);
        if (type == null || type.isEmpty()) {
            type = yaml.getString("database.default", "sqlite");
        }
        return type.toLowerCase(Locale.ROOT);
    }

    /** @return true if the given feature uses MySQL */
    public static boolean isMysql(String feature) {
        return "mysql".equals(getDatabaseType(feature));
    }

    /** @return true if the given feature uses SQLite */
    public static boolean isSqlite(String feature) {
        return "sqlite".equals(getDatabaseType(feature));
    }

    // MySQL connection settings

    public static String getMysqlUrl() {
        return yaml.getString("database.mysql.jdbc-url",
                "jdbc:mysql://127.0.0.1:3306/minecraft?useSSL=false&allowPublicKeyRetrieval=true");
    }

    public static String getMysqlUsername() {
        return yaml.getString("database.mysql.username", "root");
    }

    public static String getMysqlPassword() {
        return yaml.getString("database.mysql.password", "");
    }

    public static String getMysqlTablePrefix() {
        return yaml.getString("database.mysql.table-prefix", "youer");
    }
}
