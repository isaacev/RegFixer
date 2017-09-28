package edu.wisc.regfixer.enumerate;

import java.util.Arrays;
import java.util.LinkedList;
import java.util.List;

import edu.wisc.regfixer.parser.CharClass;
import edu.wisc.regfixer.parser.ConcatNode;
import edu.wisc.regfixer.parser.OptionalNode;
import edu.wisc.regfixer.parser.PlusNode;
import edu.wisc.regfixer.parser.RegexNode;
import edu.wisc.regfixer.parser.RepetitionNode;
import edu.wisc.regfixer.parser.StarNode;
import edu.wisc.regfixer.parser.UnionNode;

public class Slicer {
  public static List<Enumerant> slice (RegexNode node) {
    return sliceNode(node);
  }

  private static List<Enumerant> sliceNode (RegexNode node) {
         if (node instanceof ConcatNode)     { return sliceConcat((ConcatNode) node); }
    else if (node instanceof UnionNode)      { return sliceUnion((UnionNode) node); }
    else if (node instanceof RepetitionNode) { return sliceRepetition((RepetitionNode) node); }
    else if (node instanceof OptionalNode)   { return sliceOptional((OptionalNode) node); }
    else if (node instanceof StarNode)       { return sliceStar((StarNode) node); }
    else if (node instanceof PlusNode)       { return slicePlus((PlusNode) node); }
    else if (node instanceof CharClass)      { return sliceAtomic(); }
    else {
      System.err.printf("Unknown AST class: %s\n", node.getClass().getName());
      System.exit(1);
      return null;
    }
  }

  private static List<Enumerant> sliceConcat (ConcatNode node) {
    List<Enumerant> partials = new LinkedList<>();

    // Replace sub-lists of children with single holes
    List<RegexNode> children = node.getChildren();
    int totalChildren = children.size();
    for (int w = 1; w <= totalChildren; w++) {
      for (int i = 0; i <= totalChildren - w; i++) {
        // Collect nodes from 0 to i (exclusive).
        List<RegexNode> prefix = new LinkedList<>(children.subList(0, i));

        // Collect nodes from i to i+n (exclusive).
        List<RegexNode> midfix = new LinkedList<>(children.subList(i, i + w));

        // Collect nodes from i+n to end of list.
        List<RegexNode> suffix = new LinkedList<>(children.subList(i + w, totalChildren));

        List<Enumerant> midfixPartials = new LinkedList<>();
        if (midfix.size() == 1) {
          midfixPartials.addAll(sliceNode(midfix.get(0)));
        } else {
          int descendants = midfix.stream().mapToInt(RegexNode::descendants).sum();
          midfixPartials.add(new Enumerant(new HoleNode(), descendants));
        }

        if (prefix.size() == 0 && suffix.size() == 0) {
          // If there are no prefix or suffix nodes, don't wrap the midfix
          // partials in a ConcatNode object which will only obfuscate the
          // tree's structure.
          partials.addAll(midfixPartials);
        } else {
          for (Enumerant midfixPartial : midfixPartials) {
            List<RegexNode> partialChildren = new LinkedList<>();
            partialChildren.addAll(prefix);
            partialChildren.add(midfixPartial.getTree());
            partialChildren.addAll(suffix);

            ConcatNode partialNode = new ConcatNode(partialChildren);
            partials.add(new Enumerant(partialNode, midfixPartial));
          }
        }
      }
    }

    // Add holes between children
    for (int i = 0; i <= totalChildren; i++) {
      HoleNode newHole = new HoleNode();
      List<RegexNode> newChildren = new LinkedList<>();

      if (i < totalChildren) {
        newChildren.addAll(children.subList(0, i));
        newChildren.add(newHole);
        newChildren.addAll(children.subList(i, totalChildren));
      } else {
        newChildren.addAll(children);
        newChildren.add(newHole);
      }

      ConcatNode newNode = new ConcatNode(newChildren);
      partials.add(new Enumerant(newNode, newHole, 1));
    }

    return partials;
  }

  private static List<Enumerant> sliceUnion (UnionNode node) {
    List<Enumerant> partials = new LinkedList<>();

    for (Enumerant partial : sliceNode(node.getLeftChild())) {
      UnionNode branch = new UnionNode(partial.getTree(), node.getRightChild());
      partials.add(new Enumerant(branch, partial));
    }

    for (Enumerant partial : sliceNode(node.getRightChild())) {
      UnionNode branch = new UnionNode(node.getLeftChild(), partial.getTree());
      partials.add(new Enumerant(branch, partial));
    }

    partials.add(new Enumerant(new HoleNode(), node.descendants()));
    return partials;
  }

  private static List<Enumerant> sliceRepetition (RepetitionNode node) {
    return null;
  }

  private static List<Enumerant> sliceOptional (OptionalNode node) {
    List<Enumerant> partials = new LinkedList<>();

    for (Enumerant partial : sliceNode(node.getChild())) {
      OptionalNode branch = new OptionalNode(partial.getTree());
      partials.add(new Enumerant(branch, partial));
    }

    partials.add(new Enumerant(new HoleNode(false), node.descendants()));
    return partials;
  }

  private static List<Enumerant> sliceStar (StarNode node) {
    List<Enumerant> partials = new LinkedList<>();

    for (Enumerant partial : sliceNode(node.getChild())) {
      StarNode branch = new StarNode(partial.getTree());
      partials.add(new Enumerant(branch, partial));
    }

    partials.add(new Enumerant(new HoleNode(false), node.descendants()));
    return partials;
  }

  private static List<Enumerant> slicePlus (PlusNode node) {
    List<Enumerant> partials = new LinkedList<>();

    for (Enumerant partial : sliceNode(node.getChild())) {
      PlusNode branch = new PlusNode(partial.getTree());
      partials.add(new Enumerant(branch, partial));
    }

    partials.add(new Enumerant(new HoleNode(false), node.descendants()));
    return partials;
  }

  private static List<Enumerant> sliceAtomic () {
    return Arrays.asList(new Enumerant(new HoleNode(), 1));
  }
}
