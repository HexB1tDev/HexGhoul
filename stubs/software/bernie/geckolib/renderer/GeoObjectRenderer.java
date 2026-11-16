// Compile-only API stub (Minecraft 1.20.1 / Forge 47 / GeckoLib 4.8.4, bernie-g/geckolib branch 1.20.1). Never packaged; see verify/api-allowlist.txt.
package software.bernie.geckolib.renderer;

/** Forge/src/main/java/software/bernie/geckolib/renderer/GeoObjectRenderer.java */
public class GeoObjectRenderer<T extends software.bernie.geckolib.core.animatable.GeoAnimatable> implements GeoRenderer<T> {
    public GeoObjectRenderer(software.bernie.geckolib.model.GeoModel<T> model) { throw new UnsupportedOperationException(); }

    public void render(com.mojang.blaze3d.vertex.PoseStack poseStack, T animatable, net.minecraft.client.renderer.MultiBufferSource bufferSource,
                       net.minecraft.client.renderer.RenderType renderType, com.mojang.blaze3d.vertex.VertexConsumer buffer, int packedLight) {
        throw new UnsupportedOperationException();
    }
}
