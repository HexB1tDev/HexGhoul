package com.hex.kagune.control.npc;

import java.util.List;

/**
 * 1.3.14: who a one-of-a-kind companion is (pure, tested): Eto, Touka, Ken. One mind ({@code EtoBrain}), one talk server
 * ({@code EtoServer}) and one renderer serve them all; what differs is here: their key (their bond, their memory, their place in the
 * world), their name and colour, their body (size, a woman's figure or a man's, slim arms or wide), how forward they are (asking for a
 * kiss, starting things themselves), what their hands do when they are idle in each mood, what they fight with (only Eto has the Owl
 * DNA kit; the others fight with the eye-mode kit every ghoul has), what they teach ({@link Teaching}) and every word they say
 * ({@link Script}). A name tag on one changes what it is called everywhere (the name filter keeps some names out of the mod itself).
 */
public abstract class Persona {
    public static final Persona ETO = new Eto();
    public static final Persona TOUKA = new Touka();
    public static final Persona KEN = new Ken();
    /** 4.0.0 (The Great Exploration): Ayato Kirishima and Noro, Aogiri Tree's, after the three (the talk screen's faces go by this order). */
    public static final Persona AYATO = new Ayato();
    public static final Persona NORO = new Noro();
    /**
     * 4.1.5 (the owner: "a new \"general\" there talks to you and can be romanced ... Like ayato, ken, touka, eto and noro, he is an official
     * character, means he will respawn"): the CCG's General, in its building. The only human of them. After the others (the talk screen's
     * faces go by this order).
     */
    public static final Persona GENERAL = new General();
    /**
     * 4.1.49 (the owner: "New Official NPC named \"Roma\", she is the leader of the clowns, can be romanced, has a messed up sense of humor
     * but can be romatic when comes to it. She uses Kick master combat, has a Rinkaku with 4 purple tendrils ... She spawns in this circus
     * tent, is the size of Touka, is female, can be romanced by both Females and Males. (Alex Model, skinny)"): Roma, the Clowns' leader,
     * in the middle of their circus tent. After the others (the talk screen's faces go by this order).
     */
    public static final Persona ROMA = new Roma();
    public static final List<Persona> ALL = List.of(ETO, TOUKA, KEN, AYATO, NORO, GENERAL, ROMA);

    /** Its key: its bond in each player's quest log, its memory, its one place in the world ({@code UniqueNpcs}), its textures. */
    public abstract String key();

    /** What it is called (unless a name tag says otherwise). */
    public abstract String name();

    /** The colour of its name and words in chat (a section-sign code). */
    public abstract String colour();

    /** Its size against a player's. */
    public abstract float size();

    /** True for a woman (the one female body: hips, bust; she can carry a child). */
    public abstract boolean female();

    /**
     * 1.3.14: how much of the female figure's curves it has (1: Eto's, every woman's; Touka a little skinnier), and how wide its whole
     * body is drawn against a player's (Touka a touch narrower).
     */
    public float curves() {
        return 1.0F;
    }

    public float build() {
        return 1.0F;
    }

    /** 1.3.21: how strong it is as a parent (0..1): its children are born the stronger for it (ChildRules.might). */
    public float might() {
        return 0.5F;
    }

    /** 1.3.19: whether it fights with Ukaku wings of its own (Touka: a lesser pair, crystal spikes), drawn on its back in a fight. */
    public boolean wings() {
        return false;
    }

    /** 1.3.19: the final kakuja it fights in, worn the moment a fight finds it (Ken: Peace), or NONE. */
    public com.hex.kagune.control.king.KingForm finalKakuja() {
        return com.hex.kagune.control.king.KingForm.NONE;
    }

    /** 1.3.18: how much of the figure's extra width and depth its legs have (its curves unless said otherwise: Touka's a touch less). */
    public float legs() {
        return this.curves();
    }

    /** True if drawn on the slim arms. */
    public boolean slim() {
        return this.female();
    }

    /** 4.0.9: whether, with nothing to do at home (his own place, not one shared with a spouse), he sits there (Ken, on the top of his tower). */
    public boolean sitsAtHome() {
        return false;
    }

    /** 4.0.8: a man's build: how far his chest stands out past the torso, as a share of the White Reaper's ({@link ReaperBuild}); 0: none. */
    public double muscle() {
        return 0.0;
    }

    /**
     * 4.1.5: true for a human (the General): no kakugan, no kagune to pass on, nothing of a ghoul's table; the CCG's investigators are
     * his own (he never goes for one).
     */
    public boolean human() {
        return false;
    }

    /**
     * 4.1.5 (the owner: "give him all the formal outfit (but the hat)"): the outfit pieces it wears always (item ids,
     * {@code outfit.Outfits}); none for most.
     */
    public List<String> outfit() {
        return List.of();
    }

    /** She, or he. */
    public String she() {
        return this.female() ? "she" : "he";
    }

    /** Her, or him. */
    public String her() {
        return this.female() ? "her" : "him";
    }

    /** Health and armour. */
    public abstract float health();

    public abstract double armor();

    /** Hair, eyes and skin colours (a child of theirs inherits them). */
    public abstract int hair();

    public abstract int eyes();

    public abstract int skin();

    /** True if one eye only turns (Eto's, Ken's); a child of theirs may be one-eyed. */
    public abstract boolean oneEyed();

    /**
     * 1.3.43: the kagune it passes on to a child ({@code KaguneGenes} bits): Ken's Rinkaku tails, Touka's Ukaku wings, Eto's Owl kagune.
     */
    public int kagune() {
        if (this.human()) {
            return com.hex.kagune.control.family.KaguneGenes.NONE; // 4.1.5: a human has none to pass on
        }
        if (this.owlKit()) {
            return com.hex.kagune.control.family.KaguneGenes.OWL;
        }
        if (this.worms()) {
            return com.hex.kagune.control.family.KaguneGenes.WORM; // 4.0.0: a Bikaku one's worm
        }
        return this.wings() ? com.hex.kagune.control.family.KaguneGenes.WINGS : com.hex.kagune.control.family.KaguneGenes.TAILS;
    }

    /** 1.3.43: its kagune's colour, as a child may take it: the original's crimson (Touka's wings, Ken's tails); Eto's pale flesh. */
    public int kaguneColour() {
        return this.owlKit() ? 0xD8CCBC : com.hex.kagune.control.KaguneColours.DEFAULT;
    }

    /**
     * 4.0.0: whether it can be dated at all (the owner: Noro "will not have romance options. His interactions will focus entirely on
     * dialogue, combat, training, and progression"): a friend and a teacher, never more.
     */
    public boolean romanceable() {
        return true;
    }

    /** 4.0.0: its pace (the mob's movement attribute): a player's for most; Ayato quicker; Noro slow and deliberate. */
    public double speed() {
        return 0.32;
    }

    /** 4.0.0: how hard its wings hit and how fast they throw, against Touka's lesser pair (Ayato's are a full Ukaku's). */
    public double wingPower() {
        return 1.0;
    }

    /** 4.0.0: whether, badly hurt in a fight, it bursts into its own Ukaku kakuja (Ayato). */
    public boolean kakuja() {
        return false;
    }

    /** 4.0.0: whether it fights with the Bikaku's worms (Noro: three, his whole kagune). */
    public boolean worms() {
        return false;
    }

    /** 4.0.0: the mask it wears (an item id): always, or in a fight only ({@link #maskInFight}); null for none. */
    public String mask() {
        return null;
    }

    public boolean maskInFight() {
        return false;
    }

    /**
     * 4.1.49: whether its body fights Kick Master's way (its string the kicks, its kick the twirling speed kick): the General's (a human's
     * only style), and Roma's (the owner: "She uses Kick master combat").
     */
    public boolean kickMaster() {
        return this.human();
    }

    /** 4.1.49: how many Rinkaku tendrils it fights with (Roma: four), drawn out of its back in a fight; 0 for none. */
    public int tendrils() {
        return 0;
    }

    /** True for Eto alone: the Owl DNA kit in a fight (the flesh, Watch This, the Mutated Arm, the Owl). */
    public boolean owlKit() {
        return false;
    }

    /** How readily it asks its partner for a kiss itself, against Eto's. */
    public double kissing() {
        return 1.0;
    }

    /** How readily it starts things with its partner itself (a hand, a hug...), against Eto's. */
    public double forward() {
        return 1.0;
    }

    /** 1.3.14: true if it would date someone who is a woman ({@code woman}) or not: Ken only dates women; the others anyone. */
    public boolean likes(boolean woman) {
        return true;
    }

    /**
     * 1.3.14: a man's way of holding someone (Ken): he leans down to kiss, a hand to their cheek and one at their waist, kisses their
     * forehead, wraps them up in a hug; a woman's (Eto, Touka) leans up into it, hands behind her back.
     */
    public boolean leads() {
        return !this.female();
    }

    /** 1.3.14: whether {@code gesture} brings the colour to its cheeks (Touka loses her composure at almost anything sweet). */
    public boolean blushes(EtoGesture gesture) {
        return gesture.blush;
    }

    /** 1.3.14: true if it sometimes just walks up and gives its partner a quick kiss itself (Touka, Ken) instead of waiting for one. */
    public boolean pecks() {
        return !this.owlKit();
    }

    /** 1.3.29: how its body holds someone (EtoPose.Style): Eto's own, Ken's (he leads), Touka's (one hand, serious, flustered after). */
    public int style() {
        return this.leads() ? 1 : 0;
    }

    /** 1.3.29: true if it winds round its partner's arm (Eto); false, it never would (Touka: a hug if asked to hold them). */
    public boolean clings() {
        return true;
    }

    /**
     * 1.3.29: what it does unasked instead of {@code gesture}, its own way (Touka: never an arm, a quick kiss on the cheek; never a dance
     * or a song, a kiss stolen when they least expect it). {@code gesture} itself if it has no other way.
     */
    public EtoGesture unasked(EtoGesture gesture) {
        return gesture;
    }

    /** Where it goes home to, as a line says it. */
    public abstract String home();

    /** What its hands do now and then, in {@code mood}. */
    public abstract EtoGesture[] idles(EtoMood mood);

    /** Anything at all it might do idle (a fifth of the time it picks from these instead). */
    public abstract EtoGesture[] anyIdle();

    /** Every word it says. */
    public abstract Script script();

    /** What it teaches, in order (empty: Eto's lessons are her own, {@link EtoRules.Lesson}). */
    public List<Teaching.Lesson> lessons() {
        return Teaching.of(this.key());
    }

    /** The item that counts for the first of its errands (Eto: a book), by item id. */
    public abstract boolean errandItem(String id);

    /** The item that counts as its flower, its last errand's gift, by item id. */
    public boolean flower(String id) {
        return EtoRules.flower(id);
    }

    /** The persona with {@code key} (Eto for anything unknown). */
    public static Persona of(String key) {
        for (Persona persona : ALL) {
            if (persona.key().equals(key)) {
                return persona;
            }
        }
        return ETO;
    }

    /** True if {@code key} is one of the companions'. */
    public static boolean known(String key) {
        for (Persona persona : ALL) {
            if (persona.key().equals(key)) {
                return true;
            }
        }
        return false;
    }

    // ---------------------------------------------------------------- Eto, as she always was

    private static final class Eto extends Persona {
        private final Script script = new EtoScript();

        @Override
        public String key() {
            return EtoRules.KEY;
        }

        @Override
        public String name() {
            return EtoRules.NAME;
        }

        @Override
        public String colour() {
            return "§d";
        }

        @Override
        public float size() {
            return EtoRules.SIZE;
        }

        @Override
        public boolean female() {
            return true;
        }

        @Override
        public float health() {
            return EtoRules.HEALTH;
        }

        /** 1.3.21: the Owl: second only to Ken. */
        @Override
        public float might() {
            return 0.95F;
        }

        @Override
        public double armor() {
            return EtoRules.ARMOR;
        }

        @Override
        public int hair() {
            return EtoRules.HAIR;
        }

        @Override
        public int eyes() {
            return EtoRules.EYES;
        }

        @Override
        public int skin() {
            return EtoRules.SKIN;
        }

        @Override
        public boolean oneEyed() {
            return true;
        }

        @Override
        public boolean owlKit() {
            return true;
        }

        @Override
        public String home() {
            return "the Library";
        }

        @Override
        public EtoGesture[] idles(EtoMood mood) {
            return mood.idles();
        }

        @Override
        public EtoGesture[] anyIdle() {
            return EtoGesture.IDLE;
        }

        @Override
        public Script script() {
            return this.script;
        }

        @Override
        public List<Teaching.Lesson> lessons() {
            return List.of();
        }

        @Override
        public boolean errandItem(String id) {
            return EtoRules.book(id);
        }
    }

    // ---------------------------------------------------------------- Touka: the rabbit

    private static final class Touka extends Persona {
        private Script script;

        /** 1.3.17: the owner: her legs and chest "bigger by quite a bit": more hips, thighs and bust than Eto's, at a woman's full width. */
        @Override
        public float curves() {
            return 1.4F;
        }

        @Override
        public float build() {
            return 1.0F;
        }

        /** 1.3.19: the owner: "a lesser, halfway" kagune of her own, crystal spikes, the barrage; simple, nowhere near Ken or Eto. */
        @Override
        public boolean wings() {
            return true;
        }

        /** 1.3.18: the owner: her legs "a bit too thick", "just a little bit" thinner: a touch less of the figure's leg width than her curves. */
        @Override
        public float legs() {
            return 1.2F;
        }

        /** She goes pink at anything sweet: a kiss she's waiting on, a hand, a hug, being leaned on, a dance. Composure: gone. */
        @Override
        public boolean blushes(EtoGesture gesture) {
            return gesture.blush || switch (gesture) {
                case KISS, HAND, CLING, LEAN, HAIR, DANCE, HUM, PET, RING -> true;
                default -> false;
            };
        }

        /** 1.3.29: the owner: her own way, not Eto's: one hand to their head for a kiss, serious, flustered after. */
        @Override
        public int style() {
            return 2;
        }

        /** 1.3.29: the owner: "she would definitely never grab your arm like Eto does". */
        @Override
        public boolean clings() {
            return false;
        }

        /** 1.3.29: a kiss on the cheek out of nowhere, or their face turned to hers and a kiss on the lips, and then a complaint. */
        @Override
        public EtoGesture unasked(EtoGesture gesture) {
            return switch (gesture) {
                case CLING, HUM -> EtoGesture.PECK;
                case DANCE, READ -> EtoGesture.STEAL;
                default -> gesture;
            };
        }

        @Override
        public String key() {
            return Teaching.TOUKA;
        }

        @Override
        public String name() {
            return "Touka";
        }

        @Override
        public String colour() {
            return "§5";
        }

        @Override
        public float size() {
            return com.hex.kagune.control.FemaleBody.SIZE;
        }

        @Override
        public boolean female() {
            return true;
        }

        @Override
        public float health() {
            return 260.0F;
        }

        /** 1.3.21: strong, nowhere near Eto or Ken. */
        @Override
        public float might() {
            return 0.6F;
        }

        @Override
        public double armor() {
            return 12.0;
        }

        @Override
        public int hair() {
            return 0x2C2340;
        }

        @Override
        public int eyes() {
            return 0x3E5A8E;
        }

        @Override
        public int skin() {
            return 0xF1DCCF;
        }

        @Override
        public boolean oneEyed() {
            return false;
        }

        /** She would rather die than ask first. Now and then she does anyway. */
        @Override
        public double kissing() {
            return 0.35;
        }

        @Override
        public double forward() {
            return 0.5;
        }

        @Override
        public String home() {
            return "Anteiku"; // 1.3.24: the café she works at (her structure)
        }

        @Override
        public EtoGesture[] idles(EtoMood mood) {
            return switch (mood) {
                case GIDDY -> new EtoGesture[] {EtoGesture.SHY, EtoGesture.HAIR, EtoGesture.SIP, EtoGesture.HUM};
                case SULKY, JEALOUS -> new EtoGesture[] {EtoGesture.CROSS, EtoGesture.HUFF, EtoGesture.POUT, EtoGesture.TAP};
                case LONELY, BORED -> new EtoGesture[] {EtoGesture.WIPE, EtoGesture.SIP, EtoGesture.READ, EtoGesture.LOOK, EtoGesture.KICKS};
                case CLINGY, AFFECTIONATE -> new EtoGesture[] {EtoGesture.SHY, EtoGesture.HAIR, EtoGesture.SIP, EtoGesture.CROSS};
                case SLEEPY -> new EtoGesture[] {EtoGesture.YAWN, EtoGesture.STRETCH, EtoGesture.SIP};
                case BLOODTHIRSTY -> new EtoGesture[] {EtoGesture.KICKS, EtoGesture.LOOK, EtoGesture.TAP};
                case CLAIMED, WARY -> new EtoGesture[] {EtoGesture.CROSS, EtoGesture.WIPE, EtoGesture.TAP, EtoGesture.SCOLD, EtoGesture.HUFF};
                default -> new EtoGesture[] {EtoGesture.WIPE, EtoGesture.SIP, EtoGesture.CROSS, EtoGesture.READ, EtoGesture.KICKS};
            };
        }

        @Override
        public EtoGesture[] anyIdle() {
            return new EtoGesture[] {EtoGesture.CROSS, EtoGesture.TAP, EtoGesture.HUFF, EtoGesture.WIPE, EtoGesture.SIP, EtoGesture.KICKS, EtoGesture.SCOLD,
                EtoGesture.READ, EtoGesture.STRETCH, EtoGesture.LOOK};
        }

        @Override
        public Script script() {
            if (this.script == null) {
                this.script = new ToukaScript();
            }
            return this.script;
        }

        /** Coffee beans: the cafe always runs low. */
        @Override
        public boolean errandItem(String id) {
            return "minecraft:cocoa_beans".equals(id);
        }

        /** A rabbit's foot, of all things. */
        @Override
        public boolean flower(String id) {
            return "minecraft:rabbit_foot".equals(id);
        }
    }

    // ---------------------------------------------------------------- Ken: the one-eyed king

    private static final class Ken extends Persona {
        private Script script;

        @Override
        public String key() {
            return Teaching.KEN;
        }

        @Override
        public String name() {
            return "Ken";
        }

        @Override
        public String colour() {
            return "§f";
        }

        @Override
        public float size() {
            return 0.94F;
        }

        @Override
        public boolean female() {
            return false;
        }

        @Override
        public float health() {
            return 340.0F;
        }

        /** 1.3.21: the strongest of them all: his children are born the strongest. */
        @Override
        public float might() {
            return 1.0F;
        }

        /** 1.3.19: the owner: the strongest of them all; in a fight, Peace (the final kakuja) at once, every one of its moves his. */
        @Override
        public com.hex.kagune.control.king.KingForm finalKakuja() {
            return com.hex.kagune.control.king.KingForm.PEACE;
        }

        @Override
        public double armor() {
            return 15.0;
        }

        @Override
        public int hair() {
            return 0xE6E6EA;
        }

        @Override
        public int eyes() {
            return 0x4B4A52;
        }

        @Override
        public int skin() {
            return 0xEFDACB;
        }

        @Override
        public boolean oneEyed() {
            return true;
        }

        /** A devoted lover: he reaches for her as often as Eto reaches for hers, and a little more. */
        @Override
        public double kissing() {
            return 1.0;
        }

        @Override
        public double forward() {
            return 1.25;
        }

        @Override
        public String home() {
            return "his tower"; // 1.3.24: the top of the tower (his structure)
        }

        @Override
        public EtoGesture[] idles(EtoMood mood) {
            return switch (mood) {
                case GIDDY -> new EtoGesture[] {EtoGesture.HUM, EtoGesture.HAIR, EtoGesture.SIP, EtoGesture.BOW};
                case SULKY, JEALOUS -> new EtoGesture[] {EtoGesture.CHIN, EtoGesture.CRACK, EtoGesture.READ, EtoGesture.LOOK};
                case LONELY, BORED -> new EtoGesture[] {EtoGesture.READ, EtoGesture.READ, EtoGesture.WRITE, EtoGesture.SIP, EtoGesture.LOOK};
                case CLINGY, AFFECTIONATE -> new EtoGesture[] {EtoGesture.HAIR, EtoGesture.SIP, EtoGesture.READ, EtoGesture.HUM};
                case SLEEPY -> new EtoGesture[] {EtoGesture.YAWN, EtoGesture.STRETCH, EtoGesture.READ};
                case BLOODTHIRSTY -> new EtoGesture[] {EtoGesture.CRACK, EtoGesture.CRACK, EtoGesture.LOOK, EtoGesture.CHIN};
                case CLAIMED, WARY -> new EtoGesture[] {EtoGesture.READ, EtoGesture.SWEEP, EtoGesture.CHIN, EtoGesture.BOW, EtoGesture.SIP};
                default -> new EtoGesture[] {EtoGesture.READ, EtoGesture.SIP, EtoGesture.CHIN, EtoGesture.SWEEP, EtoGesture.CRACK};
            };
        }

        @Override
        public EtoGesture[] anyIdle() {
            return new EtoGesture[] {EtoGesture.READ, EtoGesture.SIP, EtoGesture.CHIN, EtoGesture.CRACK, EtoGesture.BOW, EtoGesture.SWEEP, EtoGesture.WRITE,
                EtoGesture.STRETCH, EtoGesture.LOOK};
        }

        @Override
        public Script script() {
            if (this.script == null) {
                this.script = new KenScript();
            }
            return this.script;
        }

        /** The owner's skin for him is made for the slim arms (a man's body otherwise: no hips, no bust). */
        @Override
        public boolean slim() {
            return true;
        }

        /** 4.0.9 (the owner: "make sure he sits on the top"): on the top of his tower, he sits, looking out. */
        @Override
        public boolean sitsAtHome() {
            return true;
        }

        /** He is only interested in women; with anyone else he is a friend and a teacher. */
        @Override
        public boolean likes(boolean woman) {
            return woman;
        }

        /** Books: he has read everything, and still wants more. */
        @Override
        public boolean errandItem(String id) {
            return EtoRules.book(id);
        }

        /** A white flower. */
        @Override
        public boolean flower(String id) {
            return "minecraft:white_tulip".equals(id) || "minecraft:lily_of_the_valley".equals(id) || "minecraft:oxeye_daisy".equals(id)
                || "minecraft:azure_bluet".equals(id);
        }
    }

    // ---------------------------------------------------------------- 4.0.0: Ayato: the black rabbit

    /**
     * 4.0.0 (the owner: "edgy, arrogant, confrontational, confident, sarcastic, and occasionally aggressive"; on his romance: "quite similar
     * to kaneki, animations can be the same"): Touka's little brother, Aogiri Tree's. Ukaku: his wings a full pair, faster and harder than
     * hers; his kakuja when he is losing. He leads as Ken does (Ken's way of holding someone, his animations), but slowly: he asks for less,
     * starts less, and means more by it.
     */
    private static final class Ayato extends Persona {
        /**
         * 4.1.48 (the owner: "Ayato and Noro should both be Alex models"): on the slim arms (Ayato's skin is painted for them; Noro's game
         * copy has its arms narrowed: tools/companion_textures.py, narrow_arms). A man's body otherwise (no hips, no bust).
         */
        @Override
        public boolean slim() {
            return true;
        }

        private Script script;

        /**
         * 4.0.8 (the owner: "I sent you the new ayato skin, make him a little muscular but way less than the male player."): his chest a
         * little broader ({@link ReaperBuild#AYATO_SHARE} of the White Reaper's; a male player's is 0.6); his new skin is made for the wide
         * arms, as he is drawn.
         */
        @Override
        public double muscle() {
            return ReaperBuild.AYATO_SHARE;
        }

        @Override
        public String key() {
            return Teaching.AYATO;
        }

        @Override
        public String name() {
            return "Ayato";
        }

        @Override
        public String colour() {
            return "\u00a79";
        }

        /** Shorter than most, and touchy about it. */
        @Override
        public float size() {
            return 0.92F;
        }

        @Override
        public boolean female() {
            return false;
        }

        @Override
        public float health() {
            return 300.0F;
        }

        @Override
        public double armor() {
            return 13.0;
        }

        @Override
        public float might() {
            return 0.8F;
        }

        @Override
        public boolean wings() {
            return true;
        }

        @Override
        public double wingPower() {
            return 1.7;
        }

        @Override
        public boolean kakuja() {
            return true;
        }

        /** The owner: "noticeably faster and more dangerous than that of ordinary wild ghouls". */
        @Override
        public double speed() {
            return 0.38;
        }

        @Override
        public String mask() {
            return "mask_ayato";
        }

        /** His respirator goes on when he fights; off it, his face (and the scowl) for everyone to see. */
        @Override
        public boolean maskInFight() {
            return true;
        }

        @Override
        public int hair() {
            return 0x3A1C66;
        }

        @Override
        public int eyes() {
            return 0x2B2F5C;
        }

        @Override
        public int skin() {
            return 0xF9E8DC;
        }

        @Override
        public boolean oneEyed() {
            return false;
        }

        /** He'd rather die than ask first. Later, he asks anyway, scowling. */
        @Override
        public double kissing() {
            return 0.55;
        }

        @Override
        public double forward() {
            return 0.65;
        }

        /** Ken's way with someone he holds (the owner: "animations can be the same"). */
        @Override
        public int style() {
            return 1;
        }

        @Override
        public boolean clings() {
            return false;
        }

        @Override
        public String home() {
            return "the hideout";
        }

        @Override
        public EtoGesture[] idles(EtoMood mood) {
            return switch (mood) {
                case GIDDY -> new EtoGesture[] {EtoGesture.HUFF, EtoGesture.HAIR, EtoGesture.CROSS, EtoGesture.HUM};
                case SULKY, JEALOUS -> new EtoGesture[] {EtoGesture.CROSS, EtoGesture.HUFF, EtoGesture.TAP, EtoGesture.KICKS};
                case LONELY, BORED -> new EtoGesture[] {EtoGesture.LOOK, EtoGesture.KICKS, EtoGesture.STRETCH, EtoGesture.CROSS};
                case CLINGY, AFFECTIONATE -> new EtoGesture[] {EtoGesture.HAIR, EtoGesture.CROSS, EtoGesture.LOOK, EtoGesture.SHY};
                case SLEEPY -> new EtoGesture[] {EtoGesture.YAWN, EtoGesture.STRETCH, EtoGesture.CROSS};
                // 4.0.2 (the owner: "he just looks edgy. He likes to pose. Just a massive poser"; the finger crack is Ken's alone).
                case BLOODTHIRSTY -> new EtoGesture[] {EtoGesture.POSE, EtoGesture.KICKS, EtoGesture.SLICK};
                case CLAIMED, WARY -> new EtoGesture[] {EtoGesture.CROSS, EtoGesture.POSE, EtoGesture.SHRUG, EtoGesture.SCOLD};
                default -> new EtoGesture[] {EtoGesture.POSE, EtoGesture.SLICK, EtoGesture.CROSS, EtoGesture.SHRUG, EtoGesture.POSE, EtoGesture.KICKS};
            };
        }

        @Override
        public EtoGesture[] anyIdle() {
            return new EtoGesture[] {EtoGesture.POSE, EtoGesture.SLICK, EtoGesture.SHRUG, EtoGesture.CROSS, EtoGesture.POSE, EtoGesture.HUFF,
                EtoGesture.KICKS, EtoGesture.SLICK, EtoGesture.STRETCH};
        }

        @Override
        public Script script() {
            if (this.script == null) {
                this.script = new AyatoScript();
            }
            return this.script;
        }

        /** Feathers: his wings shed them; he collects the black ones and would kill anyone who said so. */
        @Override
        public boolean errandItem(String id) {
            return "minecraft:feather".equals(id);
        }

        /** A wither rose: nothing softer would do. */
        @Override
        public boolean flower(String id) {
            return "minecraft:wither_rose".equals(id);
        }
    }

    // ---------------------------------------------------------------- 4.0.0: Noro

    /**
     * 4.0.0 (the owner: "extremely quiet, unsettling, and difficult to read ... limited speech, deliberate movements, and an intimidating
     * presence"; "Noro is taller than the average player and is very brief, barely talks"): Aogiri Tree's, masked always, a head taller than
     * anyone. Bikaku: three worms, the whole of it, and a body that will not stay dead. A teacher, never a lover.
     */
    private static final class Noro extends Persona {
        /** 4.1.48 (the owner: "give some to Noro, both less than the player, noro more than ayato"): {@link ReaperBuild#NORO_SHARE}. */
        @Override
        public double muscle() {
            return ReaperBuild.NORO_SHARE;
        }

        /**
         * 4.1.48 (the owner: "Ayato and Noro should both be Alex models"): on the slim arms (Ayato's skin is painted for them; Noro's game
         * copy has its arms narrowed: tools/companion_textures.py, narrow_arms). A man's body otherwise (no hips, no bust).
         */
        @Override
        public boolean slim() {
            return true;
        }

        private Script script;

        @Override
        public String key() {
            return Teaching.NORO;
        }

        @Override
        public String name() {
            return "Noro";
        }

        @Override
        public String colour() {
            return "\u00a78";
        }

        @Override
        public float size() {
            return 1.15F;
        }

        @Override
        public boolean female() {
            return false;
        }

        @Override
        public float health() {
            return 520.0F;
        }

        @Override
        public double armor() {
            return 16.0;
        }

        @Override
        public float might() {
            return 0.9F;
        }

        @Override
        public boolean romanceable() {
            return false;
        }

        @Override
        public boolean worms() {
            return true;
        }

        /** "deliberate movements": he walks; his worms do the running. */
        @Override
        public double speed() {
            return 0.27;
        }

        @Override
        public String mask() {
            return "mask_noro";
        }

        @Override
        public int hair() {
            return 0x1C1A24;
        }

        @Override
        public int eyes() {
            return 0x8A1A1A;
        }

        @Override
        public int skin() {
            return 0xF5F2F5;
        }

        @Override
        public boolean oneEyed() {
            return false;
        }

        @Override
        public int kagune() {
            return com.hex.kagune.control.family.KaguneGenes.WORM; // 4.0.0: his worm (were he ever a parent)
        }

        @Override
        public double kissing() {
            return 0.0;
        }

        @Override
        public double forward() {
            return 0.0;
        }

        @Override
        public boolean likes(boolean woman) {
            return false;
        }

        @Override
        public boolean clings() {
            return false;
        }

        @Override
        public boolean pecks() {
            return false;
        }

        @Override
        public String home() {
            return "the hideout";
        }

        /** Still. Now and then his head tilts, slowly, far over, and he stares (4.0.2, the owner: "he's just a weird guy"). */
        @Override
        public EtoGesture[] idles(EtoMood mood) {
            return new EtoGesture[] {EtoGesture.TILT};
        }

        @Override
        public EtoGesture[] anyIdle() {
            return new EtoGesture[] {EtoGesture.TILT, EtoGesture.TILT, EtoGesture.TILT, EtoGesture.TILT, EtoGesture.TILT};
        }

        @Override
        public Script script() {
            if (this.script == null) {
                this.script = new NoroScript();
            }
            return this.script;
        }

        /** Meat. Ghoul meat. */
        @Override
        public boolean errandItem(String id) {
            return "hexkagune:ghoul_meat".equals(id);
        }

        @Override
        public boolean flower(String id) {
            return "minecraft:bone".equals(id);
        }
    }

    // ---------------------------------------------------------------- 4.1.5: the General

    /**
     * 4.1.5 (the owner: "a new \"general\" there talks to you and can be romanced, use one of the male skins currently available (NOT
     * CHILDREN skins) and change it a bit to make it original, give him some muscles also less than the player ... Like ayato, ken, touka, eto
     * and noro, he is an official character, means he will respawn ... Attitude: Tough, softens up when romanced, embarrased easily, proud
     * of the CCG, has to be convinced to love a ghoul, Only interested in woman players"; then, with a skin of their own: "use this skin
     * for the general and give him all the formal outfit (but the hat)"): the CCG's General, a human, at home on the top floor of its
     * building. His skin is the owner's (art/exploration/General_skin.png, slim arms); he wears the Formal Outfit's coat, trousers and
     * shoes, never its cap.
     */
    private static final class General extends Persona {
        private Script script;

        @Override
        public String key() {
            return "general";
        }

        @Override
        public String name() {
            return "General Kuroda";
        }

        @Override
        public String colour() {
            return "\u00a76";
        }

        /** A player's height: it is his chest and his coat that make him big. */
        @Override
        public float size() {
            return 1.0F;
        }

        @Override
        public boolean female() {
            return false;
        }

        @Override
        public boolean human() {
            return true;
        }

        /** The owner's skin for him is made for the slim arms (a man's body otherwise). */
        @Override
        public boolean slim() {
            return true;
        }

        /** The owner: "give him some muscles also less than the player": between Ayato's (0.3) and a male player's (0.6). */
        @Override
        public double muscle() {
            return ReaperBuild.GENERAL_SHARE;
        }

        /** The owner: "give him all the formal outfit (but the hat)". */
        @Override
        public List<String> outfit() {
            return List.of("formal_chestplate", "formal_leggings", "formal_boots");
        }

        @Override
        public float health() {
            return 320.0F;
        }

        @Override
        public double armor() {
            return 14.0;
        }

        @Override
        public float might() {
            return 0.75F;
        }

        @Override
        public int hair() {
            return 0x44352C;
        }

        @Override
        public int eyes() {
            return 0x6A8099;
        }

        @Override
        public int skin() {
            return 0xCEA386;
        }

        @Override
        public boolean oneEyed() {
            return false;
        }

        /** The owner: "Only interested in woman players". */
        @Override
        public boolean likes(boolean woman) {
            return woman;
        }

        /** The owner: "embarrased easily": nearly anything sweet brings the colour up over his beard. */
        @Override
        public boolean blushes(EtoGesture gesture) {
            return gesture.blush || switch (gesture) {
                case KISS, HAND, CLING, LEAN, HAIR, DANCE, HUM, PET, RING, HUG, PECK -> true;
                default -> false;
            };
        }

        /** He would never ask first, and when he does it comes out like an order. */
        @Override
        public double kissing() {
            return 0.4;
        }

        @Override
        public double forward() {
            return 0.5;
        }

        @Override
        public boolean pecks() {
            return false;
        }

        @Override
        public boolean clings() {
            return false;
        }

        @Override
        public String home() {
            return "headquarters";
        }

        @Override
        public EtoGesture[] idles(EtoMood mood) {
            return switch (mood) {
                case GIDDY -> new EtoGesture[] {EtoGesture.HUM, EtoGesture.STRETCH, EtoGesture.CROSS, EtoGesture.CHIN};
                case SULKY, JEALOUS -> new EtoGesture[] {EtoGesture.CROSS, EtoGesture.HUFF, EtoGesture.TAP, EtoGesture.LOOK};
                case LONELY, BORED -> new EtoGesture[] {EtoGesture.LOOK, EtoGesture.TAP, EtoGesture.STRETCH, EtoGesture.CHIN};
                case CLINGY, AFFECTIONATE -> new EtoGesture[] {EtoGesture.SHY, EtoGesture.CROSS, EtoGesture.LOOK, EtoGesture.CHIN};
                case SLEEPY -> new EtoGesture[] {EtoGesture.YAWN, EtoGesture.STRETCH, EtoGesture.CROSS};
                case BLOODTHIRSTY -> new EtoGesture[] {EtoGesture.CROSS, EtoGesture.TAP, EtoGesture.STRETCH};
                case CLAIMED, WARY -> new EtoGesture[] {EtoGesture.CROSS, EtoGesture.LOOK, EtoGesture.SCOLD, EtoGesture.CHIN};
                default -> new EtoGesture[] {EtoGesture.CROSS, EtoGesture.LOOK, EtoGesture.CHIN, EtoGesture.STRETCH, EtoGesture.TAP};
            };
        }

        @Override
        public EtoGesture[] anyIdle() {
            return new EtoGesture[] {EtoGesture.CROSS, EtoGesture.LOOK, EtoGesture.CHIN, EtoGesture.STRETCH, EtoGesture.TAP, EtoGesture.HUFF,
                EtoGesture.WRITE, EtoGesture.READ};
        }

        @Override
        public Script script() {
            if (this.script == null) {
                this.script = new GeneralScript();
            }
            return this.script;
        }

        /** Paper: he signs every report himself and the building never has enough of it. */
        @Override
        public boolean errandItem(String id) {
            return "minecraft:paper".equals(id);
        }

        /** A cornflower: the blue of the bureau's old colours. */
        @Override
        public boolean flower(String id) {
            return "minecraft:cornflower".equals(id);
        }
    }

    // ---------------------------------------------------------------- Roma: the Clowns' leader

    /**
     * 4.1.49: Roma, who leads the Clowns from their circus tent. A joker whose jokes are mostly about death, delighted by everything,
     * impossible to embarrass, and then, when it counts, suddenly and completely sincere. Touka's size, a skinny woman's figure on the slim
     * arms. Dates anyone. Fights with her legs (Kick Master) and four purple Rinkaku tendrils.
     */
    private static final class Roma extends Persona {
        private Script script;

        @Override
        public String key() {
            return Teaching.ROMA;
        }

        @Override
        public String name() {
            return "Roma";
        }

        @Override
        public String colour() {
            return "\u00a7d";
        }

        /** The owner: "is the size of Touka". */
        @Override
        public float size() {
            return com.hex.kagune.control.FemaleBody.SIZE;
        }

        @Override
        public boolean female() {
            return true;
        }

        /** The owner: "(Alex Model, skinny)": less of the figure's curves than Eto's, narrower, thinner legs. */
        @Override
        public float curves() {
            return 0.75F;
        }

        @Override
        public float build() {
            return 0.94F;
        }

        @Override
        public float legs() {
            return 0.7F;
        }

        @Override
        public float health() {
            return 280.0F;
        }

        /** Strong: she has run the Clowns a long time. */
        @Override
        public float might() {
            return 0.7F;
        }

        @Override
        public double armor() {
            return 12.0;
        }

        @Override
        public int hair() {
            return ROMA_HAIR;
        }

        @Override
        public int eyes() {
            return ROMA_EYES;
        }

        @Override
        public int skin() {
            return ROMA_SKIN;
        }

        @Override
        public boolean oneEyed() {
            return false;
        }

        @Override
        public boolean kickMaster() {
            return true;
        }

        @Override
        public int tendrils() {
            return 4;
        }

        /** The owner: "4 purple tendrils". */
        @Override
        public int kaguneColour() {
            return ROMA_KAGUNE;
        }

        /** She asks for a kiss the way she asks for everything: out of nowhere, grinning. */
        @Override
        public double kissing() {
            return 1.3;
        }

        @Override
        public double forward() {
            return 1.4;
        }

        @Override
        public String home() {
            return "the circus tent";
        }

        @Override
        public EtoGesture[] idles(EtoMood mood) {
            return switch (mood) {
                case GIDDY -> new EtoGesture[] {EtoGesture.LAUGH, EtoGesture.TWIRL, EtoGesture.GIGGLE, EtoGesture.DANCE, EtoGesture.HOP};
                case SULKY, JEALOUS -> new EtoGesture[] {EtoGesture.POUT, EtoGesture.CROSS, EtoGesture.TAP, EtoGesture.HUFF};
                case LONELY, BORED -> new EtoGesture[] {EtoGesture.TWIRL, EtoGesture.KICKS, EtoGesture.LOOK, EtoGesture.STRETCH, EtoGesture.HUM};
                case CLINGY, AFFECTIONATE -> new EtoGesture[] {EtoGesture.GIGGLE, EtoGesture.HAIR, EtoGesture.LEAN, EtoGesture.TILT, EtoGesture.HUM};
                case SLEEPY -> new EtoGesture[] {EtoGesture.YAWN, EtoGesture.STRETCH, EtoGesture.GIGGLE};
                case BLOODTHIRSTY -> new EtoGesture[] {EtoGesture.LICK, EtoGesture.LAUGH, EtoGesture.KICKS, EtoGesture.CRACK};
                case CLAIMED, WARY -> new EtoGesture[] {EtoGesture.TILT, EtoGesture.LAUGH, EtoGesture.TAP, EtoGesture.SHRUG};
                default -> new EtoGesture[] {EtoGesture.LAUGH, EtoGesture.TWIRL, EtoGesture.KICKS, EtoGesture.TILT, EtoGesture.HUM};
            };
        }

        @Override
        public EtoGesture[] anyIdle() {
            return new EtoGesture[] {EtoGesture.LAUGH, EtoGesture.TWIRL, EtoGesture.GIGGLE, EtoGesture.KICKS, EtoGesture.TILT, EtoGesture.STRETCH,
                EtoGesture.DANCE, EtoGesture.SHRUG, EtoGesture.LOOK, EtoGesture.HOP};
        }

        @Override
        public Script script() {
            if (this.script == null) {
                this.script = new RomaScript();
            }
            return this.script;
        }

        /** Fireworks: a show needs a bang. */
        @Override
        public boolean errandItem(String id) {
            return "minecraft:firework_rocket".equals(id);
        }

        /** An allium: the one flower her colour. */
        @Override
        public boolean flower(String id) {
            return "minecraft:allium".equals(id);
        }
    }

    /** 4.1.49: Roma's colours (her skin's hair, eyes and skin; a child of hers takes them) and her tendrils' purple. */
    static final int ROMA_HAIR = 0x8EA1A6; // her grey-teal hair (the owner's skin)
    static final int ROMA_EYES = 0xBDB6B9;
    static final int ROMA_SKIN = 0xF9E2D4;
    public static final int ROMA_KAGUNE = 0x8B3FD9;
}
