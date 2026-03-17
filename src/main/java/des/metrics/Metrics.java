package des.metrics;

import des.model.Request;

public final class Metrics {
  private boolean measuring = false;
  private double measurementStartMs = Double.NaN;

  private long issued = 0L;
  private long goodCompleted = 0L;
  private long badCompleted = 0L;
  private long timedOut = 0L;
  private long dropped = 0L;

  private int currentWaitQ = 0;
  private int maxWaitQ = 0;
  private double waitQAreaMs = 0.0;
  private double waitQLastTsMs = Double.NaN;
  private boolean waitQTracking = false;

  private double sumGoodRespMs = 0.0;
  private long goodRespCount = 0L;

  private double sumClientRespMs = 0.0;
  private long clientRespCount = 0L;

  public boolean isMeasuring() {
    return measuring;
  }

  public double measurementStartMs() {
    return measurementStartMs;
  }

  public void startMeasurement(double nowMs) {
    measuring = true;
    measurementStartMs = nowMs;

    issued = 0L;
    goodCompleted = 0L;
    badCompleted = 0L;
    timedOut = 0L;
    dropped = 0L;

    currentWaitQ = 0;
    maxWaitQ = 0;
    waitQAreaMs = 0.0;
    waitQLastTsMs = nowMs;
    waitQTracking = true;

    sumGoodRespMs = 0.0;
    goodRespCount = 0L;

    sumClientRespMs = 0.0;
    clientRespCount = 0L;
  }

  public void onIssue(Request r) {
    if (!measuring) {
      r.measuredIssue = false;
      return;
    }
    issued++;
    r.measuredIssue = true;
  }

  public void onTimeout(Request r, double nowMs) {
    if (!measuring) {
      return;
    }
    timedOut++;
    if (r.measuredIssue) {
      sumClientRespMs += nowMs - r.issueTimeMs;
      clientRespCount++;
    }
  }

  public void onDrop(Request r, double nowMs) {
    if (!measuring) {
      return;
    }
    dropped++;
    if (r.measuredIssue) {
      sumClientRespMs += nowMs - r.issueTimeMs;
      clientRespCount++;
    }
  }

  public void onGoodCompletion(Request r, double nowMs) {
    if (!measuring) {
      return;
    }
    goodCompleted++;
    if (r.measuredIssue) {
      double respMs = nowMs - r.issueTimeMs;
      sumGoodRespMs += respMs;
      goodRespCount++;

      sumClientRespMs += respMs;
      clientRespCount++;
    }
  }

  public void onBadCompletion(Request r, double nowMs) {
    if (!measuring) {
      return;
    }
    badCompleted++;
  }

  public void observeWaitQueue(double nowMs, int waitQ) {
    if (!waitQTracking || Double.isNaN(waitQLastTsMs)) {
      currentWaitQ = Math.max(0, waitQ);
      maxWaitQ = Math.max(maxWaitQ, currentWaitQ);
      waitQLastTsMs = nowMs;
      return;
    }

    double dt = Math.max(0.0, nowMs - waitQLastTsMs);
    waitQAreaMs += dt * currentWaitQ;
    waitQLastTsMs = nowMs;

    currentWaitQ = Math.max(0, waitQ);
    if (currentWaitQ > maxWaitQ) {
      maxWaitQ = currentWaitQ;
    }
  }

  public void finishWaitQueueTracking(double endMs) {
    if (!waitQTracking || Double.isNaN(waitQLastTsMs)) {
      return;
    }
    double dt = Math.max(0.0, endMs - waitQLastTsMs);
    waitQAreaMs += dt * currentWaitQ;
    waitQLastTsMs = endMs;
  }

  public long issued() {
    return issued;
  }

  public long goodCompleted() {
    return goodCompleted;
  }

  public long badCompleted() {
    return badCompleted;
  }

  public long timedOut() {
    return timedOut;
  }

  public long dropped() {
    return dropped;
  }

  public int maxWaitQ() {
    return maxWaitQ;
  }

  public double avgWaitQ(double measureMs) {
    return measureMs <= 0.0 ? Double.NaN : (waitQAreaMs / measureMs);
  }

  public long goodRespCount() {
    return goodRespCount;
  }

  public double goodRespMeanMs() {
    return goodRespCount == 0L ? Double.NaN : (sumGoodRespMs / goodRespCount);
  }

  public long clientRespCount() {
    return clientRespCount;
  }

  public double clientRespMeanMs() {
    return clientRespCount == 0L ? Double.NaN : (sumClientRespMs / clientRespCount);
  }
}

