package edu.wisc.regfixer.enumerate;

import java.util.Arrays;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.regex.Pattern;
import java.util.Set;

import edu.wisc.regfixer.automata.Automaton;
import edu.wisc.regfixer.automata.Route;
import edu.wisc.regfixer.diagnostic.Diagnostic;
import edu.wisc.regfixer.parser.Bounds;
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
  private final Map<UnknownId, Unknown> unknowns;
  private final int cost;
  private final Expansion expansion;

  public Enumerant (RegexNode tree, Unknown unknown, int cost, Expansion expansion) {
    this(tree, Arrays.asList(unknown), cost, expansion);
  }

  public Enumerant (RegexNode tree, Collection<Unknown> unknowns, int cost, Expansion expansion) {
    Map<UnknownId, Unknown> map = new HashMap<>();

    for (Unknown unknown : unknowns) {
      map.put(unknown.getId(), unknown);
    }

    this.tree = tree;
    this.unknowns = map;
    this.cost = cost;
    this.expansion = expansion;
  }

  public Enumerant (RegexNode tree, Map<UnknownId, Unknown> unknowns, int cost) {
    this.tree = tree;
    this.unknowns = unknowns;
    this.cost = cost;
    this.expansion = null;
  }

  public RegexNode getTree () {
    return this.tree;
  }

  public Set<Unknown> getUnknowns () {
    return new HashSet<Unknown>(this.unknowns.values());
  }

  public Unknown getUnknown (UnknownId id) {
    return this.unknowns.get(id);
  }

  public boolean hasUnknown (Unknown unknown) {
    return this.hasUnknown(unknown.getId());
  }

  public boolean hasUnknown (UnknownId id) {
    return this.unknowns.containsKey(id);
  }

  public int getCost () {
    return this.cost;
  }

  public Expansion getExpansion () {
    return this.expansion;
  }

  public Pattern toPattern (UnknownChar.FillType type) {
    for (Unknown unknown : this.unknowns.values()) {
      if (unknown instanceof UnknownChar) {
        ((UnknownChar)unknown).fill(type);
      } else if (unknown instanceof UnknownBounds) {
        ((UnknownBounds)unknown).fill(Bounds.atLeast(0));
      }
    }

    Pattern pattern = Pattern.compile(String.format("^%s$", this.tree));

    for (Unknown unknown : this.unknowns.values()) {
      if (unknown instanceof UnknownChar) {
        ((UnknownChar)unknown).clear();
      }
    }

    return pattern;
  }

  public List<Enumerant> expand () {
    List<Enumerant> expansions = new LinkedList<>();

    for (Unknown u : this.unknowns.values()) {
      if (u instanceof UnknownChar) {
        UnknownChar unknown = (UnknownChar)u;

        expansions.add(this.expandWithUnion(unknown));

        if (unknown.canInsertQuantifierNodes()) {
          expansions.add(this.expandWithOptional(unknown));
          expansions.add(this.expandWithStar(unknown));
          expansions.add(this.expandWithPlus(unknown));
        }

        expansions.add(this.expandWithConcat(unknown));
      }
    }

    return expansions;
  }

  private Enumerant expandWithUnion (UnknownChar unknown) {
    UnknownChar unknown1 = unknown.expand(Expansion.Union);
    UnknownChar unknown2 = unknown.expand(Expansion.Union);
    List<Unknown> newUnknowns = Arrays.asList(unknown1, unknown2);
    RegexNode newTree = new UnionNode(unknown1, unknown2);
    Enumerant twig = new Enumerant(newTree, newUnknowns, Enumerant.UNION_COST, Expansion.Union);
    return Grafter.graft(this, unknown, twig, Expansion.Union);
  }

  private Enumerant expandWithOptional (UnknownChar unknown) {
    UnknownChar newUnknown = unknown.expand(Expansion.Optional);
    RegexNode newTree = new OptionalNode(newUnknown);
    Enumerant twig = new Enumerant(newTree, newUnknown, Enumerant.OPTIONAL_COST, Expansion.Optional);
    return Grafter.graft(this, unknown, twig, Expansion.Optional);
  }

  private Enumerant expandWithStar (UnknownChar unknown) {
    UnknownChar newUnknown = unknown.expand(Expansion.Star);
    RegexNode newTree = new StarNode(newUnknown);
    Enumerant twig = new Enumerant(newTree, newUnknown, Enumerant.STAR_COST, Expansion.Star);
    return Grafter.graft(this, unknown, twig, Expansion.Star);
  }

  private Enumerant expandWithPlus (UnknownChar unknown) {
    UnknownChar newUnknown = unknown.expand(Expansion.Plus);
    RegexNode newTree = new PlusNode(newUnknown);
    Enumerant twig = new Enumerant(newTree, newUnknown, Enumerant.PLUS_COST, Expansion.Plus);
    return Grafter.graft(this, unknown, twig, Expansion.Plus);
  }

  private Enumerant expandWithConcat (UnknownChar unknown) {
    UnknownChar unknown1 = unknown.expand(Expansion.Concat);
    UnknownChar unknown2 = unknown.expand(Expansion.Concat);
    List<UnknownChar> newUnknownChars = Arrays.asList(unknown1, unknown2);
    List<Unknown> newUnknowns = Arrays.asList((Unknown)unknown1, (Unknown)unknown2);
    RegexNode newTree = new ConcatNode(new LinkedList<RegexNode>(newUnknownChars));
    Enumerant twig = new Enumerant(newTree, newUnknowns, Enumerant.CONCAT_COST, Expansion.Concat);
    return Grafter.graft(this, unknown, twig, Expansion.Concat);
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
