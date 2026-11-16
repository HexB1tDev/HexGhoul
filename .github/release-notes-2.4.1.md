## 2.4.1 (HexGhoul): partners come to you far less; no false talk alerts

HexKagune 2.4.1 for Forge 1.20.1. Install matching versions on client and server. Not tested in game.

Partners come to you far less, and the false "wants to talk to you" alert is gone. Spouses and partners were walking up to kiss, hug, cling and take your hand almost constantly. The biggest cause was a 1-in-100 chance every tick of clinging to you whenever they stood near you in a soft mood, which made it every few seconds.

- **Things she does by herself** (a hand, an arm, a hug, a lean): at most once every 5 minutes, was 90 seconds. The cling-when-standing-near is now rare and counts towards that same 5 minutes.
- **Coming over with a word**: four times as far apart (lonely or clingy every 8 minutes, happy every 16). She now just walks over and waves, without grabbing hold.
- **Asking you for a kiss**: still at most once every 20 minutes, but a quarter as eager once that time is up. Before, she asked within seconds.
- **Coming over with something to say** (her scenes): half as often.
- **"Wants to talk to you"**: no longer shown for what she does by herself. Alerts stay for things that really want you: a kiss she's asking for, the night, missing you, a birth.
- **Code**: `EtoInitiative.EVERY`, `EtoMood.seeks`, `EtoRules.KISS_ASK_CHANCE`, `SceneServer.every`, and `EtoBrain.together`. Protocol stays 84. Not tested in game.
