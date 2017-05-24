package edu.wisc.regfixer.automata;

import java.util.LinkedList;
import java.util.List;

import edu.wisc.regfixer.enumerate.HoleId;

class State {
  private final int stateId;
  private final State parent;
  private final Character value;
  private final HoleId holeId;

  public State (int stateId) {
    this(stateId, null, null, null);
  }

  public State (int stateId, State parent) {
    this(stateId, parent, null, null);
  }

  public State (int stateId, State parent, Character value) {
    this(stateId, parent, value, null);
  }

  public State (int stateId, State parent, Character value, HoleId holeId) {
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

  public HoleId getHoleId () {
    return this.holeId;
  }

  @Override
  public boolean equals (Object obj) {
    if (obj instanceof State) {
      State cast = (State) obj;
      return (this.stateId == cast.stateId);
    }

    return false;
  }

  @Override
  public int hashCode () {
    return this.stateId;
  }

  public String toString () {
    return String.format("%d%s%s <- %s",
      this.stateId,
      (this.holeId != null) ? String.format("%s", this.holeId.toString()) : "",
      (this.value != null) ? String.format(" (%c)", this.value) : "",
      (this.parent != null) ? this.parent.toString() : "null");
  }
}
