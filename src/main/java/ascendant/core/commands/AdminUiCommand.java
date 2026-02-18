package ascendant.core.commands;

import ascendant.core.ui.AdminUi;
import com.hypixel.hytale.component.Store;
import com.hypixel.hytale.server.core.command.system.CommandContext;
import com.hypixel.hytale.server.core.universe.PlayerRef;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;
import org.checkerframework.checker.nullness.compatqual.NonNullDecl;

import java.util.UUID;

public final class AdminUiCommand extends AbstractPlayerCommand {

    public AdminUiCommand(String commandName, String commandDescription, String commandPermission) {
        super(commandName, commandDescription, commandPermission);
    }

    @Override
    protected void executeOnWorldThread(
            @NonNullDecl PlayerRef playerRef,
            @NonNullDecl Store<EntityStore> store,
            @NonNullDecl UUID playerUuid,
            @NonNullDecl CommandContext commandContext
    ) {
        AdminUi.openOrUpdateUi(playerRef, store, playerUuid, commandContext);
    }
}
