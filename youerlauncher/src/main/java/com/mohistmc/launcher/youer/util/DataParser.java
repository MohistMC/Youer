package com.mohistmc.launcher.youer.util;

import com.mohistmc.launcher.youer.Main;
import com.mohistmc.tools.FileUtils;
import com.mohistmc.tools.OSUtil;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

public class DataParser {

    public static final HashMap<String, String> versionMap = new HashMap<>();
    public static final List<String> launchArgs = new ArrayList<>();

    public static void parseVersions() {
        versionMap.put("neoforge", FileUtils.readFileFromJar(DataParser.class.getClassLoader(), "versions/neoforge.txt").getFirst());
        versionMap.put("minecraft", FileUtils.readFileFromJar(DataParser.class.getClassLoader(), "versions/minecraft.txt").getFirst());
        versionMap.put("youer", FileUtils.readFileFromJar(DataParser.class.getClassLoader(), "versions/youer.txt").getFirst());
        versionMap.put("mcp", FileUtils.readFileFromJar(DataParser.class.getClassLoader(), "versions/mcp.txt").getFirst());

        Main.MCVERSION = versionMap.get("minecraft");
    }

    public static void parseLaunchArgs() {
        OSUtil.OS os = OSUtil.getOS();
        String osName = os.isWindows() ? "win" : "unix";
        launchArgs.addAll(FileUtils.readFileFromJar(DataParser.class.getClassLoader(), "data/" + osName + "_args.txt"));
    }
}
