package io.papermc.paper.tag;

import org.bukkit.NamespacedKey;

import static org.bukkit.entity.EntityType.AXOLOTL;
import static org.bukkit.entity.EntityType.BOGGED;
import static org.bukkit.entity.EntityType.COD;
import static org.bukkit.entity.EntityType.DOLPHIN;
import static org.bukkit.entity.EntityType.DROWNED;
import static org.bukkit.entity.EntityType.ELDER_GUARDIAN;
import static org.bukkit.entity.EntityType.GLOW_SQUID;
import static org.bukkit.entity.EntityType.GUARDIAN;
import static org.bukkit.entity.EntityType.HUSK;
import static org.bukkit.entity.EntityType.MAGMA_CUBE;
import static org.bukkit.entity.EntityType.PHANTOM;
import static org.bukkit.entity.EntityType.PUFFERFISH;
import static org.bukkit.entity.EntityType.SALMON;
import static org.bukkit.entity.EntityType.SKELETON;
import static org.bukkit.entity.EntityType.SKELETON_HORSE;
import static org.bukkit.entity.EntityType.SLIME;
import static org.bukkit.entity.EntityType.SQUID;
import static org.bukkit.entity.EntityType.STRAY;
import static org.bukkit.entity.EntityType.TADPOLE;
import static org.bukkit.entity.EntityType.TROPICAL_FISH;
import static org.bukkit.entity.EntityType.TURTLE;
import static org.bukkit.entity.EntityType.WITHER;
import static org.bukkit.entity.EntityType.WITHER_SKELETON;
import static org.bukkit.entity.EntityType.ZOGLIN;
import static org.bukkit.entity.EntityType.ZOMBIE;
import static org.bukkit.entity.EntityType.ZOMBIE_HORSE;
import static org.bukkit.entity.EntityType.ZOMBIE_VILLAGER;
import static org.bukkit.entity.EntityType.ZOMBIFIED_PIGLIN;

/**
 * All tags in this class are unmodifiable, attempting to modify them will throw an
 * {@link UnsupportedOperationException}.
 */
public class EntityTags {

    private static NamespacedKey keyFor(String key) {
        //noinspection deprecation
        return new NamespacedKey("paper", key + "_settag");
    }

    /**
     * Covers undead mobs
     * @see <a href="https://minecraft.wiki/wiki/Mob#Undead_mobs">https://minecraft.wiki/wiki/Mob#Undead_mobs</a>
     */
    public static final EntitySetTag UNDEADS = new EntitySetTag(keyFor("undeads"))
        .add(DROWNED, HUSK, PHANTOM, SKELETON, SKELETON_HORSE, STRAY, WITHER, WITHER_SKELETON, ZOGLIN, ZOMBIE, ZOMBIE_HORSE, ZOMBIE_VILLAGER, ZOMBIFIED_PIGLIN, BOGGED)
        .lock();

    /**
     * Covers all horses
     */
    public static final EntitySetTag HORSES = new EntitySetTag(keyFor("horses"))
        .contains("HORSE")
        .lock();

    /**
     * Covers all minecarts
     */
    public static final EntitySetTag MINECARTS = new EntitySetTag(keyFor("minecarts"))
        .contains("MINECART")
        .lock();

    /**
     * Covers mobs that split into smaller mobs
     */
    public static final EntitySetTag SPLITTING_MOBS = new EntitySetTag(keyFor("splitting_mobs"))
        .add(SLIME, MAGMA_CUBE)
        .lock();

    /**
     * Covers all water based mobs
     * @see <a href="https://minecraft.wiki/wiki/Mob#Aquatic_mobs">https://minecraft.wiki/wiki/Mob#Aquatic_mobs</a>
     * @deprecated in favour of {@link org.bukkit.Tag#ENTITY_TYPES_AQUATIC}
     */
    @Deprecated
    public static final EntitySetTag WATER_BASED = new EntitySetTag(keyFor("water_based"))
        .add(AXOLOTL, DOLPHIN, SQUID, GLOW_SQUID, GUARDIAN, ELDER_GUARDIAN, TURTLE, COD, SALMON, PUFFERFISH, TROPICAL_FISH, TADPOLE)
        .lock();
}
