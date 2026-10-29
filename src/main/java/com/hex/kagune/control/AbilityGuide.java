package com.hex.kagune.control;

import com.hex.kagune.control.ukaku.UkakuRules;
import com.hex.kagune.control.ukaku.WingForm;
import com.hex.kagune.control.graft.GraftMove;
import com.hex.kagune.control.graft.GraftStage;
import com.hex.kagune.gameplay.KaguneForm;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * What the abilities panel shows (3.1.0, pure, tested): the moves the ghoul has right now with the keys the player bound to
 * them, how to open the eyes and unleash the kagune (R, Ctrl + R), and how to go into a kakuja (K). Names and colours follow the
 * path and the form: Rinkaku's combo is named for his form, crimson in the rinkaku forms, plum in the kakuja; Ukaku's moves are
 * fire in the wings (vermilion), crystal in the crystal mode (ice), needles in the first kakuja (blood red), needles and fire in
 * the final kakuja (ember). A move his mastery hasn't reached yet is shown dim with the mastery it needs.
 */
public final class AbilityGuide {
    /** The keys a row can name (the panel's owner turns each into the player's bound key). */
    public enum Key {
        ATTACK, USE, TOGGLE, MENU, GRAB, THROW, RAPID, SLINGSHOT, CYCLE, CRUSH, LIFT, SLAM, GUARD, UNLEASH, QUESTS
    }

    /** Where the ghoul is: kagune in, the kakugan open (ghoul eyes), or the kagune out. */
    public enum State {
        CLOSED, EYES, OUT
    }

    /** One line: the keys (already the player's names for them), the move, its colour, and the mastery it needs (0 = has it). */
    public record Row(String keys, String name, int colour, int needs) {
        public boolean locked() {
            return this.needs > 0;
        }
    }

    /** The whole panel: its title and subtitle, the form's accent colour, and its lines. */
    public record Panel(String title, String subtitle, int accent, List<Row> rows) {
    }

    /**
     * What to show for.
     *
     * @param path       the ghoul's path
     * @param state      kagune in, eyes open, or kagune out
     * @param mastery    kagune mastery
     * @param rinkaku     Rinkaku's form (ignored on Ukaku's path)
     * @param ukaku      Ukaku's kagune form: his wings, or a kakuja (ignored on Rinkaku's path)
     * @param crystal    Ukaku's wings crystallized
     * @param keys       the player's name for each key ("R", "LMB", "Ctrl+G")
     */
    public record Input(KagunePath path, State state, int mastery, KaguneForm rinkaku, WingForm ukaku, boolean crystal, Map<Key, String> keys,
                        boolean blade, GraftStage graft, boolean graftKakuja, java.util.Set<com.hex.kagune.control.quest.Gate> shut) {
        public Input {
            shut = shut == null ? java.util.Set.of() : java.util.Set.copyOf(shut);
        }

        /** 7.0.1: as below, every gate open (a preview, a test). */
        public Input(KagunePath path, State state, int mastery, KaguneForm rinkaku, WingForm ukaku, boolean crystal, Map<Key, String> keys,
                     boolean blade, GraftStage graft, boolean graftKakuja) {
            this(path, state, mastery, rinkaku, ukaku, crystal, keys, blade, graft, graftKakuja, java.util.Set.of());
        }

        public Input(KagunePath path, State state, int mastery, KaguneForm rinkaku, WingForm ukaku, boolean crystal, Map<Key, String> keys,
                     boolean blade) {
            this(path, state, mastery, rinkaku, ukaku, crystal, keys, blade, GraftStage.TRANSPLANT, false);
        }

        public Input(KagunePath path, State state, int mastery, KaguneForm rinkaku, WingForm ukaku, boolean crystal, Map<Key, String> keys) {
            this(path, state, mastery, rinkaku, ukaku, crystal, keys, false);
        }
    }

    // The colours.
    public static final int TEXT = 0xFFEDE3D6;
    public static final int DIM = 0xFF7D7480;
    static final int EYES = 0xFFFF3347;
    static final int CLOSED = 0xFFB9AFC0;
    static final int RINKAKU = 0xFFE5394F;
    static final int KAKUJA_PLUM = 0xFFB27FD6;
    static final int KAKUJA_RED = 0xFFF2493A;
    static final int FIRE = 0xFFFF8040;
    static final int CRYSTAL = 0xFF86DCFF;
    static final int NEEDLE = 0xFFE8383F;
    static final int EMBER = 0xFFFF6A2A;
    static final int UNDECIDED = 0xFFC9C0B4;
    /** 6.1.0: Pale's pale flesh, and the red of her one eye. */
    static final int PALE_FLESH = 0xFFE9BFAE;
    static final int PALE_EYE = 0xFFFF4A5A;

    /** 7.0.0: Graft DNA's crimson, and the kakuja's burning slashes. */
    static final int GRAFT_RED = 0xFFD8283A;
    static final int GRAFT_BURN = 0xFFFF5A3C;

    /** The kakuja need these. */
    public static final int FIRST_KAKUJA = 750;
    public static final int FINAL_KAKUJA = 1000;

    private AbilityGuide() {
    }

    public static Panel panel(Input in) {
        List<Row> rows = new ArrayList<>();
        if (in.path() == KagunePath.NONE) {
            rows.add(row(in, "Choose your kagune path", UNDECIDED, 0, Key.MENU));
            return new Panel("KAGUNE", "Undecided", UNDECIDED, rows);
        }
        if (in.state() != State.OUT) {
            boolean eyes = in.state() == State.EYES;
            rows.add(row(in, eyes ? "Close the kakugan" : "Ghoul eyes (kakugan)", EYES, 0, Key.TOGGLE));
            if (eyes) {
                // 6.1.0: the eyes' own form (GhoulBuffs), paid for in RC.
                rows.add(new Row("eyes", "Resistance I · Strength I · drains RC", EYES, 0));
                // 7.0.1: the shared eye-mode fight (EyeRules): Owl DNA's body, whatever the path.
                int m = in.mastery();
                rows.add(row(in, "Speed Blitz (" + com.hex.kagune.control.pale.PaleStrike.count(m) + " strikes)", EYES, 0, Key.ATTACK));
                rows.add(row(in, "Flips (W A S D)", TEXT, 0, Key.USE));
                rows.add(new Row(key(in, Key.RAPID) + " hold", "Big Punch!", EYES, 0));
                // 7.0.1: past its mastery it may still wait for its quest.
                boolean quest = in.shut().contains(com.hex.kagune.control.quest.Gate.EYES_KICK) && m >= com.hex.kagune.control.pale.PaleRules.KICK_MASTERY;
                rows.add(row(in, quest ? "Speed Kick (needs its quest)" : "Speed Kick", quest ? DIM : TEXT,
                    locked(m, com.hex.kagune.control.pale.PaleRules.KICK_MASTERY), Key.LIFT));
            }
            rows.add(new Row(key(in, Key.UNLEASH) + "+" + key(in, Key.TOGGLE), "Unleash the kagune", accent(in), 0));
            rows.add(row(in, "Kagune menu", CLOSED, 0, Key.MENU));
            rows.add(row(in, "Quests", CLOSED, 0, Key.QUESTS)); // 7.0.1
            String title = switch (in.path()) {
                case UKAKU -> "UKAKU";
                case PALE -> "ONE-EYED OWL";
                case GRAFT -> "GRAFT DNA";
                default -> "RINKAKU";
            };
            return new Panel(title, eyes ? "Ghoul eyes open" : "Kagune sheathed", eyes ? EYES : CLOSED, rows);
        }
        return switch (in.path()) {
            case UKAKU -> ukaku(in, rows);
            case PALE -> pale(in, rows);
            case GRAFT -> graft(in, rows);
            default -> rinkaku(in, rows);
        };
    }

    /** The accent of the form the ghoul would be in. */
    static int accent(Input in) {
        if (in.path() == KagunePath.PALE) {
            return PALE_FLESH;
        }
        if (in.path() == KagunePath.GRAFT) {
            return in.graftKakuja() ? GRAFT_BURN : GRAFT_RED;
        }
        if (in.path() == KagunePath.UKAKU) {
            WingForm form = in.ukaku();
            return form.finalKakuja() ? EMBER : form.kakuja() ? NEEDLE : in.crystal() ? CRYSTAL : FIRE;
        }
        return in.rinkaku().isKakuja() ? KAKUJA_PLUM : RINKAKU;
    }

    static Panel rinkaku(Input in, List<Row> rows) {
        KaguneForm form = in.rinkaku();
        int accent = accent(in);
        boolean kakuja = form.isKakuja();
        boolean full = form == KaguneForm.FULL_KAKUJA;
        if (RinkakuForms.autonomous(form)) {
            // The first kakuja's legs fight on their own; he can only walk (5.0.10). K and R still take him out of it.
            rows.add(new Row("auto", "Centipede legs fight on their own", KAKUJA_PLUM, 0));
            rows.add(new Row("move", "You can only walk", TEXT, 0));
        } else {
            rows.add(row(in, combo(form), full ? KAKUJA_RED : accent, 0, Key.ATTACK));
            // 6.0.4: each move unlocks with mastery (RinkakuUnlocks); a locked one shows dim with the mastery it needs.
            int m = in.mastery();
            rows.add(row(in, kakuja ? "Rapid Strike (legs and tails)" : "Rapid Strike", TEXT, locked(m, RinkakuUnlocks.RAPID_STRIKE), Key.RAPID));
            rows.add(row(in, "Grapple leap", TEXT, locked(m, RinkakuUnlocks.GRAPPLE), Key.SLINGSHOT));
            rows.add(row(in, "Grab · Throw", TEXT, locked(m, RinkakuUnlocks.GRAB), Key.GRAB, Key.THROW));
            rows.add(row(in, "Crush", TEXT, locked(m, RinkakuUnlocks.CRUSH), Key.CRUSH));
            rows.add(row(in, "Lift", TEXT, locked(m, RinkakuUnlocks.LIFT), Key.LIFT));
            rows.add(row(in, "Slam", TEXT, locked(m, RinkakuUnlocks.SLAM), Key.SLAM));
            rows.add(row(in, "Guard", TEXT, 0, Key.GUARD));
            if (form.hasCentipedeLegs()) {
                rows.add(new Row("auto", "Centipede legs hunt and grab", KAKUJA_PLUM, 0));
            }
            if (full) {
                // 6.0.0: the kakuja's passive and dash.
                rows.add(new Row(key(in, Key.USE), "Dash (S held: back)", KAKUJA_RED, 0));
                rows.add(new Row("sprint", "Vanish 5 s (30 s cooldown)", KAKUJA_PLUM, 0));
            }
        }
        transform(in, rows, kakuja, full, accent);
        String title = switch (form) {
            case RINKAKU_SEED -> "RINKAKU SEED";
            case RINKAKU_DUAL -> "TWIN RINKAKU";
            case RINKAKU_QUAD -> "FOUR-TAIL RINKAKU";
            case CENTIPEDE -> "CENTIPEDE KAKUJA";
            case FULL_KAKUJA -> "FULL KAKUJA";
            default -> form.displayName().toUpperCase(java.util.Locale.ROOT);
        };
        return new Panel(title, kakuja ? (full ? "Rinkaku · final kakuja" : "Rinkaku · first kakuja") : "Rinkaku · kagune out", accent, rows);
    }

    /** Rinkaku's M1 for his form (ComboStyle's stages). */
    static String combo(KaguneForm form) {
        return switch (form) {
            case RINKAKU_SEED -> "Whip: Lash · Whip Crack";
            case RINKAKU_DUAL -> "Scissor: Lashes · Scissor";
            case RINKAKU_QUAD -> "Flurry: Jabs · Sweep · Impale";
            case CENTIPEDE -> "Heavy: Crash · Scythe · Drill";
            case FULL_KAKUJA -> "Predator: Cleave · Devour";
            default -> "Blade: Slashes · Whirlwind";
        };
    }

    static Panel ukaku(Input in, List<Row> rows) {
        WingForm form = in.ukaku();
        int m = in.mastery();
        int accent = accent(in);
        String title;
        String subtitle;
        if (form.finalKakuja() && in.blade()) {
            // 6.0.0: the Steel Blade out.
            rows.add(row(in, "Steel Blade: Sweep · Return · Execution", EMBER, 0, Key.ATTACK));
            rows.add(new Row(key(in, Key.USE), "Dash (S held: back)", EMBER, 0));
            rows.add(new Row("sprint", "Vanish 5 s (30 s cooldown)", TEXT, 0));
            rows.add(new Row(key(in, Key.SLINGSHOT), "Fireballs", TEXT, 0));
            rows.add(row(in, "Open the wings", TEXT, 0, Key.GUARD));
            title = "STEEL BLADE";
            subtitle = "Ukaku · final kakuja";
        } else if (form.finalKakuja()) {
            rows.add(row(in, "Needles + fire (bleed, burn)", EMBER, 0, Key.ATTACK));
            rows.add(row(in, "Pyre enders", EMBER, 0, Key.USE));
            rows.add(row(in, "Fire cyclone", TEXT, 0, Key.RAPID));
            rows.add(new Row(key(in, Key.SLINGSHOT) + " hold", "Pyre Gatling", TEXT, 0));
            rows.add(row(in, "Steel Blade", TEXT, 0, Key.GUARD));
            title = "FINAL KAKUJA";
            subtitle = "Ukaku · needles and fire";
        } else if (form.kakuja()) {
            rows.add(row(in, "Needle Storm", NEEDLE, 0, Key.ATTACK));
            rows.add(row(in, "Needle enders", NEEDLE, 0, Key.USE));
            rows.add(row(in, "Rushing Death", TEXT, locked(m, UkakuRules.RUSH_MASTERY), Key.RAPID));
            rows.add(new Row(key(in, Key.SLINGSHOT) + " hold", "Needle Gatling", TEXT, locked(m, UkakuRules.NEEDLE_GATLING_MASTERY)));
            title = "FIRST KAKUJA";
            subtitle = "Ukaku · six tentacles";
        } else if (in.crystal()) {
            rows.add(row(in, "Crystal spikes (bleed)", CRYSTAL, 0, Key.ATTACK));
            rows.add(row(in, "Crystal enders", CRYSTAL, 0, Key.USE));
            rows.add(new Row(key(in, Key.SLINGSHOT) + " hold", "Gatling", TEXT, locked(m, UkakuRules.GATLING_MASTERY)));
            // 5.0.9: no Wing Vortex here: it is a fire move.
            rows.add(row(in, "Thaw the wings", CRYSTAL, 0, Key.LIFT));
            title = "CRYSTAL WINGS";
            subtitle = "Ukaku · crystal mode";
        } else {
            rows.add(row(in, "Fire shots", FIRE, 0, Key.ATTACK));
            rows.add(row(in, "Fire enders", FIRE, 0, Key.USE));
            rows.add(row(in, "Wing Vortex", TEXT, locked(m, UkakuRules.VORTEX_MASTERY), Key.RAPID));
            rows.add(row(in, "Crystal mode", CRYSTAL, locked(m, UkakuRules.CRYSTAL_MASTERY), Key.LIFT));
            title = switch (form) {
                // The server sends RABBIT for a new Ukaku's one wing (WingForm.shown).
                case RABBIT -> m < WingForm.TWIN.requiredMastery() ? "ONE WING" : "RABBIT GUISE";
                case ONE_WING -> "ONE-WINGED KAGUNE";
                default -> m < WingForm.TWIN.requiredMastery() ? "ONE WING" : "TWIN WINGS";
            };
            subtitle = "Ukaku · fire";
        }
        transform(in, rows, form.kakuja(), form.finalKakuja(), accent);
        return new Panel(title, subtitle, accent, rows);
    }

    /**
     * 6.1.0: Pale's base form: her fists, flips and Big Punch from the start, the pale flesh's sweep, grab and hammer and the speed
     * kick as her mastery reaches them, and her kakuja, the Owl.
     */
    static Panel pale(Input in, List<Row> rows) {
        int m = in.mastery();
        rows.add(row(in, "Speed Blitz (" + com.hex.kagune.control.pale.PaleStrike.count(m) + " strikes)", PALE_FLESH, 0, Key.ATTACK));
        rows.add(row(in, "Flips (W A S D)", TEXT, 0, Key.USE));
        rows.add(new Row(key(in, Key.RAPID) + " hold", "Big Punch!", PALE_EYE, 0));
        rows.add(new Row(key(in, Key.SLINGSHOT) + " tap", "Flesh Swipe", PALE_FLESH, locked(m, com.hex.kagune.control.pale.PaleRules.SWIPE_MASTERY)));
        rows.add(new Row(key(in, Key.SLINGSHOT) + " hold", "Flesh Grab · Throw", PALE_FLESH,
            locked(m, com.hex.kagune.control.pale.PaleRules.GRAB_MASTERY)));
        rows.add(new Row(key(in, Key.LIFT) + " tap", "Speed Kick", TEXT, locked(m, com.hex.kagune.control.pale.PaleRules.KICK_MASTERY)));
        rows.add(new Row(key(in, Key.LIFT) + " hold", "Flesh Hammer", PALE_FLESH, locked(m, com.hex.kagune.control.pale.PaleRules.HAMMER_MASTERY)));
        rows.add(new Row(key(in, Key.SLAM), "Watch This", PALE_FLESH, locked(m, com.hex.kagune.control.pale.PaleRules.WATCH_MASTERY)));
        rows.add(new Row(key(in, Key.THROW) + " tap · hold", "Mutated Arm · Sphere", PALE_FLESH, locked(m, com.hex.kagune.control.pale.ArmRules.MASTERY)));
        rows.add(new Row(key(in, Key.CYCLE) + " tap", "The Owl (kakuja)", PALE_FLESH, locked(m, com.hex.kagune.control.pale.OwlRules.MASTERY)));
        away(in, rows);
        return new Panel("BASE FORM", "Owl DNA · one-eyed owl", PALE_FLESH, rows);
    }

    /** 1.0.22: the final kakujas' colours: War's crimson, Peace's white and the red of its last one. */
    static final int WAR_RED = 0xFFE33A4A;
    static final int PEACE_WHITE = 0xFFE4EAF2;
    static final int PEACE_RED = 0xFFFF4A5A;

    /** The keys of the final kakujas' six abilities, in the order of their {@code Ability} enums ({@code KingInput}). */
    static final Key[] KING_KEYS = {Key.RAPID, Key.SLINGSHOT, Key.GRAB, Key.SLAM, Key.GUARD, Key.LIFT};
    /** Where a final kakuja's abilities start among its rows (after M1, M1 in the air and the heavy). */
    public static final int KING_FIRST = 3;

    /**
     * 1.0.22: War's or Peace's panel ({@code peace}), the same card as every other form's: the fighting grammar ({@code KingCombo}),
     * the six abilities on the keys the player bound (rows {@link #KING_FIRST} on, in their enum's order), and K or R to turn back.
     */
    public static Panel king(Map<Key, String> keys, boolean peace) {
        Input in = new Input(KagunePath.RINKAKU, State.OUT, FINAL_KAKUJA, KaguneForm.FULL_KAKUJA, WingForm.TWIN, false, keys);
        int accent = peace ? PEACE_WHITE : WAR_RED;
        List<Row> rows = new ArrayList<>();
        rows.add(row(in, (peace ? "Blades" : "Fists") + ": chain · Space held: launch", accent, 0, Key.ATTACK));
        rows.add(new Row(key(in, Key.ATTACK) + " air", "Juggle, ends in a spike", accent, 0));
        rows.add(row(in, "Heavy · hold: charge · after a blow: ender", accent, 0, Key.USE));
        int count = peace ? com.hex.kagune.control.king.PeaceRules.Ability.values().length
            : com.hex.kagune.control.king.WarRules.Ability.values().length;
        for (int i = 0; i < count; i++) {
            String name = peace ? com.hex.kagune.control.king.PeaceRules.Ability.values()[i].displayName()
                : com.hex.kagune.control.king.WarRules.Ability.values()[i].displayName();
            boolean last = i == count - 1;
            rows.add(row(in, name, last ? (peace ? PEACE_RED : WAR_RED) : TEXT, 0, KING_KEYS[i]));
        }
        rows.add(new Row(key(in, Key.CYCLE) + " tap", "Turn back", accent, 0));
        away(in, rows);
        return new Panel(peace ? "PEACE" : "WAR", peace ? "Rinkaku · final kakuja · white wings" : "Rinkaku · final kakuja · One-Eyed King",
            accent, rows);
    }

    /** The original's keys each of Graft's move slots is on. */
    private static final Object[][] GRAFT_KEYS = {{GraftMove.Slot.Z, Key.RAPID}, {GraftMove.Slot.J, Key.SLINGSHOT}, {GraftMove.Slot.V, Key.LIFT},
        {GraftMove.Slot.B, Key.SLAM}, {GraftMove.Slot.N, Key.THROW}, {GraftMove.Slot.H, Key.CRUSH}, {GraftMove.Slot.G, Key.GRAB},
        {GraftMove.Slot.C, Key.GUARD}};

    /**
     * 7.0.0: Graft DNA with the kagune out: the stage's string, the dash, its moves on their keys (dim until the mastery reaches
     * them), K for the kakuja (tap) or the next stage (hold).
     */
    static Panel graft(Input in, List<Row> rows) {
        int m = in.mastery();
        boolean kakuja = in.graftKakuja();
        GraftStage stage = kakuja ? GraftStage.KAKUJA : in.graft();
        int accent = accent(in);
        GraftMove first = GraftMove.firstStrike(stage);
        rows.add(row(in, first.displayName() + " string", accent, locked(m, first.mastery()), Key.ATTACK));
        rows.add(row(in, "Kagune Step (W A S D)", TEXT, 0, Key.USE));
        for (Object[] entry : GRAFT_KEYS) {
            GraftMove move = GraftMove.of((GraftMove.Slot)entry[0], stage);
            if (move == GraftMove.NONE) {
                continue;
            }
            String name = move.hint().isEmpty() ? move.displayName() : move.displayName() + " · " + move.hint();
            rows.add(row(in, name, move.mastery() >= GraftStage.KAKUJA.mastery() ? GRAFT_BURN : TEXT, locked(m, move.mastery()), (Key)entry[1]));
        }
        if (kakuja) {
            rows.add(new Row(key(in, Key.CYCLE) + " tap", "Turn back", accent, 0));
        } else {
            rows.add(new Row(key(in, Key.CYCLE) + " tap", "Kakuja", GRAFT_BURN, locked(m, GraftStage.KAKUJA.mastery())));
            rows.add(new Row(key(in, Key.CYCLE) + " hold", "Next stage", accent, 0));
        }
        away(in, rows);
        return new Panel(kakuja ? "KAKUJA" : stage.displayName().toUpperCase(java.util.Locale.ROOT), "Graft DNA · right kakugan", accent, rows);
    }

    /** K into and out of the kakuja (tap: the first, hold: the final, from the first too), R to kakugan mode, Ctrl + R all away. */
    static void transform(Input in, List<Row> rows, boolean kakuja, boolean finalForm, int accent) {
        boolean ukaku = in.path() == KagunePath.UKAKU;
        if (kakuja) {
            rows.add(new Row(key(in, Key.CYCLE) + " tap", "Turn back", accent, 0));
        } else {
            rows.add(new Row(key(in, Key.CYCLE) + " tap", "First kakuja", ukaku ? NEEDLE : KAKUJA_PLUM, locked(in.mastery(), FIRST_KAKUJA)));
        }
        if (!finalForm) {
            rows.add(new Row(key(in, Key.CYCLE) + " hold", "Final kakuja", ukaku ? EMBER : KAKUJA_RED, locked(in.mastery(), FINAL_KAKUJA)));
        }
        away(in, rows);
    }

    /** 1.2.0: the kagune out: R back to kakugan mode (the kagune in, the eyes kept), Ctrl + R everything away. */
    static void away(Input in, List<Row> rows) {
        rows.add(row(in, "Kakugan mode (eyes stay open)", EYES, 0, Key.TOGGLE));
        rows.add(new Row(key(in, Key.UNLEASH) + "+" + key(in, Key.TOGGLE), "Put it all away", DIM, 0));
    }

    static int locked(int mastery, int needs) {
        return mastery >= needs ? 0 : needs;
    }

    static Row row(Input in, String name, int colour, int needs, Key... keys) {
        StringBuilder text = new StringBuilder();
        for (Key each : keys) {
            if (text.length() > 0) {
                text.append('·');
            }
            text.append(key(in, each));
        }
        return new Row(text.toString(), name, colour, needs);
    }

    static String key(Input in, Key key) {
        String name = in.keys() == null ? null : in.keys().get(key);
        return name == null || name.isEmpty() ? "?" : name;
    }

    /**
     * A key's short name from Minecraft's saved name ({@code key.keyboard.r}, {@code key.mouse.left}) and its Forge modifier
     * ({@code CONTROL}, {@code SHIFT}, {@code ALT}, {@code NONE}): "R", "LMB", "Ctrl+G". Unbound is "-".
     */
    public static String keyName(String saved, String modifier) {
        String name = shortName(saved);
        String prefix = switch (modifier == null ? "NONE" : modifier) {
            case "CONTROL" -> "Ctrl+";
            case "SHIFT" -> "Shift+";
            case "ALT" -> "Alt+";
            default -> "";
        };
        return name.equals("-") ? name : prefix + name;
    }

    static String shortName(String saved) {
        if (saved == null || saved.isEmpty() || saved.equals("key.keyboard.unknown")) {
            return "-";
        }
        if (saved.startsWith("key.mouse.")) {
            String button = saved.substring("key.mouse.".length());
            return switch (button) {
                case "left" -> "LMB";
                case "right" -> "RMB";
                case "middle" -> "MMB";
                default -> "M" + button;
            };
        }
        String key = saved.startsWith("key.keyboard.") ? saved.substring("key.keyboard.".length()) : saved;
        return switch (key) {
            case "left.shift" -> "LShift";
            case "right.shift" -> "RShift";
            case "left.control" -> "LCtrl";
            case "right.control" -> "RCtrl";
            case "left.alt" -> "LAlt";
            case "right.alt" -> "RAlt";
            case "space" -> "Space";
            case "tab" -> "Tab";
            case "caps.lock" -> "Caps";
            case "enter" -> "Enter";
            case "backspace" -> "Bksp";
            case "grave.accent" -> "`";
            default -> {
                if (key.startsWith("keypad.")) {
                    yield "Num" + key.substring("keypad.".length());
                }
                yield key.length() <= 2 ? key.toUpperCase(java.util.Locale.ROOT) : Character.toUpperCase(key.charAt(0)) + key.substring(1);
            }
        };
    }
}
