package des.config;

/** Utility for converting duration literals like 5ms, 2s, and 1m into milliseconds. */
public final class TimeParser {
  /** Static utility holder. */
  private TimeParser() {}

  /**
   * Parses a duration string into milliseconds.
   *
   * <p>Supported suffixes: ms, s, m. Missing unit defaults to milliseconds.
   */
  public static double parseMs(String raw) {
    if (raw == null) {
      throw new IllegalArgumentException("time value is null");
    }
    String s = raw.trim().toLowerCase();
    if (s.isEmpty()) {
      throw new IllegalArgumentException("time value is empty");
    }

    int unitStart = s.length();
    while (unitStart > 0 && Character.isLetter(s.charAt(unitStart - 1))) {
      unitStart--;
    }

    String numberPart = s.substring(0, unitStart).trim();
    String unitPart = s.substring(unitStart).trim();
    if (numberPart.isEmpty()) {
      throw new IllegalArgumentException("time value missing number: " + raw);
    }

    double value;
    try {
      value = Double.parseDouble(numberPart);
    } catch (NumberFormatException e) {
      throw new IllegalArgumentException("invalid time number: " + raw, e);
    }

    return switch (unitPart) {
      case "", "ms" -> value;
      case "s" -> value * 1_000.0;
      case "m" -> value * 60_000.0;
      default -> throw new IllegalArgumentException("invalid time unit in: " + raw);
    };
  }
}

