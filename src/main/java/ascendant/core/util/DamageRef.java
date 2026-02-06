package ascendant.core.util;

import ascendant.core.config.DifficultyManager;
import com.hypixel.hytale.component.ArchetypeChunk;
import com.hypixel.hytale.component.Ref;
import com.hypixel.hytale.component.Store;
import com.hypixel.hytale.server.core.entity.Entity;
import com.hypixel.hytale.server.core.entity.EntityUtils;
import com.hypixel.hytale.server.core.entity.entities.Player;
import com.hypixel.hytale.server.core.modules.entity.damage.Damage;
import com.hypixel.hytale.server.core.modules.entity.damage.DamageCause;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;

import javax.annotation.Nullable;
import java.util.UUID;

public class DamageRef {
    @Nullable
    @SuppressWarnings("removal")
    public static Entity resolveVictim(int index, ArchetypeChunk<EntityStore> chunk, Store<EntityStore> store) {
        Ref<EntityStore> victimRef = chunk.getReferenceTo(index);
        if (!victimRef.isValid()) {
            return null;
        }

        Entity victim = EntityUtils.getEntity(victimRef, store);
        return victim;
    }

    @Nullable
    @SuppressWarnings("removal")
    public static UUID resolveVictimUUID(int index, ArchetypeChunk<EntityStore> chunk, Store<EntityStore> store) {
        Entity victim = resolveVictim(index, chunk, store);
        if (!(victim instanceof Player)) {
            return null;
        }

        return victim.getUuid();
    }

    @Nullable
    public static Player resolveAttacker(Damage damage, Store<EntityStore> store) {
        if (!(damage.getSource() instanceof Damage.EntitySource src)) {
            return null;
        }

        Ref<EntityStore> ref = src.getRef();
        if (!ref.isValid()) {
            return null;
        }

        Entity attacker = EntityUtils.getEntity(ref, store);
        if (!(attacker instanceof Player player)) {
            return null;
        }

        return player;
    }

    @Nullable
    @SuppressWarnings("removal")
    public static UUID resolveAttackerPlayerUuid(Damage damage, Store<EntityStore> store) {
        Player player = resolveAttacker(damage, store);
        if (player == null) {
            return null;
        }


        return player.getUuid();
    }

    public static double resolveTierConfigKeyForUUID(UUID entityUuid, String configKey) {
        String tierId = DifficultyManager.getDifficulty(entityUuid);
        if (tierId == null) {
            return -1.0;
        }

        return DifficultyManager.getSettings().get(tierId, configKey);
    }

    public static boolean checkInvalidDamage(Damage damage) {
        if (damage.getAmount() <= 0.0f) {
            return false;
        }

        DamageCause cause = damage.getCause();
        return cause != null;
    }
}
