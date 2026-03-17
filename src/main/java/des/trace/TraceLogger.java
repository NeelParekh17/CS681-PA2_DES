package des.trace;

import des.model.Request;
import java.io.BufferedWriter;
import java.io.IOException;
import java.io.PrintWriter;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Locale;

public final class TraceLogger implements AutoCloseable {
  public enum Level {
    SUMMARY,
    VERBOSE
  }

  private final boolean enabled;
  private final Level level;
  private final long maxLines;
  private final PrintWriter out;
  private long linesWritten = 0L;

  private TraceLogger(boolean enabled, Level level, long maxLines, PrintWriter out) {
    this.enabled = enabled;
    this.level = level;
    this.maxLines = maxLines;
    this.out = out;
  }

  public static TraceLogger disabled() {
    return new TraceLogger(false, Level.SUMMARY, 0L, null);
  }

  public static TraceLogger create(boolean enabled, Level level, long maxLines, Path path)
      throws IOException {
    if (!enabled) {
      return disabled();
    }
    Path parent = path.toAbsolutePath().getParent();
    if (parent != null) {
      Files.createDirectories(parent);
    }
    PrintWriter pw = new PrintWriter(new BufferedWriter(Files.newBufferedWriter(path)));
    return new TraceLogger(true, level, maxLines, pw);
  }

  public void onWarmupReset(double nowMs) {
    log(String.format(Locale.ROOT, "t=%.3fms | WARMUP_RESET", nowMs));
  }

  public void onIssue(double nowMs, Request r, int waitQ, int idleThreads) {
    log(
        String.format(
            Locale.ROOT,
            "t=%.3fms | ISSUE | u=%d r=%d | waitQ=%d idleThr=%d | cpu=%.3fms timeoutAt=%.3fms",
            nowMs,
            r.userId,
            r.id,
            waitQ,
            idleThreads,
            r.remainingCpuMs,
            r.timeoutAtMs));
  }

  public void onQueued(double nowMs, Request r, int waitQ, int idleThreads) {
    log(
        String.format(
            Locale.ROOT,
            "t=%.3fms | QUEUE | u=%d r=%d | waitQ=%d idleThr=%d",
            nowMs,
            r.userId,
            r.id,
            waitQ,
            idleThreads));
  }

  public void onDrop(double nowMs, Request r, int waitQ, int idleThreads) {
    log(
        String.format(
            Locale.ROOT,
            "t=%.3fms | DROP_QUEUE_FULL | u=%d r=%d | waitQ=%d idleThr=%d",
            nowMs,
            r.userId,
            r.id,
            waitQ,
            idleThreads));
  }

  public void onDispatch(double nowMs, Request r, int coreId, int threadId, int waitQ, int idleThreads) {
    log(
        String.format(
            Locale.ROOT,
            "t=%.3fms | DISPATCH | u=%d r=%d | core=%d thr=%d | waitQ=%d idleThr=%d",
            nowMs,
            r.userId,
            r.id,
            coreId,
            threadId,
            waitQ,
            idleThreads));
  }

  public void onTimeout(double nowMs, Request r) {
    log(
        String.format(
            Locale.ROOT, "t=%.3fms | TIMEOUT | u=%d r=%d", nowMs, r.userId, r.id));
  }

  public void onGoodCompletion(double nowMs, Request r, int coreId, int threadId) {
    log(
        String.format(
            Locale.ROOT,
            "t=%.3fms | COMPLETE_GOOD | u=%d r=%d | core=%d thr=%d | rt=%.3fms",
            nowMs,
            r.userId,
            r.id,
            coreId,
            threadId,
            nowMs - r.issueTimeMs));
  }

  public void onBadCompletion(double nowMs, Request r, int coreId, int threadId) {
    log(
        String.format(
            Locale.ROOT,
            "t=%.3fms | COMPLETE_BAD | u=%d r=%d | core=%d thr=%d | lateBy=%.3fms",
            nowMs,
            r.userId,
            r.id,
            coreId,
            threadId,
            nowMs - r.timeoutAtMs));
  }

  public void onTimeslice(
      double nowMs,
      Request r,
      int coreId,
      int threadId,
      double remainingCpuMs,
      int runnableQ) {
    if (level != Level.VERBOSE) {
      return;
    }
    log(
        String.format(
            Locale.ROOT,
            "t=%.3fms | SLICE_END | u=%d r=%d | core=%d thr=%d | remCpu=%.3fms runQ=%d",
            nowMs,
            r.userId,
            r.id,
            coreId,
            threadId,
            remainingCpuMs,
            runnableQ));
  }

  private void log(String line) {
    if (!enabled) {
      return;
    }
    if (maxLines > 0 && linesWritten >= maxLines) {
      return;
    }
    out.println(line);
    linesWritten++;
  }

  @Override
  public void close() {
    if (out != null) {
      out.flush();
      out.close();
    }
  }
}
