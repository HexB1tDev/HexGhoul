# HANDOFF: HexGhoul after 1.3.63. Read this first, then `docs/handoff/next-ai-1.3.60.md` (house rules, family talk), then the top
sections of `docs/handoff/companions-handoff.md`.

Branch **`claude/relaxed-carson-4y0frw`** (check it out from origin; push with `git push -u origin claude/relaxed-carson-4y0frw`).
Never push to `main`; no PR unless asked. Version **1.3.63** in `build.py` and `patch/KagunePatch.java` (the next is **1.3.64**).
Network `PROTOCOL` is **"71"**. Nothing since 1.3.28 has been tried in game: never say anything works in game.

## The owner's current update (their words, sent during 1.3.60)

> "When 2 hits collide theres an impact, thats already in the game, but depending on how hard the player / NPC hit the impact should be
> bigger, even shake the floor / camera, to make the fights more dramatic and fun, also if both use the same move like, big punch, speed
> kick or any of the sort it should cancel both out and cause a big impact.
> Need more and better reactions to the final kakujas, Rinaku and ukaku both, but especially the War/Peace path, being scared of the War
> form.
> New more dramatic running animations for both War and peace.
> Use the new sounds I sent as the new clash sounds whenever theres an impact."

## Done (each its own version, built EXIT 0, pushed)

- **1.3.61** the owner's `Clashes.zip` (Clash1..4.ogg) are the clash sound (`sounds/sfx/clashes/clash_1..4.ogg`, the `clash` pool in
  `tools/sfx_pack.py`, which writes `sounds.json`: run it after any sound change).
- **1.3.62** a clash is sized by both blows' damage (`ClashRules.impact`; every `KaguneClash.swing` site passes its blow's damage); a hard
  one shakes the ground (`KingMarkFx.GROUND`, a camera rumble, `sfx.quake`). Protocol 71.
- **1.3.63** the same big move meeting itself cancels both with a big impact (`pale/MoveClashRules`, `server/MoveClash`): Pale's Big
  Punch / Speed Kick / club slam and the Owl's versions, for Pale players and Eto. NOT War/Peace (their kits have no way to cut a move
  short: `KingServer.move` only sets the animation; a cancel per kit is needed first) and not Eto's child's flesh. If the 1.3.63 commit is
  missing from the branch, its files were in the working tree mid-build: rebuild, check, commit.

## Left to do (each its own version)

1. **1.3.64 (a bug, found by reading):** `Sightings.of(player)` returns `Display.DREAD` when `KingServer.awake(player)`, which is the
   permanent "the Dragon has been through him" save, not "wearing War/Peace now". So a War/Peace-path player always reads as DREAD: NPCs
   react once and never again (and never to their kagune or kakuja). Fix: DREAD while `KingServer.wearing(player) != KingForm.NONE` or
   `DragonLock.dragon(player)`; consider making the Rinkaku final kakuja (`RinkakuForms.finalKakuja()` stage) and Ukaku's
   `WingForm.FINAL_KAKUJA` (`UkakuServer.form(player).finalKakuja()`) DREAD too.
2. **1.3.65 reactions to the final kakujas, fear of War:** `Sightings` watchers are Eto/Touka/Ken (`EtoBrain.saw`), wild ghouls
   (`scaredUntil`, `ReactionLines.stranger/ally`), children (`ReactionLines.child`). Ideas: tell WAR, PEACE, the Dragon, Rinkaku's and
   Ukaku's final kakujas apart (a separate kind beside `Display`; `Display` isn't sent, but keep its order); keep wild ghouls running
   while War is in sight (refresh `scaredUntil` each check), humans (`HumanBrain`) fleeing War from much further with screams, CCG agents
   (`AgentBrain`) freezing or falling back with a line, children hiding behind the other parent with a SHY/SORRY cue (`ChildCue`),
   companions' own lines per persona (Peace: awe; War: dread, Touka worried, Eto fascinated), all line sets tested for banned names.
3. **1.3.66 War and Peace running animations:** their poses are `client/animation/WarPose` / `PeacePose`, applied in
   `KaguneBodyAnimation.war/peace` (players) and `EtoFight.peace` (Ken); the run is currently left to the legs' own walk
   (`npc.f_267362_` walk animation, `WarPose.peace(p, time, 1 - 0.7 x run)`). Preview with the pose tools (`tools/PosePreview.java`,
   `tools/pose-preview.sh`) and look honestly.
4. Optional, if the owner wants it: War/Peace same-move cancels (needs a cancel per King kit).

## House rules (short; full list in next-ai-1.3.60.md)

Nothing tested in game, never claim it. Banned in the jar: Kaneki, Ayato, Furuta; in visible text: Takatsuki, Aogiri, Yoshimura. Sounds
only from the hexkagune: pack. Never alter the owner's skins or files (their sounds were copied unchanged). New Minecraft API needs an
allowlist entry with [srg]/[order] evidence plus a stub. Append only to ordinal-sent enums (`ChildCue`, `KingMarkFx` style ints,
`PaleMove`...). Each fix/feature its own version: bump `build.py` and `patch/KagunePatch.java`, release notes, README entry, a new top
section in `companions-handoff.md`, full `python3 build.py` to EXIT 0 (~4 min, in the background; never edit sources while it runs),
commit "1.3.xx: ..." with `Co-Authored-By: Claude <noreply@anthropic.com>` and the session's `Claude-Session:` line, no model names, push.
Quick loop: `python3 tools/quick_check.py ClashTests FamilyTests NpcTests`. Answer the owner in English; say plainly what was a judgment
call or an approximation.
