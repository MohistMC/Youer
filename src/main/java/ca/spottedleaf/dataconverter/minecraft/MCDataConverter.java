package ca.spottedleaf.dataconverter.minecraft;

import ca.spottedleaf.converter.DataConverter;
import ca.spottedleaf.converter.datatypes.DataType;
import ca.spottedleaf.dataconverter.minecraft.datatypes.MCDataType;
import ca.spottedleaf.dataconverter.minecraft.versions.V99;
import ca.spottedleaf.dataconverter.types.json.JsonMapType;
import ca.spottedleaf.dataconverter.types.nbt.NBTMapType;
import com.google.gson.JsonObject;
import com.mojang.datafixers.DSL;
import com.mojang.datafixers.DataFixer;
import com.mojang.logging.LogUtils;
import com.mojang.serialization.Dynamic;
import it.unimi.dsi.fastutil.longs.LongArrayList;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.NbtOps;
import net.minecraft.util.datafix.DataFixTypes;
import net.minecraft.util.datafix.DataFixers;
import org.slf4j.Logger;

public final class MCDataConverter {

    private static final LongArrayList BREAKPOINTS = MCVersionRegistry.getBreakpoints();
    private static final Logger LOGGER = LogUtils.getLogger();
    private static boolean warnedForPassOn;

    public static <T> T copy(final T type) {
        if (type instanceof CompoundTag) {
            return (T)((CompoundTag)type).copy();
        } else if (type instanceof JsonObject) {
            return (T)((JsonObject)type).deepCopy();
        }

        return type;
    }

    public static CompoundTag convertTag(final MCDataType type, final DataFixTypes vanillaType, final CompoundTag data, final int fromVersion, final int toVersion) {
        final int maxVersion = MCVersionRegistry.getMaxImplementedVersion();
        if (toVersion > maxVersion) {
            warnPassOn(toVersion);

            // Convert what we can first
            final CompoundTag converted = fromVersion < maxVersion ? convertTag(type, data, fromVersion, maxVersion) : data;

            // Convert the rest via Vanilla fixers
            return vanillaType.update(DataFixers.getDataFixer(), converted, Math.max(fromVersion, maxVersion), toVersion);
        }

        return convertTag(type, data, fromVersion, toVersion);
    }

    public static CompoundTag convertTag(final MCDataType type, final DSL.TypeReference vanillaType, final DataFixer fixer, final CompoundTag data, final int fromVersion, final int toVersion) {
        final int maxVersion = MCVersionRegistry.getMaxImplementedVersion();
        if (toVersion > maxVersion) {
            warnPassOn(toVersion);

            // Convert what we can first
            final CompoundTag converted = fromVersion < maxVersion ? convertTag(type, data, fromVersion, maxVersion) : data;

            // Convert the rest via Vanilla fixers
            return (CompoundTag)fixer.update(
                vanillaType,
                new Dynamic<>(NbtOps.INSTANCE, converted),
                Math.max(fromVersion, maxVersion), toVersion
            ).getValue();
        }

        return convertTag(type, data, fromVersion, toVersion);
    }

    private static void warnPassOn(final int toVersion) {
        if (!warnedForPassOn) {
            warnedForPassOn = true;
            LOGGER.warn("Passing on conversion to Vanilla converters to version: {}", toVersion);
        }
    }

    public static CompoundTag convertTag(final MCDataType type, final CompoundTag data, final int fromVersion, final int toVersion) {
        final NBTMapType wrapped = new NBTMapType(data);

        final NBTMapType replaced = (NBTMapType)convert(type, wrapped, fromVersion, toVersion);

        return replaced == null ? wrapped.getTag() : replaced.getTag();
    }

    public static JsonObject convertJson(final MCDataType type, final JsonObject data, final boolean compressed, final int fromVersion, final int toVersion) {
        final JsonMapType wrapped = new JsonMapType(data, compressed);

        final JsonMapType replaced = (JsonMapType)convert(type, wrapped, fromVersion, toVersion);

        return replaced == null ? wrapped.getJson() : replaced.getJson();
    }

    public static <T, R> R convert(final DataType<T, R> type, final T data, final int fromVersion, final int toVersion) {
        return convertWithSubVersion(
            type, data,
            DataConverter.encodeVersions(Math.max(fromVersion, V99.VERSION), Integer.MAX_VALUE),
            DataConverter.encodeVersions(toVersion, Integer.MAX_VALUE)
        );
    }

    public static <T, R> R convertWithSubVersion(final DataType<T, R> type, final T data, final long fromVersion, final long toVersion) {
        Object ret = data;

        long currentVersion = fromVersion;

        for (int i = 0, len = BREAKPOINTS.size(); i < len; ++i) {
            final long breakpoint = BREAKPOINTS.getLong(i);

            if (currentVersion >= breakpoint) {
                continue;
            }

            final Object converted = type.convert((T)ret, currentVersion, Math.min(toVersion, breakpoint - 1L));
            if (converted != null) {
                ret = converted;
            }

            currentVersion = Math.min(toVersion, breakpoint - 1L);

            if (currentVersion == toVersion) {
                break;
            }
        }

        if (currentVersion != toVersion) {
            final Object converted = type.convert((T)ret, currentVersion, toVersion);
            if (converted != null) {
                ret = converted;
            }
        }

        return (R)ret;
    }

    private MCDataConverter() {}
}
