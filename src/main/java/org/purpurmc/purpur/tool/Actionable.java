package org.purpurmc.purpur.tool;

import java.util.Map;
import net.minecraft.world.item.Item;
import net.minecraft.world.level.block.Block;

public abstract class Actionable {
    private final Block into;
    private final Map<Item, Double> drops;

    public Actionable(Block into, Map<Item, Double> drops) {
        this.into = into;
        this.drops = drops;
    }

    public Block into() {
        return into;
    }

    public Map<Item, Double> drops() {
        return drops;
    }
}
