package org.purpurmc.purpur.configuration.transformation;

import com.mohistmc.org.spongepowered.configurate.ConfigurateException;
import com.mohistmc.org.spongepowered.configurate.ConfigurationNode;
import com.mohistmc.org.spongepowered.configurate.NodePath;
import com.mohistmc.org.spongepowered.configurate.transformation.ConfigurationTransformation;
import com.mohistmc.org.spongepowered.configurate.transformation.TransformAction;
import io.papermc.paper.configuration.Configurations;
import io.papermc.paper.configuration.PaperConfigurations;
import io.papermc.paper.configuration.type.number.DoubleOr;
import java.util.OptionalDouble;
import org.checkerframework.checker.nullness.qual.Nullable;
import org.purpurmc.purpur.PurpurConfig;

import static com.mohistmc.org.spongepowered.configurate.NodePath.path;

public class VoidDamageHeightMigration implements TransformAction {

    public static boolean HAS_BEEN_REGISTERED = false;

    public static final String ENVIRONMENT_KEY = "environment";
    public static final String VOID_DAMAGE_KEY = "void-damage-amount";
    public static final String VOID_DAMAGE_MIN_HEIGHT_OFFSET_KEY = "void-damage-min-build-height-offset";
    public static final double DEFAULT_VOID_DAMAGE_HEIGHT = -64.0D;
    public static final double DEFAULT_VOID_DAMAGE = 4.0D;

    private final String worldName;

    private VoidDamageHeightMigration(String worldName) {
        this.worldName = PaperConfigurations.WORLD_DEFAULTS.equals(worldName) ? "default" : worldName;
    }

    @Override
    public Object @Nullable [] visitPath(final NodePath path, final ConfigurationNode value) throws ConfigurateException {
        String purpurVoidDamageHeightPath = "world-settings." + this.worldName + ".gameplay-mechanics.void-damage-height";
        ConfigurationNode voidDamageMinHeightOffsetNode = value.node(ENVIRONMENT_KEY, VOID_DAMAGE_MIN_HEIGHT_OFFSET_KEY);
        if (PurpurConfig.config.contains(purpurVoidDamageHeightPath)) {
            double purpurVoidDamageHeight = PurpurConfig.config.getDouble(purpurVoidDamageHeightPath);
            if (purpurVoidDamageHeight != DEFAULT_VOID_DAMAGE_HEIGHT && (voidDamageMinHeightOffsetNode.empty() || voidDamageMinHeightOffsetNode.getDouble() == DEFAULT_VOID_DAMAGE_HEIGHT)) {
                voidDamageMinHeightOffsetNode.raw(null);
                voidDamageMinHeightOffsetNode.set(purpurVoidDamageHeight);
            }
            PurpurConfig.config.set(purpurVoidDamageHeightPath, null);
        }

        String purpurVoidDamagePath = "world-settings." + this.worldName + ".gameplay-mechanics.void-damage-dealt";
        ConfigurationNode voidDamageNode = value.node(ENVIRONMENT_KEY, VOID_DAMAGE_KEY);
        if (PurpurConfig.config.contains(purpurVoidDamagePath)) {
            double purpurVoidDamage = PurpurConfig.config.getDouble(purpurVoidDamagePath);
            if (purpurVoidDamage != DEFAULT_VOID_DAMAGE && (voidDamageNode.empty() || voidDamageNode.getDouble() == DEFAULT_VOID_DAMAGE)) {
                voidDamageNode.raw(null);
                voidDamageNode.set(new DoubleOr.Disabled(OptionalDouble.of(purpurVoidDamage)));
            }
            PurpurConfig.config.set(purpurVoidDamagePath, null);
        }

        return null;
    }

    public static void apply(final ConfigurationTransformation.Builder builder, final Configurations.ContextMap contextMap) {
        if (PurpurConfig.version < 36) {
            HAS_BEEN_REGISTERED = true;
            builder.addAction(path(), new VoidDamageHeightMigration(contextMap.require(Configurations.WORLD_NAME)));
        }
    }

}
