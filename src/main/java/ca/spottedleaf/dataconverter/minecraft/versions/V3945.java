package ca.spottedleaf.dataconverter.minecraft.versions;

import ca.spottedleaf.dataconverter.converters.DataConverter;
import ca.spottedleaf.dataconverter.minecraft.MCVersions;
import ca.spottedleaf.dataconverter.minecraft.converters.helpers.RenameHelper;
import ca.spottedleaf.dataconverter.minecraft.datatypes.MCTypeRegistry;
import ca.spottedleaf.dataconverter.types.ListType;
import ca.spottedleaf.dataconverter.types.MapType;
import ca.spottedleaf.dataconverter.types.ObjectType;
import com.google.common.collect.ImmutableMap;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.Locale;
import java.util.Map;
import java.util.UUID;

public final class V3945 {

    private static final int VERSION = MCVersions.V2420WA + 1;

    private static final Map<UUID, String> UUID_TO_ID = new HashMap<>(
        ImmutableMap.<UUID, String>builder()
            .put(UUID.fromString("736565d2-e1a7-403d-a3f8-1aeb3e302542"), "minecraft:creative_mode_block_range")
            .put(UUID.fromString("98491ef6-97b1-4584-ae82-71a8cc85cf73"), "minecraft:creative_mode_entity_range")
            .put(UUID.fromString("91AEAA56-376B-4498-935B-2F7F68070635"), "minecraft:effect.speed")
            .put(UUID.fromString("7107DE5E-7CE8-4030-940E-514C1F160890"), "minecraft:effect.slowness")
            .put(UUID.fromString("AF8B6E3F-3328-4C0A-AA36-5BA2BB9DBEF3"), "minecraft:effect.haste")
            .put(UUID.fromString("55FCED67-E92A-486E-9800-B47F202C4386"), "minecraft:effect.mining_fatigue")
            .put(UUID.fromString("648D7064-6A60-4F59-8ABE-C2C23A6DD7A9"), "minecraft:effect.strength")
            .put(UUID.fromString("C0105BF3-AEF8-46B0-9EBC-92943757CCBE"), "minecraft:effect.jump_boost")
            .put(UUID.fromString("22653B89-116E-49DC-9B6B-9971489B5BE5"), "minecraft:effect.weakness")
            .put(UUID.fromString("5D6F0BA2-1186-46AC-B896-C61C5CEE99CC"), "minecraft:effect.health_boost")
            .put(UUID.fromString("EAE29CF0-701E-4ED6-883A-96F798F3DAB5"), "minecraft:effect.absorption")
            .put(UUID.fromString("03C3C89D-7037-4B42-869F-B146BCB64D2E"), "minecraft:effect.luck")
            .put(UUID.fromString("CC5AF142-2BD2-4215-B636-2605AED11727"), "minecraft:effect.unluck")
            .put(UUID.fromString("6555be74-63b3-41f1-a245-77833b3c2562"), "minecraft:evil")
            .put(UUID.fromString("1eaf83ff-7207-4596-b37a-d7a07b3ec4ce"), "minecraft:powder_snow")
            .put(UUID.fromString("662A6B8D-DA3E-4C1C-8813-96EA6097278D"), "minecraft:sprinting")
            .put(UUID.fromString("020E0DFB-87AE-4653-9556-831010E291A0"), "minecraft:attacking")
            .put(UUID.fromString("766bfa64-11f3-11ea-8d71-362b9e155667"), "minecraft:baby")
            .put(UUID.fromString("7E0292F2-9434-48D5-A29F-9583AF7DF27F"), "minecraft:covered")
            .put(UUID.fromString("9e362924-01de-4ddd-a2b2-d0f7a405a174"), "minecraft:suffocating")
            .put(UUID.fromString("5CD17E52-A79A-43D3-A529-90FDE04B181E"), "minecraft:drinking")
            .put(UUID.fromString("B9766B59-9566-4402-BC1F-2EE2A276D836"), "minecraft:baby")
            .put(UUID.fromString("49455A49-7EC5-45BA-B886-3B90B23A1718"), "minecraft:attacking")
            .put(UUID.fromString("845DB27C-C624-495F-8C9F-6020A9A58B6B"), "minecraft:armor.boots")
            .put(UUID.fromString("D8499B04-0E66-4726-AB29-64469D734E0D"), "minecraft:armor.leggings")
            .put(UUID.fromString("9F3D476D-C118-4544-8365-64846904B48E"), "minecraft:armor.chestplate")
            .put(UUID.fromString("2AD3F246-FEE1-4E67-B886-69FD380BB150"), "minecraft:armor.helmet")
            .put(UUID.fromString("C1C72771-8B8E-BA4A-ACE0-81A93C8928B2"), "minecraft:armor.body")
            .put(UUID.fromString("b572ecd2-ac0c-4071-abde-9594af072a37"), "minecraft:enchantment.fire_protection")
            .put(UUID.fromString("40a9968f-5c66-4e2f-b7f4-2ec2f4b3e450"), "minecraft:enchantment.blast_protection")
            .put(UUID.fromString("07a65791-f64d-4e79-86c7-f83932f007ec"), "minecraft:enchantment.respiration")
            .put(UUID.fromString("60b1b7db-fffd-4ad0-817c-d6c6a93d8a45"), "minecraft:enchantment.aqua_affinity")
            .put(UUID.fromString("11dc269a-4476-46c0-aff3-9e17d7eb6801"), "minecraft:enchantment.depth_strider")
            .put(UUID.fromString("87f46a96-686f-4796-b035-22e16ee9e038"), "minecraft:enchantment.soul_speed")
            .put(UUID.fromString("b9716dbd-50df-4080-850e-70347d24e687"), "minecraft:enchantment.soul_speed")
            .put(UUID.fromString("92437d00-c3a7-4f2e-8f6c-1f21585d5dd0"), "minecraft:enchantment.swift_sneak")
            .put(UUID.fromString("5d3d087b-debe-4037-b53e-d84f3ff51f17"), "minecraft:enchantment.sweeping_edge")
            .put(UUID.fromString("3ceb37c0-db62-46b5-bd02-785457b01d96"), "minecraft:enchantment.efficiency")
            .put(UUID.fromString("CB3F55D3-645C-4F38-A497-9C13A33DB5CF"), "minecraft:base_attack_damage")
            .put(UUID.fromString("FA233E1C-4180-4865-B01B-BCCE9785ACA3"), "minecraft:base_attack_speed")
            .build()
    );
    private static final Map<String, String> NAME_TO_ID = new HashMap<>(
        Map.of(
            "Random spawn bonus", "minecraft:random_spawn_bonus",
            "Random zombie-spawn bonus", "minecraft:zombie_random_spawn_bonus",
            "Leader zombie bonus", "minecraft:leader_zombie_bonus",
            "Zombie reinforcement callee charge", "minecraft:reinforcement_callee_charge",
            "Zombie reinforcement caller charge", "minecraft:reinforcement_caller_charge"
        )
    );

    private static UUID makeUUID(final int[] arr) {
        if (arr == null || arr.length != 4) {
            return null;
        }

        final long most = ((long)arr[0] << 32) | ((long)arr[1] & 0xFFFFFFFFL);
        final long least = ((long)arr[2] << 32) | ((long)arr[3] & 0xFFFFFFFFL);

        return new UUID(most, least);
    }

    private static ListType remapModifiers(final ListType list) {
        final Map<String, MapType<String>> ret = new LinkedHashMap<>();

        for (int i = 0, len = list.size(); i < len; ++i) {
            final MapType<String> modifier = list.<String>getMap(i).copy();

            final UUID uuid = makeUUID(modifier.getInts("uuid"));
            final String name = modifier.getString("name", "");

            final String remappedUUID = UUID_TO_ID.get(uuid);
            final String remappedName = NAME_TO_ID.get(name);

            if (remappedUUID != null) {
                modifier.remove("uuid");
                modifier.remove("name");

                modifier.setString("id", remappedUUID);

                ret.put(remappedUUID, modifier);
            } else if (remappedName != null) {
                final MapType<String> existing = ret.get(remappedName);
                if (existing != null) {
                    existing.setDouble("amount",
                        existing.getDouble("amount", 0.0) + modifier.getDouble("amount", 0.0)
                    );
                } else {
                    modifier.remove("uuid");
                    modifier.remove("name");

                    modifier.setString("id", remappedName);

                    ret.put(remappedName, modifier);
                }
            } else {
                final String id = "minecraft:" + (uuid == null ? "unknown" : uuid.toString().toLowerCase(Locale.ROOT));

                modifier.setString("id", id);

                ret.put(id, modifier);
            }
        }

        final ListType retList = list.getTypeUtil().createEmptyList();

        for (final MapType<String> modifier : ret.values()) {
            retList.addMap(modifier);
        }

        return retList;
    }


    public static void register() {

        MCTypeRegistry.ITEM_STACK.addStructureConverter(new DataConverter<>(VERSION) {
            @Override
            public MapType<String> convert(final MapType<String> data, final long sourceVersion, final long toVersion) {
                final MapType<String> components = data.getMap("components");
                if (components == null) {
                    return null;
                }

                final MapType<String> attributeModifiers = components.getMap("minecraft:attribute_modifiers");
                if (attributeModifiers == null) {
                    return null;
                }

                final ListType modifiers = attributeModifiers.getList("modifiers", ObjectType.MAP);
                if (modifiers == null) {
                    return null;
                }

                attributeModifiers.setList("modifiers", remapModifiers(modifiers));

                return null;
            }
        });

        final DataConverter<MapType<String>, MapType<String>> entityConverter = new DataConverter<>(VERSION) {
            @Override
            public MapType<String> convert(final MapType<String> data, final long sourceVersion, final long toVersion) {
                RenameHelper.renameSingle(data, "Attributes", "attributes");

                final ListType attributes = data.getList("attributes", ObjectType.MAP);
                if (attributes == null) {
                    return null;
                }

                for (int i = 0, len = attributes.size(); i < len; ++i) {
                    final MapType<String> attribute = attributes.getMap(i);

                    RenameHelper.renameSingle(attribute, "Name", "id");
                    RenameHelper.renameSingle(attribute, "Base", "base");
                    RenameHelper.renameSingle(attribute, "Modifiers", "modifiers");

                    final ListType modifiers = attribute.getList("modifiers", ObjectType.MAP);

                    if (modifiers == null) {
                        continue;
                    }

                    for (int j = 0, len2 = modifiers.size(); j < len2; ++j) {
                        final MapType<String> modifier = modifiers.getMap(j);

                        RenameHelper.renameSingle(modifier, "UUID", "uuid");
                        RenameHelper.renameSingle(modifier, "Name", "name");
                        RenameHelper.renameSingle(modifier, "Amount", "amount");
                        final String newOp;
                        switch (modifier.getInt("Operation", 0)) {
                            case 0: {
                                newOp = "add_value";
                                break;
                            }
                            case 1: {
                                newOp = "add_multiplied_base";
                                break;
                            }
                            case 2: {
                                newOp = "add_multiplied_total";
                                break;
                            }
                            default: {
                                newOp = "invalid";
                                break;
                            }
                        }
                        modifier.remove("Operation");
                        modifier.setString("operation", newOp);
                    }

                    attribute.setList("modifiers", remapModifiers(modifiers));
                }

                return null;
            }
        };

        MCTypeRegistry.ENTITY.addStructureConverter(entityConverter);
        MCTypeRegistry.PLAYER.addStructureConverter(entityConverter);


        MCTypeRegistry.TILE_ENTITY.addConverterForId("minecraft:jukebox", new DataConverter<>(VERSION) {
            @Override
            public MapType<String> convert(final MapType<String> data, final long sourceVersion, final long toVersion) {
                final long playingFor = data.getLong("TickCount") - data.getLong("RecordStartTick");

                data.remove("IsPlaying");
                data.remove("TickCount");
                data.remove("RecordStartTick");

                if (playingFor > 0L) {
                    data.setLong("ticks_since_song_started", playingFor);
                }

                return null;
            }
        });
    }

    private V3945() {}
}
