## 2.1.8 (HexGhoul): Ken asks for proof before a final kakuja

HexKagune 2.1.8 for Forge 1.20.1. Install matching versions on client and server. Not tested in game.

Ken's final-kakuja lessons are harder. Before he teaches the Full Kakuja, the Dragon or Ukaku's final kakuja, he asks you to prove you're ready. The proof is set the moment you ask, tracked on the action bar, and the lesson is taught (or the trial lived through) once it's done.

- **The Full Kakuja**: eat 5 ghoul meat. "The Full Kakuja isn't taught. It's eaten."
- **The Dragon**: kill 3 Doves of Rank 6 or above. "The Dragon only wakes in someone who's stood at the edge and come back."
- **Ukaku's final kakuja**: kill 10 wild ghouls with your first kakuja's wings out, then the trial as before. "The storm has to be fed first."
- **Unchanged**: the centipede and Touka's lessons. The mastery each lesson needs is unchanged too.
- **Code**: `Teaching.Proof` (pure, tested), counted in the player's memory of Ken (`NpcMemory.PROOF`). Ghoul meat eaten (`QuestServer.ate`) and Doves and ghouls killed (`EtoServer.died`) count toward it. Tests in `CompanionTests.proofs`. Not tested in game.
