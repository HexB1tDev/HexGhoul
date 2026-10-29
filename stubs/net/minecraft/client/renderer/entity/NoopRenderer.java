// Compile-only API stub (Minecraft 1.20.1 / Forge 47 / PlayerAnimator). Never packaged; see verify/api-allowlist.txt.
package net.minecraft.client.renderer.entity;

/** Vanilla's renderer that draws nothing (the marker's, the area effect cloud's). */
public class NoopRenderer<T extends net.minecraft.world.entity.Entity> extends EntityRenderer<T> {
    public NoopRenderer(EntityRendererProvider.Context context) {
        super(context);
    }
}
