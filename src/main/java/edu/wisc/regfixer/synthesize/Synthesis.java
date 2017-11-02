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
import edu.wisc.regfixer.enumerate.HoleId;
import edu.wisc.regfixer.enumerate.HoleNode;
import edu.wisc.regfixer.parser.CharClass;
import edu.wisc.regfixer.parser.RegexNode;

public class Synthesis {
  private RegexNode tree;

  public Synthesis (Enumerant enumerant, List<Set<Route>> positives, List<Set<Route>> negatives) throws SynthesisFailure {
    this(enumerant, positives, negatives, new Diagnostic());
  }

  public Synthesis (Enumerant enumerant, List<Set<Route>> positives, List<Set<Route>> negatives, Diagnostic diag) throws SynthesisFailure {
    Formula formula = new Formula(positives, negatives, diag);
    Map<HoleId, CharClass> holeSolutions = formula.solve();

    if (holeSolutions.size() != enumerant.getHoles().size()) {
      throw new SynthesisFailure("no solution for some holes");
    }

    RegexNode solution = enumerant.getTree();

    for (Entry<HoleId, CharClass> holeSolution : holeSolutions.entrySet()) {
      HoleNode hole = enumerant.getHole(holeSolution.getKey());
      RegexNode twig = holeSolution.getValue();
      solution = Grafter.graft(solution, hole, twig);
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
