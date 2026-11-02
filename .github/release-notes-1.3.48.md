## 1.3.48 (HexGhoul): children use their parents' whole kit: crystal wings and spikes, the tentacle's grab, throw and crush, back and front flips (never a kakuja) (testing)
HexKagune 1.3.48 for Forge 1.20.1. Install on client and server (**protocol 68**, unchanged). Testing build: compiled, tests pass, **not tried in game**.

- **Crystallize.** A child's wing or wings turn to crystal a second after they come out, as Touka's do: the crystal sound, the burst at its back, the wings drawn in crystal. Its shots become **crystal spikes** (harder than fire shots, no burning; drawn as Touka's spikes). Every so often it throws a **fan of five spikes** at once. Two wings fan from both; one wing from its side.
- **Grab, throw, crush.** A child with a tentacle can **grab** what it fights if it's no bigger than a person (a zombie or skeleton, not a spider or a golem):
  - It shoots its tentacle out, winds it round the target, hauls it up in front of itself and **squeezes** it every few ticks. While held, the target can't act.
  - Then it either **throws** it far away (three times in five), or lifts it high and **crushes** it into the ground on the guillotine's slam, with a shockwave.
  - If it misses or the target gets away, the tentacle comes back.
- **Flips.** Like the wild ghouls: a **back flip** out when something is right on top of it, and a **front flip** in over a gap when its tentacle can't reach yet. The whole body turns over in the air.
- **Never a kakuja.** None of this grows a kakuja; children never get one.
- Everything still lands only on what it fights and on monsters, never on a player, a companion or another child. When the fight ends the wings go back to normal, and anything it was holding is let go.

Untested in game: compiled and unit tested only; the grab's hold and the crystal wings were checked offline (`tools/ChildKagunePreview.java`).
