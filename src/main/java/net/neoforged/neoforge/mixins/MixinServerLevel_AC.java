package net.neoforged.neoforge.mixins;

import alternate.current.wire.WireHandler;
import net.minecraft.server.level.ServerLevel;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(ServerLevel.class)
public class MixinServerLevel_AC implements IWireHandler {

    @Unique
    private WireHandler youer$wireHandler;

    @Inject(method = "<init>", at = @At("RETURN"))
    private void youer$init(CallbackInfo ci) {
        this.youer$wireHandler = new WireHandler((ServerLevel)(Object)this);
    }

    @Override
    public WireHandler youer$getWireHandler() {
        return this.youer$wireHandler;
    }
}
