package com.mohistmc.youer.feature.entitylimits;

import com.mohistmc.youer.api.ItemAPI;
import com.mohistmc.youer.api.WorldAPI;
import com.mohistmc.youer.api.gui.DemoGUI;
import com.mohistmc.youer.api.gui.GUIItem;
import com.mohistmc.youer.api.gui.ItemStackFactory;
import com.mohistmc.youer.util.I18n;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Collectors;
import java.util.stream.StreamSupport;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.item.SpawnEggItem;
import org.bukkit.ChatColor;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.event.inventory.ClickType;
import org.bukkit.inventory.ItemStack;
import org.jetbrains.annotations.NotNull;

/**
 * @author Mgazul
 * @date 2025/10/2 21:50
 */
public class EntityLimitsCommands extends Command {

    public EntityLimitsCommands(@NotNull String name) {
        super(name);
    }

    private final List<String> params = List.of("add");

    @Override
    public @NotNull List<String> tabComplete(@NotNull CommandSender sender, @NotNull String alias, String[] args) {
        List<String> list = new ArrayList<>();
        if (args.length == 1 && (sender.isOp())) {
            for (String param : params) {
                if (param.toLowerCase().startsWith(args[0].toLowerCase())) {
                    list.add(param);
                }
            }
        }

        return list;
    }

    @Override
    public boolean execute(@NotNull CommandSender sender, @NotNull String commandLabel, @NotNull String[] args) {
        if (!(sender instanceof Player player)) {
            sender.sendMessage(ChatColor.RED + I18n.as("error.notplayer"));
            return false;
        }
        World world = player.getWorld();
        if (args.length == 0) {
            if (EntityLimitsConfig.INSTANCE.hasWorld(world.getName())) {
                DemoGUI wh = new DemoGUI(I18n.as("entitylimits.gui.title"));
                Map<EntityType<?>, Integer> collect =
                        StreamSupport.stream(WorldAPI.getServerLevel(player.getWorld()).getAllEntities().spliterator(), false)
                                .collect(Collectors.toMap(
                                        net.minecraft.world.entity.Entity::getType,
                                        entity -> 1,
                                        Integer::sum
                                ));

                Map<EntityType<?>, Map<String, Integer>> entityChunkCount = new HashMap<>();

                StreamSupport.stream(WorldAPI.getServerLevel(player.getWorld()).getAllEntities().spliterator(), false)
                        .forEach(entity -> {
                            EntityType<?> type = entity.getType();
                            long chunkX = entity.blockPosition().getX() >> 4;
                            long chunkZ = entity.blockPosition().getZ() >> 4;
                            String chunkKey = chunkX + "," + chunkZ;

                            entityChunkCount.computeIfAbsent(type, k -> new HashMap<>())
                                    .merge(chunkKey, 1, Integer::sum);
                        });

                List<Map.Entry<EntityType<?>, Integer>> infoIds = new ArrayList<>(collect.entrySet());
                infoIds.sort((o1, o2) -> {
                    Integer p1 = o1.getValue();
                    Integer p2 = o2.getValue();
                    return p2 - p1;
                });

                LinkedHashMap<EntityType<?>, Integer> newMap = new LinkedHashMap<>();
                AtomicInteger allSize = new AtomicInteger(0);
                for (Map.Entry<EntityType<?>, Integer> entity : infoIds) {
                    newMap.put(entity.getKey(), entity.getValue());
                    allSize.addAndGet(entity.getValue());
                }
                for (EntityLimits s : EntityLimitsConfig.INSTANCE.getEntityLimits()) {
                    var type = BuiltInRegistries.ENTITY_TYPE.get(ResourceLocation.parse(s.getEntityName()));
                    String topChunk = "";
                    int maxCount = 0;
                    if (entityChunkCount.containsKey(type)) {
                        for (Map.Entry<String, Integer> chunkEntry : entityChunkCount.get(type).entrySet()) {
                            if (chunkEntry.getValue() > maxCount) {
                                maxCount = chunkEntry.getValue();
                                topChunk = chunkEntry.getKey();
                            }
                        }
                    }
                    String finalTopChunk = I18n.as("entitylimits.gui.unknown");
                    if (!topChunk.isEmpty()) {
                        String[] coords = topChunk.split(",");
                        int chunkX = Integer.parseInt(coords[0]);
                        int chunkZ = Integer.parseInt(coords[1]);
                        Location loc = world.getHighestBlockAt(chunkX * 16 + 8, chunkZ * 16 + 8).getLocation().add(0.5, 1, 0.5);
                        finalTopChunk = loc.getBlockX() + "," + loc.getBlockY() + "," + loc.getBlockZ();
                    }
                    int count = collect.getOrDefault(type, 0);
                    wh.addItem(new GUIItem(new ItemStackFactory(ItemAPI.getEggMaterial(type))
                            .addLore(I18n.as("entitylimits.gui.limit").formatted(s.getEntityLimit(), count >= s.getEntityLimit() ? I18n.as("entitylimits.gui.exceeded") : I18n.as("entitylimits.gui.notreached")))
                            .addLore(I18n.as("entitylimits.gui.topchunk").formatted(finalTopChunk))
                            .build()) {
                        @Override
                        public void ClickAction(ClickType type, Player u, ItemStack itemStack) {
                        }
                    });
                }
                wh.openGUI(player);
                return true;
            } else {
                player.sendMessage(ChatColor.RED + I18n.as("entitylimits.no.limit"));
                return false;
            }
        }
        if (args[0].equalsIgnoreCase("add") && sender.isOp()) {
            if (args.length < 2) {
                player.sendMessage(ChatColor.RED + I18n.as("entitylimits.add.usage"));
                return false;
            }

            try {
                int limit = Integer.parseInt(args[1]);
                ItemStack itemInHand = player.getInventory().getItemInMainHand();
                if (itemInHand.isEmpty()) {
                    player.sendMessage(ChatColor.RED + I18n.as("entitylimits.add.noegg"));
                    return false;
                }
                net.minecraft.world.item.ItemStack nmsItem = ItemAPI.toNMSItem(itemInHand);
                if (nmsItem.getItem() instanceof SpawnEggItem spawnEggItem) {
                    EntityType<?> entitytype = spawnEggItem.getType(nmsItem);
                    String entityName = BuiltInRegistries.ENTITY_TYPE.getKey(entitytype).toString();
                    EntityLimitsConfig.INSTANCE.set(world.getName(), entityName, limit);
                    player.sendMessage(ChatColor.GREEN + I18n.as("entitylimits.add.success").formatted(entityName, world.getName(), limit));
                    return true;
                }
            } catch (NumberFormatException e) {
                player.sendMessage(ChatColor.RED + I18n.as("entitylimits.add.invalidnumber"));
            }
        }
        return false;
    }
}
