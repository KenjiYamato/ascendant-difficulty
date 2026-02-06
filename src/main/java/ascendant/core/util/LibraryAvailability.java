package ascendant.core.util;

import com.hypixel.hytale.logger.HytaleLogger;

import java.util.logging.Level;

public final class LibraryAvailability {
    private static final HytaleLogger LOGGER = HytaleLogger.forEnclosingClass();

    private static final boolean LEVELING_CORE_PRESENT = _checkAndLog(
            "LevelingCore",
            "com.azuredoom.levelingcore.api.LevelingCoreApi"
    );
    private static final boolean ECOTALE_PRESENT = _checkAndLog(
            "Ecotale",
            "com.ecotale.api.EcotaleAPI"
    );
    private static final boolean MMO_SKILLTREE_PRESENT = _checkAndLog(
            "MMOSkillTree",
            "com.ziggfreed.mmoskilltree.api.MMOSkillTreeAPI"
    );

    private LibraryAvailability() {
    }

    public static boolean isLevelingCorePresent() {
        return LEVELING_CORE_PRESENT;
    }

    public static boolean isEcotalePresent() {
        return ECOTALE_PRESENT;
    }

    public static boolean isMMOSkillTreePresent() {
        return MMO_SKILLTREE_PRESENT;
    }

    public static void logMissingDependency(String name, Throwable error) {
        String reason = error != null ? error.getClass().getSimpleName() : "unknown";
        LOGGER.at(Level.INFO).log(name + " not available (" + reason + "). Skipping integration.");
    }

    private static boolean _checkAndLog(String name, String className) {
        try {
            Class.forName(className, false, LibraryAvailability.class.getClassLoader());
            return true;
        } catch (Throwable t) {
            logMissingDependency(name, t);
            return false;
        }
    }
}
