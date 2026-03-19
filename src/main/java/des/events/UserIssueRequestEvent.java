package des.events;

import des.model.Request;
import des.model.SimState;
import des.model.User;
import des.sim.Event;
import des.sim.Simulation;

/** Generates a new request from a user and submits it to the server model. */
public final class UserIssueRequestEvent extends Event {
  /** Epsilon used to avoid zero-delay re-scheduling loops. */
  private static final double EPS_MS = 1e-9;

  /** User that issues this request. */
  private final int userId;
  /** Shared simulation state referenced by handlers. */
  private final SimState state;

  /** Creates a user-issue event at the given simulation timestamp. */
  public UserIssueRequestEvent(double timeMs, int userId, SimState state) {
    super(timeMs, 2);
    this.userId = userId;
    this.state = state;
  }

  @Override
  /** Samples request parameters, schedules timeout, and submits to server queue/thread. */
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
    boolean accepted = state.server.accept(r, state, sim);
    if (!accepted) {
      r.completed = true;
      if (r.timeoutEvent != null) {
        r.timeoutEvent.cancel();
      }
      state.metrics.onDrop(r, sim.nowMs());
      state.trace.onDrop(sim.nowMs(), r, state.server.waitQueueSize(), state.server.idleThreadsCount());
      user.onDropped(r, state, sim);
    }
  }
}

