package ascendant.core.scaling;

import ascendant.core.config.DifficultyManager;
import ascendant.core.config.DifficultySettings;
import com.hypixel.hytale.component.AddReason;
import com.hypixel.hytale.component.Holder;
import com.hypixel.hytale.component.Store;
import com.hypixel.hytale.component.dependency.Dependency;
import com.hypixel.hytale.component.dependency.Order;
import com.hypixel.hytale.component.dependency.SystemDependency;
import com.hypixel.hytale.component.query.Query;
import com.hypixel.hytale.math.vector.Vector3d;
import com.hypixel.hytale.server.core.entity.entities.Player;
import com.hypixel.hytale.server.core.modules.entity.AllLegacyLivingEntityTypesQuery;
import com.hypixel.hytale.server.core.modules.entity.component.TransformComponent;
import com.hypixel.hytale.server.core.modules.entitystats.EntityStatMap;
import com.hypixel.hytale.server.core.modules.entitystats.EntityStatValue;
import com.hypixel.hytale.server.core.modules.entitystats.EntityStatsModule;
import com.hypixel.hytale.server.core.modules.entitystats.EntityStatsSystems;
import com.hypixel.hytale.server.core.modules.entitystats.asset.DefaultEntityStatTypes;
import com.hypixel.hytale.server.core.modules.entitystats.modifier.Modifier;
import com.hypixel.hytale.server.core.modules.entitystats.modifier.StaticModifier;
import com.hypixel.hytale.server.core.modules.entitystats.modifier.StaticModifier.CalculationType;
import com.hypixel.hytale.server.core.universe.world.World;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.lang.reflect.Method;
import java.util.Objects;
import java.util.Set;
import java.util.UUID;

@SuppressWarnings("removal")
public final class NearestPlayerHealthScaleSystem extends com.hypixel.hytale.component.system.HolderSystem<EntityStore>
        implements EntityStatsSystems.StatModifyingSystem {

    private static final String MOD_KEY = "ascendant.nearestPlayerHealthScale.health_multiplier";

    private float _fallbackRadiusSq;

    private float _minFactor;
    private float _maxFactor;
    private float _healthScalingTolerance;
    private boolean _allowHealthModifier;

    public NearestPlayerHealthScaleSystem() {

        this(48.0f, 0.05f, 100.0f);
        double radius = DifficultyManager.getConfig().getDouble("base.playerDistanceRadiusToCheck", 48.0);
        double minHealthScalingFactor = DifficultyManager.getConfig().getDouble("base.minHealthScalingFactor", 0.05);
        double maxHealthScalingFactor = DifficultyManager.getConfig().getDouble("base.maxHealthScalingFactor", 300.0);
        double healthScalingTolerance = (float) DifficultyManager.getConfig().getDouble("base.healthScalingTolerance", 0.0001);
        _allowHealthModifier = DifficultyManager.getConfig().getBoolean("base.allowHealthModifier", true);
        float r = (float) Math.max(0.0, radius);
        _fallbackRadiusSq = r * r;
        _minFactor = (float) minHealthScalingFactor;
        _maxFactor = (float) maxHealthScalingFactor;
        _healthScalingTolerance = (float) healthScalingTolerance;
    }

    public NearestPlayerHealthScaleSystem(float fallbackRadius, float minFactor, float maxFactor) {
        this._fallbackRadiusSq = fallbackRadius <= 0.0f ? 0.0f : fallbackRadius * fallbackRadius;
        this._minFactor = Math.max(0.0f, minFactor);
        this._maxFactor = Math.max(this._minFactor, maxFactor);
    }

    @Nonnull
    @Override
    public Query<EntityStore> getQuery() {
        return AllLegacyLivingEntityTypesQuery.INSTANCE;
    }

    @Nonnull
    @Override
    public Set<Dependency<EntityStore>> getDependencies() {
        return Set.of(new SystemDependency(Order.AFTER, EntityStatsSystems.Setup.class));
    }

    @Override
    public void onEntityAdd(@Nonnull Holder<EntityStore> holder,
                            @Nonnull AddReason reason,
                            @Nonnull Store<EntityStore> store) {

        if(!_allowHealthModifier) {
            return;
        }

        Player maybePlayer = holder.getComponent(Player.getComponentType());
        if (maybePlayer != null) {
            return;
        }

        EntityStatMap statMap = holder.getComponent(EntityStatsModule.get().getEntityStatMapComponentType());
        if (statMap == null) {
            statMap = holder.ensureAndGetComponent(EntityStatsModule.get().getEntityStatMapComponentType());
            statMap.update();
        }

        int healthIndex = DefaultEntityStatTypes.getHealth();
        if (healthIndex == Integer.MIN_VALUE) {
            return;
        }

        Player nearest = _findNearestPlayer(store, holder);
        if (nearest == null) {
            return;
        }

        UUID playerUuid = nearest.getUuid();
        String tier = DifficultyManager.getDifficulty(playerUuid);
        DifficultySettings settings = DifficultyManager.getSettings();

        float factor = _clampFactor((float) settings.get(tier, "health_multiplier"));

        EntityStatValue healthValue = statMap.get(healthIndex);
        if (healthValue == null) {
            return;
        }

        boolean firstApply = healthValue.getModifier(MOD_KEY) == null;

        if (Math.abs(factor - 1.0f) < _healthScalingTolerance) {
            ReflectiveStatModifierBridge.removeModifier(healthValue, MOD_KEY);
            return;
        }

        Modifier mod = new StaticModifier(
                Modifier.ModifierTarget.MAX,
                CalculationType.MULTIPLICATIVE,
                factor
        );
        ReflectiveStatModifierBridge.putModifier(healthValue, MOD_KEY, mod);

        if (firstApply) {
            float newMax = healthValue.getMax();
            float delta = newMax - healthValue.get();

            if (delta > _healthScalingTolerance) {
                statMap.addStatValue(healthIndex, delta);
            }
        }
    }


    @Override
    public void onEntityRemoved(@Nonnull Holder<EntityStore> holder, @Nonnull com.hypixel.hytale.component.RemoveReason reason, @Nonnull Store<EntityStore> store) {
        // (entity is going away anyway).
    }

    private float _clampFactor(float f) {
        if (!Float.isFinite(f)) return 1.0f;
        if (f < _minFactor) return _minFactor;
        if (f > _maxFactor) return _maxFactor;
        return f;
    }

    @Nullable
    private Player _findNearestPlayer(@Nonnull Store<EntityStore> store, @Nonnull Holder<EntityStore> holder) {
        if (_fallbackRadiusSq <= 0.0f) {
            return null;
        }

        World world = store.getExternalData().getWorld();
        Vector3d victimPos = _getPosition(holder);
        if (victimPos == null) {
            return null;
        }

        Player nearest = null;
        double best = Double.MAX_VALUE;

        for (Player p : world.getPlayers()) {
            com.hypixel.hytale.component.Ref<EntityStore> pref = p.getReference();
            if (pref == null || !pref.isValid()) {
                continue;
            }

            Vector3d ppos = _getPosition(store, pref);
            if (ppos == null) {
                continue;
            }

            double d2 = _distanceSq(ppos, victimPos);
            if (d2 <= (double) _fallbackRadiusSq && d2 < best) {
                best = d2;
                nearest = p;
            }
        }

        return nearest;
    }

    @Nullable
    private Vector3d _getPosition(@Nonnull Holder<EntityStore> holder) {
        TransformComponent tc = holder.getComponent(TransformComponent.getComponentType());
        return tc != null ? tc.getPosition() : null;
    }

    @Nullable
    private Vector3d _getPosition(@Nonnull Store<EntityStore> store, @Nonnull com.hypixel.hytale.component.Ref<EntityStore> ref) {
        TransformComponent tc = store.getComponent(ref, TransformComponent.getComponentType());
        if (tc == null) {
            return null;
        }
        return tc.getPosition();
    }

    private double _distanceSq(@Nonnull Vector3d a, @Nonnull Vector3d b) {
        double dx = a.getX() - b.getX();
        double dy = a.getY() - b.getY();
        double dz = a.getZ() - b.getZ();
        return dx * dx + dy * dy + dz * dz;
    }

    private static final class ReflectiveStatModifierBridge {
        private static final Method PUT;
        private static final Method REMOVE;

        static {
            try {
                Class<?> v = Class.forName("com.hypixel.hytale.server.core.modules.entitystats.EntityStatValue");
                PUT = v.getDeclaredMethod("putModifier", String.class, Modifier.class);
                REMOVE = v.getDeclaredMethod("removeModifier", String.class);

                PUT.setAccessible(true);
                REMOVE.setAccessible(true);
            } catch (Throwable t) {
                throw new ExceptionInInitializerError("Failed to init ReflectiveStatModifierBridge: " + t);
            }
        }

        private ReflectiveStatModifierBridge() {
        }

        static void putModifier(@Nonnull Object entityStatValue, @Nonnull String key, @Nonnull Modifier modifier) {
            Objects.requireNonNull(entityStatValue, "entityStatValue");
            Objects.requireNonNull(key, "key");
            Objects.requireNonNull(modifier, "modifier");
            try {
                PUT.invoke(entityStatValue, key, modifier);
            } catch (Throwable ignored) {
                // Intentionally silent: if this fails you’ll notice immediately by missing scaling.
            }
        }

        static void removeModifier(@Nonnull Object entityStatValue, @Nonnull String key) {
            Objects.requireNonNull(entityStatValue, "entityStatValue");
            Objects.requireNonNull(key, "key");
            try {
                REMOVE.invoke(entityStatValue, key);
            } catch (Throwable ignored) {
            }
        }
    }
}
