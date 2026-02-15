package ascendant.core.commands.debug;

import ascendant.core.commands.AbstractPlayerCommand;
import ascendant.core.scaling.EntityDamageReceiveMultiplier;
import ascendant.core.util.EventNotificationWrapper;
import com.hypixel.hytale.component.Store;
import com.hypixel.hytale.server.core.command.system.CommandContext;
import com.hypixel.hytale.server.core.universe.PlayerRef;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;
import org.checkerframework.checker.nullness.compatqual.NonNullDecl;

import java.util.UUID;

public final class TestAttackToggleCommand extends AbstractPlayerCommand {

    public TestAttackToggleCommand(String commandName, String commandDescription, String commandPermission) {
        super(commandName, commandDescription, commandPermission);
    }

    @Override
    protected void executeOnWorldThread(@NonNullDecl PlayerRef playerRef, @NonNullDecl Store<EntityStore> store, @NonNullDecl UUID playerUuid, @NonNullDecl CommandContext commandContext) {
        boolean enabled = EntityDamageReceiveMultiplier.toggleDebugMaxAttack(playerUuid);
        String message = enabled ? "Debug attack: max damage enabled." : "Debug attack: max damage disabled.";
        EventNotificationWrapper.sendMinorEventNotification(playerRef, commandContext, message);
    }
}
