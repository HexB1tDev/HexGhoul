## 4.1.55 (HexGhoul): the talk screen in the quest menu's style

HexKagune 4.1.55 for Forge 1.20.1. Not tested in game. Protocol unchanged (125).

Your words (your list after 4.1.49): "new Menus: Every single menu in the game is to mimic and look like the quest menu, and be as
interactable as it is, Dialog, mastery menus ALL OF THEM"; and after 4.1.54: "I ordered the GUIS all remade in image of the quest menu
style, I didnt get to see any of them, can you confirm it's done and show me results?" They were not done yet; this is the first of them,
the one you asked for first: the talk screen (every conversation: the companions, Banjou and the gang leaders, wild ghouls, villagers,
children). One screen a version from here.

**The talk screen, remade as the quest menu:**
- **The quest menu's box**: the same plate with its cut corners, the bright corner brackets that pulse, the faint honeycomb, the red rule
  under the header fading out at both ends. It sits low on the screen so the one you're talking to stays in sight above it, and it
  grows upward to fit what is said and how many answers there are (sized from the start, so nothing jumps when the answers come in).
- **The header** as the menu's: the face in a framed tile like a quest's icon (its edge pulses red while they talk), the name large like
  "QUESTS", the mood as a chip beside it, who they are to you in red under it ("THE GAS MASKS' LEADER", "YOUR DATE"). The bond meter,
  and for a gang member your reputation bar in the gang's colour, at the top right where the menu's progress bar is.
- **What they say** set off as a quest's story: a red bar down its side that beats while the words are coming, typed out as before (a
  caret, pauses at commas and full stops; click to skip).
- **ANSWERS** under a heading with its rule, as the quest list's cards: each in its own plate with its number in a key cap, the words,
  and a small mark at the right for what kind of answer it is (a speech bubble for words, a heart for sweet things, a parcel for gifts, a
  rising arrow for lessons, a star for favours, chevrons for going with them, an arrow back for leaving, a red cross for a cruel word).
  Under the pointer a card lights up, takes a red edge and a bar down its side, and leans toward you, as the quest cards do. They slide
  in one after another once the words are said; two columns when there are more than four. Click one, or press its number
  ("click, or press its number" at the heading's end).

### Other

- An answer with no words of its own always shows its own (never a blank card).
- Render: `docs/handoff/talk-screen-4.1.55.png` (Banjou with the Gas Masks' reputation bar, a villager with nine answers, Eto talking,
  Eto with someone else's partner and a cruel answer under the pointer).
- Next: the next menu in the quest menu's style (the mastery menus: Ukaku, Rinkaku, Bikaku, the Graft, Pale; some are the original
  jar's own and riskier to patch: I'll say which before each), one a version. Then the marriage and gang to-do items, then your new list
  (Ken onto Anteiku and his sitting leg, the Anteiku and CCG factions, the CCG's emblem a briefcase).
