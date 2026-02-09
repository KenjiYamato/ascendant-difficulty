package ascendant.core.integration.elitemobs;

import ascendant.core.config.DifficultyIO;
import ascendant.core.config.DifficultyManager;
import ascendant.core.util.Logging;
import ascendant.core.util.ReflectionHelper;
import com.hypixel.hytale.component.CommandBuffer;
import com.hypixel.hytale.component.Ref;
import com.hypixel.hytale.component.Store;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;
import com.hypixel.hytale.server.npc.entities.NPCEntity;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.lang.reflect.Array;
import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ThreadLocalRandom;

public final class EliteMobsDifficultySpawner {

    private static final String CLS_SPAWN_SYSTEM = "com.github.cedeli.core.system.EntitySpawnSystem";
    private static final String CLS_ELITE_COMPONENT = "com.github.cedeli.api.component.EliteComponent";
    private static final String CLS_MOB_RARITY = "com.github.cedeli.api.rarity.MobRarity";
    private static final String CLS_CONFIG = "com.hypixel.hytale.server.core.util.Config";
    private static final ClassLoader CLASS_LOADER = EliteMobsDifficultySpawner.class.getClassLoader();
    private static final Object _autoRollLock = new Object();
    private static final IdentityHashMap<Object, AutoRollState> _autoRollStates = new IdentityHashMap<>();
    private static final ConcurrentHashMap<Class<?>, ConcurrentHashMap<Class<?>, Method>> _rarityTierCache = new ConcurrentHashMap<>();
    private static final ConcurrentHashMap<Class<?>, Constructor<?>> _eliteCtorByRarity = new ConcurrentHashMap<>();
    private static volatile Field _fieldConfig;
    private static volatile Field _fieldPresetsConfig;
    private static volatile Field _fieldSelector;
    private static volatile Method _methodConfigGet;
    private static volatile Method _methodHandleInitialApplication;
    private static volatile Method _methodMobRarityRoll;
    private static volatile Method _methodMobRarityIsElite;
    private static volatile Method _selectorSelectAffixes;
    private static volatile Object _eliteType;
    private static volatile int _availability = -1;

    private EliteMobsDifficultySpawner() {
    }

    public static boolean isAvailable() {
        if (!isIntegrationEnabled()) {
            _availability = 0;
            return false;
        }
        int a = _availability;
        if (a >= 0) {
            return a == 1;
        }
        boolean ok = ReflectionHelper.resolveClass(CLS_SPAWN_SYSTEM, CLASS_LOADER) != null
                && ReflectionHelper.resolveClass(CLS_ELITE_COMPONENT, CLASS_LOADER) != null
                && ReflectionHelper.resolveClass(CLS_MOB_RARITY, CLASS_LOADER) != null
                && ReflectionHelper.resolveClass(CLS_CONFIG, CLASS_LOADER) != null;
        _availability = ok ? 1 : 0;
        return ok;
    }

    private static boolean isIntegrationEnabled() {
        try {
            return DifficultyManager.getFromConfig(DifficultyIO.INTEGRATION_ELITE_MOBS);
        } catch (RuntimeException ignored) {
            return DifficultyIO.DEFAULT_INTEGRATION_ELITE_MOBS;
        }
    }

    public static boolean rollAndApply(
            @Nonnull NPCEntity npc,
            @Nonnull Ref<EntityStore> ref,
            @Nonnull Store<EntityStore> store,
            @Nonnull CommandBuffer<EntityStore> commandBuffer,
            double uncommonChance,
            double rareChance,
            double legendaryChance
    ) {
        if (!isAvailable()) {
            Logging.debug("[ROLLING] integration not available");
            return false;
        }

        Object spawnSystem = findSpawnSystem(npc);
        if (spawnSystem == null) {
            Logging.debug("[ROLLING] spawn system not found");
            return false;
        }

        Object mainConfig = _getMainConfig(spawnSystem);
        Object presets = _getPresets(spawnSystem);
        Object selector = _getSelector(spawnSystem);
        if (mainConfig == null || presets == null || selector == null) {
            Logging.debug("[ROLLING] missing config or selector");
            return false;
        }

        AutoRollInit autoRoll = _ensureAutoRollState(spawnSystem, mainConfig, presets);
        if (autoRoll == null || !autoRoll.state.disabledChances || autoRoll.first) {
            Logging.debug("[ROLLING] auto-roll not ready");
            return false;
        }

        if (_hasEliteComponentAlready(store, ref, commandBuffer)) {
            Logging.debug("[ROLLING] has elite compount already ~");
            return false;
        }

        String roleName = npc.getRoleName();
        if (roleName == null || roleName.isEmpty()) {
            Logging.debug("[ROLLING] npc role missing");
            return false;
        }

        if (_isBlacklisted(mainConfig, roleName)) {
            Logging.debug("[ROLLING] npc role is blacklisted");
            return false;
        }

        if (!_isWhitelistAllowed(mainConfig, roleName)) {
            Logging.debug("[ROLLING] npc role not in whitelist");
            return false;
        }

        Map<String, Object> presetCache = autoRoll.state.presetCache;
        Object override = presetCache != null ? presetCache.get(roleName) : _getPresetOverride(presets, roleName);
        if (!autoRoll.state.presetsCleared && override != null) {
            Logging.debug("[ROLLING] preset override present");
            return false;
        }

        Object rarity;
        List<String> affixIds;

        if (override != null) {
            rarity = ReflectionHelper.invokeNoArgs(override, "getRarity");
            affixIds = _asStringList(ReflectionHelper.invokeNoArgs(override, "getAffixes"));
            if (rarity == null || affixIds == null) {
                Logging.debug("[ROLLING] preset override missing rarity or affixes");
                return false;
            }
        } else {
            rarity = _rollRarity(_clamp01(uncommonChance), _clamp01(rareChance), _clamp01(legendaryChance));
            if (rarity == null || !_isEliteRarity(rarity)) {
                Logging.debug("[ROLLING] rarity roll failed or not elite");
                return false;
            }

            Object tier = _getRarityTier(mainConfig, rarity);
            if (tier == null) {
                Logging.debug("[ROLLING] rarity tier missing");
                return false;
            }

            int min = _int(ReflectionHelper.invokeNoArgs(tier, "getMin"));
            int max = _int(ReflectionHelper.invokeNoArgs(tier, "getMax"));
            int affixCount = max > min ? min + ThreadLocalRandom.current().nextInt(max - min + 1) : min;

            List<?> selected = _selectAffixes(selector, npc, affixCount);
            affixIds = _affixIds(selected);
        }

        Object eliteComponentRaw = _newEliteComponent(rarity, affixIds);
        if (!(eliteComponentRaw instanceof com.hypixel.hytale.component.Component<?>)) {
            Logging.debug("[ROLLING] elite component create failed");
            return false;
        }

        @SuppressWarnings("unchecked")
        com.hypixel.hytale.component.Component<EntityStore> eliteComponent =
                (com.hypixel.hytale.component.Component<EntityStore>) eliteComponentRaw;

        Object eliteTypeRaw = _eliteType();
        if (!(eliteTypeRaw instanceof com.hypixel.hytale.component.ComponentType<?, ?>)) {
            Logging.debug("[ROLLING] elite type missing");
            return false;
        }

        @SuppressWarnings("unchecked")
        com.hypixel.hytale.component.ComponentType<EntityStore, com.hypixel.hytale.component.Component<EntityStore>> eliteType =
                (com.hypixel.hytale.component.ComponentType<EntityStore, com.hypixel.hytale.component.Component<EntityStore>>) eliteTypeRaw;

        commandBuffer.addComponent(ref, eliteType, eliteComponent);

        Method apply = _handleInitialApplication(spawnSystem.getClass());
        if (apply == null) {
            Logging.debug("[ROLLING] handleInitialApplication missing");
            return false;
        }

        try {
            apply.invoke(spawnSystem, ref, npc, eliteComponent, store, commandBuffer);
            Logging.debug("[ROLLING] applied elite component");
            return true;
        } catch (Throwable ignored) {
            Logging.debug("[ROLLING] apply failed");
            return false;
        }
    }

    @Nullable
    public static Object findSpawnSystem(@Nonnull NPCEntity npc) {
        Object world = npc.getWorld();
        return world == null ? null : _bfsFind(world, ReflectionHelper.resolveClass(CLS_SPAWN_SYSTEM, CLASS_LOADER), 6);
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

        if (o instanceof Map<?, ?> m) {
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

    @SuppressWarnings("unchecked")
    private static boolean _hasEliteComponentAlready(Store<EntityStore> store, Ref<EntityStore> ref, CommandBuffer<EntityStore> commandBuffer) {
        try {
            Object typeRaw = _eliteType();
            if (!(typeRaw instanceof com.hypixel.hytale.component.ComponentType<?, ?> type)) {
                return false;
            }

            com.hypixel.hytale.component.ComponentType<EntityStore, com.hypixel.hytale.component.Component<EntityStore>> eliteType =
                    (com.hypixel.hytale.component.ComponentType<EntityStore, com.hypixel.hytale.component.Component<EntityStore>>) type;

            if (commandBuffer != null) {
                try {
                    com.hypixel.hytale.component.Component<EntityStore> pending = commandBuffer.getComponent(ref, eliteType);
                    if (pending != null) {
                        return true;
                    }
                } catch (Throwable ignored) {
                }
            }
            return store.getComponent(ref, eliteType) != null;
        } catch (Throwable ignored) {
            return false;
        }
    }

    @Nullable
    private static Object _getMainConfig(Object spawnSystem) {
        Object cfg = ReflectionHelper.getFieldValue(_field(spawnSystem.getClass(), "config", "_fieldConfig"), spawnSystem);
        return _configGet(cfg);
    }

    @Nullable
    private static Object _getPresets(Object spawnSystem) {
        Object cfg = ReflectionHelper.getFieldValue(_field(spawnSystem.getClass(), "presetsConfig", "_fieldPresetsConfig"), spawnSystem);
        return _configGet(cfg);
    }

    @Nullable
    private static Object _getSelector(Object spawnSystem) {
        return ReflectionHelper.getFieldValue(_field(spawnSystem.getClass(), "selector", "_fieldSelector"), spawnSystem);
    }

    @Nullable
    private static Object _configGet(Object cfg) {
        if (cfg == null) {
            return null;
        }
        Method m = _methodConfigGet;
        if (m == null) {
            m = ReflectionHelper.getMethod(cfg.getClass(), "get");
            if (m == null) {
                return null;
            }
            _methodConfigGet = m;
        }
        try {
            return m.invoke(cfg);
        } catch (Throwable ignored) {
            return null;
        }
    }

    private static boolean _isBlacklisted(Object mainConfig, String roleName) {
        Object spawning = ReflectionHelper.invokeNoArgs(mainConfig, "getSpawning");
        List<String> blacklist = _asStringList(ReflectionHelper.invokeNoArgs(spawning, "getBlacklist"));
        if (blacklist == null || blacklist.isEmpty()) {
            return false;
        }
        for (String s : blacklist) {
            if (s != null && s.equalsIgnoreCase(roleName)) {
                return true;
            }
        }
        return false;
    }

    private static boolean _isWhitelistAllowed(Object mainConfig, String roleName) {
        Object spawning = ReflectionHelper.invokeNoArgs(mainConfig, "getSpawning");
        List<String> whitelist = _asStringList(ReflectionHelper.invokeNoArgs(spawning, "getWhitelist"));
        if (whitelist == null || whitelist.isEmpty()) {
            return true;
        }
        for (String s : whitelist) {
            if (s != null && s.equalsIgnoreCase(roleName)) {
                return true;
            }
        }
        return false;
    }

    @Nullable
    private static Object _getPresetOverride(Object presets, String roleName) {
        Object map = ReflectionHelper.invokeNoArgs(presets, "getPresets");
        return map instanceof Map<?, ?> m ? m.get(roleName) : null;
    }

    @Nullable
    private static AutoRollInit _ensureAutoRollState(@Nullable Object spawnSystem, @Nullable Object mainConfig, @Nullable Object presets) {
        if (spawnSystem == null || mainConfig == null) {
            return null;
        }
        synchronized (_autoRollLock) {
            AutoRollState state = _autoRollStates.get(spawnSystem);
            if (state != null) {
                return new AutoRollInit(state, false);
            }
            state = new AutoRollState();
            state.disabledChances = _disableSpawnChances(mainConfig);
            if (state.disabledChances) {
                state.presetsCleared = _disablePresets(presets, state);
            }
            _autoRollStates.put(spawnSystem, state);
            return new AutoRollInit(state, true);
        }
    }

    private static boolean _disableSpawnChances(Object mainConfig) {
        Object spawning = ReflectionHelper.invokeNoArgs(mainConfig, "getSpawning");
        return spawning != null
                && ReflectionHelper.setField(spawning, "uncommonChance", 0.0d)
                && ReflectionHelper.setField(spawning, "rareChance", 0.0d)
                && ReflectionHelper.setField(spawning, "legendaryChance", 0.0d);
    }

    private static boolean _disablePresets(@Nullable Object presets, AutoRollState state) {
        if (presets == null) {
            state.presetCache = Map.of();
            return true;
        }
        Object mapObj = ReflectionHelper.invokeNoArgs(presets, "getPresets");
        if (!(mapObj instanceof Map<?, ?> m)) {
            state.presetCache = Map.of();
            return true;
        }
        state.presetCache = _copyPresets(m);
        if (m.isEmpty()) {
            return true;
        }
        try {
            m.clear();
            return true;
        } catch (Throwable ignored) {
            return ReflectionHelper.setField(presets, "presets", new HashMap<String, Object>());
        }
    }

    private static Map<String, Object> _copyPresets(Map<?, ?> source) {
        if (source.isEmpty()) {
            return Map.of();
        }
        HashMap<String, Object> out = new HashMap<>(source.size());
        for (Map.Entry<?, ?> entry : source.entrySet()) {
            if (entry.getKey() instanceof String key && entry.getValue() != null) {
                out.put(key, entry.getValue());
            }
        }
        return out.isEmpty() ? Map.of() : Map.copyOf(out);
    }

    @Nullable
    private static Object _getRarityTier(Object mainConfig, Object rarity) {
        Object raritySettings = ReflectionHelper.invokeNoArgs(mainConfig, "getRaritySettings");
        if (raritySettings == null) {
            return null;
        }
        Class<?> settingsCls = raritySettings.getClass();
        Class<?> rarityCls = rarity.getClass();

        Method m = _rarityTierCache
                .computeIfAbsent(settingsCls, _k -> new ConcurrentHashMap<>())
                .computeIfAbsent(rarityCls, _k -> ReflectionHelper.getMethod(settingsCls, "getTier", rarityCls));

        if (m == null) {
            return null;
        }
        try {
            return m.invoke(raritySettings, rarity);
        } catch (Throwable ignored) {
            return null;
        }
    }

    @Nullable
    private static Object _rollRarity(double uncommon, double rare, double legendary) {
        try {
            Class<?> rarityCls = ReflectionHelper.resolveClass(CLS_MOB_RARITY, CLASS_LOADER);
            if (rarityCls == null) {
                return null;
            }
            Method m = _methodMobRarityRoll;
            if (m == null) {
                m = ReflectionHelper.getMethod(rarityCls, "roll", float.class, float.class, float.class, float.class);
                _methodMobRarityRoll = m;
                if (m == null) {
                    return null;
                }
            }
            float roll = ThreadLocalRandom.current().nextFloat();
            return m.invoke(null, (float) uncommon, (float) rare, (float) legendary, roll);
        } catch (Throwable ignored) {
            return null;
        }
    }

    private static boolean _isEliteRarity(Object rarity) {
        try {
            Method m = _methodMobRarityIsElite;
            if (m == null) {
                m = ReflectionHelper.getMethod(rarity.getClass(), "isElite");
                _methodMobRarityIsElite = m;
                if (m == null) {
                    return false;
                }
            }
            Object v = m.invoke(rarity);
            return v instanceof Boolean b && b;
        } catch (Throwable ignored) {
            return false;
        }
    }

    @Nullable
    private static List<?> _selectAffixes(Object selector, NPCEntity npc, int count) {
        try {
            Method m = _selectorSelectAffixes;
            if (m == null) {
                m = ReflectionHelper.getMethod(selector.getClass(), "selectAffixes", NPCEntity.class, int.class);
                _selectorSelectAffixes = m;
                if (m == null) {
                    return null;
                }
            }
            Object v = m.invoke(selector, npc, count);
            return v instanceof List<?> l ? l : null;
        } catch (Throwable ignored) {
            return null;
        }
    }

    private static List<String> _affixIds(List<?> affixes) {
        if (affixes == null || affixes.isEmpty()) {
            return List.of();
        }
        ArrayList<String> out = new ArrayList<>(affixes.size());
        for (Object a : affixes) {
            Object id = ReflectionHelper.invokeNoArgs(a, "getId");
            if (id instanceof String s && !s.isEmpty()) {
                out.add(s);
            }
        }
        return out.isEmpty() ? List.of() : List.copyOf(out);
    }

    @Nullable
    private static Object _newEliteComponent(Object rarity, List<String> ids) {
        try {
            Class<?> eliteComp = ReflectionHelper.resolveClass(CLS_ELITE_COMPONENT, CLASS_LOADER);
            if (eliteComp == null) {
                return null;
            }

            Constructor<?> ctor = _eliteCtorByRarity.computeIfAbsent(
                    rarity.getClass(),
                    rCls -> ReflectionHelper.getConstructor(eliteComp, rCls, List.class, List.class, boolean.class)
            );

            if (ctor == null) {
                return null;
            }
            return ctor.newInstance(rarity, ids, new ArrayList<>(), false);
        } catch (Throwable ignored) {
            return null;
        }
    }

    @Nullable
    private static Object _eliteType() {
        Object t = _eliteType;
        if (t != null) {
            return t;
        }
        try {
            Class<?> eliteComp = ReflectionHelper.resolveClass(CLS_ELITE_COMPONENT, CLASS_LOADER);
            if (eliteComp == null) {
                return null;
            }
            Field f = ReflectionHelper.getField(eliteComp, "TYPE");
            Object v = ReflectionHelper.getFieldValue(f, null);
            _eliteType = v;
            return v;
        } catch (Throwable ignored) {
            return null;
        }
    }

    @Nullable
    private static Method _handleInitialApplication(Class<?> spawnSystemClass) {
        Method m = _methodHandleInitialApplication;
        if (m != null) {
            return m;
        }
        try {
            Class<?> eliteComp = ReflectionHelper.resolveClass(CLS_ELITE_COMPONENT, CLASS_LOADER);
            if (eliteComp == null) {
                return null;
            }
            m = ReflectionHelper.getDeclaredMethod(
                    spawnSystemClass,
                    "handleInitialApplication",
                    Ref.class,
                    NPCEntity.class,
                    eliteComp,
                    Store.class,
                    CommandBuffer.class
            );
            if (m == null) {
                return null;
            }
            _methodHandleInitialApplication = m;
            return m;
        } catch (Throwable ignored) {
            return null;
        }
    }

    @Nullable
    private static Field _field(Class<?> owner, String name, String cacheName) {
        try {
            Field cached = switch (cacheName) {
                case "_fieldConfig" -> _fieldConfig;
                case "_fieldPresetsConfig" -> _fieldPresetsConfig;
                default -> _fieldSelector;
            };
            if (cached != null) {
                return cached;
            }

            Field f = ReflectionHelper.getDeclaredField(owner, name);
            if (f == null) {
                return null;
            }

            if ("_fieldConfig".equals(cacheName)) {
                _fieldConfig = f;
            } else if ("_fieldPresetsConfig".equals(cacheName)) {
                _fieldPresetsConfig = f;
            } else {
                _fieldSelector = f;
            }

            return f;
        } catch (Throwable ignored) {
            return null;
        }
    }

    private static int _int(Object v) {
        return v instanceof Number n ? n.intValue() : 0;
    }

    private static double _clamp01(double v) {
        return v <= 0.0 ? 0.0 : Math.min(v, 1.0);
    }

    @Nullable
    private static List<String> _asStringList(Object obj) {
        if (!(obj instanceof List<?> l)) {
            return null;
        }
        ArrayList<String> out = new ArrayList<>(l.size());
        for (Object v : l) {
            if (v instanceof String s) {
                out.add(s);
            }
        }
        return out;
    }

    private record AutoRollInit(AutoRollState state, boolean first) {
    }

    private static final class AutoRollState {
        private boolean disabledChances;
        private boolean presetsCleared;
        private Map<String, Object> presetCache;
    }

    private record Node(Object obj, int depth) {
    }
}
