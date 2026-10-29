import com.hex.kagune.control.graft.GraftSkin;
import java.awt.image.BufferedImage;
import java.io.File;
import javax.imageio.ImageIO;

/**
 * Paints the kakuja's hide (7.0.0): every texel of {@link GraftSkin}'s layout gets the paint of the point of the body it lands on,
 * so the whorls and slashes run on across the edges of the boxes. Writes {@code graft_kakuja.png} and its glow
 * ({@code graft_kakuja_glow.png}) into the given directory (the mod's textures/entity).
 * Usage: {@code java -cp <classes> GraftTextures src/main/resources/assets/hexkagune/textures/entity}
 */
public final class GraftTextures {
    private GraftTextures() {
    }

    public static void main(String[] args) throws Exception {
        File dir = new File(args.length > 0 ? args[0] : "build/preview");
        dir.mkdirs();
        int tw = GraftSkin.WIDTH * GraftSkin.TEXELS;
        int th = GraftSkin.HEIGHT * GraftSkin.TEXELS;
        BufferedImage hide = new BufferedImage(tw, th, BufferedImage.TYPE_INT_ARGB);
        BufferedImage glow = new BufferedImage(tw, th, BufferedImage.TYPE_INT_ARGB);
        for (GraftSkin.Box box : GraftSkin.Box.values()) {
            for (GraftSkin.Face face : GraftSkin.Face.values()) {
                double[] r = GraftSkin.rect(box, face);
                double[] n = GraftSkin.normal(face);
                int x0 = (int)Math.round(r[0] * GraftSkin.TEXELS);
                int y0 = (int)Math.round(r[1] * GraftSkin.TEXELS);
                int w = (int)Math.round(r[2] * GraftSkin.TEXELS);
                int h = (int)Math.round(r[3] * GraftSkin.TEXELS);
                for (int i = 0; i < w; i++) {
                    for (int j = 0; j < h; j++) {
                        double s = (i + 0.5) / GraftSkin.TEXELS;
                        double t = (j + 0.5) / GraftSkin.TEXELS;
                        double[] p = GraftSkin.point(box, face, s, t, 0.0);
                        hide.setRGB(x0 + i, y0 + j, 0xFF000000 | GraftSkin.paint(box, p, n, false));
                        glow.setRGB(x0 + i, y0 + j, GraftSkin.paint(box, p, n, true));
                    }
                }
            }
        }
        ImageIO.write(hide, "png", new File(dir, "graft_kakuja.png"));
        ImageIO.write(glow, "png", new File(dir, "graft_kakuja_glow.png"));
        System.out.println("wrote " + new File(dir, "graft_kakuja.png") + " and its glow");
    }
}
