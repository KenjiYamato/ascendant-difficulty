package ascendant.core.config;

import com.google.gson.*;
import com.hypixel.hytale.server.core.universe.PlayerRef;
import com.hypixel.hytale.server.core.universe.Universe;

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
    private static final String WORLD_TIER_SETTING_FIXED_OVERRIDE = "fixedTierOverride";
    private static final boolean DEFAULT_SHOW_BADGE = true;
    private static volatile boolean initialized = false;
    private static DifficultyConfig config;
    private static DifficultySettings settings;
    private static volatile String worldTierAdminOverride;

    private DifficultyManager() {
    }

    public static void initialize(DifficultyConfig config, DifficultySettings settings) {
        Objects.requireNonNull(config, "config");
        synchronized (INIT_LOCK) {
            DifficultyManager.config = config;
            DifficultyManager.settings = (settings != null) ? settings : DifficultySettings.fromConfig(config);
            loadPlayerSettings();
            loadWorldTierSettings();
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

    // Global switch from config: base.allow.difficulty.change
    public static boolean allowDifficultyChange() {
        ensureInitialized();
        return getFromConfig(DifficultyIO.ALLOW_DIFFICULTY_CHANGE);
    }

    // Effective difficulty = world tier (if enabled) otherwise player tier.
    public static String getDifficulty(UUID playerUuid) {
        Objects.requireNonNull(playerUuid, "playerUuid");
        ensureInitialized();
        if (isWorldTierActive()) {
            return resolveWorldTierOrDefault();
        }
        return getPlayerDifficulty(playerUuid);
    }

    // Player tier only (ignores world tier mode).
    public static String getPlayerDifficulty(UUID playerUuid) {
        Objects.requireNonNull(playerUuid, "playerUuid");
        ensureInitialized();

        PlayerSettings playerSetting = playerSettings.get(playerUuid);
        String override = playerSetting != null ? playerSetting.difficultyOverride() : null;
        if (isValidTier(override)) {
            return override;
        }

        String fallback = getFromConfig(DifficultyIO.DEFAULT_DIFFICULTY);
        if (isValidTier(fallback)) {
            return fallback;
        }

        return pickAnyTierOrDefault();
    }

    public static boolean isWorldTierActive() {
        ensureInitialized();
        return getFromConfig(DifficultyIO.WORLD_TIER_ENABLED);
    }

    public static String getWorldTier() {
        ensureInitialized();
        return resolveWorldTierOrDefault();
    }

    public static String getWorldTierAdminOverride() {
        ensureInitialized();
        String value = worldTierAdminOverride;
        return isValidTier(value) ? value : null;
    }

    public static boolean setWorldTierAdminOverride(String tierId) {
        ensureInitialized();
        synchronized (INIT_LOCK) {
            String normalized = normalizeTierId(tierId);
            if (normalized == null) {
                clearWorldTierAdminOverride();
                return true;
            }
            String canonicalTierId = resolveCanonicalTierId(normalized);
            if (canonicalTierId == null) {
                return false;
            }
            worldTierAdminOverride = canonicalTierId;
            saveWorldTierSettings();
            return true;
        }
    }

    public static void clearWorldTierAdminOverride() {
        ensureInitialized();
        synchronized (INIT_LOCK) {
            worldTierAdminOverride = null;
            saveWorldTierSettings();
        }
    }

    public static WorldTierSnapshot getWorldTierSnapshot() {
        ensureInitialized();
        boolean active = isWorldTierActive();
        WorldTierMode mode = resolveWorldTierMode();
        String resolved = resolveWorldTierOrDefault();
        String fixedTier = resolveFixedWorldTier();
        String adminOverride = getWorldTierAdminOverride();
        double scaledFactor = clamp01(getFromConfig(DifficultyIO.WORLD_TIER_SCALED_FACTOR));
        boolean scaledUseAllOnlinePlayers = getFromConfig(DifficultyIO.WORLD_TIER_SCALED_USE_ALL_ONLINE_PLAYERS);
        return new WorldTierSnapshot(
                active,
                mode.configValue,
                resolved,
                fixedTier,
                adminOverride,
                scaledFactor,
                scaledUseAllOnlinePlayers
        );
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
        String canonicalTierId = resolveCanonicalTierId(tierId);
        if (canonicalTierId == null) {
            return;
        }
        PlayerSettings current = getPlayerSettings(playerUuid);
        PlayerSettings updated = new PlayerSettings(canonicalTierId, current.showBadge(), current.showTierValuesAsPercent());
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
            loadWorldTierSettings();
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
            loadWorldTierSettings();
            initialized = true;
        }
    }

    private static boolean isValidTier(String tierId) {
        return resolveCanonicalTierId(tierId) != null;
    }

    private static String resolveCanonicalTierId(String tierId) {
        if (tierId == null || tierId.isBlank()) {
            return null;
        }
        Map<String, Map<String, Double>> tiers = settings.tiers();
        if (tiers.containsKey(tierId)) {
            return tierId;
        }
        for (String existing : tiers.keySet()) {
            if (existing.equalsIgnoreCase(tierId)) {
                return existing;
            }
        }
        return null;
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

    private static String resolveWorldTierOrDefault() {
        String adminOverride = getWorldTierAdminOverride();
        if (adminOverride != null) {
            return adminOverride;
        }

        WorldTierMode mode = resolveWorldTierMode();
        return switch (mode) {
            case HIGHEST, LOWEST, SCALED -> resolveOnlineTierByMode(mode);
            case FIXED -> resolveFixedWorldTier();
        };
    }

    private static String resolveFixedWorldTier() {
        String fixed = getFromConfig(DifficultyIO.WORLD_TIER_FIXED_TIER);
        if (isValidTier(fixed)) {
            return fixed;
        }
        String defaultTier = getFromConfig(DifficultyIO.DEFAULT_DIFFICULTY);
        if (isValidTier(defaultTier)) {
            return defaultTier;
        }
        return pickAnyTierOrDefault();
    }

    private static String resolveOnlineTierByMode(WorldTierMode mode) {
        List<String> orderedTierIds = new ArrayList<>(settings.tiers().keySet());
        if (orderedTierIds.isEmpty()) {
            return DifficultyIO.DEFAULT_BASE_DIFFICULTY;
        }

        Universe universe = Universe.get();
        if (universe == null) {
            return resolveFixedWorldTier();
        }
        List<PlayerRef> onlinePlayers = universe.getPlayers();
        if (onlinePlayers.isEmpty()) {
            return resolveFixedWorldTier();
        }

        Map<String, Integer> tierIndexById = new HashMap<>();
        for (int i = 0; i < orderedTierIds.size(); i++) {
            tierIndexById.put(orderedTierIds.get(i), i);
        }

        int min = Integer.MAX_VALUE;
        int max = Integer.MIN_VALUE;
        List<Integer> onlineTierIndices = new ArrayList<>();
        for (PlayerRef playerRef : onlinePlayers) {
            if (playerRef == null || !playerRef.isValid()) {
                continue;
            }
            UUID uuid = playerRef.getUuid();
            String playerTier = getPlayerDifficulty(uuid);
            Integer idx = tierIndexById.get(playerTier);
            if (idx == null) {
                continue;
            }
            onlineTierIndices.add(idx);
            if (idx < min) {
                min = idx;
            }
            if (idx > max) {
                max = idx;
            }
        }

        if (onlineTierIndices.isEmpty() || min == Integer.MAX_VALUE || max == Integer.MIN_VALUE) {
            return resolveFixedWorldTier();
        }

        int resolvedIndex = switch (mode) {
            case HIGHEST -> max;
            case LOWEST -> min;
            case SCALED -> resolveScaledTierIndex(min, max, onlineTierIndices);
            case FIXED -> min;
        };

        if (resolvedIndex < 0) {
            resolvedIndex = 0;
        } else if (resolvedIndex >= orderedTierIds.size()) {
            resolvedIndex = orderedTierIds.size() - 1;
        }
        return orderedTierIds.get(resolvedIndex);
    }

    private static int resolveScaledTierIndex(int min, int max, List<Integer> onlineTierIndices) {
        double factor = clamp01(getFromConfig(DifficultyIO.WORLD_TIER_SCALED_FACTOR));
        boolean useAllOnlinePlayers = getFromConfig(DifficultyIO.WORLD_TIER_SCALED_USE_ALL_ONLINE_PLAYERS);

        if (!useAllOnlinePlayers || onlineTierIndices.size() <= 1) {
            double scaled = min + ((max - min) * factor);
            return (int) Math.round(scaled);
        }

        List<Integer> sorted = new ArrayList<>(onlineTierIndices);
        sorted.sort(Integer::compareTo);

        int size = sorted.size();
        if (size == 1) {
            return sorted.getFirst();
        }

        double position = factor * (size - 1);
        int lowerIndex = (int) Math.floor(position);
        int upperIndex = (int) Math.ceil(position);
        double lowerValue = sorted.get(lowerIndex);
        double upperValue = sorted.get(upperIndex);
        double localT = position - lowerIndex;
        double interpolated = lowerValue + ((upperValue - lowerValue) * localT);
        return (int) Math.round(interpolated);
    }

    private static WorldTierMode resolveWorldTierMode() {
        String raw = getFromConfig(DifficultyIO.WORLD_TIER_MODE);
        return WorldTierMode.fromConfig(raw);
    }

    private static double clamp01(double value) {
        if (!Double.isFinite(value)) {
            return 0.0;
        }
        if (value <= 0.0) {
            return 0.0;
        }
        if (value >= 1.0) {
            return 1.0;
        }
        return value;
    }

    private static String normalizeTierId(String value) {
        if (value == null) {
            return null;
        }
        String trimmed = value.trim();
        return trimmed.isEmpty() ? null : trimmed;
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

    private static void loadWorldTierSettings() {
        worldTierAdminOverride = null;
        Path path = DifficultyIO.WORLD_TIER_SETTINGS_PATH;
        if (Files.notExists(path)) {
            return;
        }
        try (Reader reader = Files.newBufferedReader(path, StandardCharsets.UTF_8)) {
            JsonElement parsed = JsonParser.parseReader(reader);
            if (parsed == null || !parsed.isJsonObject()) {
                return;
            }
            JsonObject obj = parsed.getAsJsonObject();
            JsonElement fixedTierOverrideElement = obj.get(WORLD_TIER_SETTING_FIXED_OVERRIDE);
            if (fixedTierOverrideElement == null || !fixedTierOverrideElement.isJsonPrimitive()) {
                return;
            }
            String candidate = normalizeTierId(fixedTierOverrideElement.getAsString());
            String canonicalTierId = resolveCanonicalTierId(candidate);
            if (canonicalTierId != null) {
                worldTierAdminOverride = canonicalTierId;
            }
        } catch (IOException e) {
            System.err.println("[ascendant] Failed to read " + path + ": " + e.getMessage());
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
            String canonicalTierId = resolveCanonicalTierId(tier);
            if (canonicalTierId == null) {
                return null;
            }
            return new PlayerSettings(canonicalTierId, DEFAULT_SHOW_BADGE, defaultShowTierValuesAsPercent());
        }
        if (!value.isJsonObject()) {
            return null;
        }
        JsonObject obj = value.getAsJsonObject();
        String difficulty = null;
        JsonElement difficultyElement = obj.get(PLAYER_SETTING_DIFFICULTY);
        if (difficultyElement != null && difficultyElement.isJsonPrimitive()) {
            String candidate = difficultyElement.getAsString();
            String canonicalTierId = resolveCanonicalTierId(candidate);
            if (canonicalTierId != null) {
                difficulty = canonicalTierId;
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

    private static void saveWorldTierSettings() {
        try {
            Files.createDirectories(DifficultyIO.WORLD_TIER_SETTINGS_PATH.getParent());
            JsonObject root = new JsonObject();
            if (worldTierAdminOverride != null && !worldTierAdminOverride.isBlank()) {
                root.addProperty(WORLD_TIER_SETTING_FIXED_OVERRIDE, worldTierAdminOverride);
            }
            Files.writeString(DifficultyIO.WORLD_TIER_SETTINGS_PATH, GSON.toJson(root), StandardCharsets.UTF_8);
        } catch (IOException e) {
            System.err.println("[ascendant] Failed to write " + DifficultyIO.WORLD_TIER_SETTINGS_PATH + ": " + e.getMessage());
        }
    }

    public record WorldTierSnapshot(
            boolean active,
            String mode,
            String resolvedTier,
            String fixedTier,
            String adminOverrideTier,
            double scaledFactor,
            boolean scaledUseAllOnlinePlayers
    ) {
    }

    private enum WorldTierMode {
        FIXED("fixed"),
        HIGHEST("highest"),
        LOWEST("lowest"),
        SCALED("scaled");

        private final String configValue;

        WorldTierMode(String configValue) {
            this.configValue = configValue;
        }

        private static WorldTierMode fromConfig(String value) {
            if (value == null || value.isBlank()) {
                return FIXED;
            }
            return switch (value.trim().toLowerCase(Locale.ROOT)) {
                case "highest", "max" -> HIGHEST;
                case "lowest", "min" -> LOWEST;
                case "scaled", "scale" -> SCALED;
                default -> FIXED;
            };
        }
    }

    private record PlayerSettings(String difficultyOverride, boolean showBadge, boolean showTierValuesAsPercent) {
    }
}
