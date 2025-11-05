package com.mohistmc.youer.commands;

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
import java.util.Locale;
import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Collectors;
import java.util.stream.StreamSupport;
import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.level.block.entity.TickingBlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.Sound;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.event.inventory.ClickType;
import org.bukkit.inventory.ItemStack;
import org.jetbrains.annotations.NotNull;

/**
 * @author Mgazul by MohistMC
 * @date 2023/8/1 20:00:00
 */
public class ShowsCommand extends Command {

    private final List<String> params = List.of("sound", "entitys", "blockentitys");

    public ShowsCommand(String name) {
        super(name);
        this.description = "Youer shows commands";
        this.usageMessage = "/shows [sound|entitys|blockentitys]";
        this.setPermission("youer.command.shows");
    }

    @Override
    public @NotNull List<String> tabComplete(@NotNull CommandSender sender, @NotNull String alias, String[] args) {
        List<String> list = new ArrayList<>();
        if (args.length == 1 && (sender.isOp() || testPermission(sender))) {
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
        if (!testPermission(sender)) {
            return false;
        }

        if (args.length == 0) {
            sender.sendMessage(ChatColor.RED + "Usage: " + usageMessage);
            return false;
        }


        if (!(sender instanceof Player player)) {
            sender.sendMessage(ChatColor.RED + I18n.as("error.notplayer"));
            return false;
        }

        switch (args[0].toLowerCase(Locale.ENGLISH)) {
            case "sound" -> {
                DemoGUI wh = new DemoGUI("Sounds");

                Map<String, List<Sound>> soundsByNamespace = new HashMap<>();
                for (Sound s : Sound.values()) {
                    NamespacedKey key = s.getKey();
                    soundsByNamespace.computeIfAbsent(key.getNamespace(), k -> new ArrayList<>()).add(s);
                }

                wh.setItem(47, new GUIItem(new ItemStackFactory(Material.REDSTONE)
                        .setDisplayName(I18n.as("shows.sound.stopall"))
                        .build()) {
                    @Override
                    public void ClickAction(ClickType type, Player u, ItemStack itemStack) {
                        u.stopAllSounds();
                    }
                });
                for (Map.Entry<String, List<Sound>> entry : soundsByNamespace.entrySet()) {
                    String namespace = entry.getKey();
                    List<Sound> sounds = entry.getValue();

                    wh.addItem(new GUIItem(new ItemStackFactory(Material.CHEST)
                            .setDisplayName("§b" + namespace + " §7(" + sounds.size() + ")")
                            .build()) {
                        @Override
                        public void ClickAction(ClickType type, Player u, ItemStack itemStack) {
                            openSoundCategoryGUI(u, namespace, sounds);
                        }
                    });
                }
                wh.openGUI(player);
                return true;
            }
            case "entitys" -> {

                Map<net.minecraft.world.entity.EntityType<?>, Integer> collect =
                        StreamSupport.stream(WorldAPI.getServerLevel(player.getWorld()).getAllEntities().spliterator(), false)
                                .collect(Collectors.toMap(
                                        net.minecraft.world.entity.Entity::getType,
                                        entity -> 1,
                                        Integer::sum
                                ));

                Map<net.minecraft.world.entity.EntityType<?>, Map<String, Integer>> entityChunkCount = new HashMap<>();

                StreamSupport.stream(WorldAPI.getServerLevel(player.getWorld()).getAllEntities().spliterator(), false)
                        .forEach(entity -> {
                            net.minecraft.world.entity.EntityType<?> type = entity.getType();
                            long chunkX = entity.blockPosition().getX() >> 4;
                            long chunkZ = entity.blockPosition().getZ() >> 4;
                            String chunkKey = chunkX + "," + chunkZ;

                            entityChunkCount.computeIfAbsent(type, k -> new HashMap<>())
                                    .merge(chunkKey, 1, Integer::sum);
                        });

                List<Map.Entry<net.minecraft.world.entity.EntityType<?>, Integer>> infoIds = new ArrayList<>(collect.entrySet());
                infoIds.sort((o1, o2) -> {
                    Integer p1 = o1.getValue();
                    Integer p2 = o2.getValue();
                    return p2 - p1;
                });

                LinkedHashMap<net.minecraft.world.entity.EntityType<?>, Integer> newMap = new LinkedHashMap<>();
                AtomicInteger allSize = new AtomicInteger(0);
                for (Map.Entry<net.minecraft.world.entity.EntityType<?>, Integer> entity : infoIds) {
                    newMap.put(entity.getKey(), entity.getValue());
                    allSize.addAndGet(entity.getValue());
                }

                DemoGUI wh = new DemoGUI(I18n.as("shows.entitys.title", allSize.getAndSet(0)));
                for (Map.Entry<net.minecraft.world.entity.EntityType<?>, Integer> s : newMap.entrySet()) {

                    String topChunk = "";
                    int maxCount = 0;
                    if (entityChunkCount.containsKey(s.getKey())) {
                        for (Map.Entry<String, Integer> chunkEntry : entityChunkCount.get(s.getKey()).entrySet()) {
                            if (chunkEntry.getValue() > maxCount) {
                                maxCount = chunkEntry.getValue();
                                topChunk = chunkEntry.getKey();
                            }
                        }
                    }

                    String finalTopChunk = topChunk;
                    int finalMaxCount = maxCount;
                    wh.addItem(new GUIItem(new ItemStackFactory(ItemAPI.getEggMaterial(s.getKey()))
                                       .setLore(List.of(
                                               "§7====================",
                                               I18n.as("shows.entitys.item.name", s.getValue()),
                                               I18n.as("shows.entitys.item.entity", EntityType.getKey(s.getKey())),
                                               I18n.as("shows.entitys.item.chunk", finalTopChunk, finalMaxCount),
                                               "",
                                               I18n.as("shows.entitys.item.click"),
                                               "§7===================="
                                       ))
                                       .build()) {
                                   @Override
                                   public void ClickAction(ClickType type, Player u, ItemStack itemStack) {
                                       if (!finalTopChunk.isEmpty()) {
                                           String[] coords = finalTopChunk.split(",");
                                           if (coords.length == 2) {
                                               try {
                                                   int chunkX = Integer.parseInt(coords[0]);
                                                   int chunkZ = Integer.parseInt(coords[1]);
                                                   u.teleport(u.getWorld().getHighestBlockAt(chunkX * 16 + 8, chunkZ * 16 + 8).getLocation().add(0.5, 1, 0.5));
                                                   u.sendMessage(I18n.as("shows.entitys.teleport.success", chunkX, chunkZ, finalMaxCount));
                                               } catch (NumberFormatException e) {
                                                   u.sendMessage(I18n.as("shows.entitys.teleport.error"));
                                               }
                                           }
                                       } else {
                                           u.sendMessage(I18n.as("shows.entitys.chunk.notfound"));
                                       }
                                   }
                               }
                    );
                }
                wh.openGUI(player);
                return true;
            }
            case "blockentitys" -> {

                var serverLevel = WorldAPI.getServerLevel(player.getWorld());
                Map<Material, Map<String, Integer>> blockEntityChunkCount = new HashMap<>();
                Map<Material, Integer> collect = new HashMap<>();
                for (TickingBlockEntity blockEntityTicker : serverLevel.blockEntityTickers) {
                    BlockPos pos = blockEntityTicker.getPos();
                    BlockState block = serverLevel.getBlockState(pos);
                    net.minecraft.world.item.ItemStack itemStack = block.getBlock().asItem().getDefaultInstance();
                    Material material = itemStack.getBukkitStack().getType();
                    if (material.isAir() || material.asItemType() == null) continue;
                    collect.merge(material, 1, Integer::sum);

                    long chunkX = pos.getX() >> 4;
                    long chunkZ = pos.getZ() >> 4;
                    String chunkKey = chunkX + "," + chunkZ;
                    blockEntityChunkCount.computeIfAbsent(material, k -> new HashMap<>())
                            .merge(chunkKey, 1, Integer::sum);
                }

                List<Map.Entry<Material, Integer>> infoIds = new ArrayList<>(collect.entrySet());
                infoIds.sort((o1, o2) -> {
                    Integer p1 = o1.getValue();
                    Integer p2 = o2.getValue();
                    return p2 - p1;
                });

                LinkedHashMap<Material, Integer> newMap = new LinkedHashMap<>();
                AtomicInteger allSize = new AtomicInteger(0);
                for (Map.Entry<Material, Integer> entity : infoIds) {
                    newMap.put(entity.getKey(), entity.getValue());
                    allSize.addAndGet(entity.getValue());
                }

                DemoGUI wh = new DemoGUI(I18n.as("shows.blockentitys.title", allSize.getAndSet(0)));
                for (Map.Entry<Material, Integer> s : newMap.entrySet()) {

                    String topChunk = "";
                    int maxCount = 0;
                    if (blockEntityChunkCount.containsKey(s.getKey())) {
                        for (Map.Entry<String, Integer> chunkEntry : blockEntityChunkCount.get(s.getKey()).entrySet()) {
                            if (chunkEntry.getValue() > maxCount) {
                                maxCount = chunkEntry.getValue();
                                topChunk = chunkEntry.getKey();
                            }
                        }
                    }

                    String finalTopChunk = topChunk;
                    int finalMaxCount = maxCount;

                    wh.addItem(new GUIItem(new ItemStackFactory(s.getKey())
                                       .setLore(List.of(
                                               "§7====================",
                                               I18n.as("shows.entitys.item.name", s.getValue()),
                                               I18n.as("shows.blockentitys.item.entity", s.getKey()),
                                               I18n.as("shows.entitys.item.chunk", finalTopChunk, finalMaxCount),
                                               "",
                                               I18n.as("shows.entitys.item.click"),
                                               "§7===================="
                                       ))
                                       .build()) {
                                   @Override
                                   public void ClickAction(ClickType type, Player u, ItemStack itemStack) {
                                       if (!finalTopChunk.isEmpty()) {
                                           String[] coords = finalTopChunk.split(",");
                                           if (coords.length == 2) {
                                               try {
                                                   int chunkX = Integer.parseInt(coords[0]);
                                                   int chunkZ = Integer.parseInt(coords[1]);
                                                   u.teleport(u.getWorld().getHighestBlockAt(chunkX * 16 + 8, chunkZ * 16 + 8).getLocation().add(0.5, 1, 0.5));
                                                   u.sendMessage(I18n.as("shows.entitys.teleport.success", chunkX, chunkZ, finalMaxCount));
                                               } catch (NumberFormatException e) {
                                                   u.sendMessage(I18n.as("shows.entitys.teleport.error"));
                                               }
                                           }
                                       } else {
                                           u.sendMessage(I18n.as("shows.entitys.chunk.notfound"));
                                       }
                                   }
                               }
                    );
                }
                wh.openGUI(player);
                return true;
            }
            default -> {
                sender.sendMessage(ChatColor.RED + "Usage: " + usageMessage);
                return false;
            }
        }
    }

    private void openSoundCategoryGUI(Player player, String namespace, List<Sound> sounds) {
        DemoGUI categoryGUI = new DemoGUI(namespace + " Sounds");

        categoryGUI.setItem(47, new GUIItem(new ItemStackFactory(Material.ARROW)
                .setDisplayName("§cBack")
                .build()) {
            @Override
            public void ClickAction(ClickType type, Player u, ItemStack itemStack) {
                execute(u, "shows", new String[]{"sound"});
            }
        });

        for (Sound s : sounds) {
            categoryGUI.addItem(new GUIItem(new ItemStackFactory(Material.NOTE_BLOCK)
                    .setDisplayName(s.name())
                    .build()) {
                @Override
                public void ClickAction(ClickType type, Player u, ItemStack itemStack) {
                    player.playSound(player.getLocation(), s, 1f, 1.0f);
                }
            });
        }

        categoryGUI.openGUI(player);
    }
}
