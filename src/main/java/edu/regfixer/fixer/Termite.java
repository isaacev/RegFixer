package edu.wisc.regfixer.fixer;

import java.util.*;
import java.util.stream.*;
import RegexParser.*;

/**
 * TODO:
 * - AST nodes to support:
 *   - IntervalNode
 *   - RepetitionNode (how to handle limits?)
 *
 * IDEAS:
 * - Since many AST nodes have structural similarities (2 child nodes, 1 child
 *   node, etc.) it may be more efficient to assign like nodes common abstract
 *   parent classes in the AST library. Then the code in this class may be
 *   made more generic and reusable between similarily structured nodes.
 */

/**
 * Termite is responsible for taking regular expression abstract syntax trees
 * and finding all the sub-expressions that can be replaced with a hole. The
 * `Termite.digest` method will return a list of ASTs where each AST has a
 * different sub-expression replaced with a hole.
 */
public class Termite {
  public static TermiteForest digest (RegexNode tree) {
    List<TermiteTree> trees = digestNode(tree).stream()
      .filter(elem -> (elem.tree != elem.hole))
      .map(elem -> new TermiteTree(elem.tree, elem.hole))
      .collect(Collectors.toList());

    return new TermiteForest(tree, trees);
  }

  private static List<TreeHolePair> digestNode (RegexNode expr) {
         if (expr instanceof ConcatNode)     { return digestConcat((ConcatNode) expr); }
    else if (expr instanceof UnionNode)      { return digestUnion((UnionNode) expr); }
    else if (expr instanceof RepetitionNode) { return digestRepetition((RepetitionNode) expr); }
    else if (expr instanceof OptionalNode)   { return digestOptional((OptionalNode) expr); }
    else if (expr instanceof StarNode)       { return digestStar((StarNode) expr); }
    else if (expr instanceof PlusNode)       { return digestPlus((PlusNode) expr); }
    else if (expr instanceof CharClass)      { return digestAtom(); }
    else {
      System.err.printf("Unknown AST class: %s\n", expr.getClass().getName());
      System.exit(1);
      return null;
    }
  }

  private static List<TreeHolePair> digestConcat (ConcatNode expr) {
    List<TreeHolePair> digestedExprs = new LinkedList<TreeHolePair>();
    List<RegexNode> children = expr.getChildren();
    int numChilds = children.size();

    for (int n = 1; n <= numChilds; n++) {
      for (int i = 0; i <= numChilds - n; i++) {
        List<RegexNode> prefix = new LinkedList<RegexNode>();
        List<RegexNode> midfix = new LinkedList<RegexNode>();
        List<RegexNode> suffix = new LinkedList<RegexNode>();

        // Collect exprs from 0 to i (exclusive).
        prefix = new LinkedList<RegexNode>(children.subList(0, i));

        // Collect exprs from i to i+n (exclusive).
        midfix = new LinkedList<RegexNode>(children.subList(i, i + n));

        // Collect exprs from i+n to end of list.
        suffix = new LinkedList<RegexNode>(children.subList(i + n, numChilds));

        List<TreeHolePair> digestedMidfixes = new LinkedList<TreeHolePair>();
        if (midfix.size() == 1) {
          digestedMidfixes.addAll(digestNode(midfix.get(0)));
        } else {
          HoleNode hole = new HoleNode();
          TreeHolePair pair = new TreeHolePair(hole, hole);
          digestedMidfixes.add(pair);
        }

        if (prefix.size() == 0 && suffix.size() == 0) {
          // If there are no prefix or suffix nodes, don't wrap the digested
          // node in a ConcatNode object which will only obfuscate the tree's
          // structure.
          digestedExprs.addAll(digestedMidfixes);
        } else {
          for (TreeHolePair digestedMidfix : digestedMidfixes) {
            List<RegexNode> newChildren = new LinkedList<RegexNode>();
            newChildren.addAll(prefix);
            newChildren.add(digestedMidfix.tree);
            newChildren.addAll(suffix);

            RegexNode tree = new ConcatNode(newChildren);
            HoleNode hole = digestedMidfix.hole;
            TreeHolePair pair = new TreeHolePair(tree, hole);
            digestedExprs.add(pair);
          }
        }
      }
    }

    return digestedExprs;
  }

  private static List<TreeHolePair> digestUnion (UnionNode expr) {
    List<TreeHolePair> digestedExprs = new LinkedList<TreeHolePair>();

    // Recursively compute holes of left sub-expression(s).
    for (TreeHolePair digestedExpr : digestNode(expr.getLeftChild())) {
      RegexNode tree = new UnionNode(digestedExpr.tree, expr.getRightChild());
      TreeHolePair pair = new TreeHolePair(tree, digestedExpr.hole);
      digestedExprs.add(pair);
    }

    // Recursively compute holes of right sub-expression(s).
    for (TreeHolePair digestedExpr : digestNode(expr.getRightChild())) {
      RegexNode tree = new UnionNode(expr.getLeftChild(), digestedExpr.tree);
      TreeHolePair pair = new TreeHolePair(tree, digestedExpr.hole);
      digestedExprs.add(pair);
    }

    // Replace entire expression with a hole.
    HoleNode hole = new HoleNode();
    TreeHolePair pair = new TreeHolePair(hole, hole);
    digestedExprs.add(pair);

    return digestedExprs;
  }

  private static List<TreeHolePair> digestRepetition (RepetitionNode expr) {
    List<TreeHolePair> digestedExprs = new LinkedList<TreeHolePair>();

    // Recursively compute holes of sub-expression(s).
    for (TreeHolePair digestedExpr : digestNode(expr.getChild())) {
      if (expr.hasMax()) {
        int min = expr.getMin();
        int max = expr.getMax();
        RegexNode tree = new RepetitionNode(digestedExpr.tree, min, max);
        TreeHolePair pair = new TreeHolePair(tree, digestedExpr.hole);
        digestedExprs.add(pair);
      } else {
        int min = expr.getMin();
        RegexNode tree = new RepetitionNode(digestedExpr.tree, min);
        TreeHolePair pair = new TreeHolePair(tree, digestedExpr.hole);
        digestedExprs.add(pair);
      }
    }

    // Replace entire expression with a hole.
    HoleNode hole = new HoleNode();
    TreeHolePair pair = new TreeHolePair(hole, hole);
    digestedExprs.add(pair);

    return digestedExprs;
  }

  private static List<TreeHolePair> digestOptional (OptionalNode expr) {
    List<TreeHolePair> digestedExprs = new LinkedList<TreeHolePair>();

    // Recursively compute holes of sub-expression(s).
    for (TreeHolePair digestedExpr : digestNode(expr.getChild())) {
      RegexNode tree = new OptionalNode(digestedExpr.tree);
      TreeHolePair pair = new TreeHolePair(tree, digestedExpr.hole);
      digestedExprs.add(pair);
    }

    // Replace entire expression with a hole.
    HoleNode hole = new HoleNode();
    TreeHolePair pair = new TreeHolePair(hole, hole);
    digestedExprs.add(pair);

    return digestedExprs;
  }

  private static List<TreeHolePair> digestStar (StarNode expr) {
    List<TreeHolePair> digestedExprs = new LinkedList<TreeHolePair>();

    // Recursively compute holes of sub-expression(s).
    for (TreeHolePair digestedExpr : digestNode(expr.getChild())) {
      RegexNode tree = new StarNode(digestedExpr.tree);
      TreeHolePair pair = new TreeHolePair(tree, digestedExpr.hole);
      digestedExprs.add(pair);
    }

    // Replace entire expression with a hole.
    HoleNode hole = new HoleNode();
    TreeHolePair pair = new TreeHolePair(hole, hole);
    digestedExprs.add(pair);

    return digestedExprs;
  }

  private static List<TreeHolePair> digestPlus (PlusNode expr) {
    List<TreeHolePair> digestedExprs = new LinkedList<TreeHolePair>();

    // Recursively compute holes of sub-expression(s).
    for (TreeHolePair digestedExpr : digestNode(expr.getChild())) {
      RegexNode tree = new PlusNode(digestedExpr.tree);
      TreeHolePair pair = new TreeHolePair(tree, digestedExpr.hole);
      digestedExprs.add(pair);
    }

    // Replace entire expression with a hole.
    HoleNode hole = new HoleNode();
    TreeHolePair pair = new TreeHolePair(hole, hole);
    digestedExprs.add(pair);

    return digestedExprs;
  }

  private static List<TreeHolePair> digestAtom () {
    LinkedList<TreeHolePair> digestedExprs = new LinkedList<TreeHolePair>();

    // Replace entire expression with a hole.
    HoleNode hole = new HoleNode();
    TreeHolePair pair = new TreeHolePair(hole, hole);
    digestedExprs.add(pair);

    return digestedExprs;
  }
}

class TreeHolePair {
  RegexNode tree;
  HoleNode hole;

  TreeHolePair (RegexNode tree, HoleNode hole) {
    this.tree = tree;
    this.hole = hole;
  }
}
