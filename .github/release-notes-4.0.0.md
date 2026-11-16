## 4.0.0 (HexGhoul): The Great Exploration

HexKagune 4.0.0 for Forge 1.20.1. Not tested in game.

Your request was the 4.0.0 spec you pasted, with your files (`art/exploration/`: the two village schematics, the Aogiri hideout, Ayato's and Noro's skins; `art/outfits/`: the Formal and Crimson outfits and the 24 masks). You asked for one release: "Update 4.0.0 – The Great Exploration". Your later words are quoted in each part below.

### New places

- **The human town** (`hexkagune:village_1`) and **the CCG settlement** (`hexkagune:village_2`), built from your schematics. They replace the old village in newly generated chunks; places already generated stay as they are.
- **The Aogiri Tree hideout** (`hexkagune:aogiri_hideout`).
- **Placement:**
  - Each is placed whole, turned at random, on dry, flat-enough ground, with the terrain blended round it (`beard_box`).
  - Spacing: town 40 chunks (separation 22), settlement 48 (24), hideout 44 (22).
  - People: townsfolk in the town, investigators in the settlement, Aogiri members in the hideout, with Ayato and Noro.
- **Find them:** `/locate structure hexkagune:village_1` (or `village_2`, `aogiri_hideout`).

### Outfits and masks (real items: equip them, see them in first and third person, on players and NPCs)

- **The Formal Outfit:** Formal Cap, Formal Overcoat, Formal Trousers, Formal Shoes.
- **The Crimson Outfit:** Crimson Hood, Crimson Coat, Crimson Coat Tails, Crimson Boots.
- **Your 24 masks.** Kaneki's is the "Eyepatch Mask" and Yoshimura's the "Dotted Mask", so those names stay out of the mod.
- **Nine generic masks for wild ghouls:** Clown, Gas, Skull, Fox, Oni, Cloth, Porcelain, Bandage, Kabuki.
- **Protection:** iron armour's. Every mask goes in the head slot (a hood and a mask, one at a time).
- **Your words after seeing them:** "Armor is too bulky, if the player is alex model it should adapt to it as well as the male body's muscle shape. Masks are… very very lack luster, half masks are covering most of the face instead of being lower, way lower. The other masks are very basic and lack details, not only that but the eye position of the masks are very off." Fixed as follows. Your files are unchanged; the fitting happens when the models are baked (`tools/outfit_models.py`).
  - **Half masks** (Eyepatch, Ayato's, Nishiki's, the Fanged Mask, Tatara's, Yomo's, Rize's, the Dotted Mask): they now start below the eyes, over the nose and mouth to the chin.
  - **Full masks:** each one's eye holes are moved onto the skin's eye row (the face's fifth pixel row). The brow is stretched down to meet them, and the nose and mouth are drawn up under them.
  - **Every mask** stands just clear of the skin's hat layer, so hair and hat pixels no longer show through it.
  - **The generic masks** are redrawn in detail: shading, a curved face, raised noses, horns, ears and filters. Their eye holes sit on the skin's own eyes, so a ghoul's kakugan shows through.
  - **Armour:**
    - Coats, sleeves and trousers are drawn in close to the body.
    - The Formal Cap sits out past the hat layer.
    - Each sleeve has a second version for slim (Alex) arms. It's drawn on slim-armed players (by their skin) and on every slim-armed NPC, in first and third person.

### Wild ghouls, investigators, Aogiri Tree

- **Wild ghouls are ranked D, C, B, A, S and SS.** Each rank up has more health, armour, speed, damage, sight and regeneration, longer strings and shorter pauses.
  - D only has its basic string. Flips come from C, and clashes and heavy blows from B.
  - S has three centipede tails, and SS four plus thorns.
  - S and SS go into a frenzy when badly hurt.
  - Ghouls from an older save count as C, B or A.
- **Masks on wild ghouls:** 60% wear one of the generic masks, never a character's or a faction's.
- **Investigators:** every one but the White Reaper wears the whole Formal Outfit. They fight as before (the outfit's protection is taken off their own). Killed by a player, each piece has its own 4% chance to drop.
- **Aogiri members:**
  - They wear the whole Crimson Outfit, with their faction's mask under the hood (the Laughing Mask or the Fanged Mask).
  - They attack only investigators and whatever is attacking one of their own.
  - Attack one and it fights back, and comrades within 24 blocks join in.
  - They keep to their hideout.
  - The hideout's chests can hold Crimson pieces.

### Ayato Kirishima

- **Who he is:** an Ukaku companion with a full pair of wings.
  - He respawns like the others.
  - He fights with his Ukaku and goes into his kakuja below a third of his health, with a crystal burst up close.
  - His respirator mask goes on in a fight.
- **Romance:** you said "Ayato romance should be quite similar to kaneki, animations can be the same". It runs on Ken's animations and pace and uses the same affection, dating and exclusivity: five courtship chapters, six dates and his own words (`talk/ayato.txt`).
- **His questline:**
  - Two proofs of respect (ranked investigators, A-rank ghouls) and an S-rank kill.
  - Then he teaches the Ukaku's first kakuja as a trial, lived through in his own cutscene.
  - Touka no longer teaches it.
- **Summoning:** operators can call him with `/kagune ayato here`.

### Noro

- **Who he is:** you said "Noro is taller than the average player and is very brief, barely talks". He is taller, masked and almost silent, and can never be romanced (a friend at most).
- **What he teaches:** the Bikaku, in order, each after a proof, and each still needing its mastery. Saves already past a move's mastery keep it.
  - The Worms: Twin and Three Worms, Ravenous Pursuit, Carrion Eruption, Monstrous Regeneration.
  - The Walker.
  - The kakuja.
  - Grand Devouring.
  - Potential Unlock.
- **In a fight:**
  - He walks at you and lets his three worms work from 14 blocks: bites in turn (each one feeds him), and moves made by all three (Devouring Maw, Predatory Drag, Carrion Eruption, Ravenous Pursuit).
  - He regenerates abnormally.
  - The first blow that should kill him in two minutes leaves him mending instead.
- **Summoning:** `/kagune noro here`.

### Your to-do list (all done)

- **Bikaku regeneration.** Your words: "Bikaku users dont have the same regeneration as regular wounds, they either regenerate with this mutation or with their other ability to regenerate, wounds appear like any other ghoul but regen for them does not cost energy at all, it works with cooldowns, and waiting."
  - Potential Unlock's torn parts always come back on their own ~5 s timeline. They never drain energy and never wait for it.
  - Monstrous Regeneration and Undying Abomination cost no energy; their cooldowns are their price.
  - A Bikaku ghoul's ordinary wounds show as anyone's and close after the usual wait, costing nothing (not even the flesh reserve).
  - Surviving a killing blow with Potential Unlock still needs energy in the bar, as you first asked.
- **The Walker.** Your words: "needs more flluidity and better arching, it walks super robotic ... the legs look like sticks".
  - Steps are slower and eased in and out: the foot lifts quickly and is set down softly, higher on a longer stride.
  - Each leg is one smooth arch (up out of the hip, over, down onto its foot) that lifts higher while its foot is in the air, with a slow wave down it.
  - The legs are drawn thick at the hip, tapering to the foot.
- **Children.** Your words: "Make sure The child of a Bikaku user with all the other combinations matches perfectly ... just 2 ukaku wings, just Bikaku worm tendrils, or both."
  - A Bikaku parent (a player on the path, or Noro) passes on a worm. Two Bikaku parents give two worms, one from each.
  - Of two different kinds, a third of children take the mother's kind whole, a third the father's whole, and a third both. With both, the wings go down to one beside a tentacle; a worm carried low leaves room for both wings.
  - A child's worm bites in fights (snap, lunge, maul, each feeding it a little) and is drawn at the child's size in its colour.
  - Children born before keep what they have.

### My guesses

- Rank numbers and their spread; the 60% mask chance; the 4% drop.
- Structure spacing.
- Aogiri's 24-block call.
- Ayato's kakuja threshold and the order of his proofs.
- Noro's numbers (reach 14, his bites and moves, the two-minute mend).
- The mask fitting: the half-mask top, the eye row, 5% girth.
- The armour's snugness: 0.3 clearance, then 45%.
- The children's thirds; the child worm's reach and bite.
- The Walker's step length (8 ticks) and arch.

### Known limits

- Not tested in game. The previews (mask and outfit renders, the Walker's arch) were drawn from the baked geometry and code outside the game.
- The owner's character masks are fitted by remapping their shapes. A mask whose eyes are painted rather than cut may still look a pixel off on some skins.
- The old village's structures already in a world stay.

### Network and tests

- **Network:** protocol 105. The outfits and masks are new items, so clients and the server must both be on 4.0.0.
- **Tests:** OutfitTests (catalogue, box UV, slim sleeves, half masks below the eyes, generic masks clear of the hat layer); NpcTests (ranks); AgentTests; FamilyTests (every Bikaku pairing, the thirds, the child's worm); BikakuTests and BikakuWormTests (free regeneration, the Walker's arch and steps); CompanionTests (Ayato's and Noro's scripts); the full suite.

Validation: the full `python3 build.py` (javac --release 17, the regression suites, the API/bytecode and archive checks, dedicated-server loading checks) completed with EXIT 0 on these sources before this was pushed. Not tested in game.
