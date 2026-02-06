package ascendant.core.util;

import com.hypixel.hytale.component.CommandBuffer;
import com.hypixel.hytale.component.Holder;
import com.hypixel.hytale.component.Ref;
import com.hypixel.hytale.component.Store;
import com.hypixel.hytale.math.vector.Vector3d;
import com.hypixel.hytale.server.core.entity.entities.Player;
import com.hypixel.hytale.server.core.modules.entity.component.TransformComponent;
import com.hypixel.hytale.server.core.modules.entity.damage.Damage;
import com.hypixel.hytale.server.core.universe.world.World;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.util.UUID;

public final class NearestPlayerFinder {
    private NearestPlayerFinder() {
    }

    @Nullable
    public static Player findNearestPlayer(
            @Nonnull World world,
            @Nonnull Store<EntityStore> store,
            @Nonnull Ref<EntityStore> targetRef,
            float maxRadiusSq
    ) {
        Vector3d position = getPosition(store, targetRef);
        return position == null ? null : findNearestPlayer(world, store, position, maxRadiusSq);
    }

    @Nullable
    public static Player findNearestPlayer(
            @Nonnull World world,
            @Nonnull Store<EntityStore> store,
            @Nonnull Holder<EntityStore> targetHolder,
            float maxRadiusSq
    ) {
        Vector3d position = getPosition(targetHolder);
        return position == null ? null : findNearestPlayer(world, store, position, maxRadiusSq);
    }

    @SuppressWarnings("removal")
    @Nullable
    public static Player findNearestPlayer(
            @Nonnull World world,
            @Nonnull Store<EntityStore> store,
            @Nonnull Vector3d targetPos,
            float maxRadiusSq
    ) {
        if (maxRadiusSq <= 0.0f) {
            return null;
        }

        Player nearest = null;
        double best = Double.MAX_VALUE;

        for (Player p : world.getPlayers()) {
            Ref<EntityStore> pref = p.getReference();
            if (pref == null || !pref.isValid()) {
                continue;
            }

            Vector3d ppos = getPosition(store, pref);
            if (ppos == null) {
                continue;
            }

            double d2 = distanceSq(ppos, targetPos);
            if (d2 <= (double) maxRadiusSq && d2 < best) {
                best = d2;
                nearest = p;
            }
        }

        return nearest;
    }

    @Nullable
    private static Vector3d getPosition(@Nonnull Holder<EntityStore> holder) {
        TransformComponent tc = holder.getComponent(TransformComponent.getComponentType());
        return tc != null ? tc.getPosition() : null;
    }

    @Nullable
    private static Vector3d getPosition(@Nonnull Store<EntityStore> store, @Nonnull Ref<EntityStore> ref) {
        TransformComponent tc = store.getComponent(ref, TransformComponent.getComponentType());
        return tc != null ? tc.getPosition() : null;
    }

    private static double distanceSq(@Nonnull Vector3d a, @Nonnull Vector3d b) {
        double dx = a.getX() - b.getX();
        double dy = a.getY() - b.getY();
        double dz = a.getZ() - b.getZ();
        return dx * dx + dy * dy + dz * dz;
    }

    @Nullable
    @SuppressWarnings("removal")
    public static UUID resolveRelevantPlayerUuid(
            Damage damage,
            Store<EntityStore> store,
            CommandBuffer<EntityStore> commandBuffer,
            Ref<EntityStore> victimRef,
            float maxRadiusSq
            ) {
        UUID attacker = DamageRef.resolveAttackerPlayerUuid(damage, store);
        if (attacker != null) {
            return attacker;
        }

        if (maxRadiusSq <= 0.0f) {
            return null;
        }

        World world = ((EntityStore) commandBuffer.getExternalData()).getWorld();
        Player nearest = NearestPlayerFinder.findNearestPlayer(world, store, victimRef, maxRadiusSq);
        return nearest != null ? nearest.getUuid() : null;
    }
}
