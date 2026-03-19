package des.dist;

import des.rng.Rng;

/** Triangular distribution with configurable min/mode/max in milliseconds. */
public final class TriangularDist implements Distribution {
  /** Lower bound of support. */
  private final double minMs;
  /** Most likely value (peak). */
  private final double modeMs;
  /** Upper bound of support. */
  private final double maxMs;
  /** RNG stream for this distribution. */
  private final Rng rng;

  /** Validates shape constraints and stores parameters. */
  public TriangularDist(double minMs, double modeMs, double maxMs, Rng rng) {
    if (minMs < 0.0 || maxMs < 0.0 || !(minMs <= modeMs && modeMs <= maxMs)) {
      throw new IllegalArgumentException("triangular requires 0 <= min <= mode <= max");
    }
    this.minMs = minMs;
    this.modeMs = modeMs;
    this.maxMs = maxMs;
    this.rng = rng;
  }

  @Override
  /** Samples using triangular inverse-CDF split at mode fraction c. */
  public double sampleMs() {
    if (minMs == maxMs) {
      return minMs;
    }
    double u = rng.nextDouble();
    double c = (modeMs - minMs) / (maxMs - minMs);
    if (u < c) {
      return minMs + Math.sqrt(u * (maxMs - minMs) * (modeMs - minMs));
    }
    return maxMs - Math.sqrt((1.0 - u) * (maxMs - minMs) * (maxMs - modeMs));
  }
}

