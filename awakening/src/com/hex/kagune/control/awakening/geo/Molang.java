package com.hex.kagune.control.awakening.geo;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

/**
 * The part of Molang that keyframes in Blockbench animations use: numbers, {@code query.anim_time},
 * {@code query.life_time}, {@code variable.<name>} supplied by the game, arithmetic, comparisons,
 * {@code ?:} and the {@code math.*} functions below. Like Bedrock and GeckoLib, trigonometry takes
 * degrees. Anything else is rejected when the file is read, so a typo cannot silently become 0.
 */
public final class Molang {
    /** What an expression can read while it is evaluated. */
    public interface Context {
        /** Seconds since the animation started. */
        double animTime();

        /** Seconds the animated thing has existed. */
        double lifeTime();

        /** A game-supplied value such as a charge level; 0 when unknown. */
        double variable(String name);
    }

    /** A compiled expression. */
    public interface Expr {
        double eval(Context context);

        /** True when the value never depends on the context, so it can be folded once. */
        default boolean constant() {
            return false;
        }
    }

    public static final Context NONE = new Context() {
        @Override
        public double animTime() {
            return 0.0;
        }

        @Override
        public double lifeTime() {
            return 0.0;
        }

        @Override
        public double variable(String name) {
            return 0.0;
        }
    };

    private record Constant(double value) implements Expr {
        @Override
        public double eval(Context context) {
            return value;
        }

        @Override
        public boolean constant() {
            return true;
        }
    }

    public static Expr constant(double value) {
        return new Constant(value);
    }

    private final String text;
    private int at;

    private Molang(String text) {
        this.text = text.toLowerCase(Locale.ROOT);
    }

    /** Compiles an expression; a trailing {@code ;} (as Blockbench sometimes writes) is allowed. */
    public static Expr compile(String source) {
        String trimmed = source.strip();
        if (trimmed.endsWith(";")) trimmed = trimmed.substring(0, trimmed.length() - 1);
        if (trimmed.isEmpty()) return new Constant(0.0);
        Molang parser = new Molang(trimmed);
        Expr expr = parser.ternary();
        parser.space();
        if (parser.at != parser.text.length()) throw parser.error("unexpected '" + parser.text.substring(parser.at) + "'");
        return fold(expr);
    }

    /** A keyframe component from JSON: a number or a Molang string. */
    static Expr of(Object value, String what) {
        if (value instanceof Double d) return new Constant(d);
        if (value instanceof String s) {
            try {
                return compile(s);
            } catch (IllegalArgumentException bad) {
                throw new IllegalArgumentException(what + ": " + bad.getMessage(), bad);
            }
        }
        throw new IllegalArgumentException(what + " must be a number or a Molang string");
    }

    private static Expr fold(Expr expr) {
        return expr.constant() && !(expr instanceof Constant) ? new Constant(expr.eval(NONE)) : expr;
    }

    private IllegalArgumentException error(String message) {
        return new IllegalArgumentException("Molang " + message + " in \"" + text + "\"");
    }

    private void space() {
        while (at < text.length() && Character.isWhitespace(text.charAt(at))) at++;
    }

    private boolean take(String token) {
        space();
        if (text.startsWith(token, at)) {
            at += token.length();
            return true;
        }
        return false;
    }

    private interface Binary {
        double apply(double a, double b);
    }

    private static Expr binary(Expr a, Expr b, Binary op) {
        Expr result = new Expr() {
            @Override
            public double eval(Context context) {
                return op.apply(a.eval(context), b.eval(context));
            }

            @Override
            public boolean constant() {
                return a.constant() && b.constant();
            }
        };
        return fold(result);
    }

    private static double truth(boolean value) {
        return value ? 1.0 : 0.0;
    }

    private Expr ternary() {
        Expr condition = or();
        if (take("?")) {
            Expr yes = ternary();
            if (!take(":")) throw error("expected ':'");
            Expr no = ternary();
            return fold(new Expr() {
                @Override
                public double eval(Context context) {
                    return condition.eval(context) != 0.0 ? yes.eval(context) : no.eval(context);
                }

                @Override
                public boolean constant() {
                    return condition.constant() && yes.constant() && no.constant();
                }
            });
        }
        return condition;
    }

    private Expr or() {
        Expr left = and();
        while (take("||")) {
            Expr right = and();
            left = binary(left, right, (a, b) -> truth(a != 0.0 || b != 0.0));
        }
        return left;
    }

    private Expr and() {
        Expr left = comparison();
        while (take("&&")) {
            Expr right = comparison();
            left = binary(left, right, (a, b) -> truth(a != 0.0 && b != 0.0));
        }
        return left;
    }

    private Expr comparison() {
        Expr left = sum();
        if (take("<=")) return binary(left, sum(), (a, b) -> truth(a <= b));
        if (take(">=")) return binary(left, sum(), (a, b) -> truth(a >= b));
        if (take("==")) return binary(left, sum(), (a, b) -> truth(a == b));
        if (take("!=")) return binary(left, sum(), (a, b) -> truth(a != b));
        if (take("<")) return binary(left, sum(), (a, b) -> truth(a < b));
        if (take(">")) return binary(left, sum(), (a, b) -> truth(a > b));
        return left;
    }

    private Expr sum() {
        Expr left = product();
        while (true) {
            if (take("+")) {
                left = binary(left, product(), Double::sum);
            } else if (take("-")) {
                left = binary(left, product(), (a, b) -> a - b);
            } else {
                return left;
            }
        }
    }

    private Expr product() {
        Expr left = unary();
        while (true) {
            if (take("*")) {
                left = binary(left, unary(), (a, b) -> a * b);
            } else if (take("/")) {
                left = binary(left, unary(), (a, b) -> b == 0.0 ? 0.0 : a / b);
            } else {
                return left;
            }
        }
    }

    private Expr unary() {
        if (take("-")) {
            Expr inner = unary();
            return binary(new Constant(0.0), inner, (a, b) -> -b);
        }
        if (take("!")) {
            Expr inner = unary();
            return binary(new Constant(0.0), inner, (a, b) -> truth(b == 0.0));
        }
        return primary();
    }

    private Expr primary() {
        space();
        if (at >= text.length()) throw error("unexpected end");
        char c = text.charAt(at);
        if (c == '(') {
            at++;
            Expr inner = ternary();
            if (!take(")")) throw error("expected ')'");
            return inner;
        }
        if (Character.isDigit(c) || c == '.') {
            int start = at;
            while (at < text.length() && (Character.isDigit(text.charAt(at)) || text.charAt(at) == '.')) at++;
            try {
                return new Constant(Double.parseDouble(text.substring(start, at)));
            } catch (NumberFormatException bad) {
                throw error("bad number");
            }
        }
        if (Character.isLetter(c)) {
            int start = at;
            while (at < text.length() && (Character.isLetterOrDigit(text.charAt(at)) || text.charAt(at) == '_' || text.charAt(at) == '.')) at++;
            String name = text.substring(start, at);
            if (take("(")) {
                List<Expr> args = new ArrayList<>();
                if (!take(")")) {
                    do {
                        args.add(ternary());
                    } while (take(","));
                    if (!take(")")) throw error("expected ')'");
                }
                return function(name, args);
            }
            return name(name);
        }
        throw error("unexpected '" + c + "'");
    }

    private Expr name(String name) {
        switch (name) {
            case "math.pi":
                return new Constant(Math.PI);
            case "query.anim_time":
            case "q.anim_time":
                return Context::animTime;
            case "query.life_time":
            case "q.life_time":
                return Context::lifeTime;
            default:
                break;
        }
        String variable = name.startsWith("variable.") ? name.substring(9) : name.startsWith("v.") ? name.substring(2) : null;
        if (variable == null || variable.isEmpty()) throw error("unknown name '" + name + "'");
        return context -> context.variable(variable);
    }

    private Expr function(String name, List<Expr> args) {
        int count = switch (name) {
            case "math.sin", "math.cos", "math.abs", "math.sqrt", "math.floor", "math.ceil", "math.round", "math.exp" -> 1;
            case "math.min", "math.max", "math.mod", "math.pow", "math.atan2" -> 2;
            case "math.clamp", "math.lerp" -> 3;
            default -> throw error("unknown function '" + name + "'");
        };
        if (args.size() != count) throw error(name + " takes " + count + " arguments");
        Expr a = args.get(0);
        Expr b = count > 1 ? args.get(1) : null;
        Expr c = count > 2 ? args.get(2) : null;
        Expr result = switch (name) {
            case "math.sin" -> context -> Math.sin(Math.toRadians(a.eval(context)));
            case "math.cos" -> context -> Math.cos(Math.toRadians(a.eval(context)));
            case "math.abs" -> context -> Math.abs(a.eval(context));
            case "math.sqrt" -> context -> Math.sqrt(Math.max(0.0, a.eval(context)));
            case "math.floor" -> context -> Math.floor(a.eval(context));
            case "math.ceil" -> context -> Math.ceil(a.eval(context));
            case "math.round" -> context -> Math.round(a.eval(context));
            case "math.exp" -> context -> Math.exp(a.eval(context));
            case "math.min" -> context -> Math.min(a.eval(context), b.eval(context));
            case "math.max" -> context -> Math.max(a.eval(context), b.eval(context));
            case "math.mod" -> context -> {
                double d = b.eval(context);
                return d == 0.0 ? 0.0 : a.eval(context) % d;
            };
            case "math.pow" -> context -> Math.pow(a.eval(context), b.eval(context));
            case "math.atan2" -> context -> Math.toDegrees(Math.atan2(a.eval(context), b.eval(context)));
            case "math.clamp" -> context -> Math.max(b.eval(context), Math.min(c.eval(context), a.eval(context)));
            default -> context -> {
                double t = c.eval(context);
                return a.eval(context) + (b.eval(context) - a.eval(context)) * t;
            };
        };
        boolean constant = args.stream().allMatch(Expr::constant);
        return constant ? new Constant(result.eval(NONE)) : result;
    }
}
