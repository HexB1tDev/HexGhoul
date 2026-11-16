## 4.1.37 (HexGhoul): the Reaper's change: a real coat taken off, untouchable, a slower draw, the blade held point-down, his blows clashable

HexKagune 4.1.37 for Forge 1.20.1. Not tested in game. Previews: `docs/handoff/arima-change-4.1.37.png` (his whole change in order: the
coat, then the case, then the blade), `docs/handoff/arima-guard-4.1.37.png` (his new guard, standing and running),
`docs/handoff/arima-string-4.1.37.png` (his four blows from the new guard).

Your words: "whenever he's transforming, you can still grab him, which should not be allowed. And I don't know, I feel like transformation
needs to be a little more dramatic. I mean, like when he takes off his jacket, I feel like he should actually like take off an actual
jacket, before switching to the other skin. The taking out the quinque from the case also should take a little longer. It's pretty too
fast." Then: "[Arima] is actually holding the blade upside down, bro. The blade is facing up instead of down ... And he's still very
hittable. Like, I told you to make him completely immortal while he's transforming." Then: "You should be holding that upper bone thing on
the blade always. Mind you that, like, his moves are currently not clashable ... You gotta patch that ... You should take off an actual
jacket. Like, you can use the armor that we got ... just make sure he takes it off, bro. And you can see him taking it off." And: "The
briefcase should come after the jacket comes off".

**What it was:**
- His immunity only stopped the damage (the hurt event), after the game had already accepted the hit: you still got the knockback, the red
  flash and the hurt sound, and every grab, throw, bleed and push that our moves apply on their own still landed. Grabs could catch him.
- The jacket was just a skin swap while his arms were behind him.
- The case opened in 3 ticks and the blade slid out in 5; the whole draw was 1.8 s.
- His guard held the blade raised, point up and rod down, both hands at his belly.
- His blade string and his kicks/blitz never went on the clash board, so nothing could meet them.

**Now:**
- **Untouchable through his whole change (and his end on his knees):** the attack itself is cancelled before the game does anything with it
  (no damage, no knockback, no red flash, no sound); nobody can target him (the original kagune's own target checks say no, so every grab,
  tail and swipe that asks them passes him by, and the quinque arms' Bind too); and every tick he's freed from anything already holding him
  (the kagune's tails, a Bikaku's worms, a Pale's flesh, a Graft's limbs, a bound arm) and pinned where he stands. `/kill` and the void still
  take him. Not tested in game: if some grab still catches him, tell me which key it was.
- **A real coat comes off:** the Formal Overcoat's own 3D model (body, sleeves, tails), repainted white, is on him from the first tick. He
  grips his lapels, pulls it open, and it goes back off his shoulders while its sleeves slide down his arms and off his hands. Free of him,
  it drops behind him, falls flat (empty) on its back on the ground, lies there through his whole draw (where it fell, even when he turns),
  and sinks away before he fights. His look turns bare under the coat right at the start, so you never see the swap. The jacket part is
  a bit slower too (2.3 s, was 1.7 s).
- **Then the briefcase:** only after the coat is on the ground he reaches behind his back for the golden case. The whole draw is now 3.1 s:
  the case opens over 6 ticks (was 3), the blade slides out over 10 (was 5), then he lets go with his left hand and lowers the blade into
  his guard.
- **The blade point-down, like your picture:** right fist on the bone handle (the ribbed grip behind the guard: always where he holds it),
  low at his hip, the blade pointing forward and down along his right side, the rod with its ring back past his hip, his left hand loose.
  Same on his run. Each blow takes the second hand onto the grip over its first 2 ticks and lets go over its last 2.
- **Clashable:** each blade blow and each kick/blitz strike goes on the clash board as it sets out, as hard as it would hit; meet it with
  your M1 and both are thrown apart, nothing lands, his string is broken off. (Your message said "for some reason, you can clash with this
  form": I took it as "can't", since his blade's blows really weren't on the board.)
- **Bug found on the way:** his change waited for him to stand on the ground. In water (or held up by anything) he never "landed", so he
  never went bare and, with the jacket on, could never drop below 90 HP: unkillable. Now it waits at most 3 seconds.

**Watch out:**
- The change is longer (5.4 s from coat to fight, was 3.5 s); he's untouchable for all of it.

### Other

- `tools/ReaperBladePreview.java` (guard, draw, string, shed, case sheets), `tools/reaper_coat.py` (the white coat texture).
- Protocol 117 (unchanged).
