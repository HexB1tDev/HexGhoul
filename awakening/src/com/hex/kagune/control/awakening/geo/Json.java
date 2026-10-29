package com.hex.kagune.control.awakening.geo;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Minimal JSON reader for the Blockbench model and animation files: objects become ordered maps,
 * arrays lists, numbers doubles. Strict enough to reject a damaged file instead of guessing.
 */
public final class Json {
    private final String text;
    private int at;

    private Json(String text) {
        this.text = text;
    }

    public static Object parse(String text) {
        Json json = new Json(text);
        json.space();
        Object value = json.value();
        json.space();
        if (json.at != text.length()) throw json.error("trailing characters");
        return value;
    }

    private IllegalArgumentException error(String message) {
        return new IllegalArgumentException("JSON " + message + " at character " + at);
    }

    private void space() {
        while (at < text.length()) {
            char c = text.charAt(at);
            if (c == ' ' || c == '\n' || c == '\r' || c == '\t') {
                at++;
            } else if (c == '/' && at + 1 < text.length() && text.charAt(at + 1) == '/') {
                // Blockbench never writes comments, but hand-edited files sometimes have them.
                while (at < text.length() && text.charAt(at) != '\n') at++;
            } else {
                return;
            }
        }
    }

    private Object value() {
        if (at >= text.length()) throw error("unexpected end");
        char c = text.charAt(at);
        return switch (c) {
            case '{' -> object();
            case '[' -> array();
            case '"' -> string();
            case 't' -> literal("true", Boolean.TRUE);
            case 'f' -> literal("false", Boolean.FALSE);
            case 'n' -> literal("null", null);
            default -> number();
        };
    }

    private Object literal(String word, Object value) {
        if (!text.startsWith(word, at)) throw error("unknown literal");
        at += word.length();
        return value;
    }

    private Map<String, Object> object() {
        Map<String, Object> map = new LinkedHashMap<>();
        at++;
        space();
        if (peek('}')) {
            at++;
            return map;
        }
        while (true) {
            space();
            if (!peek('"')) throw error("expected a key");
            String key = string();
            space();
            expect(':');
            space();
            map.put(key, value());
            space();
            if (peek(',')) {
                at++;
                continue;
            }
            expect('}');
            return map;
        }
    }

    private List<Object> array() {
        List<Object> list = new ArrayList<>();
        at++;
        space();
        if (peek(']')) {
            at++;
            return list;
        }
        while (true) {
            space();
            list.add(value());
            space();
            if (peek(',')) {
                at++;
                continue;
            }
            expect(']');
            return list;
        }
    }

    private String string() {
        at++;
        StringBuilder out = new StringBuilder();
        while (true) {
            if (at >= text.length()) throw error("unterminated string");
            char c = text.charAt(at++);
            if (c == '"') return out.toString();
            if (c != '\\') {
                out.append(c);
                continue;
            }
            if (at >= text.length()) throw error("unterminated escape");
            char e = text.charAt(at++);
            switch (e) {
                case '"', '\\', '/' -> out.append(e);
                case 'b' -> out.append('\b');
                case 'f' -> out.append('\f');
                case 'n' -> out.append('\n');
                case 'r' -> out.append('\r');
                case 't' -> out.append('\t');
                case 'u' -> {
                    if (at + 4 > text.length()) throw error("short unicode escape");
                    out.append((char) Integer.parseInt(text.substring(at, at + 4), 16));
                    at += 4;
                }
                default -> throw error("bad escape");
            }
        }
    }

    private Double number() {
        int start = at;
        if (peek('-')) at++;
        while (at < text.length() && "0123456789.eE+-".indexOf(text.charAt(at)) >= 0) at++;
        if (start == at) throw error("unexpected character '" + text.charAt(at) + "'");
        try {
            return Double.valueOf(text.substring(start, at));
        } catch (NumberFormatException bad) {
            throw error("bad number");
        }
    }

    private boolean peek(char c) {
        return at < text.length() && text.charAt(at) == c;
    }

    private void expect(char c) {
        if (!peek(c)) throw error("expected '" + c + "'");
        at++;
    }

    // ---- Typed access used by the model and animation readers -----------------------------------

    @SuppressWarnings("unchecked")
    static Map<String, Object> map(Object value, String what) {
        if (!(value instanceof Map)) throw new IllegalArgumentException(what + " must be an object");
        return (Map<String, Object>) value;
    }

    @SuppressWarnings("unchecked")
    static List<Object> list(Object value, String what) {
        if (!(value instanceof List)) throw new IllegalArgumentException(what + " must be an array");
        return (List<Object>) value;
    }

    static double number(Object value, String what) {
        if (!(value instanceof Double d)) throw new IllegalArgumentException(what + " must be a number");
        return d;
    }

    static double[] vector(Object value, int size, double[] fallback, String what) {
        if (value == null) return fallback;
        List<Object> list = list(value, what);
        if (list.size() != size) throw new IllegalArgumentException(what + " must have " + size + " numbers");
        double[] out = new double[size];
        for (int i = 0; i < size; i++) out[i] = number(list.get(i), what);
        return out;
    }
}
