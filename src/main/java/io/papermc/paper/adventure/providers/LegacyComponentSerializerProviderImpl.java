package io.papermc.paper.adventure.providers;

import io.papermc.paper.adventure.PaperAdventure;
import java.util.function.Consumer;
import net.kyori.adventure.text.serializer.legacy.LegacyComponentSerializer;
import org.jetbrains.annotations.NotNull;

@SuppressWarnings("UnstableApiUsage") // permitted provider
public class LegacyComponentSerializerProviderImpl implements LegacyComponentSerializer.Provider {

    @Override
    public @NotNull LegacyComponentSerializer legacyAmpersand() {
        return LegacyComponentSerializer.builder()
            .flattener(PaperAdventure.FLATTENER)
            .character(LegacyComponentSerializer.AMPERSAND_CHAR)
            .hexColors()
            .useUnusualXRepeatedCharacterHexFormat()
            .build();
    }

    @Override
    public @NotNull LegacyComponentSerializer legacySection() {
        return LegacyComponentSerializer.builder()
            .flattener(PaperAdventure.FLATTENER)
            .character(LegacyComponentSerializer.SECTION_CHAR)
            .hexColors()
            .useUnusualXRepeatedCharacterHexFormat()
            .build();
    }

    @Override
    public @NotNull Consumer<LegacyComponentSerializer.Builder> legacy() {
        return builder -> builder.flattener(PaperAdventure.FLATTENER);
    }
}
