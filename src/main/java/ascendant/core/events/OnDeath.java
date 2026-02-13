package ascendant.core.events;

import ascendant.core.config.DifficultyIO;
import ascendant.core.config.DifficultyManager;
import ascendant.core.config.RuntimeSettings;
import ascendant.core.scaling.ExperienceAndCashMultiplier;
import ascendant.core.util.DamageRef;
import ascendant.core.util.LibraryAvailability;
import ascendant.core.util.Logging;
import ascendant.core.util.NpcRoles;
import com.azuredoom.levelingcore.api.LevelingCoreApi;
import com.azuredoom.levelingcore.level.LevelServiceImpl;
import com.azuredoom.levelingcore.ui.hud.XPBarHud;
import com.hypixel.hytale.component.CommandBuffer;
import com.hypixel.hytale.component.ComponentType;
import com.hypixel.hytale.component.Ref;
import com.hypixel.hytale.component.Store;
import com.hypixel.hytale.component.query.Query;
import com.hypixel.hytale.protocol.packets.interface_.NotificationStyle;
import com.hypixel.hytale.server.core.Message;
import com.hypixel.hytale.server.core.asset.type.attitude.Attitude;
import com.hypixel.hytale.server.core.entity.entities.Player;
import com.hypixel.hytale.server.core.modules.entity.damage.Damage;
import com.hypixel.hytale.server.core.modules.entity.damage.DeathComponent;
import com.hypixel.hytale.server.core.modules.entity.damage.DeathSystems;
import com.hypixel.hytale.server.core.modules.entitystats.EntityStatMap;
import com.hypixel.hytale.server.core.modules.entitystats.EntityStatValue;
import com.hypixel.hytale.server.core.modules.entitystats.asset.DefaultEntityStatTypes;
import com.hypixel.hytale.server.core.universe.PlayerRef;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;
import com.hypixel.hytale.server.core.util.NotificationUtil;
import com.hypixel.hytale.server.npc.entities.NPCEntity;
import com.hypixel.hytale.server.npc.role.Role;
import com.hypixel.hytale.server.npc.role.support.RoleStats;
import com.hypixel.hytale.server.npc.role.support.WorldSupport;
import org.checkerframework.checker.nullness.qual.NonNull;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.util.UUID;

public class OnDeath extends DeathSystems.OnDeathSystem {

    private final float _minDamageFactor;

    public OnDeath() {
        double minDamageFactor = DifficultyManager.getFromConfig(DifficultyIO.MIN_DAMAGE_FACTOR);
        _minDamageFactor = (float) minDamageFactor;
    }

    @Nonnull
    private static final Query<EntityStore> QUERY =
            Query.and(new Query[]{DeathComponent.getComponentType(), Query.not(Player.getComponentType())});

    @Nonnull
    @Override
    public Query<EntityStore> getQuery() {
        return QUERY;
    }

    @SuppressWarnings("removal")
    @Override
    public void onComponentAdded(
            @NonNull Ref<EntityStore> ref,
            @NonNull DeathComponent deathComponent,
            @NonNull Store<EntityStore> store,
            @NonNull CommandBuffer<EntityStore> commandBuffer
    ) {
        if (!RuntimeSettings.allowCustomLeveling()
                || !LibraryAvailability.isLevelingCorePresent()
                || !RuntimeSettings.allowLevelingCoreIntegration()) {
            return;
        }
        Damage deathInfoDamage = deathComponent.getDeathInfo();
        if (deathInfoDamage == null) {
            return;
        }

        Damage.Source source = deathInfoDamage.getSource();
        if (!(source instanceof Damage.EntitySource entitySource)) {
            return;
        }

        EntityStatMap statMap = store.getComponent(ref, EntityStatMap.getComponentType());
        if (statMap == null) {
            return;
        }

        ComponentType<EntityStore, NPCEntity> npcType = NPCEntity.getComponentType();
        if (npcType == null) {
            return;
        }

        NPCEntity entity = store.getComponent(ref, npcType);
        if (entity == null) {
            return;
        }

        Ref<EntityStore> sourceRef = entitySource.getRef();
        if (!sourceRef.isValid()) {
            return;
        }

        Player attacker = store.getComponent(sourceRef, Player.getComponentType());
        if (attacker == null) {
            return;
        }

        UUID attackerUuid = attacker.getUuid();
        if (attackerUuid == null) {
            return;
        }

        float baseFactor = 0.0f;

        RoleAndRangeResult roleAndRangeResult = _getMaxRange(entity);
        if (RuntimeSettings.customLevelingIncludeRange()) {
            baseFactor += roleAndRangeResult.range;
        }

        if (RuntimeSettings.customLevelingIncludeDefaultStats()) {
            baseFactor += (float) _defaultEntityStats(statMap);
        }

        float baseDamage = 0.0f;
        float scaledDamage = 0.0f;
        if (RuntimeSettings.customLevelingIncludeScaledDamage()) {
            baseDamage = _getCalculatedBaseDamage(entity);
            scaledDamage = baseDamage;

            double damageMultiplierCfg = DamageRef.resolveTierConfigKeyForUUID(attackerUuid, DifficultyIO.SETTING_DAMAGE_MULTIPLIER);
            if (damageMultiplierCfg > 0) {
                scaledDamage = applyDamageMultiplier(baseDamage, (float) Math.max(0.0, damageMultiplierCfg));
            }

            float factor = (float) RuntimeSettings.customLevelingScaledDamageFactor();
            baseFactor += scaledDamage * factor;
            Logging.debug("[ONDEATH] Entity " + entity.getRoleName() + " baseDamage is " + baseDamage + " scaledDamage" + scaledDamage);
        }

        PlayerRef attackerPlayerRef = attacker.getPlayerRef();
        float attitudeScore = _attitudeScore(roleAndRangeResult.role);

        try {
            float capturedBaseFactor = baseFactor;

            LevelingCoreApi.getLevelServiceIfPresent().ifPresent(levelService -> {

                double finalFactor = capturedBaseFactor;

                if (RuntimeSettings.customLevelingUseAttitudeMultiplier()) {
                    finalFactor = _applyAttitudeMultiplier(finalFactor, attitudeScore);
                }


                if (RuntimeSettings.customLevelingUseMostDamage()) {
                    Player mostDamageAttacker = _getPlayerWithMostDamage(entity);
                    if (mostDamageAttacker != null) {
                        if (RuntimeSettings.customLevelingRewardMostDamage() && !mostDamageAttacker.equals(attacker)) {
                            UUID mostDamageAttackerUuid = mostDamageAttacker.getUuid();
                            PlayerRef mostDamageAttackerPlayerRef = mostDamageAttacker.getPlayerRef();
                            double finalFactorMostDamage = finalFactor * RuntimeSettings.customLevelingOtherAttackerMultiplier();

                            if (mostDamageAttackerPlayerRef != null && mostDamageAttackerUuid != null) {
                                sendXPReward(levelService, mostDamageAttackerUuid, mostDamageAttackerPlayerRef, finalFactorMostDamage);
                            }
                        }

                        if (mostDamageAttacker.equals(attacker)) {
                            finalFactor *= RuntimeSettings.customLevelingMostDamageMultiplier();
                            sendXPReward(levelService, attackerUuid, attackerPlayerRef, finalFactor);
                            return;
                        }
                    }
                }

                sendXPReward(levelService, attackerUuid, attackerPlayerRef, finalFactor);
            });
        } catch (NoClassDefFoundError error) {
            LibraryAvailability.logMissingDependency("LevelingCore", error);
        }
    }

    public void sendXPReward(LevelServiceImpl levelService, UUID uuidToReward, PlayerRef playerRefToReward, double finalFactor) {
        int level = levelService.getLevel(uuidToReward);

        long xpToReward = _calculateXpRewardHardDownscaled(finalFactor, level);
        if (xpToReward <= 0L) {
            return;
        }
        Logging.debug("[ONDEATH] xpToReward=" + xpToReward + " finalFactor=" + finalFactor);

        levelService.addXp(uuidToReward, xpToReward);
        XPBarHud.updateHud(playerRefToReward);
        NotificationUtil.sendNotification(playerRefToReward.getPacketHandler(),
                Message.translation("commands.levelingcore.gained").param("xp", xpToReward), NotificationStyle.Success);
    }

    public record RoleAndRangeResult(
            Role role,
            float range
    ) {
    }

    private RoleAndRangeResult _getMaxRange(NPCEntity entity) {
        Role role = entity.getRole();
        if (role == null) {
            return new RoleAndRangeResult(null, 0);
        }
        RoleStats roleStats = role.getRoleStats();
        if (roleStats == null) {
            return new RoleAndRangeResult(null, 0);
        }
        int[] ranges = roleStats.getRangesSorted(true, RoleStats.RangeType.SORTED);
        int maxRange = ranges.length > 0 ? ranges[ranges.length - 1] : 0;
        return new RoleAndRangeResult(role, (float) maxRange);
    }

    private static long _calculateXpRewardHardDownscaled(double _finalFactor, int _level) {
        if (!Double.isFinite(_finalFactor) || _finalFactor <= 0.0) {
            return 0L;
        }

        int safeLevel = Math.max(1, _level);

        double downScale = RuntimeSettings.customLevelingDownscaleBase()
                + Math.pow(safeLevel, RuntimeSettings.customLevelingDownscaleLevelExponent())
                * RuntimeSettings.customLevelingDownscaleLevelMultiplier();
        double xp = _finalFactor / downScale;

        if (!Double.isFinite(xp) || xp <= 0.0) {
            return 0L;
        }

        return Math.max(0L, Math.round(xp));
    }


    // attacker
    private float _getCalculatedBaseDamage(@Nonnull NPCEntity entity) {
        return NpcRoles.getBaseDamageMax(entity.getRoleName());
    }

    @Nullable
    private Player _getPlayerWithMostDamage(@Nonnull NPCEntity entity) {
        Ref<EntityStore> refMostDamageAttacker = entity.getDamageData().getMostDamagingAttacker();
        if (refMostDamageAttacker == null || !refMostDamageAttacker.isValid()) {
            return null;
        }

        Store<EntityStore> storeMostDamageAttacker = refMostDamageAttacker.getStore();
        return (Player) storeMostDamageAttacker.getComponent(refMostDamageAttacker, Player.getComponentType());
    }

    private long _defaultEntityStats(@Nonnull EntityStatMap statMap) {
        double mana = RuntimeSettings.customLevelingStatsManaMultiplier();
        double ammo = RuntimeSettings.customLevelingStatsAmmoMultiplier();
        double signature = RuntimeSettings.customLevelingStatsSignatureMultiplier();
        return Math.round(
                _statMax(statMap, DefaultEntityStatTypes.getHealth())
                        + _statMax(statMap, DefaultEntityStatTypes.getMana()) * mana
                        + _statMax(statMap, DefaultEntityStatTypes.getAmmo()) * ammo
                        + _statMax(statMap, DefaultEntityStatTypes.getSignatureEnergy()) * signature
        );
    }

    private static float _statMax(@Nonnull EntityStatMap map, int stat) {
        EntityStatValue v = map.get(stat);
        return v != null ? v.getMax() : 0.0f;
    }

    private float _attitudeScore(Role role) {
        if (role == null) {
            return 0;
        }
        WorldSupport worldSupport = role.getWorldSupport();
        Attitude againstNPC = worldSupport.getDefaultNPCAttitude();
        Attitude againstPlayer = worldSupport.getDefaultPlayerAttitude();

        int base = switch (againstPlayer) {
            case REVERED -> (int) Math.round(RuntimeSettings.customLevelingAttitudePlayerReveredScore());
            case FRIENDLY, NEUTRAL, IGNORE -> (int) Math.round(RuntimeSettings.customLevelingAttitudePlayerFriendlyScore());
            case HOSTILE -> (int) Math.round(RuntimeSettings.customLevelingAttitudePlayerHostileScore());
            default -> 0;
        };

        if (againstNPC == Attitude.HOSTILE) {
            base += (int) Math.round(RuntimeSettings.customLevelingAttitudeNpcHostileBonus());
        }

        return (float) base;
    }

    private static double _applyAttitudeMultiplier(double _f, float _attitudeScore) {
        double m = 1.0;

        if (_attitudeScore <= RuntimeSettings.customLevelingAttitudeThresholdLow()) {
            m = RuntimeSettings.customLevelingAttitudeMultiplierLow();
        } else if (_attitudeScore <= RuntimeSettings.customLevelingAttitudeThresholdMid()) {
            m = RuntimeSettings.customLevelingAttitudeMultiplierMid();
        } else if (_attitudeScore >= RuntimeSettings.customLevelingAttitudeThresholdHigh()) {
            m = RuntimeSettings.customLevelingAttitudeMultiplierHigh();
        }

        return _f * m;
    }

    public float applyDamageMultiplier(float damage, float damageMultiplier) {
        float d = Math.max(0.0f, damage);
        float m = Math.max(0.0f, damageMultiplier);
        float scaled = d * m;
        return Math.max(d * _minDamageFactor, scaled);
    }
}
