package com.mohistmc.youer.util;

import java.io.File;
import java.io.IOException;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.bukkit.configuration.file.FileConfiguration;

/**
 * @author Mgazul by MohistMC
 * @date 2023/8/2 18:28:15
 */
public class YamlUtils {

    private static final Logger LOGGER = LogManager.getLogger("Youer-YamlUtils");

    public static void save(File file, FileConfiguration yaml) {
        try {
            yaml.save(file);
        } catch (IOException e) {
            LOGGER.error("Failed to save YAML file: " + file.getAbsolutePath(), e);
        }
    }
}
