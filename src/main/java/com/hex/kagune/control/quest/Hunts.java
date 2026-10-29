package com.hex.kagune.control.quest;

import java.util.List;
import java.util.Random;

/**
 * 7.0.1: the hunts (pure, tested): a night's quest drawn at random at dusk ({@link #draw}), gone at dawn. Which kind, what to
 * kill, how many and what it pays are all rolled, so no two nights ask the same thing; a hunt is rebuilt from its {@link Spec}
 * (five small numbers) whenever the save is read.
 */
public final class Hunts {
    /** What a hunt can ask for. */
    enum Template {
        KILL_MOB, EYES, FEED, BRINK, RAMPAGE, KAGUNE, AIRBORNE, GHOUL
    }

    /** The night's prey: the mob, its name, and how many a hunt asks for at least and at most. */
    record Prey(String id, String name, int min, int max) {
    }

    static final List<Prey> PREY = List.of(
        new Prey("minecraft:zombie", "zombies", 5, 9),
        new Prey("minecraft:skeleton", "skeletons", 4, 8),
        new Prey("minecraft:spider", "spiders", 4, 8),
        new Prey("minecraft:creeper", "creepers", 3, 6),
        new Prey("minecraft:witch", "witches", 1, 3),
        new Prey("minecraft:enderman", "endermen", 2, 4),
        new Prey("minecraft:pillager", "pillagers", 2, 5),
        new Prey("minecraft:vindicator", "vindicators", 1, 3),
        new Prey("minecraft:iron_golem", "iron golems", 1, 2),
        new Prey("minecraft:phantom", "phantoms", 2, 4),
        new Prey("minecraft:slime", "slimes", 3, 6),
        new Prey("minecraft:drowned", "drowned", 3, 6),
        new Prey("minecraft:husk", "husks", 3, 6),
        new Prey("minecraft:stray", "strays", 2, 5),
        new Prey("minecraft:cave_spider", "cave spiders", 3, 6));

    /** What a hunt pays: mastery, Ghoul Meat or (rarely) a mutation point. */
    public static final int PAY_MASTERY = 0;
    public static final int PAY_MEAT = 1;
    public static final int PAY_POINT = 2;
    /** Hunts on at once at most. */
    public static final int MAX_ON = 2;
    /** The chance a dusk brings a hunt (when one may come). */
    public static final double OFFER_CHANCE = 0.6;

    /**
     * One hunt: its template, its parameter (the prey's index for a kill hunt), how many, and what it pays. Saved as
     * "template:param:count:pay:amount".
     */
    public record Spec(int template, int param, int count, int pay, int amount) {
        public String save() {
            return this.template + ":" + this.param + ":" + this.count + ":" + this.pay + ":" + this.amount;
        }

        public static Spec load(String saved) {
            String[] parts = saved.split(":");
            if (parts.length != 5) {
                return null;
            }
            try {
                Spec spec = new Spec(Integer.parseInt(parts[0]), Integer.parseInt(parts[1]), Integer.parseInt(parts[2]), Integer.parseInt(parts[3]),
                    Integer.parseInt(parts[4]));
                return spec.template >= 0 && spec.template < Template.values().length && spec.count > 0 ? spec : null;
            } catch (NumberFormatException bad) {
                return null;
            }
        }
    }

    private Hunts() {
    }

    /** A random hunt; {@code ghouls}: ghoul NPCs roam, so a hunt may ask for one. */
    public static Spec draw(Random random, boolean ghouls) {
        Template[] all = Template.values();
        Template template;
        do {
            template = all[random.nextInt(all.length)];
        } while (template == Template.GHOUL && !ghouls);
        int param = 0;
        int count = switch (template) {
            case KILL_MOB -> {
                param = random.nextInt(PREY.size());
                Prey prey = PREY.get(param);
                yield prey.min() + random.nextInt(prey.max() - prey.min() + 1);
            }
            case EYES -> 6 + random.nextInt(7);
            case FEED -> 4 + random.nextInt(5);
            case BRINK, GHOUL -> 1;
            case RAMPAGE -> 3 + random.nextInt(3);
            case KAGUNE -> 8 + random.nextInt(8);
            case AIRBORNE -> 3 + random.nextInt(3);
        };
        double roll = random.nextDouble();
        int pay = roll < 0.12 ? PAY_POINT : roll < 0.4 ? PAY_MEAT : PAY_MASTERY;
        int amount = switch (pay) {
            case PAY_POINT -> 1;
            case PAY_MEAT -> 1 + (random.nextDouble() < 0.3 ? 1 : 0);
            default -> 15 + random.nextInt(36);
        };
        return new Spec(template.ordinal(), param, count, pay, amount);
    }

    /** The hunt {@code spec} describes, as a quest (its id names the spec, so the same hunt keeps its progress). */
    public static Quest quest(Spec spec) {
        Template template = Template.values()[Math.max(0, Math.min(Template.values().length - 1, spec.template()))];
        int count = spec.count();
        Objective objective;
        String title;
        String story;
        switch (template) {
            case KILL_MOB -> {
                Prey prey = PREY.get(Math.max(0, Math.min(PREY.size() - 1, spec.param())));
                title = "Hunt: " + prey.name();
                story = "Something in you wants " + prey.name() + " tonight. Don't make it wait.";
                objective = Objective.of("Kill " + prey.name(), Signal.KILL, count, Tags.mob(prey.id()));
            }
            case EYES -> {
                title = "Hunt: bare-handed";
                story = "No kagune tonight. Only the eyes, and your hands.";
                objective = Objective.of("Kill hostiles, eyes open", Signal.KILL, count, Tags.HOSTILE, Tags.EYES);
            }
            case FEED -> {
                title = "Hunt: hunger";
                story = "The reserve is never full enough. Eat before the sun comes up.";
                objective = Objective.of("Eat raw flesh", Signal.EAT, count, Tags.FLESH);
            }
            case BRINK -> {
                title = "Hunt: the edge";
                story = "Go looking for a fight you might lose. Then don't lose it.";
                objective = Objective.of("Live through the brink", Signal.BRINK, 1);
            }
            case RAMPAGE -> {
                title = "Hunt: frenzy";
                story = "Kill, and keep killing, before the blood cools.";
                objective = Objective.of("Kill " + count + " in a row", Signal.RAMPAGE, count);
            }
            case KAGUNE -> {
                title = "Hunt: let it out";
                story = "The kagune has been sheathed too long. Feed it.";
                objective = Objective.of("Kill with the kagune out", Signal.KILL, count, Tags.KAGUNE);
            }
            case AIRBORNE -> {
                title = "Hunt: from above";
                story = "Fall on them. Kill them before you touch the ground.";
                objective = Objective.of("Kill in mid-air", Signal.KILL, count, Tags.AIRBORNE);
            }
            default -> {
                title = "Hunt: one of your own";
                story = "A ghoul is out there tonight. Only one of you walks home.";
                objective = Objective.of("Kill a ghoul", Signal.KILL, count, Tags.GHOUL);
            }
        }
        Reward reward = switch (spec.pay()) {
            case PAY_POINT -> Reward.NONE.points(Math.max(1, spec.amount()));
            case PAY_MEAT -> Reward.NONE.meat(Math.max(1, spec.amount()));
            default -> Reward.mastery(Math.max(1, spec.amount()));
        };
        return new Quest("hunt_" + spec.save().replace(':', '_'), Quest.Line.HUNT, null, title, story, 0, null, null, List.of(objective), reward);
    }

    /** What {@code spec} pays, in words. */
    public static String payText(Spec spec) {
        return switch (spec.pay()) {
            case PAY_POINT -> spec.amount() + " mutation point" + (spec.amount() == 1 ? "" : "s");
            case PAY_MEAT -> spec.amount() + " Ghoul Meat";
            default -> spec.amount() + " mastery";
        };
    }
}
