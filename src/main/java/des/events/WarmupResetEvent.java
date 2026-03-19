package des.events;

import des.model.SimState;
import des.sim.Event;
import des.sim.Simulation;

/** Starts metric collection after the warm-up phase. */
public final class WarmupResetEvent extends Event {
  /** Shared state whose counters and utilization windows are reset. */
  private final SimState state;

  /** Creates warmup-reset event at configured warmup boundary. */
  public WarmupResetEvent(double timeMs, SimState state) {
    super(timeMs, -1);
    this.state = state;
  }

  @Override
  /** Switches metrics from warmup to measurement mode. */
  public void process(Simulation sim) {
    state.metrics.startMeasurement(sim.nowMs());
    state.metrics.observeWaitQueue(sim.nowMs(), state.server.waitQueueSize());
    state.server.resetUtilization(sim.nowMs());
    state.trace.onWarmupReset(sim.nowMs());
  }
}

