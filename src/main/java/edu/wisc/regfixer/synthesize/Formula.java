package edu.wisc.regfixer.synthesize;

import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;

import com.microsoft.z3.ArithExpr;
import com.microsoft.z3.BoolExpr;
import com.microsoft.z3.Context;
import com.microsoft.z3.IntExpr;
import com.microsoft.z3.IntNum;
import com.microsoft.z3.Model;
import com.microsoft.z3.Optimize;
import com.microsoft.z3.Status;
import edu.wisc.regfixer.automata.Route;
import edu.wisc.regfixer.diagnostic.Diagnostic;
import edu.wisc.regfixer.enumerate.UnknownId;
import edu.wisc.regfixer.parser.Bounds;
import edu.wisc.regfixer.parser.CharClass;
import edu.wisc.regfixer.parser.CharClassSetNode;
import edu.wisc.regfixer.parser.CharLiteralNode;
import edu.wisc.regfixer.parser.CharRangeNode;
import edu.wisc.regfixer.parser.ConcreteCharClass;

public class Formula {
  private List<Set<Route>> positives;
  private List<Set<Route>> negatives;
  private Diagnostic diag;

  private Context ctx;
  private Optimize opt;
  private Model model;

  private int nextVarId;
  private Set<UnknownId> unknowns;
  private Map<UnknownId, Set<BoolExpr>> unknownToVars;
  private Map<UnknownId, List<IntExpr>> unknownToWeights;
  private Map<UnknownId, Map<MetaClassTree, BoolExpr>> unknownToTreeToVar;
  private Map<BoolExpr, MetaClassTree> varToTree;
  private Map<BoolExpr, Predicate> varToPred;
  private MetaClassTree tree;
  private Set<MetaClassTree> misc;
  private Map<UnknownId, Map<Character, BoolExpr>> unknownToCharToVar;

  private Set<UnknownId> UnknownBoundss;
  private Map<UnknownId, IntExpr> unknownToMinVar;
  private Map<UnknownId, IntExpr> unknownToMaxVar;

  public Formula (List<Set<Route>> positives, List<Set<Route>> negatives) {
    this(positives, negatives, new Diagnostic());
  }

  public Formula (List<Set<Route>> positives, List<Set<Route>> negatives, Diagnostic diag) {
    this.positives = positives;
    this.negatives = negatives;
    this.diag = diag;

    // Initialize SAT formula objects
    this.ctx = new Context();
    this.opt = this.ctx.mkOptimize();
    this.model = null;

    // Initialize structures for tracking state
    this.nextVarId = 0;
    this.unknowns = new HashSet<>();
    this.unknownToVars = new HashMap<>();
    this.unknownToWeights = new HashMap<>();
    this.unknownToTreeToVar = new HashMap<>();
    this.varToTree = new HashMap<>();
    this.varToPred = new HashMap<>();
    this.tree = MetaClassTree.initialize();
    this.unknownToCharToVar = new HashMap<>();
    this.misc = new HashSet<>();

    this.UnknownBoundss = new HashSet<>();
    this.unknownToMinVar = new HashMap<>();
    this.unknownToMaxVar = new HashMap<>();

    // Build the formula and encode meta-class formulae
    this.encodeSingleCharClasses();
    this.encodeRepetitionsForEachExample();

    for (UnknownId id : this.unknowns) {
      this.encodeCharClass(id, this.tree);
      this.encodeCharClassSummation(id);
    }
  }

  private void encodeSingleCharClasses () {
    for (Set<Route> routes : this.positives) {
      BoolExpr expr = this.encodeRoutes(routes, true);

      if (expr != null) {
        this.opt.Add(expr);
      }
    }

    for (Set<Route> routes : this.negatives) {
      BoolExpr expr = this.encodeRoutes(routes, false);

      if (expr != null) {
        this.opt.Add(expr);
      }
    }
  }

  private BoolExpr encodeRoutes (Set<Route> routes, boolean posFlag) {
    BoolExpr exprFinal = null;

    for (Route route : routes) {
      BoolExpr exprRoute = this.encodeRoute(route, posFlag);

      if (exprRoute == null) {
        continue;
      }

      if (exprFinal == null) {
        exprFinal = exprRoute;
      } else if (posFlag) {
        exprFinal = this.ctx.mkOr(exprFinal, exprRoute);
      } else {
        exprFinal = this.ctx.mkAnd(exprFinal, exprRoute);
      }
    }

    return exprFinal;
  }

  private BoolExpr encodeRoute (Route route, boolean posFlag) {
    Map<UnknownId, Set<Character>> spans = route.getSpans();
    BoolExpr exprRoute = null;

    for (UnknownId id : spans.keySet()) {
      this.unknowns.add(id);

      BoolExpr exprChars = this.encodeUnknownInRoute(id, spans.get(id), posFlag);

      if (exprRoute == null) {
        exprRoute = exprChars;
      } else if (posFlag) {
        exprRoute = this.ctx.mkAnd(exprRoute, exprChars);
      } else {
        exprRoute = this.ctx.mkOr(exprRoute, exprChars);
      }
    }

    return exprRoute;
  }

  private BoolExpr encodeUnknownInRoute (UnknownId id, Set<Character> chars, boolean posFlag) {
    Set<BoolExpr> vars = new HashSet<>();
    for (Character ch : chars) {
      BoolExpr var = this.encodeChar(id, ch, posFlag);
      vars.add(var);
    }

    return vars
      .stream()
      .filter(v -> v != null)
      .map(v -> posFlag ? v : this.ctx.mkNot(v))
      .reduce(null, (accum, v) -> {
        if (accum == null) {
          return v;
        } else if (posFlag) {
          return this.ctx.mkAnd(accum, v);
        } else {
          return this.ctx.mkOr(accum, v);
        }
      });
  }

  private BoolExpr encodeChar (UnknownId id, char ch, boolean posFlag) {
    MetaClassTree tree = this.tree.incrementTally(id, ch);

    if (tree == null) {
      // Create a new tree and categorize this new tree as "miscellaneous".
      tree = new MetaClassTree(new CharLiteralNode(ch), new SimplePredicate(ch), 1);
      this.misc.add(tree);
      tree.incrementTally(id, ch);
    }

    if (this.unknownToCharToVar.containsKey(id)) {
      BoolExpr var = this.unknownToCharToVar.get(id).get(ch);
      if (var != null) {
        return var;
      }
    } else {
      this.unknownToCharToVar.put(id, new HashMap<>());
    }


    BoolExpr var = this.encodeWeightedConstraint(id, tree);
    this.unknownToCharToVar.get(id).put(ch, var);
    return var;
  }

  private void getAllRelevantUnknownExits (List<Set<Route>> data) {
    for (Set<Route> s : data) {
      for (Route r : s) {
        for (UnknownId id : r.getExits().keySet()) {
          this.UnknownBoundss.add(id);
        }
      }
    }
  }

  private void encodeRepetitionsForEachExample () {
    // Build a list of all unknown IDs encountered by these automata routes.
    this.getAllRelevantUnknownExits(this.positives);
    this.getAllRelevantUnknownExits(this.negatives);

    // Create all 'H?_max' and 'H?_min' variables for all relevant IDs.
    for (UnknownId id : this.UnknownBoundss) {
      this.unknownToMinVar.put(id, this.ctx.mkIntConst(id.toString() + "_min"));
      this.unknownToMaxVar.put(id, this.ctx.mkIntConst(id.toString() + "_max"));
    }

    // TOOD: reject negative repetitions 'OR' negative character encodings

    for (Set<Route> example : this.positives) {
      List<BoolExpr> exprs = encodeUnknownBounds(example, true);
      this.opt.Assert(this.ctx.mkOr(exprs.toArray(new BoolExpr[exprs.size()])));
    }

    for (Set<Route> example : this.negatives) {
      List<BoolExpr> exprs = encodeUnknownBounds(example, false);
      this.opt.Assert(this.ctx.mkAnd(exprs.toArray(new BoolExpr[exprs.size()])));
    }
  }

  private List<BoolExpr> encodeUnknownBounds (Set<Route> example, boolean matchThisExample) {
    List<BoolExpr> exprs = new LinkedList<>();
    Map<UnknownId, Integer> max = new HashMap<>();
    Map<UnknownId, Integer> min = new HashMap<>();

    for (UnknownId id : this.UnknownBoundss) {
      max.put(id, -1);
      min.put(id, -1);
    }

    /**
     * For the set of routes generated by this example, determine the minimum
     * and maximum times the automata must travel through an UnknownBounds to
     * be accepted. Note that these minimums and maximums are computed
     * per/unknown and are stored in the 'min' and 'max' maps.
     */
    for (Route route : example) {
      Set<UnknownId> idsRelevantToRoute = route.getExits().keySet();
      for (UnknownId id : idsRelevantToRoute) {
        // Count # of times this route passed through this unknown (default 0).
        int count = route.countExits(id);

        // Update minimum for this unknown if the count is significant.
        if (count < min.get(id) || min.get(id) < 0) {
          min.put(id, count);
        }

        // Update maximum for this unknown if the count is significant.
        if (count > max.get(id) || max.get(id) < 0) {
          max.put(id, count);
        }
      }
    }

    // For each route, build a SAT expression that relates the 'min' and
    // 'max' variables associated with a given ID to the route's minimum
    // and maximum repeat values.
    for (UnknownId id : this.UnknownBoundss) {
      // Compute minimum and maximum values to constrain this ID.
      ArithExpr maxVal = this.ctx.mkInt(max.get(id) > -1 ? max.get(id) : 0);
      ArithExpr minVal = this.ctx.mkInt(min.get(id) > -1 ? min.get(id) : 0);

      // Lookup 'min' and 'max' variables associated with this ID.
      IntExpr minVar = this.unknownToMinVar.get(id);
      IntExpr maxVar = this.unknownToMaxVar.get(id);

      // If this example should be matched, require that the unknown int is
      // between the 'min' and 'max' values. If this example should NOT be
      // matched, require that the unknown int has a 'max' less than 'minVal'
      // or that it has a 'min' greater than 'maxVal'.
      if (matchThisExample) {
        exprs.add(this.ctx.mkAnd(
          this.ctx.mkLe(minVar, minVal),
          this.ctx.mkGe(maxVar, maxVal)));
      } else {
        exprs.add(this.ctx.mkOr(
          this.ctx.mkGt(minVar, minVal),
          this.ctx.mkLt(maxVar, maxVal)));
      }
    }

    return exprs;
  }

  private Set<BoolExpr> encodeCharClass (UnknownId id, MetaClassTree tree) {
    // TODO
    // - call encodeCharClass(branch) which returns the SAT variables created
    //   from the tree's child classes
    // - bind each child class variable to the tree variable in the

    Set<BoolExpr> vars = new HashSet<>();
    for (MetaClassTree branch : tree.getBranches()) {
      vars.addAll(encodeCharClass(id, branch));
    }

    if (tree.isCandidateBranch(id)) {
      Set<BoolExpr> s = new HashSet<>();

      if (vars.size() == 0) {
        s.add(this.unknownToTreeToVar.get(id).get(tree));
      } else {
        s.add(encodeMetaCharClass(id, vars, tree));
      }

      return s;
    }

    return vars;
  }

  private void encodeCharClassSummation (UnknownId id) {
    List<IntExpr> weightsList = this.unknownToWeights.get(id);
    IntExpr[] weightsArr = new IntExpr[weightsList.size()];

    for (int i = 0; i < weightsList.size(); i++) {
      weightsArr[i] = weightsList.get(i);
    }

    this.opt.MkMaximize(this.ctx.mkAdd(weightsArr));
  }

  private String createVariableName (UnknownId id) {
    return String.format("%s_%d", id, this.nextVarId++);
  }

  private void saveWeightForSummation (UnknownId id, IntExpr weight) {
    if (this.unknownToWeights.containsKey(id) == false) {
      this.unknownToWeights.put(id, new LinkedList<>());
    }

    this.unknownToWeights.get(id).add(weight);
  }

  // Register variable with unknown -> variable mapping.
  private void saveVar (UnknownId id, MetaClassTree tree, BoolExpr var) {
    if (this.unknownToVars.containsKey(id) == false) {
      this.unknownToVars.put(id, new HashSet<>());
    }
    this.unknownToVars.get(id).add(var);

    if (this.unknownToTreeToVar.containsKey(id) == false) {
      this.unknownToTreeToVar.put(id, new HashMap<>());
    }
    this.unknownToTreeToVar.get(id).put(tree, var);
  }

  private BoolExpr createVariable (UnknownId id) {
    String name = String.format("%s_%d", id, this.nextVarId++);
    BoolExpr var = this.ctx.mkBoolConst(name);

    // Register variable with unknown -> variable mapping.
    if (this.unknownToVars.containsKey(id) == false) {
      this.unknownToVars.put(id, new HashSet<>());
    }
    this.unknownToVars.get(id).add(var);

    return var;
  }

  private BoolExpr encodeWeightedConstraint (UnknownId id, MetaClassTree tree) {
    String name = this.createVariableName(id);

    // (declare-fun H1_x_v () Bool)
    BoolExpr var = this.ctx.mkBoolConst(name + "_v");

    // (declare-const H1_x_w Int)
    IntExpr weight = this.ctx.mkIntConst(name + "_w");

    // (assert (ite H1_x_v (= H1_x_w 5) (= H1_x_w 0)))
    int scalar = tree.getSATWeight(id);
    BoolExpr ifTrue = this.ctx.mkEq(weight, this.ctx.mkInt(scalar));
    BoolExpr ifFalse = this.ctx.mkEq(weight, this.ctx.mkInt(0));
    this.opt.Assert((BoolExpr)this.ctx.mkITE(var, ifTrue, ifFalse));

    this.saveVar(id, tree, var);
    this.saveWeightForSummation(id, weight);

    // Register variable with variable -> tree mapping.
    this.varToTree.put(var, tree);

    return var;
  }

  private BoolExpr encodeMetaCharClass (UnknownId id, Set<BoolExpr> vars, MetaClassTree tree) {
    BoolExpr var = this.encodeWeightedConstraint(id, tree);
    BoolExpr relations = vars
      .stream()
      .map(v -> this.ctx.mkOr(this.ctx.mkNot(var), v))
      .reduce(null, (accum, v) -> (accum == null) ? v : this.ctx.mkAnd(accum, v));

    if (relations != null) {
      this.opt.Add(relations);
    }

    return var;
  }

  public void solve () throws SynthesisFailure {
    /**
     * First, check that the formula was satisifed
     */
    if (this.opt.Check() == Status.UNSATISFIABLE) {
      throw new SynthesisFailure("unsatisfiable SAT formula");
    } else {
      if (this.diag.getBool("print-var-map")) {
        String header = String.format("\nVAR MAP for %d", this.diag.output().count());
        this.diag.output().printBlock(header);
        for (BoolExpr var : this.varToTree.keySet()) {
          CharClass cc = this.varToTree.get(var).getCharClass();
          String line = String.format("%-10s : '%s'\n", var, cc);
          this.diag.output().printBlock(line);
        }
        this.diag.output().printBreak();
      }

      if (this.diag.getBool("print-class-map")) {
        String header = String.format("\nCLASS MAP for %d", this.diag.output().count());
        this.diag.output().printBlock(header);
        this.diag.output().printBlock(this.tree);
        for (MetaClassTree tree : this.misc) {
          this.diag.output().printBlock(tree);
        }
        this.diag.output().printBreak();
      }

      if (this.diag.getBool("print-formula")) {
        String header = String.format("\nFORMULA for %d", this.diag.output().count());
        this.diag.output().printBlock(header);
        this.diag.output().printBlock(this.opt.toString());
        this.diag.output().printBreak();
      }

      // Use the SAT solver to attempt to resolve the variables and their constraints.
      this.model = this.opt.getModel();

      if (this.diag.getBool("print-model")) {
        String header = String.format("\nMODEL for %d", this.diag.output().count());
        this.diag.output().printBlock(header);
        this.diag.output().printBlock(this.model.toString());
        this.diag.output().printBreak();
      }
    }
  }

  public Map<UnknownId, CharClass> getCharSolutions () throws SynthesisFailure {
    if (this.model == null) {
      throw new IllegalStateException("solutions requested before model was solved");
    }

    Map<UnknownId, CharClass> solutions = new HashMap<>();
    for (UnknownId id : this.unknowns) {
      solutions.put(id, getCharSolution(id));
    }
    return solutions;
  }

  private CharClass getCharSolution (UnknownId id) throws SynthesisFailure {
    /**
     * - for each variable determined to be TRUE:
     *   - get that variable's corresponding tree
     *   - find the oldest ancestor of that tree determined to be true
     *   - add that ancestor to the set of solutions
     */
    Map<MetaClassTree, Boolean> treeIsTrue = new HashMap<>();

    // Determine which character classes were evaluated to true for this unknown.
    for (BoolExpr var : this.unknownToVars.get(id)) {
      MetaClassTree tree = this.varToTree.get(var);
      if (this.model.evaluate(var, false).isTrue()) {
        treeIsTrue.put(tree, true);
      }
    }

    // For each true variable associated with the given unknown, find its
    // corresponding tree and find the oldest true ancestor of that tree.
    Set<CharClass> solutions = new HashSet<>();
    for (BoolExpr var : this.unknownToVars.get(id)) {
      MetaClassTree ancestor = this.varToTree.get(var).getFurthestTrueAncestor(treeIsTrue);
      if (ancestor != null) {
        solutions.add(ancestor.getCharClass());
      }
    }

    if (solutions.size() == 0) {
      throw new SynthesisFailure("SAT produced no solutions for " + id.toString());
    } if (solutions.size() == 1) {
      CharClass only = solutions.iterator().next();
      if (only instanceof CharRangeNode) {
        return new CharClassSetNode((CharRangeNode)only);
      }

      return only;
    } else {
      Collection<CharRangeNode> subClasses = new HashSet<>();

      for (CharClass subClass : solutions) {
        if (subClass instanceof CharRangeNode) {
          subClasses.add((CharRangeNode)subClass);
        } else if (subClass instanceof ConcreteCharClass) {
          subClasses.add(new CharRangeNode((ConcreteCharClass)subClass));
        } else {
          // TODO
          System.out.println("Could not handle character class");
          System.out.println(subClass);
          System.exit(1);
        }
      }

      return new CharClassSetNode(subClasses);
    }
  }

  public Map<UnknownId, Bounds> getBoundsSolutions () throws SynthesisFailure {
    if (this.model == null) {
      throw new IllegalStateException("solutions requested before model was solved");
    }

    Map<UnknownId, Bounds> solutions = new HashMap<>();
    for (UnknownId id : this.UnknownBoundss) {
      solutions.put(id, getBoundsSolution(id));
    }
    return solutions;
  }

  private Bounds getBoundsSolution (UnknownId id) throws SynthesisFailure {
    IntExpr minVar = this.unknownToMinVar.get(id);
    IntExpr maxVar = this.unknownToMaxVar.get(id);

    int min = ((IntNum)this.model.evaluate(minVar, false)).getInt();
    int max = ((IntNum)this.model.evaluate(maxVar, false)).getInt();

    return Bounds.between(min, max);
  }

  @Override
  public String toString () {
    return this.opt.toString();
  }
}
