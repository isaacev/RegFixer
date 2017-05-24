package edu.wisc.regfixer.enumerate;

public class HoleId {
  private int id;

  public HoleId () {
    this.id = HoleId.getNextId();
  }

  @Override
  public boolean equals (Object obj) {
    if (obj instanceof HoleId) {
      return (this.id == ((HoleId) obj).id);
    }

    return false;
  }

  @Override
  public int hashCode () {
    return this.id;
  }

  @Override
  public String toString () {
    return String.format("H%d", this.id);
  }

  private static int nextId = 0;

  private static int getNextId () {
    return HoleId.nextId++;
  }
}
