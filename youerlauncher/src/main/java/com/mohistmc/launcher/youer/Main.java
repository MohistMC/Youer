/*
 * Copyright (C) MohistMC.
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

package com.mohistmc.launcher.youer;

import com.mohistmc.i18n.i18n;
import com.mohistmc.launcher.youer.action.Action;
import com.mohistmc.launcher.youer.config.YouerConfigUtil;
import com.mohistmc.launcher.youer.feature.DefaultLibraries;
import com.mohistmc.launcher.youer.util.DataParser;
import com.mohistmc.launcher.youer.util.I18n;
import com.mohistmc.tools.Logo;
import java.lang.management.ManagementFactory;
import java.util.ArrayList;
import java.util.List;
import lombok.SneakyThrows;

public class Main {
    public static final boolean DEBUG = Boolean.getBoolean("youer.debug");
    public static final List<String> mainArgs = new ArrayList<>();
    public static String MCVERSION;
    public static i18n i18n;

    public static String getVersion() {
        return (Main.class.getPackage().getImplementationVersion() != null) ? Main.class.getPackage().getImplementationVersion() : MCVERSION;
    }

    @SneakyThrows
    static void main(String[] args) {
        System.setProperty("org.jline.terminal.disableDeprecatedProviderWarning", "true");
        mainArgs.addAll(List.of(args));
        DataParser.parseVersions();
        DataParser.parseLaunchArgs();
        YouerConfigUtil.init();
        YouerConfigUtil.i18n();
        if (YouerConfigUtil.aBoolean("youer.show_logo", true)) {
            System.out.printf("%n%s%n%s - %s, Java(%s) %s PID: %s%n",
                    Logo.asYouer(),
                    i18n.as("youer.launch.welcomemessage"),
                    getVersion(),
                    System.getProperty("java.class.version"),
                    System.getProperty("java.version"),
                    ManagementFactory.getRuntimeMXBean().getName().split("@")[0]
            );
            if (YouerConfigUtil.isCN()) {
                System.out.println("+------------------------------------------------------+");
                System.out.println("|                                                      |");
                System.out.println("| 官网推出一键开服功能　　　　　　　　　　　　　　　　 |");
                System.out.println("| 官网(中国): https://www.mohistmc.cn/                 |");
                System.out.println("| 爱发电: https://ifdian.net/a/MohistMC                |");
                System.out.println("|                                                      |");
                System.out.println("+------------------------------------------------------+");
            }
        }

        long startTime = System.currentTimeMillis();
        if (System.getProperty("log4j2.configurationFile") == null) {
            System.setProperty("log4j2.configurationFile", "log4j2_youer.xml");
        }
        System.out.println(" ");
        System.out.println(I18n.as("deployment.environment"));
        if (YouerConfigUtil.CHECK_LIBRARIES()) {
            DefaultLibraries.run();
        }
        var action = new Action();
        long endTime = System.currentTimeMillis();
        long duration = endTime - startTime;
        double seconds = duration / 1000.0;
        System.out.println(I18n.as("deployment.finished", seconds));
        action.start();
    }
}
