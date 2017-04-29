package edu.wisc.regfixer.automata;

import java.util.LinkedList;
import java.util.List;

import edu.wisc.regfixer.parser.CharClassSetNode;
import edu.wisc.regfixer.parser.CharDotNode;
import edu.wisc.regfixer.parser.CharEscapedNode;
import edu.wisc.regfixer.parser.CharLiteralNode;
import edu.wisc.regfixer.parser.CharRangeNode;
import edu.wisc.regfixer.parser.ConcatNode;
import edu.wisc.regfixer.fixer.HoleNode;
import edu.wisc.regfixer.parser.OptionalNode;
import edu.wisc.regfixer.parser.PlusNode;
import edu.wisc.regfixer.parser.RegexNode;
import edu.wisc.regfixer.parser.RepetitionNode;
import edu.wisc.regfixer.parser.StarNode;
import edu.wisc.regfixer.parser.UnionNode;
import org.sat4j.specs.TimeoutException;
import theory.characters.CharPred;

public class Converter {
  private static int nextHoleId = 0;

  public static Automaton regexToAutomaton (RegexNode node) throws TimeoutException {
    Automaton automaton = nodeToAutomaton(node);
    automaton.setTotalHoles(nextHoleId);

    return automaton;
  }

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

    return Automaton.concatenate(automata);
  }

  private static Automaton unionToAutomaton (UnionNode node) throws TimeoutException {
    Automaton left  = nodeToAutomaton(node.getLeftChild());
    Automaton right = nodeToAutomaton(node.getRightChild());
    return Automaton.union(left, right);
  }

  private static Automaton repetitionToAutomaton (RepetitionNode node) throws TimeoutException {
    if (node.hasMax() && node.getMax() == 0) {
      return Automaton.empty();
    }

    Automaton sub = nodeToAutomaton(node.getChild());
    Automaton min = Automaton.empty();

    for (int i = 0; i < node.getMin(); i++) {
      if (i == 0) {
        min = sub;
      } else {
        min = Automaton.concatenate(min, sub);
      }
    }

    if (node.hasMax() == false) {
      // min to infinite
      Automaton star = Automaton.star(sub);
      return Automaton.concatenate(min, star);
    } else if (node.getMin() < node.getMax()) {
      // min to max
      Automaton union = min;
      Automaton whole = min;

      for (int i = node.getMin(); i < node.getMax(); i++) {
        union = Automaton.concatenate(union, sub);
        whole = Automaton.union(whole, union);
      }

      return whole;
    } else {
      // just min
      return min;
    }
  }

  private static Automaton optionalToAutomaton (OptionalNode node) throws TimeoutException {
    return Automaton.union(nodeToAutomaton(node.getChild()), Automaton.empty());
  }

  private static Automaton starToAutomaton (StarNode node) throws TimeoutException {
    return Automaton.star(nodeToAutomaton(node.getChild()));
  }

  private static Automaton plusToAutomaton (PlusNode node) throws TimeoutException {
    Automaton sub = nodeToAutomaton(node.getChild());
    return Automaton.concatenate(sub, Automaton.star(sub));
  }

  private static Automaton holeToAutomaton (HoleNode node) throws TimeoutException {
    return Automaton.fromHolePredicate(nextHoleId++);
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
      return Automaton.fromInversePredicates(predicates);
    } else {
      return Automaton.fromPredicates(predicates);
    }
  }

  private static Automaton charDotToAutomaton (CharDotNode node) throws TimeoutException {
    return Automaton.fromTruePredicate();
  }

  private static Automaton charEscapedToAutomaton (CharEscapedNode node) throws TimeoutException {
    return Automaton.fromPredicate(predicateFromMetaChar(node.getChar()));
  }

  private static Automaton charLiteralToAutomaton (CharLiteralNode node) throws TimeoutException {
    return Automaton.fromPredicate(node.getChar());
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
