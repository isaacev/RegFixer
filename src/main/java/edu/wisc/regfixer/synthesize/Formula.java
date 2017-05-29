package edu.wisc.regfixer.synthesize;

import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.BinaryOperator;
import java.util.function.Function;

import com.microsoft.z3.BoolExpr;
import com.microsoft.z3.Context;
import com.microsoft.z3.Optimize;
import com.microsoft.z3.Status;
import com.microsoft.z3.Model;
import edu.wisc.regfixer.automata.Route;
import edu.wisc.regfixer.parser.CharClass;
import edu.wisc.regfixer.parser.CharLiteralNode;
import edu.wisc.regfixer.parser.CharEscapedNode;
import edu.wisc.regfixer.enumerate.HoleId;

public class Formula {
  private Context ctx;
  private Optimize opt;
  private Set<BoolExpr> vars;
  private Map<BoolExpr, HoleId> varToHoleId;
  private Map<BoolExpr, CharClass> varToCharClass;
  private Model model;

  public Formula () {
    this.ctx = new Context();
    this.opt = this.ctx.mkOptimize();
    this.vars = new HashSet<>();
    this.varToHoleId = new HashMap<>();
    this.varToCharClass = new HashMap<>();
    this.model = null;
  }

  public boolean isUnSatisfiable () {
    return (this.opt.Check() == Status.UNSATISFIABLE);
  }

  public Optimize getOptimize () {
    return this.opt;
  }

  public Set<BoolExpr> getVariables () {
    return this.vars;
  }

  public boolean variableEvaluatesTrue (BoolExpr var) {
    if (this.model == null) {
      this.model = this.opt.getModel();
    }

    return this.model.evaluate(var, false).isTrue();
  }

  public HoleId getHoleIdForVariable (BoolExpr var) {
    return this.varToHoleId.get(var);
  }

  public CharClass getCharClassForVariable (BoolExpr var) {
    return this.varToCharClass.get(var);
  }

  public BoolExpr registerVariable (HoleId holeId, CharClass charClass) {
    String varName = String.format("%s_C%s", holeId, charClass);
    BoolExpr varExpr = this.ctx.mkBoolConst(varName);

    this.vars.add(varExpr);
    this.varToHoleId.put(varExpr, holeId);
    this.varToCharClass.put(varExpr, charClass);

    return varExpr;
  }

  private static class ExprPredPair {
    BoolExpr expr;
    BoolExpr pred;

    ExprPredPair () {
      this(null, null);
    }

    ExprPredPair (BoolExpr expr, BoolExpr pred) {
      this.expr = expr;
      this.pred = pred;
    }
  }

  public static Formula build (List<Set<Route>> positives, List<Set<Route>> negatives) {
    Formula frm = new Formula();

    for (Set<Route> routes : positives) {
      if (routes.stream().anyMatch(r -> !r.isEmpty())) {
        buildExampleFormula(frm, routes, true);
      }
    }

    for (Set<Route> routes : negatives) {
      if (routes.stream().anyMatch(r -> !r.isEmpty())) {
        buildExampleFormula(frm, routes, false);
      }
    }

    return frm;
  }

  private static void buildExampleFormula (Formula frm, Set<Route> routes, boolean isPosExample) {
    final BinaryOperator<ExprPredPair> mergeFormulae = (accum, pair) -> {
      if (accum.expr == null) {
        accum.expr = pair.expr;
      } else if (isPosExample) {
        accum.expr = frm.ctx.mkOr(accum.expr, pair.expr);
      } else {
        accum.expr = frm.ctx.mkAnd(accum.expr, pair.expr);
      }

      if (accum.pred == null) {
        accum.pred = pair.pred;
      } else if (pair.pred != null) {
        accum.pred = frm.ctx.mkAnd(accum.pred, pair.pred);
      }

      return accum;
    };

    ExprPredPair pair = routes.stream()
      .filter(route -> !route.isEmpty())
      .map(route -> buildRouteFormula(frm, route, isPosExample))
      .reduce(new ExprPredPair(), mergeFormulae);

    frm.opt.Add(pair.expr);

    if (pair.pred != null) {
      frm.opt.Add(pair.pred);
    }
  }

  private static ExprPredPair buildRouteFormula (Formula frm, Route route, boolean isPosExample) {
    final BinaryOperator<ExprPredPair> mergeFormulae = (accum, pair) -> {
      if (accum.expr == null) {
        accum.expr = pair.expr;
      } else if (isPosExample) {
        accum.expr = frm.ctx.mkAnd(accum.expr, pair.expr);
      } else {
        accum.expr = frm.ctx.mkOr(accum.expr, pair.expr);
      }

      if (accum.pred == null) {
        accum.pred = pair.pred;
      } else if (pair.pred != null) {
        accum.pred = frm.ctx.mkAnd(accum.pred, pair.pred);
      }

      return accum;
    };

    return route.getSpans()
                .entrySet()
                .stream()
                .map(hole -> buildHoleFormula(frm, hole, isPosExample))
                .reduce(new ExprPredPair(), mergeFormulae);
  }

  private static ExprPredPair buildHoleFormula (Formula frm, Entry<HoleId, Set<Character>> hole, boolean isPositive) {
    final Function<Character, ExprPredPair> charToFormula = (ch) -> {
      HoleId holeId = hole.getKey();
      BoolExpr expr = frm.registerVariable(holeId, new CharLiteralNode(ch));
      return new ExprPredPair(expr, null);
    };

    AtomicInteger index = new AtomicInteger();
    final BinaryOperator<ExprPredPair> mergeFormulae = (accum, pair) -> {
      if (index.getAndIncrement() > 1) {
        HoleId holeId = hole.getKey();
        BoolExpr pred = frm.registerVariable(holeId, new CharEscapedNode('w'));
        frm.opt.AssertSoft(pred, 5, "MAX_SAT");

        if (accum.pred == null) {
          accum.pred = frm.ctx.mkOr(frm.ctx.mkNot(pred), pair.expr);
        } else if (pred != null) {
          accum.pred = frm.ctx.mkAnd(accum.pred, frm.ctx.mkOr(frm.ctx.mkNot(pred), pair.expr));
        }
      }

      frm.opt.AssertSoft(pair.expr, -2, "MAX_SAT");

      if (accum.expr == null) {
        if (isPositive) {
          accum.expr = pair.expr;
        } else {
          accum.expr = frm.ctx.mkNot(pair.expr);
        }
      } else {
        if (isPositive) {
          accum.expr = frm.ctx.mkAnd(accum.expr, pair.expr);
        } else {
          accum.expr = frm.ctx.mkOr(accum.expr, frm.ctx.mkNot(pair.expr));
        }
      }

      return accum;
    };

    return hole.getValue()
               .stream()
               .map(charToFormula)
               .reduce(new ExprPredPair(), mergeFormulae);
  }
}
