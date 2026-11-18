package com.mohistmc.youer.mixins.compat.spark;

import net.minecraft.server.level.ServerLevel;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/**
 * Youer - mod compat: Paper's chunk system replaced ServerLevel#entityManager with the moonrise entity lookup,
 * so spark's NeoForge world info provider died on every metrics poll with
 * <pre>
 * NoSuchFieldError: Class net.minecraft.server.level.ServerLevel does not have member field
 * 'net.minecraft.world.level.entity.PersistentEntitySectionManager entityManager'
 * </pre>
 * and spark logged "Uncaught exception thrown in thread spark-monitoring-..." while the tps/entity counters stayed
 * empty. Answer the count from the chunk system instead; ServerLevel#getEntityCount() reads the moonrise
 * EntityLookup, which is where every entity lives on this server.
 * <p>
 * The target is a spark class, so it is addressed by name - spark is an optional mod here and the mixin is gated
 * in {@link com.mohistmc.youer.YouerMixinPlugin}.
 *
 * @author Mgazul
 */
@Mixin(targets = "me.lucko.spark.neoforge.NeoForgeWorldInfoProvider$Server")
public class MixinNeoForgeWorldInfoProvider {

    @Inject(method = "countEntities", at = @At("HEAD"), cancellable = true)
    private void youer$countEntities(ServerLevel level, CallbackInfoReturnable<Integer> cir) {
        cir.setReturnValue(level.getEntityCount());
    }
}
