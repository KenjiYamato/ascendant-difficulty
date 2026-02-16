package ascendant.core.adapter;

import ascendant.core.config.DifficultyIO;
import ascendant.core.config.DifficultyManager;
import ascendant.core.util.FormattedMessageInspector;
import ascendant.core.util.Logging;
import com.hypixel.hytale.protocol.Packet;
import com.hypixel.hytale.protocol.packets.interface_.KillFeedMessage;
import com.hypixel.hytale.server.core.io.adapter.PacketAdapters;
import com.hypixel.hytale.server.core.io.adapter.PacketFilter;
import com.hypixel.hytale.server.core.io.adapter.PlayerPacketWatcher;
import com.hypixel.hytale.server.core.universe.PlayerRef;

import javax.annotation.Nonnull;

public final class KillFeedAdapter {

    private static volatile PacketFilter _registered;
    private static boolean _allowDebugLogging;

    private KillFeedAdapter() {
    }

    public static void register() {
        if (_registered != null) {
            return;
        }

        PlayerPacketWatcher watcher = KillFeedAdapter::_handleOutbound;
        _registered = PacketAdapters.registerOutbound(watcher);
        _allowDebugLogging = DifficultyManager.getFromConfig(DifficultyIO.ALLOW_DEBUG_LOGGING);
    }

    public static void unregister() {
        PacketFilter f = _registered;
        if (f == null) {
            return;
        }
        _registered = null;
        PacketAdapters.deregisterOutbound(f);
    }

    private static void _handleOutbound(@Nonnull PlayerRef playerRef, @Nonnull Packet packet) {
        if (!(packet instanceof KillFeedMessage kf)) {
            return;
        }

        if (!_allowDebugLogging) {
            return;
        }

        String killerDebug = FormattedMessageInspector.toDebugString(kf.killer);
        String decedentDebug = FormattedMessageInspector.toDebugString(kf.decedent);

        Logging.debug("[KILLFEED LOGGER] killer " + killerDebug);
        Logging.debug("[KILLFEED LOGGER] decedent " + decedentDebug);
        Logging.debug("[KILLFEED LOGGER] icon " + (kf.icon == null ? "null" : kf.icon));
    }
}
