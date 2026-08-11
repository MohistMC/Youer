package com.mohistmc.youer.feature;

import com.mohistmc.youer.YouerConfig;
import com.mohistmc.youer.api.gui.DemoGUI;
import com.mohistmc.youer.api.gui.GUIItem;
import com.mohistmc.youer.util.I18n;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledThreadPoolExecutor;
import java.util.concurrent.TimeUnit;
import net.minecraft.server.MinecraftServer;
import net.minecraft.util.thread.NamedThreadFactory;
import org.bukkit.Bukkit;
import org.bukkit.World;
import org.bukkit.entity.Player;
import org.bukkit.event.inventory.ClickType;
import org.bukkit.inventory.ItemStack;

/**
 * Item trash can: cleared dropped items go here, players can freely take them out.
 * The overworld day index (fullTime / 24000) increments once per game day;
 * the trash is automatically emptied after the configured number of days (default: 1).
 *
 * @author Mgazul
 * @date 2026/8/11
 */
public class EntityClearTrash {

    // Shared trash item list
    private static final List<ItemStack> trashItems = new ArrayList<>();
    // Per-player DemoGUI cache to preserve page state across refreshes
    private static final Map<UUID, DemoGUI> playerGUIs = new HashMap<>();
    // Day index of the last cleanup
    private static long lastTrashDay = -1;

    public static final ScheduledExecutorService TRASH = new ScheduledThreadPoolExecutor(1, new NamedThreadFactory("EntityClear - Trash"));

    public static void start() {
        init();
        // Check the overworld day index every 5 seconds (fullTime increases each tick, unaffected by doDaylightCycle)
        TRASH.scheduleAtFixedRate(EntityClearTrash::checkDay, 0, 5, TimeUnit.SECONDS);
    }

    public static void stop() {
        TRASH.shutdown();
    }

    public static void init() {
        World overworld = Bukkit.getWorlds().get(0);
        if (overworld != null) {
            lastTrashDay = overworld.getFullTime() / 24000;
        }
    }

    // Put a cleared dropped item into the trash
    public static void addItem(ItemStack itemStack) {
        if (itemStack == null || itemStack.isEmpty()) return;
        synchronized (trashItems) {
            trashItems.add(itemStack.clone());
        }
    }

    private static void checkDay() {
        if (MinecraftServer.getServer().hasStopped()) return;
        MinecraftServer.getServer().execute(() -> {
            if (!YouerConfig.trash_enable) return;
            World overworld = Bukkit.getWorlds().get(0);
            if (overworld == null) return;
            long day = overworld.getFullTime() / 24000;
            if (lastTrashDay < 0) {
                lastTrashDay = day;
                return;
            }
            if (day - lastTrashDay >= YouerConfig.trash_days) {
                synchronized (trashItems) {
                    trashItems.clear();
                }
                lastTrashDay = day;
            }
        });
    }

    public static void openTrash(Player player) {
        DemoGUI demoGUI = playerGUIs.computeIfAbsent(player.getUniqueId(),
                k -> new DemoGUI(I18n.as("entityclear.trash.title")));
        demoGUI.clearItems();

        synchronized (trashItems) {
            for (ItemStack item : trashItems) {
                final ItemStack original = item;
                GUIItem guiItem = new GUIItem(original.clone()) {
                    @Override
                    public void ClickAction(ClickType type, Player p, ItemStack itemStack) {
                        if (!type.isShiftClick() && type.isLeftClick()) {
                            synchronized (trashItems) {
                                Iterator<ItemStack> iter = trashItems.iterator();
                                while (iter.hasNext()) {
                                    if (iter.next() == original) {
                                        iter.remove();
                                        // Give the item to the player; if inventory is full, put it back
                                        p.getInventory().addItem(original.clone())
                                                .values().forEach(trashItems::add);
                                        break;
                                    }
                                }
                            }
                            openTrash(p);
                        }
                    }
                };
                demoGUI.addItem(guiItem);
            }
        }

        demoGUI.openGUI(player);
    }
}
