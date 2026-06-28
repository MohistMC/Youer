package ca.spottedleaf.dataconverter.minecraft.versions;

import ca.spottedleaf.dataconverter.converters.DataConverter;
import ca.spottedleaf.dataconverter.minecraft.MCVersions;
import ca.spottedleaf.dataconverter.minecraft.converters.helpers.CopyHelper;
import ca.spottedleaf.dataconverter.minecraft.converters.helpers.RenameHelper;
import ca.spottedleaf.dataconverter.minecraft.datatypes.MCTypeRegistry;
import ca.spottedleaf.dataconverter.minecraft.walkers.generic.WalkerUtils;
import ca.spottedleaf.dataconverter.types.ListType;
import ca.spottedleaf.dataconverter.types.MapType;
import ca.spottedleaf.dataconverter.types.ObjectType;
import java.net.URI;

public final class V4292 {

    private static final int VERSION = MCVersions.V1_21_4 + 103;

    public static void register() {
        MCTypeRegistry.TEXT_COMPONENT.addStructureConverter(new DataConverter<>(VERSION) {
            private static boolean isWebUrl(final String value) {
                try {
                    final String scheme = new URI(value).getScheme();

                    return "http".equalsIgnoreCase(scheme) || "https".equalsIgnoreCase(scheme);
                } catch (final Exception ex) {
                    return false;
                }
            }

            private static boolean isValidCommandOrChat(final String value) {
                for (int i = 0, len = value.length(); i < len; ++i) {
                    final char c = value.charAt(i);

                    if (c < ' ' || c == 127 || c == 167) {
                        return false;
                    }
                }

                return true;
            }

            private static Integer parseInteger(final MapType root, final String path) {
                final Object value = root.getGeneric(path);
                if (value instanceof String string) {
                    try {
                        return Integer.parseInt(string);
                    } catch (final Exception ex) {
                        return null;
                    }
                } else if (value instanceof Number number) {
                    return Integer.valueOf(number.intValue());
                }

                return null;
            }

            @Override
            public Object convert(final Object data, final long sourceVersion, final long toVersion) {
                if (!(data instanceof MapType root)) {
                    // list or string, don't care about formatting for those
                    return null;
                }

                RenameHelper.renameSingle(root, "hoverEvent", "hover_event");
                final MapType hoverEvent = root.getMap("hover_event");
                if (hoverEvent != null) {
                    switch (hoverEvent.getString("action", "")) {
                        case "show_text": {
                            RenameHelper.renameSingle(hoverEvent, "contents", "value");
                            break;
                        }
                        case "show_item": {
                            if (hoverEvent.hasKey("contents", ObjectType.STRING)) {
                                RenameHelper.renameSingle(hoverEvent, "contents", "id");
                            } else {
                                final MapType contents = hoverEvent.getOrCreateMap("contents");
                                hoverEvent.remove("contents");

                                CopyHelper.move(contents, "id", hoverEvent, "id");
                                CopyHelper.move(contents, "count", hoverEvent, "count");
                                CopyHelper.move(contents, "components", hoverEvent, "components");
                            }
                            break;
                        }
                        case "show_entity": {
                            final MapType contents = hoverEvent.getOrCreateMap("contents");
                            hoverEvent.remove("contents");

                            CopyHelper.move(contents, "id", hoverEvent, "uuid");
                            CopyHelper.move(contents, "type", hoverEvent, "id");
                            CopyHelper.move(contents, "name", hoverEvent, "name");

                            break;
                        }

                        // default: do nothing
                    }
                }

                RenameHelper.renameSingle(root, "clickEvent", "click_event");
                final MapType clickEvent = root.getMap("click_event");
                if (clickEvent != null) {
                    final String value = clickEvent.getString("value", "");

                    switch (clickEvent.getString("action", "")) {
                        case "open_url": {
                            if (!isWebUrl(value)) {
                                root.remove("click_event");
                                break;
                            }
                            RenameHelper.renameSingle(clickEvent, "value", "url");
                            break;
                        }
                        case "open_file": {
                            RenameHelper.renameSingle(clickEvent, "value", "path");
                            break;
                        }
                        case "run_command":
                        case "suggest_command": {
                            if (!isValidCommandOrChat(value)) {
                                root.remove("click_event");
                                break;
                            }
                            RenameHelper.renameSingle(clickEvent, "value", "command");
                            break;
                        }
                        case "change_page": {
                            final Integer page = parseInteger(clickEvent, "value");
                            if (page == null) {
                                root.remove("click_event");
                                break;
                            }
                            clickEvent.remove("value");
                            clickEvent.setInt("page", Math.max(1, page.intValue()));
                        }

                        // default: do nothing
                    }
                }

                return null;
            }
        });

        MCTypeRegistry.TEXT_COMPONENT.addStructureWalker(VERSION, (final Object input, final long fromVersion, final long toVersion) -> {
            if (input instanceof ListType listType) {
                WalkerUtils.convert(MCTypeRegistry.TEXT_COMPONENT, listType, fromVersion, toVersion);
            } else if (input instanceof MapType root) {
                WalkerUtils.convertList(MCTypeRegistry.TEXT_COMPONENT, root, "extra", fromVersion, toVersion);
                WalkerUtils.convert(MCTypeRegistry.TEXT_COMPONENT, root, "separator", fromVersion, toVersion);


                WalkerUtils.convert(MCTypeRegistry.DATACONVERTER_CUSTOM_TYPE_COMMAND, root.getMap("clickEvent"), "command", fromVersion, toVersion);

                final MapType hoverEvent = root.getMap("hover_event");
                if (hoverEvent != null) {
                    switch (hoverEvent.getString("action", "")) {
                        case "show_text": {
                            WalkerUtils.convert(MCTypeRegistry.TEXT_COMPONENT, hoverEvent, "value", fromVersion, toVersion);
                            break;
                        }
                        case "show_item": {
                            WalkerUtils.convert(MCTypeRegistry.ITEM_STACK, root, "hover_event", fromVersion, toVersion);
                            break;
                        }
                        case "show_entity": {
                            WalkerUtils.convert(MCTypeRegistry.ENTITY_NAME, hoverEvent, "id", fromVersion, toVersion);
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

    private V4292() {}
}
