## 3.0.7 (HexGhoul): Recorded insect, swing and flesh sounds; finger crack leads the mix

HexKagune 3.0.7 for Forge 1.20.1. Network protocol 88 (unchanged). Not tested in game.

Centipede legs now use real elephant-beetle and close-recorded grasshopper sounds. Both Rinkaku half kakujas use short, quiet Madagascar hissing-cockroach recordings; emergence and dismissal use insects too. Graft's centipede stage and its centipede-bearing kakuja follow the same sound palette.

Kagune swing, heavy swing, arc, dash and whoosh pools use Benjamin Burnes's recorded Organic Wooshes. The Steel Blade's extra trident-throw layer is removed: it was adding metallic sound after the swing had played.

The bleeding effect itself has no audio callback. Hits opening a wound play flesh_impact -> sfx.blood_splat (including UkakuCrystalCombat's impale); other cuts and tears use blood_spray, slash_spray and gush. Those old synthetic splats/sprays/gushes, including the reused dense_squelch, are no longer selected by any blood/flesh pool. The exact random variant the owner heard was not identified by listening; replacing the complete offending pools prevents any of those old variants from recurring. Stab samples are unchanged.

The owner's finger_crack.ogg is byte-for-byte unchanged, at full gain. tools/sfx_pack.py now measures mean and peak level, with no minimum-gain floor: ordinary pack sounds target -34 dB mean and peak <= 0.16; insect emergence/clicks and writhing target -40 dB and peak <= 0.06. Clashes, their gust and rumble have extra headroom for the largest layered call. The eye-opening cue is attenuated too. These are authored/off-game levels; final listening and multiplayer balance still need checking in game.

16 mono OGG excerpts from 14 real recordings, all CC0 1.0. Sources: warrenXG (elephant beetle), cregstorm (grasshopper), TheKingOfGeeks360 (hissing cockroach), Benjamin Burnes / Abstraction (Organic Wooshes and CaptSubtle Melon). tools/sfx_recordings.json records every source's author, title, licence evidence, pinned GitHub download and SHA-256. Only tools/sfx_pack.py imports sound assets and writes sounds.json. Run it with --import-recordings SOURCE_DIRECTORY to verify and import downloaded masters; without arguments it rebuilds the event map. Processing is limited to excerpts, short edge fades, mono/resampling and Vorbis conversion: no synthesized waveform, pitch alteration or layering. The pack includes recorded/CREDITS.txt, SOURCES.json (source/output hashes and exact edit spans), and CC0-1.0.txt.

Sound references, subtitle keys, source/output checksums, mono channels, routing, unchanged stab selections and the conservative largest-clash peak bound are checked off-game. The full release build (python3 build.py, Java 17) completed with EXIT 0, including all 34 pure-logic suites, sound checks, bytecode/API verification, archive scope and dedicated-server loading checks. Network protocol stays 88; no new Minecraft API. Nothing tested in game.
