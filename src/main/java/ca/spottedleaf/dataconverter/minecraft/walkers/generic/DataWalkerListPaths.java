package ca.spottedleaf.dataconverter.minecraft.walkers.generic;

import ca.spottedleaf.converter.datatypes.DataType;
import ca.spottedleaf.converter.datatypes.DataWalker;
import ca.spottedleaf.converter.types.ListType;
import ca.spottedleaf.converter.types.MapType;

public class DataWalkerListPaths<T, R> extends DataWalker<MapType> {

    protected final DataType<T, R> type;
    protected final String[] paths;

    public DataWalkerListPaths(final DataType<T, R> type, final String... paths) {
        this.type = type;
        this.paths = paths;
    }

    @Override
    public final MapType walk(final MapType data, final long fromVersion, final long toVersion) {
        final DataType<T, R> type = this.type;
        for (final String path : this.paths) {
            final ListType list = data.getListUnchecked(path);
            if (list == null) {
                continue;
            }

            for (int i = 0, len = list.size(); i < len; ++i) {
                final Object current = list.getGeneric(i);
                final Object converted = type.convert((T)current, fromVersion, toVersion);
                if (converted != null) {
                    list.setGeneric(i, converted);
                }
            }
        }

        return null;
    }
}
