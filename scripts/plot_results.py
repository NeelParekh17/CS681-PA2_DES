#!/usr/bin/env python3
from __future__ import annotations

import argparse
import csv
import math
import os
from pathlib import Path

import matplotlib.pyplot as plt


def read_summary(path: Path) -> list[dict[str, str]]:
    with path.open(newline="") as f:
        return list(csv.DictReader(f))


def to_float(rows: list[dict[str, str]], key: str) -> list[float]:
    out: list[float] = []
    for r in rows:
        v = r.get(key, "")
        out.append(float(v) if v not in ("", "NaN", "nan") else float("nan"))
    return out


def save_response_time_plot(rows: list[dict[str, str]], outdir: Path) -> None:
    users = to_float(rows, "users")
    mean = to_float(rows, "good_resp_mean_ms")
    lo = to_float(rows, "good_resp_ci_low_ms")
    hi = to_float(rows, "good_resp_ci_high_ms")
    yerr = [
        [m - l for m, l in zip(mean, lo)],
        [h - m for h, m in zip(hi, mean)],
    ]

    plt.figure()
    plt.errorbar(users, mean, yerr=yerr, fmt="-o", capsize=3)
    plt.xlabel("Users")
    plt.ylabel("Avg response time (good) [ms]")
    plt.title("Response Time vs Users (95% CI)")
    plt.grid(True, alpha=0.3)
    plt.tight_layout()
    plt.savefig(outdir / "response_time_vs_users.png", dpi=150)


def save_throughput_plot(rows: list[dict[str, str]], outdir: Path) -> None:
    users = to_float(rows, "users")
    goodput = to_float(rows, "goodput_rps_mean")
    badput = to_float(rows, "badput_rps_mean")
    thr = to_float(rows, "throughput_rps_mean")

    plt.figure()
    plt.plot(users, thr, "-o", label="throughput")
    plt.plot(users, goodput, "-o", label="goodput")
    plt.plot(users, badput, "-o", label="badput")
    plt.xlabel("Users")
    plt.ylabel("Requests/sec")
    plt.title("Throughput / Goodput / Badput vs Users")
    plt.grid(True, alpha=0.3)
    plt.legend()
    plt.tight_layout()
    plt.savefig(outdir / "throughput_goodput_badput_vs_users.png", dpi=150)


def save_timeout_plot(rows: list[dict[str, str]], outdir: Path) -> None:
    users = to_float(rows, "users")
    timeout_rps = to_float(rows, "timeout_rps_mean")

    plt.figure()
    plt.plot(users, timeout_rps, "-o")
    plt.xlabel("Users")
    plt.ylabel("Timeouts/sec")
    plt.title("Timeout (Drop) Rate vs Users")
    plt.grid(True, alpha=0.3)
    plt.tight_layout()
    plt.savefig(outdir / "timeout_rate_vs_users.png", dpi=150)


def save_util_plot(rows: list[dict[str, str]], outdir: Path) -> None:
    users = to_float(rows, "users")
    util = to_float(rows, "avg_core_util_mean")

    plt.figure()
    plt.plot(users, util, "-o")
    plt.xlabel("Users")
    plt.ylabel("Avg core utilization")
    plt.title("Average Core Utilization vs Users")
    plt.ylim(0.0, 1.05)
    plt.grid(True, alpha=0.3)
    plt.tight_layout()
    plt.savefig(outdir / "core_util_vs_users.png", dpi=150)


def moving_average(values: list[float], window: int) -> list[float]:
    if window <= 1:
        return list(values)

    half = window // 2
    out: list[float] = []
    for i in range(len(values)):
        lo = max(0, i - half)
        hi = min(len(values), i + half + 1)
        chunk = [v for v in values[lo:hi] if math.isfinite(v)]
        out.append(sum(chunk) / len(chunk) if chunk else float("nan"))
    return out


def cumulative_weighted_mean(values: list[float], weights: list[float]) -> list[float]:
    out: list[float] = []
    running_weight = 0.0
    running_sum = 0.0
    for v, w in zip(values, weights):
        if math.isfinite(v) and math.isfinite(w) and w > 0.0:
            running_sum += v * w
            running_weight += w
        out.append((running_sum / running_weight) if running_weight > 0.0 else float("nan"))
    return out


def cumulative_count(counts: list[float]) -> list[float]:
    out: list[float] = []
    running = 0.0
    for c in counts:
        if math.isfinite(c) and c > 0.0:
            running += c
        out.append(running)
    return out


def suggest_warmup_cutoff(
    bin_end_ms: list[float],
    smooth_mean_ms: list[float],
    stability_pct: float,
    stability_bins: int,
) -> tuple[int | None, float]:
    finite = [x for x in smooth_mean_ms if math.isfinite(x)]
    if not finite:
        return None, float("nan")

    tail_n = max(5, int(len(finite) * 0.2))
    tail_mean = sum(finite[-tail_n:]) / tail_n
    if not math.isfinite(tail_mean) or tail_mean <= 0.0:
        return None, tail_mean

    threshold = abs(tail_mean) * (stability_pct / 100.0)
    for i in range(0, max(0, len(smooth_mean_ms) - stability_bins + 1)):
        window = smooth_mean_ms[i : i + stability_bins]
        if not all(math.isfinite(v) for v in window):
            continue
        if all(abs(v - tail_mean) <= threshold for v in window):
            return i, tail_mean

    return None, tail_mean


def write_welch_analysis_csv(
    outdir: Path,
    bin_end_ms: list[float],
    cum_completed: list[float],
    mean_resp_ms: list[float],
    smooth_resp_ms: list[float],
    cum_weighted_mean_ms: list[float],
    count: list[float],
) -> None:
    out_path = outdir / "welch_analysis_summary.csv"
    with out_path.open("w", newline="") as f:
        w = csv.writer(f)
        w.writerow(
            [
                "bin_end_ms",
                "cum_completed",
                "mean_resp_ms",
                "smooth_resp_ms",
                "cum_weighted_mean_ms",
                "count",
            ]
        )
        for t, cc, m, s, cwm, c in zip(bin_end_ms, cum_completed, mean_resp_ms, smooth_resp_ms, cum_weighted_mean_ms, count):
            w.writerow([f"{t:.3f}", int(cc), f"{m:.6f}", f"{s:.6f}", f"{cwm:.6f}", int(c)])


def write_welch_recommendation(
    outdir: Path,
    bin_end_ms: list[float],
    cum_completed: list[float],
    cutoff_idx: int | None,
    tail_mean: float,
    stability_pct: float,
    stability_bins: int,
) -> None:
    out_path = outdir / "welch_recommendation.txt"
    with out_path.open("w") as f:
        f.write("Welch Warm-up Recommendation\n")
        f.write("===========================\n")
        f.write(f"Stability rule: within +/-{stability_pct:.1f}% of tail mean for {stability_bins} consecutive bins.\n")
        if math.isfinite(tail_mean):
            f.write(f"Tail mean (smoothed response): {tail_mean:.3f} ms\n")
        else:
            f.write("Tail mean (smoothed response): NaN\n")

        if cutoff_idx is None:
            f.write("Suggested warm-up cutoff: not found with current rule.\n")
            f.write("Try increasing welch.duration, bin size, or smoothing window.\n")
            return

        cutoff_ms = bin_end_ms[cutoff_idx]
        cutoff_completed = cum_completed[cutoff_idx]
        f.write(f"Suggested warm-up cutoff: {cutoff_ms:.3f} ms ({cutoff_ms / 1000.0:.3f} s)\n")
        f.write(f"Suggested warm-up cutoff (completed requests): {int(cutoff_completed)}\n")


def choose_welch_xmax(
    x: list[float],
    cutoff_idx: int | None,
    auto_zoom: bool,
    auto_zoom_factor: float,
    explicit_xmax: float | None,
    x_axis: str,
    completed_hard_cap: float,
) -> float | None:
    if explicit_xmax is not None:
        return explicit_xmax

    finite_x = [v for v in x if math.isfinite(v)]
    if not finite_x:
        return None

    max_x = max(finite_x)
    if max_x <= 0.0:
        return None

    limit: float | None = None

    if auto_zoom:
        # Keep enough post-cutoff context while avoiding overly wide plots.
        if cutoff_idx is not None and 0 <= cutoff_idx < len(x):
            cutoff_x = x[cutoff_idx]
            if math.isfinite(cutoff_x) and cutoff_x > 0.0:
                min_window = max_x * 0.10
                suggested = max(min_window, cutoff_x * auto_zoom_factor)
                limit = min(max_x, suggested)

        # Fallback when no cutoff is found: show early quarter where transient usually occurs.
        if limit is None:
            limit = max_x * 0.25

    # For completed-request axis, keep plots readable for low/medium user ranges.
    # Set to <= 0 to disable this cap.
    if x_axis == "completed" and completed_hard_cap > 0.0:
        cap = min(max_x, completed_hard_cap)
        limit = cap if limit is None else min(limit, cap)

    return limit


def save_welch_plot(
    rows: list[dict[str, str]],
    outdir: Path,
    window: int,
    stability_pct: float,
    stability_bins: int,
    x_axis: str,
    x_max: float | None,
    auto_zoom: bool,
    auto_zoom_factor: float,
    completed_hard_cap: float,
) -> None:
    bin_end_ms = to_float(rows, "bin_end_ms")
    mean_resp_ms = to_float(rows, "mean_resp_ms")
    count = to_float(rows, "count")
    ci_low_ms = to_float(rows, "ci_low_ms")
    ci_high_ms = to_float(rows, "ci_high_ms")
    cum_completed = cumulative_count(count)
    smooth_resp_ms = moving_average(mean_resp_ms, window)
    cum_weighted_mean_ms = cumulative_weighted_mean(mean_resp_ms, count)

    if x_axis == "completed":
        x = cum_completed
        x_label = "Cumulative Completed Requests"
        main_plot_name = "welch_mean_response_vs_completed.png"
        cum_plot_name = "welch_cumulative_mean_vs_completed.png"
    else:
        x = bin_end_ms
        x_label = "Bin End Time [ms]"
        main_plot_name = "welch_mean_response_vs_time.png"
        cum_plot_name = "welch_cumulative_mean_vs_time.png"

    cutoff_idx, tail_mean = suggest_warmup_cutoff(bin_end_ms, smooth_resp_ms, stability_pct, stability_bins)
    x_limit = choose_welch_xmax(
        x,
        cutoff_idx,
        auto_zoom,
        auto_zoom_factor,
        x_max,
        x_axis,
        completed_hard_cap,
    )

    fig, ax1 = plt.subplots(figsize=(10, 5))
    ax1.plot(x, mean_resp_ms, "-o", alpha=0.35, markersize=3, label="raw mean")
    has_ci = any(math.isfinite(v) for v in ci_low_ms) and any(math.isfinite(v) for v in ci_high_ms)
    if has_ci:
        ax1.fill_between(x, ci_low_ms, ci_high_ms, alpha=0.15, color="tab:blue", label="95% CI")
    ax1.plot(x, smooth_resp_ms, "-", linewidth=2, color="tab:blue", label=f"smoothed (w={window})")
    ax1.plot(x, cum_weighted_mean_ms, "-", linewidth=1.8, color="tab:green", label="cumulative mean")
    ax1.set_xlabel(x_label)
    ax1.set_ylabel("Response Time [ms]")
    ax1.set_title("Welch Warm-up Analysis")
    ax1.grid(True, alpha=0.3)
    if x_limit is not None:
        ax1.set_xlim(left=0.0, right=x_limit)
    else:
        ax1.set_xlim(left=0.0)

    if cutoff_idx is not None:
        cutoff_ms = bin_end_ms[cutoff_idx]
        cutoff_x = x[cutoff_idx]
        cutoff_label = f"cutoff {int(cutoff_x)} req" if x_axis == "completed" else f"cutoff {cutoff_ms/1000.0:.2f}s"
        ax1.axvline(cutoff_x, color="tab:red", linestyle="--", linewidth=1.5, label=cutoff_label)
    ax1.legend(loc="upper left")

    # Overlay the number of completions per bin to help judge data stability.
    ax2 = ax1.twinx()
    ax2.plot(x, count, "--", alpha=0.5, color="tab:gray", label="count")
    ax2.set_ylabel("Completions per Bin")

    fig.tight_layout()
    fig.savefig(outdir / main_plot_name, dpi=150)

    plt.figure(figsize=(10, 4.5))
    plt.plot(x, smooth_resp_ms, "-", color="tab:blue", linewidth=1.8, label=f"smoothed mean (w={window})")
    plt.plot(x, cum_weighted_mean_ms, "-", color="tab:green", linewidth=2.0, label="cumulative mean")
    plt.xlabel(x_label)
    plt.ylabel("Cumulative Weighted Mean [ms]")
    plt.title("Welch Cumulative Mean")
    plt.grid(True, alpha=0.3)
    plt.legend()
    if x_limit is not None:
        plt.xlim(left=0.0, right=x_limit)
    else:
        plt.xlim(left=0.0)
    plt.tight_layout()
    plt.savefig(outdir / cum_plot_name, dpi=150)

    write_welch_analysis_csv(outdir, bin_end_ms, cum_completed, mean_resp_ms, smooth_resp_ms, cum_weighted_mean_ms, count)
    write_welch_recommendation(outdir, bin_end_ms, cum_completed, cutoff_idx, tail_mean, stability_pct, stability_bins)


def default_outdir(input_csv: Path, welch_mode: bool) -> Path:
    suffix = "plots_welch" if welch_mode else "plots_exp"
    return input_csv.parent / suffix


def main() -> int:
    ap = argparse.ArgumentParser(
        description=(
            "Generate experiment plots from summary CSV (default mode), "
            "or Welch plots with --welch."
        )
    )
    ap.add_argument(
        "--welch",
        action="store_true",
        help="Treat input CSV as Welch output (bin_end_ms, mean_resp_ms, count).",
    )
    ap.add_argument(
        "--welch-window",
        type=int,
        default=7,
        help="Smoothing window (bins) used in Welch mode.",
    )
    ap.add_argument(
        "--welch-stability-pct",
        type=float,
        default=5.0,
        help="Stability threshold in percent for warm-up cutoff recommendation.",
    )
    ap.add_argument(
        "--welch-stability-bins",
        type=int,
        default=8,
        help="Required consecutive stable bins for warm-up cutoff recommendation.",
    )
    ap.add_argument(
        "--welch-x-axis",
        choices=["time", "completed"],
        default="time",
        help="X-axis for Welch plots: simulation time or cumulative completed requests.",
    )
    ap.add_argument(
        "--welch-x-max",
        type=float,
        default=None,
        help="Optional x-axis upper limit for Welch plots (ms for time mode, requests for completed mode).",
    )
    ap.add_argument(
        "--welch-auto-zoom",
        choices=["on", "off"],
        default="on",
        help=(
            "Automatically zoom x-axis to the transient region based on detected cutoff when --welch-x-max is not set. "
            "Use 'off' to keep full range."
        ),
    )
    ap.add_argument(
        "--welch-auto-zoom-factor",
        type=float,
        default=4.0,
        help="How much of post-cutoff region to include in auto-zoom (x_max ~= cutoff * factor).",
    )
    ap.add_argument(
        "--welch-completed-hard-cap",
        type=float,
        default=30000.0,
        help=(
            "Hard upper bound for x-axis in completed mode when --welch-x-max is not set. "
            "Use <= 0 to disable."
        ),
    )
    ap.add_argument("input_csv", type=Path, help="Input CSV file path.")
    ap.add_argument(
        "outdir",
        type=Path,
        nargs="?",
        help="Output directory. Defaults to <input_csv_dir>/plots_exp or plots_welch.",
    )
    args = ap.parse_args()

    rows = read_summary(args.input_csv)
    if not rows:
        raise SystemExit(f"No rows found in {args.input_csv}")

    outdir = args.outdir if args.outdir is not None else default_outdir(args.input_csv, args.welch)

    os.makedirs(outdir, exist_ok=True)
    if args.welch:
        if args.welch_window < 1:
            raise SystemExit("--welch-window must be >= 1")
        if args.welch_stability_pct <= 0.0:
            raise SystemExit("--welch-stability-pct must be > 0")
        if args.welch_stability_bins < 1:
            raise SystemExit("--welch-stability-bins must be >= 1")
        if args.welch_x_max is not None and args.welch_x_max <= 0.0:
            raise SystemExit("--welch-x-max must be > 0")
        if args.welch_auto_zoom_factor <= 0.0:
            raise SystemExit("--welch-auto-zoom-factor must be > 0")

        save_welch_plot(
            rows,
            outdir,
            args.welch_window,
            args.welch_stability_pct,
            args.welch_stability_bins,
            args.welch_x_axis,
            args.welch_x_max,
            args.welch_auto_zoom == "on",
            args.welch_auto_zoom_factor,
            args.welch_completed_hard_cap,
        )
        print(f"Wrote Welch analysis outputs to {outdir}")
        return 0

    save_response_time_plot(rows, outdir)
    save_throughput_plot(rows, outdir)
    save_timeout_plot(rows, outdir)
    save_util_plot(rows, outdir)
    print(f"Wrote experiment plots to {outdir}")
    return 0


if __name__ == "__main__":
    raise SystemExit(main())

