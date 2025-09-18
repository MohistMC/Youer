package org.spigotmc;

import net.minecraft.core.BlockPos;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerChunkCache;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.ExperienceOrb;
import net.minecraft.world.entity.FlyingMob;
import net.minecraft.world.entity.LightningBolt;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.PathfinderMob;
import net.minecraft.world.entity.ai.Brain;
import net.minecraft.world.entity.ambient.AmbientCreature;
import net.minecraft.world.entity.animal.Animal;
import net.minecraft.world.entity.animal.Bee;
import net.minecraft.world.entity.animal.Sheep;
import net.minecraft.world.entity.animal.WaterAnimal;
import net.minecraft.world.entity.animal.horse.Llama;
import net.minecraft.world.entity.boss.EnderDragonPart;
import net.minecraft.world.entity.boss.enderdragon.EndCrystal;
import net.minecraft.world.entity.boss.enderdragon.EnderDragon;
import net.minecraft.world.entity.boss.wither.WitherBoss;
import net.minecraft.world.entity.item.PrimedTnt;
import net.minecraft.world.entity.monster.Creeper;
import net.minecraft.world.entity.monster.Enemy;
import net.minecraft.world.entity.monster.Pillager;
import net.minecraft.world.entity.npc.Villager;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.projectile.AbstractArrow;
import net.minecraft.world.entity.projectile.AbstractHurtingProjectile;
import net.minecraft.world.entity.projectile.EyeOfEnder;
import net.minecraft.world.entity.projectile.FireworkRocketEntity;
import net.minecraft.world.entity.projectile.ThrowableProjectile;
import net.minecraft.world.entity.projectile.ThrownTrident;
import net.minecraft.world.entity.raid.Raider;
import net.minecraft.world.entity.schedule.Activity;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.AABB;
// Pufferfish start
import net.minecraft.world.phys.Vec3;
import java.util.List;
// Pufferfish end

public class ActivationRange
{

    public enum ActivationType
    {
        WATER, // Paper
        FLYING_MONSTER, // Paper
        VILLAGER, // Paper
        MONSTER,
        ANIMAL,
        RAIDER,
        MISC;

        AABB boundingBox = new AABB( 0, 0, 0, 0, 0, 0 );
    }
    // Paper start

    static Activity[] VILLAGER_PANIC_IMMUNITIES = {
            Activity.HIDE,
            Activity.PRE_RAID,
            Activity.RAID,
            Activity.PANIC
    };

    private static int checkInactiveWakeup(Entity entity) {
        Level world = entity.level();
        SpigotWorldConfig config = world.spigotConfig;
        long inactiveFor = MinecraftServer.currentTick - entity.activatedTick;
        if (entity.activationType == ActivationType.VILLAGER) {
            if (inactiveFor > config.wakeUpInactiveVillagersEvery && world.wakeupInactiveRemainingVillagers > 0) {
                world.wakeupInactiveRemainingVillagers--;
                return config.wakeUpInactiveVillagersFor;
            }
        } else if (entity.activationType == ActivationType.ANIMAL) {
            if (inactiveFor > config.wakeUpInactiveAnimalsEvery && world.wakeupInactiveRemainingAnimals > 0) {
                world.wakeupInactiveRemainingAnimals--;
                return config.wakeUpInactiveAnimalsFor;
            }
        } else if (entity.activationType == ActivationType.FLYING_MONSTER) {
            if (inactiveFor > config.wakeUpInactiveFlyingEvery && world.wakeupInactiveRemainingFlying > 0) {
                world.wakeupInactiveRemainingFlying--;
                return config.wakeUpInactiveFlyingFor;
            }
        } else if (entity.activationType == ActivationType.MONSTER || entity.activationType == ActivationType.RAIDER) {
            if (inactiveFor > config.wakeUpInactiveMonstersEvery && world.wakeupInactiveRemainingMonsters > 0) {
                world.wakeupInactiveRemainingMonsters--;
                return config.wakeUpInactiveMonstersFor;
            }
        }
        return -1;
    }
    // Paper end

    static AABB maxBB = new AABB( 0, 0, 0, 0, 0, 0 );

    /**
     * Initializes an entities type on construction to specify what group this
     * entity is in for activation ranges.
     *
     * @param entity
     * @return group id
     */
    public static ActivationType initializeEntityActivationType(Entity entity)
    {
        if (entity instanceof WaterAnimal) { return ActivationType.WATER; } // Paper
        else if (entity instanceof Villager) { return ActivationType.VILLAGER; } // Paper
        else if (entity instanceof FlyingMob && entity instanceof Enemy) { return ActivationType.FLYING_MONSTER; } // Paper - doing & Monster incase Flying no longer includes monster in future
        if ( entity instanceof Raider )
        {
            return ActivationType.RAIDER;
        } else if ( entity instanceof Enemy ) // Paper - correct monster check
        {
            return ActivationType.MONSTER;
        } else if ( entity instanceof PathfinderMob || entity instanceof AmbientCreature )
        {
            return ActivationType.ANIMAL;
        } else
        {
            return ActivationType.MISC;
        }
    }

    /**
     * These entities are excluded from Activation range checks.
     *
     * @param entity Entity to initialize
     * @param config Spigot config to determine ranges
     * @return boolean If it should always tick.
     */
    public static boolean initializeEntityActivationState(Entity entity, SpigotWorldConfig config)
    {
        if ( ( entity.activationType == ActivationType.MISC && config.miscActivationRange <= 0 )
                || ( entity.activationType == ActivationType.RAIDER && config.raiderActivationRange <= 0 )
                || ( entity.activationType == ActivationType.ANIMAL && config.animalActivationRange <= 0 )
                || ( entity.activationType == ActivationType.MONSTER && config.monsterActivationRange <= 0 )
                || ( entity.activationType == ActivationType.VILLAGER && config.villagerActivationRange <= 0 ) // Paper
                || ( entity.activationType == ActivationType.WATER && config.waterActivationRange <= 0 ) // Paper
                || ( entity.activationType == ActivationType.FLYING_MONSTER && config.flyingMonsterActivationRange <= 0 ) // Paper
                || entity instanceof EyeOfEnder // Paper
                || entity instanceof Player
                || entity instanceof ThrowableProjectile
                || entity instanceof EnderDragon
                || entity instanceof EnderDragonPart
                || entity instanceof WitherBoss
                || entity instanceof AbstractHurtingProjectile
                || entity instanceof LightningBolt
                || entity instanceof PrimedTnt
                || entity instanceof net.minecraft.world.entity.item.FallingBlockEntity // Paper - Always tick falling blocks
                || entity instanceof net.minecraft.world.entity.vehicle.AbstractMinecart // Paper
                || entity instanceof net.minecraft.world.entity.vehicle.Boat // Paper
                || entity instanceof EndCrystal
                || entity instanceof FireworkRocketEntity
                || entity instanceof ThrownTrident )
        {
            return true;
        }

        return false;
    }

    /**
     * Find what entities are in range of the players in the world and set
     * active if in range.
     *
     * @param world
     */
    public static void activateEntities(Level world)
    {
        //MinecraftTimings.entityActivationCheckTimer.startTiming(); // Purpur
        final int miscActivationRange = world.spigotConfig.miscActivationRange;
        final int raiderActivationRange = world.spigotConfig.raiderActivationRange;
        final int animalActivationRange = world.spigotConfig.animalActivationRange;
        final int monsterActivationRange = world.spigotConfig.monsterActivationRange;
        // Paper start
        final int waterActivationRange = world.spigotConfig.waterActivationRange;
        final int flyingActivationRange = world.spigotConfig.flyingMonsterActivationRange;
        final int villagerActivationRange = world.spigotConfig.villagerActivationRange;
        world.wakeupInactiveRemainingAnimals = Math.min(world.wakeupInactiveRemainingAnimals + 1, world.spigotConfig.wakeUpInactiveAnimals);
        world.wakeupInactiveRemainingVillagers = Math.min(world.wakeupInactiveRemainingVillagers + 1, world.spigotConfig.wakeUpInactiveVillagers);
        world.wakeupInactiveRemainingMonsters = Math.min(world.wakeupInactiveRemainingMonsters + 1, world.spigotConfig.wakeUpInactiveMonsters);
        world.wakeupInactiveRemainingFlying = Math.min(world.wakeupInactiveRemainingFlying + 1, world.spigotConfig.wakeUpInactiveFlying);
        final ServerChunkCache chunkProvider = (ServerChunkCache) world.getChunkSource();
        // Paper end

        int maxRange = Math.max( monsterActivationRange, animalActivationRange );
        maxRange = Math.max( maxRange, raiderActivationRange );
        maxRange = Math.max( maxRange, miscActivationRange );
        // Paper start
        maxRange = Math.max( maxRange, flyingActivationRange );
        maxRange = Math.max( maxRange, waterActivationRange );
        maxRange = Math.max( maxRange, villagerActivationRange );
        // Paper end
        maxRange = Math.min( ( world.spigotConfig.simulationDistance << 4 ) - 8, maxRange );

        for ( Player player : world.players() )
        {
            player.activatedTick = MinecraftServer.currentTick;
            if ( world.spigotConfig.ignoreSpectatorActivation && player.isSpectator() )
            {
                continue;
            }

            if (!player.level().purpurConfig.idleTimeoutTickNearbyEntities && player.isAfk()) continue; // Purpur

            // Paper start
            int worldHeight = world.getHeight();
            ActivationRange.maxBB = player.getBoundingBox().inflate( maxRange, worldHeight, maxRange );
            ActivationType.MISC.boundingBox = player.getBoundingBox().inflate( miscActivationRange, worldHeight, miscActivationRange );
            ActivationType.RAIDER.boundingBox = player.getBoundingBox().inflate( raiderActivationRange, worldHeight, raiderActivationRange );
            ActivationType.ANIMAL.boundingBox = player.getBoundingBox().inflate( animalActivationRange, worldHeight, animalActivationRange );
            ActivationType.MONSTER.boundingBox = player.getBoundingBox().inflate( monsterActivationRange, worldHeight, monsterActivationRange );
            ActivationType.WATER.boundingBox = player.getBoundingBox().inflate( waterActivationRange, worldHeight, waterActivationRange );
            ActivationType.FLYING_MONSTER.boundingBox = player.getBoundingBox().inflate( flyingActivationRange, worldHeight, flyingActivationRange );
            ActivationType.VILLAGER.boundingBox = player.getBoundingBox().inflate( villagerActivationRange, worldHeight, villagerActivationRange );
            // Paper end

            // Paper start
            java.util.List<Entity> entities = world.getEntities((Entity)null, ActivationRange.maxBB, null);
            boolean tickMarkers = world.paperConfig().entities.markers.tick; // Paper - Configurable marker ticking
            for (Entity entity : entities) {
                // Paper start - Configurable marker ticking
                if (!tickMarkers && entity instanceof net.minecraft.world.entity.Marker) {
                    continue;
                }
                // Paper end - Configurable marker ticking
                ActivationRange.activateEntity(entity);

                // Pufferfish start
                if (gg.pufferfish.pufferfish.PufferfishConfig.dearEnabled && entity.getType().dabEnabled) {
                    if (!entity.activatedPriorityReset) {
                        entity.activatedPriorityReset = true;
                        entity.activatedPriority = gg.pufferfish.pufferfish.PufferfishConfig.maximumActivationPrio;
                    }
                    Vec3 playerVec = player.position();
                    Vec3 entityVec = entity.position();
                    double diffX = playerVec.x - entityVec.x, diffY = playerVec.y - entityVec.y, diffZ = playerVec.z - entityVec.z;
                    int squaredDistance = (int) (diffX * diffX + diffY * diffY + diffZ * diffZ);
                    entity.activatedPriority = squaredDistance > gg.pufferfish.pufferfish.PufferfishConfig.startDistanceSquared ?
                            Math.max(1, Math.min(squaredDistance >> gg.pufferfish.pufferfish.PufferfishConfig.activationDistanceMod, entity.activatedPriority)) :
                            1;
                } else {
                    entity.activatedPriority = 1;
                }
                // Pufferfish end

            }
            // Paper end
        }
        //MinecraftTimings.entityActivationCheckTimer.stopTiming(); // Purpur
    }

    /**
     * Checks for the activation state of all entities in this chunk.
     */
    private static void activateEntity(Entity entity)
    {
        if ( MinecraftServer.currentTick > entity.activatedTick )
        {
            if ( entity.defaultActivationState )
            { // Pufferfish - diff on change
                entity.activatedTick = MinecraftServer.currentTick;
                return;
            }
            if ( entity.activationType.boundingBox.intersects( entity.getBoundingBox() ) )
            {  // Pufferfish - diff on change
                entity.activatedTick = MinecraftServer.currentTick;
            }
        }
    }

    /**
     * If an entity is not in range, do some more checks to see if we should
     * give it a shot.
     *
     * @param entity
     * @return
     */
    public static int checkEntityImmunities(Entity entity) // Paper - return # of ticks to get immunity
    {
        // Paper start
        SpigotWorldConfig config = entity.level().spigotConfig;
        int inactiveWakeUpImmunity = checkInactiveWakeup(entity);
        if (inactiveWakeUpImmunity > -1) {
            return inactiveWakeUpImmunity;
        }
        if (entity.getRemainingFireTicks() > 0) {
            return 2;
        }
        if (entity.activatedImmunityTick >= MinecraftServer.currentTick) {
            return 1;
        }
        long inactiveFor = MinecraftServer.currentTick - entity.activatedTick;
        // Paper end
        // quick checks.
        if ( (entity.activationType != ActivationType.WATER && entity.wasTouchingWater && entity.isPushedByFluid()) ) // Paper
        {
            return 100; // Paper
        }
        // Paper start
        if ( !entity.onGround() || entity.getDeltaMovement().horizontalDistanceSqr() > 9.999999747378752E-6D )
        {
            return 100;
        }
        // Paper end
        if ( !( entity instanceof AbstractArrow ) )
        {
            if ( (!entity.onGround() && !(entity instanceof FlyingMob)) ) // Paper - remove passengers logic
            {
                return 10; // Paper
            }
        } else if ( !( (AbstractArrow) entity ).inGround )
        {
            return 1; // Paper
        }
        // special cases.
        if ( entity instanceof LivingEntity )
        {
            LivingEntity living = (LivingEntity) entity;
            if ( living.onClimableCached() || living.jumping || living.hurtTime > 0 || living.activeEffects.size() > 0 || living.isFreezing() ) // Paper // Pufferfish - use cached
            {
                return 1; // Paper
            }
            if ( entity instanceof Mob && ((Mob) entity ).getTarget() != null) // Paper
            {
                return 20; // Paper
            }
            // Paper start
            if (entity instanceof Bee) {
                Bee bee = (Bee)entity;
                BlockPos movingTarget = bee.getMovingTarget();
                if (bee.isAngry() ||
                        (bee.getHivePos() != null && bee.getHivePos().equals(movingTarget)) ||
                        (bee.getSavedFlowerPos() != null && bee.getSavedFlowerPos().equals(movingTarget))
                ) {
                    return 20;
                }
            }
            if ( entity instanceof Villager ) {
                Brain<Villager> behaviorController = ((Villager) entity).getBrain();

                if (config.villagersActiveForPanic) {
                    for (Activity activity : VILLAGER_PANIC_IMMUNITIES) {
                        if (behaviorController.isActive(activity)) {
                            return 20*5;
                        }
                    }
                }

                if (config.villagersWorkImmunityAfter > 0 && inactiveFor >= config.villagersWorkImmunityAfter) {
                    if (behaviorController.isActive(Activity.WORK)) {
                        return config.villagersWorkImmunityFor;
                    }
                }
            }
            if ( entity instanceof Llama && ( (Llama) entity ).inCaravan() )
            {
                return 1;
            }
            // Paper end
            if ( entity instanceof Animal )
            {
                Animal animal = (Animal) entity;
                if ( animal.isBaby() || animal.isInLove() )
                {
                    return 5; // Paper
                }
                if ( entity instanceof Sheep && ( (Sheep) entity ).isSheared() )
                {
                    return 1; // Paper
                }
            }
            if (entity instanceof Creeper && ((Creeper) entity).isIgnited()) { // isExplosive
                return 20; // Paper
            }
            // Paper start
            if (entity instanceof Mob && ((Mob) entity).targetSelector.hasTasks() ) {
                return 0;
            }
            if (entity instanceof Pillager) {
                Pillager pillager = (Pillager) entity;
                // TODO:?
            }
            // Paper end
        }
        // SPIGOT-6644: Otherwise the target refresh tick will be missed
        if (entity instanceof ExperienceOrb) {
            return 20; // Paper
        }
        return -1; // Paper
    }

    /**
     * Checks if the entity is active for this tick.
     *
     * @param entity
     * @return
     */
    public static boolean checkIfActive(Entity entity)
    {
        if (entity.level().purpurConfig.squidImmuneToEAR && entity instanceof net.minecraft.world.entity.animal.Squid) return true; // Purpur
        // Never safe to skip fireworks or entities not yet added to chunk
        if ( entity instanceof FireworkRocketEntity ) {
            return true;
        }
        // Paper start - special case always immunities
        // immunize brand new entities, dead entities, and portal scenarios
        if (entity.defaultActivationState || entity.tickCount < 20*10 || !entity.isAlive() || (entity.portalProcess != null && !entity.portalProcess.hasExpired()) || entity.portalCooldown > 0) {
            return true;
        }
        // immunize leashed entities
        if (entity instanceof Mob && ((Mob)entity).getLeashHolder() instanceof Player) {
            return true;
        }
        // Paper end

        boolean isActive = entity.activatedTick >= MinecraftServer.currentTick;
        entity.isTemporarilyActive = false; // Paper

        // Should this entity tick?
        if ( !isActive )
        {
            if ( ( MinecraftServer.currentTick - entity.activatedTick - 1 ) % 20 == 0 )
            {
                // Check immunities every 20 ticks.
                // Paper start
                int immunity = checkEntityImmunities(entity);
                if (immunity >= 0) {
                    entity.activatedTick = MinecraftServer.currentTick + immunity;
                } else {
                    entity.isTemporarilyActive = true;
                }
                // Paper end
                isActive = true;
            } else if (entity instanceof net.minecraft.world.entity.item.ItemEntity && (entity.tickCount + 1 + entity.getId()) % 4 == 0) { // Paper - Needed for item gravity, see ItemEntity tick. +1 since the entity tick counter is increased AFTER...
                isActive = true;
            }
        }
        // Paper - remove dumb active tick skipping
        return isActive;
    }
}
