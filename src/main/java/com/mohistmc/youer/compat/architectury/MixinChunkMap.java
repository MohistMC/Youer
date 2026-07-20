package com.mohistmc.youer.compat.architectury;

import dev.architectury.event.neoforge.EventHandlerImplCommon;
import net.minecraft.world.level.Level;
import net.neoforged.bus.api.Event;
import net.neoforged.neoforge.event.level.ChunkDataEvent;

/**
 * @author Mgazul
 * @date 2026/7/20 20:09
 */
public class MixinChunkMap {

    public static Event modifyProtoChunkLevel(Event event, Level level) {
        if (event instanceof ChunkDataEvent.Load load) {
            ((EventHandlerImplCommon.LevelEventAttachment) load).architectury$attachLevel(level);
        }
        return event;
    }
}
