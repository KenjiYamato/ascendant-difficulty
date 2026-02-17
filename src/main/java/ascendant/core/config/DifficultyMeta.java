package ascendant.core.config;

import java.util.Objects;

/**
 * Shared tier metadata helpers to avoid duplicating meta key strings and defaults.
 */
public final class DifficultyMeta {
    public static final String META_PREFIX = "meta.";
    public static final String KEY_DISPLAY_NAME = "displayName";
    public static final String KEY_DESCRIPTION = "description";
    public static final String KEY_IMAGE_PATH = "imagePath";
    public static final String KEY_ICON_PATH = "iconPath";
    public static final String KEY_KILLFEED_PREFIX = "killFeedPrefix";
    public static final String KEY_CHAT_PREFIX = "chatPrefix";

    public static final String DEFAULT_IMAGE_PATH = "Images/Difficulty/normal.png";
    public static final String DEFAULT_ICON_PATH = "Images/Difficulty/normal@icon.png";
    public static final String DEFAULT_KILLFEED_PREFIX = "[{tier}] ";
    public static final String DEFAULT_CHAT_PREFIX = "[{tier}] ";

    private DifficultyMeta() {
    }

    public static TierMeta resolve(DifficultyConfig config, String tierId) {
        Objects.requireNonNull(config, "config");
        String safeTierId = (tierId == null || tierId.isBlank()) ? DifficultyIO.DEFAULT_BASE_DIFFICULTY : tierId;
        String base = META_PREFIX + safeTierId + ".";
        String displayName = config.getString(base + KEY_DISPLAY_NAME, safeTierId);
        String description = config.getString(base + KEY_DESCRIPTION, "");
        String imagePath = config.getString(base + KEY_IMAGE_PATH, DEFAULT_IMAGE_PATH);
        String iconPath = config.getString(base + KEY_ICON_PATH, DEFAULT_ICON_PATH);
        String killFeedPrefix = config.getString(base + KEY_KILLFEED_PREFIX, DEFAULT_KILLFEED_PREFIX);
        String chatPrefix = config.getString(base + KEY_CHAT_PREFIX, DEFAULT_CHAT_PREFIX);
        return new TierMeta(safeTierId, displayName, description, imagePath, iconPath, killFeedPrefix, chatPrefix);
    }

    public record TierMeta(
            String tierId,
            String displayName,
            String description,
            String imagePath,
            String iconPath,
            String killFeedPrefix,
            String chatPrefix
    ) {
    }

    public static String formatTierPrefix(String template, String tierName, String tierId) {
        if (template == null) {
            return null;
        }
        String safeTierName = (tierName == null || tierName.isBlank()) ? tierId : tierName;
        String safeTierId = tierId == null ? "" : tierId;
        return template
                .replace("{tier}", safeTierName == null ? "" : safeTierName)
                .replace("{tierName}", safeTierName == null ? "" : safeTierName)
                .replace("{tierId}", safeTierId);
    }
}
