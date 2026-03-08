package des.dist;

public final class ConstantDist implements Distribution {
  private final double valueMs;

  public ConstantDist(double valueMs) {
    if (valueMs < 0.0) {
      throw new IllegalArgumentException("constant must be >= 0");
    }
    this.valueMs = valueMs;
  }

  @Override
  public double sampleMs() {
    return valueMs;
  }
}

