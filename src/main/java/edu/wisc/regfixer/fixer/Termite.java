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
  public static List<PartialTree> digest (RegexNode n) {
    return digestNode(n);
  }

  private static List<PartialTree> digestNode (RegexNode n) {
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

  private static List<PartialTree> digestConcat (ConcatNode n) {
    List<PartialTree> digestedExprs = new LinkedList<PartialTree>();
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

        List<PartialTree> digestedMidfixes = new LinkedList<PartialTree>();
        if (midfix.size() == 1) {
          digestedMidfixes.addAll(digestNode(midfix.get(0)));
        } else {
          HoleNode hole = new HoleNode();
          PartialTree pair = new PartialTree(hole, hole);
          digestedMidfixes.add(pair);
        }

        if (prefix.size() == 0 && suffix.size() == 0) {
          // If there are no prefix or suffix nodes, don't wrap the digested
          // node in a ConcatNode object which will only obfuscate the tree's
          // structure.
          digestedExprs.addAll(digestedMidfixes);
        } else {
          for (PartialTree digestedMidfix : digestedMidfixes) {
            List<RegexNode> newChildren = new LinkedList<RegexNode>();
            newChildren.addAll(prefix);
            newChildren.add(digestedMidfix.getTree());
            newChildren.addAll(suffix);

            RegexNode tree = new ConcatNode(newChildren);
            HoleNode hole = digestedMidfix.getHole();
            PartialTree pair = new PartialTree(tree, hole);
            digestedExprs.add(pair);
          }
        }
      }
    }

    return digestedExprs;
  }

  private static List<PartialTree> digestUnion (UnionNode n) {
    List<PartialTree> digestedExprs = new LinkedList<PartialTree>();

    // Recursively compute holes of left sub-node(s).
    for (PartialTree digestedExpr : digestNode(n.getLeftChild())) {
      RegexNode tree = new UnionNode(digestedExpr.getTree(), n.getRightChild());
      PartialTree pair = new PartialTree(tree, digestedExpr.getHole());
      digestedExprs.add(pair);
    }

    // Recursively compute holes of right sub-node(s).
    for (PartialTree digestedExpr : digestNode(n.getRightChild())) {
      RegexNode tree = new UnionNode(n.getLeftChild(), digestedExpr.getTree());
      PartialTree pair = new PartialTree(tree, digestedExpr.getHole());
      digestedExprs.add(pair);
    }

    // Replace entire node with a hole.
    HoleNode hole = new HoleNode();
    PartialTree pair = new PartialTree(hole, hole);
    digestedExprs.add(pair);

    return digestedExprs;
  }

  private static List<PartialTree> digestRepetition (RepetitionNode n) {
    List<PartialTree> digestedExprs = new LinkedList<PartialTree>();

    // Recursively compute holes of sub-node(s).
    for (PartialTree digestedExpr : digestNode(n.getChild())) {
      if (n.hasMax()) {
        int min = n.getMin();
        int max = n.getMax();
        RegexNode tree = new RepetitionNode(digestedExpr.getTree(), min, max);
        PartialTree pair = new PartialTree(tree, digestedExpr.getHole());
        digestedExprs.add(pair);
      } else {
        int min = n.getMin();
        RegexNode tree = new RepetitionNode(digestedExpr.getTree(), min);
        PartialTree pair = new PartialTree(tree, digestedExpr.getHole());
        digestedExprs.add(pair);
      }
    }

    // Replace entire node with a hole.
    HoleNode hole = new HoleNode();
    PartialTree pair = new PartialTree(hole, hole);
    digestedExprs.add(pair);

    return digestedExprs;
  }

  private static List<PartialTree> digestOptional (OptionalNode n) {
    List<PartialTree> digestedExprs = new LinkedList<PartialTree>();

    // Recursively compute holes of sub-node(s).
    for (PartialTree digestedExpr : digestNode(n.getChild())) {
      RegexNode tree = new OptionalNode(digestedExpr.getTree());
      PartialTree pair = new PartialTree(tree, digestedExpr.getHole());
      digestedExprs.add(pair);
    }

    // Replace entire node with a hole.
    HoleNode hole = new HoleNode();
    PartialTree pair = new PartialTree(hole, hole);
    digestedExprs.add(pair);

    return digestedExprs;
  }

  private static List<PartialTree> digestStar (StarNode n) {
    List<PartialTree> digestedExprs = new LinkedList<PartialTree>();

    // Recursively compute holes of sub-node(s).
    for (PartialTree digestedExpr : digestNode(n.getChild())) {
      RegexNode tree = new StarNode(digestedExpr.getTree());
      PartialTree pair = new PartialTree(tree, digestedExpr.getHole());
      digestedExprs.add(pair);
    }

    // Replace entire node with a hole.
    HoleNode hole = new HoleNode();
    PartialTree pair = new PartialTree(hole, hole);
    digestedExprs.add(pair);

    return digestedExprs;
  }

  private static List<PartialTree> digestPlus (PlusNode n) {
    List<PartialTree> digestedExprs = new LinkedList<PartialTree>();

    // Recursively compute holes of sub-node(s).
    for (PartialTree digestedExpr : digestNode(n.getChild())) {
      RegexNode tree = new PlusNode(digestedExpr.getTree());
      PartialTree pair = new PartialTree(tree, digestedExpr.getHole());
      digestedExprs.add(pair);
    }

    // Replace entire node with a hole.
    HoleNode hole = new HoleNode();
    PartialTree pair = new PartialTree(hole, hole);
    digestedExprs.add(pair);

    return digestedExprs;
  }

  private static List<PartialTree> digestAtom () {
    LinkedList<PartialTree> digestedExprs = new LinkedList<PartialTree>();

    // Replace entire node with a hole.
    HoleNode hole = new HoleNode();
    PartialTree pair = new PartialTree(hole, hole);
    digestedExprs.add(pair);

    return digestedExprs;
  }
}
