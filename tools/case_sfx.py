#!/usr/bin/env python3
"""1.3.28: the quinque case's own sounds, opening and shutting (python3 tools/case_sfx.py; numpy and ffmpeg).

A real attache case's latches on a mechanism: no recording could be fetched where this was made, so each is built from its parts the
way they sound (each part a struck or scraped piece of metal: a short excitation ringing through the modes of what it hits, the case's
shell under every click as a low knock):

  open   the two latches thumbed back one after the other (a button pressed, a spring snapping the hasp up against its stop), the
         seal letting go (a breath of air), a geared servo spinning up under it (a motor's whine, its gear teeth ticking, friction),
         plates sliding out over each other, two segments locking, and the last lock home (a bolt's click, then the heavy clunk)
  close  the servo running the other way (its whine falling), the plates folding back with two clacks, the halves meeting (a deep
         knock), and the two latches snapped shut

Both fit Quinque.UNFOLD (12 ticks, 0.6 s): the last lock (open) or the second latch (close) lands at about 0.56 s, the rest is the
ring and the room. Two takes of each (timing, pitch and weight a little different), mono Vorbis at 44.1 kHz like the rest of the pack,
written to src/main/resources/assets/hexkagune/sounds/sfx/quinque/ (tools/sfx_pack.py makes them the pools sfx.case_open and
sfx.case_close).
"""
import math
import pathlib
import subprocess
import tempfile
import wave

import numpy as np

ROOT = pathlib.Path(__file__).resolve().parent.parent
OUT = ROOT / "src/main/resources/assets/hexkagune/sounds/sfx/quinque"
RATE = 44100
LENGTH = 1.25  # seconds


class Take:
    def __init__(self, seed):
        self.rng = np.random.default_rng(seed)
        self.n = int(LENGTH * RATE)
        self.t = np.arange(self.n) / RATE
        self.out = np.zeros(self.n)

    # ---------------------------------------------------------------------------------------------------- building blocks

    def jitter(self, value, spread):
        return value * (1.0 + self.rng.uniform(-spread, spread))

    def at(self, t0):
        return max(0, min(self.n - 1, int(round(t0 * RATE))))

    def noise(self, length):
        return self.rng.standard_normal(length)

    def ring(self, t0, modes, gain, attack=0.0004):
        """Decaying partials from t0: modes [(hz, decay s, weight)], each with a random phase, a raised-cosine attack."""
        i = self.at(t0)
        tt = self.t[i:] - self.t[i]
        env_a = np.clip(tt / attack, 0.0, 1.0)
        env_a = 0.5 - 0.5 * np.cos(math.pi * env_a)
        sig = np.zeros_like(tt)
        for hz, decay, weight in modes:
            hz = self.jitter(hz, 0.015)
            sig += weight * np.exp(-tt / decay) * np.sin(2 * math.pi * hz * tt + self.rng.uniform(0, 2 * math.pi))
        self.out[i:] += gain * env_a * sig

    def burst(self, t0, length, gain, low=None, high=None, shape=0.0006):
        """A filtered noise transient (the contact itself): {length} s, an exponential tail of {shape} s."""
        i = self.at(t0)
        m = min(self.n - i, int(length * RATE))
        tt = np.arange(m) / RATE
        sig = self.noise(m) * np.exp(-tt / shape)
        sig = band(sig, low, high)
        self.out[i:i + m] += gain * sig

    # ---------------------------------------------------------------------------------------------------- the case's parts

    def shell(self, t0, gain, deep=1.0):
        """The case's body knocked: its panels' low modes (an aluminium-framed shell: a hollow, damped knock)."""
        self.ring(t0, [(self.jitter(168, 0.04) * deep, 0.05, 1.0), (self.jitter(262, 0.04) * deep, 0.038, 0.7),
                       (self.jitter(395, 0.04) * deep, 0.028, 0.5), (self.jitter(610, 0.04), 0.02, 0.35),
                       (self.jitter(905, 0.05), 0.014, 0.25)], gain, attack=0.0012)

    def latch_open(self, t0, gain, tone=1.0):
        """A latch thumbed back: the button's soft press, then the spring snapping the hasp up, it hitting its stop."""
        self.burst(t0, 0.006, 0.18 * gain, 900, 4200, 0.0012)                      # the thumb on the button
        self.ring(t0, [(1650 * tone, 0.006, 0.6), (2900 * tone, 0.004, 0.4)], 0.08 * gain)
        snap = t0 + self.jitter(0.012, 0.2)
        self.burst(snap, 0.004, 0.9 * gain, 2500, 12000, 0.0005)                  # the spring lets go
        self.ring(snap, [(2340 * tone, 0.018, 1.0), (3860 * tone, 0.012, 0.75), (5610 * tone, 0.009, 0.55),
                         (8070 * tone, 0.006, 0.35), (10350 * tone, 0.004, 0.2)], 0.55 * gain)
        stop = snap + self.jitter(0.0065, 0.25)
        self.burst(stop, 0.003, 0.45 * gain, 1800, 9000, 0.0004)                   # the hasp hits its stop
        self.ring(stop, [(1980 * tone, 0.015, 1.0), (3320 * tone, 0.01, 0.6), (4720 * tone, 0.007, 0.4)], 0.3 * gain)
        self.shell(snap, 0.32 * gain)

    def latch_shut(self, t0, gain, tone=1.0):
        """A latch snapped shut: the hasp pressed down over its catch, a firmer, lower clack, the shell under it."""
        self.burst(t0, 0.004, 0.7 * gain, 1500, 9000, 0.0006)
        self.ring(t0, [(1880 * tone, 0.016, 1.0), (3070 * tone, 0.011, 0.7), (4420 * tone, 0.008, 0.5), (6900 * tone, 0.005, 0.3)],
                  0.5 * gain)
        catch = t0 + self.jitter(0.004, 0.25)
        self.burst(catch, 0.003, 0.5 * gain, 3000, 12000, 0.0003)                  # the catch clicking over
        self.ring(catch, [(4100 * tone, 0.006, 1.0), (6350 * tone, 0.004, 0.6)], 0.22 * gain)
        self.shell(t0, 0.45 * gain, deep=0.92)

    def seal(self, t0, gain):
        """The lid's seal letting go: a short breath of air and the lid lifting off its seat."""
        i = self.at(t0)
        m = min(self.n - i, int(0.16 * RATE))
        tt = np.arange(m) / RATE
        env = np.clip(tt / 0.012, 0, 1) * np.exp(-tt / 0.05)
        self.out[i:i + m] += gain * band(self.noise(m), 1800, 7000) * env
        self.ring(t0 + 0.004, [(120, 0.04, 1.0), (190, 0.03, 0.5)], 0.25 * gain, attack=0.006)

    def servo(self, t0, t1, gain, f_from, f_to, ticks_from, ticks_to):
        """A geared servo running from t0 to t1: the motor's whine (its pitch from f_from to f_to as it spins up or down), its gear
        teeth ticking past (a rate from ticks_from to ticks_to a second), the friction of it all."""
        i0, i1 = self.at(t0), self.at(t1)
        m = i1 - i0
        u = np.linspace(0.0, 1.0, m)
        ease = 1 - (1 - u) ** 2.2
        freq = f_from + (f_to - f_from) * ease
        freq = freq * (1 + 0.012 * np.sin(2 * math.pi * 23 * u * (t1 - t0)) + 0.004 * self.noise(m).cumsum() / max(1.0, math.sqrt(m)))
        phase = 2 * math.pi * np.cumsum(freq) / RATE
        whine = np.zeros(m)
        for k in range(1, 10):
            whine += (1.0 / k ** 1.25) * np.sin(k * phase + self.rng.uniform(0, 2 * math.pi))
        whine = band(whine, 350, 3200)
        env = np.clip(u * (t1 - t0) / 0.03, 0, 1) * np.clip((1 - u) * (t1 - t0) / 0.045, 0, 1)
        sig = 0.55 * whine * env
        # The gear train: a tooth ticking past at a rate that follows the motor.
        rate = ticks_from + (ticks_to - ticks_from) * ease
        tooth = np.cumsum(rate) / RATE
        edges = np.nonzero(np.diff(np.floor(tooth)) > 0)[0]
        for e in edges:
            self.ring(t0 + e / RATE, [(self.jitter(4300, 0.05), 0.0025, 1.0), (self.jitter(6800, 0.05), 0.0018, 0.6),
                                      (self.jitter(2600, 0.05), 0.003, 0.4)], 0.07 * gain * env[e])
        # Friction: a grainy hiss, louder as it labours.
        sig += 0.34 * band(self.noise(m), 900, 4200) * env * (0.7 + 0.3 * np.abs(np.sin(2 * math.pi * 31 * u * (t1 - t0))))
        self.out[i0:i1] += gain * sig

    def slide(self, t0, t1, gain, f_from, f_to):
        """Plates sliding over each other: a scrape (noise through a narrow band sweeping f_from to f_to), stick and slip in it."""
        i0, i1 = self.at(t0), self.at(t1)
        m = i1 - i0
        u = np.linspace(0, 1, m)
        src = self.noise(m)
        sig = np.zeros(m)
        # A few overlapping narrow bands, swept: a coarse resonant filter bank.
        for k, w in ((1.0, 1.0), (1.63, 0.6), (2.41, 0.4)):
            centre = (f_from + (f_to - f_from) * u) * k
            sig += w * swept_band(src, centre, 0.08)
        stick = 0.55 + 0.45 * np.abs(np.sin(2 * math.pi * self.jitter(34, 0.15) * u * (t1 - t0)) ** 3)
        env = np.sin(math.pi * np.clip(u, 0, 1)) ** 0.7
        self.out[i0:i1] += gain * sig * stick * env

    def clack(self, t0, gain, tone=1.0):
        """A segment locking into place: a mid metallic clack with the shell under it."""
        self.burst(t0, 0.004, 0.6 * gain, 1200, 9000, 0.0006)
        self.ring(t0, [(1410 * tone, 0.035, 1.0), (2930 * tone, 0.025, 0.7), (4620 * tone, 0.016, 0.45), (7300 * tone, 0.008, 0.25)],
                  0.45 * gain)
        self.ring(t0, [(150, 0.04, 1.0), (235, 0.03, 0.5)], 0.3 * gain, attack=0.002)

    def lock(self, t0, gain):
        """The last lock home: a bolt's small click, then the heavy clunk (a thump that drops in pitch, the body, a ring that holds)."""
        self.burst(t0, 0.003, 0.4 * gain, 3000, 12000, 0.0004)
        self.ring(t0, [(3650, 0.008, 1.0), (5900, 0.005, 0.5)], 0.2 * gain)
        hit = t0 + self.jitter(0.019, 0.15)
        i = self.at(hit)
        tt = self.t[i:] - self.t[i]
        f = 62 + 48 * np.exp(-tt / 0.025)
        thump = np.sin(2 * math.pi * np.cumsum(f) / RATE) * np.exp(-tt / 0.07) * np.clip(tt / 0.0015, 0, 1)
        self.out[i:] += 0.85 * gain * thump
        self.burst(hit, 0.008, 0.9 * gain, 600, 8000, 0.0012)
        self.ring(hit, [(258, 0.06, 1.0), (412, 0.05, 0.7), (640, 0.035, 0.45)], 0.5 * gain, attack=0.0015)
        self.ring(hit, [(1180, 0.085, 0.6), (1965, 0.065, 0.75), (3150, 0.045, 0.55), (4780, 0.03, 0.35), (7020, 0.018, 0.2)], 0.3 * gain)

    def knock(self, t0, gain):
        """The two halves meeting (shutting): a deep, padded knock of the whole shell, the frame's edge ringing a little."""
        self.burst(t0, 0.01, 0.6 * gain, 300, 5000, 0.0018)
        self.shell(t0, 0.9 * gain, deep=0.85)
        self.ring(t0, [(95, 0.06, 1.0)], 0.5 * gain, attack=0.002)
        self.ring(t0, [(1320, 0.07, 0.5), (2210, 0.05, 0.4), (3480, 0.03, 0.25)], 0.16 * gain)

    # ---------------------------------------------------------------------------------------------------- the room, the end

    def finish(self):
        dry = self.out
        wet = room(dry, self.rng)
        mix = dry + 0.13 * wet
        mix = highpass(mix, 45)
        fade = np.ones(self.n)
        tail = int(0.08 * RATE)
        fade[-tail:] = np.linspace(1, 0, tail)
        mix *= fade
        peak = np.max(np.abs(mix))
        return mix / peak * 0.89 if peak > 0 else mix


# -------------------------------------------------------------------------------------------------------- filters (no scipy)

def biquad(x, b0, b1, b2, a1, a2):
    y = np.zeros_like(x)
    x1 = x2 = y1 = y2 = 0.0
    for n in range(len(x)):
        v = b0 * x[n] + b1 * x1 + b2 * x2 - a1 * y1 - a2 * y2
        x2, x1 = x1, x[n]
        y2, y1 = y1, v
        y[n] = v
    return y


def lowpass(x, hz, q=0.707):
    w = 2 * math.pi * hz / RATE
    alpha = math.sin(w) / (2 * q)
    c = math.cos(w)
    a0 = 1 + alpha
    return biquad(x, (1 - c) / 2 / a0, (1 - c) / a0, (1 - c) / 2 / a0, -2 * c / a0, (1 - alpha) / a0)


def highpass(x, hz, q=0.707):
    w = 2 * math.pi * hz / RATE
    alpha = math.sin(w) / (2 * q)
    c = math.cos(w)
    a0 = 1 + alpha
    return biquad(x, (1 + c) / 2 / a0, -(1 + c) / a0, (1 + c) / 2 / a0, -2 * c / a0, (1 - alpha) / a0)


def band(x, low, high):
    if low:
        x = highpass(x, low)
    if high and high < RATE / 2 * 0.95:
        x = lowpass(x, high)
    return x


def swept_band(x, centre, width):
    """A two-pole resonator whose centre follows {centre} (Hz, per sample), bandwidth {width} of it."""
    y = np.zeros_like(x)
    y1 = y2 = 0.0
    for n in range(len(x)):
        w = 2 * math.pi * centre[n] / RATE
        r = 1 - math.pi * width * centre[n] / RATE
        v = (1 - r) * x[n] + 2 * r * math.cos(w) * y1 - r * r * y2
        y2, y1 = y1, v
        y[n] = v
    return y / max(1e-9, np.max(np.abs(y))) * 0.5


def room(x, rng):
    """A small room: early reflections, then a short diffuse tail (four combs into two allpasses), darkened."""
    out = np.zeros_like(x)
    for ms, g in ((6.1, 0.42), (11.3, 0.33), (17.9, 0.26), (26.6, 0.19), (35.2, 0.13)):
        d = int(ms / 1000 * RATE)
        out[d:] += g * x[:-d]
    tail = np.zeros_like(x)
    for ms, fb in ((29.7, 0.5), (37.1, 0.48), (41.1, 0.46), (43.7, 0.44)):
        d = int(ms / 1000 * RATE)
        buf = x.copy()
        for n in range(d, len(buf)):
            buf[n] += fb * buf[n - d]
        tail += buf
    for ms, g in ((5.0, 0.7), (1.7, 0.7)):
        d = int(ms / 1000 * RATE)
        y = np.zeros_like(tail)
        for n in range(len(tail)):
            xd = tail[n - d] if n >= d else 0.0
            yd = y[n - d] if n >= d else 0.0
            y[n] = -g * tail[n] + xd + g * yd
        tail = y
    out += 0.35 * tail / max(1e-9, np.max(np.abs(tail))) * np.max(np.abs(x))
    return lowpass(out, 5200)


# -------------------------------------------------------------------------------------------------------- the two sounds

def case_open(seed):
    s = Take(seed)
    j = s.jitter
    first = j(0.004, 0.5)
    s.latch_open(first, 1.0, tone=j(1.0, 0.03))
    second = first + j(0.085, 0.15)
    s.latch_open(second, 1.12, tone=j(0.97, 0.03))
    s.seal(second + j(0.06, 0.2), 0.22)
    start = second + j(0.075, 0.15)
    s.servo(start, j(0.52, 0.03), 0.26, j(150, 0.06), j(290, 0.06), 70, 150)
    s.slide(start + 0.03, j(0.5, 0.03), 0.22, j(1700, 0.08), j(3100, 0.08))
    s.clack(j(0.33, 0.04), 0.55, tone=j(1.0, 0.05))
    s.clack(j(0.43, 0.03), 0.65, tone=j(0.92, 0.05))
    s.lock(j(0.535, 0.02), 1.15)
    return s.finish()


def case_close(seed):
    s = Take(seed)
    j = s.jitter
    s.servo(j(0.005, 0.5), j(0.36, 0.04), 0.25, j(280, 0.06), j(140, 0.06), 140, 65)
    s.slide(j(0.03, 0.3), j(0.34, 0.04), 0.2, j(3000, 0.08), j(1600, 0.08))
    s.clack(j(0.13, 0.08), 0.5, tone=j(1.04, 0.05))
    s.clack(j(0.25, 0.06), 0.55, tone=j(0.95, 0.05))
    shut = j(0.4, 0.03)
    s.knock(shut, 1.0)
    first = shut + j(0.075, 0.12)
    s.latch_shut(first, 0.95, tone=j(1.0, 0.03))
    s.latch_shut(first + j(0.07, 0.15), 1.05, tone=j(0.96, 0.03))
    return s.finish()


def write_ogg(signal, path):
    pcm = (np.clip(signal, -1, 1) * 32767).astype("<i2")
    with tempfile.TemporaryDirectory() as tmp:
        raw = pathlib.Path(tmp) / "take.wav"
        with wave.open(str(raw), "wb") as w:
            w.setnchannels(1)
            w.setsampwidth(2)
            w.setframerate(RATE)
            w.writeframes(pcm.tobytes())
        subprocess.run(["ffmpeg", "-hide_banner", "-loglevel", "error", "-y", "-i", str(raw), "-ac", "1", "-ar", str(RATE),
                        "-c:a", "libvorbis", "-q:a", "6", "-map_metadata", "-1", str(path)], check=True)


def main():
    OUT.mkdir(parents=True, exist_ok=True)
    for name, make, seeds in (("case_open", case_open, (1728, 9031)), ("case_close", case_close, (4410, 615))):
        for k, seed in enumerate(seeds, 1):
            path = OUT / f"{name}_{k}.ogg"
            write_ogg(make(seed), path)
            print(f"wrote {path.relative_to(ROOT)}")


if __name__ == "__main__":
    main()
