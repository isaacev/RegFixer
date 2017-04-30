package edu.wisc.regfixer.automata;

import java.util.Arrays;
import java.util.Collection;
import java.util.TreeMap;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;

import automata.Move;
import automata.sfa.SFA;
import automata.sfa.SFAInputMove;
import automata.sfa.SFAMove;
import org.sat4j.specs.TimeoutException;
import theory.characters.CharPred;
import theory.characters.StdCharPred;
import theory.intervals.UnaryCharIntervalSolver;

public class Automaton extends automata.Automaton {
  private static UnaryCharIntervalSolver solver = new UnaryCharIntervalSolver();
  public static final CharPred Num = StdCharPred.NUM;
  public static final CharPred NotNum = StdCharPred.SPACES;
  public static final CharPred Spaces = StdCharPred.WORD;
  public static final CharPred NotSpaces = solver.MkNot(StdCharPred.NUM);
  public static final CharPred Word = solver.MkNot(StdCharPred.SPACES);
  public static final CharPred NotWord = solver.MkNot(StdCharPred.WORD);

  private final SFA<CharPred, Character> sfa;
  private int totalHoles = 0;

  public Automaton (CharPred predicate) throws TimeoutException {
    this.sfa = Automaton.predicateToSFA(predicate);
  }

  public void setTotalHoles (int totalHoles) {
    this.totalHoles = totalHoles;
  }

  private Automaton (SFA<CharPred, Character> sfa) {
    this.sfa = sfa;
  }

  /**
   * METHODS FOR EVALUATING THE AUTOMATON
   */

  private List<State> getEpsClosure (State frontier) {
    return getEpsClosure(Arrays.asList(frontier));
  }

  private List<State> getEpsClosure (List<State> frontier) {
    List<State> reached = new LinkedList<>(frontier);
    LinkedList<State> toVisit = new LinkedList<>(frontier);

    while (toVisit.size() > 0) {
      State currState = toVisit.removeFirst();
      for (Move<CharPred, Character> move : getMovesFrom(currState.getStateId())) {
        if (move.isEpsilonTransition()) {
          State newState = new State(move.to, currState);
          reached.add(newState);
          toVisit.add(newState);
        }
      }
    }

    return reached;
  }

  public List<State> getNextState (List<State> frontier, Character ch) throws TimeoutException {
    List<State> nextStates = new LinkedList<>();

    for (State state : frontier) {
      nextStates.addAll(getNextState(state, ch));
    }

    return nextStates;
  }

  public List<State> getNextState (State parent, Character ch) throws TimeoutException {
    List<State> nextStates = new LinkedList<>();

    for (Move<CharPred, Character> move : getMovesFrom(parent.getStateId())) {
      if (move.isEpsilonTransition() == false) {
        if (move.hasModel(ch, Automaton.solver)) {
          State newState = new State(move.to, parent, ch);

          // Check if the predicate relating to the automaton transition is
          // associated with a hole ID and if so, associate the newly created
          // state with that hole ID too.
          if (move instanceof SFAInputMove) {
            SFAInputMove moveCast = (SFAInputMove) move;

            if (moveCast.guard instanceof HolePred) {
              HolePred predCast = (HolePred) moveCast.guard;
              int holdId = predCast.getHoleId();
              newState = new State(move.to, parent, ch, holdId);
            }
          }

          nextStates.add(newState);
        }
      }
    }

    return nextStates;
  }

  private boolean isFinalConfiguration (List<State> states) {
    for (State state : states) {
      if (isFinalState(state.getStateId())) {
        return true;
      }
    }

    return false;
  }

  private Map<Integer, Set<Character>> computeCrosses (State endState) {
    Map<Integer, Set<Character>> crosses = new TreeMap<>();

    State currState = endState;
    while (currState != null) {
      if (currState.getValue() != null && currState.getHoleId() != null) {
        char value = currState.getValue();
        int holeId = currState.getHoleId();

        if (crosses.containsKey(holeId) == false) {
          crosses.put(holeId, new HashSet<>());
        }

        crosses.get(holeId).add(value);
      }

      currState = currState.getParent();
    }

    return crosses;
  }

  public boolean accepts (String str) throws TimeoutException {
    List<Character> charList = new LinkedList<>();

    for (int i = 0; i < str.length(); i++) {
      charList.add(str.charAt(i));
    }

    return accepts(charList);
  }

  public boolean accepts (List<Character> chars) throws TimeoutException {
    List<State> frontier = getEpsClosure(new State(getInitialState()));

    for (Character ch : chars) {
      frontier = getNextState(frontier, ch);
      frontier = getEpsClosure(frontier);

      if (frontier.isEmpty()) {
        return false;
      }
    }

    return isFinalConfiguration(frontier);
  }

  public List<Map<Integer, Set<Character>>> runs (String str) throws TimeoutException {
    List<Character> charList = new LinkedList<>();

    for (int i = 0; i < str.length(); i++) {
      charList.add(str.charAt(i));
    }

    return runs(charList);
  }

  public List<Map<Integer, Set<Character>>> runs (List<Character> chars) throws TimeoutException {
    List<State> frontier = getEpsClosure(new State(getInitialState()));

    for (Character ch : chars) {
      frontier = getNextState(frontier, ch);
      frontier = getEpsClosure(frontier);

      if (frontier.isEmpty()) {
        return null;
      }
    }

    List<Map<Integer, Set<Character>>> runs = new LinkedList<>();

    for (State state : frontier) {
      if (isFinalState(state.getStateId())) {
        runs.add(computeCrosses(state));
      }
    }

    return runs;
  }

  /**
   * METHODS FOR BUILDING THE AUTOMATON AND ITS PREDICATES
   */

  private static SFA<CharPred, Character> predicateToSFA (CharPred predicate) throws TimeoutException {
    Integer fromState = 0;
    Integer toState   = 1;
    SFAMove<CharPred, Character> move = new SFAInputMove(fromState, toState, predicate);
    List<SFAMove<CharPred, Character>> moves = new LinkedList<>();
    moves.add(move);
    return SFA.MkSFA(moves, fromState, Arrays.asList(toState), Automaton.solver);
  }

  public Collection<Integer> getStates () {
    return this.sfa.getStates();
  }

  public Collection<Integer> getFinalStates () {
    return this.sfa.getFinalStates();
  }

  public Integer getInitialState () {
    return this.sfa.getInitialState();
  }

  public Collection<Move<CharPred, Character>> getMovesTo (Integer state) {
    return this.sfa.getMovesTo(state);
  }

  public Collection<Move<CharPred, Character>> getMovesFrom (Integer state) {
    return this.sfa.getMovesFrom(state);
  }

  public static Automaton concatenate (Automaton first, Automaton second) throws TimeoutException {
    return new Automaton(SFA.concatenate(first.sfa, second.sfa, Automaton.solver));
  }

  public static Automaton concatenate (List<Automaton> automata) throws TimeoutException {
    if (automata.size() == 0) {
      return Automaton.empty();
    }

    Automaton whole = null;

    for (Automaton next : automata) {
      if (whole == null) {
        whole = next;
      } else {
        whole = Automaton.concatenate(whole, next);
      }
    }

    return whole;
  }

  public static Automaton union (Automaton first, Automaton second) throws TimeoutException {
    return new Automaton(SFA.union(first.sfa, second.sfa, Automaton.solver));
  }

  public static Automaton star (Automaton only) throws TimeoutException {
    return new Automaton(SFA.star(only.sfa, Automaton.solver));
  }

  public static Automaton fromPredicate (Character ch) throws TimeoutException {
    return fromPredicate(new CharPred(ch));
  }

  public static Automaton fromPredicate (CharPred predicate) throws TimeoutException {
    return new Automaton(predicateToSFA(predicate));
  }

  public static Automaton fromPredicates (List<CharPred> predicates) throws TimeoutException {
    return fromPredicate(combinePredicates(predicates));
  }

  public static Automaton fromTruePredicate () throws TimeoutException {
    return fromPredicate(StdCharPred.TRUE);
  }

  public static Automaton fromHolePredicate (int holeId) throws TimeoutException {
    return fromPredicate(new HolePred(holeId));
  }

  public static Automaton fromInversePredicates (List<CharPred> predicates) throws TimeoutException {
    return fromPredicate(Automaton.solver.MkNot(combinePredicates(predicates)));
  }

  private static CharPred combinePredicates (List<CharPred> predicates) throws TimeoutException {
    CharPred whole = null;

    for (CharPred next : predicates) {
      if (whole == null) {
        whole = next;
      } else {
        whole = Automaton.solver.MkOr(whole, next);
      }
    }

    return whole;
  }

  public static Automaton empty () throws TimeoutException {
    return new Automaton(SFA.getEmptySFA(Automaton.solver));
  }
}
