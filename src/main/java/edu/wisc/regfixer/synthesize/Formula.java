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
      Boolean eval = evaluation.get(this);
      if (eval == null || eval != true) {
        return null;
      }

      if (this.parent == null) {
        return this;
      }

      MetaClassTree ancestor = this.parent.getFurthestTrueAncestor(evaluation);
      return (ancestor == null) ? this : ancestor;
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
       *
       * Meta-classes should only be considered for inclusion in the SAT
       * formula if the class meets all of the following criteria:
       * - Class has more than 0 tallies
       * - If class has child classes, at least 2 have tallies
       */
      if (this.getTally(id) > 0) {
        if (this.children.size() == 0) {
          return true;
        }

        // countTalliedChildren is the number of child classes that match at
        // least 1 character passing through the specified hole.
        int countTalliedChildren = this.children.stream()
          .filter(c -> c.getTally(id) > 0)
          .collect(Collectors.toSet())
          .size();

        if (countTalliedChildren >= 2) {
          return true;
        }
      }

      return false;
    }

    public String toString () {
      String out = this.cc.toString();

      for (Map.Entry<HoleId, Integer> entry : this.tally.entrySet()) {
        out += String.format("\n%s %d (%s)", entry.getKey(), entry.getValue(), this.getSATWeight(entry.getKey()));
      }

      return out;
    }

    public static MetaClassTree initialize () {
      MetaClassTree lit_a = new MetaClassTree(new CharLiteralNode('a'), new SimplePredicate('a'), 1);
      MetaClassTree lit_b = new MetaClassTree(new CharLiteralNode('b'), new SimplePredicate('b'), 1);
      MetaClassTree lit_c = new MetaClassTree(new CharLiteralNode('c'), new SimplePredicate('c'), 1);
      MetaClassTree lit_d = new MetaClassTree(new CharLiteralNode('d'), new SimplePredicate('d'), 1);
      MetaClassTree lit_e = new MetaClassTree(new CharLiteralNode('e'), new SimplePredicate('e'), 1);
      MetaClassTree lit_f = new MetaClassTree(new CharLiteralNode('f'), new SimplePredicate('f'), 1);
      MetaClassTree lit_g = new MetaClassTree(new CharLiteralNode('g'), new SimplePredicate('g'), 1);
      MetaClassTree lit_h = new MetaClassTree(new CharLiteralNode('h'), new SimplePredicate('h'), 1);
      MetaClassTree lit_i = new MetaClassTree(new CharLiteralNode('i'), new SimplePredicate('i'), 1);
      MetaClassTree lit_j = new MetaClassTree(new CharLiteralNode('j'), new SimplePredicate('j'), 1);
      MetaClassTree lit_k = new MetaClassTree(new CharLiteralNode('k'), new SimplePredicate('k'), 1);
      MetaClassTree lit_l = new MetaClassTree(new CharLiteralNode('l'), new SimplePredicate('l'), 1);
      MetaClassTree lit_m = new MetaClassTree(new CharLiteralNode('m'), new SimplePredicate('m'), 1);
      MetaClassTree lit_n = new MetaClassTree(new CharLiteralNode('n'), new SimplePredicate('n'), 1);
      MetaClassTree lit_o = new MetaClassTree(new CharLiteralNode('o'), new SimplePredicate('o'), 1);
      MetaClassTree lit_p = new MetaClassTree(new CharLiteralNode('p'), new SimplePredicate('p'), 1);
      MetaClassTree lit_q = new MetaClassTree(new CharLiteralNode('q'), new SimplePredicate('q'), 1);
      MetaClassTree lit_r = new MetaClassTree(new CharLiteralNode('r'), new SimplePredicate('r'), 1);
      MetaClassTree lit_s = new MetaClassTree(new CharLiteralNode('s'), new SimplePredicate('s'), 1);
      MetaClassTree lit_t = new MetaClassTree(new CharLiteralNode('t'), new SimplePredicate('t'), 1);
      MetaClassTree lit_u = new MetaClassTree(new CharLiteralNode('u'), new SimplePredicate('u'), 1);
      MetaClassTree lit_v = new MetaClassTree(new CharLiteralNode('v'), new SimplePredicate('v'), 1);
      MetaClassTree lit_w = new MetaClassTree(new CharLiteralNode('w'), new SimplePredicate('w'), 1);
      MetaClassTree lit_x = new MetaClassTree(new CharLiteralNode('x'), new SimplePredicate('x'), 1);
      MetaClassTree lit_y = new MetaClassTree(new CharLiteralNode('y'), new SimplePredicate('y'), 1);
      MetaClassTree lit_z = new MetaClassTree(new CharLiteralNode('z'), new SimplePredicate('z'), 1);

      MetaClassTree lit_A = new MetaClassTree(new CharLiteralNode('A'), new SimplePredicate('A'), 1);
      MetaClassTree lit_B = new MetaClassTree(new CharLiteralNode('B'), new SimplePredicate('B'), 1);
      MetaClassTree lit_C = new MetaClassTree(new CharLiteralNode('C'), new SimplePredicate('C'), 1);
      MetaClassTree lit_D = new MetaClassTree(new CharLiteralNode('D'), new SimplePredicate('D'), 1);
      MetaClassTree lit_E = new MetaClassTree(new CharLiteralNode('E'), new SimplePredicate('E'), 1);
      MetaClassTree lit_F = new MetaClassTree(new CharLiteralNode('F'), new SimplePredicate('F'), 1);
      MetaClassTree lit_G = new MetaClassTree(new CharLiteralNode('G'), new SimplePredicate('G'), 1);
      MetaClassTree lit_H = new MetaClassTree(new CharLiteralNode('H'), new SimplePredicate('H'), 1);
      MetaClassTree lit_I = new MetaClassTree(new CharLiteralNode('I'), new SimplePredicate('I'), 1);
      MetaClassTree lit_J = new MetaClassTree(new CharLiteralNode('J'), new SimplePredicate('J'), 1);
      MetaClassTree lit_K = new MetaClassTree(new CharLiteralNode('K'), new SimplePredicate('K'), 1);
      MetaClassTree lit_L = new MetaClassTree(new CharLiteralNode('L'), new SimplePredicate('L'), 1);
      MetaClassTree lit_M = new MetaClassTree(new CharLiteralNode('M'), new SimplePredicate('M'), 1);
      MetaClassTree lit_N = new MetaClassTree(new CharLiteralNode('N'), new SimplePredicate('N'), 1);
      MetaClassTree lit_O = new MetaClassTree(new CharLiteralNode('O'), new SimplePredicate('O'), 1);
      MetaClassTree lit_P = new MetaClassTree(new CharLiteralNode('P'), new SimplePredicate('P'), 1);
      MetaClassTree lit_Q = new MetaClassTree(new CharLiteralNode('Q'), new SimplePredicate('Q'), 1);
      MetaClassTree lit_R = new MetaClassTree(new CharLiteralNode('R'), new SimplePredicate('R'), 1);
      MetaClassTree lit_S = new MetaClassTree(new CharLiteralNode('S'), new SimplePredicate('S'), 1);
      MetaClassTree lit_T = new MetaClassTree(new CharLiteralNode('T'), new SimplePredicate('T'), 1);
      MetaClassTree lit_U = new MetaClassTree(new CharLiteralNode('U'), new SimplePredicate('U'), 1);
      MetaClassTree lit_V = new MetaClassTree(new CharLiteralNode('V'), new SimplePredicate('V'), 1);
      MetaClassTree lit_W = new MetaClassTree(new CharLiteralNode('W'), new SimplePredicate('W'), 1);
      MetaClassTree lit_X = new MetaClassTree(new CharLiteralNode('X'), new SimplePredicate('X'), 1);
      MetaClassTree lit_Y = new MetaClassTree(new CharLiteralNode('Y'), new SimplePredicate('Y'), 1);
      MetaClassTree lit_Z = new MetaClassTree(new CharLiteralNode('Z'), new SimplePredicate('Z'), 1);

      MetaClassTree lit_0 = new MetaClassTree(new CharLiteralNode('0'), new SimplePredicate('0'), 1);
      MetaClassTree lit_1 = new MetaClassTree(new CharLiteralNode('1'), new SimplePredicate('1'), 1);
      MetaClassTree lit_2 = new MetaClassTree(new CharLiteralNode('2'), new SimplePredicate('2'), 1);
      MetaClassTree lit_3 = new MetaClassTree(new CharLiteralNode('3'), new SimplePredicate('3'), 1);
      MetaClassTree lit_4 = new MetaClassTree(new CharLiteralNode('4'), new SimplePredicate('4'), 1);
      MetaClassTree lit_5 = new MetaClassTree(new CharLiteralNode('5'), new SimplePredicate('5'), 1);
      MetaClassTree lit_6 = new MetaClassTree(new CharLiteralNode('6'), new SimplePredicate('6'), 1);
      MetaClassTree lit_7 = new MetaClassTree(new CharLiteralNode('7'), new SimplePredicate('7'), 1);
      MetaClassTree lit_8 = new MetaClassTree(new CharLiteralNode('8'), new SimplePredicate('8'), 1);
      MetaClassTree lit_9 = new MetaClassTree(new CharLiteralNode('9'), new SimplePredicate('9'), 1);

      MetaClassTree cls_d = new MetaClassTree(class_d, pred_d, 2, lit_0, lit_1,
        lit_2, lit_3, lit_4, lit_5, lit_6, lit_7, lit_8, lit_9);

      MetaClassTree cls_AZ = new MetaClassTree(class_AZ, pred_AZ, 2, lit_A,
        lit_B, lit_C, lit_D, lit_E, lit_F, lit_G, lit_H, lit_I, lit_J, lit_K,
        lit_L, lit_M, lit_N, lit_O, lit_P, lit_Q, lit_R, lit_S, lit_T, lit_U,
        lit_V, lit_W, lit_X, lit_Y, lit_Z);

      MetaClassTree cls_az = new MetaClassTree(class_az, pred_az, 2, lit_a,
        lit_b, lit_c, lit_d, lit_e, lit_f, lit_g, lit_h, lit_i, lit_j, lit_k,
        lit_l, lit_m, lit_n, lit_o, lit_p, lit_q, lit_r, lit_s, lit_t, lit_u,
        lit_v, lit_w, lit_x, lit_y, lit_z);

      MetaClassTree cls_w = new MetaClassTree(class_w, pred_w, 3, cls_d, cls_AZ, cls_az);

      return cls_w;
    }
  }

  private List<Set<Route>> positives;
  private List<Set<Route>> negatives;

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

  public Formula (List<Set<Route>> positives, List<Set<Route>> negatives) {
    this.positives = positives;
    this.negatives = negatives;

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
    }

    return this.encodeWeightedConstraint(id, tree);
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
      System.out.println();
      System.out.println(PrintableTree.toString(this.tree));

      System.out.println(this.toString());
      this.model = this.opt.getModel();
      System.out.println(this.model);
      // System.exit(1);
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
