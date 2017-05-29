package edu.wisc.regfixer.synthesize;

import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import com.microsoft.z3.BoolExpr;
import com.microsoft.z3.Context;
import com.microsoft.z3.Model;
import com.microsoft.z3.Optimize;
import com.microsoft.z3.Status;
import edu.wisc.regfixer.automata.Route;
import edu.wisc.regfixer.enumerate.HoleId;
import edu.wisc.regfixer.parser.CharClass;
import edu.wisc.regfixer.parser.CharClassSetNode;
import edu.wisc.regfixer.parser.CharDotNode;
import edu.wisc.regfixer.parser.CharEscapedNode;
import edu.wisc.regfixer.parser.CharLiteralNode;
import edu.wisc.regfixer.parser.CharLiteralNode;
import edu.wisc.regfixer.parser.CharRangeNode;
import edu.wisc.regfixer.parser.ConcreteCharClass;

public class CharClassSolver {
  public static Map<HoleId, CharClass> solve (Formula formula) throws SynthesisFailure {
    if (formula.isUnSatisfiable()) {
      throw new SynthesisFailure("unsatisfiable SAT formula");
    }

    Map<HoleId, Set<CharClass>> candidates = new HashMap<>();
    for (BoolExpr var : formula.getVariables()) {
      if (formula.variableEvaluatesTrue(var)) {
        HoleId holeId = formula.getHoleIdForVariable(var);
        CharClass charClass = formula.getCharClassForVariable(var);

        if (candidates.containsKey(holeId) == false) {
          candidates.put(holeId, new HashSet<CharClass>());
        }

        candidates.get(holeId).add(charClass);
      }
    }

    Map<HoleId, CharClass> solution = new HashMap<>();
    for (HoleId holeId : candidates.keySet()) {
      solution.put(holeId, maximizeCharClasses(candidates.get(holeId)));
    }

    return solution;
  }

  private static CharClass maximizeCharClasses (Set<CharClass> classes) throws SynthesisFailure {
    if (classes.contains(new CharEscapedNode('d'))) { return new CharEscapedNode('d'); }
    if (classes.contains(new CharEscapedNode('D'))) { return new CharEscapedNode('D'); }
    if (classes.contains(new CharEscapedNode('s'))) { return new CharEscapedNode('s'); }
    if (classes.contains(new CharEscapedNode('S'))) { return new CharEscapedNode('S'); }
    if (classes.contains(new CharEscapedNode('w'))) { return new CharEscapedNode('w'); }
    if (classes.contains(new CharEscapedNode('W'))) { return new CharEscapedNode('W'); }

    if (classes.size() > 1) {
      if (classes.stream().noneMatch(c -> c instanceof ConcreteCharClass)) {
        throw new SynthesisFailure("cannot combine into a single character class");
      }

      List<CharRangeNode> ranges = classes.stream()
        .map(c -> new CharRangeNode((ConcreteCharClass) c))
        .collect(Collectors.toList());

      return new CharClassSetNode(false, ranges);
    } else if (classes.size() == 1) {
      return (new LinkedList<>(classes)).get(0);
    }

    throw new SynthesisFailure("no character classes");
  }
}
