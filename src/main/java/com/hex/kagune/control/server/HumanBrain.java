package com.hex.kagune.control.server;

import com.hex.kagune.control.ControlItems;
import com.hex.kagune.control.npc.FearLines;
import com.hex.kagune.control.npc.FinalSight;
import com.hex.kagune.control.npc.GhoulNpc;
import com.hex.kagune.control.npc.HumanNpc;
import com.hex.kagune.control.npc.HumanRules;
import com.hex.kagune.data.KaguneProfile;
import java.util.UUID;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.event.entity.living.LivingHurtEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

/**
 * Home Living (1.3.40): what The Human does ({@link HumanRules}), on the server: given a look the first time it ticks (kept in its saved
 * data), it wanders a few steps now and then; a ghoul that shows itself (a ghoul player with its kagune out, or one come too close; a
 * wild ghoul) or anyone who has hurt it sends it running straight away from them, its arms up, round whatever is in the way, until it
 * is far enough to stop, frightened a while after. 1.3.65: War and the Dragon it sees from much further and runs from at a sprint,
 * screaming (a bubble over its head); a final kakuja from further than a kagune.
 */
@Mod.EventBusSubscriber(modid = "hexkagune")
public final class HumanBrain {
    private HumanBrain() {
    }

    /** The server's state for one. */
    public static final class Mind {
        UUID hurtBy;
        long hurtUntil;
        long shakenUntil;
        long nextWander;
        long nextPlan;
        /** 1.3.65: a terror it saw (War, the Dragon: a player's entity id) and until when it keeps running from it out of sight. */
        int dreadOf;
        long dreadUntil = Long.MIN_VALUE;
        /** 1.3.65: what it last ran from (an entity id, 0 nothing) and when it last screamed. */
        int ranFrom;
        long screamAt = Long.MIN_VALUE / 2;
        /** 1.3.72: a trade in the street it is in (null: none), when it may start another, when it last had a word in passing. */
        VillageTrade trade;
        long tradeReady = Long.MIN_VALUE / 2;
        long greetAt = Long.MIN_VALUE / 2;
        boolean dusk;
        /** 1.3.77: the move it is making (HumanGestures code, 0 none), until when, how many it has made, what comes after (a kiss's second beat). */
        int gesture;
        long gestureUntil;
        int gestures;
        int then;
        /** 1.3.77: the player it is making it with (hearts between them), and when it next does something by itself in the street. */
        UUID with;
        long idleAt = Long.MIN_VALUE / 2;
        /** 1.3.77: a move it walks up to its partner ({@code with}) to make (a kiss, a hug), till when it tries, what after, its line. */
        int pending;
        long pendingUntil;
        int pendingThen;
        String pendingLine;
        /** 1.3.78: on a date: whose hand it holds (null: nobody's), whether they were crouching last tick, when it next says a word. */
        UUID holding;
        boolean holdShift;
        long strollAt;
        /** 2.2.6: a player it is coming over to by itself (null: none), why, until when it tries, and its roll for what it says. */
        UUID comeTo;
        com.hex.kagune.control.village.HumanApproach.Why comeWhy;
        long comeUntil;
        int comeRoll;
        /** 2.3.4: a kiss as a companion's goes (EtoRules.kiss: waiting, blushing, pulling them in), since when, Shift held, what it says. */
        com.hex.kagune.control.npc.EtoRules.Kiss kiss;
        long kissAt;
        int kissHeld;
        String kissLine;
    }

    /** 1.3.78: it takes {@code player}'s hand for a walk (a date), glued at their right side until one of them lets go. */
    public static void takeHand(HumanNpc human, ServerPlayer player, long now) {
        Mind mind = human.mind;
        if (mind == null) {
            return;
        }
        mind.holding = player.m_20148_();
        mind.with = player.m_20148_();
        mind.holdShift = player.m_6047_();
        mind.strollAt = now + 200L + human.m_217043_().m_188503_(200);
        mind.pending = 0;
        if (mind.trade != null) {
            mind.trade.end((ServerLevel)human.m_9236_(), now);
        }
    }

    /**
     * 1.3.78: hand in hand (EtoBrain's way): every tick at its partner's right side ({@link com.hex.kagune.control.village.HumanHand}),
     * facing the way they face, through whatever is there (it never chokes in a wall), a word now and then, a heart. It lets go when they
     * crouch, go far or die, and at the village's edge: a villager of the Brick Village never goes past it.
     */
    static void holdHands(HumanNpc human, Mind mind, VillagePlace.Place place, ServerLevel level, long now) {
        net.minecraft.world.entity.Entity found = level.m_8791_(mind.holding);
        if (!(found instanceof ServerPlayer player) || !player.m_6084_() || player.m_5833_() || player.m_20280_(human) > 12.0 * 12.0) {
            letGo(human, mind, level, now, null);
            return;
        }
        com.hex.kagune.control.village.HumanPersona.Temper temper = temper(human);
        boolean shift = player.m_6047_();
        if (shift && !mind.holdShift) {
            letGo(human, mind, level, now, com.hex.kagune.control.village.HumanPersona.letGo(temper, human.m_217043_().m_188503_(1000)));
            return;
        }
        mind.holdShift = shift;
        float yaw = player.f_20883_;
        double[] off = com.hex.kagune.control.village.HumanHand.offset(yaw, com.hex.kagune.control.village.HumanHand.out(HumanRules.slim(human.look()),
            com.hex.kagune.control.Genders.female(player)));
        double x = player.m_20185_() + off[0];
        double z = player.m_20189_() + off[1];
        if (place != null && !place.inside(x, z)) {
            letGo(human, mind, level, now, com.hex.kagune.control.village.HumanPersona.edge(temper, human.m_217043_().m_188503_(1000)));
            return;
        }
        int hand = com.hex.kagune.control.village.HumanGestures.of(com.hex.kagune.control.npc.EtoGesture.HAND);
        if (mind.gesture != hand) {
            gesture(human, hand, 0, now);
        }
        mind.gestureUntil = Long.MAX_VALUE;
        human.holding(player.m_19879_());
        human.f_19794_ = true;
        human.m_20242_(true);
        human.m_21573_().m_26573_();
        human.m_6021_(x, player.m_20186_(), z);
        human.m_20256_(player.m_20184_());
        human.f_19789_ = 0.0F;
        human.m_146922_(yaw);
        human.f_20883_ = yaw;
        human.f_20885_ = yaw;
        if (now % 80L == 0L) {
            hearts(human, level, 1);
        }
        if (now >= mind.strollAt) {
            mind.strollAt = now + 300L + human.m_217043_().m_188503_(400);
            say(human, level, com.hex.kagune.control.village.HumanPersona.stroll(temper, human.m_217043_().m_188503_(1000)));
        }
    }

    /** 2.3.4: it leans in for {@code partner}'s kiss and waits, as a companion does ({@code line}: what it says, kissed). */
    static void startKiss(HumanNpc human, Mind mind, ServerPlayer partner, long now, String line) {
        mind.kiss = com.hex.kagune.control.npc.EtoRules.Kiss.WAIT;
        mind.kissAt = now;
        mind.kissHeld = 0;
        mind.kissLine = line;
        mind.with = partner.m_20148_();
        held(human, com.hex.kagune.control.npc.EtoGesture.KISS, now);
        partner.m_5661_(net.minecraft.network.chat.Component.m_237113_("\u00a7d\u2764 " + name(human) + " leans in, eyes closed... \u00a77(tap or hold Shift)"), true);
    }

    /** 2.3.4: a gesture held until the kiss moves on (never timed out under it, as before: it stopped as Shift was held). */
    private static void held(HumanNpc human, com.hex.kagune.control.npc.EtoGesture gesture, long now) {
        gesture(human, com.hex.kagune.control.village.HumanGestures.of(gesture), 0, now);
        human.mind.gestureUntil = Long.MAX_VALUE;
    }

    /**
     * 2.3.4 (the owner: "if [your partner]'s a human, the kiss mechanics don't seem to apply like [the companions'] ... you should be
     * able to hold shift to make out with them"): the kiss as {@code EtoRules.kiss} has it: a tap of Shift a quick kiss; held, a blush;
     * held longer, it pulls them in; let go, kissed. Left waiting too long, it gives up.
     */
    static void kissing(HumanNpc human, Mind mind, ServerLevel level, long now) {
        net.minecraft.world.entity.Entity found = mind.with == null ? null : level.m_8791_(mind.with);
        if (!(found instanceof ServerPlayer player) || !player.m_6084_() || player.m_20280_(human) > 6.0 * 6.0) {
            mind.kiss = null;
            mind.gesture = 0;
            human.gesture(0);
            return;
        }
        human.m_21573_().m_26573_();
        face(human, player);
        human.m_21563_().m_24960_(player, 30.0F, 30.0F);
        boolean shift = player.m_6047_();
        int released = !shift && mind.kissHeld > 0 ? mind.kissHeld : 0;
        mind.kissHeld = shift ? mind.kissHeld + 1 : 0;
        com.hex.kagune.control.npc.EtoRules.Kiss before = mind.kiss;
        com.hex.kagune.control.npc.EtoRules.Kiss after = com.hex.kagune.control.npc.EtoRules.kiss(before, now - mind.kissAt, shift, mind.kissHeld,
            released, Math.sqrt(player.m_20280_(human)));
        if (after == before) {
            if (before == com.hex.kagune.control.npc.EtoRules.Kiss.BLUSH && now % 15L == 0L
                || before == com.hex.kagune.control.npc.EtoRules.Kiss.PULL && now % 10L == 0L) {
                hearts(human, level, before == com.hex.kagune.control.npc.EtoRules.Kiss.PULL ? 2 : 1);
            }
            return;
        }
        com.hex.kagune.control.village.HumanPersona.Temper temper = temper(human);
        int roll = human.m_217043_().m_188503_(1000);
        switch (after) {
            case BLUSH -> {
                mind.kiss = after;
                mind.kissAt = now;
                held(human, com.hex.kagune.control.npc.EtoGesture.BLUSH, now);
                hearts(human, level, 3);
            }
            case PULL -> {
                mind.kiss = after;
                held(human, com.hex.kagune.control.npc.EtoGesture.PULL, now);
                hearts(human, level, 8);
            }
            case KISSED -> {
                mind.kiss = null;
                gesture(human, com.hex.kagune.control.village.HumanGestures.of(com.hex.kagune.control.npc.EtoGesture.KISSED), 0, now);
                hearts(human, level, before == com.hex.kagune.control.npc.EtoRules.Kiss.PULL ? 14 : 8);
                say(human, level, mind.kissLine != null ? mind.kissLine : com.hex.kagune.control.village.HumanRomance.kissed(temper, roll));
            }
            default -> {
                mind.kiss = null;
                gesture(human, com.hex.kagune.control.village.HumanGestures.of(com.hex.kagune.control.npc.EtoGesture.POUT), 0, now);
                say(human, level, com.hex.kagune.control.village.HumanRomance.hanging(temper, roll));
            }
        }
    }

    /** 1.3.78: it lets go of the hand it holds (saying {@code line}, if any), back to a body that collides and falls. */
    static void letGo(HumanNpc human, Mind mind, ServerLevel level, long now, String line) {
        if (mind.holding == null) {
            return;
        }
        net.minecraft.world.entity.Entity by = level.m_8791_(mind.holding);
        mind.holding = null;
        human.holding(-1);
        EtoBrain.solid(human, by instanceof net.minecraft.world.entity.LivingEntity living ? living : null);
        if (mind.gesture == com.hex.kagune.control.village.HumanGestures.of(com.hex.kagune.control.npc.EtoGesture.HAND)) {
            mind.gesture = 0;
            human.gesture(0);
        }
        human.m_21573_().m_26573_();
        mind.nextWander = now + 60L;
        if (line != null) {
            say(human, level, line);
            gesture(human, com.hex.kagune.control.village.HumanGestures.of(com.hex.kagune.control.npc.EtoGesture.WAVE), 0, now);
        }
    }

    /** 1.3.77: {@code human} makes move {@code code} (HumanGestures), every client drawing it; {@code then}: the move after it, or 0. */
    public static void gesture(HumanNpc human, int code, int then, long now) {
        Mind mind = human.mind;
        if (mind == null || code <= 0) {
            return;
        }
        mind.gesture = code;
        mind.gestureUntil = now + com.hex.kagune.control.village.HumanGestures.ticks(code);
        mind.then = then;
        mind.gestures++;
        com.hex.kagune.control.village.HumanPersona.Temper temper = temper(human);
        human.gesture(com.hex.kagune.control.village.HumanGestures.pack(code, com.hex.kagune.control.village.HumanGestures.style(temper), mind.gestures));
    }

    /** 1.3.77: its move over, the next (a kiss's second beat, with hearts), else nothing; a fright ends any. */
    static void gestures(HumanNpc human, Mind mind, ServerLevel level, long now, boolean frightened) {
        if (mind.gesture == 0) {
            return;
        }
        if (frightened) {
            mind.gesture = 0;
            mind.then = 0;
            human.gesture(0);
            return;
        }
        if (mind.gestureUntil == Long.MAX_VALUE && mind.kiss == null && mind.holding == null) {
            mind.gestureUntil = now; // 3.2.3: held for a kiss or a hand that's over: never left frozen in it
        }
        if (now < mind.gestureUntil) {
            return;
        }
        int next = mind.then;
        mind.gesture = 0;
        mind.then = 0;
        human.gesture(0);
        if (next > 0) {
            gesture(human, next, 0, now);
            if (com.hex.kagune.control.village.HumanGestures.gesture(next) == com.hex.kagune.control.npc.EtoGesture.KISSED) {
                hearts(human, level, 8);
            }
        }
    }

    /** 1.3.77: hearts between it and the player it is with (or over its head). */
    static void hearts(HumanNpc human, ServerLevel level, int count) {
        net.minecraft.world.entity.Entity other = human.mind == null || human.mind.with == null ? null : level.m_8791_(human.mind.with);
        double x = human.m_20185_();
        double z = human.m_20189_();
        if (other != null && other.m_20280_(human) < 16.0) {
            x = (x + other.m_20185_()) * 0.5;
            z = (z + other.m_20189_()) * 0.5;
        }
        level.m_8767_(net.minecraft.core.particles.ParticleTypes.f_123750_, x, human.m_20186_() + 1.7, z, count, 0.3, 0.25, 0.3, 0.02);
    }

    /** 2.0: how often (ticks) a calm villager looks around for danger. */
    static final long LOOKS = 4L;

    /** 2.0: in its saved data: its name (a village's own, by its slot), and that its village gave it its look and name. */
    static final String NAME = "HexHumanName";
    /** 2.0.36: its temper (ordinal), saved for a villager placed since 2.0.36 (any of them); one placed before has its legacy one. */
    static final String TEMPER = "HexHumanTemper";

    /** 2.0.36: {@code human}'s temper: its saved one, else the one its seed always gave it (one of the first eight). */
    public static com.hex.kagune.control.village.HumanPersona.Temper temper(HumanNpc human) {
        CompoundTag saved = human.getPersistentData();
        if (saved.m_128441_(TEMPER)) {
            com.hex.kagune.control.village.HumanPersona.Temper[] all = com.hex.kagune.control.village.HumanPersona.Temper.values();
            return all[Math.floorMod(saved.m_128451_(TEMPER), all.length)];
        }
        return com.hex.kagune.control.village.HumanPersona.temper(HumanTalk.seed(human));
    }
    static final String UNIQUE = "HexHumanUnique";

    /** 2.1.3: in its saved data: its name among a woman's or a man's (a villager placed since), and its job (ordinal + 1). */
    static final String NAME2 = "HexHumanName2";
    static final String JOB = "HexHumanJob";

    /**
     * 2.0: its name: its village's for it, else by its seed. 2.1.3: a woman's or a man's name for one placed since (one named before keeps
     * its own).
     */
    public static String name(HumanNpc human) {
        CompoundTag saved = human.getPersistentData();
        boolean woman = HumanRules.female(human.look());
        if (saved.m_128441_(NAME2)) {
            return com.hex.kagune.control.village.HumanLife.name(woman, saved.m_128451_(NAME2));
        }
        return saved.m_128441_(NAME) ? com.hex.kagune.control.village.HumanPersona.named(saved.m_128451_(NAME))
            : com.hex.kagune.control.village.HumanLife.name(woman, (int)Math.floorMod(HumanTalk.seed(human) >>> 7,
            (long)com.hex.kagune.control.village.HumanLife.names(woman)));
    }

    /** 2.1.3: {@code human}'s life (its job its village's for it, if it gave one; the rest its seed's). */
    public static com.hex.kagune.control.village.HumanLife.Life life(HumanNpc human) {
        CompoundTag saved = human.getPersistentData();
        com.hex.kagune.control.village.HumanLife.Job[] jobs = com.hex.kagune.control.village.HumanLife.Job.values();
        com.hex.kagune.control.village.HumanLife.Job job = saved.m_128441_(JOB) ? jobs[Math.floorMod(saved.m_128451_(JOB) - 1, jobs.length)] : null;
        return com.hex.kagune.control.village.HumanLife.of(HumanTalk.seed(human), job);
    }

    /** 1.3.65: what it runs from, how bad a sight it is, and whether it is a ghoul it screams about. */
    record Danger(LivingEntity body, FinalSight sight, boolean ghoul) {
    }

    public static void tick(HumanNpc human) {
        if (!(human.m_9236_() instanceof ServerLevel level)) {
            return;
        }
        long now = level.m_46467_();
        Mind mind = human.mind;
        if (mind == null) {
            mind = human.mind = new Mind();
            CompoundTag saved = human.getPersistentData();
            int look = saved.m_128441_(HumanNpc.KEY) ? saved.m_128451_(HumanNpc.KEY)
                : HumanRules.pick(human.m_217043_().m_188503_(1 << 20)); // 1.3.75: a villager's own look;
            saved.m_128405_(HumanNpc.KEY, look);
            human.dress(look);
            mind.nextWander = now + 40 + human.m_217043_().m_188503_(80);
            // 1.3.78: saved mid-date (no gravity is kept with it), it comes back a body that falls; nobody's hand in it.
            human.m_20242_(false);
            human.holding(-1);
        }
        VillagePlace.Place place = VillagePlace.of(human); // 1.3.71: one of the Brick Village's people (null: anyone else)
        // 2.0: once, a village's own look and name for it, by its slot (no two of a village's people share either).
        if (place != null && !human.getPersistentData().m_128441_(UNIQUE)) {
            int slot = Math.max(0, VillagePlace.slot(human));
            CompoundTag saved = human.getPersistentData();
            int look = HumanRules.villageLook(place.x(), place.z(), slot);
            saved.m_128405_(HumanNpc.KEY, look);
            saved.m_128405_(NAME, HumanRules.villageName(place.x(), place.z(), slot, com.hex.kagune.control.village.HumanPersona.names()));
            // 2.1.3: a woman's name or a man's, none shared in its village.
            boolean woman = HumanRules.female(look);
            saved.m_128405_(NAME2, HumanRules.villageName(place.x(), place.z() + (woman ? 1 : 0), slot,
                com.hex.kagune.control.village.HumanLife.names(woman)));
            saved.m_128405_(UNIQUE, 1);
            // 2.0.36: a villager placed from now on may have any temper (the new ones too), kept with it.
            saved.m_128405_(TEMPER, com.hex.kagune.control.village.HumanPersona.rolled(HumanTalk.seed(human)).ordinal());
            human.dress(look);
        }
        // 2.1.3: once, a village's own job for each of its people (none shared while there are enough), kept with it.
        if (place != null && !human.getPersistentData().m_128441_(JOB)) {
            human.getPersistentData().m_128405_(JOB, com.hex.kagune.control.village.HumanLife.villageJob(place.x(), place.z(),
                Math.max(0, VillagePlace.slot(human))).ordinal() + 1);
        }
        // 2.0 (lag): a calm villager looks around every LOOKS ticks (staggered by its id), a frightened or hurt one every tick.
        boolean alert = now < mind.shakenUntil || now < mind.hurtUntil || (now + human.m_19879_()) % LOOKS == 0L;
        Danger danger = alert ? danger(human, level, mind, now) : null;
        if (danger != null) {
            if (mind.trade != null) {
                mind.trade.end(level, now); // 1.3.72: a fright ends a trade
            }
            mind.shakenUntil = now + HumanRules.SHAKEN;
            human.scared(true);
            gestures(human, mind, level, now, true); // 1.3.77: a fright ends any move
            mind.kiss = null; // 2.3.4
            letGo(human, mind, level, now, null); // 1.3.78: a fright lets go of any hand
            scream(human, level, mind, danger, now);
            mind.ranFrom = danger.body().m_19879_();
            if (now >= mind.nextPlan || human.m_21573_().m_26571_()) {
                mind.nextPlan = now + 10;
                // 1.3.70 (the owner: "Humans will run to a nearby ccg agent ... if any are available"): behind the nearest investigator,
                // if one is near; else away.
                com.hex.kagune.control.npc.CcgAgent guard = guard(human, level, danger.body());
                if (guard != null) {
                    double[] spot = com.hex.kagune.control.npc.FinalSight.behind(guard.m_20185_(), guard.m_20189_(), danger.body().m_20185_(),
                        danger.body().m_20189_(), 1.6);
                    double dx = spot[0] - human.m_20185_();
                    double dz = spot[1] - human.m_20189_();
                    if (dx * dx + dz * dz > 1.2 * 1.2) {
                        human.m_21573_().m_26519_(spot[0], guard.m_20186_(), spot[1], HumanRules.pace(danger.sight()));
                    } else {
                        human.m_21573_().m_26573_();
                    }
                } else {
                    flee(human, danger.body(), HumanRules.pace(danger.sight()), danger.sight().terror() ? 14.0 : 10.0, place);
                }
            }
            return;
        }
        mind.ranFrom = 0;
        human.scared(now < mind.shakenUntil);
        // 1.3.78: on a date, hand in hand: nothing else.
        if (mind.holding != null) {
            holdHands(human, mind, place, level, now);
            if (mind.holding != null) {
                return;
            }
        }
        gestures(human, mind, level, now, false);
        // 2.3.4: kissing (leaning in, blushing, pulling them in): nothing else till it's over.
        if (mind.kiss != null) {
            kissing(human, mind, level, now);
            return;
        }
        // 1.3.77: walking up to its partner for a kiss or a hug: close enough (or after a while), it faces them and does it.
        if (mind.pending != 0) {
            net.minecraft.world.entity.Entity to = mind.with == null ? null : level.m_8791_(mind.with);
            if (!(to instanceof ServerPlayer partner) || !partner.m_6084_() || partner.m_20280_(human) > 12.0 * 12.0) {
                mind.pending = 0;
            } else if (partner.m_20280_(human) <= HumanTalk.CLOSE * HumanTalk.CLOSE || now >= mind.pendingUntil
                || place != null && !place.inside(partner.m_20185_(), partner.m_20189_())) { // 2.0: never out of its village for it
                human.m_21573_().m_26573_();
                face(human, partner);
                human.m_21563_().m_24960_(partner, 30.0F, 30.0F);
                int what = mind.pending;
                mind.pending = 0;
                if (com.hex.kagune.control.village.HumanGestures.gesture(what) == com.hex.kagune.control.npc.EtoGesture.KISS) {
                    // 2.3.4: a kiss as a companion's: leaning in, waiting for them (a tap of Shift, or held: blushing, then pulled in).
                    startKiss(human, mind, partner, now, mind.pendingLine);
                    return;
                }
                gesture(human, what, mind.pendingThen, now);
                if (com.hex.kagune.control.village.HumanGestures.gesture(what) == com.hex.kagune.control.npc.EtoGesture.HUG) {
                    hearts(human, level, 5);
                }
                say(human, level, mind.pendingLine);
            } else {
                if (now % 10L == 0L || human.m_21573_().m_26571_()) {
                    human.m_21573_().m_26519_(partner.m_20185_(), partner.m_20186_(), partner.m_20189_(), HumanRules.WALK);
                }
                return;
            }
        }
        // 1.3.77: a move it stands still for (a kiss, a hug, a bow): it stays where it is, facing whoever it is with.
        if (mind.gesture != 0 && mind.trade == null && com.hex.kagune.control.village.HumanGestures.still(mind.gesture)) {
            human.m_21573_().m_26573_();
            net.minecraft.world.entity.Entity with = mind.with == null ? null : level.m_8791_(mind.with);
            if (with != null && with.m_20280_(human) < 8.0 * 8.0) {
                face(human, with);
            }
            if (HumanTalk.talker(human, level) == null) {
                return;
            }
        }
        // 1.3.73: a player talking to it: it stands still, facing them.
        ServerPlayer talker = HumanTalk.talker(human, level);
        if (talker != null) {
            mind.comeTo = null; // 2.2.6: talked to first: whatever it was coming over for can wait
            human.m_21573_().m_26573_();
            human.m_21563_().m_24960_(talker, 30.0F, 30.0F);
            face(human, talker);
            return;
        }
        // 2.2.6: coming over by itself to a player it knows, with something to say (by its temper and how close they are).
        if (HumanApproachServer.tick(human, mind, place, level, now)) {
            return;
        }
        if (place != null) {
            village(human, mind, place, level, now);
            return;
        }
        if (now >= mind.nextWander && now >= mind.shakenUntil) {
            mind.nextWander = now + 120 + human.m_217043_().m_188503_(160);
            double angle = human.m_217043_().m_188500_() * Math.PI * 2.0;
            double length = 3.0 + human.m_217043_().m_188500_() * 6.0;
            // 4.1.38: one a vanilla villager brought keeps about its village (VillageHumans.HOME): strayed too far, it heads back.
            CompoundTag saved = human.getPersistentData();
            if (saved.m_128441_(VillageHumans.HOME)) {
                double dx = saved.m_128451_(VillageHumans.HOME + "X") + 0.5 - human.m_20185_();
                double dz = saved.m_128451_(VillageHumans.HOME + "Z") + 0.5 - human.m_20189_();
                if (dx * dx + dz * dz > VillageHumans.HOME_RANGE * VillageHumans.HOME_RANGE) {
                    angle = Math.atan2(dz, dx) + (human.m_217043_().m_188500_() - 0.5) * 0.6;
                    length = Math.min(12.0, Math.sqrt(dx * dx + dz * dz));
                }
            }
            human.m_21573_().m_26519_(human.m_20185_() + Math.cos(angle) * length, human.m_20186_(), human.m_20189_() + Math.sin(angle) * length,
                HumanRules.WALK);
        }
    }

    /**
     * The nearest thing it is running from, or null. 1.3.65: a ghoul player as {@link HumanRules#notice} says (War and the Dragon from
     * {@link HumanRules#TERROR_NOTICE} off, another final kakuja from {@link HumanRules#DREAD_NOTICE}: beyond {@link HumanRules#NOTICE} only
     * in its sight), and a terror kept running from out of sight a while ({@link HumanRules#TERROR_MEMORY}, out to {@link HumanRules#TERROR_SAFE}).
     */
    static Danger danger(HumanNpc human, ServerLevel level, Mind mind, long now) {
        Danger best = null;
        double bestDistance = Double.MAX_VALUE;
        if (mind.hurtBy != null && now < mind.hurtUntil) {
            net.minecraft.world.entity.Entity by = level.m_8791_(mind.hurtBy);
            if (by instanceof LivingEntity living && living.m_6084_() && living.m_20280_(human) < HumanRules.SAFE * HumanRules.SAFE) {
                best = new Danger(living, living instanceof ServerPlayer hurter ? Sightings.seen(hurter) : FinalSight.NONE,
                    living instanceof GhoulNpc || living instanceof ServerPlayer hurter2 && KaguneProfile.isUnlocked(hurter2));
                bestDistance = Math.sqrt(living.m_20280_(human));
            }
        }
        for (ServerPlayer player : level.m_6907_()) {
            if (player.m_5833_() || !player.m_6084_() || !KaguneProfile.isUnlocked(player)
                || player.m_20280_(human) > HumanRules.TERROR_SAFE * HumanRules.TERROR_SAFE) {
                continue;
            }
            // 2.3.4 (the owner: "when you turn ghoul in front of them, even if you already admitted that you're a ghoul, they start running
            // away and shouting ghoul"): one it knows is a ghoul and took them as they are (told and accepted, or its partner) stays.
            if (knows(human, player)) {
                continue;
            }
            double distance = Math.sqrt(player.m_20280_(human));
            FinalSight sight = Sightings.seen(player);
            // 4.0.7: kakugan behind a full mask is not seen (Sightings.masked); a kagune or a kakuja still is.
            boolean shown = KaguneProfile.isActive(player) || sight.any() || GhoulServer.ghoul(player) && !Sightings.masked(player)
                || Sightings.last(player) != com.hex.kagune.control.npc.Display.NONE; // 1.3.68: their eyes on is enough
            boolean noticed = distance <= HumanRules.notice(sight, shown) && (distance <= HumanRules.NOTICE || human.m_142582_(player));
            if (noticed && sight.terror()) {
                mind.dreadOf = player.m_19879_();
                mind.dreadUntil = now + HumanRules.TERROR_MEMORY;
            }
            boolean remembered = sight.terror() && mind.dreadOf == player.m_19879_() && now < mind.dreadUntil;
            if ((noticed || remembered) && distance < bestDistance) {
                best = new Danger(player, sight, true);
                bestDistance = distance;
            }
        }
        // 1.3.68: a ghoul NPC only with its eyes on or its kagune out (by day a wild one hides both: just a person).
        for (GhoulNpc ghoul : level.m_6443_(GhoulNpc.class, human.m_20191_().m_82400_(HumanRules.NOTICE),
                living -> living.m_6084_() && living.mind != null && (living.mind.out || living.mind.eyes))) {
            double distance = Math.sqrt(ghoul.m_20280_(human));
            if (HumanRules.notices(distance, true) && distance < bestDistance) {
                best = new Danger(ghoul, FinalSight.NONE, true);
                bestDistance = distance;
            }
        }
        return best;
    }

    /** 2.3.4: true if {@code human} knows {@code player} is a ghoul and took them as they are (confessed and accepted, or together). */
    static boolean knows(HumanNpc human, ServerPlayer player) {
        com.hex.kagune.control.quest.QuestLog.Bond bond = QuestServer.bond(player, human.m_20148_().toString());
        return bond.has(com.hex.kagune.control.village.HumanPersona.CONFESSED) || bond.has(com.hex.kagune.control.village.HumanPersona.PARTNER);
    }

    /**
     * 1.3.65: a scream, as it starts to run from a ghoul (not again for a while), and from a terror every {@link HumanRules#SCREAMS} ticks
     * or so; in a bubble over its head, to every player near who sees it.
     */
    static void scream(HumanNpc human, ServerLevel level, Mind mind, Danger danger, long now) {
        if (!danger.ghoul()) {
            return;
        }
        boolean fresh = danger.body().m_19879_() != mind.ranFrom;
        long since = now - mind.screamAt;
        boolean due = danger.sight().terror() ? since >= HumanRules.SCREAMS + human.m_217043_().m_188503_(30) : fresh && since >= 200L;
        if (!due || since < 30L) {
            return;
        }
        mind.screamAt = now;
        String line = FearLines.scream(danger.sight(), human.m_217043_().m_188503_(100));
        for (ServerPlayer player : level.m_6907_()) {
            if (player.m_20280_(human) <= HumanRules.HEARD * HumanRules.HEARD && human.m_142582_(player)) {
                com.hex.kagune.control.network.ControlNetwork.sendTo(player, new com.hex.kagune.control.network.S2CNpcSay(human.m_19879_(), line));
            }
        }
    }

    /**
     * 1.3.70: the investigator it runs to from {@code danger}: the nearest alive within {@code AgentRules.DEFEND}, never one it would have to
     * run past the danger to reach; null if there is none.
     */
    static com.hex.kagune.control.npc.CcgAgent guard(HumanNpc human, ServerLevel level, LivingEntity danger) {
        com.hex.kagune.control.npc.CcgAgent best = null;
        double bestDistance = Double.MAX_VALUE;
        double range = com.hex.kagune.control.npc.AgentRules.DEFEND;
        for (com.hex.kagune.control.npc.CcgAgent agent : level.m_6443_(com.hex.kagune.control.npc.CcgAgent.class, human.m_20191_().m_82400_(range),
                a -> a.m_6084_() && a != danger)) {
            // Not one it would have to run past the danger to reach.
            boolean past = danger.m_20280_(agent) < danger.m_20280_(human) && agent.m_20280_(human) > danger.m_20280_(human);
            double distance = agent.m_20280_(human);
            if (!past && distance < bestDistance) {
                best = agent;
                bestDistance = distance;
            }
        }
        return best;
    }

    /** Off, straight away from {@code danger} if it can, else turned a little either way, then further round. */
    static void flee(HumanNpc human, LivingEntity danger, double pace, double length, VillagePlace.Place place) {
        for (double turn : new double[] {0.0, 0.6, -0.6, 1.2, -1.2, 1.8, -1.8}) {
            double[] to = HumanRules.fleeStep(human.m_20185_(), human.m_20189_(), danger.m_20185_(), danger.m_20189_(), turn, length);
            if (place != null) {
                to = place.clamp(to[0], to[1], 2.0); // 1.3.71: never past the village's perimeter, even running
            }
            if (human.m_21573_().m_26519_(to[0], human.m_20186_(), to[1], pace)) {
                return;
            }
        }
    }

    /**
     * 1.3.71: a Brick Village human's day ({@code VillageRules}): by night it goes home (where it was placed, inside its house) and stays;
     * by day, now and then, it walks to a spot on the streets near it; never past the perimeter.
     */
    static void village(HumanNpc human, Mind mind, VillagePlace.Place place, ServerLevel level, long now) {
        // 1.3.72: in a trade in the street: it runs it.
        if (mind.trade != null && mind.trade.tick(human, level, now)) {
            return;
        }
        mind.trade = null;
        boolean night = com.hex.kagune.control.npc.NpcRules.night(level.m_46468_());
        if (night && !mind.dusk && human.m_217043_().m_188503_(3) == 0) {
            say(human, level, com.hex.kagune.control.village.VillageTalk.dusk(human.m_217043_().m_188503_(100)));
            gesture(human, com.hex.kagune.control.village.HumanGestures.dusk(human.m_217043_().m_188503_(100)), 0, now); // 1.3.77
        }
        mind.dusk = night;
        // 2.4.7: now and then, by day, a favour asked of a player near (rare: HumanErrand).
        if (!night && (now + human.m_19879_()) % 200L == 0L) {
            HumanErrandServer.maybeAsk(human, level, now);
        }
        if (night) {
            double dx = place.homeX() + 0.5 - human.m_20185_();
            double dz = place.homeZ() + 0.5 - human.m_20189_();
            if (dx * dx + dz * dz > 1.5 * 1.5) {
                if (now % 20L == 0L || human.m_21573_().m_26571_()) {
                    human.m_21573_().m_26519_(place.homeX() + 0.5, place.homeY(), place.homeZ() + 0.5, com.hex.kagune.control.village.VillageRules.HOMEWARD);
                }
            } else {
                human.m_21573_().m_26573_();
            }
            return;
        }
        if (now % 20L == Math.floorMod(human.m_19879_(), 20) && now >= mind.shakenUntil) {
            meet(human, mind, place, level, now);
            if (mind.trade != null) {
                return;
            }
        }
        // 1.3.77: standing in the street, now and then something of its own (a stretch, a hum, a twirl of its hair: by its temper).
        if (mind.gesture == 0 && now >= mind.idleAt && now >= mind.shakenUntil && human.m_21573_().m_26571_()) {
            if (mind.idleAt != Long.MIN_VALUE / 2) {
                gesture(human, com.hex.kagune.control.village.HumanGestures.idle(temper(human),
                    human.m_217043_().m_188503_(1000)), 0, now);
                mind.nextWander = Math.max(mind.nextWander, mind.gestureUntil + 20L);
            }
            mind.idleAt = now + 500L + human.m_217043_().m_188503_(900);
        }
        if (now < mind.nextWander || now < mind.shakenUntil || !human.m_21573_().m_26571_() || mind.gesture != 0
            && com.hex.kagune.control.village.HumanGestures.still(mind.gesture)) {
            return;
        }
        mind.nextWander = now + com.hex.kagune.control.village.VillageRules.STROLL_EVERY
            + human.m_217043_().m_188503_(com.hex.kagune.control.village.VillageRules.STROLL_MORE);
        int[][] streets = place.streets();
        double reach = com.hex.kagune.control.village.VillageRules.STROLL_REACH;
        for (int tries = 0; tries < 12; tries++) {
            double[] to = place.world(streets[human.m_217043_().m_188503_(streets.length)]);
            double dx = to[0] - human.m_20185_();
            double dz = to[2] - human.m_20189_();
            if (dx * dx + dz * dz <= reach * reach && place.inside(to[0], to[2])) {
                human.m_21573_().m_26519_(to[0], to[1], to[2], com.hex.kagune.control.village.VillageRules.STROLL);
                return;
            }
        }
    }

    /**
     * 1.3.72: once a second, by day: a word to another of the village it passes (within 4 blocks, each at most once a minute), and now and
     * then (when it may trade again) a trade with one within 20 who is free.
     */
    static void meet(HumanNpc human, Mind mind, VillagePlace.Place place, ServerLevel level, long now) {
        for (HumanNpc other : level.m_6443_(HumanNpc.class, human.m_20191_().m_82400_(20.0), o -> o != human && o.m_6084_() && o.mind != null
                && o.mind.trade == null && now >= o.mind.shakenUntil && VillagePlace.of(o) != null)) {
            double d = other.m_20280_(human);
            if (d <= 4.0 * 4.0 && now - mind.greetAt >= 1200L && now - other.mind.greetAt >= 1200L && human.m_217043_().m_188503_(4) == 0) {
                mind.greetAt = now;
                other.mind.greetAt = now;
                face(human, other);
                face(other, human);
                say(human, level, com.hex.kagune.control.village.VillageTalk.greet(human.m_217043_().m_188503_(1000)));
                // 1.3.77: a wave or a little bow, and one back.
                gesture(human, com.hex.kagune.control.village.HumanGestures.greet(human.m_217043_().m_188503_(1000)), 0, now);
                gesture(other, com.hex.kagune.control.village.HumanGestures.greet(other.m_217043_().m_188503_(1000)), 0, now + 6L);
                return;
            }
            if (now >= mind.tradeReady && now >= other.mind.tradeReady && human.m_217043_().m_188503_(6) == 0) {
                VillageTrade.start(human, other, place, now);
                return;
            }
        }
    }

    /** 1.3.72: {@code human} turned square to {@code other}. */
    static void face(HumanNpc human, net.minecraft.world.entity.Entity other) {
        double dx = other.m_20185_() - human.m_20185_();
        double dz = other.m_20189_() - human.m_20189_();
        if (dx * dx + dz * dz < 1.0E-4) {
            return;
        }
        float yaw = (float)Math.toDegrees(Math.atan2(-dx, dz));
        human.m_146922_(yaw);
        human.f_20883_ = yaw;
        human.f_20885_ = yaw;
    }

    /** 1.3.72: a word in a bubble over its head, to every player near who sees it. */
    static void say(HumanNpc human, ServerLevel level, String line) {
        if (line == null || line.isEmpty()) {
            return;
        }
        for (ServerPlayer player : level.m_6907_()) {
            if (player.m_20280_(human) <= 24.0 * 24.0 && human.m_142582_(player)) {
                com.hex.kagune.control.network.ControlNetwork.sendTo(player, new com.hex.kagune.control.network.S2CNpcSay(human.m_19879_(), line));
            }
        }
    }

    /** Its meat, when it dies. */
    public static void drop(LivingEntity human, int looting) {
        Item meat = ControlItems.dishItem("human_meat");
        if (meat != null) {
            human.m_19983_(new ItemStack(meat, HumanRules.meat(human.m_217043_().m_188500_(), looting)));
        }
    }

    /** Hurt: it remembers who did it and runs from them. */
    @SubscribeEvent
    public static void hurt(LivingHurtEvent event) {
        if (!(event.getEntity() instanceof HumanNpc human) || !(human.m_9236_() instanceof ServerLevel level)
            || !(event.getSource().m_7639_() instanceof LivingEntity attacker) || attacker == human) {
            return;
        }
        if (human.mind == null) {
            human.mind = new Mind();
        }
        human.mind.hurtBy = attacker.m_20148_();
        human.mind.hurtUntil = level.m_46467_() + HumanRules.HURT_MEMORY;
        human.mind.nextPlan = 0L;
    }
}
