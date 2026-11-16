# Next AI: 4.1.2 is ready to version and ship

Read this first. The house rules and history are in `docs/handoff/next-ai-4.0.0.md` and `docs/handoff/next-ai-4.1.1.md` (they still
hold). The short version:

- Answer the owner in English and quote their words exactly.
- Never claim in-game testing ("Not tested in game").
- The full `python3 build.py` must end with EXIT 0 on the exact commit before it is pushed.
- Never alter the owner's files (`art/`).
- `verify/api-allowlist.txt` is exact. Each new Minecraft or Forge reference needs evidence from `build/mapping-cache/joined.tsrg` or the
  intermediary file.
- One version at a time, pushed by SHA. No PR.
- Commit trailers: the ones your own session gives you. No model names anywhere else in pushed content.

## The branch

- The newest work is on **`claude/nice-allen-y6r0pb`** (this session's branch). It was fast-forwarded from
  `claude/tender-brahmagupta-yhr1ga`, which stops at the handoff after 4.1.0.
- `main` is still at 1.3.0. A new session's own branch usually starts there. If yours does, fast-forward it first:
  `git fetch origin claude/nice-allen-y6r0pb && git merge --ff-only origin/claude/nice-allen-y6r0pb`.
- Push to the branch your session names. Both workflows run on any `claude/**` branch; a new `.github/release-notes-<ver>.md` publishes
  the release.

## How to build

- **Full build, isolated** (long, and `PaleTests` prints nothing for a long while: start it in the background and use the quick loop meanwhile):
  1. `git archive <sha> | tar -x -C <scratch>/fullbuild`
  2. In that folder: `python3 build.py > ../fullbuild.log 2>&1; echo "EXIT $?" >> ../fullbuild.log`
  3. Grep the log for `FAILED`. The build stops at the first failing suite.
- **Quick loop:** `python3 tools/quick_check.py [--noverify] SuiteName ...`. It needs `build/base.jar`, `build/patch-classes` and
  `build/awakening-srg`: copy them from a full build's `build/` folder (they appear a few minutes into it, before the suites).
- **Protocol:** `ControlNetwork.PROTOCOL`, checked in `BikakuWormTests` (search `PROTOCOL = \"`).

## Where things stand

| Version | What | State |
|---|---|---|
| 4.1.0 | Ayato's pull-out | pushed (121a679) |
| 4.1.1 | No structure in water | pushed (eda98fb); its full build ended with EXIT 0 (42 suites) |
| 4.1.2 | Ukaku wing slams and wing guard | code and tests done, not versioned: `docs/handoff/ukaku-wings-4.1.2-wip.patch` |

## 4.1.2: what is left (short)

The owner's words and the design are in `next-ai-4.1.1.md`. Apply the patch: `git apply
docs/handoff/ukaku-wings-4.1.2-wip.patch`, and delete the patch file in the 4.1.2 commit.

### In the patch now (everything in the old patch, plus this session's work)

- **Tests:** `UkakuTests.wingSlams()` (called after `crystalMode()`), with a `read(path)` helper for the source checks. It covers every
  point of the old "Left to do" item 2.
  - The wide swing carries its wing across the middle to the other side, so its check is "crosses the whole front" and "mirrored", not
    "stays on its own side".
- **The guard grows:** `WingRig.GUARD_GROWTH` 0.3 (public): the guarding wings are up to 30% longer as they close (the owner: "wings grow
  and protect the player"). `WingRig.striking` is public for the tests.
- **Network:** `ComboStyle` ordinals are sent (`ControlServer.snapshot` to `ControlView`, `ComboStyle.byOrdinal`), so the protocol is
  **111** in `ControlNetwork` and in `BikakuWormTests`.
- **Checked:** the server already refuses an ender for the slam styles (`KaguneCombat.startEnder`: `ukakuShots()` is false for them), so
  the base form's enders and Fire Dash can't be started by an old client either.
- **Run with the patch on 4.1.1:** `UkakuTests` (39470 checks), `ClashTests`, `ControlTests` and `BikakuWormTests` pass; the API verifier
  and the name check pass. The full build has **not** been run on it.

### Left to do

1. Version 4.1.2: `build.py` (`VERSION`), `patch/KagunePatch.java` (`VERSION`), `.github/release-notes-4.1.2.md`, a README section, and a
   "4.1.2 (released)" section in `next-ai-4.0.0.md`. Follow 4.1.1's commit for the shape of each.
   - Say in the notes that the base form's fire enders (Rabbit Hop, Flare, Rising Flame, Fire Fan, Rabbit Dash, Black Rabbit) and the
     W + right-click Fire Dash are gone, because M2 is now the guard.
   - Crystal mode keeps its shots, its enders and Fire Dash.
   - Rabbit Guise's one wing slams and guards too.
   - The mutations that only change the fire's shots or enders do nothing in the base form now. Check `UkakuMutation` and say which.
2. Full build, EXIT 0, then push.

## Then, in this order, one version each

The owner's words for these are quoted exactly in `next-ai-4.1.1.md` ("Then, from the owner's big message").

1. **Fighting styles:** Speedster (the present M1 and M2 flips), unlocked by a new quest with Ayato. Until a style is unlocked, only
   default punches and the default right click in kakugan mode.
2. **The CCG building:** `art/exploration/ccg_building.schem` (39×68×39), one per world, placed as the hideout is (`DryLand` offers plus
   `choose`, on dry land). Agents guard, patrol and sit in the offices.
3. **The General:** an official character (respawns), a male adult skin changed to be original, a little muscle (less than the player's),
   talk and romance (women players only; tough, softens, easily embarrassed, proud of the CCG, has to be convinced to love a ghoul).
4. **Kick Master:** his style, taught by quests only to a human or a player in a full mask. It replaces the default M1 combos (side kicks,
   spinning kicks, mixed with punches); right click is a chargeable teleport speed kick. Research the choreography before writing it.

Nothing of these four is started.

## The prompt for the next session

> Read CLAUDE.md, then docs/handoff/next-ai-4.1.2.md and follow it. The newest work is on origin/claude/nice-allen-y6r0pb: if your
> branch doesn't contain it, fast-forward to it first, and tell me which commit you start from. Finish 4.1.2 (the Ukaku wing slams and wing guard): apply
> docs/handoff/ukaku-wings-4.1.2-wip.patch, do the "Left to do" list, run the full build to EXIT 0 and push. After that, do the fighting
> styles (Speedster via a new Ayato quest), then the CCG building, the General and Kick Master, in that order, one version at a time.
