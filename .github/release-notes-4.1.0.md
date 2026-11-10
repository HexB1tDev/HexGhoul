## 4.1.0 (HexGhoul): Ayato's way of bringing the kagune out

HexKagune 4.1.0 for Forge 1.20.1. Not tested in game.

Your words: "after training with Ayato (Ukaku kakuja quest) the player gains a new pull out kagune animation sort of like Ken gives the player. the player lifts both hands god like and then they're unleashed."

### Ayato's way

- **Who:** once Ayato has taught you the Ukaku's First Kakuja, bringing your kagune out is his way, like the finger crack Ken gives his students.
- **The lift:**
  - You rise a little onto your toes as you breathe in.
  - Both arms lift out wide from your sides, a little below the shoulders, elbows soft, palms turned open to the sky, chin up. Calm, like a god.
  - A second in, your wings are unleashed behind you, with the Ukaku's own sound and a burst of air.
  - The arms come back down.
- **Everyone sees it:** other players see it too.
- **Unchanged:** Noro's students (Bikaku) still tilt their heads. Ken's finger crack stays Ken's, for his Rinkaku kakuja students.

### My guesses

- An Ukaku player taught by both Ayato and Ken (Ken teaches the Ukaku final kakuja) brings the kagune out Ayato's way: he is the Ukaku's own teacher.
- The timing: the arms out by ¾ of a second, the wings at 1 second, all over by 2.

### Network and tests

- **Network:** protocol 109 (a new effect for the lift), so clients and the server must both be on 4.1.0.
- **CompanionTests:**
  - the lift's shape: arms out wide, palms up, chin up, the wings while it's held, back down after;
  - Ayato's way comes after Noro's and before Ken's.
- **The full suite.**

Validation: the full `python3 build.py` completed with EXIT 0 on these sources before this was pushed. Not tested in game.
