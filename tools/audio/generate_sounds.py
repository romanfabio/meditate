#!/usr/bin/env python3
"""Generate the Meditate app bell sounds (original work, MIT licensed like the rest of the repo).

Singing-bowl style bell built with additive synthesis:
  * a few inharmonic partials (ratios typical of a bowl: 1, 2.71, 5.15, 8.0 ...)
  * every partial has a slightly detuned twin, which produces the slow "shimmer" (beating)
  * soft attack, long exponential decay
  * a very quiet band-limited strike transient
  * light synthetic stereo reverb (decaying noise impulse response)

Usage:
    python generate_sounds.py [output_dir]

Requires: numpy, soundfile (libsndfile with Vorbis support). Falls back to WAV if OGG fails.
Default output dir: app/src/main/res/raw
"""
from __future__ import annotations

import sys
from pathlib import Path

import numpy as np

SR = 44100
RNG = np.random.default_rng(20261108)  # fixed seed -> reproducible output

# (frequency ratio, relative amplitude, decay time constant in seconds, detune twin in Hz)
PARTIALS = [
    (1.00, 1.00, 4.2, 0.45),
    (2.71, 0.45, 2.8, 0.70),
    (5.15, 0.18, 1.8, 0.90),
    (8.00, 0.07, 1.1, 1.20),
]


def strike(f0: float, length_s: float, gain: float = 1.0) -> np.ndarray:
    """One bowl strike of `length_s` seconds (mono, dry)."""
    n = int(length_s * SR)
    t = np.arange(n) / SR
    out = np.zeros(n)
    for ratio, amp, tau, detune in PARTIALS:
        f = f0 * ratio
        env = np.exp(-t / tau)
        phase = RNG.uniform(0, 2 * np.pi, 2)
        out += amp * env * (
            0.6 * np.sin(2 * np.pi * f * t + phase[0])
            + 0.4 * np.sin(2 * np.pi * (f + detune) * t + phase[1])
        )
    # soft attack (about 18 ms raised-cosine) so there is no click
    attack = int(0.018 * SR)
    out[:attack] *= 0.5 * (1 - np.cos(np.pi * np.arange(attack) / attack))
    # very quiet strike transient: short noise burst, low-passed by moving average
    burst_len = int(0.035 * SR)
    burst = RNG.normal(0, 1, burst_len) * np.exp(-np.arange(burst_len) / (0.008 * SR))
    burst = np.convolve(burst, np.ones(24) / 24, mode="same")
    out[:burst_len] += 0.05 * burst * (np.arange(burst_len) / burst_len) ** 0.3
    return gain * out


def reverb_ir(seconds: float = 2.8, decay_s: float = 0.9) -> np.ndarray:
    """Stereo impulse response: exponentially decaying, darkened noise."""
    n = int(seconds * SR)
    t = np.arange(n) / SR
    irs = []
    for _ in range(2):
        noise = RNG.normal(0, 1, n) * np.exp(-t / decay_s)
        # progressively darker tail: running low-pass via cumulative smoothing
        k = 48
        noise = np.convolve(noise, np.ones(k) / k, mode="same")
        noise[0] = 1.0  # direct path
        irs.append(noise / np.sqrt(np.sum(noise**2)))
    return np.stack(irs, axis=1)


def apply_reverb(dry: np.ndarray, ir: np.ndarray, wet: float = 0.3) -> np.ndarray:
    n = len(dry) + len(ir) - 1
    size = 1 << (n - 1).bit_length()
    spec = np.fft.rfft(dry, size)
    out = np.zeros((n, 2))
    for ch in range(2):
        out[:, ch] = np.fft.irfft(spec * np.fft.rfft(ir[:, ch], size), size)[:n]
    dry_st = np.zeros((n, 2))
    dry_st[: len(dry), :] = dry[:, None]
    return (1 - wet) * dry_st + wet * out


def mix(strikes: list[tuple[float, float, float]], f0: float, tail_s: float) -> np.ndarray:
    """strikes = [(start_time_s, gain, ...)], returns stereo float array normalised to -9 dBFS."""
    last_start = max(s[0] for s in strikes)
    total = last_start + tail_s
    dry = np.zeros(int(total * SR))
    for start, gain, _ in strikes:
        s = strike(f0, tail_s, gain)
        i = int(start * SR)
        dry[i : i + len(s)] += s[: len(dry) - i]
    wet = apply_reverb(dry, reverb_ir())
    # fade the last 2 s smoothly to silence
    fade = int(2.0 * SR)
    wet = wet[: len(dry)]
    wet[-fade:] *= np.linspace(1, 0, fade)[:, None] ** 2
    peak = np.max(np.abs(wet))
    return wet * (10 ** (-9 / 20) / peak)


def write(path: Path, data: np.ndarray) -> Path:
    import soundfile as sf

    try:
        # Write in small blocks: encoding one huge buffer can overflow the stack
        # inside libsndfile's Vorbis encoder on Windows.
        block = np.ascontiguousarray(data, dtype=np.float32)
        with sf.SoundFile(str(path), "w", SR, 2, format="OGG", subtype="VORBIS") as f:
            for i in range(0, len(block), 4096):
                f.write(block[i : i + 4096])
        return path
    except Exception as exc:  # noqa: BLE001
        wav = path.with_suffix(".wav")
        print(f"OGG failed ({exc}); writing WAV instead", file=sys.stderr)
        sf.write(str(wav), data, SR, subtype="PCM_16")
        return wav


def main() -> None:
    root = Path(__file__).resolve().parents[2]
    out_dir = Path(sys.argv[1]) if len(sys.argv) > 1 else root / "app/src/main/res/raw"
    out_dir.mkdir(parents=True, exist_ok=True)

    # Start: one gentle strike (A3-ish, 220 Hz).
    start = mix([(0.0, 1.0, 0)], f0=220.0, tail_s=9.0)
    # End: three strikes, a little lower (G3, 196 Hz), spaced 5 s apart, last one softer.
    end = mix([(0.0, 1.0, 0), (5.0, 0.85, 0), (10.0, 0.7, 0)], f0=196.0, tail_s=10.0)

    for name, data in (("bell_start", start), ("bell_end", end)):
        p = write(out_dir / f"{name}.ogg", data)
        print(f"wrote {p} ({len(data) / SR:.1f}s, peak {np.max(np.abs(data)):.3f})")


if __name__ == "__main__":
    main()
