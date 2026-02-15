package ascendant.core.commands.debug;

import ascendant.core.commands.AbstractPlayerCommand;
import ascendant.core.config.DifficultyIO;
import ascendant.core.config.DifficultyManager;
import ascendant.core.config.DifficultyMeta;
import ascendant.core.config.DifficultySettings;
import ascendant.core.ui.DifficultyBadge;
import ascendant.core.util.EventNotificationWrapper;
import com.hypixel.hytale.component.Store;
import com.hypixel.hytale.server.core.command.system.CommandContext;
import com.hypixel.hytale.server.core.universe.PlayerRef;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;
import org.checkerframework.checker.nullness.compatqual.NonNullDecl;

import java.util.Map;
import java.util.UUID;

public final class SetTierHighestCommand extends AbstractPlayerCommand {

    public SetTierHighestCommand(String commandName, String commandDescription, String commandPermission) {
        super(commandName, commandDescription, commandPermission);
    }

    @Override
    protected void executeOnWorldThread(@NonNullDecl PlayerRef playerRef, @NonNullDecl Store<EntityStore> store, @NonNullDecl UUID playerUuid, @NonNullDecl CommandContext commandContext) {
        String tierId = resolveHighestTierId();
        DifficultyManager.setPlayerDifficultyOverride(playerUuid, tierId);
        DifficultyBadge.updateForPlayer(playerRef);

        DifficultyMeta.TierMeta meta = DifficultyMeta.resolve(DifficultyManager.getConfig(), tierId);
        EventNotificationWrapper.sendMajorEventNotification(playerRef, commandContext, meta.displayName(), "selected difficulty");
    }

    private static String resolveHighestTierId() {
        DifficultySettings settings = DifficultyManager.getSettings();
        String resolved = null;
        for (Map.Entry<String, Map<String, Double>> entry : settings.tiers().entrySet()) {
            resolved = entry.getKey();
        }
        if (resolved == null || resolved.isBlank()) {
            return DifficultyIO.DEFAULT_BASE_DIFFICULTY;
        }
        return resolved;
    }
}
