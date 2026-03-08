package des.events;

import des.model.Request;
import des.model.SimState;
import des.model.User;
import des.sim.Event;
import des.sim.Simulation;

public final class RequestTimeoutEvent extends Event {
  private final Request request;
  private final SimState state;

  public RequestTimeoutEvent(double timeMs, Request request, SimState state) {
    super(timeMs, 1);
    this.request = request;
    this.state = state;
  }

  @Override
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

