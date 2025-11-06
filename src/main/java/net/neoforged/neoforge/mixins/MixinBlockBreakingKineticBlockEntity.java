package net.neoforged.neoforge.mixins;

import com.mohistmc.youer.api.event.block.BlockSetBlockEvent;
import com.simibubi.create.content.kinetics.base.BlockBreakingKineticBlockEntity;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.block.state.BlockState;
import org.bukkit.Location;
import org.bukkit.craftbukkit.util.CraftLocation;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(BlockBreakingKineticBlockEntity.class)
public class MixinBlockBreakingKineticBlockEntity {

    @Shadow protected BlockPos breakingPos;

    @Inject(method = "onBlockBroken",at = @At("HEAD"), cancellable = true)
    private void ffAPI(BlockState stateToBreak, CallbackInfo ci) {
        var blockEntity = ((BlockBreakingKineticBlockEntity)(Object)this);
        var pos = blockEntity.getBlockPos();
        var world = blockEntity.getLevel();
        if (world == null) {
            return;
        }
        if (BlockSetBlockEvent.getHandlerList().getRegisteredListeners().length > 0) {
            Location sourceLocation = CraftLocation.toBukkit(pos, world);
            Location location = CraftLocation.toBukkit(breakingPos, world);

            BlockSetBlockEvent event = new BlockSetBlockEvent(sourceLocation, location);
            if (!event.callEvent()) {
                ci.cancel();
            }
        }
    }
}
