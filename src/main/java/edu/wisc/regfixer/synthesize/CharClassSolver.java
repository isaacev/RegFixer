package edu.wisc.regfixer.synthesize;

import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map.Entry;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.BinaryOperator;
import java.util.function.Function;
import java.util.stream.Collectors;

import com.microsoft.z3.BoolExpr;
import com.microsoft.z3.Context;
import com.microsoft.z3.Model;
import com.microsoft.z3.Optimize;
import com.microsoft.z3.Status;
import edu.wisc.regfixer.automata.Route;
import edu.wisc.regfixer.parser.CharClass;
import edu.wisc.regfixer.parser.CharEscapedNode;
import edu.wisc.regfixer.parser.CharLiteralNode;

public class CharClassSolver {
  public static Map<Integer, CharClass> solve (List<Set<Route>> positives, List<Set<Route>> negatives) throws SynthesisFailure {
    InnerState state = new InnerState();

    for (Set<Route> routes : positives) {
      ExprPredPair pair = buildFormula(state, routes, true);
      state.opt.Add(pair.expr);
      state.opt.Add(pair.pred);
    }

    for (Set<Route> routes : negatives) {
      ExprPredPair pair = buildFormula(state, routes, false);
      state.opt.Add(pair.expr);
      state.opt.Add(pair.pred);
    }

    return solveFormula(state);
  }

  private static class InnerState {
    Context ctx;
    Optimize opt;
    Set<BoolExpr> vars;
    Map<BoolExpr, Integer> varToHoleId;
    Map<BoolExpr, CharClass> varToCharClass;

    public InnerState () {
      this.ctx = new Context();
      this.opt = this.ctx.mkOptimize();
      this.vars = new HashSet<>();
      this.varToHoleId = new HashMap<>();
      this.varToCharClass = new HashMap<>();
    }
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

  private static Map<Integer, CharClass> solveFormula (InnerState state) throws SynthesisFailure {
    if (state.opt.Check() == Status.UNSATISFIABLE) {
      throw new SynthesisFailure("failed to solve formula");
    }

    Model model = state.opt.getModel();
    System.out.println(state.opt.toString());
    System.out.println(model.toString());
    Map<Integer, Set<CharClass>> candidates = new HashMap<>();
    for (BoolExpr var : state.vars) {
      if (model.evaluate(var, false).isTrue()) {
        Integer holeId = state.varToHoleId.get(var);
        CharClass charClass = state.varToCharClass.get(var);

        if (candidates.containsKey(holeId) == false) {
          candidates.put(holeId, new HashSet<CharClass>());
        }

        candidates.get(holeId).add(charClass);
      }
    }

    Map<Integer, CharClass> solution = new HashMap<>();
    for (Integer holeId : candidates.keySet()) {
      Set<CharClass> charClasses = candidates.get(holeId);

      if (charClasses.contains("\\w")) {
        solution.put(holeId, new CharEscapedNode('w'));
      } else {
        String charClass = String.format("[%s]", String.join("", charClasses.stream().map(c -> c.toString()).collect(Collectors.toList())));
        solution.put(holeId, new CharLiteralNode('w'));
      }
    }

    return solution;
  }

  private static ExprPredPair buildFormula (InnerState state, Set<Route> routes, boolean isPositive) {
    final BinaryOperator<ExprPredPair> mergeFormulae = (accum, pair) -> {
      if (accum.expr == null) {
        accum.expr = pair.expr;
      } else if (isPositive) {
        accum.expr = state.ctx.mkOr(accum.expr, pair.expr);
      } else {
        accum.expr = state.ctx.mkAnd(accum.expr, pair.expr);
      }

      if (accum.pred == null) {
        accum.pred = pair.pred;
      } else {
        accum.pred = state.ctx.mkAnd(accum.pred, pair.pred);
      }

      return accum;
    };

    return routes.stream()
                 .map(route -> buildRouteFormula(state, route, isPositive))
                 .reduce(new ExprPredPair(), mergeFormulae);
  }

  private static ExprPredPair buildRouteFormula (InnerState state, Route route, boolean isPositive) {
    final BinaryOperator<ExprPredPair> mergeFormulae = (accum, pair) -> {
      if (accum.expr == null) {
        accum.expr = pair.expr;
      } else if (isPositive) {
        accum.expr = state.ctx.mkAnd(accum.expr, pair.expr);
      } else {
        accum.expr = state.ctx.mkOr(accum.expr, pair.expr);
      }

      if (accum.pred == null) {
        accum.pred = pair.pred;
      } else {
        accum.pred = state.ctx.mkAnd(accum.pred, pair.pred);
      }

      return accum;
    };

    return route.getSpans()
                .entrySet()
                .stream()
                .map(hole -> buildHoleFormula(state, hole, isPositive))
                .reduce(new ExprPredPair(), mergeFormulae);
  }

  private static ExprPredPair buildHoleFormula (InnerState state, Entry<Integer, Set<Character>> hole, boolean isPositive) {
    final Function<Character, ExprPredPair> charToFormula = (ch) -> {
      Integer holeId = hole.getKey();
      BoolExpr expr = registerVariable(state, holeId, new CharLiteralNode(ch));
      BoolExpr pred = registerVariable(state, holeId, new CharEscapedNode('w'));
      return new ExprPredPair(expr, pred);
    };

    AtomicInteger index = new AtomicInteger();
    final BinaryOperator<ExprPredPair> mergeFormulae = (accum, pair) -> {
      if (index.getAndIncrement() > 1) {
        state.opt.AssertSoft(pair.pred, 5, "MAX_SAT");
      }

      state.opt.AssertSoft(pair.expr, -2, "MAX_SAT");

      if (accum.expr == null) {
        if (isPositive) {
          accum.expr = pair.expr;
        } else {
          accum.expr = state.ctx.mkNot(pair.expr);
        }
      } else {
        if (isPositive) {
          accum.expr = state.ctx.mkAnd(accum.expr, pair.expr);
        } else {
          accum.expr = state.ctx.mkOr(accum.expr, state.ctx.mkNot(pair.expr));
        }
      }

      if (accum.pred == null) {
        accum.pred = state.ctx.mkOr(state.ctx.mkNot(pair.pred), pair.expr);
      } else {
        accum.pred = state.ctx.mkAnd(accum.pred, state.ctx.mkOr(state.ctx.mkNot(pair.pred), pair.expr));
      }

      return accum;
    };

    return hole.getValue()
               .stream()
               .map(charToFormula)
               .reduce(new ExprPredPair(), mergeFormulae);
  }

  private static BoolExpr registerVariable (InnerState state, Integer holeId, CharClass charClass) {
    String name = String.format("H%d_C%s", holeId, charClass.toString());
    BoolExpr expr = state.ctx.mkBoolConst(name);

    state.vars.add(expr);
    state.varToHoleId.put(expr, holeId);
    state.varToCharClass.put(expr, charClass);

    return expr;
  }
}
