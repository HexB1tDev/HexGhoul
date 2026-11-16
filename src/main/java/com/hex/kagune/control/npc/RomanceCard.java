package com.hex.kagune.control.npc;

import java.util.ArrayList;
import java.util.List;

/**
 * 1.3.29: how the player stands with one companion, for the quest journal's Romance tab (pure, tested; made on the server from the world's
 * romance, the player's bond and their family, and sent with the quest log): met or not, the stage (with someone else, friends, dating,
 * married), the regard, the courtship before they may ask (2.1.2: its chapter, {@link Courtship}), days together, the home, the children, a
 * child on the way, and what to do next.
 */
public record RomanceCard(String key, boolean met, Romance.Stage stage, int regard, boolean likes, String claimedBy, EtoRules.Step step, int done,
                          long days, int children, boolean home, boolean expecting, int chapter) {
    /** As 1.3.29 made one: no courtship chapter closed (2.1.2: {@code chapter}, how many are). */
    public RomanceCard(String key, boolean met, Romance.Stage stage, int regard, boolean likes, String claimedBy, EtoRules.Step step, int done,
                       long days, int children, boolean home, boolean expecting) {
        this(key, met, stage, regard, likes, claimedBy, step, done, days, children, home, expecting, 0);
    }

    /** Who it is. */
    public Persona persona() {
        return Persona.of(this.key);
    }

    /** The stage in a word or two. */
    public String stageText() {
        if (!this.met) {
            return "Not met yet";
        }
        if (this.claimedBy != null && !this.claimedBy.isEmpty() && !this.stage.together()) {
            return "With " + this.claimedBy;
        }
        return switch (this.stage) {
            case MARRIED -> this.persona().female() ? "Your wife" : "Your husband";
            case DATING -> "Dating";
            default -> !this.likes ? "Friends (only)" : this.step == EtoRules.Step.ASK ? "Waiting to be asked out"
                : this.step == EtoRules.Step.COURTING ? "Courting: chapter " + (this.chapter + 1) + " of " + Courtship.CHAPTERS
                : NpcBonds.band(this.regard).label();
        };
    }

    /** How far the regard is, from the least to the most (0 to 1). */
    public float regardShare() {
        return Math.max(0.0F, Math.min(1.0F, (this.regard - NpcBonds.LEAST) / (float)(NpcBonds.MOST - NpcBonds.LEAST)));
    }

    /** 2.1.2: where the courtship is: the chapter on and its task, done so far (only while it is on). */
    public String errands() {
        Persona p = this.persona();
        return switch (this.step) {
            case COURTING -> this.chapter < Courtship.CHAPTERS ? Courtship.journal(this.key, this.chapter, this.done)
                : "Every chapter closed: ask " + p.her() + " out";
            case ASK -> "Every chapter closed: ask " + p.her() + " out";
            case DATING -> "Done";
            default -> "Not begun";
        };
    }

    /** What to do next, in a line. */
    public String next() {
        Persona p = this.persona();
        if (!this.met) {
            return "Find " + p.her() + " at " + p.home() + " and talk.";
        }
        if (this.claimedBy != null && !this.claimedBy.isEmpty() && !this.stage.together()) {
            return p.she().substring(0, 1).toUpperCase(java.util.Locale.ROOT) + p.she().substring(1) + "'s with " + this.claimedBy + ". Friendship is still yours.";
        }
        return switch (this.stage) {
            case MARRIED -> !this.home ? "Stand where you want to live and tell " + p.her() + ": \"This is our home now.\""
                : this.expecting ? "A child is on the way." : this.children == 0 ? "Your home is set. A family, when you are both ready."
                : "Your home is set; " + this.children + (this.children == 1 ? " child" : " children") + ".";
            case DATING -> this.regard < Romance.PROPOSE_REGARD ? "Regard " + this.regard + " of " + Romance.PROPOSE_REGARD
                + " to propose: take " + p.her() + " on dates (\"Let's go somewhere.\" when you're sweet), kisses, gifts, time."
                : this.days < Romance.PROPOSE_DAYS ? "Give it a day together, then propose."
                : "Ready: offer " + p.her() + " a ring (\"Marry me.\").";
            default -> !this.likes ? "Friends: " + p.she() + " won't date you." : this.regard < Courtship.BEGIN_AT && this.step == EtoRules.Step.NONE
                ? "Regard " + this.regard + " of " + Courtship.BEGIN_AT + " to begin courting: talk, gifts, lessons."
                : this.step == EtoRules.Step.NONE ? "Ask " + p.her() + ": \"Need anything done?\" Your courtship begins."
                : this.step == EtoRules.Step.ASK ? "Ask " + p.her() + " out."
                : this.step == EtoRules.Step.COURTING && this.chapter < Courtship.CHAPTERS ? Courtship.task(this.key, this.chapter, this.done)
                : this.errands() + ".";
        };
    }

    /** The cards as one text (a line each), for the quest log's packet. */
    public static String save(List<RomanceCard> cards) {
        StringBuilder out = new StringBuilder();
        for (RomanceCard c : cards) {
            out.append(clean(c.key)).append('\t').append(c.met ? 1 : 0).append('\t').append(c.stage.ordinal()).append('\t').append(c.regard).append('\t')
                .append(c.likes ? 1 : 0).append('\t').append(clean(c.claimedBy == null ? "" : c.claimedBy)).append('\t').append(c.step.ordinal()).append('\t')
                .append(c.done).append('\t').append(c.days).append('\t').append(c.children).append('\t').append(c.home ? 1 : 0).append('\t')
                .append(c.expecting ? 1 : 0).append('\t').append(c.chapter).append('\n');
        }
        return out.toString();
    }

    /** The cards back from {@link #save}'s text (a bad line is left out). */
    public static List<RomanceCard> load(String text) {
        List<RomanceCard> out = new ArrayList<>();
        if (text == null) {
            return out;
        }
        for (String line : text.split("\n")) {
            String[] f = line.split("\t", -1);
            if (f.length < 12 || !Persona.known(f[0])) {
                continue;
            }
            try {
                EtoRules.Step[] steps = EtoRules.Step.values();
                int step = Integer.parseInt(f[6]);
                out.add(new RomanceCard(f[0], f[1].equals("1"), Romance.Stage.byOrdinal(Integer.parseInt(f[2])), NpcBonds.clamp(Integer.parseInt(f[3])),
                    f[4].equals("1"), f[5].isEmpty() ? null : f[5], step >= 0 && step < steps.length ? steps[step] : EtoRules.Step.NONE,
                    Math.max(0, Integer.parseInt(f[7])), Math.max(0L, Long.parseLong(f[8])), Math.max(0, Integer.parseInt(f[9])), f[10].equals("1"),
                    f[11].equals("1"), f.length > 12 ? Math.max(0, Math.min(Courtship.CHAPTERS, Integer.parseInt(f[12]))) : 0));
            } catch (NumberFormatException bad) {
                // left out
            }
        }
        return out;
    }

    private static String clean(String text) {
        return text.replace('\t', ' ').replace('\n', ' ');
    }
}
