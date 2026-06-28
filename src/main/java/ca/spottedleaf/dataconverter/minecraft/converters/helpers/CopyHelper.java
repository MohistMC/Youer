package ca.spottedleaf.dataconverter.minecraft.converters.helpers;

import ca.spottedleaf.dataconverter.types.ListType;
import ca.spottedleaf.dataconverter.types.MapType;

public final class CopyHelper {

    // sets value at dstPath in dst to a copied value of the value at srcPath in src
    public static boolean copy(final MapType src, final String srcPath, final MapType dst, final String dstPath) {
        final Object val = src.getGeneric(srcPath);
        if (val == null) {
            dst.remove(dstPath);
            return false;
        }

        dst.setGeneric(dstPath, copyGeneric(val));
        return true;
    }

    // moves the value at dstPath in dst to srcPath in src
    public static boolean move(final MapType src, final String srcPath, final MapType dst, final String dstPath) {
        final Object val = src.getGeneric(srcPath);
        src.remove(srcPath);
        if (val == null) {
            dst.remove(dstPath);
            return false;
        }

        dst.setGeneric(dstPath, val);
        return true;
    }

    public static Number sanitizeNumber(final Number input) {
        return switch (input) {
            case Byte b -> b;
            case Short s -> s;
            case Integer i -> i;
            case Long l -> l;
            case Float f -> f;
            case Double d -> d;
            default -> null;
        };
    }

    public static Object copyGeneric(final Object value) {
        if (value == null || value instanceof String || value instanceof Boolean) {
            // immutable
            return value;
        }
        if (value instanceof Number number) {
            if (sanitizeNumber(number) == null) {
                throw new IllegalArgumentException("Unknown type: " + value.getClass());
            }
            return number;
        }

        if (value instanceof MapType mapType) {
            return mapType.copy();
        }

        if (value instanceof ListType listType) {
            return listType.copy();
        }

        if (value.getClass().isArray()) {
            if (value instanceof byte[] bytes) {
                return bytes.clone();
            } else if (value instanceof short[] shorts) {
                return shorts.clone();
            } else if (value instanceof int[] ints) {
                return ints.clone();
            } else if (value instanceof long[] longs) {
                return longs.clone();
            } // else: fall through to throw
        }

        throw new IllegalArgumentException("Unknown type: " + value.getClass());
    }
}
