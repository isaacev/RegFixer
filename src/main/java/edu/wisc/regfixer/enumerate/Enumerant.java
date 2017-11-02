package edu.wisc.regfixer.enumerate;

import java.util.Arrays;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.regex.Pattern;

import edu.wisc.regfixer.automata.Automaton;
import edu.wisc.regfixer.automata.Route;
import edu.wisc.regfixer.diagnostic.Diagnostic;
import edu.wisc.regfixer.parser.ConcatNode;
import edu.wisc.regfixer.parser.OptionalNode;
import edu.wisc.regfixer.parser.PlusNode;
import edu.wisc.regfixer.parser.RegexNode;
import edu.wisc.regfixer.parser.StarNode;
import edu.wisc.regfixer.parser.UnionNode;
import edu.wisc.regfixer.synthesize.Synthesis;
import edu.wisc.regfixer.synthesize.SynthesisFailure;
import org.sat4j.specs.TimeoutException;

public class Enumerant implements Comparable<Enumerant> {
  public final static int UNION_COST    = 1;
  public final static int OPTIONAL_COST = 1;
  public final static int STAR_COST     = 3;
  public final static int PLUS_COST     = 2;
  public final static int CONCAT_COST   = 1;

  private final RegexNode tree;
  private final Map<UnknownId, UnknownNode> unknowns;
  private final int cost;
  private final UnknownNode.ExpansionChoice expansion;

  public Enumerant (RegexNode tree, UnknownNode unknown, int cost, UnknownNode.ExpansionChoice expansion) {
    this(tree, Arrays.asList(unknown), cost, expansion);
  }

  public Enumerant (RegexNode tree, Collection<UnknownNode> unknowns, int cost, UnknownNode.ExpansionChoice expansion) {
    Map<UnknownId, UnknownNode> map = new HashMap<>();

    for (UnknownNode unknown : unknowns) {
      map.put(unknown.getId(), unknown);
    }

    this.tree = tree;
    this.unknowns = map;
    this.cost = cost;
    this.expansion = expansion;
  }

  public Enumerant (RegexNode tree, Map<UnknownId, UnknownNode> unknowns, int cost) {
    this.tree = tree;
    this.unknowns = unknowns;
    this.cost = cost;
    this.expansion = null;
  }

  public RegexNode getTree () {
    return this.tree;
  }

  public Set<UnknownNode> getUnknowns () {
    return new HashSet<UnknownNode>(this.unknowns.values());
  }

  public UnknownNode getUnknown (UnknownId id) {
    return this.unknowns.get(id);
  }

  public boolean hasUnknown (UnknownNode unknown) {
    return this.hasUnknown(unknown.getId());
  }

  public boolean hasUnknown (UnknownId id) {
    return this.unknowns.containsKey(id);
  }

  public int getCost () {
    return this.cost;
  }

  public UnknownNode.ExpansionChoice getExpansionChoice () {
    return this.expansion;
  }

  public Pattern toPattern (UnknownNode.FillType type) {
    for (UnknownNode unknown : this.unknowns.values()) {
      unknown.fill(type);
    }

    Pattern pattern = Pattern.compile(String.format("^%s$", this.tree));

    for (UnknownNode unknown : this.unknowns.values()) {
      unknown.clear();
    }

    return pattern;
  }

  public List<Enumerant> expand () {
    List<Enumerant> expansions = new LinkedList<>();

    for (UnknownNode unknown : this.unknowns.values()) {
      expansions.add(this.expandWithUnion(unknown));

      if (unknown.canInsertQuantifierNodes()) {
        expansions.add(this.expandWithOptional(unknown));
        expansions.add(this.expandWithStar(unknown));
        expansions.add(this.expandWithPlus(unknown));
      }

      expansions.add(this.expandWithConcat(unknown));
    }

    return expansions;
  }

  private Enumerant expandWithUnion (UnknownNode unknown) {
    UnknownNode unknown1 = unknown.expand(UnknownNode.ExpansionChoice.Union);
    UnknownNode unknown2 = unknown.expand(UnknownNode.ExpansionChoice.Union);
    List<UnknownNode> newUnknowns = Arrays.asList(unknown1, unknown2);
    RegexNode newTree = new UnionNode(unknown1, unknown2);
    Enumerant twig = new Enumerant(newTree, newUnknowns, Enumerant.UNION_COST, UnknownNode.ExpansionChoice.Union);
    return Grafter.graft(this, unknown, twig, UnknownNode.ExpansionChoice.Union);
  }

  private Enumerant expandWithOptional (UnknownNode unknown) {
    UnknownNode newUnknown = unknown.expand(UnknownNode.ExpansionChoice.Optional);
    RegexNode newTree = new OptionalNode(newUnknown);
    Enumerant twig = new Enumerant(newTree, newUnknown, Enumerant.OPTIONAL_COST, UnknownNode.ExpansionChoice.Optional);
    return Grafter.graft(this, unknown, twig, UnknownNode.ExpansionChoice.Optional);
  }

  private Enumerant expandWithStar (UnknownNode unknown) {
    UnknownNode newUnknown = unknown.expand(UnknownNode.ExpansionChoice.Star);
    RegexNode newTree = new StarNode(newUnknown);
    Enumerant twig = new Enumerant(newTree, newUnknown, Enumerant.STAR_COST, UnknownNode.ExpansionChoice.Star);
    return Grafter.graft(this, unknown, twig, UnknownNode.ExpansionChoice.Star);
  }

  private Enumerant expandWithPlus (UnknownNode unknown) {
    UnknownNode newUnknown = unknown.expand(UnknownNode.ExpansionChoice.Plus);
    RegexNode newTree = new PlusNode(newUnknown);
    Enumerant twig = new Enumerant(newTree, newUnknown, Enumerant.PLUS_COST, UnknownNode.ExpansionChoice.Plus);
    return Grafter.graft(this, unknown, twig, UnknownNode.ExpansionChoice.Plus);
  }

  private Enumerant expandWithConcat (UnknownNode unknown) {
    UnknownNode unknown1 = unknown.expand(UnknownNode.ExpansionChoice.Concat);
    UnknownNode unknown2 = unknown.expand(UnknownNode.ExpansionChoice.Concat);
    List<UnknownNode> newUnknowns = Arrays.asList(unknown1, unknown2);
    RegexNode newTree = new ConcatNode(new LinkedList<RegexNode>(newUnknowns));
    Enumerant twig = new Enumerant(newTree, newUnknowns, Enumerant.CONCAT_COST, UnknownNode.ExpansionChoice.Concat);
    return Grafter.graft(this, unknown, twig, UnknownNode.ExpansionChoice.Concat);
  }

  public Synthesis synthesize (Set<String> p, Set<String> n) throws SynthesisFailure {
    return this.synthesize(p, n, new Diagnostic());
  }

  public Synthesis synthesize (Set<String> p, Set<String> n, Diagnostic diag) throws SynthesisFailure {
    Automaton automaton = null;

    try {
      automaton = new Automaton(this.tree);
    } catch (TimeoutException ex) {
      String fmt = "timed-out building automaton for `%s`";
      throw new SynthesisFailure(String.format(fmt, this.tree));
    }

    List<Set<Route>> positiveRuns = new LinkedList<>();
    List<Set<Route>> negativeRuns = new LinkedList<>();

    try {
      for (String source : p) {
        positiveRuns.add(automaton.trace(source));
      }

      for (String source : n) {
        negativeRuns.add(automaton.trace(source));
      }
    } catch (TimeoutException ex) {
      String fmt = "timed-out computing runs for `%s`";
      throw new SynthesisFailure(String.format(fmt, this.tree));
    }

    return new Synthesis(this, positiveRuns, negativeRuns, diag);
  }

  @Override
  public int compareTo (Enumerant other) {
    return Integer.compare(this.getCost(), other.getCost());
  }

  @Override
  public String toString () {
    return this.tree.toString();
  }
}
