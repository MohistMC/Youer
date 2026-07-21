package com.mohistmc.launcher.youer.feature;

import com.mohistmc.launcher.youer.config.YouerConfigUtil;
import com.mohistmc.launcher.youer.util.I18n;
import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.StandardCopyOption;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;

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
        put("me.wesley1808.servercore.common.ServerCore", DeletionReason.DUPLICATE_FEATURE);
        put("i18nupdatemod.I18nUpdateMod", DeletionReason.CLIENT_ONLY);
        put("dev.tr7zw.skinlayers.SkinLayersMod", DeletionReason.CLIENT_ONLY);
        put("com.biel.mod.mixin.VelocityMixin", DeletionReason.DUPLICATE_FEATURE);
        put("optifine.Differ", DeletionReason.CLIENT_ONLY);
        put("de.keksuccino.drippyloadingscreen.DrippyLoadingScreenNeoForge", DeletionReason.CLIENT_ONLY);
        put("com.meowconsole.neoforge.MeowConsoleNeoForgeMod", DeletionReason.DUPLICATE_FEATURE);
        //put("org.embeddedt.modernfix.ModernFix", DeletionReason.DUPLICATE_FEATURE);
        put("ca.spottedleaf.moonrise.neoforge.MoonriseNeoForge", DeletionReason.DUPLICATE_FEATURE);
        put("me.steinborn.krypton.mod.server.KryptonServerInitializer", DeletionReason.DUPLICATE_FEATURE);
        put("me.steinborn.krypton.mod.KryptonBootstrap", DeletionReason.DUPLICATE_FEATURE);
        put("org.thinkingstudio.krypton_foxified.KryptonFoxified", DeletionReason.DUPLICATE_FEATURE);
        put("net.caffeinemc.mods.lithium.neoforge.LithiumNeoForgeMod", DeletionReason.DUPLICATE_FEATURE);
        put("me.jellysquid.mods.lithium.common.LithiumMod", DeletionReason.DUPLICATE_FEATURE);
        //put("com.bawnorton.neruina.Neruina", DeletionReason.DUPLICATE_FEATURE);
        put("ca.spottedleaf.starlight.common.ScalableLuxEntrypoint", DeletionReason.DUPLICATE_FEATURE);
        put("me.drex.antixray.neoforge.AntiXrayMod", DeletionReason.DUPLICATE_FEATURE);
        //put("dev.uncandango.alltheleaks.AllTheLeaks", DeletionReason.DUPLICATE_FEATURE);
        //put("com.yshs.searchonmcmod.SearchOnMcmod", DeletionReason.CLIENT_ONLY);
        //put("eu.midnightdust.cullleaves.neoforge.CullLeavesClientForge", DeletionReason.CLIENT_ONLY);
        //put("net.xolt.freecam.forge.FreecamForge", DeletionReason.CLIENT_ONLY);
        //put("com.buuz135.smithingtemplateviewer.SmithingTemplateViewer", DeletionReason.CLIENT_ONLY);
        //put("com.leclowndu93150.particular.Main", DeletionReason.CLIENT_ONLY);
        //put("dev.imb11.sounds.loaders.neoforge.SoundsNeoForge", DeletionReason.CLIENT_ONLY);
        //put("me.drex.crashexploitfixer.neoforge.CrashExploitFixerNeoforge", DeletionReason.DUPLICATE_FEATURE);
        //put("fabric-carpet-refmap", DeletionReason.FABRIC_ONLY);
        //put("carpet.CarpetServer", DeletionReason.DUPLICATE_FEATURE);
        put("com.axalotl.async.neoforge.AsyncNeoForge", DeletionReason.DUPLICATE_FEATURE);
        put("com.ishland.c2me.C2MEMod", DeletionReason.DUPLICATE_FEATURE);
    }};

    /**
     * Scan and remove incompatible mods
     */
    public static void deleteIncompatibleMods() {
        if (!YouerConfigUtil.AutoDeleteMods()) return;
        System.out.println(I18n.as("update.mods"));

        File modsDir = new File("mods");
        if (!modsDir.exists()) {
            modsDir.mkdir();
            return;
        }

        File[] jarFiles = modsDir.listFiles((dir, name) -> name.endsWith(".jar"));
        if (jarFiles == null || jarFiles.length == 0) return;

        // Collect JAR files that need to be deleted (deduplicated via Set)
        Set<File> modsToDelete = new HashSet<>();
        for (File jarFile : jarFiles) {
            for (String identifier : MOD_BLACKLIST.keySet()) {
                try {
                    if (identifier.contains(".")) {
                        String classPath = identifier.replace(".", "/") + ".class";
                        if (classExistsInJar(jarFile, classPath)) {
                            modsToDelete.add(jarFile);
                            break; // Found a match, no need to check other identifiers for this JAR
                        }
                    }
                } catch (Exception ignored) {
                }
            }
        }

        // Process each matched JAR file
        for (File modFile : modsToDelete) {
            // Find the matching reason (use the first identifier that matches)
            String matchedClass = null;
            for (String identifier : MOD_BLACKLIST.keySet()) {
                if (!identifier.contains(".")) continue;
                String classPath = identifier.replace(".", "/") + ".class";
                if (classExistsInJar(modFile, classPath)) {
                    matchedClass = identifier;
                    break;
                }
            }
            if (matchedClass != null) {
                backupAndDelete(modFile, matchedClass);
            }
        }
    }

    /**
     * Check if a class file exists inside a JAR file.
     * Uses try-with-resources to ensure the ZipFile is always properly closed,
     * preventing file handle leaks on Windows.
     *
     * @param jarFile   the JAR file to search in
     * @param classPath the internal path to the class file (e.g. "org/example/ModClass.class")
     * @return true if the class file exists in the JAR
     */
    private static boolean classExistsInJar(File jarFile, String classPath) {
        try (ZipFile zip = new ZipFile(jarFile)) {
            ZipEntry entry = zip.getEntry(classPath);
            return entry != null;
        } catch (IOException e) {
            return false;
        }
    }

    /**
     * Backup and delete MOD files
     *
     * @param modFile   the MOD file to process
     * @param className the full class name of the blacklisted mod
     */
    private static void backupAndDelete(File modFile, String className) {
        DeletionReason reason = MOD_BLACKLIST.getOrDefault(className, DeletionReason.UNKNOWN);

        File backupFile = new File("delete", modFile.getPath());

        // Ensure the backup parent directory exists
        File backupParentDir = backupFile.getParentFile();
        if (!backupParentDir.exists()) {
            backupParentDir.mkdirs();
        }

        // Remove old backup if it exists
        if (backupFile.exists()) {
            tryDeleteFile(backupFile);
        }

        // Copy mod to backup location
        try {
            Files.copy(modFile.toPath(), backupFile.toPath(), StandardCopyOption.REPLACE_EXISTING);
        } catch (IOException e) {
            System.err.println(I18n.as("update.backup.failed", modFile.getName(), e.getMessage()));
            return;
        }

        // Delete the original mod file with retry
        boolean deleted = tryDeleteFile(modFile);
        if (deleted) {
            System.out.println(I18n.as("update.deleting",
                    modFile.getName(),
                    reason.getDisplayText()
            ));
        } else {
            System.err.println(I18n.as("update.delete.failed", modFile.getName()));
        }
    }

    /**
     * Attempt to delete a file with retries.
     * On Windows, file handles may not be released immediately even after
     * the stream is closed, so we retry with GC hints and short delays.
     *
     * @param file the file to delete
     * @return true if the file was successfully deleted
     */
    private static boolean tryDeleteFile(File file) {
        final int MAX_RETRIES = 5;
        for (int attempt = 0; attempt < MAX_RETRIES; attempt++) {
            try {
                if (Files.deleteIfExists(file.toPath())) {
                    return true;
                }
                // File didn't exist in the first place — success
                if (!file.exists()) {
                    return true;
                }
            } catch (IOException e) {
                // File is still locked, wait and retry
            }

            if (attempt < MAX_RETRIES - 1) {
                System.gc();
                try {
                    Thread.sleep(200L * (attempt + 1)); // Increasing backoff: 200ms, 400ms, 600ms...
                } catch (InterruptedException ignored) {
                    Thread.currentThread().interrupt();
                    return false;
                }
            }
        }
        return false;
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
}
