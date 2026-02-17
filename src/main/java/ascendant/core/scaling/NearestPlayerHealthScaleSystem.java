package ascendant.core.scaling;

import ascendant.core.config.DifficultyIO;
import ascendant.core.config.DifficultyManager;
import ascendant.core.config.DifficultyMeta;
import ascendant.core.config.DifficultySettings;
import ascendant.core.util.NearestPlayerFinder;
import ascendant.core.util.ReflectionHelper;
import com.hypixel.hytale.codec.KeyedCodec;
import com.hypixel.hytale.codec.builder.BuilderCodec;
import com.hypixel.hytale.codec.codecs.simple.StringCodec;
import com.hypixel.hytale.component.*;
import com.hypixel.hytale.component.dependency.Dependency;
import com.hypixel.hytale.component.dependency.Order;
import com.hypixel.hytale.component.dependency.SystemDependency;
import com.hypixel.hytale.component.query.Query;
import com.hypixel.hytale.server.core.entity.entities.Player;
import com.hypixel.hytale.server.core.entity.nameplate.Nameplate;
import com.hypixel.hytale.server.core.modules.entity.AllLegacyLivingEntityTypesQuery;
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
    private static final String SPAWN_TIER_COMPONENT_ID = "ascendant.spawn_tier";
    private static final String SPAWN_TIER_FIELD_KEY = "TierId";
    private static final BuilderCodec<SpawnTierComponent> SPAWN_TIER_CODEC = BuilderCodec
            .builder(SpawnTierComponent.class, SpawnTierComponent::new)
            .addField(
                    new KeyedCodec<>(SPAWN_TIER_FIELD_KEY, new StringCodec()),
                    SpawnTierComponent::setTierId,
                    SpawnTierComponent::getTierId
            )
            .build();
    private static ComponentType<EntityStore, SpawnTierComponent> SPAWN_TIER_COMPONENT_TYPE;

    private final float _fallbackRadiusSq;

    private final float _minFactor;
    private final float _maxFactor;
    private final float _healthScalingTolerance;
    private final boolean _allowHealthModifier;
    private final boolean _allowSpawnTierNameplate;

    public NearestPlayerHealthScaleSystem() {
        double radius = DifficultyManager.getFromConfig(DifficultyIO.PLAYER_DISTANCE_RADIUS_TO_CHECK);
        double minHealthScalingFactor = DifficultyManager.getFromConfig(DifficultyIO.MIN_HEALTH_SCALING_FACTOR);
        double maxHealthScalingFactor = DifficultyManager.getFromConfig(DifficultyIO.MAX_HEALTH_SCALING_FACTOR);
        double healthScalingTolerance = DifficultyManager.getFromConfig(DifficultyIO.HEALTH_SCALING_TOLERANCE);
        _allowHealthModifier = DifficultyManager.getFromConfig(DifficultyIO.ALLOW_HEALTH_MODIFIER);
        _allowSpawnTierNameplate = DifficultyManager.getFromConfig(DifficultyIO.ALLOW_SPAWN_TIER_NAMEPLATE);
        ensureSpawnTierComponentType();
        float r = (float) Math.max(0.0, radius);
        _fallbackRadiusSq = r * r;
        _minFactor = (float) minHealthScalingFactor;
        _maxFactor = (float) maxHealthScalingFactor;
        _healthScalingTolerance = (float) healthScalingTolerance;
    }

    @Nullable
    public static String getSpawnTier(@Nonnull Store<EntityStore> store, @Nonnull Ref<EntityStore> ref) {
        ComponentType<EntityStore, SpawnTierComponent> type = SPAWN_TIER_COMPONENT_TYPE;
        if (type == null) {
            return null;
        }
        SpawnTierComponent component = store.getComponent(ref, type);
        if (component == null) {
            return null;
        }
        String tierId = component.getTierId();
        return (tierId == null || tierId.isBlank()) ? null : tierId;
    }

    @Nullable
    private static String getSpawnTierFromHolder(@Nonnull Holder<EntityStore> holder) {
        ComponentType<EntityStore, SpawnTierComponent> type = SPAWN_TIER_COMPONENT_TYPE;
        if (type == null) {
            return null;
        }
        SpawnTierComponent component = holder.getComponent(type);
        if (component == null) {
            return null;
        }
        String tierId = component.getTierId();
        return (tierId == null || tierId.isBlank()) ? null : tierId;
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
        Player maybePlayer = holder.getComponent(Player.getComponentType());
        if (maybePlayer != null) {
            return;
        }

        String tier = getSpawnTierFromHolder(holder);
        if (tier == null) {
            World world = store.getExternalData().getWorld();
            Player nearest = NearestPlayerFinder.findNearestPlayer(world, store, holder, _fallbackRadiusSq);
            if (nearest == null) {
                return;
            }

            UUID playerUuid = nearest.getUuid();
            tier = DifficultyManager.getDifficulty(playerUuid);
            if (tier == null || tier.isBlank()) {
                tier = DifficultyIO.DEFAULT_BASE_DIFFICULTY;
            }
            setSpawnTier(holder, tier);
        }

        if (_allowSpawnTierNameplate) {
            applySpawnTierNameplate(holder, tier);
        }

        if (!_allowHealthModifier) {
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

        DifficultySettings settings = DifficultyManager.getSettings();

        float factor = _clampFactor((float) settings.get(tier, DifficultyIO.SETTING_HEALTH_MULTIPLIER));

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

    private void setSpawnTier(@Nonnull Holder<EntityStore> holder, @Nonnull String tierId) {
        ComponentType<EntityStore, SpawnTierComponent> type = SPAWN_TIER_COMPONENT_TYPE;
        if (type == null || tierId.isBlank()) {
            return;
        }
        SpawnTierComponent component = holder.getComponent(type);
        if (component == null) {
            component = holder.ensureAndGetComponent(type);
        }
        if (component.getTierId() == null || component.getTierId().isBlank()) {
            component.setTierId(tierId);
        }
    }

    private void applySpawnTierNameplate(@Nonnull Holder<EntityStore> holder, @Nonnull String tierId) {
        if (tierId.isBlank()) {
            return;
        }
        DifficultyMeta.TierMeta meta = DifficultyMeta.resolve(DifficultyManager.getConfig(), tierId);
        String displayName = meta.displayName();
        if (displayName == null || displayName.isBlank()) {
            displayName = tierId;
        }
        String label = displayName;
        if (!displayName.equalsIgnoreCase(tierId)) {
            label = displayName + " (" + tierId + ")";
        }

        Nameplate nameplate = holder.getComponent(Nameplate.getComponentType());
        if (nameplate == null) {
            nameplate = holder.ensureAndGetComponent(Nameplate.getComponentType());
            nameplate.setText(label);
            return;
        }
        String current = nameplate.getText();
        if (current == null || current.isBlank()) {
            nameplate.setText(label);
        } else if (!current.contains(displayName) && !current.contains(tierId)) {
            nameplate.setText(current + " [" + label + "]");
        }
    }

    private void ensureSpawnTierComponentType() {
        if (SPAWN_TIER_COMPONENT_TYPE != null) {
            return;
        }
        SPAWN_TIER_COMPONENT_TYPE = registerComponent(
                SpawnTierComponent.class,
                SPAWN_TIER_COMPONENT_ID,
                SPAWN_TIER_CODEC,
                SpawnTierComponent::new
        );
    }

    public static final class SpawnTierComponent implements Component<EntityStore> {
        private String tierId = "";

        public SpawnTierComponent() {
        }

        public SpawnTierComponent(@Nullable String tierId) {
            this.tierId = tierId;
        }

        @Nullable
        public String getTierId() {
            return tierId;
        }

        public void setTierId(@Nullable String tierId) {
            this.tierId = tierId == null ? "" : tierId;
        }

        @Override
        public @Nullable Component<EntityStore> clone() {
            return new SpawnTierComponent(this.tierId);
        }
    }

    private static final class ReflectiveStatModifierBridge {
        private static final Method PUT;
        private static final Method REMOVE;

        static {
            try {
                Class<?> v = ReflectionHelper.resolveClass(
                        "com.hypixel.hytale.server.core.modules.entitystats.EntityStatValue",
                        NearestPlayerHealthScaleSystem.class.getClassLoader()
                );
                if (v == null) {
                    throw new ClassNotFoundException("com.hypixel.hytale.server.core.modules.entitystats.EntityStatValue");
                }
                PUT = ReflectionHelper.getDeclaredMethod(v, "putModifier", String.class, Modifier.class);
                REMOVE = ReflectionHelper.getDeclaredMethod(v, "removeModifier", String.class);
                if (PUT == null || REMOVE == null) {
                    throw new NoSuchMethodException("Missing EntityStatValue modifier methods");
                }
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
