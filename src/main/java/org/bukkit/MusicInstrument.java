package org.bukkit;

import com.google.common.collect.Lists;
import java.util.Collection;
import java.util.Collections;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public abstract class MusicInstrument implements Keyed, net.kyori.adventure.translation.Translatable { // Paper - translation keys

    public static final MusicInstrument PONDER_GOAT_HORN = getInstrument("ponder_goat_horn");
    public static final MusicInstrument SING_GOAT_HORN = getInstrument("sing_goat_horn");
    public static final MusicInstrument SEEK_GOAT_HORN = getInstrument("seek_goat_horn");
    public static final MusicInstrument FEEL_GOAT_HORN = getInstrument("feel_goat_horn");
    public static final MusicInstrument ADMIRE_GOAT_HORN = getInstrument("admire_goat_horn");
    public static final MusicInstrument CALL_GOAT_HORN = getInstrument("call_goat_horn");
    public static final MusicInstrument YEARN_GOAT_HORN = getInstrument("yearn_goat_horn");
    public static final MusicInstrument DREAM_GOAT_HORN = getInstrument("dream_goat_horn");

    /**
     * Returns a {@link MusicInstrument} by a {@link NamespacedKey}.
     *
     * @param namespacedKey the key
     * @return the event or null
     * @deprecated Use {@link Registry#get(NamespacedKey)} instead.
     */
    @Nullable
    @Deprecated
    public static MusicInstrument getByKey(@NotNull NamespacedKey namespacedKey) {
        return Registry.INSTRUMENT.get(namespacedKey);
    }

    /**
     * Returns all known MusicInstruments.
     *
     * @return the memoryKeys
     * @deprecated use {@link Registry#iterator()}.
     */
    @NotNull
    @Deprecated
    public static Collection<MusicInstrument> values() {
        return Collections.unmodifiableCollection(Lists.newArrayList(Registry.INSTRUMENT));
    }

    @NotNull
    private static MusicInstrument getInstrument(@NotNull String key) {
        return Registry.INSTRUMENT.getOrThrow(NamespacedKey.minecraft(key));
    }

    // Paper start - deprecate getKey
    /**
     * @deprecated use {@link Registry#getKey(Keyed)} and {@link Registry#INSTRUMENT}. MusicInstruments
     * can exist without a key.
     */
    @Deprecated(forRemoval = true, since = "1.20.5")
    @Override
    public abstract @NotNull NamespacedKey getKey();
    // Paper end - deprecate getKey

    // Paper start - translation key
    @Override
    public @NotNull String translationKey() {
        return "instrument.minecraft." + this.getKey().value();
    }
    // Paper end - translation key
}
