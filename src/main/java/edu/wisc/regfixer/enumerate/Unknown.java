package edu.wisc.regfixer.enumerate;

public class Unknown {
  private UnknownId id;

  public Unknown () {
    this.id = new UnknownId();
  }

  public UnknownId getId () {
    return this.id;
  }

  public String toString () {
    return "❑";
  }
}
