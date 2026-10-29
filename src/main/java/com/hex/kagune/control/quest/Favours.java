package com.hex.kagune.control.quest;

import com.hex.kagune.control.npc.NpcIdentity;
import com.hex.kagune.control.npc.NpcTemper;
import java.util.List;
import java.util.Random;

/**
 * 7.0.2: favours (pure, tested): what a ghoul NPC asks of a player it talks to, taken on in conversation and kept in their journal
 * (the ghoul's way) until it is done. What it asks fits its temper (a drifter's errands, Anteiku's protecting, the Pack's killing),
 * is rolled from the ghoul's own seed and the favours already done for it (the same ghoul asks the same thing until then), and pays
 * mastery, Ghoul Meat or now and then a mutation point; done, the ghoul thinks better of whoever did it. Rebuilt from its
 * {@link Spec} and its giver whenever the save is read, as a hunt is.
 */
public final class Favours {
    /** What a favour can ask for. */
    enum Template {
        KILL_MOB, NIGHT_GUARD, FEED, EYES, GHOUL, FERAL, ANTEIKU_HUNT, KAGUNE
    }

    /** What each temper asks (a feral asks nothing: it does not talk). */
    static List<Template> asks(NpcTemper temper) {
        return switch (temper) {
            case DRIFTER -> List.of(Template.KILL_MOB, Template.EYES, Template.FEED);
            case SCAVENGER -> List.of(Template.FEED, Template.KILL_MOB, Template.GHOUL);
            case ANTEIKU -> List.of(Template.NIGHT_GUARD, Template.FERAL, Template.NIGHT_GUARD);
            case PACK -> List.of(Template.GHOUL, Template.ANTEIKU_HUNT, Template.KAGUNE);
            case GOURMET -> List.of(Template.GHOUL, Template.KAGUNE);
            case FERAL -> List.of();
        };
    }

    /** One favour: its template, its parameter (the prey's index for a kill), how many, what it pays. Saved "t:p:c:pay:amount". */
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

    private Favours() {
    }

    /** What the ghoul {@code who} asks after {@code done} favours for this player (null: it asks nothing). */
    public static Spec offer(NpcIdentity who, int done) {
        List<Template> asks = asks(who.temper());
        if (asks.isEmpty()) {
            return null;
        }
        Random random = new Random(who.seed() * 31L + done * 7919L + who.temper().ordinal());
        Template template = asks.get((done + random.nextInt(asks.size())) % asks.size());
        int param = 0;
        int count = switch (template) {
            case KILL_MOB -> {
                param = random.nextInt(Hunts.PREY.size());
                Hunts.Prey prey = Hunts.PREY.get(param);
                yield prey.min() + random.nextInt(prey.max() - prey.min() + 1);
            }
            case NIGHT_GUARD -> 8 + random.nextInt(7);
            case FEED -> 4 + random.nextInt(5);
            case EYES -> 6 + random.nextInt(5);
            case GHOUL, FERAL, ANTEIKU_HUNT -> 1;
            case KAGUNE -> 10 + random.nextInt(7);
        };
        double roll = random.nextDouble();
        int pay = roll < 0.06 ? Hunts.PAY_POINT : roll < 0.4 ? Hunts.PAY_MEAT : Hunts.PAY_MASTERY;
        int amount = switch (pay) {
            case Hunts.PAY_POINT -> 1;
            case Hunts.PAY_MEAT -> 1 + (template == Template.GHOUL || template == Template.FERAL ? 1 : 0);
            default -> 20 + random.nextInt(21) + (count == 1 ? 15 : 0);
        };
        return new Spec(template.ordinal(), param, count, pay, amount);
    }

    /**
     * The favour {@code spec}, asked by {@code giver} (the ghoul's UUID) who is {@code who}, the {@code number}th it asked of this
     * player, as a quest (its id names all of that, so the same favour keeps its progress).
     */
    public static Quest quest(Spec spec, String giver, NpcIdentity who, int number) {
        Template template = Template.values()[Math.max(0, Math.min(Template.values().length - 1, spec.template()))];
        int count = spec.count();
        String name = who.displayName();
        Objective objective;
        String story;
        switch (template) {
            case KILL_MOB -> {
                Hunts.Prey prey = Hunts.PREY.get(Math.max(0, Math.min(Hunts.PREY.size() - 1, spec.param())));
                story = name + " wants the " + prey.name() + " round their spot thinned out.";
                objective = Objective.of("Kill " + prey.name(), Signal.KILL, count, Tags.mob(prey.id()));
            }
            case NIGHT_GUARD -> {
                story = name + ": people still walk about after dark. Keep what hunts at night off them.";
                objective = Objective.of("Kill hostiles at night", Signal.KILL, count, Tags.HOSTILE, Tags.NIGHT);
            }
            case FEED -> {
                story = name + " wants to see you live off what the night leaves behind.";
                objective = Objective.of("Eat raw flesh", Signal.EAT, count, Tags.FLESH);
            }
            case EYES -> {
                story = name + ": fight as ghouls did before the kagune. Eyes only.";
                objective = Objective.of("Kill hostiles, eyes open", Signal.KILL, count, Tags.HOSTILE, Tags.EYES);
            }
            case GHOUL -> {
                story = name + " says there is a ghoul out there who has it coming.";
                objective = Objective.of("Kill a ghoul", Signal.KILL, count, Tags.GHOUL);
            }
            case FERAL -> {
                story = name + ": a feral ghoul is loose. It will kill someone if no one stops it.";
                objective = Objective.of("Kill a feral ghoul", Signal.KILL, count, Tags.temper("feral"));
            }
            case ANTEIKU_HUNT -> {
                story = name + ": Anteiku's soft-hearted lot keep getting in the Pack's way. Remove one.";
                objective = Objective.of("Kill a ghoul of Anteiku", Signal.KILL, count, Tags.temper("anteiku"));
            }
            default -> {
                story = name + " wants to see what you are. Let it out.";
                objective = Objective.of("Kill with the kagune out", Signal.KILL, count, Tags.KAGUNE);
            }
        }
        Reward reward = switch (spec.pay()) {
            case Hunts.PAY_POINT -> Reward.NONE.points(Math.max(1, spec.amount()));
            case Hunts.PAY_MEAT -> Reward.NONE.meat(Math.max(1, spec.amount()));
            default -> Reward.mastery(Math.max(1, spec.amount()));
        };
        String id = "favour_" + giver.replace("-", "").substring(0, Math.min(12, giver.replace("-", "").length())) + "_" + number + "_"
            + spec.save().replace(':', '_');
        return new Quest(id, Quest.Line.FAVOUR, null, "Favour: " + name, story, 0, null, null, List.of(objective), reward);
    }

    /** What the ghoul says when it asks (its story, and what it pays). */
    public static String asking(Spec spec, NpcIdentity who) {
        Quest quest = quest(spec, "0", who, 0);
        Objective objective = quest.objectives().get(0);
        return objective.text() + (objective.count() > 1 ? " (" + objective.count() + ")" : "") + ". It pays " + Hunts.payText(new Hunts.Spec(0, 0, 1,
            spec.pay(), spec.amount())) + ".";
    }
}
