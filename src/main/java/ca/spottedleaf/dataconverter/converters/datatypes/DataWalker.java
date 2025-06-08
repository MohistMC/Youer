package ca.spottedleaf.dataconverter.converters.datatypes;

import ca.spottedleaf.dataconverter.types.MapType;

public interface DataWalker<T> {

    public T walk(final T data, final long fromVersion, final long toVersion);

}
