package com.hex.kagune.control.village;

import java.util.List;
import java.util.Set;

/**
 * 2.1.3 (pure, tested; the owner: "we gotta add wayyyyy more options and text to each personality and make it feel like every NPC is
 * unique"): a village human's life, its own for life from its seed (each part from its own bits, so no part drags another along): a job
 * (with what it does all day, how its day went, and the wares it sells), family, what it does for fun, what scares it, what it dreams of,
 * an odd habit, a secret it tells only a close friend, the one gift that wins it over, and what it thinks of ghouls. Its temper
 * ({@link HumanPersona.Temper}) is how it says all of this ({@link HumanVoice}); this is what it has to say.
 *
 * <p>Every line here is the human's own, first person, and fits any temper: the temper's voice goes around it.
 */
public final class HumanLife {
    private HumanLife() {
    }

    // ---------------------------------------------------------------- names (2.1.3: a woman's or a man's)

    /** Women's names and men's (none of them anyone's from the stories). */
    static final String[] WOMEN = {"Aiko", "Akane", "Ayaka", "Emiko", "Fumiko", "Hana", "Haruka", "Hikari", "Hiromi", "Kaede", "Kana", "Kasumi", "Keiko",
        "Kiyomi", "Kumiko", "Mai", "Mariko", "Megumi", "Michiko", "Midori", "Misaki", "Momoko", "Nanami", "Natsuki", "Noriko", "Reiko", "Sachiko",
        "Sakura", "Sayuri", "Shiori", "Sumire", "Tomoko", "Umeko", "Yoko", "Yumi", "Yuna", "Yuriko", "Ayumi", "Chiyo", "Eri", "Fuyumi", "Hotaru", "Kiku",
        "Mayu", "Nozomi", "Rumi", "Tamaki", "Asami"};
    static final String[] MEN = {"Daichi", "Daisuke", "Eiji", "Goro", "Hajime", "Haruto", "Hiroki", "Hiroshi", "Ichiro", "Isamu", "Jiro", "Jun", "Kaito",
        "Katsu", "Kazuki", "Koji", "Kota", "Kyo", "Masaru", "Minoru", "Noboru", "Osamu", "Ryota", "Satoshi", "Shigeru", "Shin", "Shota", "Sota",
        "Tadashi", "Takumi", "Taro", "Tetsu", "Tsuyoshi", "Yasu", "Yuji", "Yusuke", "Yuto", "Ryo", "Akio", "Fumio", "Genji", "Hideo", "Mamoru", "Nobu",
        "Shiro", "Tomo", "Wataru", "Yoichi"};

    /** How many names a woman (or a man) may be given. */
    public static int names(boolean woman) {
        return woman ? WOMEN.length : MEN.length;
    }

    /** Name {@code index} of a woman's (or a man's). */
    public static String name(boolean woman, int index) {
        String[] all = woman ? WOMEN : MEN;
        return all[Math.floorMod(index, all.length)];
    }

    // ---------------------------------------------------------------- the job

    /**
     * What it does: its title, where, what it says of its work (four), how its day went (three), and what it sells ({@code wares}: its
     * trade's own, the first always offered).
     */
    public enum Job {
        BAKER("baker", "the bakery",
            s("I bake the village's bread. I'm up before the roosters, every day, kneading in the dark.",
                "Bread, buns, and on festival days, honey cake. My hands smell of yeast even in my sleep.",
                "The oven's older than I am. It has moods. I talk to it. It listens, mostly.",
                "Everyone comes to the bakery sooner or later. I know the whole village by what they buy."),
            s("The oven was cranky this morning. Burnt the first batch. The birds ate well.", "Sold out of buns before noon. A good day.",
                "Flour everywhere. In my hair, my shoes, my soul."),
            w("minecraft:bread", "bread", 6, 1), w("minecraft:cookie", "cookies", 12, 1), w("minecraft:cake", "cake", 1, 3),
            w("minecraft:pumpkin_pie", "pumpkin pie", 2, 1), w("minecraft:wheat", "wheat", 12, 1), w("minecraft:sugar", "sugar", 12, 1),
            w("minecraft:egg", "eggs", 8, 1)),
        FARMER("farmer", "the fields",
            s("I farm the east fields. Wheat, carrots, potatoes. The ground and I have an understanding.",
                "I plant, I weed, I pray for rain, I pray against too much rain. That's farming.",
                "My grandfather farmed these same rows. I found his old hoe in the shed. Still sharp.",
                "If the village eats, it's because somebody got muddy. That's me. I'm the somebody."),
            s("The carrots came up crooked. They taste the same. Don't tell anyone.", "Crows again. I've started naming them.",
                "Rain at last. The whole field drank it up."),
            w("minecraft:carrot", "carrots", 12, 1), w("minecraft:potato", "potatoes", 12, 1), w("minecraft:beetroot", "beetroots", 12, 1),
            w("minecraft:wheat_seeds", "wheat seeds", 16, 1), w("minecraft:beetroot_seeds", "beetroot seeds", 16, 1),
            w("minecraft:hay_block", "hay bales", 2, 1), w("minecraft:pumpkin", "pumpkins", 4, 1)),
        BEEKEEPER("beekeeper", "the hives by the orchard",
            s("I keep the bees behind the orchard. Thirty hives. I know the queens by their temper.",
                "The bees sting everyone but me. I think they know I'd never take more than they can spare.",
                "Honey, wax, candles. Everything sweet and everything bright in this village goes through my hands.",
                "You have to move slowly around bees. I've moved slowly my whole life. It suits me."),
            s("A swarm left this morning. I followed it halfway to the river and talked it home.", "Three stings today. A good day, for me.",
                "The clover's blooming. The honey will taste like summer."),
            w("minecraft:honey_bottle", "honey bottles", 2, 1), w("minecraft:honeycomb", "honeycomb", 3, 1), w("minecraft:candle", "candles", 3, 1),
            w("minecraft:glass_bottle", "glass bottles", 6, 1), w("minecraft:sweet_berries", "sweet berries", 12, 1),
            w("minecraft:sunflower", "sunflowers", 2, 1)),
        FLORIST("florist", "the flower stall",
            s("I sell flowers in the square. People buy them for love, for apologies, for funerals. I can always tell which.",
                "Every flower means something. Poppies for remembering. Daisies for new starts. I could tell you all of them.",
                "I grow most of them myself, behind the house. The rest I find where nobody's looking.",
                "Some days I sell more flowers for funerals than for weddings. Those are the long days."),
            s("Someone bought every red rose I had. Somebody's in big trouble, or big love.", "The tulips opened all at once this morning.",
                "Slow day. I made crowns out of the leftover daisies."),
            w("minecraft:poppy", "poppies", 4, 1), w("minecraft:oxeye_daisy", "daisies", 4, 1), w("minecraft:cornflower", "cornflowers", 4, 1),
            w("minecraft:allium", "alliums", 4, 1), w("minecraft:lily_of_the_valley", "lilies of the valley", 3, 1), w("minecraft:white_tulip", "white tulips", 3, 1),
            w("minecraft:bone_meal", "bone meal", 12, 1), w("minecraft:cocoa_beans", "cocoa beans", 8, 1)),
        SMITH("smith", "the forge",
            s("I work the forge. Hinges, nails, hoes, buckets. The CCG's blades I'm not allowed near.",
                "Iron listens if you're patient and loud at the same time. Like most people.",
                "My arms are burned from wrist to elbow. Every mark's a lesson. I've learned a lot.",
                "When the night watch needs a lock that holds, they come to me. I make them hold."),
            s("Burned my thumb. Again. The forge wins most days.", "Mended four buckets and a broken heart. The buckets were easier.",
                "Good iron came in. I've been happy all day. Don't tell anyone."),
            w("minecraft:iron_ingot", "iron ingot", 1, 2), w("minecraft:bucket", "bucket", 1, 2), w("minecraft:shears", "shears", 1, 2),
            w("minecraft:iron_hoe", "iron hoe", 1, 3), w("minecraft:lantern", "lantern", 1, 2), w("minecraft:flint_and_steel", "flint and steel", 1, 2),
            w("minecraft:chain", "chains", 4, 1)),
        CARPENTER("carpenter", "the woodshop",
            s("I build things. Doors, chairs, coffins, cradles. The same wood, depending on the week.",
                "Every house on this street has something of mine in it. Usually the door. I'm proud of the doors.",
                "You learn to listen to wood. It tells you where it wants to split.",
                "I made my own bed. Then I made my own table. Then I got carried away and made a boat. It's in the shed."),
            s("Splinter the size of a nail. Pulled it out with my teeth.", "Finished a cradle today. Someone's having a baby. Good news.",
                "Sanded all morning. My arms are made of sawdust now."),
            w("minecraft:stick", "sticks", 32, 1), w("minecraft:oak_planks", "oak planks", 16, 1), w("minecraft:bowl", "bowls", 8, 1),
            w("minecraft:ladder", "ladders", 6, 1), w("minecraft:chest", "chest", 1, 2), w("minecraft:oak_sapling", "oak saplings", 4, 1),
            w("minecraft:composter", "composter", 1, 2)),
        TAILOR("tailor", "the sewing room above the inn",
            s("I sew. Coats, dresses, the CCG's spare uniforms when they rip them chasing things.",
                "I can tell your whole life from your clothes. Don't worry. I'm too polite to say it.",
                "A good seam is invisible. A good tailor is too. Nobody thanks the thread.",
                "I've sewn every wedding dress in this village for twenty years. I cry at every one."),
            s("Pricked my finger eleven times. A personal best.", "Sold a scarf to a stranger. It looked better on them than it did on me.",
                "Patched three coats with holes I didn't ask about."),
            w("minecraft:string", "string", 8, 1), w("minecraft:white_wool", "white wool", 4, 1), w("minecraft:leather", "leather", 4, 1),
            w("minecraft:red_dye", "red dye", 4, 1), w("minecraft:blue_dye", "blue dye", 4, 1), w("minecraft:leather_boots", "leather boots", 1, 2)),
        SCRIBE("scribe", "the records office",
            s("I keep the village records. Births, deaths, marriages, debts. Everyone's life in my handwriting.",
                "People come to me to write letters they can't write themselves. I've written more love letters than anyone alive.",
                "I know every name in this village going back eighty years. Most of them are gone now.",
                "Ink stains, paper cuts, and a back that hurts. Glamorous work."),
            s("Wrote a letter for someone to their son. I'm not crying. It's the ink.", "Found a mistake in the 'deaths' book. Someone's alive. Good day.",
                "Copied the same page nine times. My hand's asleep."),
            w("minecraft:paper", "paper", 16, 1), w("minecraft:book", "books", 2, 1), w("minecraft:writable_book", "book and quill", 1, 2),
            w("minecraft:ink_sac", "ink sacs", 4, 1), w("minecraft:feather", "feathers", 8, 1), w("minecraft:candle", "candles", 3, 1)),
        CANDLEMAKER("candle-maker", "the little shop with the smoky window",
            s("I make candles. Every light in every window after dark, somebody bought it from me.",
                "People think ghouls hate light. I don't know if that's true. I make the candles anyway. Just in case.",
                "Tallow, wax, wick. Pour, wait, cut. It's slow. I like slow things.",
                "I make special ones for the dead. A candle for every name. The shop never really goes dark."),
            s("Melted a whole batch. The shop smells like a birthday.", "Sold a candle to a little girl who's afraid of the dark. I gave her two.",
                "The wicks were too short. Everything was too short today."),
            w("minecraft:candle", "candles", 4, 1), w("minecraft:torch", "torches", 16, 1), w("minecraft:lantern", "lantern", 1, 2),
            w("minecraft:coal", "coal", 8, 1), w("minecraft:honeycomb", "honeycomb", 3, 1), w("minecraft:string", "string", 8, 1)),
        MILLER("miller", "the mill on the stream",
            s("I run the mill. The wheel turns, the stones grind, the village eats. I just keep it all from falling apart.",
                "The mill's been in my family for four generations. I'm the first one who can swim.",
                "I hear the wheel even when I'm asleep. If it stops, I wake up.",
                "Flour, sugar, the odd sack of something someone wants ground. Ask, and I'll grind it."),
            s("A fish got into the wheel. It's fine. The fish is not.", "The stream's high. The wheel's singing.", "Ground forty sacks today. My ears are ringing."),
            w("minecraft:wheat", "wheat", 12, 1), w("minecraft:sugar", "sugar", 12, 1), w("minecraft:bread", "bread", 6, 1),
            w("minecraft:sugar_cane", "sugar cane", 8, 1), w("minecraft:egg", "eggs", 8, 1), w("minecraft:bucket", "bucket", 1, 2)),
        COOK("cook", "the inn's kitchen",
            s("I cook at the inn. Stew for the travellers, soup for the sick, cake for anyone who looks sad.",
                "Food is how you tell someone they're safe. I tell a lot of people they're safe.",
                "I've cooked for the CCG when they pass through. They eat like they haven't in weeks. Maybe they haven't.",
                "I can't sing and I can't dance, but I can make a potato taste like a holiday."),
            s("Someone sent the soup back. I sent it back to them. Hotter.", "Ran out of mushrooms. The stew wept.",
                "Made a pie so good the innkeeper hugged me. He never hugs."),
            w("minecraft:baked_potato", "baked potatoes", 8, 1), w("minecraft:mushroom_stew", "mushroom stew", 1, 1),
            w("minecraft:beetroot_soup", "beetroot soup", 1, 1), w("minecraft:brown_mushroom", "brown mushrooms", 6, 1),
            w("minecraft:red_mushroom", "red mushrooms", 6, 1), w("minecraft:apple", "apples", 6, 1), w("minecraft:bowl", "bowls", 8, 1)),
        SHEPHERD("shepherd", "the hill pasture",
            s("I keep the sheep up on the hill. And the cows. And one goat that thinks it's a sheep.",
                "Milk in the morning, wool in the spring, and I count them every night. Every single night.",
                "The animals are calmer than people. They don't pretend. If they're scared, they run.",
                "I sleep up on the hill in summer. It's just me, the flock and the stars."),
            s("Lost a lamb this morning. Found it asleep in the hay.", "The goat ate my hat. I liked that hat.", "Sheared four sheep. I'm mostly wool now."),
            w("minecraft:milk_bucket", "bucket of milk", 1, 2), w("minecraft:white_wool", "white wool", 4, 1), w("minecraft:egg", "eggs", 8, 1),
            w("minecraft:leather", "leather", 4, 1), w("minecraft:hay_block", "hay bales", 2, 1), w("minecraft:feather", "feathers", 8, 1)),
        POTTER("potter", "the kiln behind the well",
            s("I throw pots. Bowls, jugs, the flower pots on every sill. Mud, fire, patience.",
                "Every pot I make is a little crooked. I've decided that's my signature.",
                "Clay remembers every touch. Press too hard, it shows forever. People are like that too.",
                "The kiln's going day and night. I sleep next to it in winter."),
            s("Two pots cracked in the kiln. One came out perfect. That's a good ratio.", "Mud up to my elbows. Happy.",
                "Someone dropped a jug I made ten years ago. I was oddly proud it lasted."),
            w("minecraft:flower_pot", "flower pots", 2, 1), w("minecraft:bowl", "bowls", 8, 1), w("minecraft:clay_ball", "clay", 12, 1),
            w("minecraft:brick", "bricks", 12, 1), w("minecraft:glass_bottle", "glass bottles", 6, 1), w("minecraft:decorated_pot", "decorated pot", 1, 2)),
        MASON("mason", "the quarry road",
            s("I'm a mason. The walls around this village? Half of them are mine. The good half.",
                "Stone doesn't lie. If a wall's going to fall, it tells you first. People should listen more.",
                "I built the well. And the bridge. And my own house, twice. The first one I don't talk about.",
                "Every morning I walk the wall and check it. If a ghoul comes in, it won't be through my stones."),
            s("A stone cracked clean in half. Satisfying. Don't ask.", "Fixed a hole in the north wall. Something dug it. Something big.",
                "Hauled stone all day. My back's three hundred years old."),
            w("minecraft:cobblestone", "cobblestone", 32, 1), w("minecraft:stone_bricks", "stone bricks", 16, 1), w("minecraft:bricks", "bricks", 8, 1),
            w("minecraft:torch", "torches", 16, 1), w("minecraft:clay_ball", "clay", 12, 1), w("minecraft:stone_shovel", "stone shovel", 1, 1)),
        HERBALIST("herbalist", "the green house by the creek",
            s("I gather herbs. For tea, for fevers, for people who can't sleep. Most people can't sleep, lately.",
                "There's a mushroom in the woods that glows. I won't tell you where. It's mine.",
                "I mend people the CCG doesn't have time for. Cuts, colds, broken hearts. Tea helps all three.",
                "The forest's full of medicine if you know how to look. And poison, if you don't."),
            s("Found a patch of glow berries by the creek. Best day this week.", "Made a tea that cured a cough. Then a tea that caused one.",
                "Picked herbs until my knees gave out."),
            w("minecraft:sweet_berries", "sweet berries", 12, 1), w("minecraft:glow_berries", "glow berries", 8, 1),
            w("minecraft:brown_mushroom", "brown mushrooms", 6, 1), w("minecraft:red_mushroom", "red mushrooms", 6, 1),
            w("minecraft:glass_bottle", "glass bottles", 6, 1), w("minecraft:bone_meal", "bone meal", 12, 1), w("minecraft:kelp", "kelp", 12, 1)),
        NIGHT_WATCH("night watch", "the gate",
            s("I keep the night watch at the gate. Lantern, horn, and a lot of staring at the dark.",
                "Most nights nothing happens. I've learned to love nothing happening.",
                "I've seen things out there. Eyes, red, in the tree line. They never come closer. Not yet.",
                "I sleep all day and the whole village thinks I'm lazy. They're asleep when I'm keeping them alive."),
            s("Quiet night. Beautiful, boring, quiet night.", "Something walked the tree line for an hour. Then it left. I'm still shaking.",
                "Fell asleep at my post. Don't tell the elder. Please."),
            w("minecraft:arrow", "arrows", 16, 1), w("minecraft:torch", "torches", 16, 1), w("minecraft:bow", "bow", 1, 3),
            w("minecraft:lantern", "lantern", 1, 2), w("minecraft:shield", "shield", 1, 4), w("minecraft:flint", "flint", 10, 1)),
        INNKEEPER("innkeeper", "the inn",
            s("I keep the inn. Beds, food, gossip. The gossip's free.",
                "Travellers bring news from everywhere. I know what's happening three villages over before they do.",
                "I've had CCG investigators, merchants and one man who I'm still not sure was human. Everyone pays the same.",
                "The inn was my mother's. She said: never ask a guest where they're from. I've kept that rule."),
            s("A traveller paid in fish. I took it.", "Full house tonight. Somebody's snoring already.", "Someone left without paying. I hope they're alright."),
            w("minecraft:bread", "bread", 6, 1), w("minecraft:baked_potato", "baked potatoes", 8, 1), w("minecraft:apple", "apples", 6, 1),
            w("minecraft:honey_bottle", "honey bottles", 2, 1), w("minecraft:milk_bucket", "bucket of milk", 1, 2), w("minecraft:white_bed", "bed", 1, 3)),
        TEACHER("teacher", "the schoolhouse",
            s("I teach the village children. Letters, numbers, and how to hide when the bell rings twice.",
                "The children ask me if ghouls are real. I tell them the truth, gently. That's the hardest lesson I teach.",
                "Some of my first pupils have children of their own now. I teach those too. I feel ancient.",
                "Chalk dust, spilled ink and thirty questions a minute. I wouldn't trade it."),
            s("A boy brought me a frog. In his pocket. During arithmetic.", "Everyone passed their reading test. I nearly cried.",
                "The little ones were loud today. I'm still hearing them."),
            w("minecraft:paper", "paper", 16, 1), w("minecraft:book", "books", 2, 1), w("minecraft:writable_book", "book and quill", 1, 2),
            w("minecraft:apple", "apples", 6, 1), w("minecraft:white_dye", "white dye", 4, 1), w("minecraft:map", "map", 1, 2)),
        GRAVEDIGGER("gravedigger", "the cemetery on the hill",
            s("I dig the graves. Somebody has to. I try to make them nice. Straight edges. A good view.",
                "People cross the street when they see me. I don't blame them. I'd cross the street too.",
                "Some graves come back up empty. I don't talk about that. The CCG asked me not to.",
                "I talk to the dead while I work. They're good listeners. Better than most of the living."),
            s("Dug two today. Neither of them old. A bad day.", "Quiet up on the hill. The flowers are coming in nicely.",
                "Someone left a note on a grave. I didn't read it. I wanted to."),
            w("minecraft:bone", "bones", 8, 1), w("minecraft:bone_meal", "bone meal", 12, 1), w("minecraft:candle", "candles", 3, 1),
            w("minecraft:stone_shovel", "stone shovel", 1, 1), w("minecraft:poppy", "poppies", 4, 1), w("minecraft:lantern", "lantern", 1, 2)),
        FISHER("fisher", "the riverbank",
            s("I fish the river. Lines, nets, a lot of waiting. I've done my best thinking with my feet in the water.",
                "The river gives what it gives. Some days nothing. Some days a boot. Once, a ring.",
                "I sell kelp and string here. The fish go to the inn. Ghouls don't eat fish, you know. It's the safest job there is.",
                "Fog on the river at dawn. I've seen ghosts in it. Or herons. Mostly herons."),
            s("Caught nothing all morning. Then everything at once.", "Lost my best hook to something big. I'll get it back.",
                "The river's low. The fish are sulking."),
            w("minecraft:kelp", "kelp", 12, 1), w("minecraft:dried_kelp", "dried kelp", 12, 1), w("minecraft:string", "string", 8, 1),
            w("minecraft:fishing_rod", "fishing rod", 1, 2), w("minecraft:ink_sac", "ink sacs", 4, 1), w("minecraft:lily_pad", "lily pads", 4, 1),
            w("minecraft:water_bucket", "bucket of water", 1, 2)),
        MERCHANT("merchant", "the market",
            s("I buy and sell. A bit of everything. If you need it, I've probably got it, or I know someone who does.",
                "I travel between villages twice a year. Every time I come back, there are fewer people. I count.",
                "Everything has a price. Except a good conversation. Those I give away.",
                "I've sold to farmers, nobles, investigators. A coin's a coin. A smile's extra."),
            s("Sold a broken lantern as an antique. It is an antique. It's also broken.", "Business is slow. Talk to me, at least.",
                "Haggled with a child for ten minutes. The child won."),
            w("minecraft:apple", "apples", 6, 1), w("minecraft:sweet_berries", "sweet berries", 12, 1), w("minecraft:paper", "paper", 16, 1),
            w("minecraft:coal", "coal", 8, 1), w("minecraft:leather", "leather", 4, 1), w("minecraft:glass_bottle", "glass bottles", 6, 1),
            w("minecraft:cocoa_beans", "cocoa beans", 8, 1), w("minecraft:sugar", "sugar", 12, 1));

        public final String title;
        public final String place;
        final String[] work;
        final String[] day;
        public final List<VillageShop.Offer> wares;

        Job(String title, String place, String[] work, String[] day, VillageShop.Offer... wares) {
            this.title = title;
            this.place = place;
            this.work = work;
            this.day = day;
            this.wares = List.of(wares);
        }

        /** What it says of its work. */
        public String work(int seed) {
            return pick(seed, this.work);
        }

        /** How its day went. */
        public String day(int seed) {
            return pick(seed, this.day);
        }
    }

    // ---------------------------------------------------------------- the rest of its life

    static final String[] FAMILY = {
        "I live with my grandmother. She's ninety-one and still beats me at cards.",
        "It's just me. My parents went to the city when I was small and never came back. I stopped waiting a long time ago.",
        "I have four younger brothers. The house is never quiet. I love it. I'd never say so.",
        "My twin lives in the next village. We write every week. We say the same things, mostly.",
        "My father was night watch before me. The CCG brought his lantern home. Just the lantern.",
        "I was married once. The winter took them. ...I don't say the name out loud anymore.",
        "My mother runs half this village and doesn't know it. Ask anyone. They're all a little afraid of her.",
        "I'm the youngest of seven. Nobody's ever remembered my birthday on the right day.",
        "My older sister joined the CCG. She sends letters with half the words blacked out.",
        "I was found on the inn's steps as a baby. The innkeeper's mother raised me. That's family enough.",
        "My grandfather tells the same story every night. I've heard it a thousand times. I'd hear it a thousand more.",
        "I have a little daughter. She's the only reason I'm still brave.",
        "My brother went into the woods one night and didn't come out. We don't know what happened. We think we do.",
        "My parents still hold hands at dinner. Forty years. I want that. I don't know if it exists twice.",
        "It's me and my cat. The cat's in charge.",
        "My aunt raised me. She was strict, and she was right about everything, and I miss her.",
        "Big family. Loud, nosy, always in each other's business. Exhausting. I'd die for any of them.",
        "I've got a little brother who wants to be an investigator. I'm trying to talk him out of it.",
        "My mother and I don't speak. It's a long story. A boring one, mostly. Just sad.",
        "I live with my best friend from childhood. People assume things. We let them.",
        "My father's going blind. I read to him every night. He pretends he can still see the pictures.",
        "I have three children. All grown. All far away. The house echoes.",
        "My grandmother says our family's been in this valley since before the walls. She says a lot of things.",
        "I'm the only one left. Fever, years back. The house is too big now. I keep every room clean anyway."};

    static final String[] HOBBY = {
        "I collect stones that look like faces. I have forty-one. One looks like the elder. Don't tell him.",
        "I carve little animals out of wood. My windowsill's a whole zoo now.",
        "I play the flute, badly, on the roof, when everyone's asleep. The cats are my audience.",
        "Cards. I'm unbeatable at cards. Sit down sometime and lose.",
        "I press flowers in books. Every book in my house is a little garden.",
        "I watch birds. I know every one in the valley by its song. There's one I've never seen. I'll find it.",
        "I write songs nobody's heard. Maybe someday. Probably not.",
        "I swim in the river at dawn, even in winter. It wakes me up better than anything.",
        "I knit. Scarves, mostly. Everyone in the village has one of mine whether they wanted it or not.",
        "I make kites. On windy days I fly three at once. People stop and watch.",
        "I paint, when I can get the colours. Mostly the sky. I can never get the sky right.",
        "I bake honey cakes and leave them on doorsteps. Nobody knows it's me. Well. Now you do.",
        "I read anything I can find. Even the CCG's leaflets. Especially those. They're funnier than they mean to be.",
        "I climb the old watchtower to watch the sunset. It's the only time the world stops.",
        "I tell the children ghost stories. Not the true ones. The made-up ones are scarier.",
        "I keep a garden of herbs nobody else can grow. They like me. I talk to them.",
        "I walk. Just walk. Around the wall, twice, every evening. It clears my head.",
        "I sew tiny clothes for the children's dolls. Every doll in the village is better dressed than I am.",
        "I play chess with the old man at the inn. Six years. He's never once let me win.",
        "I sing while I work. Loudly. People pretend to complain.",
        "I count the stars. Not all of them. I pick one patch and count it every night. Some nights there's one fewer.",
        "I keep a journal. Every day since I was nine. Nothing interesting ever happens in it.",
        "I fish for the fun of it and throw them all back. The fisher thinks I'm mad.",
        "I make wind chimes out of whatever I find. The whole street rings when it storms."};

    static final String[] FEAR = {
        "The dark. Not what's in it. Just the dark. Since I was little.",
        "Deep water. I can't swim, and I'm too proud to learn now.",
        "Being forgotten. Dying, and nobody saying my name after.",
        "The bell ringing twice at night. It means a ghoul. It's rung twice four times in my life.",
        "Fire. My family's barn burned when I was six. I still smell it sometimes, out of nowhere.",
        "That the CCG will come one day and say someone I love was one of them. A ghoul. That I never knew.",
        "Being alone when I'm old. Rocking in a chair with nobody to talk to.",
        "Spiders. Laugh. Go on. Everyone does.",
        "Hospitals. Doctors. Needles. The smell of them.",
        "My own temper. I've only lost it once. Once was enough.",
        "That I'll never leave this valley. That this is all there is.",
        "Masks. People in masks. The ghouls wear them, they say. I can't look at a festival mask without shaking.",
        "Thunder. I hide under the bed. I'm a grown adult. I hide under the bed.",
        "The woods past the north wall. Something lives there. Everyone knows. Nobody says.",
        "Losing my sight. My father did. I check every morning that I still can.",
        "That I'm a coward. That if a ghoul came for someone I love, I'd run.",
        "Wells. Something about looking down into the black. I don't go near the well after dark.",
        "Heights. I fell off the mill roof as a child. I still dream about the falling part.",
        "That someone I trust is lying to me about something big.",
        "Hunger. Real hunger. I was hungry once, for a whole winter. I keep three of everything now."};

    static final String[] DREAM = {
        "I want to see the sea. Just once. I've heard it's so big you can't see the end.",
        "I want my own shop. Just a little one. My name over the door.",
        "I want to fall in love the way they do in books. Stupidly. All at once.",
        "I want to go to the city and see the lights. Then come home and tell everyone.",
        "I want a house full of children and dogs and noise.",
        "I want to write a book that makes someone cry on a train.",
        "I want the walls to come down someday. Because we don't need them anymore.",
        "I want to learn to read properly. Don't laugh. I'm learning. Slowly.",
        "I want to see a ghoul up close. Not to fight. Just to understand.",
        "I want to grow old here, in this valley, with someone who still thinks I'm funny.",
        "I want to climb the highest mountain I can see from the tower and stand on top and shout.",
        "I want to be brave, just once, when it matters.",
        "I want to sail somewhere. Anywhere. Somewhere nobody knows my name.",
        "I want my father to be proud of me before he dies. He's not good at saying it.",
        "I want to sing in front of people and not have my voice crack.",
        "I want a garden so big I get lost in it.",
        "I want to have one quiet year. No deaths, no fear, no bells at night.",
        "I want to make something that lasts after I'm gone. A bridge. A song. A child. Something.",
        "I want to see my brother again. Just to know.",
        "I want to sleep a whole night without dreaming about the woods."};

    /** An odd habit: what it is caught doing, and what it says of it. */
    public record Quirk(String doing, String says) {
    }

    static final Quirk[] QUIRKS = {
        new Quirk("*is counting the cobblestones under their breath*", "I count things. Steps, stones, stars. If I lose count, something bad happens. It never has. Because I count."),
        new Quirk("*is humming the same four notes, over and over*", "It's a song my mother hummed. I only remember four notes. I keep hoping the rest will come back."),
        new Quirk("*is talking quietly to a cat on the wall*", "The cats know everything that happens in this village. You just have to ask them nicely."),
        new Quirk("*knocks on the nearest wooden post, twice*", "For luck. Everyone should. You'd be surprised how much bad luck is out there."),
        new Quirk("*is writing something on their palm with a finger*", "Shopping lists. I write them on my hand. Then I wash my hands. It's a flawed system."),
        new Quirk("*is wearing two different shoes, very confidently*", "It's a choice. A deliberate choice. Mostly. One of them's my brother's."),
        new Quirk("*has a flower tucked behind one ear*", "I wear a new one every day. If I ever don't, something's wrong. Ask me how I am that day."),
        new Quirk("*is eating an apple, core and all*", "Wasteful, leaving the core. My grandmother ate the seeds too. She lived to a hundred."),
        new Quirk("*keeps glancing at the sky, checking*", "I can tell the weather a day ahead. My knee tells me. My knee's never been wrong."),
        new Quirk("*is whistling at the birds, and they're answering*", "I learned all their calls when I was small. Now they think I'm one of them. Maybe I am."),
        new Quirk("*has ink on their nose and doesn't know it*", "Is there something on my face? ...Is there? Tell me. Don't laugh. You're laughing."),
        new Quirk("*is tying and untying a knot in a piece of string*", "It keeps my hands busy. If my hands are busy, my head's quiet."),
        new Quirk("*says good morning to the well*", "The well's older than the village. Someone has to be polite to it."),
        new Quirk("*walks around the puddles in a very particular pattern*", "Never step in your own reflection. Bad luck. Everyone knows that. Don't they?"),
        new Quirk("*is carrying a lantern, though it's broad daylight*", "You never know when it'll get dark. It got dark once in the middle of the day. Eclipse. Nobody was ready but me."),
        new Quirk("*keeps a pebble in their mouth, and takes it out to talk*", "Helps me think. And stops me saying things too quickly. You should try it."),
        new Quirk("*is reading while walking, and has not walked into anything yet*", "Twenty years of practice. I walked into the well once. Once."),
        new Quirk("*sniffs the air before answering*", "I can smell rain coming. And bread. And trouble. Trouble smells like iron."),
        new Quirk("*has a little bell sewn on their sleeve*", "So I always know where I am. ...That doesn't make sense. I know. I like the sound."),
        new Quirk("*taps their foot to a rhythm only they can hear*", "There's always a tune in my head. Today's is a fast one. It's a good day."),
    };

    static final String[] SECRET = {
        "I'm the one who leaves flowers on every doorstep. Every spring. Nobody knows. Don't you dare tell.",
        "I can't read. I pretend. I've pretended for twenty years. You're the first person I've told.",
        "I rang the bell at midnight once. As a joke. The whole village hid till morning. I've never told anyone.",
        "I took food to someone hiding in the woods, last winter. I don't know what they were. They never hurt me.",
        "I'm in love with someone who'll never know. They're married. It's fine. It's not fine.",
        "I wrote to the CCG to join, three times. They said no three times. I tell everyone I never applied.",
        "I keep a mask under my bed. A ghoul's. I found it in the woods. I can't throw it away.",
        "I'm saving every emerald I earn to leave this village. I've almost got enough. Almost.",
        "My father didn't die of a fever. Everyone knows that. Nobody says it. Now I've said it.",
        "I'm terrified all the time. Every day. I'm just good at smiling.",
        "I know who's been stealing eggs. It's the elder. I'll take it to my grave.",
        "I saw red eyes looking in my window one night. It just watched me sleep. Then left. I never told anyone. I wasn't scared. That's what scares me.",
        "I once kissed someone at the festival in a mask, and I still don't know who it was.",
        "I've never once been outside the walls. Not once. Everyone thinks I have.",
        "I can't swim. I'm the fisher's best friend and I can't swim.",
        "The elder asked me to keep a list of anyone who doesn't eat at the festival. I burned it.",
        "I write letters to my dead brother and put them in the river. Every week.",
        "I'm not from here. I came over the hills when I was twelve with nothing but a name I made up.",
        "I cheat at cards. Only with the old man. He cheats too. We both know.",
        "I sleepwalk. They found me at the north gate once, trying to open it. I don't know why."};

    /** The gift that wins it over: the item, what it's called, and what it says of it asked. */
    public record Favourite(String item, String name, String says) {
    }

    static final Favourite[] FAVOURITES = {
        new Favourite("minecraft:cake", "cake", "Cake. Any cake. I'd sell my soul for cake. It's not worth much, but it's yours for a slice."),
        new Favourite("minecraft:honey_bottle", "honey", "Honey. Straight from the bottle. Don't judge me."),
        new Favourite("minecraft:poppy", "poppies", "Poppies. Red ones. My mother grew them. Every time I see one, I see her."),
        new Favourite("minecraft:sweet_berries", "sweet berries", "Sweet berries. Pricking my fingers is half the fun."),
        new Favourite("minecraft:cookie", "cookies", "Cookies. Warm ones. Cold ones. Burnt ones. All of them."),
        new Favourite("minecraft:pumpkin_pie", "pumpkin pie", "Pumpkin pie. It tastes like the harvest festival when I was little."),
        new Favourite("minecraft:apple", "apples", "A good apple. Crisp. The kind that cracks when you bite it."),
        new Favourite("minecraft:sunflower", "sunflowers", "Sunflowers. They always face the light. I'm trying to learn that."),
        new Favourite("minecraft:book", "books", "Books. A new one is the best thing in the world. Second best is an old one."),
        new Favourite("minecraft:amethyst_shard", "amethyst", "Amethyst. Purple stones that sing if you hold them to your ear. I swear they sing."),
        new Favourite("minecraft:glow_berries", "glow berries", "Glow berries. They light up your hand. Like holding a tiny lantern you can eat."),
        new Favourite("minecraft:cornflower", "cornflowers", "Cornflowers. That blue. Nothing else is that blue."),
        new Favourite("minecraft:feather", "feathers", "Feathers. I keep every one I find. I'm going to make wings someday. A joke. Mostly."),
        new Favourite("minecraft:golden_carrot", "golden carrots", "Golden carrots. I had one once. I think about it every day."),
        new Favourite("minecraft:candle", "candles", "Candles. The little light in the window that means someone's home."),
        new Favourite("minecraft:lily_of_the_valley", "lilies of the valley", "Lilies of the valley. They're poison, you know. Beautiful and poison."),
        new Favourite("minecraft:music_disc_cat", "music", "Music. If you ever find a record, I'll love you forever. Not literally. Maybe literally."),
        new Favourite("minecraft:baked_potato", "baked potatoes", "A baked potato with nothing on it. Simple. Perfect. Like me. ...That was a joke."),
        new Favourite("minecraft:nautilus_shell", "shells", "Shells from the sea. I've never seen the sea. Holding one is the closest I've come."),
        new Favourite("minecraft:pink_tulip", "pink tulips", "Pink tulips. Don't ask why. There's a story. It's embarrassing."),
    };

    /** What it thinks of ghouls, as it says it; and how readily it takes a ghoul's confession for it. */
    public enum View {
        AFRAID(-0.05,
            s("Ghouls? Don't. I can't even say the word after dark. I lock my door three times.", "They look like us. That's the worst part. Anyone could be one.",
                "I pray the CCG gets every last one. Is that cruel? I don't care. I'm scared.")),
        HATES(-0.15,
            s("Monsters. Every one of them. They took people I loved. I hope they all burn.", "Don't talk to me about ghouls. Not today. Not ever.",
                "The CCG should burn the woods to the ground and everything in them.")),
        CURIOUS(0.08,
            s("I've always wondered what they're like. Do they dream? Do they fall in love? Nobody asks that.",
                "Everyone says they're monsters. I've never met one. How would I know?", "I'd like to talk to one, someday. From a safe distance. A very safe distance.")),
        SORRY(0.15,
            s("I think they're just hungry. Like anyone. It's a terrible thing to be born hungry for the wrong thing.",
                "Some of them must be kind. Some of us are cruel. Why would they all be the same?",
                "My grandmother said one saved her once, in the war. She never told anyone but me.")),
        TIRED(0.0,
            s("Ghouls, the CCG, the war... it's all the same thing over and over. I just want to grow carrots.",
                "I don't think about them. If I thought about them, I'd never sleep.", "They're out there. We're in here. That's all I need to know."));

        public final double trust;
        final String[] says;

        View(double trust, String[] says) {
            this.trust = trust;
            this.says = says;
        }

        public String say(int seed) {
            return pick(seed, this.says);
        }
    }

    // ---------------------------------------------------------------- one person's life

    /** One human's life, as its seed decides it (and its job, which its village may have given it). */
    public record Life(Job job, String family, String hobby, String fear, String dream, Quirk quirk, String secret, Favourite favourite, View view) {
        /** What it does all day, said by it. */
        public String quirkDoing() {
            return this.quirk.doing();
        }

        public String quirkSays() {
            return this.quirk.says();
        }

        /** The item that wins it over (an item id), and what it's called. */
        public String loves() {
            return this.favourite.item();
        }

        public String lovesName() {
            return this.favourite.name();
        }

        public String favouriteSays() {
            return this.favourite.says();
        }
    }

    /** {@code seed}'s life ({@code job}: given it by its village, or null: its seed's). */
    public static Life of(long seed, Job job) {
        Job[] jobs = Job.values();
        Job j = job != null ? job : jobs[index(seed, 0x10BL, jobs.length)];
        return new Life(j, FAMILY[index(seed, 0xFA31L, FAMILY.length)], HOBBY[index(seed, 0x40BBL, HOBBY.length)], FEAR[index(seed, 0xFEA2L, FEAR.length)],
            DREAM[index(seed, 0xD2EAL, DREAM.length)], QUIRKS[index(seed, 0x9E1BL, QUIRKS.length)], SECRET[index(seed, 0x5EC2L, SECRET.length)],
            FAVOURITES[index(seed, 0xFA70L, FAVOURITES.length)], view(seed, null));
    }

    /** What it thinks of ghouls: mostly afraid, a few hating them, some curious, some sorry for them, some too tired to care. */
    static View view(long seed, HumanPersona.Temper temper) {
        double r = HumanPersona.unit(seed, 0x6A0E1L);
        return r < 0.3 ? View.AFRAID : r < 0.42 ? View.HATES : r < 0.62 ? View.CURIOUS : r < 0.8 ? View.SORRY : View.TIRED;
    }

    static int index(long seed, long salt, int n) {
        return Math.min(n - 1, (int)(HumanPersona.unit(seed, salt) * n));
    }

    /** The job slot {@code slot} of a village at ({@code x}, {@code z}) is given: no two of a village's people share one, while there are enough. */
    public static Job villageJob(int x, int z, int slot) {
        Job[] jobs = Job.values();
        return jobs[com.hex.kagune.control.npc.HumanRules.villageName(x, z, slot, jobs.length)];
    }

    // ---------------------------------------------------------------- gifts

    /** How a gift lands: its favourite, something it likes (food, flowers, pretty things), nothing much, or something it dislikes. */
    public enum Gift {
        LOVED(8), LIKED(3), FINE(1), DISLIKED(-3);

        public final int regard;

        Gift(int regard) {
            this.regard = regard;
        }
    }

    /** Things anyone likes a little: food, flowers, pretty things. */
    static final Set<String> NICE = Set.of("minecraft:bread", "minecraft:cookie", "minecraft:cake", "minecraft:pumpkin_pie", "minecraft:apple",
        "minecraft:golden_apple", "minecraft:honey_bottle", "minecraft:sweet_berries", "minecraft:glow_berries", "minecraft:baked_potato",
        "minecraft:carrot", "minecraft:golden_carrot", "minecraft:melon_slice", "minecraft:mushroom_stew", "minecraft:beetroot_soup", "minecraft:poppy",
        "minecraft:dandelion", "minecraft:blue_orchid", "minecraft:allium", "minecraft:azure_bluet", "minecraft:red_tulip", "minecraft:orange_tulip",
        "minecraft:white_tulip", "minecraft:pink_tulip", "minecraft:oxeye_daisy", "minecraft:cornflower", "minecraft:lily_of_the_valley", "minecraft:sunflower",
        "minecraft:lilac", "minecraft:rose_bush", "minecraft:peony", "minecraft:emerald", "minecraft:diamond", "minecraft:amethyst_shard", "minecraft:book",
        "minecraft:candle", "minecraft:feather", "minecraft:nautilus_shell", "minecraft:music_disc_cat", "minecraft:gold_ingot");
    /** Things nobody wants handed to them. */
    static final Set<String> NASTY = Set.of("minecraft:rotten_flesh", "minecraft:spider_eye", "minecraft:poisonous_potato", "minecraft:bone",
        "minecraft:dirt", "minecraft:gravel", "minecraft:wither_rose", "minecraft:pufferfish", "minecraft:fermented_spider_eye", "hexkagune:ghoul_meat");

    /** How {@code item} lands with {@code life}. */
    public static Gift gift(Life life, String item) {
        if (item == null || item.isEmpty()) {
            return Gift.FINE;
        }
        if (item.equals(life.loves())) {
            return Gift.LOVED;
        }
        if (NASTY.contains(item) || item.contains("flesh") || item.startsWith("hexkagune:ghoul")) {
            return Gift.DISLIKED;
        }
        return NICE.contains(item) ? Gift.LIKED : Gift.FINE;
    }

    // ---------------------------------------------------------------- helpers

    static String[] s(String... lines) {
        return lines;
    }

    static VillageShop.Offer w(String item, String name, int count, int price) {
        return new VillageShop.Offer(item, name, count, price);
    }

    static String pick(int seed, String[] lines) {
        return lines[Math.floorMod(seed, lines.length)];
    }
}
