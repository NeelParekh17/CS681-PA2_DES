package des.dist;

import des.config.TimeParser;
import des.rng.Rng;

public final class DistributionParser {
  private DistributionParser() {}

  public static Distribution parse(String spec, Rng rng) {
    if (spec == null) {
      throw new IllegalArgumentException("distribution spec is null");
    }
    String s = spec.trim();
    if (s.isEmpty()) {
      throw new IllegalArgumentException("distribution spec is empty");
    }
    Parser p = new Parser(s, rng);
    Distribution d = p.parseExpr();
    p.skipWs();
    if (!p.eof()) {
      throw new IllegalArgumentException("unexpected trailing characters in: " + spec);
    }
    return d;
  }

  private static final class Parser {
    private final String s;
    private final Rng rng;
    private int pos = 0;

    private Parser(String s, Rng rng) {
      this.s = s;
      this.rng = rng;
    }

    private Distribution parseExpr() {
      skipWs();
      String name = parseIdent().toLowerCase();
      skipWs();
      expect('(');
      skipWs();

      if (tryConsume(')')) {
        throw new IllegalArgumentException("distribution requires args: " + name);
      }

      Object[] args = new Object[4];
      int argc = 0;
      while (true) {
        if (argc >= args.length) {
          throw new IllegalArgumentException("too many args for: " + name);
        }
        args[argc++] = parseArg();
        skipWs();
        if (tryConsume(',')) {
          continue;
        }
        expect(')');
        break;
      }

      return build(name, args, argc);
    }

    private Object parseArg() {
      skipWs();
      if (eof()) {
        throw new IllegalArgumentException("unexpected end of input");
      }
      char c = s.charAt(pos);
      if (Character.isLetter(c)) {
        int save = pos;
        String ident = parseIdent();
        skipWs();
        if (peek('(')) {
          pos = save;
          return parseExpr();
        }
        throw new IllegalArgumentException("unexpected identifier arg: " + ident);
      }
      return parseTimeLiteral();
    }

    private Double parseTimeLiteral() {
      skipWs();
      int start = pos;
      while (!eof()) {
        char c = s.charAt(pos);
        if (c == ',' || c == ')') {
          break;
        }
        pos++;
      }
      String raw = s.substring(start, pos).trim();
      if (raw.isEmpty()) {
        throw new IllegalArgumentException("empty time literal");
      }
      return TimeParser.parseMs(raw);
    }

    private Distribution build(String name, Object[] args, int argc) {
      return switch (name) {
        case "constant" -> {
          requireArgc(name, argc, 1);
          yield new ConstantDist(asMs(args[0]));
        }
        case "uniform" -> {
          requireArgc(name, argc, 2);
          yield new UniformDist(asMs(args[0]), asMs(args[1]), rng);
        }
        case "exponential" -> {
          requireArgc(name, argc, 1);
          yield new ExponentialDist(asMs(args[0]), rng);
        }
        case "triangular" -> {
          requireArgc(name, argc, 3);
          yield new TriangularDist(asMs(args[0]), asMs(args[1]), asMs(args[2]), rng);
        }
        case "shifted" -> {
          requireArgc(name, argc, 2);
          double offsetMs = asMs(args[0]);
          Distribution inner = asDist(args[1]);
          yield new ShiftedDist(offsetMs, inner);
        }
        default -> throw new IllegalArgumentException("unknown distribution: " + name);
      };
    }

    private static void requireArgc(String name, int actual, int expected) {
      if (actual != expected) {
        throw new IllegalArgumentException(name + " requires " + expected + " args, got " + actual);
      }
    }

    private static double asMs(Object o) {
      if (o instanceof Double d) {
        return d;
      }
      throw new IllegalArgumentException("expected time literal, got: " + o);
    }

    private static Distribution asDist(Object o) {
      if (o instanceof Distribution d) {
        return d;
      }
      throw new IllegalArgumentException("expected distribution, got: " + o);
    }

    private String parseIdent() {
      skipWs();
      int start = pos;
      while (!eof()) {
        char c = s.charAt(pos);
        if (!Character.isLetterOrDigit(c) && c != '_' && c != '-') {
          break;
        }
        pos++;
      }
      if (start == pos) {
        throw new IllegalArgumentException("expected identifier at pos " + pos + " in: " + s);
      }
      return s.substring(start, pos);
    }

    private void skipWs() {
      while (!eof() && Character.isWhitespace(s.charAt(pos))) {
        pos++;
      }
    }

    private boolean eof() {
      return pos >= s.length();
    }

    private boolean peek(char c) {
      return !eof() && s.charAt(pos) == c;
    }

    private boolean tryConsume(char c) {
      if (peek(c)) {
        pos++;
        return true;
      }
      return false;
    }

    private void expect(char c) {
      if (eof() || s.charAt(pos) != c) {
        throw new IllegalArgumentException("expected '" + c + "' at pos " + pos + " in: " + s);
      }
      pos++;
    }
  }
}

