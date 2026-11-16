# HANDOFF: HexGhoul after 1.3.32. Read this first, all of it.

Branch **`claude/relaxed-carson-4y0frw`** (pushed; read `git log -1` for the last commit, the one that adds this file). Never push to
`main`. No PR unless the owner asks. Version **1.3.32** in both `build.py` and `patch/KagunePatch.java` (always bump both, every release:
the next one is 1.3.33). Network `PROTOCOL` (`network/ControlNetwork.java`) is **"66"**; bump it whenever a packet's format changes or one
is added (note it in its comment). **Nothing since 1.3.28 has been tried in game.** The owner tests and reports bugs as they find them.
Never say anything works in game.

## How the owner works (read this)

- They test in game and send bugs as they find them, often mid-task, often angry. Fix what they report first, ship it as its own version
  quickly (build, commit, push), then go back to the bigger task.
- **English only**, in chat and in the mod. (One session answered in Portuguese because a message came in Portuguese; it went badly.)
- **Look at your own previews honestly.** Twice a shut-eye texture was called "matching the marks" when it didn't. If they mark an image,
  match it exactly (they said "one pixel down"). Send previews with `SendUserFile` and ask when unsure; never claim a visual is right
  just because the code ran.
- They want **realism and depth**, not the minimum ("do a little research on what a player could want"). Companion life (spouse,
  children, home) is the heart of the mod beside PvP. Think through what a real companion would do before building.
- Short, plain reports. Say what you changed, what's untested, what you need from them.

## House rules (the build enforces most of them)

- `python3 build.py` must end with **EXIT 0** (about 6 minutes; run it in the background and wait on the log). It patches the jar,
  compiles, verifies every external API reference, checks that the jar loads on a dedicated server, guards listeners, checks banned names,
  then runs every `src/test/java/**/*Tests.java`.
- **Quick loop** (about 25 s, after one full build): `python3 tools/quick_check.py [--noverify] [SuiteName ...]` (e.g. `FamilyTests
  CompanionTests EtoTests QuestTests SceneTests NpcTests AgentTests QuinqueTests`). It compiles, verifies the API, checks names, runs suites.
- New Minecraft/Forge references need a stub in `stubs/` and a line in `verify/api-allowlist.txt` with evidence (`[srg]` from
  `build/mapping-cache/joined.tsrg` etc.; `python3 tools/find_srg.py <yarn class> <member>` fetches mappings from GitHub; ParticleTypes have
  no Yarn names: use `[order]`, the obfuscated field letter in `iv`'s declaration order with anchors, as the existing entries do). An entry
  whose reference the original jar already makes is "stale" and fails the build, as do unused ones. The verifier prints exactly what's
  missing. Prefer calls the code already makes.
- Banned in the jar: Kaneki, Ayato, Furuta (Ken is "Ken"). Banned in visible text: Takatsuki, Aogiri, Yoshimura.
- Sounds only from the `hexkagune:` pack (`SfxTests`).
- **Never alter the owner's skins** (`art/*_skin.png` and the copies under `textures/entity/<key>/<key>.png`). Generated overlays
  (`*_closed.png`, `*_blush.png`, `*_kakugan*.png`) come from `tools/companion_textures.py`; regenerate, then `git status` to confirm only
  the overlays changed.
- Enums sent by ordinal (`NpcTalk.Choice`, `EtoGesture`, `Persona.ALL`): **append only**; tests pin their ends (`CompanionTests`).
- Commits: `"1.3.xx: ..."`, then the trailer lines `Co-Authored-By: Claude <noreply@anthropic.com>` and the session's `Claude-Session:`
  line. **No model names** anywhere (commits, code, docs). Push with `git push -u origin claude/relaxed-carson-4y0frw`.
- Each release: `.github/release-notes-1.3.xx.md`, a README entry (copy the shape of the one above it), a new top section in
  `docs/handoff/companions-handoff.md` (technical: where things live), full build EXIT 0, commit, push.

## What 1.3.29 to 1.3.32 did (details in `docs/handoff/companions-handoff.md`, top sections)

- **1.3.29**: scenes engine (`npc/scene/`, scripts in `src/main/resources/data/hexkagune/talk/*.txt`); NPCs come up to you (approach card,
  Answer key O); Touka's own kiss/hug/stolen kiss and one wing; spouse chores at home (`family/Chores`, `server/HomeChores`); "Go home,
  please."; quests only from their giver (`QuestBook.Giver`, `QuestLog.given/shown`), feats, hunts asked of ghouls at night, "you already
  know this"; Romance tab in the journal (`npc/RomanceCard`, `S2CQuests.romance`).
- **1.3.30**: speech bubbles over heads instead of chat (`S2CNpcSay`, `client/NpcSpeech`, `npc/Speech`, drawn with vanilla's
  `renderNameTag`); spouse doesn't trail at home; Touka's kiss is a lean, hand only once kissed; shut eyes (lashes kept, black line a
  pixel lower); cradle held the whole time.
- **1.3.31**: line of sight for every unprompted approach/greeting/line (`SceneServer.sees`); welcome when she sees you come in;
  cooking/smelting/harvest (`family/Kitchen`, `server/HomeKitchen`; a ghoul partner gets raw cuts); chores before idle child care;
  guarding the home (`EtoBrain.guard`); `/kagune home` status.
- **1.3.32**: NPC spin direction fixed (airplane); kids never hover after a moment (stale carrier in `ChildBrain.tick`); Shift tap lets
  go of a kid's hand; the spouse never teleports at home.

## What's left (the owner's list, in their words where possible)

### A. Keep fixing what the owner reports (always first)

Likely areas to double-check before they find them: chores actually starting in a real house (`/kagune home` shows what was found and why
a chore can't run), pathing through doors and stairs at home, the COOK pose (`EtoPose` case COOK, a bowl and a wooden shovel in hand),
bubbles stacking over the name/tag, the welcome meal handover, children's moments ending (every path that calls `putDown`).

### B. Companion life, more depth (the owner's priority over the CCG work)

Ideas the owner raised or implied, not done: feeding the kids, storing the harvest in a pantry chest, tending animals, lighting the house
at dusk, repairing, more home activities and conversations that reference what she did ("I smelted your iron"), memories shared with the
player. Do a little thinking about what a player would want from a live-in companion before building; ask the owner if unsure.

### C. The CCG investigators and quinques (the owner said: do this last)

The owner, in their words: "The CCG inspectors are still not as agile as expected... a dash animation, like dash left, right, with some
afterimages maybe, just very briefly. The holding animation of the weapon itself, a lot of them look bugged. The combos for it are pretty
bad. When you have one, you should be able to turn it back into a briefcase by tapping right click. The M1 animations with those weapons
are pretty bad. The gun that shoots spikes is really slow when it comes to shooting. And the holding animation of that one is incredibly
cooked. It doesn't even look like it's holding a gun, especially that size of a gun." And about the launcher model: "comically bad
compared to the other models... I was expecting something less squared... should be like a proper gun."

What was found (nothing of this is written yet):
- **Preview tool:** `tools/QuinquePreview.java` renders each quinque's real model (its `_full.json` frame and third-person transform) in
  the hand through vanilla's chain (translateToHand, ItemInHandLayer's Rx(-90) Ry(180) and (1/16, 0.125, -0.625), ItemTransform, the
  -0.5 shift, the elements' -45° about z) on a body posed by `QuinquePose` (via `PoseRender`). Usage is in its header. Use it to judge
  every stance and swing before shipping, and look at the result honestly.
- **Hands drift apart on two-handed weapons:** `QuinquePose.swing` adds the same rotation deltas to both arms from different shoulders,
  so the left hand leaves the grip during combos. Fix: solve the left arm each frame (IK) to the left grip point, computed through the
  item chain from the right arm. Both arms' rotations are in the chest frame when held (`RigPose.heldArm` with own=0), so the IK can be
  done in the chest frame. `AgentTests.hang` has a Java copy of the vanilla item chain to reuse.
- **Launcher:** rebuild `launcher_parts()` in `tools/quinque_models.py` as a proper gun (receiver, raked grip, stock, shroud, muzzle,
  foregrip; element rotations may only be ±22.5/±45 on one axis, and every element is already turned -45° about z for the handheld
  diagonal: consider building it axis-aligned with its own display transforms instead). Give it a rifle hold: a bladed stance (`twist`),
  the stock at the right shoulder, both hands on it, the barrel along the look. The arms are straight sticks on investigators (no elbows),
  which limits reach: plan positions with that. Regenerate the models with the script, never by hand.
- **Fire rate:** players fire on `RightClickItem` (repeats every 4 ticks, 5/s); make it about 10/s (fire every 2 ticks while use is held)
  and rebalance `Quinque.SHARD_DAMAGE` / heat / speed. Investigators' bursts are in `AgentBrain` (`CrystalShots.fire`).
- **Right click folds:** `QuinqueServer.use`: a melee weapon shuts on a plain right click (today it needs Shift); the launcher fires on
  right click, so it keeps Shift + right click to shut (say so in the notes).
- **Dashes and afterimages:** `AgentBrain` dashes (4 ticks of push) and `AgentPose.dash`; add frequent left/right dodges in a fight and
  brief afterimages on the client (see `client/render/KingAfterimages.java` / `PaleAfterimages.java` for how others do it).

## Tell the owner (every release)

What changed, in plain words; that it's untested in game; how to try it (`/kagune <eto|touka|ken> here`, `/kagune <...> max`, marry,
"This is our home now.", `/kagune home`, `/kagune children`).
