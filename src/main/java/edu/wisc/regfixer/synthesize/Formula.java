package edu.wisc.regfixer.synthesize;

import java.util.Arrays;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import com.microsoft.z3.BoolExpr;
import com.microsoft.z3.IntExpr;
import com.microsoft.z3.Context;
import com.microsoft.z3.Model;
import com.microsoft.z3.Optimize;
import com.microsoft.z3.Status;
import edu.wisc.regfixer.automata.Route;
import edu.wisc.regfixer.enumerate.HoleId;
import edu.wisc.regfixer.parser.CharClass;
import edu.wisc.regfixer.parser.CharClassSetNode;
import edu.wisc.regfixer.parser.CharEscapedNode;
import edu.wisc.regfixer.parser.CharLiteralNode;
import edu.wisc.regfixer.parser.CharRangeNode;
import edu.wisc.regfixer.parser.ConcreteCharClass;
import edu.wisc.regfixer.util.PrintableTree;
import edu.wisc.regfixer.Config;


public class Formula {
  private static Predicate pred_d = new SimplePredicate('0', '9');
  private static Predicate pred_D = new CompoundPredicate(false,
    new SimplePredicate('0', '9'));
  private static Predicate pred_w = new CompoundPredicate(
    new SimplePredicate('_'),
    new SimplePredicate('A', 'Z'),
    new SimplePredicate('a', 'z'),
    new SimplePredicate('0', '9'));
  private static Predicate pred_W = new CompoundPredicate(false,
    new SimplePredicate('_'),
    new SimplePredicate('A', 'Z'),
    new SimplePredicate('a', 'z'),
    new SimplePredicate('0', '9'));
  private static Predicate pred_az = new SimplePredicate('a', 'z');
  private static Predicate pred_AZ = new SimplePredicate('A', 'Z');
  private static Predicate pred_azAZ = new CompoundPredicate(
    new SimplePredicate('a', 'z'),
    new SimplePredicate('A', 'Z'));

  private static CharClass class_d = new CharEscapedNode('d');
  private static CharClass class_D = new CharEscapedNode('D');
  private static CharClass class_w = new CharEscapedNode('w');
  private static CharClass class_W = new CharEscapedNode('W');
  private static CharRangeNode class_az = new CharRangeNode('a', 'z');
  private static CharRangeNode class_AZ = new CharRangeNode('A', 'Z');
  private static CharClass class_azAZ = new CharClassSetNode(class_az, class_AZ);

  /**
   * The SAT weight of a particular character class is determined based on the
   * weight of the character class layer (where classes in the bottom layer have
   * a weight of 2 and each layer up increments the previous layer's weight)
   * minus the sum of all child classes (if no child classes exist, use a sum of
   * 0 is used).
   *
   * A larger character class should only be chosen if choosing the larger class
   * would prevent the use of 2 or more separate character classes. An example:
   *
   * (Tally represents how many characters matched a particular class)
   *
   *   Class   Tally   Layer Weight
   * --------------------------------
   *   \w      6       4
   *   \d      0       3
   *   [a-z]   4       3
   *   [A-Z]   2       3
   *   F       1       2
   *   o       2       2
   *   B       1       2
   *   a       1       2
   *   r       1       2
   *
   *
   * In this case, the following classes would all match these inputs:
   *
   *   Class
   * ------------
   *   \w
   *   [a-zA-Z]
   *   [FoBar]
   *
   *
   * But which resultant class is preferred? The algorithm will prefer the \w
   * class because it can be used instead of the 2 seperate character classes
   * present in the second option and the 5 seperate classes present in the
   * third option.
   *
   * However, in the following example the \d class will be preferred over the
   * \w class because the using the \w class does not simplify the resultant
   * class as in the first example.
   *
   *   Class   Tally   Layer Weight
   * --------------------------------
   *   \w      3       4
   *   \d      3       3
   *   1       1       2
   *   2       1       2
   *   3       1       2
   */
  private static class MetaClassTree extends PrintableTree {
    private CharClass cc;
    private Predicate pred;
    private int layer;
    private MetaClassTree parent;
    private List<MetaClassTree> children;
    private Map<HoleId, Integer> tally;

    public MetaClassTree (CharClass cc, Predicate pred, int layer, MetaClassTree... children) {
      this(cc, pred, layer, Arrays.asList(children));
    }

    public MetaClassTree (CharClass cc, Predicate pred, int layer, List<MetaClassTree> children) {
      this.cc       = cc;
      this.pred     = pred;
      this.layer    = layer;
      this.parent   = null;
      this.children = children;
      this.tally    = new HashMap<>();

      for (MetaClassTree child : this.children) {
        child.parent = this;
      }
    }

    public static int LEAF_WEIGHT = 3;

    public static int layerWeight (int layer) {
      int weight = 0;

      for (int i = 0; i < layer; i++) {
        if (i == 0) {
          weight = MetaClassTree.LEAF_WEIGHT;
        } else {
          weight = (2 * weight) - 1;
        }
      }

      return weight;
    }

    public CharClass getCharClass () {
      return this.cc;
    }

    public Predicate getPred () {
      return this.pred;
    }

    public List<PrintableTree> getChildren () {
      List<PrintableTree> cast = new LinkedList<>();
      for (MetaClassTree child : this.children) {
        if (child.tally.size() > 0) {
          cast.add(child);
        }
      }
      return cast;
    }

    public MetaClassTree getFurthestTrueAncestor (Map<MetaClassTree, Boolean> evaluation) {
      MetaClassTree ancestor = null;
      if (this.parent != null) {
        ancestor = this.parent.getFurthestTrueAncestor(evaluation);
      }

      Boolean isTrue = evaluation.get(this);

      if (ancestor != null) {
        return ancestor;
      } else if (isTrue != null && isTrue) {
        return this;
      } else {
        return null;
      }
    }

    public List<MetaClassTree> getBranches () {
      return this.children;
    }

    public int getSATWeight (HoleId id) {
      int totalChildren = 0;
      for (MetaClassTree child : this.children) {
        if (child.tally.containsKey(id)) {
          totalChildren++;
        }
      }

      int childWeight = MetaClassTree.layerWeight(this.layer - 1);
      int treeWeight = MetaClassTree.layerWeight(this.layer);
      return (totalChildren * childWeight) - treeWeight;
    }

    private boolean isSatisfied (char ch) {
      return this.pred.includes(ch);
    }

    public int getTally (HoleId id) {
      return (this.tally.containsKey(id)) ? this.tally.get(id) : 0;
    }

    public MetaClassTree incrementTally (HoleId id, char ch) {
      if (this.isSatisfied(ch) == false) {
        return null;
      }

      this.tally.put(id, this.getTally(id) + 1);

      for (MetaClassTree child : this.children) {
        MetaClassTree match = child.incrementTally(id, ch);
        if (match != null) {
          return match;
        }
      }

      return this;
    }

    public boolean isCandidateBranch (HoleId id) {
      /**
       * Literal-classes should only be considered for inclusion in the SAT
       * formula iff:
       * - Class has more than 0 tallies
       */
      if (this.getTally(id) > 0) {
        return true;
      }

      return false;
    }

    public String toString () {
      String out = String.format("'%s'", this.cc.toString());

      for (Map.Entry<HoleId, Integer> entry : this.tally.entrySet()) {
        out += String.format("\n%s %d (%s)", entry.getKey(), entry.getValue(), this.getSATWeight(entry.getKey()));
      }

      return out;
    }

    public static MetaClassTree[] createMetaClassRange (char low, char high, int level) {
      int total = high - low + 1;
      MetaClassTree[] out = new MetaClassTree[total];

      int i = 0;
      for (char c = low; c <= high; c++) {
        CharLiteralNode cc = new CharLiteralNode(c);
        SimplePredicate pred = new SimplePredicate(c);
        out[i++] = new MetaClassTree(cc, pred, level);
      }

      return out;
    }

    public static MetaClassTree initialize () {
      MetaClassTree[] children_d  = createMetaClassRange('0', '9', 1);
      MetaClassTree[] children_az = createMetaClassRange('a', 'z', 1);
      MetaClassTree[] children_AZ = createMetaClassRange('A', 'Z', 1);

      MetaClassTree cls_d  = new MetaClassTree(class_d,  pred_d,  2, children_d);
      MetaClassTree cls_az = new MetaClassTree(class_az, pred_az, 2, children_az);
      MetaClassTree cls_AZ = new MetaClassTree(class_AZ, pred_AZ, 2, children_AZ);

      MetaClassTree cls_w  = new MetaClassTree(class_w,  pred_w,  3, cls_d, cls_az, cls_AZ);

      return cls_w;
    }
  }

  private List<Set<Route>> positives;
  private List<Set<Route>> negatives;
  private Config config;

  private Context ctx;
  private Optimize opt;
  private Model model;

  private int nextVarId;
  private Set<HoleId> holes;
  private Map<HoleId, Set<BoolExpr>> holeToVars;
  private Map<HoleId, List<IntExpr>> holeToWeights;
  private Map<HoleId, Map<MetaClassTree, BoolExpr>> holeToTreeToVar;
  private Map<BoolExpr, MetaClassTree> varToTree;
  private Map<BoolExpr, Predicate> varToPred;
  private MetaClassTree tree;
  private Set<MetaClassTree> misc;
  private Map<HoleId, Map<Character, BoolExpr>> holeToCharToVar;

  public Formula (List<Set<Route>> positives, List<Set<Route>> negatives) {
    this(positives, negatives, new Config());
  }

  public Formula (List<Set<Route>> positives, List<Set<Route>> negatives, Config config) {
    this.positives = positives;
    this.negatives = negatives;
    this.config = config;

    // Initialize SAT formula objects
    this.ctx = new Context();
    this.opt = this.ctx.mkOptimize();
    this.model = null;

    // Initialize structures for tracking state
    this.nextVarId = 0;
    this.holes = new HashSet<>();
    this.holeToVars = new HashMap<>();
    this.holeToWeights = new HashMap<>();
    this.holeToTreeToVar = new HashMap<>();
    this.varToTree = new HashMap<>();
    this.varToPred = new HashMap<>();
    this.tree = MetaClassTree.initialize();
    this.holeToCharToVar = new HashMap<>();
    this.misc = new HashSet<>();

    // Build the formula and encode meta-class formulae
    this.encodeSingleCharClasses();

    for (HoleId id : this.holes) {
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
    Map<HoleId, Set<Character>> spans = route.getSpans();
    BoolExpr exprRoute = null;

    for (HoleId holeId : spans.keySet()) {
      this.holes.add(holeId);

      BoolExpr exprChars = this.encodeHoleInRoute(holeId, spans.get(holeId), posFlag);

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

  private BoolExpr encodeHoleInRoute (HoleId id, Set<Character> chars, boolean posFlag) {
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

  private BoolExpr encodeChar (HoleId id, char ch, boolean posFlag) {
    MetaClassTree tree = this.tree.incrementTally(id, ch);

    if (tree == null) {
      // Create a new tree and categorize this new tree as "miscellaneous".
      tree = new MetaClassTree(new CharLiteralNode(ch), new SimplePredicate(ch), 1);
      this.misc.add(tree);
      tree.incrementTally(id, ch);
    }

    if (this.holeToCharToVar.containsKey(id)) {
      BoolExpr var = this.holeToCharToVar.get(id).get(ch);
      if (var != null) {
        return var;
      }
    } else {
      this.holeToCharToVar.put(id, new HashMap<>());
    }


    BoolExpr var = this.encodeWeightedConstraint(id, tree);
    this.holeToCharToVar.get(id).put(ch, var);
    return var;
  }

  private Set<BoolExpr> encodeCharClass (HoleId id, MetaClassTree tree) {
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
        s.add(this.holeToTreeToVar.get(id).get(tree));
      } else {
        s.add(encodeMetaCharClass(id, vars, tree));
      }

      return s;
    }

    return vars;
  }

  private void encodeCharClassSummation (HoleId id) {
    List<IntExpr> weightsList = this.holeToWeights.get(id);
    IntExpr[] weightsArr = new IntExpr[weightsList.size()];

    for (int i = 0; i < weightsList.size(); i++) {
      weightsArr[i] = weightsList.get(i);
    }

    this.opt.MkMaximize(this.ctx.mkAdd(weightsArr));
  }

  private String createVariableName (HoleId id) {
    return String.format("%s_%d", id, this.nextVarId++);
  }

  private void saveWeightForSummation (HoleId id, IntExpr weight) {
    if (this.holeToWeights.containsKey(id) == false) {
      this.holeToWeights.put(id, new LinkedList<>());
    }

    this.holeToWeights.get(id).add(weight);
  }

  // Register variable with hole -> variable mapping.
  private void saveVar (HoleId id, MetaClassTree tree, BoolExpr var) {
    if (this.holeToVars.containsKey(id) == false) {
      this.holeToVars.put(id, new HashSet<>());
    }
    this.holeToVars.get(id).add(var);

    if (this.holeToTreeToVar.containsKey(id) == false) {
      this.holeToTreeToVar.put(id, new HashMap<>());
    }
    this.holeToTreeToVar.get(id).put(tree, var);
  }

  private BoolExpr createVariable (HoleId id) {
    String name = String.format("%s_%d", id, this.nextVarId++);
    BoolExpr var = this.ctx.mkBoolConst(name);

    // Register variable with hole -> variable mapping.
    if (this.holeToVars.containsKey(id) == false) {
      this.holeToVars.put(id, new HashSet<>());
    }
    this.holeToVars.get(id).add(var);

    return var;
  }

  private BoolExpr encodeWeightedConstraint (HoleId id, MetaClassTree tree) {
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

  private BoolExpr encodeMetaCharClass (HoleId id, Set<BoolExpr> vars, MetaClassTree tree) {
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

  public Map<HoleId, CharClass> solve () throws SynthesisFailure {
    /**
     * First, check that the formula was satisifed
     */
    if (this.opt.Check() == Status.UNSATISFIABLE) {
      throw new SynthesisFailure("unsatisfiable SAT formula");
    } else {
      if (this.config.getBool("print-var-map")) {
        System.out.println();
        for (BoolExpr var : this.varToTree.keySet()) {
          System.out.printf("%s\t : '%s'\n", var, this.varToTree.get(var).getCharClass());
        }
      }

      if (this.config.getBool("print-class-tree")) {
        System.out.println();
        System.out.println(PrintableTree.toString(this.tree));
        for (MetaClassTree tree : this.misc) {
          System.out.println(PrintableTree.toString(tree));
        }
      }

      if (this.config.getBool("print-formula")) {
        System.out.println();
        System.out.println(this.opt.toString());
      }

      this.model = this.opt.getModel();

      if (this.config.getBool("print-model")) {
        System.out.println(this.model);
      }
    }

    /**
     * - for each variable determined to be TRUE:
     *   - get that variable's corresponding tree
     *   - find the oldest ancestor of that tree determined to be true
     *   - add that ancestor to the set of solutions
     */

    Map<HoleId, CharClass> solutions = new HashMap<>();
    for (HoleId id : this.holes) {
      solutions.put(id, solveHole(id));
    }

    return solutions;
  }

  public CharClass solveHole (HoleId id) throws SynthesisFailure {
    Map<MetaClassTree, Boolean> treeIsTrue = new HashMap<>();

    // Determine which character classes were evaluated to true for this hole.
    for (BoolExpr var : this.holeToVars.get(id)) {
      MetaClassTree tree = this.varToTree.get(var);
      if (this.model.evaluate(var, false).isTrue()) {
        treeIsTrue.put(tree, true);
      }
    }

    // For each true variable associated with the given hole, find its
    // corresponding tree and find the oldest true ancestor of that tree.
    Set<CharClass> solutions = new HashSet<>();
    for (BoolExpr var : this.holeToVars.get(id)) {
      MetaClassTree ancestor = this.varToTree.get(var).getFurthestTrueAncestor(treeIsTrue);
      if (ancestor != null) {
        solutions.add(ancestor.getCharClass());
      }
    }

    if (solutions.size() == 0) {
      throw new SynthesisFailure("SAT produced no solutions for " + id.toString());
    } if (solutions.size() == 1) {
      return solutions.iterator().next();
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

  @Override
  public String toString () {
    return this.opt.toString();
  }
}
