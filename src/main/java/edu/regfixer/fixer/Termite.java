package edu.wisc.regfixer.fixer;

import java.util.LinkedList;
import java.util.List;
import java.util.stream.Collectors;
import edu.wisc.regfixer.parser.CharClass;
import edu.wisc.regfixer.parser.ConcatNode;
import edu.wisc.regfixer.parser.HoleNode;
import edu.wisc.regfixer.parser.OptionalNode;
import edu.wisc.regfixer.parser.PlusNode;
import edu.wisc.regfixer.parser.RegexNode;
import edu.wisc.regfixer.parser.RegexNode;
import edu.wisc.regfixer.parser.RepetitionNode;
import edu.wisc.regfixer.parser.StarNode;
import edu.wisc.regfixer.parser.UnionNode;

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
 *   made more generic and reusable between similarly structured nodes.
 */

/**
 * Termite is responsible for taking regular expression abstract syntax trees
 * and finding all the sub-expressions that can be replaced with a hole. The
 * `Termite.digest` method will return a list of ASTs where each AST has a
 * different sub-expression replaced with a hole.
 */
public class Termite {
  public static List<IncompleteTree> digest (RegexNode n) {
    return digestNode(n);
  }

  private static List<IncompleteTree> digestNode (RegexNode n) {
         if (n instanceof ConcatNode)     { return digestConcat((ConcatNode) n); }
    else if (n instanceof UnionNode)      { return digestUnion((UnionNode) n); }
    else if (n instanceof RepetitionNode) { return digestRepetition((RepetitionNode) n); }
    else if (n instanceof OptionalNode)   { return digestOptional((OptionalNode) n); }
    else if (n instanceof StarNode)       { return digestStar((StarNode) n); }
    else if (n instanceof PlusNode)       { return digestPlus((PlusNode) n); }
    else if (n instanceof CharClass)      { return digestAtom(); }
    else {
      System.err.printf("Unknown AST class: %s\n", n.getClass().getName());
      System.exit(1);
      return null;
    }
  }

  private static List<IncompleteTree> digestConcat (ConcatNode n) {
    List<IncompleteTree> digestedExprs = new LinkedList<IncompleteTree>();
    List<RegexNode> children = n.getChildren();
    int numChilds = children.size();

    for (int w = 1; w <= numChilds; w++) {
      for (int i = 0; i <= numChilds - w; i++) {
        List<RegexNode> prefix = new LinkedList<RegexNode>();
        List<RegexNode> midfix = new LinkedList<RegexNode>();
        List<RegexNode> suffix = new LinkedList<RegexNode>();

        // Collect nodes from 0 to i (exclusive).
        prefix = new LinkedList<RegexNode>(children.subList(0, i));

        // Collect nodes from i to i+n (exclusive).
        midfix = new LinkedList<RegexNode>(children.subList(i, i + w));

        // Collect nodes from i+n to end of list.
        suffix = new LinkedList<RegexNode>(children.subList(i + w, numChilds));

        List<IncompleteTree> digestedMidfixes = new LinkedList<IncompleteTree>();
        if (midfix.size() == 1) {
          digestedMidfixes.addAll(digestNode(midfix.get(0)));
        } else {
          HoleNode hole = new HoleNode();
          IncompleteTree pair = new IncompleteTree(hole, hole);
          digestedMidfixes.add(pair);
        }

        if (prefix.size() == 0 && suffix.size() == 0) {
          // If there are no prefix or suffix nodes, don't wrap the digested
          // node in a ConcatNode object which will only obfuscate the tree's
          // structure.
          digestedExprs.addAll(digestedMidfixes);
        } else {
          for (IncompleteTree digestedMidfix : digestedMidfixes) {
            List<RegexNode> newChildren = new LinkedList<RegexNode>();
            newChildren.addAll(prefix);
            newChildren.add(digestedMidfix.getTree());
            newChildren.addAll(suffix);

            RegexNode tree = new ConcatNode(newChildren);
            HoleNode hole = digestedMidfix.getHole();
            IncompleteTree pair = new IncompleteTree(tree, hole);
            digestedExprs.add(pair);
          }
        }
      }
    }

    return digestedExprs;
  }

  private static List<IncompleteTree> digestUnion (UnionNode n) {
    List<IncompleteTree> digestedExprs = new LinkedList<IncompleteTree>();

    // Recursively compute holes of left sub-node(s).
    for (IncompleteTree digestedExpr : digestNode(n.getLeftChild())) {
      RegexNode tree = new UnionNode(digestedExpr.getTree(), n.getRightChild());
      IncompleteTree pair = new IncompleteTree(tree, digestedExpr.getHole());
      digestedExprs.add(pair);
    }

    // Recursively compute holes of right sub-node(s).
    for (IncompleteTree digestedExpr : digestNode(n.getRightChild())) {
      RegexNode tree = new UnionNode(n.getLeftChild(), digestedExpr.getTree());
      IncompleteTree pair = new IncompleteTree(tree, digestedExpr.getHole());
      digestedExprs.add(pair);
    }

    // Replace entire node with a hole.
    HoleNode hole = new HoleNode();
    IncompleteTree pair = new IncompleteTree(hole, hole);
    digestedExprs.add(pair);

    return digestedExprs;
  }

  private static List<IncompleteTree> digestRepetition (RepetitionNode n) {
    List<IncompleteTree> digestedExprs = new LinkedList<IncompleteTree>();

    // Recursively compute holes of sub-node(s).
    for (IncompleteTree digestedExpr : digestNode(n.getChild())) {
      if (n.hasMax()) {
        int min = n.getMin();
        int max = n.getMax();
        RegexNode tree = new RepetitionNode(digestedExpr.getTree(), min, max);
        IncompleteTree pair = new IncompleteTree(tree, digestedExpr.getHole());
        digestedExprs.add(pair);
      } else {
        int min = n.getMin();
        RegexNode tree = new RepetitionNode(digestedExpr.getTree(), min);
        IncompleteTree pair = new IncompleteTree(tree, digestedExpr.getHole());
        digestedExprs.add(pair);
      }
    }

    // Replace entire node with a hole.
    HoleNode hole = new HoleNode();
    IncompleteTree pair = new IncompleteTree(hole, hole);
    digestedExprs.add(pair);

    return digestedExprs;
  }

  private static List<IncompleteTree> digestOptional (OptionalNode n) {
    List<IncompleteTree> digestedExprs = new LinkedList<IncompleteTree>();

    // Recursively compute holes of sub-node(s).
    for (IncompleteTree digestedExpr : digestNode(n.getChild())) {
      RegexNode tree = new OptionalNode(digestedExpr.getTree());
      IncompleteTree pair = new IncompleteTree(tree, digestedExpr.getHole());
      digestedExprs.add(pair);
    }

    // Replace entire node with a hole.
    HoleNode hole = new HoleNode();
    IncompleteTree pair = new IncompleteTree(hole, hole);
    digestedExprs.add(pair);

    return digestedExprs;
  }

  private static List<IncompleteTree> digestStar (StarNode n) {
    List<IncompleteTree> digestedExprs = new LinkedList<IncompleteTree>();

    // Recursively compute holes of sub-node(s).
    for (IncompleteTree digestedExpr : digestNode(n.getChild())) {
      RegexNode tree = new StarNode(digestedExpr.getTree());
      IncompleteTree pair = new IncompleteTree(tree, digestedExpr.getHole());
      digestedExprs.add(pair);
    }

    // Replace entire node with a hole.
    HoleNode hole = new HoleNode();
    IncompleteTree pair = new IncompleteTree(hole, hole);
    digestedExprs.add(pair);

    return digestedExprs;
  }

  private static List<IncompleteTree> digestPlus (PlusNode n) {
    List<IncompleteTree> digestedExprs = new LinkedList<IncompleteTree>();

    // Recursively compute holes of sub-node(s).
    for (IncompleteTree digestedExpr : digestNode(n.getChild())) {
      RegexNode tree = new PlusNode(digestedExpr.getTree());
      IncompleteTree pair = new IncompleteTree(tree, digestedExpr.getHole());
      digestedExprs.add(pair);
    }

    // Replace entire node with a hole.
    HoleNode hole = new HoleNode();
    IncompleteTree pair = new IncompleteTree(hole, hole);
    digestedExprs.add(pair);

    return digestedExprs;
  }

  private static List<IncompleteTree> digestAtom () {
    LinkedList<IncompleteTree> digestedExprs = new LinkedList<IncompleteTree>();

    // Replace entire node with a hole.
    HoleNode hole = new HoleNode();
    IncompleteTree pair = new IncompleteTree(hole, hole);
    digestedExprs.add(pair);

    return digestedExprs;
  }
}
