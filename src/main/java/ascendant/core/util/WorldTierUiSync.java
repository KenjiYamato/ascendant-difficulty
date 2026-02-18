package ascendant.core.util;

import ascendant.core.adapter.ServerPlayerListAdapter;
import ascendant.core.ui.DifficultyBadge;
import com.hypixel.hytale.server.core.universe.PlayerRef;
import com.hypixel.hytale.server.core.universe.Universe;

import java.util.List;

public final class WorldTierUiSync {
    private WorldTierUiSync() {
    }

    public static void refreshAllPlayers() {
        Universe universe = Universe.get();
        if (universe == null) {
            return;
        }

        List<PlayerRef> players = universe.getPlayers();
        if (players.isEmpty()) {
            return;
        }

        for (PlayerRef playerRef : players) {
            if (playerRef == null) {
                continue;
            }
            PlayerWorldExecutor.execute(playerRef, () -> {
                ServerPlayerListAdapter.refreshPlayerEntry(playerRef);
                DifficultyBadge.updateForPlayer(playerRef);
            });
        }
    }
}
