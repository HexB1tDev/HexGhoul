## 4.1.77 (HexGhoul): Arima looks at his broken blade, two seconds, then falls

HexKagune 4.1.77 for Forge 1.20.1. Not tested in game. Protocol unchanged (126).

Your words (quoted exactly): "after the blade breaks make him look at it, then wait like 2 seconds and then fall!"

- **He looks at it.** The blade shatters in his one hand as in 4.1.76; then, over half a second, he brings the broken hilt up before him
  at his chest, the stub out ahead, straightens from his guard, and bows his head to look at it: his eyes are aimed at where it snapped
  (worked out from where his hand holds it, every moment), his head bowed 25 to 33 degrees over it.
- **He waits about two seconds,** staring at it. Not frozen: the hilt sinks a little in his hand as the moment drags on and his head
  follows it down.
- **Then he falls,** as in 4.1.76: his arm drops, and he goes over straight on his face from his feet, lands with a thud, lies there 3
  seconds, and dies face down. His whole end: about 6.5 seconds.

### Choices of mine you may want changed

- He holds it at his chest, not up at his face (held higher, his look at it barely shows: his head hardly bows).
- His left hand hangs loose while he looks; it could come up to it too.

### Other

- Renders: `docs/handoff/arima-end-4.1.77.gif` (real speed, a frame a world tick, three-quarter beside his right side),
  `arima-end-4.1.77.png` (frame by frame, both sides), `arima-look-4.1.77.png` (the stare from four sides: three-quarter, front, his
  right-front, above).
- Tests: `AgentTests.reaperFallen`: about two seconds of staring; his eyes on where it snapped, within 3 degrees (his face's way worked out
  in the test itself); nothing else turning his head; bowed over it; the broken blade up before him above his waist; nothing held still
  between the shatter and the ground; 7 rules sabotaged one at a time, each caught.
