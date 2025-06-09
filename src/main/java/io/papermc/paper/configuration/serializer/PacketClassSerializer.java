package io.papermc.paper.configuration.serializer;

import com.google.common.collect.BiMap;
import com.google.common.collect.ImmutableBiMap;
import com.mohistmc.io.leangen.geantyref.TypeToken;
import com.mohistmc.org.spongepowered.configurate.serialize.ScalarSerializer;
import com.mohistmc.org.spongepowered.configurate.serialize.SerializationException;
import io.papermc.paper.configuration.serializer.collections.MapSerializer;
import io.papermc.paper.util.MappingEnvironment;
import io.papermc.paper.util.ObfHelper;
import java.lang.reflect.Type;
import java.util.List;
import java.util.Map;
import java.util.function.Predicate;
import net.minecraft.network.protocol.Packet;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.checkerframework.checker.nullness.qual.Nullable;

@SuppressWarnings("Convert2Diamond")
public final class PacketClassSerializer extends ScalarSerializer<Class<? extends Packet<?>>> implements MapSerializer.WriteBack {

    private static final Logger LOGGER = LogManager.getLogger();
    private static final TypeToken<Class<? extends Packet<?>>> TYPE = new TypeToken<Class<? extends Packet<?>>>() {};
    private static final List<String> SUBPACKAGES = List.of("game", "handshake", "login", "status");
    private static final BiMap<String, String> MOJANG_TO_OBF;

    static {
        final ImmutableBiMap.Builder<String, String> builder = ImmutableBiMap.builder();
        final @Nullable Map<String, ObfHelper.ClassMapping> classMappingMap = ObfHelper.INSTANCE.mappingsByMojangName();
        if (classMappingMap != null) {
            classMappingMap.forEach((mojMap, classMapping) -> {
                if (mojMap.startsWith("net.minecraft.network.protocol.")) {
                    builder.put(classMapping.mojangName(), classMapping.obfName());
                }
            });
        }
        MOJANG_TO_OBF = builder.build();
    }

    public PacketClassSerializer() {
        super(TYPE);
    }

    @SuppressWarnings("unchecked")
    @Override
    public Class<? extends Packet<?>> deserialize(final Type type, final Object obj) throws SerializationException {
        @Nullable Class<?> packetClass = null;
        for (final String subpackage : SUBPACKAGES) {
            final String fullClassName = "net.minecraft.network.protocol." + subpackage + "." + obj;
            try {
                packetClass = Class.forName(fullClassName);
                break;
            } catch (final ClassNotFoundException ex) {
                final @Nullable String spigotClassName = MOJANG_TO_OBF.get(fullClassName);
                if (spigotClassName != null) {
                    try {
                        packetClass = Class.forName(spigotClassName);
                    } catch (final ClassNotFoundException ignore) {}
                }
            }
        }
        if (packetClass == null || !Packet.class.isAssignableFrom(packetClass)) {
            throw new SerializationException("Could not deserialize a packet from " + obj);
        }
        return (Class<? extends Packet<?>>) packetClass;
    }

    @Override
    protected @Nullable Object serialize(final Class<? extends Packet<?>> packetClass, final Predicate<Class<?>> typeSupported) {
        final String name = packetClass.getName();
        @Nullable String mojName = ObfHelper.INSTANCE.mappingsByMojangName() == null || !MappingEnvironment.reobf() ? name : MOJANG_TO_OBF.inverse().get(name); // if the mappings are null, running on moj-mapped server
        if (mojName == null && MOJANG_TO_OBF.containsKey(name)) {
            mojName = name;
        }
        if (mojName != null) {
            int pos = mojName.lastIndexOf('.');
            if (pos != -1 && pos != mojName.length() - 1) {
                return mojName.substring(pos + 1);
            }
        }

        LOGGER.error("Could not serialize {} into a mojang-mapped packet class name", packetClass);
        return null;
    }
}
