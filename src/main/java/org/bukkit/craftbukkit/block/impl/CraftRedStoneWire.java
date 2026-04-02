/**
 * Automatically generated file, changes will be lost.
 */
package org.bukkit.craftbukkit.block.impl;

public final class CraftRedStoneWire extends org.bukkit.craftbukkit.block.data.CraftBlockData implements org.bukkit.block.data.type.RedstoneWire, org.bukkit.block.data.AnaloguePowerable {

    public CraftRedStoneWire() {
        super();
    }

    public CraftRedStoneWire(net.minecraft.world.level.block.state.BlockState state) {
        super(state);
    }

    // org.bukkit.craftbukkit.block.data.type.CraftRedstoneWire

    private static final org.bukkit.craftbukkit.block.data.CraftBlockStateEnum<?, org.bukkit.block.data.type.RedstoneWire.Connection> NORTH = getEnum(net.minecraft.world.level.block.RedStoneWireBlock.class, "north", org.bukkit.block.data.type.RedstoneWire.Connection.class);
    private static final org.bukkit.craftbukkit.block.data.CraftBlockStateEnum<?, org.bukkit.block.data.type.RedstoneWire.Connection> EAST = getEnum(net.minecraft.world.level.block.RedStoneWireBlock.class, "east", org.bukkit.block.data.type.RedstoneWire.Connection.class);
    private static final org.bukkit.craftbukkit.block.data.CraftBlockStateEnum<?, org.bukkit.block.data.type.RedstoneWire.Connection> SOUTH = getEnum(net.minecraft.world.level.block.RedStoneWireBlock.class, "south", org.bukkit.block.data.type.RedstoneWire.Connection.class);
    private static final org.bukkit.craftbukkit.block.data.CraftBlockStateEnum<?, org.bukkit.block.data.type.RedstoneWire.Connection> WEST = getEnum(net.minecraft.world.level.block.RedStoneWireBlock.class, "west", org.bukkit.block.data.type.RedstoneWire.Connection.class);

    @Override
    public org.bukkit.block.data.type.RedstoneWire.Connection getFace(org.bukkit.block.BlockFace face) {
        switch (face) {
            case NORTH:
                return get(NORTH);
            case EAST:
                return get(EAST);
            case SOUTH:
                return get(SOUTH);
            case WEST:
                return get(WEST);
            default:
                throw new IllegalArgumentException("Cannot have face " + face);
        }
    }

    @Override
    public void setFace(org.bukkit.block.BlockFace face, org.bukkit.block.data.type.RedstoneWire.Connection connection) {
        switch (face) {
            case NORTH:
                set(NORTH, connection);
                break;
            case EAST:
                set(EAST, connection);
                break;
            case SOUTH:
                set(SOUTH, connection);
                break;
            case WEST:
                set(WEST, connection);
                break;
            default:
                throw new IllegalArgumentException("Cannot have face " + face);
        }
    }

    @Override
    public java.util.Set<org.bukkit.block.BlockFace> getAllowedFaces() {
        return com.google.common.collect.ImmutableSet.of(org.bukkit.block.BlockFace.NORTH, org.bukkit.block.BlockFace.EAST, org.bukkit.block.BlockFace.SOUTH, org.bukkit.block.BlockFace.WEST);
    }

    // org.bukkit.craftbukkit.block.data.CraftAnaloguePowerable

    private static final net.minecraft.world.level.block.state.properties.IntegerProperty POWER = getInteger(net.minecraft.world.level.block.RedStoneWireBlock.class, "power");

    @Override
    public int getPower() {
        return get(POWER);
    }

    @Override
    public void setPower(int power) {
        set(POWER, power);
    }

    @Override
    public int getMaximumPower() {
        return getMax(POWER);
    }
}
