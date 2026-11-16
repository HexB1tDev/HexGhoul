## 2.4.0 (HexGhoul): final sweep: health kept across logins, children's halves only added

HexKagune 2.4.0 for Forge 1.20.1. Install matching versions on client and server. Not tested in game.

The final sweep after 2.3.2 to 2.3.9. All 34 test suites pass (millions of checks), the Minecraft API check passes, and two problems that would have shown up in play are fixed.

- **Extra health across logins**: the ghoul health bonus from 2.3.6 is now put back at login, together with the health you had when you left. Before, loading clamped your health to 20 before the bonus was re-applied, so every relog cost you up to 20 health.
- **Children's kagune**: bringing older children up to both parents' halves (2.3.7) now only ever adds the half a child lacked. It never takes a kind away, and never changes when a player parent later switches path.
- **Banned names**: two source comments still quoted banned names; they're gone. They never reached the jar.
- **Checked**:
  - Every suite: Eto, Companion, Scene, Quest, Family, NPC, Control, Agent, Final kakuja, Ukaku, Kitchen, Path screen, Ghoul meat, Graft, Pale, Kakugan, Wound and every other.
  - Every sound name added this session goes through the pack (`Sfx`).
  - The network protocol is 84 since 2.3.0.
- Protocol stays 84. Not tested in game.
