#!/usr/bin/env bash
set -euo pipefail

ROOT="$(cd "$(dirname "${BASH_SOURCE[0]}")/.." && pwd)"
cd "$ROOT"

CONFIG="${1:-}"
OUT="${2:-out/welch.csv}"

if [[ -z "$CONFIG" ]]; then
  echo "Usage: $0 <config.properties> [out/welch.csv]" >&2
  exit 2
fi

"$ROOT/scripts/compile.sh"
mkdir -p "$(dirname "$OUT")"
java -cp build/classes des.Main --mode welch --config "$CONFIG" --out "$OUT"

