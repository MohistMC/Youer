package com.mohistmc.youer.plugins;

import com.mohistmc.youer.YouerConfig;
import com.mohistmc.youer.api.gui.GuiListener;
import com.mohistmc.youer.feature.GlobalVariableSystem;
import com.mohistmc.youer.plugins.back.BackCommands;
import com.mohistmc.youer.plugins.back.BackConfig;
import com.mohistmc.youer.plugins.ban.BanListener;
import com.mohistmc.youer.plugins.item.ItemsConfig;
import com.mohistmc.youer.plugins.tpa.TpaComamands;
import com.mohistmc.youer.plugins.tpa.TpacceptCommands;
import com.mohistmc.youer.plugins.tpa.TpadenyCommands;
import com.mohistmc.youer.plugins.warps.WarpsCommands;
import com.mohistmc.youer.plugins.warps.WarpsConfig;
import com.mohistmc.youer.plugins.world.WorldManage;
import com.mohistmc.youer.plugins.world.commands.WorldsCommands;
import java.util.Map;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.bukkit.command.Command;
import org.bukkit.event.Event;
import org.bukkit.event.entity.PlayerDeathEvent;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryCloseEvent;
import org.bukkit.event.inventory.PrepareAnvilEvent;
import org.bukkit.event.player.PlayerTeleportEvent;

/**
 * @author Mgazul by MohistMC
 * @date 2023/6/14 14:46:34
 */
public class MohistPlugin {

    public static Logger LOGGER = LogManager.getLogger("MohistPlugin");

    public static void init() {
        if (YouerConfig.config.getBoolean("worldmanage", true)) WorldManage.onEnable();
        ItemsConfig.init();
        BackConfig.init();
        WarpsConfig.init();
        EntityClear.start();
        GlobalVariableSystem.register();
    }

    public static void registerCommands(Map<String, Command> map) {
        if (YouerConfig.config.getBoolean("worldmanage", true)) {
            map.put("worlds", new WorldsCommands("worlds"));
        }
        if (YouerConfig.config.getBoolean("warps.enable", false)) {
            map.put("warps", new WarpsCommands("warps"));
        }
        if (YouerConfig.config.getBoolean("tpa.enable", false)) {
            map.put("tpa", new TpaComamands("tpa"));
            map.put("tpadeny", new TpadenyCommands("tpadeny"));
            map.put("tpaccept", new TpacceptCommands("tpaccept"));
        }
        if (YouerConfig.config.getBoolean("back.enable", false)) {
            map.put("back", new BackCommands("back"));
        }
    }

    public static void registerListener(Event event) {
        if (event instanceof InventoryClickEvent inventoryClickEvent) {
            GuiListener.onInventoryClickEvent(inventoryClickEvent);
        }
        if (event instanceof PrepareAnvilEvent prepareAnvilEvent) {
            EnchantmentFix.anvilListener(prepareAnvilEvent);
        }
        if (event instanceof InventoryCloseEvent event1) {
            BanListener.save(event1);
            GuiListener.onInventoryCloseEvent(event1);
        }
        if (event instanceof PlayerTeleportEvent event1) {
            BackCommands.hookTeleport(event1);
        }
        if (event instanceof PlayerDeathEvent event1) {
            BackCommands.hooktDeath(event1);
        }
    }
}
