package edu.wisc.regfixer.parser;

public class CharRangeNode implements CharClass {
  private ConcreteCharClass left;
  private ConcreteCharClass right;

  public CharRangeNode (ConcreteCharClass left) {
    this.left = left;
    this.right = left;
  }

  public CharRangeNode (ConcreteCharClass left, ConcreteCharClass right) {
    this.left = left;
    this.right = right;
  }

  public ConcreteCharClass getLeftChild () {
    return this.left;
  }

  public boolean isSingle () {
    return (this.left == this.right);
  }

  public ConcreteCharClass getRightChild () {
    return this.right;
  }

  public int descendants () {
    return 1;
  }

  public String toString () {
    if (this.left == this.right) {
      return this.left.toString();
    } else {
      return String.format("%s-%s", this.left, this.right);
    }
  }
}
