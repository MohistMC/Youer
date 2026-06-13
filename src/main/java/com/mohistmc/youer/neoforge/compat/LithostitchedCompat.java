package com.mohistmc.youer.neoforge.compat;

import com.google.common.base.Suppliers;
import dev.worldgen.lithostitched.Lithostitched;
import dev.worldgen.lithostitched.api.event.AddBiomeInjectorsEvent;
import dev.worldgen.lithostitched.api.event.AddRegionsEvent;
import dev.worldgen.lithostitched.api.tag.LithostitchedBiomeSourceTags;
import dev.worldgen.lithostitched.api.worldgen.util.DensityFunctionWrapper;
import dev.worldgen.lithostitched.impl.LithostitchedVersion;
import dev.worldgen.lithostitched.impl.worldgen.biomeinjector.internal.InjectorBiomeSource;
import dev.worldgen.lithostitched.mixin.common.BiomeSourceInvoker;
import dev.worldgen.lithostitched.mixin.common.ChunkGeneratorAccessor;
import dev.worldgen.lithostitched.mixin.common.RandomStateAccessor;
import dev.worldgen.lithostitched.api.registry.LithostitchedRegistries;
import dev.worldgen.lithostitched.api.worldgen.biomeinjector.BiomeInjector;
import dev.worldgen.lithostitched.impl.worldgen.biomeinjector.region.Region;
import net.minecraft.core.RegistryAccess;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.MinecraftServer;
import net.minecraft.world.level.biome.BiomeSource;
import net.minecraft.world.level.biome.FeatureSorter;
import net.minecraft.world.level.chunk.ChunkGenerator;
import net.minecraft.world.level.dimension.LevelStem;
import net.minecraft.world.level.levelgen.DensityFunction;
import net.minecraft.world.level.levelgen.NoiseBasedChunkGenerator;
import net.minecraft.world.level.levelgen.RandomState;

import java.util.*;

/**
 * Copy form <a href="https://github.com/Apollounknowndev/lithostitched/blob/8102667dc465377e19691e152493e0ca246f4ce6/src/common/main/java/dev/worldgen/lithostitched/impl/worldgen/biomeinjector/internal/BiomeInjectorManager.java#L33">...</a>
 *
 * @author Mgazul
 * @date 2026/6/13 14:35
 */
public class LithostitchedCompat {

    public static void applyBiomeInjectors(MinecraftServer server, ResourceKey<LevelStem> dimension, LevelStem levelStem) {
        RegistryAccess registries = server.registryAccess();

        long seed = LithostitchedVersion.getSeed(server);

        Map<ResourceLocation, BiomeInjector> injectors = new HashMap<>();
        registries.lookupOrThrow(LithostitchedRegistries.BIOME_INJECTOR).listElements().forEach(holder -> {
            if (holder.value().dimension().equals(dimension)) {
                injectors.put(holder.key().location(), holder.value());
            }
        });
        AddBiomeInjectorsEvent.EVENT.invoker().addInjectors(registries, (id, injector) -> {
            if (!injectors.containsKey(id) && injector.dimension().equals(dimension)) {
                injectors.put(id, injector);
            }
        });
        if (injectors.isEmpty()) return;

        ChunkGenerator generator = levelStem.generator();
        if (!(generator instanceof NoiseBasedChunkGenerator noiseGenerator)) return;

        RandomState randomState = RandomState.create(noiseGenerator.generatorSettings().value(), registries.lookupOrThrow(Registries.NOISE), seed);
        DensityFunctionWrapper noiseHelper = new DensityFunctionWrapper(
                seed,
                noiseGenerator.generatorSettings().value().useLegacyRandomSource(),
                randomState,
                ((RandomStateAccessor) (Object) randomState).getRandom()
        );

        Map<ResourceKey<Region>, Region> regions = new HashMap<>();
        registries
                .lookupOrThrow(LithostitchedRegistries.REGION)
                .listElements()
                .filter(holder -> holder.value().dimension().equals(dimension))
                .forEach(reference -> regions.put(reference.key(), reference.value()));
        AddRegionsEvent.EVENT.invoker().addRegions(registries, (key, level, biomes, weight) -> {
            Region region = Region.create(key, level, biomes, weight);
            if (!injectors.containsKey(key) && region.dimension().equals(dimension)) {
                regions.put(key, region);
            }
        });

        ResourceKey<DensityFunction> regionKey = ResourceKey.create(Registries.DENSITY_FUNCTION, createRegionId(dimension));
        Optional<DensityFunction> regionFunction = Lithostitched.registry(registries, Registries.DENSITY_FUNCTION).getOptional(regionKey);

        ChunkGeneratorAccessor accessor = (ChunkGeneratorAccessor) generator;
        BiomeSource currentSource = accessor.getBiomeSource();

        // Don't apply injections to biome sources in the #cannot_inject_into tag
        // By default, this includes `fixed` and `checkerboard`
        boolean canInject = true;
        for (var codec : BuiltInRegistries.BIOME_SOURCE.getTagOrEmpty(LithostitchedBiomeSourceTags.CANNOT_INJECT_INTO)) {
            if (codec.value().equals(((BiomeSourceInvoker) currentSource).getCodec())) {
                canInject = false;
                break;
            }
        }
        if (!canInject) return;

        InjectorBiomeSource injectorSource = currentSource instanceof InjectorBiomeSource injector ? injector : new InjectorBiomeSource(accessor.getBiomeSource());
        injectorSource.applyInjectors(injectors, regionFunction, regions, noiseHelper);
        accessor.setBiomeSource(injectorSource);
        accessor.setFeaturesPerStep(Suppliers.memoize(() ->
                FeatureSorter.buildFeaturesPerStep(List.copyOf(injectorSource.possibleBiomes()), biome -> accessor.getGetter().apply(biome).features(), true)
        ));
        Lithostitched.debug("Applying {} biome injections for dimension {}", injectors.size(), dimension.location());
    }

    private static ResourceLocation createRegionId(ResourceKey<LevelStem> dimension) {
        return Lithostitched.vanillaToLithostitched(dimension.location()).withPrefix("region/");
    }
}
