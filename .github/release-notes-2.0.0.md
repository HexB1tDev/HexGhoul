## 2.0.0 (HexGhoul): Release 2.0
HexKagune 2.0.0 for Forge 1.20.1. Install on client and server (**protocol 72**). Compiled, every test suite passes, the dedicated-server check passes. **Not yet tried in game**: please report anything that looks or feels wrong, and it gets fixed first.

### What 2.0 brings (since 1.3.67)

**Ghouls as living tanks (new in 2.0)**
- Mastery and forms no longer add health. War's and Peace's extra max health and the centipede forms' Health Boost are gone.
- Instead, every blow a ghoul takes is cut by its mastery (up to 35%), the ghoul meat it has devoured (up to 25%, half of it by 20 pieces), and its form (kagune out 10%, rinkaku stages up to 22%, any kakuja 32%, a final kakuja 50%). Fully grown in a final kakuja, it takes about a quarter of a blow, before War's and Peace's own cuts. The void and /kill are never softened.

**The Brick Village** (spawns in plains)
- Your village build, with **16 villagers**, two in every house. Each has a random personality and their own name and look, and no two in a village share either. There are **26 villager looks**, made from the children's skins with other hair, clothes and eyes.
- Villagers go home at night, trade and chat in the streets, never leave the village, and run from anyone showing eyes or kagune, ideally to a CCG agent. CCG agents patrol the streets (never the houses) and drop everything to defend a villager.
- **Talk and romance:** chat, compliments and news, by personality. Ask a villager out once they like you; a ghoul must confess first (they may need convincing). Partners can **kiss, hug, and go on dates**, walking hand in hand through the village (they let go at the edge).
- **Gestures:** villagers move with their words, using Eto's gestures (in Eto's, Ken's or Touka's style by personality) and the kids' talk poses. They wave to each other, yawn at dusk and idle in the streets. Blushes, hearts.
- **Blinking:** villagers and children blink (shut eyes made for every face, checked one by one).

**Children**
- **136 new looks:** 4 versions of most children's skins with other hair and eye colours. Long hair on the girls' backs changes too, and clothes never do. A child wears the version nearest the hair and eyes its genes gave it, so children take after their parents. (Existing children may change colour.)

**CCG and wild ghouls**
- CCG ranks (Rank 3 to Special Class, with more health and speed the higher they are). They only attack ghouls who show their eyes or kagune, and defending humans comes first.
- Wild ghouls hunt villagers at night with kagune and kakugan out, and hide them by day. Sharper, smoother kagune arm blades.

**Final kakujas**
- Everyone fears War and Peace (fleeing humans, cowering children, faltering CCG, companions flinching). War's back tendrils move like living kagune. Run animations for War and Peace.

**Fixes and polish**
- A bug and lag pass: calm villagers check for danger less often, children's textures are cached, and a kiss walk never leaves the village.
- An animation pass: six broken poses fixed for villagers and companions (giggle, laugh, lean, yawn, lick, the ring's hands over the mouth).
- The War/Peace choice screen shows "Toughness".

Full details per version: `.github/release-notes-1.3.68.md` through `.github/release-notes-1.3.83.md`.
