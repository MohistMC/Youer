package ca.spottedleaf.dataconverter.types.json;

import ca.spottedleaf.dataconverter.minecraft.converters.helpers.CopyHelper;
import ca.spottedleaf.dataconverter.types.ListType;
import ca.spottedleaf.dataconverter.types.MapType;
import ca.spottedleaf.dataconverter.types.TypeUtil;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonNull;
import com.google.gson.JsonObject;
import com.google.gson.JsonPrimitive;
import java.math.BigDecimal;
import java.util.Map;

public final class JsonTypeUtil implements TypeUtil<JsonElement> {

    private final boolean compressed;

    public JsonTypeUtil(final boolean compressed) {
        this.compressed = compressed;
    }

    public boolean isCompressed() {
        return this.compressed;
    }

    @Override
    public ListType createEmptyList() {
        return new JsonListType(this.compressed);
    }

    @Override
    public MapType createEmptyMap() {
        return new JsonMapType(this.compressed);
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
        if (valueGeneric instanceof JsonListType listType) {
            return convertJson(to, listType.array, this.compressed); // override input compression
        }
        if (valueGeneric instanceof JsonMapType mapType) {
            return convertJson(to, mapType.map, this.compressed); // override input compression
        }
        throw new IllegalStateException("Unknown type: " + valueGeneric);
    }

    @Override
    public Object convertFromBaseToGeneric(final JsonElement input, final TypeUtil<?> to) {
        return convertJsonToGeneric(to, input, this.compressed);
    }

    @Override
    public Object baseToGeneric(final JsonElement input) {
        if (input == null) {
            return null;
        }
        if (input instanceof JsonObject jsonObject) {
            return new JsonMapType(jsonObject, this.compressed);
        } else if (input instanceof JsonArray array) {
            return new JsonListType(array, this.compressed);
        } else if (input instanceof JsonNull) {
            return null;
        } else {
            final JsonPrimitive primitive = (JsonPrimitive)input;
            if (primitive.isBoolean()) {
                return Boolean.valueOf(primitive.getAsBoolean());
            } else if (primitive.isNumber()) {
                final Number number = CopyHelper.sanitizeNumber(primitive.getAsNumber());
                return number != null ? number : convertBDToGeneric(primitive.getAsBigDecimal());
            } else if (primitive.isString()) {
                return primitive.getAsString();
            }
        }

        throw new IllegalStateException("Unrecognized type " + input);
    }

    @Override
    public JsonElement genericToBase(final Object input) {
        if (input == null) {
            return JsonNull.INSTANCE;
        } else if (input instanceof JsonMapType mapType) {
            return mapType.map;
        } else if (input instanceof JsonListType listType) {
            return listType.array;
        } else if (input instanceof Boolean bool) {
            return new JsonPrimitive(bool);
        } else if (input instanceof Number number) {
            return new JsonPrimitive(number);
        } else if (input instanceof String string) {
            return new JsonPrimitive(string);
        }

        throw new IllegalStateException("Unrecognized type " + input);
    }

    private static Number convertBDToGeneric(final BigDecimal input) {
        try {
            final long l = input.longValueExact();
            final byte b = (byte)l;
            final short s = (short)l;
            final int i = (int)l;

            if ((long)b == l) {
                return Byte.valueOf(b);
            }
            if ((long)s == l) {
                return Short.valueOf(s);
            }
            if ((long)i == l) {
                return Integer.valueOf(i);
            }

            return Long.valueOf(l);
        } catch (final ArithmeticException ex) {
            final double d = input.doubleValue(); // possibly lose precision!
            final float f = (float)d;

            return (double)f == d ? Float.valueOf(f) : Double.valueOf(d);
        }
    }

    private static Object convertJsonToGeneric(final TypeUtil<?> to, final JsonElement element, final boolean compressed) {
        if (element == null) {
            return null;
        }
        if (element instanceof JsonObject jsonObject) {
            return convertJson(to, jsonObject, compressed);
        } else if (element instanceof JsonArray array) {
            return convertJson(to, array, compressed);
        } else if (element instanceof JsonNull) {
            return null;
        } else {
            final JsonPrimitive primitive = (JsonPrimitive)element;
            if (primitive.isBoolean()) {
                return Boolean.valueOf(primitive.getAsBoolean());
            } else if (primitive.isNumber()) {
                final Number number = CopyHelper.sanitizeNumber(primitive.getAsNumber());
                return number != null ? number : convertBDToGeneric(primitive.getAsBigDecimal());
            } else if (primitive.isString()) {
                return primitive.getAsString();
            }
        }

        throw new IllegalStateException("Unrecognized type " + element);
    }

    private static MapType convertJson(final TypeUtil<?> to, final JsonObject json, final boolean compressed) {
        final MapType ret = to.createEmptyMap();
        for (final Map.Entry<String, JsonElement> entry : json.entrySet()) {
            final Object obj = convertJsonToGeneric(to, entry.getValue(), compressed);
            if (obj == null) {
                continue;
            }

            ret.setGeneric(entry.getKey(), obj);
        }

        return ret;
    }

    private static ListType convertJson(final TypeUtil<?> to, final JsonArray json, final boolean compressed) {
        final ListType ret = to.createEmptyList();

        for (int i = 0, len = json.size(); i < len; ++i) {
            ret.addGeneric(convertJsonToGeneric(to, json.get(i), compressed));
        }

        return ret;
    }
}
