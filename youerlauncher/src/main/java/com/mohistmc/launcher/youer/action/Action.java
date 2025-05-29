/*
 * MohistMC
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

package com.mohistmc.launcher.youer.action;

import com.mohistmc.launcher.youer.Main;
import com.mohistmc.launcher.youer.config.YouerConfigUtil;
import com.mohistmc.launcher.youer.feature.DefaultLibraries;
import com.mohistmc.launcher.youer.libraries.Libraries;
import com.mohistmc.launcher.youer.util.DataParser;
import com.mohistmc.launcher.youer.util.I18n;
import com.mohistmc.launcher.youer.util.YouerModuleManager;
import com.mohistmc.tools.FileUtils;
import com.mohistmc.tools.JarTool;
import com.mohistmc.tools.SHA256;
import java.io.BufferedOutputStream;
import java.io.BufferedReader;
import java.io.File;
import java.io.FileOutputStream;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.PrintStream;
import java.net.URL;
import java.net.URLClassLoader;
import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.StandardCopyOption;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import lombok.SneakyThrows;

public class Action {

    private static final PrintStream origin = System.out;
    public static ArrayList<String> launchArgs = new ArrayList<>(Arrays.asList("java", "-jar"));
    public final String mohistVer;
    public final String neoforgeVer;
    public final String mcpVer;
    public final String mcVer;
    public final String libPath;
    public final String forgeStart;
    public final File universalJar;
    public final File BINPATCH;
    public final File installInfo;
    public final String otherStart;
    public final File MC_UNPACKED;
    public final File MC_SLIM;
    public final File MC_SRG;
    public final File MC_EXTRA;
    public final String mcpStart;
    public final File mcpZip;
    public final File MAPPINGS;
    public final File MINECRAFT_JAR;
    public final File mojmap;
    public final File MERGED_MAPPINGS;
    public final File PATCHED;
    public List<URL> installerTourls = new ArrayList<>();

    @SneakyThrows
    public Action() {
        init();
        this.mohistVer = DataParser.versionMap.get("youer");
        this.neoforgeVer = DataParser.versionMap.get("neoforge");
        this.mcpVer = DataParser.versionMap.get("mcp");
        this.mcVer = DataParser.versionMap.get("minecraft");
        this.libPath = new File("libraries").getAbsolutePath() + "/";

        this.forgeStart = libPath + "net/neoforged/neoforge/" + neoforgeVer + "/neoforge-" + neoforgeVer;
        this.universalJar = new File(forgeStart + "-universal.jar");

        this.BINPATCH = new File(libPath + "com/mohistmc/installation/data/server.lzma");
        this.installInfo = new File(libPath + "com/mohistmc/installation/installInfo");
        this.otherStart = libPath + "net/minecraft/server/" + mcpVer + "/server-" + mcpVer;

        this.MC_UNPACKED = new File(otherStart + "-unpacked.jar");
        this.MC_SLIM = new File(otherStart + "-slim.jar");
        this.MC_SRG = new File(otherStart + "-srg.jar");
        this.MC_EXTRA = new File(otherStart + "-extra.jar");

        this.mojmap = new File(otherStart + "-mappings.txt");

        this.mcpStart = libPath + "net/neoforged/neoform/" + mcpVer + "/neoform-" + mcpVer;
        this.mcpZip = new File(mcpStart + ".zip");
        this.MAPPINGS = new File(mcpStart + "-mappings.txt");
        this.MERGED_MAPPINGS = new File(mcpStart + "-mappings-merged.txt");
        this.MINECRAFT_JAR = new File(libPath + "net/minecraft/server/" + mcVer + "/server-" + mcVer + ".jar");
        this.PATCHED = new File(forgeStart + "-server.jar");


        install();
    }

    private void install() throws Exception {

        launchArgs.add(new File(URLDecoder.decode(YouerModuleManager.class.getProtectionDomain().getCodeSource().getLocation().toURI().getPath(), StandardCharsets.UTF_8)).getAbsolutePath());
        launchArgs.addAll(Main.mainArgs);

        copyFileFromJar(BINPATCH, "data/server.lzma", true);
        copyFileFromJar(universalJar, "data/neoforge-" + neoforgeVer + "-universal.jar", false);

        if (!needsInstall()) return;
        System.out.println(I18n.as("installation.start"));

        copyFileFromJar(universalJar, "data/neoforge-" + neoforgeVer + "-universal.jar", true);

        if (mohistVer == null || mcpVer == null) {
            System.out.println("[Mohist] There is an error with the installation, the forge / mcp version is not set.");
            System.exit(0);
        }

        mute();
        if (MINECRAFT_JAR.exists()) {
            run("net.neoforged.installertools.ConsoleTool",
                    "--task",
                    "BUNDLER_EXTRACT",
                    "--input", MINECRAFT_JAR.getAbsolutePath(),
                    "--output", libPath,
                    "--libraries");
            if (!MC_UNPACKED.exists()) {
                run("net.neoforged.installertools.ConsoleTool",
                        "--task",
                        "BUNDLER_EXTRACT",
                        "--input", MINECRAFT_JAR.getAbsolutePath(),
                        "--output", MC_UNPACKED.getAbsolutePath(),
                        "--jar-only");
            }
        } else {
            System.out.println(I18n.as("installation.minecraftserver") + MINECRAFT_JAR.getAbsolutePath());
            System.exit(0);
        }

        if (mcpZip.exists()) {
            if (!MAPPINGS.exists()) {
                run("net.neoforged.installertools.ConsoleTool",
                        "--task",
                        "MCP_DATA",
                        "--input", mcpZip.getAbsolutePath(),
                        "--output", MAPPINGS.getAbsolutePath(),
                        "--key",
                        "mappings");
            }
        } else {
            System.out.println(I18n.as("installation.mcpfilemissing"));
            System.exit(0);
        }

        if (JarTool.isCorrupted(MC_UNPACKED)) {
            MC_UNPACKED.delete();
        }

        if (JarTool.isCorrupted(MC_SRG)) {
            MC_SRG.delete();
        }

        if (!MC_SLIM.exists()) {
            run("net.neoforged.jarsplitter.ConsoleTool",
                    "--input",
                    MC_UNPACKED.getAbsolutePath(),
                    "--slim",
                    MC_SLIM.getAbsolutePath(),
                    "--extra",
                    MC_EXTRA.getAbsolutePath(),
                    "--srg",
                    MERGED_MAPPINGS.getAbsolutePath());
        }

        if (!MC_SRG.exists()) {
            run("net.neoforged.art.Main",
                    "--input", MC_SLIM.getAbsolutePath(),
                    "--output", MC_SRG.getAbsolutePath(),
                    "--names", MERGED_MAPPINGS.getAbsolutePath(),
                    "--ann-fix",
                    "--ids-fix",
                    "--src-fix",
                    "--record-fix");
        }

        String storedServerMD5 = null;
        String storedLzmaMD5 = null;
        String storeduniversalJarMD5 = SHA256.as(universalJar);
        String serverMD5 = SHA256.as(PATCHED);
        String lzmaMD5 = SHA256.as(BINPATCH);

        if (installInfo.exists()) {
            List<String> infoLines = Files.readAllLines(installInfo.toPath());
            if (!infoLines.isEmpty()) {
                storedServerMD5 = infoLines.getFirst();
            }
            if (infoLines.size() > 1) {
                storedLzmaMD5 = infoLines.get(1);
            }
        }

        if (!PATCHED.exists() || storedServerMD5 == null || storedLzmaMD5 == null || !storedServerMD5.equals(serverMD5) || !storedLzmaMD5.equals(lzmaMD5)) {
            run("net.neoforged.binarypatcher.ConsoleTool",
                    "--clean", MC_SRG.getAbsolutePath(),
                    "--output", PATCHED.getAbsolutePath(),
                    "--apply", BINPATCH.getAbsolutePath());
            serverMD5 = SHA256.as(PATCHED);
        }

        FileWriter fw = new FileWriter(installInfo);
        fw.write(serverMD5 + "\n");
        fw.write(lzmaMD5 + "\n");
        fw.write(storeduniversalJarMD5);
        fw.close();
        unmute();

        System.out.println(I18n.as("installation.finished"));
        YouerConfigUtil.yml.set("youer.installation-finished", true);
        YouerConfigUtil.save();
        JarTool.restartServer(launchArgs, true);
    }

    protected void run(String mainClass, String... args) throws Exception {
        List<URL> classPath = installerTourls;
        System.out.println("[Mohist] Loading " + classPath);
        URLClassLoader loader = URLClassLoader.newInstance(classPath.toArray(new URL[0]));
        Class.forName(mainClass, true, loader).getDeclaredMethod("main", String[].class).invoke(null, new Object[]{args});
        loader.clearAssertionStatus();
        loader.close();
    }

    protected void mute() throws Exception {
        if (Main.DEBUG) return;
        File out = new File(libPath + "com/mohistmc/installation", "installationLogs.txt");
        if (!out.exists()) {
            out.getParentFile().mkdirs();
            out.createNewFile();
        }
        System.setOut(new PrintStream(new BufferedOutputStream(new FileOutputStream(out))));
    }

    protected void unmute() {
        if (Main.DEBUG) return;
        System.setOut(origin);
    }

    protected void copyFileFromJar(File file, String pathInJar, boolean clearOld) {
        if (file == BINPATCH) file.delete();
        InputStream is = Main.class.getClassLoader().getResourceAsStream(pathInJar);
        if (!file.exists() || !SHA256.as(file).equals(SHA256.as(is)) || file.length() <= 1) {
            // Clear old version
            if (clearOld) {
                File parentfile = file.getParentFile();
                if (file.getAbsolutePath().contains("minecraftforge")) {
                    int lastSlashIndex = parentfile.getAbsolutePath().replaceAll("\\\\", "/").lastIndexOf("/");
                    String result = parentfile.getAbsolutePath().substring(0, lastSlashIndex + 1);
                    File old = new File(result);
                    if (old.exists()) {
                        FileUtils.deleteFolders(old);
                    }
                }
            }
            if (is != null) {
                try {
                    Files.createDirectories(file.toPath());
                    Files.copy(is, file.toPath(), StandardCopyOption.REPLACE_EXISTING);
                } catch (IOException ignored) {
                }
            } else {
                System.out.println("[Mohist] The file " + file.getName() + " doesn't exists in the Mohist jar !");
                System.exit(0);
            }
        }
    }

    private void init() {
        try {
            BufferedReader b = new BufferedReader(new InputStreamReader(DefaultLibraries.class.getClassLoader().getResourceAsStream("installer.txt")));
            for (String line = b.readLine(); line != null; line = b.readLine()) {
                Libraries libraries = Libraries.from(line);
                File file = new File("libraries", libraries.getPath());
                URL url = file.toURI().toURL();
                installerTourls.add(url);
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }


    public boolean needsInstall() throws IOException {
        if (installInfo.exists()) {
            String lzmaMD5 = SHA256.as(BINPATCH);
            List<String> lines = Files.readAllLines(installInfo.toPath());

            return lines.size() < 3 || !lzmaMD5.equals(lines.get(1)) || !SHA256.as(universalJar).equals(lines.get(2));
        }
        return true;
    }

}
