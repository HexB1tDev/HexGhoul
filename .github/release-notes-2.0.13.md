## 2.0.13 (HexGhoul): trading with village humans
HexKagune 2.0.13 for Forge 1.20.1. Install on client and server (**protocol 73**, unchanged). Built and tested by CI. **Not yet tried in game**: please report anything that looks or feels wrong.

### From your request

- **You can trade with the village's humans.** Right-click one to talk and choose **"Let's trade."** Its wares come up on the talk screen.
- **What they sell:** mostly food, the ingredients the cookbook's recipes need (never meat, never fish), plus basic goods like a village's.
  - Each villager always sells the same six things: one staple (wheat, eggs, sugar or bread), three more foods, and two basic goods. Different villagers sell different things, and every non-meat ingredient the cookbook needs is sold by someone.
  - **Foods:** wheat, eggs, sugar, bread, beetroots, brown and red mushrooms, carrots, potatoes, baked potatoes, cocoa beans, dried kelp, kelp, sweet berries, glow berries, honey, milk, apples, pumpkins, and now and then a golden apple.
  - **Basic goods:** bowls, glass bottles, a bucket of water, sticks, bones, ink sacs, slimeballs, snowballs, blaze powder, coal, torches, paper, arrows, string, leather, flint, iron ingots.
  - **Prices** are like a vanilla villager's. For example, 12 wheat, 8 eggs or 6 bread for 1 emerald; a bucket of milk for 2; a golden apple for 10.
- **What they buy:** crops and plain goods, as vanilla villagers do. Wheat, potatoes, carrots, beetroots, pumpkins, melons, sugar cane, sweet berries, apples, eggs, paper, string, coal, sticks, leather, feathers, flint and iron ingots.
- **How to pay:**
  - **To buy,** hold emeralds in your main hand and pick a ware. The screen shows how many you're holding.
  - **To sell,** hold what you want to sell. A "Sell …" choice appears if they buy it, and they take as many whole lots as you hold.
  - What you buy goes into your inventory, or drops at your feet if it's full.
- **"Let's talk instead."** takes you back to the normal talk page.

### Known approximations

- **Payment only from your hand:** they take emeralds only from the stack in your hand, not from the rest of your inventory. That's the same as buying Eto's cookbook with coffee beans in hand, and it needs no new Minecraft API.
- **No limits:** wares don't run out and prices don't change.
- **Creative mode:** you buy for free (as the mod's other purchases).
