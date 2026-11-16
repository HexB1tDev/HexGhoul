## 2.1.4 (HexGhoul): romance for every personality

HexKagune 2.1.4 for Forge 1.20.1. Install matching versions on client and server. Not tested in game.

Romance with village humans now has words for every personality. You can flirt with anyone (strangers, friends and close friends each answer differently). A partner gets conversations of their own. Kisses, hugs and "I love you" have more variety.

- **Flirting**: "Flirt a little." (or "Flirt with Hana." once you're close) on every non-partner's talk page.
  - All 12 personalities have their own answers for a stranger (2), a friend (3) and a close friend (3). A close friend's answers push you to ask them out.
  - If you're not who they fall for, it's a kind no (2 each) and nothing more.
  - A flirt that lands warms them once a day: the flirty and the romantic most (+5), the stoic and the sarcastic least (+1).
- **A partner's talk**: three at a time on the "Talk a while." page, beside the rest of their life.
  - "What do you love about me?": their personality's opener, then what they look for, found in you (by their taste: strong, gentle, funny, mysterious, clever, daring, loyal, caring).
  - "Where do you see us in a year?" ends with their own dream, with you in it now: "And someday... I'd still like to see the sea. With you beside me, this time."
  - Also: "Do you remember when we met?", "Do you ever get jealous?", "What if I were in danger?" and "Tell me something sweet.", each in every personality's words.
- **More love**: two more lines each, per personality, after a kiss, in a hug and to "I love you", beside the first two.
- **New code**: `village/HumanRomance` (pure). Tests: `NpcTests.romance214` (every personality's romance said cleanly at every warmth and for every taste; each has at least 60 different lines in love). Protocol stays 80.
