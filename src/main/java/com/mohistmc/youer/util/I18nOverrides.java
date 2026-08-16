package com.mohistmc.youer.util;

import com.mohistmc.youer.Youer;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.Map;
import java.util.Properties;
import org.bukkit.Bukkit;

/**
 * Loads user-provided translations from youer-config/lang/overrides.properties and
 * applies them on top of the built-in i18n. The mohist i18n implementation resolves
 * a key against the CURRENT_CACHE map as the final fallback, so entries put there
 * take precedence over both the language bundle and the default bundle without
 * modifying the library itself.
 */
public class I18nOverrides {

    private static final File DIR = new File("youer-config", "lang");
    private static final File FILE = new File(DIR, "overrides.properties");

    private static final String TEMPLATE = """
            # ===================================================
            # Youer i18n overrides
            # Override or add translation keys for the built-in Youer i18n.
            #
            # Syntax is the same as Java .properties files:
            #   key=value
            #   # comment
            # Values support %s placeholders, e.g.
            #   worldcommands.command.teleportSpawn=Teleported to %s Spawn
            #
            # This file applies to every language. Make sure the values match
            # the language configured in youer.yml (youer.lang).
            # Edit the file and run /youer reload to apply the changes.
            # ===================================================
            """;

    private I18nOverrides() {
    }

    public static void init() {
        if (Youer.i18n == null) {
            return;
        }
        try {
            if (!DIR.isDirectory() && !DIR.mkdirs()) {
                Youer.LOGGER.warn("[Youer] Failed to create lang directory: {}", DIR);
                return;
            }
            if (!FILE.isFile()) {
                Files.writeString(FILE.toPath(), TEMPLATE, StandardCharsets.UTF_8);
            }
            Properties overrides = new Properties();
            try (InputStream in = new FileInputStream(FILE);
                 InputStreamReader reader = new InputStreamReader(in, StandardCharsets.UTF_8)) {
                overrides.load(reader);
            }
            Map<String, String> cache = Youer.i18n.CURRENT_CACHE;
            cache.clear();
            for (Map.Entry<Object, Object> entry : overrides.entrySet()) {
                cache.put(String.valueOf(entry.getKey()), String.valueOf(entry.getValue()));
            }
        } catch (IOException e) {
            Bukkit.getLogger().log(java.util.logging.Level.SEVERE, "Could not load " + FILE, e);
        }
    }
}
