> **Superseded (2026-10-27):** the final kakujas (War and Peace, HexKagunesv2 1.0.13 to 1.0.17) are handed over in `docs/handoff/kings.md`, and the prompt to paste is `docs/handoff/kings-next-prompt.md`.

# Next session: where the work stopped (2026-10-24, session 3)

Read this file, then `docs/HANDOFF.md` and `docs/handoff/requests.md` (the user's words, binding). Session 3 worked on branch
`claude/fervent-knuth-p89n79` (started from `claude/furuta-dna-path-replacement-wja31k`, which it carries in full); start from the
newest of the two. **No agents.** Push as soon as something works. CI publishes `build-<branch>`, and the user tests that jar on a
phone through Amethyst. Render previews, look at them yourself, and send them.

**Released: HexKagune 7.0.1.** Not released since: phases 4 and 5, and the Furuta rework's first stage (below). The next version is yours to name when the user asks
for a release (bump `VERSION` in `build.py` and `patch/KagunePatch.java`, write `.github/release-notes-<VERSION>.md`).

**The progression overhaul is built, all five phases.** Nothing of phases 4 and 5 has been tried in game yet: the user's verdict
on the CI jar comes first. They saw the model sheet (`docs/images/ghoul-npc.png`) and the fight sheet
(`docs/images/ghoul-npc-fight.png`) and the talk screen (`docs/images/npc-talk.png`), but had not answered when the session ended.

## The Furuta ability rework (the user's, verbatim in `requests.md`): all five stages pushed

Going stage by stage, one push each with frame sheets: **two tails (done)**, **centipede (done)**, **four thorns (done)**,
**mass of eyes (done)**, **final kakuja (done)**. The user said not to stop between stages: go straight on. The fifteen old
abilities are deleted (their server code, Predatory Evolution's swelling, Final Calamity's eruption, the rush's boost, the limb
actions only they used: impale, coil, anchor, rise, surge); protocol "31". Nothing of the rework has been tried in game yet:
the user's verdict on the CI jar comes first.

- **Rules for every reworked ability:** stun is `EtoStun.stun` (renewed, capped at 30 ticks: refresh, never stack; it stops the
  body walking and cancels its attacks); bleeding on every Furuta blow is now `AyatoCrystal.furutaBleed` (refreshed, never
  stacked: `FurutaRules.refreshBleed`, strength by the blow's weight `FurutaRules.bleedStacks`). "Eto's Execution" ships as
  **Owl's Execution** (the no-"Eto" rule; told the user).
- **Two tails** (Rize's Kakuhou; `FurutaMove` 7.0.2 block, server `server/FurutaTails`, numbers in `FurutaRules`):
  - **Mockingbird** (Z, mastery 100): 30-tick stance, tails dropped low and coiled at his hips (`Kind.LURK`, `FurutaRig.lurk`).
    A living attacker within 6.5 blocks striking at him (`FurutaServer.attacked` -> `FurutaTails.attacked`): the blow misses, he
    slips aside to his right, `MOCKINGBIRD_COUNTER` (a follow-up: `Slot.NONE`, `followUp()`): `HOOK` (tail 0 wraps the attacker,
    holds it where it stands) and `FLANK` (tail 1 out wide, round behind it, through it toward him; 2.2 power, heavy stun). Then
    `lastStrike = LASH_3`, so the next M1 is the string's 4th blow. Unused stance: cooldown cut to 60.
  - **Owl's Execution** (J, 40): seeks prey within 11, rushes to 2.6 off it (ticks 0..5), circles it 270 degrees (5..22, his
    `state.yaw` kept on it for the server's sweeps; the prey pinned), eight cuts (each tail back and forth at new heights, one
    cut's end the next's start), then both tails through it (`BREAK`: a raised shield is disabled, `Player.m_36384_`). One tail
    alone makes every other cut.
  - **Scorpion Vault** (V, 0): needs ground behind him (`FurutaTails.ready`); at tick 2 the tails go into the ground behind him
    (small craters, anything there stabbed) and he is thrown (1.0 up, 0.55 ahead). For 60 ticks in the air, V again =
    `VAULT_DIVE`: down where he looks (never flatter than 0.45 down, `FurutaRules.diveDirection`), 2 blocks a tick, impact on
    the first body in his path or the ground: 2.0 heavy-stun bleed on it, 0.8 splash with stun within 2.8, a crater. The rig keeps
    the planted tips in the ground as he rises (`FurutaRig.vault`, `FurutaRules.vaultRise`).
  - **Human Puppet** (B, 180): tail 0 catches at up to 9 and drags it in, held out at the tail's length (3.4 ahead), stunned.
    Then: B again -> `PUPPET_THROW` (hoisted, hurled along his look, `BlockSmash.launch`); M1 -> `PUPPET_STRIKE` (tail 1 rips up
    through it, thrown up); right click -> `PUPPET_PULL` (pinned, he hauls himself in, two claws, tail 1 through it). Follow-ups
    keep the hold (`CARRY` beats skip the catch) and are committed until their last blow (`FurutaCombat.committed`).
  - Client: the renderer aims `follows()` moves at their target's live position; `FurutaPose` has the stance, slip, flank, dive,
    vault (crouch, spring, tuck) and throw. Sheet `tools/furuta-preview.sh <dir> tails tails-counter`
    (`docs/images/furuta-two-tails.png`, `docs/images/furuta-mockingbird-counter.png`): chains each into its follow-ups with the
    world moving round him. Tests `FurutaTests.tails` (+ the strike-contact test now covers hooks, flanks and every follow-up).
  - Gotcha: `CentipedeMotion.fit` lengthens a short curve by lifting its middle; a curve that dips (the stance's low coil) gets
    hoisted out of shape. Scale such a pose to the length and `resample` it instead (`FurutaRig.scaled`).

- **Centipedes** (stage 2, pushed; server `server/FurutaCentipedes`): **jobs** are new: a `FurutaMove.job()` runs alongside the move on
  its own clock (`FurutaState.job/jobAt/jobHeld`, sent in `S2CFurutaState`, protocol "30"; the rig takes `Input.job/jobAge/jobHeld`:
  a job's beat owns its limb where the move leaves it free, and always while it holds something (`CARRY`); the server skips a
  move's beat on a limb the job holds, `FurutaCentipedes.taken`; an ability that needs the mouth makes the drag let go, `yield`).
  - **Hungry Maw** (Z, 320): `Kind.CHASE`: the mouth out along a route (the move's point is the bend: `FurutaCentipedes.route`
    tries an arch, up over, round either side; re-found every 4 ticks), `FurutaRules.mawReach` further each tick, snapping at what
    its head meets every 5; at its prey it clamps -> job `MAW_DRAG` (100 ticks, `Kind.CLAMP`): carried low at his side
    (`FurutaRules.DRAG_POINT`), stunned, chewed every 20 ticks; Z again flings it.
  - **Centipede Locomotion** (J, 280, a job): `Kind.CRAWL` strides, sides in turn every 8 ticks; each stride stabs the ground
    ahead at its side (what stands there is stabbed) and hauls him along his look (`CRAWL_PUSH`); M1 strikes on the way; at its
    end (or J again) `CRAWL_BITE` (the mouth lunges).
  - **The Ambush** (V, 250): the side centipedes' light thrusts at its front, then the mouth's `UPPER` from under it (gaping, snapping
    shut; `BREAK`, heavy stun, bleed).
  - **Living Carapace** (B, 380): the side centipedes `CURL` round him (a guard; the mouth free); a blow from within 5 blocks:
    `CARAPACE_SPRING` (both `BURST` out at it, heavy stun), then M1 or B: `CARAPACE_BITE`.
  - Sheet `tools/furuta-preview.sh <dir> centipedes` (`docs/images/furuta-centipedes.png`); tests `FurutaTests.centipedes`.

- **Four thorns** (stage 3, pushed; server `server/FurutaThorns`):
  - **Crucifixion** (Z, 450): the upper pair `SEIZE` it from either side (limb 1 `CARRY`) and hold it up
    (`heldPoint`: feet off the ground); the lower pair's eight `THRUST`s in turn from new angles (bleed, pierce); at 37 the upper
    pair hurl it (`WHIP`; `FurutaThorns.crucify`, `BlockSmash.launch`).
  - **Unstoppable Breach** (J, 500, `travels()`): thrown along his look 20 ticks, walls ahead at his height broken
    (`BlockSmash.crater`, put back later), what he runs into struck (heavy stun) and thrown aside; six `THRUST`s with `CRATER` (a
    thrust that runs nothing through smashes where it lands: `FurutaCombat.thrust` already did that).
  - **Dragon's Dragnet** (V, 560): new flag `GATHER`: the lower pair's sweeps (reach 9, from 115 degrees in) catch and drag what
    they pass to `DRAGNET_POINT` (held, stunned) instead of throwing it; the upper pair `SLAM` it together at 18.
  - **Living Catapult** (B, 620): four planted round him (`FurutaRig.SPLAY`), flung (`CATAPULT_UP` 1.8: about 17 blocks); the
    flight is a job (`CATAPULT_FLIGHT`, `Kind.SOAR`: splayed, reaching down as he falls), steered toward his look; on the ground,
    `CATAPULT_LAND`: four `SLAM`s round him in turn. Jobs are no longer the centipede's alone (`job.stage()` must be the stage).
  - Sheet `tools/furuta-preview.sh <dir> thorns` (`docs/images/furuta-thorns.png`); tests `FurutaTests.thorns`.

- **Mass of eyes** (stage 4, pushed; server `server/FurutaMasses`; the crescents' look untouched, only how they move):
  - **Eye of the Storm** (Z, 700): `Kind.STORM`: each crescent wheels round him on its own path (`FurutaRules.stormAngle/Radius/
    Height`, shared by rig and server; odd ones the other way), the server cutting what each sweeps past (bleed), once a half turn.
  - **Flesh Bloom** (J, 740): `CURL` in, `BURST` out, waves of spikes at `BLOOM_WAVES` (`bloomFlare` drives the spikes and heaves the
    mass out), each wave cutting what is in reach (bleed); `CURL` in again.
  - **Black Curtain** (V, 780): `Kind.CURTAIN` (`FurutaRig.curtain`): the crescents rise as bands of a wall across his front,
    fold forward about its foot (`curtainFold`) to lie on the ground ahead, their ends curling round; while it stands, bodies
    in front are kept from slipping out round its ends; when it lands, those under it are struck (heavy stun) and pinned.
  - **The King's Game** (B, 820, a job; needs prey within 16): the crescents at it in turn from every side (`THRUST`, `FLANK`,
    `UPPER`, `SLAM`) while he moves freely; with a sword or axe, M1 rushes him in on it (`FurutaMasses.sword`), his own vanilla
    blow lands, bleeds (stacks 4), and `KINGS_STRIKE` drives a crescent in with it. The job's held body is its aim too
    (`Input.asJob`). The old evolved four and the borrowed rush/cyclone are off every key now (deleted after stage 5).
  - Sheet `tools/furuta-preview.sh <dir> eyes` (`docs/images/furuta-mass-of-eyes.png`); tests `FurutaTests.eyes`.

- **Final kakuja** (stage 5, pushed; server `server/FurutaFinal`):
  - **King's Checkmate** (Z, 900): colossus 0's frontal `THRUST`; colossus 1's `HOOK` round behind it (holds it, `BREAK`); his
    charge in (`CHECKMATE_CHARGE_FROM..TO`, to 1.8 blocks); then claws, his jaws, colossus 0 through it, a last knock-up blow.
  - **Centipede Hunt** (J, 920, `travels()`): hurled after its prey (re-steered every tick), colossus 0 biting every 6 ticks,
    colossus 1 striding (`CRAWL`); within `HUNT_CATCH`: `HUNT_DRAG` (held low at the side by a `CLAMP` `CARRY` beat, dragged as he
    runs on, chewed every 4 ticks, ground shocks), thrown at `HUNT_THROW`.
  - **Monstrous Recovery** (V, 940, a job): the colossi `CURL` round him; `RECOVERY_SHARE` of his health over 80 ticks at
    `RECOVERY_COST` RC a health (it stops when RC runs out); blows halved on top of the kakuja's own share (`FurutaFinal.taken`);
    every other key and M1 refused while it runs (V again stops it).
  - **The Final Performance** (B, 980, a job): the colossi at whatever he looks at (`FurutaCombat.aim` again every 10 ticks:
    redirected by looking), taking turns from every side, every blow bleeding; M1 is `PERF_CLAW` (a rake as fast as he clicks);
    B again or its end: `PERF_FINISH` (caught, hauled in, colossus 1 through it, his knock-up blow).
  - The renderer and the body now take a kakuja job on the colossi. Sheet `tools/furuta-preview.sh <dir> kakuja-four`
    (`docs/images/furuta-final-kakuja.png`); tests `FurutaTests.kakuja`.

- **Weak spots and PvP** (after all five, the user: "make it viable and good for pvp"):
  - Visuals: Locomotion's strides now arch out and stab in 4.2 ahead, `CRAWL_SPLAY` 1.6 out to the side (rig and server share it;
    the sheet's row is side on now, front on it foreshortened). Black Curtain's fallen wall drapes over what is under it
    (`CURTAIN_DRAPE` 2.1) instead of bodies standing through it. Monstrous Recovery's colossi wind tighter and higher
    (`KAKUJA_COCOON_RADIUS` 1.3, up to 3.3, each more than once round): a closed cocoon, not a squat barrel. The many-eyed
    cocoon is untouched.
  - PvP: a Furuta blow on a player does `PVP_SHARE` (0.45) of its damage (`FurutaCombat.strikeBody`): before armor, Checkmate's
    whole string is about 30, the Performance's finisher about 23, an M1 5 to 11.5. Stuns on players can't chain forever:
    `StunRules.PLAYER_CHAIN` (40 ticks of unbroken stun) breaks it, then `PLAYER_IMMUNE` (40) of immunity; `EtoStun.stun` returns
    false then, and every hold (puppet, hook, crucifixion, dragnet, curtain pins, both drags) lets that player go. A stunned
    player can't use Furuta, Ayato or Eto abilities or M1. The Performance's claws come at most every `PERF_CLAW_EVERY` (4)
    ticks. Tests `FurutaTests.pvp` (every move under 36 to a player before armor, jobs under 8 a second, the chain cap).

## Phase 4, ghoul NPCs (session 3: built and pushed)

- **Entity** `control/npc/GhoulNpc` (a `PathfinderMob`, `hexkagune:ghoul`, MobCategory MONSTER, 0.6 x 1.8), registered by
  `control/ControlEntities` (DeferredRegister of ENTITY_TYPES, attributes on `EntityAttributeCreationEvent`). Who it is:
  `npc/NpcIdentity` (kind RINKAKU/UKAKU, `NpcTemper`, tier 0..2, skin 0..8, name, seed) packed in one long in Forge's persistent
  data (`HexGhoulNpc`). Vanilla goals only float and look about (`FloatGoal`, `LookAtPlayerGoal`, `RandomLookAroundGoal`); its AI
  step (`m_8024_`) calls `server/GhoulNpcBrain.tick`, its drops (`m_7472_`) `GhoulNpcBrain.drop` (Ghoul Meat, `NpcRules.meat`).
  `patch/KaguneGuard` guards `m_8024_` (discard on error, like the spike), `m_8099_` and `m_7472_`.
- **Look:** vanilla's nine default skins (wide), `npc/NpcLook`; all nine have their eyes at the same place (`NpcLook.EYES`, found
  offline with `KakuganSkin.detect`, so phones never read textures back). `client/render/GhoulNpcRenderer` (a
  `HumanoidMobRenderer` with `PlayerModel`, layer `Eyes`: the kakugan as the kagune comes out; the layer also notes which NPCs were
  drawn this frame, `drawn()`). Their kagune: `FurutaRenderer` (its helpers now take any `LivingEntity`) draws rinkaku NPCs'
  transplant tails, `AyatoWingRenderer` (vertex loop factored into `emit`) ukaku NPCs' twin wings, aimed at their prey as they shoot.
- **Sync:** `network/S2CGhoulNpc` (protocol 28 then 29): identity, kagune out, the lash (FurutaMove, start, point), prey, volley,
  last shot and its wing; on change and on StartTracking. Client store `client/GhoulNpcClient` (times onto `EtoClient.clock`).
- **Tempers** (`npc/NpcTemper`): Feral (goes for anyone), Gourmet (ghouls, players and NPCs), Pack (humans and Anteiku's side;
  friendly to its own), Drifter (fights back), Scavenger (runs when losing or when a ghoul comes close), Anteiku (never first,
  except at a feral). Grudges per temper. `NpcRules`: night 13000..23000, a try every 200 ticks per player at 5%, 24..48 blocks out,
  none within 20 of a player or 72 of another NPC, at most one other within 128; tiers by the player's mastery; health 40/60/85,
  armor 4/6/8, damage 3/4/5 a unit of power (a mob attack, difficulty-scaled); regeneration; slips away at dawn unwatched.
- **Combat** (`server/GhoulNpcCombat`): rinkaku: the transplant string LASH_1..5 (3/4/5 lashes a string by tier, then circling), each
  beat on its tick (sweeps hit as the arc crosses, `FurutaCombat.smooth`, as a player's), no craters; ukaku: volleys of fire shots
  from the wing tip (`WingRig.aimTip`), flown on the server, sent as `S2CAyatoFx` BOLT/IMPACT/FIZZLE (ids from 2^30). Spawner
  `server/GhoulNpcSpawner`; `/kagune npc [rinkaku|ukaku] [temper] [tier]` spawns one five blocks ahead (operators).
- **Quests:** `GhoulKinds` recognises them; kills carry `ghoul`, `npc`, `temper:<id>`, and `hostile` only for the hostile tempers.
- Tests `NpcTests`; previews `tools/furuta-preview.sh <dir> npc npc-fight npc-scene` (fetches vanilla's skins into `build/skins`).

## Phase 5, talking (session 3: built and pushed)

- **Right click** a ghoul NPC: `server/GhoulNpcTalk.interact` (PlayerInteractEvent.EntityInteract, cancelled on both sides so
  nothing in hand is used; the client still sends the interact packet first) opens the talk if it would talk
  (`NpcTalk.talks`: not a feral, not hostile to them, no grudge, not hunting them). It stands still and faces them while they talk.
- **Pure core:** `npc/NpcTalk` (pages, the answers `Choice`, replies with a regard change and an `Effect`; every temper's own lines
  by standing, ghoul or human, side), `npc/NpcBonds` (regard -100..100: enemy <= -40 goes for them, >= 20 friendly, >= 60 friend;
  a friend forgives one stray blow, the blow that turns them is a betrayal, -60), `quest/Favours` (each temper's asks, rolled from
  its seed and the favours done for it; quests of `Line.FAVOUR`), `QuestLog` bonds (`b.<uuid>`, 48 remembered) and favours (`f.<i>`,
  3 at once) in the save string the journal already reads.
- **Answers:** who are you, talk (once a night), give the Ghoul Meat in hand (not Anteiku), a favour (offered, accept/decline),
  walk with me (an ally: from friend, tempers that would, 2 at most; follows or waits, fights what hurts or hunts its player,
  never despawns, can't be hurt by its player, catches up from far), follow/wait, go your own way (it leaves), insult (each temper
  reacts: the Pack fights, the scavenger runs).
- **Network:** `S2CNpcTalk` (a page), `C2SNpcTalk` (an answer, checked against what was offered). Protocol "29".
- **Client:** `client/NpcTalkScreen` + `client/NpcTalkView` (the house board, low on the screen; click or number keys),
  `client/NpcTalkClient`. Preview `java -cp ... UiPreview <dir> talk` (`-Dtalk.scene=<npc-scene.png> -Dtalk.skins=build/skins`).
- **Quests** (ghoul line): Among Our Own (talk to 3 new ghouls), A Friend in the Dark (befriend + a favour: a second way to the
  Speed Kick) **or** Cold Blood (betray one: closes the other, `trust` group), Errands of the Night (4 favours), Side by Side
  (an ally, and 10 hostile kills with it near: the `ally` context tag). Signals TALK (`first`), BEFRIEND (`ally`), FAVOUR, BETRAY.

## Ideas not built (offer them; the user decides)

- Masks for the hostile tempers (ghouls hunt masked in the source): a tell of danger at a glance. Art work: show a sheet first.
- Snap/hiss sounds for Furuta's mouths (needs a client sound call on the allowlist), and NPC voices.
- Hunts that ask for a temper (`temper:<id>` is on every NPC kill already).

## Progress since (session 2, same branch; newest first)

- **The progression overhaul** (the user's request, verbatim in `requests.md`, "The progression overhaul"). Five phases, each
  pushed when it works:
  1. **Pushed: every ghoul fights in eye mode.** With only the kakugan open (R; kagune away), any path fights with Owl DNA's body:
     the Speed Blitz on M1, flips on an empty right click, the Big Punch on Z, the Speed Kick on V (mastery 250). It is the Eto code
     itself: `EtoServer.eyes(player)` (not Eto, unlocked, `GhoulServer.ghoul`, kagune not out) opens `ready`; `eyesTick` runs only
     the body moves (`body`: pushes, soar, safe falls, `EtoBlitz.tick`, `EtoMoves.tick`), and the moment the eyes close or the
     kagune comes out it calms the move, broadcasts it and keeps a mid-air body safe from the fall. `eyesAction` hears only
     `STRIKE` (new `C2SEtoAction.STRIKE = 12`: the client sends M1 itself, the original only sends one with the kagune out), FLIP,
     PUNCH, KICK; `punched` turns a vanilla AttackEntityEvent in eye mode into a blitz click. Damage: `EtoServer.power` =
     `EyeRules.power(mastery)` (x0.70 at 0 to x1.00 at 1000) on the blitz, punch, kick and Witness blows; a sustained eye blitz is
     ~5/s at 0 and ~14/s at 1000 (test `EtoTests.eyes`, under the kagunes' 18..24). `EtoServer.landed` feeds the Kaneki frenzy
     only with the kagune out. Client: `EtoClient.eyes/fighter`; `EtoInput.attackInput` (M1 on a living body or the air strikes; on
     a block it mines unless a body is right by the crosshair, `EyeRules.strikesOverBlock`; a hold strikes once), `keys` at
     HIGHEST (Z and V only in eye mode, before FurutaInput eats them); `KaguneBodyAnimation.wants` and `pose` (eye mode: only
     `eyeMoves`, nothing of any kagune; a cut move blends out over the kagune); first-person arms on `fighter`. The abilities
     panel lists the kit with the eyes open. Sheet `tools/eto-preview.sh <dir> eyes` (`docs/images/eye-mode.png`).
  2. **Pushed: Ghoul Meat.** `ControlItems` (the expansion's own DeferredRegister<Item> on the mod bus, from
     `KaguneControlBootstrap.init`; Food & Drinks tab), texture `textures/item/ghoul_meat.png` drawn by hand
     (`tools/gen_ghoul_meat_texture.py`), model `models/item/ghoul_meat.json` (build.py now allows `assets/hexkagune/models/item/`),
     in `ghoul_food`. `GhoulMeat` (pure): strength = 1 + 0.4 (1 - e^(-n/40)) on every blow the ghoul lands
     (`GhoulMeatServer.hurt`, LivingHurtEvent LOW), endurance = 1 + 1.0 (1 - e^(-n/40)) dividing every RC cost and refund in
     `SharedBar` (the bar is untouched: "don't touch any bar"). Count in the profile root (`GhoulMeatEaten`), synced by
     `S2CGhoulMeat` (protocol 27) for the tooltip (`GhoulMeatClient`). A human gets nausea, poison and hunger. `/kagune ghoulmeat
     [set <n>]`. Tests `GhoulMeatTests`; sheet `docs/images/ghoul-meat.png`. SRG evidence came from MCPConfig `joined.tsrg` +
     Fabric intermediary + yarn 1.20.1, fetched from raw.githubusercontent.com (Forge/Fabric mavens and Mojang are blocked here).
  3. **Pushed: quests and progression.** Pure core in `control/quest` (tested, `QuestTests`): `Signal` + `Tags` (the event words),
     `QuestEvent`, `Objective` (all/none tags, best-of, one-life), `Reward` (mastery, points, Ghoul Meat, gates, side), `Quest`
     (line PATH/GHOUL/HUNT/FAVOUR, mastery, after, exclusive group), `Gate` (20 gates; `mutation()` gates grandfather by ownership),
     `QuestBook` (every quest; most gates have two openers; Anteiku vs the Pack are exclusive), `QuestLog` (states are computed;
     DONE/CLOSED stored; one string in the profile root, `HexQuests`), `Hunts` (rolled at dusk, rebuilt from a 5-number spec),
     `Gates` (form/stage/mutation -> gate; `furutaUsable`/`furutaNext` skip shut stages). Server `QuestServer`: KILL (victim and
     player context tags, RANGED for projectiles or kills > 6 blocks off, BOSS by id or 150+ health), DIE, EAT, DEAL/TAKE
     (LivingDamageEvent), BRINK (below 20% and alive 5 s later), RAMPAGE (kills within 200 ticks), TIME (each second in eye mode
     or kagune out); moves signal STRIKE/PUNCH/KICK/FLIP (`EtoBlitz`, `EtoMoves`) and RESTRAIN (`KaguneInstinct`). Rewards pay at
     once; hunts at dusk (60%, then a night or two off), gone at dawn. Gate checks: `QuestServer.pass/open` in `EtoMoves.kick`
     (eyes), soaring flips, `EtoWitness.has` (eyes), `AyatoForms.kanekiKey/kakujaKey`, `AyatoGatling`, `AyatoServer.unlockMutation`,
     `FurutaServer` (chosen, pickStage, kakujaKey, unlock), `KanekiMutationsServer.hasMutation`; Kaneki's menu-picked forms are
     taken back once a second if their quest isn't done (`enforceKaneki`). Client: `QuestClient` (+ U key opens `QuestScreen`,
     drawn by `QuestJournalView`), `QuestLocks` threaded into the Kaneki/Ayato/Furuta menus (protocol 27 carries `S2CQuests`).
     The user's rule from 6.2.0 forbids naming Eto, Takatsuki, Aogiri or Yoshimura anywhere a player sees (EtoTests.owlDna):
     the violent side is "the Pack". Previews: `java -cp build/classes:build/base.jar tools/UiPreview.java <dir> quests`
     (`docs/images/quests-*.png`). Commands `/kagune quests [reset|complete <id>|gate <id>|hunt]`.
  4. **Built in session 3: ghoul NPCs** (see "Phase 4" at the top).
  5. **Built in session 3: NPC dialogue** (see "Phase 5" at the top).

## Phase 4 plan (session 2's research, kept for reference; built in session 3, see the top)

What can be reused, found by reading the code:
- **Bodies:** a `PathfinderMob` drawn by a `HumanoidMobRenderer` with the vanilla `PlayerModel` (wide) and the vanilla default skins
  (`minecraft:textures/entity/player/wide/{steve,alex,ari,efe,kai,makena,noor,sunny,zuri}.png`), so no new body art is needed.
- **Eyes:** the kakugan from `KakuganModel.build(...)` drawn on the model's head exactly as `KakuganLayer.draw` does; eye places
  from `SkinReader.read(ResourceLocation)` on the NPC's skin (make it public; it falls back to `KakuganEyes.DEFAULT`).
- **Rinkaku ghouls:** tails from Furuta DNA's rig. `FurutaRenderer` is player-bound only by its parameter types: change
  `AbstractClientPlayer` to `LivingEntity` in `drawSet`, `body`, `prey` and `Frame.of` (they only use LivingEntity members; the posed
  body `RigPose` may be null, `FurutaPose.ride` handles it) and add a loop over the NPCs with a `FurutaClient.View` built from the
  NPC's state. Their M1 is the transplant string (`FurutaMove.firstStrike/nextStrike(TRANSPLANT)`: LASH_1..LASH_5, beats with land
  ticks and reach), hit on each beat's land tick by the NPC's own server code (`FurutaCombat` is ServerPlayer-bound).
- **Ukaku ghouls:** wings from `WingRig.build(new WingRig.Input(...), eye)` drawn with `AyatoWingRenderer`'s vertex loop (factor it
  into a helper). Their M1 is Ayato's fire shots: send `S2CAyatoFx` BOLT/IMPACT/FIZZLE packets (the client already draws them as
  `AyatoFx` bolts) and fly the bolt on the server as `AyatoFire.step` does (that code is ServerPlayer-bound: copy its shape for
  a mob shooter, damage `npc.m_269291_().m_269333_(npc)` = mobAttack). The original's `KaguneSpikeEntity(Level, LivingEntity owner,
  float damage, int color)` is a projectile any mob can fire too.
- **State without SynchedEntityData:** keep the NPC's kind, personality, tier, skin and name in Forge's `getPersistentData()` (the
  original uses it, so no evidence needed) and send clients a `S2CGhoulNpc` packet on change and on `PlayerEvent.StartTracking`.
- **Hooks waiting for it:** `server/GhoulKinds.ghoul(entity)` and `roaming()` return false; make them recognise the NPC so
  quests count ghoul kills and hunts may ask for one. Ghoul Meat drops on death (`spawnAtLocation`).
- **Spawner:** a server tick per player at night (day time 13000..23000), low chance, 24..48 blocks out on the surface
  (`getHeight(MOTION_BLOCKING_NO_LEAVES, x, z)`), none if another ghoul NPC is within ~72 blocks or two are within 128.
  MobCategory MONSTER so they despawn like monsters; persistent once befriended (phase 5).
- **Personalities (a start):** Feral (hostile on sight), Gourmet (hunts ghouls), Drifter (neutral, fights back), Scavenger
  (cowardly), Anteiku (friendly, peaceful), Pack (hostile to Anteiku-side players). `QuestLog.side(ANTEIKU / PACK)` is the
  player's side.
- **Names to keep out:** `EtoTests.owlDna` fails the build if any player-visible text says Eto, Takatsuki, Aogiri or Yoshimura (the
  user's 6.2.0 rule). The violent faction is "the Pack".

SRG names already resolved for it (with `tools/srg_lookup.py`; still add each to `verify/api-allowlist.txt` with its evidence, and a
stub under `stubs/`):

| Use | Declaring class (Mojang name) | SRG |
| --- | --- | --- |
| registerGoals (override) | Mob | `m_8099_ ()V` |
| goalSelector / targetSelector | Mob | `f_21345_` / `f_21346_` (GoalSelector) |
| getNavigation / getLookControl | Mob | `m_21573_` / `m_21563_` |
| removeWhenFarAway (override) | Mob | `m_6785_ (D)Z` |
| setPersistenceRequired / isPersistenceRequired | Mob | `m_21530_` / `m_21532_` |
| createMobAttributes | Mob | `m_21552_ ()Lnet/minecraft/world/entity/ai/attributes/AttributeSupplier$Builder;` |
| mobInteract (override, phase 5) | Mob | `m_6071_ (Player, InteractionHand)InteractionResult` |
| Goal canUse, canContinueToUse, start, stop, tick | Goal | `m_8036_`, `m_8045_`, `m_8056_`, `m_8041_`, `m_8037_` |
| Goal setFlags / requiresUpdateEveryTick | Goal | `m_7021_ (EnumSet)V` / `m_183429_ ()Z`; Goal$Flag is `bmv$a` |
| addGoal | GoalSelector | `m_25352_ (ILGoal;)V` |
| moveTo(entity, speed) / moveTo(x,y,z,speed) / stop / isDone | PathNavigation | `m_5624_` / `m_26519_` / `m_26573_` / `m_26571_` |
| setLookAt(entity) / (Vec3) / (x,y,z) | LookControl | `m_148051_` / `m_24964_` / `m_24946_` |
| add(attribute, value) / add(attribute) / build | AttributeSupplier$Builder | `m_22268_` / `m_22266_` / `m_22265_` |
| Attributes, in order (`bhg` a..i) | Attributes | MAX_HEALTH `f_22276_`, FOLLOW_RANGE `f_22277_`, KNOCKBACK_RESISTANCE `f_22278_`, MOVEMENT_SPEED `f_22279_`, FLYING_SPEED `f_22280_`, ATTACK_DAMAGE `f_22281_`, ATTACK_KNOCKBACK `f_22282_`, ATTACK_SPEED `f_22283_`, ARMOR `f_22284_` |
| create(level) | EntityType | `m_20615_` |
| spawnAtLocation(stack) | Entity | `m_19983_` |
| setCustomName / setCustomNameVisible | Entity | `m_6593_` / `m_20340_` |
| moveTo(x, y, z, yaw, pitch) / discard | Entity | `m_7678_` / `m_146870_` |
| mobAttack(attacker) | DamageSources | `m_269333_` |
| getHeight(type, x, z) | LevelReader | `m_6924_` (Heightmap$Types is `dhk$a`; its constants' SRG still to look up) |
| addFreshEntity | LevelWriter | `m_7967_` |
| dropCustomDeathLoot (if an override is wanted) | LivingEntity | `m_7472_` |
| HumanoidMobRenderer(Context, HumanoidModel, shadow) | HumanoidMobRenderer | `<init>` (yarn BipedEntityRenderer) |
| PlayerModel(ModelPart, slim) | PlayerModel | `<init>` |
| bakeLayer | EntityRendererProvider$Context | `m_174023_` |
| PLAYER / PLAYER_SLIM | ModelLayers | `f_171162_` / `f_171166_` |
| addLayer | LivingEntityRenderer | `m_115326_` |
| getTextureLocation (override) | EntityRenderer | `m_5478_` |
| registerEntityRenderer, getPersistentData | (Forge) | already used by the original jar: accepted |

Forge (`[forge]` evidence from MinecraftForge 1.20.1 sources on raw.githubusercontent.com): `EntityAttributeCreationEvent.put`
(mod bus), `PlayerInteractEvent.EntityInteract` (phase 5). The original registers its spike entity with a `DeferredRegister` of
ENTITY_TYPES and `EntityType$Builder` (`m_20704_` of, `m_20699_` sized, `m_20702_` clientTrackingRange, `m_20717_` updateInterval,
`m_20712_` build), all accepted; do the same from `KaguneControlBootstrap.init` as `ControlItems` does.

Before building the combat, show the user the NPC model (a preview sheet of a ghoul with its tails out and one with its wings out):
their standing rule is "show me a model before building animations or combat on it".

## Phase 5 plan (session 2's, kept for reference; built in session 3, see the top)

Right click a non-hostile ghoul (PlayerInteractEvent.EntityInteract, or `mobInteract`): a dialogue screen in the house style
(a `UiCanvas` view like `QuestJournalView`, previewable through `tools/UiPreview.java`). Lines by personality and by the player's
side; choices that change the NPC's regard for the player (kept per player in the NPC's persistent data); favours offered as
quests (`Quest.Line.FAVOUR`, a hunt-like spec rebuilt from numbers, `Signal.TALK/BEFRIEND/FAVOUR/BETRAY` are already in the enum);
befriended ghouls become allies (persistent, follow, fight hostiles) and betrayed or insulted ones turn hostile, as do Pack ghouls
to Anteiku-side players.

- **Pushed: item 5, every stage frame by frame** (`docs/images/furuta-frames-<stage>-m1.png` / `-abilities.png`, all ten
  reviewed). Every point blow lands on its hit on the landing tick (test `strikes`). Found and fixed:
  - Kakuja M1: Rake, Rake and Devouring Lunge were his own claws and jaws alone (the colossi sat still): each Rake is now
    followed by the colossus on that side snapping at it, and both colossi bite beside his jaws in the Lunge.
  - Balance after the hurt-cooldown fix (every blow now lands): at mastery 1000 the kakuja string was 40/s, Rampage 130 and
    Execution 72 (the mod's own target for a sustained M1 at 1000 is 18..24/s, `DamageBalance`). Now: kakuja string ~30/s,
    Rampage ~76, Execution ~47; the many-eyed string is ~28/s, the thorns' ~26/s, the centipedes' ~19/s, the transplant's ~11/s.
  - Preview cameras follow long reaches (Crimson Impaler's dummy in view).

- **Pushed: ferocity by behaviour (item 4).** `FurutaRig.Body.prey` (rig space; the renderer finds the nearest living thing within
  `PREY_RANGE` 10 of him once a tick, `FurutaRenderer.prey`, and the eyes look at it at rest). `FurutaRig.hunt` drives every
  mouth at rest (`maw`: the centipede's, both colossi): with prey the head turns on it and trembles, jaws working; each
  `strikePeriod` it rears back gaping (`REAR`), lunges (`LUNGE`: a snap shuts on landing, a feint stops short, jaws wide) and
  recoils (`RECOIL`); `nextStrike` gives the timing. With none: jerky search, a new bearing every 16..24 ticks snapped to in 3,
  frozen, sniffing, now and then snapping at nothing. A BITE beat (Flesh Devourer, Snapping Lunge) trembles through its wind-up
  and shakes its head to tear after it lands (zero on the landing tick). Teeth back to 5 a rim (no inner row, no big fangs).
  Tests `FurutaTests.hunting`; sheet `hunt` (`docs/images/furuta-hunt.png`). No snap/hiss sounds yet (a client one-shot sound
  needs `ClientLevel.m_7785_` with SRG evidence on the allowlist).

- **Pushed: the many-eyed kagune: the OLD look, the panel's pose and shape.** The user, angry: "I DONT MEAN THE LOOK IDIOT, I
  MEANT THE POSE AND THE SHAPE". The look stays exactly the old crescent's (6d64b39: colours MASS/MASS_CREASE/FLESH_DEEP, the five
  almond eyes at their old places and sizes, spike clusters, veins, lumps). **Never change the look again unless asked; never use
  disc eyes.** Changed, and only these:
  - Pose (`FurutaRig.LAYER_0..4`): four thick bands laid one over another about a band's breadth apart, a wall behind him from
    his left shoulder to his right (L0 rises behind his left shoulder, L1 behind his head), each running out LEVEL to his right
    and keeping its height to its end (the mass as tall at its far edge as behind him), coming a little forward there; L0's tip
    hooked down over L1's end curling up (the beak); L4 a tail down to the ground and along it in front of him on his right.
    Tried and rejected by the look of them: a fan from one point, nested arches, a dome with fingers, arching/drooping bands
    (a mushroom cap), bands wrapping far forward (a wing converging on one point).
  - Shape (`FurutaModel.crescent`): round, not a flat slab (`FLAT` 0.84), a crest along its back (`CREST`), holding its thickness
    to `CRESCENT_HOLD` 0.74 then tapering to a hooked point; `tendrils`: 3 thin claw-like whips off each far end; width 0.46.
  - The user has not seen this one yet.
- **Pushed: the many-eyed kagune and the hit fixes.**
  - Crescent look restored from `6d64b39` (`FurutaModel.crescent`, `crescentWidth` now thin at the root via `emerge`, almond eyes,
    spike clusters, `MASS` colours, stage width back to 0.55); `lobe`/`disc`/`streaks` are gone. No mass blob.
  - Posed as the manga's stack: every DRAGON limb on his right (`FurutaRig.side` = 1), `LAYER_0..4` nested arches ~8 blocks each,
    the top curling over him, the lowest trailing on the ground (rest points floored at y 0.08). Eyes start at s 0.3 (the layers
    run together before that). Preview sheet `dragon` (`docs/images/furuta-dragon.png`).
  - **Server bug fixed:** `FurutaCombat.strikeBody` never reset `invulnerableTime` (`f_19802_`), so any blow within 10 ticks of the
    last was refused and its stun, bleed and knockback skipped (Rinkaku Barrage landed 1 stab of 9, the barrages and the
    execution likewise). Every other path resets it.
  - **Seen = hit:** blows aimed at a point are let go `FurutaRig.LEAD` (1 tick, at most 45% of the wind-up) before they land, so
    the drawn limb (on its springs) is on the server's point on the landing tick. Test `FurutaTests.strikes`: every THRUST, BITE,
    UPPER, SLAM, SEIZE, IMPALE beat of every move a stage can make, four aims, springs simulated from rest, within 0.9 blocks
    (it failed 416 checks, 2 to 4 blocks off, before). `FurutaRig.hitPoint` is the server's formula in rig space;
    `FurutaRules.heldPoint` is now shared by the server and the previews.
  - Crescent M1 4 is now **Crescent Jaws** (top layers slam, the middle thrusts, the lowest rip up, one tick).
  - **Living Fortress / Monstrous Regeneration:** `FurutaRig.curl` is a cocoon wall (`COCOON_RADIUS` 1.8, bands 0.35..2.75,
    alternate ways round, cut at the limb's length via `wrapped`), the dorsal hint sideways at the root so the faces are radial,
    and `FurutaModel.Look.outward` puts every crescent eye on the outward face. Sheet `fortress`.
  - `FurutaRig.fan`: limbs sharing a side rear, coil and arch apart (they moved as one rope); Predatory Evolution rears the
    stack itself (`rearing`).
  - Previews: `frames` / `frames-<stage>` sheets (`docs/images/furuta-frames-<stage>-m1.png` / `-abilities.png`): 6 frames a move,
    springs from rest, his posed body, a dummy, yellow where the server hits that tick; side view for strikes, high view for sweeps.
- **Next:** the user's in-game verdict on this jar; snap/hiss sounds for the hunting mouths (needs a client sound call on the
  allowlist).

## The user's latest complaints (angry, after testing the jar)

1. **Skins broken.** The kakuja's hide left the forearms bare when the elbows bent. **Fixed in this push** (see below).
2. **Star.** It was off centre, grey in game and unlike the reference. **Fixed in this push**: centred and truly glowing. Their
   sketch is a big five-pointed star over the middle of the face, above the grin.
3. **Many-eyed kagune (stage DRAGON).** Their words: *"I told you to rework the I one just so to make it pose it differently to
   look like the reference image... You changed it to something else entirely and kept the same pose."*
   - See `many-eyed-before-rework.png` (the look they had: lumpy crescent tentacles, almond eyes, spike clusters, plus a thick
     mass on the back) and `many-eyed-current-rejected.png` (flat ribbons, fanned to both sides).
   - The original bug report only said: the back was too thick, and there was no form like the manga's "Let's play house"
     panel. In that panel the kagune is a towering, layered mass to one side of him, layers stacked one over another, ends
     curling, the lowest trailing on the ground.
   - **TODO.** Restore the crescent look from commit `6d64b39`: `FurutaModel.crescent`, `crescentWidth`, `CRESCENT_EYES`, the
     MASS colours, and `FurutaMesh.almond` (deleted since). Keep the back thin (no `mass` blob). Pose all five on one side as a
     towering stack (`FurutaRig.side` = +1 for every DRAGON limb).
   - Design each layer's rest curve with a polyline length of about 8 blocks. `whole()` rescales the design to the stage length
     (7.6 to 8.6), so a short design pushes the low one under the ground.
   - Draft layers (rig space: x to his right, y up, z forward; root `{0.13, 1.5 - 0.05*layer, -0.2}`):
     - L0: up to about 7.2 high, curling forward over him.
     - L1: about 5.5 high, about 3.5 out to the side.
     - L2: about 3.9 high, 4.4 out.
     - L3: about 2.3 high, 4.9 out.
     - L4: trailing on the ground ahead of him, y about 0.12.
   - Draft points: L0 `{0.13,1.5,-0.2},{0.4,2.0,-0.65},{0.9,3.1,-1.0},{1.4,4.4,-1.1},{1.9,5.7,-0.9},{2.3,6.7,-0.4},{2.4,7.2,0.3},{2.1,7.1,1.0},{1.7,6.6,1.5}`.
   - Show this in a preview **before** pushing. Also check all the DRAGON moves (CRESCENT_1..5, DRAGONS_TENDRILS,
     THOUSAND_THORNS, LIVING_FORTRESS, PREDATORY_EVOLUTION, CENTIPEDE_RUSH, THORN_CYCLONE) with every limb on one side.
4. **Ferocity.** *"the way you make them more ferocious is to add more teeth? What the fuck is wrong with you?"* Ferocity comes
   from **behaviour and life**, not teeth: the centipede mouth, the kakuja's colossi, the Flesh Devourer.
   - **TODO.** At rest, pass the nearest living entity within about 10 blocks (not himself) to `FurutaRig` as prey.
   - The heads track it, rear back, gape, snap fast, feint-lunge at it, jitter, with a quicker spring response.
   - With no prey: jerky stop-and-start hunting, the head sweeping and freezing.
   - Possibly snap/hiss sounds (`ManifestSounds` may show how sounds are played).
   - Maybe cut back the extra tooth rows added last time (`FurutaModel.maw`).
5. **Abilities "dog shit", Living Fortress "buggy", "you are not testing anything, not even the M1 combos".**
   - **TODO.** Verify every M1 and ability frame by frame. Add a `frames` sheet to `tools/FurutaPreview.java`: per move, about 6
     frames over its ticks, with the body posed by `posed(...)` (this push adds it) and the kakuja hide via `kakujaBody`.
   - Fix what looks bad. Living Fortress is `FurutaRig.curl()`: too tight (radius 0.72 to 1.31) and clips. Make it a cocoon
     wall at about 1.7 to 1.9 blocks round him, the ribbons alternating direction at heights 0.35 to 2.75, the faces and eyes
     turned outward (dorsal hint radial). Monstrous Regeneration uses the same curl for the kakuja.
   - Add tests that each THRUST/BITE/UPPER beat's rig tip at `land` is near the server's strike point in `FurutaCombat`.
   - Judge whether each ability is useful: damage, reach, cost, cooldown. Show the user the strips.

## Done in this push (build green: every test passes, the jar is verified)

- `FurutaArmorLayer`: the hide and claws bend with bendy-lib's elbows and knees.
  - Uses `BodyFrame.bent(pose, part, local)` and `KaguneBodyAnimation.limbs(player)`, which is null without bendy-lib or with
    3D Skin Layers.
  - The glow quads (normal exactly (0,1,0)) are collected and drawn afterwards with `RenderType.m_110488_` (eyes, additive,
    full bright), colour premultiplied by alpha. Before, entityTranslucent shaded them by a normal that points at the ground on
    the face, which is why the star came out grey.
  - Two non-fixed render types must never take vertices in turn: they share one buffer.
- `FurutaArmor`: star centred (`STAR_X`), `STAR_Y` -5.0, `STAR_RADIUS` 3.2 (top point at the head top, lower points at the
  grin). The glints are removed.
- `FurutaRenderer`: the limbs' roots ride his posed torso (hunch, lean, lunge).
  - Uses `FurutaPose.ride(KaguneBodyAnimation.body(player, ControlClient.worldTime(partial)), points)`, with `BodyFrame.carried`.
  - The carry fades over the first 45% of the limb, so the aim is kept.
- `FurutaTests`: the hide stays on the bent limb (within 1.6 px), and drawn straight it would not.
- `tools/FurutaPreview.java`:
  - `posed(stage, mastery, move, age)` builds his body from `FurutaPose`.
  - `whole()` rides it.
  - The `kakuja` and `moves` sheets now use the real body pose. `docs/images/furuta-kakuja.png` is updated.

## Working fast

- Quick compile and the Furuta tests in seconds:
  `javac -nowarn -encoding UTF-8 --release 17 -proc:none -cp build/stubs:build/base.jar -sourcepath src/main/java:src/test/java -d <dir> src/test/java/com/hex/kagune/control/FurutaTests.java <changed client files>`
  then `java -cp <dir>:build/stubs:build/base.jar com.hex.kagune.control.FurutaTests`. The repo needs `python3 build.py` run
  once (about 5 minutes) before `build/base.jar` and `build/stubs` exist.
- Previews: `tools/furuta-preview.sh <out> [stages details kakuja moves deploy head mouth maws]` (a few seconds).
- Full gate before every push: `python3 build.py` (patch, compile, API allowlist, server check, all `*Tests`).
- Commit trailer:
  `Co-Authored-By: Claude Opus 5.5 <noreply@anthropic.com>` / `Claude-Session: <your session URL>`.
  Never put a model id anywhere in the repo.
