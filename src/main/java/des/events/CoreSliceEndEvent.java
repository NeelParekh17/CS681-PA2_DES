package des.events;

import des.model.Core;
import des.model.Request;
import des.model.SimState;
import des.model.SimThread;
import des.sim.Event;
import des.sim.Simulation;

/**
 * Completes one CPU timeslice on a core/thread and advances request execution.
 *
 * <p>This event either finishes the request or re-queues the thread for another slice.
 */
public final class CoreSliceEndEvent extends Event {
  /** Core that executed this slice. */
  private final int coreId;
  /** Thread that executed this slice. */
  private final int threadId;
  /** Request expected on this thread when event fires. */
  private final long requestId;
  /** CPU work consumed during this slice (excluding context switch overhead). */
  private final double runCpuMs;
  /** Slice token used to invalidate stale scheduled events. */
  private final long token;
  /** Shared simulation state referenced by handlers. */
  private final SimState state;

  /** Creates a slice-completion event for one core/thread/request tuple. */
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
  /** Applies CPU progress and drives request completion or re-queue behavior. */
  public void process(Simulation sim) {
    Core core = state.server.cores[coreId];
    SimThread running = core.running;
    // Guard against stale events after preemption/reordering.
    if (running == null || running.id != threadId) {
      return;
    }
    Request r = running.current;
    if (r == null || r.id != requestId) {
      return;
    }
    // Token ensures only the latest scheduled slice end can mutate this core.
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
