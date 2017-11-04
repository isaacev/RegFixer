package edu.wisc.regfixer.enumerate;

import edu.wisc.regfixer.parser.Bounds;

public class UnknownBounds extends Bounds implements Unknown {
  // SEE: UnknownChar
  private static Bounds fill = null;
  private static void setFill () { this.setFill(Bounds.atLeat(0)); }
  private static void setFill (Bounds bounds) { UnknownBounds.bounds = bounds; }
  private static void clearFill () { UnknownBounds.bounds = null; }

  private UnknownId id;

  public UnknownBounds () {
    super(0, null);
    this.id = new UnknownId();
  }

  public UnknownId getId () {
    return this.id;
  }

  public String toString () {
    if (UnknownBounds.fill == null) {
      return "{■}";
    } else {
      return UnknownBounds.fill.toString();
    }
  }
}
