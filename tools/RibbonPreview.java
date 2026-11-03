import com.hex.kagune.control.RinkakuRibbon;
import java.awt.image.BufferedImage;
public class RibbonPreview {
  static BufferedImage tex;
  public static void main(String[] a) throws Exception {
    tex = javax.imageio.ImageIO.read(new java.io.File(a[0]));
    int[] colours = {0xA50F37, 0x1E50D8, 0x7A1FB8, 0x1F9E4A, 0x141414, 0xE8E8E8};
    String[] names = {"default crimson", "blue", "purple", "green", "black", "white"};
    int W = 520, H = 460;
    BufferedImage sheet = new BufferedImage(W * 3, H * 2, BufferedImage.TYPE_INT_RGB);
    for (int c = 0; c < colours.length; c++) {
      BufferedImage img = render(colours[c], W, H);
      java.awt.Graphics2D g = sheet.createGraphics(); g.drawImage(img, (c % 3) * W, (c / 3) * H, null);
      g.setColor(java.awt.Color.BLACK); g.drawString(names[c], (c % 3) * W + 8, (c / 3) * H + 16); g.dispose();
    }
    javax.imageio.ImageIO.write(sheet, "png", new java.io.File(a[1]));
    javax.imageio.ImageIO.write(render(colours[0], 1100, 900), "png", new java.io.File(a[2]));
  }
  static double[][] tail(int which) {
    int n = 40; double[][] p = new double[n][];
    double sx = which % 2 == 0 ? -1 : 1; boolean up = which < 2;
    for (int i = 0; i < n; i++) { double t = i / (double)(n - 1);
      double ang = up ? (0.25 + 2.6 * t) : (-0.35 - 2.2 * t);
      double rad = 0.4 + 1.1 * t;
      double x = sx * (0.2 + 1.6 * t + 0.4 * Math.cos(ang) * t);
      double y = (up ? 0.6 : -0.3) * t + Math.sin(ang) * 0.9 * t + (up ? 0.3 : 0) ;
      p[i] = new double[] {x, y, -0.25 - 0.5 * t};
    }
    return p;
  }
  static BufferedImage render(int colour, int W, int H) {
    BufferedImage img = new BufferedImage(W, H, BufferedImage.TYPE_INT_RGB);
    double[] zb = new double[W * H]; java.util.Arrays.fill(zb, -1e9);
    for (int i = 0; i < W * H; i++) img.setRGB(i % W, i / W, 0xF2F2F2);
    double scale = W / 4.4, ox = W / 2.0, oy = H * 0.52;
    for (int which = 0; which < 4; which++) {
      RinkakuRibbon.Mesh m = RinkakuRibbon.build(tail(which), 0.28, colour, which * 131);
      double[][][] over = RinkakuRibbon.over(m);
      int R = m.at().length, K = RinkakuRibbon.SIDES;
      for (int pass = 0; pass < 2; pass++)
      for (int r = 0; r < R - 1; r++) for (int k = 0; k < K; k++) {
        int[] rs = {r, r + 1, r + 1, r}, ks = {k, k, k + 1, k + 1};
        double[][] P = new double[4][]; double[][] N = new double[4][]; int[] C = new int[4]; double[][] UV = new double[4][];
        for (int v = 0; v < 4; v++) { P[v] = (pass == 0 ? m.at() : over)[rs[v]][ks[v]]; N[v] = m.way()[rs[v]][ks[v]]; C[v] = pass == 0 ? m.tint()[rs[v]][ks[v]] : m.light();
          UV[v] = new double[] {RinkakuRibbon.u(ks[v]), RinkakuRibbon.v(m.along()[rs[v]])}; }
        tri(img, zb, W, H, scale, ox, oy, P, N, C, UV, new int[]{0,1,2}, pass);
        tri(img, zb, W, H, scale, ox, oy, P, N, C, UV, new int[]{0,2,3}, pass);
      }
    }
    return img;
  }
  static void tri(BufferedImage img, double[] zb, int W, int H, double s, double ox, double oy, double[][] P, double[][] N, int[] C, double[][] UV, int[] id, int pass) {
    double[] x = new double[3], y = new double[3], z = new double[3];
    for (int i = 0; i < 3; i++) { x[i] = ox + P[id[i]][0] * s; y[i] = oy - P[id[i]][1] * s; z[i] = -P[id[i]][2]; }
    int x0 = (int)Math.max(0, Math.floor(Math.min(x[0], Math.min(x[1], x[2])))), x1 = (int)Math.min(W - 1, Math.ceil(Math.max(x[0], Math.max(x[1], x[2]))));
    int y0 = (int)Math.max(0, Math.floor(Math.min(y[0], Math.min(y[1], y[2])))), y1 = (int)Math.min(H - 1, Math.ceil(Math.max(y[0], Math.max(y[1], y[2]))));
    double den = (y[1] - y[2]) * (x[0] - x[2]) + (x[2] - x[1]) * (y[0] - y[2]); if (Math.abs(den) < 1e-9) return;
    for (int py = y0; py <= y1; py++) for (int px = x0; px <= x1; px++) {
      double a = ((y[1] - y[2]) * (px - x[2]) + (x[2] - x[1]) * (py - y[2])) / den, b = ((y[2] - y[0]) * (px - x[2]) + (x[0] - x[2]) * (py - y[2])) / den, c = 1 - a - b;
      if (a < 0 || b < 0 || c < 0) continue;
      double zz = a * z[0] + b * z[1] + c * z[2]; int idx = py * W + px;
      if (zz < zb[idx] - (pass == 1 ? 0.02 : 0)) continue;
      double[] n = {a*N[id[0]][0]+b*N[id[1]][0]+c*N[id[2]][0], a*N[id[0]][1]+b*N[id[1]][1]+c*N[id[2]][1], a*N[id[0]][2]+b*N[id[1]][2]+c*N[id[2]][2]};
      double nl = Math.sqrt(n[0]*n[0]+n[1]*n[1]+n[2]*n[2]); double lit = 0.55 + 0.45 * Math.max(0, (n[0]*0.3 + n[1]*0.6 - n[2]*0.74) / nl);
      int col = pass == 0 ? lerp3(C[id[0]], C[id[1]], C[id[2]], a, b, c) : C[id[0]];
      if (pass == 1) {
        double u = a*UV[id[0]][0]+b*UV[id[1]][0]+c*UV[id[2]][0], v = a*UV[id[0]][1]+b*UV[id[1]][1]+c*UV[id[2]][1];
        int tx = Math.floorMod((int)Math.floor(u * tex.getWidth()), tex.getWidth()), ty = Math.floorMod((int)Math.floor(v * tex.getHeight()), tex.getHeight());
        int t = tex.getRGB(tx, ty); double al = (t >>> 24) / 255.0; if (al < 0.05) continue;
        int tr = ((t>>16)&255) * ((col>>16)&255) / 255, tg = ((t>>8)&255) * ((col>>8)&255) / 255, tb = (t&255) * (col&255) / 255;
        int under = img.getRGB(px, py);
        int rr = (int)(((under>>16)&255) * (1-al) + tr*lit*al), gg = (int)(((under>>8)&255) * (1-al) + tg*lit*al), bb = (int)((under&255) * (1-al) + tb*lit*al);
        img.setRGB(px, py, (clamp(rr) << 16) | (clamp(gg) << 8) | clamp(bb)); continue;
      }
      zb[idx] = zz;
      img.setRGB(px, py, (clamp((int)(((col>>16)&255)*lit)) << 16) | (clamp((int)(((col>>8)&255)*lit)) << 8) | clamp((int)((col&255)*lit)));
    }
  }
  static int clamp(int v) { return Math.max(0, Math.min(255, v)); }
  static int lerp3(int p, int q, int r, double a, double b, double c) {
    int rr = (int)(((p>>16)&255)*a + ((q>>16)&255)*b + ((r>>16)&255)*c), gg = (int)(((p>>8)&255)*a + ((q>>8)&255)*b + ((r>>8)&255)*c), bb = (int)((p&255)*a + (q&255)*b + (r&255)*c);
    return (rr<<16)|(gg<<8)|bb; }
}
