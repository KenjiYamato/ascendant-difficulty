package ascendant.core.scaling;

import com.hypixel.hytale.component.Store;
import com.hypixel.hytale.component.system.tick.TickingSystem;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;

import javax.annotation.Nonnull;
import java.util.concurrent.atomic.AtomicInteger;

public final class SpawnCountRemovalTickSystem extends TickingSystem<EntityStore> {
    private static final AtomicInteger CURRENT_TICK = new AtomicInteger(0);

    static int getCurrentTick() {
        return CURRENT_TICK.get();
    }

    @Override
    public void tick(float dt, int tick, @Nonnull Store<EntityStore> store) {
        int current = CURRENT_TICK.incrementAndGet();
        if ((current & 0xFF) == 0) {
            SpawnCountMultiplierSystem.logRemovalTick(store, current);
        }
        SpawnCountMultiplierSystem.drainPendingRemovals(store, current);
    }
}
