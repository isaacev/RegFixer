package edu.wisc.regfixer.enumerate;

import java.util.LinkedList;
import java.util.List;
import java.util.stream.Collectors;

import edu.wisc.regfixer.parser.CharClass;
import edu.wisc.regfixer.parser.ConcatNode;
import edu.wisc.regfixer.parser.OptionalNode;
import edu.wisc.regfixer.parser.PlusNode;
import edu.wisc.regfixer.parser.RegexNode;
import edu.wisc.regfixer.parser.RepetitionNode;
import edu.wisc.regfixer.parser.StarNode;
import edu.wisc.regfixer.parser.UnionNode;

public class Grafter {
  public static Enumerant graft (Enumerant original, HoleNode hole, Enumerant twig, HoleNode.ExpansionChoice expansion) {
    if (original.getHoles().contains(hole) == false) {
      throw new IllegalArgumentException("hole object must be in the partial tree");
    }

    RegexNode graftedTree = graftNode(original.getTree(), hole, twig.getTree());
    List<HoleNode> graftedHoles = original.getHoles()
      .stream()
      .filter(h -> h != hole)
      .collect(Collectors.toList());
    graftedHoles.addAll(twig.getHoles());
    int graftedCost = original.getCost() + twig.getCost();

    return new Enumerant(graftedTree, graftedHoles, graftedCost, expansion);
  }

  public static RegexNode graft (RegexNode original, HoleNode hole, RegexNode twig) {
    return graftNode(original, hole, twig);
  }

  private static RegexNode graftNode (RegexNode node, HoleNode hole, RegexNode twig) {
         if (node instanceof HoleNode)       { return graftHole((HoleNode) node, hole, twig); }
    else if (node instanceof ConcatNode)     { return graftConcat((ConcatNode) node, hole, twig); }
    else if (node instanceof UnionNode)      { return graftUnion((UnionNode) node, hole, twig); }
    else if (node instanceof RepetitionNode) { return graftRepetition((RepetitionNode) node, hole, twig); }
    else if (node instanceof OptionalNode)   { return graftOptional((OptionalNode) node, hole, twig); }
    else if (node instanceof StarNode)       { return graftStar((StarNode) node, hole, twig); }
    else if (node instanceof PlusNode)       { return graftPlus((PlusNode) node, hole, twig); }
    else if (node instanceof CharClass)      { return graftAtom(node); }
    else {
      System.err.printf("Unknown AST class: %s\n", node.getClass().getName());
      System.exit(1);
      return null;
    }
  }

  private static RegexNode graftHole (HoleNode node, HoleNode hole, RegexNode twig) {
    if (hole == node) {
      return twig;
    } else {
      return node;
    }
  }

  private static RegexNode graftConcat (ConcatNode node, HoleNode hole, RegexNode twig) {
    List<RegexNode> children = node.getChildren();
    List<RegexNode> newChildren = new LinkedList<>(children);
    boolean childrenNoChange = true;

    for (int i = 0; i < children.size(); i++) {
      RegexNode graftee = graftNode(children.get(i), hole, twig);

      if (graftee != children.get(i)) {
        childrenNoChange = false;
        newChildren.set(i, graftee);
      }
    }

    if (childrenNoChange) {
      return node;
    } else {
      return new ConcatNode(newChildren);
    }
  }

  private static RegexNode graftUnion (UnionNode node, HoleNode hole, RegexNode twig) {
    RegexNode leftGraftee  = graftNode(node.getLeftChild(), hole, twig);
    RegexNode rightGraftee = graftNode(node.getRightChild(), hole, twig);

    boolean leftNoChange  = (leftGraftee == node.getLeftChild());
    boolean rightNoChange = (rightGraftee == node.getRightChild());
    if (leftNoChange && rightNoChange) {
      return node;
    } else {
      return new UnionNode(leftGraftee, rightGraftee);
    }
  }

  private static RegexNode graftRepetition (RepetitionNode node, HoleNode hole, RegexNode twig) {
    RegexNode graftee = graftNode(node.getChild(), hole, twig);

    if (graftee == node.getChild()) {
      return node;
    } else if (node.hasMax()) {
      return new RepetitionNode(graftee, node.getMin(), node.getMax());
    } else {
      return new RepetitionNode(graftee, node.getMin());
    }
  }

  private static RegexNode graftOptional (OptionalNode node, HoleNode hole, RegexNode twig) {
    RegexNode graftee = graftNode(node.getChild(), hole, twig);

    if (graftee == node) {
      return node;
    } else {
      return new OptionalNode(graftee);
    }
  }

  private static RegexNode graftStar (StarNode node, HoleNode hole, RegexNode twig) {
    RegexNode graftee = graftNode(node.getChild(), hole, twig);

    if (graftee == node) {
      return node;
    } else {
      return new StarNode(graftee);
    }
  }

  private static RegexNode graftPlus (PlusNode node, HoleNode hole, RegexNode twig) {
    RegexNode graftee = graftNode(node.getChild(), hole, twig);

    if (graftee == node) {
      return node;
    } else {
      return new PlusNode(graftee);
    }
  }

  private static RegexNode graftAtom (RegexNode node) {
    return node;
  }
}
