package ca.spottedleaf.dataconverter.minecraft.versions;

import ca.spottedleaf.dataconverter.minecraft.MCVersions;
import ca.spottedleaf.dataconverter.minecraft.converters.attributes.ConverterAbstractAttributesRename;
import com.google.common.collect.ImmutableMap;
import java.util.HashMap;
import java.util.Map;

public final class V2523 {

    private static final int VERSION = MCVersions.V20W13B + 2;

    private static final Map<String, String> RENAMES = new HashMap<>(
            ImmutableMap.<String, String>builder()
                    .put("generic.maxHealth", "minecraft:generic.max_health")
                    .put("Max Health", "minecraft:generic.max_health")
                    .put("zombie.spawnReinforcements", "minecraft:zombie.spawn_reinforcements")
                    .put("Spawn Reinforcements Chance", "minecraft:zombie.spawn_reinforcements")
                    .put("horse.jumpStrength", "minecraft:horse.jump_strength")
                    .put("Jump Strength", "minecraft:horse.jump_strength")
                    .put("generic.followRange", "minecraft:generic.follow_range")
                    .put("Follow Range", "minecraft:generic.follow_range")
                    .put("generic.knockbackResistance", "minecraft:generic.knockback_resistance")
                    .put("Knockback Resistance", "minecraft:generic.knockback_resistance")
                    .put("generic.movementSpeed", "minecraft:generic.movement_speed")
                    .put("Movement Speed", "minecraft:generic.movement_speed")
                    .put("generic.flyingSpeed", "minecraft:generic.flying_speed")
                    .put("Flying Speed", "minecraft:generic.flying_speed")
                    .put("generic.attackDamage", "minecraft:generic.attack_damage")
                    .put("generic.attackKnockback", "minecraft:generic.attack_knockback")
                    .put("generic.attackSpeed", "minecraft:generic.attack_speed")
                    .put("generic.armorToughness", "minecraft:generic.armor_toughness")
                    .build()
    );

    public static void register() {
        ConverterAbstractAttributesRename.register(VERSION, RENAMES::get);
    }

    private V2523() {}
}
