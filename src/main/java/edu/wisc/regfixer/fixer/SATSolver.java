package edu.wisc.regfixer.fixer;

import java.util.*;

import com.microsoft.z3.*;


/**
 * Created by sangyunpark on 4/30/17.
 */
public class SATSolver {
    // SAT Solver related variables
    public static HashMap<String, String> cfg = new HashMap<String, String>();
    public static Context ctx = new Context(cfg);
    public static Solver solver = ctx.mkSolver();     // actual solver for SAT
    public static Map<BoolExpr, Boolean> table = new HashMap<>();

    public static void makeFormula(List<Map<Integer, Set<Character>>> runs, boolean posFlag) {
        formulaForExample(runs, posFlag);
    }

    public static void formulaForExample(List<Map<Integer, Set<Character>>> runs, boolean posFlag) {
        // iterate runs
        BoolExpr exprFinal = null;
        for(Map<Integer, Set<Character>> run : runs) {
            BoolExpr exprRun = null;

            // iterate each holes
            for(Integer holeNum : run.keySet()) {
                BoolExpr exprChars = null;  // for different chars in the same hole

                // iterate over chars
                for(Character charVal : run.get(holeNum)) {
                    // make each variable by  "HOLE#_CHARACTER"
                    String var = holeNum.toString() + '_' + charVal.toString();
                    BoolExpr exprTmp = ctx.mkBoolConst(var);

                    // 1. merging different chars
                    if(posFlag) {
                        if(exprChars == null) { exprChars = exprTmp; }
                        else { exprChars = ctx.mkAnd(exprChars, exprTmp); }
                    } else {
                        if(exprChars == null) { exprChars = ctx.mkNot(exprTmp); }
                        else {    // Negative -- OR different chars in the
                            exprChars = ctx.mkOr(exprChars, ctx.mkNot(exprTmp));
                            int dummy = 0;
                        }
                    }
                    // check table if exists; add only if it does not exists
                    if(!table.containsKey(exprTmp)) { table.put(exprTmp, false); }
//                    System.out.println("exprChar");
//                    System.out.println(exprChars.toString());
                }

                // 2. merging different holes
                if(exprRun == null) { exprRun = exprChars; }
                else {
                    if (posFlag) {   // Positive example -- AND different holes{
                        exprRun = ctx.mkAnd(exprRun, exprChars);
                    } else {        // Negative example -- NOT OR different holes
                        exprRun = ctx.mkOr(exprRun, exprChars);
                    }
                }
            }
            System.out.println("exprRuns");
            System.out.println(exprRun.toString());
            // 3. merging different runs
            if(exprFinal == null) {
                exprFinal = exprRun;
            } else {
                if(posFlag) { exprFinal = ctx.mkOr(exprFinal, exprRun); }
                else { exprFinal = ctx.mkAnd(exprFinal, exprRun); }
            }
        }

        // positive ones can be added at the end as one formula
        solver.add(exprFinal);
        System.out.println("FINAL...");
        System.out.println(exprFinal.toString());
        System.out.println();

    }

    public static void solveFormula() {
        System.out.println("SAT formula to solve is...\n" + solver.toString());
        Status status = solver.check();
        if(status == Status.UNSATISFIABLE) {
            System.out.println("Unsatisfiable SAT formula");
        } else {
            System.out.println("Satisfiable SAT formula, possible char class");
            Model model = solver.getModel();
            for(BoolExpr key : table.keySet()) {
                System.out.println(key.toString() + " = " + model.evaluate(key, false));
            }
        }
    }
}
