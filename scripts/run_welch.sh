#!/usr/bin/env bash
set -euo pipefail

ROOT="$(cd "$(dirname "${BASH_SOURCE[0]}")/.." && pwd)"
cd "$ROOT"

CONFIG="${1:-}"
OUT="${2:-out/welch.csv}"

# If the user passes unquoted paths that contain spaces, Bash will split them into
# multiple args. Try to infer where <config> ends and <out> begins.
if [[ $# -gt 2 ]]; then
  args=("$@")
  inferred_config=""
  inferred_out=""

  for ((k=1; k<$#; k++)); do
    config_candidate="${args[*]:0:k}"
    out_candidate="${args[*]:k}"

    if [[ "$config_candidate" == *.properties && -f "$config_candidate" ]]; then
      inferred_config="$config_candidate"
      inferred_out="$out_candidate"
      break
    fi
  done

  if [[ -z "$inferred_config" ]]; then
    echo "Error: Could not parse arguments. If your paths contain spaces, quote them." >&2
    echo "Usage: $0 <config.properties> [out/welch.csv]" >&2
    echo "Example: $0 \"$ROOT/configs/sanity.properties\" \"$ROOT/out/welch.csv\"" >&2
    exit 2
  fi

  CONFIG="$inferred_config"
  OUT="$inferred_out"
fi

if [[ -z "$CONFIG" ]]; then
  echo "Usage: $0 <config.properties> [out/welch.csv]" >&2
  echo "Tip: If the path contains spaces, wrap it in quotes." >&2
  exit 2
fi

"$ROOT/scripts/compile.sh"
mkdir -p "$(dirname "$OUT")"

# Dynamic fetching of plotting parameters from the config file
WINDOW=$(grep "^welch.window=" "$CONFIG" | cut -d'=' -f2 || echo "7")
STABILITY_BINS=$(grep "^welch.stabilityBins=" "$CONFIG" | cut -d'=' -f2 || echo "8")
STABILITY_PCT=$(grep "^welch.stabilityPct=" "$CONFIG" | cut -d'=' -f2 || echo "5.0")
X_AXIS=$(grep "^welch.xAxis=" "$CONFIG" | cut -d'=' -f2 || echo "time")
AUTO_ZOOM=$(grep "^welch.autoZoom=" "$CONFIG" | cut -d'=' -f2 || echo "on")
X_MAX=$(grep "^welch.xMax=" "$CONFIG" | cut -d'=' -f2 || echo "")

# Run simulation
java -cp build/classes des.Main --mode welch --config "$CONFIG" --out "$OUT"

# Construct optional x-max argument
X_MAX_ARG=""
if [[ -n "$X_MAX" ]]; then
    X_MAX_ARG="--welch-x-max $X_MAX"
fi

# Run plotting with parameters from config
python3 "$ROOT/scripts/plot_results.py" --welch \
    --welch-window "$WINDOW" \
    --welch-stability-bins "$STABILITY_BINS" \
    --welch-stability-pct "$STABILITY_PCT" \
    --welch-x-axis "$X_AXIS" \
    --welch-auto-zoom "$AUTO_ZOOM" \
    $X_MAX_ARG \
    "$OUT"

