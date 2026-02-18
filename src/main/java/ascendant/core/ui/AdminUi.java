package ascendant.core.ui;

import ascendant.core.config.DifficultyAdminConfigEditor;
import ascendant.core.config.DifficultyIO;
import ascendant.core.config.DifficultyManager;
import ascendant.core.config.DifficultyMeta;
import ascendant.core.config.DifficultySettings;
import ascendant.core.util.EventNotificationWrapper;
import ascendant.core.util.WorldTierUiSync;
import au.ellie.hyui.builders.PageBuilder;
import au.ellie.hyui.html.TemplateProcessor;
import com.hypixel.hytale.component.Store;
import com.hypixel.hytale.protocol.packets.interface_.CustomPageLifetime;
import com.hypixel.hytale.protocol.packets.interface_.CustomUIEventBindingType;
import com.hypixel.hytale.server.core.command.system.CommandContext;
import com.hypixel.hytale.server.core.universe.PlayerRef;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;
import org.checkerframework.checker.nullness.compatqual.NonNullDecl;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

public final class AdminUi {
    private static final String PAGE_HTML = "Pages/DifficultyAdmin.html";
    private static final int ROWS_PER_PAGE = 15;
    private static final List<String> WORLD_MODES = List.of("fixed", "highest", "lowest", "scaled");

    private static final Map<UUID, UiState> uiStateByPlayer = new ConcurrentHashMap<>();

    private static final List<ToggleSettingDefinition> TOGGLE_SETTINGS = List.of(
            new ToggleSettingDefinition("difficulty_change", "Allow difficulty change", DifficultyIO.PATH_ALLOW_DIFFICULTY_CHANGE, DifficultyIO.DEFAULT_ALLOW_DIFFICULTY_CHANGE),
            new ToggleSettingDefinition("difficulty_change_combat", "Allow change in combat", DifficultyIO.PATH_ALLOW_DIFFICULTY_CHANGE_IN_COMBAT, DifficultyIO.DEFAULT_ALLOW_DIFFICULTY_CHANGE_IN_COMBAT),
            new ToggleSettingDefinition("ui_badge", "Allow badge UI", DifficultyIO.PATH_ALLOW_BADGE, DifficultyIO.DEFAULT_ALLOW_BADGE),
            new ToggleSettingDefinition("reward_xp", "Enable XP rewards", DifficultyIO.PATH_ALLOW_XP_REWARD, DifficultyIO.DEFAULT_ALLOW_XP_REWARD),
            new ToggleSettingDefinition("reward_cash", "Enable cash rewards", DifficultyIO.PATH_ALLOW_CASH_REWARD, DifficultyIO.DEFAULT_ALLOW_CASH_REWARD),
            new ToggleSettingDefinition("reward_cash_physical", "Cash reward with physical damage", DifficultyIO.PATH_ALLOW_CASH_REWARD_EVEN_WITH_PHYSICAL, DifficultyIO.DEFAULT_ALLOW_CASH_REWARD_EVEN_WITH_PHYSICAL),
            new ToggleSettingDefinition("reward_spawn_tier", "Enable spawn-tier reward scaling", DifficultyIO.PATH_ALLOW_SPAWN_TIER_REWARD, DifficultyIO.DEFAULT_ALLOW_SPAWN_TIER_REWARD),
            new ToggleSettingDefinition("modifier_health", "Enable health modifier", DifficultyIO.PATH_ALLOW_HEALTH_MODIFIER, DifficultyIO.DEFAULT_ALLOW_HEALTH_MODIFIER),
            new ToggleSettingDefinition("modifier_damage", "Enable damage modifier", DifficultyIO.PATH_ALLOW_DAMAGE_MODIFIER, DifficultyIO.DEFAULT_ALLOW_DAMAGE_MODIFIER),
            new ToggleSettingDefinition("modifier_armor", "Enable armor modifier", DifficultyIO.PATH_ALLOW_ARMOR_MODIFIER, DifficultyIO.DEFAULT_ALLOW_ARMOR_MODIFIER),
            new ToggleSettingDefinition("modifier_drop", "Enable drop modifier", DifficultyIO.PATH_ALLOW_DROP_MODIFIER, DifficultyIO.DEFAULT_ALLOW_DROP_MODIFIER),
            new ToggleSettingDefinition("spawn_count", "Enable spawn count multiplier", DifficultyIO.PATH_ALLOW_SPAWN_COUNT_MULTIPLIER, DifficultyIO.DEFAULT_ALLOW_SPAWN_COUNT_MULTIPLIER),
            new ToggleSettingDefinition("spawn_elite", "Enable elite spawn modifier", DifficultyIO.PATH_ALLOW_ELITE_SPAWN_MODIFIER, DifficultyIO.DEFAULT_ALLOW_ELITE_SPAWN_MODIFIER),
            new ToggleSettingDefinition("spawn_nameplate", "Enable spawn tier nameplate", DifficultyIO.PATH_ALLOW_SPAWN_TIER_NAMEPLATE, DifficultyIO.DEFAULT_ALLOW_SPAWN_TIER_NAMEPLATE),
            new ToggleSettingDefinition("tag_killfeed", "Enable killfeed tier tag", DifficultyIO.PATH_ALLOW_KILLFEED_TIER_TAG, DifficultyIO.DEFAULT_ALLOW_KILLFEED_TIER_TAG),
            new ToggleSettingDefinition("tag_killfeed_chat", "Enable killfeed tier chat", DifficultyIO.PATH_ALLOW_KILLFEED_TIER_CHAT, DifficultyIO.DEFAULT_ALLOW_KILLFEED_TIER_CHAT),
            new ToggleSettingDefinition("tag_chat", "Enable chat tier tag", DifficultyIO.PATH_ALLOW_CHAT_TIER_TAG, DifficultyIO.DEFAULT_ALLOW_CHAT_TIER_TAG),
            new ToggleSettingDefinition("tag_serverlist", "Enable server-list tier tag", DifficultyIO.PATH_ALLOW_SERVERLIST_TIER_TAG, DifficultyIO.DEFAULT_ALLOW_SERVERLIST_TIER_TAG),
            new ToggleSettingDefinition("debug_commands", "Enable debug commands (reload required)", DifficultyIO.PATH_ALLOW_DEBUG_COMMANDS, DifficultyIO.DEFAULT_ALLOW_DEBUG_COMMANDS),
            new ToggleSettingDefinition("debug_logging", "Enable debug logging", DifficultyIO.PATH_ALLOW_DEBUG_LOGGING, DifficultyIO.DEFAULT_ALLOW_DEBUG_LOGGING),
            new ToggleSettingDefinition("integration_elitemobs", "Enable EliteMobs integration", DifficultyIO.PATH_INTEGRATION_ELITE_MOBS, DifficultyIO.DEFAULT_INTEGRATION_ELITE_MOBS),
            new ToggleSettingDefinition("integration_levelingcore", "Enable LevelingCore integration", DifficultyIO.PATH_INTEGRATION_LEVELING_CORE, DifficultyIO.DEFAULT_INTEGRATION_LEVELING_CORE),
            new ToggleSettingDefinition("integration_mmoskilltree", "Enable MMOSkillTree integration", DifficultyIO.PATH_INTEGRATION_MMO_SKILLTREE, DifficultyIO.DEFAULT_INTEGRATION_MMO_SKILLTREE),
            new ToggleSettingDefinition("integration_ecotale", "Enable Ecotale integration", DifficultyIO.PATH_INTEGRATION_ECOTALE, DifficultyIO.DEFAULT_INTEGRATION_ECOTALE)
    );

    private static final List<NumberSettingDefinition> NUMBER_SETTINGS = List.of(
            new NumberSettingDefinition("badge_delay", "Badge start delay (ms)", DifficultyIO.PATH_UI_BADGE_START_DELAY_MS, 100.0, 0.0, 20_000.0, true, DifficultyIO.DEFAULT_UI_BADGE_START_DELAY_MS),
            new NumberSettingDefinition("difficulty_cooldown", "Difficulty cooldown (ms)", DifficultyIO.PATH_DIFFICULTY_CHANGE_COOLDOWN_MS, 500.0, 0.0, 600_000.0, true, DifficultyIO.DEFAULT_DIFFICULTY_CHANGE_COOLDOWN_MS),
            new NumberSettingDefinition("difficulty_combat_timeout", "Combat timeout (ms)", DifficultyIO.PATH_DIFFICULTY_CHANGE_COMBAT_TIMEOUT_MS, 500.0, 0.0, 120_000.0, true, DifficultyIO.DEFAULT_DIFFICULTY_CHANGE_COMBAT_TIMEOUT_MS),
            new NumberSettingDefinition("player_radius", "Player distance radius", DifficultyIO.PATH_PLAYER_DISTANCE_RADIUS_TO_CHECK, 1.0, 1.0, 2048.0, false, DifficultyIO.DEFAULT_PLAYER_DISTANCE_RADIUS_TO_CHECK),
            new NumberSettingDefinition("min_damage_factor", "Minimum damage factor", DifficultyIO.PATH_MIN_DAMAGE_FACTOR, 0.001, 0.0, 1.0, false, DifficultyIO.DEFAULT_MIN_DAMAGE_FACTOR),
            new NumberSettingDefinition("min_health_scaling", "Minimum health scaling", DifficultyIO.PATH_MIN_HEALTH_SCALING_FACTOR, 0.05, 0.0, 500.0, false, DifficultyIO.DEFAULT_MIN_HEALTH_SCALING_FACTOR),
            new NumberSettingDefinition("max_health_scaling", "Maximum health scaling", DifficultyIO.PATH_MAX_HEALTH_SCALING_FACTOR, 1.0, 0.0, 5000.0, false, DifficultyIO.DEFAULT_MAX_HEALTH_SCALING_FACTOR),
            new NumberSettingDefinition("cash_variance", "Cash variance factor", DifficultyIO.PATH_CASH_VARIANCE_FACTOR, 0.01, 0.0, 1.0, false, DifficultyIO.DEFAULT_CASH_VARIANCE_FACTOR),
            new NumberSettingDefinition("spawn_over", "Spawn-tier reward over factor", DifficultyIO.PATH_SPAWN_TIER_REWARD_OVER_FACTOR, 0.01, 0.0, 20.0, false, DifficultyIO.DEFAULT_SPAWN_TIER_REWARD_OVER_FACTOR),
            new NumberSettingDefinition("spawn_under", "Spawn-tier reward under factor", DifficultyIO.PATH_SPAWN_TIER_REWARD_UNDER_FACTOR, 0.01, 0.0, 20.0, false, DifficultyIO.DEFAULT_SPAWN_TIER_REWARD_UNDER_FACTOR),
            new NumberSettingDefinition("elite_interval", "Elite queue interval (ms)", DifficultyIO.PATH_ELITE_SPAWN_QUEUE_INTERVAL_MS, 1.0, 0.0, 1000.0, false, DifficultyIO.DEFAULT_ELITE_SPAWN_QUEUE_INTERVAL_MS),
            new NumberSettingDefinition("elite_max_per_drain", "Elite queue max per drain", DifficultyIO.PATH_ELITE_SPAWN_QUEUE_MAX_PER_DRAIN, 1.0, 0.0, 128.0, true, DifficultyIO.DEFAULT_ELITE_SPAWN_QUEUE_MAX_PER_DRAIN),
            new NumberSettingDefinition("elite_max_drain_ms", "Elite queue max drain (ms)", DifficultyIO.PATH_ELITE_SPAWN_QUEUE_MAX_DRAIN_MS, 0.5, 0.0, 500.0, false, DifficultyIO.DEFAULT_ELITE_SPAWN_QUEUE_MAX_DRAIN_MS),
            new NumberSettingDefinition("integration_mult_leveling", "Integration multiplier LevelingCore", DifficultyIO.PATH_INTEGRATION_MULTIPLIER_LEVELING_CORE, 0.05, 0.0, 10.0, false, DifficultyIO.DEFAULT_INTEGRATION_MULTIPLIER_LEVELING_CORE),
            new NumberSettingDefinition("integration_mult_mmo", "Integration multiplier MMOSkillTree", DifficultyIO.PATH_INTEGRATION_MULTIPLIER_MMO_SKILLTREE, 0.05, 0.0, 10.0, false, DifficultyIO.DEFAULT_INTEGRATION_MULTIPLIER_MMO_SKILLTREE),
            new NumberSettingDefinition("integration_mult_ecotale", "Integration multiplier Ecotale", DifficultyIO.PATH_INTEGRATION_MULTIPLIER_ECOTALE, 0.05, 0.0, 10.0, false, DifficultyIO.DEFAULT_INTEGRATION_MULTIPLIER_ECOTALE)
    );

    private static final List<TierNumberSettingDefinition> TIER_NUMBER_SETTINGS = List.of(
            new TierNumberSettingDefinition("health", "Health multiplier", DifficultyIO.SETTING_HEALTH_MULTIPLIER, 0.1, 0.0, 5000.0, false),
            new TierNumberSettingDefinition("damage", "Damage multiplier", DifficultyIO.SETTING_DAMAGE_MULTIPLIER, 0.05, 0.0, 5000.0, false),
            new TierNumberSettingDefinition("damage_physical", "Damage multiplier physical", DifficultyIO.SETTING_DAMAGE_MULTIPLIER_PHYSICAL, 0.05, 0.0, 5000.0, false),
            new TierNumberSettingDefinition("damage_projectile", "Damage multiplier projectile", DifficultyIO.SETTING_DAMAGE_MULTIPLIER_PROJECTILE, 0.05, 0.0, 5000.0, false),
            new TierNumberSettingDefinition("damage_command", "Damage multiplier command", DifficultyIO.SETTING_DAMAGE_MULTIPLIER_COMMAND, 0.05, 0.0, 5000.0, false),
            new TierNumberSettingDefinition("damage_drowning", "Damage multiplier drowning", DifficultyIO.SETTING_DAMAGE_MULTIPLIER_DROWNING, 0.05, 0.0, 5000.0, false),
            new TierNumberSettingDefinition("damage_environment", "Damage multiplier environment", DifficultyIO.SETTING_DAMAGE_MULTIPLIER_ENVIRONMENT, 0.05, 0.0, 5000.0, false),
            new TierNumberSettingDefinition("damage_fall", "Damage multiplier fall", DifficultyIO.SETTING_DAMAGE_MULTIPLIER_FALL, 0.05, 0.0, 5000.0, false),
            new TierNumberSettingDefinition("damage_out_of_world", "Damage multiplier out of world", DifficultyIO.SETTING_DAMAGE_MULTIPLIER_OUT_OF_WORLD, 0.05, 0.0, 5000.0, false),
            new TierNumberSettingDefinition("damage_suffocation", "Damage multiplier suffocation", DifficultyIO.SETTING_DAMAGE_MULTIPLIER_SUFFOCATION, 0.05, 0.0, 5000.0, false),
            new TierNumberSettingDefinition("armor", "Armor multiplier", DifficultyIO.SETTING_ARMOR_MULTIPLIER, 0.02, 0.0, 1.0, false),
            new TierNumberSettingDefinition("drop_rate", "Drop-rate multiplier", DifficultyIO.SETTING_DROP_RATE_MULTIPLIER, 0.05, 0.0, 1000.0, false),
            new TierNumberSettingDefinition("drop_quantity", "Drop-quantity multiplier", DifficultyIO.SETTING_DROP_QUANTITY_MULTIPLIER, 0.05, 0.0, 1000.0, false),
            new TierNumberSettingDefinition("spawn_count", "Spawn-count multiplier", DifficultyIO.SETTING_SPAWN_COUNT_MULTIPLIER, 0.05, 0.0, 1000.0, false),
            new TierNumberSettingDefinition("drop_quality", "Drop-quality multiplier", DifficultyIO.SETTING_DROP_QUALITY_MULTIPLIER, 0.02, 0.0, 1.0, false),
            new TierNumberSettingDefinition("xp", "XP multiplier", DifficultyIO.SETTING_XP_MULTIPLIER, 0.05, 0.0, 1000.0, false),
            new TierNumberSettingDefinition("cash", "Cash multiplier", DifficultyIO.SETTING_CASH_MULTIPLIER, 0.05, 0.0, 1000.0, false),
            new TierNumberSettingDefinition("elite_chance_multiplier", "Elite chance multiplier", DifficultyIO.SETTING_ELITE_MOBS_CHANCE_MULTIPLIER, 1.0, 0.0, 5000.0, true),
            new TierNumberSettingDefinition("elite_uncommon", "Elite uncommon chance", DifficultyIO.SETTING_ELITE_MOBS_CHANCE_UNCOMMON, 0.001, 0.0, 1.0, false),
            new TierNumberSettingDefinition("elite_rare", "Elite rare chance", DifficultyIO.SETTING_ELITE_MOBS_CHANCE_RARE, 0.001, 0.0, 1.0, false),
            new TierNumberSettingDefinition("elite_legendary", "Elite legendary chance", DifficultyIO.SETTING_ELITE_MOBS_CHANCE_LEGENDARY, 0.001, 0.0, 1.0, false)
    );

    private AdminUi() {
    }

    public static void openOrUpdateUi(
            @NonNullDecl PlayerRef playerRef,
            @NonNullDecl Store<EntityStore> store,
            @NonNullDecl UUID playerUuid,
            @NonNullDecl CommandContext commandContext
    ) {
        List<String> tierIds = orderedTierIds();
        if (tierIds.isEmpty()) {
            EventNotificationWrapper.sendMinorEventNotification(playerRef, commandContext, "No tiers available.");
            return;
        }

        UiState state = uiStateByPlayer.computeIfAbsent(playerUuid, ignored -> UiState.createDefault());
        clampState(state, tierIds.size());

        DifficultySettings settings = DifficultyManager.getSettings();
        String selectedTierId = tierIds.get(state.tierIndex);
        DifficultyMeta.TierMeta selectedMeta = DifficultyMeta.resolve(DifficultyManager.getConfig(), selectedTierId);
        DifficultyManager.WorldTierSnapshot worldSnapshot = DifficultyManager.getWorldTierSnapshot();

        int pageIndexMax = pageIndexMax(state.section);
        if (state.pageIndex > pageIndexMax) {
            state.pageIndex = pageIndexMax;
        }
        if (state.pageIndex < 0) {
            state.pageIndex = 0;
        }

        List<ToggleSettingDefinition> toggleDefsPage = state.section == AdminSection.TOGGLES
                ? page(TOGGLE_SETTINGS, state.pageIndex, ROWS_PER_PAGE + 3)
                : List.of();
        List<NumberSettingDefinition> numberDefsPage = state.section == AdminSection.NUMBERS
                ? page(NUMBER_SETTINGS, state.pageIndex, ROWS_PER_PAGE)
                : List.of();
        List<TierNumberSettingDefinition> tierDefsPage = state.section == AdminSection.TIERS
                ? page(TIER_NUMBER_SETTINGS, state.pageIndex, ROWS_PER_PAGE)
                : List.of();

        TemplateProcessor template = new TemplateProcessor()
                .setVariable("sectionWorld", state.section == AdminSection.WORLD)
                .setVariable("sectionToggles", state.section == AdminSection.TOGGLES)
                .setVariable("sectionNumbers", state.section == AdminSection.NUMBERS)
                .setVariable("sectionTiers", state.section == AdminSection.TIERS)
                .setVariable("sectionLabel", state.section.label)
                .setVariable("pageEnabled", state.section != AdminSection.WORLD)
                .setVariable("pageIndex", state.pageIndex)
                .setVariable("pageIndexMax", pageIndexMax)
                .setVariable("pageCurrent", state.pageIndex + 1)
                .setVariable("pageTotal", pageIndexMax + 1)
                .setVariable("selectedTierId", selectedTierId)
                .setVariable("selectedTierName", selectedMeta.displayName())
                .setVariable("selectedTierDescription", selectedMeta.description())
                .setVariable("selectedTierAllowed", settings.getBoolean(selectedTierId, DifficultyIO.SETTING_IS_ALLOWED))
                .setVariable("selectedTierHidden", settings.getBoolean(selectedTierId, DifficultyIO.SETTING_IS_HIDDEN))
                .setVariable("worldActive", worldSnapshot.active())
                .setVariable("worldMode", worldSnapshot.mode())
                .setVariable("worldResolvedTier", worldSnapshot.resolvedTier())
                .setVariable("worldFixedTier", worldSnapshot.fixedTier())
                .setVariable("worldAdminOverride", worldSnapshot.adminOverrideTier() == null ? "<none>" : worldSnapshot.adminOverrideTier())
                .setVariable("worldScaledFactor", formatDouble(worldSnapshot.scaledFactor()))
                .setVariable("worldScaledUseAllOnlinePlayers", worldSnapshot.scaledUseAllOnlinePlayers())
                .setVariable("toggleRows", buildToggleRows(toggleDefsPage))
                .setVariable("numberRows", buildNumberRows(numberDefsPage))
                .setVariable("tierRows", buildTierRows(tierDefsPage, selectedTierId));

        PageBuilder builder = PageBuilder.pageForPlayer(playerRef)
                .loadHtml(PAGE_HTML, template)
                .enableRuntimeTemplateUpdates(true)
                .withLifetime(CustomPageLifetime.CanDismiss);

        boolean pageEnabled = state.section != AdminSection.WORLD;
        registerNavigationEvents(builder, playerRef, store, playerUuid, commandContext, state, tierIds.size(), pageEnabled);
        switch (state.section) {
            case WORLD -> registerWorldEvents(builder, playerRef, store, playerUuid, commandContext, state);
            case TOGGLES -> registerToggleEvents(builder, playerRef, store, playerUuid, commandContext, toggleDefsPage);
            case NUMBERS -> registerNumberEvents(builder, playerRef, store, playerUuid, commandContext, numberDefsPage);
            case TIERS -> registerTierEvents(builder, playerRef, store, playerUuid, commandContext, state, tierDefsPage);
        }

        builder.open(store);
    }

    private static void registerNavigationEvents(
            PageBuilder builder,
            PlayerRef playerRef,
            Store<EntityStore> store,
            UUID playerUuid,
            CommandContext commandContext,
            UiState state,
            int tierCount,
            boolean pageEnabled
    ) {
        builder.addEventListener("refreshAdminUi", CustomUIEventBindingType.Activating, (ignored, _) ->
                openOrUpdateUi(playerRef, store, playerUuid, commandContext));

        builder.addEventListener("reloadConfigNow", CustomUIEventBindingType.Activating, (ignored, _) -> {
            try {
                DifficultyManager.reloadConfig();
                WorldTierUiSync.refreshAllPlayers();
                EventNotificationWrapper.sendMinorEventNotification(playerRef, commandContext, "Configuration reloaded.");
            } catch (IOException e) {
                EventNotificationWrapper.sendMinorEventNotification(playerRef, commandContext, "Reload failed: " + e.getMessage());
            }
            openOrUpdateUi(playerRef, store, playerUuid, commandContext);
        });

        builder.addEventListener("sectionPrev", CustomUIEventBindingType.Activating, (ignored, _) -> {
            state.section = state.section.previous();
            state.pageIndex = 0;
            openOrUpdateUi(playerRef, store, playerUuid, commandContext);
        });

        builder.addEventListener("sectionNext", CustomUIEventBindingType.Activating, (ignored, _) -> {
            state.section = state.section.next();
            state.pageIndex = 0;
            openOrUpdateUi(playerRef, store, playerUuid, commandContext);
        });

        if (pageEnabled) {
            builder.addEventListener("listPrev", CustomUIEventBindingType.Activating, (ignored, _) -> {
                if (state.pageIndex > 0) {
                    state.pageIndex--;
                }
                openOrUpdateUi(playerRef, store, playerUuid, commandContext);
            });

            builder.addEventListener("listNext", CustomUIEventBindingType.Activating, (ignored, _) -> {
                int max = pageIndexMax(state.section);
                if (state.pageIndex < max) {
                    state.pageIndex++;
                }
                openOrUpdateUi(playerRef, store, playerUuid, commandContext);
            });
        }

        builder.addEventListener("selectedTierPrev", CustomUIEventBindingType.Activating, (ignored, _) -> {
            state.tierIndex = wrapIndex(state.tierIndex - 1, tierCount);
            openOrUpdateUi(playerRef, store, playerUuid, commandContext);
        });

        builder.addEventListener("selectedTierNext", CustomUIEventBindingType.Activating, (ignored, _) -> {
            state.tierIndex = wrapIndex(state.tierIndex + 1, tierCount);
            openOrUpdateUi(playerRef, store, playerUuid, commandContext);
        });
    }

    private static void registerWorldEvents(
            PageBuilder builder,
            PlayerRef playerRef,
            Store<EntityStore> store,
            UUID playerUuid,
            CommandContext commandContext,
            UiState state
    ) {
        builder.addEventListener("worldToggleEnabled", CustomUIEventBindingType.Activating, (ignored, _) -> {
            boolean current = readBoolean(DifficultyIO.PATH_WORLD_TIER_ENABLED, DifficultyIO.DEFAULT_WORLD_TIER_ENABLED);
            mutateAndReload(
                    playerRef,
                    commandContext,
                    () -> DifficultyAdminConfigEditor.setBaseBoolean(DifficultyIO.PATH_WORLD_TIER_ENABLED, !current),
                    "World tier " + (!current ? "enabled" : "disabled") + "."
            );
            openOrUpdateUi(playerRef, store, playerUuid, commandContext);
        });

        builder.addEventListener("worldModePrev", CustomUIEventBindingType.Activating, (ignored, _) -> {
            String current = readString(DifficultyIO.PATH_WORLD_TIER_MODE, DifficultyIO.DEFAULT_WORLD_TIER_MODE);
            String target = cycleWorldMode(current, -1);
            mutateAndReload(
                    playerRef,
                    commandContext,
                    () -> DifficultyAdminConfigEditor.setBaseString(DifficultyIO.PATH_WORLD_TIER_MODE, target),
                    "World mode set to " + target + "."
            );
            openOrUpdateUi(playerRef, store, playerUuid, commandContext);
        });

        builder.addEventListener("worldModeNext", CustomUIEventBindingType.Activating, (ignored, _) -> {
            String current = readString(DifficultyIO.PATH_WORLD_TIER_MODE, DifficultyIO.DEFAULT_WORLD_TIER_MODE);
            String target = cycleWorldMode(current, 1);
            mutateAndReload(
                    playerRef,
                    commandContext,
                    () -> DifficultyAdminConfigEditor.setBaseString(DifficultyIO.PATH_WORLD_TIER_MODE, target),
                    "World mode set to " + target + "."
            );
            openOrUpdateUi(playerRef, store, playerUuid, commandContext);
        });

        builder.addEventListener("worldFixedPrev", CustomUIEventBindingType.Activating, (ignored, _) -> {
            List<String> tierIds = orderedTierIds();
            String current = readString(DifficultyIO.PATH_WORLD_TIER_FIXED_TIER, DifficultyIO.DEFAULT_WORLD_TIER_FIXED_TIER);
            String target = cycleTierId(current, tierIds, -1);
            mutateAndReload(
                    playerRef,
                    commandContext,
                    () -> DifficultyAdminConfigEditor.setBaseString(DifficultyIO.PATH_WORLD_TIER_FIXED_TIER, target),
                    "World fixed tier set to " + target + "."
            );
            openOrUpdateUi(playerRef, store, playerUuid, commandContext);
        });

        builder.addEventListener("worldFixedNext", CustomUIEventBindingType.Activating, (ignored, _) -> {
            List<String> tierIds = orderedTierIds();
            String current = readString(DifficultyIO.PATH_WORLD_TIER_FIXED_TIER, DifficultyIO.DEFAULT_WORLD_TIER_FIXED_TIER);
            String target = cycleTierId(current, tierIds, 1);
            mutateAndReload(
                    playerRef,
                    commandContext,
                    () -> DifficultyAdminConfigEditor.setBaseString(DifficultyIO.PATH_WORLD_TIER_FIXED_TIER, target),
                    "World fixed tier set to " + target + "."
            );
            openOrUpdateUi(playerRef, store, playerUuid, commandContext);
        });

        builder.addEventListener("worldScaledDown", CustomUIEventBindingType.Activating, (ignored, _) -> {
            double current = readDouble(DifficultyIO.PATH_WORLD_TIER_SCALED_FACTOR, DifficultyIO.DEFAULT_WORLD_TIER_SCALED_FACTOR);
            double target = clamp(round(current - 0.05, 3), 0.0, 1.0);
            mutateAndReload(
                    playerRef,
                    commandContext,
                    () -> DifficultyAdminConfigEditor.setBaseNumber(DifficultyIO.PATH_WORLD_TIER_SCALED_FACTOR, target, false),
                    "World scaled factor set to " + formatDouble(target) + "."
            );
            openOrUpdateUi(playerRef, store, playerUuid, commandContext);
        });

        builder.addEventListener("worldScaledUp", CustomUIEventBindingType.Activating, (ignored, _) -> {
            double current = readDouble(DifficultyIO.PATH_WORLD_TIER_SCALED_FACTOR, DifficultyIO.DEFAULT_WORLD_TIER_SCALED_FACTOR);
            double target = clamp(round(current + 0.05, 3), 0.0, 1.0);
            mutateAndReload(
                    playerRef,
                    commandContext,
                    () -> DifficultyAdminConfigEditor.setBaseNumber(DifficultyIO.PATH_WORLD_TIER_SCALED_FACTOR, target, false),
                    "World scaled factor set to " + formatDouble(target) + "."
            );
            openOrUpdateUi(playerRef, store, playerUuid, commandContext);
        });

        builder.addEventListener("worldToggleScaledUseAll", CustomUIEventBindingType.Activating, (ignored, _) -> {
            boolean current = readBoolean(
                    DifficultyIO.PATH_WORLD_TIER_SCALED_USE_ALL_ONLINE_PLAYERS,
                    DifficultyIO.DEFAULT_WORLD_TIER_SCALED_USE_ALL_ONLINE_PLAYERS
            );
            mutateAndReload(
                    playerRef,
                    commandContext,
                    () -> DifficultyAdminConfigEditor.setBaseBoolean(DifficultyIO.PATH_WORLD_TIER_SCALED_USE_ALL_ONLINE_PLAYERS, !current),
                    "Scaled world tier player source set to " + (!current ? "all online players" : "min/max players") + "."
            );
            openOrUpdateUi(playerRef, store, playerUuid, commandContext);
        });

        builder.addEventListener("worldOverrideSetSelected", CustomUIEventBindingType.Activating, (ignored, _) -> {
            String selectedTier = selectedTierId(state);
            if (selectedTier == null) {
                EventNotificationWrapper.sendMinorEventNotification(playerRef, commandContext, "No tier selected.");
                openOrUpdateUi(playerRef, store, playerUuid, commandContext);
                return;
            }
            boolean changed = DifficultyManager.setWorldTierAdminOverride(selectedTier);
            if (!changed) {
                EventNotificationWrapper.sendMinorEventNotification(playerRef, commandContext, "Invalid tier: " + selectedTier);
            } else {
                WorldTierUiSync.refreshAllPlayers();
                EventNotificationWrapper.sendMinorEventNotification(playerRef, commandContext, "World override set to " + selectedTier + ".");
            }
            openOrUpdateUi(playerRef, store, playerUuid, commandContext);
        });

        builder.addEventListener("worldOverrideClear", CustomUIEventBindingType.Activating, (ignored, _) -> {
            DifficultyManager.clearWorldTierAdminOverride();
            WorldTierUiSync.refreshAllPlayers();
            EventNotificationWrapper.sendMinorEventNotification(playerRef, commandContext, "World override cleared.");
            openOrUpdateUi(playerRef, store, playerUuid, commandContext);
        });
    }

    private static void registerToggleEvents(
            PageBuilder builder,
            PlayerRef playerRef,
            Store<EntityStore> store,
            UUID playerUuid,
            CommandContext commandContext,
            List<ToggleSettingDefinition> definitions
    ) {
        for (ToggleSettingDefinition definition : definitions) {
            builder.addEventListener(toggleEventId(definition), CustomUIEventBindingType.Activating, (ignored, _) -> {
                boolean current = readBoolean(definition.path, definition.fallback);
                boolean next = !current;
                mutateAndReload(
                        playerRef,
                        commandContext,
                        () -> DifficultyAdminConfigEditor.setBaseBoolean(definition.path, next),
                        definition.label + " set to " + (next ? "ON" : "OFF") + "."
                );
                openOrUpdateUi(playerRef, store, playerUuid, commandContext);
            });
        }
    }

    private static void registerNumberEvents(
            PageBuilder builder,
            PlayerRef playerRef,
            Store<EntityStore> store,
            UUID playerUuid,
            CommandContext commandContext,
            List<NumberSettingDefinition> definitions
    ) {
        for (NumberSettingDefinition definition : definitions) {
            builder.addEventListener(numberDecreaseEventId(definition), CustomUIEventBindingType.Activating, (ignored, _) -> {
                double current = readDouble(definition.path, definition.fallback);
                double next = clamp(round(current - definition.step, 6), definition.min, definition.max);
                mutateAndReload(
                        playerRef,
                        commandContext,
                        () -> DifficultyAdminConfigEditor.setBaseNumber(definition.path, next, definition.integer),
                        definition.label + " set to " + formatNumber(next, definition.integer) + "."
                );
                openOrUpdateUi(playerRef, store, playerUuid, commandContext);
            });

            builder.addEventListener(numberIncreaseEventId(definition), CustomUIEventBindingType.Activating, (ignored, _) -> {
                double current = readDouble(definition.path, definition.fallback);
                double next = clamp(round(current + definition.step, 6), definition.min, definition.max);
                mutateAndReload(
                        playerRef,
                        commandContext,
                        () -> DifficultyAdminConfigEditor.setBaseNumber(definition.path, next, definition.integer),
                        definition.label + " set to " + formatNumber(next, definition.integer) + "."
                );
                openOrUpdateUi(playerRef, store, playerUuid, commandContext);
            });
        }
    }

    private static void registerTierEvents(
            PageBuilder builder,
            PlayerRef playerRef,
            Store<EntityStore> store,
            UUID playerUuid,
            CommandContext commandContext,
            UiState state,
            List<TierNumberSettingDefinition> definitions
    ) {
        builder.addEventListener("tierToggleAllowed", CustomUIEventBindingType.Activating, (ignored, _) -> {
            String tierId = selectedTierId(state);
            if (tierId == null) {
                openOrUpdateUi(playerRef, store, playerUuid, commandContext);
                return;
            }
            boolean current = DifficultyManager.getSettings().getBoolean(tierId, DifficultyIO.SETTING_IS_ALLOWED);
            mutateAndReload(
                    playerRef,
                    commandContext,
                    () -> DifficultyAdminConfigEditor.setTierBoolean(tierId, DifficultyIO.SETTING_IS_ALLOWED, !current),
                    "Tier " + tierId + " allowed set to " + (!current ? "ON" : "OFF") + "."
            );
            openOrUpdateUi(playerRef, store, playerUuid, commandContext);
        });

        builder.addEventListener("tierToggleHidden", CustomUIEventBindingType.Activating, (ignored, _) -> {
            String tierId = selectedTierId(state);
            if (tierId == null) {
                openOrUpdateUi(playerRef, store, playerUuid, commandContext);
                return;
            }
            boolean current = DifficultyManager.getSettings().getBoolean(tierId, DifficultyIO.SETTING_IS_HIDDEN);
            mutateAndReload(
                    playerRef,
                    commandContext,
                    () -> DifficultyAdminConfigEditor.setTierBoolean(tierId, DifficultyIO.SETTING_IS_HIDDEN, !current),
                    "Tier " + tierId + " hidden set to " + (!current ? "ON" : "OFF") + "."
            );
            openOrUpdateUi(playerRef, store, playerUuid, commandContext);
        });

        builder.addEventListener("tierSetAsWorldFixed", CustomUIEventBindingType.Activating, (ignored, _) -> {
            String tierId = selectedTierId(state);
            if (tierId == null) {
                openOrUpdateUi(playerRef, store, playerUuid, commandContext);
                return;
            }
            mutateAndReload(
                    playerRef,
                    commandContext,
                    () -> DifficultyAdminConfigEditor.setBaseString(DifficultyIO.PATH_WORLD_TIER_FIXED_TIER, tierId),
                    "World fixed tier set to " + tierId + "."
            );
            openOrUpdateUi(playerRef, store, playerUuid, commandContext);
        });

        builder.addEventListener("tierSetAsWorldOverride", CustomUIEventBindingType.Activating, (ignored, _) -> {
            String tierId = selectedTierId(state);
            if (tierId == null) {
                openOrUpdateUi(playerRef, store, playerUuid, commandContext);
                return;
            }
            boolean changed = DifficultyManager.setWorldTierAdminOverride(tierId);
            if (changed) {
                WorldTierUiSync.refreshAllPlayers();
                EventNotificationWrapper.sendMinorEventNotification(playerRef, commandContext, "World override set to " + tierId + ".");
            } else {
                EventNotificationWrapper.sendMinorEventNotification(playerRef, commandContext, "Failed to set world override.");
            }
            openOrUpdateUi(playerRef, store, playerUuid, commandContext);
        });

        for (TierNumberSettingDefinition definition : definitions) {
            builder.addEventListener(tierDecreaseEventId(definition), CustomUIEventBindingType.Activating, (ignored, _) -> {
                String tierId = selectedTierId(state);
                if (tierId == null) {
                    openOrUpdateUi(playerRef, store, playerUuid, commandContext);
                    return;
                }
                double current = DifficultyManager.getSettings().get(tierId, definition.key);
                double next = clamp(round(current - definition.step, 6), definition.min, definition.max);
                mutateAndReload(
                        playerRef,
                        commandContext,
                        () -> DifficultyAdminConfigEditor.setTierNumber(tierId, definition.key, next, definition.integer),
                        tierId + ": " + definition.label + " set to " + formatNumber(next, definition.integer) + "."
                );
                openOrUpdateUi(playerRef, store, playerUuid, commandContext);
            });

            builder.addEventListener(tierIncreaseEventId(definition), CustomUIEventBindingType.Activating, (ignored, _) -> {
                String tierId = selectedTierId(state);
                if (tierId == null) {
                    openOrUpdateUi(playerRef, store, playerUuid, commandContext);
                    return;
                }
                double current = DifficultyManager.getSettings().get(tierId, definition.key);
                double next = clamp(round(current + definition.step, 6), definition.min, definition.max);
                mutateAndReload(
                        playerRef,
                        commandContext,
                        () -> DifficultyAdminConfigEditor.setTierNumber(tierId, definition.key, next, definition.integer),
                        tierId + ": " + definition.label + " set to " + formatNumber(next, definition.integer) + "."
                );
                openOrUpdateUi(playerRef, store, playerUuid, commandContext);
            });
        }
    }

    private static void mutateAndReload(PlayerRef playerRef, CommandContext commandContext, IoMutation mutation, String successMessage) {
        try {
            mutation.run();
            DifficultyManager.reloadConfig();
            WorldTierUiSync.refreshAllPlayers();
            EventNotificationWrapper.sendMinorEventNotification(playerRef, commandContext, successMessage);
        } catch (IOException e) {
            EventNotificationWrapper.sendMinorEventNotification(playerRef, commandContext, "Admin update failed: " + e.getMessage());
        }
    }

    private static List<ToggleRow> buildToggleRows(List<ToggleSettingDefinition> definitions) {
        List<ToggleRow> rows = new ArrayList<>(definitions.size());
        for (ToggleSettingDefinition definition : definitions) {
            boolean value = readBoolean(definition.path, definition.fallback);
            rows.add(new ToggleRow(definition.label, toggleEventId(definition), value));
        }
        return rows;
    }

    private static List<NumberRow> buildNumberRows(List<NumberSettingDefinition> definitions) {
        List<NumberRow> rows = new ArrayList<>(definitions.size());
        for (NumberSettingDefinition definition : definitions) {
            double value = readDouble(definition.path, definition.fallback);
            rows.add(new NumberRow(
                    definition.label,
                    numberDecreaseEventId(definition),
                    numberIncreaseEventId(definition),
                    formatNumber(value, definition.integer),
                    formatNumber(definition.step, definition.integer)
            ));
        }
        return rows;
    }

    private static List<NumberRow> buildTierRows(List<TierNumberSettingDefinition> definitions, String tierId) {
        DifficultySettings settings = DifficultyManager.getSettings();
        List<NumberRow> rows = new ArrayList<>(definitions.size());
        for (TierNumberSettingDefinition definition : definitions) {
            double value = settings.get(tierId, definition.key);
            rows.add(new NumberRow(
                    definition.label,
                    tierDecreaseEventId(definition),
                    tierIncreaseEventId(definition),
                    formatNumber(value, definition.integer),
                    formatNumber(definition.step, definition.integer)
            ));
        }
        return rows;
    }

    private static String selectedTierId(UiState state) {
        List<String> tierIds = orderedTierIds();
        if (tierIds.isEmpty()) {
            return null;
        }
        if (state.tierIndex < 0 || state.tierIndex >= tierIds.size()) {
            state.tierIndex = wrapIndex(state.tierIndex, tierIds.size());
        }
        return tierIds.get(state.tierIndex);
    }

    private static List<String> orderedTierIds() {
        return new ArrayList<>(DifficultyManager.getSettings().tiers().keySet());
    }

    private static <T> List<T> page(List<T> all, int pageIndex, int rows_per_page) {
        if (all.isEmpty()) {
            return List.of();
        }
        int from = Math.max(0, pageIndex * rows_per_page);
        if (from >= all.size()) {
            return List.of();
        }
        int to = Math.min(all.size(), from + rows_per_page);
        return all.subList(from, to);
    }

    private static int pageIndexMax(AdminSection section) {
        final int rowsPerPage = ROWS_PER_PAGE +
                ((section == AdminSection.TOGGLES || section == AdminSection.NUMBERS) ? 3 : 0);

        final int size = switch (section) {
            case WORLD -> 0;
            case TOGGLES -> TOGGLE_SETTINGS.size();
            case NUMBERS -> NUMBER_SETTINGS.size();
            case TIERS -> TIER_NUMBER_SETTINGS.size();
        };

        return size <= 0 ? 0 : (size - 1) / rowsPerPage;
    }

    private static void clampState(UiState state, int tierCount) {
        if (tierCount <= 0) {
            state.tierIndex = 0;
            state.pageIndex = 0;
            return;
        }
        state.tierIndex = wrapIndex(state.tierIndex, tierCount);
        int max = pageIndexMax(state.section);
        if (state.pageIndex > max) {
            state.pageIndex = max;
        }
        if (state.pageIndex < 0) {
            state.pageIndex = 0;
        }
    }

    private static int wrapIndex(int value, int size) {
        if (size <= 0) {
            return 0;
        }
        int wrapped = value % size;
        if (wrapped < 0) {
            wrapped += size;
        }
        return wrapped;
    }

    private static String cycleWorldMode(String current, int delta) {
        int index = 0;
        for (int i = 0; i < WORLD_MODES.size(); i++) {
            if (WORLD_MODES.get(i).equalsIgnoreCase(current)) {
                index = i;
                break;
            }
        }
        return WORLD_MODES.get(wrapIndex(index + delta, WORLD_MODES.size()));
    }

    private static String cycleTierId(String current, List<String> tierIds, int delta) {
        if (tierIds.isEmpty()) {
            return DifficultyIO.DEFAULT_BASE_DIFFICULTY;
        }
        int index = 0;
        for (int i = 0; i < tierIds.size(); i++) {
            if (tierIds.get(i).equalsIgnoreCase(current)) {
                index = i;
                break;
            }
        }
        return tierIds.get(wrapIndex(index + delta, tierIds.size()));
    }

    private static boolean readBoolean(String path, boolean fallback) {
        return DifficultyManager.getConfig().getBoolean(path, fallback);
    }

    private static double readDouble(String path, double fallback) {
        return DifficultyManager.getConfig().getDouble(path, fallback);
    }

    private static String readString(String path, String fallback) {
        return DifficultyManager.getConfig().getString(path, fallback);
    }

    private static double clamp(double value, double min, double max) {
        if (value < min) {
            return min;
        }
        if (value > max) {
            return max;
        }
        return value;
    }

    private static double round(double value, int digits) {
        double factor = Math.pow(10.0, Math.max(0, digits));
        return Math.round(value * factor) / factor;
    }

    private static String formatDouble(double value) {
        return formatNumber(value, false);
    }

    private static String formatNumber(double value, boolean integer) {
        if (integer) {
            return Integer.toString((int) Math.round(value));
        }
        String text = String.format(Locale.ROOT, "%.4f", value);
        int end = text.length();
        while (end > 0 && text.charAt(end - 1) == '0') {
            end--;
        }
        if (end > 0 && text.charAt(end - 1) == '.') {
            end--;
        }
        return end <= 0 ? "0" : text.substring(0, end);
    }

    private static String toggleEventId(ToggleSettingDefinition definition) {
        return "toggle-" + definition.id;
    }

    private static String numberDecreaseEventId(NumberSettingDefinition definition) {
        return "number-dec-" + definition.id;
    }

    private static String numberIncreaseEventId(NumberSettingDefinition definition) {
        return "number-inc-" + definition.id;
    }

    private static String tierDecreaseEventId(TierNumberSettingDefinition definition) {
        return "tier-dec-" + definition.id;
    }

    private static String tierIncreaseEventId(TierNumberSettingDefinition definition) {
        return "tier-inc-" + definition.id;
    }

    private record ToggleSettingDefinition(String id, String label, String path, boolean fallback) {
    }

    private record NumberSettingDefinition(
            String id,
            String label,
            String path,
            double step,
            double min,
            double max,
            boolean integer,
            double fallback
    ) {
    }

    private record TierNumberSettingDefinition(
            String id,
            String label,
            String key,
            double step,
            double min,
            double max,
            boolean integer
    ) {
    }

    private record ToggleRow(String label, String eventId, boolean enabled) {
    }

    private record NumberRow(
            String label,
            String decreaseEventId,
            String increaseEventId,
            String valueText,
            String stepText
    ) {
    }

    private enum AdminSection {
        WORLD("World Tier"),
        TOGGLES("Feature Toggles"),
        NUMBERS("Base Numbers"),
        TIERS("Tier Editor");

        private static final AdminSection[] VALUES = values();
        private final String label;

        AdminSection(String label) {
            this.label = label;
        }

        private AdminSection previous() {
            int index = wrapIndex(ordinal() - 1, VALUES.length);
            return VALUES[index];
        }

        private AdminSection next() {
            int index = wrapIndex(ordinal() + 1, VALUES.length);
            return VALUES[index];
        }
    }

    private static final class UiState {
        private AdminSection section;
        private int pageIndex;
        private int tierIndex;

        private static UiState createDefault() {
            UiState state = new UiState();
            state.section = AdminSection.WORLD;
            state.pageIndex = 0;
            state.tierIndex = 0;
            return state;
        }
    }

    @FunctionalInterface
    private interface IoMutation {
        void run() throws IOException;
    }
}
