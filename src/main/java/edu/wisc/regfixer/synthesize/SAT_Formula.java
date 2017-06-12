package edu.wisc.regfixer.synthesize;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.List;

import com.microsoft.z3.BoolExpr;
import com.microsoft.z3.Context;
import com.microsoft.z3.Model;
import com.microsoft.z3.Optimize;
import com.microsoft.z3.Status;
import edu.wisc.regfixer.automata.Route;
import edu.wisc.regfixer.enumerate.HoleId;
import edu.wisc.regfixer.parser.CharClass;
import edu.wisc.regfixer.parser.CharEscapedNode;
import edu.wisc.regfixer.parser.CharLiteralNode;

/**
 * Created by sangyunpark on 6/8/17.
 */
public class SAT_Formula {
  private Context ctx;
  private Optimize opt;
  private Model model;
  private Set<BoolExpr> knownVars;
  private Map<BoolExpr, HoleId> varToHoleId;
  private Map<HoleId, Set<BoolExpr>> holeIdToVars;
  private Map<BoolExpr, CharClass> varToCharClass;
  private Map<HoleId, Map<CharEscapedNode, Integer>> charCountTable;
  private Map<HoleId, Map<Character, Boolean>> knownHoleCharCrosses;
  private List<Set<Route>> positives;
  private List<Set<Route>> negatives;

  public SAT_Formula (List<Set<Route>> positives, List<Set<Route>> negatives) {
    this.positives = positives;
    this.negatives = negatives;
    this.ctx = new Context();
    this.opt = this.ctx.mkOptimize();
    this.model = null;
    this.knownVars = new HashSet<>();
    this.varToHoleId = new HashMap<>();
    this.holeIdToVars = new HashMap<>();
    this.varToCharClass = new HashMap<>();
    this.charCountTable = new HashMap<>();
    this.knownHoleCharCrosses = new HashMap<>();

    this.buildFormula();
  }

  private void buildFormula () {
    for (Set<Route> routes : this.positives) {
      BoolExpr expr = this.buildExampleFormula(routes, true);

      if (expr != null) {
        this.opt.Add(expr);
      }
    }

    for (Set<Route> routes : this.negatives) {
      BoolExpr expr = this.buildExampleFormula(routes, false);

      if (expr != null) {
        this.opt.Add(expr);
      }
    }

    this.encodePredicates();
  }

  private BoolExpr buildExampleFormula (Set<Route> routes, boolean posFlag) {
    BoolExpr exprFinal = null;

    for (Route route : routes) {
      BoolExpr exprRoute = this.buildRouteFormula(route, posFlag);

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

  private BoolExpr buildRouteFormula (Route route, boolean posFlag) {
    Map<HoleId, Set<Character>> spans = route.getSpans();
    BoolExpr exprRoute = null;

    for (HoleId holeId : spans.keySet()) {
      if (this.charCountTable.containsKey(holeId) == false) {
        this.initializeCountTable(holeId);
      }

      BoolExpr exprChars = this.buildCharFormula(holeId, spans.get(holeId), posFlag);

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

  private BoolExpr buildCharFormula (HoleId holeId, Set<Character> chars, boolean posFlag) {
    BoolExpr exprChars = null;

    for (Character charVal : chars) {
      BoolExpr exprChar = this.registerVariable(holeId, new CharLiteralNode(charVal));
      this.opt.AssertSoft(exprChar, -2, "MAX_SAT");
      this.countCharType(holeId, charVal);

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

  private void encodePredicates () {
    for (HoleId holeId : this.charCountTable.keySet()) {
      for (CharEscapedNode predClass : this.charCountTable.get(holeId).keySet()) {
        BoolExpr exprPreds = null;
        Integer count = this.charCountTable.get(holeId).get(predClass);

        if (count > 2) {
          BoolExpr exprPred = this.registerVariable(holeId, predClass);
          Integer metaClassWeight = SAT_Formula.computeMetaClassWeight(predClass);
          this.opt.AssertSoft(exprPred, metaClassWeight, "MAX_SAT");

          for (BoolExpr var : this.holeIdToVars.get(holeId)) {
            CharClass varClass = this.varToCharClass.get(var);
            boolean isLiteralClass = varClass instanceof CharLiteralNode;

            if (isLiteralClass) {
              boolean childOfPredClass = SAT_Formula.charIsMemberOfMetaClass((CharLiteralNode) varClass, predClass);

              if (childOfPredClass) {
                if (exprPreds == null) {
                  exprPreds = this.ctx.mkOr(this.ctx.mkNot(exprPred), var);
                } else {
                  exprPreds = this.ctx.mkAnd(exprPreds, this.ctx.mkOr(this.ctx.mkNot(exprPred), var));
                }
              }
            }
          }

          if (exprPreds != null) {
            this.opt.Add(exprPreds);
          }
        }
      }
    }
  }

  private void initializeCountTable (HoleId holeId) {
    this.charCountTable.put(holeId, new HashMap<>());
    this.charCountTable.get(holeId).put(new CharEscapedNode('w'), 0);
    this.charCountTable.get(holeId).put(new CharEscapedNode('d'), 0);
    this.charCountTable.get(holeId).put(new CharEscapedNode('s'), 0);
    this.charCountTable.get(holeId).put(new CharEscapedNode('a'), 0);
  }

  private void countCharType (HoleId holeId, Character charVal) {
    Map<CharEscapedNode, Integer> count = this.charCountTable.get(holeId);

    if (this.knownHoleCharCrosses.get(holeId) == null) {
      this.knownHoleCharCrosses.put(holeId, new HashMap<>());
    }

    if (this.knownHoleCharCrosses.get(holeId).get(charVal) == null) {
      this.knownHoleCharCrosses.get(holeId).put(charVal, true);
    } else {
      return;
    }

    if (Character.isLetterOrDigit(charVal)) {
      CharEscapedNode metaClass = new CharEscapedNode('w');
      int old = count.get(metaClass);
      count.put(metaClass, old + 1);
    }

    if (Character.isLetter(charVal)) {
      CharEscapedNode metaClass = new CharEscapedNode('a');
      int old = count.get(metaClass);
      count.put(metaClass, old + 1);
    }

    if (Character.isDigit(charVal)) {
      CharEscapedNode metaClass = new CharEscapedNode('d');
      int old = count.get(metaClass);
      count.put(metaClass, old + 1);
    }

    if (Character.isWhitespace(charVal)) {
      CharEscapedNode metaClass = new CharEscapedNode('s');
      int old = count.get(metaClass);
      count.put(metaClass, old + 1);
    }
  }

  public static Integer computeMetaClassWeight (CharClass charClass) {
    switch (charClass.toString()) {
      case "\\w":
      case "\\W":
        return 5;
      case "\\a":
        return 4;
      case "\\d":
      case "\\D":
        return 3;
      case "\\s":
      case "\\S":
        return 2;
      default:
        return 0;
    }
  }

  public static boolean charClassIsMetaClass (CharClass charClass) {
    switch (charClass.toString()) {
      case "\\w":
      case "\\W":
      case "\\a":
      case "\\d":
      case "\\D":
      case "\\s":
      case "\\S":
        return true;
      default:
        return false;
    }
  }

  public Optimize getOpt() {
    return opt;
  }

  public Map<HoleId, Map<String, Boolean>> getTable() {
    return null;
  }

  public static boolean charIsMemberOfMetaClass (CharLiteralNode literal, CharEscapedNode meta) {
    switch (meta.toString()) {
      case "\\w":
        return Character.isLetterOrDigit(literal.getChar());
      case "\\W":
        return Character.isLetterOrDigit(literal.getChar()) == false;
      case "\\a":
        return Character.isLetter(literal.getChar());
      case "\\d":
        return Character.isDigit(literal.getChar());
      case "\\D":
        return Character.isDigit(literal.getChar()) == false;
      case "\\s":
        return Character.isWhitespace(literal.getChar());
      case "\\S":
        return Character.isWhitespace(literal.getChar()) == false;
      default:
        return false;
    }
  }

  public Context getCtx() { return ctx; }

  public boolean isUnsatisfiable() {
    return (this.opt.Check() == Status.UNSATISFIABLE);
  }

  /*
  It tests whether a given predicate (e.g. \w) is a solution for the corresponding hole
   */
  public boolean predicateIsSolutionInHole(HoleId holeId, String predicate) {
    return false;
  }

  private BoolExpr registerVariable (HoleId holeId, CharClass charClass) {
    String name = SAT_Formula.makeVarName(holeId, charClass.toString());
    BoolExpr expr = this.ctx.mkBoolConst(name);

    if (this.holeIdToVars.containsKey(holeId) == false) {
      this.holeIdToVars.put(holeId, new HashSet<>());
    }

    this.knownVars.add(expr);
    this.varToHoleId.put(expr, holeId);
    this.holeIdToVars.get(holeId).add(expr);
    this.varToCharClass.put(expr, charClass);

    return expr;
  }

  private static String makeVarName (HoleId holeId, String predicate) {
    return String.format("%s_%s", holeId.toString(), predicate);
  }

  public boolean isUnSatisfiable () {
    return (this.opt.Check() == Status.UNSATISFIABLE);
  }

  public Set<BoolExpr> getVariables () {
    return this.knownVars;
  }

  public HoleId getHoleIdForVariable (BoolExpr var) {
    return this.varToHoleId.get(var);
  }

  public CharClass getCharClassForVariable (BoolExpr var) {
    return this.varToCharClass.get(var);
  }

  public boolean variableEvaluatesTrue (BoolExpr var) {
    if (this.model == null) {
      this.model = this.opt.getModel();
    }

    return this.model.evaluate(var, false).isTrue();
  }

  @Override
  public String toString () {
    return this.opt.toString();
  }
}
