package com.mohistmc.youer.feature.create;

import com.mohistmc.youer.api.gui.DemoGUI;
import com.mohistmc.youer.api.gui.GUIItem;
import com.mohistmc.youer.api.gui.ItemStackFactory;
import com.mohistmc.youer.util.I18n;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Locale;
import java.util.Optional;
import java.util.stream.Collectors;
import net.minecraft.core.Holder;
import net.minecraft.core.component.DataComponents;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.alchemy.Potion;
import net.minecraft.world.item.alchemy.PotionContents;
import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.craftbukkit.inventory.CraftItemStack;
import org.bukkit.entity.Player;
import org.bukkit.event.inventory.ClickType;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.jetbrains.annotations.NotNull;

/**
 * Create item drain related command. Sub-command:
 * - /create_item_drain potionban add|show — Manage the potion blacklist that blocks item emptying on the item drain (MixinGenericItemEmptying), same interaction as /bans add|show
 * Named create_item_drain because Create mod ships its own "create" command.
 *
 * @author Mgazul
 * @date 2026/8/7
 */
public class CreateItemDrainCommand extends Command {

    private final List<String> potionbanActions = Arrays.asList("add", "show");

    public CreateItemDrainCommand(String name) {
        super(name);
        this.description = I18n.as("createitemdraincmd.description");
        this.usageMessage = "/create_item_drain potionban [add|show]";
        this.setPermission("youer.command.create_item_drain");
    }

    @Override
    public boolean execute(@NotNull CommandSender sender, @NotNull String commandLabel, @NotNull String[] args) {
        if (!testPermission(sender)) {
            return false;
        }
        if (args.length < 2 || !args[0].equalsIgnoreCase("potionban")) {
            sender.sendMessage(ChatColor.RED + I18n.as("banscmd.usage.prefix") + usageMessage);
            return false;
        }

        if (!(sender instanceof Player player)) {
            sender.sendMessage(ChatColor.RED + I18n.as("banscmd.error.notplayer"));
            return false;
        }
        switch (args[1].toLowerCase(Locale.ENGLISH)) {
            case "add" -> {
                if (args.length == 2) {
                    PotionBanSaveInventory potionBanSaveInventory = new PotionBanSaveInventory(I18n.as("createitemdraincmd.potionban.add.gui.title"));
                    Inventory inventory = potionBanSaveInventory.getInventory();
                    player.openInventory(inventory);
                    PotionBanListener.openInventory = potionBanSaveInventory;
                    return true;
                }
                if (args.length != 3) {
                    sender.sendMessage(ChatColor.RED + I18n.as("banscmd.usage.prefix") + usageMessage);
                    return false;
                }
                String id = args[2];
                ResourceLocation rl;
                try {
                    rl = ResourceLocation.parse(id);
                } catch (IllegalArgumentException e) {
                    sender.sendMessage(ChatColor.RED + I18n.as("createitemdraincmd.potionban.add.invalid").formatted(id));
                    return false;
                }
                if (!BuiltInRegistries.POTION.containsKey(rl)) {
                    sender.sendMessage(ChatColor.RED + I18n.as("createitemdraincmd.potionban.add.notexists").formatted(id));
                    return false;
                }
                if (PotionBanConfig.INSTANCE.has(id)) {
                    sender.sendMessage(ChatColor.RED + I18n.as("createitemdraincmd.potionban.add.exists").formatted(id));
                    return false;
                }
                PotionBanConfig.INSTANCE.add(id);
                sender.sendMessage(ChatColor.GREEN + I18n.as("createitemdraincmd.potionban.add.success").formatted(id));
                return true;
            }
            case "show" -> {
                if (args.length != 2) {
                    sender.sendMessage(ChatColor.RED + I18n.as("banscmd.usage.prefix") + usageMessage);
                    return false;
                }
                DemoGUI wh = new DemoGUI(I18n.as("createitemdraincmd.potionban.show.title"));
                for (String s : PotionBanConfig.getBlockedPotions()) {
                    wh.addItem(new GUIItem(new ItemStackFactory(createPotionIcon(s))
                            .setDisplayName(s)
                            .addLore("§e" + I18n.as("banscmd.show.lore"))
                            .build()) {
                        @Override
                        public void ClickAction(ClickType type, Player u, ItemStack itemStack) {
                            if (type.isRightClick()) {
                                PotionBanConfig.INSTANCE.remove(s);
                                wh.removeItem(this);
                                wh.openGUI(player);
                            }
                        }
                    });
                }
                wh.openGUI(player);
                return true;
            }
            default -> {
                sender.sendMessage(ChatColor.RED + I18n.as("banscmd.usage.prefix") + usageMessage);
                return false;
            }
        }
    }

    @Override
    public @NotNull List<String> tabComplete(@NotNull CommandSender sender, @NotNull String alias, String[] args) {
        List<String> list = new ArrayList<>();
        if (args.length == 1 && (sender.isOp() || testPermission(sender))) {
            list.add("potionban");
        }
        if (args.length == 2 && args[0].equalsIgnoreCase("potionban") && (sender.isOp() || testPermission(sender))) {
            for (String param : potionbanActions) {
                if (param.toLowerCase().startsWith(args[1].toLowerCase())) {
                    list.add(param);
                }
            }
        }
        if (args.length == 3 && args[0].equalsIgnoreCase("potionban") && args[1].equalsIgnoreCase("add") && (sender.isOp() || testPermission(sender))) {
            return BuiltInRegistries.POTION.keySet().stream()
                    .map(ResourceLocation::toString)
                    .filter(name -> name.toLowerCase().startsWith(args[2].toLowerCase()))
                    .collect(Collectors.toList());
        }
        return list;
    }

    // Resolves a blocked potion id into its real potion item (color, texture), falling back to a plain potion icon
    private static ItemStack createPotionIcon(String id) {
        try {
            ResourceLocation rl = ResourceLocation.parse(id);
            Optional<Holder.Reference<Potion>> holder = BuiltInRegistries.POTION.getHolder(rl);
            if (holder.isPresent()) {
                net.minecraft.world.item.ItemStack nms = new net.minecraft.world.item.ItemStack(Items.POTION);
                nms.set(DataComponents.POTION_CONTENTS, new PotionContents(Optional.of(holder.get()), Optional.empty(), List.of()));
                return CraftItemStack.asBukkitCopy(nms);
            }
        } catch (Exception ignored) {
        }
        return new ItemStack(Material.POTION);
    }
}
