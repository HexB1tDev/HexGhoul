> **Superseded (2026-10-27):** the final kakujas (War and Peace, HexKagunesv2 1.0.13 to 1.0.17) are handed over in `docs/handoff/kings.md`, and the prompt to paste is `docs/handoff/kings-next-prompt.md`.

# Handoff: 7.0.0, Furuta DNA replaces Owl DNA (state on 2026-10-24, branch `claude/furuta-dna-path-replacement-wja31k`)

Started from `claude/peaceful-archimedes-mn7lzq` (the newest work, with the user's own Owl model). The user's words are in
`docs/handoff/requests.md` ("The user's requests for 7.0.0"): binding. **No agents.** Push as soon as something works (CI
publishes `build-<branch>`; the user tests that jar on a phone through the Amethyst launcher). Render previews and send them.

## How Owl DNA is dormant

`KagunePath.ETO` keeps its id `owl` and its ordinal, but `dormant()` is true and `active()` maps it to `FURUTA`: `fromId`
("owl", "eto") and `byOrdinal` return Furuta, so no saved path, command or packet can land on Owl DNA, and `isEto` is never true.
`PathServer.path` rewrites a stored owl/eto id to furuta. Its card is kept (`PathCards.dormantOwl`) but never shown; `PathMenus`
sends ETO to Furuta's menu. All Eto/Owl classes are untouched and compile; restoring it means removing the mapping in
`KagunePath.active` and `RENAMED`, and giving it back its card and menu.

## Where Furuta DNA lives

- Pure (tested in `FurutaTests`): `control/furuta/` `FurutaStage` (stages, limbs, lengths), `FurutaMove` (every move: key, stage,
  mastery, ticks, cost, cooldown; its `Beat`s per limb), `FurutaRig` (rest designs per stage and the pose for each beat kind,
  kept out of his body by `clear`), `FurutaMotion` (springs; a limb starts coiled in its root, so it grows out), `FurutaModel`
  (rinkaku, centipede with its mouth, thorn, crescent with almond eyes, the mass, the colossus), `FurutaMesh` (primitives),
  `FurutaArmor` (head: crown, star eye, grin; sockets; claws), `FurutaSkin` (the kakuja hide: a textured inflated box per body
  part, painted by `tools/FurutaTextures` into `textures/entity/furuta_kakuja(_glow).png`), `FurutaRules` (numbers).
- Server: `FurutaServer` (state, keys, stage, kakuja, regeneration, guards, survive, sync `S2CFurutaState`), `FurutaCombat`
  (every move's blows, travel, holds, tosses, counters). Hooks in `KaguneCombat.startSwipe` (M1), `AyatoServer.onAction` (K),
  `AyatoGates` (the original's grab/slam/guard refused for him), `GhoulRegen` (faster healing paid in RC), `KakuganServer`
  (right eye), `Cooldowns.clear`.
- Client: `FurutaClient` (views), `FurutaInput` (keys, right-click dash, `C2SFurutaAction`), `FurutaRenderer` (limbs, mass,
  colossi), `FurutaArmorLayer` (hide and head, a player layer), `FurutaMenuScreen`/`FurutaMenuView`, the HUD rows in
  `AbilityGuide.furuta`, the card `PathSilhouette.furuta`. Protocol "26".
- Previews: `tools/furuta-preview.sh` (sheets `stages`, `details`, `kakuja`, `head`, `moves`, `deploy` into `docs/images/`),
  `tools/furuta-textures.sh` (repaints the hide textures).

## Not done / not tested

- Nothing of it has been tried in game yet; first feedback will come from the user's test of the CI jar.
- The menu draws no live preview of the kagune (it lists stages and moves).

# Handoff: the 6.2.0 release (state on 2026-10-19, branch `claude/exciting-shannon-5imyb5`)

Started from `claude/funny-pascal-l0upfh` (the newest work). The user's words are in `docs/handoff/requests.md` ("The user's
requests for 6.2.0"): binding. **No agents** (said again). They want it fast: push as soon as an item works.

## Done (pushed)

- **Kakuja V2 deleted** (`EtoUnlock.KAKUJA_V2`, `EtoRules.KAKUJA_V2_MASTERY`, the K-hold notice and its lang key are gone; K held =
  K tapped, `EtoOwl.key`; menu: one wide kakuja slot, `EtoMenuView.kakuja`; path card strip ends at the Owl).
- **Speed Kick (base form, V tap), Kizaru's flying side kick.** `EtoRules` kick section (range 20; `KICK_BURST` lunge 2 ticks;
  dash 2..4 ticks at 5 b/t, peak 1.4x; `kickArrive`, `kickGone`, `kickUntouchable`, `kickHeight` = lift x `kickRise` + the
  rebound's `kickHop`, `kickAirborne`; foot `KICK_FOOT_*` measured off the pose; flight `KICK_SPEED` 4.6 carried by
  `KICK_CARRY` 0.98, `KICK_HARDEST` 5, `KICK_PER_BLOCK`, `KICK_MOST_BLOCKS`, `KICK_BOWLED`). Poses `tools/EtoPoseDesign`
  `kickRush` (stick figure 1), `kickChamber`, `kick` (stick figure 2: flip 1.42 + roll 1.3 laid out flat; then a backflip to
  2 pi; the generator now writes `flipped(guard(), flip)` for guard keys that carry a flip). `EtoMotions.kick`: the lunge,
  gone (lunge blending into the chamber), the kick; the drop is zeroed while airborne (`kickAirborne`). Server
  `EtoMoves.kick/plan/tickKick/kickLands` (invisibility effect for the dash; `EtoServer.attacked` cancels `LivingAttackEvent`
  while `untouchable`); `BlockSmash.kick` + `carried` (a third launch mode: horizontal speed held, gravity vanilla's, bowling).
  Client: `EtoClient.kickGone` hides her body/hands, `EtoFleshRenderer` skips her first-person leg while gone, `KickDust`
  (vanish, wake, appear, the big blow), `EtoReactions.trail` (torn air behind the flying body), `EtoCamera` locks onto its
  middle. The Owl's kick keeps range 40 (`OwlRules.KICK_RANGE`). Previews `kick`, `kick-pose`, `kicked`, `kick-fp`.
- **Mutated Arm slower:** `ArmRules.SHOOT_TICKS` 12 (was 6), `DRAG_TICKS` 14 (was 9).
- **Mutations 3 and 4** (`EtoMutation`, milestones 500/800/900/1000, `EtoUnlock.MUTATION_POINT_3/4`; menu cards 44 high):
  - **True Anonymity:** `EtoRules.HAMMER_HUGE` 1.7, `hammerCharge(held, huge)`, size functions take charge up to it (`grown`);
    the hammer moves carry detail 1; `EtoFleshModel.Shape.CLUB_SPIKED` + `boneSpikes`; bleed in `EtoMoves.hammerLands`.
    Preview `hammer-huge`. The huge plain club is ~3250 quads (over the 3200 budget; the spikes add ~200).
  - **Witness This:** (reworked twice after the user's GIF reviews: the blitz is 24 mixed punches and kicks, one a tick, straight
    after the uppercut, from points of a sphere round it, each knocking it `ACROSS` 3 blocks with `CENTRE` drawing it back,
    `WitnessRules.BLOWS`/`AROUND`/`OFFSETS`, her body tipped to aim, `tilt`; `EtoMotions.witnessBlow`; the finisher is a stomp
    straight down, `stompTuck`/`stompOut`, then `crouch`; the target is a KickTumble rag doll from the uppercut, `KICKED` fx +
    `EtoKnockdown.launched` there; GIF preview `witness-gif` writes `eto-witness.gif` and `eto-witness-slow.gif`) pure `eto/WitnessRules` (every place and tick), `EtoMove.WITNESS` (appended; `owl()` now bounded to the
    Owl's range), server `EtoWitness` (start from `EtoMoves.punch` at charge 1 with the mutation; hold, stun, blows, crater,
    `frozen` gates `EtoServer.onAction`/`AyatoServer.onAction`), `EtoCancel.allowed` refuses cuts during it, client
    `EtoClient.drive` (her path; a frozen local player is carried along the target path, `frozenIn`), `EtoMotions.witness`,
    `KickDust.witness` (afterimages at every blink). Protocol "25". Preview `witness`.

## Not done yet (the user's Request 1)

- **The Owl (V1): improve the model, smoother moves, a more polished look, a cooler mask.** Nothing changed there yet. Start
  with the mask (`eto/OwlModel`: `relief`, `beak`, `tufts`, `plumage`, `gashes`), render `owl` / `owl-anim` previews, show the
  user; then the smoothness of the Owl moves (`OwlMotions`, `OwlArms` blending).
- Not tested in game: everything above.

# Handoff: 6.2.0 part 2, the queue for the next session (state on 2026-10-18, end of session)

Branch `claude/lucid-ramanujan-blivey`, HEAD after a11ef25 (everything from part 1, the section below this one, is pushed and
built green). **Nothing in this queue is built yet.** The user's words for every item are saved verbatim in
`docs/handoff/requests.md`: read that file in full first, it is binding. The rules in "What the user said (binding)" and "How it
is built" / "Facts that bite" further down still hold.

**No agents, no subagents, no workflows. Work solo, one thing at a time** (the user said so again, loudly). Push a pre-release
(just push the branch; CI publishes `build-<branch>`) every time an item is done so they can test while you go on.

## Progress (2026-10-19, branch `claude/funny-pascal-l0upfh`)

Items 1 to 6 are done and pushed, plus the user's extra asks: the revive from the kakugan off (stands up in her kagune mode),
the 4x4 gust, the cleaner reassembly, the Mutated Arm catching close targets, the menu's moves tab replaced by the mutations.
Then, from the user's testing:

- **The revive's blood is the explosion's alone** ("just the initial explosion blood and then stop"): the gushes, blood off
  the flying pieces, a pool where each lands; no pumping from torn ends, trails or squeezes after (`ReviveClient.bleed`).
- **First person showed none of her arms: fixed at the root.** `KaguneBodyAnimation.LAYERS` was a WeakHashMap keyed by the
  player; an entity's equals/hashCode are its id, and the layer is made in the player's constructor before `setId`, so every
  lookup missed (her own arms, `drawn`, `drawnNow`, `bladePose` all fell back or vanished). Layers are now found by identity.

Item 7, the Owl's movesets, one by one (`EtoMove.owl()` moves, `EtoOwlMoves` server, `OwlMotions` body and giant arms,
`OwlArms` per-player blend in and out, `OwlPose.toTorso`: giant arm keys are laid out in the frame of the way she faces and
converted onto her torso as posed; blends turn each part the shortest way round, `OwlMotions.blend`):

- **Z, the Owl's Big Punch: built.** `OWL_PUNCH_CHARGE`/`OWL_PUNCH`, the same keys, charge and cooldown as the Big Punch
  (`EtoMoves.punchKey` picks the Owl's while `state.owl`). The right giant arm rears up behind her, the left blade braces on the
  ground; let go, her fist and the giant elbow land together, the whole arm driven through (weighted: `OwlMotions.heavy`).
  The target flies dead level along the blow (`BlockSmash.drive`: its height held, constant deceleration to `OwlRules.punchDistance`,
  16 to 56 blocks; anything up to `PUNCH_HARDEST` in its way within `PUNCH_SMASH_REACH` 10 blocks broken and restored as every
  smashed block; past that or harder: stopped dead, crash damage; what it slams into is hurt and thrown aside; stunned in flight).
  Fx: `S2CEtoFx.OWL_PUNCH` impact (rings marching along the line, view jolts), `SMASH` debris per tick of breaking.
  Tests `EtoTests.owlMoves`; preview `owl-punch` (`docs/images/eto-owl-punch.png`).
- **V tap, the Owl's Speed Kick: built.** `OWL_KICK` (detail: dash ticks; point: where she plants; `EtoOwlMoves.kick/tickKick`,
  same target choice and "No target found" as the Speed Kick). She dashes at `OwlRules.KICK_DASH_SPEED` 6 blocks a tick to
  `KICK_STAND` 2 blocks in front of it (the client moves her, `OwlRules.kickPlace`, keys held, view locked on), the right giant
  arm cocked over her head and driven through its middle (`KICK_SKEWER`: stab, bleed, `IMPALED` fx; held aimed at its middle
  while she twists, `OwlPose.aimAt`), the left wound back and slashed across its neck (`KICK_SLASH`: bleed, `BONE` fx), the
  right drawn out (`KICK_PULL`, blood), a 270-degree spinning heel stepping in `KICK_STEP` (`KICK_BLOW`: launched straight away
  and up, knockdown, `KICKED`), down by `KICK_TICKS`. It is pinned and stunned until the heel. The blades' arms are pulled up
  overhead through the fast part of the spin (as a skater) and flung out wide after; the arms follow her whole-body spin only
  from the spin on (`OwlMotions.followsSpin`). Dash dust and afterimages (`KickDust.owlDash`); blade trails on every Owl move
  (`client/fx/OwlTrails`, from the drawn blade points). Preview `owl-kick` (`docs/images/eto-owl-kick.png`).
- **J hold, the Owl's bone spikes: built.** `OWL_SPIKES` (detail: the seed; charge: the tick J was let go, 0 while held; the
  move re-broadcast with it, the client clock runs on). Pure shared model `eto/OwlSpikes` (server and clients from one seed):
  14 spikes all round her (front ones out of the bulked shoulder masses at +-6.6..7.6 px, splayed past her head and arched up;
  side ones rising off her shoulders; back ones off her back, now and then one standing up high), staggered in a shuffled order
  `STAGGER` 0.45 ticks apart after the `SPIKES_HUNCH`, each bursting out over `GROW` with a hair of overshoot, held while J is
  (at most `SPIKES_HOLD` once all out), drawn in over `RETRACT`. Mesh: ridged, knobbed, raw red at the root, a dark needle
  point, one or two hooked barbs; ~60 quads a spike. Server (`EtoOwlMoves.spikesKey/tickSpikes`): each spike's line from
  `OwlSpikes.root` (her hunched back as a fixed frame, within 0.3 blocks of the drawn root, tested) hits each body once a burst:
  through its middle = run through (held on it while out, `IMPALED` fx), else knocked aside and staggered (`BONE` fx); bleed,
  stun. Body: hunched, jolted by each spike the way it went, trembling under them, straightening back up; the giant arms braced
  on the ground like a second pair of legs, flaring as they burst, lifted before going back to rest. Fx `client/fx/OwlSpikeFx`
  (dust ring and view jolt at the first, blood and bone chips at each root, a spatter as they go in). Preview `owl-spikes`.
- **M1, the Owl's Rend: built.** `OWL_STRIKE` (detail: the cut 0..3). `EtoBlitz.click` hands M1 to `EtoOwlMoves.click` in the Owl;
  clicks queue and chain like the blitz (`OwlRules.REND_CHAIN`, `EtoRules.CHAIN_WINDOW`, `state.owlRend/owlRendEndsAt`), she
  steps in to `REND_STAND`. Cuts: the right blade down across, the left back across, both scissoring, both overhead into the ground
  (finisher: knockdown, `FINISH_RECOVERY`, two little craters where the blades bite, `REND_BITE_*`). Each hit: slash wound, bleed,
  stun (the finisher's heavy), knocked the way the blade went (`REND_KNOCK`), `BONE` fx. First person shows her own arms through
  it (`EtoAnimator.arms`). Preview `owl-rend`.
- **J tap, the Owl's Reaping Whirl: built.** `OWL_WHIRL`: coiled to her right `WHIRL_WIND`, then a whole turn to her left
  (`OwlRules.whirlTurn`, the very function her body spins by) with both giant arms out at full reach, the blades carried out; each
  tick both blades' sweep is checked (`EtoOwlMoves.reap`, `swept`): reaped once (slash, bleed, stun, thrown out and on round).
  On the swipe's cooldown. Fx `S2CEtoFx.OWL_WHIRL` (ring of torn air, dust). Preview `owl-whirl`.
- **M2, the Owl's Pounce: built.** `OWL_LEAP` (detail `OwlRules.leapDetail`: the flip's way + 16 when Up We Go soars; charge:
  the tick she landed, re-broadcast). Sunk with the blades reared (`LEAP_CROUCH`), the bound (`OwlRules.leap`: further and flatter
  than a flip, more with Up We Go), tucked in the air, and `LEAP_SLAM_AFTER` ticks after touching down both blades into the ground
  `LEAP_SLAM_AHEAD` ahead: everything within `LEAP_SLAM_REACH` cut, bled, thrown up, stunned (heavy), two craters, shockwave fx.
  No fall damage; on the flip's key and `LEAP_COOLDOWN`. Preview `owl-pounce`.
- **V hold, the Owl's Fissure: built.** `OWL_FISSURE_CHARGE`/`OWL_FISSURE` on the Flesh Hammer's key, charge and cost
  (`EtoOwlMoves.fissureKey/tickFissureCharge/bringDown/tickFissure`, `EtoRules.hammerCharge`). Held: rooted, reared onto her
  toes, both giant arms raised high with the blades hooked over her, trembling at full charge. Let go: both blades and her
  weight brought down `FISSURE_AHEAD` 2.4 ahead at `FISSURE_IMPACT`, a heavier crater there, then the split runs on along the
  way she faces `FISSURE_SPEED` 2 blocks a tick for `OwlRules.fissureLength` (5 + 6 x charge) blocks: a crater every
  `FISSURE_STEP`, shockwave fx, everything within `FISSURE_WIDTH` of the line thrown up (`FISSURE_UP`), hurt, heavily stunned,
  once. Preview `owl-fissure` (`docs/images/eto-owl-fissure.png`).
- **The giant arms: the red muscle does the work** (the user: "the blades are not meant to move so much, the red part is the
  muscle, it should be the one doing most of the work"). The blade is carried at an elbow hinge: `OwlPose.elbow(swing)` reads a
  swing as {how far the blade is bent off the upper arm, which way round from the hinge}, `OwlPose.carry` builds one back. The
  way it naturally bends (`HINGE`: toward her front, a little up and in) is carried from `HINGE_FROM` to the arm by the least
  turn, smooth everywhere except `HINGE_BLIND` (in, down and back into her own body, where an arm never points: any such
  reference has one blind spot, and the old one, up and forward, was where every overhead slam swung through). Keys are sampled,
  weighted (`heavy`/`follow`: the upper arm lags `UPPER_LAG`, the bend settles `BLADE_LAG` 0.5 behind) and blended in bend space,
  so between keys the blade only flexes, never orbits the arm; every key is bent 32..45 degrees the same way (re-solved from the
  old keys to keep each blade point within ~0.1 block where it struck). `OwlMotions.carried` guards the bend at
  `ELBOW_LEAST`..`ELBOW_MOST`; blends in from what was drawn happen in the facing frame (`OwlPose.fromTorso`), where the keys bend
  the blade; `aimAt` swings the whole arm onto the target; the idle (`OwlPose.swing`) is carried too (the muscle heaves, flicks
  and lifts the blades clear running). Measured in world space, the blades' own share of their motion went from 13..63% (the punch
  56..63%, the Fissure 42..51%) to 6..31%, the elbow from 0..94 degrees to 32..47, the worst flick from 3.3 to under 1.9 blocks a tick. Tests (`owlSequence`, every
  Owl move): the bend in range every frame, a blade's own motion at most 35% of its point's travel, never 0.5 blocks in a quarter
  tick (all fail on the old arms).
- **B, the Owl's Watch This: built.** `OWL_WATCH` (the body: hunched, reared up with the giant arms flung wide as they tear out,
  `LANCES_POSE_TICKS`) and `server/OwlWatch` (Watch This's mastery, cost and cooldown): six lances of red muscle tipped with
  barbed bone spears (`eto/OwlLances`: phases, sectors, geometry, mesh ~300 quads each; `client/animation/OwlLancePose`: each out
  of its place on her back as drawn, fanned toward its own way round her) burst out into a crown and hunt for `LANCES_ACTIVE` 10 s,
  each favouring its `bearing` (ahead either side, her sides, behind): something hostile to her (as Watch This: a mob after her or
  a grudge, `EtoWatch.grudges`) within `LANCE_RANGE`, not another lance's, not just slammed. It rears up high and dives
  (`diveTicks`) steeply down through it into the ground (`diveDir`, `through`: a 2.3-block spear reaches the ground even through a
  golem's chest; stab wound, bleed, stun, `IMPALED` fx), pinned there `LANCE_PIN` (fixed to the ground, wherever she goes), then
  `hoist`ed up over her (its feet clear above her head, tested) and slammed down on her far side (`slamPoint`, the ground found
  there; hurt again, crater, shockwave, heavy stun, a bounce), torn free (`LANCE_RECOVER`). Turning back out of the Owl draws them
  in (`OwlWatch.retract`); death, logout, another world drop them at once. New trackers get every lance (`OwlWatch.tracked`).
  Packet `S2COwlLance` (id 24), client store `OwlLanceClient`, drawn in `EtoFleshRenderer.lances` (spear trails into `OwlTrails`),
  burst fx `client/fx/OwlLanceFx` (blood, bone chips, a view jolt; a spatter going in). Tests `EtoTests.owlLances`; preview
  `owl-watch` (`docs/images/eto-owl-watch.png`).
- **N, the Owl's blade volley: built** (the user's pick after a thin-lance version was rejected: "everything about that move looks
  off"). `OWL_VOLLEY` (detail 1 held; charge: the tick it was let go, re-broadcast) on the Mutated Arm's mastery, cost and
  cooldown (`server/OwlVolley`). Each giant arm rears back over its shoulder and whips through (`OwlMotions.volley`/`volleyArm`, a
  6-tick cycle per arm, the torso winding and uncoiling; she faces her look, `EtoMotions.facesLook`), ripping a fan of bone spears
  off its blade (`eto/OwlShards`: `release` within a block of the drawn blade tip, tested) at what her crosshair is on
  (`OwlVolley.aim`: the first body or block along her look). Tapped: right then left, three spears each; held: a storm, turn about
  every `STORM_EVERY` ticks while held (each throw past the first two costs `STORM_COST` RC; out of RC ends it), out to
  `STORM_MOST`. Spears fly in the world on their own (`SPEED` 3, a little `GRAVITY`, `LIFE`): the first body met is run through
  (hurt, bleed, stun, `IMPALED` fx) and carried along the flight until a wall (pinned there) or `PIN_CARRY` (driven down into the
  ground there), held `PINNED` ticks; a spear that hits nothing sticks where it lands, and every stuck spear crumbles after `STUCK`.
  Cut by another move: no more throws, the cooldown charged, spears out fly on. Packet `S2COwlShard` (id 25: thrown, stuck, gone,
  clear), client `OwlShardClient` (flies each from its throw alone) drawn in `EtoFleshRenderer.shards`, fx `client/fx/OwlShardFx`
  (ripped off the blade, stuck, crumbled). Tests `EtoTests.owlVolley`; preview `owl-volley` (`docs/images/eto-owl-volley.png`).
- Every base move now has its Owl counterpart; the old "That move isn't in the Owl yet" notice is gone.
- **The path is Owl DNA (queue item 8: done).** `KagunePath.ETO` keeps its constant (code only) but its id is `owl` and its name
  "Owl DNA"; `KagunePath.fromId` still reads the old saved id `eto` (`RENAMED`), so nobody loses their path. Commands:
  `/kagune path owl` (an operator's own path; new) and `/kagune path set <player> owl` (`eto` still accepted, unlisted). Renamed
  everywhere a player sees it: path card ("OWL DNA PATH", its stage strip shows the Owl as built), menu title, ability guide, chat
  messages, config comments and keys (`owlBlockSmash`, `owlBlockSmashRestoreSeconds`: a changed value must be set again), log
  line, README; her rank titles are an experiment's ("Test Subject", "Owl Strain", "One-Eyed Owl"; no Sen Takatsuki, no Aogiri).
  Kept on purpose: class and package names (`Eto*`), lang keys (`message.hexkagune.eto.*`) and save-data keys (`EtoMutations`,
  `EtoRevive*`), none of which a player sees, the last of which would wipe saves if renamed. Guard: `EtoTests.owlDna` scans
  every lang value and every string literal in the mod's code for the names (fails on the old code, tested).
- Dispatch: `EtoServer.onAction` routes keys in the Owl (`EtoOwl.ready`: grown out); base moves keep `EtoCancel.allowed`
  (never in the Owl), Owl moves `EtoCancel.allowedOwl`. K out of the Owl cuts any of its moves. Body order in
  `KaguneBodyAnimation`: the Owl's stance goes under its own moves (`EtoAnimator.owlPosing`) and over anything else.

## The queue (do it in this order: quick wins first, then the big ones)

1. **M1 stun.** Every Speed Blitz hit (M1, and later the Owl's M1) stuns what it hits for at least 0.5 s (10 ticks),
   refreshed by every hit, so it can't fight back mid-combo (iron golems hit her the moment she touches them now).
   - Hits land in `server/EtoBlitz.java`: `land` (:245) -> `strike` (:286, damage at :300 `m_6469_`).
   - Plan: a pure `eto/StunRules` (duration, refresh, cap) + server `EtoStun` (map entity id -> stunned-until tick):
     cancel `LivingAttackEvent` whose source entity is stunned (melee, golem slams, projectiles from it); each tick stop the
     mob's navigation and damp its horizontal speed; a stunned player does no damage and is slowed. Prefer API already on
     `verify/api-allowlist.txt`; anything new needs a stub + allowlist entry with SRG evidence. Look at `EtoKnockdown`
     (the slow-to-nothing) for reuse. A small client tell (the head reeling, `EtoReactions`) if cheap. Tests.
2. **`/kagune cooldowns off|on`** (op level 2, optional targets): with it off every cooldown in the mod is skipped (every Eto
   move, the Owl's `owlReadyAt`, the revive's `EtoReviveReadyAt` / `ReviveRules.COOLDOWN`, Watch, the arm, and Ayato's and
   Kaneki's moves, kakujas and mutations, plus any in the original jar's `KaguneServerController`/`KaguneProfile`: check with
   `javap -p -c` on `build/base.jar`), and turning it off clears every running cooldown at once. Commands are registered in
   `server/ControlCommands.java` and `server/PathCommands.java` (the `/kagune` tree). Do it with ONE central helper every
   cooldown site calls (grep `ReadyAt`, `readyAt`, `cooldown`, `COOLDOWN` across `control/` to find every site), keep the HUD
   and the Eto menu's REGROWING timer truthful. Recommend persisting the switch in the profile. Energy is NOT a cooldown:
   don't touch the RC bar.
3. **First-person M1 (bug).** In first person the blitz strikes show nothing; they must match third person exactly. Reuse
   what the kick/hammer/arm do: `client/animation/EtoOwnLeg` (`limb`, `shift`: seen from where the pose has moved her head),
   `EtoOwnArm`, `EtoFleshRenderer.ownArm`; draw both arms (skin + sleeve, slim/wide) from the same `BodyFrame` pose as third
   person (`EtoStrikePoses` via `EtoMotions` / `KaguneBodyAnimation.drawn(player, move, time)`, falling back to the move's
   own pose), vanilla's hand hidden while a strike is on (`client/render/ArmHider`). Add a first-person preview sheet
   (`tools/EtoPreview.java` has `kick-fp`, `hammer-fp` to copy).
4. **The revive, more (You Thought You'd Get Rid of Me That Easily?).** The user loves it; wants **even more blood** (more
   `AyatoBlood.spurt` bursts, longer pumping from the torn ends, bigger `BloodStains` pools: `client/ReviveClient.java`,
   `client/render/RemainsRenderer.java`) and **every entity acts as if she really died**: on the cancelled death
   (`server/EtoRevive.dying`) every mob targeting her drops her (target cleared, brain memories like attack target / angry-at
   erased, neutral mobs' anger stopped, the Warden's anger, bosses too: Wither, Ender Dragon, raiders, piglins), and nothing
   can target her while she is in pieces (Forge `LivingChangeTargetEvent` cancelled when the new target is a downed player;
   check what 1.20.1 Forge has). Should also hold for the moment she stands up whole (they have forgotten her).
5. **Up We Go (Eto's second mutation).** Improves the M2 flips: launched significantly higher, travels further upward, more
   power, real momentum and smooth acceleration (not an abrupt vertical pop). Add it to `eto/EtoMutation` as a second entry
   (a new milestone after 500, e.g. 800; keep 500 as it is), the menu's MUTATIONS card (`client/EtoMenuView`, `EtoMenuScreen`,
   lang keys, `tools/UiPreview` mutation preview), the server/profile bitmask (`EtoRevive.MUTATIONS_KEY`, `hasMutation`). The
   flips: `EtoRules.Flip`, `EtoPoseDesign` flips, the client motion in `client/EtoClient` (find who applies the launch
   velocity). Tests `EtoTests.flips`.
6. **Animation cancelling and transitions.** Every ability usable at any moment, even mid-animation, mid-flip, airborne. The
   current pose (as drawn this frame) is captured and blended quickly into the new move with turns wrapped (a spin concludes
   naturally as she goes into the punch); upper-body attacks layer over ongoing leg/flip motion where possible, full-body ones
   take over. No snapping, no broken poses, no overlap. Never duplicate damage, never a second hit from one attack, never bypass
   a cooldown, never leave flesh/geometry/effects/held bodies behind. Today `EtoServer.free()` and the per-move checks refuse
   a new action while one runs (`EtoOwl.busy` blocks everything in the Owl): each move needs a proper "cut off" cleanup (the
   held/pinned target let go, flesh retracted, flags cleared, cooldown already charged stays charged). Client side: the move's
   start pose in `EtoMotions`/`EtoAnimator` must become "whatever was drawn" with a short blend. Extend the clipping tests to
   start every move from every frame of every other move.
7. **The Owl's movesets (Kakuja V1): a unique, much stronger counterpart for EVERY base ability**, reimagined animation and
   anatomy, not bigger numbers. Remove the blanket `EtoOwl.busy` block move by move as each is built. The user specified three:
   - **Z Big Punch:** charged as now (tap already strong, full charge enormous); the target flies in a **perfectly straight
     horizontal line** along the punch direction (no up/down, no random knockback), smashing through every block in its way
     within a 10-block terrain-destruction reach, debris and impact effects, timed screen shake and sounds; every block
     restored exactly afterwards (extend `server/BlockSmash`: it already breaks weak blocks along a launched body and restores
     exact states after 30 s, protection respected).
   - **V tap Speed Kick:** a near-invisible high-speed dash to the target; the first of the two giant upper appendages (the Owl's
     scimitar kagune arms above her own arms, `eto/OwlModel` + `client/animation/OwlPose.swing`) thrusts and skewers it and holds
     it briefly; the second sweeps across the head/neck in a fast sharp cut; then an upper-body twist and a powerful spinning
     kick sends it flying. Appendages move independently with momentum (heavy ends trailing), never rigid or teleporting.
     Restrained blood, motion trails, impact flashes, sounds. Responsive, not cinematic. No target: the existing no-target
     behaviour ("No target found").
   - **J hold Grab:** she hunches (head and upper torso down, shoulders and back tense) and many huge, irregular, sharp bone
     spikes erupt from her back in a fast staggered sequence (varied lengths, angles, trajectories); hits deal damage + the
     existing bleed (`AyatoCrystal.kanekiBleed`); some impale or stagger briefly; her shoulders/spine/arms react; the spikes
     retract smoothly. Bone cracks, flesh sounds, blood on hits, subtle environment effects.
   - **Designed by you (the user left it to us):** **M1** (the Owl's own combo: e.g. alternating giant-blade slashes with her
     body weight behind them, the last one a heavy overhead; it must stun like item 1), **M2** (combat use: e.g. a bounding
     pounce/leap that lands with both blades driven down, or a spinning blade vault), **J tap Swipe** (e.g. a huge horizontal
     sweep of both blades round her), **V hold Hammer** (e.g. both blades and her weight brought down in an axe slam that
     cracks the ground, BlockSmash crater), **B Watch This** (e.g. the tentacles become bone-blade lances from her back),
     **N Mutated Arm** (e.g. a giant arm's blade extends as a piercing kagune lance). Each needs its own identity, purpose and
     animation; use the claws, blades, shoulders, body weight; extra bone grows from a real place on her and retracts.
   - Everything: believable physics (inertia, anticipation, follow-through, force through rotations), smooth blending,
     multiplayer-safe (server decides damage/bleed/knockback/terrain, clients synced), optimised (quad budget under 3200 per
     player, cheap particles: custom `RcFx` only, never vanilla particles), and works with the cancelling of item 6.
   - Show the user each new model/animation in previews (PNG sheets) before wiring combat around it, as before.
8. **(Done, see Progress.) Rename the whole path to Owl DNA** (requests.md, Request 6: "this is not Eto from the show anymore ... some kind of
   experiment"; "Ito" is Eto as the user's speech-to-text writes it). After the Owl's B and N. The path command id becomes
   `/kagune path owl` (keep the old `eto` id working as a silent alias so saved players and old commands don't break); every
   player-facing string says Owl DNA instead of Eto: the menu (titles, cards, tabs, mutations, REGROWING), HUD, chat messages
   (`message.hexkagune.eto.*` values), item/advancement/key names, command feedback and suggestions, tooltips, the README's
   player-facing text. "I don't want to see Ito anywhere in the mod": sweep every lang value and literal string for "Eto"
   (case-insensitive, also "Eto's"); internal class/package names and lang keys may stay (players never see them), but say so
   to the user. Tests for the alias and the new id; full build.
9. Docs (README sections + images, this file), full `python3 build.py` green ("Verified hexkagune-6.1.0.jar"), push.

## The prompt the user gives the next session

> Continue the HexKagunes work from branch `claude/lucid-ramanujan-blivey` (merge it into your branch first). Read
> `docs/HANDOFF.md` from the top (the 6.2.0 part 2 queue) and `docs/handoff/requests.md` (my words, binding), then work the
> queue in order, solo: no agents, no subagents, no workflows. Push after every finished item so I can test the pre-release.

# Handoff: 6.2.0 work on the Path of Eto (state on 2026-10-18)

Branch `claude/lucid-ramanujan-blivey` (continues `claude/fervent-knuth-9h4i7r` at eee5068). Every push publishes the rolling
pre-release `build-claude-lucid-ramanujan-blivey`. Never add `.github/release-notes-*.md` (that publishes an official release).
Don't open a PR unless asked. The section after this one (2026-10-17) still holds for everything not changed here.

## This session's queue (from the user, in order)

1. Speed Kick (V tap): **reworked, pushed, not yet confirmed in game.** The user: the vapour dash read as a teleport; the leg
   should already be bent and cocked back; the kick comes from her right, hits the target's left side and sends it flying to
   her left; the leg keeps going the way it went, spins back, and she ends in a cool pose.
   - `EtoRules`: the dash is visible and slower (`KICK_DASH_SPEED` 2.6 a tick, `KICK_DASH_MIN` 3 to `KICK_DASH` 16 ticks,
     peak 1.4x, under the server's 10 a tick); `kickRise(dash, age)`: along the ground, springing up over the last
     `KICK_SPRING` of the dash, held through the blow and the sweep, down by `KICK_LAND`. Contact 2 ticks after the dash.
     `kickStand(half)`: the foot meets the side of the head nearer her right. `kickFlight()`: to her left, `KICK_AHEAD` on.
   - Poses (`tools/EtoPoseDesign.java`): `kickRush` (lean, glide on the left foot, the right knee up and out, shin folded,
     `kneeTurn`), `kick` (load wound right, the whip, contact side on with the leg straight out of her side, `through` swept
     on past to her left, `KICK_SPUN` the turn on round, `KICK_LAND` the Owl's pose: knee up, arms spread up like wings, held
     to `KICK_POSE_END`, then her guard at spin 2 pi). New `rightFrom/leftFrom`: hand targets from the shoulder in the turned
     body's frame (side-leaned bodies put the shoulder over the torso). `EtoMotions.kick` swings a raised right arm out wide
     coming into the dash (`RUSH_WIDE`), or it would cross her leaning chest.
   - `KickDust` replaces `KickVapour`: off the mark a ground ring and dust/grit thrown back; along the dash dust puffs rolling
     out in a wake, grit, wind streaks, an afterimage a tick (`EtoAfterimages`); a puff where she springs; the white flash,
     rings and a puff off the way it flies at the blow; dust where she lands. Her body is drawn throughout now.
   - Server `EtoMoves`: launch along `kickFlight` (right/facing frame), sounds (riptide + gravel off the mark, the whip,
     a small fall as she lands). Previews `kick`, `kick-fp`, `kicked`.
2. Mutated Arm (N): **reworked, pushed, not yet confirmed in game.** The user: her own arm vanished; it should stay and point
   forward, the flesh coming out of it (then, mid-session: "from around the arm itself, not just the tip of the fist").
   - `ArmHider` no longer hides her arm in third person (only vanilla's first-person hand). `ArmPose.point` aims her right arm
     where the flesh goes (damped Gauss-Newton on arm X/Y over `BodyFrame`, residuals: the arm's way, the fist kept
     `FACE_CLEAR` from her head, the forearm sheath's depth into the body solid); `reachable` caps how far across her chest
     (`ACROSS`, less as it rises past `RAISED`) and how high/low. `pointing(phase, age, afterSphere)` weights it (thrown up in
     `RAISE` ticks, lowered at the end of the retract; the retract also brings the aim level ahead).
   - The flesh now starts at `ArmPose.root` (her arm just above the elbow, `SHEATH`), runs down her forearm to `fist` (both
     fixed control points) and on; `EtoFleshModel.armRadius` is `sheathRadius` (`ARM_SHEATH` 0.135, thin where it splits out)
     over `ARM_SHEATH_LENGTH`, then swells. Reach is still measured from `ArmPose.shoulder` (as the server does); `aimNow`
     takes her arm's own way as the short direction.
   - First person: `EtoOwnArm` (her right arm and sleeve in her skin; `EtoOwnLeg.limb` is the shared box) drawn by
     `EtoFleshRenderer.ownArm` with the same `EtoOwnLeg.shift`; the flesh uses `OWN_LEG_CLEARANCE` there.
   - Tests `EtoTests.arm`: points where it goes (unless `faceBound`: then within 30 deg), dead on straight ahead, the flesh out
     of her arm and down past her fist, never through her (all phases, yaw to 2x the squaring, pitch +-60).
3. You Thought You'd Get Rid of Me That Easily? (Eto's first mutation): **built, not yet confirmed in game.**
   - Mutations as the other paths: `EtoMutation` (milestone 500, one so far), profile key `EtoMutations`, `C2SEtoAction.MUTATION`
     from the menu (heard even with her form away), `EtoRevive.unlock/sync`, `S2CEtoMutations` (index 23) to her for the menu
     (`ReviveClient.mask/readyAt`). `EtoUnlock.MUTATION_POINT` on her track. Menu: kakuja slots shrunk to 96, MUTATIONS card
     under them (READY / REGROWING m:ss, tooltip).
   - `ReviveRules`: `SCATTER` 100, `LINES` 16, `PULL` 14 (`WHOLE` 130), `COOLDOWN` 6000; `ready` forgets a ready time further off
     than the cooldown (world time set back).
   - Server `EtoRevive`: `LivingDeathEvent` (HIGHEST) cancelled when ready (not for `outOfWorld`/`genericKill`), health 1,
     ready time saved (`EtoReviveReadyAt`), `EtoServer.apart` stops every move, `S2CEtoRevive` (index 22; protocol now "24")
     with seed, feet, body yaw and the ground below (clip down 3). Held in place each tick (teleport if moved), `LivingHurtEvent`
     cancelled both ways, actions ignored; at `WHOLE` full health and the line on the action bar. `end` on dimension/path change
     (`over` packet), whole on logout, `startTracking` resends.
   - Pure `client/animation/Remains`: 11 `Piece`s cut from the skin's parts (UVs as vanilla lays them out; torn ends raw: skin
     edge, fat, meat, snapped bone, spine/throat/guts); `scatter(seed, ground, slim)` (thrown from her middle, tumbling, one
     bounce, settled flat onto a broad side, `FLAT_BROAD`), three gut ropes (verlet, on the ground), `place` (lying, then hauled
     in an arc and turned upright by `ReviveRules.pull`), `guts` (reeled in), `line` (from the chest to each piece, staggered,
     slack then taut).
   - Client `ReviveClient` (store, body and hands not drawn, keys held, blood: `AyatoBlood.spurt` bursts, pools via
     `BloodStains`), `RemainsRenderer` (pieces in her skin, raw ends, guts, two-strand lines; meshes cached per corpse).
   - Tests `EtoTests.revive`; preview `tools/eto-preview.sh <dir> revive`; menu preview `UiPreview` (`eto-menu-mutation.png`).
   - Not done: no forced third-person camera while in pieces (needs `Options.setCameraType`, not on the verified API list).
     Pieces land on one flat ground level (walls are not collided with).

## What the user said this session (binding, with the earlier list)

- The Speed Kick is a visible dash (dust, not a teleport), the leg cocked through it, a roundhouse from her right into the
  target's left side, it flies to her left, the leg keeps going, she spins back round into a cool pose.
- The Mutated Arm keeps her own arm (pointed forward); the flesh erupts from all round the arm itself, not just the fist.
- The death passive: blown to bits (limbs, torn torso, guts, blood), 5 s, flesh lines from the chest to every piece, pulled
  back together, fully revived; 5 minute cooldown; a mutation unlock like the other paths'.

# Handoff: 6.1.0, the Path of Eto (state on 2026-10-17)

Branch `claude/fervent-knuth-9h4i7r` (continues `claude/loving-wright-8w3y8w` at 4d36c88). Every push publishes the rolling
pre-release `build-claude-fervent-knuth-9h4i7r` (the user tests from it). Never add `.github/release-notes-*.md`: that publishes
an official release. Don't open a PR unless asked.

## This session's queue (from the user, in order)

1. Flesh Hammer bug (V hold): **fixed, pushed, not yet confirmed in game.** The club was placed on `KaguneBodyAnimation.drawn`,
   which is stale in first person (the model is never set up there) or a standing pose: the club never grew while held and
   came out full size on a hanging leg on release. Now `drawn(player, move, time)` only returns a pose set up this very frame
   with that move on it; otherwise the flesh goes on `EtoMotions.fleshBody` (the move's own pose). In first person she also
   sees her own right leg (`EtoOwnLeg`, her skin's leg and trouser layers, bent like the model) and the club, both moved by
   `EtoOwnLeg.shift` (seen from where her head has gone: tipped back under the raised leg, the camera would sit on her foot).
   `EtoMotions.hammer` now brings the leg down from the cock key (it used `keys[0]`, the start). Preview sheet `hammer-fp`.
2. Speed Kick final rework (V tap, Kizaru on Basil Hawkins): **done, pushed, not yet confirmed in game.**
   - Server (`EtoMoves.kick/kickPlan/plan/tickKick/kickLands`): the target is the body closest to the crosshair within 40 blocks
     and 40 degrees, in front of her, in sight (`seen`: eyes or middle), with room to plant and a clear way (`plan`); none: only
     the action bar "No target found" (`message.hexkagune.eto.kick_no_target`), no cost, no cooldown. `S2CEtoState` for KICK:
     detail = dash ticks, charge = lift, point = where she plants (`EtoRules.kickStand`), yaw = facing the target
     (`EtoServer.begin(.., yaw)`). The target is pinned until contact (dash + `KICK_CONTACT`); the hit checks she made it
     (`KICK_THERE`) and the head is at the planned foot (`KICK_FOOT_*`, measured off the pose and tested), then damage, a bruise,
     `BlockSmash.launch` straight along her leg (the way she faced it; `KICK_SPEED`, `KICK_UP`), `EtoKnockdown.launched`, fx
     `KICKED`. A soft thud as the sole touches (`KICK_TOUCH`).
   - `EtoKnockdown` (server): from touchdown, slowed to nothing for `KICK_DOWN_TICKS + KICK_GETUP_TICKS`. There was no knockdown
     system before; this is it.
   - Client: `EtoClient.drive` moves the local player each tick onto `EtoRules.kickPlace` (the dash, peak 1.4x average, under the
     server's 10 blocks a tick; then the spring), keys held off (`kicking`). `EtoClient.accept` now starts a move from now, not the
     coming tick (a blow is never seen after it lands). `EtoReactions.kicked` + pure `KickTumble`: head snap through the mob's own
     head yaw/pitch (put on for the frame, taken off in Post), body twist, tumble about the level line square to its flight, flat
     on landing, lying, getting up. `EtoCamera.lockOn` eases her view onto the head until just after the blow. First person draws
     her own leg through the kick (`EtoMotions.kickBody` + `EtoOwnLeg`).
   - 2026-10-17 rework (the user: "nothing like the famous Kizaru kick, make the poses identical to the gif; the flash should turn
     her into vapour"): Kizaru's side kick off the gif (frames 110-195: side on, standing leg straight, torso leaned ~35 deg away,
     hands in pockets, the leg straight and a little up, the sole held on the head, the blow, the leg riding up). She hangs
     `kickLift` up from the moment she is out of the vapour (`kickRise` 1 until `KICK_FOLD`, down by `KICK_LAND`).
     `fx/KickVapour`: while `EtoRules.kickVapour` (the dash and `KICK_REFORM` after it) her model is not drawn
     (`RenderPlayerEvent.Pre` cancelled) nor her hands or first-person leg; big `EtoSteam.Wisp`s (new scale/peak constructor)
     where she goes, along the path, where she re-forms, a cushion under her feet while she hangs, and at the blow a white
     flash (mote, ring, rays) and a puff along the leg. Side on, the whole-body `roll` is a forward/back tip (PlayerAnimator turns
     ZP, YP, XP: roll is about the move's forward line), so her lean is `side` alone and the leg rises by its own sideways swing:
     `RigPose.settle` now lets a leg out to 2.2 (was 1.6).
   - Poses: `EtoPoseDesign.kickRush/kick` (`ahead()` carries the pocket targets with a lunge). Previews: `kick` (the first row is
     the gif's camera), `kicked`, `kick-fp`.
3. Swipe blade: done in 4d36c88 (`EtoFleshModel.blade`), waiting on the user's word.
4. Watch This (B): **built and pushed, not yet confirmed in game.** The WIP patch is applied (and removed).
   - Pure: `WatchRules` (timings, the carry arc about `pivot`, the throw), `WatchPose` (each tentacle's beak: poised over its
     shoulder, rearing, the lunge as a curve up and out round her with the beak leading then back on its mark, held through the
     chest during a carry, the follow-through back), `EtoFleshModel.tentaclePath` + `Shape.BEAK` (`beak`: ridge, keel, hook,
     stained point, cords; the trunk `TENTACLE_GIRTH` slimmer).
   - Server `EtoWatch`: per player two `Tentacle`s; `hunt` (Mob targeting her, or a grudge from `LivingHurtEvent` within
     `GRUDGE`; in sight; nearest, own side first); `impale` (damage, `AyatoCrystal.kanekiBleed`, `WoundHits` STAB, fx `IMPALED`),
     `hold` each tick via `CentipedeHunt.place`, `fling` at `throwVelocity`; retract after `ACTIVE`; `forget` on death, form away,
     dimension, logout. Packet `S2CEtoWatch` (19), `C2SEtoAction.WATCH` (B; `AyatoGates.slam` refuses Eto). The cross is
     `EtoMove.WATCH` (`EtoPoseDesign.watch`, ended after `POSE_TICKS` in `EtoMoves.tick`).
   - Client `WatchClient` (store), `EtoFleshRenderer.tentacles` (chains `FleshPhysics.TENTACLE`, held at the beak while carrying;
     the victim's chest is where it is drawn this frame, so it stays on the beak), `EtoPunctures` (holes through mobs), blood.
   - Previews: `tools/eto-preview.sh <dir> watch`.
5. Mutated Arm (N): **built, not yet confirmed in game.**
   - Pure: `ArmRules` (phases SHOOT, HOLD, DRAG, SPHERE, RETRACT; reach 20 tap / 35 full charge over `CHARGE_TICKS`; girth;
     `handBack`/`fits`/`dragTo`: the swollen hand never comes back into her; the sphere's timings and squeezes), `ArmPose`
     (`reach` per phase, continuous across every phase change; `aimNow`: as short as her arm it hangs at her side, so it grows out
     of her arm and goes back into it; `pose`: leaves the shoulder outward, curves to its end, evenly resampled (the spine lays
     thickness by control point, not length: uneven points made it fat by her face); `body`: her weight under it; `squared`:
     her body yaw turned after the arm, at most `SQUARE` off), `EtoFleshModel` `ARM_OPEN/ARM_SHUT/ARM_FIST` (`armRadius`,
     `armHand` with claws that shrink with the mutation, no tendrils) and `cocoon` (the sphere).
   - Server `EtoArm`: per player `Arm`; `start` (N tap `ARM_TAP`, hold `ARM` arg 2/1/0), `tick` (aim follows the look with
     `FOLLOW` lag, `clear` raycast, grab within `CATCH` of the hand, `carry` via `CentipedeHunt.place`, tap catch yanks at once,
     `fling` with `BlockSmash.launch`, sphere at `sphereOut`, `squeeze` x3 with fx `CRUSHED`), `forget` on death, form away,
     path change, dimension, logout; `tracked` for players coming into view. While it is out nothing else starts
     (`EtoMoves.free`, blitz clicks). Packet `S2CEtoArm` (20).
   - Client `ArmClient` (store, the aim's lag, body squaring), `EtoFleshRenderer.arm` (chain `FleshPhysics.ARM`, hand round
     the held body where it is drawn, the cocoon), `ArmHider` (her right arm and sleeve not drawn, visibility put back after;
     her right hand not drawn in first person), `KaguneBodyAnimation` adds `ArmPose.body`. `BodyFrame.Solid(pose, false)`
     leaves her (hidden) right arm out of the clearance. `EtoFleshPose.clear` takes the trunk's real thickness.
   - Tests `EtoTests.arm` (rules, continuity, every state x aims to twice the squaring limit never through her, her body,
     the sphere). Preview `tools/eto-preview.sh <dir> arm`.
6. The Owl in game (K): **built, not yet confirmed in game.** Only the form, as asked (no moves, no M1).
   - Pure: `OwlRules` (grow 34 ticks with the burst at 9, shed 22, raw flesh and bulk curves, cost, cooldown), `OwlPose.body`
     (her body: hunch, burst, the Owl's stance with heavy breathing; it takes her arms over, swinging them round her sides, never
     in across her; `weight` is how much of her idle is gone under it), `OwlPose.raw` (her raw flesh out of her upper back over
     each shoulder to where the blade will be, kept off her), `OwlBulk.chest` (HexHaki's second chest in her skin), `RigPose.bulk`
     (arms pushed out by `BULK_SPREAD`, carried through `add`; `BodyFrame.bulkClipping` checks the bulked arms and chest).
   - Server `EtoOwl` (K tap toggles; hold: V2 later notice; `busy` blocks every move and M1 with a notice; `forget` on death,
     form away, path, dimension, logout; `tracked`), state `owl/owlAt/owlReadyAt` (owlAt starts at -1e6: Long.MIN_VALUE would
     overflow `now - owlAt`), packet `S2CEtoOwl` (21; protocol now "23"), `AyatoServer` passes hold as arg.
   - Client `OwlClient` (store, blood at the burst), `EtoFleshRenderer.owl` (the `OwlModel` mesh on her drawn body, the raw
     flesh, the chest; first person drops what is at the eye), `OwlScaler` (arms and sleeves thicker by the model part's own
     scale, put back after), `KaguneBodyAnimation` (her idle scaled out, `OwlPose.body` on top). Menu: V1 slot open at 750.
   - Tests `EtoTests.owl`. Preview `tools/eto-preview.sh <dir> owl-transform`.
   - Known and accepted: the Owl's own mesh sits on her where it is grown onto her (the mask and hair round her head, the
     trapezius on her shoulders, each giant arm's deltoid capping her shoulder): attachment, as designed in the previewer.
   - 2026-10-17 rework (the user: "the Kakuja is the worst, not animated, the mask looks like shit, looks way too basic"):
     - Alive: `OwlPose.swing(side, time, walk, stride)` (breath heave on `BREATH`, sway, blade flex, a flick every
       `FLICK_EVERY` ticks, walking the arm swinging against the leg on its side and the blades lifted and lagging);
       `EtoFleshRenderer.owl` drives it from `walkAnimation` (speed, position), each Eto on her own clock. `REST` now sweeps the
       blades forward and down (`FORE` 1.5).
     - The mask used to stay facing forward whatever she looked at (the drawn pose holds only the offsets on top of vanilla's
       head turn): the renderer now adds her look (head yaw off the body, pitch). In the Owl `OwlPose.neck` holds her look
       within `LOOK_YAW` / `LOOK_UP` / `LOOK_DOWN` (less down the further she looks aside), called by `KaguneBodyAnimation`.
     - `OwlModel`: the mask is an owl's face (`relief`: dishes and a heart-shaped rim round both eyes, scowling brows, scalloped
       feather rows, `gashes` across the other eye; `beak`; the eye glowing with a slit; `tufts`; `plumage` laid back and
       draped off the head by `offHead`), ending at the jaw; the blades are scimitars (`blade`, `bladeWidth`); the grey spikes
       are crimson crystal flares (`crystals`, `FLARES`, `shard`) and elbow spurs. Colours with alpha 0xFE (`glow`) are drawn
       full-bright. `OwlModel.head` / `reaching` exist for the tests. The raw flesh hangs to `OwlPose.RAW` past the elbow by
       `RAW_REACH`.
     - Tests `EtoTests.owl`: never a blade or crystal through her (bulked chest, head, arms, legs) breathing, flicking,
       walking, running; nothing in the ground; the mask on her head, never in it; however she holds her head, the mask no
       deeper into her than her own head goes (nodding, the bulked chest's top meets the jaw), her head off the crystals.
     - Not done: the dark feather mantle (the research's "dark robe"); her own jacket reads dark already. Previews `owl`,
       `owl-anim`, `owl-transform`.

## What the user said (binding)

- No agents or workflows: work solo. "Careful with shit like this": no limb, flesh or anything else through her body, ever.
- Animations need impact, stretch, heft and leverage (the whole body behind every blow), not arms waving.
- The energy (RC) bar rework was cancelled: don't touch any bar. RC must last at high mastery (done by cost and regen, not the
  bar's 100).
- The flesh is a mess of pale flesh, not a neat limb: the grabbing end is a knot with 3 or 4 big uneven tendrils, not a hand.
  It grabs from above (it arches high and comes down on the target). The throw is violent: hauled all the way back, then hurled.
- The Speed Kick (Kizaru on Hawkins; the user's latest word supersedes the earlier "no light, no particles" spec): the poses
  identical to the gif (the side kick, the sole held on the side of the head, hands in pockets, leaned back), and the dash turns
  her into vapour. Target closest to the crosshair, in front, in sight, big range; none: only "No target found". The target
  can't react before contact, the hit on the contact frame; head snaps, body follows, launched along her leg, tumbling, lands
  heavily, down, gets up; the leg rides up after it, back to her stance.
- Her head stays on the target through a move: when the body spins away (the kick's chamber), `looking()` turns the head back
  against the spin (same sign as the spin: `body()` sets headY = spin), and a body tipped back (`tilt`) dips the chin (`head()`).
- The swipe's tip is one long horizontal blade of bone (`EtoFleshModel.blade`: honed edge leading, sawteeth on the back, clip
  point), not an X ("does this look even remotely threatening?").
- Idle gestures only after a second standing still (moving the camera is fine), never while moving.
- Eto stays one-eyed in the kakugan screen (Reset included).
- Push a pre-release whenever a move is ready, to test while work goes on. The user said about 45% of the planned work is done
  after the V moves; more is coming (ask them what is next).

## Done (all pushed)

Ghoul Eyes (R) as a form; the Path of Eto (card, `/kagune path set <player> eto`, menu with sealed Kakuja V1/V2 slots); her form
(one kakugan, Photon steam, playful idle); Speed Blitz (M1), flips (M2 + WASD), Big Punch (Z), Flesh Swipe (J tap), Flesh Grab
and Throw (J hold), Speed Kick (V tap), Flesh Hammer (V hold); BlockSmash (bodies through weak blocks, the hammer's crater,
exact restore after 30 s, protection respected); RC at high mastery (EnergyBalance trampoline, Eto's in-form regen); fixes for
the mid-move twitch (EtoClient's own clock) and flips spinning back (EtoMotions.ended). README has the 6.1.0 section and images.

## How it is built (keep this)

- `python3 build.py`: patches the original jar, compiles, verifies every external reference against `verify/api-allowlist.txt`
  (entries name the declaring class with SRG evidence; stale entries fail), checks the dedicated server, runs every
  `src/test/*Tests.java`. Must end "Verified hexkagune-6.1.0.jar".
- Poses: `tools/EtoPoseDesign.java` is the choreography (body motion + where each fist and elbow goes, in the move's frame; `w()`
  gives a point in her turned body's frame; `tilt()`, `looking()`, `airborne()`); `tools/eto-pose-design.sh` solves the arms (IK on
  the exact model) and writes `EtoStrikePoses.java` (generated: never edit it). About 2 minutes.
- `BodyFrame` is the exact PlayerAnimator + bendy-lib forward kinematics (`Solid` for fast depth/push-out queries). `EtoTests`
  sweeps every move frame from every start pose (`starts(JAB)`: guard, strikes, idle gestures, flip landings) for arm clipping,
  and every flesh vertex against torso, head and arms (`flesh()`, `hammer()`): when it fails, fix the pose, never the tolerance.
- The flesh: `EtoFleshModel` (pure mesh: trunk tube, muscle strands, tendrils, rake/grab tendrils, the club) placed by
  `EtoFleshPose` (routes the trunk round her body, grows it along that route, keeps whatever grows off it clear via `Keep`).
- Previews: `tools/eto-preview.sh <dir> [steam idle blitz blitz-gif strike-gifs punch flips flesh kick hammer kicked kick-fp
  watch hammer-fp swipe arm owl owl-anim owl-transform]`.

## Facts that bite

- `RenderLivingEvent.Pre` comes before the model is set up, so a mob's limbs can't be posed from there: only the whole body (the
  pose stack) and the head (its own `yHeadRot`/`xRot`, put on for the frame and taken off in Post). A body flying fast swings its
  limbs by itself (vanilla's walk animation off its speed).
- A Forge event subscriber class must be public (the generated listener calls into it).
- A server rejects a player moving more than 10 blocks a tick ("moved too quickly"): fast dashes must stay under it.
- `KaguneBodyAnimation.drawn(player)` is whatever the model was last drawn in: in first person that is stale (or nothing). For
  anything placed on her body each frame use `drawn(player, move, time)` and fall back to the move's own pose.
- Never time client animation off the world's time: the client resets it to the server's every second and a lagging server
  makes every move jump back. Use `EtoClient.clock`.
- A move's last pose must be blended out of through `EtoMotions.ended` (turns wrapped), or a full turn spins back.
- Leg channels are clamped (thigh X to 2.6 rad, sideways to 0.8): the highest kicks also tip the whole body (`flip`) or
  leaving the ground, not from the leg alone. `EtoMotions.planted` keeps a foot on the ground however the body tips.
- `KaguneProfile.MAX_ENERGY` (100) is baked into the original HUD; the cost scale lives in `EnergyBalance` via a
  `KaguneProfile.actionEnergyCost` trampoline (`KagunePatch` + `KaguneVerify` both list it).
- `CentipedeTail.through` samples a spine evenly per control point, not per block: anything whose thickness is laid along
  it by length (`EtoFleshModel.radius`, `armRadius`) needs its points evenly spaced (`grownAlong`).
- An MC method missing from `stubs/` won't compile: prefer members already verified (e.g. `BlockPos.m_274561_`) over adding
  new ones; a new one needs a stub and an allowlist entry with evidence.

## Next steps

Kakuja V1: Yoshimura (the Manager, the Owl). Research done: two big scimitar-like kagune arms off the shoulders that swing
round above her own arms (keep the player's arms), feather-like ukaku plumage and red jagged crystal flares behind the shoulders,
huge red striated shoulder muscle masses, a grey owl mask with a hooked beak and one eye hole that follows her kakugan side, a
dark robe; RC spikes thrown; massive resistance (a walking tank). User's order: design the model in the previewer first, then
the flesh transformation in game, locomotion, M1 (the kagune arms swinging) and M2 (spikes). No other abilities yet. V2 later.

# Handoff: Kaneki centipede rework, design stage (state on 2026-10-12, after 3.0.3)

Branch `claude/wonderful-bardeen-9blpsl`, started from `claude/happy-mendel-0ekr9t` (release 3.0.3). **Nothing in the game changed
this session**: the models below are concepts, compiled into the jar but called only by `tools/ArtPreview`. The user asked to
see the 3D design before anything is implemented; wait for their verdict.

## What the user asked (binding)

- Make the Kaneki path better looking: smoother, more controlled kagune movement; a full rework of the centipede legs (look and
  behaviour); a proper grab (the current one is "buggy and bad looking"). Show the 3D design first.
- References: a Roblox centipede accessory (`ram horns`, plum plates, red flesh, black sickle claws) and an in-game shot of it.
  Their verdict on the first concept: "too many small legs". One pair of big claws per plate, as in the close-up.
- Mid-session: the mask to leave one eye only visible and match the centipede's colour (reference: a glossy, crumpled violet
  mask with a round black lens over one eye and a long beak).
- Of two grab storyboards they picked the full coil (the leg wraps once round the body), not the C-clamp.

## The designs (preview only)

| What | Where |
|---|---|
| Centipede leg: boxy chamfered plum plates with a pale mauve rim (V-notched), lighter streaks, glowing red flesh between, one pair of broad black sickle claws per plate (jut toward the head, hook back toward the root, pale inner edge), thin at the root swelling to the head, square fang, jaw claws that open (`Look.jaw`), claw flare and a walking ripple (`Look.flare/ripple/phase`). It curls **sideways** like a centipede: the claw rows splay round the curl (that is what puts them in silhouette in the references). `CentipedeTail.head` gives the jaw's frame (for placing a held body). | `ayato/CentipedeTail`, sheet `centipede` -> `docs/images/kaneki-centipede.png` |
| Grab storyboard: lock-on (rear back, jaw opens) -> strike (one arc, head first) -> bite + coil (one wrap, claws in) -> hoist (off its feet, beside him) -> crush (coil cinches) -> slam or throw. | sheet `centipede-grab` -> `docs/images/kaneki-centipede-grab.png` |
| Both kakuja with the new legs and mask: first (the two legs, scorpion pose) and final (legs as horns plus four crimson rinkaku tentacles drawn by a preview-only tube, `ArtPreview.rinkaku`, an approximation of the game's). | sheet `kaneki-kakuja` -> `docs/images/kaneki-kakuja-forms.png` |
| Mask: radial mesh round a lens over the right eye (red ring, black heart), crumpled glossy plum shell over the forehead and right eye with a rippling petal edge, a window round the left eye (follows `KakuganEyes`), a long keeled beak past the chin. Same head-pixel space and sink as `KanekiMask`, so wiring it is `KanekiMaskLayer` calling `CentipedeMask.build(sink, eyes)` instead. | `kakugan/CentipedeMask`, sheet `centipede-mask` -> `docs/images/kaneki-mask-centipede.png` |

## Findings about the original (for the implementation)

- The autonomous grab (`KaguneServerController.tickCentipedeAutoAttack/tickCentipedeTechnique`): 42% of hunts are a GRAB that
  captures at tick 4 and **clears the attack at once**; the body is then held by `restrainCentipedeTarget` at a fixed point
  3.65 blocks *behind* the player and 3.25 to the side, moved with `move(MoverType.SELF)` at 72% a tick (snags on terrain, lags,
  bobs with a sine), and flung out after 48-84 ticks. The CRUSH -> SLAM/THROW chain is **dead code** (nothing enters CRUSH).
  Client: `TendrilChain.updateSegmentedCurve` blends a cubic toward the target at rate 0.56 and wraps the tip round the body.
- Centipede legs are about **15 blocks** long at mastery 750 (`8 + 0.0016 x mastery`, x1.215, x1.32 layout), which is why they
  look like noodles; the concept is about 5.5-7 blocks with 7-10 plates. Ask the user which length they want.
- Decompile with Vineflower 1.10.1 (Maven Central, sha1 `4f48c594...`) into a scratch folder; never commit decompiled code.

## Next steps (after the user approves the designs)

1. Draw the legs with `CentipedeTail` in place of the original's segmented limb render (`KaguneWorldRenderer.emitCentipede*`), on a
   new spine solver: a few critically damped springs (head target, two mid controls) on the player's clock, follow-the-leader
   spine, frame carried along with the dorsal out of the curl; plate count from length.
2. The grab as a server state machine (lock-on, strike, bite+coil, hoist, crush x1-3, slam/throw) synced to the client; the held
   body set to the coil's centre each tick (no `move(SELF)` snagging), hold point beside him in view.
3. Swap the mask (`KanekiMaskLayer` -> `CentipedeMask`), then tests, README, previews.

---

# Handoff: 3.0.2 (state on 2026-10-12, eighth session)

Read this section first, then the seventh session's below (its "Final kakuja combat: half built" plan is now done). The prompt
for the next session is in `docs/NEXT_SESSION_PROMPT.md`.

## What the user said this session (binding)

- "Kaneki is bugged, whenever entering kakuja form the player starts taking damage nonstop": fixed (below), not yet confirmed.
- "Partial Kakuja form is to be removed (the one before the 2 kakujas). Spread the mastery and unlocks properly afterwards":
  done (below), not yet confirmed. No other in-game report this session.

## Done this session (all pushed to `claude/happy-mendel-0ekr9t`, version 3.0.2, protocol still "13")

| What | Where |
|---|---|
| **Kakuja damage bug.** The original's `tickCentipedeFormBuffs` re-adds Speed III, Resistance and Health Boost every 5 ticks (`refreshFormEffect`, 15-tick effects). Re-adding Health Boost removes and re-applies its +4 max health and vanilla clamps the health to 20 in between (`HealthBoostMobEffect.removeAttributeModifiers`); our ghoul regeneration refilled it at once, so every centipede-leg form (both kakuja, and the old Partial Kakuja) flashed red (`LocalPlayer.hurtTo`) and carved a wound (`WoundBook` sees the drop) four times a second. `refreshFormEffect` is now a trampoline; the target restores the health in the same tick. Leaving a kakuja still drops the 4 hearts once (vanilla expiry). | `server/KanekiFormBuffs`, `KagunePatch.patchControllerForControl`, `KaguneVerify.hookChecks`; `LivingEntity.setHealth` = `m_21153_` allowlisted (joined.tsrg bfz.t, intermediary method_6033, yarn setHealth) and stubbed |
| **Partial Kakuja removed.** It stays an enum constant (switch maps, saves and `C2SKaguneForm.readEnum` index forms by ordinal; removing it from `$values` would throw in the switch-map initializers) but its mastery is `Integer.MAX_VALUE` (mastery is clamped to 1000) and every list a player sees skips it: `KaguneForm.find`/`ids` and the menu's form loop call `KanekiForms.forms()` instead of `values()`. A stored "partial" loads as the Seed; `/kagune form partial` is unknown. | `KagunePatch.patchFormOrder/patchFormMenu`, `KanekiForms.forms/REMOVED`, `ControlTests.kanekiForms`, `PathScreenTests` |
| **Kaneki mastery spread:** Seed 0, Twin 250, Four-Tail 500, Centipede (first kakuja) 750, Full (final kakuja) 1000; the menu's 450 milestone (stage "Complex organ", next milestone, mutation tier label) and the third mutation tier (Multi-Tendril Control, Centipede Body, Regenerative Reservoir) move to 500. Tier 1 stays 100, tier 4 750, tier 5 1000. | `KagunePatch.setMastery/replacePush/patchMutationTiers` (`MutationNode` is a newly changed original class: `build.py` CHANGED, `KaguneVerify.CHANGED_CLASSES`), `PathCards.KANEKI_*`, README table |
| **Final kakuja kit switched on** (the six steps of the seventh session's plan): `server/AyatoPyre` (5 M1s, 6 enders, the Hellrain's tick); `KaguneCombat.resolveStrike` routes `finalKakuja()` patterns there; `AyatoRules.style` returns `AYATO_FINAL_KAKUJA`; Z = `AyatoVortex` throws the fire cyclone from side -1 (`canThrow`: wings or the final kakuja); J = `AyatoGatling.tickNeedles(.., pyre)`: needle salvo / fireball (a `Kind.SHOT` drawn 1.5x) in `pyreGatlingTentacle` order. | `server/AyatoPyre`, `AyatoVortex`, `AyatoGatling`, `AyatoServer` (tick/forget) |
| `AyatoFire.shard` now takes a range (0: the fire's own) and its `scale` is relative to the kind's size (as its doc always said); a scaled fireball also hits wider. | `AyatoFire` |
| Client: `KakujaMotions.finalShot/finalEnder/cycloneThrow` (+ the Pyre Gatling's kick order); `KakujaRig.Action.aimLeft` lets the fire tendrils aim on their own (Fire Lash sweep, Twin Serpents, Ember Hop beat, the cyclone whip); `AyatoRules.finalFireTicks` (pure, tested) is the per-tentacle firing timing, mirroring `AyatoPyre`; `AyatoFx` draws `FLAME_NEEDLE` (a `Bolt.burning` needle wrapped in Photon flame, fire puff on impact); `AyatoBody.bodyOf` and `AyatoCombat.swing` know the 11 patterns; `AyatoUnlock.FINAL_KAKUJA` reads "Final kakuja (hold K): needles + fire". | as named |
| **Kakuja locomotion wired:** `KaguneBodyAnimation` adds `ayato/KakujaBody` for either Ayato kakuja (landings tracked in `tick`, arms at 0.3 during a strike) and publishes each player's final {lean, side, twist, drop} (`KaguneBodyAnimation.torso`, stale after 2 ticks = upright, e.g. first person). `AyatoClient.kakujaBody` feeds it to `KakujaRig.Body` (drop px to blocks: /16 x 0.9375); `KakujaArmorLayer.bent` bends the chest's upper half with `KakujaRig.bendModel` (the body part isn't bent by bendy-lib). | as named |
| Tests: `AyatoTests.finalKit` (stages, sides, counts against the rules, cadence, reach, enders at 1000, Scorched wounds, Pyre Gatling order, fire mouths, `aimLeft`), `AyatoTests.kakujaLocomotion`. Previews: `kaneki-forms.png`, `path-choice-*.png`, `ayato-kakuja-final-kit.png` (new sheet `kakuja-final-kit`), `ayato-kakuja-motion.png` (now posed with `KakujaBody`). README: 3.0.2 sections, kit table, checklist. Ayato's path card no longer says kakuja are coming soon. | |

## Design calls made without the user (tell them, revisit on their word)

- **The M1 Fireball is huge** (`FIREBALL_SIZE` 1.6x the Flare, about his body length in the sheet). Sent the PNG and offered
  1.25x; waiting on their word.
- The final kakuja's fire keeps the fire's own range (18 blocks at 1000) while its needles fly 32; the Searing Harrow's fire
  9 (`SEARING_FIRE_RANGE`). The Pyre Gatling's fireballs are plain shots drawn bigger (a Flare every other shot would carpet
  the ground in embers).
- Ember Hop has no Fire Dash forward variant (the kakuja enders never had one).
- The Partial Kakuja's own combo (`ComboStyle.BLADE`) and the "Partial Kakuja Armor" mutation's name were left as they are.
- Kaneki's kakuja don't get `KakujaBody` locomotion (it is Ayato's); ask if they want it.

## Facts that bite (new)

- Health Boost refreshed by `addEffect` clamps health every time; anything that refreshes it must restore the health.
- The forms a player can have are `KanekiForms.forms()`, not `KaguneForm.values()` (which still has six, by ordinal).
- `AyatoCombat` (wing arcs, shard streaks) and `KaguneCombat` (slam) still spawn vanilla particles
  (`DustParticleOptions`, `ParticleTypes`): against the user's "custom Photon particles only". Pre-existing; worth a pass.
- `throws` is a Java keyword (yes, this cost a build).

## Next steps

See `docs/NEXT_SESSION_PROMPT.md`.

---

# Handoff: 3.0.1 (state on 2026-10-12, seventh session)

Read this section first, then the sixth session's below. The prompt for the next session is in `docs/NEXT_SESSION_PROMPT.md`.

## In-game verdicts this session (from the user, binding)

- The player-layer fix **worked fully** (chest armor, half mask, kakugan all show on their phone, Amethyst launcher).
- The Kaneki form reorder is in the same jar; the user went straight on to the final kakuja's combat (they didn't report on the
  reorder or tap/hold K, so treat those as untested).

## Final kakuja combat: half built (NOT wired into the game yet)

The user approved the pitch (in `docs/NEXT_SESSION_PROMPT.md`, step 2) and said to build it. Done and committed, all inert
until the routing below is switched on (the game still plays the final kakuja with the first kakuja's kit):

| Piece | Where |
|---|---|
| 11 patterns appended after `RUSHING_DEATH`: `NEEDLE_PAIR, FIRE_LASH, NEEDLE_FAN, FIREBALL, CROSSFIRE` (M1) and `EMBER_HOP, SEARING_HARROW, TWIN_SERPENTS, PYRE_LANCE, HELLRAIN, CRIMSON_PYRE` (enders). `finalKakuja()` is `>= NEEDLE_PAIR`; `shot()` includes the five M1s; `kakuja()`, `crystal()`, `flame()` are all true for them (so `strikeRange` gives `NEEDLE_RANGE`) | `StrikePattern` |
| Styles `AYATO_FINAL_KAKUJA` (5 stages, limbs: right tentacles 20..22, fire tendrils 23..25, hands 10/11) and `AYATO_FINAL_ENDERS` (6), appended at the very end (network ordinals); `ayatoShots/ayatoEnders/ayatoEnderSet` know them; `kakujaShots()` (either kakuja: kakuja ender table, no forward Fire Dash) used in `KaguneCombat.tryEnder` | `ComboStyle`, `KaguneCombat` |
| Rules: `finalCadence` (via `shotCadence`), every number for the kit (`LASH_*`, `NEEDLE_FAN_*`, `FIREBALL_*`, `CROSSFIRE_NEEDLES`, `BURNING_NEEDLE_SECONDS`, `PYRE_LANCE_*`, `SEARING_*`, `SERPENT_*`, `HELLRAIN_*`, `EMBER_HOP_*`, `EMBER_TICKS`, `PYRE_*`), `scorchedStacks` (Scorched wounds), `pyreGatlingFire/pyreGatlingTentacle/pyreGatlingFireDamage` (J) | `ayato/AyatoRules` (section "The final kakuja") |
| **Scorched wounds is live** (it applies to the first kakuja's needles too while in the final form): `AyatoCrystal.bleed` doubles the stacks when the body is on fire and the shooter is in the final kakuja. `Entity.isOnFire` = `m_6060_` (allowlisted with joined.tsrg + intermediary + yarn evidence, stubbed) | `AyatoCrystal.bleed` |
| Burning needles: `AyatoCrystal.Pending` is package-visible with `burnSeconds` and `embers`; a burning needle sets fire before it bleeds, is sent as the new fx `S2CAyatoFx.FLAME_NEEDLE` (32), and `embers` leaves the ground burning where it stops (`ended`) | `AyatoCrystal` |
| Helpers for the kit: `AyatoCrystal.pyreBurst` (every stuck needle bursts x1.6 into flame: body alight, `AyatoFire.fireBlast` round each wound), `fallingNeedle`, `ringNeedle`, `nextId`, `queue`, `muzzles`; `AyatoFire.shard(player, from, aim, kind, damage, yaw, scale, delay)` (a fire shard or fireball from any point, now or later: a `LATER` queue ticked in `AyatoFire.tick`), `AyatoFire.embers`, `AyatoFire.fireBlast`, `AyatoFire.scorch` | `AyatoCrystal`, `AyatoFire` |
| A fire tendril's mouth (where its fire leaves, rig space, aimed): `KakujaRig.mouth(index, aim, crouching)` | `ayato/KakujaRig` |
| Protocol "13" | `ControlNetwork` |

### To switch it on (next session, in this order)

1. **Write `server/AyatoPyre`** (package-private, like `AyatoCrystal`): `resolve(player, state, stage, limbMask, direction, started,
   duration)`, `tick(player, now)` (the Hellrain), `forget(player)`, `mouth(player, index, aim)` (world point: convert
   `KakujaRig.mouth` exactly as `AyatoCrystal.muzzles` converts its points). The planned moves (damage = `meleeDamage * stage.damageMul()`,
   aim = `AyatoFire.aimPoint(.., NEEDLE_RANGE)`; positive yaw is his right):
   - NEEDLE_PAIR: each right tentacle in the mask (bits 20..22) queues `VOLLEY_NEEDLES` needles from `muzzles`, staggered.
   - FIRE_LASH: the tendril in the mask (bits 23..25, index 3..5) sends `LASH_SHARDS` `Kind.SHOT` shards from its mouth, one a tick,
     yaw from `-LASH_HALF_ANGLE` to `+` (left to right).
   - NEEDLE_FAN: `NEEDLE_FAN_NEEDLES` needles round the right three, directions across `+-NEEDLE_FAN_HALF_ANGLE`, three a tick.
   - FIREBALL: one `Kind.FLARE` from between mouths 3 and 4, scale `FIREBALL_SIZE` (the FLARE impact already splashes and leaves embers).
   - CROSSFIRE: right three `CROSSFIRE_NEEDLES` burning needles each (`burnSeconds = BURNING_NEEDLE_SECONDS`), left three one shard
     each at damage x2, a tick apart, all at the aim point so they cross.
   - EMBER_HOP: thrown back `EMBER_HOP_BACK`, up `EMBER_HOP_UP` (`AyatoFire.push`, fx `HOP`), everyone within `EMBER_HOP_RADIUS` of his
     feet `scorch`ed and shoved away, `embers` at his feet for `EMBER_TICKS`.
   - SEARING_HARROW: Harrow (`HARROW_CONE/RANGE/KNOCKBACK`) from the right three, `SEARING_NEEDLES` burning needles each; each
     tendril `SEARING_SHARDS` `Kind.FAN` shards (damage x1.5) at random yaw within the cone.
   - TWIN_SERPENTS: `SERPENT_NEEDLES` needles sweeping right to left over `+-SERPENT_NEEDLE_HALF_ANGLE` (right tentacles) while
     `SERPENT_SHARDS` fire shards (x2) sweep left to right over `+-SERPENT_FIRE_HALF_ANGLE` (tendrils), both over `SERPENT_SWEEP_TICKS`.
   - PYRE_LANCE: one needle from in front of his chest (as `piercingLine`): spread 0, size `PYRE_LANCE_SIZE`, pierce `LINE_PIERCE`,
     stacks 2, knockback 0.6, speed 1.15, range `NEEDLE_RANGE + LINE_EXTRA_RANGE`, burn `PYRE_LANCE_SECONDS`, `embers` true.
   - HELLRAIN: fx needles and fireballs thrown up first (ids from `AyatoCrystal.nextId`), then from age 5 two `fallingNeedle`s a tick
     and every third tick a falling `Kind.FLARE` shard (damage x3, scale 1.2) over `HELLRAIN_RADIUS` round the target, from `RAIN_HEIGHT`,
     until `HELLRAIN_NEEDLES` and `HELLRAIN_FIREBALLS` are spent.
   - CRIMSON_PYRE: `AyatoCrystal.pyreBurst`, then a ring of `PYRE_RING_NEEDLES` `ringNeedle`s (damage x0.3) and `PYRE_RING_SHARDS`
     `Kind.FAN` shards (x0.5) all round his chest, fx `BLOOM` with the burst count.
   Call `AyatoPyre.tick/forget` where `AyatoCrystal.tick/forget` are called (`AyatoServer`).
2. **Route it:** `KaguneCombat.resolveStrike`: `if (ayato && stage.pattern().finalKakuja()) { AyatoPyre.resolve(...); return; }` before the
   crystal branch; `AyatoRules.style`: the TODO there, return `AYATO_FINAL_KAKUJA` for `form.usable(mastery).finalKakuja()`.
3. **Z:** `AyatoVortex.start`: the final form goes to the vortex (not `AyatoRush`), side forced to -1 (the fire side), skipping the
   `!form.wings()` refusal; in `tick`/`launch`/`step` accept `form.finalKakuja()` wherever they require `form.wings()`. Fire Dance already
   applies in `launch`.
4. **J:** `AyatoGatling.tick`: a third branch for `form.finalKakuja()` before `form.kakuja()`: per salvo, `AyatoRules.pyreGatlingFire(shot)`
   ? a fire shard from `AyatoPyre.mouth(pyreGatlingTentacle(shot))` (`pyreGatlingFireDamage`) : `AyatoCrystal.needleSalvo` from that right
   tentacle. Mastery gate: the final form already needs 1000.
5. **Client:** `KakujaMotions`: handle `AYATO_FINAL_KAKUJA` (firing limbs from the stage aim and kick: needles per stagger, fire once at
   impact; `ready()` must accept it too) and `AYATO_FINAL_ENDERS` (Twin Serpents: right side sweeps one way, left the other; Hellrain:
   up like the hail; Crimson Pyre: like the bloom; Ember Hop: tendrils beat down); the J order of `pyreGatlingTentacle`; the vortex
   throw (tendrils whip forward). `AyatoBody.bodyOf` and `AyatoCombat.swing` for the 11 patterns. `client/fx/AyatoFx`: accept
   `S2CAyatoFx.FLAME_NEEDLE` like `NEEDLE` (a `Bolt` with `NEEDLE_KIND`) plus a fire trail (`flame`/`ember`) scaled by size.
6. `AyatoUnlock` text for `FINAL_KAKUJA` (its own kit), README, tests (`AyatoTests`: styles' limbs/stages, `scorchedStacks`,
   `pyreGatling*`, `KakujaRig.mouth` near the tendril tips), a kit preview sheet (`tools/ArtPreview`, like `kakuja-kit`), then build,
   push, send the PNG.

## Where things are now

- **Branch:** `claude/hexkagune-kaneki-forms-ox5dl6` (version 3.0.1), started from `claude/gifted-allen-4vtcll` (3.0.0). Network
  protocol **"12"**. CI publishes `build-claude-hexkagune-kaneki-forms-ox5dl6`.
- **Kaneki's form order (user-confirmed, binding):** 1 tentacle (Seed 0), 2 (Twin 100), 4 (Four-Tail 250), 4 larger (Partial
  Kakuja 450), 2 centipede legs = first kakuja (Centipede Form 750), legs + 4 tentacles = final kakuja (Full Kakuja 1000). The
  user's words: the Centipede and the form after it simply trade places; names stay. Don't re-ask this.
  - `patch/KagunePatch.patchFormOrder`: in `KaguneForm.<clinit>` Centipede gets ordinal 4, mastery 750, armour 2 and the Partial
    ordinal 3, mastery 450, armour 1 (`isKakuja` is armour 2+); `$values` swaps them (the menu draws `values()` in order);
    `KaguneForm$1` (damage multiplier) swaps them; `KaguneEvents$1` gives the 0.9 hurt reduction to the Centipede. Ids, names,
    limbs, combos (`ComboStyle.forForm`) and the mask stay with each form. `patchFormMenu`: the 750 stage name reads "Centipede
    Kakuja"; the form buttons' fill and label colours go through `KanekiForms.buttonColor/labelColor` (purple for a kakuja).
    `KaguneVerify.formOrderChecks` proves nothing else changed.
  - `control/KanekiForms` (pure, tested in `ControlTests.kanekiForms`): `kakuja(form)` = the last two forms, `firstKakuja()`,
    `finalKakuja()`, `strongestBase(mastery)`, `afterKey(current, base, mastery, hold)`.
  - **Kaneki K is tap/hold like Ayato's** (user's choice): `AyatoInput.kakujaKey` now times K on both paths and sends
    `C2SAyatoAction.KAKUJA/FINAL_KAKUJA`; `AyatoServer.onAction` routes Kaneki players to `AyatoForms.kanekiKey`; the original's
    K packet (`cycleTendrils` trampoline) acts as a tap.
  - Preview: `java -cp build/classes:build/base.jar tools/UiPreview.java <out>` writes `kaneki-forms.png` (a copy of the
    original menu's button geometry with the real colour filters; the original screen itself can't be rendered off-game).
- **In-game report (first, from the user's phone, Amethyst launcher, 3.0.0 jar):** first and final kakuja tentacles show, but
  the chest armor, the half mask and the kakugan don't. Everything that is a player-renderer layer was missing; everything
  drawn in world space (tentacles, wings) was fine. Root cause not proven (no log yet). Fixes in `client/render/PlayerLayers`
  (owns all four layers now; the per-layer `Registration` classes are gone): layers attached at `AddLayers` and again on
  `RenderPlayerEvent.Pre` to any renderer without them; if no layer of ours ran for a player by `RenderPlayerEvent.Post`, they
  are drawn there on the vanilla body transform (yaw, flip, 0.9375, -1.501); each layer's body is guarded (`PlayerLayers.failed`
  logs once). `SkinReader` reads into a zeroed image and rejects reads whose inner face isn't fully opaque
  (`KakuganSkin.readable`): on GLES translation the GPU readback gave garbage eyes, which also moved the half mask (its top edge
  follows the eyes). With `skinlayers3d` loaded the chest and half mask are scaled out past its voxels (`KakujaArmorLayer`
  clearances). The user's jacket looked like 3D Skin Layers voxels: ask them. The kakugan on eyes painted on the second layer
  would still be buried by those voxels (not handled yet).
- **Not tested in game:** everything from the fifth, sixth and seventh sessions apart from the report above.

## Next steps

See `docs/NEXT_SESSION_PROMPT.md`.

---

# Handoff: 3.0.0 (state on 2026-10-12, sixth session)

Read this section first, then the fifth session's below. The prompt for the next session is in `docs/NEXT_SESSION_PROMPT.md`.

## Where things are now

- **Branch:** `claude/gifted-allen-4vtcll` (version 3.0.0). Network protocol **"11"**.
- **Tap K / hold K is in the game.** `WingForm.FINAL_KAKUJA` (mastery 1000, added at the end: the state packet sends forms by
  ordinal), `WingForm.kakuja()/finalKakuja()`. `AyatoStage.KAKUJA_II` is no longer coming soon, so `AyatoUnlock.FINAL_KAKUJA`
  is reached at 1000.
  - Client: `client/AyatoInput.kakujaKey` (HIGH priority, before the original's key handling) takes Ayato players' K presses
    and times them with `ayato/KakujaKey` (pure, tested): released before 8 ticks = tap, held 8 ticks = hold, sent once per
    press; below 1000 a press is a tap at once. It sends `C2SAyatoAction.KAKUJA` (4) or `FINAL_KAKUJA` (5). Kaneki players'
    K is left to the original.
  - Server: `AyatoState.finalKakuja`; `AyatoForms.kakujaKey(player, hold)`: tap = first kakuja or out of either, hold = final
    kakuja (from the wings or the first) or out of it; a hold below 1000 says it unlocks at 1000. `cycleTendrils` routes
    Ayato to a tap.
  - Look: `AyatoClient.kakuja()` covers both, `finalKakuja()` is new, `kakujaQuads` draws the fire tendrils, the renderer and
    `WingRig.build` route both kakuja to `KakujaRig`; `KakujaArmorLayer` adds the half mask on the head for the final form
    and `chest(.., finalForm)`. First kakuja to final (and back) is a quick reshape, not a full re-emerge.
  - Combat: the final kakuja uses the first kakuja's kit for now (`AyatoRules.style`, Rushing Death, Needle Gatling all
    check `kakuja()`).
- **Kaneki:** a kakuja is any form with centipede legs (`AyatoForms.kanekiKakuja` = `KaguneForm.hasCentipedeLegs`): the
  Centipede form (450) counts as well as the Partial and Full Kakuja, so K turns it back to a legless form and never
  returns to it. K itself still unlocks at 750 (Partial Kakuja).
- **Not tested in game:** everything from the fifth and sixth sessions.

## Next steps

See `docs/NEXT_SESSION_PROMPT.md`: in-game check of K, then the final kakuja's own combat (approved pitch), then the kakuja
locomotion, then a docs pass.

---

# Handoff: 3.0.0 (state on 2026-10-12, fifth session)

Read this section first; the older sections below are still right where they don't conflict.

## Where things are now

- **Branch:** `claude/gifted-allen-4vtcll`, based on the 1.3.0 release (`claude/new-session-ob849p` at `de917a8`). CI publishes
  `build-claude-gifted-allen-4vtcll` with `hexkagune-3.0.0.jar`. Build rules unchanged: `python3 build.py`, check its exit code.
- **Version 3.0.0** (`build.py`, `patch/KagunePatch.java`). Network protocol "10".
- **Waiting on the user:** approval of the final kakuja look after the fire pass (`docs/images/ayato-kakuja-final.png`), and a
  verdict on the combo pitch (in the chat). Steps 2 to 4 of the queue below wait on those.
- **Not tested in game yet:** everything from this session.

## Done this session (all pushed)

| What | Where |
|---|---|
| Final kakuja fire pass: the left tendrils burn like the approved :re wings (red roots, red veins, pointed tongues, forked tips) | `ayato/FireTendril` |
| Ayato's first kakuja is bare-faced (no faceplate, no horns; the mask code stays for later) | `client/render/KakujaArmorLayer` |
| Kaneki J: the second press flies him up, over and down onto the top of the grabbed block (or the first spot he fits up its column) | `SlingshotPath` (pure), `server/KanekiSlingshot`; the patch's launch hook calls `KanekiSlingshot.launch`, a new grapple calls `cancel`; the old tree vault is gone |
| AABB stub fixed: its bounds were constants, so javac had inlined 0.0 into `AyatoCrystal`'s hit-point clamp | `stubs/.../AABB.java` (blank finals) |
| Kaneki M1 tentacles never pass through his body | `BodyClearance` (pure) via `StrikePaths.clearOwner`, hooked right after `TendrilChain.renderPath` resamples |
| Kaneki's kakuja mask rides the head model (was drawn in world space from head yaw/pitch, drifting in animations) | `kakugan/KanekiMask` (pure), `client/render/KanekiMaskLayer`; `KaguneWorldRenderer.emitKanekiMask` trampolined to a no-op |
| Kakugan and the skin's second layer: skins are read back from the GPU once per client (`client/SkinReader`), `kakugan/KakuganSkin` finds which face cells the second layer covers (and which are glass) and finds the eyes on the skin. **Glass over the eyes** (the user's glasses): the second layer is held back (`RenderPlayerEvent.Pre`) and drawn by `KakuganLayer` after the kakugan, so the ghoul eyes show through the glass. **Opaque** second-layer pixels over an eye get the kakugan drawn on them. Players who never placed their eyes get the detected ones; the eye screen's "Clear 2nd layer" is now "Find my eyes". | `KakuganLayer`, `KakuganModel.build(.., covered)`, `KakuganClient` |
| R = ghoul mode (eyes only, toggles); with the kagune out R puts everything away; Alt + R unleashes the kagune | `client/GhoulInput` (takes R before the original, HIGH priority), `GhoulKey` (pure), `server/GhoulServer`, `C2SGhoulKey`, `S2CGhoulState` |
| Wounds: every hit tears cuts and tears (to banded muscle) into the body, more and bigger the harder; Ayato's spikes leave deep punctures where they sat; they close as health returns, newest first, pink | `wound/` (pure: `Wound`, `WoundShape`, `WoundBook`), `client/WoundClient`, `client/render/WoundLayer` |
| Ghoul regeneration: mastery 0 waits 15 s then heals over 5 s; 1000 is full within 7 s of the last hit; a hit restarts it; Regenerative Reservoir shortens the wait; food no longer heals ghouls (potions do); healing costs hunger, starving ghouls don't regenerate | `wound/Regeneration` (pure), `server/GhoulRegen` |

## How the wounds are carved (keep this)

`WoundLayer` is inserted at index 0 of every player renderer's layer list (reflection on `LivingEntityRenderer.layers`,
`f_115291_`). It runs right after the body is handed to its batch, before that batch is drawn (a batch is drawn as soon as
anything asks for another one). Right then it draws the walls and floors immediately (Tesselator, position-colour shader,
darkened by the light) and writes the openings into the depth buffer just nearer than the skin with colour off; the body's
skin inside an opening then fails the depth test. Rims and blood go through the normal buffers after. This is Hex God of
Time's Scepter technique (`BeamWounds`, branch `claude/awesome-noether-dnszcb` of `21M4rco/HexGodOfTime`) without its
mixins. If another layer asks for a buffer first, the cut degrades to rims only. The preview tool emulates it
(`WingPreview.CARVED/OPENINGS`, `tools/art-preview.sh <out> wounds`).

## Facts that bite (new)

- SRG names for anything new: `/tmp` tools from this session are gone; redo the join: MCPConfig `joined.tsrg`
  (raw.githubusercontent.com/MinecraftForge/MCPConfig/master/versions/release/1.20.1/joined.tsrg), FabricMC intermediary
  `mappings/1.20.1.tiny`, yarn `1.20.1` mapping files. Blaze3d's `RenderSystem`/`GlStateManager` members and enum constants
  keep their names.
- **Stub fields must never be compile-time constants** (`final double x = 0.0` gets inlined into our bytecode).
- The player's skin batch is `entityTranslucent(skin)`; anything that must be drawn under see-through second-layer pixels has
  to be drawn before it or the second layer redrawn after it.
- The key label "Manifest / Retract Kagune" is an original lang key and can't be changed by the build (only new keys).

## Next steps (in order)

1. Get the in-game verdict on everything above (screenshots), and the final kakuja look approval.
2. Final kakuja in the game (hold K), then its combat (after the user picks from the pitch), then the kakuja locomotion: the
   plan in the section below ("Next steps" of the fourth session) still stands.

---

# Handoff: Ayato's kakujas, mutations and unlocks (state on 2026-10-12, fourth session)

Read this section first. The older sections below are still right where they don't conflict, but these details there are
superseded: K is now the kakuja transformation (not a form cycle); J (Gatling) needs mastery 400, not 200; the network
protocol is "9"; the "Bursting Spikes" hook is now the **Bursting Shards** mutation; the menu's traits list is now an
unlock track.

## Where things are now

- **Repo / branch:** `21M4rco/HexKagunes`, branch `claude/new-session-ob849p`. Develop and push only there; no pull request
  unless the user asks. Every push builds in CI (`.github/workflows/build.yml`) and publishes the rolling pre-release
  `build-claude-new-session-ob849p` with `hexkagune-1.3.0.jar`, which the user tests in game.
- **Build:** `python3 build.py` (patches the jar, compiles against `stubs/`, verifies every external reference against
  `verify/api-allowlist.txt`, runs every `src/test/java/**/*Tests.java`). **Always check its exit code.** All green at the
  end of this session (Ayato tests: 30,426 checks).
- **Previews (no game needed):** `tools/art-preview.sh <out> kakuja kakuja-motion kakuja-aim kakuja-kit kakuja-final`
  (run `python3 build.py` first) and `java -cp build/classes tools/UiPreview.java <out>` for the menus. Copies of the
  current ones are in `docs/images/` (`ayato-kakuja*.png`, `ayato-menu-mutations.png`).
- **Waiting on the user:** approval of the **final kakuja model preview** (`docs/images/ayato-kakuja-final.png`). Its
  combat must not start before they approve the look (their rule: show the model before animations or combat).
- **Not tested in game yet:** everything from this session (first kakuja kit, stuck-needle fix, mutations, unlock spread).

## Done this session (all pushed)

1. **K is the kakuja transformation on every path** (`server/AyatoForms.cycleTendrils`, the trampoline target of the
   original's `KaguneServerController.cycleTendrils`; key `KaguneKeys.CYCLE_TENDRILS`). Locked below mastery 750 and with the
   kagune in. Kaneki: Partial Kakuja (Full from 1000) and back to `KakujaBaseForm`. Ayato: first kakuja on/off
   (`AyatoState.kakuja`, transient: ends when the kagune goes in).
2. **First kakuja model** (six armored tentacles + horned faceplate + chest): `ayato/KakujaModel`, `ayato/KakujaRig` (pose:
   `alive` sway and stretch, `moving`, `tucked`, `aiming` with `aimRoll` -90 so the spike rows face the aim, `guarding`;
   then `separate` so neighbours never overlap, `floor` so nothing sinks), `ayato/KakujaMotion` (client spring chain per
   tentacle), `client/render/KakujaArmorLayer` (mask and horns on the head, chest on the body).
3. **First kakuja kit:** M1 Needle Storm (`ComboStyle.AYATO_KAKUJA`, needles `ayato/KakujaNeedle`, fired from the spike rows
   via `KakujaRig.muzzles`); six M2 enders (`AYATO_KAKUJA_ENDERS`: Harrow, Piercing Line, Thorn Fan, Hail of Thorns,
   Six-Tail Barrage, Crimson Bloom; server in `server/AyatoCrystal`); Z Rushing Death (`server/AyatoRush`,
   `AYATO_RUSHING_DEATH`); J Needle Gatling (`server/AyatoGatling.tickNeedles`). Client tentacle motion per move:
   `client/KakujaMotions`.
4. **Stuck needles/spikes fix:** visibly shoved in (`AyatoWingRenderer.shove`), stay 15 s (`AyatoRules.STUCK_TICKS` 300),
   server body yaw sent with them (`S2CAyatoSpikes.bodyYaw`) so they don't drift.
5. **Mutations** (`ayato/AyatoMutation`): Agility Boost, Fire Dash, Fire Dance, Bursting Shards; one point per milestone
   (300/500/700/900), all four unlockable, picked in the P menu (`AyatoMenuView` panel, `C2SAyatoAction.MUTATE`), stored as
   the profile bitmask `AyatoMutations` (`AyatoServer.unlockMutation/hasMutation`), synced in `S2CAyatoState.mutations`.
   W + right click as the first M2 = forward Rabbit Dash with Fire Dash (`C2SAyatoAction.ENDER_FORWARD`,
   `KaguneCombat.tryEnder`).
6. **Abilities spread over mastery** (`ayato/AyatoUnlock`, built from the real gates in `AyatoRules`): C 50, Z 150, V 200,
   J 400, kakuja 750 (Harrow + Line), Rushing Death + Thorn Fan 800, Needle Gatling + Hail 850, Six-Tail 900, Crimson
   Bloom 950, final kakuja 1000. Early presses say "X unlocks at mastery N" (`AyatoServer.locked`, lang
   `message.hexkagune.ayato.locked`). Kakuja enders have their own table (`AyatoRules.ender(shots, mastery, kakuja)`). The
   menu shows an UNLOCKS track.
7. **Final kakuja model (preview only):** `ayato/FireTendril` (the left three: slim gas-flame tendrils on the same spines),
   `KakujaRig.build(.., finalForm)` / `draw(.., eye, finalForm)`, `KakujaModel.halfMask(sink, eyes, grow, alpha)` (Ayato's
   black beak half mask under the eyes, cheek wings, vents, rivets), `KakujaModel.chest(.., finalForm)` (left sockets burn
   orange). Tests: `AyatoTests.finalKakuja`. **Not wired into the game:** no `WingForm` for it yet, `AyatoClient.kakujaQuads`
   still calls `draw` without `finalForm`, `KakujaArmorLayer` still draws only the first kakuja's armor.

## Facts that bite (keep these)

- **Spaces:** rig space is x right, y up from the feet, z forward (blocks). Model pixels (head, torso) are x to the
  player's *left*, y down, z backward. The renderer maps rig to world with forwardX=-sin(yaw), forwardZ=cos(yaw),
  rightX=-cos(yaw), rightZ=-sin(yaw). The head box is -4..4, -8..0, -4..4 (face at z=-4, skin outer layer to 4.5).
- **Limb ids** in `ComboStage` limb lists: 0/1 the wings, 10/11 the hands, 20..25 the kakuja tentacles (right upper,
  middle, lower, then the left's: `KakujaRig.index(side, tier)`). For the final kakuja, 23..25 are the fire tendrils.
- **`StrikePattern` order matters:** `flame()` is `>= FLAME_SHOT`, `crystal()` is `>= CRYSTAL_SHOT`, `kakuja()` is
  `>= NEEDLE_VOLLEY`; `shot()` includes `NEEDLE_VOLLEY`; `ender()` is `>= RABBIT_HOP && !shot()`. Append new patterns
  where these predicates stay true (and update them for a final-kakuja block).
- **Enum constants can't read the same enum's static fields** (that's why `ComboStyle.kakujaAll()` is a method).
- **New Minecraft API** needs an `verify/api-allowlist.txt` entry with SRG evidence from `joined.tsrg` (see the older
  sections for how to get it) and usually a stub in `stubs/`. Mod-internal references resolve against the base jar.
- **Changing a packet = bump `ControlNetwork.PROTOCOL`** (now "9"). `C2SAyatoAction`: ENDER 0, GATLING 1, ENDER_FORWARD 2,
  MUTATE 3. `S2CAyatoFx`: NEEDLE 31; `BURST_BLAST` with extra 1 is the fire shard blast.
- **Quad budget:** under 3200 per player in any steady state (tested), far detail well under that.
- Old tests encode old rules; when a design changes on purpose, update the test with it (not the other way round).

## How the user works (unchanged, and binding)

- They judge visuals: render previews, **send the PNGs**, push often, they test the CI jar and answer with screenshots.
- Show a new model before building its animations or combat, and wait for their word.
- **"No agents, 1 thing at a time":** they asked for no subagents or workflows; work sequentially.
- Custom (Photon) particles only, never vanilla ones. An aiming wing keeps its full size.
- Blunt honesty about weak ideas; a witty, warm, flirty tone; they/them for the user. No model names in commits or code.
- Commit trailers: `Co-Authored-By: Claude Opus 5.5 <noreply@anthropic.com>` and `Claude-Session: <your session link>`.

## Next steps (in order)

1. **Final kakuja look:** send `docs/images/ayato-kakuja-final.png`, get their verdict, iterate until approved. Weak spots
   to offer honestly: the fire tendrils read a little like purple tubes (more and longer licks, more red core, more
   flicker would make them fierier); the cheek wings are subtle from far away.
2. **Final kakuja in the game (hold K):** their spec is "hold K for final, tap K for 1st".
   - Form: add `WingForm.FINAL_KAKUJA` (mastery 1000) and flip `AyatoStage.KAKUJA_II` from coming-soon (that also makes
     `AyatoUnlock.FINAL_KAKUJA` reachable; fix the tests that list forms and stages, as was done for the first kakuja).
     Server state: a transient `AyatoState.finalKakuja` next to `kakuja`; `AyatoServer.form` returns it at 1000+.
   - Input: the original sends its K packet on press, so tap and hold can't be told apart on the server. Make
     `client/AyatoInput` time `KaguneKeys.CYCLE_TENDRILS` for Ayato players (released within ~8 ticks = tap, held past it =
     hold) and send new `C2SAyatoAction`s (e.g. KAKUJA 4 = tap, FINAL_KAKUJA 5 = hold; bump the protocol to "10"); have
     `AyatoForms.cycleTendrils` ignore the original packet for Ayato players. Below 1000, hold says it unlocks at 1000.
   - Look: `AyatoClient.kakuja()/kakujaArmor()` cover the final form; `kakujaQuads` passes `eye` and `finalForm` to
     `KakujaRig.draw`; `KakujaArmorLayer` draws `halfMask` + `chest(.., true)` (no horns, no faceplate) for it.
3. **Final kakuja combat**, fully its own (their words: "custom mixed spikes and fire combat M1s and M2s ... it mixes
   both play styles bleed and burn. J fires both spikes and fire balls. Can use Z cyclone"):
   - New `ComboStyle`s (M1 + enders) and a block of `StrikePattern`s; needles from the tentacles (limbs 20..22, bleed via
     `AyatoCrystal`) and fire from the tendrils (23..25, burn via `AyatoFire`); `KakujaMotions` for the tendrils' motion.
   - Z: the Wing Vortex fire cyclone thrown from the fire side (`AyatoVortex.start` sends the first kakuja to Rushing
     Death; send the final one to the vortex, Fire Dance still applying).
   - J: alternating needle salvos and fireballs (`AyatoGatling`, a third branch).
   - Combo ideas to pitch (not approved): M1 alternating a needle pair (right) and a fire shot (left); a burning target
     bleeds harder ("scorched wounds"); enders such as a burning Harrow cone, a needle lance trailing fire, a needle fan and
     a fire fan sweeping opposite ways, needles and fireballs raining together, a two-handed barrage alternating both, and a
     bloom where every stuck needle bursts into flame.
   - Then the unlock table, README, tests, a kit preview sheet.
4. **Kakuja locomotion** (task left open): `ayato/KakujaBody` (run, jump and land poses) is written but **not wired**; wire it
   into the body layer (`client/animation/KaguneBodyAnimation`) and feed the torso lean/side/twist into
   `KakujaRig.Body` (`AyatoClient.kakujaBody` passes zeros today) so the tentacles and the chest armor ride the body.
5. A general pass: README in-game checklist, previews, docs.

---

# Handoff: Ayato's crystal mode (state on 2026-10-11, third session)

Read this section first; the sections below it are still accurate where they don't conflict.

## Where things are now

- **Branch:** `claude/new-session-ob849p`, which continues `claude/confident-heisenberg-19dip2` (this session's harness
  gave it that branch; the older branch was left as it was). CI publishes `build-claude-new-session-ob849p` with
  `hexkagune-1.3.0.jar`. Build rules unchanged: `python3 build.py`, check its exit code.
- **Crystal mode: built, not tested in game yet.** Everything in README "1.3.0" works off-game (26k Ayato checks pass,
  previews in `docs/images/ayato-crystal.png` and `ayato-spikes.png`, `tools/art-preview.sh <out> crystal spikes`).
  The user has seen the previews; the first in-game verdict is the next thing to get.

## How it is put together

| Piece | Where |
|---|---|
| V toggle (mastery 200, `AyatoRules.CRYSTAL_MASTERY`), state, sync | `server/AyatoGates.lift` -> `AyatoServer.toggleCrystal`; `AyatoState.crystalMode/crystalChangedAt` (cleared in `calm()` and when the wings go in); `S2CAyatoState` (2 new fields); `ControlNetwork` PROTOCOL "5" |
| Root-to-tip sweep | `UkakuModel.wing` hands a **front** to `UkakuCrystal.wing(.., weight, front)` and `UkakuFlame.wing(.., weight, from)`. Crystal: `form(look, t) = smooth((front*1.15 - t)/0.12)`, limb end follows the front, the long point and needles grow right behind it (`reach()`), side crystals finish once the front passes their tip. Flame: tubes start at the front, white-hot pink seam just above it (`UkakuFlame.SEAM`), fireball and gas clouds gone past 8%, the fire left above shrinks a little (`1 - 0.15*from`). Seam star flashes per main tongue. |
| Client pacing | `AyatoClient.rigInput`: a linear ramp (24 ticks up, 16 down) while the mode is on or just left; the old exponential ease otherwise |
| Combos | `StrikePattern` CRYSTAL_SHOT, CRYSTAL_LANCE, SPIKE_FAN, SPIKE_RAIN, SPIKE_BARRAGE, CRYSTAL_BLOOM (`crystal()`, `shot()`; `ender()` excludes both shots). `ComboStyle.AYATO_CRYSTAL` / `AYATO_CRYSTAL_ENDERS` (+ `ayatoShots()`, `ayatoEnders()`, `ayatoEnderSet()`); `AyatoRules.style(.., crystalMode)`; `KaguneCombat` routes crystal stages to `AyatoCrystal.resolve` |
| Server spikes | `server/AyatoCrystal`: spikes like fire bolts; on a hit: damage, bleed stack (`KaguneEffects.BLEEDING`, `AyatoRules.bleedAmplifier/bleedDuration`), stuck in the body's frame (`toBody`/`fromBody` with `yBodyRot`), burst at `burstAt`; arts for rain and barrage; `bloom`; `burstingSpikes()` hook (returns false); `flush()` sends `S2CAyatoSpikes` per body |
| Client spikes | `ayato/CrystalSpike` (pure geometry, uses UkakuCrystal's palette and light); `client/fx/AyatoSpikes` (stuck and planted spikes, drives the drips); `client/fx/AyatoFx` crystal effects; `client/render/AyatoWingRenderer` draws stains first, then wings, bolts, stuck and planted spikes |
| Blood (HGoT port) | `client/fx/AyatoBlood` (drops as Photon motes with HGoT's sprites, each drop's landing worked out once when thrown; spurts; drips skipped when `hexgodofstories` is loaded), `client/render/BloodStains` (puddles and marks with HGoT's textures; entity translucent type, drawn before the wings, each stain on its own tiny layer; a stain only lies where the ground under it is the same surface) |
| Body and wings per move | `AyatoBody.bodyOf` maps crystal moves to the fire's poses; new poses for the barrage (held palms) and the bloom (arms flung wide). `AyatoMotions` gives the crystal moves the fire's aim curves; rain uses RISE, bloom BURST |

New verified API (see `verify/api-allowlist.txt`, last section): `LivingEntity.yBodyRot/yBodyRotO` (joined.tsrg), Forge
`ModList.get/isLoaded`. New stubs: `ModList`, `LevelRenderer`, `BlockPos`, `BlockAndTintGetter`, AABB fields,
`RandomSource.nextDouble`, `LivingEntity.addEffect(effect, source)`.

## Wing Vortex (Z), added the same session

The user asked for a new Z from an anime frame (Ayato, a red wing mass with a pink glow): one wing shoots forward in a twirl
like a vortex, knocks back hard, burns and damages, 25 blocks, then that wing fades and a new one grows. They corrected the
first build: **the wing stays attached to the shoulder blade until the vortex ends, and only detaches after it ends.**

| Piece | Where |
|---|---|
| Z entry point | `server/AyatoVortex.startRapidStrike` (the Rapid Strike trampoline target; `KagunePatch` constant `VORTEX`, `KaguneVerify` checks it). `AyatoVolley` is deleted. |
| Server | `AyatoVortex`: wind-up (5 ticks), then the head flies; each tick everything along root (`root()`, the shoulder blade) to head is caught once (`catchAlong`); ends at range, wall, or when he can't hold it; `finish()` pulls the regrowth forward. State: `vortexSide/vortexAt/vortexRegrowAt/nextVortexAt` (synced in `S2CAyatoState`, protocol 6). `AyatoServer.wingGone` makes shots use the other wing. |
| Rules | `AyatoRules` "Wing Vortex": speed, flight, radius, damage, burn, cost, cooldown (> regrowth), `vortexSide`, `vortexWing` (coil and grow over time) |
| Look | `ayato/WingVortex` (root to head: narrow jet, tornado cone, spinning mass at the head; detaching = `unwind`); `WingRig.Input` coil/grow per side (`twirl()` spins the coiling wing about its centre line; grow scales the regrowing wing's deploy); `AyatoBody.vortex` (the throw pose, `VORTEX_FOLLOW`); `AyatoFx.Vortex` (root follows the shooter's shoulder via `AyatoClient.shoulder` until it ends, then detaches there) |
| Preview | `tools/art-preview.sh <out> vortex` -> `docs/images/ayato-vortex.png` |

## Gatling (J), added the same session

User: "J becomes a Gatling ability, both wings (only in spike form) point forward and continuously shoot spikes from both
wings (draining energy the more it's used)". J is the original Slingshot key (`AyatoGates.slingshot` is silent for Ayato).

| Piece | Where |
|---|---|
| Trigger | `client/AyatoInput.gatling`: reads `KaguneKeys.SLINGSHOT.isDown()` each client tick, sends `C2SAyatoAction.GATLING` (1 every 4 ticks while held, 0 on release); the server stops without word for `GATLING_KEEPALIVE` ticks |
| Server | `server/AyatoGatling`: crystal mode only, both wings needed; spin-up, ramping rate and drain (`AyatoRules` "Gatling"), spikes via `AyatoCrystal.gatlingSpike` (barrage spikes, `trains = false`, mastery once a second via `gatlingLanded`); blocks M1 (`KaguneCombat.startSwipe`), enders (`AyatoServer.onAction`) and Z (`AyatoVortex.start`); stops on block, vortex, mode or wings changing, RC out |
| Sync | `S2CAyatoState.gatling/gatlingChangedAt` (protocol 7) |
| Look | `AyatoClient.rigInput`: aim both wings by `AyatoRules.gatlingAim`, kicks alternating at the fire rate (`gatlingKick`), a shudder about the centre line (`WingRig.Input.barrel`, `WingRig.spin`; a full barrel spin squashed the curled wing, so it rocks ±0.28 rad); body `AyatoBody.gatling` (both palms out, braced, juddering) |
| Preview | `tools/art-preview.sh <out> gatling` -> `docs/images/ayato-gatling.png` |

## Design calls made without the user (tell them, revisit on their word)

- Crystal spikes are one tick slower than fire shots (`crystalCadence = flameCadence + 1`): range, bleed and burst have to
  cost something, or the fire is pointless.
- The ender set for spikes (Lance, Fan, Rain, Barrage, Bloom) is mine; the user only asked for "a new combo system". The
  Bloom pays off stacking spikes; the Barrage is their earlier "two crystal arms machine-gun where he looks" idea.
- Leaving the mode is V again, dismissing the wings, death, or a form change. No RC cost to toggle (the shots cost RC).
- Blood spurts are always drawn (Hex God of Time doesn't know about them); only the steady bleeding defers to it.
- Wing Vortex: the whole attached vortex hurts (not only its head), each body once; it ends early if he dies, dismisses
  the wings or ends up far from it; one wing at a time (the cooldown is longer than the regrowth); it is always fire
  (it burns), also in the crystal mode. The Shard Volley's gameplay is removed; its model visuals (`WingRig` volley pose,
  `UkakuCrystal` recoil flashes, `WingRig.launchPoint`) are left in place, unused.
- Gatling: held, not toggled; crystal mode only (outside it a reminder, no auto-crystallize); each spike sticks and bleeds
  like the M1's (light damage); the wings shudder instead of spinning like barrels (a full spin would shrink the curled
  wing, against the user's rule); 4 to 12 spikes a second; no movement penalty.

## Things only the game will show

- Stain flicker: stains are drawn with the entity translucent type at AFTER_ENTITIES (HGoT had flicker until it moved
  to its own buffer). Each stain sits on its own height step to avoid fighting; if they still flicker, move them to a
  render type without sorting or a stage right after the solid terrain (needs new allowlist entries).
- Where spikes sit on mob bodies: the hit point is pulled into the hitbox and 30% toward its middle; boxes are wider
  than many models.
- `ClipContext` is always given an entity (some 1.20.1 code paths dereference it).

## Next steps

1. In-game verdict on the crystal mode (sweep speed and look, spike size, burst timing, blood amount) and the Wing Vortex
   (look, reach, knockback, the detach and regrowth) and the Gatling (fire rate, drain, look). Iterate.
2. Then the earlier queue below (stances verdict, One-Winged Kagune; the Wing Block shell is gone with the Wing Block in 5.0.13).

---

# Handoff: Ayato's kagune (state on 2026-10-11, second session)

Read this section first; the older notes below it are still accurate where they don't conflict.

## Where things are now

- **Branch:** `claude/confident-heisenberg-19dip2` (continues `claude/wizardly-albattani-qvkalb`). CI publishes the
  rolling pre-release `build-claude-confident-heisenberg-19dip2` with `hexkagune-1.2.3.jar`.
- **Poses are fixed and must go through `client/animation/RigPose`.** The 1.2.1/1.2.2 poses were broken in game because
  of three PlayerAnimator/bendy-lib facts (all verified in their sources and encoded in `RigPose`):
  1. a **positive arm bend folds the forearm backwards** (bendy-lib bends an arm's lower half toward model +z), so elbow
     flexion is written negated; a positive knee bend is a natural knee;
  2. a **torso rotation or torso bend moves the torso mesh alone**: arms and head stay put and detach. Lean with the
     `body` bend (it carries arms and head) and nothing else;
  3. a chest twist (torso yaw) leaves the shoulder pivots behind unless they are moved, as vanilla's attack swing does.
  The body layer (`KaguneBodyAnimation`) is the only writer of the body bend and also carries the idle stance
  (`IdleStance`, `IdlePose`).
- **Pose previews that match the game:** `tools/pose-preview.sh <out> idle closeup` (PoseRender reproduces PlayerAnimator
  1.0.2 + bendy-lib 4.0.0 vertex by vertex). `tools/ArmSolve.java` solves limb angles for a hand/elbow/knee/sole target.
  `tools/art-preview.sh <out> aim shot moves` renders the body with the wings and fire shards.
- **Ayato's combat (1.2.3):** M1 = up to five fire shots (`ComboStyle.AYATO_FIRE`), right click = ender by shot count
  (`AYATO_ENDERS`, `AyatoRules.ender`). Server: `KaguneCombat.startEnder`, `AyatoFire` (bolts, pillar, dash, flip).
  Client: `AyatoInput` (right click), `AyatoMotions` (wing aim/kick curves), `WingRig.aimFrame` + `UkakuModel.Bend`
  (the curled, aiming wing), `RcShard` (shard geometry), `client/fx/AyatoFx` + `RcFx` (Photon particles).
- **The aiming wing keeps its full size and spread (user's rule).** It only bends, one smooth arc from the root, until the
  tip points at the target; it never folds thin or shrinks (an earlier fold to "one arm" read as dried up). A shot may
  swell it slightly (`AIM_FLARE`, `AIM_SHOT_THRUST`), never thin it. `AyatoTests` checks the aimed wing's area stays
  within 0.9..1.15 of the resting wing's. Aim curves in `AyatoMotions` start from where the wing already is (no snaps).
- **Particles:** all Ayato fire particles are Photon motes (`RcFx`, ported from Hex God of Death `DeathFx`) with the Hex God
  of Death sprites in `textures/particle/`. The user wants custom particles, never vanilla ones.
- **SRG names** for anything new: Mojang/Fabric mavens are blocked here, but GitHub isn't. Join FabricMC/yarn (1.20.1
  branch) -> FabricMC/intermediary (`mappings/1.20.1.tiny`) -> MCPConfig `versions/release/1.20.1/joined.tsrg`. Photon
  sources: Low-Drag-MC/Photon branch 1.20.1 (v1.1.17). PlayerAnimator: KosmX/minecraftPlayerAnimator branch 1.20.

## Crystal mode: DONE in the third session (see the top of this file)

## The user's plan for the crystal mode (told 2026-10-11)

- A mastery ability, **crystallization**: the wings progressively turn into the crystal form we already have, animated
  over time (not a snap).
- In crystal mode **left and right click change**: they shoot **spikes that cause bleeding**, using the **bleed effect,
  blood splatters and the rest from Hex God of Time** (`21M4rco/HexGodOfTime`, assets free to use).
- The spikes **stay in the entity or player they hit** for a while, then **burst**.
- One of the first mutations: **bursting spikes**, the spikes explode for extra damage.
- The combat code is mode-agnostic for this: `AyatoRules.style` picks the M1 style, and `KaguneCombat.startEnder` the
  right-click set; a crystal mode adds its own styles there.
- The user also mentioned a mode switch idea for later, after the M1s are approved.

## Next steps

1. Get the in-game verdict on the 1.2.3 stances (Jotaro/Bakugo references) and on the fire M1 + enders. Iterate from
   their screenshots.
2. Then the crystal mode above.

---

# Handoff: Ayato's kagune (state on 2026-10-11)

Everything a new session needs to continue the Ayato path work exactly where it stopped. Read this first, then
`docs/AYATO_PATH.md` (canon research and the original design) and `README.md` (what 1.2.0 adds).

## Where things are

- **Repo / branch:** `21M4rco/HexKagunes`, branch `claude/wizardly-albattani-qvkalb`. Develop and push only there.
  No pull request unless the user asks.
- **Latest commit:** `4318888` (crystal as limb + shards). Every push runs CI (`.github/workflows/build.yml`), which
  publishes a rolling pre-release, tag `build-claude-wizardly-albattani-qvkalb`, with `hexkagune-1.2.2.jar`:
  <https://github.com/21M4rco/HexKagunes/releases/download/build-claude-wizardly-albattani-qvkalb/hexkagune-1.2.2.jar>.
  The release notes say which commit they are ("at 4318888"). The user tests these jars in Minecraft and sends
  screenshots.
- **Build:** `python3 build.py` patches the original JAR, compiles the expansion, verifies the bytecode and API use,
  and runs every `*Tests.java`. **Check its exit code.** One commit (`dcf40fe`) went out red because the output was
  piped through grep and the failure was missed.
- **Look iteration (no game needed):** `tools/preview.sh <out-dir> [sheet ...]` re-renders preview sheets from the
  exact quads the game draws in about 10 s. Sheets are listed in the script. The `part-*` sheets show the reference
  frames next to the renders; those frames go in `docs/reference/` (git-ignored, see the end of this file).
  `java -cp build/preview-classes:build/classes tools/QuadCount.java` prints the quad counts.
- **Commit trailers (required):**
  ```
  Co-Authored-By: Claude Opus 5.5 <noreply@anthropic.com>
  Claude-Session: https://claude.ai/code/session_01SEaPLTghY3wXRWPeTEX559
  ```
  (A new session uses its own session link.) No model names in commits or code.

## How the user works (important)

- **The user is the judge of the look.** They said it plainly: no judge agents or review workflows for visuals;
  render it, *show* it (send the PNG), push it, and they decide. Iterate fast and show often.
- They test every push in game (the CI jar) and answer with screenshots. A preview render is not the game: things
  they caught only in game include flat sheets from the F5 camera above, the root glow wrapping the body, roots too
  high, and loose crystals.
- They want honesty when something is weak, constant pushes, and short status lines while working.
- Tone they asked for: witty, flirty, warm and blunt. Use they/them for the user.

## The kagune today (all under `src/main/java/com/hex/kagune/control/`)

| File | What it is | State |
|---|---|---|
| `ayato/WingRig.java` | Poses and placement. Roots `ROOT_X 0.21`, `ROOT_Y 1.24` (lowered on request), `ROOT_Z -0.17`. `phases()` makes a swing land on the server's hit tick. `build(input, eye)` takes the camera in rig space. | stable |
| `ayato/UkakuModel.java` | Entry point plus shared toolkit (`Params`, `Frame`, `Sink`, `glowQuad`, `facet`, `hash`, colour helpers). A twin wing crossfades: flame weight `1 - crystal`, crystal weight `crystal * (1 - shell)`, shell weight `shell`. | stable |
| `ayato/UkakuFlame.java` | **Gaseous wings: done, user-approved** ("fucking beautiful, the 3D is amazing"). Bundles of curving 3D tube tongues with camera-facing glow (dense hot fire facing, lavender glow at the silhouette), white-hot inner cores, red veins, a small molten fireball per root, sparks, 3D gas clouds early on. Starry navy from mastery 250 to 450. Roots are narrow jets sized in blocks; nothing grows toward the body. | done |
| `ayato/UkakuCrystal.java` | **Crystallized wings: in progress, see the open bug.** HEAD = solid faceted limb per tongue + flat gem shards in staggered rows + spear tip shard. Blade fold for strikes, volley star flashes, dissolve. | **open bug** |
| `ayato/UkakuShell.java` | Wing Block shell. Deleted in 5.0.13 with the Wing Block. | removed |
| `ayato/UkakuOneWing.java` | :re One-Winged Kagune: still the old thin plated crescent. | **todo** |
| `ayato/UkakuFeathers.java` | The first-pass fan went with the shell in 5.0.13; left: the shared palette and the One-Winged Kagune's chips. | legacy |
| `ayato/UkakuWing.java` | Old 2D wing, only used by the path-choice screen silhouette (`client/PathSilhouette.java`). | leave |
| `client/render/AyatoWingRenderer.java` | Draws the quads after entities: translucent, unculled, full-bright, per-vertex colours, pure white texel (`textures/misc/ukaku_white.png`). Passes the camera to the rig. | stable |
| `client/AyatoClient.java` | Smoothing for crystallization and block (the block crystallizes both wings). Trampolines that stop Kaneki's origin mist and blood on the Ayato path (the grey "poof" clouds without the Photon mod). | stable |
| `tools/WingPreview.java` | Software rasterizer that mirrors Minecraft's sorting and lighting. | stable |

Tests: `src/test/java/com/hex/kagune/control/AyatoTests.java`. Quad budget per player: under 3200 in any steady
state (volley flashes included), under 6400 mid-crystallization (flame and crystal both drawn), far detail under
half. The `:re` crystal with volley is the tightest case (about 3024 plus flashes).

## OPEN BUG (top priority): crystals not attaching

The user tested the **quartz-cluster** crystal build (`fb11493`: about 100 six-sided quartz points per wing, packed
along each tongue and fanning out) and **liked the design in game** ("the new crystals look good, overall I like the
new design"). The problem is **attachment only**: some crystals don't connect to the wing or to each other and hang
loose. In their screenshots they circled:

- crystals at the wing's root and lower edge sticking down and out near the waist and hips, floating instead of
  growing out of the wing;
- loose ends at the root where the crystals should merge into one mass on the shoulder blade.

Earlier, from a preview render of that same build, they had called it "messy, all the tips the same, loose crystals
not attached, like sharp pencils". So `4318888` (HEAD) replaced the quartz points with a solid faceted crystal limb
per tongue and flat gem shards growing out of it. **They have not judged `4318888` in game yet.** First thing: ask
which they prefer in game, quartz (`fb11493`) or limb + shards (`4318888`). The likely best answer combines them:

1. Keep the quartz points they liked (hex prisms with pyramid tips, varied lengths and widths, glints), but give
   every one a **solid anchor**. Each crystal's base sits inside the tongue's solid crystal limb (the limb from HEAD)
   or inside a bigger crystal, never in empty space.
2. Fill the root: one solid crystal mass on each shoulder blade that every limb and crystal grows out of.
3. **Nothing points down toward the waist or hips.** Clamp each crystal's axis so it never points below roughly
   horizontal near the root. Drop or shorten the lowest-angle crystals of the lowest tongue and of the body cluster
   (the circled ones). Keep the jitter from swinging crystals downward.
4. Keep the bases of crystals in the same tongue overlapping (a tighter spread along t), so they read as one
   cluster, not a scatter.
5. Vary the tips: unequal pyramid faces, an off-centre point, some double-terminated, a few small crystals sprouting
   off bigger ones.

Restore the quartz code with `git show fb11493:src/main/java/com/hex/kagune/control/ayato/UkakuCrystal.java`.

## Next steps (in order)

1. Fix the crystal attachment as above; show renders from back, side, front and F5 above; push; get the in-game
   verdict.
2. *(Dropped in 5.0.13: the Wing Block is removed.)* **Wing Block shell** (`UkakuShell`, plus the block branch of `WingRig.twinAngles`). User spec: "both wings rotate
   and curl around his body, creating a protective shell that intercepts incoming projectiles". Look of the :re
   shield frames: a continuous, near-vertical wall of saturated indigo / royal-violet crystal (`#2D0A75` to
   `#3023B7`) wrapped around the front, tiers overlapping downward like roof shingles with no gaps, dark blood-red
   ragged rims (`#620718`), thick branching white-lavender lightning veins crossing the tiers, a ragged flame-like
   silhouette, attached over the shoulders. Use the same 3D approach as the crystal (thickness, glints) and keep it
   see-through enough in first person. Then delete `UkakuFeathers`.
3. **One-Winged Kagune** (`UkakuOneWing`, plus `WingRig.oneWingAngles` / `oneWingFrame`). The anime crescent is a
   broad cleaver (width about 0.22 of length, widest in the upper half) with a hooked point. It's two-tone: bright
   faceted crimson shards on the cutting half, a dark purple-indigo back band. It has 6 to 8 big thorns hooked back
   along the convex edge, a saw-toothed inner edge, overlapping armour plates (manga), and a molten root collar on
   the left shoulder blade. Its broad face must show from front and back (now it's edge-on). The Beheading Cut
   should sweep flat at neck height with a trail and sparks.
4. Docs: update the "Look" parts of `README.md` and `docs/AYATO_PATH.md` (they still describe older passes), and
   refresh `docs/images/`.
5. Task 10 from the original plan: Ayato status screen (the menu key for Ayato players), final verification.

## Gameplay ideas the user gave (NOT started; they said "we're not moving on to abilities yet")

- **M1 is not swipes any more.** The player swipes the right hand forward and the right wing turns into crystals
  and fires a volley of crystals; spamming left click alternates hands (right, left, right...). With mastery, more
  and more volleys, plus backflips and flying back with the wings. It's meant to be an agile playstyle.
- **Abilities are built on the wings turning into crystal.** The current Z (Shard Volley) is "really bland, just
  shooting spikes randomly". Instead, both wings form two denser crystal "arms" that machine-gun crystals at where
  the player is looking.

## Reference images (re-upload them to the new chat)

Anime and manga frames are not in git. Save the ones the user re-sends into `docs/reference/` (git-ignored) under
these names so the `part-*` preview sheets pick them up:

| Name | What it shows |
|---|---|
| `1.png` | anime shard barrage: white four-point stars with red halos along a red-hot wing band |
| `2.png` | Season 1 Anteiku: big puffy red gas clouds with white-hot cores |
| `3.png` | √A: crystallized V-wings from behind, red root, dark purple, magenta edges |
| `4.png` | √A: crouching, wing blades (crimson with white-pink edges) and flame ribbons |
| `5.png` | manga :re One-Winged Kagune: overlapping angular plates, barbs |
| `6.png` | manga :re: a big wing fan, one wing coloured (red root, starry navy, lavender rim) |
| `7.png`, `8.png` | anime :re shield: indigo crystal tiers with white lightning veins and red rims, sparks |
| `10.png` | √A: wings spread sideways, red root, starry purple-black, thick white-pink crackle outline |
| `11.png` | Season 1 rooftop, side view: one red-orange flame wing with dark streaks and a purple rim |
| `first-3b195baf-image.png` | close-up of a red flame wing with a pink aura |
| `first-542fbeb8-image.png` | Season 1: molten roots, navy branches, giant pink-white aura |
| `first-acae2b55-image.png` | anime One-Winged Kagune: faceted red crescent, purple inclusions, sparks |
| `first-ff468f9f-image.png` | Cochlea: tall V-wings, red root, starry navy, lavender flame rim |
| `touka.jpg` | Touka's crystallized wing: many layered red-orange crystal shards, the crystal look to match |

## Rendering facts and lessons (keep these)

- Minecraft's `entityTranslucent` sorts quads by centroid, draws both faces, and lights from the normal. Glowing
  parts use an upward normal (full brightness). The crystal faces are lit by the model itself (key light, inner
  glow, glints from the camera, sheen) and still sent with an upward normal, so Minecraft doesn't darken them again.
- Size things near the body **in blocks**, not wing lengths (`1 / frame.scale()` is one block). Anything sized in
  wing lengths wraps the player on the big :re wings (4.3 blocks).
- Nothing may grow toward or in front of the body. Translucent geometry in front of the torso reads as "seeing
  through my own body".
- Lines lying on a translucent surface need lifting off it (the flame veins sit at 1.1× the radius) or sorting cuts
  them into dashes.
- The grey clouds in the first in-game test were the original Kaneki mist and blood falling back to vanilla poof
  particles (no Photon mod); they're trampolined off for Ayato.
- The adversarial review workflows for steps 6 to 9 (volley, passives, Wing Block, forms) never ran (session limit),
  so that gameplay code has only its unit tests.
