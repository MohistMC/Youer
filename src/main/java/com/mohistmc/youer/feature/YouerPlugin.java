package com.mohistmc.youer.feature;

import com.mohistmc.youer.api.gui.GuiListener;
import com.mohistmc.youer.commands.AiCommand;
import com.mohistmc.youer.commands.HatCommand;
import com.mohistmc.youer.commands.HideAllCommand;
import com.mohistmc.youer.commands.HideCommand;
import com.mohistmc.youer.commands.LightFixCommand;
import com.mohistmc.youer.commands.OpenInvCommand;
import com.mohistmc.youer.commands.VanishCommand;
import com.mohistmc.youer.feature.back.BackCommands;
import com.mohistmc.youer.feature.back.BackConfig;
import com.mohistmc.youer.feature.ban.BanListener;
import com.mohistmc.youer.feature.commands.CommandsConfig;
import com.mohistmc.youer.feature.create.PotionBanListener;
import com.mohistmc.youer.feature.entityclear.EntityClear;
import com.mohistmc.youer.feature.entityclear.EntityClearCommand;
import com.mohistmc.youer.feature.entityclear.EntityClearListener;
import com.mohistmc.youer.feature.entityclear.EntityClearTrash;
import com.mohistmc.youer.feature.entitylimits.EntityLimitsCommands;
import com.mohistmc.youer.feature.item.ItemsConfig;
import com.mohistmc.youer.feature.menu.MenuCommand;
import com.mohistmc.youer.feature.tpa.TpaCommands;
import com.mohistmc.youer.feature.tpa.TpacceptCommands;
import com.mohistmc.youer.feature.tpa.TpadenyCommands;
import com.mohistmc.youer.feature.warps.WarpsCommands;
import com.mohistmc.youer.feature.warps.WarpsConfig;
import com.mohistmc.youer.feature.world.WorldManage;
import com.mohistmc.youer.feature.world.commands.WorldsCommands;
import com.mohistmc.youer.feature.world.utils.ConfigByWorlds;
import io.papermc.paper.event.world.WorldGameRuleChangeEvent;
import io.papermc.paper.event.world.border.WorldBorderBoundsChangeEvent;
import io.papermc.paper.event.world.border.WorldBorderBoundsChangeFinishEvent;
import io.papermc.paper.event.world.border.WorldBorderCenterChangeEvent;
import java.util.Map;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.bukkit.command.Command;
import org.bukkit.event.Event;
import org.bukkit.event.entity.PlayerDeathEvent;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryCloseEvent;
import org.bukkit.event.inventory.PrepareAnvilEvent;
import org.bukkit.event.player.PlayerChangedWorldEvent;
import org.bukkit.event.player.PlayerTeleportEvent;
import org.bukkit.event.server.PluginDisableEvent;
import com.mohistmc.youer.api.ai.tool.AiTools;

/**
 * @author Mgazul by MohistMC
 * @date 2023/6/14 14:46:34
 */
public class YouerPlugin {

    public static Logger LOGGER = LogManager.getLogger("YouerPlugin");

    public static void init() {
        if (CommandsConfig.INSTANCE.enable("worlds.enable")) {
            WorldManage.onEnable();
        }
        ItemsConfig.init();
        BackConfig.init();
        WarpsConfig.init();
        EntityClear.start();
        EntityClearTrash.start();
        WorldBackup.start();
        GlobalVariableSystem.register();
    }

    public static void registerCommands(Map<String, Command> commands) {
        CommandsConfig.init();
        if (CommandsConfig.INSTANCE.enable("worlds.enable")) {
            commands.put("worlds", new WorldsCommands("worlds"));
        }
        if (CommandsConfig.INSTANCE.enable("warps.enable")) {
            commands.put("warps", new WarpsCommands("warps"));
        }
        if (CommandsConfig.INSTANCE.enable("tpa.enable")) {
            commands.put("tpa", new TpaCommands("tpa"));
            commands.put("tpadeny", new TpadenyCommands("tpadeny"));
            commands.put("tpaccept", new TpacceptCommands("tpaccept"));
        }
        if (CommandsConfig.INSTANCE.enable("back.enable")) {
            commands.put("back", new BackCommands("back"));
        }
        if (CommandsConfig.INSTANCE.enable("menus.enable")) {
            commands.put("menus", new MenuCommand("menus"));
        }
        if (CommandsConfig.INSTANCE.enable("hideall.enable")) {
            commands.put("hideall", new HideAllCommand("hideall"));
        }
        if (CommandsConfig.INSTANCE.enable("hide.enable")) {
            commands.put("hide", new HideCommand("hide"));
        }
        if (CommandsConfig.INSTANCE.enable("hat.enable")) {
            commands.put("hat", new HatCommand("hat"));
        }
        if (CommandsConfig.INSTANCE.enable("vanish.enable")) {
            commands.put("vanish", new VanishCommand("vanish"));
        }
        if (CommandsConfig.INSTANCE.enable("openinv.enable")) {
            commands.put("openinv", new OpenInvCommand("openinv"));
        }
        if (CommandsConfig.INSTANCE.enable("entitylimits.enable")) {
            commands.put("entitylimits", new EntityLimitsCommands("entitylimits"));
        }
        if (CommandsConfig.INSTANCE.enable("ai.enable")) {
            commands.put("ai", new AiCommand("ai"));
        }
        if (CommandsConfig.INSTANCE.enable("entityclear.enable")) {
            commands.put("entityclear", new EntityClearCommand("entityclear"));
        }
        commands.put("lightfix", new LightFixCommand("lightfix"));
    }

    public static void registerListener(Event event) {
        if (event instanceof PluginDisableEvent event1) {
            AiTools.unregister(event1.getPlugin());
        }
        if (event instanceof InventoryClickEvent inventoryClickEvent) {
            GuiListener.onInventoryClickEvent(inventoryClickEvent);
        }
        if (event instanceof PrepareAnvilEvent prepareAnvilEvent) {
            EnchantmentFix.anvilListener(prepareAnvilEvent);
        }
        if (event instanceof InventoryCloseEvent event1) {
            BanListener.save(event1);
            PotionBanListener.save(event1);
            EntityClearListener.save(event1);
            GuiListener.onInventoryCloseEvent(event1);
        }
        if (event instanceof PlayerTeleportEvent event1) {
            WorldManage.hookTeleport(event1);
            BackCommands.hookTeleport(event1);
        }
        if (event instanceof PlayerChangedWorldEvent event1) {
            WorldManage.hookChangedWorld(event1);
        }
        if (event instanceof PlayerDeathEvent event1) {
            BackCommands.hooktDeath(event1);
        }
        // Persist world border and game rule changes for custom worlds
        if (event instanceof WorldBorderBoundsChangeEvent event1) {
            if (CommandsConfig.INSTANCE.enable("worlds.enable") && !event1.isCancelled()) {
                // Event fires before the change, save the new size from the event
                ConfigByWorlds.saveWorldBorder(event1.getWorld(), event1.getNewSize());
            }
        }
        if (event instanceof WorldBorderBoundsChangeFinishEvent event1) {
            if (CommandsConfig.INSTANCE.enable("worlds.enable")) {
                // Event fires when timed move finishes, save the final size from the event
                ConfigByWorlds.saveWorldBorder(event1.getWorld(), event1.getNewSize());
            }
        }
        if (event instanceof WorldBorderCenterChangeEvent event1) {
            if (CommandsConfig.INSTANCE.enable("worlds.enable") && !event1.isCancelled()) {
                // Event fires before the change, save the new center from the event
                ConfigByWorlds.saveWorldBorderCenter(event1.getWorld(), event1.getNewCenter().getX(), event1.getNewCenter().getZ());
            }
        }
        if (event instanceof WorldGameRuleChangeEvent event1) {
            if (CommandsConfig.INSTANCE.enable("worlds.enable") && !event1.isCancelled()) {
                org.bukkit.GameRule<?> gameRule = event1.getGameRule();
                if (gameRule != null) {
                    ConfigByWorlds.saveGameRule(event1.getWorld(), gameRule.getName(), event1.getValue());
                }
            }
        }
    }
}
