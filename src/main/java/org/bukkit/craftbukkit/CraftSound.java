package org.bukkit.craftbukkit;

import com.google.common.base.Preconditions;
import com.mohistmc.youer.neoforge.NeoForgeInjectBukkit;
import net.minecraft.core.Holder;
import net.minecraft.core.registries.Registries;
import net.minecraft.sounds.SoundEvent;
import org.bukkit.Registry;
import org.bukkit.Sound;
import org.bukkit.craftbukkit.util.CraftNamespacedKey;

public class CraftSound {

    public static Sound minecraftToBukkit(SoundEvent minecraft) {
        if(minecraft == null) {
            return null;
        }

        if (NeoForgeInjectBukkit.MODD_SOUNDS.containsKey(minecraft)) {
            return NeoForgeInjectBukkit.MODD_SOUNDS.get(minecraft);
        }

        try {
            net.minecraft.core.Registry<SoundEvent> registry = CraftRegistry.getMinecraftRegistry(Registries.SOUND_EVENT);
            Sound bukkit = Registry.SOUNDS.get(CraftNamespacedKey.fromMinecraft(registry.getResourceKey(minecraft).orElseThrow().location()));

            Preconditions.checkArgument(bukkit != null);
            return bukkit;
        } catch (Exception e) {
            return null;
        }
    }

    public static SoundEvent bukkitToMinecraft(Sound bukkit) {
        Preconditions.checkArgument(bukkit != null);
        if (NeoForgeInjectBukkit.MODD_SOUNDS.containsValue(bukkit)) {
            return NeoForgeInjectBukkit.MODD_SOUNDS.inverse().get(bukkit);
        }
        return CraftRegistry.getMinecraftRegistry(Registries.SOUND_EVENT)
                .getOptional(CraftNamespacedKey.toMinecraft(bukkit.getKey())).orElseThrow();
    }

    public static Holder<SoundEvent> bukkitToMinecraftHolder(Sound bukkit) {
        Preconditions.checkArgument(bukkit != null);

        net.minecraft.core.Registry<SoundEvent> registry = CraftRegistry.getMinecraftRegistry(Registries.SOUND_EVENT);

        if (registry.wrapAsHolder(CraftSound.bukkitToMinecraft(bukkit)) instanceof Holder.Reference<SoundEvent> holder) {
            return holder;
        }

        throw new IllegalArgumentException("No Reference holder found for " + bukkit
                + ", this can happen if a plugin creates its own sound effect with out properly registering it.");
    }

    // Paper start
    public static String getSound(Sound sound) {
        return sound.getKey().getKey();
    }
    // Paper end
}
