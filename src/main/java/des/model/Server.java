package des.model;

import des.sim.Simulation;
import java.util.ArrayDeque;

/**
 * Server-side resource model containing thread pool, wait queue, and CPU cores.
 */
public final class Server {
  /** Physical cores available for CPU scheduling. */
  public final Core[] cores;
  /** Worker thread pool available for request execution. */
  public final SimThread[] threads;

  /** Idle worker deque for O(1) dispatch lookup. */
  private final ArrayDeque<SimThread> idleThreads = new ArrayDeque<>();
  /** Requests waiting for an available worker thread. */
  private final ArrayDeque<Request> threadWaitQ = new ArrayDeque<>();
  /** Maximum wait-queue size; -1 means unbounded. */
  private final int maxQueue;

  /** Creates cores, threads, and initial idle-thread pool. */
  public Server(int coresCount, int maxThreads, int maxQueue, double quantumMs, double ctxSwitchMs) {
    if (coresCount <= 0) {
      throw new IllegalArgumentException("cores must be > 0");
    }
    if (maxThreads <= 0) {
      throw new IllegalArgumentException("maxThreads must be > 0");
    }
    if (maxQueue < -1) {
      throw new IllegalArgumentException("maxQueue must be >= -1");
    }
    this.maxQueue = maxQueue;
    this.cores = new Core[coresCount];
    for (int i = 0; i < coresCount; i++) {
      this.cores[i] = new Core(i, quantumMs, ctxSwitchMs);
    }

    this.threads = new SimThread[maxThreads];
    for (int i = 0; i < maxThreads; i++) {
      int coreId = i % coresCount;
      SimThread t = new SimThread(i, coreId);
      threads[i] = t;
      idleThreads.addLast(t);
    }
  }

  /** Returns current number of requests waiting for worker assignment. */
  public int waitQueueSize() {
    return threadWaitQ.size();
  }

  /** Returns number of currently idle threads. */
  public int idleThreadsCount() {
    return idleThreads.size();
  }

  /** Resets per-core utilization window at warm-up boundary. */
  public void resetUtilization(double nowMs) {
    for (Core c : cores) {
      c.resetUtilization(nowMs);
    }
  }

  /** Closes utilization accounting interval at replication end. */
  public void finishUtilization(double endMs) {
    for (Core c : cores) {
      c.finishUtilization(endMs);
    }
  }

  /**
   * Accepts a request into service or queue.
   *
   * <p>Returns false only when queue capacity is configured and already full.
   */
  public boolean accept(Request r, SimState state, Simulation sim) {
    SimThread t = idleThreads.pollFirst();
    if (t == null) {
      // No free thread: enqueue request unless queue capacity is exhausted.
      if (maxQueue >= 0 && threadWaitQ.size() >= maxQueue) {
        return false;
      }
      threadWaitQ.addLast(r);
      state.metrics.observeWaitQueue(sim.nowMs(), threadWaitQ.size());
      state.trace.onQueued(sim.nowMs(), r, threadWaitQ.size(), idleThreads.size());
      return true;
    }
    assignToThread(t, r, state, sim);
    return true;
  }

  /** Reuses idle thread for queued work or returns it to idle pool. */
  public void onThreadBecameIdle(SimThread t, SimState state, Simulation sim) {
    if (!t.isIdle()) {
      throw new IllegalStateException("thread is not idle");
    }
    Request next = threadWaitQ.pollFirst();
    if (next != null) {
      state.metrics.observeWaitQueue(sim.nowMs(), threadWaitQ.size());
      assignToThread(t, next, state, sim);
      return;
    }
    idleThreads.addLast(t);
  }

  /** Assigns one request to a specific worker and enqueues it on the target core. */
  private void assignToThread(SimThread t, Request r, SimState state, Simulation sim) {
    if (!t.isIdle()) {
      throw new IllegalStateException("assigning to a busy thread");
    }
    t.current = r;
    Core core = cores[t.coreId];
    state.trace.onDispatch(sim.nowMs(), r, core.id, t.id, threadWaitQ.size(), idleThreads.size());
    core.enqueueRunnable(t, state, sim);
  }
}

