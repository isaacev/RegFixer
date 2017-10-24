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
    private int layerWeight;
    private MetaClassTree parent;
    private List<MetaClassTree> children;
    private Map<HoleId, Integer> tally;

    public MetaClassTree (CharClass cc, Predicate pred, int layerWeight, MetaClassTree... children) {
      this(cc, pred, layerWeight, Arrays.asList(children));
    }

    public MetaClassTree (CharClass cc, Predicate pred, int layerWeight, List<MetaClassTree> children) {
      this.cc          = cc;
      this.pred        = pred;
      this.layerWeight = layerWeight;
      this.parent      = null;
      this.children    = children;
      this.tally       = new HashMap<>();

      for (MetaClassTree child : this.children) {
        child.parent = this;
      }
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

    private int sumChildWeights (HoleId id) {
      int sum = 0;
      for (MetaClassTree child : this.children) {
        if (child.tally.containsKey(id)) {
          sum += child.getLayerWeight();
        }
      }
      return sum;
    }

    public int getLayerWeight () {
      return this.layerWeight;
    }

    public int getSATWeight (HoleId id) {
      if (this.tally.containsKey(id)) {
        return this.layerWeight - this.sumChildWeights(id);
      }

      return 0;
    }

    private boolean isSatisfied (char ch) {
      return this.pred.includes(ch);
    }

    public int getTally (HoleId id) {
      return (this.tally.containsKey(id)) ? this.tally.get(id) : 0;
    }

    public boolean incrementTally (HoleId id, char ch) {
      if (this.isSatisfied(ch) == false) {
        return false;
      }

      this.tally.put(id, this.getTally(id) + 1);

      for (MetaClassTree child : this.children) {
        child.incrementTally(id, ch);
      }

      return true;
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

    public List<MetaClassTree> getCandidateBranches (HoleId id) {
      /**
       * Literal-classes should only be considered for inclusion in the SAT
       * formula if:
       * - Class has more than 0 tallies
       *
       * Meta-classes should only be considered for inclusion in the SAT
       * formula if the class meets all of the following criteria:
       * - Class has more than 0 tallies
       * - If class has child classes, at least 2 have tallies
       */
      List<MetaClassTree> trees = new LinkedList<>();

      if (this.getTally(id) > 0) {
        // countTalliedChildren is the number of child classes that match at
        // least 1 character passing through the specified hole.
        int countTalliedChildren = this.children.stream()
          .filter(c -> c.getTally(id) > 0)
          .collect(Collectors.toSet())
          .size();

        // If this class has at least 2 child classes then this class should be
        // considered as a possible solution.
        if (countTalliedChildren >= 2) {
          trees.add(this);
        } else if (this.children.size() == 0) {
          trees.add(this);
        }

        // Recursively check child classes to determine if those classes should
        // be considered as possible solutions.
        for (MetaClassTree child : this.children) {
          trees.addAll(child.getCandidateBranches(id));
        }
      }

      return trees;
    }

    public String toString () {
      String out = this.cc.toString();

      for (Map.Entry<HoleId, Integer> entry : this.tally.entrySet()) {
        out += String.format("\n%s %d (%s)", entry.getKey(), entry.getValue(), this.getSATWeight(entry.getKey()));
      }

      return out;
    }

    public static MetaClassTree initialize () {
      MetaClassTree lit_a = new MetaClassTree(new CharLiteralNode('a'), new SimplePredicate('a'), -2);
      MetaClassTree lit_b = new MetaClassTree(new CharLiteralNode('b'), new SimplePredicate('b'), -2);
      MetaClassTree lit_c = new MetaClassTree(new CharLiteralNode('c'), new SimplePredicate('c'), -2);
      MetaClassTree lit_d = new MetaClassTree(new CharLiteralNode('d'), new SimplePredicate('d'), -2);
      MetaClassTree lit_e = new MetaClassTree(new CharLiteralNode('e'), new SimplePredicate('e'), -2);
      MetaClassTree lit_f = new MetaClassTree(new CharLiteralNode('f'), new SimplePredicate('f'), -2);
      MetaClassTree lit_g = new MetaClassTree(new CharLiteralNode('g'), new SimplePredicate('g'), -2);
      MetaClassTree lit_h = new MetaClassTree(new CharLiteralNode('h'), new SimplePredicate('h'), -2);
      MetaClassTree lit_i = new MetaClassTree(new CharLiteralNode('i'), new SimplePredicate('i'), -2);
      MetaClassTree lit_j = new MetaClassTree(new CharLiteralNode('j'), new SimplePredicate('j'), -2);
      MetaClassTree lit_k = new MetaClassTree(new CharLiteralNode('k'), new SimplePredicate('k'), -2);
      MetaClassTree lit_l = new MetaClassTree(new CharLiteralNode('l'), new SimplePredicate('l'), -2);
      MetaClassTree lit_m = new MetaClassTree(new CharLiteralNode('m'), new SimplePredicate('m'), -2);
      MetaClassTree lit_n = new MetaClassTree(new CharLiteralNode('n'), new SimplePredicate('n'), -2);
      MetaClassTree lit_o = new MetaClassTree(new CharLiteralNode('o'), new SimplePredicate('o'), -2);
      MetaClassTree lit_p = new MetaClassTree(new CharLiteralNode('p'), new SimplePredicate('p'), -2);
      MetaClassTree lit_q = new MetaClassTree(new CharLiteralNode('q'), new SimplePredicate('q'), -2);
      MetaClassTree lit_r = new MetaClassTree(new CharLiteralNode('r'), new SimplePredicate('r'), -2);
      MetaClassTree lit_s = new MetaClassTree(new CharLiteralNode('s'), new SimplePredicate('s'), -2);
      MetaClassTree lit_t = new MetaClassTree(new CharLiteralNode('t'), new SimplePredicate('t'), -2);
      MetaClassTree lit_u = new MetaClassTree(new CharLiteralNode('u'), new SimplePredicate('u'), -2);
      MetaClassTree lit_v = new MetaClassTree(new CharLiteralNode('v'), new SimplePredicate('v'), -2);
      MetaClassTree lit_w = new MetaClassTree(new CharLiteralNode('w'), new SimplePredicate('w'), -2);
      MetaClassTree lit_x = new MetaClassTree(new CharLiteralNode('x'), new SimplePredicate('x'), -2);
      MetaClassTree lit_y = new MetaClassTree(new CharLiteralNode('y'), new SimplePredicate('y'), -2);
      MetaClassTree lit_z = new MetaClassTree(new CharLiteralNode('z'), new SimplePredicate('z'), -2);

      MetaClassTree lit_A = new MetaClassTree(new CharLiteralNode('A'), new SimplePredicate('A'), -2);
      MetaClassTree lit_B = new MetaClassTree(new CharLiteralNode('B'), new SimplePredicate('B'), -2);
      MetaClassTree lit_C = new MetaClassTree(new CharLiteralNode('C'), new SimplePredicate('C'), -2);
      MetaClassTree lit_D = new MetaClassTree(new CharLiteralNode('D'), new SimplePredicate('D'), -2);
      MetaClassTree lit_E = new MetaClassTree(new CharLiteralNode('E'), new SimplePredicate('E'), -2);
      MetaClassTree lit_F = new MetaClassTree(new CharLiteralNode('F'), new SimplePredicate('F'), -2);
      MetaClassTree lit_G = new MetaClassTree(new CharLiteralNode('G'), new SimplePredicate('G'), -2);
      MetaClassTree lit_H = new MetaClassTree(new CharLiteralNode('H'), new SimplePredicate('H'), -2);
      MetaClassTree lit_I = new MetaClassTree(new CharLiteralNode('I'), new SimplePredicate('I'), -2);
      MetaClassTree lit_J = new MetaClassTree(new CharLiteralNode('J'), new SimplePredicate('J'), -2);
      MetaClassTree lit_K = new MetaClassTree(new CharLiteralNode('K'), new SimplePredicate('K'), -2);
      MetaClassTree lit_L = new MetaClassTree(new CharLiteralNode('L'), new SimplePredicate('L'), -2);
      MetaClassTree lit_M = new MetaClassTree(new CharLiteralNode('M'), new SimplePredicate('M'), -2);
      MetaClassTree lit_N = new MetaClassTree(new CharLiteralNode('N'), new SimplePredicate('N'), -2);
      MetaClassTree lit_O = new MetaClassTree(new CharLiteralNode('O'), new SimplePredicate('O'), -2);
      MetaClassTree lit_P = new MetaClassTree(new CharLiteralNode('P'), new SimplePredicate('P'), -2);
      MetaClassTree lit_Q = new MetaClassTree(new CharLiteralNode('Q'), new SimplePredicate('Q'), -2);
      MetaClassTree lit_R = new MetaClassTree(new CharLiteralNode('R'), new SimplePredicate('R'), -2);
      MetaClassTree lit_S = new MetaClassTree(new CharLiteralNode('S'), new SimplePredicate('S'), -2);
      MetaClassTree lit_T = new MetaClassTree(new CharLiteralNode('T'), new SimplePredicate('T'), -2);
      MetaClassTree lit_U = new MetaClassTree(new CharLiteralNode('U'), new SimplePredicate('U'), -2);
      MetaClassTree lit_V = new MetaClassTree(new CharLiteralNode('V'), new SimplePredicate('V'), -2);
      MetaClassTree lit_W = new MetaClassTree(new CharLiteralNode('W'), new SimplePredicate('W'), -2);
      MetaClassTree lit_X = new MetaClassTree(new CharLiteralNode('X'), new SimplePredicate('X'), -2);
      MetaClassTree lit_Y = new MetaClassTree(new CharLiteralNode('Y'), new SimplePredicate('Y'), -2);
      MetaClassTree lit_Z = new MetaClassTree(new CharLiteralNode('Z'), new SimplePredicate('Z'), -2);

      MetaClassTree lit_0 = new MetaClassTree(new CharLiteralNode('0'), new SimplePredicate('0'), -2);
      MetaClassTree lit_1 = new MetaClassTree(new CharLiteralNode('1'), new SimplePredicate('1'), -2);
      MetaClassTree lit_2 = new MetaClassTree(new CharLiteralNode('2'), new SimplePredicate('2'), -2);
      MetaClassTree lit_3 = new MetaClassTree(new CharLiteralNode('3'), new SimplePredicate('3'), -2);
      MetaClassTree lit_4 = new MetaClassTree(new CharLiteralNode('4'), new SimplePredicate('4'), -2);
      MetaClassTree lit_5 = new MetaClassTree(new CharLiteralNode('5'), new SimplePredicate('5'), -2);
      MetaClassTree lit_6 = new MetaClassTree(new CharLiteralNode('6'), new SimplePredicate('6'), -2);
      MetaClassTree lit_7 = new MetaClassTree(new CharLiteralNode('7'), new SimplePredicate('7'), -2);
      MetaClassTree lit_8 = new MetaClassTree(new CharLiteralNode('8'), new SimplePredicate('8'), -2);
      MetaClassTree lit_9 = new MetaClassTree(new CharLiteralNode('9'), new SimplePredicate('9'), -2);

      MetaClassTree cls_d = new MetaClassTree(class_d, pred_d, 3, lit_0, lit_1,
        lit_2, lit_3, lit_4, lit_5, lit_6, lit_7, lit_8, lit_9);

      MetaClassTree cls_AZ = new MetaClassTree(class_AZ, pred_AZ, 3, lit_A,
        lit_B, lit_C, lit_D, lit_E, lit_F, lit_G, lit_H, lit_I, lit_J, lit_K,
        lit_L, lit_M, lit_N, lit_O, lit_P, lit_Q, lit_R, lit_S, lit_T, lit_U,
        lit_V, lit_W, lit_X, lit_Y, lit_Z);

      MetaClassTree cls_az = new MetaClassTree(class_az, pred_az, 3, lit_a,
        lit_b, lit_c, lit_d, lit_e, lit_f, lit_g, lit_h, lit_i, lit_j, lit_k,
        lit_l, lit_m, lit_n, lit_o, lit_p, lit_q, lit_r, lit_s, lit_t, lit_u,
        lit_v, lit_w, lit_x, lit_y, lit_z);

      MetaClassTree cls_w = new MetaClassTree(class_w, pred_w, 4, cls_d, cls_AZ, cls_az);

      return cls_w;
    }
  }

  private static class MetaClassTally {
    public Map<HoleId, Map<Predicate, Integer>> tally;
    private Map<Predicate, Integer> weights;

    public MetaClassTally () {
      this.tally = new HashMap<>();
      this.weights = new HashMap<>();
    }

    public boolean hasTally (HoleId id) {
      return this.tally.containsKey(id);
    }

    public void initializeTally (HoleId id) {
      this.tally.put(id, new HashMap<>());

      // Adds \d meta-class (aka [0-9])
      this.addMetaClassToTally(id, 3, Formula.pred_d);

      // Adds \D meta-class (aka [^0-9])
      this.addMetaClassToTally(id, 3, Formula.pred_D);

      // Adds \w meta-class (aka [a-zA-Z0-9_])
      this.addMetaClassToTally(id, 5, Formula.pred_w);

      // Adds \W meta-class (aka [^a-zA-Z0-9_])
      this.addMetaClassToTally(id, 5, Formula.pred_W);
    }

    private void addMetaClassToTally (HoleId id, int weight, Predicate pred) {
      this.tally.get(id).put(pred, 0);
      this.weights.put(pred, weight);
    }

    /**
     * For each predicate associated with a hole a tally is being kept of how
     * many characters passing through that hole are included in that predicate.
     * This function is responsible for updating that tally every time it is
     * called with a hole ID and a character. For each predicate associated with
     * that hole, if the character satisifes the predicate, increment that
     * predicate's tally by 1.
     */
    public void increment (HoleId id, Character ch) {
      for (Map.Entry<Predicate, Integer> entry : this.getEntries(id)) {
        if (entry.getKey().includes(ch)) {
          entry.setValue(entry.getValue() + 1);
        }
      }
    }

    public Set<HoleId> getHoleIds () {
      return this.tally.keySet();
    }

    public Set<Map.Entry<Predicate, Integer>> getEntries (HoleId id) {
      return this.tally.get(id).entrySet();
    }

    public int getWeight (Predicate pred) {
      if (this.weights.containsKey(pred)) {
        return this.weights.get(pred);
      } else {
        return -2;
      }
    }
  }

  private List<Set<Route>> positives;
  private List<Set<Route>> negatives;

  private Context ctx;
  private Optimize opt;
  private Model model;

  private int nextVarId;
  private Set<HoleId> holes;
  private Map<Predicate, CharClass> predToClass;
  private Map<HoleId, Set<BoolExpr>> holeToVars;
  private Map<BoolExpr, MetaClassTree> varToTree;
  private Map<BoolExpr, Predicate> varToPred;
  private MetaClassTally tally;
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
    this.predToClass = new HashMap<>();
    this.predToClass.put(Formula.pred_d, Formula.class_d);
    this.predToClass.put(Formula.pred_D, Formula.class_D);
    this.predToClass.put(Formula.pred_w, Formula.class_w);
    this.predToClass.put(Formula.pred_W, Formula.class_W);
    this.predToClass.put(Formula.pred_azAZ, Formula.class_azAZ);
    this.holeToVars = new HashMap<>();
    this.varToTree = new HashMap<>();
    this.varToPred = new HashMap<>();
    this.tally = new MetaClassTally();
    this.tree = MetaClassTree.initialize();
    this.misc = new HashSet<>();

    // Build the formula and encode meta-class formulae
    this.encodeSingleCharClasses();

    for (HoleId id : this.holes) {
      this.encodeCharClass(id, this.tree);
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
      if (this.tally.hasTally(holeId) == false) {
        this.tally.initializeTally(holeId);
      }

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
    if (posFlag) {
      /**
       * Characters that can be placed in the character-class tree are encoded
       * in the SAT formula later so don't return a variable here.
       */
      if (this.tree.incrementTally(id, ch)) {
        return null;
      }
    }

    BoolExpr var = this.createVariable(id);
    MetaClassTree tree = new MetaClassTree(new CharLiteralNode(ch), new SimplePredicate(ch), -2);

    // Categorize this new tree as "miscellaneous".
    this.misc.add(tree);

    // Register variable with variable -> tree mapping.
    this.varToTree.put(var, tree);

    return var;
  }

  private BoolExpr encodeSingleChar (HoleId holeId, Set<Character> chars, boolean posFlag) {
    BoolExpr exprChars = null;

    for (Character ch : chars) {
      Predicate pred = new SimplePredicate(ch);
      CharClass cc = new CharLiteralNode(ch);
      BoolExpr exprChar = this.registerPredicate(holeId, cc, pred);
      this.opt.AssertSoft(exprChar, -2, "MAX_SAT");
      this.tally.increment(holeId, ch);

      if (posFlag) {
        if (exprChars == null) {
          exprChars = exprChar;
        } else {
          exprChars = this.ctx.mkAnd(exprChars, exprChar);
        }
      } else {
        if (exprChars == null) {
          exprChars = this.ctx.mkNot(exprChar);
        } else {
          exprChars = this.ctx.mkOr(exprChars, this.ctx.mkNot(exprChar));
        }
      }

    }

    return exprChars;
  }

  /**
   * Each boolean variable in the SAT formula needs to be assigned a unique
   * name, mapped to its corresponding hole (for record keeping), and mapped to
   * its corresponding predicate (also for record keeping).
   */
  private BoolExpr registerPredicate (HoleId id, CharClass cc, Predicate pred) {
    String name = String.format("%s_%d", id.toString(), this.nextVarId++);
    BoolExpr var = this.ctx.mkBoolConst(name);

    if (this.holeToVars.get(id) == null) {
      this.holeToVars.put(id, new HashSet<>());
    }

    this.predToClass.put(pred, cc);
    this.holeToVars.get(id).add(var);
    this.varToPred.put(var, pred);
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
        s.add(encodeSimpleCharClass(id, tree));
      } else {
        s.add(encodeMetaCharClass(id, vars, tree));
      }

      return s;
    }

    return vars;
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

  private BoolExpr encodeSimpleCharClass (HoleId id, MetaClassTree tree) {
    BoolExpr var = this.createVariable(id);

    // Register variable with variable -> tree mapping.
    this.varToTree.put(var, tree);

    int weight = tree.getSATWeight(id);
    this.opt.AssertSoft(var, weight, "MAX_SAT");
    return var;
  }

  private BoolExpr encodeMetaCharClass (HoleId id, Set<BoolExpr> vars, MetaClassTree tree) {
    BoolExpr var = this.createVariable(id);

    // Register variable with variable -> tree mapping.
    this.varToTree.put(var, tree);

    int weight = tree.getSATWeight(id);
    this.opt.AssertSoft(var, weight, "MAX_SAT");

    BoolExpr relations = vars
      .stream()
      .map(v -> this.ctx.mkOr(this.ctx.mkNot(var), v))
      .reduce(null, (accum, v) -> (accum == null) ? v : this.ctx.mkAnd(accum, v));

    if (relations != null) {
      this.opt.Add(relations);
    }

    return var;
  }

  private void encodeMetaCharClasses () {
    BoolExpr exprs = null;

    /**
     * As individual characters were being added to the SAT formula a tally was
     * being kept of which meta-classes fit the most characters. If any
     * meta-classes fir MORE than 2 characters, that meta-class will be added to
     * the SAT formula as a potential solution for its corresponding hole.
     */
    int METACLASS_THRESHOLD = 2;
    for (HoleId id : this.tally.getHoleIds()) {
      for (Map.Entry<Predicate, Integer> entry : this.tally.getEntries(id)) {
        if (entry.getValue() > METACLASS_THRESHOLD) {
          Predicate pred = entry.getKey();
          CharClass cc = this.predToClass.get(pred);
          this.encodeMetaCharClassForHole(id, cc, pred);
        }
      }
    }
  }

  private void encodeMetaCharClassForHole (HoleId holeId, CharClass cc, Predicate pred) {
    /**
     * Create a SAT variable and associate that variable with this predicate. Give
     * that variable a weight corresponding to how favorable the algorithm favors
     * the predicate.
     */
    BoolExpr exprs = null;
    BoolExpr expr = this.registerPredicate(holeId, cc, pred);
    this.opt.AssertSoft(expr, this.tally.getWeight(pred), "MAX_SAT");

    /**
     * Any predicate passed to this method has already been determined to include
     * at least 2 characters passing through this hole. However many other
     * characters that are NOT included in this predicate may have also passed
     * through this hole. The following loop checks each existing SAT variable
     * associated with this hole and--if that variable corresponds to a character
     * included in this predicate--logically links this predicate and the existing
     * variable in the SAT formula.
     */
    for (BoolExpr existingVar : this.holeToVars.get(holeId)) {
      Predicate existingPred = this.varToPred.get(existingVar);

      if (pred.includes(existingPred)) {
        if (exprs == null) {
          exprs = this.ctx.mkOr(this.ctx.mkNot(expr), existingVar);
        } else {
          exprs = this.ctx.mkAnd(exprs, this.ctx.mkOr(this.ctx.mkNot(expr), existingVar));
        }
      }
    }

    if (exprs != null) {
      this.opt.Add(exprs);
    }
  }

  public Map<HoleId, CharClass> solve () throws SynthesisFailure {
    /**
     * First, check that the formula was satisifed
     */
    if (this.opt.Check() == Status.UNSATISFIABLE) {
      throw new SynthesisFailure("unsatisfiable SAT formula");
    } else {
      for (HoleId id : this.holes) {
        for (MetaClassTree candidate : this.tree.getCandidateBranches(id)) {
          System.out.println(PrintableTree.toString(candidate));
        }
      }

      System.out.println(this.toString());
      this.model = this.opt.getModel();
      System.out.println(this.model);
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
