package edu.wisc.regfixer.automata;

import java.util.Arrays;
import java.util.LinkedList;
import java.util.List;

import automata.sfa.SFA;
import automata.sfa.SFAInputMove;
import automata.sfa.SFAMove;
import edu.wisc.regfixer.parser.RegexNode;
import edu.wisc.regfixer.parser.CharClass;
import edu.wisc.regfixer.parser.CharClassSetNode;
import edu.wisc.regfixer.parser.CharDotNode;
import edu.wisc.regfixer.parser.CharEscapedNode;
import edu.wisc.regfixer.parser.CharLiteralNode;
import edu.wisc.regfixer.parser.CharRangeNode;
import edu.wisc.regfixer.parser.ConcatNode;
import edu.wisc.regfixer.parser.HoleNode;
import edu.wisc.regfixer.parser.OptionalNode;
import edu.wisc.regfixer.parser.PlusNode;
import edu.wisc.regfixer.parser.RepetitionNode;
import edu.wisc.regfixer.parser.StarNode;
import edu.wisc.regfixer.parser.UnionNode;
import org.sat4j.specs.TimeoutException;
import theory.characters.CharPred;
import theory.characters.StdCharPred;
import theory.intervals.UnaryCharIntervalSolver;

public class Converter {
  public static SFA<CharPred, Character> regexToSFA (RegexNode node) throws TimeoutException {
    UnaryCharIntervalSolver solver = new UnaryCharIntervalSolver();
    return regexToSFA(node, solver);
  }

  public static SFA<CharPred, Character> regexToSFA (RegexNode node, UnaryCharIntervalSolver solver) throws TimeoutException {
    return nodeToSFA(node, solver);
  }

  private static SFA<CharPred, Character> nodeToSFA
      (RegexNode n, UnaryCharIntervalSolver s) throws TimeoutException {
         if (n instanceof ConcatNode)       return concatToSFA((ConcatNode)n, s);
    else if (n instanceof UnionNode)        return unionToSFA((UnionNode)n, s);
    else if (n instanceof RepetitionNode)   return repetitionToSFA((RepetitionNode)n, s);
    else if (n instanceof OptionalNode)     return optionalToSFA((OptionalNode)n, s);
    else if (n instanceof StarNode)         return starToSFA((StarNode)n, s);
    else if (n instanceof PlusNode)         return plusToSFA((PlusNode)n, s);
    else if (n instanceof HoleNode)         return holeToSFA((HoleNode)n, s);
    else if (n instanceof CharClassSetNode) return charClassSetToSFA((CharClassSetNode)n, s);
    else if (n instanceof CharDotNode)      return charDotToSFA((CharDotNode)n, s);
    else if (n instanceof CharEscapedNode)  return charEscapedToSFA((CharEscapedNode)n, s);
    else if (n instanceof CharLiteralNode)  return charLiteralToSFA((CharLiteralNode)n, s);
    else {
      System.err.printf("Unknown AST class: %s\n", n.getClass().getName());
      System.exit(-1);
      return null;
    }
  }

  private static SFA<CharPred, Character> concatToSFA (ConcatNode node, UnaryCharIntervalSolver solver) throws TimeoutException {
/*    SFA<CharPred, Character> subAutomaton = null;

    // Build a composite automaton from the sub-automata derrived from each
    // child node of this concatenation expression.
    for (RegexNode child : node.getChildren()) {
      if (subAutomaton == null) {
        subAutomaton = nodeToSFA(child, solver);
      } else {
        SFA<CharPred, Character> followingAutomata = nodeToSFA(child, solver);
        subAutomaton = SFA.concatenate(subAutomaton, followingAutomata, solver);
      }
    }

    if (subAutomaton == null) {
      return SFA.getEmptySFA(solver);
    } else {
      return subAutomaton;
    }
    */
    SFA<CharPred, Character> iterateSFA = SFA.getEmptySFA(solver);
    System.out.print(iterateSFA.stateCount());
    for(RegexNode child : node.getChildren()) {
      if(iterateSFA.stateCount() == 1) {
        iterateSFA = nodeToSFA(child, solver);
        continue;
      }
      SFA<CharPred, Character> followingSFA = nodeToSFA(child, solver);
      iterateSFA = SFA.concatenate(iterateSFA, followingSFA, solver) ;
    }
    return iterateSFA;
  }

  private static SFA<CharPred, Character> unionToSFA (UnionNode node, UnaryCharIntervalSolver solver) throws TimeoutException {
    SFA<CharPred, Character> leftAutomaton = nodeToSFA(node.getLeftChild(), solver);  // recursively find SFA of child
    SFA<CharPred, Character> rightAutomaton = nodeToSFA(node.getRightChild(), solver);
    SFA<CharPred, Character> unionAutomaton = SFA.union(leftAutomaton, rightAutomaton, solver); // union on left and right child's SFA

    return unionAutomaton;
  }

  private static SFA<CharPred, Character> repetitionToSFA (RepetitionNode node, UnaryCharIntervalSolver solver) throws TimeoutException {
    if (node.hasMax() && node.getMax() == 0) {
      // Special case when max is 0 means the sub expression does not exist.
      return SFA.getEmptySFA(solver);
    }

    // The `subAutomaton` is the automaton derrived just from this node's child expression.
    SFA<CharPred, Character> subAutomaton = nodeToSFA(node.getChild(), solver);

    // The `minAutomaton` is the `subAutomaton` repeated the minimum number of
    // times required by the repetition node.
    SFA<CharPred, Character> minAutomaton = SFA.getEmptySFA(solver);

    // Starts looping at 1 because the first repetition was added above.
    for (int i = 0; i < node.getMin(); i++) {
      if (i == 0) {
        minAutomaton = subAutomaton;
      } else {
        minAutomaton = SFA.concatenate(minAutomaton, subAutomaton, solver);
      }
    }

    if (node.hasMax() == false) {
      // min to infinite
      SFA<CharPred, Character> starAutomaton = SFA.star(subAutomaton, solver);
      SFA<CharPred, Character> wholeAutomaton = SFA.concatenate(minAutomaton, starAutomaton, solver);

      return wholeAutomaton;
    } else if (node.getMin() < node.getMax()) {
      // min to max
      SFA<CharPred, Character> unionAutomaton = minAutomaton;
      SFA<CharPred, Character> wholeAutomaton = minAutomaton;

      for (int i = node.getMin(); i < node.getMax(); i++) {
        unionAutomaton = SFA.concatenate(unionAutomaton, subAutomaton, solver);
        wholeAutomaton = SFA.union(wholeAutomaton, unionAutomaton, solver);
      }

      return wholeAutomaton;
    } else {
      // just min
      return minAutomaton;
    }
  }

  private static SFA<CharPred, Character> optionalToSFA (OptionalNode node, UnaryCharIntervalSolver solver) throws TimeoutException {
    // Build an automaton that accepts the child once...
    SFA<CharPred, Character> subAutomaton = nodeToSFA(node.getChild(), solver);

    // ...and combine that automaton with an automaton that only accepts an empty string.
    List<SFAMove<CharPred, Character>> transitions = new LinkedList<>();
    int initialState = 0;
    List<Integer> finalStates = Arrays.asList(0);
    SFA<CharPred, Character> emptySetAutomaton = SFA.MkSFA(transitions, initialState, finalStates, solver);
    SFA<CharPred, Character> optionalAutomaton = SFA.union(subAutomaton, emptySetAutomaton, solver);

    return optionalAutomaton;
  }

  private static SFA<CharPred, Character> starToSFA (StarNode node, UnaryCharIntervalSolver solver) throws TimeoutException {
    SFA<CharPred, Character> subAutomaton = nodeToSFA(node.getChild(), solver);
    SFA<CharPred, Character> starAutomaton = SFA.star(subAutomaton, solver);

    return starAutomaton;
  }

  private static SFA<CharPred, Character> plusToSFA (PlusNode node, UnaryCharIntervalSolver solver) throws TimeoutException {
    // Concatenate a star and singleton automaton to represent an automaton that
    // matches the subautomaton repeated 1+ times.
    SFA<CharPred, Character> subAutomaton = nodeToSFA(node.getChild(), solver);
    SFA<CharPred, Character> starAutomaton = SFA.star(subAutomaton, solver);
    SFA<CharPred, Character> plusAutomaton = SFA.concatenate(subAutomaton, starAutomaton, solver);

    return plusAutomaton;
  }

  private static SFA<CharPred, Character> holeToSFA (HoleNode node, UnaryCharIntervalSolver solver) throws TimeoutException {
    // TODO
    System.err.println("HoleNode not supported");
    System.exit(-1);
    return null;
  }

  private static SFA<CharPred, Character> charClassSetToSFA (CharClassSetNode node, UnaryCharIntervalSolver solver) throws TimeoutException {
    CharPred wholePredicate = null;

    for (CharRangeNode subClass : node.getSubClasses()) {
      CharPred subPredicate = null;

      if (subClass.isSingle()) {
        char leftCh = subClass.getLeftChild().getChar();

        if (subClass.getLeftChild() instanceof CharEscapedNode) {
          subPredicate = getEscapedCharPredicate(leftCh, solver);
        } else {
          subPredicate = new CharPred(leftCh);
        }
      } else {
        char leftCh = subClass.getLeftChild().getChar();
        char rightCh = subClass.getRightChild().getChar();
        subPredicate = new CharPred(leftCh, rightCh);
      }

      if (wholePredicate == null) {
        wholePredicate = subPredicate;
      } else {
        wholePredicate = solver.MkOr(wholePredicate, subPredicate);
      }
    }

    if (node.isInverted()) {
      wholePredicate = solver.MkNot(wholePredicate);
    }

    return predicateToSFA(wholePredicate, solver);
  }

  private static SFA<CharPred, Character> charDotToSFA (CharDotNode node, UnaryCharIntervalSolver solver) throws TimeoutException {
    // Create an automaton that has a transition which accepts anything.
    CharPred predicate = solver.True();
    return predicateToSFA(predicate, solver);
  }

  private static SFA<CharPred, Character> charEscapedToSFA (CharEscapedNode node, UnaryCharIntervalSolver solver) throws TimeoutException {
    return predicateToSFA(getEscapedCharPredicate(node.getChar(), solver), solver);
  }

  private static SFA<CharPred, Character> charLiteralToSFA (CharLiteralNode node, UnaryCharIntervalSolver solver) throws TimeoutException {
    // Define a transition from states 0 -> 1 that only accepts the character
    // described by this node.
    CharPred predicate = new CharPred(node.getChar());
    return predicateToSFA(predicate, solver);
  }

  private static SFA<CharPred, Character> predicateToSFA (CharPred predicate, UnaryCharIntervalSolver solver) throws TimeoutException {
    // Create a transition between states that is filtered by the given predicate.
    int fromState = 0;
    int toState = 1;
    SFAInputMove<CharPred, Character> transition = new SFAInputMove<>(fromState, toState, predicate);
    List<SFAMove<CharPred, Character>> transitions = Arrays.asList(transition);

    // Build transition list into an automaton.
    int initialState = 0;
    List<Integer> finalStates = Arrays.asList(1);
    SFA<CharPred, Character> automaton = SFA.MkSFA(transitions, initialState, finalStates, solver);

    return automaton;
  }

  private static CharPred getEscapedCharPredicate (char ch, UnaryCharIntervalSolver solver) {
    switch (ch) {
      case 't': return new CharPred('\t');
      case 'n': return new CharPred('\n');
      case 'r': return new CharPred('\r');
      case 'f': return new CharPred('\f');
      case 'd': return StdCharPred.NUM;
      case 'D': return solver.MkNot(StdCharPred.NUM);
      case 's': return StdCharPred.SPACES;
      case 'S': return solver.MkNot(StdCharPred.SPACES);
      case 'w': return StdCharPred.WORD;
      case 'W': return solver.MkNot(StdCharPred.WORD);
      case 'v': throw new UnsupportedOperationException();
      case 'b': throw new UnsupportedOperationException();
      case 'B': throw new UnsupportedOperationException();
      default:  return new CharPred(ch);
    }
  }
}
