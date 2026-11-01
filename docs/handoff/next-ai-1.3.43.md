# HANDOFF: HexGhoul after 1.3.43. Read this first, then `docs/handoff/next-ai-1.3.33.md` (how the owner works, older ideas).

Branch **`claude/relaxed-carson-4y0frw`** (check it out from origin; push with `git push -u origin claude/relaxed-carson-4y0frw`).
Never push to `main`; no PR unless asked. Version **1.3.43** in `build.py` and `patch/KagunePatch.java` (bump both; the next is
**1.3.44**). Network `PROTOCOL` is **"68"**. Nothing since 1.3.28 has been tried in game: never say anything works in game.

## House rules (the owner's, verbatim where quoted)

- "Rules: nothing is tested in game, never claim it is. Look at your own previews honestly and match my marks exactly. Banned names in
  the jar: Kaneki, Ayato, Furuta. Banned in visible text: Takatsuki, Aogiri, Yoshimura. Sounds only from the hexkagune: pack. Never
  alter the owner's skins (only the generated overlays). Any new Minecraft API reference needs an entry in verify/api-allowlist.txt with
  [srg] or [order] evidence plus a stub; stale entries also fail the build. Append only to ordinal-sent enums."
- SRG lookups: `python3 tools/find_srg.py <yarn class> <member>`; obf names and ids in `build/mapping-cache/joined.tsrg` and
  `intermediary-1.20.1.tiny` (verify every id you quote; Forge members cite the 1.20.1 Forge source file and line).
- Every fix or feature ships as its own version: bump `build.py` VERSION and `patch/KagunePatch.java` VERSION, write
  `.github/release-notes-<version>.md`, add a README entry (same shape as the others), add a new top section to
  `docs/handoff/companions-handoff.md`, run the full `python3 build.py` to EXIT 0 (about 10-15 minutes; run it in the background),
  commit as `1.3.xx: ...` with the trailers `Co-Authored-By: Claude <noreply@anthropic.com>` and the session's `Claude-Session:` line,
  no model names anywhere (commits, code, docs), then push. Stage the version's files explicitly; never commit scratch files.
- Answer the owner in English only, even when they write in another language. They value companion life, realism and depth.

## What the last session shipped (details: the top sections of `docs/handoff/companions-handoff.md`)

- **1.3.42** the partner cooks the cookbook's dishes at home (`family/Kitchen.cooking/choose/passes/dishDue`, `HomeKitchen.dinner/
  cookDish/hand`, `HomeChores` COOK with `Job.dish`; ghoul spouses only; 4 a day, 2 minutes apart; `Menu.NAMES` from `tools/kitchen.py`).
- **1.3.43** children inherit their parents' kagune, part 1: `family/KaguneGenes` + `Genome.kagune/colour`, Rinkaku tails and Ukaku
  wing(s) on ghoul children (`server/ChildKagune`, `family/ChildKaguneRules`, drawn by `GraftRenderer`/`UkakuWingRenderer` at the
  child's scale in the colour from one parent). Protocol 68.

## What's next

1. **1.3.44: Eto's child (the rest of the owner's ask).** The owner: "Eto's child gets her kagune abilities and transformation at low
   health". The genome already carries it (`KaguneGenes.OWL`, Eto's colour 0xD8CCBC); `ChildKagune.has` ignores OWL for now and
   `/kagune children` says it comes later. What exists to build on:
   - Server: Eto's kit is `server/EtoOwl` (+ `EtoFlesh` swipe/grab/hammer, `EtoWatch`, `EtoArm`, `EtoOwlForm` grow/fight, rules in
     `npc/EtoRules`: `owlNow(share, ...)` = her low-health trigger), all typed on `EtoNpc` and `EtoBrain.Mind`/`EtoOwl.Fight`. Either
     generalise the pieces a child needs (the flesh moves, the Owl's grow and its moves) to a small interface both brains implement, or
     write a child-sized subset in `ChildKagune` (the cleaner path last time: the tails and wings were rewritten, not shared).
   - Client: her flesh is drawn by `PaleFleshRenderer` through `EtoRenderer.Kit` (`draw(LivingEntity who, PaleClient.PaleView view,
     RigPose drawn, double scale)`: any body, any scale) fed from `EtoClient` views; the Owl's creature replaces her body while
     `OwlClient.creature(npc, now)`. A child needs a `PaleView` (its move) sent to its clients and a loop like `EtoRenderer.kit` over
     children, with `ChildKaguneRules.scale(stage)`.
   - Rules to keep: only what it fights and monsters are hit (`ChildKagune.fair`); a child of Eto and a Rinkaku/Ukaku father also has
     the father's kagune (OWL|TAILS, OWL|WING): decide with the owner how the two show together if it looks crowded.
2. **The Human's structure later** (the owner: "spawnable via command only for now; future structure later"): don't build it until asked.
3. Then the owner's bug reports from playing (they come first whenever they arrive; each fix its own version).

## Things worth checking when the owner plays (all untested in game)

- 1.3.42: a chore's cooking at the stove with a full chest; the "made" lines; the meal handed over; buckets and bottles put back;
  `/kagune home`'s dish list. A human spouse gets no cookbook dishes (by design: every dish has ghoul or human meat).
- 1.3.43: a ghoul child's tails/wing in a fight at its size (offline preview only: `GraftPreview <dir> child`), the shots' origin at
  the wing tip, the colour; old children's kagune worked out once a player parent is online.
- Older: the cookbook screen at every GUI scale; dish effects stacking; The Human's flee pathing; structure generation in new worlds.
