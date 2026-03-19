package des.config;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Properties;

/**
 * Typed wrapper around .properties input used by simulation modes.
 *
 * <p>Provides parsing helpers for ints, time strings, booleans, and user-count ranges.
 */
public final class SimConfig {
  /** Backing key/value map loaded from a properties file. */
  private final Properties props;
  /** Absolute or relative source path used for diagnostics. */
  private final Path sourcePath;

  private SimConfig(Properties props, Path sourcePath) {
    this.props = props;
    this.sourcePath = sourcePath;
  }

  /** Loads properties from disk and creates a typed config wrapper. */
  public static SimConfig load(Path path) throws IOException {
    Properties p = new Properties();
    try (InputStream in = Files.newInputStream(path)) {
      p.load(in);
    }
    return new SimConfig(p, path);
  }

  /** Returns the config file path used to create this object. */
  public Path sourcePath() {
    return sourcePath;
  }

  /** Returns required string value and throws when key is missing. */
  public String getString(String key) {
    String v = props.getProperty(key);
    if (v == null) {
      throw new IllegalArgumentException("Missing config key: " + key);
    }
    return v.trim();
  }

  /** Returns optional string value with default fallback. */
  public String getString(String key, String def) {
    String v = props.getProperty(key);
    return v == null ? def : v.trim();
  }

  /** Returns boolean value with default fallback when absent. */
  public boolean getBool(String key, boolean def) {
    String v = props.getProperty(key);
    if (v == null) {
      return def;
    }
    return Boolean.parseBoolean(v.trim());
  }

  /** Returns required integer value. */
  public int getInt(String key) {
    return Integer.parseInt(getString(key));
  }

  /** Returns optional integer value with fallback. */
  public int getInt(String key, int def) {
    String v = props.getProperty(key);
    return v == null ? def : Integer.parseInt(v.trim());
  }

  /** Returns optional long value with fallback. */
  public long getLong(String key, long def) {
    String v = props.getProperty(key);
    return v == null ? def : Long.parseLong(v.trim());
  }

  /** Parses required duration key into milliseconds. */
  public double getTimeMs(String key) {
    return TimeParser.parseMs(getString(key));
  }

  /** Parses optional duration key into milliseconds with fallback. */
  public double getTimeMs(String key, double defMs) {
    String v = props.getProperty(key);
    return v == null ? defMs : TimeParser.parseMs(v.trim());
  }

  /** Returns user-count array either from key or caller-provided default. */
  public int[] getUserCounts(String key, int[] def) {
    String v = props.getProperty(key);
    return v == null ? def : parseUserCounts(v.trim());
  }

  /**
   * Parses user-count specs in one of three forms:
   * start:end[:step], comma-list, or single integer.
   */
  public static int[] parseUserCounts(String spec) {
    String s = spec.trim();
    if (s.isEmpty()) {
      throw new IllegalArgumentException("empty userCounts spec");
    }

    if (s.contains(":")) {
      String[] parts = s.split(":");
      if (parts.length < 2 || parts.length > 3) {
        throw new IllegalArgumentException("invalid range spec: " + spec);
      }
      int start = Integer.parseInt(parts[0].trim());
      int end = Integer.parseInt(parts[1].trim());
      int step = parts.length == 3 ? Integer.parseInt(parts[2].trim()) : 1;
      if (step <= 0) {
        throw new IllegalArgumentException("step must be > 0 in: " + spec);
      }
      if (end < start) {
        throw new IllegalArgumentException("end must be >= start in: " + spec);
      }
      int n = ((end - start) / step) + 1;
      int[] out = new int[n];
      int cur = start;
      for (int i = 0; i < n; i++) {
        out[i] = cur;
        cur += step;
      }
      return out;
    }

    if (s.contains(",")) {
      String[] parts = s.split(",");
      int[] out = new int[parts.length];
      for (int i = 0; i < parts.length; i++) {
        out[i] = Integer.parseInt(parts[i].trim());
      }
      return out;
    }

    return new int[] {Integer.parseInt(s)};
  }
}

