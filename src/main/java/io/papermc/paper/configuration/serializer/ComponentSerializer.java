package io.papermc.paper.configuration.serializer;

import com.mohistmc.net.kyori.adventure.text.Component;
import com.mohistmc.net.kyori.adventure.text.minimessage.MiniMessage;
import com.mohistmc.org.spongepowered.configurate.serialize.ScalarSerializer;
import com.mohistmc.org.spongepowered.configurate.serialize.SerializationException;
import java.lang.reflect.Type;
import java.util.function.Predicate;

public class ComponentSerializer extends ScalarSerializer<Component> {

    public ComponentSerializer() {
        super(Component.class);
    }

    @Override
    public Component deserialize(Type type, Object obj) throws SerializationException {
        return MiniMessage.miniMessage().deserialize(obj.toString());
    }

    @Override
    protected Object serialize(Component component, Predicate<Class<?>> typeSupported) {
        return MiniMessage.miniMessage().serialize(component);
    }
}
