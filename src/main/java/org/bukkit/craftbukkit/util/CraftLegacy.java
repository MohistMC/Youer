package org.bukkit.craftbukkit.util;

import java.util.Arrays;
import org.bukkit.Material;
import org.bukkit.material.MaterialData;

/**
 * @deprecated legacy use only
 */
@Deprecated
public final class CraftLegacy {

    // Youer appends mod materials after Bukkit's fixed block of legacy constants.
    private static final int LEGACY_MATERIAL_COUNT = (int) Arrays.stream(Material.values()).filter(Material::isLegacy).count();

    private CraftLegacy() {
    }

    public static Material fromLegacy(Material material) {
        if (material == null || !material.isLegacy()) {
            return material;
        }

        return org.bukkit.craftbukkit.legacy.CraftLegacy.fromLegacy(material);
    }

    public static Material fromLegacy(MaterialData materialData) {
        return org.bukkit.craftbukkit.legacy.CraftLegacy.fromLegacy(materialData);
    }

    public static Material[] modern_values() {
        return Arrays.stream(Material.values()).filter(material -> !material.isLegacy()).toArray(Material[]::new);
    }

    public static int modern_ordinal(Material material) {
        if (material.isLegacy()) {
            // SPIGOT-4002: Fix for eclipse compiler manually compiling in default statements to lookupswitch
            throw new NoSuchFieldError("Legacy field ordinal: " + material);
        }

        int ordinal = material.ordinal();
        // Match the dense index in modern_values(), including mod constants after the legacy gap.
        return ordinal < Material.LEGACY_AIR.ordinal() ? ordinal : ordinal - LEGACY_MATERIAL_COUNT;
    }
}
