package ascendant.core.config;

import com.google.gson.JsonObject;

import java.io.IOException;
import java.nio.file.Path;

/**
 * Central location for difficulty file paths and load helpers.
 */
public final class DifficultyIO {
    public static final String RESOURCE_DEFAULT_PATH = "difficulty.json";
    public static final Path DEFAULT_CONFIG_PATH = DifficultyConfig.DEFAULT_PATH;
    public static final Path PLAYER_OVERRIDES_PATH = Path.of("config", "ascendant", "difficulty-players.json");
    public static final String DEFAULT_TIER_ID = "normal";

    public static final String PATH_DEFAULT_DIFFICULTY = "base.defaultDifficulty";
    public static final String PATH_ALLOW_CHANGE = "base.allowDifficultyChange";

    private DifficultyIO() {
    }

    public static DifficultyConfig loadOrCreateConfig() throws IOException {
        JsonObject defaults = DifficultySettings.defaultJsonFromResource(RESOURCE_DEFAULT_PATH);
        return DifficultyConfig.loadOrCreate(DEFAULT_CONFIG_PATH, defaults);
    }
}
