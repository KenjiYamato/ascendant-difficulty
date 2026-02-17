package ascendant.core;

import ascendant.core.adapter.ChatTierTagHandler;
import ascendant.core.adapter.KillFeedAdapter;
import ascendant.core.adapter.KillFeedTierTagHandler;
import ascendant.core.adapter.NotificationsAdapter;
import ascendant.core.adapter.ServerPlayerListAdapter;
import ascendant.core.commands.debug.ClearAllEntityCommand;
import ascendant.core.commands.debug.ClearDroppedItemsCommand;
import ascendant.core.commands.DifficultyBadgeToggleCommand;
import ascendant.core.commands.ReloadConfigCommand;
import ascendant.core.commands.debug.SetTierHighestCommand;
import ascendant.core.commands.debug.SetTierLowestCommand;
import ascendant.core.commands.debug.SpawnWraithCommand;
import ascendant.core.commands.TierSelectCommand;
import ascendant.core.commands.debug.TestAttackToggleCommand;
import ascendant.core.commands.debug.TestDamageToggleCommand;
import ascendant.core.config.DifficultyConfig;
import ascendant.core.config.DifficultyIO;
import ascendant.core.config.DifficultyManager;
import ascendant.core.config.DifficultySettings;
import ascendant.core.config.RuntimeSettings;
import ascendant.core.events.OnDeath;
import ascendant.core.scaling.*;
import ascendant.core.ui.DifficultyBadge;
import ascendant.core.util.CommandRegistrationUtil;
import ascendant.core.util.NpcRoles;
import com.hypixel.hytale.server.core.event.events.player.PlayerChatEvent;
import com.hypixel.hytale.server.core.event.events.player.PlayerDisconnectEvent;
import com.hypixel.hytale.server.core.event.events.player.PlayerReadyEvent;
import com.hypixel.hytale.server.core.command.system.AbstractCommand;
import com.hypixel.hytale.server.core.plugin.JavaPlugin;
import com.hypixel.hytale.server.core.plugin.JavaPluginInit;

import javax.annotation.Nonnull;
import java.io.IOException;
import java.util.function.Consumer;

public class AscendantDifficultyPlugin extends JavaPlugin {
    private DifficultyConfig difficultyConfig;
    private DifficultySettings difficultySettings;

    public AscendantDifficultyPlugin(@Nonnull JavaPluginInit init) {
        super(init);
    }

    @Override
    protected void setup() {
        super.setup();
        this.loadDifficultyConfig();
        DifficultyManager.initialize(this.difficultyConfig, this.difficultySettings);
        RuntimeSettings.load();
        registerCommands();

        NpcRoles.preload();
        // damage entity receive from player
        this.getEntityStoreRegistry().registerSystem(new EntityDamageReceiveMultiplier());
        // damage player receive
        this.getEntityStoreRegistry().registerSystem(new PlayerDamageReceiveMultiplier());
        // health
        this.getEntityStoreRegistry().registerSystem(new NearestPlayerHealthScaleSystem());
        // drop
        this.getEntityStoreRegistry().registerSystem(new EntityDropMultiplier());
        // elite spawn
        this.getEntityStoreRegistry().registerSystem(new EntityEliteSpawn());
        this.getEntityStoreRegistry().registerSystem(new EliteSpawnQueueTickSystem());
        // custom experience for levelingcore
        this.getEntityStoreRegistry().registerSystem(new OnDeath());
        //this.getEntityStoreRegistry().registerSystem(new OnSpawn());
        // experience
        ExperienceAndCashMultiplier.initialize();
        //CosmeticDamageNumbersAdapter.register();
        // badge
        this.getEventRegistry().registerGlobal(PlayerReadyEvent.class, (playerReadyEvent) -> {
            DifficultyBadge.onPlayerReady(playerReadyEvent);
        });

        this.getEventRegistry().registerGlobal(PlayerDisconnectEvent.class, (playerDisconnectEvent) -> {
            DifficultyBadge.onPlayerDisconnect(playerDisconnectEvent);
        });
        this.getEventRegistry().registerGlobal(PlayerChatEvent.class, ChatTierTagHandler::handle);
        // Notifications adapter PACKET_ID = 212
        NotificationsAdapter.register();
        // KillFeed adapter PACKET_ID = 213
        KillFeedAdapter.register();
        // Server player list adapter PACKET_ID = 224
        ServerPlayerListAdapter.register();
        this.getEntityStoreRegistry().registerSystem(new KillFeedTierTagHandler.KillerSystem());
        this.getEntityStoreRegistry().registerSystem(new KillFeedTierTagHandler.DecedentSystem());
        this.getEntityStoreRegistry().registerSystem(new KillFeedTierTagHandler.DisplaySystem());
    }

    private void loadDifficultyConfig() {
        try {
            this.difficultyConfig = DifficultyIO.loadOrCreateConfig();
            this.difficultySettings = this.difficultyConfig.toSettings();
        } catch (IOException e) {
            throw new IllegalStateException("Failed to load difficulty config.", e);
        }
    }

    private void registerCommands() {
        Consumer<AbstractCommand> registrar = this.getCommandRegistry()::registerCommand;
        CommandRegistrationUtil.registerCommandWithAliases(
                registrar,
                DifficultyIO.COMMAND_TIER_SELECT_NAME,
                DifficultyIO.DEFAULT_COMMAND_TIER_SELECT_NAME,
                DifficultyIO.COMMAND_TIER_SELECT_ALIASES,
                DifficultyIO.COMMAND_TIER_SELECT_PERMISSION,
                DifficultyIO.DEFAULT_COMMAND_TIER_SELECT_PERMISSION,
                (name, perm) -> new TierSelectCommand(name, "Difficulty / Tier selection", perm));
        CommandRegistrationUtil.registerCommandWithAliases(
                registrar,
                DifficultyIO.COMMAND_BADGE_TOGGLE_NAME,
                DifficultyIO.DEFAULT_COMMAND_BADGE_TOGGLE_NAME,
                DifficultyIO.COMMAND_BADGE_TOGGLE_ALIASES,
                DifficultyIO.COMMAND_BADGE_TOGGLE_PERMISSION,
                DifficultyIO.DEFAULT_COMMAND_BADGE_TOGGLE_PERMISSION,
                (name, perm) -> new DifficultyBadgeToggleCommand(name, "Toggle difficulty badge display", perm));
        CommandRegistrationUtil.registerCommandWithAliases(
                registrar,
                DifficultyIO.COMMAND_RELOAD_NAME,
                DifficultyIO.DEFAULT_COMMAND_RELOAD_NAME,
                DifficultyIO.COMMAND_RELOAD_ALIASES,
                DifficultyIO.COMMAND_RELOAD_PERMISSION,
                DifficultyIO.DEFAULT_COMMAND_RELOAD_PERMISSION,
                (name, perm) -> new ReloadConfigCommand(name, "Reload ascendant difficulty config", perm));
        if (RuntimeSettings.allowDebugCommands()) {
            registerDebugCommands(registrar);
        }
    }

    private void registerDebugCommands(Consumer<AbstractCommand> registrar) {
        CommandRegistrationUtil.registerCommandWithAliases(
                registrar,
                DifficultyIO.COMMAND_DEBUG_CLEAR_ENTITIES_NAME,
                DifficultyIO.DEFAULT_COMMAND_DEBUG_CLEAR_ENTITIES_NAME,
                DifficultyIO.COMMAND_DEBUG_CLEAR_ENTITIES_ALIASES,
                DifficultyIO.COMMAND_DEBUG_CLEAR_ENTITIES_PERMISSION,
                DifficultyIO.DEFAULT_COMMAND_DEBUG_CLEAR_ENTITIES_PERMISSION,
                (name, perm) -> new ClearAllEntityCommand(name, "Clear Entities", perm));
        CommandRegistrationUtil.registerCommandWithAliases(
                registrar,
                DifficultyIO.COMMAND_DEBUG_CLEAR_ITEMS_NAME,
                DifficultyIO.DEFAULT_COMMAND_DEBUG_CLEAR_ITEMS_NAME,
                DifficultyIO.COMMAND_DEBUG_CLEAR_ITEMS_ALIASES,
                DifficultyIO.COMMAND_DEBUG_CLEAR_ITEMS_PERMISSION,
                DifficultyIO.DEFAULT_COMMAND_DEBUG_CLEAR_ITEMS_PERMISSION,
                (name, perm) -> new ClearDroppedItemsCommand(name, "Clear dropped items", perm));
        CommandRegistrationUtil.registerCommandWithAliases(
                registrar,
                DifficultyIO.COMMAND_DEBUG_TEST_ATTACK_NAME,
                DifficultyIO.DEFAULT_COMMAND_DEBUG_TEST_ATTACK_NAME,
                DifficultyIO.COMMAND_DEBUG_TEST_ATTACK_ALIASES,
                DifficultyIO.COMMAND_DEBUG_TEST_ATTACK_PERMISSION,
                DifficultyIO.DEFAULT_COMMAND_DEBUG_TEST_ATTACK_PERMISSION,
                (name, perm) -> new TestAttackToggleCommand(name, "Toggle debug max attack damage", perm));
        CommandRegistrationUtil.registerCommandWithAliases(
                registrar,
                DifficultyIO.COMMAND_DEBUG_TEST_DAMAGE_NAME,
                DifficultyIO.DEFAULT_COMMAND_DEBUG_TEST_DAMAGE_NAME,
                DifficultyIO.COMMAND_DEBUG_TEST_DAMAGE_ALIASES,
                DifficultyIO.COMMAND_DEBUG_TEST_DAMAGE_PERMISSION,
                DifficultyIO.DEFAULT_COMMAND_DEBUG_TEST_DAMAGE_PERMISSION,
                (name, perm) -> new TestDamageToggleCommand(name, "Toggle debug max incoming damage", perm));
        CommandRegistrationUtil.registerCommandWithAliases(
                registrar,
                DifficultyIO.COMMAND_DEBUG_SPAWN_WRAITH_NAME,
                DifficultyIO.DEFAULT_COMMAND_DEBUG_SPAWN_WRAITH_NAME,
                DifficultyIO.COMMAND_DEBUG_SPAWN_WRAITH_ALIASES,
                DifficultyIO.COMMAND_DEBUG_SPAWN_WRAITH_PERMISSION,
                DifficultyIO.DEFAULT_COMMAND_DEBUG_SPAWN_WRAITH_PERMISSION,
                (name, perm) -> new SpawnWraithCommand(name, "Spawn a Wraith (debug)", perm));
        CommandRegistrationUtil.registerCommandWithAliases(
                registrar,
                DifficultyIO.COMMAND_DEBUG_TIER_LOWEST_NAME,
                DifficultyIO.DEFAULT_COMMAND_DEBUG_TIER_LOWEST_NAME,
                DifficultyIO.COMMAND_DEBUG_TIER_LOWEST_ALIASES,
                DifficultyIO.COMMAND_DEBUG_TIER_LOWEST_PERMISSION,
                DifficultyIO.DEFAULT_COMMAND_DEBUG_TIER_LOWEST_PERMISSION,
                (name, perm) -> new SetTierLowestCommand(name, "Set tier to lowest (debug)", perm));
        CommandRegistrationUtil.registerCommandWithAliases(
                registrar,
                DifficultyIO.COMMAND_DEBUG_TIER_HIGHEST_NAME,
                DifficultyIO.DEFAULT_COMMAND_DEBUG_TIER_HIGHEST_NAME,
                DifficultyIO.COMMAND_DEBUG_TIER_HIGHEST_ALIASES,
                DifficultyIO.COMMAND_DEBUG_TIER_HIGHEST_PERMISSION,
                DifficultyIO.DEFAULT_COMMAND_DEBUG_TIER_HIGHEST_PERMISSION,
                (name, perm) -> new SetTierHighestCommand(name, "Set tier to highest (debug)", perm));
    }
}
