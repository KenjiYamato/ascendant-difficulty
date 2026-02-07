package ascendant.core.commands;

import ascendant.core.config.DifficultyManager;
import ascendant.core.ui.TierSelect;
import com.hypixel.hytale.component.Store;
import com.hypixel.hytale.server.core.command.system.CommandContext;
import com.hypixel.hytale.server.core.universe.PlayerRef;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;
import org.checkerframework.checker.nullness.compatqual.NonNullDecl;

import java.util.UUID;

public final class TierSelectCommand extends AbstractPlayerUICommand {

    public TierSelectCommand(String commandName, String commandDescription, String commandPermission) {
        super(commandName, commandDescription, commandPermission);
        DifficultyManager.getConfig();
        DifficultyManager.getSettings();
    }

    @Override
    protected void openOrUpdateUi(@NonNullDecl PlayerRef playerRef, @NonNullDecl Store<EntityStore> store, @NonNullDecl UUID playerUuid, @NonNullDecl CommandContext commandContext) {
        TierSelect.openOrUpdateUi(playerRef, store, playerUuid, commandContext);
    }
}
