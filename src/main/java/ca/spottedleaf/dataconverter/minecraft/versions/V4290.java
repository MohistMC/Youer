package ca.spottedleaf.dataconverter.minecraft.versions;

import ca.spottedleaf.dataconverter.converters.DataConverter;
import ca.spottedleaf.dataconverter.minecraft.MCVersions;
import ca.spottedleaf.dataconverter.minecraft.datatypes.MCTypeRegistry;
import ca.spottedleaf.dataconverter.minecraft.walkers.generic.WalkerUtils;
import ca.spottedleaf.dataconverter.types.ListType;
import ca.spottedleaf.dataconverter.types.MapType;
import ca.spottedleaf.dataconverter.types.ObjectType;
import ca.spottedleaf.dataconverter.types.TypeUtil;
import ca.spottedleaf.dataconverter.types.Types;
import ca.spottedleaf.dataconverter.types.nbt.NBTMapType;
import com.google.gson.JsonElement;
import com.google.gson.JsonParseException;
import com.google.gson.JsonParser;
import com.mojang.logging.LogUtils;
import net.minecraft.nbt.TagParser;
import org.slf4j.Logger;

public final class V4290 {

    public static final int VERSION = MCVersions.V1_21_4 + 101;

    private static final Logger LOGGER = LogUtils.getLogger();

    private static void convertNestedList(final ListType components) {
        if (components == null) {
            return;
        }
        for (int i = 0, len = components.size(); i < len; ++i) {
            convertNested(components.getGeneric(i));
        }
    }

    private static void convertNested(final Object component) {
        if (component instanceof ListType listType) {
            convertNestedList(listType);
        } else if (component instanceof MapType root) {
            convertLegacyHover(root);

            convertNestedList(root.getListUnchecked("extra"));
            convertNested(root.getGeneric("separator"));

            final MapType hoverEvent = root.getMap("hoverEvent");
            if (hoverEvent != null) {
                switch (hoverEvent.getString("action", "")) {
                    case "show_text": {
                        convertNested(hoverEvent.getGeneric("contents"));
                        break;
                    }
                    case "show_item": {
                        break;
                    }
                    case "show_entity": {
                        convertNested(hoverEvent.getGeneric("name"));
                        break;
                    }
                    // default: do nothing
                }
            }
        } // else: should only be string
    }

    private static void convertLegacyHover(final MapType textComponent) {
        final TypeUtil<?> typeUtil = textComponent.getTypeUtil();
        final MapType hoverEvent = textComponent.getMap("hoverEvent");
        if (hoverEvent == null) {
            return;
        }

        final Object legacyValue = hoverEvent.getGeneric("value");
        if (legacyValue == null) {
            // nothing to do here
            return;
        }

        switch (hoverEvent.getString("action", "")) {
            case "show_text": {
                // value -> another text component; all we need to do is just move it to contents
                hoverEvent.remove("value");
                hoverEvent.setGeneric("contents", legacyValue);
                break;
            }
            case "show_item": {
                // value -> snbt of serialized item
                hoverEvent.remove("value");
                if (!(legacyValue instanceof String legacyItemStr)) {
                    LOGGER.error("Legacy HoverEvent with action=show_item has invalid value, expected string: " + legacyValue);
                    break;
                }

                final MapType legacyItem;
                try {
                    legacyItem = new NBTMapType(TagParser.parseCompoundFully(legacyItemStr));
                } catch (final Exception ex) {
                    LOGGER.error("Failed to parse SNBT for legacy item HoverEvent: " + legacyItemStr, ex);
                    break;
                }

                // note: blindly take precedence over non-legacy data
                hoverEvent.setMap("contents", legacyItem);
                break;
            }
            case "show_entity": {
                // value -> snbt of {name:<string json text component>,type:<string>,id:<dashed uuid>}
                hoverEvent.remove("value");
                if (!(legacyValue instanceof String legacyEntityStr)) {
                    LOGGER.error("Legacy HoverEvent with action=show_entity has invalid value, expected string: " + legacyValue);
                    break;
                }

                final MapType legacyEntity;
                try {
                    legacyEntity = new NBTMapType(TagParser.parseCompoundFully(legacyEntityStr));
                } catch (final Exception ex) {
                    LOGGER.error("Failed to parse SNBT for legacy entity HoverEvent: " + legacyEntityStr, ex);
                    break;
                }

                final MapType newContents = typeUtil.createEmptyMap();
                // note: blindly take precedence over non-legacy data
                hoverEvent.setMap("contents", newContents);

                final String name = legacyEntity.getString("name");
                if (name != null) {
                    newContents.setString("name", name);
                }
                final String type = legacyEntity.getString("type");
                if (type != null) {
                    newContents.setString("type", type);
                }
                final String id = legacyEntity.getString("id");
                if (id != null) {
                    newContents.setString("id", id);
                }

                break;
            }
            // default: do nothing
        }
    }

    private static void directWalkComponentList(final ListType list, final long fromVersion, final long toVersion) {
        for (int i = 0, len = list.size(); i < len; ++i) {
            directWalkComponent(list.getGeneric(i), fromVersion, toVersion);
        }
    }

    private static void directWalkComponent(final Object input, final long fromVersion, final long toVersion) {
        if (input instanceof ListType listType) {
            directWalkComponentList(listType, fromVersion, toVersion);
        } else if (input instanceof MapType root) {
            final ListType extra = root.getListUnchecked("extra");
            if (extra != null) {
                directWalkComponentList(extra, fromVersion, toVersion);
            }

            final Object separator = root.getGeneric("separator");
            if (separator != null) {
                directWalkComponent(separator, fromVersion, toVersion);
            }

            final MapType clickEvent = root.getMap("clickEvent");
            if (clickEvent != null) {
                switch (clickEvent.getString("action", "")) {
                    case "run_command":
                    case "suggest_command": {
                        WalkerUtils.convert(MCTypeRegistry.DATACONVERTER_CUSTOM_TYPE_COMMAND, clickEvent, "value", fromVersion, toVersion);
                        break;
                    }
                }
            }

            final MapType hoverEvent = root.getMap("hoverEvent");
            if (hoverEvent != null) {
                switch (hoverEvent.getString("action", "")) {
                    case "show_text": {
                        final Object contents = hoverEvent.getGeneric("contents");
                        if (contents != null) {
                            directWalkComponent(contents, fromVersion, toVersion);
                        }
                        break;
                    }
                    case "show_item": {
                        if (hoverEvent.hasKey("contents", ObjectType.STRING)) {
                            WalkerUtils.convert(MCTypeRegistry.ITEM_NAME, hoverEvent, "contents", fromVersion, toVersion);
                        } else {
                            WalkerUtils.convert(MCTypeRegistry.ITEM_STACK, hoverEvent, "contents", fromVersion, toVersion);
                        }
                        break;
                    }
                    case "show_entity": {
                        WalkerUtils.convert(MCTypeRegistry.ENTITY_NAME, hoverEvent, "type", fromVersion, toVersion);

                        final Object name = hoverEvent.getGeneric("name");
                        if (name != null) {
                            directWalkComponent(name, fromVersion, toVersion);
                        }
                        break;
                    }
                    // default: do nothing
                }
            }
        } // else: should only be string
    }

    public static void register() {
        MCTypeRegistry.TEXT_COMPONENT.addStructureConverter(new DataConverter<>(VERSION) {
            @Override
            public Object convert(final Object data, final long sourceVersion, final long toVersion) {
                if (data instanceof ListType) {
                    // will be iterated by walker
                    return null;
                }
                if (data instanceof MapType) {
                    // (probably) iterated by walker
                    return null;
                }
                if (data instanceof Number number) {
                    // text component parsed as list of numbers, which was then iterated by walker
                    // we need to convert to correct type, which is string
                    return number.toString();
                }
                if (data instanceof Boolean bool) {
                    // text component parsed as list of booleans, which was then iterated by walker
                    // we need to convert to correct type, which is string
                    return bool.toString();
                }

                if (!(data instanceof String unparsedJson)) {
                    throw new IllegalArgumentException("Wrong type for text component: " + data);
                }

                try {
                    final JsonElement json = JsonParser.parseString(unparsedJson);

                    if (!json.isJsonNull()) {
                        final Object generic = Types.JSON.convertFromBaseToGeneric(json, Types.NBT);
                        final Object ret = switch (generic) {
                            case Number nt -> nt.toString(); // json may parse as integer, need to convert to string
                            case Boolean bt -> bt.toString(); // json may parse as boolean, need to convert to string
                            case String s -> s; // simple string component
                            case ListType lt -> lt; // list of text components
                            case MapType ct -> ct; // complex text component
                            case byte[] bt -> throw new IllegalStateException("Unexpected byte[] output from JsonTypeUtil");
                            case int[] it -> throw new IllegalStateException("Unexpected int[] output from JsonTypeUtil");
                            case long[] lt -> throw new IllegalStateException("Unexpected long[] output from JsonTypeUtil");
                            // null is handled by isJsonNull()
                            default -> throw new IllegalStateException("Unknown nbt type: " + generic);
                        };

                        convertNested(ret);
                        directWalkComponent(ret, sourceVersion, toVersion); // Fix MC-299285
                        return ret;
                    }
                } catch (final JsonParseException ex) {
                    LOGGER.error("Failed to convert json to nbt: " + unparsedJson, ex);
                }

                return null;
            }
        });

        // step 1
        MCTypeRegistry.TEXT_COMPONENT.addStructureWalker(VERSION, 1, (final Object input, final long fromVersion, final long toVersion) -> {
            if (input instanceof ListType listType) {
                WalkerUtils.convert(MCTypeRegistry.TEXT_COMPONENT, listType, fromVersion, toVersion);
            } else if (input instanceof MapType root) {
                WalkerUtils.convertList(MCTypeRegistry.TEXT_COMPONENT, root, "extra", fromVersion, toVersion);
                WalkerUtils.convert(MCTypeRegistry.TEXT_COMPONENT, root, "separator", fromVersion, toVersion);

                final MapType clickEvent = root.getMap("clickEvent");
                if (clickEvent != null) {
                    switch (clickEvent.getString("action", "")) {
                        case "run_command":
                        case "suggest_command": {
                            WalkerUtils.convert(MCTypeRegistry.DATACONVERTER_CUSTOM_TYPE_COMMAND, clickEvent, "value", fromVersion, toVersion);
                            break;
                        }
                    }
                }

                final MapType hoverEvent = root.getMap("hoverEvent");
                if (hoverEvent != null) {
                    switch (hoverEvent.getString("action", "")) {
                        case "show_text": {
                            WalkerUtils.convert(MCTypeRegistry.TEXT_COMPONENT, hoverEvent, "contents", fromVersion, toVersion);
                            break;
                        }
                        case "show_item": {
                            if (hoverEvent.hasKey("contents", ObjectType.STRING)) {
                                WalkerUtils.convert(MCTypeRegistry.ITEM_NAME, hoverEvent, "contents", fromVersion, toVersion);
                            } else {
                                WalkerUtils.convert(MCTypeRegistry.ITEM_STACK, hoverEvent, "contents", fromVersion, toVersion);
                            }
                            break;
                        }
                        case "show_entity": {
                            WalkerUtils.convert(MCTypeRegistry.ENTITY_NAME, hoverEvent, "type", fromVersion, toVersion);
                            WalkerUtils.convert(MCTypeRegistry.TEXT_COMPONENT, hoverEvent, "name", fromVersion, toVersion);
                            break;
                        }
                        // default: do nothing
                    }
                }
            } // else: should only be string
            return null;
        });
    }

    private V4290() {}
}
