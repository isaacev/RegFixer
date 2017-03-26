package RegexParser;

public class IntervalNode implements RegexNode {
  private enum Mode { SINGLE, RANGE }
  private Mode mode;
  private CharNode left;
  private CharNode right;

  public IntervalNode (CharNode left) {
    this.mode = Mode.SINGLE;
    this.left = left;
  }

  public IntervalNode (CharNode left, CharNode right) {
    this.mode = Mode.RANGE;
    this.left = left;
    this.right = right;
  }

  public String toString () {
    if (this.mode == Mode.SINGLE) {
      return this.left.toString();
    } else {
      return this.left.toString() + "-" + this.right.toString();
    }
  }
}
