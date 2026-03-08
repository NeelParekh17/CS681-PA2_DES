package des.model;

import des.events.CoreSliceEndEvent;
import des.sim.Simulation;
import java.util.ArrayDeque;

public final class Core {
  private static final double EPS_MS = 1e-9;

  public final int id;
  public final double quantumMs;
  public final double ctxSwitchMs;

  public final ArrayDeque<SimThread> runnable = new ArrayDeque<>();
  public SimThread running = null;

  private boolean busy = false;
  private double busyStartMs = 0.0;
  private double busyTimeMs = 0.0;

  private int lastThreadId = -1;
  private long sliceToken = 0L;

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

  public double busyTimeMs() {
    return busyTimeMs;
  }

  public boolean isBusy() {
    return busy;
  }

  public void resetUtilization(double nowMs) {
    busyTimeMs = 0.0;
    if (busy) {
      busyStartMs = nowMs;
    }
  }

  public void finishUtilization(double endMs) {
    if (busy) {
      busyTimeMs += endMs - busyStartMs;
      busyStartMs = endMs;
    }
  }

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

  public void onSliceEnded(SimState state, Simulation sim) {
    scheduleNextSlice(state, sim);
  }

  public long currentSliceToken() {
    return sliceToken;
  }

  long nextSliceToken() {
    sliceToken++;
    return sliceToken;
  }

  private void scheduleNextSlice(SimState state, Simulation sim) {
    if (running != null) {
      return;
    }
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
