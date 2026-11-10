# Next AI: after 4.1.1

> **Superseded for the state of things by `next-ai-4.1.2.md`** (4.1.1 is pushed; the 4.1.2 patch is further along). The rules and the owner's words below still hold, except the branch: see the newer file.

Read this first. The full house rules and history are in `docs/handoff/next-ai-4.0.0.md` (they still hold). The short version:

- Answer the owner in English and quote their words exactly.
- Never claim in-game testing ("Not tested in game").
- The full `python3 build.py` must end with EXIT 0 on the exact commit before it is pushed.
- Never alter the owner's files (`art/`).
- `verify/api-allowlist.txt` is exact. Each new Minecraft or Forge reference needs evidence from `build/mapping-cache/joined.tsrg` or the intermediary file.
- Push only to `claude/tender-brahmagupta-yhr1ga`, one version at a time, by SHA. No PR.
- Commit trailers: `Co-Authored-By: Claude Opus 5.5 <noreply@anthropic.com>` and `Claude-Session: ...`.
- No model names in pushed content.

## How to build

- **Quick loop:** `python3 tools/quick_check.py [--noverify] SuiteName ...` (needs one full build first, so that `build/base.jar` exists).
- **Full build, isolated:**
  1. `git archive <sha> | tar -x -C <scratch>/fullbuild`
  2. In that folder: `python3 build.py > ../fullbuild.log 2>&1; echo "EXIT $?" >> ../fullbuild.log`
  3. Grep the log for `FAILED`. The build stops at the first failing suite, so later suites don't run.
- **Push:** `git push origin <sha>:refs/heads/claude/tender-brahmagupta-yhr1ga`. Adding `.github/release-notes-<ver>.md` triggers the release workflow.
- **Protocol:** `ControlNetwork.PROTOCOL`, checked in `BikakuWormTests` (~line 1844).

## Where things stand

| Version | What | State |
|---|---|---|
| 4.0.9 | Ken's tower, Ken sits | pushed (7cda697) |
| 4.1.0 | Ayato's pull-out | pushed (121a679) |
| 4.1.1 | No structure in water | built but maybe not pushed: if no commit "HexKagune 4.1.1" is on the branch, run `git am docs/handoff/water-4.1.1.patch`, delete that patch file, run the full build to EXIT 0, then push. Its first build was still running at handoff, with no failures through OutfitTests. |
| 4.1.2 | Ukaku wing slams and wing guard | unfinished: `docs/handoff/ukaku-wings-4.1.2-wip.patch` |

### 4.1.1 (the owner: "bug, make sure NONE of the structures spawns in water or collides with water bodies, the mansion is currently happily resting at the very bottom of the ocean...")

- **`village/DryLand`** (pure, tested in `NpcTests.dryLand`):
  - Checks the footprint plus 8 blocks round it.
  - The `hexkagune:wet` biome tag every 4 blocks; water over the ground (WORLD_SURFACE_WG > OCEAN_FLOOR_WG) every 16.
  - Also holds the hideout's spread offers and its chooser (`choose`).
- **The hideout:** its structure set is `random_spread` 40/20, with salt 1873602243. `ControlStructures.Site.hideout(context)` builds it only at the first dry offer, cached per seed and generator.
- **The villages:** try 9 spots, 32 blocks apart.
- **New type `hexkagune:dry_place`** (Anteiku, the library, the cyber tower): runs the vanilla jigsaw in a `GenerationContext` moved up to 5 chunks, keeping the pieces' box within the start chunk's reach.
- **Protocol 110.**

## 4.1.2 (unfinished): the Ukaku's fiery base form

The owner, quoted exactly: "Another rework: Ukaku kagune base firery form no longer fires projectiles, M1s are a series of different wing slams combos. (create highly detailed moveset switching between left wing and right wing slaps, swings, upwards to downwards slams (These can now clash like Rinkaku) and has lightly less range than Rinkaku. Right click (M2) is now a protection like Rinkaku has, wings grow and protect the player , they come together to protect the player, just like rinkaku they break after a bunch of hits, perfectly timed right clicks can do the same as Rinkaku and stagger. Note: Crystal mode still has the same custom shooting crystals and right click, we're only touching the base from, everything else stays as it is."

Apply it: `git apply docs/handoff/ukaku-wings-4.1.2-wip.patch`, then delete the patch file in the 4.1.2 commit.

### Done (in the patch)

- **`StrikePattern`:** WING_SLAP, WING_SWING, WING_RISE, WING_SLAM and TWIN_SLAM, after BEHEADING. They are not flame, crystal or shot, so `KaguneCombat` registers them with `KaguneClash`.
- **`ComboStyle.UKAKU_WINGS`** (twin wings): 7 stages.
  1. Right Wing Slap
  2. Left Wing Slap
  3. Right Wing Swing (160°)
  4. Left Rising Wing
  5. Right Overhead Slam
  6. Left Overhead Slam
  7. Twin Slam
- **`ComboStyle.UKAKU_LEFT_WING`** (Rabbit Guise: a new Ukaku under mastery 100 has only the left wing): slap, swing, rise, slam, all left.
- **`UkakuRules`:**
  - `style`: the fire gives these two; crystal mode still gives UKAKU_CRYSTAL.
  - `SLAM_REACH 0.85`, `slams(style)`, `wingGuard(mastery, form, crystalMode)`.
- **`KaguneCombat.range`:** Rinkaku's reach × SLAM_REACH. `UkakuCombat` draws the arcs and `UkakuBody` the poses.
- **`WingRig`:**
  - Motions SLAP/SWING/RISE/SLAM, each _RIGHT and _LEFT, plus TWIN_SLAM and GUARD (`slam()`, `striking()`; GUARD_SWEEP -112, GUARD_ROLL 14).
  - The slam's strike roll was raised to -12 and the rise's wind-up to {-20, -32}, so the wings no longer sink into the ground (`UkakuTests.rig`).
- **`UkakuMotions`:** maps the patterns to the motions.
- **The guard:**
  - `UkakuClient.rigInput`: GUARD while `RinkakuBlockClient.amount > 0` and no blow is under way.
  - `RinkakuBlockServer`: `ukakuGuard`, and `can()` lets the Ukaku guard (wings IDLE, not stunned). Rinkaku's block rules apply: weight, break, perfect-timing stagger.
  - `RinkakuBlockClient.ready`: the Ukaku branch.
  - `UkakuInput.useInput`: returns early when `wingGuard`, so the off-hand click reaches `RinkakuBlockClient`.
- **`UkakuMenuView.comboName`:** names for the two new styles.
- **`UkakuTests`:** six old checks updated to the new styles. Not re-run since the last edit.

### Left to do

1. Run `UkakuTests`, `ClashTests` and `ControlTests`, and fix what fails.
2. Add a `wingSlams()` suite to `UkakuTests` (called after `crystalMode()`):
   - the twin string alternates right and left, then both;
   - the left-wing string uses only limb 1;
   - no stage is shot, flame, crystal, finalKakuja or ender (so all clash);
   - 0.75 ≤ SLAM_REACH < 1;
   - `wingGuard` is true for RABBIT and TWIN in the fire, and false in crystal mode, for ONE_WING and for the kakuja;
   - crystal mode still gives UKAKU_CRYSTAL, with shots;
   - the rig: GUARD at progress 1 has a larger max z (forward is +z) than at rest; each slam's forward-most corner is on its own side (RIGHT and LEFT mirrored).
   - Source checks on `UkakuInput`, `RinkakuBlockServer` and `UkakuClient`.
3. **Network:**
   - Two ComboStyle values were inserted after UKAKU_FIRE, which shifts the ordinals. Check whether ComboStyle or StrikePattern ordinals are sent (`ComboStyle.byOrdinal`), and bump the protocol to 111 either way. Update `BikakuWormTests`.
   - Check the tests that pin enum order.
4. Version 4.1.2: `build.py`, `patch/KagunePatch.java`, `.github/release-notes-4.1.2.md`, README and handoff.
   - Say in the notes that the base form's fire enders (Rabbit Hop, Flare, Rising Flame, Fire Fan, Rabbit Dash, Black Rabbit) and the W + right-click Fire Dash are gone, because M2 is now the guard.
   - Crystal mode keeps its enders and Fire Dash.
   - Rabbit Guise's one wing slams and guards too.
5. Full build, EXIT 0, then push.

## Then, from the owner's big message (quoted exactly)

- **Fighting styles:** "The default M1 fighting style is only unlocked through a new quest with ayato, until a fighting style is unlocked, the player can only use default punches in kakugan mode, right click is also default, the previous M1 and M2 flips are now all part of a fighting style called Speedster."
- **The CCG building:**
  - "CCG building, tons of ccg agents guard and patrol the area, sit in their offices, a new "general" there talks to you and can be romanced, use one of the male skins currently available (NOT CHILDREN skins) and change it a bit to make it original, give him some muscles also less than the player, he can be talked to, romanced and teaches a new combat style (and uses it in self defense) Like ayato, ken, touka, eto and noro, he is an official character, means he will respawn. He teaches a new combat style mixing kicks and punches (mostly side kicks, spinning kicks etc), he only teaches the player this through quests if the player is either a human or wearing a full mask. Attitude: Tough, softens up when romanced, embarrased easily, proud of the CCG, has to be convinced to love a ghoul, Only interested in woman players. Do a very deep research into combat moves like I described make sure the corehography is perfect, this combat style upon learned fully replaces the default M1 combos. Right click is a chargeable teleport speed kick (dash to an oponent at high speed and side kick with a side twirl. New fighting style called: "Kick Master" Make up the quests yourself make sure they're creative and beautful."
  - "Also I forgot to send you the CCG building, this is it, also like the others only 1 of them spawns." The file is `art/exploration/ccg_building.schem` (39×68×39).
  - Place it one per world with the hideout's approach (`DryLand` offers plus `choose`, on dry land).
