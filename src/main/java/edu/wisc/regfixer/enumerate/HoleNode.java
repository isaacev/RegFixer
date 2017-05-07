package edu.wisc.regfixer.enumerate;

import edu.wisc.regfixer.parser.CharDotNode;
import edu.wisc.regfixer.parser.CharLiteralNode;
import edu.wisc.regfixer.parser.RegexNode;
import edu.wisc.regfixer.parser.StarNode;

public class HoleNode implements RegexNode, Comparable<HoleNode> {
  public static enum FillType {
    Dot,
    DotStar,
    EmptySet
  }

  private static int nextAge = 0;

  private RegexNode child = null;
  private int age;

  public HoleNode () {
    this.age = HoleNode.nextAge++;
  }

  public int descendants () {
    return 0;
  }

  public void fill (FillType type) {
    switch (type) {
      case Dot:
        this.child = new CharDotNode();
        break;
      case DotStar:
        this.child = new StarNode(new CharDotNode());
        break;
      case EmptySet:
        // FIXME
        this.child = new CharLiteralNode('!');
        break;
    }
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
