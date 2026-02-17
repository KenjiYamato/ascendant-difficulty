package ascendant.core.config;

import java.util.List;
import java.util.Objects;

public final class ConfigKey<T> {
    private final String path;
    private final T defaultValue;
    private final Reader<T> reader;

    private ConfigKey(String path, T defaultValue, Reader<T> reader) {
        this.path = Objects.requireNonNull(path, "path");
        this.defaultValue = Objects.requireNonNull(defaultValue, "defaultValue");
        this.reader = Objects.requireNonNull(reader, "reader");
    }

    public static ConfigKey<String> ofString(String path, String defaultValue) {
        return new ConfigKey<>(path, defaultValue, DifficultyConfig::getString);
    }

    public static ConfigKey<Integer> ofInt(String path, int defaultValue) {
        return new ConfigKey<>(path, defaultValue, DifficultyConfig::getInt);
    }

    public static ConfigKey<Double> ofDouble(String path, double defaultValue) {
        return new ConfigKey<>(path, defaultValue, DifficultyConfig::getDouble);
    }

    public static ConfigKey<Boolean> ofBoolean(String path, boolean defaultValue) {
        return new ConfigKey<>(path, defaultValue, DifficultyConfig::getBoolean);
    }

    public static ConfigKey<Boolean> ofBooleanWithFallback(String path, String fallbackPath, boolean defaultValue) {
        return new ConfigKey<>(path, defaultValue, (config, primaryPath, fallback) -> {
            if (config.get(primaryPath).isPresent()) {
                return config.getBoolean(primaryPath, fallback);
            }
            if (fallbackPath == null || fallbackPath.isBlank()) {
                return fallback;
            }
            return config.getBoolean(fallbackPath, fallback);
        });
    }

    public static ConfigKey<List<String>> ofStringList(String path, List<String> defaultValue) {
        return new ConfigKey<>(path, defaultValue, DifficultyConfig::getStringList);
    }

    public T read(DifficultyConfig config) {
        Objects.requireNonNull(config, "config");
        return reader.read(config, path, defaultValue);
    }

    public String path() {
        return path;
    }

    public T defaultValue() {
        return defaultValue;
    }

    @FunctionalInterface
    public interface Reader<T> {
        T read(DifficultyConfig config, String path, T defaultValue);
    }
}
