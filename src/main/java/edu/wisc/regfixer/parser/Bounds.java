package edu.wisc.regfixer.parser;

public class Bounds {
  private int min;
  private Integer max;

  public static Bounds exactly (int n) {
    return new Bounds(n, n);
  }

  public static Bounds atLeast (int min) {
    return new Bounds(min, null);
  }

  public static Bounds between (int min, int max) {
    return new Bounds(min, max);
  }

  private Bounds (int min, Integer max) {
    if (min < 0) {
      throw new IllegalArgumentException("illegal min < 0");
    }

    if (max != null && max < 0) {
      throw new IllegalArgumentException("illegal max < 0");
    }

    this.min = min;
    this.max = max;
  }

  public String toString () {
    if (this.max == null) {
      return String.format("{%d,}", this.min);
    } else if (this.min == this.max) {
      return String.format("{%d}", this.min);
    } else {
      return String.format("{%d,%d}", this.min, this.max);
    }
  }
}
