package des.metrics;

import des.model.Request;

public final class Metrics {
  private boolean measuring = false;
  private double measurementStartMs = Double.NaN;

  private long issued = 0L;
  private long goodCompleted = 0L;
  private long badCompleted = 0L;
  private long timedOut = 0L;

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

