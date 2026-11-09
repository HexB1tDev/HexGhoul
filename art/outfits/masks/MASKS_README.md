# 24 Tokyo Ghoul masks — reference-sheet update

Added to the existing Crimson & Formal Armor package. The two armor sets and their original item IDs remain included. The mod is now version 1.1.0 for Minecraft 1.20.1 / Forge 47.x.

## Install and equip

Replace the previous `referencearmor-1.0.0.jar` in your `mods` folder with `mod/referencearmor-1.1.0.jar`. Keep one version of this mod installed. On multiplayer, use the same version on clients and server.

All 24 masks are separate helmet-slot armor items in the **Tokyo Ghoul Masks** creative tab. They use vanilla iron helmet protection and durability, with no added abilities. A mask and a hood both occupy the helmet slot, so Minecraft equips one at a time. The chestplate, leggings and boots can be worn with a mask.

## Assets

Every named mask folder contains an editable `.bbmodel` with an embedded texture, a GeckoLib `.geo.json` using `armorHead`, the 512 × 512 PNG atlas, and canonical `.model.json` geometry. The game uses the same cube positions, dimensions, pivots, rotations and UVs through native Forge armor models. No GeckoLib dependency is required by the supplied mod.

`Mask_Collection.blend` contains all 24 masks, one rigid head rig per mask, packed textures and separate `*_PREVIEW_MANNEQUIN_ONLY` collections. Mannequin heads, eyes, necks and shoulders appear only in previews. They are excluded from the wearable exports and the mod. The mask geometry follows the wearer's original head without replacing their skin, face or hair. A mask can physically conceal parts of the skin. Recessed black fabric is used inside specified dark eye openings and vents.

`Mask_Model_Preview.png` shows every model from the three-quarter view. `Mask_Front_Views.png` shows every front view. Individual renders are in `previews/`. These are renders of the actual exported geometry, not generated concept illustrations or in-game screenshots.

## Reference and fidelity

The supplied final **6 × 4 sheet containing 24 masks** is the visual reference; a copy is in `reference/`. The earlier, larger mask collage is superseded by this sheet. Names and variants follow the final image. Ayato uses the lower-face respirator shown here and has no rabbit ears. Touka has the tall white rabbit ears and pink insets. These are faithful reconstructions from the visible views; tiny markings and unseen backs are interpreted. A 100% or pixel-exact match cannot be certified from this single concept image.

The masks use real 3D shells, cutouts, wraps, straps, nose pieces, teeth, ears, beaks, metal hardware and thin marking geometry. Eye sockets and vents are openings in the outer shell, with recessed lining where the reference shows darkness. Purple, gold, red and black markings follow the shapes in the sheet. The player head remains intact underneath.

## Index and commands

| Reference cell | Mask | Item ID |
| --- | --- | --- |
| 01 | Kaneki Ken | `referencearmor:mask_kaneki` |
| 02 | Touka Kirishima | `referencearmor:mask_touka` |
| 03 | Ayato Kirishima | `referencearmor:mask_ayato` |
| 04 | Noro | `referencearmor:mask_noro` |
| 05 | Yamori / Jason | `referencearmor:mask_yamori` |
| 06 | Eto | `referencearmor:mask_eto` |
| 07 | Tatara | `referencearmor:mask_tatara` |
| 08 | Bin Brothers | `referencearmor:mask_bin_brothers` |
| 09 | Roma Hoito | `referencearmor:mask_roma` |
| 10 | Shachi | `referencearmor:mask_shachi` |
| 11 | Rize Kamishiro | `referencearmor:mask_rize` |
| 12 | Uta | `referencearmor:mask_uta` |
| 13 | Itori | `referencearmor:mask_itori` |
| 14 | Yomo Renji | `referencearmor:mask_yomo` |
| 15 | Yoshimura | `referencearmor:mask_yoshimura` |
| 16 | Nishiki Nishio | `referencearmor:mask_nishiki` |
| 17 | Tsukiyama Shuu | `referencearmor:mask_tsukiyama` |
| 18 | Kanae von Rosewald | `referencearmor:mask_kanae` |
| 19 | Donato Porpora | `referencearmor:mask_donato` |
| 20 | Koori Ui | `referencearmor:mask_koori` |
| 21 | Koutarou Amon | `referencearmor:mask_amon` |
| 22 | Seidou Takizawa | `referencearmor:mask_seidou` |
| 23 | Mado Akira | `referencearmor:mask_akira` |
| 24 | Arima Kishou | `referencearmor:mask_arima` |

```mcfunction
/give @s referencearmor:mask_kaneki
/give @s referencearmor:mask_touka
/give @s referencearmor:mask_ayato
/give @s referencearmor:mask_noro
/give @s referencearmor:mask_yamori
/give @s referencearmor:mask_eto
/give @s referencearmor:mask_tatara
/give @s referencearmor:mask_bin_brothers
/give @s referencearmor:mask_roma
/give @s referencearmor:mask_shachi
/give @s referencearmor:mask_rize
/give @s referencearmor:mask_uta
/give @s referencearmor:mask_itori
/give @s referencearmor:mask_yomo
/give @s referencearmor:mask_yoshimura
/give @s referencearmor:mask_nishiki
/give @s referencearmor:mask_tsukiyama
/give @s referencearmor:mask_kanae
/give @s referencearmor:mask_donato
/give @s referencearmor:mask_koori
/give @s referencearmor:mask_amon
/give @s referencearmor:mask_seidou
/give @s referencearmor:mask_akira
/give @s referencearmor:mask_arima
```

## Verification and source

The Forge 1.20.1 build and reobfuscation passed. Minecraft's own model-baking classes successfully baked all eight armor pieces and all 24 mask models. Checks verify that every mask cube belongs to the helmet's head bone and that the exported model counts, texture UV coverage, item textures and item definitions agree. See `mask-validation.txt` and the package's `build-verification.txt`.

An interactive Minecraft gameplay session was not run. Check your particular skin and animation mods in-game for clipping and appearance under game lighting.

Rebuild the complete mod using Java 17 and `./gradlew build` inside `source/`. Run `./gradlew verifyArmorModels` to repeat the native model-baking checks. `tools/generate_masks.py` regenerates the 24 assets, geometry classes, registry catalog, inventory icons and names from its individual model specifications; `tools/render_masks.py` produces the previews and Blender collection; `tools/validate_masks.py` validates the asset files.
