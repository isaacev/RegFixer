package edu.wisc.regfixer.fixer;

import edu.wisc.regfixer.parser.CharDotNode;
import edu.wisc.regfixer.parser.CharEscapedNode;
import edu.wisc.regfixer.parser.RegexNode;

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
