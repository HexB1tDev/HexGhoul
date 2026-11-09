// Compile-only API stub (Minecraft 1.20.1 / Forge 47 / PlayerAnimator). Never packaged; see verify/api-allowlist.txt.
package net.minecraft.world.level.levelgen.structure.pools;

public abstract class StructurePoolElement {
    /** single(location): a single element by its template's id, for a projection */
    public static java.util.function.Function<StructureTemplatePool.Projection, ? extends StructurePoolElement> m_210526_(String location) {
        throw new UnsupportedOperationException();
    }

    /** getBoundingBox */
    public abstract net.minecraft.world.level.levelgen.structure.BoundingBox m_214015_(
        net.minecraft.world.level.levelgen.structure.templatesystem.StructureTemplateManager manager, net.minecraft.core.BlockPos position,
        net.minecraft.world.level.block.Rotation rotation);
}
