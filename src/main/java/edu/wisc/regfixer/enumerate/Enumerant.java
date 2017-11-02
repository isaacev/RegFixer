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
  private final Map<HoleId, HoleNode> holes;
  private final int cost;
  private final HoleNode.ExpansionChoice expansion;

  public Enumerant (RegexNode tree, HoleNode hole, int cost, HoleNode.ExpansionChoice expansion) {
    this(tree, Arrays.asList(hole), cost, expansion);
  }

  public Enumerant (RegexNode tree, Collection<HoleNode> holes, int cost, HoleNode.ExpansionChoice expansion) {
    Map<HoleId, HoleNode> map = new HashMap<>();

    for (HoleNode hole : holes) {
      map.put(hole.getHoleId(), hole);
    }

    this.tree = tree;
    this.holes = map;
    this.cost = cost;
    this.expansion = expansion;
  }

  public Enumerant (RegexNode tree, Map<HoleId, HoleNode> holes, int cost) {
    this.tree = tree;
    this.holes = holes;
    this.cost = cost;
    this.expansion = null;
  }

  public RegexNode getTree () {
    return this.tree;
  }

  public Set<HoleNode> getHoles () {
    return new HashSet<HoleNode>(this.holes.values());
  }

  public HoleNode getHole (HoleId holeId) {
    return this.holes.get(holeId);
  }

  public boolean hasHole (HoleNode hole) {
    return this.hasHole(hole.getHoleId());
  }

  public boolean hasHole (HoleId holeId) {
    return this.holes.containsKey(holeId);
  }

  public int getCost () {
    return this.cost;
  }

  public HoleNode.ExpansionChoice getExpansionChoice () {
    return this.expansion;
  }

  public Pattern toPattern (HoleNode.FillType type) {
    for (HoleNode hole : this.holes.values()) {
      hole.fill(type);
    }

    Pattern pattern = Pattern.compile(String.format("^%s$", this.tree));

    for (HoleNode hole : this.holes.values()) {
      hole.clear();
    }

    return pattern;
  }

  public List<Enumerant> expand () {
    List<Enumerant> expansions = new LinkedList<>();

    for (HoleNode hole : this.holes.values()) {
      expansions.add(this.expandWithUnion(hole));

      if (hole.canInsertQuantifierNodes()) {
        expansions.add(this.expandWithOptional(hole));
        expansions.add(this.expandWithStar(hole));
        expansions.add(this.expandWithPlus(hole));
      }

      expansions.add(this.expandWithConcat(hole));
    }

    return expansions;
  }

  private Enumerant expandWithUnion (HoleNode hole) {
    HoleNode hole1 = hole.expand(HoleNode.ExpansionChoice.Union);
    HoleNode hole2 = hole.expand(HoleNode.ExpansionChoice.Union);
    List<HoleNode> newHoles = Arrays.asList(hole1, hole2);
    RegexNode newTree = new UnionNode(hole1, hole2);
    Enumerant twig = new Enumerant(newTree, newHoles, Enumerant.UNION_COST, HoleNode.ExpansionChoice.Union);
    return Grafter.graft(this, hole, twig, HoleNode.ExpansionChoice.Union);
  }

  private Enumerant expandWithOptional (HoleNode hole) {
    HoleNode newHole = hole.expand(HoleNode.ExpansionChoice.Optional);
    RegexNode newTree = new OptionalNode(newHole);
    Enumerant twig = new Enumerant(newTree, newHole, Enumerant.OPTIONAL_COST, HoleNode.ExpansionChoice.Optional);
    return Grafter.graft(this, hole, twig, HoleNode.ExpansionChoice.Optional);
  }

  private Enumerant expandWithStar (HoleNode hole) {
    HoleNode newHole = hole.expand(HoleNode.ExpansionChoice.Star);
    RegexNode newTree = new StarNode(newHole);
    Enumerant twig = new Enumerant(newTree, newHole, Enumerant.STAR_COST, HoleNode.ExpansionChoice.Star);
    return Grafter.graft(this, hole, twig, HoleNode.ExpansionChoice.Star);
  }

  private Enumerant expandWithPlus (HoleNode hole) {
    HoleNode newHole = hole.expand(HoleNode.ExpansionChoice.Plus);
    RegexNode newTree = new PlusNode(newHole);
    Enumerant twig = new Enumerant(newTree, newHole, Enumerant.PLUS_COST, HoleNode.ExpansionChoice.Plus);
    return Grafter.graft(this, hole, twig, HoleNode.ExpansionChoice.Plus);
  }

  private Enumerant expandWithConcat (HoleNode hole) {
    HoleNode hole1 = hole.expand(HoleNode.ExpansionChoice.Concat);
    HoleNode hole2 = hole.expand(HoleNode.ExpansionChoice.Concat);
    List<HoleNode> newHoles = Arrays.asList(hole1, hole2);
    RegexNode newTree = new ConcatNode(new LinkedList<RegexNode>(newHoles));
    Enumerant twig = new Enumerant(newTree, newHoles, Enumerant.CONCAT_COST, HoleNode.ExpansionChoice.Concat);
    return Grafter.graft(this, hole, twig, HoleNode.ExpansionChoice.Concat);
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
