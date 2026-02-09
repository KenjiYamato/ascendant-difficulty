package ascendant.core.util;

import ascendant.core.config.DifficultyIO;
import ascendant.core.config.DifficultyManager;
import com.hypixel.hytale.logger.HytaleLogger;

import java.util.logging.Level;

public class Logging {
    private static final HytaleLogger LOGGER = HytaleLogger.forEnclosingClass();

    public static void info(String message) {
        LOGGER.at(Level.INFO).log(message);
    }

    public static void debug(String message) {
        if (!DifficultyManager.getFromConfig(DifficultyIO.ALLOW_DEBUG_LOGGING)) {
            return;
        }
        LOGGER.at(Level.INFO).log(message);
    }
}
