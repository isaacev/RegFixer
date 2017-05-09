package edu.wisc.regfixer.synthesize;

import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.regex.Pattern;

import edu.wisc.regfixer.automata.Route;
import edu.wisc.regfixer.parser.RegexNode;

public class Synthesis {
  private RegexNode tree;

  public Synthesis (RegexNode tree, List<Set<Route>> positiveRuns, List<Set<Route>> negativeRuns) throws SynthesisFailure {
    this.tree = tree;
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
