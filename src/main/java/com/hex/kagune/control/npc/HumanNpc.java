package com.hex.kagune.control.npc;

import com.hex.kagune.control.server.HumanBrain;
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
 * Home Living (1.3.40): The Human ({@link HumanRules}): a person in one of vanilla's default skins, wandering, running from ghouls
 * ({@link HumanBrain}). Its look and whether it is frightened ride on its entity data (every client draws its arms up as it runs).
 */
public class HumanNpc extends PathfinderMob {
    /** Where its look is kept in its saved data. */
    public static final String KEY = "HexHuman";

    /** Its look plus one (0: not given one yet). */
    static final EntityDataAccessor<Integer> LOOK = SynchedEntityData.m_135353_(HumanNpc.class, EntityDataSerializers.f_135028_);
    /** 1 while it is frightened. */
    static final EntityDataAccessor<Integer> SCARED = SynchedEntityData.m_135353_(HumanNpc.class, EntityDataSerializers.f_135028_);

    /** The server's state for it; null on clients and until its first tick. */
    public HumanBrain.Mind mind;

    public HumanNpc(EntityType<? extends HumanNpc> type, Level level) {
        super(type, level);
    }

    public static AttributeSupplier.Builder attributes() {
        return Mob.m_21552_()
            .m_22268_(Attributes.f_22276_, HumanRules.HEALTH)
            .m_22268_(Attributes.f_22279_, HumanRules.SPEED)
            .m_22268_(Attributes.f_22277_, 24.0);
    }

    @Override
    protected void m_8097_() {
        super.m_8097_();
        this.f_19804_.m_135372_(LOOK, 0);
        this.f_19804_.m_135372_(SCARED, 0);
    }

    public boolean dressed() {
        return this.f_19804_.m_135370_(LOOK) != 0;
    }

    public int look() {
        return Math.max(0, this.f_19804_.m_135370_(LOOK) - 1);
    }

    public void dress(int look) {
        if (this.f_19804_.m_135370_(LOOK) != look + 1) {
            this.f_19804_.m_135381_(LOOK, look + 1);
        }
    }

    public boolean scared() {
        return this.f_19804_.m_135370_(SCARED) != 0;
    }

    public void scared(boolean scared) {
        int value = scared ? 1 : 0;
        if (this.f_19804_.m_135370_(SCARED) != value) {
            this.f_19804_.m_135381_(SCARED, value);
        }
    }

    @Override
    protected void m_8099_() {
        this.f_21345_.m_25352_(0, new FloatGoal(this));
        this.f_21345_.m_25352_(7, new LookAtPlayerGoal(this, Player.class, 8.0F));
        this.f_21345_.m_25352_(8, new RandomLookAroundGoal(this));
    }

    @Override
    protected void m_8024_() {
        HumanBrain.tick(this);
    }

    /** What it leaves: its meat ({@link HumanRules#meat}). */
    @Override
    protected void m_7472_(DamageSource source, int looting, boolean recentlyHit) {
        HumanBrain.drop(this, looting);
    }

    /** Made by hand (a command), it stays where it was put. */
    @Override
    public boolean m_6785_(double distanceSqr) {
        return false;
    }
}
