package edu.wisc.regfixer.enumerate;

import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.regex.Pattern;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.TreeSet;

import edu.wisc.regfixer.automata.Automaton;
import edu.wisc.regfixer.automata.Route;
import edu.wisc.regfixer.diagnostic.Diagnostic;
import edu.wisc.regfixer.parser.Bounds;
import edu.wisc.regfixer.parser.ConcatNode;
import edu.wisc.regfixer.parser.OptionalNode;
import edu.wisc.regfixer.parser.PlusNode;
import edu.wisc.regfixer.parser.RegexNode;
import edu.wisc.regfixer.parser.RepetitionNode;
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
  public final static int REPEAT_COST   = 1;

  private final RegexNode tree;
  private final Set<UnknownId> ids;
  private final int cost;
  private final Expansion latest;

  @FunctionalInterface
  public static interface ExpansionFunction {
    Enumerant apply(UnknownChar unknown) throws ForbiddenExpansionException;
  }

  @FunctionalInterface
  public static interface MultExpansionFunction {
    Enumerant apply(Collection<UnknownChar> unknowns) throws ForbiddenExpansionException;
  }

  public Enumerant (RegexNode tree, UnknownId id, int cost, Expansion latest) {
    this(tree, Arrays.asList(id), cost, latest);
  }

  public Enumerant (RegexNode tree, Collection<UnknownId> ids, int cost, Expansion latest) {
    this.tree = tree;
    this.ids = new HashSet<>(ids);
    this.cost = cost;
    this.latest = latest;
  }

  public RegexNode getTree () {
    return this.tree;
  }

  public Set<UnknownId> getIds () {
    return this.ids;
  }

  public boolean hasUnknownId (UnknownId id) {
    return this.ids.contains(id);
  }

  public int getCost () {
    return this.cost;
  }

  public Expansion getLatestExpansion () {
    return this.latest;
  }

  public Pattern toPattern (UnknownChar.FillType type) {
    // Set temporary values for unknowns.
    UnknownChar.setFill(type);

    if (type == UnknownChar.FillType.EmptySet) {
      UnknownBounds.setFill(Bounds.exactly(0));
    } else {
      UnknownBounds.setFill();
    }

    // Build the pattern with temporary values replacing unknowns.
    Pattern pattern = Pattern.compile(String.format("^%s$", this.tree));

    // Clear the temporary values.
    UnknownChar.clearFill();
    UnknownBounds.clearFill();

    return pattern;
  }

  public List<Enumerant> expand () {
    List<Enumerant> expansions = new LinkedList<>();

    // Create a sorted list of UnknownChar's from youngest -> oldest.
    TreeSet<UnknownChar> unknowns = new TreeSet<UnknownChar>(this.ids
      .stream()
      .filter(id -> id.getUnknown() instanceof UnknownChar)
      .map(id -> (UnknownChar)id.getUnknown())
      .collect(Collectors.toSet()));

    // Replace unknown character classes with more complex expressions.
    for (UnknownChar unknown : unknowns) {
      if (unknown.isFrozen()) {
        continue;
      }

      // Perform expansion converting unknown char -> union, quantifier, and concat.
      this.addExpansion(expansions, unknown, this::expandWithUnion);
      if (unknown.canInsertQuantifierNodes()) {
        this.addExpansion(expansions, unknown, this::expandWithUnknownQuantifier);
      }
      this.addExpansion(expansions, unknown, this::expandWithConcat);

      // Freeze any unfrozen unknown character classes younger than the current unknown.
      TreeSet<UnknownChar> toFreeze = new TreeSet<UnknownChar>(Collections.reverseOrder());
      for (UnknownChar freezable : unknowns) {
        if (freezable.getAge() >= unknown.getAge()) {
          break;
        } else if (freezable.isFrozen() == false && freezable.getAge() < unknown.getAge()) {
          toFreeze.add(freezable);
        }
      }
      if (toFreeze.size() > 0) {
        this.addExpansion(expansions, toFreeze, this::expandWithFrozenUnknown);
      }
    }

    return expansions;
  }

  private void addExpansion (List<Enumerant> expansions, UnknownChar unknown, ExpansionFunction expander) {
    Enumerant expansion = null;
    try {
      expansion = expander.apply(unknown);
    } catch (ForbiddenExpansionException ex) {
      return;
    }

    if (expansion != null) {
      expansions.add(expansion);
    }
  }

  private void addExpansion (List<Enumerant> expansions, Collection<UnknownChar> unknowns, MultExpansionFunction expander) {
    Enumerant expansion = null;
    try {
      expansion = expander.apply(unknowns);
    } catch (ForbiddenExpansionException ex) {
      return;
    }

    if (expansion != null) {
      expansions.add(expansion);
    }
  }

  private Enumerant expandWithUnion (UnknownChar unknown) throws ForbiddenExpansionException {
    // Create both unknown chars to be added to the regex tree.
    UnknownChar un1 = new UnknownChar(unknown.getHistory(), Expansion.SyntheticUnion);
    UnknownChar un2 = new UnknownChar(unknown.getHistory(), Expansion.SyntheticUnion);

    // Create union node to added in place of the given 'unknown'.
    RegexNode scion = new UnionNode(un1, un2, true);

    // Graft scion onto the root regex tree.
    RegexNode root = Grafter.graft(this.tree, unknown.getId(), scion);

    // Build set of IDs custom to the new enumerant.
    Set<UnknownId> ids = new HashSet<>();
    ids.addAll(this.getIds());
    ids.remove(unknown.getId());
    ids.add(un1.getId());
    ids.add(un2.getId());

    // Add cost of the expansion.
    int cost = this.getCost() + Enumerant.UNION_COST;

    // Build components into new enumerant.
    return new Enumerant(root, ids, cost, Expansion.SyntheticUnion);
  }

  private Enumerant expandWithUnknownQuantifier (UnknownChar unknown) throws ForbiddenExpansionException {
    // Create an unknown char to be added to the regex tree.
    UnknownChar child = new UnknownChar(unknown.getHistory(), Expansion.Repeat);
    UnknownBounds bounds = new UnknownBounds();

    // Create unknown node to add in place of the given 'unknown'.
    RegexNode scion = new RepetitionNode(child, bounds);

    // Graft scion onto the root regex tree.
    RegexNode root = Grafter.graftWithUnknownAncestors(this.tree, unknown.getId(), scion);

    // Build set of IDs custom to the new enumerant.
    Set<UnknownId> ids = new HashSet<>();
    ids.addAll(this.getIds());
    ids.remove(unknown.getId());
    ids.add(child.getId());
    ids.add(bounds.getId());
    ids.addAll(Grafter.addedBounds);

    // Add cost of the expansion.
    int cost = this.getCost() + Enumerant.REPEAT_COST;

    // Build components into new enumerant.
    return new Enumerant(root, ids, cost, Expansion.Repeat);
  }

  private Enumerant expandWithConcat (UnknownChar unknown) throws ForbiddenExpansionException {
    // Create both unknown chars to be added to the regex tree.
    UnknownChar un1 = new UnknownChar(unknown.getHistory(), Expansion.Concat);
    UnknownChar un2 = new UnknownChar(unknown.getHistory(), Expansion.Concat);

    // Create concatenation node to added in place of the given 'unknown'.
    RegexNode scion = new ConcatNode(un1, un2);

    // Graft scion onto the root regex tree.
    RegexNode root = Grafter.graft(this.tree, unknown.getId(), scion);

    // Build set of IDs custom to the new enumerant.
    Set<UnknownId> ids = new HashSet<>();
    ids.addAll(this.getIds());
    ids.remove(unknown.getId());
    ids.add(un1.getId());
    ids.add(un2.getId());

    // Add cost of the expansion.
    int cost = this.getCost() + Enumerant.CONCAT_COST;

    // Build components into new enumerant.
    return new Enumerant(root, ids, cost, Expansion.Concat);
  }

  private Enumerant expandWithFrozenUnknown (Collection<UnknownChar> unknowns) throws ForbiddenExpansionException {
    RegexNode root = this.tree;
    Set<UnknownId> ids = new HashSet<>(this.ids);
    for (UnknownChar unknown : unknowns) {
      UnknownChar frozenUnknown = new UnknownChar(unknown.getHistory());
      frozenUnknown.freeze();
      root = Grafter.graft(root, unknown.getId(), frozenUnknown);

      ids.remove(unknown.getId());
      ids.add(frozenUnknown.getId());
    }

    return new Enumerant(root, ids, this.getCost(), Expansion.Freeze);
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

    int totalRuns = positiveRuns.size() + negativeRuns.size();
    if (diag.getInt("maximumRoutes") < totalRuns) {
      diag.registry().setInt("maximumRoutes", totalRuns);
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
