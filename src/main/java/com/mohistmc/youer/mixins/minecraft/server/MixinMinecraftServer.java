package com.mohistmc.youer.mixins.minecraft.server;

import net.minecraft.server.MinecraftServer;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;

/**
 * @author Mgazul
 * @date 2026/2/27 15:55
 */
@Mixin(MinecraftServer.class)
public abstract class MixinMinecraftServer {

    /**
     * EssentialsX {@code ReflServerStateProvider} uses {@code MethodHandles.findVirtual} for Spigot-obfuscated
     * {@code boolean MinecraftServer#z()} on 1.21.7+; Mojang-mapped code exposes the same behaviour as {@link #isRunning()}.
     */
    public boolean z() {
        return this.isRunning();
    }

    @Shadow
    public abstract boolean isRunning();
}
