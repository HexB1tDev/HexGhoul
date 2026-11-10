package com.hex.kagune.control.npc;

import com.hex.kagune.control.quinque.Arm;
import com.hex.kagune.control.quinque.Quinque;
import com.hex.kagune.control.server.AgentBrain;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.PathfinderMob;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.ai.goal.FloatGoal;
import net.minecraft.world.entity.ai.goal.LookAtPlayerGoal;
import net.minecraft.world.entity.ai.goal.RandomLookAroundGoal;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;

/**
 * 1.3.27: a CCG investigator ({@link AgentRules}): one of a pair, a human in a coat with a quinque shut in its case. Everything it does
 * is decided on the server ({@link AgentBrain}); what every client needs to draw it rides on its entity data: who it looks like and the
 * quinque it carries, the swing it is making (which of its string, or a parry) and when it began, its flip and its dash, its last shot.
 * The quinque itself is the stack in its hand (opening and shutting as a player's does, drawn by the same models).
 */
public class CcgAgent extends PathfinderMob {
    /** Where who it is (its look, its quinque, its partner, whether it leads) is kept in its saved data. */
    public static final String KEY = "HexCcgAgent";

    /** Its look (the low byte: {@link AgentRules#LOOKS}) and its quinque's ordinal (the next), plus one (0: not dressed yet). */
    static final EntityDataAccessor<Integer> LOOK = SynchedEntityData.m_135353_(CcgAgent.class, EntityDataSerializers.f_135028_);
    /** Its swing: which of its string (0..2), {@link #PARRY}, or -1; and the game tick it began (the low 32 bits). */
    static final EntityDataAccessor<Integer> SWING = SynchedEntityData.m_135353_(CcgAgent.class, EntityDataSerializers.f_135028_);
    static final EntityDataAccessor<Integer> SWING_AT = SynchedEntityData.m_135353_(CcgAgent.class, EntityDataSerializers.f_135028_);
    /** Its flip ({@code PaleRules.Flip}'s ordinal, -1 none) and when it took off. */
    static final EntityDataAccessor<Integer> FLIP = SynchedEntityData.m_135353_(CcgAgent.class, EntityDataSerializers.f_135028_);
    static final EntityDataAccessor<Integer> FLIP_AT = SynchedEntityData.m_135353_(CcgAgent.class, EntityDataSerializers.f_135028_);
    /** Its dash (0 forward, 1 back, 2 left, 3 right) and when it pushed off; its last shard and when. */
    static final EntityDataAccessor<Integer> DASH = SynchedEntityData.m_135353_(CcgAgent.class, EntityDataSerializers.f_135028_);
    static final EntityDataAccessor<Integer> DASH_AT = SynchedEntityData.m_135353_(CcgAgent.class, EntityDataSerializers.f_135028_);
    static final EntityDataAccessor<Integer> SHOT_AT = SynchedEntityData.m_135353_(CcgAgent.class, EntityDataSerializers.f_135028_);
    /** 2.1.9: 1 while it trembles (Peace or War in front of it), else 0. */
    static final EntityDataAccessor<Integer> AFRAID = SynchedEntityData.m_135353_(CcgAgent.class, EntityDataSerializers.f_135028_);
    /**
     * 3.0.0: its kagune-made weapon (the higher ranks', {@link ArmRules#pack}: which, a masterwork, out of its case; 0 none), when it last
     * tore out of its case or went back in, and who its whip binds (an entity id, 0 none).
     */
    static final EntityDataAccessor<Integer> ARM = SynchedEntityData.m_135353_(CcgAgent.class, EntityDataSerializers.f_135028_);
    static final EntityDataAccessor<Integer> ARM_AT = SynchedEntityData.m_135353_(CcgAgent.class, EntityDataSerializers.f_135028_);
    static final EntityDataAccessor<Integer> ARM_TARGET = SynchedEntityData.m_135353_(CcgAgent.class, EntityDataSerializers.f_135028_);

    /** 4.1.4: 1 while it sits at its desk in the CCG building ({@link HqRules}), else 0. */
    static final EntityDataAccessor<Integer> SEAT = SynchedEntityData.m_135353_(CcgAgent.class, EntityDataSerializers.f_135028_);

    /** The swing that meets a blow coming at it: a quick cut up across it, its weapon slapping the blow aside. */
    public static final int PARRY = 3;
    public static final int KICK = 4;
    public static final int AIM_DAWN = 5;
    /** 2.0.33: the White Reaper driving Dawn into the ground for his tendril (npc.ReaperTendril). */
    public static final int GROUND = 6;
    /** "Never", on the 32-bit clocks. */
    public static final int NEVER = AgentRules.NEVER;

    /** The server's state for it; null on clients and until its first tick. */
    public AgentBrain.Mind mind;

    public CcgAgent(EntityType<? extends CcgAgent> type, Level level) {
        super(type, level);
    }

    public static AttributeSupplier.Builder attributes() {
        return Mob.m_21552_()
            .m_22268_(Attributes.f_22276_, AgentRules.HEALTH)
            .m_22268_(Attributes.f_22279_, AgentRules.SPEED)
            .m_22268_(Attributes.f_22277_, AgentRules.FOLLOW)
            .m_22268_(Attributes.f_22284_, AgentRules.ARMOR)
            .m_22268_(Attributes.f_22278_, 0.35);
    }

    @Override
    protected void m_8097_() {
        super.m_8097_();
        this.f_19804_.m_135372_(LOOK, 0);
        this.f_19804_.m_135372_(SWING, -1);
        this.f_19804_.m_135372_(SWING_AT, NEVER);
        this.f_19804_.m_135372_(FLIP, -1);
        this.f_19804_.m_135372_(FLIP_AT, NEVER);
        this.f_19804_.m_135372_(DASH, 0);
        this.f_19804_.m_135372_(DASH_AT, NEVER);
        this.f_19804_.m_135372_(SHOT_AT, NEVER);
        this.f_19804_.m_135372_(AFRAID, 0);
        this.f_19804_.m_135372_(ARM, 0);
        this.f_19804_.m_135372_(ARM_AT, NEVER);
        this.f_19804_.m_135372_(ARM_TARGET, 0);
        this.f_19804_.m_135372_(SEAT, 0);
    }

    /** 4.1.4: true while it sits at its desk. */
    public boolean seated() {
        return this.get(SEAT) != 0;
    }

    public void seated(boolean seated) {
        this.put(SEAT, seated ? 1 : 0);
    }

    /** 3.0.0: its kagune-made weapon as packed ({@link ArmRules#pack}; 0 none). */
    public int armPacked() {
        return this.get(ARM);
    }

    /** 3.0.0: the weapon it carries (null: a regular quinque). */
    public Arm weapon() {
        return ArmRules.arm(this.get(ARM));
    }

    public int armAt() {
        return this.get(ARM_AT);
    }

    /** 3.0.0: its weapon ({@code packed}) as it is now, and when it last came out or went in ({@link Long#MIN_VALUE}: never). */
    public void arm(int packed, long at) {
        this.put(ARM, packed);
        this.put(ARM_AT, at == Long.MIN_VALUE ? NEVER : (int)at);
    }

    /** 3.0.0: who its whip binds (an entity id; 0 none). */
    public int armTarget() {
        return this.get(ARM_TARGET);
    }

    public void armTarget(int id) {
        this.put(ARM_TARGET, id);
    }

    /** 2.1.9: true while it trembles before Peace or War. */
    public boolean afraid() {
        return this.get(AFRAID) != 0;
    }

    public void afraid(boolean afraid) {
        this.put(AFRAID, afraid ? 1 : 0);
    }

    private int get(EntityDataAccessor<Integer> key) {
        return this.f_19804_.m_135370_(key);
    }

    private void put(EntityDataAccessor<Integer> key, int value) {
        if (this.get(key) != value) {
            this.f_19804_.m_135381_(key, value);
        }
    }

    /** True once it has been given who it is (its look and quinque). */
    public boolean dressed() {
        return this.get(LOOK) != 0;
    }

    /** Its look's number in {@link AgentRules#LOOKS}. */
    public int lookIndex() {
        int packed = this.get(LOOK);
        return packed == 0 ? 0 : (packed - 1) & 0xFF;
    }

    public AgentRules.Look look() {
        return AgentRules.look(this.lookIndex());
    }

    /** The quinque it carries. */
    public Quinque kind() {
        int packed = this.get(LOOK);
        Quinque[] all = Quinque.values();
        return packed == 0 ? Quinque.BLADE : all[Math.floorMod(((packed - 1) >> 8) & 0xFF, all.length)];
    }

    /** It looks like {@code look} and carries {@code kind} (server: every client is told). */
    public void dress(int look, Quinque kind) {
        this.put(LOOK, 1 + (Math.floorMod(look, AgentRules.LOOKS.length) | kind.ordinal() << 8));
    }

    /**
     * Its swing: which (0..2, {@link #PARRY}) or -1, and when it began (the low 32 bits of the game tick). 3.0.0: with a kagune-made weapon,
     * a blow of its string ({@link ArmRules#stepCode}) or one of its moves ({@link ArmRules#abilityCode}).
     */
    public int swing() {
        return this.get(SWING);
    }

    public int swingAt() {
        return this.get(SWING_AT);
    }

    public void swing(int which, long at) {
        this.put(SWING, which);
        this.put(SWING_AT, (int)at);
    }

    public int flip() {
        return this.get(FLIP);
    }

    public int flipAt() {
        return this.get(FLIP_AT);
    }

    /**
     * 2.0.49: added to a flip's number (PaleRules.Flip) for the White Reaper's soar (an Up We Go launch, ReaperTactics): drawn rising
     * long and turning over by the top of it, where he aims.
     */
    public static final int SOARING = 10;

    public void flip(int which, long at) {
        this.put(FLIP, which);
        this.put(FLIP_AT, (int)at);
    }

    public int dash() {
        return this.get(DASH);
    }

    public int dashAt() {
        return this.get(DASH_AT);
    }

    public void dash(int way, long at) {
        this.put(DASH, way);
        this.put(DASH_AT, (int)at);
    }

    public int shotAt() {
        return this.get(SHOT_AT);
    }

    public void shot(long at) {
        this.put(SHOT_AT, (int)at);
    }

    /** Ticks since {@code at} (one of its 32-bit clocks) on the game clock {@code now} ({@link AgentRules#since}). */
    public static long since(long now, int at) {
        return AgentRules.since(now, at);
    }

    @Override
    protected void m_8099_() {
        this.f_21345_.m_25352_(0, new FloatGoal(this));
        this.f_21345_.m_25352_(7, new LookAtPlayerGoal(this, Player.class, 10.0F));
        this.f_21345_.m_25352_(8, new RandomLookAroundGoal(this));
    }

    @Override
    protected void m_8024_() {
        AgentBrain.tick(this);
    }

    /** What it leaves when it falls: now and then its case, the commoner the likelier ({@link AgentRules#caseChance}); a ration. */
    @Override
    protected void m_7472_(DamageSource source, int looting, boolean recentlyHit) {
        AgentBrain.drop(this, looting, recentlyHit);
    }

    /** Far from everyone it goes, as a wanderer does (never in the middle of anything a player could see). */
    @Override
    public boolean m_6785_(double distanceSqr) {
        return distanceSqr > 64.0 * 64.0;
    }
}
