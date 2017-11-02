package edu.wisc.regfixer.enumerate;

public class UnknownInt {
  private UnknownId id;
  private Integer child;

  public UnknownInt () {
    this.id = new UnknownId();
    this.child = null;
  }

  public UnknownId getId () {
    return this.id;
  }

  public void fill (int child) {
    this.child = child;
  }

  public void clear () {
    this.child = null;
  }

  public String toString () {
    if (this.child == null) {
      return "❑";
    } else {
      return this.child.toString();
    }
  }
}
