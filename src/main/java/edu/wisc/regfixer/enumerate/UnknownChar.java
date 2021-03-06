package edu.wisc.regfixer.enumerate;

import java.util.LinkedList;
import java.util.List;

import edu.wisc.regfixer.parser.CharDotNode;
import edu.wisc.regfixer.parser.CharLiteralNode;
import edu.wisc.regfixer.parser.RegexNode;
import edu.wisc.regfixer.parser.StarNode;

public class UnknownChar implements Unknown, RegexNode, Comparable<UnknownChar> {
  // A bit of a hack...
  //
  // During enumeration of many possible regex templates it's necessary to
  // build regular expressions of incomplete templates where each unknown char
  // is rendered as a some temporary char-class before the whole expression is
  // passed to the regex engine. As long as the 'fill' property is NULL, any
  // call to UnknownChar#toString() will return '■' but if 'fill' is some other
  // value the method will return the appropriate expression.
  //
  // It's important that any time this value is set to a non-NULL value it is
  // reset back to NULL immediately to prevent bugs further in the enumeration
  // process.
  public static enum FillType { Dot, DotStar, EmptySet, Unfrozen, Default }
  private static FillType fill = FillType.Default;
  public static void setFill (FillType which) { UnknownChar.fill = which; }
  public static void clearFill () { UnknownChar.fill = FillType.Default; }

  // Used to compute a unique age for each UnknownChar generated so that older
  // nodes can be expanded before younger nodes.
  private static int nextAge = 0;

  private UnknownId id;
  private int age;
  private List<Expansion> history;
  private boolean frozen = false;

  private UnknownChar () {
    this(new LinkedList<>());
  }

  public UnknownChar (Expansion latest) {
    this.id = new UnknownId(this);
    this.history.add(latest);
  }

  public UnknownChar (List<Expansion> history) {
    this.id = new UnknownId(this);
    this.age = UnknownChar.nextAge++;
    this.history = history;
  }

  public UnknownChar (List<Expansion> history, Expansion latest) {
    this.id = new UnknownId(this);
    this.age = UnknownChar.nextAge++;
    this.history = new LinkedList<>(history);
    this.history.add(latest);
  }

  public UnknownId getId () {
    return this.id;
  }

  public int getAge () {
    return this.age;
  }

  public List<Expansion> getHistory () {
    return this.history;
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

  public void freeze () {
    this.frozen = true;
  }

  public boolean isFrozen () {
    return this.frozen;
  }

  public int descendants () {
    return 0;
  }

  @Override
  public int compareTo (UnknownChar other) {
    return Integer.compare(this.age, other.age);
  }

  public String toString () {
    switch (UnknownChar.fill) {
      case Dot:
        return ".";
      case DotStar:
        return ".*";
      case EmptySet:
        // FIXME
        return "\0000";
      case Default:
        if (this.isFrozen()) {
          return "▓";
        }
      default:
        return "■";
    }
  }
}
