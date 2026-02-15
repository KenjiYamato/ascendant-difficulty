package ascendant.core;

import ascendant.core.adapter.KillFeedAdapter;
import ascendant.core.adapter.NotificationsAdapter;
import ascendant.core.commands.debug.ClearAllEntityCommand;
import ascendant.core.commands.debug.ClearDroppedItemsCommand;
import ascendant.core.commands.DifficultyBadgeToggleCommand;
import ascendant.core.commands.debug.SetTierHighestCommand;
import ascendant.core.commands.debug.SetTierLowestCommand;
import ascendant.core.commands.debug.SpawnWraithCommand;
import ascendant.core.commands.TierSelectCommand;
import ascendant.core.commands.debug.TestAttackToggleCommand;
import ascendant.core.config.DifficultyConfig;
import ascendant.core.config.DifficultyIO;
import ascendant.core.config.DifficultySettings;
import ascendant.core.config.RuntimeSettings;
import ascendant.core.events.OnDeath;
import ascendant.core.scaling.*;
import ascendant.core.ui.DifficultyBadge;
import ascendant.core.util.NpcRoles;
import com.hypixel.hytale.server.core.event.events.player.PlayerDisconnectEvent;
import com.hypixel.hytale.server.core.event.events.player.PlayerReadyEvent;
import com.hypixel.hytale.server.core.plugin.JavaPlugin;
import com.hypixel.hytale.server.core.plugin.JavaPluginInit;

import javax.annotation.Nonnull;
import java.io.IOException;

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
        ascendant.core.config.DifficultyManager.initialize(this.difficultyConfig, this.difficultySettings);
        RuntimeSettings.load();
        this.getCommandRegistry().registerCommand(
                new TierSelectCommand("ascendant-difficulty", "Difficulty / Tier selection", "ascendant.difficulty"));
        this.getCommandRegistry().registerCommand(
                new DifficultyBadgeToggleCommand("ascendant-difficulty-badge-toggle", "Toggle difficulty badge display", "ascendant.difficulty"));
        if (RuntimeSettings.allowDebugCommands()) {
            this.getCommandRegistry().registerCommand(
                    new ClearAllEntityCommand("ce", "Clear Entities", "ascendant.debug.clear_entities"));
            this.getCommandRegistry().registerCommand(
                    new ClearDroppedItemsCommand("ci", "Clear dropped items", "ascendant.debug.clear_items"));
            this.getCommandRegistry().registerCommand(
                    new TestAttackToggleCommand("test_attack", "Toggle debug max attack damage", "ascendant.debug.test_attack"));
            this.getCommandRegistry().registerCommand(
                    new SpawnWraithCommand("spawn_wraith", "Spawn a Wraith (debug)", "ascendant.debug.spawn_wraith"));
            this.getCommandRegistry().registerCommand(
                    new SetTierLowestCommand("tier_lowest", "Set tier to lowest (debug)", "ascendant.debug.tier_lowest"));
            this.getCommandRegistry().registerCommand(
                    new SetTierHighestCommand("tier_highest", "Set tier to highest (debug)", "ascendant.debug.tier_highest"));
        }

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
        // Notifications adapter PACKET_ID = 212
        NotificationsAdapter.register();
        // KillFeed adapter PACKET_ID = 213;
        KillFeedAdapter.register();
    }

    private void loadDifficultyConfig() {
        try {
            this.difficultyConfig = DifficultyIO.loadOrCreateConfig();
            this.difficultySettings = this.difficultyConfig.toSettings();
        } catch (IOException e) {
            throw new IllegalStateException("Failed to load difficulty config.", e);
        }
    }

}
