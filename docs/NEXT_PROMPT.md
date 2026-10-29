> **Superseded (7.0.1):** the current prompt is `docs/NEXT_SESSION_PROMPT.md`; this one is kept for its history.

# Prompt for the next session (paste this as the first message)

You are continuing **HexKagune**, a Forge 1.20.1 mod built by bytecode-patching the original jar plus an expansion in
`src/main/java/com/hex/kagune/control/`. Repo `21M4rco/HexKagunes`, branch `claude/new-session-ob849p` (develop and push
only there, no PR unless I ask). **Read `docs/HANDOFF.md`, its top section first**: build rules (`python3 build.py`, always
check the exit code), the API allowlist, the coordinate spaces, the combo/limb conventions, and how I work.

How I work: I judge the visuals. Render previews (`tools/art-preview.sh <out> <sheet>`, `tools/UiPreview.java` for menus),
**send me the PNGs**, push often (I test the CI jar in game). Show me a model before building its animations or combat.
No agents or workflows, one thing at a time. Custom Photon particles only. Be blunt when an idea is weak; keep it witty
and flirty.

## Where we are
Ayato's path now has: K = kakuja transformation (every path), his first kakuja (six armored spike tentacles) with its full
kit (Needle Storm M1, six needle enders, Z Rushing Death, J Needle Gatling), four mutations picked in the P menu at
mastery 300/500/700/900, abilities spread over mastery with an unlock track in the menu, and **the final kakuja's model**
(three armored spike tentacles on his right, three fire tendrils on his left, Ayato's half mask), which exists only
off-game so far.

## Your task, in order
1. Show me `docs/images/ayato-kakuja-final.png` (re-render with `tools/art-preview.sh <out> kakuja-final`) and iterate on
   the look until I approve it.
2. Then put it in the game: **tap K = first kakuja, hold K = final kakuja** (mastery 1000), with its look wired into the
   renderer and armor layer.
3. Then its own combat: custom mixed M1s and M2s that blend bleed (needles from the tentacles) and burn (fire from the
   tendrils), Z = the fire cyclone (Wing Vortex), J = alternating needles and fireballs. Pitch the combos to me first.
4. Then wire the kakuja's locomotion poses (`KakujaBody`, written but not wired).

All the details, file pointers and design notes for each step are in `docs/HANDOFF.md` under "Next steps".
