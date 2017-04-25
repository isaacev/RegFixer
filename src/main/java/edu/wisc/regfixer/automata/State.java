package edu.wisc.regfixer.automata;

import java.util.LinkedList;
import java.util.List;

class State {
  private final int stateId;
  private final State parent;
  private final Character value;
  private final Integer holeId;

  public State (int stateId) {
    this(stateId, null, null, null);
  }

  public State (int stateId, State parent) {
    this(stateId, parent, null, null);
  }

  public State (int stateId, State parent, Character value) {
    this(stateId, parent, value, null);
  }

  public State (int stateId, State parent, Character value, Integer holeId) {
    this.stateId = stateId;
    this.parent = parent;
    this.value = value;
    this.holeId = holeId;
  }

  public int getStateId () {
    return this.stateId;
  }

  public State getParent () {
    return this.parent;
  }

  public Character getValue () {
    return this.value;
  }

  public Integer getHoleId () {
    return this.holeId;
  }

  public String toString () {
    return String.format("%d%s%s <- %s",
      this.stateId,
      (this.holeId != null) ? String.format("'%d", this.holeId) : "",
      (this.value != null) ? String.format(" (%c)", this.value) : "",
      (this.parent != null) ? this.parent.toString() : "null");
  }
}
