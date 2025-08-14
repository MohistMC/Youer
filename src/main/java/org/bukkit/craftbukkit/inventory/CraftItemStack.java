package org.bukkit.craftbukkit.inventory;

import com.google.common.base.Preconditions;
import com.google.common.collect.ImmutableMap;
import java.util.Map;
import net.minecraft.core.Holder;
import net.minecraft.core.component.DataComponentPatch;
import net.minecraft.core.component.DataComponents;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.enchantment.EnchantmentHelper;
import net.minecraft.world.item.enchantment.ItemEnchantments;
import org.bukkit.Material;
import org.bukkit.configuration.serialization.DelegateDeserialization;
import org.bukkit.craftbukkit.enchantments.CraftEnchantment;
import org.bukkit.craftbukkit.entity.CraftLivingEntity;
import org.bukkit.craftbukkit.persistence.CraftPersistentDataTypeRegistry;
import org.bukkit.craftbukkit.util.CraftMagicNumbers;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.entity.LivingEntity;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.material.MaterialData;
import org.jetbrains.annotations.ApiStatus;

@DelegateDeserialization(ItemStack.class)
public final class CraftItemStack extends ItemStack {

    // Paper start - delegate api-ItemStack to CraftItemStack
    private static final java.lang.invoke.VarHandle API_ITEM_STACK_CRAFT_DELEGATE_FIELD;
    static {
        try {
            API_ITEM_STACK_CRAFT_DELEGATE_FIELD = java.lang.invoke.MethodHandles.privateLookupIn(
                    ItemStack.class,
                    java.lang.invoke.MethodHandles.lookup()
            ).findVarHandle(ItemStack.class, "craftDelegate", ItemStack.class);
        } catch (final IllegalAccessException | NoSuchFieldException exception) {
            throw new RuntimeException(exception);
        }
    }

    private static CraftItemStack getCraftStack(final ItemStack bukkit) {
        if (bukkit instanceof final CraftItemStack craftItemStack) {
            return craftItemStack;
        } else {
            return  (CraftItemStack) API_ITEM_STACK_CRAFT_DELEGATE_FIELD.get(bukkit);
        }
    }

    @Override
    public int hashCode() {
        if (this.handle == null || this.handle.isEmpty()) {
            return net.minecraft.world.item.ItemStack.EMPTY.hashCode();
        } else {
            int hash = net.minecraft.world.item.ItemStack.hashItemAndComponents(this.handle);
            hash = hash * 31 + this.handle.getCount();
            return hash;
        }
    }

    @Override
    public boolean equals(final Object obj) {
        if (!(obj instanceof final org.bukkit.inventory.ItemStack bukkit)) return false;
        final CraftItemStack craftStack = getCraftStack(bukkit);
        if (this.handle == craftStack.handle) return true;
        else if (this.handle == null || craftStack.handle == null) return false;
        else if (this.handle.isEmpty() && craftStack.handle.isEmpty()) return true;
        else return net.minecraft.world.item.ItemStack.matches(this.handle, craftStack.handle);
    }
    // Paper end

    // Paper start - MC Utils
    public static net.minecraft.world.item.ItemStack unwrap(ItemStack bukkit) {
        // Paper start - re-implement after delegating all api ItemStack calls to CraftItemStack
        final CraftItemStack craftItemStack = getCraftStack(bukkit);
        return craftItemStack.handle == null ? net.minecraft.world.item.ItemStack.EMPTY : craftItemStack.handle;
        // Paper end - re-implement after delegating all api ItemStack calls to CraftItemStack
    }

    public static net.minecraft.world.item.ItemStack getOrCloneOnMutation(ItemStack old, ItemStack newInstance) {
        return old == newInstance ? unwrap(old) : asNMSCopy(newInstance);
    }
    // Paper end - MC Utils

    // Paper start - override isEmpty to use vanilla's impl
    @Override
    public boolean isEmpty() {
        return handle == null || handle.isEmpty();
    }
    // Paper end - override isEmpty to use vanilla's impl

    public static net.minecraft.world.item.ItemStack asNMSCopy(ItemStack original) {
        // Paper start - re-implement after delegating all api ItemStack calls to CraftItemStack
        if (original == null || original.isEmpty()) {
            return net.minecraft.world.item.ItemStack.EMPTY;
        }
        final CraftItemStack stack = getCraftStack(original);
        return stack.handle == null ? net.minecraft.world.item.ItemStack.EMPTY : stack.handle.copy();
        // Paper end - re-implement after delegating all api ItemStack calls to CraftItemStack
    }

    // Paper start
    public static java.util.List<net.minecraft.world.item.ItemStack> asNMSCopy(java.util.List<? extends ItemStack> originals) {
        final java.util.List<net.minecraft.world.item.ItemStack> items = new java.util.ArrayList<>(originals.size());
        for (final ItemStack original : originals) {
            items.add(asNMSCopy(original));
        }
        return items;
    }
    // Paper end

    public static net.minecraft.world.item.ItemStack copyNMSStack(net.minecraft.world.item.ItemStack original, int amount) {
        net.minecraft.world.item.ItemStack stack = original.copy();
        stack.setCount(amount);
        return stack;
    }

    /**
     * Copies the NMS stack to return as a strictly-Bukkit stack
     */
    public static ItemStack asBukkitCopy(net.minecraft.world.item.ItemStack original) {
        // Paper start - no such thing as a "strictly-Bukkit stack" anymore
        // we copy the stack since it should be a complete copy not a mirror
        return asCraftMirror(original.copy());
        // Paper end
    }

    public static CraftItemStack asCraftMirror(net.minecraft.world.item.ItemStack original) {
        return new CraftItemStack((original == null || original.isEmpty()) ? null : original);
    }

    public static CraftItemStack asCraftCopy(ItemStack original) {
        if (original instanceof CraftItemStack) {
            CraftItemStack stack = (CraftItemStack) original;
            return new CraftItemStack(stack.handle == null ? null : stack.handle.copy());
        }
        return new CraftItemStack(original);
    }

    public static CraftItemStack asNewCraftStack(Item item) {
        return CraftItemStack.asNewCraftStack(item, 1);
    }

    public static CraftItemStack asNewCraftStack(Item item, int amount) {
        return new CraftItemStack(CraftItemType.minecraftToBukkit(item), amount, (short) 0, null);
    }

    public net.minecraft.world.item.ItemStack handle;
    private boolean isForInventoryDrop;

    /**
     * Mirror
     */
    private CraftItemStack(net.minecraft.world.item.ItemStack item) {
        this.handle = item;
    }

    private CraftItemStack(ItemStack item) {
        this(item.getType(), item.getAmount(), item.getDurability(), item.hasItemMeta() ? item.getItemMeta() : null);
    }

    private CraftItemStack(Material type, int amount, short durability, ItemMeta itemMeta) {
        this.setType(type);
        this.setAmount(amount);
        this.setDurability(durability);
        this.setItemMeta(itemMeta);
    }

    /**
     * Gets if the item is marked as an inventory drop in death events.
     *
     * @return true if the item is marked as an inventory drop
     */
    @ApiStatus.Internal
    public boolean isForInventoryDrop() {
        return this.isForInventoryDrop;
    }

    /**
     * Marks this item as an inventory drop in death events.
     *
     * @return the ItemStack marked as an inventory drop
     */
    @ApiStatus.Internal
    public ItemStack markForInventoryDrop() {
        this.isForInventoryDrop = true;
        return this;
    }

    @Override
    public MaterialData getData() {
        return this.handle != null ? CraftMagicNumbers.getMaterialData(this.handle.getItem()) : super.getData();
    }

    @Override
    public Material getType() {
        return this.handle != null ? CraftItemType.minecraftToBukkit(this.handle.getItem()) : Material.AIR;
    }

    @Override
    public void setType(Material type) {
        if (this.getType() == type) {
            return;
        } else if (type == Material.AIR) {
            this.handle = null;
        } else if (CraftItemType.bukkitToMinecraft(type) == null) { // :(
            this.handle = null;
        } else if (this.handle == null) {
            this.handle = new net.minecraft.world.item.ItemStack(CraftItemType.bukkitToMinecraft(type), 1);
        } else {
            final Material oldType = CraftMagicNumbers.getMaterial(this.handle.getItem()); // Paper
            this.handle.setItem(CraftItemType.bukkitToMinecraft(type));
            if (this.hasItemMeta()) {
                // This will create the appropriate item meta, which will contain all the data we intend to keep
                this.adjustTagForItemMeta(oldType); // Paper
            }
        }
        this.setData(null);
    }

    @Override
    public int getAmount() {
        return this.handle != null ? this.handle.getCount() : 0;
    }

    @Override
    public void setAmount(int amount) {
        if (this.handle == null) {
            return;
        }

        this.handle.setCount(amount);
        if (false && amount == 0) { // Paper - remove CraftItemStack#setAmount null assignment
            this.handle = null;
        }
    }

    @Override
    public void setDurability(final short durability) {
        // Ignore damage if item is null
        if (this.handle != null) {
            this.handle.setDamageValue(durability);
        }
    }

    @Override
    public short getDurability() {
        if (this.handle != null) {
            return (short) this.handle.getDamageValue();
        } else {
            return -1;
        }
    }

    @Override
    public int getMaxStackSize() {
        return (this.handle == null) ? Material.AIR.getMaxStackSize() : this.handle.getMaxStackSize();
    }

    // Paper start
    @Override
    public int getMaxItemUseDuration(final LivingEntity entity) {
        if (handle == null) {
            return 0;
        }

        // Make sure plugins calling the old method don't blow up
        if (entity == null && handle.is(Items.CROSSBOW)) {
            throw new UnsupportedOperationException("This item requires an entity to determine the max use duration");
        }
        return handle.getUseDuration(entity != null ? ((CraftLivingEntity) entity).getHandle() : null);
    }
    // Paper end

    @Override
    public void addUnsafeEnchantment(Enchantment ench, int level) {
        Preconditions.checkArgument(ench != null, "Enchantment cannot be null");

        if (!CraftItemStack.makeTag(this.handle)) {
            return;
        }
        ItemEnchantments list = CraftItemStack.getEnchantmentList(this.handle);
        if (list == null) {
            list = ItemEnchantments.EMPTY;
        }
        ItemEnchantments.Mutable listCopy = new ItemEnchantments.Mutable(list);
        listCopy.set(CraftEnchantment.bukkitToMinecraftHolder(ench), level);
        this.handle.set(DataComponents.ENCHANTMENTS, listCopy.toImmutable());
    }

    static boolean makeTag(net.minecraft.world.item.ItemStack item) {
        if (item == null) {
            return false;
        }

        return true;
    }

    @Override
    public boolean containsEnchantment(Enchantment ench) {
        return this.getEnchantmentLevel(ench) > 0;
    }

    @Override
    public int getEnchantmentLevel(Enchantment ench) {
        Preconditions.checkArgument(ench != null, "Enchantment cannot be null");
        if (this.handle == null) {
            return 0;
        }
        return EnchantmentHelper.getItemEnchantmentLevel(CraftEnchantment.bukkitToMinecraftHolder(ench), this.handle);
    }

    @Override
    public int removeEnchantment(Enchantment ench) {
        Preconditions.checkArgument(ench != null, "Enchantment cannot be null");

        ItemEnchantments list = CraftItemStack.getEnchantmentList(this.handle);
        if (list == null) {
            return 0;
        }
        int level = this.getEnchantmentLevel(ench);
        if (level <= 0) {
            return 0;
        }
        int size = list.size();

        if (size == 1) {
            this.handle.remove(DataComponents.ENCHANTMENTS);
            return level;
        }

        ItemEnchantments.Mutable listCopy = new ItemEnchantments.Mutable(list);
        listCopy.set(CraftEnchantment.bukkitToMinecraftHolder(ench), -1); // Negative to remove
        this.handle.set(DataComponents.ENCHANTMENTS, listCopy.toImmutable());

        return level;
    }

    @Override
    public void removeEnchantments() {
        if (this.handle != null) { // Paper - fix NPE
            this.handle.set(DataComponents.ENCHANTMENTS, ItemEnchantments.EMPTY); // Paper - set to default instead of removing the component
        } // Paper
    }

    @Override
    public Map<Enchantment, Integer> getEnchantments() {
        return CraftItemStack.getEnchantments(this.handle);
    }

    static Map<Enchantment, Integer> getEnchantments(net.minecraft.world.item.ItemStack item) {
        ItemEnchantments list = (item != null && item.isEnchanted()) ? item.get(DataComponents.ENCHANTMENTS) : null;

        if (list == null || list.size() == 0) {
            return ImmutableMap.of();
        }

        ImmutableMap.Builder<Enchantment, Integer> result = ImmutableMap.builder();

        list.entrySet().forEach((entry) -> {
            Holder<net.minecraft.world.item.enchantment.Enchantment> id = entry.getKey();
            int level = entry.getIntValue();

            Enchantment enchant = CraftEnchantment.minecraftHolderToBukkit(id);
            if (enchant != null) {
                result.put(enchant, level);
            }
        });

        return result.build();
    }

    static ItemEnchantments getEnchantmentList(net.minecraft.world.item.ItemStack item) {
        return (item != null && item.isEnchanted()) ? item.get(DataComponents.ENCHANTMENTS) : null;
    }

    @Override
    public CraftItemStack clone() {
        return new CraftItemStack(this.handle != null ? this.handle.copy() : null); // Paper
    }

    @Override
    public ItemMeta getItemMeta() {
        return CraftItemStack.getItemMeta(this.handle);
    }

    // Paper start - improve handled tags on type change
    public void adjustTagForItemMeta(final Material oldType) {
        final CraftMetaItem oldMeta = (CraftMetaItem) CraftItemFactory.instance().getItemMeta(oldType);
        final ItemMeta newMeta;
        if (oldMeta == null) {
            newMeta = getItemMeta(this.handle);
        } else {
            final java.util.Set<net.minecraft.core.component.DataComponentType<?>> extraHandledDcts = new java.util.HashSet<>(CraftMetaItem.getTopLevelHandledDcts(oldMeta.getClass()));
            newMeta = getItemMeta(this.handle, CraftItemType.minecraftToBukkitNew(this.handle.getItem()), extraHandledDcts);
        }
        this.setItemMeta(newMeta);
    }
    // Paper end - improve handled tags on type change

    public static void applyMetaToItem(net.minecraft.world.item.ItemStack itemStack, ItemMeta itemMeta) {
        // Paper start - support updating profile after resolving it
        final CraftMetaItem.Applicator tag = new CraftMetaItem.Applicator() {
            @Override
            void skullCallback(final net.minecraft.world.item.component.ResolvableProfile profile) {
                itemStack.set(DataComponents.PROFILE, profile);
            }
        };
        // Paper end - support updating profile after resolving it
        ((CraftMetaItem) itemMeta).applyToItem(tag);
        itemStack.applyComponents(tag.build());
    }

    // Paper start
    public static ItemMeta getItemMeta(net.minecraft.world.item.ItemStack item) {
        return getItemMeta(item, null);
    }
    public static ItemMeta getItemMeta(net.minecraft.world.item.ItemStack item, org.bukkit.inventory.ItemType metaForType) {
        // Paper end
        // Paper start - handled tags on type change
        return getItemMeta(item, metaForType, null);
    }
    public static ItemMeta getItemMeta(net.minecraft.world.item.ItemStack item, org.bukkit.inventory.ItemType metaForType, final java.util.Set<net.minecraft.core.component.DataComponentType<?>> extraHandledDcts) {
        // Paper end - handled tags on type change
        if (!CraftItemStack.hasItemMeta(item)) {
            return CraftItemFactory.instance().getItemMeta(CraftItemStack.getType(item));
        }

        if (metaForType != null) { return ((CraftItemType<?>) metaForType).getItemMeta(item, extraHandledDcts); } // Paper
        return ((CraftItemType<?>) CraftItemType.minecraftToBukkitNew(item.getItem())).getItemMeta(item, extraHandledDcts); // Paper
    }

    static Material getType(net.minecraft.world.item.ItemStack item) {
        return item == null ? Material.AIR : CraftItemType.minecraftToBukkit(item.getItem());
    }

    @Override
    public boolean setItemMeta(ItemMeta itemMeta) {
        return CraftItemStack.setItemMeta(this.handle, itemMeta);
    }

    public static boolean setItemMeta(net.minecraft.world.item.ItemStack item, ItemMeta itemMeta) {
        if (item == null) {
            return false;
        }
        if (CraftItemFactory.instance().equals(itemMeta, null)) {
            item.restorePatch(DataComponentPatch.EMPTY);
            return true;
        }
        if (!CraftItemFactory.instance().isApplicable(itemMeta, CraftItemStack.getType(item))) {
            return false;
        }

        itemMeta = CraftItemFactory.instance().asMetaFor(itemMeta, CraftItemStack.getType(item));
        if (itemMeta == null) return true;

        if (!((CraftMetaItem) itemMeta).isEmpty()) {
            // Paper start - support updating profile after resolving it
            CraftMetaItem.Applicator tag = new CraftMetaItem.Applicator() {
                @Override
                void skullCallback(final net.minecraft.world.item.component.ResolvableProfile resolvableProfile) {
                    item.set(DataComponents.PROFILE, resolvableProfile);
                }
            };
            // Paper end - support updating profile after resolving it

            ((CraftMetaItem) itemMeta).applyToItem(tag);
            item.restorePatch(DataComponentPatch.EMPTY); // Paper - properly apply the new patch from itemmeta
            item.applyComponents(tag.build()); // Paper - properly apply the new patch from itemmeta
        }
        // Paper - this is no longer needed

        return true;
    }

    @Override
    public boolean isSimilar(ItemStack stack) {
        if (stack == null) {
            return false;
        }
        if (stack == this) {
            return true;
        }
        final CraftItemStack that = getCraftStack(stack); // Paper - re-implement after delegating all api ItemStack calls to CraftItemStack
        if (this.handle == that.handle) {
            return true;
        }
        if (this.handle == null || that.handle == null) {
            return false;
        }
        return net.minecraft.world.item.ItemStack.isSameItemSameComponents(this.handle, that.handle); // Paper - re-implement after delegating all api ItemStack calls to CraftItemStack
    }

    @Override
    public boolean hasItemMeta() {
        return CraftItemStack.hasItemMeta(this.handle) && !CraftItemFactory.instance().equals(this.getItemMeta(), null);
    }

    static boolean hasItemMeta(net.minecraft.world.item.ItemStack item) {
        return !(item == null || item.getComponentsPatch().isEmpty());
    }

    // Paper start - with type
    @Override
    public ItemStack withType(final Material type) {
        if (type == Material.AIR) {
            return CraftItemStack.asCraftMirror(null);
        }

        final net.minecraft.world.item.ItemStack copy = new net.minecraft.world.item.ItemStack(
                CraftItemType.bukkitToMinecraft(type), this.getAmount()
        );

        if (this.handle != null) {
            copy.applyComponents(this.handle.getComponentsPatch());
        }

        final CraftItemStack mirrored = CraftItemStack.asCraftMirror(copy);
        mirrored.setItemMeta(mirrored.getItemMeta());
        return mirrored;
    }
    // Paper end

    // Paper start - pdc
    private net.minecraft.nbt.CompoundTag getPdcTag() {
        if (this.handle == null) {
            return new net.minecraft.nbt.CompoundTag();
        }
        final net.minecraft.world.item.component.CustomData customData = this.handle.getOrDefault(DataComponents.CUSTOM_DATA, net.minecraft.world.item.component.CustomData.EMPTY);
        // getUnsafe is OK here because we are only ever *reading* the data so immutability is preserved
        //noinspection deprecation
        return customData.getUnsafe().getCompound("PublicBukkitValues");
    }

    private static final CraftPersistentDataTypeRegistry REGISTRY = new CraftPersistentDataTypeRegistry();
    private final io.papermc.paper.persistence.PaperPersistentDataContainerView pdcView = new io.papermc.paper.persistence.PaperPersistentDataContainerView(REGISTRY) {

        @Override
        public net.minecraft.nbt.CompoundTag toTagCompound() {
            return CraftItemStack.this.getPdcTag();
        }

        @Override
        public net.minecraft.nbt.Tag getTag(final String key) {
            return CraftItemStack.this.getPdcTag().get(key);
        }
    };
    @Override
    public io.papermc.paper.persistence.PersistentDataContainerView getPersistentDataContainer() {
        return this.pdcView;
    }
    // Paper end - pdc

    // Purpur start
    @Override
    public String getDisplayName() {
        return getItemMeta().getDisplayName();
    }

    @Override
    public void setDisplayName(String name) {
        ItemMeta itemMeta = getItemMeta();
        itemMeta.setDisplayName(name);
        setItemMeta(itemMeta);
    }

    @Override
    public boolean hasDisplayName() {
        return hasItemMeta() && getItemMeta().hasDisplayName();
    }

    @Override
    public String getLocalizedName() {
        return getItemMeta().getLocalizedName();
    }

    @Override
    public void setLocalizedName(String name) {
        ItemMeta itemMeta = getItemMeta();
        itemMeta.setLocalizedName(name);
        setItemMeta(itemMeta);
    }

    @Override
    public boolean hasLocalizedName() {
        return hasItemMeta() && getItemMeta().hasLocalizedName();
    }

    @Override
    public boolean hasLore() {
        return hasItemMeta() && getItemMeta().hasLore();
    }

    @Override
    public boolean hasEnchant(Enchantment ench) {
        return hasItemMeta() && getItemMeta().hasEnchant(ench);
    }

    @Override
    public int getEnchantLevel(Enchantment ench) {
        return getItemMeta().getEnchantLevel(ench);
    }

    @Override
    public Map<Enchantment, Integer> getEnchants() {
        return getItemMeta().getEnchants();
    }

    @Override
    public boolean addEnchant(Enchantment ench, int level, boolean ignoreLevelRestriction) {
        ItemMeta itemMeta = getItemMeta();
        boolean result = itemMeta.addEnchant(ench, level, ignoreLevelRestriction);
        setItemMeta(itemMeta);
        return result;
    }

    @Override
    public boolean removeEnchant(Enchantment ench) {
        ItemMeta itemMeta = getItemMeta();
        boolean result = itemMeta.removeEnchant(ench);
        setItemMeta(itemMeta);
        return result;
    }

    @Override
    public boolean hasEnchants() {
        return hasItemMeta() && getItemMeta().hasEnchants();
    }

    @Override
    public boolean hasConflictingEnchant(Enchantment ench) {
        return hasItemMeta() && getItemMeta().hasConflictingEnchant(ench);
    }

    @Override
    public void setCustomModelData(Integer data) {
        ItemMeta itemMeta = getItemMeta();
        itemMeta.setCustomModelData(data);
        setItemMeta(itemMeta);
    }

    @Override
    public int getCustomModelData() {
        return getItemMeta().getCustomModelData();
    }

    @Override
    public boolean hasCustomModelData() {
        return hasItemMeta() && getItemMeta().hasCustomModelData();
    }

    @Override
    public boolean hasBlockData() {
        return hasItemMeta() && ((org.bukkit.inventory.meta.BlockDataMeta) getItemMeta()).hasBlockData();
    }

    @Override
    public org.bukkit.block.data.BlockData getBlockData(Material material) {
        return ((org.bukkit.inventory.meta.BlockDataMeta) getItemMeta()).getBlockData(material);
    }

    @Override
    public void setBlockData(org.bukkit.block.data.BlockData blockData) {
        ItemMeta itemMeta = getItemMeta();
        ((org.bukkit.inventory.meta.BlockDataMeta) itemMeta).setBlockData(blockData);
        setItemMeta(itemMeta);
    }

    @Override
    public int getRepairCost() {
        return ((org.bukkit.inventory.meta.Repairable) getItemMeta()).getRepairCost();
    }

    @Override
    public void setRepairCost(int cost) {
        ItemMeta itemMeta = getItemMeta();
        ((org.bukkit.inventory.meta.Repairable) itemMeta).setRepairCost(cost);
        setItemMeta(itemMeta);
    }

    @Override
    public boolean hasRepairCost() {
        return hasItemMeta() && ((org.bukkit.inventory.meta.Repairable) getItemMeta()).hasRepairCost();
    }

    @Override
    public boolean isUnbreakable() {
        return hasItemMeta() && getItemMeta().isUnbreakable();
    }

    @Override
    public void setUnbreakable(boolean unbreakable) {
        ItemMeta itemMeta = getItemMeta();
        itemMeta.setUnbreakable(unbreakable);
        setItemMeta(itemMeta);
    }

    @Override
    public boolean hasAttributeModifiers() {
        return hasItemMeta() && getItemMeta().hasAttributeModifiers();
    }

    @Override
    public com.google.common.collect.Multimap<org.bukkit.attribute.Attribute, org.bukkit.attribute.AttributeModifier> getAttributeModifiers() {
        return getItemMeta().getAttributeModifiers();
    }

    @Override
    public com.google.common.collect.Multimap<org.bukkit.attribute.Attribute, org.bukkit.attribute.AttributeModifier> getAttributeModifiers(org.bukkit.inventory.EquipmentSlot slot) {
        return getItemMeta().getAttributeModifiers(slot);
    }

    @Override
    public java.util.Collection<org.bukkit.attribute.AttributeModifier> getAttributeModifiers(org.bukkit.attribute.Attribute attribute) {
        return getItemMeta().getAttributeModifiers(attribute);
    }

    @Override
    public boolean addAttributeModifier(org.bukkit.attribute.Attribute attribute, org.bukkit.attribute.AttributeModifier modifier) {
        ItemMeta itemMeta = getItemMeta();
        boolean result = itemMeta.addAttributeModifier(attribute, modifier);
        setItemMeta(itemMeta);
        return result;
    }

    @Override
    public void setAttributeModifiers(com.google.common.collect.Multimap<org.bukkit.attribute.Attribute, org.bukkit.attribute.AttributeModifier> attributeModifiers) {
        ItemMeta itemMeta = getItemMeta();
        itemMeta.setAttributeModifiers(attributeModifiers);
        setItemMeta(itemMeta);
    }

    @Override
    public boolean removeAttributeModifier(org.bukkit.attribute.Attribute attribute) {
        ItemMeta itemMeta = getItemMeta();
        boolean result = itemMeta.removeAttributeModifier(attribute);
        setItemMeta(itemMeta);
        return result;
    }

    @Override
    public boolean removeAttributeModifier(org.bukkit.inventory.EquipmentSlot slot) {
        ItemMeta itemMeta = getItemMeta();
        boolean result = itemMeta.removeAttributeModifier(slot);
        setItemMeta(itemMeta);
        return result;
    }

    @Override
    public boolean removeAttributeModifier(org.bukkit.attribute.Attribute attribute, org.bukkit.attribute.AttributeModifier modifier) {
        ItemMeta itemMeta = getItemMeta();
        boolean result = itemMeta.removeAttributeModifier(attribute, modifier);
        setItemMeta(itemMeta);
        return result;
    }

    @Override
    public boolean hasDamage() {
        return hasItemMeta() && ((org.bukkit.inventory.meta.Damageable) getItemMeta()).hasDamage();
    }

    @Override
    public int getDamage() {
        return ((org.bukkit.inventory.meta.Damageable) getItemMeta()).getDamage();
    }

    @Override
    public void setDamage(int damage) {
        ItemMeta itemMeta = getItemMeta();
        ((org.bukkit.inventory.meta.Damageable) itemMeta).setDamage(damage);
        setItemMeta(itemMeta);
    }

    @Override
    public void repair() {
        repair(1);
    }

    @Override
    public boolean damage() {
        return damage(1);
    }

    @Override
    public void repair(int amount) {
        damage(-amount);
    }

    @Override
    public boolean damage(int amount) {
        return damage(amount, false);
    }

    @Override
    public boolean damage(int amount, boolean ignoreUnbreaking) {
        org.bukkit.inventory.meta.Damageable damageable = (org.bukkit.inventory.meta.Damageable) getItemMeta();
        if (amount > 0) {
            int unbreaking = getEnchantLevel(Enchantment.UNBREAKING);
            int reduce = 0;
            for (int i = 0; unbreaking > 0 && i < amount; ++i) {
                if (reduceDamage(java.util.concurrent.ThreadLocalRandom.current(), unbreaking)) {
                    ++reduce;
                }
            }
            amount -= reduce;
            if (amount <= 0) {
                return isBroke(damageable.getDamage());
            }
        }
        int damage = damageable.getDamage() + amount;
        damageable.setDamage(damage);
        setItemMeta((ItemMeta) damageable);
        return isBroke(damage);
    }

    private boolean isBroke(int damage) {
        if (damage > getType().getMaxDurability()) {
            if (getAmount() > 0) {
                // ensure it "breaks"
                setAmount(0);
            }
            return true;
        }
        return false;
    }

    private boolean reduceDamage(java.util.Random random, int unbreaking) {
        if (getType().isArmor()) {
            return random.nextFloat() < 0.6F;
        }
        return random.nextInt(unbreaking + 1) > 0;
    }
    // Purpur end
}
