#!/usr/bin/env bash
set -euo pipefail

ROOT="$(cd "$(dirname "${BASH_SOURCE[0]}")/.." && pwd)"
cd "$ROOT"

CONFIG="${1:-}"
OUT="${2:-out/trace.txt}"

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
    echo "Usage: $0 <config.properties> [out/trace.txt]" >&2
    echo "Example: $0 \"$ROOT/configs/demo.properties\" \"$ROOT/out/trace_demo.txt\"" >&2
    exit 2
  fi

  CONFIG="$inferred_config"
  OUT="$inferred_out"
fi

if [[ -z "$CONFIG" ]]; then
  echo "Usage: $0 <config.properties> [out/trace.txt]" >&2
  echo "Tip: If the path contains spaces, wrap it in quotes." >&2
  exit 2
fi

"$ROOT/scripts/compile.sh"
mkdir -p "$(dirname "$OUT")"
java -cp build/classes des.Main --mode demo --config "$CONFIG" --out "$OUT"

