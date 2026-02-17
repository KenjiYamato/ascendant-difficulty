package ascendant.core.scaling;

import ascendant.core.config.DifficultyIO;
import ascendant.core.config.DifficultyManager;
import ascendant.core.config.RuntimeSettings;
import ascendant.core.util.LibraryAvailability;
import ascendant.core.util.Logging;
import ascendant.core.util.PlayerWorldExecutor;
import com.azuredoom.levelingcore.api.LevelingCoreApi;
import com.azuredoom.levelingcore.ui.hud.XPBarHud;
import com.ecotale.api.EcotaleAPI;
import com.hypixel.hytale.component.Ref;
import com.hypixel.hytale.component.Store;
import com.hypixel.hytale.logger.HytaleLogger;
import com.hypixel.hytale.server.core.universe.PlayerRef;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;
import com.ziggfreed.mmoskilltree.api.MMOSkillTreeAPI;
import com.ziggfreed.mmoskilltree.data.SkillType;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

import static ascendant.core.config.DifficultyIO.DEFAULT_BASE_DIFFICULTY;
import static ascendant.core.config.DifficultyMeta.KEY_DISPLAY_NAME;
import static ascendant.core.config.DifficultyMeta.META_PREFIX;

public final class ExperienceAndCashMultiplier {
    private static final HytaleLogger LOGGER = HytaleLogger.forEnclosingClass();
    private static final ConcurrentHashMap<UUID, RewardTierOverride> REWARD_TIER_OVERRIDES = new ConcurrentHashMap<>();
    private static final double MIN_REWARD_SCALE = 0.01;
    private static final double MAX_REWARD_SCALE = 100.0;

    public ExperienceAndCashMultiplier() {
        initialize();
    }

    public static void initialize() {
        RuntimeSettings.load();

        if (!RuntimeSettings.allowCashReward() && !RuntimeSettings.allowXPReward()) {
            return;
        }

        if (RuntimeSettings.allowMMOSkillTreeIntegration()
                && RuntimeSettings.allowXPReward()
                && !LibraryAvailability.isMMOSkillTreePresent()) {
            return;
        }
    }

    private static MultiplierResult getMultiplierXPAmount(String tierId, long amount, double integrationMultiplier) {
        if (amount <= 0L) {
            return new MultiplierResult(amount, 0L, 0L, tierId);
        }
        double effectiveMultiplier = integrationMultiplier <= 0.0 ? 1.0 : integrationMultiplier;
        double xpMultiplier = DifficultyManager.getSettings().get(tierId, DifficultyIO.SETTING_XP_MULTIPLIER);
        double baseMultiplier = xpMultiplier - 1.0;
        if (baseMultiplier <= 0.0) {
            return new MultiplierResult(amount, 0L, 0L, tierId);
        }
        double scaledMultiplier = baseMultiplier * effectiveMultiplier;
        if (scaledMultiplier <= 0.0) {
            return new MultiplierResult(amount, 0L, 0L, tierId);
        }
        long extraAmount = (long) Math.floor((double) amount * scaledMultiplier);
        if (extraAmount <= 0L) {
            return new MultiplierResult(amount, 0L, 0L, tierId);
        }
        long percent = Math.round(scaledMultiplier * 100.0);
        return new MultiplierResult(amount, extraAmount, percent, tierId);
    }

    private static MultiplierResult getMultiplierCashAmount(String tierId, long amount, double integrationMultiplier) {
        if (amount <= 0L) {
            return new MultiplierResult(amount, 0L, 0L, tierId);
        }
        double effectiveMultiplier = integrationMultiplier <= 0.0 ? 1.0 : integrationMultiplier;
        double cashMultiplier = DifficultyManager.getSettings().get(tierId, DifficultyIO.SETTING_CASH_MULTIPLIER);
        if (cashMultiplier <= 0.0) {
            return new MultiplierResult(amount, 0L, 0L, tierId);
        }

        double variance = RuntimeSettings.cashVarianceFactor();
        double min = cashMultiplier - variance;
        double max = cashMultiplier + variance;

        double factor = min + (Math.random() * (max - min));
        long baseExtra = (long) Math.max(1L, Math.floor((double) amount / 10 * factor));
        long extraAmount = (long) Math.floor(baseExtra * effectiveMultiplier);
        if (extraAmount <= 0L) {
            return new MultiplierResult(amount, 0L, 0L, tierId);
        }

        long percent = Math.round((cashMultiplier - 1.0) * 100.0 * effectiveMultiplier);
        return new MultiplierResult(amount, extraAmount, percent, tierId);
    }

    private static MultiplierResult applyRewardScale(@Nonnull MultiplierResult base, double rewardScale) {
        double scale = sanitizeRewardScale(rewardScale);
        if (Math.abs(scale - 1.0) < 0.000001) {
            return base;
        }

        long original = base.originalAmount();
        long extra = base.extraAmount();
        long scaledExtra;

        if (scale >= 1.0) {
            long total = safeAdd(original, extra);
            double scaledTotalD = (double) total * scale;
            long scaledTotal = scaledTotalD >= Long.MAX_VALUE ? Long.MAX_VALUE : (long) Math.floor(scaledTotalD);
            long desiredExtra = safeSubtract(scaledTotal, original);
            scaledExtra = Math.max(0L, desiredExtra);
        } else {
            scaledExtra = (long) Math.floor((double) extra * scale);
        }

        if (scaledExtra <= 0L) {
            return new MultiplierResult(original, 0L, 0L, base.tierId());
        }

        long percent = original > 0L
                ? Math.round((double) scaledExtra / (double) original * 100.0)
                : base.percent();
        return new MultiplierResult(original, scaledExtra, percent, base.tierId());
    }

    private static long safeAdd(long a, long b) {
        long result = a + b;
        if (((a ^ result) & (b ^ result)) < 0L) {
            return a < 0L ? Long.MIN_VALUE : Long.MAX_VALUE;
        }
        return result;
    }

    private static long safeSubtract(long a, long b) {
        long result = a - b;
        if (((a ^ b) & (a ^ result)) < 0L) {
            return a < 0L ? Long.MIN_VALUE : Long.MAX_VALUE;
        }
        return result;
    }

    private static double sanitizeRewardScale(double rewardScale) {
        if (!Double.isFinite(rewardScale) || rewardScale <= 0.0) {
            return 1.0;
        }
        if (rewardScale < MIN_REWARD_SCALE) {
            return MIN_REWARD_SCALE;
        }
        if (rewardScale > MAX_REWARD_SCALE) {
            return MAX_REWARD_SCALE;
        }
        return rewardScale;
    }

    public static double computeTierMismatchScale(@Nullable String playerTier, @Nullable String spawnTier) {
        if (!RuntimeSettings.allowSpawnTierReward()) {
            return 1.0;
        }
        if (playerTier == null || playerTier.isBlank() || spawnTier == null || spawnTier.isBlank()) {
            return 1.0;
        }
        if (playerTier.equals(spawnTier)) {
            return 1.0;
        }
        int playerIndex = tierIndex(playerTier);
        int spawnIndex = tierIndex(spawnTier);
        if (playerIndex == Integer.MAX_VALUE || spawnIndex == Integer.MAX_VALUE) {
            return 1.0;
        }
        int diff = Math.abs(spawnIndex - playerIndex);
        if (diff <= 0) {
            return 1.0;
        }
        boolean playerLower = playerIndex < spawnIndex;
        double perTier = playerLower
                ? RuntimeSettings.spawnTierRewardOverFactor()
                : RuntimeSettings.spawnTierRewardUnderFactor();
        if (!Double.isFinite(perTier) || perTier <= 0.0) {
            return 1.0;
        }
        double scale = Math.pow(perTier, diff);
        return sanitizeRewardScale(scale);
    }

    public static MultiplierResult applyLevelingCoreXPMultiplier(@Nonnull PlayerRef playerRef, long amount) {
        if (!RuntimeSettings.allowLevelingCoreIntegration() || !RuntimeSettings.allowXPReward()) {
            return new MultiplierResult(amount, 0L, 0L, DEFAULT_BASE_DIFFICULTY);
        }
        if (!LibraryAvailability.isLevelingCorePresent()) {
            return new MultiplierResult(amount, 0L, 0L, DEFAULT_BASE_DIFFICULTY);
        }

        UUID playerUuid = playerRef.getUuid();
        RewardTierResolution resolution = resolveRewardTierResolution(playerUuid);
        return applyLevelingCoreXPMultiplier(playerRef, amount, resolution.tierId(), resolution.rewardScale());
    }

    public static MultiplierResult applyLevelingCoreXPMultiplier(@Nonnull PlayerRef playerRef, long amount, @Nonnull String tierId) {
        return applyLevelingCoreXPMultiplier(playerRef, amount, tierId, 1.0);
    }

    public static MultiplierResult applyLevelingCoreXPMultiplier(@Nonnull PlayerRef playerRef, long amount, @Nonnull String tierId, double rewardScale) {
        if (!RuntimeSettings.allowLevelingCoreIntegration() || !RuntimeSettings.allowXPReward()) {
            return new MultiplierResult(amount, 0L, 0L, DEFAULT_BASE_DIFFICULTY);
        }
        if (!LibraryAvailability.isLevelingCorePresent()) {
            return new MultiplierResult(amount, 0L, 0L, DEFAULT_BASE_DIFFICULTY);
        }

        UUID playerUuid = playerRef.getUuid();
        String resolvedTier = resolveTierOrDefault(tierId);
        MultiplierResult result = getMultiplierXPAmount(resolvedTier, amount, RuntimeSettings.levelingCoreMultiplier());
        result = applyRewardScale(result, rewardScale);

        if (result.isZero()) {
            return new MultiplierResult(amount, 0L, 0L, resolvedTier);
        }

        long extraXp = result.extraAmount();

        try {
            LevelingCoreApi.getLevelServiceIfPresent().ifPresent(levelService -> {
                Runnable task = () -> {
                    levelService.addXp(playerUuid, extraXp);
                    XPBarHud.updateHud(playerRef);
                };
                if (!PlayerWorldExecutor.execute(playerRef, task)) {
                    Logging.debug("[XP] Failed to schedule LevelingCore XP update for " + playerUuid);
                }
                //sendNotification(playerRef.getPacketHandler(), extraXp + "XP (+" + percent + "%)", NotificationStyle.Warning);
            });
        } catch (NoClassDefFoundError error) {
            LibraryAvailability.logMissingDependency("LevelingCore", error);
        }
        return new MultiplierResult(result.originalAmount(), result.extraAmount(), result.percent(), resolvedTier);
    }

    public static void applyEcotaleCashMultiplier(@Nonnull PlayerRef playerRef, long amount) {
        if (!RuntimeSettings.allowEcotaleIntegration() || !RuntimeSettings.allowCashReward()) {
            return;
        }
        if (!LibraryAvailability.isEcotalePresent()) {
            return;
        }

        UUID playerUuid = playerRef.getUuid();
        RewardTierResolution resolution = resolveRewardTierResolution(playerUuid);
        applyEcotaleCashMultiplier(playerRef, amount, resolution.tierId(), resolution.rewardScale());
    }

    public static void applyEcotaleCashMultiplier(@Nonnull PlayerRef playerRef, long amount, @Nonnull String tierId) {
        applyEcotaleCashMultiplier(playerRef, amount, tierId, 1.0);
    }

    public static void applyEcotaleCashMultiplier(@Nonnull PlayerRef playerRef, long amount, @Nonnull String tierId, double rewardScale) {
        if (!RuntimeSettings.allowEcotaleIntegration() || !RuntimeSettings.allowCashReward()) {
            return;
        }
        if (!LibraryAvailability.isEcotalePresent()) {
            return;
        }

        UUID playerUuid = playerRef.getUuid();
        String resolvedTier = resolveTierOrDefault(tierId);
        MultiplierResult result = getMultiplierCashAmount(resolvedTier, amount, RuntimeSettings.ecotaleMultiplier());
        result = applyRewardScale(result, rewardScale);

        if (result.isZero()) {
            return;
        }

        long extraCash = result.extraAmount();
        long percent = result.percent();

        try {
            if (!EcotaleAPI.isPhysicalCoinsAvailable() || RuntimeSettings.allowCashRewardEvenWithPhysical()) {
                EcotaleAPI.deposit(playerUuid, extraCash, "Transfer for Difficulty (+" + percent + "%)");
            }
        } catch (NoClassDefFoundError error) {
            LibraryAvailability.logMissingDependency("Ecotale", error);
        }
    }

    public static MultiplierResult applyMMOSkillTreeXPMultiplier(@Nonnull PlayerRef playerRef, long amount, String skillName, String rawMessage) {
        if (!RuntimeSettings.allowMMOSkillTreeIntegration() || !RuntimeSettings.allowXPReward()) {
            return new MultiplierResult(amount, 0L, 0L, DEFAULT_BASE_DIFFICULTY);
        }
        if (!LibraryAvailability.isMMOSkillTreePresent()) {
            return new MultiplierResult(amount, 0L, 0L, DEFAULT_BASE_DIFFICULTY);
        }

        UUID playerUuid = playerRef.getUuid();
        RewardTierResolution resolution = resolveRewardTierResolution(playerUuid);
        return applyMMOSkillTreeXPMultiplier(playerRef, amount, skillName, rawMessage, resolution.tierId(), resolution.rewardScale());
    }

    public static MultiplierResult applyMMOSkillTreeXPMultiplier(@Nonnull PlayerRef playerRef, long amount, String skillName, String rawMessage, @Nonnull String tierId) {
        return applyMMOSkillTreeXPMultiplier(playerRef, amount, skillName, rawMessage, tierId, 1.0);
    }

    public static MultiplierResult applyMMOSkillTreeXPMultiplier(@Nonnull PlayerRef playerRef, long amount, String skillName, String rawMessage, @Nonnull String tierId, double rewardScale) {
        if (!RuntimeSettings.allowMMOSkillTreeIntegration() || !RuntimeSettings.allowXPReward()) {
            return new MultiplierResult(amount, 0L, 0L, DEFAULT_BASE_DIFFICULTY);
        }
        if (!LibraryAvailability.isMMOSkillTreePresent()) {
            return new MultiplierResult(amount, 0L, 0L, DEFAULT_BASE_DIFFICULTY);
        }

        String normalizedSkill = RuntimeSettings.normalizeMmoSkillName(skillName);
        String resolvedTier = resolveTierOrDefault(tierId);
        if (!RuntimeSettings.isMmoSkillTreeXpBonusAllowedKey(normalizedSkill)) {
            return new MultiplierResult(amount, 0L, 0L, resolvedTier);
        }
        SkillType skillType = resolveSkillType(normalizedSkill);
        if (skillType == null) {
            return new MultiplierResult(amount, 0L, 0L, resolvedTier);
        }

        MultiplierResult result = getMultiplierXPAmount(resolvedTier, amount, RuntimeSettings.mmoSkillTreeMultiplier());
        result = applyRewardScale(result, rewardScale);
        if (result.isZero()) {
            return new MultiplierResult(amount, 0L, 0L, resolvedTier);
        }

        String displayName = getDisplayNameFromMultiplierResult(result);
        if (rawMessage.contains(displayName)) {
            return new MultiplierResult(amount, 0L, 0L, resolvedTier);
        }

        if (result.isZero()) {
            return new MultiplierResult(amount, 0L, 0L, resolvedTier);
        }

        try {
            Ref<EntityStore> storeRef = playerRef.getReference();
            if (storeRef == null) {
                return new MultiplierResult(amount, 0L, 0L, resolvedTier);
            }
            Store<EntityStore> store = storeRef.getStore();
            long extraXp = result.extraAmount();
            MMOSkillTreeAPI.addXp(store, storeRef, skillType, extraXp);
            //sendNotification(playerRef.getPacketHandler(), extraXp + "XP (+" + percent + "%)", NotificationStyle.Warning);
        } catch (NoClassDefFoundError error) {
            LibraryAvailability.logMissingDependency("MMOSkillTree", error);
        }
        return new MultiplierResult(result.originalAmount(), result.extraAmount(), result.percent(), resolvedTier);
    }

    @Nullable
    private static SkillType resolveSkillType(@Nullable String normalizedSkill) {
        if (normalizedSkill == null || normalizedSkill.isBlank()) {
            return null;
        }
        try {
            return SkillType.valueOf(normalizedSkill);
        } catch (IllegalArgumentException ignored) {
            return null;
        }
    }

    public static String getDisplayNameFromMultiplierResult(ExperienceAndCashMultiplier.MultiplierResult multiplierResult) {
        String tierId = multiplierResult.tierId();
        String base = META_PREFIX + tierId + ".";
        return DifficultyManager.getConfig().getString(base + KEY_DISPLAY_NAME, tierId);
    }

    public static void queueRewardTierOverride(@Nonnull UUID playerUuid, @Nonnull String tierId) {
        queueRewardTierOverride(playerUuid, tierId, 1.0);
    }

    public static void queueRewardTierOverride(@Nonnull UUID playerUuid, @Nonnull String tierId, double rewardScale) {
        if (!RuntimeSettings.allowSpawnTierReward()) {
            return;
        }
        if (tierId.isBlank()) {
            return;
        }
        REWARD_TIER_OVERRIDES.put(playerUuid, new RewardTierOverride(tierId, sanitizeRewardScale(rewardScale)));
    }

    @Nullable
    public static RewardTierOverride consumeRewardTierOverride(@Nonnull UUID playerUuid) {
        return REWARD_TIER_OVERRIDES.remove(playerUuid);
    }

    public static RewardTierResolution resolveRewardTierResolution(@Nonnull UUID playerUuid) {
        RewardTierOverride override = consumeRewardTierOverride(playerUuid);
        if (RuntimeSettings.allowSpawnTierReward()) {
            if (override != null && override.tierId() != null && !override.tierId().isBlank()) {
                return new RewardTierResolution(override.tierId(), sanitizeRewardScale(override.rewardScale()));
            }
        }
        String tierId = DifficultyManager.getDifficulty(playerUuid);
        String resolved = tierId != null && !tierId.isBlank() ? tierId : DEFAULT_BASE_DIFFICULTY;
        return new RewardTierResolution(resolved, 1.0);
    }

    public static String resolveRewardTier(@Nonnull UUID playerUuid) {
        return resolveRewardTierResolution(playerUuid).tierId();
    }

    @Nullable
    public static String resolveLowerTier(@Nullable String tierA, @Nullable String tierB) {
        String a = (tierA == null || tierA.isBlank()) ? null : tierA;
        String b = (tierB == null || tierB.isBlank()) ? null : tierB;
        if (a == null) {
            return b;
        }
        if (b == null) {
            return a;
        }
        if (a.equals(b)) {
            return a;
        }
        int indexA = tierIndex(a);
        int indexB = tierIndex(b);
        if (indexA == indexB) {
            return a;
        }
        return indexA < indexB ? a : b;
    }

    private static int tierIndex(@Nonnull String tierId) {
        int i = 0;
        for (String id : DifficultyManager.getSettings().tiers().keySet()) {
            if (id.equals(tierId)) {
                return i;
            }
            i++;
        }
        return Integer.MAX_VALUE;
    }

    private static String resolveTierOrDefault(@Nullable String tierId) {
        if (tierId == null || tierId.isBlank()) {
            return DEFAULT_BASE_DIFFICULTY;
        }
        return tierId;
    }

    public record RewardTierResolution(String tierId, double rewardScale) {
    }

    private record RewardTierOverride(String tierId, double rewardScale) {
    }

    public record MultiplierResult(
            long originalAmount,
            long extraAmount,
            long percent,
            String tierId
    ) {
        public boolean isZero() {
            return extraAmount <= 0L;
        }
    }
}
