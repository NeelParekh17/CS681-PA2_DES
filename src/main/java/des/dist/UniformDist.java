package des.dist;

import des.rng.Rng;

public final class UniformDist implements Distribution {
  private final double minMs;
  private final double maxMs;
  private final Rng rng;

  public UniformDist(double minMs, double maxMs, Rng rng) {
    if (minMs < 0.0 || maxMs < 0.0 || maxMs < minMs) {
      throw new IllegalArgumentException("uniform requires 0 <= min <= max");
    }
    this.minMs = minMs;
    this.maxMs = maxMs;
    this.rng = rng;
  }

  @Override
  public double sampleMs() {
    if (minMs == maxMs) {
      return minMs;
    }
    return minMs + rng.nextDouble() * (maxMs - minMs);
  }
}

