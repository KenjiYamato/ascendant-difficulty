package ascendant.core.util;

import com.hypixel.hytale.component.Ref;
import com.hypixel.hytale.component.Store;
import com.hypixel.hytale.server.core.universe.PlayerRef;
import com.hypixel.hytale.server.core.universe.world.World;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

public final class PlayerWorldExecutor {
    private PlayerWorldExecutor() {
    }

    public static boolean execute(@Nullable PlayerRef playerRef, @Nonnull Runnable action) {
        if (playerRef == null || !playerRef.isValid()) {
            return false;
        }
        Ref<EntityStore> storeRef = playerRef.getReference();
        if (storeRef == null || !storeRef.isValid()) {
            return false;
        }
        Store<EntityStore> store = storeRef.getStore();
        World world = store.getExternalData().getWorld();
        if (world == null) {
            return false;
        }
        world.execute(action);
        return true;
    }
}
