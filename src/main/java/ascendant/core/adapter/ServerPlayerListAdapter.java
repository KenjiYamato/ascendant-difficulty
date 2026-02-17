package ascendant.core.adapter;

import ascendant.core.config.DifficultyIO;
import ascendant.core.config.DifficultyManager;
import ascendant.core.util.ReflectionHelper;
import ascendant.core.util.TierTagUtil;
import com.hypixel.hytale.protocol.Packet;
import com.hypixel.hytale.protocol.packets.interface_.AddToServerPlayerList;
import com.hypixel.hytale.protocol.packets.interface_.RemoveFromServerPlayerList;
import com.hypixel.hytale.protocol.packets.interface_.ServerPlayerListPlayer;
import com.hypixel.hytale.server.core.io.adapter.PacketAdapters;
import com.hypixel.hytale.server.core.io.adapter.PacketFilter;
import com.hypixel.hytale.server.core.io.adapter.PlayerPacketWatcher;
import com.hypixel.hytale.server.core.universe.PlayerRef;
import com.hypixel.hytale.server.core.universe.Universe;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.lang.reflect.Method;
import java.util.UUID;

public final class ServerPlayerListAdapter {
    private static volatile PacketFilter _registered;

    private ServerPlayerListAdapter() {
    }

    public static void register() {
        if (_registered != null) {
            return;
        }

        PlayerPacketWatcher watcher = ServerPlayerListAdapter::_handleOutbound;
        _registered = PacketAdapters.registerOutbound(watcher);
    }

    public static void unregister() {
        PacketFilter f = _registered;
        if (f == null) {
            return;
        }
        _registered = null;
        PacketAdapters.deregisterOutbound(f);
    }

    public static void refreshPlayerEntry(@Nullable PlayerRef playerRef) {
        if (playerRef == null) {
            return;
        }

        ServerPlayerListPlayer entry = createServerListEntry(playerRef);
        if (entry == null) {
            return;
        }

        if (DifficultyManager.getFromConfig(DifficultyIO.ALLOW_SERVERLIST_TIER_TAG)) {
            String tagged = buildTaggedUsername(entry.uuid, entry.username);
            if (tagged != null && !tagged.equals(entry.username)) {
                entry.username = tagged;
            }
        }

        Universe universe = Universe.get();
        if (universe == null) {
            return;
        }

        UUID uuid = entry.uuid;
        universe.broadcastPacket(new RemoveFromServerPlayerList(new UUID[]{uuid}));
        universe.broadcastPacket(new AddToServerPlayerList(new ServerPlayerListPlayer[]{entry}));
    }

    private static void _handleOutbound(@Nonnull PlayerRef playerRef, @Nonnull Packet packet) {
        if (!(packet instanceof AddToServerPlayerList add)) {
            return;
        }
        if (!DifficultyManager.getFromConfig(DifficultyIO.ALLOW_SERVERLIST_TIER_TAG)) {
            return;
        }

        ServerPlayerListPlayer[] players = add.players;
        if (players == null) {
            return;
        }

        for (ServerPlayerListPlayer entry : players) {
            if (entry == null) {
                continue;
            }
            String tagged = buildTaggedUsername(entry.uuid, entry.username);
            if (tagged != null && !tagged.equals(entry.username)) {
                entry.username = tagged;
            }
        }
    }

    @Nullable
    private static ServerPlayerListPlayer createServerListEntry(@Nonnull PlayerRef playerRef) {
        Method method = getCreateEntryMethod();
        if (method == null) {
            return null;
        }
        try {
            Object value = method.invoke(null, playerRef);
            if (value instanceof ServerPlayerListPlayer entry) {
                return entry;
            }
        } catch (ReflectiveOperationException ignored) {
        }
        return null;
    }

    @Nullable
    private static Method getCreateEntryMethod() {
        Class<?> moduleClass = ReflectionHelper.resolveClass(
                "com.hypixel.hytale.server.core.modules.serverplayerlist.ServerPlayerListModule");
        if (moduleClass == null) {
            return null;
        }
        return ReflectionHelper.getDeclaredMethod(moduleClass, "createServerPlayerListPlayer", PlayerRef.class);
    }

    private static String buildTaggedUsername(UUID uuid, String username) {
        String prefix = TierTagUtil.resolveTierPrefix(uuid, TierTagUtil.PrefixKind.CHAT);
        if (prefix == null || prefix.isBlank()) {
            return username;
        }

        String safeName = username == null ? "" : username;
        if (safeName.startsWith(prefix)) {
            return safeName;
        }
        return prefix + safeName;
    }
}
