package edu.wisc.regfixer.enumerate;

import edu.wisc.regfixer.parser.Bounds;

public class UnknownBounds extends Bounds implements Unknown {
  // SEE: UnknownChar
  private static Bounds fill = null;
  public static void setFill () { UnknownBounds.setFill(Bounds.atLeast(0)); }
  public static void setFill (Bounds bounds) { UnknownBounds.fill = bounds; }
  public static void clearFill () { UnknownBounds.fill = null; }

  private UnknownId id;

  public UnknownBounds () {
    super(0, null);
    this.id = new UnknownId(this);
  }

  public UnknownBounds (Bounds original) {
    super(original);
    this.id = new UnknownId(this);
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
