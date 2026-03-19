package des.sim;

/** Base type for scheduled discrete events ordered by time, priority, and sequence. */
public abstract class Event implements Comparable<Event> {
  /** Simulation timestamp at which this event should fire. */
  public final double timeMs;
  /** Priority tie-breaker for same-time events (lower value executes first). */
  public final int priority;
  /** Monotonic insertion sequence assigned by the kernel. */
  private long seq;
  /** Lazy-cancellation flag checked when dequeued. */
  private boolean cancelled = false;

  /** Creates event shell with execution time and priority. */
  protected Event(double timeMs, int priority) {
    this.timeMs = timeMs;
    this.priority = priority;
  }

  /** Assigned by Simulation.schedule for deterministic ordering. */
  final void setSeq(long seq) {
    this.seq = seq;
  }

  /** Returns assigned insertion sequence. */
  public final long seq() {
    return seq;
  }

  /** Marks event as cancelled; kernel will skip it when popped. */
  public final void cancel() {
    cancelled = true;
  }

  /** Returns true if event has been cancelled. */
  public final boolean isCancelled() {
    return cancelled;
  }

  /** Event-specific state transition logic. */
  public abstract void process(Simulation sim);

  /** Natural ordering used by simulation priority queue. */
  @Override
  public final int compareTo(Event other) {
    int t = Double.compare(this.timeMs, other.timeMs);
    if (t != 0) {
      return t;
    }
    int p = Integer.compare(this.priority, other.priority);
    if (p != 0) {
      return p;
    }
    return Long.compare(this.seq, other.seq);
  }
}

