package com.mohistmc.youer.feature.menu;

import java.util.List;
import org.bukkit.inventory.ItemFlag;

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
    private String use_permission;
    private String display_permission;
    private boolean hideTooltip;
    private Integer customModelData;
    private List<ItemFlag> itemFlags;
    private String base64;

    public String getMaterial() {
        return material;
    }

    public void setMaterial(String material) {
        this.material = material;
    }

    public int getPositionX() {
        return positionX;
    }

    public void setPositionX(int positionX) {
        this.positionX = positionX;
    }

    public int getPositionY() {
        return positionY;
    }

    public void setPositionY(int positionY) {
        this.positionY = positionY;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public List<String> getLore() {
        return lore;
    }

    public void setLore(List<String> lore) {
        this.lore = lore;
    }

    public List<String> getActions() {
        return actions;
    }

    public void setActions(List<String> actions) {
        this.actions = actions;
    }

    public Integer getDurability() {
        return durability;
    }

    public void setDurability(Integer durability) {
        this.durability = durability;
    }

    public List<String> getEnchantments() {
        return enchantments;
    }

    public void setEnchantments(List<String> enchantments) {
        this.enchantments = enchantments;
    }

    public Integer getAmount() {
        return amount;
    }

    public void setAmount(Integer amount) {
        this.amount = amount;
    }

    public List<String> getRequiredItems() {
        return requiredItems;
    }

    public void setRequiredItems(List<String> requiredItems) {
        this.requiredItems = requiredItems;
    }

    public boolean isKeepOpen() {
        return keepOpen;
    }

    public void setKeepOpen(boolean keepOpen) {
        this.keepOpen = keepOpen;
    }

    public String getUse_permission() {
        return use_permission;
    }

    public void setUse_permission(String use_permission) {
        this.use_permission = use_permission;
    }

    public String getDisplay_permission() {
        return display_permission;
    }

    public void setDisplay_permission(String display_permission) {
        this.display_permission = display_permission;
    }

    public boolean isHideTooltip() {
        return hideTooltip;
    }

    public void setHideTooltip(boolean hideTooltip) {
        this.hideTooltip = hideTooltip;
    }

    public Integer getCustomModelData() {
        return customModelData;
    }

    public void setCustomModelData(Integer customModelData) {
        this.customModelData = customModelData;
    }

    public List<ItemFlag> getItemFlags() {
        return itemFlags;
    }

    public void setItemFlags(List<ItemFlag> itemFlags) {
        this.itemFlags = itemFlags;
    }

    public String getBase64() {
        return base64;
    }

    public void setBase64(String base64) {
        this.base64 = base64;
    }

    public boolean hasDisplayPermission() {
        return display_permission != null;
    }

    public boolean hasUsePermission() {
        return use_permission != null;
    }
}
