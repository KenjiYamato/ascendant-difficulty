package ascendant.core.scaling;

import ascendant.core.config.DifficultyIO;
import ascendant.core.config.DifficultyManager;
import ascendant.core.util.DamageRef;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.hypixel.hytale.component.ArchetypeChunk;
import com.hypixel.hytale.component.CommandBuffer;
import com.hypixel.hytale.component.Store;
import com.hypixel.hytale.component.dependency.Dependency;
import com.hypixel.hytale.component.dependency.Order;
import com.hypixel.hytale.component.dependency.SystemDependency;
import com.hypixel.hytale.component.dependency.SystemGroupDependency;
import com.hypixel.hytale.component.query.Query;
import com.hypixel.hytale.logger.HytaleLogger;
import com.hypixel.hytale.server.core.modules.entity.AllLegacyLivingEntityTypesQuery;
import com.hypixel.hytale.server.core.modules.entity.damage.Damage;
import com.hypixel.hytale.server.core.modules.entity.damage.DamageCause;
import com.hypixel.hytale.server.core.modules.entity.damage.DamageEventSystem;
import com.hypixel.hytale.server.core.modules.entity.damage.DamageModule;
import com.hypixel.hytale.server.core.modules.entity.damage.DamageSystems;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.util.Set;
import java.util.UUID;


public final class PlayerDamageReceiveMultiplier extends DamageEventSystem {

    private static final HytaleLogger LOGGER = HytaleLogger.forEnclosingClass();

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
        if (!_allowDamageModifier) {
            return;
        }
        DamageContext ctx = buildContext(index, chunk, store, commandBuffer, damage);
        if (ctx == null) {
            return;
        }

        float afterMultiplier = applyDamageMultiplier(ctx.baseDamage, ctx.damageMultiplier);
        damage.setAmount(afterMultiplier);
    }

    @Nullable
    @SuppressWarnings("removal")
    private DamageContext buildContext(
            int index,
            ArchetypeChunk<EntityStore> chunk,
            Store<EntityStore> store,
            CommandBuffer<EntityStore> commandBuffer,
            Damage damage
    ) {
        if (!DamageRef.checkInvalidDamage(damage)) {
            return null;
        }

        UUID victimUUID = DamageRef.resolveVictimUUID(index, chunk, store);
        if (victimUUID == null) {
            return null;
        }

        String tierId = DifficultyManager.getDifficulty(victimUUID);
        if (tierId == null) {
            return null;
        }

        double damageMultiplierCfg = resolveDamageMultiplier(tierId, damage.getCause());
        if (damageMultiplierCfg <= 0) {
            return null;
        }

        return new DamageContext(
                damage.getAmount(),
                (float) Math.max(0.0, damageMultiplierCfg)
        );
    }

    private double resolveDamageMultiplier(String tierId, DamageCause cause) {
        double baseMultiplier = DifficultyManager.getSettings().get(tierId, DifficultyIO.SETTING_DAMAGE_MULTIPLIER);
        SpecificDamageMultiplier specific = resolveSpecificDamageMultiplier(cause);
        if (specific == null) {
            return baseMultiplier;
        }
        if (!specific.allowed()) {
            return -1.0;
        }
        if (!hasTierOrBaseKey(tierId, specific.key())) {
            return baseMultiplier;
        }
        return DifficultyManager.getSettings().get(tierId, specific.key());
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
