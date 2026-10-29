package com.hex.kagune.control.quest;

import com.hex.kagune.control.ukaku.UkakuMutation;
import com.hex.kagune.control.graft.GraftMutation;
import com.hex.kagune.control.graft.GraftStage;
import com.hex.kagune.gameplay.KaguneForm;
import com.hex.kagune.gameplay.MutationNode;
import java.util.function.Predicate;

/** 7.0.1: which {@link Gate} each path's forms, stages and mutations sit behind (pure, tested); null where none does. */
public final class Gates {
    private Gates() {
    }

    public static Gate rinkaku(KaguneForm form) {
        if (form == null) {
            return null;
        }
        return switch (form) {
            case RINKAKU_DUAL -> Gate.RINKAKU_TWIN;
            case RINKAKU_QUAD -> Gate.RINKAKU_FOUR;
            case CENTIPEDE -> Gate.RINKAKU_CENTIPEDE;
            case FULL_KAKUJA -> Gate.RINKAKU_FULL_KAKUJA;
            default -> null;
        };
    }

    /** 1.0.41: none: the Rinkaku path has no mutations left to gate. */
    public static Gate rinkaku(MutationNode node) {
        return null;
    }

    public static Gate graft(GraftStage stage) {
        if (stage == null) {
            return null;
        }
        return switch (stage) {
            case CENTIPEDE -> Gate.GRAFT_CENTIPEDE;
            case THORNS -> Gate.GRAFT_THORNS;
            case DRAGON -> Gate.GRAFT_DRAGON;
            case KAKUJA -> Gate.GRAFT_KAKUJA;
            default -> null;
        };
    }

    public static Gate graft(GraftMutation mutation) {
        if (mutation == GraftMutation.RIZES_APPETITE) {
            return Gate.GRAFT_APPETITE;
        }
        return mutation == GraftMutation.NOT_THAT_EASILY ? Gate.GRAFT_NOT_THAT_EASILY : null;
    }

    public static Gate ukaku(UkakuMutation mutation) {
        if (mutation == UkakuMutation.NOT_THAT_EASILY) {
            return Gate.UKAKU_NOT_THAT_EASILY;
        }
        return mutation == UkakuMutation.BURSTING_SHARDS ? Gate.UKAKU_BURSTING_SHARDS : null;
    }

    /** True if {@code gate} is null (nothing gates it) or {@code open} says it is open. */
    public static boolean passes(Gate gate, Predicate<Gate> open) {
        return gate == null || open.test(gate);
    }

    /** K held: the next base stage round from {@code from} that mastery and the open gates allow (as {@code GraftStage.next}). */
    public static GraftStage graftNext(GraftStage from, int mastery, Predicate<Gate> open) {
        GraftStage[] all = GraftStage.values();
        for (int step = 1; step <= all.length; step++) {
            GraftStage stage = all[Math.floorMod(from.ordinal() + step, all.length)];
            if (!stage.kakuja() && stage.unlocked(mastery) && passes(graft(stage), open)) {
                return stage;
            }
        }
        return GraftStage.TRANSPLANT;
    }

    /**
     * The highest base stage at or below {@code wanted} that {@code mastery} and the open gates allow (the transplant always);
     * never the kakuja, which is K's.
     */
    public static GraftStage graftUsable(GraftStage wanted, int mastery, Predicate<Gate> open) {
        GraftStage[] all = GraftStage.values();
        int from = wanted == null || wanted == GraftStage.KAKUJA ? GraftStage.DRAGON.ordinal() : wanted.ordinal();
        for (int i = from; i > 0; i--) {
            GraftStage stage = all[i];
            if (!stage.kakuja() && stage.unlocked(mastery) && passes(graft(stage), open)) {
                return stage;
            }
        }
        return GraftStage.TRANSPLANT;
    }
}
