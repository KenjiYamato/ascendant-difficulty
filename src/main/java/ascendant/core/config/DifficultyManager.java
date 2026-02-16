package ascendant.core.config;

import com.google.gson.*;

import java.io.IOException;
import java.io.Reader;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

public final class DifficultyManager {
    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();

    private static final Object INIT_LOCK = new Object();
    private static final Map<UUID, PlayerSettings> playerSettings = new ConcurrentHashMap<>();
    private static final String PLAYER_SETTING_DIFFICULTY = "difficulty";
    private static final String PLAYER_SETTING_SHOW_BADGE = "showBadge";
    private static final String PLAYER_SETTING_SHOW_TIER_VALUES_AS_PERCENT = "showTierValuesAsPercent";
    private static final boolean DEFAULT_SHOW_BADGE = true;
    private static volatile boolean initialized = false;
    private static DifficultyConfig config;
    private static DifficultySettings settings;

    private DifficultyManager() {
    }

    public static void initialize(DifficultyConfig config, DifficultySettings settings) {
        Objects.requireNonNull(config, "config");
        synchronized (INIT_LOCK) {
            DifficultyManager.config = config;
            DifficultyManager.settings = (settings != null) ? settings : DifficultySettings.fromConfig(config);
            loadPlayerSettings();
            initialized = true;
        }
    }

    // Exposed for systems that need to read raw config or computed settings.
    public static DifficultySettings getSettings() {
        ensureInitialized();
        return settings;
    }

    public static DifficultyConfig getConfig() {
        ensureInitialized();
        return config;
    }

    public static <T> T getFromConfig(ConfigKey<T> key) {
        Objects.requireNonNull(key, "key");
        ensureInitialized();
        return key.read(config);
    }

    // Global switch from config: base.allow.difficultyChange
    public static boolean allowDifficultyChange() {
        ensureInitialized();
        return getFromConfig(DifficultyIO.ALLOW_DIFFICULTY_CHANGE);
    }

    // Resolved difficulty = player override -> default -> first available tier.
    public static String getDifficulty(UUID playerUuid) {
        Objects.requireNonNull(playerUuid, "playerUuid");
        ensureInitialized();

        PlayerSettings settings = playerSettings.get(playerUuid);
        String override = settings != null ? settings.difficultyOverride() : null;
        if (isValidTier(override)) {
            return override;
        }

        String fallback = getFromConfig(DifficultyIO.DEFAULT_DIFFICULTY);
        if (isValidTier(fallback)) {
            return fallback;
        }

        return pickAnyTierOrDefault();
    }

    // Persist player override only if selection is enabled and tier exists.
    public static void setPlayerDifficultyOverride(UUID playerUuid, String tierId) {
        Objects.requireNonNull(playerUuid, "playerUuid");
        ensureInitialized();
        if (!allowDifficultyChange()) {
            return;
        }
        if (tierId == null || tierId.isBlank()) {
            clearPlayerDifficultyOverride(playerUuid);
            return;
        }
        if (!isValidTier(tierId)) {
            return;
        }
        PlayerSettings current = getPlayerSettings(playerUuid);
        PlayerSettings updated = new PlayerSettings(tierId, current.showBadge(), current.showTierValuesAsPercent());
        savePlayerSettings(playerUuid, updated);
    }

    // Removes override so the player falls back to default.
    public static void clearPlayerDifficultyOverride(UUID playerUuid) {
        Objects.requireNonNull(playerUuid, "playerUuid");
        ensureInitialized();
        PlayerSettings current = getPlayerSettings(playerUuid);
        PlayerSettings updated = new PlayerSettings(null, current.showBadge(), current.showTierValuesAsPercent());
        savePlayerSettings(playerUuid, updated);
    }

    public static Map<UUID, String> getPlayerOverridesSnapshot() {
        ensureInitialized();
        Map<UUID, String> snapshot = new HashMap<>();
        for (Map.Entry<UUID, PlayerSettings> entry : playerSettings.entrySet()) {
            String override = entry.getValue().difficultyOverride();
            if (override != null) {
                snapshot.put(entry.getKey(), override);
            }
        }
        return Collections.unmodifiableMap(snapshot);
    }

    public static boolean isBadgeVisible(UUID playerUuid) {
        Objects.requireNonNull(playerUuid, "playerUuid");
        ensureInitialized();
        PlayerSettings settings = playerSettings.get(playerUuid);
        return settings == null ? DEFAULT_SHOW_BADGE : settings.showBadge();
    }

    public static boolean togglePlayerBadgeVisibility(UUID playerUuid) {
        Objects.requireNonNull(playerUuid, "playerUuid");
        ensureInitialized();
        PlayerSettings current = getPlayerSettings(playerUuid);
        boolean newValue = !current.showBadge();
        savePlayerSettings(playerUuid, new PlayerSettings(current.difficultyOverride(), newValue, current.showTierValuesAsPercent()));
        return newValue;
    }

    public static boolean isTierValuesAsPercent(UUID playerUuid) {
        Objects.requireNonNull(playerUuid, "playerUuid");
        ensureInitialized();
        PlayerSettings settings = playerSettings.get(playerUuid);
        return settings == null ? defaultShowTierValuesAsPercent() : settings.showTierValuesAsPercent();
    }

    public static boolean togglePlayerTierValuesAsPercent(UUID playerUuid) {
        Objects.requireNonNull(playerUuid, "playerUuid");
        ensureInitialized();
        PlayerSettings current = getPlayerSettings(playerUuid);
        boolean newValue = !current.showTierValuesAsPercent();
        savePlayerSettings(playerUuid, new PlayerSettings(current.difficultyOverride(), current.showBadge(), newValue));
        return newValue;
    }

    // Reloads base difficulty.json and drop-ins without touching overrides.
    public static void reloadConfig() throws IOException {
        ensureInitialized();
        synchronized (INIT_LOCK) {
            config.reload();
            settings = config.toSettings();
            RuntimeSettings.reload();
        }
    }

    private static void ensureInitialized() {
        if (initialized) {
            return;
        }
        synchronized (INIT_LOCK) {
            if (initialized) {
                return;
            }
            try {
                config = DifficultyIO.loadOrCreateConfig();
                settings = config.toSettings();
            } catch (IOException e) {
                throw new IllegalStateException("Failed to load difficulty config.", e);
            }
            loadPlayerSettings();
            initialized = true;
        }
    }

    private static boolean isValidTier(String tierId) {
        if (tierId == null || tierId.isBlank()) {
            return false;
        }
        Map<String, Map<String, Double>> tiers = settings.tiers();
        return tiers.containsKey(tierId);
    }

    private static String pickAnyTierOrDefault() {
        Set<String> ids = settings.tiers().keySet();
        if (!ids.isEmpty()) {
            return ids.iterator().next();
        }
        return DifficultyIO.DEFAULT_BASE_DIFFICULTY;
    }

    private static boolean defaultShowTierValuesAsPercent() {
        DifficultyConfig currentConfig = config;
        if (currentConfig == null) {
            return DifficultyIO.DEFAULT_UI_TIER_VALUES_AS_PERCENT;
        }
        return DifficultyIO.UI_TIER_VALUES_AS_PERCENT.read(currentConfig);
    }

    private static void loadPlayerSettings() {
        playerSettings.clear();
        boolean loaded = loadPlayerSettingsFromPath(DifficultyIO.PLAYER_SETTINGS_PATH);
        if (loaded) {
            return;
        }
        if (loadPlayerSettingsFromPath(DifficultyIO.LEGACY_PLAYER_OVERRIDES_PATH)) {
            savePlayerSettings();
        }
    }

    private static boolean loadPlayerSettingsFromPath(Path path) {
        if (Files.notExists(path)) {
            return false;
        }
        boolean needsSave = false;
        try (Reader reader = Files.newBufferedReader(path, StandardCharsets.UTF_8)) {
            JsonElement parsed = JsonParser.parseReader(reader);
            if (!parsed.isJsonObject()) {
                return true;
            }
            JsonObject obj = parsed.getAsJsonObject();
            for (Map.Entry<String, JsonElement> entry : obj.entrySet()) {
                UUID uuid;
                try {
                    uuid = UUID.fromString(entry.getKey());
                } catch (IllegalArgumentException ignored) {
                    continue;
                }
                JsonElement value = entry.getValue();
                if (value == null || value.isJsonNull()) {
                    continue;
                }
                if (value.isJsonPrimitive()) {
                    needsSave = true;
                } else if (value.isJsonObject()) {
                    JsonObject settingsObject = value.getAsJsonObject();
                    if (!settingsObject.has(PLAYER_SETTING_SHOW_BADGE)
                            || !settingsObject.has(PLAYER_SETTING_SHOW_TIER_VALUES_AS_PERCENT)) {
                        needsSave = true;
                    }
                }
                PlayerSettings settings = parsePlayerSettings(value);
                if (settings == null) {
                    continue;
                }
                if (shouldStore(settings)) {
                    playerSettings.put(uuid, settings);
                }
            }
        } catch (IOException e) {
            System.err.println("[ascendant] Failed to read " + path + ": " + e.getMessage());
        }
        if (needsSave && path.equals(DifficultyIO.PLAYER_SETTINGS_PATH)) {
            savePlayerSettings();
        }
        return true;
    }

    private static PlayerSettings parsePlayerSettings(JsonElement value) {
        if (value == null || value.isJsonNull()) {
            return null;
        }
        if (value.isJsonPrimitive()) {
            String tier = value.getAsString();
            if (!isValidTier(tier)) {
                return null;
            }
            return new PlayerSettings(tier, DEFAULT_SHOW_BADGE, defaultShowTierValuesAsPercent());
        }
        if (!value.isJsonObject()) {
            return null;
        }
        JsonObject obj = value.getAsJsonObject();
        String difficulty = null;
        JsonElement difficultyElement = obj.get(PLAYER_SETTING_DIFFICULTY);
        if (difficultyElement != null && difficultyElement.isJsonPrimitive()) {
            String candidate = difficultyElement.getAsString();
            if (isValidTier(candidate)) {
                difficulty = candidate;
            }
        }
        boolean showBadge = DEFAULT_SHOW_BADGE;
        JsonElement showBadgeElement = obj.get(PLAYER_SETTING_SHOW_BADGE);
        if (showBadgeElement != null && showBadgeElement.isJsonPrimitive()) {
            try {
                showBadge = showBadgeElement.getAsBoolean();
            } catch (UnsupportedOperationException ignored) {
            }
        }
        boolean showTierValuesAsPercent = defaultShowTierValuesAsPercent();
        JsonElement showTierValuesElement = obj.get(PLAYER_SETTING_SHOW_TIER_VALUES_AS_PERCENT);
        if (showTierValuesElement != null && showTierValuesElement.isJsonPrimitive()) {
            try {
                showTierValuesAsPercent = showTierValuesElement.getAsBoolean();
            } catch (UnsupportedOperationException ignored) {
            }
        }
        return new PlayerSettings(difficulty, showBadge, showTierValuesAsPercent);
    }

    private static PlayerSettings getPlayerSettings(UUID playerUuid) {
        PlayerSettings existing = playerSettings.get(playerUuid);
        if (existing != null) {
            return existing;
        }
        return new PlayerSettings(null, DEFAULT_SHOW_BADGE, defaultShowTierValuesAsPercent());
    }

    private static void savePlayerSettings(UUID playerUuid, PlayerSettings settings) {
        if (shouldStore(settings)) {
            playerSettings.put(playerUuid, settings);
        } else {
            playerSettings.remove(playerUuid);
        }
        savePlayerSettings();
    }

    private static boolean shouldStore(PlayerSettings settings) {
        return settings.difficultyOverride() != null
                || settings.showBadge() != DEFAULT_SHOW_BADGE
                || settings.showTierValuesAsPercent() != defaultShowTierValuesAsPercent();
    }

    private static void savePlayerSettings() {
        try {
            Files.createDirectories(DifficultyIO.PLAYER_SETTINGS_PATH.getParent());
            JsonObject root = new JsonObject();
            for (Map.Entry<UUID, PlayerSettings> entry : playerSettings.entrySet()) {
                PlayerSettings settings = entry.getValue();
                if (!shouldStore(settings)) {
                    continue;
                }
                JsonObject playerNode = new JsonObject();
                if (settings.difficultyOverride() != null) {
                    playerNode.addProperty(PLAYER_SETTING_DIFFICULTY, settings.difficultyOverride());
                }
                playerNode.addProperty(PLAYER_SETTING_SHOW_BADGE, settings.showBadge());
                playerNode.addProperty(PLAYER_SETTING_SHOW_TIER_VALUES_AS_PERCENT, settings.showTierValuesAsPercent());
                root.add(entry.getKey().toString(), playerNode);
            }
            Files.writeString(DifficultyIO.PLAYER_SETTINGS_PATH, GSON.toJson(root), StandardCharsets.UTF_8);
        } catch (IOException e) {
            System.err.println("[ascendant] Failed to write " + DifficultyIO.PLAYER_SETTINGS_PATH + ": " + e.getMessage());
        }
    }

    private record PlayerSettings(String difficultyOverride, boolean showBadge, boolean showTierValuesAsPercent) {
    }
}
