package edu.wisc.regfixer.enumerate;

import java.util.Arrays;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.regex.Pattern;

import edu.wisc.regfixer.automata.Automaton;
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
  private final RegexNode tree;
  private final List<HoleNode> holes;
  private final int cost;

  public Enumerant (RegexNode tree, Enumerant other) {
    this(tree, other.holes, other.cost);
  }

  public Enumerant (HoleNode hole, int cost) {
    this(hole, hole, cost);
  }

  public Enumerant (RegexNode tree, HoleNode hole, int cost) {
    this(tree, Arrays.asList(hole), cost);
  }

  public Enumerant (RegexNode tree, List<HoleNode> holes, int cost) {
    this.tree = tree;
    this.holes = holes;
    this.cost = cost;
  }

  public RegexNode getTree () {
    return this.tree;
  }

  public List<HoleNode> getHoles () {
    return this.holes;
  }

  public int getCost () {
    return this.cost;
  }

  public Pattern toPattern (HoleNode.FillType type) {
    for (HoleNode hole : this.holes) {
      hole.fill(type);
    }

    Pattern pattern = Pattern.compile(String.format("^%s$", this.tree));

    for (HoleNode hole : this.holes) {
      hole.clear();
    }

    return pattern;
  }

  public List<Enumerant> expand () {
    List<Enumerant> expansions = new LinkedList<>();

    for (HoleNode hole : this.holes) {
      expansions.add(this.expandWithUnion(hole));
      expansions.add(this.expandWithOptional(hole));
      expansions.add(this.expandWithStar(hole));
      expansions.add(this.expandWithPlus(hole));
      expansions.add(this.expandWithConcat(hole));
    }

    return expansions;
  }

  private Enumerant expandWithUnion (HoleNode hole) {
    List<HoleNode> newHoles = Arrays.asList(new HoleNode(), new HoleNode());
    RegexNode newTree = new UnionNode(newHoles.get(0), newHoles.get(1));
    Enumerant twig = new Enumerant(newTree, newHoles, 1);
    return Grafter.graft(this, hole, twig);
  }

  private Enumerant expandWithOptional (HoleNode hole) {
    HoleNode newHole = new HoleNode();
    RegexNode newTree = new OptionalNode(newHole);
    Enumerant twig = new Enumerant(newTree, newHole, 1);
    return Grafter.graft(this, hole, twig);
  }

  private Enumerant expandWithStar (HoleNode hole) {
    HoleNode newHole = new HoleNode();
    RegexNode newTree = new StarNode(newHole);
    Enumerant twig = new Enumerant(newTree, newHole, 1);
    return Grafter.graft(this, hole, twig);
  }

  private Enumerant expandWithPlus (HoleNode hole) {
    HoleNode newHole = new HoleNode();
    RegexNode newTree = new PlusNode(newHole);
    Enumerant twig = new Enumerant(newTree, newHole, 1);
    return Grafter.graft(this, hole, twig);
  }

  private Enumerant expandWithConcat (HoleNode hole) {
    List<HoleNode> newHoles = Arrays.asList(new HoleNode(), new HoleNode());
    RegexNode newTree = new ConcatNode(new LinkedList<RegexNode>(newHoles));
    Enumerant twig = new Enumerant(newTree, newHoles, 1);
    return Grafter.graft(this, hole, twig);
  }

  public Synthesis synthesize (Corpus corpus) throws SynthesisFailure {
    Automaton automaton = null;

    try {
      automaton = new Automaton(this.tree);
    } catch (TimeoutException ex) {
      String fmt = "timed-out building automaton for `%s`";
      throw new SynthesisFailure(String.format(fmt, this.tree));
    }

    Map<String, List<Map<Integer, Set<Character>>>> positiveRuns, negativeRuns;

    try {
      positiveRuns = automaton.computeRuns(corpus.getPositiveExamples());
      negativeRuns = automaton.computeRuns(corpus.getNegativeExamples());
    } catch (TimeoutException ex) {
      String fmt = "timed-out computing runs for `%s`";
      throw new SynthesisFailure(String.format(fmt, this.tree));
    }

    return new Synthesis(this.tree, positiveRuns, negativeRuns);
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
