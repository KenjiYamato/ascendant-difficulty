package ascendant.core.scaling;

import ascendant.core.config.DifficultyManager;
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
    private static final Query<EntityStore> QUERY = Query.and(new Query[]{NPCEntity.getComponentType(), TransformComponent.getComponentType(), HeadRotation.getComponentType(), Query.not(Player.getComponentType())});

    private final float _fallbackRadiusSq;
    private final boolean _allowDropModifier;

    public EntityDropMultiplier() {
        double radius = DifficultyManager.getConfig().getDouble("base.playerDistanceRadiusToCheck", 48.0);
        float r = (float) Math.max(0.0, radius);
        _fallbackRadiusSq = r * r;
        _allowDropModifier = DifficultyManager.getConfig().getBoolean("base.allowDropModifier", true);
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

        UUID playerUuid = _resolveRelevantPlayerUuid(store, ref);
        if (playerUuid == null) {
            return;
        }

        String tierId = DifficultyManager.getDifficulty(playerUuid);
        if (tierId == null) {
            return;
        }

        double dropRateCfg = DifficultyManager.getSettings().get(tierId, "drop_rate_multiplier");
        double dropQtyCfg = DifficultyManager.getSettings().get(tierId, "drop_quantity_multiplier");
        double dropQualityCfg = DifficultyManager.getSettings().get(tierId, "drop_quality_multiplier");
        double cashMultiplierCfg = DifficultyManager.getSettings().get(tierId, "cash_multiplier");

        float dropRateMult = (float) Math.max(0.0, dropRateCfg);
        float dropQtyMult = (float) Math.max(0.0, dropQtyCfg);
        float dropQualityMult = (float) Math.max(0.0, dropQualityCfg);
        float cashMultiplier = (float) Math.max(0.0, cashMultiplierCfg);

        ExperienceAndCashMultiplier.applyCashMultiplier(playerUuid, cashMultiplier, ref, store, commandBuffer);

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
        Player nearest = _findNearestPlayer(world, store, victimRef);
        return nearest != null ? nearest.getUuid() : null;
    }

    @Nullable
    private Player _findNearestPlayer(@Nonnull World world, @Nonnull Store<EntityStore> store, @Nonnull Ref<EntityStore> victimRef) {
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
    private Vector3d _getPosition(@Nonnull Store<EntityStore> store, @Nonnull Ref<EntityStore> ref) {
        TransformComponent tc = store.getComponent(ref, TransformComponent.getComponentType());
        return tc != null ? tc.getPosition() : null;
    }

    private double _distanceSq(@Nonnull Vector3d a, @Nonnull Vector3d b) {
        double dx = a.getX() - b.getX();
        double dy = a.getY() - b.getY();
        double dz = a.getZ() - b.getZ();
        return dx * dx + dy * dy + dz * dz;
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
                Class<?> c = Class.forName("com.hypixel.hytale.server.core.inventory.ItemStack");

                getRarity = _findMethod(c, "getRarity");
                if (getRarity != null && getRarity.getReturnType().isEnum()) {
                    Class<?> enumType = getRarity.getReturnType();
                    withRarity = _findMethod(c, "withRarity", enumType);
                }

                getQualityInt = _findMethod(c, "getQuality");
                if (getQualityInt != null && (getQualityInt.getReturnType() == int.class || getQualityInt.getReturnType() == Integer.class)) {
                    withQualityInt = _findMethod(c, "withQuality", int.class);
                    if (withQualityInt == null) {
                        withQualityInt = _findMethod(c, "withQuality", Integer.class);
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

                Enum<?>[] values = (Enum<?>[]) e.getDeclaringClass().getEnumConstants();
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

        @Nullable
        private static Method _findMethod(@Nonnull Class<?> c, @Nonnull String name, Class<?>... params) {
            try {
                Method m = c.getDeclaredMethod(name, params);
                m.setAccessible(true);
                return m;
            } catch (Throwable ignored) {
                try {
                    Method m = c.getMethod(name, params);
                    m.setAccessible(true);
                    return m;
                } catch (Throwable ignored2) {
                    return null;
                }
            }
        }
    }
}
