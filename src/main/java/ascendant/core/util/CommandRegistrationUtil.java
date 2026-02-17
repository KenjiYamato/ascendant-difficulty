package ascendant.core.util;

import ascendant.core.config.ConfigKey;
import ascendant.core.config.DifficultyManager;
import com.hypixel.hytale.server.core.command.system.AbstractCommand;

import java.util.*;
import java.util.function.BiFunction;
import java.util.function.Consumer;

public final class CommandRegistrationUtil {
    private CommandRegistrationUtil() {
    }

    public static void registerCommandWithAliases(
            Consumer<AbstractCommand> registrar,
            ConfigKey<String> nameKey,
            String defaultName,
            ConfigKey<List<String>> aliasesKey,
            ConfigKey<String> permissionKey,
            String defaultPermission,
            BiFunction<String, String, AbstractCommand> factory
    ) {
        String primary = resolveCommandName(nameKey, defaultName);
        String permission = resolveCommandPermission(permissionKey, defaultPermission);
        List<String> names = new ArrayList<>();
        addCommandNames(names, primary);

        List<String> rawAliases = DifficultyManager.getFromConfig(aliasesKey);
        if (rawAliases != null) {
            for (String alias : rawAliases) {
                addCommandNames(names, alias);
            }
        }

        if (names.isEmpty()) {
            addCommandNames(names, defaultName);
        }

        Set<String> seen = new HashSet<>();
        for (String name : names) {
            String key = name.toLowerCase(Locale.ROOT);
            if (!seen.add(key)) {
                continue;
            }
            registrar.accept(factory.apply(name, permission));
        }
    }

    private static void addCommandNames(List<String> out, String raw) {
        if (raw == null) {
            return;
        }
        for (String part : raw.split(",")) {
            String trimmed = part.trim();
            if (!trimmed.isEmpty()) {
                out.add(trimmed);
            }
        }
    }

    private static String resolveCommandName(ConfigKey<String> key, String fallback) {
        String name = DifficultyManager.getFromConfig(key);
        if (name == null || name.isBlank()) {
            return fallback;
        }
        return name;
    }

    private static String resolveCommandPermission(ConfigKey<String> key, String fallback) {
        String permission = DifficultyManager.getFromConfig(key);
        if (permission == null || permission.isBlank()) {
            return fallback;
        }
        return permission;
    }
}
