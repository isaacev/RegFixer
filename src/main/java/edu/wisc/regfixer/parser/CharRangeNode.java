package edu.wisc.regfixer.parser;

public class CharRangeNode implements CharClass {
  private CharClass left;
  private CharClass right;

  public CharRangeNode (CharClass left) {
    this.left = left;
    this.right = left;
  }

  public CharRangeNode (CharClass left, CharClass right) {
    this.left = left;
    this.right = right;
  }

  public String toString () {
    if (this.left == this.right) {
      return this.left.toString();
    } else {
      return String.format("%s-%s", this.left, this.right);
    }
  }
}
