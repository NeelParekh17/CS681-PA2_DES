package des.model;

import des.sim.Simulation;
import java.util.ArrayDeque;

public final class Server {
  public final Core[] cores;
  public final SimThread[] threads;

  private final ArrayDeque<SimThread> idleThreads = new ArrayDeque<>();
  private final ArrayDeque<Request> threadWaitQ = new ArrayDeque<>();

  public Server(int coresCount, int maxThreads, double quantumMs, double ctxSwitchMs) {
    if (coresCount <= 0) {
      throw new IllegalArgumentException("cores must be > 0");
    }
    if (maxThreads <= 0) {
      throw new IllegalArgumentException("maxThreads must be > 0");
    }
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

  public void accept(Request r, SimState state, Simulation sim) {
    SimThread t = idleThreads.pollFirst();
    if (t == null) {
      threadWaitQ.addLast(r);
      state.trace.onQueued(sim.nowMs(), r, threadWaitQ.size(), idleThreads.size());
      return;
    }
    assignToThread(t, r, state, sim);
  }

  public void onThreadBecameIdle(SimThread t, SimState state, Simulation sim) {
    if (!t.isIdle()) {
      throw new IllegalStateException("thread is not idle");
    }
    Request next = threadWaitQ.pollFirst();
    if (next != null) {
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

