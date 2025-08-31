package org.purpurmc.purpur.tool;

import java.util.Map;
import net.minecraft.world.item.Item;
import net.minecraft.world.level.block.Block;

public class Flattenable extends Actionable {
    public Flattenable(Block into, Map<Item, Double> drops) {
        super(into, drops);
    }
}
