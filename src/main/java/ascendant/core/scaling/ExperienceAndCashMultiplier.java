package ascendant.core.scaling;

import ascendant.core.config.DifficultyIO;
import ascendant.core.config.DifficultyManager;
import ascendant.core.config.RuntimeSettings;
import ascendant.core.util.LibraryAvailability;
import ascendant.core.util.Logging;
import ascendant.core.util.PlayerWorldExecutor;
import com.azuredoom.levelingcore.api.LevelingCoreApi;
import com.azuredoom.levelingcore.ui.hud.XPBarHud;
import com.ecotale.api.EcotaleAPI;
import com.hypixel.hytale.component.Ref;
import com.hypixel.hytale.component.Store;
import com.hypixel.hytale.logger.HytaleLogger;
import com.hypixel.hytale.server.core.universe.PlayerRef;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;
import com.ziggfreed.mmoskilltree.api.MMOSkillTreeAPI;
import com.ziggfreed.mmoskilltree.data.SkillType;

import javax.annotation.Nonnull;
import java.util.UUID;

import static ascendant.core.config.DifficultyIO.DEFAULT_BASE_DIFFICULTY;
import static ascendant.core.config.DifficultyMeta.KEY_DISPLAY_NAME;
import static ascendant.core.config.DifficultyMeta.META_PREFIX;

public final class ExperienceAndCashMultiplier {
    private static final HytaleLogger LOGGER = HytaleLogger.forEnclosingClass();

    public ExperienceAndCashMultiplier() {
        initialize();
    }

    public static void initialize() {
        RuntimeSettings.load();

        if (!RuntimeSettings.allowCashReward() && !RuntimeSettings.allowXPReward()) {
            return;
        }

        if (RuntimeSettings.allowMMOSkillTreeIntegration()
                && RuntimeSettings.allowXPReward()
                && !LibraryAvailability.isMMOSkillTreePresent()) {
            return;
        }
    }

    private static MultiplierResult getMultiplierXPAmount(String tierId, long amount, double integrationMultiplier) {
        if (amount <= 0L) {
            return new MultiplierResult(amount, 0L, 0L, tierId);
        }
        double effectiveMultiplier = integrationMultiplier <= 0.0 ? 1.0 : integrationMultiplier;
        double xpMultiplier = DifficultyManager.getSettings().get(tierId, DifficultyIO.SETTING_XP_MULTIPLIER);
        double baseMultiplier = xpMultiplier - 1.0;
        if (baseMultiplier <= 0.0) {
            return new MultiplierResult(amount, 0L, 0L, tierId);
        }
        double scaledMultiplier = baseMultiplier * effectiveMultiplier;
        if (scaledMultiplier <= 0.0) {
            return new MultiplierResult(amount, 0L, 0L, tierId);
        }
        long extraAmount = (long) Math.floor((double) amount * scaledMultiplier);
        if (extraAmount <= 0L) {
            return new MultiplierResult(amount, 0L, 0L, tierId);
        }
        long percent = Math.round(scaledMultiplier * 100.0);
        return new MultiplierResult(amount, extraAmount, percent, tierId);
    }

    private static MultiplierResult getMultiplierCashAmount(String tierId, long amount, double integrationMultiplier) {
        if (amount <= 0L) {
            return new MultiplierResult(amount, 0L, 0L, tierId);
        }
        double effectiveMultiplier = integrationMultiplier <= 0.0 ? 1.0 : integrationMultiplier;
        double cashMultiplier = DifficultyManager.getSettings().get(tierId, DifficultyIO.SETTING_CASH_MULTIPLIER);
        if (cashMultiplier <= 0.0) {
            return new MultiplierResult(amount, 0L, 0L, tierId);
        }

        double variance = RuntimeSettings.cashVarianceFactor();
        double min = cashMultiplier - variance;
        double max = cashMultiplier + variance;

        double factor = min + (Math.random() * (max - min));
        long baseExtra = (long) Math.max(1L, Math.floor((double) amount / 10 * factor));
        long extraAmount = (long) Math.floor(baseExtra * effectiveMultiplier);
        if (extraAmount <= 0L) {
            return new MultiplierResult(amount, 0L, 0L, tierId);
        }

        long percent = Math.round((cashMultiplier - 1.0) * 100.0 * effectiveMultiplier);
        return new MultiplierResult(amount, extraAmount, percent, tierId);
    }

    public static MultiplierResult applyLevelingCoreXPMultiplier(@Nonnull PlayerRef playerRef, long amount) {
        if (!RuntimeSettings.allowLevelingCoreIntegration() || !RuntimeSettings.allowXPReward()) {
            return new MultiplierResult(amount, 0L, 0L, DEFAULT_BASE_DIFFICULTY);
        }
        if (!LibraryAvailability.isLevelingCorePresent()) {
            return new MultiplierResult(amount, 0L, 0L, DEFAULT_BASE_DIFFICULTY);
        }

        UUID playerUuid = playerRef.getUuid();
        String tierId = DifficultyManager.getDifficulty(playerUuid);
        if (tierId == null) {
            return new MultiplierResult(amount, 0L, 0L, DEFAULT_BASE_DIFFICULTY);
        }

        MultiplierResult result = getMultiplierXPAmount(tierId, amount, RuntimeSettings.levelingCoreMultiplier());

        if (result.isZero()) {
            return new MultiplierResult(amount, 0L, 0L, tierId);
        }

        long extraXp = result.extraAmount();

        try {
            LevelingCoreApi.getLevelServiceIfPresent().ifPresent(levelService -> {
                Runnable task = () -> {
                    levelService.addXp(playerUuid, extraXp);
                    XPBarHud.updateHud(playerRef);
                };
                if (!PlayerWorldExecutor.execute(playerRef, task)) {
                    Logging.debug("[XP] Failed to schedule LevelingCore XP update for " + playerUuid);
                }
                //sendNotification(playerRef.getPacketHandler(), extraXp + "XP (+" + percent + "%)", NotificationStyle.Warning);
            });
        } catch (NoClassDefFoundError error) {
            LibraryAvailability.logMissingDependency("LevelingCore", error);
        }
        return result;
    }

    public static void applyEcotaleCashMultiplier(@Nonnull PlayerRef playerRef, long amount) {
        if (!RuntimeSettings.allowEcotaleIntegration() || !RuntimeSettings.allowCashReward()) {
            return;
        }
        if (!LibraryAvailability.isEcotalePresent()) {
            return;
        }

        UUID playerUuid = playerRef.getUuid();
        String tierId = DifficultyManager.getDifficulty(playerUuid);
        if (tierId == null) {
            return;
        }

        MultiplierResult result = getMultiplierCashAmount(tierId, amount, RuntimeSettings.ecotaleMultiplier());

        if (result.isZero()) {
            return;
        }

        long extraCash = result.extraAmount();
        long percent = result.percent();

        try {
            if (!EcotaleAPI.isPhysicalCoinsAvailable() || RuntimeSettings.allowCashRewardEvenWithPhysical()) {
                EcotaleAPI.deposit(playerUuid, extraCash, "Transfer for Difficulty (+" + percent + "%)");
            }
        } catch (NoClassDefFoundError error) {
            LibraryAvailability.logMissingDependency("Ecotale", error);
        }
    }

    public static MultiplierResult applyMMOSkillTreeXPMultiplier(@Nonnull PlayerRef playerRef, long amount, String skillName, String rawMessage) {
        if (!RuntimeSettings.allowMMOSkillTreeIntegration() || !RuntimeSettings.allowXPReward()) {
            return new MultiplierResult(amount, 0L, 0L, DEFAULT_BASE_DIFFICULTY);
        }
        if (!LibraryAvailability.isMMOSkillTreePresent()) {
            return new MultiplierResult(amount, 0L, 0L, DEFAULT_BASE_DIFFICULTY);
        }

        UUID playerUuid = playerRef.getUuid();
        String tierId = DifficultyManager.getDifficulty(playerUuid);
        if (tierId == null) {
            return new MultiplierResult(amount, 0L, 0L, DEFAULT_BASE_DIFFICULTY);
        }

        MultiplierResult result = getMultiplierXPAmount(tierId, amount, RuntimeSettings.mmoSkillTreeMultiplier());
        if (result.isZero()) {
            return new MultiplierResult(amount, 0L, 0L, tierId);
        }

        String displayName = getDisplayNameFromMultiplierResult(result);
        if (rawMessage.contains(displayName)) {
            return new MultiplierResult(amount, 0L, 0L, tierId);
        }

        if (result.isZero()) {
            return new MultiplierResult(amount, 0L, 0L, tierId);
        }

        long extraXp = result.extraAmount();

        try {
            Ref<EntityStore> storeRef = playerRef.getReference();
            if (storeRef == null) {
                return new MultiplierResult(amount, 0L, 0L, tierId);
            }
            Store<EntityStore> store = storeRef.getStore();
            SkillType skillType = SkillType.valueOf(skillName.toUpperCase().replace(' ', '_'));
            MMOSkillTreeAPI.addXp(store, storeRef, skillType, extraXp);
            //sendNotification(playerRef.getPacketHandler(), extraXp + "XP (+" + percent + "%)", NotificationStyle.Warning);
        } catch (NoClassDefFoundError error) {
            LibraryAvailability.logMissingDependency("MMOSkillTree", error);
        }
        return result;
    }

    public static String getDisplayNameFromMultiplierResult(ExperienceAndCashMultiplier.MultiplierResult multiplierResult) {
        String tierId = multiplierResult.tierId();
        String base = META_PREFIX + tierId + ".";
        return DifficultyManager.getConfig().getString(base + KEY_DISPLAY_NAME, tierId);
    }

    public record MultiplierResult(
            long originalAmount,
            long extraAmount,
            long percent,
            String tierId
    ) {
        public boolean isZero() {
            return extraAmount <= 0L;
        }
    }
}
