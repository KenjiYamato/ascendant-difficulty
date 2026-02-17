package ascendant.core.config;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

import java.io.Reader;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

/**
 * Central location for difficulty file paths and load helpers.
 */
public final class DifficultyIO {
    public static final String RESOURCE_DEFAULT_PATH = "difficulty.json";
    public static final Path DEFAULT_CONFIG_PATH = DifficultyConfig.DEFAULT_PATH;
    public static final Path DIFFICULTY_DROPINS_PATH = Path.of("config", "ascendant", "difficultys");
    public static final Path PLAYER_SETTINGS_PATH = Path.of("config", "ascendant", "players-settings.json");
    public static final Path LEGACY_PLAYER_OVERRIDES_PATH = Path.of("config", "ascendant", "difficulty-players.json");
    public static final Path NPC_ROLES_PATH = Path.of("config", "ascendant", "npc_roles.json");

    public static final String PATH_DEFAULT_DIFFICULTY = "base.defaultDifficulty";
    public static final String PATH_ALLOW_DIFFICULTY_CHANGE = "base.allow.difficultyChange";
    public static final String PATH_ALLOW_DIFFICULTY_CHANGE_IN_COMBAT = "base.allow.difficultyChangeInCombat";
    public static final String PATH_ALLOW_BADGE = "base.allow.uiBadge";
    public static final String PATH_UI_BADGE_START_DELAY_MS = "base.uiBadgeStartDelayMs";
    public static final String PATH_UI_TIER_VALUES_AS_PERCENT = "base.uiTierValuesAsPercent";
    public static final String PATH_DIFFICULTY_CHANGE_COOLDOWN_MS = "base.difficultyChangeCooldownMs";
    public static final String PATH_DIFFICULTY_CHANGE_COMBAT_TIMEOUT_MS = "base.difficultyChangeCombatTimeoutMs";
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
    public static final String PATH_ALLOW_SPAWN_TIER_REWARD = "base.allow.spawnTierReward";
    public static final String PATH_SPAWN_TIER_REWARD_OVER_FACTOR = "base.spawnTierRewardOverFactor";
    public static final String PATH_SPAWN_TIER_REWARD_UNDER_FACTOR = "base.spawnTierRewardUnderFactor";
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
    public static final String PATH_ALLOW_DAMAGE_PHYSICAL = "base.allow.damagePhysical";
    public static final String PATH_ALLOW_DAMAGE_PROJECTILE = "base.allow.damageProjectile";
    public static final String PATH_ALLOW_DAMAGE_COMMAND = "base.allow.damageCommand";
    public static final String PATH_ALLOW_DAMAGE_DROWNING = "base.allow.damageDrowning";
    public static final String PATH_ALLOW_DAMAGE_ENVIRONMENT = "base.allow.damageEnvironment";
    public static final String PATH_ALLOW_DAMAGE_FALL = "base.allow.damageFall";
    public static final String PATH_ALLOW_DAMAGE_OUT_OF_WORLD = "base.allow.damageOutOfWorld";
    public static final String PATH_ALLOW_DAMAGE_SUFFOCATION = "base.allow.damageSuffocation";
    public static final String PATH_ALLOW_ARMOR_MODIFIER = "base.allow.armorModifier";
    public static final String PATH_ALLOW_DROP_MODIFIER = "base.allow.dropModifier";
    public static final String PATH_ALLOW_DEBUG_LOGGING = "base.allow.debugLogging";
    public static final String PATH_ALLOW_SPAWN_TIER_NAMEPLATE = "base.allow.spawnTierNameplate";
    public static final String PATH_ALLOW_KILLFEED_TIER_TAG = "base.allow.killFeedTierTag";
    public static final String PATH_ALLOW_KILLFEED_TIER_CHAT = "base.allow.killFeedTierChat";
    public static final String PATH_ALLOW_CHAT_TIER_TAG = "base.allow.chatTierTag";
    public static final String PATH_ALLOW_SERVERLIST_TIER_TAG = "base.allow.serverListTierTag";
    public static final String PATH_COMMAND_TIER_SELECT_NAME = "base.commands.tierSelect.name";
    public static final String PATH_COMMAND_TIER_SELECT_ALIASES = "base.commands.tierSelect.aliases";
    public static final String PATH_COMMAND_TIER_SELECT_PERMISSION = "base.commands.tierSelect.permission";
    public static final String PATH_COMMAND_BADGE_TOGGLE_NAME = "base.commands.badgeToggle.name";
    public static final String PATH_COMMAND_BADGE_TOGGLE_ALIASES = "base.commands.badgeToggle.aliases";
    public static final String PATH_COMMAND_BADGE_TOGGLE_PERMISSION = "base.commands.badgeToggle.permission";
    public static final String PATH_COMMAND_RELOAD_NAME = "base.commands.reload.name";
    public static final String PATH_COMMAND_RELOAD_ALIASES = "base.commands.reload.aliases";
    public static final String PATH_COMMAND_RELOAD_PERMISSION = "base.commands.reload.permission";
    public static final String PATH_COMMAND_DEBUG_CLEAR_ENTITIES_NAME = "base.commands.debugClearEntities.name";
    public static final String PATH_COMMAND_DEBUG_CLEAR_ENTITIES_ALIASES = "base.commands.debugClearEntities.aliases";
    public static final String PATH_COMMAND_DEBUG_CLEAR_ENTITIES_PERMISSION = "base.commands.debugClearEntities.permission";
    public static final String PATH_COMMAND_DEBUG_CLEAR_ITEMS_NAME = "base.commands.debugClearItems.name";
    public static final String PATH_COMMAND_DEBUG_CLEAR_ITEMS_ALIASES = "base.commands.debugClearItems.aliases";
    public static final String PATH_COMMAND_DEBUG_CLEAR_ITEMS_PERMISSION = "base.commands.debugClearItems.permission";
    public static final String PATH_COMMAND_DEBUG_TEST_ATTACK_NAME = "base.commands.debugTestAttack.name";
    public static final String PATH_COMMAND_DEBUG_TEST_ATTACK_ALIASES = "base.commands.debugTestAttack.aliases";
    public static final String PATH_COMMAND_DEBUG_TEST_ATTACK_PERMISSION = "base.commands.debugTestAttack.permission";
    public static final String PATH_COMMAND_DEBUG_TEST_DAMAGE_NAME = "base.commands.debugTestDamage.name";
    public static final String PATH_COMMAND_DEBUG_TEST_DAMAGE_ALIASES = "base.commands.debugTestDamage.aliases";
    public static final String PATH_COMMAND_DEBUG_TEST_DAMAGE_PERMISSION = "base.commands.debugTestDamage.permission";
    public static final String PATH_COMMAND_DEBUG_SPAWN_WRAITH_NAME = "base.commands.debugSpawnWraith.name";
    public static final String PATH_COMMAND_DEBUG_SPAWN_WRAITH_ALIASES = "base.commands.debugSpawnWraith.aliases";
    public static final String PATH_COMMAND_DEBUG_SPAWN_WRAITH_PERMISSION = "base.commands.debugSpawnWraith.permission";
    public static final String PATH_COMMAND_DEBUG_TIER_LOWEST_NAME = "base.commands.debugTierLowest.name";
    public static final String PATH_COMMAND_DEBUG_TIER_LOWEST_ALIASES = "base.commands.debugTierLowest.aliases";
    public static final String PATH_COMMAND_DEBUG_TIER_LOWEST_PERMISSION = "base.commands.debugTierLowest.permission";
    public static final String PATH_COMMAND_DEBUG_TIER_HIGHEST_NAME = "base.commands.debugTierHighest.name";
    public static final String PATH_COMMAND_DEBUG_TIER_HIGHEST_ALIASES = "base.commands.debugTierHighest.aliases";
    public static final String PATH_COMMAND_DEBUG_TIER_HIGHEST_PERMISSION = "base.commands.debugTierHighest.permission";
    public static final String PATH_KILLFEED_CHAT_COLOR_PLAYER = "base.killFeedTierChatColors.playerName";
    public static final String PATH_KILLFEED_CHAT_COLOR_MIDDLE = "base.killFeedTierChatColors.middle";
    public static final String PATH_KILLFEED_CHAT_COLOR_CAUSE = "base.killFeedTierChatColors.deathCause";
    public static final String PATH_KILLFEED_CHAT_MESSAGES_BY_CAUSE_ID = "base.killFeedTierChatMessages.byCauseId";
    public static final String PATH_KILLFEED_CHAT_MESSAGES_FALLBACK = "base.killFeedTierChatMessages.fallback";
    public static final String PATH_KILLFEED_CHAT_MESSAGES_COMMAND = "base.killFeedTierChatMessages.command";
    public static final String PATH_KILLFEED_CHAT_MESSAGES_DROWNING = "base.killFeedTierChatMessages.drowning";
    public static final String PATH_KILLFEED_CHAT_MESSAGES_SUFFOCATION = "base.killFeedTierChatMessages.suffocation";
    public static final String PATH_KILLFEED_CHAT_MESSAGES_ENVIRONMENT = "base.killFeedTierChatMessages.environment";
    public static final String PATH_KILLFEED_CHAT_MESSAGES_FALL = "base.killFeedTierChatMessages.fall";
    public static final String PATH_KILLFEED_CHAT_MESSAGES_OUT_OF_WORLD = "base.killFeedTierChatMessages.outOfWorld";
    public static final String PATH_KILLFEED_CHAT_MESSAGES_PHYSICAL_KILLER_ACTION = "base.killFeedTierChatMessages.physical.killerAction";
    public static final String PATH_KILLFEED_CHAT_MESSAGES_PHYSICAL_KILLER_CAUSE = "base.killFeedTierChatMessages.physical.killerCause";
    public static final String PATH_KILLFEED_CHAT_MESSAGES_PHYSICAL_VICTIM_CAUSE = "base.killFeedTierChatMessages.physical.victimCause";
    public static final String PATH_KILLFEED_CHAT_MESSAGES_PHYSICAL_VICTIM_DIED = "base.killFeedTierChatMessages.physical.victimDied";
    public static final String PATH_ALLOW_DEBUG_COMMANDS = "base.allow.debugCommands";
    public static final String PATH_MMO_SKILLTREE_XP_BONUS_WHITELIST = "base.mmoSkillTree.xpBonusWhitelist";
    public static final String PATH_ALLOW_ELITE_SPAWN_MODIFIER = "base.allow.eliteSpawn";
    public static final String PATH_ELITE_SPAWN_QUEUE_INTERVAL_MS = "base.eliteSpawnQueue.intervalMs";
    public static final String PATH_ELITE_SPAWN_QUEUE_MAX_PER_DRAIN = "base.eliteSpawnQueue.maxPerDrain";
    public static final String PATH_ELITE_SPAWN_QUEUE_MAX_DRAIN_MS = "base.eliteSpawnQueue.maxDrainMs";

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
    public static final String SETTING_DAMAGE_MULTIPLIER_PHYSICAL = "damage_multiplier_physical";
    public static final String SETTING_DAMAGE_MULTIPLIER_PROJECTILE = "damage_multiplier_projectile";
    public static final String SETTING_DAMAGE_MULTIPLIER_COMMAND = "damage_multiplier_command";
    public static final String SETTING_DAMAGE_MULTIPLIER_DROWNING = "damage_multiplier_drowning";
    public static final String SETTING_DAMAGE_MULTIPLIER_ENVIRONMENT = "damage_multiplier_environment";
    public static final String SETTING_DAMAGE_MULTIPLIER_FALL = "damage_multiplier_fall";
    public static final String SETTING_DAMAGE_MULTIPLIER_OUT_OF_WORLD = "damage_multiplier_out_of_world";
    public static final String SETTING_DAMAGE_MULTIPLIER_SUFFOCATION = "damage_multiplier_suffocation";
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
    public static final boolean DEFAULT_ALLOW_DIFFICULTY_CHANGE_IN_COMBAT = false;
    public static final double DEFAULT_UI_BADGE_START_DELAY_MS = 0.0;
    public static final boolean DEFAULT_UI_TIER_VALUES_AS_PERCENT = true;
    public static final double DEFAULT_DIFFICULTY_CHANGE_COOLDOWN_MS = 0.0;
    public static final double DEFAULT_DIFFICULTY_CHANGE_COMBAT_TIMEOUT_MS = 10000.0;
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
    public static final boolean DEFAULT_ALLOW_SPAWN_TIER_REWARD = true;
    public static final double DEFAULT_SPAWN_TIER_REWARD_OVER_FACTOR = 1.05;
    public static final double DEFAULT_SPAWN_TIER_REWARD_UNDER_FACTOR = 0.95;
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
    public static final boolean DEFAULT_ALLOW_DAMAGE_PHYSICAL = true;
    public static final boolean DEFAULT_ALLOW_DAMAGE_PROJECTILE = true;
    public static final boolean DEFAULT_ALLOW_DAMAGE_COMMAND = true;
    public static final boolean DEFAULT_ALLOW_DAMAGE_DROWNING = true;
    public static final boolean DEFAULT_ALLOW_DAMAGE_ENVIRONMENT = true;
    public static final boolean DEFAULT_ALLOW_DAMAGE_FALL = true;
    public static final boolean DEFAULT_ALLOW_DAMAGE_OUT_OF_WORLD = true;
    public static final boolean DEFAULT_ALLOW_DAMAGE_SUFFOCATION = true;
    public static final boolean DEFAULT_ALLOW_ARMOR_MODIFIER = true;
    public static final boolean DEFAULT_ALLOW_DROP_MODIFIER = true;
    public static final boolean DEFAULT_ALLOW_DEBUG_LOGGING = false;
    public static final boolean DEFAULT_ALLOW_SPAWN_TIER_NAMEPLATE = false;
    public static final boolean DEFAULT_ALLOW_KILLFEED_TIER_TAG = false;
    public static final boolean DEFAULT_ALLOW_KILLFEED_TIER_CHAT = false;
    public static final boolean DEFAULT_ALLOW_CHAT_TIER_TAG = false;
    public static final boolean DEFAULT_ALLOW_SERVERLIST_TIER_TAG = true;
    public static final String DEFAULT_COMMAND_TIER_SELECT_NAME = "ascendant-difficulty";
    public static final List<String> DEFAULT_COMMAND_TIER_SELECT_ALIASES = List.of();
    public static final String DEFAULT_COMMAND_TIER_SELECT_PERMISSION = "ascendant.difficulty";
    public static final String DEFAULT_COMMAND_BADGE_TOGGLE_NAME = "ascendant-difficulty-badge-toggle";
    public static final List<String> DEFAULT_COMMAND_BADGE_TOGGLE_ALIASES = List.of();
    public static final String DEFAULT_COMMAND_BADGE_TOGGLE_PERMISSION = "ascendant.difficulty";
    public static final String DEFAULT_COMMAND_RELOAD_NAME = "ascendant-difficulty-reload";
    public static final List<String> DEFAULT_COMMAND_RELOAD_ALIASES = List.of();
    public static final String DEFAULT_COMMAND_RELOAD_PERMISSION = "ascendant.difficulty.reload";
    public static final String DEFAULT_COMMAND_DEBUG_CLEAR_ENTITIES_NAME = "ce";
    public static final List<String> DEFAULT_COMMAND_DEBUG_CLEAR_ENTITIES_ALIASES = List.of();
    public static final String DEFAULT_COMMAND_DEBUG_CLEAR_ENTITIES_PERMISSION = "ascendant.debug.clear_entities";
    public static final String DEFAULT_COMMAND_DEBUG_CLEAR_ITEMS_NAME = "ci";
    public static final List<String> DEFAULT_COMMAND_DEBUG_CLEAR_ITEMS_ALIASES = List.of();
    public static final String DEFAULT_COMMAND_DEBUG_CLEAR_ITEMS_PERMISSION = "ascendant.debug.clear_items";
    public static final String DEFAULT_COMMAND_DEBUG_TEST_ATTACK_NAME = "test_attack";
    public static final List<String> DEFAULT_COMMAND_DEBUG_TEST_ATTACK_ALIASES = List.of();
    public static final String DEFAULT_COMMAND_DEBUG_TEST_ATTACK_PERMISSION = "ascendant.debug.test_attack";
    public static final String DEFAULT_COMMAND_DEBUG_TEST_DAMAGE_NAME = "test_damage";
    public static final List<String> DEFAULT_COMMAND_DEBUG_TEST_DAMAGE_ALIASES = List.of();
    public static final String DEFAULT_COMMAND_DEBUG_TEST_DAMAGE_PERMISSION = "ascendant.debug.test_damage";
    public static final String DEFAULT_COMMAND_DEBUG_SPAWN_WRAITH_NAME = "spawn_wraith";
    public static final List<String> DEFAULT_COMMAND_DEBUG_SPAWN_WRAITH_ALIASES = List.of();
    public static final String DEFAULT_COMMAND_DEBUG_SPAWN_WRAITH_PERMISSION = "ascendant.debug.spawn_wraith";
    public static final String DEFAULT_COMMAND_DEBUG_TIER_LOWEST_NAME = "tier_lowest";
    public static final List<String> DEFAULT_COMMAND_DEBUG_TIER_LOWEST_ALIASES = List.of();
    public static final String DEFAULT_COMMAND_DEBUG_TIER_LOWEST_PERMISSION = "ascendant.debug.tier_lowest";
    public static final String DEFAULT_COMMAND_DEBUG_TIER_HIGHEST_NAME = "tier_highest";
    public static final List<String> DEFAULT_COMMAND_DEBUG_TIER_HIGHEST_ALIASES = List.of();
    public static final String DEFAULT_COMMAND_DEBUG_TIER_HIGHEST_PERMISSION = "ascendant.debug.tier_highest";
    public static final String DEFAULT_KILLFEED_CHAT_COLOR_PLAYER = "#FFF2A0";
    public static final String DEFAULT_KILLFEED_CHAT_COLOR_MIDDLE = "#FFFFFF";
    public static final String DEFAULT_KILLFEED_CHAT_COLOR_CAUSE = "#FF2A2A";
    public static final List<String> DEFAULT_KILLFEED_CHAT_MESSAGES_FALLBACK =
            List.of(" died", " perished", " met their end");
    public static final List<String> DEFAULT_KILLFEED_CHAT_MESSAGES_COMMAND =
            List.of(
                    " died by the will of the goddess",
                    " killed by an eternal existence",
                    " was smitten by higher powers",
                    " was ended by a command"
            );
    public static final List<String> DEFAULT_KILLFEED_CHAT_MESSAGES_DROWNING =
            List.of(" drowned", " ran out of air", " sank beneath the waves", " couldn't keep afloat");
    public static final List<String> DEFAULT_KILLFEED_CHAT_MESSAGES_SUFFOCATION =
            List.of(" failed to breath", " couldn't breathe", " was buried alive", " suffocated");
    public static final List<String> DEFAULT_KILLFEED_CHAT_MESSAGES_ENVIRONMENT =
            List.of(" was killed by environment", " was claimed by the elements", " met the environment", " was consumed by nature");
    public static final List<String> DEFAULT_KILLFEED_CHAT_MESSAGES_FALL =
            List.of(" land to hard", " tried to defeat gravity", " hit ground way to hard", " fell too far", " forgot how to land", " cratered");
    public static final List<String> DEFAULT_KILLFEED_CHAT_MESSAGES_OUT_OF_WORLD =
            List.of(" doesn't want to live anymore", " fell out of world", " slipped into the void", " left the world behind");
    public static final List<String> DEFAULT_KILLFEED_CHAT_MESSAGES_PHYSICAL_KILLER_ACTION =
            List.of(" killed ", " eliminated ", " defeated ", " took down ", " slew ");
    public static final List<String> DEFAULT_KILLFEED_CHAT_MESSAGES_PHYSICAL_KILLER_CAUSE =
            List.of(" by ", " using ", " with ", " through ");
    public static final List<String> DEFAULT_KILLFEED_CHAT_MESSAGES_PHYSICAL_VICTIM_CAUSE =
            List.of(" was killed by ", " was defeated by ", " was taken down by ", " was slain by ");
    public static final List<String> DEFAULT_KILLFEED_CHAT_MESSAGES_PHYSICAL_VICTIM_DIED =
            List.of(" died", " perished", " met their end");
    public static final boolean DEFAULT_ALLOW_DEBUG_COMMANDS = true;
    public static final boolean DEFAULT_ALLOW_ELITE_SPAWN_MODIFIER = true;
    public static final double DEFAULT_ELITE_SPAWN_QUEUE_INTERVAL_MS = 0.0;
    public static final int DEFAULT_ELITE_SPAWN_QUEUE_MAX_PER_DRAIN = 2;
    public static final double DEFAULT_ELITE_SPAWN_QUEUE_MAX_DRAIN_MS = 4.0;
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
    public static final ConfigKey<Boolean> ALLOW_DIFFICULTY_CHANGE_IN_COMBAT =
            ConfigKey.ofBoolean(PATH_ALLOW_DIFFICULTY_CHANGE_IN_COMBAT, DEFAULT_ALLOW_DIFFICULTY_CHANGE_IN_COMBAT);
    public static final ConfigKey<Boolean> ALLOW_BADGE =
            ConfigKey.ofBoolean(PATH_ALLOW_BADGE, DEFAULT_ALLOW_BADGE);
    public static final ConfigKey<Double> UI_BADGE_START_DELAY_MS =
            ConfigKey.ofDouble(PATH_UI_BADGE_START_DELAY_MS, DEFAULT_UI_BADGE_START_DELAY_MS);
    public static final ConfigKey<Boolean> UI_TIER_VALUES_AS_PERCENT =
            ConfigKey.ofBoolean(PATH_UI_TIER_VALUES_AS_PERCENT, DEFAULT_UI_TIER_VALUES_AS_PERCENT);
    public static final ConfigKey<Double> DIFFICULTY_CHANGE_COOLDOWN_MS =
            ConfigKey.ofDouble(PATH_DIFFICULTY_CHANGE_COOLDOWN_MS, DEFAULT_DIFFICULTY_CHANGE_COOLDOWN_MS);
    public static final ConfigKey<Double> DIFFICULTY_CHANGE_COMBAT_TIMEOUT_MS =
            ConfigKey.ofDouble(PATH_DIFFICULTY_CHANGE_COMBAT_TIMEOUT_MS, DEFAULT_DIFFICULTY_CHANGE_COMBAT_TIMEOUT_MS);
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
    public static final ConfigKey<Boolean> ALLOW_SPAWN_TIER_REWARD =
            ConfigKey.ofBoolean(PATH_ALLOW_SPAWN_TIER_REWARD, DEFAULT_ALLOW_SPAWN_TIER_REWARD);
    public static final ConfigKey<Double> SPAWN_TIER_REWARD_OVER_FACTOR =
            ConfigKey.ofDouble(PATH_SPAWN_TIER_REWARD_OVER_FACTOR, DEFAULT_SPAWN_TIER_REWARD_OVER_FACTOR);
    public static final ConfigKey<Double> SPAWN_TIER_REWARD_UNDER_FACTOR =
            ConfigKey.ofDouble(PATH_SPAWN_TIER_REWARD_UNDER_FACTOR, DEFAULT_SPAWN_TIER_REWARD_UNDER_FACTOR);
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
    public static final ConfigKey<Boolean> ALLOW_DAMAGE_PHYSICAL =
            ConfigKey.ofBoolean(PATH_ALLOW_DAMAGE_PHYSICAL, DEFAULT_ALLOW_DAMAGE_PHYSICAL);
    public static final ConfigKey<Boolean> ALLOW_DAMAGE_PROJECTILE =
            ConfigKey.ofBoolean(PATH_ALLOW_DAMAGE_PROJECTILE, DEFAULT_ALLOW_DAMAGE_PROJECTILE);
    public static final ConfigKey<Boolean> ALLOW_DAMAGE_COMMAND =
            ConfigKey.ofBoolean(PATH_ALLOW_DAMAGE_COMMAND, DEFAULT_ALLOW_DAMAGE_COMMAND);
    public static final ConfigKey<Boolean> ALLOW_DAMAGE_DROWNING =
            ConfigKey.ofBoolean(PATH_ALLOW_DAMAGE_DROWNING, DEFAULT_ALLOW_DAMAGE_DROWNING);
    public static final ConfigKey<Boolean> ALLOW_DAMAGE_ENVIRONMENT =
            ConfigKey.ofBoolean(PATH_ALLOW_DAMAGE_ENVIRONMENT, DEFAULT_ALLOW_DAMAGE_ENVIRONMENT);
    public static final ConfigKey<Boolean> ALLOW_DAMAGE_FALL =
            ConfigKey.ofBoolean(PATH_ALLOW_DAMAGE_FALL, DEFAULT_ALLOW_DAMAGE_FALL);
    public static final ConfigKey<Boolean> ALLOW_DAMAGE_OUT_OF_WORLD =
            ConfigKey.ofBoolean(PATH_ALLOW_DAMAGE_OUT_OF_WORLD, DEFAULT_ALLOW_DAMAGE_OUT_OF_WORLD);
    public static final ConfigKey<Boolean> ALLOW_DAMAGE_SUFFOCATION =
            ConfigKey.ofBoolean(PATH_ALLOW_DAMAGE_SUFFOCATION, DEFAULT_ALLOW_DAMAGE_SUFFOCATION);
    public static final ConfigKey<Boolean> ALLOW_ARMOR_MODIFIER =
            ConfigKey.ofBoolean(PATH_ALLOW_ARMOR_MODIFIER, DEFAULT_ALLOW_ARMOR_MODIFIER);
    public static final ConfigKey<Boolean> ALLOW_DROP_MODIFIER =
            ConfigKey.ofBoolean(PATH_ALLOW_DROP_MODIFIER, DEFAULT_ALLOW_DROP_MODIFIER);
    public static final ConfigKey<Boolean> ALLOW_DEBUG_LOGGING =
            ConfigKey.ofBoolean(PATH_ALLOW_DEBUG_LOGGING, DEFAULT_ALLOW_DEBUG_LOGGING);
    public static final ConfigKey<Boolean> ALLOW_SPAWN_TIER_NAMEPLATE =
            ConfigKey.ofBoolean(PATH_ALLOW_SPAWN_TIER_NAMEPLATE, DEFAULT_ALLOW_SPAWN_TIER_NAMEPLATE);
    public static final ConfigKey<Boolean> ALLOW_KILLFEED_TIER_TAG =
            ConfigKey.ofBoolean(PATH_ALLOW_KILLFEED_TIER_TAG, DEFAULT_ALLOW_KILLFEED_TIER_TAG);
    public static final ConfigKey<Boolean> ALLOW_KILLFEED_TIER_CHAT =
            ConfigKey.ofBoolean(PATH_ALLOW_KILLFEED_TIER_CHAT, DEFAULT_ALLOW_KILLFEED_TIER_CHAT);
    public static final ConfigKey<Boolean> ALLOW_CHAT_TIER_TAG =
            ConfigKey.ofBoolean(PATH_ALLOW_CHAT_TIER_TAG, DEFAULT_ALLOW_CHAT_TIER_TAG);
    public static final ConfigKey<Boolean> ALLOW_SERVERLIST_TIER_TAG =
            ConfigKey.ofBoolean(PATH_ALLOW_SERVERLIST_TIER_TAG, DEFAULT_ALLOW_SERVERLIST_TIER_TAG);
    public static final ConfigKey<String> COMMAND_TIER_SELECT_NAME =
            ConfigKey.ofString(PATH_COMMAND_TIER_SELECT_NAME, DEFAULT_COMMAND_TIER_SELECT_NAME);
    public static final ConfigKey<List<String>> COMMAND_TIER_SELECT_ALIASES =
            ConfigKey.ofStringList(PATH_COMMAND_TIER_SELECT_ALIASES, DEFAULT_COMMAND_TIER_SELECT_ALIASES);
    public static final ConfigKey<String> COMMAND_TIER_SELECT_PERMISSION =
            ConfigKey.ofString(PATH_COMMAND_TIER_SELECT_PERMISSION, DEFAULT_COMMAND_TIER_SELECT_PERMISSION);
    public static final ConfigKey<String> COMMAND_BADGE_TOGGLE_NAME =
            ConfigKey.ofString(PATH_COMMAND_BADGE_TOGGLE_NAME, DEFAULT_COMMAND_BADGE_TOGGLE_NAME);
    public static final ConfigKey<List<String>> COMMAND_BADGE_TOGGLE_ALIASES =
            ConfigKey.ofStringList(PATH_COMMAND_BADGE_TOGGLE_ALIASES, DEFAULT_COMMAND_BADGE_TOGGLE_ALIASES);
    public static final ConfigKey<String> COMMAND_BADGE_TOGGLE_PERMISSION =
            ConfigKey.ofString(PATH_COMMAND_BADGE_TOGGLE_PERMISSION, DEFAULT_COMMAND_BADGE_TOGGLE_PERMISSION);
    public static final ConfigKey<String> COMMAND_RELOAD_NAME =
            ConfigKey.ofString(PATH_COMMAND_RELOAD_NAME, DEFAULT_COMMAND_RELOAD_NAME);
    public static final ConfigKey<List<String>> COMMAND_RELOAD_ALIASES =
            ConfigKey.ofStringList(PATH_COMMAND_RELOAD_ALIASES, DEFAULT_COMMAND_RELOAD_ALIASES);
    public static final ConfigKey<String> COMMAND_RELOAD_PERMISSION =
            ConfigKey.ofString(PATH_COMMAND_RELOAD_PERMISSION, DEFAULT_COMMAND_RELOAD_PERMISSION);
    public static final ConfigKey<String> COMMAND_DEBUG_CLEAR_ENTITIES_NAME =
            ConfigKey.ofString(PATH_COMMAND_DEBUG_CLEAR_ENTITIES_NAME, DEFAULT_COMMAND_DEBUG_CLEAR_ENTITIES_NAME);
    public static final ConfigKey<List<String>> COMMAND_DEBUG_CLEAR_ENTITIES_ALIASES =
            ConfigKey.ofStringList(PATH_COMMAND_DEBUG_CLEAR_ENTITIES_ALIASES, DEFAULT_COMMAND_DEBUG_CLEAR_ENTITIES_ALIASES);
    public static final ConfigKey<String> COMMAND_DEBUG_CLEAR_ENTITIES_PERMISSION =
            ConfigKey.ofString(PATH_COMMAND_DEBUG_CLEAR_ENTITIES_PERMISSION, DEFAULT_COMMAND_DEBUG_CLEAR_ENTITIES_PERMISSION);
    public static final ConfigKey<String> COMMAND_DEBUG_CLEAR_ITEMS_NAME =
            ConfigKey.ofString(PATH_COMMAND_DEBUG_CLEAR_ITEMS_NAME, DEFAULT_COMMAND_DEBUG_CLEAR_ITEMS_NAME);
    public static final ConfigKey<List<String>> COMMAND_DEBUG_CLEAR_ITEMS_ALIASES =
            ConfigKey.ofStringList(PATH_COMMAND_DEBUG_CLEAR_ITEMS_ALIASES, DEFAULT_COMMAND_DEBUG_CLEAR_ITEMS_ALIASES);
    public static final ConfigKey<String> COMMAND_DEBUG_CLEAR_ITEMS_PERMISSION =
            ConfigKey.ofString(PATH_COMMAND_DEBUG_CLEAR_ITEMS_PERMISSION, DEFAULT_COMMAND_DEBUG_CLEAR_ITEMS_PERMISSION);
    public static final ConfigKey<String> COMMAND_DEBUG_TEST_ATTACK_NAME =
            ConfigKey.ofString(PATH_COMMAND_DEBUG_TEST_ATTACK_NAME, DEFAULT_COMMAND_DEBUG_TEST_ATTACK_NAME);
    public static final ConfigKey<List<String>> COMMAND_DEBUG_TEST_ATTACK_ALIASES =
            ConfigKey.ofStringList(PATH_COMMAND_DEBUG_TEST_ATTACK_ALIASES, DEFAULT_COMMAND_DEBUG_TEST_ATTACK_ALIASES);
    public static final ConfigKey<String> COMMAND_DEBUG_TEST_ATTACK_PERMISSION =
            ConfigKey.ofString(PATH_COMMAND_DEBUG_TEST_ATTACK_PERMISSION, DEFAULT_COMMAND_DEBUG_TEST_ATTACK_PERMISSION);
    public static final ConfigKey<String> COMMAND_DEBUG_TEST_DAMAGE_NAME =
            ConfigKey.ofString(PATH_COMMAND_DEBUG_TEST_DAMAGE_NAME, DEFAULT_COMMAND_DEBUG_TEST_DAMAGE_NAME);
    public static final ConfigKey<List<String>> COMMAND_DEBUG_TEST_DAMAGE_ALIASES =
            ConfigKey.ofStringList(PATH_COMMAND_DEBUG_TEST_DAMAGE_ALIASES, DEFAULT_COMMAND_DEBUG_TEST_DAMAGE_ALIASES);
    public static final ConfigKey<String> COMMAND_DEBUG_TEST_DAMAGE_PERMISSION =
            ConfigKey.ofString(PATH_COMMAND_DEBUG_TEST_DAMAGE_PERMISSION, DEFAULT_COMMAND_DEBUG_TEST_DAMAGE_PERMISSION);
    public static final ConfigKey<String> COMMAND_DEBUG_SPAWN_WRAITH_NAME =
            ConfigKey.ofString(PATH_COMMAND_DEBUG_SPAWN_WRAITH_NAME, DEFAULT_COMMAND_DEBUG_SPAWN_WRAITH_NAME);
    public static final ConfigKey<List<String>> COMMAND_DEBUG_SPAWN_WRAITH_ALIASES =
            ConfigKey.ofStringList(PATH_COMMAND_DEBUG_SPAWN_WRAITH_ALIASES, DEFAULT_COMMAND_DEBUG_SPAWN_WRAITH_ALIASES);
    public static final ConfigKey<String> COMMAND_DEBUG_SPAWN_WRAITH_PERMISSION =
            ConfigKey.ofString(PATH_COMMAND_DEBUG_SPAWN_WRAITH_PERMISSION, DEFAULT_COMMAND_DEBUG_SPAWN_WRAITH_PERMISSION);
    public static final ConfigKey<String> COMMAND_DEBUG_TIER_LOWEST_NAME =
            ConfigKey.ofString(PATH_COMMAND_DEBUG_TIER_LOWEST_NAME, DEFAULT_COMMAND_DEBUG_TIER_LOWEST_NAME);
    public static final ConfigKey<List<String>> COMMAND_DEBUG_TIER_LOWEST_ALIASES =
            ConfigKey.ofStringList(PATH_COMMAND_DEBUG_TIER_LOWEST_ALIASES, DEFAULT_COMMAND_DEBUG_TIER_LOWEST_ALIASES);
    public static final ConfigKey<String> COMMAND_DEBUG_TIER_LOWEST_PERMISSION =
            ConfigKey.ofString(PATH_COMMAND_DEBUG_TIER_LOWEST_PERMISSION, DEFAULT_COMMAND_DEBUG_TIER_LOWEST_PERMISSION);
    public static final ConfigKey<String> COMMAND_DEBUG_TIER_HIGHEST_NAME =
            ConfigKey.ofString(PATH_COMMAND_DEBUG_TIER_HIGHEST_NAME, DEFAULT_COMMAND_DEBUG_TIER_HIGHEST_NAME);
    public static final ConfigKey<List<String>> COMMAND_DEBUG_TIER_HIGHEST_ALIASES =
            ConfigKey.ofStringList(PATH_COMMAND_DEBUG_TIER_HIGHEST_ALIASES, DEFAULT_COMMAND_DEBUG_TIER_HIGHEST_ALIASES);
    public static final ConfigKey<String> COMMAND_DEBUG_TIER_HIGHEST_PERMISSION =
            ConfigKey.ofString(PATH_COMMAND_DEBUG_TIER_HIGHEST_PERMISSION, DEFAULT_COMMAND_DEBUG_TIER_HIGHEST_PERMISSION);
    public static final ConfigKey<String> KILLFEED_CHAT_COLOR_PLAYER =
            ConfigKey.ofString(PATH_KILLFEED_CHAT_COLOR_PLAYER, DEFAULT_KILLFEED_CHAT_COLOR_PLAYER);
    public static final ConfigKey<String> KILLFEED_CHAT_COLOR_MIDDLE =
            ConfigKey.ofString(PATH_KILLFEED_CHAT_COLOR_MIDDLE, DEFAULT_KILLFEED_CHAT_COLOR_MIDDLE);
    public static final ConfigKey<String> KILLFEED_CHAT_COLOR_CAUSE =
            ConfigKey.ofString(PATH_KILLFEED_CHAT_COLOR_CAUSE, DEFAULT_KILLFEED_CHAT_COLOR_CAUSE);
    public static final ConfigKey<List<String>> KILLFEED_CHAT_MESSAGES_FALLBACK =
            ConfigKey.ofStringList(PATH_KILLFEED_CHAT_MESSAGES_FALLBACK, DEFAULT_KILLFEED_CHAT_MESSAGES_FALLBACK);
    public static final ConfigKey<List<String>> KILLFEED_CHAT_MESSAGES_COMMAND =
            ConfigKey.ofStringList(PATH_KILLFEED_CHAT_MESSAGES_COMMAND, DEFAULT_KILLFEED_CHAT_MESSAGES_COMMAND);
    public static final ConfigKey<List<String>> KILLFEED_CHAT_MESSAGES_DROWNING =
            ConfigKey.ofStringList(PATH_KILLFEED_CHAT_MESSAGES_DROWNING, DEFAULT_KILLFEED_CHAT_MESSAGES_DROWNING);
    public static final ConfigKey<List<String>> KILLFEED_CHAT_MESSAGES_SUFFOCATION =
            ConfigKey.ofStringList(PATH_KILLFEED_CHAT_MESSAGES_SUFFOCATION, DEFAULT_KILLFEED_CHAT_MESSAGES_SUFFOCATION);
    public static final ConfigKey<List<String>> KILLFEED_CHAT_MESSAGES_ENVIRONMENT =
            ConfigKey.ofStringList(PATH_KILLFEED_CHAT_MESSAGES_ENVIRONMENT, DEFAULT_KILLFEED_CHAT_MESSAGES_ENVIRONMENT);
    public static final ConfigKey<List<String>> KILLFEED_CHAT_MESSAGES_FALL =
            ConfigKey.ofStringList(PATH_KILLFEED_CHAT_MESSAGES_FALL, DEFAULT_KILLFEED_CHAT_MESSAGES_FALL);
    public static final ConfigKey<List<String>> KILLFEED_CHAT_MESSAGES_OUT_OF_WORLD =
            ConfigKey.ofStringList(PATH_KILLFEED_CHAT_MESSAGES_OUT_OF_WORLD, DEFAULT_KILLFEED_CHAT_MESSAGES_OUT_OF_WORLD);
    public static final ConfigKey<List<String>> KILLFEED_CHAT_MESSAGES_PHYSICAL_KILLER_ACTION =
            ConfigKey.ofStringList(PATH_KILLFEED_CHAT_MESSAGES_PHYSICAL_KILLER_ACTION, DEFAULT_KILLFEED_CHAT_MESSAGES_PHYSICAL_KILLER_ACTION);
    public static final ConfigKey<List<String>> KILLFEED_CHAT_MESSAGES_PHYSICAL_KILLER_CAUSE =
            ConfigKey.ofStringList(PATH_KILLFEED_CHAT_MESSAGES_PHYSICAL_KILLER_CAUSE, DEFAULT_KILLFEED_CHAT_MESSAGES_PHYSICAL_KILLER_CAUSE);
    public static final ConfigKey<List<String>> KILLFEED_CHAT_MESSAGES_PHYSICAL_VICTIM_CAUSE =
            ConfigKey.ofStringList(PATH_KILLFEED_CHAT_MESSAGES_PHYSICAL_VICTIM_CAUSE, DEFAULT_KILLFEED_CHAT_MESSAGES_PHYSICAL_VICTIM_CAUSE);
    public static final ConfigKey<List<String>> KILLFEED_CHAT_MESSAGES_PHYSICAL_VICTIM_DIED =
            ConfigKey.ofStringList(PATH_KILLFEED_CHAT_MESSAGES_PHYSICAL_VICTIM_DIED, DEFAULT_KILLFEED_CHAT_MESSAGES_PHYSICAL_VICTIM_DIED);
    public static final ConfigKey<Boolean> ALLOW_DEBUG_COMMANDS =
            ConfigKey.ofBoolean(PATH_ALLOW_DEBUG_COMMANDS, DEFAULT_ALLOW_DEBUG_COMMANDS);
    public static final ConfigKey<Boolean> ALLOW_ELITE_SPAWN_MODIFIER =
            ConfigKey.ofBoolean(PATH_ALLOW_ELITE_SPAWN_MODIFIER, DEFAULT_ALLOW_ELITE_SPAWN_MODIFIER);
    public static final ConfigKey<Double> ELITE_SPAWN_QUEUE_INTERVAL_MS =
            ConfigKey.ofDouble(PATH_ELITE_SPAWN_QUEUE_INTERVAL_MS, DEFAULT_ELITE_SPAWN_QUEUE_INTERVAL_MS);
    public static final ConfigKey<Integer> ELITE_SPAWN_QUEUE_MAX_PER_DRAIN =
            ConfigKey.ofInt(PATH_ELITE_SPAWN_QUEUE_MAX_PER_DRAIN, DEFAULT_ELITE_SPAWN_QUEUE_MAX_PER_DRAIN);
    public static final ConfigKey<Double> ELITE_SPAWN_QUEUE_MAX_DRAIN_MS =
            ConfigKey.ofDouble(PATH_ELITE_SPAWN_QUEUE_MAX_DRAIN_MS, DEFAULT_ELITE_SPAWN_QUEUE_MAX_DRAIN_MS);
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
        if (Files.notExists(DEFAULT_CONFIG_PATH)) {
            JsonObject baseDefaults = defaults.deepCopy();
            baseDefaults.remove("meta");
            baseDefaults.remove("tiers");
            DifficultyConfig.loadOrCreate(DEFAULT_CONFIG_PATH, baseDefaults);
            if (!DifficultyDropIns.writeDefaultsFromResources(DIFFICULTY_DROPINS_PATH)) {
                DifficultyDropIns.writeDefaultsFromLegacy(defaults, DIFFICULTY_DROPINS_PATH);
            }
            return DifficultyConfig.loadWithDropIns(DEFAULT_CONFIG_PATH, DIFFICULTY_DROPINS_PATH);
        }
        if (!DifficultyDropIns.hasJsonFiles(DIFFICULTY_DROPINS_PATH)) {
            JsonObject legacyRoot = readRoot(DEFAULT_CONFIG_PATH);
            if (legacyRoot != null && (legacyRoot.has("tiers") || legacyRoot.has("meta"))) {
                DifficultyDropIns.writeDefaultsFromLegacy(legacyRoot, DIFFICULTY_DROPINS_PATH);
                stripLegacySections(DEFAULT_CONFIG_PATH, legacyRoot);
            }
            if (!DifficultyDropIns.hasJsonFiles(DIFFICULTY_DROPINS_PATH)) {
                DifficultyDropIns.writeDefaultsFromResources(DIFFICULTY_DROPINS_PATH);
            }
        }
        return DifficultyConfig.loadOrCreateWithDropIns(DEFAULT_CONFIG_PATH, DIFFICULTY_DROPINS_PATH, defaults);
    }

    private static void stripLegacySections(Path path, JsonObject root) {
        if (path == null || root == null) {
            return;
        }
        boolean changed = false;
        if (root.has("meta")) {
            root.remove("meta");
            changed = true;
        }
        if (root.has("tiers")) {
            root.remove("tiers");
            changed = true;
        }
        if (!changed) {
            return;
        }
        try {
            Files.writeString(path, root.toString(), StandardCharsets.UTF_8);
        } catch (IOException ignored) {
        }
    }

    private static JsonObject readRoot(Path path) {
        if (path == null || Files.notExists(path)) {
            return null;
        }
        try (Reader reader = Files.newBufferedReader(path, StandardCharsets.UTF_8)) {
            JsonElement parsed = JsonParser.parseReader(reader);
            if (parsed != null && parsed.isJsonObject()) {
                return parsed.getAsJsonObject();
            }
        } catch (IOException ignored) {
        }
        return null;
    }
}
