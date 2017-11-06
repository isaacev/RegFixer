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
  private Set<UnknownId> unknownChars;
  private Map<UnknownId, Set<BoolExpr>> unknownToVars;
  private Map<UnknownId, List<IntExpr>> unknownToWeights;
  private Map<UnknownId, Map<MetaClassTree, BoolExpr>> unknownToTreeToVar;
  private Map<BoolExpr, MetaClassTree> varToTree;
  private Map<BoolExpr, Predicate> varToPred;
  private MetaClassTree tree;
  private Set<MetaClassTree> misc;
  private Map<UnknownId, Map<Character, BoolExpr>> unknownToCharToVar;

  private Set<UnknownId> unknownBounds;
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
    this.unknownChars = new HashSet<>();
    this.unknownToVars = new HashMap<>();
    this.unknownToWeights = new HashMap<>();
    this.unknownToTreeToVar = new HashMap<>();
    this.varToTree = new HashMap<>();
    this.varToPred = new HashMap<>();
    this.tree = MetaClassTree.initialize();
    this.unknownToCharToVar = new HashMap<>();
    this.misc = new HashSet<>();

    this.unknownBounds = new HashSet<>();
    this.unknownToMinVar = new HashMap<>();
    this.unknownToMaxVar = new HashMap<>();

    // Build a list of all unknown IDs encountered by these automata routes.
    this.getAllRelevantUnknownExits(this.positives);
    this.getAllRelevantUnknownExits(this.negatives);

    // Create all 'H?_max' and 'H?_min' variables for all relevant IDs.
    for (UnknownId id : this.unknownBounds) {
      // Create minimum and maximum bound variables.
      IntExpr minVar = this.ctx.mkIntConst(id.toString() + "_min");
      IntExpr maxVar = this.ctx.mkIntConst(id.toString() + "_max");

      // Associate minimum and maximum bound variables with appropriate ID.
      this.unknownToMinVar.put(id, minVar);
      this.unknownToMaxVar.put(id, maxVar);

      // Force every minimum bound to be <= corresponding maximum bound.
      this.opt.Assert(this.ctx.mkLe(minVar, maxVar));
    }

    // Build the formula and encode meta-class formulae
    this.encodeRoutes();

    for (UnknownId id : this.unknownChars) {
      this.encodeCharClass(id, this.tree);
      this.encodeCharClassSummation(id);
    }
  }

  private void encodeRoutes () {
    for (Set<Route> example : this.positives) {
      this.encodePositiveExample(example);
    }

    for (Set<Route> example : this.negatives) {
      this.encodeNegativeExample(example);
    }
  }

  private void encodePositiveExample (Set<Route> example) {
    BoolExpr whole = null;

    for (Route route : example) {
      BoolExpr charFormula = this.buildPositiveRouteFormula(route);
      BoolExpr quantFormula = this.buildPositiveQuantifierFormula(route);

      // If character and quantifier constraints both exist, AND them together.
      BoolExpr part = null;
      if (charFormula != null && quantFormula != null) {
        part = this.ctx.mkAnd(charFormula, quantFormula);
      } else if (charFormula != null) {
        part = charFormula;
      } else {
        part = quantFormula;
      }

      // If this route didn't produce any constraints on either the accepted
      // characters or the bounds for repetition, skip this route.
      if (part == null) {
        continue;
      }

      // Since the entire example is accepted if at least 1 route is accepted,
      // OR this route's formula with the formulae of all the other routes for
      // this positive example.
      if (whole == null) {
        whole = part;
      } else {
        whole = this.ctx.mkOr(whole, part);
      }
    }

    if (whole != null) {
      this.opt.Add(whole);
    }
  }

  public BoolExpr buildPositiveRouteFormula (Route route) {
    return encodeRoute(route, true);
  }

  public BoolExpr buildPositiveQuantifierFormula (Route route) {
    BoolExpr whole = null;

    for (Map.Entry<UnknownId, Integer> entry : route.getExits().entrySet()) {
      IntNum countVal = this.ctx.mkInt(entry.getValue());
      IntExpr minVar = unknownToMinVar.get(entry.getKey());
      IntExpr maxVar = unknownToMaxVar.get(entry.getKey());
      BoolExpr part = this.ctx.mkAnd(
        this.ctx.mkLe(minVar, countVal),
        this.ctx.mkGe(maxVar, countVal));

      if (whole == null) {
        whole = part;
      } else {
        whole = this.ctx.mkOr(whole, part);
      }
    }

    return whole;
  }

  private void encodeNegativeExample (Set<Route> example) {
    BoolExpr whole = null;

    for (Route route : example) {
      BoolExpr charFormula = this.buildNegativeRouteFormula(route);
      BoolExpr quantFormula = this.buildNegativeQuantifierFormula(route);

      // If character and quantifier both exist, OR them together.
      BoolExpr part = null;
      if (charFormula != null && quantFormula != null) {
        part = this.ctx.mkOr(charFormula, quantFormula);
      } else if (charFormula != null) {
        part = charFormula;
      } else {
        part = quantFormula;
      }

      // If this route didn't produce any constraints on either the accepted
      // characters or the bounds for repetition, skip this route.
      if (part == null) {
        continue;
      }

      // Since the entire example is rejected if no routes are accepted, AND
      // this route's formula with the formula of all other routes for this
      // negative example.
      if (whole == null) {
        whole = part;
      } else {
        whole = this.ctx.mkAnd(whole, part);
      }
    }

    if (whole != null) {
      this.opt.Add(whole);
    }
  }

  public BoolExpr buildNegativeRouteFormula (Route route) {
    return encodeRoute(route, false);
  }

  public BoolExpr buildNegativeQuantifierFormula (Route route) {
    BoolExpr whole = null;

    for (Map.Entry<UnknownId, Integer> entry : route.getExits().entrySet()) {
      IntNum countVal = this.ctx.mkInt(entry.getValue());
      IntExpr minVar = unknownToMinVar.get(entry.getKey());
      IntExpr maxVar = unknownToMaxVar.get(entry.getKey());
      BoolExpr part = this.ctx.mkOr(
        this.ctx.mkGt(minVar, countVal),
        this.ctx.mkLt(maxVar, countVal));

      if (whole == null) {
        whole = part;
      } else {
        whole = this.ctx.mkAnd(whole, part);
      }
    }

    return whole;
  }

  private BoolExpr encodeRoute (Route route, boolean posFlag) {
    Map<UnknownId, Set<Character>> spans = route.getSpans();
    BoolExpr exprRoute = null;

    for (UnknownId id : spans.keySet()) {
      this.unknownChars.add(id);

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
          this.unknownBounds.add(id);
        }
      }
    }
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

    /**
     * First, check that the formula was satisifed
     */
    if (this.opt.Check() == Status.UNSATISFIABLE) {
      throw new SynthesisFailure("unsatisfiable SAT formula");
    } else {
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
    for (UnknownId id : this.unknownChars) {
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
    for (UnknownId id : this.unknownBounds) {
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
