package des.model;

import des.dist.Distribution;
import des.metrics.Metrics;
import des.trace.TraceLogger;
import des.welch.WelchCollector;

public final class SimState {
  private long nextRequestId = 1L;

  public final Distribution thinkDist;
  public final Distribution serviceDist;
  public final Distribution timeoutDist;
  public final Server server;
  public final User[] users;
  public final Metrics metrics;
  public final TraceLogger trace;
  public final WelchCollector welch;

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

  public long nextRequestId() {
    return nextRequestId++;
  }
}
