package des.welch;

import java.io.BufferedWriter;
import java.io.IOException;
import java.io.PrintWriter;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Locale;

/** Collects per-time-bin response samples for Welch warm-up analysis. */
public final class WelchCollector {
  /** Bin width in milliseconds. */
  private final double binMs;
  /** Number of bins spanning configured duration. */
  private final int bins;
  /** Sum of response times per bin. */
  private final double[] sumRespMs;
  /** Number of samples per bin. */
  private final int[] count;

  /** Creates empty accumulator arrays for the configured Welch window. */
  public WelchCollector(double durationMs, double binMs) {
    if (durationMs <= 0.0) {
      throw new IllegalArgumentException("duration must be > 0");
    }
    if (binMs <= 0.0) {
      throw new IllegalArgumentException("bin must be > 0");
    }
    this.binMs = binMs;
    this.bins = (int) Math.ceil(durationMs / binMs);
    this.sumRespMs = new double[bins];
    this.count = new int[bins];
  }

  /** Records one completion response into its completion-time bin. */
  public void record(double completionTimeMs, double respMs) {
    if (completionTimeMs < 0.0) {
      return;
    }
    int idx = (int) (completionTimeMs / binMs);
    if (idx < 0 || idx >= bins) {
      return;
    }
    sumRespMs[idx] += respMs;
    count[idx] += 1;
  }

  /** Writes bin-end, mean, and count columns to CSV. */
  public void writeCsv(Path path) throws IOException {
    Files.createDirectories(path.toAbsolutePath().getParent());
    try (PrintWriter out = new PrintWriter(new BufferedWriter(Files.newBufferedWriter(path)))) {
      out.println("bin_end_ms,mean_resp_ms,count");
      for (int i = 0; i < bins; i++) {
        out.printf(Locale.ROOT, "%.3f,%.6f,%d%n", binEndMs(i), meanAt(i), count[i]);
      }
    }
  }

  /** Returns configured bin width in milliseconds. */
  public double binMs() {
    return binMs;
  }

  /** Returns number of bins. */
  public int bins() {
    return bins;
  }

  /** Returns end timestamp of a bin index. */
  public double binEndMs(int idx) {
    return (idx + 1) * binMs;
  }

  /** Returns sample count for a bin index. */
  public int countAt(int idx) {
    return count[idx];
  }

  /** Returns mean response for a bin index or NaN when empty. */
  public double meanAt(int idx) {
    return count[idx] == 0 ? Double.NaN : (sumRespMs[idx] / count[idx]);
  }

  /** Returns raw response-time sum for a bin index. */
  public double sumRespMsAt(int idx) {
    return sumRespMs[idx];
  }
}

