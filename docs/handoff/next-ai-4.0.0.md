# HANDOFF: HexGhoul 4.0.0 "The Great Exploration" (released). READ THIS FIRST.

Branch: `claude/tender-brahmagupta-yhr1ga` (started from 3.7.3, `origin/claude/hopeful-noether-5c2wex` 9c5dcc8). Push only there.

The owner's whole request was the 4.0.0 spec they pasted (new villages, Aogiri hideouts, armour and masks, ranked wild ghouls, Ayato
Kirishima, Noro), as ONE release: **4.0.0, The Great Exploration**. Their files are in `art/exploration/` (the three schematics, Ayato's
and Noro's skins) and `art/outfits/` (the two outfits and 24 masks, canonical geometry; never edit them: the fitting is in the baker).
Release notes: `.github/release-notes-4.0.0.md`. Protocol 105. Not tested in game.

## Where things are

1. Structures: `tools/exploration_structures.py` -> `data/hexkagune/structures/{village_1,village_2,aogiri_hideout}/t_i_j.nbt`,
   `village/SiteMaps.java`; placed by `ControlStructures.Site` (types `hexkagune:site_<id>`, beard_box); old village's structure_set removed.
2. Outfits and masks: `tools/outfit_models.py` (bakes `models/outfits.bin` v2: each cube carries its texture net, so fitted cubes keep
   their texture; `fit_mask`: half masks squashed below the eyes, full masks' eye bands moved to y -3.5, 5% girth; `fit_outfit`: x/z
   drawn in past 0.3 to 45%, Formal Cap out past the hat layer, slim-arm sleeves as bones 6 and 7; the generic masks drawn on a
   quarter-pixel grid: `Face`, `clown()` ... `kabuki()`). Before shipping a change, render the baked cubes (with `faces()`) front, side
   and three-quarter on a Steve-like head and body and look at them (4.0.0 did this with a scratch script, not kept in the repo).
   `outfit/{OutfitModels,Outfits,OutfitItem}`,
   `client/render/{OutfitLayer,OutfitFirstPerson}` (`OutfitLayer.slim(model)` registers an NPC renderer's slim-armed model; players by
   their skin), `OutfitTests`.
3. Investigators in the Formal Outfit (`server/OutfitServer`, `AgentBrain`), Reaper excepted; per-piece drops.
4. Wild ghouls ranked D..SS (`NpcRules`, `NpcIdentity` packs rank/mask/faction), nine generic masks (`Outfits.GENERIC_MASKS`: append
   only, a saved ghoul's mask is its index).
5. Aogiri members (`GhoulNpcBrain`: `HexAogiri`, `aogiriFoe`, comrades' call, hideout leash; mask via `OutfitLayer.faction`).
6. Ayato (Persona AYATO, `AyatoNpc`, `AyatoScript`, `talk/ayato.txt`, Courtship chapters, `NpcWings` kakuja, Ukaku first-kakuja trial
   with cutscene `S2CCutscene.AYATO_KAKUJA`).
7. Noro (Persona NORO, `NoroScript`, Bikaku gates in `BikakuServer`/`quest/Gate`, `NpcWorms` fight, drawn by `BikakuRenderer.drawNpc`).
8. The owner's to-do list: Bikaku regeneration free (`BikakuPotential`, `BikakuMove.regenerates()`, `GhoulRegen`); The Walker
   (`WalkerLegs.shape/carried/raised`, `MawWorm.legRadius`); children (`KaguneGenes.WORM/WORM_TWIN`, `ONE_KIND` a third,
   `Children.both` no longer adds halves, `ChildWorm` fight, `BikakuRenderer` draws a child's worm).

Previews (renders of the baked geometry outside the game, a Steve-like mannequin, the skin's eye row marked red at the sides):
`docs/handoff/generic-masks-4.0.0.png`, `owner-masks-fitted-4.0.0.png`, `outfits-fitted-4.0.0.png` (wide and slim arms),
`walker-legs-4.0.0.png` (leg paths, front and side).

## 4.0.1 (released)

The owner, after 4.0.0, on the outfits: "They seem a little too thick. Can you add more details to it as well to make it as polished and
like not so thick in general?"; "you could add like a color or something to the formal outfit". On The Walker: "the walking is way too
weird, bro. It's too robotic, too slow. Like the steps should be like they should vary in length ... it still feels like it's two sticks
walking"; then "See, there's the issue. One leg at a time, not two in front at once. Come on, bro ... They don't go back like further back
enough or forward enough. This feels very unrealistic, and it's always you can always tell where the next step is gonna be, which is what I
don't want." Done in `tools/outfit_models.py` (`layers`/`through`/`snugger`, `details`, `RECOLOUR`, `finish`) and
`bikaku/WalkerLegs.java` (`update`/`step`: a true alternating stride by chance; `wobble`, `body`, the S `shape`); `WalkerPose` takes the
body's roll and dip from `BikakuRenderer.walkerBody`.

## Next

Nothing outstanding from the owner. If they report in-game problems with the fit, adjust `HALF_TOP`, `MASK_EYES`, `EYE_Y`, `CLEAR`,
`SNUG` in `tools/outfit_models.py` and rebake (`python3 -I tools/outfit_models.py`), never their art files.

House rules as in `next-ai-3.6.6.md` (nothing tested in game; answer in English; quote the owner exactly).

## The owner's words during this session (quoted exactly)

- On the failing CI: "note that all the latest pushed failed!" (fixed: two name checks in the tests predated the owner allowing Ayato and
  Aogiri; the dotted mask's id; every push since builds with EXIT 0 locally first).
- "Ayato romance should be quite similar to kaneki, animations can be the same. Noro is taller than the average player and is very brief,
  barely talks." (Ayato: Ken's style and household words; Noro: size 1.15, nearly silent.)
- "Add to the to-do list: bugs and needed fixes: Bikaku: Sometimes the limbs break and dont regenerate cause of the lack of energy, so let me
  specify, Bikaku users dont have the same regeneration as regular wounds, they either regenerate with this mutation or with their other
  ability to regenerate, wounds appear like any other ghoul but regen for them does not cost energy at all, it works with cooldowns, and
  waiting. The walker legs needs more flluidity and better arching, it walks super robotic, sure it works but it's super ugly to see, the
  legs look like sticks, and dont move naturally. Make sure The child of a Bikaku user with all the other combinations matches perfectly.
  Children: Kids work with chances now, depending on the parents, assuming for example a Bikaku user and a Ukaku user, the child has a
  chance to come out with only 1 kagune type like just 2 ukaku wings, just Bikaku worm tendrils, or both. Armor and Masks: Armor is too
  bulky, if the player is alex model it should adapt to it as well as the male body's muscle shape. Masks are… very very lack luster, half
  masks are covering most of the face instead of being lower, way lower. The other masks are very basic and lack details, not only that
  but the eye position of the masks are very off."
