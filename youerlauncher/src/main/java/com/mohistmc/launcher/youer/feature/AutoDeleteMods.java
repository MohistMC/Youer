package com.mohistmc.launcher.youer.feature;

import com.mohistmc.launcher.youer.config.YouerConfigUtil;
import com.mohistmc.launcher.youer.util.I18n;
import com.mohistmc.launcher.youer.util.JarLoader;
import com.mohistmc.launcher.youer.util.JarModifier;
import com.mohistmc.tools.FileUtils;
import com.mohistmc.tools.OSUtil;
import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.StandardCopyOption;
import java.util.AbstractMap;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Automatically remove mods that are not compatible with Mohist servers
 */
public class AutoDeleteMods {

    /**
     * MOD blacklist mapping table
     * Key: Full class name (e.g. "org.example.ModClass")
     * Value: Reason for deletion
     */
    private static final Map<String, DeletionReason> MOD_BLACKLIST = new HashMap<>() {{
        put("org.spongepowered.common.applaunch.AppLaunch", DeletionReason.CORE_CONFLICT);
        //put("me.wesley1808.servercore.common.ServerCore", DeletionReason.DUPLICATE_FEATURE);
        put("i18nupdatemod.I18nUpdateMod", DeletionReason.CLIENT_ONLY);
        put("dev.tr7zw.skinlayers.SkinLayersMod", DeletionReason.CLIENT_ONLY);
        put("com.biel.mod.mixin.VelocityMixin", DeletionReason.DUPLICATE_FEATURE);
        put("optifine.Differ", DeletionReason.CLIENT_ONLY);
        //put("org.embeddedt.modernfix.ModernFix", DeletionReason.DUPLICATE_FEATURE);
        put("ca.spottedleaf.moonrise.neoforge.MoonriseNeoForge", DeletionReason.DUPLICATE_FEATURE);
        put("me.steinborn.krypton.mod.server.KryptonServerInitializer", DeletionReason.DUPLICATE_FEATURE);
        put("me.steinborn.krypton.mod.KryptonBootstrap", DeletionReason.DUPLICATE_FEATURE);
        put("org.thinkingstudio.krypton_foxified.KryptonFoxified", DeletionReason.DUPLICATE_FEATURE);
        put("one.pkg.mod.krypton_fnp.NeoModBootstrap", DeletionReason.DUPLICATE_FEATURE);
        put("one.pkg.kfnp.NeoModBootstrap", DeletionReason.DUPLICATE_FEATURE);
        put("net.caffeinemc.mods.lithium.neoforge.LithiumNeoForgeMod", DeletionReason.DUPLICATE_FEATURE);
        put("me.jellysquid.mods.lithium.common.LithiumMod", DeletionReason.DUPLICATE_FEATURE);
        //put("com.bawnorton.neruina.Neruina", DeletionReason.DUPLICATE_FEATURE);
        put("ca.spottedleaf.starlight.common.ScalableLuxEntrypoint", DeletionReason.DUPLICATE_FEATURE);
        //put("me.drex.antixray.neoforge.AntiXrayMod", DeletionReason.DUPLICATE_FEATURE);
        put("dev.uncandango.alltheleaks.AllTheLeaks", DeletionReason.DUPLICATE_FEATURE);
        put("com.yshs.searchonmcmod.SearchOnMcmod", DeletionReason.CLIENT_ONLY);
        put("eu.midnightdust.cullleaves.neoforge.CullLeavesClientForge", DeletionReason.CLIENT_ONLY);
        put("net.xolt.freecam.forge.FreecamForge", DeletionReason.CLIENT_ONLY);
        put("com.buuz135.smithingtemplateviewer.SmithingTemplateViewer", DeletionReason.CLIENT_ONLY);
        put("com.leclowndu93150.particular.Main", DeletionReason.CLIENT_ONLY);
        put("dev.imb11.sounds.loaders.neoforge.SoundsNeoForge", DeletionReason.CLIENT_ONLY);
        put("me.drex.crashexploitfixer.neoforge.CrashExploitFixerNeoforge", DeletionReason.DUPLICATE_FEATURE);
        put("fabric-carpet-refmap", DeletionReason.FABRIC_ONLY);
        put("com.ishland.c2me.C2MEMod", DeletionReason.CLIENT_ONLY);
        put("me.pepperbell.continuity.client.ContinuityClient", DeletionReason.CLIENT_ONLY);
        put("link.e4mc.neoforge.E4mcClientNeoForge", DeletionReason.DUPLICATE_FEATURE);
        put("org.adde0109.pcf.PCFNeo", DeletionReason.DUPLICATE_FEATURE);
        put("com.leclowndu93150.threadtweak.ThreadTweak", DeletionReason.DUPLICATE_FEATURE);
        put("ru.vidtu.ksyxis.platform.KNeoForge", DeletionReason.DUPLICATE_FEATURE);
        //put("carpet.CarpetServer", DeletionReason.DUPLICATE_FEATURE);
        put("io.github.reserveword.imblocker.IMBlocker", DeletionReason.CLIENT_ONLY);
        put("snownee.pdgamerules.PDGameRulesMod", DeletionReason.DUPLICATE_FEATURE);
        put("com.xinian.KryptonHybrid.kryptonhybrid", DeletionReason.DUPLICATE_FEATURE);
        put("com.wfphantom.stfudisconnect.STFUDisconnect", DeletionReason.DUPLICATE_FEATURE);
    }};

    private static final String END = OSUtil.getOS().isWindows() ? ";" : ":";
    private static final String ZSTD = "libraries/com/github/luben/zstd-jni/1.5.7-8/zstd-jni-1.5.7-8.jar" + END;
    private static final String MYSQL = "libraries/com/mysql/mysql-connector-j/8.4.0/mysql-connector-j-8.4.0.jar" + END;
    private static final String SQLITE = "libraries/org/xerial/sqlite-jdbc/3.46.0.0/sqlite-jdbc-3.46.0.0.jar" + END;
    private static final String PROTOBUF = "libraries/com/google/protobuf/protobuf-java/3.25.1/protobuf-java-3.25.1.jar" + END;
    private static final List<Map.Entry<String, String>> LIB_BLACKLIST = Arrays.asList(
            new AbstractMap.SimpleEntry<>("cn.tohsaka.factory.zstdmc.Zstdmc", ZSTD),
            new AbstractMap.SimpleEntry<>("cn.tohsaka.factory.zstdnet.Zstdnet", ZSTD),
            new AbstractMap.SimpleEntry<>("cn.ussshenzhou.notenoughbandwidth.NotEnoughBandwidthLegacy", ZSTD),
            new AbstractMap.SimpleEntry<>("cn.ussshenzhou.notenoughbandwidth.NotEnoughBandwidth", ZSTD),
            new AbstractMap.SimpleEntry<>("com.daqem.grieflogger.neoforge.GriefLoggerNeoForge", MYSQL),
            new AbstractMap.SimpleEntry<>("com.daqem.grieflogger.neoforge.GriefLoggerNeoForge", SQLITE),
            new AbstractMap.SimpleEntry<>("com.daqem.grieflogger.neoforge.GriefLoggerNeoForge", PROTOBUF)
    );

    /**
     * Mapping tables for classes to directories
     * Key: Detected class (e.g. "org.example.ModClass")
     * Value: The directory path that needs to be removed from the JAR
     */
    private static final Map<String, String> CLASS_TO_DIRECTORY_MAPPING = new HashMap<>() {{
        put("de.bluecolored.bluemap.forge.ForgeMod", "META-INF/services");
    }};

    /**
     * Scan and remove incompatible mods
     */
    public static void deleteIncompatibleMods() {
        List<String> services = new ArrayList<>(CLASS_TO_DIRECTORY_MAPPING.keySet());
        for (String identifier : services) {
            try {
                checkModFile(identifier, true);
            } catch (Exception ignored) {
            }
        }
        syncLibBlacklistToJarLoader();
        if (!YouerConfigUtil.AutoDeleteMods()) return;
        System.out.println(I18n.as("update.mods"));

        List<String> identifiers = new ArrayList<>(MOD_BLACKLIST.keySet());
        for (String identifier : identifiers) {
            try {
                checkModFile(identifier, false);
            } catch (Exception ignored) {
            }
        }
    }

    /**
     * Check and process individual mod files
     *
     * @param identifier of the class or file to be checked (can be a full class name or a file identifier)
     */
    private static void checkModFile(String identifier, boolean isFile) {
        File modsDir = new File("mods");

        if (!modsDir.exists()) {
            modsDir.mkdir();
            return;
        }

        File[] jarFiles = modsDir.listFiles((dir, name) -> name.endsWith(".jar"));
        if (jarFiles == null) return;

        for (File jarFile : jarFiles) {
            try {
                if (identifier.contains(".")) {
                    String classPath = identifier.replaceAll("\\.", "/") + ".class";
                    if (FileUtils.fileExists(jarFile, classPath)) {
                        backupAndDelete(jarFile, identifier, isFile);
                    }
                }
            } catch (Exception ignored) {
            }
        }
    }

    /**
     * Backup and delete MOD files
     *
     * @param modFile to process MOD files
     */
    private static void backupAndDelete(File modFile, String className, boolean isFile) throws Exception {
        DeletionReason reason = MOD_BLACKLIST.getOrDefault(className, DeletionReason.UNKNOWN);

        String directoryToRemove = CLASS_TO_DIRECTORY_MAPPING.get(className);
        if (isFile && directoryToRemove != null && !directoryToRemove.isEmpty()) {
            try {
                JarModifier.removeDirectoryFromJar(modFile.getAbsolutePath(), directoryToRemove);
                return;
            } catch (IOException e) {
                System.err.println("Failed to remove directory from JAR: " + modFile.getName() + " - " + e.getMessage());
            }
        }

        File backupDir = new File("delete/mods");
        File backupFile = new File("delete", modFile.getPath());

        System.gc();
        Thread.sleep(100);
        if (!backupDir.exists()) {
            backupDir.mkdirs();
        } else if (backupFile.exists()) {
            backupFile.delete();
        }

        Files.copy(modFile.toPath(), backupFile.toPath(), StandardCopyOption.REPLACE_EXISTING);
        try {
            Files.deleteIfExists(modFile.toPath());
            System.out.println(I18n.as("update.deleting",
                    modFile.getName(),
                    reason.getDisplayText()
            ));
        } catch (IOException ignored) {
        }
    }


    public enum DeletionReason {
        CORE_CONFLICT("core_conflict"),
        DUPLICATE_FEATURE("duplicate_feature"),
        CLIENT_ONLY("client_only"),
        FABRIC_ONLY("fabric_only"),
        UNKNOWN("unknown");

        private final String i18nKey;

        DeletionReason(String i18nKey) {
            this.i18nKey = i18nKey;
        }

        public String getDisplayText() {
            return I18n.as("update.deleting.reason." + i18nKey);
        }
    }

    public static List<Map.Entry<String, String>> getAllBlacklistEntries() {
        return new ArrayList<>(LIB_BLACKLIST);
    }

    public static void syncLibBlacklistToJarLoader() {
        for (Map.Entry<String, String> entry : getAllBlacklistEntries()) {
            String className = entry.getKey();
            String jarName = entry.getValue();

            File modsDir = new File("mods");
            if (!modsDir.exists()) {
                continue;
            }

            File[] jarFiles = modsDir.listFiles((dir, name) -> name.endsWith(".jar"));
            if (jarFiles == null) return;

            for (File jarFile : jarFiles) {
                try {
                    String classPath = className.replaceAll("\\.", "/") + ".class";
                    if (FileUtils.fileExists(jarFile, classPath)) {
                        JarLoader.addToBlacklist(jarName);
                    }
                } catch (Exception ignored) {
                }
            }
        }
    }
}
