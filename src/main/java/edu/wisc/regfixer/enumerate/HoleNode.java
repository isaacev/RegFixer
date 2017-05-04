package edu.wisc.regfixer.enumerate;

import edu.wisc.regfixer.parser.CharDotNode;
import edu.wisc.regfixer.parser.RegexNode;
import edu.wisc.regfixer.parser.StarNode;
import edu.wisc.regfixer.parser.CharLiteralNode;

public class HoleNode implements RegexNode, Comparable<HoleNode> {
  private static int nextAge = 0;

  private RegexNode child = null;
  private int age;

  public HoleNode () {
    this.age = HoleNode.nextAge++;
  }

  public int descendants () {
    return 0;
  }

  public void fill (RegexNode child) {
    this.child = child;
  }

  public void clear () {
    this.child = null;
  }

  @Override
  public int compareTo (HoleNode other) {
    return Integer.compare(this.age, other.age);
  }

  public String toString () {
    if (this.child == null) {
      return "❑";
    } else {
      return this.child.toString();
    }
  }
}
