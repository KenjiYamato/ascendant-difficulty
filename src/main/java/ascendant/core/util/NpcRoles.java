package ascendant.core.util;

import ascendant.core.config.DifficultyIO;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.Map;

public final class NpcRoles {
    private static final String NPC_ROLES_RESOURCE = "npc_roles.json";
    private static final Object LOCK = new Object();
    @Nullable
    private static volatile Map<String, Float> ROLE_DAMAGE_MAX;

    private NpcRoles() {
    }

    public static void preload() {
        Map<String, Float> map = getRoleDamageMax();
        Logging.debug("[NpcRoles] npc_roles loaded: " + map.size());
    }

    public static float getBaseDamageMax(@Nullable String roleName) {
        if (roleName == null || roleName.isBlank()) {
            return 0.0f;
        }
        Float max = getRoleDamageMax().get(roleName);
        return max != null ? max : 0.0f;
    }

    @Nonnull
    public static Map<String, Float> getRoleDamageMax() {
        Map<String, Float> cached = ROLE_DAMAGE_MAX;
        if (cached != null) {
            return cached;
        }
        synchronized (LOCK) {
            if (ROLE_DAMAGE_MAX != null) {
                return ROLE_DAMAGE_MAX;
            }
            ROLE_DAMAGE_MAX = loadRoleDamageMax();
            return ROLE_DAMAGE_MAX;
        }
    }

    @Nonnull
    private static Map<String, Float> loadRoleDamageMax() {
        Path configPath = DifficultyIO.NPC_ROLES_PATH;
        if (Files.exists(configPath)) {
            return loadRoleDamageMaxFromFile(configPath);
        }
        return loadRoleDamageMaxFromResource(configPath);
    }

    @Nonnull
    private static Map<String, Float> loadRoleDamageMaxFromFile(@Nonnull Path path) {
        try (InputStream in = Files.newInputStream(path);
             InputStreamReader reader = new InputStreamReader(in, StandardCharsets.UTF_8)) {
            JsonElement parsed = JsonParser.parseReader(reader);
            JsonObject rolesObj = extractRolesObject(parsed, "file");
            if (rolesObj == null) {
                return Map.of();
            }
            return buildRoleDamageMax(rolesObj);
        } catch (Exception e) {
            Logging.debug("[NpcRoles] npc_roles.json load failed from file: " + e.getMessage());
            return Map.of();
        }
    }

    @Nonnull
    private static Map<String, Float> loadRoleDamageMaxFromResource(@Nonnull Path configPath) {
        ClassLoader loader = NpcRoles.class.getClassLoader();
        try (InputStream in = loader.getResourceAsStream(NPC_ROLES_RESOURCE)) {
            if (in == null) {
                Logging.debug("[NpcRoles] npc_roles.json not found on classpath");
                return Map.of();
            }
            byte[] data = in.readAllBytes();
            try (InputStreamReader reader = new InputStreamReader(new ByteArrayInputStream(data), StandardCharsets.UTF_8)) {
                JsonElement parsed = JsonParser.parseReader(reader);
                JsonObject rolesObj = extractRolesObject(parsed, "resource");
                if (rolesObj == null) {
                    return Map.of();
                }
                writeFallbackConfig(configPath, data);
                return buildRoleDamageMax(rolesObj);
            }
        } catch (Exception e) {
            Logging.debug("[NpcRoles] npc_roles.json load failed from resource: " + e.getMessage());
            return Map.of();
        }
    }

    private static void writeFallbackConfig(@Nonnull Path path, @Nonnull byte[] data) {
        try {
            Files.createDirectories(path.getParent());
            Files.write(path, data);
            Logging.debug("[NpcRoles] wrote npc_roles.json fallback to " + path);
        } catch (Exception e) {
            Logging.debug("[NpcRoles] failed to write npc_roles.json fallback: " + e.getMessage());
        }
    }

    @Nullable
    private static JsonObject extractRolesObject(@Nullable JsonElement parsed, @Nonnull String sourceLabel) {
        if (parsed == null || !parsed.isJsonObject()) {
            Logging.debug("[NpcRoles] npc_roles.json root is not an object (" + sourceLabel + ")");
            return null;
        }
        JsonObject root = parsed.getAsJsonObject();
        JsonObject rolesObj = root.getAsJsonObject("roles");
        if (rolesObj == null) {
            Logging.debug("[NpcRoles] npc_roles.json missing roles section (" + sourceLabel + ")");
        }
        return rolesObj;
    }

    @Nonnull
    private static Map<String, Float> buildRoleDamageMax(@Nonnull JsonObject rolesObj) {
        HashMap<String, Float> out = new HashMap<>(Math.max(16, rolesObj.size()));
        for (Map.Entry<String, JsonElement> entry : rolesObj.entrySet()) {
            if (!entry.getValue().isJsonObject()) {
                continue;
            }
            JsonObject roleObj = entry.getValue().getAsJsonObject();
            JsonObject baseDamageStats = roleObj.getAsJsonObject("baseDamageStats");
            float max = readMaxDamage(baseDamageStats);
            out.put(entry.getKey(), max);
        }
        return out.isEmpty() ? Map.of() : Map.copyOf(out);
    }

    private static float readMaxDamage(@Nullable JsonObject baseDamageStats) {
        if (baseDamageStats == null || baseDamageStats.isEmpty()) {
            return 0.0f;
        }
        float max = 0.0f;
        for (Map.Entry<String, JsonElement> entry : baseDamageStats.entrySet()) {
            if (!entry.getValue().isJsonObject()) {
                continue;
            }
            JsonObject stat = entry.getValue().getAsJsonObject();
            JsonElement maxEl = stat.get("max");
            if (maxEl != null && maxEl.isJsonPrimitive()) {
                try {
                    float v = maxEl.getAsFloat();
                    if (v > max) {
                        max = v;
                    }
                } catch (NumberFormatException ignored) {
                }
            }
        }
        return max;
    }
}
