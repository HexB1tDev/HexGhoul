import com.hex.kagune.control.Gender;
import com.hex.kagune.control.family.*;
import java.awt.image.BufferedImage;
import java.io.File;
import javax.imageio.ImageIO;

/**
 * 1.3.10: child skins off-game, face on (as the model shows them): girls on the top row, boys under, four made-up couples, stages
 * across. Run from the repo root after a build: java -Djava.awt.headless=true -cp build/classes tools/ChildSkinPreview.java <out-dir>
 */
public class ChildSkinPreview {
    static int[] load(String n) {
        try (java.io.InputStream in = new java.io.FileInputStream("src/main/resources/assets/hexkagune/textures/entity/child/" + n + ".png")) {
            return Png.read(in).pixels();
        } catch (Exception e) { return null; }
    }

    static void blit(BufferedImage out, int[] px, int u, int v, int w, int h, int x, int y, int k) {
        for (int j = 0; j < h * k; j++) for (int i = 0; i < w * k; i++) {
            int p = px[(v + j / k) * 64 + u + i / k];
            if ((p >>> 24) != 0) out.setRGB(x + i, y + j, p);
        }
    }

    public static void main(String[] args) throws Exception {
        int k = 6;
        int cell = 18 * k;
        Genome.Parent[][] couples = {
            {new Genome.Parent(0x78C0A8, 0x3E333D, 0xEBE0E0, true, true), new Genome.Parent(0x5A3A22, 0x4A3020, 0xD9A07E, false, false)},
            {new Genome.Parent(0x6B4423, 0x5A3A1E, 0xE0B090, false, false), new Genome.Parent(0x3B2A1E, 0x3A6EA5, 0xD9A07E, true, false)},
            {new Genome.Parent(0xD8B45A, 0x3A6EA5, 0xF0D0B8, false, false), new Genome.Parent(0x7A4E2A, 0x4A3020, 0xE0B090, false, false)},
            {new Genome.Parent(0x1E1A1A, 0x2A1E14, 0x8E5E42, true, true), new Genome.Parent(0x2A1E18, 0x3A2A1A, 0xA87050, false, false)}};
        ChildStage[] stages = ChildStage.values();
        BufferedImage out = new BufferedImage(cell * 8, 2 * 36 * k, BufferedImage.TYPE_INT_ARGB);
        for (int y = 0; y < out.getHeight(); y++) for (int x = 0; x < out.getWidth(); x++) out.setRGB(x, y, 0xFF2A2A2A);
        java.util.Random r = new java.util.Random(11);
        for (int row = 0; row < 2; row++) {
            for (int col = 0; col < 8; col++) {
                Genome.Parent[] c = couples[col / 2];
                Genome g;
                do { g = Genome.of(c[0], c[1], r); } while (g.gender() != (row == 0 ? Gender.FEMALE : Gender.MALE));
                ChildStage stage = stages[(col % 2) * 2 + (row == 0 ? 1 : 0) == 0 ? 0 : ((col % 2) * 2 + 1 + row) % 4];
                stage = stages[(col + row) % 4];
                int[] px = ChildSkin.paint(g, stage, false, ChildSkinPreview::load);
                int x0 = col * cell + 3 * k;
                int y0 = row * 36 * k + 2 * k;
                blit(out, px, 8, 8, 8, 8, x0 + 3 * k, y0, k); blit(out, px, 40, 8, 8, 8, x0 + 3 * k, y0, k);
                blit(out, px, 20, 20, 8, 12, x0 + 3 * k, y0 + 8 * k, k);
                blit(out, px, 44, 20, 4, 12, x0 - k, y0 + 8 * k, k);
                blit(out, px, 36, 52, 4, 12, x0 + 11 * k, y0 + 8 * k, k);
                blit(out, px, 4, 20, 4, 12, x0 + 3 * k, y0 + 20 * k, k);
                blit(out, px, 20, 52, 4, 12, x0 + 7 * k, y0 + 20 * k, k);
            }
        }
        ImageIO.write(out, "png", new File(args[0], "child-skins.png"));
    }
}
