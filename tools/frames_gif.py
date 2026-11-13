"""4.1.74: frames written one per world tick (frame-NNN.png, ReaperBladePreview shedgif/endgif) into a GIF at real speed (20 a second).

    python3 tools/frames_gif.py <frames dir> <out.gif> [scale]
"""
import sys
from pathlib import Path

from PIL import Image


def main():
    frames_dir, out = Path(sys.argv[1]), Path(sys.argv[2])
    scale = float(sys.argv[3]) if len(sys.argv) > 3 else 1.0
    frames = []
    for path in sorted(frames_dir.glob("frame-*.png")):
        img = Image.open(path).convert("RGB")
        if scale != 1.0:
            img = img.resize((round(img.width * scale), round(img.height * scale)), Image.LANCZOS)
        frames.append(img.quantize(colors=128, method=Image.Quantize.MEDIANCUT))
    if not frames:
        raise SystemExit(f"no frames in {frames_dir}")
    # 50 ms a frame: a world tick each (GIF delays are hundredths: 5, exact).
    frames[0].save(out, save_all=True, append_images=frames[1:], duration=50, loop=0, optimize=True)
    print(f"wrote {out}: {len(frames)} frames, {len(frames) / 20.0:.2f} s")


if __name__ == "__main__":
    main()
