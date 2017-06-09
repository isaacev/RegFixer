package edu.wisc.regfixer.fixer;

import java.util.*;

import com.microsoft.z3.*;
import edu.wisc.regfixer.parser.RegexNode;
import edu.wisc.regfixer.enumerate.HoleId;


/**
 * Created by sangyunpark on 4/30/17.
 */
public class SATSolver {

    private static final String CHARCLASS_WORD = "\\w";
    private static final String CHARCLASS_NOT_WORD = "\\W";
    private static final String CHARCLASS_DIGIT= "\\d";
    private static final String CHARCLASS_NOT_DIGIT= "\\D";
    private static final String CHARCLASS_WHITESPACE= "\\s";
    private static final String CHARCLASS_NOT_WHITESPACE= "\\S";



    private HashMap<String, String> cfg;
    private Context ctx;
    private Solver solver;
    private Optimize opt;
    private RegexNode regex;
    private Map<HoleId, Map<String, Boolean>> table;   // table manages <Hole#, <Hole#_Character, T/F>
    private Map<HoleId, Map<String, Integer>> countTable;  // counting table for <Hole#, <CharClasses, Count>>

    public SATSolver(RegexNode regex) {
        this.cfg = new HashMap<String, String>();
        this.ctx = new Context(cfg);
        this.solver = ctx.mkSolver();     // actual solver for SAT
        this.table = new HashMap<>();
        this.opt = ctx.mkOptimize();    // optimizer for MAX-SAT
        this.regex = regex;
        this.countTable = new HashMap<>();
    }

    public void makeFormula(List<Map<HoleId, Set<Character>>> runs, boolean posFlag) {
        formulaForExample(runs, posFlag);
    }

    public void formulaForExample(List<Map<HoleId, Set<Character>>> runs, boolean posFlag) {
        // iterate runs
        BoolExpr exprFinal = null;

        for(Map<HoleId, Set<Character>> run : runs) {
            BoolExpr exprRun = null;

            // iterate each holes
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
            }   // each hole For Loop ends
            if(exprRun == null) // case when no character is going through a hole --> return
                return;

            // 3. merging different runs
            if(exprFinal == null) {
                exprFinal = exprRun;
            } else {
                if(posFlag) { exprFinal = ctx.mkOr(exprFinal, exprRun); }
                else { exprFinal = ctx.mkAnd(exprFinal, exprRun); }
            }
        }   // runs For Loop ends
        opt.Add(exprFinal);
    }

    public void encodePredConstraint() {

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

    private boolean checkInsertPred(Integer holeNum, String predDesc) {
        // returns true if a count for predicate with corresponding hole number is > 2
        // e.g. count for word in holeNumber 1 is greater 2 == "there are more than 2 word count in hole 1"
        return countTable.containsKey(holeNum) && countTable.get(holeNum).get(predDesc) > 2;
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

    public RegexNode solveFormula() {
        System.out.println("SAT formula to solve is...\n" + opt.toString());
        Status status = opt.Check();
        RegexNode newRegex = null;

        if(status == Status.UNSATISFIABLE) {
            System.out.println("\nUnsatisfiable SAT formula");
            return newRegex;
        } else {
            System.out.println("\nSatisfiable SAT formula, possible char class");
            Model model = opt.getModel();

            for(HoleId holeNum : table.keySet()) {
                for (String key : table.get(holeNum).keySet()) {
                    BoolExpr boolExpr = ctx.mkBoolConst(key);
                    if (model.evaluate(boolExpr, false).isTrue()) {
                        table.get(holeNum).replace(key, true);
                    }
//                System.out.println(key.toString() + " = " + model.evaluate(boolExpr, false));
                    System.out.println(key + " = " + table.get(holeNum).get(key));
                }
            }
            // retrieves a new regex with SAT formula result stored in 'table'
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
        HoleId holeId = new HoleId();
        System.out.println(holeId.hashCode());
        int startIndex = 0;
        int endIndex = newRegex.indexOf(hole);

        while(endIndex >= 0) {
            String pred_w = Integer.toString(holeNum) + '_' + "\\w";
            String pred_W = Integer.toString(holeNum) + '_' + "\\W";
            String pred_d = Integer.toString(holeNum) + '_' + "\\d";
            String pred_D = Integer.toString(holeNum) + '_' + "\\D";
            String pred_s = Integer.toString(holeNum) + '_' + "\\s";
            String pred_S = Integer.toString(holeNum) + '_' + "\\S";

            // if predicate in each step is evaluated to true, replace with it
            if(table.get(holeNum) != null) {
                if (table.get(holeNum).containsKey(pred_w) && table.get(holeNum).get(pred_w)) {
                    newRegex = newRegex.substring(startIndex, endIndex) + "\\w" + newRegex.substring(endIndex + 1);
//                } else if (table.get(holeNum).containsKey(pred_W) && table.get(holeNum).get(pred_W)) {
//                    newRegex = newRegex.substring(startIndex, endIndex) + "\\W" + newRegex.substring(endIndex + 1);
                } else if (table.get(holeNum).containsKey(pred_d) && table.get(holeNum).get(pred_d)) {
                    newRegex = newRegex.substring(startIndex, endIndex) + "\\d" + newRegex.substring(endIndex + 1);
//                } else if (table.get(holeNum).containsKey(pred_D) && table.get(holeNum).get(pred_D)) {
//                    newRegex = newRegex.substring(startIndex, endIndex) + "\\D" + newRegex.substring(endIndex + 1);
                } else if (table.get(holeNum).containsKey(pred_s) && table.get(holeNum).get(pred_s)) {
                    newRegex = newRegex.substring(startIndex, endIndex) + "\\s" + newRegex.substring(endIndex + 1);
//                } else if (table.get(holeNum).containsKey(pred_S) && table.get(holeNum).get(pred_S)) {
//                    newRegex = newRegex.substring(startIndex, endIndex) + "\\S" + newRegex.substring(endIndex + 1);
                }
                // no predicates are found to be true, so make a character set by concatenating true characters
                else {
                    String replace = "[";
                    // iterate the table and pick the ones with true
                    Map<String, Boolean> tmpTable = table.get(holeNum);
                    for (String key : tmpTable.keySet()) {
                        int delimiterPos = key.indexOf('_');
                        int tmpHoleNum = Integer.parseInt(key.substring(0, delimiterPos));

                        if ((holeNum == tmpHoleNum) && table.get(holeNum).get(key)) {
                            replace += key.substring(key.indexOf('_') + 1);
                        }
                    }
                    replace += "]";
                    newRegex = newRegex.substring(startIndex, endIndex) + replace + newRegex.substring(endIndex + 1);
                }
            }
            endIndex = newRegex.indexOf(hole);
            holeNum++;
        }
        System.out.println("Changed to: " + newRegex);
        return newRegex;
    }

}
