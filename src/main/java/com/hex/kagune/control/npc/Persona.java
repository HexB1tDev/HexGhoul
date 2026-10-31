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
    public static final List<Persona> ALL = List.of(ETO, TOUKA, KEN);

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

    /** 1.3.18: how much of the figure's extra width and depth its legs have (its curves unless said otherwise: Touka's a touch less). */
    public float legs() {
        return this.curves();
    }

    /** True if drawn on the slim arms. */
    public boolean slim() {
        return this.female();
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
            return "her spot";
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
            return "his spot";
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
}
