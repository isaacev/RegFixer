package RegFixer;

import RegexParser.*;

public class CharClassSuggestEngine {
  private int nextIndex = 0;

  private RegexNode[] charClasses = new RegexNode[]{
    new CharDotNode(),
    new CharEscapedNode('w'),
    new CharEscapedNode('d'),
  };

  public RegexNode nextCharClass () {
    if (this.nextIndex < this.charClasses.length) {
      return this.charClasses[this.nextIndex++];
    }

    return null;
  }
}
