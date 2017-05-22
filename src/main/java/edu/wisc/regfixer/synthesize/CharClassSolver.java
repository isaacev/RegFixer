package edu.wisc.regfixer.synthesize;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import com.microsoft.z3.BoolExpr;
import com.microsoft.z3.Context;
import com.microsoft.z3.Model;
import com.microsoft.z3.Solver;
import com.microsoft.z3.Status;
import edu.wisc.regfixer.automata.Route;

/**
 * BUGS:
 * - will crash if a given Route is empty
 */

public class CharClassSolver {
  public static Map<Integer, String> solve (List<Set<Route>> positives, List<Set<Route>> negatives) throws SynthesisFailure {
    Map<String, String> cfg = new HashMap<>();
    Context ctx = new Context(cfg);
    Solver solver = ctx.mkSolver();
    Map<BoolExpr, Boolean> table = new HashMap<>();

    for (Set<Route> positive : positives) {
      solver.add(buildFormula(ctx, table, true, positive));
    }

    for (Set<Route> negative : negatives) {
      solver.add(buildFormula(ctx, table, false, negative));
    }

    return solveFormula(solver, table);
  }

  private static Map<Integer, String> solveFormula (Solver solver, Map<BoolExpr, Boolean> table) throws SynthesisFailure {
    if (solver.check() == Status.UNSATISFIABLE) {
      throw new SynthesisFailure("cannot derrive character classes");
    } else {
      // return synthesizeClasses(solver.getModel(), table);
      return null;
    }
  }

  private static BoolExpr buildFormula (Context ctx, Map<BoolExpr, Boolean> table, boolean isPositive, Set<Route> routes) {
    return routes.stream()
      .map(route -> buildRunExpr(ctx, table, isPositive, route))
      .reduce(null, (accum, expr) -> {
        if (accum == null) {
          return expr;
        } else if (isPositive) {
          return ctx.mkOr(accum, expr);
        } else {
          return ctx.mkAnd(accum, expr);
        }
      });
  }

  private static BoolExpr buildRunExpr (Context ctx, Map<BoolExpr, Boolean> table, boolean isPositive, Route route) {
    return route.getSpans().entrySet().stream()
      .map(entry -> buildHoleExpr(ctx, table, isPositive, entry))
      .reduce(null, (accum, expr) -> {
        if (accum == null) {
          return expr;
        } else if (isPositive) {
          return ctx.mkAnd(accum, expr);
        } else {
          return ctx.mkOr(accum, expr);
        }
      });
  }

  private static BoolExpr buildHoleExpr (Context ctx, Map<BoolExpr, Boolean> table, boolean isPositive, Map.Entry<Integer, Set<Character>> hole) {
    return hole.getValue().stream()
      .map(ch -> ctx.mkBoolConst(String.format("H%d_C%c", hole.getKey(), ch)))
      .reduce(null, (accum, expr) -> {
        if (false == table.containsKey(expr)) {
          table.put(expr, false);
        }

        if (isPositive && accum == null) {
          return expr;
        } else if (isPositive) {
          return ctx.mkAnd(accum, expr);
        } else if (accum == null) {
          return ctx.mkNot(expr);
        } else {
          return ctx.mkOr(accum, ctx.mkNot(expr));
        }
      });
  }
}
