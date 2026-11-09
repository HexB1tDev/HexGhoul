# Crimson Hooded Coat & Grey Formal Armor

Minecraft Java Edition 1.20.1 / Forge 47.x. Created from the supplied concept sheet and written specification.

## Install

Copy `mod/referencearmor-1.1.0.jar` into your Forge 1.20.1 `mods` folder. No GeckoLib or other rendering dependency is required by this standalone mod. For multiplayer, install the same mod on the server and clients.

Open the **Crimson & Formal Armor** creative tab, or use:

```mcfunction
/give @s referencearmor:crimson_helmet
/give @s referencearmor:crimson_chestplate
/give @s referencearmor:crimson_leggings
/give @s referencearmor:crimson_boots
/give @s referencearmor:formal_helmet
/give @s referencearmor:formal_chestplate
/give @s referencearmor:formal_leggings
/give @s referencearmor:formal_boots
```

Equip all four pieces for the complete silhouette. The chestplate contains the upper coat and sleeves; the leggings contain the trousers, long coat panels, and, on the formal set, the lower pockets. The helmet contains only the hood or grey cap. The boots are separate. Armor protection and durability use vanilla iron values; there are no new powers or progression rules. Pieces are available through Creative or commands; no crafting recipes are added.

## Model files

`models/crimson_full.bbmodel` and `models/formal_full.bbmodel` open directly in Blockbench with embedded textures. Separate helmet, chestplate, leggings, and boots projects are provided for each outfit. The `*.geo.json` copies use the GeckoLib armor bone names (`armorHead`, `armorBody`, `armorRightArm`, `armorLeftArm`, `armorRightLeg`, `armorLeftLeg`, `armorRightBoot`, `armorLeftBoot`) for integration into another mod. Those JSON files are assets, not a standalone resource pack; a corresponding renderer and item registration are needed when using them independently.

`Reference_Armor.blend` contains the same cuboids, packed textures, two rigid player rigs, studio lighting, and separate preview mannequin collections. Mannequin heads, bodies, and hands are for display only. They are excluded from all armor model exports and the mod's wearable geometry. Hide the `*_PREVIEW_MANNEQUIN_ONLY` collections to inspect clothing alone.

`models/*.model.json` is the canonical geometry description: Minecraft model units (16 units per block), Java Y-down coordinates, separate player bone pivots, explicit cube pivots/rotations, slot ownership, and UV offsets. The same geometry generates the Java model, Blockbench, and GeckoLib exports. Textures are 512 × 512 RGBA with nearest-neighbor pixel detail.

## Design choices

The crimson outfit has exactly four raised clasp assemblies, an oversized forward hood, opaque recessed black fabric lining, layered shoulders and cuffs, and divided calf-length coat panels. The black lining conceals the skin's face without modifying or replacing the player's head.

The formal outfit has a shallow grey cap, separate raised lapels, crimson tie and knot, offset front closure, raised hip pockets, layered shoulders, and exactly three metal bands per forearm. The cap leaves the actual player's face visible. Neither outfit changes the player's skin, hair, hands, or underlying model.

Coat panels are attached to the corresponding leg and shoulders/sleeves to the corresponding arm. This preserves vanilla movement and allows pieces to be worn independently. These are rigid Minecraft model parts, not simulated cloth: large leg swings, sneaking, riding, or additional animation mods can produce overlap. The standalone mod uses vanilla armor rendering; it does not add a separate first-person sleeve renderer. The 4-pixel sleeves also fit over slim player arms with extra space inside.

The image contains small differences between its individual views. The written specification takes priority, including four clasps. Hidden interiors and dimensions are reconstructed from the available views; this is a close modeled interpretation rather than a provable dimension-exact copy. Preview images are Blender renders of the exported geometry, not in-game screenshots.

## Source and rebuilding

`source/` is a complete Forge MDK project with Gradle wrapper. Use a Java 17 JDK and run `./gradlew build` (Windows: `gradlew.bat build`). The installable, remapped artifact is written to `build/libs/referencearmor-1.1.0.jar`.

The standalone mod uses native `HumanoidModel` geometry and Forge's `IClientItemExtensions.getHumanoidArmorModel` hook. Copying these assets into an existing mod does not update or replace that mod automatically. There has been no GitHub push or change to an existing HexGhoul branch.

`tools/generate_models.py` regenerates both model sets, textures, and Java geometry. `tools/setup_project.py` generates the registration and client renderer files from a Forge MDK. `tools/render_models.py` generates previews and the Blender scene. `tools/validate_models.py` checks cube counts, UV coverage, grouping, the separate armor slots, clasp count, and bracer-band counts.

Forge setup reference: https://docs.minecraftforge.net/en/1.20.1/gettingstarted/

## Verification

See `geometry-validation.txt` for geometry and UV validation. Build verification is recorded in `build-verification.txt`. An interactive Minecraft gameplay session has not been run; player motion, additional mod compatibility, and final in-game lighting need checking in your pack.


## Added masks — version 1.1.0

This archive now also contains the 24 masks from the final 6 × 4 reference sheet. Open `masks/MASKS_README.md` for the full index, commands, model files and preview sheets. Equip them from the new Tokyo Ghoul Masks creative tab. The original two armor sets are preserved. The installable mod is `mod/referencearmor-1.1.0.jar`.
