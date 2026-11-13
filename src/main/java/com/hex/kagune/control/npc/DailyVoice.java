package com.hex.kagune.control.npc;

/**
 * 4.1.78: what each giver of dailies says (pure, tested): Noro, Roma, Banjou, the General (their 4.1.53, 4.1.54 and 4.1.71 lines) and Ken
 * (new), at each moment of the daily board.
 */
public final class DailyVoice {
    private DailyVoice() {
    }

    /**
     * 4.1.78: what {@code gang}'s giver says ({@code moment}: "board" offering today's jobs, "took" one taken ({@code what}: it, mid-sentence),
     * "going" how far, "short" not enough in the hand, "paid", "rested" done for today, "nothing" none they can do today, "outsider" not one
     * of theirs), picked by {@code seed}. Their own voices (4.1.53 Noro, Roma; 4.1.54 Banjou; 4.1.71 the General; Ken's new).
     */
    public static String voice(int gang, String moment, int seed, String what, GangDaily d) {
        String count = d == null ? "" : d.count() + " of " + d.goal();
        String goal = d == null ? "" : String.valueOf(d.goal());
        return switch (gang) {
            case GangPlayer.AOGIRI -> switch (moment) {
                case "board" -> pick(seed, "*tilts his head* ...Three jobs. One is yours.", "...Work. Pick.", "Aogiri has work. Choose.");
                case "took" -> pick(seed, "*tilts his head* ...Work. " + capFirst(what) + ". Today.", "Aogiri needs this done. " + capFirst(what) + ".",
                    "...You came. Good. " + capFirst(what) + ". Then come back.");
                case "going" -> count + ". ...Keep going.";
                case "short" -> "...Not enough. " + goal + ". In your hand.";
                case "paid" -> pick(seed, "...Good. *hands it over without a word*", "Aogiri remembers this. Take it.");
                case "rested" -> pick(seed, "...Done for today. Rest.", "Tomorrow.");
                case "nothing" -> "...Nothing today.";
                default -> "...";
            };
            case GangPlayer.GAS_MASKS -> switch (moment) {
                case "board" -> pick(seed, "*unfolds a crumpled list* Okay. Three things the boys need. ...You pick. Whichever you like.",
                    "The boys need a few things. ...Pick one? If you want.");
                case "took" -> pick(seed, "Okay. Today the boys need you to " + what + ". ...If you can. Please.", "Right. Boss voice. *clears his throat* "
                    + "I need you to " + what + ". ...That sounded good, right?", "I hate asking. Could you " + what + "? I'd do it myself, but you'd do it better.");
                case "going" -> count + ". You're doing great. Better than I would.";
                case "short" -> goal + ". In your hand, so I can count it. ...Sorry. I count everything. Things run out fast here.";
                case "paid" -> pick(seed, "You did it. *hands it over with both hands* ...Thank you. Really.", "The boys will hear about this. Take it. You earned it.");
                case "rested" -> pick(seed, "You already did today's. Rest. That's an order. ...A request.", "Tomorrow. The boys are fine. Thanks to you.");
                case "nothing" -> "Nothing today. ...Go rest. Seriously.";
                default -> "Jobs are for the boys. ...Join first. Then I'll find you something.";
            };
            case GangPlayer.CCG -> switch (moment) {
                case "board" -> pick(seed, "*He slides three files across the desk.* Three assignments. Choose one. Read it properly.",
                    "Today's assignments. Pick the one you can finish, investigator.");
                case "took" -> pick(seed, "Today's orders: " + what + ". Report back when it's done.", "*He doesn't look up from the report.* " + capFirst(what)
                    + ". Dismissed.", "The ward needs this. " + capFirst(what) + ". Then come back here, in one piece.");
                case "going" -> count + ". Keep at it. Don't get careless near the end; that's when they get you.";
                case "short" -> goal + ". In your hand. The bureau signs for what it can count.";
                case "paid" -> pick(seed, "Good work. *He signs something without reading it, then reads it.* Take this. You earned it.",
                    "Noted in your file. *A short nod.* That's a good file to have. Here.");
                case "rested" -> pick(seed, "You've done today's. Rest. Tired investigators make widows.", "Tomorrow, investigator. Go home.");
                case "nothing" -> "Nothing on my desk for you today. Go home.";
                default -> "Orders are for investigators. You aren't one.";
            };
            case GangPlayer.ANTEIKU -> switch (moment) {
                case "board" -> pick(seed, "There's always something. ...Would you take one of these? Only if you have time.",
                    "Touka left me a list. *smiles* Pick whichever you like.");
                case "took" -> pick(seed, "Thank you. Really. " + capFirst(what) + ". ...Be careful, okay?", capFirst(what)
                    + ". I'll keep a cup warm for when you're back.", "You'd do that? ..." + capFirst(what) + ", then. Come back safe.");
                case "going" -> count + ". You don't have to rush. Just come back in one piece.";
                case "short" -> "I need " + goal + ". In your hand, if you can. ...Sorry, I'm terrible at counting things in pockets.";
                case "paid" -> pick(seed, "Thank you. Here. It's not much, but... thank you.", "*hands it to you with both hands* For the café. For all of us.");
                case "rested" -> pick(seed, "You've done enough today. Sit for a while. Have a coffee.", "Tomorrow. Today, rest. Please.");
                case "nothing" -> "Nothing today. ...Stay for a coffee instead?";
                default -> "That's for Anteiku's people. ...You'd be welcome, though. If you ever want to be.";
            };
            default -> switch (moment) {
                case "board" -> pick(seed, "Jobs! I have jobs! Pick your act, darling.", "Three acts on tonight's bill. Which one's yours?");
                case "took" -> pick(seed, "A job! For you! " + capFirst(what) + ". Make it funny.", "Today's act: " + what + ". Break a leg. Theirs, preferably.",
                    "*claps* Ooh, homework! " + capFirst(what) + ". Chop chop.");
                case "going" -> count + "! The suspense is killing me. Not literally. Yet.";
                case "short" -> "I said " + goal + "! In your hand, where I can see them. I count everything.";
                case "paid" -> pick(seed, "*throws confetti made of something* Bravo! Take your prize!", "What a show! Here, a little something. Don't eat the confetti.");
                case "rested" -> pick(seed, "You already did today's act! Encores are tomorrow.", "Shoo. Come back tomorrow, I'll have something awful.");
                case "nothing" -> "No acts tonight! The show's sold out. Come back tomorrow.";
                default -> "Jobs are for Clowns, sweetheart. Join first.";
            };
        };
    }

    private static String pick(int seed, String... lines) {
        return lines[Math.floorMod(seed, lines.length)];
    }

    private static String capFirst(String text) {
        return text.isEmpty() ? text : Character.toUpperCase(text.charAt(0)) + text.substring(1);
    }
}
