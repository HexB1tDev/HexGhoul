## 2.0.34 (HexGhoul): village women's bodies

HexKagune 2.0.34 for Forge 1.20.1. Install matching versions on client and server. Not tested in game.

Village women now have the female body the game already gives its CCG women and grown daughters: hips and thighs shaped, a wide-painted skin's arms drawn slim, the bust in her own skin (and its second layer), bouncing as she moves, drawn a little shorter and slimmer than the men.

- Who: the 12 villager skins made from daughters' skins (`HumanSkins` marks them `girl`), and the women among vanilla's default skins (Alex, Ari, Efe, Makena, Noor) for humans made before 1.3.75.
- Her proportions are the CCG women's (`HumanRules.WOMAN_*` = `AgentRules.FEMALE_*`): about 0.92 of a man's height, 7% narrower, 85% of the figure's curves.
- The villagers share one model, so each is put back to a man's legs, arms and leg pivots before a woman's are shaped: no woman's shape carries over onto the next man drawn.
- Previewed off-game with the new `tools/VillageWomenPreview.java`: every village woman before and now, front, three-quarter and side, a man beside for scale (`docs/handoff/village-women-2.0.34.png`). Protocol stays 77.
