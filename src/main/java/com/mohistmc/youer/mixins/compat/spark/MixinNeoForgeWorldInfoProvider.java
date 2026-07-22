package com.mohistmc.youer.mixins.compat.spark;

import me.lucko.spark.neoforge.NeoForgeWorldInfoProvider;
import net.minecraft.server.level.ServerLevel;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/**
 * @author Mgazul
 * @date 2026/7/21 19:46
 */
@Mixin(NeoForgeWorldInfoProvider.Server.class)
public class MixinNeoForgeWorldInfoProvider {

    @Inject(method = "countEntities", at = @At("HEAD"), cancellable = true)
    private void youer$countEntities(ServerLevel level, CallbackInfoReturnable<Integer> cir) {
        int count = level.getEntityCount();
       cir.setReturnValue(count);
    }
}
