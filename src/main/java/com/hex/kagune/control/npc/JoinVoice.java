package com.hex.kagune.control.npc;

/**
 * 4.1.79: what the five leaders say through their ways in (pure, tested): each trial offered, begun, asked about while it runs, won, and
 * each way it is lost, and the scene at the end; in their own voices (Ayato cocky and sore, Roma giddy, Banjou trying to sound like a boss,
 * Ken gentle and a little sad, the General clipped and dry). The first asks and their answers stay {@code GangServer}'s (4.1.50 to 4.1.71).
 */
public final class JoinVoice {
    private JoinVoice() {
    }

    /**
     * The leader of {@code kind}'s gang at {@code moment}: "offer" (the step's trial put to them, before it starts), "start" (it starts),
     * "on" (talked to while it runs), "won", or a way it is lost ("lost" the spar lost, "draw" its time out, "fled" gone off its ground,
     * "died", "called" the patrol called it in, "slow" too slow, "booed" an act too long, "masked" a gas mask on, "walked" regulars walked
     * out, "killed" one of the raid killed, "partner" the partner lost). Picked by {@code seed}; never empty.
     */
    public static String say(JoinTrial.Kind kind, String moment, int seed) {
        String[] lines = switch (kind) {
            case SPAR -> switch (moment) {
                case "offer" -> new String[] {"Join Aogiri? ...You? Tch. Words are cheap. Fight me. Here. Get me to half and we'll talk. Drop under a "
                    + "quarter and you get nothing.", "Still want in? Then put your fists up. Half of me before a quarter of you. That's the deal."};
                case "start" -> new String[] {"*He cracks his neck and rolls his shoulders.* Don't hold back. I won't. Three... two...",
                    "*A slow, nasty grin.* Finally. Try to keep up. Three... two..."};
                case "on" -> new String[] {"Why are you TALKING? Hit me!"};
                case "won" -> new String[] {"*He wipes his lip and looks at the blood.* ...Tch. Fine. You hit like you mean it. Don't get a big head.",
                    "*Breathing hard, he lowers his hands first.* ...Enough. You're not useless. That's more than most."};
                case "lost" -> new String[] {"Get up. ...Come back when you can stand longer than that.", "That's it? Go eat something. Then come back."};
                case "draw" -> new String[] {"Done. You lasted. Lasting isn't winning. Again, when you've got something.",
                    "*He drops his hands.* I'm bored. You were careful. Careful doesn't get you into Aogiri."};
                case "fled" -> new String[] {"You ran. Aogiri doesn't run. Come back when you're done being scared."};
                default -> new String[] {"...Where'd you go? Tch. Come back when you're serious."};
            };
            case AMBUSH -> switch (moment) {
                case "offer" -> new String[] {"Fighting me is one thing. Doves are another. There's a pair walking their beat. Hit them before they "
                    + "know you're there, and finish it fast. If they get a call out, don't bother coming back.",
                    "One more thing, then you're in. Two doves, out on patrol. Ambush them. Fast and quiet."};
                case "start" -> new String[] {"They're out there. *He points with his chin.* You'll see them. Go. Thirty seconds once you start, no "
                    + "more.", "Two doves, that way. First blow starts the clock. Don't let them breathe."};
                case "on" -> new String[] {"The patrol. Go. Are you waiting for an invitation?"};
                case "won" -> new String[] {"Both down? ...Good. Come back to me."};
                case "called" -> new String[] {"Too slow. They got the call out. Now the bureau knows where Aogiri's looking. ...Come back and ask "
                    + "again when you've learned to finish things."};
                case "slow" -> new String[] {"You let them walk off. Pathetic. Ask me again."};
                default -> new String[] {"You went down to two doves? ...Ask me again when you're breathing."};
            };
            case RING -> switch (moment) {
                case "offer" -> new String[] {"Okay, funny one! Next act: YOU, in my ring, tonight. Three volunteers, one at a time. Put each one down "
                    + "before the crowd gets bored. Thirty seconds! Don't leave the stage, and don't die, it ruins the lighting.",
                    "Showtime's at night, sweetheart. Three volunteers, thirty seconds each, and you stay in the ring. Make it PRETTY."};
                case "start" -> new String[] {"*She throws her arms wide to the empty seats.* Ladies and gentlemen and things that eat them! Our new "
                    + "act! ...Volunteer number one, come on down!", "*claps once, very loud* Lights! Volunteers! GO!"};
                case "on" -> new String[] {"Don't talk to me, the act's ON! Look at your volunteer!"};
                case "won" -> new String[] {"*She leaps to her feet, clapping over her head.* ENCORE! Three for three! Okay, okay, you're good. Now "
                    + "come here, I've got a real job for you."};
                case "booed" -> new String[] {"Boooo! *She throws a flower at you.* Too slow, the crowd fell asleep. Again tomorrow night! Or "
                    + "tonight. Whenever."};
                case "fled" -> new String[] {"You LEFT THE STAGE? Nobody leaves my stage mid-act. ...Come back and do it properly."};
                case "night" -> new String[] {"In daylight? Nobody comes to a circus in daylight, silly. Come back tonight."};
                default -> new String[] {"Aww, you died. That's the oldest trick there is. Try a new one next time."};
            };
            case GAS -> switch (moment) {
                case "offer" -> new String[] {"*He swallows.* The boys keep the garden full of gas. Anyone who wants in walks through it. To "
                    + "me. Go out past the gate, then walk back. No mask. ...I did it too. It's awful. You'll be fine.",
                    "Out past the gate, then back to me, through the gas. Without a mask. That's how all of us got in."};
                case "start" -> new String[] {"Go on, out past the gate. When you turn round, the boys open the valves. Then just... keep walking. "
                    + "Don't stop. Stopping's the worst part.", "Past the gate first. I'll be right here. I'm not going anywhere."};
                case "on" -> new String[] {"Don't talk, breathe! ...No, don't breathe. Walk!"};
                case "won" -> new String[] {"*He catches you before you fall.* You made it. You made it! ...Sorry. I'm loud when I'm happy. Sit "
                    + "down a second.", "*He holds you up by the shoulders.* Nobody's ever walked it that fast. Well. Somebody has. But still!"};
                case "masked" -> new String[] {"*He shakes his head, sorry about it.* No mask. That's the whole point. Go back out and try again."};
                case "slow" -> new String[] {"You stopped. In the gas, stopping's the worst thing you can do. ...Get some air. Try again when you're "
                    + "ready."};
                default -> new String[] {"*He looks sick.* That... wasn't supposed to happen. Are you okay? We'll try again. Slower. No, faster."};
            };
            case GATE -> switch (moment) {
                case "offer" -> new String[] {"The Clowns are coming. Tonight, maybe now. They always come for the gate. Hold it with me? ...I'm "
                    + "not asking because I'm brave. I'm asking because I'm not.", "Clowns. At the gate. Will you stand with us?"};
                case "start" -> new String[] {"*He grips a pipe like it's a sword.* Here they come. Stay by the gate. Don't let them past. ...I'm "
                    + "right behind you. Next to you. I'm next to you.", "Positions, boys! ...That means you too. Here they come!"};
                case "on" -> new String[] {"Not now! The gate!"};
                case "won" -> new String[] {"*He laughs, shaky, and the boys cheer behind him.* They ran. They actually ran! ...Come inside. I need "
                    + "to say something to you."};
                case "fled" -> new String[] {"*Quietly.* You left the gate. ...The boys saw. Come back when you can stay."};
                case "slow" -> new String[] {"They're still at the gate and the boys are tired. ...We'll try again. I'm not angry. I'm tired too."};
                default -> new String[] {"You went down at the gate. *He sits next to you.* Me too, the first time. We'll go again."};
            };
            case SHIFT -> switch (moment) {
                case "offer" -> new String[] {"Okay. Small things first, as we say. Touka's off tonight, and I can't brew and serve at the same time. "
                    + "Cover her shift? Brew Anteiku Blends, and take a cup to each of our regulars before they give up and leave.",
                    "There's a shift that needs covering. Brew, serve, smile. ...Mostly brew and serve."};
                case "start" -> new String[] {"*He hands you an apron and a bag.* Bottles, beans, and Touka's sugar. Don't ask what's in it. The "
                    + "recipe's in the cookbook: a bottle, two beans, the sugar. Regulars sit near the counter. Hand them a cup. Be kind.",
                    "Apron on. Three regulars tonight. Brew them a cup each and take it to them. They don't wait forever."};
                case "on" -> new String[] {"*whispering* There's someone waiting. A cup, in your hand, to them. You're doing fine."};
                case "won" -> new String[] {"*He's smiling for real.* Three cups, three people who went home a little happier. ...That's all "
                    + "Anteiku is, really. Thank you."};
                case "walked" -> new String[] {"Two of them walked out. ...That's okay. Touka once threw a cup at one. We'll try again."};
                case "fled" -> new String[] {"You left in the middle of a shift? ...Please don't do that to the regulars. Come back and we'll start "
                    + "again."};
                default -> new String[] {"Are you all right? ...Forget the shift. Sit down. We'll do it another time."};
            };
            case CAFE -> switch (moment) {
                case "offer" -> new String[] {"There's something else. *His face falls.* Doves have been coming round, asking about the café. Today "
                    + "they're coming in. Drive them off. Please, don't kill anyone. If an investigator dies here, the whole bureau comes, and "
                    + "everyone here dies.", "They're coming to look round. Scare them off. Hurt them if you must. Nobody dies."};
                case "start" -> new String[] {"*He closes his book.* They're here. Beat them till they run. ...Hold back. I mean it.",
                    "Three of them. Make them run, not fall. I know it's harder. That's why I'm asking you."};
                case "on" -> new String[] {"Not now, they're here! Hold back!"};
                case "won" -> new String[] {"*He lets out a long breath.* They ran. All of them, alive. ...You don't know how much that means."};
                case "killed" -> new String[] {"*He goes very still.* ...He's dead. I asked you. *quietly* They'll come for all of us now. Please... "
                    + "think about what Anteiku is. Then come back."};
                case "slow" -> new String[] {"They're still poking round the café. ...We'll have to try again."};
                case "fled" -> new String[] {"You left them in the café with us. ...Come back. We can't do this without you."};
                default -> new String[] {"*He kneels by you.* Don't move. ...We'll try again. Just get better first."};
            };
            case HUNT -> switch (moment) {
                case "offer" -> new String[] {"Now the field. A ghoul was reported nearby. You'll hunt it with a partner. You bring "
                    + "your partner back alive, or you don't come back at all. Understood?",
                    "Field assignment: one ghoul, one partner. The ghoul dies. The partner doesn't."};
                case "start" -> new String[] {"Your partner's with you. The ghoul was seen out there. *He taps the map.* Work in pairs. That's not a "
                    + "suggestion.", "Here's your partner. Here's the report. Go."};
                case "on" -> new String[] {"Why are you in my office? Your partner's waiting."};
                case "won" -> new String[] {"Target down, partner alive. *A short nod.* Come back and report, investigator."};
                case "partner" -> new String[] {"*He doesn't raise his voice.* You lost your partner. ...That's the one thing I asked. Come back when "
                    + "you understand why."};
                case "slow" -> new String[] {"The trail's gone cold. Report back. We'll reassign it."};
                default -> new String[] {"You went down out there. Your partner brought you back. ...Think about that. Then try again."};
            };
        };
        return lines[Math.floorMod(seed, lines.length)];
    }

    /** The button that starts {@code kind} on the talk screen. */
    public static String start(JoinTrial.Kind kind) {
        return switch (kind) {
            case SPAR -> "Fine. Let's fight.";
            case AMBUSH -> "Where's the patrol?";
            case RING -> "Showtime.";
            case GAS -> "I'll walk it.";
            case GATE -> "I'll hold the gate.";
            case SHIFT -> "I'll cover the shift.";
            case CAFE -> "Let them come.";
            case HUNT -> "Assign me a partner, sir.";
        };
    }

    /** What the talk screen's gang button reads while {@code kind} is the step (not running). */
    public static String ask(JoinTrial.Kind kind) {
        return switch (kind) {
            case SPAR -> "About joining Aogiri...";
            case AMBUSH -> "About that patrol...";
            case RING -> "About the show...";
            case GAS -> "About the gas...";
            case GATE -> "About the gate...";
            case SHIFT -> "About the shift...";
            case CAFE -> "About the doves...";
            case HUNT -> "About the field assignment...";
        };
    }

    // ---------------------------------------------------------------- the steps between trials

    /** Roma, the job in her face: given ("job"), reminded ("job_on"), the kill done and the case asked ("case"). */
    public static String roma(String moment, int seed) {
        String[] lines = switch (moment) {
            case "job" -> new String[] {"Now a REAL job. *She tosses you one of her masks.* Wear my face. Find a dove, take him down while you're "
                + "wearing it, then bring me his briefcase. I want to put flowers in it.", "A job, in costume! Our face, a dove, his case. In that "
                + "order. Keep the mask, it suits you. ...It doesn't. Keep it anyway."};
            case "job_on" -> new String[] {"In my mask! On your face! Then a dove! Did I say it wrong? I never say things wrong.",
                "No face, no show. Mask on, then the dove."};
            default -> new String[] {"Now his case! The briefcase! In your hand, so I can see it.", "Where's my briefcase? Flowers are wilting."};
        };
        return lines[Math.floorMod(seed, lines.length)];
    }

    /** The General's test: begun ("exam", before the first question), a question passed ("next"), failed ("failed"), today's a fail ("today"). */
    public static String general(String moment, int seed) {
        String[] lines = switch (moment) {
            case "exam" -> new String[] {"Before the field, the academy. *He slides a sheet across the desk.* Three questions. One wrong and you sit "
                + "it again tomorrow.", "Written test. Three questions. Answer like your partner's life depends on it. One day it will."};
            case "next" -> new String[] {"Correct. Next.", "*He makes a mark.* Next question.", "Right. Don't relax."};
            case "failed" -> new String[] {"Wrong. *He takes the sheet back.* Tomorrow. Study.",
                "*He doesn't look up.* Incorrect. You'll sit it again tomorrow."};
            case "passed" -> new String[] {"Three for three. *He signs the sheet.* You've passed the academy's test.",
                "Correct. All of them. *Something close to a smile.* Not bad."};
            default -> new String[] {"You sat it today. Tomorrow, recruit.", "The academy's closed to you until tomorrow."};
        };
        return lines[Math.floorMod(seed, lines.length)];
    }

    /**
     * How {@code gang}'s leader takes the right first answer (Roma's laugh, Banjou taken seriously, Ken's reason, the bureau's oath), said
     * before the next step is put to them (4.1.50 to 4.1.71's words); Ayato asks nothing first ("").
     */
    public static String answered(int gang) {
        return switch (gang) {
            case GangPlayer.CLOWNS -> "*bursts out laughing so hard she has to hold onto you* HA! The BRIEFCASE! ...Okay. Okay. You're funny. ";
            case GangPlayer.GAS_MASKS -> "*He looks at you for a long moment, like nobody's ever said that to him.* ...Okay. Okay. ";
            case GangPlayer.ANTEIKU -> "*He's quiet for a long moment. Then he smiles, a little sadly.* ...Yeah. That's the right reason. It was mine too. ";
            case GangPlayer.CCG -> "*A short nod. Something close to approval.* ...Correct. Most people get that one wrong. ";
            default -> "";
        };
    }

    /** The scene at the end of {@code gang}'s way in: the leader taking them in (said as it happens, with the gesture and the mask). */
    public static String scene(int gang, int seed) {
        String[] lines = switch (gang) {
            case GangPlayer.AOGIRI -> new String[] {"*He tosses you a mask without looking at it.* Me, then two doves. You did it the hard way. ...You're "
                + "Aogiri now. Don't make me regret it. And if you ever turn on us, I'll kill you myself."};
            case GangPlayer.CLOWNS -> new String[] {"*She opens the case, drops an allium in it, and cackles.* A dove's briefcase! Welcome to the "
                + "Clowns, sweetheart. Nobody touches you now. Except me."};
            case GangPlayer.GAS_MASKS -> new String[] {"*He pulls a mask out of his coat, turns it over in his big hands, and holds it out with "
                + "both.* You walked the gas and held the gate. ...Welcome to the Gas Masks. I'm the weakest one here. You'll be fine."};
            case GangPlayer.ANTEIKU -> new String[] {"*He hangs a second apron on the hook by the door, and pours you a cup.* ...Welcome to Anteiku. "
                + "Nobody here will ask what you did before. Just... be kind, okay?"};
            default -> new String[] {"*He stands, which he doesn't do for many.* Investigator. Welcome to the Commission of Counter Ghoul. Your "
                + "quinque's in the case. Look after it, and it'll look after you."};
        };
        return lines[Math.floorMod(seed, lines.length)];
    }
}
