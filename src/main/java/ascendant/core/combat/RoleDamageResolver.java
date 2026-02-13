package ascendant.core.combat;

import ascendant.core.util.Logging;
import com.hypixel.hytale.server.core.modules.interaction.interaction.config.Interaction;
import com.hypixel.hytale.server.core.modules.interaction.interaction.config.server.combat.DamageCalculator;
import com.hypixel.hytale.server.core.modules.entity.damage.DamageCause;
import it.unimi.dsi.fastutil.objects.Object2FloatMap;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.lang.reflect.Field;
import java.util.Map;

@Deprecated
public final class RoleDamageResolver {
    private RoleDamageResolver() {}

    public static float resolveConfiguredDamageFromInteractionVars(@Nullable Map<String, String> _interactionVars) {
        if (_interactionVars == null || _interactionVars.isEmpty()) {
            return 0.0f;
        }

        float _sum = 0.0f;

        for (Map.Entry<String, String> e : _interactionVars.entrySet()) {
            String _varKey = e.getKey();
            String _assetKey = _normalizeAssetKey(e.getValue());
            if (_assetKey == null) {
                continue;
            }

            // Heuristic: we care primarily about damage interactions
            if (!_looksLikeDamageVar(_varKey, _assetKey)) {
                continue;
            }

            Interaction _interaction = (Interaction) Interaction.getAssetMap().getAsset(_assetKey);
            if (_interaction == null) {
                continue;
            }

            DamageCalculator _dc = _extractDamageCalculator(_interaction);
            if (_dc == null) {
                continue;
            }

            Object2FloatMap<DamageCause> _damageMap = _dc.calculateDamage(10.0);
            if (_damageMap == null || _damageMap.isEmpty()) {
                continue;
            }

            float _local = 0.0f;
            for (Object2FloatMap.Entry<DamageCause> de : _damageMap.object2FloatEntrySet()) {
                _local += de.getFloatValue();
            }

            _sum += _local;
        }

        return _sum;
    }

    private static boolean _looksLikeDamageVar(@Nonnull String _varKey, @Nonnull String _assetKey) {
        String k = _varKey.toLowerCase();
        String a = _assetKey.toLowerCase();
        return k.contains("damage") || a.contains("damage");
    }

    @Nullable
    private static String _normalizeAssetKey(@Nullable String _raw) {
        if (_raw == null || _raw.isBlank()) {
            return null;
        }
        String s = _raw.trim();
        if (s.startsWith("*")) {
            s = s.substring(1);
        }
        return s.isBlank() ? null : s;
    }

    @Nullable
    private static DamageCalculator _extractDamageCalculator(@Nonnull Interaction _interaction) {
        // Try common names first
        DamageCalculator _dc = _getField(_interaction, "damageCalculator", DamageCalculator.class);
        if (_dc != null) return _dc;

        _dc = _getField(_interaction, "calculator", DamageCalculator.class);
        if (_dc != null) return _dc;

        _dc = _getField(_interaction, "DamageCalculator", DamageCalculator.class);
        if (_dc != null) return _dc;

        // Fallback: scan all declared fields once
        for (Field f : _interaction.getClass().getDeclaredFields()) {
            if (DamageCalculator.class.isAssignableFrom(f.getType())) {
                try {
                    f.setAccessible(true);
                    Object v = f.get(_interaction);
                    if (v instanceof DamageCalculator dc) {
                        return dc;
                    }
                } catch (IllegalAccessException ignored) {
                    return null;
                }
            }
        }

        return null;
    }

    @Nullable
    private static <T> T _getField(@Nonnull Object _obj, @Nonnull String _name, @Nonnull Class<T> _type) {
        Class<?> c = _obj.getClass();
        while (c != null && c != Object.class) {
            try {
                Field f = c.getDeclaredField(_name);
                if (!_type.isAssignableFrom(f.getType())) {
                    return null;
                }
                f.setAccessible(true);
                Object v = f.get(_obj);
                return _type.cast(v);
            } catch (NoSuchFieldException ignored) {
                c = c.getSuperclass();
            } catch (IllegalAccessException ignored) {
                return null;
            }
        }
        return null;
    }
}
