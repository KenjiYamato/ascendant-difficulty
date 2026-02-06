package ascendant.core.ui;

import ascendant.core.config.DifficultyIO;
import ascendant.core.config.DifficultyManager;
import ascendant.core.config.DifficultyMeta;
import au.ellie.hyui.builders.HudBuilder;
import au.ellie.hyui.builders.HyUIHud;
import au.ellie.hyui.html.TemplateProcessor;
import com.hypixel.hytale.component.Ref;
import com.hypixel.hytale.component.Store;
import com.hypixel.hytale.logger.HytaleLogger;
import com.hypixel.hytale.protocol.packets.interface_.CustomUIEventBindingType;
import com.hypixel.hytale.server.core.entity.entities.Player;
import com.hypixel.hytale.server.core.event.events.player.PlayerDisconnectEvent;
import com.hypixel.hytale.server.core.event.events.player.PlayerReadyEvent;
import com.hypixel.hytale.server.core.universe.PlayerRef;
import com.hypixel.hytale.server.core.universe.world.World;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;

import javax.annotation.Nullable;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.logging.Level;

public final class DifficultyBadge {
    private static final HytaleLogger LOGGER = HytaleLogger.forEnclosingClass();
    private static final String HUD_HTML = "Pages/DifficultyBadge.html";

    private static final ConcurrentHashMap<UUID, HudBuilder> _hudByPlayer = new ConcurrentHashMap<>();
    private static final ConcurrentHashMap<UUID, HyUIHud> _hyUIHudByPlayer = new ConcurrentHashMap<>();

    public static void onPlayerReady(PlayerReadyEvent _event) {
        if (_event == null) {
            return;
        }
        _showForPlayer(_event.getPlayer());
    }

    public static void updateForPlayer(PlayerRef _playerRef) {
        UUID playerUuid = _playerRef.getUuid();
        if (_hudByPlayer.containsKey(playerUuid) && _hyUIHudByPlayer.containsKey(playerUuid)) {
            removeUIForPlayerUUID(playerUuid);
        }
        _showForPlayer(_playerRef);
    }

    @SuppressWarnings("removal")
    public static void onPlayerDisconnect(PlayerDisconnectEvent _event) {
        if (_event == null) {
            return;
        }
        PlayerRef playerRef = _event.getPlayerRef();
        UUID uuid = playerRef.getUuid();
        removeUIForPlayerUUID(uuid);
    }

    public static void removeUIForPlayerUUID(UUID uuid) {
        _hyUIHudByPlayer.get(uuid).remove();
        _hudByPlayer.remove(uuid);
        _hyUIHudByPlayer.remove(uuid);
    }

    @SuppressWarnings("removal")
    private static void _showForPlayer(Player _player) {
        if (_player == null) {
            return;
        }

        PlayerRef playerRef = _player.getPlayerRef();
        if (playerRef == null || !playerRef.isValid()) {
            return;
        }
        _showForPlayer(playerRef);
    }

    @SuppressWarnings("removal")
    private static void _showForPlayer(PlayerRef playerRef) {
        if (!allowBadge()) {
            return;
        }
        UUID playerUuid = playerRef.getUuid();


        String tierId = DifficultyManager.getDifficulty(playerUuid);
        DifficultyMeta.TierMeta meta = DifficultyMeta.resolve(DifficultyManager.getConfig(), tierId);

        TemplateProcessor template = new TemplateProcessor();
        template.setVariable("badgeImagePath", meta.iconPath());

        HudBuilder hud = _hudByPlayer.computeIfAbsent(playerUuid, _uuid -> HudBuilder.detachedHud());

        Ref<EntityStore> storeRef = playerRef.getReference();
        if (storeRef == null) {
            return;
        }
        Store<EntityStore> store = storeRef.getStore();
        World world = store.getExternalData().getWorld();

        world.execute(() -> {
            try {
                hud.loadHtml(HUD_HTML, template);
                _hyUIHudByPlayer.computeIfAbsent(playerUuid, _uuid -> hud.show(playerRef, store));
            } catch (Throwable _) {
            }
        });
    }

    private static boolean allowBadge() {
        return DifficultyManager.getFromConfig(DifficultyIO.ALLOW_BADGE);
    }
}
