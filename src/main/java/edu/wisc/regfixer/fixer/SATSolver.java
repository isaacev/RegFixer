package edu.wisc.regfixer.fixer;

import java.util.*;

import com.microsoft.z3.*;
import edu.wisc.regfixer.parser.RegexNode;


/**
 * Created by sangyunpark on 4/30/17.
 */
public class SATSolver {
    // SAT Solver related variables
    private HashMap<String, String> cfg;
    private Context ctx;
    private Solver solver;
    private Map<String, Boolean> table;
    private List<String> predicates;
    private Optimize opt;
    private RegexNode regex;

    SATSolver(RegexNode regex) {
        this.cfg = new HashMap<String, String>();
        this.ctx = new Context(cfg);
        this.solver = ctx.mkSolver();     // actual solver for SAT
        this.table = new HashMap<>();
        this.opt = ctx.mkOptimize();    // optimizer for MAX-SAT
        this.predicates = new ArrayList<>();
        this.regex = regex;
    }

    public void makeFormula(List<Map<Integer, Set<Character>>> runs, boolean posFlag) {
        formulaForExample(runs, posFlag);
    }

    public void formulaForExample(List<Map<Integer, Set<Character>>> runs, boolean posFlag) {
        // iterate runs
        BoolExpr exprFinal = null;
        BoolExpr predFinal = null;
        for(Map<Integer, Set<Character>> run : runs) {
            BoolExpr exprRun = null;
            BoolExpr predRun = null;

            // iterate each holes
            for(Integer holeNum : run.keySet()) {
                BoolExpr exprChars = null;  // for different chars in the same hole
                BoolExpr exprPreds = null;  // hard constraint for char classes like \w, \d, \s

                // iterate over chars
                for(Character charVal : run.get(holeNum)) {
                    // make each variable by  "HOLE#_CHARACTER"
                    String var = holeNum.toString() + '_' + charVal.toString();
                    String predW = holeNum.toString() + "_\\w";
                    // TODO: add other predicates like \d or \s

                    BoolExpr exprChar = ctx.mkBoolConst(var);
                    BoolExpr exprPred = ctx.mkBoolConst(predW);
                    if(!predicates.contains(predW)) {
                        opt.AssertSoft(exprPred, 1, "w");
                        predicates.add(predW);
                    }
                    // 1. merging different chars
                    if(posFlag) {
                        if(exprChars == null) { exprChars = exprChar; }
                        else { exprChars = ctx.mkAnd(exprChars, exprChar); }
                    } else {
                        if(exprChars == null) { exprChars = ctx.mkNot(exprChar); }
                        else {    // Negative -- OR different chars in the
                            exprChars = ctx.mkOr(exprChars, ctx.mkNot(exprChar));
                        }
                    }
                    // TODO: add predicate hard-constraint
                    if(exprPreds == null) { exprPreds = ctx.mkOr(ctx.mkNot(exprPred), exprChar); }
                    else {
                        exprPreds = ctx.mkAnd(exprPreds, ctx.mkOr(ctx.mkNot(exprPred), exprChar));
                    }
                    // check table if exists; add only if it does not exists
                    if(!table.containsKey(exprChar)) { table.put(var, false); }
                    if(!table.containsKey(exprPred)) { table.put(predW, false); }
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
                // adding predicate constraint
                if(predRun == null) { predRun = exprPreds; }
                else { predRun = ctx.mkAnd(predRun, exprPreds); }

            }   // each hole For Loop ends
            if(exprRun == null)
                return;

//            System.out.println("Expression for Run");
//            System.out.println(exprRun.toString());

            // 3. merging different runs
            if(exprFinal == null) {
                exprFinal = exprRun;
            } else {
                if(posFlag) { exprFinal = ctx.mkOr(exprFinal, exprRun); }
                else { exprFinal = ctx.mkAnd(exprFinal, exprRun); }
            }
            if(predFinal == null)  { predFinal = predRun; }
            else { predFinal = ctx.mkAnd(predFinal, predRun); }
        }   // runs For Loop ends

        // positive ones can be added at the end as one formula
        opt.Add(exprFinal);
        opt.Add(predFinal);
//        System.out.println("FINAL...");
//        System.out.println(opt.toString());
//        System.out.println();

        // TODO  return character class in form of Map<Interger, CharClass>
        // (0,x) (1,y), (2,z)
    }


    public RegexNode solveFormula() {
//        System.out.println("SAT formula to solve is...\n" + opt.toString());
        Status status = opt.Check();
        RegexNode newRegex = null;

        if(status == Status.UNSATISFIABLE) {
            System.out.println("\nUnsatisfiable SAT formula");
            return newRegex;
        } else {
            System.out.println("\nSatisfiable SAT formula, possible char class");
            Model model = opt.getModel();
            for(String key : table.keySet()) {
                BoolExpr boolExpr = ctx.mkBoolConst(key);
                if(model.evaluate(boolExpr, false).isTrue()) {
                    table.replace(key, true);
                }
//                System.out.println(key.toString() + " = " + model.evaluate(boolExpr, false));
                System.out.println(key + " = " + table.get(key));
            }

            // replace wholes here
            String newRegexStr = getNewRegex();
            try {
                newRegex = edu.wisc.regfixer.parser.Main.parse(newRegexStr);
            } catch(Exception e) {

            }


            return newRegex;
        }
    }

    public String getNewRegex() {
        String newRegex = regex.toString();
        String hole = "❑";

        System.out.println("\nStart changing: " + newRegex);
        int holeNum = 0;
        int startIndex = 0;
        int endIndex = newRegex.indexOf(hole);

        while(endIndex >= 0) {
            String predW = Integer.toString(holeNum) + '_' + "\\w";
            // TODO: add more predicates later

            // if predicate is evaluated to true, replace with it
            if(table.containsKey(predW) && table.get(predW)) {
                newRegex = newRegex.substring(startIndex,endIndex) + "\\w" + newRegex.substring(endIndex+1);
            } else {
                // iterate the table and pick the ones with true
                String replace = "";
                for(String key : table.keySet()) {
                    if(key.startsWith(Integer.toString(holeNum)) && key.charAt(1) == '_'
                        && table.get(key)) {
                        replace += key.substring(key.indexOf('_')+1);
                    }
                }
                newRegex = newRegex.substring(startIndex,endIndex) + replace + newRegex.substring(endIndex+1);
            }
            endIndex = newRegex.indexOf(hole);
            holeNum++;
        }
        System.out.println("Changed to: " + newRegex);
        return newRegex;
    }

}
