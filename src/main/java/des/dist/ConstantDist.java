package des.dist;

/** Distribution that always returns the same fixed sample in milliseconds. */
public final class ConstantDist implements Distribution {
  /** Constant sample value returned for every draw. */
  private final double valueMs;

  /** Validates and stores constant sample value. */
  public ConstantDist(double valueMs) {
    if (valueMs < 0.0) {
      throw new IllegalArgumentException("constant must be >= 0");
    }
    this.valueMs = valueMs;
  }

  @Override
  /** Returns the configured constant value. */
  public double sampleMs() {
    return valueMs;
  }
}

