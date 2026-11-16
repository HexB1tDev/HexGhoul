# The CCG: research notes and plan (started 1.3.25)

What the mod's lore already says: ghouls call investigators **Doves**. Touka: "White coats, suitcases full of kagune. Ours. They cut us up
and make weapons out of us." Eto warns about "someone hunting ghouls with suitcases". Everything below builds on that.

## What the CCG is (source material, in short)

- **The Commission of Counter Ghoul**: the government body that hunts ghouls. Investigators work in pairs or squads, wear long white
  coats, and carry their weapon shut in an **attaché case**.
- **Quinque**: a weapon made from a dead ghoul's **kakuhou** (the organ that makes its kagune). The case opens from its handle and the
  quinque unfolds in the hand. A quinque keeps the traits of the kagune it came from:
  - **Bikaku**: balanced, segmented blades and swords (the all-rounders).
  - **Koukaku**: hard and heavy: lances, shields, clubs. The toughest quinques.
  - **Ukaku**: light and ranged: projectile launchers, quick blades.
  - **Rinkaku**: flexible and vicious: whips, scythes, segmented weapons. Fragile, regenerating.
  - Quinques are **rated** like the ghouls they came from (a strong ghoul makes a strong quinque) and they **suppress RC cells**: a ghoul
    cut by one heals slower.
- **Quinque steel (Q-steel)**: RC-cell metal used for bullets and armour; a special armour suit worn by top investigators is made of it.
- **Ranks** (lowest to highest): Rank 3 Investigator, Rank 2, Rank 1, First Class, Associate Special Class, Special Class.
- Investigators train at an academy; quinque engineers build and maintain the weapons.

Web sources were thin (mostly merchandise pages); the notes above are consistent with what the mod's own lore already uses.

## Done in 1.3.25

- **Quinque cases** that open into weapons: a **Bikaku blade** (sword) and a **Koukaku spear** (lance). Right click opens the case
  (animated: the case splits on its glowing core, the weapon runs out); Shift + right click folds it back. Netherite-tier swords
  (enchantable, they wear), 1.5x/1.6x damage against ghouls, a cut holds a ghoul player's regeneration back 3-4 s, the spear has +1.5
  reach. Crafted from iron, leather, a ghoul's meat (its RC cells), netherite scrap and redstone (blade) or a diamond (spear).
- Code: `quinque/Quinque` (pure rules), `server/QuinqueServer`, `client/QuinqueClient`, `tools/quinque_models.py` (textures and the five
  frames per kind), `QuinqueTests`.

## Next, in order

1. **Kakuhou drops**: a ghoul killed (wild ghouls, ghoul players) drops its kakuhou, typed by its kagune and rated by its strength. A
   quinque is then crafted from a kakuhou of its type (replacing ghoul meat in the recipe), and a higher rated kakuhou makes a stronger
   quinque (stored on the stack).
2. **Investigator NPCs**: white coats, case in hand; they patrol towns and roads at night in pairs; a rank decides their quinque and
   health. They attack ghoul players whose kakugan or kagune is out (or who are seen feeding), and wild ghouls; they leave humans
   alone. Their case opens with the same animation when a fight starts. Killing one may drop its quinque case.
3. **More quinques**: an Ukaku launcher (fires RC shards, slow reload) and a Rinkaku whip (long sweeping reach).
4. **Q-steel**: bullets for a CCG pistol, and Q-steel armour (resists kagune).
5. **A CCG branch office** structure (like Anteiku and the tower: one per world or rare), with investigators and a quinque lab.
6. **Standing**: ghouls vs Doves: killing investigators raises a ghoul's notoriety (more patrols, higher ranks come); companions react.

Name rule reminder: no character names of the show anywhere in the jar (`check_names`, PaleTests). Name quinques and investigators
generically or originally.
