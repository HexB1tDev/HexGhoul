# HANDOFF: HexGhoul after 2.0.20, for 2.0.21+: the White Reaper. Read this first, then `docs/handoff/next-ai-2.0.9.md` (what 2.0.5
to 2.0.20 did), `docs/handoff/next-ai-2.0.5.md` (how the owner works now), `docs/handoff/next-ai-1.3.60.md` (house rules), then the top
sections of `docs/handoff/companions-handoff.md`.

Branch **`claude/relaxed-carson-4y0frw`** (check it out from origin; push only there with `git push -u origin claude/relaxed-carson-4y0frw`).
Never push to `main`; no PR unless asked. Version **2.0.20** in `build.py` and `patch/KagunePatch.java`; the next is **2.0.21**. Protocol
**"73"** (`ControlNetwork.PROTOCOL`): bump it if a new message or a new ordinal-sent value goes over the wire. Nothing is tested in game.

## The owner's request (their words)

> "howver RANK 10 Investigators will now be named White Reaper, get insane speed and only spawns 1 of them, never 2 at once. rarer than
> any other and super hard to kill often taking a full mastery player with kakuja or a duo of players. this person will use both combat
> m1s and kicks, dashes, flips, and a new special quinque that is both used as a sword and a long range weapon"
>
> "Long range attach is basically lighting projectiles (yellow lightning ball that explode on impact) (Simple pointing the tip
> animation)"
>
> "also the briefcase is black instead of white"

They sent three reference pictures (a long black lance; the same lance held at the hip; it swung with dark ribbons around it). **Ask them
to attach the pictures again** in the session that builds the model: they are not in the repo. What the pictures show, for the model:
- **The weapon:** a lance about two blocks long, all black with a faint dark red-brown sheen. Its front is a long, thin, straight spike,
  thicker in its middle third where two shallow twisted ridges run along it. Behind the spike, a big hollow cone (a vamplate) widening
  toward the hand: black, with a gold band near its wide rim (two thin gold rings with a row of small gold circles and dots between them).
  Behind the cone, a thin rod (the grip) about half a block long, ending in a short spike at the back. Near the cone, a curved black
  claw or hook comes off the grip and bends back along it (a hand guard).
- **The case (briefcase):** like the other quinque cases, but **black** where theirs are white (the owner's last word).
- Visible names: the owner chose **"White Reaper"** for the investigator. Do not use the source character's or the weapon's original names
  anywhere (keep the banned-name lists in mind). The weapon's name is not decided: **ask the owner** (a placeholder: "Reaper's Lance").

## What exists now (2.0.17 to 2.0.20; read before changing)

- `npc/AgentRules.Rank`: RANK_0..RANK_10 (weight, health, speed, armor, power, pace; title "Rank N Investigator", red from `DANGEROUS` 8).
  RANK_10 now: weight 0.8 (out of 100), health 280, speed 0.41, armor 18, power 3.2, pace 0.55. `dashes(rank)` (6 at Rank 10),
  `dashSpeed`, `dashEvery`, `dashOften`, `zig` (zigzag chains, 2.0.18). `superior`/`subordinate`, `SUPERIOR_HEALTH`/`POWER` (2.0.20:
  a pair's leader always the stronger).
- `server/AgentBrain`: `wake()` rolls or reads the rank (`RANK_LEVEL` saved; old `RANK` via `fromOld`), clamps it against a partner,
  sets health/speed/armor attributes and the name (`agent.m_6593_`). Damage everywhere is `AgentRules.power(mind.rank, mind.superior)`.
  Footwork: `maneuver` (parry, dodge by flip or dash, front/back flips, side dashes), `feet` (flip/dash pushes, zigzag chaining), `dash`,
  `flip`. Melee strings: `begin`/`strike` with `Quinque.weight(step)`. The launcher: `shoot` and `fire` (`CrystalShots.fire`, shards).
  `hurt(LivingHurtEvent)` at the bottom (its own hurt handling).
- `server/AgentSpawner`: wild pairs near players (`AgentRules.SPAWN_CHANCE` each `SPAWN_EVERY`), `partner(...)` makes the second;
  `AgentBrain.pair` saves both. Village patrols (`VillagePlace.KEY` agents) walk alone and also roll a rank.
- `quinque/Quinque` enum: BLADE, SPEAR, KATANA, MACE, LAUNCHER (ids, models, reach, damage, `ranged`, swing). **Its ordinal goes over the
  wire** (`CcgAgent` entity data `LOOK` packs `kind.ordinal() << 8`): **append only** (the new one goes last). Items: `ControlItems.caseOf`
  / `weaponOf`; models from `tools/quinque_models.py` (cases `quinque_case_*.json`, weapons); deploy frames by the `hexkagune:deploy`
  property (`QuinqueClient`, now registered by reflection since 2.0.19); a player can carry and use quinques too (`QuinqueServer`).
- Rendering: `client/render/AgentRenderer`, `AgentModel`, poses `client/animation/AgentPose` (one pose per move; add one for "pointing the
  tip"). Preview tool: `tools/QuinquePreview.java` (renders quinque models; make a sheet of the new lance and the black case and look
  at it honestly, zoomed in, before saying it matches).
- Kicks to copy: `server/EtoOwl.kick`/`kickLands` (an NPC's Speed Kick: launch, `PaleKnockdown.launched`, `S2CPaleFx.KICKED` tumble);
  2.0.11's overwhelm in `KaguneClash.apart` (the same, from a clash). An NPC can't smash blocks (`BlockSmash.kick` needs a player).
- Clashes (2.0.11/2.0.12): a much stronger blow overwhelms the weaker (`ClashRules.GAP` 2.5): the Reaper's blows will often overwhelm.
- Projectiles never burn (2.0.15), trails take the shooter's colour (2.0.16; the Reaper's lightning is yellow by design, not a kagune).
- Sounds: only the `hexkagune:` pack (plus the owner's approved vanilla list in `SfxTests.KEPT_VANILLA`). The pack has `sfx.thunder`,
  `sfx.burst`, `sfx.shockwave`, `sfx.crunch`, `sfx.spike_throw`, `sfx.dash`, `sfx.blade_swing`, `sfx.clash`: enough for the lightning.
- One-of-a-kind bodies: `server/UniqueNpcs.claim(level, key, uuid, home)` keeps one body per key in the world (companions, children):
  use it (key e.g. "ccg:reaper") so there is never a second White Reaper.

## A plan (proposals; the numbers are first guesses to tune, say so to the owner)

Ship it in a few versions, each its own (2.0.21, 2.0.22, ...), pushing as each is done:

1. **The Reaper himself (server):** Rank 10 is the White Reaper. Title "White Reaper" (white or bold, the owner's choice; never two).
   - Rarer than any rank: RANK_10 weight down to about 0.15. When Rank 10 is rolled while a Reaper is alive (`UniqueNpcs` claim held),
     it becomes Rank 9 instead. He always walks alone: no partner made for him (`AgentSpawner`), never a village patrol (village agents
     cap at Rank 9). The 2.0.20 superior rule doesn't apply to him (no pair); a test should guard "never two".
   - Insane speed: movement about 0.6 (Rank 9 is 0.40), fight pace on top; dashes chained 8 (`dashes`), dash speed far above Rank 9's,
     flips and dodges much more often; knockback resistance high (`Attributes.f_22278_`, already allowlisted).
   - Super hard to kill, "often taking a full mastery player with kakuja or a duo of players": a lot of health (e.g. 900) and armour,
     damage taken cut (e.g. to 45%, in `AgentBrain.hurt`), slow healing out of a fight. **Calibrate from the code, not by feel:** work out
     a mastery-1000 kakuja player's sustained damage per second from the path rules (`UkakuRules`, Rinkaku/Graft/Owl rules, `GhoulTank`
     for what they take), and aim for roughly a minute or more alone, half that for two; his damage against `GhoulTank.taken(1000, ...,
     kakuja)` so a kakuja master survives a while but must not stand still. Put the numbers in pure `AgentRules` with tests.
   - Uses everything: M1 strings with the lance (as a sword), kicks (copy `EtoOwl.kickLands`: launch, tumble, knockdown; no blocks
     smashed), dashes (zigzags), flips, parries (clashes), and the lightning at range.
2. **The new quinque, appended last to `Quinque`** (name to ask): melee like a long sword/spear (reach ~3), and a ranged mode the Reaper
   switches to when his prey is far (and players can use it too, if the owner wants the item obtainable: ask; the Reaper's case could be
   his rare drop). The case is **black**. Models from `tools/quinque_models.py` (case + weapon, the deploy frames), checked on a preview
   sheet against the owner's pictures.
3. **The lightning ball (ranged):** a "point the tip" pose (`AgentPose`: the lance raised level at the prey, a short hold), then a
   yellow lightning ball flies (about 1.5 blocks a tick, range ~32): its own server projectile (like `CrystalShots`) and a new client fx
   (a bright yellow core, white crackling arcs, small sparks; no new particle textures needed: `RcFx` SPARK/EMBER/PUFF in yellow and white).
   It explodes on impact (radius ~2.5): damage, knockback away from the blast, **no fire** (2.0.15), a flash, `sfx.thunder`. A new
   network message or fx kind means a protocol bump. Preview the fx frames if you can render them.

## House rules (unchanged; full text in `next-ai-1.3.60.md`)

Nothing is tested in game, never claim it is. Look at your own previews honestly, zoom into every face and texture, match the owner's
pictures. Banned names in the jar: Kaneki, Ayato, Furuta. Banned in visible text: Takatsuki, Aogiri, Yoshimura. Sounds only from the
hexkagune: pack. Never alter the owner's skins or files (only generated copies). New Minecraft API: an entry in `verify/api-allowlist.txt`
with [srg] or [order] evidence plus a stub (`tools/find_srg.py <yarn class> <member>`); enum constants keep their names; stale entries
fail the build. **The verifier checks names, not access:** never call a vanilla-private member directly (2.0.19's crash); use Forge's
`ObfuscationReflectionHelper`. Append only to ordinal-sent enums. No model names in commits or files. The owner is short on usage: no
full local build; push each piece; CI builds and publishes from `.github/release-notes-<ver>.md`. This session's pure tests were run in a
small harness (compile the pure classes with `javac -sourcepath src/main/java:stubs` and run the new test block); `build/` doesn't exist
in a fresh container, so `tools/quick_check.py` needs one full build first.
