/*
 * Mohist - MohistMC
 * Copyright (C) 2018-2024.
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program. If not, see <https://www.gnu.org/licenses/>.
 */

package cn.mohistmc.youer.eventhandler;

import cn.mohistmc.youer.Youer;
import cn.mohistmc.youer.eventhandler.dispatcher.BlockEventDispatcher;
import cn.mohistmc.youer.eventhandler.dispatcher.EntityEventDispatcher;
import cn.mohistmc.youer.eventhandler.dispatcher.ItemEventDispatcher;
import cn.mohistmc.youer.eventhandler.dispatcher.PlayerEventDispatcher;
import cn.mohistmc.youer.eventhandler.dispatcher.WorldEventDispatcher;
import net.neoforged.neoforge.common.NeoForge;

public class EventDispatcherRegistry {

    public static void init() {
        NeoForge.EVENT_BUS.register(new PlayerEventDispatcher());
        NeoForge.EVENT_BUS.register(new WorldEventDispatcher());
        NeoForge.EVENT_BUS.register(new ItemEventDispatcher());
        NeoForge.EVENT_BUS.register(new EntityEventDispatcher());
        NeoForge.EVENT_BUS.register(new BlockEventDispatcher());
        Youer.LOGGER.info("EventDispatcherRegistry initialized");
    }
}
