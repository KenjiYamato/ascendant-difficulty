package ascendant.core.scaling;

import ascendant.core.config.DifficultyManager;
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
import com.hypixel.hytale.server.core.entity.Entity;
import com.hypixel.hytale.server.core.entity.EntityUtils;
import com.hypixel.hytale.server.core.entity.entities.Player;
import com.hypixel.hytale.server.core.modules.entity.AllLegacyLivingEntityTypesQuery;
import com.hypixel.hytale.server.core.modules.entity.damage.*;
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
                new SystemGroupDependency(Order.BEFORE, DamageModule.get().getInspectDamageGroup()),
                new SystemDependency(Order.BEFORE, DamageSystems.ApplyDamage.class)
        );
        double minDamageFactor = DifficultyManager.getConfig().getDouble("base.minDamageFactor", 0.001);
        _minDamageFactor = (float) minDamageFactor;
        _allowDamageModifier = DifficultyManager.getConfig().getBoolean("base.allowDamageModifier", true);
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
        if(!_allowDamageModifier) {
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
        if (damage.getAmount() <= 0.0f) {
            return null;
        }

        DamageCause cause = damage.getCause();
        if (cause == null) {
            return null;
        }

        Ref<EntityStore> victimRef = chunk.getReferenceTo(index);
        if (!victimRef.isValid()) {
            return null;
        }

        Entity victim = EntityUtils.getEntity(victimRef, store);
        if (!(victim instanceof Player)) {
            return null;
        }

        UUID playerUuid = victim.getUuid();
        if (playerUuid == null) {
            return null;
        }

        String tierId = DifficultyManager.getDifficulty(playerUuid);
        if (tierId == null) {
            return null;
        }

        double damageMultiplierCfg = DifficultyManager.getSettings().get(tierId, "damage_multiplier");

        return new DamageContext(
                damage.getAmount(),
                (float) Math.max(0.0, damageMultiplierCfg)
        );
    }

    private record DamageContext(float baseDamage, float damageMultiplier) {
    }

    private float applyDamageMultiplier(float damage, float damageMultiplier) {
        float d = Math.max(0.0f, damage);
        float m = Math.max(0.0f, damageMultiplier);
        float scaled = d * m;
        return Math.max(d * _minDamageFactor, scaled);
    }
}
