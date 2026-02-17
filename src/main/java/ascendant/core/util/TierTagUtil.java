package ascendant.core.util;

import ascendant.core.config.DifficultyManager;
import ascendant.core.config.DifficultyMeta;

import javax.annotation.Nullable;
import java.util.UUID;

public final class TierTagUtil {
    private TierTagUtil() {
    }

    @Nullable
    public static String resolveTierPrefix(@Nullable UUID playerUuid, @Nullable PrefixKind kind) {
        if (playerUuid == null || kind == null) {
            return null;
        }

        String tierId = DifficultyManager.getDifficulty(playerUuid);
        if (tierId == null || tierId.isBlank()) {
            return null;
        }

        DifficultyMeta.TierMeta meta = DifficultyMeta.resolve(DifficultyManager.getConfig(), tierId);
        String tierName = meta.displayName();
        if (tierName == null || tierName.isBlank()) {
            tierName = tierId;
        }

        String template = (kind == PrefixKind.KILLFEED) ? meta.killFeedPrefix() : meta.chatPrefix();
        return DifficultyMeta.formatTierPrefix(template, tierName, tierId);
    }

    @Nullable
    public static String buildTaggedName(@Nullable UUID playerUuid, @Nullable String playerName, @Nullable PrefixKind kind) {
        if (playerName == null || playerName.isBlank()) {
            return null;
        }
        String prefix = resolveTierPrefix(playerUuid, kind);
        if (prefix == null || prefix.isBlank()) {
            return null;
        }
        return prefix + playerName;
    }

    public static String buildTaggedNameOrFallback(@Nullable UUID playerUuid, @Nullable String playerName, @Nullable PrefixKind kind) {
        String tagged = buildTaggedName(playerUuid, playerName, kind);
        if (tagged != null && !tagged.isBlank()) {
            return tagged;
        }
        return playerName == null ? "" : playerName;
    }

    public enum PrefixKind {
        KILLFEED,
        CHAT
    }
}
