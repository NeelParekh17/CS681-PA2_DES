package des;

import des.config.SimConfig;
import des.experiment.ExperimentRunner;
import des.run.Simulator;
import des.trace.TraceLogger;
import des.welch.WelchCollector;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.Locale;

public final class Main {
  private Main() {}

  public static void main(String[] args) throws Exception {
    HashMap<String, String> flags = parseArgs(args);
    if (flags.containsKey("help") || !flags.containsKey("config") || !flags.containsKey("mode")) {
      printUsage();
      return;
    }

    String mode = flags.get("mode");
    Path configPath = Path.of(flags.get("config"));
    SimConfig cfg = SimConfig.load(configPath);

    long seedOverride = flags.containsKey("seed") ? Long.parseLong(flags.get("seed")) : Long.MIN_VALUE;
    Integer usersOverride = flags.containsKey("users") ? Integer.parseInt(flags.get("users")) : null;
    Integer repOverride = flags.containsKey("replications") ? Integer.parseInt(flags.get("replications")) : null;
    Path outPath = Path.of(flags.getOrDefault("out", defaultOut(mode)));

    switch (mode) {
      case "demo" -> runDemo(cfg, outPath, seedOverride, usersOverride);
      case "experiment" -> runExperiment(cfg, outPath, seedOverride, usersOverride, repOverride);
      case "welch" -> runWelch(cfg, outPath, seedOverride, usersOverride);
      default -> throw new IllegalArgumentException("Unknown --mode: " + mode);
    }
  }

  private static void runDemo(SimConfig cfg, Path outPath, long seedOverride, Integer usersOverride)
      throws Exception {
    int users = usersOverride != null ? usersOverride : cfg.getInt("sim.users");

    long baseSeed =
        seedOverride != Long.MIN_VALUE
            ? seedOverride
            : cfg.getLong("sim.seed", cfg.getLong("experiment.baseSeed", 12345L));

    double warmupMs = cfg.getTimeMs("sim.warmup");
    double measureMs = cfg.getTimeMs("sim.measure");

    int cores = cfg.getInt("sim.cores");
    int maxThreads = cfg.getInt("sim.maxThreads");
    double quantumMs = cfg.getTimeMs("sim.quantum");
    double ctxSwitchMs = cfg.getTimeMs("sim.ctxSwitch");

    String thinkSpec = cfg.getString("dist.think");
    String serviceSpec = cfg.getString("dist.service");
    String timeoutSpec = cfg.getString("dist.timeout");

    boolean traceEnabled = cfg.getBool("trace.enabled", true);
    TraceLogger.Level level =
        TraceLogger.Level.valueOf(cfg.getString("trace.level", "VERBOSE").trim().toUpperCase(Locale.ROOT));
    long maxLines = (long) cfg.getInt("trace.maxLines", 10_000);

    try (TraceLogger trace = TraceLogger.create(traceEnabled, level, maxLines, outPath)) {
      var res =
          Simulator.runReplication(
              users,
              0,
              baseSeed,
              cores,
              maxThreads,
              quantumMs,
              ctxSwitchMs,
              warmupMs,
              measureMs,
              thinkSpec,
              serviceSpec,
              timeoutSpec,
              trace,
              null);
      System.out.printf(
          Locale.ROOT,
          "Demo done. users=%d goodput=%.3f rps badput=%.3f rps timeout=%.3f rps good_rt=%.3f ms%n",
          users,
          res.goodputRps(),
          res.badputRps(),
          res.timeoutRps(),
          res.goodRespMeanMs());
    }
  }

  private static void runExperiment(
      SimConfig cfg,
      Path summaryCsv,
      long seedOverride,
      Integer usersOverride,
      Integer repOverride)
      throws Exception {
    int[] userCounts =
        usersOverride != null
            ? new int[] {usersOverride}
            : cfg.getUserCounts("experiment.userCounts", new int[] {cfg.getInt("sim.users", 1)});

    int replications = repOverride != null ? repOverride : cfg.getInt("experiment.replications");
    long baseSeed =
        seedOverride != Long.MIN_VALUE
            ? seedOverride
            : cfg.getLong("experiment.baseSeed", cfg.getLong("sim.seed", 12345L));

    boolean traceEnabled = cfg.getBool("trace.enabled", false);
    TraceLogger.Level level =
        TraceLogger.Level.valueOf(cfg.getString("trace.level", "SUMMARY").trim().toUpperCase(Locale.ROOT));
    long maxLines = (long) cfg.getInt("trace.maxLines", 0);
    try (TraceLogger trace = TraceLogger.create(traceEnabled, level, maxLines, Path.of("out/trace_experiment.txt"))) {
      ExperimentRunner.run(cfg, userCounts, replications, baseSeed, summaryCsv, trace);
    }
    System.out.println("Wrote " + summaryCsv.toAbsolutePath());
    Path parent = summaryCsv.toAbsolutePath().getParent();
    System.out.println("Wrote " + (parent == null ? Path.of("replications.csv") : parent.resolve("replications.csv")));
  }

  private static void runWelch(SimConfig cfg, Path outPath, long seedOverride, Integer usersOverride)
      throws Exception {
    int users = usersOverride != null ? usersOverride : cfg.getInt("welch.users", cfg.getInt("sim.users", 1));

    long seed =
        seedOverride != Long.MIN_VALUE
            ? seedOverride
            : cfg.getLong("sim.seed", cfg.getLong("experiment.baseSeed", 12345L));

    int cores = cfg.getInt("sim.cores");
    int maxThreads = cfg.getInt("sim.maxThreads");
    double quantumMs = cfg.getTimeMs("sim.quantum");
    double ctxSwitchMs = cfg.getTimeMs("sim.ctxSwitch");

    String thinkSpec = cfg.getString("dist.think");
    String serviceSpec = cfg.getString("dist.service");
    String timeoutSpec = cfg.getString("dist.timeout");

    double durationMs = cfg.getTimeMs("welch.duration", cfg.getTimeMs("sim.measure"));
    double binMs = cfg.getTimeMs("welch.bin", 100.0);
    WelchCollector welch = new WelchCollector(durationMs, binMs);

    try (TraceLogger trace = TraceLogger.disabled()) {
      Simulator.runReplication(
          users,
          0,
          seed,
          cores,
          maxThreads,
          quantumMs,
          ctxSwitchMs,
          0.0,
          durationMs,
          thinkSpec,
          serviceSpec,
          timeoutSpec,
          trace,
          welch);
    }

    welch.writeCsv(outPath);
    System.out.println("Wrote " + outPath.toAbsolutePath());
  }

  private static String defaultOut(String mode) {
    return switch (mode) {
      case "demo" -> "out/trace.txt";
      case "experiment" -> "out/summary.csv";
      case "welch" -> "out/welch.csv";
      default -> "out/out.txt";
    };
  }

  private static void printUsage() {
    System.out.println(
        """
Usage:
  java -cp build/classes des.Main --mode demo|experiment|welch --config <path> [--out <path>]
        [--seed <n>] [--users <n>] [--replications <n>]

Config keys (minimum):
  sim.cores, sim.maxThreads, sim.quantum, sim.ctxSwitch
  sim.warmup, sim.measure
  dist.think, dist.service, dist.timeout

Experiment keys:
  experiment.userCounts (e.g. 5:50:5 or 10,20,30), experiment.replications, experiment.baseSeed

Trace keys (demo):
  trace.enabled, trace.level (SUMMARY|VERBOSE), trace.maxLines
""");
  }

  private static HashMap<String, String> parseArgs(String[] args) {
    HashMap<String, String> out = new HashMap<>();
    for (int i = 0; i < args.length; i++) {
      String a = args[i];
      if (a.equals("--help") || a.equals("-h")) {
        out.put("help", "1");
        continue;
      }
      if (!a.startsWith("--")) {
        throw new IllegalArgumentException("Unexpected arg: " + a);
      }
      String key = a.substring(2);
      if (i + 1 >= args.length) {
        throw new IllegalArgumentException("Missing value for: " + a);
      }
      String val = args[++i];
      out.put(key, val);
    }
    return out;
  }
}
