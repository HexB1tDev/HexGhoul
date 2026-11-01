## 1.3.26 (HexGhoul): Touka's wings show again; quinques reworked: a bigger mid-grip spear, a two-handed katana, a flesh mace, a crystal launcher, custom M1s (testing)
HexKagune 1.3.26 for Forge 1.20.1. Install on client and server (**protocol 63**, unchanged). Testing build: compiled, tests pass, not tried in game.

- **Fix: Touka's kagune never showed.** She fought fine, but her wings were drawn as if both had just been thrown away as projectiles, so nothing appeared on her back. They come out now, crystallize as she throws, and a test keeps it that way.
- **Five quinques now**, each a case that opens into its weapon (right click), shuts back with Sneak + right click, and animates both ways:
  - **Bikaku Quinque Blade:** one-handed, quick. 8 damage.
  - **Koukaku Quinque Spear (reworked):** much bigger (about three blocks), **gripped in the middle with both hands**, a broad plated head. Agile: the fastest swing, **2 blocks more reach**, and 8% faster on your feet while it's out.
  - **Bikaku Quinque Katana (new):** held in **both hands** in a kamae, a long hilt, a slightly curved dark blade with a glowing red edge. 9 damage.
  - **Rinkaku Flesh Mace (new):** a long **two-handed** handle and a huge lump of living flesh for a head, bone spikes through it. 14 damage, slow.
  - **Ukaku Crystal Launcher (new):** a crystal machine gun. **Hold right click** to fire crystal shards (5 a second); about 6 seconds of continuous fire overheats it for 3 seconds, while bursts never do. Held level at the hip.
- **Custom M1s.** Every melee quinque has its own three-hit string with its own swings (the katana cuts down, up, then wide; the mace swings side to side, then smashes overhead; the spear thrusts and lunges; the blade slashes across and back, then chops). The **third hit in a row is a finisher**: the blade knocks back, the katana's wide cut also hits everything in front, the spear's thrust pierces everything in its line, and the mace's smash slams everyone around the target up into the air.
- **Two-handed stances:** the spear, katana, mace and launcher are held in both hands, the left hand on the lower grip. Each stance was worked out on the real player model so both hands sit on the weapon. Your arms rise into the stance as the case opens and drop out of it as it shuts.
- Quinques still hit ghouls harder and stop a ghoul player's regeneration for a few seconds.
