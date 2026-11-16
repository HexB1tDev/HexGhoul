## 4.1.19 (HexGhoul): quinques in first person move exactly as your body swings them

HexKagune 4.1.19 for Forge 1.20.1. Not tested in game. Previews: `docs/handoff/katana-first-person-4.1.19.png` (each katana cut in
third person over the same moments in first person), `docs/handoff/quinques-first-person-4.1.19.png` (the spear, the mace, the blade and
Dawn the same way).

Your words: "don't forget the the first person animation, like first person view to match the animation that they got, because they
currently don't have that."

**What it was:** in first person a quinque sat at a fixed spot in front of the camera and rocked through one of three stock motions (a
wave, an overhead, a thrust), the same for every weapon and every hit. Nothing like what your body did.

**Now:** the katana, the spear, the mace, the blade and Dawn are placed in first person by the same arm, hand, model and transforms as in
third person, frame by frame, and your body's step carries them. So every cut you see in first person is the cut your body makes:

- **The view:** it's taken from just behind your head, looking a little down, zoomed in to a held weapon's size. From your actual eyes
  the cuts happen a block below the screen and the hilt fills the view, so that was tried and dropped.
- **The katana** rests just right of the crosshair. The kesa-giri goes up over your head and comes down in a diagonal to the lower left.
  The gyaku-kesa rises to the right. The yoko-giri sweeps flat across the middle of the screen. The tsuki drives away from you, and the
  finisher's sweep crosses the whole view (its full-body spin is left out: the camera doesn't spin with you).
- **The spear** points down the crosshair and drives forward on each thrust. **The mace** smashes down onto the crosshair. **The blade** and
  **Dawn** sweep across the view.
- **The draw:** the weapon slides out of its case in view, right of the crosshair.
- Checked: at rest every one is in view, right of and below the crosshair. Through every swing part of it is always in view, every
  impact is in view, and no part of it comes within a block of the camera.

**Not yet:**

- **The mace's second swing** winds up behind your head (in third person too: the old swing). For a moment its head passes over the
  view. The mace's string is rewritten next.
- **The launcher** keeps its own first person: it aims along the crosshair.
- **The look:** the katana's checkered hilt and every quinque's boxy shape are the next job (your words: "every weapon in the game should
  look like it's made from a kagune").

### Other

- Protocol 117 (unchanged).
