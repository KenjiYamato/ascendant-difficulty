package ascendant.core.config;

import com.google.gson.*;

import java.io.IOException;
import java.io.Reader;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

public final class DifficultyManager {
    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();

    private static final Object INIT_LOCK = new Object();
    private static final Map<UUID, String> playerOverrides = new ConcurrentHashMap<>();
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
            loadOverrides();
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

    // Global switch from config: base.allowDifficultyChange
    public static boolean canPlayerSelect() {
        ensureInitialized();
        return getFromConfig(DifficultyIO.ALLOW_CHANGE);
    }

    // Resolved difficulty = player override -> default -> first available tier.
    public static String getDifficulty(UUID playerUuid) {
        Objects.requireNonNull(playerUuid, "playerUuid");
        ensureInitialized();

        String override = playerOverrides.get(playerUuid);
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
        if (!canPlayerSelect()) {
            return;
        }
        if (tierId == null || tierId.isBlank()) {
            clearPlayerDifficultyOverride(playerUuid);
            return;
        }
        if (!isValidTier(tierId)) {
            return;
        }
        playerOverrides.put(playerUuid, tierId);
        saveOverrides();
    }

    // Removes override so the player falls back to default.
    public static void clearPlayerDifficultyOverride(UUID playerUuid) {
        Objects.requireNonNull(playerUuid, "playerUuid");
        ensureInitialized();
        playerOverrides.remove(playerUuid);
        saveOverrides();
    }

    public static Map<UUID, String> getPlayerOverridesSnapshot() {
        ensureInitialized();
        return Collections.unmodifiableMap(playerOverrides);
    }

    // Reloads difficulty.json without touching overrides.
    public static void reloadConfig() throws IOException {
        ensureInitialized();
        synchronized (INIT_LOCK) {
            config.reload();
            settings = config.toSettings();
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
            loadOverrides();
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

    private static void loadOverrides() {
        playerOverrides.clear();
        if (Files.notExists(DifficultyIO.PLAYER_OVERRIDES_PATH)) {
            return;
        }
        try (Reader reader = Files.newBufferedReader(DifficultyIO.PLAYER_OVERRIDES_PATH, StandardCharsets.UTF_8)) {
            JsonElement parsed = JsonParser.parseReader(reader);
            if (!parsed.isJsonObject()) {
                return;
            }
            JsonObject obj = parsed.getAsJsonObject();
            for (Map.Entry<String, JsonElement> entry : obj.entrySet()) {
                if (!entry.getValue().isJsonPrimitive()) {
                    continue;
                }
                String tier = entry.getValue().getAsString();
                if (!isValidTier(tier)) {
                    continue;
                }
                try {
                    UUID uuid = UUID.fromString(entry.getKey());
                    playerOverrides.put(uuid, tier);
                } catch (IllegalArgumentException ignored) {
                }
            }
        } catch (IOException e) {
            System.err.println("[ascendant] Failed to read " + DifficultyIO.PLAYER_OVERRIDES_PATH + ": " + e.getMessage());
        }
    }

    private static void saveOverrides() {
        try {
            Files.createDirectories(DifficultyIO.PLAYER_OVERRIDES_PATH.getParent());
            JsonObject root = new JsonObject();
            for (Map.Entry<UUID, String> entry : playerOverrides.entrySet()) {
                root.addProperty(entry.getKey().toString(), entry.getValue());
            }
            Files.writeString(DifficultyIO.PLAYER_OVERRIDES_PATH, GSON.toJson(root), StandardCharsets.UTF_8);
        } catch (IOException e) {
            System.err.println("[ascendant] Failed to write " + DifficultyIO.PLAYER_OVERRIDES_PATH + ": " + e.getMessage());
        }
    }
}
