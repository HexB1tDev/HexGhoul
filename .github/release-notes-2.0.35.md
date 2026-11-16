## 2.0.35 (HexGhoul): who villagers fall for

HexKagune 2.0.35 for Forge 1.20.1. Install matching versions on client and server. Not tested in game.

Every villager, man or woman, now has their own idea of who they fall for and what they look for in a partner. You can romance male and female villagers alike, as long as you're who they fall for; ask "What's your type?" to find out.

- Who they fall for, their own for life (from their seed): most the other sex (62%), a fair share both (22%), the rest their own (16%). Men and women alike.
- What they look for, also their own: strong, gentle, funny, mysterious, clever, a little dangerous, loyal, or caring. Those who like mystery or danger take a ghoul's confession more readily (+15%); loyal or caring ones a little (+5%).
- "What's your type?" (once they're warm to you, regard 20): they tell you who they fall for and what they look for, in their temper's voice.
- Asking out someone who doesn't fall for your gender (your choice on the gender screen) gets a kind, in-character no ("I only fall for women. Still my favourite person to talk to!"). You stay friends and the ask isn't offered again. A player who never chose a gender can date anyone, and couples already together stay together.
- Pure rules and lines in `village/HumanPersona` (`Attraction`, `Taste`, `type`, `notMyType`), tested: the shares for men and women over 20,000 seeds, who may date whom, every line for every temper, attraction and taste filled, saying who they fall for, with no banned names. Protocol stays 77.
