package ca.spottedleaf.dataconverter.types;

public interface ListType {

    public TypeUtil<?> getTypeUtil();

    @Override
    public int hashCode();

    @Override
    public boolean equals(final Object other);

    // Provides a deep copy of this list
    public ListType copy();

    // Returns the type of all elements in this list. Returns NONE if empty, returns UNDEFINED if not supported, MIXED if mixed types
    public ObjectType getUniformType();

    public int size();

    public void remove(final int index);

    public Object getGeneric(final int index);

    public default void setGeneric(final int index, final Object to) {
        if (to instanceof Number) {
            if (to instanceof Byte b) {
                this.setByte(index, b.byteValue());
                return;
            } else if (to instanceof Short s) {
                this.setShort(index, s.shortValue());
                return;
            } else if (to instanceof Integer i) {
                this.setInt(index, i.intValue());
                return;
            } else if (to instanceof Long l) {
                this.setLong(index, l.longValue());
                return;
            } else if (to instanceof Float f) {
                this.setFloat(index, f.floatValue());
                return;
            } else if (to instanceof Double d) {
                this.setDouble(index, d.doubleValue());
                return;
            } // else fall through to throw
        } else if (to instanceof MapType m) {
            this.setMap(index, m);
            return;
        } else if (to instanceof ListType l) {
            this.setList(index, l);
            return;
        } else if (to instanceof String s) {
            this.setString(index, s);
            return;
        } else if (to.getClass().isArray()) {
            if (to instanceof byte[] bytes) {
                this.setBytes(index, bytes);
                return;
            } else if (to instanceof short[] shorts) {
                this.setShorts(index, shorts);
                return;
            } else if (to instanceof int[] ints) {
                this.setInts(index, ints);
                return;
            } else if (to instanceof long[] longs) {
                this.setLongs(index, longs);
                return;
            } // else fall through to throw
        }

        throw new IllegalArgumentException("Object " + to + " is not a valid type!");
    }

    // types here are strict. if the type on get does not match the underlying type, will throw - except for the
    // default parameter methods, in such cases the default value will be returned.

    public Number getNumber(final int index);

    public Number getNumber(final int index, final Number dfl);

    // if the value at index is a Number but not a byte, then returns the number casted to byte.
    public byte getByte(final int index);

    // if the value at index is a Number but not a byte, then returns the number casted to byte.
    public byte getByte(final int index, final byte dfl);

    public void setByte(final int index, final byte to);

    // if the value at index is a Number but not a short, then returns the number casted to short.
    public short getShort(final int index);

    // if the value at index is a Number but not a short, then returns the number casted to short.
    public short getShort(final int index, final short dfl);

    public void setShort(final int index, final short to);

    // if the value at index is a Number but not a int, then returns the number casted to int.
    public int getInt(final int index);

    // if the value at index is a Number but not a int, then returns the number casted to int.
    public int getInt(final int index, final int dfl);

    public void setInt(final int index, final int to);

    // if the value at index is a Number but not a long, then returns the number casted to long.
    public long getLong(final int index);

    // if the value at index is a Number but not a long, then returns the number casted to long.
    public long getLong(final int index, final long dfl);

    public void setLong(final int index, final long to);

    // if the value at index is a Number but not a float, then returns the number casted to float.
    public float getFloat(final int index);

    // if the value at index is a Number but not a float, then returns the number casted to float.
    public float getFloat(final int index, final float dfl);

    public void setFloat(final int index, final float to);

    // if the value at index is a Number but not a double, then returns the number casted to double.
    public double getDouble(final int index);

    // if the value at index is a Number but not a double, then returns the number casted to double.
    public double getDouble(final int index, final double dfl);

    public void setDouble(final int index, final double to);

    public byte[] getBytes(final int index);

    public byte[] getBytes(final int index, final byte[] bytes);

    public void setBytes(final int index, final byte[] to);

    public short[] getShorts(final int index);

    public short[] getShorts(final int index, final short[] dfl);

    public void setShorts(final int index, final short[] to);

    public int[] getInts(final int index);

    public int[] getInts(final int index, final int[] dfl);

    public void setInts(final int index, final int[] to);

    public long[] getLongs(final int index);

    public long[] getLongs(final int index, final long[] dfl);

    public void setLongs(final int index, final long[] to);

    public ListType getList(final int index);

    public ListType getList(final int index, final ListType dfl);

    public void setList(final int index, final ListType list);

    public MapType getMap(final int index);

    public MapType getMap(final int index, final MapType dfl);

    public void setMap(final int index, final MapType to);

    public String getString(final int index);

    public String getString(final int index, final String dfl);

    public void setString(final int index, final String to);

    public default void addGeneric(final Object to) {
        if (to instanceof Number) {
            if (to instanceof Byte b) {
                this.addByte(b.byteValue());
                return;
            } else if (to instanceof Short s) {
                this.addShort(s.shortValue());
                return;
            } else if (to instanceof Integer i) {
                this.addInt(i.intValue());
                return;
            } else if (to instanceof Long l) {
                this.addLong(l.longValue());
                return;
            } else if (to instanceof Float f) {
                this.addFloat(f.floatValue());
                return;
            } else if (to instanceof Double d) {
                this.addDouble(d.doubleValue());
                return;
            } // else fall through to throw
        } else if (to instanceof MapType m) {
            this.addMap(m);
            return;
        } else if (to instanceof ListType l) {
            this.addList(l);
            return;
        } else if (to instanceof String s) {
            this.addString(s);
            return;
        } else if (to.getClass().isArray()) {
            if (to instanceof byte[] bytes) {
                this.addByteArray(bytes);
                return;
            } else if (to instanceof short[] shorts) {
                this.addShortArray(shorts);
                return;
            } else if (to instanceof int[] ints) {
                this.addIntArray(ints);
                return;
            } else if (to instanceof long[] longs) {
                this.addLongArray(longs);
                return;
            } // else fall through to throw
        }

        throw new IllegalArgumentException("Object " + to + " is not a valid type!");
    }

    public void addByte(final byte b);

    public void addByte(final int index, final byte b);

    public void addShort(final short s);

    public void addShort(final int index, final short s);

    public void addInt(final int i);

    public void addInt(final int index, final int i);

    public void addLong(final long l);

    public void addLong(final int index, final long l);

    public void addFloat(final float f);

    public void addFloat(final int index, final float f);

    public void addDouble(final double d);

    public void addDouble(final int index, final double d);

    public void addByteArray(final byte[] arr);

    public void addByteArray(final int index, final byte[] arr);

    public void addShortArray(final short[] arr);

    public void addShortArray(final int index, final short[] arr);

    public void addIntArray(final int[] arr);

    public void addIntArray(final int index, final int[] arr);

    public void addLongArray(final long[] arr);

    public void addLongArray(final int index, final long[] arr);

    public void addList(final ListType list);

    public void addList(final int index, final ListType list);

    public void addMap(final MapType map);

    public void addMap(final int index, final MapType map);

    public void addString(final String string);

    public void addString(final int index, final String string);

}
