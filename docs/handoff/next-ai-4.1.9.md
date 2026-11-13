# Next AI: after 4.1.77 (Arima looks at his broken blade 2 s, then falls on his face; pushed); then the quests. after 4.1.76 (Arima's blade shatters at once in his one hand, he falls straight on his face; pushed); then the quests. after 4.1.75 (Arima falls on his face and lies there; pushed); then the quests. after 4.1.74 (Arima's change and death slowed, nothing touches him as he dies; 4.1.73 released late, CI fixed; pushed); then the quests. after 4.1.73, handoff (4.1.74 Arima's change and death half written: docs/handoff/wip-4.1.74.patch, the plan below; then the owner's to-do: better the quests). after 4.1.73 (NPCs never in the ground; lag, structures and official NPCs checked; pushed). after 4.1.72 (the hotel replacing the Small City, plains only; pushed). after 4.1.71, handoff (4.1.72 the hotel half written, now shipped). after 4.1.71 (the CCG faction through the General, with dailies; pushed). after 4.1.70 (the Anteiku faction through Ken; pushed). after 4.1.69, handoff (4.1.70 was half written in a patch, now shipped). after 4.1.69 (Ken's sit for stiff legs, nothing in the air; pushed). Next: the Anteiku faction through Ken, the CCG faction through the General, then the hotel. after 4.1.68 (Ken's tower gone, Ken on Anteiku's roof; pushed). after 4.1.67 (the old to-do: quitting a gang, the marriage rules, a spouse's demands, Touka's marriage reactions; pushed). after 4.1.66 (the cookbook, every screen the quest menu's, pushed). after 4.1.65 (the king's choice, pushed). after 4.1.64 (the path choice, pushed). after 4.1.63 (the gender screen, pushed). after 4.1.62 (the kakugan screen, pushed). after 4.1.61 (the Pale menu, pushed). after 4.1.60 (the Graft menu, pushed). after 4.1.59 (the Bikaku menu, pushed). after 4.1.58 (the Rinkaku menu, pushed). after 4.1.57 (every place cleared and set on its own floor, graded round, built after the trees; every template's glass panes joined; pushed). after 4.1.56 (the Ukaku menu in the quest menu's style, pushed). after 4.1.55, handoff (4.1.56 next: the Ukaku menu in the quest menu's style, planned below; the menus go on, one a version). after 4.1.55 (the talk screen in the quest menu's style; the menus go on, one a version). after 4.1.54 (the Gas Masks and Banjou, pushed). Then: the owner's new list (below, quoted). Before: after 4.1.53, handoff (4.1.54 the Gas Masks/Banjou next: its first step in docs/handoff/wip-4.1.54.patch; 4.1.53: the gangs' traits and daily quests; 4.1.52: the reputation bar; 4.1.51: the new CCG tower, the owner's furnished CCG.schematic, a working lift; 4.1.50: joining gangs). Before: after 4.1.47, 4.1.48 half done (4.1.48 WIP: Ayato and Noro on slim arms with builds 0.42/0.52, in docs/handoff/wip-4.1.48.patch; 4.1.47: the ghouls' arm blade rebuilt as the owner's picture, four slashes, a living stance, protocol 121; 4.1.46: the CCG's ghoul division, protocol 120; 4.1.45: roaming gangs, the Clowns and the Gas Masks, protocol 119; 4.1.44: the hideout and the CCG building spawn again; 4.1.43: the one-of-a-kind claim taken as the place is built, never two Etos; 4.1.42: structures once per world, flat, never floating, spread out; 4.1.41: the Reaper's blade turned over about its length; 4.1.40: older worlds' townspeople keep their maps, "complete all" reopens gates at login; 4.1.39: Small City fixes, humans kept in their village, the city ground cached, protocol 118; 4.1.38: two minutes after a fight the Reaper puts his jacket back on; 4.1.37: the Reaper untouchable through his change, a real coat taken off, a slower draw, the blade point-down, his blows clashable; 4.1.36: the Small City in worlds, the villages deleted, humans in vanilla villages; 4.1.35: the Small City cut for 1.20.1; 4.1.28-4.1.31: Arima phase 2 done; 4.1.32: a quinque's M1 first; 4.1.33: quests finished for good; 4.1.34: the quest menu a compact box; next: the Small City, its WIP patch ready). Older: (4.1.27: Arima part 5, the ground strike grows Dawn's blades; 4.1.26: Arima part 4, Dawn smooth and opening like a reverse umbrella; 4.1.25: Arima part 3, both fighting styles; 4.1.24: Arima part 2, his mansion, one per world, him in its garden; 4.1.23: Arima part 1, Dawn every time and Slain Reaper for everyone who hit him; 4.1.22: first person for the kagune-made weapons; 4.1.21: the katana's cuts hands first, with wrists; 4.1.20: the katana a smooth kagune-grown mesh drawn by its own item renderer; 4.1.19: melee quinques in first person through the third person chain, seen from behind the head; 4.1.18: the katana rebuilt, its five cuts by intent, quinques' seven-frame draw; 4.1.17: coat halves overlap, places on their highest ground and adopted when found standing, planted Ukaku spikes tinted; 4.1.16: the kiss blush by mood, eyes fixed on Ayato, Touka, the kids' blinks and kakugan; 4.1.15: reactions to who the player is with, the night's captions, more kid games and no touching teenage sons, six new kisses and smoother gestures; 4.1.14: places built at any good spot, first one kept; coats on the legs; no Ukaku M1 spam; 4.1.9: the inventory crash, the special places; 4.1.10-4.1.13: the wing slams; 4.1.12: places never nowhere, the coat; 4.1.13: slams as arms and hands clear of his body, the guard, /kagune places checks each place)

**RULE (the owner, after 4.1.16, quoted exactly): "always specify that the AI is to evaluate the renders that it generates, and it does not
give up until it's a perfect match and it's a perfect combat system. Like, the animations always have to be top tier, no excuses
whatsoever."** Render everything you change (front, side, first person), judge it yourself, fix and re-render until it is right; tests
passing is not the bar. It is also in `CLAUDE.md`.

Read this first. The house rules are in `next-ai-4.1.2.md` and `next-ai-4.0.0.md` (they still hold): answer in English, quote the owner
exactly, never claim in-game testing, the full `python3 build.py` must end with EXIT 0 on the exact commit before it is pushed, never
alter the owner's files, one version at a time, no PR.

## START HERE (newest, after 4.1.77): better the quests (the owner's final to-do step), shown as a plan first

**This session, after 4.1.76:** the owner (quoted exactly): "after the blade breaks make him look at it, then wait like 2 seconds and then
fall!". 4.1.77 (this commit; pushed only after its full build ended EXIT 0, then checked green): `ReaperBladePose.look` after the shatter
(the hilt raised to his chest, the stub out ahead via the wrist, the left arm loose, twist and lean straightened, `headHold` 1 so vanilla's
head look never turns him), the head angles from `gaze` (pitch and yaw from `EYES` to `snapped`, recomputed each tick), the arm sinking
`LOOK_SAG` through the 2 s stare so it is never a frozen pose, the arm dropping `DROPS` ticks before the fall. Held at his face his head
hardly bowed (15 to 22 degrees: a held pose, not a look); at his chest it bows 25 to 33. Judged from four sides (`arima-look-4.1.77.png`).

Then the quests, as below.

## START HERE (after 4.1.76): better the quests (the owner's final to-do step), shown as a plan first

**This session, after 4.1.75:** the owner, on 4.1.75's GIF (quoted exactly): "why tf does he hold off on that michael jackson ah pose?????
and why does he hold his weapon like that when it's breaking??" Then: "just make the blade instantly shatter  while he's holding it on one
hand, and the fall straight on his face". 4.1.76 (this commit; pushed only after its full build ended EXIT 0, then checked green):
- The "michael jackson" pose was the kneel (`KNEEL` legs swung back, hips sunk: a body with no knees reads from the side as a stiff lean);
  the weapon "like that" was 4.1.31's two-handed lowering, held 3 s by 4.1.74's slow break. Both gone, with the piece-by-piece break
  (`ReaperBladeRules.level`/`breaksAt`/`breaking`, `ReaperBladeMesh.brokenAt`) and their tests.
- `ReaperBladePose.end`: `guard(0)` (one hand) until `REAPER_END_SHATTER` (2), every shard at once then (layer: `broken()`, `dt = age -
  SHATTER`; server: one crash), the arm slack over `SLACK` (5), `fall` from `REAPER_END_FALLS` (6) over 16: stiff, `t` squared, about
  `TOES` (the front of his feet: `toes`, kept where they stand, rising to a pixel as his chest lands a pixel over the ground), lying 60
  ticks, dead at 82. Judged on the real-speed GIF's motion: rising every tick from the shatter to the ground (no held pose), then still.
- **Lesson:** judge every pose from the side too, not only three-quarter front; and the owner reads "slowly"/"too fast" against how long
  an awkward pose is held: fix the pose before stretching its time.

Then the quests, as below.

## START HERE (after 4.1.75): better the quests (the owner's final to-do step), shown as a plan first

**This session, after 4.1.74:** the owner (quoted exactly): "make him actually lay on the floor face down like falling on his face and lay
there a bit". 4.1.75 (this commit; pushed only after its full build ended EXIT 0 in an isolated copy, then checked green on GitHub):
- `ReaperBladePose.end`: bowed till `REAPER_END_FALLS` (132), then `fall`: the whole body's `flip` to `-FACE_DOWN` as `t` squared (a topple:
  faster every tick, measured on the GIF's frame-to-frame motion: 0.5 to 5.4 then dead still), its `drop`/`lunge` worked out so his knee
  (`kneeModel`: halfway down the leg) stays where it rests, rising to `LYING` (3 px: chest a pixel over the ground, head a pixel into it);
  legs, back and head straightened in line; arms limp out a little (`LIE_ARM_*`); the wrist turned `LIE_WRIST` (pi/2: HELD points the blade
  along the arm's front, which is down when he lies on his face: the hilt stood straight up out of him in the first render).
- `AgentRenderer` turns the whole pose stack (layers too) by flip/drop/lunge about `BodyFrame.TURN_PIVOT`: the lying shards would swing up
  with him. `ReaperBladePose.place`/`local`/`carried` (the renderer's turn as maths, tested against itself on random poses);
  `ReaperBladeLayer.shards` works in `shatterPose()`'s frame and carries each vertex into his body's now. `ReaperBladePreview.endShards`
  the same.
- Vanilla tips a dying body 90 degrees on its side (`LivingEntityRenderer.getFlipDegrees`, SRG `m_6441_`, proven with `tools/srg_lookup.py`
  and the three mapping files from raw.githubusercontent.com): `AgentRenderer.m_6441_` returns 0 in his end (stub and allowlist line).
- Not done (choices in the notes): he keeps hold of the hilt; his face straight down; Minecraft's red flash in his last second stays.

Then the quests, as below.

## START HERE (after 4.1.74): better the quests (the owner's final to-do step), shown as a plan first

**This session:** v4.1.73 published at last (its CI fix, `2da41c36`: GitHub's runner had no numpy/Pillow for `tools/npc_spots.py`; both
workflows now install `tools/requirements.txt`; runs green, release up), then 4.1.74 (this commit; pushed only after its full build ended
EXIT 0 in an isolated copy, then checked green on GitHub. If the branch's `git log` has no "HexKagune 4.1.74", redo it from below).

**4.1.74, built from `wip-4.1.74.patch` (deleted in this commit) and its plan, with these found and fixed on the way:**
- **A grab left open:** `EtoBrain.foe` never asked `AgentBrain.untouchable`, and the official NPCs' `NpcTendrils` (Roma's impale, which holds
  what it catches) and `NpcWorms` pick their victims through it: Arima could be impaled and held through his change or his end, `pin`
  snapping him back each tick (a jitter). Now no official NPC's foe while untouchable. Checked the rest: the original kagune's
  `validSwipeTarget`/`isHostileTo` (KagunePatch hooks `spared`) cover Centipede, Bikaku's devour, the Pale owl grab and blitz; `free`
  lets go every player-held hold each tick; wild ghoul NPCs have no grabs (their blows are cancelled by `attacked`); `HurlServer` throws
  blocks only. `PaleStun.clear`/`PaleKnockdown.clear` were wedged between another method's javadoc and its method: moved.
- **The tests' blind spot:** `text.indexOf(x, from) - from < N` passes when `x` is missing (-1). 5 of the first 21 sabotages slipped
  through it. `AgentTests.within(text, from, what, n)` now (found, and near); all 19 such checks in AgentTests rewritten, which revealed
  `guarded`'s "untouchable while he changes or ends" had been dead since its line wrapped (4.1.38's `dressAt`): fixed. **Other suites still
  use the -1-blind pattern** (`grep -rn "indexOf(.*) - .* < [0-9]" src/test`): worth the same helper when you touch them.
- Judged on real-speed GIFs (one frame a world tick) and their frame-to-frame motion: the change in 7 clear beats over 9.5 s; the death's
  lowering and kneel smooth, the body dead still while the blade breaks (a blip each 5 ticks: one cut length gone). The case now falls at
  about 2 g (6 g before: the slowdown made it more real, not floaty); the coat lands in 0.6 s. The shards stay 4.1.31's (their area is the
  blade's own: small, many). Preview only: `ReaperBladePreview` draws the dropped case unsunk (the layer sinks it).
- Tools: `ReaperBladePreview end` (his whole end), `shedgif`/`endgif` (frames a world tick, front and side) and `tools/frames_gif.py`; the
  `shed` sheet labelled in world seconds.

**Next: the owner's to-do, its final step (quoted exactly):** "Add to the to-do list that the final step is to better the quests, to include
everything in the mod + minecraft, daily  and romance quest betterment, so it's more fun, less boring, same for the joining gang quests."
Start by listing what there is (the quest package and `QuestServer`; `GangDaily` and each leader's dailies; the romance cards,
`RomanceCard`/`Courtship`; the joining chains in `GangServer`: Aogiri, the Clowns, the Gas Masks, Anteiku through Ken, the CCG through the
General) and show the owner what each asks now beside a proposal drawing on the whole mod (kagune paths, quinques, the places: hotel,
hideout, circus, CCG tower, library, Anteiku; the NPCs; the kitchen and cookbook; family) and on Minecraft (biomes, mobs, items, crafting,
trading, exploring), before building. One area a version (dailies, romance, gang joining), each judged on screen (`tools/UiPreview.java`)
and tested (sabotaged). Also open from the notes: snowy plains and BOP's other "plains" for the hotel; the CCG banners' patterns.

**Prompt for the next session:**

> Continue HexGhoul on branch claude/amazing-mayer-833gup (fast-forward your session branch onto it first). Read
> docs/handoff/next-ai-4.1.9.md first: the "START HERE (newest, after 4.1.77)" section and the ones after 4.1.76, 4.1.75 and 4.1.74 under it, and CLAUDE.md (render and judge everything you
> change; a push is done only when GitHub's runs are green and the release is up). 4.1.77 is pushed and released. Next, the owner's
> final to-do step: better the quests (dailies, romance, gang joining), drawing on the whole mod and Minecraft: show the owner a plan of
> what each asks now and what it would ask, then one area a version. Full build in an isolated copy, push on EXIT 0, check CI green and
> the release up, send the jar, show the renders. Quote the owner exactly; never claim in-game testing.

## FIRST (after the handoff): 4.1.73 released late; check CI after every push

The 4.1.73 commit (`dd6f394b`) and the handoff after it failed on GitHub ("Build HexKagune" and "Release HexKagune"): `NpcTests.footing`
runs `tools/npc_spots.py`, which loads `city_render`/`interior_render`, which import numpy and Pillow, which the runner didn't have
(`ModuleNotFoundError: No module named 'numpy'`, 1 of 326047 checks failed). Local builds had them, so the session saw EXIT 0 and the
Releases page stayed at 4.1.72. Fixed: `tools/requirements.txt` (numpy, Pillow, pinned) installed by both workflows through
`actions/setup-python`; 4.1.73's notes touched so the release workflow publishes v4.1.73. New rule (in `CLAUDE.md`): after every push,
check the runs end green and the release is up; a local EXIT 0 alone is not a shipped version. Then carry on below with 4.1.74.

## START HERE (after 4.1.73, handoff): finish 4.1.74 (Arima's change and death) from its patch; then better the quests

The owner stopped the session (quoted exactly): "Handoff + prompt for the next ai". **Pushed this session** (each after a full build EXIT 0
on its exact commit, jar sent): 4.1.72 the hotel (`f0d637a1`), 4.1.73 NPCs never in the ground, lag/structures/official NPCs checked
(`dd6f394b`). The commit carrying this section changes only docs/ (the build's code is 4.1.73's; no test or build step reads docs/).

**The owner's words for 4.1.74 (quoted exactly):** "bug found, while transforming arima keeps holding the old quinque instead of getting rid
of it, the death animation is too fast, during his death he should remain immortal and the blade slowly shatter, then he holds still for a
bit then dies. during this death animation he's 100% immortal, ungrabbable and all effects like bleed etc are removed from him." Then: "I
think same for the transformation animation, it goes by too quick, can barely see it".

**The owner's to-do list, its newest item (quoted exactly):** "Add to the to-do list that the final step is to better the quests, to include
everything in the mod + minecraft, daily  and romance quest betterment, so it's more fun, less boring, same for the joining gang quests."

**4.1.74, half written:** `git apply docs/handoff/wip-4.1.74.patch` on 4.1.73 (`dd6f394b`; checked: it applies). With it the code compiles,
the API verifier passes, `AgentTests` (64777) and `NpcTests` (326047) pass. In the patch (done):
- **Dawn kept in his hand through the change (the bug):** `act` returns from `shed()` every tick of the change, so `arm()` (which empties
  his hand once bare: its 4.1.28 comment says "Dawn put by while he takes it off") never ran until it was over. Now `AgentBrain.letGo`
  empties his main hand as the change begins and every tick of it. Not dropped as an item (a player could take a free Dawn mid-fight; it
  is his reward on death); not drawn falling (no preview draws Dawn's model to judge it): tell the owner it leaves his hand at once.
- **The change slowed:** `AgentRules.REAPER_CHANGE_SLOW` 1.75 (every key of the change keeps its own ticks: `REAPER_SHED_TICKS`,
  `REAPER_DRAW_TICKS`, the case's, the coat's); `REAPER_CHANGE_REAL` (189 world ticks, 9.5 s; 108, 5.4 s, before) and `changeAge(real)`.
  Server: `shed()` swaps at `changeAge(age) >= REAPER_SHED_SWAP`, ends at `REAPER_CHANGE_REAL`. Client: `AgentClient` (the pose),
  `ReaperBladeLayer.draw` (the case and the blade), `ReaperCoatLayer` (the coat; DRESS untouched). Uniform: the coat's and the case's falls
  are slower too (judge them in the GIF; if the case's drop looks floaty, slow only its `fall` term's clock: `ReaperBladeLayer.fallen` and
  `ReaperBladePreview`, `dt * REAPER_CHANGE_SLOW`).
- **The death slowed, the blade breaking piece by piece:** `REAPER_END_SHATTER` 20 (lowered: 9 before), `REAPER_END_SHATTERED` 80 (the
  last piece), `REAPER_END_KNEELS` 86 over `REAPER_END_KNEELING` 24, dies at `REAPER_END_TICKS` 170 (8.5 s; 3.5 s before). Pure:
  `ReaperBladeRules.shattered`, `remaining` (the blade's end u, `SHATTER_FROM` = `ReaperBladeMesh.TIP` down to `SHATTER_TO` = `SNAPS`),
  `breaksAt(i)` (each shard as the break reaches its u: tip first), `breaking(age)` (shards off this tick), `level(age)` (of `LEVELS` 12
  cut lengths, the next up from the break). `ReaperBladeMesh.brokenAt(level)` (cached; 12 the whole blade, 0 the stub). `ReaperBladePose.end`:
  lowered over 19 ticks, held still through the whole shatter, arms slack only after `SHATTERED`, the kneel over 24, the bow after.
  `ReaperBladeLayer.ended`/`shards`: the blade cut at `level(age)`, each shard from its own `breaksAt` (its fall by its own dt).
  `AgentBrain.ending`: a glass crack each tick a piece breaks (quieter than the end, pitch falling as it goes), the old heavy sounds when
  the last goes, death at 170 as before (by the blow that began it: Slain Reaper and Dawn's drop unchanged).
- **100% immortal, ungrabbable, everything removed:** `untouchable` (4.1.37) already cancels every attack through his end, and `pin`/`free`
  release the original's holds, Bikaku, Pale, Graft and the bound arm each tick. New: `EtoOwl.fair` (a companion's arm grabs),
  `ChildKagune.fair` (a child's), `KenPullOut`'s burst push all skip `AgentBrain.untouchable`. `AgentBrain.cleanse` every tick of his end:
  `removeAllEffects` (`m_21219_`: new in the stub `LivingEntity` and in `verify/api-allowlist.txt`, evidence `joined.tsrg bfz en ()Z m_21219_`
  from MCPConfig 1.20.1), `clearFire` (`m_20095_`, allowlisted before on LivingEntity: declared in that stub now), `UkakuCrystalCombat.stanch`
  (its bleed stacks and the spikes stuck in him gone, clients told), `PaleStun.clear`, `PaleKnockdown.clear` (a stun re-adds slowness each
  tick: cleared or it comes back).
- Tests: two `AgentTests` source checks moved to the new code (`REAPER_CHANGE_REAL`; the layer's `brokenAt(level(age))` and per-shard dt).

Still to do (4.1.74):
1. **Tests** (`AgentTests.reaperEnd` and its neighbours; sabotage each): the order SHATTER < SHATTERED < KNEELS, and still on his knees at
   least 60 ticks before `END_TICKS`; `breaksAt` tip first (falls with u) and all within SHATTER..SHATTERED; `remaining` from TIP down to
   SNAPS; `level` 12 before, 0 after, its cut never shorter than `remaining`; `breaking` over the end sums to `SHARDS`; the pose unchanged
   through the shatter (hands still) and slack only after it; `brokenAt` lengths; `REAPER_CHANGE_REAL` = ceil(108 x 1.75) and `changeAge`;
   wiring: `letGo` at the change's start and each tick, `cleanse` each tick of the end, `untouchable` in the two `fair`s and Ken's push,
   `stanch`/`clear` there.
2. **Renders, judged** (CLAUDE.md): an `end` mode in `tools/ReaperBladePreview.java` (the pose `ReaperBladePose.end(age)`, the blade
   `brokenAt(level(age))` in his fist, the shards from `breaksAt` as `ReaperBladeLayer.shards` places them, falling to the ground), frames
   through the whole end from the front and the side; the `shed` sheet labelled in world seconds; and GIFs at real speed (each world tick
   through `changeAge`) of the change and of the death: the owner's complaint is speed, which only moving frames show. Fix and re-render
   until it is right.
3. **Docs and ship:** VERSION 4.1.74 (`build.py`, `patch/KagunePatch.java`), release notes (quote the owner; say: Dawn leaves his hand as
   the change begins; the change 5.4 s to 9.5 s; the death 3.5 s to 8.5 s, the blade breaking piece by piece, then his knees, then still;
   everything on him cleared, nothing can take hold of him), README, a table row, the patch file deleted in the commit; full build in an
   isolated copy; push on EXIT 0 to both branches; jar and renders sent. Not tested in game.

**After 4.1.74, the owner's to-do (the final step):** better the quests: everything in the mod and in Minecraft, the dailies and the romance
quests made more fun and less boring, and the gang-joining quests too. Start by listing what there is (the quest package and
`QuestServer`; `GangDaily` and each leader's dailies; the romance cards, `RomanceCard`/`Courtship`; the joining chains in `GangServer`:
Aogiri, the Clowns, the Gas Masks, Anteiku through Ken, the CCG through the General) and show the owner what each asks now beside a proposal
that draws on the whole mod (kagune paths, quinques, the places: hotel, hideout, circus, CCG tower; the NPCs; the kitchen and cookbook;
family) and on Minecraft (biomes, mobs, items, crafting, trading, exploring), before building. One area a version (dailies, romance,
gang joining), each judged on screen (`tools/UiPreview.java`) and tested.

**Prompt for the next session:**

> Continue HexGhoul on branch claude/admiring-johnson-8df58h (fast-forward your session branch onto it first). Read
> docs/handoff/next-ai-4.1.9.md first: the "START HERE (newest, after 4.1.73, handoff)" section, and CLAUDE.md (render and judge everything
> you change; tests passing is not the bar). Finish 4.1.74, Arima's change and death: git apply docs/handoff/wip-4.1.74.patch, then the rest
> of its plan in that section (the new tests, the end preview and real-speed GIFs of the change and the death, judged until right, docs).
> Full build in an isolated copy, push on EXIT 0 to both branches, send the jar, show the renders. Then the owner's to-do, its final step:
> better the quests (the dailies, the romance quests, the gang-joining quests, drawing on the whole mod and Minecraft), shown to the owner as
> a plan first, then one version at a time. Quote the owner exactly; never claim in-game testing.

## START HERE (after 4.1.73): ask the owner what's next

**This session:** 4.1.72 (`f0d637a1`, pushed after EXIT 0, jar sent), then 4.1.73 (this commit; pushed only after its full build ended
EXIT 0. If the branch's `git log` has no "HexKagune 4.1.73", redo it from below). The owner's words for 4.1.73 (quoted exactly): "Run a lag
prevention check / Make sure NPCS dont spawn in the ground at any point, especially official NPCS. / Make sure Structures are unique and
spawn correctly. / Make sure every OFFICIAL npc is spawning correctly and are indeed working."

**4.1.73, what was found and where:**
- Every NPC home is kept as a whole block (`EtoNpc.settle`: feet + 1), and every put-down went to `home.y - 1.0`: into a slab or carpet
  under it, into ground built up since, unchecked. Now `Footing.stand` (pure: nearest collision top from `DOWN` 2 under to `UP` 3 over
  with the body free) via `NpcFooting.feet`/`put` in `UniqueNpcs.levelTick` (coming back), `EtoNpc.goHome`, `ChildNpc.goHome`,
  `Homeward.put` (now takes the level), `GhoulNpcBrain.accompany` (ally brought up behind its player). `NpcFooting.tick`: every NPC of
  the mod's (`NpcSwim.ours`) every 10 ticks; in the blocks (`getBlockCollisions` of its box shrunk by `INSIDE` 0.15) twice running, stood
  on the nearest floor; never noPhysics, passenger, sleeping or a seated investigator.
- `tools/npc_spots.py [--check]`: every template's NPCs (117) against interior_render's shapes (passable blocks as the game has them);
  unwritten cells are air over the floor, ground at and under it; an investigator flagged `HexCcgSeat` may start in its own chair's
  stairs (AgentDesk sits it on the step at once). Run by `NpcTests.footing`. The makers now set feet on the collision top:
  `exploration_structures.rest` (a carpet or snow layer in the feet block, a bottom slab under it), `companion_structures` (down onto a
  carpet, snow layer or bottom slab under the person). Regenerated: `ccg_building` (16 NPCs +1/16; its 153 light blue banners now carry
  the file's patterns, the 4.1.72 converter's; the owner is asked in the notes) and `gas_hideout` (`--only gas_hideout`); the hideout's
  tiles checked out again (unchanged NPCs).
- Lag: the wide searches are gated already (humans `LOOKS` 4 staggered, ghouls/investigators prey every 10, companions every 5, defend
  every 10 staggered), packets are tracking-only and dirty-flagged, heightmap reads stay in the loaded rings, renderers once a frame capped
  at 256. Fixed: `AgentBrain.partner` searched 72 blocks each second forever once a partner died: `AgentRules.look` (20 ticks, then
  `LOST_LOOK` 600 after `PARTNER_LOST`).
- Structures: the seven one-of-a-kind places all go through `claimed`/`onlyIfClaimed` (`/place` too); sets, salts, structure JSONs and
  jigsaw pools consistent. Official NPCs: all 13 entity types have attributes and renderers; all 8 personas `UniqueNpcs.kind`; each in
  exactly its own template once; Arima `claimExclusive`. Nothing else to fix there.
- Renders: `docs/handoff/npc-spot-4.1.73.png` (the garden house's Gas Mask, body box before/after; `interior_render` with marks),
  `docs/handoff/ccg-banner-4.1.73.png` (a sketch: no tool draws banner patterns).

**Next:** ask the owner what's next (and answer the notes' choices they reply to: snowy plains and BOP's other "plains" for the hotel; the
CCG banners' patterns).

**Prompt for the next session:**

> Continue HexGhoul on branch claude/admiring-johnson-8df58h (fast-forward your session branch onto it first). Read
> docs/handoff/next-ai-4.1.9.md first: the "START HERE (newest, after 4.1.73)" section, and CLAUDE.md (render and judge everything you
> change; tests passing is not the bar). 4.1.73 is pushed; ask the owner what's next. One version at a time; full build in an isolated
> copy, push on EXIT 0 to both branches, send the jar, show the renders. Quote the owner exactly; never claim in-game testing.

## START HERE (after 4.1.72): the owner's list is done; ask them what's next

**This session:** 4.1.72, the hotel replacing the Small City (this commit; pushed only after its full build ended EXIT 0. If the branch's
`git log` has no "HexKagune 4.1.72", redo it from below). Built from `wip-4.1.72.patch` (deleted in this commit) and its plan, with these
changes:

- **The owner, this session (quoted exactly):** "Only spawns on plains or biomes o plenty equivallent biomes, we cant have that shit
  spawning on mountains". So `hexkagune:plains` (`tags/worldgen/biome/plains.json`: minecraft:plains, sunflower_plains, snowy_plains;
  Biomes O' Plenty's field, grassland, pasture, prairie, lavender_field, each `required: false`) and `HotelPlan.onPlains`: every column
  of the footprint and 8 round it, every 16 blocks, in that tag (`ControlStructures.plains`, in `Site.hotelAt` unless /place asked).
  BOP 1.20.1 facts (read on github.com/Glitchfiend/BiomesOPlenty, branch `1.20.1`; its tags are static JSON under
  `common/src/main/resources/data/`): its `forge:is_plains` holds grassland, highland, lavender_field, lush_savanna, orchard,
  overgrown_greens, pasture, prairie, rocky_shrubland, shrubland, tundra; Forge 1.20.1's own holds plains, snowy_plains, meadow,
  sunflower_plains. Neither tag is used (meadow, highland, rocky_shrubland are hills). Snowy plains are in: the notes ask the owner.
- **Placement measured and fixed** (`village/HotelPlan`, pure): the patch's plan (12 chunks out, a random side, nothing else checked)
  let the hotel's 8-block clearing come to 79 blocks from a village's start corner on its north and west sides (its pieces reach up to ~96: 80 from the
  start piece's middle) and, over eight seeds, 166 of 597 hotels had another village's offer within reach or overlapped another hotel.
  Now: `DISTANCE` 14 (near edge 119 blocks out of the start corner); the side is the first from `first` (by seed) round clockwise whose
  hotel has no other offer within `CLEAR_OF` 14 chunks (`side`, `neighbours`); of two hotels within `APART` 15, the one beside the village
  first in (x, z) order stands (`hotelOf`). `near` (what `inHotel` asks for every other place) runs `hotelOf` on every offer within 27, so
  the places keep clear of exactly the hotels. The scan (`scan`, `offersNear`) asks both ends of its square: the patch's `inHotel` stepped
  -13 by 6s and never asked 12 or 13 chunks out (218 chunks misjudged in the test before the fix).
- **The patrol outside:** `tools/exploration_structures.py` site "hotel" `patrol_sky`: every patrol stop under the open sky (two of the
  patch's 20 were in the great hall, whose ceiling, 12 up, is past `roofed`'s 7). Regenerated; `git checkout` of the hideout's and the
  CCG building's tiles, `tools/join_panes.py` (hotel 719 joined, the rest 0), as the patch's plan said.
- **The Small City's API gone from `verify/api-allowlist.txt` and `stubs/`** (its placement type, `ChunkGeneratorStructureState`,
  `RandomSpreadType`, `MapCodec.unit`, `Registries.STRUCTURE_PLACEMENT`): the verifier refuses stale entries.
- **The people (checked in the tiles):** 20 humans (two at each of 10 homes: 6 in the staff rooms of the working floor cut into the slab
  under the street, 4 on the ground floor) and 6 investigators on the patrol; `NpcTests.hotel` counts them exactly.

**4.1.72, where:** `village/HotelPlan`, `ControlStructures` (`Site.hotel`, `hotelAt`, `villages`, `villageOffers`, `village`, `inHotel`,
`PLAINS`, `plains`), `DryLand` (`DEEP`, `HOTEL_SALT`), `RetiredSites` (maps 7-10), `SiteMaps` (generated), `structures/hotel/`,
`worldgen/structure{,_set}/hotel.json`, `tags/worldgen/biome/plains.json`, protocol 126. Tests `NpcTests.hotel` (and `worldgen`'s land
checks on the hotel's footprint, `cityFixes`). Renders: `python3 tools/city_render.py out.png [--site hotel] [--grass] [--turn T]
[--roof Y] [--people]` (any tiled site now; the Small City's loader went with it), `python3 tools/hotel_render.py village|street out.png`;
`docs/handoff/hotel-se-4.1.72.png`, `hotel-nw-4.1.72.png`, `hotel-street-4.1.72.png`, `hotel-village-4.1.72.png`. The generator needs
`--blocks <minecraft-data 1.20 blocks.json>` (`npm pack minecraft-data@3.62.0`).

**Next:** every item on the owner's lists is done (the new list after 4.1.54: Ken on Anteiku's roof, his sit, Anteiku, the CCG, the hotel).
Ask the owner what they want next; answer the choices in the 4.1.72 notes they reply to (snowy plains, BOP's other "plains").

**Prompt for the next session:**

> Continue HexGhoul on branch claude/admiring-johnson-8df58h (fast-forward your session branch onto it first). Read
> docs/handoff/next-ai-4.1.9.md first: the "START HERE (newest, after 4.1.72)" section, and CLAUDE.md (render and judge everything you
> change; tests passing is not the bar). 4.1.72 (the hotel replacing the Small City, plains only) is pushed and the owner's list is done:
> ask the owner what's next. One version at a time; full build in an isolated copy, push on EXIT 0 to both branches, send the jar, show
> the renders. Quote the owner exactly; never claim in-game testing.

## START HERE (after 4.1.71, handoff): finish 4.1.72, the hotel, from its patch

The owner stopped the session (quoted exactly): "Stop, handoff and prompt for the next ai, now". **Pushed this session** (each after a full
build EXIT 0 on its exact commit, jar sent): 4.1.70 Anteiku through Ken (`4b7373d9`), 4.1.71 the CCG through the General (`6b1d210b`). The
commit carrying this section changes only docs/ (the build's code is 4.1.71's). The previous session's 4.1.69 (`d54ced3f`) is the real one;
a duplicate 4.1.69 made by mistake (`b3cd375a`) is only in `claude/sharp-ptolemy-cjxldm`'s history, under merges whose trees are the shared
branch's commits: ignore it.

**The owner's words this session (quoted exactly):** of 4.1.70's first bar, which ran past the hotbar: "naw man just make the energy bar
smaller but longer to fit the bar, dont make it past it wtf bruh"; shown two ways, they chose "B: shrunk into its spot (Recommended)" (an
Anteiku member's bar on half pixels in the usual spot: `ReserveBarView.drawFine`). The middle gap between hearts and food is the XP level
number's: never draw a bar there. Asked how many hotels, they chose "Beside some villages" (about one village in four).

**4.1.72, the hotel, half written:** `git apply docs/handoff/wip-4.1.72.patch` on 4.1.71 (binary; checked: it applies), then
`git rm -r -q src/main/resources/data/hexkagune/structures/small_city_0 src/main/resources/data/hexkagune/structures/small_city_1
src/main/resources/data/hexkagune/structures/small_city_2 src/main/resources/data/hexkagune/structures/small_city_3` (left out of the patch:
7 MB of old tiles), and delete the patch file in the 4.1.72 commit. The owner's words (in the section after 4.1.56 below): "To-do list
addition: / from now on the small city is gone, erased. / From now on close to a village will be an hotel where humans work, live and walk
around doing their own thing. / CCG patrols around the hotel at all times."

In the patch (done):
- `tools/exploration_structures.py`: the city's sites out (no `city_structure.make()` run); site "hotel" (map 12, kind human, 10 homes = 20
  humans, 6 investigators on its patrol ring (55, 125), ground 11, top 194, `keep` 8, `hollow_below`: the owner's file is 191 x 233 x 196
  from 1.16.5: a stone slab (layers 3-11, a basement of corridors cut in it, kept with its air written in) on a superflat's dirt (0-2, left
  to the world's), the tower to layer 193); 1.16 block entities kept (signs' Text1-4 to front_text, banners' Patterns, skulls' SkullOwner),
  `cauldron[level>0]` to `water_cauldron`. Its run: hotel 191 x 194 x 196, 1,128,086 blocks, tiles 3 x 4, 20 patrol spots, 10 homes.
  Run it with `--blocks <minecraft-data 1.20 blocks.json>` (`npm pack minecraft-data@3.62.0`, then `package/minecraft-data/data/pc/1.20/
  blocks.json`): afterwards `git checkout` the aogiri_hideout and ccg_building tiles (they differ only by panes `tools/join_panes.py`
  joined after 4.1.57) and run `tools/join_panes.py` (the hotel's: 719 joined; every other template 0).
- `structures/hotel/t_*.nbt` (3.5 MB, panes joined), `worldgen/structure/hotel.json` (site_hotel, top_layer_modification, beard_box),
  `worldgen/structure_set/hotel.json` (random_spread spacing 1, separation 0: every chunk offered; the hotel decides), the Small City's
  structure and set deleted.
- `SiteMaps.java` regenerated (aogiri_hideout, ccg_building, hotel); the city's four maps (7-10) moved by hand into `RetiredSites`.
- `village/HotelPlan` (pure): one village in `ONE_IN` 4 (`chosen`, by seed and the village's chunk), on a `side` (0 N, 1 E, 2 S, 3 W),
  `DISTANCE` 12 chunks out (a hotel reaches ~98 blocks, a village 80: they never meet), `turns(side)` faces its forecourt (the file's
  south) to the village, `covers`, `VILLAGE_TAGS` (vanilla's has_structure/village_*).
- `ControlStructures`: `City`, `CityPlacement`, `cityOffers`, `inCity`, the small_city type and the city placement (and its
  DeferredRegister) removed; `villages(context)` (the world's minecraft:villages set, a random spread or null), `village(context, x, z)`
  (its biome in a village tag), `inHotel` (replaces `inCity` for the CCG building, the hideout and the jigsaw places); `Site.place` sends
  the hotel to `hotel()`/`hotelAt()` (/place builds one where asked; else only on the chunk beside a real, chosen village, on land as the
  hideout's `DryLand.land`, clear of an older world's claimed place: `holdsPlace(ChunkPos)`). `CityPlan` deleted, `DEEP` moved to
  `DryLand`, `DryLand.CITY_*` replaced by `HOTEL_SALT`. It compiles (`quick_check --noverify`).
- Tests: `NpcTests.hotel()` replaces `smallCity()` (site, tiles, people, `HotelPlan`, the code and JSONs, the city gone, the owner's file's
  sha256); `cityFixes` checks the retired maps; counts moved (sites 3, foundations 2, m_262864_ overrides 3); `KitchenTests` "hotel".

Still to do:
1. **The tests don't compile yet:** `NpcTests` ~lines 3190-3215 (a land test block) still call `CityPlan.land`/`CityPlan.FLAT` etc.:
   rewrite them on `DryLand.land` (the hideout's and hotel's) or drop what only judged the city. Then run `python3 tools/quick_check.py
   NpcTests KitchenTests ControlTests CompanionTests EtoTests AgentTests` (with the verifier) and fix what fails; `hotel()` has never run
   (its people count reads the tiles' text: check it really counts).
2. Protocol 125 to 126 (`ControlNetwork.PROTOCOL` with its note: the structure types differ; `BikakuWormTests`' pin).
3. Renders, judged: the hotel from its tiles (`tools/city_render.py` reads a tiled site; or `exploration_structures.py --preview <dir>`),
   from two sides and a street-level cutaway with its people; a plan of a village with its hotel turned to face it.
4. Docs, VERSION 4.1.72, release notes (say: the Small City is no longer built; old worlds keep theirs and its people their maps; hotels
   beside about one village in four, turned to face it, 20 humans living in each and 6 investigators always on patrol round it), README,
   a table row here; full build in an isolated copy; push on EXIT 0 to both branches; send the jar.

**Prompt for the next session:**

> Continue HexGhoul on branch claude/admiring-johnson-8df58h (fast-forward your session branch onto it first). Read
> docs/handoff/next-ai-4.1.9.md first: the "START HERE (newest, after 4.1.71, handoff)" section, and CLAUDE.md (render and judge everything
> you change; tests passing is not the bar). Finish 4.1.72, the hotel replacing the Small City: git apply docs/handoff/wip-4.1.72.patch,
> git rm the four small_city tile folders as that section says, then the rest of its plan (the tests that don't compile yet, the protocol,
> renders of the hotel and of one beside its village, docs). Full build in an isolated copy, push on EXIT 0 to both branches, send the jar,
> show the renders. Quote the owner exactly; never claim in-game testing.

## START HERE (after 4.1.71): the hotel replacing the Small City next

**This session:** 4.1.70 (`4b7373d9`) pushed after EXIT 0, jar sent; 4.1.71 the CCG faction (this commit; pushed only after its full
build ended EXIT 0. If the branch's `git log` has no "HexKagune 4.1.71", redo it from below). The session branch
(`claude/sharp-ptolemy-cjxldm`) follows the shared one through merges whose trees are the shared commit's (it once held a duplicate 4.1.69).

**4.1.71, where:** `GangPlayer` (CCG 5, `generalStep`/`ghoulKills`, `sworn`/`killedGhoul`/`ccgDone`, `BRIEFCASE` U+E004, `CCG_CYAN`,
`CCG_DAMAGE` 1.3; `of(who)`: `who.ccg()` is the CCG, so the division's stance, defence and reputation follow the gang rules), `GangServer`
(`general`, `generalDaily`, `hunted`, `ccgStance`, `ghoul`, `fought`'s damage, `nameTag`, `inHideout` "ccg_building" `CCG_HIDEOUT` 50,
`giveMask` the issued `quinque_case_bikaku`, `died`: an investigator killed by a member is one of their own, any ghoul counts for the chain
and earns reputation), `GangDaily` (three jobs appended, the CCG's pay), `GangWords` (the General), `AgentBrain.wanted` (the CCG stance
before the grudge check), `reveal` (members never marked), `hurt` (a member's blow: `struckOwn`), `GhoulNpcBrain.wanted` (after Anteiku's
neutral check: every non-division ghoul goes for a member). Tests `NpcTests.ccg` (and the dailies loop, the leaders' words). Renders:
`tools/UiPreview.java <dir> ccg`; the emblem sheet `python3 tools/gang_emblems.py --preview <png>`.

**Next, 4.1.72: the hotel replacing the Small City** (`art/exploration/humanhotel.schem`, the owner's: never altered; the Small City's
worldgen, its humans and its tests go; `SitePrep`, `tools/join_panes.py` on the template made from it; render it with
`tools/interior_render.py`).

## START HERE (after 4.1.70): the CCG faction through the General next, then the hotel

**This session:** 4.1.70 the Anteiku faction (this commit; pushed only after its full build ended EXIT 0. If the branch's `git log` has no
"HexKagune 4.1.70", redo it from below). Built from `wip-4.1.70.patch` (deleted in this commit) and its plan, with these changes:
`KEN_ANSWERS` live in `GangWords` (`KEN_WHY`, `KEN_RIGHT`: a preview touching `GangServer` loads Minecraft classes); the HUD's purple tint
reads `BikakuClient.potential` (Potential Unlock alone), `capacity` adds Anteiku's one (else every Anteiku bar turned purple); Anteiku earns
no rival-kill reputation. A session duplicated 4.1.69 by mistake (its first fetch came before 4.1.69 was pushed): that commit
(`b3cd375a`) is only on `claude/sharp-ptolemy-cjxldm`'s history; ignore it.

**4.1.70, where:** `GangPlayer` (ANTEIKU 4, chain, `ANTEIKU_CAPACITY`), `GangServer` (`ken`, `holdsBeans`/`takeBeans`, `nameTag`,
`neutral`, `capacity`; `leads`/`gangOf`/`memberOf` with Ken and Touka), `GangWords` (Ken), `GhoulNpcBrain.wanted` (after the grudge and
creative checks, before `who.ccg()`) and `fair`, `SharedBar.capacity`, `BikakuClient.capacity`/`potential`, `GangClient.anteiku`,
`ReserveBarView.draw(..., tail)`/`width`/`drawFine` (`FINE` 2, `ANTEIKU_TAIL` 153, `FINE_DOWN`), `ReserveHud`. The owner, of a first bar
1.5 times long past the hotbar's end (quoted exactly): "naw man just make the energy bar smaller but longer to fit the bar, dont make it
past it wtf bruh"; shown two ways (A: slimmer, grown into the middle, under the XP level number; B: on half pixels in its own spot), they
picked "B: shrunk into its spot". The middle gap between hearts and food is the XP level number's: never draw there. Tests `NpcTests.anteiku`. Renders: `tools/UiPreview.java <dir> anteiku`
(Ken's four pages, `anteiku-bar.png`). Learned: a preview that names any class touching Minecraft (`GangServer`) fails to run: keep what
the previews read in pure classes.

**Next, 4.1.71: the CCG faction through the General** (the owner, quoted exactly in the section after 4.1.54: "CCG Faction: / Join through
the general (quest) and now daily quests as well. / Traits: / CCG no longer attacks the player. / All ghouls but official NPCS are hostile
towards the player. / Damage increased when fighting ghouls. / Looks:  Cyan Blue with a brief cafe symbol."; the symbol: "A briefcase
(Recommended)"). Same rails as Anteiku: `GangPlayer.CCG` 5, a chain at the General (`GeneralScript`, `Persona.GENERAL`), dailies in
`GangDaily` (the General gives them), the CCG's agents (`AgentBrain`) leave a member alone, every ghoul NPC but the named ones goes for
them, more damage to ghouls (`LivingHurtEvent`), a briefcase glyph U+E004, the name in `§b`. Then the hotel.

## START HERE (after 4.1.69, handoff): finish 4.1.70, the Anteiku faction, from its patch

The owner stopped the session ("prompt + handoff now"). **Pushed this session** (each after a full build EXIT 0 on its exact commit, jar
sent): 4.1.56 (`1b444f34`) to 4.1.66 (`a0a55d6f`), 4.1.67 (`e32fb7b1`, the old to-do), 4.1.68 (`4024deb8`, Ken's tower gone, Ken on
Anteiku's roof), 4.1.69 (`d54ced3f`, Ken's sit; pushed with this commit once its build ended EXIT 0: if the branch's `git log` has no
"HexKagune 4.1.69", its notes are below and its build must be redone). The commit carrying this section changes only docs/ (the build's
code is 4.1.69's).

**4.1.70, the Anteiku faction, half written:** `git apply docs/handoff/wip-4.1.70.patch` (applies cleanly on 4.1.69; never compiled or
tested yet). The owner's words (quoted exactly, from the section after 4.1.54): "Anteikou - Joinable through Ken (quest) / Traits: / All
ghouls and factions are neutral to the player but the CCG. / Enegy bar increases size and increases maximum capacity. / Looks: Light
brown name tag with a coffee symbol."

In the patch (done):
- `GangPlayer`: `ANTEIKU = 4`; Ken's chain `ANTEIKU_NOT_YET/BEANS/DOVES/DONE` with two new record fields `anteikuStep`, `doveKills`
  (all 11 constructions carry them); `answered()`, `beansBrought()`, `droveDove()`, `anteikuDone()`; `ANTEIKU_CCG = 3`,
  `ANTEIKU_BEAN_COUNT = 8`; `COFFEE` (U+E003); `ANTEIKU_BROWN = 0xC89B6D`; `named` (plain-text fallback gold), `title` "Anteiku".
- The emblem: `tools/gang_emblems.py` COFFEE (a cup on its saucer, steam curling; judged against two other steams, the curled one kept),
  `textures/font/anteiku_coffee.png`, `assets/minecraft/font/default.json` U+E003. Render: `docs/handoff/anteiku-emblem-4.1.70.png` (all
  four emblems, each in a name tag and a chat line, the name in its colour).
- The light brown name: none of the 16 `§` colours, so `GangServer.nameTag(gang, name)` builds a Component: the emblem, then the name with
  `Style.EMPTY.withColor(ANTEIKU_BROWN)`. New API, stubbed in `stubs/net/minecraft/network/chat/` (`Style`, `MutableComponent.m_6270_`,
  `m_7220_`) and in `verify/api-allowlist.txt` with evidence (worked out this session from MCPConfig 1.20.1 `joined.tsrg`, Fabric
  intermediary 1.20.1 and Yarn 1.20.1, all reachable on raw.githubusercontent.com): `Style` = obf `ts` (C_5020_, yarn Style);
  `f_131099_` EMPTY; `m_178520_` withColor(int); `MutableComponent` = `tj`: `m_6270_` setStyle, `m_7220_` append. The build's verify step
  has not seen them yet: if it complains about the C line's form, compare with other C lines.
- `GangServer`: the two fields kept with the player (`HexGangAnteikuStep`, `HexGangAnteikuDoves`); `died()` counts an investigator for
  Ken's chain (a CCG agent or one of the CCG's ghoul division) with its progress line ("§6Anteiku: n/3 investigators (go back to Ken)");
  `colour` §6; `joined()` says Anteiku's own line (ghouls and gangs leave you be unless you start it, the CCG won't, RC goes further).
- `NpcTalkView.GANG_COLOURS[4]` Anteiku's brown (the reputation meter clamped gang 4 to the Gas Masks' green).

Still to write (the plan):
- `GangServer.leads` + `Persona.KEN`; `gangOf(KEN)` and `memberOf(KEN or TOUKA)` = ANTEIKU. Ken's first page has room (it never uses
  SAY_3; `EtoServer.withGang` adds the topic). This brings 4.1.67's rules to Ken: marrying Ken while in another gang takes the player out
  of it, and a Ken married to a player in another gang asks them to quit: tell the owner plainly in the notes.
- `ask()`: member "How's Anteiku?"; not yet "I want to join Anteiku."; beans step with 8 cocoa beans in the main hand "Here. Coffee beans.
  (the cocoa beans in your hand)"; done "It's done. The doves are gone."; else "About joining Anteiku...".
- `talk()`: after the 4.1.67 quit check, `if (gang == GangPlayer.ANTEIKU) return ken(player, g, key, seed);`, built like `banjou()`.
  Ken asks why (three answers rotated by seed, keys "gang:why:n"; the right one: "So I don't turn into a monster. And so nobody else has to
  be alone like I was."; wrong: "I want to get stronger. Anteiku's ghouls are the strongest there are." (Aogiri is that way, sorry) and "I
  need somewhere to hide." (everyone here hides from something; that can't be the reason)). Then 8 cocoa beans held (`holdsBeans`/
  `takeBeans` like `holdsCase`/`takeCase`, kept in creative; Touka already calls cocoa beans coffee beans). Then 3 investigators asking
  about the café. Then he takes them in (`set(join)`, `joined`): "*He hangs a second apron on the hook by the door.* ...Welcome to
  Anteiku. Nobody here will ask what you did before. Just... be kind, okay?". Member lines with `wayOut(gang)`; in another gang: he can't
  take someone who belongs to them (it would bring them to the café's door). Ken is polite, gentle, apologetic, fond of coffee and books
  (`KenScript`'s header).
- `GangWords` Anteiku cases in Ken's voice (`asked`, `stay`, `gone`, `demand`); today they fall to the Clowns' `default`.
- Traits: neutral, in `GhoulNpcBrain.wanted` after the gang stance block: an Anteiku member (not creative) is gone for by a ghoul only on
  a grudge (provoked), never by an unprovoked ghoul or gang; `who.ccg()` (the CCG's ghouls) as before; banned-gang outcasts still on
  sight. Capacity: `SharedBar.capacity` +1 for Anteiku (1 to 2; Potential Unlock's 3 to 4), and its client twin (`BikakuClient.capacity`,
  read by the HUD and the menus; the client knows gangs from `GangServer.SEEN`, filled by `S2CGangName`). Size: `ReserveBarView`'s tail
  is one constant (`TAIL` 72, the kakuhou bulb right after it, `WIDTH` 81): make the tail's length a parameter (Anteiku 108, 1.5 times,
  same 9 px height so it never meets the air bubbles above) and have `ReserveHud` pass it.
- No dailies or mask for Anteiku (the owner asked dailies for the CCG only); `inHideout` leaves Anteiku out, so the reputation bar never
  turns on them. Check `GangDaily` and `dailyKill` take gang 4 quietly.
- Tests (`NpcTests.joining` is where the gangs' are): the chain step by step, the name (`named`, `nameTag` source), `GangWords`, the
  neutrality and capacity hooks (source checks as 4.1.67's), the bar's length. Sabotage each. Renders, judged: Ken's Anteiku pages on the
  talk screen (`tools/UiPreview.java gang-talk` mode, 4.1.67), the longer reserve bar beside the normal one (UiPreview's reserve mode),
  the name tag.
- Then docs, version 4.1.70, full build, push, jar, renders.

**After 4.1.70:** the CCG faction through the General (a quest and dailies; "CCG no longer attacks the player. / All ghouls but official
NPCS are hostile towards the player. / Damage increased when fighting ghouls. / Looks: Cyan Blue with a brief cafe symbol" (a briefcase,
the owner's answer); `GeneralScript`; the CCG's agents are `AgentBrain`, its ghoul division `who.ccg()`), the same rails as Anteiku; then
the hotel replacing the Small City (`art/exploration/humanhotel.schem`, the owner's: never altered).

**Prompt for the next session:**

> Continue HexGhoul on branch claude/admiring-johnson-8df58h (fast-forward your session branch onto it first). Read
> docs/handoff/next-ai-4.1.9.md first: the "START HERE (newest, after 4.1.69, handoff)" section, and CLAUDE.md (render and judge everything
> you change; tests passing is not the bar). Finish 4.1.70, the Anteiku faction through Ken: `git apply docs/handoff/wip-4.1.70.patch`,
> then the rest of its plan in that section (Ken's chain, his words, the traits, tests, renders). Then the CCG faction through the General
> with dailies, then the hotel replacing the Small City, one a version. For each version: full build in an isolated copy, push on EXIT 0
> to both branches, send the jar, show the renders. Quote the owner exactly; never claim in-game testing.

## START HERE (after 4.1.69): the Anteiku faction next, then the CCG faction, then the hotel

**This session:** 4.1.56 (`1b444f34`) to 4.1.68 pushed after EXIT 0, jars sent; 4.1.69 Ken's sit (this commit; pushed only after its
full build ended EXIT 0. If the branch's `git log` has no "HexKagune 4.1.69", redo it from below).

**4.1.69, where:** `EtoPose` SIT (`SIT_DROP`, `SIT_LEVEL`, `sitDrop`, `seated`); `PoseFade.blend` (getting up); `CompanionTests` (after the Ayato/Ken pull-out checks);
`tools/CompanionPreview.java sit` (writes `ken-sit.png`, `ken-sit-down.png`, `ken-sit-up.png`). Learned: **a companion's body (EtoModel) is stiff**:
`EtoModel.apply` sets each part's position and rotation only, so a pose's knees and elbows (and any BEND) do nothing on Ken, Touka or
Eto. Render companions with the preview's stiff flag on (`sheet(..., true, ...)`) and pose them with whole limbs. To check contact
(a hand on the floor, a hand on a thigh) measure it: `PoseRender.point(pose, part, local)` gives a part's point in the world (blocks;
the floor at 0); the probes this session used are short Java mains in the scratchpad against `build/quick/classes`.

**Next, the Anteiku faction through Ken** (the owner's words are in the sections below): a quest from Ken to join; its traits (every
ghoul and gang neutral to the player but the CCG; a bigger energy bar, more capacity); its name tag light brown with a coffee cup. Then
the CCG faction through the General (a quest and dailies; the CCG no longer attack; every ghoul but the named ones hostile; more damage
to ghouls; cyan blue with a briefcase). Look at how the gangs were done (4.1.50 joining, 4.1.53 traits and dailies, `GangPlayer`,
`GangServer`, the reputation bar of 4.1.52) and build the factions on the same rails. Render every screen and tag you change.

**Prompt for the next session:**

> Continue HexGhoul on branch claude/admiring-johnson-8df58h (fast-forward your session branch onto it first). Read
> docs/handoff/next-ai-4.1.9.md first: the "START HERE (newest, after 4.1.69)" section, and CLAUDE.md (render and judge everything you
> change; tests passing is not the bar). Next: the Anteiku faction through Ken, then the CCG faction through the General with dailies,
> one a version; then the hotel replacing the Small City. For each version: full build in an isolated copy, push on EXIT 0 to both
> branches, send the jar, show the renders. Quote the owner exactly; never claim in-game testing.

## START HERE (after 4.1.68): Ken's sitting leg next, then the factions, then the hotel

**This session:** 4.1.56 (`1b444f34`) to 4.1.67 pushed after EXIT 0, jars sent; 4.1.68 Ken's tower gone and Ken on Anteiku's roof (this
commit; pushed only after its full build ended EXIT 0. If the branch's `git log` has no "HexKagune 4.1.68", redo it from below).

**4.1.68, where:** `tools/companion_structures.py` (Anteiku's entry now has `"people"`: Touka at (17, 10, 8), Ken at (17, 30, 17) on the
roof deck, both yaw 0; the `cyber_tower` entry gone), `anteiku.nbt` rebuilt by it; the cyber tower's structure, set, pool and template
deleted; out of `PlaceClaims`, `ControlStructures` and `DryLand`'s lists; `Persona` KEN's home "Anteiku's roof". 4.1.67's first build
failed on `SfxTests` ("block.chain.break" is not in the owner's pack): a sound named in code must be remapped by `Sfx` or be in
`SfxTests.KEPT_VANILLA`; leaving a gang now snuffs a candle ("block.candle.extinguish", kept vanilla).

**Next, 4.1.69: Ken's sitting leg.** The owner: "Ken's sitting animation is broken, one of his legs is just in the air." Cause: the
companion model has no knee joints, so SIT's raised left leg (`leftLegX` -2.05 with a `leftKnee` 1.95 the model can't bend) points out
stiff. Fix it with a pose that reads as sitting without knees (both thighs forward, hanging over the roof's edge, or crossed), render it
with `javac -nowarn -cp build/quick/classes:build/quick/base.jar -d <out> tools/PoseRender.java tools/CompanionPreview.java` then
`java -cp <out>:build/quick/classes:build/quick/base.jar CompanionPreview <dir> sit`, judge front and side, redo until it's right.
Then the Anteiku faction through Ken, the CCG faction through the General with dailies (quoted in the sections below), one a version;
then the hotel replacing the Small City.

**Prompt for the next session:**

> Continue HexGhoul on branch claude/admiring-johnson-8df58h (fast-forward your session branch onto it first). Read
> docs/handoff/next-ai-4.1.9.md first: the "START HERE (newest, after 4.1.68)" section, and CLAUDE.md (render and judge everything you
> change; tests passing is not the bar). Next: Ken's sitting leg (4.1.69, rendered front and side), then the Anteiku faction through Ken,
> the CCG faction through the General with dailies, one a version; then the hotel replacing the Small City. For each version: full build
> in an isolated copy, push on EXIT 0 to both branches, send the jar, show the renders. Quote the owner exactly; never claim in-game
> testing.

## START HERE (after 4.1.67): the owner's new list next, then the hotel

**This session:** 4.1.56 (`1b444f34`) to 4.1.66 (`a0a55d6f`) pushed after EXIT 0, jars sent; 4.1.67 the old to-do (this commit; pushed
only after its full build ended EXIT 0. If the branch's `git log` has no "HexKagune 4.1.67", redo it from below).

**4.1.67, where:** `GangPlayer.quit()`; `GangWords` (the leaders' words, pure); `GangServer.quit/married/spouseDemand/nag`; hooks in
`EtoServer.propose` (after `UniqueNpcs.pair(... MARRIED)`), `EtoServer.open` (the greeting), `ControlCommands.marry`; the General's and
Eto's Touka scenes. Learned: `SceneTests` forbids some names anywhere in a talk file, comments included (`SceneTests.FORBIDDEN`): never
quote the owner's words there when they contain one; describe them instead.

**Next, the owner's new list** (quoted in the sections below): Ken onto Anteiku's roof with the cyber tower removed, his sitting leg fixed,
the Anteiku faction through Ken, the CCG faction through the General with dailies (Anteiku light brown with a coffee cup, the CCG cyan
blue with a briefcase). One a version, each rendered and judged (Ken's sitting pose from the front, the side and in first person where it
shows). Then the hotel replacing the Small City (`art/exploration/humanhotel.schem`, the owner's: never altered; `SitePrep`,
`tools/join_panes.py` on the template made from it).

**Prompt for the next session:**

> Continue HexGhoul on branch claude/admiring-johnson-8df58h (fast-forward your session branch onto it first). Read
> docs/handoff/next-ai-4.1.9.md first: the "START HERE (newest, after 4.1.67)" section, and CLAUDE.md (render and judge everything you
> change; tests passing is not the bar). Every screen is in the quest menu's style and the old to-do is done. The owner's order goes on:
> the owner's new list (Ken onto Anteiku's roof with the cyber tower removed, his sitting leg, the Anteiku faction through Ken, the CCG
> faction through the General with dailies), one a version; then the hotel replacing the Small City. For each version: full build in an
> isolated copy, push on EXIT 0 to both branches, send the jar, show the renders. Quote the owner exactly; never claim in-game testing.

## START HERE (after 4.1.66): every screen done; the old to-do next, then the new list, then the hotel

**This session:** 4.1.56 (`1b444f34`) to 4.1.65 (`b428e10b`) pushed after EXIT 0, jars sent; 4.1.66 the cookbook (this commit; pushed only
after its full build ended EXIT 0. If the branch's `git log` has no "HexKagune 4.1.66", redo it from below).

**4.1.66, where:** `CookbookView` (pure), `CookbookScreen` through it; `UiCanvas.item/texture/tr/itemTooltip` (use them for any view
that needs items, textures or translated words; `tools/UiPreview.java`'s canvas loads the mod's PNGs and lang); the stale allowlist
entry for `renderBackground` removed (the verifier insists the allowlist is an exact inventory). Learned again: a sabotage that does not
break the rule proves nothing (a grid moved 60 px stayed inside the box); sabotage the rule itself (dishes past the tenth unclickable).

**Every screen in the quest menu's style:** the quest menu (4.1.34), the talk screen (4.1.55), Ukaku (4.1.56), Rinkaku (4.1.58), Bikaku
(4.1.59), Graft (4.1.60), Pale (4.1.61), the kakugan screen (4.1.62), the gender screen (4.1.63), the path choice (4.1.64), the king's choice
(4.1.65), the cookbook (4.1.66). `grep -rl "extends Screen" src/main/java` lists no other.

**Next, the old to-do** (each its own version, rendered and judged where it shows): Ayato's and Touka's reactions to the player's marriage,
a spouse's reactions to gang life, "Quit the gang", the marriage and membership rules (see the to-do sections below). Then the owner's new
list: Ken onto Anteiku's roof with the cyber tower removed, his sitting leg fixed, the Anteiku faction through Ken, the CCG faction through
the General with dailies (Anteiku light brown with a coffee cup, the CCG cyan blue with a briefcase). Then the hotel replacing the Small
City (the owner's `art/exploration/humanhotel.schem`; use `SitePrep` and `tools/join_panes.py` on its template).

**Prompt for the next session:**

> Continue HexGhoul on branch claude/admiring-johnson-8df58h (fast-forward your session branch onto it first). Read
> docs/handoff/next-ai-4.1.9.md first: the "START HERE (newest, after 4.1.66)" section, and CLAUDE.md (render and judge everything you
> change; tests passing is not the bar). Every screen is now in the quest menu's style. The owner's order goes on: the old to-do items
> (Ayato/Touka marriage reactions, a spouse's reactions to gang life, "Quit the gang", the marriage and membership rules); then the owner's
> new list (Ken onto Anteiku's roof with the cyber tower removed, his sitting leg, the Anteiku faction through Ken, the CCG faction through
> the General with dailies); then the hotel replacing the Small City. For each version: full build in an isolated copy, push on EXIT 0 to
> both branches, send the jar, show the renders. Quote the owner exactly; never claim in-game testing.

## START HERE (after 4.1.65): the cookbook next, then the old to-do, the new list, the hotel

**This session:** 4.1.56 (`1b444f34`), 4.1.57 (`2374d4d1`), 4.1.58 (`6d7a8435`), 4.1.59 (`a5a82567`), 4.1.60 (`b504afde`), 4.1.61 (`beefc7bd`),
4.1.62 (`9cc0e197`), 4.1.63 (`2d9ac99a`), 4.1.64 (`4f55b5ab`) pushed after EXIT 0, jars sent; 4.1.65 the king's choice (this commit; pushed
only after its full build ended EXIT 0. If the branch's `git log` has no "HexKagune 4.1.65", redo it from below).

**4.1.65, where:** `KingChoiceView` in the quest box (`BOX_WIDTH`, `CARD_Y` 66, the cards 28 apart, "choose" a quest button), its card art
and `WAR_ROWS`/`PEACE_ROWS` untouched (the Rinkaku menu uses them); `KingChoiceScreen` scales the box; `QuestTests.kingChoice`. Learned:
a sabotage run (proving a test can fail) compiles the broken code into `build/quick`; recompile after restoring before rendering, or the
render shows the sabotage (it happened once here and was caught on the render).

**Done so far of "every menu":** the talk screen (4.1.55), the five mastery menus (4.1.56, 4.1.58 to 4.1.61), the kakugan screen, the
gender screen, the path choice, the king's choice (4.1.62 to 4.1.65); the quest menu itself (4.1.34). **Next, the cookbook**
(`CookbookScreen`, drawn straight on `GuiGraphics`: move its drawing into a pure view first, as `PathChoiceView` in 4.1.64, so it renders).
Then check for any other screen left (`grep -l "extends Screen" src/main/java -r`), then the old to-do and the owner's new list (above).

**Prompt for the next session:**

> Continue HexGhoul on branch claude/admiring-johnson-8df58h (fast-forward your session branch onto it first). Read
> docs/handoff/next-ai-4.1.9.md first: the "START HERE (newest, after 4.1.65)" section, and CLAUDE.md (render and judge everything you
> change; tests passing is not the bar). The owner's order: the menus in the quest menu's style, one a version (the cookbook next, then any
> screen left), each rendered and shown; then the old to-do items; then the owner's new list (Ken onto Anteiku's roof with the cyber tower
> removed, his sitting leg, the Anteiku faction through Ken, the CCG faction through the General with dailies); then the hotel replacing
> the Small City. For each version: full build in an isolated copy, push on EXIT 0 to both branches, send the jar, show the renders.
> Quote the owner exactly; never claim in-game testing.

## START HERE (after 4.1.64): the menus go on (the king's choice next), then the old to-do, the new list, the hotel

**This session:** 4.1.56 (`1b444f34`), 4.1.57 (`2374d4d1`), 4.1.58 (`6d7a8435`), 4.1.59 (`a5a82567`), 4.1.60 (`b504afde`), 4.1.61 (`beefc7bd`),
4.1.62 (`9cc0e197`), 4.1.63 (`2d9ac99a`) pushed after EXIT 0, jars sent; 4.1.64 the path choice (this commit; pushed only after its full
build ended EXIT 0. If the branch's `git log` has no "HexKagune 4.1.64", redo it from below).

**4.1.64, where:** `PathChoiceView` (pure; the screen's drawing moved there so it renders with `tools/UiPreview.java <dir> path-choice`);
`PathChoiceLayout` untouched (its tests sweep every size; the board is the screen less `PathChoiceView.INSET`, since the path cards
need the room). `PathScreenTests.layouts` (board, rule) and `drawnByView`.

**Next, the king's choice** (`KingChoiceScreen`; its cards are `KingChoiceView`, already drawn on `UiCanvas` and in the Rinkaku menu),
then the cookbook (`CookbookScreen`): the same way, each rendered, judged and shown. Then the old to-do and the owner's new list (above).

**Prompt for the next session:**

> Continue HexGhoul on branch claude/admiring-johnson-8df58h (fast-forward your session branch onto it first). Read
> docs/handoff/next-ai-4.1.9.md first: the "START HERE (newest, after 4.1.64)" section, and CLAUDE.md (render and judge everything you
> change; tests passing is not the bar). The owner's order: the menus in the quest menu's style, one a version (the king's choice next,
> then the cookbook), each rendered and shown; then the old to-do items; then the owner's new list (Ken onto Anteiku's roof with the
> cyber tower removed, his sitting leg, the Anteiku faction through Ken, the CCG faction through the General with dailies); then the hotel
> replacing the Small City. For each version: full build in an isolated copy, push on EXIT 0 to both branches, send the jar, show the
> renders. Quote the owner exactly; never claim in-game testing.

## START HERE (after 4.1.63): the menus go on (the path choice next), then the old to-do, the new list, the hotel

**This session:** 4.1.56 (`1b444f34`), 4.1.57 (`2374d4d1`), 4.1.58 (`6d7a8435`), 4.1.59 (`a5a82567`), 4.1.60 (`b504afde`), 4.1.61 (`beefc7bd`),
4.1.62 (`9cc0e197`) pushed after EXIT 0, jars sent; 4.1.63 the gender screen (this commit; pushed only after its full build ended EXIT 0.
If the branch's `git log` has no "HexKagune 4.1.63", redo it from below).

**4.1.63, where:** `GenderView.Layout.box` (440 x 300 at most), the old layout inside it; `GenderView` drawn in the quest menu's pieces
(as `KakuganView` in 4.1.62). `FamilyTests.screen`.

**Next, the path choice** (`PathChoiceScreen`, `PathChoiceLayout`, `PathCards`; tested in `PathScreenTests`), then the king's choice
(`KingChoiceScreen`, its cards already `KingChoiceView`), the cookbook (`CookbookScreen`): the same way, each rendered, judged and shown.
Then the old to-do and the owner's new list (above).

**Prompt for the next session:**

> Continue HexGhoul on branch claude/admiring-johnson-8df58h (fast-forward your session branch onto it first). Read
> docs/handoff/next-ai-4.1.9.md first: the "START HERE (newest, after 4.1.63)" section, and CLAUDE.md (render and judge everything you
> change; tests passing is not the bar). The owner's order: the menus in the quest menu's style, one a version (the path choice next),
> each rendered and shown; then the old to-do items; then the owner's new list (Ken onto Anteiku's roof with the cyber tower removed, his
> sitting leg, the Anteiku faction through Ken, the CCG faction through the General with dailies); then the hotel replacing the Small
> City. For each version: full build in an isolated copy, push on EXIT 0 to both branches, send the jar, show the renders. Quote the owner
> exactly; never claim in-game testing.

## START HERE (after 4.1.62): the menus go on (the gender screen next), then the old to-do, the new list, the hotel

**This session:** 4.1.56 (`1b444f34`), 4.1.57 (`2374d4d1`), 4.1.58 (`6d7a8435`), 4.1.59 (`a5a82567`), 4.1.60 (`b504afde`), 4.1.61 (`beefc7bd`)
pushed after EXIT 0, jars sent; 4.1.62 the kakugan screen (this commit; pushed only after its full build ended EXIT 0. If the branch's
`git log` has no "HexKagune 4.1.62", redo it from below).

**4.1.62, where:** the kakugan screen keeps its own responsive layout in GUI pixels (no canvas scaling: dragging stays exact), now inside
`KakuganScreenLayout.box`; `KakuganView` draws the quest menu's pieces round it (`QuestStyle.backdrop`, title, rule, tile, buttons). Its
tests (`KakuganTests.screen`) sweep every screen size. A screen that is a layout in GUI pixels (the gender screen's `GenderView.Layout`
too) is remade the same way: a box inside the screen, the old layout inside the box.

**Next, the gender screen** (`GenderView`/`GenderScreen`, after the kakugan screen's Next), then the path choice (`PathChoiceScreen`), the
king's choice (`KingChoiceScreen`), the cookbook (`CookbookScreen`): the same way, each rendered, judged and shown.

**Prompt for the next session:**

> Continue HexGhoul on branch claude/admiring-johnson-8df58h (fast-forward your session branch onto it first). Read
> docs/handoff/next-ai-4.1.9.md first: the "START HERE (newest, after 4.1.62)" section, and CLAUDE.md (render and judge everything you
> change; tests passing is not the bar). The owner's order: the menus in the quest menu's style, one a version (the gender screen next,
> as the kakugan screen in 4.1.62), each rendered and shown; then the old to-do items; then the owner's new list (Ken onto Anteiku's roof
> with the cyber tower removed, his sitting leg, the Anteiku faction through Ken, the CCG faction through the General with dailies); then
> the hotel replacing the Small City. For each version: full build in an isolated copy, push on EXIT 0 to both branches, send the jar,
> show the renders. Quote the owner exactly; never claim in-game testing.

## START HERE (after 4.1.61): the menus go on (the kakugan screen next), then the old to-do, the new list, the hotel

**This session:** 4.1.56 (`1b444f34`), 4.1.57 (`2374d4d1`), 4.1.58 (`6d7a8435`), 4.1.59 (`a5a82567`), 4.1.60 (`b504afde`) pushed after EXIT
0, jars sent; 4.1.61 the Pale menu (this commit; pushed only after its full build ended EXIT 0. If the branch's `git log` has no
"HexKagune 4.1.61", redo it from below). All five mastery menus (Ukaku, Rinkaku, Bikaku, Graft, Pale) are now on `QuestFrame`.

**4.1.61, where:** `PaleMenuView` on `QuestFrame` (four tabs; UNLOCK mirrors `PaleRevive.unlock`: `PaleMutation.canUnlock`, no quests on this
path; the Instant Big Punch switch mirrors `PaleRevive.instantPunch`: Witness This had); the moves' numbers are the ones the server spends
(checked in `PaleMoves`, `PaleOwlMoves`, `OwlWatch`, `OwlGrab`: the Owl's Whirl waits `SWIPE_COOLDOWN`, its spikes `GRAB_COOLDOWN`);
`PathSilhouette.owl`; `QuestTests.paleMenu` (proven able to fail: UNLOCK without a point, the switch without Witness This, the Owl
overflowing its tile). Learned: card icons slide in from the left for a quarter second, so text in an icon is checked after the slide.

**Next, the kakugan screen** (`KakuganScreen`/`KakuganView`), then the path choice, the king's choice, the cookbook, the gender screen: the
same way, each rendered, judged and shown, a `QuestTests` check with `Recorder`/`boxed`.

**Prompt for the next session:**

> Continue HexGhoul on branch claude/admiring-johnson-8df58h (fast-forward your session branch onto it first). Read
> docs/handoff/next-ai-4.1.9.md first: the "START HERE (newest, after 4.1.61)" section, and CLAUDE.md (render and judge everything you
> change; tests passing is not the bar). The owner's order: the menus in the quest menu's style, one a version (the kakugan screen next,
> on `QuestFrame` as the five mastery menus), each rendered and shown; then the old to-do items; then the owner's new list (Ken onto
> Anteiku's roof with the cyber tower removed, his sitting leg, the Anteiku faction through Ken, the CCG faction through the General with
> dailies); then the hotel replacing the Small City. For each version: full build in an isolated copy, push on EXIT 0 to both branches,
> send the jar, show the renders. Quote the owner exactly; never claim in-game testing.

## START HERE (after 4.1.60): the menus go on (Pale next), then the old to-do, the new list, the hotel

**This session:** 4.1.56 (`1b444f34`), 4.1.57 (`2374d4d1`), 4.1.58 (`6d7a8435`), 4.1.59 (`a5a82567`) pushed after EXIT 0, jars sent; 4.1.60
the Graft menu (this commit; pushed only after its full build ended EXIT 0. If the branch's `git log` has no "HexKagune 4.1.60", redo it
from below).

**A slip to learn from (4.1.59):** its first commit (`2631c53f`) was pushed to both branches although its full build had ended EXIT 1
(`BikakuWormTests.walkerLift` still read the old menu's words), because the push command did not wait for the EXIT line. `a5a82567` fixed
the test and was pushed after EXIT 0; the owner was told. Always: `grep -q "^EXIT 0" log && push`; and before the build, grep
`src/test/java` for every class and file you touched and run those suites (`BikakuWormTests` reads `BikakuMenuView`'s source).

**4.1.60, where:** `GraftMenuView` on `QuestFrame` (three tabs; GROW INTO IT mirrors `GraftServer.pickStage`: mastery, `Gates.graft`, never
the kakuja, never `model.stage()` (in the kakuja too: it rests on that stage, its card says GROWN · UNDER THE KAKUJA); UNLOCK mirrors
`GraftServer`'s unlock: `GraftMutation.canUnlock` with the quest points and `Gates.graft(mutation)`); `PathSilhouette.graftForm` (each
stage drawn from `GraftRig.pose`, the world's own rest pose; look per `GraftModel`; `tools/UiPreview.java <dir> graft-menu` writes
`graft-forms-a/b.png`, `-Dgraft.yaws=0.3,0.7` other angles); `QuestFrame.opener` now quotes ("the quest "Four Thorns"", "Noro's trial
"The Walker""); `QuestTests.graftMenu` (proven able to fail: GROW in the kakuja, ticks on a shut stage, a tile overflowing). Learned: a
thick `Stroke` is drawn in squares, so a tube must be a chain of `Disc`s where it can be seen end-on (`PathSilhouette.tube`).

**Next, the Pale menu** (`PaleMenuView`/`Screen`), then the kakugan screen, the path choice, the king's choice, the cookbook, the gender
screen: the same way, each rendered, judged and shown, a `QuestTests` check with `Recorder`/`boxed`.

**Prompt for the next session:**

> Continue HexGhoul on branch claude/admiring-johnson-8df58h (fast-forward your session branch onto it first). Read
> docs/handoff/next-ai-4.1.9.md first: the "START HERE (newest, after 4.1.60)" section, and CLAUDE.md (render and judge everything you
> change; tests passing is not the bar). The owner's order: the menus in the quest menu's style, one a version (Pale next, on
> `QuestFrame` as the Ukaku, Rinkaku, Bikaku and Graft menus), each rendered and shown; then the old to-do items; then the owner's new
> list (Ken onto Anteiku's roof with the cyber tower removed, his sitting leg, the Anteiku faction through Ken, the CCG faction through
> the General with dailies); then the hotel replacing the Small City. For each version: full build in an isolated copy, push on EXIT 0
> to both branches, send the jar, show the renders. Quote the owner exactly; never claim in-game testing.

## START HERE (after 4.1.59): the menus go on (the Graft next), then the old to-do, the new list, the hotel

**This session:** 4.1.56 (`1b444f34`), 4.1.57 (`2374d4d1`), 4.1.58 (`6d7a8435`) pushed after EXIT 0, jars sent; 4.1.59 the Bikaku menu (this
commit; pushed only after its full build ended EXIT 0. If the branch's `git log` has no "HexKagune 4.1.59", redo it from below).

**4.1.59, where:** `BikakuMenuView` on `QuestFrame` (four tabs; gates mirrored from `BikakuServer`: forms past one worm `BIKAKU_WORMS`, The
Walker `BIKAKU_WALKER` too; Pursuit/Eruption/Regeneration `BIKAKU_WORMS`; the kakuja's `BIKAKU_KAKUJA`; mutations `BIKAKU_DEVOURING`/
`BIKAKU_POTENTIAL`); USE THIS FORM "formN" (the screen's old request, now only from the button); `PathSilhouette.bikakuForm`;
`QuestFrame.opener` (a lesson-gated step names its teacher: `Teaching.ALL`); `QuestTests.bikakuMenu`. Learned: many gates are opened by a
teacher's lesson (`Teaching`), not a `QuestBook` quest: name them with `QuestFrame.opener`, never `QuestBook.openersText` alone.

**Next, the Graft menu** (`GraftMenuView`/`Screen`), then Pale (`PaleMenuView`/`Screen`), the kakugan screen, the path choice, the king's
choice, the cookbook, the gender screen: the same way, each rendered, judged and shown, a `QuestTests` check with `Recorder`/`boxed`.

**Prompt for the next session:**

> Continue HexGhoul on branch claude/admiring-johnson-8df58h (fast-forward your session branch onto it first). Read
> docs/handoff/next-ai-4.1.9.md first: the "START HERE (newest, after 4.1.59)" section, and CLAUDE.md (render and judge everything you
> change; tests passing is not the bar). The owner's order: the menus in the quest menu's style, one a version (the Graft next, on
> `QuestFrame` as the Ukaku, Rinkaku and Bikaku menus), each rendered and shown; then the old to-do items; then the owner's new list
> (Ken onto Anteiku's roof with the cyber tower removed, his sitting leg, the Anteiku faction through Ken, the CCG faction through the
> General with dailies); then the hotel replacing the Small City. For each version: full build in an isolated copy, push on EXIT 0 to
> both branches, send the jar, show the renders. Quote the owner exactly; never claim in-game testing.

## START HERE (after 4.1.58): the menus go on (Bikaku next), then the old to-do, the new list, the hotel

**This session:** 4.1.56 the Ukaku menu (`1b444f34`), 4.1.57 the structures and the glass (`2374d4d1`), each pushed after EXIT 0 and its jar
sent; 4.1.58 the Rinkaku menu (this commit; pushed only after its full build ended EXIT 0. If the branch's `git log` has no "HexKagune
4.1.58", redo it from below). The owner after 4.1.57 (quoted exactly): "COntinue, no one told you to stop".

**4.1.58, where:** `RinkakuMenuView` on `QuestFrame` as the Ukaku menu (tabs FORMS and KING "FINAL KAKUJA"; the cards, the detail, the
road; WEAR THIS FORM "form:ID" and CHOOSE ON ITS CARDS "king:ID" drawn only when they may be used; the chosen king's emblem in the header's
middle instead of the old whole-board themes: `KingTheme` is still used by `KingChoiceView`); `PathSilhouette.rinkakuForm`;
`KingChoiceView` emblems shared (`warEye`, `peaceCross`, the rows); `QuestTests.rinkakuMenu` (with `Recorder` and `boxed`: reuse them for the
next menus). Rule learned: the name check (`build.check_names`) refuses a character's name in a class's strings: the old menu spelled the
Peace title letter by letter; the new one says "RINKAKU :RE". Choice the owner may want changed: Peace's white board is now the quest
box with Peace's emblem (asked in the release notes).

**Next, the Bikaku menu** (`BikakuMenuView`/`Screen`): the same way (box, header with a bar to the next step, tabs, cards on the left, the
picked one on the right as a quest with its tile, a road), its energy bar and Potential Unlock, its forms and its quest-unlocked mutations
(Grand Devouring etc.); keep the screen's requests; a `QuestTests` check with `Recorder`/`boxed`; `UiPreview` mode; render, judge, show.
Then the Graft, Pale, Kakugan, the path choice, the king's choice, the cookbook, the gender screen.

**Prompt for the next session:**

> Continue HexGhoul on branch claude/admiring-johnson-8df58h (fast-forward your session branch onto it first). Read
> docs/handoff/next-ai-4.1.9.md first: the "START HERE (newest, after 4.1.58)" section, and CLAUDE.md (render and judge everything you
> change; tests passing is not the bar). The owner's order: the menus in the quest menu's style, one a version (Bikaku next, on
> `QuestFrame` as the Ukaku and Rinkaku menus), each rendered and shown; then the old to-do items; then the owner's new list (Ken onto
> Anteiku's roof with the cyber tower removed, his sitting leg, the Anteiku faction through Ken, the CCG faction through the General with
> dailies); then the hotel replacing the Small City. For each version: full build in an isolated copy, push on EXIT 0 to both branches,
> send the jar, show the renders. Quote the owner exactly; never claim in-game testing.

## START HERE (after 4.1.57): the menus go on (Rinkaku next), then the old to-do, the new list, the hotel

**This session:** 4.1.56 the Ukaku menu (`1b444f34`, pushed after EXIT 0, jar sent); 4.1.57 the structures and the glass (this commit;
pushed only after its full build ended EXIT 0, jar sent. If the branch's `git log` has no "HexKagune 4.1.57", redo it from below).

**4.1.57, what and where** (the owner's words quoted in the section after 4.1.56 below, and in the release notes):
- Why they floated: `DryLand.lift` (4.1.17) set each jigsaw place on its footprint's highest ground (a mound over its low side: the circus
  10 blocks up on a slope); most templates are saved without their air (`keep_air: False`), so the hill and trees stayed inside; places
  were built at `surface_structures`, before the trees.
- Now: `SitePrep` (pure) and `ControlStructures.clearing`/`apron`. The clearing's tiles (`hexkagune:clearing`, 16^3 of air, vanilla pool
  pieces with ground delta 0, laid first in the pieces' list) take everything over the floor across the footprint, and over the bank's top
  across the footprint and 8 blocks round it, up to 32 over the highest ground, within the start's reach (128 back, 143 on); every tile's
  ground delta (`SitePrep.groundDelta`) puts its ground at the floor, so the beard (`beard_box`/`beard_thin`, kept) carves the site to the
  floor and fills under it, never raising land up to a high tile's foot (a tile is told apart by a ground delta of 0 or less, 16 across). The floor is
  the footprint's median ground (`SitePrep.floor`; the Site's `middle` already was). `apron` (after the place, in each chunk): flat 2
  round it, then a bank a block a block (cut, its top skinned with the ground's own top block; built up by copying the ground under);
  never under the place itself (its foundation and the beard fill there; a gap at its floor may be its own pool or stairwell: the mansion
  has water at its ground layer); trees, snow and plants never ground. Every place's JSON step is now
  `top_layer_modification` (after the trees); the Small City's not (it goes, for the hotel).
- Glass: `tools/join_panes.py` joins every pane and bar in every template as `IronBarsBlock.attachsTo` does (panes, bars, walls, full
  faces; not leaves, pumpkins, melons, barriers, shulker boxes; stairs and trapdoors kept as the template has them), across tile edges;
  its `--check` runs in `NpcTests.sitePrep`. Its rule changed nothing in the game-saved templates (Anteiku, the mansion, the library: a check
  the rule is the game's). Run it after any template is rebuilt (`tools/ccg_tower.py` still writes the panes unjoined; the tool fixes the
  tiles).
- Proof renders: `ground-circus-4.1.57.png`, `ground-gas_hideout-4.1.57.png` (`tools/SitePrepPreview.java <out> <template>`: a wooded
  hillside, the old and new placement from the real template and `SitePrep`; it leaves out the game's beard), `ccg-glass-4.1.57.png`.
- Not done: worlds whose places were started before 4.1.57 keep their old pieces where not yet generated. The beard (`beard_box`) is kept.

**The order now:** the menus, one a version (Rinkaku next, on `QuestFrame` as the Ukaku menu: see the 4.1.56 notes below) -> the old
to-do (marriage reactions, a spouse's reactions to gang life, "Quit the gang", the marriage and membership rules) -> the owner's new list
(Ken onto Anteiku's roof and the cyber tower removed, his sitting leg, the Anteiku and CCG factions) -> the hotel (the Small City erased; a
hotel from `art/exploration/humanhotel.schem` by a village, humans working, living and walking round it, CCG patrols always; when it is
built, clear and set it with `SitePrep` like every place, run `tools/join_panes.py` on its template).

**Prompt for the next session:**

> Continue HexGhoul on branch claude/admiring-johnson-8df58h (fast-forward your session branch onto it first). Read
> docs/handoff/next-ai-4.1.9.md first: the "START HERE (newest, after 4.1.57)" section, and CLAUDE.md (render and judge everything you
> change; tests passing is not the bar). The owner's order: the menus in the quest menu's style, one a version (Rinkaku next, on
> `QuestFrame` as the Ukaku menu in 4.1.56), each rendered and shown; then the old to-do items; then the owner's new list (Ken onto
> Anteiku's roof with the cyber tower removed, his sitting leg, the Anteiku faction through Ken, the CCG faction through the General with
> dailies); then the hotel replacing the Small City. For each version: full build in an isolated copy, push on EXIT 0 to both branches,
> send the jar, show the renders. Quote the owner exactly; never claim in-game testing.

## START HERE (after 4.1.56): 4.1.57 the structures on the ground (the owner's order), then the menus go on

**This session:** 4.1.56 the Ukaku menu in the quest menu's style (this commit; pushed only after its full build ended EXIT 0, jar sent. If
the branch's `git log` has no "HexKagune 4.1.56", the session ended first: redo it from the notes below).

**The owner, this session (quoted exactly), in the order they came:**
1. With the schematic `humanhotel.schem` (kept as sent: `art/exploration/humanhotel.schem`): "To-do list addition: / from now on the small
   city is gone, erased. / From now on close to a village will be an hotel where humans work, live and walk around doing their own thing.
   / CCG patrols around the hotel at all times."
2. "bugs: / Be careful with the glass placement on the structures, the CCG building has all glass pannels unconnected and not attached to
   eachother!"
3. "Tente novamente" (Portuguese: "try again", after the session's worker restarted; answered in English as the rules say).
4. Mid-4.1.56: "AFTER this, please for the love of god, FIX THE FUCKING STRUCTURE SPAWNING PLACEMENT, i asked multiple times to fix it,
   CLEAR THE FUCKING AREA OF SPAWN, CLEAR ALL BLOCKS OR TREES, AND THEN PLACE IT DIRECTLY ON THE FLOOR, NOT FLYING, NOT HOVERING OVER THE
   FLOOR, I SAID ON IT."

**The order now:** 4.1.57 the structures (every place the mod builds: clear its whole footprint of blocks and trees above the floor, set
it directly on the ground, never floating or hovering; render it from the side to prove it; and the glass panes joined, the CCG building's
first) -> the menus, one a version (Rinkaku next, then Bikaku, the Graft, Pale, Kakugan, the path choice, the king's choice, the cookbook,
the gender screen) -> the old to-do (marriage reactions, a spouse's reactions to gang life, "Quit the gang", the marriage and membership
rules) -> the owner's new list (Ken onto Anteiku's roof, his sitting leg, the Anteiku and CCG factions) -> the hotel (the Small City
erased, a hotel by a village from the owner's schematic, humans working, living and walking round it, CCG patrols round it always). The
hotel came as "To-do list addition", so it is last unless the owner says otherwise.

**4.1.57 notes (not started):** the places are built by `ControlStructures` (`foundation`, `afterPlace` of City, Site, DryPlace;
`DryLand.footing`, `DryLand.lift`, `DryPlace.flattest`, `PlaceClaims`). Earlier fixes (4.1.12, 4.1.14, 4.1.42, 4.1.44) chose flat ground and
filled under the footprint; the owner still sees them floating, so check the place's Y against the real surface (heightmap
WORLD_SURFACE_WG vs OCEAN_FLOOR_WG, leaves and logs counted as ground by the heightmap is the likely cause of "hovering": a tree's canopy
taken for the floor), clear everything above the floor in the footprint (trees whole, leaves, logs, plants, snow), fill below to the
footprint's floor, and place the template's floor on the ground. Glass panes: a template placed without neighbour updates leaves panes'
and bars' north/south/east/west false: set their connections from their neighbours after placing (or use `Block.updateFromNeighbourShapes`
on each pane), or fix the states in the template itself (`tools/ccg_tower.py`, `tools/companion_structures.py`). Stubs and allowlist
lines need SRG evidence (the house rules).

**4.1.56, where:** `client/QuestFrame` (the quest menu's pieces for every menu: `plate`, `header`, `chipLeft`, `tabs`, `card`, `section`,
`story`, `checkRow`, `row`, `button`, `road`, `tooltip`, `stamp`, `badge`); `UkakuMenuView` on it (box 740 x 440; tabs ORGAN/MUTATIONS; picks
"stage:", "unlock:", "pick:"; the UNLOCK button "mutation:N" only while it can be spent; `effects` the WITH IT numbers); `UkakuMenuScreen`
(no overlay, keeps tab and pick); `PathSilhouette.ukakuKakuja`; `QuestTests.ukakuMenu` (2 million checks: all inside the box, nothing
clickable over anything else, every word inside, the UNLOCK button only when a point can be spent). Render: `docs/handoff/ukaku-menu-4.1.56.png`
(`UiPreview <dir> ukaku`). **For the next menus:** draw them on `QuestFrame` the same way (the box, header, tabs, cards on the left, the
picked one on the right as a quest, a road at the bottom), keep their old `Model` constructors, give the screen its tab and pick, and add a
`QuestTests` check like `ukakuMenu`. Choices of mine the owner may want changed: the box's size (740 x 440, larger than the quest menu's
640 x 380 to hold the readout), the stage stories' words, the glyphs.

**Prompt for the next session:**

> Continue HexGhoul on branch claude/admiring-johnson-8df58h (fast-forward your session branch onto it first). Read
> docs/handoff/next-ai-4.1.9.md first: the "START HERE (newest, after 4.1.56)" section, and CLAUDE.md (render and judge everything you
> change; tests passing is not the bar). The owner's order: 4.1.57 first, the structures: clear every place's footprint of blocks and
> trees, set it directly on the ground, never floating or hovering (render it from the side to prove it), and join the glass panes (the
> CCG building's); then the menus in the quest menu's style, one a version (Rinkaku next, on `QuestFrame` as the Ukaku menu in 4.1.56);
> then the old to-do, the owner's new list, and the hotel. For each version: full build in an isolated copy, push on EXIT 0 to both
> branches, send the jar, show the renders. Quote the owner exactly; never claim in-game testing.

## START HERE (after 4.1.55, handoff): 4.1.56 the Ukaku menu next (planned, nothing written)

The owner stopped the session (quoted exactly): "Handoff + prompt now." **Pushed this session**, each after a full build EXIT 0 on its exact
commit, jar sent: 4.1.54 the Gas Masks and Banjou (`75e56ee0`), 4.1.55 the talk screen in the quest menu's style (`bd083772`; its full build
was running when the owner asked for the handoff, and this commit, docs only, was pushed after it ended EXIT 0). If `git log` shows no
"HexKagune 4.1.55" on the branch, the session ended first: check out `bd083772` if it exists, else redo 4.1.55 from its notes below.

**The order (the owner's answers this session):** the menus, one a version, each rendered and shown -> the old to-do (marriage reactions,
a spouse's reactions to gang life, "Quit the gang", the marriage and membership rules) -> the owner's new list (quoted in the section after
4.1.54: Ken onto Anteiku's roof and the cyber tower gone, his sitting leg, the Anteiku faction through Ken, the CCG faction through the
General; Anteiku light brown with a coffee cup, the CCG cyan blue with a briefcase: their answer "A briefcase (Recommended)").

**4.1.56 plan, the Ukaku menu (`client/UkakuMenuView`, `UkakuMenuScreen`), worked out, not written:**
- Today: the organic board (`organicPanel`), three columns: CURRENT ORGAN rows, MASTERY bar, UNLOCKS list, KAKUJA FORMS boxes; LIVE RC
  RESPONSE (the wing silhouette `PathSilhouette.ukaku`) with the stage buttons ONE/TWIN/FULL/KAK I/KAK II (display only), COMBAT READOUT,
  KAKUGAN + "ADJUST KAKUGAN" (hit "eyes": opens `KakuganScreen`); MUTATIONS cards (hit "mutation:N": the screen sends C2SUkakuAction.MUTATE
  at once). Render it: `java -Djava.awt.headless=true -cp build/quick/classes:build/quick/base.jar tools/UiPreview.java <dir>` (the default
  mode writes ukaku-menu-*.png and the other menus).
- To: the quest box (`QuestStyle.backdrop`, 900 x 480 inside the 960 x 520 space); header as the journal's: "UKAKU" title, "RC CELL /
  BLACK RABBIT MATRIX  ·  MASTERY 760" in red, top right the bar from the last unlock to the next ("760 / 800  ·  next: Rushing Death (Z),
  Thorn Fan": `UkakuUnlock.next`), chips: points to spend (gold), kagune out/in ("R to retract"); the red rule; tabs ORGAN and MUTATIONS
  (a badge: points to spend). ORGAN: the five `UkakuStage`s as quest cards on the left (a wing glyph tile, the name, a chip CURRENT /
  REACHED / "at mastery 750" / Kakuja's "tap K" "hold K", a bar to it); on the right the live preview in a framed tile, the organ's rows,
  COMBAT READOUT (`section`), KAKUGAN with an ADJUST button (keep hit "eyes"), and the unlocks as the journal's "YOUR ROAD" (nodes at each
  `UkakuUnlock` mastery, hover: its text). MUTATIONS: the mutations as cards (UNLOCKED / CLICK TO UNLOCK / NEEDS A POINT / NEEDS ITS QUEST),
  a click picks one (new hit "pick:N"), the right side shows it (title, its description as a quest story, "Points at mastery ...") and an
  UNLOCK button (hit "mutation:N": the screen's MUTATE, unchanged), so a stray click spends nothing. The screen keeps the tab and the pick
  (new Model fields, old constructors kept).
- Put the shared pieces in one helper the other menus reuse (header, tabs, card, section, button, road): `QuestJournalView`'s `header`,
  `tabs`, `list`, `section` and `roadmap` are private and journal-specific; `NpcTalkView.section`/`card` (4.1.55) are the model.
- Then Rinkaku, Bikaku, the Graft, Pale, Kakugan, the path choice, the king's choice, the cookbook, the gender screen, one a version (the
  list in the section below). Render each with `UiPreview`, judge it against `docs/handoff/quests-box-4.1.34.png`, show it.

**Prompt for the next session:**

> Continue HexGhoul on branch claude/admiring-johnson-8df58h (fast-forward your session branch onto it first). Read
> docs/handoff/next-ai-4.1.9.md first: the "START HERE (newest, after 4.1.55, handoff)" section, and CLAUDE.md (render and judge
> everything you change; tests passing is not the bar). The owner's order: every menu remade in the quest menu's style, one menu per
> version, each rendered and shown to them (the talk screen is done in 4.1.55; next 4.1.56 the Ukaku menu, planned in that section, then
> Rinkaku, Bikaku, the Graft, Pale and the rest); then the old to-do items (Ayato/Touka marriage reactions, a spouse's reactions to gang
> life, "Quit the gang", the marriage and membership rules); then the owner's new list (Ken onto Anteiku's roof with the cyber tower
> removed, his sitting leg fixed, the Anteiku faction through Ken, the CCG faction through the General with dailies; Anteiku light brown
> with a coffee cup, the CCG cyan blue with a briefcase). For each version: full build in an isolated copy, push on EXIT 0 to both
> branches, send the jar, show the renders. Quote the owner exactly; never claim in-game testing.

## START HERE (after 4.1.55): the menus, one a version; then the old to-do; then the owner's new list

**Pushed this session** (each after a full build EXIT 0 on its exact commit, jar sent): 4.1.54 the Gas Masks and Banjou (`75e56ee0`),
4.1.55 the talk screen in the quest menu's style (its commit carries this section).

**The owner's answers this session (AskUserQuestion), the agreed order now:** "Menus first (Recommended)" -> the older to-do items "Before
the new factions" -> the new list. The CCG faction's symbol: "A briefcase (Recommended)". So:
1. **Every menu in the quest menu's style, one a version, each rendered and shown** (`QuestStyle`, `QuestJournalView`: the plate, header,
   tabs, cards, sections, chips; `NpcTalkView` 4.1.55 shows how a screen is moved onto them). Done: the talk screen (every conversation).
   Left, ours (`client/`): `UkakuMenuScreen`/`View`, `RinkakuMenuScreen`/`View`, `BikakuMenuScreen`/`View`, `GraftMenuScreen`/`View`,
   `PaleMenuScreen`/`View` (the mastery menus), `KakuganScreen`, `PathChoiceScreen`/`PathScreens`, `KingChoiceScreen`, `CookbookScreen`,
   `GenderScreen`, `AbilityHudView` (a HUD, ask whether it counts). The original jar's own screens, if any are still shown, are riskier to
   patch: say so to the owner before starting one. Each: `tools/UiPreview.java` has a mode per screen (add one if missing), render it, judge
   it against `quests-box-4.1.34.png`, show it.
2. The old to-do: Ayato/Touka marriage reactions, a spouse's reactions to gang life, "Quit the gang", the marriage and membership rules
   (the notes under "Added to the to-do" in the section after 4.1.49).
3. The new list (quoted below): Ken's cyber tower removed and him on Anteiku's roof, his sitting leg, the Anteiku faction (Ken), the CCG
   faction (the General), their traits, names and emblems (Anteiku light brown with a coffee cup, the CCG cyan blue with a briefcase).

**4.1.55, where:** `NpcTalkView.draw` (box `BOARD_WIDTH` 680 at the bottom of the 960 x 520 space, its height from the page: `HEADER`,
the words' lines up to `MOST_LINES`, the answers' rows), `card`/`mark`/`Kind`, `section`; `QuestStyle.backdrop` overload (honeycomb
placement); `EtoTests.talkBox` (every card in the box and the screen, apart, none while the words come). Renders:
`docs/handoff/talk-screen-4.1.55.png` (`UiPreview` modes `banjou`, `village-talk`, `eto-talk`, `reputation`).

## START HERE (after 4.1.54): the owner's new list; the menus were never done

**4.1.54, the Gas Masks and Banjou, pushed** (its release notes and README row say what and where). Built from the patch the last session
left (his skin and textures) and the plan below. Judged in renders (`docs/handoff/*-4.1.54.png`, `.gif`). Choices of mine the owner may
want changed: the overhead slam left out of his string (at his wing's size its hand lands beside his own shoulder: rendered), his wing at
0.7 of the twin wings' smallest (about half Touka's), the blue 0x4038E8 (0x2E6FE6 turned the crystal teal), the joining answer that takes
him seriously, his courtship tasks, his daily jobs and pay, the toxic trefoil emblem.

**Rules learned this session:** the wing rig's slams are choreographed for full-size wings: shrinking a wing about its root pulls the strike
zone into the body (render each blow with a stand-in body at arm's length: `ArtPreview banjou` writes `banjou-impacts.png`).
`KaguneTint.recolour` turns the whole red palette round the wheel: pick a kagune colour by rendering it (the crystal's depths are violet and
land far from the target hue). Glyph emblems must be two pixels thick to hold up at a small GUI scale. NIGHT_VISION (`MobEffects.f_19611_`)
is now allowlisted and stubbed.

**The owner, mid-session after 4.1.54 was written (quoted exactly):** "Before any of what follows, I ordered the GUIS all remade in image
of the quest menu style, I didnt get to see any of them, can you confirm it's done and show me results?" They were told the truth: no menu
has been remade yet (they were queued after the Gas Masks and the marriage/gang to-do items), and shown the quest menu
(`quests-box-4.1.34.png`) beside the talk screen as it still is (`banjou-talk-4.1.54.png`).

Then (quoted exactly): "Update: / Ken's cybertower is now fully removed, he is to spawn on top of Anteikou. / Ken's sitting animation is
broken, one of his legs is just in the air. / New Gangs and Faction: / Anteikou - Joinable through Ken (quest) / Traits: / All ghouls and
factions are neutral to the player but the CCG. / Enegy bar increases size and increases maximum capacity. / Looks: Light brown name tag
with a coffee symbol. / CCG Faction: / Join through the general (quest) and now daily quests as well. / Traits: / CCG no longer attacks
the player. / All ghouls but official NPCS are hostile towards the player. / Damage increased when fighting ghouls. / Looks:  Cyan Blue
with a brief cafe symbol."

Notes on it: "brief cafe symbol" is most likely a briefcase (the CCG's quinque case); ask if unsure. "Anteikou" is Anteiku (`anteiku`,
Touka's café: Ken goes on its roof; the cyber tower's structure, set, pool, `DryLand.PLACES` entry, claims and tests go). Ken sits with
`EtoGesture.SIT` (`EtoPose` SIT, `Persona.sitsAtHome`): render it (front, side) and fix the leg. The Anteiku gang's energy bar trait needs
the energy bar's code found first (the original jar's or ours).

## START HERE (after 4.1.53, handoff): the Gas Masks (Banjou, 4.1.54), its first step saved as a patch

The owner stopped the session ("prompt + handoff please"). **Pushed this session** (each after a full build EXIT 0 on its exact commit, jar
sent): 4.1.50 joining gangs (`2732d8ba`), 4.1.51 the new CCG tower (`f607d42f`), 4.1.52 the reputation bar (`c856404a`), 4.1.53 the gangs'
traits and daily quests (`c9e49da3`). The commit carrying this section changes only docs/ (the build's code is 4.1.53's).

**Rules learned this session:** a server map of game times must be `WorldTimes.map()` (ControlTests fails the full build otherwise); the
build runs every `*Tests` suite alphabetically, so before a full build run at least ControlTests, NpcTests and whatever reads the sources
you changed in the quick loop. `tools/interior_render.py` renders any structure in true block shapes and light levels (cutaways, eye views,
`dark` spawnable spots): use it for every structure. `tools/UiPreview.java` modes `reputation` and `dailies` render the talk screen.

**4.1.54 so far:** `git apply docs/handoff/wip-4.1.54.patch` (binary) gives Banjou's skin (`art/banjou_skin.png`, an unchanged copy of the
owner's `art/exploration/Banjou_skin.png`) and his textures (`tools/companion_textures.py` "banjou": slim, `narrow` like Noro's, EYES on
the face's row 4; `textures/entity/banjou/*`, faces judged right). Delete the patch file in the 4.1.54 commit. Nothing else is written.

**The owner's answer this session (quoted exactly):** Banjou's home in `GasMask_hideout.schem` is "The cottage" (the main cherry house with
the pink roof, not the little crimson-roofed garden house). Render of the hideout: build it with `tools/exploration_structures.load` and
`tools/interior_render.py` (28 x 24 x 19; 109 signs, 11 lecterns, 4 beds).

**4.1.54 plan (worked out, not written):**
- **Persona `BANJOU`** modelled on the General (male, `likes(woman)` -> woman: "Romanceable (Only females)", `slim()` true, `muscle()` 0:
  "just alex model by the way, not muscled at all") and Ayato (`wings()`): key "banjou", colour "§a", size 1.0, hair 0x6B4E36, eyes
  0x87A7B6, skin 0xF7CAA5 (from the skin), `mask()` "mask_gas" with a new `maskAtNight()` read in `OutfitLayer.faction` ("puts on the
  mask at night"), home "the cottage". Append to `Persona.ALL` (CompanionTests ~line 810 pins Roma last: update it).
- **Registration as Roma's** (commit `1e3bd752` shows every file): `BanjouNpc`, `ControlEntities.BANJOU` (+ attributes, `EtoRenderer`,
  `UniqueNpcs.kind`), `Teaching.BANJOU`, `ControlCommands` summon.
- **His voice** (the owner: "wants to be tough but truly isnt, is aware of how weak he truly is"; "Daily quest giver, Chat, Romanceable
  (Only females)"): `BanjouScript` (RomaScript's methods), `talk/banjou.txt`, `Courtship.BANJOU`, and his lines everywhere Roma has hers
  (MoodActs, MoodKiss, NightWords, Tolerance, Drowsy, SceneWords' ROMANCE_SCRIPTS/RELATED, SceneServer, EtoBrain's bed line, Chores/Kitchen).
- **His fight** ("super small blue Ukaku wing (singular) cannot shoot projectiles, only uses basic m1s with it"): companions already draw
  one wing (`WingRules.companion`, SIDE); in `UkakuWingRenderer`'s companion loop scale his carry down (~0.55) and colour it from
  `persona.kaguneColour()` (blue) instead of `KaguneColours.DEFAULT`; no shots (a persona flag that makes `NpcWings` choose nothing); his M1
  a string of one-wing slaps from `WingRig.Motion` (SLAP_RIGHT, SWING_RIGHT, RISE_RIGHT, SLAM_RIGHT: the player's 4.1.2 slams): a strike
  start and motion on `S2CNpcWings` (protocol 125), drawn by the renderer, damage on the server when the slap lands. Render front, side,
  and the slaps, and judge them.
- **The hideout:** `tools/companion_structures.py` PLACES "gas_hideout" from `exploration/GasMask_hideout.schem` (Sponge), Banjou in the
  cottage, three or four Gas Masks about it (a flag like `HexCircus`, turned into `ganged(Gang.GAS_MASKS, mask)` by `GhoulNpcBrain`, kept
  near home as `atCircus` does); worldgen as the circus's (structure, structure_set, template_pool, `DryLand.PLACES` + salt,
  `PlaceClaims.ONE_OF_A_KIND`, `ControlStructures.ONE_OF_A_KIND` and its name; NpcTests pins those lists).
- **The gang for players:** `GangPlayer.GAS_MASKS = 3` (banned bit, title "the Gas Masks", `named`: a toxic emblem U+E002 from
  `tools/gang_emblems.py` in the font json, the name "§a" green: "Toxic logo, green name"), `GangPlayer.of` for Gas Mask NPCs; in
  `GangServer`: `leads/gangOf/memberOf/gives` for Banjou, the joining chain ("Short chain, like Roma": talk him up (an answer that takes him
  seriously), kill 3 Clowns, bring him a clown mask), `giveMask` "mask_gas", `inHideout` by the claimed "gas_hideout" (~40 blocks), his
  daily jobs in `GangDaily` (rewards of the gang's), traits in `tick` ("While wearing the gas Mask, gain night vision": Night Vision while a
  mask_gas* is on the head; "Gain speed 2 when at low HP": Speed II at 30% health or under). `NpcTalkView.GANG_COLOURS[3]` is green already.
- Then the order stays: the new to-do items (Ayato/Touka marriage reactions, spouse reactions to gang life, "Quit the gang", the marriage
  and membership rules) -> every menu redone like the quest menu, the talk screen first.

**Prompt for the next session:**

> Continue HexGhoul on branch claude/admiring-johnson-8df58h (fast-forward your session branch onto it first). Read
> docs/handoff/next-ai-4.1.9.md first: the "START HERE (newest, after 4.1.53, handoff)" section, and CLAUDE.md (render and judge
> everything you change; tests passing is not the bar). Then do 4.1.54, the Gas Masks with Banjou: git apply
> docs/handoff/wip-4.1.54.patch (his skin and textures), then everything in that section's plan (his persona, voice, courtship for women
> only, his single small blue Ukaku wing with M1 slaps only, his gas mask at night, his hideout with him in the cottage, the gang for
> players with its toxic emblem, green name, joining chain, mask, dailies and traits), delete the patch file in the same commit, run the
> full build in an isolated copy, push on EXIT 0 to both branches, send the jar. Then the new to-do items, then the menus (the talk
> screen first). One version at a time, each complete; show renders in chat; quote the owner exactly; never claim in-game testing.

## START HERE (after 4.1.53): the Gas Masks (Banjou) next

**Pushed:** 4.1.52 the reputation bar (`c856404a`, full build EXIT 0, jar sent). 4.1.53 the traits and daily quests: its commit carries
this section (release notes, README row).

**4.1.53, where:** `npc/GangDaily` (jobs, goals, rewards; add the Gas Masks' jobs there: a `Job` with `GangPlayer.GAS_MASKS` once that gang
number exists, and `GangServer.gives(Persona.BANJOU)`), `GangServer` (daily section; traits: `fought`, `vanish`; `giveMask`),
`EtoServer.withDaily`, `GhoulNpcBrain.wanted` (wild ghouls and Aogiri), `PlayerLayers.clownVanished`. Render `docs/handoff/dailies-4.1.53.png`
(`tools/UiPreview.java dailies`). The mask for Aogiri was my choice: the Laughing Mask (`mask_bin_brothers`, its rank and file's); tell the
owner they can ask for another.

**Next (agreed order):** the Gas Masks (Banjou: `art/exploration/Banjou_skin.png` painted for wide arms, he is slim and muscle 0, his gas mask
on at night; `art/exploration/GasMask_hideout.schem` (Sponge 1.20.1), he lives in the small house; a toxic emblem and green names
(`tools/gang_emblems.py`, U+E002); his single small blue Ukaku wing, M1 only; dailies, chat, romance (women only); joined by a short chain
like Roma's; traits: night vision while wearing the gas mask, Speed II at low health; GangPlayer.GAS_MASKS = 3) -> the new to-do items
(Ayato/Touka marriage reactions, spouse reactions to gang life, "Quit the gang", the marriage and membership rules) -> the menus.

## START HERE (after 4.1.52): Aogiri's and the Clowns' traits and daily quests next

**Pushed:** 4.1.51 the new CCG tower (`f607d42f`, full build EXIT 0, jar sent; its first full build failed on ControlTests: a map of game
times must be `WorldTimes.map()`, fixed). 4.1.52 the reputation bar: its commit carries this section (see its release notes and README row).

**4.1.52, where:** `npc/GangStanding` (all numbers; `DAILY` +10 is for 4.1.53's daily quests: call `GangServer.reputation(player,
GangStanding.DAILY)` when one is done), `npc/GangTalk`, `GangServer` (reputation section), `GhoulNpcBrain.yields/wanted/hurt`,
`S2CNpcTalk.standing` (protocol 124), `NpcTalkView.reputation` + `diamond`, `tools/UiPreview.java reputation` (renders the talk screen with
the bar: `java -Djava.awt.headless=true -cp build/quick/classes:build/quick/base.jar tools/UiPreview.java <dir> reputation`). Render:
`docs/handoff/reputation-4.1.52.png`. The hideout for the warning: within 180 blocks of the claimed `aogiri_hideout` chunk's middle, 70 of
the `circus` (a married leader's home moves, so not the leader's).

**Next (agreed order):** Aogiri's and the Clowns' traits and daily quests (Noro gives Aogiri's, Roma the Clowns'; rewards: ores, ghoul meat,
human meat, reputation; Aogiri: "Wild ghouls no longer attack unless provoked, gain strength when fighting CCG and other gangs"; the Clowns:
"Gain Jump boost when fighting CCG, kagune, player and armor become fully invisible for 5 seconds when at low HP (5 minute cooldown)") ->
the Gas Masks (Banjou) -> the new to-do items -> the menus.

## START HERE (after 4.1.51): the reputation bar next, then the rest of the agreed order

**Pushed:** 4.1.50 joining gangs (`2732d8ba`, full build EXIT 0 on its exact commit, jar sent). 4.1.51 the new CCG tower (see its release
notes; its commit is the one carrying this section).

**4.1.51, what and where:** the owner first sent `art/exploration/New_CCG.schematic` (an empty shell: walls only; asked "is the building not
furnished?", told no), then "then pause a second Ill get you a new one fully furnished": `art/exploration/CCG.schematic` (their upload,
unchanged; MCEdit/WorldEdit Alpha, 31 x 65 x 46, a downloaded office block: lobby with reception, restaurant, kitchen; nine identical office
floors one every 6 blocks; no stairs, only a command-block lift whose command blocks were not in the file). New_CCG is kept, unused.
Their answers (AskUserQuestion): signs "Rename to CCG (Recommended)"; villagers "Restaurant staff humans (Recommended)".
- `tools/ccg_tower.py`: every Alpha id it has to 1.20.1 (doors' halves, banners' inverted dye colours, pots, buttons, trapdoors, pistons),
  signs renamed (`SIGNS`; stray U+F700/F701 dropped), paintings and item frames (old item ids, potions) as entities (Pos = their block's
  middle: the game hangs them in the block their position is in), villagers' spots returned as the staff's; writes
  `build/ccg/ccg_tower.schem` (Sponge v3 + Entities).
- `tools/exploration_structures.py`: site `ccg_building` (source "ccg_tower", map 11, ground 3, storey 6, floors 10, "hanging"), new
  `headquarters` (posts outside and at the lobby lift, lobby seats, three desk seats a floor chosen per floor, a lift guard a floor, six
  walkers, the staff humans, the General in the top floor's boss office with its chairs left free), `load_hanging`, `write_tiles` hangs.
  Run: `python3 tools/exploration_structures.py --blocks <minecraft-data 1.20 blocks.json>` (npm: `npm pack minecraft-data`, then
  `package/minecraft-data/data/pc/1.20/blocks.json`); the other sites came out byte-identical.
- The old building's map (6) is in `village/RetiredSites` (its people in older worlds keep their patrol).
- `server/CcgLift` + `village/LiftRules`: RightClickBlock on a button in the world's CCG building (within 72 blocks of its claimed chunk,
  overworld), the player in the cabin: up 6 if a button is 6 over, else down to the lowest of the run (`PlayerInteractEvent.getPos`
  allowlisted from Forge's source; `RightClickBlock.getEntity` dropped from the allowlist: proven through PlayerEvent once the stub
  declares the class).
- `VillagePlace.indoors` / `HumanBrain.village`: the tower's humans stroll within `VillageRules.STAFF_REACH` (3) of their spot.
- `tools/interior_render.py` (new, reusable): any cells dict rendered with real block shapes and the game's light levels: `iso` cutaways of
  one storey (`marks` for people), `eye` views, `dark` (monster-spawnable spots). Use it for every structure from now on.
- Render shown to the owner: `docs/handoff/ccg-tower-4.1.51.png`. NpcTests.ccgBuilding rewritten for the tower.

**Next (the agreed order):** the reputation bar (their rules are in the section below) -> Aogiri's and the Clowns' traits and daily quests
(Noro, Roma) -> the Gas Masks (Banjou) -> the new to-do items (marriage reactions, Quit the gang, marriage/membership rules) -> every menu
redone like the quest menu, the talk screen first.

## START HERE (after 4.1.49): ship 4.1.50 from its patch, then the owner's new list (with their answers)

**State:** 4.1.49 Roma is done (her skin: the owner's second, `NewRoma.zip`, as `art/exploration/Roma_skin.png`; their first, with a
clown face, was replaced: "I changed my mind do this one for her skin").
**4.1.50 (joining gangs) is NOT pushed:** the owner stopped the session ("Handoff and prompt, now Im out of usage"). It is complete as a
patch: `git apply docs/handoff/wip-4.1.50.patch` on top of 4.1.49 (`96fa9b90`) gives the whole version (code, emblems, release notes
`.github/release-notes-4.1.50.md`, README entry, VERSION 4.1.50, protocol 123); quick_check (verifier, NpcTests, CompanionTests,
BikakuWormTests) passes. To ship: apply, delete the patch file in the same commit, run the full build in an isolated copy, push on EXIT 0
(both `claude/admiring-johnson-8df58h` and your session branch), send the jar. Then the agreed order below, with the new to-do items.

**The owner's new list (quoted exactly):** "News and reworks: / New CCG schematic, map it out with sitting areas, offices and etc. / New
Reputation bar when talking to fellow gang members, high reputation means you're feared, they react to it, some stepping back to let you
pass (just a little and not the leaders, they do not step back this also applies for official NPCS). / When the reputation bar reaches 0
the player is warned to leave the area (if within the hideout), the bar starts at 50%. / new Menus: Every single menu in the game is to
mimic and look like the quest menu, and be as interactable as it is, Dialog, mastery menus ALL OF THEM. / Gang Traits: / Aogiri - Wild
ghouls no longer attack unless provoked, gain strength when fighting CCG and other gangs. / Noro gives Daily quests (missions with item
rewards such as ores, ghoul meat,  human meat, reputation etc...) / Clowns Traits: / Gain Jump boost when fighting CCG, kagune, player and
armor become fully invisible for 5 seconds when at low HP (5 minute cooldown). / The leader gives daily quests as well with clown related
rewards, and reputation. / New gang: / Gas Masks - Toxic logo, green name. / Leader - Banjou / Personality - wants to be tough but truly
isnt, is aware of how weak he truly is. / Kagune - super small blue Ukaku wing (singular) cannot shoot projectiles, only uses basic m1s
with it. / Dialog - Daily quest giver, Chat, Romanceable (Only females). / Lives in the small house in the schematic. / Gang Bonuses: /
While wearing the gas Mask, gain night vision. / Gain speed 2 when at low HP. / Reputation". Then: "The gas mask leader puts on the mask
at night and is just alex model by the way,  not muscled at all".

**Their files (never alter them):** `art/exploration/New_CCG.schematic` (MCEdit Alpha, 43 x 58 x 37: a ~14-floor office tower, its
inside to be furnished: lobby seating, offices, etc.), `art/exploration/GasMask_hideout.schem` (Sponge, DataVersion 3465 = 1.20.1, 28 x 24
x 19: a cherry/crimson house with a walled garden; Banjou lives in the house), `art/exploration/Banjou_skin.png` (painted for WIDE arms:
the game copy narrowed as Noro's, `narrow_arms`; he is slim and muscle 0).

**Their answers:** Gas Masks are joined through Banjou by "Short chain, like Roma" (talk him up, kill 3 Clowns, bring him something
proving it); "Joining gives the mask" (each gang's mask a wearable head item on joining; the gas mask's night vision only while worn);
reputation is raised by daily quests and by killing the CCG and rivals, lowered by hitting your own (kills cost a lot; the 10-kill ban
stays), and drifts back toward 50% over days away; at 0, warned to leave the hideout, and "They attack after a warning" (~30 s; not
banned, it can recover). The emblems (`docs/handoff/gang-emblems-4.1.50.png`) got no comment yet.

**Added to the to-do (the owner, quoted exactly):** "Make sure ayato + touka and being married to tokou reactions are in." / "Then, add being
married to a gang leader but being part of another gang generates different reactions from the spouse, constantly demanding they quit the
gang." / "Add a new Quit the gang button to each leader whenever the player joins it." / "kicked from the gang if the player marries
another gang leader, but can still join if the player was already married before joining." (Notes: Ayato's and Touka's reactions to a
player married to Touka exist since 4.1.15 (`SceneServer.relations`, the "with_touka"/"wed_touka" scenes in talk/ayato.txt): check every
pairing is there and add what is missing. "Gang leader" = Ayato (Aogiri), Roma (the Clowns), Banjou (the Gas Masks, to come). The quit
button: the leader's "gang" option on their first page when the player is a member (`EtoServer.withGang`, `GangServer.ask/talk`); quitting
is not a ban. A marriage to another gang's leader while a member: membership removed (not banned); married first, then joining: allowed.)

**Agreed order (told to the owner):** 4.1.49 Roma (her skin) -> 4.1.50 joining gangs -> the new CCG tower furnished -> the reputation bar
-> Aogiri's and the Clowns' traits and daily quests (Noro, Roma) -> the Gas Masks (Banjou, his hideout, traits, the toxic emblem, green
names, his small single blue Ukaku wing, M1 only) -> every menu redone like the quest menu, one screen per version, the talk screen first
(some mastery screens are the original jar's: patching them is riskier, say so before starting).

## START HERE (after 4.1.48): Roma (4.1.49) waits on her skin; joining gangs (4.1.50) done in code

**Pushed:** 4.1.48 (`c9887594`, full build EXIT 0 on its exact commit; jar sent): the circus tent and its four Clowns, Clowns vs Gas Masks,
Ayato/Noro slim, and (the owner, mid-session, quoted exactly) "Reduce the player's muscle size to be the same as Noro." (`PLAYER_SHARE`
0.52). The commit carrying this note changes only docs/ (the build's code is 4.1.48's).

**Not pushed, saved as a patch:** `git apply docs/handoff/wip-4.1.49-4.1.50.patch` (binary: textures, the tent template) on top of 4.1.48.
It holds two versions' work, both compiling, the API verifier and Companion/Npc/Graft/Scene/Dialogue/Family/Eto/BikakuWorm tests passing
(quick_check), **the full build not run on either**:
- **4.1.49 Roma** (everything but her skin): `Persona.ROMA` (Touka's size, skinny: curves 0.75, legs 0.7, build 0.94; slim; dates anyone;
  `kickMaster()`, `tendrils()` 4, purple `ROMA_KAGUNE`), `RomaNpc`, `ControlEntities.ROMA` (+ attributes, `UniqueNpcs.kind`), renderer,
  `RomaScript` (her voice: death jokes, delighted by everything, sincere when it counts), `talk/roma.txt` (her scenes, five courtship
  moments, three dates), `Courtship.ROMA` (doves, a village walk at night, her tent in the dead of night, the Pack, dawn), her lines in
  MoodActs/MoodKiss/Tolerance/NightWords/Drowsy/SceneServer/EtoBrain, SceneWords, chores and kitchen as Touka's, `/kagune` summon; placed in
  the tent's middle (38, 1, 24). Her fight: Kick Master by `Persona.kickMaster` in `EtoOwl` (the General's too), and `NpcTendrils` (four
  transplant tails on the Graft rig, two a side: `GraftRig` TRANSPLANT limb 2/3 lower roots and `TAIL_LOW`; the Rinkaku string, the
  Fourfold Pierce, `GraftMove.ROMA_IMPALE` (run through, lifted on it, torn out), `ROMA_HURL`/`ROMA_THROW` (the lower pair tear up a chunk,
  hold it, whip it: `RinkakuHurl`'s numbers, drawn by `HurlClient` from `S2CHurl` under her id)); drawn by `GraftRenderer`'s companion pass
  in her purple (`S2CGhoulNpc` under her id). Render judged: `docs/handoff/roma-tendrils-4.1.49.png`. Protocol 122.
  **Missing: her skin.** The owner sent it as a picture in chat twice; it never reached the container. When it arrives: copy it to
  `art/roma_skin.png`, add `"roma"` to `tools/companion_textures.py` COMPANIONS (slim; which eyes turn), run it, set `ROMA_HAIR/EYES/SKIN`
  from the skin, render her (front, side, three-quarters, with the tendrils), then release notes/README/table row and the full build.
- **4.1.50 joining gangs**: `GangPlayer` (pure: member, banned bits, Aogiri's quest 5 CCG, the Clowns' chain: Roma laughs at the right
  joke (`GangServer.JOKES[FUNNY]`), 3 Gas Masks, a quinque case; over 10 of their own killed: banned, out), `GangServer` (kept in
  `PlayerPersisted`; `LivingDeathEvent` counts; `PlayerEvent.NameFormat` puts "(emblem) (space) (name)" on the name, server and clients;
  `S2CGangName` message 57; Ayato's and Roma's first page get a "gang" option, SAY_3, `EtoServer.withGang`), `GhoulNpcBrain.wanted` (its own
  never, the banned on sight, what goes for its own: `GangServer.threatens`) and `hurt` (no grudge for its own's blows). The emblems:
  `tools/gang_emblems.py` (16 x 16, in `assets/minecraft/font/default.json` as U+E000 the tree, U+E001 the clown); shown to the owner:
  `docs/handoff/gang-emblems-4.1.50.png` (their answer not yet in). Forge's NameFormat and `Player.refreshDisplayName` stubbed and
  allowlisted from Forge 1.20.1's sources. `NpcTests.joining`. Protocol 123.
  **Open to the owner:** how a player joins the Gas Masks (never asked); whether the emblems are right.

## START HERE (after 4.1.47): finish 4.1.48, then Roma (4.1.49), then joining gangs (4.1.50)

The owner stopped the session ("Stop, prompt and handoff, Now"). **Pushed** (each after a full build with EXIT 0 on its exact commit):
4.1.45 roaming gangs (`cf3fa763`), 4.1.46 the CCG ghoul division (`2dc0ee3f`), 4.1.47 the arm blade rebuilt (`2cec5cd4`). The commit
carrying this note changes only docs/ and art/ (the build's code is 4.1.47's).

**4.1.48 is done** (see its row in the table; the patch is applied and deleted). The handoff below was wrong on one point: the tent has
**one** entrance, the spruce-framed arch on the south (z 45-46, x 35-41); the "entrances" at z 1/47 are bulges of the wall.

**1. (done in 4.1.48) Apply the unfinished 4.1.48 first:** `git apply docs/handoff/wip-4.1.48.patch` (binary patch: Noro's texture too). It holds, checked
with `tools/quick_check.py` and CompanionTests passing, the full build not yet run:
- the owner: "Ayato and Noro should both be Alex models": `Persona` Ayato and Noro `slim()` true. Ayato's game texture is painted for the
  slim arms already; Noro's was wide, so `tools/companion_textures.py` (`narrow_arms`, `"narrow": True`) now writes his game copy with
  each arm face 4 to 3 pixels (art/noro_skin.png untouched). Seen in a render: clean on slim arms.
- the owner: "ayato is meant to have muscles they're gone? Also give some to Noro, both less than the player, noro more than ayato":
  `ReaperBuild.AYATO_SHARE` 0.3 to 0.42 (0.3 was lost under his black jacket), `NORO_SHARE` 0.52 (`Persona` Noro `muscle()`), a male
  player 0.6. CompanionTests updated. Render: `docs/handoff/companion-builds-4.1.48.png` (`tools/CompanionBuildPreview.java`).

**2. Then the rest of 4.1.48: the circus tent** (the owner sent `circus.schematic`, now `art/exploration/circus.schematic`, unchanged).
It is the OLD MCEdit "Alpha" format (numeric ids in Blocks/Data, gzip NBT), 77 x 46 x 49: only air (0), white wool (35:0), red wool
(35:14), glowstone (89), spruce log (17:1, upright), grass (31:1, 1.20.1 `minecraft:grass`), a dandelion (37). Its outer wall is a ring
(layer 1, rows z 1..47); four entrances: z 1 and z 47 at x 20-24 and x 52-56; no floor (it stands on the world's ground). Plan, decided:
- `tools/companion_structures.py` (writes one template with people in it, as `reaper_mansion`): add a reader for the Alpha format (map the
  ids above to states), an option for a generated floor layer under the tent's ring (use `minecraft:dirt_path`: people stand on it, and
  trees and grass cannot root there), a jigsaw anchor at the floor's middle (`hexkagune:circus_anchor`), and several people per place.
- the owner chose "One per world + clowns": four Clown gang members inside at (22,0,8), (54,0,8), (28,0,32), (48,0,18) (checked: air at
  y 0..2, inside the ring), as `hexkagune:ghoul` with ForgeData e.g. `HexCircus` 1, `HexCircusMask` 1..5 (each a different mask),
  `HexGang` one fixed number (so `GhoulNpcBrain.mates` holds), `PersistenceRequired` 1. `GhoulNpcBrain.tick` (identity null) turns the
  flag into `NpcIdentity.roll(...).ganged(Gang.CLOWNS, mask)` as it does `AOGIRI`; they keep near home (not `roam`). Roma's spot (4.1.49):
  the middle, (38,0,24).
- wire it as the mansion is: `worldgen/structure/circus.json` (type `hexkagune:dry_place`), `structure_set/circus.json` (own salt),
  `template_pool/circus/start.json`, `DryLand.PLACES` + a salt + `placeSalt`, `PlaceClaims.ONE_OF_A_KIND`, `ControlStructures.ONE_OF_A_KIND`
  and its name for `/kagune places`; NpcTests pins these lists (search "reaper_mansion" in NpcTests and CompanionTests).
- the owner chose "Yes, rivals": Clowns and Gas Masks fight on sight (a `Gang.rival` and a line in `GhoulNpcBrain.wanted`'s gang branch).
- Render the tent (front, side, inside) and judge it before pushing.

**3. 4.1.49 Roma** (the owner, quoted exactly): "New Official NPC named "Roma", she is the leader of the clowns, can be romanced, has a
messed up sense of humor but can be romatic when comes to it. She uses Kick master combat, has a Rinkaku with 4 purple tendrils (can use
all moves from Rinakaku, including impale, Debris Hurl, etc. She spawns in this circus tent, is the size of Touka, is female, can be
romanced by both Females and Males. (Alex Model, skinny)". **Her skin:** the owner sent it only as a picture in the chat (no file); I asked
them to upload it as a PNG file. Ask again if it is not in the session. She is a new `Persona` (as Touka: EtoNpc), placed in the tent.

**4. 4.1.50 joining gangs** (answers given): Aogiri through Ayato's dialog and the quest "Kill 5 CCG"; the Clowns through Roma, the owner
chose "A short chain": make her laugh in dialog, kill 3 Gas Masks, bring her an investigator's quinque case. Aogiri's name dark red with a
custom tree emblem, the Clowns' blue with a custom clown face ("Custom clown face": draw it and show a render first), "(symbol) (space)
(name)". For every gang (the owner): "the gang will never attack the player again, even when hit, they will however help the player and
defend him against hostiles. IF the player kills over 10 members of his own gang, he will be banned from joining, tag removed and will be
attacked on sight." How a player joins the Gas Masks was never asked.

**Asked in between and done:** 4.1.47's arm blade (the owner's picture: broad ribbons round the arm into one huge smooth blade; their
choice "B: blade turned over"; four slashes and a living stance; "that will do, make sure that kagunes use random colors on this one
too": they do, `KaguneTint.wild`).

## START HERE (after 4.1.45): the gangs, then the CCG division and joining gangs

The owner asked (quoted exactly): "Next up we're adding roaming gangs, such as the clowns, 3-5 roaming ghouls walking together. Gangs
needed: Clowns (all ghouls wear different clown masks (nothing else) and protect eachother) (Non-hostile unless provoked); CCG (already in
game); Gas Masks (... protect eachother) (Non-hostile unless provoked) (they wear nothing but gas masks); New Re CCG Ghoul division (Normal
ccg agents (HUMANS not ghouls), they have RC cells which means they get a random kagune, they are not ghouls and are members of the CCG,
they are never attacked by the ccg, have no kakugan, they are basically wild ghouls with CCG armor and a Kagune (NO kakugan), they spawn in
trios. Players are now able to join gangs as well, dialog with Ayato and with a quest you can join Aogiri tree, the player's name turns dark
red with a symbol on the left (symbol) (space) (name)".

Their answers to the questions asked: the Aogiri quest is **"Kill 5 CCG"**; joining "(this applies for all of them), the gang will never
attack the player again, even when hit, they will however help the player and defend him against hostiles. IF the player kills over 10
members of his own gang, he will be banned from joining, tag removed and will be attacked on sight."; the symbol is **a custom tree emblem
(render it first and show them)**; the CCG division **behaves like CCG agents**.

1. **4.1.45 done** (the Clowns and the Gas Masks; see its "what and where"). Masks judged in `docs/handoff/gang-masks-4.1.45.png`.
2. **4.1.46 done** (the CCG's ghoul division: ghoul NPCs of `Gang.CCG_DIVISION`, see its "what and where"). Judged in
   `docs/handoff/ccg-division-4.1.46.png` (Formal Outfit, each kagune, no kakugan beside a wild ghoul's).
3. **4.1.47 done (asked in between):** the ghouls' arm blade rebuilt to the owner's picture, turned over (their choice), four slashes
   and a living stance; see its "what and where" and `docs/handoff/arm-blade-*-4.1.47.png`. The owner: "that will do".
4. **4.1.48 next: joining gangs.** Aogiri through a dialog with Ayato and the "kill 5 CCG" quest; the member's name dark red with the tree emblem
   on its left ("(symbol) (space) (name)"): draw the glyph, render it in a name tag and chat, show the owner first. Membership is generic
   (all gangs): never attacked by their gang even when hitting it, defended by it; over 10 of their own gang killed: banned, tag removed,
   attacked on sight. How a player joins the Clowns or the Gas Masks was not asked yet: ask the owner.

## START HERE (after 4.1.42): structures were broken in game; check the new world

The owner found in game (quoted exactly): "Generation like this fucking acceptable?" (a city tower over a hill); "They got the fucking Kaneki
Tower, the fucking library, and the Intaku all in the same spot"; "The structures are spawning multiple times. There's tons of them spawning
like hundreds of times in the fucking world. And they're all fucking flying"; "the mod is lagging like crazy". And: "How does this generation
pass your bug test, bro?" It didn't: no test ever put a structure on terrain. 4.1.42 fixed the causes found (see its "what and where" in the
table and notes) and added `NpcTests.worldgen` on made-up land. **Still not seen in game:** ask the owner to try a NEW world (old chunks keep
the copies); if it still lags there, find out where (near a city? its 104 people per city?) before guessing.

## START HERE (after 4.1.40): the Reaper's change, then bugs

1. **Branch:** `claude/admiring-johnson-8df58h` holds 4.1.37 .. 4.1.40 (each pushed after a full build with EXIT 0 on its exact commit), on
   top of 4.1.36 (`claude/keen-hypatia-3ztwk7`). A new session's branch usually starts at `main` (1.3.0): fast-forward onto this one first.
2. **Done this session (see each "what and where"):** 4.1.37 the Reaper untouchable through his change (the attack event cancelled, nobody's
   target, freed and pinned), a real coat taken off (the formal overcoat in white), a slower draw (62 ticks), the blade held point-down in
   one hand (the owner's picture), his blade and brawl blows clashable; 4.1.38 two minutes after a fight he puts the jacket back on (the
   owner chose "reverse it in place"); 4.1.39 city fixes (village humans kept home, the city's ground cached, protocol 118); 4.1.40 old
   towns' maps kept for their people, "complete all" reopens later gates at login.
3. **Open to the owner:** "for some reason, you can clash with this form" was read as "can't" (fixed so his blows are on the clash board):
   confirm. The coat appears from nowhere behind him when he puts it back on (their choice). The change is long (5.4 s untouchable).
4. **Not checked (needs the game):** the city in a real world (`/locate` time in hilly worlds), the coat layer's "kept where it fell" turn
   sign (`ReaperCoatLayer.FELL`: if the coat swings round with him as he turns while it lies there, flip the sign of `turn`).
5. **Next:** ask the owner. Their list in "The owner's big list" below; daughters' open eyes still unasked.

## START HERE (after 4.1.34): the Small City

**The owner stopped the session (quoted exactly): "Okay, enough. Stop now. Given me a prompt in the end of the next AI to pick up now."
Then: "Handoff and prompt, now".** Still: hurry, one short version at a time, renders in chat before calling anything done.

1. **Branch:** `claude/sleepy-knuth-5jlwby` holds 4.1.28 .. 4.1.34 and this handoff (each pushed after a full build with EXIT 0 on its exact
   commit). A new session's branch usually starts at `main`: fast-forward onto it first.
2. **Done this session:** Arima's phase 2 entirely (4.1.28-4.1.31: see their sections below), quinque M1s winning over the kagune's
   (4.1.32), quests finished for good (4.1.33), the new quest menu (4.1.34). Every render is in `docs/handoff/*-4.1.2[89].png`,
   `*-4.1.3[01].png`, `quests-*-4.1.34.png`.
3. **Next: the Small City (the owner chose, asked: "Whole city").** Their words (big list, below): "ALl villages get replaced with the city
   in my minecraft world that I sent named "Small-City". Delete all ids hexkagune:village_x ( and anything related) add
   Hexkagune:small_city. This big structure is relatively rare (a bit rarer than a normal village), Humans can now however spawn in normal
   minecraft villages."
   - **What the save is:** `art/city/small-city.zip` (never alter it), DataVersion 4790, new layout (`dimensions/minecraft/overworld/
     region`). The city is chunks (-17, -17) .. (16, 16): **544 x 544 blocks**, on a thin built platform (a flat/void world: nothing under
     y 61), ground y 64, highest block y 195, ~2M blocks; a full street grid, a central park, a pool, towers. Top-down render of it was made
     with a scratch script (not kept); `tools/city_structure.py --map` is a good place to add one.
   - **The limit:** one structure places only within 8 chunks of its start (272 x 272: `ControlStructures.Site` REACH_BACK/REACH_ON). The
     owner chose the whole city: **four 272 x 272 quarters** (0 NW, 1 NE, 2 SW, 3 SE) that always spawn together side by side at **one
     shared ground height** and **one shared turn**.
   - **4.1.35 and 4.1.36 are done** (the cut; the city in worlds, the villages deleted, humans in vanilla villages: see their "what and where"). **The Small City is finished.** Next: ask the owner.
   - **WIP patch:** `docs/handoff/small-city-4.1.35-wip.patch` (`git apply` it, delete it in the commit): `tools/city_structure.py` (reads
     the zip's region files into a compact grid, finds the ground, writes `build/city/small_city_<k>.schem`, Sponge v3: ran in 38 s,
     ~500k blocks and 560-800 states per quarter), `tools/library_structure.py` (writes byte/int/long arrays), `tools/exploration_structures.py`
     (four sites `small_city_0..3`, maps 7-10, kind "human", 12 homes and 2 agents each, ground 6 = y 64). **Not yet run to the end**: run
     `python3 tools/exploration_structures.py --blocks <1.20 blocks.json>` (PrismarineJS minecraft-data `data/pc/1.20/blocks.json` from
     raw.githubusercontent.com); it will stop on every 1.21.x block 1.20.1 lacks: add each to `SWAPS`. Expect it to be slow (a quarter is
     272 x 138 x 272) and check the other sites come out byte-identical (`git diff --stat`).
   - **Then (4.1.36):** worldgen for the four quarters: a custom placement so quarter k starts at the cell's chunk + its offset (17 chunks
     apart, turned with the city), all four sampling the whole city's footprint for one ground height (deterministic, from the chunk
     generator, like `DryLand.lift`/`dryGround`), rarer than a vanilla village; remove `village_1`, `village_2` (their structures,
     structure_set `villages.json`, SiteMaps entries, tests, anything related; `village/` structures too if only they use them); humans in
     vanilla villages (spawn a few `hexkagune:human` when a vanilla village's meeting point generates or by a spawn rule near villagers).
     Render the placed city (an isometric of the four quarters together) before pushing.
4. **Open to the owner:** after `complete all`, hunts and favours still come (told them; they may want those off too). Daughters' open eyes
   (07, 10, 11, 12, 21, 22) still unasked.
5. **Watch out:** the owner wants everything compact ("why not just a cute box?") and hates full-screen menus; renders in chat early.

## START HERE (before, kept): HURRY

**The owner is out of patience. Their words (quoted exactly):** "I don't understand what's so hard. Why do you keep making the same fucking
issue over and over again? I'm out of usage. You spent your entire session in that fucking same animation of one quinque. Bro, this is a
ridiculous. Give me a handoff and a prompt right now. Stop what you're doing and give it to the next AI. Specify that this is taking way too
long and it's to hurry". And of the 4.1.21 before/after render (`docs/handoff/katana-cuts-before-after-4.1.21.png`): "I don't understand
what's so hard about making an animation with the fucking katana, bro. Holy shit. Look at how it's pointed. Look at the hands, bro. Like,
are you fucking kidding me? How do you have the balls to say before and after when the after still looks bugged as shit?"

**This is taking way too long. Work fast, show the owner renders early, and never call something fixed that still looks wrong.**

After 4.1.21 the owner said (quoted exactly): "mostly this is taking far too long. Just as soon as you finish the katana, just make the
first person of the other weapons. Don't touch their looks. Dude, it's just, you're just feels like you're doing the same thing over and
over and over again. I don't see much change. Like, it's already my second account that has been wasted time on this one thing.
Considering the amount of stuff that we got to do, you're spending so much time into one". After 4.1.24: "Continue then, taking long as
fuck". Of Dawn's opening (an umbrella opening back over the hand had been described): "It is not what I meant when I said an umbrella. I
was being, I wasn't being literal, bro. It just meant open. Like a reverse umbrella, not like that, bro." Then: "handoff and prompt for
the next AI now."

1. **Branch:** `claude/sleepy-archimedes-mbiovy` (4.1.21 .. 4.1.27 and this handoff, each pushed after a full build with EXIT 0 on its
   exact commit). A new session's branch usually starts at `main` (1.3.0): fast-forward onto this one first.
2. **The katana is done (4.1.21):** hands first, wrists, five cuts checked on renders (see "4.1.21, what and where"). Don't reopen it
   unless the owner asks.
3. **The first person of the other weapons is done (4.1.22):** the bone greatsword, flesh hammer and crystal spear through the body; the
   launcher and the whip kept theirs on purpose (see "4.1.22, what and where"). If the owner wants the whip too, its tendril path
   (`QuinqueOwn.whip`, `WeaponLayer.whip`) has to be laid from the posed arm.
4. **Arima, part 1 is done (4.1.23):** Dawn every time, the Slain Reaper advancement and his mastery for everyone who hit him in the last
   five minutes. **4.1.24:** his mansion (the owner's schematic, `art/reaper/arima_mansion.schem`), one per world, him in its garden.
   **4.1.25:** both fighting styles (Kick Master's string and the Speed Blitz, up close). **4.1.26:** Dawn smooth, opening like a reverse
   umbrella. **4.1.27:** the ground strike grows Dawn's blades, not a tentacle. **4.1.28:** the jacket comes off (part 1 below is done). **4.1.29:** at 90 HP (not half), the golden case and the blade (part 2 done;
   the owner: keep it his alone, no player version). **4.1.30:** close combat, rushes, dodges (part 3 done). **4.1.31:** the end (part 4 done): **Arima's phase 2 is finished.** **Next for Arima: phase 2**, one version each, renders judged before
   each push:
   1. **The jacket comes off** at half health (once): a shedding move (both hands to the collar, the coat swept off the shoulders, a new
      swing code), his look switched to a new appended look `ccg_reaper_bare` (from `art/reaper/Reaper_Phase2.png`, copied; slim arms
      like his first; make every `lookIndex() == REAPER_LOOK` test a `reaperLook(index)` that takes both), faster, Dawn put away (he "no
      longer has the Dawn weapon"; it still drops on his death).
   2. **The heavy blade** (`art/reaper/Reaper_Quinque1.jpg`, `Reaper_quinque2.jpg`: a long straight silver blade cracked with red veins, an
      organic silver guard set with red RC gems, a long grip, a ring and a hooked prong with a red tip): a grown mesh like the katana's,
      two-handed, its own heavy string by intent (`QuinqueAim`: hands first, wrists), bleed on every hit (the mod's bleeding effect), never
      dropped. NPC-only: an Arm or a Quinque without a case item; weigh which touches less (both enums register items and are looped over
      by tests).
   3. **Close combat:** always closing in, precise dashes (`ReaperDashServer`), dodging what is thrown at him, no ranged moves.
   4. **The end:** low on health he lowers the blade, it shatters (shards, a sound), he drops to his knees (a kneeling pose held), then
      dies; Dawn and Slain Reaper as 4.1.23.

   **Read already for phase 2 part 1 (nothing coded):** his skins are `AgentRules.LOOKS` (append only: sent by number; `REAPER_LOOK` 9,
   `REGULAR_LOOKS` 9); the look is synced by `CcgAgent.dress(look, kind)` (entity data `LOOK`) and set in `AgentBrain.wake`
   (`mind.look = AgentRules.REAPER_LOOK` for the Reaper: keep the bare look once shed, a persistent flag). Reaper-only checks on the
   look: `AgentClient.pose` (his idle), `AgentRenderer` line 80 (`brawny`) and the `Build` layer (line 170). Movement speed
   (`Attributes.f_22279_`) and `setItemSlot` (`m_8061_`) are already used (allowlisted). `AgentBrain.brawl` needs `mind.kind == DAWN`
   and `AgentPose.brawl` carries Dawn at the shoulder: when the blade replaces Dawn, give both the blade (or no weapon). Keep Dawn in his
   hand until the blade exists: an empty-handed Reaper runs code paths nobody has seen (`close`, `reach(mind)` by kind).
   **Previews that worked:** scratch sheets over `PoseRender` and `QuinqueMeshPreview` (front, three-quarter, his right, above; the
   Reaper's skin `art/white_reaper_skin.png`); write one per change, look at it, fix, send it.
   Then quests, then the Small City. One version at a time, short.
5. **Open question to the owner:** should daughters 07, 10, 11, 12, 21 and 22 get open eyes painted on? Ask before touching them.

## How to work (the owner asked the next AI to be "exactly like" the last one)

**Talking to the owner:**
- Write in English, casual and warm, witty, playful and flirty. That is how the owner likes it (their account preferences say so; follow
  those if yours differ). Swearing is fine.
- Be blunt when an idea is weak. Say so straight away, why, and what would work better. Stress-test every ask before building it:
  - multiplayer;
  - old worlds;
  - the owner's pack (Regions Unexplored/TerraBlender, Biomes of Plenty, Structure Essentials);
  - a mod that wraps the level;
  - first person.
- The owner dictates by voice on a phone, so words come out garbled ("hiw us ut" is "how is it", "Ayatollahs" is Ayato's, "Quinkays"
  and "Quinkei" are quinques, "Haiku" is Anteiku, "Cognate lock" is `/kagune unlock`). Read for intent. Ask only when the answer changes
  what you build.
- Quote them exactly in notes and handoffs; never paraphrase their words into a quote.
- They want images in chat (they said "Give me images here in chat"): send every preview with SendUserFile as it is made.
- Never say something was tested in game: it never is. Every note says "Not tested in game".
- When they report a bug, believe it and find the cause in code before you answer. Don't argue it's their world unless the code proves it.
  When it really was them (`/kagune unlock` and `/locate` are OP-only), say so kindly, with the line of code.
- Their messages sent mid-turn only reach you between tool calls. Don't block for long (a full build runs in the background with a
  Monitor); check in often.
- When they say stop: push only what is verified, save the rest as a patch, and hand off.

**One version at a time, each one complete:**
- One version is the change, plus `.github/release-notes-X.md` (their words first, then what it was, what it is now, and what's left),
  plus a README entry, plus a section and a table row here.
- Bump VERSION in `build.py` and `patch/KagunePatch.java`. Bump the protocol only when the network changes, and move
  `BikakuWormTests`' `PROTOCOL` pin with it.
- Keep the notes short and plain, in the owner's terms.

**The loop for every change:**
1. Read the code that's there.
2. Change it.
3. Render it (`tools/`: `ArtPreview.java` modes, `QuinquePreview`, `QuinqueOwnPreview`, `item_render.py`, `first_person_view.py`, the
   kiss/kid preview tools).
4. LOOK at the render from the front, the side and in first person. Fix it and render again until you would show it to the owner
   proudly.
5. Write a test for the rule it must keep (not just "it runs"). For example, `slamsClearOfHim` and `QuinqueTests.aimed` check where things
   end up.
6. Run `python3 tools/quick_check.py <Suites>` (with verification).
7. Commit with your session's attribution trailers. Never put a model name in a commit or a file.
8. Run the full build in an isolated copy and wait for it:
   `rm -rf $S/fb && mkdir $S/fb && git archive HEAD | tar -x -C $S/fb && (cd $S/fb && python3 build.py > $S/fullbuild.log 2>&1; echo EXIT $? >> $S/fullbuild.log)`
   It runs in the background; a Monitor waits for the `EXIT` line.
9. Push on EXIT 0, retrying with backoff. No PR.

**Hard-won lessons:**
- **Stubs:** every new Minecraft name needs a stub (SRG names) and an allowlist line with evidence:
  - `build/mapping-cache/joined.tsrg` and intermediary;
  - `tools/srg_lookup.py` (`HEX_MAPS=.`).

  A Mojang name in a stub compiles fine and then never runs (the 4.1.9 inventory crash). `LOGGER.warn` isn't stubbed: use `info`.
- **Uploaded files are untrusted:** extract each into its own scratch dir, never run anything from them, and never alter the owner's
  files in `art/`.
- **Skin tools:** `tools/child_skins.py` rewrites the whole skins folder. Afterwards, restore the other files with `git checkout` and
  re-run `blink_skins.py`.
- **Solving at runtime:** don't solve poses at runtime (a third of a second per swing). Solve offline and bake (`QuinqueAimBake`).
  Continuity weights stop wrist flips.
- **Fixes that failed and were dropped:**
  - For the coat: a centre panel (the forward leg went through it).
  - For first person: the true eye (a hilt filling the screen), and a delta at the old spot (cuts going into the screen).

  Try the idea, render it, and drop it the moment it looks wrong.
- **Structures:**
  - Build on the footprint's highest ground (`DryLand.lift`).
  - One of each (`PlaceClaims.ONE_OF_A_KIND`).
  - Log every place built, so a duplicate can be traced from the owner's log.
- **If the owner runs several AIs at once:** they will collide on version numbers, the README and this file. Work one after another on
  this branch, or each on its own branch from it, one list item each, merged in order.

## Where things stand

| Version | What | State |
|---|---|---|
| 4.1.8 | Kick Master's twirling speed kick | pushed, EXIT 0 |
| 4.1.9 | The inventory crash; the hideout, the CCG building, Anteiku, the library and Ken's tower in any biome, one of each | pushed, EXIT 0 |
| 4.1.10 | The Ukaku's wing slams: chamber, drive, overshoot, recoil, the whip; the guard's flare and settle | pushed, EXIT 0 |
| 4.1.11 | The slamming wing stretches out as far as the slam hits, a lash of fire; the whip keeps pace with each blow | pushed, EXIT 0 |
| 4.1.12 | Places never nowhere, `/kagune places`; slams without particles and 1.75x as long; each coat half on its own leg | pushed, EXIT 0 |
| 4.1.13 | Every slam an arm (behind a new shoulder) and a hand, aligned on its target and clear of his posed body; the guard clamped round him clear of his raised arms; `/kagune places` makes each place's chunk and says built or stopped; coat previews | pushed, EXIT 0 |
| 4.1.14 | One-of-a-kind places at any dry spot their set offers, the first the world builds kept with the world (`PlaceClaims`); coat tails drawn on the legs; each Ukaku wing slam plays out in full (1.3x damage) | pushed, EXIT 0 |
| 4.1.77 | Arima looks at his broken blade: raised to his chest over `RAISE` (10), his head aimed at where it snapped (`snapped`, `gaze`: bowed 25 to 33 degrees), staring 2 s while it sinks (`LOOK_SAG`, never frozen), then the 4.1.76 fall (`REAPER_END_FALLS` 52, dead at 128); `AgentTests.reaperFallen` (7 sabotages caught); renders `arima-end-4.1.77.*`, `arima-look-4.1.77.png` | pushed, EXIT 0, CI green |
| 4.1.76 | Arima's end remade: his one-handed guard, the blade shattering all at once (tick 2), his arm slack, then he falls straight on his face from his feet (`fall` over `TOES`, `t` squared, 16 ticks), lying 3 s, dead at 82; 4.1.74's piece-by-piece break, lowering and kneel removed (the owner: the kneel read as a "michael jackson" lean from the side); `AgentTests.reaperFallen`/`reaperEnd` (14 sabotages caught); renders `arima-end-4.1.76.*` | pushed, EXIT 0, CI green |
| 4.1.75 | Arima falls on his face: knelt and bowed, then toppled forward `t` squared about his knees over 12 ticks (`ReaperBladePose.fall`), flat face down on the ground, still 2.8 s, dead at 200; the hilt turned flat beside him (`LIE_WRIST`); the shards carried between his body's frames (`carried`), lying still as he falls; `AgentRenderer.m_6441_` 0 in his end (never tipped on his side); a thud; `AgentTests.reaperFallen` (12 sabotages caught); renders `arima-*-4.1.75.*` | pushed, EXIT 0, CI green |
| 4.1.74 | Arima: Dawn out of his hands through the change (`letGo`); the change slowed 1.75x (9.5 s, `changeAge`); his end 8.5 s (lowered, held still while the blade breaks point first over 3 s, slack, a slow kneel, 2 s still), `ReaperBladeRules.level`/`breaksAt`, `ReaperBladeMesh.brokenAt`; `cleanse` every tick of his end; no official NPC's foe (`EtoBrain.foe`: Roma's tendrils held him), nor a companion's arm, a child's kagune, Ken's push; `AgentTests.reaperSlowed` (22 sabotages caught), the tests' `within` (19 proximity checks blind to a missing line, one dead since 4.1.38); renders `arima-*-4.1.74.*` | pushed, EXIT 0, CI green |
| 4.1.73 | NPCs never in the ground (`Footing`/`NpcFooting`: every put-down on its floor's top with room; a guard over every NPC; every template's 117 NPCs checked, `tools/npc_spots.py`); lag check (a lost partner's 72-block search each second, now each 30 s); structures and official NPCs checked; `NpcTests.footing`, `lag173`; renders `npc-spot-4.1.73.png`, `ccg-banner-4.1.73.png` | pushed, EXIT 0 |
| 4.1.72 | The hotel replacing the Small City: the owner's humanhotel.schem beside about one village in four, 14 chunks out, on the first side clear of every other village, never overlapping another hotel, turned to face it; plains only (vanilla's and Biomes O' Plenty's open grasslands, every chunk of it); 20 humans at 10 homes, 6 investigators on an open-sky patrol; the city no longer built, its maps retired; protocol 126; `NpcTests.hotel`; renders `hotel-*-4.1.72.png` | pushed, EXIT 0 |
| 4.1.71 | The CCG faction through the General: why (the bureau's own answer), 5 ghouls, sworn in with a quinque; his dailies (ghouls, gang ghouls, paper); investigators and the division never go for a member, every other ghoul does; 1.3x damage to ghouls; the quinque case emblem and a cyan blue name; `NpcTests.ccg`; renders `ccg-general-talk-4.1.71.png`, `ccg-emblem-4.1.71.png` | pushed, EXIT 0 |
| 4.1.70 | The Anteiku faction through Ken: why (Anteiku's own answer), 8 cocoa beans, 3 investigators; neutral to every ghoul and gang but the CCG (`GhoulNpcBrain.wanted`/`fair`); one more bar's capacity, the bar finer and longer in its own spot (half pixels); the coffee cup and a light brown name (`Style.withColor`); `NpcTests.anteiku`; renders `anteiku-ken-talk-4.1.70.png`, `anteiku-bar-4.1.70.png`, `anteiku-emblem-4.1.70.png` | pushed, EXIT 0 |
| 4.1.69 | Ken's sit for a stiff body (no knees on companions): both legs flat on the deck, leaning back on his right hand, the left on his thigh, his heels sliding out as he sits; `EtoPose.sitDrop`, `PoseFade` keeps the feet on the floor getting up; renders `ken-sit-4.1.69.png`, `ken-sit-before-after-4.1.69.png` | pushed, EXIT 0 |
| 4.1.68 | Ken's cyber tower deleted (structure, set, pool, template, every list of places); Ken made with Anteiku, on its roof's south edge, home "Anteiku's roof"; render `ken-anteiku-roof-4.1.68.png` | pushed, EXIT 0 |
| 4.1.67 | The old to-do: "Quit the gang" at each leader (asked, not a ban), a marriage to another gang's leader takes the player out of theirs, a leader's spouse in another gang demands they quit (greeting and near), the General's and Eto's Touka scenes; `GangWords`, `GangPlayer.quit`; render `gang-leaving-4.1.67.png` | pushed, EXIT 0 |
| 4.1.66 | The cookbook in the quest menu's style: `CookbookView` (pure; a tab per chapter and the effects, dish slots, the dish's page with its recipe, the dishes that give an effect), `UiCanvas` items, textures and words; `QuestTests.cookbook`; render `cookbook-4.1.66.png` | pushed, EXIT 0 |
| 4.1.65 | The king's choice in the quest menu's style: `KingChoiceView` in the quest box (header, rule, the cards, a hold button filling); `KingChoiceScreen` scales the box; `QuestTests.kingChoice`; render `king-choice-4.1.65.png` | pushed, EXIT 0 |
| 4.1.64 | The path choice in the quest menu's style: `PathChoiceView` (pure; board, title, rule, quest cards in path colours, the Awaken button filling as held), the screen draws through it; `PathScreenTests` (board, rule, drawn by the view); render `path-choice-4.1.64.png` | pushed, EXIT 0 |
| 4.1.63 | The gender screen in the quest menu's style: `GenderView.Layout` inside a box (`box`, `ruleY`), cards and buttons as the quest menu's; `FamilyTests.screen`; render `gender-screen-4.1.63.png` | pushed, EXIT 0 |
| 4.1.62 | The kakugan screen in the quest menu's style: `KakuganScreenLayout` inside a box (`box`, `ruleY`, `tile`, `MAX_CELL` 36), `KakuganView` in the quest menu's pieces; `KakuganTests.screen` (box on screen, everything in it); render `kakugan-screen-4.1.62.png` | pushed, EXIT 0 |
| 4.1.61 | The Pale menu in the quest menu's style: `PaleMenuView` on `QuestFrame` (BASE FORM/MOVES/THE OWL/MUTATIONS, UNLOCK and the Instant Big Punch switch as the server takes them), `PathSilhouette.owl`; `QuestTests.paleMenu`; renders `pale-menu-4.1.61.png`, `pale-owl-4.1.61.png` | pushed, EXIT 0 |
| 4.1.60 | The Graft menu in the quest menu's style: `GraftMenuView` on `QuestFrame` (STAGES/MOVES/MUTATIONS, GROW INTO IT and UNLOCK as the server takes them), `PathSilhouette.graftForm` (each stage on the game's own rest pose), quest and lesson names quoted in all four menus; `QuestTests.graftMenu`; renders `graft-menu-4.1.60.png`, `graft-kagune-4.1.60.png` | pushed, EXIT 0 |
| 4.1.59 | The Bikaku menu in the quest menu's style: `BikakuMenuView` on `QuestFrame` (FORMS/MOVES/KAKUJA/MUTATIONS, USE THIS FORM), `PathSilhouette.bikakuForm`, `QuestFrame.opener` (teachers' lessons named in all three menus); `QuestTests.bikakuMenu`; render `bikaku-menu-4.1.59.png` | pushed, EXIT 0 |
| 4.1.58 | The Rinkaku menu in the quest menu's style: `RinkakuMenuView` on `QuestFrame` (FORMS/FINAL KAKUJA, WEAR and CHOOSE buttons, the road, the king's emblem), `PathSilhouette.rinkakuForm`, `KingChoiceView` emblems shared; `QuestTests.rinkakuMenu`; render `rinkaku-menu-4.1.58.png` | pushed, EXIT 0 |
| 4.1.57 | Places cleared and set on the ground: `SitePrep`, `ControlStructures.clearing`/`apron`/`settled`, `clearing.nbt`, steps `top_layer_modification`; every template's panes joined (`tools/join_panes.py`: CCG 3134, hideout 2490); `NpcTests.sitePrep`; renders `ground-*-4.1.57.png`, `ccg-glass-4.1.57.png` | pushed, EXIT 0 |
| 4.1.56 | The Ukaku menu in the quest menu's style: `QuestFrame` (shared pieces), `UkakuMenuView` (box 740 x 440, ORGAN/MUTATIONS, stage cards and detail, road, UNLOCK button), `UkakuMenuScreen` (no overlay, tab and pick), `PathSilhouette.ukakuKakuja`; `QuestTests.ukakuMenu`; render `ukaku-menu-4.1.56.png` | pushed, EXIT 0 |
| 4.1.50 | Joining gangs: `GangPlayer` (AOGIRI_CCG 5, CLOWN_GAS 3, BAN_OVER 10, `named`), `GangServer` (`PlayerPersisted` keys HexGang*, `died`, `name` (NameFormat), `loggedIn`, `talk`/`ask`, `JOKES`/`FUNNY`, `threatens`, `stance`), `S2CGangName` (57), `GangClient`, `EtoServer.withGang` (SAY_3, key "gang"), `GhoulNpcBrain.wanted`/`hurt`; emblems `tools/gang_emblems.py`, `assets/minecraft/font/default.json` (build.py NEW_PREFIXES: that one file); `NpcTests.joining`; protocol 123 | WIP: `docs/handoff/wip-4.1.50.patch` (quick_check with the verifier, NpcTests, CompanionTests, BikakuWormTests pass; full build not run) |
| 4.1.49 | Roma: `Persona.ROMA` (curves 0.75, legs 0.7, build 0.94, slim, `kickMaster`, `tendrils` 4, `ROMA_KAGUNE`), `RomaNpc`, `ControlEntities.ROMA`, `RomaScript`, `talk/roma.txt`, `Courtship.ROMA`, shared tables, circus template (38, 1, 24); `NpcTendrils` (`KICK_RANGE` 2.8, Impale 3.5-10, Hurl 6-22), `GraftRig` TRANSPLANT 4 tails (`TAIL_LOW`), `GraftMove.ROMA_IMPALE/HURL/THROW`, `GraftRules.heldPoint`, `GraftRenderer` companion pass; textures (`companion_textures.py`: the owner's second skin, NewRoma.png; EYES row 6 under her lashes); `tools/RomaPreview.java`, GraftPreview `roma`; protocol 122 | pushed, EXIT 0 |
| 4.1.48 | The circus tent (`hexkagune:circus`, the owner's Alpha schematic via `tools/companion_structures.py` `alpha`/`floor`/`people`; dirt-path floor, `circus_anchor` at the floor's middle (38, 0, 24); one per world: `DryLand.PLACES`/`CIRCUS_SALT` 1324066193, `PlaceClaims`, `ControlStructures.ONE_OF_A_KIND`); four Clowns (`HexCircus`, `HexCircusMask`, `HexGang` -1: `GhoulNpcBrain.circus`, `atCircus`, `clearWay`, `CIRCUS_STROLL` 7, `CIRCUS_LEASH` 20); `Gang.rival` (Clowns vs Gas Masks on sight); Ayato and Noro `slim()` (`narrow_arms`), `AYATO_SHARE` 0.42, `NORO_SHARE` = `PLAYER_SHARE` 0.52 (the owner: "Reduce the player's muscle size to be the same as Noro."), `PLAYER_ARM_OUT` = `armOut(PLAYER_SHARE)`; `tools/structure_render.py`; renders `circus-tent-4.1.48.png`, `companion-builds-4.1.48.png` | pushed, EXIT 0 |
| 4.1.47 | The arm blade rebuilt: `BladeArmModel` (ribbons `RIBBONS` 2 x `RIBBON_TURNS` 1.3 with `crack` veins, `blade` a single-edged great blade, `edgeAt`/`spineAt`, gleam GLOW strips, `turned` sink: edge on z +), `BladeArm` (`STEPS` 4, `SWEEP` 2 `sweeps`, `FINISHER` 3, `BLADE_LENGTH` 26, reach 3.2), `BladeArmPose` (four edge-first keys, `stand(time, seed, still)`, guard up and out), `GhoulNpcClient.blade` the stance, `GhoulNpcCombat.bladeCut` (all slashes, sweep on `fair`), `NpcTests.bladeLow`; tools `BladeArmShowcase`, `BladeArmMoves`, `BladeArmColours`, `BladeLow`; protocol 121 | pushed, EXIT 0 |
| 4.1.46 | The CCG's ghoul division: `Gang.CCG_DIVISION` (ordinal 3), `NpcIdentity.ccg`/`kakugan`, `AgentBrain.ccg` everywhere the CCG spared its own (`wanted`, `attacked` both ways, `hurt`, the Reaper's sweep, `AgentArms.foes`, `EtoBrain`), `GhoulNpcBrain.division` (`AgentBrain.wanted`'s rules; `marked`, `shows`), `called` both ways, FORMAL_SET, human meat, no `dawn`, `AgentSpawner.division` (0.35, three or none), `GhoulNpcRenderer.Eyes` none, `GhoulNpcTalk.CCG_WORDS`; a mate's stray blow no grudge; `NpcTests.division`; `tools/DivisionPreview.java`; protocol 120 | pushed, EXIT 0 |
| 4.1.45 | Roaming gangs: `npc.Gang` (CLOWNS, GAS_MASKS, 3-5, `faces` all different), `NpcIdentity.gang` bits 44-45 (`ganged`: a DRIFTER), `GhoulNpcSpawner.gang` (`GANG_CHANCE` 0.3, `GANG_ID` number in saved data), `GhoulNpcBrain` `roam`/`mates`/`wanted`/`hurt` (`GANG_CALL` 24, `GANG_GRUDGE` 2400), masks on the face (`OutfitLayer.faction`, `Outfits.geometryOf`), 8 paints (`tools/gang_masks.py`), preview `tools/GangPreview.java`; `NpcTests.gangs`; protocol 119 | pushed, EXIT 0 |
| 4.1.44 | The hideout by `DryLand.land` (city-like: 9 x 9, `HIDEOUT_FLAT` 14, `DEEP` water, no sea/steep), the CCG building the flattest of 25 spots (±2 chunks), `offered` dropped from the hideout, CCG and jigsaw places (the claim keeps them one); `NpcTests.worldgen` hideout land | pushed, EXIT 0 |
| 4.1.43 | Duplicates for good: `ControlStructures.onlyIfClaimed` (the claim taken in the stub's pieces generator: built only if claimed; `making` removed), `/place` through `claimed` (sites and places), `EtoBrain` second body leaves while hers answered within 400 ticks; CI's queue stuck: jars delivered from `downloads/` | pushed, EXIT 0 |
| 4.1.42 | Worldgen: `Site.onPurpose` needs the live world (the duplicates); `ControlStructures.foundation` (`afterPlace` of City, Site, DryPlace; `DryLand.footing`); `DryPlace.flattest` (25 nearest shifts, `roughness` <= `PLACE_FLAT` 6), `dryGround` <= `flatFor(size)`; `CityPlan.land` (pure: 17 x 17, `FLAT` 10, sea/steep/deep); `STEEP` tag; `TakenPlaces.takeApart`/`crowded`, `PlaceClaims.APART` 32; `NpcTests.worldgen` (made-up land) | pushed, EXIT 0 |
| 4.1.41 | The Reaper's blade turned over about its length in every pose (the owner: "Just completely invert it"): `ReaperBladeMesh.inverted` as the blade's builders' point and way maps (not the golden case's), `drawnV` in `ReaperBladePose.shard` | pushed, EXIT 0 |
| 4.1.40 | `village/RetiredSites` (maps 3, 4 for the old towns' people; `SiteMaps.byMap` and the tool's template fall back to it); `QuestServer.loggedIn` opens every gate for a finished log; `QuestTests.forGoodLater`, `NpcTests.cityFixes` | pushed, EXIT 0 |
| 4.1.39 | Small City fixes: `VillageHumans.HOME`/`HOME_RANGE` (a brought human keeps about its village: `HumanBrain`'s homeless wander turns back); `City.ground` cached (`GROUNDS`, `measure`); protocol 118; `NpcTests.cityFixes` | pushed, EXIT 0 |
| 4.1.38 | The Reaper re-dresses two minutes after a fight: `AgentBrain.dress` (`REAPER_REDRESS_CALM`, `CcgAgent.DRESS` 13, `REAPER_STOW_TICKS` 44 + `REAPER_DON_TICKS` 28, `REAPER_DRESS_SWAP`), `ReaperDressPose` (`LEFT_KEYS`, `held`, `bladeOut`, `coatAge`), the blade and coat layers follow it; `AgentTests.dressing` | pushed, EXIT 0 |
| 4.1.37 | The Reaper's change: untouchable (`AgentBrain.untouchable`, `attacked` cancels `LivingAttackEvent`, `EtoBrain.spared` asks it so the original's `validSwipeTarget`/`isHostileTo` say no, `PlayerArms.mayHit`, `pin`/`free` each tick), `REAPER_SHED_WAIT`; a real coat (`ReaperCoatPose`, `ReaperCoatLayer`, the formal overcoat in white: `tools/reaper_coat.py`), shed 46 ticks, swap at 3; the draw 62 ticks; the one-handed point-down guard (`ReaperBladePose.GUARD`, `PRESENT`, `HANG_X`, `oneHand`, `bothHands`); blade and brawl on `KaguneClash`; `tools/ReaperBladePreview.java` | pushed, EXIT 0 |
| 4.1.36 | The Small City in worlds: `hexkagune:small_city` (`ControlStructures.City`), placement `hexkagune:city` (`CityPlacement`, a `RandomSpreadStructurePlacement` offering a city's four start chunks), `CityPlan` (slots, turn, quarter per slot), `City.ground` (9 x 9, rivers allowed, no sea, FLAT 12, the median, >= 64), `inCity`/`holdsPlace` (places and cities never overlap), `DryLand.CITY_*`; villages and their files deleted; `VillageHumans`; `tools/city_render.py --placed`; `NpcTests.smallCity` | pushed, EXIT 0 |
| 4.1.35 | The Small City cut for 1.20.1: `tools/city_structure.py` (the save to four quarter schematics), `tools/exploration_structures.py` sites `small_city_0..3` (maps 7-10, ground 6; swaps: pale oak wall sign, tuff bricks, wildflowers, infested stone; banner `Patterns`; `NO_ENTITY` shelves), `tools/city_render.py` (isometric from the tiles); `NpcTests.smallCity`; not placed in worlds yet | pushed, EXIT 0 |
| 4.1.34 | The quest menu a compact box over the world: `QuestJournalView` (640 x 380, cards, checklist, badges, mastery bar, `roadmap`), `QuestStyle`, `QuestScreen` without the overlay; `tools/UiPreview` draws it over a stand-in world (`-Dmx -Dmy` for hover); `QuestTests.journal` | pushed, EXIT 0 |
| 4.1.33 | Quests finished for good: `QuestLog.finishAll`/`finished` (saved `all=1`, `state` DONE for every PATH/GHOUL quest), called by `QuestServer.completeOpen`; hunts and favours unchanged; `QuestTests.forGood` | pushed, EXIT 0 |
| 4.1.32 | A quinque's M1 first: `WeaponClient.quinqueFirst` (HIGH, lands the hit via `MultiPlayerGameMode.attack` m_105223_, `Minecraft.f_91072_`, cancels the click ahead of the original's kagune listener), `KaguneCombat.startSwipe`/`quinqueOut`; `QuinqueTests.first` | pushed, EXIT 0 |
| 4.1.31 | Arima phase 2 part 4: his end at 6 HP (`REAPER_END_HEALTH`, `endClamp`, `AgentBrain.ending`, `CcgAgent.END` 12): the blade lowered, shattered (`ReaperBladeMesh.broken`, shards), on his knees (`ReaperBladePose.end`, `KNEEL`, `KNEEL_DROP`), dead by the blow that brought it | pushed, EXIT 0 |
| 4.1.30 | Arima phase 2 part 3: the rush in, stopping at blade reach (`AgentBrain.rush`, `ReaperBladeRules.RUSH_*`), sidesteps of shots and blows (`AgentBrain.dodge`, `ReaperBladeRules.dodge`/`committed`), close combat only | pushed, EXIT 0 |
| 4.1.29 | Arima phase 2 part 2: the change at 90 HP (`REAPER_SHED_HEALTH`), the golden case drawn from his back and split open on its hinge, the blade slid out, the case dropped and sunk (`ReaperBladePose.draw`, `ReaperBladeLayer`); his blade (`ReaperBladeMesh`, NPC only) and heavy string (`ReaperBladePose.STRING`, `ReaperBladeRules`, `AgentBrain.blade`), 2 bleed stacks a hit (`UkakuCrystalCombat.agentBleed`) | pushed, EXIT 0 |
| 4.1.28 | Arima phase 2 part 1: the jacket off at half health, once (`AgentBrain.shed`, `CcgAgent.SHED` 7, `AgentPose.shed` keyed by `SHED_KEYS`), the look `ccg_reaper_bare` (`AgentRules.REAPER_BARE_LOOK` 10, `reaperLook(index)`), faster; `shedClamp`; invulnerable while shedding; saved as `HexCcgAgentBare` | pushed, EXIT 0 |
| 4.1.27 | Arima part 5: the ground strike's tendril replaced by Dawn's blades growing out of the mark (`ReaperTendril.blades`, `QuinqueDawnMesh.risingBlade`, `client/render/ReaperTendrils` draws them leaned and grown by `out(age - delay)`); gameplay unchanged | pushed, EXIT 0 |
| 4.1.26 | Arima part 4: Dawn a smooth mesh (`quinque/QuinqueDawnMesh`: guard panels, spike, shaft, built in item pixels along x, `STEPS` cached open steps); its guard turned inside out into a forward cup as it fires (`REVERSE`), lightning at the point; `QuinqueMesh` per-quinque turn/telescope/case/open; `Quinque.dawnOpening`/`QuinqueClient.dawnOpening` from the stack's shot tags; tools/quinque_models.py `MESHES` += dawn | pushed, EXIT 0 |
| 4.1.25 | Arima part 3: up close the Reaper throws Kick Master's string or the Speed Blitz (`AgentBrain.brawl`/`brawlBlink`/`brawlLands`, `AgentRules.REAPER_BRAWL_*`, `brawlStyle`), synced as `CcgAgent.STRIKE` + ordinal, drawn by `AgentPose.brawl` (PaleMotions.strike, Dawn carried at the shoulder and raised by the lean); `AgentTests` (Dawn above the ground through every strike) | pushed, EXIT 0 |
| 4.1.24 | Arima part 2: `hexkagune:reaper_mansion` (the owner's schematic via `tools/companion_structures.py`: short grass as 1.20.1's grass, bottom layer cropped, under-lawn air filled, `hexkagune:mansion_anchor` on the lawn's layer, Arima baked in with ForgeData `HexCcgAgent`/`RankLevel` 10/`Mansion`); a one-of-a-kind dry place (`DryLand.PLACES`, `MANSION_SALT`, `PlaceClaims`, `ControlStructures.ONE_OF_A_KIND`); his home in the garden (`AgentBrain` HOME_X/Y/Z, `AgentRules.REAPER_HOME_STROLL/LEASH`); never in the wild (`REAPER_CHANCE` 0) | pushed, EXIT 0 |
| 4.1.23 | Arima part 1: Dawn's case every time he falls (`AgentRules.dawnChance` 1); `hexkagune:slain_reaper` (data/hexkagune/advancements, impossible criterion awarded by `AgentBrain.reaperSlain`) and his 60 mastery for his killer and every player who hit him in the last 5 minutes (`AgentBrain.reaperStruck`, `AgentRules.REAPER_SHARE_TICKS`); advancement API allowlisted with joined.tsrg and intermediary evidence | pushed, EXIT 0 |
| 4.1.22 | First person for the bone greatsword, flesh hammer and crystal spear through the posed body (`ArmItemLayer.throughBody(pose, posed, slim, false)`, `WeaponLayer.mesh`; `FirstPersonQuinque.arm`); launcher and whip kept theirs; `QuinqueTests.firstPersonArms` | pushed, EXIT 0 |
| 4.1.21 | The katana's five cuts solved hands first (`QuinqueAim.HAND` 3 per pixel against `BLADE` 40), wrists turning the blade in the fist (`QuinquePose.Key.wristX/Z` within `QuinqueAim.WRIST_X/Z`, drawn by `QuinqueItemRenderer` via `client/QuinqueWrists`, `QuinqueGrip.wristInItem`), cuts rewritten on renders; `QuinqueTests.aimed` measures the arm's real turn | pushed, EXIT 0 |
| 4.1.20 | The katana a smooth mesh grown out of kagune (`quinque/QuinqueMesh`, `ArmMesh.Builder`'s frame), drawn by its own item renderer (`client/QuinqueItemRenderer`, Forge's `IClientItemExtensions` given in `ControlItems.MeshQuinque.initializeClient` through DistExecutor), turned onto the handheld diagonal as the baker's boxes; its draw continuous; enchantment glint; `QuinqueTests.meshes`; `tools/QuinqueMeshPreview.java` | pushed, EXIT 0 |
| 4.1.19 | Melee quinques (spear, katana, mace, blade, Dawn) drawn in first person through the third person chain, the body's step in, seen from behind the head looking down, zoomed (`client/animation/FirstPersonQuinque`, `ArmItemLayer.throughBody`); `QuinqueTests.firstPerson`; `tools/FirstPersonPreview.java` | pushed, EXIT 0 |
| 4.1.18 | The katana model (`quinque_models.katana_parts`, its own texture patches); each quinque its own string (`Quinque.steps/finisher/swing(step)`, `Quinque.next(q, ...)`); the katana's five by intent (`QuinquePose.katanaAims`, solved by `QuinqueAim`, baked in `QuinqueKeys` by `tools/QuinqueAimBake.java`); seven draw frames (`OUT_STEPS`) | pushed, EXIT 0 |
| 4.1.17 | Coat tails overlap at the middle and a hip lining (`outfit_models.overlap`); Ken's tower on the ground and every jigsaw place lifted onto its footprint's highest ground (`DryLand.lift`, `DryPlace.raised`); a standing place adopted as the world's one, duplicates logged, `making` only false for /locate (`PlaceClaims`); planted Ukaku spikes keep their shooter's colour | pushed, EXIT 0 |
| 4.1.16 | Every woman blushes through a kiss and after, as deep as its mood (`KissBlush`, `MoodKiss.blush`); Ayato's eyes repainted (`companion_textures.REPAINT`), nothing under hair (companions); kids' blinks and kakugan on both layers (`blink_skins.py`, `child_skins.py`) | pushed, EXIT 0 |
| 4.1.15 | Scenes that know who the player is with (`relation` flags); the night's captions (`NightWords`); five kid games, a two-page play menu, no touching sons from their teens on; six new kisses (`MoodKiss.of(mood, Moment)`), the phases blended, every gesture crossfaded (`PoseFade`) | pushed, EXIT 0 |

Branch: `claude/hopeful-euler-dnx1ub` (fast-forwarded from `claude/next-ai-4-1-6-handoff-sqv9e4`, which stops at 4.1.8). A new session's own
branch usually starts at `main` (still 1.3.0): fast-forward it first (`git merge --ff-only origin/claude/hopeful-euler-dnx1ub`).

## 4.1.9, what and where

The owner (quoted exactly): "Important top priority bugs: Cant find CCG building, Aogiri Hideout or Kaneki's building AT ALL not even with
locate. Opening the inventory crashes the game instantly." Then: "Make sure to include a bunch of, like, I don't know which biomes you're
setting, but it should be able to spawn literally in any biome as long as there's only one of each structure aside from the villages, of
course. It should also be fully compatible with Biomes of Plenty, so that should be something already in place."

- **The crash** (their report: `AbstractMethodError: ControlItems$$Lambda does not define ... m_257865_ ... CreativeModeTab$DisplayItemsGenerator`):
  the stub named `DisplayItemsGenerator.accept` by its Mojang name and the allowlist called it "unobfuscated"; it is `m_257865_`
  (joined.tsrg `cef$b accept (Lcef$d;Lcef$e;)V m_257865_`). Every lambda of the four creative tabs (4.0.2) implemented a method the game
  never calls. Stub and allowlist fixed; `OutfitTests` reads the compiled `ControlItems.class` for `m_257865_`. Lesson: a lambda or a class
  that implements a Minecraft interface takes its method name from the stub, and nothing checks that at run time but the game.
- **Why no place was found:** every structure's `biomes` was its own short list (`has_structure/*`, `prefers/*`), and `ControlStructures.biome`
  tested that list at every offer. The owner's pack runs Regions Unexplored (TerraBlender): its biomes were on no list, so near the world's
  middle no offer passed, `DryLand.pick` gave up and the log said "all sea". Now:
  - every structure's `biomes` is `#hexkagune:overworld` (= `#minecraft:is_overworld`): it only keeps a set to the overworld (a set runs only in
    a dimension whose biome source makes one of its biomes: `ChunkGeneratorStructureState.hasBiomesForStructureSet`);
  - every place type overrides `Structure.findValidGenerationPoint` (`m_262864_`, public, not final in 1.20.1; world generation and
    `StructureCheck.canCreateStructure`, /locate's, both call it) to leave the game's biome-list test at the start out;
  - `ControlStructures.biome` is now "any biome but a wet one" (`hexkagune:wet`, which also takes Forge's and the common swamp/water tags,
    each optional). The dry-land checks (`DryLand.dry`) are unchanged.
- **One of each:** Anteiku, the library and Ken's tower were rings of three (`concentric_rings`, count 3). They are random spreads now
  (40/20, their old salts; `DryLand.PLACES`, `placeSalt`), each the world's one (`DryPlace.chosen`): the first offer, nearest the middle,
  where `DryLand.firstPlaced` finds it stands once laid out exactly as the world lays it (`DryPlace.at`: the game's own 9-argument
  `GenerationContext`, its random seeded from the seed and the chunk, then `here`, the 4.1.1 dry-land shift loop), at least
  `PLACE_APART` (12) chunks from every village offer and from the hideout, the CCG building and the places chosen before it. At most
  `PLACE_TRIES` (24) offers that look like land are laid out in full. A place is found by its key in the structure registry
  (`DryPlace.id`, `byId`); a name that can't be read builds nothing that time (never many).
- **The spreads are the world's own:** `ControlStructures.spread` reads each set's placement from the structure set registry and asks it
  (`RandomSpreadStructurePlacement.getPotentialStructureChunk`, `spacing`), so a mod that changes every set's spacing (Structure Essentials,
  in the owner's pack, has a global multiplier) moves the offers with it. With the files' numbers the offers are exactly 4.1.8's
  (`NpcTests.oldOffers` holds the old algorithm to prove it).
- **Tests:** `NpcTests.onePerWorld` (new), `NpcTests.dryLand`/`ccgBuilding`/`mansion` and `KitchenTests` updated to the new rule.
- **Not tested in game.** In an old world a place whose chosen spot was generated before 4.1.9 can't appear there; `/place structure
  hexkagune:<id>` builds one. The log names each place's spot the first time a world works it out ("HexKagune: this world's ...").

## Next, in this order

1. **Ask the owner how 4.1.9 played** (the inventory, `/locate` of all five in a new world) and fix what they report.
2. **4.1.10 did the first part of the owner's ask from before the bugs**, quoted exactly: "after that please put all your focus into the combat
   rework, and kagune rework for the ukaku, this model is very expensive and I need you to focus on what's hard, study complex fighting moves,
   and combos and make it look smooth and beautiful." Ask how the slams play (`docs/handoff/ukaku-slams-4.1.10.gif`, `ukaku-slams-4.1.10.png`
   against `ukaku-slams-before-4.1.10.png`: the same camera on 4.1.9's code; `ukaku-slams-zoom-4.1.10.png`: the overhead and twin slams,
   before over after).

## 4.1.10, what and where

- **The flaw it fixed:** both the wings (`WingRig.phases` then) and the body (`UkakuBody.Keys`) slowed to a stop as the blow landed and held.
- **`WingRig`:** each slam is a `Blow` (keys CHAMBER, IMPACT, OVERSHOOT, RECOIL as {sweep, roll}; the drive's, overshoot's and recoil's
  shares; the move's ticks), timed by `beats` (chamber, load end, impact = the server's, overshoot end, recoil end). `slam`: rest to chamber
  (smooth), a `LOAD` further back, the drive on an ease-in (`DRIVE_SNAP` 2.4: fastest landing), the overshoot on an ease-out, the recoil,
  back to rest. `tip`: a damped spring (`WHIP_PERIOD` 3.2 ticks, `WHIP_DAMPING` 0.42) on the root angles, integrated from progress 0 every
  frame (pure); `slamFrame` bends the wing toward it with `UkakuModel.Bend` (at most `WHIP_MOST`), its fan folded by `slamFold` (tucked
  `SLAM_TUCK` on the drive, flared `SLAM_FLARE` on the hit). `slamThrust` (the fire streaming back), `slamHard` (the RC hardened),
  `counterPull` (the other wing). `guard(amount)`: flare (`GUARD_OPEN`), shut past (`GUARD_PAST`, `GUARD_OVER`), settle.
- **`UkakuBody`:** the slam patterns (`slams`) use `Keys.blow`: their impact `BODY_LEAD` before the wing's, a drive of at most `BODY_DRIVE`
  (0.55 of the time to it) on an ease-in (`BODY_SNAP`), a `BODY_LOAD` past the wind-up, carried `BODY_OVER` past the follow-through key.
- **Tests:** `UkakuTests.slamBeats` (keys on their beats, fastest at the impact, still speeding up into it, carried past it, overshoot and
  rebound, the tip trailing then passing, the feathers, the counter-pull, the guard, never below -0.15 at any 1% of any slam in TWIN or
  RABBIT at three masteries, both sides) and `slamBody` (rest, no snap at 1/400, limits, mirrored, speeding up into the hit and on past it).
  `wingSlams` measures each wing alone now (a slap follows through across the middle).
- **Previews:** `tools/art-preview.sh <out> slams slams-gif` (`ArtPreview.slams`: each slam key by key and its tip's path from above, dots
  every 1/60, blue to white to the impact, yellow to red after: their spacing is the speed; `slamsGif`: the whole string at 30 fps).

## 4.1.11, what and where

The owner (quoted exactly): "I thought it was clear, but I guess not. You can make the wing stretch further forward, because it currently just
feels like a little chicken wings, bro. Whenever they stretch to slap something, they can stretch to be larger in size and length as well. You
don't have to be limited by such small range. I mean, it is what it is. You do you."

- **The mismatch:** the slams hit `UkakuRules.slamReach` (the original's `KaguneProfile.attackRange`, 7.5 + 0.0075 a mastery point, at most 15,
  × `SLAM_REACH` × the stage's `rangeMul`: about 8.3 blocks at 300, 12.75 at 1000) with a wing 1.6 to 4.3 blocks long.
- **`WingRig`:** `lash` (0..1: out through the drive, held through the overshoot, back in by `LASH_BACK` past the recoil); `stretchMost`
  (the wing's scale at the impact: the reach over `TIP_SHARE` (1.4, measured: the lashed wing's furthest fire) its length, at most
  `STRETCH_MOST` 3.2); `slamFrame` now turns the wing `LASH_TURN` of the way at the root toward the lash and curls the rest (at most
  `LASH_MOST`), the lash level (`lashElevation`: 5°, the rising wing 22°) along where the blow sweeps (the root's sweep, `LASH_TRAIL` of
  the whip's trail), its feathers folded `LASH_FOLD` but for the hit's flare. Uniform scale on purpose (UkakuFlame's normals come from the
  frame's axes; the owner asked for "larger in size and length").
- **The whip fixed:** its spring's period is `WHIP_PER_DRIVE` (1.4) of each blow's drive in ticks (4.1.10's fixed 3.2 ticks left a slap's tip a
  quarter turn behind at its impact).
- **Tests:** `UkakuTests.slamBeats` (the tip reaches 0.8 to 1 of the reach (+1.5) at 300, 500 and 1000; its own size at rest; `TIP_SHARE`
  measured), the rig's bound is the reach for slams, `rig` and the fine ground check unchanged otherwise.
- **First person** draws the own wings at 0.45 alpha: a lash crosses the view. Ask the owner if it bothers them; if so, lower `alpha` for a
  lashed wing in `UkakuClient.rigInput` (first person only).
- Previews: `docs/handoff/ukaku-slams-4.1.11.png` (from in front: a lash at the camera foreshortens; the top-view tip paths show it), `.gif`
  (from his side). `tools/art-preview.sh <out> slams slams-gif`.

## 4.1.12, what and where (the owner was furious; read their words)

The owner (quoted exactly): "Listen, asshole, I'm seriously running out of patience. You took 40 minutes to fucking fix an issue that I couldn't
fucking locate a structure. And I still can't." (their screenshot: "Could not find a structure of type "hexkagune:aogiri_hideout" nearby", the same
for ccg_building); "The hits are way too fast, and I don't understand why the fuck did you add default Minecraft particles in the area that it
swipes from? ... these fuck ass red Minecraft dust particles in crit particles that you've added. No one ever asked for that"; "insanely fast wing
slap animation that you can barely fucking see"; of the Crimson Outfit in a jump and a kick: "I truly don't know how in this world you expected the
clothes to open like that and look good"; "How do you look at the block animation, for example, and you call that good?"

- **This machine cannot run Minecraft** (Mojang's, Forge's and Modrinth's hosts are blocked): nothing here was seen in game. The structures still
  failed for the owner after 4.1.9 and the cause is NOT known. 4.1.12 makes every one-of-a-kind place always have a spot (`Site.FORCED`: the
  nearest offer when none is dry; `DryPlace` lays it with `DryJigsaw.run`) and adds `/kagune places` (`ControlStructures.report`, filled by
  `Site.told` when a chooser runs). **First thing next: ask the owner for `/kagune places` after a `/locate`, in a new world, and the jar version
  from the mods list.** "not worked out yet" after a /locate means the game never asked our structure (the set is filtered out in that world or
  another mod intercepts: suspects in their pack: Structure Essentials, Regions Unexplored/TerraBlender, Noisium, Canary).
- **Slams:** `UkakuCombat.impact` spawns nothing for the five slam patterns (4.1.2 drew a red dust arc and crits). `ComboStyle.UKAKU_WINGS` and
  `UKAKU_LEFT_WING` stages ~1.75x as long (16, 20, 20, 22/23, 28 ticks), impacts 0.5 to 0.6; `WingRig.Blow` drive 0.28 of the move,
  `DRIVE_SNAP` 1.6, the whip fades out after the recoil; `UkakuBody.BODY_DRIVE` 0.28. `UkakuTests.slamBeats` reads each slam's timing from
  `ComboStyle` and wants a swing of at least 3.5 ticks; no particles (source check). Previews `ukaku-slams-4.1.12.*`; `ArtPreview.SLAMS` reads
  `ComboStyle` now. Damage per hit unchanged (so less per second): offered to the owner.
- **The coat:** `OutfitLayer.drapes`: each half of the tails from its own leg (`CoatDrape.over(own front, own back, {thigh, thigh}, {knee, knee})`,
  `HUNG` per half). 4.0.3 hung both fronts from the leg furthest forward (never parted): any kick or tuck threw the skirt out flat. `OutfitTests`
  to the new rule. Not previewed (no off-game renderer for the outfit layer).
- **The block (the wing guard):** NOT changed. It closes in `RinkakuBlock.RAISE_TICKS` (3 ticks) with Rinkaku's tail-block body pose
  (`RinkakuBlockPose`, in `KaguneBodyAnimation`): ask the owner what they want before building (a slower close with its own body pose was
  proposed to them).

## 4.1.13, what and where

The owner (quoted exactly): "You see the shapes you're making? That was the right idea that I wanted for the M1 attacks in the first place,
that they actually bend like that, like hands. That's what I fucking wanted. That's it. While being perfectly aligned as well, and it
doesn't like clip to the body." Then: "Continue and do not stop at any point till all tasks are deemed perfect by you".

- **The wing is an arm and a hand** (`WingRig.slamFrame`, `UkakuModel.Bend`'s new shoulder fields `base`, `shoulderStart`, `shoulderEnd`):
  - **Shoulder:** the first `SHOULDER_START` blocks run along `base` (out of his back at `shoulderSweep`: drawn back by
    `SHOULDER_DRAW` of the arm's swing past rest, to `SHOULDER_SWEEP` 40 at most; never higher than `SHOULDER_ROLL` 0). The wing then
    turns onto the arm's axis on an arc by `SHOULDER_END`.
  - **Arm and hand:** past the turn it is the old frame, moved. The hand curls past `handStraight` (overhead `SLAM_FOREARM` 0.5, twin
    `TWIN_FOREARM` 0.4 of the wing; at least past the shoulder) toward `cupToward`:
    - slap and swing: level across;
    - overhead: world `{-side*SLAM_IN, -1, SLAM_ON}`;
    - twin: `{-side*TWIN_IN, -1, TWIN_ON}`;
    - rise: up.
  - **Timing:** the arm leads by `ARM_LEAD` of the drive; the hand lands on the server's tick.
  - **No pop:** at rest the shoulder has no turn, so nothing jumps as a blow starts.
  - The guard (`guardFrame`) has the same shoulder; its wrap is never tighter than `GUARD_RADIUS` (small wings hooked into the head).
- **The keys (`WingRig.blow`) were found by search, not by hand**, against his body as the game draws it:
  - The body is `UkakuBody` mapped as `KaguneBodyAnimation.addUkaku` does. The wings are carried by `BodyFrame.carry(pose, "torso")` as
    `UkakuWingRenderer` does, and sunk-in points are counted with `BodyFrame.Solid.depth`.
  - The idle wing rolls out (`COUNTER_ROLL` -12) and `UkakuBody`'s twin wind-up keeps the head on the target (`headX` 0.15). Both were head
    clips.
  - Targets are the hand's centre line (`WingRig.slamTip`), carried:
    - overhead hand at about (0.6, 0.8, 3.3) at mastery 300;
    - twin hands at about (±0.4, 0.7, 3.2);
    - rise at about (0.8 to its side, 3.2, 2.0).
  - The probes and the optimizer are scratch code, not in the repo. Re-derive them from the tests: `UkakuTests.sunkIn` / `posed` / `carried`.
- **Tests** (`UkakuTests`):
  - `slamsClearOfHim`: every slam of the string, Rabbit Guise's left string and the guard (with the arms raised as `guarding` raises them),
    fire and crystal, mastery 50/150/300/1000, every 0.02. At most `GRAZE` (30) more sunk points than at rest; today's worst is about 26.
  - `slamsAligned`: each hand lands where it aims.
  - `slamBeats`: rewritten for arm and hand (the hand through its keys on its beats, the arm a beat ahead, the hand fastest as it lands and
    never braking, the hand trailing the arm through the drive, growth up to `GROW_MOST` / `GROW_MOST_DOWN`).
- **The preview now carries the wings with the torso** (`ArtPreview.render`: `BodyFrame.carry`), as the game does. Before 4.1.13 every
  leaning pose's preview put the wings in the wrong place.
  - Sheets: `impacts` (each slam at its hit, front/side/above), `guard`, `coats` (both coats in six leg poses through `CoatDrape` as
    `OutfitLayer` hangs them), `slams-gif`.
  - Run: `bash tools/art-preview.sh <out> impacts guard coats` after a `quick_check` build.
- **`/kagune places`** (`ControlStructures.report(LevelReader)`, `built`, `STARTS`):
  - From a player, it makes each one-of-a-kind place's start chunk in full and looks for its start: "built there", or "NOT built there"
    (something stopped it).
  - Why: each such place may stand in one chunk a world only, so a veto there (Structure Essentials' "minimum distance to other
    structures", in the owner's pack) leaves the world without it and `/locate` finds nothing.
  - New API (allowlisted with joined.tsrg evidence): `LevelReader.m_9598_`, `m_6325_`, `ChunkAccess.m_213652_`, `StructureStart.m_73603_`.
- Previews in the repo: `docs/handoff/ukaku-impacts-4.1.13.png`, `ukaku-guard-4.1.13.png`, `coat-crimson-4.1.13.png`, `coat-formal-4.1.13.png`.

## 4.1.14, what and where

The owner (quoted exactly): "Things to fix and improve: Prevent m1 spam with ukaku. Change the armors so they dont open like skirts, simply
attach to the leg properly. Fix structures still not spawning at all or able to be located (still not spawning)"

- **The places** (`ControlStructures`: `claimed`, `making`, `offered`, `dryGround`; `PlaceClaims`; `village/TakenPlaces`):
  - **Before:** the hideout, the CCG building, Anteiku, the library and Ken's tower were each allowed in ONE start chunk per world, worked
    out in advance (4.1.1 to 4.1.13). Anything that stopped them there left the world without them, and `/locate` found nothing.
  - **Now:** any spot their set's live spread offers can hold one, if it stands there:
    - hideout and CCG: `dryGround` over the footprint plus `CCG_ROUND` (dry, steepness at most `STEEPEST`, biome);
    - the jigsaw places: `here`;
    - all clear of the other sets' spots (`DryLand.near`).
  - **The claim:** the first spot the world MAKES takes the place (`making`: the context's heights are a `ChunkAccess`; `/locate`'s
    structure check only asks). `PlaceClaims.claim` takes it under one lock and saves it on the overworld (`hexkagune_places`, loaded on
    `LevelEvent.Load`, or from `ServerLifecycleHooks.getCurrentServer()` if ground is made first). With nowhere to keep a claim it refuses,
    never risking a second one. Every other spot is refused from then on.
  - **Releasing:** a taken spot whose chunk loads in full without the start is given up (`ChunkEvent.Load`), and the next one made takes it.
  - `/kagune places` lists the taken spots, looks in each, and gives up any that was stopped.
  - The 4.1.6 to 4.1.13 world-wide choice (`hideout()`, `ccg()`, `chosen()`, FORCED) is gone; `DryLand.pick`, `choose` and `offers` remain
    (tested, unused by the game).
  - **Risk:** if some mod made `making` always false, every spot would be built (none would ever be taken). If the owner reports several of
    one place, look there first.
- **Coats** (`OutfitLayer.draw`):
  - The tails (`RIGHT_LEG_COAT`, `LEFT_LEG_COAT`) are emitted on the leg part with its bend, exactly as the trousers.
  - `CoatDrape` and the per-wearer drape are deleted.
  - `ArtPreview`'s `coats` sheet draws them the same way.
- **No M1 spam** (`ComboStyle.chainOpen`, used by `KaguneCombat.tryBegin`):
  - An Ukaku wing slam's next stage opens at `max(duration, chainOpenOffset)`, never mid-move. Before, it opened about 60% in, right after
    the hit, and snapped the wing.
  - Wing slam `damageMul` is 1.3x 4.1.13's, so `ControlTests`' pace window (0.7 to 1.5 of the original swipe) holds.
  - `UkakuTests` checks every stage at every duration.

## 4.1.15, what and where

The owner (quoted exactly): "Now add more interactions, like if you're married to touka ayato will respond, and more this is just an example,
improve the making love scene, add more kid interactions, add more kiss types and better the animation". Then, mid-work: "I'm going to tell
you right now, female, like daughters, can dance, but there's no way a son, like starting from being a teen, there's no way he would dance
with his fucking dad or mom. It's embarrassing. They don't do that. No one does that. Like, stop the touching with your fucking son. Like,
they don't like to be touched. I don't like to be touched like"

- **Reactions** (`SceneServer.relations`, `SceneWords.RELATED`, `tieName`, `relationFlags`):
  - Every companion scene's context gets `with_<tie>`, `wed_<tie>`, `kidswith_<tie>` and `taken`, and the name `{kidwith}`.
  - Ayato's tie is spelled `blackrabbit` (`SceneWords.tieName`): `SceneTests.FORBIDDEN` bans his name in the eto, touka, ken, child and ally
    scripts, flags and comments included.
  - New scenes at the end of `ayato.txt`, `touka.txt`, `ken.txt`, `eto.txt`, `general.txt`. `SceneTests.relations` checks each of
    them has its scenes.
- **The night** (`npc/NightWords`, pure):
  - Captions by persona, weather, mood, first night or a long marriage, kids or pregnant.
  - Packed into S2CCutscene NIGHT's one string (`pack`: `\u0001` after each caption, `\u0002` before the morning words). An old string
    (no `\u0002`) is all morning words.
  - `CutsceneOverlay.night` builds the timeline from the captions (rain and thunder cues when a caption says so) and wraps text.
  - `EtoServer.night` stores `NpcMemory.NIGHT_AT` (the morning-after kiss) and sends a waking gesture (`NightWords.waking`).
  - Fade to black, never explicit: keep it so.
- **Kid games** (`ChildMoment` NUZZLE, BOOP, FIST_BUMP, PATTY_CAKE, DANCE appended; `ChildMomentPose`; `ChildLines`; `ParentCare`):
  - `ChildPage.play(stage, girl, carrying, ride, page)` splits onto two pages when the games don't fit (`more`, `play`).
  - Ten new scenes in `child.txt`. `tools/MomentPreview` draws them (`docs/handoff/kid-games-4.1.15.png`).
- **Sons from their teens on** (`ChildMoment.touchy`, `touched`, `fits(stage, girl)`):
  - No HUG, PAT, NUZZLE, BOOP, TICKLE or DANCE. Gated in `ChildBrain.start` (every moment goes through it), its own moments and
    `welcome` (a WAVE), `ParentCare`, the play page, the moment key, and scene effects (a touchy one becomes a FIST_BUMP).
  - No hand to hold (`ChildPage.parent(..., girl)` and the `hand` key and effect) and no `stroke` while asleep.
  - TUCK stays (bedtime: the covers smoothed).
  - The teen replies that said "hugs you" or "leans on you" were rewritten. `teen_dance_dare` needs `girl`.
  - `SceneTests` and `FamilyTests` check all of it.
- **Kisses** (`MoodKiss` SHY, MORNING, RAIN, MOVIE, NOSE, GALLANT appended; `MoodKiss.of(mood, Moment)`; `EtoBrain.kissMoment`):
  - A mood's kiss first. Otherwise:
    - the morning after: `MORNING_AFTER` ticks after `NIGHT_AT`;
    - rain falling on her: `Level.m_46758_` above her head, newly allowlisted;
    - the first `SHY_UNTIL` kisses, unwed;
    - else a roll of 100: under 14 MOVIE, under 28 NOSE, under 40 GALLANT.
  - Poses per persona in `MoodKissPose` (all pass `clear`). Ken's MOVIE is a dip.
  - `apply` now blends each phase from the one before over `BLEND` ticks, instead of easing in from upright.
  - `CompanionPreview <dir> new-kisses` draws them (`docs/handoff/kisses-*-4.1.15.png`).
- **Every gesture crossfaded** (`client/animation/PoseFade`, kept per companion in `EtoRenderer.FADES`): a new `gestureAt` blends from the
  last drawn pose over `TICKS`. `CompanionTests.poseFade` checks it.
- Protocol 117.

## 4.1.16, what and where

The owner (quoted exactly): "Confirm that all females when kissing have a blush, sort of. Next task is to adapt all eyes to make sure that
they all look like eyes. This also applies for kids and humans. I mean, ghouls, wild ghouls, your kids, CC. No, sorry, not CCG. Some eyes are
clearly a little fucked on the skins." Also "Ayatollahs are fucked if I'm not" (voice to text; read as Ayato's eyes) and "So every time I
blush, it depends on their mood." (read as: how much they blush goes by their mood).

- **The blush** (`client/animation/KissBlush`, pure, per companion in `EtoRenderer.BLUSHES` and per human in `HumanRenderer.BLUSHING`):
  - A woman blushes through KISS, BLUSH, PULL, PECK, STEAL and KISSED, then `LINGER` ticks after, fading over `FADE`.
  - A man blushes where `Persona.blushes` says.
  - How deep: `MoodKiss.blush(style)`, the style kept from the kiss's phases (the server resets it to SWEET at KISSED).
  - `CompanionTests.kissBlush` checks it.
- **The eyes:**
  - `tools/companion_textures.py`:
    - `REPAINT` repaints face pixels in the game's copy, never `art/` (Ayato: white outer column, blue iris inner).
    - Overlays are cleared where the base hat pixel is opaque.
    - Re-run it after any change; it reproduces every other file exactly.
  - `tools/child_skins.py` and `tools/blink_skins.py`: the kakugan and the shut eye are now painted on the face layer always, plus the hat
    where it has paint. Before, hat-only.
  - **Pipeline order (child_skins.py wipes the folder):** run `child_skins.py`, then restore the other tools' outputs (`*_blush`, `*_vN`) or
    re-run their tools, then `blink_skins.py`.
  - Previews: `docs/handoff/eyes-4.1.16.png`.
- **Not done, the owner's call:** daughters 07, 10, 11, 12, 21 and 22 have their eyes painted shut or hidden in their skins (`blink_skins.NONE`);
  open eyes would have to be painted in.
- Wild ghouls: `NpcLook.EYES` and `GIRL_EYES` match all 24 skins (checked on a sheet).

## 4.1.17, what and where

- **The coat gap** (`tools/outfit_models.py`: `overlap`, called in `fit_outfit`; rebake with `python3 tools/outfit_models.py`; it reproduces the bin
  exactly):
  - Each half's front and back panels (and hems) reach `OVERLAP_FRONT` / `OVERLAP_BACK` past the middle; the right one sits `OVER` outside.
  - The edge trims and the back seam moved with them.
  - `hip_lining`: on the body, leggings slot, behind the fronts.
  - A centre panel on its own bone (the legs' average) was tried and dropped: the forward leg swings through it.
  - Preview: `ArtPreview <dir> coat-walk` (and `coats`).
- **Places on the ground:**
  - `cyber_tower.json` now projects to `WORLD_SURFACE_WG`, absolute -1 (its anchor is the template's bottom layer, 36,0,36).
  - `DryPlace.here` returns `raised(stub, DryLand.lift(...))`. The lift samples the footprint every `LIFT_STEP` and at its edges, then
    rebuilds and shifts the pieces each time they are asked for (`StructurePiecesBuilder.m_192781_`, `m_192780_`, newly allowlisted).
  - The sites (hideout, CCG, villages) already sit on their 5x5 ground with a steepness limit; they were not changed.
- **Duplicates** (`PlaceClaims.chunk`):
  - A loaded chunk holding a one-of-a-kind start (`ONE_OF_A_KIND`) claims it if nothing is claimed yet; a second one is logged.
  - `ControlStructures.making` is `!(heights instanceof Level)`.
  - The owner saw two or three Anteikus and two towers: most likely a world carried across 4.1.9 to 4.1.14 (each with its own rules).
    If they report one in a fresh 4.1.17 world, ask for the `HexKagune:` log lines.
- **Ukaku planted spikes:** `UkakuSpikes.Planted.shooter`, set from the bolt at SPIKE_IMPACT; `UkakuWingRenderer` tints it;
  `UkakuFx.crumble(shooter, ...)`. `UkakuTests.plantedColour` checks it.

## 4.1.18, what and where (the quinque overhaul, part 1)

- **The katana model** (`tools/quinque_models.py`):
  - `katana_parts`: steel blade segments with per-face patches (`box(..., sides=...)`, "~" mirrors a face's patch).
  - Its texture cells `KATA_FLAT`, `KATA_FLAT2`, `KATA_SPINE`, `KATA_TSUKA`, `KATA_TSUBA` are painted only for the katana; the other quinques'
    files are unchanged.
  - Look at any model textured: `python3 tools/item_render.py <model.json> <texture.png> <out.png>`. `QuinquePreview` paints an element one
    flat colour (fine for poses).
- **Strings:**
  - `Quinque.steps()` (the katana 5, the others 3), `finisher()`, `swing(step)` (the katana's `KATANA_SWINGS`).
  - `Quinque.next(q, last, since)` is used by `QuinqueServer`, `AgentBrain` and `QuinqueOwn.Combo.observe(q, ...)` (`QuinqueClient` passes the
    held weapon).
  - The old `next(int, long)` and `FINISHER` remain for nothing but legacy.
- **Swings by intent** (`client/animation/QuinqueAim`):
  - A key is (t, blade direction, right fist place: right/up/forward pixels from the neck, body channels).
  - `solve` searches pitch/yaw/roll on the held model's kinematics (`QuinqueGrip.held/fist/reach`), with weights: `BLADE`, `HAND`, `LEFT`
    (left hand's reach), `KEEP` (continuity, 4.0: lower it and the wrist flips between keys).
  - `QuinquePose.Key` gained `spin` (the whole body turned; the finisher ends at 2 pi).
  - `QuinquePose.aims(q, step)` gives the intents; the katana's are in `katanaAims`.
  - **Bake after any aim change:** `javac -cp build/quick/classes:build/quick/base.jar -d <dir> tools/QuinqueAimBake.java`, then
    `java -cp <dir>:build/quick/classes:build/quick/base.jar QuinqueAimBake`. `QuinqueTests.aimed` fails if `QuinqueKeys.java` is stale,
    checks each aimed key's blade within 30 degrees, and that the arm never jumps more than 0.9 rad in 0.02 of a swing.
  - Solving at runtime took a third of a second a swing: never do that.
- **The draw:** `OUT_STEPS` (7) frames `<kind>_out_1..6`, then `full`. The item overrides follow, and the frame tests count 8 (10 for a case).
- **Next, in this order:**
  1. **First person: done in 4.1.19 (its camera replaced: see "4.1.19, what and where"). As the patch had it:** `ArmItemLayer.FirstPerson.hand` draws a non-ranged quinque with a hold
     through `throughBody`:
     - the camera's frame to the model's (scale -0.9375, -0.9375, 0.9375), back from `FIRST_PERSON_EYE` {3.5, -0.5, 12}, which is a little
       behind and to the left of the head;
     - the right arm's pivot and Rz Ry Rx from the posed `RigPose` (spin dropped, since the camera doesn't spin);
     - ItemInHandLayer's Rx(-90) Ry(180) and offset, then the item with `THIRD_PERSON_RIGHT_HAND`.

     Two things were tried and dropped:
     - The true eye: a katana in chudan points along the view, its hilt filling it.
     - A turn-from-stance delta at the old place: the cuts went into the screen, not across it.

     Check it off-game: `tools/first_person_view.py` and `tools/FirstPersonProbe.java`. Still owed:
     - the arms in first person;
     - the blade and the launcher, which keep the legacy motion.
  2. The blade's, spear's, mace's, launcher's and Dawn's own strings by intent (`aims`), each its own count.
  3. New kagune-born quinques (the owner: whips after different kagunes, more swords).

## 4.1.47, what and where (the ghouls' arm blade rebuilt, its moveset and stance)

The owner, quoted: "Improve severely the kagune arm blade of the ghouls, it looks way too small, not sharp and horribly made compared to
literally every other kagune"; my first rebuild (a beveled blade with thorns) they called "that looks horrible" and sent a picture ("it
should look like this": broad smooth bands wound round the arm with red veins, one huge smooth single-edged curved blade, a bright edge);
"blade pointing downwards mate" was asked back with both readings rendered: they chose "B: blade turned over" (the edge and point face
down with the arm out; not the guard aimed at the ground); then "also give it a proper moveset, slash attacks and a proper ambient
stand"; "that will do, make sure that kagunes use random colors on this one too" (it was already `KaguneTint.wild(seed)`: checked across
the palette, `BladeArmColours`).
- `npc/BladeArmModel`: `ribbon` (two bands, `ribbonCut` on the arm's cylinder, flaring at the wrist, `crack` veins), `blade` (wedge
  cross-section of five faces, `edgeAt` convex sweep to a point on the spine's line, `spineAt` curving back then in, gleam GLOW strips
  down both bevels, veins at the root), drawn through `turned` (z mirrored: the edge on the back of the arm). `veins`: crimson, a dark
  shade on a red kagune. `wedge`/`box` are left unused.
- `npc/BladeArm`: four cuts (`SWEEP` 2 cuts all before it, `FINISHER` 3 the cleave), ticks 10/10/12/17, lands 5/5/6/11, reach 3.2.
- `client/animation/BladeArmPose`: guard up and out (`GUARD_PITCH` -1.42, `GUARD_YAW` 0.42); keys: chop (edge forward with the arm
  up), backhand (roll 2.9-3.05: edge up), sweep (roll +1.5: edge leading left; -1.5 had the spine leading, measured), cleave (high,
  held, driven down to -1.78 so it stays out of the ground); `stand(time, seed, still)`: breathing, drift, a wrist turn every 160 ticks,
  feet set (`legHold`) only while still. `client/GhoulNpcClient.blade` uses it.
- `server/GhoulNpcCombat.bladeLand`/`bladeCut`: every cut a SLASH wound, the cleave knocks back 0.9, the sweep cuts each `fair` body in
  front within reach.
- Checked off-game: `tools/BladeArmMoves` prints the blade's lowest point through every frame (chop 0.08, backhand 0.27, sweep 0.49,
  cleave 0.24, stance 0.20 blocks); `NpcTests.bladeLow` computes the same from `RigPose.transform` (it reads -0.88 for an arm hanging
  straight down) and fails any frame below -0.05. Edge-first checked by the spine-to-edge direction against the blade's motion.
- Not done: its own sounds (it uses `blade_arc`/`blade_heavy`); the girls' slim arm uses the same blade (narrower ribbons).

## 4.1.46, what and where (the CCG's ghoul division)

- They are ghoul NPCs (`GhoulNpc`, its kagune, its fights, `GhoulNpcCombat`) of `Gang.CCG_DIVISION` (identity bits 44-45 = 3, mask 0):
  `NpcIdentity.ccg()`, `kakugan()` false (`GhoulNpcRenderer.Eyes` returns for them: every client). Chosen over a `CcgAgent` with a kagune:
  the NPC kagune (graft tails, wings, arm blade) and their fighting exist only on `GhoulNpc`.
- The CCG's side: `AgentBrain.ccg(Entity)` (an agent or a division member, read off the saved identity, server only) replaces every
  `instanceof CcgAgent` that spared the CCG: `wanted`, the partner's prey, `defend`, the Reaper's blade sweep, `hurt` (no grudge),
  `attacked` (a blow from either side on the other cancelled), `AgentArms.foes`, `EtoBrain` (a human persona's foe, a home's threat).
- Their brain: `GhoulNpcBrain.wanted` hands them to `division` first (the owner chose "Like CCG agents": a wild ghoul showing within
  `AgentRules.GHOUL_RANGE`, a player `AgentRules.revealed` by `AgentBrain.shows` (marks the three for `MARKED`, the CCG's words) or
  `AgentBrain.marked` by an investigator within 48, a mob after one of them, an `Enemy` too close; never the CCG, humans, Eto, children,
  thralls). `hurt`: the gang block (no grudge on a mate or, for them, the CCG) and `AgentBrain.called` (investigators within 24);
  `AgentBrain.hurt` calls `GhoulNpcBrain.called` (the division within 24). Wild ghouls' `wanted` and `aogiriFoe` take them for the CCG.
  `roam` keeps the three together (as the gangs). Not `wild` (no prowling eyes, no hiding), no `dawn` slip.
- `dress`: FORMAL_SET (the investigators' armour, `OutfitServer.armour` takes its protection off the rank's). `drop`: `HumanBrain.drop`.
  `GhoulNpcTalk.open`: one of `CCG_WORDS`, no talk, no bond.
- `AgentSpawner.division`: `DIVISION_CHANCE` 0.35 of the CCG's spawns, three round a spot `clear` finds (which counts them as the CCG),
  `PAIR_APART` apart, of one squad (`GANG_ID`), all three or none. `GhoulNpcSpawner.clear` doesn't count them.
- Not done: their own voices; CCG-style rank titles (they keep the ghoul letter ranks); joining (4.1.47).

## 4.1.45, what and where (roaming gangs: the Clowns and the Gas Masks)

- `npc/Gang.java` (pure): NONE, CLOWNS, GAS_MASKS (by ordinal, in the identity); each five masks (the owner's `mask_clown` / `mask_gas`
  and four repaints, `_v2` .. `_v5`); `size` 3..5, `roaming` even odds, `faces` a shuffle so every member's mask differs.
- `npc/NpcIdentity.java`: a ninth field `gang` (bits 44-45, read only under the rank flag: older saves are of no gang); `ganged(gang,
  mask)` makes a DRIFTER of no faction; `maskItem` the gang's mask; `tagName` "(Clowns)".
- `outfit/Outfits.geometryOf`, `OutfitModels.get`: a variant ("mask_clown_v3") is drawn with its base's model in its own texture
  (`textures/outfit/mask_clown_v3.png`). `tools/gang_masks.py` made the paints (texels sorted into materials by colour, each given the
  variant's colour at its own light). Gas v4 was first white with a red filter and read as a hockey mask: repainted slate with red lenses.
- `client/render/OutfitLayer.faction`: a gang member's mask drawn on its face from its identity (no item: nothing to drop, nothing else on).
- `server/GhoulNpcSpawner.gang`: with `GANG_CHANCE` (0.3) of a night's spawn, a gang round one spot judged as one ghoul's (`clear`: the
  crowd rule then keeps others away while it is there), its number in each one's saved data (`GhoulNpcBrain.GANG_ID`).
- `server/GhoulNpcBrain`: `mates` (same gang and number); `roam` (in `idle`, after Aogiri's guard: the least UUID within 48 leads and
  wanders; followers go to a spot 2.5 blocks off it at their own angle and do nothing else); `wanted` (a gang goes only for a mob going for
  a mate: no prey, no player); `fair` (its blows only on what it fights); `hurt` (whoever hurt one has a 2400-tick grudge from every mate
  within 24, not a mate hitting a mate); not `wild` (no prowling, no hiding); `dress` puts nothing on them.
- Not done: their own sounds or lines; joining them (4.1.47).

## 4.1.40, what and where (older worlds' townspeople; "complete all" and later quests)

The owner's words as 4.1.39's.

- **Old towns' people:** `VillagePlace.Place.site()` looks maps up by number (`SiteMaps.byMap`); 4.1.36 removed maps 3 and 4, so the people
  of towns built before it fell back to `VillageMap`'s streets/route/perimeter (the old jigsaw village's). `village/RetiredSites.ALL` holds
  the two `Site` records exactly as 4.1.35 had them (copied from its SiteMaps), searched by `byMap` after `SiteMaps.ALL`; never registered
  or built. Kept by hand: `tools/exploration_structures.py` writes `SiteMaps.byMap` with the fallback.
- **Finished quests and later gates:** `QuestLog.state` returns DONE for every PATH/GHOUL quest once `finished`, a later version's too, but
  `grantAll` opened only the gates of its time. `QuestServer.loggedIn` now opens every `Gate` for a finished log.
- **Checked, fine:** `WeaponClient.quinqueFirst` (blocks still dug; entities and air only), `grantAll` (every path's gates).
- **Tests:** `QuestTests.forGoodLater`; `NpcTests.cityFixes` (maps 3, 4 found, not in `ALL`); `NpcTests.smallCity`'s 4.1.36 check updated.
- **Not tested in game.**

## 4.1.39, what and where (Small City fixes)

The owner (quoted exactly): "And after that, just look for bugs on your own, like search for any possible bugs. I hear there's a bunch. That
could be related to the city. I don't know what it is, but the other AI was saying that it could have a bunch of bugs. But it ended before,
like, the usage ended before you could push a handoff or a prompt."

- **Found and fixed:** humans brought by vanilla villagers had no home (a homeless human random-walks 3-9 blocks every 6-14 s, persistent):
  they drifted away for good and newborn villagers brought more. `VillageHumans.bring` saves `HexVillageHome` (+ X, Z); `HumanBrain`'s
  homeless wander heads back past `HOME_RANGE` 24. `City.ground` (~740 terrain heights) was run by each quarter and every `inCity` caller:
  now `GROUNDS` (identity of the generator and its RandomState, the seed, the corner; cleared past 1024). Protocol 118 (4.1.36 changed the
  registries without a bump).
- **Checked, fine:** neighbouring cities never overlap (37+ chunks between starts, 34 wide); each quarter within 8 chunks of its start;
  `#hexkagune:overworld` covers `#minecraft:is_overworld`; no code points at the deleted villages; beard_box carves over the streets and
  fills under the edges as described.
- **Not checked (needs the game):** city generation in a real world (only the tools' renders exist), `/locate` time in hilly worlds (a city
  needs land flat within 12 over 544 x 544; in mountains the search may run long), the city's 24 humans and 2 agents per quarter all
  awake near a player (performance).
- **Tests:** `NpcTests.cityFixes`.
- **Not tested in game.**

## 4.1.38, what and where (two minutes after a fight, the jacket back on)

The owner (quoted exactly): "By the way, after the fight ends, I should note, let's say 2 minutes without fighting, Arima puts back his jacket
in his normal". Asked how the coat and blade come back (both sank in his change), they chose "Reverse it in place".

- **Server:** `AgentBrain.dress` runs after `shed` in `act`: bare, rank 10, not ending, `prey` 0, `foughtAt` and `hurtAt` both
  `REAPER_REDRESS_CALM` (2400) ago, on the ground, no blow/rush/dodge: `dressAt`, swing `CcgAgent.DRESS` (13), `pin` each tick
  (`untouchable` includes `dressAt`); at `REAPER_DRESS_SWAP` (3 before the end): `bare` false, `BARE` removed, `REAPER_LOOK`, speed, healed
  to max, so the next fight sheds again at 90. `arm` keeps his hand empty while he dresses (Dawn back after).
- **Client:** `client/animation/ReaperDressPose`: 0-12 the left hand behind his back for the case (`LEFT_KEYS`, the draw's right mirrored;
  the case in it from `CASE_IN` 5, hanging plumb) while the right raises the blade (`presented(lower)` 1 to 0 by `RAISED` 14); 12-19 the
  case carried to his right fist (model-frame blend of the left fist's plumb frame and the draw's `caseTurn` along frame, opened on the way:
  `held` returns it in the right arm's frame, continuous at `BRING`); 19-26 the blade slides in (`bladeOut`); 26-30 the case shuts, the left
  hand off; 30-44 `ReaperBladePose.draw` backwards from `LIFTED` to 0 (`drawTick`). The coat: `LEAD` 5 ticks of arms sweeping back, then
  `coatAge` = his shed backwards from `ReaperCoatPose.OFF` to 0 (`AgentPose.shed`, `ReaperCoatPose.frame`). `AgentClient` dispatches DRESS;
  `ReaperBladeLayer.stowed`; `ReaperCoatLayer` draws the coat at `coatAge`.
- **Tests:** `AgentTests.dressing` (calm, codes, starts at the guard, the case behind his back, set at the fist with no jump, the blade in and
  the case away before the coat, the coat whole before the swap, rest at the end, server and client wiring).
- **Preview:** `docs/handoff/arima-dress-4.1.38.png` (`tools/ReaperBladePreview.java dress`).
- **Not tested in game.** The coat appears behind his back as his hands reach for it (from nowhere, mostly hidden by him), as chosen.

## 4.1.37, what and where (the Reaper's change: untouchable, a real coat, a slower draw, point-down, clashable)

The owner (quoted exactly): "whenever he's transforming, you can still grab him, which should not be allowed. And I don't know, I feel like
transformation needs to be a little more dramatic. I mean, like when he takes off his jacket, I feel like he should actually like take off an
actual jacket, before switching to the other skin. The taking out the quinque from the case also should take a little longer. It's pretty too
fast." Then: "[Arima] is actually holding the blade upside down, bro. The blade is facing up instead of down ... And he's still very
hittable. Like, I told you to make him completely immortal while he's transforming." Then: "You should be holding that upper bone thing on the
blade always. Mind you that, like, his moves are currently not clashable ... You gotta patch that ... You should take off an actual jacket.
Like, you can use the armor that we got ... just make sure he takes it off, bro. And you can see him taking it off." And: "The briefcase
should come after the jacket comes off". Of the case turning end-on in the draw: "Just saw the sheet. Now I understand. It's fine."

- **Untouchable:** `AgentBrain.untouchable(entity)` (rank 10, `shedAt` or `endAt` set, not `finishing`). `attacked` (LivingAttackEvent)
  cancels for it unless `GhoulTank.bypasses` (the old LivingHurtEvent cancel only stopped damage: knockback, flash, sound and every direct
  push/bleed/grab still landed). `EtoBrain.spared` asks it first, and the patch already routes the original's `validSwipeTarget` and
  `isHostileTo` through `spared`, so every grab/tail/swipe using them refuses him; `PlayerArms.mayHit`/`aim`/the Bind catch too. `pin` (each
  tick in `shed` and `ending`): `free` (releases the original's `heldEntityIds` slots via `releaseHeldSlot`, Bikaku `heldId`/worms,
  Pale `heldId`, Graft `heldId`, `PlayerArms.unbind`), velocity zeroed (falling kept), teleported back to `pinnedAt` if moved.
  `REAPER_SHED_WAIT` (60): the change no longer waits for ever to land (in water he was unkillable at 90).
- **The coat:** `client/animation/ReaperCoatPose` (pure): pieces BODY, sleeves, tails = the formal model's bones 1, 2/3 (6/7 slim before the
  swap), 8/9, slots 1, 1, 2; worn = `RigPose.transform` of the part (as AgentModel), `PEEL` 14 (tipped back about its hem, back and down),
  `SLEEVES` 17 (13 px down the arm), `OFF` 26.5, falls to `lying` (flat on its back behind him, `FLAT` 0.3, sleeves splayed) by `LANDS`,
  `sunk` over the last `SINKS` 12 ticks of the change. `client/render/ReaperCoatLayer` draws it with `textures/entity/ccg/reaper_coat.png`
  (armorCutoutNoCull), clamped above the ground, kept where it fell (`FELL`: his body yaw at `OFF`, turned back by the difference).
  `REAPER_SHED_TICKS` 46 (keys retimed), `REAPER_SHED_SWAP` 3 (hidden under the coat).
- **The draw:** `REAPER_DRAW_TICKS` 62, `CASE_FROM` 11, `CASE_OPENS` 32, `CASE_DROPS` 48; `LIFT_FROM` 22, `LIFTED` 30, `GRIP` 40-46,
  `SPLITS` 6, `SLIDES` 10; `presented(lower)` from `PRESENT` (the 4.1.29 raised guard, both hands) to `GUARD` over `LOWER_FROM` 50 ..
  `LOWERED` 58.
- **The guard:** one-handed, point down along his right side (`HANG_X` -0.3, `HANG_Z` 0.12, `GUARD` pitch -0.3, yaw -0.18): `pose(k, legs,
  both)` blends `oneHand` and `hands` keeping the blade on the key (the wrist solved again for the blended arm); `strike` uses
  `bothHands(total, age)` (`TAKES` 2); `end` takes both hands as he lowers it. His fist is at u 0: on the bone grip (grip 0 .. -9, guard from
  1.9).
- **Clash:** `startBlade` and `beginStrike` put each blow on `KaguneClash.swing` (impact, reach + 0.5, aimed at the prey, `ClashPower.agent`
  of its damage), live while that blow is on its way; `bladeLands`/`brawlLands` call `meets` first; `bladeMet`/`brawlMet` break the string.
  The owner wrote "for some reason, you can clash with this form": read as "can't" (they weren't on the board). Confirm with the owner.
- **Tests:** `AgentTests.reaperBlade` (guard point down below the fist, rod clear of him, left hand loose; hands on the grip whenever
  `bothHands` is 1), `coat` (pieces in the model, worn on him at the swap, behind him when free, flat behind him through the draw, sunk at the
  end, layer wired), `guarded` (the immunity, pin, free, wait, clash wiring).
- **Previews:** `tools/ReaperBladePreview.java` (`guard`, `draw`, `string`, `shed` = the whole change, `case` = every tick of the case):
  `docs/handoff/arima-change-4.1.37.png`, `arima-guard-4.1.37.png`, `arima-string-4.1.37.png`.
- **Not tested in game.**

## 4.1.36, what and where (the Small City in worlds, the villages gone, humans in vanilla villages)

The owner's words as 4.1.35's.

- **The limit and the way round it:** a structure places only within 8 chunks of its start chunk, so the 544 x 544 city is four starts of
  the one structure `hexkagune:small_city`, 17 chunks apart. **`ControlStructures.CityPlacement`** (`hexkagune:city`, registered on
  `Registries.f_256888_`; codec `MapCodec.unit`) extends the vanilla random spread (48 spacing, 36 separation, `DryLand.CITY_SALT`): its
  offers (`m_227008_`, untouched, so `/locate` finds the north-west quarter) plus the three chunks beside each (`m_214090_` overridden:
  `CityPlan.slot`). **`City`** at a start chunk: its slot and the offer (`CityPlan.slot` over `cityOffers`, which is null unless the set's
  placement really is `CityPlacement`: then no city at all), the turn (`CityPlan.turn(seed, offer)`), the quarter in that slot
  (`CityPlan.quarterIn`), the whole city's ground (`City.ground`: 9 x 9 samples over 544 x 544, each the top of the ground or of the water
  over it, no water deeper than `CityPlan.DEEP` 8; `CityPlan.ground`: spread <= 12, their median, at least `ABOVE_SEA` 64; no sea biome
  (`SEA`, `minecraft:is_ocean`, via `DryLand.Ground.sea`) every 16 blocks, no deep water every 32), then `Site.stub` of that quarter turned,
  at `CityPlan.slotCorner`. The owner, mid-4.1.36 (quoted exactly): "Also, that city should be able to go over rivers. It doesn't really
  matter. Just make sure it blends well with the terrain." So rivers, beaches and swamps are allowed and it sits on the median (beard_box
  cuts the land inside it over its streets and fills under its edges), not the highest.
- **Kept apart:** `inCity(context, x, z)` (a built city's chunks plus `CITY_MARGIN` 10, judged by `City.ground`) in the hideout's, the CCG
  building's and every jigsaw place's spot test; `City.holdsPlace` keeps a city off an older world's claimed place. (Keeping places from
  every city *offer* failed: a region of 48 always has one, so nothing was left.)
- **Deleted:** `worldgen/structure/village*.json`, `structure_set/villages.json`, `template_pool/village/`, `structures/village*/`,
  `tools/village_structure.py`; `village_1`/`village_2` out of `exploration_structures.py` and `SiteMaps` (maps 3 and 4 gone). Kept: the
  Java of the old villages' people (`BrickVillage`, `VillageMap`, `VillageTops`, `DryJigsaw`): old worlds' people still use them.
- **`VillageHumans`:** every 100 ticks, each `Villager` within 96 of a player not yet looked at (`HexVillageHumans` in its data): one in 2
  brings a human (persistent) at its feet, unless 4 are within 48.
- **Render:** `tools/city_render.py --placed T` (slots by a port of `CityPlan.slotOf`, asserted equal to the whole city turned):
  `docs/handoff/small-city-placed-4.1.36.png` (all four turns); `--placed T --land`: on rolling land with a river, the ground fitted roughly
  as beard_box fits it (`on_land`): `docs/handoff/small-city-on-land-4.1.36.png`.
- **Tests:** `NpcTests.smallCity` (slots a permutation every turn; every quarter block where the whole turned city puts it; reach; offers
  known as their own; neighbours never touch; turns every way; `covers`; source checks for `inCity`, `holdsPlace`, `VillageHumans`); the
  older DryLand tests moved off the villages' numbers.
- **Not tested in game.** Old worlds: a city at the edge of land explored before 4.1.36 can lack a quarter. `/place` builds no city.

## 4.1.35, what and where (the Small City, cut for 1.20.1)

The owner (quoted exactly): "ALl villages get replaced with the city in my minecraft world that I sent named "Small-City". Delete all ids
hexkagune:village_x ( and anything related) add Hexkagune:small_city. This big structure is relatively rare (a bit rarer than a normal
village), Humans can now however spawn in normal minecraft villages." They chose "Whole city".

- **`tools/city_structure.py`:** reads `art/city/small-city.zip`'s region files (chunks -17..16 both ways), finds the city's ground (y 64),
  writes `build/city/small_city_<k>.schem` (Sponge v3, layers y 58 .. 195; a stamp skips the work while the zip is unchanged; ~20 s).
- **`tools/exploration_structures.py`:** four more sites (`small_city_0..3`: 0 NW, 1 NE, 2 SW, 3 SE; maps 7-10; `"ground": 6`;
  `"city": True`), each 272 x 138 x 272 in 5 x 5 tiles (~1.8 MB each), 12 homes (24 humans) and 2 investigators. New `SWAPS`:
  `pale_oak_wall_sign`, `tuff_bricks`, `wildflowers` (pink petals), `infested_stone_bricks`, `infested_stone` (the last also changed one
  block of `village_1`'s `t_1_1`). Banners' `patterns` written as 1.20.1's `Patterns` (`BANNER_CODES`, `DYES`); `NO_ENTITY` (shelves) left
  out of the city only, so the older places stay byte-identical. Run: `python3 tools/exploration_structures.py --blocks <1.20 blocks.json>`
  (~1 min; PrismarineJS minecraft-data `data/pc/1.20/blocks.json`).
- **`tools/city_render.py`:** the city from its tiles, isometric from the south-east (numpy, a depth buffer; `--box x0 z0 x1 z1`,
  `--scale N`, `--turn`, `--people`). Renders: `docs/handoff/small-city-4.1.35.png`, `small-city-middle-4.1.35.png` (the quarters' seam:
  none visible). Use it for 4.1.36's placed city too.
- **`library_structure.py`:** writes byte, int and long arrays.
- **Tests:** `NpcTests.smallCity` (four quarters, one ground and height, maps 7-10, homes and patrols, every tile on disk);
  `ccgBuilding` now expects 8 sites.
- **Not placed in worlds:** the sites' structure types are registered (`ControlStructures` loops over `SiteMaps.ALL`) but no structure or
  structure set uses them yet: that is 4.1.36.
- **Not tested in game.**

## 4.1.34, what and where (the quest menu, a cute box)

The owner (quoted exactly): "We also need to better the looks of the quest menu. It looks very bad. Very, very. I don't know. I just don't
even feel like opening it half the time. I don't feel like opening it. It's just really dull, really bad looking, not interactive at all.
It's just bad, bro. We need to better that." Then: "Fair warning is also the background and all, they're also super ugly." And: "Where does
the menu have to fill the entire screen? Why not just like a cute like thing that still allows you to see the background? Like why not just
a cute box? Why does everything have to be a full screen thing? Like I don't understand".

- **`QuestJournalView`:** its own 640 x 380 space (`UI_WIDTH`, `UI_HEIGHT`; `LIST_X/WIDTH`, `DETAIL_X/WIDTH`, `CONTENT_Y`), `QuestScreen`
  scales it to fit (at most 1) and centres it with nothing drawn behind (no `renderBackground`, no gradient). `header` (title, path and
  mastery, the bar to the next PATH quest by mastery, points/side chips on the tabs' row), `tabs`, `list` (cards: glyph tile, chip, %, bar;
  hover shifts, nearly done glows, slide in on open), `detail` (title x1.25, quote, checklist with segmented bars, reward badges via
  `rewards`, rival/side chips, a stamp), `roadmap` (PATH tab: the path's quests by mastery as nodes, rivals as a fork, hover tooltip,
  `quest:ID` hits). `romance` re-fitted to the columns. The old `gates` text dump is gone.
- **`QuestStyle`:** colours, `line`/`circle`/`ring`/`box`/`chip`/`check`/`lock`/`cross`, `glyph` per line and state, `rewardGlyph`,
  `backdrop` (shadow, cut-corner plate, header glow, honeycomb, corner brackets), `honeycomb`, `hexagon`, `polygonDiamond`.
- **Preview:** `tools/UiPreview.java quests` (over a stand-in sky and grass; `-Dmx= -Dmy=` put the mouse somewhere to see hovers).
- **Tests:** `QuestTests.journal` (a compact box; every tab, card and road node clickable and inside it; the road holds the path's quests;
  no organic board, no full-screen overlay).
- **Not tested in game.**

## 4.1.33, what and where (quests finished once, for good)

The owner (quoted exactly): "Quests should only be done once. You can spam /kagune quest complete all over and like, I mean, it's not that
you can spam it. It's just once you did all of them /quest complete, all of them should be finished. Even if you pick another kagune, this
should still be finished because you finished all of them. You don't need to be typing it again."

- **Cause:** `QuestServer.completeOpen` finished only `State.OPEN` quests for the current path and mastery; other paths' quests are
  `LOCKED` (not `on(path)`), so a new kagune (or more mastery) opened them later.
- **Fix:** `QuestLog.finished` (saved `;all=1`, loaded, cleared by `reset`); `finishAll()` marks every `QuestBook.all()` quest of line
  `PATH` or `GHOUL` DONE (closed ones stay closed) and sets the flag; `state` returns DONE for any PATH/GHOUL quest once finished (so a
  quest a later version adds is done too). `completeOpen` calls it after paying the open ones; the command says so.
- **Kept:** hunts and favours (lines `HUNT`, `FAVOUR`) still come (told to the owner, ask if they want them off too).
- **Tests:** `QuestTests.forGood`.
- **Not tested in game.**

## 4.1.32, what and where (a quinque's M1 comes first)

The owner (quoted exactly): "Another thing that I want you to keep in mind is that whenever using a Quinque, I think that it should be
prioritized over any combat that we got in the game. So basically, if you got your Kakugan activated, but you got a Quinque activated, like
your M1s are gonna be ignored. You know what I mean? Like the M1s of the Kakugan are gonna be ignored, and be you're gonna be using the one
from the Quinques."

- **Cause (in the original jar):** `com.hex.kagune.client.KaguneKeys$ForgeBus.attackInput` (normal priority) cancels the game's attack while
  the kagune is out and sends `KaguneAction.SWIPE`, so a held quinque's vanilla hit never happened. `PaleInput`, `KingInput`, `BikakuInput`
  already return for `QuinqueClient.armed`; `WeaponClient.input` (HIGH) already took kagune-made weapons' clicks.
- **Fix:** `WeaponClient.quinqueFirst` (HIGH): a CCG quinque weapon in the main hand, an entity or the air: cancel (the original listener
  does not receive cancelled events), keep the swing, and `minecraft.f_91072_.m_105223_(player, target)` (vanilla's own `startAttack` for an
  entity); a block: untouched (dig). `KaguneCombat.startSwipe` returns first when `quinqueOut` (a quinque or kagune-made weapon in the main
  hand), covering the server's vanilla-attack and C2S swipe paths.
- **API:** stub `stubs/net/minecraft/client/multiplayer/MultiPlayerGameMode.java` (`m_105223_`), `Minecraft.f_91072_`; allowlist lines with
  joined.tsrg (`ffa.a (Lbyo;Lbfj;)V`, `enn.r`) and intermediary (`method_2918`, `field_1761`) evidence (maps fetched from raw.githubusercontent.com).
- **Tests:** `QuinqueTests.first` (source checks).
- **Not tested in game.**

## 4.1.31, what and where (Arima phase 2 part 4: the end)

The owner (quoted exactly): "After the fight is done, he breaks the weapon. The blade shatters at the end, and it's just broken. And he
accepts his death a little before he dies actually, and then he just falls on his knees and dies after the weapon shatters." Later: "Keep
like the shattering animation going."

- **Server:** `hurt`: bare and not ending, a blow that would leave him at `REAPER_END_HEALTH` (6) or less is clamped to leave 1
  (`endClamp`), stores `Mind.endSource`, `endAt`, swing `CcgAgent.END`; blows cancelled after (`finishing` lets his own last one through).
  `act` runs `ending` first: he stands still; at `REAPER_END_SHATTER` (9) the glass and heavy-blade sounds; at `REAPER_END_TICKS` (70)
  `m_6469_(endSource, MAX)`: dies by that blow, so `slain`/`reaperSlain` (Slain Reaper, mastery) and `drop` (Dawn) run as before.
- **Client:** `AgentClient` returns `ReaperBladePose.end(age)` for `END` (no upper bound: held as vanilla's death tips him over):
  `END_LOW` (the point to the ground ahead), the arms falling slack after the shatter (hilt in the right hand), `KNEEL` 0.55 rad back and
  `KNEEL_DROP` 6 px (thighs seen from the front, the rest of each leg under the ground: mobs have no knees), lean and head bowed.
  `ReaperBladeLayer.ended`: the whole blade, then `ReaperBladeMesh.broken()` (blade to `SNAPS`, jagged, capped) and `shards` (each
  `ReaperBladeRules.shard(i)`: where on the blade, its flight and spin; placed by `ReaperBladePose.shard(i, dt)` from the cached `Shatter`
  pose; falling at `SHARD_FALL`, lying flat on the ground).
- **Tests:** `AgentTests.reaperEnd` (the clamp, the timings, the lowered blade to the ground and never into it, the stub, the shards fly
  and land near, the kneel and its hold, the wiring).
- **Preview:** `docs/handoff/arima-end-4.1.31.png`.
- **Not tested in game.**

## 4.1.30, what and where (Arima phase 2 part 3: close combat)

The owner (quoted exactly): "He can dash more precisely. He's way more precise. His entire moveset changes because he no longer has the Dawn
weapon. So he's more into a melee close combat type ... He will always try to, get into range and kill you. He'll dodge moves."

- **Rush:** `AgentBrain.blade` starts one when his prey is out of reach (`REAPER_BLADE_REACH` + 0.5) within `ReaperBladeRules.RUSH_FROM`
  (12), in sight, the way clear (`clear`), every `RUSH_EVERY` (36) ticks; `rush` pushes him at `rushStep(left)` (never past the stop point,
  `RUSH_STOP` 2.4 short edge to edge, re-aimed every tick), `face`s the prey, and at the stop (or `RUSH_LONGEST` 10) `startBlade` at once.
  Drawn as the forward dash (`agent.dash(0)`, AgentPose.dash over his guard in `AgentClient.bladed`).
- **Dodge:** `AgentBrain.dodge` (each tick he is ready, not `committed`: a blow two ticks from landing): projectile entities near
  (`TendrilShield.incoming` with gravity/drag as the tendril shield), Ukaku's simulated shots (`UkakuFire.shots`, `UkakuCrystalCombat.shots`
  through `TendrilShieldServer.Shots`), else `KaguneClash.threat` at `DODGE_BLOW` (0.65); sidestep `ReaperBladeRules.dodge` (square to it,
  his side of its line; the other if not clear) at `DODGE_SPEED` for `DODGE_TICKS`, every `DODGE_EVERY`; a blow in its chamber dropped;
  ready to strike and rush again right after. Drawn as the side dash (`agent.dash(2|3)`).
- **Tests:** `AgentTests.reaperClose` (the rush never overshoots and ends in reach in time, the sidestep leaves the line every time, commit
  rule, wiring, none of Dawn's ranged moves in `blade`).
- **Preview:** `docs/handoff/arima-close-4.1.30.png`.
- **Not tested in game.**

## 4.1.29, what and where (Arima phase 2 part 2: the golden case and his blade)

The owner (quoted exactly): "this phase should trigger when he reaches about 90 HP, in which case he becomes immortal, takes the jacket off,
pulls out a random briefcase from his back like a golden looking briefcase, um and then he becomes, well, this with the weapon." Of the
first draw (the case shrinking as the blade grew): "Oh, come on, bro. That cannot be the fucking blade emerging fucking animation, bro. It
looks like shit. What the fuck? That's not even turning into the blade, bro. Why can't the blade just, like the briefcase just splits apart
open like a normal fucking briefcase and releases the weapon? Like, why? What the fuck? Why does it shrink, bro? What". And: "Do not make the
last quinque of Arima droppable. It's not needed. Just keep it only for himself. Like, there's no need to be making more move sets in first
person".

- **Trigger:** `AgentRules.REAPER_SHED_HEALTH` 90 (his max is 280); `shedClamp(health, amount)`. The server's `shed` runs
  `REAPER_CHANGE_TICKS` (jacket 34 + draw 36), `hurt` cancelled all through.
- **The draw** (`ReaperBladePose.draw(age after the jacket)`): hand behind his back (`DRAW_KEYS`), the case in it at `REAPER_CASE_FROM` (7),
  out to his side, lifted into his guard's right arm by `LIFTED` (17) while the case turns end on along the blade (`caseTurn`: hanging plumb,
  then the blade's frame, `blendTurn`); split open from `REAPER_CASE_OPENS` (18) (`caseOpen`, `ReaperBladeMesh.opened`: each half about the
  hinge, its bottom edge), the blade slid out (`extended`, `ReaperBladeMesh.slid`: the blade and rod drawn out from the guard and fist), his
  left hand onto the grip (22..26), dropped at `REAPER_CASE_DROPS` (25): it falls, tips over, lands and sinks (`dropped`, cached `Release`;
  `sunk`), gone by the draw's end (36). Nothing scales.
- **The blade** (`quinque/ReaperBladeMesh`, its own frame: u along from the right fist, v to its edge, w through): `blade` (veins: `walk`
  strips), `guard` (a leaf profile, gems, spines, ribs, horn), `grip` (ribbed grip, ring, rod, red tip, claw); `goldHalf(side)`,
  `goldHandle`. Drawn by `client/render/ReaperBladeLayer` (bare look only) in the right arm's frame: `ReaperBladePose.inArm` (the wrist's
  weapon channels times `HELD`).
- **The string** (`ReaperBladePose.STRING`, keys per blow: body twist/lean/lunge/sink, the hands' height and side, the blade's pitch/yaw/roll,
  legs, head; `hands` solves both straight arms onto the grip with a two-unknown Newton step, the right wrist turning the blade):
  cleave, rising cut, sweep, thrust; timing in `npc/ReaperBladeRules` (shared with the server). `AgentClient.bladed`: a blow whole, or his
  guard on his run.
- **Server:** `AgentBrain.fight` hands the bare Reaper to `blade` (closes at a run, chains the string while in reach, a rest after);
  `bladeLands` (arc `REAPER_BLADE_ARC`, reach `REAPER_BLADE_REACH`, `REAPER_BLADE_DAMAGE` x weight x his power, a throw, wounds, 2 stacks of
  `hexkagune:bleeding` via `UkakuCrystalCombat.agentBleed`). His hand stays empty in his second phase; Dawn's case still drops.
- **Tests:** `AgentTests.reaperBlade` (both fists on the grip every half tick, the blade clear of his body and head and never more than a
  pixel into the ground, the case's hinge and slide, the draw's times and its end in his guard, the wiring).
- **Previews:** `docs/handoff/arima-case-draw-4.1.29.png`, `arima-blade-string-4.1.29.png`, `arima-blade-4.1.29.png` (scratch
  `BladePosePreview`/`BladeMeshPreview` over `PoseRender`, `ReaperPreview.chest`, `QuinqueMeshPreview.add`).
- **Not tested in game.**

## 4.1.28, what and where (Arima phase 2 part 1: the jacket comes off)

The owner (quoted exactly): "he pulls out his jacket, he just removes his jacket, revealing a new skin that I'm gonna send you. Which is
basically gonna serve as it's him, but like without the jacket ... During this next phase, he gets way faster."

- **Look:** `AgentRules.LOOKS` += `ccg_reaper_bare` (index 10, `REAPER_BARE_LOOK`; `textures/entity/ccg/ccg_reaper_bare.png` copied from
  `art/reaper/Reaper_Phase2.png`, which is painted WIDE-armed, so it is drawn on the wide model, not the slim one the plan guessed).
  `AgentRules.reaperLook(index)` takes both; `AgentClient` (his idle) and `AgentRenderer` (`brawny`, the `Build` chest) use it.
- **Server:** `AgentBrain.shed` runs first in `act`: at `REAPER_SHED_AT` (half) or when `hurt` set `Mind.shedPending`, once he is on the
  ground and not soaring, he cancels the kick, brawl, lightning and ground strike, stops, swings `CcgAgent.SHED` for `REAPER_SHED_TICKS`
  (34), facing his prey; at `REAPER_SHED_SWAP` (15) `Mind.bare`, `HexCcgAgentBare` saved, `dress` the bare look, speed `speed(mind)`
  (`REAPER_BARE_SPEED` 1.3); `REAPER_BARE_PACE` 0.7 on the rest between blows. `arm` keeps his hand empty while he sheds (Dawn back after).
  `hurt`: cancelled while shedding; with the jacket on `shedClamp` keeps any blow from taking him past half (`GhoulTank.bypasses`: `/kill`
  and the void still kill). `wake` restores the bare look and speed from the flag.
- **Client:** `AgentClient.pose` returns `AgentPose.shed(age)` whole for `SHED` (straight arms: he's a mob, no elbows): hands on his lapels
  (tick 5-8), pulled open (12), swept back behind him (16-20, the look switches at 15), dropped (24), neck roll (27-30), rest (34); the
  left arm a beat behind the right.
- **Not done (offer it if the owner wants it):** the coat itself falling to the ground behind him; now it's a clean switch while his arms
  are behind him.
- **Tests:** `AgentTests.shedding` (the clamp, hands before his chest at the grip and behind him at the swap, never inside him, rest at the
  end, the source wiring), `looks` (11 looks, the bare one wide).
- **Preview:** `docs/handoff/arima-shed-4.1.28.png` (a scratch `ShedPreview` over `PoseRender` + `ReaperPreview.chest`, the coat skin before
  the swap, the bare one after).
- **Not tested in game.**

## 4.1.27, what and where (Arima part 5: the ground strike's blades)

The owner (quoted exactly): "The thing that he does when he shoves the Quinque on the ground and emerges like a tentacle, it shouldn't be a
tentacle. It should be like a smooth part of the blade, as if the blade grew out of the ground."

- `ReaperTendril.blades(seed)`: {dx, dz, lean way, lean, height, across, delay} for a great blade (`HEIGHT`, `THICKNESS`) and three round
  it, all inside `RADIUS`; each grows by `out(age - delay)`. Its hits, timing and damage are untouched (`hits`, `erupts`).
- `QuinqueDawnMesh.risingBlade()`: Dawn's spike profile in a unit frame (y 0 .. 1, foot 1 across), 48 round, a fine gold line down each
  of six faces, its foot swelling. `client/render/ReaperTendrils.draw` scales and leans each (`lean`: Rodrigues about the level line
  square to the lean), its foot a quarter block under the ground, lit at the mark; no more `emitTubeLegacy` (`TendrilChain` dropped).
- Tests: `AgentTests` (the cluster inside the strike, staggered; the unit mesh; the renderer draws the blades).
- Preview: `docs/handoff/arima-ground-blades-4.1.27.png` (a scratch sheet: the Reaper in `AgentPose.ground`, the blades at a mark 4.5
  blocks ahead).
- **Not tested in game.**

## 4.1.26, what and where (Arima part 4: Dawn smooth, a reverse umbrella)

The owner (quoted exactly): "His Quinque, Dawn, needs a major rework in look so it's smooth and not cubic. We need it to open properly when
shooting projectiles. Like, actually make it spread open like a fucking umbrella." And after an umbrella opening back over the hand was
described: "It is not what I meant when I said an umbrella. I was being, I wasn't being literal, bro. It just meant open. Like a reverse
umbrella, not like that, bro."

- **`quinque/QuinqueDawnMesh`** (pure): built straight in item pixels (x along it, its axis at y 8 z 8, as its boxes lay: holds hold).
  `canopy` (the guard: `PANELS` 8, hinged at `HINGE`; opened, each panel keeps its share of the circle while its chord angle goes to
  `REVERSE` 145 degrees: a cup opening forward; flat at half way), `spike` (six blades laid shut, a gold line down each; its `ease`
  parameter would splay them: the alternative render, not used), `shaft` (grip wire, butt, hooked guard), the light at the point while
  open. `of(open)` caches `STEPS` + 1 meshes.
- **`QuinqueMesh`:** `has` (katana, Dawn), `diagonal(q)`, `turned(q, p)`, `drawn(q, p, out)` (Dawn telescopes along x to its grip
  `QuinqueDawnMesh.HAND`), `caseBox(q, out)` (Dawn: none), `of(q, open)`.
- **Opening:** `Quinque.dawnOpening(now, at, fires, spread, furl)` from the stack's `DAWN_AT`/`DAWN_FIRES` (set by players' shots,
  `QuinqueServer.opens`, and by every Arima aim path already); `QuinqueClient.dawnOpening(stack)`; the renderer draws that step.
- **Models:** tools/quinque_models.py `MESHES` += dawn: its weapon frames and its fire frame builtin/entity, the cases still boxes (re-run
  reproduces everything else).
- **Tests:** `QuinqueTests.meshes` per quinque (Dawn along x, grips on its axis, its opening times, the open cup forward and wide).
- **Previews:** `docs/handoff/dawn-smooth-4.1.26.png`, `dawn-first-person-4.1.26.png`, `dawn-blades-alternative-4.1.26.png`.
- **Not tested in game.**

## 4.1.25, what and where (Arima part 3: both fighting styles)

The owner (quoted exactly): "We also want to improve the combat so you can use both styles of fighting that we got. Both the Kickmaster and
the normal one, along with the flips and whatever."

- **Server:** `AgentBrain.brawl` (in the Reaper's chain after the ground strike, lightning and kick): within `REAPER_BRAWL_NEAR` (2.8),
  `REAPER_BRAWL_CHANCE` (0.08) a tick he could, a string (`AgentRules.brawlStyle`: KICKS or BLITZ) strike by strike on `PaleStrike`'s
  ticks: `brawlBlink` at `BLINK_TICK`, `brawlLands` at `impact` (within `REAPER_BRAWL_LANDS`/`KICK_LANDS` edge to edge;
  `REAPER_BRAWL_DAMAGE` x the strike's weight; its knock; `S2CPaleFx.IMPACT`), chained from `chain` while in reach, `REAPER_BRAWL_REST`
  after; `cancelBrawl` wherever the kick is cancelled.
- **Client:** the swing code `CcgAgent.STRIKE` (40) + the strike's ordinal; `AgentClient.pose` returns `AgentPose.brawl(strike, age)` whole
  (legs and all: `take` copies only arms and some body channels). `brawl` = `PaleMotions.strike` from the guard, the right arm in Dawn's
  idle shoulder carry, pitched up by the trunk's lean.
- **Tried on renders and dropped:** the strike's own right arm (Dawn swung through his legs and the ground); the right arm in Dawn's stance
  hold (it points down through a leg whenever the body turns).
- **Tests:** `AgentTests` (both strings; codes clear of the others; Dawn's ends above the ground every half tick of every strike).
- **Previews:** `docs/handoff/arima-brawl-front-4.1.25.png`, `arima-brawl-side-back-4.1.25.png` (a scratch sheet over
  `QuinquePreview.item` and `PoseRender`, the Reaper's skin `art/white_reaper_skin.png`).
- **Not tested in game.**

## 4.1.24, what and where (Arima part 2: his mansion)

The owner (quoted exactly): "Toda uma outside in the garden, like in this place. Make sure also doesn't spawn like in the ground, like the
tower" (voice: "Arima outside in the garden").

- **The template:** `tools/companion_structures.py` `PLACES["reaper_mansion"]` (re-run it after a change: Anteiku and the tower come out
  byte-identical). `crop` (1, 28) drops the empty bottom layer; `fill_below` 6 fills the air under the lawn (dirt two deep, stone under);
  the anchor jigsaw (39, 6, 60) sits on the lawn's layer, so `start_height` -1 with `WORLD_SURFACE_WG` puts the lawn on the surface (the
  pools dig in), and `DryPlace`'s lift then raises it to the footprint's highest ground. `who_data` writes Arima's ForgeData:
  `HexCcgAgent` (look 9, Dawn, leader), `HexCcgAgentRankLevel` 10, `HexCcgAgentMansion` 1. Checked against minecraft-data 1.20 blocks
  (`--blocks`, fetched from raw.githubusercontent.com).
- **Worldgen:** `worldgen/structure/reaper_mansion.json` (the tower's, its own pool and anchor), `structure_set` salt 1324066192,
  `template_pool/reaper_mansion/start.json`.
- **Arima:** `AgentBrain.wake` makes him the Reaper as any saved Rank 10 alone (the slot claim); with `Mansion` set he saves his home where
  he first wakes; `patrol` strolls him within `REAPER_HOME_STROLL` (7) of it and walks him back when further. `AgentSpawner` no longer makes
  a Reaper in the wild (`REAPER_CHANCE` 0). `/kagune reaper` still summons one for tests (a mansion Arima waking after that one is
  demoted to Rank 9, as before).
- **Tests:** `NpcTests` (the places lists, the four jigsaw places and salts), `CompanionTests` (the template: size, Arima on the lawn with
  his flags, the anchor, no short grass), `AgentTests` (never in the wild, the home's numbers).
- **Previews:** `docs/handoff/reaper-mansion-4.1.24.png`, `reaper-mansion-garden-4.1.24.png` (an isometric render of the generated
  template; the renderer was a scratch script: faces of exposed cubes, painter's order).
- **Not tested in game.**

## 4.1.23, what and where (Arima part 1)

- `AgentRules.dawnChance` returns 1; `AgentBrain.drop` drops Dawn's case however he falls.
- `AgentBrain.reaperStruck` (LivingHurtEvent): a ServerPlayer's blow on the Reaper, remembered in memory by his UUID (lost on a restart:
  fine within five minutes). `AgentBrain.slain` hands the Reaper to `reaperSlain`: his killer and everyone within
  `AgentRules.REAPER_SHARE_TICKS` (6000) who is online earns `hexkagune:slain_reaper` (`MinecraftServer.getAdvancements` m_129889_,
  `ServerAdvancementManager.getAdvancement` m_136041_, `ServerPlayer.getAdvancements` m_8960_, `PlayerAdvancements.award` m_135988_:
  stubs in `stubs/net/minecraft/{server,advancements}`, allowlisted with joined.tsrg and intermediary evidence; maps fetched from
  raw.githubusercontent.com as `tools/srg_lookup.py` says) and, if a ghoul, 60 mastery. The advancement's parent is
  `minecraft:adventure/root`.
- Tests: `AgentTests` (Dawn every time, the five-minute window, the code awards the JSON's one criterion).
- **Not tested in game.**

## 4.1.22, what and where (first person for the kagune-made weapons)

The owner (quoted exactly): "Just as soon as you finish the katana, just make the first person of the other weapons. Don't touch their looks."

- `ArmItemLayer.FirstPerson.hand`: an Arm weapon but the whip takes `KaguneBodyAnimation.current(player, partial)` (posed on demand) and
  draws through `throughBody(pose, posed, slim, false)` (the view, the arm's pivot and turn, no ItemInHandLayer step) then
  `WeaponLayer.mesh` (its turn in the fist with it), as its third person layer does on the arm. No pose (no animation layer): the old
  camera motion (`QuinqueOwn.arm`).
- Kept: the launcher (through the body it floats side-on above the crosshair; its own view aims along it) and the whip (its tendril is
  laid in camera space from `QuinqueOwn.whip`; the handle alone through the body would part from it).
- `FirstPersonQuinque.arm(body, arm, point)` mirrors the chain (tests, previews). `QuinqueTests.firstPersonArms`: in the stance and every
  half tick of every blow some of the mesh is on screen and none nearer the camera than a block (the crystal spear's reversed third blow:
  0.6, seen on renders).
- Previews: `docs/handoff/first-person-{bone_sword,flesh_hammer,crystal_spear}-4.1.22.png`.
- **Not tested in game.**

## 4.1.21, what and where (the katana's cuts, hands first)

The owner (quoted exactly), of the rejected attempt: "Look at how it's pointed. Look at the hands, bro. Like, are you fucking kidding me?"

- **Why it was bugged:** the katana sits in a stiff arm at a fixed 66 degrees to it (the hold), so the blade can only point on a cone
  round the arm. 4.1.18's aims asked for blades off that cone and weighed the blade 1000x the hand, so the solver swung the arm back
  through the body. A fixed grip also can't make a straight diagonal: at a cut's impact the blade must come near the line of the arms.
- **Fix (`client/animation/QuinqueAim`):** hands first (`HAND` 3 per pixel squared, `BLADE` 40, `KEEP` 1), and two wrist angles solved
  per key (`solve` returns pitch, yaw, roll, wrist x, wrist z; `WRIST` cost 6, range `WRIST_X` 0.8, `WRIST_Z` 0.35). Author each key's
  hand as the end of a full arm: hand = right shoulder (5, -2, 0 right/up/forward from the neck) + 9 x the arm's direction, in front of him.
- **Keys:** `QuinquePose.Key` gained `wristX`, `wristZ` (a 9-argument constructor keeps the old keys); `swing` adds them to
  `RigPose.weaponX/Z`, clamped to the range (the curve overshoots between keys). Bake as before (`tools/QuinqueAimBake.java`).
- **Drawn:** `client/QuinqueWrists` (each body's wrists as posed: players in `KaguneBodyAnimation`, investigators in `AgentClient`;
  first person in `ArmItemLayer`), applied by `QuinqueItemRenderer` about the right grip through `QuinqueGrip.wristInItem` (the arm-frame
  turn carried into the item's own frame). Only mesh quinques (the katana) turn; box models draw straight.
- **The cuts** (`QuinquePose.katanaAims`), judged on front, side and above renders: kesa-giri down through the middle (a key at 0.49
  points the blade at the target so the tip runs a straight diagonal from the front) to low left past the hip; gyaku-kesa rising low left
  to high right; yoko-giri flat right to left; tsuki level down the line; kaiten level all the way round. The arm rolls one full turn in
  kesa-giri (that is how the blade gets round): keep the roll spread out or `aimed` flags it.
- **Tests:** `QuinqueTests.aimed` now measures the angle between consecutive frames' arm turns (an Euler wrap is not a flip, a whipped
  roll is) and the wrists' range. `RigPose.zyx` is public for it.
- **Previews:** `docs/handoff/katana-cuts-final-4.1.21.png`, `katana-kesa-4.1.21.png`, `katana-first-person-4.1.21.png`. The sheet tool
  used (front, three-quarter, his right, above, columns through a swing) is a 60-line scratch class over `QuinqueMeshPreview.cell`-style
  drawing: `PoseRender.orbit(yaw, pitch, ...)` with yaw 0 the face, -90 his right, pitch 80 from above; `PoseRender.skin` a ccg skin.
- **Not tested in game.**

## 4.1.20, what and where (the katana grown out of kagune)

The owner (quoted exactly): "Gotta say that handle looks like shit, bro. The katana should be like every weapon in the game should look like
it's made from a kagune. That that's horrible. Not only that, but like I said, I don't want squares. You can see that katana still has"

- **Why a mesh:** item JSON models can only be boxes. The kagune-made Arm weapons (3.0.0) already draw smooth meshes (`ArmMesh`), so the
  quinques use the same builder. `ArmMesh.Builder` now takes where its points and ways are stored (the hand's frame for the Arm weapons,
  as before; a quinque's item model pixels for these).
- **`quinque/QuinqueMesh`** (pure): `has(q)` (the katana so far), `of(q)` (built once), stored STANDING in item pixels (weapon terms u
  along from the guard at `ANCHOR` 6.35, v toward the back, w through). **`turned`**: the item draws it turned `DIAGONAL` (-45) about z
  through 8 8 8, as tools/quinque_models.py turns every box (`box(..., turned=True)`). The holds' grips are on that diagonal (katana right
  grip 4.75 4.75 8 = standing y 3.4). Forget the turn and the blade sits 45 degrees off in every hand while looking plausible in a render
  (it happened, and the test now catches it). `out(shown)` (deploy 0.2 .. 0.95 to 0 .. 1), `drawn(y, out)` (telescoped toward `HAND` 5),
  `caseBox(out)` and `caseMesh()` (the small case round the grip, a rounded box with a glowing seam).
- **The katana:** `blade` (96 lofted sections, back to edge; plates every `PLATE` with a lip and a seam, raised on the back and the
  shinogi only so the edge bevel stays one face; glowing edge quads from the wavy `temper` line to the edge), `guard` (a flesh collar,
  two RC eyes, a lumpy veined tsuba, two Rinkaku tendrils coiled over its rim and hooked up the blade), `hilt` (ribbed core, two flat
  sinew bands half a turn apart so they cross on the hilt's faces, black collars, a flesh pommel and eye). About 4600 quads.
- **`client/QuinqueItemRenderer`** (a BlockEntityWithoutLevelRenderer: `m_108829_` renderByItem): draws the mesh turned and telescoped,
  the case while it comes out, `ItemRenderer.m_115222_` for the glint (uvs from position so it runs over it), glowing quads full bright.
  Its render type is made lazily (`Solid`): the class is loaded as items register.
- **`ControlItems.MeshQuinque`**: a SwordItem whose `initializeClient` (Forge's, called by Item's constructor on a client only) gives
  `QuinqueItemRenderer.offer` through `DistExecutor.unsafeRunWhenOn` (the server check passes).
- **Models:** tools/quinque_models.py `MESHES` (`{"katana"}`): the weapon's `full` and `out_1..6` are `minecraft:builtin/entity` with the
  same display (so the holds hold); the case frames stay boxes. Re-run it after a change; it reproduces every other file exactly.
- **API** (allowlisted with joined.tsrg, intermediary and yarn evidence; maps fetched from raw.githubusercontent.com): BEWLR class, its
  constructor and `m_108829_`, BlockEntityRenderDispatcher, EntityModelSet, `Minecraft.m_167982_`/`m_167973_`, `ItemRenderer.m_115222_`,
  `ItemStack.m_41790_`, Forge's `IClientItemExtensions` and `Item.initializeClient`. The verifier checks overrides too.
- **Tests** (`QuinqueTests.meshes`): drawn by code exactly when a mesh; frames builtin/entity, the case still boxes; thousands of quads,
  all finite, as long as the old model; both grips on the hilt's axis; `turned` as the baker's turn; the draw grows and the case shrinks;
  `out` reads the baker's deploy values; the compiled renderer has `m_108829_` with its descriptor and the item `initializeClient` through
  DistExecutor; the renderer turns about the model's middle; no render type made on loading. `firstPerson` now uses the mesh's points.
- **Previews:** `docs/handoff/katana-kagune-4.1.20.png`, `katana-kagune-held-4.1.20.png`; `QuinqueMeshPreview <dir> KATANA` (alone, held,
  draw, first person); `FirstPersonPreview` and `QuinquePreview` draw mesh quinques too.
- **Not tested in game.** If the katana is invisible in game, look first at the renderer (the log says "drawing a quinque" if it throws),
  then at whether Forge called `initializeClient` (without it, vanilla's own BEWLR draws nothing: invisible too).

## 4.1.19, what and where (first person)

The owner (quoted exactly, from 4.1.16's list): "don't forget the the first person animation, like first person view to match the
animation that they got, because they currently don't have that." And this session's rule from them: "it does not give up until it's a
perfect match and it's a perfect combat system. Like, the animations always have to be top tier, no excuses whatsoever."

- **`client/animation/FirstPersonQuinque`** (pure): `still` (spin 0, `rightArmHold` kept: `RigPose.add` drops it), `view` (model pixels to
  camera blocks: the model's 0.9375 from `EYE` {4.5, -1, 24} (left, down, back from the neck), the body's step (drop down, lunge ahead,
  blocks), `PITCH` 9 degrees down, `ZOOM` 1.7 on camera x and y only), `held` (`QuinqueGrip.held` then `view`), `seen` (70 degrees, 16:9).
- **`ArmItemLayer.throughBody`** does `view`'s steps in its order with PoseStack ops (non-uniform scale for the zoom: PoseStack fixes the
  normals), then the third person chain (arm pivot, Rz Ry Rx, ItemInHandLayer), and the item is drawn with `THIRD_PERSON_RIGHT_HAND`.
  Every quinque with a hold but the ranged launcher goes this way (the blade too: it has a hold).
- **Tried on renders and dropped** (`tools/FirstPersonPreview.java` makes every one of these in seconds):
  - the true eye: the cuts happen a block under the screen;
  - 4.1.18's patch, 12 pixels back and level: the cuts land on the bottom edge, the hilt swells into the view on a follow through;
  - the grip's path squeezed into view with the blade's angle kept: lifting the hands turns the hilt onto the eye;
  - the posed arms drawn too: from anywhere behind the shoulders they wall off the view (vanilla draws no arm under a held sword).
- **Tests** (`QuinqueTests.firstPerson`): `throughBody`'s ops in `view`'s order (source check); no spin; a lunge of 8 pixels moves it half a
  block along the look; at rest the grip in view, right of and below the crosshair; the draw in view at every frame; through every swing
  (every 0.02) some corner of its full model always in view, the impact (0.4 to 0.65) in view, no corner nearer the camera than
  `FIRST_PERSON_NEAR` (1 block; today's nearest 1.24, katana swing 2). **One exception, written in the test:** the mace's second swing
  (0.5: its old wind-up behind the head; its third swing's shaft also passes through the head in third person). Remove it when the mace
  is rewritten by intent, and add a head-clip rule for every string.
- **Previews:** `docs/handoff/katana-first-person-4.1.19.png`, `quinques-first-person-4.1.19.png` (third person from behind over first person
  at the same moments). `java ... FirstPersonPreview <dir> <kind> [match]` (kinds: katana, koukaku, mace, bikaku, dawn).
- **Not tested in game.** Left-handed players (main arm left): third person was already drawn on the right arm; first person matches it.

## The owner's big list (after 4.1.16), in their order: do it all, one version at a time

Their files (never alter them): `art/coat-gap-walking.png` (the gap circled), `art/reaper/Reaper_Phase2.png` (Arima without his jacket),
`art/reaper/Reaper_Quinque1.jpg` and `Reaper_quinque2.jpg` (the phase-2 blade), `art/city/small-city.zip` (a world save in the new
`dimensions/minecraft/overworld` layout, with the city in it: find its extent, cut it out as a 1.20.1 structure, map any block 1.20.1 lacks).
Arima's own schematic has NOT been sent yet: ask for it if it is still missing when that step comes.

The owner (quoted exactly):

> Okay, this is a big one. Make sure to add to the to-do list that I don't want whenever, like, the walking is fine with the armor. And don't
> get me wrong, now it's fine. But whenever you walk, like, it opens like a fucking, like a gap in every armor that we got. Like the two, they
> open like a gap. And that gap is what I don't like about the armor. It's just that. And after that is fixed, we gotta add the M1s. Like,
> we need to improve, like, every Kagune's M1. And, I mean, not Kagune, sorry, the Quinkei, the CCG weapons. We need to improve every single
> M1 of every single weapon. They're bad. I mean, you keep passing them, but they're just the most basic garbage you can have, which I don't
> approve of. And I think if you look at the renders, we should make this a rule for every AI. So on the AI, on the next handoff, always
> specify that the AI is to evaluate the renders that it generates, and it does not give up until it's a perfect match and it's a perfect
> combat system. Like, the animations always have to be top tier, no excuses whatsoever. So upgrade the Quinkays, add better pull-out
> animations to the Quinkays so that they are more realistic when they're returning to the case. Make sure to add more Quinkays based on
> other, like, you can add Quinkays based off the war on Kagune, like, I don't know, different types of whips based on different Kagunes,
> custom ones, like more different swords, improve the ones that are already in here, like the katana looks like shit. Just keep improving
> what we already got, and don't forget the the first person animation, like first person view to match the animation that they got,
> because they currently don't have that.
>
> After that push is done:
> After that is done, we're going to improve Arima, the Reaper. I think we need a structure for him. I don't think it's fit for him to spawn
> in the wild like a common person. I'm going to give you a schematic where he's going to spawn in always, and there's always only one of
> them also in the map. When both players are kill him, they get like a special achievement from our mod called like Slain Reaper or
> something like that. And he drops Dawn 100% guaranteed. Since there's only one of them in the map, that's gonna be new. We also want to
> improve the combat so you can use both styles of fighting that we got. Both the Kickmaster and the normal one, along with the flips and
> whatever. His Quinque, Dawn, needs a major rework in look so it's smooth and not cubic. We need it to open properly when shooting
> projectiles. Like, actually make it spread open like a fucking umbrella. I don't know how to explain. The thing that he does when he
> shoves the Quinque on the ground and emerges like a tentacle, it shouldn't be a tentacle. It should be like a smooth part of the blade, as
> if the blade grew out of the ground. And I think that we need to create a new weapon for the Reaper based on the images that I'm going to
> also send you. This is kind of a big rework. And I need to create that weapon that I'll show you exactly how it is. It is a heavy weapon,
> dual-handed, that deals massive damage and, yeah, just causes bleed whenever hit. Arima can be super fast while using it. He does not drop
> this weapon though. After the fight is done, he breaks the weapon. The blade shatters at the end, and it's just broken. And he accepts his
> death a little before he dies actually, and then he just falls on his knees and dies after the weapon shatters. During like this next
> blade that he gets it during the second phase, which is gonna have a second phase. Which is like he pulls out his jacket, he just removes
> his jacket, revealing a new skin that I'm gonna send you. Which is basically gonna serve as it's him, but like without the jacket. That's
> the skin I'm gonna send you. During this next phase, he gets way faster. He can dash more precisely. He's way more precise. His entire
> moveset changes because he no longer has the Dawn weapon. So he's more into a melee close combat type. Since the weapon is massive, the
> one he gets is meant to be massive. He will always try to, get into range and kill you. He'll dodge moves. He will be a fucking monster
> basically. Whenever like this is not meant to be a player, like a solo mission. Whenever you kill him, you get the achievement and make
> sure that both players get it. If like two players hit him within like let's say 5 minutes, then you just need to hit him once. And if the
> other player kills him, then you also get the achievement in mastery. And yeah, that should be it. Another and then we get to a few
> problems that I'm going to list below.
>
> Bugs:
>
> Quests should only be done once. You can spam /kagune quest complete all over and like, I mean, it's not that you can spam it. It's just
> once you did all of them /quest complete, all of them should be finished. Even if you pick another kagune, this should still be finished
> because you finished all of them. You don't need to be typing it again. We also need to better the looks of the quest menu. It looks very
> bad. Very, very. I don't know. I just don't even feel like opening it half the time. I don't feel like opening it. It's just really dull,
> really bad looking, not interactive at all. It's just bad, bro. We need to better that.
>
> I'll be sending new structures to replace:
>
> ALl villages get replaced with the city in my minecraft world that I sent named "Small-City".
> Delete all ids hexkagune:village_x ( and anything related)
> add Hexkagune:small_city.
>
> This big structure is relatively rare (a bit rarer than a normal village), Humans can now however spawn in normal minecraft villages.

Order:
1. The coat gap while walking (`art/coat-gap-walking.png`).
2. Every quinque's M1, draw and return to the case, first person; more quinques; the katana.
3. Push.
4. Arima: his structure (one per world), the "Slain Reaper" advancement and mastery for every player who hit him in the last 5 minutes,
   Dawn always dropped, both fighting styles, Dawn smooth and opening like an umbrella, the ground strike as the blade growing up,
   phase 2 (jacket off, the heavy two-handed bleeding blade, faster, dodging, close-range), the blade shattering, him on his knees.
5. Quests completed for good (across kagunes); a better quest menu.
6. The Small City in place of every hexkagune village; humans in vanilla villages.

## Still open

- **4.1.15's look:** the owner judges the new kisses, games and night in game. Kisses tune in `MoodKissPose` (keep `clear`); how often the
  happy ones come up is `MoodKiss.of`'s roll.
- **The places:** 4.1.14 builds them at any good spot and keeps the first. If the owner still finds none in a NEW world after a `/locate`,
  ask for the log lines `HexKagune: <id> is built at ...` and `... was stopped at chunk ...`, and for what `/kagune places` says.
- **The look:** the owner judges the new slams in game. Likely asks:
  - how far the hand curls (`blow` curl keys);
  - the shoulder's size (`SHOULDER_*`);
  - the drive's speed (`Blow.drive`, `DRIVE_SNAP`).

  Any change must pass `slamsClearOfHim` and `slamsAligned`; re-tune against them, not by eye alone.

## Next after that

- Then the combos themselves (the rest of their ask: "study complex fighting moves, and combos"): the string
  is 7 fixed stages (`ComboStyle.UKAKU_WINGS`); ideas to pitch first, not build unasked: a held M1 finisher, a launcher into an air slam,
  the chain flowing (the next stage's chamber taken from the last one's recoil instead of from rest).
