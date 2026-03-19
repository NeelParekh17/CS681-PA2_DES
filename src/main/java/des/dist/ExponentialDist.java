package des.dist;

import des.rng.Rng;

/** Exponential distribution parameterized by mean service time in milliseconds. */
public final class ExponentialDist implements Distribution {
  /** Mean of the exponential distribution in milliseconds. */
  private final double meanMs;
  /** RNG stream dedicated to this distribution. */
  private final Rng rng;

  /** Validates parameters and captures RNG stream. */
  public ExponentialDist(double meanMs, Rng rng) {
    if (meanMs <= 0.0) {
      throw new IllegalArgumentException("exponential mean must be > 0");
    }
    this.meanMs = meanMs;
    this.rng = rng;
  }

  @Override
  /** Samples using inverse CDF: x = -mean * ln(U). */
  public double sampleMs() {
    return -meanMs * Math.log(rng.nextDouble());
  }
}

