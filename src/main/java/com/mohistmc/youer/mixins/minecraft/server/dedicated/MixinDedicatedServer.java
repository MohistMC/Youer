package com.mohistmc.youer.mixins.minecraft.server.dedicated;

import com.mohistmc.youer.api.ColorAPI;
import net.minecraft.server.dedicated.DedicatedServer;
import net.minecraft.server.dedicated.DedicatedServerSettings;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Overwrite;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * @author Mgazul
 * @date 2026/1/21 16:49
 */
@Mixin(DedicatedServer.class)
public class MixinDedicatedServer {

    @Shadow @Final
    private DedicatedServerSettings settings;

    @Inject(method = "showGui", at = @At("HEAD"), cancellable = true)
    private void youer$disableGui(CallbackInfo ci) {
      ci.cancel();
    }

    /**
     * @author
     * @reason
     */
    @Overwrite
    public String getMotd() {
        return ColorAPI.string(this.settings.getProperties().motd.get());
    }
}
