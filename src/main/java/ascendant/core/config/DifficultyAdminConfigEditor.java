package ascendant.core.config;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.google.gson.JsonPrimitive;

import java.io.IOException;
import java.io.Reader;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Locale;
import java.util.Objects;
import java.util.stream.Stream;

public final class DifficultyAdminConfigEditor {
    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();

    private DifficultyAdminConfigEditor() {
    }

    public static synchronized void setBaseBoolean(String path, boolean value) throws IOException {
        Objects.requireNonNull(path, "path");
        JsonObject root = readBaseRoot();
        setPath(root, path, new JsonPrimitive(value));
        writeBaseRoot(root);
    }

    public static synchronized void setBaseNumber(String path, double value, boolean integer) throws IOException {
        Objects.requireNonNull(path, "path");
        JsonObject root = readBaseRoot();
        if (integer) {
            setPath(root, path, new JsonPrimitive((int) Math.round(value)));
        } else {
            setPath(root, path, new JsonPrimitive(value));
        }
        writeBaseRoot(root);
    }

    public static synchronized void setBaseString(String path, String value) throws IOException {
        Objects.requireNonNull(path, "path");
        Objects.requireNonNull(value, "value");
        JsonObject root = readBaseRoot();
        setPath(root, path, new JsonPrimitive(value));
        writeBaseRoot(root);
    }

    public static synchronized void setTierBoolean(String tierId, String key, boolean value) throws IOException {
        Objects.requireNonNull(tierId, "tierId");
        Objects.requireNonNull(key, "key");
        Path path = resolveTierDropInPath(tierId);
        JsonObject root = readJsonObject(path);
        JsonObject tierNode = resolveTierNode(root);
        tierNode.addProperty(key, value);
        writeJsonObject(path, root);
    }

    public static synchronized void setTierNumber(String tierId, String key, double value, boolean integer) throws IOException {
        Objects.requireNonNull(tierId, "tierId");
        Objects.requireNonNull(key, "key");
        Path path = resolveTierDropInPath(tierId);
        JsonObject root = readJsonObject(path);
        JsonObject tierNode = resolveTierNode(root);
        if (integer) {
            tierNode.addProperty(key, (int) Math.round(value));
        } else {
            tierNode.addProperty(key, value);
        }
        writeJsonObject(path, root);
    }

    private static JsonObject readBaseRoot() throws IOException {
        return readJsonObject(DifficultyIO.DEFAULT_CONFIG_PATH);
    }

    private static void writeBaseRoot(JsonObject root) throws IOException {
        writeJsonObject(DifficultyIO.DEFAULT_CONFIG_PATH, root);
    }

    private static JsonObject readJsonObject(Path path) throws IOException {
        if (path == null || Files.notExists(path)) {
            throw new IOException("Config file not found: " + path);
        }
        try (Reader reader = Files.newBufferedReader(path, StandardCharsets.UTF_8)) {
            JsonElement parsed = JsonParser.parseReader(reader);
            if (parsed == null || !parsed.isJsonObject()) {
                throw new IOException("Invalid JSON object in " + path);
            }
            return parsed.getAsJsonObject();
        }
    }

    private static void writeJsonObject(Path path, JsonObject root) throws IOException {
        Path parent = path.getParent();
        if (parent != null) {
            Files.createDirectories(parent);
        }
        Files.writeString(path, GSON.toJson(root), StandardCharsets.UTF_8);
    }

    private static void setPath(JsonObject root, String dottedPath, JsonElement value) {
        String[] parts = dottedPath.split("\\.");
        if (parts.length == 0) {
            return;
        }
        JsonObject cursor = root;
        for (int i = 0; i < parts.length - 1; i++) {
            String part = parts[i];
            JsonElement next = cursor.get(part);
            if (next == null || !next.isJsonObject()) {
                JsonObject created = new JsonObject();
                cursor.add(part, created);
                cursor = created;
                continue;
            }
            cursor = next.getAsJsonObject();
        }
        cursor.add(parts[parts.length - 1], value);
    }

    private static Path resolveTierDropInPath(String tierId) throws IOException {
        Path dir = DifficultyIO.DIFFICULTY_DROPINS_PATH;
        if (Files.notExists(dir) || !Files.isDirectory(dir)) {
            throw new IOException("Drop-in directory not found: " + dir);
        }

        String target = tierId.trim();
        try (Stream<Path> stream = Files.list(dir)) {
            for (Path file : stream.toList()) {
                if (!Files.isRegularFile(file)) {
                    continue;
                }
                String fileName = file.getFileName().toString().toLowerCase(Locale.ROOT);
                if (!fileName.endsWith(".json")) {
                    continue;
                }
                String id = readDropInTierId(file);
                if (id != null && id.equalsIgnoreCase(target)) {
                    return file;
                }
            }
        }
        throw new IOException("No drop-in file found for tier '" + tierId + "'");
    }

    private static String readDropInTierId(Path file) {
        try (Reader reader = Files.newBufferedReader(file, StandardCharsets.UTF_8)) {
            JsonElement parsed = JsonParser.parseReader(reader);
            if (parsed == null || !parsed.isJsonObject()) {
                return null;
            }
            JsonObject root = parsed.getAsJsonObject();
            JsonElement id = root.get("id");
            if (id == null || !id.isJsonPrimitive()) {
                return null;
            }
            return id.getAsString();
        } catch (Exception ignored) {
            return null;
        }
    }

    private static JsonObject resolveTierNode(JsonObject root) {
        JsonElement tierElement = root.get("tier");
        if (tierElement != null && tierElement.isJsonObject()) {
            return tierElement.getAsJsonObject();
        }
        return root;
    }
}
