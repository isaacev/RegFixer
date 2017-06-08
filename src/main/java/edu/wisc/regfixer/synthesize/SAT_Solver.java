package edu.wisc.regfixer.synthesize;

import com.microsoft.z3.BoolExpr;
import com.microsoft.z3.Context;
import com.microsoft.z3.Model;
import com.microsoft.z3.Optimize;
import edu.wisc.regfixer.enumerate.HoleId;
import edu.wisc.regfixer.parser.*;

import java.util.*;

/**
 * Created by sangyunpark on 6/8/17.
 */
public class SAT_Solver {

  public static Map<HoleId, CharClass> solve(SAT_Formula sat_formula) throws SynthesisFailure {


    if(sat_formula.isUnsatisfiable()) {   // throws exception if the formula is unsatisfiable
      throw new SynthesisFailure("unsatisfiable SAT formula");
    }


    // Display for testing -- which characters are possible solutions?
    Model model = sat_formula.getOpt().getModel();
    Map<HoleId, Map<String, Boolean>> table = sat_formula.getTable();
    Context ctx = sat_formula.getCtx();

    // if a character is true
    for(HoleId holeNum: table.keySet()) {
      for (String key : table.get(holeNum).keySet()) {
        BoolExpr boolExpr = ctx.mkBoolConst(key);
        if (model.evaluate(boolExpr, false).isTrue()) {
          table.get(holeNum).replace(key, true);
        }
      }
    }
    // Start making holeSolutions here
    Map<HoleId, CharClass> holeSolutions = new HashMap<>();
    optimizeCharClasses(sat_formula, holeSolutions, table);
/*

    throw new SynthesisFailure(String.format(fmt, this.tree));

    if (formula.isUnSatisfiable()) {
      throw new SynthesisFailure("unsatisfiable SAT formula");
    }
    */
    return holeSolutions;
  }

  /*
  It tries to optimize the char class that fits into the corresponding hole
  The order it tries to maximize is by
    1. meta char classes -- CharEscapedNode
      - \w, \d, \s
      - [a-z] [A-Z]
    2. range -- CharRangeNode
    3. character set (e.g. [abc]) -- CharClassSetNode
   */
  public static void optimizeCharClasses(SAT_Formula sat_formula, Map<HoleId, CharClass> holeSolutions, Map<HoleId, Map<String, Boolean>> table) {

    // iterate each hole in 'table' and check by the order
    for(HoleId holeNum : table.keySet()) {
      String pred_w = holeNum.toString() + "_\\w";
      String pred_W = holeNum.toString() + "_\\W";
      String pred_d = holeNum.toString() + "_\\d";
      String pred_D = holeNum.toString() + "_\\D";
      String pred_s = holeNum.toString() + "_\\s";
      String pred_S = holeNum.toString() + "_\\S";

      // TODO: \W, \D, \S are not yet included.  Need to discuss about it

      // 1. test meta char classes -- CharEscapedNode
      if(sat_formula.predicateIsSolutionInHole(holeNum, pred_w)) {
        holeSolutions.put(holeNum, new CharEscapedNode('w'));
        continue;
      } else if(sat_formula.predicateIsSolutionInHole(holeNum, pred_d)) {
        holeSolutions.put(holeNum, new CharEscapedNode('d'));
        continue;
      } else if(sat_formula.predicateIsSolutionInHole(holeNum, pred_s)) {
        holeSolutions.put(holeNum, new CharEscapedNode('s'));
        continue;
      }

      // TODO: need to include validation for [a-zA-Z] during the SAT_formula.build()
      // 2. test range -- CharRangeNode

      // 3. test character set -- CharClassSetNode
      Collection<CharRangeNode> collection = new ArrayList<>();
      Map<String, Boolean> tmpTable = table.get(holeNum);
      for(String charStr: tmpTable.keySet()) {
        if(table.get(holeNum).get(charStr)) {
          int delimiterPos = charStr.indexOf('_');
          char val = charStr.charAt(delimiterPos + 1);
          collection.add(new CharRangeNode(new CharLiteralNode(val)));
        }
      }
      CharClassSetNode charClassSetNode = new CharClassSetNode(false, collection);
      holeSolutions.put(holeNum, charClassSetNode);
    }

  }

  public static void printResult(SAT_Formula sat_formula) {
    // Display for testing -- which characters are possible solutions?
    Map<HoleId, Map<String, Boolean>> table = sat_formula.getTable();
    Optimize opt = sat_formula.getOpt();

    System.out.println("SAT formula to solve is...\n" + opt.toString());
    if(sat_formula.isUnsatisfiable()) {
      System.out.println("\nUnsatisfiable SAT formula");
      return;
    }

    System.out.println("\nSatisfiable SAT formula");
    for(HoleId holeNum: table.keySet()) {
      for (String key : table.get(holeNum).keySet()) {
        System.out.println(key + " = " + table.get(holeNum).get(key));
      }
    }
  }

}
