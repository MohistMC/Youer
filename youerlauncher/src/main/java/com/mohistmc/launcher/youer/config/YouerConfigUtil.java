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
import java.util.Locale;

public class YouerConfigUtil {

    public static final File mohistyml = new File("youer.yml");
    public static final YamlConfiguration yml = YamlConfiguration.loadConfiguration(mohistyml);

    public static void init() {
        try {
            if (!mohistyml.exists()) {
                mohistyml.createNewFile();
            }
        } catch (Exception ignored) {
        }
    }

    public static boolean INSTALLATIONFINISHED() {
        return !yml.getBoolean("youer.installation-finished", false);
    }

    public static boolean CHECK_UPDATE_AUTO_DOWNLOAD() {
        String key = "youer.check_update_auto_download";
        if (yml.get(key) == null) {
            yml.set(key, false);
            save();
        }
        return yml.getBoolean(key, false);
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
            yml.save(mohistyml);
        } catch (Exception e) {
            e.printStackTrace();
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
}
