"""
SPARC (SPectral ARC length) analysis for movement fluidity and stability.

Based on: Balasubramanian, S., Melendez-Calderon, A., Roby-Brami, A., & Burdet, E. (2015).
"On the analysis of movement smoothness." Journal of NeuroEngineering and Rehabilitation, 12, 112.

SPARC measures movement smoothness by computing the arc length of the normalized
Fourier magnitude spectrum of a speed profile. Smoother movements produce a spectrum
concentrated at low frequencies, yielding a shorter arc length (closer to 0).
Jerky, fragmented movements spread energy across higher frequencies, producing a
longer (more negative) arc length.
"""

import numpy as np
import pandas as pd
import matplotlib.pyplot as plt
from scipy.signal import welch
from dataclasses import dataclass


# ---------------------------------------------------------------------------
# 1. SPARC core implementation
# ---------------------------------------------------------------------------

def sparc(speed: np.ndarray, fs: float,
          padlevel: int = 4, fc: float = 10.0, amp_th: float = 0.05):
    """
    Compute the SPARC smoothness metric for a speed profile.

    Parameters
    ----------
    speed : 1-D array
        Movement speed (magnitude of velocity / angular velocity).
    fs : float
        Sampling frequency (Hz).
    padlevel : int
        Zero-padding factor for the FFT (2**padlevel * len(speed)).
    fc : float
        Max cutoff frequency (Hz) for the arc-length calculation.
    amp_th : float
        Amplitude threshold (fraction of max) below which the spectrum
        is not considered when choosing the adaptive cutoff.

    Returns
    -------
    sparc_value : float
        The spectral arc length.  Values closer to 0 indicate smoother
        movement; more negative values indicate jerkier movement.
    freq : ndarray
        Frequency array (Hz) of the spectrum used.
    magnitude : ndarray
        Normalized magnitude spectrum.
    """
    n = len(speed)
    nfft = int(2 ** (np.ceil(np.log2(n)) + padlevel))

    # FFT and normalised magnitude spectrum
    freq_full = np.fft.rfftfreq(nfft, d=1.0 / fs)
    spec = np.abs(np.fft.rfft(speed, n=nfft))
    spec_norm = spec / spec.max() if spec.max() > 0 else spec

    # Adaptive frequency cutoff: highest freq where amplitude > amp_th, capped at fc
    above_th = np.where((freq_full <= fc) & (spec_norm >= amp_th))[0]
    if len(above_th) == 0:
        fc_idx = 1
    else:
        fc_idx = above_th[-1] + 1      # include that bin

    freq = freq_full[:fc_idx]
    magnitude = spec_norm[:fc_idx]

    # Arc length of the normalised magnitude spectrum
    d_freq = np.diff(freq) / (freq[-1] - freq[0]) if (freq[-1] - freq[0]) > 0 else np.diff(freq)
    d_mag = np.diff(magnitude)
    arc_lengths = np.sqrt(d_freq ** 2 + d_mag ** 2)
    sparc_value = -float(np.sum(arc_lengths))

    return sparc_value, freq, magnitude


# ---------------------------------------------------------------------------
# 2. Stability / variability metrics derived from gyroscope data
# ---------------------------------------------------------------------------

@dataclass
class StabilityMetrics:
    """Container for movement-stability quantifiers."""
    sparc: float                # spectral arc length (smoothness)
    rms_speed: float            # RMS of angular speed (overall intensity)
    peak_speed: float           # peak angular speed
    jerk_rms: float             # RMS of jerk (derivative of acceleration)
    coeff_of_variation: float   # CV of angular speed (consistency)
    dominant_freq_hz: float     # dominant frequency from PSD
    spectral_entropy: float     # entropy of PSD (regularity)
    heart_rate_mean: float      # average HR during the window
    heart_rate_std: float       # HR variability during the window


def angular_speed(gyro_x: np.ndarray, gyro_y: np.ndarray, gyro_z: np.ndarray) -> np.ndarray:
    """Euclidean norm of 3-axis gyroscope → angular speed (rad/s)."""
    return np.sqrt(gyro_x ** 2 + gyro_y ** 2 + gyro_z ** 2)


def rms(signal: np.ndarray) -> float:
    return float(np.sqrt(np.mean(signal ** 2)))


def jerk_metric(speed: np.ndarray, fs: float) -> float:
    """RMS of the second derivative of speed (≈ jerk of angular motion)."""
    accel = np.gradient(speed, 1.0 / fs)
    jerk = np.gradient(accel, 1.0 / fs)
    return rms(jerk)


def spectral_entropy(speed: np.ndarray, fs: float) -> float:
    """
    Normalised spectral entropy of the speed signal.
    Low entropy → movement dominated by a single frequency (rhythmic).
    High entropy → energy spread across many frequencies (erratic).
    """
    freqs, psd = welch(speed, fs=fs, nperseg=min(len(speed), 256))
    psd_norm = psd / psd.sum() if psd.sum() > 0 else psd + 1e-12
    psd_norm = psd_norm[psd_norm > 0]
    ent = -np.sum(psd_norm * np.log2(psd_norm))
    max_ent = np.log2(len(psd_norm)) if len(psd_norm) > 1 else 1.0
    return float(ent / max_ent)


def dominant_frequency(speed: np.ndarray, fs: float) -> float:
    """Frequency (Hz) with the highest PSD component."""
    freqs, psd = welch(speed, fs=fs, nperseg=min(len(speed), 256))
    return float(freqs[np.argmax(psd)])


def compute_stability(df: pd.DataFrame, fs: float = 25.0) -> StabilityMetrics:
    """
    Compute a full suite of stability / fluidity metrics from a session DataFrame.

    Parameters
    ----------
    df : DataFrame with columns heart_rate, gyro_x, gyro_y, gyro_z
    fs : Assumed sampling frequency (Hz).  Adjust to match your actual
         watch sensor rate (Wear OS typically reports 25-50 Hz for gyro).
    """
    speed = angular_speed(df["gyro_x"].values,
                          df["gyro_y"].values,
                          df["gyro_z"].values)

    sparc_val, _, _ = sparc(speed, fs)

    cv = float(speed.std() / speed.mean()) if speed.mean() > 0 else 0.0

    return StabilityMetrics(
        sparc=sparc_val,
        rms_speed=rms(speed),
        peak_speed=float(speed.max()),
        jerk_rms=jerk_metric(speed, fs),
        coeff_of_variation=cv,
        dominant_freq_hz=dominant_frequency(speed, fs),
        spectral_entropy=spectral_entropy(speed, fs),
        heart_rate_mean=float(df["heart_rate"].mean()),
        heart_rate_std=float(df["heart_rate"].std()),
    )


# ---------------------------------------------------------------------------
# 3. Windowed analysis – track how fluidity changes over time
# ---------------------------------------------------------------------------

def windowed_sparc(df: pd.DataFrame, fs: float = 25.0,
                   window_sec: float = 5.0, overlap: float = 0.5):
    """
    Compute SPARC in sliding windows over the session.

    Returns a DataFrame with columns: window_start_s, window_end_s, sparc.
    """
    speed = angular_speed(df["gyro_x"].values,
                          df["gyro_y"].values,
                          df["gyro_z"].values)
    win_samples = int(window_sec * fs)
    step = int(win_samples * (1 - overlap))
    results = []

    for start in range(0, len(speed) - win_samples + 1, step):
        seg = speed[start: start + win_samples]
        val, _, _ = sparc(seg, fs)
        t0 = start / fs
        t1 = (start + win_samples) / fs
        results.append({"window_start_s": t0, "window_end_s": t1, "sparc": val})

    return pd.DataFrame(results)


# ---------------------------------------------------------------------------
# 4. Interpretation helpers
# ---------------------------------------------------------------------------

def interpret_sparc(value: float) -> str:
    """
    Qualitative interpretation of a SPARC value.
    Reference ranges (approximate, task-dependent):
      -1.0 …  0.0  →  very smooth / fluid
      -2.0 … -1.0  →  moderately smooth
      -4.0 … -2.0  →  somewhat jerky
         < -4.0     →  very jerky / fragmented
    """
    if value > -1.0:
        return "very smooth"
    elif value > -2.0:
        return "moderately smooth"
    elif value > -4.0:
        return "somewhat jerky"
    else:
        return "very jerky / fragmented"


# ---------------------------------------------------------------------------
# 5. Visualisation
# ---------------------------------------------------------------------------

def plot_session(df: pd.DataFrame, metrics: StabilityMetrics,
                 windowed: pd.DataFrame, fs: float = 25.0):
    """Four-panel figure summarising the session."""
    speed = angular_speed(df["gyro_x"].values,
                          df["gyro_y"].values,
                          df["gyro_z"].values)
    time = np.arange(len(speed)) / fs

    fig, axes = plt.subplots(2, 2, figsize=(14, 8))
    fig.suptitle("SPARC Movement Fluidity Analysis", fontsize=14, fontweight="bold")

    # (a) Raw gyro traces
    ax = axes[0, 0]
    ax.plot(time, df["gyro_x"].values, label="X", alpha=0.7)
    ax.plot(time, df["gyro_y"].values, label="Y", alpha=0.7)
    ax.plot(time, df["gyro_z"].values, label="Z", alpha=0.7)
    ax.set_xlabel("Time (s)")
    ax.set_ylabel("Angular velocity (rad/s)")
    ax.set_title("Gyroscope signals")
    ax.legend(loc="upper right", fontsize=8)

    # (b) Angular speed + SPARC annotation
    ax = axes[0, 1]
    ax.plot(time, speed, color="steelblue")
    ax.set_xlabel("Time (s)")
    ax.set_ylabel("Angular speed (rad/s)")
    ax.set_title(f"Angular speed  |  SPARC = {metrics.sparc:.3f} ({interpret_sparc(metrics.sparc)})")

    # (c) Windowed SPARC over time
    ax = axes[1, 0]
    mid = (windowed["window_start_s"] + windowed["window_end_s"]) / 2
    ax.plot(mid, windowed["sparc"], marker="o", markersize=4, color="darkorange")
    ax.axhline(metrics.sparc, ls="--", color="grey", label=f"Session avg {metrics.sparc:.2f}")
    ax.set_xlabel("Time (s)")
    ax.set_ylabel("SPARC")
    ax.set_title("Windowed SPARC (fluidity over time)")
    ax.legend(fontsize=8)

    # (d) Normalised spectrum used by SPARC
    _, freq, mag = sparc(speed, fs)
    ax = axes[1, 1]
    ax.plot(freq, mag, color="mediumseagreen")
    ax.fill_between(freq, mag, alpha=0.25, color="mediumseagreen")
    ax.set_xlabel("Frequency (Hz)")
    ax.set_ylabel("Normalised magnitude")
    ax.set_title("Fourier magnitude spectrum (SPARC input)")

    plt.tight_layout(rect=[0, 0, 1, 0.95])
    plt.savefig("sparc_report.png", dpi=150)
    plt.show()


# ---------------------------------------------------------------------------
# 6. Main entry point
# ---------------------------------------------------------------------------

def main():
    # --- Load data ---
    df = pd.read_csv("sessionData.csv")
    print(f"Loaded {len(df)} samples  |  columns: {list(df.columns)}")

    # Sampling frequency: adjust to your Wear OS sensor rate.
    # If the exact rate is unknown, 25 Hz is a common default for
    # SENSOR_DELAY_UI on Wear OS gyroscopes.
    FS = 25.0

    # --- Session-level metrics ---
    metrics = compute_stability(df, fs=FS)

    print("\n========== Movement Stability Report ==========")
    print(f"  SPARC (smoothness)      : {metrics.sparc:.4f}  → {interpret_sparc(metrics.sparc)}")
    print(f"  RMS angular speed       : {metrics.rms_speed:.4f} rad/s")
    print(f"  Peak angular speed      : {metrics.peak_speed:.4f} rad/s")
    print(f"  Jerk (RMS)              : {metrics.jerk_rms:.4f} rad/s³")
    print(f"  Coeff. of variation     : {metrics.coeff_of_variation:.4f}")
    print(f"  Dominant frequency      : {metrics.dominant_freq_hz:.2f} Hz")
    print(f"  Spectral entropy        : {metrics.spectral_entropy:.4f}  (0=rhythmic, 1=erratic)")
    print(f"  Heart rate (mean ± std) : {metrics.heart_rate_mean:.1f} ± {metrics.heart_rate_std:.1f} bpm")
    print("================================================\n")

    # --- Windowed analysis ---
    windowed = windowed_sparc(df, fs=FS, window_sec=3.0, overlap=0.5)
    if not windowed.empty:
        print("Windowed SPARC summary:")
        print(f"  Best  (smoothest window) : {windowed['sparc'].max():.4f}")
        print(f"  Worst (jerkiest window)  : {windowed['sparc'].min():.4f}")
        print(f"  Std. deviation           : {windowed['sparc'].std():.4f}")
        print()

    # --- Plot ---
    plot_session(df, metrics, windowed, fs=FS)


if __name__ == "__main__":
    main()
