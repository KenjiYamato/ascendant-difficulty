package ascendant.core.util;

import com.hypixel.hytale.logger.HytaleLogger;

import java.util.logging.Level;

public class Logging {
    private static final HytaleLogger LOGGER = HytaleLogger.forEnclosingClass();

    public static void info(String message) {
        LOGGER.at(Level.INFO).log(message);
    }
}
