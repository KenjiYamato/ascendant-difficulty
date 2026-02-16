package ascendant.core.commands.debug;

import ascendant.core.commands.AbstractPlayerCommand;
import ascendant.core.scaling.PlayerDamageReceiveMultiplier;
import ascendant.core.util.EventNotificationWrapper;
import com.hypixel.hytale.component.Store;
import com.hypixel.hytale.server.core.command.system.CommandContext;
import com.hypixel.hytale.server.core.universe.PlayerRef;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;
import org.checkerframework.checker.nullness.compatqual.NonNullDecl;

import java.util.UUID;

public final class TestDamageToggleCommand extends AbstractPlayerCommand {

    public TestDamageToggleCommand(String commandName, String commandDescription, String commandPermission) {
        super(commandName, commandDescription, commandPermission);
    }

    @Override
    protected void executeOnWorldThread(@NonNullDecl PlayerRef playerRef, @NonNullDecl Store<EntityStore> store, @NonNullDecl UUID playerUuid, @NonNullDecl CommandContext commandContext) {
        boolean enabled = PlayerDamageReceiveMultiplier.toggleDebugMaxDamage(playerUuid);
        String message = enabled ? "Debug damage: max damage enabled." : "Debug damage: max damage disabled.";
        EventNotificationWrapper.sendMinorEventNotification(playerRef, commandContext, message);
    }
}
