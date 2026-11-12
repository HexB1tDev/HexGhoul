package com.hex.kagune.control.npc;

/**
 * 2.2.4 (pure, tested; the owner: "I want different kiss scenes with the same principle as the one already in place, but I want different
 * kiss scenes depending on the mood. If they're angry and you ask for one, they'll give you an angry one. And whatsoever, like, make up new
 * ones"): the kiss as the mood has it. The same as ever (they lean in and wait; a tap of shift is a quick one, held they blush, held longer
 * they pull them in: {@link EtoRules#kiss}), but each mood its own: how they wait ({@link #waits}), what they say, how they're drawn
 * ({@code MoodKissPose}), and what they do after ({@link #after}).
 */
public final class MoodKiss {
    private MoodKiss() {
    }

    /** The kinds of kiss. Sent by ordinal (append only). */
    public enum Style {
        /** Happy: as it always was. */
        SWEET,
        /** Sulking: grabbed by the collar, quick and hard, shoved away. It ends the sulk. */
        ANGRY,
        /** Jealous: pulled in at once, held too long. "Mine." It ends the jealousy. */
        POSSESSIVE,
        /** Giddy: up on their toes, bouncing, a burst of little ones, a spin. */
        GIDDY,
        /** Affectionate or clingy: hands on their face, slow, forehead to forehead after. */
        TENDER,
        /** Lonely: thrown at them, arms round them, won't let go. */
        DESPERATE,
        /** Sleepy: lazy, drooping, their head on the shoulder after. */
        SLEEPY,
        /** Starving: the eye open, a bite at the end. */
        HUNGRY,
        /** Bored: dodges the first try, laughing, then a real one, then off. */
        TEASING,
        // 4.1.15 (the owner: "add more kiss types and better the animation"): appended (sent by ordinal). Not moods: moments, and the
        // happy kiss's other ways; a mood's own kiss always comes first ({@link #of(EtoMood, Moment)}).
        /** The first few, not yet married: careful, a cheek offered, hands that don't know where to go; then hiding their face. */
        SHY,
        /** The morning after a night together: slow, rumpled, a stretch in the middle of it. */
        MORNING,
        /** Rain falling on them: face tipped up into it, laughing, then a soaked kiss, then a twirl. */
        RAIN,
        /** Like the end of a film: on her toes, a foot popped up behind her; his, a dip. */
        MOVIE,
        /** Nose to nose, rubbing side to side, giggling. */
        NOSE,
        /** A bow over their hand, a kiss on the knuckles. */
        GALLANT
    }

    /** 4.1.15: the moment a kiss comes in: rain where they stand, the morning after a night together, how many kisses so far, wed, a roll (0..99). */
    public record Moment(boolean raining, boolean morningAfter, long kisses, boolean married, int roll) {
    }

    /** 4.1.15: kisses before this one that are still shy ones (not yet married). */
    public static final long SHY_UNTIL = 3L;
    /** 4.1.15: how long after a night together it is still the morning after (ticks: about five minutes). */
    public static final long MORNING_AFTER = 6000L;

    /**
     * 4.1.15: the kiss for {@code mood} at {@code moment}: the mood's own if it has one (sulking is still an angry kiss in the rain); else the
     * morning after, the rain, the first shy few; else now and then a film's, a nose rub or a bow over their hand; else the sweet one.
     */
    public static Style of(EtoMood mood, Moment moment) {
        Style moody = of(mood);
        if (moody != Style.SWEET || moment == null) {
            return moody;
        }
        if (moment.morningAfter()) {
            return Style.MORNING;
        }
        if (moment.raining()) {
            return Style.RAIN;
        }
        if (moment.kisses() < SHY_UNTIL && !moment.married()) {
            return Style.SHY;
        }
        int roll = Math.floorMod(moment.roll(), 100);
        return roll < 14 ? Style.MOVIE : roll < 28 ? Style.NOSE : roll < 40 ? Style.GALLANT : Style.SWEET;
    }

    /** The kiss for {@code mood}. */
    public static Style of(EtoMood mood) {
        return switch (mood) {
            case SULKY -> Style.ANGRY;
            case JEALOUS -> Style.POSSESSIVE;
            case GIDDY -> Style.GIDDY;
            case AFFECTIONATE, CLINGY -> Style.TENDER;
            case LONELY -> Style.DESPERATE;
            case SLEEPY -> Style.SLEEPY;
            case BLOODTHIRSTY -> Style.HUNGRY;
            case BORED -> Style.TEASING;
            default -> Style.SWEET;
        };
    }

    /** How long they lean in and wait (ticks): the angry won't wait long; the sleepy forget to stop. */
    public static int waits(Style style) {
        return switch (style) {
            case ANGRY -> 70;
            case SLEEPY -> 240;
            case MORNING -> 200;
            case DESPERATE -> 220;
            default -> EtoRules.KISS_WAIT;
        };
    }

    /** True if, teasing, the first tap is dodged (they lean back laughing, then lean in again). */
    public static boolean dodges(Style style) {
        return style == Style.TEASING;
    }

    /** What they do once it's over: the gesture after the kiss ({@code EtoGesture}), or null to keep the usual. */
    public static EtoGesture after(Style style) {
        return switch (style) {
            case ANGRY -> EtoGesture.ANGRY;
            case POSSESSIVE -> EtoGesture.CLING;
            case DESPERATE -> EtoGesture.HUG;
            case SLEEPY -> EtoGesture.LEAN;
            case HUNGRY -> EtoGesture.LICK;
            case TEASING -> EtoGesture.HOP;
            case SHY -> EtoGesture.SHY;
            case MORNING -> EtoGesture.STRETCH;
            case RAIN -> EtoGesture.TWIRL;
            case NOSE -> EtoGesture.GIGGLE;
            case GALLANT -> EtoGesture.BOW;
            default -> null;
        };
    }

    /**
     * 4.1.16 (the owner: "Confirm that all females when kissing have a blush, sort of ... So every time [they] blush, it depends on their
     * mood"): how deep the colour comes up in a kiss of {@code style} (0..1): a shy one, a giddy one, to the ears; a tender or a film's,
     * warm; an angry or a hungry one, only a flush.
     */
    public static float blush(Style style) {
        return switch (style == null ? Style.SWEET : style) {
            case SHY, GIDDY -> 1.0F;
            case MOVIE, NOSE -> 0.9F;
            case SWEET, TENDER, RAIN -> 0.8F;
            case DESPERATE, GALLANT -> 0.7F;
            case POSSESSIVE, TEASING -> 0.6F;
            case MORNING -> 0.55F;
            case SLEEPY -> 0.45F;
            case ANGRY -> 0.35F;
            case HUNGRY -> 0.3F;
        };
    }

    /** How many hearts it's worth, against the usual. */
    public static double hearts(Style style) {
        return switch (style) {
            case ANGRY -> 0.4;
            case GIDDY, TENDER -> 1.6;
            case DESPERATE, POSSESSIVE -> 1.3;
            case RAIN, MOVIE -> 1.5;
            case NOSE -> 1.3;
            case SHY, MORNING, GALLANT -> 1.2;
            default -> 1.0;
        };
    }

    // ---------------------------------------------------------------- their words

    /** What the screen says while they wait ({@code name}: what they're called; {@code pos}: her or his). */
    public static String waiting(Style style, String name, String pos) {
        return switch (style) {
            case SWEET -> name + " leans in, eyes closed...";
            case ANGRY -> name + " grabs your collar, scowling. \"Well? Hurry up.\"";
            case POSSESSIVE -> name + " pulls you down by the collar and doesn't let go...";
            case GIDDY -> name + " bounces up on " + pos + " toes, grinning, eyes squeezed shut...";
            case TENDER -> name + " cups your face in both hands, slowly...";
            case DESPERATE -> name + " throws " + pos + " arms round you and holds on...";
            case SLEEPY -> name + " leans in, half asleep, swaying...";
            case HUNGRY -> name + " leans in, kakugan glowing, lips parted...";
            case TEASING -> name + " leans in with a sly little smile...";
            case SHY -> name + " glances at you, away, back... and leans in, very carefully...";
            case MORNING -> name + " leans in, hair a mess, still warm from sleep...";
            case RAIN -> name + " tips " + pos + " face up into the rain, laughing, then leans in...";
            case MOVIE -> name + " leans in like the last scene of a film...";
            case NOSE -> name + " leans in until your noses touch, grinning...";
            case GALLANT -> name + " takes your hand and bows over it...";
        };
    }

    /** What {@code key} says after a quick one ({@code held}: held a while; {@code pulled}: pulled all the way in). */
    public static String kissed(String key, Style style, boolean held, boolean pulled, int seed) {
        String[] lines = switch (style) {
            case SWEET -> null;
            case ANGRY -> switch (key) {
                case "eto" -> new String[] {"*shoves you back* There. Happy? I'm still annoyed. ...Less annoyed.", "Hmph. That counts as an apology. Yours, not mine."};
                case "touka" -> new String[] {"*shoves you away* ...Don't get used to it. I'm still mad. A little.", "There. Now shut up. ...Okay, I'm not mad anymore. Idiot."};
                case "general" -> new String[] {"*kisses you once, stiffly, and steps back* ...I'm still angry. That was not a concession.", "That doesn't settle it. ...It helps."};
                case "roma" -> new String[] {"*kisses you hard, then flicks your nose* ...I'm still mad. That was a free sample.", "That doesn't fix it. ...It fixes like forty percent."};
                case "ayato" -> new String[] {"*grabs your collar, kisses you hard, shoves you off* ...I'm still mad. Shut up.", "That doesn't fix it. ...It fixes it a little."};
                default -> new String[] {"*pulls back, flushed* ...Okay. I can't stay angry when you do that.", "That wasn't fair. ...I'm not angry anymore."};
            };
            case POSSESSIVE -> switch (key) {
                case "eto" -> new String[] {"Mine. Every page of you. Remember that.", "*doesn't let go* Tell whoever that was that you're taken."};
                case "touka" -> new String[] {"Mine. Got it? Mine.", "*keeps hold of your collar* ...Next time someone looks at you, I'm biting them."};
                case "general" -> new String[] {"*a hand at your back, not letting go* Mine. ...Let them all see.", "*jaw set* Whoever that was can find another district."};
                case "roma" -> new String[] {"*an arm hooked round your neck, grinning at nobody* Mine. Tell the whole audience.", "*purple flickering behind her* Whoever that was, I have a trapdoor with their name on it."};
                case "ayato" -> new String[] {"*doesn't let go* Mine. Tell everyone.", "*crystal flickering on his wings* Anyone else tries that, they're dead."};
                default -> new String[] {"...I'm sorry. I needed to know you were still mine.", "*holds you a little too tight* Mine. ...Sorry. Mine."};
            };
            case GIDDY -> switch (key) {
                case "eto" -> new String[] {"Again! No— again! *laughs* I've lost count. Again.", "*spins on the spot* I'm writing this down in capital letters."};
                case "touka" -> new String[] {"*can't stop grinning* ...Shut up. I'm not smiling. I'm NOT.", "*hops on the spot, then catches herself* ...You saw nothing."};
                case "general" -> new String[] {"*laughing into it, astonished at himself* ...I don't do this. I'm doing this.", "Again. ...That's an order."};
                case "roma" -> new String[] {"*laughing into it so hard she has to stop* Again! Again! I'm keeping score!", "*spins you once, mid-kiss* Ta-daaa. ...Again."};
                case "ayato" -> new String[] {"*grinning, then scowling about it* ...Stop looking at me like that.", "Again. ...What? I said again."};
                default -> new String[] {"*laughs, surprised at himself* I don't know what's wrong with me today. Again?", "*grinning like an idiot* ...Sorry. I'm just happy."};
            };
            case TENDER -> switch (key) {
                case "eto" -> new String[] {"*rests her forehead on yours* ...Stay like this. Just a minute. Don't say anything.", "Mm. That's the one I'll remember."};
                case "touka" -> new String[] {"*forehead to yours, eyes closed* ...Okay. That one was nice. Really nice.", "*softly* Don't move yet."};
                case "general" -> new String[] {"*forehead against yours, eyes shut* ...Hold there.", "*very quietly* ...Thirty years. Nothing was ever like that."};
                case "roma" -> new String[] {"*forehead to yours, quiet, no joke at all* ...Stay right there.", "*very softly* ...That one wasn't a bit. That one was real."};
                case "ayato" -> new String[] {"*forehead against yours, quiet for once* ...Don't move.", "*very quietly* ...That one I'm keeping."};
                default -> new String[] {"*touches his forehead to yours* ...I love you. That's all.", "*keeps his hands on your face a moment longer* Thank you."};
            };
            case DESPERATE -> switch (key) {
                case "eto" -> new String[] {"Don't go away that long again. I read the same page forty times waiting.", "*won't let go* ...I missed you. Horribly. Don't."};
                case "touka" -> new String[] {"*face in your shoulder* Where were you? ...Don't answer. Just stay.", "Idiot. You were gone ages. ...Stay."};
                case "general" -> new String[] {"*holds on too hard* Where were you? I had the whole bureau looking. ...Don't tell them I said so.", "...You're back. Stay."};
                case "roma" -> new String[] {"*holds on too tight* Where WERE you? I had a whole bit ready for your funeral. I hated it.", "...You came back. Good. Don't do that again."};
                case "ayato" -> new String[] {"*holds on too hard* Where were you? Don't do that. Don't you EVER do that.", "...You came back. Good. Stay."};
                default -> new String[] {"*holds on* I kept thinking something happened to you. Please don't disappear like that.", "I missed you. So much. Stay?"};
            };
            case SLEEPY -> switch (key) {
                case "eto" -> new String[] {"*yawns against your mouth* ...Mm. Sorry. Carry me to bed.", "*head on your shoulder* Five more minutes. Of this."};
                case "touka" -> new String[] {"*leans her whole weight on you* ...I'm not asleep. I'm resting my eyes. On you.", "Mm. G'night. ...It's day? Don't care."};
                case "general" -> new String[] {"*head dropping onto your shoulder* ...Two days awake. Don't move.", "*half asleep* Stay. That's an order."};
                case "roma" -> new String[] {"*head drops onto your shoulder* ...Wake me if anyone dies. Or don't. Surprise me.", "*half asleep* Stay. Ringmaster's orders."};
                case "ayato" -> new String[] {"*head drops onto your shoulder* ...I'm not tired. Shut up.", "*half asleep* Stay. That's an order."};
                default -> new String[] {"*rests his head on your shoulder* ...Sorry. I'm so tired. This is nice.", "*half asleep* Stay. Just a little."};
            };
            case HUNGRY -> switch (key) {
                case "eto" -> new String[] {"*bites your lip, just a little* Sorry. Not sorry. You taste like dinner.", "*licks her lips, eye glowing* Mm. Feed me, and I'll do that again."};
                case "touka" -> new String[] {"*nips your lip* ...Sorry. I'm starving. That's not an excuse. It's a warning.", "*kakugan flickering* Don't look at me like that, I'm hungry."};
                case "general" -> new String[] {"*hard, brief, his hands not quite steady* ...A bad day. I needed that. Stay near me.", "*grips your shoulders* I lost people today. Not you. Never you."};
                case "roma" -> new String[] {"*bites your lip, grinning, kakugan lit* ...Relax. I only nibble on people I like.", "*purple flickering behind her* Hunt with me. It'll be a riot."};
                case "ayato" -> new String[] {"*bites your lip, eyes red* ...Don't flinch. I'm not going to eat you. Probably.", "*kakugan burning* Hunt with me. Now."};
                default -> new String[] {"*pulls back fast, kakugan bright* I'm sorry. I almost bit. I need to eat.", "*a little breathless* ...Hunt with me later? Please."};
            };
            case TEASING -> switch (key) {
                case "eto" -> new String[] {"Caught me. Barely. *runs off giggling* Catch me again!", "*skips back out of reach* That's one. Want two? Earn it."};
                case "touka" -> new String[] {"Ha! Took you long enough. *hops back* Bet you can't do it again.", "*sticks her tongue out and backs away* Slowpoke."};
                case "general" -> new String[] {"*steps back, the corner of his mouth moving* Too slow, recruit.", "Hm. You telegraph. Again."};
                case "roma" -> new String[] {"*leans out of reach, cackling* Too slow! The ringmaster is untouchable!", "Ha! You telegraph like a mime. Again."};
                case "ayato" -> new String[] {"*smirks and steps back* Too slow. Again.", "Ha. That all you've got?"};
                default -> new String[] {"*laughs and steps back* You were faster than I thought.", "*grins* Okay, okay. You win. This time."};
            };
            case SHY -> switch (key) {
                case "eto" -> new String[] {"*pulls back fast, pink to the ears* ...I've written a hundred of those. That was nothing like any of them.",
                    "*hides behind her notebook* Don't look at me. I'm revising."};
                case "touka" -> new String[] {"*bright red, staring at the floor* ...That didn't happen. ...Okay. It happened.",
                    "*kisses your cheek and steps straight back* Shut up. Don't say anything."};
                case "general" -> new String[] {"*clears his throat, ears red* ...That was unprofessional. ...Do it again sometime.",
                    "*straightens his collar twice* I have commanded whole squads. This is harder."};
                case "roma" -> new String[] {"*for once, no punchline at all, just going pink* ...Huh. That's never happened to me.",
                    "*both hands over her face, peeking through her fingers* ...Do it again before I think of a joke."};
                case "ayato" -> new String[] {"*looking anywhere but at you* ...Whatever. It was fine. It was- shut up.", "*ears going red* Don't tell my sister."};
                default -> new String[] {"*rubs the back of his neck, smiling at the ground* ...Sorry. I'm not very good at this yet.",
                    "*a shy little laugh* ...My heart's going really fast."};
            };
            case MORNING -> switch (key) {
                case "eto" -> new String[] {"*mumbles against your mouth* Morning. You snore. Charmingly. Mostly.",
                    "*stretches like a cat* Breakfast. Then more of that. Or the other way round."};
                case "touka" -> new String[] {"*grumbling* Coffee first. ...Okay, that first. Then coffee.", "*hair everywhere* Don't laugh at my hair. ...Morning."};
                case "general" -> new String[] {"*his voice still rough with sleep* ...Good morning. The bureau can wait an hour.",
                    "*half a smile* I slept. Actually slept. ...Your fault."};
                case "roma" -> new String[] {"*yawns into it* Morning. We survived the night. Statistically impressive.",
                    "*not letting go* Five more minutes. Then I'll be funny."};
                case "ayato" -> new String[] {"*yawns* ...Morning. Don't look at me, I look terrible.", "*not letting go* Five more minutes. Not a request."};
                default -> new String[] {"*smiles, still half asleep* Good morning. ...I could get used to waking up like this.", "*yawns into it* Mm. Morning. Coffee's on me."};
            };
            case RAIN -> switch (key) {
                case "eto" -> new String[] {"*laughing, soaked* Every romance novel ever written, and they were right about this one.",
                    "*wringing out her bandages* Worth it. Completely worth it."};
                case "touka" -> new String[] {"*pushing wet hair out of her face* ...Okay. That was actually kind of perfect. Don't make it weird.",
                    "Idiot. We're soaked. ...Again?"};
                case "general" -> new String[] {"*rain dripping off his cap* ...Thirty years of procedure, and not one manual covers this.",
                    "*makes no move to get out of the rain* Stay a moment longer."};
                case "roma" -> new String[] {"*face up to the rain, laughing* Free shower! Romantic AND hygienic!",
                    "*soaked, grinning, not moving* ...Stay. This is the good part."};
                case "ayato" -> new String[] {"*shakes the rain out of his hair, grinning* ...Don't tell anyone I liked that.", "We're soaked. ...Whatever. Worth it."};
                default -> new String[] {"*laughing, rain running down his face* We should get inside. ...In a minute.", "*brushes wet hair off your forehead* I'll remember this one."};
            };
            case MOVIE -> switch (key) {
                case "eto" -> new String[] {"*her foot pops up behind her* ...And scene. Roll the credits.", "*sighs, theatrically* Five stars. Six."};
                case "touka" -> new String[] {"*her foot lifts before she can stop it* ...That was involuntary. It doesn't count.", "*mortified* Did my foot just- don't answer that."};
                case "general" -> new String[] {"*straightens up slowly* ...I saw that in a film once. I always wanted to.", "*a rare, real smile* Was that adequate?"};
                case "roma" -> new String[] {"*dips you, very theatrical* Ta-da! ...Too much? Never too much.", "*pulls back and takes a little bow* Thank you, thank you, I'm here all week."};
                case "ayato" -> new String[] {"*smirks* What? I've seen movies.", "*pulls back, very pleased with himself* ...Yeah. I'm good at that."};
                default -> new String[] {"*pulls back, a little breathless* I've wanted to do that since the first time I saw you.", "*laughs* Too much? ...Not too much."};
            };
            case NOSE -> switch (key) {
                case "eto" -> new String[] {"*rubs her nose against yours, giggling* Nose kisses. I read about them. Research.", "*bumps your nose with hers* Boop. That's a word now."};
                case "touka" -> new String[] {"*nose to nose, trying not to smile* ...This is stupid. ...Do it again.",
                    "*rubs her nose against yours and looks away fast* Don't say it's cute."};
                case "general" -> new String[] {"*nose against yours, startled into a laugh* ...Where did you learn that?", "*very solemn, nose to nose* Absurd. ...Again."};
                case "roma" -> new String[] {"*nose to nose, crossing her eyes at you* Hiii.", "*boops your nose with hers* Boop. That's the whole joke. I love it."};
                case "ayato" -> new String[] {"*scrunches his nose against yours* ...What is this. ...Fine. It's nice.", "*nose to nose, scowling, failing* Stop smiling."};
                default -> new String[] {"*rubs his nose against yours, laughing* That's my favourite kind.", "*grins, nose to nose* Hi."};
            };
            case GALLANT -> switch (key) {
                case "eto" -> new String[] {"*kisses your knuckles with a flourish* My liege. ...I've always wanted to say that.",
                    "*curtsies, terribly* Your hand, and your heart. In that order."};
                case "touka" -> new String[] {"*kisses your hand quickly, glaring* ...If anyone saw that, you're dead.",
                    "*mutters into your knuckles* ...You looked like you needed it. Shut up."};
                case "general" -> new String[] {"*bows over your hand, every inch the officer* An honour. As always.", "*kisses your knuckles, very correct* At your service."};
                case "roma" -> new String[] {"*kisses your knuckles, then pretends to pocket your ring* ...Kidding. Your hand. Your very nice hand.",
                    "*a sweeping circus bow over your hand* Ladies, gentlemen, and you."};
                case "ayato" -> new String[] {"*kisses your knuckles, then drops your hand fast* ...Don't make that face. People do that.",
                    "*bows, sarcastic, then not sarcastic at all* ...Yours."};
                default -> new String[] {"*bows over your hand and kisses your knuckles* Too old-fashioned? I liked it.", "*a little bow* May I have the rest of the evening too?"};
            };
        };
        if (lines == null) {
            return null;
        }
        String line = lines[Math.floorMod(seed + (held ? 1 : 0) + (pulled ? 1 : 0), lines.length)];
        return line;
    }

    /** What {@code key} says dodging the first try (teasing). */
    public static String dodged(String key, int seed) {
        String[] lines = switch (key) {
            case "eto" -> new String[] {"*leans back at the last second* Too slow! Try again. I'll wait. Maybe.", "Ah-ah. Ask nicely. ...That was nicely. Again."};
            case "touka" -> new String[] {"*ducks away* Ha! Missed. Try again, slowpoke.", "*leans back, smirking* Nope. Again."};
            case "general" -> new String[] {"*straightens his collar, eyes amused* Not on duty.", "*turns his head at the last moment* Earn it."};
            case "roma" -> new String[] {"*sticks her tongue out and ducks* Nope! Earn it!", "*turns her head, cackling* The ringmaster doesn't kiss in the first act."};
            case "ayato" -> new String[] {"*leans back, smirking* Earn it.", "*dodges* Slow. ...Try again."};
            default -> new String[] {"*steps back, laughing* Sorry, sorry. I couldn't help it. Again?", "*dodges, grinning* One more try."};
        };
        return lines[Math.floorMod(seed, lines.length)];
    }
}
