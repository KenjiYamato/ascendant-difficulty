package ascendant.core.scaling;

import ascendant.core.config.DifficultyIO;
import ascendant.core.config.DifficultyManager;
import ascendant.core.util.DamageRef;
import ascendant.core.util.NearestPlayerFinder;
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
import com.hypixel.hytale.server.core.entity.EntityUtils;
import com.hypixel.hytale.server.core.entity.LivingEntity;
import com.hypixel.hytale.server.core.entity.entities.Player;
import com.hypixel.hytale.server.core.modules.entity.AllLegacyLivingEntityTypesQuery;
import com.hypixel.hytale.server.core.modules.entity.damage.*;
import com.hypixel.hytale.server.core.modules.entity.damage.DamageCalculatorSystems.DamageSequence;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.util.Set;
import java.util.UUID;

public final class EntityDamageReceiveMultiplier extends DamageEventSystem {

    private static final HytaleLogger LOGGER = HytaleLogger.forEnclosingClass();

    private final Set<Dependency<EntityStore>> _dependencies;
    private final float _fallbackRadiusSq;
    private final float _minDamageFactor;
    private final boolean _allowArmorModifier;

    public EntityDamageReceiveMultiplier() {
        _dependencies = Set.of(
                new SystemGroupDependency(Order.AFTER, DamageModule.get().getGatherDamageGroup()),
                new SystemGroupDependency(Order.AFTER, DamageModule.get().getFilterDamageGroup()),
                new SystemGroupDependency(Order.BEFORE, DamageModule.get().getInspectDamageGroup()),
                new SystemDependency(Order.BEFORE, DamageSystems.ApplyDamage.class)
        );
        double radius = DifficultyManager.getFromConfig(DifficultyIO.PLAYER_DISTANCE_RADIUS_TO_CHECK);
        float r = (float) Math.max(0.0, radius);
        _fallbackRadiusSq = r * r;
        double minDamageFactor = DifficultyManager.getFromConfig(DifficultyIO.MIN_DAMAGE_FACTOR);
        _minDamageFactor = (float) Math.max(0.0, minDamageFactor);
        _allowArmorModifier = DifficultyManager.getFromConfig(DifficultyIO.ALLOW_ARMOR_MODIFIER);
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
        if (!_allowArmorModifier) {
            return;
        }
        DamageContext ctx = buildContext(index, chunk, store, commandBuffer, damage);
        if (ctx == null) {
            return;
        }

        float afterArmor = applyArmor(ctx.baseDamage, ctx.armorMultiplier);

        damage.setAmount(afterArmor);
    }

    @Nullable
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

        DamageSequence seq =
                damage.getIfPresentMetaObject(DamageCalculatorSystems.DAMAGE_SEQUENCE);
        if (seq == null) {
            return null;
        }

        Ref<EntityStore> victimRef = chunk.getReferenceTo(index);
        if (!victimRef.isValid()) {
            return null;
        }

        LivingEntity victim = (LivingEntity) EntityUtils.getEntity(index, chunk);
        if (victim == null || victim instanceof Player) {
            return null;
        }

        UUID playerUuid = NearestPlayerFinder.resolveRelevantPlayerUuid(damage, store, commandBuffer, victimRef, _fallbackRadiusSq);
        if (playerUuid == null) {
            return null;
        }

        String tierId = DifficultyManager.getDifficulty(playerUuid);
        if (tierId == null) {
            return null;
        }

        double armorCfg = DifficultyManager.getSettings().get(tierId, DifficultyIO.SETTING_ARMOR_MULTIPLIER);

        return new DamageContext(
                damage.getAmount(),
                (float) Math.max(0.0, armorCfg)
        );
    }

    private float applyArmor(float damage, float armorMultiplier) {
        float d = Math.max(0.0f, damage);
        float m = Math.max(0.0f, armorMultiplier);
        float reduced = d / (1.0f + m);
        return Math.max(d * _minDamageFactor, reduced);
    }

    private record DamageContext(float baseDamage, float armorMultiplier) {
    }
}
