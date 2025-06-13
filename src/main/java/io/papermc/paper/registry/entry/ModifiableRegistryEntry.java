package io.papermc.paper.registry.entry;

import io.papermc.paper.registry.PaperRegistryBuilder;
import io.papermc.paper.registry.RegistryKey;
import io.papermc.paper.registry.TypedKey;
import io.papermc.paper.registry.data.util.Conversions;
import java.util.function.BiFunction;
import net.minecraft.core.Registry;
import net.minecraft.resources.ResourceKey;
import org.bukkit.Keyed;
import org.bukkit.NamespacedKey;

public class ModifiableRegistryEntry<M, T extends Keyed, B extends PaperRegistryBuilder<M, T>> extends CraftRegistryEntry<M, T> implements RegistryEntry.Modifiable<M, T, B> {

    protected final PaperRegistryBuilder.Filler<M, T, B> builderFiller;

    protected ModifiableRegistryEntry(
        final ResourceKey<? extends Registry<M>> mcKey,
        final RegistryKey<T> apiKey,
        final Class<?> toPreload,
        final BiFunction<NamespacedKey, M, T> minecraftToBukkit,
        final PaperRegistryBuilder.Filler<M, T, B> builderFiller
    ) {
        super(mcKey, apiKey, toPreload, minecraftToBukkit);
        this.builderFiller = builderFiller;
    }

    @Override
    public B fillBuilder(final Conversions conversions, final TypedKey<T> key, final M nms) {
        return this.builderFiller.fill(conversions, key, nms);
    }
}
