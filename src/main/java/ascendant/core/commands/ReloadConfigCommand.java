package ascendant.core.commands;

import ascendant.core.config.DifficultyManager;
import ascendant.core.util.WorldTierUiSync;
import com.hypixel.hytale.server.core.Message;
import com.hypixel.hytale.server.core.command.system.CommandContext;
import com.hypixel.hytale.server.core.command.system.basecommands.AbstractAsyncCommand;
import com.hypixel.hytale.server.core.permissions.HytalePermissions;

import javax.annotation.Nonnull;
import java.io.IOException;
import java.util.concurrent.CompletableFuture;

public final class ReloadConfigCommand extends AbstractAsyncCommand {

    public ReloadConfigCommand(String commandName, String commandDescription, String commandPermission) {
        super(commandName, commandDescription);
        requirePermission(HytalePermissions.fromCommand(commandPermission));
    }

    @Override
    @Nonnull
    protected CompletableFuture<Void> executeAsync(@Nonnull CommandContext commandContext) {
        return CompletableFuture.runAsync(() -> {
            try {
                DifficultyManager.reloadConfig();
                WorldTierUiSync.refreshAllPlayers();
                commandContext.sendMessage(Message.raw("Difficulty config reloaded."));
            } catch (IOException e) {
                commandContext.sendMessage(Message.raw("Failed to reload difficulty config: " + e.getMessage()));
            }
        });
    }
}
