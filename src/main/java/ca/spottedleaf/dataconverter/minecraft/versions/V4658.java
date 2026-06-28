package ca.spottedleaf.dataconverter.minecraft.versions;

import ca.spottedleaf.dataconverter.converters.DataConverter;
import ca.spottedleaf.dataconverter.minecraft.MCVersions;
import ca.spottedleaf.dataconverter.minecraft.converters.helpers.RenameHelper;
import ca.spottedleaf.dataconverter.minecraft.datatypes.MCTypeRegistry;
import ca.spottedleaf.dataconverter.types.MapType;
import net.minecraft.util.Mth;

public final class V4658 {

    private static final int VERSION = MCVersions.V25W43A + 3;

    public static void register() {
        MCTypeRegistry.LEVEL.addStructureConverter(new DataConverter<>(VERSION) {
            private static void convertBool(final MapType data, final String src, final String dst) {
                if (!data.hasKey(src)) {
                    return;
                }
                final String value = data.getString(src, "");

                data.remove(src);

                data.setBoolean(dst, Boolean.parseBoolean(value));
            }

            private static void convertBoolInv(final MapType data, final String src, final String dst) {
                if (!data.hasKey(src)) {
                    return;
                }
                final String value = data.getString(src, "");

                data.remove(src);

                data.setBoolean(dst, !Boolean.parseBoolean(value));
            }

            private static void convertInt(final MapType data, final String src, final String dst) {
                convertInt(data, src, dst, Integer.MIN_VALUE);
            }

            private static void convertInt(final MapType data, final String src, final String dst, final int min) {
                convertInt(data, src, dst, min, Integer.MAX_VALUE);
            }

            private static void convertInt(final MapType data, final String src, final String dst, final int min, final int max) {
                if (!data.hasKey(src)) {
                    return;
                }
                final String value = data.getString(src, "");

                data.remove(src);

                try {
                    data.setInt(dst, Mth.clamp(Integer.parseInt(value), min, max));
                } catch (final NumberFormatException ex) {
                    data.setString(dst, value);
                }
            }

            @Override
            public MapType convert(final MapType data, final long sourceVersion, final long toVersion) {
                RenameHelper.renameSingle(data, "GameRules", "game_rules");

                final MapType gameRules = data.getMap("game_rules");
                if (gameRules == null) {
                    return null;
                }

                final boolean doFireTick = Boolean.parseBoolean(gameRules.getString("doFireTick", "true"));
                final boolean allowFireTicksAwayFromPlayer = Boolean.parseBoolean(gameRules.getString("allowFireTicksAwayFromPlayer", "false"));

                gameRules.remove("spawnChunkRadius");
                gameRules.remove("entitiesWithPassengersCanUsePortals");
                gameRules.remove("gameLoopFunction");
                gameRules.remove("doFireTick");
                gameRules.remove("allowFireTicksAwayFromPlayer");

                if (!doFireTick) {
                    gameRules.setInt("minecraft:fire_spread_radius_around_player", 0);
                } else if (allowFireTicksAwayFromPlayer) {
                    gameRules.setInt("minecraft:fire_spread_radius_around_player", -1);
                } // else: default to 128

                convertBool(gameRules, "allowEnteringNetherUsingPortals", "minecraft:allow_entering_nether_using_portals");

                convertBool(gameRules, "announceAdvancements", "minecraft:show_advancement_messages");
                convertBool(gameRules, "blockExplosionDropDecay", "minecraft:block_explosion_drop_decay");
                convertBool(gameRules, "commandBlockOutput", "minecraft:command_block_output");
                convertBool(gameRules, "enableCommandBlocks", "minecraft:command_blocks_work");
                convertBool(gameRules, "commandBlocksEnabled", "minecraft:command_blocks_work");

                convertInt(gameRules, "commandModificationBlockLimit", "minecraft:max_block_modifications", 1);

                convertBoolInv(gameRules, "disableElytraMovementCheck", "minecraft:elytra_movement_check");
                convertBoolInv(gameRules, "disablePlayerMovementCheck", "minecraft:player_movement_check");
                convertBoolInv(gameRules, "disableRaids", "minecraft:raids");

                convertBool(gameRules, "doDaylightCycle", "minecraft:advance_time");
                convertBool(gameRules, "doEntityDrops", "minecraft:entity_drops");
                convertBool(gameRules, "doImmediateRespawn", "minecraft:immediate_respawn");
                convertBool(gameRules, "doInsomnia", "minecraft:spawn_phantoms");
                convertBool(gameRules, "doLimitedCrafting", "minecraft:limited_crafting");
                convertBool(gameRules, "doMobLoot", "minecraft:mob_drops");
                convertBool(gameRules, "doMobSpawning", "minecraft:spawn_mobs");
                convertBool(gameRules, "doPatrolSpawning", "minecraft:spawn_patrols");
                convertBool(gameRules, "doTileDrops", "minecraft:block_drops");
                convertBool(gameRules, "doTraderSpawning", "minecraft:spawn_wandering_traders");
                convertBool(gameRules, "doVinesSpread", "minecraft:spread_vines");
                convertBool(gameRules, "doWardenSpawning", "minecraft:spawn_wardens");
                convertBool(gameRules, "doWeatherCycle", "minecraft:advance_weather");
                convertBool(gameRules, "drowningDamage", "minecraft:drowning_damage");
                convertBool(gameRules, "enderPearlsVanishOnDeath", "minecraft:ender_pearls_vanish_on_death");
                convertBool(gameRules, "fallDamage", "minecraft:fall_damage");
                convertBool(gameRules, "fireDamage", "minecraft:fire_damage");
                convertBool(gameRules, "forgiveDeadPlayers", "minecraft:forgive_dead_players");
                convertBool(gameRules, "freezeDamage", "minecraft:freeze_damage");
                convertBool(gameRules, "globalSoundEvents", "minecraft:global_sound_events");
                convertBool(gameRules, "keepInventory", "minecraft:keep_inventory");
                convertBool(gameRules, "lavaSourceConversion", "minecraft:lava_source_conversion");
                convertBool(gameRules, "locatorBar", "minecraft:locator_bar");
                convertBool(gameRules, "logAdminCommands", "minecraft:log_admin_commands");

                convertInt(gameRules, "maxCommandChainLength", "minecraft:max_command_sequence_length", 0);
                convertInt(gameRules, "maxCommandForkCount", "minecraft:max_command_forks", 0);
                convertInt(gameRules, "maxEntityCramming", "minecraft:max_entity_cramming", 0);

                convertInt(gameRules, "minecartMaxSpeed", "minecraft:max_minecart_speed");

                convertBool(gameRules, "mobExplosionDropDecay", "minecraft:mob_explosion_drop_decay");
                convertBool(gameRules, "mobGriefing", "minecraft:mob_griefing");
                convertBool(gameRules, "naturalRegeneration", "minecraft:natural_health_regeneration");

                convertInt(gameRules, "playersNetherPortalCreativeDelay", "minecraft:players_nether_portal_creative_delay", 0);
                convertInt(gameRules, "playersNetherPortalDefaultDelay", "minecraft:players_nether_portal_default_delay", 0);
                convertInt(gameRules, "playersSleepingPercentage", "minecraft:players_sleeping_percentage", 0);

                convertBool(gameRules, "projectilesCanBreakBlocks", "minecraft:projectiles_can_break_blocks");
                convertBool(gameRules, "pvp", "minecraft:pvp");

                convertInt(gameRules, "randomTickSpeed", "minecraft:random_tick_speed", 0);

                convertBool(gameRules, "reducedDebugInfo", "minecraft:reduced_debug_info");
                convertBool(gameRules, "sendCommandFeedback", "minecraft:send_command_feedback");
                convertBool(gameRules, "showDeathMessages", "minecraft:show_death_messages");

                convertInt(gameRules, "snowAccumulationHeight", "minecraft:max_snow_accumulation_height", 0, 8);

                convertBool(gameRules, "spawnMonsters", "minecraft:spawn_monsters");

                convertInt(gameRules, "spawnRadius", "minecraft:respawn_radius");

                convertBool(gameRules, "spawnerBlocksEnabled", "minecraft:spawner_blocks_work");
                convertBool(gameRules, "spectatorsGenerateChunks", "minecraft:spectators_generate_chunks");
                convertBool(gameRules, "tntExplodes", "minecraft:tnt_explodes");
                convertBool(gameRules, "tntExplosionDropDecay", "minecraft:tnt_explosion_drop_decay");
                convertBool(gameRules, "universalAnger", "minecraft:universal_anger");
                convertBool(gameRules, "waterSourceConversion", "minecraft:water_source_conversion");

                return null;
            }
        });
    }

    private V4658() {}
}
