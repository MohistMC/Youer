package com.mohistmc.youer;

import java.util.HashSet;
import java.util.List;
import java.util.Set;
import org.objectweb.asm.tree.ClassNode;
import org.spongepowered.asm.mixin.Mixins;
import org.spongepowered.asm.mixin.extensibility.IMixinConfigPlugin;
import org.spongepowered.asm.mixin.extensibility.IMixinInfo;

/**
 * @author Mgazul
 * @date 2026/7/20 18:29
 */
public class YouerMixinPlugin implements IMixinConfigPlugin {

    public Set<String> mixins = new HashSet<String>();
    @Override
    public void onLoad(String mixinPackage) {
        Youer.LOGGER.info("YouerMixinPlugin loaded");
        mixins.add("dev.architectury.mixin.neoforge.neoforge.MixinChunkMap");
        Mixins.addBlacklist(mixins);
    }

    @Override
    public String getRefMapperConfig() {
        return "";
    }

    @Override
    public boolean shouldApplyMixin(String targetClassName, String mixinClassName) {
        if (mixinClassName.equals("com.mohistmc.youer.mixins.compat.spark.MixinNeoForgeWorldInfoProvider")) {
            return hasClass("me.lucko.spark.neoforge.NeoForgeSparkMod");
        }
        return true;
    }

    @Override
    public void acceptTargets(Set<String> myTargets, Set<String> otherTargets) {

    }

    @Override
    public List<String> getMixins() {
        return List.of();
    }

    @Override
    public void preApply(String targetClassName, ClassNode targetClass, String mixinClassName, IMixinInfo mixinInfo) {

    }

    @Override
    public void postApply(String targetClassName, ClassNode targetClass, String mixinClassName, IMixinInfo mixinInfo) {
    }

    public boolean hasClass(String className) {
        try {
            Class.forName(className);
            return true;
        } catch (ClassNotFoundException e) {
            return false;
        }
    }
}
