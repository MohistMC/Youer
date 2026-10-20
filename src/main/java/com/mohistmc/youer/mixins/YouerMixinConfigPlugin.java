package com.mohistmc.youer.mixins;

import java.util.List;
import java.util.Set;
import net.neoforged.fml.loading.LoadingModList;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.objectweb.asm.tree.ClassNode;
import org.spongepowered.asm.mixin.extensibility.IMixinConfigPlugin;
import org.spongepowered.asm.mixin.extensibility.IMixinInfo;

/**
 * Mixins below {@code compat.<modid>} are compiled against mods which are not part of the
 * runtime, so their targets are missing on a server without that mod. Mixin asks this plugin
 * before it resolves a declared target, so vetoing here is the only way to suppress both the
 * "target was not found" warning and the "Error loading class" warning in front of it.
 */
public class YouerMixinConfigPlugin implements IMixinConfigPlugin {

    private static final boolean DEBUG = Boolean.getBoolean("youer.debug");
    private static final Logger LOGGER = LogManager.getLogger("YouerMixinPlugin");
    private static final String COMPAT_PREFIX = "com.mohistmc.youer.mixins.compat.";

    @Override
    public void onLoad(String mixinPackage) {
    }

    @Override
    public String getRefMapperConfig() {
        return null;
    }

    @Override
    public boolean shouldApplyMixin(String targetClassName, String mixinClassName) {
        String modId = compatModId(mixinClassName);
        if (modId == null || !isModAbsent(modId)) {
            return true;
        }
        if (DEBUG) {
            LOGGER.info("Skipping {} (mod {} is not installed)", mixinClassName, modId);
        }
        return false;
    }

    /**
     * @return the modid encoded in the {@code compat.<modid>} package, or null when the mixin is
     *         not a compat mixin
     */
    static String compatModId(String mixinClassName) {
        if (!mixinClassName.startsWith(COMPAT_PREFIX)) {
            return null;
        }
        String modId = mixinClassName.substring(COMPAT_PREFIX.length());
        int separator = modId.indexOf('.');
        return separator < 0 ? null : modId.substring(0, separator);
    }

    /**
     * Mod discovery has already run when mixin builds its configs, but the loaded mod list has
     * not, so the discovery result is the only list to ask. Anything unexpected - the list not
     * being there yet or not being reachable from the transformer - reports the mod as present,
     * because dropping a compat mixin must never be caused by a failed lookup.
     */
    private static boolean isModAbsent(String modId) {
        try {
            LoadingModList loadingModList = LoadingModList.get();
            return loadingModList != null && loadingModList.getModFileById(modId) == null;
        } catch (Throwable th) {
            return false;
        }
    }

    @Override
    public void acceptTargets(Set<String> myTargets, Set<String> otherTargets) {
    }

    @Override
    public List<String> getMixins() {
        return null;
    }

    @Override
    public void preApply(String targetClassName, ClassNode targetClass, String mixinClassName, IMixinInfo mixinInfo) {
    }

    @Override
    public void postApply(String targetClassName, ClassNode targetClass, String mixinClassName, IMixinInfo mixinInfo) {
    }

}
