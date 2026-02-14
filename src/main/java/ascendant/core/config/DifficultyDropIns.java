package ascendant.core.config;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

import java.io.IOException;
import java.io.InputStream;
import java.io.Reader;
import java.net.JarURLConnection;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.Enumeration;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.jar.JarEntry;
import java.util.jar.JarFile;
import java.util.stream.Stream;

final class DifficultyDropIns {
    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();
    private static final Set<String> RESERVED_KEYS = Set.of("meta", "order", "id", "tier");
    private static final String RESOURCE_DIR = "difficultys";

    private DifficultyDropIns() {
    }

    static JsonObject mergeIntoRoot(JsonObject baseRoot, Path dropInsDir) {
        JsonObject merged = baseRoot != null ? baseRoot.deepCopy() : new JsonObject();
        List<DropIn> dropIns = loadDropIns(dropInsDir);
        if (dropIns.isEmpty()) {
            return merged;
        }

        merged.remove("meta");
        merged.remove("tiers");

        JsonObject metaOut = new JsonObject();
        JsonObject tiersOut = new JsonObject();
        Set<String> seen = new HashSet<>();
        for (DropIn dropIn : dropIns) {
            if (!seen.add(dropIn.id)) {
                System.err.println("[ascendant] Duplicate difficulty id '" + dropIn.id + "' in " + dropIn.source + "; skipping.");
                continue;
            }
            if (dropIn.meta != null && !dropIn.meta.isEmpty()) {
                metaOut.add(dropIn.id, dropIn.meta);
            }
            JsonObject tier = dropIn.tier != null ? dropIn.tier : new JsonObject();
            tiersOut.add(dropIn.id, tier);
        }
        merged.add("meta", metaOut);
        merged.add("tiers", tiersOut);
        return merged;
    }

    static void writeDefaultsFromLegacy(JsonObject legacyRoot, Path dropInsDir) {
        if (legacyRoot == null || dropInsDir == null) {
            return;
        }
        JsonObject tiersObj = legacyRoot.getAsJsonObject("tiers");
        if (tiersObj == null || tiersObj.isEmpty()) {
            return;
        }
        if (hasJsonFiles(dropInsDir)) {
            return;
        }
        try {
            Files.createDirectories(dropInsDir);
        } catch (IOException e) {
            System.err.println("[ascendant] Failed to create difficulty drop-ins dir: " + e.getMessage());
            return;
        }

        JsonObject metaObj = legacyRoot.getAsJsonObject("meta");
        int index = 1;
        for (Map.Entry<String, JsonElement> entry : tiersObj.entrySet()) {
            if (!entry.getValue().isJsonObject()) {
                continue;
            }
            String tierId = entry.getKey();
            JsonObject out = new JsonObject();
            out.addProperty("id", tierId);
            out.addProperty("order", index);
            if (metaObj != null) {
                JsonElement metaElement = metaObj.get(tierId);
                if (metaElement != null && metaElement.isJsonObject()) {
                    out.add("meta", metaElement.getAsJsonObject().deepCopy());
                }
            }
            JsonObject tierOverrides = entry.getValue().getAsJsonObject().deepCopy();
            for (Map.Entry<String, JsonElement> tierEntry : tierOverrides.entrySet()) {
                out.add(tierEntry.getKey(), tierEntry.getValue().deepCopy());
            }
            String safeId = sanitizeFileName(tierId);
            String fileName = safeId.isBlank() ? String.format("difficulty_%02d.json", index) : safeId + ".json";
            Path outPath = dropInsDir.resolve(fileName);
            if (Files.exists(outPath)) {
                index++;
                continue;
            }
            try {
                Files.writeString(outPath, GSON.toJson(out), StandardCharsets.UTF_8);
            } catch (IOException e) {
                System.err.println("[ascendant] Failed to write difficulty drop-in " + outPath + ": " + e.getMessage());
            }
            index++;
        }
    }

    static boolean writeDefaultsFromResources(Path dropInsDir) {
        if (dropInsDir == null) {
            return false;
        }
        if (hasJsonFiles(dropInsDir)) {
            return true;
        }
        try {
            Files.createDirectories(dropInsDir);
        } catch (IOException e) {
            System.err.println("[ascendant] Failed to create difficulty drop-ins dir: " + e.getMessage());
            return false;
        }
        ClassLoader loader = DifficultyDropIns.class.getClassLoader();
        List<String> resourceFiles = listResourceFiles(loader);
        if (resourceFiles.isEmpty()) {
            return false;
        }
        boolean wroteAny = false;
        for (String fileName : resourceFiles) {
            if (fileName == null || fileName.isBlank()) {
                continue;
            }
            Path outPath = dropInsDir.resolve(fileName);
            if (Files.exists(outPath)) {
                wroteAny = true;
                continue;
            }
            String resourcePath = RESOURCE_DIR + "/" + fileName;
            try (InputStream in = loader.getResourceAsStream(resourcePath)) {
                if (in == null) {
                    System.err.println("[ascendant] Missing difficulty drop-in resource " + resourcePath);
                    continue;
                }
                byte[] data = in.readAllBytes();
                Files.write(outPath, data);
                wroteAny = true;
            } catch (IOException e) {
                System.err.println("[ascendant] Failed to write difficulty drop-in " + outPath + ": " + e.getMessage());
            }
        }
        return wroteAny;
    }

    private static List<DropIn> loadDropIns(Path dropInsDir) {
        if (dropInsDir == null) {
            return List.of();
        }
        try {
            Files.createDirectories(dropInsDir);
        } catch (IOException e) {
            System.err.println("[ascendant] Failed to create difficulty drop-ins dir: " + e.getMessage());
            return List.of();
        }
        if (!Files.isDirectory(dropInsDir)) {
            return List.of();
        }
        List<DropIn> out = new ArrayList<>();
        try (Stream<Path> stream = Files.list(dropInsDir)) {
            stream.filter(path -> Files.isRegularFile(path))
                    .filter(path -> path.getFileName().toString().toLowerCase(Locale.ROOT).endsWith(".json"))
                    .forEach(path -> {
                        String name = path.getFileName().toString();
                        if ("index.json".equalsIgnoreCase(name)) {
                            return;
                        }
                        DropIn dropIn = readDropIn(path);
                        if (dropIn != null) {
                            out.add(dropIn);
                        }
                    });
        } catch (IOException e) {
            System.err.println("[ascendant] Failed to read difficulty drop-ins from " + dropInsDir + ": " + e.getMessage());
        }
        out.sort(DropIn.ORDER);
        return out;
    }

    private static DropIn readDropIn(Path path) {
        String fileName = path.getFileName().toString();
        try (Reader reader = Files.newBufferedReader(path, StandardCharsets.UTF_8)) {
            JsonElement parsed = JsonParser.parseReader(reader);
            if (parsed == null || !parsed.isJsonObject()) {
                return null;
            }
            JsonObject root = parsed.getAsJsonObject();
            String id = readString(root, "id");
            if (id == null || id.isBlank()) {
                System.err.println("[ascendant] Difficulty drop-in missing id in " + fileName + "; skipping.");
                return null;
            }

            Double orderValue = readNumber(root, "order");
            if (orderValue == null) {
                System.err.println("[ascendant] Difficulty drop-in missing order in " + fileName + "; defaulting to end.");
            }
            double order = orderValue != null ? orderValue : Double.POSITIVE_INFINITY;

            JsonObject meta = null;
            JsonElement metaElement = root.get("meta");
            if (metaElement != null && metaElement.isJsonObject()) {
                meta = metaElement.getAsJsonObject().deepCopy();
            }

            JsonObject tier = extractTierOverrides(root);
            return new DropIn(id, order, fileName, meta, tier);
        } catch (Exception e) {
            System.err.println("[ascendant] Failed to read difficulty drop-in " + path + ": " + e.getMessage());
            return null;
        }
    }

    private static JsonObject extractTierOverrides(JsonObject root) {
        JsonElement tierElement = root.get("tier");
        if (tierElement != null && tierElement.isJsonObject()) {
            return tierElement.getAsJsonObject().deepCopy();
        }
        JsonObject out = new JsonObject();
        for (Map.Entry<String, JsonElement> entry : root.entrySet()) {
            if (RESERVED_KEYS.contains(entry.getKey())) {
                continue;
            }
            out.add(entry.getKey(), entry.getValue().deepCopy());
        }
        return out;
    }

    static boolean hasJsonFiles(Path dir) {
        if (dir == null || !Files.isDirectory(dir)) {
            return false;
        }
        try (Stream<Path> stream = Files.list(dir)) {
            return stream.anyMatch(path -> Files.isRegularFile(path)
                    && path.getFileName().toString().toLowerCase(Locale.ROOT).endsWith(".json"));
        } catch (IOException e) {
            return false;
        }
    }

    private static String readString(JsonObject root, String key) {
        JsonElement element = root.get(key);
        if (element != null && element.isJsonPrimitive()) {
            try {
                return element.getAsString();
            } catch (UnsupportedOperationException ignored) {
            }
        }
        return null;
    }

    private static Double readNumber(JsonObject root, String key) {
        JsonElement element = root.get(key);
        if (element != null && element.isJsonPrimitive()) {
            try {
                return element.getAsDouble();
            } catch (NumberFormatException | UnsupportedOperationException ignored) {
            }
        }
        return null;
    }

    private static List<String> listResourceFiles(ClassLoader loader) {
        List<String> files = new ArrayList<>();
        try {
            URL dirUrl = loader.getResource(RESOURCE_DIR);
            if (dirUrl == null) {
                return filesFromCodeSource(files);
            }
            String protocol = dirUrl.getProtocol();
            if ("file".equalsIgnoreCase(protocol)) {
                Path dir = Paths.get(dirUrl.toURI());
                try (Stream<Path> stream = Files.list(dir)) {
                    stream.filter(Files::isRegularFile)
                            .map(path -> path.getFileName().toString())
                            .filter(name -> name.toLowerCase(Locale.ROOT).endsWith(".json"))
                            .forEach(files::add);
                }
            } else if ("jar".equalsIgnoreCase(protocol)) {
                JarURLConnection connection = (JarURLConnection) dirUrl.openConnection();
                try (JarFile jar = connection.getJarFile()) {
                    Enumeration<JarEntry> entries = jar.entries();
                    String prefix = RESOURCE_DIR + "/";
                    while (entries.hasMoreElements()) {
                        JarEntry entry = entries.nextElement();
                        String name = entry.getName();
                        if (!name.startsWith(prefix) || entry.isDirectory()) {
                            continue;
                        }
                        String fileName = name.substring(prefix.length());
                        if (!fileName.toLowerCase(Locale.ROOT).endsWith(".json")) {
                            continue;
                        }
                        files.add(fileName);
                    }
                }
            }
        } catch (Exception e) {
            System.err.println("[ascendant] Failed to list difficulty drop-in resources: " + e.getMessage());
        }
        if (files.isEmpty()) {
            return filesFromCodeSource(files);
        }
        files.sort(String.CASE_INSENSITIVE_ORDER);
        return files;
    }

    private static List<String> filesFromCodeSource(List<String> files) {
        try {
            URL location = DifficultyDropIns.class.getProtectionDomain().getCodeSource().getLocation();
            if (location == null) {
                return files;
            }
            Path locationPath = Paths.get(location.toURI());
            if (Files.isDirectory(locationPath)) {
                Path dir = locationPath.resolve(RESOURCE_DIR);
                if (Files.isDirectory(dir)) {
                    try (Stream<Path> stream = Files.list(dir)) {
                        stream.filter(Files::isRegularFile)
                                .map(path -> path.getFileName().toString())
                                .filter(name -> name.toLowerCase(Locale.ROOT).endsWith(".json"))
                                .forEach(files::add);
                    }
                }
            } else {
                try (JarFile jar = new JarFile(locationPath.toFile())) {
                    Enumeration<JarEntry> entries = jar.entries();
                    String prefix = RESOURCE_DIR + "/";
                    while (entries.hasMoreElements()) {
                        JarEntry entry = entries.nextElement();
                        String name = entry.getName();
                        if (!name.startsWith(prefix) || entry.isDirectory()) {
                            continue;
                        }
                        String fileName = name.substring(prefix.length());
                        if (!fileName.toLowerCase(Locale.ROOT).endsWith(".json")) {
                            continue;
                        }
                        files.add(fileName);
                    }
                }
            }
        } catch (Exception e) {
            System.err.println("[ascendant] Failed to list difficulty drop-in resources (code source): " + e.getMessage());
        }
        files.sort(String.CASE_INSENSITIVE_ORDER);
        return files;
    }

    private static String sanitizeFileName(String value) {
        if (value == null) {
            return "";
        }
        return value.replaceAll("[\\\\/:*?\"<>|]", "_");
    }

    private record DropIn(String id, double order, String source, JsonObject meta, JsonObject tier) {
        private static final Comparator<DropIn> ORDER = Comparator
                .comparingDouble((DropIn dropIn) -> dropIn.order)
                .thenComparing(dropIn -> Objects.toString(dropIn.id, ""));
    }
}
