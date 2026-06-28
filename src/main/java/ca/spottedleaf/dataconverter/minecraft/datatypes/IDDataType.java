package ca.spottedleaf.dataconverter.minecraft.datatypes;

import ca.spottedleaf.dataconverter.converters.DataConverter;
import ca.spottedleaf.dataconverter.converters.datatypes.DataHook;
import ca.spottedleaf.dataconverter.converters.datatypes.DataWalker;
import ca.spottedleaf.dataconverter.minecraft.MCVersionRegistry;
import ca.spottedleaf.dataconverter.types.MapType;
import ca.spottedleaf.dataconverter.util.Long2ObjectArraySortedMap;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class IDDataType extends MCDataType {

    protected final Map<String, Long2ObjectArraySortedMap<List<DataWalker<MapType>>>> walkersById = new HashMap<>();

    public IDDataType(final String name) {
        super(name);
    }

    public void addConverterForId(final String id, final DataConverter<MapType, MapType> converter) {
        this.addStructureConverter(new DataConverter<>(converter.getToVersion(), converter.getVersionStep()) {
            @Override
            public MapType convert(final MapType data, final long sourceVersion, final long toVersion) {
                if (!id.equals(data.getString("id"))) {
                    return null;
                }
                return converter.convert(data, sourceVersion, toVersion);
            }
        });
    }

    public boolean hasWalkers(final String id) {
        return this.walkersById.containsKey(id);
    }

    public void addWalker(final int minVersion, final String id, final DataWalker<MapType> walker) {
        this.addWalker(minVersion, 0, id, walker);
    }

    public void addWalker(final int minVersion, final int versionStep, final String id, final DataWalker<MapType> walker) {
        this.walkersById.computeIfAbsent(id, (final String keyInMap) -> {
            return new Long2ObjectArraySortedMap<>();
        }).computeIfAbsent(DataConverter.encodeVersions(minVersion, versionStep), (final long keyInMap) -> {
            return new ArrayList<>();
        }).add(walker);
    }

    public void copyWalkers(final int minVersion, final String fromId, final String toId) {
        this.copyWalkers(minVersion, 0, fromId, toId);
    }

    public void copyWalkers(final int minVersion, final int versionStep, final String fromId, final String toId) {
        final long version = DataConverter.encodeVersions(minVersion, versionStep);
        final Long2ObjectArraySortedMap<List<DataWalker<MapType>>> walkersForId = this.walkersById.get(fromId);
        if (walkersForId == null) {
            return;
        }

        final List<DataWalker<MapType>> nearest = walkersForId.getFloor(version);

        if (nearest == null) {
            return;
        }

        for (final DataWalker<MapType> walker : nearest) {
            this.addWalker(minVersion, versionStep, toId, walker);
        }
    }

    @Override
    public MapType convert(MapType data, final long fromVersion, final long toVersion) {
        MapType ret = null;

        final List<DataConverter<MapType, MapType>> converters = this.structureConverters;
        for (int i = 0, len = converters.size(); i < len; ++i) {
            final DataConverter<MapType, MapType> converter = converters.get(i);
            final long converterVersion = converter.getEncodedVersion();

            if (converterVersion <= fromVersion) {
                continue;
            }

            if (converterVersion > toVersion) {
                break;
            }

            List<DataHook<MapType, MapType>> hooks = this.structureHooks.getFloor(converterVersion);

            if (hooks != null) {
                for (int k = 0, klen = hooks.size(); k < klen; ++k) {
                    final MapType replace = hooks.get(k).preHook(data, fromVersion, toVersion);
                    if (replace != null) {
                        ret = data = replace;
                    }
                }
            }

            final MapType replace = converter.convert(data, fromVersion, toVersion);
            if (replace != null) {
                ret = data = replace;
            }

            // possibly new data format, update hooks
            hooks = this.structureHooks.getFloor(toVersion);

            if (hooks != null) {
                for (int klen = hooks.size(), k = klen - 1; k >= 0; --k) {
                    final MapType postReplace = hooks.get(k).postHook(data, fromVersion, toVersion);
                    if (postReplace != null) {
                        ret = data = postReplace;
                    }
                }
            }
        }

        final List<DataHook<MapType, MapType>> hooks = this.structureHooks.getFloor(toVersion);

        // run pre hooks

        if (hooks != null) {
            for (int k = 0, klen = hooks.size(); k < klen; ++k) {
                final MapType replace = hooks.get(k).preHook(data, fromVersion, toVersion);
                if (replace != null) {
                    ret = data = replace;
                }
            }
        }

        // run all walkers

        final List<DataWalker<MapType>> walkers = this.structureWalkers.getFloor(toVersion);
        if (walkers != null) {
            for (int i = 0, len = walkers.size(); i < len; ++i) {
                final MapType replace = walkers.get(i).walk(data, fromVersion, toVersion);
                if (replace != null) {
                    ret = data = replace;
                }
            }
        }

        final Long2ObjectArraySortedMap<List<DataWalker<MapType>>> walkersByVersion = this.walkersById.get(data.getString("id"));
        if (walkersByVersion != null) {
            final List<DataWalker<MapType>> walkersForId = walkersByVersion.getFloor(toVersion);
            if (walkersForId != null) {
                for (int i = 0, len = walkersForId.size(); i < len; ++i) {
                    final MapType replace = walkersForId.get(i).walk(data, fromVersion, toVersion);
                    if (replace != null) {
                        ret = data = replace;
                    }
                }
            }
        }

        // run post hooks

        if (hooks != null) {
            for (int klen = hooks.size(), k = klen - 1; k >= 0; --k) {
                final MapType postReplace = hooks.get(k).postHook(data, fromVersion, toVersion);
                if (postReplace != null) {
                    ret = data = postReplace;
                }
            }
        }

        return ret;
    }
}
