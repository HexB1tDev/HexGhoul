## 3.5.2 (HexGhoul): Fix: Bikaku's bites heal again (Predatory Feeding)

HexKagune 3.5.2 for Forge 1.20.1. Not tested in game.

Found by reading the code (not reported): since 3.4.9, a Bikaku bite that lands was meant to give back half a heart, but it never did. A ghoul's regeneration cancels every heal of 1.5 health or less that it didn't make itself (that's how food's natural regeneration is kept from healing a ghoul), and a bite's half a heart is 1 health. The bigger heals (Devouring Maw's catch, a kill) were not affected.

- **Fixed:** every Bikaku heal (each bite of the M1 string, Devouring Maw, Predatory Drag, Ravenous Pursuit, Carrion Eruption, and the bonus for a kill) now goes through the ghoul's regeneration as its own heal, so none is cancelled.
- Nothing else changes: the amounts are the same (half a heart a bite, a heart on Devouring Maw's catch, two more hearts on a kill).
- Network: protocol 95, unchanged. No new Minecraft API.
- Tests: BikakuTests (a bite's heal is one the ghoul's regeneration would cancel unless it is marked as the mod's own).

Validation: full python3 build.py completed with EXIT 0 (javac --release 17), including the regression suites, API/bytecode and archive checks, and dedicated-server loading checks. Protocol 95. Not tested in game.
