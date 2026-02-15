package ascendant.core.commands.debug;

import ascendant.core.commands.AbstractPlayerCommand;
import com.hypixel.hytale.component.Ref;
import com.hypixel.hytale.component.RemoveReason;
import com.hypixel.hytale.component.Store;
import com.hypixel.hytale.server.core.Message;
import com.hypixel.hytale.server.core.command.system.CommandContext;
import com.hypixel.hytale.server.core.modules.entity.item.ItemComponent;
import com.hypixel.hytale.server.core.universe.PlayerRef;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;
import org.checkerframework.checker.nullness.compatqual.NonNullDecl;

import java.util.UUID;
import java.util.concurrent.atomic.LongAdder;

public final class ClearDroppedItemsCommand extends AbstractPlayerCommand {

    public ClearDroppedItemsCommand(String commandName, String commandDescription, String commandPermission) {
        super(commandName, commandDescription, commandPermission);
    }

    @Override
    protected void executeOnWorldThread(@NonNullDecl PlayerRef playerRef, @NonNullDecl Store<EntityStore> store, @NonNullDecl UUID playerUuid, @NonNullDecl CommandContext commandContext) {
        LongAdder removed = new LongAdder();
        store.forEachEntityParallel((index, archetypeChunk, commandBuffer) -> {
            if (!archetypeChunk.getArchetype().contains(ItemComponent.getComponentType())) {
                return;
            }
            Ref<EntityStore> ref = archetypeChunk.getReferenceTo(index);
            if (ref.isValid()) {
                commandBuffer.removeEntity(ref, RemoveReason.REMOVE);
                removed.increment();
            }
        });
        playerRef.sendMessage(Message.raw("Removed " + removed.sum() + " dropped items."));
    }
}
