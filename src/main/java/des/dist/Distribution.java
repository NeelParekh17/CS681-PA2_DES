package des.dist;

/** Common interface for all sampled time distributions used by the simulator. */
public interface Distribution {
  /** Draws one sample value in milliseconds. */
  double sampleMs();
}

