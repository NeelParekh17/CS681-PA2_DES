package des.sim;

public abstract class Event implements Comparable<Event> {
  public final double timeMs;
  public final int priority;
  private long seq;
  private boolean cancelled = false;

  protected Event(double timeMs, int priority) {
    this.timeMs = timeMs;
    this.priority = priority;
  }

  final void setSeq(long seq) {
    this.seq = seq;
  }

  public final long seq() {
    return seq;
  }

  public final void cancel() {
    cancelled = true;
  }

  public final boolean isCancelled() {
    return cancelled;
  }

  public abstract void process(Simulation sim);

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

