package ca.spottedleaf.dataconverter.converters.datatypes;

public interface DataWalker<T> {

    public T walk(final T data, final long fromVersion, final long toVersion);

}
