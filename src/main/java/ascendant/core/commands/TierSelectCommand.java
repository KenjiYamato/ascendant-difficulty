package ascendant.core.commands;

import ascendant.core.config.DifficultyConfig;
import ascendant.core.config.DifficultyManager;
import ascendant.core.config.DifficultySettings;
import ascendant.core.ui.DifficultyBadge;
import au.ellie.hyui.builders.PageBuilder;
import au.ellie.hyui.html.TemplateProcessor;
import com.hypixel.hytale.component.Store;
import com.hypixel.hytale.protocol.packets.interface_.CustomPageLifetime;
import com.hypixel.hytale.protocol.packets.interface_.CustomUIEventBindingType;
import com.hypixel.hytale.server.core.Message;
import com.hypixel.hytale.server.core.command.system.CommandContext;
import com.hypixel.hytale.server.core.entity.entities.Player;
import com.hypixel.hytale.server.core.universe.PlayerRef;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;
import com.hypixel.hytale.server.core.util.EventTitleUtil;
import org.checkerframework.checker.nullness.compatqual.NonNullDecl;

import java.io.IOException;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

public final class TierSelectCommand extends AbstractPlayerUICommand {
    private static final int TIERS_PER_PAGE = 4;

    private final DifficultyConfig difficultyConfig;
    private volatile DifficultySettings difficultySettings;
    private final Map<UUID, Integer> pageIndexByPlayer = new ConcurrentHashMap<>();

    public TierSelectCommand(String commandName, String commandDescription, String commandPermission) {
        super(commandName, commandDescription, commandPermission);
        this.difficultyConfig = loadOrCreateConfig();
        this.difficultySettings = DifficultySettings.fromConfig(this.difficultyConfig);
    }

    private static DifficultyConfig loadOrCreateConfig() {
        try {
            return ascendant.core.config.DifficultyIO.loadOrCreateConfig();
        } catch (IOException e) {
            throw new RuntimeException("Failed to loadOrCreate difficulty config at " + DifficultyConfig.DEFAULT_PATH + ": " + e.getMessage(), e);
        }
    }

    private static final String META_PREFIX = "meta.";
    private static final String META_DISPLAY_NAME = "displayName";
    private static final String META_DESCRIPTION = "description";
    private static final String META_IMAGE_PATH = "imagePath";
    private static final String META_ICON_PATH = "iconPath";
    private static final String DEFAULT_IMAGE_PATH = "Images/Difficulty/normal.png";
    private static final String DEFAULT_ICON_PATH = "Images/Difficulty/normal@icon.png";


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
            boolean isAllowed) {}

    @Override
    protected void openOrRefreshPage(@NonNullDecl PlayerRef playerRef, @NonNullDecl Store<EntityStore> store, @NonNullDecl UUID playerUuid, @NonNullDecl CommandContext commandContext) {
        TemplateProcessor template = new TemplateProcessor();
        List<String> tierIds = visibleTierIds();
        String currentTierId = ascendant.core.config.DifficultyManager.getDifficulty(playerUuid);
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
                .loadHtml("Pages/DifficultySelection.html", template)
                .enableRuntimeTemplateUpdates(true)
                .withLifetime(CustomPageLifetime.CanDismiss);

        builder.addEventListener("tierPrev", CustomUIEventBindingType.Activating, (ignored, ctx) -> {
            List<String> currentTierIds = visibleTierIds();
            int currentPage = pageIndexByPlayer.getOrDefault(playerUuid, 0);
            if (currentPage > 0) {
                currentPage--;
            }
            pageIndexByPlayer.put(playerUuid, currentPage);
            template.setVariable("difficulty-tiers", buildDifficultyTiersPage(currentTierIds, currentPage));
            openOrRefreshPage(playerRef, store, playerUuid, commandContext);
        });

        builder.addEventListener("tierNext", CustomUIEventBindingType.Activating, (ignored, ctx) -> {
            List<String> currentTierIds = visibleTierIds();
            int maxPageIndex = Math.max(0, (currentTierIds.size() - 1) / TIERS_PER_PAGE);
            int currentPage = pageIndexByPlayer.getOrDefault(playerUuid, 0);
            if (currentPage < maxPageIndex) {
                currentPage++;
            }
            pageIndexByPlayer.put(playerUuid, currentPage);
            template.setVariable("difficulty-tiers", buildDifficultyTiersPage(currentTierIds, currentPage));
            openOrRefreshPage(playerRef, store, playerUuid, commandContext);
        });

        for (DifficultyTier tier : tiersPage) {
            String buttonId = "tier-button-" + tier.tierId();
            builder.addEventListener(buttonId, CustomUIEventBindingType.Activating, (ignored, ctx) -> {
                if (!DifficultyManager.canPlayerSelect()) {
                    sendMajorEventNotification(playerRef,commandContext, tier.displayName(), "Difficulty changes are disabled.");
                    openOrRefreshPage(playerRef, store, playerUuid, commandContext);
                    return;
                }

                if (!tier.isAllowed()) {
                    sendMinorEventNotification(playerRef,commandContext, "Selected tier is disabled.");
                    openOrRefreshPage(playerRef, store, playerUuid, commandContext);
                    return;
                }

                ascendant.core.config.DifficultyManager.setPlayerDifficultyOverride(playerUuid, tier.tierId());
                sendMajorEventNotification(playerRef,commandContext, tier.displayName(), "selected difficulty");
                DifficultyBadge.updateForPlayer(playerRef);

                openOrRefreshPage(playerRef, store, playerUuid, commandContext);
            });
        }

        builder.open(store);
    }

    private void sendMinorEventNotification(@NonNullDecl PlayerRef playerRef, @NonNullDecl CommandContext commandContext, @NonNullDecl String minorMessage) {
        sendEventNotification(playerRef,commandContext, "", minorMessage, false, null);
    }

    private void sendMajorEventNotification(@NonNullDecl PlayerRef playerRef, @NonNullDecl CommandContext commandContext, @NonNullDecl String majorMessage, @NonNullDecl String minorMessage) {
        sendEventNotification(playerRef,commandContext, majorMessage, minorMessage, true, null);
    }

    private void sendEventNotification(@NonNullDecl PlayerRef playerRef, @NonNullDecl CommandContext commandContext, @NonNullDecl String majorMessage, @NonNullDecl String minorMessage, boolean isMajor, String icon) {
        Objects.requireNonNull(commandContext.senderAs(Player.class).getWorld()).execute(() -> {
            EventTitleUtil.showEventTitleToPlayer(
                    playerRef,
                    Message.raw(majorMessage),
                    Message.raw(minorMessage),
                    isMajor,
                    icon,
                    0.8F,
                    0.3F,
                    0.3F
            );
        });
    }

    private List<DifficultyTier> buildDifficultyTiersPage(List<String> tierIds, int pageIndex) {
        int from = Math.max(0, pageIndex * TIERS_PER_PAGE);
        int to = Math.min(tierIds.size(), from + TIERS_PER_PAGE);

        List<DifficultyTier> tiers = new ArrayList<>(Math.max(0, to - from));
        for (int i = from; i < to; i++) {
            String tierId = tierIds.get(i);
            String metaBase = META_PREFIX + tierId + ".";
            String displayName = difficultyConfig.getString(metaBase + META_DISPLAY_NAME, tierId);
            String description = difficultyConfig.getString(metaBase + META_DESCRIPTION, "");
            String imagePath = difficultyConfig.getString(metaBase + META_IMAGE_PATH, DEFAULT_IMAGE_PATH);
            String iconPath = difficultyConfig.getString(metaBase + META_ICON_PATH, DEFAULT_ICON_PATH);
            double maxHealth = difficultySettings.get(tierId, "health_multiplier");
            double baseDamage = difficultySettings.get(tierId, "damage_multiplier");
            double armor = difficultySettings.get(tierId, "armor_multiplier");
            double dropRate = difficultySettings.get(tierId, "drop_rate_multiplier");
            double dropQuantity = difficultySettings.get(tierId, "drop_quantity_multiplier");
            double dropQuality = difficultySettings.get(tierId, "drop_quality_multiplier");
            double xp = difficultySettings.get(tierId, "xp_multiplier");
            double cash = difficultySettings.get(tierId, "cash_multiplier");
            boolean isAllowed = difficultySettings.getBoolean(tierId, "is_allowed");
            tiers.add(new DifficultyTier(
                    tierId,
                    displayName,
                    description,
                    imagePath,
                    iconPath,
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

    private List<String> visibleTierIds() {
        List<String> tierIds = new ArrayList<>();
        for (String tierId : difficultySettings.tiers().keySet()) {
            if (!difficultySettings.getBoolean(tierId, "is_hidden")) {
                tierIds.add(tierId);
            }
        }
        return tierIds;
    }
}
