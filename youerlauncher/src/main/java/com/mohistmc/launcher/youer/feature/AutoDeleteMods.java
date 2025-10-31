package com.mohistmc.launcher.youer.feature;

import com.mohistmc.launcher.youer.config.YouerConfigUtil;
import com.mohistmc.launcher.youer.util.I18n;
import com.mohistmc.tools.FileUtils;
import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.StandardCopyOption;
import java.util.ArrayList;
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
        put("me.wesley1808.servercore.common.ServerCore", DeletionReason.DUPLICATE_FEATURE);
        put("i18nupdatemod.I18nUpdateMod", DeletionReason.CLIENT_ONLY);
        put("dev.tr7zw.skinlayers.SkinLayersMod", DeletionReason.CLIENT_ONLY);
        put("com.biel.mod.mixin.VelocityMixin", DeletionReason.DUPLICATE_FEATURE);
        put("optifine.Differ", DeletionReason.CLIENT_ONLY);
        put("org.embeddedt.modernfix.ModernFix", DeletionReason.DUPLICATE_FEATURE);
        put("ca.spottedleaf.moonrise.neoforge.MoonriseNeoForge", DeletionReason.DUPLICATE_FEATURE);
        put("me.steinborn.krypton.mod.server.KryptonServerInitializer", DeletionReason.DUPLICATE_FEATURE);
        put("me.steinborn.krypton.mod.KryptonBootstrap", DeletionReason.DUPLICATE_FEATURE);
        put("org.thinkingstudio.krypton_foxified.KryptonFoxified", DeletionReason.DUPLICATE_FEATURE);
        put("one.pkg.mod.krypton_fnp.NeoModBootstrap", DeletionReason.DUPLICATE_FEATURE);
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
        //put("carpet.CarpetServer", DeletionReason.DUPLICATE_FEATURE);
    }};

    /**
     * Scan and remove incompatible mods
     */
    public static void deleteIncompatibleMods() {
        if (!YouerConfigUtil.AutoDeleteMods()) return;
        System.out.println(I18n.as("update.mods"));

        List<String> identifiers = new ArrayList<>(MOD_BLACKLIST.keySet());
        for (String identifier : identifiers) {
            try {
                checkModFile(identifier);
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
    }

    /**
     * Check and process individual mod files
     *
     * @param identifier of the class or file to be checked (can be a full class name or a file identifier)
     */
    private static void checkModFile(String identifier) {
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
                        backupAndDelete(jarFile, identifier);
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
    private static void backupAndDelete(File modFile, String className) throws Exception {
        DeletionReason reason = MOD_BLACKLIST.getOrDefault(className, DeletionReason.UNKNOWN);

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
        } catch (IOException e) {
            System.err.println("Failed to delete file: " + modFile.getName() + " - " + e.getMessage());
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
}
