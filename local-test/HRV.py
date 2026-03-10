import os
import numpy as np
import pandas as pd
import matplotlib.pyplot as plt
import matplotlib.gridspec as gridspec

# ---------------------------------------------------------------------------
# 1. HRV Core Functions
# ---------------------------------------------------------------------------

def bpm_to_rr(bpm_series: np.ndarray) -> np.ndarray:
    """
    Converts a BPM series to RR intervals in milliseconds.
    RR (ms) = 60000 / BPM
    """
    return 60000.0 / bpm_series


def calculate_hrv_metrics(rr_intervals: np.ndarray) -> dict:
    """
    Calculate time-domain HRV metrics from a series of RR intervals (ms).

    Metrics:
      - mean_rr    : Mean RR interval (ms)
      - sdnn       : Standard deviation of RR intervals — overall HRV (ms)
      - rmssd      : Root mean square of successive differences — short-term vagal HRV (ms)
      - pnn50      : % of successive RR differences > 50 ms — vagal index (%)
      - mean_hr    : Mean heart rate (BPM)
      - cv_rr      : Coefficient of variation of RR (SDNN / mean RR * 100) — normalised HRV (%)

    Clinical reference ranges (resting adult):
      RMSSD: 20–80 ms (healthy), < 15 ms (concerning)
      SDNN:  50–100 ms (healthy), < 20 ms (poor)
      pNN50: > 3 % (normal), near 0 % during intense exercise (expected)
    """
    if len(rr_intervals) < 4:
        return {}

    successive_diffs = np.diff(rr_intervals)

    mean_rr = float(np.mean(rr_intervals))
    sdnn = float(np.std(rr_intervals, ddof=1))
    rmssd = float(np.sqrt(np.mean(successive_diffs ** 2)))
    pnn50 = float(np.sum(np.abs(successive_diffs) > 50) / len(successive_diffs) * 100)
    mean_hr = float(60000.0 / mean_rr) if mean_rr > 0 else 0.0
    cv_rr = float(sdnn / mean_rr * 100) if mean_rr > 0 else 0.0

    return {
        "mean_rr": round(mean_rr, 2),
        "sdnn": round(sdnn, 2),
        "rmssd": round(rmssd, 2),
        "pnn50": round(pnn50, 2),
        "mean_hr": round(mean_hr, 1),
        "cv_rr": round(cv_rr, 2),
    }


def interpret_rmssd(rmssd: float) -> str:
    """
    Qualitative interpretation of RMSSD during/after exercise.
    Note: During exercise, lower RMSSD is expected. These thresholds are
    more relevant for rest periods or post-exercise recovery windows.
    """
    if rmssd >= 50:
        return "Excellent autonomic recovery / low stress"
    elif rmssd >= 30:
        return "Good HRV — moderate autonomic tone"
    elif rmssd >= 15:
        return "Reduced HRV — moderate stress / active exercise"
    elif rmssd >= 8:
        return "Low HRV — significant stress or intense exertion"
    else:
        return "Very low HRV — high physiological stress / possible arrhythmia"


def rolling_rmssd(rr_intervals: np.ndarray, window: int = 10) -> np.ndarray:
    """
    Computes rolling RMSSD over a sliding window of RR intervals.
    Useful for tracking HRV dynamics during the session.
    """
    result = np.full(len(rr_intervals), np.nan)
    for i in range(window, len(rr_intervals) + 1):
        segment = rr_intervals[i - window:i]
        diffs = np.diff(segment)
        if len(diffs) > 0:
            result[i - 1] = np.sqrt(np.mean(diffs ** 2))
    return result


def detect_stress_events(rr_intervals: np.ndarray, timestamps_ms: np.ndarray,
                          rmssd_threshold: float = 15.0, window: int = 10) -> list:
    """
    Detects windows where rolling RMSSD drops below rmssd_threshold,
    flagging potential stress / autonomic overload events.
    Returns list of (start_time_s, end_time_s, min_rmssd) tuples.
    """
    rolling = rolling_rmssd(rr_intervals, window)
    in_event = False
    events = []
    event_start = None
    event_min = float("inf")

    for i, val in enumerate(rolling):
        if np.isnan(val):
            continue
        t = timestamps_ms[i] / 1000.0
        if val < rmssd_threshold and not in_event:
            in_event = True
            event_start = t
            event_min = val
        elif val < rmssd_threshold and in_event:
            event_min = min(event_min, val)
        elif val >= rmssd_threshold and in_event:
            in_event = False
            events.append((round(event_start, 1), round(t, 1), round(event_min, 2)))
            event_min = float("inf")

    if in_event:
        events.append((round(event_start, 1), round(timestamps_ms[-1] / 1000.0, 1), round(event_min, 2)))

    return events


# ---------------------------------------------------------------------------
# 2. Main Analysis
# ---------------------------------------------------------------------------

def main():
    SCRIPT_DIR = os.path.dirname(os.path.abspath(__file__))
    CSV_PATH = os.path.join(SCRIPT_DIR, "sessionData.csv")

    if not os.path.exists(CSV_PATH):
        print(f"Error: {CSV_PATH} not found.")
        return

    try:
        df = pd.read_csv(CSV_PATH)
        print(f"Loaded {len(df)} samples from {CSV_PATH}")
    except Exception as e:
        print(f"Error reading CSV: {e}")
        return

    # --- Validate required columns ---
    if "heart_rate" not in df.columns:
        print("Error: CSV must have a 'heart_rate' column.")
        print(f"  Found columns: {list(df.columns)}")
        return

    # Synthesize a timestamp column if absent (assume 20 ms sample interval = 50 Hz)
    if "timestamp" not in df.columns:
        print("Warning: 'timestamp' column not found — synthesizing from row index at 50 Hz (20 ms steps).")
        df["timestamp"] = np.arange(len(df)) * 20

    # --- Filter valid heart rate readings ---
    df_hr = df[df["heart_rate"].notna() & (df["heart_rate"] > 0)].copy()
    print(f"Valid heart rate samples: {len(df_hr)} / {len(df)}")

    if len(df_hr) < 10:
        print("Error: Not enough valid heart rate samples for HRV analysis (need >= 10).")
        return

    bpm = df_hr["heart_rate"].values
    timestamps = df_hr["timestamp"].values  # ms

    # Normalise timestamps to start from 0
    timestamps = timestamps - timestamps[0]

    # --- Convert BPM → RR intervals ---
    rr = bpm_to_rr(bpm)

    # --- Global HRV metrics ---
    metrics = calculate_hrv_metrics(rr)
    interpretation = interpret_rmssd(metrics["rmssd"])

    # --- Rolling RMSSD ---
    roll_rmssd = rolling_rmssd(rr, window=10)

    # --- Stress events ---
    STRESS_THRESHOLD = 15.0
    stress_events = detect_stress_events(rr, timestamps, rmssd_threshold=STRESS_THRESHOLD)

    # --- Print results ---
    print("\n" + "=" * 55)
    print("  HRV Analysis Results")
    print("=" * 55)
    print(f"  Mean RR interval   : {metrics['mean_rr']} ms")
    print(f"  Mean HR            : {metrics['mean_hr']} BPM")
    print(f"  SDNN               : {metrics['sdnn']} ms")
    print(f"  RMSSD              : {metrics['rmssd']} ms  →  {interpretation}")
    print(f"  pNN50              : {metrics['pnn50']} %")
    print(f"  CV-RR              : {metrics['cv_rr']} %")
    print("-" * 55)
    if stress_events:
        print(f"  Stress events (RMSSD < {STRESS_THRESHOLD} ms):")
        for start, end, min_r in stress_events:
            print(f"    {start:.1f}s – {end:.1f}s  (min RMSSD: {min_r:.1f} ms)")
    else:
        print(f"  No stress events detected (RMSSD stayed >= {STRESS_THRESHOLD} ms)")
    print("=" * 55)

    # --- Plot ---
    time_axis_s = timestamps / 1000.0

    fig = plt.figure(figsize=(14, 9))
    fig.suptitle("HRV Analysis", fontsize=14, fontweight="bold")
    gs = gridspec.GridSpec(3, 1, hspace=0.45)

    # Panel 1 — BPM over time
    ax1 = fig.add_subplot(gs[0])
    ax1.plot(time_axis_s, bpm, color="#FF5252", linewidth=1.2)
    ax1.set_ylabel("BPM")
    ax1.set_title(f"Heart Rate  (mean: {metrics['mean_hr']} BPM)")
    ax1.grid(True, linestyle="--", alpha=0.5)

    # Panel 2 — RR tachogram
    ax2 = fig.add_subplot(gs[1])
    ax2.plot(time_axis_s, rr, color="#42A5F5", linewidth=1.0, marker=".", markersize=3)
    ax2.axhline(metrics["mean_rr"], color="white", linestyle="--", linewidth=0.8,
                label=f"Mean RR: {metrics['mean_rr']} ms")
    ax2.set_ylabel("RR interval (ms)")
    ax2.set_title("RR Tachogram")
    ax2.legend(fontsize=8)
    ax2.grid(True, linestyle="--", alpha=0.5)

    # Panel 3 — Rolling RMSSD
    ax3 = fig.add_subplot(gs[2])
    ax3.plot(time_axis_s, roll_rmssd, color="#66BB6A", linewidth=1.4, label="Rolling RMSSD (10-beat window)")
    ax3.axhline(STRESS_THRESHOLD, color="#FF7043", linestyle="--", linewidth=1.0,
                label=f"Stress threshold ({STRESS_THRESHOLD} ms)")
    for start, end, _ in stress_events:
        ax3.axvspan(start, end, color="#FF7043", alpha=0.15)
    ax3.set_ylabel("RMSSD (ms)")
    ax3.set_xlabel("Time (s)")
    ax3.set_title(f"Rolling RMSSD  (overall: {metrics['rmssd']} ms — {interpretation})")
    ax3.legend(fontsize=8)
    ax3.grid(True, linestyle="--", alpha=0.5)

    plt.style.use("dark_background")
    plt.savefig(os.path.join(SCRIPT_DIR, "hrv_output.png"), dpi=120, bbox_inches="tight")
    print("\nPlot saved to hrv_output.png")
    plt.show()


if __name__ == "__main__":
    main()
