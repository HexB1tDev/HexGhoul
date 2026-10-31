package com.hex.kagune.control.npc;

import com.hex.kagune.control.server.ChildBrain;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.PathfinderMob;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.ai.goal.FloatGoal;
import net.minecraft.world.entity.ai.goal.LookAtPlayerGoal;
import net.minecraft.world.entity.ai.goal.MoveTowardsRestrictionGoal;
import net.minecraft.world.entity.ai.goal.RandomLookAroundGoal;
import net.minecraft.world.entity.ai.goal.RandomStrollGoal;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;

/**
 * 1.3.10: a child (of a player and an NPC, or of two players to come): one of a kind ({@link UniqueBody}: no fall damage, it belongs at its
 * home and comes back there a day after it dies), known by its id ({@code Children} keeps the rest: its name, what it inherited, when it
 * was born, what it was taught). Everything it does is decided on the server ({@code ChildBrain}); every client is told what it needs to
 * draw it ({@code S2CChild}). Never despawned.
 */
public class ChildNpc extends PathfinderMob implements UniqueBody {
    static final String ID = "HexChild";
    static final String HOME = "HexChildHome";
    static final String ORIGIN = "HexChildOrigin";
    static final String FOLLOW = "HexChildFollow";

    /** The server's state for it; null on clients and until its first tick. */
    public ChildBrain.Mind mind;
    /** How grown it is, on both sides (the server's brain sets it; a client, from {@code S2CChild}): its box is its stage's. */
    public com.hex.kagune.control.family.ChildStage stage = com.hex.kagune.control.family.ChildStage.BABY;
    private BlockPos home;

    public ChildNpc(EntityType<? extends ChildNpc> type, Level level) {
        super(type, level);
        this.m_21530_();
    }

    public static AttributeSupplier.Builder attributes() {
        return Mob.m_21552_()
            .m_22268_(Attributes.f_22276_, 24.0)
            .m_22268_(Attributes.f_22279_, 0.3)
            .m_22268_(Attributes.f_22277_, 24.0);
    }

    /** Its id among the children ({@code Children}), or "" before it is given one. */
    public String childId() {
        return this.getPersistentData().m_128461_(ID);
    }

    public void setChild(String id) {
        this.getPersistentData().m_128359_(ID, id);
    }

    public BlockPos home() {
        if (this.home == null) {
            CompoundTag tag = this.getPersistentData();
            if (tag.m_128441_(HOME + "X")) {
                this.home = new BlockPos(tag.m_128451_(HOME + "X"), tag.m_128451_(HOME + "Y"), tag.m_128451_(HOME + "Z"));
            }
        }
        return this.home;
    }

    /** Its home made where it stands now (one block up). */
    public BlockPos settle() {
        int x = (int)Math.floor(this.m_20185_());
        int y = (int)Math.floor(this.m_20186_()) + 1;
        int z = (int)Math.floor(this.m_20189_());
        CompoundTag tag = this.getPersistentData();
        tag.m_128405_(HOME + "X", x);
        tag.m_128405_(HOME + "Y", y);
        tag.m_128405_(HOME + "Z", z);
        this.home = new BlockPos(x, y, z);
        return this.home;
    }

    public java.util.UUID following() {
        CompoundTag tag = this.getPersistentData();
        return tag.m_128403_(FOLLOW) ? tag.m_128342_(FOLLOW) : null;
    }

    public void follow(java.util.UUID player) {
        CompoundTag tag = this.getPersistentData();
        if (player == null) {
            tag.m_128473_(FOLLOW);
        } else {
            tag.m_128362_(FOLLOW, player);
        }
    }

    @Override
    public String uniqueKey() {
        return "child:" + this.childId();
    }

    @Override
    public BlockPos origin() {
        CompoundTag tag = this.getPersistentData();
        if (!tag.m_128441_(ORIGIN + "X")) {
            BlockPos home = this.home();
            if (home == null) {
                return null;
            }
            tag.m_128405_(ORIGIN + "X", home.m_123341_());
            tag.m_128405_(ORIGIN + "Y", home.m_123342_());
            tag.m_128405_(ORIGIN + "Z", home.m_123343_());
        }
        return new BlockPos(tag.m_128451_(ORIGIN + "X"), tag.m_128451_(ORIGIN + "Y"), tag.m_128451_(ORIGIN + "Z"));
    }

    @Override
    public void goHome() {
        BlockPos origin = this.origin();
        this.follow(null);
        if (origin == null) {
            return;
        }
        this.m_21573_().m_26573_();
        this.m_6021_(origin.m_123341_() + 0.5, origin.m_123342_() - 1.0, origin.m_123343_() + 0.5);
        CompoundTag tag = this.getPersistentData();
        tag.m_128405_(HOME + "X", origin.m_123341_());
        tag.m_128405_(HOME + "Y", origin.m_123342_());
        tag.m_128405_(HOME + "Z", origin.m_123343_());
        this.home = origin;
    }

    /** A baby or a toddler is drawn on a little one's proportions (the model's own: a big head, a short body). */
    @Override
    public boolean m_6162_() {
        return this.stage != null && this.stage.young;
    }

    @Override
    protected void m_8099_() {
        this.f_21345_.m_25352_(0, new FloatGoal(this));
        this.f_21345_.m_25352_(4, new MoveTowardsRestrictionGoal(this, 1.0));
        this.f_21345_.m_25352_(6, new RandomStrollGoal(this, 0.55));
        this.f_21345_.m_25352_(7, new LookAtPlayerGoal(this, Player.class, 8.0F));
        this.f_21345_.m_25352_(8, new RandomLookAroundGoal(this));
    }

    @Override
    protected void m_8024_() {
        try {
            ChildBrain.tick(this);
        } catch (RuntimeException failure) {
            com.hex.kagune.control.Safety.caught(failure, "a child's tick");
        }
    }
}
