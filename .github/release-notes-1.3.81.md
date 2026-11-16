## 1.3.81 (HexGhoul): twice the villagers, never two alike in a village (testing)
HexKagune 1.3.81 for Forge 1.20.1. Install on client and server (**protocol 72**, unchanged). Testing build: compiled, tests pass, **not tried in game**.

You asked: "Add more humans with random personalities per village (make sure there are no 2 of the same look per village.). same as you did with the children."

- **16 villagers per Brick Village** (was 8): two live in each of its 8 houses. They go home at night, trade, chat, date and flee as before.
- **26 villager looks** (was 10): made from the children's skins, as before, with hair, clothes and eyes turned to other colours, and now dull or grey clothes tinted too (a grey hoodie becomes a coloured one), so no villager looks like one of your children. Only skins that blink were used, so every new look blinks. Looks that changed too little from the child, or came out the same as another villager, were dropped (I checked each one front and back; the sheet is `docs/handoff/villagers-1.3.81.png`).
- **Never two alike in a village:** each village shuffles the looks in its own order and gives each of its people their own, so no two people in one village share a look. **No two share a name either** (each village shuffles the 24 names the same way).
- **Random personalities:** each villager's personality (cheerful, shy, bold, kind, grumpy, flirty, dreamy, nervous) is random, as before.
- **Less work per villager (lag):** a calm villager now looks around for danger every 4 ticks instead of every tick (a frightened or hurt one still every tick).
- Villages that already spawned get their people's looks and names reshuffled once, so they don't repeat either (they keep 8 people; new villages get 16).

Untested in game: compiled and unit tested (200 villages: no shared look or name among 16 people).
