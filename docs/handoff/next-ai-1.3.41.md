# HANDOFF: HexGhoul after 1.3.41. Read this first, then `docs/handoff/next-ai-1.3.33.md` (how the owner works, older ideas).

Branch **`claude/relaxed-carson-4y0frw`** (check it out from origin; push with `git push -u origin claude/relaxed-carson-4y0frw`).
Never push to `main`; no PR unless asked. Version **1.3.41** in `build.py` and `patch/KagunePatch.java` (bump both; the next is
**1.3.42**). Network `PROTOCOL` is **"67"**. Nothing since 1.3.28 has been tried in game: never say anything works in game.

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

- **1.3.39** fixes: NPCs hold still while a talk is open; anyone held (hand in hand, a carried child) has no block collision while
  held (`f_19794_` noPhysics) and is set down safely; wild ghouls fight back in creative, so their kagune shows.
- **1.3.40** Home Living part 1: **the ghoul kitchen**: 66 foods generated from one table (`tools/kitchen.py` -> icons via
  `tools/kitchen_art.py`, models, recipes in `recipes/kitchen/`, tags, lang, and the generated `control/kitchen/Menu.java`; never
  hand-edit Menu.java). Four new effects (Refreshed, Replenished, Content, Clear Sight). "Contains ghoul meat" tooltip and tag. Endless
  ghoul-meat growth `1 + 0.12/0.25 x ln(1 + n/20)`, mutants (Eto's one-eyed: `Mutant`), **The Human** (`/summon hexkagune:human`), and
  Anteiku/the cyber tower in plains only, the Library in plains and forests.
- **1.3.41** Home Living part 2: **Touka's cookbook** (her main talk page at regard >= 20, two cocoa beans in hand) and its
  illustrated book screen (`CookbookScreen`, art by `tools/cookbook_art.py`).

## What's next: the rest of the owner's Home Living request (do these in order, one version each)

1. **1.3.42: the partner cooks the kitchen's dishes at home.** The owner: the partner "cooks recipes at home (instant, using the
   existing cooking animation) when ingredients/recipes are available in home chests". Build on `server/HomeKitchen` +
   `family/Kitchen` + `family/Chores` (the 1.3.31 stove chore, the COOK gesture): when the home's chests hold every ingredient of a
   `Menu` recipe (`Dish.recipe().ingredients()`, cooking recipes too), the spouse takes them, plays the cooking gesture, and puts the
   dish (x `makes`) in the chest or keeps it as the meal for the player (a ghoul's meal = ghoul food: every dish counts). Prefer dishes
   with effects, never use up the last of something the player stocked for another recipe twice in a row, a line about what they made
   ("I made Crimson Curry. It's on the counter."). Keep `Kitchen` pure and tested (which recipe can be made from a chest's counts).
2. **1.3.43: children inherit their parents' kagune.** The owner: "Kids inherit parents' kagune (colour from one parent; dad Rinkaku +
   mom Ukaku -> both tentacles and 1 wing with moves of both; Eto's child gets her kagune abilities and transformation at low health)".
   Start at `family/Genome` (record decided at conception: add the kagune kinds and a colour; it is saved, so add fields with defaults
   for old saves) and `server/Children` / `ChildNpc` / `ChildBrain` (their fights). A child's kagune is NPC-side, like the wild
   ghouls' (`GhoulNpcCombat`, `NpcIdentity.Kind`, the Rinkaku tails and the arm blade's renderers), sized to the child's stage.
3. **The Human's structure later** (the owner: "spawnable via command only for now; future structure later"): don't build it until asked.
4. Then the owner's bug reports from playing (they come first whenever they arrive; each fix its own version).

## Things worth checking when the owner plays (all untested in game)

- The cookbook screen in game: text fits a page at every GUI scale? (checked only with a font-width mock); ribbons over the top edge.
- Dish effects stacking, the bowl/bottle coming back when eating the last one in a stack, Clear Sight's water fog.
- The Human's flee pathing near water and fences; its panicked arms (`HumanRenderer.Model`).
- Structure generation in new worlds: three candidate spots each (`count`/`spread` 3); extras are empty buildings.
- `GhoulMeat` numbers changed for existing saves (the piece count is kept; the curve is new).
