package des.model;

import des.sim.Simulation;
import java.util.ArrayDeque;

public final class Server {
  public final Core[] cores;
  public final SimThread[] threads;

  private final ArrayDeque<SimThread> idleThreads = new ArrayDeque<>();
  private final ArrayDeque<Request> threadWaitQ = new ArrayDeque<>();
  private final int maxQueue;

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

  public int waitQueueSize() {
    return threadWaitQ.size();
  }

  public int idleThreadsCount() {
    return idleThreads.size();
  }

  public void resetUtilization(double nowMs) {
    for (Core c : cores) {
      c.resetUtilization(nowMs);
    }
  }

  public void finishUtilization(double endMs) {
    for (Core c : cores) {
      c.finishUtilization(endMs);
    }
  }

  public boolean accept(Request r, SimState state, Simulation sim) {
    SimThread t = idleThreads.pollFirst();
    if (t == null) {
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

