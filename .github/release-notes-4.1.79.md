## 4.1.79 (HexGhoul): the gangs' ways in: each step a trial of its own, a scene with the leader at the end

HexKagune 4.1.79 for Forge 1.20.1. Not tested in game. Protocol unchanged (126).

Your to-do, its final step (quoted exactly): "Add to the to-do list that the final step is to better the quests, to include everything in the
mod + minecraft, daily  and romance quest betterment, so it's more fun, less boring, same for the joining gang quests." The plan shown to you
(you said "whatever you think is best"): the dailies (4.1.78), then the gangs' joining, each chain 3 to 4 steps, each a different kind, a
trial of its own, a scene with the leader at the end; then romance. This is the joining. Every way in used to be "the right answer, then kill
N of someone (and bring one thing)".

- **Aogiri (Ayato).** He wants a fight first: **a spar** with him, here and now. Get him to half his health before he gets you under a
  quarter of yours. Nobody dies: his blows stop at your quarter, yours at his half, and the blow that would cross it ends the spar. No bond
  lost for hitting him (even if you're his partner). Then **an ambush**: a CCG pair walking their beat 40 to 70 blocks off (they glow so you
  can find them). Your first blow starts a 30 s clock; both down in time, or they call it in. Then back to him: **his scene** (arms crossed,
  he tosses you the mask).
- **The Clowns (Roma).** Make her laugh (kept). Then **a show in her ring**, at night: three volunteers, one at a time, each down within 30 s
  of stepping in, and you never leave the stage (14 blocks round her). Then **a job in her face**: she lends you one of the Clowns' masks;
  kill an investigator while wearing it ("No face, no show!" if you don't), and bring her his quinque case. **Her scene**: the allium in the
  briefcase.
- **The Gas Masks (Banjou).** Take him seriously (kept). Then **the gas**: go out past his gate (16 blocks from him) and the boys open the
  valves; walk back to him through it (poison, slowness, nausea, smoke) within a minute, with no gas mask on. Then **the gate**: two waves of
  three Clowns come at the hideout (the Gas Masks fight them too); hold your ground until they're all down. **His scene**: the mask held out
  with both hands.
- **Anteiku (Ken).** The right reason (kept). Then **a shift at the café**: Ken hands you the makings once a day (3 glass bottles, 6 cocoa
  beans, 3 blood sugar: exactly three Anteiku Blends by the cookbook's recipe). Brew them, and as each of three regulars sits down near the
  counter (they glow), right-click them with a cup in your hand to serve it. Each waits a minute; two walking out loses the shift. Then
  **the doves come in**: three investigators. Beat each down to two fifths of their health and they break off and run; kill one and it's
  over ("I asked you"). **His scene**: a second apron on the hook, and a cup poured.
- **The CCG (the General).** The oath (kept). Then **the academy's test**: three questions drawn from ten about the bureau and ghouls (the
  kakuhou, quinques, the kakugan, working in pairs, what ghouls eat, which kagune is which, the ranks, kakuja, people first), answers in a
  different order each time. One wrong and you sit it again tomorrow. Then **the hunt**: you're given a partner (an investigator at your
  side, who never turns on you, kagune or not) and a reported ghoul 50 to 90 blocks off (it glows; the action bar says which way). Bring it
  down and bring your partner back alive. **His scene**: he stands, and swears you in.
- **While a trial runs**, the action bar shows how it goes (time left, which way, the act, the wave, the regulars served, your partner's
  health). Logging out, dying, leaving its ground or another world loses it; ask the leader again. Everything a trial makes (the patrol, the
  volunteers, the Clowns, the regulars, the raid, the partner, the reported ghoul) goes with it, and anything left over from a restart is
  taken away.

### If you were halfway through a way in

Members stay members, untouched. Halfway: Aogiri's "Kill 5 CCG" begun is now the ambush (five done: go back to Ayato). The Clowns' "three
Gas Masks" is now the ring, "the quinque case" the job. The Gas Masks' "three Clowns" is now the gas, "a clown's mask" the gate. Ken's beans
are now the shift, his three investigators the raid. The General's five ghouls are now the test; done stays done.

### Choices of mine you may want changed

- The spar stops at a quarter of your health and half of Ayato's; two minutes at most (then a draw: "Lasting isn't winning").
- The ambush is the CCG's usual pair; the ring's volunteers are C rank at most; the reported ghoul is a rank above what you'd meet, A at most.
- Roma's shows are only at night ("Nobody comes to a circus in daylight").
- The café's raiders break at 40%; a raider killed by someone else (Touka, say) just counts as gone.

### Other

- Roma now answers one of Anteiku or the CCG as what they are (she used to call them all Gas Masks).
- Render: `docs/handoff/joining-4.1.79.png` (every trial's page and button, the test, Roma's job, each leader's scene; the game's own talk
  screen).
- Tests: `NpcTests.trials` and the rewritten `joining`, `anteiku`, `ccg` (each chain's order, each step a different kind, every trial's
  rules, the test's questions and day, the makings against the recipe, the words, and how the server holds each trial); 50 rules sabotaged
  one at a time (`docs/handoff/sabotage-4.1.79.json`), each caught.
