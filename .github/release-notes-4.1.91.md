## 4.1.91 (HexGhoul): every Ukaku sound is Minecraft's own (the clash untouched)

HexKagune 4.1.91 for Forge 1.20.1. Not tested in game. Protocol unchanged (127).

You said (quoted exactly): "I need you to rework the the Ukaku sounds to use only Minecraft sounds that are ideal for every single thing
that form does. The only thing you're not allowed to touch is the clash sounds. That's a universal thing. It's not related to that. But
change every sound in the Ukaku so that every sound is Minecraft based. Just make sure that you pick the most badass sounds possible. Like
for each situation, such as like firing projectiles, crystal projectiles, the slaps with the normal wings and whatsoever."

**Why it wasn't already:** most of the Ukaku's code named vanilla sounds, but the mod swaps every vanilla name it knows for one of the
recorded pack's sounds, so you never heard them. Now every Ukaku sound names the game's own sound with its namespace and plays exactly
that, at the volume and pitch the move asks for. Each one is checked against vanilla 1.20.1's own sound list.

| Situation | Minecraft sound |
|---|---|
| Wing slaps, wing swings and slams (the swing) | the Ender Dragon's wing flap, deeper and louder the heavier the blow |
| A wing slap landing | a strong hit (`attack.strong`); a heavy one that throws them, the knockback hit |
| A wing slam landing | the Warden's attack impact, with a small blast under it |
| Wing slashes, cross cuts, the one-wing cuts, the beheading | the dragon's flap and a sweeping blade together |
| Kicks | the swing's swish |
| The Steel Blade's swings | a sweeping blade (the execution with a dragon's flap) |
| A shot's wind-up (the wings' flick before it) | the Phantom's flap |
| The Rabbit Dash, a rush through the air | the Phantom's swoop |
| RC shards fired | the Blaze's shot |
| Heavy volleys, flares, fire fans, flame bursts | a fire charge |
| The flame pillar | the Ghast's fireball, with a fire charge |
| Burning, flames out | burning, fire extinguished |
| Shards bursting (Bursting Shards) | a firework's blast |
| A flare's splash, the big blasts | an explosion |
| Crystal projectiles fired (crystal shot, crystal lance, needle volleys, the Gatling) | a crossbow's shot (the lance with cracking amethyst, the Gatling with amethyst hits) |
| Crystals growing, ringing, humming | amethyst cluster growing, amethyst chime, amethyst resonating |
| Crystals hitting, cracking, shattering | amethyst hit, amethyst cluster breaking, amethyst block breaking, glass breaking |
| A crystal driven into flesh | a trident's hit |
| A crystal shot's detonation | a lightning strike's crack and a large firework blast |
| The Thorns bursting out | the Evoker's fangs, with amethyst |
| The Wing Vortex spinning up and thrown | Riptide I and Riptide III |
| The Steel Blade drawn, put away, setting | a trident returning, netherite armour, an anvil |
| The Steel Blade's vanish and reveal | the Illusioner's mirror move (and the dragon's flap) |
| The wings tearing out (yours and Ayato's) | a fire charge |
| The Ukaku's kakuja | a Totem of Undying |
| The final kakuja | the Wither's spawn roar |
| Coming out of the kakuja | a respawn anchor running out |
| A mutation learned | the enchanting table |
| NPC Ukaku users' wings (their flaps, slaps, crystals) | the same sounds |

- **The clash sounds are untouched** (your own recordings, for every form).
- The Rinkaku's Full Kakuja shares the vanish dash with the Steel Blade: it keeps its old sounds; only the Ukaku's changed.

### Choices of mine you may want changed

- The final kakuja's Wither roar is long and loud on purpose; if it's too much, the Ender Dragon's growl is the next pick.
- The wings tearing out use a fire charge's whoomph (the wings are fire and crystal); a Totem's burst is the other candidate.

### Other

- `ukaku/UkakuSounds` holds every Ukaku sound in one place; `tools/vanilla_sounds_1.20.1.txt` is vanilla 1.20.1's sound list the tests
  check against.
- Tests: `SfxTests.ukakuVanilla491`; 20 rules sabotaged one at a time (`docs/handoff/sabotage-4.1.91.json`).
