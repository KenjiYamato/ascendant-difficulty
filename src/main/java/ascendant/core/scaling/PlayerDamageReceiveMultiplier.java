package ascendant.core.scaling;

import ascendant.core.config.DifficultyIO;
import ascendant.core.config.DifficultyManager;
import ascendant.core.util.DamageRef;
import ascendant.core.util.Logging;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.hypixel.hytale.component.ArchetypeChunk;
import com.hypixel.hytale.component.CommandBuffer;
import com.hypixel.hytale.component.Ref;
import com.hypixel.hytale.component.Store;
import com.hypixel.hytale.component.dependency.Dependency;
import com.hypixel.hytale.component.dependency.Order;
import com.hypixel.hytale.component.dependency.SystemDependency;
import com.hypixel.hytale.component.dependency.SystemGroupDependency;
import com.hypixel.hytale.component.query.Query;
import com.hypixel.hytale.logger.HytaleLogger;
import com.hypixel.hytale.server.core.modules.entity.AllLegacyLivingEntityTypesQuery;
import com.hypixel.hytale.server.core.modules.entity.damage.*;
import com.hypixel.hytale.server.core.modules.entitystats.EntityStatMap;
import com.hypixel.hytale.server.core.modules.entitystats.EntityStatValue;
import com.hypixel.hytale.server.core.modules.entitystats.EntityStatsModule;
import com.hypixel.hytale.server.core.modules.entitystats.asset.DefaultEntityStatTypes;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;


public final class PlayerDamageReceiveMultiplier extends DamageEventSystem {

    private static final HytaleLogger LOGGER = HytaleLogger.forEnclosingClass();
    private static final ConcurrentHashMap<UUID, Boolean> DEBUG_MAX_DAMAGE = new ConcurrentHashMap<>();

    private final Set<Dependency<EntityStore>> _dependencies;
    private final float _minDamageFactor;
    private final boolean _allowDamageModifier;
    private final boolean _allowDamagePhysical;
    private final boolean _allowDamageProjectile;
    private final boolean _allowDamageCommand;
    private final boolean _allowDamageDrowning;
    private final boolean _allowDamageEnvironment;
    private final boolean _allowDamageFall;
    private final boolean _allowDamageOutOfWorld;
    private final boolean _allowDamageSuffocation;

    public PlayerDamageReceiveMultiplier() {
        _dependencies = Set.of(
                new SystemGroupDependency(Order.AFTER, DamageModule.get().getGatherDamageGroup()),
                new SystemGroupDependency(Order.AFTER, DamageModule.get().getFilterDamageGroup()),
                new SystemDependency(Order.BEFORE, DamageSystems.ApplyDamage.class)
        );
        double minDamageFactor = DifficultyManager.getFromConfig(DifficultyIO.MIN_DAMAGE_FACTOR);
        _minDamageFactor = (float) minDamageFactor;
        _allowDamageModifier = DifficultyManager.getFromConfig(DifficultyIO.ALLOW_DAMAGE_MODIFIER);
        _allowDamagePhysical = DifficultyManager.getFromConfig(DifficultyIO.ALLOW_DAMAGE_PHYSICAL);
        _allowDamageProjectile = DifficultyManager.getFromConfig(DifficultyIO.ALLOW_DAMAGE_PROJECTILE);
        _allowDamageCommand = DifficultyManager.getFromConfig(DifficultyIO.ALLOW_DAMAGE_COMMAND);
        _allowDamageDrowning = DifficultyManager.getFromConfig(DifficultyIO.ALLOW_DAMAGE_DROWNING);
        _allowDamageEnvironment = DifficultyManager.getFromConfig(DifficultyIO.ALLOW_DAMAGE_ENVIRONMENT);
        _allowDamageFall = DifficultyManager.getFromConfig(DifficultyIO.ALLOW_DAMAGE_FALL);
        _allowDamageOutOfWorld = DifficultyManager.getFromConfig(DifficultyIO.ALLOW_DAMAGE_OUT_OF_WORLD);
        _allowDamageSuffocation = DifficultyManager.getFromConfig(DifficultyIO.ALLOW_DAMAGE_SUFFOCATION);
    }

    public static boolean toggleDebugMaxDamage(@Nonnull UUID playerUuid) {
        Boolean current = DEBUG_MAX_DAMAGE.get(playerUuid);
        boolean next = current == null || !current;
        if (next) {
            DEBUG_MAX_DAMAGE.put(playerUuid, true);
        } else {
            DEBUG_MAX_DAMAGE.remove(playerUuid);
        }
        return next;
    }

    public static boolean isDebugMaxDamageEnabled(@Nonnull UUID playerUuid) {
        Boolean enabled = DEBUG_MAX_DAMAGE.get(playerUuid);
        return enabled != null && enabled;
    }

    @Nonnull
    @Override
    public Set<Dependency<EntityStore>> getDependencies() {
        return _dependencies;
    }

    @Nonnull
    @Override
    public Query<EntityStore> getQuery() {
        return AllLegacyLivingEntityTypesQuery.INSTANCE;
    }

    //public static class ArmorDamageReduction/ DamageArmor
    @Override
    @SuppressWarnings("removal")
    public void handle(
            int index,
            @Nonnull ArchetypeChunk<EntityStore> chunk,
            @Nonnull Store<EntityStore> store,
            @Nonnull CommandBuffer<EntityStore> commandBuffer,
            @Nonnull Damage damage
    ) {

        Logging.debug("[DAMAGE RECEIVE] Damage entry: " + damage.getAmount());
        if (!DamageRef.checkInvalidDamage(damage)) {
            Logging.debug("[DAMAGE RECEIVE] skip: invalid damage amount/cause");
            return;
        }

        UUID victimUuid = DamageRef.resolveVictimUUID(index, chunk, store);
        if (victimUuid == null) {
            Logging.debug("[DAMAGE RECEIVE] skip: victim is not a player");
            return;
        }

        if (!_allowDamageModifier) {
            Logging.debug("[DAMAGE RECEIVE] skip: damage modifier disabled");
            return;
        }

        boolean debugEnabled = isDebugMaxDamageEnabled(victimUuid);
        if (debugEnabled) {
            float debugDamage = resolveMaxReceiveDamage(index, chunk, store);
            if (debugDamage > 0.0f && Float.isFinite(debugDamage)) {
                Logging.debug("[DAMAGE RECEIVE] debug max damage applied=" + debugDamage);
                damage.setAmount(debugDamage);
                return;
            }
            Logging.debug("[DAMAGE RECEIVE] debug max damage unavailable, fallback to scaling");
        }

        DamageContext ctx = buildContext(victimUuid, damage);
        if (ctx == null) {
            Logging.debug("[DAMAGE RECEIVE] skip: no damage context");
            return;
        }
        float afterMultiplier = applyDamageMultiplier(ctx.baseDamage, ctx.damageMultiplier);
        damage.setAmount(afterMultiplier);
    }

    @Nullable
    private DamageContext buildContext(
            UUID victimUUID,
            Damage damage
    ) {
        String tierId = DifficultyManager.getDifficulty(victimUUID);
        if (tierId == null) {
            Logging.debug("[DAMAGE RECEIVE] context: missing tier for victim=" + victimUUID);
            return null;
        }

        double damageMultiplierCfg = resolveDamageMultiplier(tierId, damage.getCause());
        if (damageMultiplierCfg <= 0) {
            Logging.debug("[DAMAGE RECEIVE] context: multiplier <= 0 tier=" + tierId + " cause=" + damage.getCause());
            return null;
        }

        return new DamageContext(
                damage.getAmount(),
                (float) Math.max(0.0, damageMultiplierCfg)
        );
    }

    @SuppressWarnings("removal")
    private float resolveMaxReceiveDamage(
            int index,
            @Nonnull ArchetypeChunk<EntityStore> chunk,
            @Nonnull Store<EntityStore> store
    ) {
        Ref<EntityStore> victimRef = chunk.getReferenceTo(index);
        if (!victimRef.isValid()) {
            Logging.debug("[DAMAGE RECEIVE] debug max damage: invalid victim ref");
            return -1.0f;
        }
        EntityStatMap statMap = store.getComponent(victimRef, EntityStatsModule.get().getEntityStatMapComponentType());
        if (statMap == null) {
            Logging.debug("[DAMAGE RECEIVE] debug max damage: missing stat map");
            return -1.0f;
        }
        int healthIndex = DefaultEntityStatTypes.getHealth();
        if (healthIndex == Integer.MIN_VALUE) {
            Logging.debug("[DAMAGE RECEIVE] debug max damage: invalid health stat index");
            return -1.0f;
        }
        EntityStatValue health = statMap.get(healthIndex);
        if (health == null) {
            Logging.debug("[DAMAGE RECEIVE] debug max damage: missing health stat");
            return -1.0f;
        }
        float maxHealth = health.getMax();
        if (!Float.isFinite(maxHealth) || maxHealth <= 0.0f) {
            Logging.debug("[DAMAGE RECEIVE] debug max damage: invalid max health=" + maxHealth);
            return -1.0f;
        }
        double raw = (double) maxHealth + 1.0;
        if (raw >= Float.MAX_VALUE) {
            Logging.debug("[DAMAGE RECEIVE] debug max damage: overflow, clamped to Float.MAX");
            return Float.MAX_VALUE;
        }
        Logging.debug("[DAMAGE RECEIVE] debug max damage resolved=" + raw);
        return (float) raw;
    }

    private double resolveDamageMultiplier(String tierId, DamageCause cause) {
        double baseMultiplier = DifficultyManager.getSettings().get(tierId, DifficultyIO.SETTING_DAMAGE_MULTIPLIER);
        SpecificDamageMultiplier specific = resolveSpecificDamageMultiplier(cause);
        if (specific == null) {
            Logging.debug("[DAMAGE RECEIVE] multiplier: using base=" + baseMultiplier + " (no specific for cause=" + cause + ")");
            return baseMultiplier;
        }
        if (!specific.allowed()) {
            Logging.debug("[DAMAGE RECEIVE] multiplier: specific not allowed key=" + specific.key() + " cause=" + cause);
            return -1.0;
        }
        if (!hasTierOrBaseKey(tierId, specific.key())) {
            Logging.debug("[DAMAGE RECEIVE] multiplier: using base=" + baseMultiplier + " (missing key=" + specific.key() + " tier=" + tierId + ")");
            return baseMultiplier;
        }
        double resolved = DifficultyManager.getSettings().get(tierId, specific.key());
        Logging.debug("[DAMAGE RECEIVE] multiplier: using specific " + specific.key() + "=" + resolved + " tier=" + tierId);
        return resolved;
    }

    @Nullable
    private SpecificDamageMultiplier resolveSpecificDamageMultiplier(@Nullable DamageCause cause) {
        if (cause == null) {
            return null;
        }
        if (cause == DamageCause.PHYSICAL) {
            return new SpecificDamageMultiplier(DifficultyIO.SETTING_DAMAGE_MULTIPLIER_PHYSICAL, _allowDamagePhysical);
        }
        if (cause == DamageCause.PROJECTILE) {
            return new SpecificDamageMultiplier(DifficultyIO.SETTING_DAMAGE_MULTIPLIER_PROJECTILE, _allowDamageProjectile);
        }
        if (cause == DamageCause.COMMAND) {
            return new SpecificDamageMultiplier(DifficultyIO.SETTING_DAMAGE_MULTIPLIER_COMMAND, _allowDamageCommand);
        }
        if (cause == DamageCause.DROWNING) {
            return new SpecificDamageMultiplier(DifficultyIO.SETTING_DAMAGE_MULTIPLIER_DROWNING, _allowDamageDrowning);
        }
        if (cause == DamageCause.ENVIRONMENT) {
            return new SpecificDamageMultiplier(DifficultyIO.SETTING_DAMAGE_MULTIPLIER_ENVIRONMENT, _allowDamageEnvironment);
        }
        if (cause == DamageCause.FALL) {
            return new SpecificDamageMultiplier(DifficultyIO.SETTING_DAMAGE_MULTIPLIER_FALL, _allowDamageFall);
        }
        if (cause == DamageCause.OUT_OF_WORLD) {
            return new SpecificDamageMultiplier(DifficultyIO.SETTING_DAMAGE_MULTIPLIER_OUT_OF_WORLD, _allowDamageOutOfWorld);
        }
        if (cause == DamageCause.SUFFOCATION) {
            return new SpecificDamageMultiplier(DifficultyIO.SETTING_DAMAGE_MULTIPLIER_SUFFOCATION, _allowDamageSuffocation);
        }
        return null;
    }

    private boolean hasTierOrBaseKey(String tierId, String key) {
        if (tierId == null || key == null) {
            return false;
        }
        JsonObject base = DifficultyManager.getConfig().getSection("base");
        if (hasPrimitiveKey(base, key)) {
            return true;
        }
        JsonObject tiers = DifficultyManager.getConfig().getSection("tiers");
        JsonElement tierElement = tiers.get(tierId);
        if (tierElement != null && tierElement.isJsonObject()) {
            return hasPrimitiveKey(tierElement.getAsJsonObject(), key);
        }
        return false;
    }

    private boolean hasPrimitiveKey(JsonObject obj, String key) {
        if (obj == null) {
            return false;
        }
        JsonElement element = obj.get(key);
        return element != null && element.isJsonPrimitive();
    }

    public float applyDamageMultiplier(float damage, float damageMultiplier) {
        float d = Math.max(0.0f, damage);
        float m = Math.max(0.0f, damageMultiplier);
        float scaled = d * m;
        return Math.max(d * _minDamageFactor, scaled);
    }

    private record DamageContext(float baseDamage, float damageMultiplier) {
    }

    private record SpecificDamageMultiplier(String key, boolean allowed) {
    }
}
