## 2.4.9 (HexGhoul): Spouse cooking: a furnace needed, in front of it, a full routine; the food to the Food chest, handed to you at home, sometimes for a kiss

HexKagune 2.4.9 for Forge 1.20.1. Install matching versions on client and server. Not tested in game.

The spouse's cooking is redone. A furnace or smoker at home is now needed (a campfire, table or chest no longer counts). At the stove the spouse stands right in front of it (the way it faces) and goes through a whole routine: feeding the fire, stirring, seasoning, stirring, tasting off the spoon, stirring. When the food is done they carry it to the chest they labeled "Food" when sorting. If you're inside the home's grounds, they walk up and hand you your meal (holding it out), and sometimes, depending on their mood, ask for a kiss for it.

- `Kitchen.oven`: furnace or smoker only, for plain cooking, a ghoul spouse's raw cuts and the cookbook's dishes (`HomeKitchen.plan`, `dinner`, `possible`). Out of fuel, they still say so.
- `Chores.front` reads the stove's facing from its block state (no new game API); `HomeChores.front` takes the block in front if there's room to stand (feet, head, a floor), and the spouse walks onto it (`there`: within 0.6 blocks; after 6 seconds of trying, near enough as before).
- `Chores.COOKING`: STOKE (coal in hand), COOK, SEASON (sugar over the bowl), COOK, TASTE (spoon up, blown on, a nod), COOK; new gestures appended to `EtoGesture` (with SERVE), posed in `EtoPose`. Previews: `docs/handoff/{eto,touka,ken}-cooking-2.4.9.png`.
- What isn't kept for you is carried (shown in hand; saved as `HexCarry`, so a stop or an unload never loses it) to the nearest chest named "Food" (`ChestSorter.food`/`foodFirst`; else any chest, never one you named yourself), opened, and put away.
- With you home: they walk to you, hold the food out (SERVE), and it goes into your inventory (at your feet if it's full) with a line of their own (`Kitchen.handed`). Then `Kitchen.kissAfterMeal`: 0.3 x their mood's kiss readiness (never sulking, jealous or wary; about 30% happy, 60% affectionate, at most 70%), the same ask as always (alert, walk up, theirs to turn down).
- With you away: everything goes in the Food chest, and when you come home they tell you what's in it.
