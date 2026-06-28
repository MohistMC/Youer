package ca.spottedleaf.dataconverter.minecraft.versions;

import ca.spottedleaf.dataconverter.converters.DataConverter;
import ca.spottedleaf.dataconverter.minecraft.MCVersions;
import ca.spottedleaf.dataconverter.minecraft.converters.helpers.RenameHelper;
import ca.spottedleaf.dataconverter.minecraft.datatypes.MCTypeRegistry;
import ca.spottedleaf.dataconverter.types.ListType;
import ca.spottedleaf.dataconverter.types.MapType;

public final class V4309 {

    private static final int VERSION = MCVersions.V25W04A + 1;

    public static int[] makeBlockPosition(final Number x, final Number y, final Number z) {
        return new int[] { x.intValue(), y.intValue(), z.intValue() };
    }

    public static void convertBlockPosition(final MapType data, final String xPath, final String yPath, final String zPath,
                                            final String toPath) {
        final Number x = data.getNumber(xPath);
        final Number y = data.getNumber(yPath);
        final Number z = data.getNumber(zPath);

        if (x == null || y == null || z == null) {
            return;
        }

        data.remove(xPath);
        data.remove(yPath);
        data.remove(zPath);

        data.setInts(toPath, makeBlockPosition(x, y, z));
    }

    public static void register() {
        MCTypeRegistry.SAVED_DATA_RAIDS.addStructureConverter(new DataConverter<>(VERSION) {
            @Override
            public MapType convert(final MapType root, final long sourceVersion, final long toVersion) {
                final MapType data = root.getMap("data");
                if (data == null) {
                    return null;
                }

                RenameHelper.renameSingle(data, "Raids", "raids");
                RenameHelper.renameSingle(data, "Tick", "tick");
                RenameHelper.renameSingle(data, "NextAvailableID", "next_id");

                final ListType raids = data.getListUnchecked("raids");
                if (raids != null) {
                    for (int i = 0, len = raids.size(); i < len; ++i) {
                        final MapType raid = raids.getMap(i, null);
                        if (raid == null) {
                            continue;
                        }

                        convertBlockPosition(raid, "CX", "CY", "CZ", "center");

                        RenameHelper.renameSingle(raid, "Id", "id");
                        RenameHelper.renameSingle(raid, "Started", "started");
                        RenameHelper.renameSingle(raid, "Active", "active");
                        RenameHelper.renameSingle(raid, "TicksActive", "ticks_active");
                        RenameHelper.renameSingle(raid, "BadOmenLevel", "raid_omen_level");
                        RenameHelper.renameSingle(raid, "GroupsSpawned", "groups_spawned");
                        RenameHelper.renameSingle(raid, "PreRaidTicks", "cooldown_ticks");
                        RenameHelper.renameSingle(raid, "PostRaidTicks", "post_raid_ticks");
                        RenameHelper.renameSingle(raid, "TotalHealth", "total_health");
                        RenameHelper.renameSingle(raid, "NumGroups", "group_count");
                        RenameHelper.renameSingle(raid, "Status", "status");
                        RenameHelper.renameSingle(raid, "HeroesOfTheVillage", "heroes_of_the_village");
                    }
                }

                return null;
            }
        });
        MCTypeRegistry.SAVED_DATA_TICKETS.addStructureConverter(new DataConverter<>(VERSION) {
            @Override
            public MapType convert(final MapType root, final long sourceVersion, final long toVersion) {
                final MapType data = root.getMap("data");
                if (data == null) {
                    return null;
                }


                final ListType tickets = data.getListUnchecked("tickets");
                if (tickets == null) {
                    return null;
                }

                for (int i = 0, len = tickets.size(); i < len; ++i) {
                    final MapType ticket = tickets.getMap(i, null);
                    if (ticket == null) {
                        continue;
                    }

                    if (ticket.hasKey("chunk_pos")) {
                        final long coordinate = ticket.getLong("chunk_pos", 0L);

                        ticket.setInts("chunk_pos", new int[] { (int)coordinate, (int)(coordinate >>> 32) });
                    }
                }

                return null;
            }
        });
    }

    private V4309() {}
}
