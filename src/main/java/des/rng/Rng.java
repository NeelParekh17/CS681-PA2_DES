package des.rng;

import java.util.SplittableRandom;

public final class Rng {
  private static final long STREAM_MIX = 0x9E3779B97F4A7C15L;

  private final long rootSeed;
  private final long streamId;
  private final SplittableRandom random;

  public Rng(long rootSeed) {
    this(rootSeed, 0L);
  }

  public Rng(long rootSeed, long streamId) {
    this.rootSeed = rootSeed;
    this.streamId = streamId;
    long mixedSeed = mix64(rootSeed ^ (streamId * STREAM_MIX));
    this.random = new SplittableRandom(mixedSeed);
  }

  public long rootSeed() {
    return rootSeed;
  }

  public long streamId() {
    return streamId;
  }

  public Rng withStream(long newStreamId) {
    return new Rng(rootSeed, newStreamId);
  }

  public double nextDouble() {
    double v = random.nextDouble();
    return v == 0.0 ? Double.MIN_VALUE : v;
  }

  public int nextInt(int bound) {
    return random.nextInt(bound);
  }

  public long nextLong() {
    return random.nextLong();
  }

  private static long mix64(long z) {
    z = (z ^ (z >>> 33)) * 0xff51afd7ed558ccdL;
    z = (z ^ (z >>> 33)) * 0xc4ceb9fe1a85ec53L;
    return z ^ (z >>> 33);
  }
}

