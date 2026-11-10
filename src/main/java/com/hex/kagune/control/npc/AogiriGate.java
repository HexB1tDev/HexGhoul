package com.hex.kagune.control.npc;

import java.util.List;
import java.util.UUID;

/**
 * 4.0.2: what is said at the Aogiri Tree hideout's gate (pure, tested; the server's side is {@code GhoulNpcTalk.gate} and
 * {@code AogiriGuard}): the guard's question, the answers he takes and the ones he doesn't, and which a player is offered.
 */
public final class AogiriGate {
    /** What he says, stopping a newcomer. */
    public static final String QUESTION = "Hold it. *He steps in front of you, mask tilted.* This is Aogiri ground. What's your business here?";

    private AogiriGate() {
    }

    /** One answer at the gate: its words, whether the guard likes it, what he says to it. */
    public record Answer(String label, boolean welcome, String reply) {
    }

    /**
     * 4.0.2 (the owner: "first time when you arrive, they should give you a little message with multiple choices ... Just asking you the
     * reason why you're there ... If he doesn't like your answer, we're going to have a bunch of different choices. If he doesn't like it,
     * they'll attack you"): what may be said at the gate, the answers he takes and the ones he doesn't.
     */
    public static final List<Answer> ANSWERS = List.of(
        new Answer("I want to join Aogiri Tree.", true, "Heh. Everyone wants in till they see what we eat. Go on. Don't touch anything."),
        new Answer("I'm here to see Ayato.", true, "Kirishima? East end, first floor. He won't be glad to see you either. Go."),
        new Answer("Noro. I'm here for Noro.", true, "*He looks at you a long moment.* ...Top floor. West. Don't make noise."),
        new Answer("I'm a ghoul. I need somewhere the Doves won't find me.", true, "Then you're in the right place. Keep your head down."),
        new Answer("I hunt Doves. Heard you do too.", true, "*A laugh behind the mask.* Then we'll get along. In."),
        new Answer("None of your business.", false, "Wrong answer."),
        new Answer("Just passing through.", false, "Nobody passes through here. Nobody."),
        new Answer("The CCG sent me.", false, "Then the CCG can come collect what's left of you."),
        new Answer("I'm here to wipe you all out.", false, "...Brave. Stupid, but brave."),
        new Answer("Get out of my way.", false, "*He doesn't move. Behind him, masks turn your way.*"));
    /** How many answers he is given (always some of each kind: shuffled for each player). */
    public static final int OFFERED = 6;

    /** The answers {@code player} is offered at the gate: {@link #OFFERED} of them, three he takes and three he doesn't, in their own order. */
    public static List<Answer> answers(UUID player) {
        java.util.Random random = new java.util.Random(player.getMostSignificantBits() ^ player.getLeastSignificantBits() ^ 0xA061L);
        List<Answer> good = new java.util.ArrayList<>();
        List<Answer> bad = new java.util.ArrayList<>();
        for (Answer answer : ANSWERS) {
            (answer.welcome() ? good : bad).add(answer);
        }
        java.util.Collections.shuffle(good, random);
        java.util.Collections.shuffle(bad, random);
        List<Answer> out = new java.util.ArrayList<>(good.subList(0, OFFERED / 2));
        out.addAll(bad.subList(0, OFFERED - OFFERED / 2));
        java.util.Collections.shuffle(out, random);
        return out;
    }

}
