package io.papermc.paper.configuration.serializer;

import com.mohistmc.io.leangen.geantyref.TypeToken;
import com.mohistmc.org.spongepowered.configurate.serialize.ScalarSerializer;
import com.mohistmc.org.spongepowered.configurate.serialize.SerializationException;
import com.mohistmc.org.spongepowered.configurate.util.EnumLookup;
import java.lang.reflect.Type;
import java.util.Arrays;
import java.util.List;
import java.util.function.Predicate;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.checkerframework.checker.nullness.qual.Nullable;

import static com.mohistmc.io.leangen.geantyref.GenericTypeReflector.erase;

/**
 * Enum serializer that lists options if fails and accepts `-` as `_`.
 */
public class EnumValueSerializer extends ScalarSerializer<Enum<?>> {

    private static final Logger LOGGER = LogManager.getLogger();

    public EnumValueSerializer() {
        super(new TypeToken<Enum<?>>() {});
    }

    @SuppressWarnings({"rawtypes", "unchecked"})
    @Override
    public @Nullable Enum<?> deserialize(final Type type, final Object obj) throws SerializationException {
        final String enumConstant = obj.toString();
        final Class<? extends Enum> typeClass = erase(type).asSubclass(Enum.class);
        @Nullable Enum<?> ret = EnumLookup.lookupEnum(typeClass, enumConstant);
        if (ret == null) {
            ret = EnumLookup.lookupEnum(typeClass, enumConstant.replace("-", "_"));
        }
        if (ret == null) {
            boolean longer = typeClass.getEnumConstants().length > 10;
            List<String> options = Arrays.stream(typeClass.getEnumConstants()).limit(10L).map(Enum::name).toList();
            LOGGER.error("Invalid enum constant provided, expected one of [" + String.join(", " ,options) + (longer ? ", ..." : "") + "], but got " + enumConstant);
        }
        return ret;
    }

    @Override
    public Object serialize(final Enum<?> item, final Predicate<Class<?>> typeSupported) {
        return item.name();
    }
}
