package org.bukkit.inventory;

import java.util.function.Supplier;
import org.jetbrains.annotations.ApiStatus;
import org.jetbrains.annotations.NotNull;

public enum EquipmentSlot {

    HAND(() -> EquipmentSlotGroup.MAINHAND),
    OFF_HAND(() -> EquipmentSlotGroup.OFFHAND),
    FEET(() -> EquipmentSlotGroup.FEET),
    LEGS(() -> EquipmentSlotGroup.LEGS),
    CHEST(() -> EquipmentSlotGroup.CHEST),
    HEAD(() -> EquipmentSlotGroup.HEAD),
    /**
     * Only for certain entities such as horses and wolves.
     */
    BODY(() -> EquipmentSlotGroup.ARMOR);

    private Supplier<EquipmentSlotGroup> group; // Supplier because of class loading order, since EquipmentSlot and EquipmentSlotGroup reference each other on class init

    public EquipmentSlotGroup group1;

    private EquipmentSlot(/*@NotNull*/ Supplier<EquipmentSlotGroup> group) {
        this.group = group;
    }

    private EquipmentSlot() {
    }

    /**
     * Gets the {@link EquipmentSlotGroup} corresponding to this slot.
     *
     * @return corresponding {@link EquipmentSlotGroup}
     */
    @NotNull
    @ApiStatus.Internal
    public EquipmentSlotGroup getGroup() {
        if (group1 != null) {
            return group1;
        }
        return group.get();
    }
}
