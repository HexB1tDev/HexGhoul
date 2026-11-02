## 1.3.76 (HexGhoul): villagers and children blink (testing)
HexKagune 1.3.76 for Forge 1.20.1. Install on client and server (**protocol 72**, unchanged). Testing build: compiled, tests pass, **not tried in game**.

You said: "they should all blink like Touka, Eto and all the kids do."

- **Villagers blink** now: a short blink every few seconds, now and then twice, each at their own moment (Eto's and the companions' blink timing).
- **Children blink too.** Honest note: reading the code, the kids did not blink before (only Eto, Touka and Ken did), so they get the same blink now, and their eyes shut while they sleep.
- **How the shut eyes are made:** every eye that blinks was boxed by hand, face by face, on a large grid. Inside its box, the eye (lashes, white, iris) is grown over from the skin around it, pixel by pixel, so the lid keeps the skin's own shading. Then a thin lash line goes across its lower part, a neutral dark as deep as the eye's own lashes, curving down a little on HD skins ("- -", like Eto's). These are generated copies (`tools/blink_skins.py`); your skin files are untouched. A villager's skin is made from a child's skin, with the eyes in the same place, so it blinks with that skin's shut eyes.
- **Your report on the first try:** it found the eyes by colour and filled them with one flat colour, so on a dozen faces it showed patches, kept bits of eye, or covered half a face (daughter_19). You marked them, and that method is gone. The sheet `docs/handoff/blinks-1.3.76.png` shows every face open and shut, kids and villagers.
- **28 of the 39 child skins blink.** The other 11 have eyes that are already drawn shut, or hidden under (or behind see-through) hair: daughter 2, 4, 6, 7, 10, 11, 12, 20, 21, 22 and son 14. They stay as drawn. So does one villager look (made from daughter 21, eyes already drawn shut). son_12's blood running past its eye is kept.

Honest look (from flat face previews, not in game): every face that blinks shuts cleanly at preview size. The 64-pixel skins (son 13, 15, 16) shut to a thick "- -", one pixel tall.

Untested in game: compiled and unit tested (every blinking skin has its shut eyes, villagers use their child skin's).
