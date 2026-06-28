package ca.spottedleaf.dataconverter.minecraft.walkers.game_event;

import ca.spottedleaf.dataconverter.converters.datatypes.DataWalker;
import ca.spottedleaf.dataconverter.minecraft.datatypes.MCTypeRegistry;
import ca.spottedleaf.dataconverter.minecraft.walkers.generic.WalkerUtils;
import ca.spottedleaf.dataconverter.types.MapType;

public final class GameEventListenerWalker implements DataWalker<MapType> {

    @Override
    public MapType walk(final MapType data, final long fromVersion, final long toVersion) {
        final MapType listener = data.getMap("listener");
        if (listener == null) {
            return null;
        }

        WalkerUtils.convert(MCTypeRegistry.GAME_EVENT_NAME, listener.getMap("event"), "game_event", fromVersion, toVersion);

        return null;
    }
}
