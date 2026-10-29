package com.hex.kagune.control.quest;

import com.hex.kagune.control.ayato.AyatoMutation;
import com.hex.kagune.control.furuta.FurutaMutation;
import com.hex.kagune.control.furuta.FurutaStage;
import com.hex.kagune.gameplay.KaguneForm;
import com.hex.kagune.gameplay.MutationNode;
import java.util.function.Predicate;

/** 7.0.1: which {@link Gate} each path's forms, stages and mutations sit behind (pure, tested); null where none does. */
public final class Gates {
    private Gates() {
    }

    public static Gate kaneki(KaguneForm form) {
        if (form == null) {
            return null;
        }
        return switch (form) {
            case RINKAKU_DUAL -> Gate.KANEKI_TWIN;
            case RINKAKU_QUAD -> Gate.KANEKI_FOUR;
            case CENTIPEDE -> Gate.KANEKI_CENTIPEDE;
            case FULL_KAKUJA -> Gate.KANEKI_FULL_KAKUJA;
            default -> null;
        };
    }

    /** 1.0.41: none: the Kaneki path has no mutations left to gate. */
    public static Gate kaneki(MutationNode node) {
        return null;
    }

    public static Gate furuta(FurutaStage stage) {
        if (stage == null) {
            return null;
        }
        return switch (stage) {
            case CENTIPEDE -> Gate.FURUTA_CENTIPEDE;
            case THORNS -> Gate.FURUTA_THORNS;
            case DRAGON -> Gate.FURUTA_DRAGON;
            case KAKUJA -> Gate.FURUTA_KAKUJA;
            default -> null;
        };
    }

    public static Gate furuta(FurutaMutation mutation) {
        if (mutation == FurutaMutation.RIZES_APPETITE) {
            return Gate.FURUTA_APPETITE;
        }
        return mutation == FurutaMutation.NOT_THAT_EASILY ? Gate.FURUTA_NOT_THAT_EASILY : null;
    }

    public static Gate ayato(AyatoMutation mutation) {
        if (mutation == AyatoMutation.NOT_THAT_EASILY) {
            return Gate.AYATO_NOT_THAT_EASILY;
        }
        return mutation == AyatoMutation.BURSTING_SHARDS ? Gate.AYATO_BURSTING_SHARDS : null;
    }

    /** True if {@code gate} is null (nothing gates it) or {@code open} says it is open. */
    public static boolean passes(Gate gate, Predicate<Gate> open) {
        return gate == null || open.test(gate);
    }

    /** K held: the next base stage round from {@code from} that mastery and the open gates allow (as {@code FurutaStage.next}). */
    public static FurutaStage furutaNext(FurutaStage from, int mastery, Predicate<Gate> open) {
        FurutaStage[] all = FurutaStage.values();
        for (int step = 1; step <= all.length; step++) {
            FurutaStage stage = all[Math.floorMod(from.ordinal() + step, all.length)];
            if (!stage.kakuja() && stage.unlocked(mastery) && passes(furuta(stage), open)) {
                return stage;
            }
        }
        return FurutaStage.TRANSPLANT;
    }

    /**
     * The highest base stage at or below {@code wanted} that {@code mastery} and the open gates allow (the transplant always);
     * never the kakuja, which is K's.
     */
    public static FurutaStage furutaUsable(FurutaStage wanted, int mastery, Predicate<Gate> open) {
        FurutaStage[] all = FurutaStage.values();
        int from = wanted == null || wanted == FurutaStage.KAKUJA ? FurutaStage.DRAGON.ordinal() : wanted.ordinal();
        for (int i = from; i > 0; i--) {
            FurutaStage stage = all[i];
            if (!stage.kakuja() && stage.unlocked(mastery) && passes(furuta(stage), open)) {
                return stage;
            }
        }
        return FurutaStage.TRANSPLANT;
    }
}
