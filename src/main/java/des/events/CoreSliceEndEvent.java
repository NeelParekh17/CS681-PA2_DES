package des.events;

import des.model.Core;
import des.model.Request;
import des.model.SimState;
import des.model.SimThread;
import des.sim.Event;
import des.sim.Simulation;

public final class CoreSliceEndEvent extends Event {
  private final int coreId;
  private final int threadId;
  private final long requestId;
  private final double runCpuMs;
  private final long token;
  private final SimState state;

  public CoreSliceEndEvent(
      double timeMs,
      int coreId,
      int threadId,
      long requestId,
      double runCpuMs,
      long token,
      SimState state) {
    super(timeMs, 0);
    this.coreId = coreId;
    this.threadId = threadId;
    this.requestId = requestId;
    this.runCpuMs = runCpuMs;
    this.token = token;
    this.state = state;
  }

  @Override
  public void process(Simulation sim) {
    Core core = state.server.cores[coreId];
    SimThread running = core.running;
    if (running == null || running.id != threadId) {
      return;
    }
    Request r = running.current;
    if (r == null || r.id != requestId) {
      return;
    }
    if (token != core.currentSliceToken()) {
      return;
    }

    if (runCpuMs > 0.0) {
      r.remainingCpuMs = Math.max(0.0, r.remainingCpuMs - runCpuMs);
    }

    core.running = null;

    boolean completed = r.remainingCpuMs <= 1e-9;
    if (completed) {
      r.completed = true;
      r.completionTimeMs = sim.nowMs();
      running.current = null;

      if (!r.timedOut) {
        if (r.timeoutEvent != null) {
          r.timeoutEvent.cancel();
        }
        if (state.welch != null) {
          state.welch.record(sim.nowMs(), sim.nowMs() - r.issueTimeMs);
        }
        state.metrics.onGoodCompletion(r, sim.nowMs());
        state.trace.onGoodCompletion(sim.nowMs(), r, coreId, threadId);
        state.users[r.userId].onGoodResponse(r, state, sim);
      } else {
        state.metrics.onBadCompletion(r, sim.nowMs());
        state.trace.onBadCompletion(sim.nowMs(), r, coreId, threadId);
      }

      state.server.onThreadBecameIdle(running, state, sim);
    } else {
      state.trace.onTimeslice(sim.nowMs(), r, coreId, threadId, r.remainingCpuMs, core.runnable.size());
      core.enqueueRunnable(running, state, sim);
    }

    core.onSliceEnded(state, sim);
  }
}
