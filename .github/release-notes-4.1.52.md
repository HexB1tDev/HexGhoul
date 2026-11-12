## 4.1.52 (HexGhoul): the reputation bar

HexKagune 4.1.52 for Forge 1.20.1. Not tested in game. **Update the server and every client together** (protocol 123 to 124).

Your words: "New Reputation bar when talking to fellow gang members, high reputation means you're feared, they react to it, some stepping
back to let you pass (just a little and not the leaders, they do not step back this also applies for official NPCS). / When the
reputation bar reaches 0 the player is warned to leave the area (if within the hideout), the bar starts at 50%."; and your answers:
raised by daily quests and by killing the CCG and rivals, lowered by hitting your own, back toward 50% over the days nothing happens;
"They attack after a warning".

**The bar:** when you talk to a member of your own gang (its ghouls, and its leaders and officials: Ayato and Noro for Aogiri, Roma for
the Clowns), the talk screen shows **REPUTATION** under the bond meter, in your gang's colour (Aogiri red, the Clowns blue): where you
stand in words (Unwanted, Despised, Doubted, Known, Respected, Feared), the share, and a diamond on the bar. It starts at **50%** when you
join (and for anyone who joined in 4.1.50).

**What moves it:**
- Up: **+3** for each investigator (or one of the CCG's ghoul division) you kill, **+4** for each member of another gang. Daily quests
  will add **+10** each when they come (next version).
- Down: **-2** for a blow on one of your own (at most one counted every 2 seconds), **-20** for killing one. The ban for killing more than
  10 of your own stays as it was.
- **Back toward 50%** by 5 points each game day nothing changes it.
- Each change shows over your hotbar ("Aogiri Tree reputation 53% +3").

**Feared (80% and up):** they react. Some of your gang's members (six in ten, always the same ones) step back a block or two out of your
way when you come close, looking at you. The leaders and the official NPCs never do. And they greet you differently: feared ("Y-yes?
Whatever you need."), respected, known, doubted, despised, unwanted ("Get out. Before it gets ugly.").

**At 0% (Unwanted):** inside your gang's hideout (Aogiri's mansion, the Clowns' tent) you are told to leave, with 30 seconds counting
down over your hotbar. Still there after that, your gang turns on you. Step out and they let you go. You are not banned: the bar can
climb again, and at 0 you are warned again each time you come back in.

### Other

- Renders: `docs/handoff/reputation-4.1.52.png` (the talk screen with the bar; the game's own drawing code, a stand-in font).
- Next on your list: Aogiri's and the Clowns' traits and their daily quests (Noro, Roma), then the Gas Masks (Banjou), the marriage and
  gang rules with "Quit the gang", then the menus.
