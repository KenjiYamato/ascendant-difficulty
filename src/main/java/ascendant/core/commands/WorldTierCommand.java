package ascendant.core.commands;

import ascendant.core.config.DifficultyManager;
import ascendant.core.config.DifficultyMeta;
import ascendant.core.util.WorldTierUiSync;
import com.hypixel.hytale.server.core.Message;
import com.hypixel.hytale.server.core.command.system.CommandContext;
import com.hypixel.hytale.server.core.command.system.arguments.system.OptionalArg;
import com.hypixel.hytale.server.core.command.system.arguments.types.ArgTypes;
import com.hypixel.hytale.server.core.command.system.basecommands.AbstractAsyncCommand;
import com.hypixel.hytale.server.core.permissions.HytalePermissions;

import javax.annotation.Nonnull;
import java.util.Locale;
import java.util.concurrent.CompletableFuture;

public final class WorldTierCommand extends AbstractAsyncCommand {

    private final String commandName;
    private final OptionalArg<String> actionArg;
    private final OptionalArg<String> valueArg;

    public WorldTierCommand(String commandName, String commandDescription, String commandPermission) {
        super(commandName, commandDescription);
        this.commandName = commandName;
        requirePermission(HytalePermissions.fromCommand(commandPermission));
        this.actionArg = withOptionalArg("action", "status|set|clear", ArgTypes.STRING);
        this.valueArg = withOptionalArg("value", "tier id for set", ArgTypes.STRING);
    }

    @Override
    @Nonnull
    protected CompletableFuture<Void> executeAsync(@Nonnull CommandContext commandContext) {
        return CompletableFuture.runAsync(() -> handle(commandContext));
    }

    private void handle(@Nonnull CommandContext commandContext) {
        String action = readAction(commandContext, "status");
        if(action == null) {
            return;
        }
        switch (action) {
            case "status", "info", "get" -> sendStatus(commandContext);
            case "set" -> handleSet(commandContext);
            case "clear", "reset" -> handleClear(commandContext);
            default -> {
                commandContext.sendMessage(Message.raw("Unknown action: " + action));
                sendUsage(commandContext);
            }
        }
    }

    private void handleSet(@Nonnull CommandContext commandContext) {
        String tierId = readValue(commandContext, valueArg, null);
        if (tierId == null) {
            commandContext.sendMessage(Message.raw("Missing tier id. Example: /" + commandName + " set hard"));
            sendUsage(commandContext);
            return;
        }

        boolean changed = DifficultyManager.setWorldTierAdminOverride(tierId);
        if (!changed) {
            commandContext.sendMessage(Message.raw("Invalid tier id: " + tierId));
            return;
        }

        WorldTierUiSync.refreshAllPlayers();
        DifficultyManager.WorldTierSnapshot snapshot = DifficultyManager.getWorldTierSnapshot();
        DifficultyMeta.TierMeta meta = DifficultyMeta.resolve(DifficultyManager.getConfig(), snapshot.resolvedTier());
        commandContext.sendMessage(Message.raw(
                "World tier admin override set to '" + snapshot.resolvedTier() + "' (" + meta.displayName() + ")."));
        sendStatus(commandContext);
    }

    private void handleClear(@Nonnull CommandContext commandContext) {
        DifficultyManager.clearWorldTierAdminOverride();
        WorldTierUiSync.refreshAllPlayers();
        commandContext.sendMessage(Message.raw("World tier admin override cleared."));
        sendStatus(commandContext);
    }

    private void sendStatus(@Nonnull CommandContext commandContext) {
        DifficultyManager.WorldTierSnapshot snapshot = DifficultyManager.getWorldTierSnapshot();
        String adminOverride = snapshot.adminOverrideTier() == null ? "<none>" : snapshot.adminOverrideTier();
        String modeText = snapshot.mode();
        commandContext.sendMessage(Message.raw(
                String.format(
                        Locale.ROOT,
                        "WorldTier active=%s mode=%s resolved=%s fixed=%s adminOverride=%s scaledFactor=%.3f scaledUseAllOnlinePlayers=%s",
                        snapshot.active(),
                        modeText,
                        snapshot.resolvedTier(),
                        snapshot.fixedTier(),
                        adminOverride,
                        snapshot.scaledFactor(),
                        snapshot.scaledUseAllOnlinePlayers()
                )
        ));
    }

    private void sendUsage(@Nonnull CommandContext commandContext) {
        commandContext.sendMessage(Message.raw("Usage: /" + commandName + " status|set <tierId>|clear"));
    }

    private String readAction(@Nonnull CommandContext commandContext, String fallback) {
        String value = readValue(commandContext, actionArg, fallback);
        if (value == null) {
            return null;
        }
        return value.toLowerCase(Locale.ROOT);
    }

    private String readValue(
            @Nonnull CommandContext commandContext,
            @Nonnull OptionalArg<String> arg,
            String fallback
    ) {
        if (!commandContext.provided(arg)) {
            return fallback;
        }
        String value = commandContext.get(arg);
        if (value == null) {
            return fallback;
        }
        String normalized = value.trim();
        if (normalized.isEmpty()) {
            return fallback;
        }
        return normalized;
    }
}
