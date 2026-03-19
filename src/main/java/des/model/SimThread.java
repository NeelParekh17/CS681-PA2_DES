package des.model;

/** Thread-pool worker bound to a fixed core for the lifetime of a replication. */
public final class SimThread {
  /** Stable thread identifier. */
  public final int id;
  /** Affined core id for this worker. */
  public final int coreId;
  /** Currently assigned request, null means idle. */
  public Request current = null;

  /** Creates thread metadata and fixed core affinity. */
  public SimThread(int id, int coreId) {
    this.id = id;
    this.coreId = coreId;
  }

  /** Returns true when worker has no assigned request. */
  public boolean isIdle() {
    return current == null;
  }
}

