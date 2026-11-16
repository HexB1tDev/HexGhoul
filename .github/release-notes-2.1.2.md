## 2.1.2 (HexGhoul): each companion's own courtship, and dates

HexKagune 2.1.2 for Forge 1.20.1. Install matching versions on client and server. Not tested in game.

Eto, Touka and Ken no longer share the same three errands (three books, ten kills at night, a flower). Each now has their own five-chapter courtship. Every chapter asks for something only that companion would ask for, and closes with a written scene that has choices, played the moment the task is done. Once you're together, you can take them on dates.

- **Eto** (her courtship begins once she's a close friend, regard 60, not 100): write her something in a book and quill and sign it; a walk with her after dark (3 minutes, her following you); ink sacs for her pen (4); kill 6 things at night while she watches you; the Library in the dead of night, where she tells you what she is.
- **Touka**: 8 cocoa beans for the cafe (then a cup after closing, where she wants you to be honest); clear 8 monsters at night to keep the Doves off the ward (then her brother, on a rooftop); take her somewhere high, height 100 or more, with her following you (and her wings, the soft way); sit with her at the cafe for 2 minutes while she studies (then she makes you quiz her); a rabbit's foot (why she wanted it).
- **Ken**: a book he hasn't read; coffee at his place, 2 minutes sitting with him (his habit with his finger); kill one wild ghoul that eats people; 3 white flowers for his door (the red ones he used to dream of); stay up with him until dawn.
- **What's handed over** is held out on the talk screen ("Here. What you asked for.") and taken all at once: a stack of 8 beans is one hand-over. Kills, walks, sits, the climb, dawn and the dead of night count on their own. When one finishes with them right there, they come up to you with the scene.
- **The scenes**: 15 chapter scenes, 2 to 4 pages each. Your answers change their regard and what they do; each closed chapter is also worth 4 regard. After the fifth, they ask you to ask them out.
- **Dates**: "Let's go somewhere. (a date)" on the sweet page, once a game day. There are 8 per companion, each a short scene of its own, some only by day, at night, in the rain or at home. Examples:
  - Eto: a bookstall, a rooftop, a graveyard, reading in the rain;
  - Touka: a movie, the beach, the arcade, fireworks;
  - Ken: a bookstore, the river, a bench in the rain, cooking.
  A date brings 3 regard, and its answers count once a day. The journal's dating line now points to dates for getting to propose.
- **The journal**: the Romance tab shows "Courting: chapter 2 of 5", the chapter's name, and what to do next with the count so far.
- **Saves from before**: an errand in progress is read as so many chapters done, with its count started over. All three errands done still means waiting to be asked out.
- **Code**:
  - new pure `npc/Courtship` (the chapters, their words, the state in three bits of the bond's flags);
  - new `server/CourtshipServer` (the timed and place-based chapters, checked every 10 s from the companion's own tick);
  - `moment` and `date` kinds in the talk scripts (`SceneBook`, `Scene.Kind`);
  - `EtoRules.Step.COURTING` appended (BOOKS, HUNT and FLOWER kept, never returned);
  - `EtoTalk.Effect.COURT`, `EtoTalk.Context.held`, `NpcMemory.DATE_DAY`;
  - the Romance card's `chapter`.
- **Tests**: `CompanionTests.courtship`, `EtoTests.errands`, and `SceneTests` (every chapter has its scene; every companion has at least 3 dates for a partner by day and by night). Journal preview: `docs/handoff/romance-courtship-2.1.2.png`. Protocol stays 80.
