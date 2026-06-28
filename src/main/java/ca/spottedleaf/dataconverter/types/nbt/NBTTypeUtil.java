package ca.spottedleaf.dataconverter.types.nbt;

import ca.spottedleaf.dataconverter.minecraft.converters.helpers.CopyHelper;
import ca.spottedleaf.dataconverter.types.ListType;
import ca.spottedleaf.dataconverter.types.MapType;
import ca.spottedleaf.dataconverter.types.TypeUtil;
import net.minecraft.nbt.ByteArrayTag;
import net.minecraft.nbt.ByteTag;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.EndTag;
import net.minecraft.nbt.IntArrayTag;
import net.minecraft.nbt.IntTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.LongArrayTag;
import net.minecraft.nbt.LongTag;
import net.minecraft.nbt.NumericTag;
import net.minecraft.nbt.ShortTag;
import net.minecraft.nbt.StringTag;
import net.minecraft.nbt.Tag;

public final class NBTTypeUtil implements TypeUtil<Tag> {

    @Override
    public ListType createEmptyList() {
        return new NBTListType();
    }

    @Override
    public MapType createEmptyMap() {
        return new NBTMapType();
    }

    @Override
    public Object convertTo(final Object valueGeneric, final TypeUtil<?> to) {
        if (valueGeneric == null || valueGeneric instanceof String || valueGeneric instanceof Boolean) {
            return valueGeneric;
        }
        if (valueGeneric instanceof Number number) {
            if (CopyHelper.sanitizeNumber(number) == null) {
                throw new IllegalStateException("Unknown type: " + number);
            }
            return number;
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
            case byte[] bytes -> new ByteArrayTag(bytes);
            case int[] ints -> new IntArrayTag(ints);
            case long[] longs -> new LongArrayTag(longs);

            default -> throw new IllegalStateException("Unrecognized type " + input);
        };
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
