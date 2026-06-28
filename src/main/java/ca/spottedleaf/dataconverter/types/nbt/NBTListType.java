package ca.spottedleaf.dataconverter.types.nbt;

import ca.spottedleaf.dataconverter.types.ObjectType;
import ca.spottedleaf.dataconverter.types.ListType;
import ca.spottedleaf.dataconverter.types.MapType;
import ca.spottedleaf.dataconverter.types.TypeUtil;
import ca.spottedleaf.dataconverter.types.Types;
import net.minecraft.nbt.ByteArrayTag;
import net.minecraft.nbt.ByteTag;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.DoubleTag;
import net.minecraft.nbt.FloatTag;
import net.minecraft.nbt.IntArrayTag;
import net.minecraft.nbt.IntTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.LongArrayTag;
import net.minecraft.nbt.LongTag;
import net.minecraft.nbt.NumericTag;
import net.minecraft.nbt.ShortTag;
import net.minecraft.nbt.StringTag;
import net.minecraft.nbt.Tag;

public final class NBTListType implements ListType {

    final ListTag list;

    public NBTListType() {
        this.list = new ListTag();
    }

    public NBTListType(final ListTag tag) {
        this.list = tag;
    }

    @Override
    public TypeUtil<Tag> getTypeUtil() {
        return Types.NBT;
    }

    @Override
    public boolean equals(final Object obj) {
        if (this == obj) {
            return true;
        }
        if (obj == null || obj.getClass() != NBTListType.class) {
            return false;
        }

        return this.list.equals(((NBTListType)obj).list);
    }

    @Override
    public int hashCode() {
        return this.list.hashCode();
    }

    @Override
    public String toString() {
        return "NBTListType{" +
                "list=" + this.list +
                '}';
    }

    public ListTag getTag() {
        return this.list;
    }

    @Override
    public ListType copy() {
        return new NBTListType(this.list.copy());
    }

    static ObjectType getType(final byte id) {
        switch (id) {
            case Tag.TAG_END:
                return ObjectType.NONE;
            case Tag.TAG_BYTE:
                return ObjectType.BYTE;
            case Tag.TAG_SHORT:
                return ObjectType.SHORT;
            case Tag.TAG_INT:
                return ObjectType.INT;
            case Tag.TAG_LONG:
                return ObjectType.LONG;
            case Tag.TAG_FLOAT:
                return ObjectType.FLOAT;
            case Tag.TAG_DOUBLE:
                return ObjectType.DOUBLE;
            case Tag.TAG_BYTE_ARRAY:
                return ObjectType.BYTE_ARRAY;
            case Tag.TAG_STRING:
                return ObjectType.STRING;
            case Tag.TAG_LIST:
                return ObjectType.LIST;
            case Tag.TAG_COMPOUND:
                return ObjectType.MAP;
            case Tag.TAG_INT_ARRAY:
                return ObjectType.INT_ARRAY;
            case Tag.TAG_LONG_ARRAY:
                return ObjectType.LONG_ARRAY;
            default:
                throw new IllegalStateException("Unknown type: " + id);
        }
    }

    @Override
    public ObjectType getUniformType() {
        ObjectType curr = null;

        for (int i = 0, len = this.list.size(); i < len; ++i) {
            final Tag tag = this.list.get(i);
            final ObjectType tagType = getType(tag.getId());
            if (curr == null) {
                curr = tagType;
            } else if (tagType != curr) {
                return ObjectType.MIXED;
            }
        }

        return curr == null ? ObjectType.NONE : curr;
    }

    @Override
    public int size() {
        return this.list.size();
    }

    @Override
    public void remove(final int index) {
        this.list.remove(index);
    }

    @Override
    public Object getGeneric(final int index) {
        return Types.NBT.baseToGeneric(this.list.get(index));
    }

    @Override
    public Number getNumber(final int index) {
        final Tag tag = this.list.get(index); // does bound checking for us
        if (!(tag instanceof NumericTag numericTag)) {
            throw new IllegalStateException();
        }
        return numericTag.box();
    }

    @Override
    public Number getNumber(final int index, final Number dfl) {
        final Tag tag = this.list.get(index); // does bound checking for us
        if (!(tag instanceof NumericTag numericTag)) {
            return dfl;
        }
        return numericTag.box();
    }

    @Override
    public byte getByte(final int index) {
        final Tag tag = this.list.get(index); // does bound checking for us
        if (!(tag instanceof NumericTag numericTag)) {
            throw new IllegalStateException();
        }
        return numericTag.byteValue();
    }

    @Override
    public byte getByte(final int index, final byte dfl) {
        final Tag tag = this.list.get(index); // does bound checking for us
        if (!(tag instanceof NumericTag numericTag)) {
            return dfl;
        }
        return numericTag.byteValue();
    }

    @Override
    public void setByte(final int index, final byte to) {
        this.list.set(index, ByteTag.valueOf(to));
    }

    @Override
    public short getShort(final int index) {
        final Tag tag = this.list.get(index); // does bound checking for us
        if (!(tag instanceof NumericTag numericTag)) {
            throw new IllegalStateException();
        }
        return numericTag.shortValue();
    }

    @Override
    public short getShort(final int index, final short dfl) {
        final Tag tag = this.list.get(index); // does bound checking for us
        if (!(tag instanceof NumericTag numericTag)) {
            return dfl;
        }
        return numericTag.shortValue();
    }

    @Override
    public void setShort(final int index, final short to) {
        this.list.set(index, ShortTag.valueOf(to));
    }

    @Override
    public int getInt(final int index) {
        final Tag tag = this.list.get(index); // does bound checking for us
        if (!(tag instanceof NumericTag numericTag)) {
            throw new IllegalStateException();
        }
        return numericTag.intValue();
    }

    @Override
    public int getInt(final int index, final int dfl) {
        final Tag tag = this.list.get(index); // does bound checking for us
        if (!(tag instanceof NumericTag numericTag)) {
            return dfl;
        }
        return numericTag.intValue();
    }

    @Override
    public void setInt(final int index, final int to) {
        this.list.set(index, IntTag.valueOf(to));
    }

    @Override
    public long getLong(final int index) {
        final Tag tag = this.list.get(index); // does bound checking for us
        if (!(tag instanceof NumericTag numericTag)) {
            throw new IllegalStateException();
        }
        return numericTag.longValue();
    }

    @Override
    public long getLong(final int index, final long dfl) {
        final Tag tag = this.list.get(index); // does bound checking for us
        if (!(tag instanceof NumericTag numericTag)) {
            return dfl;
        }
        return numericTag.longValue();
    }

    @Override
    public void setLong(final int index, final long to) {
        this.list.set(index, LongTag.valueOf(to));
    }

    @Override
    public float getFloat(final int index) {
        final Tag tag = this.list.get(index); // does bound checking for us
        if (!(tag instanceof NumericTag numericTag)) {
            throw new IllegalStateException();
        }
        return numericTag.floatValue();
    }

    @Override
    public float getFloat(final int index, final float dfl) {
        final Tag tag = this.list.get(index); // does bound checking for us
        if (!(tag instanceof NumericTag numericTag)) {
            return dfl;
        }
        return numericTag.floatValue();
    }

    @Override
    public void setFloat(final int index, final float to) {
        this.list.set(index, FloatTag.valueOf(to));
    }

    @Override
    public double getDouble(final int index) {
        final Tag tag = this.list.get(index); // does bound checking for us
        if (!(tag instanceof NumericTag numericTag)) {
            throw new IllegalStateException();
        }
        return numericTag.doubleValue();
    }

    @Override
    public double getDouble(final int index, final double dfl) {
        final Tag tag = this.list.get(index); // does bound checking for us
        if (!(tag instanceof NumericTag numericTag)) {
            return dfl;
        }
        return numericTag.doubleValue();
    }

    @Override
    public void setDouble(final int index, final double to) {
        this.list.set(index, DoubleTag.valueOf(to));
    }

    @Override
    public byte[] getBytes(final int index) {
        final Tag tag = this.list.get(index); // does bound checking for us
        if (!(tag instanceof ByteArrayTag bytes)) {
            throw new IllegalStateException();
        }
        return bytes.getAsByteArray();
    }


    @Override
    public byte[] getBytes(final int index, final byte[] dfl) {
        final Tag tag = this.list.get(index); // does bound checking for us
        if (!(tag instanceof ByteArrayTag bytes)) {
            return dfl;
        }
        return bytes.getAsByteArray();
    }

    @Override
    public void setBytes(final int index, final byte[] to) {
        this.list.set(index, new ByteArrayTag(to));
    }

    @Override
    public short[] getShorts(final int index) {
        // NBT does not support shorts
        throw new UnsupportedOperationException();
    }

    @Override
    public short[] getShorts(final int index, final short[] dfl) {
        // NBT does not support shorts
        throw new UnsupportedOperationException();
    }

    @Override
    public void setShorts(final int index, final short[] to) {
        throw new UnsupportedOperationException();
    }

    @Override
    public int[] getInts(final int index) {
        final Tag tag = this.list.get(index); // does bound checking for us
        if (!(tag instanceof IntArrayTag ints)) {
            throw new IllegalStateException();
        }
        return ints.getAsIntArray();
    }

    @Override
    public int[] getInts(final int index, final int[] dfl) {
        final Tag tag = this.list.get(index); // does bound checking for us
        if (!(tag instanceof IntArrayTag ints)) {
            return dfl;
        }
        return ints.getAsIntArray();
    }

    @Override
    public void setInts(final int index, final int[] to) {
        this.list.set(index, new IntArrayTag(to));
    }

    @Override
    public long[] getLongs(final int index) {
        final Tag tag = this.list.get(index); // does bound checking for us
        if (!(tag instanceof LongArrayTag longs)) {
            throw new IllegalStateException();
        }
        return longs.getAsLongArray();
    }

    @Override
    public long[] getLongs(final int index, final long[] dfl) {
        final Tag tag = this.list.get(index); // does bound checking for us
        if (!(tag instanceof LongArrayTag longs)) {
            return dfl;
        }
        return longs.getAsLongArray();
    }

    @Override
    public void setLongs(final int index, final long[] to) {
        this.list.set(index, new LongArrayTag(to));
    }

    @Override
    public ListType getList(final int index) {
        final Tag tag = this.list.get(index); // does bound checking for us
        if (!(tag instanceof ListTag list)) {
            throw new IllegalStateException();
        }
        return new NBTListType(list);
    }

    @Override
    public ListType getList(final int index, final ListType dfl) {
        final Tag tag = this.list.get(index); // does bound checking for us
        if (!(tag instanceof ListTag list)) {
            return dfl;
        }
        return new NBTListType(list);
    }

    @Override
    public void setList(final int index, final ListType list) {
        this.list.set(index, ((NBTListType)list).getTag());
    }

    @Override
    public MapType getMap(final int index) {
        final Tag tag = this.list.get(index); // does bound checking for us
        if (!(tag instanceof CompoundTag compoundTag)) {
            throw new IllegalStateException();
        }
        return new NBTMapType(compoundTag);
    }

    @Override
    public MapType getMap(final int index, final MapType dfl) {
        final Tag tag = this.list.get(index); // does bound checking for us
        if (!(tag instanceof CompoundTag compoundTag)) {
            return dfl;
        }
        return new NBTMapType(compoundTag);
    }

    @Override
    public void setMap(final int index, final MapType to) {
        this.list.set(index, ((NBTMapType)to).getTag());
    }

    @Override
    public String getString(final int index) {
        final Tag tag = this.list.get(index); // does bound checking for us
        if (!(tag instanceof StringTag stringTag)) {
            throw new IllegalStateException();
        }
        return stringTag.value();
    }


    @Override
    public String getString(final int index, final String dfl) {
        final Tag tag = this.list.get(index); // does bound checking for us
        if (!(tag instanceof StringTag stringTag)) {
            return dfl;
        }
        return stringTag.value();
    }

    @Override
    public void setString(final int index, final String to) {
        this.list.set(index, StringTag.valueOf(to));
    }

    @Override
    public void addByte(final byte b) {
        this.list.add(ByteTag.valueOf(b));
    }

    @Override
    public void addByte(final int index, final byte b) {
        this.list.add(index, ByteTag.valueOf(b));
    }

    @Override
    public void addShort(final short s) {
        this.list.add(ShortTag.valueOf(s));
    }

    @Override
    public void addShort(final int index, final short s) {
        this.list.add(index, ShortTag.valueOf(s));
    }

    @Override
    public void addInt(final int i) {
        this.list.add(IntTag.valueOf(i));
    }

    @Override
    public void addInt(final int index, final int i) {
        this.list.add(index, IntTag.valueOf(i));
    }

    @Override
    public void addLong(final long l) {
        this.list.add(LongTag.valueOf(l));
    }

    @Override
    public void addLong(final int index, final long l) {
        this.list.add(index, LongTag.valueOf(l));
    }

    @Override
    public void addFloat(final float f) {
        this.list.add(FloatTag.valueOf(f));
    }

    @Override
    public void addFloat(final int index, final float f) {
        this.list.add(index, FloatTag.valueOf(f));
    }

    @Override
    public void addDouble(final double d) {
        this.list.add(DoubleTag.valueOf(d));
    }

    @Override
    public void addDouble(final int index, final double d) {
        this.list.add(index, DoubleTag.valueOf(d));
    }

    @Override
    public void addByteArray(final byte[] arr) {
        this.list.add(new ByteArrayTag(arr));
    }

    @Override
    public void addByteArray(final int index, final byte[] arr) {
        this.list.add(index, new ByteArrayTag(arr));
    }

    @Override
    public void addShortArray(final short[] arr) {
        // NBT does not support short[]
        throw new UnsupportedOperationException();
    }

    @Override
    public void addShortArray(final int index, final short[] arr) {
        // NBT does not support short[]
        throw new UnsupportedOperationException();
    }

    @Override
    public void addIntArray(final int[] arr) {
        this.list.add(new IntArrayTag(arr));
    }

    @Override
    public void addIntArray(final int index, final int[] arr) {
        this.list.add(index, new IntArrayTag(arr));
    }

    @Override
    public void addLongArray(final long[] arr) {
        this.list.add(new LongArrayTag(arr));
    }

    @Override
    public void addLongArray(final int index, final long[] arr) {
        this.list.add(index, new LongArrayTag(arr));
    }

    @Override
    public void addList(final ListType list) {
        this.list.add(((NBTListType)list).getTag());
    }

    @Override
    public void addList(final int index, final ListType list) {
        this.list.add(index, ((NBTListType)list).getTag());
    }

    @Override
    public void addMap(final MapType map) {
        this.list.add(((NBTMapType)map).getTag());
    }

    @Override
    public void addMap(final int index, final MapType map) {
        this.list.add(index, ((NBTMapType)map).getTag());
    }

    @Override
    public void addString(final String string) {
        this.list.add(StringTag.valueOf(string));
    }

    @Override
    public void addString(final int index, final String string) {
        this.list.add(index, StringTag.valueOf(string));
    }
}
