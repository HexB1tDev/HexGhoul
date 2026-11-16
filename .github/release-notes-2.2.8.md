## 2.2.8 (HexGhoul): quests complete all unlocks everything

HexKagune 2.2.8 for Forge 1.20.1. Install matching versions on client and server. Not tested in game.

`/kagune quests complete all` now unlocks everything. Besides finishing every open quest, it opens every gate, including the ones a companion teaches: the Centipede, the Full Kakuja, the Dragon, Crystal Wings, Gatling, the first and final Ukaku kakujas, and every mutation's gate. Nobody sends you to a teacher any more.

- **Rinkaku**: the crown is given too, so you can choose War or Peace right away. With the Dragon's gate open, the Dragon still wakes at your next death.
- **Mastery**: each form still needs its own mastery (the kakujas 750 to 1000). If yours is under 1000, the command says so and points to `/kagune mastery set 1000`.
- The command lists what it opened.
- **Code**: `QuestServer.grantAll`, `ControlCommands.completeQuest`. Protocol stays 83. Not tested in game.
