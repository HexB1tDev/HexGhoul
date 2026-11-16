## 2.1.3 (HexGhoul): every villager is someone

HexKagune 2.1.3 for Forge 1.20.1. Install matching versions on client and server. Not tested in game.

Every village human is now someone of their own. Each has a trade, a family, a pastime, an odd habit, a fear, a dream, a secret, a favourite gift and a view of ghouls, all fixed for life, and says them in their personality's voice. A new "Talk a while." page lets you ask about all of it. Getting to know them warms them to you, and their trade decides what they sell.

- **Their lives** (`village/HumanLife`, pure, tested): 21 trades, each with what it does all day (4 lines) and how today went (3):
  - baker, farmer, beekeeper, florist, smith, carpenter, tailor;
  - scribe, candle-maker, miller, cook, shepherd, potter, mason;
  - herbalist, night watch, innkeeper, teacher, gravedigger, fisher, merchant.

  Each life also draws, by its own seed:
  - 24 family situations and 24 pastimes;
  - 20 fears, 20 dreams, 20 odd habits and 20 secrets;
  - 20 favourite gifts;
  - 5 views of ghouls: afraid, hates them, curious, sorry for them, too tired to care.

  Out of 400 villagers in the tests, no two lives were alike. A village gives each of its people a different trade.
- **Names**: 48 women's names and 48 men's. Villagers placed from now on get one that fits their body, with no two alike in a village. Villagers who already have a name keep it.
- **How they say it** (`village/HumanVoice`): each of the 12 personalities frames every answer its own way, with a softer, more careful way for fears, dreams and secrets. So two shy bakers still say different things, and a shy baker and a bold one say the same thing differently.
  - "How are you?" now has 5 small-talk lines per personality, or else how their day at work went.
  - The news has 24 items.
  - 20 bits of gossip name real neighbours: "Have you met Hana? The beekeeper..."
  - Now and then you catch them at their odd habit as you start talking.
- **"Talk a while."** opens a page of topics: what do you do, your family, for fun, what's that you're doing, ghouls, gossip; from regard 20 your favourite thing, your type and what scares you; from 60 what you dream of; from 80 a secret. What you haven't learned yet comes first. The first time you learn each personal thing, it's worth 2 regard.
- **Gifts**: "This is for you." gives them what's in your hand.
  - Their favourite: +8 and hearts.
  - Food, flowers and pretty things: +3.
  - Anything else: +1.
  - Something nasty (rotten flesh, ghoul meat...): -3, handed back.

  Gifts count once a day. Every personality has its own reaction to each.
- **Trades by trade**: a baker sells bread, cookies, cake and pie; a smith iron, buckets, shears and lanterns; a beekeeper honey, honeycomb and candles; and so on. Each sells 5 of their trade's wares (its signature first), one of the cookbook's foods and one farming or basic good. Still no meat and no fish, and every cookbook ingredient is still sold somewhere.
- **The talk screen** says who they are under their name: "Shy beekeeper". The main page is now: How are you / Talk a while / a compliment / a gift / trade / the romance answers. "What's your type?" moved to the topics.
- **Tests**: `NpcTests.lives` (names; lives for life and all different; every personality saying every part of every life cleanly; the topics as they warm; gifts) and the shop test updated. Preview: `docs/handoff/village-talk-2.1.3.png`. Romance lines for every personality come next (2.1.4). Protocol stays 80.
