package des.model;

import des.events.UserIssueRequestEvent;
import des.sim.Simulation;

/** Closed-loop user that alternates between think and request phases. */
public final class User {
  /** Epsilon used to avoid exact zero-delay re-scheduling. */
  private static final double EPS_MS = 1e-9;

  /** User identifier. */
  public final int id;
  /** Request currently awaited by this user (-1 when idle/thinking). */
  public long waitingRequestId = -1L;

  /** Creates user session state. */
  public User(int id) {
    this.id = id;
  }

  /** Schedules first request after an initial think period. */
  public void scheduleInitial(SimState state, Simulation sim) {
    double thinkMs = Math.max(0.0, state.thinkDist.sampleMs());
    sim.schedule(new UserIssueRequestEvent(sim.nowMs() + Math.max(thinkMs, EPS_MS), id, state));
  }

  /** Handles successful response and schedules next request cycle. */
  public void onGoodResponse(Request r, SimState state, Simulation sim) {
    if (waitingRequestId != r.id) {
      return;
    }
    waitingRequestId = -1L;
    scheduleNext(state, sim);
  }

  /** Handles timeout-visible completion from user's perspective. */
  public void onTimeout(Request r, SimState state, Simulation sim) {
    if (waitingRequestId != r.id) {
      return;
    }
    waitingRequestId = -1L;
    scheduleNext(state, sim);
  }

  /** Handles immediate drop (queue full) and continues closed-loop cycle. */
  public void onDropped(Request r, SimState state, Simulation sim) {
    if (waitingRequestId != r.id) {
      return;
    }
    waitingRequestId = -1L;
    scheduleNext(state, sim);
  }

  /** Shared helper to schedule next issue event after think time. */
  private void scheduleNext(SimState state, Simulation sim) {
    double thinkMs = Math.max(0.0, state.thinkDist.sampleMs());
    sim.schedule(new UserIssueRequestEvent(sim.nowMs() + Math.max(thinkMs, EPS_MS), id, state));
  }
}

