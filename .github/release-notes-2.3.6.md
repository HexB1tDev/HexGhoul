## 2.3.6 (HexGhoul): endgame ghouls far tougher: more health, more resistance

HexKagune 2.3.6 for Forge 1.20.1. Install matching versions on client and server. Not tested in game.

Endgame ghouls are far tougher. Before, a player with mastery 1000, every quest done and 100+ ghoul meat eaten still had only 20 health and took about 46% of every blow with the kagune out, so a Rank 7 investigator could nearly one-shot them. Now every factor counts for more, the quests count too, and health grows.

- **Health**: up to +20 over a player's 20, so up to 40 in all. Mastery gives up to +10, ghoul meat up to +6 (half of that at 15 pieces), and opening every unlock on your path (its quests and lessons) +4. It updates every 2 seconds. Humans keep 20.
- **Resistance**: mastery 1000 takes off 45% (was 35%). Ghoul meat takes off up to 35% (was 25%; half of it at 15 pieces, was 20). Every unlock on your path opened takes off a new 15%. Forms stay as they were (kagune out 10 to 22%, kakuja 32%, final kakuja 50%).
- **The endgame player above**: about 39 health, taking 29% of a blow with the kagune out, 22% in a kakuja and 16% in a final kakuja. That makes them about three times as hard to bring down as before, and far more in a kakuja.
- A new ghoul is unchanged: 20 health, every blow taken whole.
- **Code**: `GhoulTank` (`QUESTS`, the `HEALTH_*` values, `health`, a 4-argument `taken`; tested in `GhoulMeatTests`); `GhoulTankServer.unlocked` and the health modifier. Protocol stays 84. Not tested in game.
