package des.events;

import des.model.Request;
import des.model.SimState;
import des.model.User;
import des.sim.Event;
import des.sim.Simulation;

public final class UserIssueRequestEvent extends Event {
  private static final double EPS_MS = 1e-9;

  private final int userId;
  private final SimState state;

  public UserIssueRequestEvent(double timeMs, int userId, SimState state) {
    super(timeMs, 2);
    this.userId = userId;
    this.state = state;
  }

  @Override
  public void process(Simulation sim) {
    User user = state.users[userId];

    long requestId = state.nextRequestId();
    double issueTimeMs = sim.nowMs();
    double cpuMs = Math.max(0.0, state.serviceDist.sampleMs());
    double timeoutAfterMs = Math.max(0.0, state.timeoutDist.sampleMs());
    double timeoutAtMs = issueTimeMs + Math.max(timeoutAfterMs, EPS_MS);

    Request r = new Request(requestId, userId, issueTimeMs, timeoutAtMs, cpuMs);
    r.measuredIssue = state.metrics.isMeasuring();

    user.waitingRequestId = requestId;
    state.metrics.onIssue(r);

    RequestTimeoutEvent to = new RequestTimeoutEvent(timeoutAtMs, r, state);
    r.timeoutEvent = to;
    sim.schedule(to);

    state.trace.onIssue(issueTimeMs, r, state.server.waitQueueSize(), state.server.idleThreadsCount());
    state.server.accept(r, state, sim);
  }
}

