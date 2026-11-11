## 4.1.21 (HexGhoul): the katana's five cuts, hands in front, a real diagonal

HexKagune 4.1.21 for Forge 1.20.1. Not tested in game. Previews: `docs/handoff/katana-cuts-final-4.1.21.png` (all five cuts, front, his
right side and from above, through each swing), `docs/handoff/katana-kesa-4.1.21.png` (kesa-giri from four sides),
`docs/handoff/katana-first-person-4.1.21.png` (first person).

Your words: "I don't understand what's so hard about making an animation with the fucking katana, bro. Holy shit. Look at how it's
pointed. Look at the hands, bro." And: "The katana's M1 cuts still look bugged: the hands bunch at the chest and the blade points wrong.
4.1.18 had the hands behind the back."

**What it was:** each cut said where the blade should point and roughly where the hands go, and the solver cared about the blade a
thousand times more than the hands. A katana held in a stiff Minecraft arm sits at one fixed angle to it (66 degrees), so to point the
blade where asked it swung the arm back through his body: hands behind his back on kesa-giri's follow through, gyaku-kesa's wind-up and
yoko-giri's follow through. The rejected 4.1.21 attempt bent the wrists hard and pulled the hands into his chest.

**Now:**
- **Hands first:** each key puts the hands at the end of a full arm, out in front of him; they go exactly there, never behind him.
- **Wrists, a human amount:** the blade turns in the fist (up to about 45 degrees one way, 20 the other), so as a cut lands the blade
  comes into line with the arms, as a real one does. Drawn in your hands, an investigator's hands and first person.
- **The cuts, rewritten and checked on renders from the front, the side and above:**
  - kesa-giri: up over the right shoulder, straight down through the middle to low on the left, finishing with the blade past the
    left hip;
  - gyaku-kesa: from low on the left, rising across to high on the right;
  - yoko-giri: blade out level to the right, swept flat across to the left;
  - tsuki: drawn back by the right hip, driven straight down the line with a long lunge;
  - kaiten-giri: the whole body spun round once with the blade level.
- **Smooth:** the arm never whips round faster than 0.9 radians in 2% of a swing (now measured as the arm's real turn), and the wrists
  never bend past their range.

**Not yet:** the other quinques keep their looks and cuts; their first person comes next.

### Other

- Protocol 117 (unchanged).
