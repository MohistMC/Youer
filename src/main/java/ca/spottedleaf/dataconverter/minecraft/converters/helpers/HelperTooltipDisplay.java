package ca.spottedleaf.dataconverter.minecraft.converters.helpers;

import ca.spottedleaf.converter.types.ListType;
import ca.spottedleaf.converter.types.MapType;
import ca.spottedleaf.converter.types.ObjectType;
import ca.spottedleaf.dataconverter.util.NamespaceUtil;

public final class HelperTooltipDisplay {

    public static void replaceHiddenComponent(final MapType components, final String from, final String... to) {
        final MapType tooltipDisplay = components.getMap("minecraft:tooltip_display");
        if (tooltipDisplay == null) {
            return;
        }

        final ListType hiddenComponents = tooltipDisplay.getList("hidden_components", ObjectType.STRING);
        if (hiddenComponents == null) {
            return;
        }

        int index = -1;
        for (int i = hiddenComponents.size() - 1; i >= 0; --i) {
            if (from.equals(NamespaceUtil.correctNamespace(hiddenComponents.getString(i, "")))) {
                hiddenComponents.remove(i);
                index = i;
            }
        }
        if (index == -1) {
            return;
        }

        add_loop:
        for (final String component : to) {
            for (int i = 0, len = hiddenComponents.size(); i < len; ++i) {
                if (component.equals(NamespaceUtil.correctNamespace(hiddenComponents.getString(i, "")))) {
                    continue add_loop;
                }
            }
            hiddenComponents.addString(index++, component);
        }
    }

    private HelperTooltipDisplay() {}
}
