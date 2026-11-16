# HANDOFF: HexGhoul after 2.0.0 (Release 2.0). Read this first, then `docs/handoff/next-ai-1.3.60.md` (house rules), then the top
sections of `docs/handoff/companions-handoff.md`.

Branch **`claude/relaxed-carson-4y0frw`** (check it out from origin; push with `git push -u origin claude/relaxed-carson-4y0frw`).
Never push to `main`; no PR unless asked. Network `PROTOCOL` is **"72"**. Version **2.0.0** in `build.py` and `patch/KagunePatch.java`
(the next is **2.0.1**). Nothing since 1.3.28 has been tried in game: never say anything works in game. RULE 1 (stop at 97%) is gone.

## The owner's last request (all done, each its own version)

> "Release 2.0: Add more humans with random personalities per village (make sure there are no 2 of the same look per village.). same as
> you did with the children. Check for bugs, patch them, make sure theres no lag or unecessary lagging shit. Check all animations make sure
> all of them are, PERFECT and I mean it. Mastery & Forms does not increase HP for the player, instead it basically just boosts resistence
> by a ton, the more mastery, the more ghoul meat eaten, the better the form, etc all those factors makes the player a living tank, to make
> it more balanced with the NPCS and pvp."

- 1.3.80 the living tank (`GhoulTank`, `GhoulTankServer`), 1.3.81 16 villagers and 26 looks, unique per village, 1.3.82 bug/lag pass,
  1.3.83 animation pass (6 shared poses fixed), 2.0.0 the release. Details: the top sections of `companions-handoff.md`.

## Lessons from this stretch (keep them)

- **Zoom into every face/texture before saying it passes.** The owner rejected the first blinks (a thumbnail review let a dozen bad faces
  through). Recolouring tools need a hand pass per skin (hoods, bows, hoodies the hair's colour): see `tools/child_variants.py`'s lists.
- `PoseRender` reads 64-pixel skins only: scale HD skins down first (`VillagerPreview.small`).
- NPC arms have no elbows: a "hand to the face" turns the arm IN (right arm Y negative, left positive) and reaches the face from a raised
  X near -2.0; a positive Y throws it out sideways.

## Untested in game (what to watch when the owner plays)

The Brick Village and its 16 people (looks, blinks, blushes, gestures, kisses, hugs, dates), the children's new looks (and existing kids
changing colour), the living tank's numbers in fights and PvP, the fixed poses, War/Peace without extra health.
