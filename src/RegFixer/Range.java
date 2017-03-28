package RegFixer;

import java.util.regex.*;

class Range implements Comparable<Range> {
  private int leftIndex;
  private int rightIndex;

  Range (int leftIndex, int rightIndex) {
    this.leftIndex = leftIndex;
    this.rightIndex = rightIndex;
  }

  Range (String st) throws BadRangeException {
    Pattern pt = Pattern.compile("\\((\\d+):(\\d+)\\)");
    Matcher mt = pt.matcher(st);

    if (mt.find()) {
      this.leftIndex = Integer.parseInt(mt.group(1));
      this.rightIndex = Integer.parseInt(mt.group(2));
    } else {
      String fmt = "cannot parse '%s', expect form '(1:2)'";
      throw new BadRangeException(String.format(fmt, st));
    }
  }

  int getLeftIndex () {
    return this.leftIndex;
  }

  int getRightIndex () {
    return this.rightIndex;
  }

  boolean equals (Range other) {
    boolean leftIsEqual = (this.leftIndex == other.getLeftIndex());
    boolean rightIsEqual = (this.rightIndex == other.getRightIndex());

    return leftIsEqual && rightIsEqual;
  }

  boolean startsBefore (Range other) {
    return (this.leftIndex < other.getLeftIndex());
  }

  boolean startsAfter (Range other) {
    return (this.leftIndex > other.getRightIndex());
  }

  boolean endsBefore (Range other) {
    return (this.rightIndex < other.getLeftIndex());
  }

  boolean endsAfter (Range other) {
    return (this.rightIndex > other.getRightIndex());
  }

  boolean intersects (Range other) {
    boolean intersectionAtStart = (this.leftIndex <= other.getRightIndex());
    boolean intersectionAtEnd = (this.rightIndex <= other.getLeftIndex());

    return intersectionAtStart || intersectionAtEnd;
  }

  @Override
  public int compareTo (Range other) {
    return Integer.compare(this.leftIndex, other.leftIndex);
  }

  public String toString () {
    return String.format("(%d:%d)", this.leftIndex, this.rightIndex);
  }
}
