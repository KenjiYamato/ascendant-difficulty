package ascendant.core.integration.elitemobs;

import ascendant.core.util.ReflectionHelper;
import com.hypixel.hytale.component.CommandBuffer;
import com.hypixel.hytale.component.Ref;
import com.hypixel.hytale.component.Store;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;
import com.hypixel.hytale.server.npc.entities.NPCEntity;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.lang.reflect.Array;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.*;

public final class EliteMobsDifficultySpawner {

    private static final String CLS_SPAWN_SYSTEM = "com.github.cedeli.core.system.EntitySpawnSystem";
    private static final ClassLoader CLASS_LOADER = EliteMobsDifficultySpawner.class.getClassLoader();
    private static final Map<Object, Object> SPAWN_SYSTEM_CACHE =
            Collections.synchronizedMap(new WeakHashMap<>());
    private static volatile Field _fieldConfig;
    private static volatile Field _fieldSpawning;
    private static volatile Method _methodConfigGet;
    private static volatile Method _methodTryRollElite;

    private EliteMobsDifficultySpawner() {
    }

    public static boolean tryRoll(
            @Nonnull NPCEntity npc,
            @Nonnull Ref<EntityStore> ref,
            @Nonnull Store<EntityStore> store,
            @Nonnull CommandBuffer<EntityStore> commandBuffer,
            double chanceMultiplier,
            double uncommonChance,
            double rareChance,
            double legendaryChance
    ) {
        double mult = _sanitizeMultiplier(chanceMultiplier);
        if (_isZero(uncommonChance, rareChance, legendaryChance) && mult <= 0.0) {
            return false;
        }

        Object spawnSystem = findSpawnSystem(npc);
        if (spawnSystem == null) {
            return false;
        }

        Object mainConfig = _getMainConfig(spawnSystem);
        Object spawning = mainConfig != null ? _getSpawning(mainConfig) : null;
        SpawnChanceState prev = spawning != null ? _readSpawnChances(spawning) : null;

        double useUncommon = uncommonChance;
        double useRare = rareChance;
        double useLegendary = legendaryChance;
        if (_isZero(useUncommon, useRare, useLegendary)) {
            if (prev != null && mult > 0.0) {
                useUncommon = prev.uncommon * mult;
                useRare = prev.rare * mult;
                useLegendary = prev.legendary * mult;
            }
        } else if (mult != 1.0) {
            useUncommon *= mult;
            useRare *= mult;
            useLegendary *= mult;
        }

        if (spawning != null) {
            _applySpawnChances(spawning, useUncommon, useRare, useLegendary);
        }

        Method tryRoll = _tryRollElite(spawnSystem.getClass());
        if (tryRoll == null) {
            _restoreSpawnChances(spawning, prev);
            return false;
        }

        try {
            tryRoll.invoke(spawnSystem, ref, npc, store, commandBuffer);
            return true;
        } catch (Throwable ignored) {
            return false;
        } finally {
            _restoreSpawnChances(spawning, prev);
        }
    }

    @Nullable
    public static Object findSpawnSystem(@Nonnull NPCEntity npc) {
        Object world = npc.getWorld();
        if (world == null) {
            return null;
        }
        Object cached = SPAWN_SYSTEM_CACHE.get(world);
        if (cached != null) {
            return cached;
        }
        Object found = _bfsFind(world, ReflectionHelper.resolveClass(CLS_SPAWN_SYSTEM, CLASS_LOADER), 6);
        if (found != null) {
            SPAWN_SYSTEM_CACHE.put(world, found);
        }
        return found;
    }

    @Nullable
    private static Object _bfsFind(Object root, Class<?> target, int maxDepth) {
        if (root == null || target == null) {
            return null;
        }
        IdentityHashMap<Object, Boolean> seen = new IdentityHashMap<>();
        ArrayDeque<Node> q = new ArrayDeque<>();
        q.add(new Node(root, 0));
        seen.put(root, Boolean.TRUE);

        while (!q.isEmpty()) {
            Node cur = q.pollFirst();
            Object o = cur.obj;
            if (target.isInstance(o)) {
                return o;
            }
            if (cur.depth >= maxDepth) {
                continue;
            }
            for (Object next : _expand(o)) {
                if (next != null && seen.put(next, Boolean.TRUE) == null) {
                    q.addLast(new Node(next, cur.depth + 1));
                }
            }
        }
        return null;
    }

    private static List<Object> _expand(Object o) {
        Class<?> c = o.getClass();

        if (c.isArray()) {
            int len = Array.getLength(o);
            ArrayList<Object> out = new ArrayList<>(len);
            for (int i = 0; i < len; i++) {
                Object v = Array.get(o, i);
                if (v != null) out.add(v);
            }
            return out;
        }

        if (o instanceof Iterable<?> it) {
            ArrayList<Object> out = new ArrayList<>(16);
            for (Object v : it) if (v != null) out.add(v);
            return out;
        }

        if (o instanceof java.util.Map<?, ?> m) {
            ArrayList<Object> out = new ArrayList<>(m.size());
            for (Object v : m.values()) if (v != null) out.add(v);
            return out;
        }

        Field[] fields = ReflectionHelper.getInstanceFields(c);
        if (fields.length == 0) {
            return List.of();
        }

        ArrayList<Object> out = new ArrayList<>(Math.min(16, fields.length));
        for (Field f : fields) {
            try {
                Object v = f.get(o);
                if (v != null) out.add(v);
            } catch (Throwable ignored) {
            }
        }
        return out;
    }

    @Nullable
    private static Method _tryRollElite(Class<?> spawnSystemClass) {
        Method m = _methodTryRollElite;
        if (m != null) {
            return m;
        }
        m = ReflectionHelper.getAnyMethod(
                spawnSystemClass,
                "tryRollElite",
                Ref.class,
                NPCEntity.class,
                Store.class,
                CommandBuffer.class
        );
        _methodTryRollElite = m;
        return m;
    }

    @Nullable
    private static Object _getMainConfig(Object spawnSystem) {
        Field f = _fieldConfig;
        if (f == null) {
            f = ReflectionHelper.getDeclaredField(spawnSystem.getClass(), "config");
            _fieldConfig = f;
        }
        Object cfg = ReflectionHelper.getFieldValue(f, spawnSystem);
        return _configGet(cfg);
    }

    @Nullable
    private static Object _getSpawning(Object mainConfig) {
        Field f = _fieldSpawning;
        if (f == null) {
            f = ReflectionHelper.getDeclaredField(mainConfig.getClass(), "spawning");
            _fieldSpawning = f;
        }
        return ReflectionHelper.getFieldValue(f, mainConfig);
    }

    @Nullable
    private static Object _configGet(Object cfg) {
        if (cfg == null) {
            return null;
        }
        Method m = _methodConfigGet;
        if (m == null) {
            m = ReflectionHelper.getMethod(cfg.getClass(), "get");
            _methodConfigGet = m;
        }
        if (m == null) {
            return null;
        }
        try {
            return m.invoke(cfg);
        } catch (Throwable ignored) {
            return null;
        }
    }

    private static SpawnChanceState _readSpawnChances(Object spawning) {
        double prevUncommon = _getDoubleField(spawning, "uncommonChance");
        double prevRare = _getDoubleField(spawning, "rareChance");
        double prevLegendary = _getDoubleField(spawning, "legendaryChance");
        return new SpawnChanceState(prevUncommon, prevRare, prevLegendary);
    }

    private static void _applySpawnChances(Object spawning, double uncommon, double rare, double legendary) {
        ReflectionHelper.setField(spawning, "uncommonChance", _clamp01(uncommon));
        ReflectionHelper.setField(spawning, "rareChance", _clamp01(rare));
        ReflectionHelper.setField(spawning, "legendaryChance", _clamp01(legendary));
    }

    private static void _restoreSpawnChances(Object spawning, SpawnChanceState prev) {
        if (spawning == null || prev == null) {
            return;
        }
        ReflectionHelper.setField(spawning, "uncommonChance", prev.uncommon);
        ReflectionHelper.setField(spawning, "rareChance", prev.rare);
        ReflectionHelper.setField(spawning, "legendaryChance", prev.legendary);
    }

    private static double _getDoubleField(Object target, String name) {
        Field f = ReflectionHelper.getDeclaredField(target.getClass(), name);
        Object v = ReflectionHelper.getFieldValue(f, target);
        return v instanceof Number n ? n.doubleValue() : 0.0;
    }

    private static double _clamp01(double v) {
        return v <= 0.0 ? 0.0 : Math.min(v, 1.0);
    }

    private static boolean _isZero(double uncommon, double rare, double legendary) {
        return uncommon <= 0.0 && rare <= 0.0 && legendary <= 0.0;
    }

    private static double _sanitizeMultiplier(double v) {
        if (!Double.isFinite(v)) {
            return 1.0;
        }
        if (v < 0.0) {
            return 0.0;
        }
        return v;
    }

    private record SpawnChanceState(double uncommon, double rare, double legendary) {
    }

    private record Node(Object obj, int depth) {
    }
}
