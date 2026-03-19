package des.dist;

/** Adds a constant offset to samples produced by an inner distribution. */
public final class ShiftedDist implements Distribution {
  /** Fixed shift added to every inner sample. */
  private final double offsetMs;
  /** Wrapped distribution that provides the variable component. */
  private final Distribution inner;

  /** Builds a shifted distribution from offset and inner distribution. */
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
  /** Returns offset + inner sample. */
  public double sampleMs() {
    return offsetMs + inner.sampleMs();
  }
}

