## 4.1.93 (HexGhoul): every Rinkaku and Bikaku sound is Minecraft's own (none the Ukaku's)

HexKagune 4.1.93 for Forge 1.20.1. Not tested in game. Protocol unchanged (127).

You said (quoted exactly): "Okay, now you're gonna do the same sound thing that you just did for ukaku. You're gonna do for both of the
other cognates completely. Replace all sounds. Just make sure not to use the same ones as ukaku. Like that's a no go. The only one you're
allowed to use is the transformation in kakuja, the wither sound, if you want."

The two other kagunes are the Rinkaku and the Bikaku. Every sound of theirs now names one of the game's own and plays exactly that, checked
against vanilla 1.20.1's own sound list. None is one of the Ukaku's (only the Wither's spawn roar is shared, for a kakuja coming on, as you
allowed), and the Rinkaku and the Bikaku don't share any either: each kagune has its own voice.

**How:** the Rinkaku is the original mod's kagune, so most of its sounds come from the original's own code. Every sound that code plays
now goes through one place that plays it as your kagune does (a Rinkaku's, a Bikaku's or an Ukaku's); Owl DNA and Graft DNA keep theirs.

| Rinkaku | Minecraft sound |
|---|---|
| A tendril lashing out | a fishing line's whip crack |
| A heavy lash, the Impale's launch, the Rubble Hurl's throw | a Wither skull loosed |
| A tendril striking, the Impale's stab | a critical hit |
| Bones cracking, crushed; a crush that kills | a bone block hit, an iron golem's crunch, a slime bursting |
| Grabbing, holding and lifting | slime wrapping round, the Warden's tendrils clicking |
| Slamming down, hurling into a wall | a wooden door broken in |
| The Rubble Hurl's rock, its landing | stone torn out, gravel, the Wither breaking through |
| The tendril guard taking a hit, broken | a shield blocking, a shield breaking (its clash kept) |
| Tendrils driven into the ground | a trident striking the ground |
| Blood where the Impale went through | a small slime squish |
| The grab's timing cue | a note block bell |
| The tendrils writhing while out | a sculk sensor clicking |
| Coming out, reshaped, drawn back | a sculk catalyst blooming, sculk spreading, a sculk vein breaking |
| The centipede's chitter | a silverfish |
| The centipede coming on, coming off | a Ravager's roar, a silverfish dying |
| The full kakuja coming on, coming off | the Wither's spawn roar, a beacon going dark |
| A kakuja breaking apart | the Wither breaking blocks |

| Bikaku | Minecraft sound |
|---|---|
| The worm thrown out at them (its bites and lunges) | a trident thrown |
| The dragon's bites | a Ravager's bite |
| A lighter snap, a grab | a fox's bite |
| A bite landing (bones crushed on a heavy one) | a Hoglin's goring |
| Bones crushed | a bone block breaking |
| Anchoring in the ground, slamming down | mud breaking, an iron golem's blow |
| Writhing, hauling | sculk charging |
| Coming out, reshaped, going back | honey tearing |
| Its flesh armour shifting | chainmail |
| Eating, gulping, blood dripping, gushing | eating, drinking, dripstone dripping, splashes |
| The dragon's roar | the Ender Dragon's growl |
| The dragon's rumble while it devours | the Warden digging |
| The Cataclysm bursting out | the Warden's sonic boom |
| The kakuja coming on | the Wither's spawn roar |
| Its body warping (The Walker) | an elder guardian's curse |
| A kakuja breaking apart | the Ender Dragon hurt |

- **NPCs too:** Rinkaku NPCs' tendrils and wild ghouls' Rinkaku lashes sound the same.
- **The clash sounds are untouched**, the Rinkaku guard's own clash included.

### Ukaku sounds 4.1.91 missed (now fixed)

- The original controller still played its own sounds for an Ukaku player: the wings put away, reshaped. Putting them away now sounds a
  fire going out; reshaping, the dragon's flap.
- Wild ghouls' Ukaku feather shots (their flap, the shots, the burning) were still the pack's; now the Ukaku's own.
- A kakuja breaking apart played the pack's glass for everyone; now each kagune's own (the Ukaku's: glass breaking).

### Choices of mine you may want changed

- Koukaku (only some wild ghouls have it, no player path) keeps its blade's sounds.
- The awakening and transformation cutscenes keep their sounds (they're story, not a kagune fighting).

### Other

- `RinkakuSounds`, `BikakuSounds` (every sound in one place each), `KaguneVoices` (the original's sixteen events by kagune).
- Tests: `SfxTests.kaguneVanilla493`; 24 rules sabotaged one at a time (`docs/handoff/sabotage-4.1.93.json`).
