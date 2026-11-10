# Next AI: 4.1.6, Kick Master (in progress)

Read this first. The house rules are in `next-ai-4.1.2.md` and `next-ai-4.0.0.md` (they still hold): answer in English, quote the owner
exactly, never claim in-game testing, the full `python3 build.py` must end with EXIT 0 on the exact commit before it is pushed, never
alter the owner's files, one version at a time pushed by SHA, no PR.

## Where things stand

| Version | What | State |
|---|---|---|
| 4.1.2 | Ukaku wing slams and wing guard | pushed (4b4e176), EXIT 0 |
| 4.1.3 | Fighting styles, Speedster from Ayato | pushed (cdf6fc4), EXIT 0 |
| 4.1.4 | The CCG building, 38 investigators | pushed (e31bd75), EXIT 0 |
| 4.1.5 | The General (Persona GENERAL) | pushed (7ab4526), EXIT 0 |
| 4.1.6 | Kick Master | started: `docs/handoff/kick-master-4.1.6-wip.patch` (not built, not tested) |

Branch: `claude/modest-volta-44ltps`. Each section "4.1.x (released)" in `next-ai-4.0.0.md` says what that version changed.

## The owner's newest words (quoted exactly)

"after that please put all your focus into the combat rework, and kagune rework for the ukaku, this model is very expensive and I need
you to focus on what's hard, study complex fighting moves, and combos and make it look smooth and beautiful."

And of Kick Master (from `next-ai-4.1.1.md`): "He teaches a new combat style mixing kicks and punches (mostly side kicks, spinning kicks
etc), he only teaches the player this through quests if the player is either a human or wearing a full mask ... Do a very deep research
into combat moves like I described make sure the corehography is perfect, this combat style upon learned fully replaces the default M1
combos. Right click is a chargeable teleport speed kick (dash to an oponent at high speed and side kick with a side twirl. New fighting
style called: \"Kick Master\" Make up the quests yourself make sure they're creative and beautful."

## The tools that let you SEE a move (use them; do not choreograph blind)

- `tools/PalePoseDesign.java` + `tools/pale-pose-design.sh`: the choreography of every `PaleStrike`, written as what the body does and
  where the fists go; it solves the arms and writes `PaleStrikePoses.java` (generated: never edit it). It needs `build/classes`,
  `build/stubs` and `build/base.jar` (copy them from a full build's `build/` folder). Run unchanged, it reproduces the committed file.
- `tools/PalePreview.java` + `tools/pale-preview.sh <out> <sheet>`: the exact body, rendered off-game. `blitz` is the Speed Blitz's sheet.
  The patch adds `kicks` (Kick Master, four frames a kick, big) and `kick-test` (bare leg poses, to learn the rig).

## What the patch holds

- `PaleStrike`: a `Style` (BLITZ, KICKS) and Kick Master's eight strikes appended: KICK_JAB, KICK_CROSS, ROUND_KICK, SIDE_KICK, BACK_KICK,
  KICK_HOOK, HOOK_KICK, TORNADO_KICK (the finisher). `of(style)`, `next(previous, mastery, style)`, `count(mastery, style)`, `place()`,
  `kick()`, `lift(age)` (the tornado kick's jump). The order is a kicker's combination: each blow leaves the body where the next starts
  (the comment over the entries says how).
- `PaleMotions.strike`: the jump's lift. `EtoOwl`, `PaleMenuView`: the blitz's strikes only.
- `PalePoseDesign`: `roundKick`, `sideKick`, `backKick`, `hookKick`, `tornadoKick` (the punches reuse `straight` and `hook`), with the
  grammar of a kick written over them (chamber, pivot, extension, re-chamber).
- `PalePreview`: the two sheets.
- `PaleStrikePoses.java`: regenerated (the blitz's keys unchanged).

## What is wrong with the kicks as they are (seen in the render)

- The punches are right.
- **The kicking legs land low and short.** The trunk now leans away (a NEGATIVE `roll`: a positive one tips her head at the target), but
  the side kick's and the round kick's legs point well below level and do not reach the dummy.
- I had assumed "the leg is level when its Z less the roll is a quarter turn". The render does not bear that out. **Measure it**: print
  the foot's position with `BodyFrame.world(p, BodyFrame.model(p, "rightLeg", ...))` for a few (legZ, roll, spin) and solve for a heel at
  the target (about x 0, y 0.95 to 1.2, z 1.4 in the move's frame), then set each kick's impact key from that. Best: add a leg solver to
  the designer (a heel target, as the fists have), so kicks are written as where the heel goes.
- The back kick's trunk and leg are not yet a level T; the hook kick's leg should be level and a little past the target before it hooks.
- `kick-test` showed: `legZ` +1.5 alone puts the right leg out level to her right; with `spin` 1.57 that points at the target.

## Left to do for 4.1.6

1. Fix the kicks (above), and look at every frame of the `kicks` sheet until each reads as its kick.
2. `FightStyle.KICK_MASTER` (gate `STYLE_KICK_MASTER`, appended); `FightStyle.of` prefers it once open (the owner: "fully replaces the
   default M1 combos"). `PaleBlitz.start`: `PaleStrike.next(previous, mastery, style)` by `PaleServer.style(player)`.
3. A human has no kakugan mode, and the General teaches humans: the eye-mode fight (`PaleServer.eyes`, `PaleClient.eyes`) must also be on
   for a human with a style and an empty hand.
4. `Teaching`: the General's lesson (teacher `general`), for a human or a ghoul in a full mask (`Sightings.masked`), with quests of his
   own ("Make up the quests yourself make sure they're creative and beautful"). `GeneralScript.LESSONS` only refuses for now.
5. The General fights with it (`EtoOwl` picks his strikes from `Style.KICKS`).
6. Right click: the chargeable teleport speed kick with a side twirl (its own version is fine: 4.1.7). The Speed Kick (`PaleMoves.kick`,
   `PalePoseDesign.kick`) is the nearest thing to start from.
7. Version, notes, README, handoff section, protocol 115, full build, push.

## Then

The Ukaku rework's look: the wing slams (`WingRig` motions SLAP, SWING, RISE, SLAM, TWIN_SLAM, GUARD; `tools/WingPreview.java` renders the
wings). The owner wants them "smooth and beautiful": render each, and give them the same chamber, drive, overshoot and recoil.
