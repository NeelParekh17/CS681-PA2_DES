package des.metrics;

import des.model.Request;

/**
 * Per-replication metric accumulator used during the measurement window.
 *
 * <p>Tracks completions, failures, queue area, and response-time summaries.
 */
public final class Metrics {
  /** True when events should contribute to metric accumulation. */
  private boolean measuring = false;
  /** Simulation timestamp where measurement window began. */
  private double measurementStartMs = Double.NaN;

  /** Requests issued during measurement window. */
  private long issued = 0L;
  /** Completed before timeout. */
  private long goodCompleted = 0L;
  /** Completed after timeout already fired. */
  private long badCompleted = 0L;
  /** Timeouts observed by users. */
  private long timedOut = 0L;
  /** Drops due to full queue / admission failure. */
  private long dropped = 0L;

  /** Instantaneous wait queue size at last observation point. */
  private int currentWaitQ = 0;
  /** Maximum observed wait queue size. */
  private int maxWaitQ = 0;
  /** Time integral of wait queue length (for average queue length). */
  private double waitQAreaMs = 0.0;
  /** Last timestamp where queue area integration was updated. */
  private double waitQLastTsMs = Double.NaN;
  /** Whether queue area integration is active. */
  private boolean waitQTracking = false;

  /** Sum of good response times in ms. */
  private double sumGoodRespMs = 0.0;
  /** Number of good responses contributing to good mean. */
  private long goodRespCount = 0L;

  /** Sum of client-visible response times (good + timeout + drop). */
  private double sumClientRespMs = 0.0;
  /** Number of client-visible responses in mean denominator. */
  private long clientRespCount = 0L;

  /** Returns whether measurement mode is active. */
  public boolean isMeasuring() {
    return measuring;
  }

  /** Returns measurement start timestamp. */
  public double measurementStartMs() {
    return measurementStartMs;
  }

  /** Resets all counters at warmup boundary and starts measurement mode. */
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

  /** Records request issue event if request belongs to measurement window. */
  public void onIssue(Request r) {
    if (!measuring) {
      r.measuredIssue = false;
      return;
    }
    issued++;
    r.measuredIssue = true;
  }

  /** Records timeout and client-visible response contribution. */
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

  /** Records drop and client-visible response contribution. */
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

  /** Records good completion for both good and client-visible response metrics. */
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

  /** Records bad completion count (completed after user already timed out). */
  public void onBadCompletion(Request r, double nowMs) {
    if (!measuring) {
      return;
    }
    badCompleted++;
  }

  /** Integrates wait-queue area over time for average queue-size calculation. */
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

  /** Flushes final queue-area integration segment at simulation end. */
  public void finishWaitQueueTracking(double endMs) {
    if (!waitQTracking || Double.isNaN(waitQLastTsMs)) {
      return;
    }
    double dt = Math.max(0.0, endMs - waitQLastTsMs);
    waitQAreaMs += dt * currentWaitQ;
    waitQLastTsMs = endMs;
  }

  /** Returns issued-request count. */
  public long issued() {
    return issued;
  }

  /** Returns good-completion count. */
  public long goodCompleted() {
    return goodCompleted;
  }

  /** Returns bad-completion count. */
  public long badCompleted() {
    return badCompleted;
  }

  /** Returns timeout count. */
  public long timedOut() {
    return timedOut;
  }

  /** Returns dropped-request count. */
  public long dropped() {
    return dropped;
  }

  /** Returns maximum observed wait queue size. */
  public int maxWaitQ() {
    return maxWaitQ;
  }

  /** Returns time-average wait queue size over measurement window. */
  public double avgWaitQ(double measureMs) {
    return measureMs <= 0.0 ? Double.NaN : (waitQAreaMs / measureMs);
  }

  /** Returns count of good responses used in mean. */
  public long goodRespCount() {
    return goodRespCount;
  }

  /** Returns mean good response time in milliseconds. */
  public double goodRespMeanMs() {
    return goodRespCount == 0L ? Double.NaN : (sumGoodRespMs / goodRespCount);
  }

  /** Returns count of client-visible responses used in mean. */
  public long clientRespCount() {
    return clientRespCount;
  }

  /** Returns mean client-visible response time in milliseconds. */
  public double clientRespMeanMs() {
    return clientRespCount == 0L ? Double.NaN : (sumClientRespMs / clientRespCount);
  }
}

