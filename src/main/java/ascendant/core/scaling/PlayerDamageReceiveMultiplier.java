package ascendant.core.scaling;

import ascendant.core.config.DifficultyIO;
import ascendant.core.config.DifficultyManager;
import ascendant.core.util.DamageRef;
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

    public PlayerDamageReceiveMultiplier() {
        _dependencies = Set.of(
                new SystemGroupDependency(Order.AFTER, DamageModule.get().getGatherDamageGroup()),
                new SystemGroupDependency(Order.AFTER, DamageModule.get().getFilterDamageGroup()),
                new SystemDependency(Order.BEFORE, DamageSystems.ApplyDamage.class)
        );
        double minDamageFactor = DifficultyManager.getFromConfig(DifficultyIO.MIN_DAMAGE_FACTOR);
        _minDamageFactor = (float) minDamageFactor;
        _allowDamageModifier = DifficultyManager.getFromConfig(DifficultyIO.ALLOW_DAMAGE_MODIFIER);
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

        double damageMultiplierCfg = DamageRef.resolveTierConfigKeyForUUID(victimUUID, DifficultyIO.SETTING_DAMAGE_MULTIPLIER);
        if (damageMultiplierCfg <= 0) {
            return null;
        }

        return new DamageContext(
                damage.getAmount(),
                (float) Math.max(0.0, damageMultiplierCfg)
        );
    }

    private float applyDamageMultiplier(float damage, float damageMultiplier) {
        float d = Math.max(0.0f, damage);
        float m = Math.max(0.0f, damageMultiplier);
        float scaled = d * m;
        return Math.max(d * _minDamageFactor, scaled);
    }

    private record DamageContext(float baseDamage, float damageMultiplier) {
    }
}
