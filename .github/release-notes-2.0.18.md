## 2.0.18 (HexGhoul): CCG agents dash in zigzags, more by rank
HexKagune 2.0.18 for Forge 1.20.1. Install on client and server (**protocol 73**, unchanged). Built and tested by CI. **Not yet tried in game**: please report anything that looks or feels wrong.

### From your request

- **Agents chain their dashes in zigzags.** Each dash in a chain goes the other way across the line to you, with the left or right dash animation each time.
  - **If you're out of their reach,** the zigzag works its way in toward you.
  - **In close,** they slip from side to side.
  - **A wall or a drop** in the way ends the chain.
- **How many dashes in a row rises fast with rank:**

| Rank | Dashes in a row | Dash speed |
|---|---|---|
| 0–1 | 1 | ×1.0–1.06 |
| 2–4 | 2 | ×1.12–1.24 |
| 5–7 | 3 | ×1.30–1.42 |
| **8** | **4** | **×1.63** |
| **9** | **5** | **×1.84** |
| **10** | **6** | **×2.05** |

- **Higher ranks also dash more often** (15% likelier per rank) **and sooner after the last one** (Rank 10 in about half the time).
- Dodging your blows uses the same zigzag, so a high-rank agent is much harder to pin down.
