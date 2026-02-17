package ascendant.core.commands;

import ascendant.core.adapter.ServerPlayerListAdapter;
import ascendant.core.config.DifficultyManager;
import ascendant.core.util.PlayerWorldExecutor;
import com.hypixel.hytale.server.core.Message;
import com.hypixel.hytale.server.core.command.system.CommandContext;
import com.hypixel.hytale.server.core.command.system.basecommands.AbstractAsyncCommand;
import com.hypixel.hytale.server.core.permissions.HytalePermissions;
import com.hypixel.hytale.server.core.universe.PlayerRef;
import com.hypixel.hytale.server.core.universe.Universe;

import javax.annotation.Nonnull;
import java.io.IOException;
import java.util.List;
import java.util.concurrent.CompletableFuture;

public final class ReloadConfigCommand extends AbstractAsyncCommand {

    public ReloadConfigCommand(String commandName, String commandDescription, String commandPermission) {
        super(commandName, commandDescription);
        requirePermission(HytalePermissions.fromCommand(commandPermission));
    }

    private static void refreshServerList() {
        Universe universe = Universe.get();
        if (universe == null) {
            return;
        }
        List<PlayerRef> players = universe.getPlayers();
        if (players.isEmpty()) {
            return;
        }
        for (PlayerRef playerRef : players) {
            PlayerWorldExecutor.execute(playerRef, () -> ServerPlayerListAdapter.refreshPlayerEntry(playerRef));
        }
    }

    @Override
    @Nonnull
    protected CompletableFuture<Void> executeAsync(@Nonnull CommandContext commandContext) {
        return CompletableFuture.runAsync(() -> {
            try {
                DifficultyManager.reloadConfig();
                refreshServerList();
                commandContext.sendMessage(Message.raw("Difficulty config reloaded."));
            } catch (IOException e) {
                commandContext.sendMessage(Message.raw("Failed to reload difficulty config: " + e.getMessage()));
            }
        });
    }
}
