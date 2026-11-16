## 1.3.46 (HexGhoul): a child of a Rinkaku and an Ukaku parent is born with one of three looks: over either shoulder, or a tail with both wings (testing)
HexKagune 1.3.46 for Forge 1.20.1. Install on client and server (**protocol 68**, unchanged). Testing build: compiled, tests pass, **not tried in game**.

- **Three ways to carry it.** A child with a Rinkaku tentacle is born with one of these, and keeps it all its life (decided by its seed, so children from before get one too):
  - **Over the right shoulder** (40%): the tentacle leaves the right of the lower back and rises over the right shoulder; one Ukaku wing on the left (as in 1.3.45).
  - **Over the left shoulder** (25%): the mirror of that, with the wing on the right.
  - **A tail** (35%): the tentacle leaves the middle of the lower back and stays behind like a tail: out back at waist height, the end curling up, swaying side to side. It never goes over a shoulder, which leaves room for **both Ukaku wings** above it.
- **Fighting** follows the look: a tail child shoots from both wings in turn, a shoulder child from its one wing. Lashes start from wherever the tentacle leaves the body.
- A child with only a tentacle (two Rinkaku parents, or a Rinkaku and a human) gets the same three ways to carry it.
- `/kagune children` now says how each child carries it, for example "a Rinkaku tentacle as a tail and Ukaku wings".

Untested in game: compiled and unit tested only; checked offline with `tools/ChildKagunePreview.java` (each look from the front, behind and the side, and the tail's lashes).
