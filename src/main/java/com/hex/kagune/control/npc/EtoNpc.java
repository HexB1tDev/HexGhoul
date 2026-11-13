package com.hex.kagune.control.npc;

import com.hex.kagune.control.server.EtoBrain;
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
 * 1.3.0: Eto, who lives in the Library (the structure places her on its ground floor). A girl on the slim player model in her own skin,
 * her kakugan in one eye at night and when she fights ({@code EtoRenderer}). She keeps to her home (where she first stood, kept in her
 * saved data): vanilla's goals walk her about inside it and back into it; everything else, the fight, the talk, her gestures, is decided
 * on the server ({@link EtoBrain}) and every client is told what it needs to draw her ({@code S2CEto}). She is never despawned.
 */
public class EtoNpc extends PathfinderMob implements UniqueBody {
    /** Where her home is kept in her saved data (a block: where she first stood, one up). */
    static final String HOME = "HexEtoHome";
    /** 1.3.1: the player she follows (her saved data), if any. */
    static final String FOLLOW = "HexEtoFollow";
    /** 1.3.3: where she belongs (her first home, the Library's floor), kept for good. */
    static final String ORIGIN = "HexEtoOrigin";

    /** The server's state for her (her fight, her gesture); null on clients and until her first tick. */
    public EtoBrain.Mind mind;
    private BlockPos home;

    public EtoNpc(EntityType<? extends EtoNpc> type, Level level) {
        super(type, level);
        // 1.3.21: she opens doors (and closes them behind her), to roam her home's rooms.
        if (this.m_21573_() instanceof net.minecraft.world.entity.ai.navigation.GroundPathNavigation ground) {
            ground.m_26477_(true);
        }
    }

    public static AttributeSupplier.Builder attributes() {
        return attributes(Persona.ETO);
    }

    /** 1.3.14: any companion's: its health and armour, a player's speed. */
    public static AttributeSupplier.Builder attributes(Persona persona) {
        return Mob.m_21552_()
            .m_22268_(Attributes.f_22276_, persona.health())
            .m_22268_(Attributes.f_22279_, persona.speed())
            .m_22268_(Attributes.f_22277_, 32.0)
            .m_22268_(Attributes.f_22284_, persona.armor())
            .m_22268_(Attributes.f_22278_, 0.6);
    }

    /** 1.3.14: who this body is (Eto's; Touka's and Ken's bodies say otherwise). */
    public Persona persona() {
        return Persona.ETO;
    }

    /** 1.3.14: what it is called: its name tag's name, or its own. */
    public String displayName() {
        try {
            net.minecraft.world.entity.Entity self = this;
            if (self.m_8077_() && self.m_7770_() != null) {
                String tagged = self.m_7770_().getString();
                if (tagged != null && !tagged.isBlank()) {
                    return tagged.length() > 24 ? tagged.substring(0, 24) : tagged;
                }
            }
        } catch (RuntimeException ignored) {
            // its own name, then
        }
        return this.persona().name();
    }

    /** Her home (a block: where she first stood, one up), set on her first tick and kept from then on; null before. */
    public BlockPos home() {
        if (this.home == null) {
            CompoundTag tag = this.getPersistentData();
            if (tag.m_128441_(HOME + "X")) {
                this.home = new BlockPos(tag.m_128451_(HOME + "X"), tag.m_128451_(HOME + "Y"), tag.m_128451_(HOME + "Z"));
            }
        }
        return this.home;
    }

    /** Her home made where she stands now (one block up), kept in her saved data. */
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

    /** 1.3.1: the player she follows (kept in her saved data), or null: she keeps to her home. */
    public java.util.UUID following() {
        CompoundTag tag = this.getPersistentData();
        return tag.m_128403_(FOLLOW) ? tag.m_128342_(FOLLOW) : null;
    }

    /** 1.3.1: she follows this player from now on (null: no one). */
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
        return this.persona().key();
    }

    /** 1.3.3: where she belongs: her first home (kept the first time she has one), or null before her first tick. */
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

    /** 1.3.14: where she stands is her home and where she belongs from now on (moved by command: until her own place is built). */
    public void rehome() {
        this.follow(null);
        BlockPos at = this.settle();
        CompoundTag tag = this.getPersistentData();
        tag.m_128405_(ORIGIN + "X", at.m_123341_());
        tag.m_128405_(ORIGIN + "Y", at.m_123342_());
        tag.m_128405_(ORIGIN + "Z", at.m_123343_());
    }

    /** 1.3.21: her home is {@code at} from now on (the home shared with her spouse), where she belongs too. */
    public void homeAt(BlockPos at) {
        this.follow(null);
        CompoundTag tag = this.getPersistentData();
        tag.m_128405_(HOME + "X", at.m_123341_());
        tag.m_128405_(HOME + "Y", at.m_123342_());
        tag.m_128405_(HOME + "Z", at.m_123343_());
        tag.m_128405_(ORIGIN + "X", at.m_123341_());
        tag.m_128405_(ORIGIN + "Y", at.m_123342_());
        tag.m_128405_(ORIGIN + "Z", at.m_123343_());
        this.home = at;
    }

    /** 1.3.3: home again: following no one, back on her own floor, settled there. */
    @Override
    public void goHome() {
        BlockPos origin = this.origin();
        this.follow(null);
        if (origin == null) {
            return;
        }
        this.m_21573_().m_26573_();
        // 4.1.73: on her floor at home (its collision's top, room for all of her), never at the whole block it was kept as.
        double feet = this.m_9236_() instanceof net.minecraft.server.level.ServerLevel level
            ? com.hex.kagune.control.server.NpcFooting.feet(level, this, origin.m_123341_() + 0.5, origin.m_123342_() - 1.0, origin.m_123343_() + 0.5)
            : origin.m_123342_() - 1.0;
        this.m_6021_(origin.m_123341_() + 0.5, feet, origin.m_123343_() + 0.5);
        CompoundTag tag = this.getPersistentData();
        tag.m_128405_(HOME + "X", origin.m_123341_());
        tag.m_128405_(HOME + "Y", origin.m_123342_());
        tag.m_128405_(HOME + "Z", origin.m_123343_());
        this.home = origin;
    }

    /**
     * 1.3.29: sent home ("Go home, please."): following no one, her home where she belongs again; no teleport: she walks there (and is
     * put there only from far off, as when she is lost). Returns where that is (null before her first tick).
     */
    public BlockPos sendHome() {
        BlockPos origin = this.origin();
        this.follow(null);
        if (origin == null) {
            return null;
        }
        CompoundTag tag = this.getPersistentData();
        tag.m_128405_(HOME + "X", origin.m_123341_());
        tag.m_128405_(HOME + "Y", origin.m_123342_());
        tag.m_128405_(HOME + "Z", origin.m_123343_());
        this.home = origin;
        return origin;
    }

    /** Where her home's floor is, in the middle of its block (x, feet y, z). */
    public double[] homeFeet() {
        BlockPos at = this.home();
        if (at == null) {
            return new double[] {this.m_20185_(), this.m_20186_(), this.m_20189_()};
        }
        CompoundTag tag = this.getPersistentData();
        return new double[] {tag.m_128451_(HOME + "X") + 0.5, tag.m_128451_(HOME + "Y") - 1.0, tag.m_128451_(HOME + "Z") + 0.5};
    }

    /** isImmobile: 1.3.21: lying asleep in her bed, she doesn't move (her own goals wait till she gets up). */
    @Override
    protected boolean m_6107_() {
        return this.m_21223_() <= 0.0F || this.m_5803_();
    }

    @Override
    protected void m_8099_() {
        this.f_21345_.m_25352_(0, new FloatGoal(this));
        this.f_21345_.m_25352_(2, new net.minecraft.world.entity.ai.goal.OpenDoorGoal(this, true));
        this.f_21345_.m_25352_(4, new MoveTowardsRestrictionGoal(this, 1.0));
        // 1.3.29: her strolling waits while she does a chore at home (it walked her off to the door and back instead).
        this.f_21345_.m_25352_(6, new RandomStrollGoal(this, 0.6) {
            @Override
            public boolean m_8036_() {
                return !EtoBrain.busy(EtoNpc.this) && super.m_8036_();
            }

            @Override
            public boolean m_8045_() {
                return !EtoBrain.busy(EtoNpc.this) && super.m_8045_();
            }
        });
        // 2.0.8 (the owner: "dont allow them to rotate while cooking"): no glancing at players or about while a chore has her.
        this.f_21345_.m_25352_(7, new LookAtPlayerGoal(this, Player.class, 8.0F) {
            @Override
            public boolean m_8036_() {
                return !EtoBrain.busy(EtoNpc.this) && super.m_8036_();
            }

            @Override
            public boolean m_8045_() {
                return !EtoBrain.busy(EtoNpc.this) && super.m_8045_();
            }
        });
        this.f_21345_.m_25352_(8, new RandomLookAroundGoal(this) {
            @Override
            public boolean m_8036_() {
                return !EtoBrain.busy(EtoNpc.this) && super.m_8036_();
            }

            @Override
            public boolean m_8045_() {
                return !EtoBrain.busy(EtoNpc.this) && super.m_8045_();
            }
        });
    }

    @Override
    protected void m_8024_() {
        // Whatever goes wrong in her mind takes only this tick, never the world (logged).
        try {
            EtoBrain.tick(this);
        } catch (RuntimeException failure) {
            com.hex.kagune.control.Safety.caught(failure, this.persona().name() + "'s tick");
        }
    }
}
