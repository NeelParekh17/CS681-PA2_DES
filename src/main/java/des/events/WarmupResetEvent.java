package des.events;

import des.model.SimState;
import des.sim.Event;
import des.sim.Simulation;

public final class WarmupResetEvent extends Event {
  private final SimState state;

  public WarmupResetEvent(double timeMs, SimState state) {
    super(timeMs, -1);
    this.state = state;
  }

  @Override
  public void process(Simulation sim) {
    state.metrics.startMeasurement(sim.nowMs());
    state.metrics.observeWaitQueue(sim.nowMs(), state.server.waitQueueSize());
    state.server.resetUtilization(sim.nowMs());
    state.trace.onWarmupReset(sim.nowMs());
  }
}

