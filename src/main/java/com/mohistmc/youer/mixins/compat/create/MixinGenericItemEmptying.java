package com.mohistmc.youer.mixins.compat.create;

import com.mohistmc.youer.feature.create.PotionBanConfig;
import com.simibubi.create.content.fluids.transfer.GenericItemEmptying;
import net.minecraft.core.component.DataComponents;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.alchemy.PotionContents;
import net.minecraft.world.level.Level;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/**
 * @author Mgazul
 * @date 2026/8/7 02:07
 */
@Mixin(GenericItemEmptying.class)
public class MixinGenericItemEmptying {

    @Inject(method = "canItemBeEmptied",at = @At("HEAD"), cancellable = true)
    private static void isBlockedPotion(Level world, ItemStack stack, CallbackInfoReturnable<Boolean> cir) {
        if (youer_1_21_1$isBlockedPotion(stack)) {
          cir.setReturnValue(false);
        }
    }

    // Blocked potion entries (the potion field of PotionContents). This blacklist blocks item emptying on the Create item drain, editable via /create_item_drain potionban (youer-config/item-drain-potionban.yml)
    @Unique
    private static boolean youer_1_21_1$isBlockedPotion(ItemStack stack) {
        PotionContents contents = stack.getOrDefault(DataComponents.POTION_CONTENTS, PotionContents.EMPTY);

        return contents.potion()
                .map(holder -> PotionBanConfig.getBlockedPotions().contains(holder.getRegisteredName()))
                .orElse(false);
    }
}
