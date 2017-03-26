package RegexParser;

import java.io.PrintWriter;

import RegexParser.RegexNode;

public class RepetitionNode implements RegexNode {
  private RegexNode child;
  private RepeatLimit limit;
  private int min;
  private int max;
  private boolean hasMax;

  enum RepeatLimit { Fixed, Unfixed };

  public RepetitionNode (RegexNode child, int min, RepeatLimit limit) {
    this.child = child;
    this.limit = limit;
    this.min = min;
    this.hasMax = false;
  }

  public RepetitionNode (RegexNode child, int min, int max) {
    this.child = child;
    this.limit = RepeatLimit.Fixed;
    this.min = min;
    this.max = max;
    this.hasMax = true;
  }

  public String toString () {
    if (this.limit == RepeatLimit.Unfixed) {
      return this.child.toString() + String.format("{%d,}", this.min);
    } else {
      if (this.hasMax) {
        return this.child.toString() + String.format("{%d,%d}", this.min, this.max);
      }

      return this.child.toString() + String.format("{%d}", this.min);
    }
  }
}
