package ascendant.core.ui;

import ascendant.core.config.DifficultyIO;
import ascendant.core.config.DifficultyManager;
import ascendant.core.config.DifficultyMeta;
import ascendant.core.config.DifficultySettings;
import ascendant.core.util.EventNotificationWrapper;
import au.ellie.hyui.builders.PageBuilder;
import au.ellie.hyui.html.TemplateProcessor;
import com.hypixel.hytale.component.Store;
import com.hypixel.hytale.protocol.packets.interface_.CustomPageLifetime;
import com.hypixel.hytale.protocol.packets.interface_.CustomUIEventBindingType;
import com.hypixel.hytale.server.core.command.system.CommandContext;
import com.hypixel.hytale.server.core.universe.PlayerRef;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;
import org.checkerframework.checker.nullness.compatqual.NonNullDecl;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

public class TierSelect {

    private static final String PAGE_HTML = "Pages/DifficultySelection.html";

    private static final int TIERS_PER_PAGE = 4;
    private static final Map<UUID, Integer> pageIndexByPlayer = new ConcurrentHashMap<>();

    public static void openOrUpdateUi(@NonNullDecl PlayerRef playerRef, @NonNullDecl Store<EntityStore> store, @NonNullDecl UUID playerUuid, @NonNullDecl CommandContext commandContext) {
        TemplateProcessor template = new TemplateProcessor();
        List<String> tierIds = visibleTierIds();
        String currentTierId = DifficultyManager.getDifficulty(playerUuid);
        int pageIndex = pageIndexByPlayer.getOrDefault(playerUuid, 0);
        int maxPage = Math.max(0, (tierIds.size() - 1) / TIERS_PER_PAGE);
        if (pageIndex > maxPage) {
            pageIndex = maxPage;
            pageIndexByPlayer.put(playerUuid, pageIndex);
        }

        List<DifficultyTier> tiersPage = buildDifficultyTiersPage(tierIds, pageIndex);
        template.setVariable("difficulty-tiers", tiersPage)
                .setVariable("currentTierId", currentTierId)
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
            template.setVariable("difficulty-tiers", buildDifficultyTiersPage(currentTierIds, currentPage));
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
            template.setVariable("difficulty-tiers", buildDifficultyTiersPage(currentTierIds, currentPage));
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

                DifficultyManager.setPlayerDifficultyOverride(playerUuid, tier.tierId());
                DifficultyBadge.updateForPlayer(playerRef);
                EventNotificationWrapper.sendMajorEventNotification(playerRef, commandContext, tier.displayName(), "selected difficulty");

                openOrUpdateUi(playerRef, store, playerUuid, commandContext);
            });
        }

        builder.open(store);
    }

    private static List<DifficultyTier> buildDifficultyTiersPage(List<String> tierIds, int pageIndex) {
        int from = Math.max(0, pageIndex * TIERS_PER_PAGE);
        int to = Math.min(tierIds.size(), from + TIERS_PER_PAGE);

        DifficultySettings settings = DifficultyManager.getSettings();
        var config = DifficultyManager.getConfig();

        List<DifficultyTier> tiers = new ArrayList<>(Math.max(0, to - from));
        for (int i = from; i < to; i++) {
            String tierId = tierIds.get(i);
            DifficultyMeta.TierMeta meta = DifficultyMeta.resolve(config, tierId);
            String resolvedTierId = meta.tierId();
            double maxHealth = settings.get(resolvedTierId, DifficultyIO.SETTING_HEALTH_MULTIPLIER);
            double baseDamage = settings.get(resolvedTierId, DifficultyIO.SETTING_DAMAGE_MULTIPLIER);
            double armor = settings.get(resolvedTierId, DifficultyIO.SETTING_ARMOR_MULTIPLIER);
            double dropRate = settings.get(resolvedTierId, DifficultyIO.SETTING_DROP_RATE_MULTIPLIER);
            double dropQuantity = settings.get(resolvedTierId, DifficultyIO.SETTING_DROP_QUANTITY_MULTIPLIER);
            double dropQuality = settings.get(resolvedTierId, DifficultyIO.SETTING_DROP_QUALITY_MULTIPLIER);
            double xp = settings.get(resolvedTierId, DifficultyIO.SETTING_XP_MULTIPLIER);
            double cash = settings.get(resolvedTierId, DifficultyIO.SETTING_CASH_MULTIPLIER);
            boolean isAllowed = settings.getBoolean(resolvedTierId, DifficultyIO.SETTING_IS_ALLOWED);
            tiers.add(new DifficultyTier(
                    resolvedTierId,
                    meta.displayName(),
                    meta.description(),
                    meta.imagePath(),
                    meta.iconPath(),
                    maxHealth,
                    baseDamage,
                    armor,
                    dropRate,
                    dropQuantity,
                    dropQuality,
                    xp,
                    cash,
                    isAllowed
            ));
        }
        return tiers;
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
            boolean isAllowed) {
    }
}
