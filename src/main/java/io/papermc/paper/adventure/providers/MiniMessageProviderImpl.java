package io.papermc.paper.adventure.providers;

import java.util.function.Consumer;
import net.kyori.adventure.text.minimessage.MiniMessage;
import org.jetbrains.annotations.NotNull;

@SuppressWarnings("UnstableApiUsage") // permitted provider
public class MiniMessageProviderImpl implements MiniMessage.Provider {

    @Override
    public @NotNull MiniMessage miniMessage() {
        return MiniMessage.builder().build();
    }

    @Override
    public @NotNull Consumer<MiniMessage.Builder> builder() {
        return builder -> {};
    }
}
