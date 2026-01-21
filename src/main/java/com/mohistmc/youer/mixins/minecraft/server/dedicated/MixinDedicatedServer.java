package com.mohistmc.youer.mixins.minecraft.server.dedicated;

import net.minecraft.server.dedicated.DedicatedServer;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * @author Mgazul
 * @date 2026/1/21 16:49
 */
@Mixin(DedicatedServer.class)
public class MixinDedicatedServer {

    @Inject(method = "showGui", at = @At("HEAD"), cancellable = true)
    private void youer$disableGui(CallbackInfo ci) {
      ci.cancel();
    }
}
