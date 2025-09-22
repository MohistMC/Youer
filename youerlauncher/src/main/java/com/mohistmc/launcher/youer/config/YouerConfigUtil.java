/*
 * Mohist - MohistMC
 * Copyright (C) 2018-2025.
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program. If not, see <https://www.gnu.org/licenses/>.
 */

package com.mohistmc.launcher.youer.config;

import com.mohistmc.i18n.i18n;
import com.mohistmc.launcher.youer.Main;
import com.mohistmc.yaml.file.YamlConfiguration;
import java.io.File;
import java.util.List;
import java.util.Locale;

public class YouerConfigUtil {

    public static final File youer_yml = new File("youer-config", "youer.yml");
    public static final YamlConfiguration yml = YamlConfiguration.loadConfiguration(youer_yml);

    public static void init() {
        try {
            if (!youer_yml.exists()) {
                youer_yml.createNewFile();
            }
        } catch (Exception ignored) {
        }
    }

    public static boolean INSTALLATIONFINISHED() {
        return !yml.getBoolean("youer.installation-finished", false);
    }

    public static boolean NETWORKMANAGER_DEBUG() {
        String key = "networkmanager.debug";
        if (yml.get(key) == null) {
            yml.set(key, false);
            save();
        }
        return yml.getBoolean(key, false);
    }

    public static List<String> NETWORKMANAGER_INTERCEPT() {
        String key = "networkmanager.intercept";
        if (yml.get(key) == null) {
            yml.set(key, List.of());
            save();
        }
        return yml.getStringList(key);
    }

    public static boolean CHECK_LIBRARIES() {
        String key = "youer.libraries.check";
        if (yml.get(key) == null) {
            yml.set(key, true);
            save();
        }
        return yml.getBoolean(key, true);
    }

    public static boolean CHECK_UPDATE() {
        String key = "youer.check_update";
        if (yml.get(key) == null) {
            yml.set(key, true);
            save();
        }
        return yml.getBoolean(key, true);
    }

    public static boolean aBoolean(String key, boolean defaultReturn) {
        return yml.getBoolean(key, defaultReturn);
    }

    public static void i18n() {
        Main.i18n = new i18n(Main.class.getClassLoader(), YOUERLANG());
    }

    public static void save() {
        try {
            yml.save(youer_yml);
        } catch (Exception ignored) {
        }
    }

    public static boolean isCN() {
        return YOUERLANG().contains("CN");
    }


    public static String YOUERLANG() {
        String key = "youer.lang";
        if (yml.get(key) == null) {
            yml.set(key, Locale.getDefault().toString());
            save();
        }
        return yml.getString(key, Locale.getDefault().toString());
    }

    public static boolean AutoDeleteMods() {
        String key = "youer.auto_delete_mods";
        if (yml.get(key) == null) {
            yml.set(key, true);
            save();
        }
        return yml.getBoolean(key, true);
    }
}
