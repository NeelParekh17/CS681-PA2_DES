package des.model;

import des.events.UserIssueRequestEvent;
import des.sim.Simulation;

public final class User {
  private static final double EPS_MS = 1e-9;

  public final int id;
  public long waitingRequestId = -1L;

  public User(int id) {
    this.id = id;
  }

  public void scheduleInitial(SimState state, Simulation sim) {
    double thinkMs = Math.max(0.0, state.thinkDist.sampleMs());
    sim.schedule(new UserIssueRequestEvent(sim.nowMs() + Math.max(thinkMs, EPS_MS), id, state));
  }

  public void onGoodResponse(Request r, SimState state, Simulation sim) {
    if (waitingRequestId != r.id) {
      return;
    }
    waitingRequestId = -1L;
    scheduleNext(state, sim);
  }

  public void onTimeout(Request r, SimState state, Simulation sim) {
    if (waitingRequestId != r.id) {
      return;
    }
    waitingRequestId = -1L;
    scheduleNext(state, sim);
  }

  public void onDropped(Request r, SimState state, Simulation sim) {
    if (waitingRequestId != r.id) {
      return;
    }
    waitingRequestId = -1L;
    scheduleNext(state, sim);
  }

  private void scheduleNext(SimState state, Simulation sim) {
    double thinkMs = Math.max(0.0, state.thinkDist.sampleMs());
    sim.schedule(new UserIssueRequestEvent(sim.nowMs() + Math.max(thinkMs, EPS_MS), id, state));
  }
}

