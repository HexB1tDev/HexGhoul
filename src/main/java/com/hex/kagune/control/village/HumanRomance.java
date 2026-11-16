package com.hex.kagune.control.village;

import com.hex.kagune.control.village.HumanPersona.Taste;
import com.hex.kagune.control.village.HumanPersona.Temper;

/**
 * 2.1.4 (pure, tested; the owner: "romance dialog for every personality type"): a village human's romance in its temper's voice: how it
 * takes being flirted with (a stranger's try, a friend's, a close friend's; a kind no if the player isn't who it falls for), and what its
 * partner can talk with it about (what it loves about them, by what it looks for; the day they met; their future, with its own dream in
 * it; whether it gets jealous; what it would do if they were in danger; something sweet). Every temper says each of these its own way.
 */
public final class HumanRomance {
    private HumanRomance() {
    }

    /** Regard a flirt brings once a day, when it lands (more for the flirty and the romantic: {@link HumanPersona#liking}). */
    public static int flirted(Temper temper, boolean lands) {
        return lands ? Math.max(1, HumanPersona.liking(temper) - 1) : 0;
    }

    /** How it takes a flirt at {@code regard}, {@code attracted} or not to the player. */
    public static String flirt(Temper temper, int regard, boolean attracted, int seed) {
        if (!attracted) {
            return pick(seed, NOT_TYPE[temper.ordinal()]);
        }
        String[][] by = regard >= HumanPersona.CLOSE ? CLOSE_FLIRT : regard >= HumanPersona.WARM ? WARM_FLIRT : STRANGER_FLIRT;
        return pick(seed, by[temper.ordinal()]);
    }

    // In temper order: CHEERFUL, SHY, BOLD, KIND, GRUMPY, FLIRTY, DREAMY, NERVOUS, PLAYFUL, STOIC, SARCASTIC, ROMANTIC.

    static final String[][] STRANGER_FLIRT = {
        {"Ha! You don't waste time, do you? I like that. Ask me again when you know my name!", "Oh my! Smooth. Very smooth. Let's start with a hello, okay?", "Well, hello! That made a dull afternoon a lot more interesting. Tell me something about yourself."},
        {"*turns completely red and looks at the ground* ...W-we just met...", "*hides behind their hair* ...Um. Hi. That was... a lot.", "...That was kind. A little overwhelming. Could we start with a normal conversation?"},
        {"Bold. I respect bold. Buy me a drink first, stranger.", "Ha. You'll have to do better than that to impress me.", "You've got nerve. I like nerve. Now tell me something that isn't a pickup line."},
        {"Oh, you're sweet. But I don't even know your name yet, dear.", "*laughs kindly* Let's be friends first, hm?", "That's lovely of you. Let's get to know each other before we start making promises."},
        {"Who do you think you are? ...Hmph.", "I don't flirt with strangers. Or anyone. Go away. ...Slowly.", "Flattery? At this hour? ...Hmph. At least your timing's different."},
        {"Mm, I like you already. Keep going.", "Oh? A stranger with a silver tongue. Tell me more.", "A compliment and a smile? Darling, you came prepared. Let's see if you can hold a conversation."},
        {"Oh... are you from a story? People don't say things like that in real life.", "*blinks slowly* That was nice. Say it again, I wasn't ready.", "That sounded like the beginning of a poem. I hope the next verse has your name in it."},
        {"W-what?! Me? Are you talking to me? Oh no.", "*nearly drops what they're holding* I- um- hello?!", "Oh! I wasn't ready for that. Do I say something charming now? I've forgotten every word I know."},
        {"Ooh, a challenger! Points for nerve. Minus points for timing!", "Ha! Was that a dare? Who dared you?", "First encounter and you're already rolling for charm? Bold strategy!"},
        {"...Hm. We've only just met.", "*raises an eyebrow* Interesting opening.", "We should talk first. I prefer knowing who is making me smile."},
        {"Wow. Does that line usually work?", "Charming. Truly. Do you say that to every stranger at the gate?", "A pickup line in the wild. I thought those were extinct. Tell me your name, brave explorer."},
        {"Oh! A stranger, a flirt, a sunny day... it's like the first chapter of something.", "*hand to chest* Be careful. I fall for that sort of thing.", "That's a lovely beginning. Let's not skip the chapter where we learn each other's names."}};

    static final String[][] WARM_FLIRT = {
        {"Hehe, you're cute when you try. Keep trying!", "*giggles* Stop! ...Okay, don't stop.", "You're making me blush and I'm SO bad at hiding it!", "I was having a good day already. Then you showed up and made it unfairly better."},
        {"*goes pink to the ears* ...You mean that?", "*smiles into their sleeve* ...Thank you.", "I- I like when you say things like that. Don't tell anyone.", "...I practised something nice to say back. You smiled and I forgot it."},
        {"Careful. I might take you seriously.", "Ha. Is that all you've got? ...It's working a little.", "Keep that up and you'll have to follow through.", "Keep looking at me like that and I might start looking for you in a crowd."},
        {"Oh, you. You always know how to make me smile.", "That's so lovely. You're lovely.", "*touches your arm* You're sweet on me, aren't you?", "You remember the little things I tell you. That's the kind of charm that stays with me."},
        {"...Don't say things like that. People will hear. ...Say it quieter.", "Hmph. You're lucky I tolerate you.", "*mutters* ...you're not so bad yourself.", "You again. Making my stupid day better. Very inconsiderate of you."},
        {"Oh, now we're talking. Come closer and say that again.", "You're trouble. My favourite kind.", "Is that a flirt? Because I'm flirting back.", "You're getting dangerously good at finding excuses to stand close to me."},
        {"You make my head go all cloudy. In a good way.", "*sighs dreamily* Say that under the stars sometime.", "I'll write that down. In the special book.", "I've been daydreaming. You keep wandering into it and rearranging the scenery."},
        {"*laughs too loud* Oh! Ha! Thank you! Sorry! Ha!", "M-me? Really? You're not joking?", "My heart's going so fast. Is that normal? That's normal.", "I get less nervous around you. Except when you say things like that. Then it's worse. The nice kind."},
        {"Flirting? Ooh, this game I like. My turn: you're cute. I win.", "*pokes your nose* Smooth operator.", "Ha! Ten points! You're getting better at this!", "You made me blush. That's cheating. I'm filing a very affectionate complaint."},
        {"*a small smile* ...I don't mind hearing that.", "You're kind with your words.", "...Hm. You make it hard to keep a straight face.", "I noticed you coming before you said anything. I was pleased."},
        {"Was that a compliment? From you? Mark the calendar.", "Oh no. You're actually charming. That's a problem.", "Fine. That was a good one. Don't let it go to your head.", "I was perfectly immune to charm before you started being annoyingly considerate."},
        {"Oh! That's the sweetest thing! Say it again by moonlight!", "*clasps hands* You're making my heart flutter!", "Is this the part where I swoon? Because I'm swooning.", "If I blush any harder, the whole village will know who I've been hoping to see."}};

    static final String[][] CLOSE_FLIRT = {
        {"You know exactly what you're doing, don't you? ...Don't stop.", "If you keep looking at me like that, I'm going to kiss you. Fair warning!",
            "*takes your hand and swings it* You're my favourite person. In the whole world.", "I keep making room beside me without thinking. I think you should know that."},
        {"*whispers* ...I think about you. A lot. Too much.", "*steps closer, very slowly* ...Okay. I'm brave today. Hi.", "...If you asked me something, I'd say yes.", "...I wanted to reach for your hand. Could I? I don't want to keep wondering."},
        {"You've been flirting with me for weeks. Are you going to do something about it?", "Stop circling. Ask me already.",
            "*pulls you in by the collar* Say that again. Closer.", "I like you. No guessing game. Tell me if you feel it too."},
        {"I've been hoping you'd say something like that.", "*rests a hand on your cheek* You're very dear to me. You know that?",
            "Come by tonight. I'll make your favourite. ...That's a yes, by the way.", "You don't have to impress me today. Come as you are. That's who I hoped would visit."},
        {"...You're the only one who gets to talk to me like that. Remember that.", "Hmph. Fine. I like you. Happy now?",
            "*doesn't look at you* ...My heart does a stupid thing when you smile. Stop it.", "Sit closer. I'm not saying it a second time. ...Please."},
        {"Mm, finally serious? I've been waiting for you to catch up.", "Darling, I've been yours for a while. Keep up.",
            "*leans in very close* Ask me out. I dare you.", "I've run out of subtle hints. I want to see where this goes, with you."},
        {"Sometimes I think you walked straight out of my dreams.", "When you're near, the whole village goes quiet. Like snow.",
            "I wrote a poem about you. It's terrible. I'll read it to you someday.", "I used to imagine a face beside me in all my little futures. It's yours now."},
        {"I-I think I really, really like you. There. I said it. Oh no.", "When you're not here I worry. When you're here I'm happy. That's a lot of feelings!",
            "*grabs your sleeve* Don't go yet. Please.", "I really like you. I wrote that down in case I couldn't say it. ...I said it. Holy shit."},
        {"New game: I like you, you like me, we stop pretending. Ready? Go!", "*hugs your arm* Mine. You're mine. I called it.",
            "Tag! You're it. It means you have to ask me out now. Rules are rules.", "New rule: we stop pretending these are accidental meetings. I keep hoping for them."},
        {"...I'd like to stand beside you. For a long time.", "*takes your hand without a word, and doesn't let go*",
            "I don't say this lightly. I care for you. More than I should.", "I would like this to be more than friendship. You can take time to answer."},
        {"Okay, I'll drop the act. I like you. A ridiculous amount.", "You're the only person I can't be sarcastic with. It's infuriating.",
            "Fine. You win. I'm crazy about you. Are you happy?", "Fine, here's the unfunny truth: I look forward to you. All the damn time."},
        {"Every story I've ever loved, I see you in now.", "If you asked me to run away with you, I'd pack tonight.",
            "Ask me. Ask me the question. I've been dreaming of it.", "I'm ready to stop hiding the dedication. It was always going to have your name in it."}};

    static final String[][] NOT_TYPE = {
        {"Haha, you're sweet! But you're not really my type. Best friends, though?", "Aww! You're lovely. Just not like that, for me."},
        {"*goes red* ...Oh. I'm... I'm sorry. Not like that.", "*hides their face* You're kind. But... no. I'm sorry."},
        {"Not my type, friend. Still like you, though.", "Ha. Nice try. Wrong door."},
        {"You're very dear, but my heart goes another way. I'm sorry.", "That's sweet of you. I'm not the one for you, though."},
        {"...No. Not like that. But you can stay.", "Hmph. Flattered. Not interested. Still friends."},
        {"Mm, I love the attention, but you're not who I go for. Keep it coming anyway.", "Sorry, darling. I'm not wired that way. But I'll flirt back for fun."},
        {"In another story, maybe. Not this one.", "My heart's always pointed somewhere else. I'm sorry."},
        {"Oh! Oh no. I'm sorry! I don't- you're not- we're friends! Okay? Friends!", "I- I'm so sorry! Not like that! Still friends?"},
        {"Ha! Wrong team, buddy. Still my favourite playmate!", "Nope! But nice try. Ten points for courage."},
        {"No. But I'm honoured.", "...That's not how I feel. But you have my friendship."},
        {"Bold. Also: not my type. Still friends, I suppose.", "Aw, cute. No."},
        {"Oh, you're darling! But my heart's written for someone else's kind of story.", "If only! You're just not who I dream of."}};

    // ---------------------------------------------------------------- a partner's talk

    /** What a partner can talk about with it, and the page's words for each. */
    public enum Partner {
        LOVE("What do you love about me?"),
        MET("Do you remember when we met?"),
        FUTURE("Where do you see us in a year?"),
        JEALOUS("Do you ever get jealous?"),
        DANGER("What if I were in danger?"),
        SWEET("Tell me something sweet."),
        FEELINGS("How are you feeling about us?"),
        BABY("Could we have a baby someday?"),
        BED("Come to bed with me.");

        public final String label;

        Partner(String label) {
            this.label = label;
        }
    }

    /** Its answer on {@code topic} to its partner: in its temper's voice, with what it looks for ({@code taste}) and its own {@code dream}. */
    public static String partner(Temper temper, Partner topic, Taste taste, String dream, int seed) {
        return partner(temper, topic, taste, dream, HumanPartnerTalk.Context.ORDINARY, seed);
    }

    /** 3.1.4: additions use the current spouse/time/hurt context; the original topics retain their own pools. */
    public static String partner(Temper temper, Partner topic, Taste taste, String dream, HumanPartnerTalk.Context context, int seed) {
        return switch (topic) {
            case LOVE -> pick(seed, LOVE_OPEN[temper.ordinal()]) + " " + loves(taste, seed);
            case MET -> pick(seed, MET[temper.ordinal()]);
            case FUTURE -> pick(seed, FUTURE[temper.ordinal()]) + " " + someday(dream);
            case JEALOUS -> pick(seed, JEALOUS[temper.ordinal()]);
            case DANGER -> pick(seed, DANGER[temper.ordinal()]);
            case SWEET -> pick(seed, SWEET[temper.ordinal()]);
            case FEELINGS, BABY, BED -> HumanPartnerTalk.reply(temper, topic, context, seed);
        };
    }

    /** What it loves in them: what it looks for, found. */
    static String loves(Taste taste, int seed) {
        return switch (taste) {
            case STRONG -> pick(seed, "You're strong. When you're here, the night stays outside.", "Nobody's ever made me feel this safe.");
            case GENTLE -> pick(seed, "You're gentle. You listen. You actually listen.", "The way you hold things. Like they might break. Like I might.");
            case FUNNY -> pick(seed, "You make me laugh until my sides hurt. Every single day.", "You can turn the worst day into a joke we'll tell for years.");
            case MYSTERIOUS -> pick(seed, "There's always more of you to find. I'll never get to the bottom of you. I love that.",
                "You've got secrets. I can see them behind your eyes. I don't mind. I like a puzzle.");
            case CLEVER -> pick(seed, "Your mind. The way you see things nobody else does.", "You're the cleverest person I know. It's very attractive. Don't tell anyone.");
            case DARING -> pick(seed, "You're a little dangerous. Don't pretend you're not. I like it.", "You'd walk into the dark for me. Maybe you already have.");
            case LOYAL -> pick(seed, "You always come back. Every time. Nobody's ever always come back.", "I know you'll never leave. I've never known that before.");
            case CARING -> pick(seed, "You notice when I'm tired before I do.", "You took care of me when I was sick and you didn't even complain. Much.");
        };
    }

    /** Its dream, said as a someday with them in it. */
    static String someday(String dream) {
        String d = dream.startsWith("I want ") ? dream.substring("I want ".length()) : dream;
        int stop = d.indexOf('.');
        String first = stop > 0 ? d.substring(0, stop) : d;
        return "And someday... I'd still like " + first + ". With you beside me, this time.";
    }

    static final String[][] LOVE_OPEN = {
        {"Oh, everything! But mostly...", "Only a thousand things! Number one:"}, {"*very quietly* ...", "*hides their smile* ...Okay. Um."},
        {"Easy.", "You want a list? Fine."}, {"*takes both your hands* Let me tell you.", "Oh, love..."},
        {"...Don't make me say mushy things. *sighs* Fine.", "Ugh. Okay. Once."}, {"Mm, fishing for compliments? I'll bite.", "Where do I start?"},
        {"*looks at you like you're the moon* ...", "Mm... if I had to put it into words..."}, {"Oh! Um! Okay! So!", "*takes a breath* Okay. Okay."},
        {"Ooh, a quiz about you! I'll ace it.", "Easy! Number one:"}, {"*thinks, then says it simply*", "Simply put:"},
        {"Oh, we're being sincere now? Gross. ...Fine.", "Don't laugh."}, {"Oh, my love, I could write a book.", "*hand on heart* Everything. But if I must choose..."}};

    static final String[][] MET = {
        {"Of course! You walked in looking so lost and I thought: I'm going to be their friend whether they like it or not!",
            "You smiled at me and I tripped over a bucket. I've been falling ever since!"},
        {"*smiles at the ground* I remember. I hid behind the well. You found me anyway.", "...You said hello. Nobody ever says hello to me first."},
        {"I thought you were trouble. I was right. Best trouble I've ever had.", "You didn't back down from me. Nobody does that."},
        {"You looked so tired. I wanted to feed you. I still do.", "I remember thinking: there's someone who needs looking after. And then you looked after me."},
        {"I told you to go away. You didn't. Stubborn. I liked that. Don't tell anyone.", "...Yes. I remember. You were annoying. You still are. I love it."},
        {"I winked at you and you didn't notice. I had to work SO hard after that.", "I knew the moment I saw you. I just let you think it was your idea."},
        {"It was a cloudy day and you looked like the only bright thing in it.", "I think I dreamt about you before we ever met. Is that strange?"},
        {"I was so nervous I forgot my own name. You laughed. Nicely. I'll never forget it.", "You asked me if I was okay. I wasn't. Then I was."},
        {"I hid your boot as a joke. You found it. I knew you were the one.", "I challenged you to a race. You let me win. I noticed. I loved it."},
        {"...Every detail. The weather. Your face. What you said.", "I remember. I don't forget the important things."},
        {"I said something rude and you laughed. That's when I knew I was doomed.", "You were insufferable. I married— well. You know. I'm here."},
        {"Like the first page of my favourite book. I could recite it.", "The light was golden, you looked up, and my whole heart turned over."}};

    static final String[][] FUTURE = {
        {"In a year? A house full of flowers and noise and you in the middle of it!", "Still here! Still happy! Still laughing at your terrible jokes!"},
        {"...Somewhere quiet. You and me. That's all I want.", "*hesitates* ...Still holding your hand. If you'll let me."},
        {"Same as now. You, me, and nobody daring to get in our way.", "Stronger. Both of us. Together."},
        {"Taking care of each other. A warm kitchen. A full table.", "Growing old, slowly, together. That's my wish."},
        {"...Same place. Same you. Don't change.", "Hmph. Still putting up with you. Gladly."},
        {"In a year? You'll be even more hopelessly mine.", "Somewhere with a big bed and nobody knocking."},
        {"Somewhere the sky's very wide. With you counting stars beside me.", "In a little house at the edge of a story."},
        {"Alive! Safe! Together! That's the whole plan!", "I hope... I hope you're still here. You'll still be here, right?"},
        {"Still playing games! You'll finally beat me at one. Maybe.", "Adventures! Little ones. Inside the walls. Mostly."},
        {"Beside you. That's enough.", "Here. With you. Watching the seasons turn."},
        {"Probably still arguing about whose turn it is to cook. Bliss.", "Older. Grumpier. Still yours. Unfortunately for you."},
        {"Married, maybe? Too soon? It's never too soon in my head.", "Every day a new page. Every page with you on it."}};

    static final String[][] JEALOUS = {
        {"Me? Jealous? ...When you laugh at someone else's jokes, a little!", "Only when someone else gets your smile!", "Sometimes. It stings, and I want to tell you before I turn it into a bad joke. Can we save some time for us?"},
        {"*very small voice* ...Sometimes. When you talk to others for a long time.", "...A little. I'm sorry. I know I shouldn't.", "...Yes. I get quiet when it happens. I still love you. I'm just afraid to ask if you remember I'm here."},
        {"Jealous? No. But if anyone tries anything, they'll answer to me.", "Let them look. You're coming home with me.", "Yeah. I'll say it straight: I want some attention. I'm not breaking us over a bruised ego."},
        {"I try not to be. Your heart is yours. But I'm glad it chose me.", "A little, sometimes. Then I remember you came back to me.", "I can be jealous and still love you kindly. Please help me talk instead of swallowing it."},
        {"...Yes. Fine. Don't make me say it twice.", "Hmph. Only when someone stands too close to you. Which is always.", "Damn right I get jealous. Doesn't mean I stopped loving you. It means I'd like you to look this way occasionally."},
        {"Jealous? Darling, I'm the competition.", "Only of whoever gets to see you when I can't.", "I like being wanted by you. When I feel forgotten, the teasing gets a little thin. I'd rather tell you."},
        {"Sometimes the moon gets to see you when I'm asleep. I'm a little jealous of the moon.", "Only in dreams. Then I wake up and you're here.", "Sometimes I imagine myself fading out of your story. I know imagination isn't proof. A little reassurance helps."},
        {"YES. All the time. Is that bad? That's bad. Sorry.", "When you're late I think you've found someone better. Then you come back. Every time.", "Yes. Then I overthink being jealous too. Could you hold my hand while I get my thoughts straight?"},
        {"Only when you play games with other people! That's MY job.", "If someone else makes you laugh, I'll just make you laugh harder.", "Sometimes I feel like I'm competing in a game nobody explained. I'd rather sit beside you and talk."},
        {"...No. I trust you.", "Jealousy is fear. I'm not afraid of losing you.", "Yes. I won't punish you for a feeling. I would like us to make time for each other."},
        {"Me? Never. I just glare at everyone you talk to. Totally different.", "Jealous? Of whom? You're stuck with me.", "Occasionally. Shocking discovery: the sarcastic one also has feelings. I'd rather tell you than weaponise them."},
        {"Of every song that makes you smile that isn't about me!", "A little. It means my heart's awake.", "I do. I still choose you while I'm jealous. I want us to speak honestly about the ache, too."}};

    static final String[][] DANGER = {
        {"I'd come running! With a frying pan! And everyone I know!", "I'd ring every bell in the village until help came.", "I'd be scared as hell, honestly. Then I'd do the useful thing and get help. You matter more than looking brave."},
        {"*grips your sleeve* ...I'd be brave. For you. I don't know how, but I would.", "...I'd come for you. Even if I was shaking the whole way.", "...I'd be frightened. I would still come looking for help. I couldn't just leave you."},
        {"Whoever hurt you would wish they hadn't. That's all.", "I'd put myself between you and it. Without thinking.", "I'd get you somewhere safe first. We can argue about reckless bullshit after you're breathing easily."},
        {"I'd patch you up and sit by your bed until you woke.", "I'd do anything. Anything at all.", "I'd help you, and find someone who knows more than I do. Love doesn't make me a healer. It makes me stay."},
        {"...Don't. Don't get in danger. *quietly* I'd come. Obviously.", "Then I'd be very, very angry. At it. Not you. Mostly not you.", "I'd swear. A lot. Then I'd get help. Don't mistake being pissed off for not being scared for you."},
        {"Then the danger would have to get through me first. I'm surprisingly vicious.", "I'd come for you in my nightclothes if I had to.", "I'd stop flirting long enough to be useful. That's how you'd know it was serious, darling."},
        {"I'd follow you into the dark. Like in the old songs.", "I'd find you. Even past the walls. Even in the dark.", "I'd want to do something heroic. I'd settle for something real that brought you safely home."},
        {"I'd- I'd panic. And then I'd come anyway. Panicking.", "Don't say that! ...I'd come. I'd be terrified. I'd come.", "I'd panic. I'd probably talk too fast. I'd still get someone to help you. I'm not leaving you alone."},
        {"Rescue mission! I know every shortcut in this village.", "I'd distract it. I'm very annoying when I want to be.", "No games then. I'd get help and keep you company. We can make terrible jokes when you're safe."},
        {"I'd stand between you and it.", "I'd come. No question.", "Get you to safety. Find help. Stay until you had it."},
        {"I'd say I told you so. After I saved you.", "Then I'd have to be heroic, which is very inconvenient. I'd do it.", "I'd retire the jokes and find help. Being witty is a shit first-aid kit."},
        {"I'd storm the dark itself to bring you home!", "Every great love story has a rescue. I'd make ours a good one.", "I'd be terrified. I'd still want my next choice to bring you closer to home, not closer to danger."}};

    static final String[][] SWEET = {
        {"You're the best part of every day. Even the boring ones!", "If you were a flower I'd pick you. Then feel bad. Then keep you anyway!", "You don't have to make every day amazing. I like the ordinary ones with you too."},
        {"*whispers* ...You're my favourite person.", "...I'm happy. Because of you. That's all.", "...When you say my name gently, I stop worrying about how to be someone else."},
        {"You're mine, and I'm proud of it.", "I'd pick you every time. No contest.", "I want you. The difficult days included. That's the part I mean most."},
        {"You make this whole village feel like home.", "When you're near, I don't worry about anything.", "Being loved shouldn't be another job you're failing at. Come sit. You can rest with me."},
        {"...You're not terrible. That's the sweetest thing I've ever said. Treasure it.", "*mumbles* I'm glad you exist. There. Done.", "You're my favourite pain in the ass. That's affectionate. Don't ruin it by asking me to repeat it."},
        {"You're gorgeous, you know. Inside too. Mostly inside.", "Every time you walk in, I forget what I was saying.", "Darling, you've made coming home feel more tempting than going anywhere interesting."},
        {"You're like the first warm day of spring.", "When I'm with you, even the rain sounds like music.", "I had a hundred pretty futures. Then I found the one where we're both tired and making each other tea."},
        {"You make me feel safe. I never feel safe. Except with you.", "I-I love you! Sorry. That just came out. I mean it.", "I don't have the perfect words. I have the very real wish that you'll still be here when I find them."},
        {"You're my favourite player. In every game. Forever.", "You're the prize! I win!", "You can lose your keys, your argument, and the whole damn afternoon. You still haven't lost me."},
        {"You are my calm.", "...I'm glad it's you.", "I choose the life we're making. Even when it's quiet."},
        {"You're the only person whose face I'm happy to see. Before noon, even.", "I like you more than coffee. Don't tell coffee.", "You're my favourite person to do absolutely nothing with. That's a high bloody honour."},
        {"You're every love song I ever hummed alone.", "If I could, I'd write your name in the stars.", "I love the grand moments. I love the way you turn up on an ordinary morning even more."}};

    /** 2.1.4: more of what it says after a kiss, in a hug, and to "I love you." (four more each per temper, beside its first two). */
    static final String[][] KISSED = {
        {"*beams* That was the best one yet!", "Mmh! Sweet!", "Well. There goes every sensible thought I was trying to have!"}, {"*can't stop smiling* ...", "*touches their lips* ...Oh.", "...Could you stay close while I remember how words work?"},
        {"Again. That's an order.", "Mm. You're getting good.", "Yeah. That's worth being distracted for."}, {"*cups your cheek* There you are.", "That's my favourite welcome.", "That was lovely. Thank you for a soft moment in a noisy day."},
        {"...Don't look so pleased with yourself.", "*turns red* ...Fine. That was nice.", "...Damn it. You know exactly how to make me smile."}, {"Mm. Just like I taught you.", "You taste like trouble.", "Mm. You are getting very good at saying hello properly."},
        {"Like the moment before the rain.", "*dazed* ...What was I saying?", "For a moment there, the whole village went quiet in my head."}, {"I forgot how to breathe!", "*squeaks* Oh!", "Okay. Breathe. People do that. ...Could we do that again sometime?"},
        {"Kiss attack! You lose!", "Ha! Now we're even.", "You win this round. Rematch highly encouraged."}, {"*holds your gaze* ...Good.", "*a slow, rare smile*", "Stay a moment. I liked that."},
        {"Ugh, you're sweet. Gross. Again.", "That was barely passable. Practice.", "Well, that was unfairly persuasive. My brilliant comeback has left the building."}, {"My heart's in pieces. Happy pieces!", "That was a whole chapter.", "There. That's the moment I'm going to carry through the rest of today."}};

    static final String[][] LOVED = {
        {"I love you more than festival day! And I LOVE festival day!", "Every morning I wake up and remember you're mine. Best feeling ever!", "I love you too. Even when the day is a mess and we're both in terrible moods."},
        {"*presses their face into your shoulder* ...Me too. So much.", "*barely a whisper* ...You're my whole heart.", "...I love you. I'm saying it first next time. I've been practising."},
        {"I love you. Don't make me prove it. ...I will, though.", "Yours. Always. Get used to it.", "Good. I love you too. Come here and let me stop pretending I'm composed."},
        {"I love you, dear. Now eat something.", "You're the best thing that ever walked through that gate.", "I love you too, sweetheart. There isn't a useful thing you need to do to deserve hearing it."},
        {"...Same. Shut up. ...Same.", "*grumbles* I love you. Don't tell the rooster.", "...Love you too. A fucking lot, actually. Don't look so surprised."},
        {"Of course you do. And I love you right back. Wildly.", "Say it again, slower, so I can keep it.", "I love you, darling. Flirting with you is fun. Staying with you is the part I mean."},
        {"I love you like the tide loves the moon. Helplessly.", "You're the best dream I never woke up from.", "I love you. My daydreams have started using the word 'we' without asking me."},
        {"I love you too! I practise saying it in the mirror! Is that weird?", "Me too, me too, ME TOO.", "I love you too. I worry I say it awkwardly. I hope you can hear how much I mean it."},
        {"I love you a hundred! No, a thousand! Infinity! I win!", "*spins you around* Love you!", "Love you too. No scorekeeping. No clever catch. Just you."},
        {"...I love you. Every day.", "*squeezes your hand once* Always.", "I love you. Today as well as the easy days."},
        {"Disgusting. I love you too.", "Ugh, feelings. ...I love you. So much it's embarrassing.", "I love you too. There goes my carefully cultivated air of indifference. Worth it."},
        {"My heart is yours, every beat of it.", "I love you. I'll tell the stars, the sea, the whole world.", "I love you. Not only the story of us. The laundry and the tired evenings too."}};

    /** It answers "I love you." (its first two lines, or two more). */
    public static String loved(Temper temper, int seed) {
        return Math.floorMod(seed >> 1, 2) == 0 ? HumanPersona.love(temper, seed) : pick(seed, LOVED[temper.ordinal()]);
    }

    static final String[][] HUGGED = {
        {"*squeezes the air out of you* Got you!", "Best hugs in the village, right here!", "Oh, that's good. I needed that more than I was letting on."}, {"*melts into you* ...Mm.", "*holds on, very quietly*", "...Thank you for not making me find a brave way to ask."},
        {"*lifts you right off the ground*", "Nobody gets hugs like this but you.", "Hold on a little. I'm not finished enjoying this."}, {"*pats your back* There, there. I've got you.", "You needed that. I could tell.", "You can put the day down for a moment. I'll stay right here."},
        {"...Fine. You can stay there a bit.", "*stiffly, then not stiffly at all*", "...Fine. This is helping. Don't get smug about being right."}, {"Mm, you fit just right.", "Don't let go. That's not a request.", "Now this is an excellent interruption. More of these, please."},
        {"*sways gently* Like a slow song.", "You smell like the wind today.", "You feel like the safe place at the end of a very long story."}, {"*holds on tight* Don't go anywhere.", "Oh, I needed that. I really did.", "My thoughts were running everywhere. This helps them sit down."},
        {"Squeeze contest! Ready, go!", "Hug ambush! Gotcha!", "Emergency hug successful. Patient requests unnecessary additional hugs."}, {"*quiet, steady, warm*", "...Stay a moment.", "Thank you. I needed somewhere to rest a moment."},
        {"Okay, okay, that's enough affection. ...Five more seconds.", "Don't tell anyone I hug.", "Terrible news: this is working better than my plan to complain about everything."}, {"*sighs into your shoulder* Home.", "Hold me like the end of a film.", "Stay a little. I want to remember how quiet the world can be with you."}};

    /** It hugs (its first two lines, or two more). */
    public static String hugged(Temper temper, int seed) {
        return Math.floorMod(seed >> 1, 2) == 0 ? HumanPersona.hugged(temper, seed) : pick(seed, HUGGED[temper.ordinal()]);
    }

    /** It kisses ({@code KISSED}) and the first two of its own. */
    public static String kissed(Temper temper, int seed) {
        return Math.floorMod(seed >> 1, 2) == 0 ? HumanPersona.kissed(temper, seed) : pick(seed, KISSED[temper.ordinal()]);
    }

    /** 2.3.4: a partner given a ring, by temper. */
    static final String[][] PROPOSED = {
        {"YES! Yes yes yes! Oh, I'm going to cry. I'm crying. YES!", "Is that... oh! Yes! A thousand times yes!"},
        {"*covers their face* ...Yes. Yes. I can't look at you. Yes.", "I... yes. I've never wanted anything more. Yes."},
        {"About time you asked. Yes. Put it on me.", "Yes. And I'm telling the whole village. Right now."},
        {"Oh, my dear. Yes. Of course yes. Come here.", "Yes. I'll look after you for the rest of my life. And you me."},
        {"...Fine. Yes. Don't make that face. YES, alright?", "Took you long enough. ...Yes. Obviously yes."},
        {"A ring? You really do know the way to my heart. Yes.", "Yes. Now kiss me like you mean it."},
        {"I dreamed this. Exactly this. Yes... say it again so I know I'm awake.", "Yes. Under every star, yes."},
        {"Y-you mean it? Really? ...Yes! Yes, sorry, yes!", "I'm shaking. Yes. Please don't take it back."},
        {"Ha! Is this a trick? ...It's not? YES!", "Yes! Race you to the altar!"},
        {"...Yes.", "Yes. For the rest of my life."},
        {"Oh no, a commitment. Terrifying. ...Yes, you idiot. Yes.", "Well, someone has to marry you. Yes."},
        {"Yes, my love. A thousand times, in every lifetime.", "I've waited my whole life for this question. Yes."}};

    /** 2.3.4: what a partner says to a ring. */
    public static String proposed(Temper temper, int seed) {
        return pick(seed, PROPOSED[temper.ordinal()]);
    }

    /** 2.3.4: left leaning in for a kiss that never came, by temper. */
    static final String[][] HANGING = {
        {"...Oh. Okay! Later, then!", "Hey! You owe me one."}, {"*opens their eyes, red* ...S-sorry. Never mind.", "...Okay."},
        {"Really? Fine. Your loss.", "Coward. ...Later."}, {"That's alright. Another time, dear.", "Shy today? That's okay."},
        {"Hmph. Forget it.", "Unbelievable."}, {"Playing hard to get? Cute.", "I'll make you regret that. Sweetly."},
        {"...Oh. I was somewhere else anyway.", "The moment floated off."}, {"D-did I do something wrong?", "Sorry, sorry. Never mind."},
        {"Ha! You chickened out!", "Missed your chance!"}, {"...Another time.", "...Hm. Later."},
        {"Wow. Left hanging. Great.", "Okay, that was humiliating. For you."}, {"*sighs* The moment's gone...", "Next time, then, my love."}};

    /** 2.3.4: what a partner says when a kiss never came. */
    public static String hanging(Temper temper, int seed) {
        return pick(seed, HANGING[temper.ordinal()]);
    }

    static String pick(int seed, String... lines) {
        return lines[Math.floorMod(seed, lines.length)];
    }
}
