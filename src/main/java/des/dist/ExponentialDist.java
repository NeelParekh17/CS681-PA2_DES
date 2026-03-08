package des.dist;

import des.rng.Rng;

public final class ExponentialDist implements Distribution {
  private final double meanMs;
  private final Rng rng;

  public ExponentialDist(double meanMs, Rng rng) {
    if (meanMs <= 0.0) {
      throw new IllegalArgumentException("exponential mean must be > 0");
    }
    this.meanMs = meanMs;
    this.rng = rng;
  }

  @Override
  public double sampleMs() {
    return -meanMs * Math.log(rng.nextDouble());
  }
}

