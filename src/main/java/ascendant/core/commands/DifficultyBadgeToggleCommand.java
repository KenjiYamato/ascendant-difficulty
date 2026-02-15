package ascendant.core.commands;

import ascendant.core.config.DifficultyManager;
import ascendant.core.ui.DifficultyBadge;
import ascendant.core.util.EventNotificationWrapper;
import com.hypixel.hytale.component.Store;
import com.hypixel.hytale.server.core.command.system.CommandContext;
import com.hypixel.hytale.server.core.universe.PlayerRef;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;
import org.checkerframework.checker.nullness.compatqual.NonNullDecl;

import java.util.UUID;

public final class DifficultyBadgeToggleCommand extends AbstractPlayerCommand {

    public DifficultyBadgeToggleCommand(String commandName, String commandDescription, String commandPermission) {
        super(commandName, commandDescription, commandPermission);
        DifficultyManager.getConfig();
        DifficultyManager.getSettings();
    }

    @Override
    protected void executeOnWorldThread(@NonNullDecl PlayerRef playerRef, @NonNullDecl Store<EntityStore> store, @NonNullDecl UUID playerUuid, @NonNullDecl CommandContext commandContext) {
        boolean badgeVisible = DifficultyManager.togglePlayerBadgeVisibility(playerUuid);
        DifficultyBadge.updateForPlayer(playerRef);
        String message = badgeVisible ? "Difficulty badge shown." : "Difficulty badge hidden.";
        EventNotificationWrapper.sendMinorEventNotification(playerRef, commandContext, message);
    }
}
