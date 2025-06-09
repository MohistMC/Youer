package io.papermc.paper.configuration.serializer.collections;

import com.mohistmc.io.leangen.geantyref.GenericTypeReflector;
import com.mohistmc.io.leangen.geantyref.TypeFactory;
import com.mohistmc.org.spongepowered.configurate.ConfigurationNode;
import com.mohistmc.org.spongepowered.configurate.serialize.SerializationException;
import com.mohistmc.org.spongepowered.configurate.serialize.TypeSerializer;
import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;
import java.util.Collections;
import java.util.Map;
import java.util.function.Function;
import org.checkerframework.checker.nullness.qual.Nullable;

@SuppressWarnings("rawtypes")
public abstract class FastutilMapSerializer<M extends Map<?, ?>> implements TypeSerializer<M> {
    private final Function<Map, ? extends M> factory;

    protected FastutilMapSerializer(final Function<Map, ? extends M> factory) {
        this.factory = factory;
    }

    @Override
    public M deserialize(final Type type, final ConfigurationNode node) throws SerializationException {
        @Nullable final Map map = (Map) node.get(this.createBaseMapType((ParameterizedType) type));
        return this.factory.apply(map == null ? Collections.emptyMap() : map);
    }

    @Override
    public void serialize(final Type type, @Nullable final M obj, final ConfigurationNode node) throws SerializationException {
        if (obj == null || obj.isEmpty()) {
            node.raw(null);
        } else {
            final Type baseMapType = this.createBaseMapType((ParameterizedType) type);
            node.set(baseMapType, obj);
        }
    }

    protected abstract Type createBaseMapType(final ParameterizedType type);

    public static final class SomethingToPrimitive<M extends Map<?, ?>> extends FastutilMapSerializer<M> {
        private final Type primitiveType;

        public SomethingToPrimitive(final Function<Map, ? extends M> factory, final Type primitiveType) {
            super(factory);
            this.primitiveType = primitiveType;
        }

        @Override
        protected Type createBaseMapType(final ParameterizedType type) {
            return TypeFactory.parameterizedClass(Map.class, type.getActualTypeArguments()[0], GenericTypeReflector.box(this.primitiveType));
        }
    }

    public static final class PrimitiveToSomething<M extends Map<?, ?>> extends FastutilMapSerializer<M> {
        private final Type primitiveType;

        public PrimitiveToSomething(final Function<Map, ? extends M> factory, final Type primitiveType) {
            super(factory);
            this.primitiveType = primitiveType;
        }

        @Override
        protected Type createBaseMapType(final ParameterizedType type) {
            return TypeFactory.parameterizedClass(Map.class, GenericTypeReflector.box(this.primitiveType), type.getActualTypeArguments()[0]);
        }
    }
}
