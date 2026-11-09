# HANDOFF: HexGhoul 4.0.0 "The Great Exploration" (in progress). READ THIS FIRST.

Branch: `claude/tender-brahmagupta-yhr1ga` (started from 3.7.3, `origin/claude/hopeful-noether-5c2wex` 9c5dcc8). Push only there.

The owner's whole request is the 4.0.0 spec they pasted (new villages, Aogiri hideouts, armour and masks, ranked wild ghouls, Ayato
Kirishima, Noro). They asked for ONE release: **4.0.0, The Great Exploration** (not one version per feature this time). Their files are in
`art/exploration/` (the three schematics, Ayato's and Noro's skins) and `art/outfits/` (the two outfits and 24 masks, canonical geometry).

## Done (each an unversioned commit, quick-checked; no full build yet)

1. Structures: `tools/exploration_structures.py` -> `data/hexkagune/structures/{village_1,village_2,aogiri_hideout}/t_i_j.nbt`,
   `village/SiteMaps.java`; placed by `ControlStructures.Site` (types `hexkagune:site_<id>`, beard_box); old village's structure_set removed.
2. Outfits: `tools/outfit_models.py` -> `models/outfits.bin`, textures, icons; `outfit/{OutfitModels,Outfits,OutfitItem}`,
   `client/render/{OutfitLayer,OutfitFirstPerson}`, items in `ControlItems` (Combat tab), `OutfitTests`. Build's name guard no longer bans Ayato.
3. Investigators wear the Formal Outfit (`server/OutfitServer`, `AgentBrain`), Reaper excepted; per-piece drops.
4. Wild ghouls ranked D..SS (`NpcRules`, `NpcIdentity` packs rank/mask/faction), generic masks; `NpcTests` rewritten.
5. Aogiri members (`GhoulNpcBrain`: `HexAogiri`, `aogiriFoe`, comrades' call, hideout leash; mask via `OutfitLayer.faction`).

## Still to do (in this order)

6. Ayato (Persona AYATO, `AyatoNpc`, script, romance lines in every per-companion table, Ukaku wings stronger, kakuja below a third of
   his health, questline: respect lessons then the Ukaku first kakuja as a TRIAL with its own cutscene; Touka no longer teaches it).
7. Noro (Persona NORO, not romantic, quiet; Bikaku gates taught by him; three worms in a fight via S2CBikakuState with his UUID and
   BikakuRenderer drawing NPCs; undying regeneration).
8. Version 4.0.0 (both version strings, protocol bump), release notes `.github/release-notes-4.0.0.md`, README, full `python3 build.py`.

House rules as in `next-ai-3.6.6.md` (nothing tested in game; answer in English; quote the owner exactly).
