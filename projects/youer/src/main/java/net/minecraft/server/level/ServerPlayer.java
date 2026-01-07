package net.minecraft.server.level;

import com.mohistmc.youer.api.ColorAPI;
import com.mohistmc.youer.api.event.ClientboundOpenScreenEvent;
import com.mohistmc.youer.bukkit.inventory.InventoryOwner;
import com.mohistmc.youer.feature.KeepInventory;
import com.google.common.net.InetAddresses;
import com.mojang.authlib.GameProfile;
import com.mojang.datafixers.util.Either;
import com.mojang.logging.LogUtils;
import com.mojang.serialization.Dynamic;
import io.papermc.paper.adventure.PaperAdventure;
import com.destroystokyo.paper.event.player.PlayerSetSpawnEvent;
import com.destroystokyo.paper.event.player.PlayerTeleportEndGatewayEvent;
import java.net.InetSocketAddress;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.OptionalInt;
import java.util.Set;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.Consumer;
import java.util.function.IntConsumer;
import java.util.function.Supplier;
import java.util.stream.Collectors;
import javax.annotation.Nullable;
import net.minecraft.ChatFormatting;
import net.minecraft.CrashReport;
import net.minecraft.CrashReportCategory;
import net.minecraft.ReportedException;
import net.minecraft.Util;
import net.minecraft.advancements.CriteriaTriggers;
import net.minecraft.commands.arguments.EntityAnchorArgument;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.GlobalPos;
import net.minecraft.core.NonNullList;
import net.minecraft.core.SectionPos;
import net.minecraft.core.particles.BlockParticleOption;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.NbtOps;
import net.minecraft.nbt.Tag;
import net.minecraft.network.PacketSendListener;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.chat.ChatType;
import net.minecraft.network.chat.CommonComponents;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.HoverEvent;
import net.minecraft.network.chat.OutgoingChatMessage;
import net.minecraft.network.chat.RemoteChatSession;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.game.ClientboundAnimatePacket;
import net.minecraft.network.protocol.game.ClientboundBlockEntityDataPacket;
import net.minecraft.network.protocol.game.ClientboundBlockUpdatePacket;
import net.minecraft.network.protocol.game.ClientboundChangeDifficultyPacket;
import net.minecraft.network.protocol.game.ClientboundContainerClosePacket;
import net.minecraft.network.protocol.game.ClientboundContainerSetContentPacket;
import net.minecraft.network.protocol.game.ClientboundContainerSetDataPacket;
import net.minecraft.network.protocol.game.ClientboundContainerSetSlotPacket;
import net.minecraft.network.protocol.game.ClientboundEntityEventPacket;
import net.minecraft.network.protocol.game.ClientboundGameEventPacket;
import net.minecraft.network.protocol.game.ClientboundHorseScreenOpenPacket;
import net.minecraft.network.protocol.game.ClientboundHurtAnimationPacket;
import net.minecraft.network.protocol.game.ClientboundMerchantOffersPacket;
import net.minecraft.network.protocol.game.ClientboundOpenBookPacket;
import net.minecraft.network.protocol.game.ClientboundOpenScreenPacket;
import net.minecraft.network.protocol.game.ClientboundOpenSignEditorPacket;
import net.minecraft.network.protocol.game.ClientboundPlayerAbilitiesPacket;
import net.minecraft.network.protocol.game.ClientboundPlayerCombatEndPacket;
import net.minecraft.network.protocol.game.ClientboundPlayerCombatEnterPacket;
import net.minecraft.network.protocol.game.ClientboundPlayerCombatKillPacket;
import net.minecraft.network.protocol.game.ClientboundPlayerInfoUpdatePacket;
import net.minecraft.network.protocol.game.ClientboundPlayerLookAtPacket;
import net.minecraft.network.protocol.game.ClientboundRemoveMobEffectPacket;
import net.minecraft.network.protocol.game.ClientboundRespawnPacket;
import net.minecraft.network.protocol.game.ClientboundServerDataPacket;
import net.minecraft.network.protocol.game.ClientboundSetCameraPacket;
import net.minecraft.network.protocol.game.ClientboundSetExperiencePacket;
import net.minecraft.network.protocol.game.ClientboundSetHealthPacket;
import net.minecraft.network.protocol.game.ClientboundSoundPacket;
import net.minecraft.network.protocol.game.ClientboundSystemChatPacket;
import net.minecraft.network.protocol.game.ClientboundTabListPacket;
import net.minecraft.network.protocol.game.ClientboundUpdateMobEffectPacket;
import net.minecraft.network.protocol.game.CommonPlayerSpawnInfo;
import net.minecraft.network.protocol.status.ServerStatus;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.PlayerAdvancements;
import net.minecraft.server.network.ServerGamePacketListenerImpl;
import net.minecraft.server.network.TextFilter;
import net.minecraft.server.players.PlayerList;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundSource;
import net.minecraft.stats.ServerRecipeBook;
import net.minecraft.stats.ServerStatsCounter;
import net.minecraft.stats.Stat;
import net.minecraft.stats.Stats;
import net.minecraft.tags.DamageTypeTags;
import net.minecraft.tags.FluidTags;
import net.minecraft.util.Mth;
import net.minecraft.util.RandomSource;
import net.minecraft.util.Unit;
import net.minecraft.world.Container;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.MenuProvider;
import net.minecraft.world.damagesource.CombatTracker;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntitySelector;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.HumanoidArm;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.NeutralMob;
import net.minecraft.world.entity.Pose;
import net.minecraft.world.entity.RelativeMovement;
import net.minecraft.world.entity.ai.attributes.AttributeInstance;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.animal.Pig;
import net.minecraft.world.entity.animal.horse.AbstractHorse;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.monster.Monster;
import net.minecraft.world.entity.monster.Strider;
import net.minecraft.world.entity.monster.warden.WardenSpawnTracker;
import net.minecraft.world.entity.player.ChatVisiblity;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.projectile.AbstractArrow;
import net.minecraft.world.entity.vehicle.AbstractMinecart;
import net.minecraft.world.entity.vehicle.Boat;
import net.minecraft.world.food.FoodData;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.ContainerListener;
import net.minecraft.world.inventory.ContainerSynchronizer;
import net.minecraft.world.inventory.HorseInventoryMenu;
import net.minecraft.world.inventory.InventoryMenu;
import net.minecraft.world.inventory.ResultSlot;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ComplexItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemCooldowns;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.ServerItemCooldowns;
import net.minecraft.world.item.WrittenBookItem;
import net.minecraft.world.item.crafting.RecipeHolder;
import net.minecraft.world.item.enchantment.EnchantmentEffectComponents;
import net.minecraft.world.item.enchantment.EnchantmentHelper;
import net.minecraft.world.item.trading.MerchantOffers;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.GameRules;
import net.minecraft.world.level.GameType;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.biome.BiomeManager;
import net.minecraft.world.level.block.BedBlock;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.ChestBlock;
import net.minecraft.world.level.block.HorizontalDirectionalBlock;
import net.minecraft.world.level.block.RespawnAnchorBlock;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.CommandBlockEntity;
import net.minecraft.world.level.block.entity.SignBlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.gameevent.GameEvent;
import net.minecraft.world.level.portal.DimensionTransition;
import net.minecraft.world.level.storage.LevelData;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import net.minecraft.world.scores.PlayerTeam;
import net.minecraft.world.scores.ScoreAccess;
import net.minecraft.world.scores.ScoreHolder;
import net.minecraft.world.scores.Scoreboard;
import net.minecraft.world.scores.Team;
import net.minecraft.world.scores.criteria.ObjectiveCriteria;
import net.neoforged.neoforge.common.CommonHooks;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.common.util.FriendlyByteBufUtil;
import net.neoforged.neoforge.entity.PartEntity;
import net.neoforged.neoforge.event.EventHooks;
import net.neoforged.neoforge.event.entity.player.PlayerContainerEvent;
import net.neoforged.neoforge.event.entity.player.PlayerEvent;
import net.neoforged.neoforge.network.payload.AdvancedContainerSetDataPayload;
import net.neoforged.neoforge.network.payload.AdvancedOpenScreenPayload;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.WeatherType;
import org.bukkit.craftbukkit.CraftWorld;
import org.bukkit.craftbukkit.CraftWorldBorder;
import org.bukkit.craftbukkit.entity.CraftPlayer;
import org.bukkit.craftbukkit.event.CraftEventFactory;
import org.bukkit.craftbukkit.event.CraftPortalEvent;
import org.bukkit.craftbukkit.util.CraftLocation;
import org.bukkit.craftbukkit.inventory.CraftItemStack;
import org.bukkit.event.entity.EntityExhaustionEvent;
import org.bukkit.event.entity.EntityPotionEffectEvent;
import org.bukkit.event.entity.PlayerDeathEvent;
import org.bukkit.event.player.PlayerBedLeaveEvent;
import org.bukkit.event.player.PlayerChangedMainHandEvent;
import org.bukkit.event.player.PlayerChangedWorldEvent;
import org.bukkit.event.player.PlayerLocaleChangeEvent;
import org.bukkit.event.player.PlayerPortalEvent;
import org.bukkit.event.player.PlayerRespawnEvent;
import org.bukkit.event.player.PlayerSpawnChangeEvent;
import org.bukkit.event.player.PlayerTeleportEvent;
import org.bukkit.event.player.PlayerToggleSneakEvent;
import org.bukkit.inventory.MainHand;
import org.slf4j.Logger;

public class ServerPlayer extends Player {
    private static final Logger LOGGER = LogUtils.getLogger();
    public long lastSave = MinecraftServer.currentTick; // Paper - Incremental chunk and player saving
    private static final int NEUTRAL_MOB_DEATH_NOTIFICATION_RADII_XZ = 32;
    private static final int NEUTRAL_MOB_DEATH_NOTIFICATION_RADII_Y = 10;
    private static final int FLY_STAT_RECORDING_SPEED = 25;
    public static final double INTERACTION_DISTANCE_VERIFICATION_BUFFER = 1.0;
    private static final AttributeModifier CREATIVE_BLOCK_INTERACTION_RANGE_MODIFIER = new AttributeModifier(
        ResourceLocation.withDefaultNamespace("creative_mode_block_range"), 0.5, AttributeModifier.Operation.ADD_VALUE
    );
    private static final AttributeModifier CREATIVE_ENTITY_INTERACTION_RANGE_MODIFIER = new AttributeModifier(
        ResourceLocation.withDefaultNamespace("creative_mode_entity_range"), 2.0, AttributeModifier.Operation.ADD_VALUE
    );
    public ServerGamePacketListenerImpl connection;
    public final MinecraftServer server;
    public final ServerPlayerGameMode gameMode;
    private final PlayerAdvancements advancements;
    private final ServerStatsCounter stats;
    private float lastRecordedHealthAndAbsorption = Float.MIN_VALUE;
    private int lastRecordedFoodLevel = Integer.MIN_VALUE;
    private int lastRecordedAirLevel = Integer.MIN_VALUE;
    private int lastRecordedArmor = Integer.MIN_VALUE;
    private int lastRecordedLevel = Integer.MIN_VALUE;
    private int lastRecordedExperience = Integer.MIN_VALUE;
    private float lastSentHealth = -1.0E8F;
    private int lastSentFood = -99999999;
    private boolean lastFoodSaturationZero = true;
    public int lastSentExp = -99999999;
    public int spawnInvulnerableTime = 60;
    private ChatVisiblity chatVisibility = ChatVisiblity.FULL;
    private boolean canChatColor = true;
    private long lastActionTime = Util.getMillis();
    @Nullable
    private Entity camera;
    public boolean isChangingDimension;
    public boolean seenCredits;
    private final ServerRecipeBook recipeBook = new ServerRecipeBook();
    @Nullable
    private Vec3 levitationStartPos;
    private int levitationStartTime;
    private boolean disconnected;
    private int requestedViewDistance = 2;
    public String language = null; // CraftBukkit - default  // Paper - default to null
    public java.util.Locale adventure$locale = java.util.Locale.US; // Paper
    @Nullable
    private Vec3 startingToFallPosition;
    @Nullable
    private Vec3 enteredNetherPosition;
    @Nullable
    private Vec3 enteredLavaOnVehiclePosition;
    private SectionPos lastSectionPos = SectionPos.of(0, 0, 0);
    private ChunkTrackingView chunkTrackingView = ChunkTrackingView.EMPTY;
    private ResourceKey<Level> respawnDimension = Level.OVERWORLD;
    @Nullable
    private BlockPos respawnPosition;
    private boolean respawnForced;
    private float respawnAngle;
    private final TextFilter textFilter;
    private boolean textFilteringEnabled;
    private boolean allowsListing;
    private boolean spawnExtraParticlesOnFall;
    public WardenSpawnTracker wardenSpawnTracker = new WardenSpawnTracker(0, 0, 0);
    @Nullable
    private BlockPos raidOmenPosition;
    private Vec3 lastKnownClientMovement = Vec3.ZERO;
    public final ContainerSynchronizer containerSynchronizer = new ContainerSynchronizer() {
        @Override
        public void sendInitialData(AbstractContainerMenu p_143448_, NonNullList<ItemStack> p_143449_, ItemStack p_143450_, int[] p_143451_) {
            ServerPlayer.this.connection
                .send(new ClientboundContainerSetContentPacket(p_143448_.containerId, p_143448_.incrementStateId(), p_143449_, p_143450_));

            for (int i = 0; i < p_143451_.length; i++) {
                this.broadcastDataValue(p_143448_, i, p_143451_[i]);
            }
        }

        @Override
        public void sendSlotChange(AbstractContainerMenu p_143441_, int p_143442_, ItemStack p_143443_) {
            ServerPlayer.this.connection.send(new ClientboundContainerSetSlotPacket(p_143441_.containerId, p_143441_.incrementStateId(), p_143442_, p_143443_));
        }

        @Override
        public void sendCarriedChange(AbstractContainerMenu p_143445_, ItemStack p_143446_) {
            ServerPlayer.this.connection.send(new ClientboundContainerSetSlotPacket(-1, p_143445_.incrementStateId(), -1, p_143446_));
        }

        @Override
        public void sendDataChange(AbstractContainerMenu p_143437_, int p_143438_, int p_143439_) {
            this.broadcastDataValue(p_143437_, p_143438_, p_143439_);
        }

        private void broadcastDataValue(AbstractContainerMenu p_143455_, int p_143456_, int p_143457_) {
            if (ServerPlayer.this.connection.hasChannel(net.neoforged.neoforge.network.payload.AdvancedContainerSetDataPayload.TYPE)) {
                ServerPlayer.this.connection.send(new net.neoforged.neoforge.network.payload.AdvancedContainerSetDataPayload((byte) p_143455_.containerId, (short) p_143456_, p_143457_));
                return;
            }
            ServerPlayer.this.connection.send(new ClientboundContainerSetDataPacket(p_143455_.containerId, p_143456_, p_143457_));
        }
    };
    private final ContainerListener containerListener = new ContainerListener() {
        @Override
        public void slotChanged(AbstractContainerMenu p_143466_, int p_143467_, ItemStack p_143468_) {
            Slot slot = p_143466_.getSlot(p_143467_);
            if (!(slot instanceof ResultSlot)) {
                if (slot.container == ServerPlayer.this.getInventory()) {
                    CriteriaTriggers.INVENTORY_CHANGED.trigger(ServerPlayer.this, ServerPlayer.this.getInventory(), p_143468_);
                }
            }
        }

        // Paper start - Add PlayerInventorySlotChangeEvent
        @Override
        public void slotChanged(AbstractContainerMenu handler, int slotId, ItemStack oldStack, ItemStack stack) {
            Slot slot = handler.getSlot(slotId);
            if (!(slot instanceof ResultSlot)) {
                if (slot.container == ServerPlayer.this.getInventory()) {
                    if (io.papermc.paper.event.player.PlayerInventorySlotChangeEvent.getHandlerList().getRegisteredListeners().length == 0) {
                        CriteriaTriggers.INVENTORY_CHANGED.trigger(ServerPlayer.this, ServerPlayer.this.getInventory(), stack);
                        return;
                    }
                    io.papermc.paper.event.player.PlayerInventorySlotChangeEvent event = new io.papermc.paper.event.player.PlayerInventorySlotChangeEvent(ServerPlayer.this.getBukkitEntity(), slotId, CraftItemStack.asBukkitCopy(oldStack), CraftItemStack.asBukkitCopy(stack));
                    event.callEvent();
                    if (event.shouldTriggerAdvancements()) {
                        CriteriaTriggers.INVENTORY_CHANGED.trigger(ServerPlayer.this, ServerPlayer.this.getInventory(), stack);
                    }
                }
            }
        }
        // Paper end - Add PlayerInventorySlotChangeEvent

        @Override
        public void dataChanged(AbstractContainerMenu p_143462_, int p_143463_, int p_143464_) {
        }
    };
    @Nullable
    private RemoteChatSession chatSession;
    @Nullable
    public final Object object;
    private int containerCounter;
    public boolean wonGame;
    private int containerUpdateDelay; // Paper - Configurable container update tick rate
    // Paper start - cancellable death event
    public boolean queueHealthUpdatePacket;
    public net.minecraft.network.protocol.game.ClientboundSetHealthPacket queuedHealthUpdatePacket;
    // Paper end - cancellable death event
    // CraftBukkit start
    public CraftPlayer.TransferCookieConnection transferCookieConnection;
    public String displayName;
    public net.kyori.adventure.text.Component adventure$displayName; // Paper
    public Location compassTarget;
    public int newExp = 0;
    public int newLevel = 0;
    public int newTotalExp = 0;
    public boolean keepLevel = false;
    public double maxHealthCache;
    public boolean joining = true;
    public boolean sentListPacket = false;
    public boolean supressTrackerForLogin = false; // Paper - Fire PlayerJoinEvent when Player is actually ready
    // CraftBukkit end

    // Mohist start
    public boolean initialized = false;
    // Mohist end
    public boolean isRealPlayer; // Paper
    public long loginTime; // Paper - Replace OfflinePlayer#getLastPlayed
    public @Nullable String clientBrandName = null; // Paper - Brand support
    public org.bukkit.event.player.PlayerQuitEvent.QuitReason quitReason = null; // Paper - Add API for quit reason; there are a lot of changes to do if we change all methods leading to the event
    public com.destroystokyo.paper.event.entity.PlayerNaturallySpawnCreaturesEvent playerNaturallySpawnedEvent; // Paper - PlayerNaturallySpawnCreaturesEvent
    public boolean purpurClient = false; // Purpur
    private boolean tpsBar = false; // Purpur
    private boolean compassBar = false; // Purpur
    private boolean ramBar = false; // Purpur

    public ServerPlayer(MinecraftServer p_254143_, ServerLevel p_254435_, GameProfile p_253651_, ClientInformation p_301997_) {
        super(p_254435_, p_254435_.getSharedSpawnPos(), p_254435_.getSharedSpawnAngle(), p_253651_);
        this.textFilter = p_254143_.createTextFilterForPlayer(this);
        this.gameMode = p_254143_.createGameModeForPlayer(this);
        this.server = p_254143_;
        this.stats = p_254143_.getPlayerList().getPlayerStats(this);
        this.advancements = p_254143_.getPlayerList().getPlayerAdvancements(this);
        this.disable$moveTo.set(true);
        this.moveTo(this.adjustSpawnLocation(p_254435_, p_254435_.getSharedSpawnPos()).getBottomCenter(), 0.0F, 0.0F); // Paper - Don't move existing players to world spawn
        this.updateOptionsNoEvents(p_301997_); // Paper - don't call options events on login
        this.object = null;

        // CraftBukkit start
        this.displayName = this.getScoreboardName();
        this.adventure$displayName = net.kyori.adventure.text.Component.text(this.getScoreboardName()); // Paper
        this.bukkitPickUpLoot = true;
        this.maxHealthCache = this.getMaxHealth();
        this.initialized = true;
    }

    // Use method to resend items in hands in case of client desync, because the item use got cancelled.
    // For example, when cancelling the leash event
    public void resendItemInHands() {
        containerMenu.findSlot(getInventory(), getInventory().selected).ifPresent(new IntConsumer() {
            @Override
            public void accept(int s) {
                containerSynchronizer.sendSlotChange(containerMenu, s, ServerPlayer.this.getMainHandItem());
            }
        });
        containerSynchronizer.sendSlotChange(inventoryMenu, InventoryMenu.SHIELD_SLOT, getOffhandItem());
    }

    // Yes, this doesn't match Vanilla, but it's the best we can do for now.
    // If this is an issue, PRs are welcome
    public final BlockPos getSpawnPoint(ServerLevel worldserver) {
        BlockPos blockposition = worldserver.getSharedSpawnPos();

        if (worldserver.dimensionType().hasSkyLight() && worldserver.K.getGameType() != GameType.ADVENTURE) {
            int i = Math.max(0, this.server.getSpawnRadius(worldserver));
            int j = Mth.floor(worldserver.getWorldBorder().getDistanceToBorder((double) blockposition.getX(), (double) blockposition.getZ()));

            if (j < i) {
                i = j;
            }

            if (j <= 1) {
                i = 1;
            }

            long k = (long) (i * 2 + 1);
            long l = k * k;
            int i1 = l > 2147483647L ? Integer.MAX_VALUE : (int) l;
            int j1 = this.getCoprime(i1);
            int k1 = RandomSource.create().nextInt(i1);

            for (int l1 = 0; l1 < i1; ++l1) {
                int i2 = (k1 + j1 * l1) % i1;
                int j2 = i2 % (i * 2 + 1);
                int k2 = i2 / (i * 2 + 1);
                BlockPos blockposition1 = PlayerRespawnLogic.getOverworldRespawnPos(worldserver, blockposition.getX() + j2 - i, blockposition.getZ() + k2 - i);

                if (blockposition1 != null) {
                    return blockposition1;
                }
            }
        }

        return blockposition;
    }
    // CraftBukkit end

    // CraftBukkit start - World fallback code, either respawn location or global spawn
    public void spawnIn(Level world) {
        this.setLevel(world);
        if (world == null) {
            this.unsetRemoved();
            Vec3 position = null;
            if (this.respawnDimension != null) {
                world = this.server.getLevel(this.respawnDimension);
                if (world != null && this.getRespawnPosition() != null) {
                    position = ServerPlayer.findRespawnAndUseSpawnBlock((ServerLevel) world, this.getRespawnPosition(), this.getRespawnAngle(), false, false).map(RespawnPosAngle::position).orElse(null);
                }
            }
            if (world == null || position == null) {
                world = ((CraftWorld) Bukkit.getServer().getWorlds().get(0)).getHandle();
                position = Vec3.atCenterOf(world.getSharedSpawnPos());
            }
            this.setLevel(world);
            this.setPosRaw(position.x(), position.y(), position.z()); // Paper - don't register to chunks yet
        }
        this.gameMode.setLevel((ServerLevel) world);
    }
    // CraftBukkit end

    @Override
    public BlockPos adjustSpawnLocation(ServerLevel p_352206_, BlockPos p_352202_) {
        AABB aabb = this.getDimensions(Pose.STANDING).makeBoundingBox(Vec3.ZERO);
        BlockPos blockpos = p_352202_;
        if (p_352206_.dimensionType().hasSkyLight() && p_352206_.K.getGameType() != GameType.ADVENTURE) {
            int i = Math.max(0, this.server.getSpawnRadius(p_352206_));
            int j = Mth.floor(p_352206_.getWorldBorder().getDistanceToBorder((double)p_352202_.getX(), (double)p_352202_.getZ()));
            if (j < i) {
                i = j;
            }

            if (j <= 1) {
                i = 1;
            }

            long k = (long)(i * 2 + 1);
            long l = k * k;
            int i1 = l > 2147483647L ? Integer.MAX_VALUE : (int)l;
            int j1 = this.getCoprime(i1);
            int k1 = RandomSource.create().nextInt(i1);

            for (int l1 = 0; l1 < i1; l1++) {
                int i2 = (k1 + j1 * l1) % i1;
                int j2 = i2 % (i * 2 + 1);
                int k2 = i2 / (i * 2 + 1);
                blockpos = PlayerRespawnLogic.getOverworldRespawnPos(p_352206_, p_352202_.getX() + j2 - i, p_352202_.getZ() + k2 - i);
                if (blockpos != null && p_352206_.noCollision(this, aabb.move(blockpos.getBottomCenter()))) {
                    return blockpos;
                }
            }

            blockpos = p_352202_;
        }

        while (!p_352206_.noCollision(this, aabb.move(blockpos.getBottomCenter())) && blockpos.getY() < p_352206_.getMaxBuildHeight() - 1) {
            blockpos = blockpos.above();
        }

        while (p_352206_.noCollision(this, aabb.move(blockpos.below().getBottomCenter())) && blockpos.getY() > p_352206_.getMinBuildHeight() + 1) {
            blockpos = blockpos.below();
        }

        return blockpos;
    }

    private int getCoprime(int p_9238_) {
        return p_9238_ <= 16 ? p_9238_ - 1 : 17;
    }

    @Override
    public void readAdditionalSaveData(CompoundTag p_9131_) {
        super.readAdditionalSaveData(p_9131_);
        if (p_9131_.contains("warden_spawn_tracker", 10)) {
            WardenSpawnTracker.CODEC
                .parse(new Dynamic<>(NbtOps.INSTANCE, p_9131_.get("warden_spawn_tracker")))
                .resultOrPartial(LOGGER::error)
                .ifPresent(p_248205_ -> this.wardenSpawnTracker = p_248205_);
        }

        if (p_9131_.contains("enteredNetherPosition", 10)) {
            CompoundTag compoundtag = p_9131_.getCompound("enteredNetherPosition");
            this.enteredNetherPosition = new Vec3(compoundtag.getDouble("x"), compoundtag.getDouble("y"), compoundtag.getDouble("z"));
        }

        this.seenCredits = p_9131_.getBoolean("seenCredits");
        if (p_9131_.contains("recipeBook", 10)) {
            this.recipeBook.fromNbt(p_9131_.getCompound("recipeBook"), this.server.getRecipeManager());
        }
        this.getBukkitEntity().readExtraData(p_9131_); // CraftBukkit

        if (this.isSleeping()) {
            this.stopSleeping();
        }

        // CraftBukkit start
        String spawnWorld = p_9131_.getString("SpawnWorld");
        CraftWorld oldWorld = (CraftWorld) Bukkit.getWorld(spawnWorld);
        if (oldWorld != null) {
            this.respawnDimension = oldWorld.getHandle().dimension();
        }
        // CraftBukkit end

        if (p_9131_.contains("SpawnX", 99) && p_9131_.contains("SpawnY", 99) && p_9131_.contains("SpawnZ", 99)) {
            this.respawnPosition = new BlockPos(p_9131_.getInt("SpawnX"), p_9131_.getInt("SpawnY"), p_9131_.getInt("SpawnZ"));
            this.respawnForced = p_9131_.getBoolean("SpawnForced");
            this.respawnAngle = p_9131_.getFloat("SpawnAngle");
            if (p_9131_.contains("SpawnDimension")) {
                this.respawnDimension = Level.RESOURCE_KEY_CODEC
                    .parse(NbtOps.INSTANCE, p_9131_.get("SpawnDimension"))
                    .resultOrPartial(LOGGER::error)
                    .orElse(Level.OVERWORLD);
            }
        }

        this.spawnExtraParticlesOnFall = p_9131_.getBoolean("spawn_extra_particles_on_fall");
        Tag tag = p_9131_.get("raid_omen_position");
        if (tag != null) {
            BlockPos.CODEC.parse(NbtOps.INSTANCE, tag).resultOrPartial(LOGGER::error).ifPresent(p_337547_ -> this.raidOmenPosition = p_337547_);
        }

        if (p_9131_.contains("Purpur.TPSBar")) { this.tpsBar = p_9131_.getBoolean("Purpur.TPSBar"); } // Purpur
        if (p_9131_.contains("Purpur.CompassBar")) { this.compassBar = p_9131_.getBoolean("Purpur.CompassBar"); } // Purpur
        if (p_9131_.contains("Purpur.RamBar")) { this.ramBar = p_9131_.getBoolean("Purpur.RamBar"); } // Purpur
    }

    @Override
    public void addAdditionalSaveData(CompoundTag p_9197_) {
        super.addAdditionalSaveData(p_9197_);
        WardenSpawnTracker.CODEC
            .encodeStart(NbtOps.INSTANCE, this.wardenSpawnTracker)
            .resultOrPartial(LOGGER::error)
            .ifPresent(p_9134_ -> p_9197_.put("warden_spawn_tracker", p_9134_));
        this.storeGameTypes(p_9197_);
        p_9197_.putBoolean("seenCredits", this.seenCredits);
        if (this.enteredNetherPosition != null) {
            CompoundTag compoundtag = new CompoundTag();
            compoundtag.putDouble("x", this.enteredNetherPosition.x);
            compoundtag.putDouble("y", this.enteredNetherPosition.y);
            compoundtag.putDouble("z", this.enteredNetherPosition.z);
            p_9197_.put("enteredNetherPosition", compoundtag);
        }

        Entity entity1 = this.getRootVehicle();
        Entity entity = this.getVehicle();

        // CraftBukkit start - handle non-persistent vehicles
        boolean persistVehicle = true;
        if (entity != null) {
            Entity vehicle;
            for (vehicle = entity; vehicle != null; vehicle = vehicle.getVehicle()) {
                if (!vehicle.persist) {
                    persistVehicle = false;
                    break;
                }
            }
        }
        if (persistVehicle && entity != null && entity1 != this && entity1.hasExactlyOnePlayerPassenger() && !entity1.isRemoved()) { // Paper - Ensure valid vehicle status
            // CraftBukkit end
            CompoundTag compoundtag1 = new CompoundTag();
            CompoundTag compoundtag2 = new CompoundTag();
            entity1.save(compoundtag2);
            compoundtag1.putUUID("Attach", entity.getUUID());
            compoundtag1.put("Entity", compoundtag2);
            p_9197_.put("RootVehicle", compoundtag1);
        }

        p_9197_.put("recipeBook", this.recipeBook.toNbt());
        p_9197_.putString("Dimension", this.level().dimension().location().toString());
        if (this.respawnPosition != null) {
            p_9197_.putInt("SpawnX", this.respawnPosition.getX());
            p_9197_.putInt("SpawnY", this.respawnPosition.getY());
            p_9197_.putInt("SpawnZ", this.respawnPosition.getZ());
            p_9197_.putBoolean("SpawnForced", this.respawnForced);
            p_9197_.putFloat("SpawnAngle", this.respawnAngle);
            ResourceLocation.CODEC
                .encodeStart(NbtOps.INSTANCE, this.respawnDimension.location())
                .resultOrPartial(LOGGER::error)
                .ifPresent(p_248207_ -> p_9197_.put("SpawnDimension", p_248207_));
        }
        this.getBukkitEntity().setExtraData(p_9197_); // CraftBukkit

        p_9197_.putBoolean("spawn_extra_particles_on_fall", this.spawnExtraParticlesOnFall);
        if (this.raidOmenPosition != null) {
            BlockPos.CODEC
                .encodeStart(NbtOps.INSTANCE, this.raidOmenPosition)
                .resultOrPartial(LOGGER::error)
                .ifPresent(p_337549_ -> p_9197_.put("raid_omen_position", p_337549_));
        }

        p_9197_.putBoolean("Purpur.RamBar", this.ramBar); // Purpur
        p_9197_.putBoolean("Purpur.TPSBar", this.tpsBar); // Purpur
        p_9197_.putBoolean("Purpur.CompassBar", this.compassBar); // Purpur
    }

    public void setExperiencePoints(int p_8986_) {
        float f = (float)this.getXpNeededForNextLevel();
        float f1 = (f - 1.0F) / f;
        this.experienceProgress = Mth.clamp((float)p_8986_ / f, 0.0F, f1);
        this.lastSentExp = -1;
    }

    public void setExperienceLevels(int p_9175_) {
        this.experienceLevel = p_9175_;
        this.lastSentExp = -1;
    }

    @Override
    public void giveExperienceLevels(int p_9200_) {
        super.giveExperienceLevels(p_9200_);
        this.lastSentExp = -1;
    }

    @Override
    public void onEnchantmentPerformed(ItemStack p_9079_, int p_9080_) {
        super.onEnchantmentPerformed(p_9079_, p_9080_);
        this.lastSentExp = -1;
    }

    public void initMenu(AbstractContainerMenu p_143400_) {
        p_143400_.addSlotListener(this.containerListener);
        p_143400_.setSynchronizer(this.containerSynchronizer);
    }

    public void initInventoryMenu() {
        this.initMenu(this.inventoryMenu);
    }

    @Override
    public void onEnterCombat() {
        super.onEnterCombat();
        this.connection.send(ClientboundPlayerCombatEnterPacket.INSTANCE);
    }

    @Override
    public void onLeaveCombat() {
        super.onLeaveCombat();
        this.connection.send(new ClientboundPlayerCombatEndPacket(this.getCombatTracker()));
    }

    @Override
    public void onInsideBlock(BlockState p_9103_) {
        CriteriaTriggers.ENTER_BLOCK.trigger(this, p_9103_);
    }

    @Override
    protected ItemCooldowns createItemCooldowns() {
        return new ServerItemCooldowns(this);
    }

    @Override
    public void tick() {
        // CraftBukkit start
        if (this.joining) {
            this.joining = false;
        }
        // CraftBukkit end
        this.gameMode.tick();
        this.wardenSpawnTracker.tick();
        this.spawnInvulnerableTime--;
        if (this.invulnerableTime > 0) {
            this.invulnerableTime--;
        }

        // Paper start - Configurable container update tick rate
        if (--containerUpdateDelay <= 0) {
            this.containerMenu.broadcastChanges();
            containerUpdateDelay = this.level().paperConfig().tickRates.containerUpdate;
        }
        // Paper end - Configurable container update tick rate
        if (!this.level().isClientSide && this.containerMenu != this.inventoryMenu && (this.isImmobile() || !this.containerMenu.stillValid(this))) { // Paper - Prevent opening inventories when frozen
            InventoryOwner.setClose$Reason(org.bukkit.event.inventory.InventoryCloseEvent.Reason.CANT_USE); // Paper - Inventory close reason
            this.closeContainer();
            this.containerMenu = this.inventoryMenu;
        }

        Entity entity = this.getCamera();
        if (entity != this) {
            if (entity.isAlive()) {
                this.absMoveTo(entity.getX(), entity.getY(), entity.getZ(), entity.getYRot(), entity.getXRot());
                this.serverLevel().getChunkSource().move(this);
                if (this.wantsToStopRiding()) {
                    this.setCamera(this);
                }
            } else {
                this.setCamera(this);
            }
        }

        CriteriaTriggers.TICK.trigger(this);
        if (this.levitationStartPos != null) {
            CriteriaTriggers.LEVITATION.trigger(this, this.levitationStartPos, this.tickCount - this.levitationStartTime);
        }

        this.trackStartFallingPosition();
        this.trackEnteredOrExitedLavaOnVehicle();
        this.updatePlayerAttributes();
        this.advancements.flushDirty(this);

        // Purpur start
        if (this.level().purpurConfig.useNightVisionWhenRiding && this.getVehicle() != null && this.getVehicle().getRider() == this && this.level().getGameTime() % 100 == 0) { // 5 seconds
            MobEffectInstance nightVision = this.getEffect(MobEffects.NIGHT_VISION);
            if (nightVision == null || nightVision.getDuration() <= 300) { // 15 seconds
                this.addEffect(new MobEffectInstance(MobEffects.NIGHT_VISION, 400, 0)); // 20 seconds
            }
        }
        // Purpur end
    }

    private void updatePlayerAttributes() {
        AttributeInstance attributeinstance = this.getAttribute(Attributes.BLOCK_INTERACTION_RANGE);
        if (attributeinstance != null) {
            if (this.isCreative()) {
                attributeinstance.addOrUpdateTransientModifier(CREATIVE_BLOCK_INTERACTION_RANGE_MODIFIER);
            } else {
                attributeinstance.removeModifier(CREATIVE_BLOCK_INTERACTION_RANGE_MODIFIER);
            }
        }

        AttributeInstance attributeinstance1 = this.getAttribute(Attributes.ENTITY_INTERACTION_RANGE);
        if (attributeinstance1 != null) {
            if (this.isCreative()) {
                attributeinstance1.addOrUpdateTransientModifier(CREATIVE_ENTITY_INTERACTION_RANGE_MODIFIER);
            } else {
                attributeinstance1.removeModifier(CREATIVE_ENTITY_INTERACTION_RANGE_MODIFIER);
            }
        }
    }

    public void doTick() {
        try {
            if (valid && !this.isSpectator() || !this.touchingUnloadedChunk()) { // Paper - don't tick dead players that are not in the world currently (pending respawn)
                super.tick();
            }

            for (int i = 0; i < this.getInventory().getContainerSize(); i++) {
                ItemStack itemstack = this.getInventory().getItem(i);
                if (itemstack.getItem().isComplex()) {
                    Packet<?> packet = ((ComplexItem)itemstack.getItem()).getUpdatePacket(itemstack, this.level(), this);
                    if (packet != null) {
                        this.connection.send(packet);
                    }
                }
            }

            if (this.getHealth() != this.lastSentHealth
                || this.lastSentFood != this.foodData.getFoodLevel()
                || this.foodData.getSaturationLevel() == 0.0F != this.lastFoodSaturationZero) {
                this.connection.send(new ClientboundSetHealthPacket(this.getBukkitEntity().getScaledHealth(), this.foodData.getFoodLevel(), this.foodData.getSaturationLevel())); // CraftBukkit
                this.lastSentHealth = this.getHealth();
                this.lastSentFood = this.foodData.getFoodLevel();
                this.lastFoodSaturationZero = this.foodData.getSaturationLevel() == 0.0F;
            }

            if (this.getHealth() + this.getAbsorptionAmount() != this.lastRecordedHealthAndAbsorption) {
                this.lastRecordedHealthAndAbsorption = this.getHealth() + this.getAbsorptionAmount();
                this.updateScoreForCriteria(ObjectiveCriteria.HEALTH, Mth.ceil(this.lastRecordedHealthAndAbsorption));
            }

            if (this.foodData.getFoodLevel() != this.lastRecordedFoodLevel) {
                this.lastRecordedFoodLevel = this.foodData.getFoodLevel();
                this.updateScoreForCriteria(ObjectiveCriteria.FOOD, Mth.ceil((float)this.lastRecordedFoodLevel));
            }

            if (this.getAirSupply() != this.lastRecordedAirLevel) {
                this.lastRecordedAirLevel = this.getAirSupply();
                this.updateScoreForCriteria(ObjectiveCriteria.AIR, Mth.ceil((float)this.lastRecordedAirLevel));
            }

            if (this.getArmorValue() != this.lastRecordedArmor) {
                this.lastRecordedArmor = this.getArmorValue();
                this.updateScoreForCriteria(ObjectiveCriteria.ARMOR, Mth.ceil((float)this.lastRecordedArmor));
            }

            if (this.totalExperience != this.lastRecordedExperience) {
                this.lastRecordedExperience = this.totalExperience;
                this.updateScoreForCriteria(ObjectiveCriteria.EXPERIENCE, Mth.ceil((float)this.lastRecordedExperience));
            }

            // CraftBukkit start - Force max health updates
            if (this.maxHealthCache != this.getMaxHealth()) {
                this.getBukkitEntity().updateScaledHealth();
            }
            // CraftBukkit end

            if (this.experienceLevel != this.lastRecordedLevel) {
                this.lastRecordedLevel = this.experienceLevel;
                this.updateScoreForCriteria(ObjectiveCriteria.LEVEL, Mth.ceil((float)this.lastRecordedLevel));
            }

            if (this.totalExperience != this.lastSentExp) {
                this.lastSentExp = this.totalExperience;
                this.connection.send(new ClientboundSetExperiencePacket(this.experienceProgress, this.totalExperience, this.experienceLevel));
            }

            if (this.getAbilities().flying && !this.mayFly()) {
                this.getAbilities().flying = false;
                this.onUpdateAbilities();
            }

            if (this.tickCount % 20 == 0) {
                CriteriaTriggers.LOCATION.trigger(this);
            }

            // CraftBukkit start - initialize oldLevel, fire PlayerLevelChangeEvent, and tick client-sided world border
            if (this.oldLevel == -1) {
                this.oldLevel = this.experienceLevel;
            }

            if (this.oldLevel != this.experienceLevel) {
                CraftEventFactory.callPlayerLevelChangeEvent(this.getBukkitEntity(), this.oldLevel, this.experienceLevel);
                this.oldLevel = this.experienceLevel;
            }

            if (this.getBukkitEntity().hasClientWorldBorder()) {
                ((CraftWorldBorder) this.getBukkitEntity().getWorldBorder()).getHandle().tick();
            }
            // CraftBukkit end

        } catch (Throwable throwable) {
            CrashReport crashreport = CrashReport.forThrowable(throwable, "Ticking player");
            CrashReportCategory crashreportcategory = crashreport.addCategory("Player being ticked");
            this.fillCrashReportCategory(crashreportcategory);
            throw new ReportedException(crashreport);
        }
    }

    @Override
    public void resetFallDistance() {
        if (this.getHealth() > 0.0F && this.startingToFallPosition != null) {
            CriteriaTriggers.FALL_FROM_HEIGHT.trigger(this, this.startingToFallPosition);
        }

        this.startingToFallPosition = null;
        super.resetFallDistance();
    }

    public void trackStartFallingPosition() {
        if (this.fallDistance > 0.0F && this.startingToFallPosition == null) {
            this.startingToFallPosition = this.position();
            if (this.currentImpulseImpactPos != null && this.currentImpulseImpactPos.y <= this.startingToFallPosition.y) {
                CriteriaTriggers.FALL_AFTER_EXPLOSION.trigger(this, this.currentImpulseImpactPos, this.currentExplosionCause);
            }
        }
    }

    public void trackEnteredOrExitedLavaOnVehicle() {
        if (this.getVehicle() != null && this.getVehicle().isInLava()) {
            if (this.enteredLavaOnVehiclePosition == null) {
                this.enteredLavaOnVehiclePosition = this.position();
            } else {
                CriteriaTriggers.RIDE_ENTITY_IN_LAVA_TRIGGER.trigger(this, this.enteredLavaOnVehiclePosition);
            }
        }

        if (this.enteredLavaOnVehiclePosition != null && (this.getVehicle() == null || !this.getVehicle().isInLava())) {
            this.enteredLavaOnVehiclePosition = null;
        }
    }

    private void updateScoreForCriteria(ObjectiveCriteria p_9105_, int p_9106_) {
        this.getScoreboard().forAllObjectives(p_9105_, this, p_313589_ -> p_313589_.set(p_9106_));
    }

    @Override
    public void die(DamageSource p_9035_) {
        // this.gameEvent(GameEvent.ENTITY_DIE); // Paper - move below event cancellation check
        if (CommonHooks.onLivingDeath(this, p_9035_)) return;
        boolean flag = this.level().getGameRules().getBoolean(GameRules.RULE_SHOWDEATHMESSAGES);

        // CraftBukkit start - fire PlayerDeathEvent
        if (this.isRemoved()) {
            return;
        }
        List<org.bukkit.inventory.ItemStack> loot = new ArrayList<>(this.getInventory().getContainerSize());
        boolean keepInventory = KeepInventory.inventory(this) || this.level().getGameRules().getBoolean(GameRules.RULE_KEEPINVENTORY) || this.isSpectator();

        if (!keepInventory) {
            for (ItemStack item : this.getInventory().getContents()) {
                if (!item.isEmpty() && !EnchantmentHelper.has(item, EnchantmentEffectComponents.PREVENT_EQUIPMENT_DROP)) {
                    loot.add(CraftItemStack.asCraftMirror(item).markForInventoryDrop());
                }
            }
        }

        if (this.shouldDropLoot() && this.level().getGameRules().getBoolean(GameRules.RULE_DOMOBLOOT)) { // Paper - fix player loottables running when mob loot gamerule is false
            // SPIGOT-5071: manually add player loot tables (SPIGOT-5195 - ignores keepInventory rule)
            this.dropFromLootTable(p_9035_, this.lastHurtByPlayerTime > 0);
            this.dropCustomDeathLoot(this.serverLevel(), p_9035_, flag);
        } // Paper - fix player loottables running when mob loot gamerule is false

        Component defaultMessage = this.getCombatTracker().getDeathMessage();
        keepLevel = keepInventory; // SPIGOT-2222: pre-set keepLevel
        PlayerDeathEvent event = CraftEventFactory.callPlayerDeathEvent(this, p_9035_, loot, PaperAdventure.asAdventure(defaultMessage), keepInventory); // Paper - Adventure
        // Paper start - cancellable death event
        if (event.isCancelled()) {
            // make compatible with plugins that might have already set the health in an event listener
            if (this.getHealth() <= 0) {
                this.setHealth((float) event.getReviveHealth());
            }
            return;
        }
        this.gameEvent(GameEvent.ENTITY_DIE); // moved from the top of this method
        // Paper end

        // SPIGOT-943 - only call if they have an inventory open
        if (this.containerMenu != this.inventoryMenu) {
            InventoryOwner.setClose$Reason(org.bukkit.event.inventory.InventoryCloseEvent.Reason.DEATH); // Paper - Inventory close reason
            this.closeContainer();
        }
        net.kyori.adventure.text.Component deathMessage = event.deathMessage() != null ? event.deathMessage() : net.kyori.adventure.text.Component.empty(); // Paper - Adventure
        if (deathMessage != null && deathMessage != net.kyori.adventure.text.Component.empty() && flag) { // Paper - Adventure // TODO: allow plugins to override?
            Component component = PaperAdventure.asVanilla(deathMessage); // Paper - Adventure
            this.connection
                .send(
                    new ClientboundPlayerCombatKillPacket(this.getId(), component),
                    PacketSendListener.exceptionallySend(
                        () -> {
                            int i = 256;
                            String s = component.getString(256);
                            Component component1 = Component.translatable(
                                "death.attack.message_too_long", Component.literal(s).withStyle(ChatFormatting.YELLOW)
                            );
                            Component component2 = Component.translatable("death.attack.even_more_magic", this.getDisplayName())
                                .withStyle(p_143420_ -> p_143420_.withHoverEvent(new HoverEvent(HoverEvent.Action.SHOW_TEXT, component1)));
                            return new ClientboundPlayerCombatKillPacket(this.getId(), component2);
                        }
                    )
                );
            Team team = this.getTeam();
            if (team == null || team.getDeathMessageVisibility() == Team.Visibility.ALWAYS) {
                this.server.getPlayerList().broadcastSystemMessage(component, false);
            } else if (team.getDeathMessageVisibility() == Team.Visibility.HIDE_FOR_OTHER_TEAMS) {
                this.server.getPlayerList().broadcastSystemToTeam(this, component);
            } else if (team.getDeathMessageVisibility() == Team.Visibility.HIDE_FOR_OWN_TEAM) {
                this.server.getPlayerList().broadcastSystemToAllExceptTeam(this, component);
            }
        } else {
            this.connection.send(new ClientboundPlayerCombatKillPacket(this.getId(), CommonComponents.EMPTY));
        }

        this.removeEntitiesOnShoulder();
        if (this.level().getGameRules().getBoolean(GameRules.RULE_FORGIVE_DEAD_PLAYERS)) {
            this.tellNeutralMobsThatIDied();
        }
        shouldDropExperience.set(event.shouldDropExperience()); // Paper - tie to event
        // we clean the player's inventory after the EntityDeathEvent is called so plugins can get the exact state of the inventory.
        if (!event.getKeepInventory()) {
            this.dropAllDeathLoot(this.serverLevel(), p_9035_);
        }
        this.setCamera(this); // Remove spectated target
        // CraftBukkit end

        this.getScoreboard().forAllObjectives(ObjectiveCriteria.DEATH_COUNT, this, ScoreAccess::increment);
        LivingEntity livingentity = this.getKillCredit();
        if (livingentity != null) {
            this.awardStat(Stats.ENTITY_KILLED_BY.get(livingentity.getType()));
            livingentity.awardKillScore(this, this.deathScore, p_9035_);
            this.createWitherRose(livingentity);
        }

        this.level().broadcastEntityEvent(this, (byte)3);
        this.awardStat(Stats.DEATHS);
        this.resetStat(Stats.CUSTOM.get(Stats.TIME_SINCE_DEATH));
        this.resetStat(Stats.CUSTOM.get(Stats.TIME_SINCE_REST));
        this.clearFire();
        this.setTicksFrozen(0);
        this.setSharedFlagOnFire(false);
        this.getCombatTracker().recheckStatus();
        this.setLastDeathLocation(Optional.of(GlobalPos.of(this.level().dimension(), this.blockPosition())));
    }

    private void tellNeutralMobsThatIDied() {
        AABB aabb = new AABB(this.blockPosition()).inflate(32.0, 10.0, 32.0);
        this.level()
            .getEntitiesOfClass(Mob.class, aabb, EntitySelector.NO_SPECTATORS)
            .stream()
            .filter(p_9188_ -> p_9188_ instanceof NeutralMob)
            .forEach(p_9057_ -> ((NeutralMob)p_9057_).playerDied(this));
    }

    @Override
    public void awardKillScore(Entity p_9050_, int p_9051_, DamageSource p_9052_) {
        if (p_9050_ != this) {
            super.awardKillScore(p_9050_, p_9051_, p_9052_);
            this.increaseScore(p_9051_);
            this.getScoreboard().forAllObjectives(ObjectiveCriteria.KILL_COUNT_ALL, this, ScoreAccess::increment);
            if (p_9050_ instanceof Player) {
                this.awardStat(Stats.PLAYER_KILLS);
                this.getScoreboard().forAllObjectives(ObjectiveCriteria.KILL_COUNT_PLAYERS, this, ScoreAccess::increment);
            } else {
                this.awardStat(Stats.MOB_KILLS);
            }

            this.handleTeamKill(this, p_9050_, ObjectiveCriteria.TEAM_KILL);
            this.handleTeamKill(p_9050_, this, ObjectiveCriteria.KILLED_BY_TEAM);
            CriteriaTriggers.PLAYER_KILLED_ENTITY.trigger(this, p_9050_, p_9052_);
        }
    }

    private void handleTeamKill(ScoreHolder p_313693_, ScoreHolder p_313814_, ObjectiveCriteria[] p_9127_) {
        PlayerTeam playerteam = this.getScoreboard().getPlayersTeam(p_313814_.getScoreboardName());
        if (playerteam != null) {
            int i = playerteam.getColor().getId();
            if (i >= 0 && i < p_9127_.length) {
                this.getScoreboard().forAllObjectives(p_9127_[i], p_313693_, ScoreAccess::increment);
            }
        }
    }

    @Override
    public boolean hurt(DamageSource p_9037_, float p_9038_) {
        if (this.isInvulnerableTo(p_9037_)) {
            return false;
        } else {
            boolean flag = this.server.isDedicatedServer() && this.isPvpAllowed() && p_9037_.is(DamageTypeTags.IS_FALL);
            if (!flag && this.spawnInvulnerableTime > 0 && !p_9037_.is(DamageTypeTags.BYPASSES_INVULNERABILITY)) {
                return false;
            } else {
                Entity entity = p_9037_.getEntity();
                if (entity instanceof Player player && !this.canHarmPlayer(player)) {
                    return false;
                }

                if (entity instanceof AbstractArrow abstractarrow && abstractarrow.getOwner() instanceof Player player1 && !this.canHarmPlayer(player1)) {
                    return false;
                }

                // Paper start - cancellable death events
                //return super.hurt(source, amount);
                this.queueHealthUpdatePacket = true;
                boolean damaged = super.hurt(p_9037_, p_9038_);
                this.queueHealthUpdatePacket = false;
                if (this.queuedHealthUpdatePacket != null) {
                    this.connection.send(this.queuedHealthUpdatePacket);
                    this.queuedHealthUpdatePacket = null;
                }
                return damaged;
                // Paper end
            }
        }
    }

    @Override
    public boolean canHarmPlayer(Player p_9064_) {
        return !this.isPvpAllowed() ? false : super.canHarmPlayer(p_9064_);
    }

    private boolean isPvpAllowed() {
        // CraftBukkit - this.server.isPvpAllowed() -> this.world.pvpMode
        return this.level().pvpMode;
    }

    public PlayerRespawnEvent.RespawnReason reason = null;
    public DimensionTransition findRespawnPositionAndUseSpawnBlock(boolean p_348590_, DimensionTransition.PostDimensionTransition p_352261_, PlayerRespawnEvent.RespawnReason reason) {
        this.reason = reason;
        return this.findRespawnPositionAndUseSpawnBlock(p_348590_, p_352261_);
    }

    // CraftBukkit start
    public DimensionTransition findRespawnPositionAndUseSpawnBlock(boolean p_348590_, DimensionTransition.PostDimensionTransition p_352261_) {
        DimensionTransition dimensionTransition;
        boolean isBedSpawn = false;
        boolean isAnchorSpawn = false;
        // CraftBukkit end
        BlockPos blockpos = this.getRespawnPosition();
        float f = this.getRespawnAngle();
        boolean flag = this.isRespawnForced();
        ServerLevel serverlevel = this.server.getLevel(this.getRespawnDimension());
        if (serverlevel != null && blockpos != null) {
            Optional<RespawnPosAngle> optional = findRespawnAndUseSpawnBlock(serverlevel, blockpos, f, flag, p_348590_);
            if (optional.isPresent()) {
                RespawnPosAngle serverplayer$respawnposangle = optional.get();
                // CraftBukkit start
                isBedSpawn = serverplayer$respawnposangle.isBedSpawn();
                isAnchorSpawn = serverplayer$respawnposangle.isAnchorSpawn();
                dimensionTransition = new DimensionTransition(serverlevel, serverplayer$respawnposangle.position(), Vec3.ZERO, serverplayer$respawnposangle.yaw(), 0.0F, p_352261_);
                // CraftBukkit end
            } else {
                dimensionTransition = DimensionTransition.missingRespawnBlock(this.server.overworld(), this, p_352261_); // CraftBukkit
            }
        } else {
            dimensionTransition = new DimensionTransition(this.server.overworld(), this, p_352261_); // CraftBukkit
        }

        // CraftBukkit start
        if (reason == null) {
            return dimensionTransition;
        }

        org.bukkit.entity.Player respawnPlayer = this.getBukkitEntity();
        Location location = CraftLocation.toBukkit(dimensionTransition.pos(), dimensionTransition.newLevel().getWorld(), dimensionTransition.yRot(), dimensionTransition.xRot());
        // Paper start - respawn flags
        com.google.common.collect.ImmutableSet.Builder<org.bukkit.event.player.PlayerRespawnEvent.RespawnFlag> builder = com.google.common.collect.ImmutableSet.builder();
        if (reason == org.bukkit.event.player.PlayerRespawnEvent.RespawnReason.END_PORTAL) {
            builder.add(org.bukkit.event.player.PlayerRespawnEvent.RespawnFlag.END_PORTAL);
        }
        PlayerRespawnEvent respawnEvent = new PlayerRespawnEvent(respawnPlayer, location, isBedSpawn, isAnchorSpawn, reason, builder);
        // Paper end - respawn flags
        this.level().getCraftServer().getPluginManager().callEvent(respawnEvent);
        // Spigot Start
        if (this.connection.isDisconnected()) {
            return null;
        }
        // Spigot End
        location = respawnEvent.getRespawnLocation();
        return new DimensionTransition(((CraftWorld) location.getWorld()).getHandle(), CraftLocation.toVec3D(location), dimensionTransition.speed(), location.getYaw(), location.getPitch(), dimensionTransition.missingRespawnBlock(), dimensionTransition.postDimensionTransition(), dimensionTransition.cause());
        // CraftBukkit end
    }

    public static Optional<ServerPlayer.RespawnPosAngle> findRespawnAndUseSpawnBlock(
        ServerLevel p_348505_, BlockPos p_348607_, float p_348481_, boolean p_348513_, boolean p_348600_
    ) {
        BlockState blockstate = p_348505_.getBlockState(p_348607_);
        Block block = blockstate.getBlock();
        if (block instanceof RespawnAnchorBlock
            && (p_348513_ || blockstate.getValue(RespawnAnchorBlock.CHARGE) > 0)
            && RespawnAnchorBlock.canSetSpawn(p_348505_)) {
            Optional<Vec3> optional = RespawnAnchorBlock.findStandUpPosition(EntityType.PLAYER, p_348505_, p_348607_);
            if (!p_348513_ && !p_348600_ && optional.isPresent()) {
                p_348505_.setBlock(
                    p_348607_, blockstate.setValue(RespawnAnchorBlock.CHARGE, Integer.valueOf(blockstate.getValue(RespawnAnchorBlock.CHARGE) - 1)), 3
                );
            }

            return optional.map(p_348139_ -> ServerPlayer.RespawnPosAngle.of(p_348139_, p_348607_, false, true)); // CraftBukkit
        } else if (block instanceof BedBlock && BedBlock.canSetSpawn(p_348505_)) {
            return BedBlock.findStandUpPosition(EntityType.PLAYER, p_348505_, p_348607_, blockstate.getValue(BedBlock.FACING), p_348481_)
                .map(p_348148_ -> ServerPlayer.RespawnPosAngle.of(p_348148_, p_348607_, true, false));
        } else if (!p_348513_) {
            return blockstate.getRespawnPosition(EntityType.PLAYER, p_348505_, p_348607_, p_348481_);
        } else {
            boolean flag = block.isPossibleToRespawnInThis(blockstate);
            BlockState blockstate1 = p_348505_.getBlockState(p_348607_.above());
            boolean flag1 = blockstate1.getBlock().isPossibleToRespawnInThis(blockstate1);
            return flag && flag1
                ? Optional.of(
                    new ServerPlayer.RespawnPosAngle(
                        new Vec3((double)p_348607_.getX() + 0.5, (double)p_348607_.getY() + 0.1, (double)p_348607_.getZ() + 0.5), p_348481_, false, false // CraftBukkit
                    )
                )
                : Optional.empty();
        }
    }

    public void showEndCredits() {
        this.unRide();
        this.serverLevel().removePlayerImmediately(this, Entity.RemovalReason.CHANGED_DIMENSION);
        if (!this.wonGame) {
            this.wonGame = true;
            this.connection.send(new ClientboundGameEventPacket(ClientboundGameEventPacket.WIN_GAME, 0.0F));
            this.seenCredits = true;
        }
    }

    @Nullable
    @Override
    public Entity changeDimension(DimensionTransition p_350472_) {
	    if (this.isSleeping()) return null; // CraftBukkit - SPIGOT-3154
        if (!CommonHooks.onTravelToDimension(this, p_350472_.newLevel().dimension())) return null;
        if (this.isRemoved()) {
            return null;
        } else {
            if (p_350472_.missingRespawnBlock()) {
                this.connection.send(new ClientboundGameEventPacket(ClientboundGameEventPacket.NO_RESPAWN_BLOCK_AVAILABLE, 0.0F));
            }

            ServerLevel serverlevel = p_350472_.newLevel();
            ServerLevel serverlevel1 = this.serverLevel();
            ResourceKey<Level> resourcekey = serverlevel1.dimension();
            if (serverlevel == serverlevel1) {
                // Paper start - gateway-specific teleport event
                if (this.portalProcess != null && this.portalProcess.isSamePortal(((net.minecraft.world.level.block.EndGatewayBlock) net.minecraft.world.level.block.Blocks.END_GATEWAY)) && this.serverLevel().getBlockEntity(this.portalProcess.getEntryPosition()) instanceof net.minecraft.world.level.block.entity.TheEndGatewayBlockEntity theEndGatewayBlockEntity) {
                    Location to = CraftLocation.toBukkit(p_350472_.pos(), this.serverLevel().getWorld(), p_350472_.yRot(), p_350472_.xRot());
                    final PlayerTeleportEndGatewayEvent event = new PlayerTeleportEndGatewayEvent(this.getBukkitEntity(), this.getBukkitEntity().getLocation(), to, new org.bukkit.craftbukkit.block.CraftEndGateway(to.getWorld(), theEndGatewayBlockEntity));
                    if (!event.callEvent() || event.getTo() == null) {
                        return null;
                    }
                    this.connection.teleport(event.getTo());
                } else {
                    // Paper end - gateway-specific teleport event
                    boolean result = this.connection.teleport(p_350472_.pos().x, p_350472_.pos().y, p_350472_.pos().z, p_350472_.yRot(), p_350472_.xRot(), p_350472_.cause());
                    if (!result) {
                        return null;
                    }
                    // CraftBukkit end
                } // Paper
                this.connection.resetPosition();
                p_350472_.postDimensionTransition().onTransition(this);
                return this;
            } else {
                this.isChangingDimension = true;
                LevelData leveldata = serverlevel.getLevelData();
                this.connection.send(new ClientboundRespawnPacket(this.createCommonSpawnInfo(serverlevel), (byte)3));
                this.connection.send(new ClientboundChangeDifficultyPacket(leveldata.getDifficulty(), leveldata.isDifficultyLocked()));
                PlayerList playerlist = this.server.getPlayerList();
                playerlist.sendPlayerPermissionLevel(this);
                serverlevel1.removePlayerImmediately(this, Entity.RemovalReason.CHANGED_DIMENSION);
                this.revive();
                serverlevel1.getProfiler().push("moving");
                if (resourcekey == Level.OVERWORLD && serverlevel.dimension() == Level.NETHER) {
                    this.enteredNetherPosition = this.position();
                }

                serverlevel1.getProfiler().pop();
                serverlevel1.getProfiler().push("placing");
                this.setServerLevel(serverlevel);
                this.connection.teleport(p_350472_.pos().x, p_350472_.pos().y, p_350472_.pos().z, p_350472_.yRot(), p_350472_.xRot());
                this.connection.resetPosition();
                serverlevel.addDuringTeleport(this);
                serverlevel1.getProfiler().pop();
                this.triggerDimensionChangeTriggers(serverlevel1);
                this.connection.send(new ClientboundPlayerAbilitiesPacket(this.getAbilities()));
                playerlist.sendLevelInfo(this, serverlevel);
                playerlist.sendAllPlayerInfo(this);
                playerlist.sendActivePlayerEffects(this);
                // TODO 1.21: Play custom teleport sound
                p_350472_.postDimensionTransition().onTransition(this);
                this.lastSentExp = -1;
                this.lastSentHealth = -1.0F;
                this.lastSentFood = -1;
                // Neo: On the client all attachments are lost on dimension change and need to be re-sent
                net.neoforged.neoforge.attachment.AttachmentSync.syncInitialPlayerAttachments(this);
                EventHooks.firePlayerChangedDimensionEvent(this, resourcekey, p_350472_.newLevel().dimension());

                PlayerChangedWorldEvent changeEvent = new PlayerChangedWorldEvent(this.getBukkitEntity(), serverlevel1.getWorld());
                this.level().getCraftServer().getPluginManager().callEvent(changeEvent);
                // Paper start - Reset shield blocking on dimension change
                if (this.isBlocking()) {
                    this.stopUsingItem();
                }
                // Paper end - Reset shield blocking on dimension change
                return this;
            }
        }
    }

    // CraftBukkit start
    @Override
    public CraftPortalEvent callPortalEvent(Entity entity, Location exit, PlayerTeleportEvent.TeleportCause cause, int searchRadius, int creationRadius) {
        Location enter = this.getBukkitEntity().getLocation();
        PlayerPortalEvent event = new PlayerPortalEvent(this.getBukkitEntity(), enter, exit, cause, searchRadius, true, creationRadius);
        Bukkit.getServer().getPluginManager().callEvent(event);
        if (event.isCancelled() || event.getTo() == null || event.getTo().getWorld() == null) {
            return null;
        }
        return new CraftPortalEvent(event);
    }
    // CraftBukkit end

    public void triggerDimensionChangeTriggers(ServerLevel p_9210_) {
        ResourceKey<Level> resourcekey = p_9210_.dimension();
        ResourceKey<Level> resourcekey1 = this.level().dimension();
        CriteriaTriggers.CHANGED_DIMENSION.trigger(this, resourcekey, resourcekey1);
        if (resourcekey == Level.NETHER && resourcekey1 == Level.OVERWORLD && this.enteredNetherPosition != null) {
            CriteriaTriggers.NETHER_TRAVEL.trigger(this, this.enteredNetherPosition);
        }

        if (resourcekey1 != Level.NETHER) {
            this.enteredNetherPosition = null;
        }
    }

    @Override
    public boolean broadcastToPlayer(ServerPlayer p_9014_) {
        if (p_9014_.isSpectator()) {
            return this.getCamera() == this;
        } else {
            return this.isSpectator() ? false : super.broadcastToPlayer(p_9014_);
        }
    }

    @Override
    public void take(Entity p_9047_, int p_9048_) {
        super.take(p_9047_, p_9048_);
        this.containerMenu.broadcastChanges();
    }

    @Override
    public Either<Player.BedSleepingProblem, Unit> startSleepInBed(BlockPos p_9115_) {
        // Neo: Encapsulate the vanilla check logic to supply to the CanPlayerSleepEvent
        var vanillaResult = ((java.util.function.Supplier<Either<BedSleepingProblem, Unit>>) () -> {
        // Guard against modded beds that may not have the FACING property.
        // We just return success (Unit) here. Modders will need to implement conditions in the CanPlayerSleepEvent
        if (!this.level().getBlockState(p_9115_).hasProperty(HorizontalDirectionalBlock.FACING)) {
            return Either.right(Unit.INSTANCE);
        }

        // Start vanilla code
        Direction direction = this.level().getBlockState(p_9115_).getValue(HorizontalDirectionalBlock.FACING);
        if (this.isSleeping() || !this.isAlive()) {
            return Either.left(Player.BedSleepingProblem.OTHER_PROBLEM);
        } else if (!this.level().dimensionType().natural()) {
            return Either.left(Player.BedSleepingProblem.NOT_POSSIBLE_HERE);
        } else if (!this.bedInRange(p_9115_, direction)) {
            return Either.left(Player.BedSleepingProblem.TOO_FAR_AWAY);
        } else if (this.bedBlocked(p_9115_, direction)) {
            return Either.left(Player.BedSleepingProblem.OBSTRUCTED);
        } else {
            this.setRespawnPosition(this.level().dimension(), p_9115_, this.getYRot(), false, true);
            if (this.level().isDay()) {
                return Either.left(Player.BedSleepingProblem.NOT_POSSIBLE_NOW);
            } else {
                if (!this.isCreative()) {
                    double d0 = 8.0;
                    double d1 = 5.0;
                    Vec3 vec3 = Vec3.atBottomCenterOf(p_9115_);
                    List<Monster> list = this.level()
                        .getEntitiesOfClass(
                            Monster.class,
                            new AABB(vec3.x() - 8.0, vec3.y() - 5.0, vec3.z() - 8.0, vec3.x() + 8.0, vec3.y() + 5.0, vec3.z() + 8.0),
                            p_9062_ -> p_9062_.isPreventingPlayerRest(this)
                        );
                    if (!list.isEmpty()) {
                        return Either.left(Player.BedSleepingProblem.NOT_SAFE);
                    }
                }
        // End vanilla code
            }
        }
        return Either.right(Unit.INSTANCE);
        }).get();

        // Fire the event. Return the error if one exists after the event, otherwise use the vanilla logic to start sleeping.
        vanillaResult = net.neoforged.neoforge.event.EventHooks.canPlayerStartSleeping(this, p_9115_, vanillaResult);
        if (vanillaResult.left().isPresent()) {
            return vanillaResult;
        }

        {
            {
                // Start vanilla code
                Either<Player.BedSleepingProblem, Unit> either = super.startSleepInBed(p_9115_).ifRight(p_9029_ -> {
                    this.awardStat(Stats.SLEEP_IN_BED);
                    CriteriaTriggers.SLEPT_IN_BED.trigger(this);
                });
                if (!this.serverLevel().canSleepThroughNights()) {
                    this.displayClientMessage(Component.translatable("sleep.not_possible"), true);
                }

                ((ServerLevel)this.level()).updateSleepingPlayerList();
                return either;
            }
        }
    }

    @Override
    public void startSleeping(BlockPos p_9190_) {
        this.resetStat(Stats.CUSTOM.get(Stats.TIME_SINCE_REST));
        super.startSleeping(p_9190_);
    }

    private boolean bedInRange(BlockPos p_9117_, Direction p_9118_) {
        if (p_9118_ == null) return false;
        return this.isReachableBedBlock(p_9117_) || this.isReachableBedBlock(p_9117_.relative(p_9118_.getOpposite()));
    }

    private boolean isReachableBedBlock(BlockPos p_9223_) {
        Vec3 vec3 = Vec3.atBottomCenterOf(p_9223_);
        return Math.abs(this.getX() - vec3.x()) <= 3.0 && Math.abs(this.getY() - vec3.y()) <= 2.0 && Math.abs(this.getZ() - vec3.z()) <= 3.0;
    }

    private boolean bedBlocked(BlockPos p_9192_, Direction p_9193_) {
        BlockPos blockpos = p_9192_.above();
        return !this.freeAt(blockpos) || !this.freeAt(blockpos.relative(p_9193_.getOpposite()));
    }

    @Override
    public void stopSleepInBed(boolean p_9165_, boolean p_9166_) {
        if (!this.isSleeping()) return; // CraftBukkit - Can't leave bed if not in one!
        // CraftBukkit start - fire PlayerBedLeaveEvent
        CraftPlayer player = this.getBukkitEntity();
        BlockPos bedPosition = this.getSleepingPos().orElse(null);
        org.bukkit.block.Block bed;
        if (bedPosition != null) {
            bed = this.level().getWorld().getBlockAt(bedPosition.getX(), bedPosition.getY(), bedPosition.getZ());
        } else {
            bed = this.level().getWorld().getBlockAt(player.getLocation());
        }
        PlayerBedLeaveEvent event = new PlayerBedLeaveEvent(player, bed, true);
        this.level().getCraftServer().getPluginManager().callEvent(event);
        if (event.isCancelled()) {
            return;
        }
        // CraftBukkit end
        if (this.isSleeping()) {
            this.serverLevel().getChunkSource().broadcastAndSend(this, new ClientboundAnimatePacket(this, 2));
        }

        super.stopSleepInBed(p_9165_, p_9166_);
        if (this.connection != null) {
            this.connection.teleport(this.getX(), this.getY(), this.getZ(), this.getYRot(), this.getXRot());
        }
    }

    @Override
    public void dismountTo(double p_143389_, double p_143390_, double p_143391_) {
        this.removeVehicle();
        this.setPos(p_143389_, p_143390_, p_143391_);
    }

    @Override
    public boolean isInvulnerableTo(DamageSource p_9182_) {
        return super.isInvulnerableTo(p_9182_) || this.isChangingDimension() || !this.level().paperConfig().collisions.allowPlayerCrammingDamage && p_9182_ == damageSources().cramming(); // Paper - disable player cramming
    }

    @Override
    protected void checkFallDamage(double p_8976_, boolean p_8977_, BlockState p_8978_, BlockPos p_8979_) {
    }

    @Override
    protected void onChangedBlock(ServerLevel p_346052_, BlockPos p_9206_) {
        if (!this.isSpectator()) {
            super.onChangedBlock(p_346052_, p_9206_);
        }
    }

    public void doCheckFallDamage(double p_289676_, double p_289671_, double p_289665_, boolean p_289696_) {
        if (!this.touchingUnloadedChunk()) {
            this.checkSupportingBlock(p_289696_, new Vec3(p_289676_, p_289671_, p_289665_));
            BlockPos blockpos = this.getOnPosLegacy();
            BlockState blockstate = this.level().getBlockState(blockpos);
            if (this.spawnExtraParticlesOnFall && p_289696_ && this.fallDistance > 0.0F) {
                Vec3 vec3 = blockpos.getCenter().add(0.0, 0.5, 0.0);
                int i = (int)Mth.clamp(50.0F * this.fallDistance, 0.0F, 200.0F);
                this.serverLevel().sendParticles(new BlockParticleOption(ParticleTypes.BLOCK, blockstate), vec3.x, vec3.y, vec3.z, i, 0.3F, 0.3F, 0.3F, 0.15F);
                this.spawnExtraParticlesOnFall = false;
            }

            super.checkFallDamage(p_289671_, p_289696_, blockstate, blockpos);
        }
    }

    @Override
    public void onExplosionHit(@Nullable Entity p_326351_) {
        super.onExplosionHit(p_326351_);
        this.currentImpulseImpactPos = this.position();
        this.currentExplosionCause = p_326351_;
        this.setIgnoreFallDamageFromCurrentImpulse(p_326351_ != null && p_326351_.getType() == EntityType.WIND_CHARGE);
    }

    @Override
    protected void pushEntities() {
        if (this.level().tickRateManager().runsNormally()) {
            super.pushEntities();
        }
    }

    @Override
    public void openTextEdit(SignBlockEntity p_277909_, boolean p_277495_) {
        this.connection.send(new ClientboundBlockUpdatePacket(this.level(), p_277909_.getBlockPos()));
        this.connection.send(new ClientboundOpenSignEditorPacket(p_277909_.getBlockPos(), p_277495_));
    }

    public void nextContainerCounter() {
        this.containerCounter = this.containerCounter % 100 + 1;
    }

    public int nextContainerCounterInt() {
        this.containerCounter = this.containerCounter % 100 + 1;
        return containerCounter; // CraftBukkit
    }

    @Override
    public OptionalInt openMenu(@Nullable MenuProvider p_9033_) {
        return openMenu(p_9033_, (java.util.function.Consumer<net.minecraft.network.RegistryFriendlyByteBuf>) null);
    }

    @Override
    public OptionalInt openMenu(@Nullable MenuProvider p_9033_, @Nullable java.util.function.Consumer<net.minecraft.network.RegistryFriendlyByteBuf> extraDataWriter) {
        if (p_9033_ == null) {
            return OptionalInt.empty();
        } else {
            if (this.containerMenu != this.inventoryMenu) {
                InventoryOwner.setClose$Reason(org.bukkit.event.inventory.InventoryCloseEvent.Reason.OPEN_NEW); // Paper - Inventory close reason
                if (p_9033_.shouldTriggerClientSideContainerClosingOnOpen())
                    this.closeContainer();
                else
                    this.doCloseContainer();
                CraftEventFactory.alreadyProcessed = true;
            }

            this.nextContainerCounter();
            AbstractContainerMenu abstractcontainermenu = p_9033_.createMenu(this.containerCounter, this.getInventory(), this);

            if (abstractcontainermenu == null) {
                if (this.isSpectator()) {
                    this.displayClientMessage(Component.translatable("container.spectatorCantOpen").withStyle(ChatFormatting.RED), true);
                }

                return OptionalInt.empty();
            } else {
                // Neo: Support sending additional arbitrary data to menu factories on the client-side
                AbstractContainerMenu finalAbstractcontainermenu = abstractcontainermenu; // Youer
                var extraData = FriendlyByteBufUtil.writeCustomData(
                        buffer -> {
                            p_9033_.writeClientSideData(finalAbstractcontainermenu, buffer);
                            if (extraDataWriter != null) {
                                extraDataWriter.accept(buffer);
                            }
                        },
                        registryAccess()
                );
                if (extraData.length != 0) {
                    if (ClientboundOpenScreenEvent.getHandlerList().getRegisteredListeners().length > 0) {
                        ClientboundOpenScreenEvent clientboundOpenScreenEvent = new ClientboundOpenScreenEvent(this.getBukkitEntity(), abstractcontainermenu);
                        Bukkit.getPluginManager().callEvent(clientboundOpenScreenEvent);
                        if (clientboundOpenScreenEvent.isCancelled()) {
                            return OptionalInt.empty();
                        }
                    }
                    this.connection.send(
                        new AdvancedOpenScreenPayload(
                                abstractcontainermenu.containerId,
                                abstractcontainermenu.getType(),
                            p_9033_.getDisplayName(),
                            extraData
                        )
                    );
                } else {
                    if (!this.isImmobile()) {
                        Component title = null; // Paper - Add titleOverride to InventoryOpenEvent
                        // CraftBukkit start - Inventory open hook
                        abstractcontainermenu.setTitle(p_9033_.getDisplayName());
                        boolean cancelled = false;
                        abstractcontainermenu.containerOwner = this;
                        // Paper start - Add titleOverride to InventoryOpenEvent
                        final com.mojang.datafixers.util.Pair<net.kyori.adventure.text.Component, AbstractContainerMenu> result = CraftEventFactory.callInventoryOpenEventWithTitle(this, abstractcontainermenu, cancelled);
                        abstractcontainermenu = result.getSecond();
                        title = PaperAdventure.asVanilla(result.getFirst());
                        // Paper end - Add titleOverride to InventoryOpenEvent
                        if (abstractcontainermenu == null && !cancelled) { // Let pre-cancelled events fall through
                            // SPIGOT-5263 - close chest if cancelled
                            if (p_9033_ instanceof Container) {
                                ((Container) p_9033_).stopOpen(this);
                            } else if (p_9033_ instanceof ChestBlock.DoubleInventory) {
                                // SPIGOT-5355 - double chests too :(
                                ((ChestBlock.DoubleInventory) p_9033_).inventorylargechest.stopOpen(this);
                                // Paper start - Fix InventoryOpenEvent cancellation
                            } else if (!this.enderChestInventory.isActiveChest(null)) {
                                this.enderChestInventory.stopOpen(this);
                                // Paper end - Fix InventoryOpenEvent cancellation
                            }
                            return OptionalInt.empty();
                        }

                        // CraftBukkit end
                        this.connection
                                .send(new ClientboundOpenScreenPacket(abstractcontainermenu.containerId, abstractcontainermenu.getType(), Objects.requireNonNullElseGet(title, abstractcontainermenu::getTitle))); // Paper - Add titleOverride to InventoryOpenEvent
                    }
                }
                this.initMenu(abstractcontainermenu);
                this.containerMenu = abstractcontainermenu;
                NeoForge.EVENT_BUS.post(new PlayerContainerEvent.Open(this, this.containerMenu));
                return OptionalInt.of(this.containerCounter);
            }
        }
    }

    @Override
    public void sendMerchantOffers(int p_8988_, MerchantOffers p_8989_, int p_8990_, int p_8991_, boolean p_8992_, boolean p_8993_) {
        this.connection.send(new ClientboundMerchantOffersPacket(p_8988_, p_8989_, p_8990_, p_8991_, p_8992_, p_8993_));
    }

    @Override
    public void openHorseInventory(AbstractHorse p_9059_, Container p_9060_) {
        // CraftBukkit start - Inventory open hook
        this.nextContainerCounter();
        AbstractContainerMenu container = new HorseInventoryMenu(this.containerCounter, this.getInventory(), p_9060_, p_9059_, p_9059_.getInventoryColumns());
        container.setTitle(p_9059_.getDisplayName());
        container = CraftEventFactory.callInventoryOpenEvent(this, container);
        if (container == null) {
            p_9060_.stopOpen(this);
            return;
        }
        // CraftBukkit end
        if (this.containerMenu != this.inventoryMenu) {
            this.closeContainer();
        }

        // this.nextContainerCounter(); // CraftBukkit - moved up
        int i = p_9059_.getInventoryColumns();
        this.connection.send(new ClientboundHorseScreenOpenPacket(this.containerCounter, i, p_9059_.getId()));
        this.containerMenu = container; // CraftBukkit
        this.initMenu(this.containerMenu);
        NeoForge.EVENT_BUS.post(new PlayerContainerEvent.Open(this, this.containerMenu));
    }

    @Override
    public void openItemGui(ItemStack p_9082_, InteractionHand p_9083_) {
        if (p_9082_.is(Items.WRITTEN_BOOK)) {
            if (WrittenBookItem.resolveBookComponents(p_9082_, this.createCommandSourceStack(), this)) {
                this.containerMenu.broadcastChanges();
            }

            this.connection.send(new ClientboundOpenBookPacket(p_9083_));
        }
    }

    @Override
    public void openCommandBlock(CommandBlockEntity p_9099_) {
        this.connection.send(ClientboundBlockEntityDataPacket.create(p_9099_, BlockEntity::saveCustomOnly));
    }

    @Override
    public void closeContainer() {
        this.connection.send(new ClientboundContainerClosePacket(this.containerMenu.containerId));
        this.doCloseContainer();
    }

    @Override
    public void closeContainer(org.bukkit.event.inventory.InventoryCloseEvent.Reason reason) {
        InventoryOwner.setClose$Reason(reason);
        closeContainer();
    }

    // Paper start - special close for unloaded inventory
    @Override
    public void closeUnloadedInventory(org.bukkit.event.inventory.InventoryCloseEvent.Reason reason) {
        // copied from above
        CraftEventFactory.handleInventoryCloseEvent(this, reason); // CraftBukkit
        // Paper end
        // copied from below
        this.connection.send(new ClientboundContainerClosePacket(this.containerMenu.containerId));
        this.containerMenu = this.inventoryMenu;
        // do not run close logic
    }
    // Paper end - special close for unloaded inventory

    @Override
    public void doCloseContainer() {
        this.containerMenu.removed(this);
        this.inventoryMenu.transferState(this.containerMenu);
        NeoForge.EVENT_BUS.post(new PlayerContainerEvent.Close(this, this.containerMenu, InventoryOwner.getClose$Reason()));
        this.containerMenu = this.inventoryMenu;
    }

    public void setPlayerInput(float p_8981_, float p_8982_, boolean p_8983_, boolean p_8984_) {
        if (this.isPassenger()) {
            if (p_8981_ >= -1.0F && p_8981_ <= 1.0F) {
                this.xxa = p_8981_;
            }

            if (p_8982_ >= -1.0F && p_8982_ <= 1.0F) {
                this.zza = p_8982_;
            }

            this.jumping = p_8983_;
            // CraftBukkit start
            if (p_8984_ != this.isShiftKeyDown()) {
                PlayerToggleSneakEvent event = new PlayerToggleSneakEvent(this.getBukkitEntity(), p_8984_);
                this.server.server.getPluginManager().callEvent(event);
                if (event.isCancelled()) {
                    return;
                }
            }
            // CraftBukkit end
            this.setShiftKeyDown(p_8984_);
        }
    }

    @Override
    public void travel(Vec3 p_308985_) {
        double d0 = this.getX();
        double d1 = this.getY();
        double d2 = this.getZ();
        super.travel(p_308985_);
        this.checkMovementStatistics(this.getX() - d0, this.getY() - d1, this.getZ() - d2);
    }

    @Override
    public void rideTick() {
        double d0 = this.getX();
        double d1 = this.getY();
        double d2 = this.getZ();
        super.rideTick();
        this.checkRidingStatistics(this.getX() - d0, this.getY() - d1, this.getZ() - d2);
    }

    public void checkMovementStatistics(double p_308996_, double p_309062_, double p_309170_) {
        if (!this.isPassenger() && !didNotMove(p_308996_, p_309062_, p_309170_)) {
            if (this.isSwimming()) {
                int i = Math.round((float)Math.sqrt(p_308996_ * p_308996_ + p_309062_ * p_309062_ + p_309170_ * p_309170_) * 100.0F);
                if (i > 0) {
                    this.awardStat(Stats.SWIM_ONE_CM, i);
                    this.causeFoodExhaustion(this.level().spigotConfig.swimMultiplier * (float)i * 0.01F, EntityExhaustionEvent.ExhaustionReason.SWIM); // CraftBukkit - EntityExhaustionEvent // Spigot
                }
            } else if (this.isEyeInFluid(FluidTags.WATER)) {
                int j = Math.round((float)Math.sqrt(p_308996_ * p_308996_ + p_309062_ * p_309062_ + p_309170_ * p_309170_) * 100.0F);
                if (j > 0) {
                    this.awardStat(Stats.WALK_UNDER_WATER_ONE_CM, j);
                    this.causeFoodExhaustion(this.level().spigotConfig.swimMultiplier * (float)j * 0.01F, EntityExhaustionEvent.ExhaustionReason.WALK_UNDERWATER); // CraftBukkit - EntityExhaustionEvent // Spigot
                }
            } else if (this.isInWater()) {
                int k = Math.round((float)Math.sqrt(p_308996_ * p_308996_ + p_309170_ * p_309170_) * 100.0F);
                if (k > 0) {
                    this.awardStat(Stats.WALK_ON_WATER_ONE_CM, k);
                    this.causeFoodExhaustion(this.level().spigotConfig.swimMultiplier * (float)k * 0.01F, EntityExhaustionEvent.ExhaustionReason.WALK_ON_WATER); // CraftBukkit - EntityExhaustionEvent // Spigot
                }
            } else if (this.onClimbable()) {
                if (p_309062_ > 0.0) {
                    this.awardStat(Stats.CLIMB_ONE_CM, (int)Math.round(p_309062_ * 100.0));
                }
            } else if (this.onGround()) {
                int l = Math.round((float)Math.sqrt(p_308996_ * p_308996_ + p_309170_ * p_309170_) * 100.0F);
                if (l > 0) {
                    if (this.isSprinting()) {
                        this.awardStat(Stats.SPRINT_ONE_CM, l);
                        this.causeFoodExhaustion(this.level().spigotConfig.sprintMultiplier * (float)l * 0.01F, EntityExhaustionEvent.ExhaustionReason.SPRINT); // CraftBukkit - EntityExhaustionEvent // Spigot
                    } else if (this.isCrouching()) {
                        this.awardStat(Stats.CROUCH_ONE_CM, l);
                        this.causeFoodExhaustion(this.level().spigotConfig.otherMultiplier * (float)l * 0.01F, EntityExhaustionEvent.ExhaustionReason.CROUCH); // CraftBukkit - EntityExhaustionEvent // Spigot
                    } else {
                        this.awardStat(Stats.WALK_ONE_CM, l);
                        this.causeFoodExhaustion(this.level().spigotConfig.otherMultiplier * (float)l * 0.01F, EntityExhaustionEvent.ExhaustionReason.WALK); // CraftBukkit - EntityExhaustionEvent // Spigot
                    }
                }
            } else if (this.isFallFlying()) {
                int i1 = Math.round((float)Math.sqrt(p_308996_ * p_308996_ + p_309062_ * p_309062_ + p_309170_ * p_309170_) * 100.0F);
                this.awardStat(Stats.AVIATE_ONE_CM, i1);
            } else {
                int j1 = Math.round((float)Math.sqrt(p_308996_ * p_308996_ + p_309170_ * p_309170_) * 100.0F);
                if (j1 > 25) {
                    this.awardStat(Stats.FLY_ONE_CM, j1);
                }
            }
        }
    }

    public void checkRidingStatistics(double p_308888_, double p_309131_, double p_308893_) {
        if (this.isPassenger() && !didNotMove(p_308888_, p_309131_, p_308893_)) {
            int i = Math.round((float)Math.sqrt(p_308888_ * p_308888_ + p_309131_ * p_309131_ + p_308893_ * p_308893_) * 100.0F);
            Entity entity = this.getVehicle();
            if (entity instanceof AbstractMinecart) {
                this.awardStat(Stats.MINECART_ONE_CM, i);
            } else if (entity instanceof Boat) {
                this.awardStat(Stats.BOAT_ONE_CM, i);
            } else if (entity instanceof Pig) {
                this.awardStat(Stats.PIG_ONE_CM, i);
            } else if (entity instanceof AbstractHorse) {
                this.awardStat(Stats.HORSE_ONE_CM, i);
            } else if (entity instanceof Strider) {
                this.awardStat(Stats.STRIDER_ONE_CM, i);
            }
        }
    }

    private static boolean didNotMove(double p_309023_, double p_309067_, double p_309143_) {
        return p_309023_ == 0.0 && p_309067_ == 0.0 && p_309143_ == 0.0;
    }

    @Override
    public void awardStat(Stat<?> p_9026_, int p_9027_) {
        this.stats.increment(this, p_9026_, p_9027_);
        this.getScoreboard().forAllObjectives(p_9026_, this, p_313587_ -> p_313587_.add(p_9027_));
    }

    @Override
    public void resetStat(Stat<?> p_9024_) {
        this.stats.setValue(this, p_9024_, 0);
        this.getScoreboard().forAllObjectives(p_9024_, this, ScoreAccess::reset);
    }

    @Override
    public int awardRecipes(Collection<RecipeHolder<?>> p_9129_) {
        return this.recipeBook.addRecipes(p_9129_, this);
    }

    @Override
    public void triggerRecipeCrafted(RecipeHolder<?> p_301156_, List<ItemStack> p_282336_) {
        CriteriaTriggers.RECIPE_CRAFTED.trigger(this, p_301156_.id(), p_282336_);
    }

    @Override
    public void awardRecipesByKey(List<ResourceLocation> p_312811_) {
        List<RecipeHolder<?>> list = p_312811_.stream()
            .flatMap(p_311549_ -> this.server.getRecipeManager().byKey(p_311549_).stream())
            .collect(Collectors.toList());
        this.awardRecipes(list);
    }

    @Override
    public int resetRecipes(Collection<RecipeHolder<?>> p_9195_) {
        return this.recipeBook.removeRecipes(p_9195_, this);
    }

    @Override
    public void giveExperiencePoints(int p_9208_) {
        super.giveExperiencePoints(p_9208_);
        this.lastSentExp = -1;
    }

    public void disconnect() {
        this.disconnected = true;
        this.ejectPassengers();

        // Paper start - Workaround vehicle not tracking the passenger disconnection dismount
        if (this.isPassenger() && this.getVehicle() instanceof ServerPlayer) {
            this.stopRiding();
        }
        // Paper end - Workaround vehicle not tracking the passenger disconnection dismount

        if (this.isSleeping()) {
            this.stopSleepInBed(true, false);
        }
    }

    public boolean hasDisconnected() {
        return this.disconnected;
    }

    public void resetSentInfo() {
        this.lastSentHealth = -1.0E8F;
        this.lastSentExp = -1; // CraftBukkit - Added to reset
    }

    // Purpur start
    public void sendActionBarMessage(@Nullable String message) {
        if (message != null && !message.isEmpty()) {
            sendActionBarMessage(net.kyori.adventure.text.minimessage.MiniMessage.miniMessage().deserialize(message));
        }
    }

    public void sendActionBarMessage(@Nullable net.kyori.adventure.text.Component message) {
        if (message != null) {
            sendActionBarMessage(io.papermc.paper.adventure.PaperAdventure.asVanilla(message));
        }
    }

    public void sendActionBarMessage(@Nullable Component message) {
        if (message != null) {
            displayClientMessage(message, true);
        }
    }
    // Purpur end

    @Override
    public void displayClientMessage(Component p_9154_, boolean p_9155_) {
        this.sendSystemMessage(p_9154_, p_9155_);
    }

    @Override
    public void completeUsingItem() {
        if (!this.useItem.isEmpty() && this.isUsingItem()) {
            this.connection.send(new ClientboundEntityEventPacket(this, (byte)9));
            super.completeUsingItem();
        }
    }

    @Override
    public void lookAt(EntityAnchorArgument.Anchor p_9112_, Vec3 p_9113_) {
        super.lookAt(p_9112_, p_9113_);
        this.connection.send(new ClientboundPlayerLookAtPacket(p_9112_, p_9113_.x, p_9113_.y, p_9113_.z));
    }

    public void lookAt(EntityAnchorArgument.Anchor p_9108_, Entity p_9109_, EntityAnchorArgument.Anchor p_9110_) {
        Vec3 vec3 = p_9110_.apply(p_9109_);
        super.lookAt(p_9108_, vec3);
        this.connection.send(new ClientboundPlayerLookAtPacket(p_9108_, p_9109_, p_9110_));
    }

    public void restoreFrom(ServerPlayer p_9016_, boolean p_9017_) {
        p_9016_.getBukkitEntity().setHandle(this);
        this.transferCookieConnection = p_9016_.transferCookieConnection;
        this.setBukkitEntity(p_9016_.getBukkitEntity());
        this.isRealPlayer = p_9016_.isRealPlayer;
        this.wardenSpawnTracker = p_9016_.wardenSpawnTracker;
        this.chatSession = p_9016_.chatSession;
        this.gameMode.setGameModeForPlayer(p_9016_.gameMode.getGameModeForPlayer(), p_9016_.gameMode.getPreviousGameModeForPlayer());
        this.onUpdateAbilities();
        this.getAttributes().assignBaseValues(p_9016_.getAttributes());
        this.setHealth(this.getMaxHealth()); // CraftBukkit
        if (p_9017_) {
            this.getInventory().replaceWith(p_9016_.getInventory());
            this.setHealth(p_9016_.getHealth());
            this.foodData = p_9016_.foodData;

            for (MobEffectInstance mobeffectinstance : p_9016_.getActiveEffects()) {
                this.addEffect(new MobEffectInstance(mobeffectinstance));
            }

            this.experienceLevel = p_9016_.experienceLevel;
            this.totalExperience = p_9016_.totalExperience;
            this.experienceProgress = p_9016_.experienceProgress;
            this.setScore(p_9016_.getScore());
            this.portalProcess = p_9016_.portalProcess;
        } else if (KeepInventory.inventory(p_9016_) || p_9016_.isSpectator()) {
            this.getInventory().replaceWith(p_9016_.getInventory());
            if (KeepInventory.exp(p_9016_) || this.keepLevel) {
                this.experienceLevel = p_9016_.experienceLevel;
                this.totalExperience = p_9016_.totalExperience;
                this.experienceProgress = p_9016_.experienceProgress;
            }
            this.setScore(p_9016_.getScore());
        }

        this.enchantmentSeed = p_9016_.enchantmentSeed;
        this.enderChestInventory = p_9016_.enderChestInventory;
        this.getEntityData().set(DATA_PLAYER_MODE_CUSTOMISATION, p_9016_.getEntityData().get(DATA_PLAYER_MODE_CUSTOMISATION));
        this.lastSentExp = -1;
        this.lastSentHealth = -1.0F;
        this.lastSentFood = -1;
        // this.recipeBook.copyOverData(p_9016_.recipeBook); // CreatBukkit
        this.seenCredits = p_9016_.seenCredits;
        this.enteredNetherPosition = p_9016_.enteredNetherPosition;
        this.chunkTrackingView = p_9016_.chunkTrackingView;
        this.setShoulderEntityLeft(p_9016_.getShoulderEntityLeft());
        this.setShoulderEntityRight(p_9016_.getShoulderEntityRight());
        this.setLastDeathLocation(p_9016_.getLastDeathLocation());

        //Copy over a section of the Entity Data from the old player.
        //Allows mods to specify data that persists after players respawn.
        CompoundTag old = p_9016_.getPersistentData();
        if (old.contains(PERSISTED_NBT_TAG))
             getPersistentData().put(PERSISTED_NBT_TAG, old.get(PERSISTED_NBT_TAG));
        EventHooks.onPlayerClone(this, p_9016_, !p_9017_);
        this.tabListHeader = p_9016_.tabListHeader;
        this.tabListFooter = p_9016_.tabListFooter;
    }

    @Override
    protected void onEffectAdded(MobEffectInstance p_143393_, @Nullable Entity p_143394_) {
        super.onEffectAdded(p_143393_, p_143394_);
        this.connection.send(new ClientboundUpdateMobEffectPacket(this.getId(), p_143393_, true));
        if (p_143393_.is(MobEffects.LEVITATION)) {
            this.levitationStartTime = this.tickCount;
            this.levitationStartPos = this.position();
        }

        CriteriaTriggers.EFFECTS_CHANGED.trigger(this, p_143394_);
    }

    @Override
    protected void onEffectUpdated(MobEffectInstance p_143396_, boolean p_143397_, @Nullable Entity p_143398_) {
        super.onEffectUpdated(p_143396_, p_143397_, p_143398_);
        this.connection.send(new ClientboundUpdateMobEffectPacket(this.getId(), p_143396_, false));
        CriteriaTriggers.EFFECTS_CHANGED.trigger(this, p_143398_);
    }

    @Override
    protected void onEffectRemoved(MobEffectInstance p_9184_) {
        super.onEffectRemoved(p_9184_);
        this.connection.send(new ClientboundRemoveMobEffectPacket(this.getId(), p_9184_.getEffect()));
        if (p_9184_.is(MobEffects.LEVITATION)) {
            this.levitationStartPos = null;
        }

        CriteriaTriggers.EFFECTS_CHANGED.trigger(this, null);
    }

    @Override
    public void teleportTo(double p_8969_, double p_8970_, double p_8971_) {
        this.connection.teleport(p_8969_, p_8970_, p_8971_, this.getYRot(), this.getXRot(), RelativeMovement.ROTATION);
    }

    @Override
    public void teleportRelative(double p_251611_, double p_248861_, double p_252266_) {
        this.connection
            .teleport(this.getX() + p_251611_, this.getY() + p_248861_, this.getZ() + p_252266_, this.getYRot(), this.getXRot(), RelativeMovement.ALL);
    }

    // Youer start
    public AtomicReference<PlayerTeleportEvent.TeleportCause> teleportToS$cause = new AtomicReference<>(PlayerTeleportEvent.TeleportCause.UNKNOWN);

    @Override
    public boolean teleportTo(
        ServerLevel p_265564_, double p_265424_, double p_265680_, double p_265312_, Set<RelativeMovement> p_265192_, float p_265059_, float p_265266_
    ) {
        ChunkPos chunkpos = new ChunkPos(BlockPos.containing(p_265424_, p_265680_, p_265312_));
        p_265564_.getChunkSource().addRegionTicket(TicketType.POST_TELEPORT, chunkpos, 1, this.getId());
        this.stopRiding();
        if (this.isSleeping()) {
            this.stopSleepInBed(true, true);
        }

        if (p_265564_ == this.level()) {
            this.connection.teleport$cause(teleportToS$cause.getAndSet(PlayerTeleportEvent.TeleportCause.UNKNOWN));
            this.connection.teleport(p_265424_, p_265680_, p_265312_, p_265059_, p_265266_, p_265192_);
        } else {
            this.teleportTo$cause.set(teleportToS$cause.getAndSet(PlayerTeleportEvent.TeleportCause.UNKNOWN));
            this.teleportTo(p_265564_, p_265424_, p_265680_, p_265312_, p_265059_, p_265266_);
        }

        this.setYHeadRot(p_265059_);
        return true;
    }

    @Override
    public boolean teleportTo(
            ServerLevel p_265564_, double p_265424_, double p_265680_, double p_265312_, Set<RelativeMovement> p_265192_, float p_265059_, float p_265266_, PlayerTeleportEvent.TeleportCause cause
    ) {
        teleportToS$cause.set(cause);
        return teleportTo(p_265564_, p_265424_, p_265680_, p_265312_, p_265192_, p_265059_, p_265266_);
    }

    @Override
    public void moveTo(double p_9171_, double p_9172_, double p_9173_) {
        super.moveTo(p_9171_, p_9172_, p_9173_);
        this.connection.resetPosition();
    }

    @Override
    public void crit(Entity p_9045_) {
        this.serverLevel().getChunkSource().broadcastAndSend(this, new ClientboundAnimatePacket(p_9045_, 4));
    }

    @Override
    public void magicCrit(Entity p_9186_) {
        this.serverLevel().getChunkSource().broadcastAndSend(this, new ClientboundAnimatePacket(p_9186_, 5));
    }

    @Override
    public void onUpdateAbilities() {
        if (this.connection != null) {
            this.connection.send(new ClientboundPlayerAbilitiesPacket(this.getAbilities()));
            this.updateInvisibilityStatus();
        }
    }

    public ServerLevel serverLevel() {
        return (ServerLevel)this.level();
    }

    public boolean setGameMode(GameType p_143404_) {
        // Paper start - Expand PlayerGameModeChangeEvent
        org.bukkit.event.player.PlayerGameModeChangeEvent event = this.setGameMode(p_143404_, org.bukkit.event.player.PlayerGameModeChangeEvent.Cause.UNKNOWN, null);
        return event == null ? false : event.isCancelled();
    }
    @Nullable
    public org.bukkit.event.player.PlayerGameModeChangeEvent setGameMode(GameType p_143404_, org.bukkit.event.player.PlayerGameModeChangeEvent.Cause cause, @Nullable net.kyori.adventure.text.Component message) {
        boolean flag = this.isSpectator();

        org.bukkit.event.player.PlayerGameModeChangeEvent event = this.gameMode.changeGameModeForPlayer(p_143404_, cause, message);
        if (event == null || event.isCancelled()) {
            return null;
            // Paper end - Expand PlayerGameModeChangeEvent
        } else {
            this.connection.send(new ClientboundGameEventPacket(ClientboundGameEventPacket.CHANGE_GAME_MODE, (float)p_143404_.getId()));
            if (p_143404_ == GameType.SPECTATOR) {
                this.removeEntitiesOnShoulder();
                this.stopRiding();
                EnchantmentHelper.stopLocationBasedEffects(this);
            } else {
                this.setCamera(this);
                if (flag) {
                    EnchantmentHelper.runLocationChangedEffects(this.serverLevel(), this);
                }
            }

            this.onUpdateAbilities();
            this.updateEffectVisibility();
            return event; // Paper - Expand PlayerGameModeChangeEvent
        }
    }

    @Override
    public boolean isSpectator() {
        return this.gameMode.getGameModeForPlayer() == GameType.SPECTATOR;
    }

    @Override
    public boolean isCreative() {
        return this.gameMode.getGameModeForPlayer() == GameType.CREATIVE;
    }

    @Override
    public void sendSystemMessage(Component p_215097_) {
        this.sendSystemMessage(p_215097_, false);
    }

    public void sendSystemMessage(Component p_240560_, boolean p_240545_) {
        if (this.acceptsSystemMessages(p_240545_)) {
            this.connection
                .send(
                    new ClientboundSystemChatPacket(p_240560_, p_240545_),
                    PacketSendListener.exceptionallySend(
                        () -> {
                            if (this.acceptsSystemMessages(false)) {
                                int i = 256;
                                String s = p_240560_.getString(256);
                                Component component = Component.literal(s).withStyle(ChatFormatting.YELLOW);
                                return new ClientboundSystemChatPacket(
                                    Component.translatable("multiplayer.message_not_delivered", component).withStyle(ChatFormatting.RED), false
                                );
                            } else {
                                return null;
                            }
                        }
                    )
                );
        }
    }

    public void sendChatMessage(OutgoingChatMessage p_249852_, boolean p_250110_, ChatType.Bound p_252108_) {
        // Paper start
        this.sendChatMessage(p_249852_, p_250110_, p_252108_, null);
    }
    public void sendChatMessage(OutgoingChatMessage message, boolean filterMaskEnabled, ChatType.Bound params, @Nullable Component unsigned) {
        // Paper end
        if (this.acceptsChatMessages()) {
            message.sendToPlayer(this, filterMaskEnabled, params, unsigned); // Paper
        }

    }

    public String getIpAddress() {
        return this.connection.getRemoteAddress() instanceof InetSocketAddress inetsocketaddress
            ? InetAddresses.toAddrString(inetsocketaddress.getAddress())
            : "<unknown>";
    }

    // Paper start - Client option API
    private java.util.Map<com.destroystokyo.paper.ClientOption<?>, ?> getClientOptionMap(String locale, int viewDistance, com.destroystokyo.paper.ClientOption.ChatVisibility chatVisibility, boolean chatColors, com.destroystokyo.paper.PaperSkinParts skinParts, org.bukkit.inventory.MainHand mainHand, boolean allowsServerListing, boolean textFilteringEnabled) {
        java.util.Map<com.destroystokyo.paper.ClientOption<?>, Object> map = new java.util.HashMap<>();
        map.put(com.destroystokyo.paper.ClientOption.LOCALE, locale);
        map.put(com.destroystokyo.paper.ClientOption.VIEW_DISTANCE, viewDistance);
        map.put(com.destroystokyo.paper.ClientOption.CHAT_VISIBILITY, chatVisibility);
        map.put(com.destroystokyo.paper.ClientOption.CHAT_COLORS_ENABLED, chatColors);
        map.put(com.destroystokyo.paper.ClientOption.SKIN_PARTS, skinParts);
        map.put(com.destroystokyo.paper.ClientOption.MAIN_HAND, mainHand);
        map.put(com.destroystokyo.paper.ClientOption.ALLOW_SERVER_LISTINGS, allowsServerListing);
        map.put(com.destroystokyo.paper.ClientOption.TEXT_FILTERING_ENABLED, textFilteringEnabled);
        return map;
    }
    // Paper end

    public void updateOptions(ClientInformation p_301998_) {
        new com.destroystokyo.paper.event.player.PlayerClientOptionsChangeEvent(getBukkitEntity(), getClientOptionMap(p_301998_.language(), p_301998_.viewDistance(), com.destroystokyo.paper.ClientOption.ChatVisibility.valueOf(p_301998_.chatVisibility().name()), p_301998_.chatColors(), new com.destroystokyo.paper.PaperSkinParts(p_301998_.modelCustomisation()), p_301998_.mainHand() == HumanoidArm.LEFT ? MainHand.LEFT : MainHand.RIGHT, p_301998_.allowsListing(), p_301998_.textFilteringEnabled())).callEvent(); // Paper - settings event
        // CraftBukkit start
        if (getMainArm() != p_301998_.mainHand()) {
            PlayerChangedMainHandEvent event = new PlayerChangedMainHandEvent(getBukkitEntity(), getMainArm() == HumanoidArm.LEFT ? MainHand.LEFT : MainHand.RIGHT);
            this.server.server.getPluginManager().callEvent(event);
        }
        if (this.language == null || !this.language.equals(p_301998_.language())) { // Paper
            PlayerLocaleChangeEvent event = new PlayerLocaleChangeEvent(getBukkitEntity(), p_301998_.language());
            this.server.server.getPluginManager().callEvent(event);
        }
        // CraftBukkit end
        this.language = p_301998_.language();
        this.adventure$locale = java.util.Objects.requireNonNullElse(net.kyori.adventure.translation.Translator.parseLocale(this.language), java.util.Locale.US); // Paper
        this.requestedViewDistance = p_301998_.viewDistance();
        this.chatVisibility = p_301998_.chatVisibility();
        this.canChatColor = p_301998_.chatColors();
        this.textFilteringEnabled = p_301998_.textFilteringEnabled();
        this.allowsListing = p_301998_.allowsListing();
        this.getEntityData().set(DATA_PLAYER_MODE_CUSTOMISATION, (byte)p_301998_.modelCustomisation());
        this.getEntityData().set(DATA_PLAYER_MAIN_HAND, (byte)p_301998_.mainHand().getId());
    }

    // Paper start - don't call options events on login
    public void updateOptionsNoEvents(ClientInformation p_301998_) {
        this.language = p_301998_.language();
        this.adventure$locale = java.util.Objects.requireNonNullElse(net.kyori.adventure.translation.Translator.parseLocale(this.language), java.util.Locale.US); // Paper
        this.requestedViewDistance = p_301998_.viewDistance();
        this.chatVisibility = p_301998_.chatVisibility();
        this.canChatColor = p_301998_.chatColors();
        this.textFilteringEnabled = p_301998_.textFilteringEnabled();
        this.allowsListing = p_301998_.allowsListing();
        this.getEntityData().set(DATA_PLAYER_MODE_CUSTOMISATION, (byte)p_301998_.modelCustomisation());
        this.getEntityData().set(DATA_PLAYER_MAIN_HAND, (byte)p_301998_.mainHand().getId());
    }
    // Paper end

    public ClientInformation clientInformation() {
        int i = this.getEntityData().get(DATA_PLAYER_MODE_CUSTOMISATION);
        HumanoidArm humanoidarm = HumanoidArm.BY_ID.apply(this.getEntityData().get(DATA_PLAYER_MAIN_HAND));
        return new ClientInformation(
            this.language, this.requestedViewDistance, this.chatVisibility, this.canChatColor, i, humanoidarm, this.textFilteringEnabled, this.allowsListing
        );
    }

    public boolean canChatInColor() {
        return this.canChatColor;
    }

    public ChatVisiblity getChatVisibility() {
        return this.chatVisibility;
    }

    private boolean acceptsSystemMessages(boolean p_240568_) {
        return this.chatVisibility == ChatVisiblity.HIDDEN ? p_240568_ : true;
    }

    private boolean acceptsChatMessages() {
        return this.chatVisibility == ChatVisiblity.FULL;
    }

    public int requestedViewDistance() {
        return this.requestedViewDistance;
    }

    public void sendServerStatus(ServerStatus p_215110_) {
        this.connection.send(new ClientboundServerDataPacket(p_215110_.description(), p_215110_.favicon().map(ServerStatus.Favicon::iconBytes)));
    }

    @Override
    protected int getPermissionLevel() {
        return this.server.getProfilePermissions(this.getGameProfile());
    }

    public void resetLastActionTime() {
        this.lastActionTime = Util.getMillis();
        this.setAfk(false); // Purpur
    }

    // Purpur Start
    private boolean isAfk = false;

    @Override
    public void setAfk(boolean afk) {
        if (this.isAfk == afk) {
            return;
        }

        String msg = afk ? org.purpurmc.purpur.PurpurConfig.afkBroadcastAway : org.purpurmc.purpur.PurpurConfig.afkBroadcastBack;

        org.purpurmc.purpur.event.PlayerAFKEvent event = new org.purpurmc.purpur.event.PlayerAFKEvent(this.getBukkitEntity(), afk, this.level().purpurConfig.idleTimeoutKick, msg, !Bukkit.isPrimaryThread());
        if (!event.callEvent() || event.shouldKick()) {
            return;
        }

        this.isAfk = afk;

        if (!afk) {
            resetLastActionTime();
        }

        msg = event.getBroadcastMsg();
        if (msg != null && !msg.isEmpty()) {
            String playerName = this.getGameProfile().getName();
            if (org.purpurmc.purpur.PurpurConfig.afkBroadcastUseDisplayName) {
                net.kyori.adventure.text.Component playerDisplayNameComponent = ColorAPI.adventure(this.getBukkitEntity().getDisplayName());
                playerName = net.kyori.adventure.text.serializer.plain.PlainTextComponentSerializer.plainText().serialize(playerDisplayNameComponent);
            }
            server.getPlayerList().broadcastMiniMessage(String.format(msg, playerName), false);
        }

        if (this.level().purpurConfig.idleTimeoutUpdateTabList) {
            String scoreboardName = getScoreboardName();
            String playerListName = net.kyori.adventure.text.minimessage.MiniMessage.miniMessage().serialize(getBukkitEntity().playerListName());
            String[] split = playerListName.split(scoreboardName);
            String prefix = (split.length > 0 ? split[0] : "").replace(org.purpurmc.purpur.PurpurConfig.afkTabListPrefix, "");
            String suffix = (split.length > 1 ? split[1] : "").replace(org.purpurmc.purpur.PurpurConfig.afkTabListSuffix, "");
            if (afk) {
                getBukkitEntity().setPlayerListName(org.purpurmc.purpur.PurpurConfig.afkTabListPrefix + prefix + scoreboardName + suffix + org.purpurmc.purpur.PurpurConfig.afkTabListSuffix, true);
            } else {
                getBukkitEntity().setPlayerListName(prefix + scoreboardName + suffix, true);
            }
        }

        ((ServerLevel) this.level()).updateSleepingPlayerList();
    }

    @Override
    public boolean isAfk() {
        return this.isAfk;
    }

    @Override
    public boolean canBeCollidedWith() {
        return !this.isAfk() && super.canBeCollidedWith();
    }
    // Purpur End

    public ServerStatsCounter getStats() {
        return this.stats;
    }

    public ServerRecipeBook getRecipeBook() {
        return this.recipeBook;
    }

    @Override
    protected void updateInvisibilityStatus() {
        if (this.isSpectator()) {
            this.removeEffectParticles();
            this.setInvisible(true);
        } else {
            super.updateInvisibilityStatus();
        }
    }

    public Entity getCamera() {
        return (Entity)(this.camera == null ? this : this.camera);
    }

    public void setCamera(@Nullable Entity p_9214_) {
        Entity entity = this.getCamera();
        this.camera = (Entity)(p_9214_ == null ? this : p_9214_);
        while (this.camera instanceof PartEntity<?> partEntity) this.camera = partEntity.getParent(); // Neo: fix MC-46486
        if (entity != this.camera) {
            // Paper start - Add PlayerStartSpectatingEntityEvent and PlayerStopSpectatingEntity
            if (this.camera == this) {
                com.destroystokyo.paper.event.player.PlayerStopSpectatingEntityEvent playerStopSpectatingEntityEvent = new com.destroystokyo.paper.event.player.PlayerStopSpectatingEntityEvent(this.getBukkitEntity(), entity.getBukkitEntity());
                if (!playerStopSpectatingEntityEvent.callEvent()) {
                    this.camera = entity; // rollback camera entity again
                    return;
                }
            } else {
                com.destroystokyo.paper.event.player.PlayerStartSpectatingEntityEvent playerStartSpectatingEntityEvent = new com.destroystokyo.paper.event.player.PlayerStartSpectatingEntityEvent(this.getBukkitEntity(), entity.getBukkitEntity(), p_9214_.getBukkitEntity());
                if (!playerStartSpectatingEntityEvent.callEvent()) {
                    this.camera = entity; // rollback camera entity again
                    return;
                }
            }
            // Paper end - Add PlayerStartSpectatingEntityEvent and PlayerStopSpectatingEntity
            if (this.camera.level() instanceof ServerLevel serverlevel) {
                this.teleportTo(serverlevel, this.camera.getX(), this.camera.getY(), this.camera.getZ(), Set.of(), this.getYRot(), this.getXRot());
            }

            if (p_9214_ != null) {
                this.serverLevel().getChunkSource().move(this);
            }

            this.connection.send(new ClientboundSetCameraPacket(this.camera));
            this.connection.resetPosition();
        }
    }

    @Override
    protected void processPortalCooldown() {
        if (!this.isChangingDimension) {
            super.processPortalCooldown();
        }
    }

    @Override
    public void attack(Entity p_9220_) {
        if (this.gameMode.getGameModeForPlayer() == GameType.SPECTATOR) {
            this.setCamera(p_9220_);
        } else {
            super.attack(p_9220_);
        }
    }

    public long getLastActionTime() {
        return this.lastActionTime;
    }

    @Nullable
    public Component getTabListDisplayName() {
        if (!this.hasTabListName) {
            this.tabListDisplayName = EventHooks.getPlayerTabListDisplayName(this);
            this.hasTabListName = true;
        }
        return this.tabListDisplayName;
    }

    public void setTabListDisplayName(Component tabListDisplayName) {
        this.tabListDisplayName = tabListDisplayName;
    }

    @Override
    public void swing(InteractionHand p_9031_) {
        super.swing(p_9031_);
        this.resetAttackStrengthTicker();
    }

    public boolean isChangingDimension() {
        return this.isChangingDimension;
    }

    public void hasChangedDimension() {
        this.isChangingDimension = false;
    }

    public PlayerAdvancements getAdvancements() {
        return this.advancements;
    }

    // Mohist start
    public AtomicReference<PlayerTeleportEvent.TeleportCause> teleportTo$cause = new AtomicReference<>(PlayerTeleportEvent.TeleportCause.UNKNOWN);
    // CraftBukkit start
    public void teleportTo(ServerLevel p_9000_, double p_9001_, double p_9002_, double p_9003_, float p_9004_, float p_9005_) {
        this.setCamera(this);
        this.stopRiding();
        if (p_9000_ == this.level()) {
            this.connection.teleport(p_9001_, p_9002_, p_9003_, p_9004_, p_9005_);
        } else {
            this.changeDimension(
                new DimensionTransition(p_9000_, new Vec3(p_9001_, p_9002_, p_9003_), Vec3.ZERO, p_9004_, p_9005_, DimensionTransition.DO_NOTHING)
            );
        }
    }

    @Nullable
    public BlockPos getRespawnPosition() {
        return this.respawnPosition;
    }

    public float getRespawnAngle() {
        return this.respawnAngle;
    }

    public ResourceKey<Level> getRespawnDimension() {
        return this.respawnDimension;
    }

    public boolean isRespawnForced() {
        return this.respawnForced;
    }

    public void copyRespawnPosition(ServerPlayer p_348642_) {
        this.setRespawnPosition(
            p_348642_.getRespawnDimension(), p_348642_.getRespawnPosition(), p_348642_.getRespawnAngle(), p_348642_.isRespawnForced(), false
        );
    }

    // Youer start
    private final AtomicReference<PlayerSetSpawnEvent.Cause> setRespawnPosition$setSpawnCause = new AtomicReference<>(PlayerSetSpawnEvent.Cause.UNKNOWN);
    private final AtomicBoolean setRespawnPosition$canCancel = new AtomicBoolean(true);

    public void setRespawnPosition$setSpawnCause(PlayerSetSpawnEvent.Cause cause) {
        this.setRespawnPosition$setSpawnCause.set(cause);
    }

    public boolean setRespawnPosition$canCancel() {
        return setRespawnPosition$canCancel.getAndSet(true);
    }

    public void setRespawnPosition(ResourceKey<Level> p_9159_, @Nullable BlockPos p_9160_, float p_9161_, boolean p_9162_, boolean p_9163_) {
        if (EventHooks.onPlayerSpawnSet(this, p_9160_ == null ? Level.OVERWORLD : p_9159_, p_9160_, p_9162_)) return;
        Location spawnLoc = null;
        boolean willNotify = false;
        if (p_9160_ != null) {
            boolean flag2 = p_9160_.equals(this.respawnPosition) && p_9159_.equals(this.respawnDimension);
            spawnLoc = io.papermc.paper.util.MCUtil.toLocation(this.getServer().getLevel(p_9159_), p_9160_);
            spawnLoc.setYaw(p_9161_);
            willNotify = p_9163_ && !flag2;
        }
        PlayerSetSpawnEvent.Cause cause = setRespawnPosition$setSpawnCause.getAndSet(PlayerSetSpawnEvent.Cause.UNKNOWN);
        PlayerSpawnChangeEvent dumbEvent = new PlayerSpawnChangeEvent(this.getBukkitEntity(), spawnLoc, p_9162_,
                cause == com.destroystokyo.paper.event.player.PlayerSetSpawnEvent.Cause.PLAYER_RESPAWN ? PlayerSpawnChangeEvent.Cause.RESET : PlayerSpawnChangeEvent.Cause.valueOf(cause.name()));
        dumbEvent.callEvent();

        com.destroystokyo.paper.event.player.PlayerSetSpawnEvent event = new com.destroystokyo.paper.event.player.PlayerSetSpawnEvent(this.getBukkitEntity(), cause, dumbEvent.getNewSpawn(), dumbEvent.isForced(), willNotify, willNotify ? net.kyori.adventure.text.Component.translatable("block.minecraft.set_spawn") : null);
        event.setCancelled(dumbEvent.isCancelled());
        if (!event.callEvent()) {
            setRespawnPosition$canCancel.set(false);
            return;
        }
        if (event.getLocation() != null) {
            p_9159_ = event.getLocation().getWorld() != null ? ((CraftWorld) event.getLocation().getWorld()).getHandle().dimension() : p_9159_;
            p_9160_ = io.papermc.paper.util.MCUtil.toBlockPosition(event.getLocation());
            p_9161_ = event.getLocation().getYaw();
            p_9162_ = event.isForced();
            // Paper end - Add PlayerSetSpawnEvent

            if (event.willNotifyPlayer() && event.getNotification() != null) { // Paper - Add PlayerSetSpawnEvent
                this.sendSystemMessage(PaperAdventure.asVanilla(event.getNotification())); // Paper - Add PlayerSetSpawnEvent
            }

            this.respawnPosition = p_9160_;
            this.respawnDimension = p_9159_;
            this.respawnAngle = p_9161_;
            this.respawnForced = p_9162_;
        } else {
            this.respawnPosition = null;
            this.respawnDimension = Level.OVERWORLD;
            this.respawnAngle = 0.0F;
            this.respawnForced = false;
        }
    }
    public boolean setRespawnPosition(ResourceKey<Level> p_9159_, @Nullable BlockPos p_9160_, float p_9161_, boolean p_9162_, boolean p_9163_, PlayerSpawnChangeEvent.Cause cause) {
        setRespawnPosition$setSpawnCause(cause == PlayerSpawnChangeEvent.Cause.RESET ?
                com.destroystokyo.paper.event.player.PlayerSetSpawnEvent.Cause.PLAYER_RESPAWN : com.destroystokyo.paper.event.player.PlayerSetSpawnEvent.Cause.valueOf(cause.name()));
        setRespawnPosition(p_9159_, p_9160_, p_9161_, p_9162_, p_9163_);
        return setRespawnPosition$canCancel.getAndSet(true);
    }

    public boolean setRespawnPosition(ResourceKey<Level> dimension, @Nullable BlockPos pos, float angle, boolean forced, boolean sendMessage, com.destroystokyo.paper.event.player.PlayerSetSpawnEvent.Cause cause) {
        setRespawnPosition$setSpawnCause.set( cause);
        setRespawnPosition(dimension, pos, angle, forced, sendMessage);
        return setRespawnPosition$canCancel.getAndSet(true);
    }
    // Youer end

    public SectionPos getLastSectionPos() {
        return this.lastSectionPos;
    }

    public void setLastSectionPos(SectionPos p_9120_) {
        this.lastSectionPos = p_9120_;
    }

    public ChunkTrackingView getChunkTrackingView() {
        return this.chunkTrackingView;
    }

    public void setChunkTrackingView(ChunkTrackingView p_296310_) {
        this.chunkTrackingView = p_296310_;
    }

    @Override
    public void playNotifySound(SoundEvent p_9019_, SoundSource p_9020_, float p_9021_, float p_9022_) {
        this.connection
            .send(
                new ClientboundSoundPacket(
                    BuiltInRegistries.SOUND_EVENT.wrapAsHolder(p_9019_),
                    p_9020_,
                    this.getX(),
                    this.getY(),
                    this.getZ(),
                    p_9021_,
                    p_9022_,
                    this.random.nextLong()
                )
            );
    }

    @Override
    public ItemEntity drop(ItemStack p_9085_, boolean p_9086_, boolean p_9087_) {
        ItemEntity itementity = super.drop(p_9085_, p_9086_, p_9087_);
        if (itementity == null) {
            return null;
        } else {
            if (captureDrops() != null) captureDrops().add(itementity);
            else
            this.level().addFreshEntity(itementity);
            ItemStack itemstack = itementity.getItem();
            if (p_9087_) {
                if (!itemstack.isEmpty()) {
                    this.awardStat(Stats.ITEM_DROPPED.get(itemstack.getItem()), itemstack.getCount()); // Paper - Fix PlayerDropItemEvent using wrong item
                }

                this.awardStat(Stats.DROP);
            }

            return itementity;
        }
    }

    /**
     * Returns the language last reported by the player as their local language.
     * Defaults to en_us if the value is unknown.
     */
    public String getLanguage() {
        return this.language;
    }

    private Component tabListHeader = Component.empty();
    private Component tabListFooter = Component.empty();

    public Component getTabListHeader() {
         return this.tabListHeader;
    }

    /**
     * Set the tab list header while preserving the footer.
     *
     * @param header the new header, or {@link Component#empty()} to clear
     */
    public void setTabListHeader(final Component header) {
         this.setTabListHeaderFooter(header, this.tabListFooter);
    }

    public Component getTabListFooter() {
         return this.tabListFooter;
    }

    /**
     * Set the tab list footer while preserving the header.
     *
     * @param footer the new footer, or {@link Component#empty()} to clear
     */
    public void setTabListFooter(final Component footer) {
         this.setTabListHeaderFooter(this.tabListHeader, footer);
    }

    /**
     * Set the tab list header and footer at once.
     *
     * @param header the new header, or {@link Component#empty()} to clear
     * @param footer the new footer, or {@link Component#empty()} to clear
     */
    public void setTabListHeaderFooter(final Component header, final Component footer) {
         if (Objects.equals(header, this.tabListHeader)
              && Objects.equals(footer, this.tabListFooter)) {
              return;
         }

         this.tabListHeader = Objects.requireNonNull(header, "header");
         this.tabListFooter = Objects.requireNonNull(footer, "footer");

         this.connection.send(new ClientboundTabListPacket(header, footer));
    }

    // We need this as tablistDisplayname may be null even if the event was fired.
    private boolean hasTabListName = false;
    private Component tabListDisplayName = null; // Mohist - hook craftbukkit -> Component listName;
    /**
     * Force the name displayed in the tab list to refresh, by firing {@link PlayerEvent.TabListNameFormat}.
     */
    public void refreshTabListName() {
        Component oldName = this.tabListDisplayName;
        this.tabListDisplayName = EventHooks.getPlayerTabListDisplayName(this);
        if (!Objects.equals(oldName, this.tabListDisplayName)) {
            this.getServer().getPlayerList().broadcastAll(new ClientboundPlayerInfoUpdatePacket(ClientboundPlayerInfoUpdatePacket.Action.UPDATE_DISPLAY_NAME, this));
        }
    }

    public TextFilter getTextFilter() {
        return this.textFilter;
    }

    public void setServerLevel(ServerLevel p_284971_) {
        this.setLevel(p_284971_);
        this.gameMode.setLevel(p_284971_);
    }

    @Nullable
    private static GameType readPlayerMode(@Nullable CompoundTag p_143414_, String p_143415_) {
        return p_143414_ != null && p_143414_.contains(p_143415_, 99) ? GameType.byId(p_143414_.getInt(p_143415_)) : null;
    }

    private GameType calculateGameModeForNewPlayer(@Nullable GameType p_143424_) {
        GameType gametype = this.server.getForcedGameType();
        if (gametype != null) {
            return gametype;
        } else {
            return p_143424_ != null ? p_143424_ : this.server.getDefaultGameType();
        }
    }

    public void loadGameTypes(@Nullable CompoundTag p_143428_) {
        // Paper start - Expand PlayerGameModeChangeEvent
        if (this.server.getForcedGameType() != null && this.server.getForcedGameType() != ServerPlayer.readPlayerMode(p_143428_, "playerGameType")) {
            if (new org.bukkit.event.player.PlayerGameModeChangeEvent(this.getBukkitEntity(), org.bukkit.GameMode.getByValue(this.server.getDefaultGameType().getId()), org.bukkit.event.player.PlayerGameModeChangeEvent.Cause.DEFAULT_GAMEMODE, null).callEvent()) {
                this.gameMode.setGameModeForPlayer(this.server.getForcedGameType(), GameType.DEFAULT_MODE);
            } else {
                this.gameMode.setGameModeForPlayer(ServerPlayer.readPlayerMode(p_143428_,"playerGameType"), ServerPlayer.readPlayerMode(p_143428_, "previousPlayerGameType"));
            }
            return;
        }
        // Paper end - Expand PlayerGameModeChangeEvent
        this.gameMode
            .setGameModeForPlayer(
                this.calculateGameModeForNewPlayer(readPlayerMode(p_143428_, "playerGameType")), readPlayerMode(p_143428_, "previousPlayerGameType")
            );
    }

    private void storeGameTypes(CompoundTag p_143431_) {
        p_143431_.putInt("playerGameType", this.gameMode.getGameModeForPlayer().getId());
        GameType gametype = this.gameMode.getPreviousGameModeForPlayer();
        if (gametype != null) {
            p_143431_.putInt("previousPlayerGameType", gametype.getId());
        }
    }

    @Override
    public boolean isTextFilteringEnabled() {
        return this.textFilteringEnabled;
    }

    public boolean shouldFilterMessageTo(ServerPlayer p_143422_) {
        return p_143422_ == this ? false : this.textFilteringEnabled || p_143422_.textFilteringEnabled;
    }

    @Override
    public boolean mayInteract(Level p_143406_, BlockPos p_143407_) {
        return super.mayInteract(p_143406_, p_143407_) && p_143406_.mayInteract(this, p_143407_);
    }

    @Override
    protected void updateUsingItem(ItemStack p_143402_) {
        CriteriaTriggers.USING_ITEM.trigger(this, p_143402_);
        super.updateUsingItem(p_143402_);
    }

    public boolean drop(boolean p_182295_) {
        Inventory inventory = this.getInventory();
        ItemStack selected = inventory.getSelected();
        if (selected.isEmpty() || !selected.onDroppedByPlayer(this)) return false;
        if (isUsingItem() && getUsedItemHand() == InteractionHand.MAIN_HAND && (p_182295_ || selected.getCount() == 1)) stopUsingItem(); // Forge: fix MC-231097 on the serverside
        ItemStack itemstack = inventory.removeFromSelected(p_182295_);
        this.containerMenu.findSlot(inventory, inventory.selected).ifPresent(p_287377_ -> this.containerMenu.setRemoteSlot(p_287377_, inventory.getSelected()));
        return CommonHooks.onPlayerTossEvent(this, itemstack, true) != null;
    }

    public boolean allowsListing() {
        return this.allowsListing;
    }

    @Override
    public Optional<WardenSpawnTracker> getWardenSpawnTracker() {
        return Optional.of(this.wardenSpawnTracker);
    }

    public void setSpawnExtraParticlesOnFall(boolean p_334029_) {
        this.spawnExtraParticlesOnFall = p_334029_;
    }

    @Override
    public void onItemPickup(ItemEntity p_215095_) {
        super.onItemPickup(p_215095_);
        Entity entity = p_215095_.getOwner();
        if (entity != null) {
            CriteriaTriggers.THROWN_ITEM_PICKED_UP_BY_PLAYER.trigger(this, p_215095_.getItem(), entity);
        }
    }

    public void setChatSession(RemoteChatSession p_254468_) {
        this.chatSession = p_254468_;
    }

    @Nullable
    public RemoteChatSession getChatSession() {
        return this.chatSession != null && this.chatSession.hasExpired() ? null : this.chatSession;
    }

    @Override
    public void indicateDamage(double p_270621_, double p_270478_) {
        this.hurtDir = (float)(Mth.atan2(p_270478_, p_270621_) * 180.0F / (float)Math.PI - (double)this.getYRot());
        this.connection.send(new ClientboundHurtAnimationPacket(this));
    }

    @Override
    public boolean startRiding(Entity p_277395_, boolean p_278062_) {
        if (super.startRiding(p_277395_, p_278062_)) {
            this.setKnownMovement(Vec3.ZERO);
            p_277395_.positionRider(this);
            this.connection.teleport(this.getX(), this.getY(), this.getZ(), this.getYRot(), this.getXRot());
            if (p_277395_ instanceof LivingEntity livingentity) {
                this.server.getPlayerList().sendActiveEffects(livingentity, this.connection);
            }

            return true;
        } else {
            return false;
        }
    }

    @Override
    public void stopRiding() {
        Entity entity = this.getVehicle();
        super.stopRiding();
        if (entity instanceof LivingEntity livingentity) {
            for (MobEffectInstance mobeffectinstance : livingentity.getActiveEffects()) {
                this.connection.send(new ClientboundRemoveMobEffectPacket(entity.getId(), mobeffectinstance.getEffect()));
            }
        }
    }

    public CommonPlayerSpawnInfo createCommonSpawnInfo(ServerLevel p_294169_) {
        return new CommonPlayerSpawnInfo(
            p_294169_.dimensionTypeRegistration(),
            p_294169_.dimension(),
            BiomeManager.obfuscateSeed(p_294169_.getSeed()),
            this.gameMode.getGameModeForPlayer(),
            this.gameMode.getPreviousGameModeForPlayer(),
            p_294169_.isDebug(),
            p_294169_.isFlat(),
            this.getLastDeathLocation(),
            this.getPortalCooldown()
        );
    }

    public void setRaidOmenPosition(BlockPos p_338782_) {
        this.raidOmenPosition = p_338782_;
    }

    public void clearRaidOmenPosition() {
        this.raidOmenPosition = null;
    }

    @Nullable
    public BlockPos getRaidOmenPosition() {
        return this.raidOmenPosition;
    }

    @Override
    public Vec3 getKnownMovement() {
        Entity entity = this.getVehicle();
        return entity != null && entity.getControllingPassenger() != this ? entity.getKnownMovement() : this.lastKnownClientMovement;
    }

    public void setKnownMovement(Vec3 p_348509_) {
        this.lastKnownClientMovement = p_348509_;
    }

    @Override
    protected float getEnchantedDamage(Entity p_346252_, float p_346142_, DamageSource p_345841_) {
        return EnchantmentHelper.modifyDamage(this.serverLevel(), this.getWeaponItem(), p_346252_, p_345841_, p_346142_);
    }

    @Override
    public void onEquippedItemBroken(Item p_348565_, EquipmentSlot p_348623_) {
        super.onEquippedItemBroken(p_348565_, p_348623_);
        this.awardStat(Stats.ITEM_BROKEN.get(p_348565_));
    }

    public static record RespawnPosAngle(Vec3 position, float yaw, boolean isBedSpawn, boolean isAnchorSpawn) {
        public static RespawnPosAngle of(Vec3 p_348670_, BlockPos p_348504_, boolean isBedSpawn, boolean isAnchorSpawn) {
            return new RespawnPosAngle(p_348670_, calculateLookAtYaw(p_348670_, p_348504_), isBedSpawn, isAnchorSpawn);
        }

        private static float calculateLookAtYaw(Vec3 p_348686_, BlockPos p_348467_) {
            Vec3 vec3 = Vec3.atBottomCenterOf(p_348467_).subtract(p_348686_).normalize();
            return (float)Mth.wrapDegrees(Mth.atan2(vec3.z, vec3.x) * 180.0F / (float)Math.PI - 90.0);
        }
    }

    // CraftBukkit start - Add per-player time and weather.
    public long timeOffset = 0;
    public boolean relativeTime = true;

    public long getPlayerTime() {
        if (this.relativeTime) {
            // Adds timeOffset to the current server time.
            return this.level().getDayTime() + this.timeOffset;
        } else {
            // Adds timeOffset to the beginning of this day.
            return this.level().getDayTime() - (this.level().getDayTime() % 24000) + this.timeOffset;
        }
    }

    public WeatherType weather = null;

    public WeatherType getPlayerWeather() {
        return this.weather;
    }

    public void setPlayerWeather(WeatherType type, boolean plugin) {
        if (!plugin && this.weather != null) {
            return;
        }
        if (plugin) {
            this.weather = type;
        }

        if (type == WeatherType.DOWNFALL) {
            this.connection.send(new ClientboundGameEventPacket(ClientboundGameEventPacket.STOP_RAINING, 0));
        } else {
            this.connection.send(new ClientboundGameEventPacket(ClientboundGameEventPacket.START_RAINING, 0));
        }
    }

    private float pluginRainPosition;
    private float pluginRainPositionPrevious;

    public void updateWeather(float oldRain, float newRain, float oldThunder, float newThunder) {
        if (this.weather == null) {
            // Vanilla
            if (oldRain != newRain) {
                this.connection.send(new ClientboundGameEventPacket(ClientboundGameEventPacket.RAIN_LEVEL_CHANGE, newRain));
            }
        } else {
            // Plugin
            if (pluginRainPositionPrevious != pluginRainPosition) {
                this.connection.send(new ClientboundGameEventPacket(ClientboundGameEventPacket.RAIN_LEVEL_CHANGE, pluginRainPosition));
            }
        }

        if (oldThunder != newThunder) {
            if (weather == WeatherType.DOWNFALL || weather == null) {
                this.connection.send(new ClientboundGameEventPacket(ClientboundGameEventPacket.THUNDER_LEVEL_CHANGE, newThunder));
            } else {
                this.connection.send(new ClientboundGameEventPacket(ClientboundGameEventPacket.THUNDER_LEVEL_CHANGE, 0));
            }
        }
    }

    public void tickWeather() {
        if (this.weather == null) return;

        pluginRainPositionPrevious = pluginRainPosition;
        if (weather == WeatherType.DOWNFALL) {
            pluginRainPosition += 0.01;
        } else {
            pluginRainPosition -= 0.01;
        }

        pluginRainPosition = Mth.clamp(pluginRainPosition, 0.0F, 1.0F);
    }

    public void resetPlayerWeather() {
        this.weather = null;
        this.setPlayerWeather(this.level().getLevelData().isRaining() ? WeatherType.DOWNFALL : WeatherType.CLEAR, false);
    }

    @Override
    public String toString() {
        return super.toString() + "(" + this.getScoreboardName() + " at " + this.getX() + "," + this.getY() + "," + this.getZ() + ")";
    }

    // SPIGOT-1903, MC-98153
    public void forceSetPositionRotation(double x, double y, double z, float yaw, float pitch) {
        this.moveTo(x, y, z, yaw, pitch);
        this.connection.resetPosition();
    }

    @Override
    public boolean isImmobile() {
        return super.isImmobile() || (this.connection != null && this.connection.isDisconnected()); // Paper - Fix duplication bugs
    }

    @Override
    public Scoreboard getScoreboard() {
        return getBukkitEntity().getScoreboard().getHandle();
    }

    public void reset() {
        float exp = 0;
        boolean keepInventory = this.level().getGameRules().getBoolean(GameRules.RULE_KEEPINVENTORY);

        if (this.keepLevel) { // CraftBukkit - SPIGOT-6687: Only use keepLevel (was pre-set with RULE_KEEPINVENTORY value in PlayerDeathEvent)
            exp = this.experienceProgress;
            this.newTotalExp = this.totalExperience;
            this.newLevel = this.experienceLevel;
        }

        this.setHealth(this.getMaxHealth());
        this.stopUsingItem(); // CraftBukkit - SPIGOT-6682: Clear active item on reset
        this.setAirSupply(this.getMaxAirSupply()); // Paper - Reset players airTicks on respawn
        this.setRemainingFireTicks(0);
        this.fallDistance = 0;
        this.foodData = new FoodData().player(this);
        this.experienceLevel = this.newLevel;
        this.totalExperience = this.newTotalExp;
        this.experienceProgress = 0;
        this.deathTime = 0;
        this.setArrowCount(0, true); // CraftBukkit - ArrowBodyCountChangeEvent
        this.removeAllEffects(EntityPotionEffectEvent.Cause.DEATH);
        this.effectsDirty = true;
        this.containerMenu = this.inventoryMenu;
        this.lastHurtByPlayer = null;
        this.lastHurtByMob = null;
        this.combatTracker = new CombatTracker(this);
        this.lastSentExp = -1;
        if (this.keepLevel) { // CraftBukkit - SPIGOT-6687: Only use keepLevel (was pre-set with RULE_KEEPINVENTORY value in PlayerDeathEvent)
            this.experienceProgress = exp;
        } else {
            this.giveExperiencePoints(this.newExp);
        }
        this.keepLevel = false;
        this.setDeltaMovement(0, 0, 0); // CraftBukkit - SPIGOT-6948: Reset velocity on death
        this.skipDropExperience = false; // CraftBukkit - SPIGOT-7462: Reset experience drop skip, so that further deaths drop xp
    }

    @Override
    public CraftPlayer getBukkitEntity() {
        return (CraftPlayer) super.getBukkitEntity();
    }
   // CraftBukkit end

    // Purpur start
    public void teleport(Location to) {
        this.ejectPassengers();
        this.stopRiding();

        if (this.isSleeping()) {
            this.stopSleepInBed(true, false);
        }

        if (this.containerMenu != this.inventoryMenu) {
            this.closeContainer(org.bukkit.event.inventory.InventoryCloseEvent.Reason.TELEPORT);
        }

        ServerLevel toLevel = ((CraftWorld) to.getWorld()).getHandle();
        if (this.level() == toLevel) {
            this.connection.internalTeleport(to.getX(), to.getY(), to.getZ(), to.getYaw(), to.getPitch(), java.util.EnumSet.noneOf(net.minecraft.world.entity.RelativeMovement.class));
        } else {
            this.server.getPlayerList().respawn(this, true, RemovalReason.KILLED, org.bukkit.event.player.PlayerRespawnEvent.RespawnReason.DEATH, to);
        }
    }

    public boolean tpsBar() {
        return this.tpsBar;
    }

    public void tpsBar(boolean tpsBar) {
        this.tpsBar = tpsBar;
    }

    public boolean compassBar() {
        return this.compassBar;
    }

    public void compassBar(boolean compassBar) {
        this.compassBar = compassBar;
    }

    public boolean ramBar() {
        return this.ramBar;
    }

    public void ramBar(boolean ramBar) {
        this.ramBar = ramBar;
    }
    // Purpur end
}
