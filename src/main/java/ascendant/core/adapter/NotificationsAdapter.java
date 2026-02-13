package ascendant.core.adapter;

import ascendant.core.config.DifficultyIO;
import ascendant.core.config.DifficultyManager;
import ascendant.core.config.RuntimeSettings;
import ascendant.core.scaling.ExperienceAndCashMultiplier;
import ascendant.core.util.FormattedMessageInspector;
import ascendant.core.util.Logging;
import com.hypixel.hytale.protocol.FormattedMessage;
import com.hypixel.hytale.protocol.LongParamValue;
import com.hypixel.hytale.protocol.MaybeBool;
import com.hypixel.hytale.protocol.Packet;
import com.hypixel.hytale.protocol.packets.interface_.Notification;
import com.hypixel.hytale.protocol.packets.interface_.NotificationStyle;
import com.hypixel.hytale.server.core.io.adapter.PacketAdapters;
import com.hypixel.hytale.server.core.io.adapter.PacketFilter;
import com.hypixel.hytale.server.core.io.adapter.PlayerPacketWatcher;
import com.hypixel.hytale.server.core.universe.PlayerRef;

import javax.annotation.Nonnull;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static ascendant.core.scaling.ExperienceAndCashMultiplier.getDisplayNameFromMultiplierResult;
import static ascendant.core.util.FormattedMessageInspector.getLongParam;

public final class NotificationsAdapter {
    private static final Pattern XP_PATTERN =
            Pattern.compile("\\+(\\d+)\\s+([A-Za-z ]+)\\s+XP");

    private static volatile PacketFilter _registered;
    private static boolean _allowDebugLogging;
    private static boolean _allowXPReward;
    private static boolean _allowLevelingCoreIntegration;
    private static boolean _allowEcotaleIntegration;
    private static boolean _allowMMOSkillTreeIntegration;


    private NotificationsAdapter() {
    }

    public static void register() {
        if (_registered != null) {
            return;
        }

        PlayerPacketWatcher watcher = NotificationsAdapter::_handleOutbound;
        _registered = PacketAdapters.registerOutbound(watcher);
        _allowDebugLogging = DifficultyManager.getFromConfig(DifficultyIO.ALLOW_DEBUG_LOGGING);
        _allowXPReward = RuntimeSettings.allowXPReward();
        _allowLevelingCoreIntegration = RuntimeSettings.allowLevelingCoreIntegration();
        _allowEcotaleIntegration = RuntimeSettings.allowEcotaleIntegration();
        _allowMMOSkillTreeIntegration = RuntimeSettings.allowMMOSkillTreeIntegration();
    }

    public static void unregister() {
        PacketFilter f = _registered;
        if (f == null) {
            return;
        }
        _registered = null;
        PacketAdapters.deregisterOutbound(f);
    }

    private static void _handleOutbound(@Nonnull PlayerRef playerRef, @Nonnull Packet packet) {
        if (!(packet instanceof Notification n)) {
            return;
        }

        if (n.style.equals(NotificationStyle.Warning)) {
            return;
        }

        debugLogNotification(n);
        if (!_allowXPReward) {
            return;
        }

        FormattedMessageInspector.FormattedMessageParts formattedMessageParts = FormattedMessageInspector.inspect(n.message);
        // levelingcore
        String messageId = formattedMessageParts.messageId();
        if ("commands.levelingcore.gained".equals(messageId)) {
            if (!_allowLevelingCoreIntegration) {
                return;
            }
            long levelingCoreXP = getLongParam(formattedMessageParts, "xp");
            if (levelingCoreXP > 0L) {
                ExperienceAndCashMultiplier.MultiplierResult multiplierResult = ExperienceAndCashMultiplier.applyLevelingCoreXPMultiplier(playerRef, levelingCoreXP);
                if (!multiplierResult.isZero()) {
                    if (n.message.params != null) {
                        n.message.params.put("xp", new LongParamValue(multiplierResult.originalAmount() + multiplierResult.extraAmount()));
                    }
                    String displayName = getDisplayNameFromMultiplierResult(multiplierResult);
                    n.secondaryMessage = new FormattedMessage("+" + multiplierResult.percent() + "% " + displayName, null, null, null, null, "#FFAA00", MaybeBool.Null, MaybeBool.Null, MaybeBool.Null, MaybeBool.Null, null, false);
                }
                if (_allowEcotaleIntegration) {
                    ExperienceAndCashMultiplier.applyEcotaleCashMultiplier(playerRef, levelingCoreXP);
                }
            }
            return;
        }
        // mmoskilltree
        if (n.message.color != null && n.message.color.equals("#00ff00")) {
            if (!_allowMMOSkillTreeIntegration) {
                return;
            }
            ParsedXp xp = parseXpRawText(n.message.rawText);
            if (xp != null) {
                long amount = xp.value();
                String skillName = xp.skill();
                ExperienceAndCashMultiplier.MultiplierResult multiplierResult = ExperienceAndCashMultiplier.applyMMOSkillTreeXPMultiplier(playerRef, amount, skillName, n.message.rawText);
                if (multiplierResult.isZero()) {
                    return;
                }
                String displayName = getDisplayNameFromMultiplierResult(multiplierResult);
                if (!n.message.rawText.contains(displayName)) {
                    n.message.rawText += " +" + multiplierResult.percent() + "% " + displayName;
                }
            }
        }

    }

    private static void debugLogNotification(Notification n) {
        if (!_allowDebugLogging) {
            return;
        }
        String primaryDebug = FormattedMessageInspector.toDebugString(n.message);
        String secondaryDebug = FormattedMessageInspector.toDebugString(n.secondaryMessage);

        Logging.info("[NOTIFICATIONS LOGGER] pri " + primaryDebug);
        Logging.info("[NOTIFICATIONS LOGGER] sec " + secondaryDebug);
    }

    private static ParsedXp parseXpRawText(String rawText) {
        if (rawText == null) {
            return null;
        }

        Matcher m = XP_PATTERN.matcher(rawText.trim());
        if (!m.matches()) {
            return null;
        }

        long value = Long.parseLong(m.group(1));
        String skill = m.group(2).trim();

        return new ParsedXp(value, skill);
    }

    public record ParsedXp(long value, String skill) {
    }

}
