#!/usr/bin/env python3
"""1.3.14: the companions' gestures as animated GIFs, from the frames tools/CompanionPreview.java writes (`<out> frames`).

    python3 tools/companion-gifs.py <out-dir>
"""
import glob
import os
import sys

from PIL import Image

out = sys.argv[1] if len(sys.argv) > 1 else "build/preview"
for key in ("touka", "ken"):
    names = sorted(glob.glob(os.path.join(out, key + "-frames", "frame_*.png")))
    if not names:
        continue
    frames = [Image.open(n).convert("P", palette=Image.ADAPTIVE, colors=128) for n in names]
    frames[0].save(os.path.join(out, key + "-animations.gif"), save_all=True, append_images=frames[1:], duration=100, loop=0, optimize=True)
    print(key, len(frames), "frames")
