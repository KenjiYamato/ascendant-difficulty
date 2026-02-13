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
import com.hypixel.hytale.server.core.entity.entities.Player;
import com.hypixel.hytale.server.core.event.events.player.PlayerDisconnectEvent;
import com.hypixel.hytale.server.core.event.events.player.PlayerReadyEvent;
import com.hypixel.hytale.server.core.universe.PlayerRef;
import com.hypixel.hytale.server.core.universe.world.World;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;

import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

public final class DifficultyBadge {
    private static final HytaleLogger LOGGER = HytaleLogger.forEnclosingClass();
    private static final String HUD_HTML = "Pages/DifficultyBadge.html";
    private static final ScheduledExecutorService HUD_SCHEDULER = Executors.newSingleThreadScheduledExecutor(r -> {
        Thread t = new Thread(r, "ascendant-difficulty-badge-delay");
        t.setDaemon(true);
        return t;
    });

    private static final ConcurrentHashMap<UUID, HudBuilder> _hudByPlayer = new ConcurrentHashMap<>();
    private static final ConcurrentHashMap<UUID, HyUIHud> _hyUIHudByPlayer = new ConcurrentHashMap<>();

    @SuppressWarnings("removal")
    public static void onPlayerReady(PlayerReadyEvent _event) {
        if (_event == null) {
            return;
        }
        Player player = _event.getPlayer();
        double delayMsCfg = DifficultyManager.getFromConfig(DifficultyIO.UI_BADGE_START_DELAY_MS);
        long delayMs = Math.max(0L, Math.round(delayMsCfg));
        if (delayMs == 0L) {
            _showForPlayer(player);
            return;
        }
        PlayerRef playerRef = player.getPlayerRef();
        if (playerRef == null) {
            return;
        }
        HUD_SCHEDULER.schedule(() -> {
            if (!playerRef.isValid()) {
                return;
            }
            _showForPlayer(playerRef);
        }, delayMs, TimeUnit.MILLISECONDS);
    }

    public static void updateForPlayer(PlayerRef _playerRef) {
        UUID playerUuid = _playerRef.getUuid();
        if (_hudByPlayer.containsKey(playerUuid) || _hyUIHudByPlayer.containsKey(playerUuid)) {
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
        HyUIHud hud = _hyUIHudByPlayer.remove(uuid);
        if (hud != null) {
            hud.remove();
        }
        _hudByPlayer.remove(uuid);
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
        UUID playerUuid = playerRef.getUuid();
        if (playerUuid == null) {
            return;
        }
        if (!allowBadge(playerUuid)) {
            return;
        }

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

    private static boolean allowBadge(UUID playerUuid) {
        return DifficultyManager.getFromConfig(DifficultyIO.ALLOW_BADGE)
                && DifficultyManager.isBadgeVisible(playerUuid);
    }
}
