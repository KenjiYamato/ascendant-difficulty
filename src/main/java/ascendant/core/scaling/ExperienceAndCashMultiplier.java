package ascendant.core.scaling;

import ascendant.core.config.DifficultyManager;
import com.azuredoom.levelingcore.api.LevelingCoreApi;
import com.ecotale.api.EcotaleAPI;
import com.hypixel.hytale.component.CommandBuffer;
import com.hypixel.hytale.component.Ref;
import com.hypixel.hytale.component.Store;
import com.hypixel.hytale.logger.HytaleLogger;
import com.hypixel.hytale.protocol.packets.interface_.NotificationStyle;
import com.hypixel.hytale.server.core.universe.PlayerRef;
import com.hypixel.hytale.server.core.universe.Universe;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;

import javax.annotation.Nonnull;
import java.util.UUID;

import static com.hypixel.hytale.server.core.util.NotificationUtil.sendNotification;

public final class ExperienceAndCashMultiplier {
    private static final HytaleLogger LOGGER = HytaleLogger.forEnclosingClass();
    private static double _cashVarianceFactor;
    private static boolean _allowXPReward;
    private static boolean _allowCashReward;
    private static boolean _allowCashRewardEvenWithPhysical;

    public ExperienceAndCashMultiplier() {
        registerListener();
    }

    public static void registerListener() {
        _cashVarianceFactor = DifficultyManager.getConfig().getDouble("base.cashVarianceFactor", 0.23);
        _allowCashReward = DifficultyManager.getConfig().getBoolean("base.allowCashReward", true);
        _allowCashRewardEvenWithPhysical = DifficultyManager.getConfig().getBoolean("base.allowCashRewardEvenWithPhysical", true);
        _allowXPReward = DifficultyManager.getConfig().getBoolean("base.allowXPreward", true);

        if(!_allowCashReward && !_allowXPReward) {
            return;
        }

        LevelingCoreApi.getLevelServiceIfPresent().ifPresent(levelService -> {
            levelService.registerXpGainListener((UUID playerUuid, long amount) -> {
                if (amount <= 0.0) {
                    return;
                }
                assert playerUuid != null;
                PlayerRef playerRef = Universe.get().getPlayer(playerUuid);
                assert playerRef != null;

                String tierId = DifficultyManager.getDifficulty(playerUuid);
                if (tierId == null) {
                    return;
                }

                if(_allowXPReward) {
                    applyXPMultiplier(playerRef, playerUuid, tierId, amount);
                }

                if(_allowCashReward) {
                    applyCashMultiplier(playerUuid, tierId, amount);
                }
            });
        });
    }

    public static void applyXPMultiplier(@Nonnull PlayerRef playerRef, @Nonnull UUID playerUuid, String tierId, long amount) {

        double xpMultiplier = DifficultyManager.getSettings().get(tierId, "xp_multiplier");
        if (xpMultiplier <= 0.0 || xpMultiplier == 1.0) {
            return;
        }
        long finalAmount = (long) (amount * xpMultiplier - amount);
        long percentOfAmount = (long) ((xpMultiplier - 1.0) * 100);
        if (finalAmount <= 0.0) {
            return;
        }
        sendNotification(playerRef.getPacketHandler(), finalAmount + "XP (+" + percentOfAmount + "%)", NotificationStyle.Warning);
    }

    public static void applyCashMultiplier(@Nonnull UUID playerUuid, String tierId, long amount) {

        double cashMultiplier = DifficultyManager.getSettings().get(tierId, "cash_multiplier");
        if (cashMultiplier <= 0.0) {
            return;
        }

        double variance = _cashVarianceFactor;
        double min = cashMultiplier - variance;
        double max = cashMultiplier + variance;

        double factor = min + (Math.random() * (max - min));
        long finalAmount = (long) Math.max(1L, Math.floor((double) amount / 10 * factor));
        if (finalAmount <= 0.0) {
            return;
        }

        long percentOfAmount = (long) ((cashMultiplier - 1.0) * 100);
        if (EcotaleAPI.isAvailable()) {
            if (!EcotaleAPI.isPhysicalCoinsAvailable() || _allowCashRewardEvenWithPhysical) {
                EcotaleAPI.deposit(playerUuid, finalAmount, "Transfer for Difficulty (+" + percentOfAmount + "%)");
            }
        }
    }

    public static void applyCashMultiplier(@Nonnull UUID playerUuid, float cashMultiplier, @Nonnull Ref<EntityStore> entityRef, @Nonnull Store<EntityStore> store, @Nonnull CommandBuffer<EntityStore> commandBuffer) {

        if (EcotaleAPI.isAvailable()) {
            if (EcotaleAPI.isPhysicalCoinsAvailable()) {
                // drop rate multiplier already got physical coins
                /*
                 * PhysicalCoinsProvider coins = EcotaleAPI.getPhysicalCoins();
                 * long finalCoinsAmount = xxx * cashMultiplier;
                 * coins.dropCoinsAtEntity(entityRef, store, commandBuffer, finalCoinsAmount);
                 * */
            }
        }
    }
}
