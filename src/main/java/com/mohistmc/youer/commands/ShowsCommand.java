package com.mohistmc.youer.commands;

import com.mohistmc.youer.api.ItemAPI;
import com.mohistmc.youer.api.WorldAPI;
import com.mohistmc.youer.api.gui.DemoGUI;
import com.mohistmc.youer.api.gui.GUIItem;
import com.mohistmc.youer.api.gui.ItemStackFactory;
import com.mohistmc.youer.util.I18n;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.level.block.entity.TickingBlockEntity;
import org.bukkit.ChatColor;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.Sound;
import org.bukkit.block.Block;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.craftbukkit.block.CraftBlock;
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

                ServerLevel serverLevel = WorldAPI.getServerLevel(player.getWorld());
                Map<EntityType<?>, List<Entity>> entityByType = new HashMap<>();
                for (Entity entity : serverLevel.getAllEntities()) {
                    entityByType.computeIfAbsent(entity.getType(), k -> new ArrayList<>()).add(entity);
                }

                List<Map.Entry<EntityType<?>, List<Entity>>> infoIds = new ArrayList<>(entityByType.entrySet());
                infoIds.sort((o1, o2) -> Integer.compare(o2.getValue().size(), o1.getValue().size()));

                int allSize = 0;
                for (List<Entity> value : entityByType.values()) {
                    allSize += value.size();
                }

                DemoGUI wh = new DemoGUI(I18n.as("shows.entitys.title", allSize));
                for (Map.Entry<EntityType<?>, List<Entity>> s : infoIds) {
                    EntityType<?> entityType = s.getKey();
                    List<Entity> entities = s.getValue();

                    String topChunk = "";
                    int maxCount = 0;
                    Map<String, Integer> chunkCount = new HashMap<>();
                    for (Entity entity : entities) {
                        String chunkKey = chunkKey(entity.getX(), entity.getZ());
                        int count = chunkCount.merge(chunkKey, 1, Integer::sum);
                        if (count > maxCount) {
                            maxCount = count;
                            topChunk = chunkKey;
                        }
                    }
                    final String finalTopChunk = topChunk;
                    final int finalMaxCount = maxCount;

                    wh.addItem(new GUIItem(new ItemStackFactory(ItemAPI.getEggMaterial(entityType))
                                       .setLore(List.of(
                                               "§7====================",
                                               I18n.as("shows.entitys.item.name", entities.size()),
                                               I18n.as("shows.entitys.item.entity", EntityType.getKey(entityType)),
                                               I18n.as("shows.entitys.item.chunk", topChunk, maxCount),
                                               "",
                                               I18n.as("shows.entitys.item.click"),
                                               I18n.as("shows.entitys.item.click.right"),
                                               "§7===================="
                                       ))
                                       .build()) {
                                   @Override
                                   public void ClickAction(ClickType type, Player u, ItemStack itemStack) {
                                       if (type.isShiftClick()) {
                                           return;
                                       }
                                       if (type.isRightClick()) {
                                           openEntityDetailGUI(u, entityType, entities);
                                           return;
                                       }
                                       if (type.isLeftClick()) {
                                           teleportToChunk(u, finalTopChunk, finalMaxCount);
                                       }
                                   }
                               }
                    );
                }
                wh.openGUI(player);
                return true;
            }
            case "blockentitys" -> {

                ServerLevel serverLevel = WorldAPI.getServerLevel(player.getWorld());
                Map<Material, List<BlockPos>> posByMaterial = new HashMap<>();
                for (TickingBlockEntity blockEntityTicker : new ArrayList<>(serverLevel.blockEntityTickers)) {
                    BlockPos pos = blockEntityTicker.getPos();
                    if (pos == null) continue;
                    Block block = CraftBlock.at(serverLevel, pos);
                    Material material = block.getType();
                    if (material.isAir() || material.asItemType() == null) continue;
                    posByMaterial.computeIfAbsent(material, k -> new ArrayList<>()).add(pos);
                }

                List<Map.Entry<Material, List<BlockPos>>> infoIds = new ArrayList<>(posByMaterial.entrySet());
                infoIds.sort((o1, o2) -> Integer.compare(o2.getValue().size(), o1.getValue().size()));

                int allSize = 0;
                for (List<BlockPos> value : posByMaterial.values()) {
                    allSize += value.size();
                }

                DemoGUI wh = new DemoGUI(I18n.as("shows.blockentitys.title", allSize));
                for (Map.Entry<Material, List<BlockPos>> s : infoIds) {
                    Material material = s.getKey();
                    List<BlockPos> positions = s.getValue();

                    String topChunk = "";
                    int maxCount = 0;
                    Map<String, Integer> chunkCount = new HashMap<>();
                    for (BlockPos pos : positions) {
                        String chunkKey = chunkKey(pos.getX(), pos.getZ());
                        int count = chunkCount.merge(chunkKey, 1, Integer::sum);
                        if (count > maxCount) {
                            maxCount = count;
                            topChunk = chunkKey;
                        }
                    }
                    final String finalTopChunk = topChunk;
                    final int finalMaxCount = maxCount;

                    wh.addItem(new GUIItem(new ItemStackFactory(material)
                                       .setLore(List.of(
                                               "§7====================",
                                               I18n.as("shows.entitys.item.name", positions.size()),
                                               I18n.as("shows.blockentitys.item.entity", material),
                                               I18n.as("shows.entitys.item.chunk", topChunk, maxCount),
                                               "",
                                               I18n.as("shows.entitys.item.click"),
                                               I18n.as("shows.entitys.item.click.right"),
                                               "§7===================="
                                       ))
                                       .build()) {
                                   @Override
                                   public void ClickAction(ClickType type, Player u, ItemStack itemStack) {
                                       if (type.isShiftClick()) {
                                           return;
                                       }
                                       if (type.isRightClick()) {
                                           openBlockEntityDetailGUI(u, material, positions);
                                           return;
                                       }
                                       if (type.isLeftClick()) {
                                           teleportToChunk(u, finalTopChunk, finalMaxCount);
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

    private static String chunkKey(double x, double z) {
        return ((long) x >> 4) + "," + ((long) z >> 4);
    }

    private void teleportToChunk(Player player, String chunkKey, int count) {
        if (chunkKey.isEmpty()) {
            player.sendMessage(I18n.as("shows.entitys.chunk.notfound"));
            return;
        }
        String[] coords = chunkKey.split(",");
        if (coords.length != 2) {
            player.sendMessage(I18n.as("shows.entitys.teleport.error"));
            return;
        }
        try {
            int chunkX = Integer.parseInt(coords[0]);
            int chunkZ = Integer.parseInt(coords[1]);
            player.teleport(player.getWorld().getHighestBlockAt(chunkX * 16 + 8, chunkZ * 16 + 8).getLocation().add(0.5, 1, 0.5));
            player.sendMessage(I18n.as("shows.entitys.teleport.success", chunkX, chunkZ, count));
        } catch (NumberFormatException e) {
            player.sendMessage(I18n.as("shows.entitys.teleport.error"));
        }
    }

    private void teleportToPosition(Player player, double x, double y, double z) {
        if (!player.getWorld().isChunkLoaded((int) x >> 4, (int) z >> 4)) {
            player.sendMessage(I18n.as("shows.detail.chunk.unloaded"));
            return;
        }
        player.teleport(new Location(player.getWorld(), x, y, z));
        player.sendMessage(I18n.as("shows.detail.teleport.success", format(x), format(y), format(z)));
    }

    private static String format(double value) {
        return String.format(Locale.ROOT, "%.2f", value);
    }

    private GUIItem backItem(String param) {
        return new GUIItem(new ItemStackFactory(Material.ARROW)
                .setDisplayName(I18n.as("shows.detail.back"))
                .build()) {
            @Override
            public void ClickAction(ClickType type, Player u, ItemStack itemStack) {
                if (!type.isShiftClick() && type.isLeftClick()) {
                    execute(u, "shows", new String[]{param});
                }
            }
        };
    }

    private void openEntityDetailGUI(Player player, EntityType<?> entityType, List<Entity> entities) {
        List<Entity> sorted = new ArrayList<>(entities);
        sorted.sort(Comparator.<Entity>comparingDouble(Entity::getX)
                .thenComparingDouble(Entity::getY)
                .thenComparingDouble(Entity::getZ));

        DemoGUI gui = new DemoGUI(I18n.as("shows.entitys.detail.title", EntityType.getKey(entityType), sorted.size()));
        gui.setItem(47, backItem("entitys"));

        for (Entity entity : sorted) {
            List<String> lore = new ArrayList<>();
            lore.add("§7====================");
            lore.add(I18n.as("shows.entitys.item.entity", EntityType.getKey(entityType)));
            lore.add(I18n.as("shows.entitys.detail.item.name", entity.getName().getString()));
            lore.add(I18n.as("shows.entitys.detail.item.pos", format(entity.getX()), format(entity.getY()), format(entity.getZ())));
            lore.add(I18n.as("shows.entitys.detail.item.chunk", chunkKey(entity.getX(), entity.getZ())));
            lore.add(I18n.as("shows.entitys.detail.item.uuid", entity.getUUID()));
            if (entity instanceof LivingEntity living) {
                lore.add(I18n.as("shows.entitys.detail.item.health", format(living.getHealth()), format(living.getMaxHealth())));
            }
            lore.add(I18n.as("shows.entitys.detail.item.alive", entity.isAlive(), !entity.isRemoved()));
            lore.add("");
            lore.add(I18n.as("shows.entitys.detail.item.click"));
            lore.add("§7====================");

            double x = entity.getX();
            double y = entity.getY();
            double z = entity.getZ();
            gui.addItem(new GUIItem(new ItemStackFactory(ItemAPI.getEggMaterial(entityType)).setLore(lore).build()) {
                @Override
                public void ClickAction(ClickType type, Player u, ItemStack itemStack) {
                    if (!type.isShiftClick() && type.isLeftClick()) {
                        teleportToPosition(u, x, y, z);
                    }
                }
            });
        }
        gui.openGUI(player);
    }

    private void openBlockEntityDetailGUI(Player player, Material material, List<BlockPos> positions) {
        List<BlockPos> sorted = new ArrayList<>(positions);
        sorted.sort(Comparator.<BlockPos>comparingInt(BlockPos::getX)
                .thenComparingInt(BlockPos::getY)
                .thenComparingInt(BlockPos::getZ));

        DemoGUI gui = new DemoGUI(I18n.as("shows.blockentitys.detail.title", material, sorted.size()));
        gui.setItem(47, backItem("blockentitys"));

        for (BlockPos pos : sorted) {
            List<String> lore = new ArrayList<>();
            lore.add("§7====================");
            lore.add(I18n.as("shows.blockentitys.item.entity", material));
            lore.add(I18n.as("shows.blockentitys.detail.item.pos", pos.getX(), pos.getY(), pos.getZ()));
            lore.add(I18n.as("shows.entitys.detail.item.chunk", chunkKey(pos.getX(), pos.getZ())));
            lore.add("");
            lore.add(I18n.as("shows.entitys.detail.item.click"));
            lore.add("§7====================");

            gui.addItem(new GUIItem(new ItemStackFactory(material).setLore(lore).build()) {
                @Override
                public void ClickAction(ClickType type, Player u, ItemStack itemStack) {
                    if (!type.isShiftClick() && type.isLeftClick()) {
                        teleportToPosition(u, pos.getX() + 0.5, pos.getY() + 1, pos.getZ() + 0.5);
                    }
                }
            });
        }
        gui.openGUI(player);
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
