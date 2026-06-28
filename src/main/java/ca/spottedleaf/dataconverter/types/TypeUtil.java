package ca.spottedleaf.dataconverter.types;

public interface TypeUtil<T> {

    public ListType createEmptyList();

    public MapType createEmptyMap();

    public default Object convertFromBaseToGeneric(final T input, final TypeUtil<?> to) {
        return this.convertTo(this.baseToGeneric(input), to);
    }

    public default <D> D convertBaseToBase(final T input, final TypeUtil<D> to) {
        return to.genericToBase(this.convertFromBaseToGeneric(input, to));
    }

    public default <D> D convertGenericToBase(final Object valueGeneric, final TypeUtil<D> to) {
        return to.genericToBase(this.convertTo(valueGeneric, to));
    }

    public Object convertTo(final Object valueGeneric, final TypeUtil<?> to);

    public Object baseToGeneric(final T input);

    public T genericToBase(final Object input);
}
