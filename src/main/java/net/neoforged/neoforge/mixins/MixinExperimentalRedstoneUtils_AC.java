package net.neoforged.neoforge.mixins;

import net.minecraft.core.Direction;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(Direction.class)
public class MixinExperimentalRedstoneUtils_AC {

    @Unique
    private static final int[] youer$UPDATE_ORDER_CLOCKWISE = { 2, 3, 0, 1, 4, 5 };

    @Inject(method = "getClockWise", at = @At("HEAD"), cancellable = true)
    private void youer$getClockWise(Direction.Axis axis, CallbackInfoReturnable<Direction> cir) {
        Direction self = (Direction)(Object)this;
        if (axis == Direction.Axis.Y) {
            cir.setReturnValue(Direction.from2DDataValue(youer$UPDATE_ORDER_CLOCKWISE[self.get2DDataValue()]));
        }
    }
}
