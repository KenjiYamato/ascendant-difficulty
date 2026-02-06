package ascendant.core.adapter;

import ascendant.core.config.DifficultyIO;
import ascendant.core.config.DifficultyManager;
import com.hypixel.hytale.protocol.Packet;
import com.hypixel.hytale.protocol.packets.player.DamageInfo;
import com.hypixel.hytale.server.core.io.adapter.PacketAdapters;
import com.hypixel.hytale.server.core.io.adapter.PacketFilter;
import com.hypixel.hytale.server.core.io.adapter.PlayerPacketWatcher;
import com.hypixel.hytale.server.core.universe.PlayerRef;

import javax.annotation.Nonnull;

@Deprecated
public final class CosmeticDamageNumbersAdapter {

    private static volatile PacketFilter _registered;

    private CosmeticDamageNumbersAdapter() {
    }

    public static void register() {
        if (_registered != null) {
            return;
        }

        PlayerPacketWatcher watcher = CosmeticDamageNumbersAdapter::_handleOutbound;
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

    private static void _handleOutbound(@Nonnull PlayerRef playerRef, @Nonnull Packet packet) {
        if (!(packet instanceof DamageInfo info)) {
            return;
        }

        String tierId = DifficultyManager.getDifficulty(playerRef.getUuid());
        if (tierId == null) {
            return;
        }

        double hmCfg = DifficultyManager.getSettings().get(tierId, DifficultyIO.SETTING_HEALTH_MULTIPLIER);
        float hm = (float) Math.max(0.0, hmCfg);
        if (hm <= 0.0f || nearlyEquals(hm, 1.0f)) {
            return;
        }

        float shown = info.damageAmount / Math.max(0.000001f, hm);
        info.damageAmount = Math.max(0.0f, shown);
    }

    private static boolean nearlyEquals(float a, float b) {
        return Math.abs(a - b) < 0.000001f;
    }
}
