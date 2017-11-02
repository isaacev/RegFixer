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

/**
 * Slicer is responsible for adding unknowns to a complete tree as a starting point
 * for the enumeration search algorithm. This includes adding unknown nodes in the
 * following places within the original tree:
 * - in place of each subexpression
 * - between each top-level subexpression
 * - concatenated with the wunknown expression (preceeding and succeeding)
 */
public class Slicer {
  public static List<Enumerant> slice (RegexNode node) {
    return sliceNode(node, new LinkedList<>());
  }

  private static List<Enumerant> sliceNode (RegexNode node, List<UnknownNode.ExpansionChoice> history) {
         if (node instanceof ConcatNode)     { return sliceConcat((ConcatNode) node, history); }
    else if (node instanceof UnionNode)      { return sliceUnion((UnionNode) node, history); }
    else if (node instanceof RepetitionNode) { return sliceRepetition((RepetitionNode) node, history); }
    else if (node instanceof OptionalNode)   { return sliceOptional((OptionalNode) node, history); }
    else if (node instanceof StarNode)       { return sliceStar((StarNode) node, history); }
    else if (node instanceof PlusNode)       { return slicePlus((PlusNode) node, history); }
    else if (node instanceof CharClass)      { return sliceAtomic(history); }
    else {
      System.err.printf("Unknown AST class: %s\n", node.getClass().getName());
      System.exit(1);
      return null;
    }
  }

  private static List<Enumerant> sliceConcat (ConcatNode node, List<UnknownNode.ExpansionChoice> history) {
    List<Enumerant> partials = new LinkedList<>();
    List<UnknownNode.ExpansionChoice> newHistory = new LinkedList<>(history);
    newHistory.add(UnknownNode.ExpansionChoice.Concat);

    // Replace sub-lists of children with single unknowns
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
          midfixPartials.addAll(sliceNode(midfix.get(0), newHistory));
        } else {
          UnknownNode unknown = new UnknownNode(newHistory);
          int descendants = midfix.stream().mapToInt(RegexNode::descendants).sum();
          midfixPartials.add(new Enumerant(unknown, unknown, descendants, UnknownNode.ExpansionChoice.Concat));
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
            partials.add(new Enumerant(partialNode, midfixPartial.getUnknowns(), 0, UnknownNode.ExpansionChoice.Concat));
          }
        }
      }
    }

    // Add unknowns between children
    for (int i = 0; i <= totalChildren; i++) {
      UnknownNode newUnknown = new UnknownNode((i == totalChildren) ? history : newHistory);
      List<RegexNode> newChildren = new LinkedList<>();

      if (i < totalChildren) {
        newChildren.addAll(children.subList(0, i));
        newChildren.add(newUnknown);
        newChildren.addAll(children.subList(i, totalChildren));
      } else {
        newChildren.addAll(children);
        newChildren.add(newUnknown);
      }

      ConcatNode newNode = new ConcatNode(newChildren);
      partials.add(new Enumerant(newNode, newUnknown, 1, UnknownNode.ExpansionChoice.Concat));
    }

    return partials;
  }

  private static List<Enumerant> sliceUnion (UnionNode node, List<UnknownNode.ExpansionChoice> history) {
    List<Enumerant> partials = new LinkedList<>();
    List<UnknownNode.ExpansionChoice> newHistory = new LinkedList<>(history);
    newHistory.add(UnknownNode.ExpansionChoice.Union);

    for (Enumerant partial : sliceNode(node.getLeftChild(), newHistory)) {
      UnionNode branch = new UnionNode(partial.getTree(), node.getRightChild());
      partials.add(new Enumerant(branch, partial.getUnknowns(), 0, UnknownNode.ExpansionChoice.Union));
    }

    for (Enumerant partial : sliceNode(node.getRightChild(), newHistory)) {
      UnionNode branch = new UnionNode(node.getLeftChild(), partial.getTree());
      partials.add(new Enumerant(branch, partial.getUnknowns(), 0, UnknownNode.ExpansionChoice.Union));
    }

    UnknownNode unknown = new UnknownNode(history);
    partials.add(new Enumerant(unknown, unknown, node.descendants(), UnknownNode.ExpansionChoice.Union));
    return partials;
  }

  private static List<Enumerant> sliceRepetition (RepetitionNode node, List<UnknownNode.ExpansionChoice> history) {
    List<Enumerant> partials = new LinkedList<>();
    List<UnknownNode.ExpansionChoice> newHistory = new LinkedList<>(history);
    newHistory.add(UnknownNode.ExpansionChoice.Repeat);

    /**
     * For each permutation of unknowns added to this node's children, add
     * an enumerant with the original bounds an another enumerant with unknown
     * bounds.So given:
     * 
     * (\d\w){1,3}
     *
     * Add (where ? represents an unknown):
     *
     * (?\w){1,3}
     * (?\w){?,?}
     * (\d?){1,3}
     * (\d?){?,?}
     * ?{1,3}
     * ?{?,?}
     */
    for (Enumerant partial : sliceNode(node.getChild(), newHistory)) {
      RepetitionNode branch = new RepetitionNode(partial.getTree(), node.getMin(), node.getMax());
      partials.add(new Enumerant(branch, partial.getUnknowns(), partial.getCost(), UnknownNode.ExpansionChoice.Repeat));

      branch = new RepetitionNode(partial.getTree(), new UnknownInt());
      partials.add(new Enumerant(branch, partial.getUnknowns(), partial.getCost() + 1, UnknownNode.ExpansionChoice.Repeat));
    }

    UnknownNode unknown = new UnknownNode(history);
    partials.add(new Enumerant(unknown, unknown, node.descendants(), UnknownNode.ExpansionChoice.Repeat));
    return partials;
  }

  private static List<Enumerant> sliceOptional (OptionalNode node, List<UnknownNode.ExpansionChoice> history) {
    List<Enumerant> partials = new LinkedList<>();
    List<UnknownNode.ExpansionChoice> newHistory = new LinkedList<>(history);
    newHistory.add(UnknownNode.ExpansionChoice.Optional);

    for (Enumerant partial : sliceNode(node.getChild(), newHistory)) {
      OptionalNode branch = new OptionalNode(partial.getTree());
      partials.add(new Enumerant(branch, partial.getUnknowns(), partial.getCost(), UnknownNode.ExpansionChoice.Optional));
    }

    UnknownNode unknown = new UnknownNode(history);
    partials.add(new Enumerant(unknown, unknown, node.descendants(), UnknownNode.ExpansionChoice.Optional));
    return partials;
  }

  private static List<Enumerant> sliceStar (StarNode node, List<UnknownNode.ExpansionChoice> history) {
    List<Enumerant> partials = new LinkedList<>();
    List<UnknownNode.ExpansionChoice> newHistory = new LinkedList<>(history);
    newHistory.add(UnknownNode.ExpansionChoice.Star);

    for (Enumerant partial : sliceNode(node.getChild(), newHistory)) {
      StarNode branch = new StarNode(partial.getTree());
      partials.add(new Enumerant(branch, partial.getUnknowns(), partial.getCost(), UnknownNode.ExpansionChoice.Star));
    }

    UnknownNode unknown = new UnknownNode(history);
    partials.add(new Enumerant(unknown, unknown, node.descendants(), UnknownNode.ExpansionChoice.Star));
    return partials;
  }

  private static List<Enumerant> slicePlus (PlusNode node, List<UnknownNode.ExpansionChoice> history) {
    List<Enumerant> partials = new LinkedList<>();
    List<UnknownNode.ExpansionChoice> newHistory = new LinkedList<>(history);
    newHistory.add(UnknownNode.ExpansionChoice.Plus);

    for (Enumerant partial : sliceNode(node.getChild(), newHistory)) {
      PlusNode branch = new PlusNode(partial.getTree());
      partials.add(new Enumerant(branch, partial.getUnknowns(), partial.getCost(), UnknownNode.ExpansionChoice.Plus));
    }

    UnknownNode unknown = new UnknownNode(history);
    partials.add(new Enumerant(unknown, unknown, node.descendants(), UnknownNode.ExpansionChoice.Plus));
    return partials;
  }

  private static List<Enumerant> sliceAtomic (List<UnknownNode.ExpansionChoice> history) {
    UnknownNode unknown = new UnknownNode(history);
    return Arrays.asList(new Enumerant(unknown, unknown, 1, UnknownNode.ExpansionChoice.Concat));
  }
}
