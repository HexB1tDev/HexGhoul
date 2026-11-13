package com.hex.kagune.control.npc;

/**
 * 4.1.67: what the gangs' leaders say about leaving (pure, tested; the talk screen's renders use it too). A member asking their leader out
 * (the owner: "Add a new Quit the gang button to each leader whenever the player joins it."): asked to be sure, staying, going; a leader's
 * spouse in another gang (the owner: "being married to a gang leader but being part of another gang generates different reactions from the
 * spouse, constantly demanding they quit the gang."). Each leader in their own voice: Ayato (Aogiri) cold, Roma (the Clowns) a show, Banjou
 * (the Gas Masks) soft.
 */
public final class GangWords {
    private GangWords() {
    }

    /** What a member says to their leader to leave {@code gang}. */
    public static String wantOut(int gang) {
        return "I want out of " + GangPlayer.title(gang) + ".";
    }

    /** The leader of {@code gang}, asked out: are they sure? */
    public static String asked(int gang) {
        return switch (gang) {
            case GangPlayer.AOGIRI -> "You want out of Aogiri? *His eyes narrow.* ...Say it again. And mean it this time.";
            case GangPlayer.GAS_MASKS -> "You want to leave? *He swallows.* ...Did we do something wrong? You can say.";
            default -> "Leaving the circus? *The smile wobbles.* ...You're serious? You're SERIOUS?";
        };
    }

    /** The leader of {@code gang}, when the member stays after all. */
    public static String stay(int gang) {
        return switch (gang) {
            case GangPlayer.AOGIRI -> "Then stop wasting my time.";
            case GangPlayer.GAS_MASKS -> "*He lets out a breath he was holding.* Good. Good. The boys would've asked where you went.";
            default -> "HA! Knew it. Nobody leaves the Clowns. Nobody fun, anyway.";
        };
    }

    /** The leader of {@code gang}, as the member goes. */
    public static String gone(int gang) {
        return switch (gang) {
            case GangPlayer.AOGIRI -> "Then go. You're not Aogiri anymore. Don't come crying to the Tree when the doves find you.";
            case GangPlayer.GAS_MASKS -> "Okay. *He nods too many times.* Okay. You were good to the boys. ...Stay safe out there. Really.";
            default -> "Fine! Go! The show goes on without you! ...It does. It DOES. *She doesn't look at you.*";
        };
    }

    /** The leader of {@code leads}, married to one of {@code gang}: quit them ({@code seed} picks the words). */
    public static String demand(int leads, int gang, int seed) {
        String them = GangPlayer.title(gang);
        String Them = Character.toUpperCase(them.charAt(0)) + them.substring(1);
        String[] lines = switch (leads) {
            case GangPlayer.AOGIRI -> new String[] {"You're still running with " + them + "? You married Aogiri. You married ME. Quit them.",
                "Every time I see their mark on you I want to break something. Leave " + them + ".", Them + ". Still. Quit them, or I'll make them quit you."};
            case GangPlayer.GAS_MASKS -> new String[] {"You're still with " + them + "? *He rubs the back of his neck.* ...They're not safe. Please quit them. For me.",
                "I keep thinking " + them + " will get you killed. ...Please leave them.",
                "The boys ask why my spouse isn't one of us. I don't know what to tell them. Quit " + them + "?"};
            default -> new String[] {"My spouse in another troupe? *pouts* Quit " + them + ". Come be a Clown. Be MY Clown.",
                "Ugh, " + them + " again. Leave them. I'll juggle for you every night.",
                "The ringmaster's own spouse, in someone else's colours? Scandal! Quit " + them + "."};
        };
        return lines[Math.floorMod(seed, lines.length)];
    }

    /** True when the spouse brings it up this time (two times in three, by {@code seed}): "constantly", without every word being it. */
    public static boolean demands(int seed) {
        return Math.floorMod(seed, 3) != 0;
    }
}
