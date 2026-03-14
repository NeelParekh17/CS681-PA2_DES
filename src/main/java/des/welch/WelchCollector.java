package des.welch;

import java.io.BufferedWriter;
import java.io.IOException;
import java.io.PrintWriter;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Locale;

public final class WelchCollector {
  private final double binMs;
  private final int bins;
  private final double[] sumRespMs;
  private final int[] count;

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

  public void writeCsv(Path path) throws IOException {
    Files.createDirectories(path.toAbsolutePath().getParent());
    try (PrintWriter out = new PrintWriter(new BufferedWriter(Files.newBufferedWriter(path)))) {
      out.println("bin_end_ms,mean_resp_ms,count");
      for (int i = 0; i < bins; i++) {
        out.printf(Locale.ROOT, "%.3f,%.6f,%d%n", binEndMs(i), meanAt(i), count[i]);
      }
    }
  }

  public double binMs() {
    return binMs;
  }

  public int bins() {
    return bins;
  }

  public double binEndMs(int idx) {
    return (idx + 1) * binMs;
  }

  public int countAt(int idx) {
    return count[idx];
  }

  public double meanAt(int idx) {
    return count[idx] == 0 ? Double.NaN : (sumRespMs[idx] / count[idx]);
  }

  public double sumRespMsAt(int idx) {
    return sumRespMs[idx];
  }
}

