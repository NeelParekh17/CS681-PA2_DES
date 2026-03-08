package des.dist;

import des.rng.Rng;

public final class TriangularDist implements Distribution {
  private final double minMs;
  private final double modeMs;
  private final double maxMs;
  private final Rng rng;

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

