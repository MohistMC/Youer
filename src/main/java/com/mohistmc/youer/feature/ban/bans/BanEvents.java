package com.mohistmc.youer.feature.ban.bans;

import com.mohistmc.youer.YouerConfig;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.item.ItemStack;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.neoforge.event.entity.player.PlayerInteractEvent;

/**
 * @author Mgazul by MohistMC
 * @date 2023/8/9 20:09:51
 */
public class BanEvents {

    @SubscribeEvent
    public void onRightClickItem(PlayerInteractEvent.RightClickItem event) {
        if (!(event.getEntity() instanceof ServerPlayer player)) return;
        InteractionHand hand = event.getHand();
        ItemStack itemStack = player.getItemInHand(hand);
        if (BanItem.checkMoShou(player, itemStack)) {
            player.setItemInHand(hand, ItemStack.EMPTY);
            event.setCancellationResult(InteractionResult.FAIL);
            event.setCanceled(true);
            return;
        }
        if (BanItem.check(player, itemStack)) {
            event.setCancellationResult(InteractionResult.FAIL);
            event.setCanceled(true);
        }
    }

    public static boolean banFireTick() {
        return YouerConfig.doFireTick;
    }

    public static boolean banExplosion() {
        return YouerConfig.explosion;
    }

    public static boolean banFarmlandTrample() {
        return YouerConfig.farmlandTrample;
    }
}
