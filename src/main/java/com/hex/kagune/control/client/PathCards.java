package com.hex.kagune.control.client;

import com.hex.kagune.control.AyatoStage;
import com.hex.kagune.control.KagunePath;
import com.hex.kagune.control.client.PathChoiceLayout.Box;
import com.hex.kagune.control.furuta.FurutaStage;
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

    /** The Kaneki path's stages are his forms in the build's order (names and mastery as in {@code KaguneForm}, see {@code KanekiForms}). */
    static final List<String> KANEKI_STAGES = List.of("Rinkaku Seed", "Twin Rinkaku", "Four-Tail Rinkaku", "Centipede Form", "Full Kakuja");
    static final List<Integer> KANEKI_MASTERY = List.of(0, 250, 500, 750, 1000);

    private static final Card KANEKI = new Card(KagunePath.KANEKI, "KANEKI DNA", "Rinkaku · Centipede · Kakuja",
        List.of("Tendrils from the lower back", "Grapple, crush and slam", "Tough, regenerating tank", "Ends in the Full Kakuja"),
        "MELEE · GRAPPLE · TANK", PathSilhouette.RINKAKU, "KANEKI DNA", KANEKI_STAGES, KANEKI_MASTERY,
        (box, time, intensity, selected, background) -> PathSilhouette.kaneki(box, time, intensity, background));
    private static final Card AYATO = new Card(KagunePath.AYATO, "AYATO DNA", "Ukaku · Black Rabbit",
        List.of("Wings from the shoulders", "One wing, then two, growing", "Fire, crystal spikes, a thrown wing", "Fast on foot and in the air",
            "Two kakuja: needles, then needles and fire"),
        "RANGED · SPEED · BLADES", PathSilhouette.UKAKU, "AYATO DNA", ayatoStageNames(), ayatoStageMastery(), PathSilhouette::ayato);
    /** 6.1.0: Owl DNA's stages: its base form as it grows, then the Owl (its kakuja; 6.2.0: its only one). */
    static final List<String> ETO_STAGES = List.of(com.hex.kagune.control.eto.EtoUnlock.stageName(0), com.hex.kagune.control.eto.EtoUnlock.stageName(250),
        com.hex.kagune.control.eto.EtoUnlock.stageName(500), "The Owl (Kakuja)");
    static final List<Integer> ETO_MASTERY = List.of(0, 250, 500, 750);
    /** Owl DNA's accent: pale flesh with a blush of blood in it. */
    public static final int ETO_ACCENT = 0xE9BFAE;
    private static final Card ETO = new Card(KagunePath.ETO, "OWL DNA", "One-Eyed Owl · Pale Flesh",
        List.of("One kakugan, steam off the skin", "Blinding fists, flips, Big Punch", "Pale living flesh from the back",
            "It sweeps, grabs, flings, smashes", "The Owl: a kakuja of giant blades"),
        "SPEED · MELEE · FLESH", ETO_ACCENT, "OWL DNA", ETO_STAGES, ETO_MASTERY, PathSilhouette::eto);
    /** 7.0.0: Furuta DNA's stages: its base stages as mastery opens them, then the kakuja (names and mastery as in {@code FurutaStage}). */
    static final List<String> FURUTA_STAGES = furutaStageNames();
    static final List<Integer> FURUTA_MASTERY = furutaStageMastery();
    /** Furuta DNA's accent: the crimson of the kakuja's slashes. */
    public static final int FURUTA_ACCENT = 0xD8283A;
    private static final Card FURUTA = new Card(KagunePath.FURUTA, "FURUTA DNA", "Rinkaku · Transplant · Right Kakugan",
        List.of("Thick rinkaku from the lower back", "Centipedes, thorns, a many-eyed kagune", "Spears, sweeps, grabs and slams",
            "Heals fast, at the cost of RC", "The kakuja: armor, a grin, two colossi"),
        "MELEE · REGEN · RANGE", FURUTA_ACCENT, "FURUTA DNA", FURUTA_STAGES, FURUTA_MASTERY, PathSilhouette::furuta);

    private PathCards() {
    }

    /**
     * The card for a chosen path (an undecided path has none: it falls back to the first card). 7.0.0: Owl DNA is dormant; its card
     * is kept but never shown, its old players reading as Furuta DNA.
     */
    public static Card of(KagunePath path) {
        return switch (path) {
            case AYATO -> AYATO;
            case ETO, FURUTA -> FURUTA;
            case KANEKI, NONE -> KANEKI;
        };
    }

    /** Owl DNA's card, dormant since 7.0.0 (kept for when it comes back). */
    static Card dormantOwl() {
        return ETO;
    }

    /** The cards of every choosable path, in starting-screen order. */
    public static List<Card> choosable() {
        List<Card> cards = new ArrayList<>();
        for (KagunePath path : KagunePath.choosable()) {
            cards.add(of(path));
        }
        return cards;
    }

    private static List<String> ayatoStageNames() {
        List<String> names = new ArrayList<>();
        for (AyatoStage stage : AyatoStage.values()) {
            names.add(stage.comingSoon() ? stage.displayName() + " (coming soon)" : stage.displayName());
        }
        return List.copyOf(names);
    }

    private static List<String> furutaStageNames() {
        List<String> names = new ArrayList<>();
        for (FurutaStage stage : FurutaStage.values()) {
            names.add(stage == FurutaStage.KAKUJA ? stage.displayName() + " (Kakuja)" : stage.displayName());
        }
        return List.copyOf(names);
    }

    private static List<Integer> furutaStageMastery() {
        List<Integer> mastery = new ArrayList<>();
        for (FurutaStage stage : FurutaStage.values()) {
            mastery.add(stage.mastery());
        }
        return List.copyOf(mastery);
    }

    private static List<Integer> ayatoStageMastery() {
        List<Integer> mastery = new ArrayList<>();
        for (AyatoStage stage : AyatoStage.values()) {
            mastery.add(stage.requiredMastery());
        }
        return List.copyOf(mastery);
    }
}
