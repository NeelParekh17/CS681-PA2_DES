package des.config;

public final class TimeParser {
  private TimeParser() {}

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

