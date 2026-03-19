package des.model;

import des.sim.Event;

/** Mutable request state tracked from issue time through completion/timeout. */
public final class Request {
  /** Unique request id within replication. */
  public final long id;
  /** Originating user id. */
  public final int userId;
  /** Simulation time when request was issued. */
  public final double issueTimeMs;
  /** Absolute timeout deadline in simulation time. */
  public final double timeoutAtMs;

  /** Remaining CPU service demand. */
  public double remainingCpuMs;
  /** Whether timeout already fired before completion. */
  public boolean timedOut = false;
  /** Whether request reached terminal completion state. */
  public boolean completed = false;
  /** Completion timestamp when completed=true. */
  public double completionTimeMs = Double.NaN;

  /** Whether issue happened after warmup reset. */
  public boolean measuredIssue = false;
  /** Scheduled timeout event reference for cancellation on good completion. */
  public Event timeoutEvent = null;

  /** Constructs request with sampled demand and timeout deadline. */
  public Request(long id, int userId, double issueTimeMs, double timeoutAtMs, double cpuMs) {
    this.id = id;
    this.userId = userId;
    this.issueTimeMs = issueTimeMs;
    this.timeoutAtMs = timeoutAtMs;
    this.remainingCpuMs = cpuMs;
  }
}

