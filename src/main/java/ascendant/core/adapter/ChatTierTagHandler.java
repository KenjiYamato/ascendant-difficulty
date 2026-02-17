package ascendant.core.adapter;

import ascendant.core.config.DifficultyIO;
import ascendant.core.config.DifficultyManager;
import ascendant.core.util.TierTagUtil;
import com.hypixel.hytale.server.core.Message;
import com.hypixel.hytale.server.core.event.events.player.PlayerChatEvent;
import com.hypixel.hytale.server.core.universe.PlayerRef;

import javax.annotation.Nonnull;

public final class ChatTierTagHandler {
    private ChatTierTagHandler() {
    }

    public static void handle(@Nonnull PlayerChatEvent event) {
        if (!DifficultyManager.getFromConfig(DifficultyIO.ALLOW_CHAT_TIER_TAG)) {
            return;
        }
        event.setFormatter(ChatTierTagHandler::formatChatMessage);
    }

    private static Message formatChatMessage(PlayerRef sender, String content) {
        String safeContent = content == null ? "" : content;
        String username = sender != null ? sender.getUsername() : "";
        String taggedUsername = TierTagUtil.buildTaggedNameOrFallback(
                sender != null ? sender.getUuid() : null,
                username,
                TierTagUtil.PrefixKind.CHAT);

        Message message = Message.translation("server.chat.playerMessage");
        message.param("username", Message.raw(taggedUsername));
        message.param("message", safeContent);
        return message;
    }
}
