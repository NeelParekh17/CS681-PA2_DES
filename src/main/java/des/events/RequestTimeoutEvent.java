package des.events;

import des.model.Request;
import des.model.SimState;
import des.model.User;
import des.sim.Event;
import des.sim.Simulation;

/** Fires when a request deadline is reached before completion. */
public final class RequestTimeoutEvent extends Event {
  /** Timed request tracked by this timeout. */
  private final Request request;
  /** Shared simulation state referenced by handlers. */
  private final SimState state;

  /** Creates timeout event bound to one request. */
  public RequestTimeoutEvent(double timeMs, Request request, SimState state) {
    super(timeMs, 1);
    this.request = request;
    this.state = state;
  }

  @Override
  /** Marks timeout and wakes waiting user if request still pending. */
  public void process(Simulation sim) {
    if (request.completed) {
      return;
    }
    request.timedOut = true;
    state.metrics.onTimeout(request, sim.nowMs());

    User u = state.users[request.userId];
    state.trace.onTimeout(sim.nowMs(), request);
    u.onTimeout(request, state, sim);
  }
}

