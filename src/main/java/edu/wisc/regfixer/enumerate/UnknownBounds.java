package edu.wisc.regfixer.enumerate;

import edu.wisc.regfixer.parser.Bounds;

public class UnknownBounds extends Bounds implements Unknown {
  private UnknownId id;
  private Bounds bounds;

  public UnknownBounds () {
    super(0, null);
    this.id = new UnknownId();
    this.bounds = null;
  }

  public UnknownId getId () {
    return this.id;
  }

  public void fill (Bounds bounds) {
    this.bounds = bounds;
  }

  public void clear () {
    this.bounds = null;
  }

  public String toString () {
    if (this.bounds == null) {
      return "{■}";
    } else {
      return this.bounds.toString();
    }
  }
}
