package cn.mohistmc.youer.proxy;

import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import io.papermc.paper.configuration.GlobalConfiguration;
import java.lang.reflect.Field;
import net.minecraft.network.Connection;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.protocol.login.ServerboundCustomQueryAnswerPacket;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.network.ServerLoginPacketListenerImpl;

public class VelocityLoginPacketListenerImpl extends ServerLoginPacketListenerImpl {

    public VelocityLoginPacketListenerImpl(MinecraftServer server, Connection connection, boolean transferred) {
        super(server, connection, transferred);
    }

    @Override
    public void handleCustomQueryPacket(ServerboundCustomQueryAnswerPacket p_295398_) {
        // Paper start - Velocity support
        if (GlobalConfiguration.get().proxies.velocity.enabled && p_295398_.transactionId() == this.velocityLoginMessageId) {
            if (p_295398_.payload() instanceof ServerboundCustomQueryAnswerPacket.QueryAnswerPayload(
                    ByteBuf buffer
            )) {
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
                    LOGGER.error("Reflection error handling custom query packet", e);
                    this.disconnect("Internal server error");
                }
            }
        } else {
           super.handleCustomQueryPacket(p_295398_);
        }
    }
}
