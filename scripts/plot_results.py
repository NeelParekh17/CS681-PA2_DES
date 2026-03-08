#!/usr/bin/env python3
from __future__ import annotations

import argparse
import csv
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


def main() -> int:
    ap = argparse.ArgumentParser()
    ap.add_argument("summary_csv", type=Path)
    ap.add_argument("outdir", type=Path)
    args = ap.parse_args()

    rows = read_summary(args.summary_csv)
    if not rows:
        raise SystemExit(f"No rows found in {args.summary_csv}")

    os.makedirs(args.outdir, exist_ok=True)
    save_response_time_plot(rows, args.outdir)
    save_throughput_plot(rows, args.outdir)
    save_timeout_plot(rows, args.outdir)
    save_util_plot(rows, args.outdir)
    print(f"Wrote plots to {args.outdir}")
    return 0


if __name__ == "__main__":
    raise SystemExit(main())

