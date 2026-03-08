package des.model;

public final class SimThread {
  public final int id;
  public final int coreId;
  public Request current = null;

  public SimThread(int id, int coreId) {
    this.id = id;
    this.coreId = coreId;
  }

  public boolean isIdle() {
    return current == null;
  }
}

