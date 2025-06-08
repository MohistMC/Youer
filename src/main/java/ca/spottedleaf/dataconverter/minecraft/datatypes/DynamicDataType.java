package ca.spottedleaf.dataconverter.minecraft.datatypes;

import ca.spottedleaf.dataconverter.converters.DataConverter;
import ca.spottedleaf.dataconverter.converters.datatypes.DataHook;
import ca.spottedleaf.dataconverter.converters.datatypes.DataType;
import ca.spottedleaf.dataconverter.converters.datatypes.DataWalker;
import ca.spottedleaf.dataconverter.minecraft.MCVersionRegistry;
import ca.spottedleaf.dataconverter.util.Long2ObjectArraySortedMap;
import java.util.ArrayList;
import java.util.List;

public class DynamicDataType extends DataType<Object, Object> {

    public final String name;

    protected final ArrayList<DataConverter<Object, Object>> structureConverters = new ArrayList<>();
    protected final Long2ObjectArraySortedMap<List<DataWalker<Object>>> structureWalkers = new Long2ObjectArraySortedMap<>();
    protected final Long2ObjectArraySortedMap<List<DataHook<Object, Object>>> structureHooks = new Long2ObjectArraySortedMap<>();

    public DynamicDataType(final String name) {
        this.name = name;
    }

    public void addStructureConverter(final DataConverter<Object, Object> converter) {
        MCVersionRegistry.checkVersion(converter.getEncodedVersion());
        this.structureConverters.add(converter);
        this.structureConverters.sort(DataConverter.LOWEST_VERSION_COMPARATOR);
    }

    public void addStructureWalker(final int minVersion, final DataWalker<Object> walker) {
        this.addStructureWalker(minVersion, 0, walker);
    }

    public void addStructureWalker(final int minVersion, final int versionStep, final DataWalker<Object> walker) {
        this.structureWalkers.computeIfAbsent(DataConverter.encodeVersions(minVersion, versionStep), (final long keyInMap) -> {
            return new ArrayList<>();
        }).add(walker);
    }

    public void addStructureHook(final int minVersion, final DataHook<Object, Object> hook) {
        this.addStructureHook(minVersion, 0, hook);
    }

    public void addStructureHook(final int minVersion, final int versionStep, final DataHook<Object, Object> hook) {
        this.structureHooks.computeIfAbsent(DataConverter.encodeVersions(minVersion, versionStep), (final long keyInMap) -> {
            return new ArrayList<>();
        }).add(hook);
    }

    @Override
    public Object convert(Object data, final long fromVersion, final long toVersion) {
        Object ret = null;

        final List<DataConverter<Object, Object>> converters = this.structureConverters;
        for (int i = 0, len = converters.size(); i < len; ++i) {
            final DataConverter<Object, Object> converter = converters.get(i);
            final long converterVersion = converter.getEncodedVersion();

            if (converterVersion <= fromVersion) {
                continue;
            }

            if (converterVersion > toVersion) {
                break;
            }

            List<DataHook<Object, Object>> hooks = this.structureHooks.getFloor(converterVersion);

            if (hooks != null) {
                for (int k = 0, klen = hooks.size(); k < klen; ++k) {
                    final Object replace = hooks.get(k).preHook(data, fromVersion, toVersion);
                    if (replace != null) {
                        ret = data = replace;
                    }
                }
            }

            final Object replace = converter.convert(data, fromVersion, toVersion);
            if (replace != null) {
                ret = data = replace;
            }

            // possibly new data format, update hooks
            hooks = this.structureHooks.getFloor(toVersion);

            if (hooks != null) {
                for (int klen = hooks.size(), k = klen - 1; k >= 0; --k) {
                    final Object postReplace = hooks.get(k).postHook(data, fromVersion, toVersion);
                    if (postReplace != null) {
                        ret = data = postReplace;
                    }
                }
            }
        }

        final List<DataHook<Object, Object>> hooks = this.structureHooks.getFloor(toVersion);

        if (hooks != null) {
            for (int k = 0, klen = hooks.size(); k < klen; ++k) {
                final Object replace = hooks.get(k).preHook(data, fromVersion, toVersion);
                if (replace != null) {
                    ret = data = replace;
                }
            }
        }

        final List<DataWalker<Object>> walkers = this.structureWalkers.getFloor(toVersion);
        if (walkers != null) {
            for (int i = 0, len = walkers.size(); i < len; ++i) {
                final Object replace = walkers.get(i).walk(data, fromVersion, toVersion);
                if (replace != null) {
                    ret = data = replace;
                }
            }
        }

        if (hooks != null) {
            for (int klen = hooks.size(), k = klen - 1; k >= 0; --k) {
                final Object postReplace = hooks.get(k).postHook(data, fromVersion, toVersion);
                if (postReplace != null) {
                    ret = data = postReplace;
                }
            }
        }

        return ret;
    }
}
