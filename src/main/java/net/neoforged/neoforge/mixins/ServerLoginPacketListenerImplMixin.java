package net.neoforged.neoforge.mixins;

import com.mohistmc.youer.util.I18n;
import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import io.papermc.paper.configuration.GlobalConfiguration;
import java.lang.reflect.Field;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.protocol.login.ServerboundCustomQueryAnswerPacket;
import net.minecraft.server.network.ServerLoginPacketListenerImpl;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(ServerLoginPacketListenerImpl.class)
public abstract class ServerLoginPacketListenerImplMixin {

    // @formatter:off
    @Shadow public int velocityLoginMessageId;
    @Shadow public abstract void disconnect(String reason);
    @Shadow public abstract  void youer$handleCustomQueryPacket(FriendlyByteBuf payload, String type);
    // @formatter:on

    @Inject(method = "handleCustomQueryPacket",at = @At("HEAD"), cancellable = true)
    private void ffAPI(ServerboundCustomQueryAnswerPacket p_295398_, CallbackInfo ci) {
        if (GlobalConfiguration.get().proxies.velocity.enabled && p_295398_.transactionId() == this.velocityLoginMessageId) {
            if (p_295398_.payload() instanceof ServerboundCustomQueryAnswerPacket.QueryAnswerPayload(ByteBuf buffer)) {
                youer$handleCustomQueryPacket(new FriendlyByteBuf(buffer), "Youer");
            } else {
                try {
                    Field payloadField = ServerboundCustomQueryAnswerPacket.class.getDeclaredField("payload");
                    payloadField.setAccessible(true);
                    Object payloadObj = payloadField.get(p_295398_);

                    Class<?> queryAnswerPayloadClass = Class.forName("net.fabricmc.fabric.impl.networking.payload.PacketByteBufLoginQueryResponse");
                    if (queryAnswerPayloadClass.isInstance(payloadObj)) {
                        Field bufferField = queryAnswerPayloadClass.getDeclaredField("data");
                        bufferField.setAccessible(true);
                        FriendlyByteBuf buffer = (FriendlyByteBuf) bufferField.get(payloadObj);
                        youer$handleCustomQueryPacket(new FriendlyByteBuf(Unpooled.wrappedBuffer(Unpooled.copyBoolean(true), buffer.slice())), "Fabric api");
                    }
                } catch (Exception e) {
                    this.disconnect(I18n.as("velocity.requires"));
                }
            }
            ci.cancel();
        }
    }
}
