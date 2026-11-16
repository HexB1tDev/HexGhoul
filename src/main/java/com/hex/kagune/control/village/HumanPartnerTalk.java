package com.hex.kagune.control.village;

import com.hex.kagune.control.village.HumanPersona.Temper;
import com.hex.kagune.control.village.HumanRomance.Partner;

/**
 * 3.1.4: extra adult partner conversations, in all twelve temperaments. Context is read at each answer, not a new persistent mood.
 * Family planning and bedtime here are conversations; they do not create children, teleport anyone or add an intimacy mechanic.
 */
public final class HumanPartnerTalk {
    private HumanPartnerTalk() {
    }

    public record Context(boolean married, boolean night, boolean hurt, boolean upset) {
        public static final Context ORDINARY = new Context(false, false, false, false);
    }

    /** Only an adult spouse is offered family planning/private invitations. Existing topics keep their old visibility. */
    public static boolean available(Partner topic, Context c) {
        return topic != Partner.BABY && topic != Partner.BED || c.married();
    }

    /** Context-sensitive additions. No sexual invitation is answered warmly while hurt/upset or before marriage. */
    public static String reply(Temper temper, Partner topic, Context c, int seed) {
        if (!available(topic, c)) {
            return "Let's save that conversation for when we're married.";
        }
        if (c.upset()) {
            return pick(temper, ANGRY, seed);
        }
        if (c.hurt()) {
            return pick(temper, HURT, seed);
        }
        return switch (topic) {
            case FEELINGS -> pick(temper, TENDER, seed);
            case BABY -> pick(temper, BABY, seed);
            case BED -> pick(temper, c.night() ? BED : BED_DAY, seed);
            default -> throw new IllegalArgumentException("Not an added partner topic: " + topic);
        };
    }

    private static String pick(Temper temper, String[][] pool, int seed) {
        String[] lines = pool[temper.ordinal()];
        return lines[Math.floorMod(seed, lines.length)];
    }

    private static final String[][] ANGRY = {
        {"I'm trying not to turn being hurt into a joke. Can you hear me out?", "I'm upset. I still love you. I'd like both of those things to be allowed in the same room."},
        {"...I'm hurt. I know I get very quiet. Please don't take that as me having nothing to say.", "Could you let me finish? It took me a lot of nerve to bring this up."},
        {"I'm pissed off. Let's say what's wrong instead of taking cheap shots at each other.", "I want a straight conversation. The anger doesn't get to make the decisions for us."},
        {"I'm upset, dear. Being gentle doesn't mean something didn't hurt me.", "I need us to listen to each other before we try to make this a nice evening."},
        {"I'm angry as hell. Still love you. Sit down and let me get the actual point out.", "Don't call it a bad mood to get out of hearing what happened. That's bullshit."},
        {"The flirting can wait. I want to know you hear me when I'm not being charming.", "I'm hurt, darling. I would rather be honest than smile until I resent you."},
        {"I keep rehearsing a kinder version of this. The truth is that I'm hurt, and I need you to listen.", "Can we talk before my imagination makes this worse? I don't want to mistake a fear for a fact."},
        {"I'm upset, and now I'm worrying that being upset will upset you. Can we slow down?", "Please let me say this badly before I lose the nerve to say it at all."},
        {"No jokes for a minute. That actually hurt. I want to explain why.", "I like playing with you. I don't like feeling like my feelings are the joke."},
        {"I'm angry. I need a little space, then an honest conversation.", "This matters to me. I would like you to hear the whole thing."},
        {"I could make a devastating remark. It would fix precisely fuck all. I'd rather tell you what hurt.", "I'm dropping the sarcasm for a minute. Please don't drop the conversation with it."},
        {"I'm hurt. I still want us. That's why I want to talk rather than pretend everything is lovely.", "Love shouldn't mean never disagreeing. Let me tell you what's wrong without making it an ending."},
    };

    private static final String[][] HURT = {
        {"I'm trying to be cheerful, but I need a quiet minute to recover.", "Not my brightest day. Sitting with me would help more than a pep talk."},
        {"...I'm not feeling very strong. Could we just be quiet together?", "I don't want to make a fuss. I do need a little time to feel all right again."},
        {"I need rest. I'd rather admit that than pretend I'm too tough for it.", "Give me a minute. I've taken enough hits for one damn day."},
        {"A little sore and shaken. Your company would be lovely; nothing more demanding just now.", "Let's make this a gentle evening. I haven't much strength to spare."},
        {"I feel like shit. I'm allowed to say that without you trying to make me cheerful.", "Rest first. I'll be my charmingly miserable self again later."},
        {"The charm is temporarily out of service, darling. I'd still appreciate your company.", "I want a quiet night. Being wanted is nice; being allowed to rest is nicer right now."},
        {"I feel a little far away. Sit with me while I find my feet again?", "The day was rougher than I expected. I want somewhere calm to land."},
        {"I'm shaky. Could you talk about something ordinary? It helps me come back down.", "I need a little rest. Please don't read anything awful into that."},
        {"Bad day. No bonus round. Just a quiet seat beside you, please.", "I'd like a hug and a break from pretending I can make a joke about everything."},
        {"Still recovering. Quiet company would help.", "Rest first. We can make plans once I'm steadier."},
        {"I'm sore, tired, and fresh out of witty ways to improve it. Sit with me?", "My current romantic ambition is a chair that doesn't hurt and you nearby."},
        {"I want to feel safe and still for a while. Sharing that with you would be enough.", "A gentle evening, please. Even love stories need somewhere to rest."},
    };

    private static final String[][] TENDER = {
        {"Happy you're here. Even the chores look less annoying with you beside me.", "I was saving the best part of my day to tell you. You're also the best part. Slight problem."},
        {"...Good. Quietly good. It's easier to be myself when I don't have to be impressive.", "I still get shy about telling you I need you. I hope I'm getting better at it."},
        {"I want more ordinary time with you. We don't have to wait for an occasion.", "I like our life. I want us to keep choosing it, even when it takes some work."},
        {"Loved, and grateful. A little tired too. All three can be true.", "I want us both to be able to say what we need before we're worn out."},
        {"Better with you here. Don't make a fuss. I can be soft once a day.", "Tired of everybody. Except you. Mostly. Come sit, you pain in the ass."},
        {"Like stealing a little of your attention and giving you a very good reason to forgive me.", "Happy to be yours. Also very interested in getting you to stop standing so far away."},
        {"Like the future isn't a faraway picture anymore. It's in all these little things we do.", "Peaceful. The kind where I don't have to imagine a better place to be."},
        {"Happy. Nervous about saying it too loudly, but happy.", "I keep looking for something to worry about, then you smile and I get a moment off."},
        {"Like teaming up for the boring quests too. Especially the laundry boss.", "Good. Very fond of you. Mildly suspicious that you've stolen all the comfortable seats."},
        {"Steady. Glad we're together.", "I would like more time with you. No special occasion required."},
        {"Annoyingly happy. You've made cynicism much less convincing.", "I like us. Even the part where we both pretend the dishes will do themselves."},
        {"Loved in all the small ways I used to overlook. I want to give that back to you.", "Like I get to keep falling for the person I already chose."},
    };

    private static final String[][] BED_DAY = {
        {"Tempting! Save that look for tonight when we can have some privacy.", "You are making it hard to focus. Ask me again after the day is done."},
        {"...Tonight? Somewhere private? I'd like time to stop blushing first.", "Could we save that for tonight? I don't know where to put my face right now."},
        {"Tonight. In private. I want time for us, not a rushed distraction.", "Good idea. Wrong time of day. Come find me later."},
        {"Later, sweetheart. Let's finish the day, then make some time that's only ours.", "I'd like a private evening with you. We don't have to hurry to get there."},
        {"Tonight. I'm not abandoning the whole damn day because you gave me that look.", "You're distracting. Deliberately, I'd bet. Ask me when we've got some privacy."},
        {"Oh, I like the offer. Keep that thought warm until tonight, darling.", "You are a very persuasive distraction. Tonight, behind a closed door."},
        {"When the lamps are low and nobody needs us. I'd like a night that isn't rushed.", "Tonight? Let me look forward to finding you at the end of the day."},
        {"Tonight, maybe? I want to stop thinking about all the things we still have to do first.", "I like the idea. I just need a quieter moment to actually enjoy it."},
        {"Side quest accepted for tonight. Main quest: finish the day without grinning at each other.", "Private evening slot reserved. No trying to skip the rest of the day, cheater."},
        {"Tonight. In private.", "Later. I want unhurried time with you."},
        {"An excellent proposal. The sun and our responsibilities have filed objections. Try tonight.", "Tonight. I would like to make one good decision before you distract me from every other one."},
        {"Save that invitation for tonight. I want to give it my whole attention.", "A night just for us sounds lovely. Let's get there without rushing past the day."},
    };

    private static final String[][] BED = {
        {"Come to bed, then. Just us, door closed. I have been looking forward to you all evening.", "Yes. Leave the chores. Tonight I want my spouse, not another job to finish."},
        {"...Yes. Come to bed with me. I mean more than sleep, if you want that too.", "I'd like that. Close the door and hold my hand while I find the courage to stop talking."},
        {"Yes. Come to bed. I'm asking for a private night with you, and I think you know it.", "Shut the door, come here. I want you tonight, if you're feeling the same."},
        {"I'd love that, sweetheart. A private night together, with nothing we have to prove.", "Yes. Come close. Tell me if you're tired; I want us both to enjoy being here."},
        {"Come to bed. With me. Don't make me spell it out like we're strangers.", "Yes, damn it. I've been looking forward to you all evening. Shut the door and come here."},
        {"Come to bed, darling. I promise that invitation is every bit as deliberate as it sounds.", "Yes. The door closes, the day ends, and I get you all to myself for a while."},
        {"Yes. Come to bed and let the rest of the world be somewhere else tonight.", "I've been imagining a night just for us. I'd rather have the real thing, with you."},
        {"Yes. I'm nervous, but the happy kind. Come to bed and we can take our time.", "I'd like that. Just us, somewhere private. You can tell me if you've changed your mind."},
        {"Bed. You and me. No pretending I only meant a nap this time.", "Yes. Door closed, distractions gone. I am being very serious about a very good evening."},
        {"Yes. Come to bed with me. I want you close tonight.", "I'd like a private night with you. Take my hand."},
        {"Yes. Come to bed. For once, I have no clever alternative to exactly what you meant.", "The chores can go to hell for one night. Shut the door and come kiss your spouse."},
        {"Yes, my love. Come to bed. I want a night that's ours in every quiet, private way.", "I'd love that. Let's close the door and stop giving the rest of the world our attention."},
    };

    private static final String[][] BABY = {
        {"A baby with you? The thought makes me grin. Let's talk about whether we're ready for the tired bits too.", "I'd like a family someday. Not just the cute picture: the getting up, the worrying, the bringing everyone home."},
        {"...I've thought about it. A little one with us. Could we talk about it slowly, without deciding everything tonight?", "I'd like to talk about a baby. I'm nervous about being a parent, not about wanting a future with you."},
        {"A baby? I like the idea. Let's be honest about what we'd both need before we make that choice.", "If we do this someday, we do it as a team. Nights, chores, all of it. I won't promise only the easy parts."},
        {"I like that hope, sweetheart. Let's talk about the care a child would need, and what we both want.", "If we become parents, I want home to be somewhere our child can always come back to."},
        {"A kid with us? ...Damn. That's a big thought. Let's talk properly before you catch me smiling about it.", "I'd want to be sure we could both show up for a kid. Every day. Even the shitty ones."},
        {"A baby? You just found a way to make me blush without a single pickup line. Let's talk about it.", "I like imagining a family with you. I want the real, messy version to get as much thought as the lovely one."},
        {"I imagine a little voice asking us impossible questions. I'd like to talk about making room for that someday.", "If we have a child, I want them to grow up knowing they can come home after the worst day."},
        {"A baby? Oh. I've imagined it, then worried about imagining it. Could we talk before my brain runs a mile ahead?", "I'd want us to be ready to help each other. I don't think being scared means we can't talk about it."},
        {"A tiny teammate? That's a huge conversation. Let's have it properly, without me hiding in the joke.", "I'd want to be the parent who brings them home, listens, and doesn't turn every mistake into a boss fight."},
        {"I'd like to discuss it. A child deserves both of us to be ready.", "A family is a serious hope. Tell me what you want, and what worries you."},
        {"A baby? There goes my ability to make a joke. I'd like to talk about it, including the hard bits.", "If we become parents, I want us to share the work. Love is lovely. Sleep deprivation is a bastard."},
        {"I'd love to talk about a family with you. A real one, with tired mornings as well as beautiful firsts.", "A child with you is a precious thought. Let's give it time, care, and an honest conversation."},
    };
}
