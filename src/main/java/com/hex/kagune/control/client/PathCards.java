package com.hex.kagune.control.client;

import com.hex.kagune.control.UkakuStage;
import com.hex.kagune.control.KagunePath;
import com.hex.kagune.control.client.PathChoiceLayout.Box;
import com.hex.kagune.control.graft.GraftStage;
import java.util.ArrayList;
import java.util.List;

/**
 * How each kagune presents itself on the starting screen: its card text, accent colour, progression strip and
 * animated silhouette. Pure (no Minecraft types), so the screen preview tool and the tests use the same cards. A new
 * kagune adds its card here.
 */
public final class PathCards {
    /** Draws a path's animated silhouette into a box: {@code selected} lets it show off (the Ukaku crystallizes). */
    @FunctionalInterface
    public interface Silhouette {
        List<PathSilhouette.Mark> marks(Box box, double time, float intensity, boolean selected, int background);
    }

    /** One card. {@code stageNames} and {@code stageMastery} run in parallel, lowest stage first. */
    public record Card(KagunePath path, String name, String type, List<String> bullets, String tags, int accent, String awaken,
                       List<String> stageNames, List<Integer> stageMastery, Silhouette silhouette) {
    }

    /** The Rinkaku path's stages are his forms in the build's order (names and mastery as in {@code KaguneForm}, see {@code RinkakuForms}). */
    static final List<String> RINKAKU_STAGES = List.of("Rinkaku Seed", "Twin Rinkaku", "Four-Tail Rinkaku", "Centipede Form", "Full Kakuja");
    static final List<Integer> RINKAKU_MASTERY = List.of(0, 250, 500, 750, 1000);

    private static final Card RINKAKU = new Card(KagunePath.RINKAKU, "RINKAKU", "Rinkaku · Centipede · Kakuja",
        List.of("Tendrils from the lower back", "Grapple, crush and slam", "Tough, regenerating tank", "Ends in the Full Kakuja"),
        "MELEE · GRAPPLE · TANK", PathSilhouette.RINKAKU, "RINKAKU", RINKAKU_STAGES, RINKAKU_MASTERY,
        (box, time, intensity, selected, background) -> PathSilhouette.rinkaku(box, time, intensity, background));
    private static final Card UKAKU = new Card(KagunePath.UKAKU, "UKAKU", "Ukaku · Black Rabbit",
        List.of("Wings from the shoulders", "One wing, then two, growing", "Fire, crystal spikes, a thrown wing", "Fast on foot and in the air",
            "Two kakuja: needles, then needles and fire"),
        "RANGED · SPEED · BLADES", PathSilhouette.UKAKU, "UKAKU", ukakuStageNames(), ukakuStageMastery(), PathSilhouette::ukaku);
    /** 6.1.0: Owl DNA's stages: its base form as it grows, then the Owl (its kakuja; 6.2.0: its only one). */
    static final List<String> PALE_STAGES = List.of(com.hex.kagune.control.pale.PaleUnlock.stageName(0), com.hex.kagune.control.pale.PaleUnlock.stageName(250),
        com.hex.kagune.control.pale.PaleUnlock.stageName(500), "The Owl (Kakuja)");
    static final List<Integer> PALE_MASTERY = List.of(0, 250, 500, 750);
    /** Owl DNA's accent: pale flesh with a blush of blood in it. */
    public static final int PALE_ACCENT = 0xE9BFAE;
    private static final Card PALE = new Card(KagunePath.PALE, "OWL DNA", "One-Eyed Owl · Pale Flesh",
        List.of("One kakugan, steam off the skin", "Blinding fists, flips, Big Punch", "Pale living flesh from the back",
            "It sweeps, grabs, flings, smashes", "The Owl: a kakuja of giant blades"),
        "SPEED · MELEE · FLESH", PALE_ACCENT, "OWL DNA", PALE_STAGES, PALE_MASTERY, PathSilhouette::pale);
    /** 7.0.0: Graft DNA's stages: its base stages as mastery opens them, then the kakuja (names and mastery as in {@code GraftStage}). */
    static final List<String> GRAFT_STAGES = graftStageNames();
    static final List<Integer> GRAFT_MASTERY = graftStageMastery();
    /** Graft DNA's accent: the crimson of the kakuja's slashes. */
    public static final int GRAFT_ACCENT = 0xD8283A;
    private static final Card GRAFT = new Card(KagunePath.GRAFT, "GRAFT DNA", "Rinkaku · Transplant · Right Kakugan",
        List.of("Thick rinkaku from the lower back", "Centipedes, thorns, a many-eyed kagune", "Spears, sweeps, grabs and slams",
            "Heals fast, at the cost of RC", "The kakuja: armor, a grin, two colossi"),
        "MELEE · REGEN · RANGE", GRAFT_ACCENT, "GRAFT DNA", GRAFT_STAGES, GRAFT_MASTERY, PathSilhouette::graft);

    private PathCards() {
    }

    /**
     * The card for a chosen path (an undecided path has none: it falls back to the first card). 7.0.0: Owl DNA is dormant; its card
     * is kept but never shown, its old players reading as Graft DNA.
     */
    public static Card of(KagunePath path) {
        return switch (path) {
            case UKAKU -> UKAKU;
            case PALE, GRAFT -> GRAFT;
            case RINKAKU, NONE -> RINKAKU;
        };
    }

    /** Owl DNA's card, dormant since 7.0.0 (kept for when it comes back). */
    static Card dormantOwl() {
        return PALE;
    }

    /** The cards of every choosable path, in starting-screen order. */
    public static List<Card> choosable() {
        List<Card> cards = new ArrayList<>();
        for (KagunePath path : KagunePath.choosable()) {
            cards.add(of(path));
        }
        return cards;
    }

    private static List<String> ukakuStageNames() {
        List<String> names = new ArrayList<>();
        for (UkakuStage stage : UkakuStage.values()) {
            names.add(stage.comingSoon() ? stage.displayName() + " (coming soon)" : stage.displayName());
        }
        return List.copyOf(names);
    }

    private static List<String> graftStageNames() {
        List<String> names = new ArrayList<>();
        for (GraftStage stage : GraftStage.values()) {
            names.add(stage == GraftStage.KAKUJA ? stage.displayName() + " (Kakuja)" : stage.displayName());
        }
        return List.copyOf(names);
    }

    private static List<Integer> graftStageMastery() {
        List<Integer> mastery = new ArrayList<>();
        for (GraftStage stage : GraftStage.values()) {
            mastery.add(stage.mastery());
        }
        return List.copyOf(mastery);
    }

    private static List<Integer> ukakuStageMastery() {
        List<Integer> mastery = new ArrayList<>();
        for (UkakuStage stage : UkakuStage.values()) {
            mastery.add(stage.requiredMastery());
        }
        return List.copyOf(mastery);
    }
}
