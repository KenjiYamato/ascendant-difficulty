package ascendant.core.scaling;

import ascendant.core.config.DifficultyIO;
import ascendant.core.config.DifficultyManager;
import ascendant.core.config.DifficultySettings;
import ascendant.core.util.NearestPlayerFinder;
import com.hypixel.hytale.component.*;
import com.hypixel.hytale.component.query.Query;
import com.hypixel.hytale.component.system.RefSystem;
import com.hypixel.hytale.server.core.entity.entities.Player;
import com.hypixel.hytale.server.core.modules.entity.AllLegacyLivingEntityTypesQuery;
import com.hypixel.hytale.server.core.modules.entity.component.TransformComponent;
import com.hypixel.hytale.server.core.universe.world.World;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;
import com.hypixel.hytale.server.npc.entities.NPCEntity;
import org.checkerframework.checker.nullness.qual.NonNull;

import javax.annotation.Nonnull;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentLinkedQueue;

@SuppressWarnings("removal")
public final class EntityEliteSpawn extends RefSystem<EntityStore> {

    private static final ConcurrentHashMap<Store<EntityStore>, SpawnQueueState> QUEUES = new ConcurrentHashMap<>();
    private final boolean _allowEliteSpawnModifier;
    private final boolean _integrationEliteMobs;

    public EntityEliteSpawn() {
        _allowEliteSpawnModifier = DifficultyManager.getFromConfig(DifficultyIO.ALLOW_ELITE_SPAWN_MODIFIER);
        _integrationEliteMobs = DifficultyManager.getFromConfig(DifficultyIO.INTEGRATION_ELITE_MOBS);
    }

    static void enqueue(
            Store<EntityStore> store,
            Ref<EntityStore> ref,
            NPCEntity npc,
            double mult,
            double uncommon,
            double rare,
            double legendary,
            long enqueuedAtNs
    ) {
        if (store == null || ref == null) {
            return;
        }
        SpawnQueueState state = QUEUES.computeIfAbsent(store, _s -> new SpawnQueueState());
        state.queue.add(new SpawnTask(ref, npc, mult, uncommon, rare, legendary, enqueuedAtNs));
    }

    static SpawnQueueState getQueueState(Store<EntityStore> store) {
        return store == null ? null : QUEUES.get(store);
    }

    static void cleanupQueueState(Store<EntityStore> store, SpawnQueueState state) {
        if (store == null || state == null) {
            return;
        }
        if (store.isShutdown() || state.queue.isEmpty()) {
            QUEUES.remove(store, state);
        }
    }

    @Nonnull
    @Override
    public Query<EntityStore> getQuery() {
        return AllLegacyLivingEntityTypesQuery.INSTANCE;
    }

    @Override
    public void onEntityAdded(@Nonnull Ref<EntityStore> ref,
                              @Nonnull AddReason reason,
                              @Nonnull Store<EntityStore> store,
                              @Nonnull CommandBuffer<EntityStore> commandBuffer) {

        if (!_allowEliteSpawnModifier || !_integrationEliteMobs || reason != AddReason.SPAWN) {
            return;
        }
        World world = store.getExternalData().getWorld();
        if (world == null) {
            return;
        }

        NPCEntity npc = commandBuffer.getComponent(ref, NPCEntity.getComponentType());
        if (npc == null) {
            npc = store.getComponent(ref, NPCEntity.getComponentType());
        }
        if (npc == null) {
            return;
        }

        TransformComponent transform = commandBuffer.getComponent(ref, TransformComponent.getComponentType());
        if (transform == null) {
            transform = store.getComponent(ref, TransformComponent.getComponentType());
        }
        if (transform == null) {
            return;
        }

        double radius = DifficultyManager.getFromConfig(DifficultyIO.PLAYER_DISTANCE_RADIUS_TO_CHECK);
        float fallbackRadiusSq;
        if (radius <= 0.0) {
            fallbackRadiusSq = Float.MAX_VALUE;
        } else {
            float r = (float) radius;
            fallbackRadiusSq = r * r;
        }

        Player nearest = NearestPlayerFinder.findNearestPlayer(world, store, transform.getPosition(), fallbackRadiusSq);
        String tierId = nearest != null
                ? DifficultyManager.getDifficulty(nearest.getUuid())
                : DifficultyManager.getFromConfig(DifficultyIO.DEFAULT_DIFFICULTY);

        DifficultySettings settings = DifficultyManager.getSettings();
        double mult = settings.get(tierId, DifficultyIO.SETTING_ELITE_MOBS_CHANCE_MULTIPLIER);
        mult *= 100;
        double uncommon = settings.get(tierId, DifficultyIO.SETTING_ELITE_MOBS_CHANCE_UNCOMMON);
        double rare = settings.get(tierId, DifficultyIO.SETTING_ELITE_MOBS_CHANCE_RARE);
        double legendary = settings.get(tierId, DifficultyIO.SETTING_ELITE_MOBS_CHANCE_LEGENDARY);

        if (uncommon <= 0.0 && rare <= 0.0 && legendary <= 0.0 && mult <= 0.0) {
            return;
        }

        enqueue(store, ref, npc, mult, uncommon, rare, legendary, System.nanoTime());
    }

    @Override
    public void onEntityRemove(@NonNull Ref<EntityStore> ref, @NonNull RemoveReason removeReason, @NonNull Store<EntityStore> store, @NonNull CommandBuffer<EntityStore> commandBuffer) {

    }

    static final class SpawnQueueState {
        final ConcurrentLinkedQueue<SpawnTask> queue = new ConcurrentLinkedQueue<>();
        long lastDrainAtNs = 0L;
    }

    static final class SpawnTask {
        final Ref<EntityStore> ref;
        final NPCEntity npc;
        final double mult;
        final double uncommon;
        final double rare;
        final double legendary;
        final long enqueuedAtNs;

        SpawnTask(
                Ref<EntityStore> ref,
                NPCEntity npc,
                double mult,
                double uncommon,
                double rare,
                double legendary,
                long enqueuedAtNs
        ) {
            this.ref = ref;
            this.npc = npc;
            this.mult = mult;
            this.uncommon = uncommon;
            this.rare = rare;
            this.legendary = legendary;
            this.enqueuedAtNs = enqueuedAtNs;
        }
    }
}
