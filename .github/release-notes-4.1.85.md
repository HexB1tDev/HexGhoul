## 4.1.85 (HexGhoul): romance: the daily, "What do you want today?"

HexKagune 4.1.85 for Forge 1.20.1. Not tested in game. Protocol unchanged (126).

The plan shown to you had, as romance's last step, a small daily "what do you want today?" once together (you said "whatever you think is
best"). This is it.

- **With someone you're dating or married to, their first page asks "What do you want today?"** They tell you, in their words, the one
  thing they'd like today: "Why do you care? ...Fine. An RC tonic. And hurry up." (Ayato); "Me? Oh... some bone meal would be nice. For
  the garden. If you're passing that way." (Banjou).
- **It's a different thing most days**, always one they love and can be had (nothing behind a boss or a deep mine):
  - Eto: an Anteiku Blend, an ink sac, a feather, a candle, a book and quill.
  - Touka: an Anteiku Blend, a rabbit hide, a book and quill, a blood latte.
  - Ken: an Anteiku Blend, a book and quill, a lantern, crimson tea.
  - Ayato: black dye, an iron sword, an RC tonic.
  - The General: a clock, bread, cooked beef, a book and quill, a shield.
  - Roma: a firework star, a slime ball, a jack o'lantern, purple dye, pink dye.
  - Banjou: bone meal, an oak sapling, a flower pot, a cherry sapling, a poppy.
  - Noro can't be dated, so he has no wish.
- **Bring it the same day** (as a present: 4.1.84's "Here. I want you to have this.") and they remember: "...You actually brought it.
  *She looks away, going red.* Idiot. ...Thanks." (Touka). It's worth 4 more regard than the present alone, with hearts.
- The question is there until today's gift is given (one gift a day, as before), and only on their first page when it has room.

### Other

- Render: `docs/handoff/wish-4.1.85.png` (each one asked, then the wish brought, on the game's own talk screen; `tools/UiPreview.java <dir>
  wish`).
- Tests: `CompanionTests.wish485`; 20 rules sabotaged one at a time (`docs/handoff/sabotage-4.1.85.json`).
- Found on the way: "A rc tonic" and "A cooked beef"; names now take "an" for an initialism said with a vowel (an RC tonic) and "some" for
  what isn't counted (some bread, some dye, some cooked beef). The two lines that then said "one" of it were reworded.
