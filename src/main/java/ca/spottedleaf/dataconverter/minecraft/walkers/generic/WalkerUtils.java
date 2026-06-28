package ca.spottedleaf.dataconverter.minecraft.walkers.generic;

import ca.spottedleaf.dataconverter.converters.datatypes.DataType;
import ca.spottedleaf.dataconverter.minecraft.converters.helpers.RenameHelper;
import ca.spottedleaf.dataconverter.minecraft.datatypes.MCDataType;
import ca.spottedleaf.dataconverter.minecraft.datatypes.MCValueType;
import ca.spottedleaf.dataconverter.types.ObjectType;
import ca.spottedleaf.dataconverter.types.ListType;
import ca.spottedleaf.dataconverter.types.MapType;
import java.util.ArrayList;

public final class WalkerUtils {

    public static void convert(final MCDataType type, final MapType data, final String path, final long fromVersion, final long toVersion) {
        if (data == null) {
            return;
        }

        final MapType map = data.getMap(path);
        if (map != null) {
            final MapType replace = type.convert(map, fromVersion, toVersion);
            if (replace != null) {
                data.setMap(path, replace);
            }
        }
    }

    public static void convertList(final MCDataType type, final MapType data, final String path, final long fromVersion, final long toVersion) {
        if (data == null) {
            return;
        }

        final ListType list = data.getListUnchecked(path);
        if (list != null) {
            for (int i = 0, len = list.size(); i < len; ++i) {
                final MapType listVal = list.getMap(i, null);
                if (listVal == null) {
                    continue;
                }

                final MapType replace = type.convert(listVal, fromVersion, toVersion);
                if (replace != null) {
                    list.setMap(i, replace);
                }
            }
        }
    }

    public static void convertListPath(final MCDataType type, final MapType data, final String listPath, final String elementPath,
                                       final long fromVersion, final long toVersion) {
        if (data == null) {
            return;
        }

        final ListType list = data.getListUnchecked(listPath);
        if (list != null) {
            for (int i = 0, len = list.size(); i < len; ++i) {
                WalkerUtils.convert(type, list.getMap(i, null), elementPath, fromVersion, toVersion);
            }
        }
    }

    public static void convertListPath(final MCDataType type, final MapType data, final String listPath, final String elementPath1,
                                       final String elementPath2, final long fromVersion, final long toVersion) {
        if (data == null) {
            return;
        }

        final ListType list = data.getListUnchecked(listPath);
        if (list != null) {
            for (int i = 0, len = list.size(); i < len; ++i) {
                final MapType listVal = list.getMap(i, null);
                if (listVal == null) {
                    continue;
                }

                WalkerUtils.convert(type, listVal.getMap(elementPath1), elementPath2, fromVersion, toVersion);
            }
        }
    }

    public static void convert(final DataType<Object, Object> type, final MapType data, final String path, final long fromVersion, final long toVersion) {
        if (data == null) {
            return;
        }

        final Object value = data.getGeneric(path);
        if (value != null) {
            final Object converted = type.convert(value, fromVersion, toVersion);
            if (converted != null) {
                data.setGeneric(path, converted);
            }
        }
    }

    public static void convert(final DataType<Object, Object> type, final ListType data, final long fromVersion, final long toVersion) {
        if (data == null) {
            return;
        }

        for (int i = 0, len = data.size(); i < len; ++i) {
            final Object value = data.getGeneric(i);
            final Object converted = type.convert(value, fromVersion, toVersion);
            if (converted != null) {
                data.setGeneric(i, converted);
            }
        }
    }

    public static void convertList(final DataType<Object, Object> type, final MapType data, final String path, final long fromVersion, final long toVersion) {
        if (data == null) {
            return;
        }

        final ListType list = data.getListUnchecked(path);
        if (list != null) {
            convert(type, list, fromVersion, toVersion);
        }
    }

    public static void convertListPath(final DataType<Object, Object> type, final MapType data, final String listPath, final String elementPath,
                                       final long fromVersion, final long toVersion) {
        if (data == null) {
            return;
        }

        final ListType list = data.getListUnchecked(listPath);
        if (list != null) {
            for (int i = 0, len = list.size(); i < len; ++i) {
                WalkerUtils.convert(type, list.getMap(i, null), elementPath, fromVersion, toVersion);
            }
        }
    }

    public static void convertListPath(final DataType<Object, Object> type, final MapType data, final String listPath, final String elementPath1,
                                       final String elementPath2, final long fromVersion, final long toVersion) {
        if (data == null) {
            return;
        }

        final ListType list = data.getListUnchecked(listPath);
        if (list != null) {
            for (int i = 0, len = list.size(); i < len; ++i) {
                final MapType listVal = list.getMap(i, null);
                if (listVal == null) {
                    continue;
                }

                WalkerUtils.convert(type, listVal.getMap(elementPath1), elementPath2, fromVersion, toVersion);
            }
        }
    }

    public static void convertKeys(final DataType<Object, Object> type, final MapType data, final String path, final long fromVersion, final long toVersion) {
        if (data == null) {
            return;
        }

        final MapType map = data.getMap(path);
        if (map != null) {
            convertKeys(type, map, fromVersion, toVersion);
        }
    }

    public static void convertKeys(final DataType<Object, Object> type, final MapType data, final long fromVersion, final long toVersion) {
        if (data == null) {
            return;
        }

        RenameHelper.renameKeys(data, (final String input) -> {
            return (String)type.convert(input, fromVersion, toVersion);
        });
    }

    public static void convertValues(final MCDataType type, final MapType data, final String path, final long fromVersion, final long toVersion) {
        if (data == null) {
            return;
        }

        convertValues(type, data.getMap(path), fromVersion, toVersion);
    }

    public static void convertValues(final MCDataType type, final MapType data, final long fromVersion, final long toVersion) {
        if (data == null) {
            return;
        }

        for (final String key : data.keys()) {
            final MapType value = data.getMap(key);
            if (value != null) {
                final MapType replace = type.convert(value, fromVersion, toVersion);
                if (replace != null) {
                    // no CME, key is in map already
                    data.setMap(key, replace);
                }
            }
        }
    }
}
