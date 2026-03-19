package des.metrics;

/**
 * Immutable snapshot of one completed replication and its derived KPIs.
 *
 * @param users concurrent user population for this run
 * @param replication replication index
 * @param seed effective RNG seed used in this replication
 * @param measureMs measurement window length in milliseconds
 * @param issued issued-request count during measurement
 * @param goodCompleted completed-before-timeout count
 * @param badCompleted completed-after-timeout count
 * @param timedOut timeout count visible to clients
 * @param dropped dropped-request count (queue full)
 * @param goodRespCount denominator for good response-time mean
 * @param goodRespMeanMs mean response time of good completions
 * @param clientRespCount denominator for client-visible response mean
 * @param clientRespMeanMs mean client-visible response time
 * @param goodputRps successful completions per second
 * @param badputRps late completions per second
 * @param throughputRps total completions per second
 * @param timeoutRps timeouts per second
 * @param dropRps drops per second
 * @param dropRate drop fraction among issued requests
 * @param avgWaitQ time-average wait queue size
 * @param maxWaitQ peak wait queue size
 * @param avgCoreUtil average core utilization during measurement
 */
public record ReplicationResult(
    int users,
    int replication,
    long seed,
    double measureMs,
    long issued,
    long goodCompleted,
    long badCompleted,
    long timedOut,
    long dropped,
    long goodRespCount,
    double goodRespMeanMs,
    long clientRespCount,
    double clientRespMeanMs,
    double goodputRps,
    double badputRps,
    double throughputRps,
    double timeoutRps,
    double dropRps,
    double dropRate,
    double avgWaitQ,
    int maxWaitQ,
    double avgCoreUtil) {}

