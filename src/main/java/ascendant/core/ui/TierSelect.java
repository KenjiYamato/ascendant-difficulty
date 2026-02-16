package ascendant.core.ui;

import ascendant.core.config.DifficultyIO;
import ascendant.core.config.DifficultyManager;
import ascendant.core.config.DifficultyMeta;
import ascendant.core.config.DifficultySettings;
import ascendant.core.config.RuntimeSettings;
import ascendant.core.util.EventNotificationWrapper;
import au.ellie.hyui.builders.PageBuilder;
import au.ellie.hyui.html.TemplateProcessor;
import com.hypixel.hytale.component.Ref;
import com.hypixel.hytale.component.Store;
import com.hypixel.hytale.protocol.packets.interface_.CustomPageLifetime;
import com.hypixel.hytale.protocol.packets.interface_.CustomUIEventBindingType;
import com.hypixel.hytale.server.core.command.system.CommandContext;
import com.hypixel.hytale.server.core.entity.damage.DamageDataComponent;
import com.hypixel.hytale.server.core.universe.PlayerRef;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;
import org.checkerframework.checker.nullness.compatqual.NonNullDecl;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

public class TierSelect {

    private static final String PAGE_HTML = "Pages/DifficultySelection.html";

    private static final int TIERS_PER_PAGE = 4;
    private static final Map<UUID, Integer> pageIndexByPlayer = new ConcurrentHashMap<>();
    private static final Map<UUID, Long> lastDifficultyChangeByPlayer = new ConcurrentHashMap<>();
    private static final boolean _allowEliteSpawnModifier = DifficultyManager.getFromConfig(DifficultyIO.ALLOW_ELITE_SPAWN_MODIFIER);

    public static void openOrUpdateUi(@NonNullDecl PlayerRef playerRef, @NonNullDecl Store<EntityStore> store, @NonNullDecl UUID playerUuid, @NonNullDecl CommandContext commandContext) {
        TemplateProcessor template = new TemplateProcessor();
        List<String> tierIds = visibleTierIds();
        String currentTierId = DifficultyManager.getDifficulty(playerUuid);
        boolean showTierValuesAsPercent = DifficultyManager.isTierValuesAsPercent(playerUuid);
        boolean showBadge = DifficultyManager.isBadgeVisible(playerUuid);
        int pageIndex = pageIndexByPlayer.getOrDefault(playerUuid, 0);
        int maxPage = Math.max(0, (tierIds.size() - 1) / TIERS_PER_PAGE);
        if (pageIndex > maxPage) {
            pageIndex = maxPage;
            pageIndexByPlayer.put(playerUuid, pageIndex);
        }

        List<DifficultyTier> tiersPage = buildDifficultyTiersPage(tierIds, pageIndex, showTierValuesAsPercent);
        template.setVariable("difficulty-tiers", tiersPage)
                .setVariable("currentTierId", currentTierId)
                .setVariable("eliteSpawn", _allowEliteSpawnModifier)
                .setVariable("tierValuesAsPercent", showTierValuesAsPercent)
                .setVariable("badgeVisible", showBadge)
                .setVariable("pageIndex", pageIndex)
                .setVariable("pageIndexMax", maxPage);

        PageBuilder builder = PageBuilder.pageForPlayer(playerRef)
                .loadHtml(PAGE_HTML, template)
                .enableRuntimeTemplateUpdates(true)
                .withLifetime(CustomPageLifetime.CanDismiss);

        builder.addEventListener("tierPrev", CustomUIEventBindingType.Activating, (ignored, _) -> {
            List<String> currentTierIds = visibleTierIds();
            int currentPage = pageIndexByPlayer.getOrDefault(playerUuid, 0);
            if (currentPage > 0) {
                currentPage--;
            }
            pageIndexByPlayer.put(playerUuid, currentPage);
            boolean currentShowAsPercent = DifficultyManager.isTierValuesAsPercent(playerUuid);
            template.setVariable("difficulty-tiers", buildDifficultyTiersPage(currentTierIds, currentPage, currentShowAsPercent));
            template.setVariable("tierValuesAsPercent", currentShowAsPercent);
            template.setVariable("badgeVisible", DifficultyManager.isBadgeVisible(playerUuid));
            openOrUpdateUi(playerRef, store, playerUuid, commandContext);
        });

        builder.addEventListener("tierNext", CustomUIEventBindingType.Activating, (ignored, _) -> {
            List<String> currentTierIds = visibleTierIds();
            int maxPageIndex = Math.max(0, (currentTierIds.size() - 1) / TIERS_PER_PAGE);
            int currentPage = pageIndexByPlayer.getOrDefault(playerUuid, 0);
            if (currentPage < maxPageIndex) {
                currentPage++;
            }
            pageIndexByPlayer.put(playerUuid, currentPage);
            boolean currentShowAsPercent = DifficultyManager.isTierValuesAsPercent(playerUuid);
            template.setVariable("difficulty-tiers", buildDifficultyTiersPage(currentTierIds, currentPage, currentShowAsPercent));
            template.setVariable("tierValuesAsPercent", currentShowAsPercent);
            template.setVariable("badgeVisible", DifficultyManager.isBadgeVisible(playerUuid));
            openOrUpdateUi(playerRef, store, playerUuid, commandContext);
        });

        builder.addEventListener("toggleTierValuesFormat", CustomUIEventBindingType.Activating, (ignored, _) -> {
            boolean newValue = DifficultyManager.togglePlayerTierValuesAsPercent(playerUuid);
            List<String> currentTierIds = visibleTierIds();
            int currentPage = pageIndexByPlayer.getOrDefault(playerUuid, 0);
            template.setVariable("difficulty-tiers", buildDifficultyTiersPage(currentTierIds, currentPage, newValue));
            template.setVariable("tierValuesAsPercent", newValue);
            template.setVariable("badgeVisible", DifficultyManager.isBadgeVisible(playerUuid));
            openOrUpdateUi(playerRef, store, playerUuid, commandContext);
        });

        builder.addEventListener("toggleBadgeVisibility", CustomUIEventBindingType.Activating, (ignored, _) -> {
            boolean newValue = DifficultyManager.togglePlayerBadgeVisibility(playerUuid);
            DifficultyBadge.updateForPlayer(playerRef);
            List<String> currentTierIds = visibleTierIds();
            int currentPage = pageIndexByPlayer.getOrDefault(playerUuid, 0);
            boolean currentShowAsPercent = DifficultyManager.isTierValuesAsPercent(playerUuid);
            template.setVariable("difficulty-tiers", buildDifficultyTiersPage(currentTierIds, currentPage, currentShowAsPercent));
            template.setVariable("tierValuesAsPercent", currentShowAsPercent);
            template.setVariable("badgeVisible", newValue);
            openOrUpdateUi(playerRef, store, playerUuid, commandContext);
        });

        for (DifficultyTier tier : tiersPage) {
            String buttonId = "tier-button-" + tier.tierId();
            builder.addEventListener(buttonId, CustomUIEventBindingType.Activating, (ignored, _) -> {
                if (!DifficultyManager.allowDifficultyChange()) {
                    EventNotificationWrapper.sendMajorEventNotification(playerRef, commandContext, tier.displayName(), "Difficulty changes are disabled.");
                    openOrUpdateUi(playerRef, store, playerUuid, commandContext);
                    return;
                }

                if (!tier.isAllowed()) {
                    EventNotificationWrapper.sendMinorEventNotification(playerRef, commandContext, "Selected tier is disabled.");
                    openOrUpdateUi(playerRef, store, playerUuid, commandContext);
                    return;
                }

                if (!RuntimeSettings.allowDifficultyChangeInCombat() && isInCombat(playerRef, store)) {
                    EventNotificationWrapper.sendMinorEventNotification(playerRef, commandContext, "Difficulty cannot be changed while in combat.");
                    openOrUpdateUi(playerRef, store, playerUuid, commandContext);
                    return;
                }

                long remainingCooldownMs = remainingDifficultyChangeCooldownMs(playerUuid);
                if (remainingCooldownMs > 0L) {
                    long remainingSeconds = Math.max(1L, (long) Math.ceil(remainingCooldownMs / 1000.0));
                    EventNotificationWrapper.sendMinorEventNotification(
                            playerRef,
                            commandContext,
                            "Please wait " + remainingSeconds + "s before changing difficulty again."
                    );
                    openOrUpdateUi(playerRef, store, playerUuid, commandContext);
                    return;
                }

                DifficultyManager.setPlayerDifficultyOverride(playerUuid, tier.tierId());
                markDifficultyChange(playerUuid);
                DifficultyBadge.updateForPlayer(playerRef);
                EventNotificationWrapper.sendMajorEventNotification(playerRef, commandContext, tier.displayName(), "selected difficulty");

                openOrUpdateUi(playerRef, store, playerUuid, commandContext);
            });
        }

        builder.open(store);
    }

    private static List<DifficultyTier> buildDifficultyTiersPage(List<String> tierIds, int pageIndex, boolean showTierValuesAsPercent) {
        int from = Math.max(0, pageIndex * TIERS_PER_PAGE);
        int to = Math.min(tierIds.size(), from + TIERS_PER_PAGE);

        DifficultySettings settings = DifficultyManager.getSettings();
        var config = DifficultyManager.getConfig();

        List<DifficultyTier> tiers = new ArrayList<>(Math.max(0, to - from));
        for (int i = from; i < to; i++) {
            String tierId = tierIds.get(i);
            DifficultyMeta.TierMeta meta = DifficultyMeta.resolve(config, tierId);
            double maxHealth = settings.get(tierId, DifficultyIO.SETTING_HEALTH_MULTIPLIER);
            double baseDamage = settings.get(tierId, DifficultyIO.SETTING_DAMAGE_MULTIPLIER);
            double armor = settings.get(tierId, DifficultyIO.SETTING_ARMOR_MULTIPLIER);
            double dropRate = settings.get(tierId, DifficultyIO.SETTING_DROP_RATE_MULTIPLIER);
            double dropQuantity = settings.get(tierId, DifficultyIO.SETTING_DROP_QUANTITY_MULTIPLIER);
            double dropQuality = settings.get(tierId, DifficultyIO.SETTING_DROP_QUALITY_MULTIPLIER);
            double xp = settings.get(tierId, DifficultyIO.SETTING_XP_MULTIPLIER);
            double cash = settings.get(tierId, DifficultyIO.SETTING_CASH_MULTIPLIER);
            double eliteMobsChance = settings.get(tierId, DifficultyIO.SETTING_ELITE_MOBS_CHANCE_MULTIPLIER);
            double eliteMobsChanceUncommon = settings.get(tierId, DifficultyIO.SETTING_ELITE_MOBS_CHANCE_UNCOMMON);
            double eliteMobsChanceRare = settings.get(tierId, DifficultyIO.SETTING_ELITE_MOBS_CHANCE_RARE);
            double eliteMobsChanceLegendary = settings.get(tierId, DifficultyIO.SETTING_ELITE_MOBS_CHANCE_LEGENDARY);
            boolean isAllowed = settings.getBoolean(tierId, DifficultyIO.SETTING_IS_ALLOWED);
            tiers.add(new DifficultyTier(
                    tierId,
                    meta.displayName(),
                    meta.description(),
                    meta.imagePath(),
                    meta.iconPath(),
                    formatTierValue(maxHealth, showTierValuesAsPercent),
                    formatTierValue(baseDamage, showTierValuesAsPercent),
                    formatTierValue(armor, showTierValuesAsPercent),
                    formatTierValue(dropRate, showTierValuesAsPercent),
                    formatTierValue(dropQuantity, showTierValuesAsPercent),
                    formatTierValue(dropQuality, showTierValuesAsPercent),
                    formatTierValue(xp, showTierValuesAsPercent),
                    formatTierValue(cash, showTierValuesAsPercent),
                    formatTierValue(eliteMobsChance, showTierValuesAsPercent),
                    formatTierValue(eliteMobsChanceUncommon, showTierValuesAsPercent),
                    formatTierValue(eliteMobsChanceRare, showTierValuesAsPercent),
                    formatTierValue(eliteMobsChanceLegendary, showTierValuesAsPercent),
                    isAllowed
            ));
        }
        return tiers;
    }

    private static double formatTierValue(double value, boolean showAsPercent) {
        return showAsPercent ? toPercentageRoundedOneDecimal(value) : value;
    }

    public static double toPercentageRoundedOneDecimal(double _value) {
        return Math.round(_value * 1000.0) / 10.0;
    }

    private static List<String> visibleTierIds() {
        DifficultySettings settings = DifficultyManager.getSettings();
        List<String> tierIds = new ArrayList<>();
        for (String tierId : settings.tiers().keySet()) {
            if (!settings.getBoolean(tierId, DifficultyIO.SETTING_IS_HIDDEN)) {
                tierIds.add(tierId);
            }
        }
        return tierIds;
    }

    private static boolean isInCombat(@NonNullDecl PlayerRef playerRef, @NonNullDecl Store<EntityStore> store) {
        Ref<EntityStore> ref = playerRef.getReference();
        if (ref == null || !ref.isValid()) {
            return false;
        }

        DamageDataComponent damageData = store.getComponent(ref, DamageDataComponent.getComponentType());
        if (damageData == null) {
            return false;
        }

        long windowMs = (long) Math.ceil(RuntimeSettings.difficultyChangeCombatTimeoutMs());
        if (windowMs <= 0L) {
            return false;
        }

        Instant now = Instant.now();
        return isWithinCombatWindow(damageData.getLastCombatAction(), now, windowMs)
                || isWithinCombatWindow(damageData.getLastDamageTime(), now, windowMs);
    }

    private static boolean isWithinCombatWindow(Instant instant, Instant now, long windowMs) {
        if (instant == null) {
            return false;
        }
        long elapsed = safeElapsedMillis(instant, now);
        if (elapsed < 0L) {
            return true;
        }
        return elapsed <= windowMs;
    }

    private static long safeElapsedMillis(Instant from, Instant to) {
        long fromMs = safeEpochMillis(from);
        long toMs = safeEpochMillis(to);
        return safeSubtract(toMs, fromMs);
    }

    private static long safeEpochMillis(Instant instant) {
        long seconds = instant.getEpochSecond();
        int nanos = instant.getNano();

        long maxSeconds = Long.MAX_VALUE / 1000L;
        long minSeconds = Long.MIN_VALUE / 1000L;
        if (seconds > maxSeconds) {
            return Long.MAX_VALUE;
        }
        if (seconds < minSeconds) {
            return Long.MIN_VALUE;
        }

        long millis = seconds * 1000L;
        long adjustment = nanos / 1_000_000L;
        long result = millis + adjustment;
        if (((millis ^ result) & (adjustment ^ result)) < 0L) {
            return adjustment >= 0L ? Long.MAX_VALUE : Long.MIN_VALUE;
        }
        return result;
    }

    private static long safeSubtract(long a, long b) {
        long diff = a - b;
        if (((a ^ b) & (a ^ diff)) < 0L) {
            return a < 0L ? Long.MIN_VALUE : Long.MAX_VALUE;
        }
        return diff;
    }

    private static long remainingDifficultyChangeCooldownMs(@NonNullDecl UUID playerUuid) {
        long cooldownMs = (long) Math.ceil(RuntimeSettings.difficultyChangeCooldownMs());
        if (cooldownMs <= 0L) {
            return 0L;
        }
        Long last = lastDifficultyChangeByPlayer.get(playerUuid);
        if (last == null) {
            return 0L;
        }
        long elapsed = System.currentTimeMillis() - last;
        if (elapsed >= cooldownMs) {
            lastDifficultyChangeByPlayer.remove(playerUuid);
            return 0L;
        }
        if (elapsed < 0L) {
            return cooldownMs;
        }
        return cooldownMs - elapsed;
    }

    private static void markDifficultyChange(@NonNullDecl UUID playerUuid) {
        long cooldownMs = (long) Math.ceil(RuntimeSettings.difficultyChangeCooldownMs());
        if (cooldownMs <= 0L) {
            return;
        }
        lastDifficultyChangeByPlayer.put(playerUuid, System.currentTimeMillis());
    }

    public record DifficultyTier(
            String tierId,
            String displayName,
            String description,
            String imagePath,
            String iconPath,
            double maxHealth,
            double baseDamage,
            double armor,
            double dropRate,
            double dropQuantity,
            double dropQuality,
            double xp,
            double cash,
            double eliteMobsChance,
            double eliteMobsChanceUncommon,
            double eliteMobsChanceRare,
            double eliteMobsChanceLegendary,
            boolean isAllowed) {
    }
}
