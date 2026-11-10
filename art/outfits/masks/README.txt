TOKYO GHOUL — 24 MASK MODELS
Minecraft Java Edition 1.20.1

This is a models-only asset collection based on your supplied 24-mask sheet.
There is no mod JAR, gameplay code, item registration or resource-pack installer.
Oversized vertical side panels have been removed from all models. Face shells
retain their solid thickness and shaped edges; straps attach with short tabs.
Tsukiyama's beak projects forward with a long tapered plague-doctor profile.

WHAT TO OPEN
blockbench/*.bbmodel — 24 individually editable models, with embedded textures.
textures/*.png — matching 256×256 PNG texture atlases, also supplied separately.
geckolib/geo/*.geo.json — GeckoLib 4 compatible armor geometry for Forge 1.20.1.
java_models/*.java — Mojang-mapped 1.20.1 ModelPart geometry classes.
obj/*.obj + *.mtl — portable mask-only meshes; keep the textures folder beside obj.

The three preview sheets show the exported geometry on a fitting mannequin.
The mannequin, eyes, neck and pedestal are NOT included in any mask model.

FITTING
One modelling unit equals one Minecraft player-skin pixel.
The standard head is 8×8×8. The cavity also clears a 0.5-pixel head overlay.
Canonical head-local coordinates: x = left/right, y = up, front = negative z.
The neck/head pivot is (0,0,0); the bare head occupies x/z -4…4 and y 0…8.
The reference skin's low eyes are represented around x ±2.12, y 2.15.
Half masks leave x -3.2…-1.0 and 1.0…3.2, y 1.0…3.8 unobstructed.
The narrow nose bridge can rise between those areas. Lower jaws extend slightly
below the chin to preserve their details without covering the eyes.

HALF MASKS
Kaneki Ken, Ayato Kirishima, Tatara, Shachi, Rize Kamishiro, Yomo Renji,
Yoshimura and Nishiki Nishio.

FULL MASKS
Touka Kirishima, Noro, Yamori/Jason, Eto, Bin Brothers, Roma Hoito, Uta, Itori,
Tsukiyama Shuu, Kanae von Rosewald, Donato Porpora, Koori Ui, Koutarou Amon,
Seidou Takizawa, Mado Akira and Arima Kishou.
These retain full forehead, cheeks and chin coverage. Eye openings, where the
reference has them, are lowered. Noro and Itori retain their closed faces; Eto
and Tsukiyama retain the central decorative eye rather than adding two new holes.

BLOCKBENCH
Open a .bbmodel directly. All actual geometry is below armorHead in named groups.
These are ordinary cuboid models with box UVs and an embedded PNG; no plugin is
needed to edit them. The project uses the built-in Bedrock model container so
its geometry can also be exported for GeckoLib; the intended game is Java 1.20.1.
Do not convert the project to Java Block/Item: that format has different bounds.

GECKOLIB 4 / FORGE 1.20.1
Use the matching geo and texture in your mod's assets/<modid>/geo and
assets/<modid>/textures/masks folders. Bind to GeoArmorRenderer's armorHead bone.
The geo files have armorHead at (0,24,0), with face cubes around y 24…32.
This is a model coordinate convention, not a request to move the player's head.
X is reflected in the geo export to match Blockbench's Bedrock codec.
The JSON identifier is geometry.<mask_id>; no animation is necessary for a
static mask that follows the renderer's head bone.

DIRECT JAVA MODELS
The supplied classes use net.minecraft.client.model.geom and Mojang mappings.
Each createBodyLayer() returns a 256×256 LayerDefinition. Bake it and construct
the matching class. Its public head ModelPart has head-local coordinates and
can copy the player's head pose in your existing armor/layer renderer.
The render method renders that part only. Registering an item or renderer is
outside this models-only collection. Choose the Java OR GeckoLib route for a
given renderer; both contain the same shape.

VALIDATION
All 24 models were checked for positive solid cube volume, linked textures,
UV bounds, consistent exports and head cavity clearance. Every half mask passed
the low-eye clearance test. Preview sheets are Blender renders of the model
geometry. Minecraft gameplay and renderer integration have not been tested.
models_manifest.json lists every model, its coverage, bounds and element count.
validation.json records the checks. SHA256SUMS.txt lists the packaged files.

Format references:
https://www.blockbench.net/wiki/docs/bbmodel/
https://github.com/bernie-g/geckolib/wiki/Geckolib-Armor-(Geckolib4)
