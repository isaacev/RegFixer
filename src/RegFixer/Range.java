package RegFixer;

import java.util.regex.*;

class Range {
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

  public String toString () {
    return String.format("(%d:%d)", this.leftIndex, this.rightIndex);
  }
}
