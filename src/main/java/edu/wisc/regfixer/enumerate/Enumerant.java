package edu.wisc.regfixer.enumerate;

import java.util.Arrays;
import java.util.Collection;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.regex.Pattern;
import java.util.Set;

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
  private final Set<UnknownId> ids;
  private final int cost;
  private final Expansion latest;

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
    UnknownBounds.setFill();

    // Build the pattern with temporary values replacing unknowns.
    Pattern pattern = Pattern.compile(String.format("^%s$", this.tree));

    // Clear the temporary values.
    UnknownChar.clearFill();
    UnknownBounds.clearFill();

    return pattern;
  }

  public List<Enumerant> expand () {
    List<Enumerant> expansions = new LinkedList<>();

    for (UnknownId id : this.ids) {
      if (id.getUnknown() instanceof UnknownChar) {
        UnknownChar unknown = (UnknownChar)id.getUnknown();
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
    // Create both unknown chars to be added to the regex tree.
    UnknownChar un1 = new UnknownChar(unknown.getHistory(), Expansion.Union);
    UnknownChar un2 = new UnknownChar(unknown.getHistory(), Expansion.Union);

    // Create union node to added in place of the given 'unknown'.
    RegexNode scion = new UnionNode(un1, un2);

    // Graft scion onto the root regex tree.
    RegexNode root = Grafter.graft(this.tree, unknown.getId(), scion);

    // Build set of IDs custom to the new enumerant.
    Set<UnknownId> ids = new HashSet<>();
    ids.addAll(this.getIds());
    ids.remove(unknown.getId());
    ids.add(un1.getId());
    ids.add(un2.getId());

    // Build components into new enumerant.
    return new Enumerant(root, ids, Enumerant.UNION_COST, Expansion.Union);
  }

  private Enumerant expandWithOptional (UnknownChar unknown) {
    // Create an unknown char to be added to the regex tree.
    UnknownChar un = new UnknownChar(unknown.getHistory(), Expansion.Optional);

    // Create optional node to added in place of the given 'unknown'.
    RegexNode scion = new OptionalNode(un);

    // Graft scion onto the root regex tree.
    RegexNode root = Grafter.graft(this.tree, unknown.getId(), scion);

    // Build set of IDs custom to the new enumerant.
    Set<UnknownId> ids = new HashSet<>();
    ids.addAll(this.getIds());
    ids.remove(unknown.getId());
    ids.add(un.getId());

    // Build components into new enumerant.
    return new Enumerant(root, ids, Enumerant.OPTIONAL_COST, Expansion.Optional);
  }

  private Enumerant expandWithStar (UnknownChar unknown) {
    // Create an unknown char to be added to the regex tree.
    UnknownChar un = new UnknownChar(unknown.getHistory(), Expansion.Star);

    // Create star node to added in place of the given 'unknown'.
    RegexNode scion = new StarNode(un);

    // Graft scion onto the root regex tree.
    RegexNode root = Grafter.graft(this.tree, unknown.getId(), scion);

    // Build set of IDs custom to the new enumerant.
    Set<UnknownId> ids = new HashSet<>();
    ids.addAll(this.getIds());
    ids.remove(unknown.getId());
    ids.add(un.getId());

    // Build components into new enumerant.
    return new Enumerant(root, ids, Enumerant.STAR_COST, Expansion.Star);
  }

  private Enumerant expandWithPlus (UnknownChar unknown) {
    // Create an unknown char to be added to the regex tree.
    UnknownChar un = new UnknownChar(unknown.getHistory(), Expansion.Plus);

    // Create plus node to added in place of the given 'unknown'.
    RegexNode scion = new PlusNode(un);

    // Graft scion onto the root regex tree.
    RegexNode root = Grafter.graft(this.tree, unknown.getId(), scion);

    // Build set of IDs custom to the new enumerant.
    Set<UnknownId> ids = new HashSet<>();
    ids.addAll(this.getIds());
    ids.remove(unknown.getId());
    ids.add(un.getId());

    // Build components into new enumerant.
    return new Enumerant(root, ids, Enumerant.PLUS_COST, Expansion.Plus);
  }

  private Enumerant expandWithConcat (UnknownChar unknown) {
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

    // Build components into new enumerant.
    return new Enumerant(root, ids, Enumerant.CONCAT_COST, Expansion.Concat);
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
