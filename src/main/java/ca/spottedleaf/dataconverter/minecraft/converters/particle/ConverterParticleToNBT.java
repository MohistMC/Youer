package ca.spottedleaf.dataconverter.minecraft.converters.particle;

import ca.spottedleaf.dataconverter.types.ListType;
import ca.spottedleaf.dataconverter.types.MapType;
import ca.spottedleaf.dataconverter.types.TypeUtil;
import ca.spottedleaf.dataconverter.types.nbt.NBTMapType;
import ca.spottedleaf.dataconverter.util.NamespaceUtil;
import com.mojang.brigadier.StringReader;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import com.mojang.logging.LogUtils;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.TagParser;
import net.minecraft.util.Mth;
import org.slf4j.Logger;

public final class ConverterParticleToNBT {

    private static final Logger LOGGER = LogUtils.getLogger();

    private static CompoundTag parseNBT(final String flat) {
        try {
            return TagParser.parseTag(flat);
        } catch (final Exception ex) {
            LOGGER.warn("Failed to parse nbt: " + flat, ex);
            return null;
        }
    }

    private static void convertItem(final MapType<String> nbt, final String data) {
        final MapType<String> itemNBT = nbt.getTypeUtil().createEmptyMap();
        nbt.setMap("item", itemNBT);
        itemNBT.setInt("Count", 1);

        final int nbtStart = data.indexOf('{');
        if (nbtStart == -1) {
            // assume data is item name
            itemNBT.setString("id", NamespaceUtil.correctNamespace(data));
            return;
        }
        // itemname{tagNBT}
        itemNBT.setString("id", NamespaceUtil.correctNamespace(data.substring(0, nbtStart)));

        final CompoundTag tag = parseNBT(data.substring(nbtStart));
        if (tag != null) {
            // do we need to worry about type conversion?
            itemNBT.setMap("tag", new NBTMapType(tag));
        }
    }

    private static MapType<String> parseProperties(final String input, final TypeUtil type) {
        final MapType<String> ret = type.createEmptyMap();
        try {
            // format: [p1=v1, p2=v2, p3=v3, ...]
            final StringReader reader = new StringReader(input);

            reader.expect('[');
            reader.skipWhitespace();

            if (reader.canRead() && reader.peek() != ']') {
                while (reader.canRead()) {
                    final String property = reader.readString();
    
                    reader.skipWhitespace();
                    reader.expect('=');
                    reader.skipWhitespace();
    
                    final String value = reader.readString();
                    ret.setString(property, value);
    
                    reader.skipWhitespace();
                    if (reader.canRead()) {
                        if (reader.peek() != ',') {
                            // invalid character or ']'
                            break;
                        }
    
                        // skip ',' and move onto next entry
                        reader.skip();
                    }
                    
                    reader.skipWhitespace();
                }
            }

            reader.expect(']');
            return ret;
        } catch (final Exception ex) {
            LOGGER.warn("Failed to parse block properties: " + input, ex);
            return null;
        }
    }

    private static void convertBlock(final MapType<String> nbt, final String data) {
        final MapType<String> blockNBT = nbt.getTypeUtil().createEmptyMap();
        nbt.setMap("block_state", blockNBT);

        final int propertiesStart = data.indexOf('[');
        if (propertiesStart == -1) {
            // assume data is id
            blockNBT.setString("Name", NamespaceUtil.correctNamespace(data));
            return;
        }
        blockNBT.setString("Name", NamespaceUtil.correctNamespace(data.substring(0, propertiesStart)));

        // blockname{properties}
        final MapType<String> properties = parseProperties(data.substring(propertiesStart), nbt.getTypeUtil());
        if (properties != null && !properties.isEmpty()) {
            blockNBT.setMap("Properties", properties);
        }
    }

    private static ListType parseFloatVector(final StringReader reader, final TypeUtil type) throws CommandSyntaxException {
        final float x = reader.readFloat();

        reader.expect(' ');
        final float y = reader.readFloat();

        reader.expect(' ');
        final float z = reader.readFloat();

        final ListType ret = type.createEmptyList();
        ret.addFloat(x);
        ret.addFloat(y);
        ret.addFloat(z);

        return ret;
    }

    private static void convertDust(final MapType<String> nbt, final String data) {
        try {
            final StringReader reader = new StringReader(data);

            final ListType color = parseFloatVector(reader, nbt.getTypeUtil());

            reader.expect(' ');
            final float scale = reader.readFloat();

            nbt.setList("color", color);
            nbt.setFloat("scale", scale);
        } catch (final Exception ex) {
            LOGGER.warn("Failed to parse dust particle: " + data, ex);
        }
    }

    private static void convertColorDust(final MapType<String> nbt, final String data) {
        try {
            final StringReader reader = new StringReader(data);

            final ListType fromColor = parseFloatVector(reader, nbt.getTypeUtil());

            reader.expect(' ');
            final float scale = reader.readFloat();

            reader.expect(' ');
            final ListType toColor = parseFloatVector(reader, nbt.getTypeUtil());

            nbt.setList("from_color", fromColor);
            nbt.setFloat("scale", scale);
            nbt.setList("to_color", toColor);
        } catch (final Exception ex) {
            LOGGER.warn("Failed to parse color transition dust particle: " + data, ex);
        }
    }

    private static void convertSculk(final MapType<String> nbt, final String data) {
        try {
            final StringReader reader = new StringReader(data);

            final float roll = reader.readFloat();

            nbt.setFloat("roll", roll);
        } catch (final Exception ex) {
            LOGGER.warn("Failed to parse sculk particle: " + data, ex);
        }
    }

    private static void convertVibration(final MapType<String> nbt, final String data) {
        try {
            final StringReader reader = new StringReader(data);

            final double posX = reader.readDouble();

            reader.expect(' ');
            final double posY = reader.readDouble();

            reader.expect(' ');
            final double posZ = reader.readDouble();

            reader.expect(' ');
            final int arrival = reader.readInt();

            nbt.setInt("arrival_in_ticks", arrival);

            final MapType<String> destination = nbt.getTypeUtil().createEmptyMap();
            nbt.setMap("destination", destination);

            destination.setString("type", "minecraft:block");

            final ListType pos = nbt.getTypeUtil().createEmptyList();
            destination.setList("pos", pos);

            pos.addInt(Mth.floor(posX));
            pos.addInt(Mth.floor(posY));
            pos.addInt(Mth.floor(posZ));
        } catch (final Exception ex) {
            LOGGER.warn("Failed to parse vibration particle: " + data, ex);
        }
    }

    private static void convertShriek(final MapType<String> nbt, final String data) {
        try {
            final StringReader reader = new StringReader(data);

            final int delay = reader.readInt();

            nbt.setInt("delay", delay);
        } catch (final Exception ex) {
            LOGGER.warn("Failed to parse shriek particle: " + data, ex);
        }
    }

    public static MapType<String> convert(final String flat, final TypeUtil type) {
        final String[] split = flat.split(" ", 2);
        final String name = NamespaceUtil.correctNamespace(split[0]);

        final MapType<String> ret = type.createEmptyMap();
        ret.setString("type", name);

        if (split.length > 1) {
            final String data = split[1];
            switch (name) {
                case "minecraft:item": {
                    convertItem(ret, data);
                    break;
                }
                case "minecraft:block":
                case "minecraft:block_marker":
                case "minecraft:falling_dust":
                case "minecraft:dust_pillar": {
                    convertBlock(ret, data);
                    break;
                }
                case "minecraft:dust": {
                    convertDust(ret, data);
                    break;
                }
                case "minecraft:dust_color_transition": {
                    convertColorDust(ret, data);
                    break;
                }
                case "minecraft:sculk_charge": {
                    convertSculk(ret, data);
                    break;
                }
                case "minecraft:vibration": {
                    convertVibration(ret, data);
                    break;
                }
                case "minecraft:shriek": {
                    convertShriek(ret, data);
                    break;
                }
            }
        }

        return ret;
    }

    private ConverterParticleToNBT() {}
}
