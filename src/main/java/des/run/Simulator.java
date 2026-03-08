package des.run;

import des.dist.Distribution;
import des.dist.DistributionParser;
import des.metrics.Metrics;
import des.metrics.ReplicationResult;
import des.model.Server;
import des.model.SimState;
import des.model.User;
import des.rng.Rng;
import des.sim.Simulation;
import des.trace.TraceLogger;
import des.welch.WelchCollector;
import des.events.WarmupResetEvent;

public final class Simulator {
  private Simulator() {}

  public static ReplicationResult runReplication(
      int users,
      int replication,
      long seed,
      int cores,
      int maxThreads,
      double quantumMs,
      double ctxSwitchMs,
      double warmupMs,
      double measureMs,
      String thinkSpec,
      String serviceSpec,
      String timeoutSpec,
      TraceLogger trace,
      WelchCollector welch) {
    Simulation sim = new Simulation();
    Metrics metrics = new Metrics();
    Server server = new Server(cores, maxThreads, quantumMs, ctxSwitchMs);

    Rng root = new Rng(seed, 0L);
    Distribution thinkDist = DistributionParser.parse(thinkSpec, root.withStream(1L));
    Distribution serviceDist = DistributionParser.parse(serviceSpec, root.withStream(2L));
    Distribution timeoutDist = DistributionParser.parse(timeoutSpec, root.withStream(3L));

    User[] userArr = new User[users];
    for (int i = 0; i < users; i++) {
      userArr[i] = new User(i);
    }

    SimState state = new SimState(thinkDist, serviceDist, timeoutDist, server, userArr, metrics, trace, welch);

    if (warmupMs > 0.0) {
      sim.schedule(new WarmupResetEvent(warmupMs, state));
    } else {
      metrics.startMeasurement(0.0);
      server.resetUtilization(0.0);
      trace.onWarmupReset(0.0);
    }

    for (User u : userArr) {
      u.scheduleInitial(state, sim);
    }

    double endMs = warmupMs + measureMs;
    sim.runUntil(endMs);
    server.finishUtilization(endMs);

    double seconds = measureMs / 1_000.0;
    double goodputRps = metrics.goodCompleted() / seconds;
    double badputRps = metrics.badCompleted() / seconds;
    double throughputRps = (metrics.goodCompleted() + metrics.badCompleted()) / seconds;
    double timeoutRps = metrics.timedOut() / seconds;

    double utilSum = 0.0;
    for (var c : server.cores) {
      utilSum += c.busyTimeMs() / measureMs;
    }
    double avgCoreUtil = utilSum / server.cores.length;

    return new ReplicationResult(
        users,
        replication,
        seed,
        measureMs,
        metrics.issued(),
        metrics.goodCompleted(),
        metrics.badCompleted(),
        metrics.timedOut(),
        metrics.goodRespCount(),
        metrics.goodRespMeanMs(),
        metrics.clientRespCount(),
        metrics.clientRespMeanMs(),
        goodputRps,
        badputRps,
        throughputRps,
        timeoutRps,
        avgCoreUtil);
  }
}

