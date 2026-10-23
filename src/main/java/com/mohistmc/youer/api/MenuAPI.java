package com.mohistmc.youer.api;

import com.mohistmc.youer.Youer;
import com.mohistmc.youer.util.DynamicEnumUtil;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.Identifier;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.neoforged.fml.ModList;
import net.neoforged.neoforgespi.language.IModInfo;
import net.neoforged.neoforgespi.language.ModFileScanData;
import org.bukkit.craftbukkit.inventory.CraftInventoryView;
import org.bukkit.craftbukkit.inventory.CraftMenuType;
import org.bukkit.event.inventory.InventoryOpenEvent;
import org.bukkit.event.inventory.InventoryType;
import org.bukkit.inventory.InventoryView;
import org.bukkit.inventory.MenuType;
import org.jetbrains.annotations.ApiStatus;
import org.jetbrains.annotations.Nullable;

/**
 * Helpers to find out whether an opened menu (GUI) comes from a mod and, if so, from which mod.
 * <p>
 * The menu type registry namespace is used first (for example {@code create:mechanical_crafter}),
 * then the menu class is matched against the classes declared by every loaded mod. This second
 * check also catches modded menus that reuse a vanilla {@code MenuType} such as
 * {@code minecraft:generic_9x3}.
 *
 * @author Mgazul
 */
public final class MenuAPI {

    private static final String MINECRAFT = "minecraft";
    private static final String NEOFORGE = "neoforge";
    private static final String YOUER = "youer";

    private static final Map<Class<?>, Optional<String>> CLASS_MOD_CACHE = new ConcurrentHashMap<>();
    private static final Map<Identifier, InventoryType> MENU_INVENTORY_TYPES = new ConcurrentHashMap<>();
    private static volatile Map<String, IModInfo> CLASS_TO_MOD;

    private MenuAPI() {
    }

    public static boolean isModded(@Nullable InventoryView view) {
        return getModId(view) != null;
    }

    public static boolean isModded(@Nullable AbstractContainerMenu menu) {
        return getModId(menu) != null;
    }

    public static boolean isModded(@Nullable InventoryOpenEvent event) {
        return getModId(event) != null;
    }

    public static boolean isVanilla(@Nullable InventoryView view) {
        return !isModded(view);
    }

    public static boolean isVanilla(@Nullable AbstractContainerMenu menu) {
        return !isModded(menu);
    }

    public static boolean isVanilla(@Nullable InventoryOpenEvent event) {
        return !isModded(event);
    }

    /**
     * @return the mod id owning this view, or {@code null} when the view is vanilla/server provided
     */
    public static @Nullable String getModId(@Nullable InventoryView view) {
        return getModId(getMenu(view));
    }

    /**
     * @return the mod id owning the menu of this event, or {@code null} when it is vanilla/server provided
     */
    public static @Nullable String getModId(@Nullable InventoryOpenEvent event) {
        return event == null ? null : getModId(event.getView());
    }

    /**
     * @return the mod id owning this menu, or {@code null} when the menu is vanilla/server provided
     */
    public static @Nullable String getModId(@Nullable AbstractContainerMenu menu) {
        if (menu == null) {
            return null;
        }
        Identifier menuTypeKey = getMenuTypeKey(menu);
        if (menuTypeKey != null) {
            String namespace = menuTypeKey.getNamespace();
            if (!isBuiltin(namespace) && ModList.get().isLoaded(namespace)) {
                return namespace;
            }
        }
        return getModId(menu.getClass());
    }

    /**
     * @return the mod id owning this menu class, or {@code null} when the class is vanilla/server provided
     */
    public static @Nullable String getModId(@Nullable Class<?> menuClass) {
        if (menuClass == null) {
            return null;
        }
        Optional<String> cached = CLASS_MOD_CACHE.get(menuClass);
        if (cached == null) {
            IModInfo modInfo = getClassToMod().get(menuClass.getName());
            String modId = modInfo == null ? null : modInfo.getModId();
            if (modId != null && isBuiltin(modId)) {
                modId = null;
            }
            cached = Optional.ofNullable(modId);
            CLASS_MOD_CACHE.put(menuClass, cached);
        }
        return cached.orElse(null);
    }

    /**
     * @return the display name of the mod owning this view, or {@code null} when the view is vanilla/server provided
     */
    public static @Nullable String getModName(@Nullable InventoryView view) {
        return getModName(getMenu(view));
    }

    /**
     * @return the display name of the mod owning the menu of this event, or {@code null} when it is vanilla/server provided
     */
    public static @Nullable String getModName(@Nullable InventoryOpenEvent event) {
        return event == null ? null : getModName(event.getView());
    }

    /**
     * @return the display name of the mod owning this menu, or {@code null} when the menu is vanilla/server provided
     */
    public static @Nullable String getModName(@Nullable AbstractContainerMenu menu) {
        return getModName(getModId(menu));
    }

    /**
     * @return the display name of the mod owning this menu class, or {@code null} when the class is vanilla/server provided
     */
    public static @Nullable String getModName(@Nullable Class<?> menuClass) {
        return getModName(getModId(menuClass));
    }

    private static @Nullable String getModName(@Nullable String modId) {
        if (modId == null) {
            return null;
        }
        return ModList.get().getModContainerById(modId)
                .map(container -> container.getModInfo().getDisplayName())
                .orElse(modId);
    }

    /**
     * Creates (or returns) the Bukkit {@link InventoryType} for a modded menu type.
     * Called by {@code NeoForgeInjectBukkit} at startup and lazily when a menu type was
     * registered after the startup pass.
     */
    @ApiStatus.Internal
    public static @Nullable InventoryType ensureInventoryType(@Nullable Identifier menuTypeId, net.minecraft.world.inventory.@Nullable MenuType<?> menuType) {
        if (menuTypeId == null || menuType == null) {
            return null;
        }
        InventoryType existing = MENU_INVENTORY_TYPES.get(menuTypeId);
        if (existing != null) {
            return existing;
        }
        synchronized (MENU_INVENTORY_TYPES) {
            existing = MENU_INVENTORY_TYPES.get(menuTypeId);
            if (existing != null) {
                return existing;
            }
            MenuType bukkitMenuType;
            try {
                bukkitMenuType = CraftMenuType.minecraftToBukkit(menuType);
            } catch (Throwable t) {
                Youer.LOGGER.warn("Failed to convert mod MenuType " + menuTypeId, t);
                return null;
            }
            String name = nextInventoryTypeName(menuTypeId);
            InventoryType inventoryType = DynamicEnumUtil.addEnum(
                    InventoryType.class,
                    name,
                    new Class<?>[]{Integer.TYPE, String.class, MenuType.class, Boolean.TYPE},
                    new Object[]{0, menuTypeId.toString(), bukkitMenuType, false}
            );
            if (inventoryType == null) {
                Youer.LOGGER.warn("Failed to create InventoryType for mod MenuType {}", menuTypeId);
                return null;
            }
            MENU_INVENTORY_TYPES.put(menuTypeId, inventoryType);
            return inventoryType;
        }
    }

    /**
     * Registers a pre-created Bukkit {@link InventoryType} for a modded menu type.
     */
    @ApiStatus.Internal
    public static void registerInventoryType(@Nullable Identifier menuTypeId, @Nullable InventoryType inventoryType) {
        if (menuTypeId != null && inventoryType != null) {
            MENU_INVENTORY_TYPES.put(menuTypeId, inventoryType);
        }
    }

    /**
     * @return the Bukkit inventory type created for this menu, or {@code null} for vanilla menus
     */
    public static @Nullable InventoryType getInventoryType(@Nullable InventoryOpenEvent event) {
        return getInventoryType(getMenu(event));
    }

    /**
     * @return the Bukkit inventory type created for this menu, or {@code null} for vanilla menus
     */
    public static @Nullable InventoryType getInventoryType(@Nullable InventoryView view) {
        return getInventoryType(getMenu(view));
    }

    /**
     * @return the Bukkit inventory type created for this menu, or {@code null} for vanilla menus
     */
    public static @Nullable InventoryType getInventoryType(@Nullable AbstractContainerMenu menu) {
        if (menu == null || menu.menuType == null) {
            return null;
        }
        Identifier key = getMenuTypeKey(menu);
        if (key == null) {
            return null;
        }
        InventoryType inventoryType = MENU_INVENTORY_TYPES.get(key);
        if (inventoryType == null && isModdedMenuType(key)) {
            // The startup pass may have missed this type (or it was registered later).
            inventoryType = ensureInventoryType(key, menu.menuType);
        }
        return inventoryType;
    }

    private static String nextInventoryTypeName(Identifier menuTypeId) {
        Set<String> existing = new HashSet<>();
        for (InventoryType type : InventoryType.values()) {
            existing.add(type.name());
        }
        String name = DynamicEnumUtil.normalizeName(menuTypeId.toString());
        if (!existing.contains(name)) {
            return name;
        }
        int suffix = 2;
        while (existing.contains(name + "_" + suffix)) {
            suffix++;
        }
        return name + "_" + suffix;
    }

    private static boolean isModdedMenuType(Identifier menuTypeId) {
        String namespace = menuTypeId.getNamespace();
        return !isBuiltin(namespace) && ModList.get().isLoaded(namespace);
    }

    /**
     * @return the registry id of the menu type, for example {@code minecraft:generic_9x3}
     */
    public static @Nullable String getMenuTypeId(@Nullable InventoryView view) {
        return getMenuTypeId(getMenu(view));
    }

    /**
     * @return the registry id of the menu type, for example {@code minecraft:generic_9x3}
     */
    public static @Nullable String getMenuTypeId(@Nullable InventoryOpenEvent event) {
        return getMenuTypeId(getMenu(event));
    }

    /**
     * @return the registry id of the menu type, for example {@code minecraft:generic_9x3}
     */
    public static @Nullable String getMenuTypeId(@Nullable AbstractContainerMenu menu) {
        Identifier key = getMenuTypeKey(menu);
        return key == null ? null : key.toString();
    }

    /**
     * @return the registry key of the menu type, or {@code null} for menus without a registered type
     */
    public static @Nullable Identifier getMenuTypeKey(@Nullable AbstractContainerMenu menu) {
        // getType() throws when the menu was constructed without a MenuType, use the field instead
        if (menu == null || menu.menuType == null) {
            return null;
        }
        return BuiltInRegistries.MENU.getKey(menu.menuType);
    }

    /**
     * @return the native menu behind an event, or {@code null} when the event has no native menu
     */
    public static @Nullable AbstractContainerMenu getMenu(@Nullable InventoryOpenEvent event) {
        return event == null ? null : getMenu(event.getView());
    }

    /**
     * @return the native menu behind a CraftBukkit view, or {@code null} for foreign view implementations
     */
    public static @Nullable AbstractContainerMenu getMenu(@Nullable InventoryView view) {
        return view instanceof CraftInventoryView<?, ?> craftView ? craftView.getHandle() : null;
    }

    private static boolean isBuiltin(String modId) {
        return MINECRAFT.equals(modId) || NEOFORGE.equals(modId) || YOUER.equals(modId);
    }

    private static Map<String, IModInfo> getClassToMod() {
        Map<String, IModInfo> map = CLASS_TO_MOD;
        if (map == null) {
            synchronized (MenuAPI.class) {
                map = CLASS_TO_MOD;
                if (map == null) {
                    map = buildClassToMod();
                    CLASS_TO_MOD = map;
                }
            }
        }
        return map;
    }

    private static Map<String, IModInfo> buildClassToMod() {
        Map<String, IModInfo> map = new HashMap<>();
        ModList modList = ModList.get();
        if (modList == null) {
            return map;
        }
        for (ModFileScanData scanData : modList.getAllScanData()) {
            IModInfo modInfo = scanData.getIModInfoData().stream()
                    .flatMap(fileInfo -> fileInfo.getMods().stream())
                    .findFirst()
                    .orElse(null);
            if (modInfo == null || isBuiltin(modInfo.getModId())) {
                continue;
            }
            for (ModFileScanData.ClassData classData : scanData.getClasses()) {
                map.putIfAbsent(classData.clazz().getClassName(), modInfo);
            }
        }
        return map;
    }
}
