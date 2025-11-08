package com.mohistmc.youer.neoforge;

import com.mohistmc.youer.bukkit.entity.YouerModsAbstractHorse;
import com.mohistmc.youer.bukkit.entity.YouerModsChestHorse;
import com.mohistmc.youer.bukkit.entity.YouerModsEntity;
import com.mohistmc.youer.bukkit.entity.YouerModsFireballEntity;
import com.mohistmc.youer.bukkit.entity.YouerModsMinecart;
import com.mohistmc.youer.bukkit.entity.YouerModsMinecartContainer;
import com.mohistmc.youer.bukkit.entity.YouerModsMob;
import com.mohistmc.youer.bukkit.entity.YouerModsProjectileEntity;
import com.mohistmc.youer.bukkit.entity.YouerModsRaider;
import com.mohistmc.youer.bukkit.entity.YouerModsSkeleton;
import com.mohistmc.youer.bukkit.entity.YouerModsThrowableProjectile;
import com.mohistmc.youer.bukkit.entity.YouerModsVehicle;
import com.mohistmc.youer.bukkit.entity.YouerModsWindCharge;
import io.papermc.paper.entity.PaperSchoolableFish;
import io.papermc.paper.entity.SchoolableFish;
import java.lang.invoke.MethodHandle;
import java.lang.invoke.MethodHandles;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.BiFunction;
import java.util.function.Function;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.AgeableMob;
import net.minecraft.world.entity.AreaEffectCloud;
import net.minecraft.world.entity.Display;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.ExperienceOrb;
import net.minecraft.world.entity.FlyingMob;
import net.minecraft.world.entity.GlowSquid;
import net.minecraft.world.entity.Interaction;
import net.minecraft.world.entity.LightningBolt;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Marker;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.OminousItemSpawner;
import net.minecraft.world.entity.PathfinderMob;
import net.minecraft.world.entity.TamableAnimal;
import net.minecraft.world.entity.ambient.AmbientCreature;
import net.minecraft.world.entity.ambient.Bat;
import net.minecraft.world.entity.animal.AbstractFish;
import net.minecraft.world.entity.animal.AbstractGolem;
import net.minecraft.world.entity.animal.AbstractSchoolingFish;
import net.minecraft.world.entity.animal.Animal;
import net.minecraft.world.entity.animal.Bee;
import net.minecraft.world.entity.animal.Cat;
import net.minecraft.world.entity.animal.Chicken;
import net.minecraft.world.entity.animal.Cod;
import net.minecraft.world.entity.animal.Cow;
import net.minecraft.world.entity.animal.Dolphin;
import net.minecraft.world.entity.animal.Fox;
import net.minecraft.world.entity.animal.IronGolem;
import net.minecraft.world.entity.animal.MushroomCow;
import net.minecraft.world.entity.animal.Ocelot;
import net.minecraft.world.entity.animal.Panda;
import net.minecraft.world.entity.animal.Parrot;
import net.minecraft.world.entity.animal.Pig;
import net.minecraft.world.entity.animal.PolarBear;
import net.minecraft.world.entity.animal.Pufferfish;
import net.minecraft.world.entity.animal.Rabbit;
import net.minecraft.world.entity.animal.Salmon;
import net.minecraft.world.entity.animal.Sheep;
import net.minecraft.world.entity.animal.SnowGolem;
import net.minecraft.world.entity.animal.Squid;
import net.minecraft.world.entity.animal.TropicalFish;
import net.minecraft.world.entity.animal.Turtle;
import net.minecraft.world.entity.animal.WaterAnimal;
import net.minecraft.world.entity.animal.Wolf;
import net.minecraft.world.entity.animal.allay.Allay;
import net.minecraft.world.entity.animal.armadillo.Armadillo;
import net.minecraft.world.entity.animal.axolotl.Axolotl;
import net.minecraft.world.entity.animal.camel.Camel;
import net.minecraft.world.entity.animal.frog.Frog;
import net.minecraft.world.entity.animal.frog.Tadpole;
import net.minecraft.world.entity.animal.goat.Goat;
import net.minecraft.world.entity.animal.horse.AbstractChestedHorse;
import net.minecraft.world.entity.animal.horse.AbstractHorse;
import net.minecraft.world.entity.animal.horse.Donkey;
import net.minecraft.world.entity.animal.horse.Horse;
import net.minecraft.world.entity.animal.horse.Llama;
import net.minecraft.world.entity.animal.horse.Mule;
import net.minecraft.world.entity.animal.horse.SkeletonHorse;
import net.minecraft.world.entity.animal.horse.TraderLlama;
import net.minecraft.world.entity.animal.horse.ZombieHorse;
import net.minecraft.world.entity.animal.sniffer.Sniffer;
import net.minecraft.world.entity.boss.EnderDragonPart;
import net.minecraft.world.entity.boss.enderdragon.EndCrystal;
import net.minecraft.world.entity.boss.enderdragon.EnderDragon;
import net.minecraft.world.entity.boss.wither.WitherBoss;
import net.minecraft.world.entity.decoration.ArmorStand;
import net.minecraft.world.entity.decoration.GlowItemFrame;
import net.minecraft.world.entity.decoration.HangingEntity;
import net.minecraft.world.entity.decoration.ItemFrame;
import net.minecraft.world.entity.decoration.LeashFenceKnotEntity;
import net.minecraft.world.entity.decoration.Painting;
import net.minecraft.world.entity.item.FallingBlockEntity;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.item.PrimedTnt;
import net.minecraft.world.entity.monster.AbstractIllager;
import net.minecraft.world.entity.monster.AbstractSkeleton;
import net.minecraft.world.entity.monster.Blaze;
import net.minecraft.world.entity.monster.Bogged;
import net.minecraft.world.entity.monster.CaveSpider;
import net.minecraft.world.entity.monster.Creeper;
import net.minecraft.world.entity.monster.Drowned;
import net.minecraft.world.entity.monster.ElderGuardian;
import net.minecraft.world.entity.monster.EnderMan;
import net.minecraft.world.entity.monster.Endermite;
import net.minecraft.world.entity.monster.Evoker;
import net.minecraft.world.entity.monster.Ghast;
import net.minecraft.world.entity.monster.Giant;
import net.minecraft.world.entity.monster.Guardian;
import net.minecraft.world.entity.monster.Husk;
import net.minecraft.world.entity.monster.Illusioner;
import net.minecraft.world.entity.monster.MagmaCube;
import net.minecraft.world.entity.monster.Monster;
import net.minecraft.world.entity.monster.Phantom;
import net.minecraft.world.entity.monster.Pillager;
import net.minecraft.world.entity.monster.Ravager;
import net.minecraft.world.entity.monster.Shulker;
import net.minecraft.world.entity.monster.Silverfish;
import net.minecraft.world.entity.monster.Skeleton;
import net.minecraft.world.entity.monster.Slime;
import net.minecraft.world.entity.monster.SpellcasterIllager;
import net.minecraft.world.entity.monster.Spider;
import net.minecraft.world.entity.monster.Stray;
import net.minecraft.world.entity.monster.Strider;
import net.minecraft.world.entity.monster.Vex;
import net.minecraft.world.entity.monster.Vindicator;
import net.minecraft.world.entity.monster.Witch;
import net.minecraft.world.entity.monster.WitherSkeleton;
import net.minecraft.world.entity.monster.Zoglin;
import net.minecraft.world.entity.monster.Zombie;
import net.minecraft.world.entity.monster.ZombieVillager;
import net.minecraft.world.entity.monster.ZombifiedPiglin;
import net.minecraft.world.entity.monster.breeze.Breeze;
import net.minecraft.world.entity.monster.hoglin.Hoglin;
import net.minecraft.world.entity.monster.piglin.AbstractPiglin;
import net.minecraft.world.entity.monster.piglin.Piglin;
import net.minecraft.world.entity.monster.piglin.PiglinBrute;
import net.minecraft.world.entity.monster.warden.Warden;
import net.minecraft.world.entity.npc.AbstractVillager;
import net.minecraft.world.entity.npc.Villager;
import net.minecraft.world.entity.npc.WanderingTrader;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.projectile.AbstractArrow;
import net.minecraft.world.entity.projectile.AbstractHurtingProjectile;
import net.minecraft.world.entity.projectile.Arrow;
import net.minecraft.world.entity.projectile.DragonFireball;
import net.minecraft.world.entity.projectile.EvokerFangs;
import net.minecraft.world.entity.projectile.EyeOfEnder;
import net.minecraft.world.entity.projectile.Fireball;
import net.minecraft.world.entity.projectile.FireworkRocketEntity;
import net.minecraft.world.entity.projectile.FishingHook;
import net.minecraft.world.entity.projectile.LargeFireball;
import net.minecraft.world.entity.projectile.LlamaSpit;
import net.minecraft.world.entity.projectile.Projectile;
import net.minecraft.world.entity.projectile.ShulkerBullet;
import net.minecraft.world.entity.projectile.SmallFireball;
import net.minecraft.world.entity.projectile.Snowball;
import net.minecraft.world.entity.projectile.SpectralArrow;
import net.minecraft.world.entity.projectile.ThrowableItemProjectile;
import net.minecraft.world.entity.projectile.ThrownEgg;
import net.minecraft.world.entity.projectile.ThrownEnderpearl;
import net.minecraft.world.entity.projectile.ThrownExperienceBottle;
import net.minecraft.world.entity.projectile.ThrownPotion;
import net.minecraft.world.entity.projectile.ThrownTrident;
import net.minecraft.world.entity.projectile.WitherSkull;
import net.minecraft.world.entity.projectile.windcharge.AbstractWindCharge;
import net.minecraft.world.entity.projectile.windcharge.BreezeWindCharge;
import net.minecraft.world.entity.projectile.windcharge.WindCharge;
import net.minecraft.world.entity.raid.Raider;
import net.minecraft.world.entity.vehicle.AbstractMinecart;
import net.minecraft.world.entity.vehicle.AbstractMinecartContainer;
import net.minecraft.world.entity.vehicle.Boat;
import net.minecraft.world.entity.vehicle.ChestBoat;
import net.minecraft.world.entity.vehicle.Minecart;
import net.minecraft.world.entity.vehicle.MinecartChest;
import net.minecraft.world.entity.vehicle.MinecartCommandBlock;
import net.minecraft.world.entity.vehicle.MinecartFurnace;
import net.minecraft.world.entity.vehicle.MinecartHopper;
import net.minecraft.world.entity.vehicle.MinecartSpawner;
import net.minecraft.world.entity.vehicle.MinecartTNT;
import net.minecraft.world.entity.vehicle.VehicleEntity;
import net.minecraft.world.phys.Vec3;
import org.bukkit.craftbukkit.CraftServer;
import org.bukkit.craftbukkit.entity.CraftAbstractArrow;
import org.bukkit.craftbukkit.entity.CraftAbstractVillager;
import org.bukkit.craftbukkit.entity.CraftAgeable;
import org.bukkit.craftbukkit.entity.CraftAllay;
import org.bukkit.craftbukkit.entity.CraftAmbient;
import org.bukkit.craftbukkit.entity.CraftAnimals;
import org.bukkit.craftbukkit.entity.CraftAreaEffectCloud;
import org.bukkit.craftbukkit.entity.CraftArmadillo;
import org.bukkit.craftbukkit.entity.CraftArmorStand;
import org.bukkit.craftbukkit.entity.CraftArrow;
import org.bukkit.craftbukkit.entity.CraftAxolotl;
import org.bukkit.craftbukkit.entity.CraftBat;
import org.bukkit.craftbukkit.entity.CraftBee;
import org.bukkit.craftbukkit.entity.CraftBlaze;
import org.bukkit.craftbukkit.entity.CraftBlockDisplay;
import org.bukkit.craftbukkit.entity.CraftBoat;
import org.bukkit.craftbukkit.entity.CraftBogged;
import org.bukkit.craftbukkit.entity.CraftBreeze;
import org.bukkit.craftbukkit.entity.CraftBreezeWindCharge;
import org.bukkit.craftbukkit.entity.CraftCamel;
import org.bukkit.craftbukkit.entity.CraftCat;
import org.bukkit.craftbukkit.entity.CraftCaveSpider;
import org.bukkit.craftbukkit.entity.CraftChestBoat;
import org.bukkit.craftbukkit.entity.CraftChicken;
import org.bukkit.craftbukkit.entity.CraftCod;
import org.bukkit.craftbukkit.entity.CraftCow;
import org.bukkit.craftbukkit.entity.CraftCreature;
import org.bukkit.craftbukkit.entity.CraftCreeper;
import org.bukkit.craftbukkit.entity.CraftDisplay;
import org.bukkit.craftbukkit.entity.CraftDolphin;
import org.bukkit.craftbukkit.entity.CraftDonkey;
import org.bukkit.craftbukkit.entity.CraftDragonFireball;
import org.bukkit.craftbukkit.entity.CraftDrowned;
import org.bukkit.craftbukkit.entity.CraftEgg;
import org.bukkit.craftbukkit.entity.CraftElderGuardian;
import org.bukkit.craftbukkit.entity.CraftEnderCrystal;
import org.bukkit.craftbukkit.entity.CraftEnderDragon;
import org.bukkit.craftbukkit.entity.CraftEnderDragonPart;
import org.bukkit.craftbukkit.entity.CraftEnderPearl;
import org.bukkit.craftbukkit.entity.CraftEnderSignal;
import org.bukkit.craftbukkit.entity.CraftEnderman;
import org.bukkit.craftbukkit.entity.CraftEndermite;
import org.bukkit.craftbukkit.entity.CraftEntity;
import org.bukkit.craftbukkit.entity.CraftEntityType;
import org.bukkit.craftbukkit.entity.CraftEntityTypes;
import org.bukkit.craftbukkit.entity.CraftEvoker;
import org.bukkit.craftbukkit.entity.CraftEvokerFangs;
import org.bukkit.craftbukkit.entity.CraftExperienceOrb;
import org.bukkit.craftbukkit.entity.CraftFallingBlock;
import org.bukkit.craftbukkit.entity.CraftFireball;
import org.bukkit.craftbukkit.entity.CraftFirework;
import org.bukkit.craftbukkit.entity.CraftFish;
import org.bukkit.craftbukkit.entity.CraftFishHook;
import org.bukkit.craftbukkit.entity.CraftFlying;
import org.bukkit.craftbukkit.entity.CraftFox;
import org.bukkit.craftbukkit.entity.CraftFrog;
import org.bukkit.craftbukkit.entity.CraftGhast;
import org.bukkit.craftbukkit.entity.CraftGiant;
import org.bukkit.craftbukkit.entity.CraftGlowItemFrame;
import org.bukkit.craftbukkit.entity.CraftGlowSquid;
import org.bukkit.craftbukkit.entity.CraftGoat;
import org.bukkit.craftbukkit.entity.CraftGolem;
import org.bukkit.craftbukkit.entity.CraftGuardian;
import org.bukkit.craftbukkit.entity.CraftHanging;
import org.bukkit.craftbukkit.entity.CraftHoglin;
import org.bukkit.craftbukkit.entity.CraftHorse;
import org.bukkit.craftbukkit.entity.CraftHumanEntity;
import org.bukkit.craftbukkit.entity.CraftHusk;
import org.bukkit.craftbukkit.entity.CraftIllager;
import org.bukkit.craftbukkit.entity.CraftIllusioner;
import org.bukkit.craftbukkit.entity.CraftInteraction;
import org.bukkit.craftbukkit.entity.CraftIronGolem;
import org.bukkit.craftbukkit.entity.CraftItem;
import org.bukkit.craftbukkit.entity.CraftItemDisplay;
import org.bukkit.craftbukkit.entity.CraftItemFrame;
import org.bukkit.craftbukkit.entity.CraftLargeFireball;
import org.bukkit.craftbukkit.entity.CraftLeash;
import org.bukkit.craftbukkit.entity.CraftLightningStrike;
import org.bukkit.craftbukkit.entity.CraftLivingEntity;
import org.bukkit.craftbukkit.entity.CraftLlama;
import org.bukkit.craftbukkit.entity.CraftLlamaSpit;
import org.bukkit.craftbukkit.entity.CraftMagmaCube;
import org.bukkit.craftbukkit.entity.CraftMarker;
import org.bukkit.craftbukkit.entity.CraftMinecartChest;
import org.bukkit.craftbukkit.entity.CraftMinecartCommand;
import org.bukkit.craftbukkit.entity.CraftMinecartFurnace;
import org.bukkit.craftbukkit.entity.CraftMinecartHopper;
import org.bukkit.craftbukkit.entity.CraftMinecartMobSpawner;
import org.bukkit.craftbukkit.entity.CraftMinecartRideable;
import org.bukkit.craftbukkit.entity.CraftMinecartTNT;
import org.bukkit.craftbukkit.entity.CraftMonster;
import org.bukkit.craftbukkit.entity.CraftMule;
import org.bukkit.craftbukkit.entity.CraftMushroomCow;
import org.bukkit.craftbukkit.entity.CraftOcelot;
import org.bukkit.craftbukkit.entity.CraftOminousItemSpawner;
import org.bukkit.craftbukkit.entity.CraftPainting;
import org.bukkit.craftbukkit.entity.CraftPanda;
import org.bukkit.craftbukkit.entity.CraftParrot;
import org.bukkit.craftbukkit.entity.CraftPhantom;
import org.bukkit.craftbukkit.entity.CraftPig;
import org.bukkit.craftbukkit.entity.CraftPigZombie;
import org.bukkit.craftbukkit.entity.CraftPiglin;
import org.bukkit.craftbukkit.entity.CraftPiglinAbstract;
import org.bukkit.craftbukkit.entity.CraftPiglinBrute;
import org.bukkit.craftbukkit.entity.CraftPillager;
import org.bukkit.craftbukkit.entity.CraftPlayer;
import org.bukkit.craftbukkit.entity.CraftPolarBear;
import org.bukkit.craftbukkit.entity.CraftPufferFish;
import org.bukkit.craftbukkit.entity.CraftRabbit;
import org.bukkit.craftbukkit.entity.CraftRavager;
import org.bukkit.craftbukkit.entity.CraftSalmon;
import org.bukkit.craftbukkit.entity.CraftSheep;
import org.bukkit.craftbukkit.entity.CraftShulker;
import org.bukkit.craftbukkit.entity.CraftShulkerBullet;
import org.bukkit.craftbukkit.entity.CraftSilverfish;
import org.bukkit.craftbukkit.entity.CraftSizedFireball;
import org.bukkit.craftbukkit.entity.CraftSkeleton;
import org.bukkit.craftbukkit.entity.CraftSkeletonHorse;
import org.bukkit.craftbukkit.entity.CraftSlime;
import org.bukkit.craftbukkit.entity.CraftSmallFireball;
import org.bukkit.craftbukkit.entity.CraftSniffer;
import org.bukkit.craftbukkit.entity.CraftSnowball;
import org.bukkit.craftbukkit.entity.CraftSnowman;
import org.bukkit.craftbukkit.entity.CraftSpectralArrow;
import org.bukkit.craftbukkit.entity.CraftSpellcaster;
import org.bukkit.craftbukkit.entity.CraftSpider;
import org.bukkit.craftbukkit.entity.CraftSquid;
import org.bukkit.craftbukkit.entity.CraftStray;
import org.bukkit.craftbukkit.entity.CraftStrider;
import org.bukkit.craftbukkit.entity.CraftTNTPrimed;
import org.bukkit.craftbukkit.entity.CraftTadpole;
import org.bukkit.craftbukkit.entity.CraftTameableAnimal;
import org.bukkit.craftbukkit.entity.CraftTextDisplay;
import org.bukkit.craftbukkit.entity.CraftThrownExpBottle;
import org.bukkit.craftbukkit.entity.CraftThrownPotion;
import org.bukkit.craftbukkit.entity.CraftTraderLlama;
import org.bukkit.craftbukkit.entity.CraftTrident;
import org.bukkit.craftbukkit.entity.CraftTropicalFish;
import org.bukkit.craftbukkit.entity.CraftTurtle;
import org.bukkit.craftbukkit.entity.CraftVex;
import org.bukkit.craftbukkit.entity.CraftVillager;
import org.bukkit.craftbukkit.entity.CraftVillagerZombie;
import org.bukkit.craftbukkit.entity.CraftVindicator;
import org.bukkit.craftbukkit.entity.CraftWanderingTrader;
import org.bukkit.craftbukkit.entity.CraftWarden;
import org.bukkit.craftbukkit.entity.CraftWaterMob;
import org.bukkit.craftbukkit.entity.CraftWindCharge;
import org.bukkit.craftbukkit.entity.CraftWitch;
import org.bukkit.craftbukkit.entity.CraftWither;
import org.bukkit.craftbukkit.entity.CraftWitherSkeleton;
import org.bukkit.craftbukkit.entity.CraftWitherSkull;
import org.bukkit.craftbukkit.entity.CraftWolf;
import org.bukkit.craftbukkit.entity.CraftZoglin;
import org.bukkit.craftbukkit.entity.CraftZombie;
import org.bukkit.craftbukkit.entity.CraftZombieHorse;
import org.bukkit.util.Vector;

public class EntityClassLookup {

    private static final MethodHandle H_HANGING;
    private static final MethodHandle H_DIRECTION;
    private static final Map<Class<?>, CraftEntityTypes.EntityTypeData<?, ?>> nmsClassMap = new ConcurrentHashMap<>();
    private static final Map<Class<?>, EntityClass<?>> NMS_TO_BUKKIT = new HashMap<>();

    static {
        try {
            {
                var method = CraftEntityTypes.class.getDeclaredMethod("createHanging", Class.class, BiFunction.class);
                method.setAccessible(true);
                H_HANGING = MethodHandles.lookup().unreflect(method);
            }
            {
                var method = Arrays.stream(CraftEntityTypes.class.getDeclaredClasses()).filter(it -> it.getName().contains("HangingData")).findAny().orElseThrow()
                        .getMethod("direction");
                method.setAccessible(true);
                H_DIRECTION = MethodHandles.lookup().unreflect(method);
            }
        } catch (Throwable t) {
            throw new RuntimeException(t);
        }
    }

    static {
        // abstract types
        add(Entity.class, new EntityClass<>(org.bukkit.entity.Entity.class, YouerModsEntity.class, YouerModsEntity::new));
        add(AbstractSkeleton.class, new EntityClass<>(org.bukkit.entity.AbstractSkeleton.class, YouerModsSkeleton.class, YouerModsSkeleton::new));
        add(Mob.class, new EntityClass<>(org.bukkit.entity.Mob.class, YouerModsMob.class, YouerModsMob::new));
        add(AbstractMinecart.class, new EntityClass<>(org.bukkit.entity.Minecart.class, YouerModsMinecart.class, YouerModsMinecart::new));
        add(AbstractMinecartContainer.class, new EntityClass<>(org.bukkit.entity.Minecart.class, YouerModsMinecartContainer.class, YouerModsMinecartContainer::new));
        add(AbstractHorse.class, new EntityClass<>(org.bukkit.entity.AbstractHorse.class, YouerModsAbstractHorse.class, YouerModsAbstractHorse::new));
        add(AbstractChestedHorse.class, new EntityClass<>(org.bukkit.entity.ChestedHorse.class, YouerModsChestHorse.class, YouerModsChestHorse::new));
        add(Projectile.class, new EntityClass<>(org.bukkit.entity.Projectile.class, YouerModsProjectileEntity.class, YouerModsProjectileEntity::new));
        add(Raider.class, new EntityClass<>(org.bukkit.entity.Raider.class, YouerModsRaider.class, YouerModsRaider::new));
        add(VehicleEntity.class, new EntityClass<>(org.bukkit.entity.Vehicle.class, YouerModsVehicle.class, YouerModsVehicle::new));
        add(AbstractWindCharge.class, new EntityClass<>(org.bukkit.entity.AbstractWindCharge.class, YouerModsWindCharge.class, YouerModsWindCharge::new));
        add(LivingEntity.class, new EntityClass<>(org.bukkit.entity.LivingEntity.class, CraftLivingEntity.class, CraftLivingEntity::new));
        add(Monster.class, new EntityClass<>(org.bukkit.entity.Monster.class, CraftMonster.class, CraftMonster::new));
        add(PathfinderMob.class, new EntityClass<>(org.bukkit.entity.Creature.class, CraftCreature.class, CraftCreature::new));
        add(AgeableMob.class, new EntityClass<>(org.bukkit.entity.Ageable.class, CraftAgeable.class, CraftAgeable::new));
        add(AbstractVillager.class, new EntityClass<>(org.bukkit.entity.AbstractVillager.class, CraftAbstractVillager.class, CraftAbstractVillager::new));
        add(AbstractArrow.class, new EntityClass<>(org.bukkit.entity.AbstractArrow.class, CraftAbstractArrow.class, CraftAbstractArrow::new));
        add(Animal.class, new EntityClass<>(org.bukkit.entity.Animals.class, CraftAnimals.class, CraftAnimals::new));
        add(Fireball.class, new EntityClass<>(org.bukkit.entity.SizedFireball.class, CraftSizedFireball.class, CraftSizedFireball::new));
        add(AbstractHurtingProjectile.class, new EntityClass<>(org.bukkit.entity.Fireball.class, CraftFireball.class, CraftFireball::new));
        add(Display.class, new EntityClass<>(org.bukkit.entity.Display.class, CraftDisplay.class, CraftDisplay::new));
        add(AbstractIllager.class, new EntityClass<>(org.bukkit.entity.Illager.class, CraftIllager.class, CraftIllager::new));
        add(ThrowableItemProjectile.class, new EntityClass<>(org.bukkit.entity.ThrowableProjectile.class, YouerModsThrowableProjectile.class, YouerModsThrowableProjectile::new));
        add(HangingEntity.class, new EntityClass<>(org.bukkit.entity.Hanging.class, CraftHanging.class, CraftHanging::new));
        add(SpellcasterIllager.class, new EntityClass<>(org.bukkit.entity.Spellcaster.class, CraftSpellcaster.class, CraftSpellcaster::new));
        add(AmbientCreature.class, new EntityClass<>(org.bukkit.entity.Ambient.class, CraftAmbient.class, CraftAmbient::new));
        add(TamableAnimal.class, new EntityClass<>(org.bukkit.entity.Tameable.class, CraftTameableAnimal.class, CraftTameableAnimal::new));
        add(AbstractPiglin.class, new EntityClass<>(org.bukkit.entity.PiglinAbstract.class, CraftPiglinAbstract.class, CraftPiglinAbstract::new));
        add(FlyingMob.class, new EntityClass<>(org.bukkit.entity.Flying.class, CraftFlying.class, CraftFlying::new));
        add(WaterAnimal.class, new EntityClass<>(org.bukkit.entity.WaterMob.class, CraftWaterMob.class, CraftWaterMob::new));
        add(AbstractGolem.class, new EntityClass<>(org.bukkit.entity.Golem.class, CraftGolem.class, CraftGolem::new));
        add(Player.class, new EntityClass<>(org.bukkit.entity.HumanEntity.class, CraftHumanEntity.class, CraftHumanEntity::new));
        add(AbstractFish.class, new EntityClass<>(org.bukkit.entity.Fish.class, CraftFish.class, CraftFish::new));
        add(EnderDragonPart.class, new EntityClass<>(org.bukkit.entity.EnderDragonPart.class, CraftEnderDragonPart.class, CraftEnderDragonPart::new));

        // vanilla mob types
        add(ElderGuardian.class, new EntityClass<>(org.bukkit.entity.ElderGuardian.class, CraftElderGuardian.class, CraftElderGuardian::new));
        add(WitherSkeleton.class, new EntityClass<>(org.bukkit.entity.WitherSkeleton.class, CraftWitherSkeleton.class, CraftWitherSkeleton::new));
        add(Stray.class, new EntityClass<>(org.bukkit.entity.Stray.class, CraftStray.class, CraftStray::new));
        add(Husk.class, new EntityClass<>(org.bukkit.entity.Husk.class, CraftHusk.class, CraftHusk::new));
        add(ZombieVillager.class, new EntityClass<>(org.bukkit.entity.ZombieVillager.class, CraftVillagerZombie.class, CraftVillagerZombie::new));
        add(SkeletonHorse.class, new EntityClass<>(org.bukkit.entity.SkeletonHorse.class, CraftSkeletonHorse.class, CraftSkeletonHorse::new));
        add(ZombieHorse.class, new EntityClass<>(org.bukkit.entity.ZombieHorse.class, CraftZombieHorse.class, CraftZombieHorse::new));
        add(ArmorStand.class, new EntityClass<>(org.bukkit.entity.ArmorStand.class, CraftArmorStand.class, CraftArmorStand::new));
        add(Donkey.class, new EntityClass<>(org.bukkit.entity.Donkey.class, CraftDonkey.class, CraftDonkey::new));
        add(Mule.class, new EntityClass<>(org.bukkit.entity.Mule.class, CraftMule.class, CraftMule::new));
        add(Evoker.class, new EntityClass<>(org.bukkit.entity.Evoker.class, CraftEvoker.class, CraftEvoker::new));
        add(Vex.class, new EntityClass<>(org.bukkit.entity.Vex.class, CraftVex.class, CraftVex::new));
        add(Vindicator.class, new EntityClass<>(org.bukkit.entity.Vindicator.class, CraftVindicator.class, CraftVindicator::new));
        add(Illusioner.class, new EntityClass<>(org.bukkit.entity.Illusioner.class, CraftIllusioner.class, CraftIllusioner::new));
        add(Creeper.class, new EntityClass<>(org.bukkit.entity.Creeper.class, CraftCreeper.class, CraftCreeper::new));
        add(Skeleton.class, new EntityClass<>(org.bukkit.entity.Skeleton.class, CraftSkeleton.class, CraftSkeleton::new));
        add(Spider.class, new EntityClass<>(org.bukkit.entity.Spider.class, CraftSpider.class, CraftSpider::new));
        add(Giant.class, new EntityClass<>(org.bukkit.entity.Giant.class, CraftGiant.class, CraftGiant::new));
        add(Zombie.class, new EntityClass<>(org.bukkit.entity.Zombie.class, CraftZombie.class, CraftZombie::new));
        add(Slime.class, new EntityClass<>(org.bukkit.entity.Slime.class, CraftSlime.class, CraftSlime::new));
        add(Ghast.class, new EntityClass<>(org.bukkit.entity.Ghast.class, CraftGhast.class, CraftGhast::new));
        add(ZombifiedPiglin.class, new EntityClass<>(org.bukkit.entity.PigZombie.class, CraftPigZombie.class, CraftPigZombie::new));
        add(EnderMan.class, new EntityClass<>(org.bukkit.entity.Enderman.class, CraftEnderman.class, CraftEnderman::new));
        add(CaveSpider.class, new EntityClass<>(org.bukkit.entity.CaveSpider.class, CraftCaveSpider.class, CraftCaveSpider::new));
        add(Silverfish.class, new EntityClass<>(org.bukkit.entity.Silverfish.class, CraftSilverfish.class, CraftSilverfish::new));
        add(Blaze.class, new EntityClass<>(org.bukkit.entity.Blaze.class, CraftBlaze.class, CraftBlaze::new));
        add(MagmaCube.class, new EntityClass<>(org.bukkit.entity.MagmaCube.class, CraftMagmaCube.class, CraftMagmaCube::new));
        add(WitherBoss.class, new EntityClass<>(org.bukkit.entity.Wither.class, CraftWither.class, CraftWither::new));
        add(Bat.class, new EntityClass<>(org.bukkit.entity.Bat.class, CraftBat.class, CraftBat::new));
        add(Witch.class, new EntityClass<>(org.bukkit.entity.Witch.class, CraftWitch.class, CraftWitch::new));
        add(Endermite.class, new EntityClass<>(org.bukkit.entity.Endermite.class, CraftEndermite.class, CraftEndermite::new));
        add(Guardian.class, new EntityClass<>(org.bukkit.entity.Guardian.class, CraftGuardian.class, CraftGuardian::new));
        add(Shulker.class, new EntityClass<>(org.bukkit.entity.Shulker.class, CraftShulker.class, CraftShulker::new));
        add(Pig.class, new EntityClass<>(org.bukkit.entity.Pig.class, CraftPig.class, CraftPig::new));
        add(Sheep.class, new EntityClass<>(org.bukkit.entity.Sheep.class, CraftSheep.class, CraftSheep::new));
        add(Cow.class, new EntityClass<>(org.bukkit.entity.Cow.class, CraftCow.class, CraftCow::new));
        add(Chicken.class, new EntityClass<>(org.bukkit.entity.Chicken.class, CraftChicken.class, CraftChicken::new));
        add(Squid.class, new EntityClass<>(org.bukkit.entity.Squid.class, CraftSquid.class, CraftSquid::new));
        add(Wolf.class, new EntityClass<>(org.bukkit.entity.Wolf.class, CraftWolf.class, CraftWolf::new));
        add(MushroomCow.class, new EntityClass<>(org.bukkit.entity.MushroomCow.class, CraftMushroomCow.class, CraftMushroomCow::new));
        add(SnowGolem.class, new EntityClass<>(org.bukkit.entity.Snowman.class, CraftSnowman.class, CraftSnowman::new));
        add(Ocelot.class, new EntityClass<>(org.bukkit.entity.Ocelot.class, CraftOcelot.class, CraftOcelot::new));
        add(IronGolem.class, new EntityClass<>(org.bukkit.entity.IronGolem.class, CraftIronGolem.class, CraftIronGolem::new));
        add(Horse.class, new EntityClass<>(org.bukkit.entity.Horse.class, CraftHorse.class, CraftHorse::new));
        add(Rabbit.class, new EntityClass<>(org.bukkit.entity.Rabbit.class, CraftRabbit.class, CraftRabbit::new));
        add(PolarBear.class, new EntityClass<>(org.bukkit.entity.PolarBear.class, CraftPolarBear.class, CraftPolarBear::new));
        add(Llama.class, new EntityClass<>(org.bukkit.entity.Llama.class, CraftLlama.class, CraftLlama::new));
        add(Parrot.class, new EntityClass<>(org.bukkit.entity.Parrot.class, CraftParrot.class, CraftParrot::new));
        add(Villager.class, new EntityClass<>(org.bukkit.entity.Villager.class, CraftVillager.class, CraftVillager::new));
        add(Turtle.class, new EntityClass<>(org.bukkit.entity.Turtle.class, CraftTurtle.class, CraftTurtle::new));
        add(Phantom.class, new EntityClass<>(org.bukkit.entity.Phantom.class, CraftPhantom.class, CraftPhantom::new));
        add(Cod.class, new EntityClass<>(org.bukkit.entity.Cod.class, CraftCod.class, CraftCod::new));
        add(Salmon.class, new EntityClass<>(org.bukkit.entity.Salmon.class, CraftSalmon.class, CraftSalmon::new));
        add(Pufferfish.class, new EntityClass<>(org.bukkit.entity.PufferFish.class, CraftPufferFish.class, CraftPufferFish::new));
        add(TropicalFish.class, new EntityClass<>(org.bukkit.entity.TropicalFish.class, CraftTropicalFish.class, CraftTropicalFish::new));
        add(AbstractSchoolingFish.class, new EntityClass<>(SchoolableFish.class, PaperSchoolableFish.class, PaperSchoolableFish::new));
        add(Drowned.class, new EntityClass<>(org.bukkit.entity.Drowned.class, CraftDrowned.class, CraftDrowned::new));
        add(Dolphin.class, new EntityClass<>(org.bukkit.entity.Dolphin.class, CraftDolphin.class, CraftDolphin::new));
        add(Cat.class, new EntityClass<>(org.bukkit.entity.Cat.class, CraftCat.class, CraftCat::new));
        add(Panda.class, new EntityClass<>(org.bukkit.entity.Panda.class, CraftPanda.class, CraftPanda::new));
        add(Pillager.class, new EntityClass<>(org.bukkit.entity.Pillager.class, CraftPillager.class, CraftPillager::new));
        add(Ravager.class, new EntityClass<>(org.bukkit.entity.Ravager.class, CraftRavager.class, CraftRavager::new));
        add(TraderLlama.class, new EntityClass<>(org.bukkit.entity.TraderLlama.class, CraftTraderLlama.class, CraftTraderLlama::new));
        add(WanderingTrader.class, new EntityClass<>(org.bukkit.entity.WanderingTrader.class, CraftWanderingTrader.class, CraftWanderingTrader::new));
        add(Fox.class, new EntityClass<>(org.bukkit.entity.Fox.class, CraftFox.class, CraftFox::new));
        add(Bee.class, new EntityClass<>(org.bukkit.entity.Bee.class, CraftBee.class, CraftBee::new));
        add(Hoglin.class, new EntityClass<>(org.bukkit.entity.Hoglin.class, CraftHoglin.class, CraftHoglin::new));
        add(Piglin.class, new EntityClass<>(org.bukkit.entity.Piglin.class, CraftPiglin.class, CraftPiglin::new));
        add(Strider.class, new EntityClass<>(org.bukkit.entity.Strider.class, CraftStrider.class, CraftStrider::new));
        add(Zoglin.class, new EntityClass<>(org.bukkit.entity.Zoglin.class, CraftZoglin.class, CraftZoglin::new));
        add(PiglinBrute.class, new EntityClass<>(org.bukkit.entity.PiglinBrute.class, CraftPiglinBrute.class, CraftPiglinBrute::new));
        add(Axolotl.class, new EntityClass<>(org.bukkit.entity.Axolotl.class, CraftAxolotl.class, CraftAxolotl::new));
        add(GlowSquid.class, new EntityClass<>(org.bukkit.entity.GlowSquid.class, CraftGlowSquid.class, CraftGlowSquid::new));
        add(Goat.class, new EntityClass<>(org.bukkit.entity.Goat.class, CraftGoat.class, CraftGoat::new));
        add(Allay.class, new EntityClass<>(org.bukkit.entity.Allay.class, CraftAllay.class, CraftAllay::new));
        add(Frog.class, new EntityClass<>(org.bukkit.entity.Frog.class, CraftFrog.class, CraftFrog::new));
        add(Tadpole.class, new EntityClass<>(org.bukkit.entity.Tadpole.class, CraftTadpole.class, CraftTadpole::new));
        add(Warden.class, new EntityClass<>(org.bukkit.entity.Warden.class, CraftWarden.class, CraftWarden::new));
        add(Camel.class, new EntityClass<>(org.bukkit.entity.Camel.class, CraftCamel.class, CraftCamel::new));
        add(Sniffer.class, new EntityClass<>(org.bukkit.entity.Sniffer.class, CraftSniffer.class, CraftSniffer::new));
        add(Breeze.class, new EntityClass<>(org.bukkit.entity.Breeze.class, CraftBreeze.class, CraftBreeze::new));
        add(EnderDragon.class, new EntityClass<>(org.bukkit.entity.EnderDragon.class, CraftEnderDragon.class, CraftEnderDragon::new));
        add(LargeFireball.class, new EntityClass<>(org.bukkit.entity.LargeFireball.class, CraftLargeFireball.class, CraftLargeFireball::new));
        add(SmallFireball.class, new EntityClass<>(org.bukkit.entity.SmallFireball.class, CraftSmallFireball.class, CraftSmallFireball::new));
        add(WitherSkull.class, new EntityClass<>(org.bukkit.entity.WitherSkull.class, CraftWitherSkull.class, CraftWitherSkull::new));
        add(DragonFireball.class, new EntityClass<>(org.bukkit.entity.DragonFireball.class, CraftDragonFireball.class, CraftDragonFireball::new));
        add(WindCharge.class, new EntityClass<>(org.bukkit.entity.WindCharge.class, CraftWindCharge.class, CraftWindCharge::new));
        add(Painting.class, new EntityClass<>(org.bukkit.entity.Painting.class, CraftPainting.class, CraftPainting::new));
        add(ItemFrame.class, new EntityClass<>(org.bukkit.entity.ItemFrame.class, CraftItemFrame.class, CraftItemFrame::new));
        add(GlowItemFrame.class, new EntityClass<>(org.bukkit.entity.GlowItemFrame.class, CraftGlowItemFrame.class, CraftGlowItemFrame::new));
        add(Arrow.class, new EntityClass<>(org.bukkit.entity.Arrow.class, CraftArrow.class, CraftArrow::new));
        add(ThrownEnderpearl.class, new EntityClass<>(org.bukkit.entity.EnderPearl.class, CraftEnderPearl.class, CraftEnderPearl::new));
        add(ThrownExperienceBottle.class, new EntityClass<>(org.bukkit.entity.ThrownExpBottle.class, CraftThrownExpBottle.class, CraftThrownExpBottle::new));
        add(SpectralArrow.class, new EntityClass<>(org.bukkit.entity.SpectralArrow.class, CraftSpectralArrow.class, CraftSpectralArrow::new));
        add(EndCrystal.class, new EntityClass<>(org.bukkit.entity.EnderCrystal.class, CraftEnderCrystal.class, CraftEnderCrystal::new));
        add(ThrownTrident.class, new EntityClass<>(org.bukkit.entity.Trident.class, CraftTrident.class, CraftTrident::new));
        add(LightningBolt.class, new EntityClass<>(org.bukkit.entity.LightningStrike.class, CraftLightningStrike.class, CraftLightningStrike::new));
        add(ShulkerBullet.class, new EntityClass<>(org.bukkit.entity.ShulkerBullet.class, CraftShulkerBullet.class, CraftShulkerBullet::new));
        add(Boat.class, new EntityClass<>(org.bukkit.entity.Boat.class, CraftBoat.class, CraftBoat::new));
        add(LlamaSpit.class, new EntityClass<>(org.bukkit.entity.LlamaSpit.class, CraftLlamaSpit.class, CraftLlamaSpit::new));
        add(ChestBoat.class, new EntityClass<>(org.bukkit.entity.ChestBoat.class, CraftChestBoat.class, CraftChestBoat::new));
        add(Marker.class, new EntityClass<>(org.bukkit.entity.Marker.class, CraftMarker.class, CraftMarker::new));
        add(Display.BlockDisplay.class, new EntityClass<>(org.bukkit.entity.BlockDisplay.class, CraftBlockDisplay.class, CraftBlockDisplay::new));
        add(Interaction.class, new EntityClass<>(org.bukkit.entity.Interaction.class, CraftInteraction.class, CraftInteraction::new));
        add(Display.ItemDisplay.class, new EntityClass<>(org.bukkit.entity.ItemDisplay.class, CraftItemDisplay.class, CraftItemDisplay::new));
        add(Display.TextDisplay.class, new EntityClass<>(org.bukkit.entity.TextDisplay.class, CraftTextDisplay.class, CraftTextDisplay::new));
        add(ItemEntity.class, new EntityClass<>(org.bukkit.entity.Item.class, CraftItem.class, CraftItem::new));
        add(ExperienceOrb.class, new EntityClass<>(org.bukkit.entity.ExperienceOrb.class, CraftExperienceOrb.class, CraftExperienceOrb::new));
        add(AreaEffectCloud.class, new EntityClass<>(org.bukkit.entity.AreaEffectCloud.class, CraftAreaEffectCloud.class, CraftAreaEffectCloud::new));
        add(ThrownEgg.class, new EntityClass<>(org.bukkit.entity.Egg.class, CraftEgg.class, CraftEgg::new));
        add(LeashFenceKnotEntity.class, new EntityClass<>(org.bukkit.entity.LeashHitch.class, CraftLeash.class, CraftLeash::new));
        add(Snowball.class, new EntityClass<>(org.bukkit.entity.Snowball.class, CraftSnowball.class, CraftSnowball::new));
        add(EyeOfEnder.class, new EntityClass<>(org.bukkit.entity.EnderSignal.class, CraftEnderSignal.class, CraftEnderSignal::new));
        add(ThrownPotion.class, new EntityClass<>(org.bukkit.entity.ThrownPotion.class, CraftThrownPotion.class, CraftThrownPotion::new));
        add(PrimedTnt.class, new EntityClass<>(org.bukkit.entity.TNTPrimed.class, CraftTNTPrimed.class, CraftTNTPrimed::new));
        add(FallingBlockEntity.class, new EntityClass<>(org.bukkit.entity.FallingBlock.class, CraftFallingBlock.class, CraftFallingBlock::new));
        add(FireworkRocketEntity.class, new EntityClass<>(org.bukkit.entity.Firework.class, CraftFirework.class, CraftFirework::new));
        add(EvokerFangs.class, new EntityClass<>(org.bukkit.entity.EvokerFangs.class, CraftEvokerFangs.class, CraftEvokerFangs::new));
        add(MinecartCommandBlock.class, new EntityClass<>(org.bukkit.entity.minecart.CommandMinecart.class, CraftMinecartCommand.class, CraftMinecartCommand::new));
        add(Minecart.class, new EntityClass<>(org.bukkit.entity.minecart.RideableMinecart.class, CraftMinecartRideable.class, CraftMinecartRideable::new));
        add(MinecartChest.class, new EntityClass<>(org.bukkit.entity.minecart.StorageMinecart.class, CraftMinecartChest.class, CraftMinecartChest::new));
        add(MinecartFurnace.class, new EntityClass<>(org.bukkit.entity.minecart.PoweredMinecart.class, CraftMinecartFurnace.class, CraftMinecartFurnace::new));
        add(MinecartTNT.class, new EntityClass<>(org.bukkit.entity.minecart.ExplosiveMinecart.class, CraftMinecartTNT.class, CraftEntityTypes.getEntityTypeData(org.bukkit.entity.minecart.ExplosiveMinecart.class).convertFunction()::apply));
        add(MinecartHopper.class, new EntityClass<>(org.bukkit.entity.minecart.HopperMinecart.class, CraftMinecartHopper.class, CraftMinecartHopper::new));
        add(MinecartSpawner.class, new EntityClass<>(org.bukkit.entity.minecart.SpawnerMinecart.class, CraftMinecartMobSpawner.class, CraftEntityTypes.getEntityTypeData(org.bukkit.entity.minecart.SpawnerMinecart.class).convertFunction()::apply));
        add(FishingHook.class, new EntityClass<>(org.bukkit.entity.FishHook.class, CraftFishHook.class, CraftFishHook::new));
        add(ServerPlayer.class, new EntityClass<>(org.bukkit.entity.Player.class, CraftPlayer.class, CraftPlayer::new));
        add(Bogged.class, new EntityClass<>(org.bukkit.entity.Bogged.class, CraftBogged.class, CraftBogged::new));
        add(OminousItemSpawner.class, new EntityClass<>(org.bukkit.entity.OminousItemSpawner.class, CraftOminousItemSpawner.class, CraftOminousItemSpawner::new));
        add(Armadillo.class, new EntityClass<>(org.bukkit.entity.Armadillo.class, CraftArmadillo.class, CraftArmadillo::new));
        add(BreezeWindCharge.class, new EntityClass<>(org.bukkit.entity.BreezeWindCharge.class, CraftBreezeWindCharge.class, CraftBreezeWindCharge::new));
    }

    @SuppressWarnings("unchecked")
    private static <R extends HangingEntity> Function<CraftEntityTypes.SpawnData, R> createHanging(Class<org.bukkit.entity.Entity> clazz, BiFunction<CraftEntityTypes.SpawnData, Direction, R> spawnFunction) {
        BiFunction<CraftEntityTypes.SpawnData, ?, R> callback = (spawnData, o) -> {
            try {
                var direction = (Direction) H_DIRECTION.invoke(o);
                return spawnFunction.apply(spawnData, direction);
            } catch (Throwable e) {
                throw new RuntimeException(e);
            }
        };
        try {
            return (Function<CraftEntityTypes.SpawnData, R>) H_HANGING.invoke(clazz, callback);
        } catch (Throwable e) {
            throw new RuntimeException(e);
        }
    }

    public static void init() {
        var allEntityClasses = new HashSet<Class<?>>();
        for (var bukkitType : org.bukkit.entity.EntityType.values()) {
            if (bukkitType != null) {
                Class<? extends org.bukkit.entity.Entity> entityClass = bukkitType.getEntityClass();
                if (entityClass != null && !allEntityClasses.contains(entityClass)) {
                    var next = new LinkedList<Class<?>>();
                    next.add(entityClass);
                    while (!next.isEmpty()) {
                        Class<?> cl = next.pollFirst();
                        if (!allEntityClasses.contains(cl)) {
                            allEntityClasses.add(cl);
                            for (Class<?> intf : cl.getInterfaces()) {
                                if (org.bukkit.entity.Entity.class.isAssignableFrom(intf)) {
                                    next.addLast(intf);
                                }
                            }
                        }
                    }
                }
            }
        }
        Set<Class<?>> ignored = Set.of(
                org.bukkit.entity.Explosive.class,
                org.bukkit.entity.Damageable.class,
                org.bukkit.entity.NPC.class,
                org.bukkit.entity.Boss.class,
                org.bukkit.entity.Breedable.class,
                org.bukkit.entity.Steerable.class,
                org.bukkit.entity.Enemy.class,
                org.bukkit.entity.ComplexLivingEntity.class,
                com.destroystokyo.paper.entity.RangedEntity.class,
                io.papermc.paper.entity.Leashable.class,
                io.papermc.paper.entity.Bucketable.class,
                io.papermc.paper.entity.Shearable.class,
                io.papermc.paper.entity.CollarColorable.class
        );
        boolean error = false;
        var adadasd = new HashSet<>(allEntityClasses);
        for (Class<?> entityClass : allEntityClasses) {
            if (ignored.contains(entityClass)) continue;
            var optional = NMS_TO_BUKKIT.values().stream().filter(c -> c.bukkitClass == entityClass).findAny();
            if (optional.isEmpty()) {
                adadasd.remove(entityClass);
                error = true;
            }
        }
        if (error) {
            throw new RuntimeException("Missing valid entity class mapping: " + adadasd);
        }
    }

    @SuppressWarnings("unchecked")
    public static <T extends Entity> CraftEntityTypes.EntityTypeData<?, T> getEntityTypeData(T entity) {
        return (CraftEntityTypes.EntityTypeData<?, T>) nmsClassMap.computeIfAbsent(entity.getClass(), k -> getEntityTypeData(k, entity.getType()));
    }

    @SuppressWarnings("unchecked")
    public static <T extends Entity> CraftEntityTypes.EntityTypeData<?, T> getEntityTypeData(Class<?> type, EntityType<T> entityType) {
        EntityClass<?> entityClass = null;
        for (Class<?> c = type; entityClass == null; c = c.getSuperclass()) {
            entityClass = NMS_TO_BUKKIT.get(c);
        }
        var bukkitType = CraftEntityType.minecraftToBukkit(entityType);
        EntityClass<T> finalEntityClass = (EntityClass<T>) entityClass;
        return new CraftEntityTypes.EntityTypeData<>(
                bukkitType, (Class<org.bukkit.entity.Entity>) entityClass.bukkitClass,
                finalEntityClass.convert,
                spawnData -> spawnDynamic(finalEntityClass, bukkitType, spawnData)
        );
    }

    @SuppressWarnings("unchecked")
    private static <T extends Entity> T spawnDynamic(EntityClass<T> entityClass, org.bukkit.entity.EntityType bukkitType, CraftEntityTypes.SpawnData spawnData) {
        var entity = bukkitType.getFactory().apply(spawnData.location());
        if (entity == null) {
            return null;
        }
        if (entity instanceof AbstractHurtingProjectile) {
            Vector direction = spawnData.location().getDirection();
            ((AbstractHurtingProjectile) entity).assignDirectionalMovement(new Vec3(direction.getX(), direction.getY(), direction.getZ()), 1.0);
        }
        if (entity instanceof HangingEntity) {
            createHanging((Class<org.bukkit.entity.Entity>) entityClass.bukkitClass, (a, direction) -> {
                ((HangingEntity) entity).setDirection(direction);
                return (HangingEntity) entity;
            }).apply(spawnData);
        }
        return (T) entity;
    }

    private static <U extends V, V extends Entity> void add(Class<? super U> cl, EntityClass<? super V> entityClass) {
        if (NMS_TO_BUKKIT.put(cl, entityClass) != null) {
            throw new IllegalStateException("Duplicate " + cl + " mapping");
        }
    }

    private record EntityClass<T extends Entity>(Class<? extends org.bukkit.entity.Entity> bukkitClass,
                                                 Class<? extends CraftEntity> implClass,
                                                 BiFunction<CraftServer, T, org.bukkit.entity.Entity> convert) {
        private EntityClass {
            if (!bukkitClass.isAssignableFrom(implClass)) {
                throw new IllegalArgumentException(bukkitClass + " " + implClass);
            }
        }
    }
}
