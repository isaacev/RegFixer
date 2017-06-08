package edu.wisc.regfixer.synthesize;

import com.microsoft.z3.*;
import edu.wisc.regfixer.automata.Route;
import edu.wisc.regfixer.enumerate.HoleId;

import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import java.util.List;

/**
 * Created by sangyunpark on 6/8/17.
 */
public class SAT_Formula {

  private Map<HoleId, Map<String, Boolean>> table;   // table manages <Hole#, <Hole#_Character, T/F>
  private HashMap<String, String> cfg;
  private Context ctx;
  private Solver solver;
  private Optimize opt;
  private Map<HoleId, Map<String, Integer>> countTable;  // counting table for <Hole#, <CharClasses, Count>>
  private List<Set<Route>> postives;  // list of positive examples with set of runs (= Route)
  private List<Set<Route>> negatives;


  public SAT_Formula(List<Set<Route>> positives, List<Set<Route>> negatives) {
    this.cfg = new HashMap<String, String>();
    this.ctx = new Context(cfg);
    this.solver = ctx.mkSolver();     // actual solver for SAT
    this.table = new HashMap<>();
    this.opt = ctx.mkOptimize();    // optimizer for MAX-SAT
    this.countTable = new HashMap<>();
    this.postives = positives;
    this.negatives = negatives;
  }

  public void build() {
    for(Set<Route> runs: this.postives) {   // make for positives
      buildFormula(runs, true);
    }

    for(Set<Route> runs: this.negatives) {  // make for negatives
      buildFormula(runs, false);
    }

    this.encodePredicates();  // encode predicate constraints (e.g. \d \w ...)
  }

  private void buildFormula(Set<Route> runs, boolean posFlag) {
    BoolExpr exprFinal = null;

    // iterate each runs
    for(Route route : runs) {
      BoolExpr exprRun = null;

      // retrieve in a form of actual run
      Map<HoleId, Set<Character>> run = route.getSpans();

      // iterate each hole
      for(HoleId holeNum : run.keySet()) {
        // initialize countTable and table for each holeNum
        if(!countTable.containsKey(holeNum)) {
          initializeCountTable(countTable, holeNum);
        }
        if(!table.containsKey(holeNum)) {
          table.put(holeNum, new HashMap<>());
        }
        // for different chars in the same hole
        BoolExpr exprChars = null;

        // iterate over chars
        for(Character charVal : run.get(holeNum)) {
          // make each variable by  "HOLE#_CHARACTER", and each character costs -2
          String var = holeNum.toString() + '_' + charVal.toString();
          BoolExpr exprChar = ctx.mkBoolConst(var);
          opt.AssertSoft(exprChar, -2, "MAX_SAT");
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
          // check each hole's character class count
          countCharType(holeNum, charVal);
          if(!table.get(holeNum).containsKey(var)) {
            table.get(holeNum).put(var,false);
          }
        }   // each hole loop ends
        // 2. merging different holes
        if(exprRun == null) {
          exprRun = exprChars;
        }
        else {
          if (posFlag) {   // Positive example -- AND different holes
            exprRun = ctx.mkAnd(exprRun, exprChars);
          } else {        // Negative example -- NOT OR different holes
            exprRun = ctx.mkOr(exprRun, exprChars);
          }
        }
      } // each hole For ends
      if(exprRun == null) // case when no character is going through a hole --> return
        return;

      // 3. merging different runs
      if(exprFinal == null) {
        exprFinal = exprRun;
      } else {
        if(posFlag) { exprFinal = ctx.mkOr(exprFinal, exprRun); }
        else { exprFinal = ctx.mkAnd(exprFinal, exprRun); }
      }
    } // runs for loop ends
    opt.Add(exprFinal);
  }

  private void encodePredicates() {

    // iterating each hole
    for(HoleId holeNum : countTable.keySet()) {

      // iterating each predicate (e.g. \w, \d, \s...)
      for(String pred : countTable.get(holeNum).keySet()) {

        BoolExpr exprPreds = null;
        // encode hard-constraint for each predicate if their count is more than 2
        if(countTable.get(holeNum).get(pred) > 2) {

          String predVar = holeNum.toString() + '_' + pred;
          BoolExpr exprPred = ctx.mkBoolConst(predVar);
          table.get(holeNum).put(predVar,false);

          // soft-constraint: each predicate gets a value
          if (pred.equals("\\w") || pred.equals("\\W")) {
            opt.AssertSoft(exprPred,5,"MAX_SAT");
          } else if(pred.equals("\\d") || pred.equals("\\D")) {
            opt.AssertSoft(exprPred,4,"MAX_SAT");
          } else if(pred.equals("\\s") || pred.equals("\\S")) {
            opt.AssertSoft(exprPred,3,"MAX_SAT");
          }

          // iterating character exists with corresponding holeNum
          for(String var : table.get(holeNum).keySet()) {
            String check = var.substring(var.indexOf('_') + 1);
            BoolExpr exprChar = null;
            boolean encode = true;

            // check if it is one of the char classes
            if(check.equals("\\w") || check.equals("\\W") || check.equals("\\d") || check.equals("\\D")
                || check.equals("\\s") || check.equals("\\S")) {
              encode = false;
            } else if ( // check if a character is correct type of character class
                (pred.equals("\\w") && !Character.isLetter(check.charAt(check.length()-1))) ||
                    (pred.equals("\\W") && Character.isLetter(check.charAt(check.length()-1))) ||
                    (pred.equals("\\d") && !Character.isDigit(check.charAt(check.length()-1))) ||
                    (pred.equals("\\D") && Character.isDigit(check.charAt(check.length()-1))) ||
                    (pred.equals("\\s") && !Character.isWhitespace(check.charAt(check.length()-1))) ||
                    (pred.equals("\\S") && Character.isWhitespace(check.charAt(check.length()-1)))
                ) {
              encode = false;
            }
            // if satisfied all conditions, encode as a hard-constraint
            if(encode) {
              exprChar = ctx.mkBoolConst(var);
              if (exprPreds == null) {
                exprPreds = ctx.mkOr(ctx.mkNot(exprPred), exprChar);
              } else {
                exprPreds = ctx.mkAnd(exprPreds, ctx.mkOr(ctx.mkNot(exprPred), exprChar));
              }
            }
          }
//                    System.out.println("Encoding Predicate Constraint");
//                    System.out.println(exprPreds.toString());
          if(exprPreds != null) {
            opt.Add(exprPreds);
          }
        }
      }
    }   // outer for ends
  }

  private void initializeCountTable (Map<HoleId, Map<String, Integer>> countTable, HoleId holeNum) {
    //TODO: \W \D \S are omitted for now

    Map<String, Integer> actualCount = new HashMap<>();
    actualCount.put("\\w", 0);
//        actualCount.put("\\W", 0);
    actualCount.put("\\d", 0);
//        actualCount.put("\\D", 0);
    actualCount.put("\\s", 0);
//        actualCount.put("\\S", 0);
    countTable.put(holeNum, actualCount);
  }

  private void countCharType(HoleId holeNum, Character charVal) {
    //TODO: \W \D \S are omitted for now

    String key = holeNum.toString() + "_" + charVal.toString();

    // check whether a character is word, not word, digit, not digit, whitespace or not whitespace
    if(Character.isLetter(charVal)) {   // WORD
      if(!table.get(holeNum).containsKey(key)) {   // increment count if not already in the table
        countTable.get(holeNum).put("\\w", countTable.get(holeNum).get("\\w") + 1);
      }
//        } else {    // NOT WORD
//            if(!table.get(holeNum).containsKey(key)) {
//                countTable.get(holeNum).put("\\W", countTable.get(holeNum).get("\\W") + 1);
//            }
    }
    if(Character.isDigit(charVal)) {    // Digit
      if(!table.get(holeNum).containsKey(key)) {
        countTable.get(holeNum).put("\\d", countTable.get(holeNum).get("\\d") + 1);
      }
//        } else {    // Not Digit
//            if(!table.get(holeNum).containsKey(key)) {
//                countTable.get(holeNum).put("\\D", countTable.get(holeNum).get("\\D") + 1);
//            }
    }
    if(Character.isWhitespace(charVal)) {   // whitespace
      if(!table.get(holeNum).containsKey(key)) {
        countTable.get(holeNum).put("\\s", countTable.get(holeNum).get("\\s") + 1);
      }
//        } else {    // Not whitespace
//            if(!table.get(holeNum).containsKey(key)) {
//                countTable.get(holeNum).put("\\S", countTable.get(holeNum).get("\\S") + 1);
//            }
    }
  }

  public Optimize getOpt() {
    return opt;
  }

  public Map<HoleId, Map<String, Boolean>> getTable() {
    return table;
  }

  public Context getCtx() { return ctx; }

  public boolean isUnsatisfiable() {
    return (this.opt.Check() == Status.UNSATISFIABLE);
  }

  /*
  It tests whether a given predicate (e.g. \w) is a solution for the corresponding hole
   */
  public boolean predicateIsSolutionInHole(HoleId holeNum, String predicate) {
    if(table.get(holeNum).containsKey(predicate)) {
      if(table.get(holeNum).get(predicate)) {   // its result is true
        return true;
      }
    }
    return false;
  }
}
