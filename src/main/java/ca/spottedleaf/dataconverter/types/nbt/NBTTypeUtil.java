package ca.spottedleaf.dataconverter.types.nbt;

import ca.spottedleaf.converter.types.ListType;
import ca.spottedleaf.converter.types.MapType;
import ca.spottedleaf.converter.types.ObjectType;
import ca.spottedleaf.converter.types.TypeUtil;
import net.minecraft.nbt.ByteArrayTag;
import net.minecraft.nbt.ByteTag;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.DoubleTag;
import net.minecraft.nbt.EndTag;
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

public final class NBTTypeUtil extends TypeUtil<Tag> {

    @Override
    public NBTListType createEmptyList() {
        return new NBTListType();
    }

    @Override
    public NBTMapType createEmptyMap() {
        return new NBTMapType();
    }

    @Override
    public Object convertGenericToGeneric(final Object valueGeneric, final TypeUtil<?> to) {
        if (valueGeneric == null || valueGeneric instanceof String || valueGeneric instanceof Boolean) {
            return valueGeneric;
        }
        if (valueGeneric instanceof Number number) {
            if (to.isCompatibleNumber(number)) {
                return valueGeneric;
            }
            return number;
        }
        if (valueGeneric.getClass().isArray()) {
            if (to.isCompatibleArray(valueGeneric)) {
                return valueGeneric;
            }
            throw new IllegalStateException("Unknown type: " + valueGeneric.getClass());
        }
        if (valueGeneric instanceof NBTListType listType) {
            return convertNBT(to, listType.list);
        }
        if (valueGeneric instanceof NBTMapType mapType) {
            return convertNBT(to, mapType.map);
        }
        throw new IllegalStateException("Unknown type: " + valueGeneric);
    }

    @Override
    public Object convertFromBaseToGeneric(final Tag input, final TypeUtil<?> to) {
        return convertNBTToGeneric(to, input);
    }

    @Override
    public Object baseToGeneric(final Tag input) {
        return switch (input) {
            case CompoundTag ct -> new NBTMapType(ct);
            case ListTag lt -> new NBTListType(lt);
            case EndTag endTag -> null;
            case StringTag st -> st.value();
            case ByteArrayTag bt -> bt.getAsByteArray();
            case IntArrayTag it -> it.getAsIntArray();
            case LongArrayTag lt -> lt.getAsLongArray();
            case NumericTag nt -> nt.box();
            case null -> null;
            default -> throw new IllegalStateException("Unknown tag: " + input);
        };
    }

    @Override
    public Tag genericToBase(final Object input) {
        return switch (input) {
            case null -> EndTag.INSTANCE;
            case NBTMapType mapType -> mapType.map;
            case NBTListType listType -> listType.list;
            case String string -> StringTag.valueOf(string);
            case Boolean bool -> ByteTag.valueOf(bool.booleanValue());
            case Byte b -> ByteTag.valueOf(b.byteValue());
            case Short s -> ShortTag.valueOf(s.shortValue());
            case Integer i -> IntTag.valueOf(i.intValue());
            case Long l -> LongTag.valueOf(l.longValue());
            case Float f -> FloatTag.valueOf(f.floatValue());
            case Double d -> DoubleTag.valueOf(d.doubleValue());
            case byte[] bytes -> new ByteArrayTag(bytes);
            case int[] ints -> new IntArrayTag(ints);
            case long[] longs -> new LongArrayTag(longs);

            default -> throw new IllegalStateException("Unrecognized type " + input);
        };
    }

    @Override
    public boolean isCompatibleNumber(final Number number) {
        return switch (number) {
            case Byte b -> true;
            case Short s -> true;
            case Integer i -> true;
            case Long l -> true;
            case Float f -> true;
            case Double d -> true;

            default -> false;
        };
    }

    @Override
    public boolean isCompatibleArray(final Object array) {
        return switch (array) {
            case byte[] b -> true;
            case int[] i -> true;
            case long[] l -> true;

            default -> false;
        };
    }

    public static ObjectType getType(final byte id) {
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
    public ObjectType getTypeBase(final Tag value) {
        if (value == null) {
            return null;
        }

        return getType(value.getId());
    }

    @Override
    public Object deepCopy(final Tag base) {
        return base == null ? null : base.copy();
    }

    private static Object convertNBTToGeneric(final TypeUtil<?> to, final Tag nbt) {
        return switch (nbt) {
            case CompoundTag ct -> convertNBT(to, ct);
            case ListTag lt -> convertNBT(to, lt);
            case EndTag endTag -> null;
            case StringTag st -> st.value();
            case ByteArrayTag bt -> bt.getAsByteArray();
            case IntArrayTag it -> it.getAsIntArray();
            case LongArrayTag lt -> lt.getAsLongArray();
            case NumericTag nt -> nt.box();
            case null -> null;
            default -> throw new IllegalStateException("Unknown tag: " + nbt);
        };
    }

    public static MapType convertNBT(final TypeUtil<?> to, final CompoundTag nbt) {
        final MapType ret = to.createEmptyMap();

        for (final String key : nbt.keySet()) {
            ret.setGeneric(key, convertNBTToGeneric(to, nbt.get(key)));
        }

        return ret;
    }

    public static ListType convertNBT(final TypeUtil<?> to, final ListTag nbt) {
        final ListType ret = to.createEmptyList();

        for (int i = 0, len = nbt.size(); i < len; ++i) {
            ret.addGeneric(convertNBTToGeneric(to, nbt.get(i)));
        }

        return ret;
    }
}
