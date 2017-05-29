package edu.wisc.regfixer.automata;

import edu.wisc.regfixer.enumerate.HoleId;
import theory.characters.CharPred;
import theory.characters.StdCharPred;

public class HolePred extends CharPred {
  protected HoleId holeId = null;

  public HolePred (HoleId holeId) {
    super(StdCharPred.TRUE.intervals);
    this.holeId = holeId;
  }

  public HoleId getHoleId () {
    return this.holeId;
  }
}
