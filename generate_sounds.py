"""Generate cute boop/uwu WAV sound effects for the furry app."""
import struct
import wave
import math
import os
import random

RAW_DIR = os.path.join("app", "src", "main", "res", "raw")
SAMPLE_RATE = 44100

def generate_tone(frequency, duration, volume=0.8, fade_in=0.02, fade_out=0.05):
    """Generate a sine wave tone with fade in/out."""
    samples = []
    num_samples = int(SAMPLE_RATE * duration)
    fade_in_samples = int(SAMPLE_RATE * fade_in)
    fade_out_samples = int(SAMPLE_RATE * fade_out)
    
    for i in range(num_samples):
        t = i / SAMPLE_RATE
        sample = volume * math.sin(2 * math.pi * frequency * t)
        
        # Fade in
        if i < fade_in_samples:
            sample *= i / fade_in_samples
        # Fade out
        elif i > num_samples - fade_out_samples:
            sample *= (num_samples - i) / fade_out_samples
        
        samples.append(sample)
    return samples

def generate_boop(base_freq, duration=0.15, drop_ratio=0.6):
    """Generate a 'boop' sound - frequency drops down quickly."""
    samples = []
    num_samples = int(SAMPLE_RATE * duration)
    
    for i in range(num_samples):
        t = i / num_samples
        # Frequency drops from base_freq to base_freq * drop_ratio
        freq = base_freq * (1.0 - t * (1.0 - drop_ratio))
        phase = 2 * math.pi * freq * (i / SAMPLE_RATE)
        volume = 0.7 * (1.0 - t * 0.7)  # Volume also fades
        
        # Fade out
        if t > 0.8:
            volume *= (1.0 - t) / 0.2
        
        samples.append(volume * math.sin(phase))
    return samples

def generate_uwu_chirp(base_freq, duration=0.25):
    """Generate a cute chirp/uwu sound - frequency goes up then down."""
    samples = []
    num_samples = int(SAMPLE_RATE * duration)
    
    for i in range(num_samples):
        t = i / num_samples
        # Frequency rises then falls (bell curve-ish)
        freq_mult = 1.0 + 0.5 * math.sin(math.pi * t)
        freq = base_freq * freq_mult
        phase = 2 * math.pi * freq * (i / SAMPLE_RATE)
        volume = 0.6 * math.sin(math.pi * t)  # Smooth envelope
        
        samples.append(volume * math.sin(phase))
    return samples

def write_wav(filename, samples):
    """Write samples to a 16-bit mono WAV file."""
    filepath = os.path.join(RAW_DIR, filename)
    with wave.open(filepath, 'w') as wav_file:
        wav_file.setnchannels(1)
        wav_file.setsampwidth(2)  # 16-bit
        wav_file.setframerate(SAMPLE_RATE)
        
        for sample in samples:
            clamped = max(-1.0, min(1.0, sample))
            packed = struct.pack('<h', int(clamped * 32767))
            wav_file.writeframes(packed)

def combine_samples(*sample_lists, gap=0.03):
    """Combine multiple sample lists with a gap between them."""
    result = []
    gap_samples = [0.0] * int(SAMPLE_RATE * gap)
    for i, samples in enumerate(sample_lists):
        result.extend(samples)
        if i < len(sample_lists) - 1:
            result.extend(gap_samples)
    return result

# === Generate "Owie" (pain) sounds - cute yelps/squeaks ===

# pain1: short high boop (cute squeak)
write_wav("pain1.wav", generate_boop(880, duration=0.12))

# pain2: double squeak
s1 = generate_boop(900, duration=0.08)
s2 = generate_boop(1100, duration=0.1)
write_wav("pain2.wav", combine_samples(s1, s2, gap=0.04))

# pain3: descending owie
s1 = generate_boop(1000, duration=0.1)
s2 = generate_boop(700, duration=0.15)
write_wav("pain3.wav", combine_samples(s1, s2, gap=0.03))

# pain4: quick triple squeak
s1 = generate_boop(800, duration=0.06)
s2 = generate_boop(950, duration=0.06)
s3 = generate_boop(750, duration=0.08)
write_wav("pain4.wav", combine_samples(s1, s2, s3, gap=0.02))

# pain5: long descending whine
write_wav("pain5.wav", generate_boop(1200, duration=0.3, drop_ratio=0.3))

# === Generate "Silly" (funny) sounds - uwu chirps/boops ===

# funny1: classic boop
write_wav("funny1.wav", generate_boop(440, duration=0.2))

# funny2: uwu chirp (up-down)
write_wav("funny2.wav", generate_uwu_chirp(500, duration=0.3))

# funny3: double boop 
s1 = generate_boop(400, duration=0.12)
s2 = generate_boop(550, duration=0.15)
write_wav("funny3.wav", combine_samples(s1, s2, gap=0.05))

# funny4: silly ascending chirps
s1 = generate_uwu_chirp(400, duration=0.15)
s2 = generate_uwu_chirp(550, duration=0.15)
s3 = generate_uwu_chirp(700, duration=0.2)
write_wav("funny4.wav", combine_samples(s1, s2, s3, gap=0.03))

# funny5: deep boop
write_wav("funny5.wav", generate_boop(300, duration=0.25, drop_ratio=0.5))

print("All 10 furry sound effects generated!")
