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
import com.hypixel.hytale.math.vector.Vector3d;
import com.hypixel.hytale.server.core.entity.Entity;
import com.hypixel.hytale.server.core.entity.EntityUtils;
import com.hypixel.hytale.server.core.entity.LivingEntity;
import com.hypixel.hytale.server.core.entity.entities.Player;
import com.hypixel.hytale.server.core.modules.entity.AllLegacyLivingEntityTypesQuery;
import com.hypixel.hytale.server.core.modules.entity.component.TransformComponent;
import com.hypixel.hytale.server.core.modules.entity.damage.*;
import com.hypixel.hytale.server.core.modules.entity.damage.DamageCalculatorSystems.DamageSequence;
import com.hypixel.hytale.server.core.universe.world.World;
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
        double radius = DifficultyManager.getConfig().getDouble("base.playerDistanceRadiusToCheck", 48.0);
        float r = (float) Math.max(0.0, radius);
        _fallbackRadiusSq = r * r;
        double minDamageFactor = DifficultyManager.getConfig().getDouble("base.minDamageFactor", 0.001);
        _minDamageFactor = (float) Math.max(0.0, minDamageFactor);
        _allowArmorModifier = DifficultyManager.getConfig().getBoolean("base.allowArmorModifier", true);
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
        if(!_allowArmorModifier) {
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
        if (damage.getAmount() <= 0.0f) {
            return null;
        }

        DamageCause cause = damage.getCause();
        if (cause == null || cause.doesBypassResistances()) {
            return null;
        }

        DamageSequence seq =
                (DamageSequence) damage.getIfPresentMetaObject(DamageCalculatorSystems.DAMAGE_SEQUENCE);
        if (seq == null) {
            return null;
        }

        Ref<EntityStore> victimRef = chunk.getReferenceTo(index);
        if (!victimRef.isValid()) {
            return null;
        }

        LivingEntity victim = (LivingEntity) EntityUtils.getEntity(index, chunk);
        if (victim == null) {
            return null;
        }

        UUID playerUuid = _resolveRelevantPlayerUuid(damage, store, commandBuffer, victimRef);
        if (playerUuid == null) {
            return null;
        }

        String tierId = DifficultyManager.getDifficulty(playerUuid);
        if (tierId == null) {
            return null;
        }

        double armorCfg = DifficultyManager.getSettings().get(tierId, "armor_multiplier");

        return new DamageContext(
                damage.getAmount(),
                (float) Math.max(0.0, armorCfg)
        );
    }

    @Nullable
    @SuppressWarnings("removal")
    private UUID _resolveRelevantPlayerUuid(
            Damage damage,
            Store<EntityStore> store,
            CommandBuffer<EntityStore> commandBuffer,
            Ref<EntityStore> victimRef
    ) {
        UUID attacker = _resolveAttackerPlayerUuid(damage, store);
        if (attacker != null) {
            return attacker;
        }

        if (_fallbackRadiusSq <= 0.0f) {
            return null;
        }

        World world = ((EntityStore) commandBuffer.getExternalData()).getWorld();
        Player nearest = _findNearestPlayer(world, store, victimRef);
        return nearest != null ? nearest.getUuid() : null;
    }

    @Nullable
    @SuppressWarnings("removal")
    private UUID _resolveAttackerPlayerUuid(Damage damage, Store<EntityStore> store) {
        if (!(damage.getSource() instanceof Damage.EntitySource src)) {
            return null;
        }

        Ref<EntityStore> ref = src.getRef();
        if (!ref.isValid()) {
            return null;
        }

        Entity attacker = EntityUtils.getEntity(ref, store);
        if (!(attacker instanceof Player player)) {
            return null;
        }

        return player.getUuid();
    }

    @Nullable
    @SuppressWarnings("removal")
    private Player _findNearestPlayer(World world, Store<EntityStore> store, Ref<EntityStore> victimRef) {
        Vector3d victimPos = _getPosition(store, victimRef);
        if (victimPos == null) {
            return null;
        }

        Player nearest = null;
        double best = Double.MAX_VALUE;

        for (Player p : world.getPlayers()) {
            Ref<EntityStore> pref = p.getReference();
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
    private Vector3d _getPosition(Store<EntityStore> store, Ref<EntityStore> ref) {
        TransformComponent tc = (TransformComponent) store.getComponent(ref, TransformComponent.getComponentType());
        if (tc == null) {
            return null;
        }
        return tc.getPosition();
    }

    private double _distanceSq(Vector3d a, Vector3d b) {
        double dx = a.getX() - b.getX();
        double dy = a.getY() - b.getY();
        double dz = a.getZ() - b.getZ();
        return dx * dx + dy * dy + dz * dz;
    }

    private record DamageContext(float baseDamage, float armorMultiplier) {
    }

    private float applyArmor(float damage, float armorMultiplier) {
        float d = Math.max(0.0f, damage);
        float m = Math.max(0.0f, armorMultiplier);
        float reduced = d / (1.0f + m);
        return Math.max(d * _minDamageFactor, reduced);
    }
}
