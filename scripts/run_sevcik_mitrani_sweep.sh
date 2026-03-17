#!/usr/bin/env bash
set -euo pipefail

ROOT="$(cd "$(dirname "${BASH_SOURCE[0]}")/.." && pwd)"
cd "$ROOT"

BASE_CONFIG="${1:-configs/experiment.properties}"
OUTDIR="${2:-out/sevcik_mitrani}"
USER_RANGE="${3:-1:20}"
REPS="${4:-5}"
BASE_SEED="${5:-12345}"
TRACE_ENABLED="${6:-false}"
TRACE_LEVEL="${7:-SUMMARY}"
TRACE_MAX_LINES="${8:-0}"

if [[ ! -f "$BASE_CONFIG" ]]; then
  echo "Error: config not found: $BASE_CONFIG" >&2
  exit 2
fi

if [[ "$REPS" -le 0 ]]; then
  echo "Error: replications must be > 0" >&2
  exit 2
fi

mkdir -p "$OUTDIR"
TMP_CONFIG="$(mktemp /tmp/sevcik_mitrani_XXXX.properties)"
cp "$BASE_CONFIG" "$TMP_CONFIG"

upsert_prop() {
  local key="$1"
  local val="$2"
  if grep -q "^${key}=" "$TMP_CONFIG"; then
    sed -i "s|^${key}=.*|${key}=${val}|" "$TMP_CONFIG"
  else
    printf '\n%s=%s\n' "$key" "$val" >> "$TMP_CONFIG"
  fi
}

upsert_prop "experiment.userCounts" "$USER_RANGE"
upsert_prop "experiment.replications" "$REPS"
upsert_prop "experiment.baseSeed" "$BASE_SEED"
upsert_prop "trace.enabled" "$TRACE_ENABLED"
upsert_prop "trace.level" "$TRACE_LEVEL"
upsert_prop "trace.maxLines" "$TRACE_MAX_LINES"

SUMMARY_CSV="$OUTDIR/summary.csv"
REP_CSV="$OUTDIR/replications.csv"
ALL_CSV="$OUTDIR/all_metrics_detailed.csv"
RUN_LOG="$OUTDIR/run.log"

./scripts/compile.sh

{
  /usr/bin/time -f 'ELAPSED_SEC=%e' \
    java -cp build/classes des.Main --mode experiment --config "$TMP_CONFIG" --out "$SUMMARY_CSV"
} 2>&1 | tee "$RUN_LOG"

if [[ ! -f "$SUMMARY_CSV" ]]; then
  echo "Error: summary file not generated: $SUMMARY_CSV" >&2
  exit 3
fi

if [[ ! -f "$REP_CSV" ]]; then
  echo "Error: replications file not generated: $REP_CSV" >&2
  exit 4
fi

python3 - "$REP_CSV" "$SUMMARY_CSV" "$ALL_CSV" <<'PY'
import csv
import sys
from pathlib import Path

rep_path = Path(sys.argv[1])
sum_path = Path(sys.argv[2])
out_path = Path(sys.argv[3])

with rep_path.open(newline="") as f:
    rep_rows = list(csv.DictReader(f))

with sum_path.open(newline="") as f:
    sum_rows = list(csv.DictReader(f))

headers = [
    "row_type",
    "users",
    "replications",
    "replication",
    "seed",
    "measure_ms",
    "issued",
    "good_completed",
    "bad_completed",
    "timed_out",
    "dropped",
    "good_resp_count",
    "good_resp_mean_ms",
    "good_resp_ci_low_ms",
    "good_resp_ci_high_ms",
    "good_resp_ci_n",
    "client_resp_count",
    "client_resp_mean_ms",
    "goodput_rps",
    "badput_rps",
    "throughput_rps",
    "timeout_rps",
    "drop_rps",
    "drop_rate",
    "avg_wait_q",
    "max_wait_q",
    "avg_core_util",
    "notes",
]

with out_path.open("w", newline="") as f:
    w = csv.DictWriter(f, fieldnames=headers)
    w.writeheader()

    for r in rep_rows:
        w.writerow(
            {
                "row_type": "run",
                "users": r.get("users", ""),
                "replications": "",
                "replication": r.get("replication", ""),
                "seed": r.get("seed", ""),
                "measure_ms": r.get("measure_ms", ""),
                "issued": r.get("issued", ""),
                "good_completed": r.get("good_completed", ""),
                "bad_completed": r.get("bad_completed", ""),
                "timed_out": r.get("timed_out", ""),
                "dropped": r.get("dropped", ""),
                "good_resp_count": r.get("good_resp_count", ""),
                "good_resp_mean_ms": r.get("good_resp_mean_ms", ""),
                "good_resp_ci_low_ms": "",
                "good_resp_ci_high_ms": "",
                "good_resp_ci_n": "",
                "client_resp_count": r.get("client_resp_count", ""),
                "client_resp_mean_ms": r.get("client_resp_mean_ms", ""),
                "goodput_rps": r.get("goodput_rps", ""),
                "badput_rps": r.get("badput_rps", ""),
                "throughput_rps": r.get("throughput_rps", ""),
                "timeout_rps": r.get("timeout_rps", ""),
                "drop_rps": r.get("drop_rps", ""),
                "drop_rate": r.get("drop_rate", ""),
                "avg_wait_q": r.get("avg_wait_q", ""),
                "max_wait_q": r.get("max_wait_q", ""),
                "avg_core_util": r.get("avg_core_util", ""),
                "notes": "per replication run",
            }
        )

    for s in sum_rows:
        w.writerow(
            {
                "row_type": "avg",
                "users": s.get("users", ""),
                "replications": s.get("replications", ""),
                "replication": "",
                "seed": "",
                "measure_ms": s.get("measure_ms", ""),
                "issued": "",
                "good_completed": "",
                "bad_completed": "",
                "timed_out": "",
                "dropped": "",
                "good_resp_count": "",
                "good_resp_mean_ms": s.get("good_resp_mean_ms", ""),
                "good_resp_ci_low_ms": s.get("good_resp_ci_low_ms", ""),
                "good_resp_ci_high_ms": s.get("good_resp_ci_high_ms", ""),
                "good_resp_ci_n": s.get("good_resp_ci_n", ""),
                "client_resp_count": "",
                "client_resp_mean_ms": "",
                "goodput_rps": s.get("goodput_rps_mean", ""),
                "badput_rps": s.get("badput_rps_mean", ""),
                "throughput_rps": s.get("throughput_rps_mean", ""),
                "timeout_rps": s.get("timeout_rps_mean", ""),
                "drop_rps": s.get("drop_rps_mean", ""),
                "drop_rate": s.get("drop_rate_mean", ""),
                "avg_wait_q": s.get("avg_wait_q_mean", ""),
                "max_wait_q": s.get("max_wait_q_mean", ""),
                "avg_core_util": s.get("avg_core_util_mean", ""),
                "notes": "mean across replications",
            }
        )

print(f"Wrote {out_path}")
PY

printf '\nSweep complete.\n'
printf 'Config used: %s\n' "$TMP_CONFIG"
printf 'Summary CSV: %s\n' "$SUMMARY_CSV"
printf 'Replications CSV: %s\n' "$REP_CSV"
printf 'All-in-one CSV: %s\n' "$ALL_CSV"
printf 'Run log (includes elapsed time): %s\n' "$RUN_LOG"
