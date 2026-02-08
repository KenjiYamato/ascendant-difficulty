package ascendant.core.scaling;

import ascendant.core.config.DifficultyIO;
import ascendant.core.config.DifficultyManager;
import ascendant.core.config.DifficultySettings;
import ascendant.core.integration.elitemobs.EliteMobsDifficultySpawner;
import ascendant.core.util.NearestPlayerFinder;
import com.hypixel.hytale.component.*;
import com.hypixel.hytale.component.query.Query;
import com.hypixel.hytale.component.system.RefSystem;
import com.hypixel.hytale.server.core.entity.entities.Player;
import com.hypixel.hytale.server.core.modules.entity.AllLegacyLivingEntityTypesQuery;
import com.hypixel.hytale.server.core.universe.world.World;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;
import com.hypixel.hytale.server.npc.entities.NPCEntity;
import org.checkerframework.checker.nullness.qual.NonNull;

import javax.annotation.Nonnull;
import java.util.UUID;

@SuppressWarnings("removal")
public final class EntityEliteSpawn extends RefSystem<EntityStore> {

    private final float _fallbackRadiusSq;
    private final boolean _allowEliteSpawnModifier;

    public EntityEliteSpawn() {
        double radius = DifficultyManager.getFromConfig(DifficultyIO.PLAYER_DISTANCE_RADIUS_TO_CHECK);
        _allowEliteSpawnModifier = DifficultyManager.getFromConfig(DifficultyIO.ALLOW_ELITE_SPAWN_MODIFIER);
        float r = (float) Math.max(0.0, radius);
        _fallbackRadiusSq = r * r;
    }

    @Nonnull
    @Override
    public Query<EntityStore> getQuery() {
        return AllLegacyLivingEntityTypesQuery.INSTANCE;
    }

    @Override
    public void onEntityAdded(@Nonnull Ref<EntityStore> ref,
                              @Nonnull AddReason reason,
                              @Nonnull Store<EntityStore> store,
                              @Nonnull CommandBuffer<EntityStore> commandBuffer) {

        if (!_allowEliteSpawnModifier || reason != AddReason.SPAWN) {
            return;
        }

        Holder<EntityStore> holder = store.copyEntity(ref);
        Player maybePlayer = holder.getComponent(Player.getComponentType());
        if (maybePlayer != null) {
            return;
        }

        World world = store.getExternalData().getWorld();
        Player nearest = NearestPlayerFinder.findNearestPlayer(world, store, holder, _fallbackRadiusSq);
        if (nearest == null) {
            return;
        }

        UUID playerUuid = nearest.getUuid();
        String tierId = DifficultyManager.getDifficulty(playerUuid);
        DifficultySettings settings = DifficultyManager.getSettings();

        ComponentType<EntityStore, NPCEntity> componentType = NPCEntity.getComponentType();
        if(componentType == null) {
            return;
        }
        NPCEntity npc = (NPCEntity)store.getComponent(ref, componentType);
        if(npc == null) {
            return;
        }

        if (EliteMobsDifficultySpawner.isAvailable()) {
            float factor = (float) settings.get(tierId, DifficultyIO.SETTING_ELITE_SPAWN_MULTIPLIER);
            // 0.15
            double uncommonChance = factor + settings.get(tierId, DifficultyIO.SETTING_ELITE_MOBS_CHANCE_UNCOMMON);
            // 0.08
            double rareChance = settings.get(tierId, DifficultyIO.SETTING_ELITE_MOBS_CHANCE_RARE);
            // 0.02
            double legendaryChance = settings.get(tierId, DifficultyIO.SETTING_ELITE_MOBS_CHANCE_LEGENDARY);
            EliteMobsDifficultySpawner.rollAndApply(npc, ref, store, commandBuffer,
                    uncommonChance, rareChance, legendaryChance);
        }
    }

    @Override
    public void onEntityRemove(@NonNull Ref<EntityStore> ref, @NonNull RemoveReason removeReason, @NonNull Store<EntityStore> store, @NonNull CommandBuffer<EntityStore> commandBuffer) {

    }
}
