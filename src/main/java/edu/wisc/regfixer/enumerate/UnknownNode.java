package edu.wisc.regfixer.enumerate;

import edu.wisc.regfixer.parser.CharDotNode;
import edu.wisc.regfixer.parser.CharLiteralNode;
import edu.wisc.regfixer.parser.RegexNode;
import edu.wisc.regfixer.parser.StarNode;
import java.util.LinkedList;
import java.util.List;

public class UnknownNode implements RegexNode, Comparable<UnknownNode> {
  public static enum ExpansionChoice {
    Union,
    Concat,
    Star,
    Plus,
    Optional,
    Repeat,
  }

  public static enum FillType {
    Dot,
    DotStar,
    EmptySet
  }

  private static int nextAge = 0;

  private RegexNode child = null;
  private int age;
  private UnknownId id;
  private List<ExpansionChoice> history;

  private UnknownNode () {
    this(new LinkedList<>());
  }

  public UnknownNode (ExpansionChoice latest) {
    this();
    this.history.add(latest);
  }

  public UnknownNode (List<ExpansionChoice> history) {
    this.age = UnknownNode.nextAge++;
    this.id = new UnknownId();
    this.history = history;
  }

  public UnknownNode expand (ExpansionChoice latest) {
    List<ExpansionChoice> newHistory = new LinkedList<>(this.history);
    newHistory.add(latest);
    return new UnknownNode(newHistory);
  }

  public UnknownId getId () {
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
  public int compareTo (UnknownNode other) {
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
