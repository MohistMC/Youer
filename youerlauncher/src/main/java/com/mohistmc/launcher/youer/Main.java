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

package com.mohistmc.launcher.youer;

import com.mohistmc.i18n.i18n;
import com.mohistmc.launcher.youer.action.Action;
import com.mohistmc.launcher.youer.config.YouerConfigUtil;
import com.mohistmc.launcher.youer.feature.AutoDeleteMods;
import com.mohistmc.launcher.youer.feature.DefaultLibraries;
import com.mohistmc.launcher.youer.util.DataParser;
import com.mohistmc.launcher.youer.util.YouerModuleManager;
import com.mohistmc.tools.JarTool;
import com.mohistmc.tools.MojangEulaUtil;
import cpw.mods.bootstraplauncher.BootstrapLauncher;
import java.lang.management.ManagementFactory;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Scanner;

public class Main {
    public static final boolean DEBUG = Boolean.getBoolean("youer.debug");
    public static final List<String> mainArgs = new ArrayList<>();
    public static String MCVERSION;
    public static i18n i18n;
    public static JarTool jarTool;

    public static String getVersion() {
        return (Main.class.getPackage().getImplementationVersion() != null) ? Main.class.getPackage().getImplementationVersion() : MCVERSION;
    }

    public static void main(String[] args) throws Exception {
        mainArgs.addAll(List.of(args));
        DataParser.parseVersions();
        DataParser.parseLaunchArgs();
        YouerConfigUtil.init();
        YouerConfigUtil.i18n();
        jarTool = new JarTool(Main.class);
        if (YouerConfigUtil.INSTALLATIONFINISHED() && YouerConfigUtil.aBoolean("mohist.show_logo", true)) {
            String test = """
                    
                     ██╗   ██╗ ██████╗ ██╗   ██╗███████╗██████╗
                     ╚██╗ ██╔╝██╔═══██╗██║   ██║██╔════╝██╔══██╗
                      ╚████╔╝ ██║   ██║██║   ██║█████╗  ██████╔╝
                       ╚██╔╝  ██║   ██║██║   ██║██╔══╝  ██╔══██╗
                        ██║   ╚██████╔╝╚██████╔╝███████╗██║  ██║
                        ╚═╝    ╚═════╝  ╚═════╝ ╚══════╝╚═╝  ╚═╝
                    
                    %s-%s-%s Java(%s) %s PID: %s
                    """;
            System.out.printf(test + "%n", i18n.as("youer.launch.welcomemessage"),
                    getVersion(),
                    DataParser.versionMap.get("youer"),
                    System.getProperty("java.class.version"),
                    System.getProperty("java.version"),
                    ManagementFactory.getRuntimeMXBean().getName().split("@")[0]
            );
            if (YouerConfigUtil.isCN()) {
                System.out.println("+------------------------------------------------------+");
                System.out.println("|                                                      |");
                System.out.println("| 官方交流QQ群: 570870451                              |");
                System.out.println("| 官网(中国): https://www.mohistmc.cn/                 |");
                System.out.println("| 爱发电: https://afdian.net/a/MohistMC                |");
                System.out.println("|                                                      |");
                System.out.println("+------------------------------------------------------+");
            }
        }


        if (System.getProperty("log4j.configurationFile") == null) {
            System.setProperty("log4j.configurationFile", "log4j2_youer.xml");
        }
        if (YouerConfigUtil.INSTALLATIONFINISHED() && YouerConfigUtil.CHECK_LIBRARIES()) {
           DefaultLibraries.run();
        }
        if (YouerConfigUtil.INSTALLATIONFINISHED()) {
            new Action();
        }
        AutoDeleteMods.jar();

        // [--launchTarget, forgeserver, --fml.neoForgeVersion, 21.1.172, --fml.fmlVersion, 4.0.39, --fml.mcVersion, 1.21.1, --fml.neoFormVersion, 20240808.144430]
        List<String> forgeArgs = new ArrayList<>();
        for (String arg : DataParser.launchArgs.stream().filter(s ->
                        s.startsWith("--launchTarget")
                                || s.startsWith("--fml.neoForgeVersion")
                                || s.startsWith("--fml.mcVersion")
                                || s.startsWith("--fml.fmlVersion")
                                || s.startsWith("--fml.neoFormVersion"))
                .toList()) {
            forgeArgs.add(arg.split(" ")[0]);
            forgeArgs.add(arg.split(" ")[1]);
        }
        new YouerModuleManager(DataParser.launchArgs);

        if (!MojangEulaUtil.hasAcceptedEULA()) {
            System.out.println(i18n.as("eula"));
            while (!"true".equals(new Scanner(System.in).next())) {
            }
            MojangEulaUtil.writeInfos(i18n.as("eula.text", "https://account.mojang.com/documents/minecraft_eula") + "\n" + new SimpleDateFormat("yyyy-MM-dd").format(new Date()) + "\neula=true");
        }
        BootstrapLauncher.main(forgeArgs.toArray(String[]::new));
    }
}
