package com.mohistmc.youer.plugins.menu;

import java.util.List;
import lombok.Data;
import org.bukkit.inventory.ItemFlag;

@Data
public class Icon {
    private String material;
    private int positionX;
    private int positionY;
    private String name;
    private List<String> lore;
    private List<String> actions;
    private Integer durability;
    private List<String> enchantments;
    private Integer amount;
    private List<String> requiredItems;
    private boolean keepOpen;
    private String permission;
    private String permissionMessage;
    private boolean hideTooltip;
    private Integer customModelData;
    private List<ItemFlag> itemFlags;
    private String base64;
}