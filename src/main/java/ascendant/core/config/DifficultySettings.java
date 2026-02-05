package ascendant.core.config;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;

public final class DifficultySettings {
    public static final List<String> DEFAULT_TIERS = List.of(
            "very easy",
            "easy",
            "normal",
            "challenging",
            "hard",
            "expert",
            "elite",
            "nightmare",
            "mythic",
            "ascendant"
    );

    public static final Set<String> KEYS = Set.of(
            "baseDamageRandomPercentageModifier",
            "health_multiplier",
            "maxSpeed",
            "wanderRadius",
            "viewRange",
            "hearingRange",
            "combatRelativeTurnSpeed",
            "armor_multiplier",
            "damage_multiplier",
            "knockbackResistance",
            "regeneration",
            "drop_rate_multiplier",
            "drop_quantity_multiplier",
            "drop_quality_multiplier",
            "xp_multiplier",
            "cash_multiplier"
    );
    private static final String KEY_IS_ALLOWED = "is_allowed";
    private static final String KEY_IS_HIDDEN = "is_hidden";

    private final Map<String, Double> base;
    private final Map<String, Map<String, Double>> tiers;
    private final boolean baseIsAllowed;
    private final boolean baseIsHidden;
    private final Map<String, Boolean> tiersIsAllowed;
    private final Map<String, Boolean> tiersIsHidden;

    private DifficultySettings(Map<String, Double> base, Map<String, Map<String, Double>> tiers, boolean baseIsAllowed, boolean baseIsHidden, Map<String, Boolean> tiersIsAllowed, Map<String, Boolean> tiersIsHidden) {
        this.base = base;
        this.tiers = tiers;
        this.baseIsAllowed = baseIsAllowed;
        this.baseIsHidden = baseIsHidden;
        this.tiersIsAllowed = tiersIsAllowed;
        this.tiersIsHidden = tiersIsHidden;
    }

    public static DifficultySettings fromConfig(DifficultyConfig config) {
        Objects.requireNonNull(config, "config");
        return fromJson(config.root());
    }

    public static DifficultySettings fromJson(JsonObject root) {
        Objects.requireNonNull(root, "root");
        Map<String, Double> base = readSection(root.getAsJsonObject("base"), 1.0);
        Map<String, Map<String, Double>> tiers = readTiers(root.getAsJsonObject("tiers"), base);
        JsonObject baseObj = root.getAsJsonObject("base");
        boolean baseIsAllowed = readBoolean(baseObj, KEY_IS_ALLOWED, true);
        boolean baseIsHidden = readBoolean(baseObj, KEY_IS_HIDDEN, false);
        Map<String, Boolean> tiersIsAllowed = readTiersBoolean(root.getAsJsonObject("tiers"), baseIsAllowed, KEY_IS_ALLOWED);
        Map<String, Boolean> tiersIsHidden = readTiersBoolean(root.getAsJsonObject("tiers"), baseIsHidden, KEY_IS_HIDDEN);
        return new DifficultySettings(base, tiers, baseIsAllowed, baseIsHidden, tiersIsAllowed, tiersIsHidden);
    }

    public static JsonObject defaultJson() {
        JsonObject root = new JsonObject();
        JsonObject base = new JsonObject();
        for (String key : KEYS) {
            base.addProperty(key, 1.0);
        }
        root.add("base", base);

        JsonObject tiers = new JsonObject();
        for (String tier : DEFAULT_TIERS) {
            JsonObject section = new JsonObject();
            section.addProperty(KEY_IS_ALLOWED, true);
            section.addProperty(KEY_IS_HIDDEN, false);
            tiers.add(tier, section);
        }
        for (int i = 1; i <= 20; i++) {
            JsonObject section = new JsonObject();
            section.addProperty(KEY_IS_ALLOWED, true);
            section.addProperty(KEY_IS_HIDDEN, false);
            tiers.add("ascendant " + toRoman(i), section);
        }

        root.add("tiers", tiers);
        return root;
    }

    // Uses the resource file as defaults; falls back to generated defaults if missing or invalid.
    public static JsonObject defaultJsonFromResource(String resourcePath) {
        String path = (resourcePath == null || resourcePath.isBlank()) ? DifficultyIO.RESOURCE_DEFAULT_PATH : resourcePath;
        try (InputStream in = DifficultySettings.class.getClassLoader().getResourceAsStream(path)) {
            if (in == null) {
                return defaultJson();
            }
            try (InputStreamReader reader = new InputStreamReader(in, java.nio.charset.StandardCharsets.UTF_8)) {
                return JsonParser.parseReader(reader).getAsJsonObject();
            }
        } catch (Exception e) {
            return defaultJson();
        }
    }

    public Map<String, Double> base() {
        return Collections.unmodifiableMap(this.base);
    }

    public Map<String, Map<String, Double>> tiers() {
        return Collections.unmodifiableMap(this.tiers);
    }

    public double get(String tier, String key) {
        if (!KEYS.contains(key)) {
            return this.base.getOrDefault(key, 1.0);
        }
        Map<String, Double> tierValues = this.tiers.get(tier);
        if (tierValues != null && tierValues.containsKey(key)) {
            return tierValues.get(key);
        }
        return this.base.getOrDefault(key, 1.0);
    }

    public boolean getBoolean(String tier, String key) {
        if (KEY_IS_ALLOWED.equals(key)) {
            return this.tiersIsAllowed.getOrDefault(tier, this.baseIsAllowed);
        }
        if (KEY_IS_HIDDEN.equals(key)) {
            return this.tiersIsHidden.getOrDefault(tier, this.baseIsHidden);
        }
        return false;
    }

    public Map<String, Double> getTier(String tier) {
        return this.tiers.getOrDefault(tier, Collections.emptyMap());
    }

    private static Map<String, Double> readSection(JsonObject section, double defaultValue) {
        Map<String, Double> result = new LinkedHashMap<>();
        if (section == null) {
            for (String key : KEYS) {
                result.put(key, defaultValue);
            }
            return result;
        }
        for (String key : KEYS) {
            JsonElement element = section.get(key);
            if (element != null && element.isJsonPrimitive()) {
                result.put(key, element.getAsDouble());
            } else {
                result.put(key, defaultValue);
            }
        }
        return result;
    }

    private static Map<String, Map<String, Double>> readTiers(JsonObject tiersObj, Map<String, Double> base) {
        Map<String, Map<String, Double>> tiers = new LinkedHashMap<>();
        if (tiersObj == null) {
            return tiers;
        }
        Map<String, Double> previous = new LinkedHashMap<>(base);
        for (Map.Entry<String, JsonElement> entry : tiersObj.entrySet()) {
            if (entry.getValue().isJsonObject()) {
                Map<String, Double> merged = mergeWithPrevious(entry.getValue().getAsJsonObject(), previous);
                tiers.put(entry.getKey(), merged);
                previous = merged;
            }
        }
        return tiers;
    }

    private static Map<String, Boolean> readTiersBoolean(JsonObject tiersObj, boolean baseValue, String key) {
        Map<String, Boolean> tiers = new LinkedHashMap<>();
        if (tiersObj == null) {
            return tiers;
        }
        boolean previous = baseValue;
        for (Map.Entry<String, JsonElement> entry : tiersObj.entrySet()) {
            if (entry.getValue().isJsonObject()) {
                JsonObject section = entry.getValue().getAsJsonObject();
                previous = readBoolean(section, key, previous);
                tiers.put(entry.getKey(), previous);
            }
        }
        return tiers;
    }

    private static boolean readBoolean(JsonObject section, String key, boolean fallback) {
        if (section == null) {
            return fallback;
        }
        JsonElement element = section.get(key);
        if (element != null && element.isJsonPrimitive()) {
            try {
                return element.getAsBoolean();
            } catch (UnsupportedOperationException | NumberFormatException ignored) {
                return fallback;
            }
        }
        return fallback;
    }

    private static Map<String, Double> mergeWithPrevious(JsonObject section, Map<String, Double> previous) {
        Map<String, Double> result = new LinkedHashMap<>(previous);
        for (String key : KEYS) {
            JsonElement element = section.get(key);
            if (element != null && element.isJsonPrimitive()) {
                result.put(key, element.getAsDouble());
            }
        }
        return result;
    }

    private static String toRoman(int number) {
        if (number <= 0 || number > 20) {
            return String.valueOf(number);
        }
        int[] values = {10, 9, 5, 4, 1};
        String[] symbols = {"X", "IX", "V", "IV", "I"};
        StringBuilder sb = new StringBuilder();
        int remaining = number;
        for (int i = 0; i < values.length; i++) {
            while (remaining >= values[i]) {
                remaining -= values[i];
                sb.append(symbols[i]);
            }
        }
        return sb.toString();
    }
}
