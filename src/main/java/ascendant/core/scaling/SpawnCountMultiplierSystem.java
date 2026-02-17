package ascendant.core.scaling;

import ascendant.core.config.DifficultyIO;
import ascendant.core.config.DifficultyManager;
import ascendant.core.util.Logging;
import ascendant.core.util.NearestPlayerFinder;
import ascendant.core.util.ReflectionHelper;
import com.hypixel.hytale.component.*;
import com.hypixel.hytale.component.query.Query;
import com.hypixel.hytale.component.system.RefSystem;
import com.hypixel.hytale.math.vector.Vector3d;
import com.hypixel.hytale.math.vector.Vector3f;
import com.hypixel.hytale.server.core.entity.entities.Player;
import com.hypixel.hytale.server.core.modules.entity.component.TransformComponent;
import com.hypixel.hytale.server.core.universe.world.World;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;
import com.hypixel.hytale.server.npc.NPCPlugin;
import com.hypixel.hytale.server.npc.entities.NPCEntity;
import it.unimi.dsi.fastutil.Pair;
import org.checkerframework.checker.nullness.qual.NonNull;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.lang.reflect.Constructor;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.ThreadLocalRandom;
import java.util.concurrent.atomic.AtomicBoolean;

@SuppressWarnings("removal")
public final class SpawnCountMultiplierSystem extends RefSystem<EntityStore> {

    private static final String NO_FLOCK = "";
    private static final Set<Ref<EntityStore>> SKIP_REFS = ConcurrentHashMap.newKeySet();
    private static final ThreadLocal<Boolean> SPAWN_GUARD = ThreadLocal.withInitial(() -> Boolean.FALSE);
    private static final AtomicBoolean LOGGED_DISABLED = new AtomicBoolean(false);
    private static final Set<String> LOGGED_MISSING_MULTIPLIER = ConcurrentHashMap.newKeySet();
    private static final Set<String> LOGGED_FALLBACK_TIER = ConcurrentHashMap.newKeySet();
    private static final Set<String> LOGGED_FLOCK_ROLE_SKIP = ConcurrentHashMap.newKeySet();
    private static final Set<String> LOGGED_FLOCK_REMOVE_SKIP = ConcurrentHashMap.newKeySet();
    private static final AtomicBoolean LOGGED_FLOCK_COMPONENT_MISSING = new AtomicBoolean(false);
    private static final ComponentType<EntityStore, ?> FLOCK_MEMBERSHIP_TYPE =
            resolveComponentType("com.hypixel.hytale.server.flock.FlockMembership");
    private static final ComponentType<EntityStore, ?> FLOCK_COMPONENT_TYPE = resolveFlockComponentType();
    private static final ConcurrentHashMap<World, ConcurrentLinkedQueue<PendingRemoval>> PENDING_REMOVALS =
            new ConcurrentHashMap<>();
    private static final int REMOVAL_DELAY_TICKS = 5;
    private static final Constructor<?> COMMAND_BUFFER_CTOR =
            ReflectionHelper.getDeclaredConstructor(CommandBuffer.class, Store.class);
    private static final Method COMMAND_BUFFER_CONSUME =
            ReflectionHelper.getDeclaredMethod(CommandBuffer.class, "consume");

    private final float _fallbackRadiusSq;
    private final boolean _allowSpawnCountMultiplier;

    public SpawnCountMultiplierSystem() {
        double radius = DifficultyManager.getFromConfig(DifficultyIO.PLAYER_DISTANCE_RADIUS_TO_CHECK);
        float r = (float) Math.max(0.0, radius);
        _fallbackRadiusSq = r * r;
        _allowSpawnCountMultiplier = DifficultyManager.getFromConfig(DifficultyIO.ALLOW_SPAWN_COUNT_MULTIPLIER);
        if (!_allowSpawnCountMultiplier && LOGGED_DISABLED.compareAndSet(false, true)) {
            Logging.debug("[SPAWNCOUNT] disabled by config");
        }
    }

    private static boolean shouldSkip(Ref<EntityStore> ref) {
        boolean guard = Boolean.TRUE.equals(SPAWN_GUARD.get());
        boolean skipped = SKIP_REFS.remove(ref);
        return guard || skipped;
    }

    private static int computeExtraCount(double multiplier) {
        if (!Double.isFinite(multiplier) || multiplier <= 1.0) {
            return 0;
        }
        double extra = multiplier - 1.0;
        int guaranteed = (int) Math.floor(extra);
        double fractional = extra - guaranteed;
        if (fractional <= 0.0) {
            return guaranteed;
        }
        return guaranteed + (ThreadLocalRandom.current().nextDouble() < fractional ? 1 : 0);
    }

    private static boolean hasExplicitMultiplier(String tierId) {
        if (tierId == null || tierId.isBlank()) {
            return false;
        }
        return DifficultyManager.getConfig()
                .get("tiers." + tierId + "." + DifficultyIO.SETTING_SPAWN_COUNT_MULTIPLIER)
                .isPresent();
    }

    private static boolean isFlockMember(
            @Nonnull Ref<EntityStore> ref,
            @Nonnull Store<EntityStore> store,
            @Nonnull CommandBuffer<EntityStore> commandBuffer
    ) {
        return hasFlockMembership(ref, store, commandBuffer);
    }

    @SuppressWarnings("unchecked")
    private static ComponentType<EntityStore, ?> resolveComponentType(@Nonnull String className) {
        Class<?> cls = ReflectionHelper.resolveClass(className);
        if (cls == null) {
            return null;
        }
        Method m = ReflectionHelper.getNoArgMethod(cls, "getComponentType");
        if (m == null) {
            return null;
        }
        try {
            Object type = m.invoke(null);
            return (ComponentType<EntityStore, ?>) type;
        } catch (Throwable ignored) {
            return null;
        }
    }

    private static ComponentType<EntityStore, ?> resolveFlockComponentType() {
        ComponentType<EntityStore, ?> type = resolveComponentType("com.hypixel.hytale.server.flock.Flock");
        if (type != null) {
            return type;
        }
        type = resolveComponentType("com.hypixel.hytale.server.flock.FlockComponent");
        if (type != null) {
            return type;
        }
        return resolveComponentType("com.hypixel.hytale.server.flock.FlockLeader");
    }

    private static boolean hasComponent(
            @Nonnull Ref<EntityStore> ref,
            @Nonnull Store<EntityStore> store,
            @Nonnull CommandBuffer<EntityStore> commandBuffer,
            @Nullable ComponentType<EntityStore, ?> type
    ) {
        if (type == null) {
            return false;
        }
        try {
            Object component = commandBuffer.getComponent(ref, type);
            if (component != null) {
                return true;
            }
        } catch (Throwable ignored) {
            return false;
        }
        try {
            Object component = store.getComponent(ref, type);
            return component != null;
        } catch (Throwable ignored) {
            return false;
        }
    }

    private static boolean hasFlockMembership(
            @Nonnull Ref<EntityStore> ref,
            @Nonnull Store<EntityStore> store,
            @Nonnull CommandBuffer<EntityStore> commandBuffer
    ) {
        ComponentType<EntityStore, ?> type = FLOCK_MEMBERSHIP_TYPE;
        if (type == null) {
            if (LOGGED_FLOCK_COMPONENT_MISSING.compareAndSet(false, true)) {
                Logging.debug("[SPAWNCOUNT] flock membership component not available; cannot skip flock spawns");
            }
            return false;
        }
        return hasComponent(ref, store, commandBuffer, type);
    }

    private static boolean hasFlockComponent(
            @Nonnull Ref<EntityStore> ref,
            @Nonnull Store<EntityStore> store,
            @Nonnull CommandBuffer<EntityStore> commandBuffer
    ) {
        return hasComponent(ref, store, commandBuffer, FLOCK_COMPONENT_TYPE);
    }

    private static boolean isFlockRelated(
            @Nonnull Ref<EntityStore> ref,
            @Nonnull Store<EntityStore> store,
            @Nonnull CommandBuffer<EntityStore> commandBuffer
    ) {
        return getFlockFlags(ref, store, commandBuffer) != 0;
    }

    private static boolean tryRemoveEntity(
            @Nonnull Ref<EntityStore> ref,
            @Nonnull Store<EntityStore> store,
            @Nonnull CommandBuffer<EntityStore> commandBuffer,
            @Nonnull String tierId
    ) {
        if (!ref.isValid() || store.isShutdown()) {
            return false;
        }
        int flockFlags = getFlockFlags(ref, store, commandBuffer);
        if (flockFlags != 0) {
            maybeLogFlockRemoveSkip(ref, store, commandBuffer, tierId, flockFlags);
            return false;
        }
        enqueueRemoval(store, ref, tierId);
        return true;
    }

    @Nullable
    private static String resolveRoleName(
            @Nonnull Ref<EntityStore> ref,
            @Nonnull Store<EntityStore> store,
            @Nullable CommandBuffer<EntityStore> commandBuffer
    ) {
        NPCEntity npc = getNpcEntity(ref, store, commandBuffer);
        if (npc == null) {
            return null;
        }
        String roleName = npc.getRoleName();
        return roleName == null || roleName.isBlank() ? null : roleName;
    }

    @Nullable
    private static NPCEntity getNpcEntity(
            @Nonnull Ref<EntityStore> ref,
            @Nonnull Store<EntityStore> store,
            @Nullable CommandBuffer<EntityStore> commandBuffer
    ) {
        ComponentType<EntityStore, NPCEntity> npcType = NPCEntity.getComponentType();
        if (npcType == null) {
            return null;
        }
        NPCEntity npc = null;
        if (commandBuffer != null) {
            try {
                npc = commandBuffer.getComponent(ref, npcType);
            } catch (Throwable ignored) {
                return null;
            }
        }
        if (npc == null) {
            try {
                npc = store.getComponent(ref, npcType);
            } catch (Throwable ignored) {
                return null;
            }
        }
        return npc;
    }

    private static void maybeLogFlockRemoveSkip(
            @Nonnull Ref<EntityStore> ref,
            @Nonnull Store<EntityStore> store,
            @Nonnull CommandBuffer<EntityStore> commandBuffer,
            @Nonnull String tierId,
            int flockFlags
    ) {
        String roleName = resolveRoleName(ref, store, commandBuffer);
        String key = roleName != null ? roleName : ref.toString();
        if (LOGGED_FLOCK_REMOVE_SKIP.add(key)) {
            Logging.debug(String.format(Locale.ROOT,
                    "[SPAWNCOUNT] tier=%s role=%s flock entity detected (%s); skipping removal",
                    tierId, roleName != null ? roleName : "<unknown>", flockFlagsToString(flockFlags)));
        }
    }

    static void drainPendingRemovals(@Nonnull Store<EntityStore> store, int tick) {
        World world = store.getExternalData().getWorld();
        ConcurrentLinkedQueue<PendingRemoval> queue = PENDING_REMOVALS.get(world);
        if (queue == null || queue.isEmpty() || store.isShutdown()) {
            if (queue == null && !PENDING_REMOVALS.isEmpty()) {
                Logging.debug(String.format(Locale.ROOT,
                        "[SPAWNCOUNT] removal drain: no queue for world=%s tick=%d mapSize=%d",
                        world, tick, PENDING_REMOVALS.size()));
            }
            return;
        }
        Logging.debug(String.format(Locale.ROOT,
                "[SPAWNCOUNT] removal drain start tick=%d world=%s queue=%d",
                tick, world, queue.size()));
        while (true) {
            PendingRemoval pending = queue.peek();
            if (pending == null) {
                break;
            }
            if (tick - pending.enqueuedTick < REMOVAL_DELAY_TICKS) {
                break;
            }
            queue.poll();
            if (pending.ref == null || !pending.ref.isValid()) {
                Logging.debug(String.format(Locale.ROOT,
                        "[SPAWNCOUNT] removal skip invalid ref tier=%s ref=%s",
                        pending.tierId, pending.ref));
                continue;
            }
            world.execute(() -> removeQueued(store, pending));
        }
        if (queue.isEmpty()) {
            PENDING_REMOVALS.remove(world, queue);
        }
    }

    static void logRemovalTick(@Nonnull Store<EntityStore> store, int tick) {
        World world = store.getExternalData().getWorld();
        ConcurrentLinkedQueue<PendingRemoval> queue = PENDING_REMOVALS.get(world);
        int size = queue == null ? 0 : queue.size();
        Logging.debug(String.format(Locale.ROOT,
                "[SPAWNCOUNT] removal tick=%d world=%s queue=%d",
                tick, world, size));
    }

    private static void enqueueRemoval(
            @Nonnull Store<EntityStore> store,
            @Nonnull Ref<EntityStore> ref,
            @Nonnull String tierId
    ) {
        World world = store.getExternalData().getWorld();
        int tick = SpawnCountRemovalTickSystem.getCurrentTick();
        PendingRemoval pending = new PendingRemoval(ref, tierId, Math.max(0, tick));
        PENDING_REMOVALS.computeIfAbsent(world, ignored -> new ConcurrentLinkedQueue<>()).add(pending);
        Logging.debug(String.format(Locale.ROOT,
                "[SPAWNCOUNT] queued removal tier=%s role=%s tick=%d ref=%s world=%s",
                tierId,
                resolveRoleName(ref, store, null),
                Math.max(0, tick),
                ref,
                world));
    }

    @Nullable
    private static CommandBuffer<EntityStore> newCommandBuffer(@Nonnull Store<EntityStore> store) {
        if (COMMAND_BUFFER_CTOR == null) {
            Logging.debug("[SPAWNCOUNT] CommandBuffer ctor not found; cannot remove queued entities");
            return null;
        }
        try {
            Object created = COMMAND_BUFFER_CTOR.newInstance(store);
            @SuppressWarnings("unchecked")
            CommandBuffer<EntityStore> buffer = (CommandBuffer<EntityStore>) created;
            return buffer;
        } catch (Throwable ignored) {
            Logging.debug("[SPAWNCOUNT] CommandBuffer ctor failed; cannot remove queued entities");
            return null;
        }
    }

    private static void consume(@Nullable CommandBuffer<?> buffer) {
        if (buffer == null || COMMAND_BUFFER_CONSUME == null) {
            return;
        }
        try {
            COMMAND_BUFFER_CONSUME.invoke(buffer);
        } catch (Throwable ignored) {
        }
    }

    private static int getFlockFlags(
            @Nonnull Ref<EntityStore> ref,
            @Nonnull Store<EntityStore> store,
            @Nonnull CommandBuffer<EntityStore> commandBuffer
    ) {
        int flags = 0;
        if (hasFlockMembership(ref, store, commandBuffer)) {
            flags |= 1;
        }
        if (hasFlockComponent(ref, store, commandBuffer)) {
            flags |= 2;
        }
        return flags;
    }

    private static String flockFlagsToString(int flags) {
        if (flags == 0) {
            return "none";
        }
        StringBuilder sb = new StringBuilder();
        if ((flags & 1) != 0) {
            sb.append("membership");
        }
        if ((flags & 2) != 0) {
            if (!sb.isEmpty()) sb.append(",");
            sb.append("component");
        }
        return sb.toString();
    }

    private static void removeQueued(@Nonnull Store<EntityStore> store, @Nonnull PendingRemoval pending) {
        if (store.isShutdown()) {
            return;
        }
        CommandBuffer<EntityStore> buffer = newCommandBuffer(store);
        if (buffer == null) {
            Logging.debug("[SPAWNCOUNT] removal task skipped: CommandBuffer unavailable");
            return;
        }
        if (pending.ref == null || !pending.ref.isValid()) {
            Logging.debug(String.format(Locale.ROOT,
                    "[SPAWNCOUNT] removal task skip invalid ref tier=%s ref=%s",
                    pending.tierId, pending.ref));
            return;
        }
        int flockFlags = getFlockFlags(pending.ref, store, buffer);
        if (flockFlags != 0) {
            maybeLogFlockRemoveSkip(pending.ref, store, buffer, pending.tierId, flockFlags);
            Logging.debug(String.format(Locale.ROOT,
                    "[SPAWNCOUNT] removal task skip flock tier=%s role=%s flags=%s ref=%s",
                    pending.tierId,
                    resolveRoleName(pending.ref, store, buffer),
                    flockFlagsToString(flockFlags),
                    pending.ref));
            return;
        }
        buffer.removeEntity(pending.ref, RemoveReason.REMOVE);
        consume(buffer);
        Logging.debug(String.format(Locale.ROOT,
                "[SPAWNCOUNT] removed queued tier=%s role=%s ref=%s",
                pending.tierId,
                resolveRoleName(pending.ref, store, buffer),
                pending.ref));
    }

    private static void spawnExtras(
            Store<EntityStore> store,
            String roleName,
            Vector3f rotation,
            List<Vector3d> positions,
            String tierId,
            String source,
            double multiplier
    ) {
        NPCPlugin npcPlugin = NPCPlugin.get();
        if (npcPlugin == null || !npcPlugin.hasRoleName(roleName)) {
            return;
        }
        if (positions == null || positions.isEmpty()) {
            return;
        }
        SPAWN_GUARD.set(Boolean.TRUE);
        int spawned = 0;
        try {
            for (Vector3d spawnPos : positions) {
                Pair<Ref<EntityStore>, ?> result = npcPlugin.spawnNPC(store, roleName, NO_FLOCK, spawnPos, rotation);
                if (result == null || result.first() == null || !result.first().isValid()) {
                    continue;
                }
                SKIP_REFS.add(result.first());
                spawned++;
            }
        } finally {
            SPAWN_GUARD.set(Boolean.FALSE);
        }
        Logging.debug(String.format(Locale.ROOT,
                "[SPAWNCOUNT] tier=%s source=%s multiplier=%.3f spawned=%d/%d extras",
                tierId, source, multiplier, spawned, positions.size()));
    }

    @Nonnull
    @Override
    public Query<EntityStore> getQuery() {
        return Query.and(
                NPCEntity.getComponentType(),
                TransformComponent.getComponentType(),
                Query.not(Player.getComponentType())
        );
    }

    @Override
    public void onEntityAdded(
            @Nonnull Ref<EntityStore> ref,
            @Nonnull AddReason reason,
            @Nonnull Store<EntityStore> store,
            @Nonnull CommandBuffer<EntityStore> commandBuffer
    ) {
        if (reason != AddReason.SPAWN) {
            return;
        }
        if (!_allowSpawnCountMultiplier) {
            return;
        }
        if (shouldSkip(ref)) {
            return;
        }

        TierResolution tierResolution = resolveTier(store, ref);
        String tierId = tierResolution.tierId;
        if (tierId == null || tierId.isBlank()) {
            return;
        }
        if ("default".equals(tierResolution.source) && LOGGED_FALLBACK_TIER.add(tierId)) {
            Logging.debug(String.format(Locale.ROOT,
                    "[SPAWNCOUNT] using default tier=%s (no player within radius)",
                    tierId));
        }

        double multiplier = DifficultyManager.getSettings().get(tierId, DifficultyIO.SETTING_SPAWN_COUNT_MULTIPLIER);
        if (!hasExplicitMultiplier(tierId) && LOGGED_MISSING_MULTIPLIER.add(tierId)) {
            Logging.debug(String.format(Locale.ROOT,
                    "[SPAWNCOUNT] missing spawn_count_multiplier for tier=%s; defaulting to %.3f",
                    tierId, multiplier));
        }
        if (!Double.isFinite(multiplier)) {
            Logging.debug(String.format(Locale.ROOT,
                    "[SPAWNCOUNT] tier=%s multiplier=NaN; skipping",
                    tierId));
            return;
        }
        if (multiplier <= 0.0) {
            boolean queued = tryRemoveEntity(ref, store, commandBuffer, tierId);
            Logging.debug(String.format(Locale.ROOT,
                    "[SPAWNCOUNT] tier=%s multiplier=%.3f action=%s ref=%s",
                    tierId, multiplier, queued ? "queue" : "skip", ref));
            return;
        }
        if (multiplier < 1.0) {
            double roll = ThreadLocalRandom.current().nextDouble();
            boolean keep = roll < multiplier;
            String action;
            if (keep) {
                action = "keep";
            } else {
                action = tryRemoveEntity(ref, store, commandBuffer, tierId) ? "queue" : "skip";
            }
            Logging.debug(String.format(Locale.ROOT,
                    "[SPAWNCOUNT] tier=%s multiplier=%.3f roll=%.3f action=%s ref=%s",
                    tierId, multiplier, roll, action, ref));
            return;
        }

        int extraCount = computeExtraCount(multiplier);
        if (extraCount <= 0) {
            return;
        }

        ComponentType<EntityStore, NPCEntity> npcEntityComponentType = NPCEntity.getComponentType();
        if (npcEntityComponentType == null) {
            return;
        }
        NPCEntity npc = commandBuffer.getComponent(ref, npcEntityComponentType);
        if (npc == null) {
            npc = store.getComponent(ref, npcEntityComponentType);
        }
        if (npc == null) {
            return;
        }

        String roleName = npc.getRoleName();
        if (roleName == null || roleName.isBlank()) {
            return;
        }
        if (isFlockMember(ref, store, commandBuffer)) {
            if (LOGGED_FLOCK_ROLE_SKIP.add(roleName)) {
                Logging.debug(String.format(Locale.ROOT,
                        "[SPAWNCOUNT] role=%s flock member detected; skipping spawn multiplier",
                        roleName));
            }
            return;
        }
        Logging.debug(String.format(Locale.ROOT,
                "[SPAWNCOUNT] tier=%s source=%s multiplier=%.3f extra=%d role=%s",
                tierId, tierResolution.source, multiplier, extraCount, roleName));

        TransformComponent transform = commandBuffer.getComponent(ref, TransformComponent.getComponentType());
        if (transform == null) {
            transform = store.getComponent(ref, TransformComponent.getComponentType());
        }
        if (transform == null) {
            return;
        }

        Vector3d position = transform.getPosition();
        Vector3f rotation = transform.getRotation();

        double baseX = position.getX() + 1.0;
        double baseY = position.getY();
        double baseZ = position.getZ() + 1.0;
        int stride = Math.max(1, (int) Math.ceil(Math.sqrt(extraCount)));
        double spacing = 1.5;

        List<Vector3d> spawnPositions = new ArrayList<>(extraCount);
        for (int i = 0; i < extraCount; i++) {
            int row = i / stride;
            int col = i % stride;
            spawnPositions.add(new Vector3d(baseX + (col * spacing), baseY, baseZ + (row * spacing)));
        }

        World world = store.getExternalData().getWorld();
        Vector3f rotationCopy = new Vector3f(rotation.getX(), rotation.getY(), rotation.getZ());
        world.execute(() -> spawnExtras(store, roleName, rotationCopy, spawnPositions, tierId, tierResolution.source, multiplier));
    }

    @Override
    public void onEntityRemove(@NonNull Ref<EntityStore> ref, @NonNull RemoveReason removeReason, @NonNull Store<EntityStore> store, @NonNull CommandBuffer<EntityStore> commandBuffer) {

    }

    private TierResolution resolveTier(@Nonnull Store<EntityStore> store, @Nonnull Ref<EntityStore> ref) {
        String tierId = NearestPlayerHealthScaleSystem.getSpawnTier(store, ref);
        if (tierId != null && !tierId.isBlank()) {
            return new TierResolution(tierId, "component");
        }

        World world = store.getExternalData().getWorld();
        Player nearest = NearestPlayerFinder.findNearestPlayer(world, store, ref, _fallbackRadiusSq);
        if (nearest != null) {
            String nearestTier = DifficultyManager.getDifficulty(nearest.getUuid());
            return new TierResolution(nearestTier, "nearest");
        }
        if (!world.getPlayers().isEmpty()) {
            Player nearestAny = NearestPlayerFinder.findNearestPlayer(world, store, ref, Float.MAX_VALUE);
            if (nearestAny != null) {
                String nearestTier = DifficultyManager.getDifficulty(nearestAny.getUuid());
                return new TierResolution(nearestTier, "nearestAny");
            }
        }

        String fallback = DifficultyManager.getFromConfig(DifficultyIO.DEFAULT_DIFFICULTY);
        return new TierResolution(fallback, "default");
    }

    private record PendingRemoval(
            Ref<EntityStore> ref,
            String tierId,
            int enqueuedTick
    ) {
    }

    private static final class TierResolution {
        private final String tierId;
        private final String source;

        private TierResolution(String tierId, String source) {
            this.tierId = tierId;
            this.source = source;
        }
    }
}
