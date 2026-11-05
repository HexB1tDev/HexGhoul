## 2.4.2 (HexGhoul): Not That Easily: everything thinks you're dead

HexKagune 2.4.2 for Forge 1.20.1. Install matching versions on client and server. Not tested in game.

"You Thought You'd Get Rid of Me That Easily?" now makes everything think you're dead. That covers every path's version: Rinkaku's passive, Ukaku's, Graft's and Pale's mutations. Vanilla mobs already forgot you, but this mod's own fighters kept their own minds and came straight back for you.

- **What forgets you**: CCG investigators drop you as prey, along with their grudge and the ghoul mark on you. Wild ghouls drop you as prey and their grudge. A companion fighting you stops. A human you hurt stops remembering it.
- **Thought dead**: while you're in pieces, and for 30 seconds after you stand up, none of them takes you as prey again. Strike anything and they know you're alive.
- **Code**: `ReviveForget` (the mod's own minds, `presumedDead`), checked by `AgentBrain.wanted` and `GhoulNpcBrain.wanted`. Protocol stays 84. Not tested in game.
