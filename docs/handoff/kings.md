# Handoff: the final kakujas (War and Peace), state at 1.0.17 (testing), 2026-10-27

Repo **`21M4rco/HexKagunesv2`**, branch **`main`**. Every push to `main` runs `.github/workflows/release.yml`: it builds and
publishes release `v<VERSION>`. A version that's already released is skipped, so docs-only pushes are safe. The user tests the
released jar in game. **Released so far: 1.0.13, 1.0.14, 1.0.15, 1.0.16, 1.0.17 (testing).**

The user's own words are binding; the newest ones are quoted verbatim below. Read "Open: the user's latest complaints" first.

---

## 1. How the user works (keep to it)

- **Persona:** casual, witty, flirty and warm. Be blunt about weak ideas and stress-test everything. No formality.
- **Speed over talk.** In their words: "stop wasting time in notes, I know whats in each. Just stop wasting time and usage".
  Release notes and README entries are short, one line per item. When they say push a testing build, push one now.
- **Honesty:** never claim in-game behaviour you've only compiled or previewed. Say so plainly.
- **Every update:**
  - bump `VERSION` in `build.py` and `patch/KagunePatch.java`;
  - bump `PROTOCOL` in `ControlNetwork` (with a one-clause history note) when what the network means changes;
  - add `.github/release-notes-<ver>.md` and a three-line README section at the top;
  - commit with the attribution trailer the session gives you;
  - push with `git push <remote> <branch>:main`.
- No PRs unless asked. No destructive git. No AI model names in repo files.
- Show previews: render them, look at them yourself, then send them.

## 2. Build, test, preview

- **Full build:** `python3 build.py`. It takes about 4 minutes, so run it in the background and check the exit code. It
  patches the original jar, compiles `stubs/` and the expansion (`src/main/java`, SRG names), and compiles `awakening/`
  (Mojang names, `-Werror`, remapped through `awakening/mappings.srg`). It runs every test suite, verifies the dedicated
  server and checks every external reference against `verify/api-allowlist.txt`.
- **Quick compile (about 22 s).** Save this as `quick.py` anywhere and run `python3 quick.py` from the repo root:
  ```python
  import sys, time, importlib.util
  sys.argv = ["build.py"]
  spec = importlib.util.spec_from_file_location("build", "build.py")
  b = importlib.util.module_from_spec(spec); spec.loader.exec_module(b)
  for d in (b.TOOLS, b.STUBS, b.CLASSES): b.clean(d)
  b.run("java", "-m", "jdk.compiler/com.sun.tools.javac.Main", *b.EXPORTS, "-nowarn", "-d", b.TOOLS, "patch/KagunePatch.java", "patch/KaguneVerify.java",
        "patch/KaguneServerCheck.java", "patch/KaguneGuard.java", "patch/KaguneRemap.java")
  b.run("java", *b.EXPORTS, "-cp", b.TOOLS, "KagunePatch", b.INPUT, b.BASE)
  b.javac("--release", "17", "-d", b.STUBS, *b.sources("stubs"))
  b.javac("--release", "17", "-Xlint:-options", "-cp", f"{b.BASE}:{b.STUBS}", "-d", b.CLASSES, *b.sources("src/main/java"))
  b.build_awakening()
  ```
- **King tests (356 checks at 1.0.17), after a quick or full build:**
  ```
  rm -rf build/test-classes && javac -nowarn -cp build/classes:build/base.jar:build/stubs -d build/test-classes $(find src/test/java -name "*.java")
  java -cp build/test-classes:build/classes:build/base.jar:build/stubs com.hex.kagune.control.KingTests
  ```
- **Previews:** `tools/art-preview.sh <out-dir> <sheet...>` writes PNGs. Final-kakuja sheets: `war`, `war-views`, `war-moves`,
  `war-strikes`, `war-fx`, `war-cloth` (also writes `king-war-cloth-bare.png`), `king-coming`, `peace-moves`, `peace-views`
  (check the `wanted.contains(...)` list at the top of `tools/ArtPreview.java`).
- **New Minecraft members:**
  - Main code uses SRG names. Every new field or method needs a stub under `stubs/` and an `api-allowlist.txt` line with
    evidence (`[srg]` from `build/mapping-cache/joined.tsrg`, or `[order]` for MobEffects by field letter).
  - The awakening code uses Mojang names, which must exist in `awakening/mappings.srg`.
  - **Never** use `MobEffects.DARKNESS`: its SRG name can't be proved from the field order.

## 3. Coordinates and conventions

- **Body pixels:** x = his left, y = up from the feet, z = back.
- **Part pixels:** the sink's own: x left, y **down** from the part's pivot, z back. Pivots in body px: head and torso
  (0, 24, 0), arms (±5, 22, 0), legs (±1.9, 12, 0).
- **Blocks:** `BLOCK = 16 / 0.9375` pixels per block (the player renderer scales by 0.9375).
- **`PeaceMotion.at(x, y, z)`** takes spec blocks (X right, Y up, Z forward) and returns body px as `(-x*B, y*B, -z*B)`.
- **Body px to world:** `left = (cos yaw, 0, sin yaw)`, `forward = (-sin yaw, 0, cos yaw)`, using body yaw on the client and
  head yaw on the server for Peace.
- **Glow:** a quad whose normal is *exactly* (0, 1, 0) is drawn additively ("burns"). A lit quad lying flat must tilt its
  normal slightly.
- **The `Long.MIN_VALUE` sentinel trap:** `now - MIN_VALUE` overflows negative. It once stopped wild ghouls from ever seeking
  prey.

## 4. Where things live (final kakujas)

- **Server**
  - `control/server/KingServer`: who wears what (`WORN`), toggling (hold K), stats, routing keys and M1 to the worn form's
    `Kit`, the hurt hook, and the 1.0.17 transformation timeline (`coming()`: sounds, War's `blast()`, invulnerability and the
    ability lock).
  - `KingEffects`: stun, bleed, slow, cleanse, brace, glide, `throughArmour` and similar helpers.
- **Kits** (`awakening/src/.../awakening/`):
  - `WarKit` (War's M1 `WarStrike` and abilities).
  - `PeaceKit` (Peace's M1 `PeaceStrike` and its six abilities). Its blades cut along the **same paths the client draws**:
    `PeaceMotion.pose` + `clear` + `point`, through `world()`.
  - Both are installed in `AwakeningHooks`.
- **Pure (`control/king/`)**
  - Rules: `KingRules`, `WarRules`, `PeaceRules`, `KingMove` (move ordinals go over the network; `EMERGE` is client-only).
  - Models and motion: `WarModel`, `PeaceModel` (the 1.0.16 four-blade version), `PeaceMotion`, `WarStrike`, `PeaceStrike`.
  - Effects: `KingFxModel`.
  - New in 1.0.17: `WarCloth`, `KingTransform`, `KingGrowth`, `KingComingFx`.
- **Client**
  - `KingClient` (views from `S2CKingState`; `View.grow` now goes through `KingTransform.grow`).
  - `render/KingLayer`: draws the models on the posed parts, the War cloth pass (`cloth()`), the per-frame rebuild for Peace
    moves and `EMERGE`, and `peaceState()`.
  - `KingFx`: server-sent effects, Peace trails, and the 1.0.17 transformation effects (`comings()` and `coming()`).
  - `KingInput`, `KingHud` / `KingHudView`, `KingChoiceView` (the cards).
  - `animation/KaguneBodyAnimation` (the `war()` branch handles both forms and the transformation pose).
  - `animation/WarPose`: stances, `peace()` idle, `coming()` transformation beats, strikes. Also `PeacePose`.
  - `render/EtoFleshRenderer`: first-person War arms and Peace blades. Also `FirstPersonArms`.
- **Network:** `ControlNetwork.PROTOCOL` is **"38"**. `S2CKingFx` kinds go up to `PEACE_CROSS` = 15.

## 5. What 1.0.17 (testing) contains; none of it has been played in game

- **War's attire is pure black** (`WarModel` garment colours use `WarCloth.CLOTH*`).
- **The cape and coat skirt are real cloth** (`WarCloth`). The simulation:
  - Verlet integration, four substeps a tick, ten iterations;
  - distance, shear and bend constraints, plus a long-range stretch limit;
  - per-face aerodynamic drag and a breeze;
  - capsule collisions against the torso, arms and legs. Collisions are one-sided, so cloth caught on the wrong side of a limb
    is pushed back out on its own side, and strip edges collide too, so a leg can't pass between a strip's two sides;
  - a ground plane.

  `KingLayer.cloth()` steps it per frame at a fixed substep, interpolates, carries it with his feet and draws it double-sided in
  root model space. It falls back to the static cape when swimming, gliding, sleeping or riding.
- **Transformations** (`KingTransform`):
  - **War:** a 16-tick gathering (convulsing pose, heartbeats, black wisps), then the burst. The kakuja erupts from the chest
    (`KingGrowth` with jitter), a dark dome forms, a black and red shock ring runs out, blood flies and splats are left
    (`KingComingFx`), the server `blast()` gives knockback and 50 ticks of Blindness within 6 blocks, and the cloth is burst out.
  - **Peace:** he rises with his arms opening; the blades unfurl as wings (`KingMove.EMERGE` in `PeaceMotion.emerge`); the armour
    swells out from the heart (`KingGrowth`); then a halo, a light flash and feathers.
  - **Both:** no damage and no abilities until done.
- **Fix:** Peace trails used to need another effect on screen to draw (an early return in `KingFx.render`).
- **Known risks:**
  - the cloth costs about 1.9 ms a tick per War player in view;
  - the stretch reaches about 1.3× in hard turns and on legs swinging into the skirt;
  - blood splats assume flat ground;
  - War's burst rebuild costs about 7.5 ms and runs twice a tick for about 0.9 s.

## 6. Open: the user's latest complaints (top priority, verbatim)

With two images (saved here: `peace-ingame-1.0.17-rejected.png` is the current Peace in game, `peace-original-wanted.png` is
the original model's preview sheet):

> "I have a bunch of complaints. I mean, a lot of complaints already. I don't know why you changed the view, like the default
> ambient, to the piece version. Also, in this form, the Kakugan is meant to be activated. The red eyes are meant to be
> present, which they are currently not. And it doesn't feel alive at all. It feels like a static thing. Also, the main
> blades, I don't know why you got rid of some of them. There's only four now. It's meant to be six, I believe. And they
> should be pointing forward at all times, unless they're attacking. Like, there's so many things wrong with this. I don't
> know what you're on about. I mean what happened to the original model??"

In the in-game image the user drew red **X**s over the upper blades pointing up (wrong) and green **?**s at his sides at waist
height (where blades are missing).

What this means, and what to check:

1. **They want the original Peace model back.** It's the pre-1.0.16 `PeaceModel`: get it with
   `git show 1ea5982:src/main/java/com/hex/kagune/control/king/PeaceModel.java`, made after
   `tools/kakuja/reference/peace.webp`. It had massive twisted crimson wings, white organic armour, white horns, and giant
   four-pointed **cross blades** with glowing red cores, each held on its own kagune "blade arm" from the upper back
   (`STARS` with 4 per wing, so 8 in all, plus `ARM_ROOTS`).
   1.0.16 replaced the cross blades with four long chained blades (UR, UL, LR, LL) at the user's own overhaul spec, and they
   reject the result. Bring back the original look, cross blades included.
2. **Six main blades** ("I believe"), **pointing forward at all times unless attacking**. The original had eight smaller
   stars. Recommended: six large cross blades on arms, three each side, all aimed forward at rest, framing him. Ask the user
   in one short question if you're unsure whether they mean six or the original eight.
3. **The kakugan must be on in Peace form.** His face is bare, so the red eyes must show. Find how the kakugan layer decides
   to draw (`KakuganClient`, its layer) and force it on while Peace is worn. Check War too, where the mask has one eye.
4. **"It doesn't feel alive. It feels static."** Peace at rest is built once a tick (`KingLayer` only rebuilds per frame
   during moves) and its idle motion is tiny. It needs clearly visible life:
   - the wings breathing and writhing;
   - the blade arms swaying and drifting;
   - the blades turning slowly and bobbing, with secondary motion when he moves or turns;
   - a per-frame rebuild (or interpolation) at rest.
5. **"I don't know why you changed the view, like the default ambient, to the piece version"** is unclear. It most likely
   means the default idle ("ambient") look and stance was changed to the 1.0.16 "angelic" Peace idle (`WarPose.peace`), or the
   whole model was swapped. Restore the original idle feel. If still unsure, ask one short question.
6. **Keep the gameplay consistent.** `PeaceKit` cuts along `PeaceMotion` paths, so if the blades change, the moves
   (`PeaceMotion.pose` for each move), the server cuts and `KingTests.peaceBlades` / `peaceReach` must change with them.
   The no-clip rule still stands, in the user's earlier words: "the kagunes SHOULD NEVER clip through the player's body". The
   transformation (`EMERGE`, `KingGrowth`) must work with the restored model too.

## 7. Updated to-do list (in order)

1. **Peace back to the original model** (`1ea5982`'s look and the reference sheet), with:
   - six forward-pointing cross blades on arms (confirm six or eight);
   - the kakugan on;
   - visible life at rest (per-frame);
   - the M1 and all six abilities re-choreographed onto those blades, with server cuts matching;
   - no clipping;
   - `EMERGE` reworked for the restored blades;
   - the Peace card text (`KingChoiceView`: "Four cross blades on chains") updated.

   Preview front, side and back plus the moves, compare against `peace-original-wanted.png`, and ship as a testing build fast.
2. **Get the user's verdict on 1.0.17 in game.** Ask about War's cloth (anchoring on the real animated model, popping,
   frame cost) and both transformations. Fix what they report.
3. **War cloth polish if needed:** cost (fewer particles or iterations), turn stretch, sleeves (the left arm's ragged strips
   are still static), maybe a cloth-like hood or collar.
4. **Blood splats:** only draw them where there's ground (check the block under each), so they don't float off edges.
5. **Verify each release run succeeded** (GitHub Actions on `21M4rco/HexKagunesv2`).
6. **Older open items** from before the final-kakuja work are in `docs/handoff/next-session.md` and `requests.md` (7.x paths).
   Ask before picking any up.
