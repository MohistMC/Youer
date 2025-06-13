package io.papermc.paper.registry;

import com.mojang.serialization.Lifecycle;
import io.papermc.paper.adventure.PaperAdventure;
import io.papermc.paper.registry.data.util.Conversions;
import io.papermc.paper.registry.entry.RegistryEntry;
import io.papermc.paper.registry.event.WritableRegistry;
import java.util.Optional;
import java.util.function.BiFunction;
import java.util.function.Consumer;
import net.minecraft.core.MappedRegistry;
import net.minecraft.core.RegistrationInfo;
import net.minecraft.resources.ResourceKey;
import org.bukkit.Keyed;
import org.bukkit.NamespacedKey;
import org.bukkit.craftbukkit.CraftRegistry;
import org.bukkit.craftbukkit.util.ApiVersion;
import org.checkerframework.checker.nullness.qual.Nullable;

public class WritableCraftRegistry<M, T extends Keyed, B extends PaperRegistryBuilder<M, T>> extends CraftRegistry<T, M> {

    private static final RegistrationInfo FROM_PLUGIN = new RegistrationInfo(Optional.empty(), Lifecycle.experimental());

    private final RegistryEntry.BuilderHolder<M, T, B> entry;
    private final MappedRegistry<M> registry;
    private final PaperRegistryBuilder.Factory<M, T, ? extends B> builderFactory;
    private final BiFunction<? super NamespacedKey, M, T> minecraftToBukkit;

    public WritableCraftRegistry(
        final RegistryEntry.BuilderHolder<M, T, B> entry,
        final Class<?> classToPreload,
        final MappedRegistry<M> registry,
        final BiFunction<NamespacedKey, ApiVersion, NamespacedKey> serializationUpdater,
        final PaperRegistryBuilder.Factory<M, T, ? extends B> builderFactory,
        final BiFunction<? super NamespacedKey, M, T> minecraftToBukkit
    ) {
        super(classToPreload, registry, null, serializationUpdater);
        this.entry = entry;
        this.registry = registry;
        this.builderFactory = builderFactory;
        this.minecraftToBukkit = minecraftToBukkit;
    }

    public void register(final TypedKey<T> key, final Consumer<? super B> value, final Conversions conversions) {
        final ResourceKey<M> resourceKey = ResourceKey.create(this.registry.key(), PaperAdventure.asVanilla(key.key()));
        this.registry.validateWrite(resourceKey);
        final B builder = this.newBuilder(conversions, key);
        value.accept(builder);
        PaperRegistryListenerManager.INSTANCE.registerWithListeners(
            this.registry,
            RegistryEntry.Modifiable.asModifiable(this.entry),
            resourceKey,
            builder,
            FROM_PLUGIN,
            conversions
        );
    }

    @Override
    public final @Nullable T createBukkit(final NamespacedKey namespacedKey, final @Nullable M minecraft) {
        if (minecraft == null) {
            return null;
        }
        return this.minecraftToBukkit(namespacedKey, minecraft);
    }

    public WritableRegistry<T, B> createApiWritableRegistry(final Conversions conversions) {
        return new ApiWritableRegistry(conversions);
    }

    public T minecraftToBukkit(final NamespacedKey namespacedKey, final M minecraft) {
        return this.minecraftToBukkit.apply(namespacedKey, minecraft);
    }

    protected B newBuilder(final Conversions conversions, final TypedKey<T> key) {
        return this.builderFactory.create(conversions, key);
    }

    public class ApiWritableRegistry implements WritableRegistry<T, B> {

        private final Conversions conversions;

        public ApiWritableRegistry(final Conversions conversions) {
            this.conversions = conversions;
        }

        @Override
        public void register(final TypedKey<T> key, final Consumer<? super B> value) {
            WritableCraftRegistry.this.register(key, value, this.conversions);
        }
    }
}
