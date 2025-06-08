package ca.spottedleaf.dataconverter.minecraft.converters.itemstack;

import ca.spottedleaf.dataconverter.minecraft.converters.helpers.RenameHelper;
import ca.spottedleaf.dataconverter.minecraft.util.ComponentUtils;
import ca.spottedleaf.dataconverter.minecraft.versions.V3818;
import ca.spottedleaf.dataconverter.types.ListType;
import ca.spottedleaf.dataconverter.types.MapType;
import ca.spottedleaf.dataconverter.types.ObjectType;
import ca.spottedleaf.dataconverter.types.TypeUtil;
import ca.spottedleaf.dataconverter.util.NamespaceUtil;
import net.minecraft.util.Mth;
import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;

public final class ConverterItemStackToDataComponents {

    private static final int TOOLTIP_FLAG_HIDE_ENCHANTMENTS = 1 << 0;
    private static final int TOOLTIP_FLAG_HIDE_MODIFIERS = 1 << 1;
    private static final int TOOLTIP_FLAG_HIDE_UNBREAKABLE = 1 << 2;
    private static final int TOOLTIP_FLAG_HIDE_CAN_DESTROY = 1 << 3;
    private static final int TOOLTIP_FLAG_HIDE_CAN_PLACE = 1 << 4;
    private static final int TOOLTIP_FLAG_HIDE_ADDITIONAL = 1 << 5;
    private static final int TOOLTIP_FLAG_HIDE_DYE = 1 << 6;
    private static final int TOOLTIP_FLAG_HIDE_UPGRADES = 1 << 7;

    private static final int DEFAULT_LEATHER_COLOUR = (160 << 16) | (101 << 8) | (64 << 0); // r, g, b

    private static final String[] BUCKETED_MOB_TAGS = new String[] {
            "NoAI",
            "Silent",
            "NoGravity",
            "Glowing",
            "Invulnerable",
            "Health",
            "Age",
            "Variant",
            "HuntingCooldown",
            "BucketVariantTag"
    };
    private static final Set<String> BOOLEAN_BLOCK_STATE_PROPERTIES = new HashSet<>(
            Arrays.asList(
                    "attached",
                    "bottom",
                    "conditional",
                    "disarmed",
                    "drag",
                    "enabled",
                    "extended",
                    "eye",
                    "falling",
                    "hanging",
                    "has_bottle_0",
                    "has_bottle_1",
                    "has_bottle_2",
                    "has_record",
                    "has_book",
                    "inverted",
                    "in_wall",
                    "lit",
                    "locked",
                    "occupied",
                    "open",
                    "persistent",
                    "powered",
                    "short",
                    "signal_fire",
                    "snowy",
                    "triggered",
                    "unstable",
                    "waterlogged",
                    "berries",
                    "bloom",
                    "shrieking",
                    "can_summon",
                    "up",
                    "down",
                    "north",
                    "east",
                    "south",
                    "west",
                    "slot_0_occupied",
                    "slot_1_occupied",
                    "slot_2_occupied",
                    "slot_3_occupied",
                    "slot_4_occupied",
                    "slot_5_occupied",
                    "cracked",
                    "crafting"
            )
    );
    private static final String[] MAP_DECORATION_CONVERSION_TABLE = new String[34];
    static {
        MAP_DECORATION_CONVERSION_TABLE[0] = "player";
        MAP_DECORATION_CONVERSION_TABLE[1] = "frame";
        MAP_DECORATION_CONVERSION_TABLE[2] = "red_marker";
        MAP_DECORATION_CONVERSION_TABLE[3] = "blue_marker";
        MAP_DECORATION_CONVERSION_TABLE[4] = "target_x";
        MAP_DECORATION_CONVERSION_TABLE[5] = "target_point";
        MAP_DECORATION_CONVERSION_TABLE[6] = "player_off_map";
        MAP_DECORATION_CONVERSION_TABLE[7] = "player_off_limits";
        MAP_DECORATION_CONVERSION_TABLE[8] = "mansion";
        MAP_DECORATION_CONVERSION_TABLE[9] = "monument";
        MAP_DECORATION_CONVERSION_TABLE[10] = "banner_white";
        MAP_DECORATION_CONVERSION_TABLE[11] = "banner_orange";
        MAP_DECORATION_CONVERSION_TABLE[12] = "banner_magenta";
        MAP_DECORATION_CONVERSION_TABLE[13] = "banner_light_blue";
        MAP_DECORATION_CONVERSION_TABLE[14] = "banner_yellow";
        MAP_DECORATION_CONVERSION_TABLE[15] = "banner_lime";
        MAP_DECORATION_CONVERSION_TABLE[16] = "banner_pink";
        MAP_DECORATION_CONVERSION_TABLE[17] = "banner_gray";
        MAP_DECORATION_CONVERSION_TABLE[18] = "banner_light_gray";
        MAP_DECORATION_CONVERSION_TABLE[19] = "banner_cyan";
        MAP_DECORATION_CONVERSION_TABLE[20] = "banner_purple";
        MAP_DECORATION_CONVERSION_TABLE[21] = "banner_blue";
        MAP_DECORATION_CONVERSION_TABLE[22] = "banner_brown";
        MAP_DECORATION_CONVERSION_TABLE[23] = "banner_green";
        MAP_DECORATION_CONVERSION_TABLE[24] = "banner_red";
        MAP_DECORATION_CONVERSION_TABLE[25] = "banner_black";
        MAP_DECORATION_CONVERSION_TABLE[26] = "red_x";
        MAP_DECORATION_CONVERSION_TABLE[27] = "village_desert";
        MAP_DECORATION_CONVERSION_TABLE[28] = "village_plains";
        MAP_DECORATION_CONVERSION_TABLE[29] = "village_savanna";
        MAP_DECORATION_CONVERSION_TABLE[30] = "village_snowy";
        MAP_DECORATION_CONVERSION_TABLE[31] = "village_taiga";
        MAP_DECORATION_CONVERSION_TABLE[32] = "jungle_temple";
        MAP_DECORATION_CONVERSION_TABLE[33] = "swamp_hut";
    }

    private static String convertMapDecorationId(final int type) {
        return type >= 0 && type < MAP_DECORATION_CONVERSION_TABLE.length ? MAP_DECORATION_CONVERSION_TABLE[type] : MAP_DECORATION_CONVERSION_TABLE[0];
    }

    private static void convertBlockStateProperties(final MapType<String> properties) {
        // convert values stored as boolean/integer to string
        for (final String key : properties.keys()) {
            final Object value = properties.getGeneric(key);
            if (value instanceof Number number) {
                if (BOOLEAN_BLOCK_STATE_PROPERTIES.contains(key)) {
                    properties.setString(key, Boolean.toString(number.byteValue() != (byte)0));
                } else {
                    properties.setString(key, number.toString());
                }
            }
        }
    }

    private static void convertTileEntity(final MapType<String> tileEntity, final TransientItemStack transientItem) {
        final Object lock = tileEntity.getGeneric("Lock");
        if (lock != null) {
            tileEntity.remove("Lock");
            transientItem.componentSetGeneric("minecraft:lock", lock);
        }

        final Object lootTable = tileEntity.getGeneric("LootTable");
        if (lootTable != null) {
            final MapType<String> containerLoot = tileEntity.getTypeUtil().createEmptyMap();
            transientItem.componentSetMap("minecraft:container_loot", containerLoot);

            containerLoot.setGeneric("loot_table", lootTable);

            final long seed = tileEntity.getLong("LootTableSeed", 0L);
            if (seed != 0L) {
                containerLoot.setLong("seed", seed);
            }

            tileEntity.remove("LootTable");
            tileEntity.remove("LootTableSeed");
        }

        final String id = NamespaceUtil.correctNamespace(tileEntity.getString("id", ""));

        switch (id) {
            case "minecraft:skull": {
                final Object noteBlockSound = tileEntity.getGeneric("note_block_sound");
                if (noteBlockSound != null) {
                    tileEntity.remove("note_block_sound");
                    transientItem.componentSetGeneric("minecraft:note_block_sound", noteBlockSound);
                }

                break;
            }
            case "minecraft:decorated_pot": {
                final Object sherds = tileEntity.getGeneric("sherds");
                if (sherds != null) {
                    tileEntity.remove("sherds");
                    transientItem.componentSetGeneric("minecraft:pot_decorations", sherds);
                }

                final Object item = tileEntity.getGeneric("item");
                if (item != null) {
                    tileEntity.remove("item");

                    final ListType container = tileEntity.getTypeUtil().createEmptyList();
                    transientItem.componentSetList("minecraft:container", container);

                    final MapType<String> wrappedItem = tileEntity.getTypeUtil().createEmptyMap();
                    container.addMap(wrappedItem);

                    wrappedItem.setInt("slot", 0);
                    wrappedItem.setGeneric("item", item);
                }

                break;
            }
            case "minecraft:banner": {
                final Object patterns = tileEntity.getGeneric("patterns");
                if (patterns != null) {
                    tileEntity.remove("patterns");

                    transientItem.componentSetGeneric("minecraft:banner_patterns", patterns);
                }

                final Number base = tileEntity.getNumber("Base");
                if (base != null) {
                    tileEntity.remove("Base");

                    transientItem.componentSetString("minecraft:base_color", V3818.getBannerColour(base.intValue()));
                }

                break;
            }

            case "minecraft:shulker_box":
            case "minecraft:chest":
            case "minecraft:trapped_chest":
            case "minecraft:furnace":
            case "minecraft:ender_chest":
            case "minecraft:dispenser":
            case "minecraft:dropper":
            case "minecraft:brewing_stand":
            case "minecraft:hopper":
            case "minecraft:barrel":
            case "minecraft:smoker":
            case "minecraft:blast_furnace":
            case "minecraft:campfire":
            case "minecraft:chiseled_bookshelf":
            case "minecraft:crafter": {
                final ListType items = tileEntity.getList("Items", ObjectType.MAP);
                tileEntity.remove("Items");
                if (items != null && items.size() > 0) {
                    transientItem.componentSetList("minecraft:container", items);

                    for (int i = 0, len = items.size(); i < len; ++i) {
                        final MapType<String> item = items.getMap(i);
                        final int slot = (int)item.getByte("Slot", (byte)0) & 0xFF;
                        item.remove("Slot");

                        final MapType<String> wrappedItem = item.getTypeUtil().createEmptyMap();
                        items.setMap(i, wrappedItem);

                        wrappedItem.setInt("slot", slot);
                        wrappedItem.setMap("item", item);
                    }
                }

                break;
            }

            case "minecraft:beehive": {
                final Object bees = tileEntity.getGeneric("bees");
                if (bees != null) {
                    tileEntity.remove("bees");

                    transientItem.componentSetGeneric("minecraft:bees", bees);
                }
                break;
            }
        }
    }

    private static void convertEnchantments(final TransientItemStack transientItem, final TypeUtil type,
                                            final String tagKey, final String componentKey,
                                            final boolean hideToolTip) {
        final ListType enchantments = transientItem.tagRemoveList(tagKey, ObjectType.MAP);
        if (enchantments == null || enchantments.size() == 0) {
            if (hideToolTip) {
                final MapType<String> newEnchants = type.createEmptyMap();
                transientItem.componentSetMap(componentKey, newEnchants);

                newEnchants.setMap("levels", type.createEmptyMap());
                newEnchants.setBoolean("show_in_tooltip", false);
            }
        } else {
            final MapType<String> newEnchantments = type.createEmptyMap();

            for (int i = 0, len = enchantments.size(); i < len; ++i) {
                final MapType<String> enchantment = enchantments.getMap(i);

                final String id = enchantment.getString("id");
                final Number level = enchantment.getNumber("lvl");

                if (id == null || level == null) {
                    continue;
                }

                newEnchantments.setInt(id, Mth.clamp(level.intValue(), 0, 0xFF));
            }

            if (!newEnchantments.isEmpty() || hideToolTip) {
                final MapType<String> newEnchants = type.createEmptyMap();
                transientItem.componentSetMap(componentKey, newEnchants);

                newEnchants.setMap("levels", newEnchantments);
                if (hideToolTip) {
                    newEnchants.setBoolean("show_in_tooltip", false);
                }
            }
        }

        if (enchantments != null && enchantments.size() == 0) {
            transientItem.componentSetBoolean("minecraft:enchantment_glint_override", true);
        }
    }

    private static void convertDisplay(final TransientItemStack transientItem, final TypeUtil type, final int flags) {
        final MapType<String> display = transientItem.tag.getMap("display");

        if (display != null) {
            final Object name = display.getGeneric("Name");
            if (name != null) {
                display.remove("Name");

                transientItem.componentSetGeneric("minecraft:custom_name", name);
            }

            final Object lore = display.getGeneric("Lore");
            if (lore != null) {
                display.remove("Lore");

                transientItem.componentSetGeneric("minecraft:lore", lore);
            }
        }

        final Number color = display == null ? null : display.getNumber("color");
        final boolean hideDye = (flags & TOOLTIP_FLAG_HIDE_DYE) != 0;

        if (hideDye || color != null) {
            if (color != null) {
                display.remove("color");
            }

            final MapType<String> dyedColor = type.createEmptyMap();
            transientItem.componentSetMap("minecraft:dyed_color", dyedColor);

            dyedColor.setInt("rgb", color == null ? DEFAULT_LEATHER_COLOUR : color.intValue());
            if (hideDye) {
                dyedColor.setBoolean("show_in_tooltip", false);
            }
        }

        final Object locName = display == null ? null : display.getGeneric("LocName");
        if (locName != null) {
            display.remove("LocName");

            if (locName instanceof String locNameString) {
                transientItem.componentSetString("minecraft:item_name", ComponentUtils.createTranslatableComponent(locNameString));
            }
        }

        if (display != null && "minecraft:filled_map".equals(transientItem.id)) {
            final Object mapColor = display.getGeneric("MapColor");
            if (mapColor != null) {
                display.remove("MapColor");

                transientItem.componentSetGeneric("minecraft:map_color", mapColor);
            }
        }

        // mirror behavior of fixSubTag
        if (display != null && display.isEmpty()) {
            transientItem.tagRemoveMap("display");
        }
    }

    public static MapType<String> convertBlockStatePredicate(final String value, final TypeUtil type) {
        final int propertyStart = value.indexOf('[');
        final int nbtStart = value.indexOf('{');
        int blockNameEnd = value.length();

        if (propertyStart != -1) {
            blockNameEnd = propertyStart;
        }
        if (nbtStart != -1) {
            blockNameEnd = Math.min(blockNameEnd, nbtStart);
        }

        final MapType<String> ret = type.createEmptyMap();

        final String blockName = value.substring(0, blockNameEnd);

        // string is fine here, the underlying type accepts string AND list under the same name...
        ret.setString("blocks", blockName.trim());

        if (propertyStart != -1) {
            // unlike DFU, set the fromIndex so that on malformed data we do not IOOBE
            final int propertyEnd = value.indexOf(']', propertyStart + 1);
            if (propertyEnd != -1) {
                final MapType<String> state = type.createEmptyMap();
                ret.setMap("state", state);

                for (final String property : value.substring(propertyStart + 1, propertyEnd).split(",")) {
                    final int separatorIdx = property.indexOf('=');
                    if (separatorIdx == -1) {
                        continue;
                    }

                    final String propertyKey = property.substring(0, separatorIdx).trim();
                    final String propertyValue = property.substring(separatorIdx + 1);

                    state.setString(propertyKey, propertyValue);
                }
            }
        }

        if (nbtStart != -1) {
            // unlike DFU, set the fromIndex so that on malformed data we do not IOOBE
            final int nbtEnd = value.indexOf('}', nbtStart + 1);
            if (nbtEnd != -1) {
                // note: want to include { and }
                ret.setString("nbt", value.substring(nbtStart, nbtEnd + 1));
            }
        }

        return ret;
    }

    private static void convertBlockStatePredicates(final TransientItemStack item, final TypeUtil type,
                                                    final String tagKey, final String componentKey,
                                                    final boolean hideInTooltip) {
        final ListType blocks = item.tagRemoveListUnchecked(tagKey);
        if (blocks == null) {
            return;
        }

        final MapType<String> blockPredicates = type.createEmptyMap();
        item.componentSetMap(componentKey, blockPredicates);

        if (hideInTooltip) {
            blockPredicates.setBoolean("show_in_tooltip", false);
        }

        final ListType predicates = type.createEmptyList();
        blockPredicates.setList("predicates", predicates);

        for (int i = 0, len = blocks.size(); i < len; ++i) {
            final Object block = blocks.getGeneric(i);
            if (!(block instanceof String blockString)) {
                // cannot type error here, if block is not a string then nothing in `blocks` is as they have the same type
                predicates.addGeneric(block);
                continue;
            }

            final MapType<String> predicate = convertBlockStatePredicate(blockString, type);

            predicates.addMap(predicate);
        }
    }

    private static void convertAdventureMode(final TransientItemStack item, final TypeUtil type, final int flags) {
        convertBlockStatePredicates(
                item, type, "CanDestroy", "minecraft:can_break",
                (flags & TOOLTIP_FLAG_HIDE_CAN_DESTROY) != 0
        );
        convertBlockStatePredicates(
                item, type, "CanPlaceOn", "minecraft:can_place_on",
                (flags & TOOLTIP_FLAG_HIDE_CAN_PLACE) != 0
        );
    }

    private static void copy(final MapType<String> src, final String srcKey, final MapType<String> dst, final String dstKey) {
        if (src == null || dst == null) {
            return;
        }

        final Object srcValue = src.getGeneric(srcKey);
        if (srcValue != null) {
            dst.setGeneric(dstKey, srcValue);
        }
    }

    private static MapType<String> convertAttribute(final Object inputGeneric, final TypeUtil type) {
        final MapType<String> input = inputGeneric instanceof MapType<?> casted ? (MapType<String>)casted : null;

        final MapType<String> ret = type.createEmptyMap();
        ret.setString("name", "");
        ret.setDouble("amount", 0.0);
        ret.setString("operation", "add_value");

        copy(input, "AttributeName", ret, "type");
        copy(input, "Slot", ret, "slot");
        copy(input, "UUID", ret, "uuid");
        copy(input, "Name", ret, "name");
        copy(input, "Amount", ret, "amount");

        // note: no type check on hasKey
        if (input != null && input.hasKey("Operation")) {
            final String operation;
            switch (input.getInt("Operation", 0)) {
                case 1: {
                    operation = "add_multiplied_base";
                    break;
                }
                case 2: {
                    operation = "add_multiplied_total";
                    break;
                }
                default: {
                    operation = "add_value";
                    break;
                }
            }
            ret.setString("operation", operation);
        }

        return ret;
    }

    private static void convertAttributes(final TransientItemStack item, final TypeUtil type, final int flags) {
        final ListType attributes = item.tagRemoveListUnchecked("AttributeModifiers");
        final ListType newAttributes = type.createEmptyList();

        if (attributes != null) {
            for (int i = 0, len = attributes.size(); i < len; ++i) {
                newAttributes.addMap(convertAttribute(attributes.getGeneric(i), type));
            }
        }

        if (newAttributes.size() > 0) {
            final MapType<String> newModifiers = type.createEmptyMap();
            item.componentSetMap("minecraft:attribute_modifiers", newModifiers);

            newModifiers.setList("modifiers", newAttributes);

            final boolean hideModifiers = (flags & TOOLTIP_FLAG_HIDE_MODIFIERS) != 0;
            if (hideModifiers) {
                newModifiers.setBoolean("show_in_tooltip", false);
            }
        }
    }

    private static void convertMap(final TransientItemStack item, final TypeUtil type) {
        item.tagMigrateToComponent("map", "minecraft:map_id");

        final ListType decorations = item.tagRemoveListUnchecked("Decorations");
        if (decorations != null) {
            final MapType<String> newDecorations = type.createEmptyMap();

            for (int i = 0, len = decorations.size(); i < len; ++i) {
                final Object decorationGeneric = decorations.getGeneric(i);

                final MapType<String> decoration = decorationGeneric instanceof MapType<?> casted ? (MapType<String>)casted : null;

                // note: getForcedString mirrors DFU converting to string for key
                final String id = decoration == null ? "" : decoration.getForcedString("id", "");
                if (newDecorations.hasKey(id)) {
                    // note: never replace existing decorations by id
                    continue;
                }

                final int typeId = decoration == null ? 0 : decoration.getInt("type", 0);
                final double x = decoration == null ? 0.0 : decoration.getDouble("x", 0.0);
                final double z = decoration == null ? 0.0 : decoration.getDouble("z", 0.0);
                final float rot = decoration == null ? 0.0f : (float)decoration.getDouble("rot", 0.0);

                final MapType<String> newDecoration = type.createEmptyMap();
                newDecorations.setMap(id, newDecoration);

                newDecoration.setString("type", convertMapDecorationId(typeId));
                newDecoration.setDouble("x", x);
                newDecoration.setDouble("z", z);
                newDecoration.setFloat("rotation", rot);
            }

            if (!newDecorations.isEmpty()) {
                item.componentSetMap("minecraft:map_decorations", newDecorations);
            }
        }
    }

    private static void convertPotion(final TransientItemStack item, final TypeUtil type) {
        final MapType<String> potionContents = type.createEmptyMap();

        final String potion = item.tagRemoveString("Potion");

        if (potion != null && !"minecraft:empty".equals(potion)) {
            potionContents.setString("potion", potion);
        }

        item.migrateTagTo("CustomPotionColor", potionContents, "custom_color");
        item.migrateTagTo("custom_potion_effects", potionContents, "custom_effects");

        if (!potionContents.isEmpty()) {
            item.componentSetMap("minecraft:potion_contents", potionContents);
        }
    }

    private static MapType<String> makeFilteredText(final String raw, final String filtered, final TypeUtil type) {
        final MapType<String> ret = type.createEmptyMap();

        ret.setString("raw", raw);
        if (filtered != null) {
            ret.setString("filtered", filtered);
        }

        return ret;
    }

    private static ListType convertBookPages(final TransientItemStack item, final TypeUtil type) {
        final ListType oldPages = item.tagRemoveListUnchecked("pages");

        final MapType<String> filteredPages = item.tagRemoveMap("filtered_pages");

        if (oldPages == null || oldPages.size() == 0) {
            return null;
        }

        final ListType ret = type.createEmptyList();

        for (int i = 0, len = oldPages.size(); i < len; ++i) {
            final String page = oldPages.getGeneric(i) instanceof String str ? str : "";
            final String filtered = filteredPages == null ? null : filteredPages.getString(Integer.toString(i));

            ret.addMap(makeFilteredText(page, filtered, type));
        }

        return ret;
    }

    private static void convertWritableBook(final TransientItemStack item, final TypeUtil type) {
        final ListType pages = convertBookPages(item, type);
        if (pages != null) {
            final MapType<String> bookContent = type.createEmptyMap();
            item.componentSetMap("minecraft:writable_book_content", bookContent);

            bookContent.setList("pages", pages);
        }
    }

    private static void convertWrittenBook(final TransientItemStack item, final TypeUtil type) {
        final ListType pages = convertBookPages(item, type);

        final MapType<String> bookContent = type.createEmptyMap();
        item.componentSetMap("minecraft:written_book_content", bookContent);
        if (pages != null) {
            bookContent.setList("pages", pages);
        }

        final String title = item.tagRemoveString("title", "");
        final String filteredTitle = item.tagRemoveString("filtered_title");

        bookContent.setMap("title", makeFilteredText(title, filteredTitle, type));

        item.migrateTagTo("author", bookContent, "author");
        item.migrateTagTo("resolved", bookContent, "resolved");
        item.migrateTagTo("generation", bookContent, "generation");
    }

    private static void convertMobBucket(final TransientItemStack item, final TypeUtil type) {
        final MapType<String> bucketEntityData = type.createEmptyMap();

        for (final String oldKey : BUCKETED_MOB_TAGS) {
            item.migrateTagTo(oldKey, bucketEntityData, oldKey);
        }

        if (!bucketEntityData.isEmpty()) {
            item.componentSetMap("minecraft:bucket_entity_data", bucketEntityData);
        }
    }

    private static void convertCompass(final TransientItemStack item, final TypeUtil type) {
        final Object lodestonePos = item.tagRemoveGeneric("LodestonePos");
        final Object lodestoneDim = item.tagRemoveGeneric("LodestoneDimension");

        if (lodestonePos == null && lodestoneDim == null) {
            return;
        }

        final MapType<String> lodestoneTracker = type.createEmptyMap();
        item.componentSetMap("minecraft:lodestone_tracker", lodestoneTracker);

        if (lodestonePos != null && lodestoneDim != null) {
            final MapType<String> target = type.createEmptyMap();
            lodestoneTracker.setMap("target", target);

            target.setGeneric("pos", lodestonePos);
            target.setGeneric("dimension", lodestoneDim);
        }

        final boolean tracked = item.tagRemoveBoolean("LodestoneTracked", true);
        if (!tracked) {
            lodestoneTracker.setBoolean("tracked", false);
        }
    }

    private static void convertFireworkExplosion(final Object inputGeneric) {
        if (!(inputGeneric instanceof MapType<?>)) {
            return;
        }

        final MapType<String> input = (MapType<String>)inputGeneric;

        RenameHelper.renameSingle(input, "Colors", "colors");
        RenameHelper.renameSingle(input, "FadeColors", "fade_colors");
        RenameHelper.renameSingle(input, "Trail", "has_trail");
        RenameHelper.renameSingle(input, "Flicker", "has_twinkle");

        final int type = input.getInt("Type", 0);
        input.remove("Type");

        final String newType;
        switch (type) {
            case 1: {
                newType = "large_ball";
                break;
            }
            case 2: {
                newType = "star";
                break;
            }
            case 3: {
                newType = "creeper";
                break;
            }
            case 4: {
                newType = "burst";
                break;
            }
            default: {
                newType = "small_ball";
                break;
            }
        }

        input.setString("shape", newType);
    }

    private static void convertFireworkRocket(final TransientItemStack item, final TypeUtil type) {
        // adhere to fixSubTag(true) behavior
        final Object fireworksGeneric = item.tag.getGeneric("Fireworks");
        if (fireworksGeneric == null) {
            return;
        }

        if (!(fireworksGeneric instanceof MapType<?>)) {
            final MapType<String> newFireworks = type.createEmptyMap();
            item.componentSetMap("minecraft:fireworks", newFireworks);

            newFireworks.setList("explosions", type.createEmptyList());
            newFireworks.setByte("flight_duration", (byte)0);

            return;
        }

        final MapType<String> fireworks = (MapType<String>)fireworksGeneric;

        final MapType<String> newFireworks = type.createEmptyMap();
        item.componentSetMap("minecraft:fireworks", newFireworks);

        final int flight = fireworks.getInt("Flight", 0);
        newFireworks.setByte("flight_duration", (byte)flight);

        final ListType explosions = fireworks.getListUnchecked("Explosions", type.createEmptyList());
        newFireworks.setList("explosions", explosions);

        for (int i = 0, len = explosions.size(); i < len; ++i) {
            convertFireworkExplosion(explosions.getGeneric(i));
        }

        fireworks.remove("Explosions");
        fireworks.remove("Flight");
        if (fireworks.isEmpty()) {
            item.tag.remove("Fireworks");
        }
    }

    private static Object copyGeneric(final Object value, final TypeUtil type) {
        if (value == null || value instanceof Number || value instanceof String) {
            return value;
        }
        if (value instanceof MapType<?> mapType) {
            return mapType.copy();
        }
        if (value instanceof ListType listType) {
            return listType.copy();
        }
        // rest of the cases can take the slow path

        final ListType dummy = type.createEmptyList();
        dummy.addGeneric(value);

        return dummy.copy().getGeneric(0);
    }

    private static void convertFireworkStar(final TransientItemStack item, final TypeUtil type) {
        // note: adhere to fixSubTag(true) behavior
        final Object explosionGeneric = item.tag.getGeneric("Explosion");
        if (explosionGeneric == null) {
            return;
        }

        if (!(explosionGeneric instanceof MapType<?>)) {
            // important that we copy the generic value when not moving it
            item.componentSetGeneric("minecraft:firework_explosion", copyGeneric(explosionGeneric, type));
            return;
        }

        final MapType<String> explosion = (MapType<String>)explosionGeneric;

        final MapType<String> explosionCopy = explosion.copy();
        item.componentSetGeneric("minecraft:firework_explosion", explosionCopy);
        convertFireworkExplosion(explosionCopy);

        explosion.remove("Type");
        explosion.remove("Colors");
        explosion.remove("FadeColors");
        explosion.remove("Trail");
        explosion.remove("Flicker");

        if (explosion.isEmpty()) {
            item.tag.remove("Explosion");
        }
    }

    private static boolean isValidPlayerName(final String name) {
        if (name.length() > 16) {
            return false;
        }

        for (int i = 0, len = name.length(); i < len; ++i) {
            final char character = name.charAt(i);
            if (character <= 0x20 || character >= 0x7F) { // printable ascii
                return false;
            }
        }

        return true;
    }

    private static ListType convertProperties(final MapType<String> properties, final TypeUtil type) {
        final ListType ret = type.createEmptyList();

        for (final String propertyKey : properties.keys()) {
            final ListType propertyValues = properties.getListUnchecked(propertyKey);

            if (propertyValues == null) {
                continue;
            }

            for (int i = 0, len = propertyValues.size(); i < len; ++i) {
                final MapType<String> property = propertyValues.getGeneric(i) instanceof MapType<?> casted ? (MapType<String>)casted : null;

                final String value = property == null ? "" : property.getString("Value", "");
                final String signature = property == null ? null : property.getString("Signature");

                final MapType<String> newProperty = type.createEmptyMap();
                ret.addMap(newProperty);

                newProperty.setString("name", propertyKey);
                newProperty.setString("value", value);
                if (signature != null) {
                    newProperty.setString("signature", signature);
                }
            }
        }

        return ret;
    }

    public static MapType<String> convertProfile(final Object inputGeneric, final TypeUtil type) {
        final MapType<String> ret = type.createEmptyMap();

        if (inputGeneric instanceof String name) {
            if (!isValidPlayerName(name)) {
                return ret;
            }

            ret.setString("name", name);

            return ret;
        }

        final MapType<String> input = inputGeneric instanceof MapType<?> casted ? (MapType<String>)casted : null;

        final String name = input == null ? "" : input.getString("Name", "");

        if (isValidPlayerName(name)) {
            ret.setString("name", name);
        }

        final Object id = input == null ? null : input.getGeneric("Id");

        if (id != null) {
            ret.setGeneric("id", id);
        }

        final MapType<String> properties = input == null ? null : input.getMap("Properties");
        if (properties != null && !properties.isEmpty()) {
            ret.setList("properties", convertProperties(properties, type));
        }

        return ret;
    }

    private static void convertSukll(final TransientItemStack item, final TypeUtil type) {
        final Object skullOwnerGeneric = item.tagRemoveGeneric("SkullOwner");
        if (skullOwnerGeneric == null) {
            return;
        }

        item.componentSetMap("minecraft:profile", convertProfile(skullOwnerGeneric, type));
    }

    // input is unmodified
    public static MapType<String> convertItem(final MapType<String> input) {
        if (!input.hasKey("id", ObjectType.STRING) || !input.hasKey("Count", ObjectType.NUMBER)) {
            return input.copy();
        }

        final TypeUtil type = input.getTypeUtil();

        final TransientItemStack item = new TransientItemStack(input);

        item.tagMigrateToComponent("Damage", "minecraft:damage", 0);
        item.tagMigrateToComponent("RepairCost", "minecraft:repair_cost", 0);
        item.tagMigrateToComponent("CustomModelData", "minecraft:custom_model_data");

        final MapType<String> blockStateProperties = item.tagRemoveMap("BlockStateTag");
        if (blockStateProperties != null) {
            item.componentSetMap("minecraft:block_state", blockStateProperties);
            convertBlockStateProperties(blockStateProperties);
        }

        item.tagMigrateToComponent("EntityTag", "minecraft:entity_data");

        final MapType<String> tileEntityTag = item.tagRemoveMap("BlockEntityTag");
        if (tileEntityTag != null) {
            convertTileEntity(tileEntityTag, item);

            if (tileEntityTag.size() > 1 || (tileEntityTag.size() == 1 && !tileEntityTag.hasKey("id"))) {
                item.componentSetMap("minecraft:block_entity_data", tileEntityTag);
            }
        }

        final int flags = item.tagRemoveInt("HideFlags", 0);

        if (item.tagRemoveInt("Unbreakable", 0) != 0) {
            final MapType<String> unbreakable = type.createEmptyMap();
            item.componentSetMap("minecraft:unbreakable", unbreakable);
            if ((flags & TOOLTIP_FLAG_HIDE_UNBREAKABLE) != 0) {
                unbreakable.setBoolean("show_in_tooltip", false);
            }
        }

        convertEnchantments(
                item, type, "Enchantments", "minecraft:enchantments",
                (flags & TOOLTIP_FLAG_HIDE_ENCHANTMENTS) != 0
        );

        convertDisplay(item, type, flags);
        convertAdventureMode(item, type, flags);
        convertAttributes(item, type, flags);

        final Object trim = item.tagRemoveGeneric("Trim");
        if (trim != null) {
            // note: DFU set does nothing if not map
            if ((flags & TOOLTIP_FLAG_HIDE_UPGRADES) != 0 && trim instanceof MapType) {
                ((MapType<String>)trim).setBoolean("show_in_tooltip", false);
            }

            item.componentSetGeneric("minecraft:trim", trim);
        }

        if ((flags & TOOLTIP_FLAG_HIDE_ADDITIONAL) != 0) {
            item.componentSetMap("minecraft:hide_additional_tooltip", type.createEmptyMap());
        }

        switch (item.id) {
            case "minecraft:enchanted_book": {
                convertEnchantments(
                        item, type, "StoredEnchantments", "minecraft:stored_enchantments",
                        (flags & TOOLTIP_FLAG_HIDE_ADDITIONAL) != 0
                );
                break;
            }
            case "minecraft:crossbow": {
                item.tagRemoveGeneric("Charged");
                item.tagMigrateNonEmptyListToComponent("ChargedProjectiles", "minecraft:charged_projectiles");
                break;
            }
            case "minecraft:bundle": {
                item.tagMigrateNonEmptyListToComponent("Items", "minecraft:bundle_contents");
                break;
            }
            case "minecraft:filled_map": {
                convertMap(item, type);
                break;
            }
            case "minecraft:potion":
            case "minecraft:splash_potion":
            case "minecraft:lingering_potion":
            case "minecraft:tipped_arrow": {
                convertPotion(item, type);
                break;
            }
            case "minecraft:writable_book": {
                convertWritableBook(item, type);
                break;
            }
            case "minecraft:written_book": {
                convertWrittenBook(item, type);
                break;
            }
            case "minecraft:suspicious_stew": {
                item.tagMigrateToComponent("effects", "minecraft:suspicious_stew_effects");
                break;
            }
            case "minecraft:debug_stick": {
                item.tagMigrateToComponent("DebugProperty", "minecraft:debug_stick_state");
                break;
            }
            case "minecraft:pufferfish_bucket":
            case "minecraft:salmon_bucket":
            case "minecraft:cod_bucket":
            case "minecraft:tropical_fish_bucket":
            case "minecraft:axolotl_bucket":
            case "minecraft:tadpole_bucket": {
                convertMobBucket(item, type);
                break;
            }
            case "minecraft:goat_horn": {
                item.tagMigrateToComponent("instrument", "minecraft:instrument");
                break;
            }
            case "minecraft:knowledge_book": {
                item.tagMigrateToComponent("Recipes", "minecraft:recipes");
                break;
            }
            case "minecraft:compass": {
                convertCompass(item, type);
                break;
            }
            case "minecraft:firework_rocket": {
                convertFireworkRocket(item, type);
                break;
            }
            case "minecraft:firework_star": {
                convertFireworkStar(item, type);
                break;
            }
            case "minecraft:player_head": {
                convertSukll(item, type);
                break;
            }
        }

        return item.serialize();
    }

    private ConverterItemStackToDataComponents() {}

    private static final class TransientItemStack {

        private final String id;
        private final int count;

        private final MapType<String> components;
        private final MapType<String> tag;
        private final MapType<String> root;

        public TransientItemStack(final MapType<String> root) {
            this.id = root.getString("id");
            this.count = root.getInt("Count");

            final TypeUtil type = root.getTypeUtil();

            this.components = type.createEmptyMap();

            final MapType<String> rootCopy = root.copy();

            final MapType<String> tag = rootCopy.getMap("tag");

            rootCopy.remove("id");
            rootCopy.remove("Count");
            rootCopy.remove("tag");

            this.tag = tag == null ? type.createEmptyMap() : tag;

            this.root = rootCopy;
        }

        public void migrateTagTo(final String tagKey, final MapType<String> dst, final String dstKey) {
            final Object value = this.tag.getGeneric(tagKey);

            if (value != null) {
                this.tag.remove(tagKey);

                dst.setGeneric(dstKey, value);
            }
        }

        public String tagRemoveString(final String key) {
            final String ret = this.tag.getString(key);

            this.tag.remove(key);

            return ret;
        }

        public String tagRemoveString(final String key, final String dfl) {
            final String ret = this.tag.getString(key, dfl);

            this.tag.remove(key);

            return ret;
        }

        public ListType tagRemoveListUnchecked(final String key) {
            final ListType ret = this.tag.getListUnchecked(key);

            this.tag.remove(key);

            return ret;
        }

        public ListType tagRemoveList(final String key, final ObjectType listType) {
            final ListType ret = this.tag.getList(key, listType);

            this.tag.remove(key);

            return ret;
        }

        public MapType<String> tagRemoveMap(final String key) {
            final MapType<String> ret = this.tag.getMap(key);

            this.tag.remove(key);

            return ret;
        }

        public boolean tagRemoveBoolean(final String key, final boolean dfl) {
            final boolean ret = this.tag.getBoolean(key, dfl);

            this.tag.remove(key);

            return ret;
        }

        public int tagRemoveInt(final String key, final int dfl) {
            final int ret = this.tag.getInt(key, dfl);

            this.tag.remove(key);

            return ret;
        }

        public Object tagRemoveGeneric(final String key) {
            final Object ret = this.tag.getGeneric(key);

            if (ret != null) {
                this.tag.remove(key);
                return ret;
            }

            return ret;
        }

        public void tagMigrateToComponent(final String tagKey, final String componentKey) {
            final Object value = this.tag.getGeneric(tagKey);
            if (value != null) {
                this.tag.remove(tagKey);

                this.components.setGeneric(componentKey, value);
            }
        }

        public void tagMigrateNonEmptyListToComponent(final String tagKey, final String componentKey) {
            final Object value = this.tag.getGeneric(tagKey);
            if (value != null) {
                this.tag.remove(tagKey);

                if (!(value instanceof ListType list) || list.size() > 0) {
                    this.components.setGeneric(componentKey, value);
                }
            }
        }

        public void tagMigrateToComponent(final String tagKey, final String componentKey, final int defaultComponent) {
            final int value = this.tag.getInt(tagKey, defaultComponent);
            this.tag.remove(tagKey);

            if (value != defaultComponent) {
                this.components.setGeneric(componentKey, value);
            }
        }

        public void componentSetBoolean(final String key, final boolean value) {
            this.components.setBoolean(key, value);
        }

        public void componentSetString(final String key, final String value) {
            this.components.setString(key, value);
        }

        public void componentSetList(final String key, final ListType value) {
            this.components.setList(key, value);
        }

        public void componentSetMap(final String key, final MapType<String> value) {
            this.components.setMap(key, value);
        }

        public void componentSetGeneric(final String key, final Object value) {
            this.components.setGeneric(key, value);
        }

        public MapType<String> serialize() {
            final MapType<String> ret = this.components.getTypeUtil().createEmptyMap();

            ret.setString("id", this.id);
            ret.setInt("count", this.count);
            if (!this.tag.isEmpty()) {
                this.components.setMap("minecraft:custom_data", this.tag);
            }

            if (!this.components.isEmpty()) {
                ret.setMap("components", this.components);
            }

            // merge root to ret, with entries in ret taking priority
            if (!this.root.isEmpty()) {
                for (final String key : this.root.keys()) {
                    if (ret.hasKey(key)) {
                        continue;
                    }

                    ret.setGeneric(key, this.root.getGeneric(key));
                }
            }

            return ret;
        }
    }
}
