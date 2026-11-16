// Compile-only API stub (Minecraft 1.20.1 / Forge 47 / GeckoLib 4.8.4, bernie-g/geckolib branch 1.20.1). Never packaged; see verify/api-allowlist.txt.
package software.bernie.geckolib.model;

/** Forge/src/main/java/software/bernie/geckolib/model/GeoModel.java */
public abstract class GeoModel<T extends software.bernie.geckolib.core.animatable.GeoAnimatable>
    implements software.bernie.geckolib.core.animatable.model.CoreGeoModel<T> {
    public abstract net.minecraft.resources.ResourceLocation getModelResource(T animatable);

    public abstract net.minecraft.resources.ResourceLocation getTextureResource(T animatable);

    public abstract net.minecraft.resources.ResourceLocation getAnimationResource(T animatable);

    public java.util.Optional<software.bernie.geckolib.cache.object.GeoBone> getBone(String name) { throw new UnsupportedOperationException(); }
}
