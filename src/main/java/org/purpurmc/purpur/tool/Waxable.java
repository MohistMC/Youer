package org.purpurmc.purpur.tool;

import java.util.Map;
import net.minecraft.world.item.Item;
import net.minecraft.world.level.block.Block;

public class Waxable extends Actionable {
    public Waxable(Block into, Map<Item, Double> drops) {
        super(into, drops);
    }
}
