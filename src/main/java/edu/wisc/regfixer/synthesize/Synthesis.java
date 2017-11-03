package edu.wisc.regfixer.synthesize;

import java.util.List;
import java.util.Map.Entry;
import java.util.Map;
import java.util.Set;
import java.util.regex.Pattern;

import edu.wisc.regfixer.automata.Route;
import edu.wisc.regfixer.diagnostic.Diagnostic;
import edu.wisc.regfixer.enumerate.Enumerant;
import edu.wisc.regfixer.enumerate.Grafter;
import edu.wisc.regfixer.enumerate.Unknown;
import edu.wisc.regfixer.enumerate.UnknownId;
import edu.wisc.regfixer.enumerate.UnknownNode;
import edu.wisc.regfixer.parser.CharClass;
import edu.wisc.regfixer.parser.RegexNode;

public class Synthesis {
  private RegexNode tree;

  public Synthesis (Enumerant enumerant, List<Set<Route>> positives, List<Set<Route>> negatives) throws SynthesisFailure {
    this(enumerant, positives, negatives, new Diagnostic());
  }

  public Synthesis (Enumerant enumerant, List<Set<Route>> positives, List<Set<Route>> negatives, Diagnostic diag) throws SynthesisFailure {
    Formula formula = new Formula(positives, negatives, diag);
    Map<UnknownId, CharClass> unknownSolutions = formula.solve();

    if (unknownSolutions.size() != enumerant.getUnknowns().size()) {
      throw new SynthesisFailure("no solution for some unknowns");
    }

    RegexNode solution = enumerant.getTree();

    for (Entry<UnknownId, CharClass> unknownSolution : unknownSolutions.entrySet()) {
      Unknown unknown = enumerant.getUnknown(unknownSolution.getKey());
      RegexNode twig = unknownSolution.getValue();

      if (unknown instanceof UnknownNode) {
        solution = Grafter.graft(solution, (UnknownNode)unknown, twig);
      }
    }

    this.tree = solution;
  }

  public RegexNode getTree () {
    return this.tree;
  }

  public Pattern toPattern (boolean withAnchors) {
    if (withAnchors) {
      return Pattern.compile(String.format("^%s$", this.tree));
    }

    return this.toPattern();
  }

  public Pattern toPattern () {
    return Pattern.compile(this.tree.toString());
  }

  @Override
  public String toString () {
    return this.tree.toString();
  }
}
