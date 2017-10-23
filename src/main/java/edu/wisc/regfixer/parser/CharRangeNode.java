package edu.wisc.regfixer.parser;

import java.util.Objects;

public class CharRangeNode implements CharClass {
  private ConcreteCharClass left;
  private ConcreteCharClass right;

  public CharRangeNode (ConcreteCharClass left) {
    this.left = left;
    this.right = left;
  }

  public CharRangeNode (char left, char right) {
    this.left = new CharLiteralNode(left);
    this.right = new CharLiteralNode(right);
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

  @Override
  public int hashCode () {
    return Objects.hash(this.toString());
  }

  @Override
  public boolean equals (Object obj) {
    if (obj instanceof CharRangeNode) {
      CharRangeNode cast = (CharRangeNode) obj;
      boolean sameLeft = (this.left == cast.getLeftChild());
      boolean sameRight = (this.right == cast.getRightChild());
      return (sameLeft && sameRight);
    }

    return false;
  }

  public String toString () {
    if (this.left == this.right) {
      return this.left.toString();
    } else {
      return String.format("%s-%s", this.left, this.right);
    }
  }
}
