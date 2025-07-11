package org.bukkit.craftbukkit.entity;

import com.google.common.base.Preconditions;
import com.mohistmc.dynamicenum.MohistDynamEnum;
import com.mohistmc.youer.Youer;
import net.minecraft.world.entity.monster.SpellcasterIllager;
import org.bukkit.craftbukkit.CraftServer;
import org.bukkit.entity.Spellcaster;

public class CraftSpellcaster extends CraftIllager implements Spellcaster {

    public CraftSpellcaster(CraftServer server, SpellcasterIllager entity) {
        super(server, entity);
    }

    @Override
    public SpellcasterIllager getHandle() {
        return (SpellcasterIllager) super.getHandle();
    }

    @Override
    public String toString() {
        return "CraftSpellcaster";
    }

    @Override
    public Spell getSpell() {
        return CraftSpellcaster.toBukkitSpell(this.getHandle().getCurrentSpell());
    }

    @Override
    public void setSpell(Spell spell) {
        Preconditions.checkArgument(spell != null, "Use Spell.NONE");

        this.getHandle().setIsCastingSpell(CraftSpellcaster.toNMSSpell(spell));
    }

    public static Spell toBukkitSpell(SpellcasterIllager.IllagerSpell spell) {
        try {
            return Spellcaster.Spell.valueOf(spell.name());
        } catch (IllegalArgumentException e) {
            int forgeCount = SpellcasterIllager.IllagerSpell.values().length;
            for (var id = Spellcaster.Spell.values().length; id < forgeCount; id++) {
                String name = SpellcasterIllager.IllagerSpell.values()[id].name();
                Spell newPhase = MohistDynamEnum.addEnum(Spellcaster.Spell.class, name);
                Youer.LOGGER.debug("Save-IllagerSpell:{} - {}", name, newPhase);
            }
            return toBukkitSpell(spell);
        }
    }

    public static SpellcasterIllager.IllagerSpell toNMSSpell(Spell spell) {
        return SpellcasterIllager.IllagerSpell.byId(spell.ordinal());
    }
}
