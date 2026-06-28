package ca.spottedleaf.dataconverter.converters.datatypes;

public interface DataWalker<T> {

    public static final DataWalker<?> NO_OP = (final Object data, final long fromVersion, final long toVersion) -> {
        return null;
    };

    public static <T> DataWalker<T> noOp() {
        return (DataWalker<T>)NO_OP;
    }

    public T walk(final T data, final long fromVersion, final long toVersion);

}
