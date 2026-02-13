package ascendant.core.config;

public final class RuntimeSettings {
    private static final Object LOCK = new Object();
    private static volatile boolean LOADED;

    private static double cashVarianceFactor;
    private static boolean allowXPReward;
    private static boolean allowCashReward;
    private static boolean allowCashRewardEvenWithPhysical;
    private static boolean allowCustomLeveling;
    private static boolean customLevelingUseMostDamage;
    private static double customLevelingMostDamageMultiplier;
    private static double customLevelingOtherAttackerMultiplier;
    private static boolean customLevelingRewardMostDamage;
    private static boolean customLevelingIncludeRange;
    private static boolean customLevelingIncludeDefaultStats;
    private static boolean customLevelingIncludeScaledDamage;
    private static double customLevelingScaledDamageFactor;
    private static double customLevelingStatsManaMultiplier;
    private static double customLevelingStatsAmmoMultiplier;
    private static double customLevelingStatsSignatureMultiplier;
    private static boolean customLevelingUseAttitudeMultiplier;
    private static double customLevelingAttitudePlayerReveredScore;
    private static double customLevelingAttitudePlayerFriendlyScore;
    private static double customLevelingAttitudePlayerHostileScore;
    private static double customLevelingAttitudeNpcHostileBonus;
    private static double customLevelingAttitudeThresholdLow;
    private static double customLevelingAttitudeThresholdMid;
    private static double customLevelingAttitudeThresholdHigh;
    private static double customLevelingAttitudeMultiplierLow;
    private static double customLevelingAttitudeMultiplierMid;
    private static double customLevelingAttitudeMultiplierHigh;
    private static double customLevelingDownscaleBase;
    private static double customLevelingDownscaleLevelExponent;
    private static double customLevelingDownscaleLevelMultiplier;
    private static boolean allowLevelingCoreIntegration;
    private static boolean allowEcotaleIntegration;
    private static boolean allowMMOSkillTreeIntegration;
    private static double levelingCoreMultiplier;
    private static double mmoSkillTreeMultiplier;
    private static double ecotaleMultiplier;

    private RuntimeSettings() {
    }

    public static void load() {
        if (LOADED) {
            return;
        }
        synchronized (LOCK) {
            if (LOADED) {
                return;
            }
            cashVarianceFactor = DifficultyManager.getFromConfig(DifficultyIO.CASH_VARIANCE_FACTOR);
            allowCashReward = DifficultyManager.getFromConfig(DifficultyIO.ALLOW_CASH_REWARD);
            allowCashRewardEvenWithPhysical = DifficultyManager.getFromConfig(DifficultyIO.ALLOW_CASH_REWARD_EVEN_WITH_PHYSICAL);
            allowXPReward = DifficultyManager.getFromConfig(DifficultyIO.ALLOW_XP_REWARD);
            allowCustomLeveling = DifficultyManager.getFromConfig(DifficultyIO.ALLOW_CUSTOM_LEVELING);
            customLevelingUseMostDamage = DifficultyManager.getFromConfig(DifficultyIO.CUSTOM_LEVELING_USE_MOST_DAMAGE);
            customLevelingMostDamageMultiplier = DifficultyManager.getFromConfig(DifficultyIO.CUSTOM_LEVELING_MOST_DAMAGE_MULTIPLIER);
            customLevelingOtherAttackerMultiplier = DifficultyManager.getFromConfig(DifficultyIO.CUSTOM_LEVELING_OTHER_ATTACKER_MULTIPLIER);
            customLevelingRewardMostDamage = DifficultyManager.getFromConfig(DifficultyIO.CUSTOM_LEVELING_REWARD_MOST_DAMAGE);
            customLevelingIncludeRange = DifficultyManager.getFromConfig(DifficultyIO.CUSTOM_LEVELING_INCLUDE_RANGE);
            customLevelingIncludeDefaultStats = DifficultyManager.getFromConfig(DifficultyIO.CUSTOM_LEVELING_INCLUDE_DEFAULT_STATS);
            customLevelingIncludeScaledDamage = DifficultyManager.getFromConfig(DifficultyIO.CUSTOM_LEVELING_INCLUDE_SCALED_DAMAGE);
            customLevelingScaledDamageFactor = DifficultyManager.getFromConfig(DifficultyIO.CUSTOM_LEVELING_SCALED_DAMAGE_FACTOR);
            customLevelingStatsManaMultiplier = DifficultyManager.getFromConfig(DifficultyIO.CUSTOM_LEVELING_STATS_MANA_MULTIPLIER);
            customLevelingStatsAmmoMultiplier = DifficultyManager.getFromConfig(DifficultyIO.CUSTOM_LEVELING_STATS_AMMO_MULTIPLIER);
            customLevelingStatsSignatureMultiplier = DifficultyManager.getFromConfig(DifficultyIO.CUSTOM_LEVELING_STATS_SIGNATURE_MULTIPLIER);
            customLevelingUseAttitudeMultiplier = DifficultyManager.getFromConfig(DifficultyIO.CUSTOM_LEVELING_USE_ATTITUDE_MULTIPLIER);
            customLevelingAttitudePlayerReveredScore = DifficultyManager.getFromConfig(DifficultyIO.CUSTOM_LEVELING_ATTITUDE_PLAYER_REVERED_SCORE);
            customLevelingAttitudePlayerFriendlyScore = DifficultyManager.getFromConfig(DifficultyIO.CUSTOM_LEVELING_ATTITUDE_PLAYER_FRIENDLY_SCORE);
            customLevelingAttitudePlayerHostileScore = DifficultyManager.getFromConfig(DifficultyIO.CUSTOM_LEVELING_ATTITUDE_PLAYER_HOSTILE_SCORE);
            customLevelingAttitudeNpcHostileBonus = DifficultyManager.getFromConfig(DifficultyIO.CUSTOM_LEVELING_ATTITUDE_NPC_HOSTILE_BONUS);
            customLevelingAttitudeThresholdLow = DifficultyManager.getFromConfig(DifficultyIO.CUSTOM_LEVELING_ATTITUDE_THRESHOLD_LOW);
            customLevelingAttitudeThresholdMid = DifficultyManager.getFromConfig(DifficultyIO.CUSTOM_LEVELING_ATTITUDE_THRESHOLD_MID);
            customLevelingAttitudeThresholdHigh = DifficultyManager.getFromConfig(DifficultyIO.CUSTOM_LEVELING_ATTITUDE_THRESHOLD_HIGH);
            customLevelingAttitudeMultiplierLow = DifficultyManager.getFromConfig(DifficultyIO.CUSTOM_LEVELING_ATTITUDE_MULTIPLIER_LOW);
            customLevelingAttitudeMultiplierMid = DifficultyManager.getFromConfig(DifficultyIO.CUSTOM_LEVELING_ATTITUDE_MULTIPLIER_MID);
            customLevelingAttitudeMultiplierHigh = DifficultyManager.getFromConfig(DifficultyIO.CUSTOM_LEVELING_ATTITUDE_MULTIPLIER_HIGH);
            customLevelingDownscaleBase = DifficultyManager.getFromConfig(DifficultyIO.CUSTOM_LEVELING_DOWNSCALE_BASE);
            customLevelingDownscaleLevelExponent = DifficultyManager.getFromConfig(DifficultyIO.CUSTOM_LEVELING_DOWNSCALE_LEVEL_EXPONENT);
            customLevelingDownscaleLevelMultiplier = DifficultyManager.getFromConfig(DifficultyIO.CUSTOM_LEVELING_DOWNSCALE_LEVEL_MULTIPLIER);
            allowLevelingCoreIntegration = DifficultyManager.getFromConfig(DifficultyIO.INTEGRATION_LEVELING_CORE);
            allowEcotaleIntegration = DifficultyManager.getFromConfig(DifficultyIO.INTEGRATION_ECOTALE);
            allowMMOSkillTreeIntegration = DifficultyManager.getFromConfig(DifficultyIO.INTEGRATION_MMO_SKILLTREE);
            levelingCoreMultiplier = DifficultyManager.getFromConfig(DifficultyIO.INTEGRATION_MULTIPLIER_LEVELING_CORE);
            mmoSkillTreeMultiplier = DifficultyManager.getFromConfig(DifficultyIO.INTEGRATION_MULTIPLIER_MMO_SKILLTREE);
            ecotaleMultiplier = DifficultyManager.getFromConfig(DifficultyIO.INTEGRATION_MULTIPLIER_ECOTALE);
            LOADED = true;
        }
    }

    public static void reload() {
        synchronized (LOCK) {
            LOADED = false;
        }
        load();
    }

    public static double cashVarianceFactor() {
        ensureLoaded();
        return cashVarianceFactor;
    }

    public static boolean allowXPReward() {
        ensureLoaded();
        return allowXPReward;
    }

    public static boolean allowCashReward() {
        ensureLoaded();
        return allowCashReward;
    }

    public static boolean allowCashRewardEvenWithPhysical() {
        ensureLoaded();
        return allowCashRewardEvenWithPhysical;
    }

    public static boolean allowCustomLeveling() {
        ensureLoaded();
        return allowCustomLeveling;
    }

    public static boolean customLevelingUseMostDamage() {
        ensureLoaded();
        return customLevelingUseMostDamage;
    }

    public static double customLevelingMostDamageMultiplier() {
        ensureLoaded();
        return customLevelingMostDamageMultiplier;
    }

    public static double customLevelingOtherAttackerMultiplier() {
        ensureLoaded();
        return customLevelingOtherAttackerMultiplier;
    }

    public static boolean customLevelingRewardMostDamage() {
        ensureLoaded();
        return customLevelingRewardMostDamage;
    }

    public static boolean customLevelingIncludeRange() {
        ensureLoaded();
        return customLevelingIncludeRange;
    }

    public static boolean customLevelingIncludeDefaultStats() {
        ensureLoaded();
        return customLevelingIncludeDefaultStats;
    }

    public static boolean customLevelingIncludeScaledDamage() {
        ensureLoaded();
        return customLevelingIncludeScaledDamage;
    }

    public static double customLevelingScaledDamageFactor() {
        ensureLoaded();
        return customLevelingScaledDamageFactor;
    }

    public static double customLevelingStatsManaMultiplier() {
        ensureLoaded();
        return customLevelingStatsManaMultiplier;
    }

    public static double customLevelingStatsAmmoMultiplier() {
        ensureLoaded();
        return customLevelingStatsAmmoMultiplier;
    }

    public static double customLevelingStatsSignatureMultiplier() {
        ensureLoaded();
        return customLevelingStatsSignatureMultiplier;
    }

    public static boolean customLevelingUseAttitudeMultiplier() {
        ensureLoaded();
        return customLevelingUseAttitudeMultiplier;
    }

    public static double customLevelingAttitudePlayerReveredScore() {
        ensureLoaded();
        return customLevelingAttitudePlayerReveredScore;
    }

    public static double customLevelingAttitudePlayerFriendlyScore() {
        ensureLoaded();
        return customLevelingAttitudePlayerFriendlyScore;
    }

    public static double customLevelingAttitudePlayerHostileScore() {
        ensureLoaded();
        return customLevelingAttitudePlayerHostileScore;
    }

    public static double customLevelingAttitudeNpcHostileBonus() {
        ensureLoaded();
        return customLevelingAttitudeNpcHostileBonus;
    }

    public static double customLevelingAttitudeThresholdLow() {
        ensureLoaded();
        return customLevelingAttitudeThresholdLow;
    }

    public static double customLevelingAttitudeThresholdMid() {
        ensureLoaded();
        return customLevelingAttitudeThresholdMid;
    }

    public static double customLevelingAttitudeThresholdHigh() {
        ensureLoaded();
        return customLevelingAttitudeThresholdHigh;
    }

    public static double customLevelingAttitudeMultiplierLow() {
        ensureLoaded();
        return customLevelingAttitudeMultiplierLow;
    }

    public static double customLevelingAttitudeMultiplierMid() {
        ensureLoaded();
        return customLevelingAttitudeMultiplierMid;
    }

    public static double customLevelingAttitudeMultiplierHigh() {
        ensureLoaded();
        return customLevelingAttitudeMultiplierHigh;
    }

    public static double customLevelingDownscaleBase() {
        ensureLoaded();
        return customLevelingDownscaleBase;
    }

    public static double customLevelingDownscaleLevelExponent() {
        ensureLoaded();
        return customLevelingDownscaleLevelExponent;
    }

    public static double customLevelingDownscaleLevelMultiplier() {
        ensureLoaded();
        return customLevelingDownscaleLevelMultiplier;
    }

    public static boolean allowLevelingCoreIntegration() {
        ensureLoaded();
        return allowLevelingCoreIntegration;
    }

    public static boolean allowEcotaleIntegration() {
        ensureLoaded();
        return allowEcotaleIntegration;
    }

    public static boolean allowMMOSkillTreeIntegration() {
        ensureLoaded();
        return allowMMOSkillTreeIntegration;
    }

    public static double levelingCoreMultiplier() {
        ensureLoaded();
        return levelingCoreMultiplier;
    }

    public static double mmoSkillTreeMultiplier() {
        ensureLoaded();
        return mmoSkillTreeMultiplier;
    }

    public static double ecotaleMultiplier() {
        ensureLoaded();
        return ecotaleMultiplier;
    }

    private static void ensureLoaded() {
        if (!LOADED) {
            load();
        }
    }
}
