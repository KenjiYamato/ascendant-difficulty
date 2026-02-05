package ascendant.core.ui;

import ascendant.core.config.DifficultyConfig;
import ascendant.core.config.DifficultyIO;
import ascendant.core.config.DifficultyManager;
import au.ellie.hyui.builders.HudBuilder;
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

import java.io.IOException;
import java.lang.reflect.Method;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

public final class DifficultyBadge {
    private static final HytaleLogger LOGGER = HytaleLogger.forEnclosingClass();

    private static final String META_PREFIX = "meta.";
    private static final String META_ICON_PATH = "iconPath";
    private static final String DEFAULT_ICON_PATH = "Images/Difficulty/normal@icon.png";

    private static final String HUD_HTML = "Pages/DifficultyBadge.html";

    private static volatile DifficultyConfig _difficultyConfig;
    private static volatile boolean _allowUIBadge;

    private static final ConcurrentHashMap<UUID, HudBuilder> _hudByPlayer = new ConcurrentHashMap<>();

    public DifficultyBadge() {
        _allowUIBadge = DifficultyManager.getConfig().getBoolean("base.allowUIBadge", true);
        _difficultyConfig = _loadOrCreateConfig();
    }

    public static void onPlayerReady(PlayerReadyEvent _event) {
        if (_event == null) {
            return;
        }
        _allowUIBadge = DifficultyManager.getConfig().getBoolean("base.allowUIBadge", true);
        if (_difficultyConfig == null) {
            _difficultyConfig = _loadOrCreateConfig();
        }
        _showOrUpdateForPlayer(_event.getPlayer());
    }

    public static void updateForPlayer(PlayerRef _playerRef) {
        _allowUIBadge = DifficultyManager.getConfig().getBoolean("base.allowUIBadge", true);
        if (_difficultyConfig == null) {
            _difficultyConfig = _loadOrCreateConfig();
        }
        _showOrUpdateForPlayer(_playerRef);
    }

    @SuppressWarnings("removal")
    public static void onPlayerDisconnect(PlayerDisconnectEvent _event) {
        if (_event == null) {
            return;
        }
        PlayerRef playerRef = _event.getPlayerRef();
        UUID uuid = playerRef.getUuid();

        if (!playerRef.isValid()) {
            _hudByPlayer.remove(uuid);
            return;
        }

        Ref<EntityStore> storeRef = playerRef.getReference();
        if (storeRef == null) {
            _hudByPlayer.remove(uuid);
            return;
        }

        Store<EntityStore> store = storeRef.getStore();
        World world = store.getExternalData().getWorld();

        HudBuilder hud = _hudByPlayer.remove(uuid);
        if (hud == null) {
            return;
        }

        world.execute(() -> _tryHideHud(hud, playerRef, store));
    }

    private static DifficultyConfig _loadOrCreateConfig() {
        try {
            return DifficultyIO.loadOrCreateConfig();
        } catch (IOException e) {
            throw new RuntimeException(
                    "Failed to loadOrCreate difficulty config at " + DifficultyConfig.DEFAULT_PATH + ": " + e.getMessage(),
                    e
            );
        }
    }


    @SuppressWarnings("removal")
    private static void _showOrUpdateForPlayer(Player _player) {
        if (_player == null) {
            return;
        }

        PlayerRef playerRef = _player.getPlayerRef();
        if (playerRef == null || !playerRef.isValid()) {
            return;
        }
        _showOrUpdateForPlayer(playerRef);
    }

    @SuppressWarnings("removal")
    private static void _showOrUpdateForPlayer(PlayerRef playerRef) {
        if (!_allowUIBadge) {
            return;
        }
        UUID playerUuid = playerRef.getUuid();

        Ref<EntityStore> storeRef = playerRef.getReference();
        if (storeRef == null) {
            return;
        }
        Store<EntityStore> store = storeRef.getStore();
        World world = store.getExternalData().getWorld();

        String tierId = DifficultyManager.getDifficulty(playerUuid);
        if (tierId == null || tierId.isBlank()) {
            tierId = "normal";
        }

        String metaBase = META_PREFIX + tierId + ".";
        String iconPath = _difficultyConfig != null
                ? _difficultyConfig.getString(metaBase + META_ICON_PATH, DEFAULT_ICON_PATH)
                : DEFAULT_ICON_PATH;

        TemplateProcessor template = new TemplateProcessor();
        template.setVariable("badgeImagePath", iconPath);

        HudBuilder hud = _hudByPlayer.computeIfAbsent(playerUuid, _uuid -> HudBuilder.detachedHud());

        world.execute(() -> {
            try {
                hud.loadHtml(HUD_HTML, template);
                hud.show(playerRef, store);
            } catch (Throwable _) {
            }
        });
    }

    private static void _tryHideHud(HudBuilder _hud, PlayerRef _playerRef, Store<EntityStore> _store) {
        if (_hud == null || _playerRef == null || _store == null) {
            return;
        }

        Method hide = _findMethod(_hud.getClass(), "hide", PlayerRef.class, Store.class);
        if (hide != null) {
            _invoke(hide, _hud, _playerRef, _store);
            return;
        }

        Method dismiss = _findMethod(_hud.getClass(), "dismiss", PlayerRef.class, Store.class);
        if (dismiss != null) {
            _invoke(dismiss, _hud, _playerRef, _store);
        }
    }

    private static Method _findMethod(Class<?> _type, String _name, Class<?>... _params) {
        try {
            Method m = _type.getMethod(_name, _params);
            m.setAccessible(true);
            return m;
        } catch (Throwable ignored) {
            return null;
        }
    }

    private static void _invoke(Method _method, Object _target, Object... _args) {
        try {
            _method.invoke(_target, _args);
        } catch (Throwable ignored) {
        }
    }
}
