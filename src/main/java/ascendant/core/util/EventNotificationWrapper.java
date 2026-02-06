package ascendant.core.util;

import com.hypixel.hytale.server.core.Message;
import com.hypixel.hytale.server.core.command.system.CommandContext;
import com.hypixel.hytale.server.core.entity.entities.Player;
import com.hypixel.hytale.server.core.universe.PlayerRef;
import com.hypixel.hytale.server.core.util.EventTitleUtil;
import org.checkerframework.checker.nullness.compatqual.NonNullDecl;

import java.util.Objects;

public final class EventNotificationWrapper {
    private EventNotificationWrapper() {
    }

    public static void sendMinorEventNotification(
            @NonNullDecl PlayerRef playerRef,
            @NonNullDecl CommandContext commandContext,
            @NonNullDecl String minorMessage
    ) {
        sendEventNotification(playerRef, commandContext, "", minorMessage, false, null);
    }

    public static void sendMajorEventNotification(
            @NonNullDecl PlayerRef playerRef,
            @NonNullDecl CommandContext commandContext,
            @NonNullDecl String majorMessage,
            @NonNullDecl String minorMessage
    ) {
        sendEventNotification(playerRef, commandContext, majorMessage, minorMessage, true, null);
    }

    public static void sendEventNotification(
            @NonNullDecl PlayerRef playerRef,
            @NonNullDecl CommandContext commandContext,
            @NonNullDecl String majorMessage,
            @NonNullDecl String minorMessage,
            boolean isMajor,
            String icon
    ) {
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
}
