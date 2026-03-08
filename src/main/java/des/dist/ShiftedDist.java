package des.dist;

public final class ShiftedDist implements Distribution {
  private final double offsetMs;
  private final Distribution inner;

  public ShiftedDist(double offsetMs, Distribution inner) {
    if (offsetMs < 0.0) {
      throw new IllegalArgumentException("shift offset must be >= 0");
    }
    if (inner == null) {
      throw new IllegalArgumentException("inner distribution is null");
    }
    this.offsetMs = offsetMs;
    this.inner = inner;
  }

  @Override
  public double sampleMs() {
    return offsetMs + inner.sampleMs();
  }
}

