package ascendant.core.util;

import com.hypixel.hytale.logger.HytaleLogger;

import java.util.logging.Level;

public final class LibraryAvailability {
    private static final HytaleLogger LOGGER = HytaleLogger.forEnclosingClass();

    private static volatile Boolean LEVELING_CORE_PRESENT;
    private static volatile Boolean ECOTALE_PRESENT;
    private static volatile Boolean MMO_SKILLTREE_PRESENT;

    private LibraryAvailability() {
    }

    public static boolean isLevelingCorePresent() {
        Boolean cached = LEVELING_CORE_PRESENT;
        if (cached != null) {
            return cached;
        }
        boolean present = _checkAndLog(
                "LevelingCore",
                "com.azuredoom.levelingcore.api.LevelingCoreApi"
        );
        LEVELING_CORE_PRESENT = present;
        return present;
    }

    public static boolean isEcotalePresent() {
        Boolean cached = ECOTALE_PRESENT;
        if (cached != null) {
            return cached;
        }
        boolean present = _checkAndLog(
                "Ecotale",
                "com.ecotale.api.EcotaleAPI"
        );
        ECOTALE_PRESENT = present;
        return present;
    }

    public static boolean isMMOSkillTreePresent() {
        Boolean cached = MMO_SKILLTREE_PRESENT;
        if (cached != null) {
            return cached;
        }
        boolean present = _checkAndLog(
                "MMOSkillTree",
                "com.ziggfreed.mmoskilltree.api.MMOSkillTreeAPI"
        );
        MMO_SKILLTREE_PRESENT = present;
        return present;
    }

    public static void logMissingDependency(String name, Throwable error) {
        String reason = error != null ? error.getClass().getSimpleName() : "unknown";
        LOGGER.at(Level.INFO).log(name + " not available (" + reason + "). Skipping integration.");
    }

    private static boolean _checkAndLog(String name, String className) {
        try {
            ReflectionHelper.resolveClassOrThrow(className, LibraryAvailability.class.getClassLoader());
            return true;
        } catch (Throwable t) {
            logMissingDependency(name, t);
            return false;
        }
    }
}
