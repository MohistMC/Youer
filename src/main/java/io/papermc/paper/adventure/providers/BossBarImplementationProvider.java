package io.papermc.paper.adventure.providers;

import net.kyori.adventure.bossbar.BossBar;
import net.kyori.adventure.bossbar.BossBarImplementation;
import io.papermc.paper.adventure.BossBarImplementationImpl;
import org.jetbrains.annotations.NotNull;

@SuppressWarnings("UnstableApiUsage") // permitted provider
public class BossBarImplementationProvider implements BossBarImplementation.Provider {
    @Override
    public @NotNull BossBarImplementation create(final @NotNull BossBar bar) {
        return new BossBarImplementationImpl(bar);
    }
}
