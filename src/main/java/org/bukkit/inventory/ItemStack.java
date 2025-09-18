package org.bukkit.inventory;

import com.google.common.base.Preconditions;
import com.google.common.collect.Multimap;
import com.mohistmc.youer.Youer;
import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.Locale;
import java.util.Map;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.Registry;
import org.bukkit.Translatable;
import org.bukkit.UndefinedNullability;
import org.bukkit.Utility;
import org.bukkit.attribute.Attribute;
import org.bukkit.attribute.AttributeModifier;
import org.bukkit.block.data.BlockData;
import org.bukkit.configuration.serialization.ConfigurationSerializable;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.inventory.meta.Damageable;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.material.MaterialData;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
// Purpur end

/**
 * Represents a stack of items.
 * <p>
 * <b>IMPORTANT: An <i>Item</i>Stack is only designed to contain <i>items</i>. Do not
 * use this class to encapsulate Materials for which {@link Material#isItem()}
 * returns false.</b>
 */
public class ItemStack implements Cloneable, ConfigurationSerializable, Translatable, net.kyori.adventure.text.event.HoverEventSource<net.kyori.adventure.text.event.HoverEvent.ShowItem>, net.kyori.adventure.translation.Translatable, io.papermc.paper.persistence.PersistentDataViewHolder { // Paper
    private ItemStack craftDelegate; // Paper - always delegate to server-backed stack
    private MaterialData data = null;

    // Paper start - add static factory methods
    /**
     * Creates an itemstack with the specified item type and a count of 1.
     *
     * @param type the item type to use
     * @return a new itemstack
     * @throws IllegalArgumentException if the Material provided is not an item ({@link Material#isItem()})
     */
    @org.jetbrains.annotations.Contract(value = "_ -> new", pure = true)
    public static @NotNull ItemStack of(final @NotNull Material type) {
        return of(type, 1);
    }

    /**
     * Creates an itemstack with the specified item type and count.
     *
     * @param type the item type to use
     * @param amount the count of items in the stack
     * @return a new itemstack
     * @throws IllegalArgumentException if the Material provided is not an item ({@link Material#isItem()})
     * @throws IllegalArgumentException if the amount is less than 1
     */
    @org.jetbrains.annotations.Contract(value = "_, _ -> new", pure = true)
    public static @NotNull ItemStack of(final @NotNull Material type, final int amount) {
        if (type.asItemType() == null) {
            return ItemStack.empty();
        }
        Preconditions.checkArgument(amount > 0, "amount must be greater than 0");
        return java.util.Objects.requireNonNull(type.asItemType(), type + " is not an item").createItemStack(amount); // Paper - delegate
    }
    // Paper end

    // Paper start - pdc
    @Override
    public io.papermc.paper.persistence.@NotNull PersistentDataContainerView getPersistentDataContainer() {
        return this.craftDelegate.getPersistentDataContainer();
    }
    // Paper end - pdc

    @Utility
    protected ItemStack() {}

    /**
     * Defaults stack size to 1, with no extra data.
     * <p>
     * <b>IMPORTANT: An <i>Item</i>Stack is only designed to contain
     * <i>items</i>. Do not use this class to encapsulate Materials for which
     * {@link Material#isItem()} returns false.</b>
     *
     * @param type item material
     * @apiNote use {@link #of(Material)}
     * @see #of(Material)
     */
    @org.jetbrains.annotations.ApiStatus.Obsolete(since = "1.21") // Paper
    public ItemStack(@NotNull final Material type) {
        this(type, 1);
    }

    /**
     * An item stack with no extra data.
     * <p>
     * <b>IMPORTANT: An <i>Item</i>Stack is only designed to contain
     * <i>items</i>. Do not use this class to encapsulate Materials for which
     * {@link Material#isItem()} returns false.</b>
     *
     * @param type item material
     * @param amount stack size
     * @apiNote Use {@link #of(Material, int)}
     * @see #of(Material, int)
     */
    @org.jetbrains.annotations.ApiStatus.Obsolete(since = "1.21") // Paper
    public ItemStack(@NotNull final Material type, final int amount) {
        this(type, amount, (short) 0);
    }

    /**
     * An item stack with the specified damage / durability
     *
     * @param type item material
     * @param amount stack size
     * @param damage durability / damage
     * @deprecated see {@link #setDurability(short)}
     */
    @Deprecated
    public ItemStack(@NotNull final Material type, final int amount, final short damage) {
        this(type, amount, damage, null);
    }

    /**
     * @param type the type
     * @param amount the amount in the stack
     * @param damage the damage value of the item
     * @param data the data value or null
     * @deprecated this method uses an ambiguous data byte object
     */
    @Deprecated(forRemoval = true, since = "1.13")
    public ItemStack(@NotNull Material type, final int amount, final short damage, @Nullable final Byte data) {
        Preconditions.checkArgument(type != null, "Material cannot be null");
        if (type.isLegacy()) {
            if (type.getMaxDurability() > 0) {
                type = Bukkit.getUnsafe().fromLegacy(new MaterialData(type, data == null ? 0 : data), true);
            } else {
                type = Bukkit.getUnsafe().fromLegacy(new MaterialData(type, data == null ? (byte) damage : data), true);
            }
        }
        // Paper start - create delegate
        this.craftDelegate = type == Material.AIR ? ItemStack.empty() : ItemStack.of(type, amount);
        // Paper end - create delegate
        if (damage != 0) {
            setDurability(damage);
        }
        if (data != null) {
            createData(data);
        }
    }

    /**
     * Creates a new item stack derived from the specified stack
     *
     * @param stack the stack to copy
     * @throws IllegalArgumentException if the specified stack is null or
     *     returns an item meta not created by the item factory
     * @apiNote Use {@link #clone()}
     * @see #clone()
     */
    @org.jetbrains.annotations.ApiStatus.Obsolete(since = "1.21") // Paper
    public ItemStack(@NotNull final ItemStack stack) throws IllegalArgumentException {
        Preconditions.checkArgument(stack != null, "Cannot copy null stack");
        this.craftDelegate = stack.clone(); // Paper - delegate
        if (stack.getType().isLegacy()) {
            this.data = stack.getData();
        }
    }

    /**
     * Gets the type of this item
     *
     * @return Type of the items in this stack
     */
    @NotNull
    public Material getType() {
        return this.craftDelegate.getType(); // Paper - delegate
    }

    /**
     * Sets the type of this item
     * <p>
     * Note that in doing so you will reset the MaterialData for this stack.
     * <p>
     * <b>IMPORTANT: An <i>Item</i>Stack is only designed to contain
     * <i>items</i>. Do not use this class to encapsulate Materials for which
     * {@link Material#isItem()} returns false.</b>
     *
     * @param type New type to set the items in this stack to
     * @deprecated <b>Setting the material type of ItemStacks is no longer supported.</b>
     * <p>
     * This method is deprecated due to potential illegal behavior that may occur
     * during the context of which this ItemStack is being used, allowing for certain item validation to be bypassed.
     * It is recommended to instead create a new ItemStack object with the desired
     * Material type, and if possible, set it in the appropriate context.
     *
     * Using this method in ItemStacks passed in events will result in undefined behavior.
     * @see ItemStack#withType(Material)
     */
    @Deprecated // Paper
    public void setType(@NotNull Material type) {
        Preconditions.checkArgument(type != null, "Material cannot be null");
        this.craftDelegate.setType(type); // Paper - delegate
    }
    // Paper start
    /**
     * Creates a new ItemStack with the specified Material type, where the item count and item meta is preserved.
     *
     * @param type The Material type of the new ItemStack.
     * @return A new ItemStack instance with the specified Material type.
     */
    @NotNull
    @org.jetbrains.annotations.Contract(value = "_ -> new", pure = true)
    public ItemStack withType(@NotNull Material type) {
        return this.craftDelegate.withType(type); // Paper - delegate
    }
    // Paper end

    /**
     * Gets the amount of items in this stack
     *
     * @return Amount of items in this stack
     */
    public int getAmount() {
        return this.craftDelegate.getAmount(); // Paper - delegate
    }

    /**
     * Sets the amount of items in this stack
     *
     * @param amount New amount of items in this stack
     */
    public void setAmount(int amount) {
        this.craftDelegate.setAmount(amount); // Paper - delegate
    }

    /**
     * Gets the MaterialData for this stack of items
     *
     * @return MaterialData for this item
     * @deprecated cast to {@link org.bukkit.inventory.meta.BlockDataMeta} and use {@link org.bukkit.inventory.meta.BlockDataMeta#getBlockData(Material)}
     */
    @Nullable
    @Deprecated(forRemoval = true, since = "1.13")
    public MaterialData getData() {
        Material mat = Bukkit.getUnsafe().toLegacy(getType());
        if (data == null && mat != null && mat.getData() != null) {
            data = mat.getNewData((byte) this.getDurability());
        }

        return data;
    }

    /**
     * Sets the MaterialData for this stack of items
     *
     * @param data New MaterialData for this item
     * @deprecated cast to {@link org.bukkit.inventory.meta.BlockDataMeta} and use {@link org.bukkit.inventory.meta.BlockDataMeta#setBlockData(org.bukkit.block.data.BlockData)}
     */
    @Deprecated(forRemoval = true, since = "1.13")
    public void setData(@Nullable MaterialData data) {
        if (data == null) {
            this.data = data;
        } else {
            Material mat = Bukkit.getUnsafe().toLegacy(getType());

            if ((data.getClass() == mat.getData()) || (data.getClass() == MaterialData.class)) {
                this.data = data;
            } else {
                throw new IllegalArgumentException("Provided data is not of type " + mat.getData().getName() + ", found " + data.getClass().getName());
            }
        }
    }

    /**
     * Sets the durability of this item
     *
     * @param durability Durability of this item
     * @deprecated durability is now part of ItemMeta. To avoid confusion and
     * misuse, {@link #getItemMeta()}, {@link #setItemMeta(ItemMeta)} and
     * {@link Damageable#setDamage(int)} should be used instead. This is because
     * any call to this method will be overwritten by subsequent setting of
     * ItemMeta which was created before this call.
     */
    @Deprecated
    public void setDurability(final short durability) {
        this.craftDelegate.setDurability(durability); // Paper - delegate
    }

    /**
     * Gets the durability of this item
     *
     * @return Durability of this item
     * @deprecated see {@link #setDurability(short)}
     */
    @Deprecated
    public short getDurability() {
        return this.craftDelegate.getDurability(); // Paper - delegate
    }

    /**
     * Get the maximum stack size for this item. If this item has a max stack
     * size component ({@link ItemMeta#hasMaxStackSize()}), the value of that
     * component will be returned. Otherwise, this item's Material's {@link
     * Material#getMaxStackSize() default maximum stack size} will be returned
     * instead.
     *
     * @return The maximum you can stack this item to.
     */
    public int getMaxStackSize() {
        return this.craftDelegate.getMaxStackSize(); // Paper - delegate
    }

    private void createData(final byte data) {
        this.data = this.craftDelegate.getType().getNewData(data); // Paper
    }

    @Override
    @Utility
    public String toString() {
        StringBuilder toString = new StringBuilder("ItemStack{").append(getType().name()).append(" x ").append(getAmount());
        if (hasItemMeta()) {
            toString.append(", ").append(getItemMeta());
        }
        return toString.append('}').toString();
    }

    @Override
    public boolean equals(Object obj) {
        return this.craftDelegate.equals(obj); // Paper - delegate
    }

    /**
     * This method is the same as equals, but does not consider stack size
     * (amount).
     *
     * @param stack the item stack to compare to
     * @return true if the two stacks are equal, ignoring the amount
     */
    public boolean isSimilar(@Nullable ItemStack stack) {
        return this.craftDelegate.isSimilar(stack); // Paper - delegate
    }

    @NotNull
    @Override
    public ItemStack clone() {
        return this.craftDelegate.clone(); // Paper - delegate
    }

    @Override
    public int hashCode() {
        return this.craftDelegate.hashCode(); // Paper - delegate
    }

    /**
     * Checks if this ItemStack contains the given {@link Enchantment}
     *
     * @param ench Enchantment to test
     * @return True if this has the given enchantment
     */
    public boolean containsEnchantment(@NotNull Enchantment ench) {
        return this.craftDelegate.containsEnchantment(ench); // Paper - delegate
    }

    /**
     * Gets the level of the specified enchantment on this item stack
     *
     * @param ench Enchantment to check
     * @return Level of the enchantment, or 0
     */
    public int getEnchantmentLevel(@NotNull Enchantment ench) {
        return this.craftDelegate.getEnchantmentLevel(ench); // Paper - delegate
    }

    /**
     * Gets a map containing all enchantments and their levels on this item.
     *
     * @return Map of enchantments.
     */
    @NotNull
    public Map<Enchantment, Integer> getEnchantments() {
        return this.craftDelegate.getEnchantments(); // Paper - delegate
    }

    /**
     * Adds the specified enchantments to this item stack.
     * <p>
     * This method is the same as calling {@link
     * #addEnchantment(org.bukkit.enchantments.Enchantment, int)} for each
     * element of the map.
     *
     * @param enchantments Enchantments to add
     * @throws IllegalArgumentException if the specified enchantments is null
     * @throws IllegalArgumentException if any specific enchantment or level
     *     is null. <b>Warning</b>: Some enchantments may be added before this
     *     exception is thrown.
     */
    @Utility
    public void addEnchantments(@NotNull Map<Enchantment, Integer> enchantments) {
        Preconditions.checkArgument(enchantments != null, "Enchantments cannot be null");
        for (Map.Entry<Enchantment, Integer> entry : enchantments.entrySet()) {
            addEnchantment(entry.getKey(), entry.getValue());
        }
    }

    /**
     * Adds the specified {@link Enchantment} to this item stack.
     * <p>
     * If this item stack already contained the given enchantment (at any
     * level), it will be replaced.
     *
     * @param ench Enchantment to add
     * @param level Level of the enchantment
     * @throws IllegalArgumentException if enchantment null, or enchantment is
     *     not applicable
     */
    @Utility
    public void addEnchantment(@NotNull Enchantment ench, int level) {
        Preconditions.checkArgument(ench != null, "Enchantment cannot be null");
        if ((level < ench.getStartLevel()) || (level > ench.getMaxLevel())) {
            throw new IllegalArgumentException("Enchantment level is either too low or too high (given " + level + ", bounds are " + ench.getStartLevel() + " to " + ench.getMaxLevel() + ")");
        } else if (!ench.canEnchantItem(this)) {
            throw new IllegalArgumentException("Specified enchantment cannot be applied to this itemstack");
        }

        addUnsafeEnchantment(ench, level);
    }

    /**
     * Adds the specified enchantments to this item stack in an unsafe manner.
     * <p>
     * This method is the same as calling {@link
     * #addUnsafeEnchantment(org.bukkit.enchantments.Enchantment, int)} for
     * each element of the map.
     *
     * @param enchantments Enchantments to add
     */
    @Utility
    public void addUnsafeEnchantments(@NotNull Map<Enchantment, Integer> enchantments) {
        for (Map.Entry<Enchantment, Integer> entry : enchantments.entrySet()) {
            addUnsafeEnchantment(entry.getKey(), entry.getValue());
        }
    }

    /**
     * Adds the specified {@link Enchantment} to this item stack.
     * <p>
     * If this item stack already contained the given enchantment (at any
     * level), it will be replaced.
     * <p>
     * This method is unsafe and will ignore level restrictions or item type.
     * Use at your own discretion.
     *
     * @param ench Enchantment to add
     * @param level Level of the enchantment
     */
    public void addUnsafeEnchantment(@NotNull Enchantment ench, int level) {
        this.craftDelegate.addUnsafeEnchantment(ench, level); // Paper - delegate
    }

    /**
     * Removes the specified {@link Enchantment} if it exists on this
     * ItemStack
     *
     * @param ench Enchantment to remove
     * @return Previous level, or 0
     */
    public int removeEnchantment(@NotNull Enchantment ench) {
        return this.craftDelegate.removeEnchantment(ench); // Paper - delegate
    }

    /**
     * Removes all enchantments on this ItemStack.
     */
    public void removeEnchantments() {
        this.craftDelegate.removeEnchantments(); // Paper - delegate
    }

    @Override
    @NotNull
    @Utility
    public Map<String, Object> serialize() {
        Map<String, Object> result = new LinkedHashMap<String, Object>();

        result.put("v", Bukkit.getUnsafe().getDataVersion()); // Include version to indicate we are using modern material names (or LEGACY prefix)
        result.put("type", getType().name());

        if (getAmount() != 1) {
            result.put("amount", getAmount());
        }

        ItemMeta meta = getItemMeta();
        if (!Bukkit.getItemFactory().equals(meta, null)) {
            result.put("meta", meta);
        }

        return result;
    }

    /**
     * Required method for configuration serialization
     *
     * @param args map to deserialize
     * @return deserialized item stack
     * @see ConfigurationSerializable
     */
    @NotNull
    public static ItemStack deserialize(@NotNull Map<String, Object> args) {
        int version = (args.containsKey("v")) ? ((Number) args.get("v")).intValue() : -1;
        short damage = 0;
        int amount = 1;

        if (args.containsKey("damage")) {
            damage = ((Number) args.get("damage")).shortValue();
        }

        Material type;
        if (version < 0) {
            type = Material.getMaterial(Material.LEGACY_PREFIX + (String) args.get("type"));

            byte dataVal = (type != null && type.getMaxDurability() == 0) ? (byte) damage : 0; // Actually durable items get a 0 passed into conversion
            type = Bukkit.getUnsafe().fromLegacy(new MaterialData(type, dataVal), true);

            // We've converted now so the data val isn't a thing and can be reset
            if (dataVal != 0) {
                damage = 0;
            }
        } else {
            type = Bukkit.getUnsafe().getMaterial((String) args.get("type"), version);
        }

        if (args.containsKey("amount")) {
            amount = ((Number) args.get("amount")).intValue();
        }

        if (type == null) {
            Youer.LOGGER.error(Youer.i18n.as("bukkit.ItemStack.typenull", args.get("type")));
            type = Material.BROWN_MUSHROOM;
        }

        ItemStack result = new ItemStack(type, amount, damage);

        if (args.containsKey("enchantments")) { // Backward compatiblity, @deprecated
            Object raw = args.get("enchantments");

            if (raw instanceof Map) {
                Map<?, ?> map = (Map<?, ?>) raw;

                for (Map.Entry<?, ?> entry : map.entrySet()) {
                    String stringKey = entry.getKey().toString();
                    stringKey = Bukkit.getUnsafe().get(Enchantment.class, stringKey);
                    NamespacedKey key = NamespacedKey.fromString(stringKey.toLowerCase(Locale.ROOT));

                    Enchantment enchantment = Bukkit.getUnsafe().get(Registry.ENCHANTMENT, key);

                    if ((enchantment != null) && (entry.getValue() instanceof Integer)) {
                        result.addUnsafeEnchantment(enchantment, (Integer) entry.getValue());
                    }
                }
            }
        } else if (args.containsKey("meta")) { // We cannot and will not have meta when enchantments (pre-ItemMeta) exist
            Object raw = args.get("meta");
            if (raw instanceof ItemMeta) {
                ((ItemMeta) raw).setVersion(version);
                // Paper start - for pre 1.20.5 itemstacks, add HIDE_STORED_ENCHANTS flag if HIDE_ADDITIONAL_TOOLTIP is set
                if (version < 3837) { // 1.20.5
                    if (((ItemMeta) raw).hasItemFlag(ItemFlag.HIDE_ADDITIONAL_TOOLTIP)) {
                        ((ItemMeta) raw).addItemFlags(ItemFlag.HIDE_STORED_ENCHANTS);
                    }
                }
                // Paper end
                result.setItemMeta((ItemMeta) raw);
            }
        }

        if (version < 0) {
            // Set damage again incase meta overwrote it
            if (args.containsKey("damage")) {
                result.setDurability(damage);
            }
        }

        return result.ensureServerConversions(); // Paper
    }

    // Paper start
    /**
     * Edits the {@link ItemMeta} of this stack.
     * <p>
     * The {@link java.util.function.Consumer} must only interact
     * with this stack's {@link ItemMeta} through the provided {@link ItemMeta} instance.
     * Calling this method or any other meta-related method of the {@link ItemStack} class
     * (such as {@link #getItemMeta()}, {@link #addItemFlags(ItemFlag...)}, {@link #lore()}, etc.)
     * from inside the consumer is disallowed and will produce undefined results or exceptions.
     * </p>
     *
     * @param consumer the meta consumer
     * @return {@code true} if the edit was successful, {@code false} otherwise
     */
    public boolean editMeta(final @NotNull java.util.function.Consumer<? super ItemMeta> consumer) {
        return editMeta(ItemMeta.class, consumer);
    }

    /**
     * Edits the {@link ItemMeta} of this stack if the meta is of the specified type.
     * <p>
     * The {@link java.util.function.Consumer} must only interact
     * with this stack's {@link ItemMeta} through the provided {@link ItemMeta} instance.
     * Calling this method or any other meta-related method of the {@link ItemStack} class
     * (such as {@link #getItemMeta()}, {@link #addItemFlags(ItemFlag...)}, {@link #lore()}, etc.)
     * from inside the consumer is disallowed and will produce undefined results or exceptions.
     * </p>
     *
     * @param metaClass the type of meta to edit
     * @param consumer the meta consumer
     * @param <M> the meta type
     * @return {@code true} if the edit was successful, {@code false} otherwise
     */
    public <M extends ItemMeta> boolean editMeta(final @NotNull Class<M> metaClass, final @NotNull java.util.function.Consumer<@NotNull ? super M> consumer) {
        final @Nullable ItemMeta meta = this.getItemMeta();
        if (metaClass.isInstance(meta)) {
            consumer.accept((M) meta);
            this.setItemMeta(meta);
            return true;
        }
        return false;
    }
    // Paper end

    /**
     * Get a copy of this ItemStack's {@link ItemMeta}.
     *
     * @return a copy of the current ItemStack's ItemData
     */
    @UndefinedNullability // Paper
    public ItemMeta getItemMeta() {
        return this.craftDelegate.getItemMeta(); // Paper - delegate
    }

    /**
     * Checks to see if any meta data has been defined.
     *
     * @return Returns true if some meta data has been set for this item
     */
    public boolean hasItemMeta() {
        return this.craftDelegate.hasItemMeta(); // Paper - delegate
    }

    /**
     * Set the ItemMeta of this ItemStack.
     *
     * @param itemMeta new ItemMeta, or null to indicate meta data be cleared.
     * @return True if successfully applied ItemMeta, see {@link
     *     ItemFactory#isApplicable(ItemMeta, ItemStack)}
     * @throws IllegalArgumentException if the item meta was not created by
     *     the {@link ItemFactory}
     */
    public boolean setItemMeta(@Nullable ItemMeta itemMeta) {
        return this.craftDelegate.setItemMeta(itemMeta); // Paper - delegate
    }

    // Paper - delegate

    @Override
    @NotNull
    @Deprecated(forRemoval = true) // Paper
    public String getTranslationKey() {
        return Bukkit.getUnsafe().getTranslationKey(this);
    }

    // Paper start
    /**
     * Randomly enchants a copy of this {@link ItemStack} using the given experience levels.
     *
     * <p>If this ItemStack is already enchanted, the existing enchants will be removed before enchanting.</p>
     *
     * <p>Levels must be in range {@code [1, 30]}.</p>
     *
     * @param levels levels to use for enchanting
     * @param allowTreasure whether to allow enchantments where {@link org.bukkit.enchantments.Enchantment#isTreasure()} returns true
     * @param random {@link java.util.Random} instance to use for enchanting
     * @return enchanted copy of the provided ItemStack
     * @throws IllegalArgumentException on bad arguments
     */
    @NotNull
    public ItemStack enchantWithLevels(final @org.jetbrains.annotations.Range(from = 1, to = 30) int levels, final boolean allowTreasure, final @NotNull java.util.Random random) {
        return Bukkit.getServer().getItemFactory().enchantWithLevels(this, levels, allowTreasure, random);
    }

    /**
     * Randomly enchants a copy of this {@link ItemStack} using the given experience levels.
     *
     * <p>If the provided ItemStack is already enchanted, the existing enchants will be removed before enchanting.</p>
     *
     * <p>Levels must be in range {@code [1, 30]}.</p>
     *
     * @param levels levels to use for enchanting
     * @param keySet registry key set defining the set of possible enchantments, e.g. {@link io.papermc.paper.registry.keys.tags.EnchantmentTagKeys#IN_ENCHANTING_TABLE}.
     * @param random {@link java.util.Random} instance to use for enchanting
     * @return enchanted copy of the provided ItemStack
     * @throws IllegalArgumentException on bad arguments
     */
    public @NotNull ItemStack enchantWithLevels(final @org.jetbrains.annotations.Range(from = 1, to = 30) int levels, final @NotNull io.papermc.paper.registry.set.RegistryKeySet<@NotNull Enchantment> keySet, final @NotNull java.util.Random random) {
        return Bukkit.getItemFactory().enchantWithLevels(this, levels, keySet, random);
    }

    @NotNull
    @Override
    public net.kyori.adventure.text.event.HoverEvent<net.kyori.adventure.text.event.HoverEvent.ShowItem> asHoverEvent(final @NotNull java.util.function.UnaryOperator<net.kyori.adventure.text.event.HoverEvent.ShowItem> op) {
        return org.bukkit.Bukkit.getServer().getItemFactory().asHoverEvent(this, op);
    }

    /**
     * Get the formatted display name of the {@link ItemStack}.
     *
     * @return display name of the {@link ItemStack}
     */
    public net.kyori.adventure.text.@NotNull Component displayName() {
        return Bukkit.getServer().getItemFactory().displayName(this);
    }

    /**
     * Minecraft updates are converting simple item stacks into more complex NBT oriented Item Stacks.
     *
     * Use this method to ensure any desired data conversions are processed.
     * The input itemstack will not be the same as the returned itemstack.
     *
     * @return A potentially Data Converted ItemStack
     */
    @NotNull
    public ItemStack ensureServerConversions() {
        return Bukkit.getServer().getItemFactory().ensureServerConversions(this);
    }

    /**
     * Deserializes this itemstack from raw NBT bytes. NBT is safer for data migrations as it will
     * use the built in data converter instead of bukkits dangerous serialization system.
     *
     * This expects that the DataVersion was stored on the root of the Compound, as saved from
     * the {@link #serializeAsBytes()} API returned.
     * @param bytes bytes representing an item in NBT
     * @return ItemStack migrated to this version of Minecraft if needed.
     */
    @NotNull
    public static ItemStack deserializeBytes(@NotNull byte[] bytes) {
        return org.bukkit.Bukkit.getUnsafe().deserializeItem(bytes);
    }

    /**
     * Serializes this itemstack to raw bytes in NBT. NBT is safer for data migrations as it will
     * use the built in data converter instead of bukkits dangerous serialization system.
     * @return bytes representing this item in NBT.
     */
    @NotNull
    public byte[] serializeAsBytes() {
        return org.bukkit.Bukkit.getUnsafe().serializeItem(this);
    }

    /**
     * The current version byte of the item array format used in {@link #serializeItemsAsBytes(java.util.Collection)}
     * and {@link #deserializeItemsFromBytes(byte[])} respectively.
     */
    private static final byte ARRAY_SERIALIZATION_VERSION = 1;

    /**
     * Serializes a collection of items to raw bytes in NBT. Serializes null items as {@link #empty()}.
     * <p>
     * If you need a string representation to put into a file, you can for example use {@link java.util.Base64} encoding.
     *
     * @param items items to serialize
     * @return bytes representing the items in NBT
     * @see #serializeAsBytes()
     */
    public static byte @NotNull [] serializeItemsAsBytes(java.util.@NotNull Collection<ItemStack> items) {
        try (final java.io.ByteArrayOutputStream outputStream = new java.io.ByteArrayOutputStream()) {
            final java.io.DataOutput output = new java.io.DataOutputStream(outputStream);
            output.writeByte(ARRAY_SERIALIZATION_VERSION);
            output.writeInt(items.size());
            for (final ItemStack item : items) {
                if (item == null || item.isEmpty()) {
                    // Ensure the correct order by including empty/null items
                    output.writeInt(0);
                    continue;
                }

                final byte[] itemBytes = item.serializeAsBytes();
                output.writeInt(itemBytes.length);
                output.write(itemBytes);
            }
            return outputStream.toByteArray();
        } catch (final java.io.IOException e) {
            throw new RuntimeException("Error while writing itemstack", e);
        }
    }

    /**
     * Serializes a collection of items to raw bytes in NBT. Serializes null items as {@link #empty()}.
     * <p>
     * If you need a string representation to put into a file, you can for example use {@link java.util.Base64} encoding.
     *
     * @param items items to serialize
     * @return bytes representing the items in NBT
     * @see #serializeAsBytes()
     */
    public static byte @NotNull [] serializeItemsAsBytes(@Nullable ItemStack @NotNull [] items) {
        return serializeItemsAsBytes(java.util.Arrays.asList(items));
    }

    /**
     * Deserializes this itemstack from raw NBT bytes.
     * <p>
     * If you need a string representation to put into a file, you can for example use {@link java.util.Base64} encoding.
     *
     * @param bytes bytes representing an item in NBT
     * @return ItemStack array migrated to this version of Minecraft if needed
     * @see #deserializeBytes(byte[])
     */
    public static @NotNull ItemStack @NotNull [] deserializeItemsFromBytes(final byte @NotNull [] bytes) {
        try (final java.io.ByteArrayInputStream inputStream = new java.io.ByteArrayInputStream(bytes)) {
            final java.io.DataInputStream input = new java.io.DataInputStream(inputStream);
            final byte version = input.readByte();
            if (version != ARRAY_SERIALIZATION_VERSION) {
                throw new IllegalArgumentException("Unsupported version or bad data: " + version);
            }

            final int count = input.readInt();
            final ItemStack[] items = new ItemStack[count];
            for (int i = 0; i < count; i++) {
                final int length = input.readInt();
                if (length == 0) {
                    // Empty item, keep entry as empty
                    items[i] = ItemStack.empty();
                    continue;
                }

                final byte[] itemBytes = new byte[length];
                input.read(itemBytes);
                items[i] = ItemStack.deserializeBytes(itemBytes);
            }
            return items;
        } catch (final java.io.IOException e) {
            throw new RuntimeException("Error while reading itemstack", e);
        }
    }

    /**
     * Gets the Display name as seen in the Client.
     * Currently the server only supports the English language. To override this,
     * You must replace the language file embedded in the server jar.
     *
     * @return Display name of Item
     * @deprecated {@link ItemStack} implements {@link net.kyori.adventure.translation.Translatable}; use that and
     * {@link net.kyori.adventure.text.Component#translatable(net.kyori.adventure.translation.Translatable)} instead.
     */
    @Nullable
    @Deprecated
    public String getI18NDisplayName() {
        return Bukkit.getServer().getItemFactory().getI18NDisplayName(this);
    }

    /**
     * @deprecated use {@link #getMaxItemUseDuration(org.bukkit.entity.LivingEntity)}; crossbows, later possibly more items require an entity parameter
     */
    @Deprecated(forRemoval = true)
    public int getMaxItemUseDuration() {
        return getMaxItemUseDuration(null);
    }

    public int getMaxItemUseDuration(@NotNull final org.bukkit.entity.LivingEntity entity) {
        return this.craftDelegate.getMaxItemUseDuration(entity); // Paper - delegate
    }

    /**
     * Clones the itemstack and returns it a single quantity.
     * @return The new itemstack with 1 quantity
     */
    @NotNull
    public ItemStack asOne() {
        return asQuantity(1);
    }

    /**
     * Clones the itemstack and returns it as the specified quantity
     * @param qty The quantity of the cloned item
     * @return The new itemstack with specified quantity
     */
    @NotNull
    public ItemStack asQuantity(int qty) {
        ItemStack clone = clone();
        clone.setAmount(qty);
        return clone;
    }

    /**
     * Adds 1 to this itemstack. Will not go over the items max stack size.
     * @return The same item (not a clone)
     */
    @NotNull
    public ItemStack add() {
        return add(1);
    }

    /**
     * Adds quantity to this itemstack. Will not go over the items max stack size.
     *
     * @param qty The amount to add
     * @return The same item (not a clone)
     */
    @NotNull
    public ItemStack add(int qty) {
        setAmount(Math.min(getMaxStackSize(), getAmount() + qty));
        return this;
    }

    /**
     * Subtracts 1 to this itemstack.  Going to 0 or less will invalidate the item.
     * @return The same item (not a clone)
     */
    @NotNull
    public ItemStack subtract() {
        return subtract(1);
    }

    /**
     * Subtracts quantity to this itemstack. Going to 0 or less will invalidate the item.
     *
     * @param qty The amount to add
     * @return The same item (not a clone)
     */
    @NotNull
    public ItemStack subtract(int qty) {
        setAmount(Math.max(0, getAmount() - qty));
        return this;
    }

    /**
     * If the item has lore, returns it, else it will return null
     * @return The lore, or null
     * @deprecated in favor of {@link #lore()}
     */
    @Deprecated
    public @Nullable java.util.List<String> getLore() {
        if (!hasItemMeta()) {
            return null;
        }
        ItemMeta itemMeta = getItemMeta();
        if (!itemMeta.hasLore()) {
            return null;
        }
        return itemMeta.getLore();
    }

    /**
     * If the item has lore, returns it, else it will return null
     * @return The lore, or null
     */
    public @Nullable java.util.List<net.kyori.adventure.text.Component> lore() {
        if (!this.hasItemMeta()) {
            return null;
        }
        final ItemMeta itemMeta = getItemMeta();
        if (!itemMeta.hasLore()) {
            return null;
        }
        return itemMeta.lore();
    }

    /**
     * Sets the lore for this item.
     * Removes lore when given null.
     *
     * @param lore the lore that will be set
     * @deprecated in favour of {@link #lore(java.util.List)}
     */
    @Deprecated
    public void setLore(@Nullable java.util.List<String> lore) {
        ItemMeta itemMeta = getItemMeta();
        if (itemMeta == null) {
            throw new IllegalStateException("Cannot set lore on " + getType());
        }
        itemMeta.setLore(lore);
        setItemMeta(itemMeta);
    }

    /**
     * Sets the lore for this item.
     * Removes lore when given null.
     *
     * @param lore the lore that will be set
     */
    public void lore(@Nullable java.util.List<? extends net.kyori.adventure.text.Component> lore) {
        ItemMeta itemMeta = getItemMeta();
        if (itemMeta == null) {
            throw new IllegalStateException("Cannot set lore on " + getType());
        }
        itemMeta.lore(lore);
        this.setItemMeta(itemMeta);
    }

    /**
     * Set itemflags which should be ignored when rendering a ItemStack in the Client. This Method does silently ignore double set itemFlags.
     *
     * @param itemFlags The hideflags which shouldn't be rendered
     */
    public void addItemFlags(@NotNull ItemFlag... itemFlags) {
        ItemMeta itemMeta = getItemMeta();
        if (itemMeta == null) {
            throw new IllegalStateException("Cannot add flags on " + getType());
        }
        itemMeta.addItemFlags(itemFlags);
        setItemMeta(itemMeta);
    }

    /**
     * Remove specific set of itemFlags. This tells the Client it should render it again. This Method does silently ignore double removed itemFlags.
     *
     * @param itemFlags Hideflags which should be removed
     */
    public void removeItemFlags(@NotNull ItemFlag... itemFlags) {
        ItemMeta itemMeta = getItemMeta();
        if (itemMeta == null) {
            throw new IllegalStateException("Cannot remove flags on " + getType());
        }
        itemMeta.removeItemFlags(itemFlags);
        setItemMeta(itemMeta);
    }

    /**
     * Get current set itemFlags. The collection returned is unmodifiable.
     *
     * @return A set of all itemFlags set
     */
    @NotNull
    public java.util.Set<ItemFlag> getItemFlags() {
        ItemMeta itemMeta = getItemMeta();
        if (itemMeta == null) {
            return java.util.Collections.emptySet();
        }
        return itemMeta.getItemFlags();
    }

    /**
     * Check if the specified flag is present on this item.
     *
     * @param flag the flag to check
     * @return if it is present
     */
    public boolean hasItemFlag(@NotNull ItemFlag flag) {
        ItemMeta itemMeta = getItemMeta();
        return itemMeta != null && itemMeta.hasItemFlag(flag);
    }

    /**
     * {@inheritDoc}
     * <p>
     * This is not the same as getting the translation key
     * for the material of this itemstack.
     */
    @Override
    public @NotNull String translationKey() {
        return Bukkit.getUnsafe().getTranslationKey(this);
    }

    /**
     * Gets the item rarity of the itemstack. The rarity can change based on enchantments.
     *
     * @return the itemstack rarity
     * @deprecated Use {@link ItemMeta#hasRarity()} and {@link ItemMeta#getRarity()}
     */
    @NotNull
    @Deprecated(forRemoval = true, since = "1.20.5")
    public io.papermc.paper.inventory.ItemRarity getRarity() {
        return io.papermc.paper.inventory.ItemRarity.valueOf(this.getItemMeta().getRarity().name());
    }

    /**
     * Checks if an itemstack can repair this itemstack.
     * Returns false if {@code this} or {@code repairMaterial}'s type is not an item ({@link Material#isItem()}).
     *
     * @param repairMaterial the repair material
     * @return true if it is repairable by, false if not
     */
    public boolean isRepairableBy(@NotNull ItemStack repairMaterial) {
        return Bukkit.getUnsafe().isValidRepairItemStack(this, repairMaterial);
    }

    /**
     * Checks if this itemstack can repair another.
     * Returns false if {@code this} or {@code toBeRepaired}'s type is not an item ({@link Material#isItem()}).
     *
     * @param toBeRepaired the itemstack to be repaired
     * @return true if it can repair, false if not
     */
    public boolean canRepair(@NotNull ItemStack toBeRepaired) {
        return Bukkit.getUnsafe().isValidRepairItemStack(toBeRepaired, this);
    }

    /**
     * Damages this itemstack by the specified amount. This
     * runs all logic associated with damaging an itemstack like
     * events and stat changes.
     *
     * @param amount the amount of damage to do
     * @param livingEntity the entity related to the damage
     * @return the damaged itemstack or an empty one if it broke. May return the same instance of ItemStack
     * @see org.bukkit.entity.LivingEntity#damageItemStack(EquipmentSlot, int) to damage itemstacks in equipment slots
     */
    public @NotNull ItemStack damage(int amount, @NotNull org.bukkit.entity.LivingEntity livingEntity) {
        return livingEntity.damageItemStack(this, amount);
    }

    /**
     * Returns an empty item stack, consists of an air material and a stack size of 0.
     *
     * Any item stack with a material of air or a stack size of 0 is seen
     * as being empty by {@link ItemStack#isEmpty}.
     */
    @NotNull
    public static ItemStack empty() {
        //noinspection deprecation
        return Bukkit.getUnsafe().createEmptyStack(); // Paper - proxy ItemStack
    }

    /**
     * Returns whether this item stack is empty and contains no item. This means
     * it is either air or the stack has a size of 0.
     */
    public boolean isEmpty() {
        return this.craftDelegate.isEmpty(); // Paper - delegate
    }
    // Paper end
    // Paper start - expose itemstack tooltip lines
    /**
     * Computes the tooltip lines for this stack.
     * <p>
     * <b>Disclaimer:</b>
     * Tooltip contents are not guaranteed to be consistent across different
     * Minecraft versions.
     *
     * @param tooltipContext the tooltip context
     * @param player a player for player-specific tooltip lines
     * @return an immutable list of components (can be empty)
     */
    @SuppressWarnings("deprecation") // abusing unsafe as a bridge
    public java.util.@NotNull @org.jetbrains.annotations.Unmodifiable List<net.kyori.adventure.text.Component> computeTooltipLines(final @NotNull io.papermc.paper.inventory.tooltip.TooltipContext tooltipContext, final @Nullable org.bukkit.entity.Player player) {
        return Bukkit.getUnsafe().computeTooltipLines(this, tooltipContext, player);
    }
    // Paper end - expose itemstack tooltip lines

    // Purpur start
    /**
     * Gets the display name that is set.
     * <p>
     * Plugins should check that hasDisplayName() returns <code>true</code>
     * before calling this method.
     *
     * @return the display name that is set
     */
    @NotNull
    public String getDisplayName() {
        return this.craftDelegate.getDisplayName();
    }

    /**
     * Sets the display name.
     *
     * @param name the name to set
     */
    public void setDisplayName(@Nullable String name) {
        this.craftDelegate.setDisplayName(name);
    }

    /**
     * Checks for existence of a display name.
     *
     * @return true if this has a display name
     */
    public boolean hasDisplayName() {
        return this.craftDelegate.hasDisplayName();
    }

    /**
     * Gets the localized display name that is set.
     * <p>
     * Plugins should check that hasLocalizedName() returns <code>true</code>
     * before calling this method.
     *
     * @return the localized name that is set
     */
    @NotNull
    public String getLocalizedName() {
        return this.craftDelegate.getLocalizedName();
    }

    /**
     * Sets the localized name.
     *
     * @param name the name to set
     */
    public void setLocalizedName(@Nullable String name) {
        this.craftDelegate.setLocalizedName(name);
    }

    /**
     * Checks for existence of a localized name.
     *
     * @return true if this has a localized name
     */
    public boolean hasLocalizedName() {
        return this.craftDelegate.hasLocalizedName();
    }

    /**
     * Checks for existence of lore.
     *
     * @return true if this has lore
     */
    public boolean hasLore() {
        return this.craftDelegate.hasLore();
    }

    /**
     * Checks for existence of the specified enchantment.
     *
     * @param ench enchantment to check
     * @return true if this enchantment exists for this meta
     */
    public boolean hasEnchant(@NotNull Enchantment ench) {
        return this.craftDelegate.hasEnchant(ench);
    }

    /**
     * Checks for the level of the specified enchantment.
     *
     * @param ench enchantment to check
     * @return The level that the specified enchantment has, or 0 if none
     */
    public int getEnchantLevel(@NotNull Enchantment ench) {
        return this.craftDelegate.getEnchantLevel(ench);
    }

    /**
     * Returns a copy the enchantments in this ItemMeta. <br>
     * Returns an empty map if none.
     *
     * @return An immutable copy of the enchantments
     */
    @NotNull
    public Map<Enchantment, Integer> getEnchants() {
        return this.craftDelegate.getEnchants();
    }

    /**
     * Adds the specified enchantment to this item meta.
     *
     * @param ench Enchantment to add
     * @param level Level for the enchantment
     * @param ignoreLevelRestriction this indicates the enchantment should be
     *     applied, ignoring the level limit
     * @return true if the item meta changed as a result of this call, false
     *     otherwise
     */
    public boolean addEnchant(@NotNull Enchantment ench, int level, boolean ignoreLevelRestriction) {
        return this.craftDelegate.addEnchant(ench, level, ignoreLevelRestriction);
    }

    /**
     * Removes the specified enchantment from this item meta.
     *
     * @param ench Enchantment to remove
     * @return true if the item meta changed as a result of this call, false
     *     otherwise
     */
    public boolean removeEnchant(@NotNull Enchantment ench) {
        return this.craftDelegate.removeEnchant(ench);
    }

    /**
     * Checks for the existence of any enchantments.
     *
     * @return true if an enchantment exists on this meta
     */
    public boolean hasEnchants() {
        return this.craftDelegate.hasEnchants();
    }

    /**
     * Checks if the specified enchantment conflicts with any enchantments in
     * this ItemMeta.
     *
     * @param ench enchantment to test
     * @return true if the enchantment conflicts, false otherwise
     */
    public boolean hasConflictingEnchant(@NotNull Enchantment ench) {
        return this.craftDelegate.hasConflictingEnchant(ench);
    }

    /**
     * Sets the custom model data.
     * <p>
     * CustomModelData is an integer that may be associated client side with a
     * custom item model.
     *
     * @param data the data to set, or null to clear
     */
    public void setCustomModelData(@Nullable Integer data) {
        this.craftDelegate.setCustomModelData(data);
    }

    /**
     * Gets the custom model data that is set.
     * <p>
     * CustomModelData is an integer that may be associated client side with a
     * custom item model.
     * <p>
     * Plugins should check that hasCustomModelData() returns <code>true</code>
     * before calling this method.
     *
     * @return the localized name that is set
     */
    public int getCustomModelData() {
        return this.craftDelegate.getCustomModelData();
    }

    /**
     * Checks for existence of custom model data.
     * <p>
     * CustomModelData is an integer that may be associated client side with a
     * custom item model.
     *
     * @return true if this has custom model data
     */
    public boolean hasCustomModelData() {
        return this.craftDelegate.hasCustomModelData();
    }

    /**
     * Returns whether the item has block data currently attached to it.
     *
     * @return whether block data is already attached
     */
    public boolean hasBlockData() {
        return this.craftDelegate.hasBlockData();
    }

    /**
     * Returns the currently attached block data for this item or creates a new
     * one if one doesn't exist.
     *
     * The state is a copy, it must be set back (or to another item) with
     * {@link #setBlockData(BlockData)}
     *
     * @param material the material we wish to get this data in the context of
     * @return the attached data or new data
     */
    @NotNull
    public BlockData getBlockData(@NotNull Material material) {
        return this.craftDelegate.getBlockData(material);
    }

    /**
     * Attaches a copy of the passed block data to the item.
     *
     * @param blockData the block data to attach to the block.
     * @throws IllegalArgumentException if the blockData is null or invalid for
     * this item.
     */
    public void setBlockData(@NotNull BlockData blockData) {
        this.craftDelegate.setBlockData(blockData);
    }

    /**
     * Gets the repair penalty
     *
     * @return the repair penalty
     */
    public int getRepairCost() {
        return this.craftDelegate.getRepairCost();
    }

    /**
     * Sets the repair penalty
     *
     * @param cost repair penalty
     */
    public void setRepairCost(int cost) {
        this.craftDelegate.setRepairCost(cost);
    }

    /**
     * Checks to see if this has a repair penalty
     *
     * @return true if this has a repair penalty
     */
    public boolean hasRepairCost() {
        return this.craftDelegate.hasRepairCost();
    }

    /**
     * Return if the unbreakable tag is true. An unbreakable item will not lose
     * durability.
     *
     * @return true if the unbreakable tag is true
     */
    public boolean isUnbreakable() {
        return this.craftDelegate.isUnbreakable();
    }

    /**
     * Sets the unbreakable tag. An unbreakable item will not lose durability.
     *
     * @param unbreakable true if set unbreakable
     */
    public void setUnbreakable(boolean unbreakable) {
        this.craftDelegate.setUnbreakable(unbreakable);
    }

    /**
     * Checks for the existence of any AttributeModifiers.
     *
     * @return true if any AttributeModifiers exist
     */
    public boolean hasAttributeModifiers() {
        return this.craftDelegate.hasAttributeModifiers();
    }

    /**
     * Return an immutable copy of all Attributes and
     * their modifiers in this ItemMeta.<br>
     * Returns null if none exist.
     *
     * @return an immutable {@link Multimap} of Attributes
     *         and their AttributeModifiers, or null if none exist
     */
    @Nullable
    public Multimap<Attribute, AttributeModifier> getAttributeModifiers() {
        return this.craftDelegate.getAttributeModifiers();
    }

    /**
     * Return an immutable copy of all {@link Attribute}s and their
     * {@link AttributeModifier}s for a given {@link EquipmentSlot}.<br>
     * Any {@link AttributeModifier} that does have have a given
     * {@link EquipmentSlot} will be returned. This is because
     * AttributeModifiers without a slot are active in any slot.<br>
     * If there are no attributes set for the given slot, an empty map
     * will be returned.
     *
     * @param slot the {@link EquipmentSlot} to check
     * @return the immutable {@link Multimap} with the
     *         respective Attributes and modifiers, or an empty map
     *         if no attributes are set.
     */
    @NotNull
    public Multimap<Attribute, AttributeModifier> getAttributeModifiers(@Nullable EquipmentSlot slot) {
        return this.craftDelegate.getAttributeModifiers(slot);
    }

    /**
     * Return an immutable copy of all {@link AttributeModifier}s
     * for a given {@link Attribute}
     *
     * @param attribute the {@link Attribute}
     * @return an immutable collection of {@link AttributeModifier}s
     *          or null if no AttributeModifiers exist for the Attribute.
     * @throws NullPointerException if Attribute is null
     */
    @Nullable
    public Collection<AttributeModifier> getAttributeModifiers(@NotNull Attribute attribute) {
        return this.craftDelegate.getAttributeModifiers(attribute);
    }

    /**
     * Add an Attribute and it's Modifier.
     * AttributeModifiers can now support {@link EquipmentSlot}s.
     * If not set, the {@link AttributeModifier} will be active in ALL slots.
     * <br>
     * Two {@link AttributeModifier}s that have the same {@link java.util.UUID}
     * cannot exist on the same Attribute.
     *
     * @param attribute the {@link Attribute} to modify
     * @param modifier the {@link AttributeModifier} specifying the modification
     * @return true if the Attribute and AttributeModifier were
     *         successfully added
     * @throws NullPointerException if Attribute is null
     * @throws NullPointerException if AttributeModifier is null
     * @throws IllegalArgumentException if AttributeModifier already exists
     */
    public boolean addAttributeModifier(@NotNull Attribute attribute, @NotNull AttributeModifier modifier) {
        return this.craftDelegate.addAttributeModifier(attribute, modifier);
    }

    /**
     * Set all {@link Attribute}s and their {@link AttributeModifier}s.
     * To clear all currently set Attributes and AttributeModifiers use
     * null or an empty Multimap.
     * If not null nor empty, this will filter all entries that are not-null
     * and add them to the ItemStack.
     *
     * @param attributeModifiers the new Multimap containing the Attributes
     *                           and their AttributeModifiers
     */
    public void setAttributeModifiers(@Nullable Multimap<Attribute, AttributeModifier> attributeModifiers) {
        this.craftDelegate.setAttributeModifiers(attributeModifiers);
    }

    /**
     * Remove all {@link AttributeModifier}s associated with the given
     * {@link Attribute}.
     * This will return false if nothing was removed.
     *
     * @param attribute attribute to remove
     * @return  true if all modifiers were removed from a given
     *                  Attribute. Returns false if no attributes were
     *                  removed.
     * @throws NullPointerException if Attribute is null
     */
    public boolean removeAttributeModifier(@NotNull Attribute attribute) {
        return this.craftDelegate.removeAttributeModifier(attribute);
    }

    /**
     * Remove all {@link Attribute}s and {@link AttributeModifier}s for a
     * given {@link EquipmentSlot}.<br>
     * If the given {@link EquipmentSlot} is null, this will remove all
     * {@link AttributeModifier}s that do not have an EquipmentSlot set.
     *
     * @param slot the {@link EquipmentSlot} to clear all Attributes and
     *             their modifiers for
     * @return true if all modifiers were removed that match the given
     *         EquipmentSlot.
     */
    public boolean removeAttributeModifier(@Nullable EquipmentSlot slot) {
        return this.craftDelegate.removeAttributeModifier(slot);
    }

    /**
     * Remove a specific {@link Attribute} and {@link AttributeModifier}.
     * AttributeModifiers are matched according to their {@link java.util.UUID}.
     *
     * @param attribute the {@link Attribute} to remove
     * @param modifier the {@link AttributeModifier} to remove
     * @return if any attribute modifiers were remove
     *
     * @throws NullPointerException if the Attribute is null
     * @throws NullPointerException if the AttributeModifier is null
     *
     * @see AttributeModifier#getUniqueId()
     */
    public boolean removeAttributeModifier(@NotNull Attribute attribute, @NotNull AttributeModifier modifier) {
        return this.craftDelegate.removeAttributeModifier(attribute, modifier);
    }

    /**
     * Checks to see if this item has damage
     *
     * @return true if this has damage
     */
    public boolean hasDamage() {
        return this.craftDelegate.hasDamage();
    }

    /**
     * Gets the damage
     *
     * @return the damage
     */
    public int getDamage() {
        return this.craftDelegate.getDamage();
    }

    /**
     * Sets the damage
     *
     * @param damage item damage
     */
    public void setDamage(int damage) {
        this.craftDelegate.setDamage(damage);
    }

    /**
     * Repairs this item by 1 durability
     */
    public void repair() {
        repair(1);
    }

    /**
     * Damages this item by 1 durability
     *
     * @return True if damage broke the item
     */
    public boolean damage() {
        return damage(1);
    }

    /**
     * Repairs this item's durability by amount
     *
     * @param amount Amount of durability to repair
     */
    public void repair(int amount) {
        damage(-amount);
    }

    /**
     * Damages this item's durability by amount
     *
     * @param amount Amount of durability to damage
     * @return True if damage broke the item
     */
    public boolean damage(int amount) {
        return damage(amount, false);
    }

    /**
     * Damages this item's durability by amount
     *
     * @param amount Amount of durability to damage
     * @param ignoreUnbreaking Ignores unbreaking enchantment
     * @return True if damage broke the item
     */
    public boolean damage(int amount, boolean ignoreUnbreaking) {
        return this.craftDelegate.damage(amount, ignoreUnbreaking);
    }
    // Purpur end
}
