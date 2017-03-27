package RegFixer;

import java.util.*;
import RegexParser.*;

/**
 * Grafter is responsible for taking a regular expression abstract syntax tree
 * with a hole and attach some other regular expression tree at that location to
 * produce a larger AST with no holes.
 */
class Grafter {
  public static RegexNode graft (DigestedTree tree, RegexNode branch) {
    return graftNode(tree.getRoot(), branch);
  }

  static RegexNode graftNode (RegexNode tree, RegexNode branch) {
         if (tree instanceof ConcatNode)   { return graftConcat((ConcatNode) tree, branch); }
    else if (tree instanceof UnionNode)    { return graftUnion((UnionNode) tree, branch); }
    else if (tree instanceof OptionalNode) { return graftOptional((OptionalNode) tree, branch); }
    else if (tree instanceof StarNode)     { return graftStar((StarNode) tree, branch); }
    else if (tree instanceof PlusNode)     { return graftPlus((PlusNode) tree, branch); }
    else if (tree instanceof HoleNode)     { return graftHole((HoleNode) tree, branch); }
    else if (tree instanceof CharNode)     { return graftAtom(); }
    else {
      System.err.printf("Unknown AST class: %s\n", tree.getClass().getName());
      System.exit(1);
      return null;
    }
  }

  static RegexNode graftConcat (ConcatNode node, RegexNode branch) {
    List<RegexNode> children = node.getChildren();
    for (int i = 0; i < children.size(); i++) {
      RegexNode childGrafted = graftNode(children.get(i), branch);

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

  static RegexNode graftUnion (UnionNode node, RegexNode branch) {
    RegexNode leftChildGrafted = graftNode(node.getLeftChild(), branch);

    if (leftChildGrafted != null) {
      // Node *IS* an ancestor (through the left child) of a hole and must be
      // copied and then modified.
      return new UnionNode(leftChildGrafted, node.getRightChild());
    }

    RegexNode rightChildGrafted = graftNode(node.getRightChild(), branch);

    if (rightChildGrafted != null) {
      // Node *IS* an ancestor (through the right child) of a hole and must be
      // copied and then modified.
      return new UnionNode(node.getLeftChild(), rightChildGrafted);
    }

    // Node is *NOT* an ancestor of a hole so return `null`.
    return null;
  }

  static RegexNode graftOptional (OptionalNode node, RegexNode branch) {
    RegexNode childGraft = graftNode(node.getChild(), branch);

    if (childGraft != null) {
      return new OptionalNode(childGraft);
    }

    // Node is *NOT* an ancestor of a hole so return `null`.
    return null;
  }

  static RegexNode graftStar (StarNode node, RegexNode branch) {
    RegexNode childGraft = graftNode(node.getChild(), branch);

    if (childGraft != null) {
      return new StarNode(childGraft);
    }

    // Node is *NOT* an ancestor of a hole so return `null`.
    return null;
  }

  static RegexNode graftPlus (PlusNode node, RegexNode branch) {
    RegexNode childGraft = graftNode(node.getChild(), branch);

    if (childGraft != null) {
      return new PlusNode(childGraft);
    }

    // Node is *NOT* an ancestor of a hole so return `null`.
    return null;
  }

  static RegexNode graftHole (RegexNode node, RegexNode branch) {
    return branch;
  }

  static RegexNode graftAtom () {
    return null;
  }
}
