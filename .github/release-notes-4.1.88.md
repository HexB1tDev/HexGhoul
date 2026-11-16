## 4.1.88 (HexGhoul): Kick Master's own stance: side-on, the hands low and loose

HexKagune 4.1.88 for Forge 1.20.1. Not tested in game. Protocol unchanged (127).

You said (quoted exactly): "Next update is going to be the hand positioning of the combat styles. Currently, both fighting styles have both
fists up, like as if they want to square up. I don't think it's very nice. I think that the kick master should give you a different
stance, especially on the arms. That's it."

Both styles shared one guard: square, both fists up in front of the chest. It is where every blow started from and came back to, held
for a second after each one (her legs her own under it), and in first person it put both fists in front of your view. Kick Master's kicks
held their arms the same way (the fists a little further from the chin).

- **Kick Master has its own stance now, a kicker's, not a boxer's:** side-on (the right shoulder drawn back, the left side to what you
  face), tall and light, the weight a little back, **the hands low and loose**: the lead (left) hand out in front at belt height, the
  rear one easy by the right hip. The head turns back against the side-on chest, so the face looks where you face.
- **Every blow of the string starts from it and comes back to it** (a left-hand blow too, never the mirror of it), and it is what is held
  for that second after a blow.
- **The kicks keep the hands low through them** (the round kick, the side kick, the spinning back kick, the spinning hook kick, the
  tornado kick, and the dash kick): no more fists by the chin while kicking. The punches (the jab, the cross, the hook) still bring a fist
  up to throw it, then drop back into the stance.
- **The dash kick's charge** (right click held) sinks into its coil with the stance's low hands, and the dash kick ends in the stance,
  facing left.
- **In first person** the fists are no longer in front of your view between blows; only a hint of the lead hand at the bottom.
- The General and Roma (Kick Masters) kick the same way and keep the stance after their blows; the White Reaper's kicks start from it too.

### Choices of mine you may want changed

- Speedster keeps the fists-up guard (you asked for Kick Master's to change).
- The V kick (the Speed Kick, everyone's) still ends in the guard; the Big Punch too.

### Other

- Renders: `docs/handoff/kick-stance-4.1.88.png` (the guard both styles had, then the new stance, from the front, front-left, each side,
  behind and in first person; the jab out of it; each kick's end back in it; kept while walking; the dash kick's charge) and
  `docs/handoff/kick-master-4.1.88.png` (every kick from the side and 3/4, the hands low). `tools/PalePreview.java <dir> kick-stance`.
- Tests: `CompanionTests.kickStance488`; 20 rules sabotaged one at a time (`docs/handoff/sabotage-4.1.88.json`), and the generator's
  two (the kicks' arms as the guard's, the stance's hands up) broken by hand and regenerated.
