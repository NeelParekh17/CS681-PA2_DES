package des.dist;

import des.rng.Rng;

/** Uniform distribution over a closed-open interval in milliseconds. */
public final class UniformDist implements Distribution {
  /** Minimum value. */
  private final double minMs;
  /** Maximum value. */
  private final double maxMs;
  /** RNG stream for this distribution. */
  private final Rng rng;

  /** Validates bounds and captures RNG stream. */
  public UniformDist(double minMs, double maxMs, Rng rng) {
    if (minMs < 0.0 || maxMs < 0.0 || maxMs < minMs) {
      throw new IllegalArgumentException("uniform requires 0 <= min <= max");
    }
    this.minMs = minMs;
    this.maxMs = maxMs;
    this.rng = rng;
  }

  @Override
  /** Samples uniformly from [minMs, maxMs]. */
  public double sampleMs() {
    if (minMs == maxMs) {
      return minMs;
    }
    return minMs + rng.nextDouble() * (maxMs - minMs);
  }
}

