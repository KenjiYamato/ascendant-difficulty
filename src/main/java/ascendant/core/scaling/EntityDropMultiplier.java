package ascendant.core.scaling;

import ascendant.core.config.DifficultyIO;
import ascendant.core.config.DifficultyManager;
import ascendant.core.config.RuntimeSettings;
import ascendant.core.util.NearestPlayerFinder;
import ascendant.core.util.ReflectionHelper;
import com.hypixel.hytale.component.CommandBuffer;
import com.hypixel.hytale.component.Holder;
import com.hypixel.hytale.component.Ref;
import com.hypixel.hytale.component.Store;
import com.hypixel.hytale.component.query.Query;
import com.hypixel.hytale.math.vector.Vector3d;
import com.hypixel.hytale.math.vector.Vector3f;
import com.hypixel.hytale.server.core.asset.type.gameplay.DeathConfig;
import com.hypixel.hytale.server.core.entity.entities.Player;
import com.hypixel.hytale.server.core.inventory.Inventory;
import com.hypixel.hytale.server.core.inventory.ItemStack;
import com.hypixel.hytale.server.core.modules.entity.component.HeadRotation;
import com.hypixel.hytale.server.core.modules.entity.component.TransformComponent;
import com.hypixel.hytale.server.core.modules.entity.damage.DeathComponent;
import com.hypixel.hytale.server.core.modules.entity.damage.DeathSystems;
import com.hypixel.hytale.server.core.modules.entity.item.ItemComponent;
import com.hypixel.hytale.server.core.modules.item.ItemModule;
import com.hypixel.hytale.server.core.universe.world.World;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;
import com.hypixel.hytale.server.npc.entities.NPCEntity;
import com.hypixel.hytale.server.npc.role.Role;
import it.unimi.dsi.fastutil.objects.ObjectArrayList;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.ThreadLocalRandom;

// NPCdamageSystems
@SuppressWarnings("removal")
public class EntityDropMultiplier extends DeathSystems.OnDeathSystem {

    @Nonnull
    private static final Query<EntityStore> QUERY = Query.and(NPCEntity.getComponentType(), TransformComponent.getComponentType(), HeadRotation.getComponentType(), Query.not(Player.getComponentType()));

    private final float _fallbackRadiusSq;
    private final boolean _allowDropModifier;

    public EntityDropMultiplier() {
        double radius = DifficultyManager.getFromConfig(DifficultyIO.PLAYER_DISTANCE_RADIUS_TO_CHECK);
        float r = (float) Math.max(0.0, radius);
        _fallbackRadiusSq = r * r;
        _allowDropModifier = DifficultyManager.getFromConfig(DifficultyIO.ALLOW_DROP_MODIFIER);
    }

    @Nonnull
    private static List<ItemStack> _applyDropRate(@Nonnull List<ItemStack> in, float multiplier) {
        if (multiplier <= 0.0f) {
            return List.of();
        }
        if (multiplier == 1.0f) {
            return in;
        }

        ThreadLocalRandom rng = ThreadLocalRandom.current();
        List<ItemStack> out = new ArrayList<>(in.size());

        if (multiplier < 1.0f) {
            for (ItemStack s : in) {
                if (s == null || s.isEmpty()) {
                    continue;
                }
                if (rng.nextFloat() <= multiplier) {
                    out.add(s);
                }
            }
            return out;
        }

        int guaranteedCopies = (int) Math.floor(multiplier);
        float fractional = multiplier - guaranteedCopies;

        for (ItemStack s : in) {
            if (s == null || s.isEmpty()) {
                continue;
            }

            for (int i = 0; i < guaranteedCopies; i++) {
                out.add(s);
            }

            if (fractional > 0.0f && rng.nextFloat() < fractional) {
                out.add(s);
            }
        }

        return out;
    }

    @Nonnull
    private static List<ItemStack> _applyDropQuantity(@Nonnull List<ItemStack> in, float multiplier) {
        if (multiplier <= 0.0f) {
            return List.of();
        }
        if (multiplier == 1.0f) {
            return in;
        }

        ThreadLocalRandom rng = ThreadLocalRandom.current();
        List<ItemStack> out = new ArrayList<>(in.size());

        for (ItemStack s : in) {
            if (s == null || s.isEmpty()) {
                continue;
            }

            int qty = s.getQuantity();
            if (qty <= 0) {
                continue;
            }

            float scaled = qty * multiplier;
            int base = (int) Math.floor(scaled);
            float fractional = scaled - base;

            int finalQty = base + ((fractional > 0.0f && rng.nextFloat() < fractional) ? 1 : 0);
            if (finalQty <= 0) {
                continue;
            }

            ItemStack updated = s.withQuantity(finalQty);
            out.add(updated != null ? updated : s);
        }

        return out;
    }

    @Nonnull
    private static List<ItemStack> _applyDropQuality(@Nonnull List<ItemStack> in, float multiplier) {
        if (multiplier <= 0.0f) {
            return List.of();
        }
        if (multiplier == 1.0f) {
            return in;
        }

        List<ItemStack> out = new ArrayList<>(in.size());
        for (ItemStack s : in) {
            if (s == null || s.isEmpty()) {
                continue;
            }

            ItemStack updated = ReflectiveItemQualityBridge.scaleQuality(s, multiplier);
            out.add(updated != null ? updated : s);
        }
        return out;
    }

    @Nonnull
    @Override
    public Query<EntityStore> getQuery() {
        return QUERY;
    }

    @Override
    public void onComponentAdded(
            @Nonnull Ref<EntityStore> ref,
            @Nonnull DeathComponent component,
            @Nonnull Store<EntityStore> store,
            @Nonnull CommandBuffer<EntityStore> commandBuffer
    ) {
        if (!_allowDropModifier) {
            return;
        }
        if (component.getItemsLossMode() != DeathConfig.ItemsLossMode.ALL) {
            return;
        }

        NPCEntity npcComponent = commandBuffer.getComponent(ref, NPCEntity.getComponentType());
        if (npcComponent == null) {
            return;
        }

        Role role = npcComponent.getRole();
        if (role == null) {
            return;
        }

        String spawnTier = null;
        if (RuntimeSettings.allowSpawnTierReward()) {
            spawnTier = NearestPlayerHealthScaleSystem.getSpawnTier(store, ref);
        }

        UUID playerUuid = _resolveRelevantPlayerUuid(store, ref);
        String playerTier = null;
        if (playerUuid != null) {
            playerTier = DifficultyManager.getDifficulty(playerUuid);
        }

        String tierId = spawnTier;
        if (tierId == null) {
            if (playerTier == null || playerTier.isBlank()) {
                return;
            }
            tierId = playerTier;
        }

        double dropRateCfg = DifficultyManager.getSettings().get(tierId, DifficultyIO.SETTING_DROP_RATE_MULTIPLIER);
        double dropQtyCfg = DifficultyManager.getSettings().get(tierId, DifficultyIO.SETTING_DROP_QUANTITY_MULTIPLIER);
        double dropQualityCfg = DifficultyManager.getSettings().get(tierId, DifficultyIO.SETTING_DROP_QUALITY_MULTIPLIER);

        double rewardScale = ExperienceAndCashMultiplier.computeTierMismatchScale(playerTier, spawnTier);
        float dropRateMult = (float) Math.max(0.0, dropRateCfg * rewardScale);
        float dropQtyMult = (float) Math.max(0.0, dropQtyCfg * rewardScale);
        float dropQualityMult = (float) Math.max(0.0, dropQualityCfg * rewardScale);

        List<ItemStack> itemsToDrop = new ObjectArrayList<>();

        if (role.isPickupDropOnDeath()) {
            Inventory inventory = npcComponent.getInventory();
            if (inventory != null) {
                itemsToDrop.addAll(inventory.getStorage().dropAllItemStacks());
            }
        }

        String dropListId = role.getDropListId();
        if (dropListId != null) {
            ItemModule itemModule = ItemModule.get();
            if (itemModule.isEnabled()) {
                itemsToDrop.addAll(itemModule.getRandomItemDrops(dropListId));
            }
        }

        if (itemsToDrop.isEmpty()) {
            return;
        }

        List<ItemStack> afterRate = _applyDropRate(itemsToDrop, dropRateMult);
        if (afterRate.isEmpty()) {
            return;
        }

        List<ItemStack> afterQty = _applyDropQuantity(afterRate, dropQtyMult);
        if (afterQty.isEmpty()) {
            return;
        }

        List<ItemStack> afterQuality = _applyDropQuality(afterQty, dropQualityMult);
        if (afterQuality.isEmpty()) {
            return;
        }

        TransformComponent transformComponent = store.getComponent(ref, TransformComponent.getComponentType());
        HeadRotation headRotationComponent = store.getComponent(ref, HeadRotation.getComponentType());
        if (transformComponent == null || headRotationComponent == null) {
            return;
        }

        Vector3d position = transformComponent.getPosition();
        Vector3f headRotation = headRotationComponent.getRotation();
        Vector3d dropPosition = position.clone().add(0.0, 1.0, 0.0);

        Holder<EntityStore>[] drops = ItemComponent.generateItemDrops(store, afterQuality, dropPosition, headRotation.clone());
        commandBuffer.addEntities(drops, com.hypixel.hytale.component.AddReason.SPAWN);
    }

    @Nullable
    private UUID _resolveRelevantPlayerUuid(@Nonnull Store<EntityStore> store, @Nonnull Ref<EntityStore> victimRef) {
        if (_fallbackRadiusSq <= 0.0f) {
            return null;
        }

        World world = store.getExternalData().getWorld();
        Player nearest = NearestPlayerFinder.findNearestPlayer(world, store, victimRef, _fallbackRadiusSq);
        return nearest != null ? nearest.getUuid() : null;
    }

    private static final class ReflectiveItemQualityBridge {

        private static final Method GET_RARITY;
        private static final Method WITH_RARITY;
        private static final Method GET_QUALITY_INT;
        private static final Method WITH_QUALITY_INT;

        static {
            Method getRarity = null;
            Method withRarity = null;
            Method getQualityInt = null;
            Method withQualityInt = null;

            try {
                Class<?> c = ReflectionHelper.resolveClass(
                        "com.hypixel.hytale.server.core.inventory.ItemStack",
                        EntityDropMultiplier.class.getClassLoader()
                );
                if (c == null) {
                    throw new ClassNotFoundException("com.hypixel.hytale.server.core.inventory.ItemStack");
                }

                getRarity = ReflectionHelper.getAnyMethod(c, "getRarity");
                if (getRarity != null && getRarity.getReturnType().isEnum()) {
                    Class<?> enumType = getRarity.getReturnType();
                    withRarity = ReflectionHelper.getAnyMethod(c, "withRarity", enumType);
                }

                getQualityInt = ReflectionHelper.getAnyMethod(c, "getQuality");
                if (getQualityInt != null && (getQualityInt.getReturnType() == int.class || getQualityInt.getReturnType() == Integer.class)) {
                    withQualityInt = ReflectionHelper.getAnyMethod(c, "withQuality", int.class);
                    if (withQualityInt == null) {
                        withQualityInt = ReflectionHelper.getAnyMethod(c, "withQuality", Integer.class);
                    }
                }
            } catch (Throwable ignored) {
            }

            GET_RARITY = getRarity;
            WITH_RARITY = withRarity;
            GET_QUALITY_INT = getQualityInt;
            WITH_QUALITY_INT = withQualityInt;
        }

        private ReflectiveItemQualityBridge() {
        }

        @Nullable
        static ItemStack scaleQuality(@Nonnull ItemStack stack, float multiplier) {
            if (!Float.isFinite(multiplier)) {
                return stack;
            }

            ItemStack rarityScaled = _scaleEnumRarity(stack, multiplier);
            if (rarityScaled != null) {
                return rarityScaled;
            }

            ItemStack qualityScaled = _scaleIntQuality(stack, multiplier);
            return qualityScaled != null ? qualityScaled : stack;
        }

        @Nullable
        private static ItemStack _scaleEnumRarity(@Nonnull ItemStack stack, float multiplier) {
            if (GET_RARITY == null || WITH_RARITY == null) {
                return null;
            }

            try {
                Object rarityObj = GET_RARITY.invoke(stack);
                if (!(rarityObj instanceof Enum<?> e)) {
                    return null;
                }

                Enum<?>[] values = e.getDeclaringClass().getEnumConstants();
                if (values == null || values.length == 0) {
                    return null;
                }

                int idx = e.ordinal();
                int target = Math.round((idx + 1) * multiplier) - 1;
                if (target < 0) target = 0;
                if (target >= values.length) target = values.length - 1;

                if (target == idx) {
                    return stack;
                }

                Object updated = WITH_RARITY.invoke(stack, values[target]);
                return updated instanceof ItemStack s ? s : null;
            } catch (Throwable ignored) {
                return null;
            }
        }

        @Nullable
        private static ItemStack _scaleIntQuality(@Nonnull ItemStack stack, float multiplier) {
            if (GET_QUALITY_INT == null || WITH_QUALITY_INT == null) {
                return null;
            }

            try {
                Object qObj = GET_QUALITY_INT.invoke(stack);
                if (!(qObj instanceof Number n)) {
                    return null;
                }

                int q = n.intValue();
                int scaled = Math.round(q * multiplier);
                if (scaled < 0) scaled = 0;

                Object updated = WITH_QUALITY_INT.invoke(stack, scaled);
                return updated instanceof ItemStack s ? s : null;
            } catch (Throwable ignored) {
                return null;
            }
        }

    }
}
