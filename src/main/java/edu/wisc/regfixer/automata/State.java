package edu.wisc.regfixer.automata;

import java.util.LinkedList;
import java.util.List;

class State {
  private final int stateId;
  private final State parent;
  private final Character value;
  private final Integer holeId;
  private final int hash;

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

    if (this.parent != null) {
      this.hash = (this.stateId * this.stateId) + this.parent.hashCode();
    } else {
      this.hash = (this.stateId * this.stateId);
    }
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

  @Override
  public boolean equals (Object obj) {
    if (obj instanceof State) {
      State cast = (State) obj;
      boolean sameStateId = (this.stateId == cast.stateId);
      boolean sameParent  = (this.parent == cast.parent);

      return (sameStateId && sameParent);
    }

    return false;
  }

  @Override
  public int hashCode () {
    return this.hash;
  }

  public String toString () {
    return String.format("%d%s%s <- %s",
      this.stateId,
      (this.holeId != null) ? String.format("'%d", this.holeId) : "",
      (this.value != null) ? String.format(" (%c)", this.value) : "",
      (this.parent != null) ? this.parent.toString() : "null");
  }
}
