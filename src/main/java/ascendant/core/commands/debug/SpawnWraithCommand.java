package ascendant.core.commands.debug;

import ascendant.core.commands.AbstractPlayerCommand;
import ascendant.core.util.EventNotificationWrapper;
import com.hypixel.hytale.component.Ref;
import com.hypixel.hytale.component.Store;
import com.hypixel.hytale.math.vector.Vector3d;
import com.hypixel.hytale.math.vector.Vector3f;
import com.hypixel.hytale.server.core.command.system.CommandContext;
import com.hypixel.hytale.server.core.command.system.arguments.system.OptionalArg;
import com.hypixel.hytale.server.core.command.system.arguments.types.ArgTypes;
import com.hypixel.hytale.server.core.modules.entity.component.TransformComponent;
import com.hypixel.hytale.server.core.universe.PlayerRef;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;
import com.hypixel.hytale.server.npc.NPCPlugin;
import it.unimi.dsi.fastutil.Pair;
import org.checkerframework.checker.nullness.compatqual.NonNullDecl;

import java.util.UUID;

public final class SpawnWraithCommand extends AbstractPlayerCommand {

    private static final String ROLE_NAME = "Wraith";
    private static final String NO_FLOCK = "";

    private final OptionalArg<Integer> countArg;

    public SpawnWraithCommand(String commandName, String commandDescription, String commandPermission) {
        super(commandName, commandDescription, commandPermission);
        this.countArg = withOptionalArg("count", "Number of Wraiths to spawn", ArgTypes.INTEGER);
    }

    @Override
    protected void executeOnWorldThread(@NonNullDecl PlayerRef playerRef, @NonNullDecl Store<EntityStore> store, @NonNullDecl UUID playerUuid, @NonNullDecl CommandContext commandContext) {
        Ref<EntityStore> ref = playerRef.getReference();
        if (ref == null || !ref.isValid()) {
            EventNotificationWrapper.sendMinorEventNotification(playerRef, commandContext, "Player is not in a valid world.");
            return;
        }

        TransformComponent transform = store.getComponent(ref, TransformComponent.getComponentType());
        if (transform == null) {
            EventNotificationWrapper.sendMinorEventNotification(playerRef, commandContext, "Missing player transform.");
            return;
        }

        Vector3d position = transform.getPosition();
        Vector3f rotation = transform.getRotation();
        if (position == null) {
            EventNotificationWrapper.sendMinorEventNotification(playerRef, commandContext, "Missing player position.");
            return;
        }
        if (rotation == null) {
            rotation = new Vector3f(0f, 0f, 0f);
        }

        NPCPlugin npcPlugin = NPCPlugin.get();
        if (npcPlugin == null) {
            EventNotificationWrapper.sendMinorEventNotification(playerRef, commandContext, "NPC plugin not available.");
            return;
        }

        if (!npcPlugin.hasRoleName(ROLE_NAME)) {
            EventNotificationWrapper.sendMinorEventNotification(playerRef, commandContext, "Role not found: " + ROLE_NAME);
            return;
        }

        int count = 1;
        if (commandContext.provided(countArg)) {
            Integer provided = commandContext.get(countArg);
            if (provided != null) {
                count = Math.max(1, provided);
            }
        }

        int spawned = 0;
        double baseX = position.getX() + 1.0;
        double baseY = position.getY();
        double baseZ = position.getZ() + 1.0;
        int stride = Math.max(1, (int) Math.ceil(Math.sqrt(count)));
        double spacing = 1.5;

        for (int i = 0; i < count; i++) {
            int row = i / stride;
            int col = i % stride;
            Vector3d spawnPos = new Vector3d(baseX + (col * spacing), baseY, baseZ + (row * spacing));
            Pair<Ref<EntityStore>, ?> result = npcPlugin.spawnNPC(store, ROLE_NAME, NO_FLOCK, spawnPos, rotation);
            if (result != null && result.first() != null && result.first().isValid()) {
                spawned++;
            }
        }

        if (spawned <= 0) {
            EventNotificationWrapper.sendMinorEventNotification(playerRef, commandContext, "Failed to spawn Wraith.");
        } else if (spawned == 1) {
            EventNotificationWrapper.sendMinorEventNotification(playerRef, commandContext, "Spawned Wraith.");
        } else {
            EventNotificationWrapper.sendMinorEventNotification(playerRef, commandContext, "Spawned " + spawned + " Wraiths.");
        }
    }
}
