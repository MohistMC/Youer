package org.bukkit.craftbukkit.v1_20_R4.boss;

import net.minecraft.server.bossevents.CustomBossEvent;
import org.bukkit.NamespacedKey;
import org.bukkit.boss.KeyedBossBar;
import org.bukkit.craftbukkit.v1_20_R4.util.CraftNamespacedKey;

public class CraftKeyedBossbar extends CraftBossBar implements KeyedBossBar {

    public CraftKeyedBossbar(CustomBossEvent bossBattleCustom) {
        super(bossBattleCustom);
    }

    @Override
    public NamespacedKey getKey() {
        return CraftNamespacedKey.fromMinecraft(this.getHandle().getTextId());
    }

    @Override
    public CustomBossEvent getHandle() {
        return (CustomBossEvent) super.getHandle();
    }
}
