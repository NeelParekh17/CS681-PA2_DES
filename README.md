# Discrete Event Simulator (DES) — Web Server Model (Java 21)

## Quick start
Compile:
- `./scripts/compile.sh`

Run a trace demo:
- `./scripts/run_demo.sh configs/demo.properties out/trace_demo.txt`

Run an experiment sweep (writes CSVs):
- `./scripts/run_experiment.sh 'configs/experiment.properties' 'out/Constant-server_1_40'`

Generate plots from `summary.csv`:
- `python3 scripts/plot_response_time_ci.py --summary "out/Constant-server_1_40/summary.csv"`

Welch time‑series helper (warm‑up selection run for each user):
- `./scripts/run_welch.sh configs/welch.properties out/welch.csv`

## Config notes
All configs are `.properties` files in `configs/`.

Time values support suffixes: `ms`, `s`, `m` (e.g. `5ms`, `2s`, `1.5m`).

Distributions use function‑style strings:
- `constant(10ms)`
- `uniform(5ms, 15ms)`
- `exponential(8ms)`
- `triangular(2s, 5s, 10s)`
- `shifted(2ms, exponential(3ms))`

Queue capacity:
- `sim.maxQueue=-1` means unbounded waiting queue (default behavior).
- `sim.maxQueue=N` drops requests when `N` requests are already waiting for a thread.

