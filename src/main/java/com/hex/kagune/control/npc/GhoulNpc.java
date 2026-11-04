package com.hex.kagune.control.npc;

import com.hex.kagune.control.server.GhoulNpcBrain;
import net.minecraft.nbt.CompoundTag;
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
 * 7.0.2: a ghoul NPC. A human to look at (one of vanilla's default skins, {@link NpcLook}) until its kakugan opens and its kagune
 * tears out: the transplant's rinkaku tails or the ukaku's twin wings ({@link NpcIdentity.Kind}), fighting with that kagune's M1 and
 * nothing more. Who it is ({@link NpcIdentity}) is rolled when it turns up and kept in its saved data; everything it does is decided
 * on the server ({@link GhoulNpcBrain}), every client is told what it needs to draw it ({@code S2CGhoulNpc}). Vanilla's goals only
 * keep it afloat and let it look about; the brain walks it, picks its prey and fights.
 */
public class GhoulNpc extends PathfinderMob {
    /** Where its identity is kept in its saved data. */
    public static final String KEY = "HexGhoulNpc";

    private NpcIdentity identity;
    /** 2.1.9: 1 while it trembles (Peace or War in front of it), else 0. */
    static final net.minecraft.network.syncher.EntityDataAccessor<Integer> AFRAID = net.minecraft.network.syncher.SynchedEntityData.m_135353_(
        GhoulNpc.class, net.minecraft.network.syncher.EntityDataSerializers.f_135028_);
    /** The server's state for it (its prey, its grudges, its move); null on clients and until its first tick. */
    public GhoulNpcBrain.Mind mind;

    public GhoulNpc(EntityType<? extends GhoulNpc> type, Level level) {
        super(type, level);
    }

    /** Its attributes before its tier is put on it ({@link GhoulNpcBrain#dress}): the lowest tier's. */
    public static AttributeSupplier.Builder attributes() {
        return Mob.m_21552_()
            .m_22268_(Attributes.f_22276_, NpcRules.health(0))
            .m_22268_(Attributes.f_22279_, NpcRules.speed(0))
            .m_22268_(Attributes.f_22277_, 32.0)
            .m_22268_(Attributes.f_22284_, NpcRules.armor(0))
            .m_22268_(Attributes.f_22278_, 0.3);
    }

    @Override
    protected void m_8097_() {
        super.m_8097_();
        this.f_19804_.m_135372_(AFRAID, 0);
    }

    /** 2.1.9: true while it trembles before Peace or War. */
    public boolean afraid() {
        return this.f_19804_.m_135370_(AFRAID) != 0;
    }

    public void afraid(boolean afraid) {
        int value = afraid ? 1 : 0;
        if (this.f_19804_.m_135370_(AFRAID) != value) {
            this.f_19804_.m_135381_(AFRAID, value);
        }
    }

    /** Who it is, as kept in its saved data (server), or null if it has none yet. */
    public NpcIdentity identity() {
        if (this.identity == null) {
            CompoundTag tag = this.getPersistentData();
            if (tag.m_128441_(KEY)) {
                this.identity = NpcIdentity.unpack(tag.m_128454_(KEY));
            }
        }
        return this.identity;
    }

    /** Makes it {@code identity}, kept in its saved data. */
    public void identify(NpcIdentity identity) {
        this.identity = identity;
        this.getPersistentData().m_128356_(KEY, identity.pack());
    }

    @Override
    protected void m_8099_() {
        this.f_21345_.m_25352_(0, new FloatGoal(this));
        this.f_21345_.m_25352_(7, new LookAtPlayerGoal(this, Player.class, 10.0F));
        this.f_21345_.m_25352_(8, new RandomLookAroundGoal(this));
    }

    @Override
    protected void m_8024_() {
        GhoulNpcBrain.tick(this);
    }

    @Override
    protected void m_7472_(DamageSource source, int looting, boolean recentlyHit) {
        super.m_7472_(source, looting, recentlyHit);
        GhoulNpcBrain.drop(this);
    }
}
