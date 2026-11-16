# HANDOFF: HexGhoul after 1.3.38. Read this first, then `docs/handoff/next-ai-1.3.33.md` (house rules, how the owner works).

Branch **`claude/relaxed-carson-4y0frw`**. Never push to `main`; no PR unless asked. Version **1.3.38** in `build.py` and
`patch/KagunePatch.java` (bump both; the next is 1.3.39). Network `PROTOCOL` is **"67"** (1.3.36). Nothing since 1.3.28 has been
tried in game: never say anything works in game. Commit trailers: `Co-Authored-By: Claude <noreply@anthropic.com>` and the
session's `Claude-Session:` line, no model names anywhere. Answer the owner in English only, even when they write in another language.

## What this session shipped (details: top sections of `docs/handoff/companions-handoff.md`)

- **1.3.33** CCG and quinques (the old section C): the launcher rebuilt as a gun (axis-aligned model, rifle hold, aim pitch, double
  shot per pull = 10/s); every hold solved by `tools/quinque_models.py`, which writes `QuinqueHolds.java`; the left hand on its grip
  every frame (`QuinqueGrip.reach`, through vanilla's item chain; the spear's front hand slides); new keyed swings for every quinque
  (`QuinquePose.keys`, Catmull-Rom); the one-handed blade held 50 degrees off the forearm (the owner flagged "who grabs a sword by the
  tip"); right-click tap folds a melee quinque; investigators side-dash and dash in, with brief afterimages. Preview:
  `tools/QuinquePreview.java <dir> [kind]` (stance + m1..m3 sheets, left grip/fist marked, misses printed).
- **1.3.34/1.3.35** random kagune colours (`KaguneColours`, `/kagune color [reroll]`), on every path: Ukaku's wings, crystals,
  shards and stuck spikes recoloured by `KaguneTint`; wild ghouls get a colour from their seed. `tools/WingPreview.java <dir> part-tint`.
- **1.3.36** the NPC-only arm blade (`NpcIdentity.Kind.KOUKAKU`, `BladeArm`, `BladeArmModel`, `BladeArmPose`,
  `GhoulNpcCombat.slash`), `tools/BladeArmPreview.java`.
- **1.3.37** sleeping family: drowsy half-waking (`family/Drowsy`, EtoServer's drowsy page, ChildTalk), investigators as house threats.
- **1.3.38** real speech bubbles and thought clouds (`BubbleShape`, `SpeechBubbles`), "z Z z" over sleepers.

## What's left / worth checking

- Everything above is untested in game: expect the owner's reports first (fix them first, each its own version).
- Likely spots: the launcher's first-person placement (`FIRST` in quinque_models.py, judged from a projection only); bubble size and
  depth in game (`SpeechBubbles.SCALE`, the depths in `BubbleShape`); whether the arm blade ghoul's wrap and guard read at game
  distance; the drowsy page when the player's own bed is the double bed's other half.
- The owner's older "companion life" ideas not yet built (from next-ai-1.3.33.md, section B): feeding the kids, a pantry chest for
  the harvest, tending animals, lighting the house at dusk, repairs, conversations that recall what she did ("I smelted your iron").
