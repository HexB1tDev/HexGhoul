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
        {"Ha! You don't waste time, do you? I like that. Ask me again when you know my name!", "Oh my! Smooth. Very smooth. Let's start with a hello, okay?"},
        {"*turns completely red and looks at the ground* ...W-we just met...", "*hides behind their hair* ...Um. Hi. That was... a lot."},
        {"Bold. I respect bold. Buy me a drink first, stranger.", "Ha. You'll have to do better than that to impress me."},
        {"Oh, you're sweet. But I don't even know your name yet, dear.", "*laughs kindly* Let's be friends first, hm?"},
        {"Who do you think you are? ...Hmph.", "I don't flirt with strangers. Or anyone. Go away. ...Slowly."},
        {"Mm, I like you already. Keep going.", "Oh? A stranger with a silver tongue. Tell me more."},
        {"Oh... are you from a story? People don't say things like that in real life.", "*blinks slowly* That was nice. Say it again, I wasn't ready."},
        {"W-what?! Me? Are you talking to me? Oh no.", "*nearly drops what they're holding* I- um- hello?!"},
        {"Ooh, a challenger! Points for nerve. Minus points for timing!", "Ha! Was that a dare? Who dared you?"},
        {"...Hm. We've only just met.", "*raises an eyebrow* Interesting opening."},
        {"Wow. Does that line usually work?", "Charming. Truly. Do you say that to every stranger at the gate?"},
        {"Oh! A stranger, a flirt, a sunny day... it's like the first chapter of something.", "*hand to chest* Be careful. I fall for that sort of thing."}};

    static final String[][] WARM_FLIRT = {
        {"Hehe, you're cute when you try. Keep trying!", "*giggles* Stop! ...Okay, don't stop.", "You're making me blush and I'm SO bad at hiding it!"},
        {"*goes pink to the ears* ...You mean that?", "*smiles into their sleeve* ...Thank you.", "I- I like when you say things like that. Don't tell anyone."},
        {"Careful. I might take you seriously.", "Ha. Is that all you've got? ...It's working a little.", "Keep that up and you'll have to follow through."},
        {"Oh, you. You always know how to make me smile.", "That's so lovely. You're lovely.", "*touches your arm* You're sweet on me, aren't you?"},
        {"...Don't say things like that. People will hear. ...Say it quieter.", "Hmph. You're lucky I tolerate you.", "*mutters* ...you're not so bad yourself."},
        {"Oh, now we're talking. Come closer and say that again.", "You're trouble. My favourite kind.", "Is that a flirt? Because I'm flirting back."},
        {"You make my head go all cloudy. In a good way.", "*sighs dreamily* Say that under the stars sometime.", "I'll write that down. In the special book."},
        {"*laughs too loud* Oh! Ha! Thank you! Sorry! Ha!", "M-me? Really? You're not joking?", "My heart's going so fast. Is that normal? That's normal."},
        {"Flirting? Ooh, this game I like. My turn: you're cute. I win.", "*pokes your nose* Smooth operator.", "Ha! Ten points! You're getting better at this!"},
        {"*a small smile* ...I don't mind hearing that.", "You're kind with your words.", "...Hm. You make it hard to keep a straight face."},
        {"Was that a compliment? From you? Mark the calendar.", "Oh no. You're actually charming. That's a problem.", "Fine. That was a good one. Don't let it go to your head."},
        {"Oh! That's the sweetest thing! Say it again by moonlight!", "*clasps hands* You're making my heart flutter!", "Is this the part where I swoon? Because I'm swooning."}};

    static final String[][] CLOSE_FLIRT = {
        {"You know exactly what you're doing, don't you? ...Don't stop.", "If you keep looking at me like that, I'm going to kiss you. Fair warning!",
            "*takes your hand and swings it* You're my favourite person. In the whole world."},
        {"*whispers* ...I think about you. A lot. Too much.", "*steps closer, very slowly* ...Okay. I'm brave today. Hi.", "...If you asked me something, I'd say yes."},
        {"You've been flirting with me for weeks. Are you going to do something about it?", "Stop circling. Ask me already.",
            "*pulls you in by the collar* Say that again. Closer."},
        {"I've been hoping you'd say something like that.", "*rests a hand on your cheek* You're very dear to me. You know that?",
            "Come by tonight. I'll make your favourite. ...That's a yes, by the way."},
        {"...You're the only one who gets to talk to me like that. Remember that.", "Hmph. Fine. I like you. Happy now?",
            "*doesn't look at you* ...My heart does a stupid thing when you smile. Stop it."},
        {"Mm, finally serious? I've been waiting for you to catch up.", "Darling, I've been yours for a while. Keep up.",
            "*leans in very close* Ask me out. I dare you."},
        {"Sometimes I think you walked straight out of my dreams.", "When you're near, the whole village goes quiet. Like snow.",
            "I wrote a poem about you. It's terrible. I'll read it to you someday."},
        {"I-I think I really, really like you. There. I said it. Oh no.", "When you're not here I worry. When you're here I'm happy. That's a lot of feelings!",
            "*grabs your sleeve* Don't go yet. Please."},
        {"New game: I like you, you like me, we stop pretending. Ready? Go!", "*hugs your arm* Mine. You're mine. I called it.",
            "Tag! You're it. It means you have to ask me out now. Rules are rules."},
        {"...I'd like to stand beside you. For a long time.", "*takes your hand without a word, and doesn't let go*",
            "I don't say this lightly. I care for you. More than I should."},
        {"Okay, I'll drop the act. I like you. A ridiculous amount.", "You're the only person I can't be sarcastic with. It's infuriating.",
            "Fine. You win. I'm crazy about you. Are you happy?"},
        {"Every story I've ever loved, I see you in now.", "If you asked me to run away with you, I'd pack tonight.",
            "Ask me. Ask me the question. I've been dreaming of it."}};

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
        SWEET("Tell me something sweet.");

        public final String label;

        Partner(String label) {
            this.label = label;
        }
    }

    /** Its answer on {@code topic} to its partner: in its temper's voice, with what it looks for ({@code taste}) and its own {@code dream}. */
    public static String partner(Temper temper, Partner topic, Taste taste, String dream, int seed) {
        return switch (topic) {
            case LOVE -> pick(seed, LOVE_OPEN[temper.ordinal()]) + " " + loves(taste, seed);
            case MET -> pick(seed, MET[temper.ordinal()]);
            case FUTURE -> pick(seed, FUTURE[temper.ordinal()]) + " " + someday(dream);
            case JEALOUS -> pick(seed, JEALOUS[temper.ordinal()]);
            case DANGER -> pick(seed, DANGER[temper.ordinal()]);
            case SWEET -> pick(seed, SWEET[temper.ordinal()]);
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
        {"Me? Jealous? ...When you laugh at someone else's jokes, a little!", "Only when someone else gets your smile!"},
        {"*very small voice* ...Sometimes. When you talk to others for a long time.", "...A little. I'm sorry. I know I shouldn't."},
        {"Jealous? No. But if anyone tries anything, they'll answer to me.", "Let them look. You're coming home with me."},
        {"I try not to be. Your heart is yours. But I'm glad it chose me.", "A little, sometimes. Then I remember you came back to me."},
        {"...Yes. Fine. Don't make me say it twice.", "Hmph. Only when someone stands too close to you. Which is always."},
        {"Jealous? Darling, I'm the competition.", "Only of whoever gets to see you when I can't."},
        {"Sometimes the moon gets to see you when I'm asleep. I'm a little jealous of the moon.", "Only in dreams. Then I wake up and you're here."},
        {"YES. All the time. Is that bad? That's bad. Sorry.", "When you're late I think you've found someone better. Then you come back. Every time."},
        {"Only when you play games with other people! That's MY job.", "If someone else makes you laugh, I'll just make you laugh harder."},
        {"...No. I trust you.", "Jealousy is fear. I'm not afraid of losing you."},
        {"Me? Never. I just glare at everyone you talk to. Totally different.", "Jealous? Of whom? You're stuck with me."},
        {"Of every song that makes you smile that isn't about me!", "A little. It means my heart's awake."}};

    static final String[][] DANGER = {
        {"I'd come running! With a frying pan! And everyone I know!", "I'd ring every bell in the village until help came."},
        {"*grips your sleeve* ...I'd be brave. For you. I don't know how, but I would.", "...I'd come for you. Even if I was shaking the whole way."},
        {"Whoever hurt you would wish they hadn't. That's all.", "I'd put myself between you and it. Without thinking."},
        {"I'd patch you up and sit by your bed until you woke.", "I'd do anything. Anything at all."},
        {"...Don't. Don't get in danger. *quietly* I'd come. Obviously.", "Then I'd be very, very angry. At it. Not you. Mostly not you."},
        {"Then the danger would have to get through me first. I'm surprisingly vicious.", "I'd come for you in my nightclothes if I had to."},
        {"I'd follow you into the dark. Like in the old songs.", "I'd find you. Even past the walls. Even in the dark."},
        {"I'd- I'd panic. And then I'd come anyway. Panicking.", "Don't say that! ...I'd come. I'd be terrified. I'd come."},
        {"Rescue mission! I know every shortcut in this village.", "I'd distract it. I'm very annoying when I want to be."},
        {"I'd stand between you and it.", "I'd come. No question."},
        {"I'd say I told you so. After I saved you.", "Then I'd have to be heroic, which is very inconvenient. I'd do it."},
        {"I'd storm the dark itself to bring you home!", "Every great love story has a rescue. I'd make ours a good one."}};

    static final String[][] SWEET = {
        {"You're the best part of every day. Even the boring ones!", "If you were a flower I'd pick you. Then feel bad. Then keep you anyway!"},
        {"*whispers* ...You're my favourite person.", "...I'm happy. Because of you. That's all."},
        {"You're mine, and I'm proud of it.", "I'd pick you every time. No contest."},
        {"You make this whole village feel like home.", "When you're near, I don't worry about anything."},
        {"...You're not terrible. That's the sweetest thing I've ever said. Treasure it.", "*mumbles* I'm glad you exist. There. Done."},
        {"You're gorgeous, you know. Inside too. Mostly inside.", "Every time you walk in, I forget what I was saying."},
        {"You're like the first warm day of spring.", "When I'm with you, even the rain sounds like music."},
        {"You make me feel safe. I never feel safe. Except with you.", "I-I love you! Sorry. That just came out. I mean it."},
        {"You're my favourite player. In every game. Forever.", "You're the prize! I win!"},
        {"You are my calm.", "...I'm glad it's you."},
        {"You're the only person whose face I'm happy to see. Before noon, even.", "I like you more than coffee. Don't tell coffee."},
        {"You're every love song I ever hummed alone.", "If I could, I'd write your name in the stars."}};

    /** 2.1.4: more of what it says after a kiss, in a hug, and to "I love you." (four more each per temper, beside its first two). */
    static final String[][] KISSED = {
        {"*beams* That was the best one yet!", "Mmh! Sweet!"}, {"*can't stop smiling* ...", "*touches their lips* ...Oh."},
        {"Again. That's an order.", "Mm. You're getting good."}, {"*cups your cheek* There you are.", "That's my favourite welcome."},
        {"...Don't look so pleased with yourself.", "*turns red* ...Fine. That was nice."}, {"Mm. Just like I taught you.", "You taste like trouble."},
        {"Like the moment before the rain.", "*dazed* ...What was I saying?"}, {"I forgot how to breathe!", "*squeaks* Oh!"},
        {"Kiss attack! You lose!", "Ha! Now we're even."}, {"*holds your gaze* ...Good.", "*a slow, rare smile*"},
        {"Ugh, you're sweet. Gross. Again.", "That was barely passable. Practice."}, {"My heart's in pieces. Happy pieces!", "That was a whole chapter."}};

    static final String[][] LOVED = {
        {"I love you more than festival day! And I LOVE festival day!", "Every morning I wake up and remember you're mine. Best feeling ever!"},
        {"*presses their face into your shoulder* ...Me too. So much.", "*barely a whisper* ...You're my whole heart."},
        {"I love you. Don't make me prove it. ...I will, though.", "Yours. Always. Get used to it."},
        {"I love you, dear. Now eat something.", "You're the best thing that ever walked through that gate."},
        {"...Same. Shut up. ...Same.", "*grumbles* I love you. Don't tell the rooster."},
        {"Of course you do. And I love you right back. Wildly.", "Say it again, slower, so I can keep it."},
        {"I love you like the tide loves the moon. Helplessly.", "You're the best dream I never woke up from."},
        {"I love you too! I practise saying it in the mirror! Is that weird?", "Me too, me too, ME TOO."},
        {"I love you a hundred! No, a thousand! Infinity! I win!", "*spins you around* Love you!"},
        {"...I love you. Every day.", "*squeezes your hand once* Always."},
        {"Disgusting. I love you too.", "Ugh, feelings. ...I love you. So much it's embarrassing."},
        {"My heart is yours, every beat of it.", "I love you. I'll tell the stars, the sea, the whole world."}};

    /** It answers "I love you." (its first two lines, or two more). */
    public static String loved(Temper temper, int seed) {
        return Math.floorMod(seed >> 1, 2) == 0 ? HumanPersona.love(temper, seed) : pick(seed, LOVED[temper.ordinal()]);
    }

    static final String[][] HUGGED = {
        {"*squeezes the air out of you* Got you!", "Best hugs in the village, right here!"}, {"*melts into you* ...Mm.", "*holds on, very quietly*"},
        {"*lifts you right off the ground*", "Nobody gets hugs like this but you."}, {"*pats your back* There, there. I've got you.", "You needed that. I could tell."},
        {"...Fine. You can stay there a bit.", "*stiffly, then not stiffly at all*"}, {"Mm, you fit just right.", "Don't let go. That's not a request."},
        {"*sways gently* Like a slow song.", "You smell like the wind today."}, {"*holds on tight* Don't go anywhere.", "Oh, I needed that. I really did."},
        {"Squeeze contest! Ready, go!", "Hug ambush! Gotcha!"}, {"*quiet, steady, warm*", "...Stay a moment."},
        {"Okay, okay, that's enough affection. ...Five more seconds.", "Don't tell anyone I hug."}, {"*sighs into your shoulder* Home.", "Hold me like the end of a film."}};

    /** It hugs (its first two lines, or two more). */
    public static String hugged(Temper temper, int seed) {
        return Math.floorMod(seed >> 1, 2) == 0 ? HumanPersona.hugged(temper, seed) : pick(seed, HUGGED[temper.ordinal()]);
    }

    /** It kisses ({@code KISSED}) and the first two of its own. */
    public static String kissed(Temper temper, int seed) {
        return Math.floorMod(seed >> 1, 2) == 0 ? HumanPersona.kissed(temper, seed) : pick(seed, KISSED[temper.ordinal()]);
    }

    static String pick(int seed, String... lines) {
        return lines[Math.floorMod(seed, lines.length)];
    }
}
