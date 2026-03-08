package des.model;

import des.sim.Event;

public final class Request {
  public final long id;
  public final int userId;
  public final double issueTimeMs;
  public final double timeoutAtMs;

  public double remainingCpuMs;
  public boolean timedOut = false;
  public boolean completed = false;
  public double completionTimeMs = Double.NaN;

  public boolean measuredIssue = false;
  public Event timeoutEvent = null;

  public Request(long id, int userId, double issueTimeMs, double timeoutAtMs, double cpuMs) {
    this.id = id;
    this.userId = userId;
    this.issueTimeMs = issueTimeMs;
    this.timeoutAtMs = timeoutAtMs;
    this.remainingCpuMs = cpuMs;
  }
}

