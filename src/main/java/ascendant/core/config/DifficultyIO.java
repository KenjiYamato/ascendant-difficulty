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
    public static final Path PLAYER_SETTINGS_PATH = Path.of("config", "ascendant", "players-settings.json");
    public static final Path LEGACY_PLAYER_OVERRIDES_PATH = Path.of("config", "ascendant", "difficulty-players.json");
    public static final Path NPC_ROLES_PATH = Path.of("config", "ascendant", "npc_roles.json");

    public static final String PATH_DEFAULT_DIFFICULTY = "base.defaultDifficulty";
    public static final String PATH_ALLOW_DIFFICULTY_CHANGE = "base.allow.difficultyChange";
    public static final String PATH_ALLOW_BADGE = "base.allow.uiBadge";
    public static final String PATH_MIN_DAMAGE_FACTOR = "base.minDamageFactor";
    public static final String PATH_PLAYER_DISTANCE_RADIUS_TO_CHECK = "base.playerDistanceRadiusToCheck";
    public static final String PATH_HEALTH_SCALING_TOLERANCE = "base.healthScalingTolerance";
    public static final String PATH_MIN_HEALTH_SCALING_FACTOR = "base.minHealthScalingFactor";
    public static final String PATH_MAX_HEALTH_SCALING_FACTOR = "base.maxHealthScalingFactor";
    public static final String PATH_CASH_VARIANCE_FACTOR = "base.cashVarianceFactor";
    public static final String PATH_ROUNDING_DIGITS = "base.roundingDigits";
    public static final String PATH_ALLOW_CASH_REWARD = "base.allow.cashReward";
    public static final String PATH_ALLOW_CASH_REWARD_EVEN_WITH_PHYSICAL = "base.allow.cashRewardEvenWithPhysical";
    public static final String PATH_ALLOW_XP_REWARD = "base.allow.xpReward";
    public static final String PATH_ALLOW_CUSTOM_LEVELING = "base.allow.customLeveling";
    public static final String PATH_CUSTOM_LEVELING_USE_MOST_DAMAGE = "base.customLeveling.useMostDamageAttacker";
    public static final String PATH_CUSTOM_LEVELING_MOST_DAMAGE_MULTIPLIER = "base.customLeveling.mostDamageAttackerMultiplier";
    public static final String PATH_CUSTOM_LEVELING_OTHER_ATTACKER_MULTIPLIER = "base.customLeveling.otherAttackerMultiplier";
    public static final String PATH_CUSTOM_LEVELING_REWARD_MOST_DAMAGE = "base.customLeveling.rewardMostDamageAttacker";
    public static final String PATH_CUSTOM_LEVELING_INCLUDE_RANGE = "base.customLeveling.includeRange";
    public static final String PATH_CUSTOM_LEVELING_INCLUDE_DEFAULT_STATS = "base.customLeveling.includeDefaultEntityStats";
    public static final String PATH_CUSTOM_LEVELING_INCLUDE_SCALED_DAMAGE = "base.customLeveling.includeScaledDamage";
    public static final String PATH_CUSTOM_LEVELING_SCALED_DAMAGE_FACTOR = "base.customLeveling.scaledDamageFactor";
    public static final String PATH_CUSTOM_LEVELING_STATS_MANA_MULTIPLIER = "base.customLeveling.statsManaMultiplier";
    public static final String PATH_CUSTOM_LEVELING_STATS_AMMO_MULTIPLIER = "base.customLeveling.statsAmmoMultiplier";
    public static final String PATH_CUSTOM_LEVELING_STATS_SIGNATURE_MULTIPLIER = "base.customLeveling.statsSignatureEnergyMultiplier";
    public static final String PATH_CUSTOM_LEVELING_USE_ATTITUDE_MULTIPLIER = "base.customLeveling.useAttitudeMultiplier";
    public static final String PATH_CUSTOM_LEVELING_ATTITUDE_PLAYER_REVERED_SCORE = "base.customLeveling.attitude.playerReveredScore";
    public static final String PATH_CUSTOM_LEVELING_ATTITUDE_PLAYER_FRIENDLY_SCORE = "base.customLeveling.attitude.playerFriendlyNeutralIgnoreScore";
    public static final String PATH_CUSTOM_LEVELING_ATTITUDE_PLAYER_HOSTILE_SCORE = "base.customLeveling.attitude.playerHostileScore";
    public static final String PATH_CUSTOM_LEVELING_ATTITUDE_NPC_HOSTILE_BONUS = "base.customLeveling.attitude.npcHostileBonus";
    public static final String PATH_CUSTOM_LEVELING_ATTITUDE_THRESHOLD_LOW = "base.customLeveling.attitude.thresholdLow";
    public static final String PATH_CUSTOM_LEVELING_ATTITUDE_THRESHOLD_MID = "base.customLeveling.attitude.thresholdMid";
    public static final String PATH_CUSTOM_LEVELING_ATTITUDE_THRESHOLD_HIGH = "base.customLeveling.attitude.thresholdHigh";
    public static final String PATH_CUSTOM_LEVELING_ATTITUDE_MULTIPLIER_LOW = "base.customLeveling.attitude.multiplierLow";
    public static final String PATH_CUSTOM_LEVELING_ATTITUDE_MULTIPLIER_MID = "base.customLeveling.attitude.multiplierMid";
    public static final String PATH_CUSTOM_LEVELING_ATTITUDE_MULTIPLIER_HIGH = "base.customLeveling.attitude.multiplierHigh";
    public static final String PATH_CUSTOM_LEVELING_DOWNSCALE_BASE = "base.customLeveling.downscale.base";
    public static final String PATH_CUSTOM_LEVELING_DOWNSCALE_LEVEL_EXPONENT = "base.customLeveling.downscale.levelExponent";
    public static final String PATH_CUSTOM_LEVELING_DOWNSCALE_LEVEL_MULTIPLIER = "base.customLeveling.downscale.levelMultiplier";
    public static final String PATH_ALLOW_HEALTH_MODIFIER = "base.allow.healthModifier";
    public static final String PATH_ALLOW_DAMAGE_MODIFIER = "base.allow.damageModifier";
    public static final String PATH_ALLOW_ARMOR_MODIFIER = "base.allow.armorModifier";
    public static final String PATH_ALLOW_DROP_MODIFIER = "base.allow.dropModifier";
    public static final String PATH_ALLOW_DEBUG_LOGGING = "base.allow.debugLogging";
    public static final String PATH_ALLOW_ELITE_SPAWN_MODIFIER = "base.allow.eliteSpawn";

    public static final String PATH_INTEGRATION_ELITE_MOBS = "base.integrations.eliteMobs";
    public static final String PATH_INTEGRATION_ECOTALE = "base.integrations.ecotale";
    public static final String PATH_INTEGRATION_LEVELING_CORE = "base.integrations.levelingCore";
    public static final String PATH_INTEGRATION_MMO_SKILLTREE = "base.integrations.mmoSkillTree";
    public static final String PATH_INTEGRATION_MULTIPLIER_LEVELING_CORE = "base.integrationMultipliers.levelingCore";
    public static final String PATH_INTEGRATION_MULTIPLIER_MMO_SKILLTREE = "base.integrationMultipliers.mmoSkillTree";
    public static final String PATH_INTEGRATION_MULTIPLIER_ECOTALE = "base.integrationMultipliers.ecotale";

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
    public static final String SETTING_ELITE_MOBS_CHANCE_MULTIPLIER = "elite_mobs_chance_multiplier";
    public static final String SETTING_ELITE_MOBS_CHANCE_UNCOMMON = "elite_mobs_chance_uncommon";
    public static final String SETTING_ELITE_MOBS_CHANCE_RARE = "elite_mobs_chance_rare";
    public static final String SETTING_ELITE_MOBS_CHANCE_LEGENDARY = "elite_mobs_chance_legendary";
    public static final String SETTING_IS_ALLOWED = "is_allowed";
    public static final String SETTING_IS_HIDDEN = "is_hidden";
    public static final String SETTING_ELITE_SPAWN_MULTIPLIER = "elite_spawn_multiplier";

    public static final String DEFAULT_BASE_DIFFICULTY = "normal";
    public static final boolean DEFAULT_ALLOW_DIFFICULTY_CHANGE = true;
    public static final double DEFAULT_MIN_DAMAGE_FACTOR = 0.001;
    public static final double DEFAULT_PLAYER_DISTANCE_RADIUS_TO_CHECK = 128.0;
    public static final double DEFAULT_HEALTH_SCALING_TOLERANCE = 0.0001;
    public static final double DEFAULT_MIN_HEALTH_SCALING_FACTOR = 0.05;
    public static final double DEFAULT_MAX_HEALTH_SCALING_FACTOR = 300.0;
    public static final double DEFAULT_CASH_VARIANCE_FACTOR = 0.23;
    public static final int DEFAULT_ROUNDING_DIGITS = 3;
    public static final boolean DEFAULT_ALLOW_BADGE = true;
    public static final boolean DEFAULT_ALLOW_CASH_REWARD = true;
    public static final boolean DEFAULT_ALLOW_CASH_REWARD_EVEN_WITH_PHYSICAL = true;
    public static final boolean DEFAULT_ALLOW_XP_REWARD = true;
    public static final boolean DEFAULT_ALLOW_CUSTOM_LEVELING = true;
    public static final boolean DEFAULT_CUSTOM_LEVELING_USE_MOST_DAMAGE = true;
    public static final double DEFAULT_CUSTOM_LEVELING_MOST_DAMAGE_MULTIPLIER = 1.1;
    public static final double DEFAULT_CUSTOM_LEVELING_OTHER_ATTACKER_MULTIPLIER = 0.1;
    public static final boolean DEFAULT_CUSTOM_LEVELING_REWARD_MOST_DAMAGE = true;
    public static final boolean DEFAULT_CUSTOM_LEVELING_INCLUDE_RANGE = true;
    public static final boolean DEFAULT_CUSTOM_LEVELING_INCLUDE_DEFAULT_STATS = true;
    public static final boolean DEFAULT_CUSTOM_LEVELING_INCLUDE_SCALED_DAMAGE = true;
    public static final double DEFAULT_CUSTOM_LEVELING_SCALED_DAMAGE_FACTOR = 4.0;
    public static final double DEFAULT_CUSTOM_LEVELING_STATS_MANA_MULTIPLIER = 4.0;
    public static final double DEFAULT_CUSTOM_LEVELING_STATS_AMMO_MULTIPLIER = 4.0;
    public static final double DEFAULT_CUSTOM_LEVELING_STATS_SIGNATURE_MULTIPLIER = 10.0;
    public static final boolean DEFAULT_CUSTOM_LEVELING_USE_ATTITUDE_MULTIPLIER = true;
    public static final double DEFAULT_CUSTOM_LEVELING_ATTITUDE_PLAYER_REVERED_SCORE = -1000.0;
    public static final double DEFAULT_CUSTOM_LEVELING_ATTITUDE_PLAYER_FRIENDLY_SCORE = -500.0;
    public static final double DEFAULT_CUSTOM_LEVELING_ATTITUDE_PLAYER_HOSTILE_SCORE = 50.0;
    public static final double DEFAULT_CUSTOM_LEVELING_ATTITUDE_NPC_HOSTILE_BONUS = 50.0;
    public static final double DEFAULT_CUSTOM_LEVELING_ATTITUDE_THRESHOLD_LOW = -900.0;
    public static final double DEFAULT_CUSTOM_LEVELING_ATTITUDE_THRESHOLD_MID = -400.0;
    public static final double DEFAULT_CUSTOM_LEVELING_ATTITUDE_THRESHOLD_HIGH = 50.0;
    public static final double DEFAULT_CUSTOM_LEVELING_ATTITUDE_MULTIPLIER_LOW = 0.05;
    public static final double DEFAULT_CUSTOM_LEVELING_ATTITUDE_MULTIPLIER_MID = 0.2;
    public static final double DEFAULT_CUSTOM_LEVELING_ATTITUDE_MULTIPLIER_HIGH = 1.1;
    public static final double DEFAULT_CUSTOM_LEVELING_DOWNSCALE_BASE = 12.0;
    public static final double DEFAULT_CUSTOM_LEVELING_DOWNSCALE_LEVEL_EXPONENT = 0.75;
    public static final double DEFAULT_CUSTOM_LEVELING_DOWNSCALE_LEVEL_MULTIPLIER = 1.8;
    public static final boolean DEFAULT_ALLOW_HEALTH_MODIFIER = true;
    public static final boolean DEFAULT_ALLOW_DAMAGE_MODIFIER = true;
    public static final boolean DEFAULT_ALLOW_ARMOR_MODIFIER = true;
    public static final boolean DEFAULT_ALLOW_DROP_MODIFIER = true;
    public static final boolean DEFAULT_ALLOW_DEBUG_LOGGING = false;
    public static final boolean DEFAULT_ALLOW_ELITE_SPAWN_MODIFIER = true;
    public static final boolean DEFAULT_INTEGRATION_ELITE_MOBS = true;
    public static final boolean DEFAULT_INTEGRATION_ECOTALE = true;
    public static final boolean DEFAULT_INTEGRATION_LEVELING_CORE = true;
    public static final boolean DEFAULT_INTEGRATION_MMO_SKILLTREE = true;
    public static final double DEFAULT_INTEGRATION_MULTIPLIER_LEVELING_CORE = 0.0;
    public static final double DEFAULT_INTEGRATION_MULTIPLIER_MMO_SKILLTREE = 0.0;
    public static final double DEFAULT_INTEGRATION_MULTIPLIER_ECOTALE = 0.0;

    public static final ConfigKey<String> DEFAULT_DIFFICULTY =
            ConfigKey.ofString(PATH_DEFAULT_DIFFICULTY, DEFAULT_BASE_DIFFICULTY);
    public static final ConfigKey<Boolean> ALLOW_DIFFICULTY_CHANGE =
            ConfigKey.ofBoolean(PATH_ALLOW_DIFFICULTY_CHANGE, DEFAULT_ALLOW_DIFFICULTY_CHANGE);
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
    public static final ConfigKey<Integer> ROUNDING_DIGITS =
            ConfigKey.ofInt(PATH_ROUNDING_DIGITS, DEFAULT_ROUNDING_DIGITS);
    public static final ConfigKey<Boolean> ALLOW_CASH_REWARD =
            ConfigKey.ofBoolean(PATH_ALLOW_CASH_REWARD, DEFAULT_ALLOW_CASH_REWARD);
    public static final ConfigKey<Boolean> ALLOW_CASH_REWARD_EVEN_WITH_PHYSICAL =
            ConfigKey.ofBoolean(PATH_ALLOW_CASH_REWARD_EVEN_WITH_PHYSICAL, DEFAULT_ALLOW_CASH_REWARD_EVEN_WITH_PHYSICAL);
    public static final ConfigKey<Boolean> ALLOW_XP_REWARD =
            ConfigKey.ofBoolean(PATH_ALLOW_XP_REWARD, DEFAULT_ALLOW_XP_REWARD);
    public static final ConfigKey<Boolean> ALLOW_CUSTOM_LEVELING =
            ConfigKey.ofBoolean(PATH_ALLOW_CUSTOM_LEVELING, DEFAULT_ALLOW_CUSTOM_LEVELING);
    public static final ConfigKey<Boolean> CUSTOM_LEVELING_USE_MOST_DAMAGE =
            ConfigKey.ofBoolean(PATH_CUSTOM_LEVELING_USE_MOST_DAMAGE, DEFAULT_CUSTOM_LEVELING_USE_MOST_DAMAGE);
    public static final ConfigKey<Double> CUSTOM_LEVELING_MOST_DAMAGE_MULTIPLIER =
            ConfigKey.ofDouble(PATH_CUSTOM_LEVELING_MOST_DAMAGE_MULTIPLIER, DEFAULT_CUSTOM_LEVELING_MOST_DAMAGE_MULTIPLIER);
    public static final ConfigKey<Double> CUSTOM_LEVELING_OTHER_ATTACKER_MULTIPLIER =
            ConfigKey.ofDouble(PATH_CUSTOM_LEVELING_OTHER_ATTACKER_MULTIPLIER, DEFAULT_CUSTOM_LEVELING_OTHER_ATTACKER_MULTIPLIER);
    public static final ConfigKey<Boolean> CUSTOM_LEVELING_REWARD_MOST_DAMAGE =
            ConfigKey.ofBoolean(PATH_CUSTOM_LEVELING_REWARD_MOST_DAMAGE, DEFAULT_CUSTOM_LEVELING_REWARD_MOST_DAMAGE);
    public static final ConfigKey<Boolean> CUSTOM_LEVELING_INCLUDE_RANGE =
            ConfigKey.ofBoolean(PATH_CUSTOM_LEVELING_INCLUDE_RANGE, DEFAULT_CUSTOM_LEVELING_INCLUDE_RANGE);
    public static final ConfigKey<Boolean> CUSTOM_LEVELING_INCLUDE_DEFAULT_STATS =
            ConfigKey.ofBoolean(PATH_CUSTOM_LEVELING_INCLUDE_DEFAULT_STATS, DEFAULT_CUSTOM_LEVELING_INCLUDE_DEFAULT_STATS);
    public static final ConfigKey<Boolean> CUSTOM_LEVELING_INCLUDE_SCALED_DAMAGE =
            ConfigKey.ofBoolean(PATH_CUSTOM_LEVELING_INCLUDE_SCALED_DAMAGE, DEFAULT_CUSTOM_LEVELING_INCLUDE_SCALED_DAMAGE);
    public static final ConfigKey<Double> CUSTOM_LEVELING_SCALED_DAMAGE_FACTOR =
            ConfigKey.ofDouble(PATH_CUSTOM_LEVELING_SCALED_DAMAGE_FACTOR, DEFAULT_CUSTOM_LEVELING_SCALED_DAMAGE_FACTOR);
    public static final ConfigKey<Double> CUSTOM_LEVELING_STATS_MANA_MULTIPLIER =
            ConfigKey.ofDouble(PATH_CUSTOM_LEVELING_STATS_MANA_MULTIPLIER, DEFAULT_CUSTOM_LEVELING_STATS_MANA_MULTIPLIER);
    public static final ConfigKey<Double> CUSTOM_LEVELING_STATS_AMMO_MULTIPLIER =
            ConfigKey.ofDouble(PATH_CUSTOM_LEVELING_STATS_AMMO_MULTIPLIER, DEFAULT_CUSTOM_LEVELING_STATS_AMMO_MULTIPLIER);
    public static final ConfigKey<Double> CUSTOM_LEVELING_STATS_SIGNATURE_MULTIPLIER =
            ConfigKey.ofDouble(PATH_CUSTOM_LEVELING_STATS_SIGNATURE_MULTIPLIER, DEFAULT_CUSTOM_LEVELING_STATS_SIGNATURE_MULTIPLIER);
    public static final ConfigKey<Boolean> CUSTOM_LEVELING_USE_ATTITUDE_MULTIPLIER =
            ConfigKey.ofBoolean(PATH_CUSTOM_LEVELING_USE_ATTITUDE_MULTIPLIER, DEFAULT_CUSTOM_LEVELING_USE_ATTITUDE_MULTIPLIER);
    public static final ConfigKey<Double> CUSTOM_LEVELING_ATTITUDE_PLAYER_REVERED_SCORE =
            ConfigKey.ofDouble(PATH_CUSTOM_LEVELING_ATTITUDE_PLAYER_REVERED_SCORE, DEFAULT_CUSTOM_LEVELING_ATTITUDE_PLAYER_REVERED_SCORE);
    public static final ConfigKey<Double> CUSTOM_LEVELING_ATTITUDE_PLAYER_FRIENDLY_SCORE =
            ConfigKey.ofDouble(PATH_CUSTOM_LEVELING_ATTITUDE_PLAYER_FRIENDLY_SCORE, DEFAULT_CUSTOM_LEVELING_ATTITUDE_PLAYER_FRIENDLY_SCORE);
    public static final ConfigKey<Double> CUSTOM_LEVELING_ATTITUDE_PLAYER_HOSTILE_SCORE =
            ConfigKey.ofDouble(PATH_CUSTOM_LEVELING_ATTITUDE_PLAYER_HOSTILE_SCORE, DEFAULT_CUSTOM_LEVELING_ATTITUDE_PLAYER_HOSTILE_SCORE);
    public static final ConfigKey<Double> CUSTOM_LEVELING_ATTITUDE_NPC_HOSTILE_BONUS =
            ConfigKey.ofDouble(PATH_CUSTOM_LEVELING_ATTITUDE_NPC_HOSTILE_BONUS, DEFAULT_CUSTOM_LEVELING_ATTITUDE_NPC_HOSTILE_BONUS);
    public static final ConfigKey<Double> CUSTOM_LEVELING_ATTITUDE_THRESHOLD_LOW =
            ConfigKey.ofDouble(PATH_CUSTOM_LEVELING_ATTITUDE_THRESHOLD_LOW, DEFAULT_CUSTOM_LEVELING_ATTITUDE_THRESHOLD_LOW);
    public static final ConfigKey<Double> CUSTOM_LEVELING_ATTITUDE_THRESHOLD_MID =
            ConfigKey.ofDouble(PATH_CUSTOM_LEVELING_ATTITUDE_THRESHOLD_MID, DEFAULT_CUSTOM_LEVELING_ATTITUDE_THRESHOLD_MID);
    public static final ConfigKey<Double> CUSTOM_LEVELING_ATTITUDE_THRESHOLD_HIGH =
            ConfigKey.ofDouble(PATH_CUSTOM_LEVELING_ATTITUDE_THRESHOLD_HIGH, DEFAULT_CUSTOM_LEVELING_ATTITUDE_THRESHOLD_HIGH);
    public static final ConfigKey<Double> CUSTOM_LEVELING_ATTITUDE_MULTIPLIER_LOW =
            ConfigKey.ofDouble(PATH_CUSTOM_LEVELING_ATTITUDE_MULTIPLIER_LOW, DEFAULT_CUSTOM_LEVELING_ATTITUDE_MULTIPLIER_LOW);
    public static final ConfigKey<Double> CUSTOM_LEVELING_ATTITUDE_MULTIPLIER_MID =
            ConfigKey.ofDouble(PATH_CUSTOM_LEVELING_ATTITUDE_MULTIPLIER_MID, DEFAULT_CUSTOM_LEVELING_ATTITUDE_MULTIPLIER_MID);
    public static final ConfigKey<Double> CUSTOM_LEVELING_ATTITUDE_MULTIPLIER_HIGH =
            ConfigKey.ofDouble(PATH_CUSTOM_LEVELING_ATTITUDE_MULTIPLIER_HIGH, DEFAULT_CUSTOM_LEVELING_ATTITUDE_MULTIPLIER_HIGH);
    public static final ConfigKey<Double> CUSTOM_LEVELING_DOWNSCALE_BASE =
            ConfigKey.ofDouble(PATH_CUSTOM_LEVELING_DOWNSCALE_BASE, DEFAULT_CUSTOM_LEVELING_DOWNSCALE_BASE);
    public static final ConfigKey<Double> CUSTOM_LEVELING_DOWNSCALE_LEVEL_EXPONENT =
            ConfigKey.ofDouble(PATH_CUSTOM_LEVELING_DOWNSCALE_LEVEL_EXPONENT, DEFAULT_CUSTOM_LEVELING_DOWNSCALE_LEVEL_EXPONENT);
    public static final ConfigKey<Double> CUSTOM_LEVELING_DOWNSCALE_LEVEL_MULTIPLIER =
            ConfigKey.ofDouble(PATH_CUSTOM_LEVELING_DOWNSCALE_LEVEL_MULTIPLIER, DEFAULT_CUSTOM_LEVELING_DOWNSCALE_LEVEL_MULTIPLIER);
    public static final ConfigKey<Boolean> ALLOW_HEALTH_MODIFIER =
            ConfigKey.ofBoolean(PATH_ALLOW_HEALTH_MODIFIER, DEFAULT_ALLOW_HEALTH_MODIFIER);
    public static final ConfigKey<Boolean> ALLOW_DAMAGE_MODIFIER =
            ConfigKey.ofBoolean(PATH_ALLOW_DAMAGE_MODIFIER, DEFAULT_ALLOW_DAMAGE_MODIFIER);
    public static final ConfigKey<Boolean> ALLOW_ARMOR_MODIFIER =
            ConfigKey.ofBoolean(PATH_ALLOW_ARMOR_MODIFIER, DEFAULT_ALLOW_ARMOR_MODIFIER);
    public static final ConfigKey<Boolean> ALLOW_DROP_MODIFIER =
            ConfigKey.ofBoolean(PATH_ALLOW_DROP_MODIFIER, DEFAULT_ALLOW_DROP_MODIFIER);
    public static final ConfigKey<Boolean> ALLOW_DEBUG_LOGGING =
            ConfigKey.ofBoolean(PATH_ALLOW_DEBUG_LOGGING, DEFAULT_ALLOW_DEBUG_LOGGING);
    public static final ConfigKey<Boolean> ALLOW_ELITE_SPAWN_MODIFIER =
            ConfigKey.ofBoolean(PATH_ALLOW_ELITE_SPAWN_MODIFIER, DEFAULT_ALLOW_ELITE_SPAWN_MODIFIER);
    public static final ConfigKey<Boolean> INTEGRATION_ELITE_MOBS =
            ConfigKey.ofBoolean(PATH_INTEGRATION_ELITE_MOBS, DEFAULT_INTEGRATION_ELITE_MOBS);
    public static final ConfigKey<Boolean> INTEGRATION_ECOTALE =
            ConfigKey.ofBoolean(PATH_INTEGRATION_ECOTALE, DEFAULT_INTEGRATION_ECOTALE);
    public static final ConfigKey<Boolean> INTEGRATION_LEVELING_CORE =
            ConfigKey.ofBoolean(PATH_INTEGRATION_LEVELING_CORE, DEFAULT_INTEGRATION_LEVELING_CORE);
    public static final ConfigKey<Boolean> INTEGRATION_MMO_SKILLTREE =
            ConfigKey.ofBoolean(PATH_INTEGRATION_MMO_SKILLTREE, DEFAULT_INTEGRATION_MMO_SKILLTREE);
    public static final ConfigKey<Double> INTEGRATION_MULTIPLIER_LEVELING_CORE =
            ConfigKey.ofDouble(PATH_INTEGRATION_MULTIPLIER_LEVELING_CORE, DEFAULT_INTEGRATION_MULTIPLIER_LEVELING_CORE);
    public static final ConfigKey<Double> INTEGRATION_MULTIPLIER_MMO_SKILLTREE =
            ConfigKey.ofDouble(PATH_INTEGRATION_MULTIPLIER_MMO_SKILLTREE, DEFAULT_INTEGRATION_MULTIPLIER_MMO_SKILLTREE);
    public static final ConfigKey<Double> INTEGRATION_MULTIPLIER_ECOTALE =
            ConfigKey.ofDouble(PATH_INTEGRATION_MULTIPLIER_ECOTALE, DEFAULT_INTEGRATION_MULTIPLIER_ECOTALE);

    private DifficultyIO() {
    }

    public static DifficultyConfig loadOrCreateConfig() throws IOException {
        JsonObject defaults = DifficultySettings.defaultJsonFromResource(RESOURCE_DEFAULT_PATH);
        return DifficultyConfig.loadOrCreate(DEFAULT_CONFIG_PATH, defaults);
    }
}
