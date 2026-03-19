#!/usr/bin/env python3
"""
Plot response time vs. number of users with 95% CI error bars.

Expected CSV columns:
    users, good_resp_mean_ms,
    good_resp_ci_low_ms, good_resp_ci_high_ms, good_resp_ci_n

Usage:
    python3 plot_response_time_ci.py --summary path/to/summary.csv
    python3 plot_response_time_ci.py --summary path/to/summary.csv --out my_plot.png --title "My Title"
"""
from __future__ import annotations

import argparse
import csv
import math
from pathlib import Path

import matplotlib.pyplot as plt


# ---------------------------------------------------------------------------
# Helpers
# ---------------------------------------------------------------------------

def _to_float(value: str | None) -> float:
    """Convert a CSV cell to float; return NaN for blanks / sentinels."""
    v = (value or "").strip()
    if v in ("", "NaN", "nan", "NA", "N/A", "inf", "-inf"):
        return float("nan")
    try:
        return float(v)
    except ValueError:
        return float("nan")


def _to_int(value: str | None) -> int:
    """Convert a CSV cell to int; return 0 on failure."""
    try:
        return int(float(value or "0"))
    except (ValueError, TypeError):
        return 0


# ---------------------------------------------------------------------------
# Data loading
# ---------------------------------------------------------------------------

REQUIRED_COLS = {
    "users",
    "good_resp_mean_ms",
    "good_resp_ci_low_ms",
    "good_resp_ci_high_ms",
    "good_resp_ci_n",
}


def load_data(path: Path) -> tuple[
    list[float],   # users
    list[float],   # mean_ms
    list[float],   # ci_low_ms
    list[float],   # ci_high_ms
    list[int],     # ci_n
]:
    with path.open(newline="", encoding="utf-8-sig") as fh:
        reader = csv.DictReader(fh)
        if reader.fieldnames is None:
            raise SystemExit(f"CSV appears empty: {path}")

        actual_cols = set(reader.fieldnames)
        missing = REQUIRED_COLS - actual_cols
        if missing:
            raise SystemExit(
                f"CSV is missing required columns: {sorted(missing)}\n"
                f"Found columns: {sorted(actual_cols)}"
            )

        users, mean, ci_low, ci_high, ci_n = [], [], [], [], []
        for i, row in enumerate(reader, start=2):          # row 1 is the header
            u = _to_float(row.get("users"))
            m = _to_float(row.get("good_resp_mean_ms"))
            lo = _to_float(row.get("good_resp_ci_low_ms"))
            hi = _to_float(row.get("good_resp_ci_high_ms"))
            n  = _to_int(row.get("good_resp_ci_n"))

            if not (math.isfinite(u) and math.isfinite(m)):
                print(f"  [skip row {i}] users={row.get('users')!r} or mean={row.get('good_resp_mean_ms')!r} is not finite")
                continue

            users.append(u)
            mean.append(m)
            ci_low.append(lo)
            ci_high.append(hi)
            ci_n.append(n)

    return users, mean, ci_low, ci_high, ci_n


# ---------------------------------------------------------------------------
# Plotting
# ---------------------------------------------------------------------------

def plot(
    users: list[float],
    mean: list[float],
    ci_low: list[float],
    ci_high: list[float],
    ci_n: list[int],
    *,
    title: str,
    out_path: Path,
) -> None:
    # Split into points that have valid CI and those that don't
    ci_u, ci_m, yerr_lo, yerr_hi = [], [], [], []
    no_ci_u, no_ci_m = [], []

    for u, m, lo, hi, n in zip(users, mean, ci_low, ci_high, ci_n):
        if math.isfinite(lo) and math.isfinite(hi) and n >= 2:
            ci_u.append(u)
            ci_m.append(m)
            # clamp to zero so error bars never go negative
            yerr_lo.append(max(0.0, m - lo))
            yerr_hi.append(max(0.0, hi - m))
        else:
            no_ci_u.append(u)
            no_ci_m.append(m)

    print(f"  Points with valid 95% CI : {len(ci_u)}")
    print(f"  Points without CI        : {len(no_ci_u)}")

    fig, ax = plt.subplots(figsize=(10, 6))

    # Plot mean line over ALL points
    ax.plot(users, mean, "-", color="steelblue", linewidth=2.0,
            zorder=3, label="Mean response time")

    # Draw a vertical line from ci_low to ci_high at each user point
    if ci_u:
        ci_low_abs  = [m - lo for m, lo in zip(ci_m, yerr_lo)]
        ci_high_abs = [m + hi for m, hi in zip(ci_m, yerr_hi)]
        ax.vlines(
            ci_u,
            ci_low_abs,
            ci_high_abs,
            colors="crimson",
            linewidth=2.0,
            alpha=0.85,
            zorder=4,
            label="95% CI",
        )
    else:
        print("  WARNING: no points had valid CI bounds (ci_n>=2 and finite lo/hi).")
        print("  Only the mean line will be drawn.")

    # Mark points that lacked CI differently so they are visible
    if no_ci_u:
        ax.scatter(no_ci_u, no_ci_m, color="tomato", zorder=4,
                   s=40, label="Mean (no CI data)")

    ax.set_ylim(bottom=0, top=180)
    ax.set_xlabel("Number of users", fontsize=12)
    ax.set_ylabel("Avg response time — good responses (ms)", fontsize=12)
    ax.set_title(title, fontsize=14)
    ax.grid(True, alpha=0.3)
    ax.legend(fontsize=11)
    fig.tight_layout()
    fig.savefig(out_path, dpi=150)
    print(f"  Saved → {out_path}")


# ---------------------------------------------------------------------------
# Entry point
# ---------------------------------------------------------------------------

def main() -> int:
    ap = argparse.ArgumentParser(
        description=__doc__,
        formatter_class=argparse.RawDescriptionHelpFormatter,
    )
    ap.add_argument("--summary", type=Path, required=True,
                    help="Path to summary.csv")
    ap.add_argument("--out", type=Path, default=None,
                    help="Output image path (default: <summary_dir>/response_time_ci.png)")
    ap.add_argument("--title", type=str,
                    default="Response Time vs Users (95% CI)",
                    help="Plot title")
    args = ap.parse_args()

    summary_path: Path = args.summary.resolve()
    if not summary_path.exists():
        raise SystemExit(f"File not found: {summary_path}")

    out_path: Path = args.out if args.out else summary_path.parent / "response_time_ci.png"
    out_path.parent.mkdir(parents=True, exist_ok=True)

    print(f"Reading  : {summary_path}")
    users, mean, ci_low, ci_high, ci_n = load_data(summary_path)

    if not users:
        raise SystemExit("No plottable rows found after parsing. Check the CSV for valid 'users' and 'good_resp_mean_ms' values.")

    print(f"Total rows loaded: {len(users)}")
    plot(users, mean, ci_low, ci_high, ci_n, title=args.title, out_path=out_path)
    return 0


if __name__ == "__main__":
    raise SystemExit(main())