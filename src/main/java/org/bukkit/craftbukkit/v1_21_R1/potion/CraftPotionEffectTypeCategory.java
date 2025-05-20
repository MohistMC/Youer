package org.bukkit.craftbukkit.v1_21_R1.potion;

import com.google.common.base.Preconditions;
import net.minecraft.world.effect.MobEffectCategory;
import org.bukkit.potion.PotionEffectTypeCategory;

public final class CraftPotionEffectTypeCategory {

    public static PotionEffectTypeCategory minecraftToBukkit(MobEffectCategory minecraft) {
        Preconditions.checkArgument(minecraft != null);
        return PotionEffectTypeCategory.valueOf(minecraft.name());
    }

    public static MobEffectCategory bukkitToMinecraft(PotionEffectTypeCategory bukkit) {
        Preconditions.checkArgument(bukkit != null);
        return MobEffectCategory.valueOf(bukkit.name());
    }
}
