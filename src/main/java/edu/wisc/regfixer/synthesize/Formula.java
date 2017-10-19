package edu.wisc.regfixer.synthesize;

import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

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

  private static CharClass class_d = new CharEscapedNode('d');
  private static CharClass class_D = new CharEscapedNode('D');
  private static CharClass class_w = new CharEscapedNode('w');
  private static CharClass class_W = new CharEscapedNode('W');

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

  private Map<Predicate, CharClass> predToClass;
  private Map<HoleId, Set<BoolExpr>> holeToVars;
  private Map<BoolExpr, Predicate> varToPred;
  private MetaClassTally tally;

  public Formula (List<Set<Route>> positives, List<Set<Route>> negatives) {
    this.positives = positives;
    this.negatives = negatives;

    // Initialize SAT formula objects
    this.ctx = new Context();
    this.opt = this.ctx.mkOptimize();
    this.model = null;

    // Initialize structures for tracking state
    this.predToClass = new HashMap<>();
    this.predToClass.put(Formula.pred_d, Formula.class_d);
    this.predToClass.put(Formula.pred_D, Formula.class_D);
    this.predToClass.put(Formula.pred_w, Formula.class_w);
    this.predToClass.put(Formula.pred_W, Formula.class_W);
    this.holeToVars = new HashMap<>();
    this.varToPred = new HashMap<>();
    this.tally = new MetaClassTally();

    // Build the formula and encode meta-class formulae
    this.encodeSingleCharClasses();
    this.encodeMetaCharClasses();
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
      if (this.tally.hasTally(holeId) == false) {
        this.tally.initializeTally(holeId);
      }

      BoolExpr exprChars = this.encodeSingleChar(holeId, spans.get(holeId), posFlag);

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
    String name = String.format("%s_%s", id.toString(), cc.toString());
    BoolExpr var = this.ctx.mkBoolConst(name);

    if (this.holeToVars.get(id) == null) {
      this.holeToVars.put(id, new HashSet<>());
    }

    this.predToClass.put(pred, cc);
    this.holeToVars.get(id).add(var);
    this.varToPred.put(var, pred);
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
      this.model = this.opt.getModel();
    }

    /**
     * Build a map called `candidates` where for each hole in the expression a
     * set is built of all predicates that the formula determined were possible
     * solutions to that hole. Many of these predicates may overlap and later
     * the set of possible solutions will be simplified.
     */
    Map<HoleId, Set<Predicate>> candidatePreds = new HashMap<>();
    for (Map.Entry<HoleId, Set<BoolExpr>> entry : this.holeToVars.entrySet()) {
      HoleId id = entry.getKey();
      Set<BoolExpr> vars = entry.getValue();
      candidatePreds.put(id, new HashSet<>());

      for (BoolExpr var : vars) {
        if (this.model.evaluate(var, false).isTrue()) {
          candidatePreds.get(id).add(this.varToPred.get(var));
        }
      }
    }

    /**
     * From the list of possible solutions for each hole, condense those
     * character classes to a single character class (either select one class
     * from the list of candidates or combine multiple candidates into a new
     * composite character class).
     */
    Map<HoleId, Set<CharClass>> candidateClasses = new HashMap<>();
    for (Map.Entry<HoleId, Set<Predicate>> entry : candidatePreds.entrySet()) {
      HoleId id = entry.getKey();
      candidateLoop:
      for (Predicate candidate : entry.getValue()) {
        /**
         * Reject a candidate if that candidate is completly within the bounds
         * of another predicate within the list of candidates.
         */
        for (Predicate test : entry.getValue()) {
          if (candidate == test) {
            continue;
          } else if (test.includes(candidate)) {
            continue candidateLoop;
          }
        }

        if (candidateClasses.containsKey(id) == false) {
          candidateClasses.put(id, new HashSet<>());
        }

        CharClass cc = this.predToClass.get(candidate);
        candidateClasses.get(id).add(cc);
      }
    }

    /**
     * If the candidate classes could be reduced to a single character class,
     * use that class as a solution for a particular hole. If multiple character
     * classes were required, combine those character classes into a single
     * character class.
     */
    Map<HoleId, CharClass> solutionClasses = new HashMap<>();
    for (Map.Entry<HoleId, Set<CharClass>> entry : candidateClasses.entrySet()) {
      HoleId id = entry.getKey();
      Set<CharClass> classes = entry.getValue();

      if (classes.size() == 0) {
        throw new SynthesisFailure("SAT produced no solutions for " + id.toString());
      } else if (classes.size() == 1) {
        CharClass cc = classes.iterator().next();
        solutionClasses.put(id, cc);
      } else {
        Collection<CharRangeNode> subClasses = new HashSet<>();

        for (CharClass subClass : classes) {
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

        CharClass cc = new CharClassSetNode(subClasses);
        solutionClasses.put(id, cc);
      }
    }

    return solutionClasses;
  }
}
