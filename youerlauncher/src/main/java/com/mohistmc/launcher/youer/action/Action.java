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

package com.mohistmc.launcher.youer.action;

import com.mohistmc.launcher.youer.Main;
import com.mohistmc.launcher.youer.feature.AutoDeleteMods;
import com.mohistmc.launcher.youer.feature.YouerProxySelector;
import com.mohistmc.launcher.youer.libraries.Libraries;
import com.mohistmc.launcher.youer.util.DataParser;
import com.mohistmc.launcher.youer.util.LaunchArgsParser;
import com.mohistmc.tools.FileUtils;
import com.mohistmc.tools.MojangEulaUtil;
import com.mohistmc.tools.OSUtil;
import com.mohistmc.tools.SHA256;
import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.PrintStream;
import java.net.MalformedURLException;
import java.net.ProxySelector;
import java.net.URL;
import java.net.URLClassLoader;
import java.nio.file.Files;
import java.nio.file.StandardCopyOption;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Objects;
import java.util.Scanner;
import lombok.SneakyThrows;

public class Action {

    public static final String LIBRARIES = "libraries";
    private static final PrintStream origin = System.out;
    public static String META_INF = "META-INF/" + LIBRARIES;
    public final String youerVer;
    public final String neoforgeVer;
    public final String mcVer;
    public final String libPath;
    public final File universalJar;
    public final File MINECRAFT_JAR;
    public final File PATCHED;
    public final File BINPATCH;
    public final List<URL> installerTourls = new ArrayList<>();

    @SneakyThrows
    public Action() {
        initInstallerLib();
        this.youerVer = DataParser.getVersion("youer");
        this.neoforgeVer = DataParser.getVersion("neoforge");
        this.mcVer = DataParser.getVersion("minecraft");
        this.libPath = new File(LIBRARIES).getAbsolutePath() + "/";

        this.universalJar = new File(libPath + "net/neoforged/neoforge/%s/neoforge-%s-universal.jar".formatted(neoforgeVer, neoforgeVer));

        this.BINPATCH = new File(libPath + "com/mohistmc/installation/data/client.lzma");
        this.MINECRAFT_JAR = new File(libPath + "net/minecraft/server/%s/server-%s.jar".formatted(mcVer, mcVer));
        this.PATCHED = new File(libPath + "net/neoforged/minecraft-server-patched/%s/minecraft-server-patched-%s.jar".formatted(neoforgeVer, neoforgeVer));

        install();
    }

    public void install() throws Exception {

        List<InstallationTask> tasks = new ArrayList<>();
        copyFileFromJar(BINPATCH, "data/client.lzma", true);
        copyFileFromJar(universalJar, "data/neoforge-%s-universal.jar".formatted(neoforgeVer), false);

        if (youerVer == null) {
            System.out.println("[Youer] There is an error with the installation, the neoforge / neoform version is not set.");
            System.exit(0);
        }
        cleanMinecraftJars();
        tasks.add(new ConsoleToolTask(
                "net.neoforged.installertools.ConsoleTool",
                "--task", "PROCESS_MINECRAFT_JAR",
                "--no-mod-manifest",
                "--input", MINECRAFT_JAR.getAbsolutePath(),
                "--output", PATCHED.getAbsolutePath(),
                "--extract-libraries-to", LIBRARIES,
                "--apply-patches", BINPATCH.getAbsolutePath()
        ));

        mute();
        for (InstallationTask task : tasks) {
            task.execute();
        }
        unmute();
    }

    protected void run(String mainClass, String... args) throws Exception {
        List<URL> classPath = installerTourls;
        System.out.println("[Youer] Loading " + classPath);
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
        System.setOut(new PrintStream(new BufferedOutputStream(new FileOutputStream(out)) {
            @Override
            public void close() throws IOException {
                flush();
                super.close();
            }
        }));
    }

    protected void unmute() {
        if (Main.DEBUG) return;
        if (System.out instanceof PrintStream) {
            System.out.flush();
        }
        System.setOut(origin);
    }

    protected void copyFileFromJar(File file, String pathInJar, boolean clearOld) {
        if (file == BINPATCH) file.delete();
        InputStream is = Main.class.getClassLoader().getResourceAsStream(pathInJar);
        if (!file.exists() || !Objects.equals(SHA256.as(file), SHA256.as(is)) || file.length() <= 1) {
            // Clear old version
            if (clearOld) {
                File parentfile = file.getParentFile();
                if (file.getAbsolutePath().contains("neoforge")) {
                    int lastSlashIndex = parentfile.getAbsolutePath().replaceAll("\\\\", "/").lastIndexOf("/");
                    String result = parentfile.getAbsolutePath().substring(0, lastSlashIndex + 1);
                    File old = new File(result);
                    if (old.exists()) {
                        FileUtils.deleteFolders(old);
                    }
                }
            } else {
                if (pathInJar.contains("-universal.jar")) {
                    file.delete();
                }
            }
            if (is != null) {
                try {
                    Files.createDirectories(file.toPath());
                    Files.copy(is, file.toPath(), StandardCopyOption.REPLACE_EXISTING);
                } catch (IOException ignored) {
                }
            } else {
                System.out.println("[Youer] The file " + pathInJar + " doesn't exists in the Youer jar !");
                System.exit(0);
            }
        }
    }

    private void initInstallerLib() throws MalformedURLException {
        Libraries libraries = Libraries.from("net/neoforged/installertools/installertools/4.0.12/installertools-4.0.12-fatjar.jar|ba3e9d27d9f5145f829ac94124e3d97b80f027949d690b430305734526f143db|813509");
        File file = new File(LIBRARIES, libraries.getPath());
        URL url = file.toURI().toURL();
        installerTourls.add(url);
    }

    public void start() throws Exception {
        AutoDeleteMods.deleteIncompatibleMods();

        List<String> forgeArgs = new ArrayList<>();
        for (String arg : DataParser.launchArgs.stream().filter(s ->
                        s.startsWith("--launchTarget")
                                || s.startsWith("--fml.neoForgeVersion")
                                || s.startsWith("--fml.mcVersion")
                                || s.startsWith("--fml.neoFormVersion"))
                .toList()) {
            forgeArgs.add(arg.split(" ")[0]);
            forgeArgs.add(arg.split(" ")[1]);
        }
        LaunchArgsParser.init(DataParser.launchArgs);

        if (!MojangEulaUtil.hasAcceptedEULA() && OSUtil.getOS().isWindows()) {
            if (System.console() != null) {
                System.out.println(Main.i18n.as("eula"));
                while (!"true".equals(new Scanner(System.in).next())) {
                }
                MojangEulaUtil.writeInfos(Main.i18n.as("eula.text", "https://account.mojang.com/documents/minecraft_eula") + "\n" + new SimpleDateFormat("yyyy-MM-dd").format(new Date()) + "\neula=true");
            }
        }
        Class<?> serverClass = Class.forName("net.neoforged.fml.startup.Server");
        java.lang.reflect.Method mainMethod = serverClass.getDeclaredMethod("main", String[].class);
        mainMethod.invoke(null, (Object) forgeArgs.toArray(String[]::new));
        ProxySelector.setDefault(new YouerProxySelector(ProxySelector.getDefault()));
    }

    private interface InstallationTask {
        void execute() throws Exception;
    }

    private class ConsoleToolTask implements InstallationTask {
        private final String mainClass;
        private final String[] args;

        public ConsoleToolTask(String mainClass, String... args) {
            this.mainClass = mainClass;
            this.args = args;
        }

        @Override
        public void execute() throws Exception {
            run(mainClass, args);
        }
    }

    public static void cleanMinecraftJars() {
        String librariesPath = "libraries";
        File librariesDir = new File(librariesPath);

        if (!librariesDir.exists() || !librariesDir.isDirectory()) {
            return;
        }

        File[] files = librariesDir.listFiles();
        if (files == null) {
            return;
        }

        for (File file : files) {
            if (file.isFile() &&
                    file.getName().toLowerCase().endsWith(".jar") &&
                    file.getName().toLowerCase().startsWith("minecraft")) {
                file.delete();
            }
        }
    }
}
