## 2.0.36 (HexGhoul): four new villager tempers

HexKagune 2.0.36 for Forge 1.20.1. Install matching versions on client and server. Not tested in game.

Villagers have four new tempers, for twelve in all: Playful (teasing, games and pranks), Stoic (calm, few words, steady), Sarcastic (dry wit), and Romantic (a hopeless romantic, flowery and sincere). Each has its own lines for everything a villager says and its own body language.

- Every line set has them: greetings (stranger, warm, close, partner), small talk, about themselves, compliments, goodbyes, a ghoul's confession (taken, doubted, refused), saying yes, "I love you", kisses, hugs, walks hand in hand, letting go, the village's edge, "What's your type?", and the kind "not my type". They also get their own trust for a confession and their own warmth to compliments.
- Body language: the playful and romantic move brightly (Eto's way), the stoic steadily (Ken's), the sarcastic reserved (Touka's), with their own hello, chat, compliment and idle gestures.
- Nobody already in your world changes: villagers placed before keep the temper their seed always gave them. Villagers placed from now on (new villages) roll any of the twelve, saved with them (`HexHumanTemper`). The enum is appended to (saved by ordinal).
- Tested by the existing per-temper checks, which now cover all twelve: every line filled, no banned names. Also: villagers placed before only ever have the first eight, and new ones can have all twelve. Protocol stays 77.
