package des.metrics;

public record ReplicationResult(
    int users,
    int replication,
    long seed,
    double measureMs,
    long issued,
    long goodCompleted,
    long badCompleted,
    long timedOut,
    long goodRespCount,
    double goodRespMeanMs,
    long clientRespCount,
    double clientRespMeanMs,
    double goodputRps,
    double badputRps,
    double throughputRps,
    double timeoutRps,
    double avgCoreUtil) {}

