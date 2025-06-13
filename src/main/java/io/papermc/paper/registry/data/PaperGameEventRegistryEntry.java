package io.papermc.paper.registry.data;

import io.papermc.paper.registry.PaperRegistryBuilder;
import io.papermc.paper.registry.data.util.Conversions;
import java.util.OptionalInt;
import net.minecraft.world.level.gameevent.GameEvent;
import org.checkerframework.checker.nullness.qual.NonNull;
import org.checkerframework.checker.nullness.qual.Nullable;
import org.checkerframework.framework.qual.DefaultQualifier;
import org.jetbrains.annotations.Range;

import static io.papermc.paper.registry.data.util.Checks.asArgumentMin;
import static io.papermc.paper.registry.data.util.Checks.asConfigured;

@DefaultQualifier(NonNull.class)
public class PaperGameEventRegistryEntry implements GameEventRegistryEntry {

    protected OptionalInt range = OptionalInt.empty();

    public PaperGameEventRegistryEntry(
        final Conversions ignoredConversions,
        final io.papermc.paper.registry.TypedKey<org.bukkit.GameEvent> ignoredKey,
        final @Nullable GameEvent nms
    ) {
        if (nms == null) return;

        this.range = OptionalInt.of(nms.notificationRadius());
    }

    @Override
    public @Range(from = 0, to = Integer.MAX_VALUE) int range() {
        return asConfigured(this.range, "range");
    }

    public static final class PaperBuilder extends PaperGameEventRegistryEntry implements GameEventRegistryEntry.Builder,
        PaperRegistryBuilder<GameEvent, org.bukkit.GameEvent> {

        public PaperBuilder(
            final Conversions conversions,
            final io.papermc.paper.registry.TypedKey<org.bukkit.GameEvent> key,
            final @Nullable GameEvent nms
        ) {
            super(conversions, key, nms);
        }

        @Override
        public GameEventRegistryEntry.Builder range(final @Range(from = 0, to = Integer.MAX_VALUE) int range) {
            this.range = OptionalInt.of(asArgumentMin(range, "range", 0));
            return this;
        }

        @Override
        public GameEvent build() {
            return new GameEvent(this.range());
        }
    }
}
