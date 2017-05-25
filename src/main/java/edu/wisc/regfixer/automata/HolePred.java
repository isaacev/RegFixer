package edu.wisc.regfixer.automata;

import theory.characters.CharPred;
import theory.characters.StdCharPred;

public class HolePred extends CharPred {
  protected Integer holeId = null;

  public HolePred (int holeId) {
    super(StdCharPred.TRUE.intervals);
    this.holeId = holeId;
  }

  public int getHoleId () {
    return this.holeId;
  }
}
