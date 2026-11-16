package com.hex.kagune.control.quinque;

/**
 * 3.0.0 (the owner: "CCG rework, make the ccg moveset way more complex, better ranks get better custom weapons (super detailed with meshes
 * from the kagunes themselves, tendril whips that can cut and grab, crystal spears, big flesh hammers, big two handed swords made from flesh
 * and bone of kagunes all with long M1 chains and right click abilities that the investigators can use at will"): the higher ranks'
 * quinques, grown from the kagune they were cut from (pure). An investigator carries its case at peace and the weapon tears out of it in a
 * fight; 3.0.6: a player's too ({@link #key}: its own case and weapon items), from an investigator's hand or /give. {@link #carried} is the
 * regular quinque it is held and cuts a ghoul as.
 *
 * <p>Each has a long string of blows ({@link #chain}: four to six, each its own swing, reach, arc and weight) and two moves of its own
 * ({@link #abilities}) it uses as it sees fit, each on its own cooldown.
 */
public enum Arm {
    /**
     * Rinkaku: a tendril whip: a bone handle and, out of a knot of flesh in it, a Rinkaku tendril (drawn as a Rinkaku player's tail is), six
     * blocks long. Six lashes; it binds (wraps a foe and drags them in to be slammed down) and flays (a storm of lashes).
     */
    WHIP("Tendril Whip", "rinkaku", Quinque.MACE, false, 5.5, 6.0F, new Strike[] {
        new Strike("forehand lash", 5, 11, 0.8F, 1.0, 70.0, 0.15, 0.25, 0.0, Strike.SLASH),
        new Strike("backhand lash", 5, 11, 0.8F, 1.0, 70.0, 0.15, 0.25, 0.0, Strike.SLASH),
        new Strike("overhead crack", 7, 14, 1.15F, 1.1, 30.0, 0.2, 0.3, 0.0, Strike.SLASH),
        new Strike("low sweep", 6, 12, 0.85F, 0.95, 90.0, 0.1, 0.2, 0.25, Strike.SLASH),
        new Strike("double lash", 5, 12, 1.0F, 1.0, 60.0, 0.25, 0.3, 0.0, Strike.SLASH),
        new Strike("spinning lash", 9, 22, 1.45F, 0.9, 360.0, 0.0, 0.6, 0.15, Strike.SLASH)},
        Ability.BIND, Ability.FLAY, Ability.SWING),
    /**
     * Ukaku: a crystal spear: a shaft of dark bone, crowned with Ukaku's crystal (a long blade of it, barbs of it round its foot). Five
     * blows; it throws a javelin of crystal (and grows another) and drives through a foe at a run.
     */
    CRYSTAL_SPEAR("Crystal Spear", "ukaku", Quinque.LAUNCHER, true, 3.6, 7.0F, new Strike[] {
        new Strike("thrust", 4, 9, 1.0F, 1.1, 25.0, 0.4, 0.3, 0.0, Strike.STAB),
        new Strike("twin thrust", 4, 9, 1.0F, 1.1, 25.0, 0.35, 0.3, 0.0, Strike.STAB),
        new Strike("rising sweep", 6, 12, 0.9F, 0.85, 120.0, 0.2, 0.3, 0.35, Strike.SLASH),
        new Strike("spinning butt", 6, 12, 0.9F, 0.8, 100.0, 0.1, 0.6, 0.0, Strike.BLUNT),
        new Strike("lunging thrust", 7, 19, 1.65F, 1.25, 25.0, 0.95, 0.7, 0.0, Strike.STAB)},
        Ability.JAVELIN, Ability.PIERCE, null),
    /**
     * Koukaku: a flesh hammer: a long handle of jointed bone bound in sinew, and for a head a great swollen mass of kagune flesh, bone
     * spikes through it. Four slow, crushing blows; it shakes the ground (a slam that throws everyone near up) and whirls.
     */
    FLESH_HAMMER("Flesh Hammer", "koukaku", Quinque.SPEAR, true, 3.0, 9.0F, new Strike[] {
        new Strike("side swing", 10, 20, 1.3F, 1.0, 120.0, 0.25, 0.8, 0.1, Strike.CRUSH),
        new Strike("backswing", 9, 18, 1.2F, 1.0, 120.0, 0.2, 0.8, 0.1, Strike.CRUSH),
        new Strike("uppercut", 9, 20, 1.3F, 0.95, 60.0, 0.3, 0.3, 0.65, Strike.CRUSH),
        new Strike("overhead smash", 12, 28, 2.0F, 1.0, 50.0, 0.35, 0.5, 0.3, Strike.CRUSH)},
        Ability.QUAKE, Ability.WHIRL, Ability.FAULT),
    /**
     * Bikaku: a two-handed greatsword of bone and flesh: a serrated blade of bone with a living spine of flesh down it, a guard of curved
     * ribs, a grip bound in sinew. Six cuts; it leaps in with a crushing downward cut and cleaves (a great sweep that leaves wounds bleeding).
     */
    BONE_SWORD("Bone Greatsword", "bikaku", Quinque.BLADE, true, 3.3, 8.0F, new Strike[] {
        new Strike("falling cut", 6, 13, 1.0F, 1.0, 100.0, 0.3, 0.35, 0.0, Strike.SLASH),
        new Strike("rising cut", 6, 13, 1.0F, 1.0, 100.0, 0.25, 0.35, 0.1, Strike.SLASH),
        new Strike("level sweep", 7, 14, 1.05F, 1.0, 140.0, 0.25, 0.45, 0.0, Strike.SLASH),
        new Strike("thrust", 6, 13, 1.1F, 1.15, 30.0, 0.55, 0.4, 0.0, Strike.STAB),
        new Strike("spinning cut", 8, 16, 1.1F, 0.95, 300.0, 0.1, 0.5, 0.0, Strike.SLASH),
        new Strike("overhead cleave", 10, 24, 1.9F, 1.05, 50.0, 0.45, 0.6, 0.2, Strike.SLASH)},
        Ability.LEAP, Ability.CLEAVE, Ability.GUARD);

    /**
     * One blow of a string: when it lands and how long the swing lasts (ticks, before its rank's pace), how much of the arm's damage it deals,
     * how far it reaches (of the arm's reach), how wide (degrees, the whole arc in front: 360 all round), how far it steps in (blocks), how
     * hard it throws a foe back and up (blocks a tick), and the wound it leaves.
     */
    public record Strike(String name, int land, int total, float damage, double reach, double arc, double step, double push, double lift, int blow) {
        public static final int SLASH = 0;
        public static final int STAB = 1;
        public static final int CRUSH = 2;
        public static final int BLUNT = 3;

        /** True if it reaches all the way round. */
        public boolean round() {
            return this.arc >= 359.0;
        }
    }

    /**
     * Its moves of its own: how long the move lasts, when its blows land ({@link #hits}, ticks in), its cooldown (ticks, before its rank's
     * pace), the distances it is used from (blocks), and how much of the arm's damage each of its blows deals.
     */
    public enum Ability {
        /** The whip thrown round a foe: caught at {@code hits[0]}, dragged in till {@code hits[1]}, slammed down then. */
        BIND("Bind", 32, new int[] {8, 24}, 150, 3.0, 7.5, 2.1F),
        /** A storm of lashes, six of them, each a little blow in a wide arc in front. */
        FLAY("Flay", 38, new int[] {6, 11, 16, 21, 26, 31}, 120, 0.0, 5.5, 0.6F),
        /** The spear drawn back over the shoulder and thrown at {@code hits[0]} as a javelin of crystal; another grows in its hand. */
        JAVELIN("Crystal Javelin", 34, new int[] {14}, 130, 6.0, 22.0, 2.6F),
        /** A run at a foe, through them: crouched till {@code hits[0]}, driving on till {@code hits[1]}. */
        PIERCE("Piercing Charge", 26, new int[] {8, 15}, 100, 3.0, 9.0, 1.9F),
        /** A leap and the hammer brought down on the ground at {@code hits[0]}: everyone near thrown up. */
        QUAKE("Quake", 32, new int[] {14}, 140, 0.0, 4.5, 1.7F),
        /** Two whole turns, the hammer swung round: three blows to everyone within reach. */
        WHIRL("Whirlwind", 36, new int[] {10, 16, 22}, 110, 0.0, 3.6, 0.85F),
        /** A crouch, a leap at {@code hits[0]} at its foe, and a cut down onto them where it lands at {@code hits[1]}. */
        LEAP("Leaping Cut", 30, new int[] {6, 17}, 120, 4.0, 12.0, 2.2F),
        /** A great sweep at {@code hits[0]}, wider than any of its cuts, leaving what it cuts bleeding. */
        CLEAVE("Cleave", 30, new int[] {12}, 115, 0.0, 4.8, 1.85F),
        /**
         * 3.1.8 (the owner: "improve the move sets of the briefcase ... I meant the heavy blade, hammer and whip"): the whip's own (Sneak +
         * M1): the tendril lashed onto the block aimed at ({@link Arm#SWING_REACH}), latched at {@code hits[0]}, and him hauled to it.
         */
        SWING("Tendril Swing", 22, new int[] {5}, 70, 0.0, 16.0, 0.0F),
        /** 3.1.8: the hammer's own: driven into the ground at {@code hits[0]}, the ground breaking open on along a line, a blow at each hit. */
        FAULT("Fault Line", 30, new int[] {11, 13, 15, 17, 19, 21}, 130, 0.0, 10.5, 1.25F),
        /**
         * 3.1.8: the greatsword's own: its flat raised before him as a wall. Blows that reach him through it are mostly stopped
         * ({@link Arm#GUARD_TAKES}); one in its first {@link Arm#GUARD_PERFECT} ticks is parried outright, its striker staggered, and his next
         * blow is the string's last (its overhead cleave).
         */
        GUARD("Bone Wall", 22, new int[] {1}, 80, 0.0, 0.0, 0.0F);

        public final String title;
        public final int total;
        public final int[] hits;
        public final int cooldown;
        public final double nearest;
        public final double farthest;
        public final float damage;

        Ability(String title, int total, int[] hits, int cooldown, double nearest, double farthest, float damage) {
            this.title = title;
            this.total = total;
            this.hits = hits;
            this.cooldown = cooldown;
            this.nearest = nearest;
            this.farthest = farthest;
            this.damage = damage;
        }

        /** True if {@code distance} (blocks) is one it uses this from. */
        public boolean fits(double distance) {
            return distance >= this.nearest && distance <= this.farthest;
        }

        public static Ability byOrdinal(int ordinal) {
            Ability[] all = values();
            return ordinal >= 0 && ordinal < all.length ? all[ordinal] : null;
        }
    }

    public final String title;
    /** The kagune it was cut from. */
    public final String type;
    /** The quinque case it is carried in at peace (one of that kagune's). */
    public final Quinque carried;
    public final boolean twoHanded;
    /** How far its blows reach (blocks, before a blow's own share). */
    public final double reach;
    /** Its base damage (before a blow's share, its rank's power and a masterwork's edge). */
    public final float damage;
    private final Strike[] chain;
    private final Ability[] abilities;
    /** 3.1.8: its own third move, on Sneak + M1 (none for the crystal spear); an investigator never uses it. */
    private final Ability signature;

    Arm(String title, String type, Quinque carried, boolean twoHanded, double reach, float damage, Strike[] chain, Ability first, Ability second,
        Ability signature) {
        this.title = title;
        this.type = type;
        this.carried = carried;
        this.twoHanded = twoHanded;
        this.reach = reach;
        this.damage = damage;
        this.chain = chain;
        this.abilities = new Ability[] {first, second};
        this.signature = signature;
    }

    /** 3.1.8: its third move, on Sneak + M1 in a player's hand, or null. */
    public Ability signature() {
        return this.signature;
    }

    /** 3.1.8: how far the Tendril Swing lashes onto a block; the Bone Wall's perfect window (ticks) and what it lets through after. */
    public static final double SWING_REACH = 16.0;
    public static final int GUARD_PERFECT = 5;
    public static final float GUARD_TAKES = 0.25F;

    /** How many blows its string has. */
    public int length() {
        return this.chain.length;
    }

    /** Its {@code step}th blow (wrapped round its string). */
    public Strike strike(int step) {
        return this.chain[Math.floorMod(step, this.chain.length)];
    }

    /** Its two moves of its own. */
    public Ability[] abilities() {
        return this.abilities.clone();
    }

    /** Which of its moves {@code ability} is (0 or 1; 3.1.8: 2 its signature), or -1 if it isn't one of its. */
    public int slot(Ability ability) {
        for (int i = 0; i < this.abilities.length; i++) {
            if (this.abilities[i] == ability) {
                return i;
            }
        }
        return ability != null && ability == this.signature ? 2 : -1;
    }

    public static Arm byOrdinal(int ordinal) {
        Arm[] all = values();
        return ordinal >= 0 && ordinal < all.length ? all[ordinal] : null;
    }

    // ---------------------------------------------------------------- 3.0.6: in a player's hands

    /**
     * 3.0.6 (the owner: "it had the new spear that we just created, but it dropped the old spear ... it's not in the game for the player,
     * it's only for the NPCs"): each is an item too, its own case and the weapon out of it ({@link #caseId}, {@link #weaponId}), left by the
     * investigators who carry one and given by /give, wielded by a player as an investigator wields it (its string on M1, its two moves
     * on right click and Shift + right click). Its items' name.
     */
    public String key() {
        return switch (this) {
            case WHIP -> "tendril_whip";
            case CRYSTAL_SPEAR -> "crystal_spear";
            case FLESH_HAMMER -> "flesh_hammer";
            case BONE_SWORD -> "bone_greatsword";
        };
    }

    /** Its case's item id (shut). */
    public String caseId() {
        return "quinque_case_" + this.key();
    }

    /** Its weapon's item id (out of its case). */
    public String weaponId() {
        return "quinque_" + this.key();
    }

    /** The arm whose case or weapon has {@code id} ("hexkagune:..." or bare), or null. */
    public static Arm byId(String id) {
        if (id == null) {
            return null;
        }
        String bare = id.startsWith("hexkagune:") ? id.substring("hexkagune:".length()) : id;
        for (Arm arm : values()) {
            if (arm.caseId().equals(bare) || arm.weaponId().equals(bare)) {
                return arm;
            }
        }
        return null;
    }
}
