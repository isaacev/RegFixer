package edu.wisc.regfixer.automata;

import theory.characters.StdCharPred;
import theory.characters.CharPred;

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
