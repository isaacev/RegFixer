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
  public static Enumerant graft (Enumerant original, UnknownInt unknown, int lower, int upper) {
    // TODO
    return null;
  }

  public static Enumerant graft (Enumerant original, UnknownNode unknown, Enumerant twig, UnknownNode.ExpansionChoice expansion) {
    if (original.getUnknowns().contains(unknown) == false) {
      throw new IllegalArgumentException("unknown object must be in the partial tree");
    }

    RegexNode graftedTree = graftNode(original.getTree(), unknown, twig.getTree());
    List<Unknown> graftedUnknowns = original.getUnknowns()
      .stream()
      .filter(h -> h != unknown)
      .collect(Collectors.toList());
    graftedUnknowns.addAll(twig.getUnknowns());
    int graftedCost = original.getCost() + twig.getCost();

    return new Enumerant(graftedTree, graftedUnknowns, graftedCost, expansion);
  }

  public static RegexNode graft (RegexNode original, UnknownNode unknown, RegexNode twig) {
    return graftNode(original, unknown, twig);
  }

  private static RegexNode graftNode (RegexNode node, UnknownNode unknown, RegexNode twig) {
         if (node instanceof UnknownNode)    { return graftUnknown((UnknownNode) node, unknown, twig); }
    else if (node instanceof ConcatNode)     { return graftConcat((ConcatNode) node, unknown, twig); }
    else if (node instanceof UnionNode)      { return graftUnion((UnionNode) node, unknown, twig); }
    else if (node instanceof RepetitionNode) { return graftRepetition((RepetitionNode) node, unknown, twig); }
    else if (node instanceof OptionalNode)   { return graftOptional((OptionalNode) node, unknown, twig); }
    else if (node instanceof StarNode)       { return graftStar((StarNode) node, unknown, twig); }
    else if (node instanceof PlusNode)       { return graftPlus((PlusNode) node, unknown, twig); }
    else if (node instanceof CharClass)      { return graftAtom(node); }
    else {
      System.err.printf("Unknown AST class: %s\n", node.getClass().getName());
      System.exit(1);
      return null;
    }
  }

  private static RegexNode graftUnknown (UnknownNode node, UnknownNode unknown, RegexNode twig) {
    if (unknown == node) {
      return twig;
    } else {
      return node;
    }
  }

  private static RegexNode graftConcat (ConcatNode node, UnknownNode unknown, RegexNode twig) {
    List<RegexNode> children = node.getChildren();
    List<RegexNode> newChildren = new LinkedList<>(children);
    boolean childrenNoChange = true;

    for (int i = 0; i < children.size(); i++) {
      RegexNode graftee = graftNode(children.get(i), unknown, twig);

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

  private static RegexNode graftUnion (UnionNode node, UnknownNode unknown, RegexNode twig) {
    RegexNode leftGraftee  = graftNode(node.getLeftChild(), unknown, twig);
    RegexNode rightGraftee = graftNode(node.getRightChild(), unknown, twig);

    boolean leftNoChange  = (leftGraftee == node.getLeftChild());
    boolean rightNoChange = (rightGraftee == node.getRightChild());
    if (leftNoChange && rightNoChange) {
      return node;
    } else {
      return new UnionNode(leftGraftee, rightGraftee);
    }
  }

  private static RegexNode graftRepetition (RepetitionNode node, UnknownNode unknown, RegexNode twig) {
    RegexNode graftee = graftNode(node.getChild(), unknown, twig);

    if (graftee == node.getChild()) {
      return node;
    } else if (node.hasMax()) {
      return new RepetitionNode(graftee, node.getMin(), node.getMax());
    } else {
      return new RepetitionNode(graftee, node.getMin());
    }
  }

  private static RegexNode graftOptional (OptionalNode node, UnknownNode unknown, RegexNode twig) {
    RegexNode graftee = graftNode(node.getChild(), unknown, twig);

    if (graftee == node) {
      return node;
    } else {
      return new OptionalNode(graftee);
    }
  }

  private static RegexNode graftStar (StarNode node, UnknownNode unknown, RegexNode twig) {
    RegexNode graftee = graftNode(node.getChild(), unknown, twig);

    if (graftee == node) {
      return node;
    } else {
      return new StarNode(graftee);
    }
  }

  private static RegexNode graftPlus (PlusNode node, UnknownNode unknown, RegexNode twig) {
    RegexNode graftee = graftNode(node.getChild(), unknown, twig);

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
