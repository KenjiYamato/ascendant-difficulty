package ascendant.core.adapter;

import ascendant.core.config.ConfigKey;
import ascendant.core.config.DifficultyIO;
import ascendant.core.config.DifficultyManager;
import ascendant.core.util.DamageRef;
import ascendant.core.util.Logging;
import ascendant.core.util.TierTagUtil;
import com.hypixel.hytale.component.ArchetypeChunk;
import com.hypixel.hytale.component.CommandBuffer;
import com.hypixel.hytale.component.Ref;
import com.hypixel.hytale.component.Store;
import com.hypixel.hytale.component.query.Query;
import com.hypixel.hytale.component.system.EntityEventSystem;
import com.hypixel.hytale.protocol.FormattedMessage;
import com.hypixel.hytale.server.core.Message;
import com.hypixel.hytale.server.core.entity.Entity;
import com.hypixel.hytale.server.core.entity.EntityUtils;
import com.hypixel.hytale.server.core.entity.entities.Player;
import com.hypixel.hytale.server.core.modules.entity.damage.Damage;
import com.hypixel.hytale.server.core.modules.entity.damage.DamageCause;
import com.hypixel.hytale.server.core.modules.entity.damage.event.KillFeedEvent;
import com.hypixel.hytale.server.core.universe.PlayerRef;
import com.hypixel.hytale.server.core.universe.Universe;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;

import javax.annotation.Nonnull;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ThreadLocalRandom;

public final class KillFeedTierTagHandler {
    private static final String KILLED_BY_KEY = "server.general.killedBy";

    private KillFeedTierTagHandler() {
    }

    public static final class KillerSystem extends EntityEventSystem<EntityStore, KillFeedEvent.KillerMessage> {
        public KillerSystem() {
            super(KillFeedEvent.KillerMessage.class);
        }

        @Override
        public void handle(int index,
                           ArchetypeChunk<EntityStore> chunk,
                           Store<EntityStore> store,
                           CommandBuffer<EntityStore> commandBuffer,
                           KillFeedEvent.KillerMessage event) {
            if (store == null || event == null) {
                return;
            }
            handleKiller(store, chunk.getReferenceTo(index), event);
        }

        @Override
        public Query<EntityStore> getQuery() {
            return Query.any();
        }
    }

    public static final class DecedentSystem extends EntityEventSystem<EntityStore, KillFeedEvent.DecedentMessage> {
        public DecedentSystem() {
            super(KillFeedEvent.DecedentMessage.class);
        }

        @Override
        public void handle(int index,
                           ArchetypeChunk<EntityStore> chunk,
                           Store<EntityStore> store,
                           CommandBuffer<EntityStore> commandBuffer,
                           KillFeedEvent.DecedentMessage event) {
            if (store == null || event == null) {
                return;
            }
            handleDecedent(store, chunk.getReferenceTo(index), event);
        }

        @Override
        public Query<EntityStore> getQuery() {
            return Query.any();
        }
    }

    public static final class DisplaySystem extends EntityEventSystem<EntityStore, KillFeedEvent.Display> {
        public DisplaySystem() {
            super(KillFeedEvent.Display.class);
        }

        @Override
        public void handle(int index,
                           ArchetypeChunk<EntityStore> chunk,
                           Store<EntityStore> store,
                           CommandBuffer<EntityStore> commandBuffer,
                           KillFeedEvent.Display event) {
            if (store == null || event == null) {
                return;
            }
            handleDisplay(store, commandBuffer, chunk.getReferenceTo(index), event);
        }

        @Override
        public Query<EntityStore> getQuery() {
            return Query.any();
        }
    }

    private static void handleKiller(@Nonnull Store<EntityStore> store,
                                     @Nonnull Ref<EntityStore> victimRef,
                                     @Nonnull KillFeedEvent.KillerMessage event) {
        if (!DifficultyManager.getFromConfig(DifficultyIO.ALLOW_KILLFEED_TIER_TAG)) {
            return;
        }

        Damage damage = event.getDamage();
        if (damage == null || victimRef == null) {
            return;
        }

        Player killer = DamageRef.resolveAttacker(damage, store);
        if (killer == null) {
            return;
        }

        Message tagged = buildTaggedNameMessage(killer);
        if (tagged != null) {
            event.setMessage(tagged);
        }
    }

    private static void handleDecedent(@Nonnull Store<EntityStore> store,
                                       @Nonnull Ref<EntityStore> victimRef,
                                       @Nonnull KillFeedEvent.DecedentMessage event) {
        if (!DifficultyManager.getFromConfig(DifficultyIO.ALLOW_KILLFEED_TIER_TAG)) {
            return;
        }

        Damage damage = event.getDamage();
        if (damage == null) {
            return;
        }

        if (victimRef == null || !victimRef.isValid()) {
            return;
        }

        @SuppressWarnings("removal")
        Entity entity = EntityUtils.getEntity(victimRef, store);
        if (!(entity instanceof Player player)) {
            return;
        }

        Message tagged = buildTaggedNameMessage(player);
        if (tagged != null) {
            event.setMessage(tagged);
        }
    }

    private static void handleDisplay(@Nonnull Store<EntityStore> store,
                                      @Nonnull CommandBuffer<EntityStore> commandBuffer,
                                      @Nonnull Ref<EntityStore> victimRef,
                                      @Nonnull KillFeedEvent.Display event) {
        if (!DifficultyManager.getFromConfig(DifficultyIO.ALLOW_KILLFEED_TIER_CHAT)) {
            return;
        }

        Damage damage = event.getDamage();
        List<PlayerRef> targets = event.getBroadcastTargets();
        if (targets.isEmpty() || !victimRef.isValid()) {
            return;
        }

        Player killer = damage != null ? DamageRef.resolveAttacker(damage, store) : null;
        @SuppressWarnings("removal")
        Entity victimEntity = EntityUtils.getEntity(victimRef, store);
        if (!(victimEntity instanceof Player victim)) {
            return;
        }

        String victimTag = buildTaggedName(victim);
        if (victimTag == null) {
            return;
        }

        String playerColor = getColor(DifficultyIO.KILLFEED_CHAT_COLOR_PLAYER, DifficultyIO.DEFAULT_KILLFEED_CHAT_COLOR_PLAYER);
        String middleColor = getColor(DifficultyIO.KILLFEED_CHAT_COLOR_MIDDLE, DifficultyIO.DEFAULT_KILLFEED_CHAT_COLOR_MIDDLE);
        String causeColor = getColor(DifficultyIO.KILLFEED_CHAT_COLOR_CAUSE, DifficultyIO.DEFAULT_KILLFEED_CHAT_COLOR_CAUSE);

        DamageCause damageCause = damage != null ? damage.getCause() : null;
        String causeId = resolveCauseId(damage);

        Message victimMessage = applyColor(Message.raw(victimTag), playerColor);
        Message chat = damageCause == null
                ? buildFallbackChat(victimMessage, middleColor, causeId)
                : buildChatForCause(damageCause, store, victimRef, damage, killer, victimMessage, playerColor, middleColor, causeColor);
        if (chat == null) {
            chat = buildFallbackChat(victimMessage, middleColor, causeId);
        }
        if (chat == null) {
            return;
        }

        if (!sendToUniverse(chat)) {
            for (PlayerRef playerRef : targets) {
                if (playerRef != null) {
                    playerRef.sendMessage(chat);
                }
            }
        }
    }

    private static Message buildChatForCause(@Nonnull DamageCause damageCause,
                                             @Nonnull Store<EntityStore> store,
                                             @Nonnull Ref<EntityStore> victimRef,
                                             @Nonnull Damage damage,
                                             Player killer,
                                             @Nonnull Message victimMessage,
                                             @Nonnull String playerColor,
                                             @Nonnull String middleColor,
                                             @Nonnull String causeColor) {
        if (damageCause == DamageCause.PHYSICAL || damageCause == DamageCause.PROJECTILE) {
            return buildPhysicalChat(store, victimRef, damage, killer, victimMessage, playerColor, middleColor, causeColor);
        }
        if (damageCause == DamageCause.COMMAND) {
            return buildRandomVictimChat(victimMessage, middleColor, DifficultyIO.KILLFEED_CHAT_MESSAGES_COMMAND);
        }
        if (damageCause == DamageCause.DROWNING) {
            return buildRandomVictimChat(victimMessage, middleColor, DifficultyIO.KILLFEED_CHAT_MESSAGES_DROWNING);
        }
        if (damageCause == DamageCause.SUFFOCATION) {
            return buildRandomVictimChat(victimMessage, middleColor, DifficultyIO.KILLFEED_CHAT_MESSAGES_SUFFOCATION);
        }
        if (damageCause == DamageCause.ENVIRONMENT) {
            return buildRandomVictimChat(victimMessage, middleColor, DifficultyIO.KILLFEED_CHAT_MESSAGES_ENVIRONMENT);
        }
        if (damageCause == DamageCause.FALL) {
            return buildRandomVictimChat(victimMessage, middleColor, DifficultyIO.KILLFEED_CHAT_MESSAGES_FALL);
        }
        if (damageCause == DamageCause.OUT_OF_WORLD) {
            return buildRandomVictimChat(victimMessage, middleColor, DifficultyIO.KILLFEED_CHAT_MESSAGES_OUT_OF_WORLD);
        }
        return null;
    }

    private static Message buildFallbackChat(@Nonnull Message victimMessage,
                                             @Nonnull String middleColor, String causeId) {
        Logging.debug("[FALLBACK] " + causeId);
        String causeMessage = pickRandomCauseMessage(causeId);
        if (causeMessage != null) {
            Message chat = Message.empty();
            chat.insertAll(victimMessage, applyColor(Message.raw(causeMessage), middleColor));
            return chat;
        }
        return buildRandomVictimChat(victimMessage, middleColor, DifficultyIO.KILLFEED_CHAT_MESSAGES_FALLBACK);
    }

    private static String resolveCauseId(Damage damage) {
        if (damage == null) {
            return "unknown";
        }
        try {
            Object asset = DamageCause.getAssetMap().getAsset(damage.getDamageCauseIndex());
            if (asset instanceof DamageCause cause) {
                String id = cause.getId();
                if (id != null && !id.isBlank()) {
                    return id.toLowerCase(Locale.ROOT);
                }
            }
        } catch (RuntimeException ignored) {
        }
        return "unknown";
    }

    private static String pickRandomCauseMessage(String causeId) {
        if (causeId == null || causeId.isBlank()) {
            return null;
        }
        String path = DifficultyIO.PATH_KILLFEED_CHAT_MESSAGES_BY_CAUSE_ID + "." + causeId.toLowerCase(Locale.ROOT);
        List<String> messages = DifficultyManager.getConfig().getStringList(path, List.of());
        return pickRandomMessage(messages);
    }

    private static Message buildRandomVictimChat(@Nonnull Message victimMessage,
                                                 @Nonnull String middleColor,
                                                 @Nonnull ConfigKey<List<String>> messageKey) {
        String chosen = pickRandomMessage(messageKey);
        if (chosen == null) {
            return null;
        }
        Message chat = Message.empty();
        chat.insertAll(victimMessage, applyColor(Message.raw(chosen), middleColor));
        return chat;
    }

    private static Message buildPhysicalChat(@Nonnull Store<EntityStore> store,
                                             @Nonnull Ref<EntityStore> victimRef,
                                             @Nonnull Damage damage,
                                             Player killer,
                                             @Nonnull Message victimMessage,
                                             @Nonnull String playerColor,
                                             @Nonnull String middleColor,
                                             @Nonnull String causeColor) {
        Message causeMessage = buildDeathCauseMessage(store, victimRef, damage);
        if (causeMessage != null) {
            String causeMessageAnsi = causeMessage.getAnsiMessage();
            if (causeMessageAnsi == null || causeMessageAnsi.isBlank()) {
                causeMessage = null;
            } else {
                Logging.debug(causeMessageAnsi);
                causeMessageAnsi = causeMessageAnsi.replace("You were killed by ", "");
                causeMessage = applyColor(Message.raw(causeMessageAnsi), causeColor);
            }
        }

        String killerTag = killer != null ? buildTaggedName(killer) : null;
        if (killerTag != null) {
            String killerAction = pickRandomMessage(DifficultyIO.KILLFEED_CHAT_MESSAGES_PHYSICAL_KILLER_ACTION);
            if (killerAction == null) {
                return null;
            }
            Message killerMessage = applyColor(Message.raw(killerTag), playerColor);
            Message chat = Message.empty();
            chat.insertAll(killerMessage, applyColor(Message.raw(killerAction), middleColor), victimMessage);
            if (causeMessage != null) {
                String killerCause = pickRandomMessage(DifficultyIO.KILLFEED_CHAT_MESSAGES_PHYSICAL_KILLER_CAUSE);
                if (killerCause != null) {
                    chat.insertAll(applyColor(Message.raw(killerCause), middleColor), causeMessage);
                }
            }
            return chat;
        }

        if (causeMessage != null) {
            String victimCause = pickRandomMessage(DifficultyIO.KILLFEED_CHAT_MESSAGES_PHYSICAL_VICTIM_CAUSE);
            if (victimCause == null) {
                return null;
            }
            Message chat = Message.empty();
            chat.insertAll(victimMessage, applyColor(Message.raw(victimCause), middleColor), causeMessage);
            return chat;
        }

        String victimDied = pickRandomMessage(DifficultyIO.KILLFEED_CHAT_MESSAGES_PHYSICAL_VICTIM_DIED);
        if (victimDied == null) {
            return null;
        }
        Message chat = Message.empty();
        chat.insertAll(victimMessage, applyColor(Message.raw(victimDied), middleColor));
        return chat;
    }

    private static String pickRandomMessage(@Nonnull ConfigKey<List<String>> key) {
        List<String> messages = DifficultyManager.getFromConfig(key);
        return pickRandomMessage(messages);
    }

    private static String pickRandomMessage(List<String> messages) {
        if (messages == null || messages.isEmpty()) {
            return null;
        }
        int idx = ThreadLocalRandom.current().nextInt(messages.size());
        return messages.get(idx);
    }

    private static Message buildDeathCauseMessage(@Nonnull Store<EntityStore> store,
                                                  @Nonnull Ref<EntityStore> targetRef,
                                                  @Nonnull Damage damage) {
        if (!targetRef.isValid()) {
            return null;
        }
        Message message = damage.getDeathMessage(targetRef, store);
        message = stripKilledByPrefix(message);
        return message;
    }

    private static Message stripKilledByPrefix(@Nonnull Message message) {
        if (isKilledByMessage(message)) {
            return extractKilledByParam(message);
        }

        List<Message> children = message.getChildren();
        if (children.isEmpty()) {
            return message;
        }

        boolean removed = false;
        Message rebuilt = null;
        for (Message child : children) {
            if (child == null) {
                continue;
            }
            if (isKilledByMessage(child)) {
                removed = true;
                continue;
            }
            if (rebuilt == null) {
                rebuilt = Message.empty();
            }
            rebuilt.insert(child);
        }

        if (removed) {
            return rebuilt;
        }
        return message;
    }

    private static boolean isKilledByMessage(@Nonnull Message message) {
        String messageId = message.getMessageId();
        if (messageId != null && messageId.equals(KILLED_BY_KEY)) {
            return true;
        }
        String rawText = message.getRawText();
        return rawText != null && rawText.equals(KILLED_BY_KEY);
    }

    private static Message extractKilledByParam(@Nonnull Message message) {
        FormattedMessage formatted = message.getFormattedMessage();
        if (formatted == null || formatted.messageParams == null || formatted.messageParams.isEmpty()) {
            return null;
        }

        FormattedMessage param = formatted.messageParams.get("cause");
        if (param == null) {
            param = formatted.messageParams.get("deathCause");
        }
        if (param == null) {
            for (Map.Entry<String, FormattedMessage> entry : formatted.messageParams.entrySet()) {
                if (entry.getValue() != null) {
                    param = entry.getValue();
                    break;
                }
            }
        }

        return param != null ? new Message(new FormattedMessage(param)) : null;
    }

    private static String getColor(@Nonnull ConfigKey<String> key, @Nonnull String fallback) {
        String color = DifficultyManager.getFromConfig(key);
        if (color == null || color.isBlank()) {
            return fallback;
        }
        return color;
    }

    private static Message applyColor(@Nonnull Message message, String color) {
        if (color != null && !color.isBlank()) {
            message.color(color);
        }
        return message;
    }

    private static Message buildTaggedNameMessage(@Nonnull Player player) {
        String taggedName = buildTaggedName(player);
        if (taggedName == null) {
            return null;
        }
        return Message.raw(taggedName);
    }

    @SuppressWarnings("removal")
    private static String buildTaggedName(@Nonnull Player player) {
        String playerName = player.getDisplayName();
        return TierTagUtil.buildTaggedName(player.getUuid(), playerName, TierTagUtil.PrefixKind.KILLFEED);
    }

    private static boolean sendToUniverse(Message chat) {
        if (chat == null) {
            return false;
        }
        Universe universe = Universe.get();
        if (universe == null) {
            return false;
        }
        universe.sendMessage(chat);
        return true;
    }
}
