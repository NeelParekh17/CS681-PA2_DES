#!/usr/bin/env bash
set -euo pipefail

ROOT="$(cd "$(dirname "${BASH_SOURCE[0]}")/.." && pwd)"
cd "$ROOT"

mkdir -p build/classes
find src/main/java -name "*.java" > build/sources.txt

if [[ ! -s build/sources.txt ]]; then
  echo "No Java sources found under src/main/java" >&2
  exit 1
fi

javac -encoding UTF-8 -d build/classes @build/sources.txt

