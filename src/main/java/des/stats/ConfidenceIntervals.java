package des.stats;

import java.util.ArrayList;

/** Statistical helper for 95% confidence intervals on replication means. */
public final class ConfidenceIntervals {
  /** Utility holder. */
  private ConfidenceIntervals() {}

  /** t(0.975, df) lookup table for df in [1, 30]. */
  private static final double[] T_975_DF_1_TO_30 = {
    Double.NaN,
    12.706, 4.303, 3.182, 2.776, 2.571, 2.447, 2.365, 2.306, 2.262, 2.228,
    2.201, 2.179, 2.160, 2.145, 2.131, 2.120, 2.110, 2.101, 2.093, 2.086,
    2.080, 2.074, 2.069, 2.064, 2.060, 2.056, 2.052, 2.048, 2.045, 2.042
  };

  /** Computes mean and 95% confidence interval for finite sample values. */
  public static Result ci95Mean(double[] samples) {
    ArrayList<Double> vals = new ArrayList<>(samples.length);
    for (double v : samples) {
      if (Double.isFinite(v)) {
        vals.add(v);
      }
    }
    int n = vals.size();
    if (n == 0) {
      return new Result(Double.NaN, Double.NaN, Double.NaN, 0);
    }
    double mean = 0.0;
    for (double v : vals) {
      mean += v;
    }
    mean /= n;

    if (n == 1) {
      return new Result(mean, Double.NaN, Double.NaN, 1);
    }

    double s2 = 0.0;
    for (double v : vals) {
      double d = v - mean;
      s2 += d * d;
    }
    double var = s2 / (n - 1);
    double sd = Math.sqrt(var);
    double t = tCritical975(n - 1);
    double half = t * sd / Math.sqrt(n);
    return new Result(mean, mean - half, mean + half, n);
  }

  /** Returns two-sided 95% t critical value for given degrees of freedom. */
  private static double tCritical975(int df) {
    if (df <= 0) {
      return Double.NaN;
    }
    if (df <= 30) {
      return T_975_DF_1_TO_30[df];
    }
    return 1.96;
  }

  /** CI result container: center mean, lower/upper bound, and effective sample size. */
  public record Result(double mean, double low, double high, int n) {}
}

