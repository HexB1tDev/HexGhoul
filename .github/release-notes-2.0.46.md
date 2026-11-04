## 2.0.46 (HexGhoul): male players get the White Reaper's build

HexKagune 2.0.46 for Forge 1.20.1. Install matching versions on client and server. Not tested in game.

Players who pick male on the gender screen now have the White Reaper's build. The upper torso is drawn a little bigger over their own: broader across the shoulders and chest, the chest standing out in front of the belly, tapering into the normal waist, in their own skin and jacket layer. The arms hang slightly further out to clear it. Players who haven't picked a gender look as before, and female players keep their figure.

- `BuildLayer` (new) draws `ReaperBuild`'s chest on a man's torso as it is posed (upper-body bend carried, as the female bust is). The body layer sets the arms 0.75 px out (`KaguneBodyAnimation`, as it spreads a woman's legs; PlayerAnimator resets pivots every frame). New `Genders.male`.
- Not drawn under a chestplate, nor while something fitted to the usual torso is grown over it: Ukaku's kakuja armour, Graft's armour, the King's forms or the turning into one. The jacket layer follows the skin's jacket setting, and is also drawn when 3D Skin Layers replaces the flat jacket.
- The Rinkaku emergence mark lies on a man's broader back instead of under it: cut into bands that follow the build's back (`ReaperBuild.back`, tested).
- Previews: `docs/handoff/male-build-2.0.46.png` (before and after). `docs/handoff/kagune-showcase-2.0.46.png` shows the current Rinkaku kagune on a body, drawn with the game's own tail mesh, markings, veins, mark and colours at the game's real tail thickness (new `tools/KaguneShowcase.java`). Protocol stays 78.
