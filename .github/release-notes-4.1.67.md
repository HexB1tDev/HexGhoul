## 4.1.67 (HexGhoul): quitting a gang, the marriage rules, a spouse who wants you out

HexKagune 4.1.67 for Forge 1.20.1. Not tested in game. Protocol unchanged (125).

Your words, the old to-do (quoted exactly): "Add a new Quit the gang button to each leader whenever the player joins it." / "kicked from the
gang if the player marries another gang leader, but can still join if the player was already married before joining." / "Then, add being
married to a gang leader but being part of another gang generates different reactions from the spouse, constantly demanding they quit the
gang." / "Make sure ayato + touka and being married to tokou reactions are in."

- **Quitting:** at your own leader (Ayato for Aogiri, Roma for the Clowns, Banjou for the Gas Masks), asking how the gang is now also offers
  "I want out of ...". They ask if you're sure (Ayato cold, Roma's smile wobbling, Banjou asking if they did something wrong); "I'm out. I
  quit." takes you out: your name's emblem and colour gone, they no longer fight for you or leave you be. **Never a ban:** you can join again
  by its way in, from the start. "No. I'm staying." and they answer that too.
- **Marrying another gang's leader while in a gang:** you're out of yours the moment the ring is on (not banned), and told so. Married
  first and joining a gang after is never stopped.
- **A spouse who leads another gang:** Ayato, Roma or Banjou, married to you while you run with another gang, opens most talks (two in
  three) by telling you to quit it, each in their own voice, three ways each; and near them, every two and a half minutes, says it again in
  chat.
- **Touka's marriage, checked:** Ayato and Ken already react to you being with, married to and having a child with Touka, and Touka to you
  and Ayato. Missing, now in: the General on your wedding to Touka and on a child with her, and Eto on a child with Touka.

### Other

- Render: `docs/handoff/gang-leaving-4.1.67.png` (Roma's member page with the way out; her asking if you're sure; Banjou's goodbye;
  Ayato, your husband, while you run with the Clowns).
- The leaders' words are in `GangWords` (pure, tested); `GangPlayer.quit()`.
- Next: your new list (Ken onto Anteiku's roof with the cyber tower removed, his sitting leg, the Anteiku faction through Ken, the CCG
  faction through the General with dailies), then the hotel.
