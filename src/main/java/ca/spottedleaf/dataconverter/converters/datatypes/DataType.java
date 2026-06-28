package ca.spottedleaf.dataconverter.converters.datatypes;

public abstract class DataType<T, R> {

    public final R convertOrOriginal(final T data, final long fromVersion, final long toVersion) {
        final R replaced = this.convert(data, fromVersion, toVersion);
        if (replaced != null) {
            return replaced;
        }
        return (R)data;
    }

    public abstract R convert(final T data, final long fromVersion, final long toVersion);

}
