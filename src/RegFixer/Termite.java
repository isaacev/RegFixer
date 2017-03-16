package RegFixer;

import java.util.List;
import java.util.LinkedList;
import RegexParser.RegexNode;
import RegexParser.ConcatNode;
import RegexParser.UnionNode;
import RegexParser.OptionalNode;
import RegexParser.StarNode;
import RegexParser.PlusNode;
import RegexParser.CharNode;
import RegexParser.HoleNode;

/**
 * TODO:
 * - AST nodes to support:
 *   - CharacterClassNode
 *   - DotNode (probably atomic)
 *   - EscapedCharNode
 *   - IntervalNode
 *   - MetaCharNode
 *   - NormalCharNode (probably atomic)
 *   - RepetitionNode (how to handle limits?)
 *
 * IDEAS:
 * - Since many AST nodes have structural similarities (2 child nodes, 1 child
 *   node, etc.) it may be more efficient to assign like nodes common abstract
 *   parent classes in the AST library. Then the code in this class may be
 *   made more generic and reusable between similarily structured nodes.
 */

public class Termite {
  static List<RegexNode> digest (RegexNode expr) {
         if (expr instanceof ConcatNode)   { return digestConcat((ConcatNode) expr); }
    else if (expr instanceof UnionNode)    { return digestUnion((UnionNode) expr); }
    else if (expr instanceof OptionalNode) { return digestOptional((OptionalNode) expr); }
    else if (expr instanceof StarNode)     { return digestStar((StarNode) expr); }
    else if (expr instanceof PlusNode)     { return digestPlus((PlusNode) expr); }
    else if (expr instanceof CharNode)     { return digestAtom(); }
    else {
      System.err.printf("Unknown AST class: %s\n", expr.getClass().getName());
      System.exit(1);
      return null;
    }
  }

  static List<RegexNode> digestConcat (ConcatNode expr) {
    List<RegexNode> digestedExprs = new LinkedList<RegexNode>();
    List<RegexNode> subExprs = expr.getList();

    for (int n = 1; n <= subExprs.size(); n++) {
      for (int i = 0; i <= subExprs.size() - n; i++) {
        List<RegexNode> prefixExprs = new LinkedList<RegexNode>();
        List<RegexNode> midfixExprs = new LinkedList<RegexNode>();
        List<RegexNode> suffixExprs = new LinkedList<RegexNode>();

        // Collect exprs from 0 to i (exclusive).
        prefixExprs = new LinkedList<RegexNode>(subExprs.subList(0, i));

        // Collect exprs from i to i+n (exclusive).
        midfixExprs = new LinkedList<RegexNode>(subExprs.subList(i, i + n));

        // Collect exprs from i+n to end of list.
        suffixExprs = new LinkedList<RegexNode>(subExprs.subList(i + n, subExprs.size()));

        if (midfixExprs.size() == 1) {
          for (RegexNode subExprDigested : digest(midfixExprs.get(0))) {
            List<RegexNode> tmp = new LinkedList<RegexNode>();
            tmp.addAll(prefixExprs);
            tmp.add(subExprDigested);
            tmp.addAll(suffixExprs);

            digestedExprs.add(new ConcatNode(tmp));
          }
        } else {
          List<RegexNode> tmp = new LinkedList<RegexNode>();
          tmp.addAll(prefixExprs);
          tmp.add(new HoleNode());
          tmp.addAll(suffixExprs);

          digestedExprs.add(new ConcatNode(tmp));
        }
      }
    }

    return digestedExprs;
  }

  static List<RegexNode> digestUnion (UnionNode expr) {
    List<RegexNode> digestedExprs = new LinkedList<RegexNode>();
    
    // Recursively compute holes of left sub-expression(s).
    for (RegexNode digestedExpr : digest(expr.getMyRegex1())) {
      digestedExprs.add(new UnionNode(digestedExpr, expr.getMyRegex2()));
    }

    // Recursively compute holes of right sub-expression(s).
    for (RegexNode digestedExpr : digest(expr.getMyRegex2())) {
      digestedExprs.add(new UnionNode(expr.getMyRegex1(), digestedExpr));
    }

    // Replace entire expression with a hole.
    digestedExprs.add(new HoleNode());
    return digestedExprs;
  }

  static List<RegexNode> digestOptional (OptionalNode expr) {
    List<RegexNode> digestedExprs = new LinkedList<RegexNode>();

    // Recursively compute holes of sub-expression(s).
    for (RegexNode digestedExpr : digest(expr.getMyRegex1())) {
      digestedExprs.add(new OptionalNode(digestedExpr));
    }

    // Replace entire expression with a hole.
    digestedExprs.add(new HoleNode());
    return digestedExprs;
  }

  static List<RegexNode> digestStar (StarNode expr) {
    List<RegexNode> digestedExprs = new LinkedList<RegexNode>();

    // Recursively compute holes of sub-expression(s).
    for (RegexNode digestedExpr : digest(expr.getMyRegex1())) {
      digestedExprs.add(new StarNode(digestedExpr));
    }

    // Replace entire expression with a hole.
    digestedExprs.add(new HoleNode());
    return digestedExprs;
  }

  static List<RegexNode> digestPlus (PlusNode expr) {
    List<RegexNode> digestedExprs = new LinkedList<RegexNode>();

    // Recursively compute holes of sub-expression(s).
    for (RegexNode digestedExpr : digest(expr.getMyRegex1())) {
      digestedExprs.add(new PlusNode(digestedExpr));
    }

    // Replace entire expression with a hole.
    digestedExprs.add(new HoleNode());
    return digestedExprs;
  }

  static List<RegexNode> digestAtom () {
    LinkedList<RegexNode> digestedExprs = new LinkedList<RegexNode>();

    // Replace entire expression with a hole.
    digestedExprs.add(new HoleNode());
    return digestedExprs;
  }
}
