package edu.wisc.regfixer.enumerate;

public class UnknownInt extends Unknown {
  private Integer child;

  public UnknownInt () {
    super();
    this.child = null;
  }

  public void fill (int child) {
    this.child = child;
  }

  public void clear () {
    this.child = null;
  }

  public String toString () {
    if (this.child == null) {
      return super.toString();
    } else {
      return this.child.toString();
    }
  }
}
