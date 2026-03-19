package des.sim;

import java.util.PriorityQueue;

/** Priority-queue based DES kernel that advances simulation clock by next event. */
public final class Simulation {
  /** Future-event set ordered by time/priority/sequence. */
  private final PriorityQueue<Event> queue = new PriorityQueue<>();
  /** Next insertion sequence for deterministic same-time ordering. */
  private long nextSeq = 0L;
  /** Current simulation clock in milliseconds. */
  private double nowMs = 0.0;

  /** Returns current simulation time. */
  public double nowMs() {
    return nowMs;
  }

  /** Adds one event to the future-event set with assigned sequence id. */
  public void schedule(Event e) {
    e.setSeq(nextSeq++);
    queue.add(e);
  }

  /** Returns whether no future events remain. */
  public boolean isEmpty() {
    return queue.isEmpty();
  }

  public void runUntil(double endMs) {
    // Process events in chronological order until reaching the simulation horizon.
    while (!queue.isEmpty()) {
      Event next = queue.peek();
      if (next.timeMs > endMs) {
        nowMs = endMs;
        return;
      }

      queue.poll();
      if (next.isCancelled()) {
        continue;
      }
      nowMs = next.timeMs;
      next.process(this);
    }
    nowMs = endMs;
  }
}

