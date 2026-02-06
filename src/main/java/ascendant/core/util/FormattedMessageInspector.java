package ascendant.core.util;

import com.hypixel.hytale.protocol.FormattedMessage;
import com.hypixel.hytale.protocol.ParamValue;

import javax.annotation.Nullable;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public final class FormattedMessageInspector {

    private FormattedMessageInspector() {
    }

    public static FormattedMessageParts inspect(@Nullable FormattedMessage msg) {
        if (msg == null) {
            return new FormattedMessageParts(null, null, List.of(), Map.of(), Map.of(), false, null);
        }

        List<FormattedMessageParts> children = new ArrayList<>();
        if (msg.children != null && msg.children.length > 0) {
            for (FormattedMessage child : msg.children) {
                children.add(inspect(child));
            }
        }

        Map<String, ParamDebugValue> params = new LinkedHashMap<>();
        if (msg.params != null && !msg.params.isEmpty()) {
            for (Map.Entry<String, ParamValue> e : msg.params.entrySet()) {
                ParamValue v = e.getValue();
                params.put(e.getKey(), ParamDebugValue.from(v));
            }
        }

        Map<String, FormattedMessageParts> messageParams = new LinkedHashMap<>();
        if (msg.messageParams != null && !msg.messageParams.isEmpty()) {
            for (Map.Entry<String, FormattedMessage> e : msg.messageParams.entrySet()) {
                messageParams.put(e.getKey(), inspect(e.getValue()));
            }
        }

        return new FormattedMessageParts(
                _emptyToNull(msg.rawText),
                _emptyToNull(msg.messageId),
                List.copyOf(children),
                Map.copyOf(params),
                Map.copyOf(messageParams),
                msg.markupEnabled,
                _emptyToNull(msg.color)
        );
    }

    public static String toDebugString(@Nullable FormattedMessage msg) {
        return inspect(msg).toDebugString();
    }

    private static String _emptyToNull(String s) {
        if (s == null) {
            return null;
        }
        String t = s.trim();
        return t.isEmpty() ? null : t;
    }

    public static long getLongParam(FormattedMessageParts parts, String key) {
        if (parts == null || parts.params() == null) {
            return 0L;
        }

        ParamDebugValue v = parts.params().get(key);
        if (v == null || v.value() == null) {
            return 0L;
        }

        try {
            return Long.parseLong(v.value());
        } catch (NumberFormatException ex) {
            return 0L;
        }
    }

    public record FormattedMessageParts(
            String rawText,
            String messageId,
            List<FormattedMessageParts> children,
            Map<String, ParamDebugValue> params,
            Map<String, FormattedMessageParts> messageParams,
            boolean markupEnabled,
            String color
    ) {
        private static void _append(StringBuilder out, FormattedMessageParts p, int depth) {
            String indent = "  ".repeat(Math.max(0, depth));
            out.append(indent).append("FormattedMessage{");
            if (p.messageId != null) {
                out.append(" messageId=").append(p.messageId);
            }
            if (p.rawText != null) {
                out.append(" rawText=").append(p.rawText);
            }
            if (p.color != null) {
                out.append(" color=").append(p.color);
            }
            out.append(" markupEnabled=").append(p.markupEnabled);

            if (p.params != null && !p.params.isEmpty()) {
                out.append("\n").append(indent).append("  params:");
                for (Map.Entry<String, ParamDebugValue> e : p.params.entrySet()) {
                    out.append("\n")
                            .append(indent).append("    ")
                            .append(e.getKey()).append(" = ")
                            .append(e.getValue().type);
                    if (e.getValue().value != null) {
                        out.append("(").append(e.getValue().value).append(")");
                    } else if (e.getValue().repr != null) {
                        out.append("[").append(e.getValue().repr).append("]");
                    }
                }
            }

            if (p.messageParams != null && !p.messageParams.isEmpty()) {
                out.append("\n").append(indent).append("  messageParams:");
                for (Map.Entry<String, FormattedMessageParts> e : p.messageParams.entrySet()) {
                    out.append("\n").append(indent).append("    ").append(e.getKey()).append(" ->\n");
                    _append(out, e.getValue(), depth + 3);
                }
            }

            if (p.children != null && !p.children.isEmpty()) {
                out.append("\n").append(indent).append("  children:");
                for (FormattedMessageParts c : p.children) {
                    out.append("\n");
                    _append(out, c, depth + 2);
                }
            }

            out.append("\n").append(indent).append("}");
        }

        public String toDebugString() {
            StringBuilder out = new StringBuilder(256);
            _append(out, this, 0);
            return out.toString();
        }
    }

    public record ParamDebugValue(
            String type,
            String value,
            String repr
    ) {
        public static ParamDebugValue from(@Nullable ParamValue v) {
            if (v == null) {
                return new ParamDebugValue("null", null, null);
            }

            String type = v.getClass().getSimpleName();
            String value = _extractCommonValue(v);
            String repr = value == null ? v.toString() : null;

            return new ParamDebugValue(type, value, repr);
        }

        private static String _extractCommonValue(ParamValue v) {
            try {
                var f = v.getClass().getDeclaredField("value");
                f.setAccessible(true);
                Object o = f.get(v);
                return o == null ? null : String.valueOf(o);
            } catch (Throwable ignored) {
                return null;
            }
        }
    }
}
