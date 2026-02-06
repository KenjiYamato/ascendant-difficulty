package ascendant.core.scaling;

import ascendant.core.config.DifficultyIO;
import ascendant.core.config.DifficultyManager;
import ascendant.core.util.LibraryAvailability;
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
        _cashVarianceFactor = DifficultyManager.getFromConfig(DifficultyIO.CASH_VARIANCE_FACTOR);
        _allowCashReward = DifficultyManager.getFromConfig(DifficultyIO.ALLOW_CASH_REWARD);
        _allowCashRewardEvenWithPhysical = DifficultyManager.getFromConfig(DifficultyIO.ALLOW_CASH_REWARD_EVEN_WITH_PHYSICAL);
        _allowXPReward = DifficultyManager.getFromConfig(DifficultyIO.ALLOW_XP_REWARD);

        if (!_allowCashReward && !_allowXPReward) {
            return;
        }

        if (!LibraryAvailability.isLevelingCorePresent()) {
            return;
        }

        try {
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

                    if (_allowXPReward) {
                        applyXPMultiplier(playerRef, playerUuid, tierId, amount);
                    }

                    if (_allowCashReward) {
                        applyCashMultiplier(playerUuid, tierId, amount);
                    }
                });
            });
        } catch (NoClassDefFoundError error) {
            LibraryAvailability.logMissingDependency("LevelingCore", error);
        }
    }

    public static void applyXPMultiplier(@Nonnull PlayerRef playerRef, @Nonnull UUID playerUuid, String tierId, long amount) {
        if (!LibraryAvailability.isLevelingCorePresent()) {
            return;
        }

        double xpMultiplier = DifficultyManager.getSettings().get(tierId, DifficultyIO.SETTING_XP_MULTIPLIER);
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
        if (!LibraryAvailability.isEcotalePresent()) {
            return;
        }

        double cashMultiplier = DifficultyManager.getSettings().get(tierId, DifficultyIO.SETTING_CASH_MULTIPLIER);
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

        try {
            if (EcotaleAPI.isAvailable()) {
                if (!EcotaleAPI.isPhysicalCoinsAvailable() || _allowCashRewardEvenWithPhysical) {
                    EcotaleAPI.deposit(playerUuid, finalAmount, "Transfer for Difficulty (+" + percentOfAmount + "%)");
                }
            }
        } catch (NoClassDefFoundError error) {
            LibraryAvailability.logMissingDependency("Ecotale", error);
        }
    }

    public static void applyCashMultiplier(@Nonnull UUID playerUuid, float cashMultiplier, @Nonnull Ref<EntityStore> entityRef, @Nonnull Store<EntityStore> store, @Nonnull CommandBuffer<EntityStore> commandBuffer) {
        if (!LibraryAvailability.isEcotalePresent()) {
            return;
        }

        try {
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
        } catch (NoClassDefFoundError error) {
            LibraryAvailability.logMissingDependency("Ecotale", error);
        }
    }
}
