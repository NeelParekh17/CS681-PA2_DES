package des.rng;

import java.util.SplittableRandom;

/** Deterministic RNG wrapper supporting independently mixed logical streams. */
public final class Rng {
  /** Mixing constant used to separate stream identities. */
  private static final long STREAM_MIX = 0x9E3779B97F4A7C15L;

  /** Root seed shared by all derived streams. */
  private final long rootSeed;
  /** Logical stream id mixed with root seed. */
  private final long streamId;
  /** Backing random generator for this stream. */
  private final SplittableRandom random;

  /** Creates default stream (id 0) from root seed. */
  public Rng(long rootSeed) {
    this(rootSeed, 0L);
  }

  /** Creates a specific logical stream from root seed and stream id. */
  public Rng(long rootSeed, long streamId) {
    this.rootSeed = rootSeed;
    this.streamId = streamId;
    long mixedSeed = mix64(rootSeed ^ (streamId * STREAM_MIX));
    this.random = new SplittableRandom(mixedSeed);
  }

  /** Returns root seed value. */
  public long rootSeed() {
    return rootSeed;
  }

  /** Returns logical stream id. */
  public long streamId() {
    return streamId;
  }

  /** Creates a sibling RNG stream with same root seed and different stream id. */
  public Rng withStream(long newStreamId) {
    return new Rng(rootSeed, newStreamId);
  }

  /** Returns a U(0,1) sample while avoiding exact zero. */
  public double nextDouble() {
    double v = random.nextDouble();
    return v == 0.0 ? Double.MIN_VALUE : v;
  }

  /** Returns bounded integer sample. */
  public int nextInt(int bound) {
    return random.nextInt(bound);
  }

  /** Returns 64-bit integer sample. */
  public long nextLong() {
    return random.nextLong();
  }

  /** Bit-mixing helper to decorrelate nearby seed/stream combinations. */
  private static long mix64(long z) {
    z = (z ^ (z >>> 33)) * 0xff51afd7ed558ccdL;
    z = (z ^ (z >>> 33)) * 0xc4ceb9fe1a85ec53L;
    return z ^ (z >>> 33);
  }
}

