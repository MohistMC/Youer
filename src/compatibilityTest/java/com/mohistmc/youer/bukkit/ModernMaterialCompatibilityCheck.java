package com.mohistmc.youer.bukkit;

import java.util.Arrays;
import java.util.Set;
import net.minecraft.resources.Identifier;
import org.bukkit.Material;
import org.bukkit.craftbukkit.util.ApiVersion;
import org.bukkit.craftbukkit.util.Commodore;
import org.bukkit.craftbukkit.util.CraftLegacy;

/** Reproduces FAWE's material-indexed cache using Youer's real, dynamically extended enum. */
@SuppressWarnings("deprecation")
public final class ModernMaterialCompatibilityCheck {
    private static int checks;

    public static void main(String[] args) throws Exception {
        Material[] vanilla = CraftLegacy.modern_values();
        for (Material material : vanilla) {
            require(CraftLegacy.modern_ordinal(material) == material.ordinal(), "Changed vanilla ordinal: " + material);
        }
        checkCache();
        Material first = Material.addMaterial("YOUER_TEST_FAWE_ITEM", 30000, false, true,
                Identifier.parse("youer_test:fawe_item"));
        require(first != null, "Could not extend the actual Material enum");
        // This lookup is the failing FAWE pattern: array created from values(), indexed with ordinal().
        checkCache(first);
        Material second = Material.addMaterial("YOUER_TEST_FAWE_BLOCK", 30001, true, false,
                Identifier.parse("youer_test:fawe_block"));
        checkCache(first, second);
        Material[] copy = CraftLegacy.modern_values();
        copy[0] = null;
        require(CraftLegacy.modern_values()[0] != null, "Caller mutated the material enumeration cache");
        for (Material material : Material.values()) {
            if (material.isLegacy()) {
                try {
                    CraftLegacy.modern_ordinal(material);
                    throw new AssertionError("Legacy material received a modern ordinal: " + material);
                } catch (NoSuchFieldError expected) {
                    checks++;
                }
            }
        }
        require(Arrays.equals(vanilla, Arrays.copyOf(CraftLegacy.modern_values(), vanilla.length)),
                "Adding mod materials changed vanilla enumeration order");
        checkPluginConversion();
        System.out.println("Modern material compatibility checks passed: " + checks);
    }

    private static void checkPluginConversion() throws Exception {
        String name = "com.mohistmc.youer.bukkit.MaterialCacheFixture";
        byte[] original;
        try (var input = ModernMaterialCompatibilityCheck.class.getResourceAsStream("/" + name.replace('.', '/') + ".class")) {
            original = input.readAllBytes();
        }
        byte[] converted = new Commodore(compatibility -> false)
                .convert(original, "YouerMaterialRegression", ApiVersion.CURRENT, Set.of());
        Class<?> plugin = new ClassLoader(ModernMaterialCompatibilityCheck.class.getClassLoader()) {
            Class<?> define() {
                return defineClass(name, converted, 0, converted.length);
            }
        }.define();
        Material[] cache = (Material[]) plugin.getMethod("createCache").invoke(null);
        var lookup = plugin.getMethod("lookup", Material[].class, Material.class);
        for (Material material : Material.values()) {
            if (!material.isLegacy()) {
                require(lookup.invoke(null, cache, material) == material,
                        "Actual plugin bytecode conversion breaks material cache lookup: " + material);
            }
        }
    }

    private static void checkCache(Material... added) {
        Material[] materials = CraftLegacy.modern_values();
        Material[] cache = new Material[materials.length];
        for (int i = 0; i < materials.length; i++) {
            require(!materials[i].isLegacy(), "Modern enumeration contains a legacy material");
            cache[i] = materials[i];
        }
        for (Material material : added) {
            int ordinal = CraftLegacy.modern_ordinal(material);
            require(ordinal < cache.length,
                    "FAWE material cache index " + ordinal + " out of bounds for length " + cache.length);
            require(cache[ordinal] == material, "FAWE material cache maps the modded item to another type");
        }
        for (Material material : Material.values()) {
            if (!material.isLegacy()) {
                int ordinal = CraftLegacy.modern_ordinal(material);
                require(ordinal >= 0 && ordinal < cache.length && cache[ordinal] == material,
                        "Modern material/ordinal mismatch: " + material);
            }
        }
    }

    private static void require(boolean condition, String message) {
        if (!condition) {
            throw new AssertionError(message);
        }
        checks++;
    }
}
