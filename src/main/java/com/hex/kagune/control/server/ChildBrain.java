package com.hex.kagune.control.server;

import com.hex.kagune.control.family.ChildLines;
import com.hex.kagune.control.family.ChildRules;
import com.hex.kagune.control.family.ChildStage;
import com.hex.kagune.control.network.ControlNetwork;
import com.hex.kagune.control.network.S2CChild;
import com.hex.kagune.control.npc.ChildNpc;
import com.hex.kagune.control.npc.UniqueNpc;
import java.util.UUID;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.monster.Enemy;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.event.entity.living.LivingDeathEvent;
import net.minecraftforge.event.entity.player.PlayerEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

/**
 * 1.3.10: a child's mind (server): one body of it in the world ({@code UniqueNpcs}); growing through its stages (its box and health with
 * them, its parents told); carried in a parent's arms; a baby sitting where it is put; following who told it to, or waiting at home; a
 * child or teen that was taught fighting what threatens its family (its kakugan showing, if it has one), an untaught one running to a
 * parent; its lines now and then, and a greeting when a parent comes back. Every client near is told what it needs ({@link S2CChild}).
 */
@Mod.EventBusSubscriber(modid = "hexkagune")
public final class ChildBrain {
    public static final class Mind {
        ChildStage stage;
        UUID carrier;
        int target;
        boolean turned;
        long lastHit = Long.MIN_VALUE;
        long nextLine;
        long parentAway = Long.MIN_VALUE;
        boolean dirty = true;
        long company;
    }

    private ChildBrain() {
    }

    public static void tick(ChildNpc npc) {
        if (!(npc.m_9236_() instanceof ServerLevel level)) {
            return;
        }
        long now = level.m_46467_();
        Children.Child child = Children.get(level, npc.childId());
        if (child == null) {
            npc.m_146870_(); // a body of no one's child
            return;
        }
        String key = npc.uniqueKey();
        Mind mind = npc.mind;
        if (mind == null) {
            BlockPos first = npc.home() != null ? npc.home() : npc.settle();
            if (!UniqueNpcs.claim(level, key, npc.m_20148_(), first)) {
                npc.m_146870_();
                return;
            }
            mind = new Mind();
            npc.mind = mind;
            mind.nextLine = now + 400L + npc.m_217043_().m_188503_(600);
            mind.company = now;
        }
        BlockPos home = npc.home() == null ? npc.settle() : npc.home();
        if (now % 200L == 11L) {
            if (!UniqueNpcs.claim(level, key, npc.m_20148_(), home)) {
                npc.m_146870_();
                return;
            }
            BlockPos origin = npc.origin();
            boolean away = origin != null && Math.pow(origin.m_123341_() - home.m_123341_(), 2) + Math.pow(origin.m_123342_() - home.m_123342_(), 2)
                + Math.pow(origin.m_123343_() - home.m_123343_(), 2) > 36.0;
            UniqueNpcs.report(level, key, origin, away, mind.company);
            if (away && mind.carrier == null && now - mind.company >= UniqueNpc.GOES_HOME) {
                npc.goHome();
                mind.company = now;
            }
        }
        // Grown.
        ChildStage stage = child.stage(now);
        if (stage != mind.stage) {
            boolean first = mind.stage == null;
            mind.stage = stage;
            npc.stage = stage;
            npc.m_6210_();
            npc.m_21051_(net.minecraft.world.entity.ai.attributes.Attributes.f_22276_).m_22100_(stage.health);
            if (npc.m_21223_() > stage.health) {
                npc.m_21153_(stage.health);
            }
            if (!stage.carried()) {
                putDown(npc, mind);
            }
            if (!first) {
                tellParents(level, child, "§b" + ChildLines.grown(stage, child.name));
            }
            mind.dirty = true;
        }
        LivingEntity carrier = mind.carrier == null ? null : carrierOf(level, mind.carrier);
        if (mind.carrier != null && (carrier == null || !carrier.m_6084_() || carrier.m_9236_() != level || carrier.m_20280_(npc) > 16.0 * 16.0)) {
            putDown(npc, mind);
            carrier = null;
        }
        if (carrier != null) {
            carry(npc, carrier);
            mind.company = now;
        } else if (stage.sits()) {
            // A baby sits where it is put.
            npc.m_21573_().m_26573_();
            npc.m_20256_(new Vec3(0.0, npc.m_20184_().f_82480_, 0.0));
        } else {
            LivingEntity threat = threat(npc, child, level);
            if (threat != null && ChildRules.fights(stage, child.skill)) {
                fight(npc, mind, child, threat, level, now);
            } else {
                if (mind.turned) {
                    mind.turned = false;
                    mind.target = 0;
                    mind.dirty = true;
                }
                ServerPlayer leader = npc.following() == null ? null : player(level, npc.following());
                if (threat != null) {
                    // Untaught (or too little): it runs to a parent.
                    ServerPlayer parent = nearestParent(npc, child, level, 24.0);
                    if (parent != null) {
                        npc.m_21573_().m_5624_(parent, 1.3);
                    }
                    if (now % 60L == 0L && parent != null) {
                        say(parent, child, ChildLines.scared(stage, ChildLines.call(stage, isMother(child, parent)), npc.m_217043_().m_188503_(100)));
                    }
                } else if (leader != null && leader.m_9236_() == level) {
                    follow(npc, leader);
                    mind.company = now;
                }
            }
        }
        lines(npc, mind, child, level, now, stage);
        if (mind.dirty) {
            mind.dirty = false;
            ControlNetwork.broadcastAround(npc, message(npc, mind, child));
        }
    }

    // ---------------------------------------------------------------- carried

    /**
     * Held in the carrier's arms: sitting on their forearms at their chest ({@link ChildRules#CARRY_UP}, its hips there), facing them, a
     * little in front ({@link ChildRules#CARRY_AHEAD}). 1.3.15: no gravity while held (it fell between ticks, and was drawn by their feet),
     * and it neither pushes nor is pushed ({@code ChildNpc.carriedBy}); any living carrier (a player, or its NPC parent).
     */
    private static void carry(ChildNpc npc, LivingEntity carrier) {
        double scale = ChildNpc.carrySize(carrier);
        double yaw = Math.toRadians(carrier.f_20883_);
        double ahead = ChildRules.CARRY_AHEAD * scale;
        Vec3 at = carrier.m_20182_().m_82520_(-Math.sin(yaw) * ahead, ChildRules.CARRY_UP * scale, Math.cos(yaw) * ahead);
        npc.m_21573_().m_26573_();
        npc.m_20242_(true);
        npc.carriedBy = carrier.m_19879_();
        npc.m_6021_(at.f_82479_, at.f_82480_, at.f_82481_);
        npc.m_20256_(Vec3.f_82478_);
        npc.f_19789_ = 0.0F;
        float facing = carrier.f_20883_ + 180.0F;
        npc.m_146922_(facing);
        npc.f_20883_ = facing;
        npc.f_20885_ = facing;
    }

    /** The one {@code id} names: a player, or (1.3.15) any living body in the world (its NPC parent). */
    static LivingEntity carrierOf(ServerLevel level, UUID id) {
        ServerPlayer player = player(level, id);
        if (player != null) {
            return player;
        }
        return level.m_8791_(id) instanceof LivingEntity living ? living : null;
    }

    static void pickUp(ChildNpc npc, LivingEntity carrier) {
        if (npc.mind != null) {
            npc.mind.carrier = carrier.m_20148_();
            npc.mind.dirty = true;
        }
    }

    static void putDown(ChildNpc npc, Mind mind) {
        if (mind.carrier != null) {
            mind.carrier = null;
            mind.dirty = true;
            npc.settle();
        }
        npc.m_20242_(false);
        npc.carriedBy = 0;
    }

    static boolean carried(ChildNpc npc) {
        return npc.mind != null && npc.mind.carrier != null;
    }

    // ---------------------------------------------------------------- following

    private static void follow(ChildNpc npc, ServerPlayer leader) {
        double far = npc.m_20280_(leader);
        if (far > ChildRules.FOLLOW_TELEPORT * ChildRules.FOLLOW_TELEPORT) {
            Vec3 at = leader.m_20182_();
            npc.m_6021_(at.f_82479_, at.f_82480_, at.f_82481_);
            npc.m_21573_().m_26573_();
        } else if (far > ChildRules.FOLLOW_FROM * ChildRules.FOLLOW_FROM) {
            npc.m_21573_().m_5624_(leader, 1.15);
        } else if (far < ChildRules.FOLLOW_STOP * ChildRules.FOLLOW_STOP) {
            npc.m_21573_().m_26573_();
        }
        // Where it is told to wait is home; following, home is wherever it is.
        npc.m_21446_(new BlockPos((int)Math.floor(npc.m_20185_()), (int)Math.floor(npc.m_20186_()), (int)Math.floor(npc.m_20189_())), 6);
    }

    // ---------------------------------------------------------------- fighting

    /** Something hostile after it or its family, close: a monster with one of them as its target, or right on top of them. */
    private static LivingEntity threat(ChildNpc npc, Children.Child child, ServerLevel level) {
        LivingEntity best = null;
        double bestDistance = Double.MAX_VALUE;
        for (LivingEntity body : level.m_6443_(LivingEntity.class, npc.m_20191_().m_82400_(ChildRules.GUARD), entity -> entity instanceof Enemy && entity.m_6084_())) {
            LivingEntity target = body instanceof Mob mob ? mob.m_5448_() : null;
            boolean family = target == npc || target instanceof ServerPlayer player && child.parent(Pregnancies.player(player.m_20148_()));
            double distance = body.m_20280_(npc);
            if ((family || distance < 3.0 * 3.0) && distance < bestDistance) {
                best = body;
                bestDistance = distance;
            }
        }
        return best;
    }

    private static void fight(ChildNpc npc, Mind mind, Children.Child child, LivingEntity threat, ServerLevel level, long now) {
        boolean ghoul = child.genome().ghoul();
        if (!mind.turned || mind.target != threat.m_19879_()) {
            mind.turned = ghoul;
            mind.target = threat.m_19879_();
            mind.dirty = true;
            ServerPlayer parent = nearestParent(npc, child, level, 16.0);
            if (parent != null) {
                say(parent, child, ChildLines.fight(mind.stage, npc.m_217043_().m_188503_(100)));
            }
        }
        npc.m_21563_().m_24960_(threat, 30.0F, 30.0F);
        double reach = ChildRules.REACH + threat.m_20205_() * 0.5;
        if (npc.m_20280_(threat) > reach * reach) {
            npc.m_21573_().m_5624_(threat, 1.25);
            return;
        }
        npc.m_21573_().m_26573_();
        if (mind.lastHit == Long.MIN_VALUE || now - mind.lastHit >= ChildRules.cooldown(child.skill)) {
            mind.lastHit = now;
            npc.m_21011_(net.minecraft.world.InteractionHand.MAIN_HAND, true);
            threat.m_6469_(npc.m_269291_().m_269333_(npc), ChildRules.damage(mind.stage, child.skill, ghoul));
        }
    }

    // ---------------------------------------------------------------- what it says

    private static void lines(ChildNpc npc, Mind mind, Children.Child child, ServerLevel level, long now, ChildStage stage) {
        ServerPlayer parent = nearestParent(npc, child, level, 8.0);
        if (parent == null) {
            if (mind.parentAway == Long.MIN_VALUE) {
                mind.parentAway = now;
            }
            return;
        }
        mind.company = now;
        int seed = npc.m_217043_().m_188503_(1000);
        String call = ChildLines.call(stage, isMother(child, parent));
        if (mind.parentAway != Long.MIN_VALUE && now - mind.parentAway >= 1200L) {
            say(parent, child, ChildLines.greet(stage, call, seed));
            mind.nextLine = now + 600L + npc.m_217043_().m_188503_(900);
        }
        mind.parentAway = Long.MIN_VALUE;
        if (now >= mind.nextLine && mind.target == 0) {
            say(parent, child, ChildLines.idle(stage, call, seed));
            mind.nextLine = now + 900L + npc.m_217043_().m_188503_(1500);
        }
    }

    static void say(ServerPlayer player, Children.Child child, String line) {
        player.m_5661_(Component.m_237113_("§b" + child.name + "§r: " + line), false);
    }

    private static void tellParents(ServerLevel level, Children.Child child, String text) {
        for (String parent : new String[] {child.mother, child.father}) {
            ServerPlayer player = Pregnancies.playerOf(level, parent);
            if (player != null) {
                player.m_5661_(Component.m_237113_(text), false);
            }
        }
    }

    static boolean isMother(Children.Child child, ServerPlayer player) {
        return child.mother.equals(Pregnancies.player(player.m_20148_()));
    }

    static ServerPlayer nearestParent(ChildNpc npc, Children.Child child, ServerLevel level, double range) {
        ServerPlayer best = null;
        for (String parent : new String[] {child.mother, child.father}) {
            ServerPlayer player = Pregnancies.playerOf(level, parent);
            if (player != null && player.m_9236_() == level && player.m_20280_(npc) <= range * range && (best == null || player.m_20280_(npc) < best.m_20280_(npc))) {
                best = player;
            }
        }
        return best;
    }

    static ServerPlayer player(ServerLevel level, UUID id) {
        return id == null || level.m_7654_() == null ? null : level.m_7654_().m_6846_().m_11259_(id);
    }

    static S2CChild message(ChildNpc npc, Mind mind, Children.Child child) {
        LivingEntity carrier = mind == null || mind.carrier == null ? null : carrierOf((ServerLevel)npc.m_9236_(), mind.carrier);
        return new S2CChild(npc.m_19879_(), child.name, child.genome, (mind == null || mind.stage == null ? child.stage(npc.m_9236_().m_46467_()) : mind.stage)
            .ordinal(), carrier == null ? 0 : carrier.m_19879_(), mind != null && mind.turned);
    }

    /** A player starts seeing a child: they are told how to draw it. */
    @SubscribeEvent
    public static void startTracking(PlayerEvent.StartTracking event) {
        if (event.getTarget() instanceof ChildNpc npc && event.getEntity() instanceof ServerPlayer viewer && npc.m_9236_() instanceof ServerLevel level) {
            Children.Child child = Children.get(level, npc.childId());
            if (child != null) {
                ControlNetwork.sendTo(viewer, message(npc, npc.mind, child));
            }
        }
    }

    /** A parent's blow (a swing, a kagune's sweep) never lands on their own child. */
    @SubscribeEvent
    public static void attacked(net.minecraftforge.event.entity.living.LivingAttackEvent event) {
        if (event.getEntity() instanceof ChildNpc npc && event.getSource() != null && event.getSource().m_7639_() instanceof ServerPlayer player
            && npc.m_9236_() instanceof ServerLevel level) {
            Children.Child child = Children.get(level, npc.childId());
            if (child != null && child.parent(Pregnancies.player(player.m_20148_()))) {
                event.setCanceled(true);
            }
        }
    }

    /** It died: the world remembers (its body comes back home a day later), its parents are told. */
    @SubscribeEvent
    public static void died(LivingDeathEvent event) {
        if (event.getEntity() instanceof ChildNpc npc && npc.m_9236_() instanceof ServerLevel level) {
            UniqueNpcs.died(level, npc.uniqueKey(), npc.m_20148_());
            Children.Child child = Children.get(level, npc.childId());
            if (child != null) {
                tellParents(level, child, "§c" + child.name + " was hurt badly. They'll be back home tomorrow.");
            }
        }
    }
}
