package ascendant.core.config;

import com.google.gson.*;
import com.google.gson.reflect.TypeToken;

import java.io.IOException;
import java.io.Reader;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Collections;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;

public final class DifficultyConfig {
    public static final Path DEFAULT_PATH = Path.of("config", "ascendant", "difficulty.json");
    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();
    private final Path file;
    private final Path dropInsDir;
    private JsonObject root;

    private DifficultyConfig(Path file, JsonObject root) {
        this(file, null, root);
    }

    private DifficultyConfig(Path file, Path dropInsDir, JsonObject root) {
        this.file = Objects.requireNonNull(file, "file");
        this.dropInsDir = dropInsDir;
        this.root = Objects.requireNonNull(root, "root");
    }

    public static DifficultyConfig load(Path file) throws IOException {
        Objects.requireNonNull(file, "file");
        try (Reader reader = Files.newBufferedReader(file, StandardCharsets.UTF_8)) {
            JsonObject root = JsonParser.parseReader(reader).getAsJsonObject();
            return new DifficultyConfig(file, root);
        }
    }

    public static DifficultyConfig loadWithDropIns(Path file, Path dropInsDir) throws IOException {
        Objects.requireNonNull(file, "file");
        Objects.requireNonNull(dropInsDir, "dropInsDir");
        DifficultyConfig base = load(file);
        JsonObject merged = DifficultyDropIns.mergeIntoRoot(base.root, dropInsDir);
        return new DifficultyConfig(file, dropInsDir, merged);
    }

    public static DifficultyConfig loadDefault(JsonObject defaults) throws IOException {
        return loadOrCreate(DEFAULT_PATH, defaults);
    }

    public static DifficultyConfig loadOrCreate(Path file, JsonObject defaults) throws IOException {
        Objects.requireNonNull(file, "file");
        Objects.requireNonNull(defaults, "defaults");
        if (Files.notExists(file)) {
            Files.createDirectories(file.getParent());
            Files.writeString(file, GSON.toJson(defaults), StandardCharsets.UTF_8);
            return new DifficultyConfig(file, defaults.deepCopy());
        }
        return load(file);
    }

    public static DifficultyConfig loadOrCreateWithDropIns(Path file, Path dropInsDir, JsonObject defaults) throws IOException {
        Objects.requireNonNull(file, "file");
        Objects.requireNonNull(dropInsDir, "dropInsDir");
        Objects.requireNonNull(defaults, "defaults");
        DifficultyConfig base = loadOrCreate(file, defaults);
        JsonObject merged = DifficultyDropIns.mergeIntoRoot(base.root, dropInsDir);
        return new DifficultyConfig(file, dropInsDir, merged);
    }

    public void reload() throws IOException {
        DifficultyConfig reloaded = (this.dropInsDir == null)
                ? load(this.file)
                : loadWithDropIns(this.file, this.dropInsDir);
        this.root = reloaded.root;
    }

    public DifficultySettings reloadToSettings() throws IOException {
        reload();
        return DifficultySettings.fromConfig(this);
    }

    public DifficultySettings toSettings() {
        return DifficultySettings.fromConfig(this);
    }

    public JsonObject root() {
        return this.root;
    }

    public Optional<JsonElement> get(String path) {
        Objects.requireNonNull(path, "path");
        String[] parts = path.split("\\.");
        JsonElement current = this.root;
        for (String part : parts) {
            if (!current.isJsonObject()) {
                return Optional.empty();
            }
            JsonObject obj = current.getAsJsonObject();
            if (!obj.has(part)) {
                return Optional.empty();
            }
            current = obj.get(part);
        }
        return Optional.ofNullable(current);
    }

    public String getString(String path, String fallback) {
        return get(path).filter(JsonElement::isJsonPrimitive).map(JsonElement::getAsString).orElse(fallback);
    }

    public int getInt(String path, int fallback) {
        return get(path).filter(JsonElement::isJsonPrimitive).map(JsonElement::getAsInt).orElse(fallback);
    }

    public double getDouble(String path, double fallback) {
        return get(path).filter(JsonElement::isJsonPrimitive).map(JsonElement::getAsDouble).orElse(fallback);
    }

    public boolean getBoolean(String path, boolean fallback) {
        return get(path).filter(JsonElement::isJsonPrimitive).map(JsonElement::getAsBoolean).orElse(fallback);
    }

    public JsonObject getSection(String path) {
        return get(path).filter(JsonElement::isJsonObject).map(JsonElement::getAsJsonObject).orElseGet(JsonObject::new);
    }

    public Map<String, Object> asMap() {
        return GSON.fromJson(this.root, new TypeToken<Map<String, Object>>() {
        }.getType());
    }

    public Map<String, Object> getSectionAsMap(String path) {
        JsonObject section = getSection(path);
        if (section.isEmpty()) {
            return Collections.emptyMap();
        }
        return GSON.fromJson(section, new TypeToken<Map<String, Object>>() {
        }.getType());
    }
}
