## 1.3.36 (HexGhoul): a new kagune only wild ghouls have: the arm blade, a Koukaku that spirals down the arm into a blade, with its own wind-up, three-cut string, guard and sprint (testing)
HexKagune 1.3.36 for Forge 1.20.1. Install on client and server (**protocol 67**: changed, so client and server must both be 1.3.36). Testing build: compiled, tests pass, every pose checked in offline renders, **not tried in game**.

- **The arm blade.** A new kagune type only wild ghouls have; players can't pick it. It's a Koukaku (the hard, armoured kagune of the shoulder blade). It tears out of the right shoulder blade and winds down the arm in a spiral of hard plates, closes over the forearm and grows into a long ridged blade past the fist, with a glowing seam down each side and twisted fins at its root, like a lance. Each ghoul's comes in its own colour.
- **Winding it out.** When one draws its kagune, it brings its right arm up across its chest and grips the forearm with its other hand, head bowed to watch, while the plates spiral down. Then it flings the new blade out to the side and drops into its guard.
- **Its guard:** right side forward, blade arm low and out in front, the blade along it toward you, free arm up across the chest.
- **Its M1s, a string of three:** a flat cut from the right across to the left, chest turning through it; a rising backhand from low left to high right; then a lunging thrust of the point, long step, free arm flung back. Each cut steps in and lands partway through. A blow that meets one coming the other way throws both apart (the same clash every kagune has). The thrust reaches furthest, hits hardest and knocks you back. After the third it circles you, blade low, then comes again. It sometimes answers your swing with a cut straight back.
- **Its sprint:** pitched forward, blade arm swept back so the blade trails level behind it, other arm pumping.
- About 28% of wild ghouls carry it now (the rest are Rinkaku and Ukaku, about even). Older ghouls in your saves keep the kagune they had.
- `/kagune npc koukaku` (or `blade`) summons one to try (op).
- Wild ghouls' bodies now take the full pose system the investigators use (chest turn, lean, step-in), so their flips and cuts carry the whole body.

Untested in game: compiled, unit tested and the poses and blade checked in offline renders only. Try: `/kagune npc koukaku`, then provoke it.
