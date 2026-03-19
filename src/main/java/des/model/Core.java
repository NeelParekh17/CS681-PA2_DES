package des.model;

import des.events.CoreSliceEndEvent;
import des.sim.Simulation;
import java.util.ArrayDeque;

/**
 * Per-core round-robin scheduler with runnable queue and utilization accounting.
 */
public final class Core {
  /** Tiny positive delta used when a computed slice duration is zero. */
  private static final double EPS_MS = 1e-9;

  /** Core identifier. */
  public final int id;
  /** Round-robin time quantum used for service progression. */
  public final double quantumMs;
  /** Context-switch overhead when switching threads on this core. */
  public final double ctxSwitchMs;

  /** Runnable threads waiting for CPU on this core. */
  public final ArrayDeque<SimThread> runnable = new ArrayDeque<>();
  /** Currently running thread, null when core idle. */
  public SimThread running = null;

  /** True while core has work pending/running in current measurement window. */
  private boolean busy = false;
  /** Timestamp when current busy interval started. */
  private double busyStartMs = 0.0;
  /** Accumulated busy time in milliseconds. */
  private double busyTimeMs = 0.0;

  /** Last executed thread id used to determine context-switch penalty. */
  private int lastThreadId = -1;
  /** Monotonic token used to invalidate stale slice-end events. */
  private long sliceToken = 0L;

  /** Creates per-core scheduler state and validates quantum/overhead values. */
  public Core(int id, double quantumMs, double ctxSwitchMs) {
    if (quantumMs <= 0.0) {
      throw new IllegalArgumentException("quantum must be > 0");
    }
    if (ctxSwitchMs < 0.0) {
      throw new IllegalArgumentException("ctxSwitch must be >= 0");
    }
    this.id = id;
    this.quantumMs = quantumMs;
    this.ctxSwitchMs = ctxSwitchMs;
  }

  /** Returns accumulated busy time. */
  public double busyTimeMs() {
    return busyTimeMs;
  }

  /** Returns whether core is currently marked busy. */
  public boolean isBusy() {
    return busy;
  }

  /** Resets utilization accumulator at warm-up boundary. */
  public void resetUtilization(double nowMs) {
    busyTimeMs = 0.0;
    if (busy) {
      busyStartMs = nowMs;
    }
  }

  /** Finalizes utilization accumulation at simulation end. */
  public void finishUtilization(double endMs) {
    if (busy) {
      busyTimeMs += endMs - busyStartMs;
      busyStartMs = endMs;
    }
  }

  /** Adds thread to runnable queue and schedules execution if core is idle. */
  public void enqueueRunnable(SimThread t, SimState state, Simulation sim) {
    runnable.addLast(t);
    if (!busy) {
      busy = true;
      busyStartMs = sim.nowMs();
    }
    if (running == null) {
      scheduleNextSlice(state, sim);
    }
  }

  /** Called when a slice completes to continue round-robin scheduling. */
  public void onSliceEnded(SimState state, Simulation sim) {
    scheduleNextSlice(state, sim);
  }

  /** Exposes current token so events can verify freshness. */
  public long currentSliceToken() {
    return sliceToken;
  }

  /** Advances token before scheduling a new slice. */
  long nextSliceToken() {
    sliceToken++;
    return sliceToken;
  }

  /** Core RR scheduler: pick next runnable, compute duration, schedule slice-end event. */
  private void scheduleNextSlice(SimState state, Simulation sim) {
    if (running != null) {
      return;
    }
    // Transition to idle and close the busy interval when no runnable threads remain.
    if (runnable.isEmpty()) {
      if (busy) {
        busyTimeMs += sim.nowMs() - busyStartMs;
        busy = false;
      }
      return;
    }

    SimThread next = runnable.pollFirst();
    Request r = next.current;
    if (r == null) {
      throw new IllegalStateException("scheduled a thread with no request");
    }

    double runCpuMs = Math.min(quantumMs, Math.max(0.0, r.remainingCpuMs));
    double overheadMs = (lastThreadId == next.id) ? 0.0 : ctxSwitchMs;
    lastThreadId = next.id;

    running = next;
    long token = nextSliceToken();

    double delta = overheadMs + runCpuMs;
    if (delta <= 0.0) {
      delta = EPS_MS;
    }

    sim.schedule(
        new CoreSliceEndEvent(sim.nowMs() + delta, id, next.id, r.id, runCpuMs, token, state));
  }
}
