package des.model;

import des.dist.Distribution;
import des.metrics.Metrics;
import des.trace.TraceLogger;
import des.welch.WelchCollector;

/** Shared mutable state container passed to all event handlers during a run. */
public final class SimState {
  /** Monotonic id source for request creation. */
  private long nextRequestId = 1L;

  /** Think-time distribution for users between requests. */
  public final Distribution thinkDist;
  /** CPU service-demand distribution for requests. */
  public final Distribution serviceDist;
  /** Timeout distribution used to set request deadlines. */
  public final Distribution timeoutDist;
  /** Server resource model: threads, wait queue, cores. */
  public final Server server;
  /** Closed-loop user population. */
  public final User[] users;
  /** Per-replication metric accumulator. */
  public final Metrics metrics;
  /** Optional event trace sink. */
  public final TraceLogger trace;
  /** Optional Welch collector for response binning. */
  public final WelchCollector welch;

  /** Creates a full shared-state object for one replication. */
  public SimState(
      Distribution thinkDist,
      Distribution serviceDist,
      Distribution timeoutDist,
      Server server,
      User[] users,
      Metrics metrics,
      TraceLogger trace,
      WelchCollector welch) {
    this.thinkDist = thinkDist;
    this.serviceDist = serviceDist;
    this.timeoutDist = timeoutDist;
    this.server = server;
    this.users = users;
    this.metrics = metrics;
    this.trace = trace;
    this.welch = welch;
  }

  /** Returns next unique request id. */
  public long nextRequestId() {
    return nextRequestId++;
  }
}
