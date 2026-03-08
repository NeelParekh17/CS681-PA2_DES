package des.sim;

import java.util.PriorityQueue;

public final class Simulation {
  private final PriorityQueue<Event> queue = new PriorityQueue<>();
  private long nextSeq = 0L;
  private double nowMs = 0.0;

  public double nowMs() {
    return nowMs;
  }

  public void schedule(Event e) {
    e.setSeq(nextSeq++);
    queue.add(e);
  }

  public boolean isEmpty() {
    return queue.isEmpty();
  }

  public void runUntil(double endMs) {
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

