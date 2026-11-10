# Next AI: after 4.1.7 (Kick Master)

Read this first. The house rules are in `next-ai-4.1.2.md` and `next-ai-4.0.0.md` (they still hold): answer in English, quote the owner
exactly, never claim in-game testing, the full `python3 build.py` must end with EXIT 0 on the exact commit before it is pushed, never
alter the owner's files, one version at a time, no PR.

## Where things stand

| Version | What | State |
|---|---|---|
| 4.1.5 | The General | pushed, EXIT 0 |
| 4.1.6 | The hideout and the CCG building always placed (the owner: "also I cant get the aogiri hideout to spawn or the ccg building at all") | pushed, EXIT 0 |
| 4.1.7 | Kick Master: the string, the General's three watches, humans fight with it, the General uses it | pushed, EXIT 0 |
| 4.1.8 | Kick Master's right click: the twirling speed kick, charged on the hold | pushed, EXIT 0 |

Branch: `claude/next-ai-4-1-6-handoff-sqv9e4` (fast-forwarded from `claude/modest-volta-44ltps`, which stops at the handoff after 4.1.5).
Each "4.1.x (released)" section in `next-ai-4.0.0.md` says what that version changed.

## Next, in this order

1. ~~4.1.8: Kick Master's right click~~ (done: "4.1.8 (released)" in `next-ai-4.0.0.md`).
2. **Then the owner's newest ask, quoted exactly:** "after that please put all your focus into the combat rework, and kagune rework for the
   ukaku, this model is very expensive and I need you to focus on what's hard, study complex fighting moves, and combos and make it look smooth
   and beautiful." The Ukaku wing slams (`WingRig` motions SLAP, SWING, RISE, SLAM, TWIN_SLAM, GUARD; `tools/WingPreview.java`): render each, and
   give them a chamber, a drive, an overshoot and a recoil.

## The tools that let you see a move (use them)

- `tools/PalePoseDesign.java` + `tools/pale-pose-design.sh`: every `PaleStrike` (and the Speed Kick, Big Punch...) written as what the body does
  and where the fists go (arms solved) and, since 4.1.7, where the right sole goes (`Spec.sole(x, y, z)`: the leg's swing and swing-out solved,
  with the lunge, 0 to 8 px, so the straight leg reaches). It prints each solved key ("off" is the miss in blocks). It writes
  `PaleStrikePoses.java` (generated: never edit it). It needs `build/classes`, `build/stubs`, `build/base.jar` from a full build.
- `tools/pale-preview.sh <out> kicks kicks-film`: `kick-master.png` (every kick from the side, key by key, and its impact from 3/4) and
  `kick-master-film.png` + `kick-master.gif` (the whole string chained as the server chains it, a frame a tick). The dummy stands where
  the server stands her for a kick (`PaleRules.comboKickStand`).

## What the rig taught (measured, 4.1.7)

- A whole-body `spin` of +pi/2 (to her left) puts her right side to the target. Side-on, `roll` turns her about the line to the target, so it
  only bows her forward or back. Lay the trunk away from a side-on kick with `side` (+ to her left), never with `roll`.
- A model leg is 0.70 blocks long from a hip 0.70 up: a straight leg reaches chest height at most. Reach is the lunge (at most 8 px, 0.5
  blocks) plus the leg.
- `RigPose.settle` caps: `side` 0.85, drop -10..4 (negative is up), spin +-4 pi, lunge +-8, `rightLegZ` 2.2.
- `PaleTests.strikes` sweeps every strike every 0.05 tick from the guard and from the strike before it (`starts`), and wants the arms
  0 px into the torso or the head. An arm thrown about the body as a kick's counterweight passed through her side in the blends as the trunk
  leaned, and the head (turned to keep its eyes on the target, up to 1.3) swept its corner through a fist held at the jaw. So every kick key
  holds the kicking guard (`Spec.kickArms`: `KICK_GUARD`, fists lower and further out than the guard, held as the arms' own angles, so they
  turn with the trunk), and the back kick's head turns 1.0. A quick check of just the kicks: copy `PaleTests.strikes`' loop over
  `PaleStrike.of(Style.KICKS)` into a scratch class (it takes a minute; the whole `PaleTests` takes about fifteen).
- `PaleMotions.strike` grounds every strike (lowered till the lower foot, tucked or not, touches) and then lifts the tornado kick by
  `PaleStrike.lift`; so its keys are designed grounded (no `airborne()`), and a target in the air is given less the lift (`lifted`). Keep a
  grounded strike's lower-foot drop at 4 px or less (the cap).
- `PaleTests`: the lower foot stays on the ground except by exactly the lift; "she sees her blow land" is for punches only.
