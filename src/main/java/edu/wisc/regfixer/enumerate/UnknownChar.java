package edu.wisc.regfixer.enumerate;

import edu.wisc.regfixer.parser.CharDotNode;
import edu.wisc.regfixer.parser.CharLiteralNode;
import edu.wisc.regfixer.parser.RegexNode;
import edu.wisc.regfixer.parser.StarNode;
import java.util.LinkedList;
import java.util.List;

public class UnknownChar implements Unknown, RegexNode, Comparable<UnknownChar> {
  public static enum FillType {
    Dot,
    DotStar,
    EmptySet
  }

  private static int nextAge = 0;

  private UnknownId id;
  private RegexNode child = null;
  private int age;
  private List<Expansion> history;

  private UnknownChar () {
    this(new LinkedList<>());
  }

  public UnknownChar (Expansion latest) {
    this.id = new UnknownId();
    this.history.add(latest);
  }

  public UnknownChar (List<Expansion> history) {
    this.id = new UnknownId();
    this.age = UnknownChar.nextAge++;
    this.history = history;
  }

  public UnknownId getId () {
    return this.id;
  }

  public UnknownChar expand (Expansion latest) {
    List<Expansion> newHistory = new LinkedList<>(this.history);
    newHistory.add(latest);
    return new UnknownChar(newHistory);
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
  public int compareTo (UnknownChar other) {
    return Integer.compare(this.age, other.age);
  }

  public String toString () {
    if (this.child == null) {
      return "■";
    } else {
      return this.child.toString();
    }
  }
}
