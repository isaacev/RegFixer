package RegFixer;

import java.util.*;
import RegexParser.*;

/**
 * Grafter is responsible for taking a regular expression abstract syntax tree
 * with a hole and attach some other regular expression tree at that location to
 * produce a larger AST with no holes.
 */
class Grafter {
  public static RegexNode graft (TermiteTree tree, RegexNode twig) {
    return graftNode(tree.getRoot(), twig);
  }

  static RegexNode graftNode (RegexNode tree, RegexNode twig) {
         if (tree instanceof ConcatNode)    { return graftConcat((ConcatNode) tree, twig); }
    else if (tree instanceof UnionNode)     { return graftUnion((UnionNode) tree, twig); }
    else if (tree instanceof OptionalNode)  { return graftOptional((OptionalNode) tree, twig); }
    else if (tree instanceof StarNode)      { return graftStar((StarNode) tree, twig); }
    else if (tree instanceof PlusNode)      { return graftPlus((PlusNode) tree, twig); }
    else if (tree instanceof CharClassNode) { return graftAtom(); }
    else if (tree instanceof CharNode)      { return graftAtom(); }
    else if (tree instanceof HoleNode)      { return graftHole((HoleNode) tree, twig); }
    else {
      System.err.printf("Unknown AST class: %s\n", tree.getClass().getName());
      System.exit(1);
      return null;
    }
  }

  static RegexNode graftConcat (ConcatNode node, RegexNode twig) {
    List<RegexNode> children = node.getChildren();
    for (int i = 0; i < children.size(); i++) {
      RegexNode childGrafted = graftNode(children.get(i), twig);

      if (childGrafted != null) {
        // Node *IS* an ancestor of a hole and must be copied and then modified.
        List<RegexNode> newChildren = new LinkedList<RegexNode>(children);
        newChildren.set(i, childGrafted);

        return new ConcatNode(newChildren);
      }
    }

    // Node is *NOT* an ancestor of a hole so return `null`.
    return null;
  }

  static RegexNode graftUnion (UnionNode node, RegexNode twig) {
    RegexNode leftChildGrafted = graftNode(node.getLeftChild(), twig);

    if (leftChildGrafted != null) {
      // Node *IS* an ancestor (through the left child) of a hole and must be
      // copied and then modified.
      return new UnionNode(leftChildGrafted, node.getRightChild());
    }

    RegexNode rightChildGrafted = graftNode(node.getRightChild(), twig);

    if (rightChildGrafted != null) {
      // Node *IS* an ancestor (through the right child) of a hole and must be
      // copied and then modified.
      return new UnionNode(node.getLeftChild(), rightChildGrafted);
    }

    // Node is *NOT* an ancestor of a hole so return `null`.
    return null;
  }

  static RegexNode graftOptional (OptionalNode node, RegexNode twig) {
    RegexNode childGraft = graftNode(node.getChild(), twig);

    if (childGraft != null) {
      return new OptionalNode(childGraft);
    }

    // Node is *NOT* an ancestor of a hole so return `null`.
    return null;
  }

  static RegexNode graftStar (StarNode node, RegexNode twig) {
    RegexNode childGraft = graftNode(node.getChild(), twig);

    if (childGraft != null) {
      return new StarNode(childGraft);
    }

    // Node is *NOT* an ancestor of a hole so return `null`.
    return null;
  }

  static RegexNode graftPlus (PlusNode node, RegexNode twig) {
    RegexNode childGraft = graftNode(node.getChild(), twig);

    if (childGraft != null) {
      return new PlusNode(childGraft);
    }

    // Node is *NOT* an ancestor of a hole so return `null`.
    return null;
  }

  static RegexNode graftAtom () {
    return null;
  }

  static RegexNode graftHole (RegexNode node, RegexNode twig) {
    return twig;
  }
}
