package org.bukkit.craftbukkit.block.data;

import com.google.common.base.Function;
import com.google.common.base.Preconditions;
import com.google.common.collect.ImmutableSet;
import com.mojang.brigadier.StringReader;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;
import net.minecraft.commands.arguments.blocks.BlockStateParser;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.util.StringRepresentable;
import net.minecraft.world.level.EmptyBlockGetter;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Rotation;
import net.minecraft.world.level.block.state.StateHolder;
import net.minecraft.world.level.block.state.properties.BooleanProperty;
import net.minecraft.world.level.block.state.properties.EnumProperty;
import net.minecraft.world.level.block.state.properties.IntegerProperty;
import net.minecraft.world.level.block.state.properties.Property;
import org.bukkit.Color;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.SoundGroup;
import org.bukkit.block.BlockFace;
import org.bukkit.block.BlockState;
import org.bukkit.block.BlockSupport;
import org.bukkit.block.BlockType;
import org.bukkit.block.PistonMoveReaction;
import org.bukkit.block.data.BlockData;
import org.bukkit.block.structure.Mirror;
import org.bukkit.block.structure.StructureRotation;
import org.bukkit.craftbukkit.CraftSoundGroup;
import org.bukkit.craftbukkit.CraftWorld;
import org.bukkit.craftbukkit.block.CraftBlock;
import org.bukkit.craftbukkit.block.CraftBlockStates;
import org.bukkit.craftbukkit.block.CraftBlockSupport;
import org.bukkit.craftbukkit.block.CraftBlockType;
import org.bukkit.craftbukkit.block.impl.CraftAmethystCluster;
import org.bukkit.craftbukkit.block.impl.CraftAnvil;
import org.bukkit.craftbukkit.block.impl.CraftBamboo;
import org.bukkit.craftbukkit.block.impl.CraftBanner;
import org.bukkit.craftbukkit.block.impl.CraftBannerWall;
import org.bukkit.craftbukkit.block.impl.CraftBarrel;
import org.bukkit.craftbukkit.block.impl.CraftBarrier;
import org.bukkit.craftbukkit.block.impl.CraftBed;
import org.bukkit.craftbukkit.block.impl.CraftBeehive;
import org.bukkit.craftbukkit.block.impl.CraftBeetroot;
import org.bukkit.craftbukkit.block.impl.CraftBell;
import org.bukkit.craftbukkit.block.impl.CraftBigDripleaf;
import org.bukkit.craftbukkit.block.impl.CraftBigDripleafStem;
import org.bukkit.craftbukkit.block.impl.CraftBlastFurnace;
import org.bukkit.craftbukkit.block.impl.CraftBrewingStand;
import org.bukkit.craftbukkit.block.impl.CraftBrushable;
import org.bukkit.craftbukkit.block.impl.CraftBubbleColumn;
import org.bukkit.craftbukkit.block.impl.CraftButtonAbstract;
import org.bukkit.craftbukkit.block.impl.CraftCactus;
import org.bukkit.craftbukkit.block.impl.CraftCake;
import org.bukkit.craftbukkit.block.impl.CraftCalibratedSculkSensor;
import org.bukkit.craftbukkit.block.impl.CraftCampfire;
import org.bukkit.craftbukkit.block.impl.CraftCandle;
import org.bukkit.craftbukkit.block.impl.CraftCandleCake;
import org.bukkit.craftbukkit.block.impl.CraftCarrots;
import org.bukkit.craftbukkit.block.impl.CraftCaveVines;
import org.bukkit.craftbukkit.block.impl.CraftCaveVinesPlant;
import org.bukkit.craftbukkit.block.impl.CraftCeilingHangingSign;
import org.bukkit.craftbukkit.block.impl.CraftChain;
import org.bukkit.craftbukkit.block.impl.CraftCherryLeaves;
import org.bukkit.craftbukkit.block.impl.CraftChest;
import org.bukkit.craftbukkit.block.impl.CraftChestTrapped;
import org.bukkit.craftbukkit.block.impl.CraftChiseledBookShelf;
import org.bukkit.craftbukkit.block.impl.CraftChorusFlower;
import org.bukkit.craftbukkit.block.impl.CraftChorusFruit;
import org.bukkit.craftbukkit.block.impl.CraftCobbleWall;
import org.bukkit.craftbukkit.block.impl.CraftCocoa;
import org.bukkit.craftbukkit.block.impl.CraftCommand;
import org.bukkit.craftbukkit.block.impl.CraftComposter;
import org.bukkit.craftbukkit.block.impl.CraftConduit;
import org.bukkit.craftbukkit.block.impl.CraftCopperBulb;
import org.bukkit.craftbukkit.block.impl.CraftCoralDead;
import org.bukkit.craftbukkit.block.impl.CraftCoralFan;
import org.bukkit.craftbukkit.block.impl.CraftCoralFanAbstract;
import org.bukkit.craftbukkit.block.impl.CraftCoralFanWall;
import org.bukkit.craftbukkit.block.impl.CraftCoralFanWallAbstract;
import org.bukkit.craftbukkit.block.impl.CraftCoralPlant;
import org.bukkit.craftbukkit.block.impl.CraftCrafter;
import org.bukkit.craftbukkit.block.impl.CraftCrops;
import org.bukkit.craftbukkit.block.impl.CraftDaylightDetector;
import org.bukkit.craftbukkit.block.impl.CraftDecoratedPot;
import org.bukkit.craftbukkit.block.impl.CraftDirtSnow;
import org.bukkit.craftbukkit.block.impl.CraftDispenser;
import org.bukkit.craftbukkit.block.impl.CraftDoor;
import org.bukkit.craftbukkit.block.impl.CraftDropper;
import org.bukkit.craftbukkit.block.impl.CraftEndRod;
import org.bukkit.craftbukkit.block.impl.CraftEnderChest;
import org.bukkit.craftbukkit.block.impl.CraftEnderPortalFrame;
import org.bukkit.craftbukkit.block.impl.CraftEquipableCarvedPumpkin;
import org.bukkit.craftbukkit.block.impl.CraftFence;
import org.bukkit.craftbukkit.block.impl.CraftFenceGate;
import org.bukkit.craftbukkit.block.impl.CraftFire;
import org.bukkit.craftbukkit.block.impl.CraftFloorSign;
import org.bukkit.craftbukkit.block.impl.CraftFluids;
import org.bukkit.craftbukkit.block.impl.CraftFurnaceFurace;
import org.bukkit.craftbukkit.block.impl.CraftGlazedTerracotta;
import org.bukkit.craftbukkit.block.impl.CraftGlowLichen;
import org.bukkit.craftbukkit.block.impl.CraftGrass;
import org.bukkit.craftbukkit.block.impl.CraftGrindstone;
import org.bukkit.craftbukkit.block.impl.CraftHangingRoots;
import org.bukkit.craftbukkit.block.impl.CraftHay;
import org.bukkit.craftbukkit.block.impl.CraftHeavyCore;
import org.bukkit.craftbukkit.block.impl.CraftHopper;
import org.bukkit.craftbukkit.block.impl.CraftHugeMushroom;
import org.bukkit.craftbukkit.block.impl.CraftIceFrost;
import org.bukkit.craftbukkit.block.impl.CraftInfestedRotatedPillar;
import org.bukkit.craftbukkit.block.impl.CraftIronBars;
import org.bukkit.craftbukkit.block.impl.CraftJigsaw;
import org.bukkit.craftbukkit.block.impl.CraftJukeBox;
import org.bukkit.craftbukkit.block.impl.CraftKelp;
import org.bukkit.craftbukkit.block.impl.CraftLadder;
import org.bukkit.craftbukkit.block.impl.CraftLantern;
import org.bukkit.craftbukkit.block.impl.CraftLayeredCauldron;
import org.bukkit.craftbukkit.block.impl.CraftLeaves;
import org.bukkit.craftbukkit.block.impl.CraftLectern;
import org.bukkit.craftbukkit.block.impl.CraftLever;
import org.bukkit.craftbukkit.block.impl.CraftLight;
import org.bukkit.craftbukkit.block.impl.CraftLightningRod;
import org.bukkit.craftbukkit.block.impl.CraftLoom;
import org.bukkit.craftbukkit.block.impl.CraftMangroveLeaves;
import org.bukkit.craftbukkit.block.impl.CraftMangrovePropagule;
import org.bukkit.craftbukkit.block.impl.CraftMangroveRoots;
import org.bukkit.craftbukkit.block.impl.CraftMinecartDetector;
import org.bukkit.craftbukkit.block.impl.CraftMinecartTrack;
import org.bukkit.craftbukkit.block.impl.CraftMycel;
import org.bukkit.craftbukkit.block.impl.CraftNetherWart;
import org.bukkit.craftbukkit.block.impl.CraftNote;
import org.bukkit.craftbukkit.block.impl.CraftObserver;
import org.bukkit.craftbukkit.block.impl.CraftPiglinWallSkull;
import org.bukkit.craftbukkit.block.impl.CraftPinkPetals;
import org.bukkit.craftbukkit.block.impl.CraftPiston;
import org.bukkit.craftbukkit.block.impl.CraftPistonExtension;
import org.bukkit.craftbukkit.block.impl.CraftPistonMoving;
import org.bukkit.craftbukkit.block.impl.CraftPitcherCrop;
import org.bukkit.craftbukkit.block.impl.CraftPointedDripstone;
import org.bukkit.craftbukkit.block.impl.CraftPortal;
import org.bukkit.craftbukkit.block.impl.CraftPotatoes;
import org.bukkit.craftbukkit.block.impl.CraftPoweredRail;
import org.bukkit.craftbukkit.block.impl.CraftPressurePlateBinary;
import org.bukkit.craftbukkit.block.impl.CraftPressurePlateWeighted;
import org.bukkit.craftbukkit.block.impl.CraftPumpkinCarved;
import org.bukkit.craftbukkit.block.impl.CraftRedstoneComparator;
import org.bukkit.craftbukkit.block.impl.CraftRedstoneLamp;
import org.bukkit.craftbukkit.block.impl.CraftRedstoneOre;
import org.bukkit.craftbukkit.block.impl.CraftRedstoneTorch;
import org.bukkit.craftbukkit.block.impl.CraftRedstoneTorchWall;
import org.bukkit.craftbukkit.block.impl.CraftRedstoneWire;
import org.bukkit.craftbukkit.block.impl.CraftReed;
import org.bukkit.craftbukkit.block.impl.CraftRepeater;
import org.bukkit.craftbukkit.block.impl.CraftRespawnAnchor;
import org.bukkit.craftbukkit.block.impl.CraftRotatable;
import org.bukkit.craftbukkit.block.impl.CraftSapling;
import org.bukkit.craftbukkit.block.impl.CraftScaffolding;
import org.bukkit.craftbukkit.block.impl.CraftSculkCatalyst;
import org.bukkit.craftbukkit.block.impl.CraftSculkSensor;
import org.bukkit.craftbukkit.block.impl.CraftSculkShrieker;
import org.bukkit.craftbukkit.block.impl.CraftSculkVein;
import org.bukkit.craftbukkit.block.impl.CraftSeaPickle;
import org.bukkit.craftbukkit.block.impl.CraftShulkerBox;
import org.bukkit.craftbukkit.block.impl.CraftSkull;
import org.bukkit.craftbukkit.block.impl.CraftSkullPlayer;
import org.bukkit.craftbukkit.block.impl.CraftSkullPlayerWall;
import org.bukkit.craftbukkit.block.impl.CraftSkullWall;
import org.bukkit.craftbukkit.block.impl.CraftSmallDripleaf;
import org.bukkit.craftbukkit.block.impl.CraftSmoker;
import org.bukkit.craftbukkit.block.impl.CraftSnifferEgg;
import org.bukkit.craftbukkit.block.impl.CraftSnow;
import org.bukkit.craftbukkit.block.impl.CraftSoil;
import org.bukkit.craftbukkit.block.impl.CraftStainedGlassPane;
import org.bukkit.craftbukkit.block.impl.CraftStairs;
import org.bukkit.craftbukkit.block.impl.CraftStem;
import org.bukkit.craftbukkit.block.impl.CraftStemAttached;
import org.bukkit.craftbukkit.block.impl.CraftStepAbstract;
import org.bukkit.craftbukkit.block.impl.CraftStonecutter;
import org.bukkit.craftbukkit.block.impl.CraftStructure;
import org.bukkit.craftbukkit.block.impl.CraftSweetBerryBush;
import org.bukkit.craftbukkit.block.impl.CraftTNT;
import org.bukkit.craftbukkit.block.impl.CraftTallPlant;
import org.bukkit.craftbukkit.block.impl.CraftTallPlantFlower;
import org.bukkit.craftbukkit.block.impl.CraftTallSeagrass;
import org.bukkit.craftbukkit.block.impl.CraftTarget;
import org.bukkit.craftbukkit.block.impl.CraftTorchWall;
import org.bukkit.craftbukkit.block.impl.CraftTorchflowerCrop;
import org.bukkit.craftbukkit.block.impl.CraftTrapdoor;
import org.bukkit.craftbukkit.block.impl.CraftTrialSpawner;
import org.bukkit.craftbukkit.block.impl.CraftTripwire;
import org.bukkit.craftbukkit.block.impl.CraftTripwireHook;
import org.bukkit.craftbukkit.block.impl.CraftTurtleEgg;
import org.bukkit.craftbukkit.block.impl.CraftTwistingVines;
import org.bukkit.craftbukkit.block.impl.CraftVault;
import org.bukkit.craftbukkit.block.impl.CraftVine;
import org.bukkit.craftbukkit.block.impl.CraftWallHangingSign;
import org.bukkit.craftbukkit.block.impl.CraftWallSign;
import org.bukkit.craftbukkit.block.impl.CraftWaterloggedTransparent;
import org.bukkit.craftbukkit.block.impl.CraftWeatheringCopperBulb;
import org.bukkit.craftbukkit.block.impl.CraftWeatheringCopperDoor;
import org.bukkit.craftbukkit.block.impl.CraftWeatheringCopperGrate;
import org.bukkit.craftbukkit.block.impl.CraftWeatheringCopperSlab;
import org.bukkit.craftbukkit.block.impl.CraftWeatheringCopperStair;
import org.bukkit.craftbukkit.block.impl.CraftWeatheringCopperTrapDoor;
import org.bukkit.craftbukkit.block.impl.CraftWeepingVines;
import org.bukkit.craftbukkit.block.impl.CraftWitherSkull;
import org.bukkit.craftbukkit.block.impl.CraftWitherSkullWall;
import org.bukkit.craftbukkit.inventory.CraftItemStack;
import org.bukkit.craftbukkit.inventory.CraftItemType;
import org.bukkit.craftbukkit.util.CraftLocation;
import org.bukkit.inventory.ItemStack;
import org.jetbrains.annotations.NotNull;

public class CraftBlockData implements BlockData {

    private net.minecraft.world.level.block.state.BlockState state;
    private Map<Property<?>, Comparable<?>> parsedStates;

    protected CraftBlockData() {
        throw new AssertionError("Template Constructor");
    }

    protected CraftBlockData(net.minecraft.world.level.block.state.BlockState state) {
        this.state = state;
    }

    @Override
    public Material getMaterial() {
        return this.state.getBukkitMaterial(); // Paper - optimise getType calls
    }

    public net.minecraft.world.level.block.state.BlockState getState() {
        return this.state;
    }

    /**
     * Get a given BlockStateEnum's value as its Bukkit counterpart.
     *
     * @param nms the NMS state to convert
     * @param bukkit the Bukkit class
     * @param <B> the type
     * @return the matching Bukkit type
     */
    protected <B extends Enum<B>> B get(EnumProperty<?> nms, Class<B> bukkit) {
        return CraftBlockData.toBukkit(this.state.getValue(nms), bukkit);
    }

    /**
     * Convert all values from the given BlockStateEnum to their appropriate
     * Bukkit counterpart.
     *
     * @param nms the NMS state to get values from
     * @param bukkit the bukkit class to convert the values to
     * @param <B> the bukkit class type
     * @return an immutable Set of values in their appropriate Bukkit type
     */
    @SuppressWarnings("unchecked")
    protected <B extends Enum<B>> Set<B> getValues(EnumProperty<?> nms, Class<B> bukkit) {
        ImmutableSet.Builder<B> values = ImmutableSet.builder();

        for (Enum<?> e : nms.getPossibleValues()) {
            values.add(CraftBlockData.toBukkit(e, bukkit));
        }

        return values.build();
    }

    /**
     * Set a given {@link EnumProperty} with the matching enum from Bukkit.
     *
     * @param nms the NMS BlockStateEnum to set
     * @param bukkit the matching Bukkit Enum
     * @param <B> the Bukkit type
     * @param <N> the NMS type
     */
    protected <B extends Enum<B>, N extends Enum<N> & StringRepresentable> void set(EnumProperty<N> nms, Enum<B> bukkit) {
        this.parsedStates = null;
        this.state = this.state.setValue(nms, CraftBlockData.toNMS(bukkit, nms.getValueClass()));
    }

    @Override
    public BlockData merge(BlockData data) {
        CraftBlockData craft = (CraftBlockData) data;
        Preconditions.checkArgument(craft.parsedStates != null, "Data not created via string parsing");
        Preconditions.checkArgument(this.state.getBlock() == craft.state.getBlock(), "States have different types (got %s, expected %s)", data, this);

        CraftBlockData clone = (CraftBlockData) this.clone();
        clone.parsedStates = null;

        for (Property parsed : craft.parsedStates.keySet()) {
            clone.state = clone.state.setValue(parsed, craft.state.getValue(parsed));
        }

        return clone;
    }

    @Override
    public boolean matches(BlockData data) {
        if (data == null) {
            return false;
        }
        if (!(data instanceof CraftBlockData)) {
            return false;
        }

        CraftBlockData craft = (CraftBlockData) data;
        if (this.state.getBlock() != craft.state.getBlock()) {
            return false;
        }

        // Fastpath an exact match
        boolean exactMatch = this.equals(data);

        // If that failed, do a merge and check
        if (!exactMatch && craft.parsedStates != null) {
            return this.merge(data).equals(this);
        }

        return exactMatch;
    }

    private static final Map<Class<? extends Enum<?>>, Enum<?>[]> ENUM_VALUES = new java.util.concurrent.ConcurrentHashMap<>(); // Paper - cache block data strings; make thread safe

    /**
     * Convert an NMS Enum (usually a BlockStateEnum) to its appropriate Bukkit
     * enum from the given class.
     *
     * @throws IllegalStateException if the Enum could not be converted
     */
    @SuppressWarnings("unchecked")
    private static <B extends Enum<B>> B toBukkit(Enum<?> nms, Class<B> bukkit) {
        if (nms instanceof Direction) {
            return (B) CraftBlock.notchToBlockFace((Direction) nms);
        }
        return (B) CraftBlockData.ENUM_VALUES.computeIfAbsent(bukkit, Class::getEnumConstants)[nms.ordinal()];
    }

    /**
     * Convert a given Bukkit enum to its matching NMS enum type.
     *
     * @param bukkit the Bukkit enum to convert
     * @param nms the NMS class
     * @return the matching NMS type
     * @throws IllegalStateException if the Enum could not be converted
     */
    @SuppressWarnings("unchecked")
    public static <N extends Enum<N> & StringRepresentable> N toNMS(Enum<?> bukkit, Class<N> nms) {
        if (bukkit instanceof BlockFace) {
            return (N) CraftBlock.blockFaceToNotch((BlockFace) bukkit);
        }
        return (N) CraftBlockData.ENUM_VALUES.computeIfAbsent(nms, Class::getEnumConstants)[bukkit.ordinal()];
    }

    /**
     * Get the current value of a given state.
     *
     * @param ibs the state to check
     * @param <T> the type
     * @return the current value of the given state
     */
    protected <T extends Comparable<T>> T get(Property<T> ibs) {
        // Straight integer or boolean getter
        return this.state.getValue(ibs);
    }

    /**
     * Set the specified state's value.
     *
     * @param ibs the state to set
     * @param v the new value
     * @param <T> the state's type
     * @param <V> the value's type. Must match the state's type.
     */
    public <T extends Comparable<T>, V extends T> void set(Property<T> ibs, V v) {
        // Straight integer or boolean setter
        this.parsedStates = null;
        this.state = this.state.setValue(ibs, v);
    }

    @Override
    public String getAsString() {
        return this.toString(this.state.getValues());
    }

    @Override
    public String getAsString(boolean hideUnspecified) {
        return (hideUnspecified && this.parsedStates != null) ? this.toString(this.parsedStates) : this.getAsString();
    }

    @Override
    public BlockData clone() {
        try {
            return (BlockData) super.clone();
        } catch (CloneNotSupportedException ex) {
            throw new AssertionError("Clone not supported", ex);
        }
    }

    @Override
    public String toString() {
        return "CraftBlockData{" + this.getAsString() + "}";
    }

    // Mimicked from BlockDataAbstract#toString()
    public String toString(Map<Property<?>, Comparable<?>> states) {
        StringBuilder stateString = new StringBuilder(BuiltInRegistries.BLOCK.getKey(this.state.getBlock()).toString());

        if (!states.isEmpty()) {
            stateString.append('[');
            stateString.append(states.entrySet().stream().map(StateHolder.PROPERTY_ENTRY_TO_STRING_FUNCTION).collect(Collectors.joining(",")));
            stateString.append(']');
        }

        return stateString.toString();
    }

    public Map<String, String> toStates() {
        Map<String, String> compound = new HashMap<>();

        for (Map.Entry<Property<?>, Comparable<?>> entry : this.state.getValues().entrySet()) {
            Property iblockstate = (Property) entry.getKey();

            compound.put(iblockstate.getName(), iblockstate.getName(entry.getValue()));
        }

        return compound;
    }

    @Override
    public boolean equals(Object obj) {
        return obj instanceof CraftBlockData && this.state.equals(((CraftBlockData) obj).state);
    }

    @Override
    public int hashCode() {
        return this.state.hashCode();
    }

    protected static BooleanProperty getBoolean(String name) {
        throw new AssertionError("Template Method");
    }

    protected static BooleanProperty getBoolean(String name, boolean optional) {
        throw new AssertionError("Template Method");
    }

    protected static EnumProperty<?> getEnum(String name) {
        throw new AssertionError("Template Method");
    }

    protected static IntegerProperty getInteger(String name) {
        throw new AssertionError("Template Method");
    }

    protected static BooleanProperty getBoolean(Class<? extends Block> block, String name) {
        return (BooleanProperty) CraftBlockData.getState(block, name, false);
    }

    protected static BooleanProperty getBoolean(Class<? extends Block> block, String name, boolean optional) {
        return (BooleanProperty) CraftBlockData.getState(block, name, optional);
    }

    protected static EnumProperty<?> getEnum(Class<? extends Block> block, String name) {
        return (EnumProperty<?>) CraftBlockData.getState(block, name, false);
    }

    protected static IntegerProperty getInteger(Class<? extends Block> block, String name) {
        return (IntegerProperty) CraftBlockData.getState(block, name, false);
    }

    /**
     * Get a specified {@link Property} from a given block's class with a
     * given name
     *
     * @param block the class to retrieve the state from
     * @param name the name of the state to retrieve
     * @param optional if the state can be null
     * @return the specified state or null
     * @throws IllegalStateException if the state is null and {@code optional}
     * is false.
     */
    private static Property<?> getState(Class<? extends Block> block, String name, boolean optional) {
        Property<?> state = null;

        for (Block instance : BuiltInRegistries.BLOCK) {
            if (instance.getClass() == block) {
                if (state == null) {
                    state = instance.getStateDefinition().getProperty(name);
                } else {
                    Property<?> newState = instance.getStateDefinition().getProperty(name);

                    Preconditions.checkState(state == newState, "State mistmatch %s,%s", state, newState);
                }
            }
        }

        Preconditions.checkState(optional || state != null, "Null state for %s,%s", block, name);

        return state;
    }

    /**
     * Get the minimum value allowed by the BlockStateInteger.
     *
     * @param state the state to check
     * @return the minimum value allowed
     */
    protected static int getMin(IntegerProperty state) {
        return state.min;
    }

    /**
     * Get the maximum value allowed by the BlockStateInteger.
     *
     * @param state the state to check
     * @return the maximum value allowed
     */
    protected static int getMax(IntegerProperty state) {
        return state.max;
    }

    //
    private static final Map<Class<? extends Block>, Function<net.minecraft.world.level.block.state.BlockState, CraftBlockData>> MAP = new HashMap<>();

    static {
        //<editor-fold desc="CraftBlockData Registration" defaultstate="collapsed">
        register(net.minecraft.world.level.block.AmethystClusterBlock.class, CraftAmethystCluster::new);
        register(net.minecraft.world.level.block.BigDripleafBlock.class, CraftBigDripleaf::new);
        register(net.minecraft.world.level.block.BigDripleafStemBlock.class, CraftBigDripleafStem::new);
        register(net.minecraft.world.level.block.AnvilBlock.class, CraftAnvil::new);
        register(net.minecraft.world.level.block.BambooStalkBlock.class, CraftBamboo::new);
        register(net.minecraft.world.level.block.BannerBlock.class, CraftBanner::new);
        register(net.minecraft.world.level.block.WallBannerBlock.class, CraftBannerWall::new);
        register(net.minecraft.world.level.block.BarrelBlock.class, CraftBarrel::new);
        register(net.minecraft.world.level.block.BarrierBlock.class, CraftBarrier::new);
        register(net.minecraft.world.level.block.BedBlock.class, CraftBed::new);
        register(net.minecraft.world.level.block.BeehiveBlock.class, CraftBeehive::new);
        register(net.minecraft.world.level.block.BeetrootBlock.class, CraftBeetroot::new);
        register(net.minecraft.world.level.block.BellBlock.class, CraftBell::new);
        register(net.minecraft.world.level.block.BlastFurnaceBlock.class, CraftBlastFurnace::new);
        register(net.minecraft.world.level.block.BrewingStandBlock.class, CraftBrewingStand::new);
        register(net.minecraft.world.level.block.BubbleColumnBlock.class, CraftBubbleColumn::new);
        register(net.minecraft.world.level.block.ButtonBlock.class, CraftButtonAbstract::new);
        register(net.minecraft.world.level.block.CactusBlock.class, CraftCactus::new);
        register(net.minecraft.world.level.block.CakeBlock.class, CraftCake::new);
        register(net.minecraft.world.level.block.CampfireBlock.class, CraftCampfire::new);
        register(net.minecraft.world.level.block.CarrotBlock.class, CraftCarrots::new);
        register(net.minecraft.world.level.block.ChainBlock.class, CraftChain::new);
        register(net.minecraft.world.level.block.ChestBlock.class, CraftChest::new);
        register(net.minecraft.world.level.block.TrappedChestBlock.class, CraftChestTrapped::new);
        register(net.minecraft.world.level.block.ChorusFlowerBlock.class, CraftChorusFlower::new);
        register(net.minecraft.world.level.block.ChorusPlantBlock.class, CraftChorusFruit::new);
        register(net.minecraft.world.level.block.WallBlock.class, CraftCobbleWall::new);
        register(net.minecraft.world.level.block.CocoaBlock.class, CraftCocoa::new);
        register(net.minecraft.world.level.block.CommandBlock.class, CraftCommand::new);
        register(net.minecraft.world.level.block.ComposterBlock.class, CraftComposter::new);
        register(net.minecraft.world.level.block.ConduitBlock.class, CraftConduit::new);
        register(net.minecraft.world.level.block.BaseCoralPlantBlock.class, CraftCoralDead::new);
        register(net.minecraft.world.level.block.CoralFanBlock.class, CraftCoralFan::new);
        register(net.minecraft.world.level.block.BaseCoralFanBlock.class, CraftCoralFanAbstract::new);
        register(net.minecraft.world.level.block.CoralWallFanBlock.class, CraftCoralFanWall::new);
        register(net.minecraft.world.level.block.BaseCoralWallFanBlock.class, CraftCoralFanWallAbstract::new);
        register(net.minecraft.world.level.block.CoralPlantBlock.class, CraftCoralPlant::new);
        register(net.minecraft.world.level.block.CropBlock.class, CraftCrops::new);
        register(net.minecraft.world.level.block.DaylightDetectorBlock.class, CraftDaylightDetector::new);
        register(net.minecraft.world.level.block.SnowyDirtBlock.class, CraftDirtSnow::new);
        register(net.minecraft.world.level.block.DispenserBlock.class, CraftDispenser::new);
        register(net.minecraft.world.level.block.DoorBlock.class, CraftDoor::new);
        register(net.minecraft.world.level.block.DropperBlock.class, CraftDropper::new);
        register(net.minecraft.world.level.block.EndRodBlock.class, CraftEndRod::new);
        register(net.minecraft.world.level.block.EnderChestBlock.class, CraftEnderChest::new);
        register(net.minecraft.world.level.block.EndPortalFrameBlock.class, CraftEnderPortalFrame::new);
        register(net.minecraft.world.level.block.FenceBlock.class, CraftFence::new);
        register(net.minecraft.world.level.block.FenceGateBlock.class, CraftFenceGate::new);
        register(net.minecraft.world.level.block.FireBlock.class, CraftFire::new);
        register(net.minecraft.world.level.block.StandingSignBlock.class, CraftFloorSign::new);
        register(net.minecraft.world.level.block.LiquidBlock.class, CraftFluids::new);
        register(net.minecraft.world.level.block.FurnaceBlock.class, CraftFurnaceFurace::new);
        register(net.minecraft.world.level.block.GlazedTerracottaBlock.class, CraftGlazedTerracotta::new);
        register(net.minecraft.world.level.block.GrassBlock.class, CraftGrass::new);
        register(net.minecraft.world.level.block.GrindstoneBlock.class, CraftGrindstone::new);
        register(net.minecraft.world.level.block.HayBlock.class, CraftHay::new);
        register(net.minecraft.world.level.block.HopperBlock.class, CraftHopper::new);
        register(net.minecraft.world.level.block.HugeMushroomBlock.class, CraftHugeMushroom::new);
        register(net.minecraft.world.level.block.FrostedIceBlock.class, CraftIceFrost::new);
        register(net.minecraft.world.level.block.IronBarsBlock.class, CraftIronBars::new);
        register(net.minecraft.world.level.block.JigsawBlock.class, CraftJigsaw::new);
        register(net.minecraft.world.level.block.JukeboxBlock.class, CraftJukeBox::new);
        register(net.minecraft.world.level.block.KelpBlock.class, CraftKelp::new);
        register(net.minecraft.world.level.block.LadderBlock.class, CraftLadder::new);
        register(net.minecraft.world.level.block.LanternBlock.class, CraftLantern::new);
        register(net.minecraft.world.level.block.LeavesBlock.class, CraftLeaves::new);
        register(net.minecraft.world.level.block.LecternBlock.class, CraftLectern::new);
        register(net.minecraft.world.level.block.LeverBlock.class, CraftLever::new);
        register(net.minecraft.world.level.block.LoomBlock.class, CraftLoom::new);
        register(net.minecraft.world.level.block.DetectorRailBlock.class, CraftMinecartDetector::new);
        register(net.minecraft.world.level.block.RailBlock.class, CraftMinecartTrack::new);
        register(net.minecraft.world.level.block.MyceliumBlock.class, CraftMycel::new);
        register(net.minecraft.world.level.block.NetherWartBlock.class, CraftNetherWart::new);
        register(net.minecraft.world.level.block.NoteBlock.class, CraftNote::new);
        register(net.minecraft.world.level.block.ObserverBlock.class, CraftObserver::new);
        register(net.minecraft.world.level.block.NetherPortalBlock.class, CraftPortal::new);
        register(net.minecraft.world.level.block.PotatoBlock.class, CraftPotatoes::new);
        register(net.minecraft.world.level.block.PoweredRailBlock.class, CraftPoweredRail::new);
        register(net.minecraft.world.level.block.PressurePlateBlock.class, CraftPressurePlateBinary::new);
        register(net.minecraft.world.level.block.WeightedPressurePlateBlock.class, CraftPressurePlateWeighted::new);
        register(net.minecraft.world.level.block.CarvedPumpkinBlock.class, CraftPumpkinCarved::new);
        register(net.minecraft.world.level.block.ComparatorBlock.class, CraftRedstoneComparator::new);
        register(net.minecraft.world.level.block.RedstoneLampBlock.class, CraftRedstoneLamp::new);
        register(net.minecraft.world.level.block.RedStoneOreBlock.class, CraftRedstoneOre::new);
        register(net.minecraft.world.level.block.RedstoneTorchBlock.class, CraftRedstoneTorch::new);
        register(net.minecraft.world.level.block.RedstoneWallTorchBlock.class, CraftRedstoneTorchWall::new);
        register(net.minecraft.world.level.block.RedStoneWireBlock.class, CraftRedstoneWire::new);
        register(net.minecraft.world.level.block.SugarCaneBlock.class, CraftReed::new);
        register(net.minecraft.world.level.block.RepeaterBlock.class, CraftRepeater::new);
        register(net.minecraft.world.level.block.RespawnAnchorBlock.class, CraftRespawnAnchor::new);
        register(net.minecraft.world.level.block.RotatedPillarBlock.class, CraftRotatable::new);
        register(net.minecraft.world.level.block.SaplingBlock.class, CraftSapling::new);
        register(net.minecraft.world.level.block.ScaffoldingBlock.class, CraftScaffolding::new);
        register(net.minecraft.world.level.block.SeaPickleBlock.class, CraftSeaPickle::new);
        register(net.minecraft.world.level.block.ShulkerBoxBlock.class, CraftShulkerBox::new);
        register(net.minecraft.world.level.block.SkullBlock.class, CraftSkull::new);
        register(net.minecraft.world.level.block.PlayerHeadBlock.class, CraftSkullPlayer::new);
        register(net.minecraft.world.level.block.PlayerWallHeadBlock.class, CraftSkullPlayerWall::new);
        register(net.minecraft.world.level.block.WallSkullBlock.class, CraftSkullWall::new);
        register(net.minecraft.world.level.block.SmokerBlock.class, CraftSmoker::new);
        register(net.minecraft.world.level.block.SnowLayerBlock.class, CraftSnow::new);
        register(net.minecraft.world.level.block.FarmBlock.class, CraftSoil::new);
        register(net.minecraft.world.level.block.StainedGlassPaneBlock.class, CraftStainedGlassPane::new);
        register(net.minecraft.world.level.block.StairBlock.class, CraftStairs::new);
        register(net.minecraft.world.level.block.StemBlock.class, CraftStem::new);
        register(net.minecraft.world.level.block.AttachedStemBlock.class, CraftStemAttached::new);
        register(net.minecraft.world.level.block.SlabBlock.class, CraftStepAbstract::new);
        register(net.minecraft.world.level.block.StonecutterBlock.class, CraftStonecutter::new);
        register(net.minecraft.world.level.block.StructureBlock.class, CraftStructure::new);
        register(net.minecraft.world.level.block.SweetBerryBushBlock.class, CraftSweetBerryBush::new);
        register(net.minecraft.world.level.block.TntBlock.class, CraftTNT::new);
        register(net.minecraft.world.level.block.DoublePlantBlock.class, CraftTallPlant::new);
        register(net.minecraft.world.level.block.TallFlowerBlock.class, CraftTallPlantFlower::new);
        register(net.minecraft.world.level.block.TargetBlock.class, CraftTarget::new);
        register(net.minecraft.world.level.block.WallTorchBlock.class, CraftTorchWall::new);
        register(net.minecraft.world.level.block.TrapDoorBlock.class, CraftTrapdoor::new);
        register(net.minecraft.world.level.block.TripWireBlock.class, CraftTripwire::new);
        register(net.minecraft.world.level.block.TripWireHookBlock.class, CraftTripwireHook::new);
        register(net.minecraft.world.level.block.TurtleEggBlock.class, CraftTurtleEgg::new);
        register(net.minecraft.world.level.block.TwistingVinesBlock.class, CraftTwistingVines::new);
        register(net.minecraft.world.level.block.VineBlock.class, CraftVine::new);
        register(net.minecraft.world.level.block.WallSignBlock.class, CraftWallSign::new);
        register(net.minecraft.world.level.block.WeepingVinesBlock.class, CraftWeepingVines::new);
        register(net.minecraft.world.level.block.WitherSkullBlock.class, CraftWitherSkull::new);
        register(net.minecraft.world.level.block.WitherWallSkullBlock.class, CraftWitherSkullWall::new);
        register(net.minecraft.world.level.block.BrushableBlock.class, CraftBrushable::new);
        register(net.minecraft.world.level.block.CalibratedSculkSensorBlock.class, CraftCalibratedSculkSensor::new);
        register(net.minecraft.world.level.block.CandleBlock.class, CraftCandle::new);
        register(net.minecraft.world.level.block.CandleCakeBlock.class, CraftCandleCake::new);
        register(net.minecraft.world.level.block.CaveVinesBlock.class, CraftCaveVines::new);
        register(net.minecraft.world.level.block.CaveVinesPlantBlock.class, CraftCaveVinesPlant::new);
        register(net.minecraft.world.level.block.CeilingHangingSignBlock.class, CraftCeilingHangingSign::new);
        register(net.minecraft.world.level.block.CherryLeavesBlock.class, CraftCherryLeaves::new);
        register(net.minecraft.world.level.block.ChiseledBookShelfBlock.class, CraftChiseledBookShelf::new);
        register(net.minecraft.world.level.block.CopperBulbBlock.class, CraftCopperBulb::new);
        register(net.minecraft.world.level.block.CrafterBlock.class, CraftCrafter::new);
        register(net.minecraft.world.level.block.DecoratedPotBlock.class, CraftDecoratedPot::new);
        register(net.minecraft.world.level.block.EquipableCarvedPumpkinBlock.class, CraftEquipableCarvedPumpkin::new);
        register(net.minecraft.world.level.block.GlowLichenBlock.class, CraftGlowLichen::new);
        register(net.minecraft.world.level.block.HangingRootsBlock.class, CraftHangingRoots::new);
        register(net.minecraft.world.level.block.HeavyCoreBlock.class, CraftHeavyCore::new);
        register(net.minecraft.world.level.block.InfestedRotatedPillarBlock.class, CraftInfestedRotatedPillar::new);
        register(net.minecraft.world.level.block.LayeredCauldronBlock.class, CraftLayeredCauldron::new);
        register(net.minecraft.world.level.block.LightBlock.class, CraftLight::new);
        register(net.minecraft.world.level.block.LightningRodBlock.class, CraftLightningRod::new);
        register(net.minecraft.world.level.block.MangroveLeavesBlock.class, CraftMangroveLeaves::new);
        register(net.minecraft.world.level.block.MangrovePropaguleBlock.class, CraftMangrovePropagule::new);
        register(net.minecraft.world.level.block.MangroveRootsBlock.class, CraftMangroveRoots::new);
        register(net.minecraft.world.level.block.PiglinWallSkullBlock.class, CraftPiglinWallSkull::new);
        register(net.minecraft.world.level.block.PinkPetalsBlock.class, CraftPinkPetals::new);
        register(net.minecraft.world.level.block.PitcherCropBlock.class, CraftPitcherCrop::new);
        register(net.minecraft.world.level.block.PointedDripstoneBlock.class, CraftPointedDripstone::new);
        register(net.minecraft.world.level.block.SculkCatalystBlock.class, CraftSculkCatalyst::new);
        register(net.minecraft.world.level.block.SculkSensorBlock.class, CraftSculkSensor::new);
        register(net.minecraft.world.level.block.SculkShriekerBlock.class, CraftSculkShrieker::new);
        register(net.minecraft.world.level.block.SculkVeinBlock.class, CraftSculkVein::new);
        register(net.minecraft.world.level.block.SmallDripleafBlock.class, CraftSmallDripleaf::new);
        register(net.minecraft.world.level.block.SnifferEggBlock.class, CraftSnifferEgg::new);
        register(net.minecraft.world.level.block.TallSeagrassBlock.class, CraftTallSeagrass::new);
        register(net.minecraft.world.level.block.TorchflowerCropBlock.class, CraftTorchflowerCrop::new);
        register(net.minecraft.world.level.block.TrialSpawnerBlock.class, CraftTrialSpawner::new);
        register(net.minecraft.world.level.block.VaultBlock.class, CraftVault::new);
        register(net.minecraft.world.level.block.WallHangingSignBlock.class, CraftWallHangingSign::new);
        register(net.minecraft.world.level.block.WaterloggedTransparentBlock.class, CraftWaterloggedTransparent::new);
        register(net.minecraft.world.level.block.WeatheringCopperBulbBlock.class, CraftWeatheringCopperBulb::new);
        register(net.minecraft.world.level.block.WeatheringCopperDoorBlock.class, CraftWeatheringCopperDoor::new);
        register(net.minecraft.world.level.block.WeatheringCopperGrateBlock.class, CraftWeatheringCopperGrate::new);
        register(net.minecraft.world.level.block.WeatheringCopperSlabBlock.class, CraftWeatheringCopperSlab::new);
        register(net.minecraft.world.level.block.WeatheringCopperStairBlock.class, CraftWeatheringCopperStair::new);
        register(net.minecraft.world.level.block.WeatheringCopperTrapDoorBlock.class, CraftWeatheringCopperTrapDoor::new);
        register(net.minecraft.world.level.block.piston.PistonBaseBlock.class, CraftPiston::new);
        register(net.minecraft.world.level.block.piston.PistonHeadBlock.class, CraftPistonExtension::new);
        register(net.minecraft.world.level.block.piston.MovingPistonBlock.class, CraftPistonMoving::new);
        //</editor-fold>
    }

    private static void register(Class<? extends Block> nms, Function<net.minecraft.world.level.block.state.BlockState, CraftBlockData> bukkit) {
        Preconditions.checkState(CraftBlockData.MAP.put(nms, bukkit) == null, "Duplicate mapping %s->%s", nms, bukkit);
    }

    // Paper start - cache block data strings
    private static Map<String, CraftBlockData> stringDataCache = new java.util.concurrent.ConcurrentHashMap<>();

    static {
        // cache all of the default states at startup, will not cache ones with the custom states inside of the
        // brackets in a different order, though
        reloadCache();
    }

    public static void reloadCache() {
        stringDataCache.clear();
        Block.BLOCK_STATE_REGISTRY.forEach(blockData -> stringDataCache.put(blockData.toString(), blockData.createCraftBlockData()));
    }
    // Paper end - cache block data strings

    public static CraftBlockData newData(BlockType blockType, String data) {

        // Paper start - cache block data strings
        if (blockType != null) {
            Block block = CraftBlockType.bukkitToMinecraftNew(blockType);
            if (block != null) {
                net.minecraft.resources.ResourceLocation key = BuiltInRegistries.BLOCK.getKey(block);
                data = data == null ? key.toString() : key + data;
            }
        }

        CraftBlockData cached = stringDataCache.computeIfAbsent(data, s -> createNewData(null, s));
        return (CraftBlockData) cached.clone();
    }

    private static CraftBlockData createNewData(BlockType blockType, String data) {
        // Paper end - cache block data strings
        net.minecraft.world.level.block.state.BlockState blockData;
        Block block = blockType == null ? null : ((CraftBlockType<?>) blockType).getHandle();
        Map<Property<?>, Comparable<?>> parsed = null;

        // Data provided, use it
        if (data != null) {
            try {
                // Material provided, force that material in
                if (block != null) {
                    data = BuiltInRegistries.BLOCK.getKey(block) + data;
                }

                StringReader reader = new StringReader(data);
                BlockStateParser.BlockResult arg = BlockStateParser.parseForBlock(BuiltInRegistries.BLOCK.asLookup(), reader, false);
                Preconditions.checkArgument(!reader.canRead(), "Spurious trailing data: " + data);

                blockData = arg.blockState();
                parsed = arg.properties();
            } catch (CommandSyntaxException ex) {
                throw new IllegalArgumentException("Could not parse data: " + data, ex);
            }
        } else {
            blockData = block.defaultBlockState();
        }

        CraftBlockData craft = CraftBlockData.fromData(blockData);
        craft.parsedStates = parsed;
        return craft;
    }

    // Paper start - optimize creating BlockData to not need a map lookup
    static {
        // Initialize cached data for all IBlockData instances after registration
        Block.BLOCK_STATE_REGISTRY.iterator().forEachRemaining(net.minecraft.world.level.block.state.BlockState::createCraftBlockData);
    }
    public static CraftBlockData fromData(net.minecraft.world.level.block.state.BlockState data) {
        return data.createCraftBlockData();
    }

    public static CraftBlockData createData(net.minecraft.world.level.block.state.BlockState data) {
        // Paper end
        return CraftBlockData.MAP.getOrDefault(data.getBlock().getClass(), CraftBlockData::new).apply(data);
    }

    @Override
    public SoundGroup getSoundGroup() {
        return CraftSoundGroup.getSoundGroup(this.state.getSoundType());
    }

    @Override
    public int getLightEmission() {
        return this.state.getLightEmission();
    }

    @Override
    public boolean isOccluding() {
        return this.state.canOcclude();
    }

    @Override
    public boolean requiresCorrectToolForDrops() {
        return this.state.requiresCorrectToolForDrops();
    }

    @Override
    public boolean isPreferredTool(ItemStack tool) {
        Preconditions.checkArgument(tool != null, "tool must not be null");

        net.minecraft.world.item.ItemStack nms = CraftItemStack.asNMSCopy(tool);
        return CraftBlockData.isPreferredTool(this.state, nms);
    }

    public static boolean isPreferredTool(net.minecraft.world.level.block.state.BlockState iblockdata, net.minecraft.world.item.ItemStack nmsItem) {
        return !iblockdata.requiresCorrectToolForDrops() || nmsItem.isCorrectToolForDrops(iblockdata);
    }

    @Override
    public PistonMoveReaction getPistonMoveReaction() {
        return PistonMoveReaction.getById(this.state.getPistonPushReaction().ordinal());
    }

    @Override
    public boolean isSupported(org.bukkit.block.Block block) {
        Preconditions.checkArgument(block != null, "block must not be null");

        CraftBlock craftBlock = (CraftBlock) block;
        return this.state.canSurvive(craftBlock.getCraftWorld().getHandle(), craftBlock.getPosition());
    }

    @Override
    public boolean isSupported(Location location) {
        Preconditions.checkArgument(location != null, "location must not be null");

        CraftWorld world = (CraftWorld) location.getWorld();
        Preconditions.checkArgument(world != null, "location must not have a null world");

        BlockPos position = CraftLocation.toBlockPosition(location);
        return this.state.canSurvive(world.getHandle(), position);
    }

    @Override
    public boolean isFaceSturdy(BlockFace face, BlockSupport support) {
        Preconditions.checkArgument(face != null, "face must not be null");
        Preconditions.checkArgument(support != null, "support must not be null");

        return this.state.isFaceSturdy(EmptyBlockGetter.INSTANCE, BlockPos.ZERO, CraftBlock.blockFaceToNotch(face), CraftBlockSupport.toNMS(support));
    }

    // Paper start
    @Override
    public org.bukkit.util.VoxelShape getCollisionShape(Location location) {
        Preconditions.checkArgument(location != null, "location must not be null");

        CraftWorld world = (CraftWorld) location.getWorld();
        Preconditions.checkArgument(world != null, "location must not have a null world");

        BlockPos position = CraftLocation.toBlockPosition(location);
        net.minecraft.world.phys.shapes.VoxelShape shape = this.state.getCollisionShape(world.getHandle(), position);
        return new org.bukkit.craftbukkit.util.CraftVoxelShape(shape);
    }
    // Paper end

    @Override
    public Color getMapColor() {
        return Color.fromRGB(this.state.getMapColor(null, null).col);
    }

    @Override
    public Material getPlacementMaterial() {
        return CraftItemType.minecraftToBukkit(this.state.getBlock().asItem());
    }

    @Override
    public void rotate(StructureRotation rotation) {
        this.state = this.state.rotate(Rotation.valueOf(rotation.name()));
    }

    @Override
    public void mirror(Mirror mirror) {
        this.state = this.state.mirror(net.minecraft.world.level.block.Mirror.valueOf(mirror.name()));
    }

    @Override
    public void copyTo(BlockData blockData) {
        CraftBlockData other = (CraftBlockData) blockData;
        net.minecraft.world.level.block.state.BlockState nms = other.state;
        for (Property<?> property : this.state.getBlock().getStateDefinition().getProperties()) {
            if (nms.hasProperty(property)) {
                nms = this.copyProperty(this.state, nms, property);
            }
        }

        other.state = nms;
    }

    private <T extends Comparable<T>> net.minecraft.world.level.block.state.BlockState copyProperty(net.minecraft.world.level.block.state.BlockState source, net.minecraft.world.level.block.state.BlockState target, Property<T> property) {
        return target.setValue(property, source.getValue(property));
    }

    @NotNull
    @Override
    public BlockState createBlockState() {
        return CraftBlockStates.getBlockState(this.state, null);
    }

    // Paper start - destroy speed API
    @Override
    public float getDestroySpeed(final ItemStack itemStack, final boolean considerEnchants) {
        net.minecraft.world.item.ItemStack nmsItemStack = CraftItemStack.unwrap(itemStack);
        float speed = nmsItemStack.getDestroySpeed(this.state);
        if (speed > 1.0F && considerEnchants) {
            final net.minecraft.core.Holder<net.minecraft.world.entity.ai.attributes.Attribute> attribute = net.minecraft.world.entity.ai.attributes.Attributes.MINING_EFFICIENCY;
            // Logic sourced from AttributeInstance#calculateValue
            final double initialBaseValue = attribute.value().getDefaultValue();
            final org.apache.commons.lang3.mutable.MutableDouble modifiedBaseValue = new org.apache.commons.lang3.mutable.MutableDouble(initialBaseValue);
            final org.apache.commons.lang3.mutable.MutableDouble baseValMul = new org.apache.commons.lang3.mutable.MutableDouble(1);
            final org.apache.commons.lang3.mutable.MutableDouble totalValMul = new org.apache.commons.lang3.mutable.MutableDouble(1);

            net.minecraft.world.item.enchantment.EnchantmentHelper.forEachModifier(
                    nmsItemStack, net.minecraft.world.entity.EquipmentSlot.MAINHAND, (attributeHolder, attributeModifier) -> {
                        switch (attributeModifier.operation()) {
                            case ADD_VALUE -> modifiedBaseValue.add(attributeModifier.amount());
                            case ADD_MULTIPLIED_BASE -> baseValMul.add(attributeModifier.amount());
                            case ADD_MULTIPLIED_TOTAL -> totalValMul.setValue(totalValMul.doubleValue() * (1D + attributeModifier.amount()));
                        }
                    }
            );

            final double actualModifier = modifiedBaseValue.doubleValue() * baseValMul.doubleValue() * totalValMul.doubleValue();

            speed += (float) attribute.value().sanitizeValue(actualModifier);
        }
        return speed;
    }
    // Paper end - destroy speed API

    // Paper start - Block tick API
    @Override
    public boolean isRandomlyTicked() {
        return this.state.isRandomlyTicking();
    }
    // Paper end - Block tick API
}
