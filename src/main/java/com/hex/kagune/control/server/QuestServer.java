package com.hex.kagune.control.server;

import com.hex.kagune.control.ControlItems;
import com.hex.kagune.control.FoodClassifier;
import com.hex.kagune.control.KagunePath;
import com.hex.kagune.control.RinkakuForms;
import com.hex.kagune.control.network.ControlNetwork;
import com.hex.kagune.control.network.S2CQuests;
import com.hex.kagune.control.quest.Gate;
import com.hex.kagune.control.quest.Hunts;
import com.hex.kagune.control.quest.Objective;
import com.hex.kagune.control.quest.Quest;
import com.hex.kagune.control.quest.QuestBook;
import com.hex.kagune.control.quest.QuestEvent;
import com.hex.kagune.control.quest.QuestLog;
import com.hex.kagune.control.quest.Reward;
import com.hex.kagune.control.quest.Signal;
import com.hex.kagune.control.quest.Tags;
import com.hex.kagune.data.KaguneProfile;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.Set;
import java.util.UUID;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.monster.Enemy;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.event.entity.living.LivingDamageEvent;
import net.minecraftforge.event.entity.living.LivingDeathEvent;
import net.minecraftforge.event.entity.living.LivingEntityUseItemEvent;
import net.minecraftforge.event.entity.player.PlayerEvent;
import net.minecraftforge.eventbus.api.EventPriority;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.registries.ForgeRegistries;

/**
 * 7.0.1: the quests on the server ({@code control/quest}). Each ghoul's {@link QuestLog} lives in its profile root as one
 * string (death carries it, {@code /kagune reset} clears it), read back whenever the root no longer holds what was last written.
 * What happens in the world becomes {@link QuestEvent}s here (kills, deaths, meals, damage, the brink, time in a form, rampages)
 * or is told by the moves themselves ({@link #signal}); a quest met is paid at once (mastery, points, Ghoul Meat, its gates) and
 * the ghoul told. At dusk a hunt may be drawn ({@link Hunts}), gone at dawn. The owner's client gets the log for the journal.
 *
 * <p>Gates ({@link #open}): the path code asks before a gated transformation, move or mutation; the first time a save is seen
 * under 7.0.1 everything its mastery and mutations had already reached is kept ({@link #had}).
 */
@Mod.EventBusSubscriber(modid = "hexkagune")
public final class QuestServer {
    private static final String KEY = "HexQuests";
    /** A blow that leaves a ghoul at this share of its health or less puts it on the brink; living this long after is surviving it. */
    private static final float BRINK = 0.2F;
    private static final long BRINK_TICKS = 100L;
    /** A kill this far off (blocks) counts as a shot even when no projectile made it. */
    private static final double SHOT_RANGE = 6.0;
    /** Bosses by id; anything else this tough counts too. */
    private static final Set<String> BOSSES = Set.of("minecraft:wither", "minecraft:ender_dragon", "minecraft:warden", "minecraft:elder_guardian");
    private static final float BOSS_HEALTH = 150.0F;
    /** Dusk and dawn, in the day's ticks. */
    private static final long DUSK = 12000L;
    private static final long DUSK_END = 13000L;
    private static final long DAWN = 23500L;
    private static final long NOTICE_TICKS = 40L;

    /** Per player: the log, and the string it was read from or last written as (anything else in the root means reload). */
    private static final Map<UUID, Cached> LOGS = new HashMap<>();

    private static final class Cached {
        QuestLog log;
        String saved;
        /** The quests open at the last look (so a newly opened one is announced once); null before the first look. */
        Set<String> open;
        long brinkAt = Long.MIN_VALUE;
        long lastKillAt = Long.MIN_VALUE;
        int streak;
        long nextNoticeAt = Long.MIN_VALUE;
        boolean dirty = true;
    }

    private QuestServer() {
    }

    // ------------------------------------------------------------------ the log

    static Cached cached(ServerPlayer player) {
        Cached cached = LOGS.computeIfAbsent(player.m_20148_(), id -> new Cached());
        String stored = KaguneProfile.root(player).m_128461_(KEY);
        if (cached.log == null || !stored.equals(cached.saved)) {
            cached.log = QuestLog.load(stored);
            cached.saved = stored;
            cached.open = null;
            cached.dirty = true;
        }
        if (cached.log.migrated() < QuestLog.VERSION && KaguneProfile.isUnlocked(player)) {
            cached.log.migrate(gate -> had(player, gate));
            write(player, cached);
        }
        return cached;
    }

    public static QuestLog log(ServerPlayer player) {
        return cached(player).log;
    }

    private static void write(ServerPlayer player, Cached cached) {
        cached.saved = cached.log.save();
        KaguneProfile.root(player).m_128359_(KEY, cached.saved);
        cached.dirty = true;
    }

    /**
     * What the old progression had already given this ghoul (kept on the first look under 7.0.1): a transformation or move whose
     * mastery it had reached on its path, a mutation it had taken.
     */
    static boolean had(ServerPlayer player, Gate gate) {
        if (gate == Gate.RINKAKU_DRAGON) {
            return false; // 1.0.4: the Dragon is earned by its quest, whatever the mastery
        }
        KagunePath path = PathServer.path(player);
        if (!gate.on(path) || !path.chosen() && gate.path() != null) {
            return false;
        }
        if (!gate.mutation()) {
            return KaguneProfile.mastery(player) >= gate.mastery();
        }
        return switch (gate) {
            case UKAKU_NOT_THAT_EASILY -> UkakuServer.hasMutation(player, com.hex.kagune.control.ukaku.UkakuMutation.NOT_THAT_EASILY);
            case UKAKU_BURSTING_SHARDS -> UkakuServer.hasMutation(player, com.hex.kagune.control.ukaku.UkakuMutation.BURSTING_SHARDS);
            case GRAFT_APPETITE -> GraftServer.hasMutation(player, com.hex.kagune.control.graft.GraftMutation.RIZES_APPETITE);
            case GRAFT_NOT_THAT_EASILY -> GraftServer.hasMutation(player, com.hex.kagune.control.graft.GraftMutation.NOT_THAT_EASILY);
            default -> false;
        };
    }

    /** True if {@code gate} is open for this ghoul (a gate of another path never stands in the way). */
    public static boolean open(ServerPlayer player, Gate gate) {
        return gate == null || !gate.on(PathServer.path(player)) || log(player).open(gate);
    }

    /** {@link #open}, or false after saying on the action bar which quests open it. */
    public static boolean pass(ServerPlayer player, Gate gate) {
        if (open(player, gate)) {
            return true;
        }
        notice(player, Component.m_237110_("message.hexkagune.quest.gated", gate.displayName(), QuestBook.openersText(gate)));
        return false;
    }

    /** Mutation points earned from quests. */
    public static int points(ServerPlayer player) {
        return log(player).points();
    }

    /** 7.0.2: how the player stands with the ghoul NPC {@code npc} (its UUID). */
    public static QuestLog.Bond bond(ServerPlayer player, String npc) {
        return log(player).bond(npc);
    }

    /** 7.0.2: how the player stands with {@code npc} now (saved, and sent to their journal). */
    public static void bond(ServerPlayer player, String npc, QuestLog.Bond bond) {
        Cached cached = cached(player);
        cached.log.bond(npc, bond);
        write(player, cached);
    }

    /** 7.0.2: a favour taken on for the ghoul NPC {@code giver} who is {@code who} (saved, sent); false if it could not be. */
    public static boolean startFavour(ServerPlayer player, com.hex.kagune.control.quest.Favours.Spec spec, String giver,
                                      com.hex.kagune.control.npc.NpcIdentity who) {
        Cached cached = cached(player);
        if (!cached.log.startFavour(spec, giver, who)) {
            return false;
        }
        write(player, cached);
        QuestLog.Favour favour = cached.log.favourFrom(giver);
        player.m_5661_(Component.m_237113_("Favour taken on: " + favour.quest().objectives().get(0).text() + ". It is in your journal, the ghoul's way."),
            false);
        return true;
    }

    // ------------------------------------------------------------------ events

    /** A move of the shared kit (or a path's) tells what it did: {@code amount} of {@code signal}, tagged with how the ghoul is. */
    public static void signal(ServerPlayer player, Signal signal, int amount, String... tags) {
        if (amount <= 0 || !KaguneProfile.isUnlocked(player)) {
            return;
        }
        Set<String> all = context(player);
        all.addAll(List.of(tags));
        feed(player, new QuestEvent(signal, amount, all));
    }

    static void feed(ServerPlayer player, QuestEvent event) {
        Cached cached = cached(player);
        KagunePath path = PathServer.path(player);
        int mastery = KaguneProfile.mastery(player);
        List<QuestLog.Step> steps = cached.log.apply(event, path, mastery);
        if (steps.isEmpty()) {
            return;
        }
        Set<Quest> touched = new java.util.LinkedHashSet<>();
        for (QuestLog.Step step : steps) {
            touched.add(step.quest());
            announceStep(player, cached, step);
        }
        for (Quest quest : touched) {
            if (cached.log.met(quest)) {
                complete(player, cached, quest);
            }
        }
        write(player, cached);
    }

    /** The ghoul as it is now, as tags ({@link Tags}). */
    static Set<String> context(ServerPlayer player) {
        Set<String> tags = new HashSet<>();
        KagunePath path = PathServer.path(player);
        tags.add(Tags.path(path.id()));
        boolean out = KaguneProfile.isActive(player);
        if (PaleServer.eyes(player)) {
            tags.add(Tags.EYES);
        }
        if (out) {
            tags.add(Tags.KAGUNE);
            String form = switch (path) {
                case UKAKU -> UkakuServer.form(player).id();
                case GRAFT -> GraftServer.stage(player).id();
                default -> KaguneProfile.form(player).id();
            };
            tags.add(Tags.form(form));
            boolean kakuja = switch (path) {
                case UKAKU -> UkakuServer.form(player).kakuja();
                case GRAFT -> GraftServer.kakuja(player);
                default -> RinkakuForms.kakuja(KaguneProfile.form(player));
            };
            if (kakuja) {
                tags.add(Tags.KAKUJA);
            }
        }
        if (night(player)) {
            tags.add(Tags.NIGHT);
        }
        if (GhoulNpcBrain.allyNear(player)) {
            tags.add(Tags.ALLY);
        }
        if (!player.m_20096_() && !player.m_20069_()) {
            tags.add(Tags.AIRBORNE);
        }
        if (player.m_21223_() <= player.m_21233_() / 3.0F) {
            tags.add(Tags.LOW);
        }
        return tags;
    }

    static boolean night(ServerPlayer player) {
        long time = Math.floorMod(player.m_9236_().m_46468_(), 24000L);
        return time >= 13000L && time < 23000L;
    }

    /** What {@code entity} is, as tags: its id, hostile, a ghoul (a ghoul player, a ghoul NPC), a player, a boss. */
    static Set<String> victim(LivingEntity entity) {
        Set<String> tags = new HashSet<>();
        ResourceLocation id = ForgeRegistries.ENTITY_TYPES.getKey(entity.m_6095_());
        String name = id == null ? "unknown" : id.toString();
        tags.add(Tags.mob(name));
        if (entity instanceof Enemy) {
            tags.add(Tags.HOSTILE);
        }
        if (entity instanceof ServerPlayer other) {
            tags.add(Tags.PLAYER);
            if (KaguneProfile.isUnlocked(other)) {
                tags.add(Tags.GHOUL);
            }
        }
        if (GhoulKinds.ghoul(entity)) {
            tags.add(Tags.GHOUL);
            tags.add(Tags.NPC);
            String temper = GhoulKinds.temper(entity);
            if (temper != null) {
                tags.add(Tags.temper(temper));
            }
            if (GhoulKinds.hostile(entity)) {
                tags.add(Tags.HOSTILE);
            }
        }
        if (BOSSES.contains(name) || entity.m_21233_() >= BOSS_HEALTH) {
            tags.add(Tags.BOSS);
        }
        return tags;
    }

    @SubscribeEvent(priority = EventPriority.LOWEST)
    public static void died(LivingDeathEvent event) {
        // (A death cancelled earlier, as Not That Easily's, never reaches here: it was no death.)
        LivingEntity dead = event.getEntity();
        Entity source = event.getSource().m_7639_();
        if (source instanceof ServerPlayer killer && killer != dead && KaguneProfile.isUnlocked(killer)) {
            Set<String> tags = context(killer);
            tags.addAll(victim(dead));
            Entity direct = event.getSource().m_7640_();
            if (direct != null && direct != killer || dead.m_20280_(killer) > SHOT_RANGE * SHOT_RANGE) {
                tags.add(Tags.RANGED);
            }
            feed(killer, new QuestEvent(Signal.KILL, 1, tags));
            Cached cached = cached(killer);
            long now = killer.m_9236_().m_46467_();
            cached.streak = now - cached.lastKillAt <= QuestLog.RAMPAGE_TICKS ? cached.streak + 1 : 1;
            cached.lastKillAt = now;
            if (cached.streak >= 2) {
                feed(killer, new QuestEvent(Signal.RAMPAGE, cached.streak, context(killer)));
            }
        }
        if (dead instanceof ServerPlayer player && KaguneProfile.isUnlocked(player)) {
            Set<String> tags = context(player);
            if (source instanceof LivingEntity killer) {
                tags.addAll(victim(killer));
            }
            Cached cached = cached(player);
            cached.brinkAt = Long.MIN_VALUE;
            cached.streak = 0;
            cached.log.died();
            write(player, cached);
            feed(player, new QuestEvent(Signal.DIE, 1, tags));
        }
    }

    @SubscribeEvent(priority = EventPriority.LOWEST)
    public static void ate(LivingEntityUseItemEvent.Finish event) {
        if (!(event.getEntity() instanceof ServerPlayer player) || !KaguneProfile.isUnlocked(player)) {
            return;
        }
        ItemStack eaten = event.getItem();
        if (eaten.m_41619_() || !eaten.m_41614_()) {
            return;
        }
        Set<String> tags = context(player);
        ResourceLocation id = ForgeRegistries.ITEMS.getKey(eaten.m_41720_());
        if (id != null) {
            tags.add(Tags.item(id.toString()));
        }
        if (GhoulDiet.classify(eaten, player) == FoodClassifier.Diet.GHOUL_FOOD) {
            tags.add(Tags.FLESH);
        }
        if (ControlItems.ghoulMeat(eaten)) {
            tags.add(Tags.GHOUL_MEAT);
        }
        feed(player, new QuestEvent(Signal.EAT, 1, tags));
    }

    /** Damage as it lands: dealt by a ghoul, taken by one (and the brink). */
    @SubscribeEvent(priority = EventPriority.LOWEST)
    public static void damaged(LivingDamageEvent event) {
        if (event.getAmount() <= 0.0F) {
            return;
        }
        LivingEntity hurt = event.getEntity();
        Entity source = event.getSource().m_7639_();
        if (source instanceof ServerPlayer attacker && attacker != hurt && KaguneProfile.isUnlocked(attacker)) {
            int amount = (int)Math.floor(event.getAmount());
            if (amount > 0) {
                feed(attacker, new QuestEvent(Signal.DEAL, amount, context(attacker)));
            }
        }
        if (hurt instanceof ServerPlayer player && KaguneProfile.isUnlocked(player)) {
            float left = player.m_21223_() - event.getAmount();
            Set<String> tags = context(player);
            Entity direct = event.getSource().m_7640_();
            if (direct != null && direct != source) {
                tags.add(Tags.RANGED);
            }
            int amount = (int)Math.floor(event.getAmount());
            if (left > 0.0F && amount > 0) {
                feed(player, new QuestEvent(Signal.TAKE, amount, tags));
            }
            if (left > 0.0F && left <= player.m_21233_() * BRINK) {
                Cached cached = cached(player);
                if (cached.brinkAt == Long.MIN_VALUE) {
                    cached.brinkAt = player.m_9236_().m_46467_();
                }
            }
        }
    }

    @SubscribeEvent
    public static void playerTick(TickEvent.PlayerTickEvent event) {
        if (event.phase != TickEvent.Phase.END || !(event.player instanceof ServerPlayer player) || !KaguneProfile.isUnlocked(player)) {
            return;
        }
        long now = player.m_9236_().m_46467_();
        if (now % 20L != Math.floorMod(player.m_19879_(), 20)) {
            return; // once a second, players spread over the second
        }
        Cached cached = cached(player);
        if (cached.brinkAt != Long.MIN_VALUE && player.m_6084_() && now - cached.brinkAt >= BRINK_TICKS) {
            cached.brinkAt = Long.MIN_VALUE;
            feed(player, new QuestEvent(Signal.BRINK, 1, context(player)));
        }
        if (PaleServer.eyes(player) || KaguneProfile.isActive(player)) {
            feed(player, new QuestEvent(Signal.TIME, 1, context(player)));
        }
        hunts(player, cached, now);
        opened(player, cached);
        enforceRinkaku(player);
        if (cached.dirty) {
            cached.dirty = false;
            sync(player);
        }
    }

    /**
     * Rinkaku's base forms are picked in the original's own menu: one whose quest isn't done (a save reset, a menu out of step) is
     * taken back to the strongest form that is open, and he is told why.
     */
    private static void enforceRinkaku(ServerPlayer player) {
        if (PathServer.path(player) != KagunePath.RINKAKU) {
            return;
        }
        com.hex.kagune.gameplay.KaguneForm form = KaguneProfile.form(player);
        Gate gate = com.hex.kagune.control.quest.Gates.rinkaku(form);
        if (gate == null || open(player, gate)) {
            return;
        }
        int mastery = KaguneProfile.mastery(player);
        com.hex.kagune.gameplay.KaguneForm best = com.hex.kagune.gameplay.KaguneForm.RINKAKU_SEED;
        for (com.hex.kagune.gameplay.KaguneForm option : RinkakuForms.forms()) {
            if (!RinkakuForms.kakuja(option) && option.isUnlocked(mastery) && option.requiredMastery() >= best.requiredMastery()
                && com.hex.kagune.control.quest.Gates.passes(com.hex.kagune.control.quest.Gates.rinkaku(option), g -> open(player, g))) {
                best = option;
            }
        }
        if (best != form && KaguneProfile.setForm(player, best)) {
            com.hex.kagune.gameplay.KaguneServerController.onFormChanged(player);
            com.hex.kagune.network.KaguneNetwork.broadcast(player);
        }
        notice(player, Component.m_237110_("message.hexkagune.quest.gated", gate.displayName(), QuestBook.openersText(gate)), true);
    }

    // ------------------------------------------------------------------ hunts

    private static void hunts(ServerPlayer player, Cached cached, long now) {
        for (Quest gone : cached.log.expire(now)) {
            player.m_5661_(Component.m_237110_("message.hexkagune.quest.hunt_over", gone.title()), false);
            write(player, cached);
        }
        long day = Math.floorMod(player.m_9236_().m_46468_(), 24000L);
        if (day < DUSK || day >= DUSK_END || now < cached.log.nextHuntAt() || cached.log.hunts().size() >= Hunts.MAX_ON
            || !PathServer.path(player).chosen()) {
            return;
        }
        Random random = new Random(now * 31L + player.m_20148_().getLeastSignificantBits());
        // Whether one comes or not, the next chance is a night or two away: hunts never come on a timetable.
        cached.log.nextHuntAt(now + 24000L - (day - DUSK) + (random.nextBoolean() ? 0L : 24000L));
        if (random.nextDouble() < Hunts.OFFER_CHANCE) {
            offerHunt(player, cached, random, now);
        }
        write(player, cached);
    }

    /** A hunt for tonight: on until dawn. */
    static void offerHunt(ServerPlayer player, Cached cached, Random random, long now) {
        long day = Math.floorMod(player.m_9236_().m_46468_(), 24000L);
        long untilDawn = day < DAWN ? DAWN - day : 24000L - day + DAWN;
        Hunts.Spec spec = Hunts.draw(random, GhoulKinds.roaming());
        cached.log.startHunt(spec, now + untilDawn);
        Quest quest = Hunts.quest(spec);
        player.m_5661_(Component.m_237110_("message.hexkagune.quest.hunt", quest.title(), quest.objectives().get(0).text(),
            quest.objectives().get(0).count(), Hunts.payText(spec)), false);
        UkakuFire.sound(player.m_284548_(), player.m_20182_(), "entity.wolf.howl", 0.5F, 0.6F);
    }

    // ------------------------------------------------------------------ telling and paying

    /** Newly open quests (mastery reached, a quest before done), said once; nothing said the first time a ghoul is looked at. */
    private static void opened(ServerPlayer player, Cached cached) {
        KagunePath path = PathServer.path(player);
        int mastery = KaguneProfile.mastery(player);
        Set<String> open = new HashSet<>();
        for (Quest quest : QuestBook.all()) {
            if (cached.log.state(quest, path, mastery) == QuestLog.State.OPEN) {
                open.add(quest.id());
            }
        }
        if (cached.open != null) {
            for (String id : open) {
                if (!cached.open.contains(id)) {
                    Quest quest = QuestBook.get(id);
                    player.m_5661_(Component.m_237110_("message.hexkagune.quest.new", quest.title()), false);
                    cached.dirty = true;
                }
            }
        }
        cached.open = open;
    }

    /** A step worth a word: each one of a small count, every quarter of a large one. */
    private static void announceStep(ServerPlayer player, Cached cached, QuestLog.Step step) {
        Objective objective = step.quest().objectives().get(step.objective());
        int count = objective.count();
        boolean worth = count <= 12 || step.after() >= count || step.after() * 4 / count != step.before() * 4 / count;
        cached.dirty = true;
        if (!worth || step.finished() && cached.log.met(step.quest())) {
            return;
        }
        notice(player, Component.m_237110_("message.hexkagune.quest.progress", step.quest().title(), objective.text(), step.after(), count), true);
    }

    private static void complete(ServerPlayer player, Cached cached, Quest quest) {
        List<Quest> closed = cached.log.complete(quest);
        Reward reward = quest.reward();
        int mastery = reward.mastery();
        int points = reward.points();
        if (points > 0 && PathServer.path(player) == KagunePath.RINKAKU) {
            mastery += points * Reward.RINKAKU_POINT_MASTERY; // his mutations come with mastery tiers, not points
        }
        if (mastery > 0) {
            KaguneProfile.addMastery(player, mastery);
            com.hex.kagune.network.KaguneNetwork.broadcast(player);
        }
        if (reward.ghoulMeat() > 0) {
            ItemStack meat = new ItemStack(ControlItems.GHOUL_MEAT.get(), reward.ghoulMeat());
            if (!player.m_36356_(meat)) {
                player.m_36176_(meat, false);
            }
        }
        StringBuilder pays = new StringBuilder();
        if (mastery > 0) {
            pays.append(mastery).append(" mastery");
        }
        if (points > 0 && PathServer.path(player) != KagunePath.RINKAKU) {
            pays.append(pays.length() > 0 ? ", " : "").append(points).append(" mutation point").append(points == 1 ? "" : "s");
        }
        if (reward.ghoulMeat() > 0) {
            pays.append(pays.length() > 0 ? ", " : "").append(reward.ghoulMeat()).append(" Ghoul Meat");
        }
        player.m_5661_(Component.m_237110_("message.hexkagune.quest.done", quest.title(), pays.length() == 0 ? "-" : pays.toString()), false);
        for (Gate gate : reward.gates()) {
            player.m_5661_(Component.m_237110_("message.hexkagune.quest.unlocked", gate.displayName()), false);
        }
        for (Quest rival : closed) {
            player.m_5661_(Component.m_237110_("message.hexkagune.quest.closed", rival.title()), false);
        }
        UkakuFire.sound(player.m_284548_(), player.m_20182_(), "ui.toast.challenge_complete", 0.6F, 1.0F);
        cached.dirty = true;
    }

    private static void notice(ServerPlayer player, Component message) {
        notice(player, message, false);
    }

    private static void notice(ServerPlayer player, Component message, boolean always) {
        Cached cached = LOGS.computeIfAbsent(player.m_20148_(), id -> new Cached());
        long now = player.m_9236_().m_46467_();
        if (always || now >= cached.nextNoticeAt || now < cached.nextNoticeAt - NOTICE_TICKS) {
            cached.nextNoticeAt = now + NOTICE_TICKS;
            player.m_5661_(message, true);
        }
    }

    static void sync(ServerPlayer player) {
        ControlNetwork.sendTo(player, new S2CQuests(log(player).save()));
    }

    // ------------------------------------------------------------------ commands

    /** Every quest forgotten: the new progression from scratch (nothing grandfathered back). */
    static void reset(ServerPlayer player) {
        Cached cached = cached(player);
        cached.log.reset();
        cached.open = null;
        write(player, cached);
        sync(player);
    }

    /** {@code quest} paid now, as if met (a command). */
    public static boolean force(ServerPlayer player, String id) {
        Quest quest = QuestBook.get(id);
        Cached cached = cached(player);
        if (quest == null) {
            for (QuestLog.Hunt hunt : cached.log.hunts()) {
                if (hunt.quest().id().equals(id)) {
                    quest = hunt.quest();
                }
            }
        }
        if (quest == null) {
            return false;
        }
        complete(player, cached, quest);
        write(player, cached);
        sync(player);
        return true;
    }

    /**
     * 1.0.11: finishes every quest {@code player} has on right now (the journal's open ones, tonight's hunts, the favours taken on), each
     * paid as if done; one that an earlier one closes meanwhile (its rival) stays closed. The titles finished, in order.
     */
    public static List<String> completeOpen(ServerPlayer player) {
        Cached cached = cached(player);
        KagunePath path = PathServer.path(player);
        java.util.ArrayList<Quest> open = new java.util.ArrayList<>();
        for (Quest quest : cached.log.quests()) {
            if (cached.log.state(quest, path, KaguneProfile.mastery(player)) == QuestLog.State.OPEN) {
                open.add(quest);
            }
        }
        for (QuestLog.Hunt hunt : cached.log.hunts()) {
            open.add(hunt.quest());
        }
        for (QuestLog.Favour favour : cached.log.favours()) {
            open.add(favour.quest());
        }
        java.util.ArrayList<String> done = new java.util.ArrayList<>();
        for (Quest quest : open) {
            QuestLog.State state = cached.log.state(quest, path, KaguneProfile.mastery(player));
            if (state == QuestLog.State.DONE || state == QuestLog.State.CLOSED) {
                continue;
            }
            complete(player, cached, quest);
            done.add(quest.title());
        }
        write(player, cached);
        sync(player);
        return done;
    }

    static void grant(ServerPlayer player, Gate gate) {
        Cached cached = cached(player);
        cached.log.grant(gate);
        write(player, cached);
        sync(player);
    }

    /** A hunt now (a command), whatever the hour. */
    static void hunt(ServerPlayer player) {
        Cached cached = cached(player);
        long now = player.m_9236_().m_46467_();
        offerHunt(player, cached, new Random(now ^ player.m_20148_().getMostSignificantBits()), now);
        write(player, cached);
        sync(player);
    }

    @SubscribeEvent
    public static void loggedIn(PlayerEvent.PlayerLoggedInEvent event) {
        if (event.getEntity() instanceof ServerPlayer player) {
            Cached cached = cached(player);
            cached.open = null;
            sync(player);
        }
    }

    @SubscribeEvent
    public static void respawned(PlayerEvent.PlayerRespawnEvent event) {
        if (event.getEntity() instanceof ServerPlayer player) {
            sync(player);
        }
    }

    @SubscribeEvent
    public static void loggedOut(PlayerEvent.PlayerLoggedOutEvent event) {
        LOGS.remove(event.getEntity().m_20148_());
    }
}
