package edu.wisc.regfixer.enumerate;

import java.util.LinkedList;
import java.util.List;
import java.util.stream.Collectors;

import edu.wisc.regfixer.parser.Bounds;
import edu.wisc.regfixer.parser.CharClass;
import edu.wisc.regfixer.parser.ConcatNode;
import edu.wisc.regfixer.parser.OptionalNode;
import edu.wisc.regfixer.parser.PlusNode;
import edu.wisc.regfixer.parser.RegexNode;
import edu.wisc.regfixer.parser.RepetitionNode;
import edu.wisc.regfixer.parser.StarNode;
import edu.wisc.regfixer.parser.UnionNode;

public class Grafter {
  public static Enumerant graft (Enumerant original, UnknownId id, Object twig) {
    RegexNode graftedTree = graftNode(original.getTree(), id, twig);
    List<Unknown> graftedUnknowns = original.getUnknowns()
      .stream()
      .filter(h -> h.getId() != id)
      .collect(Collectors.toList());
    if (twig instanceof Unknown) {
      graftedUnknowns.add((Unknown)twig);
    }
    int graftedCost = original.getCost() + 1;

    return new Enumerant(graftedTree, graftedUnknowns, graftedCost, original.getExpansion());
  }

  public static Enumerant graft (Enumerant original, UnknownChar unknown, Enumerant twig, Expansion expansion) {
    if (original.getUnknowns().contains(unknown) == false) {
      throw new IllegalArgumentException("unknown object must be in the partial tree");
    }

    RegexNode graftedTree = graftNode(original.getTree(), unknown.getId(), twig.getTree());
    List<Unknown> graftedUnknowns = original.getUnknowns()
      .stream()
      .filter(h -> h != unknown)
      .collect(Collectors.toList());
    graftedUnknowns.addAll(twig.getUnknowns());
    int graftedCost = original.getCost() + twig.getCost();

    return new Enumerant(graftedTree, graftedUnknowns, graftedCost, expansion);
  }

  public static RegexNode graft (RegexNode original, UnknownId id, Object scion) {
    return graftNode(original, id, scion);
  }

  public static RegexNode graft (RegexNode original, UnknownChar unknown, Object scion) {
    return graftNode(original, unknown.getId(), scion);
  }

  private static RegexNode graftNode (RegexNode node, UnknownId id, Object scion) {
         if (node instanceof UnknownChar)    { return graftUnknown((UnknownChar) node, id, scion); }
    else if (node instanceof ConcatNode)     { return graftConcat((ConcatNode) node, id, scion); }
    else if (node instanceof UnionNode)      { return graftUnion((UnionNode) node, id, scion); }
    else if (node instanceof RepetitionNode) { return graftRepetition((RepetitionNode) node, id, scion); }
    else if (node instanceof OptionalNode)   { return graftOptional((OptionalNode) node, id, scion); }
    else if (node instanceof StarNode)       { return graftStar((StarNode) node, id, scion); }
    else if (node instanceof PlusNode)       { return graftPlus((PlusNode) node, id, scion); }
    else if (node instanceof CharClass)      { return graftAtom(node); }
    else {
      System.err.printf("Unknown AST class: %s\n", node.getClass().getName());
      System.exit(1);
      return null;
    }
  }

  private static RegexNode graftUnknown (UnknownChar node, UnknownId id, Object scion) {
    if (node.getId().equals(id)) {
      if (scion instanceof RegexNode) {
        return (RegexNode) scion;
      } else {
        String msg = String.format("cannot graft '%s'", scion);
        throw new IllegalArgumentException(msg);
      }
    } else {
      return node;
    }
  }

  private static RegexNode graftConcat (ConcatNode node, UnknownId id, Object scion) {
    List<RegexNode> children = node.getChildren();
    List<RegexNode> newChildren = new LinkedList<>(children);
    boolean childrenNoChange = true;

    for (int i = 0; i < children.size(); i++) {
      RegexNode graftee = graftNode(children.get(i), id, scion);

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

  private static RegexNode graftUnion (UnionNode node, UnknownId id, Object scion) {
    RegexNode leftGraftee  = graftNode(node.getLeftChild(), id, scion);
    RegexNode rightGraftee = graftNode(node.getRightChild(), id, scion);

    boolean leftNoChange  = (leftGraftee == node.getLeftChild());
    boolean rightNoChange = (rightGraftee == node.getRightChild());
    if (leftNoChange && rightNoChange) {
      return node;
    } else {
      return new UnionNode(leftGraftee, rightGraftee);
    }
  }

  private static RegexNode graftRepetition (RepetitionNode node, UnknownId id, Object scion) {
    if (node.getBounds() instanceof UnknownBounds && scion instanceof Bounds) {
      UnknownBounds unknown = (UnknownBounds)node.getBounds();
      if (unknown.getId().equals(id)) {
        return new RepetitionNode(node.getChild(), (Bounds)scion);
      }
    }

    RegexNode graftee = graftNode(node.getChild(), id, scion);

    if (graftee == node.getChild()) {
      return node;
    } else {
      return new RepetitionNode(graftee, node.getBounds());
    }
  }

  private static RegexNode graftOptional (OptionalNode node, UnknownId id, Object scion) {
    RegexNode graftee = graftNode(node.getChild(), id, scion);

    if (graftee == node) {
      return node;
    } else {
      return new OptionalNode(graftee);
    }
  }

  private static RegexNode graftStar (StarNode node, UnknownId id, Object scion) {
    RegexNode graftee = graftNode(node.getChild(), id, scion);

    if (graftee == node) {
      return node;
    } else {
      return new StarNode(graftee);
    }
  }

  private static RegexNode graftPlus (PlusNode node, UnknownId id, Object scion) {
    RegexNode graftee = graftNode(node.getChild(), id, scion);

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
