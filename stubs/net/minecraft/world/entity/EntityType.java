// Compile-only API stub (Minecraft 1.20.1 / Forge 47 / PlayerAnimator). Never packaged; see verify/api-allowlist.txt.
package net.minecraft.world.entity;

public class EntityType<T extends Entity> {
    /** create(level) */ public T m_20615_(net.minecraft.world.level.Level level) { throw new UnsupportedOperationException(); }

    public interface EntityFactory<T extends Entity> {
        /** create(type, level) */ T m_20721_(EntityType<T> type, net.minecraft.world.level.Level level);
    }

    public static class Builder<T extends Entity> {
        /** of(factory, category) */ public static <T extends Entity> Builder<T> m_20704_(EntityFactory<T> factory, MobCategory category) { throw new UnsupportedOperationException(); }
        /** sized(width, height) */ public Builder<T> m_20699_(float width, float height) { throw new UnsupportedOperationException(); }
        /** clientTrackingRange */ public Builder<T> m_20702_(int range) { throw new UnsupportedOperationException(); }
        /** updateInterval */ public Builder<T> m_20717_(int interval) { throw new UnsupportedOperationException(); }
        /** build(id) */ public EntityType<T> m_20712_(String id) { throw new UnsupportedOperationException(); }
    }
}
