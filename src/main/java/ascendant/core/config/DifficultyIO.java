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

    public static final String PATH_DEFAULT_DIFFICULTY = "base.defaultDifficulty";
    public static final String PATH_ALLOW_CHANGE = "base.allowDifficultyChange";
    public static final String PATH_ALLOW_BADGE = "base.allowUIBadge";
    public static final String PATH_MIN_DAMAGE_FACTOR = "base.minDamageFactor";
    public static final String PATH_PLAYER_DISTANCE_RADIUS_TO_CHECK = "base.playerDistanceRadiusToCheck";
    public static final String PATH_HEALTH_SCALING_TOLERANCE = "base.healthScalingTolerance";
    public static final String PATH_MIN_HEALTH_SCALING_FACTOR = "base.minHealthScalingFactor";
    public static final String PATH_MAX_HEALTH_SCALING_FACTOR = "base.maxHealthScalingFactor";
    public static final String PATH_CASH_VARIANCE_FACTOR = "base.cashVarianceFactor";
    public static final String PATH_ALLOW_CASH_REWARD = "base.allowCashReward";
    public static final String PATH_ALLOW_CASH_REWARD_EVEN_WITH_PHYSICAL = "base.allowCashRewardEvenWithPhysical";
    public static final String PATH_ALLOW_XP_REWARD = "base.allowXPReward";
    public static final String PATH_ALLOW_HEALTH_MODIFIER = "base.allowHealthModifier";
    public static final String PATH_ALLOW_DAMAGE_MODIFIER = "base.allowDamageModifier";
    public static final String PATH_ALLOW_ARMOR_MODIFIER = "base.allowArmorModifier";
    public static final String PATH_ALLOW_DROP_MODIFIER = "base.allowDropModifier";

    public static final String SETTING_BASE_DAMAGE_RANDOM_PERCENTAGE_MODIFIER = "baseDamageRandomPercentageModifier";
    public static final String SETTING_HEALTH_MULTIPLIER = "health_multiplier";
    public static final String SETTING_MAX_SPEED = "maxSpeed";
    public static final String SETTING_WANDER_RADIUS = "wanderRadius";
    public static final String SETTING_VIEW_RANGE = "viewRange";
    public static final String SETTING_HEARING_RANGE = "hearingRange";
    public static final String SETTING_COMBAT_RELATIVE_TURN_SPEED = "combatRelativeTurnSpeed";
    public static final String SETTING_ARMOR_MULTIPLIER = "armor_multiplier";
    public static final String SETTING_DAMAGE_MULTIPLIER = "damage_multiplier";
    public static final String SETTING_KNOCKBACK_RESISTANCE = "knockbackResistance";
    public static final String SETTING_REGENERATION = "regeneration";
    public static final String SETTING_DROP_RATE_MULTIPLIER = "drop_rate_multiplier";
    public static final String SETTING_DROP_QUANTITY_MULTIPLIER = "drop_quantity_multiplier";
    public static final String SETTING_DROP_QUALITY_MULTIPLIER = "drop_quality_multiplier";
    public static final String SETTING_XP_MULTIPLIER = "xp_multiplier";
    public static final String SETTING_CASH_MULTIPLIER = "cash_multiplier";
    public static final String SETTING_IS_ALLOWED = "is_allowed";
    public static final String SETTING_IS_HIDDEN = "is_hidden";

    public static final String DEFAULT_BASE_DIFFICULTY = "normal";
    public static final boolean DEFAULT_ALLOW_CHANGE = true;
    public static final double DEFAULT_MIN_DAMAGE_FACTOR = 0.001;
    public static final double DEFAULT_PLAYER_DISTANCE_RADIUS_TO_CHECK = 128.0;
    public static final double DEFAULT_HEALTH_SCALING_TOLERANCE = 0.0001;
    public static final double DEFAULT_MIN_HEALTH_SCALING_FACTOR = 0.05;
    public static final double DEFAULT_MAX_HEALTH_SCALING_FACTOR = 300.0;
    public static final double DEFAULT_CASH_VARIANCE_FACTOR = 0.23;
    public static final boolean DEFAULT_ALLOW_BADGE = true;
    public static final boolean DEFAULT_ALLOW_CASH_REWARD = true;
    public static final boolean DEFAULT_ALLOW_CASH_REWARD_EVEN_WITH_PHYSICAL = true;
    public static final boolean DEFAULT_ALLOW_XP_REWARD = true;
    public static final boolean DEFAULT_ALLOW_HEALTH_MODIFIER = true;
    public static final boolean DEFAULT_ALLOW_DAMAGE_MODIFIER = true;
    public static final boolean DEFAULT_ALLOW_ARMOR_MODIFIER = true;
    public static final boolean DEFAULT_ALLOW_DROP_MODIFIER = true;

    public static final ConfigKey<String> DEFAULT_DIFFICULTY =
            ConfigKey.ofString(PATH_DEFAULT_DIFFICULTY, DEFAULT_BASE_DIFFICULTY);
    public static final ConfigKey<Boolean> ALLOW_CHANGE =
            ConfigKey.ofBoolean(PATH_ALLOW_CHANGE, DEFAULT_ALLOW_CHANGE);
    public static final ConfigKey<Boolean> ALLOW_BADGE =
            ConfigKey.ofBoolean(PATH_ALLOW_BADGE, DEFAULT_ALLOW_BADGE);
    public static final ConfigKey<Double> MIN_DAMAGE_FACTOR =
            ConfigKey.ofDouble(PATH_MIN_DAMAGE_FACTOR, DEFAULT_MIN_DAMAGE_FACTOR);
    public static final ConfigKey<Double> PLAYER_DISTANCE_RADIUS_TO_CHECK =
            ConfigKey.ofDouble(PATH_PLAYER_DISTANCE_RADIUS_TO_CHECK, DEFAULT_PLAYER_DISTANCE_RADIUS_TO_CHECK);
    public static final ConfigKey<Double> HEALTH_SCALING_TOLERANCE =
            ConfigKey.ofDouble(PATH_HEALTH_SCALING_TOLERANCE, DEFAULT_HEALTH_SCALING_TOLERANCE);
    public static final ConfigKey<Double> MIN_HEALTH_SCALING_FACTOR =
            ConfigKey.ofDouble(PATH_MIN_HEALTH_SCALING_FACTOR, DEFAULT_MIN_HEALTH_SCALING_FACTOR);
    public static final ConfigKey<Double> MAX_HEALTH_SCALING_FACTOR =
            ConfigKey.ofDouble(PATH_MAX_HEALTH_SCALING_FACTOR, DEFAULT_MAX_HEALTH_SCALING_FACTOR);
    public static final ConfigKey<Double> CASH_VARIANCE_FACTOR =
            ConfigKey.ofDouble(PATH_CASH_VARIANCE_FACTOR, DEFAULT_CASH_VARIANCE_FACTOR);
    public static final ConfigKey<Boolean> ALLOW_CASH_REWARD =
            ConfigKey.ofBoolean(PATH_ALLOW_CASH_REWARD, DEFAULT_ALLOW_CASH_REWARD);
    public static final ConfigKey<Boolean> ALLOW_CASH_REWARD_EVEN_WITH_PHYSICAL =
            ConfigKey.ofBoolean(PATH_ALLOW_CASH_REWARD_EVEN_WITH_PHYSICAL, DEFAULT_ALLOW_CASH_REWARD_EVEN_WITH_PHYSICAL);
    public static final ConfigKey<Boolean> ALLOW_XP_REWARD =
            ConfigKey.ofBoolean(PATH_ALLOW_XP_REWARD, DEFAULT_ALLOW_XP_REWARD);
    public static final ConfigKey<Boolean> ALLOW_HEALTH_MODIFIER =
            ConfigKey.ofBoolean(PATH_ALLOW_HEALTH_MODIFIER, DEFAULT_ALLOW_HEALTH_MODIFIER);
    public static final ConfigKey<Boolean> ALLOW_DAMAGE_MODIFIER =
            ConfigKey.ofBoolean(PATH_ALLOW_DAMAGE_MODIFIER, DEFAULT_ALLOW_DAMAGE_MODIFIER);
    public static final ConfigKey<Boolean> ALLOW_ARMOR_MODIFIER =
            ConfigKey.ofBoolean(PATH_ALLOW_ARMOR_MODIFIER, DEFAULT_ALLOW_ARMOR_MODIFIER);
    public static final ConfigKey<Boolean> ALLOW_DROP_MODIFIER =
            ConfigKey.ofBoolean(PATH_ALLOW_DROP_MODIFIER, DEFAULT_ALLOW_DROP_MODIFIER);

    private DifficultyIO() {
    }

    public static DifficultyConfig loadOrCreateConfig() throws IOException {
        JsonObject defaults = DifficultySettings.defaultJsonFromResource(RESOURCE_DEFAULT_PATH);
        return DifficultyConfig.loadOrCreate(DEFAULT_CONFIG_PATH, defaults);
    }
}
