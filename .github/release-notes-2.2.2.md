## 2.2.2 (HexGhoul): a grown half-human child's kakuja

HexKagune 2.2.2 for Forge 1.20.1. Install matching versions on client and server. Not tested in game.

A ghoul's and a human's child, once grown (adult), uses its kagune's kakuja in a fight. A Rinkaku child grows two centipede legs out of its back beside its tail, which strike what's in reach by turns. An Ukaku child's wings become Ukaku's first kakuja, and its blows land harder. Other children still never have a kakuja.

- **Centipede legs** (tails):
  - The same motion and mesh as a player's: plates in greys, the flesh between in the child's own kagune colour (`KakujaPalette`).
  - At rest they curl round it; with a target within 5.5 blocks they take turns to rear up and strike.
  - Each strike deals the child's blow × 1.4.
- **Ukaku's first kakuja** (wings): drawn as a player's first kakuja (`WingForm.KAKUJA` through `WingRules.child`), and its blows deal × 1.3.
- **When**: only while it fights with its kagune out, so a grown child at home looks as it always did.
- **Code**: `HalfBlood.kakuja` (pure, tested in `FamilyTests`); `CentipedeRenderer.childLegs`; the strikes in `ChildKagune.fight`. Not tested in game, and not previewed off-game.
