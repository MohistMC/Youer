package org.bukkit.craftbukkit.tag;

import com.mohistmc.youer.Youer;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;
import net.minecraft.core.Registry;
import net.minecraft.tags.TagKey;
import net.minecraft.world.level.block.Block;
import org.bukkit.Material;
import org.bukkit.craftbukkit.block.CraftBlockType;

public class CraftBlockTag extends CraftTag<Block, Material> {

    public CraftBlockTag(Registry<Block> registry, TagKey<Block> tag) {
        super(registry, tag);
    }

    @Override
    public boolean isTagged(Material item) {
        Block block = CraftBlockType.bukkitToMinecraft(item);

        // SPIGOT-6952: A Material is not necessary a block, in this case return false
        if (block == null) {
            return false;
        }

        return block.builtInRegistryHolder().is(this.tag);
    }

    @Override
    public Set<Material> getValues() {
        return this.getHandle().stream()
                .map(block -> {
                    Material material = CraftBlockType.minecraftToBukkit(block.value());
                    if (material == null) {
                        String blockName = block.value().getDescriptionId();
                        String registryName = block.value().builtInRegistryHolder().key().location().toString();
                        Youer.LOGGER.warn("CraftBlockTag: No Bukkit Material mapping for Minecraft block: {} ({}) in tag {}", blockName, registryName, this.tag.location());
                    }
                    return material;
                })
                .filter(Objects::nonNull)
                .collect(Collectors.toUnmodifiableSet());
    }
}
