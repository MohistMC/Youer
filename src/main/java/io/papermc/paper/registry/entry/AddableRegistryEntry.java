package io.papermc.paper.registry.entry;

import io.papermc.paper.registry.PaperRegistryBuilder;
import io.papermc.paper.registry.RegistryHolder;
import io.papermc.paper.registry.RegistryKey;
import io.papermc.paper.registry.TypedKey;
import io.papermc.paper.registry.WritableCraftRegistry;
import io.papermc.paper.registry.data.util.Conversions;
import java.util.function.BiFunction;
import net.minecraft.core.MappedRegistry;
import net.minecraft.core.Registry;
import net.minecraft.resources.ResourceKey;
import org.bukkit.Keyed;
import org.bukkit.NamespacedKey;

public class AddableRegistryEntry<M, T extends Keyed, B extends PaperRegistryBuilder<M, T>> extends CraftRegistryEntry<M, T> implements RegistryEntry.Addable<M, T, B> {

    private final PaperRegistryBuilder.Filler<M, T, B> builderFiller;

    protected AddableRegistryEntry(
        final ResourceKey<? extends Registry<M>> mcKey,
        final RegistryKey<T> apiKey,
        final Class<?> classToPreload,
        final BiFunction<NamespacedKey, M, T> minecraftToBukkit,
        final PaperRegistryBuilder.Filler<M, T, B> builderFiller
    ) {
        super(mcKey, apiKey, classToPreload, minecraftToBukkit);
        this.builderFiller = builderFiller;
    }

    private WritableCraftRegistry<M, T, B> createRegistry(final Registry<M> registry) {
        return new WritableCraftRegistry<>(this, this.classToPreload, (MappedRegistry<M>) registry, this.updater, this.builderFiller.asFactory(), this.minecraftToBukkit);
    }

    @Override
    public RegistryHolder<T> createRegistryHolder(final Registry<M> nmsRegistry) {
        return new RegistryHolder.Memoized<>(() -> this.createRegistry(nmsRegistry));
    }

    @Override
    public B fillBuilder(final Conversions conversions, final TypedKey<T> key, final M nms) {
        return this.builderFiller.fill(conversions, key, nms);
    }
}
