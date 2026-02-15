package ascendant.core.scaling;

import ascendant.core.config.DifficultyIO;
import ascendant.core.config.DifficultyManager;
import ascendant.core.integration.elitemobs.EliteMobsDifficultySpawner;
import ascendant.core.util.Logging;
import ascendant.core.util.ReflectionHelper;
import com.hypixel.hytale.component.CommandBuffer;
import com.hypixel.hytale.component.Ref;
import com.hypixel.hytale.component.Store;
import com.hypixel.hytale.component.system.tick.TickingSystem;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;

import javax.annotation.Nonnull;
import java.lang.reflect.Constructor;
import java.lang.reflect.Method;
import java.util.Locale;

@SuppressWarnings("removal")
public final class EliteSpawnQueueTickSystem extends TickingSystem<EntityStore> {

    private static final Constructor<?> COMMAND_BUFFER_CTOR =
            ReflectionHelper.getDeclaredConstructor(CommandBuffer.class, Store.class);
    private static final Method COMMAND_BUFFER_CONSUME =
            ReflectionHelper.getDeclaredMethod(CommandBuffer.class, "consume");

    private final boolean _allowEliteSpawnModifier;
    private final boolean _integrationEliteMobs;
    private final boolean _debugLogging;
    private final long _intervalNs;
    private final long _maxDrainNs;
    private final int _maxPerDrain;

    public EliteSpawnQueueTickSystem() {
        _allowEliteSpawnModifier = DifficultyManager.getFromConfig(DifficultyIO.ALLOW_ELITE_SPAWN_MODIFIER);
        _integrationEliteMobs = DifficultyManager.getFromConfig(DifficultyIO.INTEGRATION_ELITE_MOBS);
        _debugLogging = DifficultyManager.getFromConfig(DifficultyIO.ALLOW_DEBUG_LOGGING);

        double intervalMs = DifficultyManager.getFromConfig(DifficultyIO.ELITE_SPAWN_QUEUE_INTERVAL_MS);
        _intervalNs = toNs(intervalMs);

        double maxDrainMs = DifficultyManager.getFromConfig(DifficultyIO.ELITE_SPAWN_QUEUE_MAX_DRAIN_MS);
        _maxDrainNs = maxDrainMs <= 0.0 ? Long.MAX_VALUE : toNs(maxDrainMs);

        int maxPer = DifficultyManager.getFromConfig(DifficultyIO.ELITE_SPAWN_QUEUE_MAX_PER_DRAIN);
        _maxPerDrain = maxPer <= 0 ? Integer.MAX_VALUE : maxPer;
    }

    @Override
    public void tick(float dt, int tick, @Nonnull Store<EntityStore> store) {
        if (!_allowEliteSpawnModifier || !_integrationEliteMobs) {
            return;
        }

        EntityEliteSpawn.SpawnQueueState state = EntityEliteSpawn.getQueueState(store);
        if (state == null) {
            return;
        }
        if (store.isShutdown()) {
            EntityEliteSpawn.cleanupQueueState(store, state);
            return;
        }
        if (state.queue.isEmpty()) {
            EntityEliteSpawn.cleanupQueueState(store, state);
            return;
        }

        int remaining = _maxPerDrain;

        long nowNs = System.nanoTime();
        if (_intervalNs > 0 && state.lastDrainAtNs > 0 && nowNs - state.lastDrainAtNs < _intervalNs) {
            return;
        }

        CommandBuffer<EntityStore> commandBuffer = newCommandBuffer(store);
        if (commandBuffer == null) {
            return;
        }

        boolean attemptedRoll = false;
        boolean drainedAny = false;
        long deadlineNs = _maxDrainNs == Long.MAX_VALUE ? Long.MAX_VALUE : nowNs + _maxDrainNs;
        int toProcess = remaining;

        for (int i = 0; i < toProcess; i++) {
            if (deadlineNs != Long.MAX_VALUE && System.nanoTime() > deadlineNs) {
                break;
            }
            EntityEliteSpawn.SpawnTask task = state.queue.poll();
            if (task == null) {
                break;
            }
            drainedAny = true;

            Ref<EntityStore> ref = task.ref;
            if (ref == null) {
                continue;
            }

            if (task.npc == null) {
                continue;
            }

            double mult = clampMultiplier(task.mult);
            double uncommon = clampChance(task.uncommon);
            double rare = clampChance(task.rare);
            double legendary = clampChance(task.legendary);

            if (uncommon <= 0.0 && rare <= 0.0 && legendary <= 0.0 && mult <= 0.0) {
                continue;
            }

            long rollStartNs = System.nanoTime();
            boolean rolled = EliteMobsDifficultySpawner.tryRoll(task.npc, ref, store, commandBuffer, mult, uncommon, rare, legendary);
            long rollEndNs = System.nanoTime();
            attemptedRoll = true;

            if (_debugLogging) {
                logTiming(task, rollStartNs, rollEndNs, mult, uncommon, rare, legendary, rolled);
            }
        }

        if (attemptedRoll) {
            consume(commandBuffer);
        }

        if (drainedAny) {
            state.lastDrainAtNs = System.nanoTime();
        }

        if (state.queue.isEmpty()) {
            EntityEliteSpawn.cleanupQueueState(store, state);
        }
    }

    private static long toNs(double ms) {
        if (!Double.isFinite(ms) || ms <= 0.0) {
            return 0L;
        }
        return (long) (ms * 1_000_000.0);
    }

    private static double clampChance(double v) {
        if (!Double.isFinite(v) || v <= 0.0) {
            return 0.0;
        }
        return v;
    }

    private static double clampMultiplier(double v) {
        if (!Double.isFinite(v)) {
            return 1.0;
        }
        if (v < 0.0) {
            return 0.0;
        }
        return v;
    }

    private static CommandBuffer<EntityStore> newCommandBuffer(Store<EntityStore> store) {
        if (COMMAND_BUFFER_CTOR == null) {
            return null;
        }
        try {
            Object created = COMMAND_BUFFER_CTOR.newInstance(store);
            @SuppressWarnings("unchecked")
            CommandBuffer<EntityStore> buffer = (CommandBuffer<EntityStore>) created;
            return buffer;
        } catch (Throwable ignored) {
            return null;
        }
    }

    private static void consume(CommandBuffer<?> buffer) {
        if (buffer == null || COMMAND_BUFFER_CONSUME == null) {
            return;
        }
        try {
            COMMAND_BUFFER_CONSUME.invoke(buffer);
        } catch (Throwable ignored) {
        }
    }

    private static void logTiming(
            EntityEliteSpawn.SpawnTask task,
            long rollStartNs,
            long rollEndNs,
            double mult,
            double uncommon,
            double rare,
            double legendary,
            boolean rolled
    ) {
        long totalNs = rollEndNs - task.enqueuedAtNs;
        long queueNs = rollStartNs - task.enqueuedAtNs;
        long rollNs = rollEndNs - rollStartNs;
        String msg = String.format(
                Locale.ROOT,
                "[ELITEMOBS] onSpawn->roll total=%.3fms queue=%.3fms roll=%.3fms chances(u=%.6f r=%.6f l=%.6f m=%.6f) rolled=%s",
                totalNs / 1_000_000.0,
                queueNs / 1_000_000.0,
                rollNs / 1_000_000.0,
                uncommon,
                rare,
                legendary,
                mult,
                rolled
        );
        Logging.debug(msg);
    }
}
