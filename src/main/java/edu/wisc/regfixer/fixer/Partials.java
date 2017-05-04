package edu.wisc.regfixer.fixer;

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

public class Partials {
  public static List<PartialTree> slice (RegexNode node) {
    return nodePartials(node);
  }

  private static List<PartialTree> nodePartials (RegexNode node) {
         if (node instanceof ConcatNode)     { return concatPartials((ConcatNode) node); }
    else if (node instanceof UnionNode)      { return unionPartials((UnionNode) node); }
    else if (node instanceof RepetitionNode) { return repetitionPartials((RepetitionNode) node); }
    else if (node instanceof OptionalNode)   { return optionalPartials((OptionalNode) node); }
    else if (node instanceof StarNode)       { return starPartials((StarNode) node); }
    else if (node instanceof PlusNode)       { return plusPartials((PlusNode) node); }
    else if (node instanceof CharClass)      { return atomicPartials(); }
    else {
      System.err.printf("Unknown AST class: %s\n", node.getClass().getName());
      System.exit(1);
      return null;
    }
  }

  private static List<PartialTree> concatPartials (ConcatNode node) {
    List<PartialTree> partials = new LinkedList<>();

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

        List<PartialTree> midfixPartials = new LinkedList<>();
        if (midfix.size() == 1) {
          midfixPartials.addAll(nodePartials(midfix.get(0)));
        } else {
          int descendants = midfix.stream().mapToInt(RegexNode::descendants).sum();
          midfixPartials.add(new PartialTree(new HoleNode(), descendants));
        }

        if (prefix.size() == 0 && suffix.size() == 0) {
          // If there are no prefix or suffix nodes, don't wrap the midfix
          // partials in a ConcatNode object which will only obfuscate the
          // tree's structure.
          partials.addAll(midfixPartials);
        } else {
          for (PartialTree midfixPartial : midfixPartials) {
            List<RegexNode> partialChildren = new LinkedList<>();
            partialChildren.addAll(prefix);
            partialChildren.add(midfixPartial.getTree());
            partialChildren.addAll(suffix);

            ConcatNode partialNode = new ConcatNode(partialChildren);
            List<HoleNode> partialHoles = midfixPartial.getHoles();
            partials.add(new PartialTree(partialNode, partialHoles, midfixPartial.getRemovedNodes()));
          }
        }
      }
    }

    return partials;
  }

  private static List<PartialTree> unionPartials (UnionNode node) {
    List<PartialTree> partials = new LinkedList<>();

    for (PartialTree partial : nodePartials(node.getLeftChild())) {
      UnionNode branch = new UnionNode(partial.getTree(), node.getRightChild());
      partials.add(new PartialTree(branch, partial.getHoles(), partial.getRemovedNodes()));
    }

    for (PartialTree partial : nodePartials(node.getRightChild())) {
      UnionNode branch = new UnionNode(node.getLeftChild(), partial.getTree());
      partials.add(new PartialTree(branch, partial.getHoles(), partial.getRemovedNodes()));
    }

    partials.add(new PartialTree(new HoleNode(), node.descendants()));
    return partials;
  }

  private static List<PartialTree> repetitionPartials (RepetitionNode node) {
    return null;
  }

  private static List<PartialTree> optionalPartials (OptionalNode node) {
    List<PartialTree> partials = new LinkedList<>();

    for (PartialTree partial : nodePartials(node.getChild())) {
      OptionalNode branch = new OptionalNode(partial.getTree());
      partials.add(new PartialTree(branch, partial.getHoles(), partial.getRemovedNodes()));
    }

    partials.add(new PartialTree(new HoleNode(), node.descendants()));
    return partials;
  }

  private static List<PartialTree> starPartials (StarNode node) {
    List<PartialTree> partials = new LinkedList<>();

    for (PartialTree partial : nodePartials(node.getChild())) {
      StarNode branch = new StarNode(partial.getTree());
      partials.add(new PartialTree(branch, partial.getHoles(), partial.getRemovedNodes()));
    }

    partials.add(new PartialTree(new HoleNode(), node.descendants()));
    return partials;
  }

  private static List<PartialTree> plusPartials (PlusNode node) {
    List<PartialTree> partials = new LinkedList<>();

    for (PartialTree partial : nodePartials(node.getChild())) {
      PlusNode branch = new PlusNode(partial.getTree());
      partials.add(new PartialTree(branch, partial.getHoles(), partial.getRemovedNodes()));
    }

    partials.add(new PartialTree(new HoleNode(), node.descendants()));
    return partials;
  }

  private static List<PartialTree> atomicPartials () {
    return Arrays.asList(new PartialTree(new HoleNode(), 1));
  }
}
