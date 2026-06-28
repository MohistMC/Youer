package net.neoforged.neoforge.mixins;

import alternate.current.wire.WireHandler;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.RedStoneWireBlock;
import net.minecraft.world.level.block.state.BlockState;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(RedStoneWireBlock.class)
public class MixinRedStoneWireBlock_AC {

    @Inject(method = "onPlace", at = @At("HEAD"))
    private void youer$onPlace(BlockState state, Level level, BlockPos pos, BlockState oldState, boolean movedByPiston, CallbackInfo ci) {
        if (level.isClientSide) return;
        if (oldState.is(Blocks.REDSTONE_WIRE)) return;
        if (!(level instanceof ServerLevel serverLevel)) return;
        WireHandler handler = ((IWireHandler) serverLevel).youer$getWireHandler();
        if (handler != null) {
            handler.onWireAdded(pos);
        }
    }

    @Inject(method = "onRemove", at = @At("HEAD"))
    private void youer$onRemove(BlockState state, Level level, BlockPos pos, BlockState newState, boolean movedByPiston, CallbackInfo ci) {
        if (level.isClientSide) return;
        if (movedByPiston) return;
        if (!(level instanceof ServerLevel serverLevel)) return;
        WireHandler handler = ((IWireHandler) serverLevel).youer$getWireHandler();
        if (handler != null) {
            handler.onWireRemoved(pos, state);
        }
    }

    @Inject(method = "neighborChanged", at = @At("HEAD"))
    private void youer$neighborChanged(BlockState state, Level level, BlockPos pos, Block block, BlockPos fromPos, boolean isMoving, CallbackInfo ci) {
        if (level.isClientSide) return;
        if (!(level instanceof ServerLevel serverLevel)) return;
        WireHandler handler = ((IWireHandler) serverLevel).youer$getWireHandler();
        if (handler != null) {
            handler.onWireUpdated(pos);
        }
    }
}
