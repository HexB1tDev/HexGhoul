# HANDOFF: HexGhoul after 1.3.78. Read this first, then `docs/handoff/next-ai-1.3.60.md` (house rules), then the top sections of
`docs/handoff/companions-handoff.md`.

Branch **`claude/relaxed-carson-4y0frw`** (check it out from origin; push with `git push -u origin claude/relaxed-carson-4y0frw`).
Never push to `main`; no PR unless asked. Network `PROTOCOL` is **"72"** (unchanged since 1.3.68). Version **1.3.78** in `build.py` and
`patch/KagunePatch.java` (the next is 1.3.79). Nothing since 1.3.28 has been tried in game: never say anything works in game.

**RULE 1 (stop at 97% usage) is gone for good** (the owner, after 1.3.78: "Yes take that shit out."; removed from `CLAUDE.md`). Commit
and push after every version anyway.

## Done in that session (each pushed, full build EXIT 0)

- 1.3.75 the villagers' own cute skins (finished the WIP; the face's skin matched more widely so cheeks keep their tone).
- 1.3.76 villagers and children blink (the owner: "they should all blink like Touka, Eto and all the kids do"). By reading, the kids never
  blinked before; now 28 of 39 child skins and 9 of 10 villager looks do. **The first try was rejected by the owner** (they marked a dozen
  faces: eyes found by colour, one flat fill, half of daughter_19's face covered). Redone with every eye boxed by hand and the lid grown
  from the skin round it. **Lesson: never judge faces or textures from a thumbnail; zoom every single one (40 px per face pixel) before
  saying it passes, and send the owner the full sheet** (`docs/handoff/blinks-1.3.76.png`).
- 1.3.77 villagers' gestures: Eto's gestures (in Eto's, Ken's or Touka's way by temper) and the kids' talk poses with every answer,
  greetings, dusk, idling; partners' "Kiss (name)." / "Hug (name)." (the talk closes; they walk up; a kiss in two beats with hearts);
  blush overlays per skin.
- 1.3.78 dates: "Let's go for a walk, hand in hand." for partners: glued at the player's right side (EtoHand's way, full-size), arms on
  the hand spring, lines, hearts; never past the village's edge; a shift tap lets go.

The owner's update in `next-ai-1.3.67.md` is now done (1.3.68-1.3.78).

## Untested in game (worth checking when the owner plays)

- Villager skins on the model (HD skins from 64 to 1024 px); blinks and blushes on kids and villagers.
- Gestures on villagers: poses are Eto's/Touka's/Ken's/the kids' own, never previewed on a villager's full-size body (a kid's cue on a
  grown body may look odd: HIPS, PLEAD); the hop/tip translation (EtoRenderer's) on a villager.
- Kiss/hug: the villager walking up to 1.3 blocks (it could bump the player), the talk screen closing (`S2CNpcTalk` with no choices).
- Dates: the glue and the player's arm (`EtoClient.handHeld` now counts villagers), letting go at the village edge, a villager saved
  mid-date (gravity restored on load).

## Next

Whatever the owner reports or asks (bug reports first, each its own version). Optional from older lists: War/Peace same-move cancels.
