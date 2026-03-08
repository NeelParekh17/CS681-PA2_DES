package des.experiment;

import des.config.SimConfig;
import des.metrics.ReplicationResult;
import des.run.Simulator;
import des.stats.ConfidenceIntervals;
import des.trace.TraceLogger;
import java.io.BufferedWriter;
import java.io.IOException;
import java.io.PrintWriter;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Locale;

public final class ExperimentRunner {
  private ExperimentRunner() {}

  public static void run(
      SimConfig cfg,
      int[] userCounts,
      int replications,
      long baseSeed,
      Path summaryCsv,
      TraceLogger trace)
      throws IOException {
    double warmupMs = cfg.getTimeMs("sim.warmup");
    double measureMs = cfg.getTimeMs("sim.measure");

    int cores = cfg.getInt("sim.cores");
    int maxThreads = cfg.getInt("sim.maxThreads");
    double quantumMs = cfg.getTimeMs("sim.quantum");
    double ctxSwitchMs = cfg.getTimeMs("sim.ctxSwitch");

    String thinkSpec = cfg.getString("dist.think");
    String serviceSpec = cfg.getString("dist.service");
    String timeoutSpec = cfg.getString("dist.timeout");

    Path summaryAbs = summaryCsv.toAbsolutePath();
    Path parent = summaryAbs.getParent();
    if (parent != null) {
      Files.createDirectories(parent);
    }
    Path replicationsCsv =
        (parent == null ? Path.of("replications.csv") : parent.resolve("replications.csv"));

    try (PrintWriter repOut =
            new PrintWriter(new BufferedWriter(Files.newBufferedWriter(replicationsCsv)));
        PrintWriter sumOut =
            new PrintWriter(new BufferedWriter(Files.newBufferedWriter(summaryAbs)))) {
      repOut.println(
          "users,replication,seed,measure_ms,issued,good_completed,bad_completed,timed_out,"
              + "good_resp_count,good_resp_mean_ms,client_resp_count,client_resp_mean_ms,"
              + "goodput_rps,badput_rps,throughput_rps,timeout_rps,avg_core_util");
      sumOut.println(
          "users,replications,measure_ms,"
              + "good_resp_mean_ms,good_resp_ci_low_ms,good_resp_ci_high_ms,good_resp_ci_n,"
              + "goodput_rps_mean,badput_rps_mean,throughput_rps_mean,timeout_rps_mean,avg_core_util_mean");

      for (int users : userCounts) {
        ArrayList<ReplicationResult> results = new ArrayList<>(replications);
        for (int r = 0; r < replications; r++) {
          long seed = deriveSeed(baseSeed, users, r);
          ReplicationResult res =
              Simulator.runReplication(
                  users,
                  r,
                  seed,
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
          results.add(res);
          writeReplicationRow(repOut, res);
        }

        writeSummaryRow(sumOut, users, measureMs, results);
      }
    }
  }

  private static long deriveSeed(long baseSeed, int users, int replication) {
    long a = 0x9E3779B97F4A7C15L * (long) users;
    long b = 0xBF58476D1CE4E5B9L * (long) replication;
    return baseSeed ^ a ^ b;
  }

  private static void writeReplicationRow(PrintWriter out, ReplicationResult r) {
    out.printf(
        Locale.ROOT,
        "%d,%d,%d,%.3f,%d,%d,%d,%d,%d,%.6f,%d,%.6f,%.6f,%.6f,%.6f,%.6f,%.6f%n",
        r.users(),
        r.replication(),
        r.seed(),
        r.measureMs(),
        r.issued(),
        r.goodCompleted(),
        r.badCompleted(),
        r.timedOut(),
        r.goodRespCount(),
        r.goodRespMeanMs(),
        r.clientRespCount(),
        r.clientRespMeanMs(),
        r.goodputRps(),
        r.badputRps(),
        r.throughputRps(),
        r.timeoutRps(),
        r.avgCoreUtil());
  }

  private static void writeSummaryRow(
      PrintWriter out, int users, double measureMs, ArrayList<ReplicationResult> results) {
    double[] goodRespMeans = new double[results.size()];
    for (int i = 0; i < results.size(); i++) {
      goodRespMeans[i] = results.get(i).goodRespMeanMs();
    }
    ConfidenceIntervals.Result ci = ConfidenceIntervals.ci95Mean(goodRespMeans);

    double goodputMean = mean(results, Metric.GOODPUT);
    double badputMean = mean(results, Metric.BADPUT);
    double throughputMean = mean(results, Metric.THROUGHPUT);
    double timeoutMean = mean(results, Metric.TIMEOUT);
    double utilMean = mean(results, Metric.UTIL);

    out.printf(
        Locale.ROOT,
        "%d,%d,%.3f,%.6f,%.6f,%.6f,%d,%.6f,%.6f,%.6f,%.6f,%.6f%n",
        users,
        results.size(),
        measureMs,
        ci.mean(),
        ci.low(),
        ci.high(),
        ci.n(),
        goodputMean,
        badputMean,
        throughputMean,
        timeoutMean,
        utilMean);
  }

  private enum Metric {
    GOODPUT,
    BADPUT,
    THROUGHPUT,
    TIMEOUT,
    UTIL
  }

  private static double mean(ArrayList<ReplicationResult> results, Metric m) {
    double sum = 0.0;
    int n = 0;
    for (ReplicationResult r : results) {
      double v =
          switch (m) {
            case GOODPUT -> r.goodputRps();
            case BADPUT -> r.badputRps();
            case THROUGHPUT -> r.throughputRps();
            case TIMEOUT -> r.timeoutRps();
            case UTIL -> r.avgCoreUtil();
          };
      if (!Double.isFinite(v)) {
        continue;
      }
      sum += v;
      n++;
    }
    return n == 0 ? Double.NaN : (sum / n);
  }
}
