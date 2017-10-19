package edu.wisc.regfixer.enumerate;

import edu.wisc.regfixer.parser.CharDotNode;
import edu.wisc.regfixer.parser.CharLiteralNode;
import edu.wisc.regfixer.parser.RegexNode;
import edu.wisc.regfixer.parser.StarNode;
import java.util.LinkedList;
import java.util.List;

public class HoleNode implements RegexNode, Comparable<HoleNode> {
  public static enum ExpansionChoice {
    Union,
    Concat,
    Star,
    Plus,
    Optional,
  }

  public static enum FillType {
    Dot,
    DotStar,
    EmptySet
  }

  private static int nextAge = 0;

  private RegexNode child = null;
  private int age;
  private HoleId id;
  private List<ExpansionChoice> history;

  public HoleNode () {
    this(new LinkedList<>());
  }

  public HoleNode (ExpansionChoice latest) {
    this();
    this.history.add(latest);
  }

  public HoleNode (List<ExpansionChoice> history) {
    this.age = HoleNode.nextAge++;
    this.id = new HoleId();
    this.history = history;
  }

  public HoleNode expand (ExpansionChoice latest) {
    List<ExpansionChoice> newHistory = new LinkedList<>(this.history);
    newHistory.add(latest);
    return new HoleNode(newHistory);
  }

  public HoleId getHoleId () {
    return this.id;
  }

  public boolean canInsertQuantifierNodes () {
    for (int i = this.history.size() - 1; i >= 0; i--) {
      switch (this.history.get(i)) {
        case Union:
          continue;
        case Concat:
          return true;
        default:
          return false;
      }
    }

    return true;
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
