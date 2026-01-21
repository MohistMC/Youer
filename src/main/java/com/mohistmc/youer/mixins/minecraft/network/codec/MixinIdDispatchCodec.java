package com.mohistmc.youer.mixins.minecraft.network.codec;

import io.netty.buffer.ByteBuf;
import it.unimi.dsi.fastutil.objects.Object2IntMap;
import java.util.Objects;
import java.util.function.Function;
import net.minecraft.network.codec.IdDispatchCodec;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * @author Mgazul
 * @date 2026/1/21 19:40
 */
@Mixin(IdDispatchCodec.class)
public class MixinIdDispatchCodec {

    // @formatter:off
    @Final @Shadow private Function<Object, ?> typeGetter;
    @Final @Shadow private Object2IntMap<Object> toId;
    // @formatter:on

    @Inject(at = @At("HEAD"), method = "encode(Lio/netty/buffer/ByteBuf;Ljava/lang/Object;)V", cancellable = true)
    private void youer$encode(ByteBuf output, Object value, CallbackInfo info) {
        var packetId = this.typeGetter.apply(value);
        if (!this.toId.containsKey(packetId)) {
            if (Objects.equals(String.valueOf(packetId), "clientbound/minecraft:disconnect")) {
                info.cancel();
            }
        }
    }
}
