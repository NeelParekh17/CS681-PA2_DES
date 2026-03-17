# Discrete Event Simulator (DES) — Web Server Model (Java 21)

This repo contains a small discrete‑event simulation of a multi‑core, multi‑threaded web server with:

- Thread‑to‑core affinity (fixed)
- Thread‑per‑task up to `sim.maxThreads`, then queuing for threads
- Optional bounded waiting queue via `sim.maxQueue` (queue-full requests are dropped)
- Per‑core round‑robin scheduling with configurable quantum + context‑switch overhead
- Closed‑loop users (request → wait/timeout → think → next request)
- Service and timeout distributions (constant/uniform/exponential/triangular/shifted)
- Metrics: goodput/badput/throughput, timeout/drop rates, avg response time (+ 95% CI), avg/max waiting-queue length, avg core utilization

## Requirements
- Java 21 (`java`, `javac`)
- Python 3 + matplotlib (only for plotting)

## Quick start
Compile:
- `./scripts/compile.sh`

Run a trace demo:
- `./scripts/run_demo.sh configs/demo.properties out/trace_demo.txt`

Run an experiment sweep (writes CSVs):
- `./scripts/run_experiment.sh configs/experiment.properties out/summary.csv`
  - Also writes `out/replications.csv` next to the summary.

Generate plots from `summary.csv`:
- `./scripts/plot_results.py out/summary.csv out/plots/`

Welch time‑series helper (warm‑up selection):
- `./scripts/run_welch.sh configs/experiment.properties out/welch.csv`

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

