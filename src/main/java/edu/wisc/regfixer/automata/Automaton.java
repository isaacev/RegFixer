package edu.wisc.regfixer.automata;

import java.util.Arrays;
import java.util.Collection;
import java.util.HashMap;
import java.util.TreeMap;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import automata.Move;
import automata.sfa.SFA;
import automata.sfa.SFAInputMove;
import automata.sfa.SFAMove;
import edu.wisc.regfixer.enumerate.HoleNode;
import edu.wisc.regfixer.parser.CharClassSetNode;
import edu.wisc.regfixer.parser.CharDotNode;
import edu.wisc.regfixer.parser.CharEscapedNode;
import edu.wisc.regfixer.parser.CharLiteralNode;
import edu.wisc.regfixer.parser.CharRangeNode;
import edu.wisc.regfixer.parser.ConcatNode;
import edu.wisc.regfixer.parser.OptionalNode;
import edu.wisc.regfixer.parser.PlusNode;
import edu.wisc.regfixer.parser.RegexNode;
import edu.wisc.regfixer.parser.RepetitionNode;
import edu.wisc.regfixer.parser.StarNode;
import edu.wisc.regfixer.parser.UnionNode;
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

  private static int nextHoleId = 0;

  public Automaton (RegexNode tree) throws TimeoutException {
    Automaton.nextHoleId = 0;
    this.sfa = nodeToAutomaton(tree).sfa;
  }

  public Automaton (CharPred predicate) throws TimeoutException {
    this.sfa = predicateToSFA(predicate);
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
    Set<State> seen = new HashSet<>(frontier);
    LinkedList<State> toVisit = new LinkedList<>(frontier);

    while (toVisit.size() > 0) {
      State currState = toVisit.removeFirst();
      for (Move<CharPred, Character> move : getMovesFrom(currState.getStateId())) {
        if (move.isEpsilonTransition()) {
          State newState = new State(move.to, currState);
          reached.add(newState);

          if (false == seen.contains(newState)) {
            toVisit.add(newState);
            seen.add(newState);
          }
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

  private Route traceFromState (State endState) {
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

    return new Route(crosses);
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

  public Set<Route> trace (String source) throws TimeoutException {
    List<State> frontier = getEpsClosure(new State(getInitialState()));

    for (int i = 0; i < source.length(); i++) {
      frontier = getNextState(frontier, source.charAt(i));
      frontier = getEpsClosure(frontier);

      if (frontier.isEmpty()) {
        return null;
      }
    }

    return frontier.stream()
      .filter(s -> isFinalState(s.getStateId()))
      .map(s -> traceFromState(s))
      .collect(Collectors.toSet());
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
      return empty();
    }

    Automaton whole = null;

    for (Automaton next : automata) {
      if (whole == null) {
        whole = next;
      } else {
        whole = concatenate(whole, next);
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

  /**
   * METHODS FOR CONVERTING FROM REGEX -> AUTOMATON
   */

  private static Automaton nodeToAutomaton (RegexNode node) throws TimeoutException {
         if (node instanceof ConcatNode)       return concatToAutomaton((ConcatNode) node);
    else if (node instanceof UnionNode)        return unionToAutomaton((UnionNode) node);
    else if (node instanceof RepetitionNode)   return repetitionToAutomaton((RepetitionNode) node);
    else if (node instanceof OptionalNode)     return optionalToAutomaton((OptionalNode) node);
    else if (node instanceof StarNode)         return starToAutomaton((StarNode) node);
    else if (node instanceof PlusNode)         return plusToAutomaton((PlusNode) node);
    else if (node instanceof HoleNode)         return holeToAutomaton((HoleNode) node);
    else if (node instanceof CharClassSetNode) return charClassSetToAutomaton((CharClassSetNode) node);
    else if (node instanceof CharDotNode)      return charDotToAutomaton((CharDotNode) node);
    else if (node instanceof CharEscapedNode)  return charEscapedToAutomaton((CharEscapedNode) node);
    else if (node instanceof CharLiteralNode)  return charLiteralToAutomaton((CharLiteralNode) node);
    else {
      System.err.printf("Unknown AST class: %s\n", node.getClass().getName());
      System.exit(-1);
      return null;
    }
  }

  private static Automaton concatToAutomaton (ConcatNode node) throws TimeoutException {
    List<Automaton> automata = new LinkedList<>();

    for (RegexNode child : node.getChildren()) {
      automata.add(nodeToAutomaton(child));
    }

    return concatenate(automata);
  }

  private static Automaton unionToAutomaton (UnionNode node) throws TimeoutException {
    Automaton left  = nodeToAutomaton(node.getLeftChild());
    Automaton right = nodeToAutomaton(node.getRightChild());
    return union(left, right);
  }

  private static Automaton repetitionToAutomaton (RepetitionNode node) throws TimeoutException {
    if (node.hasMax() && node.getMax() == 0) {
      return empty();
    }

    Automaton sub = nodeToAutomaton(node.getChild());
    Automaton min = empty();

    for (int i = 0; i < node.getMin(); i++) {
      if (i == 0) {
        min = sub;
      } else {
        min = concatenate(min, sub);
      }
    }

    if (node.hasMax() == false) {
      // min to infinite
      Automaton star = star(sub);
      return concatenate(min, star);
    } else if (node.getMin() < node.getMax()) {
      // min to max
      Automaton union = min;
      Automaton whole = min;

      for (int i = node.getMin(); i < node.getMax(); i++) {
        union = concatenate(union, sub);
        whole = union(whole, union);
      }

      return whole;
    } else {
      // just min
      return min;
    }
  }

  private static Automaton optionalToAutomaton (OptionalNode node) throws TimeoutException {
    return union(nodeToAutomaton(node.getChild()), empty());
  }

  private static Automaton starToAutomaton (StarNode node) throws TimeoutException {
    return star(nodeToAutomaton(node.getChild()));
  }

  private static Automaton plusToAutomaton (PlusNode node) throws TimeoutException {
    Automaton sub = nodeToAutomaton(node.getChild());
    return concatenate(sub, star(sub));
  }

  private static Automaton holeToAutomaton (HoleNode node) throws TimeoutException {
    return fromHolePredicate(Automaton.nextHoleId++);
  }

  private static Automaton charClassSetToAutomaton (CharClassSetNode node) throws TimeoutException {
    List<CharPred> predicates = new LinkedList<>();

    for (CharRangeNode charClass : node.getSubClasses()) {
      if (charClass.isSingle()) {
        char ch = charClass.getLeftChild().getChar();
        if (charClass.getLeftChild() instanceof CharEscapedNode) {
          predicates.add(predicateFromMetaChar(ch));
        } else {
          predicates.add(new CharPred(ch));
        }
      } else {
        char leftCh  = charClass.getLeftChild().getChar();
        char rightCh = charClass.getRightChild().getChar();
        predicates.add(new CharPred(leftCh, rightCh));
      }
    }

    if (node.isInverted()) {
      return fromInversePredicates(predicates);
    } else {
      return fromPredicates(predicates);
    }
  }

  private static Automaton charDotToAutomaton (CharDotNode node) throws TimeoutException {
    return fromTruePredicate();
  }

  private static Automaton charEscapedToAutomaton (CharEscapedNode node) throws TimeoutException {
    return fromPredicate(predicateFromMetaChar(node.getChar()));
  }

  private static Automaton charLiteralToAutomaton (CharLiteralNode node) throws TimeoutException {
    return fromPredicate(node.getChar());
  }

  private static CharPred predicateFromMetaChar (char ch) {
    switch (ch) {
      case 't': return new CharPred('\t');
      case 'n': return new CharPred('\n');
      case 'r': return new CharPred('\r');
      case 'f': return new CharPred('\f');
      case 'd': return Automaton.Num;
      case 'D': return Automaton.NotNum;
      case 's': return Automaton.Spaces;
      case 'S': return Automaton.NotSpaces;
      case 'w': return Automaton.Word;
      case 'W': return Automaton.NotWord;
      case 'v': throw new UnsupportedOperationException();
      case 'b': throw new UnsupportedOperationException();
      case 'B': throw new UnsupportedOperationException();
      default:  return new CharPred(ch);
    }
  }
}
