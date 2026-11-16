## 2.0.17 (HexGhoul): CCG ranks 0 to 10, stronger by rank
HexKagune 2.0.17 for Forge 1.20.1. Install on client and server (**protocol 73**, unchanged). Built and tested by CI. **Not yet tried in game**: please report anything that looks or feels wrong.

### From your request

- **Eleven ranks: Rank 0 to Rank 10.** Rank 0 is the most common and each higher rank is rarer. The name above an agent shows its rank.
- **Higher ranks are tougher on every count:**
  - more health
  - faster movement
  - more armour
  - harder hits (quinque blows and launcher shards alike)
  - shorter rests between blows
- **Ranks 8, 9 and 10 are rare (about 4 agents in 100) and really dangerous.** Their names are in red.

| Rank | Health | Hits | Rest between blows | How common |
|---|---|---|---|---|
| 0 | 30 | ×0.85 | full | 19 in 100 |
| 3 | 52 | ×1.15 | 92% | 13 in 100 |
| 5 | 74 | ×1.45 | 84% | 9 in 100 |
| 7 | 104 | ×1.8 | 76% | 5 in 100 |
| **8** | **160** | **×2.3** | **68%** | 2 in 100 |
| **9** | **210** | **×2.7** | **62%** | about 1 in 100 |
| **10** | **280** | **×3.2** | **55%** | less than 1 in 100 |

- **A pair's leader** still takes the better of two rolls.
- **Agents already in your world keep their place:** Rank 3 becomes Rank 1, Rank 2 becomes 2, Rank 1 becomes 3, First Class becomes 4, Associate Special becomes 6 and Special becomes 8.
- **Clashes:** harder hits also count in clashes (2.0.11). An investigator of rank 8 or above can overwhelm a weak kagune in a clash.
