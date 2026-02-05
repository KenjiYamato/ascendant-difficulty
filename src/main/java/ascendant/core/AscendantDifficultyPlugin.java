package ascendant.core;

import ascendant.core.commands.TierSelectCommand;
import ascendant.core.config.DifficultyConfig;
import ascendant.core.config.DifficultyIO;
import ascendant.core.config.DifficultySettings;
import ascendant.core.scaling.*;
import ascendant.core.ui.DifficultyBadge;
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
        this.getCommandRegistry().registerCommand(
                new TierSelectCommand("ascendant-difficulty", "Difficulty / Tier selection", "ascendant.difficulty"));


        // damage entity receive from player
        this.getEntityStoreRegistry().registerSystem(new EntityDamageReceiveMultiplier());
        // damage player receive
        this.getEntityStoreRegistry().registerSystem(new PlayerDamageReceiveMultiplier());
        // health
        this.getEntityStoreRegistry().registerSystem(new NearestPlayerHealthScaleSystem());
        // drop
        this.getEntityStoreRegistry().registerSystem(new EntityDropMultiplier());
        // experience
        ExperienceAndCashMultiplier.registerListener();
        //CosmeticDamageNumbersAdapter.register();
        // badge
        this.getEventRegistry().registerGlobal(PlayerReadyEvent.class, (playerReadyEvent) -> {
            DifficultyBadge.onPlayerReady(playerReadyEvent);
        });

        this.getEventRegistry().registerGlobal(PlayerDisconnectEvent.class, (playerDisconnectEvent) -> {
            DifficultyBadge.onPlayerDisconnect(playerDisconnectEvent);
        });

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
