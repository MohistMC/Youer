package ca.spottedleaf.dataconverter.util;

import ca.spottedleaf.common.util.DecimalFormats;
import ca.spottedleaf.dataconverter.minecraft.MCDataConverter;
import ca.spottedleaf.dataconverter.minecraft.datatypes.MCDataType;
import ca.spottedleaf.dataconverter.minecraft.util.Version;
import com.mojang.datafixers.DSL;
import com.mojang.datafixers.DataFixer;
import com.mojang.logging.LogUtils;
import com.mojang.serialization.Dynamic;
import it.unimi.dsi.fastutil.longs.LongArrayList;
import it.unimi.dsi.fastutil.longs.LongComparator;
import java.util.Iterator;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.NbtOps;
import net.minecraft.util.datafix.DataFixTypes;
import net.minecraft.util.datafix.fixes.References;
import org.slf4j.Logger;

public final class ConvertUtil {

    private static final Logger LOGGER = LogUtils.getLogger();

    private static final boolean USE_DATACONVERTER = true;
    private static final boolean BENCHMARK = false;

    private static class BenchmarkData {

        public final LongArrayList allTimes = new LongArrayList();

    }

    private static final ConcurrentHashMap<String, BenchmarkData> BENCHMARK_DATA = new ConcurrentHashMap<>();

    // fromIndex inclusive, toIndex exclusive
    // will throw if arr.length == 0
    private static double median(final long[] arr, final int fromIndex, final int toIndex) {
        final int len = toIndex - fromIndex;
        final int middle = fromIndex + (len >>> 1);
        if ((len & 1) == 0) {
            // even, average the two middle points
            return (double)(arr[middle - 1] + arr[middle]) / 2.0;
        } else {
            // odd, just grab the middle
            return (double)arr[middle];
        }
    }

    private static final double NS_TO_US = 1.0 / (double)TimeUnit.MICROSECONDS.toNanos(1L);

    public static void resetAndPrintBenchmark() {
        if (!BENCHMARK) {
            throw new IllegalStateException("Not in benchmark mode");
        }

        if (BENCHMARK_DATA.isEmpty()) {
            LOGGER.info("No benchmark data recorded.");
            return;
        }

        for (final Iterator<Map.Entry<String, BenchmarkData>> iterator = BENCHMARK_DATA.entrySet().iterator(); iterator.hasNext();) {
            final Map.Entry<String, BenchmarkData> entry = iterator.next();

            final String typeName = entry.getKey();
            final BenchmarkData data = entry.getValue();

            // access to data after remove is guaranteed to be only from this thread
            iterator.remove();

            long sum = 0L;

            for (int i = 0; i < data.allTimes.size(); ++i) {
                sum += data.allTimes.getLong(i);
            }

            final double averageMS = NS_TO_US * ((double)sum / (double)data.allTimes.size());

            data.allTimes.unstableSort((LongComparator)null);

            final double medianMS = NS_TO_US * median(data.allTimes.elements(), 0, data.allTimes.size());

            LOGGER.info(
                "Benchmark data for " + typeName +
                    ": total: " + DecimalFormats.NO_DECIMAL_PLACES.get().format((long)data.allTimes.size()) +
                    ": mean: " + DecimalFormats.FOUR_DECIMAL_PLACES.get().format(averageMS) + "us" +
                    ". median: " + DecimalFormats.FOUR_DECIMAL_PLACES.get().format(medianMS) + "us"
            );
        }
    }

    private static DSL.TypeReference getDSLType(final DataFixTypes type) {
        switch (type) {
            case CHUNK: {
                return References.CHUNK;
            }
            case ENTITY_CHUNK: {
                return References.ENTITY_CHUNK;
            }
            case POI_CHUNK: {
                return References.POI_CHUNK;
            }
            case PLAYER: {
                return References.PLAYER;
            }
            case HOTBAR: {
                return References.HOTBAR;
            }
            case LEVEL: {
                return References.LEVEL;
            }
            case STATS: {
                return References.STATS;
            }
            case WORLD_GEN_SETTINGS: {
                return References.WORLD_GEN_SETTINGS;
            }
            case ADVANCEMENTS: {
                return References.ADVANCEMENTS;
            }
            case STRUCTURE: {
                return References.STRUCTURE;
            }
            case OPTIONS: {
                return References.OPTIONS;
            }

            default: {
                throw new IllegalArgumentException("Unknown type: " + type);
            }
        }
    }

    private static MCDataType getDCType(final DataFixTypes type) {
        return getDCType(getDSLType(type));
    }

    private static MCDataType getDCType(final DSL.TypeReference type) {
        if (type == References.PLAYER) {
            return ca.spottedleaf.dataconverter.minecraft.datatypes.MCTypeRegistry.PLAYER;
        }
        if (type == References.CHUNK) {
            return ca.spottedleaf.dataconverter.minecraft.datatypes.MCTypeRegistry.CHUNK;
        }
        if (type == References.STRUCTURE) {
            return ca.spottedleaf.dataconverter.minecraft.datatypes.MCTypeRegistry.STRUCTURE;
        }
        if (type == References.POI_CHUNK) {
            return ca.spottedleaf.dataconverter.minecraft.datatypes.MCTypeRegistry.POI_CHUNK;
        }
        if (type == References.ENTITY_CHUNK) {
            return ca.spottedleaf.dataconverter.minecraft.datatypes.MCTypeRegistry.ENTITY_CHUNK;
        }
        if (type == References.ITEM_STACK) {
            return ca.spottedleaf.dataconverter.minecraft.datatypes.MCTypeRegistry.ITEM_STACK;
        }
        if (type == References.ENTITY || type == References.ENTITY_TREE) {
            return ca.spottedleaf.dataconverter.minecraft.datatypes.MCTypeRegistry.ENTITY;
        }
        throw new IllegalArgumentException("Unknown type: " + type);
    }

    public static CompoundTag convertTag(final DSL.TypeReference type, final DataFixer fixer, final CompoundTag input, final int fromVersion) {
        return convertTag(type, fixer, input, fromVersion, Version.getCurrentVersion());
    }

    public static CompoundTag convertTag0(final DSL.TypeReference type, final DataFixer fixer, final CompoundTag input, final int fromVersion, final int toVersion) {
        if (USE_DATACONVERTER) {
            final MCDataType mcDataType = getDCType(type);

            return MCDataConverter.convertTag(mcDataType, input, fromVersion, toVersion);
        } else {
            return (CompoundTag)fixer.update(
                type, new Dynamic<>(NbtOps.INSTANCE, input), fromVersion, toVersion
            ).getValue();
        }
    }

    public static CompoundTag convertTag(final DSL.TypeReference type, final DataFixer fixer, final CompoundTag input, final int fromVersion, final int toVersion) {
        if (BENCHMARK) {
            final long start = System.nanoTime();

            final CompoundTag ret = convertTag0(type, fixer, input, fromVersion, toVersion);

            final long diff = System.nanoTime() - start;

            BENCHMARK_DATA.compute(type.typeName(), (final String typeName, BenchmarkData existing) -> {
                if (existing == null) {
                    existing = new BenchmarkData();
                }

                existing.allTimes.add(diff);

                return existing;
            });

            return ret;
        } else {
            return convertTag0(type, fixer, input, fromVersion, toVersion);
        }
    }

    public static CompoundTag convertTag(final DataFixTypes type, final DataFixer fixer, final CompoundTag input, final int fromVersion) {
        return convertTag(type, fixer, input, fromVersion, Version.getCurrentVersion());
    }

    public static CompoundTag convertTag(final DataFixTypes type, final DataFixer fixer, final CompoundTag input, final int fromVersion, final int toVersion) {
        return convertTag(getDSLType(type), fixer, input, fromVersion, toVersion);
    }

    private ConvertUtil() {}
}
