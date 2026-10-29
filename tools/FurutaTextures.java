import com.hex.kagune.control.furuta.FurutaSkin;
import java.awt.image.BufferedImage;
import java.io.File;
import javax.imageio.ImageIO;

/**
 * Paints the kakuja's hide (7.0.0): every texel of {@link FurutaSkin}'s layout gets the paint of the point of the body it lands on,
 * so the whorls and slashes run on across the edges of the boxes. Writes {@code furuta_kakuja.png} and its glow
 * ({@code furuta_kakuja_glow.png}) into the given directory (the mod's textures/entity).
 * Usage: {@code java -cp <classes> FurutaTextures src/main/resources/assets/hexkagune/textures/entity}
 */
public final class FurutaTextures {
    private FurutaTextures() {
    }

    public static void main(String[] args) throws Exception {
        File dir = new File(args.length > 0 ? args[0] : "build/preview");
        dir.mkdirs();
        int tw = FurutaSkin.WIDTH * FurutaSkin.TEXELS;
        int th = FurutaSkin.HEIGHT * FurutaSkin.TEXELS;
        BufferedImage hide = new BufferedImage(tw, th, BufferedImage.TYPE_INT_ARGB);
        BufferedImage glow = new BufferedImage(tw, th, BufferedImage.TYPE_INT_ARGB);
        for (FurutaSkin.Box box : FurutaSkin.Box.values()) {
            for (FurutaSkin.Face face : FurutaSkin.Face.values()) {
                double[] r = FurutaSkin.rect(box, face);
                double[] n = FurutaSkin.normal(face);
                int x0 = (int)Math.round(r[0] * FurutaSkin.TEXELS);
                int y0 = (int)Math.round(r[1] * FurutaSkin.TEXELS);
                int w = (int)Math.round(r[2] * FurutaSkin.TEXELS);
                int h = (int)Math.round(r[3] * FurutaSkin.TEXELS);
                for (int i = 0; i < w; i++) {
                    for (int j = 0; j < h; j++) {
                        double s = (i + 0.5) / FurutaSkin.TEXELS;
                        double t = (j + 0.5) / FurutaSkin.TEXELS;
                        double[] p = FurutaSkin.point(box, face, s, t, 0.0);
                        hide.setRGB(x0 + i, y0 + j, 0xFF000000 | FurutaSkin.paint(box, p, n, false));
                        glow.setRGB(x0 + i, y0 + j, FurutaSkin.paint(box, p, n, true));
                    }
                }
            }
        }
        ImageIO.write(hide, "png", new File(dir, "furuta_kakuja.png"));
        ImageIO.write(glow, "png", new File(dir, "furuta_kakuja_glow.png"));
        System.out.println("wrote " + new File(dir, "furuta_kakuja.png") + " and its glow");
    }
}
