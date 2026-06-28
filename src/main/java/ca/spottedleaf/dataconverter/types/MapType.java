package ca.spottedleaf.dataconverter.types;

import java.util.Set;

public interface MapType {

    public TypeUtil<?> getTypeUtil();

    @Override
    public int hashCode();

    @Override
    public boolean equals(final Object other);

    public int size();

    public boolean isEmpty();

    public void clear();

    public Set<String> keys();

    // Provides a deep copy of this map
    public MapType copy();

    public boolean hasKey(final String key);

    public boolean hasKey(final String key, final ObjectType type);

    public void remove(final String key);

    public Object getGeneric(final String key);

    // types here are not strict. if the key maps to a different type, default is always returned
    // if default is not a parameter, then default is always null

    public Number getNumber(final String key);

    public Number getNumber(final String key, final Number dfl);

    public boolean getBoolean(final String key);

    public boolean getBoolean(final String key, final boolean dfl);

    public void setBoolean(final String key, final boolean val);

    // if the mapped value is a Number but not a byte, then the number is casted to byte. If the mapped value does not exist or is not a number, returns 0
    public byte getByte(final String key);

    // if the mapped value is a Number but not a byte, then the number is casted to byte. If the mapped value does not exist or is not a number, returns dfl
    public byte getByte(final String key, final byte dfl);

    public void setByte(final String key, final byte val);

    // if the mapped value is a Number but not a short, then the number is casted to short. If the mapped value does not exist or is not a number, returns 0
    public short getShort(final String key);

    // if the mapped value is a Number but not a short, then the number is casted to short. If the mapped value does not exist or is not a number, returns dfl
    public short getShort(final String key, final short dfl);

    public void setShort(final String key, final short val);

    // if the mapped value is a Number but not a int, then the number is casted to int. If the mapped value does not exist or is not a number, returns 0
    public int getInt(final String key);

    // if the mapped value is a Number but not a int, then the number is casted to int. If the mapped value does not exist or is not a number, returns dfl
    public int getInt(final String key, final int dfl);

    public void setInt(final String key, final int val);

    // if the mapped value is a Number but not a long, then the number is casted to long. If the mapped value does not exist or is not a number, returns 0
    public long getLong(final String key);

    // if the mapped value is a Number but not a long, then the number is casted to long. If the mapped value does not exist or is not a number, returns dfl
    public long getLong(final String key, final long dfl);

    public void setLong(final String key, final long val);

    // if the mapped value is a Number but not a float, then the number is casted to float. If the mapped value does not exist or is not a number, returns 0
    public float getFloat(final String key);

    // if the mapped value is a Number but not a float, then the number is casted to float. If the mapped value does not exist or is not a number, returns dfl
    public float getFloat(final String key, final float dfl);

    public void setFloat(final String key, final float val);

    // if the mapped value is a Number but not a double, then the number is casted to double. If the mapped value does not exist or is not a number, returns 0
    public double getDouble(final String key);

    // if the mapped value is a Number but not a double, then the number is casted to double. If the mapped value does not exist or is not a number, returns dfl
    public double getDouble(final String key, final double dfl);

    public void setDouble(final String key, final double val);

    public byte[] getBytes(final String key);

    public byte[] getBytes(final String key, final byte[] dfl);

    public void setBytes(final String key, final byte[] val);

    public short[] getShorts(final String key);

    public short[] getShorts(final String key, final short[] dfl);

    public void setShorts(final String key, final short[] val);

    public int[] getInts(final String key);

    public int[] getInts(final String key, final int[] dfl);

    public void setInts(final String key, final int[] val);

    public long[] getLongs(final String key);

    public long[] getLongs(final String key, final long[] dfl);

    public void setLongs(final String key, final long[] val);

    public ListType getListUnchecked(final String key);

    public ListType getListUnchecked(final String key, final ListType dfl);

    public default ListType getList(final String key, final ObjectType type) {
        return this.getList(key, type, null);
    }

    public default ListType getOrCreateList(final String key, final ObjectType type) {
        ListType ret = this.getList(key, type);
        if (ret == null) {
            this.setList(key, ret = this.getTypeUtil().createEmptyList());
        }

        return ret;
    }

    public default ListType getList(final String key, final ObjectType type, final ListType dfl) {
        final ListType ret = this.getListUnchecked(key, null);
        final ObjectType retType;
        if (ret != null && ((retType = ret.getUniformType()) == type || retType == ObjectType.UNDEFINED || retType == ObjectType.NONE)) {
            return ret;
        } else {
            return dfl;
        }
    }

    public void setList(final String key, final ListType val);

    public MapType getMap(final String key);

    public default MapType getOrCreateMap(final String key) {
        MapType ret = this.getMap(key);
        if (ret == null) {
            this.setMap(key, ret = this.getTypeUtil().createEmptyMap());
        }

        return ret;
    }

    public MapType getMap(final String key, final MapType dfl);

    public void setMap(final String key, final MapType val);

    public String getString(final String key);

    public String getString(final String key, final String dfl);

    public void setString(final String key, final String val);

    public default void setGeneric(final String key, final Object value) {
        if (value instanceof Boolean bool) {
            this.setBoolean(key, bool.booleanValue());
            return;
        } else if (value instanceof Number) {
            if (value instanceof Byte b) {
                this.setByte(key, b.byteValue());
                return;
            } else if (value instanceof Short s) {
                this.setShort(key, s.shortValue());
                return;
            } else if (value instanceof Integer i) {
                this.setInt(key, i.intValue());
                return;
            } else if (value instanceof Long l) {
                this.setLong(key, l.longValue());
                return;
            } else if (value instanceof Float f) {
                this.setFloat(key, f.floatValue());
                return;
            } else if (value instanceof Double d) {
                this.setDouble(key, d.doubleValue());
                return;
            } // else fall through to throw
        } else if (value instanceof MapType map) {
            this.setMap(key, map);
            return;
        } else if (value instanceof ListType list) {
            this.setList(key, list);
            return;
        } else if (value instanceof String string) {
            this.setString(key, string);
            return;
        } else if (value.getClass().isArray()) {
            if (value instanceof byte[] bytes) {
                this.setBytes(key, bytes);
                return;
            } else if (value instanceof short[] shorts) {
                this.setShorts(key, shorts);
                return;
            } else if (value instanceof int[] ints) {
                this.setInts(key, ints);
                return;
            } else if (value instanceof long[] longs) {
                this.setLongs(key, longs);
                return;
            } // else fall through to throw
        }

        throw new IllegalArgumentException("Object " + value + " is not a valid type!");
    }
}
