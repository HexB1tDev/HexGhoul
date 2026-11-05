## 2.5.2 (HexGhoul): No hearts when the kagune or a kakuja comes out

HexKagune 2.5.2 for Forge 1.20.1. Install matching versions on client and server. Not tested in game.

No more hearts when the kagune or a kakuja comes out. They were vanilla's damage-indicator particles (dark red hearts) that the original mod's fallback effects drew as "blood" and "mist" where the tails root at your lower back, on every limb tearing out and every landing impact; and a companion partner seeing your kagune also puffed hearts between you (behind you when they follow you). Rinkaku now throws the mod's own blood out of the back as each limb tears out.

- `KagunePatch.noHearts`: every `ParticleTypes.DAMAGE_INDICATOR` in `KaguneManifestationEffects` (fallback blood and origin mist: 2) and `KaguneLandingEffects` (fallback impact: 2) now draws `UkakuBlood.FALLBACK`, a small dark red dust. `KaguneLandingEffects` added to the original entries the build lets change. Checked in the built jar: no damage indicator left in either.
- `UkakuClient.spawnManifestBlood`: `UkakuBlood.spurt` from the back, out and up, instead of the original's fallback.
- `EtoBrain.saw`: a partner still giggles at your kagune, without the hearts.
- The 2.4.8 fix (the regeneration hearts) is confirmed in the jar too.
